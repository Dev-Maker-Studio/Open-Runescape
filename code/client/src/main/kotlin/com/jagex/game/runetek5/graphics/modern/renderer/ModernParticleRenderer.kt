package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.material.TextureScroll
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.model.particle.PointEntity
import com.jagex.game.runetek5.utils.BufferUtils
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ShortBuffer

class ModernParticleRenderer {

    companion object {
        const val BYTES_PER_VERTEX = 24

        private const val MAX_PARTICLES = 8191

        private const val MAX_DEPTH_BUCKETS = 1600

        private const val MAX_PER_BUCKET = 64

        private const val MAX_OVERFLOW_BUCKETS = 64
    }

    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    private var vertexData: ByteBuffer? = null

    private var staticIndices: ShortBuffer? = null

    private var uViewProjectionLocation = -1
    private var uTextureLocation = -1
    private var uTextureEnabledLocation = -1
    private var uFogEnabledLocation = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogColorLocation = -1
    private var uCombineModeLocation = -1
    private var uTextureScrollLocation = -1
    private var uAmbientLocation = -1

    private var isInitialized = false

    private var drawCallsThisFrame = 0
    private var particlesThisFrame = 0
    private val vpScratch = FloatArray(16)

    fun resetFrameStats() {
        drawCallsThisFrame = 0
        particlesThisFrame = 0
    }

    private val particleDepths = IntArray(MAX_PARTICLES)
    private val bucketCounts = IntArray(MAX_DEPTH_BUCKETS)
    private val overflowCounts = IntArray(MAX_OVERFLOW_BUCKETS)
    private val primaryBuckets = Array(MAX_DEPTH_BUCKETS) { arrayOfNulls<PointEntity>(MAX_PER_BUCKET) }
    private val overflowBuckets = Array(MAX_OVERFLOW_BUCKETS) { arrayOfNulls<PointEntity>(768) }
    private var overflowBucketIndex = 0
    private val depthShift: Int = BufferUtils.ilog(MAX_DEPTH_BUCKETS)

    private val PARTICLE_POS_DIAG = System.getProperty("render.particlediag") != null
    private var posDiagTick = 0

    fun initialize() {
        if (isInitialized) return

        shader = ShaderProgram.createFromResources("/shaders/particle.vert", "/shaders/particle.frag")

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.STREAM)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.STATIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        vao!!.bind()
        vbo!!.bind()

        vao!!.setAttribute(
            VertexArray.Companion.Attributes.POSITION, 3,
            GLWrapper.Constants.FLOAT, false,
            BYTES_PER_VERTEX, 0
        )
        vao!!.setAttribute(
            VertexArray.Companion.Attributes.TEXCOORD, 2,
            GLWrapper.Constants.FLOAT, false,
            BYTES_PER_VERTEX, 12
        )
        vao!!.setAttribute(
            VertexArray.Companion.Attributes.COLOR, 4,
            GLWrapper.Constants.UNSIGNED_BYTE, true,
            BYTES_PER_VERTEX, 20
        )

        val indexData = MemoryUtil.memAllocShort(MAX_PARTICLES * 6)
        for (i in 0 until MAX_PARTICLES) {
            val base = i * 4
            indexData.put(base.toShort())
            indexData.put((base + 1).toShort())
            indexData.put((base + 2).toShort())
            indexData.put((base + 2).toShort())
            indexData.put((base + 3).toShort())
            indexData.put(base.toShort())
        }
        indexData.flip()
        ibo!!.upload(indexData)
        staticIndices = indexData

        ibo!!.bind()
        vao!!.unbind()

        vertexData = MemoryUtil.memAlloc(MAX_PARTICLES * 4 * BYTES_PER_VERTEX)

        cacheUniformLocations()

        isInitialized = true
    }

    private fun cacheUniformLocations() {
        val s = shader ?: return
        s.bind()
        uViewProjectionLocation = s.getUniformLocation("uViewProjection")
        uTextureLocation = s.getUniformLocation("uTexture")
        uTextureEnabledLocation = s.getUniformLocation("uTextureEnabled")
        uFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uFogStartLocation = s.getUniformLocation("uFogStart")
        uFogEndLocation = s.getUniformLocation("uFogEnd")
        uFogColorLocation = s.getUniformLocation("uFogColor")
        uCombineModeLocation = s.getUniformLocation("uCombineMode")
        uTextureScrollLocation = s.getUniformLocation("uTextureScroll")
        uAmbientLocation = s.getUniformLocation("uAmbient")
        s.unbind()
    }

    fun isReady(): Boolean = isInitialized

    fun renderParticles(
        particleWrapper: ParticleWrapper,
        viewMatrixFloats: FloatArray,
        projectionMatrixFloats: FloatArray,
        lookupTextureHandle: (Int) -> Int,
        fogEnabled: Boolean,
        fogStart: Float,
        fogEnd: Float,
        fogR: Float,
        fogG: Float,
        fogB: Float,
        vpOverride: FloatArray? = null,
        lookupCombineMode: (Int) -> Int = { 0 },
        lookupScroll: (Int) -> Int = { 0 },
        textureSeconds: Float = 0f,
        sunBrightness: Float = 1f,
        sunR: Float = 1f,
        sunG: Float = 1f,
        sunB: Float = 1f
    ) {
        if (!isInitialized) return

        val rightX = viewMatrixFloats[0]
        val rightY = viewMatrixFloats[4]
        val rightZ = viewMatrixFloats[8]
        val upX = viewMatrixFloats[1]
        val upY = viewMatrixFloats[5]
        val upZ = viewMatrixFloats[9]

        val offDiag1X = rightX + upX
        val offDiag1Y = rightY + upY
        val offDiag1Z = rightZ + upZ

        val offDiag2X = rightX - upX
        val offDiag2Y = rightY - upY
        val offDiag2Z = rightZ - upZ

        val offAntiX = upX - rightX
        val offAntiY = upY - rightY
        val offAntiZ = upZ - rightZ

        val zNearA = viewMatrixFloats[2]
        val zNearB = viewMatrixFloats[6]
        val zNearC = viewMatrixFloats[10]
        val zNearD = viewMatrixFloats[14]

        val vpMatrix = vpScratch
        if (vpOverride != null) {
            System.arraycopy(vpOverride, 0, vpMatrix, 0, 16)
            vpMatrix[1] = -vpMatrix[1]; vpMatrix[5] = -vpMatrix[5]; vpMatrix[9] = -vpMatrix[9]; vpMatrix[13] = -vpMatrix[13]
        } else {
        for (row in 0..3) {
            for (col in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += projectionMatrixFloats[row + k * 4] * viewMatrixFloats[k + col * 4]
                }
                vpMatrix[row + col * 4] = sum
            }
        }
        vpMatrix[1] = -vpMatrix[1]
        vpMatrix[5] = -vpMatrix[5]
        vpMatrix[9] = -vpMatrix[9]
        vpMatrix[13] = -vpMatrix[13]
        }

        GLWrapper.enable(GLWrapper.Constants.BLEND)
        GLWrapper.blendFunc(GLWrapper.Constants.SRC_ALPHA, GLWrapper.Constants.ONE_MINUS_SRC_ALPHA)
        GLWrapper.depthMask(false)
        GLWrapper.enable(GLWrapper.Constants.DEPTH_TEST)

        val s = shader!!
        s.bind()
        GLWrapper.uniformMatrix4fv(uViewProjectionLocation, false, vpMatrix)
        s.setUniform(uTextureLocation, 0)
        s.setUniform(uFogEnabledLocation, if (fogEnabled) 1 else 0)
        if (fogEnabled) {
            s.setUniform(uFogStartLocation, fogStart)
            s.setUniform(uFogEndLocation, fogEnd)
            s.setUniform(uFogColorLocation, fogR, fogG, fogB, 1.0f)
        }

        val tree = particleWrapper.particleBinaryTree.current
        var totalParticles = 0
        var minDepth = Int.MAX_VALUE
        var maxDepth = 0

        var particle = tree.child
        while (particle != null && particle !== tree) {
            if (totalParticles >= MAX_PARTICLES) break
            val pe = particle as PointEntity
            val depth = (zNearA * (pe.x shr 12) + zNearB * (pe.y shr 12) + zNearC * (pe.z shr 12) + zNearD).toInt()
            if (depth > maxDepth) maxDepth = depth
            if (depth < minDepth) minDepth = depth
            particleDepths[totalParticles++] = depth
            particle = particle.child
        }

        if (totalParticles == 0) {
            s.unbind()
            GLWrapper.depthMask(true)
            return
        }
        if (PARTICLE_POS_DIAG && (tree.child as? PointEntity)?.materialId == 744 && posDiagTick++ % 40 == 0) {
            val sb = StringBuilder("[ParticlePos] modern n=$totalParticles")
            var q = tree.child; var k = 0
            while (q != null && q !== tree && k < 4) { val pe = q as PointEntity; sb.append(" p$k=(${pe.x shr 12},${pe.y shr 12},${pe.z shr 12} sz=${pe.size shr 12} mat=${pe.materialId})"); q = q.child; k++ }
            var yMin = Int.MAX_VALUE; var yMax = Int.MIN_VALUE; var qq = tree.child; while (qq != null && qq !== tree) { val yy = (qq as PointEntity).y shr 12; if (yy < yMin) yMin = yy; if (yy > yMax) yMax = yy; qq = qq.child }; sb.append(" yRange=$yMin..$yMax")
            sb.append(" view14=${viewMatrixFloats[14]} vp13=${vpOverride?.get(13)}")
            System.err.println(sb)
        }

        var depthRange = maxDepth - minDepth
        val bucketShift: Int
        if (depthRange + 2 > MAX_DEPTH_BUCKETS) {
            bucketShift = 1 + BufferUtils.ilog(depthRange) - depthShift
            depthRange = (depthRange shr bucketShift) + 2
        } else {
            bucketShift = 0
            depthRange += 2
        }

        var particleNode = tree.child
        var particlePtr = 0
        var currentMaterialId = -2
        var currentLightAdjust = true
        var needUpdate = true

        while (particleNode != null && particleNode !== tree) {
            overflowBucketIndex = 0

            for (i in 0 until depthRange) {
                bucketCounts[i] = 0
            }
            for (i in 0 until MAX_OVERFLOW_BUCKETS) {
                overflowCounts[i] = 0
            }

            while (particleNode != null && particleNode !== tree) {
                val pe = particleNode as PointEntity
                if (needUpdate) {
                    currentMaterialId = pe.materialId
                    currentLightAdjust = pe.lightIntensityAdjustment
                    needUpdate = false
                }

                if (particlePtr > 0 && (currentMaterialId != pe.materialId || currentLightAdjust != pe.lightIntensityAdjustment)) {
                    needUpdate = true
                    break
                }

                if (particlePtr >= totalParticles) {
                    particleNode = tree
                    break
                }
                val depth = (particleDepths[particlePtr++] - minDepth) shr bucketShift
                if (depth < MAX_DEPTH_BUCKETS) {
                    if (bucketCounts[depth] < MAX_PER_BUCKET) {
                        primaryBuckets[depth][bucketCounts[depth]++] = pe
                    } else {
                        if (bucketCounts[depth] == MAX_PER_BUCKET) {
                            if (overflowBucketIndex == MAX_OVERFLOW_BUCKETS) {
                                particleNode = particleNode.child
                                continue
                            }
                            bucketCounts[depth] += 1 + overflowBucketIndex++
                        }
                        val overflowIdx = bucketCounts[depth] - MAX_PER_BUCKET - 1
                        overflowBuckets[overflowIdx][overflowCounts[overflowIdx]++] = pe
                    }
                }
                particleNode = particleNode.child
            }

            val textureHandle = if (currentMaterialId >= 0) lookupTextureHandle(currentMaterialId) else 0
            if (textureHandle > 0) {
                GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureHandle)
                s.setUniform(uTextureEnabledLocation, 1)
                s.setUniform(uCombineModeLocation, lookupCombineMode(currentMaterialId))
                val scroll = lookupScroll(currentMaterialId)
                s.setUniform(
                    uTextureScrollLocation,
                    textureSeconds * TextureScroll.speedU(scroll) / 64f % 1f,
                    textureSeconds * TextureScroll.speedV(scroll) / 64f % 1f
                )
            } else {
                s.setUniform(uTextureEnabledLocation, 0)
                s.setUniform(uCombineModeLocation, 0)
                s.setUniform(uTextureScrollLocation, 0f, 0f)
            }
            val ambient = if (currentLightAdjust) sunBrightness else 1f
            s.setUniform(uAmbientLocation, ambient * sunR, ambient * sunG, ambient * sunB)

            val vb = vertexData!!
            vb.clear()
            var quadCount = 0

            for (bucketIdx in depthRange - 1 downTo 0) {
                val primaryCount = Math.min(MAX_PER_BUCKET, bucketCounts[bucketIdx])
                if (primaryCount > 0) {
                    for (pIdx in primaryCount - 1 downTo 0) {
                        val pe = primaryBuckets[bucketIdx][pIdx]!!
                        writeParticleQuad(
                            vb, pe,
                            offDiag1X, offDiag1Y, offDiag1Z,
                            offDiag2X, offDiag2Y, offDiag2Z,
                            offAntiX, offAntiY, offAntiZ
                        )
                        quadCount++
                    }

                    if (bucketCounts[bucketIdx] > MAX_PER_BUCKET) {
                        val overflowIdx = bucketCounts[bucketIdx] - MAX_PER_BUCKET - 1
                        for (oIdx in overflowCounts[overflowIdx] - 1 downTo 0) {
                            val pe = overflowBuckets[overflowIdx][oIdx]!!
                            writeParticleQuad(
                                vb, pe,
                                offDiag1X, offDiag1Y, offDiag1Z,
                                offDiag2X, offDiag2Y, offDiag2Z,
                                offAntiX, offAntiY, offAntiZ
                            )
                            quadCount++
                        }
                    }
                }
            }

            if (quadCount > 0) {
                vb.flip()
                vbo!!.upload(vb)

                vao!!.bind()
                ibo!!.bind()
                GLWrapper.drawElements(
                    GLWrapper.Constants.TRIANGLES,
                    quadCount * 6,
                    GLWrapper.Constants.UNSIGNED_SHORT,
                    0
                )

                drawCallsThisFrame++
                particlesThisFrame += quadCount
            }
        }

        s.unbind()
        GLWrapper.depthMask(true)
    }

    private fun writeParticleQuad(
        buffer: ByteBuffer,
        particle: PointEntity,
        d1x: Float, d1y: Float, d1z: Float,
        d2x: Float, d2y: Float, d2z: Float,
        ax: Float, ay: Float, az: Float
    ) {
        val color = particle.colorArgb
        val red = ((color shr 16) and 0xFF).toByte()
        val green = ((color shr 8) and 0xFF).toByte()
        val blue = (color and 0xFF).toByte()
        val alpha = ((color ushr 24) and 0xFF).toByte()

        val x = (particle.x shr 12).toFloat()
        val y = (particle.y shr 12).toFloat()
        val z = (particle.z shr 12).toFloat()
        val size = (particle.size shr 12).toFloat()

        buffer.putFloat(x + d1x * (-size))
        buffer.putFloat(y + d1y * (-size))
        buffer.putFloat(z + d1z * (-size))
        buffer.putFloat(0.0f)
        buffer.putFloat(0.0f)
        buffer.put(red).put(green).put(blue).put(alpha)

        buffer.putFloat(x + ax * size)
        buffer.putFloat(y + ay * size)
        buffer.putFloat(z + az * size)
        buffer.putFloat(0.0f)
        buffer.putFloat(1.0f)
        buffer.put(red).put(green).put(blue).put(alpha)

        buffer.putFloat(x + d1x * size)
        buffer.putFloat(y + d1y * size)
        buffer.putFloat(z + d1z * size)
        buffer.putFloat(1.0f)
        buffer.putFloat(1.0f)
        buffer.put(red).put(green).put(blue).put(alpha)

        buffer.putFloat(x + d2x * size)
        buffer.putFloat(y + d2y * size)
        buffer.putFloat(z + d2z * size)
        buffer.putFloat(1.0f)
        buffer.putFloat(0.0f)
        buffer.put(red).put(green).put(blue).put(alpha)
    }

    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, particlesThisFrame)
    }

    fun destroy() {
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        vertexData?.let { MemoryUtil.memFree(it) }
        staticIndices?.let { MemoryUtil.memFree(it) }

        shader = null
        vao = null
        vbo = null
        ibo = null
        vertexData = null
        staticIndices = null

        isInitialized = false
    }

    data class FrameStats(
        val drawCalls: Int,
        val particles: Int
    )
}
