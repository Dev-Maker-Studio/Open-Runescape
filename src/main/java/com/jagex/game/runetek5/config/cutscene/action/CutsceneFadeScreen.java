package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;

public class CutsceneFadeScreen extends CutsceneAction {

    public final int fadeDurationCycles;
    public final int fadeScreenColor;

    public CutsceneFadeScreen(Packet buffer) {
        super(buffer);
        this.fadeDurationCycles = buffer.readUnsignedShort();
        this.fadeScreenColor = buffer.readInt();
    }

    @Override
    public void perform() {
        InterfaceComponentRenderer.cycleCutsceneFadeStarted = client.cycles;
        InterfaceComponentRenderer.cutsceneFadeEndCycle = this.fadeDurationCycles + client.cycles;
        InterfaceComponentRenderer.prevCutsceneFadeAlpha = InterfaceComponentRenderer.cutsceneFadeAlpha;
        InterfaceComponentRenderer.prevCutsceneFadeRed = InterfaceComponentRenderer.cutsceneFadeRed;
        InterfaceComponentRenderer.prevCutsceneFadeGreen = InterfaceComponentRenderer.cutsceneFadeGreen;
        InterfaceComponentRenderer.prevCutsceneFadeBlue = InterfaceComponentRenderer.cutsceneFadeBlue;
        InterfaceComponentRenderer.cutsceneFadeAlpha = this.fadeScreenColor >>> 24;
        InterfaceComponentRenderer.cutsceneFadeRed = this.fadeScreenColor >>> 16 & 0xff;
        InterfaceComponentRenderer.cutsceneFadeGreen = this.fadeScreenColor >>> 8 & 0xff;
        InterfaceComponentRenderer.cutsceneFadeBlue = this.fadeScreenColor & 0xff;
    }
}