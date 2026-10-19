package org.darkan.lobby.web.controller

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.social.gateway.AnnouncementBroadcast
import org.darkan.core.social.gateway.AnnouncementChannel
import org.darkan.lobby.social.PlayerSocket
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.social.SocialSubscribers
import org.darkan.lobby.web.apiKeyPresented

@Serializable
data class FriendsChatSay(val username: String, val chatOwner: String, val message: String)

@Serializable
data class BroadcastSay(val username: String? = null, val message: String)

private val LEADING_TAGS = Regex("^(<(col|shad|img)=[^>]*>)+")

object SocialController {
    fun registerRoutes(routing: Routing) {
        routing.route("/social") {
            webSocket("/subscribe") {
                if (!call.apiKeyPresented()) return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                SocialSubscribers.serve(this)
            }

            webSocket("/players/{username}") {
                if (!call.apiKeyPresented()) return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                val name = call.parameters["username"].orEmpty()
                val account = Accounts.find(name) ?: Accounts.findByDisplayName(name)
                    ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No such account"))
                PlayerSocket.serve(account, this)
            }

            post("/friends-chat") {
                if (!call.apiKeyPresented()) return@post call.respond(HttpStatusCode.Unauthorized)
                val say = call.receive<FriendsChatSay>()
                val owner = Accounts.findByDisplayName(say.chatOwner) ?: Accounts.find(say.chatOwner)
                    ?: return@post call.respond(HttpStatusCode.NotFound, buildJsonObject { put("error", "No such friends chat") })
                val from = Accounts.find(say.username)
                    ?: return@post call.respond(HttpStatusCode.NotFound, buildJsonObject { put("error", "No such account") })
                SocialGateway.sayInFriendsChat(from.username, owner.username, say.message)
                call.respond(buildJsonObject { put("ok", true) })
            }

            post("/broadcast") {
                if (!call.apiKeyPresented()) return@post call.respond(HttpStatusCode.Unauthorized)
                val say = call.receive<BroadcastSay>()
                val text = say.username?.let { username ->
                    val account = Accounts.find(username)
                        ?: return@post call.respond(HttpStatusCode.NotFound, buildJsonObject { put("error", "No such account") })
                    withSenderPrefix(say.message, account.displayName)
                } ?: say.message
                SocialGateway.broadcastAnnouncement(AnnouncementBroadcast(AnnouncementChannel.GAME, text))
                call.respond(buildJsonObject { put("ok", true) })
            }
        }
    }

    private fun withSenderPrefix(message: String, displayName: String): String {
        val tags = LEADING_TAGS.find(message)?.value.orEmpty()
        return tags + "[" + displayName + "] " + message.removePrefix(tags)
    }
}
