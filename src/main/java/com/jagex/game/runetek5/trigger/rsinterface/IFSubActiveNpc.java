package com.jagex.game.runetek5.trigger.rsinterface;

import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.trigger.ClientTriggerType;

/**
 * 
 * @author Steven - 06/10/2023
 *
 */
public class IFSubActiveNpc extends SubInterface {

    public final int npcIndex;

    public IFSubActiveNpc(int subInterfaceId, int overlay, int npcIndex) {
        super(subInterfaceId, overlay);
        this.npcIndex = npcIndex;
    }

    @Override
    public boolean executeTrigger() {
        ObjectNode objectNode = client.NPC_MAP.get(this.npcIndex);
        if (objectNode != null) {
            CS2Executor.executeIfTrigger(ClientTriggerType.IFSUB_SETACTIVENPC, super.subInterfaceId, (PathingEntity) objectNode.value, this.npcIndex);
            return true;
        } 
        return false;     
    }

}
