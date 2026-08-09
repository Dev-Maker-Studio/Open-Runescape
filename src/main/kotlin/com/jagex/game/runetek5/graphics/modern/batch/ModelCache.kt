package com.jagex.game.runetek5.graphics.modern.batch

import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Caches processed model geometry to avoid per-face CPU work every frame.
 *
 * The key insight is that most model data (vertex positions, normals, UVs, face indices)
 * is static. Only the model matrix changes each frame. By caching the processed vertex
 * data, we can skip the expensive per-face loop and simply copy the cached data.
 *
 * OPTIMIZED: Pre-sorted material batches eliminate runtime sorting.
 * Each CachedModel contains MaterialBatch entries that can be directly
 * used to build the index buffer without per-face iteration.
 *
 * 40-byte vertex format (matches GPU format exactly for zero-copy):
 * Per vertex (40 bytes):
 * - position: vec3 float (12 bytes) - LOCAL space
 * - normal: vec3 float (12 bytes) - pre-computed normalized normal
 * - texcoord: vec2 float (8 bytes)
 * - color: vec4 ubyte (4 bytes) - base color (RGBA)
 * - modelIndex: float (4 bytes) - placeholder, filled at render time
 */
class ModelCache(
    /**
     * Max models to cache.
     *
     * Must be sized against the live unique-mesh count, not a round number. At 512
     * against an observed ~2500 resident unique meshes, EVERY miss tripped the
     * `cache.size >= maxCacheSize` eviction, freeing a buffer that was needed again
     * moments later — so misses rebuilt geometry the cache had just discarded, and
     * the rebuild (native alloc + full per-face walk) dominated the frame
     * (tkMs lookup=67ms of a 106ms frame). The 96MB byte cap below is the real
     * memory bound; this is only an entry-count backstop and must not bite first.
     */
    private val maxCacheSize: Int = 4096,
    private val maxVertexBytes: Long = 96 * 1024 * 1024  // 96MB max cache
) {
    companion object {
        // Cache format now matches GPU format exactly: 40 bytes per vertex
        // position(12) + normal(12) + texcoord(8) + color(4) + modelIndex(4)
        const val BYTES_PER_VERTEX = 40
    }

    /**
     * Pre-sorted material batch for fast index buffer building.
     * Contains all vertex indices for a single material.
     */
    class MaterialBatch(
        /** Material/texture ID (0 = untextured) */
        val materialId: Int,
        /** Pre-computed vertex indices relative to model start */
        val indices: IntArray,
        /** Number of indices (always a multiple of 3) */
        val indexCount: Int
    )

    /**
     * Cached model data ready for GPU upload.
     *
     * OPTIMIZED: Contains pre-sorted MaterialBatch entries that eliminate
     * the per-face material sorting loop at render time.
     */
    class CachedModel(
        /** Number of faces in this model */
        val faceCount: Int,
        /** Number of vertices (faceCount * 3) */
        val vertexCount: Int,
        /**
         * Pre-processed vertex data buffer in GPU format (40 bytes per vertex):
         * position(12) + normal(12) + texcoord(8) + color(4) + modelIndex(4)
         */
        val vertexData: ByteBuffer,
        /**
         * Pre-sorted material batches for fast index buffer building.
         * Sorted by materialId (0 first for untextured, then by ID).
         * This eliminates the per-face material sorting at render time.
         */
        val materialBatches: Array<MaterialBatch>,
        /** Frame number when this cache was created (for staleness check) */
        var creationFrame: Long = 0
    ) {
        fun free() {
            MemoryUtil.memFree(vertexData)
        }

        fun getTotalBytes(): Int = vertexData.capacity() +
            materialBatches.sumOf { it.indices.size * 4 }
    }

    // Cache storage using LRU eviction (access-ordered LinkedHashMap)
    // Key is content hash, value is the cached model data
    private val cache = LinkedHashMap<Long, CachedModel>(maxCacheSize, 0.75f, true)
    private var totalCachedBytes = 0L
    private var cacheHits = 0L
    private var cacheMisses = 0L
    private var currentFrame = 0L

    /** Nanoseconds spent inside buildCache lambdas since the last [resetBuildTimer]. */
    private var buildNanos = 0L

    /** Entries evicted since the last [resetBuildTimer]. */
    private var evictions = 0L

    fun getBuildMillis(): Float = buildNanos / 1_000_000f

    fun getEvictions(): Long = evictions

    fun getSize(): Int = cache.size

    fun resetBuildTimer() {
        buildNanos = 0L
        evictions = 0L
    }

    /**
     * Gets or creates cached data for a model.
     *
     * @param model The model to cache
     * @param modelId A content-based hash identifying the model's geometry
     * @param buildCache Lambda to build cache if not present
     * @return Cached model data, or null if caching failed
     */
    fun getOrCreate(
        model: Any,
        modelId: Long,
        buildCache: () -> CachedModel?
    ): CachedModel? {
        // Check existing cache by content hash
        val existing = cache[modelId]
        if (existing != null) {
            // Content hash matches - return cached data
            cacheHits++
            return existing
        }

        cacheMisses++

        // Build new cache. Timed separately from the lookup: a miss costs a native
        // allocation plus a full per-face walk, and lumping it into the lookup timer
        // hid which of the two was actually eating the frame.
        val buildStart = System.nanoTime()
        val cached = buildCache() ?: return null
        buildNanos += System.nanoTime() - buildStart
        cached.creationFrame = currentFrame

        // Check if we have space
        val newBytes = cached.getTotalBytes()
        while (totalCachedBytes + newBytes > maxVertexBytes && cache.isNotEmpty()) {
            evictOldest()
        }

        if (cache.size >= maxCacheSize) {
            evictOldest()
        }

        // Store in cache
        cache[modelId] = cached
        totalCachedBytes += newBytes

        return cached
    }

    private fun evict(modelId: Long) {
        val cached = cache.remove(modelId)
        if (cached != null) {
            totalCachedBytes -= cached.getTotalBytes()
            cached.free()
            evictions++
        }
    }

    private fun evictOldest() {
        val oldestKey = cache.keys.firstOrNull() ?: return
        evict(oldestKey)
    }

    /**
     * Call once per frame to track frame numbers.
     */
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
