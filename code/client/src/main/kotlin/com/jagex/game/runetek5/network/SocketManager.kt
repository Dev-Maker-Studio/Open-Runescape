package com.jagex.game.runetek5.network

import com.jagex.game.launch.Loader
import java.io.IOException
import java.net.Socket

abstract class SocketManager {
    @JvmField
    var siteHostName: String? = null

    @JvmField
    var sitePort: Int = 0

    @Throws(IOException::class)
    abstract fun getProxySocket(): Socket

    @Throws(IOException::class)
    open fun getLobbySocket(): Socket {
        return Socket(Loader.IP_ADDRESS, Loader.LOBBY_PORT)
    }
}
