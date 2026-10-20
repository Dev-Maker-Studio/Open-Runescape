package com.jagex.game.runetek5.toolkit.opengl

import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.graphics.model.particle.ParticleNode
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.model.particle.PointEntity
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.utils.BufferUtils

import jaggl.OpenGL
import com.jagex.game.runetek5.toolkit.opengl.*
class GlParticleRenderer {

    var arrayBuffer: ArrayBufferGL? = null
    var textureCoordinates: OpenGlArrayDrawBuffer? = null
    var vertices: OpenGlArrayDrawBuffer? = null
    var colors: OpenGlArrayDrawBuffer? = null
    var overflowBucketCount: Int = 0
    val textureBuffer = GlBitBuffer(786336)
    val depthBucketShiftBits: Int
    val particleDepths: IntArray
    val depthBucketCounts: IntArray
    val overflowBucketSizes: IntArray
    val depthBucketEntities: Array<Array<PointEntity?>>
    val overflowBucketEntities: Array<Array<PointEntity?>>

    init {
        this.depthBucketShiftBits = BufferUtils.ilog(1600)
        this.particleDepths = IntArray(8191)
        this.depthBucketCounts = IntArray(1600)
        this.overflowBucketSizes = IntArray(64)
        this.depthBucketEntities = Array(1600) { arrayOfNulls(64) }
        this.overflowBucketEntities = Array(64) { arrayOfNulls(768) }
        this.overflowBucketCount = 0
    }

    fun initialize(toolkit: OpenGlToolkit) {
        this.arrayBuffer = toolkit.createArrayBuffer(24, null, 196584, true)
        this.textureCoordinates = OpenGlArrayDrawBuffer(this.arrayBuffer, GL_FLOAT, 2, 0)
        this.vertices = OpenGlArrayDrawBuffer(this.arrayBuffer, GL_FLOAT, 3, 8)
        this.colors = OpenGlArrayDrawBuffer(this.arrayBuffer, GL_UNSIGNED_BYTE, 4, 20)
    }

    private val PARTICLE_POS_DIAG = System.getProperty("render.particlediag") != null
    private var posDiagTick = 0

    fun drawParticles(toolkit: OpenGlToolkit, particleWrapper: ParticleWrapper) {
        disableLights(toolkit)
        val vertexZA = toolkit.viewMatrix44.elements[2]
        val vertexZB = toolkit.viewMatrix44.elements[6]
        val vertexZC = toolkit.viewMatrix44.elements[10]
        val vertexZD = toolkit.viewMatrix44.elements[14]

        try {
            var totalParticles = 0
            var minDepth = Int.MAX_VALUE
            var maxDepth = 0
            val tree = particleWrapper.particleBinaryTree.current

            var particle = tree.child
            while (particle != tree) {
                val particleEntity = particle as PointEntity
                val depth = (vertexZA * (particleEntity.x shr 12) + vertexZB * (particleEntity.y shr 12) + vertexZC * (particleEntity.z shr 12) + vertexZD).toInt()
                if (depth > maxDepth) {
                    maxDepth = depth
                }

                if (depth < minDepth) {
                    minDepth = depth
                }

                this.particleDepths[totalParticles++] = depth
                particle = particle.child
            }

            if (PARTICLE_POS_DIAG && (tree.child as? PointEntity)?.materialId == 744 && posDiagTick++ % 40 == 0 && totalParticles > 0) {
                val sb = StringBuilder("[ParticlePos] legacy n=$totalParticles")
                var q = tree.child; var k = 0
                while (q != null && q !== tree && k < 4) { val pe = q as PointEntity; sb.append(" p$k=(${pe.x shr 12},${pe.y shr 12},${pe.z shr 12} sz=${pe.size shr 12} mat=${pe.materialId})"); q = q.child; k++ }
                var yMin = Int.MAX_VALUE; var yMax = Int.MIN_VALUE; var qq = tree.child; while (qq != null && qq !== tree) { val yy = (qq as PointEntity).y shr 12; if (yy < yMin) yMin = yy; if (yy > yMax) yMax = yy; qq = qq.child }; sb.append(" yRange=$yMin..$yMax")
            sb.append(" view14=${toolkit.viewMatrix44.elements[14]}")
                System.err.println(sb)
            }
            var depthRange = maxDepth - minDepth
            val depthShift: Int
            if (depthRange + 2 > 1600) {
                depthShift = 1 + BufferUtils.ilog(depthRange) - this.depthBucketShiftBits
                depthRange = (depthRange shr depthShift) + 2
            } else {
                depthShift = 0
                depthRange += 2
            }

            var particles = tree.child
            var particlePtr = 0
            var materialId = -2
            var brightnessUpdate = true

            var updateRequired = true
            while (particles != tree) {
                this.overflowBucketCount = 0

                for (i in 0 until depthRange) {
                    this.depthBucketCounts[i] = 0
                }

                for (i in 0 until 64) {
                    this.overflowBucketSizes[i] = 0
                }

                while (particles != tree) {
                    val particleEntity = particles as PointEntity
                    if (updateRequired) {
                        materialId = particleEntity.materialId
                        brightnessUpdate = particleEntity.lightIntensityAdjustment
                        updateRequired = false
                    }

                    if (particlePtr > 0 && (materialId != particleEntity.materialId || brightnessUpdate != particleEntity.lightIntensityAdjustment)) {
                        updateRequired = true
                        break
                    }

                    val depth = this.particleDepths[particlePtr++] - minDepth shr depthShift
                    if (depth < 1600) {
                        if (this.depthBucketCounts[depth] < 64) {
                            this.depthBucketEntities[depth][this.depthBucketCounts[depth]++] = particleEntity
                        } else {
                            if (this.depthBucketCounts[depth] == 64) {
                                if (this.overflowBucketCount == 64) {
                                    particles = particles.child
                                    continue
                                }

                                this.depthBucketCounts[depth] += 1 + this.overflowBucketCount++
                            }

                            val overflowBucket = this.overflowBucketEntities[this.depthBucketCounts[depth] - 64 - 1]
                            val overflowBucketIndex = this.depthBucketCounts[depth] - 64 - 1
                            val overflowSlot = this.overflowBucketSizes[this.depthBucketCounts[depth] - 64 - 1]
                            this.overflowBucketSizes[overflowBucketIndex] = this.overflowBucketSizes[this.depthBucketCounts[depth] - 64 - 1] + 1
                            overflowBucket[overflowSlot] = particleEntity
                        }
                    }
                    particles = particles.child
                }

                if (materialId >= 0) {
                    toolkit.renderTexture(materialId)
                } else {
                    toolkit.renderTexture(-1)
                }

                if (brightnessUpdate && toolkit.sunBrightness != sunBrightness) {
                    toolkit.updateSun(sunBrightness)
                } else if (toolkit.sunBrightness != 1.0F) {
                    toolkit.updateSun(1.0F)
                }

                draw(toolkit, depthRange)
            }
        } catch (ignored: Exception) {
        }

        enableLights(toolkit)

    }

    fun draw(toolkit: OpenGlToolkit, depthRange: Int) {
        val modelview = toolkit.viewMatrix44.elements
        val vertexXA = modelview[0]
        val vertexXB = modelview[4]
        val vertexXC = modelview[8]
        val vertexYA = modelview[1]
        val vertexYB = modelview[5]
        val vertexYC = modelview[9]
        val offX1 = vertexXA + vertexYA
        val offY1 = vertexXB + vertexYB
        val offZ1 = vertexXC + vertexYC
        val offX3 = vertexXA - vertexYA
        val offY3 = vertexXB - vertexYB
        val offZ3 = vertexXC - vertexYC
        val offX2 = vertexYA - vertexXA
        val offY2 = vertexYB - vertexXB
        val offZ2 = vertexYC - vertexXC
        this.textureBuffer.currentPosition = 0
        if (toolkit.bigEndianess) {
            for (bucket in depthRange - 1 downTo 0) {
                val count = Math.min(64, this.depthBucketCounts[bucket])
                if (count > 0) {
                    for (i in count - 1 downTo 0) {
                        val pointEntity = this.depthBucketEntities[bucket][i]!!
                        val color = pointEntity.colorArgb
                        val red = (color shr 16).toByte()
                        val green = (color shr 8).toByte()
                        val blue = color.toByte()
                        val alpha = (color ushr 24).toByte()
                        val x = (pointEntity.x shr 12).toFloat()
                        val y = (pointEntity.y shr 12).toFloat()
                        val z = (pointEntity.z shr 12).toFloat()
                        val size = pointEntity.size shr 12
                        this.textureBuffer.writeFloatBigEndian(0.0F)
                        this.textureBuffer.writeFloatBigEndian(0.0F)
                        this.textureBuffer.writeFloatBigEndian(x + offX1 * (-size))
                        this.textureBuffer.writeFloatBigEndian(y + offY1 * (-size))
                        this.textureBuffer.writeFloatBigEndian(z + offZ1 * (-size))
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                        this.textureBuffer.writeFloatBigEndian(0.0F)
                        this.textureBuffer.writeFloatBigEndian(1.0F)
                        this.textureBuffer.writeFloatBigEndian(x + offX2 * size)
                        this.textureBuffer.writeFloatBigEndian(y + offY2 * size)
                        this.textureBuffer.writeFloatBigEndian(z + offZ2 * size)
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                        this.textureBuffer.writeFloatBigEndian(1.0F)
                        this.textureBuffer.writeFloatBigEndian(1.0F)
                        this.textureBuffer.writeFloatBigEndian(x + offX1 * size)
                        this.textureBuffer.writeFloatBigEndian(y + offY1 * size)
                        this.textureBuffer.writeFloatBigEndian(z + offZ1 * size)
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                        this.textureBuffer.writeFloatBigEndian(1.0F)
                        this.textureBuffer.writeFloatBigEndian(0.0F)
                        this.textureBuffer.writeFloatBigEndian(x + offX3 * size)
                        this.textureBuffer.writeFloatBigEndian(y + offY3 * size)
                        this.textureBuffer.writeFloatBigEndian(z + offZ3 * size)
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                    }

                    if (this.depthBucketCounts[bucket] > 64) {
                        val overflowBucketIndex = this.depthBucketCounts[bucket] - 64 - 1

                        for (overflowSlot in this.overflowBucketSizes[overflowBucketIndex] - 1 downTo 0) {
                            val pointEntity = this.overflowBucketEntities[overflowBucketIndex][overflowSlot]!!
                            val color = pointEntity.colorArgb
                            val red = (color shr 16).toByte()
                            val green = (color shr 8).toByte()
                            val blue = color.toByte()
                            val alpha = (color ushr 24).toByte()
                            val x = (pointEntity.x shr 12).toFloat()
                            val y = (pointEntity.y shr 12).toFloat()
                            val z = (pointEntity.z shr 12).toFloat()
                            val size = pointEntity.size shr 12
                            this.textureBuffer.writeFloatBigEndian(0.0F)
                            this.textureBuffer.writeFloatBigEndian(0.0F)
                            this.textureBuffer.writeFloatBigEndian(x + offX1 * (-size))
                            this.textureBuffer.writeFloatBigEndian(y + offY1 * (-size))
                            this.textureBuffer.writeFloatBigEndian(z + offZ1 * (-size))
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                            this.textureBuffer.writeFloatBigEndian(0.0F)
                            this.textureBuffer.writeFloatBigEndian(1.0F)
                            this.textureBuffer.writeFloatBigEndian(x + offX2 * size)
                            this.textureBuffer.writeFloatBigEndian(y + offY2 * size)
                            this.textureBuffer.writeFloatBigEndian(z + offZ2 * size)
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                            this.textureBuffer.writeFloatBigEndian(1.0F)
                            this.textureBuffer.writeFloatBigEndian(1.0F)
                            this.textureBuffer.writeFloatBigEndian(x + offX1 * size)
                            this.textureBuffer.writeFloatBigEndian(y + offY1 * size)
                            this.textureBuffer.writeFloatBigEndian(z + offZ1 * size)
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                            this.textureBuffer.writeFloatBigEndian(1.0F)
                            this.textureBuffer.writeFloatBigEndian(0.0F)
                            this.textureBuffer.writeFloatBigEndian(x + offX3 * size)
                            this.textureBuffer.writeFloatBigEndian(y + offY3 * size)
                            this.textureBuffer.writeFloatBigEndian(z + offZ3 * size)
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                        }
                    }
                }
            }
        } else {
            for (bucket in depthRange - 1 downTo 0) {
                val count = Math.min(64, this.depthBucketCounts[bucket])
                if (count > 0) {
                    for (i in count - 1 downTo 0) {
                        val pointEntity = this.depthBucketEntities[bucket][i]!!
                        val color = pointEntity.colorArgb
                        val red = (color shr 16).toByte()
                        val green = (color shr 8).toByte()
                        val blue = color.toByte()
                        val alpha = (color ushr 24).toByte()
                        val x = (pointEntity.x shr 12).toFloat()
                        val y = (pointEntity.y shr 12).toFloat()
                        val z = (pointEntity.z shr 12).toFloat()
                        val size = pointEntity.size shr 12
                        this.textureBuffer.writeFloatLittleEndian(0.0F)
                        this.textureBuffer.writeFloatLittleEndian(0.0F)
                        this.textureBuffer.writeFloatLittleEndian(x + offX1 * (-size))
                        this.textureBuffer.writeFloatLittleEndian(y + offY1 * (-size))
                        this.textureBuffer.writeFloatLittleEndian(z + offZ1 * (-size))
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                        this.textureBuffer.writeFloatLittleEndian(0.0F)
                        this.textureBuffer.writeFloatLittleEndian(1.0F)
                        this.textureBuffer.writeFloatLittleEndian(x + offX2 * size)
                        this.textureBuffer.writeFloatLittleEndian(y + offY2 * size)
                        this.textureBuffer.writeFloatLittleEndian(z + offZ2 * size)
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                        this.textureBuffer.writeFloatLittleEndian(1.0F)
                        this.textureBuffer.writeFloatLittleEndian(1.0F)
                        this.textureBuffer.writeFloatLittleEndian(x + offX1 * size)
                        this.textureBuffer.writeFloatLittleEndian(y + offY1 * size)
                        this.textureBuffer.writeFloatLittleEndian(z + offZ1 * size)
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                        this.textureBuffer.writeFloatLittleEndian(1.0F)
                        this.textureBuffer.writeFloatLittleEndian(0.0F)
                        this.textureBuffer.writeFloatLittleEndian(x + offX3 * size)
                        this.textureBuffer.writeFloatLittleEndian(y + offY3 * size)
                        this.textureBuffer.writeFloatLittleEndian(z + offZ3 * size)
                        this.textureBuffer.writeByte(red.toInt())
                        this.textureBuffer.writeByte(green.toInt())
                        this.textureBuffer.writeByte(blue.toInt())
                        this.textureBuffer.writeByte(alpha.toInt())
                    }

                    if (this.depthBucketCounts[bucket] > 64) {
                        val overflowBucketIndex = this.depthBucketCounts[bucket] - 64 - 1

                        for (overflowSlot in this.overflowBucketSizes[overflowBucketIndex] - 1 downTo 0) {
                            val pointEntity = this.overflowBucketEntities[overflowBucketIndex][overflowSlot]!!
                            val color = pointEntity.colorArgb
                            val red = (color shr 16).toByte()
                            val green = (color shr 8).toByte()
                            val blue = color.toByte()
                            val alpha = (color ushr 24).toByte()
                            val x = (pointEntity.x shr 12).toFloat()
                            val y = (pointEntity.y shr 12).toFloat()
                            val z = (pointEntity.z shr 12).toFloat()
                            val size = pointEntity.size shr 12
                            this.textureBuffer.writeFloatLittleEndian(0.0F)
                            this.textureBuffer.writeFloatLittleEndian(0.0F)
                            this.textureBuffer.writeFloatLittleEndian(x + offX1 * (-size))
                            this.textureBuffer.writeFloatLittleEndian(y + offY1 * (-size))
                            this.textureBuffer.writeFloatLittleEndian(z + offZ1 * (-size))
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                            this.textureBuffer.writeFloatLittleEndian(0.0F)
                            this.textureBuffer.writeFloatLittleEndian(1.0F)
                            this.textureBuffer.writeFloatLittleEndian(x + offX2 * size)
                            this.textureBuffer.writeFloatLittleEndian(y + offY2 * size)
                            this.textureBuffer.writeFloatLittleEndian(z + offZ2 * size)
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                            this.textureBuffer.writeFloatLittleEndian(1.0F)
                            this.textureBuffer.writeFloatLittleEndian(1.0F)
                            this.textureBuffer.writeFloatLittleEndian(x + offX1 * size)
                            this.textureBuffer.writeFloatLittleEndian(y + offY1 * size)
                            this.textureBuffer.writeFloatLittleEndian(z + offZ1 * size)
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                            this.textureBuffer.writeFloatLittleEndian(1.0F)
                            this.textureBuffer.writeFloatLittleEndian(0.0F)
                            this.textureBuffer.writeFloatLittleEndian(x + offX3 * size)
                            this.textureBuffer.writeFloatLittleEndian(y + offY3 * size)
                            this.textureBuffer.writeFloatLittleEndian(z + offZ3 * size)
                            this.textureBuffer.writeByte(red.toInt())
                            this.textureBuffer.writeByte(green.toInt())
                            this.textureBuffer.writeByte(blue.toInt())
                            this.textureBuffer.writeByte(alpha.toInt())
                        }
                    }
                }
            }
        }

        if (this.textureBuffer.currentPosition != 0) {
            this.arrayBuffer!!.putArray(24, this.textureBuffer.buffer, this.textureBuffer.currentPosition)
            toolkit.toggleDrawingClientstates(this.vertices, null, this.colors, this.textureCoordinates)
            toolkit.drawArrays(this.textureBuffer.currentPosition / 24)
        }
    }

    fun disableLights(toolkit: OpenGlToolkit) {
        sunBrightness = toolkit.sunBrightness
        toolkit.setupMatrixRendering()
        toolkit.setDepthMask(false)
    }

    fun enableLights(toolkit: OpenGlToolkit) {
        toolkit.setDepthMask(true)
        if (toolkit.sunBrightness != sunBrightness) {
            toolkit.updateSun(sunBrightness)
        }
    }

    companion object {
        @JvmField var sunBrightness: Float = 0f
    }
}
