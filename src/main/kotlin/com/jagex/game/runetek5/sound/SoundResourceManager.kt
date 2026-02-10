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
    fun getVideoSubtitleText(i_0: Int): String? {
        val vorbis = StaticMethods.INDEX36_FILE_CACHE.get(i_0.toLong())
        if (vorbis != null) {
            val class282Sub41Sub23 = vorbis.vfReference.getKateDecoder()
            if (class282Sub41Sub23 != null) {
                val d_4 = vorbis.vfReference.getPlaybackTime()
                if (class282Sub41Sub23.getStartTime() <= d_4 && class282Sub41Sub23.getEndTime() >= d_4) {
                    return class282Sub41Sub23.getSubtitleText()
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
    fun initializePcmResources(index0: Js5ResourceProvider, index1: Js5ResourceProvider, index2: Js5ResourceProvider, class282Sub15Sub23: MidiPcmStream, class2534: PcmPlayer): Boolean {
        SOUND_MIDI_PROVIDER = index0
        MIDI_INSTRUMENT_PROVIDER = index1
        SOUND_EFFECT_PROVIDER = index2
        midiPcmStream = class282Sub15Sub23
        SongPlayer.pcmPlayer = class2534
        SongPlayer.channelVolumes = IntArray(16)
        for (i_6 in 0..15) {
            SongPlayer.channelVolumes!![i_6] = 255
        }
        return true
    }

    @JvmStatic
    fun loadPendingSong(): Boolean {
        try {
            if (songLoadState == 2) {
                if (StaticMethods.aNodeSub79563 == null) {
                    StaticMethods.aNodeSub79563 = MusicTrack.loadMusictrack(AreaSoundPlayer.MUSIC_RESOURCE!!, SongPlayer.musicArchiveId, musicFileId)
                    if (StaticMethods.aNodeSub79563 == null) {
                        return false
                    }
                }
                if (StaticMethods.aClass2505450 == null) {
                    StaticMethods.aClass2505450 = SoundBankCache(SOUND_EFFECT_PROVIDER!!, MIDI_INSTRUMENT_PROVIDER!!)
                }
                var class282Sub15Sub22 = midiPcmStream
                if (pendingMidiStream != null) {
                    class282Sub15Sub22 = pendingMidiStream
                }
                val aNodeSub79563 = StaticMethods.aNodeSub79563!!
                if (class282Sub15Sub22!!.ready(aNodeSub79563, SOUND_MIDI_PROVIDER!!, StaticMethods.aClass2505450!!)) {
                    midiPcmStream = class282Sub15Sub22
                    midiPcmStream!!.resetMusicPatches()
                    if (StaticMethods.anInt4664 > 0) {
                        songLoadState = 3
                        midiPcmStream!!.setMusicVolume(Math.min(StaticMethods.anInt4664, AreaSoundPlayer.SOUND_LEVEL))
                        for (i_3 in SongPlayer.channelVolumes!!.indices) {
                            midiPcmStream!!.setChannelVolume(i_3, SongPlayer.channelVolumes!![i_3])
                            SongPlayer.channelVolumes!![i_3] = 255
                        }
                    } else {
                        songLoadState = 0
                        midiPcmStream!!.setMusicVolume(AreaSoundPlayer.SOUND_LEVEL)
                        for (i_3 in SongPlayer.channelVolumes!!.indices) {
                            midiPcmStream!!.setChannelVolume(i_3, SongPlayer.channelVolumes!![i_3])
                            SongPlayer.channelVolumes!![i_3] = 255
                        }
                    }
                    if (pendingMidiStream == null) {
                        if (songStartPosition > 0L) {
                            midiPcmStream!!.setMusicTrackAtPosition(aNodeSub79563, StaticMethods.aBool1962, songStartPosition)
                        } else {
                            midiPcmStream!!.setMusicTrack(aNodeSub79563, StaticMethods.aBool1962)
                        }
                        if (SongPlayer.songTransition != null) {
                            SongPlayer.songTransition!!.transition()
                        }
                    }
                    if (SongPlayer.pcmPlayer != null) {
                        SongPlayer.pcmPlayer!!.currentPcmStream = midiPcmStream
                    }
                    StaticMethods.aNodeSub79563 = null
                    StaticMethods.aClass2505450 = null
                    AreaSoundPlayer.MUSIC_RESOURCE = null
                    songStartPosition = 0L
                    pendingMidiStream = null
                    SongPlayer.songTransition = null
                    return true
                }
            }
        } catch (exception4: Exception) {
            exception4.printStackTrace()
            midiPcmStream!!.turnOffSound()
            SongReference.reset()
        }
        return false
    }
}
