package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.midi.AreadSound;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;

/**
 * 
 * @author Steven - 02/10/2023
 *
 */
public class CutscenePlayVorbis extends CutsceneAction {

    public AreadSound vorbisSound;

    public final int soundId;

    public final int volume;

    public final int sampleRate;

    public final int timesRepeated;

    public CutscenePlayVorbis(Packet buffer) {
        super(buffer);
        this.soundId = buffer.readUnsignedShort();
        this.volume = buffer.readUnsignedByte();
        this.sampleRate = buffer.readUnsignedByte();
        this.timesRepeated = buffer.readUnsignedByte();
    }

    @Override
    public void perform() {
        this.vorbisSound = AreaSoundPlayer.playSoundVorbis(this.soundId, this.timesRepeated, 0, this.volume, false, this.sampleRate);
    }

    @Override
    public void method1593() {
        if (this.vorbisSound != null) {
            AreaSoundPlayer.resetAreaSound(this.vorbisSound);
            this.vorbisSound = null;
        }
    }
}
