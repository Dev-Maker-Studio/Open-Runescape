package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.entity.pathingentity.PoseMemo
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.scene.culling.Culling
import com.jagex.game.runetek5.scene.ground.GroundFunctionMask
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialTypeList
import com.jagex.game.runetek5.graphics.modern.dump.FrameDump
import com.jagex.game.runetek5.graphics.modern.debug.AutoLogin
import com.jagex.game.runetek5.graphics.modern.debug.GeometryDiff
import com.jagex.game.runetek5.graphics.modern.batch.FrameBatcher
import com.jagex.game.runetek5.graphics.modern.batch.InstancedModelBatcher
import com.jagex.game.runetek5.graphics.modern.batch.ModelCache
import com.jagex.game.runetek5.graphics.modern.batch.OptimizedFrameBatcher
import org.lwjgl.system.MemoryUtil
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.graphics.modern.renderer.ModernFontRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernGroundRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.HdWaterRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernModelRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernBillboardRenderer
import org.lwjgl.opengl.GL30
import com.jagex.game.runetek5.graphics.modern.framebuffer.MultisampleFramebuffer
import com.jagex.game.runetek5.graphics.modern.core.BufferStaging
import com.jagex.game.runetek5.graphics.modern.renderer.ModernParticleRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernSpriteRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernSurfaceMaskRenderer
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.sprite.ModernSpriteGL
import java.util.IdentityHashMap
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteGL
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.GpuTimer
import com.jagex.game.runetek5.graphics.modern.util.MeshConverter
import com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder
import com.jagex.game.runetek5.graphics.modern.material.TextureScroll
import com.jagex.game.runetek5.graphics.modern.scene.RetainedStaticScene
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.graphics.model.PoseMemoOwner
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.graphics.texture.RectangularTextureGL
import com.jagex.game.runetek5.graphics.modern.postprocess.ModernBloomPass
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.imageproducer.ImageProducerGL
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.opengl.GroundMaterialGL
import jaggl.OpenGL
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.KHRDebug
import java.nio.IntBuffer
import org.lwjgl.opengl.GL33.*
import java.awt.Canvas

class ModernGLToolkit(
    canvas: Canvas,
    materials: MaterialProvider,
    multiSamples: Int
) : OpenGlToolkit(canvas, materials, multiSamples) {

    private var modernRenderer: ModernModelRenderer? = null
    private var groundRenderer: ModernGroundRenderer? = null

    fun groundRendererForShadows(): ModernGroundRenderer? = groundRenderer
    private var spriteRenderer: ModernSpriteRenderer? = null
    private var surfaceMaskRenderer: ModernSurfaceMaskRenderer? = null
    private var particleRendererModern: ModernParticleRenderer? = null
    private var billboardRendererModern: ModernBillboardRenderer? = null

    private var bbCount = 0
    private var bbEye = FloatArray(64 * 3)
    private var bbHalfW = IntArray(64)
    private var bbHalfH = IntArray(64)
    private var bbRotation = IntArray(64)
    private var bbArgb = IntArray(64)
    private var bbMaterial = IntArray(64)
    private var bbBlend = IntArray(64)
    private var bbDepthWrite = BooleanArray(64)
    private var bbKey = IntArray(64)
    private var bbSeq = IntArray(64)
    private var bbOrder = IntArray(64)

    private fun deferBillboards(model: ModelGL, modelMatrix: Matrix43, depthKey: Int) {
        val effects = model.billboardEffects ?: return
        val surfaces = model.billboardSurfaces ?: return
        val vx = model.verticesX ?: return
        val vy = model.verticesY ?: return
        val vz = model.verticesZ ?: return
        val v = viewMatrix44.elements
        for (b in 0 until model.billboardCount) {
            val effect = effects[b] ?: continue
            val surface = surfaces[b] ?: continue
            val cx = (vx[effect.faceA] + vx[effect.faceB] + vx[effect.faceC]) * 0.3333333f
            val cy = (vy[effect.faceA] + vy[effect.faceB] + vy[effect.faceC]) * 0.3333333f
            val cz = (vz[effect.faceA] + vz[effect.faceB] + vz[effect.faceC]) * 0.3333333f
            val wx = modelMatrix.xA * cx + modelMatrix.xB * cy + modelMatrix.xC * cz + modelMatrix.xD
            val wy = modelMatrix.yA * cx + modelMatrix.yB * cy + modelMatrix.yC * cz + modelMatrix.yD
            val wz = modelMatrix.zA * cx + modelMatrix.zB * cy + modelMatrix.zC * cz + modelMatrix.zD
            val ex = v[0] * wx + v[4] * wy + v[8] * wz + v[12]
            val ey = v[1] * wx + v[5] * wy + v[9] * wz + v[13]
            val ez = v[2] * wx + v[6] * wy + v[10] * wz + v[14]
            val len = Math.sqrt((ex * ex + ey * ey + ez * ez).toDouble()).toFloat()
            val distanceScale = if (len > 0f) effect.distance / len else 0f
            if (bbCount == bbHalfW.size) {
                val n = bbCount * 2
                bbEye = bbEye.copyOf(n * 3); bbHalfW = bbHalfW.copyOf(n); bbHalfH = bbHalfH.copyOf(n)
                bbRotation = bbRotation.copyOf(n); bbArgb = bbArgb.copyOf(n); bbMaterial = bbMaterial.copyOf(n)
                bbBlend = bbBlend.copyOf(n); bbDepthWrite = bbDepthWrite.copyOf(n); bbKey = bbKey.copyOf(n)
                bbSeq = bbSeq.copyOf(n); bbOrder = bbOrder.copyOf(n)
            }
            val i = bbCount++
            bbEye[i * 3] = ex + surface.offsetX - ex * distanceScale
            bbEye[i * 3 + 1] = ey + surface.offsetY - ey * distanceScale
            bbEye[i * 3 + 2] = ez - ez * distanceScale
            bbHalfW[i] = (surface.scaleX * effect.sizeX) shr 7
            bbHalfH[i] = (surface.scaleY * effect.sizeY) shr 7
            bbRotation[i] = surface.rotation
            bbArgb[i] = surface.diffuseColor
            bbMaterial[i] = effect.materialId.toInt()
            bbBlend[i] = effect.shape.toInt()
            bbDepthWrite[i] = !model.hasAlphaTransition
            bbKey[i] = depthKey
            bbSeq[i] = translucentSeqCounter++
            bbDeferredTotal++
            if (BB_DIAG && bbDiagBudget > 0) {
                bbDiagBudget--
                System.err.println("[BillboardDiag] model=${model.hashCode()} eye=(${bbEye[i * 3]},${bbEye[i * 3 + 1]},${bbEye[i * 3 + 2]}) world=($wx,$wy,$wz) half=(${bbHalfW[i]},${bbHalfH[i]}) rot=${bbRotation[i]} argb=${Integer.toHexString(bbArgb[i])} mat=${bbMaterial[i]} tex=${if (bbMaterial[i] >= 0) lookupTextureHandle(bbMaterial[i]) else 0} blend=${bbBlend[i]} depthWrite=${bbDepthWrite[i]} dist=${effect.distance} size=(${effect.sizeX},${effect.sizeY}) scale=(${surface.scaleX},${surface.scaleY}) key=$depthKey")
            }
        }
    }

    private var bbDeferredTotal = 0L
    private var perfFrameNsMax = 0L
    private var deferredBuildNsAcc = 0L
    private var stallPrevRedraw = 0L; private var stallPrevCanvas = 0L; private var stallPrevDealloc = 0L
    private var stallPrevGpuWait = 0L; private var stallPrevStaged = 0L; private var stallPrevDeferred = 0L
    private var stallPrevWalk = 0L; private var stallPrevDraw = 0L; private var stallPrevFloor = 0L; private var stallPrevAlpha = 0L
    private var stallPrevRetained = 0L; private var stallPrevScene = 0L; private var stallPrevRender = 0L; private var stallPrevSwap = 0L
    private var stallPrevBuilds = 0L; private var stallPrevCaptures = 0L
    private var fbSamplesQueried = -1
    private var fbSampleBuffers = -1
    private var lightDiagBudget = 3
    private var lightDiagInstBudget = 12
    private var bbDrawnTotal = 0L
    private var bbDiagBudget = 12

    fun submitRetainedBillboards(model: ModelGL, legacyMatrix: Matrix43, depthKey: Int) {
        deferBillboards(model, legacyMatrix, depthKey)
    }

    private fun drawBillboardNow(i: Int) {
        val br = billboardRendererModern ?: return
        bbDrawnTotal++
        br.drawOne(
            bbEye[i * 3], bbEye[i * 3 + 1], bbEye[i * 3 + 2],
            bbHalfW[i], bbHalfH[i], bbRotation[i], bbArgb[i],
            if (bbMaterial[i] >= 0) lookupTextureHandle(bbMaterial[i]) else 0,
            bbBlend[i], bbDepthWrite[i]
        )
    }
    private var frameBatcher: FrameBatcher? = null
    private var optimizedBatcher: OptimizedFrameBatcher? = null
    private var instancedBatcher: InstancedModelBatcher? = null
    private var useOptimizedBatcher = true
    private var useInstancing = true

    private var isModernRendererInitialized = false

    private val ownerThreadId: Long = Thread.currentThread().id
    @Volatile private var ownerThreadHasCapabilities = false

    private var diagSetModelMatrixCalls = 0L
    private var diagSetModelMatrixRejected = 0L
    private var diagBatchOpens = 0L
    private var diagLazyBatchOpens = 0L
    private var diagModelGateCalls = 0L
    private var diagModelGateAccepted = 0L
    private var diagGroundCalls = 0L
    private var diagGroundNoPipeline = 0L
    private var diagGroundNoSnapshot = 0L
    private var diagGroundDrawn = 0L
    @Volatile private var lastPipelineRejectReason = "none"

    private val legacySkipCounters = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun countLegacySkip(subsystem: String) {
        legacySkipCounters.merge(subsystem, 1L, Long::plus)
    }

    private fun canUseModernPipeline(): Boolean {
        if (!isModernRendererInitialized || !ownerThreadHasCapabilities ||
            Thread.currentThread().id != ownerThreadId) {
            lastPipelineRejectReason = when {
                !isModernRendererInitialized -> "notInitialized"
                !ownerThreadHasCapabilities -> "noCaps"
                else -> "wrongThread"
            }
            return false
        }

        lastPipelineRejectReason = "none"
        return true
    }

    private var isSpriteBatchActive = false

    private var groundIndexScratch = ShortArray(0)
    private var groundIndexUpload: java.nio.ByteBuffer? = null

    private val GROUND_PROBE = System.getProperty("render.sunprobe") != null
    private var groundProbeTick = 0
    private val GROUND_COLOR_DEBUG = System.getProperty("render.groundcolor")?.let { it.equals("true", true) || it.equals("raw", true) } == true
    private val GROUND_COMBINE_OVERRIDE = System.getProperty("render.groundcombine")?.toIntOrNull() ?: -1

    private val textureHandleCache = mutableMapOf<Int, Int>()

    private var modernBuiltPathModels = 0L
    private var modernBuiltFallbackModels = 0L

    private var diagBudget = 120
    private val diagGreenSeen = HashSet<Int>()
    private val diagModelSeen = HashSet<Long>()
    private val diagDropSeen = HashSet<Int>()

    private var translucentSeqCounter = 0

    private val deferredParticles = ArrayList<ParticleWrapper>()
    private var deferredParticleSeq = IntArray(64)
    private var deferredParticleKey = IntArray(64)
    private var deferredParticleOrder = IntArray(64)

    private val BB_DIAG = System.getProperty("render.bbdiag") != null
    private val LIGHT_DIAG = System.getProperty("render.lightdiag") != null
    private val STALL_DIAG_MS = System.getProperty("render.stalldiag")?.toLongOrNull() ?: 0L
    private val PARALLEL_BUILD = System.getProperty("render.parallelbuild")?.equals("false", true) != true
    private val TRANS_DIAG = System.getProperty("render.transdiag") != null
    private val FORCE_STREAM_TRANSLUCENT = System.getProperty("render.forcestreamtrans") != null
    private var transDiagTick = 0
    private var transDiagInst = 0
    private var transDiagStream = 0
    private var transDiagMinKey = Int.MAX_VALUE
    private var transDiagMaxKey = Int.MIN_VALUE

    private var retainedScene: RetainedStaticScene? = null

    private var captureList: ArrayList<RetainedStaticScene.Capture>? = null
    private var captureUnsupported = false
    private var captureLegacyMatrix: Matrix43? = null

    private val legacyCaptureDroppedFaces = java.util.concurrent.atomic.AtomicLong()
    private val legacyCaptureModelsWithDrops = java.util.concurrent.atomic.AtomicLong()
    private val legacyCaptureModelsFullyDropped = java.util.concurrent.atomic.AtomicLong()

    private var streamingModelsDropped = 0L

    private val modernYawScratch = Matrix43()

    private fun composeModernModelMatrix(model: ModelGL, modelMatrix: Matrix43): Matrix43 {
        val yaw = if (modernYawEnabled) model.modernYaw and 0x3fff else 0
        if (yaw == 0) return modelMatrix
        val sin = MathUtils.SINE[yaw] / 16384.0f
        val cos = MathUtils.COSINE[yaw] / 16384.0f
        val m = modernYawScratch
        m.xA = cos; m.yA = 0f; m.zA = -sin
        m.xB = 0f; m.yB = 1f; m.zB = 0f
        m.xC = sin; m.yC = 0f; m.zC = cos
        m.xD = 0f; m.yD = 0f; m.zD = 0f
        m.multiply(modelMatrix)
        return m
    }

    var modelCache: ModelCache? = null
        private set

    private val modernProjectionMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val modelMat = Matrix4f()

    private var camX = 0f
    private var camY = 0f
    private var camZ = 0f
    private var camYaw = 0f
    private var camPitch = 0f

    private var frameCounter = 0
    private var modelCounter = 0
    private var totalModelsProcessed = 0L
    private var totalFacesProcessed = 0L
    private var lastBatcherStatus = "not checked"

    companion object {
        private const val DEFAULT_OVERLAY_WATER_COLOUR = 1190717
        private const val HD_WATER_FLOW = 16f
        private const val REFLECTION_CLIP_BIAS = 4f
        private var streamKeyCounter = 0x4000_0000_0000_0000L

        private fun flag(name: String) =
            System.getProperty("render.$name")?.equals("false", true) != true

        @JvmStatic var frustumCullEnabled = flag("cull")

        @JvmStatic var instancingEnabled = flag("instancing")

        @JvmStatic var meshBuiltPathEnabled =
            System.getProperty("render.meshbuilt")?.equals("false", true) != true

        @JvmStatic var alphaDiscardEnabled = flag("alphatest")

        @JvmStatic var modernYawEnabled = flag("yaw")
        @JvmStatic val cullFaceEnabled = flag("cullface")
        @JvmStatic val pointLightsEnabled = flag("pointlights")
        @JvmStatic val TEX_ALPHA_DIAG = System.getProperty("render.texalphadiag") != null
        @JvmStatic var slotColorEnabled = flag("slotcolor")

        @JvmStatic val modelBlend = System.getProperty("render.modelblend")?.equals("false", true) != true

        @JvmStatic val noLight = System.getProperty("render.nolight")?.equals("true", true) == true

        @JvmStatic var verifyLiveGeometry =
            System.getProperty("render.verifylive")?.equals("true", true) == true

        @JvmStatic var meshKeyByCurrentVertices =
            System.getProperty("render.meshkey")?.equals("hash", true) == true

        @JvmStatic
        fun createToolkit(
            canvas: Canvas?,
            materials: MaterialProvider?,
            multiSamples: Int
        ): ModernGLToolkit {
            requireNotNull(canvas) { "Canvas cannot be null" }
            requireNotNull(materials) { "MaterialProvider cannot be null" }
            return ModernGLToolkit(canvas, materials, multiSamples)
        }
    }

    private var modernBloom: ModernBloomPass? = null
    private var modernBloomOn = false
    private var modernBloomCapturing = false
    private var modernBloomAborts = 0L

    override fun hasBloomSupport(): Boolean =
        System.getProperty("render.bloomsupport")?.equals("false", true) != true

    override fun textureCacheCapacity(): Int = Integer.getInteger("render.texturecache", 1024)
    override fun textureRetainCycles(): Int = Integer.getInteger("render.textureretain", 30)
    override fun textureCycleDivider(): Int = 60
    private var textureTouchFrame = IntArray(0)

    override fun bloomSupported(): Boolean {
        if (System.getProperty("render.bloom")?.equals("false", true) == true) return false
        if (!hasBloomSupport()) return false
        modernBloomOn = true
        return true
    }

    override fun resetBloom() {
        modernBloomOn = false
        if (modernBloomCapturing) abortModernBloom()
    }

    override fun bloomEnabled(): Boolean = modernBloomOn && canUseModernPipeline()

    override fun beginBloomCapture(screenCanvasWidth: Int, screenCanvasHeight: Int) {
        if (!bloomEnabled()) return
        if (modernBloomCapturing) return
        val bp = modernBloom ?: ModernBloomPass().also { modernBloom = it }
        flushSceneForUiDraw()
        endSpriteBatch()
        resolveSceneMsaa()
        if (!bp.begin(screenCanvasWidth, screenCanvasHeight)) return
        modernBloomCapturing = true
        applyClearFlags(3, 0)
        updateViewportAndCenter()
        functionMask = 0
    }

    override fun compositeBloomEffects() {
        if (!modernBloomCapturing) return
        val bp = modernBloom ?: return
        flushSceneForUiDraw()
        endSpriteBatch()
        resolveSceneMsaa()
        modernBloomCapturing = false
        val d = display
        if (directDisplayCursorPosition >= 0 || d == null) {
            bp.abort(0, screenWidthForBloom(), screenHeightForBloom())
            modernBloomAborts++
        } else {
            bp.composite(0, d.getWidth(), d.getHeight(),
                RectangularTextureGL.whitePoint, RectangularTextureGL.bloom, RectangularTextureGL.brightpass)
        }
        finishModernBloom()
    }

    private fun screenWidthForBloom(): Int = display?.getWidth() ?: modernBloom?.width ?: 0
    private fun screenHeightForBloom(): Int = display?.getHeight() ?: modernBloom?.height ?: 0

    private fun abortModernBloom() {
        if (!modernBloomCapturing) return
        modernBloomCapturing = false
        flushSceneForUiDraw()
        endSpriteBatch()
        resolveSceneMsaa()
        modernBloom?.abort(0, screenWidthForBloom(), screenHeightForBloom())
        modernBloomAborts++
        finishModernBloom()
    }

    private fun finishModernBloom() {
        functionMask = 0
        resetClipping()
        updateViewportAndCenter()
        resyncLegacyTextureBinding()
    }

    override fun initialize() {
        super.initialize()
        if (Thread.currentThread().id == ownerThreadId && !ownerThreadHasCapabilities) {
            try {
                GL.createCapabilities()
                ownerThreadHasCapabilities = true
                println("[ModernGLToolkit] LWJGL3 capabilities created eagerly in initialize() (${Thread.currentThread().name})")
                installGlDebugCallback()
            } catch (e: Exception) {
                println("[ModernGLToolkit] Eager capability creation failed: ${e.message} — will retry lazily after first render")
            }
        }
        if (ownerThreadHasCapabilities && !isModernRendererInitialized &&
            Thread.currentThread().id == ownerThreadId) {
            initializeModernRenderer()
        }
    }

    private val seenDebugMessages = HashSet<String>()
    private fun installGlDebugCallback() {
        try {
            val caps = GL.getCapabilities()
            if (!caps.GL_KHR_debug) {
                println("[ModernGLToolkit] KHR_debug unavailable")
                return
            }
            glEnable(KHRDebug.GL_DEBUG_OUTPUT)
            KHRDebug.glDebugMessageCallback({ _, type, _, severity, length, message, _ ->
                if (type == KHRDebug.GL_DEBUG_TYPE_ERROR ||
                    severity == KHRDebug.GL_DEBUG_SEVERITY_HIGH
                ) {
                    val msg = MemoryUtil.memUTF8(message, length)
                    if (seenDebugMessages.add(msg) && seenDebugMessages.size <= 20) {
                        println("[GLDebug] $msg")
                        Thread.dumpStack()
                    }
                }
            }, 0L)
            if (System.getProperty("render.gldebug") == null) {
                KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, null as IntBuffer?, false)
                KHRDebug.glDebugMessageControl(GL_DONT_CARE, KHRDebug.GL_DEBUG_TYPE_ERROR, GL_DONT_CARE, null as IntBuffer?, true)
                KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, KHRDebug.GL_DEBUG_SEVERITY_HIGH, null as IntBuffer?, true)
            }
            println("[ModernGLToolkit] KHR_debug callback installed")
        } catch (e: Throwable) {
            println("[ModernGLToolkit] KHR_debug install failed: $e")
        }
    }

    override fun generateFlags() {
        super.generateFlags()
        this.arbRectangleTexturesSupported = false
    }

    private fun applySwapInterval() {
        if (System.getProperty("render.vsync")?.equals("true", true) == true) return
        val glxDisplay = try { org.lwjgl.opengl.GLX14.glXGetCurrentDisplay() } catch (e: Throwable) { 0L }
        val glxDrawable = try { org.lwjgl.opengl.GLX.glXGetCurrentDrawable() } catch (e: Throwable) { 0L }
        if (glxDisplay != 0L && glxDrawable != 0L) {
            try {
                org.lwjgl.opengl.GLXEXTSwapControl.glXSwapIntervalEXT(glxDisplay, glxDrawable, 0)
                println("[ModernGLToolkit] swap interval 0 via GLX_EXT_swap_control")
                return
            } catch (e: Throwable) {
            }
        }
        try {
            org.lwjgl.opengl.GLXSGISwapControl.glXSwapIntervalSGI(0)
            println("[ModernGLToolkit] swap interval 0 via GLX_SGI_swap_control")
        } catch (e: Throwable) {
            println("[ModernGLToolkit] no usable swap-control extension; presentation stays vsynced (${e.message})")
        }
    }

    private fun initializeModernRenderer() {
        Culling.disabledForModern = true
        if (isModernRendererInitialized) return

        try {

            gpuTimer.initialize()
            applySwapInterval()

            modernRenderer = ModernModelRenderer()
            modernRenderer!!.initialize()

            groundRenderer = ModernGroundRenderer()
            groundRenderer!!.initialize()
            if (HdWaterRenderer.AVAILABLE) hdWater.initialize()

            spriteRenderer = ModernSpriteRenderer()
            spriteRenderer!!.initialize()
            spriteRenderer!!.scissorRestore = {
                if (modernScissorEnabled) GLWrapper.enable(GLWrapper.Constants.SCISSOR_TEST)
            }

            surfaceMaskRenderer = ModernSurfaceMaskRenderer()
            surfaceMaskRenderer!!.initialize()

            particleRendererModern = ModernParticleRenderer()
            particleRendererModern!!.initialize()

            billboardRendererModern = ModernBillboardRenderer()
            billboardRendererModern!!.initialize()

            frameBatcher = FrameBatcher()
            frameBatcher!!.initialize()

            optimizedBatcher = OptimizedFrameBatcher()
            optimizedBatcher!!.initialize()
            optimizedBatcher!!.setTextureScrollLookup(::lookupTextureScroll)

            instancedBatcher = InstancedModelBatcher()
            instancedBatcher!!.initialize()
            instancedBatcher!!.setTextureScrollLookup(::lookupTextureScroll)

            modelCache = ModelCache(maxCacheSize = 8192, maxVertexBytes = 128 * 1024 * 1024)

            isModernRendererInitialized = true
            println("[ModernGLToolkit] Modern renderer initialized (with model caching + instancing + sprite + particle renderer)")
        if (RetainedStaticScene.ENABLED) {
            val retained = RetainedStaticScene(this)
            retainedScene = retained
            Scene.retainedHook = retained
        }
        } catch (e: Exception) {
            println("[ModernGLToolkit] Failed to initialize modern renderer: ${e.message}")
            e.printStackTrace()
        }
    }

    private var streamGeometryHits = 0L
    private var streamGeometryBuilds = 0L

    private val animatedInstancing = System.getProperty("render.animinstanced")?.equals("false", true) != true

    private val VERIFY_HASH = System.getProperty("render.verifyhash") != null
    private var hashVerifyChecks = 0L
    private var hashVerifyBad = 0L

    private var debugFrameCount = 0

    private val diagInterval = System.getProperty("render.diaginterval")?.toIntOrNull()?.coerceAtLeast(1) ?: 600
    private val diagEnabled = System.getProperty("render.diag") != null || System.getProperty("render.diaginterval") != null

    private var contextReattachCount = 0

    private var perfLastRenderEntryNs = 0L
    private var perfFrameNsAcc = 0L
    private var perfRenderNsAcc = 0L
    private var perfSwapNsAcc = 0L
    private var perfFrameSamples = 0
    private var debugModernDraws = 0
    private var unwrittenSlotFaces = 0
    private var debugLegacyDraws = 0
    private var debugSetupSpriteCalls = 0
    private var debugBatchFlushes = 0

    private var sceneDepthCleared = false

    override fun render() {
        val renderEntryNs = System.nanoTime()
        if (modernBloomCapturing) abortModernBloom()
        if (perfLastRenderEntryNs != 0L) {
            perfFrameNsAcc += renderEntryNs - perfLastRenderEntryNs
            if (renderEntryNs - perfLastRenderEntryNs > perfFrameNsMax) perfFrameNsMax = renderEntryNs - perfLastRenderEntryNs
            if (STALL_DIAG_MS > 0 && renderEntryNs - perfLastRenderEntryNs > STALL_DIAG_MS * 1_000_000L) {
                System.err.println("[StallDiag] frame=%.1fms walk=%.2f draw=%.2f floor=%.2f alpha=%.2f retained=%.2f sceneMs=%.2f render=%.2f swap=%.2f streamBuilds=%d captures=%d walkCacheRebuilds=%d redraw=%.2f canvas=%.2f dealloc=%.2f gpuWait=%.2f staged=%d deferredMs=%.2f %s".format(
                    (renderEntryNs - perfLastRenderEntryNs) / 1e6,
                    (Scene.phaseWalkNs - stallPrevWalk) / 1e6, (Scene.phaseDrawNs - stallPrevDraw) / 1e6, (Scene.phaseFloorNs - stallPrevFloor) / 1e6,
                    (Scene.phaseAlphaNs - stallPrevAlpha) / 1e6, (Scene.phaseRetainedNs - stallPrevRetained) / 1e6,
                    (Client.sceneDrawNs - stallPrevScene) / 1e6, (perfRenderNsAcc - stallPrevRender) / 1e6, (perfSwapNsAcc - stallPrevSwap) / 1e6,
                    streamGeometryBuilds - stallPrevBuilds, retainedCaptures - stallPrevCaptures, Scene.walkCacheRebuildsAcc,
                    (Client.redrawNs - stallPrevRedraw) / 1e6, (Client.canvasRefreshNs - stallPrevCanvas) / 1e6, (animateTexturesNs - stallPrevDealloc) / 1e6,
                    (phaseGpuWaitNs - stallPrevGpuWait) / 1e6, BufferStaging.stagedCopies - stallPrevStaged, (deferredBuildNsAcc - stallPrevDeferred) / 1e6, gpuTimer.lastFrameString()))
            }
            stallPrevRedraw = Client.redrawNs; stallPrevCanvas = Client.canvasRefreshNs; stallPrevDealloc = animateTexturesNs
            stallPrevGpuWait = phaseGpuWaitNs; stallPrevStaged = BufferStaging.stagedCopies; stallPrevDeferred = deferredBuildNsAcc
            stallPrevWalk = Scene.phaseWalkNs; stallPrevDraw = Scene.phaseDrawNs; stallPrevFloor = Scene.phaseFloorNs
            stallPrevAlpha = Scene.phaseAlphaNs; stallPrevRetained = Scene.phaseRetainedNs; stallPrevScene = Client.sceneDrawNs
            stallPrevRender = perfRenderNsAcc; stallPrevSwap = perfSwapNsAcc; stallPrevBuilds = streamGeometryBuilds; stallPrevCaptures = retainedCaptures
            perfFrameSamples++
        }
        perfLastRenderEntryNs = renderEntryNs

        val ogl = this.activeOpenglInstance
        if (ogl != null && ogl.threadPeer !== Thread.currentThread()) {
            attach()
            (this.display ?: this.delegatingDisplay)?.reload()
            contextReattachCount++
        }

        GeometryDiff.runOnce(this)

        AutoLogin.tick()

        if (canUseModernPipeline()) {
            renderWithModernPipeline()
        }

        if (canUseModernPipeline()) {
            if (isSpriteBatchActive) debugBatchFlushes++
            endSpriteBatch()
        }

        debugFrameCount++
        if (diagEnabled && (debugFrameCount % diagInterval == 0 || debugFrameCount == 30 || debugFrameCount == 120)) {
            println(
                "[ModernDiag] pipeline=${canUseModernPipeline()} init=$isModernRendererInitialized " +
                    "caps=$ownerThreadHasCapabilities gameState=${try { Client.gameState } catch (_: Exception) { "?" }} " +
                    "groundDraws=${groundRenderer?.getFrameStats()?.drawCalls ?: -1} " +
                    "spriteQuads=${spriteRenderer?.getFrameStats()?.quads ?: -1} " +
                    "modelsBatched=$totalModelsProcessed culled=$modelsCulled " +
                    "smm=$diagSetModelMatrixCalls(rej=$diagSetModelMatrixRejected) " +
                    "batchOpen=$diagBatchOpens(lazy=$diagLazyBatchOpens) " +
                    "modelDraws=$diagModelGateAccepted/$diagModelGateCalls unwrittenSlots=${unwrittenSlotFaces + mainScratch.unwrittenSlotFaces} " +
                    (if (VERIFY_MODEL_CULL) "cullVerify=$modelCullChecks/bad=$modelCullBad " else "") +
                    "ptLights=$diagPointLights/$diagSceneLights recolour[ran=$recolourChecks bad=$recolourBad shadow=$recolourShadowSeen rev0=$recolourRev0 noMap=$recolourNoMap] " +
                    "${ModernModelBuilder.skipSummary()} " +
                    "inval[${ModelGL.invalidateSummary()}] " +
                    "ground=$diagGroundDrawn/noPipe=$diagGroundNoPipeline/noSnap=$diagGroundNoSnapshot/calls=$diagGroundCalls " +
                    "legacyDraws=$legacyDrawCalls " +
                    "reject=$lastPipelineRejectReason " +
                    "legacySkips=${if (legacySkipCounters.isEmpty()) "{}" else legacySkipCounters.toSortedMap().toString()}"
            )
            val obStats = optimizedBatcher?.getStats()
            val instStats = instancedBatcher?.getStats()
            println(
                "[ModernPerf] cullMs=%.2f tilesMs=%.2f ".format(
                    if (Scene.phaseSamples > 0) Scene.phaseCullNs / 1e6 / Scene.phaseSamples else 0.0,
                    if (Scene.phaseSamples > 0) Scene.phaseTilesNs / 1e6 / Scene.phaseSamples else 0.0
                ) +
                gpuTimer.summary() + " " +
                "modelMs=%.2f(n=%d) groundMs=%.2f(n=%d) gpuWaitMs=%.2f grid[dim=%d items=%d sel=%d cand=%d] ".format(
                    phaseModelNs / 1e6 / diagInterval, phaseModelCalls / diagInterval,
                    phaseGroundNs / 1e6 / diagInterval, phaseGroundCalls / diagInterval,
                    phaseGpuWaitNs / 1e6 / diagInterval, lightGridDim, lightGridItemCount,
                    lightSelectCalls / diagInterval, lightCandidatesScanned / diagInterval
                ) +
                "blocks[inst=${retainedBlockInstances / diagInterval}/frame builds=$retainedBlockBuilds fallback=$retainedBlockFallbacks transFast=${retainedTranslucentFast / diagInterval}/frame blockDraws=${retainedBlockDraws / diagInterval}/frame transTpl=${retainedTransTpl / diagInterval}/frame] " +
                "pulse[n=%d ms=%.2f] planes[%d/%d] walk[nodes=%d tiles=%d drawn=%d alpha=%d walkMs=%.2f sortMs=%.2f drawMs=%.2f floorMs=%.2f alphaMs=%.2f retainedMs=%.2f] retained[inst=%d culledChunks=%d culledInst=%d captures=%d] ".format(
                    GameShell.pulseCount,
                    if (GameShell.pulseCount > 0) GameShell.pulseNs / 1e6 / GameShell.pulseCount else 0.0,
                    Scene.planesDrawn / diagInterval, (Scene.planesDrawn + Scene.planesSkipped) / diagInterval,
                    Scene.walkNodes / diagInterval, Scene.walkTiles / diagInterval, Scene.walkDrawn / diagInterval,
                    Scene.walkAlpha / diagInterval,
                    Scene.phaseWalkNs / 1e6 / diagInterval, Scene.phaseSortNs / 1e6 / diagInterval,
                    Scene.phaseDrawNs / 1e6 / diagInterval, Scene.phaseFloorNs / 1e6 / diagInterval,
                    Scene.phaseAlphaNs / 1e6 / diagInterval,
                    Scene.phaseRetainedNs / 1e6 / diagInterval,
                    retainedSubmitted / diagInterval, retainedChunksCulled / diagInterval,
                    retainedInstancesCulled / diagInterval, retainedCaptures
                ) +
                "msaa[pref=$multiSamplesPreference arb=$arbMultisample windowSamples=$fbSamplesQueried sceneTarget=${if (sceneMsaa != null) sceneMsaaActiveSamples else 0} failed=$sceneMsaaFailed] " +
                "bloom[on=$modernBloomOn composites=${modernBloom?.composites ?: 0} aborts=$modernBloomAborts failed=${modernBloom?.failed ?: false}] " +
                "sceneMs=%.2f ".format(
                    if (Client.sceneDrawSamples > 0) Client.sceneDrawNs / 1e6 / Client.sceneDrawSamples else 0.0
                ) +
                "frameMs=%.1f(max=%.1f) renderMs=%.2f swapMs=%.2f ".format(
                    if (perfFrameSamples > 0) perfFrameNsAcc / 1e6 / perfFrameSamples else 0.0,
                    perfFrameNsMax / 1e6,
                    if (perfFrameSamples > 0) perfRenderNsAcc / 1e6 / perfFrameSamples else 0.0,
                    if (perfFrameSamples > 0) perfSwapNsAcc / 1e6 / perfFrameSamples else 0.0
                ) +
                    "cache=${cacheHits}h/${cacheMisses}m " +
                    "stream=${streamGeometryHits}h/${streamGeometryBuilds}b${streamBuildsByClass.entries.sortedByDescending { it.value[0] }.joinToString(",", "[", "]") { "${it.key.simpleName}=${it.value[0]}/${it.value[1]}f" }} spriteFlush=${spriteRenderer?.let { sr -> "${sr.flushCauses.values.sum() / diagInterval}/frame(quads=${sr.flushQuads / diagInterval})" + sr.flushCauses.entries.sortedByDescending { it.value }.joinToString(",", "[", "]") { "${it.key}=${it.value / diagInterval}" } } ?: "-"} walkCacheRebuilds=${Scene.walkCacheRebuildsTotal()} ${PoseMemo.statsAndReset()} deferredBuild[n=${deferredBuildsTotal / diagInterval}/frame ms=${"%.2f".format(deferredBuildMsAcc / diagInterval)}] bb[def=${bbDeferredTotal / diagInterval} drawn=${bbDrawnTotal / diagInterval}] " +
                    "bounds=${ModelGL.boundsCalls / diagInterval}/frame(verts=${ModelGL.boundsVertices / diagInterval}) " +
                    (if (VERIFY_HASH) "hashVerify=$hashVerifyChecks/bad=$hashVerifyBad " else "") +
                    "meshBuilt=$modernBuiltPathModels/fallback=$modernBuiltFallbackModels " +
                    "dropped[faces=${legacyCaptureDroppedFaces.get()} models=${legacyCaptureModelsWithDrops.get()} " +
                    "whole=${legacyCaptureModelsFullyDropped.get()} stream=$streamingModelsDropped] " +
                    "tkMs[hash=%.2f lookup=%.2f build=%.2f addCached=%.2f cull=%.2f beginModel=%.2f] ".format(
                        timeComputeHash / 1e6, timeCacheLookup / 1e6,
                        modelCache?.getBuildMillis() ?: 0f, timeAddCachedModel / 1e6,
                        timeFrustumCull / 1e6, timeBeginModel / 1e6
                    ) +
                    "mc[size=${modelCache?.getSize() ?: 0} evict=${modelCache?.getEvictions() ?: 0}] " +
                    "mbMs[add=%.2f up=%.2f draw=%.2f total=%.2f] ".format(
                        obStats?.timeAddModelsMs ?: 0f, obStats?.timeUploadMs ?: 0f,
                        obStats?.timeRenderMs ?: 0f, obStats?.timeTotalMs ?: 0f
                    ) +
                    "instMs[geoUp=%.2f instUp=%.2f draw=%.2f] ".format(
                        instStats?.geomUploadMs ?: 0f, instStats?.instanceUploadMs ?: 0f,
                        instStats?.drawMs ?: 0f
                    ) +
                    "trans[groups=${(instancedBatcher?.translucentGroupsDrawn ?: 0L) / diagInterval} cmds=${(instancedBatcher?.translucentCommandsDrawn ?: 0L) / diagInterval}] " +
                    "inst[unique=${instStats?.uniqueMeshes ?: 0} instances=${instStats?.totalInstances ?: 0} " +
                    "drawCalls=${instStats?.drawCalls ?: 0}] " +
                    "store[verts=${instStats?.storeVertices ?: 0} evicted=${instStats?.meshesEvicted ?: 0} refit=${instStats?.refits ?: 0} " +
                    "overflow=${instStats?.capacityFallbacks ?: 0}] " +
                    "mbDrawCalls=${obStats?.drawCalls ?: 0} " +
                    "fog[on=$fogEnabled dens=$fogDensity start=%.0f end=%.0f col=%06X] ".format(fogStartPosition, fogEndPosition, fogColor and 0xffffff) +
                    "light[sunB=%.3f int=%.3f anti=%.3f col=%.2f,%.2f,%.2f] ".format(sunBrightness, sunColorIntensity, antiSunColorIntensity, sunColorRed, sunColorGreen, sunColorBlue) +
                    GeometryDiff.liveSummary() + " " + GeometryDiff.contrastSummary() + " " + GeometryDiff.groundSummary()
            )
            cacheHits = 0
            cacheMisses = 0
            streamGeometryHits = 0
            streamGeometryBuilds = 0
            streamBuildsByClass.clear()
            instancedBatcher?.let { it.translucentGroupsDrawn = 0L; it.translucentCommandsDrawn = 0L }
            spriteRenderer?.let { it.flushCauses.clear(); it.flushQuads = 0L }
            bbDeferredTotal = 0; bbDrawnTotal = 0
            deferredBuildsTotal = 0; deferredBuildMsAcc = 0.0
            ModelGL.boundsCalls = 0
            ModelGL.boundsVertices = 0
            phaseModelNs = 0L
            phaseModelCalls = 0
            phaseGroundNs = 0L
            phaseGroundCalls = 0
            phaseLightSelectNs = 0L
            phaseGpuWaitNs = 0L
            lightSelectCalls = 0L
            lightCandidatesScanned = 0L
            gpuTimer.reset()
            GameShell.pulseNs = 0L
            GameShell.pulseCount = 0L
            Scene.planesDrawn = 0L
            Scene.planesSkipped = 0L
            Scene.walkNodes = 0L
            Scene.walkTiles = 0L
            Scene.walkDrawn = 0L
            Scene.walkAlpha = 0L
            Scene.phaseAlphaNs = 0L
            Scene.phaseRetainedNs = 0L
            retainedSubmitted = 0L
            retainedBlockInstances = 0; retainedBlockBuilds = 0; retainedBlockFallbacks = 0; retainedTranslucentFast = 0; retainedBlockDraws = 0; retainedTransTpl = 0
            retainedChunksCulled = 0L
            retainedInstancesCulled = 0L
            retainedCaptures = 0L
            Scene.phaseWalkNs = 0L
            Scene.phaseSortNs = 0L
            Scene.phaseDrawNs = 0L
            Scene.phaseFloorNs = 0L
            Scene.phaseCullNs = 0L
            Scene.phaseTilesNs = 0L
            Scene.phaseSamples = 0
            Client.sceneDrawNs = 0L
            Client.sceneDrawSamples = 0
            modernBuiltPathModels = 0L
            modernBuiltFallbackModels = 0L
            legacyCaptureDroppedFaces.set(0L)
            legacyCaptureModelsWithDrops.set(0L)
            legacyCaptureModelsFullyDropped.set(0L)
            streamingModelsDropped = 0L
            perfFrameNsAcc = 0L
            perfFrameNsMax = 0L
            perfRenderNsAcc = 0L
            perfSwapNsAcc = 0L
            perfFrameSamples = 0

            val fboAtSwap = glGetInteger(GL_FRAMEBUFFER_BINDING)
            val vpNow = IntArray(4)
            glGetIntegerv(GL_VIEWPORT, vpNow)
            val scissorNow = glIsEnabled(GL_SCISSOR_TEST)
            val scissorBoxNow = IntArray(4)
            glGetIntegerv(GL_SCISSOR_BOX, scissorBoxNow)
            val sr = spriteRenderer
            println(
                "[ModernDiag2] fboAtSwap=$fboAtSwap viewportAtSwap=${vpNow.joinToString(",")} " +
                    "scissorAtSwap=${if (scissorNow) scissorBoxNow.joinToString(",") else "off"} " +
                    "fboAtFlush=${sr?.lastFlushFbo ?: -1} " +
                    "flushViewport=${sr?.lastFlushViewport?.joinToString(",") ?: "?"} " +
                    "flushScissor=${if (sr?.lastFlushScissorEnabled == true) sr.lastFlushScissorBox.joinToString(",") else "off"} " +
                    "delegatingDisplay=${delegatingDisplay != null} display=${display != null} " +
                    "displayDims=${display?.getWidth()}x${display?.getHeight()}"
            )

            try {
                val canvas = GameShell.gameCanvas
                val producer = delegatingDisplay as? ImageProducerGL
                println(
                    "[ModernDiag3] canvas=${System.identityHashCode(canvas)} " +
                        "displayable=${canvas?.isDisplayable} showing=${canvas?.isShowing} " +
                        "canvasSize=${canvas?.width}x${canvas?.height} loc=${canvas?.x},${canvas?.y} " +
                        "producerCanvas=${System.identityHashCode(producer?.canvas)} " +
                        "sameCanvas=${producer?.canvas === canvas} " +
                        "surfaceHandle=${producer?.surfaceHandle} " +
                        "parentShowing=${canvas?.parent?.isShowing} " +
                        "ctxOnThisThread=${this.activeOpenglInstance?.threadPeer === Thread.currentThread()} " +
                        "ctxReattaches=$contextReattachCount"
                )
            } catch (e: Exception) {
                println("[ModernDiag3] failed: ${e.message}")
            }

            legacySkipCounters.clear()
            legacyDrawCalls = 0L
            diagSetModelMatrixCalls = 0
            diagSetModelMatrixRejected = 0
            diagBatchOpens = 0
            diagLazyBatchOpens = 0
            diagModelGateCalls = 0
            diagModelGateAccepted = 0
            diagGroundCalls = 0
            diagGroundNoPipeline = 0
            diagGroundNoSnapshot = 0
            diagGroundDrawn = 0
            modelCullChecks = 0
            modelCullBad = 0
        }
        debugModernDraws = 0
        debugLegacyDraws = 0
        debugSetupSpriteCalls = 0
        debugBatchFlushes = 0

        timeFrustumCull = 0L
        timeBeginModel = 0L
        timeComputeHash = 0L
        timeCacheLookup = 0L
        timeAddCachedModel = 0L
        modelCache?.resetBuildTimer()
        spriteRenderer?.resetFrameStats()
        optimizedBatcher?.resetCumulativeStats()
        instancedBatcher?.resetFrameStats()
        particleRendererModern?.resetFrameStats()

        if (GPU_SYNC) {
            val fenceStartNs = System.nanoTime()
            glFinish()
            phaseGpuWaitNs += System.nanoTime() - fenceStartNs
        }

        val swapStartNs = System.nanoTime()
        gpuTimer.frameStampEnd()
        com.darkan.bot.FrameCapture.poll()
        com.jagex.game.runetek5.graphics.modern.dump.FrameDump.frameEnd()
        super.render()
        gpuTimer.frameStampStart()
        val swapEndNs = System.nanoTime()
        perfSwapNsAcc += swapEndNs - swapStartNs
        perfRenderNsAcc += swapEndNs - perfLastRenderEntryNs

        gpuTimer.nextFrame()

        sceneDepthCleared = false

        if (Thread.currentThread().id == ownerThreadId && !ownerThreadHasCapabilities) {
            try {
                GL.createCapabilities()
                ownerThreadHasCapabilities = true
                println("[ModernGLToolkit] LWJGL3 capabilities created on main thread (${Thread.currentThread().name})")
            } catch (e: Exception) {
                println("[ModernGLToolkit] Failed to create GL capabilities: ${e.message}")
            }
        }

        if (ownerThreadHasCapabilities && !isModernRendererInitialized &&
            Thread.currentThread().id == ownerThreadId) {
            initializeModernRenderer()
        }
    }

    override fun setViewMatrix(viewMatrix: Matrix43) {
        val batchOpen = canUseModernPipeline() && when {
            useOptimizedBatcher -> optimizedBatcher?.isActive() == true
            else -> frameBatcher?.isActive() == true
        }
        if (!batchOpen || sameMatrix(viewMatrix43, viewMatrix)) {
            super.setViewMatrix(viewMatrix)
            return
        }
        val pending = hasPendingSceneGeometry()
        if (pending) {
            renderWithModernPipeline()
            functionMask = 0
        }
        super.setViewMatrix(viewMatrix)
        if (pending) {
            openSceneBatch()
        } else {
            ensureSceneTarget()
            if (useOptimizedBatcher) {
                optimizedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                if (useInstancing) {
                    instancedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                    snapshotSceneMatrices()
                }
                armBatchLightingAndFog()
            }
        }
    }

    private fun sameMatrix(a: Matrix43, b: Matrix43): Boolean =
        a.xA == b.xA && a.yA == b.yA && a.zA == b.zA &&
            a.xB == b.xB && a.yB == b.yB && a.zB == b.zB &&
            a.xC == b.xC && a.yC == b.yC && a.zC == b.zC &&
            a.xD == b.xD && a.yD == b.yD && a.zD == b.zD

    override fun setModelMatrix(modelMatrix: Matrix44) {
        super.setModelMatrix(modelMatrix)

        diagSetModelMatrixCalls++

        if (!canUseModernPipeline()) {
            diagSetModelMatrixRejected++
        }
        if (canUseModernPipeline()) {
            endSpriteBatch()
            var batcherActive = when {
                useOptimizedBatcher -> optimizedBatcher?.isActive() == true
                else -> frameBatcher?.isActive() == true
            }

            if (batcherActive) {
                if (hasPendingSceneGeometry()) {
                    renderWithModernPipeline()
                    functionMask = 0
                    batcherActive = false
                } else {
                    ensureSceneTarget()
                    if (useOptimizedBatcher) {
                        optimizedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                        if (useInstancing) {
                            instancedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                            snapshotSceneMatrices()
                        }
                        armBatchLightingAndFog()
                    }
                }
            }

            if (!batcherActive) {
                openSceneBatch()
            }
        }
    }

    private var sceneMsaa: MultisampleFramebuffer? = null
    private var sceneMsaaBound = false
    private var sceneMsaaRestoreFbo = 0
    private var sceneMsaaFailed = false
    private var sceneMsaaActiveSamples = 0

    private fun bindSceneMsaa() {
        val samples = multiSamplesPreference
        if (samples <= 1 || sceneMsaaFailed || sceneMsaaBound) return
        val d = display ?: return
        val w = d.getWidth()
        val h = d.getHeight()
        if (w <= 0 || h <= 0) return
        val restoreFbo = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        var fb = sceneMsaa
        if (fb == null || fb.width != w || fb.height != h || fb.samples != samples) {
            val boundTexture = glGetInteger(GL_TEXTURE_BINDING_2D)
            fb?.destroy()
            fb = MultisampleFramebuffer(w, h, samples)
            if (!fb.create()) {
                sceneMsaaFailed = true
                sceneMsaa = null
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, restoreFbo)
                System.err.println("[ModernGLToolkit] multisampled scene target unavailable ($samples samples); rendering without anti-aliasing")
                return
            }
            sceneMsaa = fb
            glBindTexture(GL_TEXTURE_2D, boundTexture)
        }
        sceneMsaaRestoreFbo = restoreFbo
        FrameDump.aliasTarget(restoreFbo, fb.getId(), fb.width, fb.height)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sceneMsaaRestoreFbo)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fb.getId())
        blitWholeTarget(w, h)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fb.getId())
        sceneMsaaBound = true
        sceneMsaaActiveSamples = samples
    }

    private fun blitWholeTarget(w: Int, h: Int) {
        val scissored = glIsEnabled(GL_SCISSOR_TEST)
        if (scissored) glDisable(GL_SCISSOR_TEST)
        GLWrapper.blitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST)
        if (scissored) glEnable(GL_SCISSOR_TEST)
    }

    private fun ensureSceneTarget() {
        if (!sceneMsaaBound && directDisplayCursorPosition < 0) bindSceneMsaa()
    }

    private fun resolveSceneMsaa() {
        if (!sceneMsaaBound) return
        sceneMsaaBound = false
        val fb = sceneMsaa ?: return
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fb.getId())
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneMsaaRestoreFbo)
        blitWholeTarget(fb.width, fb.height)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, sceneMsaaRestoreFbo)
    }

    private fun applyModelBlend() {
        if (modelBlend) {
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        } else {
            glDisable(GL_BLEND)
        }
    }

    private fun openSceneBatch() {
        val mr = modernRenderer ?: return
        val gr = groundRenderer ?: return
        diagBatchOpens++

        bindSceneMsaa()
        if (!sceneDepthCleared) {
            glClear(GL_DEPTH_BUFFER_BIT)
            sceneDepthCleared = true
        }
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glDepthMask(true)
        applyModelBlend()
        glDisable(GL_CULL_FACE)

        mr.beginFrame()
        gr.resetFrameCounters()

        if (useOptimizedBatcher) {
            optimizedBatcher?.beginFrame()
            optimizedBatcher?.setViewProjection(viewProjectionMatrix.elements)
            buildFrameLights()
            optimizedBatcher?.setPointLights(frameLightCount, frameLightPos, frameLightColor)
            gr.setPointLights(frameLightCount, frameLightPos, frameLightColor)

            if (useInstancing) {
                instancedBatcher?.beginFrame()
                instancedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                snapshotSceneMatrices()
                instancedBatcher?.setPointLights(frameLightCount, frameLightPos, frameLightColor)
            }
            armBatchLightingAndFog()

        } else {
            frameBatcher?.beginFrame()
        }

        modelCache?.onFrameStart()

        modelCounter = 0

        textureHandleCache.clear()
        translucentSeqCounter = 0
    }

    private fun renderWithModernPipeline() {
        try {
            renderWithModernPipelineInner()
        } finally {
            resolveSceneMsaa()
            setDepthMask()
        }
    }

    private fun renderWithModernPipelineInner() {
        val mr = modernRenderer ?: return
        val gr = groundRenderer ?: return
        val batcher = frameBatcher ?: return
        setMultisampleEnabled(true)
        if (fbSamplesQueried < 0) {
            fbSamplesQueried = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_SAMPLES)
            fbSampleBuffers = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_SAMPLE_BUFFERS)
        }

        val presentTarget = delegatingDisplay ?: return
        val width = presentTarget.getWidth()
        val height = presentTarget.getHeight()
        if (width <= 0 || height <= 0) return

        val fov = Math.toRadians(60.0).toFloat()
        val aspect = width.toFloat() / height.toFloat()
        modernProjectionMatrix.identity().perspective(fov, aspect, 50f, 140000f)
        modernProjectionMatrix.scale(1f, -1f, 1f)

        updateCameraFromClient()

        viewMatrix.identity()
        viewMatrix.rotateX(-camPitch)
        viewMatrix.rotateY(-camYaw)
        viewMatrix.translate(-camX, -camY, -camZ)

        mr.setProjection(modernProjectionMatrix)
        mr.setView(viewMatrix)

        gr.setProjection(modernProjectionMatrix)
        gr.setView(viewMatrix)

        val lightDir = Vector3f(sunDirection[0], sunDirection[1], sunDirection[2])
        if (lightDir.lengthSquared() < 0.001f) {
            lightDir.set(0.5f, 1f, 0.3f)
        }
        lightDir.normalize()
        val lightColor = Vector3f(sunColorRed, sunColorGreen, sunColorBlue)
        mr.setLighting(lightDir, lightColor, 0.4f, 0.6f)
        gr.setLighting(lightDir, lightColor, 0.4f, 0.6f)

        if (fogEnabled && fogDensity > 0) {
            val legacyFogR = (fogColor and 0xff0000) / 16711680f
            val legacyFogG = (fogColor and 0xff00) / 65280f
            val legacyFogB = (fogColor and 0xff) / 255f
            mr.setFog(true, fogStartPosition, fogEndPosition, legacyFogR, legacyFogG, legacyFogB)
            gr.setFog(true, fogStartPosition, fogEndPosition, legacyFogR, legacyFogG, legacyFogB)
        } else {
            mr.setFog(false, 0f, 0f, 0f, 0f, 0f)
            gr.setFog(false, 0f, 0f, 0f, 0f, 0f)
        }

        val batcherActive = when {
            useOptimizedBatcher -> optimizedBatcher?.isActive() == true
            else -> batcher.isActive()
        }

        if (!batcherActive) {
            clearDeferredBuilds()
            hdWater.clear()
            groundReplayCount = 0
            deferredWaterCount = 0
            return
        }

        val vw = if (imageProducerWidth < 1) 1 else imageProducerWidth
        val vh = if (imageProducerHeight < 1) 1 else imageProducerHeight
        glViewport(drawableX + viewportOffsetX, viewportOffsetY + height - drawableY - vh, vw, vh)

        if (useOptimizedBatcher && optimizedBatcher?.isActive() == true) {
            flushDeferredBuilds()
            applyModelBlend()
            if (useInstancing && instancedBatcher?.isActive() == true) {
                instancedBatcher?.setTextureAlphaPredicate(::materialTextureHasAlpha)
                gpuTimer.begin(GPU_PASS_INSTANCED)
                instancedBatcher?.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
                gpuTimer.end(GPU_PASS_INSTANCED)
            }
            optimizedBatcher?.setTextureAlphaPredicate(::materialTextureHasAlpha)
            gpuTimer.begin(GPU_PASS_STREAM)
            optimizedBatcher?.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
            gpuTimer.end(GPU_PASS_STREAM)
            drawHdWater(width, height)
            drawTranslucentPass()
            mr.endFrame()
            gr.endFrame()
        } else if (batcher.isActive()) {
            batcher.endFrame(::lookupTextureHandle)
            mr.endFrame()
            gr.endFrame()
        }

        glFrontFace(GL_CCW)
        glDisable(GL_CULL_FACE)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        updateViewportAndCenter()
        resyncLegacyTextureBinding()

        frameCounter++
    }

    private fun resyncLegacyTextureBinding() {
        val cache = openglTextures ?: return
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        if (glTexturesCursorPos in cache.indices) cache[glTexturesCursorPos] = null
        functionMask = functionMask and -2
    }

    private fun drawTranslucentPass() {
        val inst = if (useInstancing) instancedBatcher else null
        val stream = optimizedBatcher
        val pr = particleRendererModern
        val particleCount = deferredParticles.size
        if (TRANS_DIAG && transDiagTick++ % 300 == 122) {
            System.err.println("[TransDiag] instPending=${inst?.pendingTranslucent()} streamPending=${stream?.pendingTranslucent()} " +
                "particles=$particleCount seqCounter=$translucentSeqCounter " +
                "submittedInst=$transDiagInst submittedStream=$transDiagStream keys=[${transDiagMinKey}..${transDiagMaxKey}]")
        }
        transDiagInst = 0; transDiagStream = 0; transDiagMinKey = Int.MAX_VALUE; transDiagMaxKey = Int.MIN_VALUE
        sortFarToNear(deferredParticleKey, deferredParticleSeq, particleCount, deferredParticleOrder)
        val bbN = bbCount
        sortFarToNear(bbKey, bbSeq, bbN, bbOrder)
        if (bbN > 0) {
            val hasFog = fogEnabled && fogDensity > 0
            billboardRendererModern?.setFrame(
                if (sceneSnapshotValid) sceneProjSnapshot else currentModelMatrix.elements, hasFog, fogStartPosition, fogEndPosition,
                if (hasFog) (fogColor and 0xff0000) / 16711680f else 0f,
                if (hasFog) (fogColor and 0xff00) / 65280f else 0f,
                if (hasFog) (fogColor and 0xff) / 255f else 0f
            )
        }
        var next = 0
        var nextBb = 0
        while (true) {
            val pKey = if (next < particleCount) deferredParticleKey[deferredParticleOrder[next]] else Int.MIN_VALUE
            val pSeq = if (next < particleCount) deferredParticleSeq[deferredParticleOrder[next]] else Int.MAX_VALUE
            val bKey = if (nextBb < bbN) bbKey[bbOrder[nextBb]] else Int.MIN_VALUE
            val bSeq = if (nextBb < bbN) bbSeq[bbOrder[nextBb]] else Int.MAX_VALUE
            val particleFirst = next < particleCount && (nextBb >= bbN || pKey > bKey || (pKey == bKey && pSeq < bSeq))
            val upToKey = if (particleFirst) pKey else bKey
            val upToSeq = if (particleFirst) pSeq else bSeq
            inst?.drawTranslucent(::lookupTextureHandle, ::lookupCombineMode, upToKey, upToSeq)
            stream?.drawTranslucent(::lookupTextureHandle, ::lookupCombineMode, upToKey, upToSeq)
            if (next >= particleCount && nextBb >= bbN) break
            if (particleFirst) {
                if (pr != null) renderParticlesNow(pr, deferredParticles[deferredParticleOrder[next]])
                next++
            } else {
                drawBillboardNow(bbOrder[nextBb])
                nextBb++
            }
        }
        deferredParticles.clear()
        bbCount = 0
    }

    fun beginCapture(): ArrayList<RetainedStaticScene.Capture> {
        val list = ArrayList<RetainedStaticScene.Capture>(4)
        captureList = list
        captureUnsupported = false
        return list
    }

    fun endCapture(): Boolean {
        captureList = null
        val unsupported = captureUnsupported
        captureUnsupported = false
        return unsupported
    }

    fun canSubmitRetained(): Boolean =
        canUseModernPipeline() && optimizedBatcher?.isActive() == true

    fun submitRetained(
        cached: ModelCache.CachedModel,
        geometryKey: Long,
        matrix: Matrix43,
        model: ModelGL,
        translucent: Boolean,
        depthKey: Int
    ) {
        val batcher = optimizedBatcher ?: return
        selectLightsForModel(model, matrix)
        if (LIGHT_DIAG && lightDiagInstBudget > 0) {
            val camX = Camera.cameraPositionAbsoluteX.toFloat(); val camZ = Camera.cameraPositionAbsoluteY.toFloat()
            val dx = matrix.xD - camX; val dz = matrix.zD - camZ
            if (dx * dx + dz * dz < 1500f * 1500f && currentLightIdx[0] >= 0f) {
                lightDiagInstBudget--
                System.err.println("[LightDiag] retained model=${model.hashCode()} faces=${model.numFaces} at=(${matrix.xD},${matrix.yD},${matrix.zD}) lights=(${currentLightIdx[0]},${currentLightIdx[1]},${currentLightIdx[2]},${currentLightIdx[3]}) contrast=${model.contrast}")
            }
        }
        val seq = if (translucent) translucentSeqCounter++ else 0
        submitResolved(batcher, cached, geometryKey, matrix, translucent, true, seq, depthKey)
        totalModelsProcessed++
    }

    fun lightEpochNow(): Int = lightEpoch

    fun nextTranslucentSeq(): Int = translucentSeqCounter++

    fun retainedBatcher(): InstancedModelBatcher? =
        if (instancingEnabled && useInstancing) instancedBatcher?.takeIf { it.isActive() } else null

    @JvmField var retainedBlockInstances = 0L
    @JvmField var retainedBlockBuilds = 0L
    @JvmField var retainedBlockFallbacks = 0L
    @JvmField var retainedTranslucentFast = 0L
    @JvmField var retainedBlockDraws = 0L
    @JvmField var retainedTransTpl = 0L

    fun fillMaterialWords(batcher: InstancedModelBatcher, mats: IntArray, count: Int, out: IntArray): Boolean =
        batcher.fillMaterialWords(mats, count, out, ::lookupTextureHandle, ::lookupCombineMode)

    fun selectLightsInto(x: Float, y: Float, z: Float, out: FloatArray) {
        selectLightsFor(x, y, z)
        out[0] = currentLightIdx[0]; out[1] = currentLightIdx[1]
        out[2] = currentLightIdx[2]; out[3] = currentLightIdx[3]
    }

    fun submitRetainedLit(
        cached: ModelCache.CachedModel,
        geometryKey: Long,
        matrix: Matrix43,
        translucent: Boolean,
        depthKey: Int,
        lightIdx: FloatArray
    ) {
        val batcher = optimizedBatcher ?: return
        currentLightIdx[0] = lightIdx[0]; currentLightIdx[1] = lightIdx[1]
        currentLightIdx[2] = lightIdx[2]; currentLightIdx[3] = lightIdx[3]
        val seq = if (translucent) translucentSeqCounter++ else 0
        submitResolved(batcher, cached, geometryKey, matrix, translucent, true, seq, depthKey)
        totalModelsProcessed++
    }

    private val retainedProjectionScratch = FloatArray(3)

    fun retainedDepth(x: Float, y: Float, z: Float): Int {
        calculateProjection(x, y, z, retainedProjectionScratch)
        return retainedProjectionScratch[2].toInt()
    }

    fun isWorldAabbVisible(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float): Boolean {
        val vp = viewProjectionMatrix.elements
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val ex = (maxX - minX) * 0.5f
        val ey = (maxY - minY) * 0.5f
        val ez = (maxZ - minZ) * 0.5f
        val vp0 = vp[0]; val vp1 = vp[1]; val vp2 = vp[2]; val vp3 = vp[3]
        val vp4 = vp[4]; val vp5 = vp[5]; val vp6 = vp[6]; val vp7 = vp[7]
        val vp8 = vp[8]; val vp9 = vp[9]; val vp10 = vp[10]; val vp11 = vp[11]
        val vp12 = vp[12]; val vp13 = vp[13]; val vp14 = vp[14]; val vp15 = vp[15]
        if (aabbOutsidePlane(vp0 + vp3, vp4 + vp7, vp8 + vp11, vp12 + vp15, cx, cy, cz, ex, ey, ez)) return false
        if (aabbOutsidePlane(vp3 - vp0, vp7 - vp4, vp11 - vp8, vp15 - vp12, cx, cy, cz, ex, ey, ez)) return false
        if (aabbOutsidePlane(vp1 + vp3, vp5 + vp7, vp9 + vp11, vp13 + vp15, cx, cy, cz, ex, ey, ez)) return false
        if (aabbOutsidePlane(vp3 - vp1, vp7 - vp5, vp11 - vp9, vp15 - vp13, cx, cy, cz, ex, ey, ez)) return false
        if (aabbOutsidePlane(vp2, vp6, vp10, vp14, cx, cy, cz, ex, ey, ez)) return false
        if (aabbOutsidePlane(vp3 - vp2, vp7 - vp6, vp11 - vp10, vp15 - vp14, cx, cy, cz, ex, ey, ez)) return false
        return true
    }

    fun isWorldAabbFullyInside(minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float): Boolean {
        val vp = viewProjectionMatrix.elements
        val cx = (minX + maxX) * 0.5f
        val cy = (minY + maxY) * 0.5f
        val cz = (minZ + maxZ) * 0.5f
        val ex = (maxX - minX) * 0.5f
        val ey = (maxY - minY) * 0.5f
        val ez = (maxZ - minZ) * 0.5f
        val vp0 = vp[0]; val vp1 = vp[1]; val vp2 = vp[2]; val vp3 = vp[3]
        val vp4 = vp[4]; val vp5 = vp[5]; val vp6 = vp[6]; val vp7 = vp[7]
        val vp8 = vp[8]; val vp9 = vp[9]; val vp10 = vp[10]; val vp11 = vp[11]
        val vp12 = vp[12]; val vp13 = vp[13]; val vp14 = vp[14]; val vp15 = vp[15]
        if (!aabbInsidePlane(vp0 + vp3, vp4 + vp7, vp8 + vp11, vp12 + vp15, cx, cy, cz, ex, ey, ez)) return false
        if (!aabbInsidePlane(vp3 - vp0, vp7 - vp4, vp11 - vp8, vp15 - vp12, cx, cy, cz, ex, ey, ez)) return false
        if (!aabbInsidePlane(vp1 + vp3, vp5 + vp7, vp9 + vp11, vp13 + vp15, cx, cy, cz, ex, ey, ez)) return false
        if (!aabbInsidePlane(vp3 - vp1, vp7 - vp5, vp11 - vp9, vp15 - vp13, cx, cy, cz, ex, ey, ez)) return false
        if (!aabbInsidePlane(vp2, vp6, vp10, vp14, cx, cy, cz, ex, ey, ez)) return false
        if (!aabbInsidePlane(vp3 - vp2, vp7 - vp6, vp11 - vp10, vp15 - vp14, cx, cy, cz, ex, ey, ez)) return false
        return true
    }

    @JvmField var retainedSubmitted = 0L
    @JvmField var retainedChunksCulled = 0L
    @JvmField var retainedInstancesCulled = 0L
    @JvmField var retainedCaptures = 0L

    private fun ensureSpriteBatch() {
        val sr = spriteRenderer ?: return
        if (isSpriteBatchActive) return
        if (!canUseModernPipeline()) return

        flushSceneForUiDraw()

        val target = display ?: delegatingDisplay ?: return
        val width = target.getWidth()
        val height = target.getHeight()
        if (width <= 0 || height <= 0) return

        setOrthoMatrixMode()

        sr.begin(width, height)
        isSpriteBatchActive = true
    }

    fun endSpriteBatch() {
        if (!isSpriteBatchActive) return
        spriteRenderer?.end()
        isSpriteBatchActive = false

        updateViewportAndCenter()

        resyncLegacyTextureBinding()

        functionMask = 0
    }

    fun getSpriteRenderer(): ModernSpriteRenderer? = spriteRenderer

    override fun setActiveTexture(replacingCombiner: OpenGlTexture?) {
        if (isSpriteBatchActive) {
            spriteRenderer?.flush("setActiveTexture")
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        }
        groundRenderer?.invalidateBoundTexture()
        super.setActiveTexture(replacingCombiner)
    }

    private val frameLights = ArrayList<PointLight>()
    private val frameLightPos = FloatArray(InstancedModelBatcher.MAX_POINT_LIGHTS * 4)
    private val frameLightColor = FloatArray(InstancedModelBatcher.MAX_POINT_LIGHTS * 3)
    private var frameLightCount = 0
    @JvmField var diagPointLights = 0
    @JvmField var diagSceneLights = 0
    private var recolourChecks = 0
    @JvmField var recolourShadowSeen = 0
    @JvmField var recolourRev0 = 0
    @JvmField var recolourNoMap = 0
    @JvmField var recolourBad = 0
    private val VERIFY_RECOLOUR = System.getProperty("render.verifyrecolour") != null
    private val currentLightIdx = FloatArray(4) { -1f }

    override fun usesFixedFunctionLights(): Boolean = false

    override fun copyLights(lightCount: Int, lights: Array<PointLight>?) {
        super.copyLights(lightCount, lights)
    }

    private fun selectLightsFor(wx: Float, wy: Float, wz: Float) {
        currentLightIdx[0] = -1f; currentLightIdx[1] = -1f
        currentLightIdx[2] = -1f; currentLightIdx[3] = -1f
        val n = frameLightCount
        if (n <= 0) {
            rememberLightSelection(wx, wy, wz)
            return
        }
        var b0 = Float.MAX_VALUE; var b1 = Float.MAX_VALUE
        var b2 = Float.MAX_VALUE; var b3 = Float.MAX_VALUE
        var i0 = -1; var i1 = -1; var i2 = -1; var i3 = -1

        lightSelectCalls++
        if (wx == lastLightSelectX && wy == lastLightSelectY && wz == lastLightSelectZ && lastLightSelectValid) {
            currentLightIdx[0] = lastLightIdx0; currentLightIdx[1] = lastLightIdx1
            currentLightIdx[2] = lastLightIdx2; currentLightIdx[3] = lastLightIdx3
            return
        }
        val dim = lightGridDim
        var scanStart = 0
        var scanEnd = n
        var useGrid = false
        var cellStart = 0
        var cellEnd = 0
        if (dim > 0) {
            val cx = ((wx - lightGridMinX) * lightGridInvCell).toInt()
            val cz = ((wz - lightGridMinZ) * lightGridInvCell).toInt()
            if (cx < 0 || cz < 0 || cx >= dim || cz >= dim) {
                rememberLightSelection(wx, wy, wz)
                return
            }
            val cell = cz * dim + cx
            cellStart = lightGridStart[cell]
            cellEnd = lightGridStart[cell + 1]
            useGrid = true
            scanStart = cellStart
            scanEnd = cellEnd
        }

        if (useGrid) {
            val pos = lightCellPos
            val idx = lightCellIdx
            val floors = lightCellRankFloor
            var s = cellStart
            var p = cellStart * 4
            while (s < cellEnd) {
                val floor = floors[s]
                if (floor >= b3 || floor >= 255f) break
                val dx = pos[p] - wx
                val dy = pos[p + 1] - wy
                val dz = pos[p + 2] - wz
                val rank = pos[p + 3] * (dx * dx + dy * dy + dz * dz)
                p += 4
                val i = idx[s]
                s++
                lightCandidatesScanned++
                if (rank >= 255f) continue
                when {
                    rank < b0 -> { b3=b2; i3=i2; b2=b1; i2=i1; b1=b0; i1=i0; b0=rank; i0=i }
                    rank < b1 -> { b3=b2; i3=i2; b2=b1; i2=i1; b1=rank; i1=i }
                    rank < b2 -> { b3=b2; i3=i2; b2=rank; i2=i }
                    rank < b3 -> { b3=rank; i3=i }
                }
            }
        } else {
            var i = scanStart
            while (i < scanEnd) {
                val base = i * 4
                val dx = frameLightPos[base] - wx
                val dy = frameLightPos[base + 1] - wy
                val dz = frameLightPos[base + 2] - wz
                val rank = frameLightPos[base + 3] * (dx * dx + dy * dy + dz * dz)
                if (rank < 255f) {
                    when {
                        rank < b0 -> { b3=b2; i3=i2; b2=b1; i2=i1; b1=b0; i1=i0; b0=rank; i0=i }
                        rank < b1 -> { b3=b2; i3=i2; b2=b1; i2=i1; b1=rank; i1=i }
                        rank < b2 -> { b3=b2; i3=i2; b2=rank; i2=i }
                        rank < b3 -> { b3=rank; i3=i }
                    }
                }
                i++
            }
        }
        currentLightIdx[0] = i0.toFloat(); currentLightIdx[1] = i1.toFloat()
        currentLightIdx[2] = i2.toFloat(); currentLightIdx[3] = i3.toFloat()
        rememberLightSelection(wx, wy, wz)
    }

    private var lastLightSelectValid = false
    private var lastLightSelectX = 0f
    private var lastLightSelectY = 0f
    private var lastLightSelectZ = 0f
    private var lastLightIdx0 = -1f
    private var lastLightIdx1 = -1f
    private var lastLightIdx2 = -1f
    private var lastLightIdx3 = -1f

    private fun rememberLightSelection(wx: Float, wy: Float, wz: Float) {
        lastLightSelectX = wx; lastLightSelectY = wy; lastLightSelectZ = wz
        lastLightIdx0 = currentLightIdx[0]; lastLightIdx1 = currentLightIdx[1]
        lastLightIdx2 = currentLightIdx[2]; lastLightIdx3 = currentLightIdx[3]
        lastLightSelectValid = true
    }

    private var lightGridDim = 0
    private var lightGridMinX = 0f
    private var lightGridMinZ = 0f
    private var lightGridInvCell = 0f
    private var lightGridStart = IntArray(0)

    private var lightCellIdx = IntArray(0)

    private var lightCellPos = FloatArray(0)

    private var lightCellRankFloor = FloatArray(0)

    private val LIGHT_GRID_DIM = 32

    private fun buildLightGrid() {
        val n = frameLightCount
        if (n <= 0) {
            if (lightGridDim != 0) lightEpoch++
            lightGridDim = 0
            lightGridSignature = 0L
            return
        }

        var signature = n.toLong() * 1099511628211L
        for (i in 0 until n) {
            val base = i * 4
            signature = signature * 31 + java.lang.Float.floatToRawIntBits(frameLightPos[base])
            signature = signature * 31 + java.lang.Float.floatToRawIntBits(frameLightPos[base + 2])
            signature = signature * 31 + java.lang.Float.floatToRawIntBits(frameLightPos[base + 3])
        }
        if (signature == lightGridSignature && lightGridDim > 0) return
        lightGridSignature = signature
        lightEpoch++

        val radiusK = 15.9687194f
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (i in 0 until n) {
            val base = i * 4
            val kq = frameLightPos[base + 3]
            val r = if (kq > 0f) radiusK / kotlin.math.sqrt(kq) else 0f
            val x = frameLightPos[base]
            val z = frameLightPos[base + 2]
            if (x - r < minX) minX = x - r
            if (x + r > maxX) maxX = x + r
            if (z - r < minZ) minZ = z - r
            if (z + r > maxZ) maxZ = z + r
        }
        val extent = kotlin.math.max(maxX - minX, maxZ - minZ)
        if (!(extent > 0f) || extent.isNaN()) { lightGridDim = 0; return }

        val dim = LIGHT_GRID_DIM
        val cell = extent / dim
        val cells = dim * dim
        if (lightGridStart.size < cells + 1) lightGridStart = IntArray(cells + 1)
        val start = lightGridStart
        java.util.Arrays.fill(start, 0, cells + 1, 0)
        lightGridDim = dim
        lightGridMinX = minX
        lightGridMinZ = minZ
        lightGridInvCell = 1f / cell

        var total = 0
        for (i in 0 until n) {
            val base = i * 4
            val kq = frameLightPos[base + 3]
            val r = if (kq > 0f) radiusK / kotlin.math.sqrt(kq) else 0f
            val x0 = (((frameLightPos[base] - r) - minX) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            val x1 = (((frameLightPos[base] + r) - minX) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            val z0 = (((frameLightPos[base + 2] - r) - minZ) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            val z1 = (((frameLightPos[base + 2] + r) - minZ) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            for (cz in z0..z1) {
                val row = cz * dim
                for (cx in x0..x1) start[row + cx + 1]++
            }
            total += (x1 - x0 + 1) * (z1 - z0 + 1)
        }
        for (c in 0 until cells) start[c + 1] += start[c]
        if (lightCellIdx.size < total) {
            lightCellIdx = IntArray(total * 2)
            lightCellPos = FloatArray(total * 2 * 4)
            lightCellRankFloor = FloatArray(total * 2)
        }
        lightGridItemCount = total
        val items = lightCellIdx
        val cellPos = lightCellPos

        if (lightGridCursor.size < cells) lightGridCursor = IntArray(cells * 2)
        val cursor = lightGridCursor
        System.arraycopy(start, 0, cursor, 0, cells)
        for (i in 0 until n) {
            val base = i * 4
            val kq = frameLightPos[base + 3]
            val r = if (kq > 0f) radiusK / kotlin.math.sqrt(kq) else 0f
            val x0 = (((frameLightPos[base] - r) - minX) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            val x1 = (((frameLightPos[base] + r) - minX) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            val z0 = (((frameLightPos[base + 2] - r) - minZ) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            val z1 = (((frameLightPos[base + 2] + r) - minZ) * lightGridInvCell).toInt().coerceIn(0, dim - 1)
            for (cz in z0..z1) {
                val row = cz * dim
                val cellMinZ = minZ + cz * cell
                val cellMaxZ = cellMinZ + cell
                val lz = frameLightPos[base + 2]
                val dzMin = if (lz < cellMinZ) cellMinZ - lz else if (lz > cellMaxZ) lz - cellMaxZ else 0f
                for (cx in x0..x1) {
                    val c = row + cx
                    val slot = cursor[c]++
                    items[slot] = i
                    val d = slot * 4
                    val lx = frameLightPos[base]
                    cellPos[d] = lx
                    cellPos[d + 1] = frameLightPos[base + 1]
                    cellPos[d + 2] = lz
                    cellPos[d + 3] = kq
                    val cellMinX = minX + cx * cell
                    val cellMaxX = cellMinX + cell
                    val dxMin = if (lx < cellMinX) cellMinX - lx else if (lx > cellMaxX) lx - cellMaxX else 0f
                    lightCellRankFloor[slot] = kq * (dxMin * dxMin + dzMin * dzMin)
                }
            }
        }

        val floors = lightCellRankFloor
        for (c in 0 until cells) {
            val from = start[c]
            val to = start[c + 1]
            for (a in from + 1 until to) {
                val keyFloor = floors[a]
                val keyIdx = items[a]
                val k0 = cellPos[a * 4]
                val k1 = cellPos[a * 4 + 1]
                val k2 = cellPos[a * 4 + 2]
                val k3 = cellPos[a * 4 + 3]
                var b = a - 1
                while (b >= from && floors[b] > keyFloor) {
                    floors[b + 1] = floors[b]
                    items[b + 1] = items[b]
                    val src = b * 4
                    val dst = src + 4
                    cellPos[dst] = cellPos[src]
                    cellPos[dst + 1] = cellPos[src + 1]
                    cellPos[dst + 2] = cellPos[src + 2]
                    cellPos[dst + 3] = cellPos[src + 3]
                    b--
                }
                floors[b + 1] = keyFloor
                items[b + 1] = keyIdx
                val dst = (b + 1) * 4
                cellPos[dst] = k0
                cellPos[dst + 1] = k1
                cellPos[dst + 2] = k2
                cellPos[dst + 3] = k3
            }
        }
    }

    private var lightGridCursor = IntArray(0)
    private var lightGridSignature = 0L
    private var lightSelectCalls = 0L
    private var lightCandidatesScanned = 0L
    private var lightGridItemCount = 0

    private fun buildFrameLights() {
        frameLights.clear()
        if (pointLightsEnabled) {
            val scene = Resource.mapRegion?.getScene()
            val occupants = scene?.lightSlotOccupant
            if (scene != null && occupants != null) {
                val count = scene.lightSlot
                for (i in 0 until count) {
                    val light = occupants[i]?.pointLight ?: continue
                    frameLights.add(light)
                }
            }
        }
        diagSceneLights = frameLights.size

        val maxLights = InstancedModelBatcher.MAX_POINT_LIGHTS
        if (frameLights.size > maxLights) {
            val region = Resource.mapRegion
            val anchorX = if (region != null) region.sceneSizeX * 256f else Camera.cameraPositionAbsoluteX.toFloat()
            val anchorZ = if (region != null) region.sceneSizeY * 256f else Camera.cameraPositionAbsoluteY.toFloat()
            frameLights.sortBy { l ->
                val dx = l.getX() - anchorX
                val dz = l.getZ() - anchorZ
                dx * dx + dz * dz
            }
        }
        val n = if (frameLights.size > maxLights) maxLights else frameLights.size
        for (i in 0 until n) {
            val l = frameLights[i]
            val size = l.getSize2d().toFloat()
            val kq = if (size > 0f) 1f / (size * size) else 1f
            frameLightPos[i * 4] = l.getX().toFloat()
            frameLightPos[i * 4 + 1] = l.getY().toFloat()
            frameLightPos[i * 4 + 2] = l.getZ().toFloat()
            frameLightPos[i * 4 + 3] = kq
            val c = l.getColor()
            val a = l.getAlpha() / 255.0f
            frameLightColor[i * 3] = ((c shr 16) and 0xff) * a
            frameLightColor[i * 3 + 1] = ((c shr 8) and 0xff) * a
            frameLightColor[i * 3 + 2] = (c and 0xff) * a
        }
        frameLightCount = n
        if (LIGHT_DIAG && lightDiagBudget > 0 && n > 0) {
            lightDiagBudget--
            val camX = Camera.cameraPositionAbsoluteX.toFloat(); val camY = Camera.cameraPositionAbsoluteZ.toFloat(); val camZ = Camera.cameraPositionAbsoluteY.toFloat()
            val sb = StringBuilder("[LightDiag] frameLights=$n scene=${frameLights.size} cam=($camX,$camY,$camZ)")
            for (i in 0 until minOf(n, 6)) {
                val l = frameLights[i]
                sb.append(" | L$i pos=(${l.getX()},${l.getY()},${l.getZ()}) size2d=${l.getSize2d()} color=${Integer.toHexString(l.getColor())} alpha=${l.getAlpha()} kq=${frameLightPos[i * 4 + 3]} col=(${frameLightColor[i * 3]},${frameLightColor[i * 3 + 1]},${frameLightColor[i * 3 + 2]})")
            }
            System.err.println(sb)
        }
        diagPointLights = n
        lastLightSelectValid = false
        buildLightGrid()
    }

    fun flushAllPendingDraws() {
        if (!canUseModernPipeline()) return
        flushSceneForUiDraw()
        endSpriteBatch()
    }

    fun drawSurfaceMaskedQuad(
        baseHandle: Int, maskHandle: Int,
        x0: Float, y0: Float, b0u: Float, b0v: Float, m0u: Float, m0v: Float,
        x1: Float, y1: Float, b1u: Float, b1v: Float, m1u: Float, m1v: Float,
        x2: Float, y2: Float, b2u: Float, b2v: Float, m2u: Float, m2v: Float,
        x3: Float, y3: Float, b3u: Float, b3v: Float, m3u: Float, m3v: Float,
        baseLinearFilter: Boolean? = null
    ): Boolean {
        val smr = surfaceMaskRenderer ?: return false
        if (!smr.isReady() || !canUseModernPipeline()) return false
        val target = display ?: delegatingDisplay ?: return false

        flushSceneForUiDraw()
        endSpriteBatch()

        smr.drawMaskedQuad(
            baseHandle, maskHandle,
            target.getWidth(), target.getHeight(),
            x0, y0, b0u, b0v, m0u, m0v,
            x1, y1, b1u, b1v, m1u, m1v,
            x2, y2, b2u, b2v, m2u, m2v,
            x3, y3, b3u, b3v, m3u, m3v,
            baseLinearFilter
        )

        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        functionMask = 0
        updateViewportAndCenter()
        return true
    }

    private fun hasPendingSceneGeometry(): Boolean = when {
        useOptimizedBatcher ->
            dbCount > 0 ||
                optimizedBatcher?.hasPendingGeometry() == true ||
                (useInstancing && instancedBatcher?.hasPendingGeometry() == true)
        else -> frameBatcher?.hasPendingGeometry() == true
    }

    private fun armBatchLightingAndFog() {
        val fogOn = fogEnabled && fogDensity >= 0
        val brightness = if (noLight) 1f else sunBrightness
        val red = if (noLight) 1f else sunColorRed
        val green = if (noLight) 1f else sunColorGreen
        val blue = if (noLight) 1f else sunColorBlue
        val contrastPositive = if (noLight) 0f else sunColorIntensity
        val contrastNegative = if (noLight) 0f else antiSunColorIntensity
        optimizedBatcher?.setLighting(
            sunDirection[0], sunDirection[1], sunDirection[2],
            brightness, red, green, blue, contrastPositive, contrastNegative
        )
        optimizedBatcher?.setFog(viewMatrix44.elements, fogOn, fogColor, fogStartPosition, fogEndPosition)
        val textureTime = TextureScroll.seconds(textureAnimatingProgression)
        optimizedBatcher?.setTextureTime(textureTime)
        if (useInstancing) {
            instancedBatcher?.setLighting(
                sunDirection[0], sunDirection[1], sunDirection[2],
                brightness, red, green, blue, contrastPositive, contrastNegative
            )
            instancedBatcher?.setFog(viewMatrix44.elements, fogOn, fogColor, fogStartPosition, fogEndPosition)
            instancedBatcher?.setTextureTime(textureTime)
        }
    }

    private fun flushSceneBeforeLightingChange(): Boolean {
        if (!canUseModernPipeline() || !isSceneBatchActive() || !hasPendingSceneGeometry()) return false
        renderWithModernPipeline()
        functionMask = 0
        return true
    }

    private fun sceneLightingApplied(flushed: Boolean) {
        if (!canUseModernPipeline()) return
        if (flushed) {
            openSceneBatch()
        } else if (isSceneBatchActive()) {
            ensureSceneTarget()
            armBatchLightingAndFog()
        }
    }

    override fun updateSun(sunBrightness: Float) {
        val changed = this.sunBrightness != sunBrightness
        val flushed = changed && flushSceneBeforeLightingChange()
        super.updateSun(sunBrightness)
        if (changed) sceneLightingApplied(flushed)
    }

    override fun applyLighting(sunColor: Int, intensity: Float, antiIntensity: Float, x: Float, y: Float, z: Float) {
        val changed = sunColorRgb != sunColor || sunColorIntensity != intensity || antiSunColorIntensity != antiIntensity ||
            sunPreviousPosition[0] != x || sunPreviousPosition[1] != y || sunPreviousPosition[2] != z
        val flushed = changed && flushSceneBeforeLightingChange()
        super.applyLighting(sunColor, intensity, antiIntensity, x, y, z)
        if (changed) sceneLightingApplied(flushed)
    }

    override fun calculateFog(color: Int, density: Int, offset: Int) {
        val changed = fogColor != color || fogDensity != density || fogOffset != offset
        val flushed = changed && flushSceneBeforeLightingChange()
        super.calculateFog(color, density, offset)
        if (changed) sceneLightingApplied(flushed)
    }

    private fun isSceneBatchActive(): Boolean = when {
        useOptimizedBatcher -> optimizedBatcher?.isActive() == true
        else -> frameBatcher?.isActive() == true
    }

    private fun flushSceneForUiDraw() {
        if (!canUseModernPipeline()) return
        if (!isSceneBatchActive()) return
        if (!hasPendingSceneGeometry()) return
        endSpriteBatch()
        renderWithModernPipeline()
        functionMask = 0
    }

    override fun setDisplay(activeDisplay: DirectDisplay) {
        if (canUseModernPipeline()) {
            flushSceneForUiDraw()
            endSpriteBatch()
            resolveSceneMsaa()
        }
        super.setDisplay(activeDisplay)
    }

    override fun resizeDisplay(directDisplay: DirectDisplay) {
        if (canUseModernPipeline()) {
            flushSceneForUiDraw()
            endSpriteBatch()
        }
        super.resizeDisplay(directDisplay)
        functionMask = 0
        if (modernBloomCapturing && directDisplayCursorPosition < 0) modernBloom?.rebind()
    }

    override fun setupSpriteRendering() {
        flushSceneForUiDraw()
        if (canUseModernPipeline()) endSpriteBatch()
        debugSetupSpriteCalls++
        super.setupSpriteRendering()
    }

    override fun setupTextRendering() {
        flushSceneForUiDraw()
        if (canUseModernPipeline()) endSpriteBatch()
        super.setupTextRendering()
    }

    override fun setupShapeRendering() {
        flushSceneForUiDraw()
        if (canUseModernPipeline()) endSpriteBatch()
        super.setupShapeRendering()
    }

    fun ensureSpriteBatchForFont() {
        ensureSpriteBatch()
    }

    override fun createFontRenderer(
        fontMetrics: FontMetrics?,
        spriteTypes: Array<SpriteType>?,
        isPlain: Boolean
    ): AbstractFontRenderer {
        return ModernFontRenderer(this, fontMetrics!!, spriteTypes!!, isPlain)
    }

    override fun createNativeSprite(pixels: IntArray, stride: Int, srcWidth: Int, width: Int, height: Int, hasAlpha: Boolean): NativeSprite {
        return ModernSpriteGL(this, width, height, pixels, stride, srcWidth)
    }

    override fun createNativeSprite(x: Int, y: Int, width: Int, height: Int, hasAlpha: Boolean): NativeSprite {
        if (canUseModernPipeline()) {
            flushSceneForUiDraw()
            endSpriteBatch()
        }
        return ModernSpriteGL(this, x, y, width, height)
    }

    override fun generateEmptySprite(width: Int, height: Int, hasAlpha: Boolean, mipmapped: Boolean): NativeSprite {
        return ModernSpriteGL(this, width, height, hasAlpha)
    }

    private val worldmapTileMasks = IdentityHashMap<ByteArray, ModernSpriteGL>()

    override fun renderWorldmapTile(startX: Int, startY: Int, width: Int, height: Int, colorArgb: Int, textureEnvColor: Int, pixelData: ByteArray, tileWidth: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr == null || !sr.isReady() || !canUseModernPipeline() || width <= 0 || height <= 0 || tileWidth <= 0) {
            countLegacySkip("renderWorldmapTile")
            return
        }
        val opaque = blendMode != 1
        val underlay = if (opaque) colorArgb or 0xff000000.toInt() else colorArgb
        val overlay = if (opaque) textureEnvColor or 0xff000000.toInt() else textureEnvColor
        ensureSpriteBatch()
        sr.drawFilledRect(startX, startY, width, height, underlay, 1)
        worldmapTileMask(pixelData, tileWidth).drawStretchedMask(startX, startY, width, height, overlay)
    }

    private fun worldmapTileMask(pixelData: ByteArray, tileWidth: Int): ModernSpriteGL {
        worldmapTileMasks[pixelData]?.let { return it }
        if (worldmapTileMasks.size >= 512) worldmapTileMasks.clear()
        val rows = pixelData.size / tileWidth
        val pixels = IntArray(pixelData.size)
        for (i in pixels.indices) {
            pixels[i] = if (pixelData[i].toInt() != 0) -1 else 0x00ffffff
        }
        val sprite = ModernSpriteGL(this, tileWidth, rows, pixels, 0, tileWidth)
        worldmapTileMasks[pixelData] = sprite
        return sprite
    }

    override fun drawFilledRectangle(x: Int, y: Int, width: Int, height: Int, colorArgb: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            sr.drawFilledRect(x, y, width, height, colorArgb, blendMode)
            debugModernDraws++
        } else {
            countLegacySkip("drawFilledRectangle")
        }
    }

    override fun drawRectangleOutline(x: Int, y: Int, width: Int, height: Int, colorArgb: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            sr.drawRectOutline(x, y, width, height, colorArgb, blendMode)
        } else {
            countLegacySkip("drawRectangleOutline")
        }
    }

    override fun drawTransparentHorizontalLine(x: Int, y: Int, width: Int, colorArgb: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            sr.drawHorizontalLine(x, y, width, colorArgb, blendMode)
        } else {
            countLegacySkip("drawTransparentHorizontalLine")
        }
    }

    override fun drawTransparentVerticalLine(x: Int, y: Int, height: Int, colorArgb: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            sr.drawVerticalLine(x, y, height, colorArgb, blendMode)
        } else {
            countLegacySkip("drawTransparentVerticalLine")
        }
    }

    override fun drawTransparentLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            sr.drawLine(startX, startY, endX, endY, colorArgb, blendMode)
        } else {
            countLegacySkip("drawTransparentLine")
        }
    }

    override fun drawThickLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, lineWidth: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            var deltaX = (rightX - leftX).toFloat()
            var deltaY = (bottomY - topY).toFloat()
            if (deltaX == 0f && deltaY == 0f) {
                deltaX = 1f
            } else {
                val invLen = (1.0 / kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
                deltaX *= invLen
                deltaY *= invLen
            }

            val halfWidth = lineWidth * 0.5f
            val perpX = -deltaY * halfWidth
            val perpY = deltaX * halfWidth

            val sx = leftX + 0.35f
            val sy = topY + 0.35f
            val ex = rightX + deltaX + 0.35f
            val ey = bottomY + deltaY + 0.35f

            sr.drawQuad(
                textureId = 0,
                x0 = sx + perpX, y0 = sy + perpY, u0 = 0f, v0 = 0f,
                x1 = sx - perpX, y1 = sy - perpY, u1 = 0f, v1 = 0f,
                x2 = ex - perpX, y2 = ey - perpY, u2 = 0f, v2 = 0f,
                x3 = ex + perpX, y3 = ey + perpY, u3 = 0f, v3 = 0f,
                colorArgb = colorArgb,
                blendMode = blendMode
            )
        } else {
            countLegacySkip("drawThickLine")
        }
    }

    override fun drawLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, lineSpacing: Int, lineLength: Int, offset: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            if (endX == startX && startY == endY) return

            var deltaX = (endX - startX).toFloat()
            var deltaY = (endY - startY).toFloat()
            val invLen = (1.0 / kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
            deltaX *= invLen
            deltaY *= invLen

            var adjustedOffset = offset % (lineLength + lineSpacing)
            val gapDx = deltaX * lineSpacing
            val gapDy = deltaY * lineSpacing
            var skipDx = 0f
            var skipDy = 0f
            var segDx = gapDx
            var segDy = gapDy
            if (adjustedOffset > lineSpacing) {
                skipDx = deltaX * (lineLength + lineSpacing - adjustedOffset)
                skipDy = deltaY * (lineLength + lineSpacing - adjustedOffset)
            } else {
                segDx = deltaX * (lineSpacing - adjustedOffset)
                segDy = deltaY * (lineSpacing - adjustedOffset)
            }

            var cx = startX + 0.35f + skipDx
            var cy = startY + 0.35f + skipDy
            val dashDx = deltaX * lineLength
            val dashDy = deltaY * lineLength

            while (true) {
                if (endX > startX) {
                    if (cx > endX + 0.35f) break
                    if (cx + segDx > endX) segDx = endX - cx
                } else {
                    if (cx < endX + 0.35f) break
                    if (cx + segDx < endX) segDx = endX - cx
                }
                if (endY > startY) {
                    if (cy > endY + 0.35f) break
                    if (cy + segDy > endY) segDy = endY - cy
                } else {
                    if (cy < endY + 0.35f) break
                    if (cy + segDy < endY) segDy = endY - cy
                }

                val perpX = -deltaY * 0.5f
                val perpY = deltaX * 0.5f
                sr.drawQuad(
                    textureId = 0,
                    x0 = cx + perpX, y0 = cy + perpY, u0 = 0f, v0 = 0f,
                    x1 = cx - perpX, y1 = cy - perpY, u1 = 0f, v1 = 0f,
                    x2 = cx + segDx - perpX, y2 = cy + segDy - perpY, u2 = 0f, v2 = 0f,
                    x3 = cx + segDx + perpX, y3 = cy + segDy + perpY, u3 = 0f, v3 = 0f,
                    colorArgb = colorArgb,
                    blendMode = 1
                )

                cx += dashDx + segDx
                cy += dashDy + segDy
                segDx = gapDx
                segDy = gapDy
            }
        } else {
            countLegacySkip("drawLine")
        }
    }

    private var modernScissorEnabled = false

    private var batchClipLeft = 0
    private var batchClipTop = 0
    private var batchClipRight = 0
    private var batchClipBottom = 0

    override fun onTextureDeleted(glTextureId: Int) {
        instancedBatcher?.releaseTextureHandle(glTextureId)
    }

    override fun setScissorTestEnabled(enabled: Boolean) {
        if (enabled != modernScissorEnabled) flushSceneForUiDraw()
        super.setScissorTestEnabled(enabled)
        modernScissorEnabled = enabled
        val sr = spriteRenderer ?: return
        if (enabled) pushScissorBoxToSprites(sr) else sr.clearClipRect()
    }

    override fun defineScissorBox() {
        if (clipLeft != batchClipLeft || clipTop != batchClipTop || clipRight != batchClipRight || clipBottom != batchClipBottom) {
            flushSceneForUiDraw()
            batchClipLeft = clipLeft
            batchClipTop = clipTop
            batchClipRight = clipRight
            batchClipBottom = clipBottom
        }
        super.defineScissorBox()
        val sr = spriteRenderer ?: return
        if (modernScissorEnabled) pushScissorBoxToSprites(sr)
    }

    private fun pushScissorBoxToSprites(sr: ModernSpriteRenderer) {
        val d = display
        if (d != null && clipLeft < clipRight && clipTop < clipBottom) {
            sr.setClipRect(
                viewportOffsetX + clipLeft,
                viewportOffsetY + d.getHeight() - clipBottom,
                clipRight - clipLeft,
                clipBottom - clipTop
            )
        } else {
            sr.setClipRect(0, 0, 0, 0)
        }
    }

    override fun applyClearFlags(functionMask: Int, colorArgb: Int) {
        if (canUseModernPipeline() && isSpriteBatchActive) {
            spriteRenderer?.flush("clear")
        }
        if ((functionMask and 0x2) != 0) {
            OpenGL.glDepthMask(true)
        }
        super.applyClearFlags(functionMask, colorArgb)
        if ((functionMask and 0x2) != 0) {
            setDepthMask()
        }
    }

    override fun clearRectangle(surface: ComponentSurface, offsetX: Int, offsetY: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            val spriteSurface = surface as? SpriteSurfaceGL
            if (spriteSurface != null) {
                val surfaceTexture = spriteSurface.surface
                val textureHandle = surfaceTexture.activeHandle
                if (textureHandle > 0) {
                    ensureSpriteBatch()
                    val texScaleU = surfaceTexture.s / surfaceTexture.singleWidth
                    val texScaleV = surfaceTexture.t / surfaceTexture.singleLength

                    val u0 = texScaleU * (this.clipLeft - offsetX)
                    val v0 = texScaleV * (this.clipTop - offsetY)
                    val u1 = texScaleU * (this.clipRight - offsetX)
                    val v1 = texScaleV * (this.clipBottom - offsetY)

                    sr.drawSprite(
                        textureId = textureHandle,
                        x0 = this.clipLeft.toFloat(), y0 = this.clipTop.toFloat(),
                        x1 = this.clipRight.toFloat(), y1 = this.clipBottom.toFloat(),
                        u0 = u0, v0 = v0,
                        u1 = u1, v1 = v1,
                        colorArgb = 0xFF000000.toInt(),
                        blendMode = 1
                    )
                    return
                }
            }
        }
        countLegacySkip("clearRectangle")
    }

    override fun drawSurfaceLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, dimension: ComponentSurface, clipLeft: Int, clipTop: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            val spriteSurface = dimension as? SpriteSurfaceGL
            if (spriteSurface != null) {
                val surfaceTexture = spriteSurface.surface
                val textureHandle = surfaceTexture.activeHandle
                if (textureHandle > 0) {
                    ensureSpriteBatch()
                    val texScaleU = surfaceTexture.s / surfaceTexture.singleWidth
                    val texScaleV = surfaceTexture.t / surfaceTexture.singleLength

                    var deltaX = (rightX - leftX).toFloat()
                    var deltaY = (bottomY - topY).toFloat()
                    val invLen = (1.0 / kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
                    deltaX *= invLen
                    deltaY *= invLen

                    val u0 = texScaleU * (leftX - clipLeft)
                    val v0 = texScaleV * (topY - clipTop)
                    val u1 = texScaleU * (rightX - clipLeft)
                    val v1 = texScaleV * (bottomY - clipTop)

                    sr.drawTexturedLine(
                        leftX + 0.35f, topY + 0.35f,
                        rightX + deltaX + 0.35f, bottomY + deltaY + 0.35f,
                        textureHandle,
                        u0, v0, u1, v1,
                        colorArgb,
                        blendMode = 1
                    )
                    return
                }
            }
        }
        countLegacySkip("drawSurfaceLine")
    }

    override fun drawTexturedLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: ComponentSurface, clipLeft: Int, clipTop: Int, lineSpacing: Int, lineLength: Int, offset: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            val spriteSurface = surface as? SpriteSurfaceGL
            if (spriteSurface != null && (endX != startX || startY != endY)) {
                val surfaceTexture = spriteSurface.surface
                val textureHandle = surfaceTexture.activeHandle
                if (textureHandle > 0) {
                    ensureSpriteBatch()
                    val texScaleU = surfaceTexture.s / surfaceTexture.singleWidth
                    val texScaleV = surfaceTexture.t / surfaceTexture.singleLength

                    var deltaX = (endX - startX).toFloat()
                    var deltaY = (endY - startY).toFloat()
                    val invLen = (1.0 / kotlin.math.sqrt((deltaX * deltaX + deltaY * deltaY).toDouble())).toFloat()
                    deltaX *= invLen
                    deltaY *= invLen

                    var adjustedOffset = offset % (lineLength + lineSpacing)
                    val gapDx = deltaX * lineSpacing
                    val gapDy = deltaY * lineSpacing
                    var skipDx = 0f
                    var skipDy = 0f
                    var segDx = gapDx
                    var segDy = gapDy
                    if (adjustedOffset > lineSpacing) {
                        skipDx = deltaX * (lineLength + lineSpacing - adjustedOffset)
                        skipDy = deltaY * (lineLength + lineSpacing - adjustedOffset)
                    } else {
                        segDx = deltaX * (lineSpacing - adjustedOffset)
                        segDy = deltaY * (lineSpacing - adjustedOffset)
                    }

                    var cx = startX + 0.35f + skipDx
                    var cy = startY + 0.35f + skipDy
                    val dashDx = deltaX * lineLength
                    val dashDy = deltaY * lineLength

                    while (true) {
                        if (endX > startX) {
                            if (cx > endX + 0.35f) break
                            if (cx + segDx > endX) segDx = endX - cx
                        } else {
                            if (cx < endX + 0.35f) break
                            if (cx + segDx < endX) segDx = endX - cx
                        }
                        if (endY > startY) {
                            if (cy > endY + 0.35f) break
                            if (cy + segDy > endY) segDy = endY - cy
                        } else {
                            if (cy < endY + 0.35f) break
                            if (cy + segDy < endY) segDy = endY - cy
                        }

                        val u0 = texScaleU * (cx - clipLeft)
                        val v0 = texScaleV * (cy - clipTop)
                        val u1 = texScaleU * (cx + segDx - clipLeft)
                        val v1 = texScaleV * (cy + segDy - clipTop)

                        sr.drawTexturedLine(
                            cx, cy,
                            cx + segDx, cy + segDy,
                            textureHandle,
                            u0, v0, u1, v1,
                            colorArgb,
                            blendMode = 1
                        )

                        cx += dashDx + segDx
                        cy += dashDy + segDy
                        segDx = gapDx
                        segDy = gapDy
                    }
                    return
                }
            }
        }
        countLegacySkip("drawTexturedLine")
    }

    override fun renderParticles(particleWrapper: ParticleWrapper) {
        val pr = particleRendererModern
        if (pr != null && pr.isReady() && canUseModernPipeline()) {
            if (isSceneBatchActive()) {
                if (deferredParticles.size == deferredParticleSeq.size) {
                    deferredParticleSeq = deferredParticleSeq.copyOf(deferredParticleSeq.size * 2)
                    deferredParticleKey = deferredParticleKey.copyOf(deferredParticleKey.size * 2)
                    deferredParticleOrder = deferredParticleOrder.copyOf(deferredParticleOrder.size * 2)
                }
                deferredParticleSeq[deferredParticles.size] = translucentSeqCounter++
                deferredParticleKey[deferredParticles.size] = Scene.currentDrawEntity?.floorDepth ?: Int.MIN_VALUE
                deferredParticles.add(particleWrapper)
                return
            }
            endSpriteBatch()
            renderParticlesNow(pr, particleWrapper)
        } else {
            countLegacySkip("renderParticles")
        }
    }

    private var particleVpDiagBudget = 3
    private val sceneVpSnapshot = FloatArray(16)
    private val sceneViewSnapshot = FloatArray(16)
    private val sceneProjSnapshot = FloatArray(16)
    private var sceneSnapshotValid = false
    private fun snapshotSceneMatrices() {
        System.arraycopy(viewProjectionMatrix.elements, 0, sceneVpSnapshot, 0, 16)
        System.arraycopy(viewMatrix44.elements, 0, sceneViewSnapshot, 0, 16)
        System.arraycopy(currentModelMatrix.elements, 0, sceneProjSnapshot, 0, 16)
        sceneSnapshotValid = true
    }
    private fun renderParticlesNow(pr: ModernParticleRenderer, particleWrapper: ParticleWrapper) {
        run {

            val viewMatrixFloats = if (sceneSnapshotValid) sceneViewSnapshot else this.viewMatrix44.elements

            val projectionMatrixFloats = if (sceneSnapshotValid) sceneProjSnapshot else this.currentModelMatrix.elements

            val hasFog = fogEnabled && fogDensity > 0
            val fR = if (hasFog) (fogColor and 0xff0000) / 16711680f else 0f
            val fG = if (hasFog) (fogColor and 0xff00) / 65280f else 0f
            val fB = if (hasFog) (fogColor and 0xff) / 255f else 0f

            val vp = if (sceneSnapshotValid) sceneVpSnapshot else viewProjectionMatrix.elements
            if (PathingEntity.PARTICLE_DIAG && particleVpDiagBudget > 0) {
                particleVpDiagBudget--
                val v = viewMatrixFloats; val p = projectionMatrixFloats
                val composed = FloatArray(16)
                for (row in 0 until 4) for (col in 0 until 4) { var sum = 0f; for (k in 0 until 4) sum += p[row + k * 4] * v[k + col * 4]; composed[row + col * 4] = sum }
                System.err.println("[ParticleVp] tracked13=${vp[13]} tracked5=${vp[5]} batcher13=${-(instancedBatcher?.vpElement(13) ?: Float.NaN)} batcher5=${-(instancedBatcher?.vpElement(5) ?: Float.NaN)} batcher0=${instancedBatcher?.vpElement(0)} tracked0=${vp[0]} viewportOriginY=$viewportOriginY centerY=$imageProducerCenterY")
            }
            pr.renderParticles(
                particleWrapper,
                viewMatrixFloats,
                projectionMatrixFloats,
                ::lookupTextureHandle,
                hasFog,
                fogStartPosition,
                fogEndPosition,
                fR, fG, fB,
                vp,
                ::lookupCombineMode,
                ::lookupTextureScroll,
                TextureScroll.seconds(textureAnimatingProgression),
                sunBrightness,
                sunColorRed,
                sunColorGreen,
                sunColorBlue
            )

            functionMask = 0
        }
    }

    private fun updateCameraFromClient() {
        try {
            val mapRegion = Resource.mapRegion
            val scene = mapRegion?.getScene()

            if (scene != null) {
                camX = scene.cameraPositionX.toFloat()
                camY = scene.cameraPositionY.toFloat()
                camZ = scene.cameraPositionZ.toFloat()

                val cameraObj = Camera

                camPitch = (cameraObj.camAngleX / 4096f) * Math.PI.toFloat() * 0.5f
                camYaw = (cameraObj.camAngleY / 16384f) * Math.PI.toFloat() * 2f
            }
        } catch (e: Exception) {
        }
    }

    private val forceModulate = System.getProperty("render.combine")?.equals("modulate", true) == true

    private fun lookupCombineMode(materialId: Int): Int {
        if (forceModulate) return 0
        if (materialId < 0) return 0
        return materialProvider?.getMaterial(materialId)?.combineMode?.toInt() ?: 0
    }

    private fun lookupTextureScroll(materialId: Int): Int {
        if (materialId < 0) return TextureScroll.NONE
        val material = materialProvider?.getMaterial(materialId) ?: return TextureScroll.NONE
        return TextureScroll.pack(material)
    }

    private fun materialNeedsBlend(materialId: Int): Boolean {
        if (materialId < 0) return false
        val material = materialProvider?.getMaterial(materialId) ?: return false
        return material.effectCombiner == 2 || !MaterialTypeList.isOpaqueEffect(material.effectId.toInt())
    }

    private fun dumpTextureAlpha(materialId: Int, handle: Int) {
        try {
            GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, handle)
            val w = org.lwjgl.opengl.GL33.glGetTexLevelParameteri(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL33.GL_TEXTURE_WIDTH)
            val h = org.lwjgl.opengl.GL33.glGetTexLevelParameteri(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL33.GL_TEXTURE_HEIGHT)
            if (w <= 0 || h <= 0 || w * h > 4 * 1024 * 1024) return
            val buf = MemoryUtil.memAlloc(w * h * 4)
            org.lwjgl.opengl.GL33.glPixelStorei(org.lwjgl.opengl.GL33.GL_PACK_ALIGNMENT, 1)
            org.lwjgl.opengl.GL33.glGetTexImage(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, 0,
                org.lwjgl.opengl.GL33.GL_RGBA, org.lwjgl.opengl.GL33.GL_UNSIGNED_BYTE, buf)
            var sum = 0L; var partial = 0; var zero = 0; var full = 0
            val n = w * h
            for (i in 0 until n) {
                val a = buf.get(i * 4 + 3).toInt() and 0xff
                sum += a
                when {
                    a == 0 -> zero++
                    a == 255 -> full++
                    else -> partial++
                }
            }
            MemoryUtil.memFree(buf)
            val mean = sum.toDouble() / n
            if (partial > 0 || zero > 0) {
                val material = materialProvider?.getMaterial(materialId)
                System.err.println("[TexAlpha] mat=$materialId ${w}x$h meanA=${"%.1f".format(mean)} " +
                    "zero=${zero * 100 / n}% partial=${partial * 100 / n}% full=${full * 100 / n}% " +
                    "combine=${material?.combineMode} effectId=${material?.effectId} effectCombiner=${material?.effectCombiner} " +
                    "hasAlpha=${materialTextureHasAlpha(materialId)}")
            }
        } catch (t: Throwable) {
            System.err.println("[TexAlpha] mat=$materialId failed: $t")
        }
    }

    fun materialTextureHasAlpha(materialId: Int): Boolean {
        if (materialId < 0) return false
        val material = materialProvider?.getMaterial(materialId) ?: return false
        return MaterialTypeList.isOpaqueEffect(material.effectId.toInt())
    }

    private fun lookupTextureHandle(materialId: Int): Int {
        if (materialId < 0) return 0

        textureHandleCache[materialId]?.let {
            if (materialId >= textureTouchFrame.size) textureTouchFrame = textureTouchFrame.copyOf(maxOf(materialId + 1, textureTouchFrame.size * 2, 256))
            if (textureTouchFrame[materialId] != frameCounter + 1) {
                textureTouchFrame[materialId] = frameCounter + 1
                textureCache?.textureCache?.get(materialId.toLong())
            }
            return it
        }

        val texture = textureCache?.getTexture(materialId)
        val handle = texture?.activeHandle ?: 0

        if (handle != 0) {
            textureHandleCache[materialId] = handle
            if (TEX_ALPHA_DIAG) dumpTextureAlpha(materialId, handle)
        }
        if (handle == 0 && diagBudget > 0 && diagGreenSeen.add(materialId)) {
            diagBudget--
            val cached = try { materialProvider?.isCached(materialId).toString() } catch (e: Exception) { "ERR:${e.message}" }
            System.err.println("[GreenDiag] mat=$materialId texNull=${texture == null} handle=$handle isCached=$cached")
        }
        return handle
    }

    override fun createModel(mesh: Mesh, modelFunctionMask: Int, preferenceMask: Int, ambient: Int, contrast: Int): Model {
        val built = try {
            ModernModelBuilder.build(mesh, ambient, materialProvider, (preferenceMask and 0x40) != 0, contrast)
        } catch (e: Exception) {
            null
        }

        val model = super.createModel(mesh, modelFunctionMask, preferenceMask, ambient, contrast)
        if (model is ModelGL) {
            model.modernBuilt = built
            model.modernYaw = 0
        }
        return model
    }

    fun renderMeshModern(mesh: Mesh, x: Float, y: Float, z: Float, rotationY: Float = 0f) {
        val mr = modernRenderer ?: return

        val converted = MeshConverter.convert(mesh) ?: return

        try {
            modelMat.identity()
            modelMat.translate(x, y, z)
            modelMat.rotateY(rotationY)
            mr.setModelMatrix(modelMat)

            mr.drawIndexed(converted.vertexData, converted.indexData, converted.indexCount)
        } finally {
            converted.free()
        }
    }

    private val reusableNdcPositions = FloatArray(9)
    private val reusableColors = ByteArray(12)
    private val reusableNormals = FloatArray(9)
    private var reusableNormalToVertex = IntArray(4096)

    private var phaseModelNs = 0L
    private var phaseModelCalls = 0
    private var phaseGroundNs = 0L
    private var phaseGroundCalls = 0
    private var phaseLightSelectNs = 0L
    private var phaseGpuWaitNs = 0L

    private val gpuTimer = GpuTimer(arrayOf("inst", "stream", "ground", "sprite"))
    private val GPU_PASS_INSTANCED = 0
    private val GPU_PASS_STREAM = 1
    private val GPU_PASS_GROUND = 2
    private val GPU_PASS_SPRITE = 3

    private val GPU_SYNC = System.getProperty("render.gpusync") != null

    private val PHASE_TIMERS = System.getProperty("render.phasetimers") != null

    private fun stageNow(): Long = if (PHASE_TIMERS) System.nanoTime() else 0L

    private val EMPTY_VERTEX_DATA: java.nio.ByteBuffer =
        java.nio.ByteBuffer.allocate(0).order(java.nio.ByteOrder.nativeOrder())
    private var matGroupCursor = IntArray(64)

    private fun colourStateHash(model: ModelGL): Long {
        val facesColor = model.facesColor ?: return 0L
        val facesAlpha = model.facesAlpha
        var h = 1125899906842597L
        h = h * 31 + model.ambient
        val n = model.numFaces
        for (i in 0 until minOf(n, facesColor.size)) {
            h = h * 31 + facesColor[i]
            if (facesAlpha != null && i < facesAlpha.size) h = h * 31 + facesAlpha[i]
        }
        return h
    }

    private fun computeModelContentHash(model: ModelGL): Long {
        if (model.modernContentValid) {
            if (VERIFY_HASH) {
                val fresh = computeModelContentHashUncached(model)
                if (fresh != model.modernContentHash) {
                    hashVerifyBad++
                    if (hashVerifyBad <= 20) System.err.println(
                        "[HashVerify] STALE memoised hash: cached=${model.modernContentHash} fresh=$fresh " +
                            "faces=${model.numFaces} verts=${model.maxDepth}")
                }
                hashVerifyChecks++
            }
            return model.modernContentHash
        }
        val hash = computeModelContentHashUncached(model)
        model.modernContentHash = hash
        model.modernContentValid = true
        return hash
    }

    private fun computeModelContentHashUncached(model: ModelGL): Long {
        var hash = 17L
        hash = hash * 31 + model.numFaces
        hash = hash * 31 + model.maxDepth
        hash = hash * 31 + model.numNormals

        val verticesX = model.verticesX
        val verticesY = model.verticesY
        val verticesZ = model.verticesZ
        val maxDepth = model.maxDepth
        if (verticesX != null && verticesY != null && verticesZ != null && maxDepth > 0) {
            var xAcc = 0
            var yAcc = 0
            var zAcc = 0
            for (i in 0 until maxDepth) {
                xAcc = xAcc * 31 + verticesX[i]
                yAcc = yAcc * 31 + verticesY[i]
                zAcc = zAcc * 31 + verticesZ[i]
            }
            hash = hash * 31 + xAcc
            hash = hash * 31 + yAcc
            hash = hash * 31 + zAcc
        }

        val facesA = model.facesA
        val facesB = model.facesB
        val facesC = model.facesC
        if (facesA != null && facesB != null && facesC != null && model.numFaces > 0) {
            hash = hash * 31 + facesA[0].toInt()
            hash = hash * 31 + facesB[0].toInt()
            hash = hash * 31 + facesC[0].toInt()
            if (model.numFaces > 2) {
                val mid = model.numFaces / 2
                hash = hash * 31 + facesA[mid].toInt()
                hash = hash * 31 + facesB[mid].toInt()
                hash = hash * 31 + facesC[mid].toInt()
            }
            if (model.numFaces > 1) {
                val last = model.numFaces - 1
                hash = hash * 31 + facesA[last].toInt()
                hash = hash * 31 + facesB[last].toInt()
                hash = hash * 31 + facesC[last].toInt()
            }
        }

        val facesMaterial = model.facesMaterial
        if (facesMaterial != null && facesMaterial.isNotEmpty()) {
            var mAcc = 0
            for (i in facesMaterial.indices) {
                mAcc = mAcc * 31 + facesMaterial[i]
            }
            hash = hash * 31 + mAcc
        }

        val facesColor = model.facesColor
        if (facesColor != null && facesColor.isNotEmpty()) {
            var cAcc = 0
            for (i in facesColor.indices) {
                cAcc = cAcc * 31 + facesColor[i]
            }
            hash = hash * 31 + cAcc
        }

        val facesAlpha = model.facesAlpha
        if (facesAlpha != null && facesAlpha.isNotEmpty()) {
            var aAcc = 0
            for (i in facesAlpha.indices) {
                aAcc = aAcc * 31 + facesAlpha[i]
            }
            hash = hash * 31 + aAcc
        }

        val textureU = model.textureU
        val textureV = model.textureV
        if (textureU != null && textureV != null) {
            var uvAcc = 0
            val n = minOf(textureU.size, textureV.size)
            for (i in 0 until n) {
                uvAcc = uvAcc * 31 + textureU[i].toRawBits()
                uvAcc = uvAcc * 31 + textureV[i].toRawBits()
            }
            hash = hash * 31 + uvAcc
        }

        hash = hash * 31 + model.ambient.toInt()
        hash = hash * 31 + model.contrast.toInt()

        return hash
    }

    fun renderModelGLModern(model: ModelGL, modelMatrix: Matrix43) {
        diagModelGateCalls++
        if (!canUseModernPipeline()) {
            countLegacySkip("ModelGL(pipeline-rejected)")
            return
        }
        diagModelGateAccepted++

        if (!PHASE_TIMERS) {
            if (useOptimizedBatcher) {
                renderModelGLOptimized(model, modelMatrix)
            } else {
                renderModelGLLegacy(model, modelMatrix)
            }
            return
        }
        val modelStartNs = System.nanoTime()
        if (useOptimizedBatcher) {
            renderModelGLOptimized(model, modelMatrix)
        } else {
            renderModelGLLegacy(model, modelMatrix)
        }
        phaseModelNs += System.nanoTime() - modelStartNs
        phaseModelCalls++
    }

    private var debugModelAttempts = 0
    private var debugModelSuccesses = 0
    private var debugFaceAttempts = 0
    private var debugFaceSuccesses = 0
    private var debugLastLogTime = 0L
    private var modelsCulled = 0L
    private var cacheHits = 0L
    private var cacheMisses = 0L

    private var timeFrustumCull = 0L
    private var timeBeginModel = 0L
    private var timeComputeHash = 0L
    private var timeCacheLookup = 0L
    private var timeAddCachedModel = 0L

    private fun renderModelGLOptimized(model: ModelGL, modelMatrix: Matrix43) {
        val batcher = optimizedBatcher ?: return
        if (!batcher.isActive()) {
            diagLazyBatchOpens++
            openSceneBatch()
            if (!batcher.isActive()) {
                lastBatcherStatus = "not_active"
                countLegacySkip("ModelGL(dropped-no-batch)")
                return
            }
        }

        lastBatcherStatus = "active"
        debugModelAttempts++
        totalModelsProcessed++

        val cullStartNs = stageNow()
        captureLegacyMatrix = modelMatrix
        if (captureList == null && frustumCullEnabled && model.boundsCalculated && isPerspectiveProjection()) {
            val vp = viewProjectionMatrix.elements
            if (!isModelVisible(model, modelMatrix, vp)) {
                if (VERIFY_MODEL_CULL) verifyModelCull(model, modelMatrix, vp)
                modelsCulled++
                if (PHASE_TIMERS) timeFrustumCull += System.nanoTime() - cullStartNs
                return
            }
        }
        if (PHASE_TIMERS) timeFrustumCull += System.nanoTime() - cullStartNs
        if (captureList == null && model.billboardCount > 0 && isSceneBatchActive()) {
            deferBillboards(model, modelMatrix, Scene.currentDrawEntity?.floorDepth ?: 0)
        }

        val numFaces = model.renderedFaceCount
        if (numFaces <= 0) return
        if (verifyLiveGeometry) GeometryDiff.recordContrast(model.contrast.toInt())

        val isAnimated = model.animatedPoseSnapshot ||
            model.vertexGroups != null ||
            ModelFunctionMaskGL.altersVertices(model.functionMask)
        val cache = if (isAnimated) null else modelCache

        val cacheStartNs = stageNow()
        if (cache != null) {
            val built = if (meshBuiltPathEnabled) model.modernBuilt else null
            if (built != null) {
                if (diagBudget > 0 && built.batches.size > 1 && diagModelSeen.add(built.id)) {
                    diagBudget--
                    val split = built.batches.joinToString(",") { "${it.materialId}:${it.indices.size / 3}" }
                    System.err.println("[PostDiag] meshBuilt id=${built.id} faces=${built.faceCount} yaw=${model.modernYaw} batches[mat:faces]=$split")
                }
                if (verifyLiveGeometry) GeometryDiff.verifyLive(model, built)
                val colourKey = if (model.modernColorRevision == 0) 0L else colourStateHash(model)
                val geometryKey =
                    if (meshKeyByCurrentVertices) computeModelContentHash(model)
                    else built.id * 1000003L + colourKey
                if (VERIFY_RECOLOUR && model.modernColorRevision > 0) {
                    verifyRecolourVsLegacyCapture(model, built)
                }
                val cachedModel = sharedGeometryFor(model, cache, geometryKey) {
                    if (model.modernColorRevision == 0) {
                        built.toCachedModel()
                    } else {
                        built.toCachedModel(
                            ModernModelBuilder.recolour(
                                built, model.facesColor, model.facesAlpha, model.facesMaterial,
                                model.ambient.toInt(), ColorUtil.hslToRgb!!, materialProvider,
                                model.meshFaceIndices
                            )
                        )
                    }
                }
                if (cachedModel != null) {
                    if (PHASE_TIMERS) timeCacheLookup += System.nanoTime() - cacheStartNs
                    cacheHits++
                    modernBuiltPathModels++
                    submitCachedModel(
                        batcher, cachedModel, geometryKey,
                        composeModernModelMatrix(model, modelMatrix), model
                    )
                    return
                }
            }

            modernBuiltFallbackModels++
            val hashStartNs = stageNow()
            val modelId = computeModelContentHash(model)
            if (PHASE_TIMERS) timeComputeHash += System.nanoTime() - hashStartNs

            val cachedModel = sharedGeometryFor(model, cache, modelId) {
                buildModelCache(model)
            }
            if (PHASE_TIMERS) timeCacheLookup += System.nanoTime() - cacheStartNs

            if (cachedModel != null) {
                cacheHits++
                if (VERIFY_RECOLOUR) verifyRecolourAgainst(model, cachedModel)
                submitCachedModel(batcher, cachedModel, modelId, modelMatrix, model)
                return
            }
        } else {
            modernBuiltFallbackModels++
            if (PHASE_TIMERS) timeCacheLookup += System.nanoTime() - cacheStartNs

            if (captureList != null) {
                captureUnsupported = true
                return
            }
            if (PARALLEL_BUILD && !model.modernStreamValid && model.privatePoseCopy && animatedInstancing) {
                val inst = instancedBatcher
                if (inst != null && inst.isActive()) {
                    selectLightsForModel(model, modelMatrix)
                    val t = Scene.alphaPassActive
                    val seq = if (t) translucentSeqCounter++ else 0
                    val key = if (t) (Scene.currentDrawEntity?.floorDepth ?: 0) else 0
                    enqueueDeferredBuild(model, modelMatrix, t, depthBufferingEnabled, seq, key)
                    modelCounter++
                    debugModelSuccesses++
                    return
                }
            }
            val streamed = animatedGeometryFor(model)
            if (streamed != null) {
                val rebuilt = animatedGeometryRebuilt
                if (animatedInstancing) {
                    val inst = instancedBatcher
                    if (inst != null && inst.isActive()) {
                        selectLightsForModel(model, modelMatrix)
                        val animTranslucent = Scene.alphaPassActive
                        val animSeq = if (animTranslucent) translucentSeqCounter else 0
                        val animKey = if (animTranslucent) (Scene.currentDrawEntity?.floorDepth ?: 0) else 0
                        val animAdded = inst.addInstance(
                            streamed, model.modernStreamKey, modelMatrix, currentLightIdx, rebuilt,
                            translucent = animTranslucent, depthWrite = depthBufferingEnabled, seq = animSeq,
                            depthKey = animKey
                        )
                        if (TRANS_DIAG && animTranslucent && transDiagTick in 120..121) {
                            System.err.println("[TransAnim] ${Scene.currentDrawEntity?.javaClass?.simpleName} added=$animAdded key=$animKey " +
                                "streamKey=${model.modernStreamKey} faces=${streamed.faceCount} rebuilt=$rebuilt origin=(${modelMatrix.xD},${modelMatrix.yD},${modelMatrix.zD})")
                        }
                        if (animAdded) {
                            if (animTranslucent) translucentSeqCounter++
                            totalFacesProcessed += streamed.faceCount
                            modelCounter++
                            debugModelSuccesses++
                            return
                        }
                    }
                }
                submitStreamedModel(batcher, streamed, modelMatrix)
                return
            }
        }

        if (captureList != null) {
            captureUnsupported = true
            return
        }
        selectLightsFor(modelMatrix.xD, modelMatrix.yD, modelMatrix.zD)
        val beginModelStartNs = stageNow()
        var modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
        if (PHASE_TIMERS) timeBeginModel += System.nanoTime() - beginModelStartNs

        if (modelIndex < 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
            if (modelIndex < 0) return
        }

        modelCounter++
        debugModelSuccesses++
        cacheMisses++
        renderModelGLDirect(model, modelMatrix, batcher, modelIndex)
    }

    private fun selectLightsForModel(model: ModelGL, modelMatrix: Matrix43) {
        if (Scene.currentDrawEntity == null) {
            currentLightIdx[0] = -1f; currentLightIdx[1] = -1f
            currentLightIdx[2] = -1f; currentLightIdx[3] = -1f
            return
        }
        if (model.modernLightEpoch == lightEpoch &&
            model.modernLightX == modelMatrix.xD &&
            model.modernLightY == modelMatrix.yD &&
            model.modernLightZ == modelMatrix.zD
        ) {
            val memo = model.modernLightIdx
            currentLightIdx[0] = memo[0]; currentLightIdx[1] = memo[1]
            currentLightIdx[2] = memo[2]; currentLightIdx[3] = memo[3]
            return
        }
        selectLightsFor(modelMatrix.xD, modelMatrix.yD, modelMatrix.zD)
        model.modernLightEpoch = lightEpoch
        model.modernLightX = modelMatrix.xD
        model.modernLightY = modelMatrix.yD
        model.modernLightZ = modelMatrix.zD
        val memo = model.modernLightIdx
        memo[0] = currentLightIdx[0]; memo[1] = currentLightIdx[1]
        memo[2] = currentLightIdx[2]; memo[3] = currentLightIdx[3]
    }

    private var lightEpoch = 0

    private fun submitCachedModel(
        batcher: OptimizedFrameBatcher,
        cachedModel: ModelCache.CachedModel,
        geometryKey: Long,
        modelMatrix: Matrix43,
        model: ModelGL
    ) {
        selectLightsForModel(model, modelMatrix)
        val capture = captureList
        if (capture != null) {
            capture.add(RetainedStaticScene.Capture(cachedModel, geometryKey, Matrix43().also { it.copyFrom(modelMatrix) },
                Matrix43().also { it.copyFrom(captureLegacyMatrix ?: modelMatrix) }, model))
            return
        }
        val translucent = Scene.alphaPassActive
        val depthWrite = depthBufferingEnabled
        val seq = if (translucent) translucentSeqCounter++ else 0
        val depthKey = if (translucent) (Scene.currentDrawEntity?.floorDepth ?: 0) else 0
        submitResolved(batcher, cachedModel, geometryKey, modelMatrix, translucent, depthWrite, seq, depthKey)
    }

    private fun submitResolved(
        batcher: OptimizedFrameBatcher,
        cachedModel: ModelCache.CachedModel,
        geometryKey: Long,
        modelMatrix: Matrix43,
        translucent: Boolean,
        depthWrite: Boolean,
        seq: Int,
        depthKey: Int
    ) {
        if (TRANS_DIAG && translucent) {
            if (depthKey < transDiagMinKey) transDiagMinKey = depthKey
            if (depthKey > transDiagMaxKey) transDiagMaxKey = depthKey
            val e = Scene.currentDrawEntity
            if (e != null && transDiagTick in 120..121) {
                val scene = Resource.mapRegion?.getScene()
                val abs = e.getAbsolutePosition().coords
                if (scene != null && Math.abs((abs.x.toInt() shr scene.tileUnits) - scene.cameraGridPositionX) <= 6 &&
                    Math.abs((abs.z.toInt() shr scene.tileUnits) - scene.cameraGridPositionY) <= 6
                ) {
                    System.err.println("[TransSubmit] ${e.javaClass.simpleName} key=$depthKey seq=$seq faces=${cachedModel.faceCount} " +
                        "batches=${cachedModel.materialBatches.size} mats=${cachedModel.materialBatches.joinToString(",") { it.materialId.toString() }} " +
                        "origin=(${modelMatrix.xD},${modelMatrix.yD},${modelMatrix.zD}) instActive=${instancedBatcher?.isActive()}")
                }
            }
        }
        if (instancingEnabled && useInstancing && instancedBatcher?.isActive() == true && !(translucent && FORCE_STREAM_TRANSLUCENT)) {
            val addInstanceStartNs = stageNow()
            val added = instancedBatcher!!.addInstance(
                cachedModel, geometryKey, modelMatrix, currentLightIdx,
                translucent = translucent, depthWrite = depthWrite, seq = seq, depthKey = depthKey
            )
            if (TRANS_DIAG && translucent && added) transDiagInst++
            if (PHASE_TIMERS) timeAddCachedModel += System.nanoTime() - addInstanceStartNs
            if (added) {
                totalFacesProcessed += cachedModel.faceCount
                modelCounter++
                debugModelSuccesses++
                return
            }
        }

        val beginModelStartNs = stageNow()
        var modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
        if (PHASE_TIMERS) timeBeginModel += System.nanoTime() - beginModelStartNs

        if (modelIndex < 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
            if (modelIndex < 0) return
        }

        modelCounter++
        debugModelSuccesses++

        val addCachedModelStartNs = stageNow()
        var facesAdded = batcher.addCachedModel(cachedModel, modelIndex, translucent, depthWrite, seq, depthKey)
        if (TRANS_DIAG && translucent && facesAdded > 0) transDiagStream++

        if (facesAdded == 0 && cachedModel.faceCount > 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
            if (modelIndex >= 0) {
                facesAdded = batcher.addCachedModel(cachedModel, modelIndex, translucent, depthWrite, seq, depthKey)
            }
            if (facesAdded == 0) streamingModelsDropped++
        }
        if (PHASE_TIMERS) timeAddCachedModel += System.nanoTime() - addCachedModelStartNs
        totalFacesProcessed += facesAdded
    }

    private fun verifyRecolourAgainst(model: ModelGL, cached: ModelCache.CachedModel) {
        if (!VERIFY_RECOLOUR || recolourChecks >= 60) return
        val built = model.modernBuiltShadow ?: return
        recolourShadowSeen++
        if (model.modernColorRevision == 0) { recolourRev0++; return }
        val map = model.meshFaceIndices ?: run { recolourNoMap++; return }
        recolourChecks++
        val redone = ModernModelBuilder.recolour(
            built, model.facesColor, model.facesAlpha, model.facesMaterial,
            model.ambient.toInt(), ColorUtil.hslToRgb!!, materialProvider, map
        )
        var maxMesh = 0
        for (v in map) if (v > maxMesh) maxMesh = v
        val meshToModel = IntArray(maxMesh + 1) { -1 }
        for (m in map.indices) meshToModel[map[m]] = m
        val stride = ModelCache.BYTES_PER_VERTEX
        val off = ModernModelBuilder.COLOR_OFFSET
        var bad = 0
        var firstMsg: String? = null
        for (i in built.acceptedFaces.indices) {
            val meshFace = built.acceptedFaces[i]
            if (meshFace >= meshToModel.size) continue
            val modelFace = meshToModel[meshFace]
            if (modelFace < 0 || modelFace >= cached.faceCount) continue
            val rp = i * 3 * stride + off
            val cp = modelFace * 3 * stride + off
            if (rp + 3 >= redone.size || cp + 3 >= cached.vertexData.limit()) continue
            for (k in 0 until 3) {
                if (redone[rp + k] != cached.vertexData.get(cp + k)) {
                    bad++
                    if (firstMsg == null) firstMsg =
                        "recolour=(${redone[rp].toInt() and 0xff},${redone[rp+1].toInt() and 0xff},${redone[rp+2].toInt() and 0xff}) " +
                        "expected=(${cached.vertexData.get(cp).toInt() and 0xff},${cached.vertexData.get(cp+1).toInt() and 0xff},${cached.vertexData.get(cp+2).toInt() and 0xff})"
                    break
                }
            }
        }
        if (bad > 0) {
            recolourBad++
            if (recolourBad <= 6) System.err.println(
                "[RecolourMut] MISMATCH $bad/${built.acceptedFaces.size} faces rev=${model.modernColorRevision} $firstMsg")
        }
    }

    private fun verifyRecolourVsLegacyCapture(model: ModelGL, built: ModernModelBuilder.BuiltModel) {
        if (recolourChecks >= 40) return
        val map = model.meshFaceIndices ?: run { recolourNoMap++; return }
        val reference = buildModelCache(model) ?: return
        recolourChecks++
        val redone = ModernModelBuilder.recolour(
            built, model.facesColor, model.facesAlpha, model.facesMaterial,
            model.ambient.toInt(), ColorUtil.hslToRgb!!, materialProvider, map
        )
        var maxMesh = 0
        for (v in map) if (v > maxMesh) maxMesh = v
        val meshToModel = IntArray(maxMesh + 1) { -1 }
        for (m in map.indices) meshToModel[map[m]] = m
        val stride = ModelCache.BYTES_PER_VERTEX
        val off = ModernModelBuilder.COLOR_OFFSET
        var bad = 0
        var firstMsg: String? = null
        for (i in built.acceptedFaces.indices) {
            val meshFace = built.acceptedFaces[i]
            if (meshFace >= meshToModel.size) continue
            val modelFace = meshToModel[meshFace]
            if (modelFace < 0 || modelFace >= reference.faceCount) continue
            val rp = i * 3 * stride + off
            val cp = modelFace * 3 * stride + off
            if (rp + 3 >= redone.size || cp + 3 >= reference.vertexData.limit()) continue
            var diff = false
            for (k in 0 until 3) if (redone[rp + k] != reference.vertexData.get(cp + k)) diff = true
            if (diff) {
                bad++
                if (firstMsg == null) firstMsg =
                    "got=(${redone[rp].toInt() and 0xff},${redone[rp+1].toInt() and 0xff},${redone[rp+2].toInt() and 0xff}) " +
                    "want=(${reference.vertexData.get(cp).toInt() and 0xff},${reference.vertexData.get(cp+1).toInt() and 0xff},${reference.vertexData.get(cp+2).toInt() and 0xff})"
            }
        }
        reference.free()
        if (bad > 0) {
            recolourBad++
            if (recolourBad <= 6) System.err.println(
                "[RecolourMut] $bad/${built.acceptedFaces.size} faces rev=${model.modernColorRevision} $firstMsg")
        }
    }

    private class BuildScratch {
        var reusableSlotColor = IntArray(4096)
        var reusableSlotStamp = IntArray(4096)
        var slotStampCounter = 0
        var normalBuildStamp = 0
        var normalCache = IntArray(4096 * 3)
        var normalCacheStamp = IntArray(4096)
        var vertexWordScratch = IntArray(0)
        var matGroupIds = IntArray(64)
        var matGroupCount = IntArray(64)
        var normalToVertexScratch = IntArray(0)
        var resolvedVertexScratch = IntArray(0)
        var faceRgbaScratch = IntArray(0)
        var slotRemapScratch = IntArray(0)
        var slotRemapStamp = IntArray(0)
        var slotRemapCounter = 0
        var faceIndexScratch = IntArray(0)
        var unwrittenSlotFaces = 0L
    }
    private val mainScratch = BuildScratch()
    private val threadScratch: ThreadLocal<BuildScratch> = ThreadLocal.withInitial { BuildScratch() }

    private fun buildModelCache(
        model: ModelGL,
        modelOwned: Boolean = false,
        reuse: ModelCache.CachedModel? = null,
        sc: BuildScratch = mainScratch
    ): ModelCache.CachedModel? {
        val numFaces = model.renderedFaceCount
        if (numFaces <= 0) return null

        val facesA = model.facesA ?: return null
        val facesB = model.facesB ?: return null
        val facesC = model.facesC ?: return null
        val verticesX = model.verticesX ?: return null
        val verticesY = model.verticesY ?: return null
        val verticesZ = model.verticesZ ?: return null
        val maxDepth = model.maxDepth
        val numNormals = model.numNormals

        val triangles = model.triangles
        val normalMapping = model.vertexNormalIndices
        val textureU = model.textureU
        val textureV = model.textureV
        val facesColor = model.facesColor
        val facesMaterial = model.facesMaterial
        val facesAlpha = model.facesAlpha
        val modelAmbient = model.ambient

        val merged = model.mergedNormals
        val normalsX = merged?.normalsX ?: model.normalsX
        val normalsY = merged?.normalsY ?: model.normalsY
        val normalsZ = merged?.normalsZ ?: model.normalsZ
        val magnitudes = merged?.magnitude ?: model.magnitudes
        val hasNormals = normalsX != null && normalsY != null && normalsZ != null && magnitudes != null

        val useDirectVertexIndices = (triangles == null || normalMapping == null)
        val normalToVertex: IntArray?
        if (!useDirectVertexIndices && triangles != null && normalMapping != null) {
            if (sc.normalToVertexScratch.size < numNormals) sc.normalToVertexScratch = IntArray(numNormals * 2)
            normalToVertex = sc.normalToVertexScratch
            java.util.Arrays.fill(normalToVertex, 0, numNormals, -1)
            for (vertIdx in 0 until maxDepth) {
                val triStart = triangles[vertIdx]
                val triEnd = triangles[vertIdx + 1]
                for (i in triStart until triEnd) {
                    val normalIdx = normalMapping[i].toInt() - 1
                    if (normalIdx == -1) break
                    if (normalIdx >= 0 && normalIdx < numNormals) {
                        normalToVertex[normalIdx] = vertIdx
                    }
                }
            }
        } else {
            normalToVertex = null
        }

        if (sc.resolvedVertexScratch.size < numFaces * 3) sc.resolvedVertexScratch = IntArray(numFaces * 6)
        val resolved = sc.resolvedVertexScratch
        var validFaceCount = 0
        for (faceIdx in 0 until numFaces) {
            val idxA = facesA[faceIdx].toInt() and 0xFFFF
            val idxB = facesB[faceIdx].toInt() and 0xFFFF
            val idxC = facesC[faceIdx].toInt() and 0xFFFF

            val vertA: Int
            val vertB: Int
            val vertC: Int
            if (useDirectVertexIndices) {
                vertA = idxA; vertB = idxB; vertC = idxC
            } else {
                vertA = if (idxA < numNormals && normalToVertex != null) normalToVertex[idxA] else -1
                vertB = if (idxB < numNormals && normalToVertex != null) normalToVertex[idxB] else -1
                vertC = if (idxC < numNormals && normalToVertex != null) normalToVertex[idxC] else -1
            }

            val r = faceIdx * 3
            if (vertA >= 0 && vertB >= 0 && vertC >= 0 &&
                vertA < maxDepth && vertB < maxDepth && vertC < maxDepth) {
                validFaceCount++
                resolved[r] = vertA; resolved[r + 1] = vertB; resolved[r + 2] = vertC
            } else {
                resolved[r] = -1
            }
        }

        val droppedFaces = numFaces - validFaceCount
        if (droppedFaces > 0) {
            legacyCaptureDroppedFaces.addAndGet(droppedFaces.toLong())
            legacyCaptureModelsWithDrops.incrementAndGet()
            if (droppedFaces == numFaces) legacyCaptureModelsFullyDropped.incrementAndGet()
            if (sc === mainScratch && diagBudget > 0 && diagDropSeen.add(numFaces * 31 + droppedFaces)) {
                diagBudget--
                System.err.println(
                    "[DropDiag] legacy-capture dropped $droppedFaces/$numFaces faces " +
                        "(directIdx=$useDirectVertexIndices maxDepth=$maxDepth " +
                        "numNormals=$numNormals numVertices=${model.numVertices})"
                )
            }
        }

        if (validFaceCount == 0) return null

        val maxVertexCount = validFaceCount * 3
        val neededBytes = maxVertexCount * ModelCache.BYTES_PER_VERTEX
        val vertexData = if (modelOwned) {
            reuse?.vertexData ?: EMPTY_VERTEX_DATA
        } else {
            MemoryUtil.memAlloc(neededBytes)
        }
        val materialIds = ShortArray(validFaceCount)

        var outFaceIdx = 0

        val contrastScale = if (model.contrast.toInt() != 0) 768f / model.contrast.toInt() else 1f

        val normalSlots = if (hasNormals) normalsX!!.size else 0
        if (normalSlots > 0) {
            if (sc.normalCacheStamp.size < normalSlots) {
                sc.normalCacheStamp = IntArray(normalSlots * 2)
                sc.normalCache = IntArray(normalSlots * 2 * 3)
            }
            sc.normalBuildStamp++
        }
        val nCache = sc.normalCache
        val nStamp = sc.normalCacheStamp
        val nStampNow = sc.normalBuildStamp
        val contrastScaleBits = java.lang.Float.floatToRawIntBits(contrastScale)
        val contrastForNormals = model.contrast.toInt().let { if (it == 0) 768 else it }
        val flatNormalScale = 2f / contrastForNormals
        val smoothNormalScale = 3f / contrastForNormals
        val colorWordsAreBigEndian = java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.BIG_ENDIAN

        val words = maxVertexCount * (ModelCache.BYTES_PER_VERTEX / 4)
        val vw: IntArray
        if (modelOwned) {
            val existing = reuse?.vertexWords
            vw = if (existing != null && existing.size >= words) existing else IntArray(words)
        } else {
            if (sc.vertexWordScratch.size < words) sc.vertexWordScratch = IntArray(words * 2)
            vw = sc.vertexWordScratch
        }
        var wp = 0

        fun writeNormal(out: Int, normalIdx: Int) {
            if (hasNormals && normalIdx >= 0 && normalIdx < normalSlots) {
                val base = normalIdx * 3
                if (nStamp[normalIdx] != nStampNow) {
                    nStamp[normalIdx] = nStampNow
                    val nX = normalsX!![normalIdx].toFloat()
                    val nY = normalsY!![normalIdx].toFloat()
                    val nZ = normalsZ!![normalIdx].toFloat()
                    val mag = magnitudes!![normalIdx].toInt() and 0xFF
                    val k = if (mag == 0) flatNormalScale else smoothNormalScale / mag
                    nCache[base] = java.lang.Float.floatToRawIntBits(nX * k)
                    nCache[base + 1] = java.lang.Float.floatToRawIntBits(nY * k)
                    nCache[base + 2] = java.lang.Float.floatToRawIntBits(nZ * k)
                }
                vw[out] = nCache[base]
                vw[out + 1] = nCache[base + 1]
                vw[out + 2] = nCache[base + 2]
            } else {
                vw[out] = 0
                vw[out + 1] = contrastScaleBits
                vw[out + 2] = 0
            }
        }

        val slotCount = if (useDirectVertexIndices) maxDepth else numNormals
        if (sc.reusableSlotColor.size < slotCount) {
            sc.reusableSlotColor = IntArray(slotCount * 2)
        }
        val slotColor = sc.reusableSlotColor
        if (sc.reusableSlotStamp.size < slotCount) {
            sc.reusableSlotStamp = IntArray(slotCount * 2)
        }
        val slotStamp = sc.reusableSlotStamp
        val stamp = ++sc.slotStampCounter
        val colorFaceCount = minOf(model.numFaces, facesA.size, facesB.size, facesC.size)
        if (sc.faceRgbaScratch.size < numFaces) sc.faceRgbaScratch = IntArray(numFaces * 2)
        val faceRgbaCache = sc.faceRgbaScratch
        for (f in 0 until colorFaceCount) {
            val materialId = facesMaterial?.get(f)?.toInt() ?: -1
            val hsl = facesColor?.get(f)?.toInt()?.and(0xFFFF) ?: 0
            val faceAlpha = facesAlpha?.get(f) ?: 0.toByte()
            val rgba = model.hslToRgb(hsl.toShort(), materialId.toShort(), modelAmbient, faceAlpha)
            if (f < numFaces) faceRgbaCache[f] = rgba
            val slotA = facesA[f].toInt() and 0xFFFF
            val slotB = facesB[f].toInt() and 0xFFFF
            val slotC = facesC[f].toInt() and 0xFFFF
            if (slotA < slotCount) { slotColor[slotA] = rgba; slotStamp[slotA] = stamp }
            if (slotB < slotCount) { slotColor[slotB] = rgba; slotStamp[slotB] = stamp }
            if (slotC < slotCount) { slotColor[slotC] = rgba; slotStamp[slotC] = stamp }
        }

        if (sc.slotRemapScratch.size < slotCount) {
            sc.slotRemapScratch = IntArray(slotCount * 2)
            sc.slotRemapStamp = IntArray(slotCount * 2)
        }
        val remap = sc.slotRemapScratch
        val remapStamp = sc.slotRemapStamp
        val remapNow = ++sc.slotRemapCounter
        if (sc.faceIndexScratch.size < validFaceCount * 3) sc.faceIndexScratch = IntArray(validFaceCount * 6)
        val faceIndex = sc.faceIndexScratch
        var emitted = 0
        val useSlot = slotColorEnabled

        fun emitSlot(slot: Int, vert: Int, faceRgba: Int): Int {
            if (remapStamp[slot] == remapNow) return remap[slot]
            val out = emitted++
            remapStamp[slot] = remapNow
            remap[slot] = out
            val rgba = if (useSlot && slot < slotCount && slotStamp[slot] == stamp) slotColor[slot] else { if (useSlot) sc.unwrittenSlotFaces++; faceRgba }
            val colorWord = if (colorWordsAreBigEndian) rgba else Integer.reverseBytes(rgba)
            val o = out * 10
            vw[o] = java.lang.Float.floatToRawIntBits(verticesX[vert].toFloat())
            vw[o + 1] = java.lang.Float.floatToRawIntBits(verticesY[vert].toFloat())
            vw[o + 2] = java.lang.Float.floatToRawIntBits(verticesZ[vert].toFloat())
            writeNormal(o + 3, slot)
            vw[o + 6] = if (textureU != null && slot < textureU.size) java.lang.Float.floatToRawIntBits(textureU[slot]) else 0
            vw[o + 7] = if (textureV != null && slot < textureV.size) java.lang.Float.floatToRawIntBits(textureV[slot]) else 0
            vw[o + 8] = colorWord
            vw[o + 9] = 0
            return out
        }

        for (faceIdx in 0 until numFaces) {
            val r = faceIdx * 3
            val vertA = resolved[r]
            if (vertA < 0) continue
            val vertB = resolved[r + 1]
            val vertC = resolved[r + 2]
            val idxA = facesA[faceIdx].toInt() and 0xFFFF
            val idxB = facesB[faceIdx].toInt() and 0xFFFF
            val idxC = facesC[faceIdx].toInt() and 0xFFFF

            val materialId = facesMaterial?.get(faceIdx)?.toInt() ?: -1
            materialIds[outFaceIdx] = materialId.toShort()

            val faceRgba = if (faceIdx < colorFaceCount) faceRgbaCache[faceIdx] else {
                val hsl = facesColor?.get(faceIdx)?.toInt()?.and(0xFFFF) ?: 0
                val faceAlpha = facesAlpha?.get(faceIdx) ?: 0.toByte()
                model.hslToRgb(hsl.toShort(), materialId.toShort(), modelAmbient, faceAlpha)
            }
            val f = outFaceIdx * 3
            faceIndex[f] = emitSlot(idxA, vertA, faceRgba)
            faceIndex[f + 1] = emitSlot(idxB, vertB, faceRgba)
            faceIndex[f + 2] = emitSlot(idxC, vertC, faceRgba)
            outFaceIdx++
        }
        val vertexCount = emitted
        wp = emitted * 10

        if (!modelOwned) {
            vertexData.clear()
            vertexData.asIntBuffer().put(vw, 0, wp)
            vertexData.position(0)
            vertexData.limit(wp * 4)
        }

        var runCount = 0
        run {
            var ids = sc.matGroupIds
            var counts = sc.matGroupCount
            var previous = Int.MIN_VALUE
            for (faceIdx in 0 until validFaceCount) {
                val rawId = materialIds[faceIdx].toInt()
                val materialId = if (rawId < 0) -1 else rawId
                if (materialId != previous) {
                    if (runCount == ids.size) {
                        ids = ids.copyOf(ids.size * 2)
                        counts = counts.copyOf(counts.size * 2)
                        sc.matGroupIds = ids
                        sc.matGroupCount = counts
                    }
                    ids[runCount] = materialId
                    counts[runCount] = 0
                    runCount++
                    previous = materialId
                }
                counts[runCount - 1]++
            }
        }
        val runIds = sc.matGroupIds
        val runCounts = sc.matGroupCount
        val prevBatches = reuse?.materialBatches
        var reuseBatches = prevBatches != null && prevBatches.size == runCount
        if (reuseBatches) {
            for (g in 0 until runCount) {
                val b = prevBatches!![g]
                if (b.materialId != runIds[g] || b.indexCount != runCounts[g] * 3 || b.indices.size < runCounts[g] * 3) { reuseBatches = false; break }
            }
        }
        val materialBatches = if (reuseBatches) prevBatches!! else Array(runCount) { g ->
            ModelCache.MaterialBatch(
                materialId = runIds[g],
                indices = IntArray(runCounts[g] * 3),
                indexCount = runCounts[g] * 3
            )
        }
        run {
            var faceIdx = 0
            for (g in 0 until runCount) {
                val out = materialBatches[g].indices
                System.arraycopy(faceIndex, faceIdx * 3, out, 0, runCounts[g] * 3)
                faceIdx += runCounts[g]
            }
        }

        if (reuse != null) {
            reuse.faceCount = validFaceCount
            reuse.vertexCount = vertexCount
            reuse.vertexData = vertexData
            reuse.materialBatches = materialBatches
            if (modelOwned) {
                reuse.vertexWords = vw
                reuse.vertexWordCount = wp
            }
            return reuse
        }
        val built = ModelCache.CachedModel(
            faceCount = validFaceCount,
            vertexCount = vertexCount,
            vertexData = vertexData,
            materialBatches = materialBatches,
            heapOwned = modelOwned
        )
        if (modelOwned) {
            built.vertexWords = vw
            built.vertexWordCount = wp
        }
        return built
    }

    private fun animatedGeometryFor(model: ModelGL): ModelCache.CachedModel? {
        val existing = model.modernStreamGeometry as? ModelCache.CachedModel
        if (existing != null && model.modernStreamValid) {
            streamGeometryHits++
            animatedGeometryRebuilt = false
            return existing
        }
        streamGeometryBuilds++
        run {
            val who: Class<*> = Scene.currentDrawEntity?.javaClass ?: Any::class.java
            val c = streamBuildsByClass.getOrPut(who) { IntArray(2) }
            c[0]++
            c[1] += model.renderedFaceCount
        }
        val built = buildModelCache(model, modelOwned = true, reuse = existing) ?: return null
        model.modernStreamGeometry = built
        model.modernStreamValid = true
        animatedGeometryRebuilt = true
        if (model.modernStreamKey == 0L) model.modernStreamKey = ++streamKeyCounter
        (model.modernPoseMemoOwner as? PoseMemoOwner)?.let { owner ->
            owner.storePoseGeometry(built, model.modernStreamKey)
            model.modernPoseMemoOwner = null
        }
        return built
    }

    private val streamBuildsByClass = HashMap<Class<*>, IntArray>()

    private var dbCount = 0
    private var dbModel = arrayOfNulls<ModelGL>(256)
    private var dbMatrix = Array(256) { Matrix43() }
    private var dbLight = FloatArray(256 * 4)
    private var dbTranslucent = BooleanArray(256)
    private var dbDepthWrite = BooleanArray(256)
    private var dbSeq = IntArray(256)
    private var dbKey = IntArray(256)
    private var dbBuildOf = IntArray(256)
    private var dbBuilt = arrayOfNulls<ModelCache.CachedModel>(256)
    private var dbClass = arrayOfNulls<Class<*>>(256)
    private var dbFrameStamp = 0
    private var deferredBuildsTotal = 0L
    private var deferredBuildFailures = 0
    private var deferredBuildMsAcc = 0.0

    private fun enqueueDeferredBuild(model: ModelGL, matrix: Matrix43, translucent: Boolean, depthWrite: Boolean, seq: Int, key: Int) {
        if (dbCount == dbModel.size) {
            val n = dbCount * 2
            val oldMatrix = dbMatrix
            dbModel = dbModel.copyOf(n)
            dbMatrix = Array(n) { if (it < oldMatrix.size) oldMatrix[it] else Matrix43() }
            dbLight = dbLight.copyOf(n * 4)
            dbTranslucent = dbTranslucent.copyOf(n); dbDepthWrite = dbDepthWrite.copyOf(n)
            dbSeq = dbSeq.copyOf(n); dbKey = dbKey.copyOf(n); dbBuildOf = dbBuildOf.copyOf(n)
            dbBuilt = dbBuilt.copyOf(n); dbClass = dbClass.copyOf(n)
        }
        val i = dbCount++
        dbModel[i] = model
        dbMatrix[i].copyFrom(matrix)
        dbLight[i * 4] = currentLightIdx[0]; dbLight[i * 4 + 1] = currentLightIdx[1]
        dbLight[i * 4 + 2] = currentLightIdx[2]; dbLight[i * 4 + 3] = currentLightIdx[3]
        dbTranslucent[i] = translucent; dbDepthWrite[i] = depthWrite
        dbSeq[i] = seq; dbKey[i] = key
        dbClass[i] = Scene.currentDrawEntity?.javaClass
        if (model.modernStreamKey == 0L) model.modernStreamKey = ++streamKeyCounter
        if (model.modernDeferredStamp == dbFrameStamp) {
            dbBuildOf[i] = model.modernDeferredIndex
        } else {
            model.modernDeferredStamp = dbFrameStamp
            model.modernDeferredIndex = i
            dbBuildOf[i] = i
        }
    }

    private fun clearDeferredBuilds() {
        for (i in 0 until dbCount) { dbModel[i] = null; dbBuilt[i] = null; dbClass[i] = null }
        dbCount = 0
        dbFrameStamp++
    }

    private fun flushDeferredBuilds() {
        val n = dbCount
        if (n == 0) { dbFrameStamp++; return }
        val t0 = System.nanoTime()
        val models = dbModel
        val buildOf = dbBuildOf
        val built = dbBuilt
        val buildOne: (Int) -> Unit = { i ->
            if (buildOf[i] == i) {
                val m = models[i]!!
                val existing = m.modernStreamGeometry as? ModelCache.CachedModel
                built[i] = try {
                    buildModelCache(m, modelOwned = true, reuse = existing, sc = threadScratch.get())
                } catch (t: Throwable) {
                    if (deferredBuildFailures++ < 5) { System.err.println("[DeferredBuild] failed: $t"); t.printStackTrace() }
                    null
                }
            }
        }
        if (n >= 4) {
            java.util.stream.IntStream.range(0, n).parallel().forEach { i -> buildOne(i) }
        } else {
            for (i in 0 until n) buildOne(i)
        }
        val inst = instancedBatcher
        val batcher = optimizedBatcher
        val lightScratch = FloatArray(4)
        for (i in 0 until n) {
            val m = models[i]!!
            val b = built[buildOf[i]]
            if (buildOf[i] == i) {
                streamGeometryBuilds++
                deferredBuildsTotal++
                val who: Class<*> = dbClass[i] ?: Any::class.java
                val c = streamBuildsByClass.getOrPut(who) { IntArray(2) }
                c[0]++; c[1] += m.renderedFaceCount
                if (b != null) { m.modernStreamGeometry = b; m.modernStreamValid = true }
            }
            if (b == null) { streamingModelsDropped++; continue }
            lightScratch[0] = dbLight[i * 4]; lightScratch[1] = dbLight[i * 4 + 1]
            lightScratch[2] = dbLight[i * 4 + 2]; lightScratch[3] = dbLight[i * 4 + 3]
            var added = false
            if (inst != null && inst.isActive()) {
                added = inst.addInstance(
                    b, m.modernStreamKey, dbMatrix[i], lightScratch, geometryDirty = (buildOf[i] == i),
                    translucent = dbTranslucent[i], depthWrite = dbDepthWrite[i], seq = dbSeq[i], depthKey = dbKey[i]
                )
            }
            if (added) {
                totalFacesProcessed += b.faceCount
            } else if (batcher != null && batcher.isActive()) {
                submitStreamedModel(batcher, b, dbMatrix[i], dbTranslucent[i], dbDepthWrite[i], dbSeq[i], dbKey[i])
            }
        }
        deferredBuildMsAcc += (System.nanoTime() - t0) / 1e6
        deferredBuildNsAcc += (((System.nanoTime() - t0) / 1e6) * 1e6).toLong()
        clearDeferredBuilds()
    }

    private var animatedGeometryRebuilt = false

    private var farToNearScratch = LongArray(256)

    private fun sortFarToNear(keys: IntArray, seqs: IntArray, n: Int, order: IntArray) {
        for (i in 0 until n) order[i] = i
        if (n < 2) return
        if (n > 4096) {
            val sorted = (0 until n).sortedWith(compareBy({ -keys[it].toLong() }, { seqs[it] }))
            for (i in 0 until n) order[i] = sorted[i]
            return
        }
        if (farToNearScratch.size < n) farToNearScratch = LongArray(n * 2)
        val scratch = farToNearScratch
        for (i in 0 until n) {
            val keyPart = (Int.MAX_VALUE.toLong() - keys[i]) and 0xFFFFFFFFL
            val seqPart = seqs[i].toLong() and 0xFFFFFL
            scratch[i] = (((keyPart shl 20) or seqPart) shl 12) or i.toLong() xor Long.MIN_VALUE
        }
        java.util.Arrays.sort(scratch, 0, n)
        for (i in 0 until n) order[i] = (scratch[i] and 0xFFFL).toInt()
    }

    private inline fun sharedGeometryFor(
        model: ModelGL,
        cache: ModelCache,
        key: Long,
        noinline build: () -> ModelCache.CachedModel?
    ): ModelCache.CachedModel? {
        if (model.modernSharedValid && model.modernSharedKey == key) {
            val memo = model.modernSharedGeometry as? ModelCache.CachedModel
            if (memo != null && !memo.evicted) {
                memo.lastUsedFrame = cache.frame
                return memo
            }
        }
        val resolved = cache.getOrCreate(model, key, build) ?: return null
        model.modernSharedGeometry = resolved
        model.modernSharedKey = key
        model.modernSharedValid = true
        return resolved
    }

    private fun submitStreamedModel(
        batcher: OptimizedFrameBatcher,
        cached: ModelCache.CachedModel,
        modelMatrix: Matrix43,
        translucent: Boolean = Scene.alphaPassActive,
        depthWrite: Boolean = depthBufferingEnabled,
        seq: Int = if (translucent) translucentSeqCounter++ else 0,
        depthKey: Int = if (translucent) (Scene.currentDrawEntity?.floorDepth ?: 0) else 0
    ) {
        selectLightsFor(modelMatrix.xD, modelMatrix.yD, modelMatrix.zD)
        var modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
        if (modelIndex < 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
            if (modelIndex < 0) return
        }
        modelCounter++
        debugModelSuccesses++
        var facesAdded = batcher.addCachedModel(cached, modelIndex, translucent, depthWrite, seq, depthKey)
        if (facesAdded == 0 && cached.faceCount > 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
            if (modelIndex >= 0) facesAdded = batcher.addCachedModel(cached, modelIndex, translucent, depthWrite, seq, depthKey)
            if (facesAdded == 0) streamingModelsDropped++
        }
        totalFacesProcessed += facesAdded
    }

    private fun renderModelGLDirect(
        model: ModelGL,
        modelMatrix: Matrix43,
        batcher: OptimizedFrameBatcher,
        modelIndex: Int
    ) {
        val numFaces = model.renderedFaceCount
        val facesA = model.facesA ?: return
        val facesB = model.facesB ?: return
        val facesC = model.facesC ?: return
        val verticesX = model.verticesX ?: return
        val verticesY = model.verticesY ?: return
        val verticesZ = model.verticesZ ?: return
        val maxDepth = model.maxDepth
        val numNormals = model.numNormals

        val triangles = model.triangles
        val normalMapping = model.vertexNormalIndices
        val textureU = model.textureU
        val textureV = model.textureV
        val facesColor = model.facesColor
        val facesMaterial = model.facesMaterial
        val facesAlpha = model.facesAlpha
        val modelAmbient = model.ambient
        val mergedDirect = model.mergedNormals
        val normalsX = mergedDirect?.normalsX ?: model.normalsX
        val normalsY = mergedDirect?.normalsY ?: model.normalsY
        val normalsZ = mergedDirect?.normalsZ ?: model.normalsZ
        val magnitudes = mergedDirect?.magnitude ?: model.magnitudes

        val useDirectVertexIndices = (triangles == null || normalMapping == null)
        val normalToVertex: IntArray?
        if (!useDirectVertexIndices && triangles != null && normalMapping != null) {
            if (reusableNormalToVertex.size < numNormals) {
                reusableNormalToVertex = IntArray(numNormals * 2)
            }
            for (i in 0 until numNormals) {
                reusableNormalToVertex[i] = -1
            }
            normalToVertex = reusableNormalToVertex
            for (vertIdx in 0 until maxDepth) {
                val triStart = triangles[vertIdx]
                val triEnd = triangles[vertIdx + 1]
                for (i in triStart until triEnd) {
                    val normalIdx = normalMapping[i].toInt() - 1
                    if (normalIdx == -1) break
                    if (normalIdx >= 0 && normalIdx < numNormals) {
                        normalToVertex[normalIdx] = vertIdx
                    }
                }
            }
        } else {
            normalToVertex = null
        }

        val hasVertexNormals = normalsX != null && normalsY != null && normalsZ != null && magnitudes != null
        val normals = reusableNormals

        val slotCount = if (useDirectVertexIndices) maxDepth else numNormals
        val sc = mainScratch
        if (sc.reusableSlotColor.size < slotCount) {
            sc.reusableSlotColor = IntArray(slotCount * 2)
        }
        val slotColor = sc.reusableSlotColor
        if (sc.reusableSlotStamp.size < slotCount) {
            sc.reusableSlotStamp = IntArray(slotCount * 2)
        }
        val slotStamp = sc.reusableSlotStamp
        val stamp = ++sc.slotStampCounter
        val colorFaceCount = minOf(model.numFaces, facesA.size, facesB.size, facesC.size)
        for (f in 0 until colorFaceCount) {
            val materialId = facesMaterial?.get(f)?.toInt() ?: -1
            val hsl = facesColor?.get(f)?.toInt()?.and(0xFFFF) ?: 0
            val faceAlpha = facesAlpha?.get(f) ?: 0.toByte()
            val rgba = model.hslToRgb(hsl.toShort(), materialId.toShort(), modelAmbient, faceAlpha)
            val slotA = facesA[f].toInt() and 0xFFFF
            val slotB = facesB[f].toInt() and 0xFFFF
            val slotC = facesC[f].toInt() and 0xFFFF
            if (slotA < slotCount) { slotColor[slotA] = rgba; slotStamp[slotA] = stamp }
            if (slotB < slotCount) { slotColor[slotB] = rgba; slotStamp[slotB] = stamp }
            if (slotC < slotCount) { slotColor[slotC] = rgba; slotStamp[slotC] = stamp }
        }

        for (faceIdx in 0 until numFaces) {
            val idxA = facesA[faceIdx].toInt() and 0xFFFF
            val idxB = facesB[faceIdx].toInt() and 0xFFFF
            val idxC = facesC[faceIdx].toInt() and 0xFFFF

            val vertA: Int
            val vertB: Int
            val vertC: Int
            if (useDirectVertexIndices) {
                vertA = idxA; vertB = idxB; vertC = idxC
            } else {
                vertA = if (idxA < numNormals && normalToVertex != null) normalToVertex[idxA] else -1
                vertB = if (idxB < numNormals && normalToVertex != null) normalToVertex[idxB] else -1
                vertC = if (idxC < numNormals && normalToVertex != null) normalToVertex[idxC] else -1
            }

            if (vertA < 0 || vertB < 0 || vertC < 0 ||
                vertA >= maxDepth || vertB >= maxDepth || vertC >= maxDepth) {
                continue
            }

            totalFacesProcessed++

            val materialId = facesMaterial?.get(faceIdx)?.toInt() ?: -1

            val faceRgba = run {
                val hsl = facesColor?.get(faceIdx)?.toInt()?.and(0xFFFF) ?: 0
                val faceAlpha = facesAlpha?.get(faceIdx) ?: 0.toByte()
                model.hslToRgb(hsl.toShort(), materialId.toShort(), modelAmbient, faceAlpha)
            }
            val useSlot = slotColorEnabled
            val rgbaA = if (useSlot && idxA < slotCount && slotStamp[idxA] == stamp) slotColor[idxA] else { if (useSlot) unwrittenSlotFaces++; faceRgba }
            val rgbaB = if (useSlot && idxB < slotCount && slotStamp[idxB] == stamp) slotColor[idxB] else faceRgba
            val rgbaC = if (useSlot && idxC < slotCount && slotStamp[idxC] == stamp) slotColor[idxC] else faceRgba
            val rA = ((rgbaA ushr 24) and 0xFF).toByte()
            val gA = ((rgbaA ushr 16) and 0xFF).toByte()
            val bA = ((rgbaA ushr 8) and 0xFF).toByte()
            val aA = (rgbaA and 0xFF).toByte()
            val rB = ((rgbaB ushr 24) and 0xFF).toByte()
            val gB = ((rgbaB ushr 16) and 0xFF).toByte()
            val bB = ((rgbaB ushr 8) and 0xFF).toByte()
            val aB = (rgbaB and 0xFF).toByte()
            val rC = ((rgbaC ushr 24) and 0xFF).toByte()
            val gC = ((rgbaC ushr 16) and 0xFF).toByte()
            val bC = ((rgbaC ushr 8) and 0xFF).toByte()
            val aC = (rgbaC and 0xFF).toByte()

            val lx0 = verticesX[vertA].toFloat()
            val ly0 = verticesY[vertA].toFloat()
            val lz0 = verticesZ[vertA].toFloat()
            val lx1 = verticesX[vertB].toFloat()
            val ly1 = verticesY[vertB].toFloat()
            val lz1 = verticesZ[vertB].toFloat()
            val lx2 = verticesX[vertC].toFloat()
            val ly2 = verticesY[vertC].toFloat()
            val lz2 = verticesZ[vertC].toFloat()

            val u0 = if (textureU != null && idxA < textureU.size) textureU[idxA] else 0f
            val v0 = if (textureV != null && idxA < textureV.size) textureV[idxA] else 0f
            val u1 = if (textureU != null && idxB < textureU.size) textureU[idxB] else 0f
            val v1 = if (textureV != null && idxB < textureV.size) textureV[idxB] else 0f
            val u2 = if (textureU != null && idxC < textureU.size) textureU[idxC] else 0f
            val v2 = if (textureV != null && idxC < textureV.size) textureV[idxC] else 0f

            if (hasVertexNormals) {
                writeNormalizedNormal(normals, 0, idxA, normalsX!!, normalsY!!, normalsZ!!, magnitudes!!, model.contrast.toInt())
                writeNormalizedNormal(normals, 3, idxB, normalsX, normalsY, normalsZ, magnitudes, model.contrast.toInt())
                writeNormalizedNormal(normals, 6, idxC, normalsX, normalsY, normalsZ, magnitudes, model.contrast.toInt())
            } else {
                for (i in 0 until 9) normals[i] = 0f
                normals[1] = 1f; normals[4] = 1f; normals[7] = 1f
            }

            batcher.addFaceOptimized(
                materialId, modelIndex,
                lx0, ly0, lz0, normals[0], normals[1], normals[2], u0, v0, rA, gA, bA, aA,
                lx1, ly1, lz1, normals[3], normals[4], normals[5], u1, v1, rB, gB, bB, aB,
                lx2, ly2, lz2, normals[6], normals[7], normals[8], u2, v2, rC, gC, bC, aC
            )
        }
    }

    private fun writeNormalizedNormal(
        out: FloatArray, off: Int, idx: Int,
        normalsX: ShortArray, normalsY: ShortArray, normalsZ: ShortArray,
        magnitudes: ByteArray, contrast: Int
    ) {
        if (idx < normalsX.size) {
            val c = if (contrast == 0) 768 else contrast
            val nX = normalsX[idx].toFloat()
            val nY = normalsY[idx].toFloat()
            val nZ = normalsZ[idx].toFloat()
            val mag = magnitudes[idx].toInt() and 0xFF
            val k = if (mag == 0) 2f / c else 3f / (c * mag)
            out[off] = nX * k
            out[off + 1] = nY * k
            out[off + 2] = nZ * k
            return
        }
        if (false) {
            val nX = normalsX[idx].toFloat()
            val nY = normalsY[idx].toFloat()
            val nZ = normalsZ[idx].toFloat()
            val mag = magnitudes[idx].toInt() and 0xFF

            val rawNx: Float
            val rawNy: Float
            val rawNz: Float
            if (mag == 0) {
                rawNx = nX; rawNy = nY; rawNz = nZ
            } else {
                val invMag = 1f / (mag * 256f)
                rawNx = nX * invMag; rawNy = nY * invMag; rawNz = nZ * invMag
            }
            val len = kotlin.math.sqrt(rawNx * rawNx + rawNy * rawNy + rawNz * rawNz)
            if (len > 0.001f) {
                out[off] = rawNx / len
                out[off + 1] = rawNy / len
                out[off + 2] = rawNz / len
                return
            }
        }
        out[off] = 0f
        out[off + 1] = 1f
        out[off + 2] = 0f
    }

    private fun renderModelGLLegacy(model: ModelGL, modelMatrix: Matrix43) {
        selectLightsFor(modelMatrix.xD, modelMatrix.yD, modelMatrix.zD)

        val batcher = frameBatcher ?: return
        if (!batcher.isActive()) return

        val numFaces = model.renderedFaceCount
        if (numFaces <= 0) return

        val facesA = model.facesA ?: return
        val facesB = model.facesB ?: return
        val facesC = model.facesC ?: return
        val verticesX = model.verticesX ?: return
        val verticesY = model.verticesY ?: return
        val verticesZ = model.verticesZ ?: return
        val maxDepth = model.maxDepth
        val numNormals = model.numNormals

        val triangles = model.triangles
        val normalMapping = model.vertexNormalIndices
        val textureU = model.textureU
        val textureV = model.textureV
        val facesColor = model.facesColor
        val facesMaterial = model.facesMaterial
        val facesAlpha = model.facesAlpha
        val modelAmbient = model.ambient
        val mergedLegacy = model.mergedNormals
        val normalsX = mergedLegacy?.normalsX ?: model.normalsX
        val normalsY = mergedLegacy?.normalsY ?: model.normalsY
        val normalsZ = mergedLegacy?.normalsZ ?: model.normalsZ
        val magnitudes = mergedLegacy?.magnitude ?: model.magnitudes

        val sunDir = sunDirection
        val sunDirX = sunDir[0]
        val sunDirY = sunDir[1]
        val sunDirZ = sunDir[2]
        val sunBright = sunBrightness
        val contrastPos = sunColorIntensity * 768.0f / model.contrast
        val contrastNeg = antiSunColorIntensity * 768.0f / model.contrast

        val vp = viewProjectionMatrix.elements
        modelCounter++

        val useDirectVertexIndices = (triangles == null || normalMapping == null)
        val normalToVertex: IntArray?
        if (!useDirectVertexIndices && triangles != null && normalMapping != null) {
            normalToVertex = IntArray(numNormals) { -1 }
            for (vertIdx in 0 until maxDepth) {
                val triStart = triangles[vertIdx]
                val triEnd = triangles[vertIdx + 1]
                for (i in triStart until triEnd) {
                    val normalIdx = normalMapping[i].toInt() - 1
                    if (normalIdx == -1) break
                    if (normalIdx >= 0 && normalIdx < numNormals) {
                        normalToVertex[normalIdx] = vertIdx
                    }
                }
            }
        } else {
            normalToVertex = null
        }

        val ndcPositions = reusableNdcPositions
        val colors = reusableColors

        for (faceIdx in 0 until numFaces) {
            val idxA = facesA[faceIdx].toInt() and 0xFFFF
            val idxB = facesB[faceIdx].toInt() and 0xFFFF
            val idxC = facesC[faceIdx].toInt() and 0xFFFF

            val vertA: Int
            val vertB: Int
            val vertC: Int

            if (useDirectVertexIndices) {
                vertA = idxA
                vertB = idxB
                vertC = idxC
            } else {
                vertA = if (idxA < numNormals && normalToVertex != null) normalToVertex[idxA] else -1
                vertB = if (idxB < numNormals && normalToVertex != null) normalToVertex[idxB] else -1
                vertC = if (idxC < numNormals && normalToVertex != null) normalToVertex[idxC] else -1
            }

            if (vertA < 0 || vertB < 0 || vertC < 0 ||
                vertA >= maxDepth || vertB >= maxDepth || vertC >= maxDepth) continue

            val materialId = facesMaterial?.get(faceIdx)?.toInt() ?: -1
            val hslColor = facesColor?.get(faceIdx)?.toInt()?.and(0xFFFF) ?: 0
            val faceAlpha = facesAlpha?.get(faceIdx) ?: 0.toByte()

            val baseRgba = model.hslToRgb(hslColor.toShort(), materialId.toShort(), modelAmbient, faceAlpha)
            val baseR = ((baseRgba ushr 24) and 0xFF) * sunColorRed
            val baseG = ((baseRgba ushr 16) and 0xFF) * sunColorGreen
            val baseB = ((baseRgba ushr 8) and 0xFF) * sunColorBlue
            val finalAlpha = (255 - (faceAlpha.toInt() and 0xFF)).toByte()

            val lx0 = verticesX[vertA].toFloat()
            val ly0 = verticesY[vertA].toFloat()
            val lz0 = verticesZ[vertA].toFloat()
            val lx1 = verticesX[vertB].toFloat()
            val ly1 = verticesY[vertB].toFloat()
            val lz1 = verticesZ[vertB].toFloat()
            val lx2 = verticesX[vertC].toFloat()
            val ly2 = verticesY[vertC].toFloat()
            val lz2 = verticesZ[vertC].toFloat()

            val e1x = lx1 - lx0; val e1y = ly1 - ly0; val e1z = lz1 - lz0
            val e2x = lx2 - lx0; val e2y = ly2 - ly0; val e2z = lz2 - lz0
            var faceNx = e1y * e2z - e1z * e2y
            var faceNy = e1z * e2x - e1x * e2z
            var faceNz = e1x * e2y - e1y * e2x
            val faceLen = kotlin.math.sqrt(faceNx * faceNx + faceNy * faceNy + faceNz * faceNz)
            if (faceLen > 0.0001f) {
                faceNx /= faceLen; faceNy /= faceLen; faceNz /= faceLen
            } else {
                faceNx = 0f; faceNy = 1f; faceNz = 0f
            }

            val localPositions = floatArrayOf(lx0, ly0, lz0, lx1, ly1, lz1, lx2, ly2, lz2)
            var anyOutside = false

            for (v in 0..2) {
                val localX = localPositions[v * 3]
                val localY = localPositions[v * 3 + 1]
                val localZ = localPositions[v * 3 + 2]

                val worldX = localX * modelMatrix.xA + localY * modelMatrix.xB + localZ * modelMatrix.xC + modelMatrix.xD
                val worldY = localX * modelMatrix.yA + localY * modelMatrix.yB + localZ * modelMatrix.yC + modelMatrix.yD
                val worldZ = localX * modelMatrix.zA + localY * modelMatrix.zB + localZ * modelMatrix.zC + modelMatrix.zD

                val clipX = vp[12] + vp[0] * worldX + vp[4] * worldY + vp[8] * worldZ
                val clipY = vp[13] + vp[1] * worldX + vp[5] * worldY + vp[9] * worldZ
                val clipZ = vp[14] + vp[2] * worldX + vp[6] * worldY + vp[10] * worldZ
                val clipW = vp[15] + vp[3] * worldX + vp[7] * worldY + vp[11] * worldZ

                if (clipW > 0.001f) {
                    ndcPositions[v * 3] = clipX / clipW
                    ndcPositions[v * 3 + 1] = -(clipY / clipW)
                    ndcPositions[v * 3 + 2] = clipZ / clipW
                } else {
                    ndcPositions[v * 3] = 1000f
                    ndcPositions[v * 3 + 1] = 1000f
                    ndcPositions[v * 3 + 2] = 1000f
                }

                val ndcX = ndcPositions[v * 3]
                val ndcY = ndcPositions[v * 3 + 1]
                val ndcZ = ndcPositions[v * 3 + 2]
                if (ndcX < -1.5f || ndcX > 1.5f || ndcY < -1.5f || ndcY > 1.5f || ndcZ < -0.1f || ndcZ > 1.5f) {
                    anyOutside = true
                }
            }

            if (anyOutside) continue

            val u0 = textureU?.getOrNull(idxA) ?: 0f
            val v0 = textureV?.getOrNull(idxA) ?: 0f
            val u1 = textureU?.getOrNull(idxB) ?: 0f
            val v1 = textureV?.getOrNull(idxB) ?: 0f
            val u2 = textureU?.getOrNull(idxC) ?: 0f
            val v2 = textureV?.getOrNull(idxC) ?: 0f

            val texIndices = intArrayOf(idxA, idxB, idxC)
            for (v in 0..2) {
                val texIdx = texIndices[v]

                var lightIntensity: Float
                if (normalsX != null && normalsY != null && normalsZ != null && magnitudes != null && texIdx < normalsX.size) {
                    val nX = normalsX[texIdx].toFloat()
                    val nY = normalsY[texIdx].toFloat()
                    val nZ = normalsZ[texIdx].toFloat()
                    val mag = magnitudes[texIdx].toInt() and 0xFF

                    val dotProduct: Float = if (mag == 0) {
                        (sunDirX * nX + sunDirY * nY + sunDirZ * nZ) * 0.0026041667f
                    } else {
                        (sunDirX * nX + sunDirY * nY + sunDirZ * nZ) / (mag * 256)
                    }
                    lightIntensity = sunBright + dotProduct * (if (dotProduct < 0.0f) contrastNeg else contrastPos)
                } else {
                    val dotProduct = sunDirX * faceNx + sunDirY * faceNy + sunDirZ * faceNz
                    lightIntensity = sunBright + dotProduct * (if (dotProduct < 0.0f) contrastNeg else contrastPos)
                }

                val finalR = (baseR * lightIntensity).toInt().coerceIn(0, 255)
                val finalG = (baseG * lightIntensity).toInt().coerceIn(0, 255)
                val finalB = (baseB * lightIntensity).toInt().coerceIn(0, 255)

                colors[v * 4] = finalR.toByte()
                colors[v * 4 + 1] = finalG.toByte()
                colors[v * 4 + 2] = finalB.toByte()
                colors[v * 4 + 3] = finalAlpha
            }

            batcher.addFace(
                materialId,
                ndcPositions[0], ndcPositions[1], ndcPositions[2],
                faceNx, faceNy, faceNz,
                u0, v0,
                colors[0], colors[1], colors[2], colors[3],
                ndcPositions[3], ndcPositions[4], ndcPositions[5],
                faceNx, faceNy, faceNz,
                u1, v1,
                colors[4], colors[5], colors[6], colors[7],
                ndcPositions[6], ndcPositions[7], ndcPositions[8],
                faceNx, faceNy, faceNz,
                u2, v2,
                colors[8], colors[9], colors[10], colors[11]
            )
        }

        batcher.endModel()
    }

    override fun shutdown() {
        if (Scene.retainedHook === retainedScene) Scene.retainedHook = null
        Culling.disabledForModern = false
        retainedScene = null
        modernRenderer?.destroy()
        modernRenderer = null

        groundRenderer?.destroy()
        groundRenderer = null
        for ((ground, vbo) in residentGroundVbos) {
            glDeleteBuffers(vbo)
            ground.modernVboId = 0
            ground.modernVboRevision = -1
        }
        residentGroundVbos.clear()

        spriteRenderer?.destroy()
        spriteRenderer = null

        surfaceMaskRenderer?.destroy()
        surfaceMaskRenderer = null

        particleRendererModern?.destroy()
        particleRendererModern = null
        billboardRendererModern?.destroy()
        billboardRendererModern = null
        resolveSceneMsaa()
        sceneMsaa?.destroy()
        sceneMsaa = null
        modernBloomCapturing = false
        modernBloom?.destroy()
        modernBloom = null

        frameBatcher?.destroy()
        frameBatcher = null

        optimizedBatcher?.destroy()
        optimizedBatcher = null

        instancedBatcher?.destroy()
        instancedBatcher = null
        BufferStaging.destroy()

        modelCache?.clear()
        modelCache = null

        isModernRendererInitialized = false
        isSpriteBatchActive = false
        super.shutdown()
    }

    fun isModernRenderingActive(): Boolean {
        return canUseModernPipeline() && modernRenderer != null
    }

    fun getModernRendererStats(): ModernModelRenderer.FrameStats? {
        if (!isModernRendererInitialized) return modernRenderer?.getFrameStats()

        var totalDrawCalls = 0
        var totalTriangles = 0

        if (useOptimizedBatcher) {
            val stats = optimizedBatcher?.getStats()
            if (stats != null) {
                totalDrawCalls += stats.drawCalls
                totalTriangles += stats.triangles
            }
        } else {
            val stats = frameBatcher?.getStats()
            if (stats != null) {
                totalDrawCalls += stats.drawCalls
                totalTriangles += stats.triangles
            }
        }

        val instanceStats = instancedBatcher?.getStats()
        if (instanceStats != null) {
            totalDrawCalls += instanceStats.drawCalls
            totalTriangles += instanceStats.triangles
        }

        val groundStats = groundRenderer?.getFrameStats()
        if (groundStats != null) {
            totalDrawCalls += groundStats.drawCalls
            totalTriangles += groundStats.triangles
        }

        val spriteStats = spriteRenderer?.getFrameStats()
        if (spriteStats != null) {
            totalDrawCalls += spriteStats.drawCalls
        }

        val particleStats = particleRendererModern?.getFrameStats()
        if (particleStats != null) {
            totalDrawCalls += particleStats.drawCalls
        }

        return ModernModelRenderer.FrameStats(
            drawCalls = totalDrawCalls,
            triangles = totalTriangles
        )
    }

    fun getPerSubsystemDrawCalls(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()

        val batcherStats = if (useOptimizedBatcher) optimizedBatcher?.getStats() else frameBatcher?.getStats()
        result["models_batched"] = if (useOptimizedBatcher) (batcherStats as? OptimizedFrameBatcher.FrameStats)?.drawCalls ?: 0
            else (batcherStats as? FrameBatcher.FrameStats)?.drawCalls ?: 0

        result["models_instanced"] = instancedBatcher?.getStats()?.drawCalls ?: 0

        result["ground"] = groundRenderer?.getFrameStats()?.drawCalls ?: 0

        result["sprites"] = spriteRenderer?.getFrameStats()?.drawCalls ?: 0

        result["particles"] = particleRendererModern?.getFrameStats()?.drawCalls ?: 0

        result["total"] = result.values.sum()

        return result
    }

    fun getOptimizedBatcherStats(): OptimizedFrameBatcher.FrameStats? {
        return optimizedBatcher?.getStats()
    }

    fun getBatcherDebugStatus(): Map<String, Any> {
        val cacheStats = modelCache?.getStats()
        val batcherStats = optimizedBatcher?.getStats()
        return mapOf(
            "useOptimizedBatcher" to useOptimizedBatcher,
            "isModernRendererInitialized" to isModernRendererInitialized,
            "optimizedBatcher_isNull" to (optimizedBatcher == null),
            "optimizedBatcher_isActive" to (optimizedBatcher?.isActive() == true),
            "frameBatcher_isNull" to (frameBatcher == null),
            "frameBatcher_isActive" to (frameBatcher?.isActive() == true),
            "totalModelsProcessed" to totalModelsProcessed,
            "totalFacesProcessed" to totalFacesProcessed,
            "modelsCulled" to modelsCulled,
            "cacheHits" to cacheHits,
            "cacheMisses" to cacheMisses,
            "cachedModels" to (cacheStats?.cachedModels ?: 0),
            "cacheTotalMB" to (cacheStats?.totalMB ?: 0f),
            "cacheHitRate" to (cacheStats?.hitRate ?: 0f),
            "lastBatcherStatus" to lastBatcherStatus,
            "frameCounter" to frameCounter,
            "modelCounter" to modelCounter,
            "timeAddModelsMs" to (batcherStats?.timeAddModelsMs ?: 0f),
            "timeUploadMs" to (batcherStats?.timeUploadMs ?: 0f),
            "timeRenderMs" to (batcherStats?.timeRenderMs ?: 0f),
            "timeTotalMs" to (batcherStats?.timeTotalMs ?: 0f),
            "timeBulkCopyMs" to (batcherStats?.timeBulkCopyMs ?: 0f),
            "timePatchModelIdxMs" to (batcherStats?.timePatchModelIdxMs ?: 0f),
            "timeIndexCopyMs" to (batcherStats?.timeIndexCopyMs ?: 0f),
            "timeFrustumCullMs" to (timeFrustumCull / 1_000_000f),
            "timeBeginModelMs" to (timeBeginModel / 1_000_000f),
            "timeComputeHashMs" to (timeComputeHash / 1_000_000f),
            "timeCacheLookupMs" to (timeCacheLookup / 1_000_000f),
            "timeAddCachedModelMs" to (timeAddCachedModel / 1_000_000f),
            "spriteRenderer_ready" to (spriteRenderer?.isReady() == true),
            "spriteDrawCalls" to (spriteRenderer?.getFrameStats()?.drawCalls ?: 0),
            "spriteQuads" to (spriteRenderer?.getFrameStats()?.quads ?: 0),
            "spriteTextureSlots" to (spriteRenderer?.getFrameStats()?.textureSlots ?: 0),
            "particleRenderer_ready" to (particleRendererModern?.isReady() == true),
            "particleDrawCalls" to (particleRendererModern?.getFrameStats()?.drawCalls ?: 0),
            "particleCount" to (particleRendererModern?.getFrameStats()?.particles ?: 0),
            "useInstancing" to useInstancing,
            "instancedBatcher_isNull" to (instancedBatcher == null),
            "instancedDrawCalls" to (instancedBatcher?.getStats()?.drawCalls ?: 0),
            "instancedTriangles" to (instancedBatcher?.getStats()?.triangles ?: 0),
            "instancedTotalInstances" to (instancedBatcher?.getStats()?.totalInstances ?: 0),
            "instancedUniqueMeshes" to (instancedBatcher?.getStats()?.uniqueMeshes ?: 0),
            "instancedMeshesUsed" to (instancedBatcher?.getStats()?.instancedMeshes ?: 0),
            "instancedMeshesReused" to (instancedBatcher?.getStats()?.meshesReused ?: 0)
        )
    }

    private fun flushBatchMidFrame(batcher: OptimizedFrameBatcher) {
        applyModelBlend()
        batcher.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
        batcher.drawTranslucent(::lookupTextureHandle, ::lookupCombineMode)

        batcher.beginFrame()

        batcher.setViewProjection(viewProjectionMatrix.elements)
        armBatchLightingAndFog()
    }

    private val VERIFY_MODEL_CULL = System.getProperty("render.verifycullmodels") != null
    private var modelCullChecks = 0L
    private var modelCullBad = 0L

    private fun verifyModelCull(model: ModelGL, modelMatrix: Matrix43, vp: FloatArray) {
        if (model.renderedFaceCount <= 0) return
        modelCullChecks++
        val minX = model.minX.toFloat(); val maxX = model.maxX.toFloat()
        val minY = model.minY.toFloat(); val maxY = model.maxY.toFloat()
        val minZ = model.minZ.toFloat(); val maxZ = model.maxZ.toFloat()
        var allLeft = true; var allRight = true
        var allBelow = true; var allAbove = true
        var allNear = true; var allFar = true
        for (corner in 0 until 8) {
            val lx = if (corner and 1 == 0) minX else maxX
            val ly = if (corner and 2 == 0) minY else maxY
            val lz = if (corner and 4 == 0) minZ else maxZ
            val wx = lx * modelMatrix.xA + ly * modelMatrix.xB + lz * modelMatrix.xC + modelMatrix.xD
            val wy = lx * modelMatrix.yA + ly * modelMatrix.yB + lz * modelMatrix.yC + modelMatrix.yD
            val wz = lx * modelMatrix.zA + ly * modelMatrix.zB + lz * modelMatrix.zC + modelMatrix.zD
            val cx = vp[12] + vp[0] * wx + vp[4] * wy + vp[8] * wz
            val cy = vp[13] + vp[1] * wx + vp[5] * wy + vp[9] * wz
            val cz = vp[14] + vp[2] * wx + vp[6] * wy + vp[10] * wz
            val cw = vp[15] + vp[3] * wx + vp[7] * wy + vp[11] * wz
            if (cx >= -cw) allLeft = false
            if (cx <= cw) allRight = false
            if (cy >= -cw) allBelow = false
            if (cy <= cw) allAbove = false
            if (cz >= 0f) allNear = false
            if (cz <= cw) allFar = false
        }
        if (!(allLeft || allRight || allBelow || allAbove || allNear || allFar)) {
            modelCullBad++
            if (modelCullBad <= 15) System.err.println(
                "[CullVerify] model culled but its AABB is inside the frustum: " +
                    "faces=${model.renderedFaceCount} origin=(${modelMatrix.xD}, ${modelMatrix.yD}, ${modelMatrix.zD})")
        }
    }

    private fun isPerspectiveProjection(): Boolean = currentModelMatrix.elements[11] != 0f

    private fun isModelVisible(
        model: ModelGL,
        modelMatrix: Matrix43,
        vp: FloatArray
    ): Boolean {
        val minX = model.minX.toFloat()
        val maxX = model.maxX.toFloat()
        val minY = model.minY.toFloat()
        val maxY = model.maxY.toFloat()
        val minZ = model.minZ.toFloat()
        val maxZ = model.maxZ.toFloat()

        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        val centerZ = (minZ + maxZ) * 0.5f

        val worldCenterX = centerX * modelMatrix.xA + centerY * modelMatrix.xB + centerZ * modelMatrix.xC + modelMatrix.xD
        val worldCenterY = centerX * modelMatrix.yA + centerY * modelMatrix.yB + centerZ * modelMatrix.yC + modelMatrix.yD
        val worldCenterZ = centerX * modelMatrix.zA + centerY * modelMatrix.zB + centerZ * modelMatrix.zC + modelMatrix.zD

        val extentX = (maxX - minX) * 0.5f
        val extentY = (maxY - minY) * 0.5f
        val extentZ = (maxZ - minZ) * 0.5f

        val absXA = kotlin.math.abs(modelMatrix.xA)
        val absXB = kotlin.math.abs(modelMatrix.xB)
        val absXC = kotlin.math.abs(modelMatrix.xC)
        val absYA = kotlin.math.abs(modelMatrix.yA)
        val absYB = kotlin.math.abs(modelMatrix.yB)
        val absYC = kotlin.math.abs(modelMatrix.yC)
        val absZA = kotlin.math.abs(modelMatrix.zA)
        val absZB = kotlin.math.abs(modelMatrix.zB)
        val absZC = kotlin.math.abs(modelMatrix.zC)

        val worldExtentX = extentX * absXA + extentY * absXB + extentZ * absXC
        val worldExtentY = extentX * absYA + extentY * absYB + extentZ * absYC
        val worldExtentZ = extentX * absZA + extentY * absZB + extentZ * absZC

        val vp0 = vp[0]; val vp1 = vp[1]; val vp2 = vp[2]; val vp3 = vp[3]
        val vp4 = vp[4]; val vp5 = vp[5]; val vp6 = vp[6]; val vp7 = vp[7]
        val vp8 = vp[8]; val vp9 = vp[9]; val vp10 = vp[10]; val vp11 = vp[11]
        val vp12 = vp[12]; val vp13 = vp[13]; val vp14 = vp[14]; val vp15 = vp[15]

        if (aabbOutsidePlane(vp0 + vp3, vp4 + vp7, vp8 + vp11, vp12 + vp15,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        if (aabbOutsidePlane(vp3 - vp0, vp7 - vp4, vp11 - vp8, vp15 - vp12,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        if (aabbOutsidePlane(vp1 + vp3, vp5 + vp7, vp9 + vp11, vp13 + vp15,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        if (aabbOutsidePlane(vp3 - vp1, vp7 - vp5, vp11 - vp9, vp15 - vp13,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        if (aabbOutsidePlane(vp2, vp6, vp10, vp14,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        if (aabbOutsidePlane(vp3 - vp2, vp7 - vp6, vp11 - vp10, vp15 - vp14,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false

        return true
    }

    private fun aabbOutsidePlane(
        a: Float, b: Float, c: Float, d: Float,
        cx: Float, cy: Float, cz: Float,
        ex: Float, ey: Float, ez: Float
    ): Boolean {
        val centreDistance = a * cx + b * cy + c * cz + d
        val reach = kotlin.math.abs(a) * ex + kotlin.math.abs(b) * ey + kotlin.math.abs(c) * ez
        return centreDistance + reach < 0f
    }

    private fun aabbInsidePlane(
        a: Float, b: Float, c: Float, d: Float,
        cx: Float, cy: Float, cz: Float,
        ex: Float, ey: Float, ez: Float
    ): Boolean {
        val centreDistance = a * cx + b * cy + c * cz + d
        val reach = kotlin.math.abs(a) * ex + kotlin.math.abs(b) * ey + kotlin.math.abs(c) * ez
        return centreDistance - reach >= 0f
    }

    private fun groundModernVertexData(ground: GroundGL): java.nio.ByteBuffer? = ground.modernVertexData

    private fun groundModernVertexStride(ground: GroundGL): Int = ground.modernVertexStride

    private fun residentGroundVbo(ground: GroundGL, data: java.nio.ByteBuffer): Int {
        var id = ground.modernVboId
        if (id == 0) {
            id = org.lwjgl.opengl.GL33.glGenBuffers()
            if (id == 0) return 0
            ground.modernVboId = id
            ground.modernVboRevision = -1
            residentGroundVbos[ground] = id
            while (residentGroundVbos.size > MAX_RESIDENT_GROUND_VBOS) {
                val oldest = residentGroundVbos.entries.iterator().next()
                residentGroundVbos.remove(oldest.key)
                if (oldest.key !== ground) {
                    org.lwjgl.opengl.GL33.glDeleteBuffers(oldest.value)
                    oldest.key.modernVboId = 0
                    oldest.key.modernVboRevision = -1
                }
            }
        } else {
            residentGroundVbos[ground] = id
        }
        if (ground.modernVboRevision != ground.modernVertexRevision) {
            data.position(0)
            org.lwjgl.opengl.GL33.glBindBuffer(org.lwjgl.opengl.GL33.GL_ARRAY_BUFFER, id)
            org.lwjgl.opengl.GL33.glBufferData(
                org.lwjgl.opengl.GL33.GL_ARRAY_BUFFER, data, org.lwjgl.opengl.GL33.GL_STATIC_DRAW
            )
            ground.modernVboRevision = ground.modernVertexRevision
        }
        return id
    }

    private val residentGroundVbos = LinkedHashMap<GroundGL, Int>(32, 0.75f, true)
    private val MAX_RESIDENT_GROUND_VBOS = 24

    fun renderMinimapFloorModern(
        ground: GroundGL,
        centerOffsetX: Int,
        centerOffsetY: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        materialized: Array<BooleanArray>
    ): Boolean {
        val gr = groundRenderer ?: return false
        if (!canUseModernPipeline()) return false
        val modernData = ground.modernVertexData ?: return false
        val stride = ground.modernVertexStride
        if (stride == 0) return false
        val activeDisplay = getDisplay() ?: return false
        if (isSpriteBatchActive) endSpriteBatch()

        var indexCount = 0
        var indexArray = groundIndexScratch
        val width = ground.width
        for (y in startY until endY) {
            var mi = y * width + startX
            for (x in startX until endX) {
                if (materialized[x - startX][y - startY]) {
                    val materials = ground.materialIndices[mi]
                    if (materials != null) {
                        if (indexCount + materials.size > indexArray.size) {
                            indexArray = indexArray.copyOf(maxOf(indexArray.size * 2, indexCount + materials.size))
                            groundIndexScratch = indexArray
                        }
                        System.arraycopy(materials, 0, indexArray, indexCount, materials.size)
                        indexCount += materials.size
                    }
                }
                mi++
            }
        }
        if (indexCount == 0) return true

        val w = activeDisplay.getWidth().toFloat()
        val h = activeDisplay.getHeight().toFloat()
        val tileUnits = ground.tileUnits
        val vp = minimapVp
        java.util.Arrays.fill(vp, 0f)
        vp[0] = 1024f / (128f * tileUnits * w)
        vp[9] = 1024f / (128f * tileUnits * h)
        vp[12] = -1f - ((1024f * startX) / 128f - (centerOffsetX * 2)) / w
        vp[13] = 1f - ((centerOffsetY * 2) + (1024f * endY) / 128f) / h
        vp[15] = 1f

        gr.setLegacySunParams(
            sunDirection[0], sunDirection[1], sunDirection[2],
            sunBrightness, sunColorIntensity, antiSunColorIntensity
        )
        gr.renderMinimapFloor(
            residentGroundVbo(ground, modernData), stride,
            ground.textureBuffer?.size?.toInt() ?: 2,
            (ground.waterFunctionMask and GroundFunctionMask.HD_LIGHT) != 0,
            indexArray, indexCount, vp
        )
        resetClientArrayCache()
        resyncLegacyTextureBinding()
        currentArrayBuffer = null
        currentElementsBuffer = null
        functionMask = 0
        return true
    }

    private val minimapVp = FloatArray(16)

    private var groundChunkVis = BooleanArray(0)
    private var groundChunkVisStamp = 0L
    private var groundChunkVisGround: GroundGL? = null
    private fun groundChunkVisible(ground: GroundGL, stamp: Long, tileIndices: IntArray, tileCount: Int, gw: Int, chunksX: Int): BooleanArray {
        if (stamp != 0L && stamp == groundChunkVisStamp && groundChunkVisGround === ground) return groundChunkVis
        val nChunks = chunksX * (((ground.materialIndices.size / gw) + 7) shr 3)
        if (groundChunkVis.size < nChunks) groundChunkVis = BooleanArray(nChunks) else java.util.Arrays.fill(groundChunkVis, false)
        val vis = groundChunkVis
        for (i in 0 until tileCount) {
            val t = tileIndices[i]
            val c = ((t % gw) shr 3) + ((t / gw) shr 3) * chunksX
            if (c < vis.size) vis[c] = true
        }
        groundChunkVisStamp = stamp
        groundChunkVisGround = ground
        return vis
    }
    private var groundVisBits = LongArray(0)
    private var groundVisBitsStamp = 0L
    private var groundVisBitsGround: GroundGL? = null
    private fun groundVisibleBits(ground: GroundGL, stamp: Long, tileIndices: IntArray, tileCount: Int): LongArray {
        if (stamp != 0L && stamp == groundVisBitsStamp && groundVisBitsGround === ground) return groundVisBits
        var maxIdx = 0
        for (i in 0 until tileCount) if (tileIndices[i] > maxIdx) maxIdx = tileIndices[i]
        val needed = maxOf((maxIdx ushr 6) + 1, (ground.materialIndices.size ushr 6) + 1)
        if (groundVisBits.size < needed) groundVisBits = LongArray(needed) else java.util.Arrays.fill(groundVisBits, 0L)
        val bits = groundVisBits
        for (i in 0 until tileCount) { val t = tileIndices[i]; bits[t ushr 6] = bits[t ushr 6] or (1L shl (t and 63)) }
        groundVisBitsStamp = stamp
        groundVisBitsGround = ground
        return bits
    }

    private val hdWater = HdWaterRenderer()
    private var hdWaterActive = false
    private val hdWaterViewProjection = FloatArray(16)
    private val hdWaterSunColour = FloatArray(3)

    private fun isHdWaterSurface(material: GroundMaterialGL, type: com.jagex.game.runetek5.config.material.MaterialType?): Boolean {
        if (!hdWaterActive || stillWaterToggled || type == null) return false
        return when (type.effectId.toInt()) {
            4, 8, 9 -> true
            2 -> material.waterTile.color != DEFAULT_OVERLAY_WATER_COLOUR && material.waterTile.color != com.jagex.game.runetek5.scene.tile.WaterTile().color
            else -> false
        }
    }

    private fun queueHdWater(
        material: GroundMaterialGL, type: com.jagex.game.runetek5.config.material.MaterialType?,
        vertexVbo: Int, stride: Int, texCoordSize: Int, colorVbo: Int, indexIbo: Int, indexCount: Int, viewProjection: Matrix44
    ) {
        val elements = viewProjection.elements ?: return
        val vp = hdWaterViewProjection
        for (i in 0..15) vp[i] = elements[i]
        vp[1] = -vp[1]
        vp[5] = -vp[5]
        vp[9] = -vp[9]
        vp[13] = -vp[13]
        val seconds = textureAnimatingProgression / 1000.0
        val period = HdWaterRenderer.FLOW_PERIOD.toDouble()
        val gr = groundRenderer
        hdWater.queue(
            vertexVbo, stride, texCoordSize, colorVbo, indexIbo, indexCount, vp, material.waterTile.color, material.waterTile.scale,
            gr?.waterShadowTexture() ?: 0, gr?.waterShadowScale() ?: 0f,
            ((type?.textureSpeedU?.toInt() ?: 0) * seconds * HD_WATER_FLOW).mod(period).toFloat(),
            ((type?.textureSpeedV?.toInt() ?: 0) * seconds * HD_WATER_FLOW).mod(period).toFloat(),
            material.modernWaterPlaneY
        )
    }

    private class GroundReplay {
        var vertexData: java.nio.ByteBuffer? = null
        var vertexVbo = 0
        var stride = 0
        var texCoordSize = 2
        var colorVbo = 0
        var indexIbo = 0
        var indexCount = 0
        var textureHandle = 0
        var textureScale = 1f
        var scrollU = 0f
        var scrollV = 0f
        var hasNormals = false
        var combineMode = 0
        var alphaFromVertex = false
        val water = com.jagex.game.runetek5.graphics.modern.renderer.GroundWater()
    }

    private val groundReplays = ArrayList<GroundReplay>()
    private var groundReplayCount = 0
    private val deferredWater = ArrayList<GroundReplay>()
    private var deferredWaterCount = 0
    private val mirroredViewProjection = Matrix44()
    private val mirroredViewProjectionGl = FloatArray(16)
    private val reflectionClipPlane = FloatArray(4)

    private fun recordGroundReplay(
        vertexData: java.nio.ByteBuffer, vertexVbo: Int, stride: Int, texCoordSize: Int, colorVbo: Int, indexIbo: Int, indexCount: Int,
        textureHandle: Int, textureScale: Float, scrollU: Float, scrollV: Float, hasNormals: Boolean, combineMode: Int, alphaFromVertex: Boolean
    ) {
        if (groundReplayCount == groundReplays.size) groundReplays.add(GroundReplay())
        fillReplay(groundReplays[groundReplayCount++], vertexData, vertexVbo, stride, texCoordSize, colorVbo, indexIbo, indexCount,
            textureHandle, textureScale, scrollU, scrollV, hasNormals, combineMode, alphaFromVertex)
    }

    private fun deferWater(
        vertexData: java.nio.ByteBuffer, vertexVbo: Int, stride: Int, texCoordSize: Int, colorVbo: Int, indexIbo: Int, indexCount: Int,
        textureHandle: Int, textureScale: Float, scrollU: Float, scrollV: Float, hasNormals: Boolean, combineMode: Int, alphaFromVertex: Boolean,
        water: com.jagex.game.runetek5.graphics.modern.renderer.GroundWater
    ) {
        if (deferredWaterCount == 0) captureDeferredWaterState()
        if (deferredWaterCount == deferredWater.size) deferredWater.add(GroundReplay())
        val r = deferredWater[deferredWaterCount++]
        fillReplay(r, vertexData, vertexVbo, stride, texCoordSize, colorVbo, indexIbo, indexCount,
            textureHandle, textureScale, scrollU, scrollV, hasNormals, combineMode, alphaFromVertex)
        water.copyInto(r.water)
    }

    private val deferredWaterSun = FloatArray(6)
    private val deferredWaterFog = FloatArray(5)
    private var deferredWaterFogEnabled = false

    private fun captureDeferredWaterState() {
        deferredWaterSun[0] = sunDirection[0]
        deferredWaterSun[1] = sunDirection[1]
        deferredWaterSun[2] = sunDirection[2]
        deferredWaterSun[3] = sunColorRed
        deferredWaterSun[4] = sunColorGreen
        deferredWaterSun[5] = sunColorBlue
        deferredWaterFogEnabled = fogEnabled && fogDensity > 0
        deferredWaterFog[0] = fogStartPosition.toFloat()
        deferredWaterFog[1] = fogEndPosition.toFloat()
        deferredWaterFog[2] = (fogColor and 0xff0000) / 16711680f
        deferredWaterFog[3] = (fogColor and 0xff00) / 65280f
        deferredWaterFog[4] = (fogColor and 0xff) / 255f
    }

    private fun drawDeferredWater(defaultWidth: Int, defaultHeight: Int) {
        if (deferredWaterCount == 0) return
        val gr = groundRenderer
        if (gr == null) {
            deferredWaterCount = 0
            return
        }
        gr.closeGroundBatch()
        val msaa = if (sceneMsaaBound) sceneMsaa else null
        val width = msaa?.width ?: defaultWidth
        val height = msaa?.height ?: defaultHeight
        val sun = deferredWaterSun
        val liveDir = Vector3f(sun[0], sun[1], sun[2])
        if (liveDir.lengthSquared() < 0.001f) liveDir.set(0.5f, 1f, 0.3f)
        liveDir.normalize()
        gr.setLighting(liveDir, Vector3f(sun[3], sun[4], sun[5]), 0.4f, 0.6f)
        val fog = deferredWaterFog
        if (deferredWaterFogEnabled) gr.setFog(true, fog[0], fog[1], fog[2], fog[3], fog[4]) else gr.setFog(false, 0f, 0f, 0f, 0f, 0f)
        gr.setDepthFog(hdWater.copySceneDepth(width, height), width, height)
        try {
            for (i in 0 until deferredWaterCount) {
                val r = deferredWater[i]
                val data = r.vertexData ?: continue
                gr.renderWithModernData(
                    vertexData = data,
                    stride = r.stride,
                    texCoordSize = r.texCoordSize,
                    indexShorts = groundIndexScratch,
                    indexCount = r.indexCount,
                    textureHandle = r.textureHandle,
                    textureScale = r.textureScale,
                    textureScrollU = r.scrollU,
                    textureScrollV = r.scrollV,
                    hasNormals = r.hasNormals,
                    viewProjection = viewProjectionMatrix,
                    colorVboId = r.colorVbo,
                    combineMode = r.combineMode,
                    alphaFromVertex = r.alphaFromVertex,
                    residentVertexVbo = r.vertexVbo,
                    residentIndexIbo = r.indexIbo,
                    water = r.water
                )
                r.vertexData = null
            }
            gr.closeGroundBatch()
        } finally {
            gr.setDepthFog(0, 0, 0)
            deferredWaterCount = 0
            resetClientArrayCache()
            currentArrayBuffer = null
            currentElementsBuffer = null
        }
    }

    private fun fillReplay(
        r: GroundReplay, vertexData: java.nio.ByteBuffer, vertexVbo: Int, stride: Int, texCoordSize: Int, colorVbo: Int, indexIbo: Int, indexCount: Int,
        textureHandle: Int, textureScale: Float, scrollU: Float, scrollV: Float, hasNormals: Boolean, combineMode: Int, alphaFromVertex: Boolean
    ) {
        r.vertexData = vertexData
        r.vertexVbo = vertexVbo
        r.stride = stride
        r.texCoordSize = texCoordSize
        r.colorVbo = colorVbo
        r.indexIbo = indexIbo
        r.indexCount = indexCount
        r.textureHandle = textureHandle
        r.textureScale = textureScale
        r.scrollU = scrollU
        r.scrollV = scrollV
        r.hasNormals = hasNormals
        r.combineMode = combineMode
        r.alphaFromVertex = alphaFromVertex
    }

    private fun samplePlaneY(data: java.nio.ByteBuffer, stride: Int, indices: ShortArray, count: Int): Float {
        val step = maxOf(1, count / 64)
        var sum = 0f
        var n = 0
        var i = 0
        while (i < count) {
            val at = (indices[i].toInt() and 0xffff) * stride + 4
            if (at + 4 <= data.limit()) {
                sum += data.getFloat(at)
                n++
            }
            i += step
        }
        return if (n == 0) Float.NaN else sum / n
    }

    private fun renderWaterReflection(width: Int, height: Int) {
        val plane = hdWater.dominantPlane()
        if (!hdWater.beginReflection(width, height, fogColor, plane)) return
        try {
            val source = viewProjectionMatrix.elements
            val mirrored = mirroredViewProjection.elements
            for (i in 0..15) mirrored[i] = source[i]
            for (c in 0..3) {
                mirrored[4 + c] = -source[4 + c]
                mirrored[12 + c] = source[12 + c] + 2f * plane * source[4 + c]
            }
            val gl = mirroredViewProjectionGl
            for (i in 0..15) gl[i] = mirrored[i]
            gl[1] = -gl[1]
            gl[5] = -gl[5]
            gl[9] = -gl[9]
            gl[13] = -gl[13]
            val clip = reflectionClipPlane
            clip[0] = 0f
            clip[1] = -1f
            clip[2] = 0f
            clip[3] = plane + REFLECTION_CLIP_BIAS

            val gr = groundRenderer
            if (gr != null) {
                gr.setClipPlane(clip[0], clip[1], clip[2], clip[3])
                for (i in 0 until groundReplayCount) {
                    val r = groundReplays[i]
                    val data = r.vertexData ?: continue
                    gr.renderWithModernData(
                        vertexData = data,
                        stride = r.stride,
                        texCoordSize = r.texCoordSize,
                        indexShorts = groundIndexScratch,
                        indexCount = r.indexCount,
                        textureHandle = r.textureHandle,
                        textureScale = r.textureScale,
                        textureScrollU = r.scrollU,
                        textureScrollV = r.scrollV,
                        hasNormals = r.hasNormals,
                        viewProjection = mirroredViewProjection,
                        colorVboId = r.colorVbo,
                        combineMode = r.combineMode,
                        alphaFromVertex = r.alphaFromVertex,
                        residentVertexVbo = r.vertexVbo,
                        residentIndexIbo = r.indexIbo
                    )
                }
                gr.setClipPlane(0f, 0f, 0f, 0f)
            }
            if (useInstancing) instancedBatcher?.drawReflection(gl, clip)
            optimizedBatcher?.drawReflection(gl, clip)
        } finally {
            hdWater.endReflection()
            resetClientArrayCache()
            currentArrayBuffer = null
            currentElementsBuffer = null
        }
    }

    private fun drawHdWater(defaultWidth: Int, defaultHeight: Int) {
        hdWaterActive = HdWaterRenderer.AVAILABLE && HdWaterRenderer.selected()
        drawDeferredWater(defaultWidth, defaultHeight)
        if (!hdWater.hasQueued) {
            groundReplayCount = 0
            return
        }
        groundRenderer?.closeGroundBatch()
        val msaa = if (sceneMsaaBound) sceneMsaa else null
        if (HdWaterRenderer.PLANAR) renderWaterReflection(msaa?.width ?: defaultWidth, msaa?.height ?: defaultHeight)
        groundReplayCount = 0
        hdWaterSunColour[0] = sunColorRed
        hdWaterSunColour[1] = sunColorGreen
        hdWaterSunColour[2] = sunColorBlue
        hdWater.render(
            msaa?.width ?: defaultWidth,
            msaa?.height ?: defaultHeight,
            sunDirection,
            hdWaterSunColour,
            sunBrightness,
            fogColor,
            effectManager?.textureContainer?.texture3d?.activeHandle ?: 0,
            effectManager?.textureContainer?.billowTexture3d?.activeHandle ?: 0,
            getCubeMap()?.activeHandle ?: 0,
            (textureAnimatingProgression % 4000) / 4000f,
            (textureAnimatingProgression % 32000) / 32000f,
            fogEnabled && fogDensity > 0,
            fogStartPosition,
            fogEndPosition,
            groundRenderer?.waterPointLightCount ?: 0,
            groundRenderer?.waterPointLightPositions ?: FloatArray(0),
            groundRenderer?.waterPointLightColours ?: FloatArray(0)
        )
    }

    private val groundWaterScratch = com.jagex.game.runetek5.graphics.modern.renderer.GroundWater()
    private var lapNoiseTable: FloatArray? = null
    private val lapNoiseFrame = FloatArray(128)
    private var lapNoiseProgression = Int.MIN_VALUE

    private fun groundWater(material: GroundMaterialGL, ground: GroundGL, type: com.jagex.game.runetek5.config.material.MaterialType?): com.jagex.game.runetek5.graphics.modern.renderer.GroundWater {
        val water = groundWaterScratch
        val progression = textureAnimatingProgression
        if (stillWaterToggled && hdWaterActive) {
            water.mode = 0
            return water
        }
        if (stillWaterToggled) {
            val tile = material.waterTile
            val fogFar = fogFarDistance
            val fogNear = fogMinimumStartPosition
            water.mode = 3
            water.stillFog[0] = fogFar - (fogFar - fogNear) * 0.25f
            water.stillFog[1] = fogFar - (fogFar - fogNear) * 0.125f
            water.stillFog[2] = 256f / tile.scale
            water.stillFog[3] = tile.intensity / 255f
            water.colour = tile.color
            water.noiseTexture = 0
            return water
        }
        var effect = type?.effectId?.toInt() ?: 0
        var param1 = type?.effectParam1?.toInt() ?: 0
        val param2: Int
        val stillWaterAllowed = (ground.waterFunctionMask and 0x8) != 0
        if (effect == 8 || effect == 9) effect = 4
        if (effect == 4 && !stillWaterAllowed) {
            effect = 2
            param2 = param1
        } else {
            param2 = type?.effectParam2?.toInt() ?: 0
        }
        val noise = effectManager?.textureContainer?.texture3d?.activeHandle ?: 0
        when (effect) {
            4 -> {
                water.mode = 4
                water.stillFog[2] = 256f / material.waterTile.scale
                water.stillFog[3] = material.waterTile.intensity / 255f
                water.colour = material.waterTile.color
                water.noiseTexture = noise
                water.time = if ((param1 and 0x1) == 1) (progression % 4000) / 4000f else 0f
                water.rippleFogStart = fogStartPosition
                water.rippleFogDensity = if (fogDensity > 0) fogDensity.toFloat() else 0f
            }
            2 -> {
                water.mode = 2
                water.noiseTexture = if ((param1 and 0x80) == 0) noise else 0
                water.time = if ((param2 and 0x1) == 1) (progression % 4000) / 4000f else 0f
                if ((param1 and 0x40) == 0) {
                    water.lapAmbient[0] = sunBrightness * sunColorRed
                    water.lapAmbient[1] = sunBrightness * sunColorGreen
                    water.lapAmbient[2] = sunBrightness * sunColorBlue
                } else {
                    water.lapAmbient.fill(1f)
                }
                water.turbulence = when (param1 and 0x3) { 2 -> 0.05f; 3 -> 0.1f; else -> 0.025f }
                water.lapNoise = lapNoise(progression)
            }
            else -> water.mode = 0
        }
        return water
    }

    private fun lapNoise(progression: Int): FloatArray {
        val table = lapNoiseTable ?: FloatArray(32768).also { noise ->
            val mapX = com.jagex.game.runetek5.config.material.texture.operation.TextureOpPerlinNoise.generateNoiseMap()
            val mapY = com.jagex.game.runetek5.config.material.texture.operation.TextureOpPerlinNoise.generateNoiseMap()
            var offset = 0
            for (index in 0 until 256) {
                for (i in 0 until 64) {
                    noise[offset++] = mapX[index][i] / 4096f
                    noise[offset++] = mapY[index][i] / 4096f
                }
            }
            lapNoiseTable = noise
        }
        if (progression != lapNoiseProgression) {
            val start = progression % 5000 * 128 / 5000
            System.arraycopy(table, start, lapNoiseFrame, 0, 128)
            lapNoiseProgression = progression
        }
        return lapNoiseFrame
    }

    fun renderGroundMaterialModern(
        material: GroundMaterialGL,
        ground: GroundGL,
        tileIndices: IntArray,
        tileCount: Int,
        tileFunctionMasks: IntArray,
        materialId: Int,
        textureScale: Float,
        hasNormals: Boolean
    ): Boolean {
        val groundStartNs = System.nanoTime()
        try {
        groundRenderer?.let { g ->
            val liveDir = Vector3f(sunDirection[0], sunDirection[1], sunDirection[2])
            if (liveDir.lengthSquared() < 0.001f) liveDir.set(0.5f, 1f, 0.3f)
            liveDir.normalize()
            g.setLighting(liveDir, Vector3f(sunColorRed, sunColorGreen, sunColorBlue), 0.4f, 0.6f)
            if (fogEnabled && fogDensity > 0) {
                g.setFog(true, fogStartPosition, fogEndPosition,
                    (fogColor and 0xff0000) / 16711680f, (fogColor and 0xff00) / 65280f, (fogColor and 0xff) / 255f)
            } else {
                g.setFog(false, 0f, 0f, 0f, 0f, 0f)
            }
        }
        diagGroundCalls++
        val gr = groundRenderer ?: return false
        if (!canUseModernPipeline()) {
            diagGroundNoPipeline++
            return false
        }

        val modernData = groundModernVertexData(ground)
        val stride = groundModernVertexStride(ground)
        if (modernData == null || stride == 0) {
            diagGroundNoSnapshot++
            return false
        }
        diagGroundDrawn++

        if (isSpriteBatchActive) endSpriteBatch()
        ensureSceneTarget()

        val stamp = ground.modernVisibleStamp
        var indexCount: Int
        val indexArray: ShortArray
        var residentIbo = 0
        if (stamp != 0L && material.modernIndexStamp == stamp && material.modernIndexCount >= 0 &&
            (material.modernIndexCount == 0 || material.modernIboId != 0)
        ) {
            indexCount = material.modernIndexCount
            indexArray = groundIndexScratch
            residentIbo = material.modernIboId
        } else {
            indexCount = 0
            val maxIndices = maxOf(tileCount, ground.materialIndices.size) * 18
            if (groundIndexScratch.size < maxIndices) {
                groundIndexScratch = ShortArray(maxIndices)
            }
            indexArray = groundIndexScratch
            val gw = ground.width
            val chunksX = (gw + 7) shr 3
            val chunkVis = groundChunkVisible(ground, stamp, tileIndices, tileCount, gw, chunksX)
            val visBits = groundVisibleBits(ground, stamp, tileIndices, tileCount)
            var chunkLists = material.modernChunkIdx
            var chunkTiles = material.modernChunkTiles
            var chunkTileEnd = material.modernChunkTileEnd
            if (chunkLists == null || chunkTiles == null || chunkTileEnd == null ||
                material.modernTilesMaskSize != tileFunctionMasks.size || material.modernChunkRevision != ground.modernVertexRevision
            ) {
                val nTiles = tileFunctionMasks.size
                val nChunks = chunksX * (((nTiles / gw) + 7) shr 3)
                val counts = IntArray(nChunks)
                val tileCounts = IntArray(nChunks)
                for (t in 0 until nTiles) {
                    val mask = tileFunctionMasks[t]
                    if (mask == 0) continue
                    val tri = ground.materialIndices[t] ?: continue
                    val c = ((t % gw) shr 3) + ((t / gw) shr 3) * chunksX
                    val triangleCount = tri.size / 3
                    val allSelected = (1 shl triangleCount) - 1
                    if ((mask and allSelected) == allSelected) counts[c] += tri.size
                    else for (k in 0 until triangleCount) if ((mask and (1 shl k)) != 0) counts[c] += 3
                    tileCounts[c]++
                }
                val lists = arrayOfNulls<ShortArray>(nChunks)
                val tiles = arrayOfNulls<IntArray>(nChunks)
                val ends = arrayOfNulls<IntArray>(nChunks)
                val fill = IntArray(nChunks)
                val tileFill = IntArray(nChunks)
                for (c in 0 until nChunks) if (counts[c] > 0) {
                    lists[c] = ShortArray(counts[c])
                    tiles[c] = IntArray(tileCounts[c])
                    ends[c] = IntArray(tileCounts[c])
                }
                for (t in 0 until nTiles) {
                    val mask = tileFunctionMasks[t]
                    if (mask == 0) continue
                    val tri = ground.materialIndices[t] ?: continue
                    val c = ((t % gw) shr 3) + ((t / gw) shr 3) * chunksX
                    val dst = lists[c] ?: continue
                    val triangleCount = tri.size / 3
                    val allSelected = (1 shl triangleCount) - 1
                    if ((mask and allSelected) == allSelected) {
                        System.arraycopy(tri, 0, dst, fill[c], tri.size)
                        fill[c] += tri.size
                    } else {
                        var v = 0
                        for (k in 0 until triangleCount) {
                            if ((mask and (1 shl k)) != 0) {
                                dst[fill[c]] = tri[v]; dst[fill[c] + 1] = tri[v + 1]; dst[fill[c] + 2] = tri[v + 2]
                                fill[c] += 3
                            }
                            v += 3
                        }
                    }
                    tiles[c]!![tileFill[c]] = t
                    ends[c]!![tileFill[c]] = fill[c]
                    tileFill[c]++
                }
                chunkLists = lists
                chunkTiles = tiles
                chunkTileEnd = ends
                material.modernChunkIdx = lists
                material.modernChunkTiles = tiles
                material.modernChunkTileEnd = ends
                material.modernTilesMaskSize = nTiles
                material.modernChunkRevision = ground.modernVertexRevision
            }
            for (c in chunkLists.indices) {
                if (c >= chunkVis.size || !chunkVis[c]) continue
                val l = chunkLists[c] ?: continue
                val tl = chunkTiles[c] ?: continue
                val te = chunkTileEnd[c] ?: continue
                var runStart = -1
                var sliceStart = 0
                for (k in tl.indices) {
                    val t = tl[k]
                    val visible = (visBits[t ushr 6] and (1L shl (t and 63))) != 0L
                    if (visible) {
                        if (runStart < 0) runStart = sliceStart
                    } else if (runStart >= 0) {
                        val len = sliceStart - runStart
                        if (indexCount + len > indexArray.size) break
                        System.arraycopy(l, runStart, indexArray, indexCount, len)
                        indexCount += len
                        runStart = -1
                    }
                    sliceStart = te[k]
                }
                if (runStart >= 0) {
                    val len = sliceStart - runStart
                    if (indexCount + len <= indexArray.size) {
                        System.arraycopy(l, runStart, indexArray, indexCount, len)
                        indexCount += len
                    }
                }
            }
            material.modernIndexStamp = stamp
            material.modernIndexCount = indexCount
            if (indexCount > 0 && HdWaterRenderer.PLANAR) material.modernWaterPlaneY = samplePlaneY(modernData, stride, indexArray, indexCount)
            if (indexCount > 0) {
                if (material.modernIboId == 0) material.modernIboId = glGenBuffers()
                val needed = indexCount * 2
                var staging = groundIndexUpload
                if (staging == null || staging.capacity() < needed) {
                    staging?.let { MemoryUtil.memFree(it) }
                    staging = MemoryUtil.memAlloc(maxOf(needed, 65536))
                    groundIndexUpload = staging
                }
                staging.clear()
                staging.asShortBuffer().put(indexArray, 0, indexCount)
                staging.limit(needed)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, material.modernIboId)
                if (needed > material.modernIboCapacity) {
                    glBufferData(GL_ELEMENT_ARRAY_BUFFER, staging, GL_DYNAMIC_DRAW)
                    material.modernIboCapacity = needed
                } else {
                    glBufferData(GL_ELEMENT_ARRAY_BUFFER, material.modernIboCapacity.toLong(), GL_DYNAMIC_DRAW)
                    glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, staging)
                }
                residentIbo = material.modernIboId
            }
        }

        if (indexCount == 0) return true

        val texCoordSize = ground.textureBuffer?.size?.toInt() ?: 2

        val textureHandle = lookupTextureHandle(materialId)
        val groundMaterial = if (materialId >= 0) materialProvider?.getMaterial(materialId) else null
        val textureTime = TextureScroll.seconds(textureAnimatingProgression)
        val textureScrollU = if (groundMaterial != null) TextureScroll.offset(textureTime, groundMaterial.textureSpeedU) else 0f
        val textureScrollV = if (groundMaterial != null) TextureScroll.offset(textureTime, groundMaterial.textureSpeedV) else 0f

        val vpMatrix = viewProjectionMatrix

        val colorBuffer = material.arrayBufferPointer?.buffer
        var colorVboId = 0
        var colorData: java.nio.ByteBuffer? = null
        if (colorBuffer != null) {
            colorVboId = colorBuffer.getId()
            if (colorVboId == 0) {
                val addr = colorBuffer.getBufferAddress()
                val vertexCount = ground.modernVertexCount
                if (addr != 0L && vertexCount > 0) {
                    colorData = MemoryUtil.memByteBuffer(addr, vertexCount * 4)
                }
            }
        }

        val combineMode = if (GROUND_COLOR_DEBUG) {
            99
        } else if (GROUND_COMBINE_OVERRIDE >= 0) {
            GROUND_COMBINE_OVERRIDE
        } else if (materialId >= 0) {
            materialProvider?.getMaterial(materialId)?.combineMode ?: 0
        } else {
            0
        }

        gr.setLegacySunParams(
            sunDirection[0], sunDirection[1], sunDirection[2],
            sunBrightness, sunColorIntensity, antiSunColorIntensity
        )
        if (GROUND_PROBE && groundProbeTick++ % 1200 < 40) {
            val tp = IntArray(1)
            val info = if (textureHandle > 0) {
                org.lwjgl.opengl.GL33.glBindTexture(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, textureHandle)
                val sb = StringBuilder()
                for ((name, pname) in listOf(
                    "minF" to org.lwjgl.opengl.GL33.GL_TEXTURE_MIN_FILTER, "magF" to org.lwjgl.opengl.GL33.GL_TEXTURE_MAG_FILTER,
                    "maxLvl" to org.lwjgl.opengl.GL33.GL_TEXTURE_MAX_LEVEL, "baseLvl" to org.lwjgl.opengl.GL33.GL_TEXTURE_BASE_LEVEL
                )) { org.lwjgl.opengl.GL33.glGetTexParameteriv(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, pname, tp); sb.append(" $name=${tp[0]}") }
                val fb = FloatArray(1)
                org.lwjgl.opengl.GL33.glGetTexParameterfv(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, org.lwjgl.opengl.GL33.GL_TEXTURE_LOD_BIAS, fb); sb.append(" lodBias=${fb[0]}")
                org.lwjgl.opengl.GL33.glGetTexLevelParameteriv(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL33.GL_TEXTURE_INTERNAL_FORMAT, tp); sb.append(" fmt=${tp[0]}")
                org.lwjgl.opengl.GL33.glGetTexLevelParameteriv(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, 0, org.lwjgl.opengl.GL33.GL_TEXTURE_WIDTH, tp); sb.append(" w=${tp[0]}")
                org.lwjgl.opengl.GL33.glGetTexLevelParameteriv(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, 1, org.lwjgl.opengl.GL33.GL_TEXTURE_WIDTH, tp); sb.append(" w1=${tp[0]}")
                for (lvl in 0 until 4) {
                    org.lwjgl.opengl.GL33.glGetTexLevelParameteriv(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, lvl, org.lwjgl.opengl.GL33.GL_TEXTURE_WIDTH, tp)
                    val w = tp[0]
                    if (w <= 0) { sb.append(" L$lvl=none"); continue }
                    val buf = java.nio.ByteBuffer.allocateDirect(w * w * 4).order(java.nio.ByteOrder.nativeOrder())
                    org.lwjgl.opengl.GL33.glGetTexImage(org.lwjgl.opengl.GL33.GL_TEXTURE_2D, lvl, org.lwjgl.opengl.GL33.GL_RGBA, org.lwjgl.opengl.GL33.GL_UNSIGNED_BYTE, buf)
                    var r = 0L; var g = 0L; var b = 0L; var a = 0L
                    for (k in 0 until w * w) { r += buf.get(k * 4).toInt() and 0xff; g += buf.get(k * 4 + 1).toInt() and 0xff; b += buf.get(k * 4 + 2).toInt() and 0xff; a += buf.get(k * 4 + 3).toInt() and 0xff }
                    val n = (w * w).toLong()
                    sb.append(" L$lvl=${w}px(${r / n},${g / n},${b / n},${a / n})")
                }
                sb.toString()
            } else ""
            System.err.println("[GroundDraw] modern mat=$materialId indices=$indexCount tiles=$tileCount tex=$textureHandle combine=$combineMode scale=$textureScale$info")
        }
        if (GROUND_PROBE && (materialId == 505 || materialId == 301) && groundProbeTick % 300 < 2) for (v0 in intArrayOf(1, 54, 838, 1778, indexArray[0].toInt() and 0xffff)) {
            val col = ByteArray(4)
            if (colorVboId != 0) {
                org.lwjgl.opengl.GL33.glBindBuffer(org.lwjgl.opengl.GL33.GL_ARRAY_BUFFER, colorVboId)
                val bb = java.nio.ByteBuffer.allocateDirect(4).order(java.nio.ByteOrder.nativeOrder())
                org.lwjgl.opengl.GL33.glGetBufferSubData(org.lwjgl.opengl.GL33.GL_ARRAY_BUFFER, v0.toLong() * 4, bb)
                bb.get(col)
            } else if (colorData != null) {
                for (k in 0 until 4) col[k] = colorData.get(v0 * 4 + k)
            }
            val base = v0 * stride
            val px = modernData.getFloat(base); val py = modernData.getFloat(base + 4); val pz = modernData.getFloat(base + 8)
            val nOff = 16 + texCoordSize * 4
            val nx = modernData.getFloat(base + nOff); val ny = modernData.getFloat(base + nOff + 4); val nz = modernData.getFloat(base + nOff + 8)
            System.err.println(
                "[GroundProbe] modern mat=$materialId combine=$combineMode hasNormals=$hasNormals v0=$v0 " +
                    "colour=(${col[0].toInt() and 0xff},${col[1].toInt() and 0xff},${col[2].toInt() and 0xff},${col[3].toInt() and 0xff}) " +
                    "pos=($px,$py,$pz) normal=($nx,$ny,$nz) sunB=$sunBrightness lights=$frameLightCount"
            )
        }

        if (verifyLiveGeometry) GeometryDiff.recordGround(materialId, combineMode, colorVboId, colorData != null)

        if (colorVboId != 0 && residentIbo != 0 && isHdWaterSurface(material, groundMaterial)) {
            queueHdWater(material, groundMaterial, residentGroundVbo(ground, modernData), stride, texCoordSize, colorVboId, residentIbo, indexCount, vpMatrix)
            return true
        }

        if (hdWaterActive && HdWaterRenderer.PLANAR && colorVboId != 0 && residentIbo != 0) {
            recordGroundReplay(
                modernData, residentGroundVbo(ground, modernData), stride, texCoordSize, colorVboId, residentIbo, indexCount,
                textureHandle, 1.0f / textureScale, textureScrollU, textureScrollV, hasNormals, combineMode,
                materialId >= 0 && !MaterialTypeList.isOpaqueEffect(materialProvider?.getMaterial(materialId)?.effectId?.toInt() ?: 0)
            )
        }

        val water = groundWater(material, ground, groundMaterial)
        val alphaFromVertex = materialId >= 0 && !MaterialTypeList.isOpaqueEffect(materialProvider?.getMaterial(materialId)?.effectId?.toInt() ?: 0)
        if (water.mode == 4 && useOptimizedBatcher && colorVboId != 0 && residentIbo != 0) {
            deferWater(
                modernData, residentGroundVbo(ground, modernData), stride, texCoordSize, colorVboId, residentIbo, indexCount,
                textureHandle, 1.0f / textureScale, textureScrollU, textureScrollV, hasNormals, combineMode, alphaFromVertex, water
            )
            return true
        }

        gpuTimer.begin(GPU_PASS_GROUND)
        gr.renderWithModernData(
            vertexData = modernData,
            stride = stride,
            texCoordSize = texCoordSize,
            indexShorts = indexArray,
            indexCount = indexCount,
            textureHandle = textureHandle,
            textureScale = 1.0f / textureScale,
            textureScrollU = textureScrollU,
            textureScrollV = textureScrollV,
            hasNormals = hasNormals,
            viewProjection = vpMatrix,
            colorVboId = colorVboId,
            colorData = colorData,
            combineMode = combineMode,
            alphaFromVertex = alphaFromVertex,
            residentVertexVbo = residentGroundVbo(ground, modernData),
            residentIndexIbo = residentIbo,
            water = water
        )
        gpuTimer.end(GPU_PASS_GROUND)
        resetClientArrayCache()
        currentArrayBuffer = null
        currentElementsBuffer = null
        return true
        } finally {
            phaseGroundNs += System.nanoTime() - groundStartNs
            phaseGroundCalls++
        }
    }
}
