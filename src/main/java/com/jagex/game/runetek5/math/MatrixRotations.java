package com.jagex.game.runetek5.math;

public class MatrixRotations {

	public static final float[] SIN = new float[16384];
    public static final float[] COS = new float[16384];

    static {
        double angle = 3.834951969714103E-4D;
        for (int index = 0; index < 16384; index++) {
            SIN[index] = (float) Math.sin(index * angle);
            COS[index] = (float) Math.cos(index * angle);
        }
    }
}
