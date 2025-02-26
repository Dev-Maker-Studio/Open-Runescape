package com.jagex.game.runetek5.cache.compression;

import java.util.zip.Inflater;

import com.jagex.game.runetek5.buffer.Packet;

public class GzipDecompressor {

    public Inflater inflater;
	public byte[] decompressCustomSize(byte[] compressed) {
        Packet buffer = new Packet(compressed);
        buffer.currentPosition = compressed.length - 4;
        int size = buffer.readUIntLittle();
        byte[] data = new byte[size];
        buffer.currentPosition = 0;
        decompress(buffer, data);
        return data;
    }

    public void decompress(Packet stream, byte[] compressed) {
        if (stream.buffer[stream.currentPosition] == 31 && stream.buffer[stream.currentPosition + 1] == -117) {
            if (this.inflater == null) {
                this.inflater = new Inflater(true);
            }

            try {
                this.inflater.setInput(stream.buffer, stream.currentPosition + 10, stream.buffer.length - (stream.currentPosition + 8 + 10));
                this.inflater.inflate(compressed);
            } catch (Exception ex) {
                this.inflater.reset();
                throw new RuntimeException("");
            }

            this.inflater.reset();
        } else {
            throw new RuntimeException("");
        }
    }
}
