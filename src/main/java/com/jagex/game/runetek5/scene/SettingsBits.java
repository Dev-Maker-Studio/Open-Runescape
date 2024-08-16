package com.jagex.game.runetek5.scene;

public class SettingsBits {

    public final byte[][][] tileFunctionMasks;

    public SettingsBits(int x, int y) {
        this.tileFunctionMasks = new byte[4][x][y];
    }

    public void reset() {
        for (int z = 0; z < this.tileFunctionMasks.length; z++) {
            for (int x = 0; x < this.tileFunctionMasks[0].length; x++) {
                for (int y = 0; y < this.tileFunctionMasks[0][0].length; y++) {
                    this.tileFunctionMasks[z][x][y] = 0;
                }
            }
        }
    }

    public boolean areRoofsHidden(int x, int y) {
        return (x >= 0 && y >= 0 && x < this.tileFunctionMasks[1].length && y < this.tileFunctionMasks[1][x].length) && (this.tileFunctionMasks[1][x][y] & 0x2) != 0;
    }

    public boolean isVisible(int collisionPlane, int z, int x, int y) {
        return (this.tileFunctionMasks[0][x][y] & 0x2) != 0 || ((this.tileFunctionMasks[z][x][y] & 0x10) == 0 && getCollisionPlane(z, x, y) == collisionPlane);
    }

    public int getCollisionPlane(int z, int x, int y) {
        return (this.tileFunctionMasks[z][x][y] & 0x8) != 0 ? 0 : (z > 0 && (this.tileFunctionMasks[1][x][y] & 0x2) != 0 ? z - 1 : z);
    }
}
