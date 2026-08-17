package com.jagex.game.runetek5.toolkit

import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.utils.ColorUtil
import com.jagex.game.runetek5.graphics.light.PointLight
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.material.MaterialProvider
import com.jagex.game.runetek5.config.material.MaterialTypeList
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
import com.jagex.game.runetek5.graphics.modern.renderer.ModernModelRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernParticleRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernSpriteRenderer
import com.jagex.game.runetek5.graphics.modern.renderer.ModernSurfaceMaskRenderer
import com.jagex.game.runetek5.graphics.model.particle.ParticleWrapper
import com.jagex.game.runetek5.graphics.sprite.ModernSpriteGL
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteGL
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import com.jagex.game.runetek5.graphics.modern.util.MeshConverter
import com.jagex.game.runetek5.graphics.modern.util.ModernModelBuilder
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.graphics.model.Mesh
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelGL
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.texture.OpenGlTexture
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.imageproducer.ImageProducerGL2
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.Matrix44
import com.jagex.game.runetek5.scene.ground.GroundGL
import com.jagex.game.runetek5.toolkit.opengl.GroundMaterialGL
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.KHRDebug
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

    /** Ground renderer, for handing over the native shadow map. */
    fun groundRendererForShadows(): ModernGroundRenderer? = groundRenderer
    private var spriteRenderer: ModernSpriteRenderer? = null
    private var surfaceMaskRenderer: ModernSurfaceMaskRenderer? = null
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

    // ===== Modern mesh-built geometry path =====
    // Models whose ModelGL carries geometry built from the clean decoded mesh
    // (ModernModelBuilder, attached at createModel) render from that geometry;
    // everything else (animated poses, mutated models) falls back to capturing
    // the legacy arrays via buildModelCache. Counters are printed and reset with
    // the [ModernPerf] line to track coverage.
    private var modernBuiltPathModels = 0L
    private var modernBuiltFallbackModels = 0L

    // TEMPORARY self-limiting diagnostics (no console command; dedups + global cap).
    // [GreenDiag] = why a textured face resolves to GL handle 0 (flat-green fallback).
    // [PostDiag]  = per multi-material mesh-built loc, its per-batch material/face split.
    private var diagBudget = 120
    private val diagGreenSeen = HashSet<Int>()
    private val diagModelSeen = HashSet<Long>()
    private val diagDropSeen = HashSet<Int>()

    /** Faces silently discarded by buildModelCache's validity check (see there). */
    private var legacyCaptureDroppedFaces = 0L
    private var legacyCaptureModelsWithDrops = 0L
    private var legacyCaptureModelsFullyDropped = 0L

    /** Models lost by the streaming batcher even after a flush-and-retry. */
    private var streamingModelsDropped = 0L

    /** Scratch for composing baked-yaw rotation into the model matrix (render thread only). */
    private val modernYawScratch = Matrix43()

    /**
     * Returns the model matrix to draw [model]'s modern-built (UNROTATED) base
     * geometry at the same world placement as its legacy (yaw-baked) vertices:
     * pre-multiplies the accumulated yaw so v_unrotated * R_yaw * M matches the
     * legacy v_rotated * M (row-vector convention, R matching rotateWithNormals).
     */
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
         * Runtime A/B switches for isolating which stage loses geometry, flipped by
         * the `render` console command. Each removes one suspect from the pipeline
         * without a rebuild, so a missing-geometry question can be bisected in a
         * single session instead of one relaunch per hypothesis.
         */
        /**
         * Each also reads `-Drender.<name>=false` at startup so tools/capture.sh can
         * A/B a hypothesis across two unattended runs (EXTRA="-Drender.cull=false")
         * without a rebuild or a human at the console.
         */
        private fun flag(name: String) =
            System.getProperty("render.$name")?.equals("false", true) != true

        @JvmStatic var frustumCullEnabled = flag("cull")

        /** Route every model to the streaming batcher, bypassing the instanced store. */
        @JvmStatic var instancingEnabled = flag("instancing")

        /**
         * Mesh-built geometry path. DEFAULT OFF — enable with -Drender.meshbuilt=true.
         *
         * It was introduced to fix multi-model locs, but GeometryDiff comparing both
         * paths over the entire cache (312,796 models) reports zero divergence in
         * face count, vertex position and per-face colour, so legacy-capture already
         * handles multi-model locs correctly and the stated benefit does not exist.
         *
         * Against that, its UVs are wrong: it recomputes the RS texture projection
         * from scratch instead of using the values legacy derives in connectNormal,
         * and the live check finds 18,040 of 27,115 models (67%) with divergent UVs,
         * all on mask 2048 — the wall/scenery mask. Geometrically perfect faces
         * sampling the wrong texels is exactly how Lumbridge/Canifis walls and
         * bushes render invisibly while their neighbours look fine.
         *
         * Correctness first: the path stays available behind the flag for whoever
         * fixes computeUVs against legacy, gated by the same oracle.
         */
        /**
         * Mesh-built geometry (ModernModelBuilder) instead of legacy capture.
         *
         * ON. It was disabled because "67% of models had wrong UVs", but that verdict
         * came from an oracle that index-aligns built face f against legacy face f --
         * legacy SORTS faces via Model.addBillboard -- and compared untextured faces
         * whose legacy UV is legitimately (0,0). The real defect was a byte-order bug
         * that stopped this path drawing anything at all; with it fixed the offline
         * scan reports countMISMATCH=0 posMISMATCH=0 colourMISMATCH=0 over 625k models.
         * -Drender.meshbuilt=false reverts to legacy capture.
         */
        @JvmStatic var meshBuiltPathEnabled =
            System.getProperty("render.meshbuilt")?.equals("false", true) != true

        /** Whether the model shaders' `color.a <= threshold` discard is active. */
        @JvmStatic var alphaDiscardEnabled = flag("alphatest")

        /**
         * Whether the mesh-built path re-applies baked loc yaw via the per-instance
         * matrix. Its geometry is UNROTATED, so turning this off leaves every
         * mesh-built loc at rotation 0 — which splits a mesh-built failure in two:
         * geometry still absent means [ModernModelBuilder] built it wrong, geometry
         * present but mis-oriented means the fault is in the yaw compose.
         */
        @JvmStatic var modernYawEnabled = flag("yaw")
        /** Legacy back-face culling for the 3D scene pass. -Drender.cullface=false to disable. */
        @JvmStatic val cullFaceEnabled = flag("cullface")
        /** Shader point lights on models. -Drender.pointlights=false disables. */
        @JvmStatic val pointLightsEnabled = flag("pointlights")
        @JvmStatic val TEX_ALPHA_DIAG = System.getProperty("render.texalphadiag") != null
        /** Legacy per-vertex-slot colours (last face wins). -Drender.slotcolor=false = per-face. */
        @JvmStatic var slotColorEnabled = flag("slotcolor")

        /** -Drender.meshkey=hash : key mesh-built geometry by current vertices. */
        /** -Drender.verifylive=true : live stale-modernBuilt detector. */
        @JvmStatic val modelBlend = System.getProperty("render.modelblend")?.equals("false", true) != true

        @JvmStatic val noLight = System.getProperty("render.nolight")?.equals("true", true) == true

        @JvmStatic var verifyLiveGeometry =
            System.getProperty("render.verifylive")?.equals("true", true) == true

        @JvmStatic var meshKeyByCurrentVertices =
            System.getProperty("render.meshkey")?.equals("hash", true) == true

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
    /**
     * Bloom is the inherited legacy post-process (RectangularTextureGL on the
     * FramebufferCompositor); it operates on the finished scene framebuffer, so it is
     * renderer-agnostic. It was hard-disabled here while the modern path was being
     * brought up. -Drender.bloomsupport=false restores the disabled behaviour.
     */
    override fun hasBloomSupport(): Boolean =
        if (System.getProperty("render.bloomsupport")?.equals("false", true) == true) false
        else super.hasBloomSupport()

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
            glEnable(KHRDebug.GL_DEBUG_OUTPUT)
            // NOT synchronous: SYNC mode validates+round-trips every GL call through
            // the driver (measured multi-x frame cost). Async still reports message
            // text; only the Java stack loses precision — acceptable now that the
            // renderer works. Re-enable SYNCHRONOUS temporarily when hunting a
            // specific new violation.
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

            surfaceMaskRenderer = ModernSurfaceMaskRenderer()
            surfaceMaskRenderer!!.initialize()

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
            // Entry count must exceed the live unique-mesh count (inst[unique=] runs
            // ~2500-3200), or every miss evicts geometry that is needed again moments
            // later: mc[size=1024 evict=853] with tkMs build=26.82ms on a 67ms frame.
            // maxVertexBytes stays the real memory bound.
            modelCache = ModelCache(maxCacheSize = 8192, maxVertexBytes = 128 * 1024 * 1024)

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

    // Frame-timing accumulators for [ModernPerf] (reset each print window).
    private var perfLastRenderEntryNs = 0L
    private var perfFrameNsAcc = 0L
    private var perfRenderNsAcc = 0L
    private var perfSwapNsAcc = 0L
    private var perfFrameSamples = 0
    private var debugModernDraws = 0
    /** Faces whose vertex slot was never written by the colour pre-pass. */
    private var unwrittenSlotFaces = 0
    private var debugLegacyDraws = 0
    private var debugSetupSpriteCalls = 0
    private var debugBatchFlushes = 0

    /**
     * Whether this frame's scene depth clear has already been issued. Reset once
     * per presented frame (after the swap in [render]); see [openSceneBatch].
     */
    private var sceneDepthCleared = false

    override fun render() {
        // Frame timing: frameMs = wall time between render() entries (whole frame incl.
        // game logic + scene draw), renderMs = this method, swapMs = super.render().
        val renderEntryNs = System.nanoTime()
        if (perfLastRenderEntryNs != 0L) {
            perfFrameNsAcc += renderEntryNs - perfLastRenderEntryNs
            perfFrameSamples++
        }
        perfLastRenderEntryNs = renderEntryNs

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

        // Self-verifying geometry check (-Dgeodiff=<locIds>). Fires once, from the
        // login screen — JS5 assets and the type lists are up long before login, so
        // this needs no account and no game session.
        GeometryDiff.runOnce(this)

        // Drives login → lobby → world so a change can be checked against a real
        // on-screen frame, not just a headless model dump.
        AutoLogin.tick()

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
                    "caps=$ownerThreadHasCapabilities gameState=${try { Client.gameState } catch (_: Exception) { "?" }} " +
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
                    "modelDraws=$diagModelGateAccepted/$diagModelGateCalls unwrittenSlots=$unwrittenSlotFaces " +
                    "ptLights=$diagPointLights/$diagSceneLights recolour[ran=$recolourChecks bad=$recolourBad shadow=$recolourShadowSeen rev0=$recolourRev0 noMap=$recolourNoMap] " +
                    "${ModernModelBuilder.skipSummary()} " +
                    "inval[${ModelGL.invalidateSummary()}] " +
                    "ground=$diagGroundDrawn/noPipe=$diagGroundNoPipeline/noSnap=$diagGroundNoSnapshot/calls=$diagGroundCalls " +
                    "reject=$lastPipelineRejectReason " +
                    "legacySkips=${if (legacySkipCounters.isEmpty()) "{}" else legacySkipCounters.toSortedMap().toString()}"
            )
            // Performance breakdown (last frame's timings; cache counters since last print)
            val obStats = optimizedBatcher?.getStats()
            val instStats = instancedBatcher?.getStats()
            println(
                "[ModernPerf] cullMs=%.2f tilesMs=%.2f ".format(
                    if (Scene.phaseSamples > 0) Scene.phaseCullNs / 1e6 / Scene.phaseSamples else 0.0,
                    if (Scene.phaseSamples > 0) Scene.phaseTilesNs / 1e6 / Scene.phaseSamples else 0.0
                ) +
                "sceneMs=%.2f ".format(
                    if (Client.sceneDrawSamples > 0) Client.sceneDrawNs / 1e6 / Client.sceneDrawSamples else 0.0
                ) +
                "frameMs=%.1f renderMs=%.2f swapMs=%.2f ".format(
                    if (perfFrameSamples > 0) perfFrameNsAcc / 1e6 / perfFrameSamples else 0.0,
                    if (perfFrameSamples > 0) perfRenderNsAcc / 1e6 / perfFrameSamples else 0.0,
                    if (perfFrameSamples > 0) perfSwapNsAcc / 1e6 / perfFrameSamples else 0.0
                ) +
                    "cache=${cacheHits}h/${cacheMisses}m " +
                    // Mesh-built geometry coverage: models drawn from the clean-mesh
                    // build vs models on the legacy-capture fallback (animated poses,
                    // mutated models, or no build available).
                    "meshBuilt=$modernBuiltPathModels/fallback=$modernBuiltFallbackModels " +
                    // Faces the legacy-capture path threw away. Nonzero here means
                    // scenery is losing geometry before it ever reaches the GPU.
                    "dropped[faces=$legacyCaptureDroppedFaces models=$legacyCaptureModelsWithDrops " +
                    "whole=$legacyCaptureModelsFullyDropped stream=$streamingModelsDropped] " +
                    "tkMs[hash=%.2f lookup=%.2f build=%.2f addCached=%.2f cull=%.2f beginModel=%.2f] ".format(
                        timeComputeHash / 1e6, timeCacheLookup / 1e6,
                        modelCache?.getBuildMillis() ?: 0f, timeAddCachedModel / 1e6,
                        timeFrustumCull / 1e6, timeBeginModel / 1e6
                    ) +
                    // build is the miss cost (inside lookup); mcEvict nonzero means the
                    // model cache is undersized and is rebuilding what it just freed.
                    "mc[size=${modelCache?.getSize() ?: 0} evict=${modelCache?.getEvictions() ?: 0}] " +
                    "mbMs[add=%.2f up=%.2f draw=%.2f total=%.2f] ".format(
                        obStats?.timeAddModelsMs ?: 0f, obStats?.timeUploadMs ?: 0f,
                        obStats?.timeRenderMs ?: 0f, obStats?.timeTotalMs ?: 0f
                    ) +
                    "instMs[geoUp=%.2f instUp=%.2f draw=%.2f] ".format(
                        instStats?.geomUploadMs ?: 0f, instStats?.instanceUploadMs ?: 0f,
                        instStats?.drawMs ?: 0f
                    ) +
                    "inst[unique=${instStats?.uniqueMeshes ?: 0} instances=${instStats?.totalInstances ?: 0} " +
                    "drawCalls=${instStats?.drawCalls ?: 0}] " +
                    // Geometry-store residency: evicted/overflow should sit at 0 in
                    // steady state; sustained nonzero means the store is undersized
                    // for the working set (raise MAX_TOTAL_VERTICES).
                    "store[verts=${instStats?.storeVertices ?: 0} evicted=${instStats?.meshesEvicted ?: 0} " +
                    "overflow=${instStats?.capacityFallbacks ?: 0}] " +
                    "mbDrawCalls=${obStats?.drawCalls ?: 0} " +
                    "fog[on=$fogEnabled dens=$fogDensity start=%.0f end=%.0f col=%06X] ".format(fogStartPosition, fogEndPosition, fogColor and 0xffffff) +
                    "light[sunB=%.3f int=%.3f anti=%.3f col=%.2f,%.2f,%.2f] ".format(sunBrightness, sunColorIntensity, antiSunColorIntensity, sunColorRed, sunColorGreen, sunColorBlue) +
                    GeometryDiff.liveSummary() + " " + GeometryDiff.contrastSummary() + " " + GeometryDiff.groundSummary()
            )
            cacheHits = 0
            cacheMisses = 0
            modernBuiltPathModels = 0L
            modernBuiltFallbackModels = 0L
            legacyCaptureDroppedFaces = 0L
            legacyCaptureModelsWithDrops = 0L
            legacyCaptureModelsFullyDropped = 0L
            streamingModelsDropped = 0L
            perfFrameNsAcc = 0L
            perfRenderNsAcc = 0L
            perfSwapNsAcc = 0L
            perfFrameSamples = 0

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
                val canvas = GameShell.gameCanvas
                val producer = delegatingDisplay as? ImageProducerGL2
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

            // (Presentation/verdict probe removed — it served its purpose finding the
            // rectangle-texture root cause. Console diagnostics above are draw-free.)
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

        // Reset per-presented-frame stats AFTER the diag has read them
        timeFrustumCull = 0L
        timeBeginModel = 0L
        timeComputeHash = 0L
        timeCacheLookup = 0L
        timeAddCachedModel = 0L
        modelCache?.resetBuildTimer()
        spriteRenderer?.resetFrameStats()
        optimizedBatcher?.resetCumulativeStats()
        instancedBatcher?.resetFrameStats()

        // Parent render: swapBuffers via jaggl (presents the framebuffer)
        val swapStartNs = System.nanoTime()
        super.render()
        val swapEndNs = System.nanoTime()
        perfSwapNsAcc += swapEndNs - swapStartNs
        perfRenderNsAcc += swapEndNs - perfLastRenderEntryNs

        // The framebuffer has been presented: the next scene traversal is a new
        // frame and owes exactly one depth clear (see openSceneBatch).
        sceneDepthCleared = false

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
    override fun setModelMatrix(matrix441: Matrix44) {
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
        // Ground renders immediately during scene traversal, so we must set depth
        // state here, not in renderWithModernPipeline().
        //
        // The depth CLEAR happens once per presented frame, NOT per batch open.
        // openSceneBatch runs on every mid-frame reopen — setModelMatrix flushes
        // and reopens whenever the VP changes or geometry is pending, which is
        // hundreds of times per scene. Clearing depth there wiped the depth of
        // everything already drawn, so each batch painted over its predecessors
        // regardless of actual distance: ground decoration through roofs, walls
        // buried under later geometry, occluded quads showing through, submeshes
        // of one loc disagreeing about what is in front.
        if (!sceneDepthCleared) {
            glClear(GL_DEPTH_BUFFER_BIT)
            sceneDepthCleared = true
        }
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)
        glDepthMask(true)
        // Match legacy's model GL state EXACTLY. setupMatrixRendering() — the path
        // that draws models — calls setBlendMode(1) = BlendType.ALPHA, which is
        // glEnable(GL_BLEND) + SRC_ALPHA/ONE_MINUS_SRC_ALPHA together with
        // glEnable(GL_ALPHA_TEST) + glAlphaFunc(GL_GREATER, 0).
        //
        // So legacy blends EVERY model and discards only fully transparent
        // fragments. An earlier reading of setBlendMode(0) from a 2D/UI path led me
        // to disable blending for "opaque" materials, which makes genuinely
        // translucent geometry render solid — the transparency mismatch.
        // -Drender.modelblend=false draws models unblended. Legacy renders Canifis
        // lampposts SOLID even though their faces carry ~50% alpha (confirmed by
        // visualising per-fragment alpha), so honouring that alpha is what makes
        // them ghostly here.
        if (modelBlend) {
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        } else {
            glDisable(GL_BLEND)
        }
        // Cull state for model geometry is asserted at the draw site in the
        // model batchers (state set here does not survive to the draws).
        glDisable(GL_CULL_FACE)

        // Begin frame for all renderers
        mr.beginFrame()
        gr.beginFrame()

        if (useOptimizedBatcher) {
            // NOTE: cumulative stats are NOT reset here — openSceneBatch runs at every
            // mid-frame batch reopen, which zeroed the counters the diag reads (the
            // "mbDrawCalls=0 while models visibly render" artifact). They reset once
            // per presented frame in render(), after the diag has read them.
            optimizedBatcher?.beginFrame()
            // Set VP matrix for GPU-side transformation
            optimizedBatcher?.setViewProjection(viewProjectionMatrix.elements)
            // Set lighting parameters for GPU-side lighting
            // These match the legacy lighting model from ModelGL.kt
            // -Drender.nolight=true collapses the sun term to identity
            // (litRGB == vertex colour), isolating how much of the modern/legacy
            // brightness gap comes from the shader's lighting rather than from the
            // baked vertex colours.
            if (noLight) {
                optimizedBatcher?.setLighting(
                    sunDirection[0], sunDirection[1], sunDirection[2],
                    1f, 1f, 1f, 1f, 0f, 0f
                )
            } else optimizedBatcher?.setLighting(
                sunDirection[0], sunDirection[1], sunDirection[2],
                sunBrightness,
                sunColorRed, sunColorGreen, sunColorBlue,
                sunColorIntensity, antiSunColorIntensity  // Contrast factors
            )
            // Legacy enables GL_LINEAR fog for every model draw via
            // setupMatrixRendering(); without it modern geometry never blends
            // toward the fog colour and the whole scene reads ~25% brighter.
            optimizedBatcher?.setFog(
                viewMatrix44.elements, fogEnabled && fogDensity >= 0,
                fogColor, fogStartPosition, fogEndPosition
            )
            buildFrameLights()
            optimizedBatcher?.setPointLights(frameLightCount, frameLightPos, frameLightColor)
            gr.setPointLights(frameLightCount, frameLightPos, frameLightColor)

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
                instancedBatcher?.setFog(
                    viewMatrix44.elements, fogEnabled && fogDensity >= 0,
                    fogColor, fogStartPosition, fogEndPosition
                )
                instancedBatcher?.setPointLights(frameLightCount, frameLightPos, frameLightColor)
            }

        } else {
            frameBatcher?.beginFrame()
        }

        // Notify model cache of new frame
        modelCache?.onFrameStart()

        // Reset diagnostic counters
        modelCounter = 0

        // NOTE: the tkMs traversal timers are NOT reset here. openSceneBatch runs at
        // every mid-frame batch reopen (119-301 per diag interval), so zeroing them
        // here left the diag reading only the final batch's slice — every tkMs field
        // printed 0.00 while the frame was spending ~69ms somewhere in traversal.
        // They reset once per presented frame in render(), after the diag reads them
        // (same fix the cumulative batcher stats already carry).

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
                instancedBatcher?.setTextureAlphaPredicate(::materialTextureHasAlpha)
                instancedBatcher?.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
            }
            // Render remaining non-instanced geometry
            optimizedBatcher?.setTextureAlphaPredicate(::materialTextureHasAlpha)
            optimizedBatcher?.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
            mr.endFrame()
            gr.endFrame()
        } else if (batcher.isActive()) {
            // Non-optimized FrameBatcher fallback (only when useOptimizedBatcher is
            // false) — combine-mode plumbing lives on the optimized/instanced path.
            batcher.endFrame(::lookupTextureHandle)
            mr.endFrame()
            gr.endFrame()
        }

        // Restore default winding order and two-sided drawing for the 2D/UI pass.
        glFrontFace(GL_CCW)
        glDisable(GL_CULL_FACE)

        // Restore blending for the 2D/UI pass. The model batchers leave GL_BLEND
        // wherever their last material batch put it, and sprites/text/interfaces
        // all assume standard alpha blending is on.
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

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
     * Legacy texture BINDS (creation/upload paths: OpenGl2DTexture constructors,
     * generateSprite, font-atlas builds — all call setActiveTexture to bind for
     * glTexImage) can run MID-FRAME while the sprite batch holds queued quads: the
     * bind lands on whatever texture unit resolveTextureSlot left active and
     * CLOBBERS that slot's binding, so already-queued quads flush sampling the
     * wrong texture (the minimenu's first-open "ghost texture" / first-frame
     * interface corruption). Flush the queued quads with their still-correct
     * bindings first — the flush resets the slot table, so later quads re-resolve
     * and re-bind cleanly — and park the active unit on TEXTURE0 so the legacy
     * bind only touches unit 0.
     */
    override fun setActiveTexture(replacingCombiner: OpenGlTexture?) {
        if (isSpriteBatchActive) {
            spriteRenderer?.flush()
            GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        }
        super.setActiveTexture(replacingCombiner)
    }

    /**
     * Flushes every deferred modern draw (3D scene batch + 2D sprite batch) to the
     * current framebuffer. Required before any operation that READS the framebuffer
     * (screen-capture sprites, copyTexSubImage updates) — legacy drew immediately,
     * so captures could assume the pixels were present.
     */
    // ===== Point lights =====
    // Legacy calls copyLights per entity (Scene.placeSceneryObjects) and programs
    // GL_LIGHT2+ for that model only. The modern batchers draw models grouped by
    // material, so per-model light state cannot be expressed; instead the frame's
    // lights are accumulated here and uploaded once as a small array. Because
    // attenuation is quadratic, a light far from a model contributes ~0, which is
    // what legacy's per-model selection approximates anyway.
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
    /** Global index of each uploaded light, for per-entity index lookup. */
    private val lightIndexOf = HashMap<PointLight, Int>()
    /** Light indices for the entity currently being submitted (-1 = unused). */
    private val currentLightIdx = FloatArray(4) { -1f }

    override fun copyLights(lightCount: Int, lights: Array<PointLight>?) {
        // Legacy GL_LIGHT state only (the ground's fixed-function pass reads it).
        // Per-model light selection is positional — see selectLightsFor. Deriving it
        // from this callback instead made a model inherit the PREVIOUS entity's lights
        // whenever it was submitted without a copyLights of its own (players/NPCs take
        // a different path), which flickered as traversal order changed and left many
        // models unlit entirely.
        super.copyLights(lightCount, lights)
    }

    /**
     * Chooses the lights that reach a model, by proximity to its world origin.
     *
     * Legacy associates up to getMaxLights() = 4 lights per entity; this reproduces the
     * selection from the model's own position so it depends only on geometry, never on
     * traversal order. Keeps the per-vertex loop at 4 instead of every scene light.
     */
    private fun selectLightsFor(wx: Float, wy: Float, wz: Float) {
        currentLightIdx[0] = -1f; currentLightIdx[1] = -1f
        currentLightIdx[2] = -1f; currentLightIdx[3] = -1f
        val n = frameLightCount
        if (n <= 0) return
        // Keep the 4 strongest contributors: attenuation is size^2/d^2, so rank on
        // d^2/size^2 (smaller is stronger) and ignore lights that cannot register on an
        // 8-bit channel at this distance.
        var b0 = Float.MAX_VALUE; var b1 = Float.MAX_VALUE
        var b2 = Float.MAX_VALUE; var b3 = Float.MAX_VALUE
        var i0 = -1; var i1 = -1; var i2 = -1; var i3 = -1
        for (i in 0 until n) {
            val dx = frameLightPos[i * 4] - wx
            val dy = frameLightPos[i * 4 + 1] - wy
            val dz = frameLightPos[i * 4 + 2] - wz
            val d2 = dx * dx + dy * dy + dz * dz
            // frameLightPos[w] holds kq = 1/size^2, so kq*d^2 is the inverse of att.
            val rank = frameLightPos[i * 4 + 3] * d2
            if (rank >= 255f) continue
            when {
                rank < b0 -> { b3=b2; i3=i2; b2=b1; i2=i1; b1=b0; i1=i0; b0=rank; i0=i }
                rank < b1 -> { b3=b2; i3=i2; b2=b1; i2=i1; b1=rank; i1=i }
                rank < b2 -> { b3=b2; i3=i2; b2=rank; i2=i }
                rank < b3 -> { b3=rank; i3=i }
            }
        }
        currentLightIdx[0] = i0.toFloat(); currentLightIdx[1] = i1.toFloat()
        currentLightIdx[2] = i2.toFloat(); currentLightIdx[3] = i3.toFloat()
    }

    /**
     * Collects the scene's point lights and packs them for upload.
     *
     * Sourced from Scene.lightSlotOccupant, which is the COMPLETE set of lights the
     * scene holds, rather than from copyLights. copyLights only fires for entities the
     * scene walk actually reaches, so a lamp went dark whenever the geometry it lit
     * fell outside the walk -- the light switched on and off as the player moved a tile
     * or turned the camera. The scene list does not depend on traversal, so a light
     * stays lit for as long as it exists.
     */
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

        // Order by distance to the camera before capping, so the cap only ever drops
        // the lights whose quadratic falloff contributes least — and does so
        // deterministically, instead of varying with collection order.
        val maxLights = InstancedModelBatcher.MAX_POINT_LIGHTS
        if (frameLights.size > maxLights) {
            val camX = Camera.cameraPositionAbsoluteX.toFloat()
            val camY = Camera.cameraPositionAbsoluteZ.toFloat()
            val camZ = Camera.cameraPositionAbsoluteY.toFloat()
            frameLights.sortBy { l ->
                val dx = l.getX() - camX
                val dy = l.getY() - camY
                val dz = l.getZ() - camZ
                dx * dx + dy * dy + dz * dz
            }
        }
        val n = if (frameLights.size > maxLights) maxLights else frameLights.size
        for (i in 0 until n) {
            val l = frameLights[i]
            val size = l.getSize2d().toFloat()
            // GL_QUADRATIC_ATTENUATION = 1/size2d^2 (OpenGlToolkit.renderLights).
            val kq = if (size > 0f) 1f / (size * size) else 1f
            frameLightPos[i * 4] = l.getX().toFloat()
            frameLightPos[i * 4 + 1] = l.getY().toFloat()
            frameLightPos[i * 4 + 2] = l.getZ().toFloat()
            frameLightPos[i * 4 + 3] = kq
            // GL_DIFFUSE = colour components * (transparency / 255), matching
            // renderLights; transparency carries the flicker factor (~1.0).
            val c = l.getColor()
            val a = l.getAlpha() / 255.0f
            frameLightColor[i * 3] = ((c shr 16) and 0xff) * a
            frameLightColor[i * 3 + 1] = ((c shr 8) and 0xff) * a
            frameLightColor[i * 3 + 2] = (c and 0xff) * a
        }
        lightIndexOf.clear()
        for (i in 0 until n) lightIndexOf[frameLights[i]] = i
        frameLightCount = n
        diagPointLights = n
    }


    fun flushAllPendingDraws() {
        if (!canUseModernPipeline()) return
        flushSceneForUiDraw()
        endSpriteBatch()
    }

    /**
     * Draws one surface-masked quad (legacy multi-texture overlay — SpriteGL
     * drawWithSurface/drawAffineMasked) through the dedicated modern composite
     * renderer, preserving submission order: pending batches are flushed first,
     * the quad draws immediately, and legacy caches are invalidated afterwards.
     * Returns false when the modern path cannot draw (caller drops the quad).
     */
    fun drawSurfaceMaskedQuad(
        baseHandle: Int, maskHandle: Int,
        x0: Float, y0: Float, b0u: Float, b0v: Float, m0u: Float, m0v: Float,
        x1: Float, y1: Float, b1u: Float, b1v: Float, m1u: Float, m1v: Float,
        x2: Float, y2: Float, b2u: Float, b2v: Float, m2u: Float, m2v: Float,
        x3: Float, y3: Float, b3u: Float, b3v: Float, m3u: Float, m3v: Float
    ): Boolean {
        val smr = surfaceMaskRenderer ?: return false
        if (!smr.isReady() || !canUseModernPipeline()) return false
        val target = display ?: delegatingDisplay ?: return false

        // Submission order: everything queued so far must be beneath this quad.
        flushSceneForUiDraw()
        endSpriteBatch()

        smr.drawMaskedQuad(
            baseHandle, maskHandle,
            target.getWidth(), target.getHeight(),
            x0, y0, b0u, b0v, m0u, m0v,
            x1, y1, b1u, b1v, m1u, m1v,
            x2, y2, b2u, b2v, m2u, m2v,
            x3, y3, b3u, b3v, m3u, m3v
        )

        // Raw GL state changed behind the legacy caches (shader, blend, textures on
        // units 0/1, viewport).
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        functionMask = 0
        updateViewportAndCenter()
        return true
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
    override fun setDisplay(activeDisplay: DirectDisplay) {
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
    override fun resizeDisplay(directDisplay: DirectDisplay) {
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

    override fun clearRectangle(surface: ComponentSurface, offsetX: Int, offsetY: Int) {
        val sr = spriteRenderer
        if (sr != null && sr.isReady() && canUseModernPipeline()) {
            // clearRectangle draws the component surface texture across the clipping region.
            // SpriteSurfaceGL holds an OpenGl2DTexture_Sub1 whose activeHandle is the GL texture ID.
            val spriteSurface = surface as? SpriteSurfaceGL
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
            // ORDERING: particles are emitted mid-entity-traversal, immediately after
            // the emitting entity's models (PlayerEntity.kt:659-670 draws models then
            // renderParticles). Under the deferred batcher those models are only
            // QUEUED, so flush the scene (and any queued 2D) first — otherwise the
            // later model flush would paint over the particles. Legacy drew both
            // immediately in this same order.
            flushSceneForUiDraw()
            endSpriteBatch()

            // BILLBOARD BASIS — legacy parity: GlParticleRenderer builds the billboard
            // right/up vectors and per-particle depths from toolkit.viewMatrix44 (the
            // CAMERA view matrix; GlParticleRenderer.kt:48-51 and :163-168 — the
            // tracked-matrix replacement for the old glGetFloatv(GL_MODELVIEW)).
            // NOTE: currentModelMatrix is the PROJECTION matrix on this toolkit
            // (setModelMatrix stores Camera.projectionMatrix, OpenGlToolkit.kt:2600) —
            // passing it here previously produced degenerate billboard vectors and
            // no visible particles.
            val viewMatrixFloats = this.viewMatrix44.elements

            // Projection for the clip transform (renderer composes proj × view; the
            // Y-flip for GL clip space is applied inside the renderer).
            val projectionMatrixFloats = this.currentModelMatrix.elements

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

            // The particle pass changed raw GL state (shader, blend, depth mask)
            // behind the legacy caches.
            functionMask = 0
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
                val cameraObj = Camera

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
    /**
     * Legacy per-material texture combine mode (MaterialType.combineMode), used by the
     * model batchers' shaders exactly like the ground path (renderGroundMaterial):
     * REPLACE(1) ignores the per-face vertex color, MODULATE(0) multiplies it, etc.
     * Without this the model shader hardwired MODULATE and tinted textured faces by
     * their (often junk-green) face colors — the green sign-board / textured-loc bug.
     */
    /** -Drender.combine=modulate forces MODULATE, matching legacy's default. */
    private val forceModulate = System.getProperty("render.combine")?.equals("modulate", true) == true

    private fun lookupCombineMode(materialId: Int): Int {
        if (forceModulate) return 0
        // Material 0 is valid/textured — only -1 is untextured (matches lookupTextureHandle).
        if (materialId < 0) return 0
        return materialProvider?.getMaterial(materialId)?.combineMode?.toInt() ?: 0
    }

    /**
     * Whether a material must be drawn with alpha blending.
     *
     * Ports legacy's opacity test verbatim: a material is opaque when
     * `effectCombiner != 2 && isOpaqueEffect(effectId)`, where effect ids 1 and 7
     * are the non-opaque effects (OpenGLTextureCache line 46,
     * MaterialTypeList.isOpaqueEffect). effectCombiner == 2 is the same
     * alpha-transition marker ModelGL uses to set hasAlphaTransition.
     *
     * Everything else is OPAQUE and must draw with GL_BLEND off, matching
     * legacy's setBlendMode(0) default. The modern scene pass used to enable
     * blending once in openSceneBatch and leave it on for every model, which made
     * any face with a nonzero RS alpha translucent (the Canifis lamps) and — with
     * depth writes still on — let those near-invisible faces depth-reject the
     * geometry behind them (the missing Canifis walls).
     */
    private fun materialNeedsBlend(materialId: Int): Boolean {
        if (materialId < 0) return false
        val material = materialProvider?.getMaterial(materialId) ?: return false
        return material.effectCombiner == 2 || !MaterialTypeList.isOpaqueEffect(material.effectId.toInt())
    }

    /**
     * Reads a material's texture alpha channel off the GPU once and reports its
     * distribution. Answers directly whether a surface that renders translucent has
     * genuinely partial alpha in its texture data, or whether the translucency comes
     * from somewhere else in the pipeline. -Drender.texalphadiag=1.
     */
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
                val mt = materialProvider?.getMaterial(materialId)
                System.err.println("[TexAlpha] mat=$materialId ${w}x$h meanA=${"%.1f".format(mean)} " +
                    "zero=${zero * 100 / n}% partial=${partial * 100 / n}% full=${full * 100 / n}% " +
                    "combine=${mt?.combineMode} effectId=${mt?.effectId} effectCombiner=${mt?.effectCombiner} " +
                    "hasAlpha=${materialTextureHasAlpha(materialId)}")
            }
        } catch (t: Throwable) {
            System.err.println("[TexAlpha] mat=$materialId failed: $t")
        }
    }

    /**
     * Whether a material's fragment alpha should come from its TEXTURE.
     *
     * Legacy decides this per material effect. Effects 1 (MaterialEffect1GL) and 7
     * (ReflectingTextureEffect) install their own combiner:
     *   loadTextureCombineMode(..., GL_REPLACE) + setTextureTransparency(0, GL_PRIMARY_COLOR)
     * i.e. fragment alpha is REPLACED by the primary (vertex) colour's alpha and the
     * texture's alpha is never used for transparency at all. Those effects load their
     * texture through getPixelsArgb precisely because they need the alpha channel as a
     * SPECULAR/REFLECTION MASK (setTexture(0, GL_TEXTURE, GL_SRC_ALPHA)) -- which is why
     * their alpha reads as a mostly-dark mask (measured meanA ~13) rather than opacity.
     * Sampling it as transparency tinted whole opaque models translucent: the Canifis
     * lamppost probes as vertex alpha 1.0 but final alpha 0.55 across post, arm and
     * lantern alike.
     *
     * Every other material falls through to loadTextureCombineMode(combineMode), whose
     * alpha combiner does read the texture -- that is what cuts out decals such as the
     * cape icon, so those must keep using it.
     *
     * The predicate is exactly MaterialTypeList.isOpaqueEffect.
     */
    fun materialTextureHasAlpha(materialId: Int): Boolean {
        if (materialId < 0) return false
        val m = materialProvider?.getMaterial(materialId) ?: return false
        return MaterialTypeList.isOpaqueEffect(m.effectId.toInt())
    }

    private fun lookupTextureHandle(materialId: Int): Int {
        // Only -1 is untextured. Material id 0 is a VALID textured material (old-format
        // meshes start texture ids at 0); legacy tests `< 0` (OpenGlToolkit.kt:1895),
        // not `<= 0`. Treating 0 as untextured dumped material-0 faces into the flat
        // vertex-color path — the green multimodel-loc quads.
        if (materialId < 0) return 0

        // Check cache first
        textureHandleCache[materialId]?.let { return it }

        // Look up via texture cache
        val texture = textureCache?.getTexture(materialId)
        val handle = texture?.activeHandle ?: 0

        // Textures stream in asynchronously; getTexture returns null until loaded.
        // Never cache a miss, or the material is stuck untextured (white) forever.
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

    override fun createModel(rsmesh1: Mesh, i_2: Int, i_3: Int, i_4: Int, i_5: Int): Model {
        // Build BEFORE super.createModel. The ModelGL constructor consumes the
        // mesh's billboard list, so by the time it returns, mesh.billboards is empty
        // and ModernModelBuilder can no longer tell which faces are billboard
        // ANCHORS. Legacy hides those (they are never rasterized — renderedFaceCount
        // excludes them); emitting them anyway draws the billboard's tint as a flat
        // untextured quad, which is the "green quads sticking out" symptom.
        // GeometryDiff proved it: every remaining divergence was extraInMesh=N with
        // bb[seen=0], i.e. the builder saw no billboards at all while legacy hid N.
        val built = try {
            ModernModelBuilder.build(rsmesh1, i_4, materialProvider, (i_3 and 0x40) != 0)
        } catch (e: Exception) {
            null  // Never let a build failure break model creation
        }

        val model = super.createModel(rsmesh1, i_2, i_3, i_4, i_5)
        // Build modern geometry from the CLEAN decoded mesh (direct vertex
        // indices, proper texture-projection UVs) BEFORE any post-create baking
        // (loc yaw, mirror, contour, recolor...). The mesh here is unrotated
        // base geometry; ModelGL tracks later mutations (modernYaw accumulates
        // representable yaw; anything else nulls modernBuilt → legacy-capture
        // fallback). i_4 is the model ambient baked into the vertex colors.
        if (model is ModelGL) {
            model.modernBuilt = built
            model.modernYaw = 0
        }
        return model
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
    private var reusableSlotColor = IntArray(4096)
    private var reusableSlotStamp = IntArray(4096)
    private var slotStampCounter = 0

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
    /**
     * Hash of the model's COLOUR state only (face colours, alphas and ambient).
     *
     * Walks faces rather than vertices, so it costs a fraction of
     * computeModelContentHash — which exists to detect geometry changes and has to
     * touch every vertex position.
     */
    private fun colourStateHash(model: ModelGL): Long {
        val fc = model.facesColor ?: return 0L
        val fa = model.facesAlpha
        var h = 1125899906842597L
        h = h * 31 + model.ambient
        val n = model.numFaces
        for (i in 0 until minOf(n, fc.size)) {
            h = h * 31 + fc[i]
            if (fa != null && i < fa.size) h = h * 31 + fa[i]
        }
        return h
    }

    private fun computeModelContentHash(model: ModelGL): Long {
        // 64-BIT hash. The cache and the PERSISTENT instanced geometry store key on
        // this value; with thousands of unique meshes accumulating per session, a
        // 32-bit key hits birthday collisions readily (~2^16 entries), and since the
        // instanced store keeps geometry across frames a single collision PERMANENTLY
        // substituted one mesh's geometry for another (the signpost-drawn-as-foliage
        // class). 64 bits pushes the collision horizon far beyond session scale.
        var hash = 17L
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

        // Include ALL material IDs and face colors via rolling hashes. Sampling only
        // the first/last entries let retexture()/recolor() VARIANTS collide with the
        // base mesh (identical geometry, different mid-array materials/colors) — the
        // colliding variant then permanently drew the WRONG cached faces (e.g. the
        // flat-green textured faces). Cost is O(numFaces), far below the existing
        // O(vertices) position hash above.
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

        // Include texture coordinates: retexture/UV-remap variants can share
        // positions/materials but differ only in UVs — without this they collide.
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
    fun renderModelGLModern(model: ModelGL, modelMatrix: Matrix43) {
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

    private fun renderModelGLOptimized(model: ModelGL, modelMatrix: Matrix43) {
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
        if (frustumCullEnabled && model.boundsCalculated) {
            val vp = viewProjectionMatrix.elements
            if (!isModelVisible(model, modelMatrix, vp)) {
                modelsCulled++
                timeFrustumCull += System.nanoTime() - t1
                return
            }
        }
        timeFrustumCull += System.nanoTime() - t1

        // renderedFaceCount, NOT numFaces — see buildModelCache: faces in
        // [renderedFaceCount, numFaces) are billboard ANCHOR faces that legacy never
        // rasterizes (it paints them as sprites via drawBillboards). They are
        // untextured and carry the billboard's tint as their face colour, so drawing
        // them paints flat green quads over glowing locs (the Canifis signpost).
        val numFaces = model.renderedFaceCount
        if (numFaces <= 0) return
        if (verifyLiveGeometry) GeometryDiff.recordContrast(model.contrast.toInt())

        // Try to get cached model data FIRST (before reserving a batcher slot)
        // Use content-based hash for cache key since ModelGL instances may be recreated each frame
        // IMPORTANT: Skip cache for animated models - their vertex positions change each frame
        // ANIMATED DETECTION — must catch per-pose entity/loc models, not just models
        // carrying live vertexGroups. NPC/player/animated-loc models are regenerated
        // as posed snapshots each animation tick (getAnimatedModel), usually with
        // vertexGroups == null — the old check admitted EVERY POSE into the content
        // cache and the PERSISTENT instanced store: ~9 cache misses/frame, unbounded
        // unique-mesh growth (1647→1843 observed), a full geometry re-upload of the
        // growing store every frame, and repeated capacity-overflow rebuilds — the
        // regime in which animated models (implings, animated signpost) ended up
        // drawing substituted geometry. Models created with the ALTERS_VERTICES
        // function-mask bit (the legacy marker that vertex data will be rewritten —
        // ModelGL init only builds vertexGroups under the same bit, ModelGL.kt:905)
        // are pose-varying by construction and must always stream.
        // PRIMARY SIGNAL: animatedPoseSnapshot is captured at copy time and survives
        // the trailing applyShading(<base mask>) that the NPC-body/head, spot-anim and
        // item pose paths run AFTER animating — that call overwrites functionMask
        // (dropping ALTERS_VERTICES) and nulls vertexGroups, so the two live-model
        // checks below are FALSE for exactly the pose snapshots that flood the store
        // (root cause of the green-quad substitution — every NPC pose became a unique
        // cached mesh). The flag closes that hole; the live checks stay as belt-and-
        // suspenders for any direct-animated model that never went through copy().
        val isAnimated = model.animatedPoseSnapshot ||
            model.vertexGroups != null ||
            ModelFunctionMaskGL.altersVertices(model.functionMask)
        val cache = if (isAnimated) null else modelCache

        val t4 = System.nanoTime()
        if (cache != null) {
            // MESH-BUILT PATH: geometry constructed from the clean decoded mesh
            // at createModel time (ModernModelBuilder) — multi-model locs keep
            // all submeshes (direct vertex indices, no slot reverse-mapping) and
            // textured faces carry proper RS texture-projection UVs. The base
            // geometry is UNROTATED; baked loc yaw is re-applied via the model
            // matrix. Present only while the model's legacy vertices still match
            // the base mesh up to yaw (any other mutation nulls modernBuilt).
            val built = if (meshBuiltPathEnabled) model.modernBuilt else null
            if (built != null) {
                if (diagBudget > 0 && built.batches.size > 1 && diagModelSeen.add(built.id)) {
                    diagBudget--
                    val split = built.batches.joinToString(",") { "${it.materialId}:${it.indices.size / 3}" }
                    System.err.println("[PostDiag] meshBuilt id=${built.id} faces=${built.faceCount} yaw=${model.modernYaw} batches[mat:faces]=$split")
                }
                // EXPERIMENT (-Drender.meshkey=hash): key the mesh-built geometry by
                // the model's CURRENT vertices instead of the shared unrotated
                // base-content hash. built.id is identical for every placement and
                // rotation of a loc, so if any per-placement vertex state is NOT
                // reflected in modernBuilt, one geometry gets served to all of them.
                // Everything else about the path is unchanged, so a difference here
                // isolates the key as the fault.
                if (verifyLiveGeometry) GeometryDiff.verifyLive(model, built)
                // Key on geometry AND colour. built.id identifies the base geometry,
                // which colour-only mutations no longer invalidate, so the colour state
                // has to distinguish otherwise-identical placements (a tinted loc from
                // an untinted one). The colour hash walks FACES, not vertices — far
                // cheaper than the legacy-capture path's full per-vertex content hash.
                // Untouched colours need no hash at all: built.id already identifies
                // the geometry AND its baked base colours. Only a model that actually
                // took a colour mutation has to be distinguished from its siblings.
                // -Drender.verifyrecolour: for an UNMUTATED model, recolour must
                // reproduce the emission bytes exactly. Any mismatch means the recolour
                // path disagrees with the builder about how a face's colour is derived.
                val colourKey = if (model.modernColorRevision == 0) 0L else colourStateHash(model)
                val geometryKey =
                    if (meshKeyByCurrentVertices) computeModelContentHash(model)
                    else built.id * 1000003L + colourKey
                if (VERIFY_RECOLOUR && model.modernColorRevision > 0) {
                    verifyRecolourVsLegacyCapture(model, built)
                }
                val cachedModel = cache.getOrCreate(model, geometryKey) {
                    if (model.modernColorRevision == 0) {
                        built.toCachedModel()
                    } else {
                        // Geometry is reused verbatim; only the colour bytes are redone.
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
                    timeCacheLookup += System.nanoTime() - t4
                    cacheHits++
                    modernBuiltPathModels++
                    submitCachedModel(
                        batcher, cachedModel, geometryKey,
                        composeModernModelMatrix(model, modelMatrix)
                    )
                    return
                }
            }

            // LEGACY-CAPTURE FALLBACK: capture the model's post-connectNormal
            // arrays, keyed by content hash (full-vertex rolling hash — only
            // meaningful for static models; computing it for animated models
            // was pure per-frame waste).
            modernBuiltFallbackModels++
            val t3 = System.nanoTime()
            val modelId = computeModelContentHash(model)
            timeComputeHash += System.nanoTime() - t3

            val cachedModel = cache.getOrCreate(model, modelId) {
                buildModelCache(model)
            }
            timeCacheLookup += System.nanoTime() - t4

            if (cachedModel != null) {
                cacheHits++
                if (VERIFY_RECOLOUR) verifyRecolourAgainst(model, cachedModel)
                submitCachedModel(batcher, cachedModel, modelId, modelMatrix)
                return
            }
        } else {
            modernBuiltFallbackModels++
            timeCacheLookup += System.nanoTime() - t4
        }

        // Fallback: process faces directly (no cache or cache build failed)
        // Register model matrix with the optimized batcher
        selectLightsFor(modelMatrix.xD, modelMatrix.yD, modelMatrix.zD)
        val t2 = System.nanoTime()
        var modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
        timeBeginModel += System.nanoTime() - t2

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

    /**
     * Routes a cached model through the instanced batcher (preferred) or the
     * regular optimized batcher. Shared by the mesh-built and legacy-capture
     * cache paths.
     *
     * @param geometryKey stable key identifying the geometry: the instanced
     *   batcher groups instances by it, so all instances sharing it must carry
     *   identical vertex data (per-instance placement lives in [matrix]).
     */
    private fun submitCachedModel(
        batcher: OptimizedFrameBatcher,
        cachedModel: ModelCache.CachedModel,
        geometryKey: Long,
        matrix: Matrix43
    ) {
        // Pick this model's lights from its own world origin (see selectLightsFor).
        selectLightsFor(matrix.xD, matrix.yD, matrix.zD)
        // INSTANCING PATH: groups identical meshes (by geometryKey) and renders
        // them with glDrawElementsInstanced, using one draw call per material
        // regardless of how many instances exist.
        //
        // Single-use geometry (the legacy-capture path: post-rotation, post-
        // contourToGround vertices) goes here too. Routing it to the streaming
        // batcher instead looks correct on paper — it is never instanced — but
        // OptimizedFrameBatcher.addCachedModel SILENTLY DROPS a model when its
        // vertex buffer fills (returns 0; only beginModel overflow flushes and
        // retries), and the legacy-capture path is ~96% of all models. That
        // overran the 500k-vertex streaming buffer every frame and deleted walls.
        if (instancingEnabled && useInstancing && instancedBatcher?.isActive() == true) {
            val t5 = System.nanoTime()
            val added = instancedBatcher!!.addInstance(cachedModel, geometryKey, matrix, currentLightIdx)
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
        var modelIndex = batcher.beginModel(matrix, currentLightIdx)
        timeBeginModel += System.nanoTime() - t2

        if (modelIndex < 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(matrix, currentLightIdx)
            if (modelIndex < 0) return
        }

        modelCounter++
        debugModelSuccesses++

        val t5 = System.nanoTime()
        var facesAdded = batcher.addCachedModel(cachedModel, modelIndex)

        // addCachedModel returns 0 when its vertex buffer is full and DISCARDS the
        // model — unlike the beginModel overflow above, which already flushes and
        // retries. Silently losing a model here is what made walls vanish and left
        // holes in lamp posts once instanced-store overflow started routing large
        // numbers of models down this path. Flush the batch to reclaim the buffer
        // and retry once, mirroring the beginModel path.
        if (facesAdded == 0 && cachedModel.faceCount > 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(matrix, currentLightIdx)
            if (modelIndex >= 0) {
                facesAdded = batcher.addCachedModel(cachedModel, modelIndex)
            }
            if (facesAdded == 0) streamingModelsDropped++
        }
        timeAddCachedModel += System.nanoTime() - t5
        totalFacesProcessed += facesAdded
    }

    /**
     * Builds a cached representation of the model's geometry.
     * This is called once per unique model, then the cache is reused.
     *
     * NEW: Outputs 40-byte vertex format with pre-computed normalized normals.
     * This eliminates per-vertex normal conversion at render time.
     */
    /**
     * Verifies the colour-only recolour path on MUTATED models.
     *
     * The previous check only compared models with modernColorRevision == 0 -- exactly
     * the ones the optimisation does not touch -- so it reported zero while recoloured
     * bushes rendered white. This compares recolour's output against the colours the
     * currently-correct path produces for the SAME model, face by face, using
     * meshFaceIndices to bridge the mesh-indexed builder to the model-indexed arrays.
     */
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

    /**
     * Compares the recolour path against a legacy-capture build of the SAME mutated
     * model -- the output that renders correctly today. Face order differs between the
     * two (mesh order vs model order), so meshFaceIndices bridges them.
     */
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
        var first: String? = null
        for (i in built.acceptedFaces.indices) {
            val mf = built.acceptedFaces[i]
            if (mf >= meshToModel.size) continue
            val modelFace = meshToModel[mf]
            if (modelFace < 0 || modelFace >= reference.faceCount) continue
            val rp = i * 3 * stride + off
            val cp = modelFace * 3 * stride + off
            if (rp + 3 >= redone.size || cp + 3 >= reference.vertexData.limit()) continue
            var diff = false
            for (k in 0 until 3) if (redone[rp + k] != reference.vertexData.get(cp + k)) diff = true
            if (diff) {
                bad++
                if (first == null) first =
                    "got=(${redone[rp].toInt() and 0xff},${redone[rp+1].toInt() and 0xff},${redone[rp+2].toInt() and 0xff}) " +
                    "want=(${reference.vertexData.get(cp).toInt() and 0xff},${reference.vertexData.get(cp+1).toInt() and 0xff},${reference.vertexData.get(cp+2).toInt() and 0xff})"
            }
        }
        reference.free()
        if (bad > 0) {
            recolourBad++
            if (recolourBad <= 6) System.err.println(
                "[RecolourMut] $bad/${built.acceptedFaces.size} faces rev=${model.modernColorRevision} $first")
        }
    }

    private fun buildModelCache(model: ModelGL): ModelCache.CachedModel? {
        // renderedFaceCount, NOT numFaces: faces in [renderedFaceCount, numFaces) are
        // BILLBOARD ANCHOR faces (uid Long.MAX_VALUE, sorted to the tail by
        // Model.addBillboard). Legacy NEVER rasterizes them — it draws only
        // [0, renderedFaceCount) and paints the billboards as textured sprites via
        // drawBillboards. These anchor faces are untextured and carry the billboard's
        // tint as their face color, so drawing them (as the modern path did, iterating
        // numFaces) renders flat green/tinted quads over implings/glowing locs — the
        // "green quad" bug. Truncating to renderedFaceCount reproduces legacy's exact
        // drawn-face set. (Model billboards themselves are still a modern feature gap.)
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
                    // Legacy BREAKS here on the 0 sentinel (ModelGL.upload:
                    // `normalIdx = vertexNormalIndices[triIdx] - 1; if (normalIdx == -1) break`).
                    // The slot run for a vertex is zero-terminated inside its
                    // [triangles[v], triangles[v+1]) window; entries past the
                    // terminator belong to other vertices. Skipping instead of
                    // breaking claimed those slots for the wrong vertex, and since
                    // later vertices overwrite, faces referencing them got wrong
                    // positions — the stretched green quads on the Canifis signpost.
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

        // A face failing the check above is SILENTLY DISCARDED — geometry vanishes
        // with no signal anywhere. That is the documented failure mode of this
        // legacy-capture path ("dropped whole submeshes on multi-model locs", see
        // ModernModelBuilder's header), and the suspected cause of the missing
        // Canifis walls and the signpost's missing post. Count it so the next run
        // says whether faces are actually being dropped, and by how much.
        val droppedFaces = numFaces - validFaceCount
        if (droppedFaces > 0) {
            legacyCaptureDroppedFaces += droppedFaces.toLong()
            legacyCaptureModelsWithDrops++
            if (droppedFaces == numFaces) legacyCaptureModelsFullyDropped++
            if (diagBudget > 0 && diagDropSeen.add(numFaces * 31 + droppedFaces)) {
                diagBudget--
                System.err.println(
                    "[DropDiag] legacy-capture dropped $droppedFaces/$numFaces faces " +
                        "(directIdx=$useDirectVertexIndices maxDepth=$maxDepth " +
                        "numNormals=$numNormals numVertices=${model.numVertices})"
                )
            }
        }

        if (validFaceCount == 0) return null

        // Allocate buffers - NEW 40-byte format
        val vertexCount = validFaceCount * 3
        val vertexData = MemoryUtil.memAlloc(vertexCount * ModelCache.BYTES_PER_VERTEX)  // 40 bytes per vertex
        val materialIds = ShortArray(validFaceCount)

        var outFaceIdx = 0

        // Legacy scales the sun term by 768/contrast per MODEL (ModelGL:1933:
        // normalScale = sunColorIntensity * 768 / contrast). The shader has only a
        // global uContrastPos, so fold the per-model factor into the NORMAL LENGTH:
        // dot(sun, N * k) * sunColorIntensity == dot(sun, N) * sunColorIntensity * k.
        // The shaders must therefore NOT renormalise. Measured contrast is 850-1000,
        // never 768, so omitting this left models ~1.20x too bright — matching the
        // 1.18x measured against legacy.
        val contrastScale = if (model.contrast.toInt() != 0) 768f / model.contrast.toInt() else 1f

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
                        vertexData.putFloat(nX / len * contrastScale)
                        vertexData.putFloat(nY / len * contrastScale)
                        vertexData.putFloat(nZ / len * contrastScale)
                    } else {
                        vertexData.putFloat(0f).putFloat(contrastScale).putFloat(0f)
                    }
                } else {
                    // Normal stored as N*magnitude
                    val invMag = 1f / (mag * 256f)
                    val rawNx = nX * invMag
                    val rawNy = nY * invMag
                    val rawNz = nZ * invMag
                    val len = kotlin.math.sqrt(rawNx * rawNx + rawNy * rawNy + rawNz * rawNz)
                    if (len > 0.001f) {
                        vertexData.putFloat(rawNx / len * contrastScale)
                        vertexData.putFloat(rawNy / len * contrastScale)
                        vertexData.putFloat(rawNz / len * contrastScale)
                    } else {
                        vertexData.putFloat(0f).putFloat(contrastScale).putFloat(0f)
                    }
                }
            } else {
                // Default up normal (carrying the per-model contrast scale)
                vertexData.putFloat(0f).putFloat(contrastScale).putFloat(0f)
            }
        }

        // Legacy colours are PER VERTEX SLOT, not per face: ModelGL.upload writes
        // hslToRgb(face) into the slots named by facesA/B/C for every face in order
        // (ModelGL.kt:2050-2058), so on shared vertices the LAST face wins for both
        // RGB and alpha. Emitting per-face colour instead let translucent faces keep
        // alpha ~127 on vertices legacy had already overwritten opaque -- the
        // see-through Canifis lampposts.
        val slotCount = if (useDirectVertexIndices) maxDepth else numNormals
        if (reusableSlotColor.size < slotCount) {
            reusableSlotColor = IntArray(slotCount * 2)
        }
        val slotColor = reusableSlotColor
        if (reusableSlotStamp.size < slotCount) {
            reusableSlotStamp = IntArray(slotCount * 2)
        }
        val slotStamp = reusableSlotStamp
        val stamp = ++slotStampCounter
        val colorFaceCount = minOf(model.numFaces, facesA.size, facesB.size, facesC.size)
        for (f in 0 until colorFaceCount) {
            val mat = facesMaterial?.get(f)?.toInt() ?: -1
            val hsl = facesColor?.get(f)?.toInt()?.and(0xFFFF) ?: 0
            val fa = facesAlpha?.get(f) ?: 0.toByte()
            val rgba = model.hslToRgb(hsl.toShort(), mat.toShort(), modelAmbient, fa)
            val sa = facesA[f].toInt() and 0xFFFF
            val sb = facesB[f].toInt() and 0xFFFF
            val sc = facesC[f].toInt() and 0xFFFF
            if (sa < slotCount) { slotColor[sa] = rgba; slotStamp[sa] = stamp }
            if (sb < slotCount) { slotColor[sb] = rgba; slotStamp[sb] = stamp }
            if (sc < slotCount) { slotColor[sc] = rgba; slotStamp[sc] = stamp }
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

            // Base colour: per vertex slot (legacy last-face-wins), not per face.
            // A slot the pre-pass never touched (model.numFaces not covering this
            // face) would read 0 = alpha 0 = invisible; fall back to the face colour.
            val faceRgba = run {
                val hsl = facesColor?.get(faceIdx)?.toInt()?.and(0xFFFF) ?: 0
                val fa = facesAlpha?.get(faceIdx) ?: 0.toByte()
model.hslToRgb(hsl.toShort(), materialId.toShort(), modelAmbient, fa)
            }
            val useSlot = slotColorEnabled
            val rgbaA = if (useSlot && idxA < slotCount && slotStamp[idxA] == stamp) slotColor[idxA] else { if (useSlot) unwrittenSlotFaces++; faceRgba }
            val rgbaB = if (useSlot && idxB < slotCount && slotStamp[idxB] == stamp) slotColor[idxB] else faceRgba
            val rgbaC = if (useSlot && idxC < slotCount && slotStamp[idxC] == stamp) slotColor[idxC] else faceRgba
            val colorRA = ((rgbaA ushr 24) and 0xFF).toByte()
            val colorGA = ((rgbaA ushr 16) and 0xFF).toByte()
            val colorBA = ((rgbaA ushr 8) and 0xFF).toByte()
            val colorAA = (rgbaA and 0xFF).toByte()
            val colorRB = ((rgbaB ushr 24) and 0xFF).toByte()
            val colorGB = ((rgbaB ushr 16) and 0xFF).toByte()
            val colorBB = ((rgbaB ushr 8) and 0xFF).toByte()
            val colorAB = (rgbaB and 0xFF).toByte()
            val colorRC = ((rgbaC ushr 24) and 0xFF).toByte()
            val colorGC = ((rgbaC ushr 16) and 0xFF).toByte()
            val colorBC = ((rgbaC ushr 8) and 0xFF).toByte()
            val colorAC = (rgbaC and 0xFF).toByte()

            // Vertex 0: position(12) + normal(12) + texcoord(8) + color(4) + modelIndex(4) = 40 bytes
            vertexData.putFloat(verticesX[vertA].toFloat())
            vertexData.putFloat(verticesY[vertA].toFloat())
            vertexData.putFloat(verticesZ[vertA].toFloat())
            writeNormal(idxA)  // Pre-computed normalized normal
            vertexData.putFloat(if (textureU != null && idxA < textureU.size) textureU[idxA] else 0f)
            vertexData.putFloat(if (textureV != null && idxA < textureV.size) textureV[idxA] else 0f)
            vertexData.put(colorRA).put(colorGA).put(colorBA).put(colorAA)
            vertexData.putFloat(0f)  // modelIndex placeholder

            // Vertex 1
            vertexData.putFloat(verticesX[vertB].toFloat())
            vertexData.putFloat(verticesY[vertB].toFloat())
            vertexData.putFloat(verticesZ[vertB].toFloat())
            writeNormal(idxB)
            vertexData.putFloat(if (textureU != null && idxB < textureU.size) textureU[idxB] else 0f)
            vertexData.putFloat(if (textureV != null && idxB < textureV.size) textureV[idxB] else 0f)
            vertexData.put(colorRB).put(colorGB).put(colorBB).put(colorAB)
            vertexData.putFloat(0f)

            // Vertex 2
            vertexData.putFloat(verticesX[vertC].toFloat())
            vertexData.putFloat(verticesY[vertC].toFloat())
            vertexData.putFloat(verticesZ[vertC].toFloat())
            writeNormal(idxC)
            vertexData.putFloat(if (textureU != null && idxC < textureU.size) textureU[idxC] else 0f)
            vertexData.putFloat(if (textureV != null && idxC < textureV.size) textureV[idxC] else 0f)
            vertexData.put(colorRC).put(colorGC).put(colorBC).put(colorAC)
            vertexData.putFloat(0f)

            outFaceIdx++
        }

        vertexData.flip()

        // Build pre-sorted material batches to eliminate runtime sorting
        // Group faces by material ID and create index arrays
        val materialToIndices = mutableMapOf<Int, MutableList<Int>>()
        for (faceIdx in 0 until validFaceCount) {
            // SIGNED read: untextured faces store materialId -1 (0..N are textured,
            // INCLUDING material 0 — old-format meshes start texture ids at 0). The
            // untextured test is `< 0`, NOT `<= 0`: `<= 0` folded valid material-0
            // faces into the untextured batch → flat green quads on multimodel locs.
            val materialId = materialIds[faceIdx].toInt()
            val normalizedId = if (materialId < 0) -1 else materialId
            val indices = materialToIndices.getOrPut(normalizedId) { mutableListOf() }
            val baseVertex = faceIdx * 3
            indices.add(baseVertex)
            indices.add(baseVertex + 1)
            indices.add(baseVertex + 2)
        }

        // Sort materials (untextured first, then by ID) and create batches
        val sortedMaterials = materialToIndices.keys.sortedBy { if (it < 0) Int.MIN_VALUE else it }
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
        modelMatrix: Matrix43,
        batcher: OptimizedFrameBatcher,
        modelIndex: Int
    ) {
        // renderedFaceCount, NOT numFaces — exclude billboard-anchor faces at the tail
        // (see buildModelCache for the full rationale: they are the flat green quads).
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
                    // Legacy BREAKS here on the 0 sentinel (ModelGL.upload:
                    // `normalIdx = vertexNormalIndices[triIdx] - 1; if (normalIdx == -1) break`).
                    // The slot run for a vertex is zero-terminated inside its
                    // [triangles[v], triangles[v+1]) window; entries past the
                    // terminator belong to other vertices. Skipping instead of
                    // breaking claimed those slots for the wrong vertex, and since
                    // later vertices overwrite, faces referencing them got wrong
                    // positions — the stretched green quads on the Canifis signpost.
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
        val normals = reusableNormals  // 9 floats for 3 vertex normals

        // Legacy colours are PER VERTEX SLOT, not per face. ModelGL.upload writes
        // hslToRgb(face) into the slots named by facesA/B/C for every face in order
        // (ModelGL.kt:2050-2058), so where faces share a vertex the LAST face to
        // touch it wins -- for both RGB and alpha. Emitting per-face colour instead
        // let a lamppost's translucent faces keep alpha ~127 on vertices that legacy
        // had already overwritten to opaque by a later face, which is what made the
        // Canifis lampposts see-through under this toolkit.
        val slotCount = if (useDirectVertexIndices) maxDepth else numNormals
        if (reusableSlotColor.size < slotCount) {
            reusableSlotColor = IntArray(slotCount * 2)
        }
        val slotColor = reusableSlotColor
        if (reusableSlotStamp.size < slotCount) {
            reusableSlotStamp = IntArray(slotCount * 2)
        }
        val slotStamp = reusableSlotStamp
        val stamp = ++slotStampCounter
        // Iterate model.numFaces, matching legacy's loop bound -- faces the render
        // pass skips still contribute their colour to shared slots.
        val colorFaceCount = minOf(model.numFaces, facesA.size, facesB.size, facesC.size)
        for (f in 0 until colorFaceCount) {
            val mat = facesMaterial?.get(f)?.toInt() ?: -1
            val hsl = facesColor?.get(f)?.toInt()?.and(0xFFFF) ?: 0
            val fa = facesAlpha?.get(f) ?: 0.toByte()
            val rgba = model.hslToRgb(hsl.toShort(), mat.toShort(), modelAmbient, fa)
            val sa = facesA[f].toInt() and 0xFFFF
            val sb = facesB[f].toInt() and 0xFFFF
            val sc = facesC[f].toInt() and 0xFFFF
            if (sa < slotCount) { slotColor[sa] = rgba; slotStamp[sa] = stamp }
            if (sb < slotCount) { slotColor[sb] = rgba; slotStamp[sb] = stamp }
            if (sc < slotCount) { slotColor[sc] = rgba; slotStamp[sc] = stamp }
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

            // Per-vertex-slot colour (legacy last-face-wins), not per-face.
            // A slot the pre-pass never touched (model.numFaces not covering this
            // face) would read 0 = alpha 0 = invisible; fall back to the face colour.
            val faceRgba = run {
                val hsl = facesColor?.get(faceIdx)?.toInt()?.and(0xFFFF) ?: 0
                val fa = facesAlpha?.get(faceIdx) ?: 0.toByte()
model.hslToRgb(hsl.toShort(), materialId.toShort(), modelAmbient, fa)
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
                lx0, ly0, lz0, normals[0], normals[1], normals[2], u0, v0, rA, gA, bA, aA,
                lx1, ly1, lz1, normals[3], normals[4], normals[5], u1, v1, rB, gB, bB, aB,
                lx2, ly2, lz2, normals[6], normals[7], normals[8], u2, v2, rC, gC, bC, aC
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
    private fun renderModelGLLegacy(model: ModelGL, modelMatrix: Matrix43) {
        // Pick this model's lights from its own world origin (see selectLightsFor).
        selectLightsFor(modelMatrix.xD, modelMatrix.yD, modelMatrix.zD)

        val batcher = frameBatcher ?: return
        if (!batcher.isActive()) return

        // renderedFaceCount, NOT numFaces — excludes the billboard anchor tail.
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
                    // Legacy BREAKS here on the 0 sentinel (ModelGL.upload:
                    // `normalIdx = vertexNormalIndices[triIdx] - 1; if (normalIdx == -1) break`).
                    // The slot run for a vertex is zero-terminated inside its
                    // [triangles[v], triangles[v+1]) window; entries past the
                    // terminator belong to other vertices. Skipping instead of
                    // breaking claimed those slots for the wrong vertex, and since
                    // later vertices overwrite, faces referencing them got wrong
                    // positions — the stretched green quads on the Canifis signpost.
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

        surfaceMaskRenderer?.destroy()
        surfaceMaskRenderer = null

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
    fun getOptimizedBatcherStats(): OptimizedFrameBatcher.FrameStats? {
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
        batcher.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)

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
        model: ModelGL,
        modelMatrix: Matrix43,
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

        // Bounding-sphere radius enclosing the transformed AABB. This is the LENGTH
        // of the extent vector, not its largest component: max() under-estimates by
        // up to sqrt(3), which shrinks the test sphere and culls geometry whose real
        // bounds still cross a frustum plane. Thin geometry (walls, posts, lamp
        // stems) is hit hardest, since one tiny extent drags the max down while the
        // true radius stays large.
        val radius = kotlin.math.sqrt(
            worldExtentX * worldExtentX + worldExtentY * worldExtentY + worldExtentZ * worldExtentZ
        )

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
        val combineMode = if (System.getProperty("render.groundcolor")?.equals("true", true) == true) {
            99
        } else if (materialId >= 0) {
            materialProvider?.getMaterial(materialId)?.combineMode ?: 0
        } else {
            0
        }

        // Raw legacy sun parameters for HD_LIGHT lighting (GroundGL.kt:458-459 formula)
        gr.setLegacySunParams(
            sunDirection[0], sunDirection[1], sunDirection[2],
            sunBrightness, sunColorIntensity, antiSunColorIntensity
        )

        if (verifyLiveGeometry) GeometryDiff.recordGround(materialId, combineMode, colorVboId, colorData != null)

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
