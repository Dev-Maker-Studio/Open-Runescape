package com.jagex.game.runetek5.sound.jagtheora;

import com.jagex.game.runetek5.cache.node.Node;

import jagtheora.ogg.OggPacket;
import jagtheora.ogg.OggStreamState;

public abstract class Node_Sub41 extends Node {

	public int anInt8031;
    public final OggStreamState anOggStreamState8032;

    public Node_Sub41(OggStreamState oggstreamstate1) {
        this.anOggStreamState8032 = oggstreamstate1;
    }

    public abstract void reset();

    public void method13361(OggPacket oggpacket1) {
        decode(oggpacket1);
        ++this.anInt8031;
    }

    abstract void decode(OggPacket var1);

}
