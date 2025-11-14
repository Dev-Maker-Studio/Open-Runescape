package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

/**
 * @author Brainpower - 06/04/2022
 */
class CutsceneDestroyObject(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField
    val cutsceneObjectPtr: Int = buffer.readUShort()

    override fun perform() {
        CutsceneTypeList.CUTSCENE_OBJECTS[cutsceneObjectPtr].destroy()
    }
}
