package com.jagex.game.runetek5.entity;

public class BoundingBoxPolygon extends EntityNode {

    public int top;
    public int left;
    public int bottom;
    public int right;
    public int size; // TODO: better name?
    public boolean positionCalculated; //TODO: is this correct?

    public boolean hovered(int mouseX, int mouseY) {
        if (!this.positionCalculated) {
            return false;
        } 
        int width = this.right - this.left;
        int length = this.top - this.bottom;
        int size2d = width * width + length * length;
        int referenceSize2d = width * mouseX + length * mouseY - (width * this.left + length * this.bottom);
        if (referenceSize2d <= 0) {
            int offsetLeft = this.left - mouseX;
            int offsetBottom = this.bottom - mouseY;
            return offsetLeft * offsetLeft + offsetBottom * offsetBottom < this.size * this.size;
        } 
        if (referenceSize2d > size2d) {
            int relativeX = this.right - mouseX;
            int relativeY = this.top - mouseY;
            return relativeX * relativeX + relativeY * relativeY < this.size * this.size;
        } 
        referenceSize2d = (referenceSize2d << 10) / size2d;
        int l = (width * referenceSize2d >> 10) + this.left - mouseX;
        int t = (length * referenceSize2d >> 10) + this.bottom - mouseY;
        return l * l + t * t < this.size * this.size;
    }
}
