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
    var anInt1616: Int = 0
    val modelviewMatrix: FloatArray = FloatArray(16)
    val textureBuffer = GlBitBuffer(786336)
    val anInt1609: Int
    val particleDepths: IntArray
    val anIntArray1615: IntArray
    val anIntArray1604: IntArray
    val aEntityNodeSub1Sub1ArrayArray1614: Array<Array<PointEntity?>>
    val aEntityNodeSub1Sub1ArrayArray1607: Array<Array<PointEntity?>>

    init {
        this.anInt1609 = BufferUtils.ilog(1600)
        this.particleDepths = IntArray(8191)
        this.anIntArray1615 = IntArray(1600)
        this.anIntArray1604 = IntArray(64)
        this.aEntityNodeSub1Sub1ArrayArray1614 = Array(1600) { arrayOfNulls(64) }
        this.aEntityNodeSub1Sub1ArrayArray1607 = Array(64) { arrayOfNulls(768) }
        this.anInt1616 = 0
    }

    fun initialize(toolkit: OpenGlToolkit) {
        this.arrayBuffer = toolkit.createArrayBuffer(24, null, 196584, true)
        this.textureCoordinates = OpenGlArrayDrawBuffer(this.arrayBuffer, GL_FLOAT, 2, 0)
        this.vertices = OpenGlArrayDrawBuffer(this.arrayBuffer, GL_FLOAT, 3, 8)
        this.colors = OpenGlArrayDrawBuffer(this.arrayBuffer, GL_UNSIGNED_BYTE, 4, 20)
    }

    fun drawParticles(toolkit: OpenGlToolkit, particleWrapper: ParticleWrapper) {
        disableLights(toolkit)
        val vertexZNearA = toolkit.viewMatrix44.elements[2]
        val vertexZNearB = toolkit.viewMatrix44.elements[6]
        val vertexZNearC = toolkit.viewMatrix44.elements[10]
        val vertexZNearD = toolkit.viewMatrix44.elements[14]

        try {
            var totalParticles = 0
            var minDepth = Int.MAX_VALUE
            var maxDepth = 0
            val tree = particleWrapper.particleBinaryTree.current

            var particle = tree.child
            while (particle != tree) {
                val particleEntity = particle as PointEntity
                val depth = (vertexZNearA * (particleEntity.x shr 12) + vertexZNearB * (particleEntity.y shr 12) + vertexZNearC * (particleEntity.z shr 12) + vertexZNearD).toInt()
                if (depth > maxDepth) {
                    maxDepth = depth
                }

                if (depth < minDepth) {
                    minDepth = depth
                }

                this.particleDepths[totalParticles++] = depth
                particle = particle.child
            }

            var depthRange = maxDepth - minDepth
            val i_20: Int
            if (depthRange + 2 > 1600) {
                i_20 = 1 + BufferUtils.ilog(depthRange) - this.anInt1609
                depthRange = (depthRange shr i_20) + 2
            } else {
                i_20 = 0
                depthRange += 2
            }

            var particles = tree.child
            var particlePtr = 0
            var materialId = -2
            var brightnessUpdate = true

            var updateRequired = true
            while (particles != tree) {
                this.anInt1616 = 0

                for (i_17 in 0 until depthRange) {
                    this.anIntArray1615[i_17] = 0
                }

                for (i_17 in 0 until 64) {
                    this.anIntArray1604[i_17] = 0
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

                    val depth = this.particleDepths[particlePtr++] - minDepth shr i_20
                    if (depth < 1600) {
                        if (this.anIntArray1615[depth] < 64) {
                            this.aEntityNodeSub1Sub1ArrayArray1614[depth][this.anIntArray1615[depth]++] = particleEntity
                        } else {
                            if (this.anIntArray1615[depth] == 64) {
                                if (this.anInt1616 == 64) {
                                    particles = particles.child
                                    continue
                                }

                                this.anIntArray1615[depth] += 1 + this.anInt1616++
                            }

                            val arr_10000 = this.aEntityNodeSub1Sub1ArrayArray1607[this.anIntArray1615[depth] - 64 - 1]
                            val i_10002 = this.anIntArray1615[depth] - 64 - 1
                            val i_10004 = this.anIntArray1604[this.anIntArray1615[depth] - 64 - 1]
                            this.anIntArray1604[i_10002] = this.anIntArray1604[this.anIntArray1615[depth] - 64 - 1] + 1
                            arr_10000[i_10004] = particleEntity
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
            // NOOP
        }

        enableLights(toolkit)

    }

    fun draw(toolkit: OpenGlToolkit, depthRange: Int) {
        OpenGL.glGetFloatv(GL_MODELVIEW_MATRIX, this.modelviewMatrix, 0)
        val vertexXA = this.modelviewMatrix[0]
        val vertexXB = this.modelviewMatrix[4]
        val vertexXC = this.modelviewMatrix[8]
        val vertexYA = this.modelviewMatrix[1]
        val vertexYB = this.modelviewMatrix[5]
        val vertexYC = this.modelviewMatrix[9]
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
            for (i_18 in depthRange - 1 downTo 0) {
                val i_19 = Math.min(64, this.anIntArray1615[i_18])
                if (i_19 > 0) {
                    for (i_20 in i_19 - 1 downTo 0) {
                        val pointEntity = this.aEntityNodeSub1Sub1ArrayArray1614[i_18][i_20]!!
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

                    if (this.anIntArray1615[i_18] > 64) {
                        val i_20 = this.anIntArray1615[i_18] - 64 - 1

                        for (i_21 in this.anIntArray1604[i_20] - 1 downTo 0) {
                            val entity = this.aEntityNodeSub1Sub1ArrayArray1607[i_20][i_21]!!
                            val color = entity.colorArgb
                            val red = (color shr 16).toByte()
                            val green = (color shr 8).toByte()
                            val blue = color.toByte()
                            val alpha = (color ushr 24).toByte()
                            val x = (entity.x shr 12).toFloat()
                            val y = (entity.y shr 12).toFloat()
                            val z = (entity.z shr 12).toFloat()
                            val size = entity.size shr 12
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
            for (i_18 in depthRange - 1 downTo 0) {
                val i_19 = Math.min(64, this.anIntArray1615[i_18])
                if (i_19 > 0) {
                    for (i_20 in i_19 - 1 downTo 0) {
                        val pointEntity = this.aEntityNodeSub1Sub1ArrayArray1614[i_18][i_20]!!
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

                    if (this.anIntArray1615[i_18] > 64) {
                        val main = this.anIntArray1615[i_18] - 64 - 1

                        for (sub in this.anIntArray1604[main] - 1 downTo 0) {
                            val entity = this.aEntityNodeSub1Sub1ArrayArray1607[main][sub]!!
                            val color = entity.colorArgb
                            val red = (color shr 16).toByte()
                            val green = (color shr 8).toByte()
                            val blue = color.toByte()
                            val alpha = (color ushr 24).toByte()
                            val x = (entity.x shr 12).toFloat()
                            val y = (entity.y shr 12).toFloat()
                            val z = (entity.z shr 12).toFloat()
                            val size = entity.size shr 12
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
        OpenGL.glDisable(GL_LIGHT0)
        OpenGL.glDisable(GL_LIGHT1)
        toolkit.setDepthMask(false)
        OpenGL.glNormal3f(0.0F, -1.0f, 0.0F)
    }

    fun enableLights(toolkit: OpenGlToolkit) {
        toolkit.setDepthMask(true)
        OpenGL.glEnable(GL_LIGHT0)
        OpenGL.glEnable(GL_LIGHT1)
        if (toolkit.sunBrightness != sunBrightness) {
            toolkit.updateSun(sunBrightness)
        }
    }

    companion object {
        @JvmField var sunBrightness: Float = 0f
    }
}
