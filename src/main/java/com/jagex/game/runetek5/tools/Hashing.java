package com.jagex.game.runetek5.tools;

/**
 * 
 * @author Brainpower - 31/05/2020
 *
 */
public class Hashing {

	public static final long[] CRC64_TABLE = new long[256];
	public static final int[] CRC32_TABLE = new int[256];
	
    static {
        for (int index = 0; index < 256; index++) {
            int hash = index;
            for (int subIndex = 0; subIndex < 8; subIndex++) {
                if ((hash & 0x1) == 1) {
                    hash = hash >>> 1 ^ -306674912;
                } else {
                    hash >>>= 1;
                }
            }
            CRC32_TABLE[index] = hash;
        }
        for (int index = 0; index < 256; index++) {
            long hash = index;
            for (int subIndex = 0; subIndex < 8; subIndex++) {
                if ((hash & 0x1L) == 1L) {
                    hash = hash >>> 1 ^ -3932672073523589310L;
                } else {
                    hash >>>= 1;
                }
            }
            CRC64_TABLE[index] = hash;
        }
    }

	public static int getCrc32(byte[] data, int start, int end) {
	    int crc32 = 0xFFFFFFFF;
	    for (int i = start; i < end; i++) {
	        crc32 = crc32 >>> 8 ^ CRC32_TABLE[(crc32 ^ data[i]) & 0xff];
	    }
	    crc32 = ~crc32;
	    return crc32;
	}

	public static int getCrc32(byte[] data, int length) {
	    return getCrc32(data, 0, length);
	}
}
