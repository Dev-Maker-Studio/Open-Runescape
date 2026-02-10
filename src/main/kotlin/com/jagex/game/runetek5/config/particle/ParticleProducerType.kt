package com.jagex.game.runetek5.config.particle

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.config.Resource

class ParticleProducerType {

    @JvmField
    var copiedParticleFileIds: IntArray? = null

    @JvmField
    var minimalAngleH: Short = 0

    @JvmField
    var maximalAngleH: Short = 0

    @JvmField
    var minimumAngleV: Short = 0

    @JvmField
    var maximalAngleV: Short = 0

    @JvmField
    var minimumSpeed: Int = 0

    @JvmField
    var maximumSpeed: Int = 0

    @JvmField
    var speedUpdateType: Int = 0

    @JvmField
    var speedFallOffStep: Int = 0

    @JvmField
    var maximumSize: Int = 0

    @JvmField
    var minimumSize: Int = 0

    @JvmField
    var minimumLifetime: Int = 0

    @JvmField
    var maximumLifetime: Int = 0

    @JvmField
    var minimumParticleRate: Int = 0

    @JvmField
    var maximumParticleRate: Int = 0

    @JvmField
    var particleFileIds2: IntArray? = null

    @JvmField
    var particleFileIds: IntArray? = null

    @JvmField
    var lowestDisplayPlane: Int = -2

    @JvmField
    var highestDisplayPlane: Int = -2

    @JvmField
    var updatesPerCycle: Int = 0

    @JvmField
    var materialId: Int = -1

    @JvmField
    var nonTexturedProducerId: Int = -1

    @JvmField
    var faceColorRgb: Int = 0

    @JvmField
    var activeFirst: Boolean = true

    @JvmField
    var emissionEndTime: Int = -1

    @JvmField
    var lifetime: Int = -1

    @JvmField
    var minimumSetting: Int = 0

    @JvmField
    var periodic: Boolean = true

    @JvmField
    var endSpeed: Int = -1

    @JvmField
    var uniformColorVariance: Boolean = true

    @JvmField
    var effectiveVertexUids: IntArray? = null

    @JvmField
    var aBool572: Boolean = true

    @JvmField
    var endSize: Int = -1

    @JvmField
    var isTextured: Boolean = false

    @JvmField
    var adjustsLightIntensity: Boolean = true

    @JvmField
    var killOverlapping: Boolean = false

    @JvmField
    var killAboveSurface: Boolean = true

    @JvmField
    var adjustsToScene: Boolean = false

    @JvmField
    var minimumStartR: Int = 0

    @JvmField
    var redVariance: Int = 0

    @JvmField
    var minimumStartG: Int = 0

    @JvmField
    var greenVariance: Int = 0

    @JvmField
    var minimumStartB: Int = 0

    @JvmField
    var blueVariance: Int = 0

    @JvmField
    var minimumStartA: Int = 0

    @JvmField
    var alphaVariance: Int = 0

    @JvmField
    var colorFadeStart: Int = 0

    @JvmField
    var alphaFadeStart: Int = 0

    @JvmField
    var redFadeStep: Int = 0

    @JvmField
    var greenFadeStep: Int = 0

    @JvmField
    var blueFadeStep: Int = 0

    @JvmField
    var startSpeedChange: Int = 0

    @JvmField
    var alphaFadeStep: Int = 0

    @JvmField
    var speedStep: Int = 0

    @JvmField
    var startSizeChange: Int = 0

    @JvmField
    var sizeChangeStep: Int = 0

    @JvmField
    var minimumStartColorRgb: Int = 0

    @JvmField
    var maximumStartColorRgb: Int = 0

    @JvmField
    var colorFading: Int = 100

    @JvmField
    var alphaFading: Int = 100

    @JvmField
    var speedChange: Int = 100

    @JvmField
    var sizeChange: Int = 100

    @JvmField
    var maximumStartR: Int = 0

    @JvmField
    var maximumStartG: Int = 0

    @JvmField
    var maximumStartB: Int = 0

    @JvmField
    var maximumStartA: Int = 0

    fun decode(buffer: Packet, opcode: Int) {
        when (opcode) {
            1 -> {
                minimalAngleH = buffer.readUShort().toShort()
                maximalAngleH = buffer.readUShort().toShort()
                minimumAngleV = buffer.readUShort().toShort()
                maximalAngleV = buffer.readUShort().toShort()
                val scale: Byte = 3
                minimalAngleH = (minimalAngleH.toInt() shl scale.toInt()).toShort()
                maximalAngleH = (maximalAngleH.toInt() shl scale.toInt()).toShort()
                minimumAngleV = (minimumAngleV.toInt() shl scale.toInt()).toShort()
                maximalAngleV = (maximalAngleV.toInt() shl scale.toInt()).toShort()
            }
            2 -> {
                buffer.readUByte()
            }
            3 -> {
                minimumSpeed = buffer.readInt()
                maximumSpeed = buffer.readInt()
            }
            4 -> {
                speedUpdateType = buffer.readUByte()
                speedFallOffStep = buffer.readByte().toInt()
            }
            5 -> {
                val size = buffer.readUShort() shl 12 shl 2
                minimumSize = size
                maximumSize = size
            }
            6 -> {
                minimumStartColorRgb = buffer.readInt()
                maximumStartColorRgb = buffer.readInt()
            }
            7 -> {
                minimumLifetime = buffer.readUShort()
                maximumLifetime = buffer.readUShort()
            }
            8 -> {
                minimumParticleRate = buffer.readUShort()
                maximumParticleRate = buffer.readUShort()
            }
            9 -> {
                val count = buffer.readUByte()
                particleFileIds2 = IntArray(count)
                for (i_5 in 0 until count) {
                    particleFileIds2!![i_5] = buffer.readUShort()
                }
            }
            10 -> {
                val count = buffer.readUByte()
                particleFileIds = IntArray(count)
                for (i_5 in 0 until count) {
                    particleFileIds!![i_5] = buffer.readUShort()
                }
            }
            12 -> {
                lowestDisplayPlane = buffer.readByte().toInt()
            }
            13 -> {
                highestDisplayPlane = buffer.readByte().toInt()
            }
            14 -> {
                updatesPerCycle = buffer.readUShort()
            }
            15 -> {
                materialId = buffer.readUShort()
            }
            16 -> {
                activeFirst = buffer.readUByte() == 1
                emissionEndTime = buffer.readUShort()
                lifetime = buffer.readUShort()
                periodic = buffer.readUByte() == 1
            }
            17 -> {
                nonTexturedProducerId = buffer.readUShort()
            }
            18 -> {
                faceColorRgb = buffer.readInt()
            }
            19 -> {
                minimumSetting = buffer.readUByte()
            }
            20 -> {
                colorFading = buffer.readUByte()
            }
            21 -> {
                alphaFading = buffer.readUByte()
            }
            22 -> {
                endSpeed = buffer.readInt()
            }
            23 -> {
                speedChange = buffer.readUByte()
            }
            24 -> {
                uniformColorVariance = false
            }
            25 -> {
                val count = buffer.readUByte()
                effectiveVertexUids = IntArray(count)
                for (i_5 in 0 until count) {
                    effectiveVertexUids!![i_5] = buffer.readUShort()
                }
            }
            26 -> {
                aBool572 = false
            }
            27 -> {
                endSize = buffer.readUShort() shl 12 shl 2
            }
            28 -> {
                sizeChange = buffer.readUByte()
            }
            29 -> {
                buffer.readShort()
            }
            30 -> {
                isTextured = true
            }
            31 -> {
                minimumSize = buffer.readUShort() shl 12 shl 2
                maximumSize = buffer.readUShort() shl 12 shl 2
            }
            32 -> {
                adjustsLightIntensity = false
            }
            33 -> {
                killOverlapping = true
            }
            34 -> {
                killAboveSurface = false
            }
        }
    }

    fun build() {
        if (lowestDisplayPlane > -2 || highestDisplayPlane > -2) {
            adjustsToScene = true
        }
        minimumStartR = minimumStartColorRgb shr 16 and 0xff
        maximumStartR = maximumStartColorRgb shr 16 and 0xff
        redVariance = maximumStartR - minimumStartR
        minimumStartG = minimumStartColorRgb shr 8 and 0xff
        maximumStartG = maximumStartColorRgb shr 8 and 0xff
        greenVariance = maximumStartG - minimumStartG
        minimumStartB = minimumStartColorRgb and 0xff
        maximumStartB = maximumStartColorRgb and 0xff
        blueVariance = maximumStartB - minimumStartB
        minimumStartA = minimumStartColorRgb shr 24 and 0xff
        maximumStartA = maximumStartColorRgb shr 24 and 0xff
        alphaVariance = maximumStartA - minimumStartA
        if (faceColorRgb != 0) {
            colorFadeStart = colorFading * maximumLifetime / 100
            alphaFadeStart = alphaFading * maximumLifetime / 100
            if (colorFadeStart == 0) {
                colorFadeStart = 1
            }
            redFadeStep = ((faceColorRgb shr 16 and 0xff) - (redVariance / 2 + minimumStartR) shl 8) / colorFadeStart
            greenFadeStep = ((faceColorRgb shr 8 and 0xff) - (greenVariance / 2 + minimumStartG) shl 8) / colorFadeStart
            blueFadeStep = ((faceColorRgb and 0xff) - (blueVariance / 2 + minimumStartB) shl 8) / colorFadeStart
            if (alphaFadeStart == 0) {
                alphaFadeStart = 1
            }
            alphaFadeStep = ((faceColorRgb shr 24 and 0xff) - (alphaVariance / 2 + minimumStartA) shl 8) / alphaFadeStart
            redFadeStep += if (redFadeStep > 0) -4 else 4
            greenFadeStep += if (greenFadeStep > 0) -4 else 4
            blueFadeStep += if (blueFadeStep > 0) -4 else 4
            alphaFadeStep += if (alphaFadeStep > 0) -4 else 4
        }
        if (endSpeed != -1) {
            startSpeedChange = maximumLifetime * speedChange / 100
            if (startSpeedChange == 0) {
                startSpeedChange = 1
            }
            speedStep = (endSpeed - ((maximumSpeed - minimumSpeed) / 2 + minimumSpeed)) / startSpeedChange
        }
        if (endSize != -1) {
            startSizeChange = sizeChange * maximumLifetime / 100
            if (startSizeChange == 0) {
                startSizeChange = 1
            }
            sizeChangeStep = (endSize - ((maximumSize - minimumSize) / 2 + minimumSize)) / startSizeChange
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

    companion object {
        @JvmField
        val particleProducers = LRUCache(64)

        @JvmStatic
        fun list(id: Int): ParticleProducerType {
            var type = particleProducers.get(id.toLong()) as ParticleProducerType?
            if (type == null) {
                val producerData = Resource.PARTICLE_PRODUCER!!.getFile(0, id)
                type = ParticleProducerType()
                if (producerData != null) {
                    type.decode(Packet(producerData))
                }

                type.build()
                particleProducers.put(type, id.toLong())
            }
            return type
        }

        @JvmStatic
        fun resetCache() {
            particleProducers.reset()
        }
    }
}
