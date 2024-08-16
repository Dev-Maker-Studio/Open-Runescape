package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.Node;

public class MusicPatchNode extends Node {

    int loopVariable;
    int currentNotePitch;
    int maxVolumeLevel;
    int anInt7571;
    int currentPanValue;
    int notePitchAdjustment;
    int anInt7574;
    int anInt7575;
    int anInt7576;
    int anInt7577;
    int anInt7578;
    int anInt7579;
    int anInt7580;
    int anInt7581;
    int currentTrack;
    int anInt7583;
    int anInt7584;
    MusicPatch patch;
    AbstractAudioFile audioFile;
    MusicPatchNode2 musicPatchInfo;
    AreaSoundStream stream;

    void clearMusicPatchNode() {
        this.patch = null;
        this.audioFile = null;
        this.musicPatchInfo = null;
        this.stream = null;
    }
}
