package com.jagex.game.runetek5.scene.culling

import com.jagex.game.runetek5.scene.Scene

class CullingCluster(
    scene: Scene,
    type: Int,
    plane: Int,
    tileEndX: Int,
    tileStartX: Int,
    tileStartX2: Int,
    tileEndX2: Int,
    heightSouthWest: Int,
    heightNorthEast: Int,
    heightNorthWest: Int,
    heightSouthEast: Int,
    tileEndY: Int,
    tileStartY: Int,
    tileStartY2: Int,
    tileEndY2: Int
) {

    @JvmField val type: Byte = type.toByte()
    @JvmField val plane: Byte = plane.toByte()
    @JvmField val tileCullingsX: IntArray
    @JvmField val drawHeights: IntArray
    @JvmField val tileCullingsZ: IntArray
    @JvmField val startX: Short
    @JvmField val endX: Short
    @JvmField val startY: Short
    @JvmField val endY: Short
    @JvmField val projectionX: ShortArray
    @JvmField val projectionY: ShortArray
    @JvmField val projectionZ: ShortArray

    init {
        this.tileCullingsX = IntArray(4)
        this.drawHeights = IntArray(4)
        this.tileCullingsZ = IntArray(4)
        this.tileCullingsX[0] = tileEndX
        this.tileCullingsX[1] = tileStartX
        this.tileCullingsX[2] = tileStartX2
        this.tileCullingsX[3] = tileEndX2
        this.drawHeights[0] = heightSouthWest
        this.drawHeights[1] = heightNorthEast
        this.drawHeights[2] = heightNorthWest
        this.drawHeights[3] = heightSouthEast
        this.tileCullingsZ[0] = tileEndY
        this.tileCullingsZ[1] = tileStartY
        this.tileCullingsZ[2] = tileStartY2
        this.tileCullingsZ[3] = tileEndY2
        this.startX = (tileEndX shr scene.tileUnits).toShort()
        this.endX = (tileStartX2 shr scene.tileUnits).toShort()
        this.startY = (tileEndY shr scene.tileUnits).toShort()
        this.endY = (tileStartY2 shr scene.tileUnits).toShort()
        this.projectionX = ShortArray(4)
        this.projectionY = ShortArray(4)
        this.projectionZ = ShortArray(4)
    }
}
