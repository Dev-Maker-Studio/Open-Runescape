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
class NativeParticleRenderer(nativeToolkit: AbstractNativeToolkit) {

    var overflowBucketCount: Int = 0
    var maxParticlesPerBucket: Int = 64
    var maxOverflowBuckets: Int = 64
    var maxOverflowEntitiesPerBucket: Int = 768
    var maxDepthBuckets: Int = 1600
    val depthBucketBits: Int = BufferUtils.ilog(1600)
    val depthValues: IntArray
    val pointEntityIndices: IntArray
    val overflowBucketSizes: IntArray
    val depthBucketEntities: Array<Array<PointEntity?>>
    val pointEntities: Array<Array<PointEntity?>>
    val vertexDeclaration: VertexDeclaration
    val dynamicVertexStream: NativeStream
    val staticNormalStream: NativeStream
    val indexBuffer: NativeToolkitBuffer

    init {
        depthValues = IntArray(8191)
        pointEntityIndices = IntArray(1600)
        overflowBucketSizes = IntArray(64)
        depthBucketEntities = Array(1600) { arrayOfNulls(64) }
        pointEntities = Array(64) { arrayOfNulls(768) }
        overflowBucketCount = 0
        vertexDeclaration = nativeToolkit.createVertexDeclaration(arrayOf(
            VertexDeclarationComponent(arrayOf(VertexDeclarationAction.POSITION, VertexDeclarationAction.COLOR, VertexDeclarationAction.TEXCOORD)),
            VertexDeclarationComponent(VertexDeclarationAction.NORMAL_POINTER_ACTION)
        ))
        dynamicVertexStream = nativeToolkit.createStream(true)
        staticNormalStream = nativeToolkit.createStream(false)
        staticNormalStream.create(393168, 12)
        indexBuffer = nativeToolkit.createIndexBuffer(false)
        indexBuffer.create(49146)
        val directBuffer = nativeToolkit.directBuffer
        directBuffer.clear()

        var i: Int = 0
        while (i < 8191) {
            val vertexBase = i * 4
            directBuffer.putShort(vertexBase.toShort())
            directBuffer.putShort((vertexBase + 1).toShort())
            directBuffer.putShort((vertexBase + 2).toShort())
            directBuffer.putShort((vertexBase + 2).toShort())
            directBuffer.putShort((vertexBase + 3).toShort())
            directBuffer.putShort(vertexBase.toShort())
            i++
        }

        indexBuffer.upload(0, directBuffer.position(), nativeToolkit.directBufferAddress)
        directBuffer.clear()

        i = 0
        while (i < 8191) {
            directBuffer.putFloat(0.0f)
            directBuffer.putFloat(-1.0f)
            directBuffer.putFloat(0.0f)
            directBuffer.putFloat(0.0f)
            directBuffer.putFloat(-1.0f)
            directBuffer.putFloat(0.0f)
            directBuffer.putFloat(0.0f)
            directBuffer.putFloat(-1.0f)
            directBuffer.putFloat(0.0f)
            directBuffer.putFloat(0.0f)
            directBuffer.putFloat(-1.0f)
            directBuffer.putFloat(0.0f)
            i++
        }

        staticNormalStream.upload(0, directBuffer.position(), nativeToolkit.directBufferAddress)
    }

    fun resetVertexStream() {
        dynamicVertexStream.create(786336, 24)
    }

    fun deallocate() {
        dynamicVertexStream.deallocate()
    }

    fun render(nativeToolkit: AbstractNativeToolkit, particleWrapper: ParticleWrapper) {
        nativeToolkit.setDepthBuffering(false)
        currentLightIntensity = nativeToolkit.lightIntensity
        val zA = nativeToolkit.particleMatrix.elements[2]
        val zB = nativeToolkit.particleMatrix.elements[6]
        val zC = nativeToolkit.particleMatrix.elements[10]
        val zD = nativeToolkit.particleMatrix.elements[14]
        var depthIndex = 0
        var minDepth = Int.MAX_VALUE
        var maxDepth = 0
        val particleTree = particleWrapper.particleBinaryTree.current

        var particleNode = particleTree.child
        while (particleNode !== particleTree) {
            val pointEntity = particleNode as PointEntity
            val depth = (zA * (pointEntity.x shr 12) + zB * (pointEntity.y shr 12) + zC * (pointEntity.z shr 12) + zD).toInt()
            if (depth > maxDepth) {
                maxDepth = depth
            }

            if (depth < minDepth) {
                minDepth = depth
            }

            depthValues[depthIndex++] = depth
            particleNode = particleNode.child
        }

        var bucketCount = maxDepth - minDepth
        val depthShift: Int
        if (bucketCount + 2 > 1600) {
            depthShift = 1 + BufferUtils.ilog(bucketCount) - depthBucketBits
            bucketCount = (bucketCount shr depthShift) + 2
        } else {
            depthShift = 0
            bucketCount += 2
        }

        var batchNode = particleTree.child
        depthIndex = 0
        nativeToolkit.bindIndexBuffer(indexBuffer)
        val particleShader = nativeToolkit.abstractParticleShader!!
        particleShader.setViewProjectionMatrix(AbstractNativeToolkit.WORLD_VIEW_PROJECTION_MATRIX)
        particleShader.textureCoordinateMatrix.identity()
        particleShader.diffuseColor = -1
        var materialId = -2
        var hasLightAdjustment = true

        var startNewBatch = true
        while (batchNode !== particleTree) {
            overflowBucketCount = 0

            var i = 0
            while (i < bucketCount) {
                pointEntityIndices[i] = 0
                i++
            }

            i = 0
            while (i < 64) {
                overflowBucketSizes[i] = 0
                i++
            }

            while (batchNode !== particleTree) {
                val pointEntity = batchNode as PointEntity
                if (startNewBatch) {
                    materialId = pointEntity.materialId
                    hasLightAdjustment = pointEntity.lightIntensityAdjustment
                    startNewBatch = false
                }

                if (depthIndex > 0 && (materialId != pointEntity.materialId || hasLightAdjustment != pointEntity.lightIntensityAdjustment)) {
                    startNewBatch = true
                    break
                }

                val bucketIndex = (depthValues[depthIndex++] - minDepth) shr depthShift
                if (bucketIndex < 1600) {
                    if (pointEntityIndices[bucketIndex] < 64) {
                        depthBucketEntities[bucketIndex][pointEntityIndices[bucketIndex]++] = pointEntity
                    } else {
                        if (pointEntityIndices[bucketIndex] == 64) {
                            if (overflowBucketCount == 64) {
                                batchNode = batchNode.child
                                continue
                            }

                            pointEntityIndices[bucketIndex] += 1 + overflowBucketCount++
                        }

                        val overflowBucket = pointEntities[pointEntityIndices[bucketIndex] - 64 - 1]
                        val overflowBucketIndex = pointEntityIndices[bucketIndex] - 64 - 1
                        val overflowSlot = overflowBucketSizes[pointEntityIndices[bucketIndex] - 64 - 1]
                        overflowBucketSizes[overflowBucketIndex] = overflowBucketSizes[pointEntityIndices[bucketIndex] - 64 - 1] + 1
                        overflowBucket[overflowSlot] = pointEntity
                    }
                }
                batchNode = batchNode.child
            }

            particleShader.diffuseSampler = nativeToolkit.materialCache!!.getTextureSampler(if (materialId >= 0) materialId else -1)
            if (hasLightAdjustment && nativeToolkit.lightIntensity != currentLightIntensity) {
                nativeToolkit.updateSun(currentLightIntensity)
            } else if (nativeToolkit.lightIntensity != 1.0f) {
                nativeToolkit.updateSun(1.0f)
            }

            renderBatch(nativeToolkit, bucketCount)
        }

        if (nativeToolkit.lightIntensity != currentLightIntensity) {
            nativeToolkit.updateSun(currentLightIntensity)
        }

        nativeToolkit.setDepthBuffering(true)
    }

    fun renderBatch(nativeToolkit: AbstractNativeToolkit, bucketCount: Int) {
        var particleCount = 0
        val particleMatrix = nativeToolkit.particleMatrix
        val xA = particleMatrix.elements[0]
        val xB = particleMatrix.elements[4]
        val xC = particleMatrix.elements[8]
        val yA = particleMatrix.elements[1]
        val yB = particleMatrix.elements[5]
        val yC = particleMatrix.elements[9]
        val scaleXA = xA + yA
        val scaleYA = xB + yB
        val scaleZA = xC + yC
        val scaleXB = xA - yA
        val scaleYB = xB - yB
        val scaleZB = xC - yC
        val scaleXC = yA - xA
        val scaleYC = yB - xB
        val scaleZC = yC - xC
        val directBuffer = nativeToolkit.directBuffer
        directBuffer.clear()

        for (bucketIndex in (bucketCount - 1) downTo 0) {
            val bucketSize = Math.min(64, pointEntityIndices[bucketIndex])
            if (bucketSize > 0) {
                var index: Int = bucketSize - 1
                while (index >= 0) {
                    val pointEntity = depthBucketEntities[bucketIndex][index]!!
                    val colorArgb = pointEntity.colorArgb
                    val red = (colorArgb shr 16).toByte()
                    val green = (colorArgb shr 8).toByte()
                    val blue = colorArgb.toByte()
                    val alpha = (colorArgb ushr 24).toByte()
                    val x = (pointEntity.x shr 12).toFloat()
                    val y = (pointEntity.y shr 12).toFloat()
                    val z = (pointEntity.z shr 12).toFloat()
                    val size = (pointEntity.size shr 12).toFloat()
                    directBuffer.putFloat(x + scaleXA * (-size))
                    directBuffer.putFloat(y + scaleYA * (-size))
                    directBuffer.putFloat(z + scaleZA * (-size))
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
                    directBuffer.putFloat(x + scaleXC * size)
                    directBuffer.putFloat(y + scaleYC * size)
                    directBuffer.putFloat(z + scaleZC * size)
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
                    directBuffer.putFloat(x + scaleXA * size)
                    directBuffer.putFloat(y + scaleYA * size)
                    directBuffer.putFloat(z + scaleZA * size)
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
                    directBuffer.putFloat(x + scaleXB * size)
                    directBuffer.putFloat(y + scaleYB * size)
                    directBuffer.putFloat(z + scaleZB * size)
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
                    ++particleCount
                    --index
                }

                if (pointEntityIndices[bucketIndex] > 64) {
                    index = pointEntityIndices[bucketIndex] - 64 - 1

                    var overflowSlot = overflowBucketSizes[index] - 1
                    while (overflowSlot >= 0) {
                        val pointEntity = pointEntities[index][overflowSlot]!!
                        val colorArgb = pointEntity.colorArgb
                        val red = (colorArgb shr 16).toByte()
                        val green = (colorArgb shr 8).toByte()
                        val blue = colorArgb.toByte()
                        val alpha = (colorArgb ushr 24).toByte()
                        val x = (pointEntity.x shr 12).toFloat()
                        val y = (pointEntity.y shr 12).toFloat()
                        val z = (pointEntity.z shr 12).toFloat()
                        val size = (pointEntity.size shr 12).toFloat()
                        directBuffer.putFloat(x + scaleXA * (-size))
                        directBuffer.putFloat(y + scaleYA * (-size))
                        directBuffer.putFloat(z + scaleZA * (-size))
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
                        directBuffer.putFloat(x + scaleXC * size)
                        directBuffer.putFloat(y + scaleYC * size)
                        directBuffer.putFloat(z + scaleZC * size)
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
                        directBuffer.putFloat(x + scaleXA * size)
                        directBuffer.putFloat(y + scaleYA * size)
                        directBuffer.putFloat(z + scaleZA * size)
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
                        directBuffer.putFloat(x + scaleXB * size)
                        directBuffer.putFloat(y + scaleYB * size)
                        directBuffer.putFloat(z + scaleZB * size)
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
                        ++particleCount
                        --overflowSlot
                    }
                }
            }
        }

        dynamicVertexStream.upload(0, directBuffer.position(), nativeToolkit.directBufferAddress)
        nativeToolkit.setStreamSource(0, dynamicVertexStream)
        nativeToolkit.setStreamSource(1, staticNormalStream)
        nativeToolkit.setVertexDeclaration(vertexDeclaration)
        val particleShader = nativeToolkit.abstractParticleShader!!
        particleShader.drawParticles(particleCount)
    }

    companion object {
        @JvmField
        var currentLightIntensity: Float = 0f
    }
}
