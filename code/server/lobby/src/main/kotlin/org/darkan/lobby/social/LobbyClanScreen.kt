package org.darkan.lobby.social

import org.darkan.core.Logger.logTrace
import org.darkan.core.clan.ClanAction
import org.darkan.core.formatPlayerNameForDisplay
import org.darkan.core.model.ClanSetting
import org.darkan.core.model.ClanVar
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.net.prot.packet.IfButton
import org.darkan.core.net.prot.packet.RunScript
import org.darkan.lobby.web.model.LobbyPlayer
import java.util.concurrent.ConcurrentHashMap

/**
 * The clan interfaces a player can use from the lobby screen. The lobby opens the same clan
 * interfaces the world server does, so the component ids are the ones the in-world clan plugin
 * uses; each button becomes the [ClanAction] a world would send, handed to [ClanService.handle]
 * so the lobby screen and the worlds share one rule set (`docs/clans.md`).
 */
object LobbyClanScreen {

    /** Chatbox "enter a name" prompt; `sendInputNameScript` in the legacy world server. */
    private const val INPUT_NAME_SCRIPT = 109

    const val CLAN_CHAT_INTERFACE = 1110
    const val CLAN_SETTINGS_INTERFACE = 1096

    /** Which outstanding prompt a player's next name entry answers. */
    private enum class NameInput { CREATE_CLAN, JOIN_GUEST_CC, BAN, UNBAN }

    private val pendingNameInput = ConcurrentHashMap<String, NameInput>()

    suspend fun button(player: LobbyPlayer, packet: IfButton) {
        val username = player.account.username
        when (packet.interfaceId) {
            CLAN_CHAT_INTERFACE -> when (packet.componentId) {
                82 -> toggleClanChannel(player)
                91 -> toggleGuestClanChannel(player)
                95 -> prompt(player, NameInput.BAN, "Which player would you like to ban?")
                99 -> prompt(player, NameInput.UNBAN, "Which player would you like to unban?")
                109 -> run(player, ClanAction.Leave)
                else -> logTrace("LobbyClanScreen: unhandled clan chat button ${packet.componentId} from $username")
            }

            CLAN_SETTINGS_INTERFACE -> when (packet.componentId) {
                94 -> toggleSetting(player, ClanSetting.GUESTS_CAN_ENTER_CC)
                95 -> toggleSetting(player, ClanSetting.GUESTS_CAN_TALK_CC)
                96 -> toggleSetting(player, ClanSetting.IS_RECRUITING)
                else -> logTrace("LobbyClanScreen: unhandled clan settings button ${packet.componentId} from $username")
            }

            else -> logTrace("LobbyClanScreen: unhandled lobby button ${packet.interfaceId}:${packet.componentId} from $username")
        }
    }

    /** The answer to the last [prompt]; ignored when nothing was asked. */
    suspend fun nameInput(player: LobbyPlayer, input: String) {
        val name = input.formatPlayerNameForDisplay()
        when (pendingNameInput.remove(player.account.username) ?: return) {
            NameInput.CREATE_CLAN -> {
                if (run(player, ClanAction.Create(name))) player.message("The clan $name has been created!")
            }
            NameInput.JOIN_GUEST_CC -> run(player, ClanAction.JoinGuestChannel(name))
            NameInput.BAN -> resolve(player, name)?.let { run(player, ClanAction.Ban(it)) }
            NameInput.UNBAN -> resolve(player, name)?.let { run(player, ClanAction.Unban(it)) }
        }
    }

    /** The forum quick-find code dialogue: a base-36 code stored as the `FORUM_QFC` setting. */
    suspend fun forumQfc(player: LobbyPlayer, qfc: String) {
        val code = qfc.toLongOrNull(36)
        if (code == null) {
            player.message("Please enter a valid formatted code.")
            return
        }
        run(player, ClanAction.UpdateSettings(mapOf(ClanSetting.FORUM_QFC.name to code)))
    }

    fun forget(username: String) {
        pendingNameInput.remove(username)
    }

    /** Clan chat toggle. With no clan yet this starts the create-a-clan flow, as in-world does. */
    private suspend fun toggleClanChannel(player: LobbyPlayer) {
        val account = Accounts.find(player.account.username) ?: return
        if (ClanService.clanOf(account.username) == null) {
            prompt(player, NameInput.CREATE_CLAN, "Which name would you like for your clan?")
            return
        }
        run(player, if (account.social.connectedToClan) ClanAction.LeaveChannel else ClanAction.JoinChannel)
    }

    private suspend fun toggleGuestClanChannel(player: LobbyPlayer) {
        val account = Accounts.find(player.account.username) ?: return
        if (account.social.guestedClanChat != null) {
            run(player, ClanAction.LeaveGuestChannel)
            return
        }
        prompt(player, NameInput.JOIN_GUEST_CC, "Which clan chat would you like to join?")
    }

    private suspend fun toggleSetting(player: LobbyPlayer, setting: ClanSetting) {
        val clan = ClanService.clanOf(player.account.username)
        if (clan == null) {
            player.message("You must be in a clan to do that.")
            return
        }
        val enabled = (clan.getSetting(setting) as? Number)?.toInt() == 1
        val value = if (enabled) 0 else 1
        if (!run(player, ClanAction.UpdateSettings(mapOf(setting.name to value)))) return
        if (setting == ClanSetting.IS_RECRUITING) run(player, ClanAction.SetVar(ClanVar.RECRUITING.id, value))
    }

    private suspend fun prompt(player: LobbyPlayer, purpose: NameInput, question: String) {
        pendingNameInput[player.account.username] = purpose
        player.session.send(RunScript(INPUT_NAME_SCRIPT, arrayOf(question)))
    }

    /** The protocol username behind a typed display name, messaging the player when there is none. */
    private suspend fun resolve(player: LobbyPlayer, displayName: String): String? {
        val target = Accounts.findByDisplayName(displayName)
        if (target == null) player.message("Player not found.")
        return target?.username
    }

    /** Runs [action] for the player and shows them the reason when it is refused. */
    private suspend fun run(player: LobbyPlayer, action: ClanAction): Boolean {
        val result = ClanService.handle(player.account.username, action)
        if (!result.ok) player.message(result.message ?: "Unable to do that right now.")
        return result.ok
    }
}
