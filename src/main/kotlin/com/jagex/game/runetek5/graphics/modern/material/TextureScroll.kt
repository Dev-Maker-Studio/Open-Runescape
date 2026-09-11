package com.jagex.game.runetek5.graphics.modern.material

import com.jagex.game.runetek5.config.material.MaterialType

object TextureScroll {
    const val NONE = 0

    fun pack(material: MaterialType): Int =
        (material.textureSpeedU.toInt() and 0xFF) or ((material.textureSpeedV.toInt() and 0xFF) shl 8)

    fun speedU(packed: Int): Float = (packed shl 24 shr 24).toFloat()

    fun speedV(packed: Int): Float = (packed shl 16 shr 24).toFloat()

    fun seconds(progressionMs: Int): Float = (progressionMs % 128000) / 1000f

    fun offset(seconds: Float, speed: Byte): Float = seconds * speed / 64f % 1f
}
