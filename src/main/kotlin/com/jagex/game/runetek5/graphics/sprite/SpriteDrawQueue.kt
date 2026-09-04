package com.jagex.game.runetek5.graphics.sprite

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.impl.SpriteRenderNode
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.cache.node.impl.DirectSpriteRenderNode
import com.jagex.game.runetek5.cache.node.impl.ReferencedSpriteRenderNode

class SpriteDrawQueue {

    @JvmField var currentCapacity: Int
    @JvmField val spriteDrawQueue = Queue<SpriteRenderNode>()
    @JvmField val maxCapacity: Int
    @JvmField val spriteRenderNodes: IterableNodeMap<SpriteRenderNode>

    init {
        maxCapacity = 250
        currentCapacity = 250
        var capacity = 1
        while (capacity + capacity < 250) {
            capacity += capacity
        }
        spriteRenderNodes = IterableNodeMap(capacity)
    }

    fun getMaxCapacity(): Int {
        return maxCapacity
    }

    fun discard(itemRenderer: IItemRenderer) {
        val uid = itemRenderer.getUid()
        var renderNode = spriteRenderNodes.get(uid)
        while (renderNode != null) {
            if (renderNode.itemRenderer.matches(itemRenderer)) {
                discardNode(renderNode)
                break
            }
            renderNode = spriteRenderNodes.nextInBucket()
        }
    }

    fun discardNode(renderNode: SpriteRenderNode?) {
        if (renderNode != null) {
            renderNode.unlink()
            renderNode.unlinkSub()
            currentCapacity += renderNode.size
        }
    }

    fun insert(nativeSprite: NativeSprite, renderer: IItemRenderer) {
        if (maxCapacity < 1) {
            throw IllegalStateException()
        }
        /** Ensure if the node already exist we delete it before adding a new one to replace it **/
        discard(renderer)
        currentCapacity -= 1
        while (currentCapacity < 0) {
            val renderNode = spriteDrawQueue.getFirst() as SpriteRenderNode
            discardNode(renderNode)
        }
        /** Insert the new node **/
        val directRenderNode = DirectSpriteRenderNode(renderer, nativeSprite, 1)
        spriteRenderNodes.put(directRenderNode, renderer.getUid())
        spriteDrawQueue.insert(directRenderNode)
        directRenderNode.indexReference = 0L
    }

    fun reset() {
        spriteDrawQueue.reset()
        spriteRenderNodes.clear()
        currentCapacity = maxCapacity
    }

    fun processCycle() {
        var spriteRenderNode = spriteDrawQueue.getNext() as SpriteRenderNode?
        while (spriteRenderNode != null) {
            if (spriteRenderNode.hasSoftReference()) {
                /** The native sprite is soft referenced, if it's not reachable we should unlink **/
                if (spriteRenderNode.createNativeSprite() == null) {
                    spriteRenderNode.unlink()
                    spriteRenderNode.unlinkSub()
                    currentCapacity += spriteRenderNode.size
                }
            } else if (++spriteRenderNode.indexReference > 5) {
                /** We reached the cycles to unlink, lets change that direct reference into a soft reference to save memory **/
                val referencedRenderNode = ReferencedSpriteRenderNode(spriteRenderNode.itemRenderer, spriteRenderNode.createNativeSprite(), spriteRenderNode.size)
                spriteRenderNodes.put(referencedRenderNode, spriteRenderNode.pointer)
                CacheableNode.rebuild(referencedRenderNode, spriteRenderNode)
                spriteRenderNode.unlink()
                spriteRenderNode.unlinkSub()
            }
            spriteRenderNode = spriteDrawQueue.getDirectNext() as SpriteRenderNode?
        }
    }

    fun getCurrentCapacity(): Int {
        return currentCapacity
    }

    fun remove() {
        var renderNode = spriteDrawQueue.getNext() as SpriteRenderNode?
        while (renderNode != null) {
            if (renderNode.hasSoftReference()) {
                renderNode.unlink()
                renderNode.unlinkSub()
                currentCapacity += renderNode.size
            }
            renderNode = spriteDrawQueue.getDirectNext() as SpriteRenderNode?
        }
    }

    fun getNativeSprite(renderer: IItemRenderer): NativeSprite? {
        val uid = renderer.getUid()
        var nativeSpriteNode = spriteRenderNodes.get(uid)
        while (nativeSpriteNode != null) {
            if (nativeSpriteNode.itemRenderer.matches(renderer)) {
                val nativeSprite = nativeSpriteNode.createNativeSprite()
                if (nativeSprite != null) {
                    if (nativeSpriteNode.hasSoftReference()) {
                        val directRenderNode = DirectSpriteRenderNode(renderer, nativeSprite, nativeSpriteNode.size)
                        spriteRenderNodes.put(directRenderNode, nativeSpriteNode.pointer)
                        spriteDrawQueue.insert(directRenderNode)
                        directRenderNode.indexReference = 0L
                        nativeSpriteNode.unlink()
                        nativeSpriteNode.unlinkSub()
                    } else {
                        spriteDrawQueue.insert(nativeSpriteNode)
                        nativeSpriteNode.indexReference = 0L
                    }
                    return nativeSprite
                }
                nativeSpriteNode.unlink()
                nativeSpriteNode.unlinkSub()
                currentCapacity += nativeSpriteNode.size
            }
            nativeSpriteNode = spriteRenderNodes.nextInBucket()
        }
        return null
    }
}
