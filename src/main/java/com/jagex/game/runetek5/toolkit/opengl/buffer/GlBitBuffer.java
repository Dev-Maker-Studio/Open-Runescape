package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.buffer.Packet;

import jaclib.memory.Stream;

/**
 * 
 * @author Brainpower - 07/05/2022
 *
 */
public class GlBitBuffer extends Packet {

	public GlBitBuffer(int size) {
        super(size);
    }

	public void writeFloatLittleEndian(float value) {
        int rawIntBits = Stream.floatToRawIntBits(value);
        super.buffer[++super.currentPosition - 1] = (byte) rawIntBits;
        super.buffer[++super.currentPosition - 1] = (byte) (rawIntBits >> 8);
        super.buffer[++super.currentPosition - 1] = (byte) (rawIntBits >> 16);
        super.buffer[++super.currentPosition - 1] = (byte) (rawIntBits >> 24);
    }

	public void writeFloatBigEndian(float value) {
        int rawIntBits = Stream.floatToRawIntBits(value);
        super.buffer[++super.currentPosition - 1] = (byte) (rawIntBits >> 24);
        super.buffer[++super.currentPosition - 1] = (byte) (rawIntBits >> 16);
        super.buffer[++super.currentPosition - 1] = (byte) (rawIntBits >> 8);
        super.buffer[++super.currentPosition - 1] = (byte) rawIntBits;
    }

}
