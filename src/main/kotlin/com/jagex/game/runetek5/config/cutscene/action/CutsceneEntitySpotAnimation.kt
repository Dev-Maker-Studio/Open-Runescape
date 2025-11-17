package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneEntitySpotAnimation(buffer: Packet) : AbstractCutsceneSpotAnimation(buffer) {
    @JvmField val targetIndex: Int = buffer.readUShort()
    @JvmField val spotAnimationIndex: Int = buffer.readUByte()
    @JvmField val adjustmentType: Int = buffer.readUShort()

    override fun perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES[targetIndex].entity.playSpotAnimation(
            gfxId,
            displayHeight shl 16,
            rotation,
            adjustmentType,
            false,
            spotAnimationIndex
        )
    }
}
