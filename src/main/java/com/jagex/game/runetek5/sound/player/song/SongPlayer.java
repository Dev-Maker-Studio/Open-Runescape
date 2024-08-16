package com.jagex.game.runetek5.sound.player.song;

import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement;
import com.jagex.game.runetek5.config.midi.SongReference;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.sound.AbstractSongTransition;
import com.jagex.game.runetek5.sound.SongTransition;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream;
import com.jagex.game.runetek5.sound.player.PcmPlayer;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.area.AreaSoundRange;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public class SongPlayer {
	
	public static int musicArchiveId;
	public static AbstractSongTransition songTransition;
	public static SongReference currentSong;
	public static AreaSoundRange secondarySound;
	public static AreaSoundRange primarySound;

	public static void playSong(int musicArchive, int volume, int i_2) {
	    if (SongPlayer.currentSong != null && SongPlayer.currentSong.isLoaded() && musicArchive == SongPlayer.currentSong.musicId) {
	        SongPlayer.play(SongPlayer.currentSong, volume);
	        AreaSoundPlayer.currentMusicArchive = musicArchive;
	        SongPlayer.currentSong = null;
	        SoundEffectPlayer.jingleStream = null;
	        TcpPacket songLoadedPacket = PacketEncoder.createPacket(ClientProt.SONG_LOADED, client.GAME_CONNECTION_CONTEXT.isaac);
	        songLoadedPacket.buffer.writeInt(-1);
	        client.GAME_CONNECTION_CONTEXT.queuePacket(songLoadedPacket);
	    } else {
	        volume = volume * PreferenceManager.preferences.musicVolume.getValue() >> 8;
	        if (musicArchive == -1 && !SoundEffectPlayer.soundJinglePlaying) {
	        	SoundResourceManager.discardSongData();
	        } else if (musicArchive != -1 && (musicArchive != AreaSoundPlayer.currentMusicArchive || !SongReference.method1966())
	        		&& volume != 0 && !SoundEffectPlayer.soundJinglePlaying) {
	            SongPlayer.transitionToNextSong(i_2, Resource.MUSIC, musicArchive, volume, new SongTransition());
	            SongPlayer.method12658();
	        }
	        if (musicArchive != AreaSoundPlayer.currentMusicArchive) {
	            SoundEffectPlayer.jingleStream = null;
	        }
	        AreaSoundPlayer.currentMusicArchive = musicArchive;
	        SongPlayer.secondarySound = null;
	    }
	}

	public static void method5731(int x, int z, int near, int far, int musicArchive, int volume) {
		SongPlayer.playSong(musicArchive, volume, 100);
	    SongPlayer.secondarySound = new AreaSoundRange(x << 9, z << 9, near << 8, far * 8);
	}

	public static void transitionToNextSong(int i_0, Js5ResourceProvider musicResource, int musicArchive, int soundLevel, AbstractSongTransition transition) {
	    if (i_0 > 0) {
	        SoundResourceManager.anInt1730 = 1;
	        AreaSoundPlayer.MUSIC_RESOURCE = musicResource;
	        SongPlayer.musicArchiveId = musicArchive;
	        SoundResourceManager.musicFileId = 0;
	        SoundResourceManager.aNodeSub15Sub27836 = null;
	        AreaSoundPlayer.SOUND_LEVEL = soundLevel;
	        StaticMethods.aBool1962 = false;
	        CutsceneBasicMovement.anInt9440 = SoundResourceManager.aNodeSub15Sub21735.getMusicVolume() / i_0;
	        if (CutsceneBasicMovement.anInt9440 < 1) {
	            CutsceneBasicMovement.anInt9440 = 1;
	        }
	        SongPlayer.songTransition = transition;
	    } else {
	        if (transition != null) {
	            transition.transition();
	        }
	        AreaSoundPlayer.method13400(musicResource, musicArchive, soundLevel);
	    }
	}

	public static void method3912() {
	    if (SongPlayer.primarySound != null) {
	        SongPlayer.primarySound = null;
	        AreaSoundPlayer.setVolumeMultiplier(256);
	    }
	}

	public static void play(SongReference songReference, int volume) {
	    volume = volume * PreferenceManager.preferences.musicVolume.getValue() >> 8;
	    if (songReference == null) {
	    	SoundResourceManager.discardSongData();
	    } else {
	        SongPlayer.playSong(songReference, volume);
	        method3912();
	    }
	}

	public static void method12658() {
	    SongPlayer.method2580(-1, 255);
	}

	public static void playSong(SongReference song, int volume) {
	    SoundResourceManager.aNodeSub15Sub21735.turnOffSound();
	    MidiPcmStream stream = song.getStream();
	    if (stream != null) {
	        SongPlayer.aClass2535830.start();
	        SoundResourceManager.aNodeSub15Sub21735 = stream;
	        SoundResourceManager.aNodeSub15Sub21735.setMusicTrack(song.getMusicTrack(), false);
	        SoundResourceManager.aNodeSub15Sub21735.setMusicVolume(volume);
	        if (SongPlayer.aClass2535830 != null) {
	            SongPlayer.aClass2535830.setCurrentPcmStream(SoundResourceManager.aNodeSub15Sub21735);
	        }
	        SongReference.reset();
	    }
	}

	public static void method2580(int i_0, int i_1) {
	    if (SoundResourceManager.anInt1730 != 0) {
	        if (i_0 < 0) {
	            for (int i_3 = 0; i_3 < 16; i_3++) {
	                SongPlayer.anIntArray5449[i_3] = i_1;
	            }
	        } else {
	            SongPlayer.anIntArray5449[i_0] = i_1;
	        }
	    }
	    SoundResourceManager.aNodeSub15Sub21735.method15095(i_0, i_1);
	}

	public static int[] anIntArray5449;
	public static PcmPlayer aClass2535830;


}
