package com.jagex.game.runetek5.network

import java.io.IOException
import java.io.OutputStream

/**
 *
 * @author Brainpower - 07/05/2022
 *
 */
class DroppedOutputStream : OutputStream() {
    @Throws(IOException::class)
    override fun write(data: Int) {
        throw IOException()
    }
}
