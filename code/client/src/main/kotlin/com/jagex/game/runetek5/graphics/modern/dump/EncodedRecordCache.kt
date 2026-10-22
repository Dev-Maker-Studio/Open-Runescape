package com.jagex.game.runetek5.graphics.modern.dump

import java.io.OutputStream

class EncodedRecordCache(private val maxBytes: Long = 32L * 1024 * 1024, private val maxEntries: Int = 512) : AutoCloseable {
    internal data class Key(val kind: Int, val fingerprint: String, val header: String, val rawLength: Int) {
        val size: Long = 128L + 2L * (fingerprint.length.toLong() + header.length)
    }

    internal class EncodedRecord(kind: Int, rawLength: Int, payload: ByteArray, packedLength: Int) {
        private val header = Rec(12).u32(kind, rawLength, packedLength).array()
        private val bytes = payload.copyOf()
        val size: Long = header.size.toLong() + bytes.size

        fun writeTo(out: OutputStream) {
            out.write(header)
            out.write(bytes)
        }
    }

    data class Stats(val bytes: Long, val entries: Int, val evictions: Long, val closed: Boolean)

    private val entries = LinkedHashMap<Key, EncodedRecord>(16, 0.75f, true)
    private var bytes = 0L
    private var evictions = 0L
    private var closed = false

    init {
        require(maxBytes >= 0 && maxEntries >= 0)
    }

    @Synchronized
    internal fun get(key: Key): EncodedRecord? = if (closed) null else entries[key]

    @Synchronized
    internal fun accepts(key: Key, recordSize: Long): Boolean = !closed && maxEntries > 0 && key.size + recordSize <= maxBytes

    @Synchronized
    internal fun put(key: Key, record: EncodedRecord) {
        if (!accepts(key, record.size)) return
        entries.remove(key)?.let { bytes -= key.size + it.size }
        while (entries.size >= maxEntries || bytes + key.size + record.size > maxBytes) {
            val oldest = entries.entries.iterator()
            val entry = oldest.next()
            bytes -= entry.key.size + entry.value.size
            oldest.remove()
            evictions++
        }
        entries[key] = record
        bytes += key.size + record.size
    }

    @Synchronized
    fun stats() = Stats(bytes, entries.size, evictions, closed)

    @Synchronized
    override fun close() {
        closed = true
        entries.clear()
        bytes = 0
    }
}
