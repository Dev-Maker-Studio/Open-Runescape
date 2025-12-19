package com.jagex.game.runetek5.scene.ground

import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.shadow.Shadow
import com.jagex.game.runetek5.scene.tile.WaterTile

/**
 * Abstract base class for ground rendering and height calculation.
 */
abstract class Ground(
    @JvmField val width: Int,
    @JvmField val length: Int,
    scaling: Int,
    @JvmField val tileHeights: Array<IntArray>
) {

    @JvmField val tileUnits: Int
    @JvmField val tileScale: Int

    init {
        var scale = 0
        var currentScaling = scaling
        while (currentScaling > 1) {
            scale++
            currentScaling = currentScaling shr 1
        }
        tileUnits = 1 shl scale
        tileScale = scale
    }

    abstract fun setTileShadow(tileX: Int, tileY: Int, displacement: Int)

    abstract fun addBlendedTile(
        tileX: Int,
        tileY: Int,
        offsetsX: IntArray?,
        lightGroundOffsetsA: IntArray?,
        offsetsZ: IntArray?,
        lightGroundOffsetsB: IntArray?,
        mainColors: IntArray,
        blendColors: IntArray?,
        waterMaterials: IntArray,
        waterScaling: IntArray,
        waterTile: WaterTile?,
        hasShadows: Boolean
    )

    abstract fun calculateBlockTiles(
        x: Int,
        y: Int,
        sizesX: IntArray,
        var4: IntArray,
        sizesY: IntArray,
        var6: IntArray,
        var7: IntArray,
        var8: IntArray,
        var9: IntArray,
        floorColor: IntArray,
        minimapColor: IntArray,
        overlayTextureIds: IntArray,
        textureScales: IntArray,
        waterTile: WaterTile?
    )

    abstract fun resetLight()

    /**
     * Calculates the average height at the given coordinates using bilinear interpolation.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The interpolated height
     */
    fun getAverageHeight(x: Int, y: Int): Int {
        val scaledX = x shr tileScale
        val scaledY = y shr tileScale
        if (scaledX >= 0 && scaledY >= 0 && scaledX <= width - 1 && scaledY <= length - 1) {
            val offsetX = x and (tileUnits - 1)
            val offsetY = y and (tileUnits - 1)
            val height1 = tileHeights[scaledX][scaledY] * (tileUnits - offsetX) + offsetX * tileHeights[scaledX + 1][scaledY] shr tileScale
            val height2 = tileHeights[scaledX][scaledY + 1] * (tileUnits - offsetX) + tileHeights[scaledX + 1][scaledY + 1] * offsetX shr tileScale
            return height2 * offsetY + height1 * (tileUnits - offsetY) shr tileScale
        }
        return 0
    }

    abstract fun addShadow(shadow: Shadow?, scaledX: Int, averageHeight: Int, scaledY: Int)

    abstract fun removeShadow(shadow: Shadow?, x: Int, groundOffset: Int, y: Int)

    abstract fun hasRenderingShadows(shadow: Shadow?, x: Int, planesHeightDelta: Int, y: Int): Boolean

    abstract fun addLight(light: PointLight, var2: IntArray)

    abstract fun renderMinimapFloor(
        var1: Int,
        var2: Int,
        var4: Int,
        var5: Int,
        var6: Int,
        var7: Int,
        visibleTiles: Array<BooleanArray>
    )

    abstract fun renderFloor(var1: Int, var2: Int, var3: Int, var4: Array<BooleanArray>, var5: Boolean)

    /**
     * Gets the height at the specified tile coordinates.
     *
     * @param x The tile x coordinate
     * @param y The tile y coordinate
     * @return The height at that tile
     */
    fun getHeight(x: Int, y: Int): Int {
        return tileHeights[x][y]
    }

    abstract fun createShadowAt(x: Int, y: Int, shadow: Shadow?): Shadow?
}
