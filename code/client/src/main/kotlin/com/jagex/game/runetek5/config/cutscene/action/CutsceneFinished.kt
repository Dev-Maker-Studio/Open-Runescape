package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.network.packet.PacketEncoder

class CutsceneFinished(buffer: Packet) : CutsceneAction(buffer) {
    override fun perform() {
        PacketEncoder.queueCutsceneExit(true)
    }
}
