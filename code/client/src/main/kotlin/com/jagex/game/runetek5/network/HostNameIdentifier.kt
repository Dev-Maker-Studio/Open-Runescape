package com.jagex.game.runetek5.network

import java.net.InetAddress
import java.net.UnknownHostException

class HostNameIdentifier @Throws(UnknownHostException::class) constructor(ip: Int) : Runnable {

    @JvmField
    @Volatile
    var host: String? = null

    @JvmField
    var address: InetAddress = InetAddress.getByAddress(
        byteArrayOf(
            (ip shr 24 and 0xff).toByte(),
            (ip shr 16 and 0xff).toByte(),
            (ip shr 8 and 0xff).toByte(),
            (ip and 0xff).toByte()
        )
    )

    fun getHostName(): String? {
        return host
    }

    override fun run() {
        host = address.hostName
    }
}
