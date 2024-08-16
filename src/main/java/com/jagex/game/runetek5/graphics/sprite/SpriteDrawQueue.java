package com.jagex.game.runetek5.graphics.sprite;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.cache.node.impl.SpriteRenderNode;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.cache.node.impl.DirectSpriteRenderNode;
import com.jagex.game.runetek5.cache.node.impl.ReferencedSpriteRenderNode;

public class SpriteDrawQueue {

    public int currentCapacity;
    public final Queue<SpriteRenderNode> spriteDrawQueue = new Queue<SpriteRenderNode>();
    public final int maxCapacity;
    public final IterableNodeMap<SpriteRenderNode> spriteRenderNodes;

    public SpriteDrawQueue() {
        this.maxCapacity = 250;
        this.currentCapacity = 250;
        int capacity;
        for (capacity = 1; capacity + capacity < 250; capacity += capacity) { }
        this.spriteRenderNodes = new IterableNodeMap<SpriteRenderNode>(capacity);
    }

    public int getMaxCapacity() {
        return this.maxCapacity;
    }

    public void discard(IItemRenderer itemRenderer) {
        long uid = itemRenderer.getUid();
        for (SpriteRenderNode renderNode = this.spriteRenderNodes.get(uid); renderNode != null; renderNode = this.spriteRenderNodes.nextInBucket()) {
            if (renderNode.itemRenderer.matches(itemRenderer)) {
                discardNode(renderNode);
                break;
            }
        }
    }

    public void discardNode(SpriteRenderNode renderNode) {
        if (renderNode != null) {
            renderNode.unlink();
            renderNode.unlinkSub();
            this.currentCapacity += renderNode.size;
        }
    }

    public void insert(NativeSprite nativeSprite, IItemRenderer renderer) {
        if (this.maxCapacity < 1) {
            throw new IllegalStateException();
        }
        /** Ensure if the node already exist we delete it before adding a new one to replace it **/
        discard(renderer);
        this.currentCapacity -= 1;
        while (this.currentCapacity < 0) {
            SpriteRenderNode renderNode = (SpriteRenderNode) this.spriteDrawQueue.getFirst();
            discardNode(renderNode);
        }
        /** Insert the new node **/
        DirectSpriteRenderNode directRenderNode = new DirectSpriteRenderNode(renderer, nativeSprite, 1);
        this.spriteRenderNodes.put(directRenderNode, renderer.getUid());
        this.spriteDrawQueue.insert(directRenderNode);
        directRenderNode.indexReference = 0L;
    }

    public void reset() {
        this.spriteDrawQueue.reset();
        this.spriteRenderNodes.clear();
        this.currentCapacity = this.maxCapacity;
    }

    public void processCycle() {
        for (SpriteRenderNode spriteRenderNode = (SpriteRenderNode) this.spriteDrawQueue.getNext(); spriteRenderNode != null; spriteRenderNode = (SpriteRenderNode) this.spriteDrawQueue.getDirectNext()) {
            if (spriteRenderNode.hasSoftReference()) {
            	/** The native sprite is soft referenced, if it's not reachable we should unlink **/
                if (spriteRenderNode.createNativeSprite() == null) {
                    spriteRenderNode.unlink();
                    spriteRenderNode.unlinkSub();
                    this.currentCapacity += spriteRenderNode.size;
                }
            } else if (++spriteRenderNode.indexReference > 5) {
            	/** We reached the cycles to unlink, lets change that direct reference into a soft reference to save memory **/
                ReferencedSpriteRenderNode referencedRenderNode = new ReferencedSpriteRenderNode(spriteRenderNode.itemRenderer, spriteRenderNode.createNativeSprite(), spriteRenderNode.size);
                this.spriteRenderNodes.put(referencedRenderNode, spriteRenderNode.pointer);
                CacheableNode.rebuild(referencedRenderNode, spriteRenderNode);
                spriteRenderNode.unlink();
                spriteRenderNode.unlinkSub();
            }
        }

    }

    public int getCurrentCapacity() {
        return this.currentCapacity;
    }

    public void remove() {
        for (SpriteRenderNode renderNode = (SpriteRenderNode) this.spriteDrawQueue.getNext(); renderNode != null; renderNode = (SpriteRenderNode) this.spriteDrawQueue.getDirectNext()) {
            if (renderNode.hasSoftReference()) {
                renderNode.unlink();
                renderNode.unlinkSub();
                this.currentCapacity += renderNode.size;
            }
        }
    }

    public NativeSprite getNativeSprite(IItemRenderer renderer) {
        long uid = renderer.getUid();
        for (SpriteRenderNode nativeSpriteNode = this.spriteRenderNodes.get(uid); nativeSpriteNode != null; nativeSpriteNode = this.spriteRenderNodes.nextInBucket()) {
            if (nativeSpriteNode.itemRenderer.matches(renderer)) {
                NativeSprite nativeSprite = nativeSpriteNode.createNativeSprite();
                if (nativeSprite != null) {
                    if (nativeSpriteNode.hasSoftReference()) {
                        DirectSpriteRenderNode directRenderNode = new DirectSpriteRenderNode(renderer, nativeSprite, nativeSpriteNode.size);
                        this.spriteRenderNodes.put(directRenderNode, nativeSpriteNode.pointer);
                        this.spriteDrawQueue.insert(directRenderNode);
                        directRenderNode.indexReference = 0L;
                        nativeSpriteNode.unlink();
                        nativeSpriteNode.unlinkSub();
                    } else {
                        this.spriteDrawQueue.insert(nativeSpriteNode);
                        nativeSpriteNode.indexReference = 0L;
                    }
                    return nativeSprite;
                }
                nativeSpriteNode.unlink();
                nativeSpriteNode.unlinkSub();
                this.currentCapacity += nativeSpriteNode.size;
            }
        }
        return null;
    }

}
