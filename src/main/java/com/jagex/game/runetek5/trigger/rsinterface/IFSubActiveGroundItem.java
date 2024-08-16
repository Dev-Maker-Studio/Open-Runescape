package com.jagex.game.runetek5.trigger.rsinterface;

import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.entity.grounditem.GroundItem;
import com.jagex.game.runetek5.scene.GroundItemFetcher;
import com.jagex.game.runetek5.trigger.ClientTriggerType;

/**
 * 
 * @author Steven - 06/10/2023
 *
 */
public class IFSubActiveGroundItem extends SubInterface {

    public final GroundItemFetcher grounditemFetcher;

    public IFSubActiveGroundItem(int interfaceId, int overlay, GroundItemFetcher grounditemFetcher) {
        super(interfaceId, overlay);
        this.grounditemFetcher = grounditemFetcher;
    }

    @Override
    public boolean executeTrigger() {
        GroundItem groundItem = this.grounditemFetcher.getGroundItem();
        if (groundItem != null) {
            CS2Executor.executeIfOnGitem(ClientTriggerType.IF_SETACTIVEGROUNDITEM, super.subInterfaceId, groundItem);
            return true;
        } 
        return false;       
    }
}
