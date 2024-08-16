package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;

public class SongReference {

    public final int musicId;
    public SoundBankCache soundBankCache;
    public int loadingStage;
    public final Js5ResourceProvider musicResource;
    public final MidiPcmStream stream;
    public MusicTrack musicTrack;

    public SongReference(Js5ResourceProvider musicResource, int musicId) {
        this.musicResource = musicResource;
        this.musicId = musicId;
        this.stream = new MidiPcmStream();
        this.stream.load();
    }

    public static boolean method1966() {
        return SoundResourceManager.anInt1730 != 0 || SoundResourceManager.aNodeSub15Sub21735.dataLoaded();
    }

    public MidiPcmStream getStream() {
        return this.stream;
    }

    public MusicTrack getMusicTrack() {
        return this.musicTrack;
    }

    public boolean isLoaded() {
        return this.loadingStage == 2;
    }

    public boolean initialize() {
        SongReference.reset();
        if (this.loadingStage == 0) {
            this.musicTrack = MusicTrack.loadMusictrack(this.musicResource, this.musicId, 0);
            if (this.musicTrack == null) {
                return false;
            }
            this.soundBankCache = new SoundBankCache(SoundResourceManager.SOUND_EFFECT_PROVIDER, SoundResourceManager.MIDI_INSTRUMENT_PROVIDER);
            this.loadingStage = 1;
        }
        if (this.loadingStage == 1) {
            if (!this.stream.ready(this.musicTrack, SoundResourceManager.SOUND_MIDI_PROVIDER, this.soundBankCache)) {
                return false;
            }
            this.loadingStage = 2;
        }
        return true;
    }

	public static void reset() {
	    SoundResourceManager.anInt1730 = 0;
	    StaticMethods.aNodeSub79563 = null;
	    StaticMethods.aClass2505450 = null;
	    AreaSoundPlayer.MUSIC_RESOURCE = null;
	    SoundResourceManager.aNodeSub15Sub27836 = null;
	}
}
