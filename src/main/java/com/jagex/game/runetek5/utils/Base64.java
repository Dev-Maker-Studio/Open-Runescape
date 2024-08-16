package com.jagex.game.runetek5.utils;

public class Base64 {

    public static final char[] ENCODE_TABLE = new char[64];

    public static final char[] URL_SAFE_ENCODE_TABLE;

    public static final int[] DECODE_TABLE;

    static {
        for (int i = 0; i < 26; i++) {
            ENCODE_TABLE[i] = (char) (i + 65);
        }
        for (int i = 26; i < 52; i++) {
            ENCODE_TABLE[i] = (char) (i + 97 - 26);
        }
        for (int i = 52; i < 62; i++) {
            ENCODE_TABLE[i] = (char) (i + 48 - 52);
        }
        ENCODE_TABLE[62] = 43;
        ENCODE_TABLE[63] = 47;
        URL_SAFE_ENCODE_TABLE = new char[64];
        for (int i = 0; i < 26; i++) {
            URL_SAFE_ENCODE_TABLE[i] = (char) (i + 65);
        }
        for (int i = 26; i < 52; i++) {
            URL_SAFE_ENCODE_TABLE[i] = (char) (i + 97 - 26);
        }
        for (int i = 52; i < 62; i++) {
            URL_SAFE_ENCODE_TABLE[i] = (char) (i + 48 - 52);
        }
        URL_SAFE_ENCODE_TABLE[62] = 42;
        URL_SAFE_ENCODE_TABLE[63] = 45;
        DECODE_TABLE = new int[128];
        for (int i = 0; i < DECODE_TABLE.length; i++) {
            DECODE_TABLE[i] = -1;
        }
        for (int i = 65; i <= 90; i++) {
            DECODE_TABLE[i] = i - 65;
        }
        for (int i = 97; i <= 122; i++) {
            DECODE_TABLE[i] = i - 97 + 26;
        }
        for (int i = 48; i <= 57; i++) {
            DECODE_TABLE[i] = i - 48 + 52;
        }
        DECODE_TABLE[43] = 62;
        DECODE_TABLE[42] = 62;
        DECODE_TABLE[47] = 63;
        DECODE_TABLE[45] = 63;
    }

	public static byte[] decode(String input) {
	    int inputLength = input.length();
	    if (inputLength == 0) {
	        return new byte[0];
	    } 
        int paddedLength = inputLength + 3 & -4;
        int outputLength = paddedLength / 4 * 3;
        if (paddedLength - 2 < inputLength && getDecodedValue(input.charAt(paddedLength - 2)) != -1) {
            if (paddedLength - 1 >= inputLength || getDecodedValue(input.charAt(paddedLength - 1)) == -1) {
                outputLength--;
            }
        } else {
            outputLength -= 2;
        }
        byte[] output = new byte[outputLength];
        decode(input, output, 0);
        return output;
	}
	
	public static int decode(String input, byte[] output, int offset) {
	    int startOffset = offset;
	    int inputLength = input.length();
	    for (int i = 0; i < inputLength; i += 4) {
	        int val1 = getDecodedValue(input.charAt(i));
	        int val2 = i + 1 < inputLength ? getDecodedValue(input.charAt(i + 1)) : -1;
	        int val3 = i + 2 < inputLength ? getDecodedValue(input.charAt(i + 2)) : -1;
	        int val4 = i + 3 < inputLength ? getDecodedValue(input.charAt(i + 3)) : -1;
	        output[offset++] = (byte) (val1 << 2 | val2 >>> 4);
	        if (val3 == -1) {
	            break;
	        }
	        output[offset++] = (byte) ((val2 & 0xf) << 4 | val3 >>> 2);
	        if (val4 == -1) {
	            break;
	        }
	        output[offset++] = (byte) ((val3 & 0x3) << 6 | val4);
	    }
	    return offset - startOffset;
	}
	
	public static int getDecodedValue(char c) {
	    return c < DECODE_TABLE.length ? DECODE_TABLE[c] : -1;
	}
}
