package com.jagex.game.runetek5.systeminfo

import java.io.File

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
            }
            infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("java.version").lowercase()
            } catch (exception: Exception) {
            }
            infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("os.name").lowercase()
            } catch (exception: Exception) {
            }
            OS_NAME = infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("os.arch").lowercase()
            } catch (exception: Exception) {
            }
            OS_ARCHITECTURE = infoCollect.lowercase()
            infoCollect = "Unknown"
            try {
                infoCollect = System.getProperty("os.version").lowercase()
            } catch (exception: Exception) {
            }
            infoCollect.lowercase()
            infoCollect = "~/"
            try {
                infoCollect = System.getProperty("user.home").lowercase()
            } catch (exception: Exception) {
            }
            File(infoCollect)
        }
    }
}
