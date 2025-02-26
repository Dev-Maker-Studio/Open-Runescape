package com.jagex.game.runetek5.config.struct;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.utils.MathUtils;

public class StructType extends CacheableNode {

    public IterableNodeMap<Node<?>> nodeMap;

    public void decode(Packet buffer, int opcode) {
        if (opcode == 249) {
            int size = buffer.readUByte();
            if (this.nodeMap == null) {
                int capacity = MathUtils.nextPowerOfTwo(size);
                this.nodeMap = new IterableNodeMap<Node<?>>(capacity);
            }

            for (int index = 0; index < size; index++) {
                boolean isString = buffer.readUByte() == 1;
                int key = buffer.readUMedium();
                Node<?> node;
                if (isString) {
                    node = new ObjectNode(buffer.readString());
                } else {
                    node = new IntNode(buffer.readInt());
                }

                this.nodeMap.put(node, key);
            }
        }

    }

    void decode(Packet buffer) {
        while (true) {
            int opcode = buffer.readUByte();
            if (opcode == 0) {
                return;
            }

            decode(buffer, opcode);
        }
    }

    public int getInteger(int id, int fallback) {
        if (this.nodeMap == null) {
            return fallback;
        } 
        IntNode intNode = (IntNode) this.nodeMap.get(id);
        return intNode == null ? fallback : intNode.value;
    }

    public String getString(int id, String fallback) {
        if (this.nodeMap == null) {
            return fallback;
        } 
        ObjectNode objectNode = (ObjectNode) this.nodeMap.get(id);
        return objectNode == null ? fallback : (String) objectNode.value;
    }

}
