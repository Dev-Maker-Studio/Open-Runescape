package com.jagex.game.runetek5.trigger.rsinterface

import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.trigger.ClientTriggerType

class IFSubActiveObject(
    subInterfaceId: Int,
    overlay: Int,
    @JvmField val objectPosition: IFSubObjectPosition
) : SubInterface(subInterfaceId, overlay) {

    override fun executeTrigger(): Boolean {
        val gameObject: GameObject? = this.objectPosition.getGameobjectVerifyId()
        if (gameObject != null) {
            CS2Executor.ifObjectTrigger(ClientTriggerType.IF_SETACTIVEOBJECT, super.subInterfaceId, gameObject)
            return true
        }
        return false
    }
}
