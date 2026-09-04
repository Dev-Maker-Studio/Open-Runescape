package com.jagex.game.runetek5.config.mapsprite

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Map sprite configuration with rotation and flip support.
 */
class MapSpriteType {

    @JvmField var fileId: Int = 0
    @JvmField var backgroundColor: Int = 0

    /**
     * Requires adjustment/upscaling?
     */
    @JvmField var requiresUpscaling: Boolean = false
    @JvmField var mapspriteTypeList: MapSpriteTypeList? = null

    /**
     * Decodes map sprite data from the packet buffer.
     *
     * @param stream The packet buffer
     */
    fun decode(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decode(stream, opcode)
        }
    }

    /**
     * Decodes a single opcode from the packet buffer.
     *
     * @param stream The packet buffer
     * @param opcode The opcode to decode
     */
    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> fileId = stream.readBigSmart()
            2 -> backgroundColor = stream.readUMedium()
            3 -> requiresUpscaling = true
            4 -> fileId = -1
        }
    }

    /**
     * Creates a rotated and optionally flipped native sprite.
     *
     * @param toolkit The rendering toolkit
     * @param rotation The number of 90-degree left rotations
     * @param flippedVertical Whether to flip the sprite vertically
     * @return The generated native sprite, or null if not cached
     */
    fun createRotatedNativeSprite(toolkit: AbstractToolkit, rotation: Int, flippedVertical: Boolean): NativeSprite? {
        val uid = fileId.toLong() or (rotation.toLong() shl 16) or
                (if (flippedVertical) 0x40000L else 0L) or
                (toolkit.renderId.toLong() shl 19)
        var nativeSprite = mapspriteTypeList!!.mapspriteCache.get(uid) as NativeSprite?
        if (nativeSprite != null) {
            return nativeSprite
        }
        if (!mapspriteTypeList!!.mapspriteResource.isFileCached(fileId)) {
            return null
        }
        var spriteType = SpriteType.listSingle(mapspriteTypeList!!.mapspriteResource, fileId, 0)
        if (spriteType != null) {
            spriteType.minY = 0
            spriteType.borderX = 0
            spriteType.minX = 0
            spriteType.borderY = 0
            if (flippedVertical) {
                spriteType.flipVertical()
            }
            repeat(rotation) {
                spriteType!!.rotate90DegLeft()
            }
        }
        nativeSprite = toolkit.generateSprite(spriteType, true)
        if (nativeSprite != null) {
            mapspriteTypeList!!.mapspriteCache.put(nativeSprite, uid)
        }
        return nativeSprite
    }

    /**
     * Checks if the map sprite file is cached.
     *
     * @return True if the sprite file is cached
     */
    fun isCached(): Boolean {
        return mapspriteTypeList!!.mapspriteResource.isFileCached(fileId)
    }
}
