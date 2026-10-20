package com.jagex.game.runetek5.trigger.rsinterface

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.trigger.ClientTriggerType

class IFSubActiveNpc(
    subInterfaceId: Int,
    overlay: Int,
    @JvmField val npcIndex: Int
) : SubInterface(subInterfaceId, overlay) {

    override fun executeTrigger(): Boolean {
        val objectNode: ObjectNode? = Client.NPC_MAP.get(this.npcIndex.toLong())
        if (objectNode != null) {
            CS2Executor.executeIfTrigger(ClientTriggerType.IFSUB_SETACTIVENPC, super.subInterfaceId, objectNode.value as PathingEntity, this.npcIndex)
            return true
        }
        return false
    }
}
