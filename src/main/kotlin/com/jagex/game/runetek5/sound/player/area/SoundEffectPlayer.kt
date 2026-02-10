package com.jagex.game.runetek5.sound.player.area

import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.midi.MixedAudioBuffer
import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.config.midi.SoundEffect
import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.sound.Resampler
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.jagtheora.AudioBufferStream
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream
import com.jagex.game.runetek5.sound.jagtheora.MixedAudioStream
import com.jagex.game.runetek5.sound.jagtheora.PcmStreamScheduler
import com.jagex.game.runetek5.sound.jagtheora.PcmStream
import com.jagex.game.runetek5.sound.player.PcmPlayer
import com.jagex.game.runetek5.sound.player.PcmPlayerManager
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.vardomain.VarProviders

object SoundEffectPlayer {

	@JvmStatic
	fun method1491(i_0: Int) {
	    if (SoundResourceManager.songLoadState != 0) {
	        AreaSoundPlayer.SOUND_LEVEL = i_0
	    } else {
	        SoundResourceManager.midiPcmStream!!.setMusicVolume(i_0)
	    }
	}

	@JvmStatic
	fun getSoundEffect(resource: Js5ResourceProvider, archiveId: Int, fileId: Int): SoundEffect? {
	    val data = resource.getFile(archiveId, fileId)
	    return if (data == null) null else SoundEffect(Packet(data))
	}

	@JvmStatic
	fun method14675(plane: Int, absX: Int, absZ: Int, step: Int) {
	    var ambient: AmbientSound? = AmbientSound.LOC_AMBIENT_SOUNDS.getFirst() as? AmbientSound
	    while (ambient != null) {
	        method2964(ambient, plane, absX, absZ, step)
	        ambient = AmbientSound.LOC_AMBIENT_SOUNDS.next() as? AmbientSound
	    }
	    var sound: AmbientSound? = AmbientSound.NPC_AMBIENT_SOUNDS.getFirst() as? AmbientSound
	    while (sound != null) {
	        var currentAnimatingType: Byte = 1
	        val bastype = sound.npcEntity!!.getBastype()
	        val npcSequenceId = sound.npcEntity!!.movingSequence.getId()
	        if (npcSequenceId != -1 && !sound.npcEntity!!.movingSequence.hasType) {
	            if (npcSequenceId != bastype.runningSequence && npcSequenceId != bastype.runDir3 && npcSequenceId != bastype.runDir1 && npcSequenceId != bastype.runDir2) {
	                if (npcSequenceId == bastype.teleportSequence || npcSequenceId == bastype.teleDir3 || npcSequenceId == bastype.teleDir1 || npcSequenceId == bastype.teleDir2) {
	                    currentAnimatingType = 3
	                }
	            } else {
	                currentAnimatingType = 2
	            }
	        } else {
	            currentAnimatingType = 0
	        }
	        if (currentAnimatingType.toInt() != sound.animationType) {
	        	val soundEffectId = getSoundEffectId(sound.npcEntity!!)
	            var npc = sound.npcEntity!!.definitions
	            if (npc!!.alternativeIds != null) {
	                npc = npc.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER) ?: npc
	            }
	            if (npc != null && soundEffectId != -1) {
	                if (soundEffectId == sound.soundEffectId && sound.instrumentSoundEffect == npc!!.instrumentSoundEffect) {
	                    sound.animationType = currentAnimatingType.toInt()
	                    sound.ambientSoundVolume = npc.ambientSoundVolume
	                } else {
	                    var replay = false
	                    val soundEffect = sound.soundEffect
	                    if (soundEffect != null) {
	                        sound.ambientSoundVolume -= 512
	                        if (sound.ambientSoundVolume <= 0) {
	                            AmbientSound.gameSounds!!.unlink(soundEffect as PcmStream)
	                            sound.soundEffect = null
	                            replay = true
	                        }
	                    } else {
	                        replay = true
	                    }
	                    if (replay) {
	                        sound.ambientSoundVolume = npc!!.ambientSoundVolume
	                        sound.aNodeSub26Sub1Sub18082 = null
	                        sound.instrumentEffect = null
	                        sound.soundEffectId = soundEffectId
	                        sound.instrumentSoundEffect = npc!!.instrumentSoundEffect
	                        sound.animationType = currentAnimatingType.toInt()
	                    }
	                }
	            } else {
	                sound.soundEffectId = -1
	                sound.instrumentSoundEffect = false
	                sound.animationType = currentAnimatingType.toInt()
	            }
	        }
	        val abs = sound.npcEntity!!.getAbsolutePosition().coords
	        sound.absStartX = abs.x.toInt()
	        sound.absEndX = abs.x.toInt() + (sound.npcEntity!!.getSize() shl 8)
	        sound.absStartY = abs.z.toInt()
	        sound.absEndY = abs.z.toInt() + (sound.npcEntity!!.getSize() shl 8)
	        sound.plane = sound.npcEntity!!.plane.toInt()
	        method2964(sound, plane, absX, absZ, step)
	        sound = AmbientSound.NPC_AMBIENT_SOUNDS.next() as? AmbientSound
	    }
	    var playerSound: AmbientSound? = AmbientSound.PLAYER_AMBIENT_SOUNDS.first() as? AmbientSound
	    while (playerSound != null) {
	        var seqType: Byte = 1
	        val bas = playerSound.playerEntity!!.getBastype()
	        val sequenceId = playerSound.playerEntity!!.movingSequence.getId()
	        if (sequenceId != -1 && !playerSound.playerEntity!!.movingSequence.hasType) {
	            if (sequenceId != bas.runningSequence && sequenceId != bas.runDir3 && sequenceId != bas.runDir1 && sequenceId != bas.runDir2) {
	                if (sequenceId == bas.teleportSequence || sequenceId == bas.teleDir3 || sequenceId == bas.teleDir1 || sequenceId == bas.teleDir2) {
	                    seqType = 3
	                }
	            } else {
	                seqType = 2
	            }
	        } else {
	            seqType = 0
	        }
	        if (seqType.toInt() != playerSound.animationType) {
	        	val soundEffectId = getSoundEffectId(playerSound.playerEntity!!)
	            if (soundEffectId == playerSound.soundEffectId && playerSound.playerEntity!!.instrumentSoundEffect == playerSound.instrumentSoundEffect) {
	                playerSound.ambientSoundVolume = playerSound.playerEntity!!.ambientSoundVolume
	                playerSound.animationType = seqType.toInt()
	            } else {
	                var replay = false
	                val soundEffect = playerSound.soundEffect
	                if (soundEffect != null) {
	                    playerSound.ambientSoundVolume -= 512
	                    if (playerSound.ambientSoundVolume <= 0) {
	                        AmbientSound.gameSounds!!.unlink(soundEffect as PcmStream)
	                        playerSound.soundEffect = null
	                        replay = true
	                    }
	                } else {
	                    replay = true
	                }
	                if (replay) {
	                    playerSound.ambientSoundVolume = playerSound.playerEntity!!.ambientSoundVolume
	                    playerSound.aNodeSub26Sub1Sub18082 = null
	                    playerSound.instrumentEffect = null
	                    playerSound.soundEffectId = soundEffectId
	                    playerSound.instrumentSoundEffect = playerSound.playerEntity!!.instrumentSoundEffect
	                    playerSound.animationType = seqType.toInt()
	                }
	            }
	        }
	        val abs = playerSound.playerEntity!!.getAbsolutePosition().coords
	        playerSound.absStartX = abs.x.toInt()
	        playerSound.absEndX = abs.x.toInt() + (playerSound.playerEntity!!.getSize() shl 8)
	        playerSound.absStartY = abs.z.toInt()
	        playerSound.absEndY = abs.z.toInt() + (playerSound.playerEntity!!.getSize() shl 8)
	        playerSound.plane = playerSound.playerEntity!!.plane.toInt()
	        method2964(playerSound, plane, absX, absZ, step)
	        playerSound = AmbientSound.PLAYER_AMBIENT_SOUNDS.next() as? AmbientSound
	    }
	}

	@JvmStatic
	fun method2964(ambientSound: AmbientSound, plane: Int, absX: Int, absY: Int, step: Int) {
	    if (ambientSound.soundEffectId != -1 || ambientSound.ambientSoundGroupIds != null) {
	        var hearDistance = 0
	        val preferedVolume = ambientSound.ambientSoundVolume * PreferenceManager.preferences!!.ambientSoundVolume!!.getValue() shr 8
	        if (absX > ambientSound.absEndX) {
	            hearDistance += absX - ambientSound.absEndX
	        } else if (absX < ambientSound.absStartX) {
	            hearDistance += ambientSound.absStartX - absX
	        }
	        if (absY > ambientSound.absEndY) {
	            hearDistance += absY - ambientSound.absEndY
	        } else if (absY < ambientSound.absStartY) {
	            hearDistance += ambientSound.absStartY - absY
	        }
	        if (ambientSound.ambientSoundMinHearDistance != 0 && hearDistance - 256 <= ambientSound.ambientSoundMinHearDistance
	        		&& PreferenceManager.preferences!!.ambientSoundVolume!!.getValue() != 0 && ambientSound.plane == plane) {
	            var adjustedHearDistance = hearDistance - 256
	            if (adjustedHearDistance < 0) {
	                adjustedHearDistance = 0
	            }
	            var minHearDistance = ambientSound.ambientSoundMinHearDistance - ambientSound.ambientSoundMaxHearDistance
	            if (minHearDistance < 0) {
	                minHearDistance = ambientSound.ambientSoundMinHearDistance
	            }
	            var volume = preferedVolume
	            val hearDistanceDelta = adjustedHearDistance - ambientSound.ambientSoundMaxHearDistance
	            if (hearDistanceDelta > 0 && minHearDistance > 0) {
	                volume = preferedVolume * (minHearDistance - hearDistanceDelta) / minHearDistance
	            }
	            PlayerUpdate.MY_PLAYER!!.getSize()
	            var i_11 = 8192
	            val i_12 = (ambientSound.absStartX + ambientSound.absEndX) / 2 - absX
	            val i_13 = (ambientSound.absStartY + ambientSound.absEndY) / 2 - absY
	            if (i_12 != 0 || i_13 != 0) {
	                var i_17 = (-Camera.cameraRoll - (Math.atan2(i_12.toDouble(), i_13.toDouble()) * 2607.5945876176133).toInt() - 4096) and 0x3fff
	                if (i_17 > 8192) {
	                    i_17 = 16384 - i_17
	                }
	                val i_18: Int
	                if (adjustedHearDistance <= 0) {
	                    i_18 = 8192
	                } else if (adjustedHearDistance >= 4096) {
	                    i_18 = 16384
	                } else {
	                    i_18 = adjustedHearDistance * 8192 / 4096 + 8192
	                }
	                i_11 = (16384 - i_18 shr 1) + i_17 * i_18 / 8192
	            }
	            var class282Sub15Sub5Sub114: AudioBufferStream?
	            var mixedAudioNode: MixedAudioBuffer
	            var soundEffectStream: MixedAudioStream
	            var soundEffect: SoundEffect?
	            if (ambientSound.soundEffect == null) {
	                if (ambientSound.soundEffectId >= 0) {
	                    val sampleRate = if (ambientSound.minDelay == 256 && ambientSound.maxDelay == 256)
	                    	256 else MathUtils.calculateSampleRate(ambientSound.maxDelay, ambientSound.minDelay)
	                    if (ambientSound.instrumentSoundEffect) {
	                        if (ambientSound.instrumentEffect == null) {
	                            ambientSound.instrumentEffect = MusicSample.list(Resource.MIDI_INSTRUMENT!!, ambientSound.soundEffectId)
	                        }
	                        if (ambientSound.instrumentEffect != null) {
	                            if (ambientSound.aNodeSub26Sub1Sub18082 == null) {
	                                ambientSound.aNodeSub26Sub1Sub18082 = ambientSound.instrumentEffect!!.getAudioBuffer()
	                            }
	                            if (ambientSound.aNodeSub26Sub1Sub18082 != null) {
	                                class282Sub15Sub5Sub114 = ambientSound.aNodeSub26Sub1Sub18082!!.getStream(sampleRate, volume shl 6, i_11) as AudioBufferStream
	                                class282Sub15Sub5Sub114.setNumLoops(-1)
	                                AmbientSound.gameSounds!!.addSubStream(class282Sub15Sub5Sub114)
	                                ambientSound.soundEffect = class282Sub15Sub5Sub114
	                            }
	                        }
	                    } else {
	                        soundEffect = getSoundEffect(Resource.SOUND_EFFECT!!, ambientSound.soundEffectId, 0)
	                        if (soundEffect != null) {
	                            mixedAudioNode = soundEffect.getMixedAudio().resample(resampler!!)
	                            soundEffectStream = mixedAudioNode.getStream(sampleRate, volume shl 6, i_11) as MixedAudioStream
	                            soundEffectStream.setNumLoops(-1)
	                            AmbientSound.gameSounds!!.addSubStream(soundEffectStream)
	                            ambientSound.soundEffect = soundEffectStream
	                        }
	                    }
	                }
	            } else {
	                ambientSound.soundEffect!!.setVolume(volume)
	                ambientSound.soundEffect!!.setPanPosition(i_11)
	            }
	            if (ambientSound.aNodeSub15Sub58096 == null) {
	                if (ambientSound.ambientSoundGroupIds != null && run { ambientSound.step -= step; ambientSound.step <= 0 }) {
	                    val sampleRate = if (ambientSound.minDelay == 256 && ambientSound.maxDelay == 256)
	                    	256 else (Math.random() * (ambientSound.minDelay - ambientSound.maxDelay)).toInt() + ambientSound.maxDelay
	                    if (ambientSound.instrumentAmbientSound) {
	                        if (ambientSound.aNodeSub188087 == null) {
	                            val random = (Math.random() * ambientSound.ambientSoundGroupIds!!.size).toInt()
	                            ambientSound.aNodeSub188087 = MusicSample.list(Resource.MIDI_INSTRUMENT!!, ambientSound.ambientSoundGroupIds!![random])
	                        }
	                        if (ambientSound.aNodeSub188087 != null) {
	                            if (ambientSound.aNodeSub26Sub1Sub18106 == null) {
	                                ambientSound.aNodeSub26Sub1Sub18106 = ambientSound.aNodeSub188087!!.getAudioBuffer()
	                            }
	                            if (ambientSound.aNodeSub26Sub1Sub18106 != null) {
	                                class282Sub15Sub5Sub114 = ambientSound.aNodeSub26Sub1Sub18106!!.getStream(sampleRate, volume shl 6, i_11) as AudioBufferStream
	                                class282Sub15Sub5Sub114.setNumLoops(0)
	                                AmbientSound.gameSounds!!.addSubStream(class282Sub15Sub5Sub114)
	                                ambientSound.aNodeSub15Sub58096 = class282Sub15Sub5Sub114
	                                ambientSound.step = ambientSound.soundMinStep + (Math.random() * (ambientSound.soundMaxStep - ambientSound.soundMinStep)).toInt()
	                            }
	                        }
	                    } else {
	                        val random = (Math.random() * ambientSound.ambientSoundGroupIds!!.size).toInt()
	                        soundEffect = getSoundEffect(Resource.SOUND_EFFECT!!, ambientSound.ambientSoundGroupIds!![random], 0)
	                        if (soundEffect != null) {
	                            mixedAudioNode = soundEffect.getMixedAudio().resample(resampler!!)
	                            soundEffectStream = mixedAudioNode.getStream(sampleRate, volume shl 6, i_11) as MixedAudioStream
	                            soundEffectStream.setNumLoops(0)
	                            AmbientSound.gameSounds!!.addSubStream(soundEffectStream)
	                            ambientSound.aNodeSub15Sub58096 = soundEffectStream
	                            ambientSound.step = ambientSound.soundMinStep + (Math.random() * (ambientSound.soundMaxStep - ambientSound.soundMinStep)).toInt()
	                        }
	                    }
	                }
	            } else {
	                ambientSound.aNodeSub15Sub58096!!.setVolume(volume)
	                ambientSound.aNodeSub15Sub58096!!.setPanPosition(i_11)
	                if (!ambientSound.aNodeSub15Sub58096!!.linked()) {
	                    ambientSound.aNodeSub15Sub58096 = null
	                    ambientSound.aNodeSub188087 = null
	                    ambientSound.aNodeSub26Sub1Sub18106 = null
	                }
	            }
	        } else {
	            val soundEffect = ambientSound.soundEffect
	            if (soundEffect != null) {
	                AmbientSound.gameSounds!!.unlink(soundEffect as PcmStream)
	                ambientSound.soundEffect = null
	                ambientSound.aNodeSub26Sub1Sub18082 = null
	                ambientSound.instrumentEffect = null
	            }
	            val aNodeSub15Sub58096 = ambientSound.aNodeSub15Sub58096
	            if (aNodeSub15Sub58096 != null) {
	                AmbientSound.gameSounds!!.unlink(aNodeSub15Sub58096 as PcmStream)
	                ambientSound.aNodeSub15Sub58096 = null
	                ambientSound.aNodeSub188087 = null
	                ambientSound.aNodeSub26Sub1Sub18106 = null
	            }
	        }
	    }
	}

	@JvmStatic
	fun getSoundEffectId(npc: NpcEntity): Int {
	    var npcType = npc.definitions
	    if (npcType!!.alternativeIds != null) {
	        npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER) ?: return -1
	    }
	    var soundEffect = npcType!!.idleSoundEffect
	    val basType = npc.getBastype()
	    val movingSequenceId = npc.movingSequence.getId()
	    if (movingSequenceId != -1 && !npc.movingSequence.hasType) {
	        if (movingSequenceId != basType.runningSequence && movingSequenceId != basType.runDir3 && movingSequenceId != basType.runDir1 && movingSequenceId != basType.runDir2) {
	            if (movingSequenceId == basType.teleportSequence || movingSequenceId == basType.teleDir3 || movingSequenceId == basType.teleDir1 || movingSequenceId == basType.teleDir2) {
	                soundEffect = npcType!!.teleportSoundEffect
	            }
	        } else {
	            soundEffect = npcType!!.runningSoundEffect
	        }
	    } else {
	        soundEffect = npcType!!.walkingSoundEffect
	    }
	    return soundEffect
	}

	@JvmStatic
	fun getSoundEffectId(player: PlayerEntity): Int {
	    var soundEffectId = player.defaultSoundEffectId
	    val bastype = player.getBastype()
	    val sequenceId = player.movingSequence.getId()
	    if (sequenceId != -1 && !player.movingSequence.hasType) {
	        if (sequenceId != bastype.runningSequence && sequenceId != bastype.runDir3 && sequenceId != bastype.runDir1 && sequenceId != bastype.runDir2) {
	            if (sequenceId == bastype.teleportSequence || sequenceId == bastype.teleDir3 || sequenceId == bastype.teleDir1 || sequenceId == bastype.teleDir2) {
	                soundEffectId = player.teleportingSoundEffectId
	            }
	        } else {
	            soundEffectId = player.runningSoundEffectId
	        }
	    } else {
	        soundEffectId = player.walkingSoundEffectId
	    }
	    return soundEffectId
	}

	@JvmStatic
	fun playSoundJingle(jingleId: Int, volume: Int) {
	    val adjustedVolume = volume * PreferenceManager.preferences!!.musicVolume!!.getValue() shr 8
	    if (adjustedVolume != 0 && jingleId != -1) {
	        if (!soundJinglePlaying && AreaSoundPlayer.currentMusicArchive != -1 && SongReference.method1966() && !StaticMethods.method6140()) {
	            jingleStream = method7272()
	            SongPlayer.method3912()
	            val jingleStreamImpl = getStream(jingleStream)
	            setJingleStream(jingleStreamImpl)
	        }
	        AreaSoundPlayer.method13400(Resource.JINGLE!!, jingleId, adjustedVolume)
	        SongPlayer.method2580(-1, 255)
	        soundJinglePlaying = true
	    }
	}

	@JvmField
	var jinglePlayer: PcmPlayer? = null

	@JvmField
	var soundEffectPlayer: PcmPlayer? = null

	@JvmField
	var resampler: Resampler? = null

	@JvmField
	var soundJinglePlaying: Boolean = false

	@JvmField
	var jingleStream: MidiPcmStream? = null

	@JvmStatic
	fun updatePlayers() {
	    jinglePlayer?.cycle()
	    soundEffectPlayer?.cycle()
	}

	@JvmStatic
	fun shutdownPlayers() {
	    jinglePlayer?.shutdown()
	    soundEffectPlayer?.shutdown()
	}

	@JvmStatic
	fun setJingleStream(stream: MidiPcmStream) {
	    jinglePlayer!!.currentPcmStream = stream
	    SoundResourceManager.initializePcmResources(Resource.SOUND_MIDI!!, Resource.MIDI_INSTRUMENT!!, Resource.SOUND_EFFECT!!, stream, jinglePlayer!!)
	}

	@JvmStatic
	fun reload() {
	    shutdownPlayers()
	    PcmPlayerManager.setStereo(PreferenceManager.preferences!!.monoStereo!!.getValue() == 1)
	    jinglePlayer = PcmPlayerManager.start(0, 22050)
	    setJingleStream(getStream(null))
	    soundEffectPlayer = PcmPlayerManager.start(1, 2048)
	    soundEffectPlayer!!.currentPcmStream = AmbientSound.gameSounds
	}

	@JvmStatic
	fun initializeAudio() {
	    PcmPlayerManager.setStereo(PreferenceManager.preferences!!.monoStereo!!.getValue() == 1)
	    jinglePlayer = PcmPlayerManager.start(0, 22050)
	    setJingleStream(getStream(null))
	    soundEffectPlayer = PcmPlayerManager.start(1, 2048)
	    AmbientSound.gameSounds = PcmStreamScheduler()
	    soundEffectPlayer!!.currentPcmStream = AmbientSound.gameSounds
	    resampler = Resampler(22050, PcmPlayerManager.sampleRate)
	    SongPlayer.method12658()
	}

	@JvmStatic
	fun getStream(stream: MidiPcmStream?): MidiPcmStream {
	    val validStream = if (stream == null) MidiPcmStream() else MidiPcmStream(stream)
	    validStream.load()
	    return validStream
	}

	@JvmStatic
	fun method7272(): MidiPcmStream {
	    return SoundResourceManager.midiPcmStream!!
	}
}
