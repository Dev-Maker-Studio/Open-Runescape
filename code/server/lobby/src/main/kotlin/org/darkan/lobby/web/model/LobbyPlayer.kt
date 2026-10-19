package org.darkan.lobby.web.model

import io.ktor.utils.io.*
import org.darkan.core.EnvVars
import org.darkan.core.Logger.logDebug
import org.darkan.core.Logger.logError
import org.darkan.core.model.Account
import org.darkan.core.model.ChatMessageType
import org.darkan.core.model.Vars
import org.darkan.core.model.toLobbyLoginDetails
import org.darkan.core.net.Session
import org.darkan.core.net.login.LoginToken
import org.darkan.core.net.prot.handler.PacketHandlers
import org.darkan.core.net.prot.packet.GameMessage
import org.darkan.core.net.prot.packet.LobbyLoginDetails
import org.darkan.core.net.prot.packet.Ping
import org.darkan.core.net.prot.packet.WorldListPacket
import org.darkan.lobby.Lobby
import org.darkan.lobby.name.LobbyNameScreen
import org.darkan.lobby.social.SocialGateway
import java.util.concurrent.TimeUnit

/**
 * Chatbox text for somebody sitting in the lobby.
 *
 * It has to be [ChatMessageType.FC_NOTIFICATION]. The lobby screen has no game chatbox - its
 * friends-chat window is the only thing that renders server text - so a `GAME` message is encoded,
 * sent, and silently dropped by the client. Legacy drew exactly this distinction:
 * `SocialPlayer.sendMessage` used FC_NOTIFICATION for lobby players while `sendGameMessage` (GAME)
 * was for players on a world, and `ChatManager.sendBroadcast` sent the same broadcast as both
 * (lobby-server `com/rs/lobby/model/SocialPlayer.java`, `com/rs/lobby/social/ChatManager.java`).
 */
suspend fun Session.sendLobbyMessage(text: String) = send(GameMessage(ChatMessageType.FC_NOTIFICATION, text))

private const val SOCIAL_HANDLERS = "org.darkan.lobby.server.packet.social"

class LobbyPlayer(val session: Session, val account: Account) {

    /** Chatbox text for this player; see [sendLobbyMessage] for why the type is not `GAME`. */
    suspend fun message(text: String) = session.sendLobbyMessage(text)

    val vars = Vars().setSession(session)
    var chatType = 0
    val worldLoginToken: String = LoginToken.issue(
        username = account.username,
        nowMs = System.currentTimeMillis(),
        ttlMs = TimeUnit.HOURS.toMillis(EnvVars.worldLoginTokenTtlHours),
        secret = EnvVars.worldLoginTokenSecret,
    )

    suspend fun login(read: ByteReadChannel) {
        try {
            // The client caches the world it is told to "play now" on, so hand it a world that
            // actually exists; the placeholder only stands in until a world registers.
            val defaultWorld = Lobby.worldList.getDefault()?.metadata?.takeIf { it.isValid }
            session.send(
                account.toLobbyLoginDetails(
                    worldLoginToken = worldLoginToken,
                    defaultWorldId = defaultWorld?.number ?: LobbyLoginDetails.DEFAULT_WORLD_ID,
                    defaultWorldAddress = defaultWorld?.ipAddress ?: LobbyLoginDetails.DEFAULT_WORLD_ADDRESS,
                ),
                noIsaac = true,
            )
            session.send(WorldListPacket(Lobby.worldList.snapshot(), refreshOnClient = true, refreshOnlyPlayerCounts = false))
            vars.setVar(281, 1000)
            vars.setVar(2528, 1)
            vars.setVar(2567, 1)

            vars.setVarBit(10242, 1) // 2 for validating email address
            vars.setVarBit(10243, 12)

            vars.setVarc(1919, 1) // set email to validated
            vars.setVarBit(11162, 1)
            vars.syncVarsToClient()

            if (LobbyNameScreen.holds(account)) openNameScreenIn = 1 else enterLobby()
            session.flush()
            session.readPackets(read)
        } finally {
            try {
                LobbyNameScreen.forget(account.username)
                SocialGateway.unregisterLobbyPlayerSession(account.username, session)
            } catch (_: Throwable) {
            }
            session.exit()
            session.disconnect()
        }
    }

    /**
     * The client cannot take the name screen on the tick it logs in - the sub-interface is dropped
     * and the lobby comes up empty - so it is opened a tick later, from the engine loop.
     */
    private var openNameScreenIn = -1

    suspend fun enterLobby() {
        SocialGateway.registerLobbyPlayerSession(account, session)
    }

    suspend fun handleDecodedPackets() {
        if (openNameScreenIn >= 0) {
            if (openNameScreenIn == 0) LobbyNameScreen.open(this)
            openNameScreenIn--
        }
        for (i in 0 until EnvVars.packetQueueCapacity) {
            val packet = session.readChannel.tryReceive().getOrNull() ?: break
            if (packet !is Ping)
                logDebug("Handling packet: $packet")
            try {
                val handler = PacketHandlers.getHandler<LobbyPlayer>(packet.javaClass) ?: continue
                if (LobbyNameScreen.holds(account) && handler.javaClass.packageName.startsWith(SOCIAL_HANDLERS)) continue
                handler.handle(this, packet)
            } catch (e: Throwable) {
                logError("Failed to handle packet: $packet", e)
            }
        }
    }
}