package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.config.midi.instrument.MusicSample;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.entity.SceneGraphEntity;
import com.jagex.game.runetek5.sound.jagtheora.AreaSoundStream;
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer;

/**
 * 
 * @author Brainpower - 29/01/2023
 *
 */
public class AreadSound {

	public MusicSample midiInstrument;
    public AudioBuffer buffer;
    public SoundEffect soundEffect;
    public AreaSoundStream areaSoundStream;
    public int startX = -1;

    public int startZ = -1;

    public final byte type;

    public final int soundId;

    public final int loops;

    public int delay;

    public final int volume;

    public final int bitpackedPosition;

    public final int sampleRate;

    public AreadSound(byte type, int soundId, int loops, int delay, int volume, int bitpackedPosition, int sampleRate, SceneGraphEntity sceneObject) {
        this.type = type;
        this.soundId = soundId;
        this.loops = loops;
        this.delay = delay;
        this.volume = volume;
        this.bitpackedPosition = bitpackedPosition;
        this.sampleRate = sampleRate;
        if (sceneObject instanceof AbstractInteractiveEntity interactiveObject) {
            this.startX = interactiveObject.startX;
            this.startZ = interactiveObject.startZ;
        }
    }

    public boolean spoken() {
        return this.type == 2 || this.type == 3;
    }
}
