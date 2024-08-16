package com.jagex.game.runetek5.entity.pathingentity;

import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.hitbar.HitbarType;
import com.jagex.game.runetek5.entity.EntityNode;
import com.jagex.game.runetek5.entity.EntityNodeList;

/**
 * 
 * @author Brainpower - 01/05/2022
 *
 */
public class EntityHitbar extends EntityNode {

    public final HitbarType hitbarType;
    public final EntityNodeList entityList = new EntityNodeList();

    public EntityHitbar(HitbarType hitbarType) {
        this.hitbarType = hitbarType;
    }

    public EntityHitbarDisplayDetails method12600(int clientCycle) {
        EntityHitbarDisplayDetails details = (EntityHitbarDisplayDetails) this.entityList.getFirst();
        if (details != null && details.discardAtCycle <= clientCycle) {
            for (EntityHitbarDisplayDetails class275Sub34 = (EntityHitbarDisplayDetails) this.entityList.getNext(); class275Sub34 != null && class275Sub34.discardAtCycle <= clientCycle; class275Sub34 = (EntityHitbarDisplayDetails) this.entityList.getNext()) {
                details.unlinkNode();
                details = class275Sub34;
            }

            if (this.hitbarType.anInt2443 + details.discardAtCycle + details.lifetimeCycles > clientCycle) {
                return details;
            } 
            details.unlinkNode();
            return null;
        } 
        return null;
    }

    public void updateHitbar(int discardCycle, int fromPercentage, int toPercentage, int lifetimeCycles) {
        EntityHitbarDisplayDetails adjustedHitbar = null;
        int hitbarCount = 0;

        for (EntityHitbarDisplayDetails hitbarDisplay = (EntityHitbarDisplayDetails) this.entityList.getFirst(); hitbarDisplay != null; hitbarDisplay = (EntityHitbarDisplayDetails) this.entityList.getNext()) {
            ++hitbarCount;
            if (hitbarDisplay.discardAtCycle == discardCycle) {
                hitbarDisplay.set(discardCycle, fromPercentage, toPercentage, lifetimeCycles);
                return;
            }

            if (hitbarDisplay.discardAtCycle <= discardCycle) {
                adjustedHitbar = hitbarDisplay;
            }
        }

        if (adjustedHitbar == null) {
            if (hitbarCount < client.ENTITY_DEFAULTS.anInt7030) {
                this.entityList.getPrevious(new EntityHitbarDisplayDetails(discardCycle, fromPercentage, toPercentage, lifetimeCycles));
            }
        } else {
            EntityNode.replace(new EntityHitbarDisplayDetails(discardCycle, fromPercentage, toPercentage, lifetimeCycles), adjustedHitbar);
            if (hitbarCount >= client.ENTITY_DEFAULTS.anInt7030) {
                this.entityList.getFirst().unlinkNode();
            }
        }

    }

    public boolean method12602() {
        return this.entityList.isEmpty();
    }

}
