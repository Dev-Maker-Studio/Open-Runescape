package com.jagex.game.runetek5.graphics.light

import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.HashTable
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ShortNode
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.utils.MathUtils
import kotlin.math.sqrt

class LightGL(
    val toolkit: OpenGlToolkit,
    val ground: GroundGL,
    val pointLight: PointLight,
    tileMask: IntArray
) : Node<LightGL>() {
    var vertexCount: Int = 0
    val minTileX: Int
    val maxTileX: Int
    val minTileZ: Int
    val maxTileZ: Int
    var tileNormalsX: Array<FloatArray>?
    var tileNormalsY: Array<FloatArray>?
    var tileNormalsZ: Array<FloatArray>?
    val indexBuffer: ElementsBufferGL?
    val vertexBuffer: ArrayBufferGL?
    val positionDrawBuffer: OpenGlArrayDrawBuffer?
    val colorDrawBuffer: OpenGlArrayDrawBuffer?
    var indexPacket: Packet? = null
    var bitBuffer: GlBitBuffer? = null
    var vertexHashTable: HashTable? = null
    var totalIndexCount: Int = 0

    init {
        val lightRadius = this.pointLight.getSize2d() - (ground.tileUnits shr 1)
        this.minTileX = this.pointLight.getX() - lightRadius shr ground.tileScale
        this.maxTileX = this.pointLight.getX() + lightRadius shr ground.tileScale
        this.minTileZ = this.pointLight.getZ() - lightRadius shr ground.tileScale
        this.maxTileZ = this.pointLight.getZ() + lightRadius shr ground.tileScale
        val tileRangeX = this.maxTileX - this.minTileX + 1
        val tileRangeZ = this.maxTileZ - this.minTileZ + 1
        this.tileNormalsX = Array(tileRangeX + 1) { FloatArray(tileRangeZ + 1) }
        this.tileNormalsY = Array(tileRangeX + 1) { FloatArray(tileRangeZ + 1) }
        this.tileNormalsZ = Array(tileRangeX + 1) { FloatArray(tileRangeZ + 1) }

        var tileZ: Int
        var tileAbsZ: Int
        var tileX: Int
        var tileAbsX: Int
        var heightDeltaX: Int
        var heightDeltaZ: Int
        tileZ = 0
        while (tileZ <= tileRangeZ) {
            tileAbsZ = tileZ + this.minTileZ
            if (tileAbsZ > 0 && tileAbsZ < this.ground.length - 1) {
                tileX = 0
                while (tileX <= tileRangeX) {
                    tileAbsX = tileX + this.minTileX
                    if (tileAbsX > 0 && tileAbsX < this.ground.width - 1) {
                        heightDeltaX = ground.getHeight(tileAbsX + 1, tileAbsZ) - ground.getHeight(tileAbsX - 1, tileAbsZ)
                        heightDeltaZ = ground.getHeight(tileAbsX, tileAbsZ + 1) - ground.getHeight(tileAbsX, tileAbsZ - 1)
                        val invMagnitude = (1.0 / sqrt((heightDeltaX * heightDeltaX + heightDeltaZ * heightDeltaZ + 65536).toDouble())).toFloat()
                        this.tileNormalsX!![tileX][tileZ] = heightDeltaX * invMagnitude
                        this.tileNormalsY!![tileX][tileZ] = -256.0f * invMagnitude
                        this.tileNormalsZ!![tileX][tileZ] = heightDeltaZ * invMagnitude
                    }
                    tileX++
                }
            }
            tileZ++
        }

        tileZ = 0

        tileAbsZ = this.minTileZ
        while (tileAbsZ <= this.maxTileZ) {
            if (tileAbsZ >= 0 && tileAbsZ < ground.length) {
                tileX = this.minTileX
                while (tileX <= this.maxTileX) {
                    if (tileX >= 0 && tileX < ground.width) {
                        tileAbsX = tileMask[tileZ]
                        val tileColors = ground.tileColors!![tileX][tileAbsZ]
                        if (tileColors != null && tileAbsX != 0) {
                            if (tileAbsX == 1) {
                                heightDeltaZ = 0

                                while (heightDeltaZ < tileColors.size) {
                                    if (tileColors[heightDeltaZ++] != -1 && tileColors[heightDeltaZ++] != -1 && tileColors[heightDeltaZ++] != -1) {
                                        this.totalIndexCount += 3
                                    }
                                }
                            } else {
                                this.totalIndexCount += 3
                            }
                        }
                    }

                    ++tileZ
                    tileX++
                }
            } else {
                tileZ += this.maxTileX - this.minTileX
            }
            tileAbsZ++
        }

        if (this.totalIndexCount > 0) {
            this.indexPacket = Packet(this.totalIndexCount * 2)
            this.bitBuffer = GlBitBuffer(this.totalIndexCount * 16)
            this.vertexHashTable = HashTable(MathUtils.nextPowerOfTwo(this.totalIndexCount))
            tileAbsZ = 0
            tileZ = 0

            tileX = this.minTileZ
            while (tileX <= this.maxTileZ) {
                if (tileX >= 0 && tileX < ground.length) {
                    tileAbsX = 0

                    heightDeltaX = this.minTileX
                    while (heightDeltaX <= this.maxTileX) {
                        if (heightDeltaX >= 0 && heightDeltaX < ground.width) {
                            heightDeltaZ = tileMask[tileZ]
                            val tileColors = ground.tileColors!![heightDeltaX][tileX]
                            if (tileColors != null && heightDeltaZ != 0) {
                                if (heightDeltaZ != 1) {
                                    if (heightDeltaZ == 3) {
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, 0, 0)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, ground.tileUnits, 0)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, 0, ground.tileUnits)
                                    } else if (heightDeltaZ == 2) {
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, ground.tileUnits, 0)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, ground.tileUnits, ground.tileUnits)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, 0, 0)
                                    } else if (heightDeltaZ == 5) {
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, ground.tileUnits, ground.tileUnits)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, 0, ground.tileUnits)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, ground.tileUnits, 0)
                                    } else if (heightDeltaZ == 4) {
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, 0, ground.tileUnits)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, 0, 0)
                                        addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, ground.tileUnits, ground.tileUnits)
                                    }
                                } else {
                                    val tileOffsetsX = ground.tileOffsetsX!![heightDeltaX][tileX]
                                    val tileOffsetsZ = ground.tileOffsetsZ!![heightDeltaX][tileX]
                                    var colorIndex = 0

                                    label108@ while (true) {
                                        while (true) {
                                            if (colorIndex >= tileColors.size) {
                                                break@label108
                                            }

                                            if (tileColors[colorIndex] != -1 && tileColors[colorIndex + 1] != -1 && tileColors[colorIndex + 2] != -1) {
                                                addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, tileOffsetsX!![colorIndex], tileOffsetsZ!![colorIndex])
                                                ++colorIndex
                                                addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, tileOffsetsX!![colorIndex], tileOffsetsZ!![colorIndex])
                                                ++colorIndex
                                                addVertex(tileAbsX, tileAbsZ, heightDeltaX, tileX, tileOffsetsX!![colorIndex], tileOffsetsZ!![colorIndex])
                                                ++colorIndex
                                            } else {
                                                colorIndex += 3
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ++tileZ
                        ++tileAbsX
                        heightDeltaX++
                    }
                } else {
                    tileZ += this.maxTileX - this.minTileX
                }

                ++tileAbsZ
                tileX++
            }

            this.indexBuffer = this.toolkit.createElementsArrayBuffer(this.indexPacket!!.buffer, this.indexPacket!!.currentPosition, false)
            this.vertexBuffer = this.toolkit.createArrayBuffer(16, this.bitBuffer!!.buffer, this.bitBuffer!!.currentPosition, false)
            this.positionDrawBuffer = OpenGlArrayDrawBuffer(this.vertexBuffer, 5126, 3, 0)
            this.colorDrawBuffer = OpenGlArrayDrawBuffer(this.vertexBuffer, 5121, 4, 12)
        } else {
            this.indexBuffer = null
            this.vertexBuffer = null
            this.positionDrawBuffer = null
            this.colorDrawBuffer = null
        }

        this.bitBuffer = null
        this.indexPacket = null
        this.vertexHashTable = null
        this.tileNormalsZ = null
        this.tileNormalsY = null
        this.tileNormalsX = null
    }

    fun writeIndex(index: Short) {
        if (this.toolkit.bigEndianess) {
            this.indexPacket!!.writeShort(index.toInt())
        } else {
            this.indexPacket!!.writeShortLittle(index.toInt())
        }
    }

    fun addVertex(localTileX: Int, localTileZ: Int, tileX: Int, tileZ: Int, offsetX: Int, offsetZ: Int) {
        var hashKey = -1L
        val sceneX = offsetX + (tileX shl this.ground.tileScale)
        val sceneZ = (tileZ shl this.ground.tileScale) + offsetZ
        val sceneY = this.ground.getAverageHeight(sceneX, sceneZ)
        if ((offsetX and 0x7f) == 0 || (offsetZ and 0x7f) == 0) {
            hashKey = (sceneZ.toLong() and 0xffffL) shl 16 or (sceneX.toLong() and 0xffffL)
            val existingNode = this.vertexHashTable!!.get(hashKey)
            if (existingNode != null) {
                writeIndex((existingNode as ShortNode).value)
                return
            }
        }

        val vertexIndex = (this.vertexCount++).toShort()
        if (hashKey != -1L) {
            this.vertexHashTable!!.put(ShortNode(vertexIndex), hashKey)
        }

        val normalX: Float
        val normalY: Float
        val normalZ: Float
        val interpFracX: Float
        val interpFracZ: Float
        val normalTopLeftX: Float
        val normalTopLeftY: Float
        val normalTopLeftZ: Float
        val normalTopRightX: Float
        val normalTopRightY: Float
        val normalTopRightZ: Float
        when {
            offsetX == 0 && offsetZ == 0 -> {
                normalX = this.tileNormalsX!![localTileX][localTileZ]
                normalY = this.tileNormalsY!![localTileX][localTileZ]
                normalZ = this.tileNormalsZ!![localTileX][localTileZ]
            }
            offsetX == this.ground.tileUnits && offsetZ == 0 -> {
                normalX = this.tileNormalsX!![localTileX + 1][localTileZ]
                normalY = this.tileNormalsY!![localTileX + 1][localTileZ]
                normalZ = this.tileNormalsZ!![localTileX + 1][localTileZ]
            }
            offsetX == this.ground.tileUnits && this.ground.tileUnits == offsetZ -> {
                normalX = this.tileNormalsX!![localTileX + 1][localTileZ + 1]
                normalY = this.tileNormalsY!![localTileX + 1][localTileZ + 1]
                normalZ = this.tileNormalsZ!![localTileX + 1][localTileZ + 1]
            }
            offsetX == 0 && this.ground.tileUnits == offsetZ -> {
                normalX = this.tileNormalsX!![localTileX][localTileZ + 1]
                normalY = this.tileNormalsY!![localTileX][localTileZ + 1]
                normalZ = this.tileNormalsZ!![localTileX][localTileZ + 1]
            }
            else -> {
                interpFracX = offsetX.toFloat() / this.ground.tileUnits
                interpFracZ = offsetZ.toFloat() / this.ground.tileUnits
                var normalTopLeftXVar = this.tileNormalsX!![localTileX][localTileZ]
                var normalTopLeftYVar = this.tileNormalsY!![localTileX][localTileZ]
                var normalTopLeftZVar = this.tileNormalsZ!![localTileX][localTileZ]
                var normalTopRightXVar = this.tileNormalsX!![localTileX + 1][localTileZ]
                var normalTopRightYVar = this.tileNormalsY!![localTileX + 1][localTileZ]
                var normalTopRightZVar = this.tileNormalsZ!![localTileX + 1][localTileZ]
                normalTopLeftXVar += (this.tileNormalsX!![localTileX][localTileZ + 1] - normalTopLeftXVar) * interpFracX
                normalTopLeftYVar += (this.tileNormalsY!![localTileX][localTileZ + 1] - normalTopLeftYVar) * interpFracX
                normalTopLeftZVar += (this.tileNormalsZ!![localTileX][localTileZ + 1] - normalTopLeftZVar) * interpFracX
                normalTopRightXVar += (this.tileNormalsX!![localTileX + 1][localTileZ + 1] - normalTopRightXVar) * interpFracX
                normalTopRightYVar += (this.tileNormalsY!![localTileX + 1][localTileZ + 1] - normalTopRightYVar) * interpFracX
                normalTopRightZVar += (this.tileNormalsZ!![localTileX + 1][localTileZ + 1] - normalTopRightZVar) * interpFracX
                normalX = normalTopLeftXVar + (normalTopRightXVar - normalTopLeftXVar) * interpFracZ
                normalY = normalTopLeftYVar + (normalTopRightYVar - normalTopLeftYVar) * interpFracZ
                normalZ = normalTopLeftZVar + (normalTopRightZVar - normalTopLeftZVar) * interpFracZ
            }
        }

        var deltaX = (this.pointLight.getX() - sceneX).toFloat()
        var deltaY = (this.pointLight.getY() - sceneY).toFloat()
        var deltaZ = (this.pointLight.getZ() - sceneZ).toFloat()
        val distance = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
        val invDistance = 1.0F / distance
        deltaX *= invDistance
        deltaY *= invDistance
        deltaZ *= invDistance
        val attenuation = distance / this.pointLight.getSize2d()
        var intensity = 1.0F - attenuation * attenuation
        if (intensity < 0.0F) {
            intensity = 0.0F
        }

        var dotProduct = deltaX * normalX + deltaY * normalY + deltaZ * normalZ
        if (dotProduct < 0.0F) {
            dotProduct = 0.0F
        }

        var lightFactor = dotProduct * intensity * 2.0F
        if (lightFactor > 1.0F) {
            lightFactor = 1.0F
        }

        val lightColor = this.pointLight.getColor()
        var red = (lightFactor * (lightColor shr 16 and 0xff)).toInt()
        if (red > 255) {
            red = 255
        }

        var green = (lightFactor * (lightColor shr 8 and 0xff)).toInt()
        if (green > 255) {
            green = 255
        }

        var blue = (lightFactor * (lightColor and 0xff)).toInt()
        if (blue > 255) {
            blue = 255
        }

        if (this.toolkit.bigEndianess) {
            this.bitBuffer!!.writeFloatBigEndian(sceneX.toFloat())
            this.bitBuffer!!.writeFloatBigEndian(sceneY.toFloat())
            this.bitBuffer!!.writeFloatBigEndian(sceneZ.toFloat())
        } else {
            this.bitBuffer!!.writeFloatLittleEndian(sceneX.toFloat())
            this.bitBuffer!!.writeFloatLittleEndian(sceneY.toFloat())
            this.bitBuffer!!.writeFloatLittleEndian(sceneZ.toFloat())
        }

        this.bitBuffer!!.writeByte(red)
        this.bitBuffer!!.writeByte(green)
        this.bitBuffer!!.writeByte(blue)
        this.bitBuffer!!.writeByte(255)
        writeIndex(vertexIndex)
    }

    fun drawLightMesh(cameraGridX: Int, cameraGridZ: Int, visibleRange: Int, tilesOnScreen: Array<BooleanArray>) {
        if (this.indexBuffer != null && this.minTileX <= visibleRange + cameraGridX && this.maxTileX >= cameraGridX - visibleRange && this.minTileZ <= visibleRange + cameraGridZ && this.maxTileZ >= cameraGridZ - visibleRange) {
            for (tileZ in this.minTileZ..this.maxTileZ) {
                for (tileX in this.minTileX..this.maxTileX) {
                    val relativeX = tileX - cameraGridX
                    val relativeZ = tileZ - cameraGridZ
                    if (relativeX > -visibleRange && relativeX < visibleRange && relativeZ > -visibleRange && relativeZ < visibleRange && tilesOnScreen[visibleRange + relativeX][visibleRange + relativeZ]) {
                        this.toolkit.setTextureEnvColor((this.pointLight.getAlpha() * 255.0F).toInt() shl 24)
                        this.toolkit.toggleDrawingClientstates(this.positionDrawBuffer, null, this.colorDrawBuffer, null)
                        this.toolkit.drawElements(this.indexBuffer!!, 0, this.totalIndexCount)
                        return
                    }
                }
            }
        }
    }
}
