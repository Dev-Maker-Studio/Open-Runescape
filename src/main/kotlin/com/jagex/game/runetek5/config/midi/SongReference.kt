package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.jagtheora.MidiPcmStream
import com.jagex.game.runetek5.sound.jagtheora.MusicTrack
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods

/**
 * Reference to a music track with sound bank caching.
 */
class SongReference(
    @JvmField val musicResource: Js5ResourceProvider,
    @JvmField val musicId: Int
) {

    @JvmField var soundBankCache: SoundBankCache? = null
    @JvmField var loadingStage: Int = 0
    @JvmField val stream: MidiPcmStream = MidiPcmStream()
    @JvmField var musicTrack: MusicTrack? = null

    init {
        stream.load()
    }

    fun getStream(): MidiPcmStream {
        return stream
    }

    fun getMusicTrack(): MusicTrack? {
        return musicTrack
    }

    fun isLoaded(): Boolean {
        return loadingStage == 2
    }

    /**
     * Initializes the song by loading the music track and sound bank.
     *
     * @return True if initialization is complete or progressed
     */
    fun initialize(): Boolean {
        reset()
        if (loadingStage == 0) {
            musicTrack = MusicTrack.loadMusictrack(musicResource, musicId, 0)
            if (musicTrack == null) {
                return false
            }
            soundBankCache = SoundBankCache(
                SoundResourceManager.SOUND_EFFECT_PROVIDER!!,
                SoundResourceManager.MIDI_INSTRUMENT_PROVIDER!!
            )
            loadingStage = 1
        }
        if (loadingStage == 1) {
            if (!stream.ready(musicTrack, SoundResourceManager.SOUND_MIDI_PROVIDER!!, soundBankCache)) {
                return false
            }
            loadingStage = 2
        }
        return true
    }

    companion object {
        @JvmStatic
        @JvmName("method1966")
        fun method1966(): Boolean {
            return SoundResourceManager.anInt1730 != 0 || SoundResourceManager.aNodeSub15Sub21735!!.dataLoaded()
        }

        @JvmStatic
        fun reset() {
            SoundResourceManager.anInt1730 = 0
            StaticMethods.aNodeSub79563 = null
            StaticMethods.aClass2505450 = null
            AreaSoundPlayer.MUSIC_RESOURCE = null
            SoundResourceManager.aNodeSub15Sub27836 = null
        }
    }
}
