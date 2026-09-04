package com.jagex.game.runetek5.scene.tile

object TileFunctionMask {
    /**
     * Has similar tile heights on all 4 sides
     */
    const val COMPLETELY_FLAT = 0x1

    /**
     * The tile is lacking color
     */
    const val HD_LIGHT = 0x2 // HD LIGHT..?

    /**
     * Has animated textures
     */
    const val TEXTURE_MOVEMENT = 0x4

    /**
     * Has underwater map
     */
    const val HD_WATER = 0x8

    /**
     * Has HD shadow
     */
    const val HD_SHADING = 0x10

    /**
     * Has
     */
    const val DISABLE_TEXTURES = 0x20

    /**
     * ************************************
     * 			Merged function masks
     * ************************************
     */

    /**
     * Applies attributes flat, monochrome, animated textures
     */
    const val HAS_WATER_LIGHTING = COMPLETELY_FLAT or HD_LIGHT or TEXTURE_MOVEMENT
}
