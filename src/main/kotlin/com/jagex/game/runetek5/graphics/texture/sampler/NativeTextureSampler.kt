package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.opengl.GLFormat

interface NativeTextureSampler : TextureSampler {

    fun supportsTextureRepeat(): Boolean

    fun toTextureCoordS(x: Float): Float

    fun updateTextureRegion(offsetX: Int, offsetY: Int, regionWidth: Int, regionHeight: Int, pixels: IntArray, offset: Int, rowLength: Int)

    fun setRepeat(repeatS: Boolean, repeatT: Boolean)

    fun updateSubImageByte(offsetX: Int, offsetY: Int, regionWidth: Int, regionHeight: Int, data: ByteArray, format: GLFormat, offset: Int, rowLength: Int)

    fun readPixels(destWidth: Int, destHeight: Int, dest: IntArray)

    fun enableMipmapping(): Boolean

    fun getTextureWidth(): Int

    fun getTextureHeight(): Int

    fun toTextureCoordT(y: Float): Float
}
