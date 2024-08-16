package com.jagex.game.runetek5.graphics.model.particle;

import com.jagex.game.runetek5.graphics.model.Model;

/**
 * Axis-aligned bounding box (AABB) ?
 * @author Steven - 17/10/2023
 *
 */
public class Silhouette {

    public int absX;
    public int absY;
    public int absZ;
    public int size3d;
    public int minX;
    public int maxX;
    public int minY;
    public int maxY;
    public int minZ;
    public int maxZ;

    public Silhouette(int absX, int absY, int absZ, int size2d, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        this.absX = absX;
        this.absY = absY;
        this.absZ = absZ;
        this.size3d = size2d * size2d;
        this.minX = this.absX + minX;
        this.maxX = this.absX + maxX;
        this.minY = this.absY + minY;
        this.maxY = this.absY + maxY;
        this.minZ = this.absZ + minZ;
        this.maxZ = this.absZ + maxZ;
    }

    /**
     * 
     */
    public void set(int absX, int absY, int absZ, int size2d, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    	this.absX = absX;
    	this.absY = absY;
    	this.absZ = absZ;
    	this.size3d = size2d * size2d;
    	this.minX = this.absX + minX;
    	this.maxX = this.absX + maxX;
    	this.minY = this.absY + minY;
    	this.maxY = this.absY + maxY;
    	this.minZ = this.absZ + minZ;
    	this.maxZ = this.absZ + maxZ;
    }

    /**
     * 
     */
    public boolean testIntersection(int posX, int posY, int posZ) {
        if (posX < this.minX || posX > this.maxX) {
            return false;
        }
        if (posY < this.minY || posY > this.maxY) {
            return false;
        }
        if (posZ < this.minZ || posZ > this.maxZ) {
            return false;
        }
        int sizex = posX - this.absX;
        int sizez = posZ - this.absZ;
        return sizex * sizex + sizez * sizez < this.size3d;
    }

    /**
     * 
     */
	public static void set(Silhouette silhouette, int x, int y, int z, Model model) {
	    if (model != null) {
	        silhouette.set(x, y, z, model.getSize2d(), model.getMinX(), model.getMaxX(), model.getMinY(), model.getMaxY(), model.getMinZ(), model.getMaxZ());
	    }
	}

	public static Silhouette generate(int x, int y, int z, Model model) {
		if (model == null) {
			return null;
		}
		Silhouette silhouette = new Silhouette(x, y, z, model.getSize2d(), model.getMinX(), model.getMaxX(),
				model.getMinY(), model.getMaxY(), model.getMinZ(), model.getMaxZ());
		return silhouette;
	}

}
