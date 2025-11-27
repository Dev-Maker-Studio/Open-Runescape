package com.jagex.game.runetek5.vardomain

import com.darkan.bot.scripts.ScriptExecutor
import com.darkan.bot.scripts.event.Varp
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.impl.LongNode
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.varp.VarpbitType
import com.jagex.game.runetek5.utils.Utils

/**
 * [J]
 * @author Steven - 22/11/2023
 *
 * [NXT]
[X]jag::game::PlayerVarDomain::GetVarBitValue(jag::shared_ptr<jag::game::VarBitType> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  00000000000A1B60 00000035 00000008 00000000 R . . . . . .
[X]jag::game::PlayerVarDomain::GetVarValue(jag::shared_ptr<jag::game::VarType> const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   .text  0000000000094D50 00000055                   R . . . . . .
[X]jag::game::PlayerVarDomain::Reset(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               .text  0000000000556080 000000FF 00000028 00000000 R . . . . T .
[X]jag::game::PlayerVarDomain::Service(void)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             .text  000000000045C8B0 0000049F 000001A8 00000018 R . . . . T .
[X]jag::game::PlayerVarDomain::SetVarBitValue(jag::shared_ptr<jag::game::VarBitType> const&,int,bool *)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  .text  00000000000AEFD0 00000153 00000098 00000000 R . . . . . .
[X]jag::game::PlayerVarDomain::SetVarBitValueFromServer(jag::shared_ptr<jag::game::VarBitType> const&,int) [clone]                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       .text  000000000045D650 000001A6 000000A8 00000000 R . . . B . .
[X]jag::game::PlayerVarDomain::SetVarValue(jag::shared_ptr<jag::game::VarType> const&,jag::game::VarValue const&)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        .text  000000000045CD50 00000302 00000088 00000000 R . . . . . .
[X]jag::game::PlayerVarDomain::SetVarValueFromServer(jag::shared_ptr<jag::game::VarType> const&,jag::game::VarValue const&) [clone]
 */
class PlayerVarDomain : VarDomain {

    @JvmField var activeVars: IntArray
    @JvmField var updates: IterableNodeMap<LongNode> = IterableNodeMap(128) // TODO: better name?
    @JvmField var pendingVars: IntArray

    init {
        pendingVars = IntArray(TypeLists.VAR.size)
        activeVars = IntArray(TypeLists.VAR.size)
    }

    /**
     * [J]
     */
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

    /**
     * [J]
     */
    override fun getVarBitValue(varbitId: Int): Int {
        val varbitType = TypeLists.VARPBIT.list(varbitId)
        val varId = varbitType.configIndex
        val start = varbitType.startBit
        val end = varbitType.endBit
        val mask = BIT_MASKS[end - start]
        return activeVars[varId] shr start and mask
    }

    /**
     * [J]
     */
    override fun getVarValue(varpId: Int): Int {
        return activeVars[varpId]
    }

    /**
     * [J]
     */
    fun setVarBitValueFromServer(varbitId: Int, variable: Int) {
        val varbit = TypeLists.VARPBIT.list(varbitId)
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

    /**
     * [J]
     */
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

    /**
     * [J]
     */
    fun setVarBitValue(varbitId: Int, variable: Int) {
        val varbitType = TypeLists.VARPBIT.list(varbitId)
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

    /**
     * [J]
     */
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

    /**
     * [J]
     */
    fun reset() {
        for (index in 0 until TypeLists.VAR.size) {
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
