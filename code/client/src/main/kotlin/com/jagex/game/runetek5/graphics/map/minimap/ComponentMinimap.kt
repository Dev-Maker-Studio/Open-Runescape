package com.jagex.game.runetek5.graphics.map.minimap

import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectShapes
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.`object`.ObjectTypeList
import com.jagex.game.runetek5.config.mapsprite.MapSpriteType
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.entity.gameobject.GameObject
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.framebuffer.RenderBuffer
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.hint.HintRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.modern.debug.MinimapDump
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapRasterManager
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.imageproducer.DirectDisplay
import com.jagex.game.runetek5.interaction.Friend
import com.jagex.game.runetek5.interaction.FriendList
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.CollisionFlag
import com.jagex.game.runetek5.scene.CollisionMap
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.scene.SettingsBits
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.TimeTools
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarProviders

object ComponentMinimap {
    @JvmField
    var minimapContainsFlag = true
    @JvmField
    var minimapFlagX = -1
    @JvmField
    var minimapFlagY = -1

    @JvmField
    val minimapLocSceneX = IntArray(1014)
    @JvmField
    val minimapLocSceneY = IntArray(1001)
    @JvmField
    val minimapLocId = IntArray(1010)
    @JvmField
    var minimapLocCount = 0
    @JvmField
    var minimapBackground: NativeSprite? = null
    @JvmField
    var MAP_PLANE = -1
    @JvmField
    var minimapState = 0
    @JvmField
    var mapAssetLoadingPercentage = 0

    @JvmStatic
    fun resetMinimapFlagAndPlane() {
        minimapState = 0
        MAP_PLANE = -1
        minimapFlagX = -1
        minimapFlagY = -1
    }

    @JvmStatic
    fun resetMinimapFlag() {
        minimapState = 0
        minimapFlagX = -1
        minimapFlagY = -1
    }

    @JvmStatic
    fun drawLocOnMinimap(toolkit: AbstractToolkit, plane: Int, sceneX: Int, sceneY: Int, x: Int, y: Int, colorNoActions: Int, colorHasActions: Int) {
        val scene: Scene = Resource.mapRegion!!.getScene()!!
        var sceneObject: GameObject? = scene.getWall(plane, sceneX, sceneY) as? GameObject
        if (sceneObject != null) {
            val locType: ObjectType = Resource.mapRegion!!.getLoctypeList().list(sceneObject.id)
            val rotation = sceneObject.getRotation() and 0x3
            val shape = sceneObject.shape
            if (locType.mapIconId != -1) {
                drawMapIcon(toolkit, locType, rotation, x, y)
            } else {
                var color = colorNoActions
                if (locType.hasActions > 0) {
                    color = colorHasActions
                }
                if (shape == ObjectShapes.WALL_STRAIGHT.shape || shape == ObjectShapes.WALL_WHOLE_CORNER.shape) {
                    if (rotation == 0) {
                        toolkit.drawOpaqueVerticalLine(x, y, 4, color)
                    } else if (rotation == 1) {
                        toolkit.drawOpaqueHorizontalLine(x, y, 4, color)
                    } else if (rotation == 2) {
                        toolkit.drawOpaqueVerticalLine(x + 3, y, 4, color)
                    } else {
                        toolkit.drawOpaqueHorizontalLine(x, y + 3, 4, color)
                    }
                }
                if (shape == ObjectShapes.WALL_STRAIGHT_CORNER.shape) {
                    if (rotation == 0) {
                        toolkit.clearPixels(x, y, 1, 1, color)
                    } else if (rotation == 1) {
                        toolkit.clearPixels(x + 3, y, 1, 1, color)
                    } else if (rotation == 2) {
                        toolkit.clearPixels(x + 3, y + 3, 1, 1, color)
                    } else {
                        toolkit.clearPixels(x, y + 3, 1, 1, color)
                    }
                }
                if (shape == ObjectShapes.WALL_WHOLE_CORNER.shape) {
                    if (rotation == 0) {
                        toolkit.drawOpaqueHorizontalLine(x, y, 4, color)
                    } else if (rotation == 1) {
                        toolkit.drawOpaqueVerticalLine(x + 3, y, 4, color)
                    } else if (rotation == 2) {
                        toolkit.drawOpaqueHorizontalLine(x, y + 3, 4, color)
                    } else {
                        toolkit.drawOpaqueVerticalLine(x, y, 4, color)
                    }
                }
            }
        }
        sceneObject = scene.getInteractiveObject(plane, sceneX, sceneY, Client.INTERACTIVE_OBJECT_COMPARABLE) as? GameObject
        if (sceneObject != null) {
            val loctype: ObjectType = Resource.mapRegion!!.getLoctypeList().list(sceneObject.id)
            val rotation = sceneObject.getRotation() and 0x3
            val type = sceneObject.shape
            if (loctype.mapIconId != -1) {
                drawMapIcon(toolkit, loctype, rotation, x, y)
            } else if (type == ObjectShapes.WALL_INTERACT.shape) {
                var color = -1118482
                if (loctype.hasActions > 0) {
                    color = -1179648
                }
                if (rotation != 0 && rotation != 2) {
                    toolkit.drawOpaqueLine(x, y, x + 3, y + 3, color)
                } else {
                    toolkit.drawOpaqueLine(x, y + 3, x + 3, y, color)
                }
            }
        }
        sceneObject = scene.getGroundDecoration(plane, sceneX, sceneY) as? GameObject
        if (sceneObject != null) {
            val locType: ObjectType = Resource.mapRegion!!.getLoctypeList().list(sceneObject.id)
            val rotation = sceneObject.getRotation() and 0x3
            if (locType.mapIconId != -1) {
                drawMapIcon(toolkit, locType, rotation, x, y)
            }
        }
    }

    @JvmStatic
    fun drawMinimap(toolkit: AbstractToolkit, component: Component, clipLeft: Int, clipTop: Int) {
        val sprite: InteractiveComponentSprite? = component.addInteractiveSprite(toolkit)
        if (sprite != null) {
            val dimension: ComponentSurface = sprite.surface
            toolkit.setClipping(clipLeft, clipTop, clipLeft + component.width, clipTop + component.height)
            if (sprite.width != component.width || sprite.height != component.height) {
                throw IllegalStateException("")
            }

            if (minimapState != 2 && minimapState != 5 && minimapBackground != null) {
                val baseCoordinates: CoordGrid = Resource.mapRegion!!.getBaseCoordinates()
                val absX: Int
                val absY: Int
                val rotation: Int
                val zoom: Int
                if (Camera.cameraCalculationType == 4) {
                    absX = Client.cameraTargetAbsX
                    absY = Client.cameraTargetAbsY
                    rotation = (-Camera.camAngleY).toInt() and 0x3fff
                    zoom = 4096
                } else {
                    val playerCoords: Vector3 = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                    absX = playerCoords.x.toInt()
                    absY = playerCoords.z.toInt()
                    rotation = (-Camera.camAngleY).toInt() + Client.minimapRotation and 0x3fff
                    zoom = 4096 - Client.minimapZoom * 16
                }

                val pivotX = absX / 128 + 48
                val pivotY = 48 + Resource.mapRegion!!.sceneSizeY * 4 - absY / 128
                minimapBackground!!.rotate(clipLeft + component.width / 2.0F, clipTop + component.height / 2.0F, pivotX.toFloat(), pivotY.toFloat(), zoom, rotation shl 2, dimension, clipLeft, clipTop)
                val staticElements: StaticElements = Resource.mapRegion!!.getStaticElements()

                var staticElementNode: IntNode? = MapRegion.staticElementsAwaitingDrawing.getFirst() as? IntNode
                while (staticElementNode != null) {
                    val index = staticElementNode.value
                    val sceneX = (staticElements.bitpackedPlacement[index] shr 14 and 0x3fff) - baseCoordinates.x
                    val sceneY = (staticElements.bitpackedPlacement[index] and 0x3fff) - baseCoordinates.y
                    val offsetX = sceneX * 4 + 2 - absX / 128
                    val offsetY = sceneY * 4 + 2 - absY / 128
                    drawMec(toolkit, dimension, component, clipLeft, clipTop, offsetX, offsetY, staticElements.mapCategoryIds[index])
                    staticElementNode = MapRegion.staticElementsAwaitingDrawing.next() as? IntNode
                }

                for (index in 0 until minimapLocCount) {
                    val offsetX = minimapLocSceneX[index] * 4 + 2 - absX / 128
                    val offsetY = minimapLocSceneY[index] * 4 + 2 - absY / 128
                    var locType: ObjectType = Resource.mapRegion!!.getLoctypeList().list(minimapLocId[index])
                    if (locType.alternativeIds != null) {
                        locType = locType.getMultiLoc(VarProviders.PLAYER_VAR_PROVIDER) ?: continue
                        if (locType.mapCategoryid == -1) {
                            continue
                        }
                    }

                    drawMec(toolkit, dimension, component, clipLeft, clipTop, offsetX, offsetY, locType.mapCategoryid)
                }

                var groundItem: GroundItemWrapper? = Client.GROUND_ITEMS.first() as? GroundItemWrapper
                while (groundItem != null) {
                    val plane = (groundItem.pointer shr 28 and 0x3L).toInt()
                    if (plane == MAP_PLANE) {
                        val sceneX = (groundItem.pointer and 0x3fffL).toInt() - baseCoordinates.x
                        val sceneY = (groundItem.pointer shr 14 and 0x3fffL).toInt() - baseCoordinates.y
                        val sizeX = sceneX * 4 + 2 - absX / 128
                        val sizeZ = sceneY * 4 + 2 - absY / 128
                        drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(0))
                    }
                    groundItem = Client.GROUND_ITEMS.next() as? GroundItemWrapper
                }

                drawNpcs(toolkit, absX, absY, component, dimension, clipLeft, clipTop)
                drawPlayers(absX, absY, component, dimension, clipLeft, clipTop)
                HintRenderer.drawMinimapHintArrows(absX, absY, component, sprite, clipLeft, clipTop)
                if (Camera.cameraCalculationType != 4) {
                    if (minimapFlagX != 0) {
                        val sizeX = minimapFlagX * 4 + 2 - absX / 128 + (PlayerUpdate.MY_PLAYER!!.getSize() - 1) * 2
                        val sizeY = 2 + minimapFlagY * 4 - absY / 128 + (PlayerUpdate.MY_PLAYER!!.getSize() - 1) * 2
                        drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeY, StaticMedia.mapflagSprites?.get(if (minimapContainsFlag) 1 else 0))
                    }

                    if (!PlayerUpdate.MY_PLAYER!!.hidden) {
                        toolkit.clearPixels(clipLeft + component.width / 2 - 1, clipTop + component.height / 2 - 1, 3, 3, -1)
                    }
                }
            } else {
                toolkit.clearRectangle(dimension, clipLeft, clipTop)
            }
        }
    }

    @JvmStatic
    fun drawEntity(component: Component, dimension: ComponentSurface, clipLeft: Int, clipTop: Int, sizeX: Int, sizeZ: Int, sprite: NativeSprite?) {
        if (sprite != null) {
            val rotation: Int
            if (Camera.cameraCalculationType == 4) {
                rotation = Camera.camAngleY.toInt() and 0x3fff
            } else {
                rotation = Camera.camAngleY.toInt() + Client.minimapRotation and 0x3fff
            }

            val largestSize = Math.max(component.width / 2, component.height / 2) + 10
            val size2d = sizeZ * sizeZ + sizeX * sizeX
            if (size2d <= largestSize * largestSize) {
                var xan = MathUtils.SINE[rotation]
                var zan = MathUtils.COSINE[rotation]
                if (Camera.cameraCalculationType != 4) {
                    xan = xan * 256 / (Client.minimapZoom + 256)
                    zan = zan * 256 / (Client.minimapZoom + 256)
                }

                val w = zan * sizeX + sizeZ * xan shr 14
                val h = sizeZ * zan - xan * sizeX shr 14
                sprite.drawWithSurface(w + component.width / 2 + clipLeft - sprite.getSpriteWidth() / 2, clipTop + component.height / 2 - h - sprite.getSpriteHeight() / 2, dimension, clipLeft, clipTop)
            }
        }
    }

    @JvmStatic
    fun drawMapIcon(toolkit: AbstractToolkit, locType: ObjectType, rotation: Int, drawPositionX: Int, drawPositionY: Int) {
        var rotation = rotation
        val mapspriteType: MapSpriteType = TypeLists.MAP_SPRITE!!.list(locType.mapIconId)
        if (mapspriteType.fileId != -1) {
            if (locType.mapIconRotates) {
                rotation += locType.mapIconRotation
                rotation = rotation and 0x3
            } else {
                rotation = 0
            }
            val nativeSprite: NativeSprite? = mapspriteType.createRotatedNativeSprite(toolkit, rotation, locType.mapIconFlipped)
            if (nativeSprite != null) {
                val sizeX: Int
                val sizeY: Int
                if ((rotation and 0x1) == 1) {
                    sizeX = locType.sizeY
                    sizeY = locType.sizeX
                } else {
                    sizeX = locType.sizeX
                    sizeY = locType.sizeY
                }
                var upscaledSizeX = nativeSprite.getSpriteWidth()
                var upscaledSizeY = nativeSprite.getSpriteHeight()
                if (mapspriteType.requiresUpscaling) {
                    upscaledSizeX = sizeX * 4
                    upscaledSizeY = sizeY * 4
                }

                if (mapspriteType.backgroundColor != 0) {
                    nativeSprite.drawCircle(drawPositionX, drawPositionY - (sizeY - 1) * 4, upscaledSizeX, upscaledSizeY, 0, 0xffffff.inv() or mapspriteType.backgroundColor, 1)
                } else {
                    nativeSprite.drawCircle(drawPositionX, drawPositionY - (sizeY - 1) * 4, upscaledSizeX, upscaledSizeY)
                }
            }
        }
    }

    @JvmField
    var mapViewportScale = 0

    @JvmStatic
    fun reset() {
        minimapBackground = null
        MAP_PLANE = -1
    }

    @JvmField
    var worldMapOverviewSprite: NativeSprite? = null

    @JvmStatic
    fun drawCompass(component: Component, clipLeft: Int, clipTop: Int) {
        val sprite: InteractiveComponentSprite? = component.addInteractiveSprite(Renderers.activeToolkit!!)
        if (sprite != null) {
            Renderers.activeToolkit!!.setClipping(clipLeft, clipTop, component.width + clipLeft, clipTop + component.height)
            if (minimapState < 3) {
                var angle = (-Camera.camAngleY).toInt()
                angle = angle + Client.minimapRotation * 2 and 0x3fff
                angle = angle shl 2
                StaticMedia.compassSprite!!.drawRotatedSprite(component.width / 2.0F + clipLeft, component.height / 2.0F + clipTop, angle, sprite.surface, clipLeft, clipTop)
            } else {
                Renderers.activeToolkit!!.clearRectangle(sprite.surface, clipLeft, clipTop)
            }
        }
    }

    @JvmStatic
    fun drawPlayers(absX: Int, absY: Int, component: Component, dimension: ComponentSurface, clipLeft: Int, clipTop: Int) {
        val numPlayerIndices = PlayerUpdate.NUM_OUT_PLAYER_INDICES
        val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES
        for (playerIndex in 0 until numPlayerIndices) {
            val player: PlayerEntity? = PlayerUpdate.players[playerIndices[playerIndex]]
            if (player != null && player.hasAppearance() && !player.hidden && player != PlayerUpdate.MY_PLAYER!! && player.plane == PlayerUpdate.MY_PLAYER!!.plane) {
                val absolutePosition: Vector3 = player.getAbsolutePosition().coords
                val sizeX = absolutePosition.x.toInt() / 128 - absX / 128
                val sizeZ = absolutePosition.z.toInt() / 128 - absY / 128
                var isFriend = false
                for (friendIndex in 0 until FriendList.FRIEND_COUNT) {
                    val friend: Friend? = FriendList.FRIENDS[friendIndex]
                    if (friend != null && player.displayName == friend.displayName && friend.worldId != 0) {
                        isFriend = true
                        break
                    }
                }
                var inFriendschat = false
                for (fcPlayerIndex in 0 until FriendsChat.FC_PLAYER_COUNT) {
                    if (player.displayName == FriendsChat.FC_PLAYERS?.get(fcPlayerIndex)?.username) {
                        inFriendschat = true
                        break
                    }
                }
                val onSameTeam = player.teamId != 0 && player.teamId == PlayerUpdate.MY_PLAYER!!.teamId
                if (player.pMapdot) {
                    drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(6))
                } else if (onSameTeam) {
                    drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(4))
                } else if (player.reduceAttackPriority) {
                    drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(7))
                } else if (isFriend) {
                    drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(3))
                } else if (inFriendschat) {
                    drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(5))
                } else {
                    drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(2))
                }
            }
        }
    }

    @JvmStatic
    fun drawNpcs(toolkit: AbstractToolkit, absX: Int, absY: Int, component: Component, dimension: ComponentSurface, clipLeft: Int, clipTop: Int) {
        for (npcIndex in 0 until Client.NPC_UPDATE_INDEX) {
            val node: ObjectNode? = Client.NPC_MAP.get(Client.NPC_UPDATE_INDICES[npcIndex].toLong()) as? ObjectNode
            if (node != null) {
                val npcEntity = node.value as? NpcEntity
                if (npcEntity != null && npcEntity.hasDefinition() && npcEntity.plane == PlayerUpdate.MY_PLAYER!!.plane) {
                    var npctype: NPCType? = npcEntity.definitions
                    if (npctype != null && npctype.alternativeIds != null) {
                        npctype = npctype.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
                    }

                    if (npctype != null && npctype.displayOnMinimap && npctype.visible) {
                        val absolutePosition: Vector3 = npcEntity.getAbsolutePosition().coords
                        val sizeX = absolutePosition.x.toInt() / 128 - absX / 128
                        val sizeZ = absolutePosition.z.toInt() / 128 - absY / 128
                        if (npctype.mecId != -1) {
                            drawMec(toolkit, dimension, component, clipLeft, clipTop, sizeX, sizeZ, npctype.mecId)
                        } else {
                            drawEntity(component, dimension, clipLeft, clipTop, sizeX, sizeZ, StaticMedia.mapdotSprites?.get(1))
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun drawMec(toolkit: AbstractToolkit, dimension: ComponentSurface, component: Component, clipLeft: Int, clipTop: Int, offsetX: Int, offsetY: Int, mecId: Int) {
        val mecType: MECType? = TypeLists.MEC!!.list(mecId)
        if (mecType != null && mecType.displayedOnMinimap && mecType.shouldDraw(VarProviders.PLAYER_VAR_PROVIDER)) {
            var i: Int
            if (mecType.offsets != null) {
                val offsets = mecType.offsets
                val polygonPoints = IntArray(offsets!!.size)
                i = 0
                while (i < polygonPoints.size / 2) {
                    val rotation: Int
                    if (Camera.cameraCalculationType == 4) {
                        rotation = Camera.camAngleY.toInt() and 0x3fff
                    } else {
                        rotation = Camera.camAngleY.toInt() + Client.minimapRotation and 0x3fff
                    }
                    var xan = MathUtils.SINE[rotation]
                    var zan = MathUtils.COSINE[rotation]
                    if (Camera.cameraCalculationType != 4) {
                        xan = xan * 256 / (Client.minimapZoom + 256)
                        zan = zan * 256 / (Client.minimapZoom + 256)
                    }
                    polygonPoints[i * 2] = clipLeft + component.width / 2 + ((offsets[i * 2 + 1] * 4 + offsetY) * xan + zan * (offsetX + offsets[i * 2] * 4) shr 14)
                    polygonPoints[i * 2 + 1] = component.height / 2 + clipTop - ((offsets[i * 2 + 1] * 4 + offsetY) * zan - xan * (offsetX + offsets[i * 2] * 4) shr 14)
                    i++
                }
                val sprite: InteractiveComponentSprite? = component.addInteractiveSprite(toolkit)
                if (sprite != null) {
                    WorldMapRasterManager.fillPolygonMasked(toolkit, polygonPoints, mecType.lineColor, sprite.rowStartX!!, sprite.rowWidth!!)
                }
                if (mecType.dashLineSpacing > 0) {
                    var closeY1: Int
                    var closeTmp: Int
                    var swapTmp: Int
                    val colors = mecType.colors!!
                    val colorPointers = mecType.colorPointers!!
                    var j = 0
                    while (j < polygonPoints.size / 2 - 1) {
                        var lineX0 = polygonPoints[j * 2]
                        var lineY0 = polygonPoints[j * 2 + 1]
                        var lineX1 = polygonPoints[(j + 1) * 2]
                        var lineY1 = polygonPoints[(j + 1) * 2 + 1]
                        if (lineX1 < lineX0) {
                            swapTmp = lineX0
                            val swapTmpY = lineY0
                            lineX0 = lineX1
                            lineY0 = lineY1
                            lineX1 = swapTmp
                            lineY1 = swapTmpY
                        } else if (lineX0 == lineX1 && lineY1 < lineY0) {
                            swapTmp = lineY0
                            lineY0 = lineY1
                            lineY1 = swapTmp
                        }
                        toolkit.drawTexturedLine(lineX0, lineY0, lineX1, lineY1, colors[colorPointers[j].toInt() and 0xff], dimension, clipLeft, clipTop, mecType.dashLineSpacing, mecType.dashLineLength, mecType.dashLineOffset)
                        j++
                    }
                    j = polygonPoints[polygonPoints.size - 2]
                    var closeY0 = polygonPoints[polygonPoints.size - 1]
                    var closeX1 = polygonPoints[0]
                    closeY1 = polygonPoints[1]
                    if (closeX1 < j) {
                        closeTmp = j
                        swapTmp = closeY0
                        j = closeX1
                        closeY0 = closeY1
                        closeX1 = closeTmp
                        closeY1 = swapTmp
                    } else if (closeX1 == j && closeY1 < closeY0) {
                        closeTmp = closeY0
                        closeY0 = closeY1
                        closeY1 = closeTmp
                    }
                    toolkit.drawTexturedLine(j, closeY0, closeX1, closeY1, colors[colorPointers[colorPointers.size - 1].toInt() and 0xff], dimension, clipLeft, clipTop, mecType.dashLineSpacing, mecType.dashLineLength, mecType.dashLineOffset)
                } else {
                    val colors = mecType.colors!!
                    val colorPointers = mecType.colorPointers!!
                    var j = 0
                    while (j < polygonPoints.size / 2 - 1) {
                        toolkit.drawSurfaceLine(polygonPoints[j * 2], polygonPoints[j * 2 + 1], polygonPoints[(j + 1) * 2], polygonPoints[(j + 1) * 2 + 1], colors[colorPointers[j].toInt() and 0xff], dimension, clipLeft, clipTop)
                        j++
                    }
                    toolkit.drawSurfaceLine(polygonPoints[polygonPoints.size - 2], polygonPoints[polygonPoints.size - 1], polygonPoints[0], polygonPoints[1], colors[colorPointers[colorPointers.size - 1].toInt() and 0xff], dimension, clipLeft, clipTop)
                }
            }
            var iconSprite: NativeSprite? = null
            if (mecType.defaultIconArchive != -1) {
                iconSprite = mecType.getIcon(toolkit, false)
                if (iconSprite != null) {
                    drawEntity(component, dimension, clipLeft, clipTop, offsetX, offsetY, iconSprite)
                }
            }
            if (mecType.areaName != null) {
                i = 0
                if (iconSprite != null) {
                    i = iconSprite.getHeight()
                }
                var fontRenderer: AbstractFontRenderer? = Renderers.FONT_RENDERER
                var fontMetrics: FontMetrics? = FontManager.worldMapAreaFont
                if (mecType.baseTextzoom == 1) {
                    fontRenderer = FontManager.debugOverlayFont
                    fontMetrics = FontManager.messageboxFont
                }
                if (mecType.baseTextzoom == 2) {
                    fontRenderer = MiniMenu.minimenuHeader
                    fontMetrics = FontMetrics.defaultFontMetrics
                }
                InterfaceComponentRenderer.drawMinimapText(component, dimension, clipLeft, clipTop, offsetX, offsetY, i, mecType.areaName, fontRenderer!!, fontMetrics!!, mecType.defaultTextColor)
            }
        }
    }

    @JvmStatic
    fun drawGame(toolkit: AbstractToolkit, fromPlane: Int): Boolean {
        Utils.time()
        if (!toolkit.readyForRendering()) {
            return false
        }
        val regionDimensionX = Resource.mapRegion!!.sceneSizeX
        val regionDimensionY = Resource.mapRegion!!.sceneSizeY
        val settingsBits: SettingsBits = Resource.mapRegion!!.settingsBits
        val scene: Scene = Resource.mapRegion!!.getScene()!!
        var halfRegionDimension = regionDimensionX / 2
        var mapSpritesCached = true
        for (x in 0 until regionDimensionX) {
            for (y in 0 until regionDimensionY) {
                for (z in fromPlane..3) {
                    if (settingsBits.isVisible(fromPlane, z, x, y)) {
                        var plane = z
                        if (settingsBits.areRoofsHidden(x, y)) {
                            plane = z - 1
                        }
                        if (plane >= 0) {
                            mapSpritesCached = mapSpritesCached and MapRegion.isMapSpriteCached(plane, x, y)
                        }
                    }
                }
            }
        }
        if (!mapSpritesCached) {
            return false
        } else {
            val minimapDimension = regionDimensionX * 4 + 48 + 48
            val minimapPixels = IntArray(minimapDimension * minimapDimension)
            for (index in minimapPixels.indices) {
                minimapPixels[index] = MapRegion.BACKGROUND_COLOR
            }
            var directDisplay: DirectDisplay? = null
            var centerOffsetX: Byte = 0
            var centerOffsetY: Byte = 0
            if (MapRegion.requiresDisplayResize) {
                minimapBackground = toolkit.generateEmptySprite(minimapDimension, minimapDimension, false, true)
                directDisplay = toolkit.getImageProducer()
                directDisplay.setFramebuffer(0, minimapBackground!!.getFramebuffer())
                val minimapRenderBuffer: RenderBuffer = toolkit.getRenderBuffer(minimapDimension, minimapDimension)
                directDisplay.setRenderBuffer(minimapRenderBuffer)
                toolkit.setDisplay(directDisplay)
                halfRegionDimension = regionDimensionX
                centerOffsetX = 48
                centerOffsetY = 48
                toolkit.applyClearFlags(1, 0)
            } else {
                minimapBackground = toolkit.createNativeSprite(minimapPixels, minimapDimension, minimapDimension, minimapDimension)
            }
            Resource.mapRegion!!.getEnvironmentManager().prepareLights()
            val backgroundMaxRandomise = MapRegion.BACKGROUND_COLOR or ((238 + (Math.random() * 20.0).toInt() - 10 shl 16) + (238 + (Math.random() * 20.0).toInt() - 10 shl 8) + (238 + (Math.random() * 20.0).toInt() - 10))
            val backgroundMinRandomise = MapRegion.BACKGROUND_COLOR or (238 + (Math.random() * 20.0).toInt() - 10 shl 16)
            val pixelClearColor = (Math.random() * 8.0).toInt() shl 16 or ((Math.random() * 8.0).toInt() shl 8) or (Math.random() * 8.0).toInt()
            val visibleTiles = Array(halfRegionDimension + 1 + 2) { BooleanArray(halfRegionDimension + 1 + 2) }
            val mapRegion2 = Resource.mapRegion!!
            var width = 0
            while (width < regionDimensionX) {
                var length = 0
                while (length < regionDimensionY) {
                    var drawPositionX = centerOffsetX.toInt()
                    var drawPositionY = centerOffsetY.toInt()
                    var tilesStartX = width
                    if (width > 0) {
                        tilesStartX = width - 1
                        drawPositionX = centerOffsetX + 4
                    }
                    var tilesStartY = length
                    if (length > 0) {
                        tilesStartY = length - 1
                    }
                    var tilesEndX = halfRegionDimension + width
                    if (tilesEndX < regionDimensionX) {
                        tilesEndX++
                    }
                    var tilesEndY = halfRegionDimension + length
                    if (tilesEndY < regionDimensionY) {
                        tilesEndY++
                        drawPositionY = centerOffsetY + 4
                    }
                    if (MapRegion.requiresClippingReset) {
                        toolkit.resetClipping()
                    } else {
                        toolkit.setClipping(0, 0, drawPositionX + halfRegionDimension * 4, drawPositionY + halfRegionDimension * 4)
                    }
                    toolkit.applyClearFlags(3, MapRegion.BACKGROUND_COLOR)
                    val minHalfRegionDimension = Math.min(halfRegionDimension, regionDimensionX - 1)
                    for (z in fromPlane..3) {
                        for (x in 0..minHalfRegionDimension) {
                            for (y in 0..minHalfRegionDimension) {
                                visibleTiles[x][y] = settingsBits.isVisible(fromPlane, z, x + tilesStartX, y + tilesStartY)
                            }
                        }
                        scene.surfaceHeightmap!![z]!!.renderMinimapFloor(centerOffsetX.toInt(), centerOffsetY.toInt(), tilesStartX, tilesStartY, tilesEndX, tilesEndY, visibleTiles)
                        if (!MapRegion.visualizeClippedTilesOnMinimap) {
                            for (offsetX in -4 until halfRegionDimension) {
                                for (offsetY in -4 until halfRegionDimension) {
                                    val sceneX = width + offsetX
                                    val sceneY = length + offsetY
                                    if (sceneX >= 0 && sceneY >= 0 && settingsBits.isVisible(fromPlane, z, sceneX, sceneY)) {
                                        var locPlane = z
                                        if (settingsBits.areRoofsHidden(sceneX, sceneY)) {
                                            locPlane = z - 1
                                        }
                                        if (locPlane >= 0) {
                                            drawLocOnMinimap(toolkit, locPlane, sceneX, sceneY, drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, backgroundMaxRandomise, backgroundMinRandomise)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (MapRegion.visualizeClippedTilesOnMinimap) {
                        val collision: CollisionMap = mapRegion2.getCollisionMap(fromPlane)
                        for (offsetX in 0 until halfRegionDimension) {
                            for (offsetY in 0 until halfRegionDimension) {
                                val tileX = width + offsetX
                                val tileY = length + offsetY
                                val activeFlag = collision.collisionFlags[tileX - collision.offsetX][tileY - collision.offsetY]
                                if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.BLOCKED_DECO, CollisionFlag.BLOCKED, CollisionFlag.ALT_OBJ)) {
                                    toolkit.clearPixels(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, 4, -1713569622)
                                } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.N_ALT_OBJ)) {
                                    toolkit.drawOpaqueHorizontalLine(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, -1713569622)
                                } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.E_ALT_OBJ)) {
                                    toolkit.drawOpaqueVerticalLine(drawPositionX + offsetX * 4 + 3, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, -1713569622)
                                } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.S_ALT_OBJ)) {
                                    toolkit.drawOpaqueHorizontalLine(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4 + 3, 4, -1713569622)
                                } else if (!CollisionMap.notFlagged(activeFlag, CollisionFlag.W_ALT_OBJ)) {
                                    toolkit.drawOpaqueVerticalLine(drawPositionX + offsetX * 4, (halfRegionDimension - offsetY) * 4 + drawPositionY - 4, 4, -1713569622)
                                }
                            }
                        }
                    }
                    toolkit.drawFilledRectangle(drawPositionX, drawPositionY, halfRegionDimension * 4, halfRegionDimension * 4, pixelClearColor, 2)
                    if (!MapRegion.requiresDisplayResize) {
                        minimapBackground!!.captureScreenRegion(width * 4 + 48, regionDimensionY * 4 + 48 - length * 4 - halfRegionDimension * 4, halfRegionDimension * 4, halfRegionDimension * 4, drawPositionX, drawPositionY)
                        if (MapRegion.requiresClippingReset) {
                            minimapBackground!!.draw(256, 0)
                            try {
                                toolkit.renderFrame()
                                TimeTools.sleep(2000L)
                            } catch (exception: Exception) {
                                exception.printStackTrace()
                            }
                        }
                    }
                    length += halfRegionDimension
                }
                width += halfRegionDimension
            }
            val mapRegion = Resource.mapRegion!!
            if (MapRegion.requiresDisplayResize) {
                toolkit.resizeDisplay(directDisplay!!)
                if (MapRegion.requiresClippingReset) {
                    minimapBackground!!.draw(256, 0)
                    try {
                        toolkit.renderFrame()
                        TimeTools.sleep(2000L)
                    } catch (exception: Exception) {
                        exception.printStackTrace()
                    }
                }
            }
            toolkit.resetClipping()
            toolkit.applyClearFlags(1, 1)
            GameShell.setComponentsOnTop()
            val locTypeList: ObjectTypeList = mapRegion.getLoctypeList()
            minimapLocCount = 0
            MapRegion.staticElementsAwaitingDrawing.remove_all()
            if (!MapRegion.visualizeClippedTilesOnMinimap) {
                for (sceneX in 0 until regionDimensionX) {
                    for (sceneY in 0 until regionDimensionY) {
                        for (sceneZ in fromPlane..Math.min(fromPlane + 1, 3)) {
                            if (settingsBits.isVisible(fromPlane, sceneZ, sceneX, sceneY)) {
                                var gameObject: GameObject? = scene.getGroundDecoration(sceneZ, sceneX, sceneY) as? GameObject
                                if (gameObject == null) {
                                    gameObject = scene.getInteractiveObject(sceneZ, sceneX, sceneY, Client.INTERACTIVE_OBJECT_COMPARABLE) as? GameObject
                                }
                                if (gameObject == null) {
                                    gameObject = scene.getWall(sceneZ, sceneX, sceneY) as? GameObject
                                }
                                if (gameObject == null) {
                                    gameObject = scene.getPrimaryWallDecoration(sceneZ, sceneX, sceneY) as? GameObject
                                }
                                if (gameObject != null) {
                                    val locType: ObjectType = locTypeList.list(gameObject.id)
                                    if (!locType.members || Client.memberWorld) {
                                        var mapCategoryId = locType.mapCategoryid
                                        if (locType.alternativeIds != null) {
                                            val alternativeIds = locType.alternativeIds!!
                                            for (altIndex in alternativeIds.indices) {
                                                if (alternativeIds[altIndex] != -1) {
                                                    val morphedLoc: ObjectType = locTypeList.list(alternativeIds[altIndex])
                                                    if (morphedLoc.mapCategoryid >= 0) {
                                                        mapCategoryId = morphedLoc.mapCategoryid
                                                    }
                                                }
                                            }
                                        }
                                        if (mapCategoryId >= 0) {
                                            var mectypeHasRandomOffset = false
                                            val mectype: MECType? = TypeLists.MEC!!.list(mapCategoryId)
                                            if (mectype != null && mectype.hasRandomisedMinimapPosition) {
                                                mectypeHasRandomOffset = true
                                            }
                                            var mectypeSceneX = sceneX
                                            var mectypeSceneY = sceneY
                                            if (mectypeHasRandomOffset) {
                                                val collision = mapRegion.getCollisionMap(sceneZ).collisionFlags
                                                val offsetX = mapRegion.getCollisionMap(sceneZ).offsetX
                                                val offsetY = mapRegion.getCollisionMap(sceneZ).offsetY
                                                for (i in 0 until 10) {
                                                    val randomPositioning = (Math.random() * 4.0).toInt()
                                                    if (randomPositioning == 0 && mectypeSceneX > 0 && mectypeSceneX > sceneX - 3 && (collision[mectypeSceneX - 1 - offsetX][mectypeSceneY - offsetY] and 0x2c0108) == 0) {
                                                        mectypeSceneX--
                                                    }
                                                    if (randomPositioning == 1 && mectypeSceneX < regionDimensionX - 1 && mectypeSceneX < sceneX + 3 && (collision[mectypeSceneX + 1 - offsetX][mectypeSceneY - offsetY] and 0x2c0180) == 0) {
                                                        mectypeSceneX++
                                                    }
                                                    if (randomPositioning == 2 && mectypeSceneY > 0 && mectypeSceneY > sceneY - 3 && (collision[mectypeSceneX - offsetX][mectypeSceneY - 1 - offsetY] and 0x2c0102) == 0) {
                                                        mectypeSceneY--
                                                    }
                                                    if (randomPositioning == 3 && mectypeSceneY < regionDimensionY - 1 && mectypeSceneY < sceneY + 3 && (collision[mectypeSceneX - offsetX][mectypeSceneY + 1 - offsetY] and 0x2c0120) == 0) {
                                                        mectypeSceneY++
                                                    }
                                                }
                                            }
                                            minimapLocId[minimapLocCount] = locType.id
                                            minimapLocSceneX[minimapLocCount] = mectypeSceneX
                                            minimapLocSceneY[minimapLocCount] = mectypeSceneY
                                            minimapLocCount++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                val staticElements: StaticElements? = mapRegion.getStaticElements()
                if (staticElements != null) {
                    TypeLists.MEC!!.createCache(1024, 64)
                    val baseCoordinates: CoordGrid = mapRegion.getBaseCoordinates()
                    for (index in 0 until staticElements.elementCount) {
                        val bitpacked = staticElements.bitpackedPlacement[index]
                        if (bitpacked shr 28 == PlayerUpdate.MY_PLAYER!!.plane.toInt()) {
                            val sceneX = (bitpacked shr 14 and 0x3fff) - baseCoordinates.x
                            val sceneY = (bitpacked and 0x3fff) - baseCoordinates.y
                            if (sceneX >= 0 && sceneX < regionDimensionX && sceneY >= 0 && sceneY < regionDimensionY) {
                                MapRegion.staticElementsAwaitingDrawing.addFirst(IntNode(index))
                            } else {
                                val mectype: MECType = TypeLists.MEC!!.list(staticElements.mapCategoryIds[index])
                                if (mectype.offsets != null && sceneX + mectype.maxOffsetX >= 0
                                        && sceneX + mectype.minOffsetX < regionDimensionX
                                        && sceneY + mectype.maxOffsetY >= 0
                                        && sceneY + mectype.minOffsetY < regionDimensionY) {
                                    MapRegion.staticElementsAwaitingDrawing.addFirst(IntNode(index))
                                }
                            }
                        }
                    }
                    TypeLists.MEC!!.createCache(128, 64)
                }
            }
            MinimapDump.dump(minimapBackground, "bg")
            return true
        }
    }

    @JvmStatic
    fun drawGame(toolkit: AbstractToolkit) {
        if (MAP_PLANE != PlayerUpdate.MY_PLAYER!!.plane.toInt() && Resource.mapRegion!!.getScene() != null) {
            Utils.time()
            if (drawGame(toolkit, PlayerUpdate.MY_PLAYER!!.plane.toInt())) {
                MAP_PLANE = PlayerUpdate.MY_PLAYER!!.plane.toInt()
            }
        }
    }

    @JvmStatic
    fun drawGame() {
        drawGame(Renderers.activeToolkit!!)
        if (Client.ROOF_OVERRIDE != PlayerUpdate.MY_PLAYER_PLANE) {
            Scene.setRoofOverride()
        }
    }
}
