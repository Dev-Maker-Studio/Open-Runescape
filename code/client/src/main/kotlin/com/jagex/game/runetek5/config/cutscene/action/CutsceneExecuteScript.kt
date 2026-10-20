package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor

class CutsceneExecuteScript(buffer: Packet) : CutsceneAction(buffer) {
    @JvmField val scriptStringParam: String = buffer.readString()
    @JvmField val scriptIntParam: Int = buffer.readUShort()

    override fun perform() {
        if (Client.CURRENT_CUTSCENE != -1) {
            CS2Executor.executeCutsceneScript(Client.CURRENT_CUTSCENE, this.scriptStringParam, this.scriptIntParam)
        }
    }
}
