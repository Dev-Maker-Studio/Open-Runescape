package com.jagex.game.runetek5.sound.jagtheora

import com.jagex.game.runetek5.cache.node.Node

class MusicPatchNode : Node<MusicPatchNode>() {

    @JvmField var loopVariable: Int = 0
    @JvmField var currentNotePitch: Int = 0
    @JvmField var maxVolumeLevel: Int = 0
    @JvmField var anInt7571: Int = 0
    @JvmField var currentPanValue: Int = 0
    @JvmField var notePitchAdjustment: Int = 0
    @JvmField var anInt7574: Int = 0
    @JvmField var anInt7575: Int = 0
    @JvmField var anInt7576: Int = 0
    @JvmField var anInt7577: Int = 0
    @JvmField var anInt7578: Int = 0
    @JvmField var anInt7579: Int = 0
    @JvmField var anInt7580: Int = 0
    @JvmField var anInt7581: Int = 0
    @JvmField var currentTrack: Int = 0
    @JvmField var anInt7583: Int = 0
    @JvmField var anInt7584: Int = 0
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
