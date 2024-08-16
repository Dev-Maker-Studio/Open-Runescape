package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.Node;
import com.jagex.game.runetek5.cache.node.impl.Node_Sub26;

public abstract class PcmStream extends Node {

	public PcmStream nextStream;
    public int streamPriority;
    public Node_Sub26 buffer;
    public volatile boolean fill = true;

    public int getPriority() {
        return 255;
    }

    public abstract PcmStream getFirstStream();

    public abstract int method12228();

    public abstract void fill(int[] buffer, int offset, int length);

    public abstract void skip(int length);

    void process(int[] buffer, int offset, int length) {
        if (this.fill) {
            fill(buffer, offset, length);
        } else {
            skip(length);
        }

    }

    public abstract PcmStream getNextStream();

}
