package com.jagex.game.runetek5.graphics.model.billboard;

public class BillboardEffect {

    public final int appliesOnFace;

    /**
     * width / size_x_z
     */
    public final short size2d;

    /**
     * height / size_y
     */
    public final short size3d;

    /**
     * Material the billboard is made of
     */
    public final short materialId;

    /**
     * 0 = color mix
     * 1 = no color mix
     */
    public final byte blendType;

    /**
     * 0 = rectangle
     * 1 = circle
     * 2 = brightened rectangle
     */
    public final byte shape;

    public final boolean hasUid;

    public final int distance;

    public BillboardEffect(int appliesOnFace, int size2d, int size3d, int materialId, int blendType, int shape, boolean hasUid, int distance) {
        this.appliesOnFace = appliesOnFace;
        this.size2d = (short) size2d;
        this.size3d = (short) size3d;
        this.materialId = (short) materialId;
        this.blendType = (byte) blendType;
        this.shape = (byte) shape;
        this.hasUid = hasUid;
        this.distance = distance;
    }
}
