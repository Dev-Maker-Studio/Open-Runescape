package com.jagex.game.runetek5.utils;

public class BufferUtils {

	public static int ilog(int num) {
	    int sizeShift = 0;
	    if (num < 0 || num >= 65536) {
	        num >>>= 16;
	        sizeShift += 16;
	    }
	    if (num >= 256) {
	        num >>>= 8;
	        sizeShift += 8;
	    }
	    if (num >= 16) {
	        num >>>= 4;
	        sizeShift += 4;
	    }
	    if (num >= 4) {
	        num >>>= 2;
	        sizeShift += 2;
	    }
	    if (num >= 1) {
	        num >>>= 1;
	        sizeShift++;
	    }
	    return num + sizeShift;
	}

	public static byte[][][] generateBytePatterns(int i_0) {
	    byte[][][] bytes_2 = new byte[8][4][];
	    int i_3 = i_0;
	    int i_4 = i_0;
	    byte[] bytes_5 = new byte[i_0 * i_0];
	    int i_6 = 0;
	
	    int i_7;
	    int i_8;
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_7) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[0][0] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_4; i_8++) {
	            if (i_8 <= i_7) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[0][1] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_7) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[0][2] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_7) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[0][3] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[1][0] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_6 >= 0 && i_6 < bytes_5.length) {
	                if (i_8 >= i_7 << 1) {
	                    bytes_5[i_6] = -1;
	                }
	
	            }
	            ++i_6;
	        }
	    }
	
	    bytes_2[1][1] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 <= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[1][2] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 >= i_7 << 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[1][3] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 <= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[2][0] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_7 << 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[2][1] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[2][2] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 >= i_7 << 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[2][3] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[3][0] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_7 << 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[3][1] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 >= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[3][2] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 <= i_7 << 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[3][3] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 >= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[4][0] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_7 << 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[4][1] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_7 >> 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[4][2] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 <= i_7 << 1) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[4][3] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_3 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[5][0] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_7 <= i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[5][1] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_3 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[5][2] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_7 >= i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[5][3] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[6][0] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 <= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[6][1] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 <= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[6][2] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 <= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[6][3] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[7][0] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = 0; i_8 < i_3; i_8++) {
	            if (i_8 >= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[7][1] = bytes_5;
	    bytes_5 = new byte[i_4 * i_3];
	    i_6 = 0;
	
	    for (i_7 = i_4 - 1; i_7 >= 0; --i_7) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 >= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[7][2] = bytes_5;
	    bytes_5 = new byte[i_3 * i_4];
	    i_6 = 0;
	
	    for (i_7 = 0; i_7 < i_4; i_7++) {
	        for (i_8 = i_3 - 1; i_8 >= 0; --i_8) {
	            if (i_8 >= i_7 - i_4 / 2) {
	                bytes_5[i_6] = -1;
	            }
	
	            ++i_6;
	        }
	    }
	
	    bytes_2[7][3] = bytes_5;
	    return bytes_2;
	}
}