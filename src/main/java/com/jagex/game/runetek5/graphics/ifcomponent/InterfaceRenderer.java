package com.jagex.game.runetek5.graphics.ifcomponent;

import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.Game;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.console.CommandExecutor;
import com.jagex.game.runetek5.client.input.console.Console;
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder;
import com.jagex.game.runetek5.client.input.mouse.CursorManager;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.client.menu.MiniMenu;
import com.jagex.game.runetek5.client.menu.MiniMenuBuilder;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentContentType;
import com.jagex.game.runetek5.config.rsinterface.ComponentType;
import com.jagex.game.runetek5.config.rsinterface.HookRequest;
import com.jagex.game.runetek5.config.rsinterface.IFTargetParams;
import com.jagex.game.runetek5.config.rsinterface.Interface;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.entity.pathingentity.EntityOverhead;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1;
import com.jagex.game.runetek5.math.Vector3;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.scene.environment.Environment;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.trigger.ClientTriggerType;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.utils.TextUtils;

public class InterfaceRenderer {

	/**
	 * Draws an interface and it's component,
	 * also processes interface interaction
	 */
	public static void processInterfaceCycle(Interface rsinterface, Component[] components, int parent, int startX, int startY, int endX, int endY, int offsetX, int offsetY, int mouseX, int mouseY) {
	    for (int index = 0; index < components.length; index++) {
	        Component component = components[index];
	        if (component != null && parent == component.parent) {
	            int x = offsetX + component.x;
	            int y = offsetY + component.y;
	            int boundaryMinX;
	            int boundaryMinY;
	            int boundaryMaxX;
	            int boundaryMaxY;
	            if (component.type == ComponentType.TYPE_2) {
	                boundaryMinX = startX;
	                boundaryMinY = startY;
	                boundaryMaxX = endX;
	                boundaryMaxY = endY;
	            } else {
	                int componentWidth = x + component.width;
	                int componentHeight = y + component.height;
	                if (component.type == ComponentType.LINE) {
	                    componentWidth++;
	                    componentHeight++;
	                }
	                boundaryMinX = Math.max(x, startX);
	                boundaryMinY = Math.max(y, startY);
	                boundaryMaxX = Math.min(componentWidth, endX);
	                boundaryMaxY = Math.min(componentHeight, endY);
	            }
	
	            if (component.type != ComponentType.CONTAINER && !component.hasScript && client.listComponentSettings(component).settingsHash == 0 && component != client.aClass1187247 && component.contentType != ComponentContentType.MINIMAP && component.contentType != ComponentContentType.CONTENT_TYPE_1406 && component.contentType != ComponentContentType.GAME_WINDOW && component.contentType != ComponentContentType.CONTENT_TYPE_1403) {
	                if (boundaryMinX < boundaryMaxX && boundaryMinY < boundaryMaxY) {
	                    Component.drawComponentObjSprite(component);
	                }
	            } else if (!InterfaceRenderer.isComponentHidden(component)) {
	                if (component == client.aClass1187257 && client.method14643(client.aClass1187257)) {
	                    client.aBool7369 = true;
	                    client.anInt7252 = x;
	                    client.anInt7215 = y;
	                }
	
	                if (component.hasInteraction || boundaryMinX < boundaryMaxX && boundaryMinY < boundaryMaxY) {
	                    if (component.noClickThrough && mouseX >= boundaryMinX && mouseY >= boundaryMinY && mouseX < boundaryMaxX && mouseY < boundaryMaxY) {
	                        for (HookRequest hook = (HookRequest) client.PENDING_HOOK_REQUESTS.getFirst(); hook != null; hook = (HookRequest) client.PENDING_HOOK_REQUESTS.next()) {
	                            if (hook.hasMousePosition) {
	                                hook.unlink();
	                                hook.source.isHooked = false;
	                            }
	                        }
	
	                        if (InterfaceRenderer.anInt8308 == 0) {
	                            client.aClass1187257 = null;
	                            client.aClass1187247 = null;
	                        }
	
	                        client.anInt7184 = 0;
	                        WorldMapSub1.isInteractive = false;
	                        CommandExecutor.teleportCommandRequested = false;
	                        if (!MiniMenu.isOpen) {
	                            MiniMenu.resetEntries();
	                        }
	                    }
	
	                    boolean addSprite = component.clickMask && component.type == ComponentType.SPRITE && component.transparency == 0 && component.skyboxId < 0 && component.containerItemId == -1 && component.anInt1435 == -1 && !component.tiling && component.angle2d == 0;
	                    boolean withinBoundaries = false;
	                    if (mouseX >= boundaryMinX && mouseY >= boundaryMinY && mouseX < boundaryMaxX && mouseY < boundaryMaxY) {
	                        if (addSprite) {
	                            InteractiveComponentSprite clickableSprite = component.addInteractiveSprite(Renderers.activeToolkit);
	                            if (clickableSprite != null && clickableSprite.width == component.width && component.height == clickableSprite.height) {
	                                int mouseOffsetX = mouseX - x;
	                                int mouseOffsetY = mouseY - y;
	                                if (mouseOffsetY >= 0 && mouseOffsetY < clickableSprite.rowStartX.length) {
	                                	int sizeX = clickableSprite.rowStartX[mouseOffsetY];
	                                    if (mouseOffsetX >= sizeX && mouseOffsetX <= sizeX + clickableSprite.rowWidth[mouseOffsetY]) {
	                                        withinBoundaries = true;
	                                    }
	                                }
	                            } else {
	                                withinBoundaries = true;
	                            }
	                        } else {
	                            withinBoundaries = true;
	                        }
	                    }
	
	                    if (!client.interfaceSelectionActive && withinBoundaries) {
	                        if (component.moveOverCursor >= 0) {
	                            client.defaultMenuCursor = component.moveOverCursor;
	                        } else if (component.noClickThrough) {
	                            client.defaultMenuCursor = -1;
	                        }
	                    }
	
	                    if (!MiniMenu.isOpen && withinBoundaries && !rsinterface.aBool999) {
	                        MiniMenuBuilder.interfaceComponentHovered(component);
	                    }
	
	                    boolean leftClick = MouseRecord.recorder.leftButtonDown() && withinBoundaries;

                        boolean bool49 = false;
	                    MouseRecord record = (MouseRecord) client.MOUSE_MOVEMENTS.getFirst();
	                    InteractiveComponentSprite class11940;
	                    if (record != null && record.getClickType() == 0 && record.getX() >= boundaryMinX && record.getY() >= boundaryMinY && record.getX() < boundaryMaxX && record.getY() < boundaryMaxY) {
	                        if (addSprite) {
	                            class11940 = component.addInteractiveSprite(Renderers.activeToolkit);
	                            if (class11940 != null && component.width == class11940.width && component.height == class11940.height) {
	                            	int i_25 = record.getX() - x;
	                            	int i_26 = record.getY() - y;
	                                if (i_26 >= 0 && i_26 < class11940.rowStartX.length) {
	                                	int i_27 = class11940.rowStartX[i_26];
	                                    if (i_25 >= i_27 && i_25 <= i_27 + class11940.rowWidth[i_26]) {
	                                        bool49 = true;
	                                    }
	                                }
	                            } else {
	                                bool49 = true;
	                            }
	                        } else {
	                            bool49 = true;
	                        }
	                    }
	
	                    if (component.hasInteraction && !Console.isOpen()) {
	                        for (int keyIndex = 0; keyIndex < component.keyTriggeringAction.length; keyIndex++) {
	                            boolean bool50 = false;
	                            boolean hasAction = false;
	                            if (component.anIntArray1267[keyIndex] > 0) {
	                                for (int i_27 = 0; i_27 < client.keyCachePointerPoss; i_27++) {
	                                    if (component.anIntArray1267[keyIndex] == client.KEYS_PRESSED[i_27].getCharacter()) {
	                                        bool50 = true;
	                                        if (component.cycles == null || component.cycles[keyIndex] <= client.cycles) {
	                                            hasAction = true;
	                                        }
	                                        break;
	                                    }
	                                }
	                            }
	
	                            if (!bool50 && component.keyTriggeringAction[keyIndex] != null) {
	                                for (int key = 0; key < component.keyTriggeringAction[keyIndex].length; key++) {
	                                    if (KeyRecorder.keyRecorder.held(component.keyTriggeringAction[keyIndex][key])) {
	                                        bool50 = true;
	                                        if (component.cycles != null && component.cycles[keyIndex] > client.cycles) {
	                                            break;
	                                        }
	
	                                        byte clickFunctionMask = component.triggeredKeyFunctionMasks[keyIndex][key];
	                                        if (clickFunctionMask == 0 || ((clickFunctionMask & 0x8) == 0 || !KeyRecorder.keyRecorder.held(86) && !KeyRecorder.keyRecorder.held(82) && !KeyRecorder.keyRecorder.held(81)) && ((clickFunctionMask & 0x2) == 0 || KeyRecorder.keyRecorder.held(86)) && ((clickFunctionMask & 0x1) == 0 || KeyRecorder.keyRecorder.held(82)) && ((clickFunctionMask & 0x4) == 0 || KeyRecorder.keyRecorder.held(81))) {
	                                            hasAction = true;
	                                            break;
	                                        }
	                                    }
	                                }
	                            }
	
	                            if (hasAction) {
	                                if (keyIndex < 10) {
	                                    Interface.ifOptionInteract(keyIndex + 1, component.idHash, component.slotId, "");
	                                } else if (keyIndex == 10) {
	                                    InterfaceComponentRenderer.method1170();
	                                    IFTargetParams iftParams = client.listComponentSettings(component);
	                                    InterfaceRenderer.setUseOptionFlags(component, iftParams.getUseOptionFlags(), iftParams.interfaceId);
	                                    client.useOnType = Interface.getUseOptionString(component);
	                                    if (client.useOnType == null) {
	                                        client.useOnType = "Null";
	                                    }
	
	                                    client.useOnTarget = component.useOnName + TextUtils.setTextColor(16777215);
	                                }
	
	                                int cycleProgress = component.cycleSteps[keyIndex];
	                                if (component.cycles == null) {
	                                    component.cycles = new int[component.keyTriggeringAction.length];
	                                }
	
	                                if (cycleProgress != 0) {
	                                    component.cycles[keyIndex] = cycleProgress + client.cycles;
	                                } else {
	                                    component.cycles[keyIndex] = Integer.MAX_VALUE;
	                                }
	                            }
	
	                            if (!bool50 && component.cycles != null) {
	                                component.cycles[keyIndex] = 0;
	                            }
	                        }
	                    }
	
	                    if (bool49) {
	                        InterfaceRenderer.method12221(component, record.getX() - x, record.getY() - y);
	                    }
	
	                    if (client.aClass1187257 != null && component != client.aClass1187257 && withinBoundaries && client.listComponentSettings(component).dragEnabled()) {
	                        client.aClass1187370 = component;
	                    }
	
	                    if (component == client.aClass1187247) {
	                        client.aBool7403 = true;
	                        client.anInt7432 = x;
	                        client.anInt7265 = y;
	                        client.anInt7367 = client.aClass1187247.width;
	                        client.anInt7476 = client.aClass1187247.height;
	                    }
	
	                    if (component.hasScript || component.contentType != 0) {
	                        if (withinBoundaries && client.anInt7191 != 0 && component.anObjectArray1412 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.hasMousePosition = true;
	                            hookRequest.source = component;
	                            hookRequest.mouseY = client.anInt7191;
	                            hookRequest.params = component.anObjectArray1412;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (client.aClass1187257 != null) {
	                            bool49 = false;
	                            leftClick = false;
	                        } else if (MiniMenu.isOpen || component.contentType != ComponentContentType.WORLDMAP && client.anInt7184 > 0) {
	                            bool49 = false;
	                            leftClick = false;
	                            withinBoundaries = false;
	                        }
	
	                        if (component.contentType != 0) {
	                            int i_52;
	                            if (component.contentType == ComponentContentType.GAME_WINDOW || component.contentType == ComponentContentType.CONTENT_TYPE_1403) {
	                                client.GAME_SCREEN_INTERFACE = component;
	                                Environment environment = Resource.mapRegion.getEnvironmentManager().getEnvironment();
	                                if (environment.getSkybox() != null && !Resource.mapRegionLoaderTask.isLoadingRegion()) {
	                                    environment.getSkybox().method4217(Renderers.activeToolkit, component.height, PreferenceManager.preferences.skyboxes.getValue());
	                                }
	
	                                if (component.contentType == ComponentContentType.GAME_WINDOW && !MiniMenu.isOpen && mouseX >= boundaryMinX && mouseY >= boundaryMinY && mouseX < boundaryMaxX && mouseY < boundaryMaxY) {
	                                    MiniMenuBuilder.tileHovered(Renderers.activeToolkit, mouseX, mouseY);
	
	                                    for (EntityOverhead overhead = (EntityOverhead) client.ENTITY_OVERHEADS.getFirst(); overhead != null; overhead = (EntityOverhead) client.ENTITY_OVERHEADS.getNext()) {
	                                        if (mouseX >= overhead.startX && mouseX < overhead.endX && mouseY >= overhead.startY && mouseY < overhead.endY) {
	                                            MiniMenu.resetEntries();
	                                            PathingEntity.interfaceComponentOnEntity(overhead.pathingEntity);
	                                        }
	                                    }
	                                }
	
	                                int i_25 = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
	                                int[] ints_46 = PlayerUpdate.OUT_PLAYER_INDICES;
	
	                                for (int i_27 = 0; i_27 < i_25; i_27++) {
	                                    PlayerEntity player55 = PlayerUpdate.players[ints_46[i_27]];
	                                    if (player55 != null) {
	                                        CS2Executor.executeIfTrigger(ClientTriggerType.IF_PLAYER_CYCLE, -1, player55, ints_46[i_27]);
	                                        player55.method15880(boundaryMinX, boundaryMinY, boundaryMaxX, boundaryMaxY, x - component.scrollX, y - component.scrollY, mouseX, mouseY);
	                                    }
	                                }
	
	                                for (int i_27 = 0; i_27 < client.NPC_UPDATE_INDEX; i_27++) {
	                                    i_52 = client.NPC_UPDATE_INDICES[i_27];
	                                    ObjectNode class282Sub4756 = client.NPC_MAP.get(i_52);
	                                    if (class282Sub4756 != null) {
	                                        CS2Executor.executeIfTrigger(ClientTriggerType.IF_NPC_CYCLE, ((NpcEntity) class282Sub4756.value).definitions.type, (PathingEntity) class282Sub4756.value, i_52);
	                                        ((PathingEntity) class282Sub4756.value).method15880(boundaryMinX, boundaryMinY, boundaryMaxX, boundaryMaxY, x - component.scrollX, y - component.scrollY, mouseX, mouseY);
	                                    }
	                                }
	                                continue;
	                            }
	
	                            if (component.contentType == ComponentContentType.MINIMAP) {
	                                class11940 = component.addInteractiveSprite(Renderers.activeToolkit);
	                                if (class11940 == null || ComponentMinimap.minimapState != 0 && ComponentMinimap.minimapState != 3 || MiniMenu.isOpen || mouseX < boundaryMinX || mouseY < boundaryMinY || mouseX >= boundaryMaxX || mouseY >= boundaryMaxY) {
	                                    continue;
	                                }
	
	                                int i_25 = mouseX - x;
	                                int i_26 = mouseY - y;
	                                int i_27 = class11940.rowStartX[i_26];
	                                if (i_25 < i_27 || i_25 > i_27 + class11940.rowWidth[i_26]) {
	                                    continue;
	                                }
	
	                                i_25 -= component.width / 2;
	                                i_26 -= component.height / 2;
	                                if (Camera.cameraCalculationType == 4) {
	                                    i_52 = (int) Camera.camAngleY & 0x3fff;
	                                } else {
	                                    i_52 = (int) Camera.camAngleY + client.minimapRotation & 0x3fff;
	                                }
	
	                                int i_29 = MathUtils.SINE[i_52];
	                                int i_30 = MathUtils.COSINE[i_52];
	                                if (Camera.cameraCalculationType != 4) {
	                                    i_29 = (client.minimapZoom + 256) * i_29 >> 8;
	                                    i_30 = i_30 * (client.minimapZoom + 256) >> 8;
	                                }
	
	                                int i_31 = i_29 * i_26 + i_25 * i_30 >> 14;
	                                int i_32 = i_26 * i_30 - i_29 * i_25 >> 14;
	                                int i_33;
	                                int i_34;
	                                if (Camera.cameraCalculationType == 4) {
	                                    i_33 = (i_31 >> 2) + (client.anInt7262 >> 9);
	                                    i_34 = (client.anInt7376 >> 9) - (i_32 >> 2);
	                                } else {
	                                    int i_45 = (PlayerUpdate.MY_PLAYER.getSize() - 1) * 256;
	                                    Vector3 vector336 = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
	                                    i_33 = ((int) vector336.x - i_45 >> 9) + (i_31 >> 2);
	                                    i_34 = ((int) vector336.z - i_45 >> 9) - (i_32 >> 2);
	                                }
	
	                                if (client.interfaceSelectionActive && (MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x40) != 0) {
	                                    Component icomponentdefinitions35 = Component.listIcomponent(client.anInt56, client.anInt7345);
	                                    if (icomponentdefinitions35 != null) {
	                                        MiniMenu.addEntry(client.useOnType, " ->", MiniMenu.useOnCursor, 59, component.containerItemId, 1L, i_33, i_34, true, false, component.slotId | component.idHash, true);
	                                    } else {
	                                        InterfaceComponentRenderer.method1170();
	                                    }
	                                    continue;
	                                }
	
	                                if (Game.STELLAR_DAWN == client.CURRENT_GAME) {
	                                    MiniMenu.addEntry(LocalizedText.FACE_HERE.translate(client.CURRENT_LANGUAGE), "", -1, 60, -1, 1L, i_33, i_34, true, false, 0L, true);
	                                }
	
	                                MiniMenu.addEntry(InterfaceRenderer.translatedUptext, "", CursorManager.activeCursorId, 23, -1, 1L, i_33, i_34, true, false, 0L, true);
	                                continue;
	                            }
	
	                            if (component.contentType == ComponentContentType.WORLDMAP) {
	                                WorldMap.worldmapComponent = component;
	                                if (withinBoundaries) {
	                                    WorldMapSub1.isInteractive = true;
	                                }
	
	                                if (bool49) {
	                                	int keyIndex = (int) ((record.getX() - x - component.width / 2) * 2.0D / WorldMap.mapZoom);
	                                    int i_25 = (int) (-((record.getY() - y - component.height / 2) * 2.0D / WorldMap.mapZoom));
	                                    int i_26 = keyIndex + WorldMap.mapSquareAbsX + WorldMapSub1.anInt5123;
	                                    int i_27 = i_25 + WorldMap.mapSquareAbsY + WorldMapSub1.anInt5624;
	                                    WorldMapElement worldMap = WorldMapSub1.getStaticElement();
	                                    if (worldMap == null) {
	                                        continue;
	                                    }
	
	                                    int[] absolutePosition = new int[3];
	                                    worldMap.method14775(i_26, i_27, absolutePosition);
	                                    if (absolutePosition != null) {
	                                        if (KeyRecorder.keyRecorder.held(82) && client.PLAYER_RIGHTS > 0) {
	                                            CommandExecutor.executeTeleportCommand(absolutePosition[0], absolutePosition[1], absolutePosition[2]);
	                                            continue;
	                                        }
	
	                                        CommandExecutor.teleportCommandRequested = true;
	                                        CommandExecutor.teleportCommandPositionZ = absolutePosition[0];
	                                        CommandExecutor.teleportCommandPositionX = absolutePosition[1];
	                                        CommandExecutor.teleportCommandPositionY = absolutePosition[2];
	                                    }
	
	                                    client.anInt7184 = 1;
	                                    client.aBool7374 = false;
	                                    client.anInt7361 = MouseRecord.recorder.getMouseX();
	                                    client.anInt7362 = MouseRecord.recorder.getMouseY();
	                                    continue;
	                                }
	
	                                if (leftClick && client.anInt7184 > 0) {
	                                    if (client.anInt7184 == 1 && (client.anInt7361 != MouseRecord.recorder.getMouseX() || client.anInt7362 != MouseRecord.recorder.getMouseY())) {
	                                        Interface.anInt3008 = WorldMapSub1.anInt5123;
	                                        InterfaceRenderer.anInt123 = WorldMapSub1.anInt5624;
	                                        client.anInt7184 = 2;
	                                    }
	
	                                    if (client.anInt7184 == 2) {
	                                        client.aBool7374 = true;
	                                        WorldMapSub1.method1503(Interface.anInt3008 + (int) ((client.anInt7361 - MouseRecord.recorder.getMouseX()) * 2.0D / WorldMap.requestedMapZoom));
	                                        WorldMapSub1.method5599(InterfaceRenderer.anInt123 - (int) ((client.anInt7362 - MouseRecord.recorder.getMouseY()) * 2.0D / WorldMap.requestedMapZoom));
	                                    }
	                                    continue;
	                                }
	
	                                if (client.anInt7184 > 0 && !client.aBool7374) {
	                                    if ((client.anInt7339 == 1 || MiniMenu.method12562()) && MiniMenu.entryCount > 2) {
	                                        StaticMethods.method4164(client.anInt7361, client.anInt7362);
	                                    } else if (MiniMenu.hasEntries()) {
	                                        StaticMethods.method4164(client.anInt7361, client.anInt7362);
	                                    }
	                                }
	
	                                client.anInt7184 = 0;
	                                continue;
	                            }
	
	                            if (component.contentType == ComponentContentType.WORLD_MAP_OVERVIEW) {
	                                if (leftClick) {
	                                    WorldMapSub1.markDestination(MouseRecord.recorder.getMouseX() - x, MouseRecord.recorder.getMouseY() - y, component.width, component.height);
	                                }
	                                continue;
	                            }
	
	                            if (component.contentType == ComponentContentType.CONTENT_TYPE_1406) {
	                                InterfaceComponentRenderer.method544(component, x, y);
	                                continue;
	                            }
	                        }
	
	                        if (!component.aBool1286 && bool49) {
	                            component.aBool1286 = true;
	                            if (component.anObjectArray1386 != null) {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.hasMousePosition = true;
	                                hookRequest.source = component;
	                                hookRequest.mouseX = record.getX() - x;
	                                hookRequest.mouseY = record.getY() - y;
	                                hookRequest.params = component.anObjectArray1386;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	                        }
	
	                        if (component.aBool1286 && leftClick && component.anObjectArray1319 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.hasMousePosition = true;
	                            hookRequest.source = component;
	                            hookRequest.mouseX = MouseRecord.recorder.getMouseX() - x;
	                            hookRequest.mouseY = MouseRecord.recorder.getMouseY() - y;
	                            hookRequest.params = component.anObjectArray1319;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (component.aBool1286 && !leftClick) {
	                            component.aBool1286 = false;
	                            if (component.anObjectArray1302 != null) {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.hasMousePosition = true;
	                                hookRequest.source = component;
	                                hookRequest.mouseX = MouseRecord.recorder.getMouseX() - x;
	                                hookRequest.mouseY = MouseRecord.recorder.getMouseY() - y;
	                                hookRequest.params = component.anObjectArray1302;
	                                client.aClass4827404.addFirst(hookRequest);
	                            }
	                        }
	
	                        if (leftClick && component.anObjectArray1389 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.hasMousePosition = true;
	                            hookRequest.source = component;
	                            hookRequest.mouseX = MouseRecord.recorder.getMouseX() - x;
	                            hookRequest.mouseY = MouseRecord.recorder.getMouseY() - y;
	                            hookRequest.params = component.anObjectArray1389;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (!component.isHooked && withinBoundaries) {
	                            component.isHooked = true;
	                            if (component.onMouseHoverScript != null) {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.hasMousePosition = true;
	                                hookRequest.source = component;
	                                hookRequest.mouseX = MouseRecord.recorder.getMouseX() - x;
	                                hookRequest.mouseY = MouseRecord.recorder.getMouseY() - y;
	                                hookRequest.params = component.onMouseHoverScript;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	                        }
	
	                        if (component.isHooked && withinBoundaries && component.popupScript != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.hasMousePosition = true;
	                            hookRequest.source = component;
	                            hookRequest.mouseX = MouseRecord.recorder.getMouseX() - x;
	                            hookRequest.mouseY = MouseRecord.recorder.getMouseY() - y;
	                            hookRequest.params = component.popupScript;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (component.isHooked && !withinBoundaries) {
	                            component.isHooked = false;
	                            if (component.onMouseLeaveScript != null) {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.hasMousePosition = true;
	                                hookRequest.source = component;
	                                hookRequest.mouseX = MouseRecord.recorder.getMouseX() - x;
	                                hookRequest.mouseY = MouseRecord.recorder.getMouseY() - y;
	                                hookRequest.params = component.onMouseLeaveScript;
	                                client.aClass4827404.addFirst(hookRequest);
	                            }
	                        }
	
	                        if (component.anObjectArray1409 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1409;
	                            client.aClass4827233.addFirst(hookRequest);
	                        }
	
	                        HookRequest hookrequest54;
	                        if (component.anObjectArray1403 != null && client.anInt7368 > component.anInt1446) {
	                            if (component.anIntArray1315 != null && client.anInt7368 - component.anInt1446 <= 32) {
	                                label969:
	                                for (int keyIndex = component.anInt1446; keyIndex < client.anInt7368; keyIndex++) {
	                                	int i_25 = client.anIntArray7150[keyIndex & 0x1f];
	
	                                    for (int i_26 = 0; i_26 < component.anIntArray1315.length; i_26++) {
	                                        if (i_25 == component.anIntArray1315[i_26]) {
	                                            hookrequest54 = new HookRequest();
	                                            hookrequest54.source = component;
	                                            hookrequest54.params = component.anObjectArray1403;
	                                            client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
	                                            break label969;
	                                        }
	                                    }
	                                }
	                            } else {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.source = component;
	                                hookRequest.params = component.anObjectArray1403;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	
	                            component.anInt1446 = client.anInt7368;
	                        }
	
	                        if (component.anObjectArray1405 != null && client.anInt7388 > component.anInt1447) {
	                            if (component.anIntArray1406 != null && client.anInt7388 - component.anInt1447 <= 32) {
	                                label945:
	                                for (int keyIndex = component.anInt1447; keyIndex < client.anInt7388; keyIndex++) {
	                                	int i_25 = client.anIntArray7387[keyIndex & 0x1f];
	
	                                    for (int i_26 = 0; i_26 < component.anIntArray1406.length; i_26++) {
	                                        if (i_25 == component.anIntArray1406[i_26]) {
	                                            hookrequest54 = new HookRequest();
	                                            hookrequest54.source = component;
	                                            hookrequest54.params = component.anObjectArray1405;
	                                            client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
	                                            break label945;
	                                        }
	                                    }
	                                }
	                            } else {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.source = component;
	                                hookRequest.params = component.anObjectArray1405;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	
	                            component.anInt1447 = client.anInt7388;
	                        }
	
	                        if (component.anObjectArray1397 != null && client.pulseEventConfigsPtr > component.anInt1443) {
	                            if (component.varps != null && client.pulseEventConfigsPtr - component.anInt1443 <= 32) {
	                                label921:
	                                for (int keyIndex = component.anInt1443; keyIndex < client.pulseEventConfigsPtr; keyIndex++) {
	                                	int i_25 = client.PULSE_EVENT_CONFIGS_CACHE[keyIndex & 0x1f];
	
	                                    for (int i_26 = 0; i_26 < component.varps.length; i_26++) {
	                                        if (i_25 == component.varps[i_26]) {
	                                            hookrequest54 = new HookRequest();
	                                            hookrequest54.source = component;
	                                            hookrequest54.params = component.anObjectArray1397;
	                                            client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
	                                            break label921;
	                                        }
	                                    }
	                                }
	                            } else {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.source = component;
	                                hookRequest.params = component.anObjectArray1397;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	
	                            component.anInt1443 = client.pulseEventConfigsPtr;
	                        }
	
	                        if (component.mouseLeaveScript != null && client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS > component.anInt1444) {
	                            if (component.mouseLeaveArrayParams != null && client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - component.anInt1444 <= 32) {
	                                label897:
	                                for (int keyIndex = component.anInt1444; keyIndex < client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS; keyIndex++) {
	                                	int i_25 = client.RECENTLY_USED_ITEM_CONTAINERS[keyIndex & 0x1f];
	
	                                    for (int i_26 = 0; i_26 < component.mouseLeaveArrayParams.length; i_26++) {
	                                        if (i_25 == component.mouseLeaveArrayParams[i_26]) {
	                                            hookrequest54 = new HookRequest();
	                                            hookrequest54.source = component;
	                                            hookrequest54.params = component.mouseLeaveScript;
	                                            client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
	                                            break label897;
	                                        }
	                                    }
	                                }
	                            } else {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.source = component;
	                                hookRequest.params = component.mouseLeaveScript;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	
	                            component.anInt1444 = client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS;
	                        }
	
	                        if (component.anObjectArray1387 != null && client.anInt7384 > component.anInt1445) {
	                            if (component.anIntArray1402 != null && client.anInt7384 - component.anInt1445 <= 32) {
	                                label873:
	                                for (int keyIndex = component.anInt1445; keyIndex < client.anInt7384; keyIndex++) {
	                                	int i_25 = client.anIntArray7283[keyIndex & 0x1f];
	
	                                    for (int i_26 = 0; i_26 < component.anIntArray1402.length; i_26++) {
	                                        if (i_25 == component.anIntArray1402[i_26]) {
	                                            hookrequest54 = new HookRequest();
	                                            hookrequest54.source = component;
	                                            hookrequest54.params = component.anObjectArray1387;
	                                            client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
	                                            break label873;
	                                        }
	                                    }
	                                }
	                            } else {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.source = component;
	                                hookRequest.params = component.anObjectArray1387;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	
	                            component.anInt1445 = client.anInt7384;
	                        }
	
	                        if (component.anObjectArray1407 != null && client.CLAN_VAR_COUNTER > component.anInt1448) {
	                            if (component.anIntArray1408 != null && client.CLAN_VAR_COUNTER - component.anInt1448 <= 32) {
	                                label849:
	                                for (int keyIndex = component.anInt1448; keyIndex < client.CLAN_VAR_COUNTER; keyIndex++) {
	                                	int i_25 = client.CLAN_VAR_KEYS[keyIndex & 0x1f];
	
	                                    for (int i_26 = 0; i_26 < component.anIntArray1408.length; i_26++) {
	                                        if (i_25 == component.anIntArray1408[i_26]) {
	                                            hookrequest54 = new HookRequest();
	                                            hookrequest54.source = component;
	                                            hookrequest54.params = component.anObjectArray1407;
	                                            client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
	                                            break label849;
	                                        }
	                                    }
	                                }
	                            } else {
	                                HookRequest hookRequest = new HookRequest();
	                                hookRequest.source = component;
	                                hookRequest.params = component.anObjectArray1407;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                            }
	
	                            component.anInt1448 = client.CLAN_VAR_COUNTER;
	                        }
	
	                        if (client.anInt7391 > component.anInt1442 && component.anObjectArray1413 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1413;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (client.anInt7386 > component.anInt1442 && component.anObjectArray1415 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1415;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (client.anInt7179 > component.anInt1442 && component.anObjectArray1416 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1416;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (client.anInt7192 > component.anInt1442 && component.anObjectArray1383 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1383;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (client.anInt7395 > component.anInt1442 && component.anObjectArray1418 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1418;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (client.anInt7320 > component.anInt1442 && component.anObjectArray1361 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1361;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        if (client.anInt7397 > component.anInt1442 && component.anObjectArray1419 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1419;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	
	                        component.anInt1442 = client.anInt7347;
	                        if (component.anObjectArray1292 != null) {
	                            for (int keyIndex = 0; keyIndex < client.keyCachePointerPoss; keyIndex++) {
	                                HookRequest hookrequest44 = new HookRequest();
	                                hookrequest44.source = component;
	                                hookrequest44.typedKeyCode = client.KEYS_PRESSED[keyIndex].getCode();
	                                hookrequest44.typedKeyChar = client.KEYS_PRESSED[keyIndex].getCharacter();
	                                hookrequest44.params = component.anObjectArray1292;
	                                client.PENDING_HOOK_REQUESTS.addFirst(hookrequest44);
	                            }
	                        }
	
	                        if (client.aBool7355 && component.anObjectArray1353 != null) {
	                            HookRequest hookRequest = new HookRequest();
	                            hookRequest.source = component;
	                            hookRequest.params = component.anObjectArray1353;
	                            client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
	                        }
	                    }
	
	                    if (component.type == ComponentType.SPRITE && component.skyboxId != -1) {
	                        component.calculateLighting(TypeLists.SKYBOX, TypeLists.SUN).method4217(Renderers.activeToolkit, component.height, PreferenceManager.preferences.skyboxes.getValue());
	                    }
	
	                    Component.drawComponentObjSprite(component);
	                    if (component.type == ComponentType.CONTAINER) {
	                        processInterfaceCycle(rsinterface, components, component.idHash, boundaryMinX, boundaryMinY, boundaryMaxX, boundaryMaxY, x - component.scrollX, y - component.scrollY, mouseX, mouseY);
	                        if (component.itemSlots != null) {
	                            processInterfaceCycle(rsinterface, component.itemSlots, component.idHash, boundaryMinX, boundaryMinY, boundaryMaxX, boundaryMaxY, x - component.scrollX, y - component.scrollY, mouseX, mouseY);
	                        }
	
	                        SubInterface subInterface = client.OPEN_INTERFACES.get(component.idHash);
	                        if (subInterface != null) {
	                            if (Game.RUNESCAPE == client.CURRENT_GAME && subInterface.overlay == 0 && !MiniMenu.isOpen && withinBoundaries && !client.DISPLAY_HIDDEN_MENU_OPTIONS) {
	                                MiniMenu.resetEntries();
	                            }
	
	                            Interface.open_sub(subInterface, subInterface.subInterfaceId, boundaryMinX, boundaryMinY, boundaryMaxX, boundaryMaxY, x, y, mouseX, mouseY);
	                        }
	                    }
	                }
	            }
	        }
	    }
	
	}

	public static void updateInterfaceDimensions(Component[] itemSlots, int idHash, int width, int height, boolean bool4) {
	    for (int i_6 = 0; i_6 < itemSlots.length; i_6++) {
	        Component component = itemSlots[i_6];
	        if (component != null && component.parent == idHash) {
	            InterfaceRenderer.initSizes(component, width, height, bool4);
	            InterfaceComponentRenderer.componentPositionAdjustment(component, width, height);
	            if (component.scrollX > component.scrollWidth - component.width) {
	                component.scrollX = component.scrollWidth - component.width;
	            }
	            if (component.scrollX < 0) {
	                component.scrollX = 0;
	            }
	            if (component.scrollY > component.scrollHeight - component.height) {
	                component.scrollY = component.scrollHeight - component.height;
	            }
	            if (component.scrollY < 0) {
	                component.scrollY = 0;
	            }
	            if (component.type == ComponentType.CONTAINER) {
	                InterfaceComponentRenderer.updateEntityOnInterface(itemSlots, component, bool4);
	            }
	        }
	    }
	}

	public static void updateInterfaceDimensions(int interfaceId, int width, int height, boolean bool3) {
	    if (Interface.interfaceLoaded(interfaceId, null)) {
	        updateInterfaceDimensions(Interface.INTERFACES[interfaceId].components, -1, width, height, bool3);
	    }	
	}

	public static String translatedUptext;

	public static void initSizes(Component interfaceComponent, int i_1, int i_2, boolean hookRequest) {
	    int width = interfaceComponent.width;
	    int height = interfaceComponent.height;
	    if (interfaceComponent.aspectWidthType == 0) {
	        interfaceComponent.width = interfaceComponent.baseWidth;
	    } else if (interfaceComponent.aspectWidthType == 1) {
	        interfaceComponent.width = i_1 - interfaceComponent.baseWidth;
	    } else if (interfaceComponent.aspectWidthType == 2) {
	        interfaceComponent.width = interfaceComponent.baseWidth * i_1 >> 14;
	    }
	    if (interfaceComponent.aspectHeightType == 0) {
	        interfaceComponent.height = interfaceComponent.baseHeight;
	    } else if (interfaceComponent.aspectHeightType == 1) {
	        interfaceComponent.height = i_2 - interfaceComponent.baseHeight;
	    } else if (interfaceComponent.aspectHeightType == 2) {
	        interfaceComponent.height = i_2 * interfaceComponent.baseHeight >> 14;
	    }
	    if (interfaceComponent.aspectWidthType == 4) {
	        interfaceComponent.width = interfaceComponent.aspectX * interfaceComponent.height / interfaceComponent.aspectY;
	    }
	    if (interfaceComponent.aspectHeightType == 4) {
	        interfaceComponent.height = interfaceComponent.width * interfaceComponent.aspectY / interfaceComponent.aspectX;
	    }
	    if (client.DISPLAY_HIDDEN_MENU_OPTIONS && (client.listComponentSettings(interfaceComponent).settingsHash != 0 || interfaceComponent.type == ComponentType.CONTAINER)) {
	        if (interfaceComponent.height < 5 && interfaceComponent.width < 5) {
	            interfaceComponent.height = 5;
	            interfaceComponent.width = 5;
	        } else {
	            if (interfaceComponent.height <= 0) {
	                interfaceComponent.height = 5;
	            }
	            if (interfaceComponent.width <= 0) {
	                interfaceComponent.width = 5;
	            }
	        }
	    }
	    if (interfaceComponent.contentType == ComponentContentType.GAME_WINDOW) {
	        client.GAME_SCREEN_INTERFACE = interfaceComponent;
	    }
	    if (hookRequest && interfaceComponent.anObjectArray1271 != null && (width != interfaceComponent.width || height != interfaceComponent.height)) {
	        HookRequest hookrequest7 = new HookRequest();
	        hookrequest7.source = interfaceComponent;
	        hookrequest7.params = interfaceComponent.anObjectArray1271;
	        client.PENDING_HOOK_REQUESTS.addFirst(hookrequest7);
	    }
	}

	public static void method12221(Component component, int offsetX, int offsetY) {
	    if (client.aClass1187257 == null && !MiniMenu.isOpen && component != null && client.method14643(component)) {
	        client.aClass1187257 = component;
	        client.aClass1187247 = Component.method12880(component);
	        client.anInt7361 = offsetX;
	        client.anInt7362 = offsetY;
	        InterfaceRenderer.anInt8308 = 0;
	        client.aBool7364 = false;
	    }
	
	}

	public static boolean isComponentHidden(Component component) {
	    if (client.DISPLAY_HIDDEN_MENU_OPTIONS) {
	        if (client.listComponentSettings(component).settingsHash != 0) {
	            return false;
	        }
	        if (component.type == ComponentType.CONTAINER) {
	            return false;
	        }
	    }
	    return component.hidden;
	}

	public static int anInt123;

	public static void setUseOptionFlags(Component componentDefs, int flags, int interfaceId) {
	    if (componentDefs != null) {
	        if (componentDefs.anObjectArray1400 != null) {
	            HookRequest hookrequest4 = new HookRequest();
	            hookrequest4.source = componentDefs;
	            hookrequest4.params = componentDefs.anObjectArray1400;
	            CS2Executor.executeHookInner(hookrequest4);
	        }
	        client.interfaceSelectionActive = true;
	        client.anInt56 = componentDefs.idHash;
	        client.anInt7345 = componentDefs.slotId;
	        MiniMenuBuilder.USE_OPTIONS_FLAGS = flags;
	        MiniMenu.USE_INTERFACE_ID = interfaceId;
	        client.anInt7346 = componentDefs.containerItemId;
	        MiniMenu.useOnCursor = componentDefs.targetOverCursor;
	        client.defaultMenuCursor = componentDefs.targetLeaveCursor;
	        Component.redrawComponent(componentDefs);
	    }
	}

	public static int anInt8308;

}
