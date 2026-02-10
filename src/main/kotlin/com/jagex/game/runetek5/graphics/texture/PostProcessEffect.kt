package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat

abstract class PostProcessEffect(
    @JvmField val activeToolkit: OpenGlToolkit
) : Node<PostProcessEffect>() {

    @JvmField var aBool7506: Boolean = false

    abstract fun draw(var2: OpenGl2DTexture)

    abstract fun destroy()

    abstract fun resetTexture()

    abstract fun calculateMipmaps(s: Int, t: Int)

    open fun getTexturedFaces(): Int {
        return 1
    }

    open fun loadTexture(): Boolean {
        return false
    }

    open fun getFormat(): GLActionFormat {
        return GLActionFormat.PIXEL_FORMAT
    }

    fun method12129(): Boolean {
        return this.aBool7506
    }

    abstract fun createShaders(): Boolean

    abstract fun displayPresent(): Boolean
}
