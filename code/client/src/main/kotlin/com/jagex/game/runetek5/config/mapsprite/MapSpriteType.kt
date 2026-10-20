package com.jagex.game.runetek5.config.mapsprite

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class MapSpriteType {

    @JvmField var fileId: Int = 0
    @JvmField var backgroundColor: Int = 0

    @JvmField var requiresUpscaling: Boolean = false
    @JvmField var mapspriteTypeList: MapSpriteTypeList? = null

    fun decode(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decode(stream, opcode)
        }
    }

    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> fileId = stream.readBigSmart()
            2 -> backgroundColor = stream.readUMedium()
            3 -> requiresUpscaling = true
            4 -> fileId = -1
        }
    }

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

    fun isCached(): Boolean {
        return mapspriteTypeList!!.mapspriteResource.isFileCached(fileId)
    }
}
