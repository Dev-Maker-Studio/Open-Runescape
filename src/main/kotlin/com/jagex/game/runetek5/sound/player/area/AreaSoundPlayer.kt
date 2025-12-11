package com.jagex.game.runetek5.sound.player.area

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement
import com.jagex.game.runetek5.config.midi.AreaSound
import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.movement.CoordGrid
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
    fun method1623() {
        PacketEncoder.method3675()
        var soundIndex = 0
        while (soundIndex < AREA_SOUND_CACHE_SIZE) {
            val sound = AREA_SOUND_CACHE[soundIndex]!!
            var bool3 = false
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
                        var i_4 = 8192
                        val volume: Int
                        if (sound.bitpackedPosition != 0) {
                            val plane = sound.bitpackedPosition shr 24 and 0x3
                            if (plane == PlayerUpdate.MY_PLAYER!!.plane.toInt()) {
                                val maxHearDistance = (sound.bitpackedPosition and 0xff) shl 9
                                val size = PlayerUpdate.MY_PLAYER!!.getSize() shl 8
                                val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                                var startX = sound.bitpackedPosition shr 16 and 0xff
                                var endX = (startX shl 9) + 256 - abs.x.toInt() + size
                                var startY = sound.bitpackedPosition shr 8 and 0xff
                                var endY = (startY shl 9) + 256 - abs.z.toInt() + size
                                var currentDistance = abs(endX) + abs(endY) - 512
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
                                    startX = sound.startX
                                    startY = sound.startZ
                                }
                                if (endX != 0 || endY != 0) {
                                    var angle = -Camera.cameraRoll - (atan2(endX.toDouble(), endY.toDouble()) * 2607.5945876176133).toInt() - 4096 and 0x3fff
                                    if (angle > 8192) {
                                        angle = 16384 - angle
                                    }
                                    val i_16: Int = if (currentDistance <= 0) {
                                        8192
                                    } else if (currentDistance >= 4096) {
                                        16384
                                    } else {
                                        (8192 - currentDistance) / 4096 + 8192
                                    }
                                    i_4 = (16384 - i_16 shr 1) + angle * i_16 / 8192
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
                                sound.areaSoundStream = mixedAudio.getStream(sound.sampleRate, volume, i_4)
                            } else if (sound.spoken()) {
                                sound.areaSoundStream = sound.buffer!!.getStream(sound.sampleRate, volume, i_4)
                            }
                            sound.areaSoundStream!!.setNumLoops(sound.loops - 1)
                            AmbientSound.gameSounds!!.addSubStream(sound.areaSoundStream!!)
                        }
                    }
                } else {
                    bool3 = true
                }
            } else if (!sound.areaSoundStream!!.linked()) {
                bool3 = true
            }
            if (bool3) {
                --AREA_SOUND_CACHE_SIZE
                for (index in soundIndex until AREA_SOUND_CACHE_SIZE) {
                    AREA_SOUND_CACHE[index] = AREA_SOUND_CACHE[index + 1]
                }
                --soundIndex
            }
            soundIndex++
        }
        if (SoundEffectPlayer.soundJinglePlaying && !SongReference.method1966()) {
            if (PreferenceManager.preferences!!.musicVolume!!.getValue() != 0 && currentMusicArchive != -1) {
                if (SoundEffectPlayer.jingleStream != null) {
                    method3690(Resource.MUSIC!!, currentMusicArchive, PreferenceManager.preferences!!.musicVolume!!.getValue(), SoundEffectPlayer.jingleStream!!)
                } else {
                    method13400(Resource.MUSIC!!, currentMusicArchive, PreferenceManager.preferences!!.musicVolume!!.getValue())
                }
            }
            SoundEffectPlayer.soundJinglePlaying = false
            SoundEffectPlayer.jingleStream = null
        } else if (PreferenceManager.preferences!!.musicVolume!!.getValue() != 0 && currentMusicArchive != -1 && !SongReference.method1966()) {
            val musicEndedPacket = PacketEncoder.createPacket(ClientProt.SOUND_EFFECT_MUSIC_ENDED, client.GAME_CONNECTION_CONTEXT.isaac)
            musicEndedPacket.buffer.writeInt(currentMusicArchive)
            client.GAME_CONNECTION_CONTEXT.queuePacket(musicEndedPacket)
            currentMusicArchive = -1
        }
    }

    @JvmStatic
    fun musicSourceUpdate(musicResource: Js5ResourceProvider, musicArchiveId: Int, soundLevel: Int) {
        SoundResourceManager.anInt1730 = 1
        MUSIC_RESOURCE = musicResource
        SongPlayer.musicArchiveId = musicArchiveId
        SoundResourceManager.musicFileId = 0
        SoundResourceManager.aNodeSub15Sub27836 = null
        SOUND_LEVEL = soundLevel
        StaticMethods.aBool1962 = false
        CutsceneBasicMovement.anInt9440 = 2
        SongPlayer.songTransition = null
    }

    @JvmStatic
    fun setVolumeMultiplier(volume: Int) {
        SoundResourceManager.aNodeSub15Sub21735!!.setVolumeMultiplier(volume shr 8)
    }

    /**
     * Plays vorbis sound with volume adjusted by distance from given position
     */
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

    /**
     * Plays vorbis sound with a constant volume
     */
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

    /**
     * Plays synth sound with volume adjusted by distance from given position
     */
    @JvmStatic
    fun playSoundSynth(soundId: Int, timesRepeat: Int, delay: Int, volume: Int, bitpackedPosition: Int, sampleRate: Int): AreaSound? {
        if (PreferenceManager.preferences!!.soundEffectVolume!!.getValue() != 0 && timesRepeat != 0 && AREA_SOUND_CACHE_SIZE < 50 && soundId != -1) {
            val areaSound = AreaSound(1.toByte(), soundId, timesRepeat, delay, volume, bitpackedPosition, sampleRate, null)
            AREA_SOUND_CACHE[++AREA_SOUND_CACHE_SIZE - 1] = areaSound
            return areaSound
        }
        return null
    }

    /**
     * Plays synth sound with a constant volume
     */
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
            AmbientSound.gameSounds!!.unlink(areaSound.areaSoundStream!!)
        } else {
            areaSound.delay = Int.MIN_VALUE
        }
    }

    @JvmStatic
    fun method3690(musicResource: Js5ResourceProvider, archive: Int, volume: Int, class282Sub15Sub25: MidiPcmStream) {
        method13400(musicResource, archive, volume)
        SoundResourceManager.aNodeSub15Sub27836 = class282Sub15Sub25
    }

    @JvmStatic
    fun method13400(musicResource: Js5ResourceProvider, musicArchive: Int, soundLevel: Int) {
        method4827(musicResource, musicArchive, soundLevel)
    }

    @JvmStatic
    fun method4827(musicResource: Js5ResourceProvider, musicArchive: Int, soundLevel: Int) {
        method1859(musicResource, musicArchive, 0, soundLevel, false)
    }

    @JvmStatic
    fun method1859(musicResource: Js5ResourceProvider, musicArchive: Int, musicFile: Int, soundLevel: Int, bool4: Boolean) {
        SoundResourceManager.anInt1730 = 1
        MUSIC_RESOURCE = musicResource
        SongPlayer.musicArchiveId = musicArchive
        SoundResourceManager.musicFileId = musicFile
        SoundResourceManager.aNodeSub15Sub27836 = null
        SOUND_LEVEL = soundLevel
        StaticMethods.aBool1962 = bool4
        CutsceneBasicMovement.anInt9440 = 10000
        SoundResourceManager.aLong1740 = 0
        StaticMethods.anInt4664 = 0
    }

    @JvmStatic
    fun method5961() {
        if (SongPlayer.primarySound != null) {
            val abs = Vector3Pool.vector3From(PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords)
            val base = Resource.mapRegion!!.getBaseCoordinates()
            abs.x += (base.x shl 9).toFloat()
            abs.z += (base.y shl 9).toFloat()
            abs.subtract(SongPlayer.primarySound!!.x.toFloat(), abs.y, SongPlayer.primarySound!!.z.toFloat())
            var magnitude = abs.magnitude()
            abs.pushVectorStack()
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
