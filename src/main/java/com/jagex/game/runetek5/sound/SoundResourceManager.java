package com.jagex.game.runetek5.sound;

import com.jagex.game.runetek5.config.midi.SongReference;
import com.jagex.game.runetek5.config.midi.SoundBankCache;
import com.jagex.game.runetek5.config.midi.VorbisNode;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream;
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack;
import com.jagex.game.runetek5.sound.jagtheora.Node_Sub41_Sub2;
import com.jagex.game.runetek5.sound.player.PcmPlayer;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public class SoundResourceManager {

    public static Js5ResourceProvider MIDI_INSTRUMENT_PROVIDER;

    public static Js5ResourceProvider SOUND_EFFECT_PROVIDER;

    public static MidiPcmStream aNodeSub15Sub21735;

    public static Js5ResourceProvider SOUND_MIDI_PROVIDER;

    public static int musicFileId;

    public static long aLong1740;

    public static int anInt1730;

    public static String method2550(int i_0) {
        VorbisNode vorbis = StaticMethods.INDEX36_FILE_CACHE.get(i_0);
        if (vorbis != null) {
            Node_Sub41_Sub2 class282Sub41Sub23 = vorbis.vfReference.method4935();
            if (class282Sub41Sub23 != null) {
                double d_4 = vorbis.vfReference.method4923();
                if (class282Sub41Sub23.method14702() <= d_4 && class282Sub41Sub23.method14705() >= d_4) {
                    return class282Sub41Sub23.method14704();
                }
            }
        }
        return null;
    }
    
    public static void discardSongData() {
        SoundResourceManager.aNodeSub15Sub21735.turnOffSound();
        SoundResourceManager.anInt1730 = 1;
        AreaSoundPlayer.MUSIC_RESOURCE = null;
        SoundResourceManager.aNodeSub15Sub27836 = null;
    }

	public static boolean initializePcmResources(Js5ResourceProvider index0, Js5ResourceProvider index1, Js5ResourceProvider index2, MidiPcmStream class282Sub15Sub23, PcmPlayer class2534) {
	    SOUND_MIDI_PROVIDER = index0;
	    MIDI_INSTRUMENT_PROVIDER = index1;
	    SOUND_EFFECT_PROVIDER = index2;
	    aNodeSub15Sub21735 = class282Sub15Sub23;
	    SongPlayer.aClass2535830 = class2534;
	    SongPlayer.anIntArray5449 = new int[16];
	    for (int i_6 = 0; i_6 < 16; i_6++) {
	        SongPlayer.anIntArray5449[i_6] = 255;
	    }
	    return true;
	}

	public static MidiPcmStream aNodeSub15Sub27836;

	public static boolean method7730() {
	    try {
	        if (anInt1730 == 2) {
	            if (StaticMethods.aNodeSub79563 == null) {
	                StaticMethods.aNodeSub79563 = MusicTrack.loadMusictrack(AreaSoundPlayer.MUSIC_RESOURCE, SongPlayer.musicArchiveId, musicFileId);
	                if (StaticMethods.aNodeSub79563 == null) {
	                    return false;
	                }
	            }
	            if (StaticMethods.aClass2505450 == null) {
	                StaticMethods.aClass2505450 = new SoundBankCache(SOUND_EFFECT_PROVIDER, MIDI_INSTRUMENT_PROVIDER);
	            }
	            MidiPcmStream class282Sub15Sub22 = aNodeSub15Sub21735;
	            if (aNodeSub15Sub27836 != null) {
	                class282Sub15Sub22 = aNodeSub15Sub27836;
	            }
	            if (class282Sub15Sub22.ready(StaticMethods.aNodeSub79563, SOUND_MIDI_PROVIDER, StaticMethods.aClass2505450)) {
	                aNodeSub15Sub21735 = class282Sub15Sub22;
	                aNodeSub15Sub21735.method15096();
	                int i_3;
	                if (StaticMethods.anInt4664 > 0) {
	                    anInt1730 = 3;
	                    aNodeSub15Sub21735.setMusicVolume(Math.min(StaticMethods.anInt4664, AreaSoundPlayer.SOUND_LEVEL));
	                    for (i_3 = 0; i_3 < SongPlayer.anIntArray5449.length; i_3++) {
	                        aNodeSub15Sub21735.method15095(i_3, SongPlayer.anIntArray5449[i_3]);
	                        SongPlayer.anIntArray5449[i_3] = 255;
	                    }
	                } else {
	                    anInt1730 = 0;
	                    aNodeSub15Sub21735.setMusicVolume(AreaSoundPlayer.SOUND_LEVEL);
	                    for (i_3 = 0; i_3 < SongPlayer.anIntArray5449.length; i_3++) {
	                        aNodeSub15Sub21735.method15095(i_3, SongPlayer.anIntArray5449[i_3]);
	                        SongPlayer.anIntArray5449[i_3] = 255;
	                    }
	                }
	                if (aNodeSub15Sub27836 == null) {
	                    if (aLong1740 > 0L) {
	                        aNodeSub15Sub21735.method15100(StaticMethods.aNodeSub79563, StaticMethods.aBool1962, aLong1740);
	                    } else {
	                        aNodeSub15Sub21735.setMusicTrack(StaticMethods.aNodeSub79563, StaticMethods.aBool1962);
	                    }
	                    if (SongPlayer.songTransition != null) {
	                        SongPlayer.songTransition.transition();
	                    }
	                }
	                if (SongPlayer.aClass2535830 != null) {
	                    SongPlayer.aClass2535830.setCurrentPcmStream(aNodeSub15Sub21735);
	                }
	                StaticMethods.aNodeSub79563 = null;
	                StaticMethods.aClass2505450 = null;
	                AreaSoundPlayer.MUSIC_RESOURCE = null;
	                aLong1740 = 0L;
	                aNodeSub15Sub27836 = null;
	                SongPlayer.songTransition = null;
	                return true;
	            }
	        }
	    } catch (Exception exception4) {
	        exception4.printStackTrace();
	        aNodeSub15Sub21735.turnOffSound();
	        SongReference.reset();
	    }
	    return false;
	}
}
