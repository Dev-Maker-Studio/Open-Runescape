package com.jagex.game.runetek5.graphics.model.billboard;

public class BillboardEffectGL {

	public final int appliesOnFace;
	public final int faceA;
    public final int faceB;
    public final int faceC;
    public final short sizeX;
    public final short sizeY;
    public final short materialId;
    public final byte shape;
    public final boolean stationary;
    public final int distance;

    public BillboardEffectGL(int face, int faceA, int faceB, int faceC, int sizeX, int sizeY, int materialId, int shape, boolean stationary, int distance) {
    	this.appliesOnFace = face;
    	this.faceA = faceA;
    	this.faceB = faceB;
    	this.faceC = faceC;
    	this.sizeX = (short) sizeX;
    	this.sizeY = (short) sizeY;
    	this.materialId = (short) materialId;
    	this.shape = (byte) shape;
        this.stationary = stationary;
        this.distance = distance;
    }
}
