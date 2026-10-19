package org.darkan.lobby.web.controller

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.darkan.core.mongo.collections.Accounts
import org.darkan.lobby.name.DisplayNameFilter
import org.darkan.lobby.name.DisplayNameService
import org.darkan.lobby.name.DisplayNameVerdict
import org.darkan.lobby.web.apiKeyPresented

@Serializable
private data class Offender(
    val username: String,
    val displayName: String,
    val verdict: DisplayNameVerdict,
    val term: String,
    val nameChangeRequired: Boolean,
)

@Serializable
private data class RequireChangesRequest(val usernames: List<String>)

/**
 * Sweeping the accounts that already hold a name the filter would refuse. The sweep only reports;
 * flagging is a second, explicit call with the usernames worth flagging, so nothing is forced onto
 * a player on the strength of an unreviewed match.
 */
object DisplayNamesController {
    fun registerRoutes(routing: Routing) {
        routing.route("/admin/display-names") {
            get("/offenders") {
                if (!call.apiKeyPresented()) return@get call.respond(HttpStatusCode.Unauthorized)
                val offenders = Accounts.findAll().mapNotNull { account ->
                    val match = DisplayNameFilter.match(account.displayName) ?: return@mapNotNull null
                    Offender(account.username, account.displayName, match.verdict, match.term, account.nameChangeRequired)
                }
                call.respond(offenders.sortedBy { it.username })
            }

            post("/require-change") {
                if (!call.apiKeyPresented()) return@post call.respond(HttpStatusCode.Unauthorized)
                val usernames = call.receive<RequireChangesRequest>().usernames
                val flagged = usernames.count { DisplayNameService.requireNameChange(it, true) != null }
                call.respond(mapOf("flagged" to flagged, "requested" to usernames.size))
            }
        }
    }
}
