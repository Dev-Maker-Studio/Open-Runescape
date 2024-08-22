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
import com.jagex.game.runetek5.config.rsinterface.IFEvents;
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

import java.util.stream.Stream;

public class InterfaceRenderer {

	/**
	 * Draws an interface and it's component,
	 * also processes interface interaction
	 */
	public static void processInterfaceCycle(Interface parentInterface, Component[] childComponents, int parentId, int clipStartX, int clipStartY, int clipEndX, int clipEndY, int parentOffsetX, int parentOffsetY, int mouseX, int mouseY) {
	    Stream.of(childComponents).forEach(childComponent -> {
			if (childComponent != null && parentId == childComponent.parentId) {
				int componentX = parentOffsetX + childComponent.x;
				int componentY = parentOffsetY + childComponent.y;
				int clipMinX, clipMinY, clipMaxX, clipMaxY;
				if (childComponent.type == ComponentType.TYPE_2) {
					clipMinX = clipStartX;
					clipMinY = clipStartY;
					clipMaxX = clipEndX;
					clipMaxY = clipEndY;
				} else {
					int componentRightX = componentX + childComponent.width;
					int componentBottomY = componentY + childComponent.height;
					if (childComponent.type == ComponentType.LINE) {
						componentRightX++;
						componentBottomY++;
					}
					clipMinX = Math.max(componentX, clipStartX);
					clipMinY = Math.max(componentY, clipStartY);
					clipMaxX = Math.min(componentRightX, clipEndX);
					clipMaxY = Math.min(componentBottomY, clipEndY);
				}

				boolean withinClipBounds = clipMinX < clipMaxX && clipMinY < clipMaxY;

				if (childComponent.type != ComponentType.CONTAINER && !childComponent.hasScript && client.getComponentSettings(childComponent).settingsHash == 0 && childComponent != client.selectedComponent && !childComponent.isIgnoredContentType()) {
					if (withinClipBounds) {
						Component.drawComponentObjSprite(childComponent);
					}
				} else if (!InterfaceRenderer.isComponentHidden(childComponent)) {
					if (childComponent == client.draggingComponent && client.hasDepthFlags(client.draggingComponent)) {
						client.isDraggingComponent = true;
						client.dragStartX = componentX;
						client.dragStartY = componentY;
					}

					if (childComponent.hasInteraction || withinClipBounds) {
						if (childComponent.preventClickThrough && mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
							resetPendingHooks();
							if (!MiniMenu.isOpen) {
								MiniMenu.resetEntries();
							}
						}

						boolean shouldAddSprite = childComponent.clickMask && childComponent.type == ComponentType.SPRITE && childComponent.transparency == 0 && childComponent.skyboxId < 0 && childComponent.containerItemId == -1 && childComponent.anInt1435 == -1 && !childComponent.tiling && childComponent.angle2d == 0;
						boolean withinBoundaries = false;
						if (mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
							if (shouldAddSprite) {
								InteractiveComponentSprite clickableSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit);
								if (clickableSprite != null && clickableSprite.width == childComponent.width && childComponent.height == clickableSprite.height) {
									int mouseOffsetX = mouseX - componentX;
									int mouseOffsetY = mouseY - componentY;
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
							if (childComponent.moveOverCursor >= 0) {
								client.defaultMenuCursor = childComponent.moveOverCursor;
							} else if (childComponent.preventClickThrough) {
								client.defaultMenuCursor = -1;
							}
						}

						if (!MiniMenu.isOpen && withinBoundaries && !parentInterface.preventInteraction) {
							MiniMenuBuilder.interfaceComponentHovered(childComponent);
						}

						boolean leftClick = MouseRecord.recorder.leftButtonDown() && withinBoundaries;
						boolean clickTriggered = false;
						MouseRecord mouseRecord = (MouseRecord) client.MOUSE_MOVEMENTS.getFirst();
						InteractiveComponentSprite interactiveSprite;

						if (mouseRecord != null && mouseRecord.getClickType() == 0 && mouseRecord.getX() >= clipMinX && mouseRecord.getY() >= clipMinY && mouseRecord.getX() < clipMaxX && mouseRecord.getY() < clipMaxY) {
							if (shouldAddSprite) {
								interactiveSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit);
								if (interactiveSprite != null && childComponent.width == interactiveSprite.width && childComponent.height == interactiveSprite.height) {
									int spriteRelativeX = mouseRecord.getX() - componentX;
									int spriteRelativeY = mouseRecord.getY() - componentY;
									if (spriteRelativeY >= 0 && spriteRelativeY < interactiveSprite.rowStartX.length) {
										int i_27 = interactiveSprite.rowStartX[spriteRelativeY];
										if (spriteRelativeX >= i_27 && spriteRelativeX <= i_27 + interactiveSprite.rowWidth[spriteRelativeY]) {
											clickTriggered = true;
										}
									}
								} else {
									clickTriggered = true;
								}
							} else {
								clickTriggered = true;
							}
						}

						if (childComponent.hasInteraction && !Console.isOpen()) {
							for (int keyIndex = 0; keyIndex < childComponent.keyTriggeringAction.length; keyIndex++) {
								boolean keyTriggered = false;
								boolean hasAction = false;
								if (childComponent.anIntArray1267[keyIndex] > 0) {
									for (int i = 0; i < client.keyCachePointerPoss; i++) {
										if (childComponent.anIntArray1267[keyIndex] == client.KEYS_PRESSED[i].getCharacter()) {
											keyTriggered = true;
											if (childComponent.cycles == null || childComponent.cycles[keyIndex] <= client.cycles) {
												hasAction = true;
											}
											break;
										}
									}
								}

								if (!keyTriggered && childComponent.keyTriggeringAction[keyIndex] != null) {
									for (int key = 0; key < childComponent.keyTriggeringAction[keyIndex].length; key++) {
										if (KeyRecorder.keyRecorder.held(childComponent.keyTriggeringAction[keyIndex][key])) {
											keyTriggered = true;
											if (childComponent.cycles != null && childComponent.cycles[keyIndex] > client.cycles) {
												break;
											}

											byte clickFunctionMask = childComponent.keyTriggerParams[keyIndex][key];
											if (clickFunctionMask == 0 || ((clickFunctionMask & 0x8) == 0 || !KeyRecorder.keyRecorder.held(86) && !KeyRecorder.keyRecorder.held(82) && !KeyRecorder.keyRecorder.held(81)) && ((clickFunctionMask & 0x2) == 0 || KeyRecorder.keyRecorder.held(86)) && ((clickFunctionMask & 0x1) == 0 || KeyRecorder.keyRecorder.held(82)) && ((clickFunctionMask & 0x4) == 0 || KeyRecorder.keyRecorder.held(81))) {
												hasAction = true;
												break;
											}
										}
									}
								}

								if (hasAction) {
									if (keyIndex < 10) {
										Interface.ifOptionInteract(keyIndex + 1, childComponent.idHash, childComponent.slotId, "");
									} else if (keyIndex == 10) {
										InterfaceComponentRenderer.method1170();
										IFEvents childEvents = client.getComponentSettings(childComponent);
										InterfaceRenderer.setUseOptionFlags(childComponent, childEvents.getUseOptionFlags(), childEvents.interfaceId);
										client.useOnType = Interface.getUseOptionString(childComponent);
										if (client.useOnType == null) {
											client.useOnType = "Null";
										}

										client.useOnTarget = childComponent.opBase + TextUtils.setTextColor(16777215);
									}

									int cycleProgress = childComponent.cycleSteps[keyIndex];
									if (childComponent.cycles == null) {
										childComponent.cycles = new int[childComponent.keyTriggeringAction.length];
									}

									if (cycleProgress != 0) {
										childComponent.cycles[keyIndex] = cycleProgress + client.cycles;
									} else {
										childComponent.cycles[keyIndex] = Integer.MAX_VALUE;
									}
								}

								if (!keyTriggered && childComponent.cycles != null) {
									childComponent.cycles[keyIndex] = 0;
								}
							}
						}

						if (clickTriggered) {
							InterfaceRenderer.method12221(childComponent, mouseRecord.getX() - componentX, mouseRecord.getY() - componentY);
						}

						if (client.draggingComponent != null && childComponent != client.draggingComponent && withinBoundaries && client.getComponentSettings(childComponent).dragEnabled()) {
							client.hoveredComponent = childComponent;
						}

						if (childComponent == client.selectedComponent) {
							client.isComponentSelected = true;
							client.selectedComponentX = componentX;
							client.selectedComponentY = componentY;
							client.selectedComponentWidth = client.selectedComponent.width;
							client.selectedComponentHeight = client.selectedComponent.height;
						}

						if (childComponent.hasScript || childComponent.contentType != null) {
							if (withinBoundaries && client.anInt7191 != 0 && childComponent.onMouseMove != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.hasMousePosition = true;
								hookRequest.source = childComponent;
								hookRequest.mouseY = client.anInt7191;
								hookRequest.params = childComponent.onMouseMove;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (client.draggingComponent != null) {
								clickTriggered = false;
								leftClick = false;
							} else if (MiniMenu.isOpen || childComponent.contentType != ComponentContentType.WORLDMAP && client.menuItemCount > 0) {
								clickTriggered = false;
								leftClick = false;
								withinBoundaries = false;
							}

							if (childComponent.contentType != null) {
								if (childComponent.contentType == ComponentContentType.GAME_WINDOW || childComponent.contentType == ComponentContentType.CONTENT_TYPE_1403) {
									client.GAME_SCREEN_INTERFACE = childComponent;
									Environment environment = Resource.mapRegion.getEnvironmentManager().getEnvironment();
									if (environment.getSkybox() != null && !Resource.mapRegionLoaderTask.isLoadingRegion()) {
										environment.getSkybox().method4217(Renderers.activeToolkit, childComponent.height, PreferenceManager.preferences.skyboxes.getValue());
									}

									if (childComponent.contentType == ComponentContentType.GAME_WINDOW && !MiniMenu.isOpen && mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
										MiniMenuBuilder.tileHovered(Renderers.activeToolkit, mouseX, mouseY);

										for (EntityOverhead overhead = (EntityOverhead) client.ENTITY_OVERHEADS.getFirst(); overhead != null; overhead = (EntityOverhead) client.ENTITY_OVERHEADS.getNext()) {
											if (mouseX >= overhead.startX && mouseX < overhead.endX && mouseY >= overhead.startY && mouseY < overhead.endY) {
												MiniMenu.resetEntries();
												PathingEntity.interfaceComponentOnEntity(overhead.pathingEntity);
											}
										}
									}

									int playerCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
									int[] playerIndices = PlayerUpdate.OUT_PLAYER_INDICES;

									for (int i = 0; i < playerCount; i++) {
										PlayerEntity player = PlayerUpdate.players[playerIndices[i]];
										if (player != null) {
											CS2Executor.executeIfTrigger(ClientTriggerType.IF_PLAYER_CYCLE, -1, player, playerIndices[i]);
											player.method15880(clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY);
										}
									}

									for (int i = 0; i < client.NPC_UPDATE_INDEX; i++) {
										int npcIndex = client.NPC_UPDATE_INDICES[i];
										ObjectNode npcNode = client.NPC_MAP.get(npcIndex);
										if (npcNode != null) {
											CS2Executor.executeIfTrigger(ClientTriggerType.IF_NPC_CYCLE, ((NpcEntity) npcNode.value).definitions.type, (PathingEntity) npcNode.value, npcIndex);
											((PathingEntity) npcNode.value).method15880(clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY);
										}
									}
									return;
								}

								if (childComponent.contentType == ComponentContentType.MINIMAP) {
									interactiveSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit);
									if (interactiveSprite == null || ComponentMinimap.minimapState != 0 && ComponentMinimap.minimapState != 3 || MiniMenu.isOpen || mouseX < clipMinX || mouseY < clipMinY || mouseX >= clipMaxX || mouseY >= clipMaxY) {
										return;
									}

									int relativeX = mouseX - componentX;
									int relativeY = mouseY - componentY;
									int rowStartX = interactiveSprite.rowStartX[relativeY];
									if (relativeX < rowStartX || relativeX > rowStartX + interactiveSprite.rowWidth[relativeY]) {
										return;
									}

									relativeX -= childComponent.width / 2;
									relativeY -= childComponent.height / 2;
									int angleY;
									if (Camera.cameraCalculationType == 4) {
										angleY = (int) Camera.camAngleY & 0x3fff;
									} else {
										angleY = (int) Camera.camAngleY + client.minimapRotation & 0x3fff;
									}

									int sinAngleY = MathUtils.SINE[angleY];
									int cosAngleY = MathUtils.COSINE[angleY];
									if (Camera.cameraCalculationType != 4) {
										sinAngleY = (client.minimapZoom + 256) * sinAngleY >> 8;
										cosAngleY = cosAngleY * (client.minimapZoom + 256) >> 8;
									}

									int worldX = sinAngleY * relativeY + relativeX * cosAngleY >> 14;
									int worldY = relativeY * cosAngleY - sinAngleY * relativeX >> 14;
									int mapX;
									int mapY;
									if (Camera.cameraCalculationType == 4) {
										mapX = (worldX >> 2) + (client.anInt7262 >> 9);
										mapY = (client.anInt7376 >> 9) - (worldY >> 2);
									} else {
										int playerSizeOffset = (PlayerUpdate.MY_PLAYER.getSize() - 1) * 256;
										Vector3 playerPosition = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords;
										mapX = ((int) playerPosition.x - playerSizeOffset >> 9) + (worldX >> 2);
										mapY = ((int) playerPosition.z - playerSizeOffset >> 9) - (worldY >> 2);
									}

									if (client.interfaceSelectionActive && (MiniMenuBuilder.USE_OPTIONS_FLAGS & 0x40) != 0) {
										Component interfaceComponent = Component.listIcomponent(client.anInt56, client.anInt7345);
										if (interfaceComponent != null) {
											MiniMenu.addEntry(client.useOnType, " ->", MiniMenu.useOnCursor, 59, childComponent.containerItemId, 1L, mapX, mapY, true, false, childComponent.slotId | childComponent.idHash, true);
										} else {
											InterfaceComponentRenderer.method1170();
										}
										return;
									}

									if (Game.STELLAR_DAWN == client.CURRENT_GAME) {
										MiniMenu.addEntry(LocalizedText.FACE_HERE.translate(client.CURRENT_LANGUAGE), "", -1, 60, -1, 1L, mapX, mapY, true, false, 0L, true);
									}

									MiniMenu.addEntry(InterfaceRenderer.translatedUptext, "", CursorManager.activeCursorId, 23, -1, 1L, mapX, mapY, true, false, 0L, true);
									return;
								}

								if (childComponent.contentType == ComponentContentType.WORLDMAP) {
									WorldMap.worldmapComponent = childComponent;
									if (withinBoundaries) {
										WorldMapSub1.isInteractive = true;
									}

									if (clickTriggered) {
										int clickX = (int) ((mouseRecord.getX() - componentX - childComponent.width / 2) * 2.0D / WorldMap.mapZoom);
										int clickY = (int) (-((mouseRecord.getY() - componentY - childComponent.height / 2) * 2.0D / WorldMap.mapZoom));
										int absX = clickX + WorldMap.mapSquareAbsX + WorldMapSub1.anInt5123;
										int absY = clickY + WorldMap.mapSquareAbsY + WorldMapSub1.anInt5624;
										WorldMapElement worldMapElement = WorldMapSub1.getStaticElement();
										if (worldMapElement == null) {
											return;
										}

										int[] worldPosition = new int[3];
										worldMapElement.method14775(absX, absY, worldPosition);
										if (worldPosition != null) {
											if (KeyRecorder.keyRecorder.held(82) && client.PLAYER_RIGHTS > 0) {
												CommandExecutor.executeTeleportCommand(worldPosition[0], worldPosition[1], worldPosition[2]);
												return;
											}

											CommandExecutor.teleportCommandRequested = true;
											CommandExecutor.teleportCommandPositionZ = worldPosition[0];
											CommandExecutor.teleportCommandPositionX = worldPosition[1];
											CommandExecutor.teleportCommandPositionY = worldPosition[2];
										}

										client.menuItemCount = 1;
										client.isMapMoving = false;
										client.mouseX = MouseRecord.recorder.getMouseX();
										client.mouseY = MouseRecord.recorder.getMouseY();
										return;
									}

									if (leftClick && client.menuItemCount > 0) {
										if (client.menuItemCount == 1 && (client.mouseX != MouseRecord.recorder.getMouseX() || client.mouseY != MouseRecord.recorder.getMouseY())) {
											Interface.anInt3008 = WorldMapSub1.anInt5123;
											InterfaceRenderer.anInt123 = WorldMapSub1.anInt5624;
											client.menuItemCount = 2;
										}

										if (client.menuItemCount == 2) {
											client.isMapMoving = true;
											WorldMapSub1.method1503(Interface.anInt3008 + (int) ((client.mouseX - MouseRecord.recorder.getMouseX()) * 2.0D / WorldMap.requestedMapZoom));
											WorldMapSub1.method5599(InterfaceRenderer.anInt123 - (int) ((client.mouseY - MouseRecord.recorder.getMouseY()) * 2.0D / WorldMap.requestedMapZoom));
										}
										return;
									}

									if (client.menuItemCount > 0 && !client.isMapMoving) {
										if ((client.anInt7339 == 1 || MiniMenu.method12562()) && MiniMenu.entryCount > 2) {
											StaticMethods.method4164(client.mouseX, client.mouseY);
										} else if (MiniMenu.hasEntries()) {
											StaticMethods.method4164(client.mouseX, client.mouseY);
										}
									}

									client.menuItemCount = 0;
									return;
								}

								if (childComponent.contentType == ComponentContentType.WORLD_MAP_OVERVIEW) {
									if (leftClick) {
										WorldMapSub1.markDestination(MouseRecord.recorder.getMouseX() - componentX, MouseRecord.recorder.getMouseY() - componentY, childComponent.width, childComponent.height);
									}
									return;
								}

								if (childComponent.contentType == ComponentContentType.CONTENT_TYPE_1406) {
									InterfaceComponentRenderer.method544(childComponent, componentX, componentY);
									return;
								}
							}

							if (!childComponent.isPressed && clickTriggered) {
								childComponent.isPressed = true;
								if (childComponent.onClick != null) {
									HookRequest hookRequest = new HookRequest();
									hookRequest.hasMousePosition = true;
									hookRequest.source = childComponent;
									hookRequest.mouseX = mouseRecord.getX() - componentX;
									hookRequest.mouseY = mouseRecord.getY() - componentY;
									hookRequest.params = childComponent.onClick;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}
							}

							if (childComponent.isPressed && leftClick && childComponent.anObjectArray1319 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.hasMousePosition = true;
								hookRequest.source = childComponent;
								hookRequest.mouseX = MouseRecord.recorder.getMouseX() - componentX;
								hookRequest.mouseY = MouseRecord.recorder.getMouseY() - componentY;
								hookRequest.params = childComponent.anObjectArray1319;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (childComponent.isPressed && !leftClick) {
								childComponent.isPressed = false;
								if (childComponent.onRelease != null) {
									HookRequest hookRequest = new HookRequest();
									hookRequest.hasMousePosition = true;
									hookRequest.source = childComponent;
									hookRequest.mouseX = MouseRecord.recorder.getMouseX() - componentX;
									hookRequest.mouseY = MouseRecord.recorder.getMouseY() - componentY;
									hookRequest.params = childComponent.onRelease;
									client.aClass4827404.addFirst(hookRequest);
								}
							}

							if (leftClick && childComponent.onHold != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.hasMousePosition = true;
								hookRequest.source = childComponent;
								hookRequest.mouseX = MouseRecord.recorder.getMouseX() - componentX;
								hookRequest.mouseY = MouseRecord.recorder.getMouseY() - componentY;
								hookRequest.params = childComponent.onHold;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (!childComponent.isHooked && withinBoundaries) {
								childComponent.isHooked = true;
								if (childComponent.onMouseOver != null) {
									HookRequest hookRequest = new HookRequest();
									hookRequest.hasMousePosition = true;
									hookRequest.source = childComponent;
									hookRequest.mouseX = MouseRecord.recorder.getMouseX() - componentX;
									hookRequest.mouseY = MouseRecord.recorder.getMouseY() - componentY;
									hookRequest.params = childComponent.onMouseOver;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}
							}

							if (childComponent.isHooked && withinBoundaries && childComponent.popupScript != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.hasMousePosition = true;
								hookRequest.source = childComponent;
								hookRequest.mouseX = MouseRecord.recorder.getMouseX() - componentX;
								hookRequest.mouseY = MouseRecord.recorder.getMouseY() - componentY;
								hookRequest.params = childComponent.popupScript;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (childComponent.isHooked && !withinBoundaries) {
								childComponent.isHooked = false;
								if (childComponent.onMouseLeaveScript != null) {
									HookRequest hookRequest = new HookRequest();
									hookRequest.hasMousePosition = true;
									hookRequest.source = childComponent;
									hookRequest.mouseX = MouseRecord.recorder.getMouseX() - componentX;
									hookRequest.mouseY = MouseRecord.recorder.getMouseY() - componentY;
									hookRequest.params = childComponent.onMouseLeaveScript;
									client.aClass4827404.addFirst(hookRequest);
								}
							}

							if (childComponent.anObjectArray1409 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1409;
								client.aClass4827233.addFirst(hookRequest);
							}

							HookRequest hookrequest54;
							if (childComponent.onKey != null && client.keyCycle > childComponent.lastKeyCycle) {
								if (childComponent.keyPressArray != null && client.keyCycle - childComponent.lastKeyCycle <= 32) {
									label969:
									for (int keyIndex = childComponent.lastKeyCycle; keyIndex < client.keyCycle; keyIndex++) {
										int i_25 = client.anIntArray7150[keyIndex & 0x1f];

										for (int i_26 = 0; i_26 < childComponent.keyPressArray.length; i_26++) {
											if (i_25 == childComponent.keyPressArray[i_26]) {
												hookrequest54 = new HookRequest();
												hookrequest54.source = childComponent;
												hookrequest54.params = childComponent.onKey;
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
												break label969;
											}
										}
									}
								} else {
									HookRequest hookRequest = new HookRequest();
									hookRequest.source = childComponent;
									hookRequest.params = childComponent.onKey;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}

								childComponent.lastKeyCycle = client.keyCycle;
							}

							if (childComponent.onScrollWheel != null && client.mouseWheelCycle > childComponent.lastMouseWheelCycle) {
								if (childComponent.mouseWheelArray != null && client.mouseWheelCycle - childComponent.lastMouseWheelCycle <= 32) {
									label945:
									for (int keyIndex = childComponent.lastMouseWheelCycle; keyIndex < client.mouseWheelCycle; keyIndex++) {
										int i_25 = client.recentMouseWheels[keyIndex & 0x1f];

										for (int i_26 = 0; i_26 < childComponent.mouseWheelArray.length; i_26++) {
											if (i_25 == childComponent.mouseWheelArray[i_26]) {
												hookrequest54 = new HookRequest();
												hookrequest54.source = childComponent;
												hookrequest54.params = childComponent.onScrollWheel;
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
												break label945;
											}
										}
									}
								} else {
									HookRequest hookRequest = new HookRequest();
									hookRequest.source = childComponent;
									hookRequest.params = childComponent.onScrollWheel;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}

								childComponent.lastMouseWheelCycle = client.mouseWheelCycle;
							}

							if (childComponent.anObjectArray1397 != null && client.pulseEventConfigsPtr > childComponent.anInt1443) {
								if (childComponent.varps != null && client.pulseEventConfigsPtr - childComponent.anInt1443 <= 32) {
									label921:
									for (int keyIndex = childComponent.anInt1443; keyIndex < client.pulseEventConfigsPtr; keyIndex++) {
										int i_25 = client.PULSE_EVENT_CONFIGS_CACHE[keyIndex & 0x1f];

										for (int i_26 = 0; i_26 < childComponent.varps.length; i_26++) {
											if (i_25 == childComponent.varps[i_26]) {
												hookrequest54 = new HookRequest();
												hookrequest54.source = childComponent;
												hookrequest54.params = childComponent.anObjectArray1397;
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
												break label921;
											}
										}
									}
								} else {
									HookRequest hookRequest = new HookRequest();
									hookRequest.source = childComponent;
									hookRequest.params = childComponent.anObjectArray1397;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}

								childComponent.anInt1443 = client.pulseEventConfigsPtr;
							}

							if (childComponent.mouseLeaveScript != null && client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS > childComponent.anInt1444) {
								if (childComponent.mouseLeaveArrayParams != null && client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - childComponent.anInt1444 <= 32) {
									label897:
									for (int keyIndex = childComponent.anInt1444; keyIndex < client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS; keyIndex++) {
										int i_25 = client.RECENTLY_USED_ITEM_CONTAINERS[keyIndex & 0x1f];

										for (int i_26 = 0; i_26 < childComponent.mouseLeaveArrayParams.length; i_26++) {
											if (i_25 == childComponent.mouseLeaveArrayParams[i_26]) {
												hookrequest54 = new HookRequest();
												hookrequest54.source = childComponent;
												hookrequest54.params = childComponent.mouseLeaveScript;
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
												break label897;
											}
										}
									}
								} else {
									HookRequest hookRequest = new HookRequest();
									hookRequest.source = childComponent;
									hookRequest.params = childComponent.mouseLeaveScript;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}

								childComponent.anInt1444 = client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS;
							}

							if (childComponent.anObjectArray1387 != null && client.anInt7384 > childComponent.anInt1445) {
								if (childComponent.anIntArray1402 != null && client.anInt7384 - childComponent.anInt1445 <= 32) {
									label873:
									for (int keyIndex = childComponent.anInt1445; keyIndex < client.anInt7384; keyIndex++) {
										int i_25 = client.anIntArray7283[keyIndex & 0x1f];

										for (int i_26 = 0; i_26 < childComponent.anIntArray1402.length; i_26++) {
											if (i_25 == childComponent.anIntArray1402[i_26]) {
												hookrequest54 = new HookRequest();
												hookrequest54.source = childComponent;
												hookrequest54.params = childComponent.anObjectArray1387;
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
												break label873;
											}
										}
									}
								} else {
									HookRequest hookRequest = new HookRequest();
									hookRequest.source = childComponent;
									hookRequest.params = childComponent.anObjectArray1387;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}

								childComponent.anInt1445 = client.anInt7384;
							}

							if (childComponent.anObjectArray1407 != null && client.CLAN_VAR_COUNTER > childComponent.anInt1448) {
								if (childComponent.anIntArray1408 != null && client.CLAN_VAR_COUNTER - childComponent.anInt1448 <= 32) {
									label849:
									for (int keyIndex = childComponent.anInt1448; keyIndex < client.CLAN_VAR_COUNTER; keyIndex++) {
										int i_25 = client.CLAN_VAR_KEYS[keyIndex & 0x1f];

										for (int i_26 = 0; i_26 < childComponent.anIntArray1408.length; i_26++) {
											if (i_25 == childComponent.anIntArray1408[i_26]) {
												hookrequest54 = new HookRequest();
												hookrequest54.source = childComponent;
												hookrequest54.params = childComponent.anObjectArray1407;
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54);
												break label849;
											}
										}
									}
								} else {
									HookRequest hookRequest = new HookRequest();
									hookRequest.source = childComponent;
									hookRequest.params = childComponent.anObjectArray1407;
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
								}

								childComponent.anInt1448 = client.CLAN_VAR_COUNTER;
							}

							if (client.anInt7391 > childComponent.anInt1442 && childComponent.anObjectArray1413 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1413;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (client.anInt7386 > childComponent.anInt1442 && childComponent.anObjectArray1415 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1415;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (client.anInt7179 > childComponent.anInt1442 && childComponent.anObjectArray1416 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1416;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (client.anInt7192 > childComponent.anInt1442 && childComponent.anObjectArray1383 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1383;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (client.anInt7395 > childComponent.anInt1442 && childComponent.anObjectArray1418 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1418;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (client.anInt7320 > childComponent.anInt1442 && childComponent.anObjectArray1361 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1361;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							if (client.anInt7397 > childComponent.anInt1442 && childComponent.anObjectArray1419 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1419;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}

							childComponent.anInt1442 = client.anInt7347;
							if (childComponent.anObjectArray1292 != null) {
								for (int keyIndex = 0; keyIndex < client.keyCachePointerPoss; keyIndex++) {
									HookRequest hookrequest44 = new HookRequest();
									hookrequest44.source = childComponent;
									hookrequest44.typedKeyCode = client.KEYS_PRESSED[keyIndex].getCode();
									hookrequest44.typedKeyChar = client.KEYS_PRESSED[keyIndex].getCharacter();
									hookrequest44.params = childComponent.anObjectArray1292;
									client.PENDING_HOOK_REQUESTS.addFirst(hookrequest44);
								}
							}

							if (client.aBool7355 && childComponent.anObjectArray1353 != null) {
								HookRequest hookRequest = new HookRequest();
								hookRequest.source = childComponent;
								hookRequest.params = childComponent.anObjectArray1353;
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest);
							}
						}

						if (childComponent.type == ComponentType.SPRITE && childComponent.skyboxId != -1) {
							childComponent.calculateLighting(TypeLists.SKYBOX, TypeLists.SUN).method4217(Renderers.activeToolkit, childComponent.height, PreferenceManager.preferences.skyboxes.getValue());
						}

						Component.drawComponentObjSprite(childComponent);
						if (childComponent.type == ComponentType.CONTAINER) {
							processInterfaceCycle(parentInterface, childComponents, childComponent.idHash, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY);
							if (childComponent.itemSlots != null) {
								processInterfaceCycle(parentInterface, childComponent.itemSlots, childComponent.idHash, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY);
							}

							SubInterface subInterface = client.OPEN_INTERFACES.get(childComponent.idHash);
							if (subInterface != null) {
								if (Game.RUNESCAPE == client.CURRENT_GAME && subInterface.overlay == 0 && !MiniMenu.isOpen && withinBoundaries && !client.DISPLAY_HIDDEN_MENU_OPTIONS) {
									MiniMenu.resetEntries();
								}

								Interface.open_sub(subInterface, subInterface.subInterfaceId, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX, componentY, mouseX, mouseY);
							}
						}
					}
				}
			}
		});
	}

	public static void resetPendingHooks() {
		for (HookRequest hook = (HookRequest) client.PENDING_HOOK_REQUESTS.getFirst(); hook != null; hook = (HookRequest) client.PENDING_HOOK_REQUESTS.next()) {
			if (hook.hasMousePosition) {
				hook.unlink();
				hook.source.isHooked = false;
			}
		}

		if (InterfaceRenderer.anInt8308 == 0) {
			client.draggingComponent = null;
			client.selectedComponent = null;
		}

		client.menuItemCount = 0;
		WorldMapSub1.isInteractive = false;
		CommandExecutor.teleportCommandRequested = false;
	}

	public static void updateInterfaceDimensions(Component[] itemSlots, int idHash, int width, int height, boolean bool4) {
	    for (int i_6 = 0; i_6 < itemSlots.length; i_6++) {
	        Component component = itemSlots[i_6];
	        if (component != null && component.parentId == idHash) {
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
	    if (client.DISPLAY_HIDDEN_MENU_OPTIONS && (client.getComponentSettings(interfaceComponent).settingsHash != 0 || interfaceComponent.type == ComponentType.CONTAINER)) {
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
	    if (client.draggingComponent == null && !MiniMenu.isOpen && component != null && client.hasDepthFlags(component)) {
	        client.draggingComponent = component;
	        client.selectedComponent = Component.getDepthChild(component);
	        client.mouseX = offsetX;
	        client.mouseY = offsetY;
	        InterfaceRenderer.anInt8308 = 0;
	        client.aBool7364 = false;
	    }
	
	}

	public static boolean isComponentHidden(Component component) {
	    if (client.DISPLAY_HIDDEN_MENU_OPTIONS) {
	        if (client.getComponentSettings(component).settingsHash != 0) {
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
