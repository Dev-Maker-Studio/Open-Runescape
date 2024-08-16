package com.jagex.game.runetek5.js5;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.compression.Bzip2Compressor;
import com.jagex.game.runetek5.cache.compression.GzipDecompressor;

/**
 * [J]
 * @author Steven - 11/10/2023
 *
 */
public class Js5Compression {
	
	public static final GzipDecompressor GZIP = new GzipDecompressor();
	public static int maxSize;

	public static byte[] decompress(byte[] compressed) {
	    try {
	        Packet buffer = new Packet(compressed);
	        int compressionType = buffer.readUnsignedByte();
	        int payloadLength = buffer.readInt();
	        if (payloadLength >= 0 && (maxSize == 0 || payloadLength <= maxSize)) {
	            if (compressionType == 0) {
	                byte[] uncompressedPayload = new byte[payloadLength];
	                buffer.readBytes(uncompressedPayload, 0, payloadLength);
	                return uncompressedPayload;
	            } 
                int decompressedLength = buffer.readInt();
                if (decompressedLength < 0 || maxSize != 0 && decompressedLength > maxSize) {
                    throw new RuntimeException();
                } 
                byte[] decompressed = new byte[decompressedLength];
                if (compressionType == 1) {
                    Bzip2Compressor.decompress(decompressed, decompressedLength, compressed);
                } else {
                    synchronized (GZIP) {
                        GZIP.decompress(buffer, decompressed);
                    }
                }
                return decompressed;	            
	        } 
	        throw new RuntimeException();	        
	    } catch (Throwable throwable) {
			return new byte[100];
	    }
	}
}