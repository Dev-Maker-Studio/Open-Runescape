package org.darkan.lobby.name

import org.darkan.core.Utils
import org.darkan.core.formatPlayerNameForDisplay
import org.darkan.core.formatPlayerNameForProtocol
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Cs2
import org.darkan.core.gameval.gen.Graphic
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.model.Account
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.net.prot.packet.IfCloseSub
import org.darkan.core.net.prot.packet.IfOpenSub
import org.darkan.core.net.prot.packet.IfSetGraphic
import org.darkan.core.net.prot.packet.IfSetHide
import org.darkan.core.net.prot.packet.IfSetText
import org.darkan.core.net.prot.packet.RunScript
import org.darkan.lobby.web.model.LobbyPlayer
import java.util.concurrent.ConcurrentHashMap

/**
 * The lobby's "choose a character name" screen. A client sign-up only gives the lobby an email, so
 * an account holds no name of its own until this screen takes one; a moderator can also send an
 * account back through it. Until it is answered the player is held in the lobby - no social, no
 * world login - so a name nobody has approved can never reach a world.
 *
 * A name is confirmed twice, as it was in the legacy lobby: the first entry only reports whether
 * the name is free, and the same name entered or the continue button pressed again takes it.
 */
object LobbyNameScreen {

    private val emptyXteas = IntArray(4)
    private val pendingConfirmation = ConcurrentHashMap<String, String>()

    private const val INITIAL_PROMPT =
        "You haven't picked a character name yet!<br>Please enter a name for your character below."
    private const val REVOKED_PROMPT =
        "Your display name is no longer available.<br>Please enter a new name for your character below."

    fun holds(account: Account) = account.mustChooseName()

    suspend fun open(player: LobbyPlayer) {
        val account = player.account
        player.session.send(IfSetText(Interface.interface_906, NAME_SCREEN_MESSAGE, ""))
        player.session.send(IfOpenSub(Interface.interface_906, NAME_SCREEN_HOST, Interface.interface_890, true, emptyXteas))
        player.session.send(IfSetHide(Interface.interface_906, NAME_SCREEN_HOST, false))
        player.session.send(IfSetHide(Interface.interface_906, NAME_SCREEN_BACKDROP, false))
        player.session.send(IfSetHide(Interface.interface_890, VERDICT_ICON, false))
        for (component in intArrayOf(SUGGESTIONS, EMAIL_ROW, EXISTING_NAME_ROW)) {
            player.session.send(IfSetHide(Interface.interface_890, component, true))
        }
        player.session.send(IfSetText(Interface.interface_890, PROMPT, prompt(account)))
    }

    /** The continue button; confirms a name the player has already been told is free. */
    suspend fun confirm(player: LobbyPlayer) {
        val pending = pendingConfirmation[player.account.username]
        if (pending == null) {
            player.session.send(RunScript(Cs2.cs2_3945, arrayOf(1, 0, 0)))
            return
        }
        submit(player, pending)
    }

    suspend fun submit(player: LobbyPlayer, input: String) {
        val account = player.account
        val username = input.formatPlayerNameForProtocol()
        val displayName = username.formatPlayerNameForDisplay()

        when (DisplayNameService.claimable(displayName, account)) {
            DisplayNameRefusal.INVALID -> return refuse(
                player,
                "Unfortunately, $displayName is not a valid name.<br>Please follow the constraints listed below.",
            )
            null -> Unit
            else -> return refuse(player, "Unfortunately, $displayName is already taken.<br>Please try another name.")
        }
        if (pendingConfirmation[account.username] != username) {
            pendingConfirmation[account.username] = username
            player.session.send(IfSetText(Interface.interface_890, PROMPT, "The name $displayName is available!<br>Press continue again to confirm."))
            player.session.send(IfSetGraphic(Interface.interface_890, VERDICT_ICON, Graphic.symbols_1_3))
            return
        }
        apply(player, username, displayName)
    }

    fun forget(username: String) {
        pendingConfirmation.remove(username)
    }

    private suspend fun apply(player: LobbyPlayer, username: String, displayName: String) {
        val account = player.account
        val previousUsername = account.username
        if (account.isUnnamedSignup()) {
            @Suppress("DEPRECATION")
            account.initializeUsername(username)
            account.nameChangeRequired = false
            Accounts.rename(previousUsername, account)
        } else {
            account.renameTo(displayName, forced = true)
            Accounts.save(account)
        }
        pendingConfirmation.remove(previousUsername)
        player.session.send(IfCloseSub(Interface.interface_906, NAME_SCREEN_HOST))
        player.session.send(IfSetHide(Interface.interface_906, NAME_SCREEN_HOST, true))
        player.session.send(IfSetHide(Interface.interface_906, NAME_SCREEN_BACKDROP, true))
        player.enterLobby()
    }

    private suspend fun refuse(player: LobbyPlayer, message: String) {
        pendingConfirmation.remove(player.account.username)
        player.session.send(IfSetText(Interface.interface_890, PROMPT, message))
        player.session.send(IfSetGraphic(Interface.interface_890, VERDICT_ICON, Graphic.symbols_1_5))
        player.session.send(RunScript(Cs2.cs2_3945, arrayOf(0, 0, 1)))
    }

    private fun prompt(account: Account) = if (account.nameChangeRequired) REVOKED_PROMPT else INITIAL_PROMPT

    private val NAME_SCREEN_HOST = Utils.componentIdFromHash(Component.interface_906.component_906_40)
    private val NAME_SCREEN_BACKDROP = Utils.componentIdFromHash(Component.interface_906.component_906_59)
    private val NAME_SCREEN_MESSAGE = Utils.componentIdFromHash(Component.interface_906.component_906_298)
    private val PROMPT = Utils.componentIdFromHash(Component.interface_890.component_890_19)
    private val EXISTING_NAME_ROW = Utils.componentIdFromHash(Component.interface_890.component_890_22)
    private val EMAIL_ROW = Utils.componentIdFromHash(Component.interface_890.component_890_29)
    private val SUGGESTIONS = Utils.componentIdFromHash(Component.interface_890.component_890_30)
    private val VERDICT_ICON = Utils.componentIdFromHash(Component.interface_890.component_890_39)
    val CONTINUE_BUTTON = Utils.componentIdFromHash(Component.interface_890.component_890_31)
}
