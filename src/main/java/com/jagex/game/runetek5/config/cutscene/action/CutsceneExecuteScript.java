package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;

/**
 * 
 * @author Steven - 17/08/2023
 *
 */
public class CutsceneExecuteScript extends CutsceneAction {

    public final String aString9318;

    public final int anInt9317;

    public CutsceneExecuteScript(Packet buffer) {
        super(buffer);
        this.aString9318 = buffer.readString();
        this.anInt9317 = buffer.readUShort();
    }


    @Override
    public void perform() {
        if (client.CURRENT_CUTSCENE != -1) {
            CS2Executor.executeCutsceneScript(client.CURRENT_CUTSCENE, this.aString9318, this.anInt9317);
        }
    }
}
