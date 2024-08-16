package com.jagex.game.runetek5.graphics.model.billboard;

public class Billboard {

    public final int billboardId;
    public final int appliesOnFace;
    public final int depth;
    public final int distance;

    public Billboard(int billboardId, int appliesOnFace, int depth, int distance) {
        this.billboardId = billboardId;
        this.appliesOnFace = appliesOnFace;
        this.depth = depth;
        this.distance = distance;
    }

    public Billboard translateToFace(int face) {
        return new Billboard(this.billboardId, face, this.depth, this.distance);
    }
}
