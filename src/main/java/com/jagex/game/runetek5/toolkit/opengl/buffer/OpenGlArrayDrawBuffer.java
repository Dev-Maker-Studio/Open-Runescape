package com.jagex.game.runetek5.toolkit.opengl.buffer;

import com.jagex.game.runetek5.buffer.ArrayBufferGL;

public class OpenGlArrayDrawBuffer {

	public ArrayBufferGL buffer;
	public short type;
	public byte size;
	public byte offset;

	public OpenGlArrayDrawBuffer(ArrayBufferGL buffer, int type, int size, int offset) {
        this.buffer = buffer;
        this.type = (short) type;
        this.size = (byte) size;
        this.offset = (byte) offset;
    }

}
