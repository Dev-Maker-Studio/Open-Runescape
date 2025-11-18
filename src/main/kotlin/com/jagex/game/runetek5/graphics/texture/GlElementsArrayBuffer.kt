package com.jagex.game.runetek5.graphics.texture

import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBuffer

/**
 *
 * @author Steven - 13/09/2023
 *
 */
class GlElementsArrayBuffer(
    toolkit: OpenGlToolkit,
    data: ByteArray?,
    initialCapacity: Int
) : GlBuffer(toolkit, data, initialCapacity), ElementsBufferGL {

    override fun putElements(data: ByteArray, size: Int) {
        put(data, size)
    }

    override fun getId(): Int {
        return 0
    }

    override fun getBufferAddress(): Long {
        return super.buffer.getBufferAddress()
    }
}
