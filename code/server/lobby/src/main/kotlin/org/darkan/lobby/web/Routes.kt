package org.darkan.lobby.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.darkan.core.EnvVars
import org.darkan.core.social.gateway.AnnouncementBroadcast
import org.darkan.lobby.social.SocialGateway
import org.darkan.lobby.web.controller.AccountsController
import org.darkan.lobby.web.controller.DisplayNamesController
import org.darkan.lobby.web.controller.SocialController
import org.darkan.lobby.web.controller.WorldListController

fun Application.configureRouting() {
    routing {
        get("/health") {
            call.respond(buildJsonObject {
                put("status", "OK")
                put("version", this.javaClass.`package`.implementationVersion ?: "dev")
                put("timestamp", System.currentTimeMillis())
            })
        }

        webSocket("/social/ws") {
            SocialGateway.handleWorldSocket(this)
        }

        post("/admin/announcements") {
            if (!call.apiKeyPresented()) {
                return@post call.respond(HttpStatusCode.Unauthorized, buildJsonObject { put("error", "Unauthorized") })
            }
            val req = call.receive<AnnouncementBroadcast>()
            SocialGateway.broadcastAnnouncement(req)
            call.respond(buildJsonObject { put("ok", true) })
        }

        AccountsController.registerRoutes(this)
        DisplayNamesController.registerRoutes(this)
        WorldListController.registerRoutes(this)
        SocialController.registerRoutes(this)
    }
}

fun ApplicationCall.apiKeyPresented(): Boolean {
    val key = EnvVars.apiKey
    return request.headers["X-API-Key"] == key || request.headers["key"] == key || request.headers["Authorization"] == "Bearer $key"
}
