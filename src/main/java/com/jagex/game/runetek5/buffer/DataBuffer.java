package com.jagex.game.runetek5.buffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataBuffer {

	public static Object createDataBuffer(byte[] data) {
	    if (data == null) {
	        return null;
	    } 
	    if (data.length > 136) {
	        ByteBuffer bytebuffer = ByteBuffer.allocateDirect(data.length);
	        bytebuffer.position(0);
	        bytebuffer.put(data);
	        return bytebuffer;
	    } 
	    return data;
	}

	public static byte[] unpackDataBuffer(Object dataBuffer, boolean copyBuffer) {
	    if (dataBuffer == null) {
	        return null;
	    } 
	    if (dataBuffer instanceof byte[] data) {
            return copyBuffer ? Arrays.copyOf(data, data.length) : data;
	    } 
	    if (dataBuffer instanceof ByteBuffer bytebuffer) {
            byte[] data = new byte[bytebuffer.capacity()];
	        bytebuffer.position(0);
	        bytebuffer.get(data);
	        return data;
	    } 
	    throw new IllegalArgumentException();
	}

	public static byte[] readDataBuffer(Object object, int position) {
	    if (object == null) {
	        return null;
	    } 
	    if (object instanceof byte[] buffer) {
            return Arrays.copyOfRange(buffer, position, 32768 + position);
	    } 
	    if (object instanceof ByteBuffer buffer) {
            byte[] data = new byte[32768];
	        buffer.position(position);
	        buffer.get(data, 0, 32768);
	        return data;
	    } 
	    throw new IllegalArgumentException();
	}
}
