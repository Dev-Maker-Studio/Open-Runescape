package com.jagex.game.runetek5.config.mec

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.vardomain.VarDomain

class MECType {

    @JvmField
    var mectypeId: Int = 0

    @JvmField
    var defaultIconArchive: Int = -1

    @JvmField
    var mouseOverIconArchive: Int = -1

    @JvmField
    var areaName: String? = null

    @JvmField
    var defaultTextColor: Int = 0

    @JvmField
    var mouseOverTextColor: Int = -1

    @JvmField
    var baseTextzoom: Int = 0

    @JvmField
    var displayedOnWorldmap: Boolean = true

    @JvmField
    var displayedOnMinimap: Boolean = false

    @JvmField
    var hasRandomisedMinimapPosition: Boolean = true

    @JvmField
    var offsets: IntArray? = null

    @JvmField
    var visible: Boolean = true

    @JvmField
    var minimenuName: String? = null

    @JvmField
    var lineColor: Int = 0

    @JvmField
    var colors: IntArray? = null

    @JvmField
    var categoryId: Int = -1

    @JvmField
    var colorPointers: ByteArray? = null

    @JvmField
    var outlineColor: Int = 0

    @JvmField
    var backgroundColor: Int = 0

    @JvmField
    var dashLineSpacing: Int = -1

    @JvmField
    var dashLineLength: Int = -1

    @JvmField
    var dashLineOffset: Int = -1

    @JvmField
    var labelOffsetX: Int = 0

    @JvmField
    var labelOffsetY: Int = 0

    @JvmField
    var minOffsetX: Int = Int.MAX_VALUE

    @JvmField
    var maxOffsetX: Int = Int.MIN_VALUE

    @JvmField
    var minOffsetY: Int = Int.MAX_VALUE

    @JvmField
    var maxOffsetY: Int = Int.MIN_VALUE

    @JvmField
    val minimenuActions: Array<String?> = arrayOfNulls(5)

    @JvmField
    var initialType: MECTypeList? = null

    @JvmField
    var primaryVarpbit: Int = -1

    @JvmField
    var primaryVarp: Int = -1

    @JvmField
    var primaryVariableMinValue: Int = 0

    @JvmField
    var primaryVariableMaxValue: Int = 0

    @JvmField
    var spriteId: Int = -1

    @JvmField
    var secondaryVarpbit: Int = -1

    @JvmField
    var secondaryVarp: Int = -1

    @JvmField
    var secondaryVariableMinValue: Int = 0

    @JvmField
    var secondaryVariableMaxValue: Int = 0

    @JvmField
    var parameters: IterableNodeMap<Node<*>>? = null

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decodeOpcode(buffer, opcode)
        }
    }

    fun decodeOpcode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> defaultIconArchive = buffer.readBigSmart()
            2 -> mouseOverIconArchive = buffer.readBigSmart()
            3 -> areaName = buffer.readString()
            4 -> defaultTextColor = buffer.readUMedium()
            5 -> mouseOverTextColor = buffer.readUMedium()
            6 -> baseTextzoom = buffer.readUByte()
            7 -> {
                val flags = buffer.readUByte()
                if (flags and 0x1 == 0) {
                    displayedOnWorldmap = false
                }
                if (flags and 0x2 == 2) {
                    displayedOnMinimap = true
                }
            }
            8 -> hasRandomisedMinimapPosition = buffer.readUByte() == 1
            9 -> {
                primaryVarpbit = buffer.readUShort()
                if (primaryVarpbit == 65535) {
                    primaryVarpbit = -1
                }
                primaryVarp = buffer.readUShort()
                if (primaryVarp == 65535) {
                    primaryVarp = -1
                }
                primaryVariableMinValue = buffer.readInt()
                primaryVariableMaxValue = buffer.readInt()
            }
            in 10..14 -> minimenuActions[opcode - 10] = buffer.readString()
            15 -> {
                val count = buffer.readUByte()
                offsets = IntArray(count * 2)
                for (index in 0 until count * 2) {
                    offsets!![index] = buffer.readShort()
                }
                lineColor = buffer.readInt()
                val numColors = buffer.readUByte()
                colors = IntArray(numColors)
                for (index in colors!!.indices) {
                    colors!![index] = buffer.readInt()
                }
                colorPointers = ByteArray(count)
                for (index in 0 until count) {
                    colorPointers!![index] = buffer.readByte()
                }
            }
            16 -> visible = false
            17 -> minimenuName = buffer.readString()
            18 -> spriteId = buffer.readBigSmart()
            19 -> categoryId = buffer.readUShort()
            20 -> {
                secondaryVarpbit = buffer.readUShort()
                if (secondaryVarpbit == 65535) {
                    secondaryVarpbit = -1
                }
                secondaryVarp = buffer.readUShort()
                if (secondaryVarp == 65535) {
                    secondaryVarp = -1
                }
                secondaryVariableMinValue = buffer.readInt()
                secondaryVariableMaxValue = buffer.readInt()
            }
            21 -> outlineColor = buffer.readInt()
            22 -> backgroundColor = buffer.readInt()
            23 -> {
                dashLineSpacing = buffer.readUByte()
                dashLineLength = buffer.readUByte()
                dashLineOffset = buffer.readUByte()
            }
            24 -> {
                labelOffsetX = buffer.readShort()
                labelOffsetY = buffer.readShort()
            }
            249 -> {
                val numParameters = buffer.readUByte()
                if (parameters == null) {
                    val capacity = MathUtils.nextPowerOfTwo(numParameters)
                    parameters = IterableNodeMap(capacity)
                }
                for (index in 0 until numParameters) {
                    val isString = buffer.readUByte() == 1
                    val key = buffer.readUMedium()
                    val value: Node<*> = if (isString) {
                        ObjectNode(buffer.readString())
                    } else {
                        IntNode(buffer.readInt())
                    }
                    parameters!!.put(value, key.toLong())
                }
            }
        }
    }

    fun postDecode() {
        offsets?.let {
            var index = 0
            while (index < it.size) {
                if (it[index] < minOffsetX) {
                    minOffsetX = it[index]
                } else if (it[index] > maxOffsetX) {
                    maxOffsetX = it[index]
                }
                if (it[index + 1] < minOffsetY) {
                    minOffsetY = it[index + 1]
                } else if (it[index + 1] > maxOffsetY) {
                    maxOffsetY = it[index + 1]
                }
                index += 2
            }
        }
    }

    fun shouldDraw(varProvider: VarDomain): Boolean {
        val primaryVariable: Int = if (primaryVarp != -1) {
            varProvider.getVarValue(primaryVarp)
        } else {
            if (primaryVarpbit == -1) {
                return true
            }
            varProvider.getVarBitValue(primaryVarpbit)
        }

        if (primaryVariable in primaryVariableMinValue..primaryVariableMaxValue) {
            val secondaryVariable: Int = if (secondaryVarp != -1) {
                varProvider.getVarValue(secondaryVarp)
            } else {
                if (secondaryVarpbit == -1) {
                    return true
                }
                varProvider.getVarBitValue(secondaryVarpbit)
            }
            return secondaryVariable in secondaryVariableMinValue..secondaryVariableMaxValue
        }
        return false
    }

    fun getStringParameter(key: Int, defaultValue: String?): String? {
        if (parameters == null) {
            return defaultValue
        }
        val value = parameters!!.get(key.toLong()) as? ObjectNode
        return if (value == null) defaultValue else value.value as String?
    }

    fun getIntParameter(key: Int, defaultValue: Int): Int {
        if (parameters == null) {
            return defaultValue
        }
        val value = parameters!!.get(key.toLong()) as? IntNode
        return value?.value ?: defaultValue
    }

    fun getSprite(toolkit: AbstractToolkit): NativeSprite? {
        val spriteKey = (spriteId.toLong() or 0x20000L or (toolkit.renderId.toLong() shl 29))
        var sprite = initialType!!.spriteCache.get(spriteKey) as NativeSprite?
        if (sprite == null) {
            initialType!!.spriteResource.isFileCached(spriteId)
            val spriteType = SpriteType.listSingle(initialType!!.spriteResource, spriteId, 0)
            if (spriteType != null) {
                sprite = toolkit.generateSprite(spriteType, true)
                initialType!!.spriteCache.put(sprite, spriteKey)
            }
        }
        return sprite
    }

    fun getIcon(toolkit: AbstractToolkit, isMouseOver: Boolean): NativeSprite? {
        val archiveId = if (isMouseOver) mouseOverIconArchive else defaultIconArchive
        val spriteReference = (archiveId or (toolkit.renderId shl 29)).toLong()
        var sprite = initialType!!.spriteCache.get(spriteReference) as NativeSprite?
        if (sprite != null) {
            return sprite
        }
        if (!initialType!!.spriteResource.isFileCached(archiveId)) {
            return null
        }
        val spriteType = SpriteType.listSingle(initialType!!.spriteResource, archiveId, 0)
        if (spriteType != null) {
            sprite = toolkit.generateSprite(spriteType, true)
            initialType!!.spriteCache.put(sprite, spriteReference)
        }
        return sprite
    }
}
