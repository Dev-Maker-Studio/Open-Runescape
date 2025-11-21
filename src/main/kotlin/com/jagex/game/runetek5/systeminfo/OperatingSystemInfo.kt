package com.jagex.game.runetek5.systeminfo

import java.io.File

/**
 * Operating system information collector that gathers system properties.
 */
class OperatingSystemInfo {

    init {
        throw Error()
    }

    companion object {
        @JvmField val OS_NAME: String
        @JvmField val OS_ARCHITECTURE: String

        init {
            var infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("java.vendor").lowercase()
            } catch (exception: Exception) {
                // NOOP
            }
            infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("java.version").lowercase()
            } catch (exception: Exception) {
                // NOOP
            }
            infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("os.name").lowercase()
            } catch (exception: Exception) {
                // NOOP
            }
            OS_NAME = infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("os.arch").lowercase()
            } catch (exception: Exception) {
                // NOOP
            }
            OS_ARCHITECTURE = infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("os.version").lowercase()
            } catch (exception: Exception) {
                // NOOP
            }
            infoCollect.lowercase()
            infoCollect = "~/"
            try {
                infoCollect = System.getProperty("user.home").lowercase()
            } catch (exception: Exception) {
                // NOOP
            }
            File(infoCollect)
        }
    }
}
