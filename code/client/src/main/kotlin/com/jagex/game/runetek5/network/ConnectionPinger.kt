package com.jagex.game.runetek5.network

import com.jagex.game.runetek5.tools.TimeTools
import jaclib.ping.Ping
import java.net.InetAddress
import java.net.UnknownHostException

class ConnectionPinger : Runnable {

    @JvmField var ip: String? = null
    @JvmField var address: InetAddress? = null

    @Volatile
    @JvmField var ping: Long = -1L

    @Volatile
    @JvmField var enabled: Boolean = true

    fun setIp(ip: String?) {
        this.ip = ip
        this.address = null
        this.ping = -1L

        if (this.ip != null) {
            try {
                this.address = InetAddress.getByName(this.ip)
            } catch (e: UnknownHostException) {
            }
        }
    }

    fun disable() {
        enabled = false
    }

    override fun run() {
        while (enabled) {
            requestPing()
        }
    }

    fun getPing(): Long = ping

    fun requestPing() {
        address?.let { addr ->
            try {
                val ipBytes = addr.address
                ping = Ping.ping(ipBytes[0], ipBytes[1], ipBytes[2], ipBytes[3], 1000L).toLong()
            } catch (e: Throwable) {
            }
        }
        TimeTools.sleep(1000L)
    }
}
