package com.jagex.game.runetek5.systeminfo

import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.Utils
import java.awt.Canvas

object ProfileCpu {
    @JvmStatic
    fun getProfile(): Int {
        val canvas = Canvas()
        canvas.setSize(100, 100)
        val toolkit = AbstractToolkit.create(AbstractToolkit.BOOTSTRAP_MODE, canvas, null, null, 0)
        val timeBeforeBenchmark = Utils.time()
        for (loops in 0 until 10000) {
            toolkit.benchmark()
        }
        val executionTime = (Utils.time() - timeBeforeBenchmark).toInt()
        toolkit.clearPixels(0, 0, 100, 100, 0xffffff.inv())
        toolkit.cleanupAndShutdown()
        return executionTime
    }
}
