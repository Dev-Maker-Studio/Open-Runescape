package com.jagex.game.runetek5.config.struct

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.utils.MathUtils

/**
 * A struct type that stores key-value pairs (integers and strings).
 */
class StructType : CacheableNode() {

    @JvmField var nodeMap: IterableNodeMap<Node<*>>? = null

    /**
     * Decodes a single opcode from the packet buffer.
     *
     * @param buffer The packet to decode from
     * @param opcode The opcode to process
     */
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

    /**
     * Decodes the struct type from a packet buffer.
     *
     * @param buffer The packet to decode from
     */
    fun decode(buffer: Packet) {
        while (true) {
            val opcode = buffer.readUByte()
            if (opcode == 0) {
                return
            }
            decode(buffer, opcode)
        }
    }

    /**
     * Retrieves an integer value by key.
     *
     * @param id The key
     * @param fallback The fallback value if key not found
     * @return The integer value or fallback
     */
    fun getInteger(id: Int, fallback: Int): Int {
        if (nodeMap == null) {
            return fallback
        }
        val intNode = nodeMap!!.get(id.toLong()) as IntNode?
        return intNode?.value ?: fallback
    }

    /**
     * Retrieves a string value by key.
     *
     * @param id The key
     * @param fallback The fallback value if key not found
     * @return The string value or fallback
     */
    fun getString(id: Int, fallback: String): String {
        if (nodeMap == null) {
            return fallback
        }
        val objectNode = nodeMap!!.get(id.toLong()) as ObjectNode?
        return objectNode?.value as String? ?: fallback
    }
}
