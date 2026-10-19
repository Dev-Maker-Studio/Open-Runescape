package com.jagex.game.runetek5.graphics.modern.renderer

import com.jagex.game.runetek5.client.PreferenceManager
import org.joml.Matrix4f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL40.*

class HdWaterRenderer {

    private class Draw {
        var vertexVbo = 0
        var stride = 0
        var texCoordSize = 2
        var colorVbo = 0
        var indexIbo = 0
        var indexCount = 0
        val viewProjection = FloatArray(16)
        var colour = 0
        var scale = 0
        var shadowTexture = 0
        var shadowScale = 0f
        var flowU = 0f
        var flowV = 0f
        var planeY = Float.NaN
    }

    private class Program(val id: Int, val tessellated: Boolean) {
        private val locations = HashMap<String, Int>()
        fun at(name: String): Int = locations.getOrPut(name) {
            val location = glGetUniformLocation(id, name)
            if (location >= 0 || !name.endsWith("]")) location else glGetUniformLocation(id, name.substringBefore('['))
        }
    }

    private val queue = ArrayList<Draw>()
    private var queued = 0
    private var program: Program? = null
    private var vao = 0
    private var copyFbo = 0
    private var colorTexture = 0
    private var depthTexture = 0
    private var copyWidth = 0
    private var copyHeight = 0
    private val inverse = Matrix4f()
    private val inverseArray = FloatArray(16)
    private val viewport = IntArray(4)
    private val binding = IntArray(1)
    private var reflectionFbo = 0
    private var reflectionTexture = 0
    private var reflectionDepth = 0
    private var reflectionWidth = 0
    private var reflectionHeight = 0
    private val savedViewport = IntArray(4)
    private var savedFramebuffer = 0
    private var reflectionPlane = Float.NaN
    private val savedClearColour = FloatArray(4)

    val hasQueued: Boolean get() = queued > 0

    fun initialize() {
        if (program != null) return
        program = build("/shaders/hdWater.frag")
        vao = glGenVertexArrays()
        println("[HdWater] ${if (program == null) "unavailable" else if (program!!.tessellated) "tessellated waves" else "flat surface"}")
    }

    private fun build(fragmentPath: String): Program? {
        val fragment = resource(fragmentPath)
        return if (TESSELLATION && GL.getCapabilities().OpenGL40) {
            link(
                GL_VERTEX_SHADER to resource("/shaders/hdWaterTess.vert"),
                GL_TESS_CONTROL_SHADER to resource("/shaders/hdWater.tesc"),
                GL_TESS_EVALUATION_SHADER to resource("/shaders/hdWater.tese"),
                GL_FRAGMENT_SHADER to fragment.replaceFirst("#version 330 core", "#version 400 core")
            )?.let { Program(it, true) }
        } else {
            null
        } ?: link(
            GL_VERTEX_SHADER to resource("/shaders/hdWater.vert"),
            GL_FRAGMENT_SHADER to fragment
        )?.let { Program(it, false) }
    }

    fun queue(
        vertexVbo: Int, stride: Int, texCoordSize: Int, colorVbo: Int, indexIbo: Int, indexCount: Int, viewProjection: FloatArray,
        colour: Int, scale: Int, shadowTexture: Int, shadowScale: Float, flowU: Float, flowV: Float, planeY: Float
    ) {
        if (queued == queue.size) queue.add(Draw())
        val draw = queue[queued++]
        draw.vertexVbo = vertexVbo
        draw.stride = stride
        draw.texCoordSize = texCoordSize
        draw.colorVbo = colorVbo
        draw.indexIbo = indexIbo
        draw.indexCount = indexCount
        System.arraycopy(viewProjection, 0, draw.viewProjection, 0, 16)
        draw.colour = colour
        draw.scale = scale
        draw.shadowTexture = shadowTexture
        draw.shadowScale = shadowScale
        draw.flowU = flowU
        draw.flowV = flowV
        draw.planeY = planeY
    }

    fun dominantPlane(): Float {
        var best = Float.NaN
        var bestCount = -1
        for (i in 0 until queued) {
            val draw = queue[i]
            if (!draw.planeY.isNaN() && draw.indexCount > bestCount) {
                bestCount = draw.indexCount
                best = draw.planeY
            }
        }
        return best
    }

    fun beginReflection(framebufferWidth: Int, framebufferHeight: Int, fogColour: Int, planeY: Float): Boolean {
        if (!PLANAR || framebufferWidth <= 0 || framebufferHeight <= 0 || planeY.isNaN()) {
            reflectionPlane = Float.NaN
            return false
        }
        val width = maxOf(1, framebufferWidth / 2)
        val height = maxOf(1, framebufferHeight / 2)
        ensureReflectionTarget(width, height)
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, binding)
        savedFramebuffer = binding[0]
        glGetIntegerv(GL_VIEWPORT, savedViewport)
        glBindFramebuffer(GL_FRAMEBUFFER, reflectionFbo)
        glViewport(savedViewport[0] / 2, savedViewport[1] / 2, maxOf(1, savedViewport[2] / 2), maxOf(1, savedViewport[3] / 2))
        glColorMask(true, true, true, true)
        glDepthMask(true)
        glGetFloatv(GL_COLOR_CLEAR_VALUE, savedClearColour)
        glClearColor((fogColour shr 16 and 0xff) / 255f, (fogColour shr 8 and 0xff) / 255f, (fogColour and 0xff) / 255f, 0f)
        glClearDepth(1.0)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glEnable(GL_CLIP_DISTANCE0)
        reflectionPlane = planeY
        return true
    }

    fun endReflection() {
        glDisable(GL_CLIP_DISTANCE0)
        glClearColor(savedClearColour[0], savedClearColour[1], savedClearColour[2], savedClearColour[3])
        glBindFramebuffer(GL_FRAMEBUFFER, savedFramebuffer)
        glViewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3])
    }

    private fun ensureReflectionTarget(width: Int, height: Int) {
        if (reflectionFbo != 0 && width == reflectionWidth && height == reflectionHeight) return
        if (reflectionFbo == 0) {
            reflectionFbo = glGenFramebuffers()
            reflectionTexture = glGenTextures()
            reflectionDepth = glGenRenderbuffers()
        }
        glBindTexture(GL_TEXTURE_2D, reflectionTexture)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L)
        sampling(GL_LINEAR)
        glBindTexture(GL_TEXTURE_2D, 0)
        glBindRenderbuffer(GL_RENDERBUFFER, reflectionDepth)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width, height)
        glBindRenderbuffer(GL_RENDERBUFFER, 0)
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, binding)
        glBindFramebuffer(GL_FRAMEBUFFER, reflectionFbo)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, reflectionTexture, 0)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, reflectionDepth)
        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (status != GL_FRAMEBUFFER_COMPLETE) System.err.println("[HdWater] reflection target incomplete: 0x${Integer.toHexString(status)}")
        glBindFramebuffer(GL_FRAMEBUFFER, binding[0])
        reflectionWidth = width
        reflectionHeight = height
    }

    fun clear() {
        queued = 0
    }

    fun render(
        framebufferWidth: Int,
        framebufferHeight: Int,
        sunDirection: FloatArray,
        sunColour: FloatArray,
        ambient: Float,
        skyColour: Int,
        waveNoise: Int,
        foamNoise: Int,
        envMap: Int,
        time: Float,
        slowTime: Float,
        fogEnabled: Boolean,
        fogStart: Float,
        fogEnd: Float,
        pointLightCount: Int,
        pointLightPositions: FloatArray,
        pointLightColours: FloatArray
    ) {
        if (queued == 0) return
        if (program == null || framebufferWidth <= 0 || framebufferHeight <= 0) {
            queued = 0
            return
        }
        copyScene(framebufferWidth, framebufferHeight)
        glGetIntegerv(GL_VIEWPORT, viewport)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, colorTexture)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, depthTexture)
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_3D, waveNoise)
        glActiveTexture(GL_TEXTURE3)
        glBindTexture(GL_TEXTURE_CUBE_MAP, envMap)
        glActiveTexture(GL_TEXTURE4)
        glBindTexture(GL_TEXTURE_3D, foamNoise)
        glActiveTexture(GL_TEXTURE6)
        glBindTexture(GL_TEXTURE_2D, if (reflectionPlane.isNaN()) 0 else reflectionTexture)

        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glDepthMask(true)
        glDisable(GL_CULL_FACE)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glBindVertexArray(vao)
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnableVertexAttribArray(2)
        val p = program!!
        glUseProgram(p.id)
        if (p.tessellated) glPatchParameteri(GL_PATCH_VERTICES, 3)
        glUniform1i(p.at("uSceneColor"), 0)
        glUniform1i(p.at("uSceneDepth"), 1)
        glUniform1i(p.at("uWaveNoise"), 2)
        glUniform1i(p.at("uEnvMap"), 3)
        glUniform1i(p.at("uFoamNoise"), 4)
        glUniform1i(p.at("uShadowTex"), 5)
        glUniform1i(p.at("uReflection"), 6)
        glUniform1f(p.at("uPlanarDistort"), PLANAR_DISTORT)
        glUniform1i(p.at("uHasWaveNoise"), if (waveNoise != 0) 1 else 0)
        glUniform1i(p.at("uHasFoamNoise"), if (foamNoise != 0) 1 else 0)
        glUniform1i(p.at("uHasEnvMap"), if (envMap != 0) 1 else 0)
        glUniform1f(p.at("uAmbient"), ambient)
        glUniform1f(p.at("uScatter"), SCATTER)
        glUniform1i(p.at("uReflectionSteps"), REFLECTION_STEPS)
        glUniform1f(p.at("uWaveStrength"), WAVE_STRENGTH)
        glUniform1f(p.at("uWaveAmplitude"), WAVE_AMPLITUDE)
        glUniform1f(p.at("uTessPixels"), TESS_PIXELS)
        val lights = pointLightCount.coerceIn(0, MAX_POINT_LIGHTS).coerceAtMost(pointLightPositions.size / 4).coerceAtMost(pointLightColours.size / 3)
        glUniform1i(p.at("uPointLightCount"), lights)
        if (lights > 0) {
            glUniform4fv(p.at("uPointLightPos[0]"), pointLightPositions.copyOf(lights * 4))
            glUniform3fv(p.at("uPointLightColor[0]"), pointLightColours.copyOf(lights * 3))
        }
        glUniform4f(p.at("uViewport"), viewport[0].toFloat(), viewport[1].toFloat(), viewport[2].toFloat(), viewport[3].toFloat())
        glUniform2f(p.at("uFramebufferSize"), framebufferWidth.toFloat(), framebufferHeight.toFloat())
        val length = Math.sqrt((sunDirection[0] * sunDirection[0] + sunDirection[1] * sunDirection[1] + sunDirection[2] * sunDirection[2]).toDouble()).toFloat().coerceAtLeast(1e-4f)
        glUniform3f(p.at("uSunDirection"), sunDirection[0] / length, sunDirection[1] / length, sunDirection[2] / length)
        glUniform3f(p.at("uSunColor"), sunColour[0], sunColour[1], sunColour[2])
        glUniform3f(p.at("uSkyColour"), (skyColour shr 16 and 0xff) / 255f, (skyColour shr 8 and 0xff) / 255f, (skyColour and 0xff) / 255f)
        glUniform1f(p.at("uRefraction"), REFRACTION)
        glUniform1f(p.at("uWaveScale"), WAVE_SCALE)
        glUniform1f(p.at("uTime"), time)
        glUniform1f(p.at("uSlowTime"), slowTime)
        glUniform1i(p.at("uFogEnabled"), if (fogEnabled) 1 else 0)
        glUniform1f(p.at("uFogStart"), fogStart)
        glUniform1f(p.at("uFogEnd"), fogEnd)
        for (i in 0 until queued) {
            val draw = queue[i]
            glUniformMatrix4fv(p.at("uViewProjection"), false, draw.viewProjection)
            inverse.set(draw.viewProjection).invert().get(inverseArray)
            glUniformMatrix4fv(p.at("uInvViewProjection"), false, inverseArray)
            val c = draw.colour
            glUniform3f(p.at("uWaterColour"), (c shr 16 and 0xff) / 255f, (c shr 8 and 0xff) / 255f, (c and 0xff) / 255f)
            glUniform1f(p.at("uAbsorbDistance"), draw.scale.coerceAtLeast(1) * ABSORB_PER_SCALE)
            glUniform2f(p.at("uFlow"), draw.flowU, draw.flowV)
            glUniform1i(p.at("uShadowEnabled"), if (draw.shadowTexture != 0) 1 else 0)
            glUniform1f(p.at("uShadowScale"), draw.shadowScale)
            glUniform1i(p.at("uHasWaterDepth"), if (draw.texCoordSize >= 3) 1 else 0)
            glUniform1i(p.at("uUsePlanar"), if (!reflectionPlane.isNaN() && Math.abs(draw.planeY - reflectionPlane) < PLANAR_TOLERANCE) 1 else 0)
            glActiveTexture(GL_TEXTURE5)
            glBindTexture(GL_TEXTURE_2D, draw.shadowTexture)
            glBindBuffer(GL_ARRAY_BUFFER, draw.vertexVbo)
            glVertexAttribPointer(0, 3, GL_FLOAT, false, draw.stride, 0L)
            glVertexAttribPointer(2, draw.texCoordSize, GL_FLOAT, false, draw.stride, 16L)
            glBindBuffer(GL_ARRAY_BUFFER, draw.colorVbo)
            glVertexAttribPointer(1, 4, GL_UNSIGNED_BYTE, true, 4, 0L)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, draw.indexIbo)
            glDrawElements(if (p.tessellated) GL_PATCHES else GL_TRIANGLES, draw.indexCount, GL_UNSIGNED_SHORT, 0L)
        }
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glDisableVertexAttribArray(2)
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glActiveTexture(GL_TEXTURE6)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE5)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE4)
        glBindTexture(GL_TEXTURE_3D, 0)
        glActiveTexture(GL_TEXTURE3)
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0)
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_3D, 0)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, 0)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glUseProgram(0)
        queued = 0
        reflectionPlane = Float.NaN
    }

    fun copySceneDepth(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 0
        copyScene(width, height)
        return depthTexture
    }

    private fun copyScene(width: Int, height: Int) {
        ensureCopyTarget(width, height)
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, binding)
        val scene = binding[0]
        glBindFramebuffer(GL_READ_FRAMEBUFFER, scene)
        if (scene == 0) glReadBuffer(GL_BACK)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, copyFbo)
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT, GL_NEAREST)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, scene)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, scene)
    }

    private fun ensureCopyTarget(width: Int, height: Int) {
        if (copyFbo != 0 && width == copyWidth && height == copyHeight) return
        if (copyFbo == 0) {
            copyFbo = glGenFramebuffers()
            colorTexture = glGenTextures()
            depthTexture = glGenTextures()
        }
        glBindTexture(GL_TEXTURE_2D, colorTexture)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L)
        sampling(GL_LINEAR)
        glBindTexture(GL_TEXTURE_2D, depthTexture)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH24_STENCIL8, width, height, 0, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8, 0L)
        sampling(GL_NEAREST)
        glBindTexture(GL_TEXTURE_2D, 0)
        glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, binding)
        glBindFramebuffer(GL_FRAMEBUFFER, copyFbo)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTexture, 0)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthTexture, 0)
        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        if (status != GL_FRAMEBUFFER_COMPLETE) System.err.println("[HdWater] scene copy target incomplete: 0x${Integer.toHexString(status)}")
        glBindFramebuffer(GL_FRAMEBUFFER, binding[0])
        copyWidth = width
        copyHeight = height
    }

    private fun sampling(filter: Int) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }

    private fun resource(path: String): String =
        HdWaterRenderer::class.java.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: error("missing shader resource $path")

    private fun link(vararg stages: Pair<Int, String>): Int? {
        val shaders = ArrayList<Int>()
        for ((type, source) in stages) {
            val shader = glCreateShader(type)
            glShaderSource(shader, source)
            glCompileShader(shader)
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                System.err.println("[HdWater] shader compile failed (type 0x${Integer.toHexString(type)}): ${glGetShaderInfoLog(shader)}")
                glDeleteShader(shader)
                shaders.forEach { glDeleteShader(it) }
                return null
            }
            shaders.add(shader)
        }
        val id = glCreateProgram()
        shaders.forEach { glAttachShader(id, it) }
        glLinkProgram(id)
        shaders.forEach { glDetachShader(id, it); glDeleteShader(it) }
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE) {
            System.err.println("[HdWater] program link failed: ${glGetProgramInfoLog(id)}")
            glDeleteProgram(id)
            return null
        }
        return id
    }

    fun destroy() {
        program?.let { glDeleteProgram(it.id) }
        program = null
        if (vao != 0) glDeleteVertexArrays(vao)
        if (reflectionFbo != 0) {
            glDeleteFramebuffers(reflectionFbo)
            glDeleteTextures(reflectionTexture)
            glDeleteRenderbuffers(reflectionDepth)
            reflectionFbo = 0
        }
        if (copyFbo != 0) {
            glDeleteFramebuffers(copyFbo)
            glDeleteTextures(colorTexture)
            glDeleteTextures(depthTexture)
        }
        vao = 0
        copyFbo = 0
    }

    companion object {
        private val FORCED = System.getProperty("render.hdwater")?.lowercase()?.toBooleanStrictOrNull()
        @JvmField val AVAILABLE = FORCED != false

        fun selected(): Boolean {
            FORCED?.let { return it }
            return PreferenceManager.preferences?.water?.waterShaders() == true
        }
        private const val MAX_POINT_LIGHTS = 128
        private val TESSELLATION = System.getProperty("render.hdwater.tessellation")?.equals("false", true) != true
        @JvmField val PLANAR = System.getProperty("render.hdwater.planar")?.equals("false", true) != true
        private val PLANAR_DISTORT = System.getProperty("render.hdwater.planardistort")?.toFloatOrNull() ?: 0.03f
        private const val PLANAR_TOLERANCE = 24f
        private val ABSORB_PER_SCALE = System.getProperty("render.hdwater.absorb")?.toFloatOrNull() ?: 40f
        private val SCATTER = System.getProperty("render.hdwater.scatter")?.toFloatOrNull() ?: 0.6f
        private val REFLECTION_STEPS = System.getProperty("render.hdwater.ssr")?.toIntOrNull() ?: 24
        private val WAVE_STRENGTH = System.getProperty("render.hdwater.waves")?.toFloatOrNull() ?: 1.3f
        private val WAVE_AMPLITUDE = System.getProperty("render.hdwater.amplitude")?.toFloatOrNull() ?: 18f
        private val TESS_PIXELS = System.getProperty("render.hdwater.tesspixels")?.toFloatOrNull() ?: 24f
        private val REFRACTION = System.getProperty("render.hdwater.refract")?.toFloatOrNull() ?: 24f
        private val WAVE_SCALE = System.getProperty("render.hdwater.wavescale")?.toFloatOrNull() ?: 2048f
        @JvmField val FLOW_PERIOD = WAVE_SCALE * 100f
    }
}
