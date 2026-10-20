package com.darkan.bot

import kotlin.concurrent.thread

object DisplayToggle {
    const val COMMAND = "/openrs-show"

    fun intercept(message: String?): Boolean {
        if (message?.trim() != COMMAND) return false
        thread(isDaemon = true) { ProcessBuilder(System.getenv("OPENRS_BIN") ?: "openrs", "toggle").start() }
        return true
    }
}
