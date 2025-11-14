package com.jagex.game.runetek5.config.material

interface MaterialProvider {
    fun isCached(id: Int): Boolean

    // var2 proly alpha
    fun getPixels(materialId: Int, brightness: Float, width: Int, length: Int, readReversed: Boolean): IntArray

    fun getPixelsArgb(textureId: Int, width: Int, height: Int): IntArray

    fun getSize(): Int

    fun getMaterial(materialId: Int): MaterialType

    fun getPixelsHdr(textureId: Int, width: Int, height: Int): FloatArray

    fun cycle()
}
