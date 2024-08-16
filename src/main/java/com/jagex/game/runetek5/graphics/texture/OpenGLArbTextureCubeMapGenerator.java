package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.config.material.MaterialProvider;
import com.jagex.game.runetek5.config.material.MaterialType;
import com.jagex.game.runetek5.toolkit.OpenGlToolkit;
import com.jagex.game.runetek5.toolkit.opengl.GLActionFormat;
import com.jagex.game.runetek5.toolkit.opengl.GLFormat;

public class OpenGLArbTextureCubeMapGenerator extends AbstractOpenGlCubeTextureGenerator {

    public static final int[] MATERIAL_BUFFER = new int[6];
    public static final int[][] PIXELS = new int[6][];
    public OpenGlArbCubeMapTexture arbCubeMapTexture;
    public final OpenGlToolkit openglToolkit;
    public final int materialAtPositiveY;
    public final int materialAtNegativeY;
    public final int materialAtPositiveZ;
    public final int materialAtNegativeZ;
    public final int materialAtPositiveX;
    public final int materialAtNegativeX;

    public OpenGLArbTextureCubeMapGenerator(OpenGlToolkit openglToolkit, int materialAtPositiveX, int materialAtNegativeX, int materialAtPositiveY, int materialAtNegativeY, int materialAtPositiveZ, int materialAtNegativeZ) {
        this.openglToolkit = openglToolkit;
        this.materialAtPositiveY = materialAtPositiveY;
        this.materialAtNegativeY = materialAtNegativeY;
        this.materialAtPositiveZ = materialAtPositiveZ;
        this.materialAtNegativeZ = materialAtNegativeZ;
        this.materialAtPositiveX = materialAtPositiveX;
        this.materialAtNegativeX = materialAtNegativeX;
    }

    @Override
    public OpenGlArbCubeMapTexture getCubeTexture() {
        if (this.arbCubeMapTexture == null) {
            MaterialProvider materialProvider = this.openglToolkit.materialProvider;
            MATERIAL_BUFFER[0] = this.materialAtPositiveX;
            MATERIAL_BUFFER[1] = this.materialAtNegativeX;
            MATERIAL_BUFFER[2] = this.materialAtPositiveY;
            MATERIAL_BUFFER[3] = this.materialAtNegativeY;
            MATERIAL_BUFFER[4] = this.materialAtPositiveZ;
            MATERIAL_BUFFER[5] = this.materialAtNegativeZ;
            boolean mipmap = false;
            int largestMaterialSize = 0;

            for (int index = 0; index < 6; index++) {
                if (!materialProvider.isCached(MATERIAL_BUFFER[index])) {
                    return null;
                }

                MaterialType material = materialProvider.getMaterial(MATERIAL_BUFFER[index]);
                int size = material.isSmall ? 64 : 128;
                if (size > largestMaterialSize) {
                    largestMaterialSize = size;
                }

                if (material.mipmapping > 0) {
                    mipmap = true;
                }
            }

            for (int index = 0; index < 6; index++) {
                PIXELS[index] = materialProvider.getPixels(MATERIAL_BUFFER[index], 1.0F, largestMaterialSize, largestMaterialSize, false);
            }

            this.arbCubeMapTexture = new OpenGlArbCubeMapTexture(this.openglToolkit, GLFormat.aClass1501950, GLActionFormat.PIXEL_FORMAT, largestMaterialSize, mipmap, PIXELS);
        }

        return this.arbCubeMapTexture;
    }

}
