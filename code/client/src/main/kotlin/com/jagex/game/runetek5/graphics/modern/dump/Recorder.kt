package com.jagex.game.runetek5.graphics.modern.dump

import java.security.MessageDigest

open class Recorder(val writer: OrfWriter, val width: Int, val height: Int) {
    class DrawRec(val pass: Int, val mesh: Int, val uniform: Int, val textures: IntArray, val state: Int, val viewport: IntArray, val scissor: IntArray, val inst: Int, val name: String)

    val drawn = sortedMapOf<String, Int>()
    val skipped = sortedMapOf<String, Int>()
    val meshIds = HashMap<String, Int>()
    val textureIds = HashMap<String, Int>()
    val uniformIds = HashMap<String, Int>()
    val instanceIds = HashMap<String, Int>()
    private val md5 = MessageDigest.getInstance("MD5")

    protected fun key(vararg parts: ByteArray): String {
        md5.reset()
        parts.forEach { md5.update(it); md5.update(0x7f) }
        return String(md5.digest(), Charsets.ISO_8859_1)
    }

    fun texture(width: Int, height: Int, layers: Int, rgba: ByteArray): Int =
        textureIds.getOrPut(key(Rec(12).u32(width, height, layers).array(), rgba)) { textureIds.size.also { writer.texture(it, width, height, layers, rgba) } }

    fun mesh(pass: Pass, vertexData: ByteArray, indexData: ByteArray): Int =
        meshIds.getOrPut(key(byteArrayOf(pass.id.toByte()), vertexData, indexData)) { meshIds.size.also { writer.mesh(it, pass.id, vertexData.size / pass.stride, indexData.size / 4, vertexData, indexData) } }

    fun uniform(block: ByteArray): Int = uniformIds.getOrPut(key(block)) { uniformIds.size.also { writer.uniform(it, block) } }

    fun instances(count: Int, data: ByteArray): Int = instanceIds.getOrPut(key(data)) { instanceIds.size.also { writer.instances(it, count, data) } }

    fun draw(d: DrawRec) {
        (if (d.pass == SKIP) skipped else drawn).merge(d.name, 1, Int::plus)
        writer.draw(d)
    }

    fun skip(why: String, state: Int, viewport: IntArray, scissor: IntArray) = draw(DrawRec(SKIP, 0, 0, IntArray(0), state, viewport, scissor, NONE, why))

    fun summary() = "drawn ${drawn.entries.joinToString(",") { "${it.key}=${it.value}" }} skipped ${skipped.entries.joinToString(",") { "${it.key}=${it.value}" }} meshes ${meshIds.size} textures ${textureIds.size} uniforms ${uniformIds.size} instances ${instanceIds.size}"

    companion object {
        const val NONE = -1
        const val SKIP = 255
    }
}
