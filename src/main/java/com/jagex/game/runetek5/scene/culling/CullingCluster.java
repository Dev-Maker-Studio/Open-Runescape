package com.jagex.game.runetek5.scene.culling;

import com.jagex.game.runetek5.scene.Scene;

public class CullingCluster {

    public final byte type;

    public final byte plane;

    public final int[] tileCullingsX; // world_x

    public final int[] drawHeights;

    public final int[] tileCullingsZ; // world_y

    public final short startX;

    public final short endX;

    public final short startY;

    public final short endY;

    public final short[] projectionX;

    public final short[] projectionY;

    public final short[] projectionZ;

    //new CullingCluster(scene, displacement, z, tile_end_x, tile_start_x, tile_start_x, tile_end_x, height_south_east, height_north_west, height_north_west - i_5, height_south_east - i_5, tile_end_y, tile_start_y, tile_start_y, tile_end_y);
    public CullingCluster(Scene scene, int type, int plane, int tileEndX, int tileStartX, int tileStartX2, int tileEndX2, int heightSouthWest, int heightNorthEast, int i_10, int i_11, int tileEndY, int tileStartY, int tileStartY2, int tileEndY2) {
        this.type = (byte) type;
        this.plane = (byte) plane;
        this.tileCullingsX = new int[4];
        this.drawHeights = new int[4];
        this.tileCullingsZ = new int[4];
        this.tileCullingsX[0] = tileEndX;
        this.tileCullingsX[1] = tileStartX;
        this.tileCullingsX[2] = tileStartX2;
        this.tileCullingsX[3] = tileEndX2;
        this.drawHeights[0] = heightSouthWest;
        this.drawHeights[1] = heightNorthEast;
        this.drawHeights[2] = i_10;
        this.drawHeights[3] = i_11;
        this.tileCullingsZ[0] = tileEndY;
        this.tileCullingsZ[1] = tileStartY;
        this.tileCullingsZ[2] = tileStartY2;
        this.tileCullingsZ[3] = tileEndY2;
        this.startX = (short) (tileEndX >> scene.tileUnits);
        this.endX = (short) (tileStartX2 >> scene.tileUnits);
        this.startY = (short) (tileEndY >> scene.tileUnits);
        this.endY = (short) (tileStartY2 >> scene.tileUnits);
        this.projectionX = new short[4];
        this.projectionY = new short[4];
        this.projectionZ = new short[4];
    }
}
