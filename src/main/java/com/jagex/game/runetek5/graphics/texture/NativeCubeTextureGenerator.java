package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.graphics.texture.sampler.CubeTextureSampler;
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit;

public class NativeCubeTextureGenerator extends AbstractNativeCubeTextureGenerator {

    public static final int[] MATERIAL_IDS = new int[6];
    public static final int[][] MATERIAL_PIXELS = new int[6][];
    public CubeTextureSampler nativeCubeTexture;
    public final AbstractNativeToolkit nativeToolkit;
    public final int material1;
    public final int material2;
    public final int material3;
    public final int material4;
    public final int material5;
    public final int material6;

    public NativeCubeTextureGenerator(AbstractNativeToolkit nativeToolkit, int material5, int material6, int material1, int material2, int material3, int material4) {
        this.nativeToolkit = nativeToolkit;
        this.material1 = material1;
        this.material2 = material2;
        this.material3 = material3;
        this.material4 = material4;
        this.material5 = material5;
        this.material6 = material6;
    }

    @Override
	public
    CubeTextureSampler getNativeCubeTexture() {
        if (this.nativeCubeTexture == null) {
            MaterialProvider materialProvider = this.nativeToolkit.materialProvider;
            MATERIAL_IDS[0] = this.material5;
            MATERIAL_IDS[1] = this.material6;
            MATERIAL_IDS[2] = this.material1;
            MATERIAL_IDS[3] = this.material2;
            MATERIAL_IDS[4] = this.material3;
            MATERIAL_IDS[5] = this.material4;
            boolean mipmappingEnabled = false;
            int largestSize = 0;

            for (int index = 0; index < 6; index++) {
                if (!materialProvider.isCached(MATERIAL_IDS[index])) {
                    return null;
                }

                MaterialType material = materialProvider.getMaterial(MATERIAL_IDS[index]);
                int size = material.isSmall ? 64 : 128;
                if (size > largestSize) {
                    largestSize = size;
                }

                if (material.mipmapping > 0) {
                    mipmappingEnabled = true;
                }
            }

            for (int index = 0; index < 6; index++) {
                MATERIAL_PIXELS[index] = materialProvider.getPixels(MATERIAL_IDS[index], 1.0F, largestSize, largestSize, false);
            }

            this.nativeCubeTexture = this.nativeToolkit.generateNativeCubeTexture(largestSize, mipmappingEnabled, MATERIAL_PIXELS);
        }

        return this.nativeCubeTexture;
    }

}
