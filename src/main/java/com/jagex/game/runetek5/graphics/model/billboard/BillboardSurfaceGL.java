package com.jagex.game.runetek5.graphics.model.billboard;

public class BillboardSurfaceGL {

	public int scaleX = 128;
	public int scaleY = 128;
	public int diffuseColor;
	public int offsetX;
	public int offsetY;
	public int rotation;

	public BillboardSurfaceGL(int diffuseColor) {
		this.diffuseColor = diffuseColor;
    }

    public BillboardSurfaceGL(int diffuseColor, int scaleX, int scaleY, int offsetX, int offsetY, int rotation) {
        this.diffuseColor = diffuseColor;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotation = rotation;
    }

    public void copyFrom(BillboardSurfaceGL class1451) {
        this.scaleX = class1451.scaleX;
        this.scaleY = class1451.scaleY;
        this.offsetX = class1451.offsetX;
        this.offsetY = class1451.offsetY;
        this.diffuseColor = class1451.diffuseColor;
        this.rotation = class1451.rotation;
    }
    
    public BillboardSurfaceGL copy() {
        return new BillboardSurfaceGL(this.diffuseColor, this.scaleX, this.scaleY, this.offsetX, this.offsetY, this.rotation);
    }
}
