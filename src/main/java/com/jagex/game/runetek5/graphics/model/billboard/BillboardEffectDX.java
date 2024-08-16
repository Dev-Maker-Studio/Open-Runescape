package com.jagex.game.runetek5.graphics.model.billboard;

public class BillboardEffectDX {

	public final int appliesOnFace;
	public final int faceA;
	public final int faceB;
	public final int faceC;
	public final short size2d;
	public final short size3d;
	public final short materialId;
    public final boolean stationary;
    public final int distance;

    public BillboardEffectDX(int appliesOnFace, int faceA, int faceB, int faceC, int size2d, int size3d, int materialId, boolean stationary, int distance) {
    	this.appliesOnFace = appliesOnFace;
    	this.faceA = faceA;
    	this.faceB = faceB;
    	this.faceC = faceC;
        this.size2d = (short) size2d;
        this.size3d = (short) size3d;
        this.materialId = (short) materialId;
        this.stationary = stationary;
        this.distance = distance;
    }

}
