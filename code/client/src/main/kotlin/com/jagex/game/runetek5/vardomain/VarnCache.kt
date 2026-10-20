package com.jagex.game.runetek5.vardomain

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.varn.VarnBitType
import com.jagex.game.runetek5.config.varn.VarnType

class VarnCache {

    @JvmField val cacheSize: Int = 32
    @JvmField var cache: IterableNodeMap<IntNode>? = null

    fun clear() {
        cache?.clear()
    }

    fun getVarn(varnId: Int): Int {
        val intNode = cache?.get(varnId.toLong())
        if (intNode != null) {
            return intNode.value
        }

        val varnType = TypeLists.VARN!!.list(varnId)
        return if (varnType.typeChar.code != 105) -1 else 0
    }

    fun getVarnBit(varnbitId: Int): Int {
        val varnBitType = TypeLists.VARNBIT!!.list(varnbitId)
        val varnId = varnBitType.varnId

        val mask = if (varnBitType.endBit == 31) {
            -1
        } else {
            (1 shl (varnBitType.endBit + 1)) - 1
        }

        return (getVarn(varnId) and mask) ushr varnBitType.startBit
    }

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
