package com.jagex.game.runetek5.graphics.texture;

import com.jagex.game.runetek5.graphics.model.Model;

public class TextureTransformationMatrix {

    public final int[] centerX;
    public final int[] centerY;
    public final int[] centerZ;
    public final float[][] matrix;
    public final Model model;

    public TextureTransformationMatrix(Model model, int[] centerX, int[] centerY, int[] centerZ, float[][] matrix) {
        this.model = model;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.matrix = matrix;
    }

}
