package com.jagex.game.runetek5.entity.pathingentity

import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.hitbar.HitbarType
import com.jagex.game.runetek5.entity.EntityNode
import com.jagex.game.runetek5.entity.EntityNodeList

/**
 * Manages hitbar display for entities with timed updates.
 *
 * @author Brainpower - 01/05/2022
 */
class EntityHitbar(
    @JvmField val hitbarType: HitbarType
) : EntityNode() {

    @JvmField val entityList = EntityNodeList()

    /**
     * Retrieves the current hitbar display details for the given client cycle.
     *
     * @param clientCycle The current client cycle
     * @return The hitbar display details, or null if none are active
     */
    fun method12600(clientCycle: Int): EntityHitbarDisplayDetails? {
        var details = entityList.getFirst() as EntityHitbarDisplayDetails?
        if (details != null && details.discardAtCycle <= clientCycle) {
            var nextDetails = entityList.getNext() as EntityHitbarDisplayDetails?
            while (nextDetails != null && nextDetails.discardAtCycle <= clientCycle) {
                details!!.unlinkNode()
                details = nextDetails
                nextDetails = entityList.getNext() as EntityHitbarDisplayDetails?
            }

            if (hitbarType.anInt2443 + details!!.discardAtCycle + details.lifetimeCycles > clientCycle) {
                return details
            }
            details.unlinkNode()
            return null
        }
        return null
    }

    /**
     * Updates or adds a hitbar display at the specified cycle.
     *
     * @param discardCycle The cycle at which this hitbar update occurs
     * @param fromPercentage The starting percentage
     * @param toPercentage The ending percentage
     * @param lifetimeCycles The number of cycles this hitbar lasts
     */
    fun updateHitbar(discardCycle: Int, fromPercentage: Int, toPercentage: Int, lifetimeCycles: Int) {
        var adjustedHitbar: EntityHitbarDisplayDetails? = null
        var hitbarCount = 0

        var hitbarDisplay = entityList.getFirst() as EntityHitbarDisplayDetails?
        while (hitbarDisplay != null) {
            hitbarCount++
            if (hitbarDisplay.discardAtCycle == discardCycle) {
                hitbarDisplay.set(discardCycle, fromPercentage, toPercentage, lifetimeCycles)
                return
            }

            if (hitbarDisplay.discardAtCycle <= discardCycle) {
                adjustedHitbar = hitbarDisplay
            }
            hitbarDisplay = entityList.getNext() as EntityHitbarDisplayDetails?
        }

        if (adjustedHitbar == null) {
            if (hitbarCount < client.ENTITY_DEFAULTS.anInt7030) {
                entityList.getPrevious(EntityHitbarDisplayDetails(discardCycle, fromPercentage, toPercentage, lifetimeCycles))
            }
        } else {
            EntityNode.replace(EntityHitbarDisplayDetails(discardCycle, fromPercentage, toPercentage, lifetimeCycles), adjustedHitbar)
            if (hitbarCount >= client.ENTITY_DEFAULTS.anInt7030) {
                entityList.getFirst()!!.unlinkNode()
            }
        }
    }

    /**
     * Checks if the hitbar list is empty.
     *
     * @return True if there are no active hitbars
     */
    fun method12602(): Boolean {
        return entityList.isEmpty()
    }
}
