package com.jagex.game.runetek5.graphics.shader;

public class ShaderBuffer {

    public final byte[] shaderData;
    public int currentPosition;

    ShaderBuffer(byte[] shaderData) {
        this.shaderData = shaderData;
        this.currentPosition = 0;
    }

    public int getEntryCount() {
        short entryCount = 0;
        for (int index = 0; index < 2; index++) {
            entryCount |= (this.shaderData[++this.currentPosition - 1] & 0xff) << index * 8;
        }
        return entryCount;
    }

    public String readString() {
        int length = getEntryCount();
        if (length == -1) {
            return null;
        } 
        if (length > 256) {
            throw new ShaderBufferException();
        } 
        String str = new String(this.shaderData, this.currentPosition, length);
        this.currentPosition += length;
        return str;
    }
}
