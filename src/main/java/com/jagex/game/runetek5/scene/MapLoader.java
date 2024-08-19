package com.jagex.game.runetek5.scene;

import static com.jagex.game.runetek5.scene.tile.TileFunctionMask.*;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.flo.FloType;
import com.jagex.game.runetek5.config.flo.FloTypeList;
import com.jagex.game.runetek5.config.flu.FluType;
import com.jagex.game.runetek5.config.flu.FluTypeList;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.scene.environment.EnvironmentManager;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.utils.ColorUtil;

public class MapLoader {

    public static final int[] OVERLAY_FACE_COUNT = {2, 1, 1, 1, 2, 2, 2, 1, 3, 3, 3, 2, 0, 4, 0};
    public static final int[] UNDERLAY_FACE_COUNT = {0, 1, 2, 2, 1, 1, 2, 3, 1, 3, 3, 4, 2, 0, 4};
    public static final int[] anIntArray3826 = {4, 2, 1, 1, 2, 2, 3, 1, 3, 3, 3, 2, 0};
    public static final int[] anIntArray3847 = {0, 2, 2, 2, 1, 1, 3, 3, 1, 3, 3, 4, 4};
    public static final int[] OVERLAY_FACE_COUNT_BLENDED = {4, 2, 1, 1, 2, 2, 3, 1, 3, 3, 3, 2, 0};
    public static final int[] UNDERLAY_FACE_COUNT_BLENDED = {0, 4, 3, 3, 1, 1, 3, 5, 1, 5, 3, 6, 4};
    public static final int[] tileSizeDeltasX = {0, 256, 512, 512, 512, 256, 0, 0, 128, 256, 128, 384, 256};
    public static final int[] tileSizeDeltasY = {0, 0, 0, 256, 512, 512, 512, 256, 256, 384, 128, 128, 256};
    public static final boolean[][] OVERLAY_SHAPE_SUPPORTS_HEIGHT = {{true, true, true, true, true, true, true, true, true, true, true, true, true}, {true, true, true, false, false, false, true, true, false, false, false, false, true}, {true, false, false, false, false, true, true, true, false, false, false, false, false}, {false, false, true, true, true, true, false, false, false, false, false, false, false}, {true, true, true, true, true, true, false, false, false, false, false, false, false}, {true, true, true, false, false, true, true, true, false, false, false, false, false}, {true, true, false, false, false, true, true, true, false, false, false, false, true}, {true, true, false, false, false, false, false, true, false, false, false, false, false}, {false, true, true, true, true, true, true, true, false, false, false, false, false}, {true, false, false, false, true, true, true, true, true, true, false, false, false}, {true, true, true, true, true, false, false, false, true, true, false, false, false}, {true, true, true, false, false, false, false, false, false, false, true, true, false}, new boolean[13], {true, true, true, true, true, true, true, true, true, true, true, true, true}, new boolean[13]};
    public static final boolean[][] aBoolArrayArray3823 = {new boolean[13], {false, false, true, true, true, true, true, false, false, false, false, false, true}, {true, true, true, true, true, true, false, false, false, false, false, false, false}, {true, true, true, false, false, true, true, true, false, false, false, false, false}, {true, false, false, false, false, true, true, true, false, false, false, false, false}, {false, false, true, true, true, true, false, false, false, false, false, false, false}, {false, true, true, true, true, true, false, false, false, false, false, false, true}, {false, true, true, true, true, true, true, true, false, false, false, false, true}, {true, true, false, false, false, false, false, true, false, false, false, false, false}, {true, true, true, true, true, false, false, false, true, true, false, false, false}, {true, false, false, false, true, true, true, true, true, true, false, false, false}, {true, false, true, true, true, true, true, true, false, false, true, true, false}, {true, true, true, true, true, true, true, true, true, true, true, true, true}, new boolean[13], {true, true, true, true, true, true, true, true, true, true, true, true, true}};
    public static final int[][] anIntArrayArray3824 = {{0, 2}, {0, 2}, {0, 0, 2}, {2, 0, 0}, {0, 2, 0}, {0, 0, 2}, {0, 5, 1, 4}, {0, 4, 4, 4}, {4, 4, 4, 0}, {6, 6, 6, 2, 2, 2}, {2, 2, 2, 6, 6, 6}, {0, 11, 6, 6, 6, 4}, {0, 2}, {0, 4, 4, 4}, {0, 4, 4, 4}};
    public static final int[][] anIntArrayArray3860 = {{2, 4}, {2, 4}, {5, 2, 4}, {4, 5, 2}, {2, 4, 5}, {5, 2, 4}, {1, 6, 2, 5}, {1, 6, 7, 1}, {6, 7, 1, 1}, {0, 8, 9, 8, 9, 4}, {8, 9, 4, 0, 8, 9}, {2, 10, 0, 10, 11, 11}, {2, 4}, {1, 6, 7, 1}, {1, 6, 7, 1}};
    public static final int[][] anIntArrayArray3815 = {{6, 6}, {6, 6}, {6, 5, 5}, {5, 6, 5}, {5, 5, 6}, {6, 5, 5}, {5, 0, 4, 1}, {7, 7, 1, 2}, {7, 1, 2, 7}, {8, 9, 4, 0, 8, 9}, {0, 8, 9, 8, 9, 4}, {11, 0, 10, 11, 4, 2}, {6, 6}, {7, 7, 1, 2}, {7, 7, 1, 2}};
    public static final boolean[][] pathSquares = {new boolean[4], new boolean[4], {false, false, true, false}, {false, false, true, false}, {false, false, true, false}, {false, false, true, false}, {true, false, true, false}, {true, false, false, true}, {true, false, false, true}, new boolean[4], new boolean[4], new boolean[4], new boolean[4]};
    public static final int[][] anIntArrayArray3828 = {{0, 1, 2, 3}, {1, 2, 3, 0}, {1, 2, -1, 0}, {2, 0, -1, 1}, {0, 1, -1, 2}, {1, 2, -1, 0}, {-1, 4, -1, 1}, {-1, 1, 3, -1}, {-1, 0, 2, -1}, {3, 5, 2, 0}, {0, 2, 5, 3}, {0, 2, 3, 5}, {0, 1, 2, 3}};
    public static final int[][] anIntArrayArray3774 = {{0, 2, 4, 6}, {6, 0, 2, 4}, {6, 0, 2}, {2, 6, 0}, {0, 2, 6}, {6, 0, 2}, {5, 6, 0, 1, 2, 4}, {7, 2, 4, 4}, {2, 4, 4, 7}, {6, 6, 4, 0, 2, 2}, {0, 2, 2, 6, 6, 4}, {0, 2, 2, 4, 6, 6}, {0, 2, 4, 6}};
    public static final int[][] anIntArrayArray3830 = {{2, 4, 6, 0}, {0, 2, 4, 6}, {0, 2, 4}, {4, 0, 2}, {2, 4, 0}, {0, 2, 4}, {6, 0, 1, 2, 4, 5}, {0, 4, 7, 6}, {4, 7, 6, 0}, {0, 8, 6, 2, 9, 4}, {2, 9, 4, 0, 8, 6}, {2, 11, 4, 6, 10, 0}, {2, 4, 6, 0}};
    public static final int[][] anIntArrayArray3831 = {{12, 12, 12, 12}, {12, 12, 12, 12}, {5, 5, 5}, {5, 5, 5}, {5, 5, 5}, {5, 5, 5}, {12, 12, 12, 12, 12, 12}, {1, 1, 1, 7}, {1, 1, 7, 1}, {8, 9, 9, 8, 8, 9}, {8, 8, 9, 8, 9, 9}, {10, 10, 11, 11, 11, 10}, {12, 12, 12, 12}};
    public static final boolean[][] pathBlendings = {new boolean[4], {false, true, true, false}, {true, false, true, false}, {true, false, true, false}, {false, false, true, false}, {false, false, true, false}, {true, false, true, false}, {true, false, false, true}, {true, false, false, true}, {true, true, false, false}, new boolean[4], {false, true, false, true}, new boolean[4]};
    public static final int[][] anIntArrayArray3833 = {{0, 1, 2, 3}, {1, -1, -1, 0}, {-1, 2, -1, 0}, {-1, 0, -1, 2}, {0, 1, -1, 2}, {1, 2, -1, 0}, {-1, 4, -1, 1}, {-1, 3, 4, -1}, {-1, 0, 2, -1}, {-1, -1, 2, 0}, {0, 2, 5, 3}, {0, -1, 6, -1}, {0, 1, 2, 3}};
    public static final int[][] anIntArrayArray3775 = {{0, 2, 4, 6}, {6, 0, 2, 3, 5, 3}, {6, 0, 2, 4}, {2, 5, 6, 1}, {0, 2, 6}, {6, 0, 2}, {5, 6, 0, 1, 2, 4}, {7, 7, 1, 2, 4, 6}, {2, 4, 4, 7}, {6, 6, 4, 0, 1, 1, 3, 3}, {0, 2, 2, 6, 6, 4}, {0, 2, 2, 3, 7, 0, 4, 3}, {0, 2, 4, 6}};
    public static final int[][] anIntArrayArray3821 = {{2, 4, 6, 0}, {0, 2, 3, 5, 6, 4}, {0, 1, 4, 5}, {4, 6, 0, 2}, {2, 4, 0}, {0, 2, 4}, {6, 0, 1, 2, 4, 5}, {0, 1, 2, 4, 6, 7}, {4, 7, 6, 0}, {0, 8, 6, 1, 9, 2, 9, 4}, {2, 9, 4, 0, 8, 6}, {2, 11, 3, 7, 10, 10, 6, 6}, {2, 4, 6, 0}};
    public static final int[][] anIntArrayArray3836 = {{12, 12, 12, 12}, {12, 12, 12, 12, 12, 5}, {5, 5, 1, 1}, {5, 1, 1, 5}, {5, 5, 5}, {5, 5, 5}, {12, 12, 12, 12, 12, 12}, {1, 12, 12, 12, 12, 12}, {1, 1, 7, 1}, {8, 9, 9, 8, 8, 3, 1, 9}, {8, 8, 9, 8, 9, 9}, {10, 10, 11, 11, 11, 7, 3, 7}, {12, 12, 12, 12}};
    public byte[][][] lighting;
    public int hdShadows;
    public boolean hdWater;
    public boolean hdLight;
    public boolean blendFloor;
    public boolean enableTextures;
    public final int regionSizeZ;
    public final boolean overlayHidden;
    public final int[][][] tileHeights;
    boolean lowDetailShadows;
    final Scene scene;
    final int regionSizeX;
    final int regionSizeY;
    final SettingsBits regionMap;
    byte[][][] cullingsMap;
    public int[] underlayHue;
    public int[] underlaySaturation;
    public int[] underlayLightness;
    public int[] underlayDivider;
    public int[] underlayCount;
    public int underlayFaceCount;
    public int overlayFaceCount;
    public int tileRotation;
    public int overlayTextureId;
    public int pathShape;
    public boolean hasShadows;
    public boolean overlaySupportsBlending;
    public boolean unblendable;
    public int overlayTotalFaces;
    public int index;
    public int[] anIntArray3857;
    public int[] anIntArray3858;
    public int[] anIntArray3859;
    public int[] overlayFaceCounts;
    public int overlayHslColor;
    public int overlayTextureScale;
    public final int[] overlayTileRotations = new int[6];
    public final int[] overlayTileColors = new int[13];
    public final int[] overlayTileRenderedColors = new int[13];
    public final int[] waterTextureIds = new int[13];
    public final int[] waterTextureScaling = new int[13];
    public final int[] waterSlots = new int[13];
    public final int[] overlayFunctionMask = new int[13];
    public final FloTypeList overlayTypeList;
    public final FluTypeList underlayTypeList;
    public final byte[][][] underlayIds;
    public final byte[][][] overlayIds;
    public final byte[][][] overlayPathShapes;
    public final byte[][][] overlayRotations;
    public boolean overlayHasMultiColor;

    MapLoader(Scene scene, int sizeZ, int sizeX, int sizeY, boolean overlayHidden, FloTypeList overlayTypeList, FluTypeList underlayTypeList, SettingsBits regionMap) {
        this.scene = scene;
        this.regionSizeZ = sizeZ;
        this.regionSizeX = sizeX;
        this.regionSizeY = sizeY;
        this.overlayHidden = overlayHidden;
        this.overlayTypeList = overlayTypeList;
        this.underlayTypeList = underlayTypeList;
        this.regionMap = regionMap;
        this.underlayIds = new byte[this.regionSizeZ][this.regionSizeX][this.regionSizeY];
        this.overlayIds = new byte[this.regionSizeZ][this.regionSizeX][this.regionSizeY];
        this.overlayPathShapes = new byte[this.regionSizeZ][this.regionSizeX][this.regionSizeY];
        this.overlayRotations = new byte[this.regionSizeZ][this.regionSizeX][this.regionSizeY];
        this.tileHeights = new int[this.regionSizeZ][this.regionSizeX + 1][this.regionSizeY + 1];
        this.cullingsMap = new byte[this.regionSizeZ][this.regionSizeX + 1][this.regionSizeY + 1];
    }

    public static long calculateLocUid(GameObject gameObject, int x, int y) {
        ObjectType loctype = Resource.mapRegion.getLoctypeList().list(gameObject.getId());
        long uid = x | (long) y << 7 | (long) gameObject.getShape() << 14 | (long) gameObject.getRotation() << 20 | 0x40000000;
        if (loctype.hasActions == 0) {
            uid |= 0x8000000000000000L;
        }
        if (loctype.supportsItems == 1) {
            uid |= 0x400000L;
        }
        uid |= (long) gameObject.getId() << 32;
        return uid;
    }

    public void clearAllPlanes(int x, int y) {
        for (int z = 0; z < this.regionSizeZ; z++) {
            clearHeights(z, x, y, 64, 64);
        }
    }

    public void resetUnderlays() {
        this.underlayHue = null;
        this.underlaySaturation = null;
        this.underlayLightness = null;
        this.underlayDivider = null;
        this.underlayCount = null;
        this.lowDetailShadows = false;
    }

    public void enableLdShadows() {
        this.lowDetailShadows = true;
    }

    public void calculateUnderlayPalette(AbstractToolkit toolkit, Ground heightsCurrentLvl, Ground heightsNextLvl) {
        int[][] paletteIndices = new int[this.regionSizeX][this.regionSizeY];
        if (this.underlayHue == null || this.underlayHue.length != this.regionSizeY) {
            this.underlayHue = new int[this.regionSizeY];
            this.underlaySaturation = new int[this.regionSizeY];
            this.underlayLightness = new int[this.regionSizeY];
            this.underlayDivider = new int[this.regionSizeY];
            this.underlayCount = new int[this.regionSizeY];
        }
        for (int z = 0; z < this.regionSizeZ; z++) {
            for (int index = 0; index < this.regionSizeY; index++) {
                this.underlayHue[index] = 0;
                this.underlaySaturation[index] = 0;
                this.underlayLightness[index] = 0;
                this.underlayDivider[index] = 0;
                this.underlayCount[index] = 0;
            }
            for (int x = -5; x < this.regionSizeX; x++) {
                for (int y = 0; y < this.regionSizeY; y++) {
                    int forwardX = x + 5;
                    if (forwardX < this.regionSizeX) {
                        int underlayId = this.underlayIds[z][forwardX][y] & 0xff;
                        if (underlayId > 0) {
                            FluType underlay = this.underlayTypeList.list(underlayId - 1);
                            this.underlayHue[y] += underlay.hue;
                            this.underlaySaturation[y] += underlay.saturation;
                            this.underlayLightness[y] += underlay.lightness;
                            this.underlayDivider[y] += underlay.divisor;
                            this.underlayCount[y]++;
                        }
                    }
                    int backwardX = x - 5;
                    if (backwardX >= 0) {
                        int underlayId = this.underlayIds[z][backwardX][y] & 0xff;
                        if (underlayId > 0) {
                            FluType underlay = this.underlayTypeList.list(underlayId - 1);
                            this.underlayHue[y] -= underlay.hue;
                            this.underlaySaturation[y] -= underlay.saturation;
                            this.underlayLightness[y] -= underlay.lightness;
                            this.underlayDivider[y] -= underlay.divisor;
                            this.underlayCount[y]--;
                        }
                    }
                }
                if (x >= 0) {
                    int hueSum = 0;
                    int saturationSum = 0;
                    int lightnessSum = 0;
                    int dividerSum = 0;
                    int depth = 0;
                    for (int y = -5; y < this.regionSizeY; y++) {
                        int forwardY = y + 5;
                        if (forwardY < this.regionSizeY) {
                            hueSum += this.underlayHue[forwardY];
                            saturationSum += this.underlaySaturation[forwardY];
                            lightnessSum += this.underlayLightness[forwardY];
                            dividerSum += this.underlayDivider[forwardY];
                            depth += this.underlayCount[forwardY];
                        }
                        int backwardY = y - 5;
                        if (backwardY >= 0) {
                            hueSum -= this.underlayHue[backwardY];
                            saturationSum -= this.underlaySaturation[backwardY];
                            lightnessSum -= this.underlayLightness[backwardY];
                            dividerSum -= this.underlayDivider[backwardY];
                            depth -= this.underlayCount[backwardY];
                        }
                        if (y >= 0 && dividerSum > 0 && depth > 0) {
                            paletteIndices[x][y] = ColorUtil.hsl16to24(hueSum * 256 / dividerSum, saturationSum / depth, lightnessSum / depth);
                        }
                    }
                }
            }
            if (this.blendFloor) {
                addBlendedTiles(toolkit, this.scene.localHeightmap[z], z, paletteIndices, z == 0 ? heightsCurrentLvl : null, z == 0 ? heightsNextLvl : null);
            } else {
                addRegularTile(this.scene.localHeightmap[z], z, paletteIndices, z == 0 ? heightsCurrentLvl : null, z == 0 ? heightsNextLvl : null);
            }
            this.underlayIds[z] = null;
            this.overlayIds[z] = null;
            this.overlayPathShapes[z] = null;
            this.overlayRotations[z] = null;
        }
        if (!this.overlayHidden) {
            if (this.hdShadows != 0) {
                this.scene.addHdShadow();
            }
            if (this.hdLight) {
                this.scene.addHdLight();
            }
        }
        for (int z = 0; z < this.regionSizeZ; z++) {
            this.scene.localHeightmap[z].resetLight();
        }
    }

    public void clearHeights(int z, int x, int y, int offsetX, int offsetY) {
        for (int nextY = y; nextY < y + offsetY; nextY++) {
            for (int nextX = x; nextX < x + offsetX; nextX++) {
                if (nextX >= 0 && nextX < this.regionSizeX && nextY >= 0 && nextY < this.regionSizeY) {
                    this.tileHeights[z][nextX][nextY] = z > 0 ? this.tileHeights[z - 1][nextX][nextY] - 960 : 0;
                }
            }
        }
        if (x > 0 && x < this.regionSizeX) {
            for (int nextY = y + 1; nextY < y + offsetY; nextY++) {
                if (nextY >= 0 && nextY < this.regionSizeY) {
                    this.tileHeights[z][x][nextY] = this.tileHeights[z][x - 1][nextY];
                }
            }
        }
        if (y > 0 && y < this.regionSizeY) {
            for (int nextY = x + 1; nextY < x + offsetX; nextY++) {
                if (nextY >= 0 && nextY < this.regionSizeX) {
                    this.tileHeights[z][nextY][y] = this.tileHeights[z][nextY][y - 1];
                }
            }
        }
        if (x >= 0 && y >= 0 && x < this.regionSizeX && y < this.regionSizeY) {
            if (z == 0) {
                if (x > 0 && this.tileHeights[0][x - 1][y] != 0) {
                    this.tileHeights[z][x][y] = this.tileHeights[z][x - 1][y];
                } else if (y > 0 && this.tileHeights[z][x][y - 1] != 0) {
                    this.tileHeights[z][x][y] = this.tileHeights[z][x][y - 1];
                } else if (x > 0 && y > 0 && this.tileHeights[z][x - 1][y - 1] != 0) {
                    this.tileHeights[z][x][y] = this.tileHeights[z][x - 1][y - 1];
                }
            } else if (x > 0 && this.tileHeights[z - 1][x - 1][y] != this.tileHeights[z][x - 1][y]) {
                this.tileHeights[z][x][y] = this.tileHeights[z][x - 1][y];
            } else if (y > 0 && this.tileHeights[z][x][y - 1] != this.tileHeights[z - 1][x][y - 1]) {
                this.tileHeights[z][x][y] = this.tileHeights[z][x][y - 1];
            } else if (x > 0 && y > 0 && this.tileHeights[z - 1][x - 1][y - 1] != this.tileHeights[z][x - 1][y - 1]) {
                this.tileHeights[z][x][y] = this.tileHeights[z][x - 1][y - 1];
            }
        }
    }

    public void decodeTilesClient(Packet stream, int chunkX, int chunkY, int baseX, int baseY, CollisionMap[] clippings) {
        if (!this.overlayHidden) {
            for (int level = 0; level < 4; level++) {
                CollisionMap clipping = clippings[level];
                for (int regionPositionX = 0; regionPositionX < 64; regionPositionX++) {
                    for (int regionPositionY = 0; regionPositionY < 64; regionPositionY++) {
                    	int sceneX = regionPositionX + chunkX;
                        int sceneY = regionPositionY + chunkY;
                        if (sceneX >= 0 && sceneX < this.regionSizeX && sceneY >= 0 && sceneY < this.regionSizeY) {
                            clipping.unblock(sceneX, sceneY);
                        }
                    }
                }
            }
        }
        int absoluteX = chunkX + baseX;
        int absoluteY = chunkY + baseY;
        for (int level = 0; level < this.regionSizeZ; level++) {
            for (int regionPositionX = 0; regionPositionX < 64; regionPositionX++) {
                for (int regionPositionY = 0; regionPositionY < 64; regionPositionY++) {
                    decodeTile(stream, level, chunkX + regionPositionX, chunkY + regionPositionY, 0, 0, absoluteX + regionPositionX, absoluteY + regionPositionY, 0, false);
                }
            }
        }
    }

    public void decodeTilesServer(Packet rsbytebuffer1, int i_2, int i_3, int i_4, int i_5, int i_6, int i_7, int i_8, CollisionMap[] arr_9) {
        int i_11 = (i_6 & 0x7) * 8;
        int i_12 = (i_7 & 0x7) * 8;
        int i_14;
        int i_17;
        if (!this.overlayHidden) {
            CollisionMap clipmap13 = arr_9[i_2];
            for (i_14 = 0; i_14 < 8; i_14++) {
                for (int i_15 = 0; i_15 < 8; i_15++) {
                    int i_16 = i_3 + EnvironmentManager.rotateTerrainBlockX(i_14 & 0x7, i_15 & 0x7, i_8);
                    i_17 = i_4 + EnvironmentManager.rotateTerrainBlockY(i_14 & 0x7, i_15 & 0x7, i_8);
                    if (i_16 > 0 && i_16 < this.regionSizeX - 1 && i_17 > 0 && i_17 < this.regionSizeY - 1) {
                        clipmap13.unblock(i_16, i_17);
                    }
                }
            }
        }
        int i_28 = (i_6 & -8) << 3;
        i_14 = (i_7 & -8) << 3;
        byte b_29 = 0;
        byte b_30 = 0;
        if (i_8 == 1) {
            b_30 = 1;
        } else if (i_8 == 2) {
            b_29 = 1;
            b_30 = 1;
        } else if (i_8 == 3) {
            b_29 = 1;
        }
        for (i_17 = 0; i_17 < this.regionSizeZ; i_17++) {
            for (int i_18 = 0; i_18 < 64; i_18++) {
                for (int i_19 = 0; i_19 < 64; i_19++) {
                    if (i_5 == i_17 && i_18 >= i_11 && i_18 <= i_11 + 8 && i_19 >= i_12 && i_19 <= i_12 + 8) {
                        int i_20;
                        int i_21;
                        if (i_18 != i_11 + 8 && i_12 + 8 != i_19) {
                            i_20 = i_3 + EnvironmentManager.rotateTerrainBlockX(i_18 & 0x7, i_19 & 0x7, i_8);
                            i_21 = i_4 + EnvironmentManager.rotateTerrainBlockY(i_18 & 0x7, i_19 & 0x7, i_8);
                            decodeTile(rsbytebuffer1, i_2, i_20, i_21, b_29, b_30, i_28 + i_18, i_14 + i_19, i_8, false);
                        } else {
                            if (i_8 == 0) {
                                i_20 = i_3 + (i_18 - i_11);
                                i_21 = i_19 - i_12 + i_4;
                            } else if (i_8 == 1) {
                                i_20 = i_3 + (i_19 - i_12);
                                i_21 = i_4 + 8 - (i_18 - i_11);
                            } else if (i_8 == 2) {
                                i_20 = i_3 + 8 - (i_18 - i_11);
                                i_21 = i_4 + 8 - (i_19 - i_12);
                            } else {
                                i_20 = i_3 + 8 - (i_19 - i_12);
                                i_21 = i_18 - i_11 + i_4;
                            }
                            decodeTile(rsbytebuffer1, i_2, i_20, i_21, 0, 0, i_28 + i_18, i_14 + i_19, 0, true);
                        }
                        if (i_18 == 63 || i_19 == 63) {
                            byte b_22 = 1;
                            if (i_18 == 63 && i_19 == 63) {
                                b_22 = 3;
                            }
                            for (int i_23 = 0; i_23 < b_22; i_23++) {
                                int i_24 = i_18;
                                int i_25 = i_19;
                                if (i_23 == 0) {
                                    i_24 = i_18 == 63 ? 64 : i_18;
                                    i_25 = i_19 == 63 ? 64 : i_19;
                                } else if (i_23 == 1) {
                                    i_24 = 64;
                                } else {
                                    i_25 = 64;
                                }
                                int i_26;
                                int i_27;
                                if (i_8 == 0) {
                                    i_26 = i_3 + (i_24 - i_11);
                                    i_27 = i_25 - i_12 + i_4;
                                } else if (i_8 == 1) {
                                    i_26 = i_3 + (i_25 - i_12);
                                    i_27 = i_4 + 8 - (i_24 - i_11);
                                } else if (i_8 == 2) {
                                    i_26 = i_3 + 8 - (i_24 - i_11);
                                    i_27 = i_4 + 8 - (i_25 - i_12);
                                } else {
                                    i_26 = i_3 + 8 - (i_25 - i_12);
                                    i_27 = i_24 - i_11 + i_4;
                                }
                                if (i_26 >= 0 && i_26 < this.regionSizeX && i_27 >= 0 && i_27 < this.regionSizeY) {
                                    this.tileHeights[i_2][i_26][i_27] = this.tileHeights[i_2][i_20 + b_29][b_30 + i_21];
                                }
                            }
                        }
                    } else {
                        decodeTile(rsbytebuffer1, 0, -1, -1, 0, 0, 0, 0, 0, false);
                    }
                }
            }
        }
    }

    public void initializeFloor(AbstractToolkit toolkit, int[][][] heightsNextLvl, CollisionMap[] clipping) {
        if (!this.overlayHidden) {
            for (int plane = 0; plane < 4; plane++) {
                for (int x = 0; x < this.regionSizeX; x++) {
                    for (int y = 0; y < this.regionSizeY; y++) {
                        if ((this.regionMap.tileFunctionMasks[plane][x][y] & 0x1) != 0) {
                            int height = plane;
                            if ((this.regionMap.tileFunctionMasks[1][x][y] & 0x2) != 0) {
                                height = plane - 1;
                            }
                            if (height >= 0) {
                                clipping[height].setUnwalkable(x, y);
                            }
                        }
                    }
                }
            }
        }
        for (int height = 0; height < this.regionSizeZ; height++) {
            int floorFunctionMask = 0;
            int waterFunctionMask = 0;
            if (!this.overlayHidden) {
                if (this.hdWater) {
                    waterFunctionMask |= HD_WATER;
                }
                if (this.hdLight) {
                    floorFunctionMask |= HD_LIGHT;
                }
                if (this.hdShadows != 0) {
                    floorFunctionMask |= COMPLETELY_FLAT;
                    waterFunctionMask |= HD_SHADING;
                }
            }
            if (this.hdLight) {
                waterFunctionMask |= HAS_WATER_LIGHTING;
            }
            if (!this.enableTextures) {
                waterFunctionMask |= DISABLE_TEXTURES;
            }
            int[][] nextHeights = heightsNextLvl != null && height < heightsNextLvl.length ? heightsNextLvl[height] : this.tileHeights[height];
            this.scene.setLocalGround(height, toolkit.createGround(this.regionSizeX, this.regionSizeY, this.tileHeights[height], nextHeights, floorFunctionMask, waterFunctionMask));
        }
    }

    public void addRegularTile(Ground localHeights, int plane, int[][] floorColors, Ground currentHeights, Ground nextHeights) {
        for (int x = 0; x < this.regionSizeX; x++) {
            for (int y = 0; y < this.regionSizeY; y++) {
                byte overlayShape = this.overlayPathShapes[plane][x][y];
                byte overlayRotation = this.overlayRotations[plane][x][y];
                int overlayId = this.overlayIds[plane][x][y] & 0xff;
                int underlayId = this.underlayIds[plane][x][y] & 0xff;
                FloType overlay = overlayId != 0 ? this.overlayTypeList.list(overlayId - 1) : null;
                FluType underlay = underlayId != 0 ? this.underlayTypeList.list(underlayId - 1) : null;
                if (overlayShape == 0 && overlay == null) {
                    overlayShape = 12;
                }
                FloType Overlay = overlay;
                if (overlay != null && overlay.tileColorHsl == -1 && overlay.minimapColorHsl == -1) {
                    overlay = null;
                }
                if (overlay != null || underlay != null) {
                    this.underlayFaceCount = UNDERLAY_FACE_COUNT[overlayShape];
                    this.overlayFaceCount = OVERLAY_FACE_COUNT[overlayShape];
                    int faceCount = (underlay != null ? this.underlayFaceCount : 0) + (overlay != null ? this.overlayFaceCount : 0);
                    int tileCount = 0;
                    this.tileRotation = 0;
                    this.overlayTextureId = overlay != null ? overlay.texture : -1;
                    int underlayTetureId = underlay != null ? underlay.texture : -1;
                    int[] ints_20 = new int[faceCount];
                    int[] ints_21 = new int[faceCount];
                    int[] ints_22 = new int[faceCount];
                    int[] floorColor = new int[faceCount];
                    int[] overlayTextures = new int[faceCount];
                    int[] textureScale = new int[faceCount];
                    int[] minimapColor = overlay != null && overlay.minimapColorHsl != -1 ? new int[faceCount] : null;
                    if (overlay != null) {
                        for (int index = 0; index < this.overlayFaceCount; index++) {
                            ints_20[tileCount] = anIntArrayArray3824[overlayShape][this.tileRotation];
                            ints_21[tileCount] = anIntArrayArray3860[overlayShape][this.tileRotation];
                            ints_22[tileCount] = anIntArrayArray3815[overlayShape][this.tileRotation];
                            overlayTextures[tileCount] = this.overlayTextureId;
                            textureScale[tileCount] = overlay.textureScale;
                            floorColor[tileCount] = overlay.tileColorHsl;
                            if (minimapColor != null) {
                                minimapColor[tileCount] = overlay.minimapColorHsl;
                            }
                            tileCount++;
                            this.tileRotation++;
                        }
                        if (!this.overlayHidden && plane == 0) {
                            this.scene.addWater(x, y, overlay.waterColor, overlay.waterFogDepth, overlay.waterIntensity, overlay.opcode20, overlay.unusedOpcode21, overlay.unusedOpcode22);
                        }
                    } else {
                        this.tileRotation += this.overlayFaceCount;
                    }
                    if (underlay != null) {
                        for (int index = 0; index < this.underlayFaceCount; index++) {
                            ints_20[tileCount] = anIntArrayArray3824[overlayShape][this.tileRotation];
                            ints_21[tileCount] = anIntArrayArray3860[overlayShape][this.tileRotation];
                            ints_22[tileCount] = anIntArrayArray3815[overlayShape][this.tileRotation];
                            overlayTextures[tileCount] = underlayTetureId;
                            textureScale[tileCount] = underlay.scale;
                            floorColor[tileCount] = floorColors[x][y];
                            if (minimapColor != null) {
                                minimapColor[tileCount] = floorColor[tileCount];
                            }
                            tileCount++;
                            this.tileRotation++;
                        }
                    }
                    int rotationCount = tileSizeDeltasX.length;
                    int[] sizesX = new int[rotationCount];
                    int[] sizesY = new int[rotationCount];
                    int[] nextLvlHeightDifference = nextHeights != null ? new int[rotationCount] : null;
                    int[] localLvlHeightDifference = nextHeights == null && currentHeights == null ? null : new int[rotationCount];
                    for (int rotation = 0; rotation < rotationCount; rotation++) {
                    	int sizeDeltaX = tileSizeDeltasX[rotation];
                    	int sizeDeltaY = tileSizeDeltasY[rotation];
                        if (overlayRotation == 0) {
                            sizesX[rotation] = sizeDeltaX;
                            sizesY[rotation] = sizeDeltaY;
                        } else if (overlayRotation == 1) {
                            sizesX[rotation] = sizeDeltaY;
                            sizesY[rotation] = 512 - sizeDeltaX;
                        } else if (overlayRotation == 2) {
                            sizesX[rotation] = 512 - sizeDeltaX;
                            sizesY[rotation] = 512 - sizeDeltaY;
                        } else if (overlayRotation == 3) {
                            sizesX[rotation] = 512 - sizeDeltaY;
                            sizesY[rotation] = sizeDeltaX;
                        }
                        if (nextLvlHeightDifference != null && OVERLAY_SHAPE_SUPPORTS_HEIGHT[overlayShape][rotation]) {
                        	int sceneX = (x << 9) + sizesX[rotation];
                        	int sceneY = (y << 9) + sizesY[rotation];
                            nextLvlHeightDifference[rotation] = nextHeights.getAverageHeight(sceneX, sceneY) - localHeights.getAverageHeight(sceneX, sceneY);
                        }
                        if (localLvlHeightDifference != null) {
                            if (nextHeights != null && !OVERLAY_SHAPE_SUPPORTS_HEIGHT[overlayShape][rotation]) {
                            	int sceneX = (x << 9) + sizesX[rotation];
                            	int sceneY = (y << 9) + sizesY[rotation];
                                localLvlHeightDifference[rotation] = localHeights.getAverageHeight(sceneX, sceneY) - nextHeights.getAverageHeight(sceneX, sceneY);
                            } else if (currentHeights != null && !aBoolArrayArray3823[overlayShape][rotation]) {
                            	int sceneX = (x << 9) + sizesX[rotation];
                            	int sceneY = (y << 9) + sizesY[rotation];
                                localLvlHeightDifference[rotation] = currentHeights.getAverageHeight(sceneX, sceneY) - localHeights.getAverageHeight(sceneX, sceneY);
                            }
                        }
                    }
                    int heightSw = localHeights.getHeight(x, y);
                    int heightSe = localHeights.getHeight(x + 1, y);
                    int heightNe = localHeights.getHeight(x + 1, y + 1);
                    int heightNw = localHeights.getHeight(x, y + 1);
                    boolean hideRoofs = this.regionMap.areRoofsHidden(x, y);
                    if (hideRoofs ? plane > 1 : plane > 0) {
                        boolean hideOverlay = true;
                        if (underlay != null && !underlay.occlude) {
                            hideOverlay = false;
                        } else if (underlayId == 0 && overlayShape != 0) {
                            hideOverlay = false;
                        } else if (overlayId > 0 && Overlay != null && !Overlay.occlude) {
                            hideOverlay = false;
                        }
                        if (hideOverlay && heightSw == heightSe && heightSw == heightNe && heightSw == heightNw) {
                            this.cullingsMap[plane][x][y] |= 0x4;
                        }
                    }
                    WaterTile tile = new WaterTile();
                    if (this.overlayHidden) {
                        tile.color = this.scene.getColor(x, y);
                        tile.scale = this.scene.getScale(x, y);
                        tile.intensity = this.scene.getIntensity(x, y);
                        tile.floOpcode20 = this.scene.getWaterOpcode20(x, y);
                        tile.floUnusedOpcode21 = this.scene.unusedOpcode21(x, y);
                        tile.floUnusedOpcode22 = this.scene.unusedOpcode22(x, y);
                    }
                    localHeights.calculateBlockTiles(x, y, sizesX, nextLvlHeightDifference, sizesY, localLvlHeightDifference, ints_20, ints_21, ints_22, floorColor, minimapColor, overlayTextures, textureScale, tile);
                    this.scene.loadTiles(plane, x, y);
                }
            }
        }
    }
    
    /**
     * Used every display mode above min
     */
    public void addBlendedTiles(AbstractToolkit toolkit, Ground ground, int z, int[][] paletteIndices, Ground heightsCurrentLvl, Ground heightsNextLvl) {
        byte[][] localPathShape = this.overlayPathShapes[z];
        byte[][] localOverlayRotation = this.overlayRotations[z];
        byte[][] localUnderlays = this.underlayIds[z];
        byte[][] localOverlays = this.overlayIds[z];
        boolean[] hasFacesOn = new boolean[4];
        for (int x = 0; x < this.regionSizeX; x++) {
            int nextX = x < this.regionSizeX - 1 ? x + 1 : x;
            for (int y = 0; y < this.regionSizeY; y++) {
                int nextY = y < this.regionSizeY - 1 ? y + 1 : y;
                this.pathShape = localPathShape[x][y];
                this.tileRotation = localOverlayRotation[x][y];
                int overlayId = localOverlays[x][y] & 0xff;
                int underlayId = localUnderlays[x][y] & 0xff;
                if (overlayId != 0 || underlayId != 0) {
                    FloType initialOverlay = overlayId != 0 ? this.overlayTypeList.list(overlayId - 1) : null;
                    FluType initialUnderlay = underlayId != 0 ? this.underlayTypeList.list(underlayId - 1) : null;
                    if (this.pathShape == 0 && initialOverlay == null) {
                        this.pathShape = 12;
                    }
                    this.hasShadows = false;
                    this.overlaySupportsBlending = false;
                    hasFacesOn[3] = false;
                    hasFacesOn[2] = false;
                    hasFacesOn[1] = false;
                    hasFacesOn[0] = false;
                    FloType Overlay = initialOverlay;
                    if (initialOverlay != null) {
                        if (initialOverlay.tileColorHsl == -1 && initialOverlay.minimapColorHsl == -1) {
                            initialOverlay = null;
                        } else if (initialUnderlay != null && this.pathShape != 0) {
                            this.overlaySupportsBlending = initialOverlay.blendsWithUnderlay;
                        }
                    }
                    this.tileRotation = calculateUnderlayRotation(underlayId, x, y, nextX, nextY, ground, localUnderlays);
                    for (int slot = 0; slot < 13; slot++) {
                        this.waterSlots[slot] = -1;
                        this.overlayFunctionMask[slot] = 1;
                    }
                    calculateOverlayRotation(toolkit, initialOverlay, x, y, localPathShape, localOverlayRotation, localOverlays, hasFacesOn);
                    this.unblendable = !this.overlaySupportsBlending && !hasFacesOn[0] && !hasFacesOn[2] && !hasFacesOn[1] && !hasFacesOn[3];
                    calculateFaceCounts(initialOverlay, initialUnderlay);
                    int floorFaceCount = this.underlayFaceCount + this.overlayFaceCount;
                    if (floorFaceCount <= 0) {
                        this.scene.loadTiles(z, x, y);
                    } else {
                        if (hasFacesOn[0]) {
                            floorFaceCount++;
                        }
                        if (hasFacesOn[2]) {
                            floorFaceCount++;
                        }
                        if (hasFacesOn[1]) {
                            floorFaceCount++;
                        }
                        if (hasFacesOn[3]) {
                            floorFaceCount++;
                        }
                        this.overlayTotalFaces = 0;
                        this.index = 0;
                        int floorVerticeCount = floorFaceCount * 3;
                        int[] blendingColors = this.overlayHasMultiColor ? new int[floorVerticeCount] : null;
                        int[] offsetsX = new int[floorVerticeCount];
                        int[] offsetsZ = new int[floorVerticeCount];
                        int[] mainColors = new int[floorVerticeCount];
                        int[] waterMaterials = new int[floorVerticeCount];
                        int[] waterScaling = new int[floorVerticeCount];
                        int[] drawHeightsA = heightsNextLvl != null ? new int[floorVerticeCount] : null;
                        int[] drawHeightsB = heightsNextLvl == null && heightsCurrentLvl == null ? null : new int[floorVerticeCount];
                        addOverlayTiles(toolkit, z, x, y, initialOverlay, hasFacesOn, blendingColors, offsetsX, offsetsZ, mainColors, waterMaterials, waterScaling, drawHeightsA, drawHeightsB, ground, heightsNextLvl, heightsCurrentLvl);
                        int underlaySouthWest = localUnderlays[x][nextY] & 0xff;
                        int underlaySouthEast = localUnderlays[nextX][nextY] & 0xff;
                        int underlayNorthEast = localUnderlays[nextX][y] & 0xff;
                        addUnderlayTiles(x, y, nextX, nextY, initialUnderlay, underlayId, underlaySouthWest, underlaySouthEast, underlayNorthEast, hasFacesOn, blendingColors, offsetsX, offsetsZ, mainColors, waterMaterials, waterScaling, drawHeightsA, drawHeightsB, paletteIndices, ground, heightsNextLvl, heightsCurrentLvl);
                        setupCulling(ground, initialUnderlay, Overlay, z, x, y, nextX, nextY, underlayId, overlayId);
                        WaterTile waterTile = new WaterTile();
                        if (this.overlayHidden) {
                            waterTile.color = this.scene.getColor(x, y);
                            waterTile.scale = this.scene.getScale(x, y);
                            waterTile.intensity = this.scene.getIntensity(x, y);
                            waterTile.floOpcode20 = this.scene.getWaterOpcode20(x, y);
                            waterTile.floUnusedOpcode21 = this.scene.unusedOpcode21(x, y);
                            waterTile.floUnusedOpcode22 = this.scene.unusedOpcode22(x, y);
                        }
                        ground.addBlendedTile(x, y, offsetsX, drawHeightsA, offsetsZ, drawHeightsB, mainColors, blendingColors, waterMaterials, waterScaling, waterTile, this.hasShadows);
                        this.scene.loadTiles(z, x, y);
                    }
                }
            }
        }
    }

    public void calculateOverlayDisplay(AbstractToolkit toolkit, FloType overlaytype, int x, int y, int rSizeX, int rSizeY, byte[][] lOverlay, byte[][] lOverlayShape, byte[][] lOverlayRotation, boolean[] hasFacesOn) {
        boolean[] blendingAttributes = overlaytype != null && overlaytype.blendsWithUnderlay ? pathBlendings[this.pathShape] : pathSquares[this.pathShape];
        if (y > 0) {
            if (x > 0) {
            	int overlayId = lOverlay[x - 1][y - 1] & 0xff;
                if (overlayId > 0) {
                    FloType overlay = this.overlayTypeList.list(overlayId - 1);
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        byte shape = lOverlayShape[x - 1][y - 1];
                        int rotation = lOverlayRotation[x - 1][y - 1] * 2 + 4 & 0x7;
                        int renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][rotation]) {
                            this.overlayTileColors[0] = overlay.tileColorHsl;
                            this.overlayTileRenderedColors[0] = renderColor;
                            this.waterTextureIds[0] = overlay.texture;
                            this.waterTextureScaling[0] = overlay.textureScale;
                            this.waterSlots[0] = overlay.slot;
                            this.overlayFunctionMask[0] = 256;
                        }
                    }
                }
            }
            if (x < rSizeX - 1) {
            	int overlayId = lOverlay[x + 1][y - 1] & 0xff;
                if (overlayId > 0) {
                    FloType overlay = this.overlayTypeList.list(overlayId - 1);
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        byte shape = lOverlayShape[x + 1][y - 1];
                        int rotation = lOverlayRotation[x + 1][y - 1] * 2 + 6 & 0x7;
                        int renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][rotation]) {
                            this.overlayTileColors[2] = overlay.tileColorHsl;
                            this.overlayTileRenderedColors[2] = renderColor;
                            this.waterTextureIds[2] = overlay.texture;
                            this.waterTextureScaling[2] = overlay.textureScale;
                            this.waterSlots[2] = overlay.slot;
                            this.overlayFunctionMask[2] = 512;
                        }
                    }
                }
            }
        }
        if (y < rSizeY - 1) {
            if (x > 0) {
            	int underlayId = lOverlay[x - 1][y + 1] & 0xff;
                if (underlayId > 0) {
                    FloType overlay = this.overlayTypeList.list(underlayId - 1);
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        byte shape = lOverlayShape[x - 1][y + 1];
                        int rotation = lOverlayRotation[x - 1][y + 1] * 2 + 2 & 0x7;
                        int renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][rotation]) {
                            this.overlayTileColors[6] = overlay.tileColorHsl;
                            this.overlayTileRenderedColors[6] = renderColor;
                            this.waterTextureIds[6] = overlay.texture;
                            this.waterTextureScaling[6] = overlay.textureScale;
                            this.waterSlots[6] = overlay.slot;
                            this.overlayFunctionMask[6] = 64;
                        }
                    }
                }
            }
            if (x < rSizeX - 1) {
            	int overlayId = lOverlay[x + 1][y + 1] & 0xff;
                if (overlayId > 0) {
                    FloType overlay = this.overlayTypeList.list(overlayId - 1);
                    if (overlay.tileColorHsl != -1 && overlay.blendsWithUnderlay) {
                        byte shape = lOverlayShape[x + 1][y + 1];
                        int rotation = lOverlayRotation[x + 1][y + 1] * 2 & 0x7;
                        int renderColor = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][rotation]) {
                            this.overlayTileColors[4] = overlay.tileColorHsl;
                            this.overlayTileRenderedColors[4] = renderColor;
                            this.waterTextureIds[4] = overlay.texture;
                            this.waterTextureScaling[4] = overlay.textureScale;
                            this.waterSlots[4] = overlay.slot;
                            this.overlayFunctionMask[4] = 128;
                        }
                    }
                }
            }
        }

        if (y > 0) {
        	int overlayId = lOverlay[x][y - 1] & 0xff;
            if (overlayId > 0) {
                FloType overlay = this.overlayTypeList.list(overlayId - 1);
                if (overlay.tileColorHsl != -1) {
                    byte shape = lOverlayShape[x][y - 1];
                    byte rotation = lOverlayRotation[x][y - 1];
                    if (overlay.blendsWithUnderlay) {
                    	int signedRotationA = 2;
                        int signedRotationB = rotation * 2 + 4;
                        int i_20 = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        for (int i_21 = 0; i_21 < 3; i_21++) {
                            signedRotationB &= 0x7;
                            signedRotationA &= 0x7;
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][signedRotationB] && this.waterSlots[signedRotationA] <= overlay.slot) {
                                this.overlayTileColors[signedRotationA] = overlay.tileColorHsl;
                                this.overlayTileRenderedColors[signedRotationA] = i_20;
                                this.waterTextureIds[signedRotationA] = overlay.texture;
                                this.waterTextureScaling[signedRotationA] = overlay.textureScale;
                                if (overlay.slot == this.waterSlots[signedRotationA]) {
                                    this.overlayFunctionMask[signedRotationA] |= 0x20;
                                } else {
                                    this.overlayFunctionMask[signedRotationA] = 32;
                                }
                                this.waterSlots[signedRotationA] = overlay.slot;
                            }
                            signedRotationB++;
                            signedRotationA--;
                        }
                        if (!blendingAttributes[this.tileRotation & 0x3]) {
                            hasFacesOn[0] = pathBlendings[shape][rotation + 2 & 0x3];
                        }
                    } else if (!blendingAttributes[this.tileRotation & 0x3]) {
                        hasFacesOn[0] = pathSquares[shape][rotation + 2 & 0x3];
                    }
                }
            }
        }
        if (y < rSizeY - 1) {
        	int overlayId = lOverlay[x][y + 1] & 0xff;
            if (overlayId > 0) {
                FloType overlay = this.overlayTypeList.list(overlayId - 1);
                if (overlay.tileColorHsl != -1) {
                    byte shape = lOverlayShape[x][y + 1];
                    byte rotation = lOverlayRotation[x][y + 1];
                    if (overlay.blendsWithUnderlay) {
                    	int signedRotationA = 4;
                        int signedRotationB = rotation * 2 + 2;
                        int i_20 = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        for (int i_21 = 0; i_21 < 3; i_21++) {
                            signedRotationB &= 0x7;
                            signedRotationA &= 0x7;
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][signedRotationB] && this.waterSlots[signedRotationA] <= overlay.slot) {
                                this.overlayTileColors[signedRotationA] = overlay.tileColorHsl;
                                this.overlayTileRenderedColors[signedRotationA] = i_20;
                                this.waterTextureIds[signedRotationA] = overlay.texture;
                                this.waterTextureScaling[signedRotationA] = overlay.textureScale;
                                if (overlay.slot == this.waterSlots[signedRotationA]) {
                                    this.overlayFunctionMask[signedRotationA] |= 0x10;
                                } else {
                                    this.overlayFunctionMask[signedRotationA] = 16;
                                }
                                this.waterSlots[signedRotationA] = overlay.slot;
                            }
                            signedRotationB--;
                            signedRotationA++;
                        }
                        if (!blendingAttributes[this.tileRotation + 2 & 0x3]) {
                            hasFacesOn[2] = pathBlendings[shape][rotation & 0x3];
                        }
                    } else if (!blendingAttributes[this.tileRotation + 2 & 0x3]) {
                        hasFacesOn[2] = pathSquares[shape][rotation & 0x3];
                    }
                }
            }
        }
        if (x > 0) {
        	int overlayId = lOverlay[x - 1][y] & 0xff;
            if (overlayId > 0) {
                FloType overlay = this.overlayTypeList.list(overlayId - 1);
                if (overlay.tileColorHsl != -1) {
                    byte shape = lOverlayShape[x - 1][y];
                    byte rotation = lOverlayRotation[x - 1][y];
                    if (overlay.blendsWithUnderlay) {
                    	int signedRotationA = 6;
                        int signedRotationB = rotation * 2 + 4;
                        int i_20 = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        for (int i_21 = 0; i_21 < 3; i_21++) {
                            signedRotationB &= 0x7;
                            signedRotationA &= 0x7;
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][signedRotationB] && this.waterSlots[signedRotationA] <= overlay.slot) {
                                this.overlayTileColors[signedRotationA] = overlay.tileColorHsl;
                                this.overlayTileRenderedColors[signedRotationA] = i_20;
                                this.waterTextureIds[signedRotationA] = overlay.texture;
                                this.waterTextureScaling[signedRotationA] = overlay.textureScale;
                                if (overlay.slot == this.waterSlots[signedRotationA]) {
                                    this.overlayFunctionMask[signedRotationA] |= 0x8;
                                } else {
                                    this.overlayFunctionMask[signedRotationA] = 8;
                                }
                                this.waterSlots[signedRotationA] = overlay.slot;
                            }
                            signedRotationB--;
                            signedRotationA++;
                        }
                        if (!blendingAttributes[this.tileRotation + 3 & 0x3]) {
                            hasFacesOn[3] = pathBlendings[shape][rotation + 1 & 0x3];
                        }
                    } else if (!blendingAttributes[this.tileRotation + 3 & 0x3]) {
                        hasFacesOn[3] = pathSquares[shape][rotation + 1 & 0x3];
                    }
                }
            }
        }
        if (x < rSizeX - 1) {
        	int underlayId = lOverlay[x + 1][y] & 0xff;
            if (underlayId > 0) {
                FloType overlay = this.overlayTypeList.list(underlayId - 1);
                if (overlay.tileColorHsl != -1) {
                    byte shape = lOverlayShape[x + 1][y];
                    byte rotation = lOverlayRotation[x + 1][y];
                    if (overlay.blendsWithUnderlay) {
                    	int signedRotationB = 4;
                        int signedRotationA = rotation * 2 + 6;
                        int renderColorHsl = ColorUtil.getOverlayColorHsl(toolkit, overlay);
                        for (int vertice = 0; vertice < 3; vertice++) {
                            signedRotationA &= 0x7;
                            signedRotationB &= 0x7;
                            if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[shape][signedRotationA] && this.waterSlots[signedRotationB] <= overlay.slot) {
                                this.overlayTileColors[signedRotationB] = overlay.tileColorHsl;
                                this.overlayTileRenderedColors[signedRotationB] = renderColorHsl;
                                this.waterTextureIds[signedRotationB] = overlay.texture;
                                this.waterTextureScaling[signedRotationB] = overlay.textureScale;
                                if (overlay.slot == this.waterSlots[signedRotationB]) {
                                    this.overlayFunctionMask[signedRotationB] |= 0x4;
                                } else {
                                    this.overlayFunctionMask[signedRotationB] = 4;
                                }
                                this.waterSlots[signedRotationB] = overlay.slot;
                            }
                            ++signedRotationA;
                            --signedRotationB;
                        }
                        if (!blendingAttributes[this.tileRotation + 1 & 0x3]) {
                            hasFacesOn[1] = pathBlendings[shape][rotation + 3 & 0x3];
                        }
                    } else if (!blendingAttributes[this.tileRotation + 1 & 0x3]) {
                        hasFacesOn[1] = pathSquares[shape][rotation + 3 & 0x3];
                    }
                }
            }
        }
        if (overlaytype != null && overlaytype.blendsWithUnderlay) {
        	int colorHsl = ColorUtil.getOverlayColorHsl(toolkit, overlaytype);
            for (int index = 0; index < 8; index++) {
                int signedRotation = index - this.tileRotation * 2 & 0x7;
                if (OVERLAY_SHAPE_SUPPORTS_HEIGHT[this.pathShape][index] && this.waterSlots[signedRotation] <= overlaytype.slot) {
                    this.overlayTileColors[signedRotation] = overlaytype.tileColorHsl;
                    this.overlayTileRenderedColors[signedRotation] = colorHsl;
                    this.waterTextureIds[signedRotation] = overlaytype.texture;
                    this.waterTextureScaling[signedRotation] = overlaytype.textureScale;
                    if (overlaytype.slot == this.waterSlots[signedRotation]) {
                        this.overlayFunctionMask[signedRotation] |= 0x2;
                    } else {
                        this.overlayFunctionMask[signedRotation] = 2;
                    }
                    this.waterSlots[signedRotation] = overlaytype.slot;
                }
            }
        }
    }

    public void calculateFaceCounts(FloType overlay, FluType underlay) {
        if (this.unblendable) {
            this.anIntArray3857 = anIntArrayArray3824[this.pathShape];
            this.anIntArray3858 = anIntArrayArray3860[this.pathShape];
            this.anIntArray3859 = anIntArrayArray3815[this.pathShape];
            this.overlayFaceCount = overlay != null ? OVERLAY_FACE_COUNT[this.pathShape] : 0;
            this.underlayFaceCount = underlay != null ? UNDERLAY_FACE_COUNT[this.pathShape] : 0;
        } else if (this.overlaySupportsBlending) {
            this.anIntArray3857 = anIntArrayArray3775[this.pathShape];
            this.anIntArray3858 = anIntArrayArray3821[this.pathShape];
            this.anIntArray3859 = anIntArrayArray3836[this.pathShape];
            this.overlayFaceCount = overlay != null ? OVERLAY_FACE_COUNT_BLENDED[this.pathShape] : 0;
            this.underlayFaceCount = underlay != null ? UNDERLAY_FACE_COUNT_BLENDED[this.pathShape] : 0;
            this.overlayFaceCounts = anIntArrayArray3833[this.pathShape];
        } else {
            this.anIntArray3857 = anIntArrayArray3774[this.pathShape];
            this.anIntArray3858 = anIntArrayArray3830[this.pathShape];
            this.anIntArray3859 = anIntArrayArray3831[this.pathShape];
            this.overlayFaceCount = overlay != null ? anIntArray3826[this.pathShape] : 0;
            this.underlayFaceCount = underlay != null ? anIntArray3847[this.pathShape] : 0;
            this.overlayFaceCounts = anIntArrayArray3828[this.pathShape];
        }
    }

    //add_overlay_tiles(toolkit, z, x, y, initial_overlay, has_faces_on, blending_colors, offsets_x, offsets_z, main_colors, water_materials, water_scaling, draw_heights_a, draw_heights_b, ground, heights_next_lvl, heights_current_lvl);
    public void addOverlayTiles(AbstractToolkit toolkit, int i_2, int x, int y, FloType overlay, boolean[] bools_6, int[] ints_7, int[] sizesX, int[] sizesY, int[] ints_10, int[] waterTextures, int[] waterTexturesScale, int[] groundDrawHeights, int[] ints_14, Ground groundA, Ground groundB, Ground groundC) {
        this.overlayHslColor = -1;
        this.overlayTextureId = -1;
        this.overlayTextureScale = 256;
        if (overlay != null) {
            this.overlayHslColor = overlay.tileColorHsl;
            this.overlayTextureId = overlay.texture;
            this.overlayTextureScale = overlay.textureScale;
            int overlayBaseColor = ColorUtil.getOverlayColorHsl(toolkit, overlay);
            for (int overlayFace = 0; overlayFace < this.overlayFaceCount; overlayFace++) {
                byte b_22;
                if (bools_6[-this.tileRotation & 0x3] && this.overlayTotalFaces == this.overlayFaceCounts[0]) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 1;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 1;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_22 = 6;
                } else if (bools_6[2 - this.tileRotation & 0x3] && this.overlayFaceCounts[2] == this.overlayTotalFaces) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 5;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 5;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_22 = 6;
                } else if (bools_6[1 - this.tileRotation & 0x3] && this.overlayFaceCounts[1] == this.overlayTotalFaces) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 3;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 3;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_22 = 6;
                } else if (bools_6[3 - this.tileRotation & 0x3] && this.overlayFaceCounts[3] == this.overlayTotalFaces) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 7;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 7;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_22 = 6;
                } else {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    b_22 = 3;
                }
                for (int i_23 = 0; i_23 < b_22; i_23++) {
                    int rotation = this.overlayTileRotations[i_23];
                    int i_25 = rotation - this.tileRotation * 2 & 0x7;
                    int i_26 = tileSizeDeltasX[rotation];
                    int i_27 = tileSizeDeltasY[rotation];
                    int sizeX;
                    int sizeY;
                    if (this.tileRotation == 1) {
                        sizeX = i_27;
                        sizeY = 512 - i_26;
                    } else if (this.tileRotation == 2) {
                        sizeX = 512 - i_26;
                        sizeY = 512 - i_27;
                    } else if (this.tileRotation == 3) {
                        sizeX = 512 - i_27;
                        sizeY = i_26;
                    } else {
                        sizeX = i_26;
                        sizeY = i_27;
                    }
                    sizesX[this.index] = sizeX;
                    sizesY[this.index] = sizeY;
                    int sceneX;
                    int sceneY;
                    if (groundDrawHeights != null && OVERLAY_SHAPE_SUPPORTS_HEIGHT[this.pathShape][rotation]) {
                        sceneX = (x << 9) + sizeX;
                        sceneY = (y << 9) + sizeY;
                        groundDrawHeights[this.index] = groundB.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY);
                    }
                    if (ints_14 != null) {
                        if (groundB != null && !OVERLAY_SHAPE_SUPPORTS_HEIGHT[this.pathShape][rotation]) {
                            sceneX = (x << 9) + sizeX;
                            sceneY = (y << 9) + sizeY;
                            ints_14[this.index] = groundA.getAverageHeight(sceneX, sceneY) - groundB.getAverageHeight(sceneX, sceneY);
                        } else if (groundC != null && !aBoolArrayArray3823[this.pathShape][rotation]) {
                            sceneX = (x << 9) + sizeX;
                            sceneY = (y << 9) + sizeY;
                            ints_14[this.index] = groundC.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY);
                        }
                    }
                    if (rotation < 8 && this.waterSlots[i_25] > overlay.slot) {
                        if (ints_7 != null) {
                            ints_7[this.index] = this.overlayTileRenderedColors[i_25];
                        }
                        waterTexturesScale[this.index] = this.waterTextureScaling[i_25];
                        waterTextures[this.index] = this.waterTextureIds[i_25];
                        ints_10[this.index] = this.overlayTileColors[i_25];
                    } else {
                        if (ints_7 != null) {
                            ints_7[this.index] = overlayBaseColor;
                        }
                        // set water texture
                        waterTextures[this.index] = overlay.texture;
                        waterTexturesScale[this.index] = overlay.textureScale;
                        ints_10[this.index] = this.overlayHslColor;
                    }
                    ++this.index;
                }
                ++this.overlayTotalFaces;
            }
            if (!this.overlayHidden && i_2 == 0) {
                this.scene.addWater(x, y, overlay.waterColor, overlay.waterFogDepth, overlay.waterIntensity, overlay.opcode20, overlay.unusedOpcode21, overlay.unusedOpcode22);
            }
            if (this.pathShape != 12 && overlay.tileColorHsl != -1 && overlay.shadowed) {
                this.hasShadows = true;
            }
        } else if (this.unblendable) { // block tiles ?
            this.overlayTotalFaces += OVERLAY_FACE_COUNT[this.pathShape];
        } else if (this.overlaySupportsBlending) { // blended untextured tiles ?
            this.overlayTotalFaces += OVERLAY_FACE_COUNT_BLENDED[this.pathShape];
        } else { // blended textured tiles ?
            this.overlayTotalFaces += anIntArray3826[this.pathShape];
        }
    }

    public void addUnderlayTiles(int x, int y, int nextX, int nextY, FluType underlay, int underlayIdA, int underlayIdB, int underlayIdC, int underlayIdD, boolean[] blendable, int[] overRenderColor, int[] sizesX, int[] sizesY, int[] overColor, int[] overTexture, int[] overScale, int[] groundHeightsA, int[] groundHeightsB, int[][] overlayColors, Ground groundA, Ground groundB, Ground groundC) {
        if (underlay != null) {
            if (underlayIdB == 0) {
                underlayIdB = underlayIdA;
            }
            if (underlayIdC == 0) {
                underlayIdC = underlayIdA;
            }
            if (underlayIdD == 0) {
                underlayIdD = underlayIdA;
            }
            FluType underlayA = this.underlayTypeList.list(underlayIdA - 1);
            FluType underlayB = this.underlayTypeList.list(underlayIdB - 1);
            FluType underlayC = this.underlayTypeList.list(underlayIdC - 1);
            FluType underlayD = this.underlayTypeList.list(underlayIdD - 1);
            for (int underlayFace = 0; underlayFace < this.underlayFaceCount; underlayFace++) {
                byte b_32;
                if (blendable[-this.tileRotation & 0x3] && this.overlayTotalFaces == this.overlayFaceCounts[0]) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 1;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 1;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_32 = 6;
                } else if (blendable[2 - this.tileRotation & 0x3] && this.overlayFaceCounts[2] == this.overlayTotalFaces) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 5;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 5;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_32 = 6;
                } else if (blendable[1 - this.tileRotation & 0x3] && this.overlayFaceCounts[1] == this.overlayTotalFaces) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 3;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 3;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_32 = 6;
                } else if (blendable[3 - this.tileRotation & 0x3] && this.overlayFaceCounts[3] == this.overlayTotalFaces) {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = 7;
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    this.overlayTileRotations[3] = 7;
                    this.overlayTileRotations[4] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[5] = this.anIntArray3859[this.overlayTotalFaces];
                    b_32 = 6;
                } else {
                    this.overlayTileRotations[0] = this.anIntArray3857[this.overlayTotalFaces];
                    this.overlayTileRotations[1] = this.anIntArray3858[this.overlayTotalFaces];
                    this.overlayTileRotations[2] = this.anIntArray3859[this.overlayTotalFaces];
                    b_32 = 3;
                }
                for (int i_33 = 0; i_33 < b_32; i_33++) {
                    int rotation = this.overlayTileRotations[i_33];
                    int slot = rotation - this.tileRotation * 2 & 0x7;
                    int dX = tileSizeDeltasX[rotation];
                    int dY = tileSizeDeltasY[rotation];
                    int sizeX;
                    int sizeY;
                    if (this.tileRotation == 1) {
                        sizeX = dY;
                        sizeY = 512 - dX;
                    } else if (this.tileRotation == 2) {
                        sizeX = 512 - dX;
                        sizeY = 512 - dY;
                    } else if (this.tileRotation == 3) {
                        sizeX = 512 - dY;
                        sizeY = dX;
                    } else {
                        sizeX = dX;
                        sizeY = dY;
                    }
                    sizesX[this.index] = sizeX;
                    sizesY[this.index] = sizeY;
                    if (groundHeightsA != null && OVERLAY_SHAPE_SUPPORTS_HEIGHT[this.pathShape][rotation]) {
                    	int sceneX = (x << 9) + sizeX;
                    	int sceneY = (y << 9) + sizeY;
                        groundHeightsA[this.index] = groundB.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY);
                    }
                    if (groundHeightsB != null) {
                        if (groundB != null && !OVERLAY_SHAPE_SUPPORTS_HEIGHT[this.pathShape][rotation]) {
                        	int sceneX = (x << 9) + sizeX;
                        	int sceneY = (y << 9) + sizeY;
                            groundHeightsB[this.index] = groundA.getAverageHeight(sceneX, sceneY) - groundB.getAverageHeight(sceneX, sceneY);
                        } else if (groundC != null && !aBoolArrayArray3823[this.pathShape][rotation]) {
                        	int sceneX = (x << 9) + sizeX;
                        	int sceneY = (y << 9) + sizeY;
                            groundHeightsB[this.index] = groundC.getAverageHeight(sceneX, sceneY) - groundA.getAverageHeight(sceneX, sceneY);
                        }
                    }
                    if (rotation < 8 && this.waterSlots[slot] >= 0) {
                        if (overRenderColor != null) {
                            overRenderColor[this.index] = this.overlayTileRenderedColors[slot];
                        }
                        overScale[this.index] = this.waterTextureScaling[slot];
                        overTexture[this.index] = this.waterTextureIds[slot];
                        overColor[this.index] = this.overlayTileColors[slot];
                    } else {
                        if (this.overlaySupportsBlending && OVERLAY_SHAPE_SUPPORTS_HEIGHT[this.pathShape][rotation]) {
                            overTexture[this.index] = this.overlayTextureId;
                            overScale[this.index] = this.overlayTextureScale;
                            overColor[this.index] = this.overlayHslColor;
                        } else if (sizeX == 0 && sizeY == 0) {
                            overColor[this.index] = overlayColors[x][y];
                            overTexture[this.index] = underlayA.texture;
                            overScale[this.index] = underlayA.scale;
                        } else if (sizeX == 0 && sizeY == 512) {
                            overColor[this.index] = overlayColors[x][nextY];
                            overTexture[this.index] = underlayB.texture;
                            overScale[this.index] = underlayB.scale;
                        } else if (sizeX == 512 && sizeY == 512) {
                            overColor[this.index] = overlayColors[nextX][nextY];
                            overTexture[this.index] = underlayC.texture;
                            overScale[this.index] = underlayC.scale;
                        } else if (sizeX == 512 && sizeY == 0) {
                            overColor[this.index] = overlayColors[nextX][y];
                            overTexture[this.index] = underlayD.texture;
                            overScale[this.index] = underlayD.scale;
                        } else {
                            if (sizeX < 256) {
                                if (sizeY < 256) {
                                    overTexture[this.index] = underlayA.texture;
                                    overScale[this.index] = underlayA.scale;
                                } else {
                                    overTexture[this.index] = underlayB.texture;
                                    overScale[this.index] = underlayB.scale;
                                }
                            } else if (sizeY < 256) {
                                overTexture[this.index] = underlayD.texture;
                                overScale[this.index] = underlayD.scale;
                            } else {
                                overTexture[this.index] = underlayC.texture;
                                overScale[this.index] = underlayC.scale;
                            }
                            int overlayColorA = ColorUtil.blend(overlayColors[x][y], overlayColors[nextX][y], sizeX << 7 >> 9);
                            int overlayColorB = ColorUtil.blend(overlayColors[x][nextY], overlayColors[nextX][nextY], sizeX << 7 >> 9);
                            overColor[this.index] = ColorUtil.blend(overlayColorA, overlayColorB, sizeY << 7 >> 9);
                        }
                        if (overRenderColor != null) {
                            overRenderColor[this.index] = overColor[this.index];
                        }
                    }
                    this.index++;
                }
                this.overlayTotalFaces++;
            }
            if (this.pathShape != 0 && underlay.shadowed) {
                this.hasShadows = true;
            }
        }
    }
    
    
    public void setupCulling(Ground ground, FluType underlay, FloType overlay, int z, int x, int y, int nextX, int nextY, int underlayId, int overlayId) {
        int heightNw = ground.getHeight(x, y);
        int heightNe = ground.getHeight(nextX, y);
        int heightSe = ground.getHeight(nextX, nextY);
        int heightSw = ground.getHeight(x, nextY);
        boolean toggleRoofs = this.regionMap.areRoofsHidden(x, y);
        if (toggleRoofs ? z > 1 : z > 0) {
            boolean occludes = true;
            if (underlay != null && !underlay.occlude) {
                occludes = false;
            } else if (underlayId == 0 && this.pathShape != 0) {
                occludes = false;
            } else if (overlayId > 0 && overlay != null && !overlay.occlude) {
                occludes = false;
            }
            if (occludes && heightNe == heightNw && heightNw == heightSe && heightNw == heightSw) {
                this.cullingsMap[z][x][y] |= CullingFunctionMask.COMPLETELY_FLAT;
            }
        }
    }

    public void offsetTileHeights(int[][] heights2) {
        int[][] heights = this.tileHeights[0];
        for (int x = 0; x < this.regionSizeX + 1; x++) {
            for (int y = 0; y < this.regionSizeY + 1; y++) {
                heights[x][y] += heights2[x][y];
            }
        }
    }

    public void calculateOverlayRotation(AbstractToolkit drawingarea, FloType overlayType, int x, int y, byte[][] lPathShape, byte[][] lOverlayRotation, byte[][] lOverlay, boolean[] hasFacesOn) {
        boolean[] blendings = overlayType != null && overlayType.blendsWithUnderlay ? pathBlendings[this.pathShape] : pathSquares[this.pathShape];
        calculateOverlayDisplay(drawingarea, overlayType, x, y, this.regionSizeX, this.regionSizeY, lOverlay, lPathShape, lOverlayRotation, hasFacesOn);
        this.overlayHasMultiColor = overlayType != null && overlayType.tileColorHsl != overlayType.minimapColorHsl;
        if (!this.overlayHasMultiColor) {
            for (int slot = 0; slot < 8; slot++) {
                if (this.waterSlots[slot] >= 0 && this.overlayTileRenderedColors[slot] != this.overlayTileColors[slot]) {
                    this.overlayHasMultiColor = true;
                    break;
                }
            }
        }
        if (!blendings[this.tileRotation + 1 & 0x3]) {
            hasFacesOn[1] |= (this.overlayFunctionMask[2] & this.overlayFunctionMask[4]) == 0;
        }
        if (!blendings[this.tileRotation + 3 & 0x3]) {
            hasFacesOn[3] |= (this.overlayFunctionMask[6] & this.overlayFunctionMask[0]) == 0;
        }
        if (!blendings[this.tileRotation & 0x3]) {
            hasFacesOn[0] |= (this.overlayFunctionMask[0] & this.overlayFunctionMask[2]) == 0;
        }
        if (!blendings[this.tileRotation + 2 & 0x3]) {
            hasFacesOn[2] |= (this.overlayFunctionMask[4] & this.overlayFunctionMask[6]) == 0;
        }
        if (!this.overlaySupportsBlending && (this.pathShape == 0 || this.pathShape == 12)) {
            if (hasFacesOn[0] && !hasFacesOn[1] && !hasFacesOn[2] && hasFacesOn[3]) {
                hasFacesOn[3] = false;
                hasFacesOn[0] = false;
                this.pathShape = this.pathShape == 0 ? 13 : 14;
                this.tileRotation = 0;
            } else if (hasFacesOn[0] && hasFacesOn[1] && !hasFacesOn[2] && !hasFacesOn[3]) {
                hasFacesOn[1] = false;
                hasFacesOn[0] = false;
                this.pathShape = this.pathShape == 0 ? 13 : 14;
                this.tileRotation = 3;
            } else if (!hasFacesOn[0] && hasFacesOn[1] && hasFacesOn[2] && !hasFacesOn[3]) {
                hasFacesOn[2] = false;
                hasFacesOn[1] = false;
                this.pathShape = this.pathShape == 0 ? 13 : 14;
                this.tileRotation = 2;
            } else if (!hasFacesOn[0] && !hasFacesOn[1] && hasFacesOn[2] && hasFacesOn[3]) {
                hasFacesOn[3] = false;
                hasFacesOn[2] = false;
                this.pathShape = this.pathShape == 0 ? 13 : 14;
                this.tileRotation = 1;
            }
        }
    }

    public int calculateUnderlayRotation(int underlayId, int x, int y, int nextX, int nextY, Ground ground, byte[][] underlays) {
        if ((this.pathShape == 0 || this.pathShape == 12) && x > 0 && y > 0 && x < this.regionSizeX && y < this.regionSizeY) {
            int underlaySouthWest = (underlays[x - 1][y - 1] == underlayId ? 1 : -1);
            int underlaySouthEast = (underlays[nextX][y - 1] == underlayId ? 1 : -1);
            int underlayNorthEast = (underlays[nextX][nextY] == underlayId ? 1 : -1);
            int underlayNorthWest = (underlays[x - 1][nextY] == underlayId ? 1 : -1);
            if (underlays[x][y - 1] == underlayId) {
                underlaySouthWest++;
                underlaySouthEast++;
            } else {
                underlaySouthWest--;
                underlaySouthEast--;
            }
            if (underlays[nextX][y] == underlayId) {
                underlaySouthEast++;
                underlayNorthEast++;
            } else {
                underlaySouthEast--;
                underlayNorthEast--;
            }
            if (underlays[x][nextY] == underlayId) {
                underlayNorthEast++;
                underlayNorthWest++;
            } else {
                underlayNorthEast--;
                underlayNorthWest--;
            }
            if (underlays[x - 1][y] == underlayId) {
                underlayNorthWest++;
                underlaySouthWest++;
            } else {
                underlayNorthWest--;
                underlaySouthWest--;
            }
            int swNeDelta = underlaySouthWest - underlayNorthEast;
            if (swNeDelta < 0) {
                swNeDelta = -swNeDelta;
            }
            int seNwDelta = underlaySouthEast - underlayNorthWest;
            if (seNwDelta < 0) {
                seNwDelta = -seNwDelta;
            }
            if (swNeDelta == seNwDelta) {
                swNeDelta = ground.getHeight(x, y) - ground.getHeight(nextX, nextY);
                if (swNeDelta < 0) {
                    swNeDelta = -swNeDelta;
                }
                seNwDelta = ground.getHeight(nextX, y) - ground.getHeight(x, nextY);
                if (seNwDelta < 0) {
                    seNwDelta = -seNwDelta;
                }
            }
            return swNeDelta < seNwDelta ? 1 : 0;
        } 
        return this.tileRotation;
    }

    public void decodeTile(Packet stream, int plane, int x, int y, int i_5, int i_6, int i_7, int i_8, int i_9, boolean underwater) {
        int deltaY = i_6;
        int deltaX = i_5;
        if (i_9 == 1) {
            deltaY = 1;
        } else if (i_9 == 2) {
            deltaX = 1;
            deltaY = 1;
        } else if (i_9 == 3) {
            deltaX = 1;
        }
        int opcode;
        if (x >= 0 && x < this.regionSizeX && y >= 0 && y < this.regionSizeY) {
            if (!this.overlayHidden && !underwater) {
                this.regionMap.tileFunctionMasks[plane][x][y] = 0;
            }
            while (true) {
                opcode = stream.readUnsignedByte();
                if (opcode == 0) {
                    if (this.overlayHidden) {
                        this.tileHeights[0][x + deltaX][y + deltaY] = 0;
                    } else if (plane == 0) {
                        this.tileHeights[0][x + deltaX][y + deltaY] = -TileUtils.calculateTileheight(i_7 + 932731, i_8 + 556238) * 8 << 2;
                    } else {
                        this.tileHeights[plane][x + deltaX][y + deltaY] = this.tileHeights[plane - 1][x + deltaX][y + deltaY] - 960;
                    }
                    break;
                }
                if (opcode == 1) {
                    int i_13 = stream.readUnsignedByte();
                    if (!this.overlayHidden) {
                        if (i_13 == 1) {
                            i_13 = 0;
                        }
                        if (plane == 0) {
                            this.tileHeights[0][x + deltaX][y + deltaY] = -i_13 * 8 << 2;
                        } else {
                            this.tileHeights[plane][x + deltaX][y + deltaY] = this.tileHeights[plane - 1][x + deltaX][y + deltaY] - (i_13 * 8 << 2);
                        }
                    } else {
                        this.tileHeights[0][x + deltaX][y + deltaY] = i_13 * 8 << 2;
                    }
                    break;
                }
                if (opcode <= 49) {
                    if (underwater) {
                        stream.readUnsignedByte();
                    } else {
                        this.overlayIds[plane][x][y] = stream.readByte();
                        this.overlayPathShapes[plane][x][y] = (byte) ((opcode - 2) / 4);
                        this.overlayRotations[plane][x][y] = (byte) (opcode - 2 + i_9 & 0x3);
                    }
                } else if (opcode <= 81) {
                    if (!this.overlayHidden && !underwater) {
                        this.regionMap.tileFunctionMasks[plane][x][y] = (byte) (opcode - 49);
                    }
                } else if (!underwater) {
                    this.underlayIds[plane][x][y] = (byte) (opcode - 81);
                }
            }
        } else {
            while (true) {
                opcode = stream.readUnsignedByte();
                if (opcode == 0) {
                    break;
                }
                if (opcode == 1) {
                    stream.readUnsignedByte();
                    break;
                }
                if (opcode <= 49) {
                    stream.readUnsignedByte();
                }
            }
        }
    }
}
