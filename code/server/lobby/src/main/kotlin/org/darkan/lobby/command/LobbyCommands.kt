package org.darkan.lobby.command

import org.darkan.core.EnvVars
import org.darkan.core.Logger.logError
import org.darkan.core.Logger.logInfo
import org.darkan.core.crypt.Crypto
import org.darkan.core.isValidPassword
import org.darkan.core.redactedCommand
import org.darkan.core.model.Rights
import org.darkan.core.mongo.collections.Accounts
import org.darkan.lobby.web.model.LobbyPlayer
import java.util.EnumMap

/**
 * `::` commands a player can run while sitting in the lobby, mirroring the legacy lobby's
 * `Commands` (`com.rs.lobby.model.commands`). A command is registered under the lowest rights
 * that may run it; [process] walks the tiers from the caller's own rights downwards, so an owner
 * can run everything and a player only the `PLAYER` tier. Non-player commands are logged with
 * their arguments - except the password commands, whose secrets never reach a log line.
 */
object LobbyCommands {

    class Command(val usage: String, val description: String, val run: suspend (LobbyPlayer, List<String>) -> Unit) {
        val name: String get() = usage.substringBefore(' ')
    }

    private val commands = EnumMap<Rights, MutableMap<String, Command>>(Rights::class.java).also { map ->
        for (rights in Rights.entries) map[rights] = LinkedHashMap()
    }

    /** Registers [usage] (`"alias1,alias2 [args]"`) for [rights] and every tier above it. */
    fun add(rights: Rights, usage: String, description: String, run: suspend (LobbyPlayer, List<String>) -> Unit) {
        val command = Command(usage, description, run)
        for (alias in command.name.split(',')) commands.getValue(rights)[alias.lowercase()] = command
    }

    /** Every command an account with [rights] and [username] can run, for a `::commands` listing. */
    fun available(rights: Rights, username: String = EnvVars.lobbyOwner): List<Command> =
        Rights.entries.filter { allowed(it, rights, username) }.flatMap { commands.getValue(it).values.distinct() }

    /**
     * Whether an account holding [rights] under [username] may run the [tier]'s commands.
     * The player tier is open; the owner tier needs the configured owner name; in debug every
     * administrative tier needs it too.
     */
    fun allowed(tier: Rights, rights: Rights, username: String, debug: Boolean = EnvVars.debug, owner: String = EnvVars.lobbyOwner): Boolean {
        if (tier == Rights.PLAYER) return true
        if (rights.ordinal < tier.ordinal) return false
        if (tier == Rights.OWNER || debug) return username.equals(owner, ignoreCase = true)
        return true
    }

    /**
     * Runs [line] (without its `::` prefix) for [player]. Returns false when no command of the
     * player's tier or below matches, so the caller can answer "unknown command".
     */
    suspend fun process(player: LobbyPlayer, line: String): Boolean {
        val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return false
        val name = parts[0].lowercase()
        val args = parts.drop(1)
        for (tier in Rights.entries.reversed()) {
            val command = commands.getValue(tier)[name] ?: continue
            if (!allowed(tier, player.account.rights, player.account.username)) {
                if (tier != Rights.PLAYER) logInfo("Lobby command '$name' refused for ${player.account.username} (${player.account.rights}): not the configured owner")
                continue
            }
            if (tier != Rights.PLAYER) {
                val logged = line.redactedCommand()
                logInfo("Lobby command by ${player.account.username} (${player.account.rights}): $logged")
            }
            try {
                command.run(player, args)
            } catch (e: Throwable) {
                logError("Lobby command '$name' from ${player.account.username} threw", e)
                player.message("Error handling command. Proper usage is ::${command.usage}")
            }
            return true
        }
        return false
    }

    init {
        add(Rights.PLAYER, "commands", "Lists the commands you can use.") { player, _ ->
            for (command in available(player.account.rights, player.account.username)) player.message("::${command.usage} - ${command.description}")
        }

        add(Rights.PLAYER, "changepass [new password]", "Changes your own password.") { player, args ->
            val password = args.joinToString(" ")
            if (!password.isValidPassword()) {
                player.message("Passwords must be at least 8 characters with letters and numbers only.")
                return@add
            }
            player.account.changePassword(Crypto.hashPasswordArgon2(password))
            Accounts.save(player.account)
            player.message("Your password has been changed.")
        }

        add(Rights.OWNER, "changepassother,setpass,resetpass [player name] [new password]", "Changes another player's password (account recovery).") { player, args ->
            if (args.size < 2) {
                player.message("Usage: ::changepassother [player name] [new password]")
                return@add
            }
            val password = args.last()
            val name = args.dropLast(1).joinToString(" ")
            if (!password.isValidPassword()) {
                player.message("Passwords must be at least 8 characters with letters and numbers only.")
                return@add
            }
            val target = Accounts.findByDisplayName(name) ?: Accounts.find(name)
            if (target == null) {
                player.message("No account called \"$name\".")
                return@add
            }
            target.changePassword(Crypto.hashPasswordArgon2(password))
            Accounts.save(target)
            logInfo("Password of ${target.username} changed by ${player.account.username}")
            player.message("${target.displayName}'s password has been changed.")
        }
    }
}
