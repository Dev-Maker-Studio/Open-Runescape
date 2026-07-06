package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.graphics.FramebufferCompositor
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.utils.MathUtils

import jaclib.memory.Buffer
import jaclib.memory.Stream
import jaclib.memory.heap.NativeHeap
import jaggl.OpenGL

import com.jagex.game.runetek5.toolkit.opengl.*

import java.awt.*

import com.jagex.game.runetek5.buffer.NativeMemoryNode
import com.jagex.game.runetek5.buffer.OpenGlRenderBuffer
import com.jagex.game.runetek5.buffer.ArrayBufferGL
import com.jagex.game.runetek5.buffer.ElementsBufferGL
import com.jagex.game.runetek5.buffer.NativeMemoryGL
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.LinkedList
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialType
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.font.FontRendererGL
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.shader.Texture3DShaderGL
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteGL
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.texture.OpenGLArbTextureCubeMapGenerator
import com.jagex.game.runetek5.graphics.texture.AbstractCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.AbstractOpenGlCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.GlElementsArrayBuffer
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture
import com.jagex.game.runetek5.graphics.texture.OpenGl2DTexture_Sub1
import com.jagex.game.runetek5.graphics.texture.OpenGlArbCubeMapTexture
import com.jagex.game.runetek5.graphics.texture.OpenGlCubeTextureGenerator
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.graphics.texture.RectangularTextureGL
import com.jagex.game.runetek5.imageproducer.DelegatingDisplay
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.imageproducer.ImageProducerGL2
import com.jagex.game.runetek5.imageproducer.IntegratedGraphicsDisplayGL
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.rendering.RenderInfo
import com.jagex.game.runetek5.scene.ground.Ground
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.scene.tile.WaterTile
import com.jagex.game.runetek5.systeminfo.OperatingSystemInfo
import com.jagex.game.runetek5.toolkit.opengl.BlendType
import com.jagex.game.runetek5.toolkit.opengl.EffectManagerGL
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat
import com.jagex.game.runetek5.toolkit.opengl.OpenGLTextureCache
import com.jagex.game.runetek5.toolkit.opengl.GLFormat
import com.jagex.game.runetek5.toolkit.opengl.GlParticleRenderer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlVboElementsArrayBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.OpenGlArrayDrawBuffer
import com.jagex.game.runetek5.toolkit.opengl.buffer.GlBitBuffer
import com.jagex.game.runetek5.tools.TimeTools
import com.jagex.game.runetek5.utils.Utils
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

open class OpenGlToolkit(canvas: Canvas, materials: MaterialProvider, multiSamples: Int) : AbstractToolkit(materials) {

    @JvmField var openglTextures: Array<OpenGlTexture?>? = null
    @JvmField var texture2d: OpenGl2DTexture? = null
    @JvmField var integratedGraphicsImageProducer: IntegratedGraphicsDisplayGL? = null
    @JvmField var activeBlendAlpha: Int = 0
    @JvmField var activeBlendType: Int = 0

    /**
     * Allow selective rendering based on fragment alpha values,
     * determining whether to display or discard pixels depending on the reference alpha value set
     */
    @JvmField var alphaTestEnabled: Boolean = false

    /** Controls whether textures are applied during rendering. When false, models render untextured. */
    @JvmField var texturesEnabled: Boolean = true

    @JvmField var colormaskActive: Boolean = false
    @JvmField var fogColor: Int = -1
    @JvmField var sunColorRgb: Int = -1
    @JvmField var colortableReset: Boolean = false
    @JvmField val currentTextureSize: Int = 128
    @JvmField val particleRenderer: GlParticleRenderer = GlParticleRenderer()
    @JvmField val scratchMatrix: Matrix44 = Matrix44()
    @JvmField val billboardMatrix: Matrix44 = Matrix44()
    @JvmField val renderingMatrix: Matrix43 = Matrix43()
    @JvmField var openglFlags: Int = 8
    @JvmField var shadowScale: Int = 3
    @JvmField val heapManager: LinkedList = LinkedList()
    @JvmField val listsAwaitingDeallocation: LinkedList = LinkedList()
    @JvmField val vboBuffersAwaitingDeallocation: LinkedList = LinkedList()
    @JvmField val texturesAwaitingDeallocation: LinkedList = LinkedList()
    @JvmField val frameBuffersAwaitingDeallocation: LinkedList = LinkedList()
    @JvmField val renderBuffersAwaitingRemoval: LinkedList = LinkedList()
    @JvmField val arbProgramsAwaitingDeallocation: LinkedList = LinkedList()
    @JvmField val shaderProgramsAwaitingDeallocation: LinkedList = LinkedList()
    @JvmField val viewMatrix43: Matrix43 = Matrix43()
    @JvmField val viewMatrix44: Matrix44 = Matrix44()
    @JvmField val inverseSunMatrix44: Matrix44 = Matrix44()
    @JvmField val currentModelMatrix: Matrix44 = Matrix44()
    @JvmField val orthoProjectionMatrix: Matrix44 = Matrix44()
    @JvmField val viewProjectionMatrix: Matrix44 = Matrix44()
    @JvmField val frustumPlanes: Array<FloatArray> = Array(6) { FloatArray(4) }
    @JvmField val lightParameterBuffer: FloatArray = FloatArray(4)
    @JvmField var depthClippingNear: Float = 0f
    @JvmField var depthClippingFar: Float = 1.0f
    @JvmField var fogMinimumStartPosition: Float = 0f
    @JvmField var fogFarDistance: Float = -1.0f
    @JvmField val inverseSunMatrix43: Matrix43 = Matrix43()
    @JvmField val tempModelMatrix: Matrix44 = Matrix44()
    @JvmField val tempProjectionMatrix: Matrix44 = Matrix44()
    @JvmField val textureMatrix: FloatArray = FloatArray(16)
    @JvmField var depthBufferingEnabled: Boolean = true
    @JvmField var clipTop: Int = 0
    @JvmField var clipBottom: Int = 0
    @JvmField var clipLeft: Int = 0
    @JvmField var clipRight: Int = 0
    @JvmField var viewportOffsetX: Int = 0
    @JvmField var viewportOffsetY: Int = 0
    @JvmField val sunPreviousPosition: FloatArray = FloatArray(4)
    @JvmField val antiSunPreviousPosition: FloatArray = FloatArray(4)

    /**
     * For naming see: [com.jagex.game.runetek5.toolkit.opengl.effect.MaterialEffect9GL.toggle]
     */
    @JvmField val sunDirection: FloatArray = FloatArray(4)

    @JvmField val lightingDirection: FloatArray = FloatArray(4)
    @JvmField var sunColorRed: Float = 1.0f
    @JvmField var sunColorGreen: Float = 1.0f
    @JvmField var sunColorBlue: Float = 1.0f
    @JvmField var sunColorIntensity: Float = -1.0f
    @JvmField var antiSunColorIntensity: Float = -1.0f
    @JvmField val lightNodes: Array<PointLight?> = arrayOfNulls(INITIAL_LIGHT_COUNT)
    @JvmField var fogDensity: Int = -1
    @JvmField var fogOffset: Int = 0
    @JvmField var densityFactor: Float = 1.0f
    @JvmField var fogMin: Float = 0.0f
    @JvmField var renderStillWater: Boolean = false
    @JvmField var textureCombineRgb: Int = 8448
    @JvmField var textureCombineAlpha: Int = 8448
    @JvmField var outlineCirclePointSize: Float = -1.0f
    @JvmField var fillCirclePointSize: Float = -1.0f
    @JvmField val copySrc: Array<ModelGL?> = arrayOfNulls(7)
    @JvmField val copyDst: Array<ModelGL?> = arrayOfNulls(7)
    @JvmField var textureBuffer: GlBitBuffer = GlBitBuffer(8192)
    /**
     * Per-thread vertex projection scratch: the game thread uses it for model picking
     * and loc updates while the async map loader bakes loc/ground shadows during a
     * background build, so it cannot be a single shared pair of arrays.
     */
    private val projectionScratch: ThreadLocal<ProjectionScratch> = ThreadLocal.withInitial { ProjectionScratch() }
    @JvmField var projectedVerticesZ: IntArray = IntArray(1)

    class ProjectionScratch {
        @JvmField var verticesX: IntArray = IntArray(1)
        @JvmField var verticesY: IntArray = IntArray(1)
    }

    fun projectionScratch(capacity: Int): ProjectionScratch {
        val scratch = projectionScratch.get()
        if (scratch.verticesX.size < capacity) {
            scratch.verticesX = IntArray(capacity)
            scratch.verticesY = IntArray(capacity)
        }
        return scratch
    }
    @JvmField val shadowTextureData: ByteArray = ByteArray(16384)
    @JvmField var multiSamplesPreference: Int = 0
    @JvmField var activeOpenglInstance: OpenGL? = null
    @JvmField var pixelType: Int = 0
    @JvmField var intelGpu: Boolean = false
    @JvmField var s3Graphics: Boolean = false
    @JvmField var arbTextureFloat: Boolean = false
    @JvmField var textureCache: OpenGLTextureCache? = null
    @JvmField var texture3dShader: Texture3DShaderGL? = null
    @JvmField var framebufferCompositor: FramebufferCompositor? = null
    @JvmField var arbRectangleTexturesSupported: Boolean = false
    @JvmField var shouldUseArbVbo: Boolean = false
    @JvmField var radeonGpu: Boolean = false
    @JvmField var rectangularTexture: RectangularTextureGL? = null
    @JvmField var effectManager: EffectManagerGL? = null
    @JvmField var vertexbufferSupported: Boolean = false
    @JvmField var texture3dSupported: Boolean = false
    @JvmField var nativeHeap: NativeHeap? = null
    @JvmField var screenQuadArrayBuffer: ArrayBufferGL? = null
    @JvmField var quadVertexBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var quadTexCoordBuffer: OpenGlArrayDrawBuffer? = null
    @JvmField var waterDepth: Int = 0
    @JvmField var waterTile: WaterTile? = null
    @JvmField var stillWaterToggled: Boolean = false
    @JvmField var drawableX: Int = 0
    @JvmField var drawableY: Int = 0
    @JvmField var imageProducerWidth: Int = 0
    @JvmField var imageProducerHeight: Int = 0
    @JvmField var imageProducerCenterX: Float = 0f
    @JvmField var imageProducerCenterY: Float = 0f
    @JvmField var viewportOriginX: Float = 0f
    @JvmField var viewportOriginY: Float = 0f
    @JvmField var sunBrightness: Float = 0f
    @JvmField var vboBuffersSize: Int = 0
    @JvmField var textureBuffersSize: Int = 0
    @JvmField var renderBuffersSize: Int = 0
    @JvmField var textureAnimatingProgression: Int = 0
    @JvmField var lastGcCycle: Long = 0L
    @JvmField var depthClippingMode: Int = 0
    @JvmField var functionMask: Int = 0
    @JvmField var shadowOffsetX: Int = 0
    @JvmField var shadowOffsetZ: Int = 0
    @JvmField var fogEndPosition: Float = 0f
    @JvmField var fogStartPosition: Float = 0f
    @JvmField var currentArrayBuffer: ArrayBufferGL? = null
    @JvmField var textureUpdate: Boolean = false
    @JvmField var lastMaterialId: Int = 0
    @JvmField var glTexturesCursorPos: Int = 0
    @JvmField var lightingEnabled: Boolean = false
    @JvmField var hideLights: Boolean = false
    @JvmField var enableDepthMask: Boolean = false
    @JvmField var activeLightCount: Int = 0
    @JvmField var fogEnabled: Boolean = false
    @JvmField var worldMapTexture: OpenGl2DTexture_Sub1? = null
    @JvmField var gpuCompanyString: String? = null
    @JvmField var gpuNameString: String? = null
    @JvmField var openglVersion: Int = 0
    @JvmField var maxTextureUnits: Int = 0
    @JvmField var maxTextureCoordinates: Int = 0
    @JvmField var maxTextureImageUnits: Int = 0
    @JvmField var bigEndianess: Boolean = false
    @JvmField var arbMultisample: Boolean = false
    @JvmField var arbVertexProgram: Boolean = false
    @JvmField var arbVertexShader: Boolean = false
    @JvmField var arbFragmentShader: Boolean = false
    @JvmField var arbTextureCubeMap: Boolean = false
    @JvmField var framebufferObjectSupported: Boolean = false
    @JvmField var framebufferBlit: Boolean = false
    @JvmField var framebufferMultisample: Boolean = false
    @JvmField var advancedFramebuffers: Boolean = false
    @JvmField var appleOperatingSystem: Boolean = false
    @JvmField var lastSceneLightCount: Int = 0
    @JvmField var depthBuffering: Boolean = false
    @JvmField var sunCubeTextureGenerator: AbstractOpenGlCubeTextureGenerator? = null
    @JvmField var currentElementsBuffer: ElementsBufferGL? = null

    init {
        try {
            this.multiSamplesPreference = multiSamples
            if (!getNativeLibraryLoader().loadIntoMemory("jaclib")) {
                println("Missing jaclib")
                throw RuntimeException("")
            }
            if (!getNativeLibraryLoader().loadIntoMemory("jaggl")) {
                println("Missing jaggl")
                throw RuntimeException("")
            }
            this.activeOpenglInstance = OpenGL()
            val generatedUid = this.activeOpenglInstance!!.init(canvas, 8, 8, 8, 24, 0, this.multiSamplesPreference)
            if (generatedUid == 0L) {
                throw RuntimeException("")
            }
            attach()
            val capabilityFlags = accumulateCapabilities()
            if (capabilityFlags != 0) {
                throw RuntimeException("")
            }
            this.pixelType = if (this.bigEndianess) GL_UNSIGNED_INT_8_8_8_8_REV else GL_UNSIGNED_BYTE
            if (this.gpuNameString!!.indexOf("radeon") != -1) {
                var gpuVersion = 0
                var hasHdPrefix = false
                var hasCrossPrefix = false
                val namePieces = Utils.split(this.gpuNameString!!.replace('/', ' '), ' ')
                for (nameIndex in namePieces.indices) {
                    var name = namePieces[nameIndex]
                    try {
                        if (name.isNotEmpty()) {
                            if (name[0].code == 120 && name.length >= 3 && Utils.isWrongJava(name.substring(1, 3))) {
                                name = name.substring(1)
                                hasCrossPrefix = true
                            }
                            if ("hd" == name) {
                                hasHdPrefix = true
                            } else {
                                if (name.startsWith("hd")) {
                                    name = name.substring(2)
                                    hasHdPrefix = true
                                }
                                if (name.length >= 4 && Utils.isWrongJava(name.substring(0, 4))) {
                                    gpuVersion = Utils.parseInt(name.substring(0, 4))
                                    break
                                }
                            }
                        }
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
                if (!hasCrossPrefix && !hasHdPrefix) {
                    if (gpuVersion in 7000..7999) {
                        this.vertexbufferSupported = false
                    }
                    if (gpuVersion in 7000..9250) {
                        this.texture3dSupported = false
                    }
                }
                if (!hasHdPrefix || gpuVersion < 4000) {
                    this.arbTextureFloat = false
                }
                this.arbRectangleTexturesSupported = this.arbRectangleTexturesSupported and this.activeOpenglInstance!!.supportsExtension("GL_ARB_half_float_pixel")
                this.shouldUseArbVbo = this.vertexbufferSupported
                this.radeonGpu = true
            }
            if (this.gpuCompanyString!!.indexOf("intel") != -1) {
                this.intelGpu = false
            }
            this.s3Graphics = "s3 graphics" != this.gpuCompanyString
            if (this.vertexbufferSupported) {
                try {
                    val handle = IntArray(1)
                    OpenGL.glGenBuffersARB(1, handle, 0)
                } catch (throwable: Throwable) {
                    throwable.printStackTrace()
                    throw RuntimeException("")
                }
            }
            ColorUtil.initializeTables(false, true)
            this.colortableReset = true
            if (materialProvider == null) {
                throw IllegalStateException("OpenGlToolkit requires non-null materialProvider")
            }
            this.textureCache = OpenGLTextureCache(this, materialProvider)
            buildTables()
            this.texture3dShader = Texture3DShaderGL(this)
            this.framebufferCompositor = FramebufferCompositor(this)
            if (this.framebufferCompositor!!.isDisplayAvailable()) {
                this.rectangularTexture = RectangularTextureGL(this)
                if (!this.rectangularTexture!!.rectangularTextureSupport()) {
                    this.rectangularTexture!!.destroy()
                    this.rectangularTexture = null
                }
            }
            addImageProducer(canvas, ImageProducerGL2(this, canvas, generatedUid))
            setActiveCanvas(canvas)
            this.effectManager = EffectManagerGL(this)
            prepareForRendering()
            renderLastFrame()
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

    fun defineScissorBox() {
        if (display != null && this.clipLeft < this.clipRight && this.clipTop < this.clipBottom) {
            OpenGL.glScissor(this.viewportOffsetX + this.clipLeft, this.viewportOffsetY + display!!.getHeight() - this.clipBottom, this.clipRight - this.clipLeft, this.clipBottom - this.clipTop)
        } else {
            OpenGL.glScissor(0, 0, 0, 0)
        }
    }

    fun attach() {
        var attachAttempt = 0
        while (!this.activeOpenglInstance!!.attach()) {
            if (attachAttempt++ > 5) {
                throw RuntimeException("")
            }
            TimeTools.sleep(1000L)
        }
    }

    fun buildTables() {
        this.openglTextures = arrayOfNulls(this.maxTextureUnits)
        this.texture2d = OpenGl2DTexture(this, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, 1, 1)
        OpenGl2DTexture(this, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, 1, 1)
        OpenGl2DTexture(this, GL_TEXTURE_2D, GLFormat.HDR, GLActionFormat.PIXEL_FORMAT, 1, 1)
        for (index in 0 until 7) {
            this.copySrc[index] = ModelGL(this)
            this.copyDst[index] = ModelGL(this)
        }
        if (this.intelGpu) {
            this.integratedGraphicsImageProducer = IntegratedGraphicsDisplayGL(this)
            IntegratedGraphicsDisplayGL(this)
        }
    }

    fun prepareForRendering() {
        renderTexture(-2)
        for (textureUnit in this.maxTextureUnits - 1 downTo 0) {
            activateTexture(textureUnit)
            setActiveTexture(null)
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE)
        }
        loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        setTexture(2, GL_PREVIOUS, GL_SRC_ALPHA)
        renderStationaryTexture()
        this.activeBlendAlpha = 1
        OpenGL.glEnable(GL_BLEND)
        OpenGL.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        this.activeBlendType = 1
        OpenGL.glEnable(GL_ALPHA_TEST)
        OpenGL.glAlphaFunc(GL_GREATER, 0.0f)
        this.alphaTestEnabled = true
        OpenGL.glColorMask(true, true, true, true)
        this.colormaskActive = true
        toggleFog(true)
        setLightingEnabled(true)
        toggleZbuffer(true)
        setDepthMask(true)
        resetDepthClippingMask()
        setOrthoMatrixMode()
        this.activeOpenglInstance!!.setSwapInterval(0)
        OpenGL.glShadeModel(GL_SMOOTH)
        OpenGL.glClearDepth(1.0f)
        OpenGL.glDepthFunc(515)
        OpenGL.glPolygonMode(GL_FRONT, GL_FILL)
        OpenGL.glEnable(GL_CULL_FACE)
        OpenGL.glCullFace(GL_BACK)
        OpenGL.glMatrixMode(GL_MODELVIEW)
        OpenGL.glLoadIdentity()
        OpenGL.glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE)
        OpenGL.glEnable(GL_COLOR_MATERIAL)
        val defaultAmbient = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
        for (lightHandleOffset in 0 until 8) {
            val lightHandle = lightHandleOffset + GL_LIGHT0
            OpenGL.glLightfv(lightHandle, GL_AMBIENT, defaultAmbient, 0)
            OpenGL.glLightf(lightHandle, GL_CONSTANT_ATTENUATION, 0.0f)
            OpenGL.glLightf(lightHandle, GL_LINEAR_ATTENUATION, 0.0f)
        }
        OpenGL.glEnable(GL_LIGHT0)
        OpenGL.glEnable(GL_LIGHT1)
        OpenGL.glFogf(GL_FOG_DENSITY, 0.95f)
        OpenGL.glFogi(GL_FOG_MODE, GL_LINEAR)
        OpenGL.glHint(GL_FOG_HINT, GL_FASTEST)
        this.fogColor = -1
        this.sunColorRgb = -1
        calculateViewportCenter()
        resetClipping()
    }

    override fun render() {
        try {
            delegatingDisplay?.draw()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        materialProvider?.cycle()
    }

    override fun awaitRenderCompletion() {
        OpenGL.glFinish()
    }

    override fun shutdown() {
        var node1 = this.heapManager.getTail()
        while (node1 != null) {
            (node1 as NativeMemoryGL).deallocate()
            node1 = this.heapManager.getPrevious()
        }
        this.framebufferCompositor?.shutdown()
        if (this.colortableReset) {
            ColorUtil.resetTables(false, true)
            this.colortableReset = false
        }
    }

    override fun supportsFullscreen(): Boolean = true

    override fun forceFixedScreenmode(): Boolean = true

    override fun hasBloomSupport(): Boolean {
        return this.rectangularTexture != null && (this.multiSamplesPreference <= 1 || this.advancedFramebuffers)
    }

    override fun hasHardwareRendering(): Boolean = true

    fun pushMatrix() {
        OpenGL.glPushMatrix()
    }

    override fun extendBuildArea(): Boolean = true

    fun resetDisplay() {
        resetOrthoAndDisplay()
    }

    override fun copyPixelRectangle(x: Int, y: Int, width: Int, height: Int): IntArray {
        if (display == null) {
            return IntArray(0)
        }
        val pixels = IntArray(width * height)
        val producerHeight = display!!.getHeight()
        for (h in 0 until height) {
            OpenGL.glReadPixelsi(x, producerHeight - y - h - 1, width, 1, 32993, this.pixelType, pixels, width * h)
        }
        return pixels
    }

    override fun renderLastFrame() {
        if (this.s3Graphics && display != null) {
            val left = this.clipLeft
            val right = this.clipRight
            val top = this.clipTop
            val bottom = this.clipBottom
            resetClipping()
            val savedDrawableX = this.drawableX
            val savedDrawableY = this.drawableY
            val savedWidth = this.imageProducerWidth
            val savedHeight = this.imageProducerHeight
            calculateViewportCenter()
            OpenGL.glReadBuffer(GL_FRONT)
            OpenGL.glDrawBuffer(GL_BACK)
            setOrthoMatrixMode()
            toggleFog(false)
            setLightingEnabled(false)
            toggleZbuffer(false)
            setDepthMask(false)
            setActiveTexture(null)
            renderTexture(-2)
            loadTextureCombineMode(1)
            setBlendMode(0)
            OpenGL.glMatrixMode(GL_PROJECTION)
            OpenGL.glLoadIdentity()
            OpenGL.glOrtho(0.0, 1.0, 0.0, 1.0, -1.0, 1.0)
            OpenGL.glMatrixMode(GL_MODELVIEW)
            OpenGL.glLoadIdentity()
            OpenGL.glRasterPos2i(0, 0)
            OpenGL.glCopyPixels(0, 0, display!!.getWidth(), display!!.getHeight(), GL_COLOR)
            OpenGL.glFlush()
            OpenGL.glReadBuffer(GL_BACK)
            OpenGL.glDrawBuffer(GL_BACK)
            setClipping(left, top, right, bottom)
            setDrawableArea(savedDrawableX, savedDrawableY, savedWidth, savedHeight)
        }
    }

    override fun applyClearFlags(functionMask: Int, colorArgb: Int) {
        var bufferClearMask = 0
        if ((functionMask and 0x1) != 0) {
            OpenGL.glClearColor((colorArgb and 0xff0000) / 1.671168E7f, (colorArgb and 0xff00) / 65280.0f, (colorArgb and 0xff) / 255.0f, (colorArgb ushr 24) / 255.0f)
            bufferClearMask = 16384
        }

        if ((functionMask and 0x2) != 0) {
            setDepthMask(true)
            bufferClearMask = bufferClearMask or 0x100
        }

        if ((functionMask and 0x4) != 0) {
            bufferClearMask = bufferClearMask or 0x400
        }

        OpenGL.glClear(bufferClearMask)
    }

    /**
     * Example: https://youtu.be/E0hXye2B08s?t=216
     */
    override fun drawRectangleOutline(x: Int, y: Int, width: Int, height: Int, colorArgb: Int, blendMode: Int) {
        val leftX = x + 0.35f
        val topY = y + 0.35f
        val rightX = leftX + width - 1.0f
        val bottomY = topY + height - 1.0f
        setupShapeRendering()
        setBlendMode(blendMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        if (this.arbMultisample) {
            OpenGL.glDisable(GL_MULTISAMPLE)
        }
        OpenGL.glBegin(GL_LINE_LOOP)
        OpenGL.glVertex2f(leftX, topY)
        OpenGL.glVertex2f(leftX, bottomY)
        OpenGL.glVertex2f(rightX, bottomY)
        OpenGL.glVertex2f(rightX, topY)
        OpenGL.glEnd()
        if (this.arbMultisample) {
            OpenGL.glEnable(GL_MULTISAMPLE)
        }
    }

    override fun drawFilledRectangle(x: Int, y: Int, width: Int, height: Int, colorArgb: Int, blendMode: Int) {
        val leftX = x + 0.35f
        val topY = y + 0.35f
        val rightX = leftX + width
        val bottomY = topY + height
        setupShapeRendering()
        setBlendMode(blendMode)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        if (this.arbMultisample) {
            OpenGL.glDisable(GL_MULTISAMPLE)
        }

        OpenGL.glBegin(GL_QUADS)
        OpenGL.glVertex2f(leftX, topY)
        OpenGL.glVertex2f(leftX, bottomY)
        OpenGL.glVertex2f(rightX, bottomY)
        OpenGL.glVertex2f(rightX, topY)
        OpenGL.glEnd()
        if (this.arbMultisample) {
            OpenGL.glEnable(GL_MULTISAMPLE)
        }
    }

    fun setDepthMask() {
        OpenGL.glDepthMask(this.enableDepthMask && this.depthBufferingEnabled)
    }

    override fun benchmark() {
        setupShapeRendering()
        setBlendMode(1)
        OpenGL.glBegin(4)
        OpenGL.glColor4ub((-65536 shr 16).toByte(), (-65536 shr 8).toByte(), (-65536).toByte(), (-65536 shr 24).toByte())
        OpenGL.glVertex3f(5.35f, 10.35f, 100.0f)
        OpenGL.glColor4ub((-65536 shr 16).toByte(), (-65536 shr 8).toByte(), (-65536).toByte(), (-65536 shr 24).toByte())
        OpenGL.glVertex3f(75.35f, 50.35f, 100.0f)
        OpenGL.glColor4ub((-65536 shr 16).toByte(), (-65536 shr 8).toByte(), (-65536).toByte(), (-65536 shr 24).toByte())
        OpenGL.glVertex3f(15.35f, 90.35f, 100.0f)
        OpenGL.glEnd()
    }

    override fun setDepthBuffering(zbuffer: Boolean) {
        this.depthBufferingEnabled = zbuffer
        setDepthMask()
    }

    fun renderTexture(materialId: Int) {
        renderTexture(materialId, true)
    }

    override fun drawLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, lineSpacing: Int, lineLength: Int, offset: Int) {
        var offset = offset
        if (endX != startX || startY != endY) {
            setupShapeRendering()
            setBlendMode(1)
            var deltaX = (endX - startX).toFloat()
            var deltaY = (endY - startY).toFloat()
            val inverseMagnitude = (1.0 / sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
            deltaX *= inverseMagnitude
            deltaY *= inverseMagnitude
            OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
            offset %= lineLength + lineSpacing
            val dx1 = deltaX * lineSpacing
            val dy1 = deltaY * lineSpacing
            var dx2 = 0.0f
            var dy2 = 0.0f
            var dx3 = dx1
            var dy3 = dy1
            if (offset > lineSpacing) {
                dx2 = deltaX * (lineLength + lineSpacing - offset)
                dy2 = deltaY * (lineLength + lineSpacing - offset)
            } else {
                dx3 = deltaX * (lineSpacing - offset)
                dy3 = deltaY * (lineSpacing - offset)
            }

            var x = startX + 0.35f + dx2
            var y = startY + 0.35f + dy2
            val dx4 = deltaX * lineLength
            val dy4 = deltaY * lineLength

            while (true) {
                if (endX > startX) {
                    if (x > endX + 0.35f) {
                        break
                    }
                    if (x + dx3 > endX) {
                        dx3 = endX - x
                    }
                } else {
                    if (x < endX + 0.35f) {
                        break
                    }
                    if (x + dx3 < endX) {
                        dx3 = endX - x
                    }
                }

                if (endY > startY) {
                    if (y > endY + 0.35f) {
                        break
                    }
                    if (y + dy3 > endY) {
                        dy3 = endY - y
                    }
                } else {
                    if (y < endY + 0.35f) {
                        break
                    }
                    if (y + dy3 < endY) {
                        dy3 = endY - y
                    }
                }

                OpenGL.glBegin(GL_LINES)
                OpenGL.glVertex2f(x, y)
                OpenGL.glVertex2f(x + dx3, y + dy3)
                OpenGL.glEnd()
                x += dx4 + dx3
                y += dy4 + dy3
                dx3 = dx1
                dy3 = dy1
            }
        }
    }

    override fun drawSurfaceLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, dimension: ComponentSurface, clipLeft: Int, clipTop: Int) {
        val spriteSurface = dimension as SpriteSurfaceGL
        val texture = spriteSurface.surface
        setupSpriteRendering()
        setActiveTexture(spriteSurface.surface)
        setBlendMode(1)
        loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        setTexture(0, GL_PRIMARY_COLOR, GL_SRC_COLOR)
        val textureScaleU = texture!!.s / texture.singleWidth
        val textureScaleV = texture.t / texture.singleLength
        var deltaX = (rightX - leftX).toFloat()
        var deltaY = (bottomY - topY).toFloat()
        val inverseMagnitude = (1.0 / sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
        deltaX *= inverseMagnitude
        deltaY *= inverseMagnitude
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        OpenGL.glBegin(GL_LINES)
        OpenGL.glTexCoord2f(textureScaleU * (leftX - clipLeft), textureScaleV * (topY - clipTop))
        OpenGL.glVertex2f(leftX + 0.35f, topY + 0.35f)
        OpenGL.glTexCoord2f(textureScaleU * (rightX - clipLeft), textureScaleV * (bottomY - clipTop))
        OpenGL.glVertex2f(rightX + deltaX + 0.35f, bottomY + deltaY + 0.35f)
        OpenGL.glEnd()
        setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
    }

    override fun drawTexturedLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: ComponentSurface, clipLeft: Int, clipTop: Int, lineSpacing: Int, lineLength: Int, offset: Int) {
        var offset = offset
        if (endX != startX || startY != endY) {
            val spriteSurface = surface as SpriteSurfaceGL
            val texture = spriteSurface.surface
            setupSpriteRendering()
            setActiveTexture(spriteSurface.surface)
            setBlendMode(1)
            loadTextureCombineMode(7681, 8448)
            setTexture(0, 34167, 768)
            val textureScaleU = texture!!.s / texture.singleWidth
            val textureScaleV = texture.t / texture.singleLength
            var deltaX = (endX - startX).toFloat()
            var deltaY = (endY - startY).toFloat()
            val inverseMagnitude = (1.0 / sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
            deltaX *= inverseMagnitude
            deltaY *= inverseMagnitude
            OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
            offset %= lineLength + lineSpacing
            val dashSpacingX = deltaX * lineSpacing
            val dashSpacingY = deltaY * lineSpacing
            var segStartOffsetX = 0.0f
            var segStartOffsetY = 0.0f
            var segLengthX = dashSpacingX
            var segLengthY = dashSpacingY
            if (offset > lineSpacing) {
                segStartOffsetX = deltaX * (lineLength + lineSpacing - offset)
                segStartOffsetY = deltaY * (lineLength + lineSpacing - offset)
            } else {
                segLengthX = deltaX * (lineSpacing - offset)
                segLengthY = deltaY * (lineSpacing - offset)
            }

            var curX = startX + 0.35f + segStartOffsetX
            var curY = startY + 0.35f + segStartOffsetY
            val dashLengthX = deltaX * lineLength
            val dashLengthY = deltaY * lineLength

            while (true) {
                if (endX > startX) {
                    if (curX > endX + 0.35f) {
                        break
                    }
                    if (curX + segLengthX > endX) {
                        segLengthX = endX - curX
                    }
                } else {
                    if (curX < endX + 0.35f) {
                        break
                    }
                    if (curX + segLengthX < endX) {
                        segLengthX = endX - curX
                    }
                }

                if (endY > startY) {
                    if (curY > endY + 0.35f) {
                        break
                    }
                    if (curY + segLengthY > endY) {
                        segLengthY = endY - curY
                    }
                } else {
                    if (curY < endY + 0.35f) {
                        break
                    }
                    if (curY + segLengthY < endY) {
                        segLengthY = endY - curY
                    }
                }

                OpenGL.glBegin(GL_LINES)
                OpenGL.glTexCoord2f(textureScaleU * (curX - clipLeft), textureScaleV * (curY - clipTop))
                OpenGL.glVertex2f(curX, curY)
                OpenGL.glTexCoord2f(textureScaleU * (curX + segLengthX - clipLeft), textureScaleV * (curY + segLengthY - clipTop))
                OpenGL.glVertex2f(curX + segLengthX, curY + segLengthY)
                OpenGL.glEnd()
                curX += dashLengthX + segLengthX
                curY += dashLengthY + segLengthY
                segLengthX = dashSpacingX
                segLengthY = dashSpacingY
            }

            setTexture(0, 5890, 768)
        }
    }

    override fun drawThickLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, lineWidth: Int, blendMode: Int) {
        OpenGL.glLineWidth(lineWidth.toFloat())
        drawTransparentLine(leftX, topY, rightX, bottomY, colorArgb, blendMode)
        OpenGL.glLineWidth(1.0f)
    }

    override fun calculateProjection(worldX: Float, worldY: Float, worldZ: Float, screenCoords: FloatArray) {
        val clipW = this.viewProjectionMatrix.elements[15] + this.viewProjectionMatrix.elements[3] * worldX + this.viewProjectionMatrix.elements[7] * worldY + this.viewProjectionMatrix.elements[11] * worldZ
        val clipX = this.viewProjectionMatrix.elements[12] + this.viewProjectionMatrix.elements[0] * worldX + this.viewProjectionMatrix.elements[4] * worldY + this.viewProjectionMatrix.elements[8] * worldZ
        val clipY = this.viewProjectionMatrix.elements[13] + this.viewProjectionMatrix.elements[1] * worldX + this.viewProjectionMatrix.elements[5] * worldY + this.viewProjectionMatrix.elements[9] * worldZ
        val viewZ = this.viewMatrix44.elements[14] + this.viewMatrix44.elements[2] * worldX + this.viewMatrix44.elements[6] * worldY + this.viewMatrix44.elements[10] * worldZ
        screenCoords[0] = this.viewportOriginX + this.imageProducerCenterX * clipX / clipW
        screenCoords[1] = this.viewportOriginY + this.imageProducerCenterY * clipY / clipW
        screenCoords[2] = viewZ
    }

    override fun computeWorldToScreen(worldX: Float, worldY: Float, worldZ: Float, screenCoords: FloatArray) {
        val clipZ = this.viewProjectionMatrix.elements[14] + this.viewProjectionMatrix.elements[2] * worldX + this.viewProjectionMatrix.elements[6] * worldY + this.viewProjectionMatrix.elements[10] * worldZ
        val clipW = this.viewProjectionMatrix.elements[15] + this.viewProjectionMatrix.elements[3] * worldX + this.viewProjectionMatrix.elements[7] * worldY + this.viewProjectionMatrix.elements[11] * worldZ
        if (clipZ >= -clipW && clipZ <= clipW) {
            val clipX = this.viewProjectionMatrix.elements[12] + this.viewProjectionMatrix.elements[0] * worldX + this.viewProjectionMatrix.elements[4] * worldY + this.viewProjectionMatrix.elements[8] * worldZ
            if (clipX >= -clipW && clipX <= clipW) {
                val clipY = this.viewProjectionMatrix.elements[13] + this.viewProjectionMatrix.elements[1] * worldX + this.viewProjectionMatrix.elements[5] * worldY + this.viewProjectionMatrix.elements[9] * worldZ
                if (clipY >= -clipW && clipY <= clipW) {
                    val viewZ = this.viewMatrix44.elements[14] + this.viewMatrix44.elements[2] * worldX + this.viewMatrix44.elements[6] * worldY + this.viewMatrix44.elements[10] * worldZ
                    screenCoords[0] = this.viewportOriginX + this.imageProducerCenterX * clipX / clipW
                    screenCoords[1] = this.viewportOriginY + this.imageProducerCenterY * clipY / clipW
                    screenCoords[2] = viewZ
                } else {
                    screenCoords[2] = Float.NaN
                    screenCoords[1] = Float.NaN
                    screenCoords[0] = Float.NaN
                }
            } else {
                screenCoords[2] = Float.NaN
                screenCoords[1] = Float.NaN
                screenCoords[0] = Float.NaN
            }
        } else {
            screenCoords[2] = Float.NaN
            screenCoords[1] = Float.NaN
            screenCoords[0] = Float.NaN
        }
    }

    override fun drawTransparentLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, blendMode: Int) {
        setupShapeRendering()
        setBlendMode(blendMode)
        var deltaX = (rightX - leftX).toFloat()
        var deltaY = (bottomY - topY).toFloat()
        if (deltaX == 0.0f && deltaY == 0.0f) {
            deltaX = 1.0f
        } else {
            val length = (1.0 / sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
            deltaX *= length
            deltaY *= length
        }

        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        OpenGL.glBegin(GL_LINES)
        OpenGL.glVertex2f(leftX + 0.35f, topY + 0.35f)
        OpenGL.glVertex2f(rightX + deltaX + 0.35f, bottomY + deltaY + 0.35f)
        OpenGL.glEnd()
    }

    override fun getNativeMemory(size: Int): NativeMemoryNode {
        val nativeMemory = NativeMemoryGL(size)
        this.heapManager.insertTail(nativeMemory)
        return nativeMemory
    }

    override fun setNativeHeap(nativeMemoryNode: NativeMemoryNode) {
        this.nativeHeap = (nativeMemoryNode as NativeMemoryGL).nativeHeap
        if (this.screenQuadArrayBuffer == null) {
            val textureBuffer = GlBitBuffer(80)
            if (this.bigEndianess) {
                textureBuffer.writeFloatBigEndian(-1.0f)
                textureBuffer.writeFloatBigEndian(-1.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
                textureBuffer.writeFloatBigEndian(-1.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(-1.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
                textureBuffer.writeFloatBigEndian(1.0f)
                textureBuffer.writeFloatBigEndian(0.0f)
            } else {
                textureBuffer.writeFloatLittleEndian(-1.0f)
                textureBuffer.writeFloatLittleEndian(-1.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
                textureBuffer.writeFloatLittleEndian(-1.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(-1.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
                textureBuffer.writeFloatLittleEndian(1.0f)
                textureBuffer.writeFloatLittleEndian(0.0f)
            }

            this.screenQuadArrayBuffer = createArrayBuffer(20, textureBuffer.buffer, textureBuffer.currentPosition, false)
            this.quadVertexBuffer = OpenGlArrayDrawBuffer(this.screenQuadArrayBuffer!!, GL_FLOAT, 3, 0)
            this.quadTexCoordBuffer = OpenGlArrayDrawBuffer(this.screenQuadArrayBuffer!!, GL_FLOAT, 2, 12)
            this.particleRenderer.initialize(this)
        }
    }

    override fun generateEmptySprite(width: Int, height: Int, transparent: Boolean, bool4: Boolean): NativeSprite {
        return SpriteGL(this, width, height, transparent)
    }

    override fun generateSprite(spriteType: SpriteType?, bool2: Boolean): NativeSprite {
        val pixels = IntArray(spriteType!!.width * spriteType.height)
        var src = 0
        var dst = 0
        if (spriteType.alpha != null) {
            for (x in 0 until spriteType.height) {
                for (y in 0 until spriteType.width) {
                    pixels[dst++] = spriteType.alpha!![src].toInt() shl 24 or spriteType.pallete[spriteType.pixels[src++].toInt() and 0xff]
                }
            }
        } else {
            for (x in 0 until spriteType.height) {
                for (y in 0 until spriteType.width) {
                    val pixel = spriteType.pallete[spriteType.pixels[src++].toInt() and 0xff]
                    pixels[dst++] = if (pixel != 0) 0xffffff.inv() or pixel else 0
                }
            }
        }
        val nativeSprite = createNativeSprite(pixels, spriteType.width, spriteType.width, spriteType.height)
        nativeSprite.setSize(spriteType.minX, spriteType.minY, spriteType.borderX, spriteType.borderY)
        return nativeSprite
    }

    override fun createNativeSprite(pixels: IntArray, defaultBackgroundColor: Int, width: Int, useWidth: Int, useHeight: Int, hasAlpha: Boolean): NativeSprite {
        return SpriteGL(this, useWidth, useHeight, pixels, defaultBackgroundColor, width)
    }

    override fun createNativeSprite(width: Int, height: Int, textureWidth: Int, textureHeight: Int, hasAlpha: Boolean): NativeSprite {
        return SpriteGL(this, width, height, textureWidth, textureHeight)
    }

    override fun createComponentSurface(sizeX: Int, sizeY: Int, rowStartX: IntArray, rowWidth: IntArray): ComponentSurface {
        return SpriteSurfaceGL.create(this, sizeX, sizeY, rowStartX, rowWidth)
    }

    override fun clearRectangle(dimension: ComponentSurface, offsetX: Int, offsetY: Int) {
        val spriteSurface = dimension as SpriteSurfaceGL
        val texture = spriteSurface.surface
        setupSpriteRendering()
        setActiveTexture(spriteSurface.surface)
        setBlendMode(1)
        loadTextureCombineMode(GL_REPLACE, GL_MODULATE)
        setTexture(0, 34167, 768)
        val textureScaleU = texture!!.s / texture.singleWidth
        val textureScaleV = texture.t / texture.singleLength
        OpenGL.glColor4ub((0xffffff.inv() shr 16).toByte(), (0xffffff.inv() shr 8).toByte(), (0xffffff.inv()).toByte(), (0xffffff.inv() shr 24).toByte())
        OpenGL.glBegin(GL_QUADS)
        OpenGL.glTexCoord2f(textureScaleU * (this.clipLeft - offsetX), textureScaleV * (this.clipTop - offsetY))
        OpenGL.glVertex2i(this.clipLeft, this.clipTop)
        OpenGL.glTexCoord2f(textureScaleU * (this.clipLeft - offsetX), textureScaleV * (this.clipBottom - offsetY))
        OpenGL.glVertex2i(this.clipLeft, this.clipBottom)
        OpenGL.glTexCoord2f(textureScaleU * (this.clipRight - offsetX), textureScaleV * (this.clipBottom - offsetY))
        OpenGL.glVertex2i(this.clipRight, this.clipBottom)
        OpenGL.glTexCoord2f(textureScaleU * (this.clipRight - offsetX), textureScaleV * (this.clipTop - offsetY))
        OpenGL.glVertex2i(this.clipRight, this.clipTop)
        OpenGL.glEnd()
        setTexture(0, 5890, 768)
    }

    override fun initialize() {
    }

    override fun createModel(mesh: Mesh, functionMask: Int, preferenceMask: Int, ambient: Int, contrast: Int): Model {
        return ModelGL(this, mesh, functionMask, ambient, contrast, preferenceMask)
    }

    override fun mismatchingFunctionMask(functionMaskA: Int, functionMaskB: Int): Int {
        return functionMaskA and functionMaskB xor functionMaskB
    }

    override fun calculateFog(color: Int, density: Int, offset: Int) {
        if (this.fogColor != color || density != this.fogDensity || offset != this.fogOffset) {
            this.fogColor = color
            this.fogDensity = density
            this.fogOffset = offset
            setFogRange()
            toggleFog()
        }
    }

    override fun getScratchMatrix(): Matrix44 = this.scratchMatrix

    override fun getRenderingMatrix(): Matrix43 = this.renderingMatrix

    override fun renderParticles(particleWrapper: ParticleWrapper) {
        this.particleRenderer.drawParticles(this, particleWrapper)
    }

    override fun addWater(waterTile: WaterTile) {
        this.waterDepth = -1
        this.waterTile = waterTile
        this.stillWaterToggled = true
    }

    override fun addWaterTile(waterDepth: Int, waterTile: WaterTile) {
        if (!this.stillWaterToggled) {
            throw RuntimeException("")
        }
        this.waterDepth = waterDepth
        this.waterTile = waterTile
        if (this.renderStillWater) {
            this.effectManager!!.waterEffect.enable()
            this.effectManager!!.waterEffect.activate()
        }
    }

    override fun getRenderBuffer(width: Int, height: Int): RenderBuffer {
        return OpenGlRenderBuffer(this, GLFormat.DEPTH, GLActionFormat.DEPTH_COMPONENT_24, width, height)
    }

    override fun calculateViewportCenter() {
        if (display == null) {
            return
        }
        this.drawableX = 0
        this.drawableY = 0
        this.imageProducerWidth = display!!.getWidth()
        this.imageProducerHeight = display!!.getHeight()
        updateViewportAndCenter()
    }

    override fun setDrawableArea(x: Int, y: Int, width: Int, height: Int) {
        this.drawableX = x
        this.drawableY = y
        this.imageProducerWidth = width
        this.imageProducerHeight = height
        updateViewportAndCenter()
    }

    override fun getClipping(clipping: IntArray) {
        clipping[0] = this.clipLeft
        clipping[1] = this.clipTop
        clipping[2] = this.clipRight
        clipping[3] = this.clipBottom
    }

    override fun resetClipping() {
        if (display != null) {
            this.clipLeft = 0
            this.clipTop = 0
            this.clipRight = display!!.getWidth()
            this.clipBottom = display!!.getHeight()
            OpenGL.glDisable(GL_SCISSOR_TEST)
        }
    }

    override fun setClipping(left: Int, top: Int, right: Int, bottom: Int) {
        var left = left
        var top = top
        var right = right
        var bottom = bottom
        if (display != null) {
            if (left < 0) {
                left = 0
            }
            if (right > display!!.getWidth()) {
                right = display!!.getWidth()
            }
            if (top < 0) {
                top = 0
            }
            if (bottom > display!!.getHeight()) {
                bottom = display!!.getHeight()
            }
            this.clipLeft = left
            this.clipTop = top
            this.clipRight = right
            this.clipBottom = bottom
            OpenGL.glEnable(GL_SCISSOR_TEST)
            defineScissorBox()
        }
    }

    override fun expandClipping(left: Int, top: Int, right: Int, bottom: Int) {
        if (this.clipLeft < left) {
            this.clipLeft = left
        }
        if (this.clipRight > right) {
            this.clipRight = right
        }
        if (this.clipTop < top) {
            this.clipTop = top
        }
        if (this.clipBottom > bottom) {
            this.clipBottom = bottom
        }
        OpenGL.glEnable(GL_SCISSOR_TEST)
        defineScissorBox()
    }

    override fun onTilesDrawn() {
        this.stillWaterToggled = false
    }

    fun updateViewportAndCenter() {
        if (display == null) return
        val viewX: Int
        val viewY: Int
        val viewWidth: Int
        val viewHeight: Int
        if (this.depthClippingMode == 2) {
            viewX = this.drawableX
            viewY = this.drawableY
            viewWidth = this.imageProducerWidth
            viewHeight = this.imageProducerHeight
        } else {
            viewX = 0
            viewY = 0
            viewWidth = display!!.getWidth()
            viewHeight = display!!.getHeight()
        }

        var width = viewWidth
        var height = viewHeight
        if (width < 1) {
            width = 1
        }
        if (height < 1) {
            height = 1
        }

        OpenGL.glViewport(viewX + this.viewportOffsetX, this.viewportOffsetY + display!!.getHeight() - viewY - height, width, height)
        this.imageProducerCenterX = this.imageProducerWidth / 2.0f
        this.imageProducerCenterY = this.imageProducerHeight / 2.0f
        this.viewportOriginX = this.drawableX + this.imageProducerCenterX
        this.viewportOriginY = this.drawableY + this.imageProducerCenterY
    }

    @Synchronized
    override fun animateTextures(delta: Int) {
        var handleCount = 0
        val maskedDelta = delta and 0x7fffffff
        while (!this.vboBuffersAwaitingDeallocation.isEmpty()) {
            val handle = this.vboBuffersAwaitingDeallocation.popTail() as IntNode
            HANDLE_BUFFER[handleCount++] = handle.pointer.toInt()
            this.vboBuffersSize -= handle.value
            if (handleCount == 1000) {
                OpenGL.glDeleteBuffersARB(1000, HANDLE_BUFFER, 0)
                handleCount = 0
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteBuffersARB(handleCount, HANDLE_BUFFER, 0)
            handleCount = 0
        }
        while (!this.texturesAwaitingDeallocation.isEmpty()) {
            val handle = this.texturesAwaitingDeallocation.popTail() as IntNode
            HANDLE_BUFFER[handleCount++] = handle.pointer.toInt()
            this.textureBuffersSize -= handle.value
            if (handleCount == 1000) {
                OpenGL.glDeleteTextures(1000, HANDLE_BUFFER, 0)
                handleCount = 0
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteTextures(handleCount, HANDLE_BUFFER, 0)
            handleCount = 0
        }
        while (!this.frameBuffersAwaitingDeallocation.isEmpty()) {
            val handle = this.frameBuffersAwaitingDeallocation.popTail() as IntNode
            HANDLE_BUFFER[handleCount++] = handle.value
            if (handleCount == 1000) {
                OpenGL.glDeleteFramebuffersEXT(1000, HANDLE_BUFFER, 0)
                handleCount = 0
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteFramebuffersEXT(handleCount, HANDLE_BUFFER, 0)
            handleCount = 0
        }
        while (!this.renderBuffersAwaitingRemoval.isEmpty()) {
            val renderBufferNode = this.renderBuffersAwaitingRemoval.popTail() as IntNode
            HANDLE_BUFFER[handleCount++] = renderBufferNode.pointer.toInt()
            this.renderBuffersSize -= renderBufferNode.value
            if (handleCount == 1000) {
                OpenGL.glDeleteRenderbuffersEXT(1000, HANDLE_BUFFER, 0)
                handleCount = 0
            }
        }
        if (handleCount > 0) {
            OpenGL.glDeleteRenderbuffersEXT(handleCount, HANDLE_BUFFER, 0)
        }
        while (!this.listsAwaitingDeallocation.isEmpty()) {
            val handle = this.listsAwaitingDeallocation.popTail() as IntNode
            OpenGL.glDeleteLists(handle.pointer.toInt(), handle.value)
        }
        while (!this.arbProgramsAwaitingDeallocation.isEmpty()) {
            val handle = this.arbProgramsAwaitingDeallocation.popTail()!!
            OpenGL.glDeleteProgramARB(handle.pointer.toInt())
        }
        while (!this.shaderProgramsAwaitingDeallocation.isEmpty()) {
            val handle = this.shaderProgramsAwaitingDeallocation.popTail()!!
            OpenGL.glDeleteShader(handle.pointer.toInt())
        }
        while (!this.listsAwaitingDeallocation.isEmpty()) {
            val handle = this.listsAwaitingDeallocation.popTail() as IntNode
            OpenGL.glDeleteLists(handle.pointer.toInt(), handle.value)
        }
        this.textureCache?.cycleBloomCache()
        if (getOffheap() > 100663296 && Utils.time() > this.lastGcCycle + 60000L) {
            System.gc()
            this.lastGcCycle = Utils.time()
        }
        this.textureAnimatingProgression = maskedDelta
    }

    override fun drawCircle(x: Int, y: Int, radius: Int, colorArgb: Int) {
        var radius = radius
        if (radius < 0) {
            radius = -radius
        }

        if (radius + x >= this.clipLeft && x - radius <= this.clipRight && radius + y >= this.clipTop && y - radius <= this.clipBottom) {
            setupShapeRendering()
            setBlendMode(1)
            OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
            val startX = x + 0.35f
            val startY = y + 0.35f
            val pointSize = radius shl 1
            if (pointSize < this.fillCirclePointSize) {
                // Filled circle
                OpenGL.glBegin(GL_QUADS)
                OpenGL.glVertex2f(startX + 1.0f, startY + 1.0f)
                OpenGL.glVertex2f(startX + 1.0f, startY - 1.0f)
                OpenGL.glVertex2f(startX - 1.0f, startY - 1.0f)
                OpenGL.glVertex2f(startX - 1.0f, startY + 1.0f)
                OpenGL.glEnd()
            } else if (pointSize <= this.outlineCirclePointSize) {
                // Circle outline
                OpenGL.glEnable(GL_POINT_SMOOTH)
                OpenGL.glPointSize(pointSize.toFloat())
                OpenGL.glBegin(GL_POINTS)
                OpenGL.glVertex2f(startX, startY)
                OpenGL.glEnd()
                OpenGL.glDisable(GL_POINT_SMOOTH)
            } else {
                // Filled circle with complex outline
                OpenGL.glBegin(GL_TRIANGLE_FAN)
                OpenGL.glVertex2f(startX, startY)
                var segments = 262144 / (radius * 6)
                if (segments <= 64) {
                    segments = 64
                } else if (segments > 512) {
                    segments = 512
                }

                segments = MathUtils.furtestBitValue(segments)
                OpenGL.glVertex2f(startX + radius, startY)

                var i = 16384 - segments
                while (i > 0) {
                    OpenGL.glVertex2f(startX + COSINE[i] * radius, startY + SINE[i] * radius)
                    i -= segments
                }

                OpenGL.glVertex2f(startX + radius, startY)
                OpenGL.glEnd()
            }
        }
    }

    fun loadMatrix(matrix4x4: Matrix44) {
        OpenGL.glLoadMatrixf(matrix4x4.elements, 0)
    }

    override fun setSunPosition(matrix44var1: Matrix43) {
        this.viewMatrix43.copyFrom(matrix44var1)
        this.viewMatrix44.fromMatrix(this.viewMatrix43)
        this.inverseSunMatrix43.copyFrom(matrix44var1)
        this.inverseSunMatrix43.updateIdentitiyMatrix()
        this.inverseSunMatrix44.fromMatrix(this.inverseSunMatrix43)
        updateViewProjectionMatrix()
        if (this.depthClippingMode != 1) {
            updateModelViewAndLights()
        }
    }

    override fun copySunPosition(): Matrix43 = Matrix43(this.viewMatrix43)

    override fun copyModelMatrix(): Matrix44 = Matrix44(this.currentModelMatrix)

    fun setOrthoMatrixMode() {
        if (this.depthClippingMode != 0) {
            this.depthClippingMode = 0
            updateViewportAndCenter()
            updateDepthRange()
            this.functionMask = this.functionMask and -16
        }
    }

    fun setProjectionMatrixMode() {
        if (this.depthClippingMode != 1) {
            this.depthClippingMode = 1
            recalculateFogAndProjection()
            updateViewportAndCenter()
            updateDepthRange()
            OpenGL.glMatrixMode(GL_MODELVIEW)
            OpenGL.glLoadIdentity()
            this.functionMask = this.functionMask and -9
        }
    }

    fun recalculateFogAndProjection() {
        this.fogFarDistance = this.currentModelMatrix.getDepthPerspectiveCD()
        this.fogMinimumStartPosition = this.currentModelMatrix.getNearClipDistance()
        setFogRange()
        if (this.depthClippingMode == 2) {
            invertMatrix(this.currentModelMatrix.elements)
        } else if (this.depthClippingMode == 1) {
            invertMatrix(this.orthoProjectionMatrix.elements)
        }
    }

    override fun utilisesNativeHeap(): Boolean = true

    override fun beginBloomCapture(screenCanvasWidth: Int, screenCanvasHeight: Int) {
        this.framebufferCompositor?.prepareForRendering(screenCanvasWidth, screenCanvasHeight)
    }

    override fun updateSun(sunBrightness: Float) {
        if (this.sunBrightness != sunBrightness) {
            this.sunBrightness = sunBrightness
            updateSunColor()
        }
    }

    override fun applyLighting(sunColor: Int, intensity: Float, antiIntensity: Float, x: Float, y: Float, z: Float) {
        val calculateSunColor = this.sunColorRgb != sunColor
        if (calculateSunColor || this.sunColorIntensity != intensity || this.antiSunColorIntensity != antiIntensity) {
            this.sunColorRgb = sunColor
            this.sunColorIntensity = intensity
            this.antiSunColorIntensity = antiIntensity
            if (calculateSunColor) {
                this.sunColorRed = (this.sunColorRgb and 0xff0000) / 1.671168E7f
                this.sunColorGreen = (this.sunColorRgb and 0xff00) / 65280.0f
                this.sunColorBlue = (this.sunColorRgb and 0xff) / 255.0f
                updateSunColor()
            }
            updateSunLightDiffuse()
        }

        if (this.sunPreviousPosition[0] != x || this.sunPreviousPosition[1] != y || this.sunPreviousPosition[2] != z) {
            this.sunPreviousPosition[0] = x
            this.sunPreviousPosition[1] = y
            this.sunPreviousPosition[2] = z
            this.antiSunPreviousPosition[0] = -x
            this.antiSunPreviousPosition[1] = -y
            this.antiSunPreviousPosition[2] = -z
            val length = (1.0 / sqrt((x * x + y * y + z * z).toDouble())).toFloat()
            this.sunDirection[0] = x * length
            this.sunDirection[1] = y * length
            this.sunDirection[2] = z * length
            this.lightingDirection[0] = -this.sunDirection[0]
            this.lightingDirection[1] = -this.sunDirection[1]
            this.lightingDirection[2] = -this.sunDirection[2]
            setLightingSourceParams()
            this.shadowOffsetX = (x * 256.0f / y).toInt()
            this.shadowOffsetZ = (z * 256.0f / y).toInt()
        }
    }

    override fun isOnScreen(minX: Int, minY: Int, minZ: Int, maxX: Int, maxY: Int, maxZ: Int): Int {
        var clipFlags = 0
        val clipZMin = this.viewProjectionMatrix.elements[14] + this.viewProjectionMatrix.elements[2] * minX + this.viewProjectionMatrix.elements[6] * minY + this.viewProjectionMatrix.elements[10] * minZ
        val clipZMax = this.viewProjectionMatrix.elements[14] + this.viewProjectionMatrix.elements[2] * maxX + this.viewProjectionMatrix.elements[6] * maxY + this.viewProjectionMatrix.elements[10] * maxZ
        val clipWMin = this.viewProjectionMatrix.elements[15] + this.viewProjectionMatrix.elements[3] * minX + this.viewProjectionMatrix.elements[7] * minY + this.viewProjectionMatrix.elements[11] * minZ
        val clipWMax = this.viewProjectionMatrix.elements[15] + this.viewProjectionMatrix.elements[3] * maxX + this.viewProjectionMatrix.elements[7] * maxY + this.viewProjectionMatrix.elements[11] * maxZ
        if (clipZMin < -clipWMin && clipZMax < -clipWMax) {
            clipFlags = clipFlags or 0x10
        } else if (clipZMin > clipWMin && clipZMax > clipWMax) {
            clipFlags = clipFlags or 0x20
        }

        val clipXMin = this.viewProjectionMatrix.elements[12] + this.viewProjectionMatrix.elements[0] * minX + this.viewProjectionMatrix.elements[4] * minY + this.viewProjectionMatrix.elements[8] * minZ
        val clipXMax = this.viewProjectionMatrix.elements[12] + this.viewProjectionMatrix.elements[0] * maxX + this.viewProjectionMatrix.elements[4] * maxY + this.viewProjectionMatrix.elements[8] * maxZ
        if (clipXMin < -clipWMin && clipXMax < -clipWMax) {
            clipFlags = clipFlags or 0x1
        }

        if (clipXMin > clipWMin && clipXMax > clipWMax) {
            clipFlags = clipFlags or 0x2
        }

        val clipYMin = this.viewProjectionMatrix.elements[13] + this.viewProjectionMatrix.elements[1] * minX + this.viewProjectionMatrix.elements[5] * minY + this.viewProjectionMatrix.elements[9] * minZ
        val clipYMax = this.viewProjectionMatrix.elements[13] + this.viewProjectionMatrix.elements[1] * maxX + this.viewProjectionMatrix.elements[5] * maxY + this.viewProjectionMatrix.elements[9] * maxZ
        if (clipYMin < -clipWMin && clipYMax < -clipWMax) {
            clipFlags = clipFlags or 0x4
        }

        if (clipYMin > clipWMin && clipYMax > clipWMax) {
            clipFlags = clipFlags or 0x8
        }

        return clipFlags
    }

    override fun createFontRenderer(fontMetrics: FontMetrics?, spriteTypes: Array<SpriteType>?, isPlain: Boolean): AbstractFontRenderer {
        return FontRendererGL(this, fontMetrics!!, spriteTypes!!, isPlain)
    }

    fun configureFog(densityFactor: Float, fogMinDistance: Float) {
        this.densityFactor = densityFactor // 0.5
        this.fogMin = fogMinDistance // fog_density
        setFogRange()
    }

    fun updateSunLightDiffuse() {
        BUFFER_4F[0] = this.sunColorIntensity * this.sunColorRed
        BUFFER_4F[1] = this.sunColorIntensity * this.sunColorGreen
        BUFFER_4F[2] = this.sunColorIntensity * this.sunColorBlue
        BUFFER_4F[3] = 1.0f
        OpenGL.glLightfv(GL_COLOR_BUFFER_BIT, GL_DIFFUSE, BUFFER_4F, 0)
        BUFFER_4F[0] = -this.antiSunColorIntensity * this.sunColorRed
        BUFFER_4F[1] = -this.antiSunColorIntensity * this.sunColorGreen
        BUFFER_4F[2] = -this.antiSunColorIntensity * this.sunColorBlue
        BUFFER_4F[3] = 1.0f
        OpenGL.glLightfv(GL_LIGHT1, GL_DIFFUSE, BUFFER_4F, 0)
    }

    fun setLightingSourceParams() {
        OpenGL.glLightfv(GL_COLOR_BUFFER_BIT, GL_POSITION, this.sunDirection, 0)
        OpenGL.glLightfv(GL_LIGHT1, GL_POSITION, this.lightingDirection, 0)
    }

    override fun hasShadows(): Boolean = true

    fun setFogRange() {
        this.fogEndPosition = this.fogFarDistance - this.fogOffset - this.fogMin
        this.fogStartPosition = this.fogEndPosition - this.fogDensity * this.densityFactor
        if (this.fogStartPosition < this.fogMinimumStartPosition) {
            this.fogStartPosition = this.fogMinimumStartPosition
        }

        OpenGL.glFogf(GL_FOG_START, this.fogStartPosition)
        OpenGL.glFogf(GL_FOG_END, this.fogEndPosition)
        BUFFER_4F[0] = (this.fogColor and 0xff0000) / 1.671168E7f
        BUFFER_4F[1] = (this.fogColor and 0xff00) / 65280.0f
        BUFFER_4F[2] = (this.fogColor and 0xff) / 255.0f
        OpenGL.glFogfv(GL_FOG_COLOR, BUFFER_4F, 0)
    }

    fun setTexture(activeUnit: Int, source: Int, operand: Int) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, activeUnit + GL_SOURCE0_RGB, source)
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, activeUnit + GL_OPERAND0_RGB, operand)
    }

    override fun createCubeTexture(materialAtPositiveX: Int, materialAtNegativeX: Int, materialAtPositiveY: Int, materialAtNegativeY: Int, materialAtPositiveZ: Int, materialAtNegativeZ: Int): AbstractCubeTextureGenerator {
        return if (this.arbTextureCubeMap) OpenGLArbTextureCubeMapGenerator(this, materialAtPositiveX, materialAtNegativeX, materialAtPositiveY, materialAtNegativeY, materialAtPositiveZ, materialAtNegativeZ) else throw UnsupportedOperationException("Cube map not supported")
    }

    override fun drawQuad(cubeTexA: AbstractCubeTextureGenerator?, cubeTexB: AbstractCubeTextureGenerator?, alpha: Float, previousResult: AbstractCubeTextureGenerator?): AbstractCubeTextureGenerator? {
        if (cubeTexA != null && cubeTexB != null && this.arbTextureCubeMap && this.intelGpu) {
            var cubeTextureGenerator: OpenGlCubeTextureGenerator? = null
            val cubeGenA = cubeTexA as AbstractOpenGlCubeTextureGenerator
            val cubeGenB = cubeTexB as AbstractOpenGlCubeTextureGenerator
            val cubeTextureA = cubeGenA.getCubeTexture()
            val cubeTextureB = cubeGenB.getCubeTexture()
            if (cubeTextureA != null && cubeTextureB != null) {
                val largestMaterialSize = Math.max(cubeTextureA.largestMaterialSize, cubeTextureB.largestMaterialSize)
                if (cubeTexA !== previousResult && cubeTexB !== previousResult && previousResult is OpenGlCubeTextureGenerator) {
                    if (previousResult.getLargestMaterialSize() == largestMaterialSize) {
                        cubeTextureGenerator = previousResult
                    }
                }

                if (cubeTextureGenerator == null) {
                    cubeTextureGenerator = OpenGlCubeTextureGenerator(this, largestMaterialSize)
                }

                if (cubeTextureGenerator.drawQuad(cubeTextureA, cubeTextureB, alpha)) {
                    return cubeTextureGenerator
                }
            }
        }

        return if (alpha < 0.5f) cubeTexA else cubeTexB
    }

    fun getCubeMap(): OpenGlArbCubeMapTexture? {
        return this.sunCubeTextureGenerator?.getCubeTexture()
    }

    override fun bloomSupported(): Boolean {
        if (this.rectangularTexture != null) {
            if (!this.rectangularTexture!!.isActive()) {
                if (!this.framebufferCompositor!!.addEffect(this.rectangularTexture!!)) {
                    return false
                }
                this.textureCache?.resetBloomCache()
            }
            return true
        }
        return false
    }

    override fun resetBloom() {
        if (this.rectangularTexture != null && this.rectangularTexture!!.isActive()) {
            this.framebufferCompositor!!.removeEffect(this.rectangularTexture!!)
            this.textureCache?.resetBloomCache()
        }
    }

    override fun bloomEnabled(): Boolean {
        return this.rectangularTexture != null && this.rectangularTexture!!.isActive()
    }

    override fun setBloomFilter(whitePoint: Float, bloom: Float, brightpass: Float) {
        RectangularTextureGL.whitePoint = whitePoint
        RectangularTextureGL.bloom = bloom
        RectangularTextureGL.brightpass = brightpass
    }

    override fun compositeBloomEffects() {
        this.framebufferCompositor?.compositeEffects()
    }

    override fun getImageProducer(): DirectDisplay {
        return IntegratedGraphicsDisplayGL(this)
    }

    fun updateViewProjectionMatrix() {
        this.viewProjectionMatrix.copyFrom(this.viewMatrix44)
        this.viewProjectionMatrix.multiplyBy(this.currentModelMatrix)
        this.viewProjectionMatrix.extractNearPlane(this.frustumPlanes[0])
        this.viewProjectionMatrix.extractFarPlane(this.frustumPlanes[1])
        this.viewProjectionMatrix.extractLeftPlane(this.frustumPlanes[2])
        this.viewProjectionMatrix.extractRightPlane(this.frustumPlanes[3])
        this.viewProjectionMatrix.extractBottomPlane(this.frustumPlanes[4])
        this.viewProjectionMatrix.extractTopPlane(this.frustumPlanes[5])
    }

    fun createElementsArrayBuffer(data: ByteArray, size: Int, isDynamic: Boolean): ElementsBufferGL {
        return if (!this.vertexbufferSupported || isDynamic && !this.shouldUseArbVbo) GlElementsArrayBuffer(this, data, size) else GlVboElementsArrayBuffer(this, data, size, isDynamic)
    }

    fun createArrayBuffer(size: Int, buffer: ByteArray?, position: Int, isDynamic: Boolean): ArrayBufferGL {
        return if (!this.vertexbufferSupported || isDynamic && !this.shouldUseArbVbo) GlArrayBuffer(this, size, buffer, position) else GlVboArrayBuffer(this, size, buffer ?: ByteArray(position), position, isDynamic)
    }

    fun createDataArrayBuffer(stride: Int, buffer: Buffer, size: Int): ArrayBufferGL {
        return if (!this.vertexbufferSupported) GlArrayBuffer(this, stride, buffer) else GlVboArrayBuffer(this, stride, buffer, size)
    }

    fun setArrayBuffer(arrayBuffer: ArrayBufferGL) {
        if (arrayBuffer !== this.currentArrayBuffer) {
            if (this.vertexbufferSupported) {
                OpenGL.glBindBufferARB(GL_ARRAY_BUFFER, arrayBuffer.getId())
            }
            this.currentArrayBuffer = arrayBuffer
        }
    }

    override fun resetTextureState() {
    }

    fun renderAnimatedTexture(x: Float, y: Float) {
        OpenGL.glMatrixMode(GL_TEXTURE)
        if (this.textureUpdate) {
            OpenGL.glLoadIdentity()
        }

        OpenGL.glTranslatef(x, y, 0.0f)
        OpenGL.glMatrixMode(GL_MODELVIEW)
        this.textureUpdate = true
    }

    fun drawArrays(count: Int) {
        OpenGL.glDrawArrays(7, 0, count)
    }

    fun setupShapeRendering() {
        if (this.functionMask != 1) {
            setProjectionMatrixMode()
            toggleFog(false)
            setLightingEnabled(false)
            toggleZbuffer(false)
            setDepthMask(false)
            setActiveTexture(null)
            renderTexture(-2)
            loadTextureCombineMode(1)
            this.functionMask = 1
        }
    }

    fun setupSpriteRendering() {
        if (this.functionMask != 2) {
            setProjectionMatrixMode()
            toggleFog(false)
            setLightingEnabled(false)
            toggleZbuffer(false)
            setDepthMask(false)
            renderTexture(-2)
            this.functionMask = 2
        }
    }

    fun setupTextRendering() {
        if (this.functionMask != 4) {
            setProjectionMatrixMode()
            toggleFog(false)
            setLightingEnabled(false)
            toggleZbuffer(false)
            setDepthMask(false)
            renderTexture(-2)
            setBlendMode(1)
            this.functionMask = 4
        }
    }

    fun setupMatrixRendering() {
        if (this.functionMask != 8) {
            setModelViewMatrixMode()
            toggleFog(true)
            toggleZbuffer(true)
            setDepthMask(true)
            setBlendMode(1)
            this.functionMask = 8
        }
    }

    fun renderTexture(materialId: Int, hasLight: Boolean) {
        renderTexture(materialId, hasLight, true)
    }

    fun renderTexture(materialId: Int, hasLight: Boolean, has0x8: Boolean) {
        // When textures are disabled, treat all materials as untextured
        val effectiveMaterialId = if (texturesEnabled) materialId else -1
        if (this.lastMaterialId != effectiveMaterialId || this.renderStillWater != this.stillWaterToggled) {
            var texture: OpenGl2DTexture? = null
            var combineMode = 0
            var param1 = 0
            var param2 = 0
            var effectId = if (this.stillWaterToggled) 3 else 0
            if (effectiveMaterialId < 0) {
                renderStationaryTexture()
            } else {
                texture = this.textureCache!!.get(effectiveMaterialId)
                val material = materialProvider!!.getMaterial(effectiveMaterialId)
                if (material.textureSpeedU.toInt() == 0 && material.textureSpeedV.toInt() == 0) {
                    renderStationaryTexture()
                } else {
                    renderAnimatedTexture((this.textureAnimatingProgression % 128000) / 1000.0f * material.textureSpeedU / 64.0f % 1.0f, (this.textureAnimatingProgression % 128000) / 1000.0f * material.textureSpeedV / 64.0f % 1.0f)
                }
                if (!this.stillWaterToggled) {
                    param1 = material.effectParam1.toInt()
                    param2 = material.effectParam2.toInt()
                    effectId = material.effectId.toInt()
                }
                combineMode = material.combineMode.toInt()
            }
            this.effectManager!!.loadEffect(effectId, param1, param2, hasLight, has0x8)
            if (!this.effectManager!!.loadCombiner(texture, combineMode)) {
                setActiveTexture(texture)
                loadTextureCombineMode(combineMode)
            }
            this.renderStillWater = this.stillWaterToggled
            this.lastMaterialId = effectiveMaterialId
        }
        this.functionMask = this.functionMask and -8
    }

    fun activateTexture(textureUnit: Int) {
        if (this.glTexturesCursorPos != textureUnit) {
            OpenGL.glActiveTexture(textureUnit + GL_TEXTURE0)
            this.glTexturesCursorPos = textureUnit
        }
    }

    fun drawElements(elementsBuffer: ElementsBufferGL, startIndex: Int, indexCount: Int) {
        setElementsBuffer(elementsBuffer)
        OpenGL.glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_SHORT, elementsBuffer.getBufferAddress() + (startIndex * 2L))
    }

    fun loadTextureCombineMode(mode: Int) {
        if (mode == 1) {
            loadTextureCombineMode(GL_REPLACE, GL_REPLACE)
        } else if (mode == 0) {
            loadTextureCombineMode(GL_MODULATE, GL_MODULATE)
        } else if (mode == 2) {
            loadTextureCombineMode(GL_INTERPOLATE, GL_INTERPOLATE)
        } else if (mode == 3) {
            loadTextureCombineMode(GL_ADD, GL_MODULATE)
        } else if (mode == 4) {
            loadTextureCombineMode(GL_SUBTRACT, GL_SUBTRACT)
        }
    }

    fun getCombineModeEffectType(mode: Int): Int {
        return when (mode) {
            1 -> GL_REPLACE
            0 -> GL_MODULATE
            2 -> GL_INTERPOLATE
            3 -> GL_ADD
            4 -> GL_SUBTRACT
            else -> throw IllegalArgumentException()
        }
    }

    fun setTextureTransparency(textureUnit: Int, alpha: Int) {
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, textureUnit + GL_SOURCE0_ALPHA, alpha)
        OpenGL.glTexEnvi(GL_TEXTURE_ENV, textureUnit + GL_OPERAND0_ALPHA, GL_SRC_ALPHA)
    }

    fun setTextureEnvColor(textureColorArgb: Int) {
        BUFFER_4F[0] = (textureColorArgb and 0xff0000) / 1.671168E7f
        BUFFER_4F[1] = (textureColorArgb and 0xff00) / 65280.0f
        BUFFER_4F[2] = (textureColorArgb and 0xff) / 255.0f
        BUFFER_4F[3] = (textureColorArgb ushr 24) / 255.0f
        OpenGL.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, BUFFER_4F, 0)
    }

    fun renderStationaryTexture() {
        if (this.textureUpdate) {
            OpenGL.glMatrixMode(GL_TEXTURE)
            OpenGL.glLoadIdentity()
            OpenGL.glMatrixMode(GL_MODELVIEW)
            this.textureUpdate = false
        }
    }

    fun setLightingEnabled(enabled: Boolean) {
        if (this.lightingEnabled != enabled) {
            this.lightingEnabled = enabled
            toggleLighting()
            this.functionMask = this.functionMask and -8
        }
    }

    fun setHideLights(enabled: Boolean) {
        if (this.hideLights != enabled) {
            this.hideLights = enabled
            toggleLighting()
        }
    }

    fun toggleLighting() {
        if (this.lightingEnabled && !this.hideLights) {
            OpenGL.glEnable(GL_LIGHTING)
        } else {
            OpenGL.glDisable(GL_LIGHTING)
        }
    }

    fun setDepthMask(enableMask: Boolean) {
        if (this.enableDepthMask != enableMask) {
            this.enableDepthMask = enableMask
            setDepthMask()
            this.functionMask = this.functionMask and -16
        }
    }

    fun setBlendMode(blendAlpha: Int) {
        if (this.activeBlendAlpha != blendAlpha) {
            val blendType: Int
            val testAlpha: Boolean
            if (blendAlpha == 1) {
                blendType = BlendType.ALPHA
                testAlpha = true
            } else if (blendAlpha == 2) {
                blendType = BlendType.ADDITIVE
                testAlpha = false
            } else if (blendAlpha == 128) {
                blendType = BlendType.MULTIPLY
                testAlpha = true
            } else {
                blendType = BlendType.DISABLED
                testAlpha = false
            }
            if (!this.colormaskActive) {
                OpenGL.glColorMask(true, true, true, true)
                this.colormaskActive = true
            }
            if (testAlpha != this.alphaTestEnabled) {
                if (testAlpha) {
                    OpenGL.glEnable(GL_ALPHA_TEST)
                } else {
                    OpenGL.glDisable(GL_ALPHA_TEST)
                }
                this.alphaTestEnabled = testAlpha
            }
            if (blendType != this.activeBlendType) {
                when (blendType) {
                    BlendType.ALPHA -> {
                        OpenGL.glEnable(GL_BLEND)
                        OpenGL.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                    }
                    BlendType.ADDITIVE -> {
                        OpenGL.glEnable(GL_BLEND)
                        OpenGL.glBlendFunc(GL_ONE, GL_ONE)
                    }
                    BlendType.MULTIPLY -> {
                        OpenGL.glEnable(GL_BLEND)
                        OpenGL.glBlendFunc(GL_DST_COLOR, GL_ONE)
                    }
                    else -> {
                        OpenGL.glDisable(GL_BLEND)
                    }
                }
                this.activeBlendType = blendType
            }
            this.activeBlendAlpha = blendAlpha
            this.functionMask = this.functionMask and -13
        }
    }

    override fun getOffheap(): Int {
        return this.vboBuffersSize + this.textureBuffersSize + this.renderBuffersSize
    }

    override fun replaceCanvasOnDisplayUpdate(): Boolean = false

    @Synchronized
    fun requestTextureCleanup(handle: Int, sizeRequirement: Int) {
        val handleAwaitingRemoval = IntNode(sizeRequirement)
        handleAwaitingRemoval.pointer = handle.toLong()
        this.texturesAwaitingDeallocation.insertTail(handleAwaitingRemoval)
    }

    @Synchronized
    fun requestShaderProgramCleanup(programHandle: Long) {
        val handleAwaitingRemoval = Node<Any?>()
        handleAwaitingRemoval.pointer = programHandle
        this.shaderProgramsAwaitingDeallocation.insertTail(handleAwaitingRemoval)
    }

    @Synchronized
    fun requestArbProgramCleanup(programHandle: Int) {
        val handleAwaitingRemoval = Node<Any?>()
        handleAwaitingRemoval.pointer = programHandle.toLong()
        this.arbProgramsAwaitingDeallocation.insertTail(handleAwaitingRemoval)
    }

    @Synchronized
    fun requestArbBufferCleanup(bufferHandle: Int, bufferSize: Int) {
        val handleAwaitingRemoval = IntNode(bufferSize)
        handleAwaitingRemoval.pointer = bufferHandle.toLong()
        this.vboBuffersAwaitingDeallocation.insertTail(handleAwaitingRemoval)
    }

    @Synchronized
    fun requestFrameBufferCleanup(bufferSize: Int) {
        val handleAwaitingRemoval = IntNode(bufferSize)
        this.frameBuffersAwaitingDeallocation.insertTail(handleAwaitingRemoval)
    }

    fun multiMatrix(matrix44: Matrix44) {
        OpenGL.glPushMatrix()
        OpenGL.glMultMatrixf(matrix44.elements, 0)
    }

    fun updatePointSizeRange(red: Float, green: Float, blue: Float, alpha: Float) {
        BUFFER_4F[0] = red
        BUFFER_4F[1] = green
        BUFFER_4F[2] = blue
        BUFFER_4F[3] = alpha
        OpenGL.glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, BUFFER_4F, 0)
    }

    fun renderLights() {
        var lightCount: Int
        lightCount = 0
        while (lightCount < this.lastSceneLightCount) {
            val activeLight = this.lightNodes[lightCount]!!
            val lightHandle = lightCount + 16386
            LIGHT_MATRIX[0] = activeLight.getX().toFloat()
            LIGHT_MATRIX[1] = activeLight.getY().toFloat()
            LIGHT_MATRIX[2] = activeLight.getZ().toFloat()
            LIGHT_MATRIX[3] = 1.0f
            OpenGL.glLightfv(lightHandle, GL_POSITION, LIGHT_MATRIX, 0)
            val color = activeLight.getColor()
            val alphaMultiplier = activeLight.getAlpha() / 255.0f
            LIGHT_MATRIX[0] = (color shr 16 and 0xff) * alphaMultiplier
            LIGHT_MATRIX[1] = (color shr 8 and 0xff) * alphaMultiplier
            LIGHT_MATRIX[2] = (color and 0xff) * alphaMultiplier
            OpenGL.glLightfv(lightHandle, GL_DIFFUSE, LIGHT_MATRIX, 0)
            OpenGL.glLightf(lightHandle, GL_QUADRATIC_ATTENUATION, 1.0f / (activeLight.getSize2d() * activeLight.getSize2d()))
            OpenGL.glEnable(lightHandle)
            lightCount++
        }
        while (lightCount < this.activeLightCount) {
            OpenGL.glDisable(lightCount + GL_LIGHT2)
            lightCount++
        }
        this.activeLightCount = this.lastSceneLightCount
    }

    fun resetOrthoAndDisplay() {
        if (display == null) {
            return
        }
        val displayWidth = display!!.getWidth()
        val displayHeight = display!!.getHeight()
        this.orthoProjectionMatrix.setOrtho(0.0f, displayWidth.toFloat(), 0.0f, displayHeight.toFloat(), -1.0f, 1.0f)
        calculateViewportCenter()
        setOrthoMatrixMode()
        resetClipping()
    }

    fun setViewportOffset(offsetX: Int, offsetY: Int) {
        this.viewportOffsetX = offsetX
        this.viewportOffsetY = offsetY
        updateViewportAndCenter()
        defineScissorBox()
    }

    fun popMatrix() {
        OpenGL.glPopMatrix()
    }

    fun toggleFog(enableFog: Boolean) {
        if (this.fogEnabled != enableFog) {
            this.fogEnabled = enableFog
            toggleFog()
            this.functionMask = this.functionMask and -16
        }
    }

    override fun generateFlags() {
        var meme = 32
        this.shadowScale = 0
        while (meme > 1) {
            meme = meme shr 1
            this.shadowScale++
        }
        this.openglFlags = 1 shl this.shadowScale
    }

    fun setModelViewMatrixMode() {
        if (this.depthClippingMode != 2) {
            this.depthClippingMode = 2
            invertMatrix(this.currentModelMatrix.elements)
            updateModelViewAndLights()
            updateViewportAndCenter()
            updateDepthRange()
            this.functionMask = this.functionMask and -8
        }
    }

    override fun debugModeChanged() {
    }

    fun toggleDrawingClientstates(vertices: OpenGlArrayDrawBuffer?, normals: OpenGlArrayDrawBuffer?, colors: OpenGlArrayDrawBuffer?, textureCoordinates: OpenGlArrayDrawBuffer?) {
        val verticesBuffer = vertices?.buffer
        if (verticesBuffer != null) {
            setArrayBuffer(verticesBuffer)
            OpenGL.glVertexPointer(vertices.size.toInt(), vertices.type.toInt(), this.currentArrayBuffer!!.getStride(), this.currentArrayBuffer!!.getBufferAddress() + vertices.offset)
            OpenGL.glEnableClientState(GL_VERTEX_ARRAY)
        } else {
            OpenGL.glDisableClientState(GL_VERTEX_ARRAY)
        }

        val normalsBuffer = normals?.buffer
        if (normalsBuffer != null) {
            setArrayBuffer(normalsBuffer)
            OpenGL.glNormalPointer(normals.type.toInt(), this.currentArrayBuffer!!.getStride(), this.currentArrayBuffer!!.getBufferAddress() + normals.offset)
            OpenGL.glEnableClientState(GL_NORMAL_ARRAY)
        } else {
            OpenGL.glDisableClientState(GL_NORMAL_ARRAY)
        }

        val colorsBuffer = colors?.buffer
        if (colorsBuffer != null) {
            setArrayBuffer(colorsBuffer)
            OpenGL.glColorPointer(colors.size.toInt(), colors.type.toInt(), this.currentArrayBuffer!!.getStride(), this.currentArrayBuffer!!.getBufferAddress() + colors.offset)
            OpenGL.glEnableClientState(GL_COLOR_ARRAY)
        } else {
            OpenGL.glDisableClientState(GL_COLOR_ARRAY)
        }

        val textureCoordinatesBuffer = textureCoordinates?.buffer
        if (textureCoordinatesBuffer != null) {
            setArrayBuffer(textureCoordinatesBuffer)
            OpenGL.glTexCoordPointer(textureCoordinates.size.toInt(), textureCoordinates.type.toInt(), this.currentArrayBuffer!!.getStride(), this.currentArrayBuffer!!.getBufferAddress() + textureCoordinates.offset)
            OpenGL.glEnableClientState(GL_TEXTURE_COORD_ARRAY)
        } else {
            OpenGL.glDisableClientState(GL_TEXTURE_COORD_ARRAY)
        }
    }

    override fun drawTransparentHorizontalLine(x: Int, y: Int, width: Int, colorArgb: Int, alpha: Int) {
        setupShapeRendering()
        setBlendMode(alpha)
        val lineX = x + 0.35f
        val lineY = y + 0.35f
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        OpenGL.glBegin(1)
        OpenGL.glVertex2f(lineX, lineY)
        OpenGL.glVertex2f(lineX + width, lineY)
        OpenGL.glEnd()
    }

    override fun resetDepthClippingMask() {
        this.depthClippingNear = 0.0f
        this.depthClippingFar = 1.0f
        updateDepthRange()
    }

    override fun drawTransparentVerticalLine(x: Int, y: Int, height: Int, colorArgb: Int, blendMode: Int) {
        setupShapeRendering()
        setBlendMode(blendMode)
        val lineX = x + 0.35f
        val lineY = y + 0.35f
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        OpenGL.glBegin(1)
        OpenGL.glVertex2f(lineX, lineY)
        OpenGL.glVertex2f(lineX, lineY + height)
        OpenGL.glEnd()
    }

    fun accumulateCapabilities(): Int {
        var attributes = 0
        this.gpuCompanyString = OpenGL.glGetString(GL_VENDOR).lowercase()
        this.gpuNameString = OpenGL.glGetString(GL_RENDERER).lowercase()
        if (this.gpuCompanyString!!.indexOf("microsoft") != -1) {
            attributes = attributes or 0x1
        }
        if (this.gpuCompanyString!!.indexOf("brian paul") != -1 || this.gpuCompanyString!!.indexOf("mesa") != -1) {
            attributes = attributes or 0x1
        }
        val version = OpenGL.glGetString(GL_VERSION)
        val versionStrings = Utils.split(version.replace('.', ' '), ' ')
        if (versionStrings.size >= 2) {
            try {
                val majorVersion = Utils.parseInt(versionStrings[0])
                val minorVersion = Utils.parseInt(versionStrings[1])
                this.openglVersion = majorVersion * 10 + minorVersion
            } catch (numberformatexception: NumberFormatException) {
                attributes = attributes or 0x4
            }
        } else {
            attributes = attributes or 0x4
        }
        if (this.openglVersion < 12) {
            attributes = attributes or 0x2
        }
        if (!this.activeOpenglInstance!!.supportsExtension("GL_ARB_multitexture")) {
            attributes = attributes or 0x8
        }
        if (!this.activeOpenglInstance!!.supportsExtension("GL_ARB_texture_env_combine")) {
            attributes = attributes or 0x20
        }
        val textureInfo = IntArray(1)
        OpenGL.glGetIntegerv(GL_MAX_TEXTURE_UNITS, textureInfo, 0)
        this.maxTextureUnits = textureInfo[0]
        OpenGL.glGetIntegerv(GL_MAX_TEXTURE_COORDS, textureInfo, 0)
        this.maxTextureCoordinates = textureInfo[0]
        OpenGL.glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, textureInfo, 0)
        this.maxTextureImageUnits = textureInfo[0]
        if (this.maxTextureUnits < 2 || this.maxTextureCoordinates < 2 || this.maxTextureImageUnits < 2) {
            attributes = attributes or 0x10
        }
        this.bigEndianess = Stream.bigEndianessEnabled()
        this.vertexbufferSupported = this.activeOpenglInstance!!.supportsExtension("GL_ARB_vertex_buffer_object")
        this.arbMultisample = this.activeOpenglInstance!!.supportsExtension("GL_ARB_multisample")
        this.arbVertexProgram = this.activeOpenglInstance!!.supportsExtension("GL_ARB_vertex_program")
        this.activeOpenglInstance!!.supportsExtension("GL_ARB_fragment_program")
        this.arbVertexShader = this.activeOpenglInstance!!.supportsExtension("GL_ARB_vertex_shader")
        this.arbFragmentShader = this.activeOpenglInstance!!.supportsExtension("GL_ARB_fragment_shader")
        this.texture3dSupported = this.activeOpenglInstance!!.supportsExtension("GL_EXT_texture3D")
        this.arbRectangleTexturesSupported = this.activeOpenglInstance!!.supportsExtension("GL_ARB_texture_rectangle")
        this.arbTextureCubeMap = this.activeOpenglInstance!!.supportsExtension("GL_ARB_texture_cube_map")
        this.arbTextureFloat = this.activeOpenglInstance!!.supportsExtension("GL_ARB_texture_float")
        this.framebufferObjectSupported = false
        this.intelGpu = this.activeOpenglInstance!!.supportsExtension("GL_EXT_framebuffer_object")
        this.framebufferBlit = this.activeOpenglInstance!!.supportsExtension("GL_EXT_framebuffer_blit")
        this.framebufferMultisample = this.activeOpenglInstance!!.supportsExtension("GL_EXT_framebuffer_multisample")
        this.advancedFramebuffers = this.framebufferBlit and this.framebufferMultisample
        this.appleOperatingSystem = OperatingSystemInfo.OS_NAME.startsWith("mac")
        OpenGL.glGetFloatv(GL_POINT_SIZE_RANGE, BUFFER_4F, 0)
        this.fillCirclePointSize = BUFFER_4F[0]
        this.outlineCirclePointSize = BUFFER_4F[1]
        return attributes
    }

    fun setActiveTexture(replacingCombiner: OpenGlTexture?) {
        val currentCombiner = this.openglTextures!![this.glTexturesCursorPos]
        if (currentCombiner !== replacingCombiner) {
            if (replacingCombiner != null) {
                if (currentCombiner != null) {
                    if (currentCombiner.textureTarget != replacingCombiner.textureTarget) {
                        OpenGL.glDisable(currentCombiner.textureTarget)
                        OpenGL.glEnable(replacingCombiner.textureTarget)
                    }
                } else {
                    OpenGL.glEnable(replacingCombiner.textureTarget)
                }
                OpenGL.glBindTexture(replacingCombiner.textureTarget, replacingCombiner.activeHandle)
            } else {
                OpenGL.glDisable(currentCombiner!!.textureTarget)
            }
            this.openglTextures!![this.glTexturesCursorPos] = replacingCombiner
        }
        this.functionMask = this.functionMask and -2
    }

    fun toggleZbuffer(zbuffer: Boolean) {
        if (this.depthBuffering != zbuffer) {
            if (zbuffer) {
                OpenGL.glEnable(GL_DEPTH_TEST)
            } else {
                OpenGL.glDisable(GL_DEPTH_TEST)
            }
            this.depthBuffering = zbuffer
            this.functionMask = this.functionMask and -16
        }
    }

    fun setElementsBuffer(elementsBuffer: ElementsBufferGL) {
        if (elementsBuffer !== this.currentElementsBuffer) {
            if (this.vertexbufferSupported) {
                OpenGL.glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER, elementsBuffer.getId())
            }
            this.currentElementsBuffer = elementsBuffer
        }
    }

    override fun createGround(regionSizeX: Int, regionSizeY: Int, heightsCurrentLvl: Array<IntArray>, heightsNextLvl: Array<IntArray>, floorFunctionMask: Int, waterFunctionMask: Int): Ground {
        return GroundGL(this, floorFunctionMask, waterFunctionMask, regionSizeX, regionSizeY, heightsCurrentLvl, heightsNextLvl)
    }

    override fun createDelegatingDisplay(canvas: Canvas, i_2: Int, i_3: Int): DelegatingDisplay {
        return ImageProducerGL2(this, canvas)
    }

    fun updateDepthRange() {
        if (this.depthClippingMode == 2) {
            OpenGL.glDepthRange(this.depthClippingNear, this.depthClippingFar)
        } else {
            OpenGL.glDepthRange(0.0f, 1.0f)
        }
    }

    override fun getRenderInfo(): RenderInfo {
        var gpuVendorIdentifier = -1
        if (this.gpuCompanyString!!.indexOf("nvidia") != -1) {
            gpuVendorIdentifier = 4318
        } else if (this.gpuCompanyString!!.indexOf("intel") != -1) {
            gpuVendorIdentifier = 32902
        } else if (this.gpuCompanyString!!.indexOf("ati") != -1) {
            gpuVendorIdentifier = 4098
        }
        return RenderInfo(gpuVendorIdentifier, "OpenGL", this.openglVersion, this.gpuNameString!!, 0L)
    }

    fun updateSunColor() {
        BUFFER_4F[0] = this.sunBrightness * this.sunColorRed
        BUFFER_4F[1] = this.sunBrightness * this.sunColorGreen
        BUFFER_4F[2] = this.sunBrightness * this.sunColorBlue
        BUFFER_4F[3] = 1.0f
        OpenGL.glLightModelfv(GL_LIGHT_MODEL_AMBIENT, BUFFER_4F, 0)
    }

    override fun renderWorldmapTile(startX: Int, startY: Int, width: Int, height: Int, colorArgb: Int, textureEnvColor: Int, pixelData: ByteArray, i_8: Int, textureBlendColor: Int) {
        val textureCoordV: Float
        val textureCoordU: Float
        if (this.worldMapTexture != null && this.worldMapTexture!!.width >= width && this.worldMapTexture!!.length >= height) {
            this.worldMapTexture!!.uploadTextureData(0, width, height, pixelData, GLFormat.RGBA, false)
            textureCoordV = this.worldMapTexture!!.t * height / this.worldMapTexture!!.length
            textureCoordU = this.worldMapTexture!!.s * width / this.worldMapTexture!!.width
        } else {
            this.worldMapTexture = OpenGl2DTexture_Sub1.createSurface(this, GLFormat.RGBA, GLActionFormat.PIXEL_FORMAT, width, height, pixelData, GLFormat.RGBA)
            this.worldMapTexture!!.setRepeat(false, false)
            textureCoordV = this.worldMapTexture!!.t
            textureCoordU = this.worldMapTexture!!.s
        }
        setupSpriteRendering()
        setActiveTexture(this.worldMapTexture)
        setBlendMode(textureBlendColor)
        OpenGL.glColor4ub((colorArgb shr 16).toByte(), (colorArgb shr 8).toByte(), colorArgb.toByte(), (colorArgb shr 24).toByte())
        setTextureEnvColor(textureEnvColor)
        loadTextureCombineMode(GL_INTERPOLATE, GL_INTERPOLATE)
        setTexture(0, GL_CONSTANT, GL_SRC_COLOR)
        setTexture(2, GL_TEXTURE, GL_SRC_ALPHA)
        setTextureTransparency(0, GL_CONSTANT)
        setTextureTransparency(2, GL_TEXTURE)
        val xA = startX.toFloat()
        val yA = startY.toFloat()
        val xB = xA + width
        val yB = yA + height
        OpenGL.glBegin(7)
        OpenGL.glTexCoord2f(0.0f, 0.0f)
        OpenGL.glVertex2f(xA, yA)
        OpenGL.glTexCoord2f(0.0f, textureCoordU)
        OpenGL.glVertex2f(xA, yB)
        OpenGL.glTexCoord2f(textureCoordV, textureCoordU)
        OpenGL.glVertex2f(xB, yB)
        OpenGL.glTexCoord2f(textureCoordV, 0.0f)
        OpenGL.glVertex2f(xB, yA)
        OpenGL.glEnd()
        setTexture(0, GL_TEXTURE, GL_SRC_COLOR)
        setTexture(2, GL_CONSTANT, GL_SRC_ALPHA)
        setTextureTransparency(0, GL_TEXTURE)
        setTextureTransparency(2, GL_CONSTANT)
    }

    override fun copyLights(lightCount: Int, lights: Array<PointLight>?) {
        if (lightCount > 0 && lights != null) System.arraycopy(lights, 0, this.lightNodes, 0, lightCount)
        this.lastSceneLightCount = lightCount
        if (this.depthClippingMode != 1) {
            renderLights()
        }
    }

    fun stillwaterEffectLoaded(): Boolean {
        return this.effectManager!!.stillwaterEffectLoaded()
    }

    @Synchronized
    fun requestRenderBufferCleanup(handle: Int, bufferSize: Int) {
        val handleAwaitingRemoval = IntNode(bufferSize)
        handleAwaitingRemoval.pointer = handle.toLong()
        this.renderBuffersAwaitingRemoval.insertTail(handleAwaitingRemoval)
    }

    override fun setSunCubeTexture(cubeTextureGenerator: AbstractCubeTextureGenerator?) {
        this.sunCubeTextureGenerator = cubeTextureGenerator as AbstractOpenGlCubeTextureGenerator?
    }

    fun loadTextureCombineMode(combineRgb: Int, combineAlpha: Int) {
        if (this.glTexturesCursorPos == 0) {
            var hasCombining = false
            if (this.textureCombineRgb != combineRgb) {
                OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, combineRgb)
                this.textureCombineRgb = combineRgb
                hasCombining = true
            }
            if (combineAlpha != this.textureCombineAlpha) {
                OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, combineAlpha)
                this.textureCombineAlpha = combineAlpha
                hasCombining = true
            }
            if (hasCombining) {
                this.functionMask = this.functionMask and -14
            }
        } else {
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, combineRgb)
            OpenGL.glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, combineAlpha)
        }
    }

    fun invertMatrix(matrix: FloatArray) {
        val invertedMatrix = FloatArray(16)
        System.arraycopy(matrix, 0, invertedMatrix, 0, 16)
        invertedMatrix[1] = -invertedMatrix[1]
        invertedMatrix[5] = -invertedMatrix[5]
        invertedMatrix[9] = -invertedMatrix[9]
        invertedMatrix[13] = -invertedMatrix[13]
        OpenGL.glMatrixMode(GL_PROJECTION)
        OpenGL.glLoadMatrixf(invertedMatrix, 0)
        OpenGL.glMatrixMode(GL_MODELVIEW)
    }

    override fun hasTextures(): Boolean = true

    override fun getMaxLights(): Int = 4

    override fun setModelMatrix(matrix441: Matrix44) {
        this.currentModelMatrix.copyFrom(matrix441)
        updateViewProjectionMatrix()
        recalculateFogAndProjection()
    }

    override fun hasAntiAliasing(): Boolean {
        return this.arbMultisample && (!bloomEnabled() || this.advancedFramebuffers)
    }

    fun toggleFog() {
        if (this.fogEnabled && this.fogDensity >= 0) {
            OpenGL.glEnable(GL_FOG)
        } else {
            OpenGL.glDisable(GL_FOG)
        }
    }

    fun updateModelViewAndLights() {
        OpenGL.glLoadIdentity()
        OpenGL.glMultMatrixf(this.viewMatrix44.elements, 0)
        if (this.renderStillWater) {
            this.effectManager!!.waterEffect.enable()
        }
        setLightingSourceParams()
        renderLights()
    }

    override fun mergeFunctionMasks(base: Int, mask: Int): Int = base or mask

    companion object {
        @JvmField val HANDLE_BUFFER = IntArray(1000)
        const val INITIAL_LIGHT_COUNT = 4
        @JvmField val BUFFER_4F = FloatArray(4)
        @JvmField val LIGHT_MATRIX = FloatArray(4)

        @JvmField val SINE = FloatArray(16384)
        @JvmField val COSINE = FloatArray(16384)

        init {
            val angle = 3.834951969714103E-4
            for (index in 0 until 16384) {
                SINE[index] = sin(index * angle).toFloat()
                COSINE[index] = cos(index * angle).toFloat()
            }
        }

        @JvmStatic
        fun createToolkitHd(gameCanvas: Canvas?, materialProvider: MaterialProvider?, resourceProvider: Js5ResourceProvider?, multiSamples: Int): AbstractToolkit {
            try {
                val jaclibLoaded = isJaclibLoaded()
                if (!jaclibLoaded) {
                    throw RuntimeException("")
                }
                if (!getNativeLibraryLoader().loadIntoMemory("jaggl")) {
                    throw RuntimeException("")
                }
                setIgnoreRepaint(gameCanvas!!)
                val opengl = OpenGL()
                val createdUid = opengl.init(gameCanvas, 8, 8, 8, 24, 0, multiSamples)
                if (createdUid == 0L) {
                    throw RuntimeException("")
                }
                val toolkit = OpenGLNativeToolkit(opengl, gameCanvas, createdUid, materialProvider!!, resourceProvider!!, multiSamples)
                toolkit.initializeTables()
                return toolkit
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
                throw RuntimeException("")
            }
        }

        @JvmStatic
        fun createToolkitLd(canvas: Canvas?, materialProvider: MaterialProvider?, multiSamples: Int): AbstractToolkit {
            return OpenGlToolkit(canvas!!, materialProvider!!, multiSamples)
        }

        @JvmStatic
        fun format(pixelFormat: GLFormat): Int {
            return when (pixelFormat.formatType) {
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
        fun getInternalTextureFormat(internalFormat: GLFormat, actionFormat: GLActionFormat): Int {
            return if (actionFormat === GLActionFormat.PIXEL_FORMAT) {
                when (internalFormat.formatType) {
                    0 -> GL_LUMINANCE
                    1 -> GL_ALPHA
                    4 -> GL_LUMINANCE_ALPHA
                    5 -> GL_RGB
                    6 -> GL_RGBA
                    else -> throw IllegalArgumentException()
                }
            } else if (actionFormat === GLActionFormat.TEXTURE_FORMAT) {
                when (internalFormat.formatType) {
                    0 -> GL_LUMINANCE16
                    1 -> GL_ALPHA16
                    4 -> GL_LUMINANCE_ALPHA16UI_EXT
                    5 -> GL_RGB16
                    6 -> GL_RGBA16
                    8 -> GL_DEPTH_COMPONENT16
                    else -> throw IllegalArgumentException()
                }
            } else if (actionFormat === GLActionFormat.DEPTH_COMPONENT_24) {
                when (internalFormat.formatType) {
                    8 -> GL_DEPTH_COMPONENT24
                    else -> throw IllegalArgumentException()
                }
            } else if (actionFormat === GLActionFormat.FLOAT_16_ARB) {
                when (internalFormat.formatType) {
                    0 -> GL_LUMINANCE16F_ARB
                    1 -> GL_ALPHA16F_ARB
                    4 -> GL_LUMINANCE_ALPHA16F_ARB
                    5 -> GL_RGB16F_ARB
                    6 -> GL_RGBA16F_ARB
                    else -> throw IllegalArgumentException()
                }
            } else if (actionFormat === GLActionFormat.FLOAT_32_ARB) {
                when (internalFormat.formatType) {
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
    }
}
