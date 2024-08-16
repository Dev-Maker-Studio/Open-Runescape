package com.jagex.game.runetek5.cache.node.lru;

import com.jagex.game.runetek5.cache.node.Node;

public class CacheableNode extends Node {

    public long indexReference;
    public CacheableNode next;
    public CacheableNode current;

    public void unlinkSub() {
        if (this.current != null) {
            this.current.next = this.next;
            this.next.current = this.current;
            this.next = null;
            this.current = null;
        }
    }

	public static void rebuild(CacheableNode currentNode, CacheableNode nextNode) {
	    if (currentNode.current != null) {
	        currentNode.unlinkSub();
	    }
	    currentNode.current = nextNode;
	    currentNode.next = nextNode.next;
	    currentNode.current.next = currentNode;
	    currentNode.next.current = currentNode;
	}
}
