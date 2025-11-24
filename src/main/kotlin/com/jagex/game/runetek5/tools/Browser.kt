package com.jagex.game.runetek5.tools

import com.jagex.game.runetek5.client.ServerEnvironment
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.systeminfo.SystemInfo
import java.awt.Desktop
import java.net.URI

object Browser {

    @JvmField var errorWebsiteBaseUrl: String? = null

    @JvmStatic
    fun openBrowser(url: String, actionType: Int, programName: String): Boolean {
        if (actionType == 0) {
            try {
                if (!SystemInfo.operatingSystemLowercase.startsWith("win")) {
                    throw Exception()
                } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    throw Exception()
                } else {
                    val allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789?&=,.%+-_#:/*"

                    for (index in url.indices) {
                        if (allowedChars.indexOf(url[index]) == -1) {
                            throw Exception()
                        }
                    }

                    Runtime.getRuntime().exec("cmd /c start \"j\" \"$url\"")
                    return true
                }
            } catch (throwable: Throwable) {
                return false
            }
        } else {
            return false
        }
    }

    @JvmStatic
    fun openBrowser(url: String, loggedIn: Boolean, programName: String, isApplet: Boolean) {
        if (loggedIn) {
            if (!isApplet && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(URI(url))
                    return
                } catch (exception: Exception) {
                    // NOOP
                }
            }
            if (SystemInfo.operatingSystemLowercase.startsWith("win") && !isApplet) {
                openBrowser(url, 0)
            } else if (SystemInfo.operatingSystemLowercase.startsWith("mac")) {
                openBrowser(url, 1, programName)
            } else {
                openBrowser(url, 2)
            }
        } else {
            openBrowser(url, 3)
        }
    }

    @JvmStatic
    fun openOpenjs(url: String, loggedIn: Boolean, isApplet: Boolean) {
        openBrowser(url, loggedIn, "openjs", isApplet)
    }

    @JvmStatic
    fun openBrowserErrorscreenOpenjs() {
        val url = errorWebsiteBaseUrl ?: ServerEnvironment.getUrl()
        openOpenjs(url, false, client.aBool7158)
    }

    @JvmStatic
    fun openBrowser(url: String, actionType: Int): Boolean {
        return openBrowser(url, actionType, "openjs")
    }
}
