package com.darkan.oracle

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.TreeMap

object JsonNull

object Canonical {

    const val MAX_INLINE = 256

    fun write(value: Any): String {
        val out = StringBuilder()
        emit(value, out)
        return out.toString()
    }

    private fun emit(value: Any, out: StringBuilder) {
        when (value) {
            JsonNull -> out.append("null")
            is Boolean -> out.append(value)
            is Long -> out.append(value)
            is String -> string(value, out)
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((key, entry) in value) {
                    if (!first) out.append(',')
                    first = false
                    string(key as String, out)
                    out.append(':')
                    emit(entry!!, out)
                }
                out.append('}')
            }
            is List<*> -> {
                out.append('[')
                value.forEachIndexed { index, entry ->
                    if (index > 0) out.append(',')
                    emit(entry!!, out)
                }
                out.append(']')
            }
            else -> error("not a canonical JSON value: ${value.javaClass.name}")
        }
    }

    private fun string(value: String, out: StringBuilder) {
        out.append('"')
        for (ch in value) {
            when {
                ch == '"' -> out.append("\\\"")
                ch == '\\' -> out.append("\\\\")
                ch.code < 0x20 || ch.code == 0x7f -> out.append("\\u%04x".format(ch.code))
                else -> out.append(ch)
            }
        }
        out.append('"')
    }

    fun obj(vararg entries: Pair<String, Any?>): TreeMap<String, Any> {
        val map = TreeMap<String, Any>()
        for ((key, entry) in entries) {
            if (entry != null) {
                require(map.put(key, entry) == null) { "duplicate key $key" }
            }
        }
        return map
    }

    fun int(value: Int): Long = value.toLong()

    fun f32(value: Float): String = "f32:0x%08x".format(value.toRawBits())

    fun f64(value: Double): String = "f64:0x%016x".format(value.toRawBits())

    fun bytes(value: ByteArray): Any = packed("i8", value.size, value)

    fun packed(type: String, length: Int, bytes: ByteArray): Any =
        if (length > MAX_INLINE) obj("len" to length.toLong(), "sha256" to sha256(bytes)) else "$type:${hex(bytes)}"

    fun array(elements: List<Any>): Any =
        if (elements.size > MAX_INLINE) {
            obj("len" to elements.size.toLong(), "sha256" to sha256(write(elements).toByteArray(Charsets.UTF_8)))
        } else {
            elements
        }

    fun primitiveArray(value: Any): Any? {
        val (type, length, buffer) = when (value) {
            is ByteArray -> Triple("i8", value.size, ByteBuffer.wrap(value.copyOf()))
            is BooleanArray -> Triple("bool", value.size, ByteBuffer.allocate(value.size).also { b -> value.forEach { b.put(if (it) 1 else 0) } })
            is ShortArray -> Triple("i16", value.size, ByteBuffer.allocate(value.size * 2).also { b -> value.forEach { b.putShort(it) } })
            is CharArray -> Triple("u16", value.size, ByteBuffer.allocate(value.size * 2).also { b -> value.forEach { b.putChar(it) } })
            is IntArray -> Triple("i32", value.size, ByteBuffer.allocate(value.size * 4).also { b -> value.forEach { b.putInt(it) } })
            is LongArray -> Triple("i64", value.size, ByteBuffer.allocate(value.size * 8).also { b -> value.forEach { b.putLong(it) } })
            is FloatArray -> Triple("f32", value.size, ByteBuffer.allocate(value.size * 4).also { b -> value.forEach { b.putInt(it.toRawBits()) } })
            is DoubleArray -> Triple("f64", value.size, ByteBuffer.allocate(value.size * 8).also { b -> value.forEach { b.putLong(it.toRawBits()) } })
            else -> return null
        }
        return packed(type, length, buffer.array())
    }

    fun sha256(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun hex(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            out.append(HEX[(b.toInt() shr 4) and 0xf]).append(HEX[b.toInt() and 0xf])
        }
        return out.toString()
    }

    private const val HEX = "0123456789abcdef"
}
