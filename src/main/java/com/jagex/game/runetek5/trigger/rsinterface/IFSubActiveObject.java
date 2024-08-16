package com.jagex.game.runetek5.trigger.rsinterface;

import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.trigger.ClientTriggerType;

/**
 * 
 * @author Steven - 06/10/2023
 *
 */
public class IFSubActiveObject extends SubInterface {

    public final IFSubObjectPosition objectPosition;

    public IFSubActiveObject(int subInterfaceId, int overlay, IFSubObjectPosition objectPosition) {
        super(subInterfaceId, overlay);
        this.objectPosition = objectPosition;
    }

    @Override
    public boolean executeTrigger() {
        GameObject gameObject = this.objectPosition.getGameobjectVerifyId();
        if (gameObject != null) {
            CS2Executor.ifObjectTrigger(ClientTriggerType.IF_SETACTIVEOBJECT, super.subInterfaceId, gameObject);
            return true;
        }
        return false;
    }

}
