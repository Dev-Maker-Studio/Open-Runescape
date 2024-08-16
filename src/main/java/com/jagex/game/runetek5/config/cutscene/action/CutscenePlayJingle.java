package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;

/**
 * 
 * @author Steven - 23/09/2023
 *
 */
public class CutscenePlayJingle extends CutsceneAction {

    public final int jingleId;
    public final int volume;

    public CutscenePlayJingle(Packet buffer) {
        super(buffer);
        this.jingleId = buffer.readUnsignedShort();
        this.volume = buffer.readUnsignedByte();
    }

    @Override
    public void perform() {
        SoundEffectPlayer.playSoundJingle(this.jingleId, this.volume);
    }
}
