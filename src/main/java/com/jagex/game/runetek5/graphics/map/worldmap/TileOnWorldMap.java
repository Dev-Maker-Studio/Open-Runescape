package com.jagex.game.runetek5.graphics.map.worldmap;

/**
 * 
 * @author Brainpower - 28/01/2023
 *
 */
public class TileOnWorldMap {

	public final byte mapsquarePosX;
	public final byte mapsquarePosY;
	public int underlayIdOrColor;
	public final byte overlayId;
	public final byte tileShapeAndRotation;
	public final int[] locIds;
	public final byte[] shapeAndRotations;

    public TileOnWorldMap(int mapsquarePosX, int mapsquarePosY, int underlayId, int overlayId, int tileShapeAndRotation, int[] locIds, byte[] shapeAndRotations) {
        this.mapsquarePosX = (byte) mapsquarePosX;
        this.mapsquarePosY = (byte) mapsquarePosY;
        this.underlayIdOrColor = underlayId;
        this.overlayId = (byte) overlayId;
        this.tileShapeAndRotation = (byte) tileShapeAndRotation;
        this.locIds = locIds;
        this.shapeAndRotations = shapeAndRotations;
    }

}
