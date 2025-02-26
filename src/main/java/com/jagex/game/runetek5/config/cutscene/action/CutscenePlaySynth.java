package com.jagex.game.runetek5.config.cutscene.action;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.cutscene.CutsceneAction;
import com.jagex.game.runetek5.config.midi.AreadSound;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;

public class CutscenePlaySynth extends CutsceneAction {

    public AreadSound aClass2689367;

    public final int anInt9363;

    public final int anInt9365;

    public final int anInt9364;

    public final int anInt9366;

    public CutscenePlaySynth(Packet rsbytebuffer1) {
        super(rsbytebuffer1);
        this.anInt9363 = rsbytebuffer1.readUShort();
        this.anInt9365 = rsbytebuffer1.readUByte();
        this.anInt9364 = rsbytebuffer1.readUByte();
        this.anInt9366 = rsbytebuffer1.readUByte();
    }

    @Override
    public void perform() {
        this.aClass2689367 = AreaSoundPlayer.playSoundSynth(this.anInt9363, this.anInt9366, 0, this.anInt9365, this.anInt9364);
    }

    @Override
    public void method1593() {
        if (this.aClass2689367 != null) {
            AreaSoundPlayer.resetAreaSound(this.aClass2689367);
            this.aClass2689367 = null;
        }
    }
}
