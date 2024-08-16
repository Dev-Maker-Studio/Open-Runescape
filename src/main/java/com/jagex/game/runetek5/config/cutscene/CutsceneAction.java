package com.jagex.game.runetek5.config.cutscene;

import com.jagex.game.runetek5.buffer.Packet;

public abstract class CutsceneAction {

    public final int lengthInCycles;
    
	protected CutsceneAction(Packet buffer) {
        this.lengthInCycles = buffer.readUnsignedShort();
    }

    public abstract void perform();

    public void method1593() {
    }

    public boolean isFullyCached() {
        return true;
    }
}
