package com.jagex.game.runetek5.entity.pathingentity.player

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.varp.VarpType
import com.jagex.game.runetek5.config.varp.VarpbitType

class ConfigNodeMap {

    @JvmField var configs: IterableNodeMap<IntNode>? = null

    @JvmField val capacity: Int

    constructor() {
        this.capacity = 16
    }

    fun clear() {
        if (this.configs != null) {
            this.configs!!.clear()
        }
    }

    fun put(key: Int, value: Int) {
        if (this.configs == null) {
            this.configs = IterableNodeMap(this.capacity)
        }
        var intNode = this.configs!!.get(key.toLong())
        if (intNode != null) {
            intNode.value = value
        } else {
            intNode = IntNode(value)
            this.configs!!.put(intNode, key.toLong())
        }
    }

    fun getVarp(varId: Int): Int {
        val intNode = this.configs?.get(varId.toLong())
        if (intNode != null) {
            return intNode.value
        }
        val varType = TypeLists.VAR.list(varId)
        return if (varType.aChar4328.code != 105) -1 else 0
    }

    fun getVarbit(varbitId: Int): Int {
        val varbitType = TypeLists.VARPBIT.list(varbitId)
        val configIndex = varbitType.configIndex
        val mask = if (varbitType.endBit == 31) -1 else (1 shl varbitType.endBit + 1) - 1
        return (getVarp(configIndex) and mask) ushr varbitType.startBit
    }
}
