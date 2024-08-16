package com.jagex.game.runetek5.scene;

import com.jagex.game.runetek5.utils.MathUtils;

public class TileUtils {

	public static int getCullingType(int bitpacked) {
	    int type = bitpacked & 0x3f;
	    int rotation = bitpacked >> 6 & 0x3;
	    if (type == 18) {
	        if (rotation == 0) {
	            return 1;
	        }
	        if (rotation == 1) {
	            return 2;
	        }
	        if (rotation == 2) {
	            return 4;
	        }
	        return 8;
	    } else if (type == 19 || type == 21) {
	        if (rotation == 0) {
	            return 16;
	        }
	        if (rotation == 1) {
	            return 32;
	        }
	        if (rotation == 2) {
	            return 64;
	        }
	        return 128;
	    }
	    return 0;
	}

	/**
	 * Start tileheights
	 */
	
	public static int calculateTileheight(int x, int y) {
	    int height = TileUtils.perlinNoise(45365 + x, y + 91923, 4) - 128 + (TileUtils.perlinNoise(x + 10294, 37821 + y, 2) - 128 >> 1) + (TileUtils.perlinNoise(x, y, 1) - 128 >> 2);
	    height = (int) (height * 0.3D) + 35;
	    if (height < 10) {
	        height = 10;
	    } else if (height > 60) {
	        height = 60;
	    }
	    return height;
	}

	public static int perlinNoise(int x, int y, int frequencyReciprocal) {
	    int adjX = x / frequencyReciprocal;
	    int angleX = x & frequencyReciprocal - 1;
	    int adjY = y / frequencyReciprocal;
	    int angleY = y & frequencyReciprocal - 1;
	    int noiseBase = TileUtils.randomNoiseWeighedSum(adjX, adjY);
	    int noiseEast = TileUtils.randomNoiseWeighedSum(adjX + 1, adjY);
	    int noiseSouth = TileUtils.randomNoiseWeighedSum(adjX, adjY + 1);
	    int noiseSouthEast = TileUtils.randomNoiseWeighedSum(adjX + 1, adjY + 1);
	    int interpolatedNoiseNorth = TileUtils.interpolate(noiseBase, noiseEast, angleX, frequencyReciprocal);
	    int interpolatedNoiseSouth = TileUtils.interpolate(noiseSouth, noiseSouthEast, angleX, frequencyReciprocal);
	    return TileUtils.interpolate(interpolatedNoiseNorth, interpolatedNoiseSouth, angleY, frequencyReciprocal);
	}

	static int randomNoiseWeighedSum(int x, int y) {
	    int corners = TileUtils.randomNoise(x - 1, y - 1) + TileUtils.randomNoise(1 + x, y - 1) + TileUtils.randomNoise(x - 1, 1 + y) + TileUtils.randomNoise(x + 1, 1 + y);
	    int sides = TileUtils.randomNoise(x - 1, y) + TileUtils.randomNoise(1 + x, y) + TileUtils.randomNoise(x, y - 1) + TileUtils.randomNoise(x, y + 1);
	    int center = TileUtils.randomNoise(x, y);
	    return corners / 16 + sides / 8 + center / 4;
	}

	public static int randomNoise(int x, int y) {
	    int n = y * 57 + x;
	    n ^= n << 13;
	    int l = n * (n * n * 15731 + 789221) + 1376312589 & 0x7fffffff;
	    return l >> 19 & 0xff;
	}

	public static int interpolate(int a, int b, int angle, int frequencyReciprocal) {
	    int cosine = 65536 - MathUtils.COSINE[angle * 8192 / frequencyReciprocal] >> 1;
	    return ((65536 - cosine) * a >> 16) + (cosine * b >> 16);
	}
}