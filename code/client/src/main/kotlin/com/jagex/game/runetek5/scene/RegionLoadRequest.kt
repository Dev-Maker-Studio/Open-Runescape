package com.jagex.game.runetek5.scene

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.movement.CoordGrid

class RegionLoadRequest(
    @JvmField val loadingType: RegionLoadingType?,
    @JvmField val buffer: Packet.Bit?
) {

    class SceneFrame(@JvmField val base: CoordGrid, @JvmField val size: Int) {
        fun contains(absX: Int, absY: Int): Boolean {
            return absX >= base.x && absY >= base.y && absX < base.x + size && absY < base.y + size
        }
    }

    fun peekSceneFrame(): SceneFrame? {
        val stream = buffer ?: return null
        val start = stream.currentPosition
        try {
            val mapSizeType: Int
            val centreChunkX: Int
            val centreChunkY: Int
            if (loadingType == RegionLoadingType.STANDARD) {
                mapSizeType = stream.readUByte()
                centreChunkX = stream.readUShort()
                centreChunkY = stream.readUShort()
            } else if (loadingType != null && loadingType.constructedMap()) {
                stream.readUByteInverse()
                stream.readUByteSubtract()
                mapSizeType = stream.readUByteAdd()
                centreChunkY = stream.readUShortAdd()
                centreChunkX = stream.readUShort()
            } else {
                return null
            }
            val size = MapSize.get(mapSizeType)?.size ?: return null
            return SceneFrame(CoordGrid(0, (centreChunkX - (size shr 4)) * 8, (centreChunkY - (size shr 4)) * 8), size)
        } finally {
            stream.currentPosition = start
        }
    }
}
