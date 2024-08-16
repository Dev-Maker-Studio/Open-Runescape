package com.jagex.game.runetek5.graphics.model.billboard;

public class BillboardSurfaceDX {

	public int scaleX = 128;
	public int scaleY = 128;
	public int diffuseColor;
	public int offsetX;
	public int offsetY;
	public int rotation;

    public BillboardSurfaceDX(int diffuseColor, int scaleX, int scaleY, int offsetX, int offsetY, int rotation) {
        this.diffuseColor = diffuseColor;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotation = rotation;
    }

    public BillboardSurfaceDX(int diffuseColor) {
        this.diffuseColor = diffuseColor;
    }

    public void copyFrom(BillboardSurfaceDX copyFrom) {
        this.scaleX = copyFrom.scaleX;
        this.scaleY = copyFrom.scaleY;
        this.offsetX = copyFrom.offsetX;
        this.offsetY = copyFrom.offsetY;
        this.diffuseColor = copyFrom.diffuseColor;
        this.rotation = copyFrom.rotation;
    }

    public BillboardSurfaceDX copy() {
        return new BillboardSurfaceDX(this.diffuseColor, this.scaleX, this.scaleY, this.offsetX, this.offsetY, this.rotation);
    }

}
