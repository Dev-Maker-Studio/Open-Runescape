package com.jagex.game.runetek5.scene.tile

object TileFunctionMask {
    const val COMPLETELY_FLAT = 0x1

    const val HD_LIGHT = 0x2

    const val TEXTURE_MOVEMENT = 0x4

    const val HD_WATER = 0x8

    const val HD_SHADING = 0x10

    const val DISABLE_TEXTURES = 0x20

    const val HAS_WATER_LIGHTING = COMPLETELY_FLAT or HD_LIGHT or TEXTURE_MOVEMENT
}
