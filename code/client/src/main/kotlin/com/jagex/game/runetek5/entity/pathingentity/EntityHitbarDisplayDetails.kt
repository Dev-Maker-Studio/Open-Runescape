package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.runetek5.entity.EntityNode

class EntityHitbarDisplayDetails(
    discardAtCycle: Int,
    fromPercentage: Int,
    toPercentage: Int,
    lifetimeCycles: Int
) : EntityNode() {

    @JvmField var discardAtCycle: Int = discardAtCycle
    @JvmField var fromPercentage: Int = fromPercentage
    @JvmField var toPercentage: Int = toPercentage
    @JvmField var lifetimeCycles: Int = lifetimeCycles

    fun set(discardAtCycle: Int, fromPercentage: Int, toPercentage: Int, lifetimeCycles: Int) {
        this.discardAtCycle = discardAtCycle
        this.fromPercentage = fromPercentage
        this.toPercentage = toPercentage
        this.lifetimeCycles = lifetimeCycles
    }
}
