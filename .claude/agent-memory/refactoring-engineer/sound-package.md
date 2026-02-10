# Sound Package Deobfuscation Notes

## AreaSoundStream.kt (parent class for all audio streams)
### Fields (10 renamed)
- `anInt9848` -> `monoVolume`, `anInt9845` -> `leftVolume`, `anInt9850` -> `rightVolume`
- `anInt9847` -> `panPosition`, `anInt9858` -> `volumeRampRemaining`
- `anInt9849` -> `monoVolumeStep`, `anInt9852` -> `leftVolumeStep`, `anInt9851` -> `rightVolumeStep`
- `anInt9854` -> `loopCount`, `anInt9853` -> `samplePosition`

### Methods (17 renamed)
- `method15308` -> `setSampleRate`, `method15311` -> `isVolumeRamping`
- `method15313` -> `reversePlaybackDirection`, `method15315` -> `getEffectiveVolume`
- `method15316` -> `getEffectivePan`, `method15317` -> `setSamplePosition`
- `method15319` -> `isPlaybackComplete`, `method15320` -> `rampToVolume`
- `method15321` -> `rampToVolumeAndPan`, `method15322` -> `fadeOutAndStop`
- `method15323` -> `getAbsoluteSampleRate`, `method15324` -> `updateVolumeRamp`
- `method15326` -> `muteVolume`, `method15332` -> `computeChannelVolumes`
- `method15340` -> `setPan`, `method15354` -> `cancelVolumeRamp`
- `method15364` -> `setVolumeAndPan`

## MidiPcmStream.kt (MIDI-to-PCM converter)
### Fields (12 renamed)
- `anInt9719` -> `queuedTrackTick`, `aBool9724` -> `queuedAllSoundOff`
- `anIntArray9702` -> `channelVolume`, `anIntArray9698` -> `defaultProgram`
- `anIntArray9723` -> `effectSpeedRaw`, `anIntArray9712` -> `effectSpeedScaled`
- `aLong9710` -> `currentTickTime`, `aNodeSub79722` -> `queuedTrack`
- `aLong9693` -> `nextEventTickTime`, `aBool9716` -> `isLooping`
- `musicPatchNode1` -> `activeNotes`, `musicPatchNode2` -> `loopGroupNotes`

### Methods (15 renamed)
- `method15095` -> `setChannelVolume`, `method15096` -> `resetMusicPatches`
- `method15099` -> `applyInitialSampleOffset`, `method15100` -> `setMusicTrackAtPosition`
- `method15102` -> `clearTrack`, `method15104` -> `setEffectSpeed`
- `method15105` -> `initDrumChannel`, `method15115` -> `isPatchNodeDead`
- `method15120` -> `calculatePitch`, `method15122` -> `calculatePan`
- `method15126` -> `advanceToTick`, `method15127` -> `processNextMidiEvents`
- `method15128` -> `updatePatchNode`, `method15131` -> `loadAndPlayTrack`
- `method15188` -> `setKeyPressure`

## MusicPatchNode.kt (per-note state)
### Fields (11 renamed)
- `anInt7571` -> `vibratoPhase`, `anInt7574` -> `portamentoPitchOffset`
- `anInt7575` -> `portamentoFactor`, `anInt7576` -> `volumeEnvelopeTime`
- `anInt7577` -> `volumeEnvelopePosition`, `anInt7578` -> `volumeEnvelopeIndex`
- `anInt7579` -> `releaseState`, `anInt7580` -> `releaseEnvelopeIndex`
- `anInt7581` -> `tickCounter`, `anInt7583` -> `retriggerPhase`
- `anInt7584` -> `tickSampleLength`

## MusicPatchPcmStream.kt
- `aNodeSub15Sub29753` -> `midiPcmStream`, `aNodeSub15Sub49755` -> `fadeOutScheduler`

## MusicPatchNode2.kt
- `aByteArray1097` -> `volumeEnvelopeData`, `aByteArray1094` -> `releaseEnvelopeData`
- `anInt1102` -> `vibratoDelay`

## MixedAudioStream.kt / AudioBufferStream.kt
- `method15990/method15769` -> `mixSamplesForward`
- `method16018/method15732` -> `mixSamplesReverse`
- 16/14 static companion mixing kernels intentionally left (low-level optimization variants)

## PcmStream.kt
- `method12228` -> `getActiveVoiceCount`

## StaticMethods.kt audio methods
- `method488` -> `computeLeftChannelGain`, `method15390` -> `computeRightChannelGain`
- `method6140` -> `isMusicLoading`

## MidiFileReader.kt
- `anInt790` -> `microsecondsPerBeat`, `aLong786` -> `tempoTimeOffset`
- `anIntArray796` -> `runningStatus`, `aByteArray784` -> `messageDataByteCount`
- `method1403` -> `getTickTimestamp`, `method1406` -> `resetTracksFromTimestamp`

## MusicTrack.kt
- `method12165` -> `buildPatchTable`

## AmbientSound.kt
- `aNodeSub26Sub1Sub18082` -> `soundEffectAudioBuffer` (AudioBuffer for looping sound)
- `aNodeSub15Sub58096` -> `groupSoundStream` (active group sound AreaSoundStream)
- `aNodeSub188087` -> `groupInstrumentEffect` (MusicSample for group sound)
- `aNodeSub26Sub1Sub18106` -> `groupSoundAudioBuffer` (AudioBuffer for group sound)

## SoundEffectPlayer.kt
- `method1491` -> `setMusicSoundLevel`, `method14675` -> `updateAmbientSounds`
- `method2964` -> `updateAmbientSound`, `method7272` -> `getActiveMidiStream`

## AreaSoundPlayer.kt
- `method1623` -> `processAreaSounds`, `method3690` -> `playMusicWithStreamRestore`
- `method13400` -> `loadMusic`, `method4827` -> `loadMusicFromStart`
- `method1859` -> `loadMusicTrack`, `method5961` -> `updateMusicVolumeByDistance`

## SongPlayer.kt
- `method3912` -> `clearPrimarySound`, `method12658` -> `resetChannelVolumes`
- `method2580` -> `setMusicChannelVolume`, `method5731` -> `playSongAtLocation`
- `anIntArray5449` -> `savedChannelVolumes`, `aClass2535830` -> `musicPlayer`

## SoundResourceManager.kt
- `aNodeSub15Sub21735` -> `activeMidiStream` (global music MidiPcmStream)
- `aNodeSub15Sub27836` -> `pendingMidiStream` (backup for jingle restore)
- `anInt1730` -> `musicLoadingState` (0=idle, 1=requesting, 2=loading patches, 3=fading in)
- `aLong1740` -> `musicResumePosition`
- `method7730` -> `processMusicLoading`, `method2550` -> `getSubtitleText`

## StaticMethods.kt sound fields
- `aBool1962` -> `musicShouldLoop`, `anInt4664` -> `musicResumeVolume`
- `aNodeSub79563` -> `pendingMusicTrack`, `aClass2505450` -> `pendingSoundBankCache`

## SongReference.kt
- `method1966` -> `isMusicActive` (also @JvmName updated)

## CutsceneBasicMovement.kt (misplaced field)
- `anInt9440` -> `musicFadeStep` (volume decrement per tick during fade-out)

## PacketEncoder.kt
- `method3675` -> `checkSongLoaded`

## Sound Architecture Summary
- **Music pipeline**: AreaSoundPlayer.loadMusic -> SoundResourceManager.processMusicLoading -> MidiPcmStream
- **Loading state machine**: musicLoadingState 0=idle, 1=requesting files, 2=loading patches, 3=fading in
- **Ambient sounds**: SoundEffectPlayer.updateAmbientSounds iterates LOC/NPC/PLAYER lists
- **Area sounds**: AreaSoundPlayer.processAreaSounds manages queued one-shot sounds
- **Jingle system**: SoundEffectPlayer.playSoundJingle saves/restores MIDI stream
- **Stereo panning**: Uses angle-based pan with distance-dependent stereo width
