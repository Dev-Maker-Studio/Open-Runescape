package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.impl.Node_Sub26;

public abstract class AbstractAudioFile extends Node_Sub26 {

    public int start;
    public int sampleRate;
    public boolean enableLoop;
    protected int end;

    public abstract int getTotalSamples();

    public abstract AreaSoundStream getStream(int var1, int var2, int var3);

}
