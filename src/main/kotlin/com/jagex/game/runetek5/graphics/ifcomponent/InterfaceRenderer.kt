package com.jagex.game.runetek5.graphics.ifcomponent

import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.interfaceComponentHovered
import com.jagex.game.runetek5.client.menu.tileHovered
import com.jagex.game.runetek5.client.menu.USE_OPTIONS_FLAGS
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentContentType
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.entity.pathingentity.EntityOverhead
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.trigger.ClientTriggerType
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.TextUtils

object InterfaceRenderer {

	@JvmField
	var translatedUptext: String? = null

	@JvmField
	var worldMapDragStartY: Int = 0

	@JvmField
	var dragTickCount: Int = 0

	/**
	 * Draws an interface and it's component,
	 * also processes interface interaction
	 */
	@JvmStatic
	fun processInterfaceCycle(
		parentInterface: Interface,
		childComponents: Array<Component?>,
		parentId: Int,
		clipStartX: Int,
		clipStartY: Int,
		clipEndX: Int,
		clipEndY: Int,
		parentOffsetX: Int,
		parentOffsetY: Int,
		mouseX: Int,
		mouseY: Int
	) {
		for (childComponent in childComponents) {
			if (childComponent != null && parentId == childComponent.parentId) {
				val componentX = parentOffsetX + childComponent.x
				val componentY = parentOffsetY + childComponent.y
				val clipMinX: Int
				val clipMinY: Int
				val clipMaxX: Int
				val clipMaxY: Int
				if (childComponent.type == ComponentType.TYPE_2) {
					clipMinX = clipStartX
					clipMinY = clipStartY
					clipMaxX = clipEndX
					clipMaxY = clipEndY
				} else {
					var componentRightX = componentX + childComponent.width
					var componentBottomY = componentY + childComponent.height
					if (childComponent.type == ComponentType.LINE) {
						componentRightX++
						componentBottomY++
					}
					clipMinX = componentX.coerceAtLeast(clipStartX)
					clipMinY = componentY.coerceAtLeast(clipStartY)
					clipMaxX = componentRightX.coerceAtMost(clipEndX)
					clipMaxY = componentBottomY.coerceAtMost(clipEndY)
				}

				val withinClipBounds = clipMinX < clipMaxX && clipMinY < clipMaxY

				if (childComponent.type != ComponentType.CONTAINER && !childComponent.hasScript && Client.getComponentSettings(childComponent)!!.settingsHash == 0 && childComponent != Client.selectedComponent && !childComponent.isIgnoredContentType()) {
					if (withinClipBounds) {
						Component.drawComponentObjSprite(childComponent)
					}
				} else if (!isComponentHidden(childComponent)) {
					val dragging = Client.draggingComponent
					if (childComponent == dragging && dragging != null && Client.hasDepthFlags(dragging)) {
						Client.isDraggingComponent = true
						Client.dragStartX = componentX
						Client.dragStartY = componentY
					}

					if (childComponent.hasInteraction || withinClipBounds) {
						if (childComponent.preventClickThrough && mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
							resetPendingHooks()
							if (!MiniMenu.isOpen) {
								MiniMenu.resetEntries()
							}
						}

						val spriteHitTest = childComponent.clickMask && childComponent.type == ComponentType.SPRITE && childComponent.transparency == 0 && childComponent.skyboxId < 0 && childComponent.containerItemId == -1 && childComponent.videoFileId == -1 && !childComponent.tiling && childComponent.angle2d == 0
						var withinBoundaries = false
						if (mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
							if (spriteHitTest) {
								val interactiveSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit!!)
								if (interactiveSprite != null && interactiveSprite.width == childComponent.width && childComponent.height == interactiveSprite.height) {
									val spriteX = mouseX - componentX
									val spriteY = mouseY - componentY
									if (spriteY >= 0 && spriteY < interactiveSprite.rowStartX.size) {
										val rowStart = interactiveSprite.rowStartX[spriteY]
										if (spriteX >= rowStart && spriteX <= rowStart + interactiveSprite.rowWidth[spriteY]) {
											withinBoundaries = true
										}
									}
								} else {
									withinBoundaries = true
								}
							} else {
								withinBoundaries = true
							}
						}

						if (!Client.interfaceSelectionActive && withinBoundaries) {
							if (childComponent.moveOverCursor >= 0) {
								Client.defaultMenuCursor = childComponent.moveOverCursor
							} else if (childComponent.preventClickThrough) {
								Client.defaultMenuCursor = -1
							}
						}

						if (!MiniMenu.isOpen && withinBoundaries && !parentInterface.preventInteraction) {
							interfaceComponentHovered(childComponent)
						}

						val leftClick = MouseRecord.recorder?.leftButtonDown() == true && withinBoundaries
						var clickTriggered = false
						val mouseRecord = Client.MOUSE_MOVEMENTS.getFirst() as? MouseRecord

						if (mouseRecord != null && mouseRecord.getClickType() == 0 && mouseRecord.getX() >= clipMinX && mouseRecord.getY() >= clipMinY && mouseRecord.getX() < clipMaxX && mouseRecord.getY() < clipMaxY) {
							if (spriteHitTest) {
								val interactiveSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit!!)
								if (interactiveSprite != null) {
									if (childComponent.width == interactiveSprite.width && childComponent.height == interactiveSprite.height) {
										val spriteX = mouseRecord.getX() - componentX
										val spriteY = mouseRecord.getY() - componentY
										if (spriteY >= 0 && spriteY < interactiveSprite.rowStartX.size) {
											val rowStart = interactiveSprite.rowStartX[spriteY]
											if (spriteX >= rowStart && spriteX <= rowStart + interactiveSprite.rowWidth[spriteY]) {
												clickTriggered = true
											}
										}
									} else {
										clickTriggered = true
									}
								} else {
									// If you want: treat null as "whole area clickable" OR "not clickable".
									// Match the behavior from your earlier logic:
									clickTriggered = true
									// or: continue / leave clickTriggered false
								}
							} else {
								clickTriggered = true
							}
						}

						if (childComponent.hasInteraction && !Console.isOpen()) {
							for (opIndex in childComponent.keyTriggeringAction!!.indices) {
								var keyTriggered = false
								var hasAction = false
								if (childComponent.keyCharCodes!![opIndex] > 0) {
									for (i in 0 until Client.keyCachePointerPoss) {
										if (childComponent.keyCharCodes!![opIndex] == Client.KEYS_PRESSED[i]!!.getCharacter().code) {
											keyTriggered = true
											val cycles = childComponent.cycles
											if (cycles == null || cycles[opIndex] <= Client.cycles) {
												hasAction = true
											}
											break
										}
									}
								}

								if (!keyTriggered && childComponent.keyTriggeringAction!![opIndex] != null) {
									for (key in childComponent.keyTriggeringAction!![opIndex]!!.indices) {
										if (KeyRecorder.keyRecorder?.held(childComponent.keyTriggeringAction!![opIndex]!![key].toInt()) == true) {
											keyTriggered = true
											val cycles = childComponent.cycles
											if (cycles != null && cycles[opIndex] > Client.cycles) {
												break
											}

											val modifierMask = childComponent.keyTriggerParams!![opIndex]!![key]
											if (modifierMask.toInt() == 0 || (modifierMask.toInt() and 0x8 == 0 || !(KeyRecorder.keyRecorder?.held(86) == true) && !(KeyRecorder.keyRecorder?.held(82) == true) && !(KeyRecorder.keyRecorder?.held(81) == true)) && (modifierMask.toInt() and 0x2 == 0 || KeyRecorder.keyRecorder?.held(86) == true) && (modifierMask.toInt() and 0x1 == 0 || KeyRecorder.keyRecorder?.held(82) == true) && (modifierMask.toInt() and 0x4 == 0 || KeyRecorder.keyRecorder?.held(81) == true)) {
												hasAction = true
												break
											}
										}
									}
								}

								if (hasAction) {
									if (opIndex < 10) {
										Interface.ifOptionInteract(opIndex + 1, childComponent.idHash, childComponent.slotId, "")
									} else if (opIndex == 10) {
										InterfaceComponentRenderer.cancelInterfaceSelection()
										val componentSettings = Client.getComponentSettings(childComponent)!!
										setUseOptionFlags(childComponent, componentSettings.getUseOptionFlags(), componentSettings.interfaceId)
										Client.useOnType = Interface.getUseOptionString(childComponent)
										if (Client.useOnType == null) {
											Client.useOnType = "Null"
										}

										Client.useOnTarget = childComponent.opBase + TextUtils.setTextColor(16777215)
									}

									val cycleDelay = childComponent.cycleSteps!![opIndex]
									if (childComponent.cycles == null) {
										childComponent.cycles = IntArray(childComponent.keyTriggeringAction!!.size)
									}

									if (cycleDelay != 0) {
										childComponent.cycles!![opIndex] = cycleDelay + Client.cycles
									} else {
										childComponent.cycles!![opIndex] = Int.MAX_VALUE
									}
								}

								val cycles = childComponent.cycles
								if (!keyTriggered && cycles != null) {
									cycles[opIndex] = 0
								}
							}
						}

						if (clickTriggered) {
							startDragging(childComponent, mouseRecord?.getX()?.minus(componentX) ?: 0, mouseRecord?.getY()?.minus(componentY) ?: 0)
						}

						if (Client.draggingComponent != null && childComponent != Client.draggingComponent && withinBoundaries && Client.getComponentSettings(childComponent)!!.dragEnabled()) {
							Client.hoveredComponent = childComponent
						}

						if (childComponent == Client.selectedComponent) {
							Client.isComponentSelected = true
							Client.selectedComponentX = componentX
							Client.selectedComponentY = componentY
							val selected = Client.selectedComponent
							Client.selectedComponentWidth = selected!!.width
							Client.selectedComponentHeight = selected.height
						}

						if (childComponent.hasScript || childComponent.contentType != null) {
							if (withinBoundaries && Client.mouseWheelDelta != 0 && childComponent.onMouseMove != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseY = Client.mouseWheelDelta
								hookRequest.params = childComponent.onMouseMove
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							var clickAllowed = clickTriggered
							var leftClickAllowed = leftClick
							if (Client.draggingComponent != null) {
								clickAllowed = false
								leftClickAllowed = false
							} else if (MiniMenu.isOpen || childComponent.contentType != ComponentContentType.WORLDMAP && Client.menuItemCount > 0) {
								clickAllowed = false
								leftClickAllowed = false
								withinBoundaries = false
							}

							if (childComponent.contentType != null) {
								if (childComponent.contentType == ComponentContentType.GAME_WINDOW || childComponent.contentType == ComponentContentType.CONTENT_TYPE_1403) {
									Client.GAME_SCREEN_INTERFACE = childComponent
									val environment = Resource.mapRegion?.environmentManager?.environment
									if (environment?.skybox != null && Resource.mapRegionLoaderTask?.isLoadingRegion() != true) {
										environment.skybox?.updateForHeight(Renderers.activeToolkit!!, childComponent.height, PreferenceManager.preferences?.skyboxes?.value ?: 0)
									}

									if (childComponent.contentType == ComponentContentType.GAME_WINDOW && !MiniMenu.isOpen && mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
										tileHovered(Renderers.activeToolkit!!, mouseX, mouseY)

										var overhead = Client.ENTITY_OVERHEADS.getFirst() as EntityOverhead?
										while (overhead != null) {
											if (mouseX >= overhead.startX && mouseX < overhead.endX && mouseY >= overhead.startY && mouseY < overhead.endY) {
												MiniMenu.resetEntries()
												PathingEntity.interfaceComponentOnEntity(overhead.pathingEntity!!)
											}
											overhead = Client.ENTITY_OVERHEADS.getNext() as EntityOverhead?
										}
									}

									val playerCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES
									val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES

									for (i in 0 until playerCount) {
										val player = PlayerUpdate.players[playerIndices[i]]
										if (player != null) {
											CS2Executor.executeIfTrigger(ClientTriggerType.IF_PLAYER_CYCLE, -1, player, playerIndices[i])
											player.processOverheadInterfaceCycle(clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
										}
									}

									for (i in 0 until Client.NPC_UPDATE_INDEX) {
										val npcIndex = Client.NPC_UPDATE_INDICES[i]
										val npcNode = Client.NPC_MAP[npcIndex.toLong()]
										if (npcNode != null) {
											val npcEntity = npcNode.value as NpcEntity
											CS2Executor.executeIfTrigger(ClientTriggerType.IF_NPC_CYCLE, npcEntity.definitions?.type ?: 0, npcNode.value as PathingEntity, npcIndex)
											(npcNode.value as PathingEntity).processOverheadInterfaceCycle(clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
										}
									}
									continue
								}

								if (childComponent.contentType == ComponentContentType.MINIMAP) {
									val interactiveSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit!!)
									if (interactiveSprite == null || ComponentMinimap.minimapState != 0 && ComponentMinimap.minimapState != 3 || MiniMenu.isOpen || mouseX < clipMinX || mouseY < clipMinY || mouseX >= clipMaxX || mouseY >= clipMaxY) {
										continue
									}

									var relativeX = mouseX - componentX
									var relativeY = mouseY - componentY
									val rowStart = interactiveSprite.rowStartX[relativeY]
									if (relativeX < rowStart || relativeX > rowStart + interactiveSprite.rowWidth[relativeY]) {
										continue
									}

									relativeX -= childComponent.width / 2
									relativeY -= childComponent.height / 2
									val angleY: Int
									if (Camera.cameraCalculationType == 4) {
										angleY = Camera.camAngleY.toInt() and 0x3fff
									} else {
										angleY = (Camera.camAngleY.toInt() + Client.minimapRotation) and 0x3fff
									}

									var sinAngleY = MathUtils.SINE[angleY]
									var cosAngleY = MathUtils.COSINE[angleY]
									if (Camera.cameraCalculationType != 4) {
										sinAngleY = (Client.minimapZoom + 256) * sinAngleY shr 8
										cosAngleY = cosAngleY * (Client.minimapZoom + 256) shr 8
									}

									val worldX = sinAngleY * relativeY + relativeX * cosAngleY shr 14
									val worldY = relativeY * cosAngleY - sinAngleY * relativeX shr 14
									val mapX: Int
									val mapY: Int
									if (Camera.cameraCalculationType == 4) {
										mapX = (worldX shr 2) + (Client.cameraTargetAbsX shr 9)
										mapY = (Client.cameraTargetAbsY shr 9) - (worldY shr 2)
									} else {
										val playerSizeOffset = (PlayerUpdate.MY_PLAYER!!.getSize() - 1) * 256
										val playerPosition = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
										mapX = (playerPosition.x.toInt() - playerSizeOffset shr 9) + (worldX shr 2)
										mapY = (playerPosition.z.toInt() - playerSizeOffset shr 9) - (worldY shr 2)
									}

									if (Client.interfaceSelectionActive && USE_OPTIONS_FLAGS and 0x40 != 0) {
										val selectedComponent = Component.listIcomponent(Client.selectedComponentHash, Client.selectedSlotId)
										if (selectedComponent != null) {
											MiniMenu.addEntry(Client.useOnType!!, " ->", MiniMenu.useOnCursor, 59, childComponent.containerItemId, 1, mapX, mapY, true, false, (childComponent.slotId or childComponent.idHash).toLong(), true)
										} else {
											InterfaceComponentRenderer.cancelInterfaceSelection()
										}
										continue
									}

									if (Game.STELLAR_DAWN == Client.CURRENT_GAME) {
										MiniMenu.addEntry(LocalizedText.FACE_HERE.translate(Client.CURRENT_LANGUAGE!!), "", -1, 60, -1, 1, mapX, mapY, true, false, 0, true)
									}

                                    translatedUptext?.let { MiniMenu.addEntry(it, "", CursorManager.activeCursorId, 23, -1, 1, mapX, mapY, true, false, 0, true) }
									continue
								}

								if (childComponent.contentType == ComponentContentType.WORLDMAP) {
									WorldMap.worldmapComponent = childComponent
									if (withinBoundaries) {
										WorldMapDisplay.isInteractive = true
									}

									if (clickAllowed) {
										val clickX = ((mouseRecord?.getX()?.minus(componentX)?.minus(childComponent.width / 2))?.times(2.0)?.div(WorldMap.mapZoom))?.toInt()
										val clickY = mouseRecord?.getY()?.minus(componentY)?.minus(childComponent.height / 2)?.let { (-(it) * 2.0 / WorldMap.mapZoom).toInt() }
										val absX = clickX?.plus(WorldMap.mapSquareAbsX)?.plus(WorldMapDisplay.mapCenterX) ?: 0
										val absY = clickY?.plus(WorldMap.mapSquareAbsY)?.plus(WorldMapDisplay.mapCenterY) ?: 0
										val worldMapElement = WorldMapDisplay.getStaticElement()
										if (worldMapElement == null)
											continue

										val worldPosition = IntArray(3)
										worldMapElement.mapToGameCoords(absX, absY, worldPosition)
										if (worldPosition != null) {
											if (KeyRecorder.keyRecorder?.held(82) == true && Client.PLAYER_RIGHTS > 0) {
												CommandExecutor.executeTeleportCommand(worldPosition[0], worldPosition[1], worldPosition[2])
												continue
											}

											CommandExecutor.teleportCommandRequested = true
											CommandExecutor.teleportCommandPositionZ = worldPosition[0]
											CommandExecutor.teleportCommandPositionX = worldPosition[1]
											CommandExecutor.teleportCommandPositionY = worldPosition[2]
										}

										Client.menuItemCount = 1
										Client.isMapMoving = false
										Client.mouseX = MouseRecord.recorder?.getMouseX() ?: 0
										Client.mouseY = MouseRecord.recorder?.getMouseY() ?: 0
										continue
									}

									if (leftClickAllowed && Client.menuItemCount > 0) {
										if (Client.menuItemCount == 1 && (Client.mouseX != MouseRecord.recorder?.getMouseX() ?: 0 || Client.mouseY != MouseRecord.recorder?.getMouseY() ?: 0)) {
											Interface.worldMapDragStartX = WorldMapDisplay.mapCenterX
											worldMapDragStartY = WorldMapDisplay.mapCenterY
											Client.menuItemCount = 2
										}

										if (Client.menuItemCount == 2) {
											Client.isMapMoving = true
											WorldMapDisplay.setMapCenterX(Interface.worldMapDragStartX + ((Client.mouseX - (MouseRecord.recorder?.getMouseX() ?: 0)) * 2.0 / WorldMap.requestedMapZoom).toInt())
											WorldMapDisplay.setMapCenterY(worldMapDragStartY - ((Client.mouseY - (MouseRecord.recorder?.getMouseY() ?: 0)) * 2.0 / WorldMap.requestedMapZoom).toInt())
										}
										continue
									}

									if (Client.menuItemCount > 0 && !Client.isMapMoving) {
										if ((Client.menuOpenMode == 1 || MiniMenu.isTopEntryInterfaceAction()) && MiniMenu.entryCount > 2) {
											StaticMethods.processMenuSelection(Client.mouseX, Client.mouseY)
										} else if (MiniMenu.hasEntries()) {
											StaticMethods.processMenuSelection(Client.mouseX, Client.mouseY)
										}
									}

									Client.menuItemCount = 0
									continue
								}

								if (childComponent.contentType == ComponentContentType.WORLD_MAP_OVERVIEW) {
									if (leftClickAllowed) {
										WorldMapDisplay.markDestination((MouseRecord.recorder?.getMouseX() ?: 0) - componentX, (MouseRecord.recorder?.getMouseY() ?: 0) - componentY, childComponent.width, childComponent.height)
									}
									continue
								}

								if (childComponent.contentType == ComponentContentType.CONTENT_TYPE_1406) {
									InterfaceComponentRenderer.setChatDisplay(childComponent, componentX, componentY)
									continue
								}
							}

							if (!childComponent.isPressed && clickAllowed) {
								childComponent.isPressed = true
								if (childComponent.onClick != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = mouseRecord?.getX()?.minus(componentX) ?: 0
									hookRequest.mouseY = mouseRecord?.getY()?.minus(componentY) ?: 0
									hookRequest.params = childComponent.onClick
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}
							}

							if (childComponent.isPressed && leftClickAllowed && childComponent.onClickRepeat != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseX = (MouseRecord.recorder?.getMouseX() ?: 0) - componentX
								hookRequest.mouseY = (MouseRecord.recorder?.getMouseY() ?: 0) - componentY
								hookRequest.params = childComponent.onClickRepeat
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (childComponent.isPressed && !leftClickAllowed) {
								childComponent.isPressed = false
								if (childComponent.onRelease != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = (MouseRecord.recorder?.getMouseX() ?: 0) - componentX
									hookRequest.mouseY = (MouseRecord.recorder?.getMouseY() ?: 0) - componentY
									hookRequest.params = childComponent.onRelease
									Client.MOUSE_HOOK_REQUESTS.addFirst(hookRequest)
								}
							}

							if (leftClickAllowed && childComponent.onHold != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseX = (MouseRecord.recorder?.getMouseX() ?: 0) - componentX
								hookRequest.mouseY = (MouseRecord.recorder?.getMouseY() ?: 0) - componentY
								hookRequest.params = childComponent.onHold
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (!childComponent.isHooked && withinBoundaries) {
								childComponent.isHooked = true
								if (childComponent.onMouseOver != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = (MouseRecord.recorder?.getMouseX() ?: 0) - componentX
									hookRequest.mouseY = (MouseRecord.recorder?.getMouseY() ?: 0) - componentY
									hookRequest.params = childComponent.onMouseOver
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}
							}

							if (childComponent.isHooked && withinBoundaries && childComponent.popupScript != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseX = (MouseRecord.recorder?.getMouseX() ?: 0) - componentX
								hookRequest.mouseY = (MouseRecord.recorder?.getMouseY() ?: 0) - componentY
								hookRequest.params = childComponent.popupScript
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (childComponent.isHooked && !withinBoundaries) {
								childComponent.isHooked = false
								if (childComponent.onMouseLeaveScript != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = (MouseRecord.recorder?.getMouseX() ?: 0) - componentX
									hookRequest.mouseY = (MouseRecord.recorder?.getMouseY() ?: 0) - componentY
									hookRequest.params = childComponent.onMouseLeaveScript
									Client.MOUSE_HOOK_REQUESTS.addFirst(hookRequest)
								}
							}

							if (childComponent.onTimer != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onTimer
								Client.TICK_HOOK_REQUESTS.addFirst(hookRequest)
							}

							var filterHookRequest: HookRequest
							if (childComponent.onKey != null && Client.keyCycle > childComponent.lastKeyCycle) {
								val keyPressArray = childComponent.keyPressArray
								if (keyPressArray != null && Client.keyCycle - childComponent.lastKeyCycle <= 32) {
									keyLoop@ for (cycle in childComponent.lastKeyCycle until Client.keyCycle) {
										val keyId = Client.recentKeyUpdates[cycle and 0x1f]

										for (j in keyPressArray.indices) {
											if (keyId == keyPressArray[j]) {
												filterHookRequest = HookRequest()
												filterHookRequest.source = childComponent
												filterHookRequest.params = childComponent.onKey
												Client.PENDING_HOOK_REQUESTS.addFirst(filterHookRequest)
												break@keyLoop
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.onKey
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastKeyCycle = Client.keyCycle
							}

							if (childComponent.onScrollWheel != null && Client.mouseWheelCycle > childComponent.lastMouseWheelCycle) {
								if (childComponent.mouseWheelArray != null && Client.mouseWheelCycle - childComponent.lastMouseWheelCycle <= 32) {
									wheelLoop@ for (cycle in childComponent.lastMouseWheelCycle until Client.mouseWheelCycle) {
										val wheelId = Client.recentMouseWheels[cycle and 0x1f]

										for (j in childComponent.mouseWheelArray!!.indices) {
											if (wheelId == childComponent.mouseWheelArray!![j]) {
												filterHookRequest = HookRequest()
												filterHookRequest.source = childComponent
												filterHookRequest.params = childComponent.onScrollWheel
												Client.PENDING_HOOK_REQUESTS.addFirst(filterHookRequest)
												break@wheelLoop
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.onScrollWheel
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastMouseWheelCycle = Client.mouseWheelCycle
							}

							if (childComponent.onVarpTransmit != null && Client.pulseEventConfigsPtr > childComponent.lastVarpTransmitCycle) {
								if (childComponent.varps != null && Client.pulseEventConfigsPtr - childComponent.lastVarpTransmitCycle <= 32) {
									varpLoop@ for (cycle in childComponent.lastVarpTransmitCycle until Client.pulseEventConfigsPtr) {
										val varpId = Client.PULSE_EVENT_CONFIGS_CACHE[cycle and 0x1f]

										for (j in childComponent.varps!!.indices) {
											if (varpId == childComponent.varps!![j]) {
												filterHookRequest = HookRequest()
												filterHookRequest.source = childComponent
												filterHookRequest.params = childComponent.onVarpTransmit
												Client.PENDING_HOOK_REQUESTS.addFirst(filterHookRequest)
												break@varpLoop
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.onVarpTransmit
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastVarpTransmitCycle = Client.pulseEventConfigsPtr
							}

							if (childComponent.mouseLeaveScript != null && Client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS > childComponent.lastContainerTransmitCycle) {
								if (childComponent.mouseLeaveArrayParams != null && Client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - childComponent.lastContainerTransmitCycle <= 32) {
									containerLoop@ for (cycle in childComponent.lastContainerTransmitCycle until Client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS) {
										val containerId = Client.RECENTLY_USED_ITEM_CONTAINERS[cycle and 0x1f]

										for (j in childComponent.mouseLeaveArrayParams!!.indices) {
											if (containerId == childComponent.mouseLeaveArrayParams!![j]) {
												filterHookRequest = HookRequest()
												filterHookRequest.source = childComponent
												filterHookRequest.params = childComponent.mouseLeaveScript
												Client.PENDING_HOOK_REQUESTS.addFirst(filterHookRequest)
												break@containerLoop
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.mouseLeaveScript
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastContainerTransmitCycle = Client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS
							}

							if (childComponent.onStatTransmit != null && Client.skillUpdateCycle > childComponent.lastStatTransmitCycle) {
								if (childComponent.statTransmitFilter != null && Client.skillUpdateCycle - childComponent.lastStatTransmitCycle <= 32) {
									statLoop@ for (cycle in childComponent.lastStatTransmitCycle until Client.skillUpdateCycle) {
										val statId = Client.recentSkillUpdates[cycle and 0x1f]

										for (j in childComponent.statTransmitFilter!!.indices) {
											if (statId == childComponent.statTransmitFilter!![j]) {
												filterHookRequest = HookRequest()
												filterHookRequest.source = childComponent
												filterHookRequest.params = childComponent.onStatTransmit
												Client.PENDING_HOOK_REQUESTS.addFirst(filterHookRequest)
												break@statLoop
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.onStatTransmit
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastStatTransmitCycle = Client.skillUpdateCycle
							}

							if (childComponent.onClanVarTransmit != null && Client.CLAN_VAR_COUNTER > childComponent.lastClanVarTransmitCycle) {
								if (childComponent.clanVarFilter != null && Client.CLAN_VAR_COUNTER - childComponent.lastClanVarTransmitCycle <= 32) {
									clanVarLoop@ for (cycle in childComponent.lastClanVarTransmitCycle until Client.CLAN_VAR_COUNTER) {
										val clanVarId = Client.CLAN_VAR_KEYS[cycle and 0x1f]

										for (j in childComponent.clanVarFilter!!.indices) {
											if (clanVarId == childComponent.clanVarFilter!![j]) {
												filterHookRequest = HookRequest()
												filterHookRequest.source = childComponent
												filterHookRequest.params = childComponent.onClanVarTransmit
												Client.PENDING_HOOK_REQUESTS.addFirst(filterHookRequest)
												break@clanVarLoop
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.onClanVarTransmit
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastClanVarTransmitCycle = Client.CLAN_VAR_COUNTER
							}

							if (Client.lastChatMessageTick > childComponent.lastServerTick && childComponent.onChatTransmit != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onChatTransmit
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (Client.lastFriendIgnoreUpdateTick > childComponent.lastServerTick && childComponent.onFriendTransmit != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onFriendTransmit
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (Client.lastFriendsChatUpdateTick > childComponent.lastServerTick && childComponent.onFriendsChatTransmit != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onFriendsChatTransmit
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (Client.lastClanSettingsUpdateTick > childComponent.lastServerTick && childComponent.onClanSettingsTransmit != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onClanSettingsTransmit
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (Client.lastClanChannelUpdateTick > childComponent.lastServerTick && childComponent.onClanChannelTransmit != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onClanChannelTransmit
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (Client.lastGrandExchangeUpdateTick > childComponent.lastServerTick && childComponent.onStockTransmit != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onStockTransmit
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (Client.lastMiscUpdateTick > childComponent.lastServerTick && childComponent.onMiscTransmit != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onMiscTransmit
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							childComponent.lastServerTick = Client.serverTick
							if (childComponent.onKeyType != null) {
								for (keyIndex in 0 until Client.keyCachePointerPoss) {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.typedKeyCode = Client.KEYS_PRESSED[keyIndex]!!.getCode()
									hookRequest.typedKeyChar = Client.KEYS_PRESSED[keyIndex]!!.getCharacter().code
									hookRequest.params = childComponent.onKeyType
									Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}
							}

							if (Client.cutsceneCameraCompleted && childComponent.onCamFinished != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.onCamFinished
								Client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}
						}

						if (childComponent.type == ComponentType.SPRITE && childComponent.skyboxId != -1) {
							childComponent.calculateLighting(TypeLists.SKYBOX!!, TypeLists.SUN!!)!!.updateForHeight(Renderers.activeToolkit!!, childComponent.height, PreferenceManager.preferences?.skyboxes?.value ?: 0)
						}

						Component.drawComponentObjSprite(childComponent)
						if (childComponent.type == ComponentType.CONTAINER) {
							processInterfaceCycle(parentInterface, childComponents, childComponent.idHash, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
							val itemSlots = childComponent.itemSlots
							if (itemSlots != null) {
								processInterfaceCycle(parentInterface, itemSlots, childComponent.idHash, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
							}

							val subInterface = Client.OPEN_INTERFACES!![childComponent.idHash.toLong()]
							if (subInterface != null) {
								if (Game.RUNESCAPE == Client.CURRENT_GAME && subInterface.overlay == 0 && !MiniMenu.isOpen && withinBoundaries && !Client.DISPLAY_HIDDEN_MENU_OPTIONS) {
									MiniMenu.resetEntries()
								}

								Interface.openSub(subInterface, subInterface.subInterfaceId, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX, componentY, mouseX, mouseY)
							}
						}
					}
				}
			}
		}
	}

	@JvmStatic
	fun resetPendingHooks() {
		// NodeCollection.first() and getNext() walk the same direction; getFirst() starts
		// the opposite walk, and pairing it with getNext() bounced between the first two
		// hooks, so hooks queued by components behind a no-click-through one survived.
		var hook = Client.PENDING_HOOK_REQUESTS.first() as HookRequest?
		while (hook != null) {
			if (hook.hasMousePosition) {
				hook.unlink()
				hook.source?.isHooked = false
			}
			hook = Client.PENDING_HOOK_REQUESTS.getNext() as HookRequest?
		}

		if (dragTickCount == 0) {
			Client.draggingComponent = null
			Client.selectedComponent = null
		}

		Client.menuItemCount = 0
		WorldMapDisplay.isInteractive = false
		CommandExecutor.teleportCommandRequested = false
	}

	@JvmStatic
	fun updateInterfaceDimensions(components: Array<Component?>, idHash: Int, width: Int, height: Int, fireResizeHooks: Boolean) {
		for (i in components.indices) {
			val component = components[i]
			if (component != null && component.parentId == idHash) {
				initSizes(component, width, height, fireResizeHooks)
				InterfaceComponentRenderer.componentPositionAdjustment(component, width, height)
				if (component.scrollX > component.scrollWidth - component.width) {
					component.scrollX = component.scrollWidth - component.width
				}
				if (component.scrollX < 0) {
					component.scrollX = 0
				}
				if (component.scrollY > component.scrollHeight - component.height) {
					component.scrollY = component.scrollHeight - component.height
				}
				if (component.scrollY < 0) {
					component.scrollY = 0
				}
				if (component.type == ComponentType.CONTAINER) {
					InterfaceComponentRenderer.updateEntityOnInterface(components, component, fireResizeHooks)
				}
			}
		}
	}

	@JvmStatic
	fun updateInterfaceDimensions(interfaceId: Int, width: Int, height: Int, fireResizeHooks: Boolean) {
		if (Interface.interfaceLoaded(interfaceId, null)) {
			updateInterfaceDimensions(Interface.INTERFACES!![interfaceId]!!.components, -1, width, height, fireResizeHooks)
		}
	}

	@JvmStatic
	fun initSizes(component: Component, parentWidth: Int, parentHeight: Int, fireResizeHooks: Boolean) {
		val width = component.width
		val height = component.height
		if (component.aspectWidthType.toInt() == 0) {
			component.width = component.baseWidth
		} else if (component.aspectWidthType.toInt() == 1) {
			component.width = parentWidth - component.baseWidth
		} else if (component.aspectWidthType.toInt() == 2) {
			component.width = component.baseWidth * parentWidth shr 14
		}
		if (component.aspectHeightType.toInt() == 0) {
			component.height = component.baseHeight
		} else if (component.aspectHeightType.toInt() == 1) {
			component.height = parentHeight - component.baseHeight
		} else if (component.aspectHeightType.toInt() == 2) {
			component.height = parentHeight * component.baseHeight shr 14
		}
		if (component.aspectWidthType.toInt() == 4) {
			component.width = component.aspectX * component.height / component.aspectY
		}
		if (component.aspectHeightType.toInt() == 4) {
			component.height = component.width * component.aspectY / component.aspectX
		}
		if (Client.DISPLAY_HIDDEN_MENU_OPTIONS && (Client.getComponentSettings(component)!!.settingsHash != 0 || component.type == ComponentType.CONTAINER)) {
			if (component.height < 5 && component.width < 5) {
				component.height = 5
				component.width = 5
			} else {
				if (component.height <= 0) {
					component.height = 5
				}
				if (component.width <= 0) {
					component.width = 5
				}
			}
		}
		if (component.contentType == ComponentContentType.GAME_WINDOW) {
			Client.GAME_SCREEN_INTERFACE = component
		}
		if (fireResizeHooks && component.onResize != null && (width != component.width || height != component.height)) {
			val resizeHook = HookRequest()
			resizeHook.source = component
			resizeHook.params = component.onResize
			Client.PENDING_HOOK_REQUESTS.addFirst(resizeHook)
		}
	}

	@JvmStatic
	fun startDragging(component: Component, offsetX: Int, offsetY: Int) {
		if (Client.draggingComponent == null && !MiniMenu.isOpen && component != null && Client.hasDepthFlags(component)) {
			Client.draggingComponent = component
			Client.selectedComponent = Component.getDepthChild(component)
			Client.mouseX = offsetX
			Client.mouseY = offsetY
			dragTickCount = 0
			Client.isDragThresholdMet = false
		}
	}

	@JvmStatic
	fun isComponentHidden(component: Component): Boolean {
		if (Client.DISPLAY_HIDDEN_MENU_OPTIONS) {
			if (Client.getComponentSettings(component)!!.settingsHash != 0) {
				return false
			}
			if (component.type == ComponentType.CONTAINER) {
				return false
			}
		}
		return component.hidden
	}

	@JvmStatic
	fun setUseOptionFlags(component: Component?, flags: Int, interfaceId: Int) {
		if (component != null) {
			if (component.onTargetEnter != null) {
				val targetEnterHook = HookRequest()
				targetEnterHook.source = component
				targetEnterHook.params = component.onTargetEnter
				CS2Executor.executeHookInner(targetEnterHook)
			}
			Client.interfaceSelectionActive = true
			Client.selectedComponentHash = component.idHash
			Client.selectedSlotId = component.slotId
			USE_OPTIONS_FLAGS = flags
			MiniMenu.USE_INTERFACE_ID = interfaceId
			Client.selectedItemId = component.containerItemId
			MiniMenu.useOnCursor = component.targetOverCursor
			Client.defaultMenuCursor = component.targetLeaveCursor
			Component.redrawComponent(component)
		}
	}

}
