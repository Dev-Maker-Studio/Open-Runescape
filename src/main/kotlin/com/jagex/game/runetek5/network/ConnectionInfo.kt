package com.jagex.game.runetek5.network

import com.jagex.game.launch.Loader
import java.io.IOException
import java.net.Socket

class ConnectionInfo {
    @JvmField var host: String? = null
    @JvmField var worldId: Int = 0
    @JvmField var devWorld2: Int = 1
    @JvmField var devWorld1: Int = -190419398
    @JvmField var alternate: Boolean = true
    @JvmField var proxied: Boolean = false

    fun equals(connectionInfo: ConnectionInfo?): Boolean {
        return connectionInfo != null && (this.worldId == connectionInfo.worldId && this.host == connectionInfo.host)
    }

    fun swap() {
        if (!this.alternate) {
            this.alternate = true
            this.proxied = true
        } else if (this.proxied) {
            this.proxied = false
        } else {
            this.alternate = false
        }
    }

    @Throws(IOException::class)
    fun createSocket(): Socket {
        val port = Loader.getPort(worldId)
        if (Loader.DEBUG)
            println("Creating socket for world: $worldId at $host:$port")
        return Socket(this.host, port).also { println("Socket created successfully") }
    }
}
