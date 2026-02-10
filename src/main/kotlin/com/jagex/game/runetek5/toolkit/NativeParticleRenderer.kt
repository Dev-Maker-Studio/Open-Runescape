package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.model.particle.PointEntity
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent
import com.jagex.game.runetek5.utils.BufferUtils

/**
 * Native particle renderer.
 * Note: Previously named DirectXParticleRenderer (incorrectly), used by both OpenGL and DirectX.
 */
class NativeParticleRenderer(class505Sub21: AbstractNativeToolkit) {

    var anInt509: Int = 0
    var anInt497: Int = 64
    var anInt501: Int = 64
    var anInt502: Int = 768
    var anInt503: Int = 1600
    val anInt504: Int = BufferUtils.ilog(1600)
    val anIntArray506: IntArray
    val pointEntityIndices: IntArray
    val anIntArray508: IntArray
    val aEntityNodeSub1Sub1ArrayArray498: Array<Array<PointEntity?>>
    val pointEntities: Array<Array<PointEntity?>>
    val aClass70499: VertexDeclaration
    val anInterface4500: NativeStream
    val anInterface4510: NativeStream
    val anInterface32511: NativeToolkitBuffer

    init {
        anIntArray506 = IntArray(8191)
        pointEntityIndices = IntArray(1600)
        anIntArray508 = IntArray(64)
        aEntityNodeSub1Sub1ArrayArray498 = Array(1600) { arrayOfNulls(64) }
        pointEntities = Array(64) { arrayOfNulls(768) }
        anInt509 = 0
        aClass70499 = class505Sub21.createVertexDeclaration(arrayOf(
            VertexDeclarationComponent(arrayOf(VertexDeclarationAction.POSITION, VertexDeclarationAction.COLOR, VertexDeclarationAction.TEXCOORD)),
            VertexDeclarationComponent(VertexDeclarationAction.NORMAL_POINTER_ACTION)
        ))
        anInterface4500 = class505Sub21.createStream(true)
        anInterface4510 = class505Sub21.createStream(false)
        anInterface4510.create(393168, 12)
        anInterface32511 = class505Sub21.createIndexBuffer(false)
        anInterface32511.create(49146)
        val bytebuffer2 = class505Sub21.directBuffer
        bytebuffer2.clear()

        var i_3: Int = 0
        while (i_3 < 8191) {
            val i_4 = i_3 * 4
            bytebuffer2.putShort(i_4.toShort())
            bytebuffer2.putShort((i_4 + 1).toShort())
            bytebuffer2.putShort((i_4 + 2).toShort())
            bytebuffer2.putShort((i_4 + 2).toShort())
            bytebuffer2.putShort((i_4 + 3).toShort())
            bytebuffer2.putShort(i_4.toShort())
            i_3++
        }

        anInterface32511.upload(0, bytebuffer2.position(), class505Sub21.directBufferAddress)
        bytebuffer2.clear()

        i_3 = 0
        while (i_3 < 8191) {
            bytebuffer2.putFloat(0.0f)
            bytebuffer2.putFloat(-1.0f)
            bytebuffer2.putFloat(0.0f)
            bytebuffer2.putFloat(0.0f)
            bytebuffer2.putFloat(-1.0f)
            bytebuffer2.putFloat(0.0f)
            bytebuffer2.putFloat(0.0f)
            bytebuffer2.putFloat(-1.0f)
            bytebuffer2.putFloat(0.0f)
            bytebuffer2.putFloat(0.0f)
            bytebuffer2.putFloat(-1.0f)
            bytebuffer2.putFloat(0.0f)
            i_3++
        }

        anInterface4510.upload(0, bytebuffer2.position(), class505Sub21.directBufferAddress)
    }

    fun method1089() {
        anInterface4500.create(786336, 24)
    }

    fun deallocate() {
        anInterface4500.deallocate()
    }

    fun render(nativeToolkit: AbstractNativeToolkit, particleWrapper: ParticleWrapper) {
        nativeToolkit.setDepthBuffering(false)
        currentLightIntensity = nativeToolkit.lightIntensity
        val zNearA = nativeToolkit.particleMatrix.faceList[2]
        val zNearB = nativeToolkit.particleMatrix.faceList[6]
        val zNearC = nativeToolkit.particleMatrix.faceList[10]
        val zNearD = nativeToolkit.particleMatrix.faceList[14]
        var i_7 = 0
        var i_8 = Int.MAX_VALUE
        var i_9 = 0
        val particleTree = particleWrapper.particleBinaryTree.current

        var particleNode = particleTree.child
        while (particleNode !== particleTree) {
            val pointEntity = particleNode as PointEntity
            val i_22 = (zNearA * (pointEntity.x shr 12) + zNearB * (pointEntity.y shr 12) + zNearC * (pointEntity.z shr 12) + zNearD).toInt()
            if (i_22 > i_9) {
                i_9 = i_22
            }

            if (i_22 < i_8) {
                i_8 = i_22
            }

            anIntArray506[i_7++] = i_22
            particleNode = particleNode.child
        }

        var i_20 = i_9 - i_8
        val i_12: Int
        if (i_20 + 2 > 1600) {
            i_12 = 1 + BufferUtils.ilog(i_20) - anInt504
            i_20 = (i_20 shr i_12) + 2
        } else {
            i_12 = 0
            i_20 += 2
        }

        var child = particleTree.child
        i_7 = 0
        nativeToolkit.bindIndexBuffer(anInterface32511)
        val class10314 = nativeToolkit.abstractParticleShader!!
        class10314.setViewProjectionMatrix(AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX)
        class10314.textureCoordinateMatrix.identity()
        class10314.diffuseColor = -1
        var materialId = -2
        var hasLightAdjustment = true

        var hasAdjustments = true
        while (child !== particleTree) {
            anInt509 = 0

            var i_18 = 0
            while (i_18 < i_20) {
                pointEntityIndices[i_18] = 0
                i_18++
            }

            i_18 = 0
            while (i_18 < 64) {
                anIntArray508[i_18] = 0
                i_18++
            }

            while (child !== particleTree) {
                val pointEntity = child as PointEntity
                if (hasAdjustments) {
                    materialId = pointEntity.materialId
                    hasLightAdjustment = pointEntity.lightIntensityAdjustment
                    hasAdjustments = false
                }

                if (i_7 > 0 && (materialId != pointEntity.materialId || hasLightAdjustment != pointEntity.lightIntensityAdjustment)) {
                    hasAdjustments = true
                    break
                }

                val i_19 = (anIntArray506[i_7++] - i_8) shr i_12
                if (i_19 < 1600) {
                    if (pointEntityIndices[i_19] < 64) {
                        aEntityNodeSub1Sub1ArrayArray498[i_19][pointEntityIndices[i_19]++] = pointEntity
                    } else {
                        if (pointEntityIndices[i_19] == 64) {
                            if (anInt509 == 64) {
                                child = child.child
                                continue
                            }

                            pointEntityIndices[i_19] += 1 + anInt509++
                        }

                        val arr_10000 = pointEntities[pointEntityIndices[i_19] - 64 - 1]
                        val i_10002 = pointEntityIndices[i_19] - 64 - 1
                        val i_10004 = anIntArray508[pointEntityIndices[i_19] - 64 - 1]
                        anIntArray508[i_10002] = anIntArray508[pointEntityIndices[i_19] - 64 - 1] + 1
                        arr_10000[i_10004] = pointEntity
                    }
                }
                child = child.child
            }

            class10314.diffuseSampler = nativeToolkit.materialCache!!.getTextureSampler(if (materialId >= 0) materialId else -1)
            if (hasLightAdjustment && nativeToolkit.lightIntensity != currentLightIntensity) {
                nativeToolkit.updateSun(currentLightIntensity)
            } else if (nativeToolkit.lightIntensity != 1.0f) {
                nativeToolkit.updateSun(1.0f)
            }

            method1096(nativeToolkit, i_20)
        }

        if (nativeToolkit.lightIntensity != currentLightIntensity) {
            nativeToolkit.updateSun(currentLightIntensity)
        }

        nativeToolkit.setDepthBuffering(true)
    }

    fun method1096(nativeToolkit: AbstractNativeToolkit, i_2: Int) {
        var i_3 = 0
        val particleMatrix = nativeToolkit.particleMatrix
        val xA = particleMatrix.faceList[0]
        val xB = particleMatrix.faceList[4]
        val xC = particleMatrix.faceList[8]
        val yA = particleMatrix.faceList[1]
        val yB = particleMatrix.faceList[5]
        val yC = particleMatrix.faceList[9]
        val scaleXA = xA + yA
        val scaleYA = xB + yB
        val scaleZA = xC + yC
        val scaleXB = xA - yA
        val scaleYB = xB - yB
        val scakeZB = xC - yC
        val scaleXC = yA - xA
        val scaleYC = yB - xB
        val scaleZC = yC - xC
        val directBuffer = nativeToolkit.directBuffer
        directBuffer.clear()

        for (i_21 in (i_2 - 1) downTo 0) {
            val i_22 = Math.min(64, pointEntityIndices[i_21])
            if (i_22 > 0) {
                var i_23: Int = i_22 - 1
                while (i_23 >= 0) {
                    val pointEntity = aEntityNodeSub1Sub1ArrayArray498[i_21][i_23]!!
                    val colorRgba = pointEntity.colorArgb
                    val red = (colorRgba shr 16).toByte()
                    val green = (colorRgba shr 8).toByte()
                    val blue = colorRgba.toByte()
                    val alpha = (colorRgba ushr 24).toByte()
                    val downscaledX = (pointEntity.x shr 12).toFloat()
                    val downscaledY = (pointEntity.y shr 12).toFloat()
                    val downscaledZ = (pointEntity.z shr 12).toFloat()
                    val downscaledSize = (pointEntity.size shr 12).toFloat()
                    directBuffer.putFloat(downscaledX + scaleXA * (-downscaledSize))
                    directBuffer.putFloat(downscaledY + scaleYA * (-downscaledSize))
                    directBuffer.putFloat(downscaledZ + scaleZA * (-downscaledSize))
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue)
                        directBuffer.put(green)
                        directBuffer.put(red)
                        directBuffer.put(alpha)
                    } else {
                        directBuffer.put(red)
                        directBuffer.put(green)
                        directBuffer.put(blue)
                        directBuffer.put(alpha)
                    }

                    directBuffer.putFloat(0.0f)
                    directBuffer.putFloat(0.0f)
                    directBuffer.putFloat(downscaledX + scaleXC * downscaledSize)
                    directBuffer.putFloat(downscaledY + scaleYC * downscaledSize)
                    directBuffer.putFloat(downscaledZ + scaleZC * downscaledSize)
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue)
                        directBuffer.put(green)
                        directBuffer.put(red)
                        directBuffer.put(alpha)
                    } else {
                        directBuffer.put(red)
                        directBuffer.put(green)
                        directBuffer.put(blue)
                        directBuffer.put(alpha)
                    }

                    directBuffer.putFloat(0.0f)
                    directBuffer.putFloat(1.0f)
                    directBuffer.putFloat(downscaledX + scaleXA * downscaledSize)
                    directBuffer.putFloat(downscaledY + scaleYA * downscaledSize)
                    directBuffer.putFloat(downscaledZ + scaleZA * downscaledSize)
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue)
                        directBuffer.put(green)
                        directBuffer.put(red)
                        directBuffer.put(alpha)
                    } else {
                        directBuffer.put(red)
                        directBuffer.put(green)
                        directBuffer.put(blue)
                        directBuffer.put(alpha)
                    }

                    directBuffer.putFloat(1.0f)
                    directBuffer.putFloat(1.0f)
                    directBuffer.putFloat(downscaledX + scaleXB * downscaledSize)
                    directBuffer.putFloat(downscaledY + scaleYB * downscaledSize)
                    directBuffer.putFloat(downscaledZ + scakeZB * downscaledSize)
                    if (nativeToolkit.endiannessType == 0) {
                        directBuffer.put(blue)
                        directBuffer.put(green)
                        directBuffer.put(red)
                        directBuffer.put(alpha)
                    } else {
                        directBuffer.put(red)
                        directBuffer.put(green)
                        directBuffer.put(blue)
                        directBuffer.put(alpha)
                    }

                    directBuffer.putFloat(1.0f)
                    directBuffer.putFloat(0.0f)
                    ++i_3
                    --i_23
                }

                if (pointEntityIndices[i_21] > 64) {
                    i_23 = pointEntityIndices[i_21] - 64 - 1

                    var i_24 = anIntArray508[i_23] - 1
                    while (i_24 >= 0) {
                        val pointEntity = pointEntities[i_23][i_24]!!
                        val color = pointEntity.colorArgb
                        val red = (color shr 16).toByte()
                        val green = (color shr 8).toByte()
                        val blue = color.toByte()
                        val alpha = (color ushr 24).toByte()
                        val downscaledX = (pointEntity.x shr 12).toFloat()
                        val downscaledY = (pointEntity.y shr 12).toFloat()
                        val downscaledZ = (pointEntity.z shr 12).toFloat()
                        val downscaledSize = (pointEntity.size shr 12).toFloat()
                        directBuffer.putFloat(downscaledX + scaleXA * (-downscaledSize))
                        directBuffer.putFloat(downscaledY + scaleYA * (-downscaledSize))
                        directBuffer.putFloat(downscaledZ + scaleZA * (-downscaledSize))
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue)
                            directBuffer.put(green)
                            directBuffer.put(red)
                            directBuffer.put(alpha)
                        } else {
                            directBuffer.put(red)
                            directBuffer.put(green)
                            directBuffer.put(blue)
                            directBuffer.put(alpha)
                        }

                        directBuffer.putFloat(0.0f)
                        directBuffer.putFloat(0.0f)
                        directBuffer.putFloat(downscaledX + scaleXC * downscaledSize)
                        directBuffer.putFloat(downscaledY + scaleYC * downscaledSize)
                        directBuffer.putFloat(downscaledZ + scaleZC * downscaledSize)
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue)
                            directBuffer.put(green)
                            directBuffer.put(red)
                            directBuffer.put(alpha)
                        } else {
                            directBuffer.put(red)
                            directBuffer.put(green)
                            directBuffer.put(blue)
                            directBuffer.put(alpha)
                        }

                        directBuffer.putFloat(0.0f)
                        directBuffer.putFloat(1.0f)
                        directBuffer.putFloat(downscaledX + scaleXA * downscaledSize)
                        directBuffer.putFloat(downscaledY + scaleYA * downscaledSize)
                        directBuffer.putFloat(downscaledZ + scaleZA * downscaledSize)
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue)
                            directBuffer.put(green)
                            directBuffer.put(red)
                            directBuffer.put(alpha)
                        } else {
                            directBuffer.put(red)
                            directBuffer.put(green)
                            directBuffer.put(blue)
                            directBuffer.put(alpha)
                        }

                        directBuffer.putFloat(1.0f)
                        directBuffer.putFloat(1.0f)
                        directBuffer.putFloat(downscaledX + scaleXB * downscaledSize)
                        directBuffer.putFloat(downscaledY + scaleYB * downscaledSize)
                        directBuffer.putFloat(downscaledZ + scakeZB * downscaledSize)
                        if (nativeToolkit.endiannessType == 0) {
                            directBuffer.put(blue)
                            directBuffer.put(green)
                            directBuffer.put(red)
                            directBuffer.put(alpha)
                        } else {
                            directBuffer.put(red)
                            directBuffer.put(green)
                            directBuffer.put(blue)
                            directBuffer.put(alpha)
                        }

                        directBuffer.putFloat(1.0f)
                        directBuffer.putFloat(0.0f)
                        ++i_3
                        --i_24
                    }
                }
            }
        }

        anInterface4500.upload(0, directBuffer.position(), nativeToolkit.directBufferAddress)
        nativeToolkit.setStreamSource(0, anInterface4500)
        nativeToolkit.setStreamSource(1, anInterface4510)
        nativeToolkit.setVertexDeclaration(aClass70499)
        val particleShader = nativeToolkit.abstractParticleShader!!
        particleShader.drawParticles(i_3)
    }

    companion object {
        @JvmField
        var currentLightIntensity: Float = 0f
    }
}
