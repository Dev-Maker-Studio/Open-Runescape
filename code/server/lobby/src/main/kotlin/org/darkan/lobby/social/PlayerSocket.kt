package org.darkan.lobby.social

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.darkan.core.model.Account
import org.darkan.core.social.SocialAction
import org.darkan.core.social.SocialEvent
import org.darkan.core.social.SocialJson

/**
 * A websocket on one player, for the website: the player's [SocialEvent]s go out as they happen,
 * starting with a snapshot, and every frame in is a [SocialAction] by that player.
 */
object PlayerSocket {
    private const val CLOSE_REASON_LIMIT = 100

    suspend fun serve(account: Account, socket: DefaultWebSocketServerSession) {
        val sink = SocketSink(socket)
        SocialGateway.attachListener(account, sink)
        try {
            for (frame in socket.incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                val action = try {
                    SocialJson.json.decodeFromString(SocialAction.serializer(), text)
                } catch (e: Exception) {
                    socket.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, (e.message ?: "Invalid action").take(CLOSE_REASON_LIMIT)))
                    break
                }
                SocialGateway.act(account.username, action)
            }
        } finally {
            withContext(NonCancellable) { SocialGateway.detachListener(account.username, sink) }
        }
    }

    private class SocketSink(private val socket: WebSocketSession) : SocialSink {
        override suspend fun deliver(usernames: List<String>, event: SocialEvent) {
            socket.send(Frame.Text(SocialJson.json.encodeToString(SocialEvent.serializer(), event)))
        }
    }
}
