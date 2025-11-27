package com.jagex.game.runetek5.config.hitsplat

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.TextUtils

// HIT
// MISS (0)
// CRITICAL
// ...?
class HitSplatType {

    @JvmField var fontId = -1
    @JvmField var color = 16777215
    @JvmField var hasColor = false
    @JvmField var anInt2846 = 0
    @JvmField var cyclesVisible = 70
    @JvmField var anInt2833 = 0
    @JvmField var anInt2847 = -1
    @JvmField var displayType = -1
    @JvmField var anInt2832 = 0
    @JvmField var typelist: HitSplatTypeList? = null
    @JvmField var spriteA = -1
    @JvmField var spriteB = -1
    @JvmField var spriteC = -1
    @JvmField var spriteD = -1
    @JvmField var placementExampleString = ""

    fun decode(stream: Packet, opcode: Int) {
        when (opcode) {
            1 -> fontId = stream.readBigSmart()
            2 -> {
                color = stream.readUMedium()
                hasColor = true
            }
            3 -> spriteA = stream.readBigSmart()
            4 -> spriteB = stream.readBigSmart()
            5 -> spriteC = stream.readBigSmart()
            6 -> spriteD = stream.readBigSmart()
            7 -> anInt2846 = stream.readShort()
            8 -> placementExampleString = stream.readJagString()
            9 -> cyclesVisible = stream.readUShort()
            10 -> anInt2833 = stream.readShort()
            11 -> anInt2847 = 0
            12 -> displayType = stream.readUByte()
            13 -> anInt2832 = stream.readShort()
            14 -> anInt2847 = stream.readUShort()
        }
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    fun generateSprites(toolkit: AbstractToolkit) {
        val hitsplatSpriteResource: Js5ResourceProvider = typelist!!.spriteResource
        var spriteType: SpriteType
        if (spriteA >= 0 && typelist!!.spriteCache.get(spriteA.toLong()) == null && hitsplatSpriteResource.isFileCached(spriteA)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, spriteA)
            typelist!!.spriteCache.put(toolkit.generateSprite(spriteType, true), spriteA.toLong())
        }

        if (spriteC >= 0 && typelist!!.spriteCache.get(spriteC.toLong()) == null && hitsplatSpriteResource.isFileCached(spriteC)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, spriteC)
            typelist!!.spriteCache.put(toolkit.generateSprite(spriteType, true), spriteC.toLong())
        }

        if (spriteB >= 0 && typelist!!.spriteCache.get(spriteB.toLong()) == null && hitsplatSpriteResource.isFileCached(spriteB)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, spriteB)
            typelist!!.spriteCache.put(toolkit.generateSprite(spriteType, true), spriteB.toLong())
        }

        if (spriteD >= 0 && typelist!!.spriteCache.get(spriteD.toLong()) == null && hitsplatSpriteResource.isFileCached(spriteD)) {
            spriteType = SpriteType.listSingle(hitsplatSpriteResource, spriteD)
            typelist!!.spriteCache.put(toolkit.generateSprite(spriteType, true), spriteD.toLong())
        }
    }

    fun getCenteredText(hit: Int): String {
        var placement = placementExampleString
        while (true) {
            val numberSlot = placement.indexOf("%1")
            if (numberSlot < 0) {
                return placement
            }
            placement = placement.substring(0, numberSlot) + TextUtils.createNumberCenteredWithinText(hit, false) + placement.substring(numberSlot + 2)
        }
    }

    fun method3835(graphicalrenderer1: AbstractToolkit): NativeSprite? {
        if (spriteA < 0) {
            return null
        }
        var nativeSprite = typelist!!.spriteCache.get(spriteA.toLong()) as NativeSprite?
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1)
            nativeSprite = typelist!!.spriteCache.get(spriteA.toLong()) as NativeSprite?
        }
        return nativeSprite
    }

    fun method3832(graphicalrenderer1: AbstractToolkit): NativeSprite? {
        if (spriteB < 0) {
            return null
        }
        var hitsplatSprite = typelist!!.spriteCache.get(spriteB.toLong()) as NativeSprite?
        if (hitsplatSprite == null) {
            generateSprites(graphicalrenderer1)
            hitsplatSprite = typelist!!.spriteCache.get(spriteB.toLong()) as NativeSprite?
        }
        return hitsplatSprite
    }

    fun method3836(graphicalrenderer1: AbstractToolkit): NativeSprite? {
        if (spriteC < 0) {
            return null
        }
        var nativeSprite = typelist!!.spriteCache.get(spriteC.toLong()) as NativeSprite?
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1)
            nativeSprite = typelist!!.spriteCache.get(spriteC.toLong()) as NativeSprite?
        }
        return nativeSprite
    }

    fun method3838(graphicalrenderer1: AbstractToolkit): NativeSprite? {
        if (spriteD < 0) {
            return null
        }
        var nativeSprite = typelist!!.spriteCache.get(spriteD.toLong()) as NativeSprite?
        if (nativeSprite == null) {
            generateSprites(graphicalrenderer1)
            nativeSprite = typelist!!.spriteCache.get(spriteD.toLong()) as NativeSprite?
        }
        return nativeSprite
    }
}
