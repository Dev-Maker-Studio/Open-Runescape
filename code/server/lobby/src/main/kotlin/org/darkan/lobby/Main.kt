package org.darkan.lobby

import kotlinx.coroutines.runBlocking
import org.darkan.core.Logger
import org.darkan.core.Logger.logError
import org.darkan.core.net.prot.revision.rev727.register727
import java.util.TimeZone
import java.util.logging.Level
import kotlin.system.exitProcess

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    try {
        Logger.setLogLevel(Level.ALL)
        register727()
        Lobby.start()
        Runtime.getRuntime().addShutdownHook(Thread {
            runBlocking {
                Lobby.stop()
            }
        })
        Thread.currentThread().join()
    } catch (e: Exception) {
        logError("Unexpected error", e)
        exitProcess(1)
    }
}