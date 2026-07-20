package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.graphics.modern.batch.FrameBatcher
import com.jagex.game.runetek5.graphics.modern.batch.InstancedModelBatcher
import com.jagex.game.runetek5.graphics.modern.batch.ModelCache
import com.jagex.game.runetek5.graphics.modern.batch.OptimizedFrameBatcher
import org.lwjgl.system.MemoryUtil
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.modern.renderer.ModernFontRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernGroundRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernModelRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernParticleRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernSpriteRenderer
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.sprite.ModernSpriteGL
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteGL
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.MeshConverter
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.opengl.GroundMaterialGL
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL33.*
import java.awt.Canvas

/**
 * Modern OpenGL 3.3 Core toolkit for rendering.
 *
 * This toolkit extends OpenGlToolkit to provide a modern shader-based
 * rendering pipeline for 3D scene content, while reusing the existing
 * 2D/UI rendering from the parent class.
 *
 * Key features:
 * - Shader-based rendering (no fixed-function pipeline)
 * - VAO/VBO for vertex management
 * - Proper lighting and fog through uniforms
 * - Compatible with the existing scene architecture
 */
class ModernGLToolkit(
    canvas: Canvas,
    materials: MaterialProvider,
    multiSamples: Int
) : OpenGlToolkit(canvas, materials, multiSamples) {

    // Modern renderer for 3D models
    private var modernRenderer: ModernModelRenderer? = null
    private var groundRenderer: ModernGroundRenderer? = null
    private var spriteRenderer: ModernSpriteRenderer? = null
    private var particleRendererModern: ModernParticleRenderer? = null
    private var frameBatcher: FrameBatcher? = null
    private var optimizedBatcher: OptimizedFrameBatcher? = null
    private var instancedBatcher: InstancedModelBatcher? = null
    private var useOptimizedBatcher = true  // Toggle for A/B testing
    private var useInstancing = true  // Enable model instancing for repeated meshes
    private var isModernRendererInitialized = false

    // Thread safety: LWJGL3 capabilities are thread-local. The modern pipeline
    // should only run on the owner thread (the thread that created this toolkit,
    // which is the main game loop thread). Other threads (e.g. the login screen
    // renderer thread) fall back to the legacy jaggl path.
    private val ownerThreadId: Long = Thread.currentThread().id
    @Volatile private var ownerThreadHasCapabilities = false

    // ===== Gate diagnostics (cumulative; printed and reset in the [ModernDiag] line) =====
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

    /**
     * Per-subsystem counters for draws that were SKIPPED because the modern pipeline
     * could not handle them and legacy fallbacks are forbidden under this toolkit.
     * Printed (and cleared) with the [ModernDiag] line — every nonzero entry is a
     * parity gap that needs a modern implementation.
     */
    private val legacySkipCounters = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /** Records a draw that was dropped instead of falling back to legacy GL. */
    fun countLegacySkip(subsystem: String) {
        legacySkipCounters.merge(subsystem, 1L, Long::plus)
    }

    /** Returns true if the modern LWJGL3 pipeline can be used on the current thread. */
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

        // NOTE: offscreen DirectDisplay targets (minimap/worldmap compositors pushed
        // via setDisplay) are supported: the sprite batch sizes its ortho/viewport
        // from the CURRENT render target (`display`), and the setDisplay/resizeDisplay
        // overrides flush all pending batches at target boundaries so nothing leaks
        // across the FBO push/pop.

        // NOTE: no gameState gate. Legacy fallbacks are forbidden under this toolkit,
        // so gating on LOGGED_INGAME would blank the lobby/loading UI outright. The
        // login screen's own renderer runs on a separate thread and is already
        // rejected by the owner-thread check above.
        lastPipelineRejectReason = "none"
        return true
    }

    // Sprite batching state
    private var isSpriteBatchActive = false

    // Scratch index buffer for ground material draws (reused across materials/frames)
    private var groundIndexScratch = ByteArray(0)

    // Texture cache for material ID to GL handle lookup
    private val textureHandleCache = mutableMapOf<Int, Int>()

    // Model cache for avoiding per-face CPU work
    private var modelCache: ModelCache? = null

    // Matrices
    private val modernProjectionMatrix = Matrix4f()
    private val viewMatrix = Matrix4f()
    private val modelMat = Matrix4f()

    // Camera state from toolkit
    private var camX = 0f
    private var camY = 0f
    private var camZ = 0f
    private var camYaw = 0f
    private var camPitch = 0f

    // Debug/stats
    private var frameCounter = 0
    private var modelCounter = 0
    private var totalModelsProcessed = 0L  // Cumulative counter for MCP debugging
    private var totalFacesProcessed = 0L   // Cumulative counter for MCP debugging
    private var lastBatcherStatus = "not checked"  // Debug status for MCP

    companion object {
        /**
         * Creates a modern GL toolkit with the specified settings.
         */
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

    /**
     * Bloom is implemented by the legacy FramebufferCompositor (immediate-mode
     * fullscreen quads + FBO ping-pong through jaggl). That path is forbidden under
     * the standalone modern renderer, so report no bloom support until a modern
     * post-processing pass exists (parity gap).
     */
    override fun hasBloomSupport(): Boolean = false

    override fun initialize() {
        super.initialize()
        // EAGER modern init. Renderers.prepareForRendering() constructs this toolkit
        // and calls initialize() on the same GL thread with the jaggl context current,
        // BEFORE Client.loadAssets creates the UI sprites/fonts (StaticMedia.loadSprites,
        // FontManager). Initializing here — instead of lazily after the first render —
        // guarantees createNativeSprite/createFontRenderer can hand out modern
        // implementations from the very first asset, so nothing is ever stuck on the
        // (forbidden) legacy SpriteGL/FontRendererGL draw paths.
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

    /**
     * KHR_debug (removable diagnostic): the driver explains, in prose, exactly which
     * GL call is invalid and why — converts anonymous 0x502s into named causes.
     * Deduplicated by message so the log stays readable; synchronous so the Java
     * stack at the print is the offending call's stack.
     */
    private val seenDebugMessages = HashSet<String>()
    private fun installGlDebugCallback() {
        try {
            val caps = GL.getCapabilities()
            if (!caps.GL_KHR_debug) {
                println("[ModernGLToolkit] KHR_debug unavailable")
                return
            }
            glEnable(org.lwjgl.opengl.KHRDebug.GL_DEBUG_OUTPUT)
            glEnable(org.lwjgl.opengl.KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS)
            org.lwjgl.opengl.KHRDebug.glDebugMessageCallback({ _, type, _, severity, length, message, _ ->
                if (type == org.lwjgl.opengl.KHRDebug.GL_DEBUG_TYPE_ERROR ||
                    severity == org.lwjgl.opengl.KHRDebug.GL_DEBUG_SEVERITY_HIGH
                ) {
                    val msg = org.lwjgl.system.MemoryUtil.memUTF8(message, length)
                    if (seenDebugMessages.add(msg) && seenDebugMessages.size <= 20) {
                        println("[GLDebug] $msg")
                        Thread.dumpStack()
                    }
                }
            }, 0L)
            println("[ModernGLToolkit] KHR_debug callback installed")
        } catch (e: Throwable) {
            println("[ModernGLToolkit] KHR_debug install failed: $e")
        }
    }

    override fun generateFlags() {
        super.generateFlags()
        // ROOT CAUSE of the invisible UI (driver: "Target doesn't match the texture's
        // target"): with this flag set, legacy sprite textures (OpenGl2DTexture_Sub1)
        // are created as GL_TEXTURE_RECTANGLE, but the modern sprite pipeline binds
        // everything as GL_TEXTURE_2D — every bind raised GL_INVALID_OPERATION and
        // every UI quad sampled nothing. Force the legacy power-of-two/TEXTURE_2D
        // fallback path (the same one 2012 low-end GPUs used) so sprite textures
        // match the modern renderer's target. Material/font textures were already
        // TEXTURE_2D, which is why the 3D world and glyphs rendered while all
        // sprites were invisible.
        this.arbRectangleTexturesSupported = false
    }

    private fun initializeModernRenderer() {
        if (isModernRendererInitialized) return

        try {
            // GL.createCapabilities() must have been called on this thread already
            // (handled in render() after swapBuffers makes the GL context current)

            modernRenderer = ModernModelRenderer()
            modernRenderer!!.initialize()

            groundRenderer = ModernGroundRenderer()
            groundRenderer!!.initialize()

            spriteRenderer = ModernSpriteRenderer()
            spriteRenderer!!.initialize()

            particleRendererModern = ModernParticleRenderer()
            particleRendererModern!!.initialize()

            // Initialize frame batchers for draw call consolidation
            frameBatcher = FrameBatcher()
            frameBatcher!!.initialize()

            optimizedBatcher = OptimizedFrameBatcher()
            optimizedBatcher!!.initialize()

            // Initialize instanced model batcher for repeated meshes
            instancedBatcher = InstancedModelBatcher()
            instancedBatcher!!.initialize()

            // Initialize model cache for avoiding per-face CPU work
            modelCache = ModelCache(maxCacheSize = 1024, maxVertexBytes = 128 * 1024 * 1024)

            isModernRendererInitialized = true
            println("[ModernGLToolkit] Modern renderer initialized (with model caching + instancing + sprite + particle renderer)")
        } catch (e: Exception) {
            println("[ModernGLToolkit] Failed to initialize modern renderer: ${e.message}")
            e.printStackTrace()
        }
    }

    // Debug counters (reset each frame in render, logged before swap)
    private var debugFrameCount = 0

    /** Times the frame-start guard had to re-make the GL context current on this thread. */
    private var contextReattachCount = 0
    private var debugModernDraws = 0
    private var debugLegacyDraws = 0
    private var debugSetupSpriteCalls = 0
    private var debugBatchFlushes = 0

    override fun render() {
        // CONTEXT CURRENCY GUARD. The login/loading-screen renderer thread calls
        // setDisplay/resizeDisplay/renderFrame on the active toolkit from ITS thread;
        // the reload() inside makes the GL context current THERE (jaggl setSurface =
        // MakeCurrent). Legacy jaggl bindings self-heal per native call, but LWJGL
        // calls libGL directly with the thread's current context — once currency
        // leaves this thread, every modern GL call here silently no-ops (black
        // screen with all counters ticking). Re-attach at frame start whenever the
        // context is current elsewhere, and re-bind the presenting surface.
        val ogl = this.activeOpenglInstance
        if (ogl != null && ogl.threadPeer !== Thread.currentThread()) {
            attach()
            (this.display ?: this.delegatingDisplay)?.reload()
            contextReattachCount++
        }

        // Flush modern 3D pipeline BEFORE swap
        if (canUseModernPipeline()) {
            renderWithModernPipeline()
        }

        // Flush sprite batch BEFORE swap so sprites appear in the current frame.
        if (canUseModernPipeline()) {
            if (isSpriteBatchActive) debugBatchFlushes++
            endSpriteBatch()
        }

        debugFrameCount++
        // One-line pipeline health diagnostic every ~600 frames. Cheap and easy to
        // delete once the modern renderer is confirmed working on this branch.
        if (debugFrameCount % 600 == 0 || debugFrameCount == 30 || debugFrameCount == 120) {
            println(
                "[ModernDiag] pipeline=${canUseModernPipeline()} init=$isModernRendererInitialized " +
                    "caps=$ownerThreadHasCapabilities gameState=${try { com.jagex.game.runetek5.client.Client.gameState } catch (_: Exception) { "?" }} " +
                    "groundDraws=${groundRenderer?.getFrameStats()?.drawCalls ?: -1} " +
                    "spriteQuads=${spriteRenderer?.getFrameStats()?.quads ?: -1} " +
                    "modelsBatched=$totalModelsProcessed culled=$modelsCulled " +
                    // Gate diagnostics since the previous [ModernDiag] print:
                    // smm      = setModelMatrix calls (rejected = pipeline gate said no at that moment)
                    // batchOpen= how often the deferred model batch was (re)opened
                    // modelGate= isModernRenderingActive() accepted/total (ModelGL.drawMatrixThreaded hook)
                    // ground   = renderGroundMaterialModern drawn/noPipeline/noSnapshot of total calls
                    // reject   = most recent reason canUseModernPipeline() returned false
                    "smm=$diagSetModelMatrixCalls(rej=$diagSetModelMatrixRejected) " +
                    "batchOpen=$diagBatchOpens(lazy=$diagLazyBatchOpens) " +
                    "modelDraws=$diagModelGateAccepted/$diagModelGateCalls " +
                    "ground=$diagGroundDrawn/noPipe=$diagGroundNoPipeline/noSnap=$diagGroundNoSnapshot/calls=$diagGroundCalls " +
                    "reject=$lastPipelineRejectReason " +
                    "legacySkips=${if (legacySkipCounters.isEmpty()) "{}" else legacySkipCounters.toSortedMap().toString()}"
            )
            // Performance breakdown (last frame's timings; cache counters since last print)
            val obStats = optimizedBatcher?.getStats()
            val instStats = instancedBatcher?.getStats()
            println(
                "[ModernPerf] cache=${cacheHits}h/${cacheMisses}m " +
                    "tkMs[hash=%.2f lookup=%.2f addCached=%.2f cull=%.2f beginModel=%.2f] ".format(
                        timeComputeHash / 1e6, timeCacheLookup / 1e6, timeAddCachedModel / 1e6,
                        timeFrustumCull / 1e6, timeBeginModel / 1e6
                    ) +
                    "mbMs[add=%.2f up=%.2f draw=%.2f total=%.2f] ".format(
                        obStats?.timeAddModelsMs ?: 0f, obStats?.timeUploadMs ?: 0f,
                        obStats?.timeRenderMs ?: 0f, obStats?.timeTotalMs ?: 0f
                    ) +
                    "instMs[geoUp=%.2f instUp=%.2f draw=%.2f] ".format(
                        instStats?.geomUploadMs ?: 0f, instStats?.instanceUploadMs ?: 0f,
                        instStats?.drawMs ?: 0f
                    ) +
                    "inst[unique=${instStats?.uniqueMeshes ?: 0} instances=${instStats?.totalInstances ?: 0} " +
                    "drawCalls=${instStats?.drawCalls ?: 0}] mbDrawCalls=${obStats?.drawCalls ?: 0}"
            )
            cacheHits = 0
            cacheMisses = 0

            // Presentation disambiguator: which framebuffer/viewport/scissor is active
            // NOW (immediately before super.render() → delegatingDisplay.draw() →
            // jaggl swapBuffers) vs at the last sprite-batch flush. fbo != 0 at either
            // point means content went to an offscreen target that is never presented.
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

            // AWT/surface identity diagnostics: is the canvas the GL surface belongs to
            // actually the one in the visible hierarchy?
            try {
                val canvas = com.jagex.game.runetek5.client.GameShell.gameCanvas
                val producer = delegatingDisplay as? com.jagex.game.runetek5.imageproducer.ImageProducerGL2
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

            // PRESENTATION PROBE (removable): draw a bright magenta bar directly to the
            // current backbuffer immediately before the swap, bypassing all game draw
            // logic. If the user sees a magenta flash every ~600 frames, presentation
            // (surface + swap) works and the blackness is CONTENT; if not, the GL
            // surface is detached from the visible canvas.
            run {
                val sr2 = spriteRenderer
                val d = display ?: delegatingDisplay
                if (sr2 != null && sr2.isReady() && d != null && !isSpriteBatchActive) {
                    glDisable(GL_SCISSOR_TEST)
                    sr2.begin(d.getWidth(), d.getHeight())
                    sr2.drawFilledRect(0, 0, d.getWidth(), 40, 0xFFFF00FF.toInt(), 1)
                    sr2.end()
                    GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
                    functionMask = 0
                    invalidateScissorCache()

                    // VERDICT PROBE: read the bar back from the backbuffer plus every GL
                    // state that can silently discard rendering while queries stay sane.
                    // pxAtBar=255,0,255 but black screen  => draws land, presentation/swap broken.
                    // pxAtBar=0,0,0                       => draws discarded: check colorMask /
                    //                                        drawBuf (0x405=GL_BACK ok, 0=GL_NONE
                    //                                        bad, 0x404=GL_FRONT wrong) / errs.
                    try {
                        var e0 = glGetError()
                        var errs = ""
                        var errGuard = 0
                        while (e0 != 0 && errGuard++ < 8) {
                            errs += "0x${Integer.toHexString(e0)} "
                            e0 = glGetError()
                        }
                        val maskBuf = org.lwjgl.BufferUtils.createByteBuffer(4)
                        glGetBooleanv(GL_COLOR_WRITEMASK, maskBuf)
                        val drawBuf = glGetInteger(GL_DRAW_BUFFER)
                        val px = org.lwjgl.BufferUtils.createByteBuffer(4)
                        glReadBuffer(GL_BACK)
                        glReadPixels(10, d.getHeight() - 20, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, px)
                        val readErr = glGetError()
                        println(
                            "[ModernProbe] errs=${errs.ifEmpty { "none" }} " +
                                "colorMask=${maskBuf.get(0)},${maskBuf.get(1)},${maskBuf.get(2)},${maskBuf.get(3)} " +
                                "drawBuf=0x${Integer.toHexString(drawBuf)} " +
                                "pxAtBar=${px.get(0).toInt() and 0xff},${px.get(1).toInt() and 0xff},${px.get(2).toInt() and 0xff} " +
                                "readErr=0x${Integer.toHexString(readErr)}"
                        )
                    } catch (e: Exception) {
                        println("[ModernProbe] failed: $e")
                    }
                }
            }
            legacySkipCounters.clear()
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
        }
        debugModernDraws = 0
        debugLegacyDraws = 0
        debugSetupSpriteCalls = 0
        debugBatchFlushes = 0

        // Reset per-presented-frame sprite stats AFTER the diag has read them
        spriteRenderer?.resetFrameStats()

        // Parent render: swapBuffers via jaggl (presents the framebuffer)
        super.render()

        // After swapBuffers, the GL context is current on this thread.
        // Initialize LWJGL3 capabilities and the modern renderer on the owner thread.
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

    /**
     * Called before scene rendering begins.
     * This starts the batcher to collect geometry from drawMatrixThreaded calls.
     */
    override fun setModelMatrix(matrix441: com.jagex.game.runetek5.math.Matrix44) {
        // Call parent to set up legacy matrix state
        super.setModelMatrix(matrix441)

        diagSetModelMatrixCalls++

        // Start the batcher if modern renderer is ready and batcher isn't already active
        if (!canUseModernPipeline()) {
            diagSetModelMatrixRejected++
        }
        if (canUseModernPipeline()) {
            // A setModelMatrix means 3D geometry is coming (scene start, interface model
            // component, post-scene overlay matrix). Any 2D quads queued so far must be
            // flushed NOW so the upcoming models draw ON TOP of them, exactly as legacy's
            // immediate-mode submission order would (see flushSceneForUiDraw kdoc).
            endSpriteBatch()
            var batcherActive = when {
                useOptimizedBatcher -> optimizedBatcher?.isActive() == true
                else -> frameBatcher?.isActive() == true
            }

            // The VP matrix just changed. Legacy draws every model against the LIVE
            // viewProjectionMatrix (ModelGL.drawMatrixThreaded composes projectionMatrix
            // with toolkit.viewProjectionMatrix at draw time, ModelGL.kt:2313), and the
            // modern ground path also reads the live matrix per draw — but the batchers
            // capture VP only once when the batch opens. If setModelMatrix is called
            // again mid-frame with a different matrix (mini-menu picking, interface 3D
            // models), queued geometry must be flushed under its original VP and the
            // batch re-armed with the new one, or models render offset against terrain.
            if (batcherActive) {
                val hasGeometry = when {
                    useOptimizedBatcher ->
                        optimizedBatcher?.hasPendingGeometry() == true ||
                            (useInstancing && instancedBatcher?.hasPendingGeometry() == true)
                    else -> frameBatcher?.hasPendingGeometry() == true
                }
                if (hasGeometry) {
                    // Flush collected geometry with the VP it was queued under,
                    // then fall through to reopen the batch with the new VP.
                    renderWithModernPipeline()
                    functionMask = 0
                    batcherActive = false
                } else {
                    // Batch open but empty: just re-arm the new VP and lighting.
                    if (useOptimizedBatcher) {
                        optimizedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                        optimizedBatcher?.setLighting(
                            sunDirection[0], sunDirection[1], sunDirection[2],
                            sunBrightness,
                            sunColorRed, sunColorGreen, sunColorBlue,
                            sunColorIntensity, antiSunColorIntensity
                        )
                        if (useInstancing) {
                            instancedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                        }
                    }
                }
            }

            if (!batcherActive) {
                openSceneBatch()
            }
        }
    }

    /**
     * Opens the deferred scene batch against the CURRENT viewProjectionMatrix and
     * sun state. Called from setModelMatrix (the normal scene entry) and LAZILY from
     * renderModelGLOptimized when a model arrives while no batch is open — some
     * paths draw models without a preceding setModelMatrix on THIS toolkit; legacy
     * drew them immediately against the live matrices (ModelGL.kt:2311-2313), so
     * opening against the live VP is exact parity and models must never be dropped.
     */
    private fun openSceneBatch() {
        val mr = modernRenderer ?: return
        val gr = groundRenderer ?: return
        diagBatchOpens++

        // CRITICAL: Set up GL state BEFORE any rendering starts.
        // Ground renders immediately during scene traversal, so we must
        // clear depth and enable depth test here, not in renderWithModernPipeline().
        glClear(GL_DEPTH_BUFFER_BIT)
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glDepthMask(true)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_CULL_FACE)

        // Begin frame for all renderers
        mr.beginFrame()
        gr.beginFrame()

        if (useOptimizedBatcher) {
            optimizedBatcher?.resetCumulativeStats()  // Reset stats for new outer frame
            optimizedBatcher?.beginFrame()
            // Set VP matrix for GPU-side transformation
            optimizedBatcher?.setViewProjection(viewProjectionMatrix.elements)
            // Set lighting parameters for GPU-side lighting
            // These match the legacy lighting model from ModelGL.kt
            optimizedBatcher?.setLighting(
                sunDirection[0], sunDirection[1], sunDirection[2],
                sunBrightness,
                sunColorRed, sunColorGreen, sunColorBlue,
                sunColorIntensity, antiSunColorIntensity  // Contrast factors
            )

            // Begin instanced batcher for repeated mesh detection
            if (useInstancing) {
                instancedBatcher?.beginFrame()
                instancedBatcher?.setViewProjection(viewProjectionMatrix.elements)
                instancedBatcher?.setLighting(
                    sunDirection[0], sunDirection[1], sunDirection[2],
                    sunBrightness,
                    sunColorRed, sunColorGreen, sunColorBlue,
                    sunColorIntensity, antiSunColorIntensity
                )
            }
        } else {
            frameBatcher?.beginFrame()
        }

        // Notify model cache of new frame
        modelCache?.onFrameStart()

        // Reset diagnostic counters
        modelCounter = 0

        // Reset detailed timing
        timeFrustumCull = 0L
        timeBeginModel = 0L
        timeComputeHash = 0L
        timeCacheLookup = 0L
        timeAddCachedModel = 0L

        // Clear texture handle cache for this frame
        textureHandleCache.clear()
    }

    private fun renderWithModernPipeline() {
        val mr = modernRenderer ?: return
        val gr = groundRenderer ?: return
        val batcher = frameBatcher ?: return

        // Get display dimensions
        val display = delegatingDisplay ?: return
        val width = display.getWidth()
        val height = display.getHeight()
        if (width <= 0 || height <= 0) return

        // Setup projection (not used directly - we use legacy's combined VP matrix)
        val fov = Math.toRadians(60.0).toFloat()
        val aspect = width.toFloat() / height.toFloat()
        // Far plane sized for extended render distance (legacy viewRangeEnd can
        // reach ~137k units on a 312-tile map); near kept high for depth precision
        modernProjectionMatrix.identity().perspective(fov, aspect, 50f, 140000f)
        modernProjectionMatrix.scale(1f, -1f, 1f)  // Flip Y to match legacy

        // Get camera from scene/client state
        updateCameraFromClient()

        // Setup view matrix
        viewMatrix.identity()
        viewMatrix.rotateX(-camPitch)
        viewMatrix.rotateY(-camYaw)
        viewMatrix.translate(-camX, -camY, -camZ)

        // Configure model renderer (for non-batched rendering if needed)
        mr.setProjection(modernProjectionMatrix)
        mr.setView(viewMatrix)

        // Configure ground renderer
        gr.setProjection(modernProjectionMatrix)
        gr.setView(viewMatrix)

        // Set lighting from toolkit state
        val lightDir = Vector3f(sunDirection[0], sunDirection[1], sunDirection[2])
        if (lightDir.lengthSquared() < 0.001f) {
            lightDir.set(0.5f, 1f, 0.3f)
        }
        lightDir.normalize()
        val lightColor = Vector3f(sunColorRed, sunColorGreen, sunColorBlue)
        mr.setLighting(lightDir, lightColor, 0.4f, 0.6f)
        gr.setLighting(lightDir, lightColor, 0.4f, 0.6f)

        // Set fog using legacy values
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

        // NOTE: The batcher is started in setModelMatrix() which is called during scene rendering.
        // By the time render() is called, models have already been collected by the batcher.
        // We just need to flush the batched geometry here.

        // Check which batcher is active
        val batcherActive = when {
            useOptimizedBatcher -> optimizedBatcher?.isActive() == true
            else -> batcher.isActive()
        }

        // If batcher isn't active, we're on login screen or no 3D scene - skip modern rendering
        if (!batcherActive) {
            return
        }

        // GL state was already set up in setModelMatrix() before scene traversal.
        // Do NOT clear depth here - ground has already rendered during traversal.

        // Match the legacy 3D viewport. The scene projection (Camera.projectionMatrix,
        // built by setupOrthoProjection for gameAreaWidth x gameAreaHeight) targets the
        // DRAWABLE SUB-AREA, and legacy renders 3D with glViewport(drawableX, ...,
        // imageProducerWidth, imageProducerHeight) (OpenGlToolkit.updateViewportAndCenter,
        // OpenGlToolkit.kt:1298-1319, depthClippingMode == 2 — the mode set by
        // setupMatrixRendering before ground/model drawing). The inline ground draws
        // inherit that viewport; flushing the batched models with a FULL-WINDOW viewport
        // instead shifted every model relative to the terrain whenever the game area is
        // not at the window origin (the reported objects-vs-terrain height offset).
        val vw = if (imageProducerWidth < 1) 1 else imageProducerWidth
        val vh = if (imageProducerHeight < 1) 1 else imageProducerHeight
        glViewport(drawableX + viewportOffsetX, viewportOffsetY + height - drawableY - vh, vw, vh)

        // End frame - flush all batched model geometry
        if (useOptimizedBatcher && optimizedBatcher?.isActive() == true) {
            // Render instanced geometry first (repeated meshes)
            if (useInstancing && instancedBatcher?.isActive() == true) {
                instancedBatcher?.endFrame(::lookupTextureHandle)
            }
            // Render remaining non-instanced geometry
            optimizedBatcher?.endFrame(::lookupTextureHandle)
            mr.endFrame()
            gr.endFrame()
        } else if (batcher.isActive()) {
            batcher.endFrame(::lookupTextureHandle)
            mr.endFrame()
            gr.endFrame()
        }

        // Restore default winding order
        glFrontFace(GL_CCW)

        // Disable depth test before UI rendering.
        // The parent's render() calls delegatingDisplay?.draw() for UI,
        // which must render on top of 3D content without depth testing.
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        // Re-apply the viewport for the legacy-cached matrix mode: the raw glViewport
        // above bypassed the depthClippingMode-keyed caching, and legacy only re-sets
        // the viewport on a MODE change (setOrthoMatrixMode/setModelViewMatrixMode are
        // no-ops when the cached mode already matches). Without this, a flush issued
        // while 2D ortho mode is active would leave the sub-area viewport behind for
        // subsequent legacy UI draws.
        updateViewportAndCenter()

        // NOTE: no materialProvider.cycle() here — the base render() already cycles
        // materials once per presented frame, and this method can now run more than
        // once per frame (mid-frame scene flushes before UI drawing).

        frameCounter++
    }

    // ===== Modern Sprite Rendering =====

    /**
     * Ensures the sprite batch is active. Begins a new batch if needed.
     * Called automatically by drawFilledRectangle/drawRectangleOutline.
     */
    private fun ensureSpriteBatch() {
        val sr = spriteRenderer ?: return
        if (isSpriteBatchActive) return
        // Never open the window-space batch while an offscreen DirectDisplay is bound
        // (or on the wrong thread/game state) — callers fall back to legacy drawing.
        if (!canUseModernPipeline()) return

        // The scene batch must reach the framebuffer before UI quads: the sprite
        // batch can be flushed at any time (scissor change, clear, mid-batch texture
        // pressure), so its contents must never be older than the 3D scene beneath.
        flushSceneForUiDraw()

        // Size the batch to the CURRENT render target: the window normally, or the
        // pushed DirectDisplay FBO while the minimap/worldmap compositor is active
        // (legacy sizes its 2D ortho the same way — resetOrthoAndDisplay uses
        // display.getWidth/Height, OpenGlToolkit.kt:2148-2154).
        val target = display ?: delegatingDisplay ?: return
        val width = target.getWidth()
        val height = target.getHeight()
        if (width <= 0 || height <= 0) return

        // Legacy-parity mode transition: 2D drawing runs in ortho mode with the
        // full-target viewport (setup*Rendering → setOrthoMatrixMode →
        // updateViewportAndCenter, OpenGlToolkit.kt:1493/1303-1319). Going through
        // setOrthoMatrixMode keeps the depthClippingMode cache honest so the next
        // legacy 3D setup (setupMatrixRendering) restores the sub-area viewport.
        setOrthoMatrixMode()

        sr.begin(width, height)
        isSpriteBatchActive = true
    }

    /**
     * Ends the current sprite batch if one is active.
     * Must be called before buffer swap or switching to 3D rendering.
     *
     * After the modern sprite batch ends, we must:
     * 1. Restore the active texture unit to TEXTURE0 (the modern batch may have
     *    left it at TEXTURE0+N via resolveTextureSlot)
     * 2. Reset the functionMask to force legacy GL state re-initialization.
     *    The modern batch modifies GL state (shader, textures, blend) without
     *    updating the functionMask, so the legacy path (setupSpriteRendering,
     *    setupShapeRendering, etc.) would incorrectly skip re-initialization.
     */
    fun endSpriteBatch() {
        if (!isSpriteBatchActive) return
        spriteRenderer?.end()
        isSpriteBatchActive = false

        // The flush inside end() re-asserted the full-window viewport for the batch's
        // window-space ortho; restore the viewport that matches the legacy-cached
        // matrix mode (sub-area viewport when depthClippingMode == 2).
        updateViewportAndCenter()

        // Restore active texture unit to TEXTURE0 for legacy SpriteGL.draw() compatibility.
        // The modern batch leaves it at TEXTURE0+N after resolveTextureSlot().
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)

        // Reset functionMask so the next legacy setup call (setupSpriteRendering,
        // setupShapeRendering, etc.) re-initializes all GL state.
        functionMask = 0
    }

    /**
     * Gets the modern sprite renderer for external use (e.g., by ModernFontRenderer).
     */
    fun getSpriteRenderer(): ModernSpriteRenderer? = spriteRenderer

    /**
     * Flushes every deferred modern draw (3D scene batch + 2D sprite batch) to the
     * current framebuffer. Required before any operation that READS the framebuffer
     * (screen-capture sprites, copyTexSubImage updates) — legacy drew immediately,
     * so captures could assume the pixels were present.
     */
    fun flushAllPendingDraws() {
        if (!canUseModernPipeline()) return
        flushSceneForUiDraw()
        endSpriteBatch()
    }

    // ===== Legacy GL Interop =====
    // When legacy SpriteGL/FontRendererGL code is about to draw using glBegin/glEnd,
    // the modern sprite batch MUST be flushed and its shader/VAO unbound first.
    // Otherwise the bound GLSL shader intercepts the immediate-mode vertices and
    // produces no visible output. This happens for sprites created before
    // initializeModernRenderer() (they remain as SpriteGL, not ModernSpriteGL).

    /** True while the deferred 3D scene batch (models collected via drawMatrixThreaded) is open. */
    private fun isSceneBatchActive(): Boolean = when {
        useOptimizedBatcher -> optimizedBatcher?.isActive() == true
        else -> frameBatcher?.isActive() == true
    }

    /**
     * Flushes the deferred 3D scene batch before any 2D/UI drawing.
     *
     * The frame draws the scene first (models are only COLLECTED into the batcher),
     * then chat/interfaces/minimap/console, then render(). Legacy-path UI elements
     * (SpriteGL sprites and FontRendererGL text created while the modern pipeline was
     * unavailable — e.g. everything recreated during a runtime `toolkit 6` switch)
     * draw immediately, so if the batched scene geometry were only flushed in
     * render(), the 3D flush would paint models OVER the already-drawn UI. Flushing
     * here, at the first 2D setup call of the frame, keeps the painter's order:
     * scene first, UI on top.
     *
     * After the first flush the batchers are inactive, so this is a cheap no-op for
     * every subsequent 2D call. If interface 3D models restart the batch mid-UI via
     * setModelMatrix, the next 2D call flushes them immediately, preserving relative
     * order. functionMask is cleared because the flush changes raw GL state (shader
     * program, depth, blend, winding) behind the legacy caches.
     */
    private fun flushSceneForUiDraw() {
        if (!canUseModernPipeline()) return
        if (!isSceneBatchActive()) return
        // Only flush once the batch actually holds geometry. 2D fills also run
        // BEFORE the scene is traversed (skybox background fill, atmosphere clear —
        // both issued right after setModelMatrix opens the batch): flushing there
        // would close the still-empty batch and every model of the frame would be
        // dropped by the inactive-batcher guard in renderModelGLOptimized.
        val hasGeometry = when {
            useOptimizedBatcher ->
                optimizedBatcher?.hasPendingGeometry() == true ||
                    (useInstancing && instancedBatcher?.hasPendingGeometry() == true)
            else -> frameBatcher?.hasPendingGeometry() == true
        }
        if (!hasGeometry) return
        // PAINTER'S ORDER: 2D quads submitted BEFORE these batched models must land
        // BENEATH them. Flushing only the scene here (as before) painted the deferred
        // models UNDER quads that were queued earlier but flushed later — the reported
        // "highly incorrect layering" whenever interface 3D models interleave with
        // sprites/text (legacy draws everything immediately in submission order).
        endSpriteBatch()
        renderWithModernPipeline()
        functionMask = 0
    }

    /**
     * An offscreen DirectDisplay is being pushed (e.g. minimap compositing via
     * getImageProducer). Everything queued in the deferred modern batches targets the
     * CURRENT framebuffer, so it must be flushed before the target changes; the batch
     * reopened inside the FBO then sizes itself from the FBO's dimensions.
     */
    override fun setDisplay(activeDisplay: com.jagex.game.runetek5.imageproducer.DirectDisplay) {
        if (canUseModernPipeline()) {
            flushSceneForUiDraw()
            endSpriteBatch()
        }
        super.setDisplay(activeDisplay)
    }

    /**
     * The offscreen DirectDisplay is being popped. Quads queued while the FBO was
     * bound must be flushed INTO the FBO before it is unbound — otherwise they would
     * later flush onto the window at FBO-local coordinates while the FBO texture
     * stays black (the original misplaced-quads/black-minimap failure mode).
     */
    override fun resizeDisplay(directDisplay: com.jagex.game.runetek5.imageproducer.DirectDisplay) {
        if (canUseModernPipeline()) {
            flushSceneForUiDraw()
            endSpriteBatch()
        }
        super.resizeDisplay(directDisplay)
        functionMask = 0
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
        // Legacy immediate-mode rectangles/lines (used whenever the modern sprite
        // renderer is unavailable) need the same treatment as sprites/text: the
        // deferred scene must be on the framebuffer first, and the modern sprite
        // batch must be flushed so relative draw order is preserved.
        flushSceneForUiDraw()
        if (canUseModernPipeline()) endSpriteBatch()
        super.setupShapeRendering()
    }

    /**
     * Ensures the sprite batch is active for font rendering.
     * Called by ModernFontRenderer.drawChar() to ensure the batch is started
     * before adding glyph quads.
     */
    fun ensureSpriteBatchForFont() {
        ensureSpriteBatch()
    }

    // ===== Modern Font Rendering =====

    /**
     * Creates a modern font renderer when the modern toolkit is active.
     * The ModernFontRenderer renders text as batched textured quads through
     * the ModernSpriteRenderer, eliminating all display list and immediate mode usage.
     */
    override fun createFontRenderer(
        fontMetrics: FontMetrics?,
        spriteTypes: Array<SpriteType>?,
        isPlain: Boolean
    ): AbstractFontRenderer {
        // UNCONDITIONALLY modern under toolkit 6. Construction only builds the glyph
        // atlas through the legacy texture-creation path (resource upload, no FFP
        // drawing) — identical GL requirements to constructing a FontRendererGL, so
        // it is safe even before the modern renderer's initialize(). drawChar()
        // resolves the sprite renderer at draw time and counts a skip if it is
        // somehow still unavailable.
        return ModernFontRenderer(this, fontMetrics!!, spriteTypes!!, isPlain)
    }

    // ===== Modern Sprite Creation =====

    /**
     * Creates a ModernSpriteGL instead of SpriteGL when the modern renderer is initialized.
     * ModernSpriteGL overrides all draw methods to route through ModernSpriteRenderer,
     * intercepting the jaggl.OpenGL calls that SpriteGL would normally make.
     *
     * Falls back to SpriteGL during early startup before the modern renderer is ready.
     */
    // Sprite creation is UNCONDITIONALLY modern under toolkit 6. The ModernSpriteGL
    // constructor is byte-for-byte the SpriteGL constructor (legacy texture upload —
    // a resource operation, not FFP drawing), so it is safe on any thread/state where
    // legacy sprite creation was safe. Modern-vs-skip is decided at DRAW time.
    override fun createNativeSprite(pixels: IntArray, stride: Int, srcWidth: Int, width: Int, height: Int, bool6: Boolean): NativeSprite {
        return ModernSpriteGL(this, width, height, pixels, stride, srcWidth)
    }

    override fun createNativeSprite(x: Int, y: Int, width: Int, height: Int, bool5: Boolean): NativeSprite {
        // SCREEN CAPTURE constructor: SpriteGL(toolkit, x, y, w, h) captures the
        // framebuffer region via OpenGl2DTexture_Sub1.createFromScreenCapture
        // (SpriteGL.kt:48 → OpenGl2DTexture.kt:118 glCopyTexImage2D). Under legacy
        // everything was drawn immediately, so the pixels were present; the modern
        // sprite batch DEFERS draws, so flush everything pending first or the
        // capture reads a stale/black framebuffer.
        if (canUseModernPipeline()) {
            flushSceneForUiDraw()
            endSpriteBatch()
        }
        return ModernSpriteGL(this, x, y, width, height)
    }

    override fun generateEmptySprite(width: Int, height: Int, hasAlpha: Boolean, bool4: Boolean): NativeSprite {
        return ModernSpriteGL(this, width, height, hasAlpha)
    }

    override fun drawFilledRectangle(x: Int, y: Int, width: Int, height: Int, colorArgb: Int, blendMode: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            ensureSpriteBatch()
            sr.drawFilledRect(x, y, width, height, colorArgb, blendMode)
            debugModernDraws++
        } else {
            // No legacy fallback under the modern toolkit — dropped, counted, loud.
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

    // ===== Modern Line Rendering (WP-13: Minimap support) =====

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
            // Draw thick line as a quad with width proportional to lineWidth
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
            // Dashed line: draw segments matching legacy drawLine logic
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

                // Draw line segment as a thin quad
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

    // ===== Modern Clipping and Clearing (WP-13: Minimap support) =====

    override fun expandClipping(left: Int, top: Int, right: Int, bottom: Int) {
        if (canUseModernPipeline() && isSpriteBatchActive) {
            spriteRenderer?.flush()
        }
        super.expandClipping(left, top, right, bottom)
    }

    override fun setClipping(left: Int, top: Int, right: Int, bottom: Int) {
        if (canUseModernPipeline() && isSpriteBatchActive) {
            spriteRenderer?.flush()
        }
        super.setClipping(left, top, right, bottom)
    }

    override fun resetClipping() {
        if (canUseModernPipeline() && isSpriteBatchActive) {
            spriteRenderer?.flush()
        }
        super.resetClipping()
    }

    override fun applyClearFlags(functionMask: Int, colorArgb: Int) {
        if (canUseModernPipeline() && isSpriteBatchActive) {
            spriteRenderer?.flush()
        }
        super.applyClearFlags(functionMask, colorArgb)
    }

    override fun clearRectangle(surface: com.jagex.game.runetek5.config.rsinterface.ComponentSurface, offsetX: Int, offsetY: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            // clearRectangle draws the component surface texture across the clipping region.
            // SpriteSurfaceGL holds an OpenGl2DTexture_Sub1 whose activeHandle is the GL texture ID.
            val spriteSurface = surface as? com.jagex.game.runetek5.toolkit.SpriteSurfaceGL
            if (spriteSurface != null) {
                val surfaceTexture = spriteSurface.surface
                val textureHandle = surfaceTexture.activeHandle
                if (textureHandle > 0) {
                    ensureSpriteBatch()
                    // Compute texture coordinate scale (matching legacy logic)
                    val texScaleU = surfaceTexture.s / surfaceTexture.singleWidth
                    val texScaleV = surfaceTexture.t / surfaceTexture.singleLength

                    val u0 = texScaleU * (this.clipLeft - offsetX)
                    val v0 = texScaleV * (this.clipTop - offsetY)
                    val u1 = texScaleU * (this.clipRight - offsetX)
                    val v1 = texScaleV * (this.clipBottom - offsetY)

                    // Legacy parity (OpenGlToolkit.clearRectangle, OpenGlToolkit.kt:1133-1155):
                    // glColor4ub(0xffffff.inv()) = ARGB 0xFF000000 (opaque BLACK), RGB combine
                    // REPLACE with SOURCE0_RGB = GL_PRIMARY_COLOR (setTexture(0, 34167, 768))
                    // and alpha MODULATE — i.e. RGB comes from the primary color (black) and
                    // alpha is the surface mask. In the modern sprite shader
                    // (finalColor = texture * vertexColor), color 0xFF000000 reproduces this:
                    // rgb = tex.rgb * 0 = black, a = tex.a * 1 = mask.
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
        // No legacy fallback under the modern toolkit
        countLegacySkip("clearRectangle")
    }

    override fun drawSurfaceLine(leftX: Int, topY: Int, rightX: Int, bottomY: Int, colorArgb: Int, dimension: com.jagex.game.runetek5.config.rsinterface.ComponentSurface, clipLeft: Int, clipTop: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            val spriteSurface = dimension as? com.jagex.game.runetek5.toolkit.SpriteSurfaceGL
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

    override fun drawTexturedLine(startX: Int, startY: Int, endX: Int, endY: Int, colorArgb: Int, surface: com.jagex.game.runetek5.config.rsinterface.ComponentSurface, clipLeft: Int, clipTop: Int, lineSpacing: Int, lineLength: Int, offset: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            val spriteSurface = surface as? com.jagex.game.runetek5.toolkit.SpriteSurfaceGL
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

    // ===== Modern Particle Rendering (WP-20) =====

    /**
     * Renders particles using the modern particle renderer when available.
     *
     * The legacy path in OpenGlToolkit.renderParticles() calls
     * GlParticleRenderer.drawParticles() which uses:
     *   - glGetFloatv(GL_MODELVIEW_MATRIX) to extract billboard vectors
     *   - glDisable(GL_LIGHT0) / glDisable(GL_LIGHT1)
     *   - glNormal3f(0, -1, 0)
     *   - OpenGlArrayDrawBuffer for vertex upload
     *
     * The modern path replaces all of this with:
     *   - View matrix from toolkit's modelViewMatrix (no glGetFloatv)
     *   - No lighting calls (shader handles unlit rendering)
     *   - VBO + VAO + IBO with a dedicated particle shader
     *
     * Falls back to the legacy path if the modern renderer is not initialized.
     */
    override fun renderParticles(particleWrapper: ParticleWrapper) {
        val pr = particleRendererModern
        if (pr != null && pr.isReady() && canUseModernPipeline()) {
            // End any active sprite batch before 3D particle rendering
            endSpriteBatch()

            // Source the view (modelview) matrix from the toolkit's modelViewMatrix.
            // The legacy GlParticleRenderer uses glGetFloatv(GL_MODELVIEW_MATRIX) which
            // returns the current modelview matrix set by setupMatrixRendering().
            // The modelViewMatrix field in OpenGlToolkit contains this same data.
            // For billboard vectors we need the raw modelview matrix (columns 0 and 1).
            val viewMatrixFloats = this.currentModelMatrix.elements

            // The projection matrix is used for depth sorting.
            // The legacy code uses projectionMatrix.elements row 2 for Z-depth computation.
            val projectionMatrixFloats = this.viewProjectionMatrix.elements

            // Fog state from the toolkit
            val hasFog = fogEnabled && fogDensity > 0
            val fR = if (hasFog) (fogColor and 0xff0000) / 16711680f else 0f
            val fG = if (hasFog) (fogColor and 0xff00) / 65280f else 0f
            val fB = if (hasFog) (fogColor and 0xff) / 255f else 0f

            pr.renderParticles(
                particleWrapper,
                viewMatrixFloats,
                projectionMatrixFloats,
                ::lookupTextureHandle,
                hasFog,
                fogStartPosition,
                fogEndPosition,
                fR, fG, fB
            )
        } else {
            // No legacy fallback under the modern toolkit
            countLegacySkip("renderParticles")
        }
    }

    private fun updateCameraFromClient() {
        try {
            // Try to get camera position from scene
            val mapRegion = Resource.mapRegion
            val scene = mapRegion?.getScene()

            if (scene != null) {
                // Scene stores camera position in world coordinates
                camX = scene.cameraPositionX.toFloat()
                camY = scene.cameraPositionY.toFloat()
                camZ = scene.cameraPositionZ.toFloat()

                // Get camera rotation from Camera object
                // camAngleX is pitch (vertical rotation), camAngleY is yaw (horizontal rotation)
                // RuneScape uses angles in units of 0.0055 degrees (65536 = 360 degrees)
                // Convert to radians for JOML
                val cameraObj = com.jagex.game.runetek5.client.input.camera.Camera

                // Convert camera angles to radians
                // camAngleX: 0-4096 (pitch), camAngleY: 0-16384 (yaw)
                // Note: RS angles are in 1/4096 or 1/16384 of a full rotation
                camPitch = (cameraObj.camAngleX / 4096f) * Math.PI.toFloat() * 0.5f  // -90 to +90 degrees
                camYaw = (cameraObj.camAngleY / 16384f) * Math.PI.toFloat() * 2f      // 0 to 360 degrees
            }
        } catch (e: Exception) {
            // Use default camera if we can't get scene camera
        }
    }

    /**
     * Looks up the GL texture handle for a material ID.
     * Used by FrameBatcher during endFrame().
     */
    private fun lookupTextureHandle(materialId: Int): Int {
        if (materialId <= 0) return 0

        // Check cache first
        textureHandleCache[materialId]?.let { return it }

        // Look up via texture cache
        val texture = textureCache?.getTexture(materialId)
        val handle = texture?.activeHandle ?: 0

        // Textures stream in asynchronously; getTexture returns null until loaded.
        // Never cache a miss, or the material is stuck untextured (white) forever.
        if (handle != 0) {
            textureHandleCache[materialId] = handle
        }
        return handle
    }

    override fun createModel(rsmesh1: Mesh, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Model {
        // Create a ModelGL that can also be used with modern pipeline
        // For now, use the parent implementation
        // In the future, we could create a ModernModel that works with both pipelines
        return super.createModel(rsmesh1, i_2, i_3, i_4, i_5)
    }

    /**
     * Renders a mesh using the modern pipeline.
     * This can be called directly for testing or from the rendering system.
     */
    fun renderMeshModern(mesh: Mesh, x: Float, y: Float, z: Float, rotationY: Float = 0f) {
        val mr = modernRenderer ?: return

        // Convert mesh to modern format
        val converted = MeshConverter.convert(mesh) ?: return

        try {
            // Set model matrix
            modelMat.identity()
            modelMat.translate(x, y, z)
            modelMat.rotateY(rotationY)
            mr.setModelMatrix(modelMat)

            // Draw
            mr.drawIndexed(converted.vertexData, converted.indexData, converted.indexCount)
        } finally {
            converted.free()
        }
    }

    // Reusable arrays to avoid per-face/per-model allocations
    private val reusableNdcPositions = FloatArray(9)
    private val reusableColors = ByteArray(12)
    private val reusableNormals = FloatArray(9)  // 3 vertices * 3 components for normals
    private var reusableNormalToVertex = IntArray(4096)  // Pooled array for normal-to-vertex mapping

    /**
     * Computes a content-based hash for a ModelGL.
     * This allows caching to work even when ModelGL instances are recreated each frame.
     *
     * IMPORTANT: Animated models have changing vertex positions each frame, so we
     * must NOT include vertex positions in the hash. Instead we use structural
     * properties that remain constant:
     * - Face count and vertex count
     * - Face indices (topology)
     * - Material IDs
     * - Ambient/contrast settings
     */
    private fun computeModelContentHash(model: ModelGL): Int {
        var hash = 17
        hash = hash * 31 + model.numFaces
        hash = hash * 31 + model.maxDepth
        hash = hash * 31 + model.numNormals

        // Include ALL vertex positions via XOR accumulation — CRITICAL because
        // rotateWithNormals(), translate(), mirror(), scale() modify verticesX/Y/Z.
        // Without this, rotated/translated variants collide, rendering wrong geometry.
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

        // Use face indices (topology) - sample first, mid, last
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

        // Include material IDs
        val facesMaterial = model.facesMaterial
        if (facesMaterial != null && facesMaterial.isNotEmpty()) {
            hash = hash * 31 + facesMaterial[0].toInt()
            if (facesMaterial.size > 1) {
                hash = hash * 31 + facesMaterial[facesMaterial.size - 1].toInt()
            }
        }

        // Include face colors
        val facesColor = model.facesColor
        if (facesColor != null && facesColor.isNotEmpty()) {
            hash = hash * 31 + facesColor[0].toInt()
        }

        // Include ambient and contrast for color variation
        hash = hash * 31 + model.ambient.toInt()
        hash = hash * 31 + model.contrast.toInt()

        return hash
    }

    /**
     * Renders a ModelGL using the modern batched pipeline.
     * Called from ModelGL.drawMatrixThreaded when modern renderer is active.
     *
     * This method uses GPU-side vertex transformation when the optimized batcher is enabled,
     * or falls back to CPU-side transformation with the legacy batcher.
     *
     * @param model The ModelGL to render
     * @param modelMatrix The model matrix from the scene (Matrix43)
     */
    fun renderModelGLModern(model: ModelGL, modelMatrix: com.jagex.game.runetek5.math.Matrix43) {
        diagModelGateCalls++
        if (!canUseModernPipeline()) {
            countLegacySkip("ModelGL(pipeline-rejected)")
            return
        }
        diagModelGateAccepted++

        if (useOptimizedBatcher) {
            renderModelGLOptimized(model, modelMatrix)
        } else {
            renderModelGLLegacy(model, modelMatrix)
        }
    }

    /**
     * Optimized rendering path with model caching.
     *
     * The key optimization is caching per-model geometry data so we only process
     * faces ONCE when a model is first seen, then reuse that cached data every frame.
     *
     * Cache hit: ~10x faster (just copy cached data + apply lighting)
     * Cache miss: Build cache once, then fast path forever
     */
    // Debug counters
    private var debugModelAttempts = 0
    private var debugModelSuccesses = 0
    private var debugFaceAttempts = 0
    private var debugFaceSuccesses = 0
    private var debugLastLogTime = 0L
    private var modelsCulled = 0L  // Models culled by frustum check
    private var cacheHits = 0L
    private var cacheMisses = 0L

    // Detailed timing for renderModelGLOptimized
    private var timeFrustumCull = 0L
    private var timeBeginModel = 0L
    private var timeComputeHash = 0L
    private var timeCacheLookup = 0L
    private var timeAddCachedModel = 0L

    private fun renderModelGLOptimized(model: ModelGL, modelMatrix: com.jagex.game.runetek5.math.Matrix43) {
        val batcher = optimizedBatcher ?: return
        if (!batcher.isActive()) {
            // LAZY BATCH OPEN: models must never vanish silently. Some paths draw
            // models without a preceding setModelMatrix on this toolkit (e.g. flows
            // where matrices were armed on another toolkit or before a state change).
            // Legacy drew them immediately against the live viewProjectionMatrix
            // (ModelGL.kt:2311-2313), so opening a batch against the live VP here is
            // exact parity. If the open still fails, count the drop loudly.
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

        // OPTIMIZATION: Frustum culling at model level
        val t1 = System.nanoTime()
        if (model.boundsCalculated) {
            val vp = viewProjectionMatrix.elements
            if (!isModelVisible(model, modelMatrix, vp)) {
                modelsCulled++
                timeFrustumCull += System.nanoTime() - t1
                return
            }
        }
        timeFrustumCull += System.nanoTime() - t1

        val numFaces = model.numFaces
        if (numFaces <= 0) return

        // Try to get cached model data FIRST (before reserving a batcher slot)
        // Use content-based hash for cache key since ModelGL instances may be recreated each frame
        // IMPORTANT: Skip cache for animated models - their vertex positions change each frame
        val isAnimated = model.vertexGroups != null
        val cache = if (isAnimated) null else modelCache

        val t4 = System.nanoTime()
        if (cache != null) {
            // Content hash (full-vertex rolling hash) is only meaningful for static
            // models — computing it for animated models was pure per-frame waste.
            val t3 = System.nanoTime()
            val modelId = computeModelContentHash(model)
            timeComputeHash += System.nanoTime() - t3

            val cachedModel = cache.getOrCreate(model, modelId) {
                buildModelCache(model)
            }
            timeCacheLookup += System.nanoTime() - t4

            if (cachedModel != null) {
                cacheHits++

                // INSTANCING PATH: Route cached models to the instanced batcher.
                // The instanced batcher groups identical meshes (by modelId) and renders
                // them with glDrawElementsInstanced, using one draw call per material
                // regardless of how many instances exist.
                if (useInstancing && instancedBatcher?.isActive() == true) {
                    val t5 = System.nanoTime()
                    val added = instancedBatcher!!.addInstance(cachedModel, modelId, modelMatrix)
                    timeAddCachedModel += System.nanoTime() - t5
                    if (added) {
                        totalFacesProcessed += cachedModel.faceCount
                        modelCounter++
                        debugModelSuccesses++
                        return
                    }
                    // If instanced batcher is full, fall through to regular batcher
                }

                // REGULAR BATCHER PATH: used when instancing is disabled or full
                val t2 = System.nanoTime()
                var modelIndex = batcher.beginModel(modelMatrix)
                timeBeginModel += System.nanoTime() - t2

                if (modelIndex < 0) {
                    flushBatchMidFrame(batcher)
                    modelIndex = batcher.beginModel(modelMatrix)
                    if (modelIndex < 0) return
                }

                modelCounter++
                debugModelSuccesses++

                val t5 = System.nanoTime()
                val facesAdded = batcher.addCachedModel(cachedModel, modelIndex)
                timeAddCachedModel += System.nanoTime() - t5
                totalFacesProcessed += facesAdded
                return
            }
        } else {
            timeCacheLookup += System.nanoTime() - t4
        }

        // Fallback: process faces directly (no cache or cache build failed)
        // Register model matrix with the optimized batcher
        val t2 = System.nanoTime()
        var modelIndex = batcher.beginModel(modelMatrix)
        timeBeginModel += System.nanoTime() - t2

        if (modelIndex < 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(modelMatrix)
            if (modelIndex < 0) return
        }

        modelCounter++
        debugModelSuccesses++
        cacheMisses++
        renderModelGLDirect(model, modelMatrix, batcher, modelIndex)
    }

    /**
     * Builds a cached representation of the model's geometry.
     * This is called once per unique model, then the cache is reused.
     *
     * NEW: Outputs 40-byte vertex format with pre-computed normalized normals.
     * This eliminates per-vertex normal conversion at render time.
     */
    private fun buildModelCache(model: ModelGL): ModelCache.CachedModel? {
        val numFaces = model.numFaces
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

        // Normal data for GPU lighting
        val normalsX = model.normalsX
        val normalsY = model.normalsY
        val normalsZ = model.normalsZ
        val magnitudes = model.magnitudes
        val hasNormals = normalsX != null && normalsY != null && normalsZ != null && magnitudes != null

        // Build normal-to-vertex mapping if needed
        val useDirectVertexIndices = (triangles == null || normalMapping == null)
        val normalToVertex: IntArray?
        if (!useDirectVertexIndices && triangles != null && normalMapping != null) {
            normalToVertex = IntArray(numNormals) { -1 }
            for (vertIdx in 0 until maxDepth) {
                val triStart = triangles[vertIdx]
                val triEnd = triangles[vertIdx + 1]
                for (i in triStart until triEnd) {
                    val normalIdx = (normalMapping[i].toInt() and 0xFFFF) - 1
                    if (normalIdx >= 0 && normalIdx < numNormals) {
                        normalToVertex[normalIdx] = vertIdx
                    }
                }
            }
        } else {
            normalToVertex = null
        }

        // Count valid faces first
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

            if (vertA >= 0 && vertB >= 0 && vertC >= 0 &&
                vertA < maxDepth && vertB < maxDepth && vertC < maxDepth) {
                validFaceCount++
            }
        }

        if (validFaceCount == 0) return null

        // Allocate buffers - NEW 40-byte format
        val vertexCount = validFaceCount * 3
        val vertexData = MemoryUtil.memAlloc(vertexCount * ModelCache.BYTES_PER_VERTEX)  // 40 bytes per vertex
        val materialIds = ShortArray(validFaceCount)

        var outFaceIdx = 0

        // Helper function to compute normalized normal from legacy format
        fun writeNormal(normalIdx: Int) {
            if (hasNormals && normalIdx >= 0 && normalIdx < normalsX!!.size) {
                val nX = normalsX[normalIdx].toFloat()
                val nY = normalsY!![normalIdx].toFloat()
                val nZ = normalsZ!![normalIdx].toFloat()
                val mag = magnitudes!![normalIdx].toInt() and 0xFF

                if (mag == 0) {
                    // mag==0 means pre-normalized with scale factor
                    val len = kotlin.math.sqrt(nX * nX + nY * nY + nZ * nZ)
                    if (len > 0.001f) {
                        vertexData.putFloat(nX / len)
                        vertexData.putFloat(nY / len)
                        vertexData.putFloat(nZ / len)
                    } else {
                        vertexData.putFloat(0f).putFloat(1f).putFloat(0f)
                    }
                } else {
                    // Normal stored as N*magnitude
                    val invMag = 1f / (mag * 256f)
                    val rawNx = nX * invMag
                    val rawNy = nY * invMag
                    val rawNz = nZ * invMag
                    val len = kotlin.math.sqrt(rawNx * rawNx + rawNy * rawNy + rawNz * rawNz)
                    if (len > 0.001f) {
                        vertexData.putFloat(rawNx / len)
                        vertexData.putFloat(rawNy / len)
                        vertexData.putFloat(rawNz / len)
                    } else {
                        vertexData.putFloat(0f).putFloat(1f).putFloat(0f)
                    }
                }
            } else {
                // Default up normal
                vertexData.putFloat(0f).putFloat(1f).putFloat(0f)
            }
        }

        // Process faces - write full 40-byte vertices with pre-computed normals
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

            // Material ID
            val materialId = facesMaterial?.get(faceIdx)?.toInt() ?: -1
            materialIds[outFaceIdx] = materialId.toShort()

            // Base color (pre-computed RGBA)
            val hslColor = facesColor?.get(faceIdx)?.toInt()?.and(0xFFFF) ?: 0
            val faceAlpha = facesAlpha?.get(faceIdx) ?: 0.toByte()
            val baseRgba = model.hslToRgb(hslColor.toShort(), materialId.toShort(), modelAmbient, faceAlpha)
            val finalAlpha = 255 - (faceAlpha.toInt() and 0xFF)
            val packedColor = (baseRgba and 0xFFFFFF00.toInt()) or finalAlpha

            val colorR = ((packedColor ushr 24) and 0xFF).toByte()
            val colorG = ((packedColor ushr 16) and 0xFF).toByte()
            val colorB = ((packedColor ushr 8) and 0xFF).toByte()
            val colorA = (packedColor and 0xFF).toByte()

            // Vertex 0: position(12) + normal(12) + texcoord(8) + color(4) + modelIndex(4) = 40 bytes
            vertexData.putFloat(verticesX[vertA].toFloat())
            vertexData.putFloat(verticesY[vertA].toFloat())
            vertexData.putFloat(verticesZ[vertA].toFloat())
            writeNormal(idxA)  // Pre-computed normalized normal
            vertexData.putFloat(if (textureU != null && idxA < textureU.size) textureU[idxA] else 0f)
            vertexData.putFloat(if (textureV != null && idxA < textureV.size) textureV[idxA] else 0f)
            vertexData.put(colorR).put(colorG).put(colorB).put(colorA)
            vertexData.putFloat(0f)  // modelIndex placeholder

            // Vertex 1
            vertexData.putFloat(verticesX[vertB].toFloat())
            vertexData.putFloat(verticesY[vertB].toFloat())
            vertexData.putFloat(verticesZ[vertB].toFloat())
            writeNormal(idxB)
            vertexData.putFloat(if (textureU != null && idxB < textureU.size) textureU[idxB] else 0f)
            vertexData.putFloat(if (textureV != null && idxB < textureV.size) textureV[idxB] else 0f)
            vertexData.put(colorR).put(colorG).put(colorB).put(colorA)
            vertexData.putFloat(0f)

            // Vertex 2
            vertexData.putFloat(verticesX[vertC].toFloat())
            vertexData.putFloat(verticesY[vertC].toFloat())
            vertexData.putFloat(verticesZ[vertC].toFloat())
            writeNormal(idxC)
            vertexData.putFloat(if (textureU != null && idxC < textureU.size) textureU[idxC] else 0f)
            vertexData.putFloat(if (textureV != null && idxC < textureV.size) textureV[idxC] else 0f)
            vertexData.put(colorR).put(colorG).put(colorB).put(colorA)
            vertexData.putFloat(0f)

            outFaceIdx++
        }

        vertexData.flip()

        // Build pre-sorted material batches to eliminate runtime sorting
        // Group faces by material ID and create index arrays
        val materialToIndices = mutableMapOf<Int, MutableList<Int>>()
        for (faceIdx in 0 until validFaceCount) {
            // SIGNED read: untextured faces store materialId -1. Reading with
            // `and 0xFFFF` turned -1 into 65535, which passed the <= 0 check and
            // created a bogus material batch — lookupTextureHandle(65535) then bound
            // arbitrary/invalid textures for every untextured face of the model.
            val materialId = materialIds[faceIdx].toInt()
            val normalizedId = if (materialId <= 0) 0 else materialId
            val indices = materialToIndices.getOrPut(normalizedId) { mutableListOf() }
            val baseVertex = faceIdx * 3
            indices.add(baseVertex)
            indices.add(baseVertex + 1)
            indices.add(baseVertex + 2)
        }

        // Sort materials (untextured first, then by ID) and create batches
        val sortedMaterials = materialToIndices.keys.sortedBy { if (it == 0) Int.MIN_VALUE else it }
        val materialBatches = sortedMaterials.map { materialId ->
            val indices = materialToIndices[materialId]!!
            ModelCache.MaterialBatch(
                materialId = materialId,
                indices = indices.toIntArray(),
                indexCount = indices.size
            )
        }.toTypedArray()

        return ModelCache.CachedModel(
            faceCount = validFaceCount,
            vertexCount = vertexCount,
            vertexData = vertexData,
            materialBatches = materialBatches
        )
    }

    /**
     * Direct rendering path without caching - fallback for when cache is unavailable.
     * Passes normals to GPU for GPU-side lighting.
     */
    private fun renderModelGLDirect(
        model: ModelGL,
        modelMatrix: com.jagex.game.runetek5.math.Matrix43,
        batcher: OptimizedFrameBatcher,
        modelIndex: Int
    ) {
        val numFaces = model.numFaces
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
        val normalsX = model.normalsX
        val normalsY = model.normalsY
        val normalsZ = model.normalsZ
        val magnitudes = model.magnitudes

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
                    val normalIdx = (normalMapping[i].toInt() and 0xFFFF) - 1
                    if (normalIdx >= 0 && normalIdx < numNormals) {
                        normalToVertex[normalIdx] = vertIdx
                    }
                }
            }
        } else {
            normalToVertex = null
        }

        val hasVertexNormals = normalsX != null && normalsY != null && normalsZ != null && magnitudes != null
        val normals = reusableNormals  // 9 floats for 3 vertex normals

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
            val hslColor = facesColor?.get(faceIdx)?.toInt()?.and(0xFFFF) ?: 0
            val faceAlpha = facesAlpha?.get(faceIdx) ?: 0.toByte()

            val baseRgba = model.hslToRgb(hslColor.toShort(), materialId.toShort(), modelAmbient, faceAlpha)
            val baseR = ((baseRgba ushr 24) and 0xFF).toByte()
            val baseG = ((baseRgba ushr 16) and 0xFF).toByte()
            val baseB = ((baseRgba ushr 8) and 0xFF).toByte()
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

            val u0 = if (textureU != null && idxA < textureU.size) textureU[idxA] else 0f
            val v0 = if (textureV != null && idxA < textureV.size) textureV[idxA] else 0f
            val u1 = if (textureU != null && idxB < textureU.size) textureU[idxB] else 0f
            val v1 = if (textureV != null && idxB < textureV.size) textureV[idxB] else 0f
            val u2 = if (textureU != null && idxC < textureU.size) textureU[idxC] else 0f
            val v2 = if (textureV != null && idxC < textureV.size) textureV[idxC] else 0f

            // Compute normalized vertex normals for GPU lighting.
            // Allocation-free: the previous `for ((v, idx) in listOf(...))` allocated a
            // List + 3 Pairs PER FACE (tens of thousands of allocations per frame in
            // animated-heavy scenes like the swamp).
            if (hasVertexNormals) {
                writeNormalizedNormal(normals, 0, idxA, normalsX!!, normalsY!!, normalsZ!!, magnitudes!!)
                writeNormalizedNormal(normals, 3, idxB, normalsX, normalsY, normalsZ, magnitudes)
                writeNormalizedNormal(normals, 6, idxC, normalsX, normalsY, normalsZ, magnitudes)
            } else {
                for (i in 0 until 9) normals[i] = 0f
                normals[1] = 1f; normals[4] = 1f; normals[7] = 1f
            }

            batcher.addFaceOptimized(
                materialId, modelIndex,
                lx0, ly0, lz0, normals[0], normals[1], normals[2], u0, v0, baseR, baseG, baseB, finalAlpha,
                lx1, ly1, lz1, normals[3], normals[4], normals[5], u1, v1, baseR, baseG, baseB, finalAlpha,
                lx2, ly2, lz2, normals[6], normals[7], normals[8], u2, v2, baseR, baseG, baseB, finalAlpha
            )
        }
    }

    /**
     * Writes the normalized direction for a legacy-format vertex normal into [out]
     * at [off]. Matches the conversion previously inlined in renderModelGLDirect.
     */
    private fun writeNormalizedNormal(
        out: FloatArray, off: Int, idx: Int,
        normalsX: ShortArray, normalsY: ShortArray, normalsZ: ShortArray,
        magnitudes: ByteArray
    ) {
        if (idx < normalsX.size) {
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
        // Default up normal
        out[off] = 0f
        out[off + 1] = 1f
        out[off + 2] = 0f
    }

    /**
     * Legacy rendering path: CPU-side vertex transformation.
     * Kept for comparison/fallback purposes.
     */
    private fun renderModelGLLegacy(model: ModelGL, modelMatrix: com.jagex.game.runetek5.math.Matrix43) {
        val batcher = frameBatcher ?: return
        if (!batcher.isActive()) return

        val numFaces = model.numFaces
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
        val normalsX = model.normalsX
        val normalsY = model.normalsY
        val normalsZ = model.normalsZ
        val magnitudes = model.magnitudes

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
                    val normalIdx = (normalMapping[i].toInt() and 0xFFFF) - 1
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

            // CPU-side MVP transformation
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
        modernRenderer?.destroy()
        modernRenderer = null

        groundRenderer?.destroy()
        groundRenderer = null

        spriteRenderer?.destroy()
        spriteRenderer = null

        particleRendererModern?.destroy()
        particleRendererModern = null

        frameBatcher?.destroy()
        frameBatcher = null

        optimizedBatcher?.destroy()
        optimizedBatcher = null

        instancedBatcher?.destroy()
        instancedBatcher = null

        modelCache?.clear()
        modelCache = null

        isModernRendererInitialized = false
        isSpriteBatchActive = false
        super.shutdown()
    }

    /**
     * Checks if the modern renderer is active, working, and available on the current thread.
     * Returns false if called from a thread other than the owner (e.g., the login screen thread).
     */
    /**
     * NOTE: called by BOTH ModelGL.drawMatrixThreaded and ModernSpriteGL.useModernPath
     * (i.e. per sprite draw too) — deliberately NOT instrumented here; earlier counters
     * on this method made the diag conflate sprite draws with model draws. Model draws
     * are counted in renderModelGLModern; gate rejections are counted at the call sites.
     */
    fun isModernRenderingActive(): Boolean {
        return canUseModernPipeline() && modernRenderer != null
    }

    /**
     * Gets aggregate statistics from ALL modern renderer subsystems.
     * Returns the total draw call count and triangle count across:
     * - OptimizedFrameBatcher (non-instanced models)
     * - InstancedModelBatcher (repeated models via glDrawElementsInstanced)
     * - ModernGroundRenderer (terrain)
     * - ModernSpriteRenderer (UI sprites, fonts, minimap lines)
     * - ModernParticleRenderer (billboard particles)
     *
     * This is the number used to evaluate the <100 draw call target.
     */
    fun getModernRendererStats(): ModernModelRenderer.FrameStats? {
        if (!isModernRendererInitialized) return modernRenderer?.getFrameStats()

        var totalDrawCalls = 0
        var totalTriangles = 0

        // Models: OptimizedFrameBatcher (non-instanced path)
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

        // Models: InstancedModelBatcher (repeated meshes)
        val instanceStats = instancedBatcher?.getStats()
        if (instanceStats != null) {
            totalDrawCalls += instanceStats.drawCalls
            totalTriangles += instanceStats.triangles
        }

        // Ground terrain
        val groundStats = groundRenderer?.getFrameStats()
        if (groundStats != null) {
            totalDrawCalls += groundStats.drawCalls
            totalTriangles += groundStats.triangles
        }

        // Sprites (UI, fonts, minimap lines)
        val spriteStats = spriteRenderer?.getFrameStats()
        if (spriteStats != null) {
            totalDrawCalls += spriteStats.drawCalls
        }

        // Particles
        val particleStats = particleRendererModern?.getFrameStats()
        if (particleStats != null) {
            totalDrawCalls += particleStats.drawCalls
        }

        return ModernModelRenderer.FrameStats(
            drawCalls = totalDrawCalls,
            triangles = totalTriangles
        )
    }

    /**
     * Gets per-subsystem draw call breakdown for the current frame.
     * Used by MCP verify_modern_renderer for detailed analysis.
     */
    fun getPerSubsystemDrawCalls(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()

        // Models: non-instanced
        val batcherStats = if (useOptimizedBatcher) optimizedBatcher?.getStats() else frameBatcher?.getStats()
        result["models_batched"] = if (useOptimizedBatcher) (batcherStats as? OptimizedFrameBatcher.FrameStats)?.drawCalls ?: 0
            else (batcherStats as? FrameBatcher.FrameStats)?.drawCalls ?: 0

        // Models: instanced
        result["models_instanced"] = instancedBatcher?.getStats()?.drawCalls ?: 0

        // Ground terrain
        result["ground"] = groundRenderer?.getFrameStats()?.drawCalls ?: 0

        // Sprites (UI, fonts, minimap)
        result["sprites"] = spriteRenderer?.getFrameStats()?.drawCalls ?: 0

        // Particles
        result["particles"] = particleRendererModern?.getFrameStats()?.drawCalls ?: 0

        // Total
        result["total"] = result.values.sum()

        return result
    }

    /**
     * Gets detailed batcher statistics for debugging.
     */
    fun getOptimizedBatcherStats(): com.jagex.game.runetek5.graphics.modern.batch.OptimizedFrameBatcher.FrameStats? {
        return optimizedBatcher?.getStats()
    }

    /**
     * Gets debug status for MCP.
     */
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
            // Timing profiling (ms)
            "timeAddModelsMs" to (batcherStats?.timeAddModelsMs ?: 0f),
            "timeUploadMs" to (batcherStats?.timeUploadMs ?: 0f),
            "timeRenderMs" to (batcherStats?.timeRenderMs ?: 0f),
            "timeTotalMs" to (batcherStats?.timeTotalMs ?: 0f),
            // Detailed breakdown (inside addCachedModel)
            "timeBulkCopyMs" to (batcherStats?.timeBulkCopyMs ?: 0f),
            "timePatchModelIdxMs" to (batcherStats?.timePatchModelIdxMs ?: 0f),
            "timeIndexCopyMs" to (batcherStats?.timeIndexCopyMs ?: 0f),
            // Detailed breakdown (in renderModelGLOptimized)
            "timeFrustumCullMs" to (timeFrustumCull / 1_000_000f),
            "timeBeginModelMs" to (timeBeginModel / 1_000_000f),
            "timeComputeHashMs" to (timeComputeHash / 1_000_000f),
            "timeCacheLookupMs" to (timeCacheLookup / 1_000_000f),
            "timeAddCachedModelMs" to (timeAddCachedModel / 1_000_000f),
            // Sprite renderer stats
            "spriteRenderer_ready" to (spriteRenderer?.isReady() == true),
            "spriteDrawCalls" to (spriteRenderer?.getFrameStats()?.drawCalls ?: 0),
            "spriteQuads" to (spriteRenderer?.getFrameStats()?.quads ?: 0),
            "spriteTextureSlots" to (spriteRenderer?.getFrameStats()?.textureSlots ?: 0),
            // Particle renderer stats
            "particleRenderer_ready" to (particleRendererModern?.isReady() == true),
            "particleDrawCalls" to (particleRendererModern?.getFrameStats()?.drawCalls ?: 0),
            "particleCount" to (particleRendererModern?.getFrameStats()?.particles ?: 0),
            // Instanced model batcher stats
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

    /**
     * Flushes the current batch mid-frame and starts a new one.
     * Called when MAX_MODELS is reached to avoid dropping geometry.
     */
    private fun flushBatchMidFrame(batcher: OptimizedFrameBatcher) {
        // Render current batch
        batcher.endFrame(::lookupTextureHandle)

        // Start new batch
        batcher.beginFrame()

        // Re-set VP matrix and lighting for new batch
        batcher.setViewProjection(viewProjectionMatrix.elements)
        batcher.setLighting(
            sunDirection[0], sunDirection[1], sunDirection[2],
            sunBrightness,
            sunColorRed, sunColorGreen, sunColorBlue,
            sunColorIntensity, antiSunColorIntensity
        )
    }

    /**
     * Checks if a model's bounding box is potentially visible in the view frustum.
     * Uses the model's AABB transformed by the model matrix against the VP matrix.
     *
     * This is a conservative check - returns true if any part of the AABB might be visible.
     */
    private fun isModelVisible(
        model: com.jagex.game.runetek5.graphics.model.ModelGL,
        modelMatrix: com.jagex.game.runetek5.math.Matrix43,
        vp: FloatArray
    ): Boolean {
        // Get model bounds in local space
        val minX = model.minX.toFloat()
        val maxX = model.maxX.toFloat()
        val minY = model.minY.toFloat()
        val maxY = model.maxY.toFloat()
        val minZ = model.minZ.toFloat()
        val maxZ = model.maxZ.toFloat()

        // Transform AABB center to world space
        val centerX = (minX + maxX) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        val centerZ = (minZ + maxZ) * 0.5f

        val worldCenterX = centerX * modelMatrix.xA + centerY * modelMatrix.xB + centerZ * modelMatrix.xC + modelMatrix.xD
        val worldCenterY = centerX * modelMatrix.yA + centerY * modelMatrix.yB + centerZ * modelMatrix.yC + modelMatrix.yD
        val worldCenterZ = centerX * modelMatrix.zA + centerY * modelMatrix.zB + centerZ * modelMatrix.zC + modelMatrix.zD

        // Calculate AABB half-extents (use max to handle rotation conservatively)
        val extentX = (maxX - minX) * 0.5f
        val extentY = (maxY - minY) * 0.5f
        val extentZ = (maxZ - minZ) * 0.5f

        // Conservative radius in world space (accounts for rotation)
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

        // Use the maximum extent as a conservative bounding sphere radius
        val radius = kotlin.math.max(worldExtentX, kotlin.math.max(worldExtentY, worldExtentZ))

        // Transform center to clip space
        val clipX = vp[12] + vp[0] * worldCenterX + vp[4] * worldCenterY + vp[8] * worldCenterZ
        val clipY = vp[13] + vp[1] * worldCenterX + vp[5] * worldCenterY + vp[9] * worldCenterZ
        val clipZ = vp[14] + vp[2] * worldCenterX + vp[6] * worldCenterY + vp[10] * worldCenterZ
        val clipW = vp[15] + vp[3] * worldCenterX + vp[7] * worldCenterY + vp[11] * worldCenterZ

        // Estimate clip-space radius (conservative)
        // Use the VP matrix's scale factor approximation
        val vpScaleApprox = kotlin.math.sqrt(vp[0]*vp[0] + vp[1]*vp[1] + vp[2]*vp[2])
        val clipRadius = radius * vpScaleApprox

        // Frustum culling with bounding sphere
        // A point is visible if -w <= x <= w, -w <= y <= w, 0 <= z <= w
        // For a sphere, we add the radius to each bound
        if (clipW <= 0.1f) {
            // Behind the camera - check if sphere might cross near plane
            return clipZ + clipRadius > 0.1f
        }

        // Check against all 6 frustum planes (with radius margin)
        if (clipX + clipRadius < -clipW) return false  // Left
        if (clipX - clipRadius > clipW) return false   // Right
        if (clipY + clipRadius < -clipW) return false  // Bottom
        if (clipY - clipRadius > clipW) return false   // Top
        if (clipZ + clipRadius < 0f) return false       // Near
        if (clipZ - clipRadius > clipW) return false   // Far

        return true
    }

    /**
     * Renders ground material using the modern pipeline.
     * Called from GroundMaterialGL.method12085 when using ModernGLToolkit.
     *
     * This method renders textured ground tiles through the modern shader
     * by reusing the legacy VBOs that GroundGL has already populated.
     *
     * @param material The GroundMaterialGL containing color data
     * @param ground The GroundGL containing vertex/normal/texcoord buffers
     * @param tileIndices Array of tile indices to render
     * @param tileCount Number of tiles to render
     * @param functionMask Per-tile function mask for face selection
     * @param materialId The material ID for texture lookup
     * @param textureScale Texture coordinate scale factor
     * @param hasNormals Whether HD lighting normals are present
     */
    private fun groundModernVertexData(ground: GroundGL): java.nio.ByteBuffer? = ground.modernVertexData

    private fun groundModernVertexStride(ground: GroundGL): Int = ground.modernVertexStride

    /**
     * Renders a ground material batch through the modern pipeline.
     *
     * @return true when the modern path handled the draw (including "nothing to draw"),
     *         false when it cannot run — the caller must fall back to the legacy path.
     *         Reachable false-states on this branch: the modern renderer is not yet
     *         initialized (first frames after a runtime `toolkit 6` switch; init happens
     *         after the first render()), a non-owner thread (login screen renderer), a
     *         non-ingame game state, or a GroundGL built without the modern vertex
     *         snapshot (scene built while a different toolkit was active).
     */
    fun renderGroundMaterialModern(
        material: GroundMaterialGL,
        ground: GroundGL,
        tileIndices: IntArray,
        tileCount: Int,
        functionMask: IntArray,
        materialId: Int,
        textureScale: Float,
        hasNormals: Boolean
    ): Boolean {
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

        // Any 2D quads queued before the scene (skybox background fill, fog clears
        // routed through the sprite batch) must reach the framebuffer BEFORE the
        // terrain draws over them; otherwise the deferred fog-colored fill is
        // flushed later in the frame and paints over the already-drawn terrain.
        if (isSpriteBatchActive) endSpriteBatch()

        // Build index array for the selected faces
        // This mirrors the logic in GroundMaterialGL.drawMaterialTriangles
        var indexCount = 0
        val maxIndices = tileCount * 18  // Max 6 triangles (18 indices) per tile
        // Reuse a scratch buffer: this runs once per material per frame, and a fresh
        // ByteArray per call showed up as avoidable per-frame allocation churn.
        if (groundIndexScratch.size < maxIndices * 2) {
            groundIndexScratch = ByteArray(maxIndices * 2)
        }
        val indexArray = groundIndexScratch

        for (i in 0 until tileCount) {
            val tileIdx = tileIndices[i]
            val materialsByTriangle = ground.materialIndices[tileIdx]
            val tileMask = functionMask[tileIdx]

            if (tileMask != 0 && materialsByTriangle != null) {
                var loopIdx = 0
                var vertIdx = 0
                while (vertIdx < materialsByTriangle.size) {
                    if ((tileMask and (1 shl loopIdx++)) != 0) {
                        // Add 3 indices for this triangle
                        val idx0 = materialsByTriangle[vertIdx++].toInt() and 0xFFFF
                        val idx1 = materialsByTriangle[vertIdx++].toInt() and 0xFFFF
                        val idx2 = materialsByTriangle[vertIdx++].toInt() and 0xFFFF

                        // Write as little-endian shorts
                        indexArray[indexCount * 2] = (idx0 and 0xFF).toByte()
                        indexArray[indexCount * 2 + 1] = ((idx0 shr 8) and 0xFF).toByte()
                        indexCount++

                        indexArray[indexCount * 2] = (idx1 and 0xFF).toByte()
                        indexArray[indexCount * 2 + 1] = ((idx1 shr 8) and 0xFF).toByte()
                        indexCount++

                        indexArray[indexCount * 2] = (idx2 and 0xFF).toByte()
                        indexArray[indexCount * 2 + 1] = ((idx2 shr 8) and 0xFF).toByte()
                        indexCount++
                    } else {
                        vertIdx += 3
                    }
                }
            }
        }

        if (indexCount == 0) return true

        // Determine texture coordinate component count from textureBuffer.size
        // size is the number of components (2 for vec2, 3 for vec3)
        val texCoordSize = ground.textureBuffer?.size?.toInt() ?: 2

        // Get texture handle
        val textureHandle = lookupTextureHandle(materialId)

        // Get the combined view-projection matrix
        val vpMatrix = viewProjectionMatrix

        // Legacy parity — color source: GroundMaterialGL.drawMaterialTriangles draws with
        // toggleDrawingClientstates(vertexBuffer, normalsBuffer, arrayBufferPointer,
        // textureBuffer) (GroundMaterialGL.kt:189), so vertex colors come from the
        // PER-MATERIAL color buffer (writeVertexColor baked light/shadow, alpha mask via
        // appendToNativeStream). With shouldUseArbVbo this is a GlVboArrayBuffer — bind
        // it directly by GL id; otherwise fall back to the client-memory copy.
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

        // Legacy parity — texture combine: OpenGlToolkit.renderTexture reads
        // material.combineMode and applies it via loadTextureCombineMode(combineMode)
        // (OpenGlToolkit.kt:1910-1915, 1935-1947). materialId -1 draws untextured with
        // the per-material vertex colors only.
        val combineMode = if (materialId >= 0) {
            materialProvider?.getMaterial(materialId)?.combineMode ?: 0
        } else {
            0
        }

        // Raw legacy sun parameters for HD_LIGHT lighting (GroundGL.kt:458-459 formula)
        gr.setLegacySunParams(
            sunDirection[0], sunDirection[1], sunDirection[2],
            sunBrightness, sunColorIntensity, antiSunColorIntensity
        )

        // Render through modern ground renderer (pure LWJGL path)
        gr.renderWithModernData(
            vertexData = modernData,
            stride = stride,
            texCoordSize = texCoordSize,
            indexData = indexArray,
            indexByteCount = indexCount * 2,
            indexCount = indexCount,
            textureHandle = textureHandle,
            textureScale = 1.0f / textureScale,
            hasNormals = hasNormals,
            viewProjection = vpMatrix,
            isBigEndian = false,
            colorVboId = colorVboId,
            colorData = colorData,
            combineMode = combineMode
        )
        // The VAO pass toggled generic attribute arrays; on most drivers attribute 0
        // aliases the fixed-function GL_VERTEX_ARRAY, so the legacy client-array cache
        // no longer matches real GL state. Drop it so the next legacy draw re-specifies
        // its pointers instead of dereferencing a stale/disabled array (native crash).
        resetClientArrayCache()
        // The modern pass also rebound GL_ARRAY_BUFFER (to our VBOs and to the
        // per-material color VBO, and finally to 0), so the legacy setArrayBuffer
        // identity cache is stale — clear it or the next legacy bind is skipped.
        currentArrayBuffer = null
        currentElementsBuffer = null
        return true
    }
}
