package com.jagex.game.runetek5.client.periodic;

import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.utils.Utils;

public class PulseEvent extends CacheableNode {

    public String string;
    public int secondary;
    public int tertiary;
    public int primary;
	
    public PulseEvent(int type, long data) {
        this.pointer = (long) type << 56 | data;
    }

    public int getType() {
        return (int) (super.pointer >>> 56 & 0xffL);
    }

    public long executeAfter() {
        return super.indexReference & 0x7fffffffffffffffL;
    }

    public long getValue() {
        return super.pointer & 0xffffffffffffffL;
    }
    
    public void queueLowPriority() {
        super.indexReference = super.indexReference & -9223372036854775808L | Utils.time() + 500L;
        PulseEventHandler.LOW_PRIORITY.insert(this);
    }

    public void queueHighPriority() {
        super.indexReference |= -9223372036854775808L;
        if (executeAfter() == 0L) {
            PulseEventHandler.HIGH_PRIORITY.insert(this);
        }
    }
}
