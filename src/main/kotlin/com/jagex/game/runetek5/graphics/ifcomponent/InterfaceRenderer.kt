package com.jagex.game.runetek5.graphics.ifcomponent

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.Game
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.client
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
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentContentType
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.IFEvents
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.entity.pathingentity.EntityOverhead
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.environment.Environment
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.trigger.ClientTriggerType
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.TextUtils
import java.util.Arrays
import java.util.stream.Stream

object InterfaceRenderer {

	@JvmField
	var translatedUptext: String? = null

	@JvmField
	var anInt123: Int = 0

	@JvmField
	var anInt8308: Int = 0

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

				if (childComponent.type != ComponentType.CONTAINER && !childComponent.hasScript && client.getComponentSettings(childComponent).settingsHash == 0 && childComponent != client.selectedComponent && !childComponent.isIgnoredContentType()) {
					if (withinClipBounds) {
						Component.drawComponentObjSprite(childComponent)
					}
				} else if (!isComponentHidden(childComponent)) {
					if (childComponent == client.draggingComponent && client.hasDepthFlags(client.draggingComponent)) {
						client.isDraggingComponent = true
						client.dragStartX = componentX
						client.dragStartY = componentY
					}

					if (childComponent.hasInteraction || withinClipBounds) {
						if (childComponent.preventClickThrough && mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
							resetPendingHooks()
							if (!MiniMenu.isOpen) {
								MiniMenu.resetEntries()
							}
						}

						val shouldAddSprite = childComponent.clickMask && childComponent.type == ComponentType.SPRITE && childComponent.transparency == 0 && childComponent.skyboxId < 0 && childComponent.containerItemId == -1 && childComponent.anInt1435 == -1 && !childComponent.tiling && childComponent.angle2d == 0
						var withinBoundaries = false
						if (mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
							if (shouldAddSprite) {
								val clickableSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit!!)
								if (clickableSprite != null && clickableSprite.width == childComponent.width && childComponent.height == clickableSprite.height) {
									val mouseOffsetX = mouseX - componentX
									val mouseOffsetY = mouseY - componentY
									if (mouseOffsetY >= 0 && mouseOffsetY < clickableSprite.rowStartX.size) {
										val sizeX = clickableSprite.rowStartX[mouseOffsetY]
										if (mouseOffsetX >= sizeX && mouseOffsetX <= sizeX + clickableSprite.rowWidth[mouseOffsetY]) {
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

						if (!client.interfaceSelectionActive && withinBoundaries) {
							if (childComponent.moveOverCursor >= 0) {
								client.defaultMenuCursor = childComponent.moveOverCursor
							} else if (childComponent.preventClickThrough) {
								client.defaultMenuCursor = -1
							}
						}

						if (!MiniMenu.isOpen && withinBoundaries && !parentInterface.preventInteraction) {
							interfaceComponentHovered(childComponent)
						}

						val leftClick = MouseRecord.recorder?.leftButtonDown() == true && withinBoundaries
						var clickTriggered = false
						val mouseRecord = client.MOUSE_MOVEMENTS.getFirst() as? MouseRecord

						if (mouseRecord != null && mouseRecord.getClickType() == 0 && mouseRecord.getX() >= clipMinX && mouseRecord.getY() >= clipMinY && mouseRecord.getX() < clipMaxX && mouseRecord.getY() < clipMaxY) {
							if (shouldAddSprite) {
								val interactiveSprite = childComponent.addInteractiveSprite(Renderers.activeToolkit!!)
								if (interactiveSprite != null) {
									if (childComponent.width == interactiveSprite.width && childComponent.height == interactiveSprite.height) {
										val spriteRelativeX = mouseRecord.getX() - componentX
										val spriteRelativeY = mouseRecord.getY() - componentY
										if (spriteRelativeY >= 0 && spriteRelativeY < interactiveSprite.rowStartX.size) {
											val startX = interactiveSprite.rowStartX[spriteRelativeY]
											if (spriteRelativeX >= startX && spriteRelativeX <= startX + interactiveSprite.rowWidth[spriteRelativeY]) {
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
							for (keyIndex in childComponent.keyTriggeringAction!!.indices) {
								var keyTriggered = false
								var hasAction = false
								if (childComponent.anIntArray1267!![keyIndex] > 0) {
									for (i in 0 until client.keyCachePointerPoss) {
										if (childComponent.anIntArray1267!![keyIndex] == client.KEYS_PRESSED[i].getCharacter().code) {
											keyTriggered = true
											val cycles = childComponent.cycles
											if (cycles == null || cycles[keyIndex] <= client.cycles) {
												hasAction = true
											}
											break
										}
									}
								}

								if (!keyTriggered && childComponent.keyTriggeringAction!![keyIndex] != null) {
									for (key in childComponent.keyTriggeringAction!![keyIndex]!!.indices) {
										if (KeyRecorder.keyRecorder?.held(childComponent.keyTriggeringAction!![keyIndex]!![key].toInt()) == true) {
											keyTriggered = true
											val cycles = childComponent.cycles
											if (cycles != null && cycles[keyIndex] > client.cycles) {
												break
											}

											val clickFunctionMask = childComponent.keyTriggerParams!![keyIndex]!![key]
											if (clickFunctionMask.toInt() == 0 || (clickFunctionMask.toInt() and 0x8 == 0 || !(KeyRecorder.keyRecorder?.held(86) == true) && !(KeyRecorder.keyRecorder?.held(82) == true) && !(KeyRecorder.keyRecorder?.held(81) == true)) && (clickFunctionMask.toInt() and 0x2 == 0 || KeyRecorder.keyRecorder?.held(86) == true) && (clickFunctionMask.toInt() and 0x1 == 0 || KeyRecorder.keyRecorder?.held(82) == true) && (clickFunctionMask.toInt() and 0x4 == 0 || KeyRecorder.keyRecorder?.held(81) == true)) {
												hasAction = true
												break
											}
										}
									}
								}

								if (hasAction) {
									if (keyIndex < 10) {
										Interface.ifOptionInteract(keyIndex + 1, childComponent.idHash, childComponent.slotId, "")
									} else if (keyIndex == 10) {
										InterfaceComponentRenderer.cancelInterfaceSelection()
										val childEvents = client.getComponentSettings(childComponent)
										setUseOptionFlags(childComponent, childEvents.getUseOptionFlags(), childEvents.interfaceId)
										client.useOnType = Interface.getUseOptionString(childComponent)
										if (client.useOnType == null) {
											client.useOnType = "Null"
										}

										client.useOnTarget = childComponent.opBase + TextUtils.setTextColor(16777215)
									}

									val cycleProgress = childComponent.cycleSteps!![keyIndex]
									if (childComponent.cycles == null) {
										childComponent.cycles = IntArray(childComponent.keyTriggeringAction!!.size)
									}

									if (cycleProgress != 0) {
										childComponent.cycles!![keyIndex] = cycleProgress + client.cycles
									} else {
										childComponent.cycles!![keyIndex] = Int.MAX_VALUE
									}
								}

								val cycles = childComponent.cycles
								if (!keyTriggered && cycles != null) {
									cycles[keyIndex] = 0
								}
							}
						}

						if (clickTriggered) {
							method12221(childComponent, mouseRecord?.getX()?.minus(componentX) ?: 0, mouseRecord?.getY()?.minus(componentY) ?: 0)
						}

						if (client.draggingComponent != null && childComponent != client.draggingComponent && withinBoundaries && client.getComponentSettings(childComponent).dragEnabled()) {
							client.hoveredComponent = childComponent
						}

						if (childComponent == client.selectedComponent) {
							client.isComponentSelected = true
							client.selectedComponentX = componentX
							client.selectedComponentY = componentY
							client.selectedComponentWidth = client.selectedComponent.width
							client.selectedComponentHeight = client.selectedComponent.height
						}

						if (childComponent.hasScript || childComponent.contentType != null) {
							if (withinBoundaries && client.anInt7191 != 0 && childComponent.onMouseMove != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseY = client.anInt7191
								hookRequest.params = childComponent.onMouseMove
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							var clickTriggeredVar = clickTriggered
							var leftClickVar = leftClick
							if (client.draggingComponent != null) {
								clickTriggeredVar = false
								leftClickVar = false
							} else if (MiniMenu.isOpen || childComponent.contentType != ComponentContentType.WORLDMAP && client.menuItemCount > 0) {
								clickTriggeredVar = false
								leftClickVar = false
								withinBoundaries = false
							}

							if (childComponent.contentType != null) {
								if (childComponent.contentType == ComponentContentType.GAME_WINDOW || childComponent.contentType == ComponentContentType.CONTENT_TYPE_1403) {
									client.GAME_SCREEN_INTERFACE = childComponent
									val environment = Resource.mapRegion?.environmentManager?.environment
									if (environment?.skybox != null && Resource.mapRegionLoaderTask?.isLoadingRegion() != true) {
										environment.skybox?.method4217(Renderers.activeToolkit!!, childComponent.height, PreferenceManager.preferences?.skyboxes?.value ?: 0)
									}

									if (childComponent.contentType == ComponentContentType.GAME_WINDOW && !MiniMenu.isOpen && mouseX >= clipMinX && mouseY >= clipMinY && mouseX < clipMaxX && mouseY < clipMaxY) {
										tileHovered(Renderers.activeToolkit!!, mouseX, mouseY)

										var overhead = client.ENTITY_OVERHEADS.getFirst() as EntityOverhead?
										while (overhead != null) {
											if (mouseX >= overhead.startX && mouseX < overhead.endX && mouseY >= overhead.startY && mouseY < overhead.endY) {
												MiniMenu.resetEntries()
												PathingEntity.interfaceComponentOnEntity(overhead.pathingEntity)
											}
											overhead = client.ENTITY_OVERHEADS.getNext() as EntityOverhead?
										}
									}

									val playerCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES
									val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES

									for (i in 0 until playerCount) {
										val player = PlayerUpdate.players[playerIndices[i]]
										if (player != null) {
											CS2Executor.executeIfTrigger(ClientTriggerType.IF_PLAYER_CYCLE, -1, player, playerIndices[i])
											player.method15880(clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
										}
									}

									for (i in 0 until client.NPC_UPDATE_INDEX) {
										val npcIndex = client.NPC_UPDATE_INDICES[i]
										val npcNode = client.NPC_MAP[npcIndex.toLong()]
										if (npcNode != null) {
											val npcEntity = npcNode.value as NpcEntity
											CS2Executor.executeIfTrigger(ClientTriggerType.IF_NPC_CYCLE, npcEntity.definitions?.type ?: 0, npcNode.value as PathingEntity, npcIndex)
											(npcNode.value as PathingEntity).method15880(clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
										}
									}
									continue
								}

								if (childComponent.contentType == ComponentContentType.MINIMAP) {
									val spriteVar = childComponent.addInteractiveSprite(Renderers.activeToolkit!!)
									if (spriteVar == null || ComponentMinimap.minimapState != 0 && ComponentMinimap.minimapState != 3 || MiniMenu.isOpen || mouseX < clipMinX || mouseY < clipMinY || mouseX >= clipMaxX || mouseY >= clipMaxY) {
										continue
									}

									var relativeX = mouseX - componentX
									var relativeY = mouseY - componentY
									val rowStartX = spriteVar.rowStartX[relativeY]
									if (relativeX < rowStartX || relativeX > rowStartX + spriteVar.rowWidth[relativeY]) {
										continue
									}

									relativeX -= childComponent.width / 2
									relativeY -= childComponent.height / 2
									val angleY: Int
									if (Camera.cameraCalculationType == 4) {
										angleY = Camera.camAngleY.toInt() and 0x3fff
									} else {
										angleY = (Camera.camAngleY.toInt() + client.minimapRotation) and 0x3fff
									}

									var sinAngleY = MathUtils.SINE[angleY]
									var cosAngleY = MathUtils.COSINE[angleY]
									if (Camera.cameraCalculationType != 4) {
										sinAngleY = (client.minimapZoom + 256) * sinAngleY shr 8
										cosAngleY = cosAngleY * (client.minimapZoom + 256) shr 8
									}

									val worldX = sinAngleY * relativeY + relativeX * cosAngleY shr 14
									val worldY = relativeY * cosAngleY - sinAngleY * relativeX shr 14
									val mapX: Int
									val mapY: Int
									if (Camera.cameraCalculationType == 4) {
										mapX = (worldX shr 2) + (client.anInt7262 shr 9)
										mapY = (client.anInt7376 shr 9) - (worldY shr 2)
									} else {
										val playerSizeOffset = (PlayerUpdate.MY_PLAYER.size - 1) * 256
										val playerPosition = PlayerUpdate.MY_PLAYER.getAbsolutePosition().coords
										mapX = (playerPosition.x.toInt() - playerSizeOffset shr 9) + (worldX shr 2)
										mapY = (playerPosition.z.toInt() - playerSizeOffset shr 9) - (worldY shr 2)
									}

									if (client.interfaceSelectionActive && USE_OPTIONS_FLAGS and 0x40 != 0) {
										val interfaceComponent = Component.listIcomponent(client.anInt56, client.anInt7345)
										if (interfaceComponent != null) {
											MiniMenu.addEntry(client.useOnType, " ->", MiniMenu.useOnCursor, 59, childComponent.containerItemId, 1, mapX, mapY, true, false, (childComponent.slotId or childComponent.idHash).toLong(), true)
										} else {
											InterfaceComponentRenderer.cancelInterfaceSelection()
										}
										continue
									}

									if (Game.STELLAR_DAWN == client.CURRENT_GAME) {
										MiniMenu.addEntry(LocalizedText.FACE_HERE.translate(client.CURRENT_LANGUAGE), "", -1, 60, -1, 1, mapX, mapY, true, false, 0, true)
									}

                                    translatedUptext?.let { MiniMenu.addEntry(it, "", CursorManager.activeCursorId, 23, -1, 1, mapX, mapY, true, false, 0, true) }
									continue
								}

								if (childComponent.contentType == ComponentContentType.WORLDMAP) {
									WorldMap.worldmapComponent = childComponent
									if (withinBoundaries) {
										WorldMapSub1.isInteractive = true
									}

									if (clickTriggeredVar) {
										val clickX = ((mouseRecord?.getX()?.minus(componentX)?.minus(childComponent.width / 2))?.times(2.0)?.div(WorldMap.mapZoom))?.toInt()
										val clickY = mouseRecord?.getY()?.minus(componentY)?.minus(childComponent.height / 2)?.let { (-(it) * 2.0 / WorldMap.mapZoom).toInt() }
										val absX = clickX?.plus(WorldMap.mapSquareAbsX)?.plus(WorldMapSub1.anInt5123) ?: 0
										val absY = clickY?.plus(WorldMap.mapSquareAbsY)?.plus(WorldMapSub1.anInt5624) ?: 0
										val worldMapElement = WorldMapSub1.getStaticElement()
										if (worldMapElement == null)
											continue

										val worldPosition = IntArray(3)
										worldMapElement.method14775(absX, absY, worldPosition)
										if (worldPosition != null) {
											if (KeyRecorder.keyRecorder?.held(82) == true && client.PLAYER_RIGHTS > 0) {
												CommandExecutor.executeTeleportCommand(worldPosition[0], worldPosition[1], worldPosition[2])
												continue
											}

											CommandExecutor.teleportCommandRequested = true
											CommandExecutor.teleportCommandPositionZ = worldPosition[0]
											CommandExecutor.teleportCommandPositionX = worldPosition[1]
											CommandExecutor.teleportCommandPositionY = worldPosition[2]
										}

										client.menuItemCount = 1
										client.isMapMoving = false
										client.mouseX = MouseRecord.recorder?.getMouseX() ?: 0
										client.mouseY = MouseRecord.recorder?.getMouseY() ?: 0
										continue
									}

									if (leftClickVar && client.menuItemCount > 0) {
										if (client.menuItemCount == 1 && (client.mouseX != MouseRecord.recorder?.getMouseX() ?: 0 || client.mouseY != MouseRecord.recorder?.getMouseY() ?: 0)) {
											Interface.anInt3008 = WorldMapSub1.anInt5123
											anInt123 = WorldMapSub1.anInt5624
											client.menuItemCount = 2
										}

										if (client.menuItemCount == 2) {
											client.isMapMoving = true
											WorldMapSub1.method1503(Interface.anInt3008 + ((client.mouseX - (MouseRecord.recorder?.getMouseX() ?: 0)) * 2.0 / WorldMap.requestedMapZoom).toInt())
											WorldMapSub1.method5599(anInt123 - ((client.mouseY - (MouseRecord.recorder?.getMouseY() ?: 0)) * 2.0 / WorldMap.requestedMapZoom).toInt())
										}
										continue
									}

									if (client.menuItemCount > 0 && !client.isMapMoving) {
										if ((client.anInt7339 == 1 || MiniMenu.method12562()) && MiniMenu.entryCount > 2) {
											StaticMethods.method4164(client.mouseX, client.mouseY)
										} else if (MiniMenu.hasEntries()) {
											StaticMethods.method4164(client.mouseX, client.mouseY)
										}
									}

									client.menuItemCount = 0
									continue
								}

								if (childComponent.contentType == ComponentContentType.WORLD_MAP_OVERVIEW) {
									if (leftClickVar) {
										WorldMapSub1.markDestination(MouseRecord.recorder?.getMouseX() ?: 0 - componentX, MouseRecord.recorder?.getMouseY() ?: 0 - componentY, childComponent.width, childComponent.height)
									}
									continue
								}

								if (childComponent.contentType == ComponentContentType.CONTENT_TYPE_1406) {
									InterfaceComponentRenderer.method544(childComponent, componentX, componentY)
									continue
								}
							}

							if (!childComponent.isPressed && clickTriggeredVar) {
								childComponent.isPressed = true
								if (childComponent.onClick != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = mouseRecord?.getX()?.minus(componentX) ?: 0
									hookRequest.mouseY = mouseRecord?.getY()?.minus(componentY) ?: 0
									hookRequest.params = childComponent.onClick
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}
							}

							if (childComponent.isPressed && leftClickVar && childComponent.anObjectArray1319 != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseX = MouseRecord.recorder?.getMouseX() ?: 0 - componentX
								hookRequest.mouseY = MouseRecord.recorder?.getMouseY() ?: 0 - componentY
								hookRequest.params = childComponent.anObjectArray1319
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (childComponent.isPressed && !leftClickVar) {
								childComponent.isPressed = false
								if (childComponent.onRelease != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = MouseRecord.recorder?.getMouseX() ?: 0 - componentX
									hookRequest.mouseY = MouseRecord.recorder?.getMouseY() ?: 0 - componentY
									hookRequest.params = childComponent.onRelease
									client.aClass4827404.addFirst(hookRequest)
								}
							}

							if (leftClickVar && childComponent.onHold != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseX = MouseRecord.recorder?.getMouseX() ?: 0 - componentX
								hookRequest.mouseY = MouseRecord.recorder?.getMouseY() ?: 0 - componentY
								hookRequest.params = childComponent.onHold
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (!childComponent.isHooked && withinBoundaries) {
								childComponent.isHooked = true
								if (childComponent.onMouseOver != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = MouseRecord.recorder?.getMouseX() ?: 0 - componentX
									hookRequest.mouseY = MouseRecord.recorder?.getMouseY() ?: 0 - componentY
									hookRequest.params = childComponent.onMouseOver
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}
							}

							if (childComponent.isHooked && withinBoundaries && childComponent.popupScript != null) {
								val hookRequest = HookRequest()
								hookRequest.hasMousePosition = true
								hookRequest.source = childComponent
								hookRequest.mouseX = MouseRecord.recorder?.getMouseX() ?: 0 - componentX
								hookRequest.mouseY = MouseRecord.recorder?.getMouseY() ?: 0 - componentY
								hookRequest.params = childComponent.popupScript
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (childComponent.isHooked && !withinBoundaries) {
								childComponent.isHooked = false
								if (childComponent.onMouseLeaveScript != null) {
									val hookRequest = HookRequest()
									hookRequest.hasMousePosition = true
									hookRequest.source = childComponent
									hookRequest.mouseX = MouseRecord.recorder?.getMouseX() ?: 0 - componentX
									hookRequest.mouseY = MouseRecord.recorder?.getMouseY() ?: 0 - componentY
									hookRequest.params = childComponent.onMouseLeaveScript
									client.aClass4827404.addFirst(hookRequest)
								}
							}

							if (childComponent.anObjectArray1409 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1409
								client.aClass4827233.addFirst(hookRequest)
							}

							var hookrequest54: HookRequest
							if (childComponent.onKey != null && client.keyCycle > childComponent.lastKeyCycle) {
								val keyPressArray = childComponent.keyPressArray
								if (keyPressArray != null && client.keyCycle - childComponent.lastKeyCycle <= 32) {
									label969@ for (keyIndex in childComponent.lastKeyCycle until client.keyCycle) {
										val i_25 = client.anIntArray7150[keyIndex and 0x1f]

										for (i_26 in keyPressArray.indices) {
											if (i_25 == keyPressArray[i_26]) {
												hookrequest54 = HookRequest()
												hookrequest54.source = childComponent
												hookrequest54.params = childComponent.onKey
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54)
												break@label969
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.onKey
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastKeyCycle = client.keyCycle
							}

							if (childComponent.onScrollWheel != null && client.mouseWheelCycle > childComponent.lastMouseWheelCycle) {
								if (childComponent.mouseWheelArray != null && client.mouseWheelCycle - childComponent.lastMouseWheelCycle <= 32) {
									label945@ for (keyIndex in childComponent.lastMouseWheelCycle until client.mouseWheelCycle) {
										val i_25 = client.recentMouseWheels[keyIndex and 0x1f]

										for (i_26 in childComponent.mouseWheelArray!!.indices) {
											if (i_25 == childComponent.mouseWheelArray!![i_26]) {
												hookrequest54 = HookRequest()
												hookrequest54.source = childComponent
												hookrequest54.params = childComponent.onScrollWheel
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54)
												break@label945
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.onScrollWheel
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.lastMouseWheelCycle = client.mouseWheelCycle
							}

							if (childComponent.anObjectArray1397 != null && client.pulseEventConfigsPtr > childComponent.anInt1443) {
								if (childComponent.varps != null && client.pulseEventConfigsPtr - childComponent.anInt1443 <= 32) {
									label921@ for (keyIndex in childComponent.anInt1443 until client.pulseEventConfigsPtr) {
										val i_25 = client.PULSE_EVENT_CONFIGS_CACHE[keyIndex and 0x1f]

										for (i_26 in childComponent.varps!!.indices) {
											if (i_25 == childComponent.varps!![i_26]) {
												hookrequest54 = HookRequest()
												hookrequest54.source = childComponent
												hookrequest54.params = childComponent.anObjectArray1397
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54)
												break@label921
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.anObjectArray1397
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.anInt1443 = client.pulseEventConfigsPtr
							}

							if (childComponent.mouseLeaveScript != null && client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS > childComponent.anInt1444) {
								if (childComponent.mouseLeaveArrayParams != null && client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS - childComponent.anInt1444 <= 32) {
									label897@ for (keyIndex in childComponent.anInt1444 until client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS) {
										val i_25 = client.RECENTLY_USED_ITEM_CONTAINERS[keyIndex and 0x1f]

										for (i_26 in childComponent.mouseLeaveArrayParams!!.indices) {
											if (i_25 == childComponent.mouseLeaveArrayParams!![i_26]) {
												hookrequest54 = HookRequest()
												hookrequest54.source = childComponent
												hookrequest54.params = childComponent.mouseLeaveScript
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54)
												break@label897
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.mouseLeaveScript
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.anInt1444 = client.RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS
							}

							if (childComponent.anObjectArray1387 != null && client.anInt7384 > childComponent.anInt1445) {
								if (childComponent.anIntArray1402 != null && client.anInt7384 - childComponent.anInt1445 <= 32) {
									label873@ for (keyIndex in childComponent.anInt1445 until client.anInt7384) {
										val i_25 = client.anIntArray7283[keyIndex and 0x1f]

										for (i_26 in childComponent.anIntArray1402!!.indices) {
											if (i_25 == childComponent.anIntArray1402!![i_26]) {
												hookrequest54 = HookRequest()
												hookrequest54.source = childComponent
												hookrequest54.params = childComponent.anObjectArray1387
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54)
												break@label873
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.anObjectArray1387
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.anInt1445 = client.anInt7384
							}

							if (childComponent.anObjectArray1407 != null && client.CLAN_VAR_COUNTER > childComponent.anInt1448) {
								if (childComponent.anIntArray1408 != null && client.CLAN_VAR_COUNTER - childComponent.anInt1448 <= 32) {
									label849@ for (keyIndex in childComponent.anInt1448 until client.CLAN_VAR_COUNTER) {
										val i_25 = client.CLAN_VAR_KEYS[keyIndex and 0x1f]

										for (i_26 in childComponent.anIntArray1408!!.indices) {
											if (i_25 == childComponent.anIntArray1408!![i_26]) {
												hookrequest54 = HookRequest()
												hookrequest54.source = childComponent
												hookrequest54.params = childComponent.anObjectArray1407
												client.PENDING_HOOK_REQUESTS.addFirst(hookrequest54)
												break@label849
											}
										}
									}
								} else {
									val hookRequest = HookRequest()
									hookRequest.source = childComponent
									hookRequest.params = childComponent.anObjectArray1407
									client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
								}

								childComponent.anInt1448 = client.CLAN_VAR_COUNTER
							}

							if (client.anInt7391 > childComponent.anInt1442 && childComponent.anObjectArray1413 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1413
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (client.anInt7386 > childComponent.anInt1442 && childComponent.anObjectArray1415 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1415
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (client.anInt7179 > childComponent.anInt1442 && childComponent.anObjectArray1416 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1416
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (client.anInt7192 > childComponent.anInt1442 && childComponent.anObjectArray1383 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1383
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (client.anInt7395 > childComponent.anInt1442 && childComponent.anObjectArray1418 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1418
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (client.anInt7320 > childComponent.anInt1442 && childComponent.anObjectArray1361 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1361
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							if (client.anInt7397 > childComponent.anInt1442 && childComponent.anObjectArray1419 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1419
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}

							childComponent.anInt1442 = client.anInt7347
							if (childComponent.anObjectArray1292 != null) {
								for (keyIndex in 0 until client.keyCachePointerPoss) {
									val hookrequest44 = HookRequest()
									hookrequest44.source = childComponent
									hookrequest44.typedKeyCode = client.KEYS_PRESSED[keyIndex].getCode()
									hookrequest44.typedKeyChar = client.KEYS_PRESSED[keyIndex].getCharacter().code
									hookrequest44.params = childComponent.anObjectArray1292
									client.PENDING_HOOK_REQUESTS.addFirst(hookrequest44)
								}
							}

							if (client.aBool7355 && childComponent.anObjectArray1353 != null) {
								val hookRequest = HookRequest()
								hookRequest.source = childComponent
								hookRequest.params = childComponent.anObjectArray1353
								client.PENDING_HOOK_REQUESTS.addFirst(hookRequest)
							}
						}

						if (childComponent.type == ComponentType.SPRITE && childComponent.skyboxId != -1) {
							childComponent.calculateLighting(TypeLists.SKYBOX!!, TypeLists.SUN!!)!!.method4217(Renderers.activeToolkit!!, childComponent.height, PreferenceManager.preferences?.skyboxes?.value ?: 0)
						}

						Component.drawComponentObjSprite(childComponent)
						if (childComponent.type == ComponentType.CONTAINER) {
							processInterfaceCycle(parentInterface, childComponents, childComponent.idHash, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
							val itemSlots = childComponent.itemSlots
							if (itemSlots != null) {
								processInterfaceCycle(parentInterface, itemSlots, childComponent.idHash, clipMinX, clipMinY, clipMaxX, clipMaxY, componentX - childComponent.scrollX, componentY - childComponent.scrollY, mouseX, mouseY)
							}

							val subInterface = client.OPEN_INTERFACES[childComponent.idHash.toLong()]
							if (subInterface != null) {
								if (Game.RUNESCAPE == client.CURRENT_GAME && subInterface.overlay == 0 && !MiniMenu.isOpen && withinBoundaries && !client.DISPLAY_HIDDEN_MENU_OPTIONS) {
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
		var hook = client.PENDING_HOOK_REQUESTS.getFirst() as HookRequest?
		while (hook != null) {
			if (hook.hasMousePosition) {
				hook.unlink()
				hook.source?.isHooked = false
			}
			hook = client.PENDING_HOOK_REQUESTS.getNext() as HookRequest?
		}

		if (anInt8308 == 0) {
			client.draggingComponent = null
			client.selectedComponent = null
		}

		client.menuItemCount = 0
		WorldMapSub1.isInteractive = false
		CommandExecutor.teleportCommandRequested = false
	}

	@JvmStatic
	fun updateInterfaceDimensions(itemSlots: Array<Component?>, idHash: Int, width: Int, height: Int, bool4: Boolean) {
		for (i_6 in itemSlots.indices) {
			val component = itemSlots[i_6]
			if (component != null && component.parentId == idHash) {
				initSizes(component, width, height, bool4)
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
					InterfaceComponentRenderer.updateEntityOnInterface(itemSlots, component, bool4)
				}
			}
		}
	}

	@JvmStatic
	fun updateInterfaceDimensions(interfaceId: Int, width: Int, height: Int, bool3: Boolean) {
		if (Interface.interfaceLoaded(interfaceId, null)) {
			updateInterfaceDimensions(Interface.INTERFACES!![interfaceId]!!.components, -1, width, height, bool3)
		}
	}

	@JvmStatic
	fun initSizes(interfaceComponent: Component, i_1: Int, i_2: Int, hookRequest: Boolean) {
		val width = interfaceComponent.width
		val height = interfaceComponent.height
		if (interfaceComponent.aspectWidthType.toInt() == 0) {
			interfaceComponent.width = interfaceComponent.baseWidth
		} else if (interfaceComponent.aspectWidthType.toInt() == 1) {
			interfaceComponent.width = i_1 - interfaceComponent.baseWidth
		} else if (interfaceComponent.aspectWidthType.toInt() == 2) {
			interfaceComponent.width = interfaceComponent.baseWidth * i_1 shr 14
		}
		if (interfaceComponent.aspectHeightType.toInt() == 0) {
			interfaceComponent.height = interfaceComponent.baseHeight
		} else if (interfaceComponent.aspectHeightType.toInt() == 1) {
			interfaceComponent.height = i_2 - interfaceComponent.baseHeight
		} else if (interfaceComponent.aspectHeightType.toInt() == 2) {
			interfaceComponent.height = i_2 * interfaceComponent.baseHeight shr 14
		}
		if (interfaceComponent.aspectWidthType.toInt() == 4) {
			interfaceComponent.width = interfaceComponent.aspectX * interfaceComponent.height / interfaceComponent.aspectY
		}
		if (interfaceComponent.aspectHeightType.toInt() == 4) {
			interfaceComponent.height = interfaceComponent.width * interfaceComponent.aspectY / interfaceComponent.aspectX
		}
		if (client.DISPLAY_HIDDEN_MENU_OPTIONS && (client.getComponentSettings(interfaceComponent).settingsHash != 0 || interfaceComponent.type == ComponentType.CONTAINER)) {
			if (interfaceComponent.height < 5 && interfaceComponent.width < 5) {
				interfaceComponent.height = 5
				interfaceComponent.width = 5
			} else {
				if (interfaceComponent.height <= 0) {
					interfaceComponent.height = 5
				}
				if (interfaceComponent.width <= 0) {
					interfaceComponent.width = 5
				}
			}
		}
		if (interfaceComponent.contentType == ComponentContentType.GAME_WINDOW) {
			client.GAME_SCREEN_INTERFACE = interfaceComponent
		}
		if (hookRequest && interfaceComponent.anObjectArray1271 != null && (width != interfaceComponent.width || height != interfaceComponent.height)) {
			val hookrequest7 = HookRequest()
			hookrequest7.source = interfaceComponent
			hookrequest7.params = interfaceComponent.anObjectArray1271
			client.PENDING_HOOK_REQUESTS.addFirst(hookrequest7)
		}
	}

	@JvmStatic
	fun method12221(component: Component, offsetX: Int, offsetY: Int) {
		if (client.draggingComponent == null && !MiniMenu.isOpen && component != null && client.hasDepthFlags(component)) {
			client.draggingComponent = component
			client.selectedComponent = Component.getDepthChild(component)
			client.mouseX = offsetX
			client.mouseY = offsetY
			anInt8308 = 0
			client.aBool7364 = false
		}
	}

	@JvmStatic
	fun isComponentHidden(component: Component): Boolean {
		if (client.DISPLAY_HIDDEN_MENU_OPTIONS) {
			if (client.getComponentSettings(component).settingsHash != 0) {
				return false
			}
			if (component.type == ComponentType.CONTAINER) {
				return false
			}
		}
		return component.hidden
	}

	@JvmStatic
	fun setUseOptionFlags(componentDefs: Component?, flags: Int, interfaceId: Int) {
		if (componentDefs != null) {
			if (componentDefs.anObjectArray1400 != null) {
				val hookrequest4 = HookRequest()
				hookrequest4.source = componentDefs
				hookrequest4.params = componentDefs.anObjectArray1400
				CS2Executor.executeHookInner(hookrequest4)
			}
			client.interfaceSelectionActive = true
			client.anInt56 = componentDefs.idHash
			client.anInt7345 = componentDefs.slotId
			com.jagex.game.runetek5.client.menu.USE_OPTIONS_FLAGS = flags
			MiniMenu.USE_INTERFACE_ID = interfaceId
			client.anInt7346 = componentDefs.containerItemId
			MiniMenu.useOnCursor = componentDefs.targetOverCursor
			client.defaultMenuCursor = componentDefs.targetLeaveCursor
			Component.redrawComponent(componentDefs)
		}
	}

}
