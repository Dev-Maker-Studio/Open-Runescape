package com.jagex.game.runetek5.trigger.rsinterface;

import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.trigger.ClientTriggerType;

/**
 * 
 * @author Steven - 06/10/2023
 *
 */
public class IFSubActivePlayer extends SubInterface {

    public final int playerIndex;

    public IFSubActivePlayer(int subInterfaceId, int overlay, int playerIndex) {
        super(subInterfaceId, overlay);
        this.playerIndex = playerIndex;
    }

    @Override
    public boolean executeTrigger() {
        PlayerEntity player = PlayerUpdate.players[this.playerIndex];
        if (player != null) {
            CS2Executor.executeIfTrigger(ClientTriggerType.IFSUB_SETACTIVEPLAYER, super.subInterfaceId, player, this.playerIndex);
            return true;
        } 
        return false;
    }

}
