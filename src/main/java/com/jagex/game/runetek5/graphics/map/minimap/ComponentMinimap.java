package com.jagex.game.runetek5.graphics.map.minimap;

import com.jagex.game.runetek5.cache.node.IntNode;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.object.ObjectShapes;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.object.ObjectTypeList;
import com.jagex.game.runetek5.config.mapsprite.MapSpriteType;
import com.jagex.game.runetek5.config.mec.MECType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.config.staticelements.StaticElements;
import com.jagex.game.runetek5.entity.gameobject.GameObject;
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.framebuffer.RenderBuffer;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.hint.HintRenderer;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapRasterManager;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.imageproducer.DirectDisplay;
import com.jagex.game.runetek5.interaction.Friend;
import com.jagex.game.runetek5.interaction.FriendList;
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.movement.CoordGrid;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.CollisionFlag;
import com.jagex.game.runetek5.scene.CollisionMap;
import com.jagex.game.runetek5.scene.MapRegion;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.scene.SettingsBits;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.TimeTools;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.utils.Utils;
import com.jagex.game.runetek5.vardomain.VarProviders;

public class ComponentMinimap {
	public static boolean minimapContainsFlag = true;
	public static int minimapFlagX = -1;
    public static int minimapFlagY = -1;
    
	public static final int[] minimapLocSceneX = new int[1014];
	public static final int[] minimapLocSceneY = new int[1001];
	public static final int[] minimapLocId = new int[1010];
	public static int minimapLocCount;
	public static NativeSprite minimapBackground;
	public static int MAP_PLANE = -1;
	public static int minimapState;
	public static int mapAssetLoadingPercentage;

    public static void resetMinimapFlagAndPlane() {
        minimapState = 0;
        MAP_PLANE = -1;
        minimapFlagX = -1;
        minimapFlagY = -1;
    }

    public static void resetMinimapFlag() {
        minimapState = 0;
        minimapFlagX = -1;
        minimapFlagY = -1;
    }

    public static void drawLocOnMinimap(AbstractToolkit toolkit, int plane, int sceneX, int sceneY, int x, int y, int colorNoActions, int colorHasActions) {
	    Scene scene = Resource.mapRegion.getScene();
	    GameObject sceneObject = (GameObject) scene.getWall(plane, sceneX, sceneY);
	    if (sceneObject != null) {
	        ObjectType locType = Resource.mapRegion.getLoctypeList().list(sceneObject.getId());
	        int rotation = sceneObject.getRotation() & 0x3;
	        int shape = sceneObject.getShape();
	        if (locType.mapIconId != -1) {
	            ComponentMinimap.drawMapIcon(toolkit, locType, rotation, x, y);
	        } else {
	        	int color = colorNoActions;
	            if (locType.hasActions > 0) {
	                color = colorHasActions;
	            }
	            if (shape == ObjectShapes.WALL_STRAIGHT.shape || shape == ObjectShapes.WALL_WHOLE_CORNER.shape) {
	                if (rotation == 0) {
	                    toolkit.drawOpaqueVerticalLine(x, y, 4, color);
	                } else if (rotation == 1) {
	                    toolkit.drawOpaqueHorizontalLine(x, y, 4, color);
	                } else if (rotation == 2) {
	                    toolkit.drawOpaqueVerticalLine(x + 3, y, 4, color);
	                } else {
	                    toolkit.drawOpaqueHorizontalLine(x, y + 3, 4, color);
	                }
	            }
	            if (shape == ObjectShapes.WALL_STRAIGHT_CORNER.shape) {
	                if (rotation == 0) {
	                    toolkit.clearPixels(x, y, 1, 1, color);
	                } else if (rotation == 1) {
	                    toolkit.clearPixels(x + 3, y, 1, 1, color);
	                } else if (rotation == 2) {
	                    toolkit.clearPixels(x + 3, y + 3, 1, 1, color);
	                } else {
	                    toolkit.clearPixels(x, y + 3, 1, 1, color);
	                }
	            }
	            if (shape == ObjectShapes.WALL_WHOLE_CORNER.shape) {
	                if (rotation == 0) {
	                    toolkit.drawOpaqueHorizontalLine(x, y, 4, color);
	                } else if (rotation == 1) {
	                    toolkit.drawOpaqueVerticalLine(x + 3, y, 4, color);
	                } else if (rotation == 2) {
	                    toolkit.drawOpaqueHorizontalLine(x, y + 3, 4, color);
	                } else {
	                    toolkit.drawOpaqueVerticalLine(x, y, 4, color);
	                }
	            }
	        }
	    }
	    sceneObject = (GameObject) scene.getInteractiveObject(plane, sceneX, sceneY, client.INTERACTIVE_OBJECT_COMPARABLE);
	    if (sceneObject != null) {
	    	ObjectType loctype = Resource.mapRegion.getLoctypeList().list(sceneObject.getId());
	    	int rotation = sceneObject.getRotation() & 0x3;
	    	int type = sceneObject.getShape();
	        if (loctype.mapIconId != -1) {
	            ComponentMinimap.drawMapIcon(toolkit, loctype, rotation, x, y);
	        } else if (type == ObjectShapes.WALL_INTERACT.shape) {
	        	int color = -1118482;
	            if (loctype.hasActions > 0) {
	                color = -1179648;
	            }
	            if (rotation != 0 && rotation != 2) {
	                toolkit.drawOpaqueLine(x, y, x + 3, y + 3, color);
	            } else {
	                toolkit.drawOpaqueLine(x, y + 3, x + 3, y, color);
	            }
	        }
	    }
	    sceneObject = (GameObject) scene.getGroundDecoration(plane, sceneX, sceneY);
	    if (sceneObject != null) {
	        ObjectType locType = Resource.mapRegion.getLoctypeList().list(sceneObject.getId());
	        int rotation = sceneObject.getRotation() & 0x3;
	        if (locType.mapIconId != -1) {
	            ComponentMinimap.drawMapIcon(toolkit, locType, rotation, x, y);
	        }
	    }
	}

    /**
     * Draws the inner part of the minimap that displays
     * the world and entites in it.
     * Does not include border
     */
	public static void drawMinimap(AbstractToolkit toolkit, Component component, int clipLeft, int clipTop) {
	    InteractiveComponentSprite sprite = component.addInteractiveSprite(toolkit);
	    if (sprite != null) {
	        ComponentSurface dimension = sprite.surface;
	        toolkit.setClipping(clipLeft, clipTop, clipLeft + component.width, clipTop + component.height);
	        if (sprite.width != component.width || sprite.height != component.height) {
	            throw new IllegalStateException("");
	        }
	
	        if (ComponentMinimap.minimapState != 2 && ComponentMinimap.minimapState != 5 && ComponentMinimap.minimapBackground != null) {
	            CoordGrid baseCoordinates = Resource.mapRegion.getBaseCoordinates();
	            int absX;
	            int absY;
	            int rotation;
	            int i_11;
	            if (Camera.cameraCalculationType == 4) {
	                absX = client.anInt7262;
	                absY = client.anInt7376;
	                rotation = (int) (-Camera.camAngleY) & 0x3fff;
	                i_11 = 4096;
	            } else {
	                Vector3 vector322 = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
	                absX = (int) vector322.x;
	                absY = (int) vector322.z;
	                rotation = (int) (-Camera.camAngleY) + client.minimapRotation & 0x3fff;
	                i_11 = 4096 - client.minimapZoom * 16;
	            }
	
	            int i_12 = absX / 128 + 48;
	            int i_13 = 48 + Resource.mapRegion.getSceneSizeY() * 4 - absY / 128;
	            ComponentMinimap.minimapBackground.rotate(clipLeft + component.width / 2.0F, clipTop + component.height / 2.0F, i_12, i_13, i_11, rotation << 2, dimension, clipLeft, clipTop);
	            StaticElements staticElements = Resource.mapRegion.getStaticElements();
	
	            for (IntNode class282Sub3815 = (IntNode) MapRegion.staticElementsAwaitingDrawing.getFirst(); class282Sub3815 != null; class282Sub3815 = (IntNode) MapRegion.staticElementsAwaitingDrawing.next()) {
	            	int index = class282Sub3815.value;
	            	int sceneX = (staticElements.bitpackedPlacement[index] >> 14 & 0x3fff) - baseCoordinates.x;
	            	int sceneY = (staticElements.bitpackedPlacement[index] & 0x3fff) - baseCoordinates.y;
	            	int i_19 = sceneX * 4 + 2 - absX / 128;
	            	int i_20 = sceneY * 4 + 2 - absY / 128;
	                ComponentMinimap.drawMec(toolkit, dimension, component, clipLeft, clipTop, i_19, i_20, staticElements.mapCategoryIds[index]);
	            }
	
	            for (int index = 0; index < minimapLocCount; index++) {
	            	int i_16 = minimapLocSceneX[index] * 4 + 2 - absX / 128;
	            	int i_17 = minimapLocSceneY[index] * 4 + 2 - absY / 128;
	                ObjectType locType = Resource.mapRegion.getLoctypeList().list(minimapLocId[index]);
	                if (locType.alternativeIds != null) {
	                    locType = locType.getMultiLoc(VarProviders.PLAYER_VAR_PROVIDER);
	                    if (locType == null || locType.mapCategoryid == -1) {
	                        continue;
	                    }
	                }
	
	                ComponentMinimap.drawMec(toolkit, dimension, component, clipLeft, clipTop, i_16, i_17, locType.mapCategoryid);
	            }
	
	            for (GroundItemWrapper groundItem = client.GROUND_ITEMS.first(); groundItem != null; groundItem = client.GROUND_ITEMS.next()) {
	            	int plane = (int) (groundItem.pointer >> 28 & 0x3L);
	                if (plane == ComponentMinimap.MAP_PLANE) {
	                	int i_17 = (int) (groundItem.pointer & 0x3fffL) - baseCoordinates.x;
	                	int i_18 = (int) (groundItem.pointer >> 14 & 0x3fffL) - baseCoordinates.y;
	                	int sizeX = i_17 * 4 + 2 - absX / 128;
	                	int sizeZ = i_18 * 4 + 2 - absY / 128;
	                    ComponentMinimap.drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[0]);
	                }
	            }
	
	            ComponentMinimap.drawNpcs(toolkit, absX, absY, component, dimension, clipLeft, clipTop);
	            ComponentMinimap.drawPlayers(absX, absY, component, dimension, clipLeft, clipTop);
	            HintRenderer.method3151(absX, absY, component, sprite, clipLeft, clipTop);
	            if (Camera.cameraCalculationType != 4) {
	                if (minimapFlagX != 0) {
	                	int sizeX = minimapFlagX * 4 + 2 - absX / 128 + (PlayerUpdate.MY_PLAYER.getSize() - 1) * 2;
	                    int sizeY = 2 + minimapFlagY * 4 - absY / 128 + (PlayerUpdate.MY_PLAYER.getSize() - 1) * 2;
	                    ComponentMinimap.drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeY, StaticMedia.mapflagSprites[minimapContainsFlag ? 1 : 0]);
	                }
	
	                if (!PlayerUpdate.MY_PLAYER.hidden) {
	                    toolkit.clearPixels(clipLeft + component.width / 2 - 1, clipTop + component.height / 2 - 1, 3, 3, -1);
	                }
	            }
	        } else {
	            toolkit.clearRectangle(dimension, clipLeft, clipTop);
	        }
	    }	
	}

	// is it size_x/y or is it offset_x/y
	public static void drawEntity(Component component, ComponentSurface dimension, int clipLeft, int clipTop, int sizeX, int sizeZ, NativeSprite sprite) {
	    if (sprite != null) {
	        int rotation;
	        if (Camera.cameraCalculationType == 4) {
	            rotation = (int) Camera.camAngleY & 0x3fff;
	        } else {
	            rotation = (int) Camera.camAngleY + client.minimapRotation & 0x3fff;
	        }
	
	        int largestSize = Math.max(component.width / 2, component.height / 2) + 10;
	        int size2d = sizeZ * sizeZ + sizeX * sizeX;
	        if (size2d <= largestSize * largestSize) {
	            int xan = MathUtils.SINE[rotation];
	            int zan = MathUtils.COSINE[rotation];
	            if (Camera.cameraCalculationType != 4) {
	                xan = xan * 256 / (client.minimapZoom + 256);
	                zan = zan * 256 / (client.minimapZoom + 256);
	            }
	
	            int w = zan * sizeX + sizeZ * xan >> 14;
	            int h = sizeZ * zan - xan * sizeX >> 14;
	            sprite.method2773(w + component.width / 2 + clipLeft - sprite.getSpriteWidth() / 2, clipTop + component.height / 2 - h - sprite.getSpriteHeight() / 2, dimension, clipLeft, clipTop);
	        }
	    }	
	}

	public static void drawMapIcon(AbstractToolkit toolkit, ObjectType locType, int rotation, int drawPositionX, int drawPositionY) {
	    MapSpriteType mapspriteType = TypeLists.MAP_SPRITE.list(locType.mapIconId);
	    if (mapspriteType.fileId != -1) {
	        if (locType.mapIconRotates) {
	            rotation += locType.mapIconRotation;
	            rotation &= 0x3;
	        } else {
	            rotation = 0;
	        }
	        NativeSprite nativeSprite = mapspriteType.createRotatedNativeSprite(toolkit, rotation, locType.mapIconFlipped);
	        if (nativeSprite != null) {
	            int sizeX = locType.sizeX;
	            int sizeY = locType.sizeY;
	            if ((rotation & 0x1) == 1) {
	                sizeX = locType.sizeY;
	                sizeY = locType.sizeX;
	            }
	            int upscaledSizeX = nativeSprite.getSpriteWidth();
	            int upscaledSizeY = nativeSprite.getSpriteHeight();
	            if (mapspriteType.requiresUpscaling) {
	                upscaledSizeX = sizeX * 4;
	                upscaledSizeY = sizeY * 4;
	            }
	            
	            if (mapspriteType.backgroundColor != 0) {
	                nativeSprite.drawCircle(drawPositionX, drawPositionY - (sizeY - 1) * 4, upscaledSizeX, upscaledSizeY, 0, ~0xffffff | mapspriteType.backgroundColor, 1);
	            } else {
	                nativeSprite.drawCircle(drawPositionX, drawPositionY - (sizeY - 1) * 4, upscaledSizeX, upscaledSizeY);
	            }
	        }
	    }
	}

	public static int anInt4097;

	public static void reset() {
	    minimapBackground = null;
	    MAP_PLANE = -1;
	}

	public static NativeSprite aNativeSprite5883;

	public static void drawCompass(Component component, int clipLeft, int clipTop) {
	    InteractiveComponentSprite sprite = component.addInteractiveSprite(Renderers.activeToolkit);
	    if (sprite != null) {
	        Renderers.activeToolkit.setClipping(clipLeft, clipTop, component.width + clipLeft, clipTop + component.height);
	        if (minimapState < 3) {
	            int i_5 = (int) (-Camera.camAngleY);
	            i_5 = i_5 + client.minimapRotation * 2 & 0x3fff;
	            i_5 <<= 2;
	            StaticMedia.compassSprite.drawRotatedSprite(component.width / 2.0F + clipLeft, component.height / 2.0F + clipTop, i_5, sprite.surface, clipLeft, clipTop);
	        } else {
	            Renderers.activeToolkit.clearRectangle(sprite.surface, clipLeft, clipTop);
	        }
	    }
	}

	public static void drawPlayers(int absX, int absY, Component component, ComponentSurface dimension, int clipLeft, int clipTop) {
	    int numPlayerIndices = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
	    int[] playerIndices = PlayerUpdate.OUT_PLAYER_INDICES;
	    for (int playerIndex = 0; playerIndex < numPlayerIndices; playerIndex++) {
	        PlayerEntity player = PlayerUpdate.players[playerIndices[playerIndex]];
	        if (player != null && player.hasAppearance() && !player.hidden && player != PlayerUpdate.MY_PLAYER && player.plane == PlayerUpdate.MY_PLAYER.plane) {
	            Vector3 absolutePosition = player.getAbsolutePosition().coords;
	            int sizeX = (int) absolutePosition.x / 128 - absX / 128;
	            int sizeZ = (int) absolutePosition.z / 128 - absY / 128;
	            boolean isFriend = false;
	            for (int friendIndex = 0; friendIndex < FriendList.FRIEND_COUNT; friendIndex++) {
	                Friend friend = FriendList.FRIENDS[friendIndex];
	                if (player.displayName.equals(friend.displayName) && friend.worldId != 0) {
	                    isFriend = true;
	                    break;
	                }
	            }
	            boolean inFriendschat = false;
	            for (int fcPlayerIndex = 0; fcPlayerIndex < FriendsChat.FC_PLAYER_COUNT; fcPlayerIndex++) {
	                if (player.displayName.equals(FriendsChat.FC_PLAYERS[fcPlayerIndex].username)) {
	                    inFriendschat = true;
	                    break;
	                }
	            }
	            boolean onSameTeam = player.teamId != 0 && player.teamId == PlayerUpdate.MY_PLAYER.teamId;
                if (player.pMapdot) {
	                drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[6]);
	            } else if (onSameTeam) {
	                drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[4]);
	            } else if (player.reduceAttackPriority) {
	                drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[7]);
	            } else if (isFriend) {
	                drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[3]);
	            } else if (inFriendschat) {
	                drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[5]);
	            } else {
	                drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[2]);
	            }
	        }
	    }
	}

	public static void drawNpcs(AbstractToolkit toolkit, int offsetX, int offsetY, Component component, ComponentSurface dimension, int clipLeft, int clipTop) {
	    for (int npcIndex = 0; npcIndex < client.NPC_UPDATE_INDEX; npcIndex++) {
	        ObjectNode node = client.NPC_MAP.get(client.NPC_UPDATE_INDICES[npcIndex]);
	        if (node != null) {
	            NpcEntity npcEntity = (NpcEntity) node.value;
	            if (npcEntity.hasDefinition() && npcEntity.plane == PlayerUpdate.MY_PLAYER.plane) {
	                NPCType npctype = npcEntity.definitions;
	                if (npctype != null && npctype.alternativeIds != null) {
	                    npctype = npctype.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
	                }
	
	                if (npctype != null && npctype.displayOnMinimap && npctype.visible) {
	                    Vector3 abs = npcEntity.getAbsolutePosition().coords;
	                    int sizeX = (int) abs.x / 128 - offsetX / 128;
	                    int sizeZ = (int) abs.z / 128 - offsetY / 128;
	                    if (npctype.mecId != -1) {
	                        drawMec(toolkit, dimension, component, clipLeft, clipTop, sizeX, sizeZ, npctype.mecId);
	                    } else {
	                        drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites[1]);
	                    }
	                }
	            }
	        }
	    }
	
	}

	public static void drawMec(AbstractToolkit toolkit, ComponentSurface class4551, Component component, int clipLeft, int clipTop, int i_5, int i_6, int mecId) {
	    MECType mecType = TypeLists.MEC.list(mecId);
	    if (mecType != null && mecType.displayedOnMinimap && mecType.shouldDraw(VarProviders.PLAYER_VAR_PROVIDER)) {
	        int i_11;
	        if (mecType.offsets != null) {
	            int[] ints_10 = new int[mecType.offsets.length];
	            int rotation;
	            int i_13;
	            int i_14;
	            for (i_11 = 0; i_11 < ints_10.length / 2; i_11++) {
	                if (Camera.cameraCalculationType == 4) {
	                    rotation = (int) Camera.camAngleY & 0x3fff;
	                } else {
	                    rotation = (int) Camera.camAngleY + client.minimapRotation & 0x3fff;
	                }
	                i_13 = MathUtils.SINE[rotation];
	                i_14 = MathUtils.COSINE[rotation];
	                if (Camera.cameraCalculationType != 4) {
	                    i_13 = i_13 * 256 / (client.minimapZoom + 256);
	                    i_14 = i_14 * 256 / (client.minimapZoom + 256);
	                }
	                ints_10[i_11 * 2] = clipLeft + component.width / 2 + ((mecType.offsets[i_11 * 2 + 1] * 4 + i_6) * i_13 + i_14 * (i_5 + mecType.offsets[i_11 * 2] * 4) >> 14);
	                ints_10[i_11 * 2 + 1] = component.height / 2 + clipTop - ((mecType.offsets[i_11 * 2 + 1] * 4 + i_6) * i_14 - i_13 * (i_5 + mecType.offsets[i_11 * 2] * 4) >> 14);
	            }
	            InteractiveComponentSprite class11919 = component.addInteractiveSprite(toolkit);
	            if (class11919 != null) {
	                WorldMapRasterManager.method2505(toolkit, ints_10, mecType.lineColor, class11919.rowStartX, class11919.rowWidth);
	            }
	            if (mecType.anInt2748 > 0) {
	                int i_15;
	                int i_16;
	                int i_17;
	                for (rotation = 0; rotation < ints_10.length / 2 - 1; rotation++) {
	                    i_13 = ints_10[rotation * 2];
	                    i_14 = ints_10[rotation * 2 + 1];
	                    i_15 = ints_10[(rotation + 1) * 2];
	                    i_16 = ints_10[(rotation + 1) * 2 + 1];
	                    if (i_15 < i_13) {
	                        i_17 = i_13;
	                        int i_18 = i_14;
	                        i_13 = i_15;
	                        i_14 = i_16;
	                        i_15 = i_17;
	                        i_16 = i_18;
	                    } else if (i_13 == i_15 && i_16 < i_14) {
	                        i_17 = i_14;
	                        i_14 = i_16;
	                        i_16 = i_17;
	                    }
	                    toolkit.drawTexturedLine(i_13, i_14, i_15, i_16, mecType.colors[mecType.colorPointers[rotation] & 0xff], class4551, clipLeft, clipTop, mecType.anInt2748, mecType.anInt2749, mecType.anInt2756);
	                }
	                rotation = ints_10[ints_10.length - 2];
	                i_13 = ints_10[ints_10.length - 1];
	                i_14 = ints_10[0];
	                i_15 = ints_10[1];
	                if (i_14 < rotation) {
	                    i_16 = rotation;
	                    i_17 = i_13;
	                    rotation = i_14;
	                    i_13 = i_15;
	                    i_14 = i_16;
	                    i_15 = i_17;
	                } else if (i_14 == rotation && i_15 < i_13) {
	                    i_16 = i_13;
	                    i_13 = i_15;
	                    i_15 = i_16;
	                }
	                toolkit.drawTexturedLine(rotation, i_13, i_14, i_15, mecType.colors[mecType.colorPointers[mecType.colorPointers.length - 1] & 0xff], class4551, clipLeft, clipTop, mecType.anInt2748, mecType.anInt2749, mecType.anInt2756);
	            } else {
	                for (rotation = 0; rotation < ints_10.length / 2 - 1; rotation++) {
	                    toolkit.method8669(ints_10[rotation * 2], ints_10[rotation * 2 + 1], ints_10[(rotation + 1) * 2], ints_10[(rotation + 1) * 2 + 1], mecType.colors[mecType.colorPointers[rotation] & 0xff], class4551, clipLeft, clipTop);
	                }
	                toolkit.method8669(ints_10[ints_10.length - 2], ints_10[ints_10.length - 1], ints_10[0], ints_10[1], mecType.colors[mecType.colorPointers[mecType.colorPointers.length - 1] & 0xff], class4551, clipLeft, clipTop);
	            }
	        }
	        NativeSprite iconSprite = null;
	        if (mecType.defaultIconArchive != -1) {
	            iconSprite = mecType.getIcon(toolkit, false);
	            if (iconSprite != null) {
	                drawEntity(component, class4551, clipLeft, clipTop, i_5, i_6, iconSprite);
	            }
	        }
	        if (mecType.areaName != null) {
	            i_11 = 0;
	            if (iconSprite != null) {
	                i_11 = iconSprite.getHeight();
	            }
	            AbstractFontRenderer fontrenderer20 = Renderers.FONT_RENDERER;
	            FontMetrics fontmetrics21 = FontManager.worldMapAreaFont;
	            if (mecType.baseTextzoom == 1) {
	                fontrenderer20 = FontManager.debugOverlayFont;
	                fontmetrics21 = FontManager.messageboxFont;
	            }
	            if (mecType.baseTextzoom == 2) {
	                fontrenderer20 = MiniMenu.minimenuHeader;
	                fontmetrics21 = FontMetrics.aClass4143438;
	            }
	            InterfaceComponentRenderer.method4152(component, class4551, clipLeft, clipTop, i_5, i_6, i_11, mecType.areaName, fontrenderer20, fontmetrics21, mecType.defaultTextColor);
	        }
	    }
	}

	/**
	 * Renders the minimap
	 */
	public static boolean drawGame(AbstractToolkit abstractToolkit, int fromPlane) {
	    Utils.time();
	    if (!abstractToolkit.readyForRendering()) {
	        return false;
	    } 
	    int regionDimensionX = Resource.mapRegion.getSceneSizeX();
	    int regionDimensionY = Resource.mapRegion.getSceneSizeY();
	    SettingsBits settingsBits = Resource.mapRegion.getSettingsBits();
	    Scene scene = Resource.mapRegion.getScene();
	    int halfRegionDimension = regionDimensionX / 2;
	    boolean mapSpritesCached = true;
	    for (int x = 0; x < regionDimensionX; x++) {
	        for (int y = 0; y < regionDimensionY; y++) {
	            for (int z = fromPlane; z <= 3; z++) {
	                if (settingsBits.isVisible(fromPlane, z, x, y)) {
	                    int plane = z;
	                    if (settingsBits.areRoofsHidden(x, y)) {
	                        plane = z - 1;
	                    }
	                    if (plane >= 0) {
	                        mapSpritesCached &= MapRegion.isMapSpriteCached(plane, x, y);
	                    }
	                }
	            }
	        }
	    }
	    if (!mapSpritesCached) {
	        return false;
	    } else { 
	        int minimapDimension = regionDimensionX * 4 + 48 + 48;
	        int[] minimapPixels = new int[minimapDimension * minimapDimension];
	        for (int index = 0; index < minimapPixels.length; index++) {
	            minimapPixels[index] = MapRegion.BACKGROUND_COLOR;
	        }
	        DirectDisplay directDisplay = null;
	        byte centerOffsetX = 0; // + makes map move east
	        byte centerOffsetY = 0; // + makes map move south
	        if (MapRegion.requiresDisplayResize) {
	            minimapBackground = abstractToolkit.generateEmptySprite(minimapDimension, minimapDimension, false, true);
	            directDisplay = abstractToolkit.getImageProducer();
	            directDisplay.setFramebuffer(0, minimapBackground.getFramebuffer());
	            RenderBuffer minimapRenderBuffer = abstractToolkit.getRenderBuffer(minimapDimension, minimapDimension);
	            directDisplay.setRenderBuffer(minimapRenderBuffer);
	            abstractToolkit.setDisplay(directDisplay);
	            halfRegionDimension = regionDimensionX;
	            centerOffsetX = 48;
	            centerOffsetY = 48;
	            abstractToolkit.applyClearFlags(1, 0);
	        } else {
	            minimapBackground = abstractToolkit.createNativeSprite(minimapPixels, minimapDimension, minimapDimension, minimapDimension);
	        }
	        Resource.mapRegion.getEnvironmentManager().prepareLights();
	        int backgroundMaxRandomise = MapRegion.BACKGROUND_COLOR | (238 + (int) (Math.random() * 20.0D) - 10 << 16) + (238 + (int) (Math.random() * 20.0D) - 10 << 8) + (238 + (int) (Math.random() * 20.0D) - 10);
	        int backgroundMinRandomise = MapRegion.BACKGROUND_COLOR | 238 + (int) (Math.random() * 20.0D) - 10 << 16;
	        int pixelClearColor = (int) (Math.random() * 8.0D) << 16 | (int) (Math.random() * 8.0D) << 8 | (int) (Math.random() * 8.0D);
	        boolean[][] visibleTiles = new boolean[halfRegionDimension + 1 + 2][halfRegionDimension + 1 + 2];
	        for (int width = 0; width < regionDimensionX; width += halfRegionDimension) {
	            for (int length = 0; length < regionDimensionY; length += halfRegionDimension) {
	                int drawPositionX = centerOffsetX;
	                int drawPositionY = centerOffsetY;
	                int positionXOffset = width; // + makes map move west
	                if (width > 0) {
	                    positionXOffset = width - 1;
	                    drawPositionX = centerOffsetX + 4;
	                }
	                int positionYOffset = length; // + makes map move north
	                if (length > 0) {
	                    positionYOffset = length - 1;
	                }
	                int i_26 = halfRegionDimension + width;
	                if (i_26 < regionDimensionX) {
	                    i_26++;
	                }
	                int altIndex = halfRegionDimension + length;
	                if (altIndex < regionDimensionY) {
	                    altIndex++;
	                    drawPositionY = centerOffsetY + 4;
	                }
	                if (MapRegion.requiresClippingReset) {
	                    abstractToolkit.resetClipping();
	                } else {
	                    abstractToolkit.setClipping(0, 0, drawPositionX + halfRegionDimension * 4, drawPositionY + halfRegionDimension * 4);
	                }
	                abstractToolkit.applyClearFlags(3, MapRegion.BACKGROUND_COLOR);
	                int minHalfRegionDimension = Math.min(halfRegionDimension, regionDimensionX - 1);
	                for (int z = fromPlane; z <= 3; z++) {
	                    for (int x = 0; x <= minHalfRegionDimension; x++) {
	                        for (int y = 0; y <= minHalfRegionDimension; y++) {
	                            visibleTiles[x][y] = settingsBits.isVisible(fromPlane, z, x + positionXOffset, y + positionYOffset);
	                        }
	                    }
	                    // FIXME: better names for the parameters
	                    scene.surfaceHeightmap[z].renderMinimapFloor(centerOffsetX, centerOffsetY, positionXOffset, positionYOffset, i_26, altIndex, visibleTiles);
	                    if (!MapRegion.visualizeClippedTilesOnMinimap) {
	                        for (int offsetX = -4; offsetX < halfRegionDimension; offsetX++) {
	                            for (int offsetY = -4; offsetY < halfRegionDimension; offsetY++) {
	                            	int sceneX = width + offsetX;
	                            	int sceneY = length + offsetY;
	                                if (sceneX >= 0 && sceneY >= 0 && settingsBits.isVisible(fromPlane, z, sceneX, sceneY)) {
	                                	int locPlane = z;
	                                    if (settingsBits.areRoofsHidden(sceneX, sceneY)) {
	                                        locPlane = z - 1;
	                                    }
	                                    if (locPlane >= 0) {
	                                        drawLocOnMinimap(abstractToolkit, locPlane, sceneX, sceneY, drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, backgroundMaxRandomise, backgroundMinRandomise);
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	                if (MapRegion.visualizeClippedTilesOnMinimap) {
	                    CollisionMap collision = Resource.mapRegion.getCollisionMap(fromPlane);
	                    for (int offsetX = 0; offsetX < halfRegionDimension; offsetX++) {
	                        for (int offsetY = 0; offsetY < halfRegionDimension; offsetY++) {
	                        	int tileX = width + offsetX;
	                        	int tileY = length + offsetY;
	                        	int activeFlag = collision.collisionFlags[tileX - collision.offsetX][tileY - collision.offsetY];
	                            if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.ALT_OBJ)) {
	                                abstractToolkit.clearPixels(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, 4, -1713569622);
	                            } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.N_ALT_OBJ)) {
	                                abstractToolkit.drawOpaqueHorizontalLine(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, -1713569622);
	                            } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.E_ALT_OBJ)) {
	                                abstractToolkit.drawOpaqueVerticalLine(drawPositionX + offsetX * 4 + 3, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, -1713569622);
	                            } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.S_ALT_OBJ)) {
	                                abstractToolkit.drawOpaqueHorizontalLine(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4 + 3, 4, -1713569622);
	                            } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.W_ALT_OBJ)) {
	                                abstractToolkit.drawOpaqueVerticalLine(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, -1713569622);
	                            }
	                        }
	                    }
	                }
	                abstractToolkit.drawFilledRectangle(drawPositionX, drawPositionY, halfRegionDimension * 4, halfRegionDimension * 4, pixelClearColor, 2);
	                if (!MapRegion.requiresDisplayResize) {
	                    minimapBackground.method2750((width) * 4 + 48, regionDimensionY * 4 + 48 - (length) * 4 - halfRegionDimension * 4, halfRegionDimension * 4, halfRegionDimension * 4, drawPositionX, drawPositionY);
	                    if (MapRegion.requiresClippingReset) {
	                        minimapBackground.draw(256, 0);
	                        try {
	                            abstractToolkit.renderFrame();
	                            TimeTools.sleep(2000L);
	                        } catch (Exception exception) {
	                        	exception.printStackTrace();
	                        }
	                    }
	                }
	            }
	        }
	        if (MapRegion.requiresDisplayResize) {
	            abstractToolkit.resizeDisplay(directDisplay);
	            if (MapRegion.requiresClippingReset) {
	                minimapBackground.draw(256, 0);
	                try {
	                    abstractToolkit.renderFrame();
	                    TimeTools.sleep(2000L);
	                } catch (Exception exception) {
	                	exception.printStackTrace();
	                }
	            }
	        }
	        abstractToolkit.resetClipping();
	        abstractToolkit.applyClearFlags(1, 1);
	        GameShell.setComponentsOnTop();
	        ObjectTypeList locTypeList = Resource.mapRegion.getLoctypeList();
	        minimapLocCount = 0;
	        MapRegion.staticElementsAwaitingDrawing.remove_all();
	        if (!MapRegion.visualizeClippedTilesOnMinimap) {
	            for (int sceneX = 0; sceneX < regionDimensionX; sceneX++) {
	                for (int sceneY = 0; sceneY < regionDimensionY; sceneY++) {
	                    for (int sceneZ = fromPlane; sceneZ <= fromPlane + 1 && sceneZ <= 3; sceneZ++) {
	                        if (settingsBits.isVisible(fromPlane, sceneZ, sceneX, sceneY)) {
	                            GameObject gameObject = (GameObject) scene.getGroundDecoration(sceneZ, sceneX, sceneY);
	                            if (gameObject == null) {
	                                gameObject = (GameObject) scene.getInteractiveObject(sceneZ, sceneX, sceneY, client.INTERACTIVE_OBJECT_COMPARABLE);
	                            }
	                            if (gameObject == null) {
	                                gameObject = (GameObject) scene.getWall(sceneZ, sceneX, sceneY);
	                            }
	                            if (gameObject == null) {
	                                gameObject = (GameObject) scene.getPrimaryWallDecoration(sceneZ, sceneX, sceneY);
	                            }
	                            if (gameObject != null) {
	                                ObjectType locType = locTypeList.list(gameObject.getId());
	                                if (!locType.members || client.memberWorld) {
	                                	int mapCategoryId = locType.mapCategoryid;
	                                    if (locType.alternativeIds != null) {
	                                        for (int altIndex = 0; altIndex < locType.alternativeIds.length; altIndex++) {
	                                            if (locType.alternativeIds[altIndex] != -1) {
	                                                ObjectType morphedLoc = locTypeList.list(locType.alternativeIds[altIndex]);
	                                                if (morphedLoc.mapCategoryid >= 0) {
	                                                    mapCategoryId = morphedLoc.mapCategoryid;
	                                                }
	                                            }
	                                        }
	                                    }
	                                    if (mapCategoryId >= 0) {
	                                        boolean mectypeHasRandomOffset = false;
	                                        MECType mectype = TypeLists.MEC.list(mapCategoryId);
	                                        if (mectype != null && mectype.hasRandomisedMinimapPosition) {
	                                            mectypeHasRandomOffset = true;
	                                        }
	                                        int mectypeSceneX = sceneX;
	                                        int mectypeSceneY = sceneY;
	                                        if (mectypeHasRandomOffset) {
	                                            int[][] collision = Resource.mapRegion.getCollisionMap(sceneZ).collisionFlags;
	                                            int offsetX = Resource.mapRegion.getCollisionMap(sceneZ).offsetX;
	                                            int offsetY = Resource.mapRegion.getCollisionMap(sceneZ).offsetY;
	                                            for (int i_33 = 0; i_33 < 10; i_33++) {
	                                            	int randomPositioning = (int) (Math.random() * 4.0D);
	                                                if (randomPositioning == 0 && mectypeSceneX > 0 && mectypeSceneX > sceneX - 3 && (collision[mectypeSceneX - 1 - offsetX][mectypeSceneY - offsetY] & 0x2c0108) == 0) {
	                                                    mectypeSceneX--;
	                                                }
	                                                if (randomPositioning == 1 && mectypeSceneX < regionDimensionX - 1 && mectypeSceneX < sceneX + 3 && (collision[mectypeSceneX + 1 - offsetX][mectypeSceneY - offsetY] & 0x2c0180) == 0) {
	                                                    mectypeSceneX++;
	                                                }
	                                                if (randomPositioning == 2 && mectypeSceneY > 0 && mectypeSceneY > sceneY - 3 && (collision[mectypeSceneX - offsetX][mectypeSceneY - 1 - offsetY] & 0x2c0102) == 0) {
	                                                    mectypeSceneY--;
	                                                }
	                                                if (randomPositioning == 3 && mectypeSceneY < regionDimensionY - 1 && mectypeSceneY < sceneY + 3 && (collision[mectypeSceneX - offsetX][mectypeSceneY + 1 - offsetY] & 0x2c0120) == 0) {
	                                                    mectypeSceneY++;
	                                                }
	                                            }
	                                        }
	                                        minimapLocId[minimapLocCount] = locType.id;
	                                        minimapLocSceneX[minimapLocCount] = mectypeSceneX;
	                                        minimapLocSceneY[minimapLocCount] = mectypeSceneY;
	                                        minimapLocCount++;
	                                    }
	                                }
	                            }
	                        }
	                    }
	                }
	            }
	            StaticElements staticElements = Resource.mapRegion.getStaticElements();
	            if (staticElements != null) {
	                TypeLists.MEC.createCache(1024, 64);
	                CoordGrid baseCoordinates = Resource.mapRegion.getBaseCoordinates();
	                for (int index = 0; index < staticElements.elementCount; index++) {
	                	int bitpacked = staticElements.bitpackedPlacement[index];
	                    if (bitpacked >> 28 == PlayerUpdate.MY_PLAYER.plane) {
	                    	int sceneX = (bitpacked >> 14 & 0x3fff) - baseCoordinates.x;
	                    	int sceneY = (bitpacked & 0x3fff) - baseCoordinates.y;
	                        if (sceneX >= 0 && sceneX < regionDimensionX && sceneY >= 0 && sceneY < regionDimensionY) {
	                            MapRegion.staticElementsAwaitingDrawing.addFirst(new IntNode(index));
	                        } else {
	                            MECType mectype = TypeLists.MEC.list(staticElements.mapCategoryIds[index]);
	                            if (mectype.offsets != null && sceneX + mectype.maxOffsetX >= 0
	                            		&& sceneX + mectype.minOffsetX < regionDimensionX
	                            		&& sceneY + mectype.maxOffsetY >= 0
	                            		&& sceneY + mectype.minOffsetY < regionDimensionY) {
	                                MapRegion.staticElementsAwaitingDrawing.addFirst(new IntNode(index));
	                            }
	                        }
	                    }
	                }
	                TypeLists.MEC.createCache(128, 64);
	            }
	        }
	        return true;
	    }
	}

	public static void drawGame(AbstractToolkit toolkit) {
	    if (MAP_PLANE != PlayerUpdate.MY_PLAYER.plane && Resource.mapRegion.getScene() != null) {
	        Utils.time();
	        if (drawGame(toolkit, PlayerUpdate.MY_PLAYER.plane)) { //<--
	            MAP_PLANE = PlayerUpdate.MY_PLAYER.plane;
	        }
	    }
	}

	public static void drawGame() {
	    drawGame(Renderers.activeToolkit); //<--
	    if (client.ROOF_OVERRIDE != PlayerUpdate.MY_PLAYER_PLANE) {
	        Scene.setRoofOverride();
	    }
	
	}
}
