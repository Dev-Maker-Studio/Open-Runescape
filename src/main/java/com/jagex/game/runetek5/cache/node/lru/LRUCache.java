package com.jagex.game.runetek5.cache.node.lru;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Queue;

/**
 * A cache that evicts that least recently used element when at capacity.
 * 
 * [NXT]
	jag::LRUCache<jag::audio::SoundResource>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         .text  00000000003624F0 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::game::AnimationWrapper>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000035EBA0 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::game::ClientScript>::Put(jag::shared_ptr<jag::game::ClientScript> const&,uint,ulong) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  0000000000307AD0 00000433 000000A8 00000048 R . . . . . .
	jag::LRUCache<jag::game::ClientScript>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           .text  0000000000361CE0 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::game::MSIType>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                .text  00000000003616F0 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::game::QuickChatCatType>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000035D350 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::game::QuickChatPhraseType>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    .text  000000000035F030 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::graphics::Animation>::Put(jag::shared_ptr<jag::graphics::Animation> const&,uint,ulong) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     .text  00000000004829E0 00000433 000000A8 00000048 R . . . . . .
	jag::LRUCache<jag::graphics::Model>::Clean(ulong) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000334830 0000012C 00000038 00000000 R . . . . . .
	jag::LRUCache<jag::graphics::Model>::Put(jag::shared_ptr<jag::graphics::Model> const&,uint,ulong) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000431E40 0000042F 000000A8 00000048 R . . . . . .
	jag::LRUCache<jag::graphics::Model>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              .text  0000000000362B60 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::graphics::SpriteAtlasReference>::Put(jag::shared_ptr<jag::graphics::SpriteAtlasReference> const&,uint,ulong) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               .text  00000000003B1480 00000453 000000A8 00000048 R . . . . . .
	jag::LRUCache<jag::graphics::SpriteAtlasReference>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               .text  000000000035F4C0 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::graphics::TextureData>::~LRUCache() [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000035DDB0 00000105 00000028 00000000 R . . . . . .
	jag::LRUCache<jag::render::Texture>::Put(jag::shared_ptr<jag::render::Texture> const&,uint,ulong) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  0000000000482580 00000453 000000A8 00000048 R . . . . . . 
 */
public class LRUCache {
    public final Queue queue;
    public final int capacity;
    public int capacityLeft;
    public final IterableNodeMap nodeMap;

    public LRUCache(int big, int small) {
        this.queue = new Queue<>();
        this.capacity = big;
        this.capacityLeft = big;
        int size;
        for (size = 1; size + size < big && size < small; size += size) { }
        this.nodeMap = new IterableNodeMap<>(size);
    }

    public LRUCache(int capacity) {
        this(capacity, capacity);
    }

    /**
     * [J]
     */
    public void put(Object object, long uid) {
        put(object, uid, 1);
    }

    /**
     * [J]
     */
    public void put(Object object, long uid, int capacityTaken) {
        if (capacityTaken > this.capacity) {
            throw new IllegalStateException();
        } 
        removeByUid(uid);
        this.capacityLeft -= capacityTaken;
        while (this.capacityLeft < 0) {
            AbstractLRUNode cacheableNode = (AbstractLRUNode) this.queue.getFirst();
            remove(cacheableNode);
        }
        LRUNode replacingNode = new LRUNode(object, capacityTaken);
        this.nodeMap.put(replacingNode, uid);
        this.queue.insert(replacingNode);
        replacingNode.indexReference = 0L;
    }

    /**
     * [J]
     */
    public void clean(int max) {
        for (AbstractLRUNode class282Sub50Sub13 = (AbstractLRUNode) this.queue.getNext(); class282Sub50Sub13 != null; class282Sub50Sub13 = (AbstractLRUNode) this.queue.getDirectNext()) {
            if (class282Sub50Sub13.isReusable()) {
                if (class282Sub50Sub13.getValue() == null) {
                    class282Sub50Sub13.unlink();
                    class282Sub50Sub13.unlinkSub();
                    this.capacityLeft += class282Sub50Sub13.capacityRequirement;
                }
            } else if (++class282Sub50Sub13.indexReference > max) {
                LRUSoftReference class282Sub50Sub1Sub24 = new LRUSoftReference(class282Sub50Sub13.getValue(), class282Sub50Sub13.capacityRequirement);
                this.nodeMap.put(class282Sub50Sub1Sub24, class282Sub50Sub13.pointer);
                CacheableNode.rebuild(class282Sub50Sub1Sub24, class282Sub50Sub13);
                class282Sub50Sub13.unlink();
                class282Sub50Sub13.unlinkSub();
            }
        }
    }

    public void reset() {
        this.queue.reset();
        this.nodeMap.clear();
        this.capacityLeft = this.capacity;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getCapacityLeft() {
        return this.capacityLeft;
    }

    public int count() {
        int count = 0;
        for (AbstractLRUNode node = (AbstractLRUNode) this.queue.getNext(); node != null; node = (AbstractLRUNode) this.queue.getDirectNext()) {
            if (!node.isReusable()) {
                ++count;
            }
        }
        return count;
    }

    public void resize() {
        for (AbstractLRUNode cacheableNode = (AbstractLRUNode) this.queue.getNext(); cacheableNode != null; cacheableNode = (AbstractLRUNode) this.queue.getDirectNext()) {
            if (cacheableNode.isReusable()) {
                cacheableNode.unlink();
                cacheableNode.unlinkSub();
                this.capacityLeft += cacheableNode.capacityRequirement;
            }
        }
    }

    public void remove(AbstractLRUNode cacheableNode) {
        if (cacheableNode != null) {
            cacheableNode.unlink();
            cacheableNode.unlinkSub();
            this.capacityLeft += cacheableNode.capacityRequirement;
        }
    }

    public Object get(long uid) {
        AbstractLRUNode cachedNode = (AbstractLRUNode) this.nodeMap.get(uid);
        if (cachedNode == null) {
            return null;
        } 
        Object object = cachedNode.getValue();
        if (object == null) {
            cachedNode.unlink();
            cachedNode.unlinkSub();
            this.capacityLeft += cachedNode.capacityRequirement;
            return null;
        } 
        if (cachedNode.isReusable()) {
            LRUNode newNode = new LRUNode(object, cachedNode.capacityRequirement);
            this.nodeMap.put(newNode, cachedNode.pointer);
            this.queue.insert(newNode);
            newNode.indexReference = 0L;
            cachedNode.unlink();
            cachedNode.unlinkSub();
        } else {
            this.queue.insert(cachedNode);
            cachedNode.indexReference = 0L;
        }
        return object;
    }

    public Object getFirst() {
        AbstractLRUNode lastNode;
        for (AbstractLRUNode cachedNode = (AbstractLRUNode) this.nodeMap.first(); cachedNode != null; this.capacityLeft += lastNode.capacityRequirement) {
            Object object = cachedNode.getValue();
            if (object != null) {
                return object;
            }
            lastNode = cachedNode;
            cachedNode = (AbstractLRUNode) this.nodeMap.next();
            lastNode.unlink();
            lastNode.unlinkSub();
        }
        return null;
    }

    public Object getNext() {
        AbstractLRUNode class282Sub50Sub14;
        for (AbstractLRUNode class282Sub50Sub12 = (AbstractLRUNode) this.nodeMap.next(); class282Sub50Sub12 != null; this.capacityLeft += class282Sub50Sub14.capacityRequirement) {
            Object object3 = class282Sub50Sub12.getValue();
            if (object3 != null) {
                return object3;
            }
            class282Sub50Sub14 = class282Sub50Sub12;
            class282Sub50Sub12 = (AbstractLRUNode) this.nodeMap.next();
            class282Sub50Sub14.unlink();
            class282Sub50Sub14.unlinkSub();
        }
        return null;
    }

    public void removeByUid(long uid) {
        AbstractLRUNode cacheableNode = (AbstractLRUNode) this.nodeMap.get(uid);
        remove(cacheableNode);
    }
}
