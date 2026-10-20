package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneSetVariable(buffer: Packet, isVarbit: Boolean) : CutsceneAction(buffer) {

    @JvmField val key: Long
    @JvmField val value: Int

    init {
        val mask = buffer.readUShort()
        this.key = if (isVarbit) {
            0x100000000L or mask.toLong()
        } else {
            mask.toLong()
        }
        this.value = buffer.readInt()
    }

    override fun perform() {
        val intnode = CutsceneTypeList.CONFIG_CUTSCENES.get(this.key)
        if (intnode != null) {
            intnode.value = this.value
        } else {
            CutsceneTypeList.CONFIG_CUTSCENES.put(IntNode(this.value), this.key)
        }
    }
}
