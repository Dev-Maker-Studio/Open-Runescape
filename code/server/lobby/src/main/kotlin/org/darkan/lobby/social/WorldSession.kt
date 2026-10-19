package org.darkan.lobby.social

import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import org.darkan.core.Logger.logWarn
import org.darkan.core.social.SocialEvent
import org.darkan.core.social.gateway.SocialGatewayWireJson
import org.darkan.core.social.gateway.SocialGatewayWireMessage
import org.darkan.core.social.gateway.SocialNotification
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One connected world: a way to push a line of JSON at it, and an inbox that processes its
 * messages one at a time in the order they arrived, so a player's online, offline and actions
 * can never be applied out of order. The transport is a lambda rather than the socket so a test
 * can drive the gateway with no socket behind it.
 */
class WorldSession(
    val worldId: Int,
    val worldName: String,
    val community: Boolean = false,
    private val transport: suspend (String) -> Unit,
) : SocialSink {
    constructor(worldId: Int, worldName: String, socket: WebSocketSession, community: Boolean) :
        this(worldId, worldName, community, { text -> socket.send(Frame.Text(text)) })

    private val closed = AtomicBoolean(false)
    private val inbox = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    fun isClosed(): Boolean = closed.get()

    suspend fun sendText(text: String) {
        if (isClosed()) return
        transport(text)
    }

    suspend fun send(msg: SocialGatewayWireMessage) =
        sendText(SocialGatewayWireJson.json.encodeToString(SocialGatewayWireMessage.serializer(), msg))

    override suspend fun deliver(usernames: List<String>, event: SocialEvent) =
        send(SocialNotification(usernames, event))

    fun post(work: suspend () -> Unit) {
        inbox.trySend(work)
    }

    suspend fun processInbox() {
        for (work in inbox) {
            try {
                work()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logWarn("SocialGateway: message from world $worldId failed", e)
            }
        }
    }

    fun closeInbox() {
        inbox.close()
    }

    fun markClosed() {
        closed.set(true)
    }
}
