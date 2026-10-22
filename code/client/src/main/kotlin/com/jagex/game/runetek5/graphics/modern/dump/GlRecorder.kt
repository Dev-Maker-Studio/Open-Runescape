package com.jagex.game.runetek5.graphics.modern.dump

import com.jagex.game.runetek5.graphics.modern.util.GLWrapper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER_BINDING
import org.lwjgl.opengl.GL40.GL_PATCHES
import org.lwjgl.opengl.GL40.GL_PATCH_VERTICES
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER_BINDING
import org.lwjgl.system.MemoryStack

internal object CaptureTopology {
    fun supported(name: String, mode: Int, patchVertices: Int): Boolean = mode == GL_TRIANGLES || mode == GL_PATCHES && name == "hdWater" && patchVertices == 3

    fun viewportBounded(pass: Pass?, mode: Int, polygonMode: Int): Boolean = pass != null && (mode == GL_TRIANGLES || mode == GL_PATCHES && pass === Passes.hdWater) && polygonMode == GL_FILL

    fun spriteClipAttribute(type: Int, size: Int, divisor: Int, integer: Boolean): Boolean = type == GL_FLOAT && size == 4 && divisor == 0 && !integer

    fun indirectBytes(commands: Int, type: Int, stride: Int): Int {
        require(type == GL_UNSIGNED_INT && (stride == 0 || stride == 20)) { "unsupported indirect index type $type stride $stride" }
        require(commands >= 0 && commands.toLong() * 20 <= TexturePacking.MAX_BYTES) { "indirect command limit $commands" }
        return commands * 20
    }

    fun indices(data: ByteArray, width: Int, baseVertex: Int): IntArray {
        require(width in intArrayOf(1, 2, 4) && data.size % width == 0)
        val raw = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        return IntArray(data.size / width) {
            val index = (when (width) { 1 -> raw.get(it).toLong() and 0xff; 2 -> raw.getShort(it * 2).toLong() and 0xffff; else -> raw.getInt(it * 4).toLong() and 0xffffffffL }) + baseVertex
            require(index in 0..Int.MAX_VALUE.toLong()) { "vertex index $index" }
            index.toInt()
        }
    }
}

class GlRecorder(writer: OrfWriter, width: Int, height: Int) : Recorder(writer, width, height) {
    private class Va(val buffer: Int, val bytes: Int, val stride: Int, val offset: Long, val divisor: Int, val enabled: Boolean)
    private class Atlas(val id: Int, val width: Int, val height: Int, val sizes: Map<Int, Pair<Int, Int>>, val layers: Map<Int, Int>)

    private val glTextures = HashMap<Triple<Int, Int, Boolean>, Int>()
    private val atlases = HashMap<String, Atlas>()
    private val targetSizes = HashMap<Int, Pair<Int, Int>>()
    private val initialTarget = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)
    private val clearTargets = hashSetOf(initialTarget)
    private val coverage = FrameCoverage(width, height)
    private val coverageDiagnostics = CoverageDiagnostics(coverage, writer.profile)
    private val aliases = FrameAliases(initialTarget, width, height)

    private fun currentScissor(): IntArray? = if (glIsEnabled(GL_SCISSOR_TEST)) ints(GL_SCISSOR_BOX).let { rect(it[0], it[1], it[2], it[3]) } else null

    fun nativeDraw() {
        val vp = ints(GL_VIEWPORT).let { rect(it[0], it[1], it[2], it[3]) }
        val scissor = currentScissor()
        val target = mainTarget()
        if (target) {
            coverage.cover(intArrayOf(0, 0, width, height), scissor)
            firstFullDraw(CoverageOperation.NATIVE_DRAW, null, 0, 0, 0, vp, scissor, argumentsKnown = false)
        }
        skip("${FrameDump.shader(GLWrapper.getInteger(GL_CURRENT_PROGRAM))}@native${if (target) "" else ".offscreen"}", state(), vp, scissor ?: IntArray(4))
    }

    fun nativeBlit(sourceBounds: IntArray, targetBounds: IntArray, mask: Int, filter: Int) {
        if (mask and GL_COLOR_BUFFER_BIT == 0 || !mainTarget()) return
        val source = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
        val target = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)
        val sourceBuffer = if (source == 0) GL_BACK else GL_COLOR_ATTACHMENT0
        val targetBuffer = if (target == 0) GL_BACK else GL_COLOR_ATTACHMENT0
        if (glGetInteger(GL_READ_BUFFER) == sourceBuffer && glGetInteger(GL_DRAW_BUFFER0) == targetBuffer && !glIsEnabled(GL_FRAMEBUFFER_SRGB) && aliases.identityCopy(source, target, sourceBounds, targetBounds, mask, filter, glIsEnabled(GL_SCISSOR_TEST))) {
            writer.profile.add("aliasIdentityCopies", 1)
            return
        }
        val (x0, y0, x1, y1) = targetBounds
        val left = minOf(x0, x1).toLong()
        val bottom = minOf(y0, y1).toLong()
        val right = maxOf(x0, x1).toLong()
        val top = maxOf(y0, y1).toLong()
        require(right - left <= Int.MAX_VALUE && top - bottom <= Int.MAX_VALUE) { "blit coverage bounds" }
        val area = ColorClear.rectangle(width, height, intArrayOf(left.toInt(), bottom.toInt(), (right - left).toInt(), (top - bottom).toInt())) ?: return
        val scissor = currentScissor()
        coverage.cover(area, scissor)
        coverageDiagnostics.blit(source, target, mask, filter, sourceBounds, targetBounds, scissor?.let { rect(it[0], it[1], it[2], it[3]) })
    }

    fun captureHistory() {
        coverageDiagnostics.history()
        val regions = coverage.inherited()
        if (regions.isEmpty()) return
        require(initialTarget == 0) { "untouched history requires default BACK framebuffer" }
        var pixels = 0L
        for (region in regions) {
            inherited(region, GlRead.backRegion(region, width, height))
            pixels += region[2].toLong() * region[3]
        }
        writer.profile.add("historyPixels", pixels)
        writer.profile.add("historyRegions", regions.size.toLong())
    }

    fun aliasTarget(source: Int, target: Int, targetWidth: Int, targetHeight: Int) {
        if (aliases.register(source, target, targetWidth, targetHeight)) clearTargets.add(target)
        else if (target != initialTarget) clearTargets.remove(target)
    }

    fun clear(mask: Int) {
        if (mask and GL_COLOR_BUFFER_BIT == 0 || glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) !in clearTargets) return
        if (glGetInteger(GL_DRAW_BUFFER0) == GL_NONE) return
        val writeMask = MemoryStack.stackPush().use { stack ->
            val buffer = stack.malloc(4)
            glGetBooleanv(GL_COLOR_WRITEMASK, buffer)
            BooleanArray(4) { buffer.get(it) != 0.toByte() }
        }
        if (!ColorClear.enabled(writeMask)) return
        require(!glIsEnabled(GL_FRAMEBUFFER_SRGB)) { "sRGB color clear unsupported" }
        val color = FloatArray(4).also { glGetFloatv(GL_COLOR_CLEAR_VALUE, it) }
        val scissor = if (glIsEnabled(GL_SCISSOR_TEST)) ints(GL_SCISSOR_BOX) else null
        ColorClear.rectangle(width, height, scissor)?.let { coverage.cover(it) }
        coverageDiagnostics.firstFull(CoverageOperation.CLEAR) {
            writer.profile.add("coverageFirstFullDrawFbo", GLWrapper.getInteger(GL_DRAW_FRAMEBUFFER_BINDING).toLong())
            coverageDiagnostics.clip(scissor)
        }
        colorClear(color, scissor, writeMask)
    }

    private fun ints(pname: Int) = IntArray(4).also { glGetIntegerv(pname, it) }

    private fun le(ints: IntArray) = ByteBuffer.allocate(ints.size * 4).order(ByteOrder.LITTLE_ENDIAN).also { it.asIntBuffer().put(ints) }.array()

    private fun glTexture(name: Int, target: Int = GL_TEXTURE_2D, flipY: Boolean = false) = if (name == 0) NONE else glTextures.getOrPut(Triple(name, target, flipY)) {
        GlRead.glTexture(name, target, flipY)?.let { texture(it.width, it.height, it.layers, it.rgba, it.format, it.sampler) } ?: NONE
    }

    private fun samplerBinding(sampler: String, u: Map<String, FloatArray>): Int {
        val value = u[sampler]?.firstOrNull() ?: return 0
        require(value.isFinite() && value == value.toInt().toFloat()) { "$sampler unit $value" }
        return GlRead.binding(value.toInt(), SamplerTargets.of(sampler))
    }

    private fun texture(sampler: String, u: Map<String, FloatArray>) = glTexture(samplerBinding(sampler, u), SamplerTargets.of(sampler), sampler == "uReflection")

    private fun requiredTexture(sampler: String, u: Map<String, FloatArray>): Int = texture(sampler, u).also {
        require(it != NONE) { "missing $sampler" }
    }

    private fun enabled(u: Map<String, FloatArray>, flag: String) = u[flag]?.firstOrNull() == 1f

    private fun scene(u: Map<String, FloatArray>, color: Boolean) {
        val size = u["uFramebufferSize"]
        require(size != null && size.size == 2 && size[0] == width.toFloat() && size[1] == height.toFloat()) { "scene framebuffer size" }
        if (color) GlRead.sceneTexture(samplerBinding("uSceneColor", u), width, height, false)
        GlRead.sceneTexture(samplerBinding("uSceneDepth", u), width, height, true)
    }

    private fun mainTarget(): Boolean {
        val fbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)
        return fbo == 0 || targetSizes.getOrPut(fbo) { GlRead.targetSize() ?: (0 to 0) } == (width to height)
    }

    private fun attrib(i: Int): Va {
        val size = glGetVertexAttribi(i, GL_VERTEX_ATTRIB_ARRAY_SIZE)
        val bytes = size * when (glGetVertexAttribi(i, GL_VERTEX_ATTRIB_ARRAY_TYPE)) { GL_UNSIGNED_BYTE, GL_BYTE -> 1; GL_UNSIGNED_SHORT, GL_SHORT -> 2; else -> 4 }
        val stride = glGetVertexAttribi(i, GL_VERTEX_ATTRIB_ARRAY_STRIDE).let { if (it == 0) bytes else it }
        return Va(glGetVertexAttribi(i, GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING), bytes, stride, glGetVertexAttribPointer(i, GL_VERTEX_ATTRIB_ARRAY_POINTER), glGetVertexAttribi(i, GL_VERTEX_ATTRIB_ARRAY_DIVISOR), glGetVertexAttribi(i, GL_VERTEX_ATTRIB_ARRAY_ENABLED) != 0)
    }

    private fun state(): Int {
        var s = 0
        if (glIsEnabled(GL_DEPTH_TEST)) s = s or 1
        if (glGetBoolean(GL_DEPTH_WRITEMASK)) s = s or 2
        if (glIsEnabled(GL_BLEND)) {
            val src = glGetInteger(GL_BLEND_SRC_RGB)
            val dst = glGetInteger(GL_BLEND_DST_RGB)
            s = s or ((if (dst == GL_ONE) 2 else if (src == GL_ONE) 3 else 1) shl 2)
        }
        if (glIsEnabled(GL_CULL_FACE)) s = s or ((if (glGetInteger(GL_CULL_FACE_MODE) == GL_FRONT) 2 else 1) shl 4)
        if (glGetInteger(GL_FRONT_FACE) == GL_CW) s = s or 64
        return s or ((glGetInteger(GL_DEPTH_FUNC) - GL_NEVER) shl 7)
    }

    private fun rect(x: Int, y: Int, w: Int, h: Int) = intArrayOf(x, height - y - h, w, h)

    private fun firstFullDraw(operation: CoverageOperation, pass: Pass?, mode: Int, type: Int, instances: Int, viewport: IntArray, scissor: IntArray?, reason: CoverageFallback? = null, projection: FloatArray? = null, argumentsKnown: Boolean = true) {
        coverageDiagnostics.firstFull(operation) {
            val profile = writer.profile
            profile.add("coverageFirstFullProgram", GLWrapper.getInteger(GL_CURRENT_PROGRAM).toLong())
            profile.add("coverageFirstFullDrawFbo", GLWrapper.getInteger(GL_DRAW_FRAMEBUFFER_BINDING).toLong())
            profile.add("coverageFirstFullPass", (pass?.id ?: -1).toLong())
            coverageDiagnostics.drawArguments(mode, type, instances, argumentsKnown)
            coverageDiagnostics.viewport(viewport)
            profile.add("coverageFirstFullPolygonMode", ints(GL_POLYGON_MODE)[0].toLong())
            coverageDiagnostics.clip(scissor?.let { rect(it[0], it[1], it[2], it[3]) })
            if (reason != null) profile.add("coverageFirstFullFallback" + reason.suffix, 1)
            if (projection != null) {
                profile.add("coverageFirstFullProjectionSize", projection.size.toLong())
                profile.add("coverageFirstFullProjectionFinite", if (projection.all { it.isFinite() }) 1 else 0)
                profile.add("coverageFirstFullProjectionAffine", if (projection.size == 16 && projection[3] == 0f && projection[7] == 0f && projection[15] == 1f) 1 else 0)
            }
            if (FrameCoverage.supports2d(pass)) {
                profile.add("coverageFirstFullPositionType", glGetVertexAttribi(0, GL_VERTEX_ATTRIB_ARRAY_TYPE).toLong())
                profile.add("coverageFirstFullPositionSize", glGetVertexAttribi(0, GL_VERTEX_ATTRIB_ARRAY_SIZE).toLong())
                profile.add("coverageFirstFullPositionDivisor", glGetVertexAttribi(0, GL_VERTEX_ATTRIB_ARRAY_DIVISOR).toLong())
                profile.add("coverageFirstFullPositionInteger", glGetVertexAttribi(0, GL_VERTEX_ATTRIB_ARRAY_INTEGER).toLong())
                if (pass === Passes.sprite) {
                    profile.add("coverageFirstFullClipType", glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_TYPE).toLong())
                    profile.add("coverageFirstFullClipSize", glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_SIZE).toLong())
                    profile.add("coverageFirstFullClipDivisor", glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_DIVISOR).toLong())
                    profile.add("coverageFirstFullClipInteger", glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_INTEGER).toLong())
                }
            }
        }
    }

    fun draw(mode: Int, count: Int, type: Int, offset: Long, instances: Int, baseInstance: Int, baseVertex: Int, drawId: Int) {
        val program = glGetInteger(GL_CURRENT_PROGRAM)
        val name = FrameDump.shader(program)
        val vp = ints(GL_VIEWPORT).let { rect(it[0], it[1], it[2], it[3]) }
        val scissor = if (glIsEnabled(GL_SCISSOR_TEST)) ints(GL_SCISSOR_BOX).let { rect(it[0], it[1], it[2], it[3]) } else IntArray(4)
        val st = state()
        if (!mainTarget()) return skip("$name@offscreen", st, vp, scissor)
        val coverageScissor = if (glIsEnabled(GL_SCISSOR_TEST)) scissor else null
        val pass = Passes.byName[name]
        val bounded = CaptureTopology.viewportBounded(pass, mode, ints(GL_POLYGON_MODE)[0])
        val bounds = if (bounded) vp else intArrayOf(0, 0, width, height)
        if (!bounded || !FrameCoverage.supports2d(pass) || type == 0 || instances != 1) {
            val reason = when {
                !bounded -> CoverageFallback.RASTER
                type == 0 -> CoverageFallback.INDEX_TYPE
                instances != 1 -> CoverageFallback.INSTANCES
                else -> null
            }
            coverage.cover(bounds, coverageScissor)
            if (reason != null) coverageDiagnostics.fallback(pass, reason)
            firstFullDraw(CoverageOperation.DRAW_BOUNDS, pass, mode, type, instances, vp, coverageScissor, reason)
        }
        val patchVertices = if (mode == GL_PATCHES && name == "hdWater") glGetInteger(GL_PATCH_VERTICES) else 0
        if (!CaptureTopology.supported(name, mode, patchVertices) || type == 0) return skip(if (mode == GL_PATCHES && name == "hdWater") "$name@patchArity$patchVertices" else "$name@mode$mode", st, vp, scissor)
        if (pass == null) return skip(name, st, vp, scissor)
        val u = Uniforms.read(program)
        try {
            if (pass === Passes.hdWater) {
                require(count > 0 && count % 3 == 0 && instances == 1) { "water triangle patch count $count instances $instances" }
                if (mode == GL_PATCHES) require(u["uTessPixels"]?.firstOrNull()?.let { it.isFinite() && it > 0f } == true) { "missing water tessellation size" }
            }
            draw(build(pass, u, count, type, offset, instances, baseInstance, baseVertex, drawId, st, vp, scissor, name, mode, bounded))
        } catch (e: Exception) {
            coverage.cover(bounds, coverageScissor)
            coverageDiagnostics.fallback(pass, CoverageFallback.BUILD)
            firstFullDraw(CoverageOperation.DRAW_ERROR, pass, mode, type, instances, vp, coverageScissor, CoverageFallback.BUILD)
            if (writer.failure != null) throw e
            if (skipped["$name.error"] == null) e.printStackTrace()
            skip("$name.error", st, vp, scissor)
        }
    }

    private fun build(pass: Pass, u: Map<String, FloatArray>, count: Int, type: Int, offset: Long, instances: Int, baseInstance: Int, baseVertex: Int, drawId: Int, st: Int, vp: IntArray, scissor: IntArray, name: String, mode: Int, bounded: Boolean): DrawRec {
        val width = when (type) { GL_UNSIGNED_BYTE -> 1; GL_UNSIGNED_SHORT -> 2; GL_UNSIGNED_INT -> 4; else -> error("index type $type") }
        require(count > 0 && count.toLong() * width <= TexturePacking.MAX_BYTES) { "index byte limit $count" }
        val idx = CaptureTopology.indices(GlRead.buffer(glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING), offset, count * width), width, baseVertex)
        val lo = idx.min()
        val span = idx.max().toLong() - lo + 1
        require(span * pass.stride + count.toLong() * 4 <= TexturePacking.MAX_BYTES) { "mesh byte limit $span vertices $count indices" }
        val n = span.toInt()
        val vertices = ByteArray(n * pass.stride)
        for (a in pass.attrs) {
            if (pass === Passes.terrain && a.location == 3 && !enabled(u, "uHasNormals")) continue
            val va = attrib(a.location)
            if (!va.enabled || va.bytes > a.bytes) throw IllegalStateException("attribute ${a.location} ${va.bytes} > ${a.bytes}")
            val length = (n - 1).toLong() * va.stride + va.bytes
            require(length in 1..TexturePacking.MAX_BYTES.toLong()) { "attribute byte limit $length" }
            val src = GlRead.buffer(va.buffer, Math.addExact(va.offset, lo.toLong() * va.stride), length.toInt())
            for (i in 0 until n) System.arraycopy(src, i * va.stride, vertices, i * pass.stride + a.offset, va.bytes)
        }
        for (i in idx.indices) idx[i] -= lo
        if (FrameCoverage.supports2d(pass)) {
            val projection = u["uProjection"] ?: FloatArray(0)
            val sc = if (glIsEnabled(GL_SCISSOR_TEST)) scissor else null
            val position = attrib(0)
            var reason = when {
                instances != 1 -> CoverageFallback.INSTANCES
                position.divisor != 0 -> CoverageFallback.POSITION_DIVISOR
                glGetVertexAttribi(0, GL_VERTEX_ATTRIB_ARRAY_TYPE) != GL_FLOAT -> CoverageFallback.POSITION_TYPE
                glGetVertexAttribi(0, GL_VERTEX_ATTRIB_ARRAY_SIZE) != 2 -> CoverageFallback.POSITION_SIZE
                else -> null
            }
            val clipValid = pass !== Passes.sprite || CaptureTopology.spriteClipAttribute(glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_TYPE), glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_SIZE), glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_DIVISOR), glGetVertexAttribi(5, GL_VERTEX_ATTRIB_ARRAY_INTEGER) != 0)
            if (reason == null && !clipValid) reason = CoverageFallback.CLIP_ATTRIBUTE
            if (reason == null && !coverage.affine2d(pass, vertices, idx, projection, vp, sc)) reason = coverage.affineFailure
            if (reason != null) {
                coverage.cover(vp, sc)
                coverageDiagnostics.fallback(pass, reason, if (bounded && instances != 1) CoverageFallback.INSTANCES else null)
            } else if (bounded) coverageDiagnostics.tight(pass)
            firstFullDraw(if (reason == null) CoverageOperation.AFFINE_2D else CoverageOperation.FALLBACK_2D, pass, mode, type, instances, vp, sc, reason, projection)
        }
        var textureIds = IntArray(0)
        var instance = NONE
        val extra = HashMap<String, FloatArray>()
        when (pass.id) {
            Passes.sprite.id -> textureIds = intArrayOf(sprite(vertices, n, u))
            Passes.model.id -> textureIds = intArrayOf(if (u["uTextureEnabled"]?.get(0) == 1f) texture("uTexture", u) else NONE)
            Passes.ground.id -> textureIds = intArrayOf(if (u["uGroundShadowEnabled"]?.get(0) == 1f) texture("uGroundShadowTex", u) else NONE, if (u["uShadowEnabled"]?.get(0) == 1f) texture("uShadowMap", u) else NONE)
            Passes.terrain.id -> {
                val mode = u["uWaterMode"]?.firstOrNull() ?: 0f
                require(mode in 0f..4f && mode == mode.toInt().toFloat()) { "terrain water mode $mode" }
                if (mode == 4f && enabled(u, "uDepthFog")) scene(u, false)
                textureIds = intArrayOf(
                    if (enabled(u, "uHasTexture")) requiredTexture("uTexture", u) else NONE,
                    if (enabled(u, "uGroundShadowEnabled")) requiredTexture("uGroundShadowTex", u) else NONE,
                    if (mode != 0f && enabled(u, "uHasWaterNoise")) requiredTexture("uWaterNoise", u) else NONE
                )
            }
            Passes.hdWater.id -> {
                scene(u, true)
                textureIds = intArrayOf(
                    if (enabled(u, "uHasWaveNoise")) requiredTexture("uWaveNoise", u) else NONE,
                    if (enabled(u, "uHasFoamNoise")) requiredTexture("uFoamNoise", u) else NONE,
                    if (enabled(u, "uHasEnvMap")) requiredTexture("uEnvMap", u) else NONE,
                    if (enabled(u, "uShadowEnabled")) requiredTexture("uShadowTex", u) else NONE,
                    if (enabled(u, "uUsePlanar")) requiredTexture("uReflection", u) else NONE
                )
            }
            Passes.instanced.id -> {
                val va = attrib(5)
                if (va.divisor != 1) throw IllegalStateException("instance divisor ${va.divisor}")
                instance = instances(instances, GlRead.buffer(va.buffer, va.offset + baseInstance.toLong() * va.stride, instances * va.stride))
                textureIds = intArrayOf(material(u, drawId, extra))
            }
            else -> textureIds = IntArray(pass.samplers.size) { texture(pass.samplers[it], u) }
        }
        return DrawRec(pass.id, mesh(pass, vertices, le(idx)), uniform(pass.pack(u, extra)), textureIds, st, vp, scissor, instance, name)
    }

    private fun material(u: Map<String, FloatArray>, drawId: Int, extra: MutableMap<String, FloatArray>): Int {
        if (u.containsKey("uTextureEnabled")) {
            val flags = (u["uTextureEnabled"]!![0].toInt() and 1) or ((u["uCombineMode"]?.get(0)?.toInt() ?: 0) shl 1) or ((u["uTexAlpha"]?.get(0)?.toInt() ?: 0) shl 4)
            extra["uMaterialFlags"] = floatArrayOf(flags.toFloat())
            return texture("uTexture", u)
        }
        val ssbo = glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING, 0)
        val m = ByteBuffer.wrap(GlRead.buffer(ssbo, (u["uDrawBase"]!![0].toLong() + drawId) * 16, 16)).order(ByteOrder.LITTLE_ENDIAN)
        val flags = m.getInt(8)
        val scroll = m.getInt(12)
        extra["uMaterialFlags"] = floatArrayOf((flags and 0x1f).toFloat())
        extra["uTextureSpeed"] = floatArrayOf((scroll shl 24 shr 24).toFloat(), (scroll shl 16 shr 24).toFloat())
        return if (flags and 1 == 0) NONE else glTexture(FrameDump.textureOf(m.getLong(0)))
    }

    private fun atlas(slots: Set<Int>, u: Map<String, FloatArray>): Atlas {
        val units = u["uTextures"] ?: FloatArray(8) { it.toFloat() }
        val names = slots.associateWith { GlRead.binding(units[it].toInt()) }
        return atlases.getOrPut(names.entries.joinToString(",") { "${it.key}:${it.value}" }) {
            val pixels = names.mapValues { if (it.value == 0) null else GlRead.glTexture(it.value) }
            require(pixels.values.all { it == null || it.format == RecordedTextureFormat.RGBA8 }) { "typed sprite atlases unsupported" }
            val packed = writer.profile.measure("atlasPack") { SpriteAtlas(pixels) }
            Atlas(texture(packed.width, packed.height, packed.layers.size, packed.rgba), packed.width, packed.height, packed.sizes, packed.layers)
        }
    }

    private fun sprite(vertices: ByteArray, n: Int, u: Map<String, FloatArray>): Int {
        val buf = ByteBuffer.wrap(vertices).order(ByteOrder.LITTLE_ENDIAN)
        val slots = (0 until n).map { Math.round(buf.getFloat(it * 40 + 20)) }
        val used = slots.filter { it >= 0 }.toSortedSet()
        for (i in 0 until n) {
            val y0 = buf.getFloat(i * 40 + 28)
            val y1 = buf.getFloat(i * 40 + 36)
            buf.putFloat(i * 40 + 28, height - y1)
            buf.putFloat(i * 40 + 36, height - y0)
        }
        if (used.isEmpty()) return NONE
        val a = atlas(used, u)
        for (i in 0 until n) {
            val (w, h) = a.sizes[slots[i]] ?: continue
            buf.putFloat(i * 40 + 20, a.layers.getValue(slots[i]).toFloat())
            buf.putFloat(i * 40 + 8, buf.getFloat(i * 40 + 8) * w / a.width)
            buf.putFloat(i * 40 + 12, buf.getFloat(i * 40 + 12) * h / a.height)
        }
        return a.id
    }

    fun indirect(mode: Int, type: Int, offset: Long, commands: Int, stride: Int) {
        val length = CaptureTopology.indirectBytes(commands, type, stride)
        val b = ByteBuffer.wrap(GlRead.buffer(glGetInteger(GL_DRAW_INDIRECT_BUFFER_BINDING), offset, length)).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until commands) draw(mode, b.getInt(i * 20), type, b.getInt(i * 20 + 8).toLong() * 4, b.getInt(i * 20 + 4), b.getInt(i * 20 + 16), b.getInt(i * 20 + 12), i)
    }
}
