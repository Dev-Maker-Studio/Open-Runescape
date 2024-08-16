package com.jagex.game.runetek5.graphics.model.billboard;

public class BillboardSurface {

    public float z;
    public int x;
    public int y;
    public int size2d;
    public int size3d;
    
    /**
     * Size_2d / size_x
     */
    public float scaleX = 1.0F;
    
    /**
     * Size_3d / size_y
     */
    public float scaleY = 1.0F;
    
    /**
     * The horizontal offset from the model
     * (the gap between the model and the billboard on the x-axis)
     */
    public int offsetX;
    
    /**
     * The vertical offset from the model
     * (the gap between the model and the billboard on the y-axis)
     */
    public int offsetY;
    public int diffuseColor;
    public int rotation;

    public BillboardSurface(int colorRgb, float scaleX, float scaleY, int x, int y, int rotation) {
    	this.diffuseColor = colorRgb;
    	this.scaleX = scaleX;
    	this.scaleY = scaleY;
    	this.offsetX = x;
    	this.offsetY = y;
    	this.rotation = rotation;
    }

    public BillboardSurface(int diffuseColorRgb) {
        this.diffuseColor = diffuseColorRgb;
    }

    public void copy(BillboardSurface colorAttribute) {
    	this.scaleX = colorAttribute.scaleX;
    	this.scaleY = colorAttribute.scaleY;
    	this.offsetX = colorAttribute.offsetX;
    	this.offsetY = colorAttribute.offsetY;
    	this.diffuseColor = colorAttribute.diffuseColor;
    	this.rotation = colorAttribute.rotation;
    }

    public BillboardSurface copy() {
        return new BillboardSurface(this.diffuseColor, this.scaleX, this.scaleY, this.offsetX, this.offsetY, this.rotation);
    }
}
