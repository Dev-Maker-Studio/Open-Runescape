package org.darkan.lobby.web

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.darkan.core.net.web.installCoreValidators
import org.darkan.core.net.web.installExceptions
import kotlin.time.Duration.Companion.seconds

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            coerceInputValues = true
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
            classDiscriminator = "type"
        })
    }

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    installExceptions()
    install(RequestValidation) {
        installCoreValidators()
    }

    configureRouting()
}