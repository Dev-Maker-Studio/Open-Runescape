package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor

/**
 *
 * @author Steven - 17/08/2023
 *
 */
class CutsceneExecuteScript(buffer: Packet) : CutsceneAction(buffer) {
    @JvmField val aString9318: String = buffer.readString()
    @JvmField val anInt9317: Int = buffer.readUShort()

    override fun perform() {
        if (Client.CURRENT_CUTSCENE != -1) {
            CS2Executor.executeCutsceneScript(Client.CURRENT_CUTSCENE, this.aString9318, this.anInt9317)
        }
    }
}
