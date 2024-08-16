package com.jagex.game.runetek5.vardomain;

import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.varn.VarnBitType;
import com.jagex.game.runetek5.config.varn.VarnType;

/**
 * 
 * @author Brainpower - 25/03/2023
 * 
 * Manages npc variables
 *
 */
public class VarnCache {

    public final int cacheSize;
    public IterableNodeMap<IntNode> cache;

    public VarnCache() {
        this.cacheSize = 32;
    }

    public void clear() {
        if (this.cache != null) {
            this.cache.clear();
        }
    }

    public int getVarn(int varnId) {
        IntNode intnode = this.cache.get(varnId);
        if (intnode != null) {
            return intnode.value;
        } 
        VarnType varnType = TypeLists.VARN.list(varnId);
        return varnType.character != 105 ? -1 : 0;
    }

    public int getVarnBit(int varnbitId) {
        VarnBitType varnBitType = TypeLists.VARNBIT.list(varnbitId);
        int varnId = varnBitType.varnId;
        int mask = varnBitType.endBit == 31 ? -1 : (1 << varnBitType.endBit + 1) - 1;
        return (getVarn(varnId) & mask) >>> varnBitType.startBit;
    }

    public void setVarn(int id, int value) {
        if (this.cache == null) {
            this.cache = new IterableNodeMap<IntNode>(this.cacheSize);
        }
        IntNode intnode = this.cache.get(id);
        if (intnode != null) {
            intnode.value = value;
        } else {
            intnode = new IntNode(value);
            this.cache.put(intnode, id);
        }
    }
}
