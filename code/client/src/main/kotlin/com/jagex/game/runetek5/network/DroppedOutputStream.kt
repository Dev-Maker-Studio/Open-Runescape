package com.jagex.game.runetek5.network

import java.io.IOException
import java.io.OutputStream

class DroppedOutputStream : OutputStream() {
    @Throws(IOException::class)
    override fun write(data: Int) {
        throw IOException()
    }
}
