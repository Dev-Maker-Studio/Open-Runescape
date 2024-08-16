package com.jagex.game.runetek5.config.sequence.frameset;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.Node;

public class AnimBase extends Node<AnimBase> {

	public final int id;
	public final int numTransformation;
	public final int[] transformationType;
	public final int[][] labels;
	public final boolean[] shadowed;
	public final int[] submeshes;

    AnimBase(int id, byte[] baseData) {
        this.id = id;
        Packet stream = new Packet(baseData);
        this.numTransformation = stream.readUnsignedByte();
        this.transformationType = new int[this.numTransformation];
        this.labels = new int[this.numTransformation][];
        this.shadowed = new boolean[this.numTransformation];
        this.submeshes = new int[this.numTransformation];
        for (int index = 0; index < this.numTransformation; index++) {
            this.transformationType[index] = stream.readUnsignedByte();
            if (this.transformationType[index] == 6) {
                this.transformationType[index] = 2;
            }
        }
        for (int index = 0; index < this.numTransformation; index++) {
            this.shadowed[index] = stream.readUnsignedByte() == 1;
        }
        for (int index = 0; index < this.numTransformation; index++) {
            this.submeshes[index] = stream.readUnsignedShort();
        }
        for (int index = 0; index < this.numTransformation; index++) {
            this.labels[index] = new int[stream.readUnsignedByte()];
        }
        for (int index = 0; index < this.numTransformation; index++) {
            for (int subIndex = 0; subIndex < this.labels[index].length; subIndex++) {
                this.labels[index][subIndex] = stream.readUnsignedByte();
            }
        }
    }
}
