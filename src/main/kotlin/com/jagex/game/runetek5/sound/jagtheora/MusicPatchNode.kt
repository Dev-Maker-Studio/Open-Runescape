package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node

class MusicPatchNode : Node<MusicPatchNode>() {

    @JvmField var loopVariable: Int = 0
    @JvmField var currentNotePitch: Int = 0
    @JvmField var maxVolumeLevel: Int = 0
    @JvmField var vibratoPhase: Int = 0
    @JvmField var currentPanValue: Int = 0
    @JvmField var notePitchAdjustment: Int = 0
    @JvmField var portamentoPitchOffset: Int = 0
    @JvmField var portamentoFactor: Int = 0
    @JvmField var volumeEnvelopeTime: Int = 0
    @JvmField var volumeEnvelopePosition: Int = 0
    @JvmField var volumeEnvelopeIndex: Int = 0
    @JvmField var releaseState: Int = 0
    @JvmField var releaseEnvelopeIndex: Int = 0
    @JvmField var tickCounter: Int = 0
    @JvmField var currentTrack: Int = 0
    @JvmField var retriggerPhase: Int = 0
    @JvmField var tickSampleLength: Int = 0
    @JvmField var patch: MusicPatch? = null
    @JvmField var audioFile: AbstractAudioFile? = null
    @JvmField var musicPatchInfo: MusicPatchNode2? = null
    @JvmField var stream: AreaSoundStream? = null

    fun clearMusicPatchNode() {
        this.patch = null
        this.audioFile = null
        this.musicPatchInfo = null
        this.stream = null
    }
}
