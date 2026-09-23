package com.jagex.game.runetek5.config.sequence.spot

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.UnknownOpcodes
import com.jagex.game.runetek5.config.sequence.Sequence
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.toolkit.AbstractToolkit

/**
 * Fully refactored
 * @author Brainpower - 21/05/2020
 *
 */
class SpotAnimType {

    @JvmField
    var gfxTypeList: SpotAnimTypeList? = null
    @JvmField
    var currentId: Int = 0
    @JvmField
    var sequenceId = -1
    @JvmField
    var replay = false
    @JvmField
    var modelId = 0
    @JvmField
    var scaleXZ = 128
    @JvmField
    var scaleY = 128
    @JvmField
    var rotation = 0
    @JvmField
    var brightness = 0
    @JvmField
    var contrast = 0
    @JvmField
    var contourType: Byte = 0
    @JvmField
    var contourModifier = -1
    @JvmField
    var recolorSrc: ShortArray? = null
    @JvmField
    var recolorDst: ShortArray? = null
    @JvmField
    var retextureSrc: ShortArray? = null
    @JvmField
    var retextureDst: ShortArray? = null

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
            1 -> modelId = stream.readBigSmart()
            2 -> sequenceId = stream.readBigSmart()
            4 -> scaleXZ = stream.readUShort()
            5 -> scaleY = stream.readUShort()
            6 -> rotation = stream.readUShort()
            7 -> brightness = stream.readUByte()
            8 -> contrast = stream.readUByte()
            9 -> {
                contourType = 3
                contourModifier = 8224
            }
            10 -> replay = true
            11 -> contourType = 1
            12 -> contourType = 4
            13 -> contourType = 5
            14 -> {
                contourType = 2
                contourModifier = stream.readUByte() * 256
            }
            15 -> {
                contourType = 3
                contourModifier = stream.readUShort()
            }
            16 -> {
                contourType = 3
                contourModifier = stream.readInt()
            }
            40 -> {
                val colorCount = stream.readUByte()
                recolorSrc = ShortArray(colorCount)
                recolorDst = ShortArray(colorCount)
                for (colorIndex in 0 until colorCount) {
                    recolorSrc!![colorIndex] = stream.readUShort().toShort()
                    recolorDst!![colorIndex] = stream.readUShort().toShort()
                }
            }
            41 -> {
                val textureCount = stream.readUByte()
                retextureSrc = ShortArray(textureCount)
                retextureDst = ShortArray(textureCount)
                for (textureIndex in 0 until textureCount) {
                    retextureSrc!![textureIndex] = stream.readUShort().toShort()
                    retextureDst!![textureIndex] = stream.readUShort().toShort()
                }
            }
            else -> UnknownOpcodes.record(opcode)
        }
    }

    fun isModelLoaded(): Boolean {
        val typeList = gfxTypeList ?: return false
        return modelId == -1 || typeList.modelResource.isCached(modelId, 0)
    }

    fun getAnimatedModel(
        toolkit: AbstractToolkit,
        initialFunctionMask: Int,
        sequence: Sequence?,
        copyType: Byte
    ): Model? {
        return getAnimatedModel(
            toolkit,
            initialFunctionMask,
            false,
            null,
            null,
            0,
            0,
            0,
            sequence,
            copyType
        )
    }

    fun getAnimatedModel(
        toolkit: AbstractToolkit,
        initialFunctionMask: Int,
        requiresAdjustment: Boolean,
        groundCurrentLvl: Ground?,
        groundNextLvl: Ground?,
        sceneX: Int,
        averageHeight: Int,
        sceneY: Int,
        sequence: Sequence?,
        copyType: Byte
    ): Model? {
        val typeList = gfxTypeList ?: return null
        var displacementFunctionMask = initialFunctionMask
        var adjustment = requiresAdjustment && contourType.toInt() != 0

        sequence?.let {
            displacementFunctionMask = initialFunctionMask or it.getFunctionMask()
        }

        if (adjustment) {
            displacementFunctionMask = displacementFunctionMask or
                if (contourType.toInt() == 3) ModelFunctionMask.FULL_DISPLACEMENT else ModelFunctionMask.DISPLACE_Y
        }

        if (scaleY != 128) {
            displacementFunctionMask = displacementFunctionMask or ModelFunctionMask.DISPLACE_Y
        }

        if (scaleXZ != 128 || rotation != 0) {
            displacementFunctionMask = displacementFunctionMask or ModelFunctionMask.DISPLACE_X or ModelFunctionMask.DISPLACE_Z
        }

        var cachedModel: Model?
        synchronized(typeList.modelCache) {
            currentId = currentId or (toolkit.renderId shl 29)
            cachedModel = typeList.modelCache[currentId.toLong()] as? Model
        }

        if (cachedModel == null || toolkit.mismatchingFunctionMask(cachedModel!!.getFunctionMask(), displacementFunctionMask) != 0) {
            if (cachedModel != null) {
                displacementFunctionMask = toolkit.mergeFunctionMasks(displacementFunctionMask, cachedModel!!.getFunctionMask())
            }

            var adjustmentFunctionMask = displacementFunctionMask
            if (recolorSrc != null) {
                adjustmentFunctionMask = displacementFunctionMask or ModelFunctionMask.RECOLOR
            }
            if (retextureSrc != null) {
                adjustmentFunctionMask = adjustmentFunctionMask or ModelFunctionMask.RETEXTURE
            }

            val mesh = Mesh.fromResource(typeList.modelResource, modelId) ?: return null

            if (mesh.version < 13) {
                mesh.upscale()
            }

            cachedModel = toolkit.createModel(
                mesh,
                adjustmentFunctionMask,
                typeList.preferenceMask,
                brightness + 64,
                contrast + 850
            )

            recolorSrc?.let { src ->
                recolorDst?.let { dst ->
                    for (index in src.indices) {
                        cachedModel!!.recolor(src[index], dst[index])
                    }
                }
            }

            retextureSrc?.let { src ->
                retextureDst?.let { dst ->
                    for (index in src.indices) {
                        cachedModel!!.rematerial(src[index], dst[index])
                    }
                }
            }

            cachedModel!!.applyShading(displacementFunctionMask)

            synchronized(typeList.modelCache) {
                currentId = currentId or (toolkit.renderId shl 29)
                typeList.modelCache.put(cachedModel, currentId.toLong())
            }
        }

        val model = cachedModel!!.copy(copyType, displacementFunctionMask, true)

        sequence?.animateRotatedModel(model!!, 0)

        if (scaleXZ != 128 || scaleY != 128) {
            model!!.scale(scaleXZ, scaleY, scaleXZ)
        }

        if (rotation != 0) {
            when (rotation) {
                90 -> model!!.yaxisRotateWithoutNormals(4096)
                180 -> model!!.yaxisRotateWithoutNormals(8192)
                270 -> model!!.yaxisRotateWithoutNormals(12288)
            }
        }

        if (adjustment) {
            model!!.contourToGround(
                contourType.toInt(),
                contourModifier,
                groundCurrentLvl,
                groundNextLvl,
                sceneX,
                averageHeight,
                sceneY
            )
        }

        model!!.applyShading(initialFunctionMask)
        return model
    }
}
