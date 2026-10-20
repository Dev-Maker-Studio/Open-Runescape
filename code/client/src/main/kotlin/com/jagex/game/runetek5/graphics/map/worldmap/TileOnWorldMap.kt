package com.jagex.game.runetek5.graphics.map.worldmap

class TileOnWorldMap(
    mapsquarePosX: Int,
    mapsquarePosY: Int,
    @JvmField var underlayIdOrColor: Int,
    overlayId: Int,
    tileShapeAndRotation: Int,
    @JvmField val locIds: IntArray?,
    @JvmField val shapeAndRotations: ByteArray?
) {
    @JvmField val mapsquarePosX: Byte = mapsquarePosX.toByte()
    @JvmField val mapsquarePosY: Byte = mapsquarePosY.toByte()
    @JvmField val overlayId: Byte = overlayId.toByte()
    @JvmField val tileShapeAndRotation: Byte = tileShapeAndRotation.toByte()
}
