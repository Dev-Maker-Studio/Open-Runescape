package com.jagex.game.runetek5.graphics.modern.debug

import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteGL
import jaggl.OpenGL
import com.jagex.game.runetek5.toolkit.opengl.GL_PACK_ALIGNMENT
import com.jagex.game.runetek5.toolkit.opengl.GL_RGBA
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_2D
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_HEIGHT
import com.jagex.game.runetek5.toolkit.opengl.GL_TEXTURE_WIDTH
import com.jagex.game.runetek5.toolkit.opengl.GL_UNSIGNED_BYTE
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object MinimapDump {

    private var done = false

    @JvmStatic
    fun dump(sprite: NativeSprite?, tag: String) {
        val prefix = System.getProperty("minimap.dump") ?: return
        if (done) return
        val gl = sprite as? SpriteGL ?: return
        val handle = gl.texture2d.activeHandle
        if (handle <= 0) return
        done = true

        try {
            OpenGL.glBindTexture(GL_TEXTURE_2D, handle)
            val w = OpenGL.glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH)
            val h = OpenGL.glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT)
            if (w <= 0 || h <= 0) return

            val buf = ByteArray(w * h * 4)
            OpenGL.glPixelStorei(GL_PACK_ALIGNMENT, 1)
            OpenGL.glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf)

            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val i = (y * w + x) * 4
                    val r = buf[i].toInt() and 0xff
                    val g = buf[i + 1].toInt() and 0xff
                    val b = buf[i + 2].toInt() and 0xff
                    val a = buf[i + 3].toInt() and 0xff
                    img.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
                }
            }
            val out = File("$prefix-$tag-${w}x$h.png")
            ImageIO.write(img, "png", out)
            System.err.println("[MinimapDump] wrote ${out.absolutePath}")
        } catch (t: Throwable) {
            System.err.println("[MinimapDump] failed: $t")
        }
    }
}
