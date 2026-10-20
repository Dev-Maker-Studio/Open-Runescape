package com.jagex.game.runetek5.config.hitbar

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.AbstractToolkit

class HitbarType {

    @JvmField var hitbarAlpha = 255
    @JvmField var priority = 255
    @JvmField var fadeStartOffset = -1
    @JvmField var fadeOutDuration = 70
    @JvmField val animationStepSize = 1

    @JvmField var hitbarTypeList: HitbarTypeList? = null

    @JvmField var greenBarSpriteId = -1

    @JvmField var redBarSpriteId = -1

    @JvmField var pGreenBarSpriteId = -1

    @JvmField var pRedBarSpriteId = -1

    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> stream.readUShort()
            2 -> hitbarAlpha = stream.readUByte()
            3 -> priority = stream.readUByte()
            4 -> fadeStartOffset = 0
            5 -> fadeOutDuration = stream.readUShort()
            6 -> stream.readUByte()
            7 -> greenBarSpriteId = stream.readBigSmart()
            8 -> redBarSpriteId = stream.readBigSmart()
            9 -> pGreenBarSpriteId = stream.readBigSmart()
            10 -> pRedBarSpriteId = stream.readBigSmart()
            11 -> fadeStartOffset = stream.readUShort()
        }
    }

    fun decode(stream: Packet) {
        while (true) {
            val opcode = stream.readUByte()
            if (opcode == 0) {
                return
            }
            decode(stream, opcode)
        }
    }

    fun generateSprites(toolkit: AbstractToolkit) {
        val resource = hitbarTypeList!!.spriteResource
        var spriteType: SpriteType
        if (greenBarSpriteId >= 0 && hitbarTypeList!!.spriteCache.get(greenBarSpriteId.toLong()) == null && resource.isFileCached(
                greenBarSpriteId
            )
        ) {
            spriteType = SpriteType.listSingle(resource, greenBarSpriteId)!!
            hitbarTypeList!!.spriteCache.put(toolkit.generateSprite(spriteType, true), greenBarSpriteId.toLong())
        }
        if (redBarSpriteId >= 0 && hitbarTypeList!!.spriteCache.get(redBarSpriteId.toLong()) == null && resource.isFileCached(
                redBarSpriteId
            )
        ) {
            spriteType = SpriteType.listSingle(resource, redBarSpriteId)!!
            hitbarTypeList!!.spriteCache.put(toolkit.generateSprite(spriteType, true), redBarSpriteId.toLong())
        }
        if (pGreenBarSpriteId >= 0 && hitbarTypeList!!.spriteCache.get(pGreenBarSpriteId.toLong()) == null && resource.isFileCached(
                pGreenBarSpriteId
            )
        ) {
            spriteType = SpriteType.listSingle(resource, pGreenBarSpriteId)!!
            hitbarTypeList!!.spriteCache.put(toolkit.generateSprite(spriteType, true), pGreenBarSpriteId.toLong())
        }
        if (pRedBarSpriteId >= 0 && hitbarTypeList!!.spriteCache.get(pRedBarSpriteId.toLong()) == null && resource.isFileCached(
                pRedBarSpriteId
            )
        ) {
            spriteType = SpriteType.listSingle(resource, pRedBarSpriteId)!!
            hitbarTypeList!!.spriteCache.put(toolkit.generateSprite(spriteType, true), pRedBarSpriteId.toLong())
        }
    }

    fun getGreenBar(toolkit: AbstractToolkit): NativeSprite? {
        if (greenBarSpriteId < 0) {
            return null
        }
        var hitbarSprite = hitbarTypeList!!.spriteCache.get(greenBarSpriteId.toLong()) as NativeSprite?
        if (hitbarSprite == null) {
            generateSprites(toolkit)
            hitbarSprite = hitbarTypeList!!.spriteCache.get(greenBarSpriteId.toLong()) as NativeSprite?
        }
        return hitbarSprite
    }

    fun getRedBar(graphicalrenderer1: AbstractToolkit): NativeSprite? {
        if (redBarSpriteId < 0) {
            return null
        }
        var nativeSprite = hitbarTypeList!!.spriteCache.get(redBarSpriteId.toLong()) as NativeSprite?
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1)
            nativeSprite = hitbarTypeList!!.spriteCache.get(redBarSpriteId.toLong()) as NativeSprite?
        }
        return nativeSprite
    }

    fun getPGreenBar(toolkit: AbstractToolkit): NativeSprite? {
        if (pGreenBarSpriteId < 0) {
            return getGreenBar(toolkit)
        }
        var nativeSprite = hitbarTypeList!!.spriteCache.get(pGreenBarSpriteId.toLong()) as NativeSprite?
        if (nativeSprite == null) {
            generateSprites(toolkit)
            nativeSprite = hitbarTypeList!!.spriteCache.get(pGreenBarSpriteId.toLong()) as NativeSprite?
        }
        return nativeSprite
    }

    fun getPRedBar(toolkit: AbstractToolkit): NativeSprite? {
        if (pRedBarSpriteId < 0) {
            return getRedBar(toolkit)
        }
        var nativeSprite = hitbarTypeList!!.spriteCache.get(pRedBarSpriteId.toLong()) as NativeSprite?
        if (nativeSprite == null) {
            generateSprites(toolkit)
            nativeSprite = hitbarTypeList!!.spriteCache.get(pRedBarSpriteId.toLong()) as NativeSprite?
        }
        return nativeSprite
    }
}
