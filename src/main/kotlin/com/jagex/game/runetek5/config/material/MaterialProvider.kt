package com.jagex.game.runetek5.config.material

interface MaterialProvider {
    fun isCached(materialId: Int): Boolean

    // brightness scales the texture output (see TextureTypeList.setBrightness)
    fun getPixels(materialId: Int, brightness: Float, width: Int, height: Int, readReversed: Boolean): IntArray

    fun getPixelsArgb(materialId: Int, width: Int, height: Int): IntArray

    fun getSize(): Int

    fun getMaterial(materialId: Int): MaterialType

    fun getPixelsHdr(materialId: Int, width: Int, height: Int): FloatArray

    fun cycle()
}
