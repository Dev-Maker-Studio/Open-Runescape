package com.jagex.game.runetek5.config.rsinterface.itemcontainer;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;

/**
 * 
 * @author Brainpower - 06/05/2022
 *
 */
public class ItemContainerType extends CacheableNode {

    public int defaultItemSize;

    public int maxItemSize;

    public int[] itemIds;

    public int[] amounts;

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }
            decode(buffer, opcode);
        }
    }

    public void decode(Packet buffer, int opcode) {
        if (opcode == 2) {
        	this.maxItemSize = buffer.readUShort();
        } else if (opcode == 4) {
            this.defaultItemSize = buffer.readUByte();
            this.itemIds = new int[this.defaultItemSize];
            this.amounts = new int[this.defaultItemSize];
            for (int index = 0; index < this.defaultItemSize; index++) {
            	this.itemIds[index] = buffer.readUShort();
            	this.amounts[index] = buffer.readUShort();
            }
        }
    }
}