package com.jagex.game.runetek5.network

import com.jagex.game.runetek5.tools.TimeTools
import jaclib.ping.Ping
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Continuously pings a server to measure network latency.
 * Runs in a background thread and updates ping measurements.
 */
class ConnectionPinger : Runnable {

    @JvmField var ip: String? = null
    @JvmField var address: InetAddress? = null

    /** Current ping time in milliseconds, or -1 if unavailable */
    @Volatile
    @JvmField var ping: Long = -1L

    /** Whether the pinger thread should continue running */
    @Volatile
    @JvmField var enabled: Boolean = true

    /**
     * Sets the IP address to ping
     * @param ip The IP address or hostname
     */
    fun setIp(ip: String?) {
        this.ip = ip
        this.address = null
        this.ping = -1L

        if (this.ip != null) {
            try {
                this.address = InetAddress.getByName(this.ip)
            } catch (e: UnknownHostException) {
                // Silently ignore - address remains null
            }
        }
    }

    /**
     * Disables the pinger, causing the thread to exit
     */
    fun disable() {
        enabled = false
    }

    override fun run() {
        while (enabled) {
            requestPing()
        }
    }

    /**
     * Gets the current ping time
     * @return Ping time in milliseconds, or -1 if unavailable
     */
    fun getPing(): Long = ping

    /**
     * Performs a single ping request and updates the ping value
     */
    fun requestPing() {
        address?.let { addr ->
            try {
                val ipBytes = addr.address
                ping = Ping.ping(ipBytes[0], ipBytes[1], ipBytes[2], ipBytes[3], 1000L).toLong()
            } catch (e: Throwable) {
                // Silently ignore ping failures
            }
        }
        TimeTools.sleep(1000L)
    }
}
