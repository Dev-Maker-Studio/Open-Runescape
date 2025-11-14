package com.jagex.game.runetek5.graphics.texture.sampler

import com.jagex.game.runetek5.toolkit.opengl.GLFormat

interface NativeTextureSampler : TextureSampler {

    fun method46(): Boolean

    fun method47(var1: Float): Float

    fun method48(var1: Int, var2: Int, var3: Int, var4: Int, var5: IntArray, var6: Int, var7: Int)

    fun setRepeat(repeatS: Boolean, repeatT: Boolean)

    fun method52(var1: Int, var2: Int, var3: Int, var4: Int, var5: ByteArray, var6: GLFormat, var7: Int, var8: Int)

    fun method53(var3: Int, var4: Int, var5: IntArray)

    fun enableMipmapping(): Boolean

    fun getTextureWidth(): Int

    fun getTextureHeight(): Int

    fun method62(var1: Float): Float
}
