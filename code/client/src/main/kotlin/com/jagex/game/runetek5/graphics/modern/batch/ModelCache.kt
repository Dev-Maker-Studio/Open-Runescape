package com.jagex.game.runetek5.graphics.modern.batch

import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class ModelCache(
    private val maxCacheSize: Int = 4096,
    private val maxVertexBytes: Long = 96 * 1024 * 1024
) {
    companion object {
        const val BYTES_PER_VERTEX = 40
    }

    class MaterialBatch(
        val materialId: Int,
        val indices: IntArray,
        val indexCount: Int
    )

    class CachedModel(
        var faceCount: Int,
        var vertexCount: Int,
        var vertexData: ByteBuffer,
        var materialBatches: Array<MaterialBatch>,
        var creationFrame: Long = 0,
        var heapOwned: Boolean = false
    ) {
        @JvmField
        var evicted: Boolean = false

        @JvmField
        var instanceEntry: Any? = null

        @JvmField
        var vertexWords: IntArray? = null

        @JvmField
        var vertexWordCount: Int = 0

        @JvmField
        var lastUsedFrame: Long = 0L

        fun free() {
            if (!heapOwned) MemoryUtil.memFree(vertexData)
        }

        fun getTotalBytes(): Int = vertexData.capacity() +
            materialBatches.sumOf { it.indices.size * 4 }
    }

    private val cache = LinkedHashMap<Long, CachedModel>(maxCacheSize, 0.75f, true)
    private var totalCachedBytes = 0L
    private var cacheHits = 0L
    private var cacheMisses = 0L
    private var currentFrame = 0L

    private var buildNanos = 0L

    private var evictions = 0L

    fun getBuildMillis(): Float = buildNanos / 1_000_000f

    fun getEvictions(): Long = evictions

    fun getSize(): Int = cache.size

    val frame: Long get() = currentFrame

    fun resetBuildTimer() {
        buildNanos = 0L
        evictions = 0L
    }

    fun getOrCreate(
        model: Any,
        modelId: Long,
        buildCache: () -> CachedModel?
    ): CachedModel? {
        val existing = cache[modelId]
        if (existing != null) {
            cacheHits++
            existing.lastUsedFrame = currentFrame
            return existing
        }

        cacheMisses++

        val buildStart = System.nanoTime()
        val cached = buildCache() ?: return null
        buildNanos += System.nanoTime() - buildStart
        cached.creationFrame = currentFrame
        cached.lastUsedFrame = currentFrame

        val newBytes = cached.getTotalBytes()
        while (totalCachedBytes + newBytes > maxVertexBytes && cache.isNotEmpty()) {
            evictOldest()
        }

        if (cache.size >= maxCacheSize) {
            evictOldest()
        }

        cache[modelId] = cached
        totalCachedBytes += newBytes

        return cached
    }

    private fun evict(modelId: Long) {
        val cached = cache.remove(modelId)
        if (cached != null) {
            totalCachedBytes -= cached.getTotalBytes()
            cached.evicted = true
            cached.free()
            evictions++
        }
    }

    private fun evictOldest() {
        var refreshed: ArrayList<Long>? = null
        var victim: Long? = null
        for ((key, entry) in cache) {
            if (entry.lastUsedFrame >= currentFrame - 1) {
                (refreshed ?: ArrayList<Long>().also { refreshed = it }).add(key)
                continue
            }
            victim = key
            break
        }
        refreshed?.forEach { cache[it] }
        evict(victim ?: cache.keys.firstOrNull() ?: return)
    }

    fun onFrameStart() {
        currentFrame++
    }

    fun getStats(): CacheStats = CacheStats(
        cachedModels = cache.size,
        totalBytes = totalCachedBytes,
        hits = cacheHits,
        misses = cacheMisses,
        hitRate = if (cacheHits + cacheMisses > 0) cacheHits.toFloat() / (cacheHits + cacheMisses) else 0f
    )

    fun clear() {
        for (cached in cache.values) {
            cached.evicted = true
            cached.free()
        }
        cache.clear()
        totalCachedBytes = 0
    }

    data class CacheStats(
        val cachedModels: Int,
        val totalBytes: Long,
        val hits: Long,
        val misses: Long,
        val hitRate: Float
    ) {
        val totalMB: Float get() = totalBytes / (1024f * 1024f)
    }
}
