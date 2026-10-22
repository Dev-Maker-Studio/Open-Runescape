package com.jagex.game.runetek5.graphics.modern.dump

import java.nio.ByteBuffer
import java.lang.invoke.MethodHandles
import java.nio.ByteOrder
import java.security.MessageDigest

open class Recorder(val writer: OrfWriter, val width: Int, val height: Int) {
    class DrawRec(val pass: Int, val mesh: Int, val uniform: Int, val textures: IntArray, val state: Int, val viewport: IntArray, val scissor: IntArray, val inst: Int, val name: String)

    val drawn = sortedMapOf<String, Int>()
    val skipped = sortedMapOf<String, Int>()
    val meshIds = HashMap<String, Int>()
    val textureIds = HashMap<String, Int>()
    val uniformIds = HashMap<String, Int>()
    val instanceIds = HashMap<String, Int>()
    protected fun key(vararg parts: ByteArray): String {
        return writer.profile.measure("resourceHash") {
            ResourceKeys.key(parts)
        }
    }

    @JvmOverloads
    fun texture(width: Int, height: Int, layers: Int, rgba: ByteArray, format: RecordedTextureFormat = RecordedTextureFormat.RGBA8, sampler: Int = 9): Int {
        val fingerprint = key(Rec(20).u32(width, height, layers, format.id, if (format == RecordedTextureFormat.RGBA8) 0 else sampler).array(), rgba)
        return textureIds.getOrPut(fingerprint) { textureIds.size.also { writer.texture(it, width, height, layers, rgba, format, sampler, fingerprint) } }
    }

    fun mesh(pass: Pass, vertexData: ByteArray, indexData: ByteArray): Int {
        val fingerprint = key(byteArrayOf(pass.id.toByte()), vertexData, indexData)
        return meshIds.getOrPut(fingerprint) { meshIds.size.also { writer.mesh(it, pass.id, vertexData.size / pass.stride, indexData.size / 4, vertexData, indexData, fingerprint) } }
    }

    fun uniform(block: ByteArray): Int = uniformIds.getOrPut(key(block)) { uniformIds.size.also { writer.uniform(it, block) } }

    fun instances(count: Int, data: ByteArray): Int = instanceIds.getOrPut(key(data)) { instanceIds.size.also { writer.instances(it, count, data) } }

    fun draw(d: DrawRec) {
        (if (d.pass == SKIP) skipped else drawn).merge(d.name, 1, Int::plus)
        writer.draw(d)
    }

    fun bloom(whitePoint: Float, bloom: Float, brightpass: Float) = writer.bloom(whitePoint, bloom, brightpass)

    fun skip(why: String, state: Int, viewport: IntArray, scissor: IntArray) = draw(DrawRec(SKIP, 0, 0, IntArray(0), state, viewport, scissor, NONE, why))

    fun colorClear(color: FloatArray, scissor: IntArray?, writeMask: BooleanArray) {
        if (!ColorClear.enabled(writeMask)) return
        val rect = ColorClear.rectangle(width, height, scissor) ?: return
        val mesh = mesh(Passes.sprite, ColorClear.vertices(rect, color), Rec(24).u32(0, 1, 2, 0, 2, 3).array())
        val uniform = uniform(Passes.sprite.pack(mapOf("uProjection" to ColorClear.projection(width, height))))
        val name = if (rect.contentEquals(intArrayOf(0, 0, width, height))) "clear.color.full" else "clear.color.scissored"
        draw(DrawRec(Passes.sprite.id, mesh, uniform, intArrayOf(NONE), 7 shl 7, intArrayOf(0, 0, width, height), rect, NONE, name))
    }

    fun inherited(rect: IntArray, rgba: ByteArray) {
        require(rect.size == 4 && rect[0] >= 0 && rect[1] >= 0 && rect[2] > 0 && rect[3] > 0 && rect[0].toLong() + rect[2] <= width && rect[1].toLong() + rect[3] <= height)
        require(rgba.size.toLong() == rect[2].toLong() * rect[3] * 4 && rgba.size <= FrameCoverage.MAX_BYTES)
        val vertices = ColorClear.vertices(rect, floatArrayOf(1f, 1f, 1f, 1f))
        val buffer = ByteBuffer.wrap(vertices).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0..3) {
            buffer.putFloat(i * 40 + 8, if (i == 1 || i == 2) 1f else 0f)
            buffer.putFloat(i * 40 + 12, if (i >= 2) 1f else 0f)
            buffer.putFloat(i * 40 + 20, -3f)
        }
        val mesh = mesh(Passes.sprite, vertices, Rec(24).u32(0, 1, 2, 0, 2, 3).array())
        val uniform = uniform(Passes.sprite.pack(mapOf("uProjection" to ColorClear.projection(width, height))))
        val texture = texture(rect[2], rect[3], 1, rgba)
        draw(DrawRec(Passes.sprite.id, mesh, uniform, intArrayOf(texture), 7 shl 7, intArrayOf(0, 0, width, height), rect, NONE, "history.untouched"))
    }

    fun summary() ="drawn ${drawn.entries.joinToString(",") { "${it.key}=${it.value}" }} skipped ${skipped.entries.joinToString(",") { "${it.key}=${it.value}" }} meshes ${meshIds.size} textures ${textureIds.size} uniforms ${uniformIds.size} instances ${instanceIds.size}"

    companion object {
        const val NONE = -1
        const val SKIP = 255
    }
}

object ResourceKeys {
    private const val DIRECT_LIMIT = 4096
    private const val MAX_BYTES = 192L shl 20
    private val longs = MethodHandles.byteArrayViewVarHandle(LongArray::class.java, ByteOrder.LITTLE_ENDIAN)
    private val md5 = MessageDigest.getInstance("MD5")

    private class Entry(val parts: Array<ByteArray>, val key: String, val bytes: Long)

    private val entries = LinkedHashMap<Long, ArrayList<Entry>>(256, 0.75f, true)
    private var held = 0L

    @JvmStatic
    @Synchronized
    fun key(parts: Array<out ByteArray>): String {
        var total = 0L
        for (p in parts) total += p.size
        if (total < DIRECT_LIMIT) return digest(parts)
        val h = fast(parts)
        val bucket = entries[h]
        if (bucket != null) {
            for (e in bucket) if (same(e.parts, parts)) return e.key
        }
        val key = digest(parts)
        val entry = Entry(Array(parts.size) { parts[it].copyOf() }, key, total)
        entries.getOrPut(h) { ArrayList(1) }.add(entry)
        held += total
        trim()
        return key
    }

    @JvmStatic
    @Synchronized
    fun clear() {
        entries.clear()
        held = 0L
    }

    @JvmStatic
    @Synchronized
    fun heldBytes(): Long = held

    @JvmStatic
    fun digest(parts: Array<out ByteArray>): String {
        md5.reset()
        for (p in parts) {
            md5.update(p)
            md5.update(0x7f)
        }
        return String(md5.digest(), Charsets.ISO_8859_1)
    }

    private fun same(a: Array<ByteArray>, b: Array<out ByteArray>): Boolean {
        if (a.size != b.size) return false
        for (i in a.indices) if (!a[i].contentEquals(b[i])) return false
        return true
    }

    private fun fast(parts: Array<out ByteArray>): Long {
        var h = parts.size.toLong() * -0x61c8864680b583ebL
        for (p in parts) {
            val n = p.size
            var a = 0x243F6A8885A308D3L
            var b = 0x13198A2E03707344L
            var c = -0x5bf0e3f1eb1c9f30L
            var d = 0x082EFA98EC4E6C89L
            var i = 0
            while (i + 32 <= n) {
                a = (a xor (longs.get(p, i) as Long)) * -0x61c8864680b583ebL
                a = a xor (a ushr 29)
                b = (b xor (longs.get(p, i + 8) as Long)) * -0x61c8864680b583ebL
                b = b xor (b ushr 29)
                c = (c xor (longs.get(p, i + 16) as Long)) * -0x61c8864680b583ebL
                c = c xor (c ushr 29)
                d = (d xor (longs.get(p, i + 24) as Long)) * -0x61c8864680b583ebL
                d = d xor (d ushr 29)
                i += 32
            }
            var tail = n.toLong()
            while (i < n) {
                tail = (tail shl 8) xor (p[i].toLong() and 0xff) xor (tail ushr 56)
                i++
            }
            h = (h xor a xor java.lang.Long.rotateLeft(b, 17) xor java.lang.Long.rotateLeft(c, 31) xor java.lang.Long.rotateLeft(d, 47) xor tail) * -0x61c8864680b583ebL
            h = h xor (h ushr 32)
        }
        return h
    }

    private fun trim() {
        val it = entries.entries.iterator()
        while (held > MAX_BYTES && it.hasNext()) {
            val bucket = it.next().value
            for (e in bucket) held -= e.bytes
            it.remove()
        }
    }
}
