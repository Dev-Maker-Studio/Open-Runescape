package com.jagex.game.runetek5.vardomain

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.varn.VarnBitType
import com.jagex.game.runetek5.config.varn.VarnType

/**
 * Manages NPC variables (varns) and varnbits.
 * Provides a cache for efficiently storing and retrieving variable values.
 *
 * @author Brainpower - 25/03/2023
 */
class VarnCache {

    @JvmField val cacheSize: Int = 32
    @JvmField var cache: IterableNodeMap<IntNode>? = null

    /**
     * Clears all cached variable values
     */
    fun clear() {
        cache?.clear()
    }

    /**
     * Gets the value of a varn (NPC variable)
     * @param varnId The varn ID
     * @return The varn value, or default value if not set
     */
    fun getVarn(varnId: Int): Int {
        val intNode = cache?.get(varnId.toLong())
        if (intNode != null) {
            return intNode.value
        }

        val varnType = TypeLists.VARN.list(varnId)
        return if (varnType.character.code != 105) -1 else 0
    }

    /**
     * Gets the value of a varnbit (bit-packed variable)
     * @param varnbitId The varnbit ID
     * @return The extracted varnbit value
     */
    fun getVarnBit(varnbitId: Int): Int {
        val varnBitType = TypeLists.VARNBIT.list(varnbitId)
        val varnId = varnBitType.varnId

        // Calculate bitmask
        val mask = if (varnBitType.endBit == 31) {
            -1
        } else {
            (1 shl (varnBitType.endBit + 1)) - 1
        }

        // Extract bits from varn value
        return (getVarn(varnId) and mask) ushr varnBitType.startBit
    }

    /**
     * Sets the value of a varn
     * @param id The varn ID
     * @param value The value to set
     */
    fun setVarn(id: Int, value: Int) {
        if (cache == null) {
            cache = IterableNodeMap(cacheSize)
        }

        val intNode = cache!!.get(id.toLong())
        if (intNode != null) {
            intNode.value = value
        } else {
            val newNode = IntNode(value)
            cache!!.put(newNode, id.toLong())
        }
    }
}
