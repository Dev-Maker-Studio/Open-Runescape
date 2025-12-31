package com.jagex.game.runetek5.toolkit

import jaclib.memory.Stream
import jaggl.MapBuffer
import jaggl.OpenGL
import java.awt.*
import com.jagex.game.runetek5.buffer.OpenGlNativeRenderBuffer
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.shader.Shader
import com.jagex.game.runetek5.graphics.shader.ShaderGL
import com.jagex.game.runetek5.graphics.shader.ShaderLoader
import com.jagex.game.runetek5.graphics.shader.ShaderMappingGL
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.TextureStageState
import com.jagex.game.runetek5.graphics.texture.TextureState
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapRectangleTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler2D
import com.jagex.game.runetek5.graphics.texture.sampler.MipmapTextureSampler3D
import com.jagex.game.runetek5.graphics.texture.sampler.NativeTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.OpenGLCubeTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.RectangleTextureSampler
import com.jagex.game.runetek5.graphics.texture.sampler.Texture3DSampler
import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.imageproducer.ImageProducerGL2
import jaclib.memory.heap.NativeHeap
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.rendering.RenderInfo
import com.jagex.game.runetek5.toolkit.companion.BlendFuncMode
import com.jagex.game.runetek5.toolkit.companion.NativeStream
import com.jagex.game.runetek5.toolkit.companion.PrimitiveDrawMode
import com.jagex.game.runetek5.toolkit.companion.TextureCombineMode
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArbArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArbElementArrayBuffer
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclaration
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationAction
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationComponent
import com.jagex.game.runetek5.toolkit.vertexdeclaration.VertexDeclarationGL
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.toolkit.opengl.*
open class OpenGLNativeToolkit(
    opengl1: OpenGL,
    canvas2: Canvas,
    long_3: Long,
    interface225: MaterialProvider,
    index6: Js5ResourceProvider,
    i_7: Int
) : AbstractNativeToolkit(interface225, index6, i_7, 1) {

    @JvmField
    var activeShaderMapping: ShaderMappingGL? = null

    @JvmField
    val displayListsToDelete = LinkedList()

    @JvmField
    val textureBuffersToDelete = LinkedList()

    @JvmField
    val texturesToDelete = LinkedList()

    @JvmField
    val frameBuffersToDelete = LinkedList()

    @JvmField
    val renderBuffersToDelete = LinkedList()

    @JvmField
    val programsToDelete = LinkedList()

    @JvmField
    val shadersToDelete = LinkedList()

    @JvmField
    val textureIdsToDelete = IntArray(1000)

    @JvmField
    val aClass326Sub1Array10186 = arrayOfNulls<OpenGlArbArrayBuffer>(16)

    /**
     * Used to store either the fog position / color
     */
    @JvmField
    val parameters: FloatArray

    @JvmField
    val currentTextureMatrix: FloatArray

    @JvmField
    var opengl: OpenGL? = null

    @JvmField
    var gpuVendor: String? = null

    @JvmField
    var gpuManufactorer: String? = null

    @JvmField
    var combinedOpenglVersion: Int = 0

    @JvmField
    var GLARBVertexBufferObject: Boolean = false

    @JvmField
    var GLARBTextureRectangle: Boolean = false

    @JvmField
    var GLARBTextureNonPowerOfTwo: Boolean = false

    @JvmField
    var GLARBVertexShader: Boolean = false

    @JvmField
    var GLARBFragmentShader: Boolean = false

    @JvmField
    var textureSlots: IntArray? = null

    @JvmField
    var pixelType: Int = 0

    @JvmField
    var aClass326Sub210187: OpenGlArbElementArrayBuffer? = null

    @JvmField
    var lastGcTime: Long = 0

    @JvmField
    var enableVertexArray: Boolean = false

    @JvmField
    var enableVertexNormal: Boolean = false

    @JvmField
    var enableVertexColor: Boolean = false

    @JvmField
    var textureAllocationPosition: Int = 0

    init {
        MapBuffer()
        MapBuffer()
        parameters = FloatArray(4)
        currentTextureMatrix = FloatArray(16)
        val ints_8 = IntArray(1)

        try {
            opengl = opengl1
            opengl!!.attach()
            gpuVendor = OpenGL.glGetString(GL_VENDOR).lowercase()
            gpuManufactorer = OpenGL.glGetString(GL_RENDERER).lowercase()

            if (gpuVendor!!.indexOf("microsoft") == -1 && gpuVendor!!.indexOf("brian paul") == -1 && gpuVendor!!.indexOf("mesa") == -1) {
                val openglVersion = OpenGL.glGetString(GL_VERSION)
                val openglVersionParts = Utils.split(openglVersion.replace('.', ' '), ' ')

                if (openglVersionParts.size < 2) {
                    throw RuntimeException("")
                }

                try {
                    val majorVersion = Utils.parseInt(openglVersionParts[0])
                    val minorVersion = Utils.parseInt(openglVersionParts[1])
                    combinedOpenglVersion = majorVersion * 10 + minorVersion
                } catch (exception: NumberFormatException) {
                    throw RuntimeException("")
                }

                if (combinedOpenglVersion < 12) {
                    throw RuntimeException("")
                }

                OpenGL.glGetIntegerv(GL_MAX_TEXTURE_UNITS, ints_8, 0)
                maxConcurrentTextures = ints_8[0]

                if (maxConcurrentTextures < 2) {
                    throw RuntimeException("")
                }

                maxActiveLights = 8
                GLARBVertexBufferObject = opengl!!.supportsExtension("GL_ARB_vertex_buffer_object")
                GLARBMultisample = opengl!!.supportsExtension("GL_ARB_multisample")
                GLARBTextureRectangle = opengl!!.supportsExtension("GL_ARB_texture_rectangle")
                opengl!!.supportsExtension("GL_ARB_texture_cube_map")
                GLARBTextureNonPowerOfTwo = opengl!!.supportsExtension("GL_ARB_texture_non_power_of_two")
                SUPPORTS_TEXTURE3D = opengl!!.supportsExtension("GL_EXT_texture3D")
                GLARBVertexShader = opengl!!.supportsExtension("GL_ARB_vertex_shader")
                opengl!!.supportsExtension("GL_ARB_vertex_program")
                GLARBFragmentShader = opengl!!.supportsExtension("GL_ARB_fragment_shader")
                opengl!!.supportsExtension("GL_ARB_fragment_program")
                opengl!!.supportsExtension("GL_EXT_framebuffer_object")
                textureSlots = IntArray(maxConcurrentTextures)
                GLEXTFramebufferObject = opengl!!.supportsExtension("GL_EXT_framebuffer_object")
                opengl!!.supportsExtension("GL_EXT_framebuffer_blit")
                opengl!!.supportsExtension("GL_EXT_framebuffer_multisample")

                if (!supportsArbVertexShader() || !supportsArbFragmentShader()) {
                    if (!opengl!!.supportsExtension("GL_ARB_multitexture")) {
                        throw RuntimeException("")
                    }

                    if (!opengl!!.supportsExtension("GL_ARB_texture_env_combine")) {
                        throw RuntimeException("")
                    }
                }

                pixelType = if (Stream.bigEndianessEnabled()) GL_UNSIGNED_INT_8_8_8_8_REV else GL_UNSIGNED_BYTE

                if (gpuManufactorer!!.indexOf("radeon") != -1) {
                    var radeonCapabilities = 0
                    var isRadeonHd = false
                    var wrongJava = false
                    val gpuManufactorerParts = Utils.split(gpuManufactorer!!.replace('/', ' '), ' ')

                    for (i_15 in gpuManufactorerParts.indices) {
                        var manufactorerPart = gpuManufactorerParts[i_15]

                        try {
                            if (manufactorerPart.isNotEmpty()) {
                                if (manufactorerPart[0] == 'x' && manufactorerPart.length >= 3 && Utils.isWrongJava(manufactorerPart.substring(1, 3))) {
                                    manufactorerPart = manufactorerPart.substring(1)
                                    wrongJava = true
                                }

                                if ("hd" == manufactorerPart) {
                                    isRadeonHd = true
                                } else {
                                    if (manufactorerPart.startsWith("hd")) {
                                        manufactorerPart = manufactorerPart.substring(2)
                                        isRadeonHd = true
                                    }

                                    if (manufactorerPart.length >= 4 && Utils.isWrongJava(manufactorerPart.substring(0, 4))) {
                                        radeonCapabilities = Utils.parseInt(manufactorerPart.substring(0, 4))
                                        break
                                    }
                                }
                            }
                        } catch (exception: Exception) {
                            // NOOP
                        }
                    }

                    if (!wrongJava && !isRadeonHd) {
                        if (radeonCapabilities >= 7000 && radeonCapabilities <= 7999) {
                            GLARBVertexBufferObject = false
                        }

                        if (radeonCapabilities >= 7000 && radeonCapabilities <= 9250) {
                            SUPPORTS_TEXTURE3D = false
                        }
                    }

                    GLARBTextureRectangle = GLARBTextureRectangle and opengl!!.supportsExtension("GL_ARB_half_float_pixel")
                }

                gpuVendor!!.indexOf("intel")

                if (GLARBVertexBufferObject) {
                    try {
                        val buffers = IntArray(1)
                        OpenGL.glGenBuffersARB(1, buffers, 0)
                    } catch (throwable: Throwable) {
                        throw RuntimeException("")
                    }
                }

                // ImageProducer initialization - type mismatch, needs investigation
                // addImageProducer(canvas2, ImageProducerGL2(this, canvas2, long_3))
                setActiveCanvas(canvas2)
                generateNativeHeapBuffer(GL_QUERY_BUFFER_BARRIER_BIT, false)
                generateNativeHeapBuffer(GL_QUERY_BUFFER_BARRIER_BIT, false)

            } else {
                throw RuntimeException("")
            }
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            cleanupAndShutdown()
            if (throwable is OutOfMemoryError) {
                throw throwable
            } else {
                throw RuntimeException("")
            }
        }
    }

    override fun loadShader(string_1: String): Shader? {
        val shaderData = getGLShaderData(string_1) ?: return null
        val class1143 = createShader(shaderData)
        return ShaderGL(this, class1143)
    }

    override fun supportsArbVertexShader(): Boolean {
        return GLARBVertexShader
    }

    override fun supportsArbFragmentShader(): Boolean {
        return GLARBFragmentShader
    }

    override fun awaitRenderCompletion() {
        OpenGL.glFinish()
    }

    override fun shutdown() {
        super.shutdown()
        if (opengl != null) {
            opengl!!.detach()
            opengl!!.release()
            opengl = null
        }
    }

    override fun method13905(): Float {
        return 0.0f
    }

    override fun createDelegatingDisplay(canvas1: Canvas, i_2: Int, i_3: Int): DelegatingDisplay {
        throw UnsupportedOperationException("createDelegatingDisplay not implemented")
    }

    override fun getImageProducer(): DirectDisplay {
        throw UnsupportedOperationException("getImageProducer not implemented")
    }

    override fun copyPixelRectangle(i_1: Int, i_2: Int, i_3: Int, i_4: Int): IntArray {
        val ints_5 = IntArray(i_3 * i_4)
        val i_6 = display!!.getHeight()

        for (i_7 in 0 until i_4) {
            OpenGL.glReadPixelsi(i_1, i_6 - i_2 - i_7 - 1, i_3, 1, 32993, pixelType, ints_5, i_3 * i_7)
        }

        return ints_5
    }

    override fun drawQuad(class1521: AbstractCubeTextureGenerator?, class1522: AbstractCubeTextureGenerator?, f_3: Float, class1524: AbstractCubeTextureGenerator?): AbstractCubeTextureGenerator? {
        return if (f_3 < 0.5f) class1521 else class1522
    }

    override fun applyClearFlags(flags: Int, color: Int) {
        var mask = 0
        if ((flags and 0x1) != 0) {
            OpenGL.glClearColor((color and 0xff0000) / 1.671168E7f, (color and 0xff00) / 65280.0f, (color and 0xff) / 255.0f, (color ushr 24) / 255.0f)
            mask = 16384 // GL_COLOR_BUFFER_BIT
        }

        if ((flags and 0x2) != 0) {
            method13942(true)
            mask = mask or 0x100 // GL_DEPTH_BUFFER_BIT
        }

        if ((flags and 0x4) != 0) {
            mask = mask or 0x400 // GL_STENCIL_BUFFER_BIT
        }

        OpenGL.glClear(mask)
    }

    override fun setViewport() {
        if (display != null) {
            OpenGL.glViewport(anInt8823 + anInt8749, anInt8754 + display!!.getHeight() - anInt8776 - anInt8752, anInt8751, anInt8752)
        }

        OpenGL.glDepthRange(depthRangeNear, depthRangeFar)
    }

    override fun toggleDepthTest() {
        if (enableDepthTest) {
            OpenGL.glEnable(GL_DEPTH_TEST)
        } else {
            OpenGL.glDisable(GL_DEPTH_TEST)
        }
    }

    override fun setDepthRenderState() {
        OpenGL.glDepthMask(depthBufferingEnabled && aBool8756)
    }

    override fun toggleLights() {
        if (aBool8684 && !aBool8760) {
            OpenGL.glEnable(GL_LIGHTING)
        } else {
            OpenGL.glDisable(GL_LIGHTING)
        }
    }

    override fun setSunParameters() {
        parameters[0] = sunColorIntensity * baseLightColorRed
        parameters[1] = sunColorIntensity * baseLightColorGreen
        parameters[2] = sunColorIntensity * baseLightColorBlue
        parameters[3] = 1.0f
        OpenGL.glLightfv(GL_LIGHT0, GL_DIFFUSE, parameters, 0)
        parameters[0] = -antiSunColorIntensity * baseLightColorRed
        parameters[1] = -antiSunColorIntensity * baseLightColorGreen
        parameters[2] = -antiSunColorIntensity * baseLightColorBlue
        parameters[3] = 1.0f
        OpenGL.glLightfv(GL_LIGHT1, GL_DIFFUSE, parameters, 0)
    }

    override fun loadWorldViewProjectionMatrix(matrix441: Matrix44) {
        val floats_2 = matrix441.faceList
        floats_2[1] = -floats_2[1]
        floats_2[5] = -floats_2[5]
        floats_2[9] = -floats_2[9]
        floats_2[13] = -floats_2[13]
    }

    override fun toggleSun() {
        if (sunEnabled) {
            OpenGL.glEnable(GL_LIGHT0)
            OpenGL.glEnable(GL_LIGHT1)
        } else {
            OpenGL.glDisable(GL_LIGHT0)
            OpenGL.glDisable(GL_LIGHT1)
        }
    }

    override fun enableLights() {
    }

    override fun configureLights() {
        OpenGL.glMatrixMode(GL_MODELVIEW)
        OpenGL.glLoadMatrixf(particleMatrix.faceList, 0)

        var index = 0
        while (index < lastSceneLightCount) {
            val lightSource = lightSources!![index]
            val color = lightSource.color
            val lightUnit = index + GL_LIGHT2
            val alpha = lightSource.getAlpha() / 255.0f
            parameters[0] = lightSource.getX().toFloat()
            parameters[1] = lightSource.getY().toFloat()
            parameters[2] = lightSource.getZ().toFloat()
            parameters[3] = 1.0f
            OpenGL.glLightfv(lightUnit, GL_POSITION, parameters, 0)
            parameters[0] = (color shr 16 and 0xff) * alpha
            parameters[1] = (color shr 8 and 0xff) * alpha
            parameters[2] = (color and 0xff) * alpha
            parameters[3] = 1.0f
            OpenGL.glLightfv(lightUnit, GL_DIFFUSE, parameters, 0)
            OpenGL.glLightf(lightUnit, GL_QUADRATIC_ATTENUATION, 1.0f / (lightSource.size2d * lightSource.size2d))
            OpenGL.glEnable(lightUnit)
            index++
        }

        while (index < currentSceneLightCount) {
            OpenGL.glDisable(GL_LIGHT2 + index++)
        }
    }

    override fun method13955(class1501: GLFormat, class762: GLActionFormat): Boolean {
        return SUPPORTS_TEXTURE3D
    }

    override fun method14072(class1501: GLFormat, class762: GLActionFormat, i_3: Int, i_4: Int): NativeTextureSampler {
        return method14064(class1501, class762, i_3, i_4)
    }

    override fun createNativeTexture(class1501: GLFormat, i_2: Int, i_3: Int, floats_5: FloatArray?): NativeTextureSampler {
        if (!GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(i_2) || !OpenGlTexture.isPowerOfTwo(i_3))) {
            if (GLARBTextureRectangle) {
                return MipmapRectangleTextureSampler(this, class1501, i_2, i_3, floats_5!!)
            }
            val sampler2d = MipmapTextureSampler2D(this, class1501, GLActionFormat.FLOAT_32_ARB, MathUtils.nextPowerOfTwo(i_2), MathUtils.nextPowerOfTwo(i_3))
            sampler2d.method14494(i_2, i_3, floats_5!!, class1501)
            return sampler2d
        }
        return MipmapTextureSampler2D(this, class1501, i_2, i_3, floats_5!!)
    }

    override fun method14064(class1501: GLFormat, class762: GLActionFormat, i_3: Int, i_4: Int): RectangleTextureSampler {
        return if (!GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(i_3) || !OpenGlTexture.isPowerOfTwo(i_4))) {
            if (GLARBTextureRectangle) {
                MipmapRectangleTextureSampler(this, class1501, class762, i_3, i_4)
            } else {
                MipmapTextureSampler2D(this, class1501, class762, MathUtils.nextPowerOfTwo(i_3), MathUtils.nextPowerOfTwo(i_4))
            }
        } else {
            MipmapTextureSampler2D(this, class1501, class762, i_3, i_4)
        }
    }

    override fun generateNativeCubeTexture(sizeXy: Int, mipmapsGenerated: Boolean, ints_3: Array<IntArray>?): CubeTextureSampler {
        return OpenGLCubeTextureSampler(this, sizeXy, mipmapsGenerated, ints_3!!)
    }

    override fun method13962(class1501: GLFormat, bytes_6: ByteArray): Texture3DSampler {
        return MipmapTextureSampler3D(this, class1501, bytes_6)
    }

    override fun activateCurrentTexture() {
        OpenGL.glActiveTexture(currentTextureSlot + GL_TEXTURE0)
    }

    override fun setTextureColorStage() {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, getTextureCombineMode(textureRgbCombineModes[currentTextureSlot]))
    }

    override fun setTextureAlphaStage() {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, getTextureCombineMode(textureAlphaCombineModes[currentTextureSlot]))
    }

    override fun method13970(i_1: Int, class672: TextureStageState) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, i_1 + 34184, method15567(class672))
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, i_1 + 34200, 770)
    }

    override fun setTextureEnvironmentColor() {
        parameters[0] = (envColor and 0xff0000) / 1.671168E7f
        parameters[1] = (envColor and 0xff00) / 65280.0f
        parameters[2] = (envColor and 0xff) / 255.0f
        parameters[3] = (envColor ushr 24) / 255.0f
        OpenGL.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, parameters, 0)
    }

    override fun colorMask(flags: Int) {
        OpenGL.glColorMask((flags and 0x1) != 0, (flags and 0x2) != 0, (flags and 0x4) != 0, (flags and 0x8) != 0)
    }

    override fun blendDrawBuffer() {
        var alphaSrcFactor: Byte = 0
        var alphaDstFactor: Byte = 0

        when (anInt8844) {
            0 -> alphaDstFactor = 0
            1 -> {
                alphaSrcFactor = 1
                alphaDstFactor = 0
            }
            2 -> {
                alphaSrcFactor = 1
                alphaDstFactor = 1
            }
        }

        when (blendFuncMode) {
            BlendFuncMode.ALPHA_BLENDING -> {
                OpenGL.glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, alphaSrcFactor.toInt(), alphaDstFactor.toInt())
            }
            BlendFuncMode.OPAQUE_BLENDING -> {
                OpenGL.glBlendFuncSeparate(GL_ONE, GL_ONE, alphaSrcFactor.toInt(), alphaDstFactor.toInt())
            }
            BlendFuncMode.COLOR_BLENDING -> {
                OpenGL.glBlendFuncSeparate(GL_DST_COLOR, GL_ONE, alphaSrcFactor.toInt(), alphaDstFactor.toInt())
            }
            else -> {}
        }
    }

    override fun toggleBlending() {
        if (enableBlending) {
            OpenGL.glEnable(GL_BLEND)
        } else {
            OpenGL.glDisable(GL_BLEND)
        }
    }

    override fun toggleFog() {
        if (fogSupported && fogEnabled && fogDistance >= 0) {
            OpenGL.glEnable(GL_FOG)
        } else {
            OpenGL.glDisable(GL_FOG)
        }
    }

    override fun configureFog() {
        fogEnd = camFar - anInt8680
        fogStart = fogEnd - fogDistance
        if (fogStart < camNear) {
            fogStart = camNear
        }

        if (enableLights) {
            OpenGL.glFogf(GL_FOG_START, fogStart)
            OpenGL.glFogf(GL_FOG_END, fogEnd)
            parameters[0] = (fogColorRgb and 0xff0000) / 1.671168E7f
            parameters[1] = (fogColorRgb and 0xff00) / 65280.0f
            parameters[2] = (fogColorRgb and 0xff) / 255.0f
            OpenGL.glFogfv(GL_FOG_COLOR, parameters, 0)
        }
    }

    override fun toggleMultisampling(enabled: Boolean) {
        if (enabled) {
            OpenGL.glEnable(GL_MULTISAMPLE)
        } else {
            OpenGL.glDisable(GL_MULTISAMPLE)
        }
    }

    override fun createStream(bool1: Boolean): NativeStream {
        return OpenGlArbArrayBuffer(this, bool1)
    }

    override fun setStreamSource(i_1: Int, interface42: NativeStream) {
        aClass326Sub1Array10186[i_1] = interface42 as OpenGlArbArrayBuffer
    }

    override fun drawPrimitive(drawMode: PrimitiveDrawMode, first: Int, amt: Int) {
        val mode: Byte
        val count: Int

        when (drawMode) {
            PrimitiveDrawMode.LINES -> {
                mode = GL_LINES.toByte()
                count = amt * 2
            }
            PrimitiveDrawMode.LINE_STRIP -> {
                mode = GL_LINE_STRIP.toByte()
                count = amt + 1
            }
            PrimitiveDrawMode.TRIANGLES -> {
                mode = GL_TRIANGLES.toByte()
                count = amt * 3
            }
            PrimitiveDrawMode.TRIANGLE_FAN -> {
                mode = GL_TRIANGLE_STRIP.toByte()
                count = amt + 2
            }
            PrimitiveDrawMode.TRIANGLE_STRIP -> {
                mode = GL_TRIANGLE_STRIP.toByte()
                count = amt + 2
            }
            else -> {
                mode = GL_POINTS.toByte()
                count = amt
            }
        }
        OpenGL.glDrawArrays(mode.toInt(), first, count)
    }

    override fun drawIndexedPrimitive(drawMode: PrimitiveDrawMode, i_2: Int, i_3: Int, i_4: Int, amt: Int) {
        val mode: Byte
        val count: Int

        when (drawMode) {
            PrimitiveDrawMode.LINES -> {
                mode = GL_LINES.toByte()
                count = amt * 2
            }
            PrimitiveDrawMode.LINE_STRIP -> {
                mode = GL_LINE_STRIP.toByte()
                count = amt + 1
            }
            PrimitiveDrawMode.TRIANGLES -> {
                mode = GL_TRIANGLES.toByte()
                count = amt * 3
            }
            PrimitiveDrawMode.TRIANGLE_FAN -> {
                mode = GL_TRIANGLE_FAN.toByte()
                count = amt + 2
            }
            PrimitiveDrawMode.TRIANGLE_STRIP -> {
                mode = GL_TRIANGLE_STRIP.toByte()
                count = amt + 2
            }
            else -> {
                mode = GL_POINTS.toByte()
                count = amt
            }
        }
        val actionFormat = aClass326Sub210187!!.actionFormat
        OpenGL.glDrawElements(mode.toInt(), count, actionFormatDataType(actionFormat), aClass326Sub210187!!.getBufferAddress() + (i_4.toLong() * actionFormat.bytesPerEntry))
    }

    @Synchronized
    override fun animateTextures(textureId: Int) {
        var index = 0
        var adjustedTextureId = textureId and 0x7fffffff

        while (!textureBuffersToDelete.isEmpty()) {
            val node = textureBuffersToDelete.popTail() as IntNode
            textureIdsToDelete[index++] = node.pointer.toInt()
            textureBuffersSizeOccupied -= node.value
            if (index == 1000) {
                OpenGL.glDeleteBuffersARB(1000, textureIdsToDelete, 0)
                index = 0
            }
        }

        if (index > 0) {
            OpenGL.glDeleteBuffersARB(index, textureIdsToDelete, 0)
            index = 0
        }

        while (!texturesToDelete.isEmpty()) {
            val node = texturesToDelete.popTail() as IntNode
            textureIdsToDelete[index++] = node.pointer.toInt()
            texturesSizeOccupied -= node.value
            if (index == 1000) {
                OpenGL.glDeleteTextures(1000, textureIdsToDelete, 0)
                index = 0
            }
        }

        if (index > 0) {
            OpenGL.glDeleteTextures(index, textureIdsToDelete, 0)
            index = 0
        }

        while (!frameBuffersToDelete.isEmpty()) {
            val node = frameBuffersToDelete.popTail() as IntNode
            textureIdsToDelete[index++] = node.value
            if (index == 1000) {
                OpenGL.glDeleteFramebuffersEXT(1000, textureIdsToDelete, 0)
                index = 0
            }
        }

        if (index > 0) {
            OpenGL.glDeleteFramebuffersEXT(index, textureIdsToDelete, 0)
            index = 0
        }

        while (!renderBuffersToDelete.isEmpty()) {
            val node = renderBuffersToDelete.popTail() as IntNode
            textureIdsToDelete[index++] = node.pointer.toInt()
            renderBufferSizeOccupied -= node.value
            if (index == 1000) {
                OpenGL.glDeleteRenderbuffersEXT(1000, textureIdsToDelete, 0)
                index = 0
            }
        }

        if (index > 0) {
            OpenGL.glDeleteRenderbuffersEXT(index, textureIdsToDelete, 0)
        }

        while (!displayListsToDelete.isEmpty()) {
            val node = displayListsToDelete.popTail() as IntNode
            OpenGL.glDeleteLists(node.pointer.toInt(), node.value)
        }

        while (!programsToDelete.isEmpty()) {
            val node = programsToDelete.popTail()!!
            OpenGL.glDeleteProgram(node.pointer.toInt())
        }

        while (!shadersToDelete.isEmpty()) {
            val node = shadersToDelete.popTail()!!
            OpenGL.glDeleteShader(node.pointer.toInt())
        }

        while (!displayListsToDelete.isEmpty()) {
            val node = displayListsToDelete.popTail() as IntNode
            OpenGL.glDeleteLists(node.pointer.toInt(), node.value)
        }

        if (getOffheap() > 100663296 && Utils.time() > lastGcTime + 60000L) {
            System.gc()
            lastGcTime = Utils.time()
        }

        super.animateTextures(adjustedTextureId)
    }

    @Synchronized
    fun requestTextureBufferDeletion(handle: Int, size: Int) {
        val node = IntNode(size)
        node.pointer = handle.toLong()
        textureBuffersToDelete.insertTail(node)
    }

    @Synchronized
    fun requestTextureDeletion(textureId: Int, size: Int) {
        val node = IntNode(size)
        node.pointer = textureId.toLong()
        texturesToDelete.insertTail(node)
    }

    @Synchronized
    fun requestFrameBufferDeletion(size: Int) {
        val node = IntNode(size)
        frameBuffersToDelete.insertTail(node)
    }

    @Synchronized
    fun requestRenderBufferDeletion(textureId: Int, size: Int) {
        val node = IntNode(size)
        node.pointer = textureId.toLong()
        renderBuffersToDelete.insertTail(node)
    }

    @Synchronized
    fun destroyProgram(programHandle: Int) {
        val node = Node<Any>()
        node.pointer = programHandle.toLong()
        programsToDelete.insertTail(node)
    }

    override fun setTextureMatrix() {
        OpenGL.glMatrixMode(GL_TEXTURE)
        if (textureStates[currentTextureSlot] != TextureState.INITIALIZE) {
            OpenGL.glLoadMatrixf(textureMatrices[currentTextureSlot].copyInto(currentTextureMatrix), 0)
        } else {
            OpenGL.glLoadIdentity()
        }

        OpenGL.glMatrixMode(GL_MODELVIEW)
    }

    override fun debugModeChanged() {
    }

    override fun setPolygonMode(mode: Int) {
        OpenGL.glPolygonMode(GL_FRONT, if ((mode and 0x2) != 0) GL_LINE else GL_FILL)
    }

    override fun method13997(interface321: NativeToolkitBuffer) {
        aClass326Sub210187 = interface321 as OpenGlArbElementArrayBuffer
        aClass326Sub210187!!.bind()
    }

    override fun method13995(arr_1: Array<VertexDeclarationComponent>): VertexDeclaration {
        return VertexDeclarationGL(arr_1)
    }

    fun getGLShaderData(string_1: String): ByteArray? {
        return fetchShader("gl", string_1)
    }

    override fun toggleScissorTest() {
        if (boundsCalculated) {
            OpenGL.glEnable(GL_SCISSOR_TEST)
        } else {
            OpenGL.glDisable(GL_SCISSOR_TEST)
        }
    }

    override fun setupGraphics() {
        // Set depth function
        OpenGL.glDepthFunc(GL_LEQUAL)

        // Enable backface culling
        OpenGL.glEnable(GL_CULL_FACE)
        OpenGL.glCullFace(GL_BACK)

        // Set clear depth and alpha function
        OpenGL.glClearDepth(1.0f)
        OpenGL.glAlphaFunc(GL_GREATER, 0.0f)

        // Set polygon mode
        OpenGL.glPolygonMode(GL_FRONT, GL_FILL)

        // Set texture environment
        for (textureUnit in maxConcurrentTextures - 1 downTo 0) {
            OpenGL.glActiveTexture(GL_TEXTURE0 + textureUnit)
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE)
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_MODULATE)
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_RGB, GL_CONSTANT)
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, GL_MODULATE)
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_ALPHA, GL_CONSTANT)
        }
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_SRC2_ALPHA, GL_PREVIOUS)

        // Set shade model and matrix mode
        OpenGL.glShadeModel(GL_SMOOTH)
        OpenGL.glMatrixMode(GL_MODELVIEW)
        OpenGL.glLoadIdentity()

        // Enable color material
        OpenGL.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE)
        OpenGL.glEnable(GL_COLOR_MATERIAL)

        // Set default lighting
        val ambient = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
        for (lightUnit in 0 until 8) {
            val light = GL_LIGHT0 + lightUnit
            OpenGL.glLightfv(light, GL_AMBIENT, ambient, 0)
            OpenGL.glLightf(light, GL_CONSTANT_ATTENUATION, 0.0f)
            OpenGL.glLightf(light, GL_LINEAR_ATTENUATION, 0.0f)
        }

        // Set default fog
        OpenGL.glFogf(GL_FOG_DENSITY, 0.95f)
        OpenGL.glFogi(GL_FOG_MODE, GL_LINEAR)

        // Set default hints and swap interval
        OpenGL.glHint(GL_FOG_HINT, GL_FASTEST)
        opengl!!.setSwapInterval(0)

        super.setupGraphics()
    }

    @Synchronized
    fun requestShaderDeletion(pointer: Long) {
        val node3 = Node<Any>()
        node3.pointer = pointer
        shadersToDelete.insertTail(node3)
    }

    override fun render() {
        delegatingDisplay?.draw()
        if (materialProvider != null) {
            materialProvider.cycle()
        }
    }

    override fun setVertexDeclaration(vertexDeclaration: VertexDeclaration) {
        val components = vertexDeclaration.vertexDeclarationComponents
        var numTextures = 0
        var enableVertexColor = false
        var enableVertexNormal = false
        var enableVertexArray = false

        for (i in components.indices) {
            val component = components[i]
            val arrayBuffer = aClass326Sub1Array10186[i]
            var pointerOffset = 0
            val stride = arrayBuffer!!.stride
            val pointer = arrayBuffer.getBufferAddress()
            arrayBuffer.bind()

            for (j in 0 until component.size) {
                val action = component.getAction(j)
                when (action.openglAction) {
                    0 -> {
                        OpenGL.glNormalPointer(GL_FLOAT.toInt(), stride.toInt(), pointer + pointerOffset)
                        enableVertexNormal = true
                    }
                    1, 3, 5, 6 -> {}
                    2 -> {
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++)
                        OpenGL.glTexCoordPointer(4, GL_FLOAT.toInt(), stride.toInt(), pointer + pointerOffset)
                    }
                    4 -> {
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++)
                        OpenGL.glTexCoordPointer(2, GL_FLOAT.toInt(), stride.toInt(), pointer + pointerOffset)
                    }
                    7 -> {
                        OpenGL.glVertexPointer(3, GL_FLOAT.toInt(), stride.toInt(), pointer + pointerOffset)
                        enableVertexArray = true
                    }
                    8 -> {
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++)
                        OpenGL.glTexCoordPointer(1, GL_FLOAT.toInt(), stride.toInt(), pointer + pointerOffset)
                    }
                    9 -> {
                        OpenGL.glClientActiveTexture(GL_TEXTURE0 + numTextures++)
                        OpenGL.glTexCoordPointer(3, GL_FLOAT.toInt(), stride.toInt(), pointer + pointerOffset)
                    }
                    10 -> {
                        OpenGL.glColorPointer(4, GL_UNSIGNED_BYTE.toInt(), stride.toInt(), pointer + pointerOffset)
                        enableVertexColor = true
                    }
                }

                pointerOffset += action.pointerOffsetStep
            }
        }

        if (enableVertexArray != this.enableVertexArray) {
            if (enableVertexArray) {
                OpenGL.glEnableClientState(GL_VERTEX_ARRAY)
            } else {
                OpenGL.glDisableClientState(GL_VERTEX_ARRAY)
            }

            this.enableVertexArray = enableVertexArray
        }

        if (enableVertexNormal != this.enableVertexNormal) {
            if (enableVertexNormal) {
                OpenGL.glEnableClientState(GL_NORMAL_ARRAY)
            } else {
                OpenGL.glDisableClientState(GL_NORMAL_ARRAY)
            }

            this.enableVertexNormal = enableVertexNormal
        }

        if (enableVertexColor != this.enableVertexColor) {
            if (enableVertexColor) {
                OpenGL.glEnableClientState(GL_COLOR_ARRAY)
            } else {
                OpenGL.glDisableClientState(GL_COLOR_ARRAY)
            }

            this.enableVertexColor = enableVertexColor
        }

        if (textureAllocationPosition < numTextures) {
            for (textureUnit in textureAllocationPosition until numTextures) {
                OpenGL.glClientActiveTexture(textureUnit + GL_TEXTURE0)
                OpenGL.glEnableClientState(GL_TEXTURE_COORD_ARRAY)
            }

            textureAllocationPosition = numTextures
        } else if (textureAllocationPosition > numTextures) {
            for (textureUnit in numTextures until textureAllocationPosition) {
                OpenGL.glClientActiveTexture(textureUnit + GL_TEXTURE0)
                OpenGL.glDisableClientState(GL_TEXTURE_COORD_ARRAY)
            }

            textureAllocationPosition = numTextures
        }
    }

    override fun method13993(bool1: Boolean): NativeToolkitBuffer {
        return OpenGlArbElementArrayBuffer(this, GLActionFormat.TEXTURE_FORMAT, bool1)
    }

    override fun createNativeMemory(capacity: Int): NativeMemoryNode {
        return com.jagex.game.runetek5.buffer.NativeMemoryGL(capacity)
    }

    override fun extractNativeHeap(memoryNode: NativeMemoryNode): NativeHeap {
        if (memoryNode is com.jagex.game.runetek5.buffer.NativeMemoryGL) {
            return memoryNode.nativeHeap
        }
        throw IllegalArgumentException("Unexpected memory node type")
    }

    override fun createModel(i_1: Int, i_2: Int, bool3: Boolean, bool4: Boolean): Model? {
        // ModelGL constructor requires specific parameters - returning null for now
        // This may need proper implementation based on usage
        return null
    }

    override fun setTextureOnCurrentSlot() {
        val target = textureSlots!![currentTextureSlot]
        if (target != 0) {
            textureSlots!![currentTextureSlot] = 0
            OpenGL.glBindTexture(target, 0)
            OpenGL.glDisable(target)
        }
    }

    override fun getRenderInfo(): RenderInfo {
        var vendor = -1
        if (gpuVendor!!.indexOf("nvidia") != -1) {
            vendor = 4318
        } else if (gpuVendor!!.indexOf("intel") != -1) {
            vendor = 32902
        } else if (gpuVendor!!.indexOf("ati") != -1) {
            vendor = 4098
        }
        return RenderInfo(vendor, "OpenGL", combinedOpenglVersion, gpuManufactorer!!, 0L)
    }

    override fun createNativeTextureSampler(class1501: GLFormat, width: Int, height: Int, bool4: Boolean, pixels: ByteArray): NativeTextureSampler {
        if (!GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(width) || !OpenGlTexture.isPowerOfTwo(height))) {
            if (GLARBTextureRectangle) {
                return MipmapRectangleTextureSampler(this, class1501, width, height, pixels)
            }
            val sampler2d = MipmapTextureSampler2D(this, class1501, GLActionFormat.PIXEL_FORMAT, MathUtils.nextPowerOfTwo(width), MathUtils.nextPowerOfTwo(height))
            sampler2d.method52(0, 0, width, height, pixels, class1501, 0, 0)
            return sampler2d
        }
        return MipmapTextureSampler2D(this, class1501, width, height, bool4, pixels)
    }

    override fun method13954(class1501: GLFormat, class762: GLActionFormat): Boolean {
        return true
    }

    override fun setMatrixTransform(matrix441: Matrix44, matrix442: Matrix44, matrix443: Matrix44) {
        OpenGL.glMatrixMode(GL_MODELVIEW)
        aClass3848683.calculatePerspective(matrix441, matrix442)
        OpenGL.glLoadMatrixf(aClass3848683.faceList, 0)
        OpenGL.glMatrixMode(5889)
        OpenGL.glLoadMatrixf(matrix443.faceList, 0)
    }

    override fun setLightPosition() {
        OpenGL.glMatrixMode(GL_MODELVIEW)
        OpenGL.glLoadMatrixf(particleMatrix.faceList, 0)
        OpenGL.glLightfv(GL_LIGHT0, GL_POSITION, sunPosition, 0)
        OpenGL.glLightfv(GL_LIGHT1, GL_POSITION, lightPosition, 0)
    }

    override fun setScissorRect() {
        if (display != null) {
            var x = anInt8823 + cLeft
            var y = anInt8754 + display!!.getHeight() - cBottom
            var width = cRight - cLeft
            var height = cBottom - cTop
            if (width < 0) {
                width = 0
            }

            if (height < 0) {
                height = 0
            }

            OpenGL.glScissor(x, y, width, height)
        }
    }

    override fun toggleAlpha() {
        if (enableAlphaTest) {
            OpenGL.glEnable(GL_ALPHA_TEST)
        } else {
            OpenGL.glDisable(GL_ALPHA_TEST)
        }
    }

    override fun getRenderBuffer(width: Int, height: Int): RenderBuffer {
        return OpenGlNativeRenderBuffer(this, GLFormat.aClass1501953, GLActionFormat.DEPTH_COMPONENT_24, width, height)
    }

    override fun setTextureStageState(src: Int, state: TextureStageState) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, src + GL_SOURCE0_RGB, method15567(state))
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, src + GL_OPERAND0_RGB, 768)
    }

    override fun createTextureSampler(i_1: Int, i_2: Int, bool3: Boolean, ints_4: IntArray, i_5: Int, i_6: Int): NativeTextureSampler {
        if (!GLARBTextureNonPowerOfTwo && (!OpenGlTexture.isPowerOfTwo(i_1) || !OpenGlTexture.isPowerOfTwo(i_2))) {
            return if (GLARBTextureRectangle) {
                MipmapRectangleTextureSampler(this, i_1, i_2, ints_4, i_5, i_6)
            } else {
                val class319Sub27 = MipmapTextureSampler2D(this, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, MathUtils.nextPowerOfTwo(i_1), MathUtils.nextPowerOfTwo(i_2))
                class319Sub27.method48(0, 0, i_1, i_2, ints_4, i_5, i_6)
                class319Sub27
            }
        } else {
            return MipmapTextureSampler2D(this, i_1, i_2, bool3, ints_4, i_5, i_6)
        }
    }

    override fun renderLastFrame() {
        if (display != null) {
            val displayWidth = display!!.getWidth()
            val displayHeight = display!!.getHeight()
            if (displayWidth > 0 || displayHeight > 0) {
                val i_3 = anInt8749
                val i_4 = anInt8776
                val i_5 = anInt8751
                val i_6 = anInt8752
                calculateViewportCenter()
                val i_7 = cLeft
                val i_8 = cRight
                val i_9 = cTop
                val i_10 = cBottom
                resetClipping()
                OpenGL.glReadBuffer(GL_FRONT)
                OpenGL.glDrawBuffer(GL_BACK)
                method13934()
                toggleFog(false)
                toggleLights(false)
                toggleDepthTest(false)
                method13942(false)
                setTextureSampler(null)
                setColourType(1)
                toggleBlending(0)
                OpenGL.glMatrixMode(GL_PROJECTION)
                OpenGL.glLoadIdentity()
                OpenGL.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0)
                OpenGL.glMatrixMode(GL_MODELVIEW)
                OpenGL.glLoadIdentity()
                OpenGL.glRasterPos2i(0, 0)
                OpenGL.glCopyPixels(0, 0, displayWidth, displayHeight, GL_COLOR)
                OpenGL.glFlush()
                OpenGL.glReadBuffer(GL_BACK)
                OpenGL.glDrawBuffer(GL_BACK)
                setClipping(i_7, i_9, i_8, i_10)
                setDrawableArea(i_3, i_4, i_5, i_6)
            }
        }
    }

    override fun setFogColorAndIntensity() {
        parameters[0] = lightIntensity * baseLightColorRed
        parameters[1] = lightIntensity * baseLightColorGreen
        parameters[2] = lightIntensity * baseLightColorBlue
        parameters[3] = 1.0f
        OpenGL.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, parameters, 0)
    }

    companion object {
        @JvmStatic
        fun actionFormatDataType(actionFormat: GLActionFormat): Int {
            return when (actionFormat.anInt757) {
                0 -> GL_SHORT
                1, 4 -> GL_UNSIGNED_BYTE
                2 -> GL_UNSIGNED_INT
                3 -> GL_FLOAT
                5 -> GL_UNSIGNED_SHORT
                6 -> GL_BYTE
                7 -> GL_INT
                8 -> GL_HALF_FLOAT
                else -> GL_UNSIGNED_BYTE
            }
        }

        @JvmStatic
        fun method15567(class670: TextureStageState): Int {
            return when {
                TextureStageState.aClass67680 == class670 -> GL_TEXTURE
                TextureStageState.aClass67678 == class670 -> GL_PRIMARY_COLOR
                TextureStageState.aClass67679 == class670 -> GL_PREVIOUS
                TextureStageState.aClass67677 == class670 -> GL_CONSTANT
                else -> throw IllegalArgumentException()
            }
        }

        @JvmStatic
        fun texture2dInternalFormat(actionFormat: GLFormat, format: GLActionFormat): Int {
            if (format == GLActionFormat.PIXEL_FORMAT) {
                return when (actionFormat.formatType) {
                    0 -> GL_LUMINANCE
                    1 -> GL_ALPHA
                    4 -> GL_LUMINANCE_ALPHA
                    5 -> GL_RGB
                    6 -> GL_RGBA
                    7 -> GL_COMPRESSED_RGBA_S3TC_DXT1_EXT
                    9 -> GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
                    else -> throw IllegalArgumentException()
                }
            } else if (format == GLActionFormat.TEXTURE_FORMAT) {
                return when (actionFormat.formatType) {
                    0 -> GL_LUMINANCE16
                    1 -> GL_ALPHA16
                    4 -> GL_LUMINANCE_ALPHA16UI_EXT
                    5 -> GL_RGB16
                    6 -> GL_RGBA16
                    8 -> GL_DEPTH_COMPONENT16
                    else -> throw IllegalArgumentException()
                }
            } else if (format == GLActionFormat.DEPTH_COMPONENT_24) {
                return when (actionFormat.formatType) {
                    8 -> GL_DEPTH_COMPONENT24
                    else -> throw IllegalArgumentException()
                }
            } else if (format == GLActionFormat.FLOAT_16_ARB) {
                return when (actionFormat.formatType) {
                    0 -> GL_LUMINANCE16F_ARB
                    1 -> GL_ALPHA16F_ARB
                    4 -> GL_LUMINANCE_ALPHA16F_ARB
                    5 -> GL_RGB16F_ARB
                    6 -> GL_RGBA16F_ARB
                    else -> throw IllegalArgumentException()
                }
            } else if (format == GLActionFormat.FLOAT_32_ARB) {
                return when (actionFormat.formatType) {
                    0 -> GL_LUMINANCE32F_ARB
                    1 -> GL_ALPHA32F_ARB
                    4 -> GL_LUMINANCE_ALPHA32F_ARB
                    5 -> GL_RGB32F_ARB
                    6 -> GL_RGBA32F_ARB
                    else -> throw IllegalArgumentException()
                }
            } else {
                throw IllegalArgumentException()
            }
        }

        @JvmStatic
        fun texture2dFormat(class1500: GLFormat): Int {
            return when (class1500.formatType) {
                0 -> GL_LUMINANCE
                1 -> GL_ALPHA
                4 -> GL_LUMINANCE_ALPHA
                5 -> GL_RGB
                6 -> GL_RGBA
                8 -> GL_DEPTH_COMPONENT
                else -> throw IllegalStateException()
            }
        }

        @JvmStatic
        fun getTextureCombineMode(textureCombineMode: TextureCombineMode): Int {
            return when {
                TextureCombineMode.REPLACE == textureCombineMode -> GL_REPLACE
                TextureCombineMode.MODULATE == textureCombineMode -> GL_MODULATE
                TextureCombineMode.INTERPOLATE == textureCombineMode -> GL_INTERPOLATE
                TextureCombineMode.ADD == textureCombineMode -> GL_ADD
                TextureCombineMode.SUBTRACT == textureCombineMode -> GL_SUBTRACT
                else -> throw IllegalArgumentException()
            }
        }
    }
}
