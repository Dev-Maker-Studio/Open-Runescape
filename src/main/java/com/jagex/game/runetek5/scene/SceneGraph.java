package com.jagex.game.runetek5.scene;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.light.LightType;
import com.jagex.game.runetek5.config.loc.LocShapes;
import com.jagex.game.runetek5.config.loc.LocType;
import com.jagex.game.runetek5.config.loc.LocTypeList;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.entity.gameobject.AbstractGroundDecoration;
import com.jagex.game.runetek5.entity.gameobject.AbstractWall;
import com.jagex.game.runetek5.entity.gameobject.AbstractWallDecoration;
import com.jagex.game.runetek5.entity.gameobject.AnimatedGroundDecoration;
import com.jagex.game.runetek5.entity.gameobject.AnimatedInteractiveObject;
import com.jagex.game.runetek5.entity.gameobject.AnimatedWall;
import com.jagex.game.runetek5.entity.gameobject.AnimatedWallDecoration;
import com.jagex.game.runetek5.entity.gameobject.StationaryGroundDecoration;
import com.jagex.game.runetek5.entity.gameobject.StationaryInteractiveObject;
import com.jagex.game.runetek5.entity.gameobject.StationaryWall;
import com.jagex.game.runetek5.entity.gameobject.StationaryWallDecoration;
import com.jagex.game.runetek5.graphics.light.FlickeringEffect;
import com.jagex.game.runetek5.graphics.light.PointLight;
import com.jagex.game.runetek5.entity.AbstractInteractiveEntity;
import com.jagex.game.runetek5.scene.environment.EnvironmentManager;
import com.jagex.game.runetek5.scene.environment.Environment;
import com.jagex.game.runetek5.scene.ground.Ground;
import com.jagex.game.runetek5.sound.player.area.AmbientSound;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;

public class SceneGraph extends MapLoader {

    public static boolean soundsOnly;

    public static boolean cullingAdjustment;

    public static final int[] WALL_DECORATION_DISPLACE_X = {1, 0, -1, 0};
    public static final int[] WALL_DECORATION_DISPLACE_Y = {0, -1, 0, 1};

    public static final int[] ANIMATED_WALL_DECORATION_DISPLACE_X = {1, -1, -1, 1};
    public static final int[] ANIMATED_WALL_DECORATION_DISPLACE_Y = {-1, -1, 1, 1};

    public int activeLogicHeight = 99;

    public final LocTypeList locTypeList;

    public final EnvironmentManager environmentManager;

    public SceneGraph(Scene scene, LocTypeList loctypelist, int dimensionZ, int dimensionX, int dimensionY, boolean hideOverlay, SettingsBits regionMap, EnvironmentManager sceneDefault) {
        super(scene, dimensionZ, dimensionX, dimensionY, hideOverlay, TypeLists.FLO, TypeLists.FLU, regionMap);
        this.locTypeList = loctypelist;
        this.environmentManager = sceneDefault;
    }
    
    public void addObject(AbstractToolkit toolkit, int locPlane, int logicHeight, int baseX, int baseY, int locId, int locRotation, int locShape, CollisionMap collisionMap, int sequenceId) {
        if (logicHeight < this.activeLogicHeight) {
            this.activeLogicHeight = logicHeight;
        }
        LocType locType = this.locTypeList.list(locId);
        if (PreferenceManager.preferences.textures.getValue() != 0 || !locType.requiresTextures) {
            int locSizeX;
            int locSizeY;
            if (locRotation != 1 && locRotation != 3) {
                locSizeX = locType.sizeX;
                locSizeY = locType.sizeY;
            } else {
                locSizeX = locType.sizeY;
                locSizeY = locType.sizeX;
            }
            int xA;
            int xB;
            if (locSizeX + baseX <= super.regionSizeX) {
                xA = (locSizeX >> 1) + baseX;
                xB = (locSizeX + 1 >> 1) + baseX;
            } else {
                xA = baseX;
                xB = baseX + 1;
            }
            int yA;
            int yB;
            if (baseY + locSizeY <= super.regionSizeY) {
                yA = baseY + (locSizeY >> 1);
                yB = baseY + (locSizeY + 1 >> 1);
            } else {
                yA = baseY;
                yB = baseY + 1;
            }
            Ground surfaceGround = super.scene.localHeightmap[locPlane];
            int averageHeight = surfaceGround.getHeight(xA, yA) + surfaceGround.getHeight(xB, yA) + surfaceGround.getHeight(xA, yB) + surfaceGround.getHeight(xB, yB) >> 2;
            int sceneX = (baseX << 9) + (locSizeX << 8);
            int sceneY = (baseY << 9) + (locSizeY << 8);
            boolean applyShading = super.lowDetailShadows && !super.overlayHidden && locType.delayShading;
            if (locType.hasSound()) {
                AmbientSound.createAmbientSoundEffect(logicHeight, baseX, baseY, locRotation, locType, null, null);
            }
            boolean stationary = sequenceId == -1 && !locType.isAnimated() && locType.alternativeIds == null && !locType.hasAnimation && !locType.transforms;
            if (!soundsOnly || (!LocShapes.isWall(locShape) || locType.occlusionMode == 1) && (!LocShapes.isRoof(locShape) || locType.occlusionMode != 0)) {
                if (locShape == LocShapes.GROUND_DECORATION.shape) {
                    if (PreferenceManager.preferences.groundDecoration.getValue() != 0 || locType.hasActions != 0 || locType.blocksMovement == 1 || locType.forceDisplayDecoration) {
                    	AbstractGroundDecoration groundDecoration;
                    	if (stationary) {
                            StationaryGroundDecoration stationaryGroundDecoration = new StationaryGroundDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locRotation, applyShading);
                            if (stationaryGroundDecoration.hasRenderingShadow()) {
                                stationaryGroundDecoration.addShadow(toolkit);
                            }
                            groundDecoration = stationaryGroundDecoration;
                        } else {
                            groundDecoration = new AnimatedGroundDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locRotation, sequenceId);
                        }
                        super.scene.addGroundDecoration(logicHeight, baseX, baseY, groundDecoration);
                        if (locType.blocksMovement == 1 && collisionMap != null) {
                            collisionMap.flagBlockedDecoration(baseX, baseY);
                        }
                    }
                } else if (locShape != LocShapes.SCENERY_INTERACT.shape && locShape != LocShapes.GROUND_INTERACT.shape) {
                    if (!LocShapes.isRoof(locShape) && !LocShapes.isRoofEdge(locShape)) {
                        if (!addWall(toolkit, locShape, locRotation, stationary, locType, sequenceId, logicHeight, locPlane, sceneX, averageHeight, sceneY, baseX, baseY, applyShading, surfaceGround, collisionMap, locSizeX, locSizeY)) {
                            addWallDecoration(toolkit, locShape, locRotation, stationary, locType, sequenceId, logicHeight, locPlane, sceneX, averageHeight, sceneY, baseX, baseY);
                        }
                    } else {
                    	AbstractInteractiveEntity interactive;
                        if (stationary) {
                            StationaryInteractiveObject stationaryInteractable = new StationaryInteractiveObject(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, applyShading);
                            if (stationaryInteractable.hasRenderingShadow()) {
                                stationaryInteractable.addShadow(toolkit);
                            }
                            interactive = stationaryInteractable;
                        } else {
                            interactive = new AnimatedInteractiveObject(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, sequenceId);
                        }
                        super.scene.addInteractive(interactive, false);
                        if (super.lowDetailShadows && !super.overlayHidden && LocShapes.isRoof(locShape) && locShape != LocShapes.DIAGONAL_SLOPE_ROOF.shape && logicHeight > 0 && locType.occlusionMode != 0) {
                            super.cullingsMap[logicHeight][baseX][baseY] |= 0x4;
                        }
                        if (locType.blocksMovement != 0 && collisionMap != null) {
                            collisionMap.flagBlockedObject(baseX, baseY, locSizeX, locSizeY, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                        }
                    }
                } else {
                    AbstractInteractiveEntity addedInteractive;
                    StationaryInteractiveObject stationaryInteractable = null;
                    int displacement;
                    if (stationary) {
                        StationaryInteractiveObject interactive = new StationaryInteractiveObject(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, applyShading);
                        displacement = interactive.getShadowDisplacement();
                        addedInteractive = interactive;
                        stationaryInteractable = interactive;
                    } else {
                        addedInteractive = new AnimatedInteractiveObject(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, baseX, locSizeX + baseX - 1, baseY, baseY + locSizeY - 1, locShape, locRotation, sequenceId);
                        displacement = 15;
                    }
                    if (super.scene.addInteractive(addedInteractive, false)) {
                        if (stationaryInteractable != null && stationaryInteractable.hasRenderingShadow()) {
                            stationaryInteractable.addShadow(toolkit);
                        }
                        if (locType.staticShadow && super.lowDetailShadows) {
                            if (displacement > 30) {
                                displacement = 30;
                            }
                            for (int baseXOffset = 0; baseXOffset <= locSizeX; baseXOffset++) {
                                for (int baseYOffset = 0; baseYOffset <= locSizeY; baseYOffset++) {
                                    surfaceGround.setTileShadow(baseXOffset + baseX, baseY + baseYOffset, displacement);
                                }
                            }
                        }
                    }
                    if (locType.blocksMovement != 0 && collisionMap != null) {
                        collisionMap.flagBlockedObject(baseX, baseY, locSizeX, locSizeY, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                    }
                }
            }
        }
    }

    public void decodeEffectsServer(AbstractToolkit toolkit, Packet mapBuffer, int lightZ, int sceneStartX, int sceneStartY, int z, int i_7, int i_8, int rotation) {
        if (!super.overlayHidden) {
            boolean hasActiveLights = false;
            Environment environment = null;
            int i_13 = (i_7 & 0x7) * 8;
            int i_14 = (i_8 & 0x7) * 8;
            while (true) {
                while (mapBuffer.currentPosition < mapBuffer.buffer.length) {
                	int opcode = mapBuffer.readUnsignedByte();
                    if (opcode == 0) {
                        if (environment == null) {
                            environment = new Environment(mapBuffer, this.environmentManager);
                        } else {
                            environment.decode(mapBuffer, this.environmentManager);
                        }
                    } else {
                        if (opcode == 1) {
                        	int loops = mapBuffer.readUnsignedByte();
                            if (loops > 0) {
                                for (int index = 0; index < loops; index++) {
                                    FlickeringEffect flickeringEffect = new FlickeringEffect(toolkit, super.scene.tileUnits, mapBuffer);
                                    if (flickeringEffect.type == 31) {
                                        LightType lightType = TypeLists.LIGHT.list(mapBuffer.readUnsignedShort());
                                        flickeringEffect.setType(lightType.effect, lightType.duration, lightType.ticker, lightType.surrounding);
                                    }
                                    if (toolkit.getMaxLights() > 0) {
                                        PointLight light = flickeringEffect.pointLight;
                                        int sceneX = light.getX() >> 9;
                                		int sceneY = light.getZ() >> 9;
                                        if (flickeringEffect.level == z && sceneX >= i_13 && sceneX < i_13 + 8 && sceneY >= i_14 && sceneY < i_14 + 8) {
                                        	int positionX = (sceneStartX << 9) + EnvironmentManager.rotatedX(light.getX() & 0xfff, light.getZ() & 0xfff, rotation);
                                        	int positionY = (sceneStartY << 9) + EnvironmentManager.rotatedZ(light.getX() & 0xfff, light.getZ() & 0xfff, rotation);
                                            sceneX = positionX >> 9;
                                            sceneY = positionY >> 9;
                                            if (sceneX >= 0 && sceneY >= 0 && sceneX < super.regionSizeX && sceneY < super.regionSizeY) {
                                                light.setPosition(positionX, super.tileHeights[z][sceneX][sceneY] - light.getY(), positionY);
                                                super.scene.calculateLightUid(flickeringEffect);
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (opcode == 2) {
                            if (environment == null) {
                                environment = new Environment();
                            }
                            environment.decodeHdr(mapBuffer);
                        } else if (opcode == 128) {
                            if (environment == null) {
                                environment = new Environment();
                            }
                            environment.decodeSkybox(mapBuffer, this.environmentManager);
                        } else {
                            if (opcode != 129) {
                                throw new IllegalStateException("");
                            }
                            if (super.lighting == null) {
                                super.lighting = new byte[4][][];
                            }
                            for (int currentZ = 0; currentZ < 4; currentZ++) {
                                byte lightingDecodeType = mapBuffer.readByte();
                                if (lightingDecodeType == 0 && super.lighting[lightZ] != null) {
                                    if (currentZ <= z) {
                                    	int minX = sceneStartX;
                                    	int maxX = sceneStartX + 7;
                                        int minY = sceneStartY;
                                        int maxY = sceneStartY + 7;
                                        if (sceneStartX < 0) {
                                            minX = 0;
                                        } else if (sceneStartX >= super.regionSizeX) {
                                            minX = super.regionSizeX;
                                        }
                                        if (maxX < 0) {
                                            maxX = 0;
                                        } else if (maxX >= super.regionSizeX) {
                                            maxX = super.regionSizeX;
                                        }
                                        if (sceneStartY < 0) {
                                            minY = 0;
                                        } else if (sceneStartY >= super.regionSizeY) {
                                            minY = super.regionSizeY;
                                        }
                                        if (maxY < 0) {
                                            maxY = 0;
                                        } else if (maxY >= super.regionSizeY) {
                                            maxY = super.regionSizeY;
                                        }
                                        while (minX < maxX) {
                                            while (minY < maxY) {
                                                super.lighting[lightZ][minX][minY] = 0;
                                                minY++;
                                            }
                                            minX++;
                                        }
                                    }
                                } else if (lightingDecodeType != 1) {
                                    if (lightingDecodeType == 2) {
                                    }
                                } else {
                                    if (super.lighting[lightZ] == null) {
                                        super.lighting[lightZ] = new byte[super.regionSizeX + 1][super.regionSizeY + 1];
                                    }
                                    for (int insideRegionX = 0; insideRegionX < 64; insideRegionX += 4) {
                                        for (int insideRegionY = 0; insideRegionY < 64; insideRegionY += 4) {
                                            byte lightIntensity = mapBuffer.readByte();
                                            if (currentZ <= z) {
                                                for (int x = insideRegionX; x < insideRegionX + 4; x++) {
                                                    for (int y = insideRegionY; y < insideRegionY + 4; y++) {
                                                        if (x >= i_13 && x < i_13 + 8 && y >= i_14 && y < i_14 + 8) {
                                                        	int lightSceneX = sceneStartX + EnvironmentManager.rotateTerrainBlockX(x & 0x7, y & 0x7, rotation);
                                                            int lightSceneY = sceneStartY + EnvironmentManager.rotateTerrainBlockY(x & 0x7, y & 0x7, rotation);
                                                            if (lightSceneX >= 0 && lightSceneX < super.regionSizeX && lightSceneY >= 0 && lightSceneY < super.regionSizeY) {
                                                                super.lighting[lightZ][lightSceneX][lightSceneY] = lightIntensity;
                                                                hasActiveLights = true;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (environment != null) {
                    this.environmentManager.addChunkEnvironment(sceneStartX >> 3, sceneStartY >> 3, environment);
                }
                if (!hasActiveLights && super.lighting != null && super.lighting[lightZ] != null) {
                	int sceneEndX = sceneStartX + 7;
                	int sceneEndY = sceneStartY + 7;
                    for (int lightSceneX = sceneStartX; lightSceneX < sceneEndX; lightSceneX++) {
                        for (int lightSceneY = sceneStartY; lightSceneY < sceneEndY; lightSceneY++) {
                            super.lighting[lightZ][lightSceneX][lightSceneY] = 0;
                        }
                    }
                }
                break;
            }
        }
    }

    public void decodeLocMapClient(AbstractToolkit toolkit, byte[] locData, int offsetX, int offsetY, CollisionMap[] collisionMaps) {
        Packet buffer = new Packet(locData);
        int locId = -1;
        while (true) {
            int locIdOffset = buffer.readSum();
            if (locIdOffset == 0) {
                return;
            }
            locId += locIdOffset;
            int bitpackedPosition = 0;
            while (true) {
                int positionOffset = buffer.readUnsignedSmart();
                if (positionOffset == 0) {
                    break;
                }
                bitpackedPosition += positionOffset - 1;
                int localY = bitpackedPosition & 0x3f;
                int localX = bitpackedPosition >> 6 & 0x3f;
                int plane = bitpackedPosition >> 12;
                int bitpackedDescription = buffer.readUnsignedByte();
                int type = bitpackedDescription >> 2;
                int rotation = bitpackedDescription & 0x3;
                int sceneX = localX + offsetX;
                int sceneY = localY + offsetY;
                if (sceneX > 0 && sceneY > 0 && sceneX < super.regionSizeX - 1 && sceneY < super.regionSizeY - 1) {
                    CollisionMap clipping = null;
                    if (!super.overlayHidden) {
                        int collisionPlane = plane;
                        if ((super.regionMap.tileFunctionMasks[1][sceneX][sceneY] & 0x2) == 2) {
                            collisionPlane = plane - 1;
                        }
                        if (collisionPlane >= 0) {
                            clipping = collisionMaps[collisionPlane];
                        }
                    }
                    addObject(toolkit, plane, plane, sceneX, sceneY, locId, rotation, type, clipping, -1);
                }
            }
        }
    }

    public void decodeLocMapServer(AbstractToolkit toolkit, byte[] locMapData, int plane, int sceneX, int sceneY, int landscapeZ, int mapX, int mapY, int rotation, CollisionMap[] clippings) {
        Packet locMapBuffer = new Packet(locMapData);
        int locId = -1;
        while (true) {
            int locOffset = locMapBuffer.readSum();
            if (locOffset == 0) {
                return;
            }
            locId += locOffset;
            int bitpackedPosition = 0;
            while (true) {
                int localPositionOffset = locMapBuffer.readUnsignedSmart();
                if (localPositionOffset == 0) {
                    break;
                }
                bitpackedPosition += localPositionOffset - 1;
                int y = bitpackedPosition & 0x3f;
                int x = bitpackedPosition >> 6 & 0x3f;
                int z = bitpackedPosition >> 12;
                int bitpackedLocDetails = locMapBuffer.readUnsignedByte();
                int type = bitpackedLocDetails >> 2;
                int locRotation = bitpackedLocDetails & 0x3;
                if (z == landscapeZ && x >= mapX && x < mapX + 8 && y >= mapY && y < mapY + 8) {
                    LocType loctype = this.locTypeList.list(locId);
                    int localX = sceneX + EnvironmentManager.localOffsetX(x & 0x7, y & 0x7, rotation, loctype.sizeX, loctype.sizeY, locRotation);
                    int localY = sceneY + EnvironmentManager.localOffsetY(x & 0x7, y & 0x7, rotation, loctype.sizeX, loctype.sizeY, locRotation);
                    if (localX > 0 && localY > 0 && localX < super.regionSizeX - 1 && localY < super.regionSizeY - 1) {
                        CollisionMap clipping = null;
                        if (!super.overlayHidden) {
                            int clipPlane = plane;
                            if ((super.regionMap.tileFunctionMasks[1][localX][localY] & 0x2) == 2) {
                                clipPlane = plane - 1;
                            }
                            if (clipPlane >= 0) {
                                clipping = clippings[clipPlane];
                            }
                        }
                        addObject(toolkit, plane, plane, localX, localY, locId, locRotation + rotation & 0x3, type, clipping, -1);
                    }
                }
            }
        }
    }

    /**
     * Contains logic for shading and culling
     */
    public void calculateVisibility(boolean underwater) {
        super.scene.applyShading();
        if (!underwater) {
            if (super.regionSizeZ > 1) {
                for (int x = 0; x < super.regionSizeX; x++) {
                    for (int y = 0; y < super.regionSizeY; y++) {
                        if ((super.regionMap.tileFunctionMasks[1][x][y] & 0x2) == 2) {
                            super.scene.linkBelow(x, y);
                        }
                    }
                }
            }
            for (int z = 0; z < super.regionSizeZ; z++) {
                for (int y = 0; y <= super.regionSizeY; y++) {
                    for (int x = 0; x <= super.regionSizeX; x++) {
                        if ((super.cullingsMap[z][x][y] & 0x4) != 0) {
                            int endX = x;
                            int startX = x;
                            int yA = y;
                            int yB;
                            for (yB = y; yA > 0 && (super.cullingsMap[z][x][yA - 1] & 0x4) != 0 && yB - yA < 10; --yA) {
                            }
                            while (yB < super.regionSizeY && (super.cullingsMap[z][x][yB + 1] & 0x4) != 0 && yB - yA < 10) {
                                ++yB;
                            }
                            label112:
                            while (endX > 0 && startX - endX < 10) {
                                for (int yC = yA; yC <= yB; yC++) {
                                    if ((super.cullingsMap[z][endX - 1][yC] & 0x4) == 0) {
                                        break label112;
                                    }
                                }
                                --endX;
                            }
                            label99:
                            while (startX < super.regionSizeX && startX - endX < 10) {
                                for (int yD = yA; yD <= yB; yD++) {
                                    if ((super.cullingsMap[z][startX + 1][yD] & 0x4) == 0) {
                                        break label99;
                                    }
                                }
                                ++startX;
                            }
                            if ((startX - endX + 1) * (yB - yA + 1) >= 4) {
                                int height = super.tileHeights[z][endX][yA];
                                super.scene.culling.createTileCullingCluster(z, endX << 9, (startX << 9) + 512, yA << 9, (yB << 9) + 512, height, height);
                                for (int i_12 = endX; i_12 <= startX; i_12++) {
                                    for (int i_13 = yA; i_13 <= yB; i_13++) {
                                        super.cullingsMap[z][i_12][i_13] &= -5;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            super.scene.culling.clear();
        }
        super.cullingsMap = null;
    }

    public boolean addWall(AbstractToolkit toolkit, int locShape, int locRotation, boolean stationary, LocType locType, int i_6, int locHeight, int locPlane, int sceneX, int averageHeight, int sceneY, int baseX, int baseY, boolean delayShading, Ground currentSurfaceGround, CollisionMap localCollisionMap, int locSizeX, int locSizeY) {
        if (locShape == LocShapes.WALL_STRAIGHT.shape) {
        	int cullable = locType.occlusionMode;
            if (cullingAdjustment && locType.occlusionMode == -1) {
                cullable = 1;
            }
            AbstractWall primaryWall;
            if (stationary) {
                StationaryWall stationaryWall = new StationaryWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation, delayShading);
                if (stationaryWall.hasRenderingShadow()) {
                    stationaryWall.addShadow(toolkit);
                }
                primaryWall = stationaryWall;
            } else {
                primaryWall = new AnimatedWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation, i_6);
            }
            super.scene.addWall(locHeight, baseX, baseY, primaryWall, null);
            if (locRotation == 0) {
                if (super.lowDetailShadows && locType.staticShadow) {
                    currentSurfaceGround.setTileShadow(baseX, baseY, 50);
                    currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50);
                }
                if (cullable == 1 && !super.overlayHidden) {
                    super.scene.culling.addWallCulling(1, locHeight, baseX, baseY, locType.cullY, locType.cullXZ);
                }
            } else if (locRotation == 1) {
                if (super.lowDetailShadows && locType.staticShadow) {
                    currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50);
                    currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50);
                }
                if (cullable == 1 && !super.overlayHidden) {
                    super.scene.culling.addWallCulling(2, locHeight, baseX, baseY + 1, locType.cullY, -locType.cullXZ);
                }
            } else if (locRotation == 2) {
                if (super.lowDetailShadows && locType.staticShadow) {
                    currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50);
                    currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50);
                }
                if (cullable == 1 && !super.overlayHidden) {
                    super.scene.culling.addWallCulling(1, locHeight, baseX + 1, baseY, locType.cullY, -locType.cullXZ);
                }
            } else if (locRotation == 3) {
                if (super.lowDetailShadows && locType.staticShadow) {
                    currentSurfaceGround.setTileShadow(baseX, baseY, 50);
                    currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50);
                }
                if (cullable == 1 && !super.overlayHidden) {
                    super.scene.culling.addWallCulling(2, locHeight, baseX, baseY, locType.cullY, locType.cullXZ);
                }
            }
            if (locType.blocksMovement != 0 && localCollisionMap != null) {
                localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
            }
            if (locType.decorDisplacement != 64) {
                super.scene.offsetWallDecoration(locHeight, baseX, baseY, locType.decorDisplacement);
            }
            return true;
        } else {          
            if (locShape == LocShapes.WALL_DIAGONAL_CORNER.shape) {
            	AbstractWall wall;
                if (stationary) {
                	StationaryWall stationaryWall = new StationaryWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation, delayShading);
                    if (stationaryWall.hasRenderingShadow()) {
                        stationaryWall.addShadow(toolkit);
                    }
                    wall = stationaryWall;
                } else {
                    wall = new AnimatedWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation, i_6);
                }
                super.scene.addWall(locHeight, baseX, baseY, wall, null);
                if (locType.staticShadow && super.lowDetailShadows) {
                    if (locRotation == 0) {
                        currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50);
                    } else if (locRotation == 1) {
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50);
                    } else if (locRotation == 2) {
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50);
                    } else if (locRotation == 3) {
                        currentSurfaceGround.setTileShadow(baseX, baseY, 50);
                    }
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                }
                return true;
            } else if (locShape == LocShapes.WALL_WHOLE_CORNER.shape) {
            	int signedRotation = locRotation + 1 & 0x3;
                AbstractWall primaryWall;
                AbstractWall secondaryWall;
                if (stationary) {
                    StationaryWall firstWall = new StationaryWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation + 4, delayShading);
                    StationaryWall secondWall = new StationaryWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, signedRotation, delayShading);
                    if (firstWall.hasRenderingShadow()) {
                        firstWall.addShadow(toolkit);
                    }
                    if (secondWall.hasRenderingShadow()) {
                        secondWall.addShadow(toolkit);
                    }
                    primaryWall = firstWall;
                    secondaryWall = secondWall;
                } else {
                    primaryWall = new AnimatedWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation + 4, i_6);
                    secondaryWall = new AnimatedWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, signedRotation, i_6);
                }
                super.scene.addWall(locHeight, baseX, baseY, primaryWall, secondaryWall);
                if ((locType.occlusionMode == 1 || cullingAdjustment && locType.occlusionMode == -1) && !super.overlayHidden) {
                    if (locRotation == 0) {
                        super.scene.culling.addWallCulling(1, locHeight, baseX, baseY, locType.cullY, locType.cullXZ);
                        super.scene.culling.addWallCulling(2, locHeight, baseX, baseY + 1, locType.cullY, locType.cullXZ);
                    } else if (locRotation == 1) {
                        super.scene.culling.addWallCulling(1, locHeight, baseX + 1, baseY, locType.cullY, locType.cullXZ);
                        super.scene.culling.addWallCulling(2, locHeight, baseX, baseY + 1, locType.cullY, locType.cullXZ);
                    } else if (locRotation == 2) {
                        super.scene.culling.addWallCulling(1, locHeight, baseX + 1, baseY, locType.cullY, locType.cullXZ);
                        super.scene.culling.addWallCulling(2, locHeight, baseX, baseY, locType.cullY, locType.cullXZ);
                    } else if (locRotation == 3) {
                        super.scene.culling.addWallCulling(1, locHeight, baseX, baseY, locType.cullY, locType.cullXZ);
                        super.scene.culling.addWallCulling(2, locHeight, baseX, baseY, locType.cullY, locType.cullXZ);
                    }
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                }
                if (locType.decorDisplacement != 64) {
                    super.scene.offsetWallDecoration(locHeight, baseX, baseY, locType.decorDisplacement);
                }
                return true;
            } else if (locShape == LocShapes.WALL_STRAIGHT_CORNER.shape) {
            	AbstractWall wall;
                if (stationary) {
                    StationaryWall stationaryWall = new StationaryWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation, delayShading);
                    if (stationaryWall.hasRenderingShadow()) {
                        stationaryWall.addShadow(toolkit);
                    }
                    wall = stationaryWall;
                } else {
                    wall = new AnimatedWall(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, locShape, locRotation, i_6);
                }
                super.scene.addWall(locHeight, baseX, baseY, wall, null);
                if (locType.staticShadow && super.lowDetailShadows) {
                    if (locRotation == 0) {
                        currentSurfaceGround.setTileShadow(baseX, baseY + 1, 50);
                    } else if (locRotation == 1) {
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY + 1, 50);
                    } else if (locRotation == 2) {
                        currentSurfaceGround.setTileShadow(baseX + 1, baseY, 50);
                    } else if (locRotation == 3) {
                        currentSurfaceGround.setTileShadow(baseX, baseY, 50);
                    }
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBoundary(baseX, baseY, locShape, locRotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                }
                return true;
            } else if (locShape == LocShapes.WALL_INTERACT.shape) {
            	AbstractInteractiveEntity interactive;
                if (stationary) {
                    StationaryInteractiveObject stationaryInteractive = new StationaryInteractiveObject(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, baseX, baseX, baseY, baseY, locShape, locRotation, delayShading);
                    if (stationaryInteractive.hasRenderingShadow()) {
                        stationaryInteractive.addShadow(toolkit);
                    }
                    interactive = stationaryInteractive;
                } else {
                    interactive = new AnimatedInteractiveObject(super.scene, toolkit, this.locTypeList, locType, locHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, baseX, locSizeX + baseX - 1, baseY, locSizeY + baseY - 1, locShape, locRotation, i_6);
                }
                super.scene.addInteractive(interactive, false);
                if (locType.occlusionMode == 1 && !super.overlayHidden) {
                    byte displacement;
                    if ((locRotation & 0x1) == 0) {
                        displacement = 8;
                    } else {
                        displacement = 16;
                    }
                    super.scene.culling.addWallCulling(displacement, locHeight, baseX, baseY, locType.cullY, 0);
                }
                if (locType.blocksMovement != 0 && localCollisionMap != null) {
                    localCollisionMap.flagBlockedObject(baseX, baseY, locSizeX, locSizeY, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                }
                if (locType.decorDisplacement != 64) {
                    super.scene.offsetWallDecoration(locHeight, baseX, baseY, locType.decorDisplacement);
                }
                return true;
            } 
            return false;
        }
    }

    public boolean addWallDecoration(AbstractToolkit toolkit, int locShape, int locRotation, boolean stationary, LocType locType, int sequenceId, int logicHeight, int locPlane, int sceneX, int averageHeight, int sceneY, int baseX, int baseY) {
        if (locShape == LocShapes.STRAIGHT_INSIDE_WALL_DEC.shape) {
            AbstractWallDecoration wallDecoration;
            if (stationary) {
                StationaryWallDecoration stationaryDecoration = new StationaryWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, 0, 0, locShape, locRotation);
                if (stationaryDecoration.hasRenderingShadow()) {
                    stationaryDecoration.addShadow(toolkit);
                }
                wallDecoration = stationaryDecoration;
            } else {
                wallDecoration = new AnimatedWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, 0, 0, locShape, locRotation, sequenceId);
            }
            super.scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null);
            return true;
        } 
        if (locShape == LocShapes.STRAIGHT_OUSIDE_WALL_DEC.shape) {
        	int decorationDisplacement = 65;
        	GameObject gameObject = (GameObject) super.scene.getWall(logicHeight, baseX, baseY);
            if (gameObject != null) {
                decorationDisplacement = this.locTypeList.list(gameObject.getId()).decorDisplacement + 1;
            }
        	AbstractWallDecoration wallDecoration;
            if (stationary) {
            	StationaryWallDecoration stationaryWallDecoration = new StationaryWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, decorationDisplacement * WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation);
                if (stationaryWallDecoration.hasRenderingShadow()) {
                    stationaryWallDecoration.addShadow(toolkit);
                }
                wallDecoration = stationaryWallDecoration;
            } else {
                wallDecoration = new AnimatedWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, decorationDisplacement * WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation, sequenceId);
            }
            super.scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null);
            return true;
        }
        if (locShape == LocShapes.DIAGONAL_OUTSIDE_WALL_DEC.shape) {
        	int decorationDisplacement = 33;
        	GameObject gameObject = (GameObject) super.scene.getWall(logicHeight, baseX, baseY);
            if (gameObject != null) {
                decorationDisplacement = this.locTypeList.list(gameObject.getId()).decorDisplacement / 2 + 1;
            }
        	AbstractWallDecoration wallDecoration;
            if (stationary) {
            	StationaryWallDecoration stationaryWallDecoration = new StationaryWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, decorationDisplacement * WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4);
                if (stationaryWallDecoration.hasRenderingShadow()) {
                    stationaryWallDecoration.addShadow(toolkit);
                }
                wallDecoration = stationaryWallDecoration;
            } else {
                wallDecoration = new AnimatedWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, decorationDisplacement * ANIMATED_WALL_DECORATION_DISPLACE_X[locRotation], decorationDisplacement * ANIMATED_WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4, sequenceId);
            }
            super.scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null);
            return true;
        } 
        if (locShape == LocShapes.DIAGONAL_INSIDE_WALL_DEC.shape) {
        	int signedRotation = locRotation + 2 & 0x3;
            AbstractWallDecoration wallDecoration;
            if (stationary) {
                StationaryWallDecoration stationaryWallDecoration = new StationaryWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, 0, 0, locShape, signedRotation + 4);
                if (stationaryWallDecoration.hasRenderingShadow()) {
                    stationaryWallDecoration.addShadow(toolkit);
                }
                wallDecoration = stationaryWallDecoration;
            } else {
                wallDecoration = new AnimatedWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, 0, 0, locShape, signedRotation + 4, sequenceId);
            }
            super.scene.addWallDecoration(logicHeight, baseX, baseY, wallDecoration, null);
            return true;
        } 
        if (locShape == LocShapes.DIAGONAL_INWALL_DEC.shape) {
        	int signedRotation = locRotation + 2 & 0x3;
            int displacement = 33;
            GameObject gameObject = (GameObject) super.scene.getWall(logicHeight, baseX, baseY);
            if (gameObject != null) {
                displacement = this.locTypeList.list(gameObject.getId()).decorDisplacement / 2 + 1;
            }
            AbstractWallDecoration primaryWallDecoration;
            AbstractWallDecoration secondaryWallDecoration;
            if (stationary) {
                StationaryWallDecoration primaryStationaryWallDecoration = new StationaryWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, displacement * ANIMATED_WALL_DECORATION_DISPLACE_X[locRotation], displacement * ANIMATED_WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4);
                StationaryWallDecoration secondaryStationaryWallDecoration = new StationaryWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, 0, 0, locShape, signedRotation + 4);
                if (primaryStationaryWallDecoration.hasRenderingShadow()) {
                    primaryStationaryWallDecoration.addShadow(toolkit);
                }
                if (secondaryStationaryWallDecoration.hasRenderingShadow()) {
                    secondaryStationaryWallDecoration.addShadow(toolkit);
                }
                primaryWallDecoration = primaryStationaryWallDecoration;
                secondaryWallDecoration = secondaryStationaryWallDecoration;
            } else {
            	primaryWallDecoration = new AnimatedWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, displacement * ANIMATED_WALL_DECORATION_DISPLACE_X[locRotation], displacement * ANIMATED_WALL_DECORATION_DISPLACE_Y[locRotation], locShape, locRotation + 4, sequenceId);
            	secondaryWallDecoration = new AnimatedWallDecoration(super.scene, toolkit, this.locTypeList, locType, logicHeight, locPlane, sceneX, averageHeight, sceneY, super.overlayHidden, 0, 0, locShape, signedRotation + 4, sequenceId);
            }
            super.scene.addWallDecoration(logicHeight, baseX, baseY, primaryWallDecoration, secondaryWallDecoration);
            return true;
        } 
        return false;            
    }

    public GameObject getGameObject(int z, int type, int x, int y) {
        GameObject gameObject = null;
        if (type == 0) {
            gameObject = (GameObject) super.scene.getWall(z, x, y);
        }
        if (type == 1) {
            gameObject = (GameObject) super.scene.getPrimaryWallDecoration(z, x, y);
        }
        if (type == 2) {
            gameObject = (GameObject) super.scene.getInteractiveObject(z, x, y, client.INTERACTIVE_OBJECT_COMPARABLE);
        }
        if (type == 3) {
            gameObject = (GameObject) super.scene.getGroundDecoration(z, x, y);
        }
        return gameObject;
    }

    public void decodeEffectsClient(AbstractToolkit toolkit, Packet buffer, int i_3, int i_4) {
        if (!super.overlayHidden) {
            boolean hasActiveLights = false;
            Environment atmosphere = null;
            while (true) {
                while (buffer.currentPosition < buffer.buffer.length) {
                    int opcode = buffer.readUnsignedByte();
                    if (opcode == 0) {
                        if (atmosphere == null) {
                            atmosphere = new Environment(buffer, this.environmentManager);
                        } else {
                            atmosphere.decode(buffer, this.environmentManager);
                        }
                    } else {
                        if (opcode == 1) {
                            int count = buffer.readUnsignedByte();
                            if (count > 0) {
                                for (int i_19 = 0; i_19 < count; i_19++) {
                                    FlickeringEffect flickeringEffect = new FlickeringEffect(toolkit, super.scene.tileUnits, buffer);
                                    if (flickeringEffect.type == 31) {
                                        LightType lightType = TypeLists.LIGHT.list(buffer.readUnsignedShort());
                                        flickeringEffect.setType(lightType.effect, lightType.duration, lightType.ticker, lightType.surrounding);
                                    }
                                    if (toolkit.getMaxLights() > 0) {
                                        PointLight lightSource = flickeringEffect.pointLight;
                                        int i_13 = lightSource.getX() + (i_3 << 9);
                                        int i_14 = lightSource.getZ() + (i_4 << 9);
                                        int i_15 = i_13 >> 9;
                                        int i_16 = i_14 >> 9;
                                        if (i_15 >= 0 && i_16 >= 0 && i_15 < super.regionSizeX && i_16 < super.regionSizeY) {
                                            lightSource.setPosition(i_13, super.tileHeights[flickeringEffect.level][i_15][i_16] - lightSource.getY(), i_14);
                                            super.scene.calculateLightUid(flickeringEffect);
                                        }
                                    }
                                }
                            }
                        } else if (opcode == 2) {
                            if (atmosphere == null) {
                                atmosphere = new Environment();
                            }
                            atmosphere.decodeHdr(buffer);
                        } else if (opcode == 128) {
                            if (atmosphere == null) {
                                atmosphere = new Environment();
                            }
                            atmosphere.decodeSkybox(buffer, this.environmentManager);
                        } else {
                            if (opcode != 129) {
                                throw new IllegalStateException("");
                            }
                            if (super.lighting == null) {
                                super.lighting = new byte[4][][];
                            }
                            for (int plane = 0; plane < 4; plane++) {
                                byte lightingDecodeType = buffer.readByte();
                                if (lightingDecodeType == 0 && super.lighting[plane] != null) {
                                    int minX = i_3;
                                    int maxX = i_3 + 64;
                                    int minY = i_4;
                                    int maxY = i_4 + 64;
                                    if (i_3 < 0) {
                                        minX = 0;
                                    } else if (i_3 >= super.regionSizeX) {
                                        minX = super.regionSizeX;
                                    }
                                    if (maxX < 0) {
                                        maxX = 0;
                                    } else if (maxX >= super.regionSizeX) {
                                        maxX = super.regionSizeX;
                                    }
                                    if (i_4 < 0) {
                                        minY = 0;
                                    } else if (i_4 >= super.regionSizeY) {
                                        minY = super.regionSizeY;
                                    }
                                    if (maxY < 0) {
                                        maxY = 0;
                                    } else if (maxY >= super.regionSizeY) {
                                        maxY = super.regionSizeY;
                                    }
                                    while (minX < maxX) {
                                        while (minY < maxY) {
                                            super.lighting[plane][minX][minY] = 0;
                                            minY++;
                                        }
                                        minX++;
                                    }
                                } else if (lightingDecodeType == 1) {
                                    if (super.lighting[plane] == null) {
                                        super.lighting[plane] = new byte[super.regionSizeX + 1][super.regionSizeY + 1];
                                    }
                                    for (int i_11 = 0; i_11 < 64; i_11 += 4) {
                                        for (int i_12 = 0; i_12 < 64; i_12 += 4) {
                                            byte b_20 = buffer.readByte();
                                            for (int i_14 = i_3 + i_11; i_14 < i_3 + i_11 + 4; i_14++) {
                                                for (int i_15 = i_12 + i_4; i_15 < i_12 + i_4 + 4; i_15++) {
                                                    if (i_14 >= 0 && i_14 < super.regionSizeX && i_15 >= 0 && i_15 < super.regionSizeY) {
                                                        super.lighting[plane][i_14][i_15] = b_20;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (lightingDecodeType == 2) {
                                    if (super.lighting[plane] == null) {
                                        super.lighting[plane] = new byte[super.regionSizeX + 1][super.regionSizeY + 1];
                                    }
                                    if (plane > 0) {
                                        int minX = i_3;
                                        int maxX = i_3 + 64;
                                        int minY = i_4;
                                        int maxY = i_4 + 64;
                                        if (i_3 < 0) {
                                            minX = 0;
                                        } else if (i_3 >= super.regionSizeX) {
                                            minX = super.regionSizeX;
                                        }
                                        if (maxX < 0) {
                                            maxX = 0;
                                        } else if (maxX >= super.regionSizeX) {
                                            maxX = super.regionSizeX;
                                        }
                                        if (i_4 < 0) {
                                            minY = 0;
                                        } else if (i_4 >= super.regionSizeY) {
                                            minY = super.regionSizeY;
                                        }
                                        if (maxY < 0) {
                                            maxY = 0;
                                        } else if (maxY >= super.regionSizeY) {
                                            maxY = super.regionSizeY;
                                        }
                                        while (minX < maxX) {
                                            while (minY < maxY) {
                                                super.lighting[plane][minX][minY] = super.lighting[plane - 1][minX][minY];
                                                minY++;
                                            }
                                            minX++;
                                        }
                                    }
                                }
                            }
                            hasActiveLights = true;
                        }
                    }
                }
                if (atmosphere != null) {
                    for (int opcode = 0; opcode < 8; opcode++) {
                        for (int i_9 = 0; i_9 < 8; i_9++) {
                            int sceneChunkX = opcode + (i_3 >> 3);
                            int sceneChunkY = i_9 + (i_4 >> 3);
                            if (sceneChunkX >= 0 && sceneChunkX < super.regionSizeX >> 3 && sceneChunkY >= 0 && sceneChunkY < super.regionSizeY >> 3) {
                                this.environmentManager.addChunkEnvironment(sceneChunkX, sceneChunkY, atmosphere);
                            }
                        }
                    }
                }
                if (!hasActiveLights && super.lighting != null) {
                    for (int lightZ = 0; lightZ < 4; lightZ++) {
                        if (super.lighting[lightZ] != null) {
                            for (int sceneChunkX = 0; sceneChunkX < 16; sceneChunkX++) {
                                for (int sceneChunkY = 0; sceneChunkY < 16; sceneChunkY++) {
                                    int lightSceneX = sceneChunkX + (i_3 >> 2);
                                    int lightSceneY = sceneChunkY + (i_4 >> 2);
                                    if (lightSceneX >= 0 && lightSceneX < 26 && lightSceneY >= 0 && lightSceneY < 26) {
                                        super.lighting[lightZ][lightSceneX][lightSceneY] = 0;
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    public void removeGameObject(AbstractToolkit toolkit, int z, int gameObjectType, int x, int y, CollisionMap clipping) {
        GameObject gameObject = getGameObject(z, gameObjectType, x, y);
        if (gameObject != null) {
            LocType locType = this.locTypeList.list(gameObject.getId());
            int type = gameObject.getShape();
            int rotation = gameObject.getRotation();
            if (locType.hasSound()) {
                AmbientSound.unlinkLocSound(z, x, y, locType);
            }
            if (gameObject.hasRenderingShadow()) {
                gameObject.removeShadow(toolkit);
            }
            if (gameObjectType == 0) {
                super.scene.removePrimaryWall(z, x, y);
                super.scene.removeSecondaryWall(z, x, y);
                if (locType.blocksMovement != 0) {
                    clipping.unflagBoundary(x, y, type, rotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                }
                if (locType.occlusionMode == 1) {
                    if (rotation == 0) {
                        super.scene.culling.removeCullingCluster(1, z, x, y);
                    } else if (rotation == 1) {
                        super.scene.culling.removeCullingCluster(2, z, x, y + 1);
                    } else if (rotation == 2) {
                        super.scene.culling.removeCullingCluster(1, z, x + 1, y);
                    } else if (rotation == 3) {
                        super.scene.culling.removeCullingCluster(2, z, x, y);
                    }
                }
            } else if (gameObjectType == 1) {
                super.scene.removePrimaryWallDecoration(z, x, y);
                super.scene.removeSecondaryWallDecoration(z, x, y);
            } else if (gameObjectType == 2) {
                super.scene.removeInteractiveOnCondition(z, x, y, client.INTERACTIVE_OBJECT_COMPARABLE);
                if (locType.blocksMovement != 0 && x + locType.sizeX < super.regionSizeX && y + locType.sizeX < super.regionSizeY && x + locType.sizeY < super.regionSizeX && y + locType.sizeY < super.regionSizeY) {
                    clipping.unflagBlocked(x, y, locType.sizeX, locType.sizeY, rotation, locType.blocksProjectiles, !locType.ignoreClipOnAltRoute);
                }
                if (type == LocShapes.WALL_INTERACT.shape) {
                    if ((rotation & 0x1) == 0) {
                        super.scene.culling.removeCullingCluster(8, z, x, y);
                    } else {
                        super.scene.culling.removeCullingCluster(16, z, x, y);
                    }
                }
            } else if (gameObjectType == 3) {
                super.scene.removeGroundDecoration(z, x, y);
                if (locType.blocksMovement == 1) {
                    clipping.unflagFloorDecoration(x, y);
                }
            }
        }
    }

	public static int loadLocs(LocTypeList locTypeList, byte[] mapdata, int x, int y, int regionSizeX, int regionSizeY) {
	    int incompleteLocs = 0;
	    Packet stream = new Packet(mapdata);
	    int locId = -1;
	    label56:
	    while (true) {
	        int idOffset = stream.readSum();
	        if (idOffset == 0) {
	            return incompleteLocs;
	        }
	        locId += idOffset;
	        int localPositionInfo = 0;
	        boolean cached = false;
	        while (true) {
	            int offset;
	            while (!cached) {
	                offset = stream.readUnsignedSmart();
	                if (offset == 0) {
	                    continue label56;
	                }
	                localPositionInfo += offset - 1;
	                int deltaY = localPositionInfo & 0x3f;
	                int deltaX = localPositionInfo >> 6 & 0x3f;
	                int type = stream.readUnsignedByte() >> 2;
	                int sceneX = deltaX + x;
	                int sceneY = y + deltaY;
	                if (sceneX > 0 && sceneY > 0 && sceneX < regionSizeX - 1 && sceneY < regionSizeY - 1) {
	                    LocType loctype = locTypeList.list(locId);
	                    if (type != LocShapes.GROUND_DECORATION.shape || PreferenceManager.preferences.groundDecoration.getValue() != 0
	                    		|| loctype.hasActions != 0 || loctype.blocksMovement == 1 || loctype.forceDisplayDecoration) {
	                        if (!loctype.fullyCached()) {
	                            incompleteLocs++;
	                        }
	                        cached = true;
	                    }
	                }
	            }
	            offset = stream.readUnsignedSmart();
	            if (offset == 0) {
	                break;
	            }
	            stream.readUnsignedByte();
	        }
	    }
	}

	public static void animateObject(int plane, int x, int y, int type, int shape, int rotation, int sequence) {
	    if (x >= 0 && y >= 0 && x < Resource.mapRegion.getSceneSizeX() - 1 && y < Resource.mapRegion.getSceneSizeY() - 1 && Resource.mapRegion.getScene() != null) {
	        if (type == 0) {
	        	GameObject primaryWall = (GameObject) Resource.mapRegion.getScene().getWall(plane, x, y);
	            GameObject secondaryWall = (GameObject) Resource.mapRegion.getScene().getSecondaryWall(plane, x, y);
	            if (primaryWall != null && shape != 2) {
	                if (primaryWall instanceof AnimatedWall) {
	                    ((AnimatedWall) primaryWall).gameObject.animate(sequence);
	                } else {
	                    MapRegion.transformObject(plane, 0, x, y, primaryWall.getId(), rotation, shape, sequence);
	                }
	            }
	            if (secondaryWall != null) {
	                if (secondaryWall instanceof AnimatedWall) {
	                    ((AnimatedWall) secondaryWall).gameObject.animate(sequence);
	                } else {
	                    MapRegion.transformObject(plane, type, x, y, secondaryWall.getId(), rotation, shape, sequence);
	                }
	            }
	        } else if (type == 1) {
	        	GameObject wallDecoration = (GameObject) Resource.mapRegion.getScene().getPrimaryWallDecoration(plane, x, y);
	            if (wallDecoration != null) {
	                if (wallDecoration instanceof AnimatedWallDecoration) {
	                    ((AnimatedWallDecoration) wallDecoration).wallDecoration.animate(sequence);
	                } else {
	                    int id = wallDecoration.getId();
	                    if (shape != 4 && shape != 5) {
	                        if (shape == 6) {
	                            MapRegion.transformObject(plane, 1, x, y, id, rotation + 4, 4, sequence);
	                        } else if (shape == 7) {
	                            MapRegion.transformObject(plane, type, x, y, id, (rotation + 2 & 0x3) + 4, 4, sequence);
	                        } else if (shape == 8) {
	                            MapRegion.transformObject(plane, type, x, y, id, rotation + 4, 4, sequence);
	                            MapRegion.transformObject(plane, type, x, y, id, (rotation + 2 & 0x3) + 4, 4, sequence);
	                        }
	                    } else {
	                        MapRegion.transformObject(plane, type, x, y, id, rotation, 4, sequence);
	                    }
	                }
	            }
	        } else if (type == 2) {
	        	GameObject interactive = (GameObject) Resource.mapRegion.getScene().getInteractiveObject(plane, x, y, client.INTERACTIVE_OBJECT_COMPARABLE);
	            if (interactive != null) {
	                if (shape == 11) {
	                    shape = 10;
	                }
	                if (interactive instanceof AnimatedInteractiveObject) {
	                    ((AnimatedInteractiveObject) interactive).activeGameObject.animate(sequence);
	                } else {
	                    MapRegion.transformObject(plane, 2, x, y, interactive.getId(), rotation, shape, sequence);
	                }
	            }
	        } else if (type == 3) {
	        	GameObject groundDecoration = (GameObject) Resource.mapRegion.getScene().getGroundDecoration(plane, x, y);
	            if (groundDecoration != null) {
	                if (groundDecoration instanceof AnimatedGroundDecoration) {
	                    ((AnimatedGroundDecoration) groundDecoration).gameObject.animate(sequence);
	                } else {
	                    MapRegion.transformObject(plane, 3, x, y, groundDecoration.getId(), rotation, shape, sequence);
	                }
	            }
	        }
	    }
	}
}
