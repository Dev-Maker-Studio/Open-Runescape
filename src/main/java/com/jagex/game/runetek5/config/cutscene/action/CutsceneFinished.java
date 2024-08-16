package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.network.packet.PacketEncoder;

/**
 * 
 * @author Steven - 24/09/2023
 *
 */
public class CutsceneFinished extends CutsceneAction {

    public CutsceneFinished(Packet buffer) {
        super(buffer);
    }

    @Override
    public void perform() {
        PacketEncoder.queueCutsceneExit(true);
    }
}
