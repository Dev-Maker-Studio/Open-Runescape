package com.jagex.game.runetek5.entity.pathingentity.player;

import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.varp.VarpType;
import com.jagex.game.runetek5.config.varp.VarpbitType;

public class ConfigNodeMap {

    public IterableNodeMap<IntNode> configs;

    public final int capacity;

    public ConfigNodeMap() {
        this.capacity = 16;
    }

    public void clear() {
        if (this.configs != null) {
            this.configs.clear();
        }
    }

    public void put(int key, int value) {
        if (this.configs == null) {
            this.configs = new IterableNodeMap<IntNode>(this.capacity);
        }
        IntNode intNode = this.configs.get(key);
        if (intNode != null) {
            intNode.value = value;
        } else {
            intNode = new IntNode(value);
            this.configs.put(intNode, key);
        }
    }

    public int getVarp(int varId) {
        IntNode intNode = this.configs.get(varId);
        if (intNode != null) {
            return intNode.value;
        } 
        VarpType varType = TypeLists.VAR.list(varId);
        return varType.aChar4328 != 105 ? -1 : 0;
    }

    public int getVarbit(int varbitId) {
        VarpbitType varbitType = TypeLists.VARPBIT.list(varbitId);
        int configIndex = varbitType.configIndex;
        int mask = varbitType.endBit == 31 ? -1 : (1 << varbitType.endBit + 1) - 1;
        return (getVarp(configIndex) & mask) >>> varbitType.startBit;
    }
}
