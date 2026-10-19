package com.jagex.game.runetek5.config.cutscene.action

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer

class CutsceneFadeScreen(buffer: Packet) : CutsceneAction(buffer) {
    @JvmField val fadeDurationCycles: Int = buffer.readUShort()
    @JvmField val fadeScreenColor: Int = buffer.readInt()

    override fun perform() {
        InterfaceComponentRenderer.cycleCutsceneFadeStarted = Client.cycles
        InterfaceComponentRenderer.cutsceneFadeEndCycle = this.fadeDurationCycles + Client.cycles
        InterfaceComponentRenderer.prevCutsceneFadeAlpha = InterfaceComponentRenderer.cutsceneFadeAlpha
        InterfaceComponentRenderer.prevCutsceneFadeRed = InterfaceComponentRenderer.cutsceneFadeRed
        InterfaceComponentRenderer.prevCutsceneFadeGreen = InterfaceComponentRenderer.cutsceneFadeGreen
        InterfaceComponentRenderer.prevCutsceneFadeBlue = InterfaceComponentRenderer.cutsceneFadeBlue
        InterfaceComponentRenderer.cutsceneFadeAlpha = this.fadeScreenColor ushr 24
        InterfaceComponentRenderer.cutsceneFadeRed = this.fadeScreenColor ushr 16 and 0xff
        InterfaceComponentRenderer.cutsceneFadeGreen = this.fadeScreenColor ushr 8 and 0xff
        InterfaceComponentRenderer.cutsceneFadeBlue = this.fadeScreenColor and 0xff
    }
}
