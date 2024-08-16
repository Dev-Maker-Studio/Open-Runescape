package com.jagex.game.runetek5.movement;

/**
 * [J]
 * < Fully refactored >
 * @author Brainpower - 28/05/2020
 * Respresents a 3 dimentional world view grid
 *
 */
public class CoordGrid {

    /**
     * The floor (z) respresented
     */
    public final int plane;
    
    /**
     * The position in the width (x)
     */
    public int x;
    
    /**
     * The position in length (y)
     */
    public int y;

    /**
     * Creates a coordgrid from a bitpacked position
     */
    public CoordGrid(int bitpacked) {
        if (bitpacked == -1) {
            this.plane = -1;
        } else {
        	this.plane = bitpacked >> 28 & 0x3;
        	this.x = bitpacked >> 14 & 0x3fff;
        	this.y = bitpacked & 0x3fff;
        }
    }

    /**
     * Creates a coordgrid that needs manipulation before use
     */
    public CoordGrid() {
    	this.plane = -1;
    }

    /**
     * Create a coordgrid directly
     */
    public CoordGrid(int plane, int x, int y) {
    	this.plane = plane;
    	this.x = x;
    	this.y = y;
    }

    /**
     * Plane, ?, ?, base_x, base_y
     */
    public String toString() {
        return this.plane + "," + (this.x >> 6) + "," + (this.y >> 6) + "," + (this.x & 0x3f) + "," + (this.y & 0x3f);
    }
}
