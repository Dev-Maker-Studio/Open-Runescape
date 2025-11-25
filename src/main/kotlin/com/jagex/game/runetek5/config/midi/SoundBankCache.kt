package com.jagex.game.runetek5.config.midi

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.config.midi.instrument.MusicSample
import com.jagex.game.runetek5.js5.Js5ResourceProvider
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer

/**
 *
 * @author Brainpower - 19/04/2022
 *
 */
class SoundBankCache(
    val soundEffectProvider: Js5ResourceProvider,
    val musicSampleProvider: Js5ResourceProvider
) {

    @JvmField val musicSampleCache = IterableNodeMap<MusicSample>(256)

    @JvmField val audioBufferCache = IterableNodeMap<AudioBuffer>(256)

    @JvmField val mixedAudioCache = IterableNodeMap<MixedAudioBuffer>(256)

    fun getSoundEffect(archiveId: Int, fileId: Int, sampleRates: IntArray?): MixedAudioBuffer? {
        var hash = fileId xor (archiveId shl 4 and 0xffff or (archiveId ushr 12))
        hash = hash or (archiveId shl 16)
        val key = hash.toLong()
        var mixedAudio = mixedAudioCache.get(key)
        if (mixedAudio != null) {
            return mixedAudio
        }
        if (sampleRates != null && sampleRates[0] <= 0) {
            return null
        }
        val effect = SoundEffectPlayer.getSoundEffect(soundEffectProvider, archiveId, fileId) ?: return null
        mixedAudio = effect.getMixedAudio()
        mixedAudioCache.put(mixedAudio, key)
        if (sampleRates != null) {
            sampleRates[0] -= mixedAudio.samples.size
        }
        return mixedAudio
    }

    fun getMusicSample(archiveId: Int, fileId: Int, sampleRates: IntArray?): AudioBuffer? {
        var hash = fileId xor (archiveId shl 4 and 0xffff or (archiveId ushr 12))
        hash = hash or (archiveId shl 16)
        val key = hash.toLong() xor 0x100000000L
        var audioBuffer = audioBufferCache.get(key)
        if (audioBuffer != null) {
            return audioBuffer
        }
        if (sampleRates != null && sampleRates[0] <= 0) {
            return null
        }
        var musicSample = musicSampleCache.get(key)
        if (musicSample == null) {
            musicSample = MusicSample.list(musicSampleProvider, archiveId, fileId) ?: return null
            musicSampleCache.put(musicSample, key)
        }
        audioBuffer = musicSample.getAudioBuffer() ?: return null
        musicSample.unlink()
        audioBufferCache.put(audioBuffer, key)
        return audioBuffer
    }

    fun listMusic(archiveOrFileId: Int, sampleRates: IntArray?): AudioBuffer? {
        return if (musicSampleProvider.getArchiveCount() == 1) {
            getMusicSample(0, archiveOrFileId, sampleRates)
        } else if (musicSampleProvider.getArchiveFileCount(archiveOrFileId) == 1) {
            getMusicSample(archiveOrFileId, 0, sampleRates)
        } else {
            throw RuntimeException()
        }
    }

    fun listEffect(archiveOrFileId: Int, sampleRates: IntArray?): MixedAudioBuffer? {
        return if (soundEffectProvider.getArchiveCount() == 1) {
            getSoundEffect(0, archiveOrFileId, sampleRates)
        } else if (soundEffectProvider.getArchiveFileCount(archiveOrFileId) == 1) {
            getSoundEffect(archiveOrFileId, 0, sampleRates)
        } else {
            throw RuntimeException()
        }
    }
}
