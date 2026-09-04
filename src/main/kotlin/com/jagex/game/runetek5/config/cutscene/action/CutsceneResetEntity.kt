package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

/**
 * @author Brainpower - 01/02/2023
 */
class CutsceneResetEntity(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField
    val entityIndex: Int = buffer.readUShort()

    override fun perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES!![entityIndex].reset()
    }
}
