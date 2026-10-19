package org.darkan.lobby.web.controller

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.darkan.core.model.Account
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.net.web.AccountCreateRequest
import org.darkan.core.net.web.ConflictException
import org.darkan.lobby.name.DisplayNameRefusal
import org.darkan.lobby.name.DisplayNameService
import org.darkan.lobby.web.apiKeyPresented

private val passwordFields = arrayOf(Account::passwordHash.name, Account::password.name, Account::legacyPass.name)

@Serializable
private data class DisplayNameRequest(val displayName: String)

@Serializable
private data class GrantChangesRequest(val changes: Int)

@Serializable
private data class RequireNameChangeRequest(val required: Boolean = true)

object AccountsController {
    fun registerRoutes(routing: Routing) {
        routing.route("/accounts") {
            get("/{username}") {
                val username = call.parameters["username"] ?: throw IllegalArgumentException("Invalid username")
                val account = Accounts.find(username)
                    ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(account.withoutPasswords())
            }

            post {
                if (!call.apiKeyPresented()) return@post call.respond(HttpStatusCode.Unauthorized)
                val req = call.receive<AccountCreateRequest>()
                if (DisplayNameService.available(req.username) != null) throw ConflictException(DisplayNameRefusal.TAKEN.message)
                val account = Accounts.create(req) ?: return@post call.respond(HttpStatusCode.Conflict)
                call.respond(account.withoutPasswords())
            }

            put("/{username}/display-name") {
                if (!call.apiKeyPresented()) return@put call.respond(HttpStatusCode.Unauthorized)
                val username = call.parameters["username"] ?: throw IllegalArgumentException("Invalid username")
                val refusal = DisplayNameService.change(username, call.receive<DisplayNameRequest>().displayName)
                    ?: return@put call.respond(HttpStatusCode.OK, Accounts.find(username)!!.withoutPasswords())
                call.respond(refusal.status(), mapOf("error" to refusal.message))
            }

            post("/{username}/display-name-changes") {
                if (!call.apiKeyPresented()) return@post call.respond(HttpStatusCode.Unauthorized)
                val username = call.parameters["username"] ?: throw IllegalArgumentException("Invalid username")
                val account = DisplayNameService.grantChanges(username, call.receive<GrantChangesRequest>().changes)
                    ?: return@post call.respond(HttpStatusCode.NotFound)
                call.respond(account.withoutPasswords())
            }

            put("/{username}/name-change-required") {
                if (!call.apiKeyPresented()) return@put call.respond(HttpStatusCode.Unauthorized)
                val username = call.parameters["username"] ?: throw IllegalArgumentException("Invalid username")
                val account = DisplayNameService.requireNameChange(username, call.receive<RequireNameChangeRequest>().required)
                    ?: return@put call.respond(HttpStatusCode.NotFound)
                call.respond(account.withoutPasswords())
            }
        }
    }
}

private fun DisplayNameRefusal.status() = when (this) {
    DisplayNameRefusal.INVALID -> HttpStatusCode.BadRequest
    DisplayNameRefusal.TAKEN -> HttpStatusCode.Conflict
    DisplayNameRefusal.NO_CHANGES_LEFT -> HttpStatusCode.PaymentRequired
    DisplayNameRefusal.UNKNOWN_ACCOUNT -> HttpStatusCode.NotFound
}

private fun Account.withoutPasswords() = JsonObject(Json.encodeToJsonElement(this).jsonObject.filterNot { passwordFields.contains(it.key) })
