package com.jagex.game.runetek5.sound.jagtheora;

import java.util.HashMap;
import java.util.Map;

import com.jagex.game.runetek5.cache.node.ConcurrentQueue;

public class MonoSterioAudioCache {

    public final long lifetimeMs;

    public final int capacity;

    public final MonoSterioCacheType cacheType;

    public Map<Long, MonoSterioAudioReference> cache;

    public ConcurrentQueue referenceCache;

    public ConcurrentQueue maxCapacityCache;

    public MonoSterioAudioCache(MonoSterioCacheType cacheType) {
        this.lifetimeMs = -1;
        this.capacity = 64;
        this.cacheType = cacheType;
        if (this.cacheType == null) {
            throw new IllegalArgumentException("");
        }
        this.cache = new HashMap<Long, MonoSterioAudioReference>(this.capacity);
        this.referenceCache = new ConcurrentQueue(this.capacity);
        this.maxCapacityCache = new ConcurrentQueue(this.capacity);
    }

    public MonoSterioData pop(Long key) {
        synchronized (this) {
            if (this.lifetimeMs != -1L) {
                sweep();
            }
            MonoSterioAudioReference node = this.cache.remove(key);
            MonoSterioData data;
            if (node != null) {
                this.referenceCache.remove(node);
                if (hasCapacity()) {
                    this.maxCapacityCache.remove(node);
                }
                data = node.data;
            } else {
                data = null;
            }
            return data;
        }
    }

    public boolean hasCapacity() {
        return this.capacity != -1;
    }

    public MonoSterioData get(Long key) {
        synchronized (this) {
            if (this.lifetimeMs != -1L) {
                sweep();
            }
            MonoSterioAudioReference node = this.cache.get(key);
            MonoSterioData data;
            if (node == null) {
                data = null;
            } else {
                refresh(node, false);
                data = node.data;
            }
            return data;
        }
    }

    public MonoSterioData put(Long key, MonoSterioData newData) {
        synchronized (this) {
            if (this.lifetimeMs != -1L) {
                sweep();
            }
            MonoSterioAudioReference reference = this.cache.get(key);
            if (reference != null) {
                MonoSterioData existingData = reference.data;
                reference.data = newData;
                refresh(reference, false);
                return existingData;
            } 
            MonoSterioAudioReference node;
            if (hasCapacity() && this.cache.size() == this.capacity) {
                node = (MonoSterioAudioReference) this.maxCapacityCache.remove();
                this.cache.remove(node.key);
                this.referenceCache.remove(node);
            }
            node = new MonoSterioAudioReference(newData, key);
            this.cache.put(key, node);
            refresh(node, true);
            return null;
        }
    }

    public void refresh(MonoSterioAudioReference reference, boolean existingEntry) {
        if (!existingEntry) {
            this.referenceCache.remove(reference);
            if (hasCapacity() && !this.maxCapacityCache.remove(reference)) {
                throw new IllegalStateException("");
            }
        }
        reference.refreshTime = System.currentTimeMillis();
        if (hasCapacity()) {
            switch (this.cacheType.type) {
                case 0:
                    reference.ticker++;
                    break;
                case 1:
                    reference.ticker = reference.refreshTime;
            }
            this.maxCapacityCache.add(reference);
        }
        this.referenceCache.add(reference);
    }

    public void clear() {
        synchronized (this) {
            this.cache.clear();
            this.referenceCache.clear();
            if (hasCapacity()) {
                this.maxCapacityCache.clear();
            }
        }
    }

    public void sweep() {
        if (this.lifetimeMs == -1L) {
            throw new IllegalStateException("");
        } 
        long cutoff = System.currentTimeMillis() - this.lifetimeMs;
        while (!this.referenceCache.isEmpty()) {
            MonoSterioAudioReference reference = (MonoSterioAudioReference) this.referenceCache.peek();
            if (reference.refreshTime >= cutoff) {
                break;
            }
            this.cache.remove(reference.key);
            this.referenceCache.remove(reference);
            if (hasCapacity()) {
                this.maxCapacityCache.remove(reference);
            }
        }
    }
}
