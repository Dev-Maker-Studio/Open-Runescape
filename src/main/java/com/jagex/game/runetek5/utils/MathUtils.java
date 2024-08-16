package com.jagex.game.runetek5.utils;

public class MathUtils {

    public static final int[] SINE = new int[16384];
    public static final int[] COSINE = new int[16384];

    static {
        double step = 3.834951969714103E-4D;

        for (int i = 0; i < 16384; i++) {
            SINE[i] = (int) (16384.0D * Math.sin(i * step));
            COSINE[i] = (int) (16384.0D * Math.cos(i * step));
        }

    }

    MathUtils() {
        throw new Error();
    }

    /**
     * Theta is used in math as a variable to represent a measured angle
     * https://wumbo.net/symbols/theta/#:~:text=The%20Greek%20letter%20%CE%B8%20(theta,tangent%20as%20the%20input%20variable.
     */
    public static float getTheta(int rotation) {
        rotation &= 0x3fff;
        return (float) ((rotation / 16384.0F) * 6.283185307179586D);
    }

    public static int calculateSampleRate(int maxDelay, int minDelay) {
        double d_3 = Math.log(minDelay) / Math.log(2.0D);
        double d_5 = Math.log(maxDelay) / Math.log(2.0D);
        double d_7 = Math.random() * (d_3 - d_5) + d_5;
        return (int) (Math.pow(2.0D, d_7) + 0.5D);
    }

	public static int furtestBitValue(int i_0) {
	    int i_2 = i_0 >>> 1;
	    i_2 |= i_2 >>> 1;
	    i_2 |= i_2 >>> 2;
	    i_2 |= i_2 >>> 4;
	    i_2 |= i_2 >>> 8;
	    i_2 |= i_2 >>> 16;
	    return i_0 & ~i_2;
	}

	public static int nextPowerOfTwo(int i_0) {
	    --i_0;
	    i_0 |= i_0 >>> 1;
	    i_0 |= i_0 >>> 2;
	    i_0 |= i_0 >>> 4;
	    i_0 |= i_0 >>> 8;
	    i_0 |= i_0 >>> 16;
	    return i_0 + 1;
	}

	public static int power(int base, int exponent) {
	    int result;
	    for (result = 1; exponent > 1; exponent >>= 1) {
	        if ((exponent & 0x1) != 0) {
	            result = base * result;
	        }
	        base *= base;
	    }
	    if (exponent == 1) {
	        return base * result;
	    } 
	    return result;
	}
}
