package com.jagex.game.runetek5.movement.pathfinding;

import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.periodic.PulseEventQueue;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.Identifiable;
import com.jagex.game.runetek5.config.loc.LocShapes;
import com.jagex.game.runetek5.config.loc.LocType;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.scene.CollisionFlag;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.unidentified.*;

public class RouteFinder {
    public static final int DIR_NORTH = 0x1;
    public static final int DIR_EAST = 0x2;
    public static final int DIR_SOUTH = 0x4;
    public static final int DIR_WEST = 0x8;
    public static final int GRAPH_SIZE = 128;
    public static final int QUEUE_SIZE = 4095;
    public static int exitY;
    public static int exitX;
    public static final int[][] directions = new int[128][128];
    public static final int[][] distances = new int[128][128];
    public static final int[] bufferX = new int[4096];
    public static final int[] bufferY = new int[4096];

    RouteFinder() {
        throw new Error();
    }
    
    public static int findRoute(int srcX, int srcY, int entitySize, RouteStrategy strategy, CollisionMap clipping, boolean bool5, int[] ints_6, int[] ints_7) {
        int sceneY;
        for (int sceneX = 0; sceneX < 128; sceneX++) {
            for (sceneY = 0; sceneY < 128; sceneY++) {
                RouteFinder.directions[sceneX][sceneY] = 0;
                RouteFinder.distances[sceneX][sceneY] = 99999999;
            }
        }
        boolean canReach;
        if (entitySize == 1) {
            canReach = RouteFinder.performCalculationS1(srcX, srcY, strategy, clipping);
        } else if (entitySize == 2) {
            canReach = RouteFinder.performCalculationS2(srcX, srcY, strategy, clipping);
        } else {
            canReach = RouteFinder.performCalculationSX(srcX, srcY, entitySize, strategy, clipping);
        }
        sceneY = srcX - 64;
        int i_11 = srcY - 64;
        int pathExitX = RouteFinder.exitX;
        int pathExitY = RouteFinder.exitY;
        int i_14;
        int i_15;
        int i_17;
        if (!canReach) {
            if (!bool5) {
                return -1;
            }
            i_14 = Integer.MAX_VALUE;
            i_15 = Integer.MAX_VALUE;
            byte b_16 = 10;
            i_17 = strategy.destinationX;
            int i_18 = strategy.destinationY;
            int i_19 = strategy.destinationSizeX;
            int i_20 = strategy.destinationSizeY;
            for (int i_21 = i_17 - b_16; i_21 <= i_17 + b_16; i_21++) {
                for (int i_22 = i_18 - b_16; i_22 <= i_18 + b_16; i_22++) {
                    int i_23 = i_21 - sceneY;
                    int i_24 = i_22 - i_11;
                    if (i_23 >= 0 && i_24 >= 0 && i_23 < 128 && i_24 < 128 && RouteFinder.distances[i_23][i_24] < 100) {
                        int i_25 = 0;
                        if (i_21 < i_17) {
                            i_25 = i_17 - i_21;
                        } else if (i_21 > i_19 + i_17 - 1) {
                            i_25 = i_21 - (i_17 + i_19 - 1);
                        }
                        int i_26 = 0;
                        if (i_22 < i_18) {
                            i_26 = i_18 - i_22;
                        } else if (i_22 > i_20 + i_18 - 1) {
                            i_26 = i_22 - (i_18 + i_20 - 1);
                        }
                        int i_27 = i_25 * i_25 + i_26 * i_26;
                        if (i_27 < i_14 || i_27 == i_14 && RouteFinder.distances[i_23][i_24] < i_15) {
                            i_14 = i_27;
                            i_15 = RouteFinder.distances[i_23][i_24];
                            pathExitX = i_21;
                            pathExitY = i_22;
                        }
                    }
                }
            }
            if (i_14 == Integer.MAX_VALUE) {
                return -1;
            }
        }
        if (srcX == pathExitX && pathExitY == srcY) {
            return 0;
        } else {
            byte b_29 = 0;
            RouteFinder.bufferX[b_29] = pathExitX;
            i_14 = b_29 + 1;
            RouteFinder.bufferY[b_29] = pathExitY;
            for (int i_30 = i_15 = RouteFinder.directions[pathExitX - sceneY][pathExitY - i_11]; srcX != pathExitX || pathExitY != srcY; i_30 = RouteFinder.directions[pathExitX - sceneY][pathExitY - i_11]) {
                if (i_15 != i_30) {
                    i_15 = i_30;
                    RouteFinder.bufferX[i_14] = pathExitX;
                    RouteFinder.bufferY[i_14++] = pathExitY;
                }
                if ((i_30 & 0x2) != 0) {
                    ++pathExitX;
                } else if ((i_30 & 0x8) != 0) {
                    --pathExitX;
                }
                if ((i_30 & 0x1) != 0) {
                    ++pathExitY;
                } else if ((i_30 & 0x4) != 0) {
                    --pathExitY;
                }
            }
            i_17 = 0;
            while (i_14-- > 0) {
                ints_6[i_17] = RouteFinder.bufferX[i_14];
                ints_7[i_17++] = RouteFinder.bufferY[i_14];
                if (i_17 >= ints_6.length) {
                    break;
                }
            }
            return i_17;
        }
    }

    public static boolean performCalculationS1(int srcX, int srcY, RouteStrategy strategy, CollisionMap clipMap) {
        int currentX = srcX;
        int currentY = srcY;
        int graphBaseX = srcX - 64;
        int graphBaseY = srcY - 64;
        directions[64][64] = 99;
        distances[64][64] = 0;
        int read = 0;
        int write = 1;
        bufferX[0] = srcX;
        bufferY[0] = srcY;
        int[][] clip = clipMap.collisionFlags;
        while (read != write) {
            currentX = bufferX[read];
            currentY = bufferY[read];
            read = read + 1 & QUEUE_SIZE;
            int currentGraphX = currentX - graphBaseX;
            int currentGraphY = currentY - graphBaseY;
            int clipX = currentX - clipMap.offsetX;
            int clipY = currentY - clipMap.offsetY;
            if (strategy.canExit(1, currentX, currentY, clipMap)) {
                exitX = currentX;
                exitY = currentY;
                return true;
            }
            int nextDistance = distances[currentGraphX][currentGraphY] + 1;
            if (currentGraphX > 0 && directions[currentGraphX - 1][currentGraphY] == 0 &&
                    CollisionMap.notFlagged(clip[clipX - 1][clipY], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX - 1;
                bufferY[write] = currentY;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX - 1][currentGraphY] = DIR_EAST;
                distances[currentGraphX - 1][currentGraphY] = nextDistance;
            }
            if (currentGraphX < 127 && directions[currentGraphX + 1][currentGraphY] == 0 &&
                    CollisionMap.notFlagged(clip[clipX + 1][clipY], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX + 1;
                bufferY[write] = currentY;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX + 1][currentGraphY] = DIR_WEST;
                distances[currentGraphX + 1][currentGraphY] = nextDistance;
            }
            if (currentGraphY > 0 && directions[currentGraphX][currentGraphY - 1] == 0 &&
                    CollisionMap.notFlagged(clip[clipX][clipY - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX;
                bufferY[write] = currentY - 1;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX][currentGraphY - 1] = DIR_NORTH;
                distances[currentGraphX][currentGraphY - 1] = nextDistance;
            }
            if (currentGraphY < 127 && directions[currentGraphX][currentGraphY + 1] == 0 &&
                    CollisionMap.notFlagged(clip[clipX][clipY + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX;
                bufferY[write] = currentY + 1;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX][currentGraphY + 1] = DIR_SOUTH;
                distances[currentGraphX][currentGraphY + 1] = nextDistance;
            }
            if (currentGraphX > 0 && currentGraphY > 0 && directions[currentGraphX - 1][currentGraphY - 1] == 0 &&
                    CollisionMap.notFlagged(clip[clipX - 1][clipY - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX - 1][clipY], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX][clipY - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX - 1;
                bufferY[write] = currentY - 1;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX - 1][currentGraphY - 1] = 3;
                distances[currentGraphX - 1][currentGraphY - 1] = nextDistance;
            }
            if (currentGraphX < 127 && currentGraphY > 0 && directions[currentGraphX + 1][currentGraphY - 1] == 0 &&
                    CollisionMap.notFlagged(clip[clipX + 1][clipY - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX + 1][clipY], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX][clipY - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX + 1;
                bufferY[write] = currentY - 1;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX + 1][currentGraphY - 1] = 9;
                distances[currentGraphX + 1][currentGraphY - 1] = nextDistance;
            }
            if (currentGraphX > 0 && currentGraphY < 127 && directions[currentGraphX - 1][currentGraphY + 1] == 0 &&
                    CollisionMap.notFlagged(clip[clipX - 1][clipY + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX - 1][clipY], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX][clipY + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX - 1;
                bufferY[write] = currentY + 1;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX - 1][currentGraphY + 1] = 6;
                distances[currentGraphX - 1][currentGraphY + 1] = nextDistance;
            }
            if (currentGraphX < 127 && currentGraphY < 127 && directions[currentGraphX + 1][currentGraphY + 1] == 0 &&
                    CollisionMap.notFlagged(clip[clipX + 1][clipY + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX + 1][clipY], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) &&
                    CollisionMap.notFlagged(clip[clipX][clipY + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[write] = currentX + 1;
                bufferY[write] = currentY + 1;
                write = write + 1 & QUEUE_SIZE;
                directions[currentGraphX + 1][currentGraphY + 1] = 12;
                distances[currentGraphX + 1][currentGraphY + 1] = nextDistance;
            }
        }
        exitX = currentX;
        exitY = currentY;
        return false;
    }

    public static boolean performCalculationS2(int i_0, int i_1, RouteStrategy routestrategy2, CollisionMap clipmap3) {
        int i_5 = i_0;
        int i_6 = i_1;
        byte b_7 = 64;
        byte b_8 = 64;
        int i_9 = i_0 - b_7;
        int i_10 = i_1 - b_8;
        directions[b_7][b_8] = 99;
        distances[b_7][b_8] = 0;
        byte b_11 = 0;
        int i_12 = 0;
        bufferX[b_11] = i_0;
        byte b_10001 = b_11;
        int i_19 = b_11 + 1;
        bufferY[b_10001] = i_1;
        int[][] ints_13 = clipmap3.collisionFlags;
        while (i_12 != i_19) {
            i_5 = bufferX[i_12];
            i_6 = bufferY[i_12];
            i_12 = i_12 + 1 & QUEUE_SIZE;
            int i_17 = i_5 - i_9;
            int i_18 = i_6 - i_10;
            int i_14 = i_5 - clipmap3.offsetX;
            int i_15 = i_6 - clipmap3.offsetY;
            if (routestrategy2.canExit(2, i_5, i_6, clipmap3)) {
                exitX = i_5;
                exitY = i_6;
                return true;
            }
            int i_16 = distances[i_17][i_18] + 1;
            if (i_17 > 0 && directions[i_17 - 1][i_18] == 0 && CollisionMap.notFlagged(ints_13[i_14 - 1][i_15], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 - 1][i_15 + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5 - 1;
                bufferY[i_19] = i_6;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17 - 1][i_18] = 2;
                distances[i_17 - 1][i_18] = i_16;
            }
            if (i_17 < 126 && directions[i_17 + 1][i_18] == 0 && CollisionMap.notFlagged(ints_13[i_14 + 2][i_15], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 + 2][i_15 + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5 + 1;
                bufferY[i_19] = i_6;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17 + 1][i_18] = 8;
                distances[i_17 + 1][i_18] = i_16;
            }
            if (i_18 > 0 && directions[i_17][i_18 - 1] == 0 && CollisionMap.notFlagged(ints_13[i_14][i_15 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 + 1][i_15 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5;
                bufferY[i_19] = i_6 - 1;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17][i_18 - 1] = 1;
                distances[i_17][i_18 - 1] = i_16;
            }
            if (i_18 < 126 && directions[i_17][i_18 + 1] == 0 && CollisionMap.notFlagged(ints_13[i_14][i_15 + 2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 + 1][i_15 + 2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5;
                bufferY[i_19] = i_6 + 1;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17][i_18 + 1] = 4;
                distances[i_17][i_18 + 1] = i_16;
            }
            if (i_17 > 0 && i_18 > 0 && directions[i_17 - 1][i_18 - 1] == 0 && CollisionMap.notFlagged(ints_13[i_14 - 1][i_15], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 - 1][i_15 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14][i_15 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5 - 1;
                bufferY[i_19] = i_6 - 1;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17 - 1][i_18 - 1] = 3;
                distances[i_17 - 1][i_18 - 1] = i_16;
            }
            if (i_17 < 126 && i_18 > 0 && directions[i_17 + 1][i_18 - 1] == 0 && CollisionMap.notFlagged(ints_13[i_14 + 1][i_15 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 + 2][i_15 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 + 2][i_15], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5 + 1;
                bufferY[i_19] = i_6 - 1;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17 + 1][i_18 - 1] = 9;
                distances[i_17 + 1][i_18 - 1] = i_16;
            }
            if (i_17 > 0 && i_18 < 126 && directions[i_17 - 1][i_18 + 1] == 0 && CollisionMap.notFlagged(ints_13[i_14 - 1][i_15 + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 - 1][i_15 + 2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14][i_15 + 2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5 - 1;
                bufferY[i_19] = i_6 + 1;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17 - 1][i_18 + 1] = 6;
                distances[i_17 - 1][i_18 + 1] = i_16;
            }
            if (i_17 < 126 && i_18 < 126 && directions[i_17 + 1][i_18 + 1] == 0 && CollisionMap.notFlagged(ints_13[i_14 + 1][i_15 + 2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 + 2][i_15 + 2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_13[i_14 + 2][i_15 + 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                bufferX[i_19] = i_5 + 1;
                bufferY[i_19] = i_6 + 1;
                i_19 = i_19 + 1 & QUEUE_SIZE;
                directions[i_17 + 1][i_18 + 1] = 12;
                distances[i_17 + 1][i_18 + 1] = i_16;
            }
        }
        exitX = i_5;
        exitY = i_6;
        return false;
    }

    public static boolean performCalculationSX(int i_0, int i_1, int i_2, RouteStrategy routestrategy3, CollisionMap clipmap4) {
        int i_6 = i_0;
        int i_7 = i_1;
        byte b_8 = 64;
        byte b_9 = 64;
        int i_10 = i_0 - b_8;
        int i_11 = i_1 - b_9;
        directions[b_8][b_9] = 99;
        distances[b_8][b_9] = 0;
        byte b_12 = 0;
        int i_13 = 0;
        bufferX[b_12] = i_0;
        int i_21 = b_12 + 1;
        bufferY[b_12] = i_1;
        int[][] ints_14 = clipmap4.collisionFlags;
        label208:
        while (i_21 != i_13) {
            i_6 = bufferX[i_13];
            i_7 = bufferY[i_13];
            i_13 = i_13 + 1 & QUEUE_SIZE;
            int i_19 = i_6 - i_10;
            int i_20 = i_7 - i_11;
            int i_15 = i_6 - clipmap4.offsetX;
            int i_16 = i_7 - clipmap4.offsetY;
            if (routestrategy3.canExit(i_2, i_6, i_7, clipmap4)) {
                exitX = i_6;
                exitY = i_7;
                return true;
            }
            int i_17 = distances[i_19][i_20] + 1;
            int i_18;
            if (i_19 > 0 && directions[i_19 - 1][i_20] == 0 && CollisionMap.notFlagged(ints_14[i_15 - 1][i_16], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_14[i_15 - 1][i_16 + i_2 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                i_18 = 1;
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6 - 1;
                        bufferY[i_21] = i_7;
                        i_21 = i_21 + 1 & QUEUE_SIZE;
                        directions[i_19 - 1][i_20] = 2;
                        distances[i_19 - 1][i_20] = i_17;
                        break;
                    }
                    if (!CollisionMap.notFlagged(ints_14[i_15 - 1][i_18 + i_16], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        break;
                    }
                    ++i_18;
                }
            }
            if (i_19 < 128 - i_2 && directions[i_19 + 1][i_20] == 0 && CollisionMap.notFlagged(ints_14[i_15 + i_2][i_16], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_14[i_15 + i_2][i_16 + i_2 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                i_18 = 1;
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6 + 1;
                        bufferY[i_21] = i_7;
                        i_21 = i_21 + 1 & QUEUE_SIZE;
                        directions[i_19 + 1][i_20] = 8;
                        distances[i_19 + 1][i_20] = i_17;
                        break;
                    }
                    if (!CollisionMap.notFlagged(ints_14[i_15 + i_2][i_18 + i_16], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        break;
                    }
                    ++i_18;
                }
            }
            if (i_20 > 0 && directions[i_19][i_20 - 1] == 0 && CollisionMap.notFlagged(ints_14[i_15][i_16 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_14[i_15 + i_2 - 1][i_16 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                i_18 = 1;
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6;
                        bufferY[i_21] = i_7 - 1;
                        i_21 = i_21 + 1 & QUEUE_SIZE;
                        directions[i_19][i_20 - 1] = 1;
                        distances[i_19][i_20 - 1] = i_17;
                        break;
                    }
                    if (!CollisionMap.notFlagged(ints_14[i_18 + i_15][i_16 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        break;
                    }
                    ++i_18;
                }
            }
            if (i_20 < 128 - i_2 && directions[i_19][i_20 + 1] == 0 && CollisionMap.notFlagged(ints_14[i_15][i_16 + i_2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) && CollisionMap.notFlagged(ints_14[i_15 + i_2 - 1][i_16 + i_2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                i_18 = 1;
                while (true) {
                    if (i_18 >= i_2 - 1) {
                        bufferX[i_21] = i_6;
                        bufferY[i_21] = i_7 + 1;
                        i_21 = i_21 + 1 & QUEUE_SIZE;
                        directions[i_19][i_20 + 1] = 4;
                        distances[i_19][i_20 + 1] = i_17;
                        break;
                    }
                    if (!CollisionMap.notFlagged(ints_14[i_15 + i_18][i_16 + i_2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        break;
                    }
                    ++i_18;
                }
            }
            if (i_19 > 0 && i_20 > 0 && directions[i_19 - 1][i_20 - 1] == 0 && CollisionMap.notFlagged(ints_14[i_15 - 1][i_16 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                i_18 = 1;
                while (true) {
                    if (i_18 >= i_2) {
                        bufferX[i_21] = i_6 - 1;
                        bufferY[i_21] = i_7 - 1;
                        i_21 = i_21 + 1 & QUEUE_SIZE;
                        directions[i_19 - 1][i_20 - 1] = 3;
                        distances[i_19 - 1][i_20 - 1] = i_17;
                        break;
                    }
                    if (!CollisionMap.notFlagged(ints_14[i_15 - 1][i_18 + (i_16 - 1)], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) || !CollisionMap.notFlagged(ints_14[i_18 + (i_15 - 1)][i_16 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        break;
                    }
                    ++i_18;
                }
            }
            if (i_19 < 128 - i_2 && i_20 > 0 && directions[i_19 + 1][i_20 - 1] == 0 && CollisionMap.notFlagged(ints_14[i_15 + i_2][i_16 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                i_18 = 1;
                while (true) {
                    if (i_18 >= i_2) {
                        bufferX[i_21] = i_6 + 1;
                        bufferY[i_21] = i_7 - 1;
                        i_21 = i_21 + 1 & QUEUE_SIZE;
                        directions[i_19 + 1][i_20 - 1] = 9;
                        distances[i_19 + 1][i_20 - 1] = i_17;
                        break;
                    }
                    if (!CollisionMap.notFlagged(ints_14[i_15 + i_2][i_18 + (i_16 - 1)], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) || !CollisionMap.notFlagged(ints_14[i_18 + i_15][i_16 - 1], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        break;
                    }
                    ++i_18;
                }
            }
            if (i_19 > 0 && i_20 < 128 - i_2 && directions[i_19 - 1][i_20 + 1] == 0 && CollisionMap.notFlagged(ints_14[i_15 - 1][i_16 + i_2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                i_18 = 1;
                while (true) {
                    if (i_18 >= i_2) {
                        bufferX[i_21] = i_6 - 1;
                        bufferY[i_21] = i_7 + 1;
                        i_21 = i_21 + 1 & QUEUE_SIZE;
                        directions[i_19 - 1][i_20 + 1] = 6;
                        distances[i_19 - 1][i_20 + 1] = i_17;
                        break;
                    }
                    if (!CollisionMap.notFlagged(ints_14[i_15 - 1][i_18 + i_16], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.N_ALT_OBJ, CollisionFlag.NE_ALT_OBJ, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.ALT_OBJ) || !CollisionMap.notFlagged(ints_14[i_18 + (i_15 - 1)][i_16 + i_2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        break;
                    }
                    ++i_18;
                }
            }
            if (i_19 < 128 - i_2 && i_20 < 128 - i_2 && directions[i_19 + 1][i_20 + 1] == 0 && CollisionMap.notFlagged(ints_14[i_15 + i_2][i_16 + i_2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                for (i_18 = 1; i_18 < i_2; i_18++) {
                    if (!CollisionMap.notFlagged(ints_14[i_15 + i_18][i_16 + i_2], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.E_ALT_OBJ, CollisionFlag.SE_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ) || !CollisionMap.notFlagged(ints_14[i_15 + i_2][i_18 + i_16], CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.NW_ALT_OBJ, CollisionFlag.N_ALT_OBJ, CollisionFlag.S_ALT_OBJ, CollisionFlag.SW_ALT_OBJ, CollisionFlag.W_ALT_OBJ, CollisionFlag.ALT_OBJ)) {
                        continue label208;
                    }
                }
                bufferX[i_21] = i_6 + 1;
                bufferY[i_21] = i_7 + 1;
                i_21 = i_21 + 1 & QUEUE_SIZE;
                directions[i_19 + 1][i_20 + 1] = 12;
                distances[i_19 + 1][i_20 + 1] = i_17;
            }
        }
        exitX = i_6;
        exitY = i_7;
        return false;
    }

    public static void routeToSceneobject(int destinationX, int destinationY, long locUid) {
        int shape = (int) locUid >> 14 & 0x1f;
        int rotation = (int) locUid >> 20 & 0x3;
        int locId = (int) (locUid >>> 32) & 0x7fffffff;
        LocShapes locShape = (LocShapes) Identifiable.identify(LocShapes.values(), shape);
        RouteStrategy strategy;
        if (locShape != LocShapes.SCENERY_INTERACT && locShape != LocShapes.GROUND_INTERACT && locShape != LocShapes.GROUND_DECORATION) {
            if (LocShapes.isWall(locShape.shape)) {
                strategy = RouteStrategies.forWallObject(destinationX, destinationY, 0, 0, locShape, rotation);
            } else {
                strategy = RouteStrategies.forInteractiveObject(destinationX, destinationY, locShape, rotation);
            }
        } else {
            LocType loctype = Resource.mapRegion.getLoctypeList().list(locId);
            int sizeX;
            int sizeY;
            if (rotation != 0 && rotation != 2) {
                sizeX = loctype.sizeY;
                sizeY = loctype.sizeX;
            } else {
                sizeX = loctype.sizeX;
                sizeY = loctype.sizeY;
            }

            strategy = RouteStrategies.forWallObject(destinationX, destinationY, sizeX, sizeY, LocShapes.WALL_STRAIGHT, 0);
        }

        RouteFinder.calculateRoute(destinationX, destinationY, true, strategy);
    }

	public static boolean calculateRoute(int destinationX, int destinationY, boolean bool2, RouteStrategy strategy) {
	    int baseX = PlayerUpdate.MY_PLAYER.movementBaseX[0];
	    int baseY = PlayerUpdate.MY_PLAYER.movementBaseY[0];
	    if (baseX >= 0 && baseX < Resource.mapRegion.getSceneSizeX() && baseY >= 0 && baseY < Resource.mapRegion.getSceneSizeY()) {
	        if (destinationX >= 0 && destinationX < Resource.mapRegion.getSceneSizeX() && destinationY >= 0 && destinationY < Resource.mapRegion.getSceneSizeY()) {
	            int executedStep = findRoute(baseX, baseY, PlayerUpdate.MY_PLAYER.getSize(), strategy, Resource.mapRegion.getCollisionMap(PlayerUpdate.MY_PLAYER.plane), bool2, client.WALKING_STEPS_X, client.WALKING_STEPS_Y);
	            if (executedStep < 1) {
	                return false;
	            } 
                ComponentMinimap.minimapFlagX = client.WALKING_STEPS_X[executedStep - 1];
                ComponentMinimap.minimapFlagY = client.WALKING_STEPS_Y[executedStep - 1];
                ComponentMinimap.minimapContainsFlag = false;
                PulseEventQueue.delayedClearMinimapFlagUpdate();
                return true;
	        } 
	    } 
	    return false;
	}

	public static void routeToGrounditem(int x, int y) {
	    if (Game.STELLAR_DAWN != client.CURRENT_GAME) {
	        if (!calculateRoute(x, y, false, RouteStrategies.forGroundItem(x, y))) {
	            calculateRoute(x, y, false, RouteStrategies.forPlayer(x, y, 1, 1));
	        }
	    } else if (!calculateRoute(x, y, false, RouteStrategies.forPlayer(x, y, 1, 1))) {
	        calculateRoute(x, y, false, RouteStrategies.forGroundItem(x, y));
	    }
	
	}
}
