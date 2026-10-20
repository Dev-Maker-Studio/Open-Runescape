package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList

class CutsceneRotateEntity(buffer: Packet) : CutsceneAction(buffer) {

    @JvmField
    val cutsceneEntityPtr: Int = buffer.readUShort()

    @JvmField
    val rotation: Int = buffer.readUShort()

    override fun perform() {
        CutsceneTypeList.CUTSCENE_ENTITIES!![cutsceneEntityPtr].getEntity().turn(rotation, true)
    }
}
