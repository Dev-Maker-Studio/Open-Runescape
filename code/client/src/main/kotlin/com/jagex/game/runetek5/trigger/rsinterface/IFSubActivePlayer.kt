package com.jagex.game.runetek5.trigger.rsinterface

import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.trigger.ClientTriggerType

class IFSubActivePlayer(
    subInterfaceId: Int,
    overlay: Int,
    @JvmField val playerIndex: Int
) : SubInterface(subInterfaceId, overlay) {

    override fun executeTrigger(): Boolean {
        val player: PlayerEntity? = PlayerUpdate.players[this.playerIndex]
        if (player != null) {
            CS2Executor.executeIfTrigger(ClientTriggerType.IFSUB_SETACTIVEPLAYER, super.subInterfaceId, player, this.playerIndex)
            return true
        }
        return false
    }
}
