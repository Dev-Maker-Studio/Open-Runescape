package com.jagex.game.runetek5.config.struct

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.utils.MathUtils

class StructType : CacheableNode() {

    @JvmField var nodeMap: IterableNodeMap<Node<*>>? = null

    fun decode(buffer: Packet, opcode: Int) {
        if (opcode == 249) {
            val size = buffer.readUByte()
            if (nodeMap == null) {
                val capacity = MathUtils.nextPowerOfTwo(size)
                nodeMap = IterableNodeMap(capacity)
            }

            for (index in 0 until size) {
                val isString = buffer.readUByte() == 1
                val key = buffer.readUMedium()
                val node: Node<*> = if (isString) {
                    ObjectNode(buffer.readString())
                } else {
                    IntNode(buffer.readInt())
                }
                nodeMap!!.put(node, key.toLong())
            }
        }
    }

    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    fun getInteger(id: Int, fallback: Int): Int {
        if (nodeMap == null) {
            return fallback
        }
        val intNode = nodeMap!!.get(id.toLong()) as IntNode?
        return intNode?.value ?: fallback
    }

    fun getString(id: Int, fallback: String): String {
        if (nodeMap == null) {
            return fallback
        }
        val objectNode = nodeMap!!.get(id.toLong()) as ObjectNode?
        return objectNode?.value as String? ?: fallback
    }
}
