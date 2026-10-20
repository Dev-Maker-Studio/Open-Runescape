package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneSetHintDetails(buffer: Packet) : CutsceneAction(buffer) {
    @JvmField val entityIndex: Int = buffer.readUShort()
    @JvmField val text: String = buffer.readString()
    @JvmField val colorType: Int = buffer.readInt()
    @JvmField val duration: Int = buffer.readUShort()

    override fun perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES!![this.entityIndex].getEntity().setHintDetails(this.text, this.colorType, 0, this.duration)
    }
}
