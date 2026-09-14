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
    private var billboardRendererModern: ModernBillboardRenderer? = null

    // ---- Deferred model billboards (ModelGL.drawBillboards on the modern path) ----
    // Legacy draws them right after each model; here they join the ordered
    // translucent pass at the model's depth key, like the deferred particles.
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

    /**
     * Queues [model]'s billboards for the translucent pass. [modelMatrix] is the
     * legacy model matrix; eye-space maths follows ModelGL.drawBillboards exactly.
     */
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
            // model -> world (Matrix43 columns A/B/C, translation D), then world -> eye
            // with the row-vector view matrix, as legacy's (model x view) does.
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
    /** Never-reset deferred build time (stall diag). */
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

    /** Retained-scene entry point: [model] with its captured legacy matrix, at [depthKey]. */
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
    private var groundIndexScratch = ShortArray(0)
    private var groundIndexUpload: java.nio.ByteBuffer? = null

    private val GROUND_PROBE = System.getProperty("render.sunprobe") != null
    private var groundProbeTick = 0
    /** -Drender.groundcolor=true forces the ground shader's vertex-colour debug mode. */
    private val GROUND_COLOR_DEBUG = System.getProperty("render.groundcolor")?.let { it.equals("true", true) || it.equals("raw", true) } == true
    /** -Drender.groundcombine=<mode>: force one legacy combine mode for every ground material (A/B aid). */
    private val GROUND_COMBINE_OVERRIDE = System.getProperty("render.groundcombine")?.toIntOrNull() ?: -1

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

    /**
     * Frame-wide ordering of everything the scene draws translucently: model
     * instances in both batchers and deferred particle systems all take a number
     * from this counter at submission, and the translucent pass replays them in that
     * order — the scene's alpha-bucket order, which is legacy's only translucency
     * mechanism.
     */
    private var translucentSeqCounter = 0

    /**
     * Particle systems submitted during the scene walk, drawn in the translucent
     * pass at their sequence position. Legacy draws them immediately after the
     * emitting entity's models; the deferred batchers used to reproduce that by
     * FLUSHING THE WHOLE SCENE at every emitter — at the spawn hub several full
     * flushes (and translucent replays) per frame, ~35% of the frame.
     */
    private val deferredParticles = ArrayList<ParticleWrapper>()
    private var deferredParticleSeq = IntArray(64)
    private var deferredParticleKey = IntArray(64)
    private var deferredParticleOrder = IntArray(64)

    private val BB_DIAG = System.getProperty("render.bbdiag") != null
    private val LIGHT_DIAG = System.getProperty("render.lightdiag") != null
    private val STALL_DIAG_MS = System.getProperty("render.stalldiag")?.toLongOrNull() ?: 0L
    /** Build private pose geometry on worker threads at flush (-Drender.parallelbuild=false disables). */
    private val PARALLEL_BUILD = System.getProperty("render.parallelbuild")?.equals("false", true) != true
    private val TRANS_DIAG = System.getProperty("render.transdiag") != null
    /** -Drender.forcestreamtrans: route every translucent model through the streaming batcher (A/B aid). */
    private val FORCE_STREAM_TRANSLUCENT = System.getProperty("render.forcestreamtrans") != null
    private var transDiagTick = 0
    private var transDiagInst = 0
    private var transDiagStream = 0
    private var transDiagMinKey = Int.MAX_VALUE
    private var transDiagMaxKey = Int.MIN_VALUE

    /** Retained static-scene store (see RetainedStaticScene); null when disabled. */
    private var retainedScene: RetainedStaticScene? = null

    /**
     * While non-null, model submissions are RECORDED here instead of drawn: the
     * retained scene captures a static entity's draw (its resolved cached geometry and
     * model matrix) by running the entity's own draw() in this mode.
     */
    private var captureList: ArrayList<RetainedStaticScene.Capture>? = null
    /** Set when a capture saw a submission the retained path cannot replay (streamed/animated). */
    private var captureUnsupported = false
    /** The legacy model matrix of the model being captured (before yaw composition). */
    private var captureLegacyMatrix: Matrix43? = null

    /** Faces silently discarded by buildModelCache's validity check (see there). */
    private val legacyCaptureDroppedFaces = java.util.concurrent.atomic.AtomicLong()
    private val legacyCaptureModelsWithDrops = java.util.concurrent.atomic.AtomicLong()
    private val legacyCaptureModelsFullyDropped = java.util.concurrent.atomic.AtomicLong()

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
         * Source of per-instance geometry keys for animated models.
         *
         * Starts far above zero and only ever increases, so a key is never reused and
         * never mistaken for "unassigned". Only the render thread assigns keys.
         */
        private var streamKeyCounter = 0x4000_0000_0000_0000L

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
     * Bloom on the modern renderer is a native post-process ([ModernBloomPass]); the
     * legacy FramebufferCompositor/RectangularTextureGL path (immediate-mode quads
     * through jaggl) is never activated here. The game's contract is unchanged:
     * bloomSupported() switches the effect on, resetBloom() off, beginBloomCapture()
     * redirects the game-window component into the capture target and
     * compositeBloomEffects() writes the result over the window.
     * -Drender.bloomsupport=false hides the option; -Drender.bloom=false refuses to
     * enable it (same switch the legacy toolkit honours).
     */
    private var modernBloom: ModernBloomPass? = null
    private var modernBloomOn = false
    private var modernBloomCapturing = false
    private var modernBloomAborts = 0L

    override fun hasBloomSupport(): Boolean =
        System.getProperty("render.bloomsupport")?.equals("false", true) != true

    /**
     * A zoomed-out view touches well over the legacy 256 materials across a camera
     * rotation, and the legacy cycle demoted anything unused for 5 frames; both made
     * the game thread regenerate procedural textures (TextureOp*) every frame while
     * rotating (~10% of the frame in the spin profile). The bindless handle cache
     * short-circuits the LRU, so lookupTextureHandle touches the LRU entry once per
     * frame to keep recency meaningful.
     */
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
        // Nothing drawn so far this frame may end up in the capture, and the scene
        // target must not stay bound to the window while the capture FBO takes over.
        flushSceneForUiDraw()
        endSpriteBatch()
        resolveSceneMsaa()
        if (!bp.begin(screenCanvasWidth, screenCanvasHeight)) return
        modernBloomCapturing = true
        // Same as the legacy compositor's prepareForRendering: fresh colour + depth.
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
            // An offscreen display is still pushed: no window to composite onto.
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

    /** Capture left open (component never composited): copy it to the window as-is. */
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
        // The pass leaves depth/blend/scissor/cull disabled and unit 0 active; make the
        // legacy state cache re-initialise on the next setup call, as endSpriteBatch does.
        functionMask = 0
        resetClipping()
        updateViewportAndCenter()
        resyncLegacyTextureBinding()
    }

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
        // target"): with this flag set, legacy sprite textures (OpenGl2DImageTexture)
        // are created as GL_TEXTURE_RECTANGLE, but the modern sprite pipeline binds
        // everything as GL_TEXTURE_2D — every bind raised GL_INVALID_OPERATION and
        // every UI quad sampled nothing. Force the legacy power-of-two/TEXTURE_2D
        // fallback path (the same one 2012 low-end GPUs used) so sprite textures
        // match the modern renderer's target. Material/font textures were already
        // TEXTURE_2D, which is why the 3D world and glyphs rendered while all
        // sprites were invisible.
        this.arbRectangleTexturesSupported = false
    }

    /**
     * Releases the swap from the display refresh, so the frame rate is bounded by how
     * fast frames can be produced rather than by the presentation path.
     *
     * With the default swap interval, glXSwapBuffers blocked for ~1.4 ms of every
     * frame on this driver even with the GPU completely idle — and every millisecond
     * removed from the scene simply moved into that block, which is why frame time sat
     * flat while measured scene time fell. Any target above the display's refresh rate
     * requires this; the cost is that presentation is no longer tear-free, so
     * `-Drender.vsync=true` puts it back.
     */
    private fun applySwapInterval() {
        if (System.getProperty("render.vsync")?.equals("true", true) == true) return
        // Whichever swap-control extension the driver exposes. LWJGL resolves entry
        // points lazily, so an unavailable one throws rather than reporting absence.
        val glxDisplay = try { org.lwjgl.opengl.GLX14.glXGetCurrentDisplay() } catch (e: Throwable) { 0L }
        val glxDrawable = try { org.lwjgl.opengl.GLX.glXGetCurrentDrawable() } catch (e: Throwable) { 0L }
        if (glxDisplay != 0L && glxDrawable != 0L) {
            try {
                org.lwjgl.opengl.GLXEXTSwapControl.glXSwapIntervalEXT(glxDisplay, glxDrawable, 0)
                println("[ModernGLToolkit] swap interval 0 via GLX_EXT_swap_control")
                return
            } catch (e: Throwable) {
                // fall through to SGI
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
        // The legacy software occluder must never decide visibility for the modern
        // path (it dropped whole locs that another loc barely overlapped).
        Culling.disabledForModern = true
        if (isModernRendererInitialized) return

        try {
            // GL.createCapabilities() must have been called on this thread already
            // (handled in render() after swapBuffers makes the GL context current)

            gpuTimer.initialize()
            applySwapInterval()

            modernRenderer = ModernModelRenderer()
            modernRenderer!!.initialize()

            groundRenderer = ModernGroundRenderer()
            groundRenderer!!.initialize()

            spriteRenderer = ModernSpriteRenderer()
            spriteRenderer!!.initialize()
            spriteRenderer!!.scissorRestore = {
                // Raw re-enable: the legacy cache still says "enabled", which is what
                // GL is about to be again, so no cache update is needed.
                if (modernScissorEnabled) GLWrapper.enable(GLWrapper.Constants.SCISSOR_TEST)
            }

            surfaceMaskRenderer = ModernSurfaceMaskRenderer()
            surfaceMaskRenderer!!.initialize()

            particleRendererModern = ModernParticleRenderer()
            particleRendererModern!!.initialize()

            billboardRendererModern = ModernBillboardRenderer()
            billboardRendererModern!!.initialize()

            // Initialize frame batchers for draw call consolidation
            frameBatcher = FrameBatcher()
            frameBatcher!!.initialize()

            optimizedBatcher = OptimizedFrameBatcher()
            optimizedBatcher!!.initialize()
            optimizedBatcher!!.setTextureScrollLookup(::lookupTextureScroll)

            // Initialize instanced model batcher for repeated meshes
            instancedBatcher = InstancedModelBatcher()
            instancedBatcher!!.initialize()
            instancedBatcher!!.setTextureScrollLookup(::lookupTextureScroll)

            // Initialize model cache for avoiding per-face CPU work
            // Entry count must exceed the live unique-mesh count (inst[unique=] runs
            // ~2500-3200), or every miss evicts geometry that is needed again moments
            // later: mc[size=1024 evict=853] with tkMs build=26.82ms on a 67ms frame.
            // maxVertexBytes stays the real memory bound.
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

    // Debug counters (reset each frame in render, logged before swap)
    /** Animated/pose geometry served from the model's own cache vs rebuilt. */
    private var streamGeometryHits = 0L
    private var streamGeometryBuilds = 0L

    /** -Drender.animinstanced=false streams animated poses instead of keeping them resident. */
    private val animatedInstancing = System.getProperty("render.animinstanced")?.equals("false", true) != true

    /** -Drender.verifyhash: recompute the memoised content hash every frame and report drift. */
    private val VERIFY_HASH = System.getProperty("render.verifyhash") != null
    private var hashVerifyChecks = 0L
    private var hashVerifyBad = 0L

    private var debugFrameCount = 0

    /** Frames between [ModernDiag]/[ModernPerf] prints. Override with `-Drender.diaginterval=<n>`. */
    private val diagInterval = System.getProperty("render.diaginterval")?.toIntOrNull()?.coerceAtLeast(1) ?: 600

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
        if (debugFrameCount % diagInterval == 0 || debugFrameCount == 30 || debugFrameCount == 120) {
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
            // Performance breakdown (last frame's timings; cache counters since last print)
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
                    // Animated/pose geometry reuse: builds should track the 50Hz logic
                    // rate, not the render rate. hits/(hits+builds) rising with fps is
                    // the whole point of the per-instance stream cache.
                    "stream=${streamGeometryHits}h/${streamGeometryBuilds}b${streamBuildsByClass.entries.sortedByDescending { it.value[0] }.joinToString(",", "[", "]") { "${it.key.simpleName}=${it.value[0]}/${it.value[1]}f" }} spriteFlush=${spriteRenderer?.let { sr -> "${sr.flushCauses.values.sum() / diagInterval}/frame(quads=${sr.flushQuads / diagInterval})" + sr.flushCauses.entries.sortedByDescending { it.value }.joinToString(",", "[", "]") { "${it.key}=${it.value / diagInterval}" } } ?: "-"} walkCacheRebuilds=${Scene.walkCacheRebuildsTotal()} ${PoseMemo.statsAndReset()} deferredBuild[n=${deferredBuildsTotal / diagInterval}/frame ms=${"%.2f".format(deferredBuildMsAcc / diagInterval)}] bb[def=${bbDeferredTotal / diagInterval} drawn=${bbDrawnTotal / diagInterval}] " +
                    "bounds=${ModelGL.boundsCalls / diagInterval}/frame(verts=${ModelGL.boundsVertices / diagInterval}) " +
                    (if (VERIFY_HASH) "hashVerify=$hashVerifyChecks/bad=$hashVerifyBad " else "") +
                    // Mesh-built geometry coverage: models drawn from the clean-mesh
                    // build vs models on the legacy-capture fallback (animated poses,
                    // mutated models, or no build available).
                    "meshBuilt=$modernBuiltPathModels/fallback=$modernBuiltFallbackModels " +
                    // Faces the legacy-capture path threw away. Nonzero here means
                    // scenery is losing geometry before it ever reaches the GPU.
                    "dropped[faces=${legacyCaptureDroppedFaces.get()} models=${legacyCaptureModelsWithDrops.get()} " +
                    "whole=${legacyCaptureModelsFullyDropped.get()} stream=$streamingModelsDropped] " +
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
                    "trans[groups=${(instancedBatcher?.translucentGroupsDrawn ?: 0L) / diagInterval} cmds=${(instancedBatcher?.translucentCommandsDrawn ?: 0L) / diagInterval}] " +
                    "inst[unique=${instStats?.uniqueMeshes ?: 0} instances=${instStats?.totalInstances ?: 0} " +
                    "drawCalls=${instStats?.drawCalls ?: 0}] " +
                    // Geometry-store residency: evicted/overflow should sit at 0 in
                    // steady state; sustained nonzero means the store is undersized
                    // for the working set (raise MAX_TOTAL_VERTICES).
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
            // Phase timers are averages over the samples accumulated since the last
            // print. They used to accumulate for the whole session, which made them
            // dominated by startup frames and effectively frozen — a change worth
            // several milliseconds moved the printed value by hundredths.
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

            // (Presentation/verdict probe removed — it served its purpose finding the
            // rectangle-texture root cause. Console diagnostics above are draw-free.)
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

        // -Drender.gpusync: block until the GPU has drained before presenting, and
        // report how long that took. A large value here means the frame is GPU-bound
        // and no amount of CPU work removal will raise the frame rate; a near-zero
        // value means the GPU is idle waiting on us.
        if (GPU_SYNC) {
            val fenceStartNs = System.nanoTime()
            glFinish()
            phaseGpuWaitNs += System.nanoTime() - fenceStartNs
        }

        // Parent render: swapBuffers via jaggl (presents the framebuffer)
        val swapStartNs = System.nanoTime()
        gpuTimer.frameStampEnd()
        super.render()
        gpuTimer.frameStampStart()
        val swapEndNs = System.nanoTime()
        perfSwapNsAcc += swapEndNs - swapStartNs
        perfRenderNsAcc += swapEndNs - perfLastRenderEntryNs

        gpuTimer.nextFrame()

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
    override fun setModelMatrix(modelMatrix: Matrix44) {
        // Call parent to set up legacy matrix state
        super.setModelMatrix(modelMatrix)

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
                if (hasPendingSceneGeometry()) {
                    // Flush collected geometry with the VP it was queued under,
                    // then fall through to reopen the batch with the new VP.
                    renderWithModernPipeline()
                    functionMask = 0
                    batcherActive = false
                } else {
                    // Batch open but empty: just re-arm the new VP and lighting.
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

    /**
     * Opens the deferred scene batch against the CURRENT viewProjectionMatrix and
     * sun state. Called from setModelMatrix (the normal scene entry) and LAZILY from
     * renderModelGLOptimized when a model arrives while no batch is open — some
     * paths draw models without a preceding setModelMatrix on THIS toolkit; legacy
     * drew them immediately against the live matrices (ModelGL.kt:2311-2313), so
     * opening against the live VP is exact parity and models must never be dropped.
     */
    // ---- Multisampled scene target -------------------------------------------------
    // The window's default framebuffer has no sample buffers on this path (GL_SAMPLES
    // reads 0 even with the anti-aliasing preference at 4), and legacy only ever got
    // anti-aliasing from its FramebufferCompositor's multisampled FBO — a
    // fixed-function path. So the modern scene draws into its own multisampled FBO,
    // seeded with the current window contents (so translucent geometry and any 2D
    // quads flushed mid-scene composite over what is already there) and resolved back
    // by a blit whenever the batch flushes or the render target changes.
    private var sceneMsaa: MultisampleFramebuffer? = null
    private var sceneMsaaBound = false
    private var sceneMsaaRestoreFbo = 0
    private var sceneMsaaFailed = false
    /** Samples of the bound scene target (diag). */
    private var sceneMsaaActiveSamples = 0

    private fun bindSceneMsaa() {
        val samples = multiSamplesPreference
        if (samples <= 1 || sceneMsaaFailed || sceneMsaaBound) return
        val d = display ?: return
        val w = d.getWidth()
        val h = d.getHeight()
        if (w <= 0 || h <= 0) return
        var fb = sceneMsaa
        if (fb == null || fb.width != w || fb.height != h || fb.samples != samples) {
            fb?.destroy()
            fb = MultisampleFramebuffer(w, h, samples)
            if (!fb.create()) {
                sceneMsaaFailed = true
                sceneMsaa = null
                System.err.println("[ModernGLToolkit] multisampled scene target unavailable ($samples samples); rendering without anti-aliasing")
                return
            }
            sceneMsaa = fb
        }
        sceneMsaaRestoreFbo = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sceneMsaaRestoreFbo)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fb.getId())
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL_COLOR_BUFFER_BIT, GL_NEAREST)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fb.getId())
        sceneMsaaBound = true
        sceneMsaaActiveSamples = samples
    }

    /** Resolves the multisampled scene into the framebuffer that was bound before [bindSceneMsaa]. */
    private fun resolveSceneMsaa() {
        if (!sceneMsaaBound) return
        sceneMsaaBound = false
        val fb = sceneMsaa ?: return
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, fb.getId())
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneMsaaRestoreFbo)
        GL30.glBlitFramebuffer(0, 0, fb.width, fb.height, 0, 0, fb.width, fb.height, GL_COLOR_BUFFER_BIT, GL_NEAREST)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, sceneMsaaRestoreFbo)
    }

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
        bindSceneMsaa()
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
        translucentSeqCounter = 0
    }

    private fun renderWithModernPipeline() {
        try {
            renderWithModernPipelineInner()
        } finally {
            // Every exit path, including the early returns, must hand the window its
            // resolved scene back.
            resolveSceneMsaa()
            setDepthMask()
        }
    }

    private fun renderWithModernPipelineInner() {
        val mr = modernRenderer ?: return
        val gr = groundRenderer ?: return
        val batcher = frameBatcher ?: return
        // Legacy enables GL_MULTISAMPLE in setupMatrixRendering before every model
        // draw (and disables it for sprites). The modern pass issues no legacy setup
        // call, so it must switch multisampling on itself or the multisampled default
        // framebuffer (init(..., multiSamplesPreference)) never resolves any edge.
        setMultisampleEnabled(true)
        if (fbSamplesQueried < 0) {
            fbSamplesQueried = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_SAMPLES)
            fbSampleBuffers = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_SAMPLE_BUFFERS)
        }

        // Get display dimensions
        val presentTarget = delegatingDisplay ?: return
        val width = presentTarget.getWidth()
        val height = presentTarget.getHeight()
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
            clearDeferredBuilds()
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
            flushDeferredBuilds()
            if (useInstancing && instancedBatcher?.isActive() == true) {
                instancedBatcher?.setTextureAlphaPredicate(::materialTextureHasAlpha)
                gpuTimer.begin(GPU_PASS_INSTANCED)
                instancedBatcher?.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
                gpuTimer.end(GPU_PASS_INSTANCED)
            }
            // Render remaining non-instanced geometry
            optimizedBatcher?.setTextureAlphaPredicate(::materialTextureHasAlpha)
            gpuTimer.begin(GPU_PASS_STREAM)
            optimizedBatcher?.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
            gpuTimer.end(GPU_PASS_STREAM)
            // Translucent pass LAST, after every opaque draw of both batchers, replayed
            // in scene order — legacy's alpha bucket — with deferred particle systems
            // spliced in at their sequence positions.
            drawTranslucentPass()
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
        resyncLegacyTextureBinding()

        // NOTE: no materialProvider.cycle() here — the base render() already cycles
        // materials once per presented frame, and this method can now run more than
        // once per frame (mid-frame scene flushes before UI drawing).

        frameCounter++
    }

    /**
     * Every modern flush binds textures with raw GL, so the legacy per-unit binding
     * cache (openglTextures[glTexturesCursorPos]) no longer describes what unit 0
     * holds. Legacy resource operations that follow — texture uploads, and the
     * minimap's copyTexSubImage screen captures — bind through setActiveTexture,
     * which is a NO-OP when the cache already names the target texture, so a stale
     * cache silently redirects the operation to whatever the modern batch left
     * bound (the minimap quadrant copies landed in an 8 px sprite texture). Park
     * unit 0 on no texture and record exactly that, so the next legacy bind is real.
     */
    private fun resyncLegacyTextureBinding() {
        val cache = openglTextures ?: return
        GLWrapper.activeTexture(GLWrapper.Constants.TEXTURE0)
        GLWrapper.bindTexture(GLWrapper.Constants.TEXTURE_2D, 0)
        if (glTexturesCursorPos in cache.indices) cache[glTexturesCursorPos] = null
        functionMask = functionMask and -2
    }

    /**
     * Replays the frame's translucent submissions in sequence order: instanced
     * items, streamed items and particle systems each drawn up to the next particle's
     * position, then the particle system, until everything is out.
     */
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
        // Particle systems in the same far-to-near (key desc, seq asc) order the batchers use.
        for (i in 0 until particleCount) deferredParticleOrder[i] = i
        if (particleCount > 1) {
            val keys = deferredParticleKey
            val seqs = deferredParticleSeq
            val order = (0 until particleCount).sortedWith(compareBy({ -keys[it].toLong() }, { seqs[it] }))
            for (i in 0 until particleCount) deferredParticleOrder[i] = order[i]
        }
        // Billboards in the same order, merged with the particle systems below.
        val bbN = bbCount
        for (i in 0 until bbN) bbOrder[i] = i
        if (bbN > 1) {
            val keys = bbKey; val seqs = bbSeq
            val order = (0 until bbN).sortedWith(compareBy({ -keys[it].toLong() }, { seqs[it] }))
            for (i in 0 until bbN) bbOrder[i] = order[i]
        }
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
            // Whichever comes first in (key desc, seq asc) is the next boundary.
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

    // ===== Retained static scene support =====

    /** Begins recording model submissions instead of drawing them; see [captureList]. */
    fun beginCapture(): ArrayList<RetainedStaticScene.Capture> {
        val list = ArrayList<RetainedStaticScene.Capture>(4)
        captureList = list
        captureUnsupported = false
        return list
    }

    /** Ends a capture; returns true if any submission could not be captured. */
    fun endCapture(): Boolean {
        captureList = null
        val unsupported = captureUnsupported
        captureUnsupported = false
        return unsupported
    }

    /** True while the deferred scene batch can accept retained submissions. */
    fun canSubmitRetained(): Boolean =
        canUseModernPipeline() && optimizedBatcher?.isActive() == true

    /**
     * Submits one retained static instance. [depthKey] is the legacy floorDepth of
     * the entity (only meaningful for translucent instances).
     */
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

    /** Next frame-wide translucent sequence number (retained translucent fast path). */
    fun nextTranslucentSeq(): Int = translucentSeqCounter++

    /** The instanced batcher when retained blocks may be appended to it this frame. */
    fun retainedBatcher(): InstancedModelBatcher? =
        if (instancingEnabled && useInstancing) instancedBatcher?.takeIf { it.isActive() } else null

    @JvmField var retainedBlockInstances = 0L
    @JvmField var retainedBlockBuilds = 0L
    @JvmField var retainedBlockFallbacks = 0L
    @JvmField var retainedTranslucentFast = 0L
    @JvmField var retainedBlockDraws = 0L
    @JvmField var retainedTransTpl = 0L

    /** Resolves per-draw material words for the retained scene's translucent templates. */
    fun fillMaterialWords(batcher: InstancedModelBatcher, mats: IntArray, count: Int, out: IntArray): Boolean =
        batcher.fillMaterialWords(mats, count, out, ::lookupTextureHandle, ::lookupCombineMode)

    /** Point-light selection for a world position, written to [out] (4 indices, -1 = none). */
    fun selectLightsInto(x: Float, y: Float, z: Float, out: FloatArray) {
        selectLightsFor(x, y, z)
        out[0] = currentLightIdx[0]; out[1] = currentLightIdx[1]
        out[2] = currentLightIdx[2]; out[3] = currentLightIdx[3]
    }

    /** [submitRetained] with a caller-memoised light selection; never touches the ModelGL. */
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

    /** Legacy floorDepth of a world position: the scene's separateFromFloor formula. */
    fun retainedDepth(x: Float, y: Float, z: Float): Int {
        calculateProjection(x, y, z, retainedProjectionScratch)
        return retainedProjectionScratch[2].toInt()
    }

    /** Conservative AABB-vs-frustum test against the live view-projection. */
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

    /**
     * Whether the world AABB lies ENTIRELY inside the view frustum: every plane has
     * the box's most-negative corner on its inner side. A chunk that passes needs no
     * per-instance frustum tests — every instance inside it is visible.
     */
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

    /** Per-frame count of retained instances submitted (diag). */
    @JvmField var retainedSubmitted = 0L
    @JvmField var retainedChunksCulled = 0L
    @JvmField var retainedInstancesCulled = 0L
    @JvmField var retainedCaptures = 0L

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
        resyncLegacyTextureBinding()

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
            spriteRenderer?.flush("setActiveTexture")
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

    /**
     * False: the modern scene pass lights models from [selectLightsFor] and the
     * frame's own light array, and the ground shader uploads its own, so nothing
     * here reads GL_LIGHT state.
     */
    override fun usesFixedFunctionLights(): Boolean = false

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
        if (n <= 0) {
            rememberLightSelection(wx, wy, wz)
            return
        }
        // Keep the 4 strongest contributors: attenuation is size^2/d^2, so rank on
        // d^2/size^2 (smaller is stronger) and ignore lights that cannot register on an
        // 8-bit channel at this distance.
        //
        // Candidates come from the per-frame XZ grid rather than the whole light list.
        // A light can only be selected when rank < 255, i.e. only inside a sphere of
        // radius sqrt(255)/sqrt(kq) around it, and the grid indexes exactly those
        // spheres — so this considers a strict superset of the selectable lights and
        // the result is identical to the exhaustive scan, at a fraction of the cost
        // (this used to be ~90 lights x ~2300 models per frame).
        var b0 = Float.MAX_VALUE; var b1 = Float.MAX_VALUE
        var b2 = Float.MAX_VALUE; var b3 = Float.MAX_VALUE
        var i0 = -1; var i1 = -1; var i2 = -1; var i3 = -1

        lightSelectCalls++
        // Consecutive submissions frequently share a world origin (an entity drawn
        // from several models, a loc with several submeshes), and the answer depends
        // only on that origin — so repeat it rather than rescanning the cell.
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
                // Outside the grid is outside every light's influence sphere by
                // construction (the bounds are expanded by each light's radius).
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
            // The cell's candidates are stored as contiguous (x, y, z, kq) quads with
            // a parallel index array, so this walks memory in order instead of
            // gathering each light out of the frame-wide array through an index.
            //
            // They are also ORDERED by the smallest rank the light can possibly reach
            // anywhere in this cell (see buildLightGrid), which makes the scan a
            // branch and bound: once that floor is no worse than the weakest light
            // already kept, nothing later in the list can displace it. Exact — it
            // only skips lights that provably cannot be selected — and it matters,
            // because near a lit town ~50 influence spheres overlap a single point
            // while at most four are ever used.
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
                // pos[+3] holds kq = 1/size^2, so kq*d^2 is the inverse of attenuation.
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

    // ===== Point-light lookup grid =====
    // Uniform XZ grid over the frame's lights, rebuilt once per frame by
    // [buildLightGrid]. Y is deliberately not bucketed: including it would only
    // narrow the candidate set, and scenes are far flatter in Y than in XZ.
    private var lightGridDim = 0
    private var lightGridMinX = 0f
    private var lightGridMinZ = 0f
    private var lightGridInvCell = 0f
    /** CSR offsets: cell c owns items [lightGridStart[c], lightGridStart[c+1]). */
    private var lightGridStart = IntArray(0)

    /** Light index per CSR slot. */
    private var lightCellIdx = IntArray(0)

    /** (x, y, z, kq) per CSR slot, so the scan reads one contiguous run per cell. */
    private var lightCellPos = FloatArray(0)

    /**
     * Smallest rank this light can reach anywhere inside its cell, i.e. kq times the
     * squared XZ distance from the light to the nearest point of the cell. A lower
     * bound on the rank any query in the cell can produce, which is what makes the
     * ordered scan in [selectLightsFor] able to stop early without changing its result.
     */
    private var lightCellRankFloor = FloatArray(0)

    /** Target cells per axis. 32x32 keeps the build cheap and cells small enough to matter. */
    private val LIGHT_GRID_DIM = 32

    private fun buildLightGrid() {
        val n = frameLightCount
        if (n <= 0) {
            if (lightGridDim != 0) lightEpoch++
            lightGridDim = 0
            lightGridSignature = 0L
            return
        }

        // The grid depends only on light POSITIONS and radii, which change when the
        // scene changes — not on colour or the per-frame flicker alpha. Skip the
        // rebuild when nothing that shapes the grid has moved.
        var signature = n.toLong() * 1099511628211L
        for (i in 0 until n) {
            val base = i * 4
            signature = signature * 31 + java.lang.Float.floatToRawIntBits(frameLightPos[base])
            signature = signature * 31 + java.lang.Float.floatToRawIntBits(frameLightPos[base + 2])
            signature = signature * 31 + java.lang.Float.floatToRawIntBits(frameLightPos[base + 3])
        }
        if (signature == lightGridSignature && lightGridDim > 0) return
        lightGridSignature = signature
        // Selection depends only on the light positions and radii — the same inputs as
        // this signature — so the per-model memos stay valid across the frames where
        // only colour and flicker change, which is nearly all of them.
        lightEpoch++

        // sqrt(255) — a light contributes only where kq*d^2 < 255, i.e. d < radiusK/sqrt(kq).
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

        // Pass 1: count per cell (start[c+1] holds the count for cell c).
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

        // Pass 2: scatter, using a copy of the offsets as write cursors.
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

        // Order each cell's candidates by their rank floor so the scan can bound.
        // Insertion sort: cells hold tens of entries, and this runs only when the
        // light set actually changes, not per frame.
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
            // Rank by distance to the centre of the loaded map (the player is always
            // near it), NOT to the camera: the camera orbits while rotating, so the
            // camera-ranked cut-off reshuffled the 128-light set every frame, which
            // rebuilt the light grid and invalidated every model's light memo (17%
            // of the spin frame). The map centre only moves on a region load.
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
        x3: Float, y3: Float, b3u: Float, b3v: Float, m3u: Float, m3v: Float,
        baseLinearFilter: Boolean? = null
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
            x3, y3, b3u, b3v, m3u, m3v,
            baseLinearFilter
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

    private fun hasPendingSceneGeometry(): Boolean = when {
        useOptimizedBatcher ->
            dbCount > 0 ||
                optimizedBatcher?.hasPendingGeometry() == true ||
                (useInstancing && instancedBatcher?.hasPendingGeometry() == true)
        else -> frameBatcher?.hasPendingGeometry() == true
    }

    /**
     * Arms the open scene batch with the toolkit's CURRENT sun and fog.
     *
     * -Drender.nolight=true collapses the sun term to identity (litRGB == vertex
     * colour). Legacy enables GL_LINEAR fog for every model draw, so the batch
     * carries the fog too or the scene reads ~25% brighter.
     */
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

    /**
     * The sun or fog changed while a scene batch is open. Legacy applied both at
     * draw time, so geometry already queued keeps the state it was queued under
     * (flush, then reopen) and an empty batch simply takes the new state. Without
     * this, sun changes made by UI drawing after the scene (item icon rendering
     * uses a random-intensity sun with fog off) reached the next frame's world
     * models, because the batch captured its lighting when it opened, before the
     * client re-applied the scene environment for the frame.
     */
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
     * unavailable — e.g. everything recreated during a runtime `toolkit 3` switch)
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
        if (!hasPendingSceneGeometry()) return
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
            // An offscreen display is about to bind its own FBO; the scene target must
            // not stay bound (or be resolved over it later).
            resolveSceneMsaa()
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
        // Popping back to the window re-bound framebuffer 0; the bloom capture must
        // keep receiving the rest of the frame.
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
        // UNCONDITIONALLY modern under toolkit 3. Construction only builds the glyph
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
    // Sprite creation is UNCONDITIONALLY modern under toolkit 3. The ModernSpriteGL
    // constructor is byte-for-byte the SpriteGL constructor (legacy texture upload —
    // a resource operation, not FFP drawing), so it is safe on any thread/state where
    // legacy sprite creation was safe. Modern-vs-skip is decided at DRAW time.
    override fun createNativeSprite(pixels: IntArray, stride: Int, srcWidth: Int, width: Int, height: Int, hasAlpha: Boolean): NativeSprite {
        return ModernSpriteGL(this, width, height, pixels, stride, srcWidth)
    }

    override fun createNativeSprite(x: Int, y: Int, width: Int, height: Int, hasAlpha: Boolean): NativeSprite {
        // SCREEN CAPTURE constructor: SpriteGL(toolkit, x, y, w, h) captures the
        // framebuffer region via OpenGl2DImageTexture.createFromScreenCapture
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

    override fun generateEmptySprite(width: Int, height: Int, hasAlpha: Boolean, mipmapped: Boolean): NativeSprite {
        return ModernSpriteGL(this, width, height, hasAlpha)
    }

    private val worldmapTileMasks = IdentityHashMap<ByteArray, ModernSpriteGL>()

    /**
     * Legacy draws each world map tile as an alpha-mask texture through
     * GL_INTERPOLATE (overlay colour where the mask is set, underlay colour
     * elsewhere) in immediate mode. That fixed-function path cannot run inside
     * the modern pipeline, so the tile becomes an underlay fill plus the mask
     * drawn as a white sprite tinted with the overlay colour, both in alpha blend
     * mode so a zoomed-out map does not flush the batch on every tile.
     */
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


    // Clip changes no longer flush the sprite batch: each queued quad carries the
    // scissor box current at queue time (ModernSpriteRenderer.setClipRect) and the
    // fragment shader applies it. The two legacy scissor primitives are the single
    // place every clip path (setClipping, resetClipping, expandClipping) goes through,
    // so mirroring them keeps the renderer's rect exact.
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

    /** Same box as OpenGlToolkit.defineScissorBox computes for glScissor. */
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
        // The batchers drive glDepthMask directly for their translucent segments, so
        // the legacy toolkit's cached mask can read "on" while GL has it off; a depth
        // clear issued in that state is a silent no-op (the chat head then fails the
        // depth test against the scene behind the dialogue). Assert the real mask.
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
            // clearRectangle draws the component surface texture across the clipping region.
            // SpriteSurfaceGL holds an OpenGl2DImageTexture whose activeHandle is the GL texture ID.
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
            // the emitting entity's models. Under the deferred batchers the models are
            // only QUEUED, so the particles are queued too, with a sequence number
            // that puts them at this position of the translucent pass. Particle state
            // is stable for the rest of the frame (ParticleSystemManager cycles before
            // the walk), so holding the wrapper is safe. Without an open scene batch
            // (a particle system outside the walk) draw immediately, as before.
            if (isSceneBatchActive()) {
                if (deferredParticles.size == deferredParticleSeq.size) {
                    deferredParticleSeq = deferredParticleSeq.copyOf(deferredParticleSeq.size * 2)
                    deferredParticleKey = deferredParticleKey.copyOf(deferredParticleKey.size * 2)
                    deferredParticleOrder = deferredParticleOrder.copyOf(deferredParticleOrder.size * 2)
                }
                deferredParticleSeq[deferredParticles.size] = translucentSeqCounter++
                // Sort key: the emitter's legacy depth, so the system lands where its
                // entity sits in the far-to-near alpha order (Int.MIN_VALUE = nearest,
                // i.e. last, for systems drawn outside the walk).
                deferredParticleKey[deferredParticles.size] = Scene.currentDrawEntity?.floorDepth ?: Int.MIN_VALUE
                deferredParticles.add(particleWrapper)
                return
            }
            endSpriteBatch()
            renderParticlesNow(pr, particleWrapper)
        } else {
            // No legacy fallback under the modern toolkit
            countLegacySkip("renderParticles")
        }
    }

    private var particleVpDiagBudget = 3
    // The scene's matrices as the batch saw them. Deferred particle systems and
    // billboards draw at the end of the frame, when the tracked matrices may already
    // belong to something else (picking/interface projections): with the live
    // tracked VP the cape sparkles rendered ~70 units below their emitters.
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

            // BILLBOARD BASIS — legacy parity: GlParticleRenderer builds the billboard
            // right/up vectors and per-particle depths from toolkit.viewMatrix44 (the
            // CAMERA view matrix; GlParticleRenderer.kt:48-51 and :163-168 — the
            // tracked-matrix replacement for the old glGetFloatv(GL_MODELVIEW)).
            // NOTE: currentModelMatrix is the PROJECTION matrix on this toolkit
            // (setModelMatrix stores Camera.projectionMatrix, OpenGlToolkit.kt:2600) —
            // passing it here previously produced degenerate billboard vectors and
            // no visible particles.
            val viewMatrixFloats = if (sceneSnapshotValid) sceneViewSnapshot else this.viewMatrix44.elements

            // Projection for the clip transform (renderer composes proj × view; the
            // Y-flip for GL clip space is applied inside the renderer).
            val projectionMatrixFloats = if (sceneSnapshotValid) sceneProjSnapshot else this.currentModelMatrix.elements

            // Fog state from the toolkit
            val hasFog = fogEnabled && fogDensity > 0
            val fR = if (hasFog) (fogColor and 0xff0000) / 16711680f else 0f
            val fG = if (hasFog) (fogColor and 0xff00) / 65280f else 0f
            val fB = if (hasFog) (fogColor and 0xff) / 255f else 0f

            // The scene's own VP (what every model and ground draw of this frame used):
            // composing currentModelMatrix x viewMatrix44 here placed particles below
            // their emitters (the cape sparkles sat under the player's feet).
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
                vp
            )

            // The particle pass changed raw GL state (shader, blend, depth mask)
            // behind the legacy caches.
            functionMask = 0
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

    private fun lookupTextureScroll(materialId: Int): Int {
        if (materialId < 0) return TextureScroll.NONE
        val material = materialProvider?.getMaterial(materialId) ?: return TextureScroll.NONE
        return TextureScroll.pack(material)
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

    /**
     * Whether a material's fragment alpha should come from its TEXTURE.
     *
     * Legacy decides this per material effect. Effects 1 (CubeMapSpecularEffect) and 7
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
        val material = materialProvider?.getMaterial(materialId) ?: return false
        return MaterialTypeList.isOpaqueEffect(material.effectId.toInt())
    }

    private fun lookupTextureHandle(materialId: Int): Int {
        // Only -1 is untextured. Material id 0 is a VALID textured material (old-format
        // meshes start texture ids at 0); legacy tests `< 0` (OpenGlToolkit.kt:1895),
        // not `<= 0`. Treating 0 as untextured dumped material-0 faces into the flat
        // vertex-color path — the green multimodel-loc quads.
        if (materialId < 0) return 0

        // Check cache first
        textureHandleCache[materialId]?.let {
            if (materialId >= textureTouchFrame.size) textureTouchFrame = textureTouchFrame.copyOf(maxOf(materialId + 1, textureTouchFrame.size * 2, 256))
            if (textureTouchFrame[materialId] != frameCounter + 1) {
                textureTouchFrame[materialId] = frameCounter + 1
                textureCache?.textureCache?.get(materialId.toLong())
            }
            return it
        }

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

    override fun createModel(mesh: Mesh, modelFunctionMask: Int, preferenceMask: Int, ambient: Int, contrast: Int): Model {
        // Build BEFORE super.createModel. The ModelGL constructor consumes the
        // mesh's billboard list, so by the time it returns, mesh.billboards is empty
        // and ModernModelBuilder can no longer tell which faces are billboard
        // ANCHORS. Legacy hides those (they are never rasterized — renderedFaceCount
        // excludes them); emitting them anyway draws the billboard's tint as a flat
        // untextured quad, which is the "green quads sticking out" symptom.
        // GeometryDiff proved it: every remaining divergence was extraInMesh=N with
        // bb[seen=0], i.e. the builder saw no billboards at all while legacy hid N.
        val built = try {
            ModernModelBuilder.build(mesh, ambient, materialProvider, (preferenceMask and 0x40) != 0, contrast)
        } catch (e: Exception) {
            null  // Never let a build failure break model creation
        }

        val model = super.createModel(mesh, modelFunctionMask, preferenceMask, ambient, contrast)
        // Build modern geometry from the CLEAN decoded mesh (direct vertex
        // indices, proper texture-projection UVs) BEFORE any post-create baking
        // (loc yaw, mirror, contour, recolor...). The mesh here is unrotated
        // base geometry; ModelGL tracks later mutations (modernYaw accumulates
        // representable yaw; anything else nulls modernBuilt → legacy-capture
        // fallback). `ambient` is the model ambient baked into the vertex colors.
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

    /** Per-presented-frame phase accumulators (reset in render(), read by [ModernPerf]). */
    private var phaseModelNs = 0L
    private var phaseModelCalls = 0
    private var phaseGroundNs = 0L
    private var phaseGroundCalls = 0
    private var phaseLightSelectNs = 0L
    private var phaseGpuWaitNs = 0L

    /**
     * Per-pass GPU cost (`-Drender.gputimers`). CPU timers cannot see it: the driver
     * returns from a draw long before the work runs.
     */
    private val gpuTimer = GpuTimer(arrayOf("inst", "stream", "ground", "sprite"))
    private val GPU_PASS_INSTANCED = 0
    private val GPU_PASS_STREAM = 1
    private val GPU_PASS_GROUND = 2
    private val GPU_PASS_SPRITE = 3

    /** -Drender.gpusync: glFinish before present, to separate GPU-bound from CPU-bound frames. */
    private val GPU_SYNC = System.getProperty("render.gpusync") != null

    /**
     * -Drender.phasetimers: time each model draw and each of its stages individually.
     *
     * Off by default and deliberately so: the per-stage breakdown needs up to ten
     * System.nanoTime calls per model draw, which at this model count is a few percent
     * of the frame spent in clock_gettime — enough to distort what it is measuring.
     */
    private val PHASE_TIMERS = System.getProperty("render.phasetimers") != null

    /** [System.nanoTime] when the per-stage timers are enabled, 0 otherwise. */
    private fun stageNow(): Long = if (PHASE_TIMERS) System.nanoTime() else 0L

    /**
     * Per-build memo for [buildModelCache]'s normalized vertex normals.
     *
     * Faces share normal slots (2-6 faces per slot is typical), and the conversion
     * costs a sqrt and three divisions, so the un-memoised version paid for the
     * same normal several times per model. [normalCacheStamp] avoids having to
     * clear the memo between builds.
     */

    /** Scratch for buildModelCache's material grouping (distinct ids, counts, write cursors). */

    /**
     * Stand-in for [ModelCache.CachedModel.vertexData] on model-owned entries, whose
     * geometry lives in the word array instead. Never read.
     */
    private val EMPTY_VERTEX_DATA: java.nio.ByteBuffer =
        java.nio.ByteBuffer.allocate(0).order(java.nio.ByteOrder.nativeOrder())
    private var matGroupCursor = IntArray(64)

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

    /**
     * Memoised wrapper around [computeModelContentHashUncached].
     *
     * The uncached hash is O(vertices + faces) and used to run for EVERY model on
     * the legacy-capture path EVERY render frame — ~12% of a logged-in frame. Its
     * inputs only change when game logic mutates the model (50Hz at most), and
     * every ModelGL method that writes a geometry array clears
     * [ModelGL.modernContentValid], so a render frame with no intervening mutation
     * reuses the stored value.
     *
     * `-Drender.verifyhash` recomputes unconditionally and reports disagreement,
     * which is the direct test that the invalidation set is complete.
     */
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

        // Whole-path timer. Everything the scene walk spends submitting models lands
        // here, so `tilesMs - modelMs - groundMs` is the legacy traversal's own cost —
        // the split that the per-stage timers below could never give, because most of
        // the work is not inside any of them.
        //
        // Opt-in: two System.nanoTime calls per model draw is ~2% of the frame at this
        // model count, which is enough to distort the very thing it measures.
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

        // OPTIMIZATION: Frustum culling at model level (not while capturing for the
        // retained store — the capture must see everything, the store culls per frame)
        val cullStartNs = stageNow()
        captureLegacyMatrix = modelMatrix
        if (captureList == null && frustumCullEnabled && model.boundsCalculated) {
            val vp = viewProjectionMatrix.elements
            if (!isModelVisible(model, modelMatrix, vp)) {
                if (VERIFY_MODEL_CULL) verifyModelCull(model, modelMatrix, vp)
                modelsCulled++
                if (PHASE_TIMERS) timeFrustumCull += System.nanoTime() - cullStartNs
                return
            }
        }
        if (PHASE_TIMERS) timeFrustumCull += System.nanoTime() - cullStartNs
        // Billboards ride with the model (retained records queue theirs at resubmission).
        if (captureList == null && model.billboardCount > 0 && isSceneBatchActive()) {
            deferBillboards(model, modelMatrix, Scene.currentDrawEntity?.floorDepth ?: 0)
        }

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

        val cacheStartNs = stageNow()
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
                val cachedModel = sharedGeometryFor(model, cache, geometryKey) {
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

            // LEGACY-CAPTURE FALLBACK: capture the model's post-connectNormal
            // arrays, keyed by content hash (full-vertex rolling hash — only
            // meaningful for static models; computing it for animated models
            // was pure per-frame waste).
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

            // ANIMATED / POSE-SNAPSHOT PATH.
            //
            // These models are excluded from the shared content cache on purpose:
            // every animation tick produces different vertices, so keying them by
            // content would flood the persistent instanced store with single-use
            // meshes (see the isAnimated comment above). But that exclusion used to
            // mean the FULL per-face build ran again on EVERY RENDER FRAME — ~22% of
            // a logged-in frame — even though the vertices only change when game
            // logic animates the model, at 50Hz. With the render loop uncapped that
            // is up to six identical rebuilds per pose.
            //
            // So keep the geometry on the model itself, keyed by the same
            // invalidation stamp the content hash uses, and rebuild it only when a
            // mutation actually invalidated it. The buffer is reused in place, so a
            // steady-state pose change allocates nothing.
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
                // Preferred: keep the pose in the PERSISTENT instanced store, keyed by
                // instance, and re-send it only when the pose actually changed. The
                // streaming alternative re-copied every animated vertex into a staging
                // buffer and re-stamped a model index into every one of them, every
                // render frame — together the largest single cost left in the frame.
                // -Drender.animinstanced=false restores that path.
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

        // Fallback: process faces directly (no cache or cache build failed)
        if (captureList != null) {
            captureUnsupported = true
            return
        }
        // Register model matrix with the optimized batcher
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

    /**
     * Routes a cached model through the instanced batcher (preferred) or the
     * regular optimized batcher. Shared by the mesh-built and legacy-capture
     * cache paths.
     *
     * @param geometryKey stable key identifying the geometry: the instanced
     *   batcher groups instances by it, so all instances sharing it must carry
     *   identical vertex data (per-instance placement lives in [modelMatrix]).
     */
    /**
     * Fills [currentLightIdx] for [model] at [modelMatrix]'s origin, reusing the model's own
     * memo when neither its position nor the frame's light set has changed.
     */
    private fun selectLightsForModel(model: ModelGL, modelMatrix: Matrix43) {
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

    /** Bumped whenever the frame's light set is rebuilt, invalidating every per-model memo. */
    private var lightEpoch = 0

    private fun submitCachedModel(
        batcher: OptimizedFrameBatcher,
        cachedModel: ModelCache.CachedModel,
        geometryKey: Long,
        modelMatrix: Matrix43,
        model: ModelGL
    ) {
        // Pick this model's lights from its own world origin (see selectLightsFor).
        selectLightsForModel(model, modelMatrix)
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
        // Legacy's only translucency mechanism is ORDER: the scene draws its alpha
        // bucket after everything opaque, back to front. Deferred batchers must keep
        // those draws in a separate ordered pass (see InstancedModelBatcher
        // .drawTranslucent), so each submission carries the walk's phase and the
        // legacy depth-write state it would have been drawn with.
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

    /**
     * Submits resolved geometry to the instanced store, falling back to the streaming
     * batcher when the store cannot take it. Shared by the live scene walk and the
     * retained static store.
     */
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
            // If instanced batcher is full, fall through to regular batcher
        }

        // REGULAR BATCHER PATH: used when instancing is disabled or full
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

        // addCachedModel returns 0 when its vertex buffer is full and DISCARDS the
        // model — unlike the beginModel overflow above, which already flushes and
        // retries. Silently losing a model here is what made walls vanish and left
        // holes in lamp posts once instanced-store overflow started routing large
        // numbers of models down this path. Flush the batch to reclaim the buffer
        // and retry once, mirroring the beginModel path.
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

    /**
     * Builds the modern vertex/index representation of [model]'s CURRENT legacy arrays.
     *
     * @param modelOwned build for the animated/pose path, which keeps the result on
     *   the ModelGL itself and rebuilds it once per logic tick. Those buffers are
     *   allocated on the heap so the GC owns them — a pose model can be dropped at
     *   any tick with nobody left to free a native allocation.
     * @param reuse when non-null, the result is written back into this instance and
     *   its vertex buffer is reused if it is large enough, so the steady state
     *   allocates nothing at all.
     */
    /**
     * Everything buildModelCache scribbles in between builds. One instance per
     * thread: pose geometry is now built on worker threads at flush time (see
     * [flushDeferredBuilds]), and the render thread keeps its own for the rest.
     */
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

        // Normal data for GPU lighting. Legacy lights from the MERGED normals when a
        // neighbouring loc shaded this model (ModelGL.applyShading(model, ...) builds
        // them; upload() prefers them) — so must the capture, or shared wall edges
        // light differently from legacy.
        val merged = model.mergedNormals
        val normalsX = merged?.normalsX ?: model.normalsX
        val normalsY = merged?.normalsY ?: model.normalsY
        val normalsZ = merged?.normalsZ ?: model.normalsZ
        val magnitudes = merged?.magnitude ?: model.magnitudes
        val hasNormals = normalsX != null && normalsY != null && normalsZ != null && magnitudes != null

        // Build normal-to-vertex mapping if needed
        val useDirectVertexIndices = (triangles == null || normalMapping == null)
        val normalToVertex: IntArray?
        if (!useDirectVertexIndices && triangles != null && normalMapping != null) {
            // Scratch, not a fresh IntArray: this runs once per pose change of every
            // animated model, and the allocation plus zero-fill showed in the profile.
            if (sc.normalToVertexScratch.size < numNormals) sc.normalToVertexScratch = IntArray(numNormals * 2)
            normalToVertex = sc.normalToVertexScratch
            java.util.Arrays.fill(normalToVertex, 0, numNormals, -1)
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


        // Resolve every face's vertex indices ONCE (the main loop used to redo the
        // same lookups), remembering them per face; -1 marks a face to drop.
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

        // A face failing the check above is SILENTLY DISCARDED — geometry vanishes
        // with no signal anywhere. That is the documented failure mode of this
        // legacy-capture path ("dropped whole submeshes on multi-model locs", see
        // ModernModelBuilder's header), and the suspected cause of the missing
        // Canifis walls and the signpost's missing post. Count it so the next run
        // says whether faces are actually being dropped, and by how much.
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

        // Allocate buffers - NEW 40-byte format. Worst case is three vertices per
        // face (the per-face path); the shared-slot path below emits far fewer and
        // reports the real count afterwards.
        val maxVertexCount = validFaceCount * 3
        val neededBytes = maxVertexCount * ModelCache.BYTES_PER_VERTEX  // 40 bytes per vertex
        // Heap buffers for the reuse path (see CachedModel.heapOwned): the GC owns
        // them, because a pose model can be dropped at any tick with no one to free
        // a native allocation. nativeOrder is mandatory — the bytes are memcpy'd
        // straight into the GL staging buffer, so the float encoding must match.
        val vertexData = if (modelOwned) {
            // Model-owned geometry lives in its word array; this placeholder only
            // exists because CachedModel.vertexData is non-null, and is never read
            // for such an entry.
            reuse?.vertexData ?: EMPTY_VERTEX_DATA
        } else {
            MemoryUtil.memAlloc(neededBytes)
        }
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

        // Normalized normals, memoised per normal slot for the duration of this build.
        // Faces share normal slots, so the un-memoised version repeated the same
        // sqrt + three divisions several times per model. The stamp array means no
        // clearing pass between builds.
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
        // Legacy's GL upload (ModelGL.upload, the normalsBuffer fill) scales a slot
        // normal by 3/(contrast*magnitude) when it has a magnitude — the stored vector
        // is the SUM of unit(256) face normals and magnitude their count, so this is
        // the plain average, shorter wherever the faces disagree — and by 2/contrast
        // (= 3/(1.5*contrast)) when the magnitude is 0 (a flat-shaded face normal of
        // length 256, i.e. two thirds of the smooth strength). Nothing is ever
        // renormalised; the CPU lighting path uses the identical factors. Normalising
        // here, as this used to, lit creases and every flat wall too brightly.
        val contrastForNormals = model.contrast.toInt().let { if (it == 0) 768 else it }
        val flatNormalScale = 2f / contrastForNormals
        val smoothNormalScale = 3f / contrastForNormals
        val colorWordsAreBigEndian = java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.BIG_ENDIAN

        // Vertices are assembled as raw 32-bit words in a scratch array and copied
        // into the buffer in ONE bulk transfer at the end. Emitting them with ~30
        // ByteBuffer puts per vertex — each with its own bounds and byte-order
        // handling — was the single most expensive thing in this build.
        val words = maxVertexCount * (ModelCache.BYTES_PER_VERTEX / 4)
        // Model-owned geometry keeps the WORDS as its authoritative copy (see
        // CachedModel.vertexWords), so the assembly below writes straight into it
        // and no ByteBuffer is involved on this path at all. Cache-owned geometry
        // still goes through the shared scratch and a native buffer.
        val vw: IntArray
        if (modelOwned) {
            val existing = reuse?.vertexWords
            vw = if (existing != null && existing.size >= words) existing else IntArray(words)
        } else {
            if (sc.vertexWordScratch.size < words) sc.vertexWordScratch = IntArray(words * 2)
            vw = sc.vertexWordScratch
        }
        var wp = 0

        /** Writes the 3 normal words for [normalIdx] at [out], memoised per normal slot. */
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
                // Default up normal (carrying the per-model contrast scale)
                vw[out] = 0
                vw[out + 1] = contrastScaleBits
                vw[out + 2] = 0
            }
        }

        // Legacy colours are PER VERTEX SLOT, not per face: ModelGL.upload writes
        // hslToRgb(face) into the slots named by facesA/B/C for every face in order
        // (ModelGL.kt:2050-2058), so on shared vertices the LAST face wins for both
        // RGB and alpha. Emitting per-face colour instead let translucent faces keep
        // alpha ~127 on vertices legacy had already overwritten opaque -- the
        // see-through Canifis lampposts.
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
        // Per-face colour, kept for the main loop's fallback so hslToRgb (palette +
        // material lookups) runs once per face instead of twice.
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

        // Vertices are emitted PER SLOT (legacy's own layout: ModelGL.upload writes one
        // vertex per normal slot and draws indexed triangles), so a slot shared by
        // several faces is written once. The per-face layout this replaces wrote three
        // vertices per face, ~2.5x the data on every pose rebuild and upload. Same
        // position/normal/uv/colour per slot as before, so the pixels are unchanged.
        // Face -> (a, b, c) output indices are kept in sc.faceIndexScratch for the runs.
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

        /** Output index of [slot] (positioned at legacy vertex [vert]), emitting it on first use. */
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
            vw[o + 9] = 0  // modelIndex placeholder
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

            // Material ID
            val materialId = facesMaterial?.get(faceIdx)?.toInt() ?: -1
            materialIds[outFaceIdx] = materialId.toShort()

            // Base colour: per vertex slot (legacy last-face-wins), not per face.
            // A slot the pre-pass never touched (model.numFaces not covering this
            // face) would read 0 = alpha 0 = invisible; fall back to the face colour.
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
            // One bulk transfer instead of ~90 puts per face.
            vertexData.clear()
            vertexData.asIntBuffer().put(vw, 0, wp)
            vertexData.position(0)
            vertexData.limit(wp * 4)
        }

        // Material RUNS in face order — legacy's materialBatchOffsets. The faces
        // are already in legacy's sorted order (Model.addBillboard: layer, alpha,
        // effect, material), so a run boundary falls exactly where legacy's does and
        // the run count is ~the material count. Runs, not per-material groups,
        // because the translucent pass draws a model's runs IN ORDER: legacy puts a
        // model's alpha faces after its opaque ones, and merging by material would
        // undo that.
        //
        // Allocation-free apart from the final per-run index arrays (this reruns on
        // every pose change on the animated path).
        var runCount = 0
        run {
            var ids = sc.matGroupIds
            var counts = sc.matGroupCount
            var previous = Int.MIN_VALUE
            for (faceIdx in 0 until validFaceCount) {
                val rawId = materialIds[faceIdx].toInt()
                // SIGNED: untextured is -1; 0..N are textured INCLUDING material 0.
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
        // A pose rebuild keeps its material runs (same faces, same order); reuse the
        // previous batch objects and index arrays instead of allocating per pose.
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

    /**
     * Returns [model]'s own geometry for the CURRENT pose, rebuilding it only if a
     * mutation invalidated it since the last build.
     *
     * Unlike the shared [modelCache], this is keyed by instance identity rather than
     * by content, which is exactly right for pose snapshots: no hashing, no map, and
     * no chance of one pose's geometry being served for another's.
     */
    private fun animatedGeometryFor(model: ModelGL): ModelCache.CachedModel? {
        val existing = model.modernStreamGeometry as? ModelCache.CachedModel
        if (existing != null && model.modernStreamValid) {
            streamGeometryHits++
            animatedGeometryRebuilt = false
            return existing
        }
        streamGeometryBuilds++
        run {
            // Class-keyed, in-place counters: the String/boxing version of this showed
            // up in the allocation profile (it runs per geometry rebuild).
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

    // ---- Deferred, parallel pose geometry builds ---------------------------------
    // A pose change costs a full geometry rebuild (~50 us). Camera rotation brings
    // dozens of animated locs into view per frame, each needing one, and locs of a
    // type step their frames on the same cycle — 50-80 builds in one frame was the
    // choppiness. Private pose copies are not written by anyone between the walk and
    // the flush, so their builds are queued here and run on the common pool at flush
    // time; submission order (and the translucent sequence numbers) is preserved.
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
        // The same model queued twice in a frame is built once.
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

    /** Builds every queued pose in parallel, then submits them in queue order. */
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
                    // A worker must never take the frame down; the entry is dropped and counted.
                    if (deferredBuildFailures++ < 5) { System.err.println("[DeferredBuild] failed: $t"); t.printStackTrace() }
                    null
                }
            }
        }
        // Fork-join overhead is not worth it for a handful of builds.
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
                submitStreamedModel(batcher, b, dbMatrix[i])
            }
        }
        deferredBuildMsAcc += (System.nanoTime() - t0) / 1e6
        deferredBuildNsAcc += (((System.nanoTime() - t0) / 1e6) * 1e6).toLong()
        clearDeferredBuilds()
    }

    /** Whether the last [animatedGeometryFor] actually rebuilt, i.e. the resident copy is stale. */
    private var animatedGeometryRebuilt = false

    /**
     * Resolves [model]'s entry in the shared content cache, remembering the result on
     * the model so an unchanged model skips the map lookup entirely.
     *
     * The cache is an access-ordered LinkedHashMap keyed by a boxed Long: every get
     * boxes the key, walks a bucket comparing Longs, and then RESTRUCTURES the list to
     * record the access. At ~1700 model draws per frame that showed up plainly in the
     * profile. The memo is keyed by the same invalidation stamp as the content hash,
     * plus the entry's own eviction flag so a freed buffer can never be handed back.
     */
    private inline fun sharedGeometryFor(
        model: ModelGL,
        cache: ModelCache,
        key: Long,
        noinline build: () -> ModelCache.CachedModel?
    ): ModelCache.CachedModel? {
        if (model.modernSharedValid && model.modernSharedKey == key) {
            val memo = model.modernSharedGeometry as? ModelCache.CachedModel
            if (memo != null && !memo.evicted) return memo
        }
        val resolved = cache.getOrCreate(model, key, build) ?: return null
        model.modernSharedGeometry = resolved
        model.modernSharedKey = key
        model.modernSharedValid = true
        return resolved
    }

    /**
     * Streams a single-use cached model through the regular (non-instanced) batcher.
     *
     * Pose geometry is unique per instance, so it must never enter the persistent
     * instanced store — that is what [submitCachedModel] is for.
     */
    private fun submitStreamedModel(
        batcher: OptimizedFrameBatcher,
        cached: ModelCache.CachedModel,
        modelMatrix: Matrix43
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
        val translucent = Scene.alphaPassActive
        val depthWrite = depthBufferingEnabled
        val seq = if (translucent) translucentSeqCounter++ else 0
        val depthKey = if (translucent) (Scene.currentDrawEntity?.floorDepth ?: 0) else 0
        var facesAdded = batcher.addCachedModel(cached, modelIndex, translucent, depthWrite, seq, depthKey)
        // addCachedModel returns 0 and DISCARDS the model when its vertex buffer is
        // full; flush and retry once, exactly as submitCachedModel does.
        if (facesAdded == 0 && cached.faceCount > 0) {
            flushBatchMidFrame(batcher)
            modelIndex = batcher.beginModel(modelMatrix, currentLightIdx)
            if (modelIndex >= 0) facesAdded = batcher.addCachedModel(cached, modelIndex, translucent, depthWrite, seq, depthKey)
            if (facesAdded == 0) streamingModelsDropped++
        }
        totalFacesProcessed += facesAdded
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
        // Iterate model.numFaces, matching legacy's loop bound -- faces the render
        // pass skips still contribute their colour to shared slots.
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

            // Per-vertex-slot colour (legacy last-face-wins), not per-face.
            // A slot the pre-pass never touched (model.numFaces not covering this
            // face) would read 0 = alpha 0 = invisible; fall back to the face colour.
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

            // Compute normalized vertex normals for GPU lighting.
            // Allocation-free: the previous `for ((v, idx) in listOf(...))` allocated a
            // List + 3 Pairs PER FACE (tens of thousands of allocations per frame in
            // animated-heavy scenes like the swamp).
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

    /**
     * Writes a legacy-format vertex normal into [out] at [off] with legacy's GL
     * scaling (see buildModelCache): n * 3/(contrast*magnitude), or n * 2/contrast
     * for a magnitude-0 flat normal. Never renormalised.
     */
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
        if (Scene.retainedHook === retainedScene) Scene.retainedHook = null
        Culling.disabledForModern = false
        retainedScene = null
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
        // Render current batch. A mid-frame flush cannot wait for the rest of the
        // scene, so its translucent stream goes out now, right after its own opaque
        // geometry — the closest to legacy order a capacity overflow allows.
        batcher.endFrame(::lookupTextureHandle, ::lookupCombineMode, ::materialNeedsBlend, alphaDiscardEnabled)
        batcher.drawTranslucent(::lookupTextureHandle, ::lookupCombineMode)

        // Start new batch
        batcher.beginFrame()

        batcher.setViewProjection(viewProjectionMatrix.elements)
        armBatchLightingAndFog()
    }

    /**
     * Checks if a model's bounding box is potentially visible in the view frustum.
     * Uses the model's AABB transformed by the model matrix against the VP matrix.
     *
     * This is a conservative check - returns true if any part of the AABB might be visible.
     */
    /**
     * -Drender.verifycullmodels: for every model the frustum test REJECTS, re-test its
     * eight transformed AABB corners exactly.
     *
     * The test must be conservative — it may keep things that are off screen, but it
     * must never drop something that is on it. A hit here means it is dropping visible
     * geometry, which reads on screen as scenery vanishing at the edges while the
     * camera turns. This is the check that would have caught the old sphere test.
     */
    private val VERIFY_MODEL_CULL = System.getProperty("render.verifycullmodels") != null
    private var modelCullChecks = 0L
    private var modelCullBad = 0L

    private fun verifyModelCull(model: ModelGL, modelMatrix: Matrix43, vp: FloatArray) {
        // A model with no drawn faces has degenerate bounds and paints nothing, so
        // whether it is culled is unobservable. Counting them only buries real hits.
        if (model.renderedFaceCount <= 0) return
        modelCullChecks++
        val minX = model.minX.toFloat(); val maxX = model.maxX.toFloat()
        val minY = model.minY.toFloat(); val maxY = model.maxY.toFloat()
        val minZ = model.minZ.toFloat(); val maxZ = model.maxZ.toFloat()
        // A rejection is only sound when EVERY corner is outside the SAME plane.
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

        // AABB against the six frustum planes, extracted straight from the VP matrix.
        //
        // This used to enclose the box in a sphere, scale its WORLD-space radius by a
        // single scalar taken from one row of VP, and compare that against clip-space
        // bounds. Those bounds are ±w, w varies with depth, and the projection's X and
        // Y scales are not equal — so the comparison was only ever a guess, and it
        // guessed badly for large objects near the edge of the screen, dropping them
        // while most of the object was still in view (a market tent vanishing as the
        // camera turned). The plane test below is exact for an AABB and conservative
        // by construction: it rejects only when the whole box is outside ONE plane,
        // so nothing visible can be culled.
        //
        // Planes come from the clip-coordinate expressions themselves; inside is
        // a*x + b*y + c*z + d >= 0. Recomputed per call rather than cached — it is a
        // couple of dozen adds, and a cache keyed on this matrix is exactly what went
        // stale here before.
        val vp0 = vp[0]; val vp1 = vp[1]; val vp2 = vp[2]; val vp3 = vp[3]
        val vp4 = vp[4]; val vp5 = vp[5]; val vp6 = vp[6]; val vp7 = vp[7]
        val vp8 = vp[8]; val vp9 = vp[9]; val vp10 = vp[10]; val vp11 = vp[11]
        val vp12 = vp[12]; val vp13 = vp[13]; val vp14 = vp[14]; val vp15 = vp[15]

        // left: clipX + clipW >= 0
        if (aabbOutsidePlane(vp0 + vp3, vp4 + vp7, vp8 + vp11, vp12 + vp15,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        // right: clipW - clipX >= 0
        if (aabbOutsidePlane(vp3 - vp0, vp7 - vp4, vp11 - vp8, vp15 - vp12,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        // bottom: clipY + clipW >= 0
        if (aabbOutsidePlane(vp1 + vp3, vp5 + vp7, vp9 + vp11, vp13 + vp15,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        // top: clipW - clipY >= 0
        if (aabbOutsidePlane(vp3 - vp1, vp7 - vp5, vp11 - vp9, vp15 - vp13,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        // near: clipZ >= 0. This projection's depth range is 0..w, not the GL -w..w —
        // the same convention the previous test used. Taking the GL one "to be safe"
        // stopped rejecting everything behind the camera and put thousands of extra
        // models a frame through submission.
        if (aabbOutsidePlane(vp2, vp6, vp10, vp14,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false
        // far: clipW - clipZ >= 0
        if (aabbOutsidePlane(vp3 - vp2, vp7 - vp6, vp11 - vp10, vp15 - vp14,
                worldCenterX, worldCenterY, worldCenterZ, worldExtentX, worldExtentY, worldExtentZ)) return false

        return true
    }

    /**
     * True when the axis-aligned box centred at ([cx], [cy], [cz]) with half-extents
     * ([ex], [ey], [ez]) lies entirely on the negative side of plane (a, b, c, d).
     *
     * Standard "n-vertex" form: the box's most negative point along the plane normal
     * sits at `dot(centre, n) - dot(extent, |n|)`, so the whole box is outside only
     * when even that point is below the plane.
     */
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

    /**
     * Renders ground material using the modern pipeline.
     * Called from GroundMaterialGL.drawMaterialTriangles when using ModernGLToolkit.
     *
     * This method renders textured ground tiles through the modern shader
     * by reusing the legacy VBOs that GroundGL has already populated.
     *
     * @param material The GroundMaterialGL containing color data
     * @param ground The GroundGL containing vertex/normal/texcoord buffers
     * @param tileIndices Array of tile indices to render
     * @param tileCount Number of tiles to render
     * @param tileFunctionMasks Per-tile function mask for face selection
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
     *         initialized (first frames after a runtime `toolkit 3` switch; init happens
     *         after the first render()), a non-owner thread (login screen renderer), a
     *         non-ingame game state, or a GroundGL built without the modern vertex
     *         snapshot (scene built while a different toolkit was active).
     */
    /**
     * Returns a GL buffer holding [ground]'s terrain snapshot, uploading it only when
     * the snapshot has actually been rebuilt.
     *
     * Terrain geometry is static between ground rebuilds, but the ground pass draws
     * once per material — so the previous unconditional glBufferData re-sent the whole
     * snapshot ~35 times a frame. Buffers are held in a small access-ordered map and
     * deleted on eviction, so a session that walks through many regions cannot
     * accumulate them.
     */
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
            // Touch for LRU ordering.
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

    /** Access-ordered so eviction drops the ground that has gone longest without a draw. */
    private val residentGroundVbos = LinkedHashMap<GroundGL, Int>(32, 0.75f, true)
    private val MAX_RESIDENT_GROUND_VBOS = 24

    /**
     * Modern minimap floor: the top-down slice of [ground] that legacy draws through
     * fixed-function GL in GroundGL.renderMinimapFloor. Same ortho mapping
     * (GROUND_MATRIX there, world X/Z to minimap pixels at 4 px per tile), same
     * lighting rule (sun lighting only with HD normals), drawn from the ground's
     * resident interleaved VBO by the modern ground shader.
     *
     * @return false if the modern path cannot draw (caller draws nothing).
     */
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
        // x_clip = sx * worldX + tx ; y_clip = sy * worldZ + ty (legacy: ortho projection
        // times a modelview that swaps Y and Z). Column-major for GLSL.
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
    /** Which 8x8 tile chunks contain at least one visible tile, cached per visible-set stamp. */
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
    /** Visibility bitmap over tile indices for the current visible-set stamp. */
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
        // Ground draws immediately, but its lighting and fog uniforms used to come from
        // the last deferred flush. An interface model (chat head, item icon) installs its
        // own sun colour and fog through prepareLights and its own projection, so a flush
        // during the UI left the terrain lit and fogged for the interface until the next
        // flush. Legacy reads the live fixed-function state at draw time; do the same.
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

        // Any 2D quads queued before the scene (skybox background fill, fog clears
        // routed through the sprite batch) must reach the framebuffer BEFORE the
        // terrain draws over them; otherwise the deferred fog-colored fill is
        // flushed later in the frame and paints over the already-drawn terrain.
        if (isSpriteBatchActive) endSpriteBatch()

        // The index list depends only on the visible tile set and this material's tile
        // masks, so a frame with the same visible set (the camera did not move) reuses
        // the list AND its resident element buffer: no per-tile loop, no upload.
        // (Legacy logic mirrored from GroundMaterialGL.drawMaterialTriangles.)
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
            val maxIndices = maxOf(tileCount, ground.materialIndices.size) * 18  // Max 6 triangles (18 indices) per tile
            if (groundIndexScratch.size < maxIndices) {
                groundIndexScratch = ShortArray(maxIndices)
            }
            indexArray = groundIndexScratch
            // Walk only this material's own tiles against a per-frame visibility bitmap
            // instead of every visible tile for every material (49 x 10k per frame
            // while the camera moved).
            // Per-material index lists are cached per 8x8 tile chunk (static until the
            // ground's vertex revision changes), each list tagged with the tile every
            // slice belongs to. A frame concatenates, from the chunks that contain a
            // visible tile, only the slices of tiles in the visible set: the visible
            // set is per tile because the roof test (Scene.drawTiles) excludes the
            // upper-plane floor over a hidden roof tile by tile, and a chunk-level
            // copy drew that floor as a flat slab over the world.
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
                // Copy maximal runs of consecutive visible tiles in one arraycopy each.
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
            if (indexCount > 0) {
                // Upload into the material's own element buffer so later frames bind it.
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
                    glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0L, staging)
                }
                residentIbo = material.modernIboId
            }
        }

        if (indexCount == 0) return true

        // Determine texture coordinate component count from textureBuffer.size
        // size is the number of components (2 for vec2, 3 for vec3)
        val texCoordSize = ground.textureBuffer?.size?.toInt() ?: 2

        // Get texture handle
        val textureHandle = lookupTextureHandle(materialId)
        val groundMaterial = if (materialId >= 0) materialProvider?.getMaterial(materialId) else null
        val textureTime = TextureScroll.seconds(textureAnimatingProgression)
        val textureScrollU = if (groundMaterial != null) TextureScroll.offset(textureTime, groundMaterial.textureSpeedU) else 0f
        val textureScrollV = if (groundMaterial != null) TextureScroll.offset(textureTime, groundMaterial.textureSpeedV) else 0f

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
        val combineMode = if (GROUND_COLOR_DEBUG) {
            99
        } else if (GROUND_COMBINE_OVERRIDE >= 0) {
            GROUND_COMBINE_OVERRIDE
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
            // Raw inputs of this draw: what the shader multiplies, byte for byte.
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

        // Render through modern ground renderer (pure LWJGL path)
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
            alphaFromVertex = materialId >= 0 && !MaterialTypeList.isOpaqueEffect(materialProvider?.getMaterial(materialId)?.effectId?.toInt() ?: 0),
            residentVertexVbo = residentGroundVbo(ground, modernData),
            residentIndexIbo = residentIbo
        )
        gpuTimer.end(GPU_PASS_GROUND)
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
        } finally {
            phaseGroundNs += System.nanoTime() - groundStartNs
            phaseGroundCalls++
        }
    }
}
