package com.jagex.game.runetek5.config.material.texture

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.texture.operation.*
import com.jagex.game.runetek5.graphics.light.Skybox
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import kotlin.math.min
import kotlin.math.pow

/**
 * @author Brainpower - 02/06/2020
 * @author OpenOSRS - All operation names
 *
 */
object TextureTypeList {

    @JvmField
    val BRIGHTNESS_TABLE = IntArray(256)
    const val TEXTURE_GROUP_ID = -1

    @JvmField
    var modelResource: Js5ResourceProvider? = null
    @JvmField
    var currentBrightness = -1.0

    @JvmField
    var textureSpriteResource: Js5ResourceProvider? = null
    @JvmField
    var materialProvider: MaterialProvider? = null

    @JvmStatic
    fun initialize(modelResource: Js5ResourceProvider, materialProvider: MaterialProvider) {
        TextureTypeList.modelResource = modelResource
        Skybox.materialProvider = materialProvider
    }

    @JvmStatic
    fun listOperation(stream: Packet): TextureOperation {
        stream.readUByte()
        val implementationType = stream.readUByte()
        val textureNode = implementationForType(implementationType)
        textureNode.imageCacheCapacity = stream.readUByte()
        val opcodeCount = stream.readUByte()
        for (index in 0 until opcodeCount) {
            val opcode = stream.readUByte()
            textureNode.decodeOpcode(opcode, stream)
        }
        textureNode.postDecode()
        return textureNode
    }

    @JvmStatic
    fun setBrightness(brightness: Double) {
        if (brightness != currentBrightness) {
            for (grade in 0..255) {
                val value = ((grade / 255.0).pow(brightness) * 255.0).toInt()
                BRIGHTNESS_TABLE[grade] = min(255, value)
            }
            currentBrightness = brightness
        }
    }

    @JvmStatic
    fun implementationForType(type: Int): TextureOperation {
        return when (type) {
            0 -> TextureOpMonochromeFill()
            1 -> TextureOpColorFill()
            2 -> TextureOpHorizontalGradient()
            3 -> TextureOpVerticalGradient()
            4 -> TextureOpBricks()
            5 -> TextureOpBoxBlur()
            6 -> TextureOpClamp()
            7 -> TextureOpCombine()
            8 -> TextureOpCurve()
            9 -> TextureOpFlip()
            10 -> TextureOpColorGradient()
            11 -> TextureOpColorize()
            12 -> TextureOpWaveform()
            13 -> TextureOpNoise()
            14 -> TextureOpWeave()
            15 -> TextureOpVoronoiNoise()
            16 -> TextureOpHerringbone()
            17 -> TextureOpHslAdjust()
            18 -> TextureOpTiledSprite()
            19 -> TextureOpPolarDistortion()
            20 -> TextureOpTile()
            21 -> TextureOpInterpolate()
            22 -> TextureOpInvert()
            23 -> TextureOpKaleidoscope()
            24 -> TextureOpMonochrome()
            25 -> TextureOpBrightness()
            26 -> TextureOpBinary()
            27 -> TextureOpSquareWaveform()
            28 -> TextureOpIrregularBricks()
            29 -> TextureOpRasterizer()
            30 -> TextureOpRange()
            31 -> TextureOpMandelbrot()
            32 -> TextureOpEmboss()
            33 -> TextureOpColorEdgeDetector()
            34 -> TextureOpPerlinNoise()
            35 -> TextureOpMonochromeEdgeDetector()
            36 -> TextureOpTexture()
            37 -> TextureOp37()
            38 -> TextureOpLineNoise()
            39 -> TextureOpSprite()
            else -> throw IllegalArgumentException("Unknown texture operation type: $type")
        }
    }
}
