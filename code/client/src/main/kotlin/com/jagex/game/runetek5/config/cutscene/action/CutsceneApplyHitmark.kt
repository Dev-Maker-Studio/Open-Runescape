package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneApplyHitmark(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField val entityIndex: Int

    @JvmField val hitsplatId: Int

    @JvmField val hitText: Int

    @JvmField val soakHitsplatId: Int

    @JvmField val soakText: Int

    init {
        entityIndex = buffer.readUShort()
        val flags = buffer.readUByte()

        if ((flags and 0x1) != 0) {
            hitsplatId = buffer.readUShort()
            hitText = buffer.readUShort()
        } else {
            hitsplatId = -1
            hitText = -1
        }

        if ((flags and 0x2) != 0) {
            soakHitsplatId = buffer.readUShort()
            soakText = buffer.readUShort()
        } else {
            soakHitsplatId = -1
            soakText = -1
        }

        if ((flags and 0x4) != 0) {
            val currentHealth = buffer.readUShort()
            val maxHealth = buffer.readUShort()
            val healthPercent = currentHealth * 255 / maxHealth

            @Suppress("UNUSED_VARIABLE")
            val shouldDisplayLowHealth = currentHealth > 0 && healthPercent < 1
        }
    }

    override fun perform() {
        val entity = CutsceneTypeList.CUTSCENE_ENTITIES!![entityIndex].getEntity()
        entity.applyHit(hitsplatId, hitText, null, soakHitsplatId, soakText, null, Client.cycles, 0)
    }
}
