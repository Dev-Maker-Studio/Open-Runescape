package com.jagex.game.runetek5.trigger.rsinterface

import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.entity.grounditem.GroundItem
import com.jagex.game.runetek5.scene.GroundItemFetcher
import com.jagex.game.runetek5.trigger.ClientTriggerType

/**
 *
 * @author Steven - 06/10/2023
 *
 */
class IFSubActiveGroundItem(
    interfaceId: Int,
    overlay: Int,
    @JvmField val grounditemFetcher: GroundItemFetcher
) : SubInterface(interfaceId, overlay) {

    override fun executeTrigger(): Boolean {
        val groundItem: GroundItem? = this.grounditemFetcher.getGroundItem()
        if (groundItem != null) {
            CS2Executor.executeIfOnGitem(ClientTriggerType.IF_SETACTIVEGROUNDITEM, super.subInterfaceId, groundItem)
            return true
        }
        return false
    }
}
