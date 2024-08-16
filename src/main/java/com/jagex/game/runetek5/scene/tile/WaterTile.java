package com.jagex.game.runetek5.scene.tile;

public class WaterTile {

    public int color;
    public int scale;
    public int intensity;
    public int floOpcode20;
    public int floUnusedOpcode21;
    public int floUnusedOpcode22;
    public final int anInt948;

    public WaterTile() {
        this.color = 1583160;
        this.scale = 40;
        this.intensity = 127;
        this.anInt948 = 63;
        this.floOpcode20 = 0;
        this.floUnusedOpcode21 = 0;
        this.floUnusedOpcode22 = 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WaterTile tile))
            return false;
        return this.color == tile.color && this.scale == tile.scale && this.intensity == tile.intensity && this.anInt948 == tile.anInt948 && this.floOpcode20 == tile.floOpcode20 && this.floUnusedOpcode21 == tile.floUnusedOpcode21 && this.floUnusedOpcode22 == tile.floUnusedOpcode22;
    }
}
