package com.jagex.game.runetek5.entity.pathingentity;

import com.jagex.game.runetek5.entity.EntityNode;

/**
 * 
 * @author Brainpower - 14/01/2022
 *
 */
public class EntityHitbarDisplayDetails extends EntityNode {

    public int discardAtCycle;
    public int fromPercentage;
    public int toPercentage;
    public int lifetimeCycles;

    public EntityHitbarDisplayDetails(int discardAtCycle, int fromPercentage, int toPercentage, int lifetimeCycles) {
        this.discardAtCycle = discardAtCycle;
        this.fromPercentage = fromPercentage;
        this.toPercentage = toPercentage;
        this.lifetimeCycles = lifetimeCycles;
    }

    public void set(int discardAtCycle, int fromPercentage, int toPercentage, int lifetimeCycles) {
    	this.discardAtCycle = discardAtCycle;
    	this.fromPercentage = fromPercentage;
    	this.toPercentage = toPercentage;
    	this.lifetimeCycles = lifetimeCycles;
    }

}
