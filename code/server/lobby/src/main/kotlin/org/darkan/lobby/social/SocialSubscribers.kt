package org.darkan.lobby.social

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.darkan.core.Logger.logWarn
import org.darkan.core.model.Rights
import java.util.concurrent.CopyOnWriteArraySet

@Serializable
data class FriendsChatMessageEvent(val fcOwner: String, val srcName: String, val srcRights: Rights, val message: String)

@Serializable
private data class SubscriberEvent(val type: String, val data: FriendsChatMessageEvent)

object SocialSubscribers {
    private val sessions = CopyOnWriteArraySet<DefaultWebSocketServerSession>()
    private val json = Json { encodeDefaults = true }

    suspend fun serve(session: DefaultWebSocketServerSession) {
        sessions.add(session)
        try {
            for (frame in session.incoming) if (frame is Frame.Close) break
        } finally {
            sessions.remove(session)
        }
    }

    suspend fun publish(event: FriendsChatMessageEvent) {
        if (sessions.isEmpty()) return
        val text = json.encodeToString(SubscriberEvent("FriendsChatMessage", event))
        for (session in sessions) {
            try {
                session.send(Frame.Text(text))
            } catch (e: Exception) {
                logWarn("Dropping a social subscriber that could not be written to", e)
                sessions.remove(session)
            }
        }
    }
}
