package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

/**
 * Cutscene action that applies hit markers (damage numbers) to an entity.
 *
 * @author Steven - 17/08/2023
 */
class CutsceneApplyHitmark(buffer: Packet) : CutsceneAction(buffer) {

    /** Index of the entity to apply hitmarks to */
    @JvmField val entityIndex: Int

    /** Primary hitsplat sprite ID */
    @JvmField val hitsplatId: Int

    /** Primary hitsplat text (damage amount) */
    @JvmField val hitText: Int

    /** Secondary/soak hitsplat sprite ID */
    @JvmField val soakHitsplatId: Int

    /** Secondary/soak hitsplat text (damage amount) */
    @JvmField val soakText: Int

    init {
        entityIndex = buffer.readUShort()
        val flags = buffer.readUByte()

        // Read primary hitsplat if flag is set
        if ((flags and 0x1) != 0) {
            hitsplatId = buffer.readUShort()
            hitText = buffer.readUShort()
        } else {
            hitsplatId = -1
            hitText = -1
        }

        // Read soak hitsplat if flag is set
        if ((flags and 0x2) != 0) {
            soakHitsplatId = buffer.readUShort()
            soakText = buffer.readUShort()
        } else {
            soakHitsplatId = -1
            soakText = -1
        }

        // Read health bar information if flag is set (unused in current implementation)
        if ((flags and 0x4) != 0) {
            val currentHealth = buffer.readUShort()
            val maxHealth = buffer.readUShort()
            val healthPercent = currentHealth * 255 / maxHealth

            // This appears to be a debugging check for low health display
            @Suppress("UNUSED_VARIABLE")
            val shouldDisplayLowHealth = currentHealth > 0 && healthPercent < 1
        }
    }

    override fun perform() {
        val entity = CutsceneTypeList.CUTSCENE_ENTITIES[entityIndex].getEntity()
        entity.applyHit(hitsplatId, hitText, soakHitsplatId, soakText, client.cycles, 0)
    }
}
