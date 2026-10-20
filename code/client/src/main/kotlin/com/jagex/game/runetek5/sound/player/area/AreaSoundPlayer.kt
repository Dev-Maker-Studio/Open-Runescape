package com.jagex.game.runetek5.sound.player.area

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement
import com.jagex.game.runetek5.config.midi.AreaSound
import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import kotlin.math.abs
import kotlin.math.atan2

object AreaSoundPlayer {

    @JvmField
    var currentMusicArchive: Int = -1

    @JvmField
    var titlescreenMusicArchive: Int = -1

    @JvmField
    var AREA_SOUND_CACHE_SIZE: Int = 0

    @JvmField
    var AREA_SOUND_CACHE: Array<AreaSound?> = arrayOfNulls(50)

    @JvmField
    var SOUND_LEVEL: Int = 0

    @JvmField
    var MUSIC_RESOURCE: Js5ResourceProvider? = null

    @JvmStatic
    fun initialize() {
        AREA_SOUND_CACHE = arrayOfNulls(50)
        AREA_SOUND_CACHE_SIZE = 0
    }

    @JvmStatic
    fun updateAreaSounds() {
        PacketEncoder.checkSongLoaded()
        var soundIndex = 0
        while (soundIndex < AREA_SOUND_CACHE_SIZE) {
            val sound = AREA_SOUND_CACHE[soundIndex]!!
            var shouldRemove = false
            if (sound.areaSoundStream == null) {
                if (--sound.delay >= (if (sound.spoken()) -1500 else -10)) {
                    if (sound.type.toInt() == 1 && sound.soundEffect == null) {
                        sound.soundEffect = SoundEffectPlayer.getSoundEffect(Resource.SOUND_EFFECT!!, sound.soundId, 0)
                        if (sound.soundEffect == null) {
                            soundIndex++
                            continue
                        }
                        sound.delay += sound.soundEffect!!.getDelay()
                    } else if (sound.spoken() && (sound.midiInstrument == null || sound.buffer == null)) {
                        if (sound.midiInstrument == null) {
                            sound.midiInstrument = MusicSample.list(Resource.MIDI_INSTRUMENT!!, sound.soundId)
                        }
                        if (sound.midiInstrument == null) {
                            soundIndex++
                            continue
                        }
                        if (sound.buffer == null) {
                            sound.buffer = sound.midiInstrument!!.getAudioBuffer()
                            if (sound.buffer == null) {
                                soundIndex++
                                continue
                            }
                        }
                    }
                    if (sound.delay < 0) {
                        var panValue = 8192
                        val volume: Int
                        if (sound.bitpackedPosition != 0) {
                            val plane = sound.bitpackedPosition shr 24 and 0x3
                            if (plane == PlayerUpdate.MY_PLAYER!!.plane.toInt()) {
                                val maxHearDistance = (sound.bitpackedPosition and 0xff) shl 9
                                val entitySize = PlayerUpdate.MY_PLAYER!!.getSize() shl 8
                                val playerPos = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                                var tileX = sound.bitpackedPosition shr 16 and 0xff
                                var deltaX = (tileX shl 9) + 256 - playerPos.x.toInt() + entitySize
                                var tileY = sound.bitpackedPosition shr 8 and 0xff
                                var deltaY = (tileY shl 9) + 256 - playerPos.z.toInt() + entitySize
                                var currentDistance = abs(deltaX) + abs(deltaY) - 512
                                if (currentDistance > maxHearDistance) {
                                    sound.delay = -99999
                                    soundIndex++
                                    continue
                                }
                                if (currentDistance < 0) {
                                    currentDistance = 0
                                }
                                volume = (maxHearDistance - currentDistance) * PreferenceManager.preferences!!.ambientSoundVolume!!.getValue() * sound.volume / maxHearDistance shr 2
                                if (sound.startX != -1) {
                                    tileX = sound.startX
                                    tileY = sound.startZ
                                }
                                if (deltaX != 0 || deltaY != 0) {
                                    var angle = -Camera.cameraRoll - (atan2(deltaX.toDouble(), deltaY.toDouble()) * 2607.5945876176133).toInt() - 4096 and 0x3fff
                                    if (angle > 8192) {
                                        angle = 16384 - angle
                                    }
                                    val stereoWidth: Int = if (currentDistance <= 0) {
                                        8192
                                    } else if (currentDistance >= 4096) {
                                        16384
                                    } else {
                                        currentDistance * 8192 / 4096 + 8192
                                    }
                                    panValue = (16384 - stereoWidth shr 1) + angle * stereoWidth / 8192
                                }
                            } else {
                                volume = 0
                            }
                        } else {
                            volume = sound.volume * (if (sound.type.toInt() == 3)
                                PreferenceManager.preferences!!.voiceOverVolume!!.getValue()
                            else
                                PreferenceManager.preferences!!.soundEffectVolume!!.getValue()) shr 2
                        }
                        if (volume > 0) {
                            if (sound.type.toInt() == 1) {
                                val mixedAudio = sound.soundEffect!!.getMixedAudio().resample(SoundEffectPlayer.resampler!!)
                                sound.areaSoundStream = mixedAudio.getStream(sound.sampleRate, volume, panValue)
                            } else if (sound.spoken()) {
                                sound.areaSoundStream = sound.buffer!!.getStream(sound.sampleRate, volume, panValue)
                            }
                            sound.areaSoundStream!!.setNumLoops(sound.loops - 1)
                            AmbientSound.gameSounds!!.addSubStream(sound.areaSoundStream!!)
                        }
                    }
                } else {
                    shouldRemove = true
                }
            } else if (!sound.areaSoundStream!!.linked()) {
                shouldRemove = true
            }
            if (shouldRemove) {
                --AREA_SOUND_CACHE_SIZE
                for (index in soundIndex until AREA_SOUND_CACHE_SIZE) {
                    AREA_SOUND_CACHE[index] = AREA_SOUND_CACHE[index + 1]
                }
                --soundIndex
            }
            soundIndex++
        }
        if (SoundEffectPlayer.soundJinglePlaying && !SongReference.isSongLoading()) {
            if (PreferenceManager.preferences!!.musicVolume!!.getValue() != 0 && currentMusicArchive != -1) {
                if (SoundEffectPlayer.jingleStream != null) {
                    playMusicWithTransition(Resource.MUSIC!!, currentMusicArchive, PreferenceManager.preferences!!.musicVolume!!.getValue(), SoundEffectPlayer.jingleStream!!)
                } else {
                    playMusic(Resource.MUSIC!!, currentMusicArchive, PreferenceManager.preferences!!.musicVolume!!.getValue())
                }
            }
            SoundEffectPlayer.soundJinglePlaying = false
            SoundEffectPlayer.jingleStream = null
        } else if (PreferenceManager.preferences!!.musicVolume!!.getValue() != 0 && currentMusicArchive != -1 && !SongReference.isSongLoading()) {
            val musicEndedPacket = PacketEncoder.createPacket(ClientProt.SOUND_EFFECT_MUSIC_ENDED, Client.GAME_CONNECTION_CONTEXT.isaac)
            musicEndedPacket.buffer.writeInt(currentMusicArchive)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(musicEndedPacket)
            currentMusicArchive = -1
        }
    }

    @JvmStatic
    fun musicSourceUpdate(musicResource: Js5ResourceProvider, musicArchiveId: Int, soundLevel: Int) {
        SoundResourceManager.songLoadState = 1
        MUSIC_RESOURCE = musicResource
        SongPlayer.musicArchiveId = musicArchiveId
        SoundResourceManager.musicFileId = 0
        SoundResourceManager.pendingMidiStream = null
        SOUND_LEVEL = soundLevel
        StaticMethods.songLooping = false
        CutsceneBasicMovement.musicFadeStep = 2
        SongPlayer.songTransition = null
    }

    @JvmStatic
    fun setVolumeMultiplier(volume: Int) {
        SoundResourceManager.midiPcmStream!!.setVolumeMultiplier(volume shr 8)
    }

    @JvmStatic
    fun playSoundVorbis(soundId: Int, timesRepeated: Int, delay: Int, volume: Int, bitpackedPosition: Int, voiceEffect: Boolean, sampleRate: Int): AreaSound? {
        if ((if (voiceEffect) PreferenceManager.preferences!!.voiceOverVolume!!.getValue()
            else PreferenceManager.preferences!!.soundEffectVolume!!.getValue()) != 0 && timesRepeated != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
            val areaSound = AreaSound((if (voiceEffect) 3 else 2).toByte(), soundId, timesRepeated, delay, volume, bitpackedPosition, sampleRate, null)
            AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound
            return areaSound
        }
        return null
    }

    @JvmStatic
    fun playSoundVorbis(soundId: Int, timesRepeated: Int, delay: Int, volume: Int, voiceEffect: Boolean, sampleRate: Int): AreaSound? {
        if ((if (voiceEffect) PreferenceManager.preferences!!.voiceOverVolume!!.getValue()
            else PreferenceManager.preferences!!.soundEffectVolume!!.getValue()) != 0 && timesRepeated != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
            val areaSound = AreaSound((if (voiceEffect) 3 else 2).toByte(), soundId, timesRepeated, delay, volume, 0, sampleRate, null)
            AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound
            return areaSound
        }
        return null
    }

    @JvmStatic
    fun playSoundSynth(soundId: Int, timesRepeat: Int, delay: Int, volume: Int, bitpackedPosition: Int, sampleRate: Int): AreaSound? {
        if (PreferenceManager.preferences!!.soundEffectVolume!!.getValue() != 0 && timesRepeat != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
            val areaSound = AreaSound(1.toByte(), soundId, timesRepeat, delay, volume, bitpackedPosition, sampleRate, null)
            AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound
            return areaSound
        }
        return null
    }

    @JvmStatic
    fun playSoundSynth(soundId: Int, timesRepeat: Int, delay: Int, volume: Int, sampleRate: Int): AreaSound? {
        if (PreferenceManager.preferences!!.soundEffectVolume!!.getValue() != 0 && timesRepeat != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
            val areaSound = AreaSound(1.toByte(), soundId, timesRepeat, delay, volume, 0, sampleRate, null)
            AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound
            return areaSound
        }
        return null
    }

    @JvmStatic
    fun resetSounds() {
        for (index in 0 until AREA_SOUND_CACHE_SIZE) {
            val areaSound = AREA_SOUND_CACHE[index]
            if (areaSound!!.type.toInt() == 3) {
                resetAreaSound(areaSound)
            }
        }
    }

    @JvmStatic
    fun resetAreaSound(areaSound: AreaSound) {
        if (areaSound.areaSoundStream != null) {
            AmbientSound.gameSounds!!.removeSubStream(areaSound.areaSoundStream!!)
        } else {
            areaSound.delay = Int.MIN_VALUE
        }
    }

    @JvmStatic
    fun playMusicWithTransition(musicResource: Js5ResourceProvider, archive: Int, volume: Int, transitionStream: MidiPcmStream) {
        playMusic(musicResource, archive, volume)
        SoundResourceManager.pendingMidiStream = transitionStream
    }

    @JvmStatic
    fun playMusic(musicResource: Js5ResourceProvider, musicArchive: Int, soundLevel: Int) {
        playMusicFromStart(musicResource, musicArchive, soundLevel)
    }

    @JvmStatic
    fun playMusicFromStart(musicResource: Js5ResourceProvider, musicArchive: Int, soundLevel: Int) {
        loadAndPlayMusic(musicResource, musicArchive, 0, soundLevel, false)
    }

    @JvmStatic
    fun loadAndPlayMusic(musicResource: Js5ResourceProvider, musicArchive: Int, musicFile: Int, soundLevel: Int, looping: Boolean) {
        SoundResourceManager.songLoadState = 1
        MUSIC_RESOURCE = musicResource
        SongPlayer.musicArchiveId = musicArchive
        SoundResourceManager.musicFileId = musicFile
        SoundResourceManager.pendingMidiStream = null
        SOUND_LEVEL = soundLevel
        StaticMethods.songLooping = looping
        CutsceneBasicMovement.musicFadeStep = 10000
        SoundResourceManager.songStartPosition = 0
        StaticMethods.songFadeInRate = 0
    }

    @JvmStatic
    fun updateSpatialMusicVolume() {
        if (SongPlayer.primarySound != null) {
            val playerPos = Vector3Pool.vector3From(PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords)
            val base = Resource.mapRegion!!.getBaseCoordinates()
            playerPos.x += (base.x shl 9).toFloat()
            playerPos.z += (base.y shl 9).toFloat()
            playerPos.subtract(SongPlayer.primarySound!!.x.toFloat(), playerPos.y, SongPlayer.primarySound!!.z.toFloat())
            var magnitude = playerPos.magnitude()
            playerPos.pushVectorStack()
            val volumeMultiplier: Float = if (magnitude < SongPlayer.primarySound!!.near) {
                1.0f
            } else if (magnitude > SongPlayer.primarySound!!.far) {
                0.0f
            } else {
                magnitude -= SongPlayer.primarySound!!.near
                1.0f - magnitude / SongPlayer.primarySound!!.range
            }
            setVolumeMultiplier((volumeMultiplier * 256.0f).toInt())
        }
    }
}
