package com.jagex.game.runetek5.scene;

import java.util.Iterator;

import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.entity.*;
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration;
import com.jagex.game.runetek5.entity.gameobject.AbstractWall;
import com.jagex.game.runetek5.entity.gameobject.AbstractWallDecoration;
import com.jagex.game.runetek5.entity.grounditem.AbstractGroundItem;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.light.FlickeringEffect;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager;
import com.jagex.game.runetek5.graphics.shadow.Shadow;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.scene.culling.Culling;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.scene.tile.WaterTile;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.NanoTimer;

public class Scene {

    public static boolean synchronousMaploading = true;

    public Tile[][][] tiles;

    public Ground[] localHeightmap;

    public int regionSizeZ;
    public final int tileUnits;
    public final int sceneDimensionX;
    public final int sceneDimensionY;
    public final Culling culling;
    public final Tile[][][] surfaceTiles;
    public final Ground[] surfaceHeightmap;
    public Tile[][][] waterTiles;
    public Ground[] underwaterHeightmap;
    public final InteractiveEntityCache interactiveEntities;
    public int interactiveObjectCount;
    public int lightSlot;
    public int depthObjectCount;
    public int layeredObjectCount;
    public int tileDrawCycle;
    public boolean calculateTilesOnScreen;
    public int cameraGridPositionX;
    public int cameraGridPositionY;
    public int cameraPositionX;
    public int cameraPositionZ;
    public int cameraPositionY;
    public int visibleToXnegative;
    public int visibleToInvertedXnegative;
    public int visibleToYnegative;
    public int visibleToInvertedYnegative;
    public int visibleToXpositive;
    public int visibleToYpositive;
    public boolean[][] adjacentTileOnScreen;
    public boolean[][] tileOnScreen;
    public boolean[][] adjacent_tile_on_screen;
    public int anInt2618 = 5015;
    public int anInt2619 = 5040;
    public int anInt2622 = 16;
    public int anInt2634 = 10072;
    public int anInt2643 = 1;
    public final float[] renderedGraphNodePosition;
    public final PointLight[] activeLights;
    public final AbstractToolkit activeToolkit;
    public final boolean copyLights;
    public final int fullTileDimension;
    public final int halfTileOffset;
    public final int visibleTiles;
    public final NanoTimer nanoTimer;
    public int[][] colors;
    public byte[][] intensities;
    public short[][] scales;
    public byte[][] opcode20;
    public byte[][] opcode21;
    public byte[][] opcode22;
    public long[][][] tileLightUids;
    public FlickeringEffect[] lightSlotOccupant;
    public boolean[] lightSlotAvailable;
    public final SceneGraphEntity[] renderObjects3d;
    public final SceneGraphEntity[] renderObjects2d;
    public final SceneGraphEntity[] dynamicRenderObjects;
    
    /**
     * Objects of which face_render_priorities
     * can be disregarded
     * (Could be wrong, could be: opaque_graph_node)
     * graph_nodes_3d before
     */
    public final SceneGraphEntity[] depthRenderedEntities;
    
    /**
     * Objects that need to have their face_render_priority
     * taken into account for correct rendering
     * (could be wrong, could be: transparent_graph_node)
     */
    public final SceneGraphEntity[] priorityRenderedEntities;
    public final AbstractInteractiveEntity[] interactiveObjects;
    
    /**
     * 0 = onscreen
     * 1 = offscreen
     */
    public final int[] tileVisibility;

    public Scene(AbstractToolkit renderer, int sceneWidth, int sceneLength, int visibleTiles, boolean hasUnderwaterMap, boolean hasLights) {
    	this.renderedGraphNodePosition = new float[3];
        this.activeLights = new PointLight[8];
        this.activeToolkit = renderer;
        this.copyLights = this.activeToolkit.getMaxLights() > 0;
        this.tileUnits = 9;
        this.fullTileDimension = 1 << this.tileUnits;
        this.halfTileOffset = this.fullTileDimension >> 1;
        this.regionSizeZ = 4;
        this.sceneDimensionX = sceneWidth;
        this.sceneDimensionY = sceneLength;
        this.visibleTiles = visibleTiles;
        this.nanoTimer = new NanoTimer();
        this.culling = new Culling(this);
        this.surfaceTiles = new Tile[4][sceneWidth][sceneLength];
        this.surfaceHeightmap = new Ground[4];
        if (hasUnderwaterMap) {
        	this.colors = new int[sceneWidth][sceneLength];
        	this.intensities = new byte[sceneWidth][sceneLength];
        	this.scales = new short[sceneWidth][sceneLength];
        	this.opcode20 = new byte[sceneWidth][sceneLength];
        	this.opcode21 = new byte[sceneWidth][sceneLength];
        	this.opcode22 = new byte[sceneWidth][sceneLength];
        	this.waterTiles = new Tile[1][sceneWidth][sceneLength];
        	this.underwaterHeightmap = new Ground[1];
        }
        if (hasLights) {
        	this.tileLightUids = new long[4][sceneWidth][sceneLength];
        	this.lightSlotOccupant = new FlickeringEffect[65361];
        	this.lightSlotAvailable = new boolean[65361];
        	this.lightSlot = 0;
        }
        this.setUnderwater(false);
        this.renderObjects3d = new SceneGraphEntity[2];
        this.renderObjects2d = new SceneGraphEntity[2];
        this.dynamicRenderObjects = new SceneGraphEntity[2];
        this.depthRenderedEntities = new SceneGraphEntity[10072];
        this.depthObjectCount = 0;
        this.priorityRenderedEntities = new SceneGraphEntity[5015];
        this.layeredObjectCount = 0;
        this.interactiveObjects = new AbstractInteractiveEntity[5040];
        this.interactiveObjectCount = 0;
        this.adjacent_tile_on_screen = new boolean[visibleTiles + visibleTiles + 1][visibleTiles + visibleTiles + 1];
        this.tileOnScreen = new boolean[visibleTiles + visibleTiles + 2][visibleTiles + visibleTiles + 2];
        this.tileVisibility = new int[visibleTiles + visibleTiles + 2];
        this.interactiveEntities = new InteractiveEntityCache();
    }

    public static void setRoofOverride() {
        int roofToggle = PreferenceManager.preferences.removeRoofsOptionOverride.getValue();
        if (roofToggle == 0) {
        	// Reset
            Resource.mapRegion.setActiveSettingsBits(null);
            initializeRoofSettings(0);
        } else if (roofToggle == 1) {
        	// Always
            resetSettingsBits((byte) 0);
            initializeRoofSettings(512);
            if (Resource.mapRegion.getScene() != null) {
                method570();
            }
        } else {
        	// Selectively
            resetSettingsBits((byte) (client.timesSceneRendered - 4 & 0xff));
            initializeRoofSettings(2);
        }

        client.ROOF_OVERRIDE = PlayerUpdate.MY_PLAYER_PLANE;
    }

    public static void method570() {
        int i_1 = 0;
        Scene scene = Resource.mapRegion.getScene();
        for (int sceneX = 0; sceneX < Resource.mapRegion.getSceneSizeX(); sceneX++) {
            for (int sceneY = 0; sceneY < Resource.mapRegion.getSceneSizeY(); sceneY++) {
                if (MapRegion.calculateVisibilityAdjustments(scene.tiles, i_1, sceneX, sceneY, true)) {
                    i_1++;
                }
                if (i_1 >= 512) {
                    return;
                }
            }
        }
    }

    public static void initializeRoofSettings(int i_0) {
        client.visibleBelowDrawheight = new int[i_0];
        client.visibleAboveX = new int[i_0];
        client.visibleBelowX = new int[i_0];
        client.visibleBelowY = new int[i_0];
        client.visibleAboveY = new int[i_0];
    }

    public static void resetSettingsBits(byte defaultSettings) {
        byte[][][] settingsBits = Resource.mapRegion.getBits();
        if (settingsBits == null) {
            settingsBits = new byte[4][Resource.mapRegion.getSceneSizeX()][Resource.mapRegion.getSceneSizeY()];
            Resource.mapRegion.setActiveSettingsBits(settingsBits);
        }
        for (int plane = 0; plane < 4; plane++) {
            for (int x = 0; x < Resource.mapRegion.getSceneSizeX(); x++) {
                for (int y = 0; y < Resource.mapRegion.getSceneSizeY(); y++) {
                    settingsBits[plane][x][y] = defaultSettings;
                }
            }
        }
    }

    public void empty1() {
    }

    public void empty2() {
    }

    public void setUnderwater(boolean underwater) {
        if (underwater) {
            this.tiles = this.waterTiles;
            this.localHeightmap = this.underwaterHeightmap;
        } else {
            this.tiles = this.surfaceTiles;
            this.localHeightmap = this.surfaceHeightmap;
        }
        this.regionSizeZ = this.tiles.length;
    }

    public AbstractWall getWall(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        return tile == null ? null : tile.primaryWall;
    }

    public void linkBelow(int x, int y) {
        Tile baseFloorTile = this.tiles[0][x][y];
        for (int plane = 0; plane < 3; plane++) {
            Tile tileNextLvl = this.tiles[plane][x][y] = this.tiles[plane + 1][x][y];
            if (tileNextLvl != null) {
                for (InteractiveEntityContainer wrapper = tileNextLvl.interactives; wrapper != null; wrapper = wrapper.child) {
                    AbstractInteractiveEntity interactive = wrapper.interactiveObject;
                    if (interactive.startX == x && y == interactive.startZ) {
                        interactive.plane -= 1;
                    }
                }
                if (tileNextLvl.groundDecoration != null) {
                    tileNextLvl.groundDecoration.plane -= 1;
                }
                if (tileNextLvl.primaryWall != null) {
                    tileNextLvl.primaryWall.plane -= 1;
                }
                if (tileNextLvl.secondaryWall != null) {
                    tileNextLvl.secondaryWall.plane -= 1;
                }
                if (tileNextLvl.primaryWallDecoration != null) {
                    tileNextLvl.primaryWallDecoration.plane -= 1;
                }
                if (tileNextLvl.secondaryWallDecoration != null) {
                    tileNextLvl.secondaryWallDecoration.plane -= 1;
                }
            }
        }
        if (this.tiles[0][x][y] == null) {
            this.tiles[0][x][y] = new Tile(0);
            this.tiles[0][x][y].plane = 1;
        }
        this.tiles[0][x][y].bridge = baseFloorTile;
        this.tiles[3][x][y] = null;
    }

    public void loadTiles(int z, int x, int y) {
        boolean hasBridge = this.tiles[0][x][y] != null && this.tiles[0][x][y].bridge != null;
        for (int currentZ = z; currentZ >= 0; --currentZ) {
            if (this.tiles[currentZ][x][y] == null) {
                Tile tile = this.tiles[currentZ][x][y] = new Tile(currentZ);
                if (hasBridge) {
                    tile.plane++;
                }
            }
        }
    }

    public void setLocalGround(int z, Ground ground) {
        this.localHeightmap[z] = ground;
    }

    public int getScale(int x, int y) {
        return this.scales != null ? this.scales[x][y] & 0xffff : 0;
    }

    public int getColor(int x, int y) {
        return this.colors != null ? this.colors[x][y] & 0xffffff : 0;
    }

    public int unusedOpcode22(int x, int y) {
        return this.opcode22 != null ? this.opcode22[x][y] & 0xff : 0;
    }

    public void addWater(int x, int y, int waterColor, int waterScale, int waterIntensity, int opcode20, int opcode21, int opcode22) {
        if (this.colors != null) {
        	this.colors[x][y] = -16777216 | waterColor;
        }
        if (this.scales != null) {
        	this.scales[x][y] = (short) waterScale;
        }
        if (this.intensities != null) {
        	this.intensities[x][y] = (byte) waterIntensity;
        }
        if (this.opcode20 != null) {
        	this.opcode20[x][y] = (byte) opcode20;
        }
        if (this.opcode21 != null) {
        	this.opcode21[x][y] = (byte) opcode21;
        }
        if (this.opcode22 != null) {
        	this.opcode22[x][y] = (byte) opcode22;
        }
    }

    public void addGroundDecoration(int logicHeight, int baseX, int baseY, AbstractGroundDecoration groundDecoration) {
        Tile tile = getTile(logicHeight, baseX, baseY);
        if (tile != null) {
            tile.groundDecoration = groundDecoration;
            int type = this.localHeightmap == this.underwaterHeightmap ? 1 : 0;
            if (groundDecoration.disregardTextureMovement()) {
                if (groundDecoration.hasAlpha()) {
                    groundDecoration.child = this.renderObjects2d[type];
                    this.renderObjects2d[type] = groundDecoration;
                } else {
                    groundDecoration.child = this.renderObjects3d[type];
                    this.renderObjects3d[type] = groundDecoration;
                }
            } else {
                groundDecoration.child = this.dynamicRenderObjects[type];
                this.dynamicRenderObjects[type] = groundDecoration;
            }
        }
    }

    public void addGroundItem(int plane, int baseX, int baseY, int drawHeight, AbstractGroundItem groundItem) {
        Tile tile = getTile(plane, baseX, baseY);
        if (tile != null) {
            groundItem.setBasePosition(new Vector3(((baseX << this.tileUnits) + this.halfTileOffset), drawHeight, ((baseY << this.tileUnits) + this.halfTileOffset)));
            tile.groundItem = groundItem;
            int type = this.underwaterHeightmap == this.localHeightmap ? 1 : 0;
            if (groundItem.disregardTextureMovement()) {
                if (groundItem.hasAlpha()) {
                    groundItem.child = this.renderObjects2d[type];
                    this.renderObjects2d[type] = groundItem;
                } else {
                    groundItem.child = this.renderObjects3d[type];
                    this.renderObjects3d[type] = groundItem;
                }
            } else {
                groundItem.child = this.dynamicRenderObjects[type];
                this.dynamicRenderObjects[type] = groundItem;
            }
        }
    }

    
    public void addWall(int locHeight, int baseX, int baseY, AbstractWall primaryWall, AbstractWall secondaryWall) {
        Tile tile = getTile(locHeight, baseX, baseY);
        if (tile != null) {
            tile.primaryWall = primaryWall;
            tile.secondaryWall = secondaryWall;
            int type = this.underwaterHeightmap == this.localHeightmap ? 1 : 0;
            if (primaryWall.disregardTextureMovement()) {
                if (primaryWall.hasAlpha()) {
                    primaryWall.child = this.renderObjects2d[type];
                    this.renderObjects2d[type] = primaryWall;
                } else {
                    primaryWall.child = this.renderObjects3d[type];
                    this.renderObjects3d[type] = primaryWall;
                }
            } else {
                primaryWall.child = this.dynamicRenderObjects[type];
                this.dynamicRenderObjects[type] = primaryWall;
            }
            if (secondaryWall != null) {
                if (secondaryWall.disregardTextureMovement()) {
                    if (secondaryWall.hasAlpha()) {
                        secondaryWall.child = this.renderObjects2d[type];
                        this.renderObjects2d[type] = secondaryWall;
                    } else {
                        secondaryWall.child = this.renderObjects3d[type];
                        this.renderObjects3d[type] = secondaryWall;
                    }
                } else {
                    secondaryWall.child = this.dynamicRenderObjects[type];
                    this.dynamicRenderObjects[type] = secondaryWall;
                }
            }
        }
    }

    public void addWallDecoration(int wallPlane, int baseX, int baseY, AbstractWallDecoration primaryWallDecoration, AbstractWallDecoration secondaryWallDecoration) {
        Tile tile = getTile(wallPlane, baseX, baseY);
        if (tile != null) {
            tile.primaryWallDecoration = primaryWallDecoration;
            tile.secondaryWallDecoration = secondaryWallDecoration;
            int renderType = this.underwaterHeightmap == this.localHeightmap ? 1 : 0;
            if (primaryWallDecoration.disregardTextureMovement()) {
                if (primaryWallDecoration.hasAlpha()) {
                    primaryWallDecoration.child = this.renderObjects2d[renderType];
                    this.renderObjects2d[renderType] = primaryWallDecoration;
                } else {
                    primaryWallDecoration.child = this.renderObjects3d[renderType];
                    this.renderObjects3d[renderType] = primaryWallDecoration;
                }
            } else {
                primaryWallDecoration.child = this.dynamicRenderObjects[renderType];
                this.dynamicRenderObjects[renderType] = primaryWallDecoration;
            }
            if (secondaryWallDecoration != null) {
                if (secondaryWallDecoration.disregardTextureMovement()) {
                    if (secondaryWallDecoration.hasAlpha()) {
                        secondaryWallDecoration.child = this.renderObjects2d[renderType];
                        this.renderObjects2d[renderType] = secondaryWallDecoration;
                    } else {
                        secondaryWallDecoration.child = this.renderObjects3d[renderType];
                        this.renderObjects3d[renderType] = secondaryWallDecoration;
                    }
                } else {
                    secondaryWallDecoration.child = this.dynamicRenderObjects[renderType];
                    this.dynamicRenderObjects[renderType] = secondaryWallDecoration;
                }
            }
        }
    }

    public boolean addInteractive(AbstractInteractiveEntity node, boolean cached) {
        boolean isUnderwater = this.localHeightmap == this.underwaterHeightmap;
        int color = 0;
        short scale = 0;
        byte intensity = 0;
        node.calculatePosition();
        if (node.startX >= 0 && node.startZ >= 0 && node.endX < this.sceneDimensionX && node.endZ < this.sceneDimensionY) {
            short floorOffsetY = 0;
            for (int x = node.startX; x <= node.endX; x++) {
                for (int y = node.startZ; y <= node.endZ; y++) {
                    Tile tile = getTile(node.plane, x, y);
                    if (tile != null) {
                        InteractiveEntityContainer wrapper = InteractiveEntityContainer.add(node);
                        InteractiveEntityContainer emptyChild = tile.interactives;
                        if (emptyChild == null) {
                            tile.interactives = wrapper;
                        } else {
                            while (emptyChild.child != null) {
                                emptyChild = emptyChild.child;
                            }
                            emptyChild.child = wrapper;
                        }
                        if (isUnderwater && (this.colors[x][y] & -16777216) != 0) {
                            color = this.colors[x][y];
                            scale = this.scales[x][y];
                            intensity = this.intensities[x][y];
                        }
                        if (!cached && tile.groundDecoration != null && tile.groundDecoration.floorOffsetY > floorOffsetY) {
                            floorOffsetY = tile.groundDecoration.floorOffsetY;
                        }
                    }
                }
            }
            if (isUnderwater && (color & -16777216) != 0) {
                for (int x = node.startX; x <= node.endX; x++) {
                    for (int y = node.startZ; y <= node.endZ; y++) {
                        if ((this.colors[x][y] & -16777216) == 0) {
                            this.colors[x][y] = color;
                            this.scales[x][y] = scale;
                            this.intensities[x][y] = intensity;
                        }
                    }
                }
            }
            if (cached) {
                this.interactiveObjects[++this.interactiveObjectCount - 1] = node;
                node.scene = this;
            } else {
            	int nodePointer = this.underwaterHeightmap == this.localHeightmap ? 1 : 0;
                if (node.disregardTextureMovement()) {
                    if (node.hasAlpha()) {
                        node.child = this.renderObjects2d[nodePointer];
                        this.renderObjects2d[nodePointer] = node;
                    } else {
                        node.child = this.renderObjects3d[nodePointer];
                        this.renderObjects3d[nodePointer] = node;
                    }
                } else {
                    node.child = this.dynamicRenderObjects[nodePointer];
                    this.dynamicRenderObjects[nodePointer] = node;
                }
            }
            if (cached) {
                Vector3 absolutePosition = Vector3Pool.vector3From(node.getAbsolutePosition().coords);
                absolutePosition.y -= floorOffsetY;
                node.setBasePosition(absolutePosition);
                absolutePosition.pushVectorStack();
            }
            return true;
        } 
        return false;
    }

    public void offsetWallDecoration(int z, int x, int y, int displacement) {
        Tile tile = this.tiles[z][x][y];
        if (tile != null) {
            AbstractWallDecoration first = tile.primaryWallDecoration;
            AbstractWallDecoration second = tile.secondaryWallDecoration;
            /** The upscaling happening below equals to 9 - 7 = 2, to match the model upscaling **/
            if (first != null) {
                first.positionX = (short) (displacement * first.positionX / (16 << this.tileUnits - 7));
                first.positionY = (short) (displacement * first.positionY / (16 << this.tileUnits - 7));
            }
            if (second != null) {
                second.positionX = (short) (displacement * second.positionX / (16 << this.tileUnits - 7));
                second.positionY = (short) (displacement * second.positionY / (16 << this.tileUnits - 7));
            }
        }
    }

    public void clearInteractives() {
        for (int index = 0; index < this.interactiveObjectCount; index++) {
            AbstractInteractiveEntity inteactive = this.interactiveObjects[index];
            removeInteractive(inteactive, true);
            this.interactiveObjects[index] = null;
        }
        this.interactiveObjectCount = 0;
    }

    public void applyShading() {
        for (int z = 0; z < this.regionSizeZ; z++) {
            for (int x = 0; x < this.sceneDimensionX; x++) {
                for (int y = 0; y < this.sceneDimensionY; y++) {
                    Tile tile = this.tiles[z][x][y];
                    if (tile != null) {
                        AbstractWall primaryWall = tile.primaryWall;
                        AbstractWall secondaryWall = tile.secondaryWall;
                        if (primaryWall != null && primaryWall.hasTileShadow()) {
                            castShadow(primaryWall, z, x, y, 1, 1);
                            if (secondaryWall != null && secondaryWall.hasTileShadow()) {
                                castShadow(secondaryWall, z, x, y, 1, 1);
                                secondaryWall.castShadow(primaryWall, 0, 0, 0, false);
                                secondaryWall.removeTileShadow();
                            }
                            primaryWall.removeTileShadow();
                        }
                        for (InteractiveEntityContainer interactiveWrapper = tile.interactives; interactiveWrapper != null; interactiveWrapper = interactiveWrapper.child) {
                            AbstractInteractiveEntity interactive = interactiveWrapper.interactiveObject;
                            if (interactive != null && interactive.hasTileShadow()) {
                                castShadow(interactive, z, x, y, interactive.endX - interactive.startX + 1, interactive.endZ - interactive.startZ + 1);
                                interactive.removeTileShadow();
                            }
                        }
                        AbstractGroundDecoration groundDecoration = tile.groundDecoration;
                        if (groundDecoration != null && groundDecoration.hasTileShadow()) {
                            castShadow(groundDecoration, z, x, y);
                            groundDecoration.removeTileShadow();
                        }
                    }
                }
            }
        }
    }

    public AbstractWallDecoration removePrimaryWallDecoration(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        if (tile != null) {
            pushIntoStack(tile.primaryWallDecoration);
            if (tile.primaryWallDecoration != null) {
                AbstractWallDecoration wallDecoration = tile.primaryWallDecoration;
                tile.primaryWallDecoration = null;
                return wallDecoration;
            }
        }
        return null;
    }

    public AbstractGroundDecoration removeGroundDecoration(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        if (tile == null) {
            return null;
        } 
        pushIntoStack(tile.groundDecoration);
        if (tile.groundDecoration != null) {
            AbstractGroundDecoration groundDecoration = tile.groundDecoration;
            tile.groundDecoration = null;
            return groundDecoration;
        }
        return null;
    }

    public AbstractGroundItem removeGroundItem(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        if (tile == null) {
            return null;
        } 
        AbstractGroundItem groundItem = tile.groundItem;
        tile.groundItem = null;
        pushIntoStack(groundItem);
        return groundItem;
    }

    public void removeInteractive(AbstractInteractiveEntity interactive, boolean clearFully) {
        for (int x = interactive.startX; x <= interactive.endX; x++) {
            for (int y = interactive.startZ; y <= interactive.endZ; y++) {
                Tile tile = this.tiles[interactive.plane][x][y];
                if (tile != null) {
                    InteractiveEntityContainer interactives = tile.interactives;
                    for (InteractiveEntityContainer child = null; interactives != null; interactives = interactives.child) {
                        if (interactive == interactives.interactiveObject) {
                            if (child != null) {
                                child.child = interactives.child;
                            } else {
                                tile.interactives = interactives.child;
                            }
                            interactives.method3560();
                            break;
                        }
                        child = interactives;
                    }
                }
            }
        }
        if (!clearFully) {
            pushIntoStack(interactive);
        }
    }

    public void pushIntoStack(SceneGraphEntity renderedNode) {
        if (renderedNode != null) {
            renderedNode.method11176();
            for (int index = 0; index < 2; index++) {
                SceneGraphEntity childNode = null;
                SceneGraphEntity primaryNode;
                for (primaryNode = this.renderObjects3d[index]; primaryNode != null; primaryNode = primaryNode.child) {
                    if (primaryNode == renderedNode) {
                        if (childNode != null) {
                            childNode.child = primaryNode.child;
                        } else {
                            this.renderObjects3d[index] = primaryNode.child;
                        }
                        return;
                    }
                    childNode = primaryNode;
                }
                childNode = null;
                for (primaryNode = this.renderObjects2d[index]; primaryNode != null; primaryNode = primaryNode.child) {
                    if (primaryNode == renderedNode) {
                        if (childNode != null) {
                            childNode.child = primaryNode.child;
                        } else {
                            this.renderObjects2d[index] = primaryNode.child;
                        }
                        return;
                    }
                    childNode = primaryNode;
                }
                childNode = null;
                for (primaryNode = this.dynamicRenderObjects[index]; primaryNode != null; primaryNode = primaryNode.child) {
                    if (primaryNode == renderedNode) {
                        if (childNode != null) {
                            childNode.child = primaryNode.child;
                        } else {
                            this.dynamicRenderObjects[index] = primaryNode.child;
                        }
                        return;
                    }
                    childNode = primaryNode;
                }
            }
        }
    }

    public AbstractWall getSecondaryWall(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        return tile == null ? null : tile.secondaryWall;
    }

    public AbstractInteractiveEntity getInteractiveObject(int z, int x, int y, ComparableGraphNode comparable) {
        Tile tile = this.tiles[z][x][y];
        if (tile != null) {
            for (InteractiveEntityContainer container = tile.interactives; container != null; container = container.child) {
                AbstractInteractiveEntity interactive = container.interactiveObject;
                if ((comparable == null || comparable.matches(interactive)) && x == interactive.startX && y == interactive.startZ) {
                    return interactive;
                }
            }
        }
        return null;
    }

    public AbstractGroundDecoration getGroundDecoration(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        return tile != null && tile.groundDecoration != null ? tile.groundDecoration : null;
    }

    public void castShadow(SceneGraphEntity graphEntity, int sceneZ, int sceneX, int sceneY) {
        if (sceneX < this.sceneDimensionX) {
            Tile tile = this.tiles[sceneZ][sceneX + 1][sceneY];
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration.hasTileShadow()) {
                graphEntity.castShadow(tile.groundDecoration, this.fullTileDimension, 0, 0, true);
            }
        }
        if (sceneY < this.sceneDimensionX) {
        	Tile tile = this.tiles[sceneZ][sceneX][sceneY + 1];
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration.hasTileShadow()) {
                graphEntity.castShadow(tile.groundDecoration, 0, 0, this.fullTileDimension, true);
            }
        }
        if (sceneX < this.sceneDimensionX && sceneY < this.sceneDimensionY) {
        	Tile tile = this.tiles[sceneZ][sceneX + 1][sceneY + 1];
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration.hasTileShadow()) {
                graphEntity.castShadow(tile.groundDecoration, this.fullTileDimension, 0, this.fullTileDimension, true);
            }
        }
        if (sceneX < this.sceneDimensionX && sceneY > 0) {
        	Tile tile = this.tiles[sceneZ][sceneX + 1][sceneY - 1];
            if (tile != null && tile.groundDecoration != null && tile.groundDecoration.hasTileShadow()) {
                graphEntity.castShadow(tile.groundDecoration, this.fullTileDimension, 0, -this.fullTileDimension, true);
            }
        }
    }

    /**
     * Adjusts the depth of the object (perspective_z)
     * to prevent the floor rendering over an object
     */
    public void separateFromFloor(SceneGraphEntity graphNode) {
        Vector3 absolutePosition = graphNode.getAbsolutePosition().coords;
        this.activeToolkit.calculateProjection(((int) absolutePosition.x), ((int) absolutePosition.y + (graphNode.getMinY() >> 1)), ((int) absolutePosition.z), this.renderedGraphNodePosition);
        graphNode.floorDepth = (int) this.renderedGraphNodePosition[2];
    }

    public AbstractWall removePrimaryWall(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        if (tile != null) {
            pushIntoStack(tile.primaryWall);
            if (tile.primaryWall != null) {
                AbstractWall primaryWall = tile.primaryWall;
                tile.primaryWall = null;
                return primaryWall;
            }
        }
        return null;
    }

    /**
     * Determine if the game object on the tile is supposed to be culled or not
     * If the object is culled that DOES NOT mean the tile is culled too, it just means the gameobject is hidden
     */
    public boolean hideGameobject(SceneGraphEntity graphNode, boolean hideInteractives, byte[][][] tileSettingBits, int locPlane, byte roofPreference) {
        if (!this.calculateTilesOnScreen) {
            return false;
        } 
        if (graphNode instanceof AbstractInteractiveEntity interactive) {
            short endX = interactive.endX;
            short endY = interactive.endZ;
            short startX = interactive.startX;
            short startY = interactive.startZ;
            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {
                    if (graphNode.collisionPlane < this.regionSizeZ && x >= this.visibleToXnegative && x < this.visibleToXpositive && y >= this.visibleToYnegative && y < this.visibleToYpositive) {
                        if ((tileSettingBits == null || graphNode.plane < locPlane || roofPreference != tileSettingBits[graphNode.plane][x][y])
                        		&& graphNode.canRender() && !graphNode.isSoftHidden(this.activeToolkit)) {
                            return false;
                        }
                        if (!hideInteractives && x >= this.cameraGridPositionX - 16 && x <= this.cameraGridPositionX + 16
                        					   && y >= this.cameraGridPositionY - 16 && y <= this.cameraGridPositionY + 16) {
                            graphNode.particleCycle(this.activeToolkit);
                        }
                        return true;
                    }
                }
            }
            return true;
        } else {
            Vector3 absolutePosition = graphNode.getAbsolutePosition().coords;
            int sceneX = (int) absolutePosition.x >> this.tileUnits;
            int sceneY = (int) absolutePosition.z >> this.tileUnits;
            if (graphNode.collisionPlane < this.regionSizeZ && sceneX >= this.visibleToXnegative && sceneX < this.visibleToXpositive
            											   && sceneY >= this.visibleToYnegative && sceneY < this.visibleToYpositive) {
                if ((tileSettingBits == null || graphNode.plane < locPlane || roofPreference != tileSettingBits[graphNode.plane][sceneX][sceneY])
                		&& graphNode.canRender() && !graphNode.isSoftHidden(this.activeToolkit)) {
                    return false;
                } 
                if (!hideInteractives && sceneX >= this.cameraGridPositionX - 16 && sceneX <= this.cameraGridPositionX + 16
                					   && sceneY >= this.cameraGridPositionY - 16 && sceneY <= this.cameraGridPositionY + 16) {
                    graphNode.particleCycle(this.activeToolkit);
                }
                return true;
            } 
            return true;
        }
    }

    public boolean renderShadows(Shadow shadow, int highestDisplayedPlane, int x, int y, boolean[] shadowedTiles) {
        boolean shadowsAdded = false;
        if (this.localHeightmap != this.underwaterHeightmap) {
            int highestPlaneHeight = this.surfaceHeightmap[highestDisplayedPlane].getAverageHeight(x, y);
            for (int plane = 0; plane <= highestDisplayedPlane; plane++) {
                Ground ground = this.surfaceHeightmap[plane];
                if (ground != null) {
                    int heightDelta = highestPlaneHeight - ground.getAverageHeight(x, y);
                    if (shadowedTiles != null) {
                        shadowedTiles[plane] = ground.hasRenderingShadows(shadow, x, heightDelta, y);
                        if (!shadowedTiles[plane]) {
                            continue;
                        }
                    }
                    ground.addShadow(shadow, x, heightDelta, y);
                    shadowsAdded = true;
                }
            }
        }
        return shadowsAdded;
    }

    public void removeShadow(Shadow shadow, int maxPlane, int x, int y, boolean[] castShadowAtPlane) {
        if (this.underwaterHeightmap != this.localHeightmap) {
            int averageHeight = this.surfaceHeightmap[maxPlane].getAverageHeight(x, y);
            for (int plane = 0; plane <= maxPlane; plane++) {
                if (castShadowAtPlane == null || castShadowAtPlane[plane]) {
                    Ground ground = this.surfaceHeightmap[plane];
                    if (ground != null) {
                        ground.removeShadow(shadow, x, averageHeight - ground.getAverageHeight(x, y), y);
                    }
                }
            }
        }
    }

    public void resetSurfaceTiles() {
        for (int x = 0; x < this.sceneDimensionX; x++) {
            for (int y = 0; y < this.sceneDimensionY; y++) {
                if (this.tiles[0][x][y] == null) {
                    this.tiles[0][x][y] = new Tile(0);
                }
            }
        }
    }

    public void addHdShadow() {
        addHdShadow(this.regionSizeZ);
    }

    public void calculateLightUid(FlickeringEffect positionedLight) {
        if (this.lightSlot < 65361) {
            PointLight light = positionedLight.pointLight;
            this.lightSlotOccupant[this.lightSlot] = positionedLight;
            this.lightSlotAvailable[this.lightSlot] = false;
            this.lightSlot++;
            int lightStartZ = positionedLight.level;
            if (positionedLight.growsDownwards) {
                lightStartZ = 0;
            }
            int lightEndZ = positionedLight.level;
            if (positionedLight.growsUpwards) {
                lightEndZ = this.regionSizeZ - 1;
            }
            for (int z = lightStartZ; z <= lightEndZ; z++) {
                int i_7 = 0;
                int lightStartY = light.getZ() - light.getSize2d() + this.halfTileOffset >> this.tileUnits;
                if (lightStartY < 0) {
                    i_7 -= lightStartY;
                    lightStartY = 0;
                }
                int lightEndY = light.getZ() + light.getSize2d() - this.halfTileOffset >> this.tileUnits;
                if (lightEndY >= this.sceneDimensionY) {
                    lightEndY = this.sceneDimensionY - 1;
                }
                for (int y = lightStartY; y <= lightEndY; y++) {
                    short range = positionedLight.ranges[i_7++];
                    int lightStartX = (light.getX() - light.getSize2d() + this.halfTileOffset >> this.tileUnits) + (range >>> 8);
                    int lightEndX = (range & 0xff) + lightStartX - 1;
                    if (lightStartX < 0) {
                        lightStartX = 0;
                    }
                    if (lightEndX >= this.sceneDimensionX) {
                        lightEndX = this.sceneDimensionX - 1;
                    }
                    for (int x = lightStartX; x <= lightEndX; x++) {
                        long lightBaseUids = this.tileLightUids[z][x][y];
                        if ((lightBaseUids & 0xffffL) == 0L) {
                            this.tileLightUids[z][x][y] = lightBaseUids | this.lightSlot;
                        } else if ((lightBaseUids & 0xffff0000L) == 0L) {
                            this.tileLightUids[z][x][y] = lightBaseUids | (long) this.lightSlot << 16;
                        } else if ((lightBaseUids & 0xffff00000000L) == 0L) {
                            this.tileLightUids[z][x][y] = lightBaseUids | (long) this.lightSlot << 32;
                        } else if ((lightBaseUids & -281474976710656L) == 0L) {
                            this.tileLightUids[z][x][y] = lightBaseUids | (long) this.lightSlot << 48;
                        }
                    }
                }
            }
        }
    }

    // bake light?
    public void addHdLight() {
        for (int slotIndex = 0; slotIndex < this.lightSlot; slotIndex++) {
            if (!this.lightSlotAvailable[slotIndex]) {
                FlickeringEffect positionedLight = this.lightSlotOccupant[slotIndex];
                PointLight lightSource = positionedLight.pointLight;
                int z = positionedLight.level;
                int offset = lightSource.getSize2d() - this.halfTileOffset;
                int dimension = (offset * 2 >> this.tileUnits) + 1;
                int Y = 0;
                int[] lightDisplacement = new int[dimension * dimension];
                int deltaX = lightSource.getX() - offset >> this.tileUnits;
                int sceneStartY = lightSource.getZ() - offset >> this.tileUnits;
                int sceneEndY = lightSource.getZ() + offset >> this.tileUnits;
                if (sceneStartY < 0) {
                    Y -= sceneStartY;
                    sceneStartY = 0;
                }
                if (sceneEndY >= this.sceneDimensionY) {
                    sceneEndY = this.sceneDimensionY - 1;
                }
                for (int y = sceneStartY; y <= sceneEndY; y++) {
                    short range = positionedLight.ranges[Y];
                    int sX = range >>> 8;
                    int X = sX + Y * dimension;
                    int sceneStartX = (range >>> 8) + deltaX;
                    int sceneX = (range & 0xff) + sceneStartX - 1;
                    if (sceneStartX < 0) {
                        X -= sceneStartX;
                        sceneStartX = 0;
                    }
                    if (sceneX >= this.sceneDimensionX) {
                        sceneX = this.sceneDimensionX - 1;
                    }
                    for (int x = sceneStartX; x <= sceneX; x++) {
                        byte displacement = 1;
                        AbstractInteractiveEntity interactive = getInteractiveObject(z, x, y, null);
                        if (interactive != null && interactive.rotation != 0) {
                            boolean westWithinScene;
                            boolean eastWithinScene;
                            short bitpackedRange;
                            int startX;
                            int endX;
                            if (interactive.rotation == 1) {
                                westWithinScene = x - 1 >= sceneStartX;
                                eastWithinScene = x + 1 <= sceneX;
                                if (!westWithinScene && y + 1 <= sceneEndY) {
                                    bitpackedRange = positionedLight.ranges[Y + 1];
                                    startX = (bitpackedRange >>> 8) + deltaX;
                                    endX = (bitpackedRange & 0xff) + startX;
                                    westWithinScene = x > startX && x < endX;
                                }
                                if (!eastWithinScene && y - 1 >= sceneStartY) {
                                    bitpackedRange = positionedLight.ranges[Y - 1];
                                    startX = (bitpackedRange >>> 8) + deltaX;
                                    endX = (bitpackedRange & 0xff) + startX;
                                    eastWithinScene = x > startX && x < endX;
                                }
                                if (westWithinScene && !eastWithinScene) {
                                    displacement = 4;
                                } else if (eastWithinScene && !westWithinScene) {
                                    displacement = 2;
                                }
                            } else {
                                westWithinScene = x - 1 >= sceneStartX;
                                eastWithinScene = x + 1 <= sceneX;
                                if (!westWithinScene && y - 1 >= sceneStartY) {
                                    bitpackedRange = positionedLight.ranges[Y - 1];
                                    startX = (bitpackedRange >>> 8) + deltaX;
                                    endX = (bitpackedRange & 0xff) + startX;
                                    westWithinScene = x > startX && x < endX;
                                }
                                if (!eastWithinScene && y + 1 <= sceneEndY) {
                                    bitpackedRange = positionedLight.ranges[Y + 1];
                                    startX = (bitpackedRange >>> 8) + deltaX;
                                    endX = (bitpackedRange & 0xff) + startX;
                                    eastWithinScene = x > startX && x < endX;
                                }
                                if (westWithinScene && !eastWithinScene) {
                                    displacement = 3;
                                } else if (eastWithinScene && !westWithinScene) {
                                    displacement = 5;
                                }
                            }
                        }
                        lightDisplacement[X++] = displacement;
                    }
                    ++Y;
                }
                this.lightSlotAvailable[slotIndex] = true;
                if (synchronousMaploading) {
                    this.localHeightmap[z].addLight(lightSource, lightDisplacement);
                }
            }
        }
    }

    public int getWaterOpcode20(int i_1, int i_2) {
        return this.opcode20 != null ? this.opcode20[i_1][i_2] & 0xff : 0;
    }

    public AbstractGroundItem getGroundItem(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        return tile == null ? null : tile.groundItem;
    }

    public AbstractWallDecoration removeSecondaryWallDecoration(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        if (tile != null) {
            pushIntoStack(tile.secondaryWallDecoration);
            if (tile.secondaryWallDecoration != null) {
                AbstractWallDecoration wallDecoration = tile.secondaryWallDecoration;
                tile.secondaryWallDecoration = null;
                return wallDecoration;
            }
        }
        return null;
    }

    public void drawTiles(boolean hideInteratives, byte[][][] settingBits, int lowestRenderedPlane, byte roofPreference) {
        int graphNodeIndex = hideInteratives ? 1 : 0;
        this.depthObjectCount = 0;
        this.layeredObjectCount = 0;
        this.tileDrawCycle++;
        for (SceneGraphEntity graphNode = this.renderObjects3d[graphNodeIndex]; graphNode != null; graphNode = graphNode.child) {
            separateFromFloor(graphNode);
            if (graphNode.floorDepth != -1 && !hideGameobject(graphNode, hideInteratives, settingBits, lowestRenderedPlane, roofPreference)) {
                this.depthRenderedEntities[++this.depthObjectCount - 1] = graphNode;
            }
        }
        for (SceneGraphEntity graphNode = this.renderObjects2d[graphNodeIndex]; graphNode != null; graphNode = graphNode.child) {
            separateFromFloor(graphNode);
            if (graphNode.floorDepth != -1 && !hideGameobject(graphNode, hideInteratives, settingBits, lowestRenderedPlane, roofPreference)) {
                this.priorityRenderedEntities[++this.layeredObjectCount - 1] = graphNode;
            }
        }
        for (SceneGraphEntity graphNode = this.dynamicRenderObjects[graphNodeIndex]; graphNode != null; graphNode = graphNode.child) {
            separateFromFloor(graphNode);
            if (graphNode.floorDepth != -1 && !hideGameobject(graphNode, hideInteratives, settingBits, lowestRenderedPlane, roofPreference)) {
                if (graphNode.hasAlpha()) {
                    this.priorityRenderedEntities[++this.layeredObjectCount - 1] = graphNode;
                } else {
                    this.depthRenderedEntities[++this.depthObjectCount - 1] = graphNode;
                }
            }
        }
        if (!hideInteratives) {
            for (int interactive = 0; interactive < this.interactiveObjectCount; interactive++) {
                separateFromFloor(this.interactiveObjects[interactive]);
                if (this.interactiveObjects[interactive].floorDepth != -1 && !hideGameobject(this.interactiveObjects[interactive], false, settingBits, lowestRenderedPlane, roofPreference)) {
                    if (this.interactiveObjects[interactive].hasAlpha()) {
                        this.priorityRenderedEntities[++this.layeredObjectCount - 1] = this.interactiveObjects[interactive];
                    } else {
                        this.depthRenderedEntities[++this.depthObjectCount - 1] = this.interactiveObjects[interactive];
                    }
                }
            }
        }
        /**
         * Renders general scene objects, walls
         */
        if (this.depthObjectCount > 0) {
            quicksortBelow(this.depthRenderedEntities, 0, this.depthObjectCount - 1);
            for (int objectIndex = 0; objectIndex < this.depthObjectCount; objectIndex++) {
                placeSceneryObjects(this.depthRenderedEntities[objectIndex], this.activeLights);
            }
        }
        if (this.copyLights) {
            this.activeToolkit.copyLights(0, null);
        }
        for (int z = 0; z < this.regionSizeZ; z++) {
            if (z >= lowestRenderedPlane && settingBits != null) {
            	int visibleSizeX = this.adjacent_tile_on_screen.length;
                if (this.adjacent_tile_on_screen.length + this.visibleToXnegative > this.sceneDimensionX) {
                    visibleSizeX -= this.adjacent_tile_on_screen.length + this.visibleToXnegative - this.sceneDimensionX;
                }
                int visibleSizeY = this.adjacent_tile_on_screen[0].length;
                if (this.adjacent_tile_on_screen[0].length + this.visibleToYnegative > this.sceneDimensionY) {
                    visibleSizeY -= this.adjacent_tile_on_screen[0].length + this.visibleToYnegative - this.sceneDimensionY;
                }
                if (this.calculateTilesOnScreen) {
                    for (int tileX = this.visibleToInvertedXnegative; tileX < visibleSizeX; tileX++) {
                    	int x = tileX + this.visibleToXnegative - this.visibleToInvertedXnegative;
                        for (int tileY = this.visibleToInvertedYnegative; tileY < visibleSizeY; tileY++) {
                        	this.tileOnScreen[tileX][tileY] = false;
                            if (this.adjacent_tile_on_screen[tileX][tileY]) {
                                int y = tileY + this.visibleToYnegative - this.visibleToInvertedYnegative;
                                for (int descendingPlane = z; descendingPlane >= 0; --descendingPlane) {
                                    if (this.tiles[descendingPlane][x][y] != null && this.tiles[descendingPlane][x][y].plane == z) {
                                        if ((descendingPlane < lowestRenderedPlane || settingBits[descendingPlane][x][y] != roofPreference) && !this.culling.isTileHidden(z, x, y)) {
                                        	this.tileOnScreen[tileX][tileY] = true;
                                            break;
                                        }
                                        this.tileOnScreen[tileX][tileY] = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                this.localHeightmap[z].renderFloor(this.cameraGridPositionX, this.cameraGridPositionY, this.visibleTiles, this.tileOnScreen, false);
            } else {
            	int adjacentTileRowCount = this.adjacent_tile_on_screen.length;
                if (this.adjacent_tile_on_screen.length + this.visibleToXnegative > this.sceneDimensionX) {
                    adjacentTileRowCount -= this.adjacent_tile_on_screen.length + this.visibleToXnegative - this.sceneDimensionX;
                }
                int adjacentTileColumnCount = this.adjacent_tile_on_screen[0].length;
                if (this.adjacent_tile_on_screen[0].length + this.visibleToYnegative > this.sceneDimensionY) {
                    adjacentTileColumnCount -= this.adjacent_tile_on_screen[0].length + this.visibleToYnegative - this.sceneDimensionY;
                }
                if (this.calculateTilesOnScreen) {
                    for (int tileX = this.visibleToInvertedXnegative; tileX < adjacentTileRowCount; tileX++) {
                    	int adjustedX = tileX + this.visibleToXnegative - this.visibleToInvertedXnegative;
                        for (int tileY = this.visibleToInvertedYnegative; tileY < adjacentTileColumnCount; tileY++) {
                        	this.tileOnScreen[tileX][tileY] = this.adjacent_tile_on_screen[tileX][tileY] && !this.culling.isTileHidden(z, adjustedX, tileY + this.visibleToYnegative - this.visibleToInvertedYnegative);
                        }
                    }
                }
                this.localHeightmap[z].renderFloor(this.cameraGridPositionX, this.cameraGridPositionY, this.visibleTiles, this.tileOnScreen, true);
            }
        }
        /**
         * Renders ground decoration, light rays, objects with particles, roof / next level decoration
         */
        if (this.layeredObjectCount > 0) {
            quicksortAbove(this.priorityRenderedEntities, 0, this.layeredObjectCount - 1);
            for (int index = 0; index < this.layeredObjectCount; index++) {
                placeSceneryObjects(this.priorityRenderedEntities[index], this.activeLights);
            }
        }
    }

    public void drawScene(int rotation, int cameraX, int cameraZ, int caneraY, byte[][][] bits,
                          int[] visibleBelowDrawheight, int[] visibleAboveX, int[] visibleBelowX,
                          int[] visibleBelowY, int[] visibleAboveY, int nextPlane, byte roofPreference,
                          boolean disableFlickeringEffects) {
        this.culling.drawingScene = true;
        this.calculateTilesOnScreen = true;
        this.cameraGridPositionX = cameraX >> this.tileUnits;
        this.cameraGridPositionY = caneraY >> this.tileUnits;
        this.cameraPositionX = cameraX;
        this.cameraPositionZ = caneraY;
        this.cameraPositionY = cameraZ;
        this.visibleToXnegative = this.cameraGridPositionX - this.visibleTiles;
        if (this.visibleToXnegative < 0) {
            this.visibleToInvertedXnegative = -(this.visibleToXnegative * 1607871809) * -1275764031;
            this.visibleToXnegative = 0;
        } else {
            this.visibleToInvertedXnegative = 0;
        }
        this.visibleToYnegative = this.cameraGridPositionY - this.visibleTiles;
        if (this.visibleToYnegative < 0) {
            this.visibleToInvertedYnegative = -(this.visibleToYnegative * 1831414049) * -2139776287;
            this.visibleToYnegative = 0;
        } else {
            this.visibleToInvertedYnegative = 0;
        }
        this.visibleToXpositive = this.visibleTiles + this.cameraGridPositionX;
        if (this.visibleToXpositive > this.sceneDimensionX) {
            this.visibleToXpositive = this.sceneDimensionX;
        }
        this.visibleToYpositive = this.visibleTiles + this.cameraGridPositionY;
        if (this.visibleToYpositive > this.sceneDimensionY) {
            this.visibleToYpositive = this.sceneDimensionY;
        }
        boolean[][] unadjustedAdjacentTilesOnScreen = this.adjacent_tile_on_screen;
        boolean[][] unadjustedTilesOnScreen = this.tileOnScreen;
        for (int x = 0; x < this.visibleTiles + this.visibleTiles + 2; x++) {
            int previousOnScreen = 0;
            int currentOnScreen = 0;
            for (int y = 0; y < this.visibleTiles + this.visibleTiles + 2; y++) {
                if (y > 1) {
                    this.tileVisibility[y - 2] = previousOnScreen;
                }
                previousOnScreen = currentOnScreen;
                int sceneX = x + (this.cameraGridPositionX - this.visibleTiles);
                int sceneY = y + (this.cameraGridPositionY - this.visibleTiles);
                if (sceneX >= 0 && sceneY >= 0 && sceneX < this.sceneDimensionX && sceneY < this.sceneDimensionY) {
                    int upscaledSceneX = sceneX << this.tileUnits;
                    int upscaledSceneY = sceneY << this.tileUnits;
                    int surfaceHeight = this.surfaceHeightmap[this.surfaceHeightmap.length - 1].getHeight(sceneX, sceneY) - (1000 << this.tileUnits - 7);
                    int waterHeight = (this.underwaterHeightmap != null ? this.underwaterHeightmap[0].getHeight(sceneX, sceneY) + this.fullTileDimension : this.surfaceHeightmap[0].getHeight(sceneX, sceneY) + this.fullTileDimension) + (1000 << this.tileUnits - 7);
                    currentOnScreen = this.activeToolkit.isOnScreen(upscaledSceneX, surfaceHeight, upscaledSceneY, upscaledSceneX, waterHeight, upscaledSceneY);
                    this.tileOnScreen[x][y] = currentOnScreen == 0;
                } else {
                    currentOnScreen = -1;
                    this.tileOnScreen[x][y] = false;
                }
                if (x > 0 && y > 0) {
                    int isOnscreen = this.tileVisibility[y - 1] & this.tileVisibility[y] & previousOnScreen & currentOnScreen;
                    this.adjacent_tile_on_screen[x - 1][y - 1] = isOnscreen == 0;
                }
            }
            this.tileVisibility[this.visibleTiles + this.visibleTiles] = previousOnScreen;
            this.tileVisibility[this.visibleTiles + this.visibleTiles + 1] = currentOnScreen;
        }
        this.culling.visibleBelowDrawheight = visibleBelowDrawheight;
        this.culling.visibleAboveX = visibleAboveX;
        this.culling.visibleBelowX = visibleBelowX;
        this.culling.visibleBelowY = visibleBelowY;
        this.culling.visibleAboveY = visibleAboveY;
        this.culling.processCulling(this.activeToolkit, nextPlane);
        ParticleSystemManager.processParticleCycle(this, this.activeToolkit);
        if (!this.interactiveEntities.removeDuplicates) {
            Iterator<RenderedEntity> interactivesIterator = this.interactiveEntities.entities.iterator();
            while (interactivesIterator.hasNext()) {
                RenderedEntity reference = interactivesIterator.next();
                interactivesIterator.remove();
                RenderedEntityCache.clear(reference);
            }
        }
        if (this.copyLights) {
            for (int slot = 0; slot < this.lightSlot; slot++) {
                this.lightSlotOccupant[slot].applyEffect(rotation, disableFlickeringEffects);
            }
        }
        if (this.waterTiles != null) {
            setUnderwater(true);
            this.activeToolkit.addWater(new WaterTile());
            drawTiles(true, bits, nextPlane, roofPreference);
            this.activeToolkit.onTilesDrawn();
            setUnderwater(false);
        }
        drawTiles(false, bits, nextPlane, roofPreference);
        if (!this.calculateTilesOnScreen) {
            this.adjacent_tile_on_screen = unadjustedAdjacentTilesOnScreen;
            this.tileOnScreen = unadjustedTilesOnScreen;
        }
    }

    public void castShadow(SceneGraphEntity rendered, int lowestZ, int startX, int startY, int width, int length) {
        boolean bool7 = true;
        int lowestX = startX;
        int highestX = startX + width;
        int lowestY = startY - 1;
        int highestY = startY + length;
        for (int z = lowestZ; z <= lowestZ + 1; z++) {
            if (z != this.regionSizeZ) {
                for (int x = lowestX; x <= highestX; x++) {
                    if (x >= 0 && x < this.sceneDimensionX) {
                        for (int y = lowestY; y <= highestY; y++) {
                            if (y >= 0 && y < this.sceneDimensionY && (!bool7 || x >= highestX || y >= highestY || y < startY && startX != x)) {
                                Tile tile = this.tiles[z][x][y];
                                if (tile != null) {
                                    int tileheightDifference =
                                    		(this.localHeightmap[z].getHeight(x, y) + this.localHeightmap[z].getHeight(1 + x, y)
                                    		+ this.localHeightmap[z].getHeight(x, 1 + y) + this.localHeightmap[z].getHeight(1 + x, 1 + y)) / 4
                                    		- (this.localHeightmap[lowestZ].getHeight(startX, startY) + this.localHeightmap[lowestZ].getHeight(startX + 1, startY)
                                    		+ this.localHeightmap[lowestZ].getHeight(startX, startY + 1) + this.localHeightmap[lowestZ].getHeight(startX + 1, startY + 1)) / 4;
                                    AbstractWall primaryWall = tile.primaryWall;
                                    AbstractWall secondaryWall = tile.secondaryWall;
                                    if (primaryWall != null && primaryWall.hasTileShadow()) {
                                        rendered.castShadow(primaryWall,
                                        		(x - startX) * this.fullTileDimension + (1 - width) * this.halfTileOffset, tileheightDifference,
                                        		(y - startY) * this.fullTileDimension + (1 - length) * this.halfTileOffset, bool7);
                                    }
                                    if (secondaryWall != null && secondaryWall.hasTileShadow()) {
                                        rendered.castShadow(secondaryWall,
                                        		(x - startX) * this.fullTileDimension + (1 - width) * this.halfTileOffset, tileheightDifference,
                                        		(y - startY) * this.fullTileDimension + (1 - length) * this.halfTileOffset, bool7);
                                    }
                                    for (InteractiveEntityContainer interactives = tile.interactives; interactives != null; interactives = interactives.child) {
                                        AbstractInteractiveEntity interactive = interactives.interactiveObject;
                                        if (interactive != null && interactive.hasTileShadow() && (x == interactive.startX || lowestX == x) && (y == interactive.startZ || lowestY == y)) {
                                            int sizeX = interactive.endX - interactive.startX + 1;
                                            int sizeZ = interactive.endZ - interactive.startZ + 1;
                                            rendered.castShadow(interactive,
                                            		this.fullTileDimension * (interactive.startX - startX) + this.halfTileOffset * (sizeX - width), tileheightDifference,
                                            		this.fullTileDimension * (interactive.startZ - startY) + this.halfTileOffset * (sizeZ - length), bool7);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                --lowestX;
                bool7 = false;
            }
        }
    }

    public Tile getTile(int plane, int x, int y) {
        if (this.tiles[plane][x][y] == null) {
            boolean isBridge = this.tiles[0][x][y] != null && this.tiles[0][x][y].bridge != null;
            if (isBridge && plane >= this.regionSizeZ - 1) {
                return null;
            }
            loadTiles(plane, x, y);
        }
        return this.tiles[plane][x][y];
    }

    public AbstractWall removeSecondaryWall(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        if (tile != null) {
            pushIntoStack(tile.secondaryWall);
            if (tile.secondaryWall != null) {
                AbstractWall wall = tile.secondaryWall;
                tile.secondaryWall = null;
                return wall;
            }
        }
        return null;
    }

    public int unusedOpcode21(int x, int y) {
        return this.opcode21 != null ? this.opcode21[x][y] & 0xff : 0;
    }

    /**
     * Disables lighting on objects if the light is off screen,
     * disabling this fixes some glitches 
     * EG: object is half onscreen but the light is not
     */
    public void quicksortBelow(SceneGraphEntity[] graphNodes, int startPosition, int endPosition) {
        if (startPosition < endPosition) {
            int center = (endPosition + startPosition) / 2;
            int curPosition = startPosition;
            SceneGraphEntity graphNode = graphNodes[center];
            graphNodes[center] = graphNodes[endPosition];
            graphNodes[endPosition] = graphNode;
            int offset = graphNode.floorDepth;
            for (int index = startPosition; index < endPosition; index++) {
                if (graphNodes[index].floorDepth < (index & 0x1) + offset) {
                    SceneGraphEntity node = graphNodes[index];
                    graphNodes[index] = graphNodes[curPosition];
                    graphNodes[curPosition++] = node;
                }
            }
            graphNodes[endPosition] = graphNodes[curPosition];
            graphNodes[curPosition] = graphNode;
            quicksortBelow(graphNodes, startPosition, curPosition - 1);
            quicksortBelow(graphNodes, curPosition + 1, endPosition);
        }
    }
    
    public void quicksortAbove(SceneGraphEntity[] graphNodes, int startPosition, int endPosition) {
        if (startPosition < endPosition) {
            int center = (endPosition + startPosition) / 2;
            int curPosition = startPosition;
            SceneGraphEntity graphNode = graphNodes[center];
            graphNodes[center] = graphNodes[endPosition];
            graphNodes[endPosition] = graphNode;
            int offset = graphNode.floorDepth;
            for (int index = startPosition; index < endPosition; index++) {
                if (graphNodes[index].floorDepth > (index & 0x1) + offset) {
                    SceneGraphEntity node = graphNodes[index];
                    graphNodes[index] = graphNodes[curPosition];
                    graphNodes[curPosition++] = node;
                }
            }
            graphNodes[endPosition] = graphNodes[curPosition];
            graphNodes[curPosition] = graphNode;
            quicksortAbove(graphNodes, startPosition, curPosition - 1);
            quicksortAbove(graphNodes, curPosition + 1, endPosition);
        }
    }

    public void addHdShadow(int renderUpToPlane) {
        Shadow shadow = null;
        for (int z = 1; z < renderUpToPlane; z++) {
            Ground ground = this.surfaceHeightmap[z];
            if (ground != null) {
                for (int y = 0; y < this.sceneDimensionY; y++) {
                    for (int x = 0; x < this.sceneDimensionX; x++) {
                        shadow = ground.createShadowAt(x, y, shadow);
                        if (shadow != null) {
                            int scaledX = x << this.tileUnits;
                            int scaledY = y << this.tileUnits;
                            for (int maxZ = z - 1; maxZ >= 0; --maxZ) {
                                Ground nextGround = this.surfaceHeightmap[maxZ];
                                if (nextGround != null) {
                                    int heightSw = ground.getHeight(x, y) - nextGround.getHeight(x, y);
                                    int heightSe = ground.getHeight(x + 1, y) - nextGround.getHeight(x + 1, y);
                                    int heightNe = ground.getHeight(x + 1, y + 1) - nextGround.getHeight(x + 1, y + 1);
                                    int heightNw = ground.getHeight(x, y + 1) - nextGround.getHeight(x, y + 1);
                                    nextGround.addShadow(shadow, scaledX, (heightNw + heightNe + heightSe + heightSw) / 4, scaledY);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public InteractiveEntityContainer getInteractiveObjects(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        return tile == null ? null : tile.interactives;
    }

    /**
     * Places any object/entity into the scene
     */
    public void placeSceneryObjects(SceneGraphEntity entity, PointLight[] lights) {
        if (this.copyLights) {
            int activeLights = entity.getActiveLights(lights);
            this.activeToolkit.copyLights(activeLights, lights);
        }
        if (this.underwaterHeightmap == this.localHeightmap) {
            Vector3 absolutePosition = entity.getAbsolutePosition().coords;
            int startX;
            int startY;
            if (entity instanceof AbstractInteractiveEntity) {
                startX = ((AbstractInteractiveEntity) entity).startX;
                startY = ((AbstractInteractiveEntity) entity).startZ;
            } else {
                startX = (int) absolutePosition.x >> this.tileUnits;
                startY = (int) absolutePosition.z >> this.tileUnits;
            }
            WaterTile waterTile = new WaterTile();
            waterTile.color = getColor(startX, startY);
            waterTile.scale = getScale(startX, startY);
            waterTile.intensity = getIntensity(startX, startY);
            waterTile.floOpcode20 = getWaterOpcode20(startX, startY);
            waterTile.floUnusedOpcode21 = unusedOpcode21(startX, startY);
            waterTile.floUnusedOpcode22 = unusedOpcode22(startX, startY);
            this.activeToolkit.addWaterTile(this.surfaceHeightmap[0].getAverageHeight((int) absolutePosition.x, (int) absolutePosition.z), waterTile);
        }
        RenderedEntity entityReference = entity.draw(this.activeToolkit);
        if (entityReference != null) {
            if (entityReference.interactive) {
                entityReference.entity = entity;
                this.interactiveEntities.add(entityReference);
            } else {
                RenderedEntityCache.clear(entityReference);
            }
        }
    }

    public AbstractWallDecoration getPrimaryWallDecoration(int z, int x, int y) {
        Tile tile = this.tiles[z][x][y];
        return tile == null ? null : tile.primaryWallDecoration;
    }

    public AbstractInteractiveEntity removeInteractiveOnCondition(int z, int x, int y, ComparableGraphNode comparable) {
        Tile tile = this.tiles[z][x][y];
        if (tile != null) {
            for (InteractiveEntityContainer interactives = tile.interactives; interactives != null; interactives = interactives.child) {
                AbstractInteractiveEntity interactive = interactives.interactiveObject;
                if ((comparable == null || comparable.matches(interactive)) && x == interactive.startX && y == interactive.startZ) {
                    removeInteractive(interactive, false);
                    return interactive;
                }
            }
        }
        return null;
    }

    public int getIntensity(int x, int y) {
        return this.intensities != null ? this.intensities[x][y] & 0xff : 0;
    }
}
