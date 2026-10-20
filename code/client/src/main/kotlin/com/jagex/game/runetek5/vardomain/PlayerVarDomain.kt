package com.jagex.game.runetek5.vardomain

import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.event.Varp
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.impl.LongNode
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.varp.VarpbitType
import com.jagex.game.runetek5.utils.Utils

class PlayerVarDomain : VarDomain {

    @JvmField var activeVars: IntArray
    @JvmField var updates: IterableNodeMap<LongNode> = IterableNodeMap(128)
    @JvmField var pendingVars: IntArray

    init {
        pendingVars = IntArray(TypeLists.VAR!!.size)
        activeVars = IntArray(TypeLists.VAR!!.size)
    }

    fun service(startHead: Boolean): Int {
        val currentTime = Utils.time()
        var update: LongNode? = if (startHead) updates.first() else updates.next()
        while (update != null) {
            if (update.bitpacked and 0x3fffffffffffffffL >= currentTime) {
                update = updates.next()
                continue
            }
            if (update.bitpacked and 0x4000000000000000L == 0L) {
                update.unlink()
                update = updates.next()
                continue
            }
            val configIndex = update.pointer.toInt()
            activeVars[configIndex] = pendingVars[configIndex]
            update.unlink()
            return configIndex
        }
        return -1
    }

    override fun getVarBitValue(varbitId: Int): Int {
        val varbitType = TypeLists.VARPBIT!!.list(varbitId)
        val varId = varbitType.configIndex
        val start = varbitType.startBit
        val end = varbitType.endBit
        val mask = BIT_MASKS[end - start]
        return activeVars[varId] shr start and mask
    }

    override fun getVarValue(varpId: Int): Int {
        return activeVars[varpId]
    }

    fun setVarBitValueFromServer(varbitId: Int, variable: Int) {
        val varbit = TypeLists.VARPBIT!!.list(varbitId)
        val configIndex = varbit.configIndex
        val start = varbit.startBit
        val end = varbit.endBit
        var bitmask = BIT_MASKS[end - start]
        var adjustedVariable = variable
        if (adjustedVariable < 0 || adjustedVariable > bitmask) {
            adjustedVariable = 0
        }
        bitmask = bitmask shl start
        setVarValueFromServer(configIndex, pendingVars[configIndex] and bitmask.inv() or (adjustedVariable shl start and bitmask))
    }

    fun setVarValueFromServer(configIndex: Int, varValue: Int) {
        pendingVars[configIndex] = varValue
        val value = updates[configIndex.toLong()]
        if (value != null) {
            if (value.bitpacked != 4611686018427387905L) {
                value.bitpacked = Utils.time() + 500L or 0x4000000000000000L
            }
        } else {
            val newValue = LongNode(4611686018427387905L)
            updates.put(newValue, configIndex.toLong())
        }
    }

    fun setVarBitValue(varbitId: Int, variable: Int) {
        val varbitType = TypeLists.VARPBIT!!.list(varbitId)
        val configIndex = varbitType.configIndex
        val start = varbitType.startBit
        val end = varbitType.endBit
        var bitmask = BIT_MASKS[end - start]
        var adjustedVariable = variable
        if (adjustedVariable < 0 || adjustedVariable > bitmask) {
            adjustedVariable = 0
        }
        bitmask = bitmask shl start
        setVarValue(configIndex, activeVars[configIndex] and bitmask.inv() or (adjustedVariable shl start and bitmask))
    }

    fun setVarValue(configIndex: Int, varValue: Int) {
        ScriptExecutor.pushEvent(Varp(configIndex, activeVars[configIndex], varValue))
        activeVars[configIndex] = varValue
        val value = updates[configIndex.toLong()]
        if (value != null) {
            value.bitpacked = Utils.time() + 500L
        } else {
            val newValue = LongNode(Utils.time() + 500L)
            updates.put(newValue, configIndex.toLong())
        }
    }

    fun reset() {
        for (index in 0 until TypeLists.VAR!!.size) {
            pendingVars[index] = 0
            activeVars[index] = 0
        }
        updates = IterableNodeMap<LongNode>(128)
    }

    companion object {
        @JvmField
        val BIT_MASKS = IntArray(32)

        init {
            var variable = 2
            for (index in 0 until 32) {
                BIT_MASKS[index] = variable - 1
                variable += variable
            }
        }
    }
}
