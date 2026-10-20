package com.jagex.game.runetek5.network

import com.jagex.game.runetek5.tools.TimeTools
import java.io.InputStream

class DroppedInputStream : InputStream() {
    override fun read(): Int {
        TimeTools.sleep(30000L)
        return -1
    }
}
