package com.jagex.game.runetek5.sound.player.area;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.bas.BasType;
import com.jagex.game.runetek5.config.midi.MixedAudioBuffer;
import com.jagex.game.runetek5.config.midi.SongReference;
import com.jagex.game.runetek5.config.midi.SoundEffect;
import com.jagex.game.runetek5.config.midi.instrument.MusicSample;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.sound.Resampler;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.jagtheora.AudioBufferStream;
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream;
import com.jagex.game.runetek5.sound.jagtheora.MixedAudioStream;
import com.jagex.game.runetek5.sound.jagtheora.Node_Sub15_Sub4;
import com.jagex.game.runetek5.sound.player.PcmPlayer;
import com.jagex.game.runetek5.sound.player.PcmPlayerManager;
import com.jagex.game.runetek5.sound.player.song.SongPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.vardomain.VarProviders;

public class SoundEffectPlayer {

	public static void method1491(int i_0) {
	    if (SoundResourceManager.anInt1730 != 0) {
	        AreaSoundPlayer.SOUND_LEVEL = i_0;
	    } else {
	        SoundResourceManager.aNodeSub15Sub21735.setMusicVolume(i_0);
	    }
	}

	public static SoundEffect getSoundEffect(Js5ResourceProvider resource, int archiveId, int fileId) {
	    byte[] data = resource.getFile(archiveId, fileId);
	    return data == null ? null : new SoundEffect(new Packet(data));
	}

	public static void method14675(int plane, int absX, int absZ, int step) {
	    for (AmbientSound ambient = (AmbientSound) AmbientSound.LOC_AMBIENT_SOUNDS.getFirst(); ambient != null; ambient = (AmbientSound) AmbientSound.LOC_AMBIENT_SOUNDS.next()) {
	        SoundEffectPlayer.method2964(ambient, plane, absX, absZ, step);
	    }
	    for (AmbientSound sound = (AmbientSound) AmbientSound.NPC_AMBIENT_SOUNDS.getFirst(); sound != null; sound = (AmbientSound) AmbientSound.NPC_AMBIENT_SOUNDS.next()) {
	        byte currentAnimatingType = 1;
	        BasType bastype = sound.npcEntity.getBastype();
	        int npcSequenceId = sound.npcEntity.movingSequence.getId();
	        if (npcSequenceId != -1 && !sound.npcEntity.movingSequence.hasType) {
	            if (npcSequenceId != bastype.runningSequence && npcSequenceId != bastype.runDir3 && npcSequenceId != bastype.runDir1 && npcSequenceId != bastype.runDir2) {
	                if (npcSequenceId == bastype.teleportSequence || npcSequenceId == bastype.teleDir3 || npcSequenceId == bastype.teleDir1 || npcSequenceId == bastype.teleDir2) {
	                    currentAnimatingType = 3;
	                }
	            } else {
	                currentAnimatingType = 2;
	            }
	        } else {
	            currentAnimatingType = 0;
	        }
	        if (currentAnimatingType != sound.animationType) {
	        	int soundEffectId = SoundEffectPlayer.getSoundEffectId(sound.npcEntity);
	            NPCType npc = sound.npcEntity.definitions;
	            if (npc.alternativeIds != null) {
	                npc = npc.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
	            }
	            if (npc != null && soundEffectId != -1) {
	                if (soundEffectId == sound.soundEffectId && sound.instrumentSoundEffect == npc.instrumentSoundEffect) {
	                    sound.animationType = currentAnimatingType;
	                    sound.ambientSoundVolume = npc.ambientSoundVolume;
	                } else {
	                    boolean replay = false;
	                    if (sound.soundEffect != null) {
	                        sound.ambientSoundVolume -= 512;
	                        if (sound.ambientSoundVolume <= 0) {
	                            AmbientSound.gameSounds.unlink(sound.soundEffect);
	                            sound.soundEffect = null;
	                            replay = true;
	                        }
	                    } else {
	                        replay = true;
	                    }
	                    if (replay) {
	                        sound.ambientSoundVolume = npc.ambientSoundVolume;
	                        sound.aNodeSub26Sub1Sub18082 = null;
	                        sound.instrumentEffect = null;
	                        sound.soundEffectId = soundEffectId;
	                        sound.instrumentSoundEffect = npc.instrumentSoundEffect;
	                        sound.animationType = currentAnimatingType;
	                    }
	                }
	            } else {
	                sound.soundEffectId = -1;
	                sound.instrumentSoundEffect = false;
	                sound.animationType = currentAnimatingType;
	            }
	        }
	        Vector3 abs = sound.npcEntity.getAbsolutePosition().coords;
	        sound.absStartX = (int) abs.x;
	        sound.absEndX = (int) abs.x + (sound.npcEntity.getSize() << 8);
	        sound.absStartY = (int) abs.z;
	        sound.absEndY = (int) abs.z + (sound.npcEntity.getSize() << 8);
	        sound.plane = sound.npcEntity.plane;
	        SoundEffectPlayer.method2964(sound, plane, absX, absZ, step);
	    }
	    for (AmbientSound sound = (AmbientSound) AmbientSound.PLAYER_AMBIENT_SOUNDS.first(); sound != null; sound = (AmbientSound) AmbientSound.PLAYER_AMBIENT_SOUNDS.next()) {
	        byte seqType = 1;
	        BasType bas = sound.playerEntity.getBastype();
	        int sequenceId = sound.playerEntity.movingSequence.getId();
	        if (sequenceId != -1 && !sound.playerEntity.movingSequence.hasType) {
	            if (sequenceId != bas.runningSequence && sequenceId != bas.runDir3 && sequenceId != bas.runDir1 && sequenceId != bas.runDir2) {
	                if (sequenceId == bas.teleportSequence || sequenceId == bas.teleDir3 || sequenceId == bas.teleDir1 || sequenceId == bas.teleDir2) {
	                    seqType = 3;
	                }
	            } else {
	                seqType = 2;
	            }
	        } else {
	            seqType = 0;
	        }
	        if (seqType != sound.animationType) {
	        	int soundEffectId = getSoundEffectId(sound.playerEntity);
	            if (soundEffectId == sound.soundEffectId && sound.playerEntity.instrumentSoundEffect == sound.instrumentSoundEffect) {
	                sound.ambientSoundVolume = sound.playerEntity.ambientSoundVolume;
	                sound.animationType = seqType;
	            } else {
	                boolean replay = false;
	                if (sound.soundEffect != null) {
	                    sound.ambientSoundVolume -= 512;
	                    if (sound.ambientSoundVolume <= 0) {
	                        AmbientSound.gameSounds.unlink(sound.soundEffect);
	                        sound.soundEffect = null;
	                        replay = true;
	                    }
	                } else {
	                    replay = true;
	                }
	                if (replay) {
	                    sound.ambientSoundVolume = sound.playerEntity.ambientSoundVolume;
	                    sound.aNodeSub26Sub1Sub18082 = null;
	                    sound.instrumentEffect = null;
	                    sound.soundEffectId = soundEffectId;
	                    sound.instrumentSoundEffect = sound.playerEntity.instrumentSoundEffect;
	                    sound.animationType = seqType;
	                }
	            }
	        }
	        Vector3 abs = sound.playerEntity.getAbsolutePosition().coords;
	        sound.absStartX = (int) abs.x;
	        sound.absEndX = (int) abs.x + (sound.playerEntity.getSize() << 8);
	        sound.absStartY = (int) abs.z;
	        sound.absEndY = (int) abs.z + (sound.playerEntity.getSize() << 8);
	        sound.plane = sound.playerEntity.plane;
	        SoundEffectPlayer.method2964(sound, plane, absX, absZ, step);
	    }
	}

	public static void method2964(AmbientSound ambientSound, int plane, int absX, int absY, int step) {
	    if (ambientSound.soundEffectId != -1 || ambientSound.ambientSoundGroupIds != null) {
	        int hearDistance = 0;
	        int preferedVolume = ambientSound.ambientSoundVolume * PreferenceManager.preferences.ambientSoundVolume.getValue() >> 8;
	        if (absX > ambientSound.absEndX) {
	            hearDistance += absX - ambientSound.absEndX;
	        } else if (absX < ambientSound.absStartX) {
	            hearDistance += ambientSound.absStartX - absX;
	        }
	        if (absY > ambientSound.absEndY) {
	            hearDistance += absY - ambientSound.absEndY;
	        } else if (absY < ambientSound.absStartY) {
	            hearDistance += ambientSound.absStartY - absY;
	        }
	        if (ambientSound.ambientSoundMinHearDistance != 0 && hearDistance - 256 <= ambientSound.ambientSoundMinHearDistance
	        		&& PreferenceManager.preferences.ambientSoundVolume.getValue() != 0 && ambientSound.plane == plane) {
	            hearDistance -= 256;
	            if (hearDistance < 0) {
	                hearDistance = 0;
	            }
	            int minHearDistance = ambientSound.ambientSoundMinHearDistance - ambientSound.ambientSoundMaxHearDistance;
	            if (minHearDistance < 0) {
	                minHearDistance = ambientSound.ambientSoundMinHearDistance;
	            }
	            int volume = preferedVolume;
	            int hearDistanceDelta = hearDistance - ambientSound.ambientSoundMaxHearDistance;
	            if (hearDistanceDelta > 0 && minHearDistance > 0) {
	                volume = preferedVolume * (minHearDistance - hearDistanceDelta) / minHearDistance;
	            }
	            PlayerUpdate.MY_PLAYER.getSize();
	            int i_11 = 8192;
	            int i_12 = (ambientSound.absStartX + ambientSound.absEndX) / 2 - absX;
	            int i_13 = (ambientSound.absStartY + ambientSound.absEndY) / 2 - absY;
	            if (i_12 != 0 || i_13 != 0) {
	                int i_17 = -Camera.cameraRoll - (int) (Math.atan2(i_12, i_13) * 2607.5945876176133D) - 4096 & 0x3fff;
	                if (i_17 > 8192) {
	                    i_17 = 16384 - i_17;
	                }
	                int i_18;
	                if (hearDistance <= 0) {
	                    i_18 = 8192;
	                } else if (hearDistance >= 4096) {
	                    i_18 = 16384;
	                } else {
	                    i_18 = hearDistance * 8192 / 4096 + 8192;
	                }
	                i_11 = (16384 - i_18 >> 1) + i_17 * i_18 / 8192;
	            }
	            AudioBufferStream class282Sub15Sub5Sub114;
	            MixedAudioBuffer mixedAudioNode;
	            MixedAudioStream stream;
	            SoundEffect soundEffect;
	            if (ambientSound.soundEffect == null) {
	                if (ambientSound.soundEffectId >= 0) {
	                    int sampleRate = ambientSound.minDelay == 256 && ambientSound.maxDelay == 256
	                    		? 256 : MathUtils.calculateSampleRate(ambientSound.maxDelay, ambientSound.minDelay);
	                    if (ambientSound.instrumentSoundEffect) {
	                        if (ambientSound.instrumentEffect == null) {
	                            ambientSound.instrumentEffect = MusicSample.list(Resource.MIDI_INSTRUMENT, ambientSound.soundEffectId);
	                        }
	                        if (ambientSound.instrumentEffect != null) {
	                            if (ambientSound.aNodeSub26Sub1Sub18082 == null) {
	                                ambientSound.aNodeSub26Sub1Sub18082 = ambientSound.instrumentEffect.getAudioBuffer();
	                            }
	                            if (ambientSound.aNodeSub26Sub1Sub18082 != null) {
	                                class282Sub15Sub5Sub114 = (AudioBufferStream) ambientSound.aNodeSub26Sub1Sub18082.getStream(sampleRate, volume << 6, i_11);
	                                class282Sub15Sub5Sub114.setNumLoops(-1);
	                                AmbientSound.gameSounds.addSubStream(class282Sub15Sub5Sub114);
	                                ambientSound.soundEffect = class282Sub15Sub5Sub114;
	                            }
	                        }
	                    } else {
	                        soundEffect = getSoundEffect(Resource.SOUND_EFFECT, ambientSound.soundEffectId, 0);
	                        if (soundEffect != null) {
	                            mixedAudioNode = soundEffect.getMixedAudio().resample(SoundEffectPlayer.resampler);
	                            stream = (MixedAudioStream) mixedAudioNode.getStream(sampleRate, volume << 6, i_11);
	                            stream.setNumLoops(-1);
	                            AmbientSound.gameSounds.addSubStream(stream);
	                            ambientSound.soundEffect = stream;
	                        }
	                    }
	                }
	            } else {
	                ambientSound.soundEffect.setVolume(volume);
	                ambientSound.soundEffect.method15340(i_11);
	            }
	            if (ambientSound.aNodeSub15Sub58096 == null) {
	                if (ambientSound.ambientSoundGroupIds != null && (ambientSound.step -= step) <= 0) {
	                    int sampleRate = ambientSound.minDelay == 256 && ambientSound.maxDelay == 256
	                    		? 256 : (int) (Math.random() * (ambientSound.minDelay - ambientSound.maxDelay)) + ambientSound.maxDelay;
	                    if (ambientSound.instrumentAmbientSound) {
	                        if (ambientSound.aNodeSub188087 == null) {
	                            int random = (int) (Math.random() * ambientSound.ambientSoundGroupIds.length);
	                            ambientSound.aNodeSub188087 = MusicSample.list(Resource.MIDI_INSTRUMENT, ambientSound.ambientSoundGroupIds[random]);
	                        }
	                        if (ambientSound.aNodeSub188087 != null) {
	                            if (ambientSound.aNodeSub26Sub1Sub18106 == null) {
	                                ambientSound.aNodeSub26Sub1Sub18106 = ambientSound.aNodeSub188087.getAudioBuffer();
	                            }
	                            if (ambientSound.aNodeSub26Sub1Sub18106 != null) {
	                                class282Sub15Sub5Sub114 = (AudioBufferStream) ambientSound.aNodeSub26Sub1Sub18106.getStream(sampleRate, volume << 6, i_11);
	                                class282Sub15Sub5Sub114.setNumLoops(0);
	                                AmbientSound.gameSounds.addSubStream(class282Sub15Sub5Sub114);
	                                ambientSound.aNodeSub15Sub58096 = class282Sub15Sub5Sub114;
	                                ambientSound.step = ambientSound.soundMinStep + (int) (Math.random() * (ambientSound.soundMaxStep - ambientSound.soundMinStep));
	                            }
	                        }
	                    } else {
	                        int random = (int) (Math.random() * ambientSound.ambientSoundGroupIds.length);
	                        soundEffect = getSoundEffect(Resource.SOUND_EFFECT, ambientSound.ambientSoundGroupIds[random], 0);
	                        if (soundEffect != null) {
	                            mixedAudioNode = soundEffect.getMixedAudio().resample(SoundEffectPlayer.resampler);
	                            stream = (MixedAudioStream) mixedAudioNode.getStream(sampleRate, volume << 6, i_11);
	                            stream.setNumLoops(0);
	                            AmbientSound.gameSounds.addSubStream(stream);
	                            ambientSound.aNodeSub15Sub58096 = stream;
	                            ambientSound.step = ambientSound.soundMinStep + (int) (Math.random() * (ambientSound.soundMaxStep - ambientSound.soundMinStep));
	                        }
	                    }
	                }
	            } else {
	                ambientSound.aNodeSub15Sub58096.setVolume(volume);
	                ambientSound.aNodeSub15Sub58096.method15340(i_11);
	                if (!ambientSound.aNodeSub15Sub58096.linked()) {
	                    ambientSound.aNodeSub15Sub58096 = null;
	                    ambientSound.aNodeSub188087 = null;
	                    ambientSound.aNodeSub26Sub1Sub18106 = null;
	                }
	            }
	        } else {
	            if (ambientSound.soundEffect != null) {
	                AmbientSound.gameSounds.unlink(ambientSound.soundEffect);
	                ambientSound.soundEffect = null;
	                ambientSound.aNodeSub26Sub1Sub18082 = null;
	                ambientSound.instrumentEffect = null;
	            }
	            if (ambientSound.aNodeSub15Sub58096 != null) {
	                AmbientSound.gameSounds.unlink(ambientSound.aNodeSub15Sub58096);
	                ambientSound.aNodeSub15Sub58096 = null;
	                ambientSound.aNodeSub188087 = null;
	                ambientSound.aNodeSub26Sub1Sub18106 = null;
	            }
	        }
	    }
	}

	public static int getSoundEffectId(NpcEntity npc) {
	    NPCType npcType = npc.definitions;
	    if (npcType.alternativeIds != null) {
	        npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
	        if (npcType == null) {
	            return -1;
	        }
	    }
	    int soundEffect = npcType.idleSoundEffect;
	    BasType basType = npc.getBastype();
	    int movingSequenceId = npc.movingSequence.getId();
	    if (movingSequenceId != -1 && !npc.movingSequence.hasType) {
	        if (movingSequenceId != basType.runningSequence && movingSequenceId != basType.runDir3 && movingSequenceId != basType.runDir1 && movingSequenceId != basType.runDir2) {
	            if (movingSequenceId == basType.teleportSequence || movingSequenceId == basType.teleDir3 || movingSequenceId == basType.teleDir1 || movingSequenceId == basType.teleDir2) {
	                soundEffect = npcType.teleportSoundEffect;
	            }
	        } else {
	            soundEffect = npcType.runningSoundEffect;
	        }
	    } else {
	        soundEffect = npcType.walkingSoundEffect;
	    }
	    return soundEffect;
	}
	
	public static int getSoundEffectId(PlayerEntity player) {
	    int soundEffectId = player.defaultSoundEffectId;
	    BasType bastype = player.getBastype();
	    int sequenceId = player.movingSequence.getId();
	    if (sequenceId != -1 && !player.movingSequence.hasType) {
	        if (sequenceId != bastype.runningSequence && sequenceId != bastype.runDir3 && sequenceId != bastype.runDir1 && sequenceId != bastype.runDir2) {
	            if (sequenceId == bastype.teleportSequence || sequenceId == bastype.teleDir3 || sequenceId == bastype.teleDir1 || sequenceId == bastype.teleDir2) {
	                soundEffectId = player.teleportingSoundEffectId;
	            }
	        } else {
	            soundEffectId = player.runningSoundEffectId;
	        }
	    } else {
	        soundEffectId = player.walkingSoundEffectId;
	    }
	    return soundEffectId;
	}

	public static void playSoundJingle(int jingleId, int volume) {
	    volume = volume * PreferenceManager.preferences.musicVolume.getValue() >> 8;
	    if (volume != 0 && jingleId != -1) {
	        if (!SoundEffectPlayer.soundJinglePlaying && AreaSoundPlayer.currentMusicArchive != -1 && SongReference.method1966() && !StaticMethods.method6140()) {
	            SoundEffectPlayer.jingleStream = SoundEffectPlayer.method7272();
	            SongPlayer.method3912();
	            MidiPcmStream jingleStream = SoundEffectPlayer.getStream(SoundEffectPlayer.jingleStream);
	            SoundEffectPlayer.setJingleStream(jingleStream);
	        }
	        AreaSoundPlayer.method13400(Resource.JINGLE, jingleId, volume);
	        SongPlayer.method2580(-1, 255);
	        SoundEffectPlayer.soundJinglePlaying = true;
	    }
	}
	public static PcmPlayer jinglePlayer;
	public static PcmPlayer soundEffectPlayer;
	public static Resampler resampler;
	public static boolean soundJinglePlaying;
	public static MidiPcmStream jingleStream;
	public static void updatePlayers() {
	    if (SoundEffectPlayer.jinglePlayer != null) {
	        SoundEffectPlayer.jinglePlayer.cycle();
	    }
	    if (SoundEffectPlayer.soundEffectPlayer != null) {
	        SoundEffectPlayer.soundEffectPlayer.cycle();
	    }	
	}

	public static void shutdownPlayers() {
	    if (SoundEffectPlayer.jinglePlayer != null) {
	        SoundEffectPlayer.jinglePlayer.shutdown();
	    }
	    if (SoundEffectPlayer.soundEffectPlayer != null) {
	        SoundEffectPlayer.soundEffectPlayer.shutdown();
	    }
	}

	public static void setJingleStream(MidiPcmStream stream) {
	    SoundEffectPlayer.jinglePlayer.setCurrentPcmStream(stream);
	    SoundResourceManager.initializePcmResources(Resource.SOUND_MIDI, Resource.MIDI_INSTRUMENT, Resource.SOUND_EFFECT, stream, SoundEffectPlayer.jinglePlayer);
	}

	public static void reload() {
	    shutdownPlayers();
	    PcmPlayerManager.setSterio(PreferenceManager.preferences.monoStereo.getValue() == 1);
	    SoundEffectPlayer.jinglePlayer = PcmPlayerManager.start(0, 22050);
	    setJingleStream(SoundEffectPlayer.getStream(null));
	    SoundEffectPlayer.soundEffectPlayer = PcmPlayerManager.start(1, 2048);
	    SoundEffectPlayer.soundEffectPlayer.setCurrentPcmStream(AmbientSound.gameSounds);
	}

	public static void initializeAudio() {
	    PcmPlayerManager.setSterio(PreferenceManager.preferences.monoStereo.getValue() == 1);
	    SoundEffectPlayer.jinglePlayer = PcmPlayerManager.start(0, 22050);
	    setJingleStream(SoundEffectPlayer.getStream(null));
	    SoundEffectPlayer.soundEffectPlayer = PcmPlayerManager.start(1, 2048);
	    AmbientSound.gameSounds = new Node_Sub15_Sub4();
	    SoundEffectPlayer.soundEffectPlayer.setCurrentPcmStream(AmbientSound.gameSounds);
	    SoundEffectPlayer.resampler = new Resampler(22050, PcmPlayerManager.sampleRate);
	    SongPlayer.method12658();
	}

	public static MidiPcmStream getStream(MidiPcmStream stream) {
	    MidiPcmStream validStream = stream == null ? new MidiPcmStream() : new MidiPcmStream(stream);
	    validStream.load();
	    return validStream;
	}

	public static MidiPcmStream method7272() {
	    return SoundResourceManager.aNodeSub15Sub21735;
	}
}
