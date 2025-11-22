package com.jagex.game.runetek5.scene;

import java.util.Iterator;

import com.jagex.game.launch.Loader;
import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.impl.*;
import com.jagex.game.runetek5.client.*;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.window.ApplicationType;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.cutscene.CutsceneArea;
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList;
import com.jagex.game.runetek5.config.object.ObjectTypeCustomisation;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.config.mapsprite.MapSpriteTypeList;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.sequence.PositionedGfx;
import com.jagex.game.runetek5.config.sequence.Projectile;
import com.jagex.game.runetek5.config.staticelements.StaticElementTypeList;
import com.jagex.game.runetek5.config.staticelements.StaticElements;
import com.jagex.game.runetek5.entity.*;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.entity.gameobject.AnimatedGroundDecoration;
import com.jagex.game.runetek5.entity.gameobject.AnimatedInteractiveObject;
import com.jagex.game.runetek5.entity.gameobject.AnimatedWall;
import com.jagex.game.runetek5.entity.gameobject.AnimatedWallDecoration;
import com.jagex.game.runetek5.entity.grounditem.GroundItemUpdating;
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.entity.spotanimation.SpotAnimation;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.hint.HintArrow;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1;
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.math.Vector3Pool;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.network.PingRequester;
import com.jagex.game.runetek5.network.packet.ClientProt;
import com.jagex.game.runetek5.network.packet.PacketEncoder;
import com.jagex.game.runetek5.network.packet.TcpPacket;
import com.jagex.game.runetek5.rendering.MessageBox;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.environment.EnvironmentManager;
import com.jagex.game.runetek5.scene.tile.Tile;
import com.jagex.game.runetek5.sound.player.area.AmbientSound;
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.Utils;

public class MapRegion {

	/**
	 * The color behind the scene / the default background pixel color
	 */
	public static final int BACKGROUND_COLOR = ~0xffffff;
	
    public static boolean requiresClippingReset;
    public static boolean requiresDisplayResize;
    
    /**
     * Draws clipped tiles as pink on the minimap
     */
    public static boolean visualizeClippedTilesOnMinimap;
    public long debugRegionLoadtimeStart;
    public boolean debugRegionLoadtime;
    public boolean asyncMaploading;
    public RegionLoadingType previousLoadingType;
    public CoordGrid lastCoordgrid = new CoordGrid();
    public int[][] xteas;
    public int[] regionIds;
    public int[] surfaceMapArchives;
    public int[] surfaceLocArchives;
    public int[] npcSpawnArchives;
    public byte[][] surfaceMapBuffer;
    public byte[][] surfaceLocBuffer;
    public byte[][] npcSpawnBuffer;
    public int[] underwaterMapArchives;
    public int[] underwaterLocArchives;
    public byte[][] underwaterMapBuffer;
    public byte[][] underwaterLocBuffer;
    public RegionLoadingType currentLoadingType;
    public byte[][][] configuration;
    public ObjectTypeList loctypeList;
    public Scene scene;
    public RegionLoadingStage currentStage;
    public int missingMapCount;
    public int totalMapCount = 1;
    public int locsStillLoading;
    public int totalLocsLoading = 1;
    public EnvironmentManager environmentManager;
    public int viewRangeStart;
    public int viewRangeEnd;
    public int sceneSizeY;
    public int sceneSizeX;
    public int[][][] chunkBitpackedPosition;
    public int[][] drawPriority;
    /**
     * Amount of entities on one tile
     * sharing the highest draw priority
     */
    public int[][] drawPrioritySharedby;
    public SettingsBits tileSettingsBits;
    public MapSize activeMapSize;
    public int baseX;
    public int baseY;
    public CoordGrid baseCoordinates = new CoordGrid();
    public WorldMapElement worldMapType;
    public StaticElements staticElements;
    public SceneGraph underwaterMap;
    public SceneGraph surfaceMap;
    public DynamicTint dyanmicTint = new DynamicTint();
    public float randomColorOffset;
    public CollisionMap[] collisionMaps = new CollisionMap[4];
	public static final NodeCollection staticElementsAwaitingDrawing = new NodeCollection();
	public static int[][] CUTSCENE_MAP_XTEAS;

    public MapRegion(boolean loadScreensDisabled) {
        this.asyncMaploading = loadScreensDisabled;
    }
    
    public void initialize(RegionLoadRequest regionLoadRequest) {
        this.currentLoadingType = regionLoadRequest.loadingType;
        if (this.currentLoadingType == RegionLoadingType.ANIMATED_LOGIN_SCREEN) {
            initializeAnimatedLoginscreen();
        } else if (this.currentLoadingType == RegionLoadingType.STANDARD) {
            initializeRegion(regionLoadRequest.buffer);
        } else if (this.currentLoadingType == RegionLoadingType.CUTSCENE) {
            initializeCutscene();
        } else if (this.currentLoadingType.constructedMap()) {
            initializeConstructedMap(regionLoadRequest.buffer);
        }
    }
    
    public void initializeAnimatedLoginscreen() {
        setMapSize(MapSize.get(PreferenceManager.preferences.mapSize.getValue()));
        int baseX = this.baseCoordinates.x;
        int baseY = this.baseCoordinates.y;
        int mapBaseX = (Camera.cameraPositionAbsoluteX >> 12) + (baseX >> 3);
        int mapBaseY = (Camera.cameraPositionAbsoluteY >> 12) + (baseY >> 3);
        PlayerUpdate.MY_PLAYER.plane = 0;
        PlayerUpdate.MY_PLAYER_PLANE = 0;
        PlayerUpdate.MY_PLAYER.move(8, 8);
        byte numRegions = 18;
        this.xteas = new int[numRegions][4];
        this.regionIds = new int[numRegions];
        this.surfaceMapArchives = new int[numRegions];
        this.surfaceLocArchives = new int[numRegions];
        this.npcSpawnArchives = new int[numRegions];
        this.surfaceMapBuffer = new byte[numRegions][];
        this.surfaceLocBuffer = new byte[numRegions][];
        this.npcSpawnBuffer = new byte[numRegions][];
        this.underwaterMapArchives = new int[numRegions];
        this.underwaterLocArchives = new int[numRegions];
        this.underwaterMapBuffer = new byte[numRegions][];
        this.underwaterLocBuffer = new byte[numRegions][];
        int mapIndex = 0;
        for (int regionX = (mapBaseX - (this.sceneSizeX >> 4)) / 8; regionX <= (mapBaseX + (this.sceneSizeX >> 4)) / 8; regionX++) {
            for (int regionY = (mapBaseY - (this.sceneSizeY >> 4)) / 8; regionY <= (mapBaseY + (this.sceneSizeY >> 4)) / 8; regionY++) {
                int regionId = regionY + (regionX << 8);
                this.regionIds[mapIndex] = regionId;
                this.surfaceMapArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(true, false, regionX, regionY));
                this.surfaceLocArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(false, false, regionX, regionY));
                this.npcSpawnArchives[mapIndex] = Resource.MAPS.getArchiveId("n" + regionX + '_' + regionY);
                this.underwaterMapArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(true, true, regionX, regionY));
                this.underwaterLocArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(false, true, regionX, regionY));
                if (this.npcSpawnArchives[mapIndex] == -1) {
                    this.surfaceMapArchives[mapIndex] = -1;
                    this.surfaceLocArchives[mapIndex] = -1;
                    this.underwaterMapArchives[mapIndex] = -1;
                    this.underwaterLocArchives[mapIndex] = -1;
                }
                mapIndex++;
            }
        }
        for (int index = mapIndex; index < this.npcSpawnArchives.length; index++) {
            this.npcSpawnArchives[index] = -1;
            this.surfaceMapArchives[index] = -1;
            this.surfaceLocArchives[index] = -1;
            this.underwaterMapArchives[index] = -1;
            this.underwaterLocArchives[index] = -1;
        }
        GameState newGamestate;
        if (client.gameState == GameState.UNK_5) {
            newGamestate = GameState.UNK_11;
        } else if (client.gameState == GameState.UNK_7) {
            newGamestate = GameState.UNK_6;
        } else if (client.gameState == GameState.UNK_0) {
            newGamestate = GameState.UNK_2;
        } else if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
        	newGamestate = GameState.UNK_9;
        } else {
        	throw new RuntimeException("" + client.gameState);
        }
        performUpdate(mapBaseX, mapBaseY, newGamestate, false);
    }

    
    public void initializeRegion(Packet.Bit instream) {
        int mapsizeType = instream.readUByte();
        int chunkX = instream.readUShort();
        int chunkY = instream.readUShort();
        boolean positionUpdate = instream.readUByte() == 1;
        if (!this.asyncMaploading) {
            resetNonCutsceneEntities();
        }
        setMapSize(MapSize.get(mapsizeType));
        int regionCount = (instream.buffer.length - instream.currentPosition) / 16;
        this.xteas = new int[regionCount][4];
        int regionX;
        int regionY;
        for (regionX = 0; regionX < regionCount; regionX++) {
            for (regionY = 0; regionY < 4; regionY++) {
                this.xteas[regionX][regionY] = instream.readInt();
            }
        }
        this.regionIds = new int[regionCount];
        this.surfaceMapArchives = new int[regionCount];
        this.surfaceLocArchives = new int[regionCount];
        this.npcSpawnArchives = null;
        this.surfaceMapBuffer = new byte[regionCount][];
        this.surfaceLocBuffer = new byte[regionCount][];
        this.npcSpawnBuffer = null;
        this.underwaterMapArchives = new int[regionCount];
        this.underwaterLocArchives = new int[regionCount];
        this.underwaterMapBuffer = new byte[regionCount][];
        this.underwaterLocBuffer = new byte[regionCount][];
        regionCount = 0;
        for (regionX = (chunkX - (this.sceneSizeX >> 4)) / 8; regionX <= (chunkX + (this.sceneSizeX >> 4)) / 8; regionX++) {
            for (regionY = (chunkY - (this.sceneSizeY >> 4)) / 8; regionY <= (chunkY + (this.sceneSizeY >> 4)) / 8; regionY++) {
                this.regionIds[regionCount] = regionY + (regionX << 8);
                this.surfaceMapArchives[regionCount] = Resource.MAPS.getArchiveId(getArchiveName(true, false, regionX, regionY));
                this.surfaceLocArchives[regionCount] = Resource.MAPS.getArchiveId(getArchiveName(false, false, regionX, regionY));
                this.underwaterMapArchives[regionCount] = Resource.MAPS.getArchiveId(getArchiveName(true, true, regionX, regionY));
                this.underwaterLocArchives[regionCount] = Resource.MAPS.getArchiveId(getArchiveName(false, true, regionX, regionY));
                regionCount++;
            }
        }
        performUpdate(chunkX, chunkY, GameState.LOADING_PLEASE_WAIT, positionUpdate);
    }
    
    public void initializeCutscene() {
        this.previousLoadingType = this.currentLoadingType;
        setMapSize(MapSize.MAPSIZE_104_DEFAULT);
        for (int z = 0; z < 4; z++) {
            for (int chunkX = 0; chunkX < this.sceneSizeX >> 3; chunkX++) {
                for (int chunkY = 0; chunkY < this.sceneSizeY >> 3; chunkY++) {
                    this.chunkBitpackedPosition[z][chunkX][chunkY] = -1;
                }
            }
        }
        for (CutsceneArea cutsceneArea = (CutsceneArea) CutsceneTypeList.CUTSCENE_AREAS.getFirst(); cutsceneArea != null; cutsceneArea = (CutsceneArea) CutsceneTypeList.CUTSCENE_AREAS.next()) {
            int rotation = cutsceneArea.rotation;
            boolean isRotated = (rotation & 0x1) == 1;
            int chunkX = cutsceneArea.regionX >> 3;
            int chunkY = cutsceneArea.regionY >> 3;
            int cutsceneChunkXBase = cutsceneArea.anInt7480;
            int cutsceneChunkYBase = cutsceneArea.anInt7483;
            int z = cutsceneArea.plane;
            int cutscenePlane = cutsceneArea.cutscenePlane;
            int stepX = cutsceneArea.width;
            int stepY = cutsceneArea.length;
            int offsetY = 0;
            int offX = 0;
            byte offXStep = 1;
            byte offYStep = 1;
            if (rotation == 1) {
                offX = stepX - 1;
                offXStep = -1;
            } else if (rotation == 2) {
                offX = stepX - 1;
                offsetY = stepY - 1;
                offXStep = -1;
                offYStep = -1;
            } else if (rotation == 3) {
                offsetY = stepY - 1;
                offXStep = 1;
                offYStep = -1;
            }
            for (int y = chunkY; y < stepY + chunkY; offsetY += offYStep) {
                int offsetX = offX;
                for (int x = chunkX; x < stepX + chunkX; offsetX += offXStep) {
                    if (isRotated) {
                        this.chunkBitpackedPosition[cutscenePlane][cutsceneChunkXBase + offsetY][offsetX + cutsceneChunkYBase] = (x << 14) + (y << 3) + (z << 24) + (rotation << 1);
                    } else {
                        this.chunkBitpackedPosition[cutscenePlane][offsetX + cutsceneChunkXBase][cutsceneChunkYBase + offsetY] = (x << 14) + (y << 3) + (z << 24) + (rotation << 1);
                    }
                    x++;
                }
                y++;
            }
        }
        int mapIndex = MapRegion.CUTSCENE_MAP_XTEAS.length;
        this.regionIds = new int[mapIndex];
        this.surfaceMapArchives = new int[mapIndex];
        this.surfaceLocArchives = new int[mapIndex];
        this.npcSpawnArchives = null;
        this.surfaceMapBuffer = new byte[mapIndex][];
        this.surfaceLocBuffer = new byte[mapIndex][];
        this.npcSpawnBuffer = null;
        this.underwaterMapArchives = new int[mapIndex];
        this.underwaterLocArchives = new int[mapIndex];
        this.underwaterMapBuffer = new byte[mapIndex][];
        this.underwaterLocBuffer = new byte[mapIndex][];
        mapIndex = 0;
        for (CutsceneArea area = (CutsceneArea) CutsceneTypeList.CUTSCENE_AREAS.getFirst(); area != null; area = (CutsceneArea) CutsceneTypeList.CUTSCENE_AREAS.next()) {
        	int chunkX = area.regionX >>> 3;
            int chunkY = area.regionY >>> 3;
            int regionXEnd = chunkX + area.width;
            if ((regionXEnd & 0x7) == 0) {
                regionXEnd--;
            }
            regionXEnd >>>= 3;
            int regionYEnd = chunkY + area.length;
            if ((regionYEnd & 0x7) == 0) {
                regionYEnd--;
            }
            regionYEnd >>>= 3;
            for (int regionX = chunkX >>> 3; regionX <= regionXEnd; regionX++) {
                LoadMapArchives:
                for (int regionY = chunkY >>> 3; regionY <= regionYEnd; regionY++) {
                    int regionId = regionX << 8 | regionY;
                    for (int map = 0; map < mapIndex; map++) {
                        if (regionId == this.regionIds[map]) {
                            continue LoadMapArchives;
                        }
                    }
                    this.regionIds[mapIndex] = regionId;
                    this.surfaceMapArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(true, false, regionX, regionY));
                    this.surfaceLocArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(false, false, regionX, regionY));
                    this.underwaterMapArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(true, true, regionX, regionY));
                    this.underwaterLocArchives[mapIndex] = Resource.MAPS.getArchiveId(getArchiveName(false, true, regionX, regionY));
                    mapIndex++;
                }
            }
        }
        this.xteas = MapRegion.CUTSCENE_MAP_XTEAS;
        MapRegion.CUTSCENE_MAP_XTEAS = null;
        performUpdate(this.sceneSizeX >> 4, this.sceneSizeY >> 4, GameState.LOADING_PLEASE_WAIT, false);
    }
    
    public void initializeConstructedMap(Packet.Bit stream) {
        int regionUpdateMask = stream.readUByteInverse();
        boolean forcedPositionUpdate = (regionUpdateMask & 0x1) != 0;
        int type = stream.readUByteSubtract();
        if (type == 1) {
            this.currentLoadingType = RegionLoadingType.INSTANCE;
        } else if (type == 2) {
            this.currentLoadingType = RegionLoadingType.INSTANCE_NO_ENTITY_RESET;
        } else if (type == 3) {
            this.currentLoadingType = RegionLoadingType.INSTANCE_LARGE;
        } else if (type == 4) {
            this.currentLoadingType = RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES;
        }
        int mapSizeType = stream.readUByteAdd();
        int mapBaseY = stream.readUShortAdd();
        int mapBaseX = stream.readUShort();
        if (!this.asyncMaploading) {
            resetNonCutsceneEntities();
        }
        setMapSize(MapSize.get(mapSizeType));
        stream.initBitAccess();
        for (int chunkPlane = 0; chunkPlane < 4; chunkPlane++) {
            for (int chunkIndexX = 0; chunkIndexX < this.sceneSizeX >> 3; chunkIndexX++) {
                for (int chunkIndexY = 0; chunkIndexY < this.sceneSizeY >> 3; chunkIndexY++) {
                    int hasChunkFlags = stream.readBits(1);
                    if (hasChunkFlags == 1) {
                        this.chunkBitpackedPosition[chunkPlane][chunkIndexX][chunkIndexY] = stream.readBits(26);
                    } else {
                        this.chunkBitpackedPosition[chunkPlane][chunkIndexX][chunkIndexY] = -1;
                    }
                }
            }
        }
        stream.finishBitAccess();
        int mapCount = (stream.buffer.length - stream.currentPosition) / 16;
        this.xteas = new int[mapCount][4];
        for (int region = 0; region < mapCount; region++) {
            for (int map = 0; map < 4; map++) {
                this.xteas[region][map] = stream.readInt();
            }
        }
        this.regionIds = new int[mapCount];
        this.surfaceMapArchives = new int[mapCount];
        this.surfaceLocArchives = new int[mapCount];
        this.npcSpawnArchives = null;
        this.surfaceMapBuffer = new byte[mapCount][];
        this.surfaceLocBuffer = new byte[mapCount][];
        this.npcSpawnBuffer = null;
        this.underwaterMapArchives = new int[mapCount];
        this.underwaterLocArchives = new int[mapCount];
        this.underwaterMapBuffer = new byte[mapCount][];
        this.underwaterLocBuffer = new byte[mapCount][];
        mapCount = 0;
        for (int chunkPlane = 0; chunkPlane < 4; chunkPlane++) {
            for (int chunkIndexX = 0; chunkIndexX < this.sceneSizeX >> 3; chunkIndexX++) {
                for (int chunkIndexY = 0; chunkIndexY < this.sceneSizeY >> 3; chunkIndexY++) {
                    int bitpackedPosition = this.chunkBitpackedPosition[chunkPlane][chunkIndexX][chunkIndexY];
                    if (bitpackedPosition != -1) {
                        int regionX = bitpackedPosition >> 14 & 0x3ff;
                        int regionY = bitpackedPosition >> 3 & 0x7ff;
                        int regionId = (regionX / 8 << 8) + regionY / 8;
                        for (int index = 0; index < mapCount; index++) {
                            if (regionId == this.regionIds[index]) {
                                regionId = -1;
                                break;
                            }
                        }
                        if (regionId != -1) {
                            this.regionIds[mapCount] = regionId;
                            int RegionX = regionId >> 8 & 0xff;
                            int RegionY = regionId & 0xff;
                            this.surfaceMapArchives[mapCount] = Resource.MAPS.getArchiveId(getArchiveName(true, false, RegionX, RegionY));
                            this.surfaceLocArchives[mapCount] = Resource.MAPS.getArchiveId(getArchiveName(false, false, RegionX, RegionY));
                            this.underwaterMapArchives[mapCount] = Resource.MAPS.getArchiveId(getArchiveName(true, true, RegionX, RegionY));
                            this.underwaterLocArchives[mapCount] = Resource.MAPS.getArchiveId(getArchiveName(false, true, RegionX, RegionY));
                            mapCount++;
                        }
                    }
                }
            }
        }
        performUpdate(mapBaseX, mapBaseY, GameState.LOADING_PLEASE_WAIT, forcedPositionUpdate);
    }
    
    public void buildRegionBase(MapRegion mapRegion) {
        boolean multiThread = mapRegion.asyncMaploading;
        mapRegion.asyncMaploading = this.asyncMaploading;
        this.asyncMaploading = multiThread;
        RegionLoadingType loadingType = mapRegion.previousLoadingType;
        mapRegion.previousLoadingType = this.previousLoadingType;
        this.previousLoadingType = loadingType;
        mapRegion.lastCoordgrid = this.baseCoordinates;
        this.lastCoordgrid = mapRegion.baseCoordinates;
        this.environmentManager.buildEnvironment(mapRegion.getEnvironmentManager());
    }
    
    public boolean loadRegion() {
        if (!this.asyncMaploading) {
            PingRequester.keepalive(false);
        }
        this.missingMapCount = 0;
        for (int index = 0; index < this.surfaceMapBuffer.length; index++) {
            if (this.surfaceMapArchives[index] != -1 && this.surfaceMapBuffer[index] == null) {
                this.surfaceMapBuffer[index] = Resource.MAPS.getFile(this.surfaceMapArchives[index], 0);
                if (this.surfaceMapBuffer[index] == null) {
                    this.missingMapCount++;
                }
            }
            if (this.surfaceLocArchives[index] != -1 && this.surfaceLocBuffer[index] == null) {
                this.surfaceLocBuffer[index] = Resource.MAPS.getFile(this.surfaceLocArchives[index], 0, this.xteas[index]);
                if (this.surfaceLocBuffer[index] == null) {
                	this.missingMapCount++;
                }
            }
            
            if (this.underwaterMapArchives[index] != -1 && this.underwaterMapBuffer[index] == null) {
                this.underwaterMapBuffer[index] = Resource.MAPS.getFile(this.underwaterMapArchives[index], 0);
                if (this.underwaterMapBuffer[index] == null) {
                	this.missingMapCount++;
                }
            }

            if (this.underwaterLocArchives[index] != -1 && this.underwaterLocBuffer[index] == null) {
                this.underwaterLocBuffer[index] = Resource.MAPS.getFile(this.underwaterLocArchives[index], 0);
                if (this.underwaterLocBuffer[index] == null) {
                	this.missingMapCount++;
                }
            }
            if (this.npcSpawnArchives != null && this.npcSpawnBuffer[index] == null && this.npcSpawnArchives[index] != -1) {
                this.npcSpawnBuffer[index] = Resource.MAPS.getFile(this.npcSpawnArchives[index], 0, this.xteas[index]);
                if (this.npcSpawnBuffer[index] == null) {
                	this.missingMapCount++;
                }
            }
        }
        if (this.staticElements == null) {
            if (this.worldMapType != null && Resource.WORLD_MAP.validFile(this.worldMapType.filenamePrefix + "_staticelements")) {
                if (!Resource.WORLD_MAP.isCached(this.worldMapType.filenamePrefix + "_staticelements")) {
                    this.missingMapCount++;
                } else {
                    this.staticElements = StaticElementTypeList.list(Resource.WORLD_MAP, this.worldMapType.filenamePrefix + "_staticelements", client.memberWorld);
                }
            } else {
                this.staticElements = new StaticElements(0);
            }
        }
        if (this.missingMapCount > 0) {
            if (this.totalMapCount < this.missingMapCount) {
                this.totalMapCount = this.missingMapCount;
            }
            this.currentStage = RegionLoadingStage.LOADING_MAPS;
            return false;
        }
        System.out.println("LOCS STILL LOADING");
        this.locsStillLoading = 0;
        for (int mapPtr = 0; mapPtr < this.surfaceMapBuffer.length; mapPtr++) {
            byte[] mapdata = this.surfaceLocBuffer[mapPtr];
            if (mapdata != null) {
                int x = (this.regionIds[mapPtr] >> 8) * 64 - this.baseCoordinates.x;
                int y = (this.regionIds[mapPtr] & 0xff) * 64 - this.baseCoordinates.y;
                if (this.currentLoadingType.constructedMap()) {
                    x = 10;
                    y = 10;
                }
                int incompleteLocs = SceneGraph.loadLocs(this.loctypeList, mapdata, x, y, this.sceneSizeX, this.sceneSizeY);
                if (incompleteLocs > 0) {
                    System.out.println("surfaceLocBuffer: " + incompleteLocs);
                    this.locsStillLoading += incompleteLocs;
                }
            }
            mapdata = this.underwaterLocBuffer[mapPtr];
            if (mapdata != null) {
            	int x = (this.regionIds[mapPtr] >> 8) * 64 - this.baseCoordinates.x;
            	int y = (this.regionIds[mapPtr] & 0xff) * 64 - this.baseCoordinates.y;
                if (this.currentLoadingType.constructedMap()) {
                    x = 10;
                    y = 10;
                }
                int incompleteLocs = SceneGraph.loadLocs(this.loctypeList, mapdata, x, y, this.sceneSizeX, this.sceneSizeY);
                if (incompleteLocs > 0) {
                    System.out.println("underwaterLocBuffer: " + incompleteLocs);
                    this.locsStillLoading += incompleteLocs;
                }
            }
        }
        if (this.locsStillLoading > 0) {
            System.out.println("[MapRegion] Locs still loading: " + this.locsStillLoading + "/" + this.totalLocsLoading);
            if (this.totalLocsLoading < this.locsStillLoading) {
                this.totalLocsLoading = this.locsStillLoading;
            }
            this.currentStage = RegionLoadingStage.LOADING_LOCS;
            return false;
        } 
        if (!this.asyncMaploading && this.currentStage != RegionLoadingStage.LOADING) {
            MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(client.CURRENT_LANGUAGE) + "<br>" + "(100%)", true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
        }
        this.currentStage = RegionLoadingStage.PREPARE_DISPLAY;
        if (!this.asyncMaploading) {
            SoundEffectPlayer.updatePlayers(); // instanced in 910
        }
        if (!this.asyncMaploading) {
            for (int index = 0; index < 2048; index++) {
                PlayerEntity playerEntity = PlayerUpdate.players[index];
                if (playerEntity != null) {
                    playerEntity.scene = null;
                }
            }
            for (int index = 0; index < client.NPC_ARRAY.length; index++) {
                ObjectNode npcObjectNode = client.NPC_ARRAY[index];
                if (npcObjectNode != null) {
                    ((SceneGraphEntity) npcObjectNode.value).scene = null;
                }
            }
        }
        if (!this.asyncMaploading) {
            TypeLists.resetCachedData(true);
        }
        if (!this.asyncMaploading) {
            StaticMethods.method4250(); // not in 910
        }
        boolean hasUnderwaterMap = false;
        if (PreferenceManager.preferences.water.getValue() == 2) {
            for (int map = 0; map < this.surfaceMapBuffer.length; map++) {
                if (this.underwaterLocBuffer[map] != null || this.underwaterMapBuffer[map] != null) {
                    hasUnderwaterMap = true;
                    break;
                }
            }
        }
        int buildAreaChunks = BuildArea.parse(PreferenceManager.preferences.buildArea.getValue()).chunkSize * 8 * Loader.RENDER_DISTANCE_MULTIPLIER;
        if (Renderers.activeToolkit.extendBuildArea()) {
            buildAreaChunks++;
        }
        calculateViewRange();
        clear();
        this.scene = new Scene(Renderers.activeToolkit, this.sceneSizeX, this.sceneSizeY, buildAreaChunks, hasUnderwaterMap, Renderers.activeToolkit.getMaxLights() > 0);
        this.scene.setUnderwater(false);
        this.scene.empty1();
        if (client.sceneDummyOp != 0) {
            this.scene.empty2();
        } else {
            this.scene.empty2();
        }
        this.dyanmicTint = new DynamicTint();
        this.randomColorOffset = -0.05f + (float) (Math.random() / 10.0D);
        this.surfaceMap = new SceneGraph(this.scene, this.loctypeList, 4, this.sceneSizeX, this.sceneSizeY, false, this.tileSettingsBits, this.environmentManager);
        this.surfaceMap.enableLdShadows();
        this.surfaceMap.hdShadows = PreferenceManager.preferences.sceneryShadows.getValue();
        this.surfaceMap.hdWater = PreferenceManager.preferences.water.getValue() == 2;
        this.surfaceMap.hdLight = PreferenceManager.preferences.lightDetail.getValue() == 1;
        this.surfaceMap.blendFloor = PreferenceManager.preferences.goundBlending.getValue() == 1;
        this.surfaceMap.enableTextures = PreferenceManager.preferences.textures.getValue() == 1;
        if (!this.currentLoadingType.constructedMap()) {
            decodeClientUnderlayMasks(this.surfaceMap, this.surfaceMapBuffer);
        } else {
            decodeServerUnderlayMasks(this.surfaceMap, this.surfaceMapBuffer);
        }
        if (this.asyncMaploading) {
            sleep(50);
        }
        //ADDED
        if (this.asyncMaploading) {
            Utils.time();
            while (!Renderers.activeToolkit.readyForRendering()) {
                System.out.println("Waiting for renderer");
                sleep(1);
            }
        }
        this.environmentManager.initializeEnvironment(this.sceneSizeX >> 4, this.sceneSizeY >> 4);
        this.environmentManager.updateAtmosphere(this);
        if (hasUnderwaterMap) {
            this.scene.setUnderwater(true);
            this.underwaterMap = new SceneGraph(this.scene, this.loctypeList, 1, this.sceneSizeX, this.sceneSizeY, true, this.tileSettingsBits, this.environmentManager);
            this.underwaterMap.enableLdShadows();
            this.underwaterMap.hdShadows = PreferenceManager.preferences.sceneryShadows.getValue();
            this.underwaterMap.hdWater = PreferenceManager.preferences.water.getValue() == 2;
            this.underwaterMap.hdLight = PreferenceManager.preferences.lightDetail.getValue() == 1;
            this.underwaterMap.blendFloor = PreferenceManager.preferences.goundBlending.getValue() == 1;
            this.underwaterMap.enableTextures = PreferenceManager.preferences.textures.getValue() == 1;
            if (!this.currentLoadingType.constructedMap()) {
                decodeClientUnderlayMasks(this.underwaterMap, this.underwaterMapBuffer);
                if (!this.asyncMaploading) {
                    PingRequester.keepalive(true);
                }
            } else {
                decodeServerUnderlayMasks(this.underwaterMap, this.underwaterMapBuffer);
                if (!this.asyncMaploading) {
                    PingRequester.keepalive(true);
                }
            }
            this.underwaterMap.offsetTileHeights(this.surfaceMap.tileHeights[0]);
            this.underwaterMap.initializeFloor(Renderers.activeToolkit, null, null);
            this.scene.setUnderwater(false);
            if (this.asyncMaploading) {
                sleep(50);
            }
        }
        this.surfaceMap.initializeFloor(Renderers.activeToolkit, hasUnderwaterMap ? this.underwaterMap.tileHeights : null, this.collisionMaps);
        if (!this.currentLoadingType.constructedMap()) {
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true);
            }
            decodeLocMapsClient(this.surfaceMap, this.surfaceLocBuffer);
            if (this.npcSpawnBuffer != null) {
                decodeNpcSpawns();
            }
        } else {
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true);
            }
            decodeLocMapsServer(this.surfaceMap, this.surfaceLocBuffer);
        }
        if (!this.asyncMaploading) {
            PingRequester.keepalive(true);
        }
        if (this.asyncMaploading) {
            Utils.time();
            while (!Renderers.activeToolkit.readyForRendering()) {
                sleep(1);
            }
        }
        this.surfaceMap.calculateUnderlayPalette(Renderers.activeToolkit, hasUnderwaterMap ? this.scene.underwaterHeightmap[0] : null, null);
        if (this.asyncMaploading) {
            sleep(75);
        }
        this.surfaceMap.calculateVisibility(false);
        if (this.asyncMaploading) {
            sleep(75);
        }
        if (!this.asyncMaploading) {
            PingRequester.keepalive(true);
        }
        if (hasUnderwaterMap) {
            this.scene.setUnderwater(true);
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true);
            }
            if (!this.currentLoadingType.constructedMap()) {
                decodeLocMapsClient(this.underwaterMap, this.underwaterLocBuffer);
            } else {
                decodeLocMapsServer(this.underwaterMap, this.underwaterLocBuffer);
            }
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true);
            }
            //ADDED
            if (this.asyncMaploading) {
                Utils.time();
                while (!Renderers.activeToolkit.readyForRendering()) {
                    sleep(1);
                }
            }
            this.underwaterMap.calculateUnderlayPalette(Renderers.activeToolkit, null, this.scene.surfaceHeightmap[0]);
            this.underwaterMap.calculateVisibility(true);
            if (!this.asyncMaploading) {
                PingRequester.keepalive(true);
            }
            this.scene.setUnderwater(false);
            if (this.asyncMaploading) {
                sleep(50);
            }
        }
        this.surfaceMap.resetUnderlays();
        if (this.underwaterMap != null) {
            this.underwaterMap.resetUnderlays();
        }
        this.scene.resetSurfaceTiles();
        if (this.asyncMaploading) {
            Utils.time();
            while (!Renderers.activeToolkit.readyForRendering()) {
                sleep(1);
            }
        }
        boolean async = false;
        if (this.asyncMaploading) {
            MapRegion mapRegion = Resource.mapRegion;
            buildRegionBase(mapRegion);
            Resource.mapRegionLoaderTask.setMapRegion(mapRegion);
            async = true;
            Utils.time();
            synchronized (client.ASYNC_MAPLOADING_LOCK) {
                client.maploadingLockTaken = true;
                try {
                    client.ASYNC_MAPLOADING_LOCK.wait();
                } catch (InterruptedException interruptedException) {
                	// NOOP
                }
            }
            Resource.mapRegion = this;
            mapRegion.clear();
            updateEntities(GameState.LOADING_PLEASE_WAIT);
            resetNonCutsceneEntities();
        } else {
            SoundEffectPlayer.updatePlayers();
            this.environmentManager.calculateFog();
            GameShell.resetDrawCache();
        }
        for (int plane = 0; plane < 4; plane++) {
            for (int sceneX = 0; sceneX < this.sceneSizeX; sceneX++) {
                for (int sceneY = 0; sceneY < this.sceneSizeY; sceneY++) {
                    GroundItemUpdating.removeGroundItem(plane, sceneX, sceneY);
                }
            }
        }
        for (int index = 0; index < client.HINT_TRAILS.length; index++) {
            if (client.HINT_TRAILS[index] != null) {
                client.HINT_TRAILS[index].addTo(this.scene);
            }
        }
        Scene.setRoofOverride();
        LocAction.onRegionChanged();
        if (GameShell.getApplicationType() == ApplicationType.APPLET && client.GAME_CONNECTION_CONTEXT.getConnection() != null && client.gameState == GameState.LOADING_PLEASE_WAIT) {
        	TcpPacket packet = PacketEncoder.createPacket(ClientProt.UNK_82, client.GAME_CONNECTION_CONTEXT.isaac);
            packet.buffer.writeInt(1057001181);
            client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
        }
        if (!this.currentLoadingType.constructedMap()) {
        	int regionStartX = (this.baseX - (this.sceneSizeX >> 4)) / 8;
        	int regionEndX = (this.baseX + (this.sceneSizeX >> 4)) / 8;
            int regionStartY = (this.baseY - (this.sceneSizeY >> 4)) / 8;
            int regionEndY = (this.baseY + (this.sceneSizeY >> 4)) / 8;
            for (int regionX = regionStartX - 1; regionX <= regionEndX + 1; regionX++) {
                for (int regionY = regionStartY - 1; regionY <= regionEndY + 1; regionY++) {
                    if (regionX < regionStartX || regionX > regionEndX || regionY < regionStartY || regionY > regionEndY) {
                        Resource.MAPS.prefetch(getArchiveName(true, false, regionX, regionY));
                        Resource.MAPS.prefetch(getArchiveName(false, false, regionX, regionY));
                        Resource.MAPS.prefetch(getArchiveName(true, true, regionX, regionY));
                        Resource.MAPS.prefetch(getArchiveName(false, true, regionX, regionY));
                    }
                }
            }
        }
        if (client.gameState == GameState.UNK_11) {
            client.setGameState(GameState.UNK_5);
        } else if (client.gameState == GameState.UNK_2) {
            client.setGameState(GameState.UNK_0);
        } else if (client.gameState == GameState.UNK_6) {
            client.setGameState(GameState.UNK_7);
        } else if (client.gameState == GameState.UNK_9) {
            client.setGameState(GameState.IN_ACCOUNT_CREATION);
        } else {
            client.setGameState(GameState.LOGGED_INGAME);
            if (client.GAME_CONNECTION_CONTEXT.getConnection() != null) {
            	TcpPacket packet = PacketEncoder.createPacket(ClientProt.REGION_LOADED_CONFIRM, client.GAME_CONNECTION_CONTEXT.isaac);
                client.GAME_CONNECTION_CONTEXT.queuePacket(packet);
            }
        }
        WorldMapSub1.reset();
        if (this.debugRegionLoadtime) {
            Console.writeLine(Long.toString(Utils.time() - this.debugRegionLoadtimeStart));
            this.debugRegionLoadtime = false;
        }
        if (async) {
            synchronized (client.MAP_REGION_LOADER_LOCK) {
                client.MAP_REGION_LOADER_LOCK.notify();
            }
        }
        return true;           
    }

    public static int tileheightForAbsolutePosition(int absoluteX, int absoluteY, int plane) {
        if (Resource.mapRegion.getScene() == null) {
            return 0;
        } 
        int sceneX = absoluteX >> 9;
        int sceneY = absoluteY >> 9;
        if (sceneX >= 0 && sceneY >= 0 && sceneX <= Resource.mapRegion.getSceneSizeX() - 1 && sceneY <= Resource.mapRegion.getSceneSizeY() - 1) {
            int collisionPlane = plane;
            if (plane < 3 && (Resource.mapRegion.getSettingsBits().tileFunctionMasks[1][sceneX][sceneY] & 0x2) != 0) {
                collisionPlane = plane + 1;
            }
            return Resource.mapRegion.getScene().localHeightmap[collisionPlane].getAverageHeight(absoluteX, absoluteY);
        } 
        return 0;
    }
    
    public static boolean isMapSpriteCached(int z, int x, int y) {
        Scene scene = Resource.mapRegion.getScene();
        boolean fullyCached = true;
        GameObject gameObject = (GameObject) scene.getWall(z, x, y);
        if (gameObject != null) {
            fullyCached &= MapSpriteTypeList.isCached(gameObject);
        }
        gameObject = (GameObject) scene.getInteractiveObject(z, x, y, client.INTERACTIVE_OBJECT_COMPARABLE);
        if (gameObject != null) {
            fullyCached &= MapSpriteTypeList.isCached(gameObject);
        }
        gameObject = (GameObject) scene.getGroundDecoration(z, x, y);
        if (gameObject != null) {
            fullyCached &= MapSpriteTypeList.isCached(gameObject);
        }
        return fullyCached;
    }

    public static void handleRoofDisplay() {
        if (PreferenceManager.preferences.removeRoofsOptionOverride.getValue() == 2) {
            byte[][][] settingsBits = Resource.mapRegion.getBits();
            byte b_2 = (byte) (client.timesSceneRendered - 4 & 0xff);
            int x = client.timesSceneRendered % Resource.mapRegion.getSceneSizeX();
            for (int plane = 0; plane < 4; plane++) {
                for (int y = 0; y < Resource.mapRegion.getSceneSizeY(); y++) {
                    settingsBits[plane][x][y] = b_2;
                }
            }
            if (PlayerUpdate.MY_PLAYER_PLANE != 3) {
                for (int index = 0; index < 2; index++) { 
                    client.visibleBelowDrawheight[index] = -1000000;
                    client.visibleAboveX[index] = 1000000;
                    client.visibleBelowX[index] = 0;
                    client.visibleAboveY[index] = 1000000;
                    client.visibleBelowY[index] = 0;
                }

                Vector3 absolutePosition = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
                int absoluteX = (int) absolutePosition.x;
                int absoluteZ = (int) absolutePosition.z;
                SettingsBits regionMap = Resource.mapRegion.getSettingsBits();
                Scene scene = Resource.mapRegion.getScene();
                if (Camera.cameraCalculationType != 2 && PingRequester.anInt5864 == -1) {
                	int tileheight = tileheightForAbsolutePosition(Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteY, PlayerUpdate.MY_PLAYER_PLANE);
                    if (tileheight - Camera.cameraPositionAbsoluteZ < 3200 &&
                    		(regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][Camera.cameraPositionAbsoluteX >> 9][Camera.cameraPositionAbsoluteY >> 9] & 0x4) != 0) {
                        MapRegion.calculateVisibilityAdjustments(scene.tiles, 1, Camera.cameraPositionAbsoluteX >> 9, Camera.cameraPositionAbsoluteY >> 9, false);
                    }
                } else {
                    if (Camera.cameraCalculationType != 2) {
                        absoluteX = PingRequester.anInt5864;
                        absoluteZ = StaticMethods.anInt833;
                    }

                    if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][absoluteX >> 9][absoluteZ >> 9] & 0x4) != 0) {
                        MapRegion.calculateVisibilityAdjustments(scene.tiles, 0, absoluteX >> 9, absoluteZ >> 9, false);
                    } else if (Camera.cameraYaw < 2560) {
                    	int cameraBaseX = Camera.cameraPositionAbsoluteX >> 9;
                        int cameraBaseZ = Camera.cameraPositionAbsoluteY >> 9;
                        int baseX = absoluteX >> 9;
                        int baseZ = absoluteZ >> 9;
            
                        int cameraBaseXOffset;
                        if (baseX > cameraBaseX) {
                            cameraBaseXOffset = baseX - cameraBaseX;
                        } else {
                            cameraBaseXOffset = cameraBaseX - baseX;
                        }
                        
                        int cameraBaseZOffset;
                        if (baseZ > cameraBaseZ) {
                            cameraBaseZOffset = baseZ - cameraBaseZ;
                        } else {
                            cameraBaseZOffset = cameraBaseZ - baseZ;
                        }

                        if ((cameraBaseXOffset != 0 || cameraBaseZOffset != 0) && cameraBaseXOffset > -Resource.mapRegion.getSceneSizeX() && cameraBaseXOffset < Resource.mapRegion.getSceneSizeX() && cameraBaseZOffset > -Resource.mapRegion.getSceneSizeY() && cameraBaseZOffset < Resource.mapRegion.getSceneSizeY()) {
                            int i_16;
                            int i_18;
                            if (cameraBaseXOffset > cameraBaseZOffset) {
                                i_18 = cameraBaseZOffset * 65536 / cameraBaseXOffset;
                                i_16 = 32768;

                                while (cameraBaseX != baseX) {
                                    if (cameraBaseX < baseX) {
                                        ++cameraBaseX;
                                    } else {
                                        --cameraBaseX;
                                    }

                                    if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ] & 0x4) != 0) {
                                        MapRegion.calculateVisibilityAdjustments(scene.tiles, 1, cameraBaseX, cameraBaseZ, false);
                                        break;
                                    }

                                    i_16 += i_18;
                                    if (i_16 >= 65536) {
                                        i_16 -= 65536;
                                        if (cameraBaseZ < baseZ) {
                                            ++cameraBaseZ;
                                        } else if (cameraBaseZ > baseZ) {
                                            --cameraBaseZ;
                                        }

                                        if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ] & 0x4) != 0) {
                                            MapRegion.calculateVisibilityAdjustments(scene.tiles, 1, cameraBaseX, cameraBaseZ, false);
                                            break;
                                        }
                                    }
                                }
                            } else {
                                i_18 = cameraBaseXOffset * 65536 / cameraBaseZOffset;
                                i_16 = 32768;

                                while (baseZ != cameraBaseZ) {
                                    if (cameraBaseZ < baseZ) {
                                        ++cameraBaseZ;
                                    } else {
                                        --cameraBaseZ;
                                    }

                                    if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ] & 0x4) != 0) {
                                        MapRegion.calculateVisibilityAdjustments(scene.tiles, 1, cameraBaseX, cameraBaseZ, false);
                                        break;
                                    }

                                    i_16 += i_18;
                                    if (i_16 >= 65536) {
                                        i_16 -= 65536;
                                        if (cameraBaseX < baseX) {
                                            ++cameraBaseX;
                                        } else if (cameraBaseX > baseX) {
                                            --cameraBaseX;
                                        }

                                        if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][cameraBaseX][cameraBaseZ] & 0x4) != 0) {
                                            MapRegion.calculateVisibilityAdjustments(scene.tiles, 1, cameraBaseX, cameraBaseZ, false);
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            CoordGrid grid = Resource.mapRegion.getBaseCoordinates();
                            PacketEncoder.sendErrorToServer(cameraBaseX + "," + cameraBaseZ + " " + baseX + "," + baseZ + " " + grid.x + "," + grid.y, new RuntimeException());
                        }
                    }
                }
            }
        }

    }

    public static void addCustomisation(int z, int type, int x, int y, ObjectTypeCustomisation locCustomisation) {
        if (x >= 1 && y >= 1 && x <= Resource.mapRegion.getSceneSizeX() - 2 && y <= Resource.mapRegion.getSceneSizeY() - 2 && Resource.mapRegion.getScene() != null) {
            GameObject gameObject = Resource.mapRegion.getSurfaceSceneGraph().getGameObject(z, type, x, y);
            if (gameObject != null) {
                if (gameObject instanceof AnimatedInteractiveObject) {
                    ((AnimatedInteractiveObject) gameObject).addCustomisation(locCustomisation);
                } else if (gameObject instanceof AnimatedGroundDecoration) {
                    ((AnimatedGroundDecoration) gameObject).addCustomisation(locCustomisation);
                } else if (gameObject instanceof AnimatedWall) {
                    ((AnimatedWall) gameObject).addCustomisation(locCustomisation);
                } else if (gameObject instanceof AnimatedWallDecoration) {
                    ((AnimatedWallDecoration) gameObject).addCustomisation(locCustomisation);
                }
            }
        }
    }

    public static void upload(Tile[][][] tiles) {
        for (int z = 0; z < tiles.length; z++) {
            Tile[][] localTiles = tiles[z];
            for (int x = 0; x < localTiles.length; x++) {
                for (int y = 0; y < localTiles[x].length; y++) {
                    Tile tile = localTiles[x][y];
                    if (tile != null) {
                        if (tile.groundDecoration instanceof GameObject) {
                            ((GameObject) tile.groundDecoration).upload();
                        }

                        if (tile.primaryWallDecoration instanceof GameObject) {
                            ((GameObject) tile.primaryWallDecoration).upload();
                        }

                        if (tile.secondaryWallDecoration instanceof GameObject) {
                            ((GameObject) tile.secondaryWallDecoration).upload();
                        }

                        if (tile.primaryWall instanceof GameObject) {
                            ((GameObject) tile.primaryWall).upload();
                        }

                        if (tile.secondaryWall instanceof GameObject) {
                            ((GameObject) tile.secondaryWall).upload();
                        }

                        for (InteractiveEntityContainer interactives = tile.interactives; interactives != null; interactives = interactives.child) {
                            AbstractInteractiveEntity interactive = interactives.interactiveObject;
                            if (interactive instanceof GameObject) {
                                ((GameObject) interactive).upload();
                            }
                        }
                    }
                }
            }
        }

    }

    public RegionLoadingType getLoadingType() {
        return this.currentLoadingType;
    }

    public RegionLoadingStage getCurrentLoadingStage() {
        return this.currentStage;
    }

    public int getCompletion() {
        return 100 - this.missingMapCount * 100 / this.totalMapCount;
    }

    public int getSceneSizeX() {
        return this.sceneSizeX;
    }

    public DynamicTint getDynamicTint() {
        return this.dyanmicTint;
    }

    public float getRandomColorOffset() {
        return this.randomColorOffset;
    }

    public Scene getScene() {
        return this.scene;
    }

    public void decodeLocMapsServer(SceneGraph sceneGraph, byte[][] locBufferByRegion) {
        for (int z = 0; z < sceneGraph.regionSizeZ; z++) {
            if (!this.asyncMaploading) {
                SoundEffectPlayer.updatePlayers();
            }
            for (int localChunkX = 0; localChunkX < this.sceneSizeX >> 3; localChunkX++) {
                for (int localChunkY = 0; localChunkY < this.sceneSizeY >> 3; localChunkY++) {
                    int bitpackedRegionPosition = this.chunkBitpackedPosition[z][localChunkX][localChunkY];
                    if (bitpackedRegionPosition != -1) {
                        int chunkRotation = bitpackedRegionPosition >> 24 & 0x3;
                        if (!sceneGraph.overlayHidden || chunkRotation == 0) {
                            int rotation = bitpackedRegionPosition >> 1 & 0x3;
                            int regionX = bitpackedRegionPosition >> 14 & 0x3ff;
                            int regionY = bitpackedRegionPosition >> 3 & 0x7ff;
                            int regionId = (regionX / 8 << 8) + regionY / 8;
                            for (int index = 0; index < this.regionIds.length; index++) {
                                if (regionId == this.regionIds[index] && locBufferByRegion[index] != null) {
                                    sceneGraph.decodeLocMapServer(Renderers.activeToolkit, locBufferByRegion[index], z, localChunkX * 8, localChunkY * 8, chunkRotation, (regionX & 0x7) * 8, (regionY & 0x7) * 8, rotation, this.collisionMaps);
                                    break;
                                }
                            }
                        }
                    }
                    if (this.asyncMaploading) {
                        sleep(5);
                    }
                }
            }
        }
    }

    public SettingsBits getSettingsBits() {
        return this.tileSettingsBits;
    }

    public EnvironmentManager getEnvironmentManager() {
        return this.environmentManager;
    }

    public ObjectTypeList getLoctypeList() {
        return this.loctypeList;
    }

    public int[][] getDrawPrioritySharedby() {
        return this.drawPrioritySharedby;
    }

    public int[][] getDrawPriority() {
        return this.drawPriority;
    }
    
	public static void resetDrawPriorities() {
	    int[][] drawPriorities = Resource.mapRegion.getDrawPriority();
	    int regionSizeX = Resource.mapRegion.getSceneSizeX();
	    int regionSizeY = Resource.mapRegion.getSceneSizeY();
	
	    for (int x = 0; x < regionSizeX; x++) {
	        int[] prios = drawPriorities[x];
	        for (int y = 0; y < regionSizeY; y++) {
	            prios[y] = 0;
	        }
	    }
	}

    public void setActiveSettingsBits(byte[][][] settingsBits) {
        this.configuration = settingsBits;
    }

    public SceneGraph getSurfaceSceneGraph() {
        return this.surfaceMap;
    }

    public void setLoctypelist(ObjectTypeList loctypelist) {
        this.loctypeList = loctypelist;
    }

    public void resetInteractiveEntities() {
        if (this.scene != null) {
            RenderedEntityCache.reset();
            this.scene.interactiveEntities.clear();
            this.scene = null;
        }
    }

    public void setLoadingRegion() {
        this.currentStage = RegionLoadingStage.LOADING;
        this.missingMapCount = 0;
        this.totalMapCount = 1;
        this.locsStillLoading = 0;
        this.totalLocsLoading = 1;
    }

    public void resetAtmosphere() {
        this.environmentManager = new EnvironmentManager(Renderers.activeToolkit, this.sceneSizeX >> 3, this.sceneSizeY >> 3);
    }

    /**
     * In 317 you'll see the view range is 50 - 3500
     * this matches with what's calculated here, example: 104 * 34.46 = 3,583
     * since high revisions don't downscale models but move the camera further away 
     * (proper model precision) upscale view range too to match this.
     * start: 50 in 317 --> << 2 --> 200
     * end: 3500 in 317 --> << 2 --> 14000
     * 
     */
    public void calculateViewRange() {
        this.viewRangeStart = 200;
        this.viewRangeEnd = (int) (34.46D * this.sceneSizeX);
        this.viewRangeEnd <<= 2;
        if (Renderers.activeToolkit.extendBuildArea()) {
            this.viewRangeEnd += 512;
        }
        this.viewRangeEnd += 3072 * Loader.FOG_DISTANCE_MULTIPLIER;
    }

    public int getSceneSizeY() {
        return this.sceneSizeY;
    }
    
    public String getArchiveName(boolean floorData, boolean underwater, int regionX, int regionY) {
		String mapName = "";
		if (floorData) {
			mapName = mapName + "m" + regionX + '_' + regionY;
		} else {
			mapName = mapName + "l" + regionX + '_' + regionY;
		}
		if (underwater) {
			mapName = 'u' + mapName;
		}
		return mapName;
	}

    public void setMapSize(MapSize size) {
        if (size != this.activeMapSize) {
            this.sceneSizeX = this.sceneSizeY = size.size;
            this.chunkBitpackedPosition = new int[4][this.sceneSizeX >> 3][this.sceneSizeY >> 3];
            this.drawPriority = new int[this.sceneSizeX][this.sceneSizeY];
            this.drawPrioritySharedby = new int[this.sceneSizeX][this.sceneSizeY];
            for (int heightLevel = 0; heightLevel < 4; heightLevel++) {
                this.collisionMaps[heightLevel] = CollisionMap.createClipMap(this.sceneSizeX, this.sceneSizeY);
            }
            this.configuration = new byte[4][this.sceneSizeX][this.sceneSizeY];
            this.tileSettingsBits = new SettingsBits(this.sceneSizeX, this.sceneSizeY);
            resetAtmosphere();
            this.activeMapSize = size;
        }
    }

    public void resetNonCutsceneEntities() {
        if (this.currentLoadingType != RegionLoadingType.CUTSCENE && this.previousLoadingType != RegionLoadingType.CUTSCENE) {
            if (this.currentLoadingType == RegionLoadingType.INSTANCE || this.currentLoadingType == RegionLoadingType.INSTANCE_LARGE || this.previousLoadingType != this.currentLoadingType &&
            		(this.currentLoadingType == RegionLoadingType.STANDARD || this.previousLoadingType == RegionLoadingType.STANDARD)) {
                client.NPC_UPDATE_INDEX = 0;
                client.npcCount = 0;
                client.NPC_MAP.clear();
            }
            this.previousLoadingType = this.currentLoadingType;
        }
    }

    public void performUpdate(int BaseX, int BaseY, GameState newGamestate, boolean forcedPositionUpdate) {
        if (client.MAP_LOADING_STAGE == 2) {
            if (this.asyncMaploading) {
                throw new IllegalStateException();
            }
            client.MAP_LOADING_STAGE = 1;
            client.CURRENT_CUTSCENE = -1;
        }
        if (forcedPositionUpdate || this.baseX != BaseX || BaseY != this.baseY) {
            this.baseX = BaseX;
            this.baseY = BaseY;
            if (!this.asyncMaploading) {
                client.setGameState(newGamestate);
                MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(client.CURRENT_LANGUAGE), true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont);
            }
            if (this.baseCoordinates != null) {
                this.lastCoordgrid = this.baseCoordinates;
            } else {
                this.lastCoordgrid = new CoordGrid(0, 0, 0);
            }
            this.baseCoordinates = new CoordGrid(0, (this.baseX - (this.sceneSizeX >> 4)) * 8, (this.baseY - (this.sceneSizeY >> 4)) * 8);
            this.worldMapType = WorldMap.getMapDef(this.baseCoordinates.x, this.baseCoordinates.y);
            this.staticElements = null;
            if (!this.asyncMaploading) {
                updateEntities(newGamestate);
            }
        }
    }

    public void updateEntities(GameState requestedGamestate) {
        int deltaX = this.baseCoordinates.x - this.lastCoordgrid.x;
        int deltaY = this.baseCoordinates.y - this.lastCoordgrid.y;
        if (requestedGamestate == GameState.LOADING_PLEASE_WAIT) {
            for (int npcIndex = 0; npcIndex < client.npcCount; npcIndex++) {
                ObjectNode objectNode = client.NPC_ARRAY[npcIndex];
                if (objectNode != null) {
                    NpcEntity npcEntity = (NpcEntity) objectNode.value;
                    for (int index = 0; index < npcEntity.movementBaseX.length; index++) {
                        npcEntity.movementBaseX[index] -= deltaX;
                        npcEntity.movementBaseY[index] -= deltaY;
                    }
                    Vector3 abs = Vector3Pool.vector3From(npcEntity.getAbsolutePosition().coords);
                    abs.x -= (deltaX * 512);
                    abs.z -= (deltaY * 512);
                    npcEntity.setBasePosition(abs);
                    abs.pushVectorStack();
                }
            }
        } else {
            boolean copyExisting = false;
            client.NPC_UPDATE_INDEX = 0;
            int x = this.sceneSizeX * 512 - 512;
            int y = this.sceneSizeY * 512 - 512;
            for (int npcIndex = 0; npcIndex < client.npcCount; npcIndex++) {
                ObjectNode objectNode = client.NPC_ARRAY[npcIndex];
                if (objectNode != null) {
                    NpcEntity npcEntity = (NpcEntity) objectNode.value;
                    Vector3 abs = Vector3Pool.vector3From(npcEntity.getAbsolutePosition().coords);
                    abs.x -= (deltaX * 512);
                    abs.z -= (deltaY * 512);
                    npcEntity.setBasePosition(abs);
                    if ((int) abs.x >= 0 && (int) abs.x <= x && (int) abs.z >= 0 && (int) abs.z <= y) {
                        boolean hasUpdate = true;
                        for (int index = 0; index < npcEntity.movementBaseX.length; index++) {
                            npcEntity.movementBaseX[index] -= deltaX;
                            npcEntity.movementBaseY[index] -= deltaY;
                            if (npcEntity.movementBaseX[index] < 0 || npcEntity.movementBaseX[index] >= this.sceneSizeX ||
                            		npcEntity.movementBaseY[index] < 0 || npcEntity.movementBaseY[index] >= this.sceneSizeY) {
                                hasUpdate = false;
                            }
                        }
                        if (hasUpdate) {
                            client.NPC_UPDATE_INDICES[++client.NPC_UPDATE_INDEX - 1] = npcEntity.playerIndex;
                        } else {
                            npcEntity.setDirection(null);
                            objectNode.unlink();
                            copyExisting = true;
                        }
                    } else {
                        npcEntity.setDirection(null);
                        objectNode.unlink();
                        copyExisting = true;
                    }
                    abs.pushVectorStack();
                }
            }
            if (copyExisting) {
                client.npcCount = client.NPC_MAP.size();
                int count = 0;
                ObjectNode objectNode;
                for (Iterator<ObjectNode> npcIt = client.NPC_MAP.iterator(); npcIt.hasNext(); client.NPC_ARRAY[count++] = objectNode) {
                    objectNode = npcIt.next();
                }
            }
        }
        for (int playerIndex = 0; playerIndex < 2048; playerIndex++) {
            PlayerEntity playerEntity = PlayerUpdate.players[playerIndex];
            if (playerEntity != null) {
                for (int index = 0; index < playerEntity.movementBaseX.length; index++) {
                    playerEntity.movementBaseX[index] -= deltaX;
                    playerEntity.movementBaseY[index] -= deltaY;
                }
                Vector3 abs = Vector3Pool.vector3From(playerEntity.getAbsolutePosition().coords);
                abs.x -= (deltaX * 512);
                abs.z -= (deltaY * 512);
                playerEntity.setBasePosition(abs);
                abs.pushVectorStack();
            }
        }
        HintArrow[] hintArrows = PlayerUpdate.HINT_ARROWS;
        for (int arrowIndex = 0; arrowIndex < hintArrows.length; arrowIndex++) {
            HintArrow hintArrow = hintArrows[arrowIndex];
            if (hintArrow != null) {
                hintArrow.x -= deltaX * 512;
                hintArrow.y -= deltaY * 512;
            }
        }
        for (LocAction action = (LocAction) LocAction.pendingRemovals.getFirst(); action != null; action = (LocAction) LocAction.pendingRemovals.next()) {
            action.x -= deltaX;
            action.y -= deltaY;
            if (this.currentLoadingType != RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES && (action.x < 0 || action.y < 0 || action.x >= this.sceneSizeX || action.y >= this.sceneSizeY)) {
                action.unlink();
            }
        }
        for (LocAction action = (LocAction) LocAction.pendingCustomizations.getFirst(); action != null; action = (LocAction) LocAction.pendingCustomizations.next()) {
            action.x -= deltaX;
            action.y -= deltaY;
            if (this.currentLoadingType != RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES && (action.x < 0 || action.y < 0 || action.x >= this.sceneSizeX || action.y >= this.sceneSizeY)) {
                action.unlink();
            }
        }
        for (GroundItemWrapper groundItems = client.GROUND_ITEMS.first(); groundItems != null; groundItems = client.GROUND_ITEMS.next()) {
            int rotation = (int) (groundItems.pointer >> 28 & 0x3L);
            int sceneX = (int) (groundItems.pointer & 0x3fffL);
            int nodeSizeX = sceneX - this.baseCoordinates.x;
            int sceneY = (int) (groundItems.pointer >> 14 & 0x3fffL);
            int nodeSizeY = sceneY - this.baseCoordinates.y;
            if (this.scene != null) {
                if (nodeSizeX >= 0 && nodeSizeY >= 0 && nodeSizeX < this.sceneSizeX && nodeSizeY < this.sceneSizeY && nodeSizeX < this.scene.sceneDimensionX && nodeSizeY < this.scene.sceneDimensionY) {
                    if (this.scene.tiles != null) {
                        this.scene.removeGroundItem(rotation, nodeSizeX, nodeSizeY);
                    }
                } else if (this.currentLoadingType != RegionLoadingType.INSTANCE_LARGE_NO_RELOAD_UPDATEZONES) {
                    groundItems.unlink();
                }
            }
        }
        if (ComponentMinimap.minimapFlagX != 0) {
            ComponentMinimap.minimapFlagX -= deltaX;
            ComponentMinimap.minimapFlagY -= deltaY;
        }
        AreaSoundPlayer.initialize();
        AmbientSound.unlinkSounds(false);
        if (requestedGamestate == GameState.LOADING_PLEASE_WAIT) {
            client.anInt7262 -= deltaX * 512;
            client.anInt7376 -= deltaY * 512;
            Camera.cameraBaseX -= deltaX * 512;
            Camera.cameraBaseY -= deltaY * 512;
            if (Camera.cameraCalculationType != 4) {
                Camera.cameraCalculationType = 2;
                StaticMethods.anInt833 = -1;
                PingRequester.anInt5864 = -1;
            }
        } else {
            Camera.cameraMovementLocalX -= deltaX;
            Camera.cameraMovementLocalY -= deltaY;
            Camera.lookX -= deltaX;
            Camera.lookY -= deltaY;
            Camera.cameraPositionAbsoluteX -= deltaX * 512;
            Camera.cameraPositionAbsoluteY -= deltaY * 512;
            if (Math.abs(deltaX) > this.sceneSizeX || Math.abs(deltaY) > this.sceneSizeY) {
                this.environmentManager.requestDirectEnvironmentTransition();
            }
        }
        MiniMenu.reset();
        ComponentMinimap.reset();
        client.ACTIVE_GFX.clear();
        client.PROJECTILES.remove_all();
        client.TILE_MESSAGES.unlink();
        ParticleSystemManager.destroy();
    }

    public void clear() {
        this.underwaterMap = null;
        this.surfaceMap = null;
        for (int z = 0; z < 4; z++) {
            if (this.collisionMaps[z] != null) {
                this.collisionMaps[z].reset();
            }
        }
        if (this.tileSettingsBits != null) {
            this.tileSettingsBits.reset();
        }
        if (this.environmentManager != null) {
            this.environmentManager.resetChunkEnvironments();
        }
        if (this.scene != null) {
            this.scene.interactiveEntities.clear();
            this.scene = null;
        }
    }

    public void sleep(int ms) {
//        try {
//            Thread.sleep(ms);
//        } catch (InterruptedException interruptedException) {
//        	// NOOP
//        }
    }

    public void decodeClientUnderlayMasks(SceneGraph sceneGraph, byte[][] mapDatas) {
        int mapCount = mapDatas.length;
        for (int mapIndex = 0; mapIndex < mapCount; mapIndex++) {
            byte[] mapData = mapDatas[mapIndex];
            if (mapData != null) {
                Packet stream = new Packet(mapData);
                int x = this.regionIds[mapIndex] >> 8;
                int y = this.regionIds[mapIndex] & 0xff;
                int chunkX = x * 64 - this.baseCoordinates.x;
                int chunkY = y * 64 - this.baseCoordinates.y;
                if (!this.asyncMaploading) {
                    SoundEffectPlayer.updatePlayers();
                }
                sceneGraph.decodeTilesClient(stream, chunkX, chunkY, this.baseCoordinates.x, this.baseCoordinates.y, this.collisionMaps);
                sceneGraph.decodeEffectsClient(Renderers.activeToolkit, stream, chunkX, chunkY);
            }
        }
        for (int mapIndex = 0; mapIndex < mapCount; mapIndex++) {
            int x = (this.regionIds[mapIndex] >> 8) * 64 - this.baseCoordinates.x;
            int y = (this.regionIds[mapIndex] & 0xff) * 64 - this.baseCoordinates.y;
            byte[] mapBytes = mapDatas[mapIndex];
            if (mapBytes == null && this.baseY < 800) {
                if (!this.asyncMaploading) {
                    SoundEffectPlayer.updatePlayers();
                }
                sceneGraph.clearAllPlanes(x, y);
            }
        }
    }

    public void decodeServerUnderlayMasks(SceneGraph sceneGraph, byte[][] mapdata) {
        for (int sceneZ = 0; sceneZ < sceneGraph.regionSizeZ; sceneZ++) {
            if (!this.asyncMaploading) {
                SoundEffectPlayer.updatePlayers();
            }
            for (int chunkX = 0; chunkX < this.sceneSizeX >> 3; chunkX++) {
                for (int chunkY = 0; chunkY < this.sceneSizeY >> 3; chunkY++) {
                    int bitpacked = this.chunkBitpackedPosition[sceneZ][chunkX][chunkY];
                    if (bitpacked != -1) {
                        int plane = bitpacked >> 24 & 0x3;
                        if (!sceneGraph.overlayHidden || plane == 0) {
                            int rotation = bitpacked >> 1 & 0x3;
                            int absoluteX = bitpacked >> 14 & 0x3ff;
                            int absoluteY = bitpacked >> 3 & 0x7ff;
                            int regionId = (absoluteX / 8 << 8) + absoluteY / 8;
                            for (int mapIndex = 0; mapIndex < this.regionIds.length; mapIndex++) {
                                if (regionId == this.regionIds[mapIndex] && mapdata[mapIndex] != null) {
                                    Packet stream = new Packet(mapdata[mapIndex]);
                                    sceneGraph.decodeTilesServer(stream, sceneZ, chunkX * 8, chunkY * 8, plane, absoluteX, absoluteY, rotation, this.collisionMaps);
                                    sceneGraph.decodeEffectsServer(Renderers.activeToolkit, stream, sceneZ, chunkX * 8, chunkY * 8, plane, absoluteX, absoluteY, rotation);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int sceneZ = 0; sceneZ < sceneGraph.regionSizeZ; sceneZ++) {
            if (!this.asyncMaploading) {
                SoundEffectPlayer.updatePlayers();
            }
            for (int chunkX = 0; chunkX < this.sceneSizeX >> 3; chunkX++) {
                for (int chunkY = 0; chunkY < this.sceneSizeY >> 3; chunkY++) {
                    int bitpacked = this.chunkBitpackedPosition[sceneZ][chunkX][chunkY];
                    if (bitpacked == -1) {
                        sceneGraph.clearHeights(sceneZ, chunkX * 8, chunkY * 8, 8, 8);
                    }
                }
            }
        }
    }

    public void decodeLocMapsClient(SceneGraph sceneGraph, byte[][] locMapdataByRegion) {
        int regionCount = this.surfaceMapBuffer.length;
        for (int regionIndex = 0; regionIndex < regionCount; regionIndex++) {
            byte[] currentRegionBuffer = locMapdataByRegion[regionIndex];
            if (currentRegionBuffer != null) {
                int chunkX = (this.regionIds[regionIndex] >> 8) * 64 - this.baseCoordinates.x;
                int chunkY = (this.regionIds[regionIndex] & 0xff) * 64 - this.baseCoordinates.y;
                if (!this.asyncMaploading) {
                    SoundEffectPlayer.updatePlayers();
                }
                sceneGraph.decodeLocMapClient(Renderers.activeToolkit, currentRegionBuffer, chunkX, chunkY, this.collisionMaps);
                if (this.asyncMaploading) {
                    sleep(10);
                }
            }
        }
    }

    public void decodeNpcSpawns() {
        int npcMapCount = this.npcSpawnBuffer.length;
        for (int mapIndex = 0; mapIndex < npcMapCount; mapIndex++) {
            if (this.npcSpawnBuffer[mapIndex] != null) {
                int cursorPosition = -1;
                for (int cursor = 0; cursor < client.npcSpawnCursorPos; cursor++) {
                    if (client.npcSpawnMaps[cursor] == this.regionIds[mapIndex]) {
                        cursorPosition = cursor;
                        break;
                    }
                }
                if (cursorPosition == -1) {
                    client.npcSpawnMaps[client.npcSpawnCursorPos] = this.regionIds[mapIndex];
                    cursorPosition = ++client.npcSpawnCursorPos - 1;
                }
                Packet stream = new Packet(this.npcSpawnBuffer[mapIndex]);
                int index = 0;
                while (stream.currentPosition < this.npcSpawnBuffer[mapIndex].length && index < 511 && client.NPC_UPDATE_INDEX < 1023) {
                    int slot = cursorPosition | index++ << 6;
                    int i_8 = stream.readUShort();
                    int plane = i_8 >> 14;
                    int i_10 = i_8 >> 7 & 0x3f;
                    int i_11 = i_8 & 0x3f;
                    int i_12 = (this.regionIds[mapIndex] >> 8) * 64 - this.baseCoordinates.x + i_10;
                    int i_13 = (this.regionIds[mapIndex] & 0xff) * 64 - this.baseCoordinates.y + i_11;
                    NPCType npcType = TypeLists.NPC.list(stream.readUShort());
                    ObjectNode npcObjectNode = client.NPC_MAP.get(slot);
                    if (npcObjectNode == null && (npcType.walkingFlags & 0x1) > 0 && i_12 >= 0 && i_12 + npcType.size < this.sceneSizeX && i_13 >= 0 && i_13 + npcType.size < this.sceneSizeY) {
                        NpcEntity npcEntity = new NpcEntity(this.scene);
                        npcEntity.playerIndex = slot;
                        ObjectNode npcEntityObjectNode = new ObjectNode(npcEntity);
                        client.NPC_MAP.put(npcEntityObjectNode, slot);
                        client.NPC_ARRAY[++client.npcCount - 1] = npcEntityObjectNode;
                        client.NPC_UPDATE_INDICES[++client.NPC_UPDATE_INDEX - 1] = slot;
                        npcEntity.lastUpdate = client.cycles;
                        npcEntity.setDirection(npcType);
                        npcEntity.setSize(npcEntity.definitions.size);
                        npcEntity.turnDirection = npcEntity.definitions.turnDirection << 3;
                        npcEntity.turn(npcEntity.definitions.respawnDirection.getRotation().getValue() << 11 & 0x3fff, true);
                        npcEntity.move(plane, i_12, i_13, true, npcEntity.getSize());
                    }
                }
            }
        }
    }

    public void resetBasePosition() {
        this.baseCoordinates = new CoordGrid();
        this.baseY = 0;
        this.baseX = 0;
    }

    public byte[][] getLocalLighting(int z) {
        return this.surfaceMap != null && this.surfaceMap.lighting != null && this.surfaceMap.lighting[z] != null ? this.surfaceMap.lighting[z] : null;
    }

    public CoordGrid getBaseCoordinates() {
        return this.baseCoordinates;
    }

    public int getViewRangeEnd() {
        return this.viewRangeEnd;
    }

    public StaticElements getStaticElements() {
        return this.staticElements;
    }

    public byte[][][] getBits() {
        return this.configuration;
    }

    public int locLoadingPercent() {
        return 100 - this.locsStillLoading * 100 / this.totalLocsLoading;
    }

    public int getViewRangeStart() {
        return this.viewRangeStart;
    }

    public void reload() {
        if (this.asyncMaploading) {
            setLoadingRegion();
            GraphicsCache.resetWorldmapCaches(true);
            MapRegion mapRegion = Resource.mapRegion;
            this.regionIds = mapRegion.regionIds;
            this.surfaceMapArchives = mapRegion.surfaceMapArchives;
            this.surfaceLocArchives = mapRegion.surfaceLocArchives;
            this.npcSpawnArchives = mapRegion.npcSpawnArchives;
            this.underwaterMapArchives = mapRegion.underwaterMapArchives;
            this.underwaterLocArchives = mapRegion.underwaterLocArchives;
            this.xteas = mapRegion.xteas;
            this.surfaceMapBuffer = mapRegion.surfaceMapBuffer;
            this.surfaceLocBuffer = mapRegion.surfaceLocBuffer;
            this.npcSpawnBuffer = mapRegion.npcSpawnBuffer;
            this.underwaterMapBuffer = mapRegion.underwaterMapBuffer;
            this.underwaterLocBuffer = mapRegion.underwaterLocBuffer;
            this.collisionMaps = mapRegion.collisionMaps;
            this.tileSettingsBits = mapRegion.tileSettingsBits;
            this.environmentManager = mapRegion.environmentManager;
            this.chunkBitpackedPosition = mapRegion.chunkBitpackedPosition;
            this.viewRangeStart = mapRegion.viewRangeStart;
            this.viewRangeEnd = mapRegion.viewRangeEnd;
            this.worldMapType = mapRegion.worldMapType;
            this.staticElements = mapRegion.staticElements;
            this.drawPriority = mapRegion.drawPriority;
            this.drawPrioritySharedby = mapRegion.drawPrioritySharedby;
            this.configuration = mapRegion.configuration;
            this.currentLoadingType = mapRegion.currentLoadingType;
            this.activeMapSize = mapRegion.activeMapSize;
            this.baseCoordinates = mapRegion.baseCoordinates;
            this.lastCoordgrid = mapRegion.lastCoordgrid;
            this.baseX = mapRegion.baseX;
            this.baseY = mapRegion.baseY;
            this.sceneSizeX = mapRegion.sceneSizeX;
            this.sceneSizeY = mapRegion.sceneSizeY;
        } else if (client.gameState == GameState.UNK_5) {
            client.setGameState(GameState.UNK_11);
        } else if (client.gameState == GameState.UNK_0) {
            client.setGameState(GameState.UNK_2);
        } else if (client.gameState == GameState.UNK_7) {
            client.setGameState(GameState.UNK_6);
        } else if (client.gameState == GameState.LOGGED_INGAME) {
            client.setGameState(GameState.LOADING_PLEASE_WAIT);
        } else if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
            client.setGameState(GameState.UNK_9);
        }
    }

    public CollisionMap getCollisionMap(int z) {
        return this.collisionMaps[z];
    }

	public static void requestDebugMapUpdate() {
	    if (PreferenceManager.preferences.asyncMaploading.getValue() == 1) {
	        Resource.mapRegionLoaderTask.requestMapUpdate(new RegionLoadRequest(RegionLoadingType.RELOAD, null));
	    } else {
	        Resource.mapRegion.reload();
	        ComponentMinimap.reset();
	    }
	}

	public static void addGfxsToScene() {
	    for (PositionedGfx positionedGfx : client.ACTIVE_GFX) {
	        SpotAnimation objectGfx = positionedGfx.objectGfx;
	        if (Resource.mapRegion.getScene() != null && objectGfx.notAccelerated()) {
	            Resource.mapRegion.getScene().addInteractive(objectGfx, true);
	        }
	    }
	}
	
	public static void cycleGfx() {
	    for (PositionedGfx positionedGfx : client.ACTIVE_GFX) {
	        SpotAnimation spotAnimation = positionedGfx.objectGfx;
	        spotAnimation.progressCycle();
	        if (spotAnimation.needsReset()) {
	            positionedGfx.unlink();
	            spotAnimation.resetCycle();
	        }
	    }
	}

	public static void addProjectileSpawnsToScene() {
	    for (ProjectileSpawnRequest spawnRequest = (ProjectileSpawnRequest) client.PROJECTILES.getFirst(); spawnRequest != null; spawnRequest = (ProjectileSpawnRequest) client.PROJECTILES.next()) {
	        Projectile projectile = spawnRequest.projectile;
	        if (client.cycles > projectile.cyclesUntilDisappear) {
	            spawnRequest.unlink();
	            projectile.nextParticleCycle();
	        } else if (client.cycles >= projectile.cycleToSpawnAt) {
	            projectile.cycle();
	            if (projectile.entityLockOn > 0) {
	                if (client.MAP_LOADING_STAGE == 4) {
	                    PathingEntity animable3 = CutsceneTypeList.CUTSCENE_ENTITIES[projectile.entityLockOn - 1].getEntity();
	                    if (animable3 != null) {
	                        Vector3 abs = animable3.getAbsolutePosition().coords;
	                        if ((int) abs.x >= 0 && (int) abs.x < Resource.mapRegion.getSceneSizeX() * 512 && (int) abs.z >= 0 && (int) abs.z < Resource.mapRegion.getSceneSizeY() * 512) {
	                            projectile.start((int) abs.x, (int) abs.z, tileheightForAbsolutePosition((int) abs.x, (int) abs.z, animable3.plane) - projectile.endHeight, client.cycles);
	                        }
	                    }
	                } else {
	                    ObjectNode objectNode = client.NPC_MAP.get(projectile.entityLockOn - 1);
	                    if (objectNode != null) {
	                        NpcEntity npcEntity = (NpcEntity) objectNode.value;
	                        Vector3 abs = npcEntity.getAbsolutePosition().coords;
	                        if ((int) abs.x >= 0 && (int) abs.x < Resource.mapRegion.getSceneSizeX() * 512 && (int) abs.z >= 0 && (int) abs.z < Resource.mapRegion.getSceneSizeY() * 512) {
	                            projectile.start((int) abs.x, (int) abs.z, tileheightForAbsolutePosition((int) abs.x, (int) abs.z, projectile.plane) - projectile.endHeight, client.cycles);
	                        }
	                    }
	                }
	            }
	            if (projectile.entityLockOn < 0) {
	                int lockOn = -projectile.entityLockOn - 1;
	                PlayerEntity player;
	                if (lockOn == PlayerUpdate.myPlayerIndex) {
	                    player = PlayerUpdate.MY_PLAYER;
	                } else {
	                    player = PlayerUpdate.players[lockOn];
	                }
	                if (player != null) {
	                	Vector3 abs = player.getAbsolutePosition().coords;
	                    if ((int) abs.x >= 0 && (int) abs.x < Resource.mapRegion.getSceneSizeX() * 512 && (int) abs.z >= 0 && (int) abs.z < Resource.mapRegion.getSceneSizeY() * 512) {
	                        projectile.start((int) abs.x, (int) abs.z, tileheightForAbsolutePosition((int) abs.x, (int) abs.z, projectile.plane) - projectile.endHeight, client.cycles);
	                    }
	                }
	            }
	            projectile.animate(client.animatingStep);
	            Resource.mapRegion.getScene().addInteractive(projectile, true);
	        }
	    }
	}

	public static void applyGameobjectAction(LocAction action, boolean customize) {
	    if (action.inUpdateZone) {
	        if (action.srcId < 0 || ObjectType.isTypeCached(Resource.mapRegion.getLoctypeList(), action.srcId, action.srcShape)) {
	            if (customize) {
	                addCustomisation(action.plane, action.gameObjectType, action.x, action.y, null);
	            } else {
	                transformObject(action.plane, action.gameObjectType, action.x, action.y, action.srcId, action.srcRotation, action.srcShape, -1);
	            }
	            action.unlink();
	        }
	    } else if (action.queued && action.x >= 1 && action.y >= 1 && action.x <= Resource.mapRegion.getSceneSizeX() - 2 && action.y <= Resource.mapRegion.getSceneSizeY() - 2 && (action.dstId < 0 || ObjectType.isTypeCached(Resource.mapRegion.getLoctypeList(), action.dstId, action.dstShape))) {
	        if (customize) {
	            addCustomisation(action.plane, action.gameObjectType, action.x, action.y, action.dstCustomization);
	        } else {
	            transformObject(action.plane, action.gameObjectType, action.x, action.y, action.dstId, action.dstRotation, action.dstShape, -1);
	        }
	        action.queued = false;
	        if (!customize && action.srcId == action.dstId && action.srcId == -1) {
	            action.unlink();
	        } else if (!customize && action.srcId == action.dstId && action.srcRotation == action.dstRotation && action.dstShape == action.srcShape) {
	            action.unlink();
	        }
	    }
	}

	public static void transformObject(int plane, int type, int x, int y, int id, int rotation, int shape, int sequenceId) {
	    if (x >= 1 && y >= 1 && x <= Resource.mapRegion.getSceneSizeX() - 2 && y <= Resource.mapRegion.getSceneSizeY() - 2) {
	        int collisionPlane = plane;
	        if (plane < 3 && Resource.mapRegion.getSettingsBits().areRoofsHidden(x, y)) {
	            collisionPlane = plane + 1;
	        }
	        if (Resource.mapRegion.getScene() != null) {
	            Resource.mapRegion.getSurfaceSceneGraph().removeGameObject(Renderers.activeToolkit, plane, type, x, y, Resource.mapRegion.getCollisionMap(plane));
	            if (id >= 0) {
	                int groundDecorationPreference = PreferenceManager.preferences.groundDecoration.getValue();
	                PreferenceManager.preferences.setValue(PreferenceManager.preferences.groundDecoration, 1);
	                Resource.mapRegion.getSurfaceSceneGraph().addObject(Renderers.activeToolkit, collisionPlane, plane, x, y, id, rotation, shape, Resource.mapRegion.getCollisionMap(plane), sequenceId);
	                PreferenceManager.preferences.setValue(PreferenceManager.preferences.groundDecoration, groundDecorationPreference);
	            }
	        }
	    }
	}

	// 0x4 = renderOnLowerZ (render object from next plane on plane-1), 0x2 = hide roofs
	// Something with roofs? - TODO: fix the horrible naming in this
	public static boolean calculateVisibilityAdjustments(Tile[][][] regionTiles, int anInt1234, int baseX, int baseY, boolean hideRoofs) {
	    byte[][][] settingBits = Resource.mapRegion.getBits();
	    byte setting = hideRoofs ? 1 : (byte) (client.timesSceneRendered & 0xff);
	    if (settingBits[PlayerUpdate.MY_PLAYER_PLANE][baseX][baseY] == setting) {
	        return false;
	    } 
        SettingsBits regionMap = Resource.mapRegion.getSettingsBits();
        if ((regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][baseX][baseY] & 0x4) == 0) {
            return false;
        } 
        int currentPosition = 0;
        int position = 1;
        client.bigX[0] = baseX;
        client.bigY[0] = baseY;
        settingBits[PlayerUpdate.MY_PLAYER_PLANE][baseX][baseY] = setting;
        while (position != currentPosition) {
            int tileBaseX = client.bigX[currentPosition] & 0xffff;
            int bitpackedTypeRotA = client.bigX[currentPosition] >> 16 & 0xff;
            int sceneXHashB = client.bigX[currentPosition] >> 24 & 0xff;
            int tileBaseY = client.bigY[currentPosition] & 0xffff;
            int bitpackedTypeRotB = client.bigY[currentPosition] >> 16 & 0xff;
            currentPosition = currentPosition + 1 & 0xfff;
            boolean renderOnLowerZ = (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY] & 0x4) == 0;
            boolean visibilityUpdate = false;
            if (regionTiles != null) {
                label270:
                for (int planeAbove = PlayerUpdate.MY_PLAYER_PLANE + 1; planeAbove <= 3; planeAbove++) {
                    if (regionTiles[planeAbove] != null && (regionMap.tileFunctionMasks[planeAbove][tileBaseX][tileBaseY] & 0x8) == 0) {
                        if (renderOnLowerZ && regionTiles[planeAbove][tileBaseX][tileBaseY] != null) {
                            if (regionTiles[planeAbove][tileBaseX][tileBaseY].primaryWall != null) {
                            	int cullingType = TileUtils.getCullingType(bitpackedTypeRotA);
                                if (regionTiles[planeAbove][tileBaseX][tileBaseY].primaryWall.cullingType == cullingType
                                		|| regionTiles[planeAbove][tileBaseX][tileBaseY].secondaryWall != null
                                		&& regionTiles[planeAbove][tileBaseX][tileBaseY].secondaryWall.cullingType == cullingType) {
                                    continue;
                                }
                                if (sceneXHashB != 0) {
                                	int i_20 = TileUtils.getCullingType(sceneXHashB);
                                    if (regionTiles[planeAbove][tileBaseX][tileBaseY].primaryWall.cullingType == i_20
                                    		|| regionTiles[planeAbove][tileBaseX][tileBaseY].secondaryWall != null
                                    		&& regionTiles[planeAbove][tileBaseX][tileBaseY].secondaryWall.cullingType == i_20) {
                                        continue;
                                    }
                                }
                                if (bitpackedTypeRotB != 0) {
                                	int i_20 = TileUtils.getCullingType(bitpackedTypeRotB);
                                    if (regionTiles[planeAbove][tileBaseX][tileBaseY].primaryWall.cullingType == i_20
                                    		|| regionTiles[planeAbove][tileBaseX][tileBaseY].secondaryWall != null
                                    		&& regionTiles[planeAbove][tileBaseX][tileBaseY].secondaryWall.cullingType == i_20) {
                                        continue;
                                    }
                                }
                            }
                            Tile tile = regionTiles[planeAbove][tileBaseX][tileBaseY];
                            if (tile.interactives != null) {
                                for (InteractiveEntityContainer interactives = tile.interactives; interactives != null; interactives = interactives.child) {
                                	AbstractInteractiveEntity abstractInteractive = interactives.interactiveObject;
                                    if (abstractInteractive instanceof GameObject gameObject) {
                                        int shape = gameObject.getShape();
                                        int rotation = gameObject.getRotation();
                                        if (shape == 21) {
                                            shape = 19;
                                        }
                                        int bitpackedPlacement = shape | rotation << 6;
                                        if (bitpackedTypeRotA == bitpackedPlacement ||
                                        		sceneXHashB != 0 && sceneXHashB == bitpackedPlacement ||
                                        		bitpackedTypeRotB != 0 && bitpackedTypeRotB == bitpackedPlacement) {
                                            continue label270;
                                        }
                                    }
                                }
                            }
                        }
                        Tile tile = regionTiles[planeAbove][tileBaseX][tileBaseY];
                        if (tile != null && tile.interactives != null) {
                            for (InteractiveEntityContainer interactives = tile.interactives; interactives != null; interactives = interactives.child) {
                            	AbstractInteractiveEntity abstractInteractive = interactives.interactiveObject;
                                if (abstractInteractive.endX != abstractInteractive.startX || abstractInteractive.endZ != abstractInteractive.startZ) {
                                    for (int x = abstractInteractive.startX; x <= abstractInteractive.endX; x++) {
                                        for (int y = abstractInteractive.startZ; y <= abstractInteractive.endZ; y++) {
                                            settingBits[planeAbove][x][y] = setting;
                                        }
                                    }
                                }
                            }
                        }
                        settingBits[planeAbove][tileBaseX][tileBaseY] = setting;
                        visibilityUpdate = true;
                    }
                }
            }
            if (visibilityUpdate) {
            	int floorHeight = Resource.mapRegion.getScene().localHeightmap[PlayerUpdate.MY_PLAYER_PLANE + 1].getHeight(tileBaseX, tileBaseY);
                if (client.visibleBelowDrawheight[anInt1234] < floorHeight) {
                    client.visibleBelowDrawheight[anInt1234] = floorHeight;
                }
                int upscaledX = tileBaseX << 9;
                int upscaledY = tileBaseY << 9;
                if (client.visibleAboveX[anInt1234] > upscaledX) {
                    client.visibleAboveX[anInt1234] = upscaledX;
                } else if (client.visibleBelowX[anInt1234] < upscaledX) {
                    client.visibleBelowX[anInt1234] = upscaledX;
                }
                if (client.visibleAboveY[anInt1234] > upscaledY) {
                    client.visibleAboveY[anInt1234] = upscaledY;
                } else if (client.visibleBelowY[anInt1234] < upscaledY) {
                    client.visibleBelowY[anInt1234] = upscaledY;
                }
            }
            if (!renderOnLowerZ) {
                if (tileBaseX >= 1 && setting != settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY]) {
                    client.bigX[position] = tileBaseX - 1 | 0x120000 | -754974720;
                    client.bigY[position] = tileBaseY | 0x130000;
                    position = position + 1 & 0xfff;
                    settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY] = setting;
                }
                if (++tileBaseY < Resource.mapRegion.getSceneSizeY()) {
                    if (tileBaseX - 1 >= 0 && setting != settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY]
                    		&& (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY] & 0x4) == 0
                    		&& (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY - 1] & 0x4) == 0) {
                        client.bigX[position] = tileBaseX - 1 | 0x120000 | 0x52000000;
                        client.bigY[position] = tileBaseY | 0x130000;
                        position = position + 1 & 0xfff;
                        settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY] = setting;
                    }
                    if (setting != settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY]) {
                        client.bigX[position] = tileBaseX | 0x520000 | 0x13000000;
                        client.bigY[position] = tileBaseY | 0x530000;
                        position = position + 1 & 0xfff;
                        settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY] = setting;
                    }
                    if (tileBaseX + 1 < Resource.mapRegion.getSceneSizeX() && settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY] != setting && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY] & 0x4) == 0 && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY - 1] & 0x4) == 0) {
                        client.bigX[position] = tileBaseX + 1 | 0x520000 | -1845493760;
                        client.bigY[position] = tileBaseY | 0x530000;
                        position = position + 1 & 0xfff;
                        settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY] = setting;
                    }
                }
                tileBaseY--;
                if (tileBaseX + 1 < Resource.mapRegion.getSceneSizeX() && setting != settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY]) {
                    client.bigX[position] = tileBaseX + 1 | 0x920000 | 0x53000000;
                    client.bigY[position] = tileBaseY | 0x930000;
                    position = position + 1 & 0xfff;
                    settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY] = setting;
                }
                if (--tileBaseY >= 0) {
                    if (tileBaseX - 1 >= 0 && setting != settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY] && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY] & 0x4) == 0 && (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY + 1] & 0x4) == 0) {
                        client.bigX[position] = tileBaseX - 1 | 0xd20000 | 0x12000000;
                        client.bigY[position] = tileBaseY | 0xd30000;
                        position = position + 1 & 0xfff;
                        settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX - 1][tileBaseY] = setting;
                    }
                    if (setting != settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY]) {
                        client.bigX[position] = tileBaseX | 0xd20000 | -1828716544;
                        client.bigY[position] = tileBaseY | 0xd30000;
                        position = position + 1 & 0xfff;
                        settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY] = setting;
                    }
                    if (tileBaseX + 1 < Resource.mapRegion.getSceneSizeX()
                    		&& settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY] != setting
                    		&& (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX][tileBaseY] & 0x4) == 0
                    		&& (regionMap.tileFunctionMasks[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY + 1] & 0x4) == 0) {
                        client.bigX[position] = tileBaseX + 1 | 0x920000 | -771751936;
                        client.bigY[position] = tileBaseY | 0x930000;
                        position = position + 1 & 0xfff;
                        settingBits[PlayerUpdate.MY_PLAYER_PLANE][tileBaseX + 1][tileBaseY] = setting;
                    }
                }
            }
        }
        if (client.visibleBelowDrawheight[anInt1234] != -1000000) {
            client.visibleBelowDrawheight[anInt1234] += 40;
            client.visibleAboveX[anInt1234] -= 512;
            client.visibleBelowX[anInt1234] += 512;
            client.visibleBelowY[anInt1234] += 512;
            client.visibleAboveY[anInt1234] -= 512;
        }
        return true;
    }
}
