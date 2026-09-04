package com.jagex.game.runetek5.network

class PingRequest(
    @JvmField val ip: String
) {
    @Volatile
    @JvmField
    var ping: Int = -1

    @JvmField
    var startTime: Long = System.currentTimeMillis()
}
