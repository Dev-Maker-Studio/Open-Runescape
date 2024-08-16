package com.jagex.game.runetek5.config.midi;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.config.midi.instrument.MusicSample;
import com.jagex.game.runetek5.js5.Js5ResourceProvider;
import com.jagex.game.runetek5.sound.jagtheora.AudioBuffer;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;

/**
 * 
 * @author Brainpower - 19/04/2022
 *
 */
public class SoundBankCache {

    public final IterableNodeMap<MusicSample> musicSampleCache = new IterableNodeMap<MusicSample>(256);

    public final IterableNodeMap<AudioBuffer> audioBufferCache = new IterableNodeMap<AudioBuffer>(256);

    public final IterableNodeMap<MixedAudioBuffer> mixedAudioCache = new IterableNodeMap<MixedAudioBuffer>(256);

    public final Js5ResourceProvider soundEffectProvider;

    public final Js5ResourceProvider musicSampleProvider;

    public SoundBankCache(Js5ResourceProvider soundEffectProvider, Js5ResourceProvider midiInstrumentProvider) {
        this.soundEffectProvider = soundEffectProvider;
        this.musicSampleProvider = midiInstrumentProvider;
    }

    public MixedAudioBuffer getSoundEffect(int archiveId, int fileId, int[] sampleRates) {
        int hash = fileId ^ (archiveId << 4 & 0xffff | archiveId >>> 12);
        hash |= archiveId << 16;
        long key = hash;
        MixedAudioBuffer mixedAudio = this.mixedAudioCache.get(key);
        if (mixedAudio != null) {
            return mixedAudio;
        } 
        if (sampleRates != null && sampleRates[0] <= 0) {
            return null;
        } 
        SoundEffect effect = SoundEffectPlayer.getSoundEffect(this.soundEffectProvider, archiveId, fileId);
        if (effect == null) {
            return null;
        } 
        mixedAudio = effect.getMixedAudio();
        this.mixedAudioCache.put(mixedAudio, key);
        if (sampleRates != null) {
            sampleRates[0] -= mixedAudio.samples.length;
        }
        return mixedAudio;
    }

    public AudioBuffer getMusicSample(int archiveId, int fileId, int[] ints_3) {
        int hash = fileId ^ (archiveId << 4 & 0xffff | archiveId >>> 12);
        hash |= archiveId << 16;
        long key = hash ^ 0x100000000L;
        AudioBuffer audioBuffer = this.audioBufferCache.get(key);
        if (audioBuffer != null) {
            return audioBuffer;
        } 
        if (ints_3 != null && ints_3[0] <= 0) {
            return null;
        } 
        MusicSample musicSample = this.musicSampleCache.get(key);
        if (musicSample == null) {
            musicSample = MusicSample.list(this.musicSampleProvider, archiveId, fileId);
            if (musicSample == null) {
                return null;
            }
            this.musicSampleCache.put(musicSample, key);
        }
        audioBuffer = musicSample.getAudioBuffer();
        if (audioBuffer == null) {
            return null;
        } 
        musicSample.unlink();
        this.audioBufferCache.put(audioBuffer, key);
        return audioBuffer;
    }

    public AudioBuffer listMusic(int archiveOrFileId, int[] ints_2) {
        if (this.musicSampleProvider.getArchiveCount() == 1) {
            return getMusicSample(0, archiveOrFileId, ints_2);
        } else if (this.musicSampleProvider.getArchiveFileCount(archiveOrFileId) == 1) {
            return getMusicSample(archiveOrFileId, 0, ints_2);
        } else {
            throw new RuntimeException();
        }
    }

    public MixedAudioBuffer listEffect(int archiveOrFileId, int[] sampleRates) {
        if (this.soundEffectProvider.getArchiveCount() == 1) {
            return getSoundEffect(0, archiveOrFileId, sampleRates);
        } else if (this.soundEffectProvider.getArchiveFileCount(archiveOrFileId) == 1) {
            return getSoundEffect(archiveOrFileId, 0, sampleRates);
        } else {
            throw new RuntimeException();
        }
    }
}
