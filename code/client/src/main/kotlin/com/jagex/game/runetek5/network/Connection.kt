package com.jagex.game.runetek5.network

import java.io.IOException

abstract class Connection {
    @Throws(IOException::class)
    abstract fun write(buffer: ByteArray, length: Int)

    @Throws(IOException::class)
    abstract fun getInstreamCapacityLeft(): Int

    @Throws(IOException::class)
    abstract fun available(byteCount: Int): Boolean

    abstract fun close()

    abstract fun drop()

    @Throws(IOException::class)
    abstract fun read(buffer: ByteArray, src: Int, length: Int): Int

    companion object {
        @JvmField
        var updateConnection: Connection? = null
    }
}
