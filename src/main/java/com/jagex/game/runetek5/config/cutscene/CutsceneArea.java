package com.jagex.game.runetek5.config.cutscene;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;

public class CutsceneArea extends Node<CutsceneArea> {

    public final int plane;
    public final int regionX;
    public final int regionY;
    public final int width;
    public final int length;
    public final int cutscenePlane;
    public final int anInt7480;
    public final int anInt7483;
    public final int rotation;

    CutsceneArea(Packet buffer) {
        int position = buffer.readInt();
        this.plane = position >>> 28;
        this.regionX = position >>> 14 & 0x3fff;
        this.regionY = position & 0x3fff;
        this.width = buffer.readUByte();
        this.length = buffer.readUByte();
        this.cutscenePlane = buffer.readUByte();
        this.anInt7480 = buffer.readUByte();
        this.anInt7483 = buffer.readUByte();
        this.rotation = buffer.readUByte();
    }

}
