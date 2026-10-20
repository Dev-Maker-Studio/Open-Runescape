package com.jagex.game.runetek5.config.material

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object TextureDump {
    private val dir: File? = System.getProperty("render.texdump")?.let { File(it) }

    @JvmStatic
    fun write(materialId: Int, pixels: IntArray, width: Int, height: Int) {
        val out = dir ?: return
        try {
            out.mkdirs()
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            image.setRGB(0, 0, width, height, pixels, 0, width)
            ImageIO.write(image, "png", File(out, "mat-$materialId.png"))
        } catch (e: Exception) {
            System.err.println("[TextureDump] mat=$materialId failed: $e")
        }
    }
}
