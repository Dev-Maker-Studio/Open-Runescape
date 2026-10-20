package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.hitbar.HitbarType
import com.jagex.game.runetek5.entity.EntityNode
import com.jagex.game.runetek5.entity.EntityNodeList

class EntityHitbar(
    @JvmField val hitbarType: HitbarType
) : EntityNode() {

    @JvmField val entityList = EntityNodeList<EntityHitbarDisplayDetails>()

    fun getActiveDisplay(clientCycle: Int): EntityHitbarDisplayDetails? {
        var details = entityList.getFirst()
        if (details != null && details.discardAtCycle <= clientCycle) {
            var nextDetails = entityList.getNext()
            while (nextDetails != null && nextDetails.discardAtCycle <= clientCycle) {
                details!!.unlinkNode()
                details = nextDetails
                nextDetails = entityList.getNext()
            }

            if (hitbarType.fadeOutDuration + details.discardAtCycle + details.lifetimeCycles > clientCycle) {
                return details
            }
            details.unlinkNode()
            return null
        }
        return null
    }

    fun updateHitbar(discardCycle: Int, fromPercentage: Int, toPercentage: Int, lifetimeCycles: Int) {
        var adjustedHitbar: EntityHitbarDisplayDetails? = null
        var hitbarCount = 0

        var hitbarDisplay = entityList.getFirst()
        while (hitbarDisplay != null) {
            hitbarCount++
            if (hitbarDisplay.discardAtCycle == discardCycle) {
                hitbarDisplay.set(discardCycle, fromPercentage, toPercentage, lifetimeCycles)
                return
            }

            if (hitbarDisplay.discardAtCycle <= discardCycle) {
                adjustedHitbar = hitbarDisplay
            }
            hitbarDisplay = entityList.getNext()
        }

        if (adjustedHitbar == null) {
            if (hitbarCount < Client.ENTITY_DEFAULTS!!.maxVisibleHitbars) {
                entityList.getPrevious(EntityHitbarDisplayDetails(discardCycle, fromPercentage, toPercentage, lifetimeCycles))
            }
        } else {
            replace(EntityHitbarDisplayDetails(discardCycle, fromPercentage, toPercentage, lifetimeCycles), adjustedHitbar)
            if (hitbarCount >= Client.ENTITY_DEFAULTS!!.maxVisibleHitbars) {
                entityList.getFirst()!!.unlinkNode()
            }
        }
    }

    fun isEmpty(): Boolean {
        return entityList.isEmpty()
    }
}
