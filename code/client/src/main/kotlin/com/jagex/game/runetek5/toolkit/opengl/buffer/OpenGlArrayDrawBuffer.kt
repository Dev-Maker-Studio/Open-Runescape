package com.jagex.game.runetek5.toolkit.opengl.buffer

import com.jagex.game.runetek5.buffer.ArrayBufferGL

class OpenGlArrayDrawBuffer(
    @JvmField var buffer: ArrayBufferGL?,
    type: Int,
    size: Int,
    offset: Int
) {
    @JvmField
    var type: Short = type.toShort()

    @JvmField
    var size: Byte = size.toByte()

    @JvmField
    var offset: Byte = offset.toByte()
}
