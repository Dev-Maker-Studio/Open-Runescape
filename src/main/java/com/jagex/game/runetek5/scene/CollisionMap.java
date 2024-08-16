package com.jagex.game.runetek5.scene;

import java.util.ArrayList;

public class CollisionMap {

    public int[][] collisionFlags;
    public int offsetX;
    public int offsetY;
    public int sizeX;
    public int sizeY;

    public static ArrayList<CollisionFlag> getFlags(int value) {
        ArrayList<CollisionFlag> flags = new ArrayList<>();
        for (CollisionFlag f : CollisionFlag.values()) {
            if ((value & f.flag) != 0 && f != CollisionFlag.EMPTY)
                flags.add(f);
        }
        return flags;
    }

    public static boolean notFlagged(int value, CollisionFlag... flags) {
        int flag = 0;
        for (CollisionFlag f : flags)
            flag |= f.flag;
        return (value & flag) == 0;
    }

    public static CollisionMap createClipMap(int i_0, int i_1) {
        CollisionMap clipping = new CollisionMap();
        clipping.offsetX = -1;
        clipping.offsetY = -1;
        clipping.sizeX = i_0 + 1 + 5;
        clipping.sizeY = i_1 + 1 + 5;
        clipping.collisionFlags = new int[clipping.sizeX][clipping.sizeY];
        clipping.reset();
        return clipping;
    }

    public void reset() {
        for (int x = 0; x < this.sizeX; x++) {
            for (int y = 0; y < this.sizeY; y++) {
                if (x != 0 && y != 0 && x < this.sizeX - 5 && y < this.sizeY - 5) {
                    this.collisionFlags[x][y] = CollisionFlag.BLOCKED.flag;
                } else {
                    this.collisionFlags[x][y] = CollisionFlag.EMPTY.flag;
                }
            }
        }
    }

    public void flagBlockedObject(int x, int y, int sizeX, int sizeY, boolean blocked, boolean notAlt) {
        int flag = CollisionFlag.OBJ.flag;
        if (blocked) {
            flag |= CollisionFlag.BLOCKED_OBJ.flag;
        }
        if (notAlt) {
            flag |= CollisionFlag.ALT_OBJ.flag;
        }
        x -= this.offsetX;
        y -= this.offsetY;
        for (int tileX = x; tileX < x + sizeX; tileX++) {
            if (tileX >= 0 && tileX < this.sizeX) {
                for (int tileY = y; tileY < y + sizeY; tileY++) {
                    if (tileY >= 0 && tileY < this.sizeY) {
                        addFlag(tileX, tileY, flag);
                    }
                }
            }
        }
    }

    public void unflagBlocked(int x, int y, int sizeX, int sizeY, int rotation, boolean blocked, boolean notAlt) {
        int sizeX1 = sizeX;
        int sizeY1 = sizeY;
        int flag = CollisionFlag.OBJ.flag;
        if (blocked) {
            flag |= CollisionFlag.BLOCKED_OBJ.flag;
        }
        if (notAlt) {
            flag |= CollisionFlag.ALT_OBJ.flag;
        }
        x -= this.offsetX;
        y -= this.offsetY;
        int tileX;
        if (rotation == 1 || rotation == 3) {
            tileX = sizeX1;
            sizeX1 = sizeY1;
            sizeY1 = tileX;
        }
        for (tileX = x; tileX < sizeX1 + x; tileX++) {
            if (tileX >= 0 && tileX < this.sizeX) {
                for (int tileY = y; tileY < y + sizeY1; tileY++) {
                    if (tileY >= 0 && tileY < this.sizeY) {
                        removeFlag(tileX, tileY, flag);
                    }
                }
            }
        }
    }

    /**
     * was called addwall
     */
    public void flagBoundary(int x, int y, int type, int rotation, boolean blocks, boolean alt) {
        x -= this.offsetX;
        y -= this.offsetY;
        if (type == 0) {
            if (rotation == 0) {
                addFlag(x, y, CollisionFlag.W_OBJ);
                addFlag(x - 1, y, CollisionFlag.E_OBJ);
            }
            if (rotation == 1) {
                addFlag(x, y, CollisionFlag.N_OBJ);
                addFlag(x, y + 1, CollisionFlag.S_OBJ);
            }
            if (rotation == 2) {
                addFlag(x, y, CollisionFlag.E_OBJ);
                addFlag(x + 1, y, CollisionFlag.W_OBJ);
            }
            if (rotation == 3) {
                addFlag(x, y, CollisionFlag.S_OBJ);
                addFlag(x, y - 1, CollisionFlag.N_OBJ);
            }
        }
        if (type == 1 || type == 3) {
            if (rotation == 0) {
                addFlag(x, y, CollisionFlag.NW_OBJ);
                addFlag(x - 1, y + 1, CollisionFlag.SE_OBJ);
            }
            if (rotation == 1) {
                addFlag(x, y, CollisionFlag.NE_OBJ);
                addFlag(x + 1, y + 1, CollisionFlag.SW_OBJ);
            }
            if (rotation == 2) {
                addFlag(x, y, CollisionFlag.SE_OBJ);
                addFlag(x + 1, y - 1, CollisionFlag.NW_OBJ);
            }
            if (rotation == 3) {
                addFlag(x, y, CollisionFlag.SW_OBJ);
                addFlag(x - 1, y - 1, CollisionFlag.NE_OBJ);
            }
        }
        if (type == 2) {
            if (rotation == 0) {
                addFlag(x, y, CollisionFlag.N_OBJ, CollisionFlag.W_OBJ);
                addFlag(x - 1, y, CollisionFlag.E_OBJ);
                addFlag(x, y + 1, CollisionFlag.S_OBJ);
            }
            if (rotation == 1) {
                addFlag(x, y, CollisionFlag.N_OBJ, CollisionFlag.E_OBJ);
                addFlag(x, y + 1, CollisionFlag.S_OBJ);
                addFlag(x + 1, y, CollisionFlag.W_OBJ);
            }
            if (rotation == 2) {
                addFlag(x, y, CollisionFlag.E_OBJ, CollisionFlag.S_OBJ);
                addFlag(x + 1, y, CollisionFlag.W_OBJ);
                addFlag(x, y - 1, CollisionFlag.N_OBJ);
            }
            if (rotation == 3) {
                addFlag(x, y, CollisionFlag.S_OBJ, CollisionFlag.W_OBJ);
                addFlag(x, y - 1, CollisionFlag.N_OBJ);
                addFlag(x - 1, y, CollisionFlag.E_OBJ);
            }
        }
        if (blocks) {
            if (type == 0) {
                if (rotation == 0) {
                    addFlag(x, y, CollisionFlag.W_BOUND);
                    addFlag(x - 1, y, CollisionFlag.E_BOUND);
                }
                if (rotation == 1) {
                    addFlag(x, y, CollisionFlag.N_BOUND);
                    addFlag(x, y + 1, CollisionFlag.S_BOUND);
                }
                if (rotation == 2) {
                    addFlag(x, y, CollisionFlag.E_BOUND);
                    addFlag(x + 1, y, CollisionFlag.W_BOUND);
                }
                if (rotation == 3) {
                    addFlag(x, y, CollisionFlag.S_BOUND);
                    addFlag(x, y - 1, CollisionFlag.N_BOUND);
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    addFlag(x, y, CollisionFlag.NW_BOUND);
                    addFlag(x - 1, y + 1, CollisionFlag.SE_BOUND);
                }
                if (rotation == 1) {
                    addFlag(x, y, CollisionFlag.NE_BOUND);
                    addFlag(x + 1, y + 1, CollisionFlag.SW_BOUND);
                }
                if (rotation == 2) {
                    addFlag(x, y, CollisionFlag.SE_BOUND);
                    addFlag(x + 1, y - 1, CollisionFlag.NW_BOUND);
                }
                if (rotation == 3) {
                    addFlag(x, y, CollisionFlag.SW_BOUND);
                    addFlag(x - 1, y - 1, CollisionFlag.NE_BOUND);
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    addFlag(x, y, CollisionFlag.N_BOUND, CollisionFlag.W_BOUND);
                    addFlag(x - 1, y, CollisionFlag.E_BOUND);
                    addFlag(x, y + 1, CollisionFlag.S_BOUND);
                }
                if (rotation == 1) {
                    addFlag(x, y, CollisionFlag.N_BOUND, CollisionFlag.E_BOUND);
                    addFlag(x, y + 1, CollisionFlag.S_BOUND);
                    addFlag(x + 1, y, CollisionFlag.W_BOUND);
                }
                if (rotation == 2) {
                    addFlag(x, y, CollisionFlag.E_BOUND, CollisionFlag.S_BOUND);
                    addFlag(x + 1, y, CollisionFlag.W_BOUND);
                    addFlag(x, y - 1, CollisionFlag.N_BOUND);
                }
                if (rotation == 3) {
                    addFlag(x, y, CollisionFlag.S_BOUND, CollisionFlag.W_BOUND);
                    addFlag(x, y - 1, CollisionFlag.N_BOUND);
                    addFlag(x - 1, y, CollisionFlag.E_BOUND);
                }
            }
        }
        if (alt) {
            if (type == 0) {
                if (rotation == 0) {
                    addFlag(x, y, CollisionFlag.W_ALT_OBJ);
                    addFlag(x - 1, y, CollisionFlag.E_ALT_OBJ);
                }
                if (rotation == 1) {
                    addFlag(x, y, CollisionFlag.N_ALT_OBJ);
                    addFlag(x, y + 1, CollisionFlag.S_ALT_OBJ);
                }
                if (rotation == 2) {
                    addFlag(x, y, CollisionFlag.E_ALT_OBJ);
                    addFlag(x + 1, y, CollisionFlag.W_ALT_OBJ);
                }
                if (rotation == 3) {
                    addFlag(x, y, CollisionFlag.S_ALT_OBJ);
                    addFlag(x, y - 1, CollisionFlag.N_ALT_OBJ);
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    addFlag(x, y, CollisionFlag.NW_ALT_OBJ);
                    addFlag(x - 1, y + 1, CollisionFlag.SE_ALT_OBJ);
                }
                if (rotation == 1) {
                    addFlag(x, y, CollisionFlag.NE_ALT_OBJ);
                    addFlag(x + 1, y + 1, CollisionFlag.SW_ALT_OBJ);
                }
                if (rotation == 2) {
                    addFlag(x, y, CollisionFlag.SE_ALT_OBJ);
                    addFlag(x + 1, y - 1, CollisionFlag.NW_ALT_OBJ);
                }
                if (rotation == 3) {
                    addFlag(x, y, CollisionFlag.SW_ALT_OBJ);
                    addFlag(x - 1, y - 1, CollisionFlag.NE_ALT_OBJ);
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    addFlag(x, y, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ);
                    addFlag(x - 1, y, CollisionFlag.E_ALT_OBJ);
                    addFlag(x, y + 1, CollisionFlag.S_ALT_OBJ);
                }
                if (rotation == 1) {
                    addFlag(x, y, CollisionFlag.N_ALT_OBJ, CollisionFlag.E_ALT_OBJ);
                    addFlag(x, y + 1, CollisionFlag.S_ALT_OBJ);
                    addFlag(x + 1, y, CollisionFlag.W_ALT_OBJ);
                }
                if (rotation == 2) {
                    addFlag(x, y, CollisionFlag.E_ALT_OBJ, CollisionFlag.S_ALT_OBJ);
                    addFlag(x + 1, y, CollisionFlag.W_ALT_OBJ);
                    addFlag(x, y - 1, CollisionFlag.N_ALT_OBJ);
                }
                if (rotation == 3) {
                    addFlag(x, y, CollisionFlag.S_ALT_OBJ, CollisionFlag.W_ALT_OBJ);
                    addFlag(x, y - 1, CollisionFlag.N_ALT_OBJ);
                    addFlag(x - 1, y, CollisionFlag.E_ALT_OBJ);
                }
            }
        }
    }

    public void flagBlockedDecoration(int i_1, int i_2) {
        i_1 -= this.offsetX;
        i_2 -= this.offsetY;
        this.collisionFlags[i_1][i_2] |= CollisionFlag.BLOCKED_DECO.flag;
    }

    public void unflagFloorDecoration(int i_1, int i_2) {
        i_1 -= this.offsetX;
        i_2 -= this.offsetY;
        this.collisionFlags[i_1][i_2] &= ~CollisionFlag.BLOCKED_DECO.flag;
    }

    public void setUnwalkable(int i_1, int i_2) {
        i_1 -= this.offsetX;
        i_2 -= this.offsetY;
        this.collisionFlags[i_1][i_2] |= CollisionFlag.BLOCKED.flag;
    }

    public void unblock(int sceneX, int sceneY) {
        sceneX -= this.offsetX;
        sceneY -= this.offsetY;
        this.collisionFlags[sceneX][sceneY] &= ~CollisionFlag.BLOCKED.flag;
    }

    public void removeFlag(int i_1, int i_2, int i_3) {
        this.collisionFlags[i_1][i_2] &= ~i_3;
    }

    public void addFlag(int i_1, int i_2, int i_3) {
        this.collisionFlags[i_1][i_2] |= i_3;
    }

    public void removeFlag(int i_1, int i_2, CollisionFlag... flags) {
        int flag = 0;
        for (CollisionFlag f : flags)
            flag |= f.flag;
        removeFlag(i_1, i_2, flag);
    }

    public void addFlag(int i_1, int i_2, CollisionFlag... flags) {
        int flag = 0;
        for (CollisionFlag f : flags)
            flag |= f.flag;
        addFlag(i_1, i_2, flag);
    }

    public boolean walkable(int i_1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, int flag) {
        int i_11 = i_3 + i_1;
        int i_12 = i_2 + i_4;
        int i_13 = i_5 + i_7;
        int i_14 = i_8 + i_6;
        int i_15;
        int i_16;
        if (i_13 == i_1 && notFlagged(flag, CollisionFlag.N_OBJ)) {
            i_15 = Math.max(i_2, i_6);
            for (i_16 = Math.min(i_12, i_14); i_15 < i_16; i_15++) {
                if (notFlagged(this.collisionFlags[i_13 - 1 - this.offsetX][i_15 - this.offsetY], CollisionFlag.E_OBJ)) {
                    return true;
                }
            }
        } else if (i_5 == i_11 && notFlagged(flag, CollisionFlag.E_OBJ)) {
            i_15 = Math.max(i_2, i_6);
            for (i_16 = Math.min(i_12, i_14); i_15 < i_16; i_15++) {
                if (notFlagged(this.collisionFlags[i_5 - this.offsetX][i_15 - this.offsetY], CollisionFlag.W_OBJ)) {
                    return true;
                }
            }
        } else if (i_14 == i_2 && notFlagged(flag, CollisionFlag.NW_OBJ)) {
            i_15 = Math.max(i_1, i_5);
            for (i_16 = Math.min(i_11, i_13); i_15 < i_16; i_15++) {
                if (notFlagged(this.collisionFlags[i_15 - this.offsetX][i_14 - 1 - this.offsetY], CollisionFlag.N_OBJ)) {
                    return true;
                }
            }
        } else if (i_12 == i_6 && notFlagged(flag, CollisionFlag.NE_OBJ)) {
            i_15 = Math.max(i_1, i_5);
            for (i_16 = Math.min(i_11, i_13); i_15 < i_16; i_15++) {
                if (notFlagged(this.collisionFlags[i_15 - this.offsetX][i_6 - this.offsetY], CollisionFlag.S_OBJ)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canReachLoc(int srcX, int srcY, int size, int dstX, int dstY, int shape, int rotation) {
        if (size == 1) {
            if (dstX == srcX && dstY == srcY) {
                return true;
            }
        } else if (dstX >= srcX && dstX <= size + srcX - 1 && dstY <= size + dstY - 1) {
            return true;
        }
        srcX -= this.offsetX;
        srcY -= this.offsetY;
        dstX -= this.offsetX;
        dstY -= this.offsetY;
        if (size == 1) {
            if (shape == 0) {
                if (rotation == 0) {
                    if (dstX - 1 == srcX && dstY == srcY) {
                        return true;
                    }
                    if (dstX == srcX && dstY + 1 == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY - 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 1) {
                    if (dstX == srcX && dstY + 1 == srcY) {
                        return true;
                    }
                    if (dstX - 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 2) {
                    if (dstX + 1 == srcX && dstY == srcY) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY + 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY - 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 3) {
                    if (dstX == srcX && srcY == dstY - 1) {
                        return true;
                    }
                    if (dstX - 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                }
            }
            if (shape == 2) {
                if (rotation == 0) {
                    if (dstX - 1 == srcX && dstY == srcY) {
                        return true;
                    }
                    if (dstX == srcX && dstY + 1 == srcY) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY - 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 1) {
                    if (dstX - 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX == srcX && dstY + 1 == srcY) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY == srcY) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY - 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 2) {
                    if (dstX - 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY + 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY == srcY) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY - 1) {
                        return true;
                    }
                } else if (rotation == 3) {
                    if (dstX - 1 == srcX && dstY == srcY) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY + 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX == srcX && srcY == dstY - 1) {
                        return true;
                    }
                }
            }
            if (shape == 9) {
                if (dstX == srcX && srcY == dstY + 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.S_OBJ)) {
                    return true;
                }
                if (dstX == srcX && srcY == dstY - 1 && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.N_OBJ)) {
                    return true;
                }
                if (dstX - 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.E_OBJ)) {
                    return true;
                }
                return dstX + 1 == srcX && dstY == srcY && notFlagged(this.collisionFlags[srcX][srcY], CollisionFlag.W_OBJ);
            }
        } else {
            int endX = size + srcX - 1;
            int endY = size + srcY - 1;
            if (shape == 0) {
                if (rotation == 0) {
                    if (dstX - size == srcX && dstY >= srcY && dstY <= endY) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && srcY == dstY + 1 && notFlagged(this.collisionFlags[dstX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY - size == srcY && notFlagged(this.collisionFlags[dstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 1) {
                    if (dstX >= srcX && dstX <= endX && dstY + 1 == srcY) {
                        return true;
                    }
                    if (dstX - size == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[endX][dstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[srcX][dstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 2) {
                    if (dstX + 1 == srcX && dstY >= srcY && dstY <= endY) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY + 1 == srcY && notFlagged(this.collisionFlags[dstX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY - size == srcY && notFlagged(this.collisionFlags[dstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 3) {
                    if (dstX >= srcX && dstX <= endX && dstY - size == srcY) {
                        return true;
                    }
                    if (dstX - size == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[endX][dstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[srcX][dstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                }
            }
            if (shape == 2) {
                if (rotation == 0) {
                    if (dstX - size == srcX && dstY >= srcY && dstY <= endY) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY + 1 == srcY) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[srcX][dstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY - size == srcY && notFlagged(this.collisionFlags[dstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 1) {
                    if (dstX - size == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[endX][dstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY + 1 == srcY) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY >= srcY && dstY <= endY) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY - size == srcY && notFlagged(this.collisionFlags[dstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                } else if (rotation == 2) {
                    if (dstX - size == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[endX][dstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY + 1 == srcY && notFlagged(this.collisionFlags[dstX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY >= srcY && dstY <= endY) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY - size == srcY) {
                        return true;
                    }
                } else if (rotation == 3) {
                    if (dstX - size == srcX && dstY >= srcY && dstY <= endY) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && srcY == dstY + 1 && notFlagged(this.collisionFlags[dstX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX + 1 == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[srcX][dstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                        return true;
                    }
                    if (dstX >= srcX && dstX <= endX && dstY - size == srcY) {
                        return true;
                    }
                }
            }
            if (shape == 9) {
                if (dstX >= srcX && dstX <= endX && dstY + 1 == srcY && notFlagged(this.collisionFlags[dstX][srcY], CollisionFlag.S_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                    return true;
                }
                if (dstX >= srcX && dstX <= endX && dstY - size == srcY && notFlagged(this.collisionFlags[dstX][endY], CollisionFlag.N_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                    return true;
                }
                if (dstX - size == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[endX][dstY], CollisionFlag.E_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED)) {
                    return true;
                }
                return dstX + 1 == srcX && dstY >= srcY && dstY <= endY && notFlagged(this.collisionFlags[srcX][dstY], CollisionFlag.W_OBJ, CollisionFlag.OBJ, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED);
            }
        }
        return false;
    }

    public boolean canReachNpc(int srcX, int srcY, int size, int dstX, int dstY, int dstSizeX, int dstSizeY, int clippingFlag) {
        if (size > 1) {
            return CollisionMap.canReach(srcX, srcY, size, size, dstX, dstY, dstSizeX, dstSizeY) || walkable(srcX, srcY, size, size, dstX, dstY, dstSizeX, dstSizeY, clippingFlag);
        } else {
            int endX = dstX + dstSizeX - 1;
            int endY = dstY + dstSizeY - 1;
            return srcX >= dstX && srcX <= endX && srcY >= dstY && srcY <= endY || (dstX - 1 == srcX && srcY >= dstY && srcY <= endY && notFlagged(this.collisionFlags[srcX - this.offsetX][srcY - this.offsetY], CollisionFlag.E_OBJ) && notFlagged(clippingFlag, CollisionFlag.E_OBJ) || (endX + 1 == srcX && srcY >= dstY && srcY <= endY && notFlagged(this.collisionFlags[srcX - this.offsetX][srcY - this.offsetY], CollisionFlag.W_OBJ) && notFlagged(clippingFlag, CollisionFlag.N_OBJ) || (srcY == dstY - 1 && srcX >= dstX && srcX <= endX && notFlagged(this.collisionFlags[srcX - this.offsetX][srcY - this.offsetY], CollisionFlag.N_OBJ) && notFlagged(clippingFlag, CollisionFlag.NE_OBJ) || srcY == endY + 1 && srcX >= dstX && srcX <= endX && notFlagged(this.collisionFlags[srcX - this.offsetX][srcY - this.offsetY], CollisionFlag.S_OBJ) && notFlagged(clippingFlag, CollisionFlag.NW_OBJ))));
        }
    }

    public void unflagBoundary(int x, int y, int type, int rotation, boolean blocks, boolean notAlternative) {
        x -= this.offsetX;
        y -= this.offsetY;
        if (type == 0) {
            if (rotation == 0) {
                removeFlag(x, y, CollisionFlag.W_OBJ);
                removeFlag(x - 1, y, CollisionFlag.E_OBJ);
            }
            if (rotation == 1) {
                removeFlag(x, y, CollisionFlag.N_OBJ);
                removeFlag(x, y + 1, CollisionFlag.S_OBJ);
            }
            if (rotation == 2) {
                removeFlag(x, y, CollisionFlag.E_OBJ);
                removeFlag(x + 1, y, CollisionFlag.W_OBJ);
            }
            if (rotation == 3) {
                removeFlag(x, y, CollisionFlag.S_OBJ);
                removeFlag(x, y - 1, CollisionFlag.N_OBJ);
            }
        }
        if (type == 1 || type == 3) {
            if (rotation == 0) {
                removeFlag(x, y, CollisionFlag.NW_OBJ);
                removeFlag(x - 1, y + 1, CollisionFlag.SE_OBJ);
            }
            if (rotation == 1) {
                removeFlag(x, y, CollisionFlag.NE_OBJ);
                removeFlag(x + 1, y + 1, CollisionFlag.SW_OBJ);
            }
            if (rotation == 2) {
                removeFlag(x, y, CollisionFlag.SE_OBJ);
                removeFlag(x + 1, y - 1, CollisionFlag.NW_OBJ);
            }
            if (rotation == 3) {
                removeFlag(x, y, CollisionFlag.SW_OBJ);
                removeFlag(x - 1, y - 1, CollisionFlag.NE_OBJ);
            }
        }
        if (type == 2) {
            if (rotation == 0) {
                removeFlag(x, y, CollisionFlag.N_OBJ, CollisionFlag.W_OBJ);
                removeFlag(x - 1, y, CollisionFlag.E_OBJ);
                removeFlag(x, y + 1, CollisionFlag.S_OBJ);
            }
            if (rotation == 1) {
                removeFlag(x, y, CollisionFlag.N_OBJ, CollisionFlag.E_OBJ);
                removeFlag(x, y + 1, CollisionFlag.S_OBJ);
                removeFlag(x + 1, y, CollisionFlag.W_OBJ);
            }
            if (rotation == 2) {
                removeFlag(x, y, CollisionFlag.E_OBJ, CollisionFlag.S_OBJ);
                removeFlag(x + 1, y, CollisionFlag.W_OBJ);
                removeFlag(x, y - 1, CollisionFlag.N_OBJ);
            }
            if (rotation == 3) {
                removeFlag(x, y, CollisionFlag.S_OBJ, CollisionFlag.W_OBJ);
                removeFlag(x, y - 1, CollisionFlag.N_OBJ);
                removeFlag(x - 1, y, CollisionFlag.E_OBJ);
            }
        }
        if (blocks) {
            if (type == 0) {
                if (rotation == 0) {
                    removeFlag(x, y, CollisionFlag.W_BOUND);
                    removeFlag(x - 1, y, CollisionFlag.E_BOUND);
                }
                if (rotation == 1) {
                    removeFlag(x, y, CollisionFlag.N_BOUND);
                    removeFlag(x, y + 1, CollisionFlag.S_BOUND);
                }
                if (rotation == 2) {
                    removeFlag(x, y, CollisionFlag.E_BOUND);
                    removeFlag(x + 1, y, CollisionFlag.W_BOUND);
                }
                if (rotation == 3) {
                    removeFlag(x, y, CollisionFlag.S_BOUND);
                    removeFlag(x, y - 1, CollisionFlag.N_BOUND);
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    removeFlag(x, y, CollisionFlag.NW_BOUND);
                    removeFlag(x - 1, y + 1, CollisionFlag.SE_BOUND);
                }
                if (rotation == 1) {
                    removeFlag(x, y, CollisionFlag.NE_BOUND);
                    removeFlag(x + 1, y + 1, CollisionFlag.SW_BOUND);
                }
                if (rotation == 2) {
                    removeFlag(x, y, CollisionFlag.SE_BOUND);
                    removeFlag(x + 1, y - 1, CollisionFlag.NW_BOUND);
                }
                if (rotation == 3) {
                    removeFlag(x, y, CollisionFlag.SW_BOUND);
                    removeFlag(x - 1, y - 1, CollisionFlag.NE_BOUND);
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    removeFlag(x, y, CollisionFlag.N_BOUND, CollisionFlag.W_BOUND);
                    removeFlag(x - 1, y, CollisionFlag.E_BOUND);
                    removeFlag(x, y + 1, CollisionFlag.S_BOUND);
                }
                if (rotation == 1) {
                    removeFlag(x, y, CollisionFlag.N_BOUND, CollisionFlag.E_BOUND);
                    removeFlag(x, y + 1, CollisionFlag.S_BOUND);
                    removeFlag(x + 1, y, CollisionFlag.W_BOUND);
                }
                if (rotation == 2) {
                    removeFlag(x, y, CollisionFlag.E_BOUND, CollisionFlag.S_BOUND);
                    removeFlag(x + 1, y, CollisionFlag.W_BOUND);
                    removeFlag(x, y - 1, CollisionFlag.N_BOUND);
                }
                if (rotation == 3) {
                    removeFlag(x, y, CollisionFlag.S_BOUND, CollisionFlag.W_BOUND);
                    removeFlag(x, y - 1, CollisionFlag.N_BOUND);
                    removeFlag(x - 1, y, CollisionFlag.E_BOUND);
                }
            }
        }
        if (notAlternative) {
            if (type == 0) {
                if (rotation == 0) {
                    removeFlag(x, y, CollisionFlag.W_ALT_OBJ);
                    removeFlag(x - 1, y, CollisionFlag.E_ALT_OBJ);
                }
                if (rotation == 1) {
                    removeFlag(x, y, CollisionFlag.N_ALT_OBJ);
                    removeFlag(x, y + 1, CollisionFlag.S_ALT_OBJ);
                }
                if (rotation == 2) {
                    removeFlag(x, y, CollisionFlag.E_ALT_OBJ);
                    removeFlag(x + 1, y, CollisionFlag.W_ALT_OBJ);
                }
                if (rotation == 3) {
                    removeFlag(x, y, CollisionFlag.S_ALT_OBJ);
                    removeFlag(x, y - 1, CollisionFlag.N_ALT_OBJ);
                }
            }
            if (type == 1 || type == 3) {
                if (rotation == 0) {
                    removeFlag(x, y, CollisionFlag.NW_ALT_OBJ);
                    removeFlag(x - 1, y + 1, CollisionFlag.SE_ALT_OBJ);
                }
                if (rotation == 1) {
                    removeFlag(x, y, CollisionFlag.NE_ALT_OBJ);
                    removeFlag(x + 1, y + 1, CollisionFlag.SW_ALT_OBJ);
                }
                if (rotation == 2) {
                    removeFlag(x, y, CollisionFlag.SE_ALT_OBJ);
                    removeFlag(x + 1, y - 1, CollisionFlag.NW_ALT_OBJ);
                }
                if (rotation == 3) {
                    removeFlag(x, y, CollisionFlag.SW_ALT_OBJ);
                    removeFlag(x - 1, y - 1, CollisionFlag.NE_ALT_OBJ);
                }
            }
            if (type == 2) {
                if (rotation == 0) {
                    removeFlag(x, y, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ);
                    removeFlag(x - 1, y, CollisionFlag.E_ALT_OBJ);
                    removeFlag(x, y + 1, CollisionFlag.S_ALT_OBJ);
                }
                if (rotation == 1) {
                    removeFlag(x, y, CollisionFlag.N_ALT_OBJ, CollisionFlag.E_ALT_OBJ);
                    removeFlag(x, y + 1, CollisionFlag.S_ALT_OBJ);
                    removeFlag(x + 1, y, CollisionFlag.W_ALT_OBJ);
                }
                if (rotation == 2) {
                    removeFlag(x, y, CollisionFlag.E_ALT_OBJ, CollisionFlag.S_ALT_OBJ);
                    removeFlag(x + 1, y, CollisionFlag.W_ALT_OBJ);
                    removeFlag(x, y - 1, CollisionFlag.N_ALT_OBJ);
                }
                if (rotation == 3) {
                    removeFlag(x, y, CollisionFlag.S_ALT_OBJ, CollisionFlag.W_ALT_OBJ);
                    removeFlag(x, y - 1, CollisionFlag.N_ALT_OBJ);
                    removeFlag(x - 1, y, CollisionFlag.E_ALT_OBJ);
                }
            }
        }
    }

	public static boolean canReach(int srcX, int srcY, int sizeX, int sizeY, int dstX, int dstY, int dstSizeX, int dstSizeY) {
	    return (srcX < dstX + dstSizeX && dstX < srcX + sizeX) && (srcY < dstY + dstSizeY && dstY < sizeY + srcY);
	}
}
