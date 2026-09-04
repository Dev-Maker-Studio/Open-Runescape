package com.jagex.game.runetek5.graphics.light

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpPerlinNoise
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils

/**
 * Flickering light effect for dynamic lighting.
 *
 * @author Brainpower
 */
class FlickeringEffect(
    toolkit: AbstractToolkit,
    sizeShift: Int,
    stream: Packet
) {

    @JvmField var level: Int
    @JvmField val type: Int
    @JvmField var pointLight: PointLight? = null
    @JvmField val growsUpwards: Boolean
    @JvmField val growsDownwards: Boolean
    @JvmField val ranges: ShortArray
    @JvmField val rotationOffset: Int
    @JvmField var surrounding: Int = 0
    @JvmField var effect: Int = 0
    @JvmField var ticker: Int = 0
    @JvmField var duration: Int = 0

    init {
        if (flickeringTexture == null) {
            createFlickeringTexture()
        }

        level = stream.readUByte()
        growsUpwards = (level and 0x8) != 0
        growsDownwards = (level and 0x10) != 0
        level = level and 0x7

        val positionX = stream.readUShort() shl 2
        val positionZ = stream.readUShort() shl 2
        val positionY = stream.readUShort() shl 2
        var size2d = stream.readUByte()
        val size3d = size2d * 2 + 1

        ranges = ShortArray(size3d)
        for (index in ranges.indices) {
            val bitpacked = stream.readUShort()
            var height = bitpacked ushr 8
            var lowestHeight = bitpacked and 0xff

            if (height >= size3d) {
                height = size3d - 1
            }
            if (lowestHeight > size3d - height) {
                lowestHeight = size3d - height
            }

            ranges[index] = (height shl 8 or lowestHeight).toShort()
        }

        size2d = (size2d shl sizeShift) + (1 shl sizeShift shr 1)
        val color = if (ColorUtil.hslToRgb != null) {
            ColorUtil.hslToRgb!![stream.readUShort()]
        } else {
            ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(stream.readUShort()).toInt() and 0xffff]
        }

        val bitpacked = stream.readUByte()
        type = bitpacked and 0x1f
        rotationOffset = (bitpacked and 0xe0) shl 3

        if (type != 31) {
            updateConstants()
        }

        generateLight(toolkit, positionX, positionY, positionZ, size2d, color)
    }

    fun generateLight(toolkit: AbstractToolkit, x: Int, y: Int, z: Int, size2d: Int, color: Int) {
        pointLight = toolkit.createPointLight(x, y, z, size2d, color)
    }

    fun updateConstants() {
        when (type) {
            2 -> {
                effect = 1
                surrounding = 0
                ticker = 2048
                duration = 2048
            }
            3 -> {
                effect = 1
                surrounding = 0
                ticker = 2048
                duration = 4096
            }
            4 -> {
                effect = 4
                surrounding = 0
                ticker = 2048
                duration = 2048
            }
            5 -> {
                effect = 4
                surrounding = 0
                ticker = 2048
                duration = 8192
            }
            6 -> {
                effect = 3
                surrounding = 1280
                ticker = 768
                duration = 2048
            }
            7 -> {
                effect = 3
                surrounding = 1280
                ticker = 768
                duration = 4096
            }
            8 -> {
                effect = 3
                surrounding = 1024
                ticker = 1024
                duration = 2048
            }
            9 -> {
                effect = 3
                surrounding = 1024
                ticker = 1024
                duration = 4096
            }
            10 -> {
                effect = 3
                surrounding = 1536
                ticker = 512
                duration = 2048
            }
            11 -> {
                effect = 3
                surrounding = 1536
                ticker = 512
                duration = 4096
            }
            12 -> {
                effect = 2
                surrounding = 0
                ticker = 2048
                duration = 2048
            }
            13 -> {
                effect = 2
                surrounding = 0
                ticker = 2048
                duration = 8192
            }
            14 -> {
                effect = 1
                surrounding = 1280
                ticker = 768
                duration = 2048
            }
            15 -> {
                effect = 1
                surrounding = 1536
                ticker = 512
                duration = 4096
            }
            16 -> {
                effect = 1
                surrounding = 1792
                ticker = 256
                duration = 8192
            }
            else -> {
                surrounding = 0
                effect = 0
                ticker = 2048
                duration = 2048
            }
        }
    }

    fun applyEffect(baseRotation: Int, disableFlickingEffects: Boolean) {
        val xan: Int
        if (!disableFlickingEffects) {
            val rotation = duration * baseRotation / 50 + rotationOffset and 0x7ff
            xan = when (effect) {
                1 -> (MathUtils.SINE[rotation shl 3] shr 4) + 1024
                2 -> rotation
                3 -> flickeringTexture!![rotation] shr 1
                4 -> rotation shr 10 shl 11
                5 -> (if (rotation < 1024) rotation else 2048 - rotation) shl 1
                else -> 2048
            }
        } else {
            xan = 2048
        }
        pointLight!!.setTransparency(((xan * ticker shr 11) + surrounding) / 2048.0f)
    }

    fun setType(effect: Int, duration: Int, ticker: Int, surrounding: Int) {
        this.effect = effect
        this.surrounding = surrounding
        this.ticker = ticker
        this.duration = duration
    }

    companion object {
        @JvmField var flickeringTexture: IntArray? = null

        @JvmStatic
        fun createFlickeringTexture() {
            flickeringTexture = TextureOpPerlinNoise.generateFlickeringEffect()
        }
    }
}
