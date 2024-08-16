package com.jagex.game.runetek5.buffer;

public class CircularBuffer {

    /**
     * 0 = not in use
     * 1 = in use
     */
	public static int[] SLOT_IN_USE;
	
	public static int[] STASH_FOR_BUFFER_SIZE;
    public static int UNUSED_1_CURSOR_POS;
    public static int LOGIN_PACKET_CURSOR_POS;
    public static int UNUSED_2_CURSOR_POS;
	
    public static final byte[][] UNUSED_1 = new byte[1000][];
    public static final byte[][] LOGIN_PACKET_BUFFERS = new byte[250][];
    public static final byte[][] UNUSED_2 = new byte[50][];
	public static byte[][][] STASH;
    
	public static void initialize(int[] sizeStashes, int[] maxStashCapacities) {
	    if (sizeStashes != null && maxStashCapacities != null) {
	        STASH_FOR_BUFFER_SIZE = sizeStashes;
	        SLOT_IN_USE = new int[sizeStashes.length];
	        STASH = new byte[sizeStashes.length][][];
	        for (int index = 0; index < STASH_FOR_BUFFER_SIZE.length; index++) {
	            STASH[index] = new byte[maxStashCapacities[index]][];
	        }
	    } else {
	        STASH_FOR_BUFFER_SIZE = null;
	        SLOT_IN_USE = null;
	        STASH = null;
	    }
	}
	
    public static synchronized byte[] createBuffer(int size) {
        if (size == 100 && UNUSED_1_CURSOR_POS > 0) {
            byte[] data = UNUSED_1[--UNUSED_1_CURSOR_POS];
            UNUSED_1[UNUSED_1_CURSOR_POS] = null;
            return data;
        } else if (size == 5000 && LOGIN_PACKET_CURSOR_POS > 0) {
        	byte[] data = LOGIN_PACKET_BUFFERS[--LOGIN_PACKET_CURSOR_POS];
            LOGIN_PACKET_BUFFERS[LOGIN_PACKET_CURSOR_POS] = null;
            return data;
        } else if (size == 30000 && UNUSED_2_CURSOR_POS > 0) {
        	byte[] data = UNUSED_2[--UNUSED_2_CURSOR_POS];
            UNUSED_2[UNUSED_2_CURSOR_POS] = null;
            return data;
        } else if (CircularBuffer.STASH != null) {
            for (int index = 0; index < STASH_FOR_BUFFER_SIZE.length; index++) {
                if (STASH_FOR_BUFFER_SIZE[index] == size && CircularBuffer.SLOT_IN_USE[index] > 0) {
                    byte[] data = CircularBuffer.STASH[index][--CircularBuffer.SLOT_IN_USE[index]];
                    CircularBuffer.STASH[index][CircularBuffer.SLOT_IN_USE[index]] = null;
                    return data;
                }
            }
        }
        return new byte[size];
    }
    
    public static synchronized void stash(byte[] data) {
        if (data.length == 100 && UNUSED_1_CURSOR_POS < 1000) {
            UNUSED_1[++UNUSED_1_CURSOR_POS - 1] = data;
        } else if (data.length == 5000 && LOGIN_PACKET_CURSOR_POS < 250) {
            LOGIN_PACKET_BUFFERS[++LOGIN_PACKET_CURSOR_POS - 1] = data;
        } else if (data.length == 30000 && UNUSED_2_CURSOR_POS < 50) {
            UNUSED_2[++UNUSED_2_CURSOR_POS - 1] = data;
        } else if (CircularBuffer.STASH != null) {
            for (int index = 0; index < STASH_FOR_BUFFER_SIZE.length; index++) {
                if (data.length == STASH_FOR_BUFFER_SIZE[index] && CircularBuffer.SLOT_IN_USE[index] < CircularBuffer.STASH[index].length) {
                    CircularBuffer.STASH[index][CircularBuffer.SLOT_IN_USE[index]++] = data;
                    break;
                }
            }
        }
    }
}
