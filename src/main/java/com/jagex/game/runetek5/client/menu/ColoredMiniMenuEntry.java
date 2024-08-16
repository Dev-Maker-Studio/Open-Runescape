package com.jagex.game.runetek5.client.menu;

import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;

public class ColoredMiniMenuEntry extends CacheableNode {

	public final String tagStart;
    public final Queue nodeQueue;
    public int queueSize;

    public ColoredMiniMenuEntry(String string_1) {
        this.tagStart = string_1;
        this.nodeQueue = new Queue();
    }

    public int getClickType() {
        return this.nodeQueue.current.next != this.nodeQueue.current ? ((MiniMenuEntry) this.nodeQueue.current.next).clickType : -1;
    }

    public boolean addEntry(MiniMenuEntry baseEntry) {
        boolean bool3 = true;
        baseEntry.unlinkSub();

        for (MiniMenuEntry subEntry = (MiniMenuEntry) this.nodeQueue.getNext(); subEntry != null; bool3 = false) {
            if (MiniMenu.method5334(baseEntry.clickType, subEntry.clickType)) {
                ColoredMiniMenuEntry.push(baseEntry, subEntry);
                ++this.queueSize;
                return !bool3;
            }

            subEntry = (MiniMenuEntry) this.nodeQueue.getDirectNext();
        }

        this.nodeQueue.insert(baseEntry);
        ++this.queueSize;
        return bool3;
    }

    public boolean removeEntry(MiniMenuEntry class282Sub50Sub71) {
        int clickType = getClickType();
        class282Sub50Sub71.unlinkSub();
        --this.queueSize;
        if (this.queueSize == 0) {
            unlink();
            unlinkSub();
            --MiniMenu.anInt170;
            MiniMenu.aClass229164.put(this, class282Sub50Sub71.bitpacked);
            return false;
        } else {
            return clickType != getClickType();
        }
    }

	public static void push(CacheableNode nodeA, CacheableNode nodeB) {
	    if (nodeA.current != null) {
	        nodeA.unlinkSub();
	    }
	    nodeA.current = nodeB.current;
	    nodeA.next = nodeB;
	    nodeA.current.next = nodeA;
	    nodeA.next.current = nodeA;
	}

}
