package com.jagex.game.runetek5.sound

import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.config.midi.SoundBankCache
import com.jagex.game.runetek5.config.midi.VorbisNode
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack
import com.jagex.game.runetek5.sound.jagtheora.OggKateDecoder
import com.jagex.game.runetek5.sound.player.PcmPlayer
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods

object SoundResourceManager {

    @JvmField var MIDI_INSTRUMENT_PROVIDER: Js5ResourceProvider? = null

    @JvmField var SOUND_EFFECT_PROVIDER: Js5ResourceProvider? = null

    @JvmField var midiPcmStream: MidiPcmStream? = null

    @JvmField var SOUND_MIDI_PROVIDER: Js5ResourceProvider? = null

    @JvmField var musicFileId = 0

    @JvmField var songStartPosition = 0L

    @JvmField var songLoadState = 0

    @JvmField var pendingMidiStream: MidiPcmStream? = null

    @JvmStatic
    fun getVideoSubtitleText(fileId: Int): String? {
        val vorbis = StaticMethods.INDEX36_FILE_CACHE.get(fileId.toLong())
        if (vorbis != null) {
            val kateDecoder = vorbis.vfReference.getKateDecoder()
            if (kateDecoder != null) {
                val playbackTime = vorbis.vfReference.getPlaybackTime()
                if (kateDecoder.getStartTime() <= playbackTime && kateDecoder.getEndTime() >= playbackTime) {
                    return kateDecoder.getSubtitleText()
                }
            }
        }
        return null
    }

    @JvmStatic
    fun discardSongData() {
        midiPcmStream!!.turnOffSound()
        songLoadState = 1
        AreaSoundPlayer.MUSIC_RESOURCE = null
        pendingMidiStream = null
    }

    @JvmStatic
    fun initializePcmResources(midiProvider: Js5ResourceProvider, instrumentProvider: Js5ResourceProvider, soundEffectProvider: Js5ResourceProvider, pcmStream: MidiPcmStream, pcmPlayer: PcmPlayer): Boolean {
        SOUND_MIDI_PROVIDER = midiProvider
        MIDI_INSTRUMENT_PROVIDER = instrumentProvider
        SOUND_EFFECT_PROVIDER = soundEffectProvider
        midiPcmStream = pcmStream
        SongPlayer.pcmPlayer = pcmPlayer
        SongPlayer.channelVolumes = IntArray(16)
        for (channel in 0..15) {
            SongPlayer.channelVolumes!![channel] = 255
        }
        return true
    }

    @JvmStatic
    fun loadPendingSong(): Boolean {
        try {
            if (songLoadState == 2) {
                if (StaticMethods.pendingMusicTrack == null) {
                    StaticMethods.pendingMusicTrack = MusicTrack.loadMusictrack(AreaSoundPlayer.MUSIC_RESOURCE!!, SongPlayer.musicArchiveId, musicFileId)
                    if (StaticMethods.pendingMusicTrack == null) {
                        return false
                    }
                }
                if (StaticMethods.pendingSoundBankCache == null) {
                    StaticMethods.pendingSoundBankCache = SoundBankCache(SOUND_EFFECT_PROVIDER!!, MIDI_INSTRUMENT_PROVIDER!!)
                }
                var targetStream = midiPcmStream
                if (pendingMidiStream != null) {
                    targetStream = pendingMidiStream
                }
                val pendingMusicTrack = StaticMethods.pendingMusicTrack!!
                if (targetStream!!.ready(pendingMusicTrack, SOUND_MIDI_PROVIDER!!, StaticMethods.pendingSoundBankCache!!)) {
                    midiPcmStream = targetStream
                    midiPcmStream!!.resetMusicPatches()
                    if (StaticMethods.songFadeInRate > 0) {
                        songLoadState = 3
                        midiPcmStream!!.setMusicVolume(Math.min(StaticMethods.songFadeInRate, AreaSoundPlayer.SOUND_LEVEL))
                        for (channel in SongPlayer.channelVolumes!!.indices) {
                            midiPcmStream!!.setChannelVolume(channel, SongPlayer.channelVolumes!![channel])
                            SongPlayer.channelVolumes!![channel] = 255
                        }
                    } else {
                        songLoadState = 0
                        midiPcmStream!!.setMusicVolume(AreaSoundPlayer.SOUND_LEVEL)
                        for (channel in SongPlayer.channelVolumes!!.indices) {
                            midiPcmStream!!.setChannelVolume(channel, SongPlayer.channelVolumes!![channel])
                            SongPlayer.channelVolumes!![channel] = 255
                        }
                    }
                    if (pendingMidiStream == null) {
                        if (songStartPosition > 0L) {
                            midiPcmStream!!.setMusicTrackAtPosition(pendingMusicTrack, StaticMethods.songLooping, songStartPosition)
                        } else {
                            midiPcmStream!!.setMusicTrack(pendingMusicTrack, StaticMethods.songLooping)
                        }
                        if (SongPlayer.songTransition != null) {
                            SongPlayer.songTransition!!.transition()
                        }
                    }
                    if (SongPlayer.pcmPlayer != null) {
                        SongPlayer.pcmPlayer!!.currentPcmStream = midiPcmStream
                    }
                    StaticMethods.pendingMusicTrack = null
                    StaticMethods.pendingSoundBankCache = null
                    AreaSoundPlayer.MUSIC_RESOURCE = null
                    songStartPosition = 0L
                    pendingMidiStream = null
                    SongPlayer.songTransition = null
                    return true
                }
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            midiPcmStream!!.turnOffSound()
            SongReference.reset()
        }
        return false
    }
}
