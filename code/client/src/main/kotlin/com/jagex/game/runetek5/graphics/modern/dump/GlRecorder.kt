package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.lwjgl.opengl.GL33.*
import org.lwjgl.opengl.GL40.GL_DRAW_INDIRECT_BUFFER_BINDING
import org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER_BINDING

class GlRecorder(writer: OrfWriter, width: Int, height: Int) : Recorder(writer, width, height) {
    private class Va(val buffer: Int, val bytes: Int, val stride: Int, val offset: Long, val divisor: Int, val enabled: Boolean)
    private class Atlas(val id: Int, val width: Int, val height: Int, val sizes: Map<Int, Pair<Int, Int>>)

    private val glTextures = HashMap<Int, Int>()
    private val atlases = HashMap<String, Atlas>()
    private val targetSizes = HashMap<Int, Pair<Int, Int>>()

    private fun ints(pname: Int) = IntArray(4).also { glGetIntegerv(pname, it) }

    private fun le(ints: IntArray) = ByteBuffer.allocate(ints.size * 4).order(ByteOrder.LITTLE_ENDIAN).also { it.asIntBuffer().put(ints) }.array()

    private fun glTexture(name: Int) = if (name == 0) NONE else glTextures.getOrPut(name) { GlRead.glTexture(name)?.let { texture(it.width, it.height, 1, it.rgba) } ?: NONE }

    private fun texture(sampler: String, u: Map<String, FloatArray>) = u[sampler]?.get(0)?.toInt()?.let { glTexture(GlRead.binding(it)) } ?: NONE

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

    fun draw(mode: Int, count: Int, type: Int, offset: Long, instances: Int, baseInstance: Int, baseVertex: Int, drawId: Int) {
        val program = glGetInteger(GL_CURRENT_PROGRAM)
        val name = FrameDump.shader(program)
        val vp = ints(GL_VIEWPORT).let { rect(it[0], it[1], it[2], it[3]) }
        val scissor = if (glIsEnabled(GL_SCISSOR_TEST)) ints(GL_SCISSOR_BOX).let { rect(it[0], it[1], it[2], it[3]) } else IntArray(4)
        val st = state()
        if (!mainTarget()) return skip("$name@offscreen", st, vp, scissor)
        if (mode != GL_TRIANGLES || type == 0) return skip("$name@mode$mode", st, vp, scissor)
        val pass = Passes.byName[name] ?: return skip(name, st, vp, scissor)
        val u = Uniforms.read(program)
        if (pass === Passes.terrain && (u["uWaterMode"]?.get(0) ?: 0f) != 0f) return skip("$name.water", st, vp, scissor)
        try {
            draw(build(pass, u, count, type, offset, instances, baseInstance, baseVertex, drawId, st, vp, scissor, name))
        } catch (e: Exception) {
            if (writer.failure != null) throw e
            if (skipped["$name.error"] == null) e.printStackTrace()
            skip("$name.error", st, vp, scissor)
        }
    }

    private fun build(pass: Pass, u: Map<String, FloatArray>, count: Int, type: Int, offset: Long, instances: Int, baseInstance: Int, baseVertex: Int, drawId: Int, st: Int, vp: IntArray, scissor: IntArray, name: String): DrawRec {
        val width = when (type) { GL_UNSIGNED_BYTE -> 1; GL_UNSIGNED_SHORT -> 2; else -> 4 }
        val raw = ByteBuffer.wrap(GlRead.buffer(glGetInteger(GL_ELEMENT_ARRAY_BUFFER_BINDING), offset, count * width)).order(ByteOrder.LITTLE_ENDIAN)
        val idx = IntArray(count) { (when (width) { 1 -> raw.get(it).toInt() and 0xff; 2 -> raw.getShort(it * 2).toInt() and 0xffff; else -> raw.getInt(it * 4) }) + baseVertex }
        val lo = idx.min()
        val n = idx.max() - lo + 1
        val vertices = ByteArray(n * pass.stride)
        for (a in pass.attrs) {
            val va = attrib(a.location)
            if (!va.enabled || va.bytes > a.bytes) throw IllegalStateException("attribute ${a.location} ${va.bytes} > ${a.bytes}")
            val src = GlRead.buffer(va.buffer, va.offset + lo.toLong() * va.stride, (n - 1) * va.stride + va.bytes)
            for (i in 0 until n) System.arraycopy(src, i * va.stride, vertices, i * pass.stride + a.offset, va.bytes)
        }
        for (i in idx.indices) idx[i] -= lo
        var textureIds = IntArray(0)
        var instance = NONE
        val extra = HashMap<String, FloatArray>()
        when (pass.id) {
            Passes.sprite.id -> textureIds = intArrayOf(sprite(vertices, n, u))
            Passes.model.id -> textureIds = intArrayOf(if (u["uTextureEnabled"]?.get(0) == 1f) texture("uTexture", u) else NONE)
            Passes.ground.id -> textureIds = intArrayOf(if (u["uGroundShadowEnabled"]?.get(0) == 1f) texture("uGroundShadowTex", u) else NONE, if (u["uShadowEnabled"]?.get(0) == 1f) texture("uShadowMap", u) else NONE)
            Passes.terrain.id -> textureIds = intArrayOf(if (u["uHasTexture"]?.get(0) == 1f) texture("uTexture", u) else NONE, if (u["uGroundShadowEnabled"]?.get(0) == 1f) texture("uGroundShadowTex", u) else NONE)
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
            val w = pixels.values.maxOf { it?.width ?: 1 }
            val h = pixels.values.maxOf { it?.height ?: 1 }
            val layers = slots.max() + 1
            val data = ByteArray(w * h * layers * 4)
            for ((slot, p) in pixels) {
                if (p == null) continue
                for (y in 0 until p.height) System.arraycopy(p.rgba, y * p.width * 4, data, ((slot * h + y) * w) * 4, p.width * 4)
            }
            Atlas(texture(w, h, layers, data), w, h, pixels.mapValues { (it.value?.width ?: w) to (it.value?.height ?: h) })
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
            buf.putFloat(i * 40 + 8, buf.getFloat(i * 40 + 8) * w / a.width)
            buf.putFloat(i * 40 + 12, buf.getFloat(i * 40 + 12) * h / a.height)
        }
        return a.id
    }

    fun indirect(mode: Int, type: Int, offset: Long, commands: Int, stride: Int) {
        val b = ByteBuffer.wrap(GlRead.buffer(glGetInteger(GL_DRAW_INDIRECT_BUFFER_BINDING), offset, commands * 20)).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until commands) draw(mode, b.getInt(i * 20), type, b.getInt(i * 20 + 8).toLong() * 4, b.getInt(i * 20 + 4), b.getInt(i * 20 + 16), b.getInt(i * 20 + 12), i)
    }
}
