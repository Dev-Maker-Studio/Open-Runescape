package org.darkan.lobby.web.controller

import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.darkan.lobby.Lobby

object WorldListController {
    fun registerRoutes(routing: Routing) {
        routing.route("/worldlist") {
            get { call.respond(Lobby.worldList.getWorldArray()) }
        }
    }
}
