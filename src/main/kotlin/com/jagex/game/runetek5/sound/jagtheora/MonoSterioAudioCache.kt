package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.ConcurrentQueue
import java.util.HashMap

class MonoSterioAudioCache(@JvmField val cacheType: MonoSterioCacheType) {

    @JvmField val lifetimeMs: Long = -1
    @JvmField val capacity: Int = 64
    @JvmField var cache: MutableMap<Long, MonoSterioAudioReference>
    @JvmField var referenceCache: ConcurrentQueue
    @JvmField var maxCapacityCache: ConcurrentQueue

    init {
        if (cacheType == null) {
            throw IllegalArgumentException("")
        }
        cache = HashMap(capacity)
        referenceCache = ConcurrentQueue(capacity)
        maxCapacityCache = ConcurrentQueue(capacity)
    }

    @Synchronized
    @JvmName("pop")
    fun pop(key: Long): MonoSterioData? {
        if (lifetimeMs != -1L) {
            sweep()
        }
        val node = cache.remove(key)
        val data: MonoSterioData?
        if (node != null) {
            referenceCache.remove(node)
            if (hasCapacity()) {
                maxCapacityCache.remove(node)
            }
            data = node.data
        } else {
            data = null
        }
        return data
    }

    fun hasCapacity(): Boolean {
        return capacity != -1
    }

    @Synchronized
    @JvmName("get")
    operator fun get(key: Long): MonoSterioData? {
        if (lifetimeMs != -1L) {
            sweep()
        }
        val node = cache[key]
        val data: MonoSterioData?
        if (node == null) {
            data = null
        } else {
            refresh(node, false)
            data = node.data
        }
        return data
    }

    @Synchronized
    @JvmName("put")
    fun put(key: Long, newData: MonoSterioData): MonoSterioData? {
        if (lifetimeMs != -1L) {
            sweep()
        }
        val reference = cache[key]
        if (reference != null) {
            val existingData = reference.data
            reference.data = newData
            refresh(reference, false)
            return existingData
        }
        var node: MonoSterioAudioReference
        if (hasCapacity() && cache.size == capacity) {
            node = maxCapacityCache.remove() as MonoSterioAudioReference
            cache.remove(node.key)
            referenceCache.remove(node)
        }
        node = MonoSterioAudioReference(newData, key)
        cache[key] = node
        refresh(node, true)
        return null
    }

    fun refresh(reference: MonoSterioAudioReference, existingEntry: Boolean) {
        if (!existingEntry) {
            referenceCache.remove(reference)
            if (hasCapacity() && !maxCapacityCache.remove(reference)) {
                throw IllegalStateException("")
            }
        }
        reference.refreshTime = System.currentTimeMillis()
        if (hasCapacity()) {
            when (cacheType.type) {
                0 -> reference.ticker++
                1 -> reference.ticker = reference.refreshTime
            }
            maxCapacityCache.add(reference)
        }
        referenceCache.add(reference)
    }

    @Synchronized
    fun clear() {
        cache.clear()
        referenceCache.clear()
        if (hasCapacity()) {
            maxCapacityCache.clear()
        }
    }

    fun sweep() {
        if (lifetimeMs == -1L) {
            throw IllegalStateException("")
        }
        val cutoff = System.currentTimeMillis() - lifetimeMs
        while (!referenceCache.isEmpty()) {
            val reference = referenceCache.peek() as MonoSterioAudioReference
            if (reference.refreshTime >= cutoff) {
                break
            }
            cache.remove(reference.key)
            referenceCache.remove(reference)
            if (hasCapacity()) {
                maxCapacityCache.remove(reference)
            }
        }
    }
}
