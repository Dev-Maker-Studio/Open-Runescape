package com.jagex.game.runetek5.client

import com.jagex.game.launch.Loader
import java.awt.Graphics

@Suppress("removal")
interface Engine {
    fun supplyApplet(applet: Loader)

    fun destroy()

    fun start()

    fun init()

    fun paint(graphics: Graphics)
}
