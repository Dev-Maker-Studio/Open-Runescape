package com.jagex.game.runetek5.network

import com.jagex.game.runetek5.tools.TimeTools
import java.io.InputStream

/**
 *
 * @author Brainpower - 07/05/2022
 *
 */
class DroppedInputStream : InputStream() {
    override fun read(): Int {
        TimeTools.sleep(30000L)
        return -1
    }
}
