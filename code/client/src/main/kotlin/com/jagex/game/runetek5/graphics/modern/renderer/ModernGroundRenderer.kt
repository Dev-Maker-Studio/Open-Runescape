package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.graphics.modern.core.IndexBuffer
import com.jagex.game.runetek5.graphics.modern.core.ShaderProgram
import com.jagex.game.runetek5.graphics.modern.core.VertexArray
import com.jagex.game.runetek5.graphics.modern.core.VertexBuffer
import com.jagex.game.runetek5.graphics.modern.core.VertexFormats
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.shadow.ShadowMapperGL
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.TRIANGLES
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper.Constants.UNSIGNED_SHORT
import com.jagex.game.runetek5.math.Matrix44
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

private val RAW_COLOR_DEBUG = System.getProperty("render.groundcolor")?.equals("raw", true) == true

class GroundWater(
    @JvmField var mode: Int = 0,
    @JvmField var noiseTexture: Int = 0,
    @JvmField var time: Float = 0f,
    @JvmField val stillFog: FloatArray = FloatArray(4),
    @JvmField var colour: Int = 0,
    @JvmField var rippleFogStart: Float = 0f,
    @JvmField var rippleFogDensity: Float = 0f,
    @JvmField var turbulence: Float = 0f,
    @JvmField val lapAmbient: FloatArray = floatArrayOf(1f, 1f, 1f),
    @JvmField var lapNoise: FloatArray? = null,
) {
    fun copyInto(target: GroundWater) {
        target.mode = mode
        target.noiseTexture = noiseTexture
        target.time = time
        stillFog.copyInto(target.stillFog)
        target.colour = colour
        target.rippleFogStart = rippleFogStart
        target.rippleFogDensity = rippleFogDensity
        target.turbulence = turbulence
        lapAmbient.copyInto(target.lapAmbient)
        target.lapNoise = lapNoise
    }

    companion object {
        @JvmField val NONE = GroundWater()
    }
}

class ModernGroundRenderer {

    companion object {
        const val BYTES_PER_VERTEX = 28
        private const val DEFAULT_VERTEX_CAPACITY = 65536
        private const val DEFAULT_INDEX_CAPACITY = 131072
    }

    private var vao: VertexArray? = null
    private var vbo: VertexBuffer? = null
    private var ibo: IndexBuffer? = null
    private var shader: ShaderProgram? = null

    private var texturedShader: ShaderProgram? = null
    private var externalVao: VertexArray? = null
    private var externalIbo: IndexBuffer? = null

    private var uTexViewProjectionLocation = -1
    private var uTexTextureScaleLocation = -1
    private var uTexTextureScrollLocation = -1
    private var uTexHasNormalsLocation = -1
    private var uTexSunDirectionLocation = -1
    private var uTexSunColorLocation = -1
    private var uTexAmbientLocation = -1
    private var uTexContrastLocation = -1
    private var uTexFogStartLocation = -1
    private var uTexFogEndLocation = -1
    private var uTexFogEnabledLocation = -1
    private var uTexFogColorLocation = -1
    private var uTexTextureLocation = -1
    private var uTexHasTextureLocation = -1
    private var uTexCombineModeLocation = -1
    private var uTexAlphaFromVertexLocation = -1
    private var uTexSunBrightnessLocation = -1
    private var uTexContrastPosLocation = -1
    private var uTexContrastNegLocation = -1
    private var uTexWaterModeLocation = -1
    private var uTexClipPlaneLocation = -1
    private var uTexDepthFogLocation = -1
    private var uTexInvViewProjectionLocation = -1
    private var uTexViewportLocation = -1
    private var uTexFramebufferSizeLocation = -1
    private var depthFogTexture = 0
    private var depthFogWidth = 0
    private var depthFogHeight = 0
    private val depthFogInverse = Matrix4f()
    private val depthFogInverseArray = FloatArray(16)
    private val depthFogViewport = IntArray(4)
    private val clipPlane = FloatArray(4)
    private var uTexWaterNoiseLocation = -1
    private var uTexHasWaterNoiseLocation = -1
    private var uTexWaterColourLocation = -1
    private var uTexStillFogLocation = -1
    private var uTexLapNoiseLocation = -1
    private var uTexLapTurbulenceLocation = -1
    private var uTexLapAmbientLocation = -1
    private var uTexWaterTimeLocation = -1
    private var uTexRippleFogLocation = -1
    private val WATER_TEXTURE_UNIT = 5
    private val SCENE_DEPTH_TEXTURE_UNIT = 6

    private var legacySunDirX = 0f
    private var legacySunDirY = -1f
    private var legacySunDirZ = 0f
    private var legacySunBrightness = 1f
    private var legacyContrastPos = 0f
    private var legacyContrastNeg = 0f

    private class ShadowTexEntry(val mapper: ShadowMapperGL) {
        var tex = 0
        val w = mapper.width
        val h = mapper.length
        val derived = ByteArray(w * h)
        var uploadedVersion = -1
        var lastUsedFrame = 0L
    }
    private val shadowEntries = LinkedHashMap<ShadowMapperGL, ShadowTexEntry>()
    private var currentShadowMapper: ShadowMapperGL? = null
    private var boundShadowMapper: ShadowMapperGL? = null
    private var shadowTexelSize = 32f
    private val SHADOW_TEXTURE_UNIT = 4
    private var shadowUploadScratch: ByteBuffer? = null
    private var shadowFrame = 0L
    private var shadowDiagCount = 0
    private val GROUND_POINT_LIGHTS =
        System.getProperty("render.groundlights")?.equals("false", true) != true
    private val GROUND_SHADOWS =
        System.getProperty("render.groundshadows")?.equals("false", true) != true

    fun setShadowMapper(sm: ShadowMapperGL?, texelSize: Float) {
        if (!GROUND_SHADOWS) return
        currentShadowMapper = sm
        shadowTexelSize = texelSize
    }

    private fun shadowEntryFor(sm: ShadowMapperGL): ShadowTexEntry {
        var e = shadowEntries[sm]
        if (e == null) {
            while (shadowEntries.size >= 6) {
                val oldest = shadowEntries.values.minByOrNull { it.lastUsedFrame } ?: break
                if (oldest.tex != 0) glDeleteTextures(oldest.tex)
                shadowEntries.remove(oldest.mapper)
                if (boundShadowMapper === oldest.mapper) boundShadowMapper = null
            }
            e = ShadowTexEntry(sm)
            shadowEntries[sm] = e
            sm.markAllDirty()
        }
        e.lastUsedFrame = shadowFrame
        return e
    }

    private fun syncShadowTexture(e: ShadowTexEntry) {
        val sm = e.mapper
        val derived = e.derived
        val w = e.w
        val h = e.h
        if (w <= 0 || h <= 0) return
        if (e.tex == 0) {
            e.tex = glGenTextures()
            glBindTexture(GL_TEXTURE_2D, e.tex)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, w, h, 0, GL_RED, GL_UNSIGNED_BYTE, null as ByteBuffer?)
            sm.markAllDirty()
        }
        if (!sm.dirty && e.uploadedVersion == sm.shadowVersion) return
        if (!sm.dirty) sm.markAllDirty()
        e.uploadedVersion = sm.shadowVersion
        val rectCount = sm.dirtyRectCount
        if (rectCount == 0) {
            syncShadowRect(e, sm.dirtyMinX, sm.dirtyMinY, sm.dirtyMaxX, sm.dirtyMaxY)
        } else {
            val rects = sm.dirtyRects
            for (k in 0 until rectCount) {
                val r = k * 4
                syncShadowRect(e, rects[r], rects[r + 1], rects[r + 2], rects[r + 3])
            }
        }
        sm.dirty = false
        sm.dirtyRectCount = 0
    }

    private fun syncShadowRect(e: ShadowTexEntry, rx0: Int, ry0: Int, rx1: Int, ry1: Int) {
        val sm = e.mapper
        val derived = e.derived
        val w = e.w
        val h = e.h
        val x0 = maxOf(0, rx0 - 1)
        val y0 = maxOf(0, ry0 - 1)
        val x1 = minOf(w, rx1 + 1)
        val y1 = minOf(h, ry1 + 1)
        if (x1 <= x0 || y1 <= y0) return
        val data = sm.shadowMapperData
        val rectW = x1 - x0
        val rectH = y1 - y0
        var scratch = shadowUploadScratch
        if (scratch == null || scratch.capacity() < rectW * rectH) {
            scratch?.let { MemoryUtil.memFree(it) }
            scratch = MemoryUtil.memAlloc(maxOf(rectW * rectH, 4096))
            shadowUploadScratch = scratch
        }
        scratch.clear()
        for (y in y0 until y1) {
            val row = y * w
            for (x in x0 until x1) {
                val i = row + x
                val v: Int = if (data[i].toInt() != 0) {
                    68
                } else {
                    var n = 0
                    if (x > 0 && data[i - 1].toInt() != 0) n++
                    if (x < w - 1 && data[i + 1].toInt() != 0) n++
                    if (y > 0 && data[i - w].toInt() != 0) n++
                    if (y < h - 1 && data[i + w].toInt() != 0) n++
                    n * 17
                }
                derived[i] = v.toByte()
                scratch.put(v.toByte())
            }
        }
        scratch.flip()
        glBindTexture(GL_TEXTURE_2D, e.tex)
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
        glPixelStorei(GL_UNPACK_ROW_LENGTH, 0)
        glTexSubImage2D(GL_TEXTURE_2D, 0, x0, y0, rectW, rectH, GL_RED, GL_UNSIGNED_BYTE, scratch)
        if (System.getProperty("shadow.diag") != null) {
            var nonZero = 0
            for (i in derived.indices) if (derived[i].toInt() != 0) nonZero++
            System.err.println("[GroundShadow] upload rect=($x0,$y0)-($x1,$y1) of ${w}x$h tex=${e.tex} " +
                "ver=${sm.shadowVersion} derivedNonZero=$nonZero glErr=${glGetError()}")
        }
    }

    private fun uploadShadowUniforms(s: ShaderProgram) {
        val sm = currentShadowMapper
        boundShadowMapper = sm
        val en = s.getUniformLocation("uGroundShadowEnabled")
        if (System.getProperty("shadow.diag") != null && shadowDiagCount++ % 2000 == 1500) {
            System.err.println("[GroundShadow] uniformLoc=$en mapper=${sm != null} entries=${shadowEntries.size} texel=$shadowTexelSize")
        }
        if (en < 0) return
        if (sm == null) { GLWrapper.uniform1i(en, 0); return }
        val e = shadowEntryFor(sm)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + SHADOW_TEXTURE_UNIT)
        syncShadowTexture(e)
        glBindTexture(GL_TEXTURE_2D, e.tex)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        GLWrapper.uniform1i(en, 1)
        val sc = s.getUniformLocation("uGroundShadowScale")
        if (sc >= 0) GLWrapper.uniform1f(sc, 1f / (shadowTexelSize * e.w))
        val tx = s.getUniformLocation("uGroundShadowTex")
        if (tx >= 0) GLWrapper.uniform1i(tx, SHADOW_TEXTURE_UNIT)
    }

    private var plCount = 0
    private var plPos = FloatArray(0)
    private var plColor = FloatArray(0)

    fun setPointLights(count: Int, pos: FloatArray, color: FloatArray) {
        plCount = count
        plPos = pos
        plColor = color
    }

    fun setDepthFog(depthTexture: Int, framebufferWidth: Int, framebufferHeight: Int) {
        closeGroundBatch()
        depthFogTexture = depthTexture
        depthFogWidth = framebufferWidth
        depthFogHeight = framebufferHeight
        glActiveTexture(GL_TEXTURE0 + SCENE_DEPTH_TEXTURE_UNIT)
        glBindTexture(GL_TEXTURE_2D, depthTexture)
        glActiveTexture(GL_TEXTURE0)
    }

    fun setClipPlane(a: Float, b: Float, c: Float, d: Float) {
        closeGroundBatch()
        clipPlane[0] = a
        clipPlane[1] = b
        clipPlane[2] = c
        clipPlane[3] = d
    }

    val waterPointLightCount: Int get() = if (GROUND_POINT_LIGHTS) plCount else 0
    val waterPointLightPositions: FloatArray get() = plPos
    val waterPointLightColours: FloatArray get() = plColor

    fun waterShadowTexture(): Int {
        if (!GROUND_SHADOWS) return 0
        val sm = currentShadowMapper ?: return 0
        val e = shadowEntryFor(sm)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + SHADOW_TEXTURE_UNIT)
        syncShadowTexture(e)
        glBindTexture(GL_TEXTURE_2D, e.tex)
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        return e.tex
    }

    fun waterShadowScale(): Float {
        val sm = currentShadowMapper ?: return 0f
        return 1f / (shadowTexelSize * shadowEntryFor(sm).w)
    }

    private fun uploadPointLights(s: ShaderProgram) {
        val loc = s.getUniformLocation("uPointLightCount")
        if (loc < 0) return
        if (!GROUND_POINT_LIGHTS) { GLWrapper.uniform1i(loc, 0); return }
        GLWrapper.uniform1i(loc, plCount)
        if (plCount > 0) {
            val lp = s.getArrayUniformLocation("uPointLightPos")
            val lc = s.getArrayUniformLocation("uPointLightColor")
            if (lp >= 0) GLWrapper.uniform4fv(lp, plPos, plCount)
            if (lc >= 0) GLWrapper.uniform3fv(lc, plColor, plCount)
        }
    }

    fun setLegacySunParams(
        dirX: Float, dirY: Float, dirZ: Float,
        brightness: Float, contrastPos: Float, contrastNeg: Float
    ) {
        legacySunDirX = dirX
        legacySunDirY = dirY
        legacySunDirZ = dirZ
        legacySunBrightness = brightness
        legacyContrastPos = contrastPos
        legacyContrastNeg = contrastNeg
    }

    private var vertexData: ByteBuffer? = null
    private var indexData: java.nio.ShortBuffer? = null

    private val viewMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()
    private val modelViewMatrix = Matrix4f()
    private val modelViewProjectionMatrix = Matrix4f()
    private val normalMatrix = Matrix3f()

    private val shadowMatrix = Matrix4f()

    private val sunDirection = Vector3f(0f, 1f, 0f)
    private val sunColor = Vector3f(1f, 1f, 1f)
    private var ambient = 0.3f
    private var contrast = 0.7f

    private var fogEnabled = false
    private var fogStart = 100f
    private var fogEnd = 500f
    private val fogColor = floatArrayOf(0.7f, 0.8f, 0.9f, 1f)

    private var shadowEnabled = false
    private var shadowMapTextureId = 0
    private var shadowBias = 0.005f
    private var shadowIntensity = 0.5f
    private var shadowMapSize = 2048f

    private var uMVPLocation = -1
    private var uMVLocation = -1
    private var uNormalMatrixLocation = -1
    private var uSunDirectionLocation = -1
    private var uSunColorLocation = -1
    private var uAmbientLocation = -1
    private var uContrastLocation = -1
    private var uFogStartLocation = -1
    private var uFogEndLocation = -1
    private var uFogEnabledLocation = -1
    private var uFogColorLocation = -1
    private var uShadowMatrixLocation = -1
    private var uShadowEnabledLocation = -1
    private var uShadowMapLocation = -1
    private var uShadowBiasLocation = -1
    private var uShadowIntensityLocation = -1
    private var uShadowMapSizeLocation = -1

    private var isInitialized = false

    private var drawCallsThisFrame = 0
    private var trianglesThisFrame = 0

    fun initialize() {
        if (isInitialized) return

        shader = ShaderProgram.createFromResources("/shaders/ground.vert", "/shaders/ground.frag")

        vao = VertexArray()
        vbo = VertexBuffer(VertexBuffer.BufferUsage.DYNAMIC)
        ibo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        vao!!.bind()
        vbo!!.bind()
        VertexFormats.setupGroundFormat(vao!!)
        ibo!!.bind()
        vao!!.unbind()

        vertexData = MemoryUtil.memAlloc(DEFAULT_VERTEX_CAPACITY * BYTES_PER_VERTEX)
        indexData = MemoryUtil.memAllocShort(DEFAULT_INDEX_CAPACITY)

        cacheUniformLocations()

        texturedShader = ShaderProgram.createFromResources("/shaders/groundTextured.vert", "/shaders/groundTextured.frag")
        externalVao = VertexArray()
        externalIbo = IndexBuffer(IndexBuffer.BufferUsage.DYNAMIC, IndexBuffer.IndexType.UNSIGNED_SHORT)

        cacheTexturedUniformLocations()

        isInitialized = true
    }

    private fun cacheUniformLocations() {
        val s = shader ?: return
        s.bind()
        uMVPLocation = s.getUniformLocation("uModelViewProjection")
        uMVLocation = s.getUniformLocation("uModelView")
        uNormalMatrixLocation = s.getUniformLocation("uNormalMatrix")
        uSunDirectionLocation = s.getUniformLocation("uSunDirection")
        uSunColorLocation = s.getUniformLocation("uSunColor")
        uAmbientLocation = s.getUniformLocation("uAmbient")
        uContrastLocation = s.getUniformLocation("uContrast")
        uFogStartLocation = s.getUniformLocation("uFogStart")
        uFogEndLocation = s.getUniformLocation("uFogEnd")
        uFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uFogColorLocation = s.getUniformLocation("uFogColor")
        uShadowMatrixLocation = s.getUniformLocation("uShadowMatrix")
        uShadowEnabledLocation = s.getUniformLocation("uShadowEnabled")
        uShadowMapLocation = s.getUniformLocation("uShadowMap")
        uShadowBiasLocation = s.getUniformLocation("uShadowBias")
        uShadowIntensityLocation = s.getUniformLocation("uShadowIntensity")
        uShadowMapSizeLocation = s.getUniformLocation("uShadowMapSize")
        s.unbind()
    }

    private fun cacheTexturedUniformLocations() {
        val s = texturedShader ?: return
        s.bind()
        uTexViewProjectionLocation = s.getUniformLocation("uViewProjection")
        uTexTextureScaleLocation = s.getUniformLocation("uTextureScale")
        uTexTextureScrollLocation = s.getUniformLocation("uTextureScroll")
        uTexHasNormalsLocation = s.getUniformLocation("uHasNormals")
        uTexSunDirectionLocation = s.getUniformLocation("uSunDirection")
        uTexSunColorLocation = s.getUniformLocation("uSunColor")
        uTexAmbientLocation = s.getUniformLocation("uAmbient")
        uTexContrastLocation = s.getUniformLocation("uContrast")
        uTexFogStartLocation = s.getUniformLocation("uFogStart")
        uTexFogEndLocation = s.getUniformLocation("uFogEnd")
        uTexFogEnabledLocation = s.getUniformLocation("uFogEnabled")
        uTexFogColorLocation = s.getUniformLocation("uFogColor")
        uTexTextureLocation = s.getUniformLocation("uTexture")
        uTexHasTextureLocation = s.getUniformLocation("uHasTexture")
        uTexCombineModeLocation = s.getUniformLocation("uCombineMode")
        uTexAlphaFromVertexLocation = s.getUniformLocation("uAlphaFromVertex")
        uTexSunBrightnessLocation = s.getUniformLocation("uSunBrightness")
        uTexContrastPosLocation = s.getUniformLocation("uContrastPos")
        uTexContrastNegLocation = s.getUniformLocation("uContrastNeg")
        uTexWaterModeLocation = s.getUniformLocation("uWaterMode")
        uTexClipPlaneLocation = s.getUniformLocation("uClipPlane")
        uTexDepthFogLocation = s.getUniformLocation("uDepthFog")
        uTexInvViewProjectionLocation = s.getUniformLocation("uInvViewProjection")
        uTexViewportLocation = s.getUniformLocation("uViewport")
        uTexFramebufferSizeLocation = s.getUniformLocation("uFramebufferSize")
        uTexWaterNoiseLocation = s.getUniformLocation("uWaterNoise")
        uTexHasWaterNoiseLocation = s.getUniformLocation("uHasWaterNoise")
        uTexWaterColourLocation = s.getUniformLocation("uWaterColour")
        uTexStillFogLocation = s.getUniformLocation("uStillFog")
        uTexLapNoiseLocation = s.getUniformLocation("uLapNoise")
        uTexLapTurbulenceLocation = s.getUniformLocation("uLapTurbulence")
        uTexLapAmbientLocation = s.getUniformLocation("uLapAmbient")
        uTexWaterTimeLocation = s.getUniformLocation("uWaterTime")
        uTexRippleFogLocation = s.getUniformLocation("uRippleFog")
        if (uTexTextureLocation >= 0) GLWrapper.uniform1i(uTexTextureLocation, 0)
        if (uTexWaterNoiseLocation >= 0) GLWrapper.uniform1i(uTexWaterNoiseLocation, WATER_TEXTURE_UNIT)
        val shadowTex = s.getUniformLocation("uGroundShadowTex")
        if (shadowTex >= 0) GLWrapper.uniform1i(shadowTex, SHADOW_TEXTURE_UNIT)
        val sceneDepth = s.getUniformLocation("uSceneDepth")
        if (sceneDepth >= 0) GLWrapper.uniform1i(sceneDepth, SCENE_DEPTH_TEXTURE_UNIT)
        s.unbind()
    }

    fun setProjection(matrix: Matrix4f) {
        projectionMatrix.set(matrix)
    }

    fun setView(matrix: Matrix4f) {
        viewMatrix.set(matrix)
    }

    fun setLighting(direction: Vector3f, color: Vector3f, ambientIntensity: Float, contrastIntensity: Float) {
        sunDirection.set(direction).normalize()
        sunColor.set(color)
        ambient = ambientIntensity
        contrast = contrastIntensity
    }

    fun setFog(enabled: Boolean, start: Float, end: Float, r: Float, g: Float, b: Float) {
        fogEnabled = enabled
        fogStart = start
        fogEnd = end
        fogColor[0] = r
        fogColor[1] = g
        fogColor[2] = b
    }

    fun setShadowMapping(enabled: Boolean, shadowMap: Int, matrix: Matrix4f, bias: Float = 0.005f, intensity: Float = 0.5f, mapSize: Int = 2048) {
        shadowEnabled = enabled
        shadowMapTextureId = shadowMap
        shadowMatrix.set(matrix)
        shadowBias = bias
        shadowIntensity = intensity
        shadowMapSize = mapSize.toFloat()
    }

    fun invalidateBoundTexture() {
        boundTexture = -1
    }

    fun resetFrameCounters() {
        drawCallsThisFrame = 0
        trianglesThisFrame = 0
    }

    fun beginFrame() {
        check(isInitialized) { "Renderer not initialized" }

        resetFrameCounters()

        shader!!.bind()
        setLightingUniforms()
        setFogUniforms()
        setShadowUniforms()
    }

    fun endFrame() {
        closeGroundBatch()
        shader?.unbind()
    }

    fun closeGroundBatch() {
        if (!groundBatchOpen) return
        groundBatchOpen = false
        ShaderProgram.groundPassProgramId = -1
        ShaderProgram.releaseGroundPass = null
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)
        glDisableVertexAttribArray(3)
        externalVao?.unbind()
        if (boundTexture > 0) GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        if (boundWaterNoise > 0) {
            glActiveTexture(GL_TEXTURE0 + WATER_TEXTURE_UNIT)
            glBindTexture(GL_TEXTURE_3D, 0)
            glActiveTexture(GL_TEXTURE0)
        }
        boundWaterMode = -1
        boundWaterNoise = -1
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        texturedShader?.unbind()
        boundVertexVbo = 0
        boundTexture = -1
        boundHasNormals = -1
        boundCombineMode = Int.MIN_VALUE
        boundAlphaFromVertex = null
        boundTextureScale = Float.NaN
        boundTextureScrollU = Float.NaN
        boundTextureScrollV = Float.NaN
        boundStride = -1
        boundTexCoordSize = -1
    }

    private val releaseAction = Runnable { closeGroundBatch() }
    private var groundBatchOpen = false
    private var boundVertexVbo = 0
    private var boundStride = -1
    private var boundTexCoordSize = -1
    private var boundHasNormals = -1
    private var boundTexture = -1
    private var boundCombineMode = Int.MIN_VALUE
    private var boundAlphaFromVertex: Boolean? = null
    private var boundTextureScale = Float.NaN
    private var boundTextureScrollU = Float.NaN
    private var boundTextureScrollV = Float.NaN
    private var boundWaterMode = -1
    private var boundWaterNoise = -1

    private fun applyWater(water: GroundWater) {
        if (water.mode != boundWaterMode) {
            GLWrapper.uniform1i(uTexWaterModeLocation, water.mode)
            boundWaterMode = water.mode
        }
        if (water.mode == 0) return
        if (water.noiseTexture != boundWaterNoise) {
            glActiveTexture(GL_TEXTURE0 + WATER_TEXTURE_UNIT)
            glBindTexture(GL_TEXTURE_3D, water.noiseTexture)
            glActiveTexture(GL_TEXTURE0)
            GLWrapper.uniform1i(uTexHasWaterNoiseLocation, if (water.noiseTexture != 0) 1 else 0)
            boundWaterNoise = water.noiseTexture
        }
        glUniform1f(uTexWaterTimeLocation, water.time)
        when (water.mode) {
            3 -> {
                glUniform4f(uTexStillFogLocation, water.stillFog[0], water.stillFog[1], water.stillFog[2], water.stillFog[3])
                val c = water.colour
                glUniform3f(uTexWaterColourLocation, (c shr 16 and 0xff) / 255f, (c shr 8 and 0xff) / 255f, (c and 0xff) / 255f)
            }
            4 -> {
                glUniform2f(uTexRippleFogLocation, water.rippleFogStart, water.rippleFogDensity)
                glUniform4f(uTexStillFogLocation, water.stillFog[0], water.stillFog[1], water.stillFog[2], water.stillFog[3])
                val c = water.colour
                glUniform3f(uTexWaterColourLocation, (c shr 16 and 0xff) / 255f, (c shr 8 and 0xff) / 255f, (c and 0xff) / 255f)
            }
            2 -> {
                glUniform1f(uTexLapTurbulenceLocation, water.turbulence)
                glUniform3f(uTexLapAmbientLocation, water.lapAmbient[0], water.lapAmbient[1], water.lapAmbient[2])
                water.lapNoise?.let { glUniform2fv(uTexLapNoiseLocation, it) }
            }
        }
    }

    fun drawIndexed(vertices: ByteBuffer, indices: java.nio.ShortBuffer, indexCount: Int) {
        if (indexCount == 0) return

        projectionMatrix.mul(viewMatrix, modelViewProjectionMatrix)
        viewMatrix.normal(normalMatrix)

        shader!!.setUniformMatrix4(uMVPLocation, modelViewProjectionMatrix)
        shader!!.setUniformMatrix4(uMVLocation, viewMatrix)
        shader!!.setUniform("uNormalMatrix", normalMatrix)

        vbo!!.upload(vertices)
        ibo!!.upload(indices)

        vao!!.bind()
        ibo!!.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    fun drawTile(
        positions: FloatArray,
        normals: FloatArray,
        colors: ByteArray,
        indices: ShortArray,
        vertexCount: Int,
        indexCount: Int
    ) {
        if (indexCount == 0 || vertexCount == 0) return

        ensureVertexCapacity(vertexCount)
        ensureIndexCapacity(indexCount)

        val vb = vertexData!!
        val ib = indexData!!

        vb.clear()
        ib.clear()

        for (i in 0 until vertexCount) {
            vb.putFloat(positions[i * 3])
            vb.putFloat(positions[i * 3 + 1])
            vb.putFloat(positions[i * 3 + 2])

            vb.putFloat(normals[i * 3])
            vb.putFloat(normals[i * 3 + 1])
            vb.putFloat(normals[i * 3 + 2])

            vb.put(colors[i * 4])
            vb.put(colors[i * 4 + 1])
            vb.put(colors[i * 4 + 2])
            vb.put(colors[i * 4 + 3])
        }

        ib.put(indices, 0, indexCount)

        vb.flip()
        ib.flip()

        drawIndexed(vb, ib, indexCount)
    }

    fun getFrameStats(): FrameStats {
        return FrameStats(drawCallsThisFrame, trianglesThisFrame)
    }

    fun renderWithExternalVBOs(
        interleavedVboId: Int,
        stride: Int,
        texCoordSize: Int,
        indexData: ByteArray,
        indexByteCount: Int,
        indexCount: Int,
        textureHandle: Int,
        textureScale: Float,
        hasNormals: Boolean,
        viewProjection: Matrix44,
        isBigEndian: Boolean
    ) {
        if (indexCount == 0) return

        val s = texturedShader ?: return
        val vao = externalVao ?: return
        val ibo = externalIbo ?: return

        s.bind()

        val vp = viewProjection.elements ?: return
        val colMajor = FloatArray(16)
        for (i in 0..15) colMajor[i] = vp[i]
        colMajor[1] = -colMajor[1]
        colMajor[5] = -colMajor[5]
        colMajor[9] = -colMajor[9]
        colMajor[13] = -colMajor[13]
        glUniformMatrix4fv(uTexViewProjectionLocation, false, colMajor)

        s.setUniform(uTexTextureScaleLocation, textureScale)
        s.setUniform(uTexTextureScrollLocation, 0f, 0f)

        s.setUniform(uTexHasNormalsLocation, if (hasNormals) 1 else 0)
        s.setUniform(uTexSunDirectionLocation, sunDirection.x, sunDirection.y, sunDirection.z)
        s.setUniform("uSunColor", sunColor)
        uploadPointLights(s)
        uploadShadowUniforms(s)
        s.setUniform(uTexAmbientLocation, ambient)
        s.setUniform(uTexContrastLocation, contrast)

        s.setUniform(uTexFogEnabledLocation, if (fogEnabled) 1 else 0)
        s.setUniform(uTexFogStartLocation, fogStart)
        s.setUniform(uTexFogEndLocation, fogEnd)
        s.setUniform(uTexFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])

        val hasTexture = textureHandle > 0
        s.setUniform(uTexHasTextureLocation, if (hasTexture) 1 else 0)
        if (hasTexture) {
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureHandle)
            GLWrapper.texParameteri(GLWrapper.Constants.TEXTURE_2D, GLWrapper.Constants.TEXTURE_WRAP_S, GLWrapper.Constants.REPEAT)
            GLWrapper.texParameteri(GLWrapper.Constants.TEXTURE_2D, GLWrapper.Constants.TEXTURE_WRAP_T, GLWrapper.Constants.REPEAT)
            s.setUniform(uTexTextureLocation, 0)
        }

        vao.bind()

        GLWrapper.bindBuffer(GLWrapper.Constants.ARRAY_BUFFER, interleavedVboId)

        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L)

        glEnableVertexAttribArray(1)
        glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, stride, 12L)

        glEnableVertexAttribArray(2)
        glVertexAttribPointer(2, texCoordSize, GL_FLOAT, false, stride, 16L)

        val texCoordBytes = texCoordSize * 4

        if (hasNormals) {
            val normalOffset = 16L + texCoordBytes
            glEnableVertexAttribArray(3)
            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, normalOffset)
        } else {
            glDisableVertexAttribArray(3)
        }

        ensureIndexCapacity(indexCount)
        val shortBuffer = this.indexData!!
        shortBuffer.clear()
        for (i in 0 until indexCount) {
            val byteOffset = i * 2
            val idx = if (isBigEndian) {
                ((indexData[byteOffset].toInt() and 0xFF) shl 8) or (indexData[byteOffset + 1].toInt() and 0xFF)
            } else {
                (indexData[byteOffset].toInt() and 0xFF) or ((indexData[byteOffset + 1].toInt() and 0xFF) shl 8)
            }
            shortBuffer.put(idx.toShort())
        }
        shortBuffer.flip()
        ibo.upload(shortBuffer)

        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)
        glDisableVertexAttribArray(3)
        vao.unbind()

        if (hasTexture) {
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        }

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    private var modernVbo: Int = 0

    private var modernColorVbo: Int = 0

    private val vpScratch = FloatArray(16)

    fun renderWithModernData(
        vertexData: java.nio.ByteBuffer,
        stride: Int,
        texCoordSize: Int,
        indexShorts: ShortArray,
        indexCount: Int,
        textureHandle: Int,
        textureScale: Float,
        textureScrollU: Float,
        textureScrollV: Float,
        hasNormals: Boolean,
        viewProjection: Matrix44,
        colorVboId: Int = 0,
        colorData: java.nio.ByteBuffer? = null,
        combineMode: Int = 0,
        alphaFromVertex: Boolean = false,
        residentVertexVbo: Int = 0,
        residentIndexIbo: Int = 0,
        water: GroundWater = GroundWater.NONE
    ) {
        if (indexCount == 0) return

        val s = texturedShader ?: return
        val vao = externalVao ?: return
        val ibo = externalIbo ?: return

        val vertexVbo: Int
        if (residentVertexVbo != 0) {
            vertexVbo = residentVertexVbo
        } else {
            if (modernVbo == 0) modernVbo = glGenBuffers()
            vertexVbo = modernVbo
            vertexData.position(0)
            glBindBuffer(GL_ARRAY_BUFFER, vertexVbo)
            glBufferData(GL_ARRAY_BUFFER, vertexData, GL_DYNAMIC_DRAW)
        }

        if (!groundBatchOpen) {
            s.bind()

            val vp = viewProjection.elements ?: return
            val colMajor = vpScratch
            for (i in 0..15) colMajor[i] = vp[i]
            colMajor[1] = -colMajor[1]
            colMajor[5] = -colMajor[5]
            colMajor[9] = -colMajor[9]
            colMajor[13] = -colMajor[13]
            glUniformMatrix4fv(uTexViewProjectionLocation, false, colMajor)
            glUniform4f(uTexClipPlaneLocation, clipPlane[0], clipPlane[1], clipPlane[2], clipPlane[3])
            GLWrapper.uniform1i(uTexDepthFogLocation, if (depthFogTexture != 0) 1 else 0)
            if (depthFogTexture != 0) {
                depthFogInverse.set(colMajor).invert().get(depthFogInverseArray)
                glUniformMatrix4fv(uTexInvViewProjectionLocation, false, depthFogInverseArray)
                glGetIntegerv(GL_VIEWPORT, depthFogViewport)
                glUniform4f(uTexViewportLocation, depthFogViewport[0].toFloat(), depthFogViewport[1].toFloat(), depthFogViewport[2].toFloat(), depthFogViewport[3].toFloat())
                glUniform2f(uTexFramebufferSizeLocation, depthFogWidth.toFloat(), depthFogHeight.toFloat())
            }

            s.setUniform(uTexSunDirectionLocation, legacySunDirX, legacySunDirY, legacySunDirZ)
            s.setUniform("uSunColor", sunColor)
            uploadPointLights(s)
            shadowFrame++
            uploadShadowUniforms(s)
            s.setUniform(uTexAmbientLocation, ambient)
            s.setUniform(uTexContrastLocation, contrast)
            s.setUniform(uTexSunBrightnessLocation, legacySunBrightness)
            s.setUniform(uTexContrastPosLocation, legacyContrastPos)
            s.setUniform(uTexContrastNegLocation, legacyContrastNeg)
            s.setUniform(uTexTextureLocation, 0)
            s.setUniform("uRawColorDebug", if (RAW_COLOR_DEBUG) 1 else 0)

            s.setUniform(uTexFogEnabledLocation, if (fogEnabled) 1 else 0)
            s.setUniform(uTexFogStartLocation, fogStart)
            s.setUniform(uTexFogEndLocation, fogEnd)
            s.setUniform(uTexFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])

            glEnable(GL_DEPTH_TEST)
            glDepthFunc(GL_LEQUAL)
            glDepthMask(true)
            glDisable(GL_CULL_FACE)
            glColorMask(true, true, true, true)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

            vao.bind()
            glEnableVertexAttribArray(0)
            glEnableVertexAttribArray(1)
            glEnableVertexAttribArray(2)

            groundBatchOpen = true
            ShaderProgram.groundPassProgramId = s.programId
            ShaderProgram.releaseGroundPass = releaseAction
            boundVertexVbo = 0
            boundHasNormals = -1
            boundTexture = -1
            boundCombineMode = Int.MIN_VALUE
            boundAlphaFromVertex = null
            boundTextureScale = Float.NaN
            boundTextureScrollU = Float.NaN
            boundTextureScrollV = Float.NaN
            boundWaterMode = -1
            boundWaterNoise = -1
        }

        if (currentShadowMapper !== boundShadowMapper) uploadShadowUniforms(s)

        val texCoordBytes = texCoordSize * 4
        if (vertexVbo != boundVertexVbo || stride != boundStride || texCoordSize != boundTexCoordSize) {
            glBindBuffer(GL_ARRAY_BUFFER, vertexVbo)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L)
            glVertexAttribPointer(2, texCoordSize, GL_FLOAT, false, stride, 16L)
            boundVertexVbo = vertexVbo
            boundStride = stride
            boundTexCoordSize = texCoordSize
            boundHasNormals = -1
        }
        val hasNormalsFlag = if (hasNormals) 1 else 0
        if (hasNormalsFlag != boundHasNormals) {
            s.setUniform(uTexHasNormalsLocation, hasNormalsFlag)
            if (hasNormals) {
                glBindBuffer(GL_ARRAY_BUFFER, vertexVbo)
                glEnableVertexAttribArray(3)
                glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 16L + texCoordBytes)
            } else {
                glDisableVertexAttribArray(3)
            }
            boundHasNormals = hasNormalsFlag
        }

        if (textureScale != boundTextureScale) {
            s.setUniform(uTexTextureScaleLocation, textureScale)
            boundTextureScale = textureScale
        }
        if (textureScrollU != boundTextureScrollU || textureScrollV != boundTextureScrollV) {
            s.setUniform(uTexTextureScrollLocation, textureScrollU, textureScrollV)
            boundTextureScrollU = textureScrollU
            boundTextureScrollV = textureScrollV
        }
        val hasTexture = textureHandle > 0
        if (textureHandle != boundTexture) {
            s.setUniform(uTexHasTextureLocation, if (hasTexture) 1 else 0)
            if (hasTexture) {
                GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, textureHandle)
            }
            boundTexture = textureHandle
        }
        if (combineMode != boundCombineMode) {
            s.setUniform(uTexCombineModeLocation, combineMode)
            boundCombineMode = combineMode
        }
        if (alphaFromVertex != boundAlphaFromVertex) {
            s.setUniform(uTexAlphaFromVertexLocation, if (alphaFromVertex) 1 else 0)
            boundAlphaFromVertex = alphaFromVertex
        }

        applyWater(water)

        when {
            colorVboId != 0 -> {
                glBindBuffer(GL_ARRAY_BUFFER, colorVboId)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 4, 0L)
            }
            colorData != null -> {
                if (modernColorVbo == 0) modernColorVbo = glGenBuffers()
                colorData.position(0)
                glBindBuffer(GL_ARRAY_BUFFER, modernColorVbo)
                glBufferData(GL_ARRAY_BUFFER, colorData, GL_DYNAMIC_DRAW)
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 4, 0L)
            }
            else -> {
                glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, stride, 12L)
            }
        }

        if (residentIndexIbo != 0) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, residentIndexIbo)
        } else {
            ensureIndexCapacity(indexCount)
            val shortBuffer = this.indexData!!
            shortBuffer.clear()
            shortBuffer.put(indexShorts, 0, indexCount)
            shortBuffer.flip()
            ibo.upload(shortBuffer)
            ibo.bind()
        }

        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)

        drawCallsThisFrame++
        trianglesThisFrame += indexCount / 3
    }

    fun renderMinimapFloor(
        vertexVbo: Int,
        stride: Int,
        texCoordSize: Int,
        hasNormals: Boolean,
        indexShorts: ShortArray,
        indexCount: Int,
        vpColMajor: FloatArray
    ) {
        if (indexCount == 0 || vertexVbo == 0) return
        val s = texturedShader ?: return
        val vao = externalVao ?: return
        val ibo = externalIbo ?: return
        closeGroundBatch()
        s.bind()
        glUniformMatrix4fv(uTexViewProjectionLocation, false, vpColMajor)
        s.setUniform(uTexSunDirectionLocation, legacySunDirX, legacySunDirY, legacySunDirZ)
        s.setUniform("uSunColor", sunColor)
        s.setUniform(uTexSunBrightnessLocation, legacySunBrightness)
        s.setUniform(uTexContrastPosLocation, legacyContrastPos)
        s.setUniform(uTexContrastNegLocation, legacyContrastNeg)
        s.setUniform(uTexHasNormalsLocation, if (hasNormals) 1 else 0)
        s.setUniform(uTexHasTextureLocation, 0)
        s.setUniform(uTexCombineModeLocation, 0)
        s.setUniform(uTexTextureScaleLocation, 1f)
        s.setUniform(uTexTextureScrollLocation, 0f, 0f)
        s.setUniform(uTexFogEnabledLocation, 0)
        s.setUniform("uRawColorDebug", 0)
        GLWrapper.uniform1i(uTexWaterModeLocation, 0)
        val en = s.getUniformLocation("uGroundShadowEnabled")
        if (en >= 0) GLWrapper.uniform1i(en, 0)
        val pl = s.getUniformLocation("uPointLightCount")
        if (pl >= 0) GLWrapper.uniform1i(pl, 0)

        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        glDisable(GL_BLEND)
        glDisable(GL_CULL_FACE)
        glColorMask(true, true, true, true)

        vao.bind()
        glBindBuffer(GL_ARRAY_BUFFER, vertexVbo)
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L)
        glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, stride, 12L)
        glVertexAttribPointer(2, texCoordSize, GL_FLOAT, false, stride, 16L)
        if (hasNormals) {
            glEnableVertexAttribArray(3)
            glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 16L + texCoordSize * 4L)
        } else {
            glDisableVertexAttribArray(3)
        }
        ensureIndexCapacity(indexCount)
        val shortBuffer = this.indexData!!
        shortBuffer.clear()
        shortBuffer.put(indexShorts, 0, indexCount)
        shortBuffer.flip()
        ibo.upload(shortBuffer)
        ibo.bind()
        GLWrapper.drawElements(TRIANGLES, indexCount, UNSIGNED_SHORT, 0)
        vao.unbind()
        s.unbind()
        glDepthMask(true)
        boundVertexVbo = 0
        boundHasNormals = -1
        boundTexture = -1
        boundCombineMode = Int.MIN_VALUE
        boundAlphaFromVertex = null
        boundTextureScale = Float.NaN
        boundTextureScrollU = Float.NaN
        boundTextureScrollV = Float.NaN
        boundShadowMapper = null
    }

    fun destroy() {
        if (modernVbo != 0) {
            glDeleteBuffers(modernVbo)
            modernVbo = 0
        }
        if (modernColorVbo != 0) {
            glDeleteBuffers(modernColorVbo)
            modernColorVbo = 0
        }
        shader?.destroy()
        vao?.destroy()
        vbo?.destroy()
        ibo?.destroy()

        texturedShader?.destroy()
        externalVao?.destroy()
        externalIbo?.destroy()

        vertexData?.let { MemoryUtil.memFree(it) }
        indexData?.let { MemoryUtil.memFree(it) }
        shadowUploadScratch?.let { MemoryUtil.memFree(it) }
        shadowUploadScratch = null
        for (entry in shadowEntries.values) if (entry.tex != 0) glDeleteTextures(entry.tex)
        shadowEntries.clear()
        currentShadowMapper = null
        boundShadowMapper = null

        shader = null
        vao = null
        vbo = null
        ibo = null
        texturedShader = null
        externalVao = null
        externalIbo = null
        vertexData = null
        indexData = null

        isInitialized = false
    }

    private fun setLightingUniforms() {
        val s = shader ?: return
        s.setUniform(uSunDirectionLocation, sunDirection.x, sunDirection.y, sunDirection.z)
        s.setUniform("uSunColor", sunColor)
        uploadPointLights(s)
        uploadShadowUniforms(s)
        s.setUniform(uAmbientLocation, ambient)
        s.setUniform(uContrastLocation, contrast)
    }

    private fun setFogUniforms() {
        val s = shader ?: return
        s.setUniform(uFogEnabledLocation, if (fogEnabled) 1 else 0)
        s.setUniform(uFogStartLocation, fogStart)
        s.setUniform(uFogEndLocation, fogEnd)
        s.setUniform(uFogColorLocation, fogColor[0], fogColor[1], fogColor[2], fogColor[3])
    }

    private fun setShadowUniforms() {
        val s = shader ?: return
        s.setUniform(uShadowEnabledLocation, if (shadowEnabled) 1 else 0)
        if (shadowEnabled) {
            s.setUniform("uShadowMatrix", shadowMatrix)
            s.setUniform(uShadowMapLocation, 1)
            s.setUniform(uShadowBiasLocation, shadowBias)
            s.setUniform(uShadowIntensityLocation, shadowIntensity)
            GLWrapper.uniform2f(uShadowMapSizeLocation, shadowMapSize, shadowMapSize)

            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0 + 1)
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, shadowMapTextureId)
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        }
    }

    private fun ensureVertexCapacity(vertexCount: Int) {
        val requiredBytes = vertexCount * BYTES_PER_VERTEX
        val vb = vertexData
        if (vb == null || requiredBytes > vb.capacity()) {
            vb?.let { MemoryUtil.memFree(it) }
            val newCapacity = (requiredBytes * 1.5).toInt().coerceAtLeast(DEFAULT_VERTEX_CAPACITY * BYTES_PER_VERTEX)
            vertexData = MemoryUtil.memAlloc(newCapacity)
        }
    }

    private fun ensureIndexCapacity(indexCount: Int) {
        val ib = indexData
        if (ib == null || indexCount > ib.capacity()) {
            ib?.let { MemoryUtil.memFree(it) }
            val newCapacity = (indexCount * 1.5).toInt().coerceAtLeast(DEFAULT_INDEX_CAPACITY)
            indexData = MemoryUtil.memAllocShort(newCapacity)
        }
    }

    data class FrameStats(
        val drawCalls: Int,
        val triangles: Int
    )
}
