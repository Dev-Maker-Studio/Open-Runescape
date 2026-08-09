package com.jagex.game.runetek5.graphics.modern.debug

import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteGL
import org.lwjgl.opengl.GL33
import org.lwjgl.system.MemoryUtil
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Reads the generated minimap background texture straight off the GPU and writes it
 * to a PNG, so the composited-to-screen minimap can be compared against the texture
 * it was built from. That separates a generation bug (wrong pixels in the texture)
 * from a compositing bug (right texture, wrong rotate/scale/sample).
 *
 * Enabled with -Dminimap.dump=<path-prefix>. Dumps once per process.
 */
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
            GL33.glBindTexture(GL33.GL_TEXTURE_2D, handle)
            val w = GL33.glGetTexLevelParameteri(GL33.GL_TEXTURE_2D, 0, GL33.GL_TEXTURE_WIDTH)
            val h = GL33.glGetTexLevelParameteri(GL33.GL_TEXTURE_2D, 0, GL33.GL_TEXTURE_HEIGHT)
            if (w <= 0 || h <= 0) return

            val buf = MemoryUtil.memAlloc(w * h * 4)
            GL33.glPixelStorei(GL33.GL_PACK_ALIGNMENT, 1)
            GL33.glGetTexImage(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, buf)

            val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val i = (y * w + x) * 4
                    val r = buf.get(i).toInt() and 0xff
                    val g = buf.get(i + 1).toInt() and 0xff
                    val b = buf.get(i + 2).toInt() and 0xff
                    val a = buf.get(i + 3).toInt() and 0xff
                    img.setRGB(x, y, (a shl 24) or (r shl 16) or (g shl 8) or b)
                }
            }
            MemoryUtil.memFree(buf)

            val out = File("$prefix-$tag-${w}x$h.png")
            ImageIO.write(img, "png", out)
            System.err.println("[MinimapDump] wrote ${out.absolutePath}")
        } catch (t: Throwable) {
            System.err.println("[MinimapDump] failed: $t")
        }
    }
}
