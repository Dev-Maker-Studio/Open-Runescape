package com.jagex.game.runetek5.sound.player.song

import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement
import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.sound.AbstractSongTransition
import com.jagex.game.runetek5.sound.SongTransition
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.player.PcmPlayer
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.AreaSoundRange
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods

object SongPlayer {

    @JvmField var musicArchiveId = 0
    @JvmField var songTransition: AbstractSongTransition? = null
    @JvmField var currentSong: SongReference? = null
    @JvmField var secondarySound: AreaSoundRange? = null
    @JvmField var primarySound: AreaSoundRange? = null
    @JvmField var channelVolumes: IntArray? = null
    @JvmField var pcmPlayer: PcmPlayer? = null

    @JvmStatic
    fun playSong(musicArchive: Int, volume: Int, i_2: Int) {
        if (currentSong != null && currentSong!!.isLoaded() && musicArchive == currentSong!!.musicId) {
            play(currentSong, volume)
            AreaSoundPlayer.currentMusicArchive = musicArchive
            currentSong = null
            SoundEffectPlayer.jingleStream = null
            val songLoadedPacket = PacketEncoder.createPacket(ClientProt.SONG_LOADED, Client.GAME_CONNECTION_CONTEXT.isaac)
            songLoadedPacket.buffer.writeInt(-1)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(songLoadedPacket)
        } else {
            var adjustedVolume = volume * PreferenceManager.preferences!!.musicVolume!!.getValue() shr 8
            if (musicArchive == -1 && !SoundEffectPlayer.soundJinglePlaying) {
                SoundResourceManager.discardSongData()
            } else if (musicArchive != -1 && (musicArchive != AreaSoundPlayer.currentMusicArchive || !SongReference.method1966())
                && adjustedVolume != 0 && !SoundEffectPlayer.soundJinglePlaying
            ) {
                transitionToNextSong(i_2, Resource.MUSIC!!, musicArchive, adjustedVolume, SongTransition())
                method12658()
            }
            if (musicArchive != AreaSoundPlayer.currentMusicArchive) {
                SoundEffectPlayer.jingleStream = null
            }
            AreaSoundPlayer.currentMusicArchive = musicArchive
            secondarySound = null
        }
    }

    @JvmStatic
    fun method5731(x: Int, z: Int, near: Int, far: Int, musicArchive: Int, volume: Int) {
        playSong(musicArchive, volume, 100)
        secondarySound = AreaSoundRange(x shl 9, z shl 9, near shl 8, far * 8)
    }

    @JvmStatic
    fun transitionToNextSong(i_0: Int, musicResource: Js5ResourceProvider, musicArchive: Int, soundLevel: Int, transition: AbstractSongTransition) {
        if (i_0 > 0) {
            SoundResourceManager.songLoadState = 1
            AreaSoundPlayer.MUSIC_RESOURCE = musicResource
            musicArchiveId = musicArchive
            SoundResourceManager.musicFileId = 0
            SoundResourceManager.pendingMidiStream = null
            AreaSoundPlayer.SOUND_LEVEL = soundLevel
            StaticMethods.aBool1962 = false
            CutsceneBasicMovement.anInt9440 = SoundResourceManager.midiPcmStream!!.getMusicVolume() / i_0
            if (CutsceneBasicMovement.anInt9440 < 1) {
                CutsceneBasicMovement.anInt9440 = 1
            }
            songTransition = transition
        } else {
            transition.transition()
            AreaSoundPlayer.method13400(musicResource, musicArchive, soundLevel)
        }
    }

    @JvmStatic
    fun method3912() {
        if (primarySound != null) {
            primarySound = null
            AreaSoundPlayer.setVolumeMultiplier(256)
        }
    }

    @JvmStatic
    fun play(songReference: SongReference?, volume: Int) {
        val adjustedVolume = volume * PreferenceManager.preferences!!.musicVolume!!.getValue() shr 8
        if (songReference == null) {
            SoundResourceManager.discardSongData()
        } else {
            playSong(songReference, adjustedVolume)
            method3912()
        }
    }

    @JvmStatic
    fun method12658() {
        method2580(-1, 255)
    }

    @JvmStatic
    fun playSong(song: SongReference, volume: Int) {
        SoundResourceManager.midiPcmStream!!.turnOffSound()
        val stream = song.getStream()
        if (stream != null) {
            pcmPlayer!!.start()
            SoundResourceManager.midiPcmStream = stream
            SoundResourceManager.midiPcmStream!!.setMusicTrack(song.getMusicTrack()!!, false)
            SoundResourceManager.midiPcmStream!!.setMusicVolume(volume)
            if (pcmPlayer != null) {
                pcmPlayer!!.currentPcmStream = SoundResourceManager.midiPcmStream
            }
            SongReference.reset()
        }
    }

    @JvmStatic
    fun method2580(i_0: Int, i_1: Int) {
        if (SoundResourceManager.songLoadState != 0) {
            if (i_0 < 0) {
                for (i_3 in 0 until 16) {
                    channelVolumes!![i_3] = i_1
                }
            } else {
                channelVolumes!![i_0] = i_1
            }
        }
        SoundResourceManager.midiPcmStream!!.setChannelVolume(i_0, i_1)
    }
}
