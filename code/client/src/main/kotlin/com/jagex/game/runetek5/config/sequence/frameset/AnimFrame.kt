package com.jagex.game.runetek5.config.sequence.frameset

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.graphics.model.ModelFunctionMask

class AnimFrame(data: ByteArray, @JvmField val base: AnimBase) {

    @JvmField var numTransformations: Int = 0
    @JvmField var modifiesAlpha: Boolean = false
    @JvmField var modifiesColor: Boolean = false
    @JvmField var modifiesBillboard: Boolean = false
    @JvmField var baseIndices: ShortArray? = null
    @JvmField var transformationX: ShortArray? = null
    @JvmField var transformationY: ShortArray? = null
    @JvmField var transformationZ: ShortArray? = null
    @JvmField var labels: ShortArray? = null
    @JvmField var tweeningProperties: ByteArray? = null

    init {
        try {
            val attribBuffer = Packet(data)
            val transformationBuffer = Packet(data)
            attribBuffer.readUByte()
            attribBuffer.currentPosition += 2
            val count = attribBuffer.readUByte()
            var used = 0
            var last = -1
            var lastUsed = -1
            transformationBuffer.currentPosition = count + attribBuffer.currentPosition
            var index: Int
            for (i in 0 until count) {
                val type = base.transformationType[i]
                if (type == 0) {
                    last = i
                }
                val attribute = attribBuffer.readUByte()
                if (attribute > 0) {
                    if (type == 0) {
                        lastUsed = i
                    }
                    INDICES[used] = i.toShort()
                    var value: Short = 0
                    if (type == 3 || type == 10) {
                        value = 128
                    }

                    if (attribute and ModelFunctionMask.DISPLACE_X != 0) {
                        TRANSFORM_X[used] = transformationBuffer.readSmart().toShort()
                    } else {
                        TRANSFORM_X[used] = value
                    }

                    if (attribute and ModelFunctionMask.DISPLACE_Y != 0) {
                        TRANSFORM_Y[used] = transformationBuffer.readSmart().toShort()
                    } else {
                        TRANSFORM_Y[used] = value
                    }

                    if (attribute and ModelFunctionMask.DISPLACE_Z != 0) {
                        TRANSFORM_Z[used] = transformationBuffer.readSmart().toShort()
                    } else {
                        TRANSFORM_Z[used] = value
                    }

                    FLAGS[used] = (attribute ushr 3 and 0x3).toByte()
                    if (type == 2 || type == 9) {
                        TRANSFORM_X[used] = (TRANSFORM_X[used].toInt() shl 2 and 0x3fff).toShort()
                        TRANSFORM_Y[used] = (TRANSFORM_Y[used].toInt() shl 2 and 0x3fff).toShort()
                        TRANSFORM_Z[used] = (TRANSFORM_Z[used].toInt() shl 2 and 0x3fff).toShort()
                    }

                    SKIPPED[used] = -1
                    if (type != 1 && type != 2 && type != 3) {
                        if (type == 5) {
                            modifiesAlpha = true
                        } else if (type == 7) {
                            modifiesColor = true
                        } else if (type == 9 || type == 10 || type == 8) {
                            modifiesBillboard = true
                        }
                    } else if (last > lastUsed) {
                        SKIPPED[used] = last.toShort()
                        lastUsed = last
                    }
                    used++
                }
            }

            if (data.size != transformationBuffer.currentPosition) {
                throw RuntimeException()
            }

            numTransformations = used
            baseIndices = ShortArray(used)
            transformationX = ShortArray(used)
            transformationY = ShortArray(used)
            transformationZ = ShortArray(used)
            labels = ShortArray(used)
            tweeningProperties = ByteArray(used)

            for (i in 0 until used) {
                baseIndices!![i] = INDICES[i]
                transformationX!![i] = TRANSFORM_X[i]
                transformationY!![i] = TRANSFORM_Y[i]
                transformationZ!![i] = TRANSFORM_Z[i]
                labels!![i] = SKIPPED[i]
                tweeningProperties!![i] = FLAGS[i]
            }
        } catch (ex: Exception) {
            numTransformations = 0
            modifiesAlpha = false
            modifiesColor = false
        }
    }

    companion object {
        private val INDICES = ShortArray(500)
        private val TRANSFORM_X = ShortArray(500)
        private val TRANSFORM_Y = ShortArray(500)
        private val TRANSFORM_Z = ShortArray(500)
        private val SKIPPED = ShortArray(500)
        private val FLAGS = ByteArray(500)
    }
}
