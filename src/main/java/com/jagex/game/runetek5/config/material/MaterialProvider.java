package com.jagex.game.runetek5.config.material;

public interface MaterialProvider {

    boolean isCached(int id);

    // var2 proly alpha
    int[] getPixels(int materialId, float brightness, int width, int length, boolean readReversed);

    int[] getPixelsArgb(int textureId, int width, int height);

    int getSize();

    MaterialType getMaterial(int materialId);

    float[] getPixelsHdr(int textureId, int width, int height);

    void cycle();

}
