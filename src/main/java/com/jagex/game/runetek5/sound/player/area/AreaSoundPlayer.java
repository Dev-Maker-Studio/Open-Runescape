package com.jagex.game.runetek5.sound.player.area;

import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement;
import com.jagex.game.runetek5.config.midi.AreaSound;
import com.jagex.game.runetek5.config.midi.MixedAudioBuffer;
import com.jagex.game.runetek5.config.midi.SongReference;
import com.jagex.game.runetek5.config.midi.instrument.MusicSample;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;

public class AreaSoundPlayer {

    public static int currentMusicArchive = -1;
    public static int titlescreenMusicArchive = -1;
    public static int AREA_SOUND_CACHE_SIZE;

    public static AreaSound[] AREA_SOUND_CACHE = new AreaSound[50];

    public static int SOUND_LEVEL;
	public static Js5ResourceProvider MUSIC_RESOURCE;

    AreaSoundPlayer() {
        throw new Error();
    }

	public static void initialize() {
	    AREA_SOUND_CACHE = new AreaSound[50];
	    AREA_SOUND_CACHE_SIZE = 0;
	}
    
	public static void method1623() {
	    PacketEncoder.method3675();
	    for (int soundIndex = 0; soundIndex < AREA_SOUND_CACHE_SIZE; soundIndex++) {
	        AreaSound sound = AREA_SOUND_CACHE[soundIndex];
	        boolean bool3 = false;
	        if (sound.areaSoundStream == null) {
	            if (--sound.delay >= (sound.spoken() ? -1500 : -10)) {
	                if (sound.type == 1 && sound.soundEffect == null) {
	                    sound.soundEffect = SoundEffectPlayer.getSoundEffect(Resource.SOUND_EFFECT, sound.soundId, 0);
	                    if (sound.soundEffect == null) {
	                        continue;
	                    }
	                    sound.delay += sound.soundEffect.getDelay();
	                } else if (sound.spoken() && (sound.midiInstrument == null || sound.buffer == null)) {
	                    if (sound.midiInstrument == null) {
	                        sound.midiInstrument = MusicSample.list(Resource.MIDI_INSTRUMENT, sound.soundId);
	                    }
	                    if (sound.midiInstrument == null) {
	                        continue;
	                    }
	                    if (sound.buffer == null) {
	                        sound.buffer = sound.midiInstrument.getAudioBuffer();
	                        if (sound.buffer == null) {
	                            continue;
	                        }
	                    }
	                }
	                if (sound.delay < 0) {
	                    int i_4 = 8192;
	                    int volume;
	                    if (sound.bitpackedPosition != 0) {
	                        int plane = sound.bitpackedPosition >> 24 & 0x3;
	                        if (plane == PlayerUpdate.MY_PLAYER.plane) {
	                            int maxHearDistance = (sound.bitpackedPosition & 0xff) << 9;
	                            int size = PlayerUpdate.MY_PLAYER.getSize() << 8;
	                            Vector3 abs = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
	                            int startX = sound.bitpackedPosition >> 16 & 0xff;
	                            int endX = (startX << 9) + 256 - (int) abs.x + size;
	                            int startY = sound.bitpackedPosition >> 8 & 0xff;
	                            int endY = (startY << 9) + 256 - (int) abs.z + size;
	                            int currentDistance = Math.abs(endX) + Math.abs(endY) - 512;
	                            if (currentDistance > maxHearDistance) {
	                                sound.delay = -99999;
	                                continue;
	                            }
	                            if (currentDistance < 0) {
	                                currentDistance = 0;
	                            }
	                            volume = (maxHearDistance - currentDistance) * PreferenceManager.preferences.ambientSoundVolume.getValue() * sound.volume / maxHearDistance >> 2;
	                            if (sound.startX != -1) {
	                                startX = sound.startX;
	                                startY = sound.startZ;
	                            }
	                            if (endX != 0 || endY != 0) {
	                                int angle = -Camera.cameraRoll - (int) (Math.atan2(endX, endY) * 2607.5945876176133D) - 4096 & 0x3fff;
	                                if (angle > 8192) {
	                                    angle = 16384 - angle;
	                                }
	                                int i_16;
	                                if (currentDistance <= 0) {
	                                    i_16 = 8192;
	                                } else if (currentDistance >= 4096) {
	                                    i_16 = 16384;
	                                } else {
	                                    i_16 = (8192 - currentDistance) / 4096 + 8192;
	                                }
	                                i_4 = (16384 - i_16 >> 1) + angle * i_16 / 8192;
	                            }
	                        } else {
	                            volume = 0;
	                        }
	                    } else {
	                        volume = sound.volume * (sound.type == 3 
	                        		? PreferenceManager.preferences.voiceOverVolume.getValue()
	                        		: PreferenceManager.preferences.soundEffectVolume.getValue()) >> 2;
	                    }
	                    if (volume > 0) {
	                        if (sound.type == 1) {
	                            MixedAudioBuffer mixedAudio = sound.soundEffect.getMixedAudio().resample(SoundEffectPlayer.resampler);
	                            sound.areaSoundStream = mixedAudio.getStream(sound.sampleRate, volume, i_4);
	                        } else if (sound.spoken()) {
	                            sound.areaSoundStream = sound.buffer.getStream(sound.sampleRate, volume, i_4);
	                        }
	                        sound.areaSoundStream.setNumLoops(sound.loops - 1);
	                        AmbientSound.gameSounds.addSubStream(sound.areaSoundStream);
	                    }
	                }
	            } else {
	                bool3 = true;
	            }
	        } else if (!sound.areaSoundStream.linked()) {
	            bool3 = true;
	        }
	        if (bool3) {
	            --AREA_SOUND_CACHE_SIZE;
	            for (int index = soundIndex; index < AREA_SOUND_CACHE_SIZE; index++) {
	                AREA_SOUND_CACHE[index] = AREA_SOUND_CACHE[index + 1];
	            }
	            --soundIndex;
	        }
	    }
	    if (SoundEffectPlayer.soundJinglePlaying && !SongReference.method1966()) {
	        if (PreferenceManager.preferences.musicVolume.getValue() != 0 && currentMusicArchive != -1) {
	            if (SoundEffectPlayer.jingleStream != null) {
	                AreaSoundPlayer.method3690(Resource.MUSIC, currentMusicArchive, PreferenceManager.preferences.musicVolume.getValue(), SoundEffectPlayer.jingleStream);
	            } else {
	                AreaSoundPlayer.method13400(Resource.MUSIC, currentMusicArchive, PreferenceManager.preferences.musicVolume.getValue());
	            }
	        }
	        SoundEffectPlayer.soundJinglePlaying = false;
	        SoundEffectPlayer.jingleStream = null;
	    } else if (PreferenceManager.preferences.musicVolume.getValue() != 0 && currentMusicArchive != -1 && !SongReference.method1966()) {
	        TcpPacket musicEndedPacket = PacketEncoder.createPacket(ClientProt.SOUND_EFFECT_MUSIC_ENDED, client.GAME_CONNECTION_CONTEXT.isaac);
	        musicEndedPacket.buffer.writeInt(currentMusicArchive);
	        client.GAME_CONNECTION_CONTEXT.queuePacket(musicEndedPacket);
	        currentMusicArchive = -1;
	    }
	}

	public static void musicSourceUpdate(Js5ResourceProvider musicResource, int musicArchiveId, int soundLevel) {
	    SoundResourceManager.anInt1730 = 1;
	    AreaSoundPlayer.MUSIC_RESOURCE = musicResource;
	    SongPlayer.musicArchiveId = musicArchiveId;
	    SoundResourceManager.musicFileId = 0;
	    SoundResourceManager.aNodeSub15Sub27836 = null;
	    AreaSoundPlayer.SOUND_LEVEL = soundLevel;
	    StaticMethods.aBool1962 = false;
	    CutsceneBasicMovement.anInt9440 = 2;
	    SongPlayer.songTransition = null;
	}

	public static void setVolumeMultiplier(int volume) {
	    SoundResourceManager.aNodeSub15Sub21735.setVolumeMultiplier(volume >> 8);
	}

	/**
	 * Plays vorbis sound with volume adjusted by distance from given position
	 */
	public static AreaSound playSoundVorbis(int soundId, int timesRepeated, int delay, int volume, int bitpackedPosition, boolean voiceEffect, int sampleRate) {
	    if ((voiceEffect ? PreferenceManager.preferences.voiceOverVolume.getValue()
	    		: PreferenceManager.preferences.soundEffectVolume.getValue()) != 0 && timesRepeated != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
	        AreaSound areaSound = new AreaSound((byte) (voiceEffect ? 3 : 2), soundId, timesRepeated, delay, volume, bitpackedPosition, sampleRate, null);
	        AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound;
	        return areaSound;
	    } 
	    return null;
	}

	/**
	 * Plays vorbis sound with a constant volume
	 */
	public static AreaSound playSoundVorbis(int soundId, int timesRepeated, int delay, int volume, boolean voiceEffect, int sampleRate) {
	    if ((voiceEffect ? PreferenceManager.preferences.voiceOverVolume.getValue()
	    		: PreferenceManager.preferences.soundEffectVolume.getValue()) != 0 && timesRepeated != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
	        AreaSound areaSound = new AreaSound((byte) (voiceEffect ? 3 : 2), soundId, timesRepeated, delay, volume, 0, sampleRate, null);
	        AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound;
	        return areaSound;
	    } 
	    return null;
	}
	
	/**
	 * Plays synth sound with volume adjusted by distance from given position
	 */
	public static AreaSound playSoundSynth(int soundId, int timesRepeat, int delay, int volume, int bitpackedPosition, int sampleRate) {
	    if (PreferenceManager.preferences.soundEffectVolume.getValue() != 0 && timesRepeat != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
	        AreaSound areaSound = new AreaSound((byte) 1, soundId, timesRepeat, delay, volume, bitpackedPosition, sampleRate, null);
	        AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound;
	        return areaSound;
	    }
	    return null;
	}
	
	/**
	 * Plays synth sound with a constant volume
	 */
	public static AreaSound playSoundSynth(int soundId, int timesRepeat, int delay, int volume, int sampleRate) {
	    if (PreferenceManager.preferences.soundEffectVolume.getValue() != 0 && timesRepeat != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
	        AreaSound areaSound = new AreaSound((byte) 1, soundId, timesRepeat, delay, volume, 0, sampleRate, null);
	        AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound;
	        return areaSound;
	    } 
	    return null;
	}

	public static void resetSounds() {
	    for (int index = 0; index < AREA_SOUND_CACHE_SIZE; index++) {
	        AreaSound areaSound = AREA_SOUND_CACHE[index];
	        if (areaSound.type == 3) {
	            resetAreaSound(areaSound);
	        }
	    }
	}
	
	public static void resetAreaSound(AreaSound areaSound) {
	    if (areaSound.areaSoundStream != null) {
	        AmbientSound.gameSounds.unlink(areaSound.areaSoundStream);
	    } else {
	        areaSound.delay = Integer.MIN_VALUE;
	    }
	}

	public static void method3690(Js5ResourceProvider musicResource, int archive, int volume, MidiPcmStream class282Sub15Sub25) {
	    AreaSoundPlayer.method13400(musicResource, archive, volume);
	    SoundResourceManager.aNodeSub15Sub27836 = class282Sub15Sub25;
	}

	public static void method13400(Js5ResourceProvider musicResource, int musicArchive, int soundLevel) {
	    AreaSoundPlayer.method4827(musicResource, musicArchive, soundLevel);
	}

	public static void method4827(Js5ResourceProvider musicResource, int musicArchive, int soundLevel) {
	    AreaSoundPlayer.method1859(musicResource, musicArchive, 0, soundLevel, false);
	}

	public static void method1859(Js5ResourceProvider musicResource, int musicArchive, int musicFile, int soundLevel, boolean bool4) {
	    SoundResourceManager.anInt1730 = 1;
	    MUSIC_RESOURCE = musicResource;
	    SongPlayer.musicArchiveId = musicArchive;
	    SoundResourceManager.musicFileId = musicFile;
	    SoundResourceManager.aNodeSub15Sub27836 = null;
	    SOUND_LEVEL = soundLevel;
	    StaticMethods.aBool1962 = bool4;
	    CutsceneBasicMovement.anInt9440 = 10000;
	    SoundResourceManager.aLong1740 = 0;
	    StaticMethods.anInt4664 = 0;
	}

	public static void method5961() {
	    if (SongPlayer.primarySound != null) {
	        Vector3 abs = Vector3Pool.vector3From(PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords);
	        CoordGrid base = Resource.mapRegion.getBaseCoordinates();
	        abs.x += (base.x << 9);
	        abs.z += (base.y << 9);
	        abs.subtract(SongPlayer.primarySound.x, abs.y, SongPlayer.primarySound.z);
	        float magnitude = abs.magnitude();
	        abs.pushVectorStack();
	        float volumeMultiplier = 0.0F;
	        if (magnitude < SongPlayer.primarySound.near) {
	            volumeMultiplier = 1.0F;
	        } else if (magnitude > SongPlayer.primarySound.far) {
	            volumeMultiplier = 0.0F;
	        } else {
	            magnitude -= SongPlayer.primarySound.near;
	            volumeMultiplier = 1.0F - magnitude / SongPlayer.primarySound.range;
	        }
	        setVolumeMultiplier((int) (volumeMultiplier * 256.0F));
	    }
	}
}
