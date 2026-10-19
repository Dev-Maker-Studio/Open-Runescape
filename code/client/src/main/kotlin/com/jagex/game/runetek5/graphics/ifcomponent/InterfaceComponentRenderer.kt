package com.jagex.game.runetek5.graphics.ifcomponent

import java.awt.Canvas

import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.midi.VorbisNode
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapOverview
import com.jagex.game.runetek5.graphics.model.Model
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL
import com.jagex.game.runetek5.graphics.model.MediaType
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.SpriteType
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarProviders

import com.jagex.game.runetek5.config.rsinterface.ComponentContentType.*

object InterfaceComponentRenderer {

    @JvmField var deferredDragComponents: Array<Component?>? = null
    @JvmField var deferredDragOffsetX: Int = 0
    @JvmField var chatDisplayComponent: Component? = null
    @JvmField var chatDisplayX: Int = -1
    @JvmField var chatDisplayY: Int = -1
    @JvmField var deferredDragOffsetY: Int = 0
    @JvmField var prevCutsceneFadeBlue: Int = 0
    @JvmField var prevCutsceneFadeGreen: Int = 0
    @JvmField var prevCutsceneFadeRed: Int = 0
    @JvmField var prevCutsceneFadeAlpha: Int = 0
    @JvmField var cutsceneFadeAlpha: Int = 0
    @JvmField var cutsceneFadeRed: Int = 0
    @JvmField var cutsceneFadeBlue: Int = 0
    @JvmField var cutsceneFadeGreen: Int = 0
    @JvmField var cutsceneFadeEndCycle: Int = 0
    @JvmField var cycleCutsceneFadeStarted: Int = 0

    @JvmStatic
    fun cancelInterfaceSelection() {
        if (Client.interfaceSelectionActive) {
            val component = Component.listIcomponent(Client.selectedComponentHash, Client.selectedSlotId)
            if (component != null && component.hookParams != null) {
                val hookRequest = HookRequest()
                hookRequest.source = component
                hookRequest.params = component.hookParams
                CS2Executor.executeHookInner(hookRequest)
            }
            Client.defaultMenuCursor = -1
            Client.selectedItemId = -1
            Client.interfaceSelectionActive = false
            if (component != null) {
                Component.redrawComponent(component)
            }
        }
    }

    @JvmStatic
    fun drawObjSprite(toolkit: AbstractToolkit, component: Component) {
        val spriteMissing = TypeLists.OBJ!!.createNativeSprite(toolkit, component.containerItemId, component.itemStackSize, component.borderThickness, -0x1000000 or component.spriteShadow, component.renderStack, if (component.wearCol) PlayerUpdate.MY_PLAYER!!.playerAppearance else null) == null
        if (spriteMissing) {
            Component.ITEMS.addFirst(Item(component.containerItemId, component.itemStackSize, component.borderThickness, -0x1000000 or component.spriteShadow, component.renderStack, component.wearCol))
            Component.redrawComponent(component)
        }
    }

    @JvmStatic
    fun drawComponents(components: Array<Component?>, parentId: Int, clippingLeft: Int, clippingTop: Int, clippingRight: Int, clippingBottom: Int, offsetX: Int, offsetY: Int, parentCacheReference: Int, isTopLevelLayer: Boolean) {
        Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
        for (componentIndex in components.indices) {
            val component = components[componentIndex]
            if (component != null && (component.parentId == parentId || parentId == -1412584499 && component === Client.draggingComponent)) {
                val componentReference: Int
                if (parentCacheReference == -1) {
                    Client.INTERFACE_BG_COMPONENTS[Client.componentBackgroundCount]!!.setBounds(component.x + offsetX, component.y + offsetY, component.width, component.height)
                    componentReference = ++Client.componentBackgroundCount - 1
                } else {
                    componentReference = parentCacheReference
                }
                component.cacheReference = componentReference
                component.lastRenderedCycle = Client.cycles
                if (!InterfaceRenderer.isComponentHidden(component)) {
                    if (component.contentType != null) {
                        Component.prepareSequenceOnInterface(component)
                    }
                    val drawX = component.x + offsetX
                    val drawY = component.y + offsetY
                    var drawTransparency = component.transparency
                    if (Client.DISPLAY_HIDDEN_MENU_OPTIONS && (Client.getComponentSettings(component)!!.settingsHash != 0 || component.type == ComponentType.CONTAINER) && drawTransparency > 127) {
                        drawTransparency = 127
                    }
                    val startX: Int
                    val startY: Int
                    if (component === Client.draggingComponent) {
                        if (parentId != -1412584499 && (component.dragType == Component.DragType.UNK_2 || component.dragType == Component.DragType.UNK_0 || Client.getComponentSettings(component)!!.ignoresDepthFlags())) {
                            deferredDragComponents = components
                            deferredDragOffsetX = offsetX
                            deferredDragOffsetY = offsetY
                            continue
                        }
                        if (Client.isDragThresholdMet && Client.isComponentSelected) {
                            var dragX = MouseRecord.recorder!!.getMouseX()
                            var dragY = MouseRecord.recorder!!.getMouseY()
                            dragX -= Client.mouseX
                            dragY -= Client.mouseY
                            if (dragX < Client.selectedComponentX) {
                                dragX = Client.selectedComponentX
                            }
                            if (dragX + component.width > Client.selectedComponentX + Client.selectedComponentWidth) {
                                dragX = Client.selectedComponentX + Client.selectedComponentWidth - component.width
                            }
                            if (dragY < Client.selectedComponentY) {
                                dragY = Client.selectedComponentY
                            }
                            if (dragY + component.height > Client.selectedComponentY + Client.selectedComponentHeight) {
                                dragY = Client.selectedComponentHeight + Client.selectedComponentY - component.height
                            }
                            if (Client.getComponentSettings(component)!!.ignoresDepthFlags()) {
                                inboundsComponentToTop(dragX, dragY, component.width, component.height)
                            }
                            startX = dragX
                            startY = dragY
                        } else {
                            startX = drawX
                            startY = drawY
                        }
                        if (component.dragType == Component.DragType.UNK_0) {
                            drawTransparency = 128
                        }
                    } else {
                        startX = drawX
                        startY = drawY
                    }
                    val endX: Int
                    val endY: Int
                    if (component.type == ComponentType.TYPE_2) {
                        endX = clippingRight
                        endY = clippingBottom
                    } else {
                        var rightX = startX + component.width
                        var bottomY = startY + component.height
                        if (component.type == ComponentType.LINE) {
                            rightX++
                            bottomY++
                        }
                        endX = Math.min(rightX, clippingRight)
                        endY = Math.min(bottomY, clippingBottom)
                    }
                    val clipStartX = if (component.type == ComponentType.TYPE_2) clippingLeft else Math.max(startX, clippingLeft)
                    val clipStartY = if (component.type == ComponentType.TYPE_2) clippingTop else Math.max(startY, clippingTop)
                    if (clipStartX < endX && clipStartY < endY) {
                        if (component.contentType != null) {
                            if (component.contentType == GAME_WINDOW || component.contentType == CONTENT_TYPE_1403) {
                                Client.drawGameArea(startX, startY, component.width, component.height, component.contentType == CONTENT_TYPE_1403)
                                EntityUpdating.drawOverheadInterfaces(componentReference, clipStartX, clipStartY, endX, endY, startX, startY)
                                Renderers.activeToolkit!!.calculateViewportCenter()
                                Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                Client.IS_COMPONENT_ON_TOP[componentReference] = true
                                continue
                            }
                            if (component.contentType == MINIMAP && Client.MAP_LOADING_STAGE == 1) {
                                val toolkit = Renderers.activeToolkit
                                if (component.addInteractiveSprite(toolkit!!) != null) {
                                    SoundEffectPlayer.updatePlayers()
                                    ComponentMinimap.drawMinimap(toolkit, component, startX, startY)
                                    toolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                }
                                continue
                            }
                            if (component.contentType == COLOR_PICKER) {
                                ColorPicker.drawColorPicker(Renderers.activeToolkit!!, startX, startY, component)
                                continue
                            }
                            if (component.contentType == CONTENT_TYPE_1409) {
                                AbstractToolkit.drawFadingFilledRectangle(Renderers.activeToolkit!!, startX, startY, component, component.color % 64)
                                continue
                            }
                            if (component.contentType == COMPASS) {
                                val toolkit = Renderers.activeToolkit
                                if (component.addInteractiveSprite(toolkit!!) != null) {
                                    ComponentMinimap.drawCompass(component, startX, startY)
                                    toolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                }
                                continue
                            }
                            if (component.contentType == WORLDMAP) {
                                WorldMap.drawWorldmap(Renderers.activeToolkit!!, TypeLists.MATERIALS!!, startX, startY, component.width, component.height)
                                Client.IS_COMPONENT_ON_TOP[componentReference] = true
                                Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                continue
                            }
                            if (component.contentType == WORLD_MAP_OVERVIEW) {
                                WorldMapOverview.drawWorldmapOverview(Renderers.activeToolkit!!, startX, startY, component.width, component.height)
                                Client.IS_COMPONENT_ON_TOP[componentReference] = true
                                Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                continue
                            }
                            if (component.contentType == DEBUG_INFORMATION) {
                                if (Client.DRAW_DEBUG || Client.DEBUG_MODE) {
                                    SystemInfo.drawDebugInformation(startX, startY, component)
                                    Client.IS_COMPONENT_ON_TOP[componentReference] = true
                                }
                                continue
                            }
                        }

                        if (component.type == ComponentType.CONTAINER) {
                            if (component.contentType == CUTSCENE_FADE && Renderers.activeToolkit!!.bloomEnabled()) {
                                Renderers.activeToolkit!!.beginBloomCapture(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                            }
                            drawComponents(components, component.idHash, clipStartX, clipStartY, endX, endY, startX - component.scrollX, startY - component.scrollY, componentReference, isTopLevelLayer)
                            val itemSlots = component.itemSlots
                            if (itemSlots != null) {
                                drawComponents(itemSlots, component.idHash, clipStartX, clipStartY, endX, endY, startX - component.scrollX, startY - component.scrollY, componentReference, isTopLevelLayer)
                            }
                            val subInterface = Client.OPEN_INTERFACES!!.get(component.idHash.toLong())
                            if (subInterface != null) {
                                Component.drawInterface(subInterface.subInterfaceId, clipStartX, clipStartY, endX, endY, startX, startY, componentReference)
                            }
                            if (component.contentType == CUTSCENE_FADE) {
                                if (Renderers.activeToolkit!!.bloomEnabled()) {
                                    Renderers.activeToolkit!!.compositeBloomEffects()
                                }
                                if (Client.MAP_LOADING_STAGE == 4) {
                                    var fadeAlpha = cutsceneFadeAlpha
                                    var fadeRed = cutsceneFadeRed
                                    var fadeGreen = cutsceneFadeGreen
                                    var fadeBlue = cutsceneFadeBlue
                                    if (Client.cycles < cutsceneFadeEndCycle) {
                                        val completion = (Client.cycles - cycleCutsceneFadeStarted) * 1.0f / (cutsceneFadeEndCycle - cycleCutsceneFadeStarted)
                                        fadeAlpha = (cutsceneFadeAlpha * completion + (1.0f - completion) * prevCutsceneFadeAlpha).toInt()
                                        fadeRed = (cutsceneFadeRed * completion + prevCutsceneFadeRed * (1.0f - completion)).toInt()
                                        fadeGreen = (completion * cutsceneFadeGreen + prevCutsceneFadeGreen * (1.0f - completion)).toInt()
                                        fadeBlue = (completion * cutsceneFadeBlue + prevCutsceneFadeBlue * (1.0f - completion)).toInt()
                                    }
                                    if (fadeAlpha > 0) {
                                        Renderers.activeToolkit!!.clearPixels(clipStartX, clipStartY, endX - clipStartX, endY - clipStartY, fadeAlpha shl 24 or (fadeRed shl 16) or (fadeGreen shl 8) or fadeBlue)
                                    }
                                }
                            }
                            Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                        }
                        if (Client.PREV_COMPONENT_ON_TOP[componentReference] || Client.interfaceDebugMode > 1) {
                            if (component.type == ComponentType.FIGURE) {
                                if (drawTransparency == 0) {
                                    if (component.filled) {
                                        Renderers.activeToolkit!!.drawFilledRectangle(startX, startY, component.width, component.height, component.color, 0)
                                    } else {
                                        Renderers.activeToolkit!!.drawRectangleOutline(startX, startY, component.width, component.height, component.color, 0)
                                    }
                                } else if (component.filled) {
                                    Renderers.activeToolkit!!.drawFilledRectangle(startX, startY, component.width, component.height, 255 - (drawTransparency and 0xff) shl 24 or (component.color and 0xffffff), 1)
                                } else {
                                    Renderers.activeToolkit!!.drawRectangleOutline(startX, startY, component.width, component.height, 255 - (drawTransparency and 0xff) shl 24 or (component.color and 0xffffff), 1)
                                }
                            } else if (component.type == ComponentType.TEXT) {
                                val fontRenderer = component.getFontRenderer(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
                                if (fontRenderer == null) {
                                    if (Component.missingMedia) {
                                        Component.redrawComponent(component)
                                    }
                                } else {
                                    var color = component.color
                                    var displayText = component.text
                                    if (component.containerItemId != -1) {
                                        val objType = TypeLists.OBJ!!.list(component.containerItemId)
                                        displayText = objType!!.name
                                        if (displayText == null) {
                                            displayText = "null"
                                        }
                                        if ((objType.stackable == 1 || component.itemStackSize != 1) && component.itemStackSize != -1) {
                                            displayText = TextUtils.setTextColor(16748608) + displayText + "</col>" + " x" + TextUtils.formatStackSize(component.itemStackSize)
                                        }
                                    }
                                    if (component.videoFileId != -1) {
                                        displayText = SoundResourceManager.getVideoSubtitleText(component.videoFileId) ?: ""
                                    }
                                    if (component === Client.activeDialogueContinue) {
                                        displayText = LocalizedText.PLEASE_WAIT.translate(Client.CURRENT_LANGUAGE!!)
                                        color = component.color
                                    }
                                    if (Client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit!!.expandClipping(startX, startY, startX + component.width, startY + component.height)
                                    }
                                    if (component.textAntiMacro) {
                                        fontRenderer.renderTextAntiMacro(displayText!!, startX, startY, component.width, component.height, 255 - (drawTransparency and 0xff) shl 24 or color, if (component.shadow) 255 - (drawTransparency and 0xff) shl 24 else -1, component.textHorizontalAli, component.textVerticalAli, Client.RANDOMIZER, Client.randomSeed, Client.ANTI_MACRO_TEXT_BOUNDS, StaticMedia.nameIconSprites, null)
                                    } else {
                                        fontRenderer.renderText(displayText!!, startX, startY, component.width, component.height, 255 - (drawTransparency and 0xff) shl 24 or color, if (component.shadow) 255 - (drawTransparency and 0xff) shl 24 else -1, component.textHorizontalAli, component.textVerticalAli, component.lineSpacing, component.maxTextLines, StaticMedia.nameIconSprites, null, null, 0, 0)
                                    }
                                    if (Client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                    }
                                }
                            } else if (component.type == ComponentType.SPRITE) {
                                if (component.skyboxId >= 0) {
                                    component.calculateLighting(TypeLists.SKYBOX!!, TypeLists.SUN!!)!!.render(Renderers.activeToolkit!!, startX, startY, component.width, component.height, component.sunRotationX shl 3, component.sunRotationY shl 3)
                                } else {
                                    val sprite: NativeSprite?
                                    if (component.containerItemId != -1) {
                                        val appearance = if (component.wearCol) PlayerUpdate.MY_PLAYER!!.playerAppearance else null
                                        sprite = TypeLists.OBJ!!.createNativeSprite(Renderers.activeToolkit!!, component.containerItemId, component.itemStackSize, component.borderThickness, 0xffffff.inv() or component.spriteShadow, component.renderStack, appearance)
                                    } else if (component.videoFileId != -1) {
                                        sprite = VorbisNode.getVideoFrame(Renderers.activeToolkit!!, component.videoFileId)
                                    } else {
                                        sprite = component.draw(Renderers.activeToolkit!!)
                                    }
                                    if (sprite != null) {
                                        val spriteWidth = sprite.getSpriteWidth()
                                        val spriteHeight = sprite.getSpriteHeight()
                                        val drawColor = 255 - (drawTransparency and 0xff) shl 24 or (if (component.color != 0) component.color and 0xffffff else 16777215)
                                        if (component.tiling) {
                                            Renderers.activeToolkit!!.expandClipping(startX, startY, startX + component.width, startY + component.height)
                                            if (component.angle2d != 0) {
                                                val tileCols = (spriteWidth - 1 + component.width) / spriteWidth
                                                val tileRows = (spriteHeight - 1 + component.height) / spriteHeight
                                                for (tileCol in 0 until tileCols) {
                                                    for (tileRow in 0 until tileRows) {
                                                        if (component.color != 0) {
                                                            sprite.drawRotatedCenterColored((startX + tileCol * spriteWidth) + spriteWidth / 2.0f, spriteHeight / 2.0f + (startY + spriteHeight * tileRow), 4096, component.angle2d, drawColor)
                                                        } else {
                                                            sprite.drawRotatedCenter((startX + spriteWidth * tileCol) + spriteWidth / 2.0f, (startY + spriteHeight * tileRow) + spriteHeight / 2.0f, 4096, component.angle2d)
                                                        }
                                                    }
                                                }
                                            } else if (component.color == 0 && drawTransparency == 0) {
                                                sprite.drawTiled(startX, startY, component.width, component.height)
                                            } else {
                                                sprite.drawScaledColored(startX, startY, component.width, component.height, 0, drawColor, 1)
                                            }
                                            Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                        } else if (component.color == 0 && drawTransparency == 0) {
                                            if (component.angle2d != 0) {
                                                sprite.drawRotatedCenter(component.width / 2.0f + startX, startY + component.height / 2.0f, component.width * 4096 / spriteWidth, component.angle2d)
                                            } else if (spriteWidth == component.width && spriteHeight == component.height) {
                                                sprite.draw(startX, startY)
                                            } else {
                                                sprite.drawCircle(startX, startY, component.width, component.height)
                                            }
                                        } else if (component.angle2d != 0) {
                                            sprite.drawRotatedCenterColored(component.width / 2.0f + startX, component.height / 2.0f + startY, component.width * 4096 / spriteWidth, component.angle2d, drawColor)
                                        } else if (spriteWidth == component.width && spriteHeight == component.height) {
                                            sprite.draw(startX, startY, 0, drawColor, 1)
                                        } else {
                                            sprite.drawCircle(startX, startY, component.width, component.height, 0, drawColor, 1)
                                        }
                                    } else if (Component.missingMedia) {
                                        Component.redrawComponent(component)
                                    }
                                }
                            } else if (component.type == ComponentType.MODEL) {
                                Resource.mapRegion!!.getEnvironmentManager().prepareLights()
                                var renderedMediaModel: Model? = null
                                var functionMask = 2048
                                if (component.modelTintScalar != 0) {
                                    functionMask = functionMask or 0x80000
                                }
                                var centerOffsetY = 0
                                if (component.containerItemId != -1) {
                                    val objType = TypeLists.OBJ!!.list(component.containerItemId)
                                    if (objType != null) {
                                        val stackAltType = objType.getStackSizeAlt(component.itemStackSize)
                                        renderedMediaModel = stackAltType.getAnimatedModel(Renderers.activeToolkit!!, functionMask, 1, if (component.wearCol) PlayerUpdate.MY_PLAYER!!.playerAppearance else null, component.animatingSequence, 0, 0, 0, 0)
                                        if (renderedMediaModel != null) {
                                            centerOffsetY = -renderedMediaModel.getMinY() shr 1
                                        } else {
                                            Component.redrawComponent(component)
                                        }
                                    }
                                } else if (component.mediaType == MediaType.PLAYER_HEAD) {
                                    val playerIndex = component.primaryMediaId
                                    if (playerIndex >= 0 && playerIndex < 2048) {
                                        val player = PlayerUpdate.players[playerIndex]
                                        if (player != null && (playerIndex == PlayerUpdate.myPlayerIndex || player.displayName?.let(Utils::stringToInt) == component.secondaryMediaId)) {
                                            val toolkit = Renderers.activeToolkit
                                            renderedMediaModel = component.getAnimatedMediaModel(toolkit!!, functionMask, TypeLists.BAS!!, TypeLists.IDK!!, TypeLists.NPC!!, TypeLists.OBJ!!, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, player.playerAppearance)
                                            if (renderedMediaModel == null && Component.missingMedia) {
                                                Component.redrawComponent(component)
                                            }
                                        }
                                    }
                                } else if (component.mediaType == MediaType.PLAYER_MODEL) {
                                    val playerIndex = component.primaryMediaId
                                    if (playerIndex >= 0 && playerIndex < 2048) {
                                        val player = PlayerUpdate.players[playerIndex]
                                        if (player != null && (playerIndex == PlayerUpdate.myPlayerIndex || player.displayName?.let(Utils::stringToInt) == component.secondaryMediaId)) {
                                            val appearance = player.playerAppearance
                                            if (appearance != null) {
                                                renderedMediaModel = appearance.getBodyModel(Renderers.activeToolkit!!, functionMask, TypeLists.BAS!!, TypeLists.IDK!!, TypeLists.NPC!!, TypeLists.OBJ!!, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, null, null, null, 0, TypeLists.EQUIPMENT_DEFAULTS!!)
                                            } else {
                                                Component.redrawComponent(component)
                                            }
                                        }
                                    }
                                } else if (component.mediaType != MediaType.ITEM_CONTAINER_MALE && component.mediaType != MediaType.ITEM_CONTAINER_FEMALE) {
                                    if (component.animatingSequence != null && component.animatingSequence!!.hasType()) {
                                        renderedMediaModel = component.getAnimatedMediaModel(Renderers.activeToolkit!!, functionMask, TypeLists.BAS!!, TypeLists.IDK!!, TypeLists.NPC!!, TypeLists.OBJ!!, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, PlayerUpdate.MY_PLAYER!!.playerAppearance)
                                    } else {
                                        renderedMediaModel = component.getAnimatedMediaModel(Renderers.activeToolkit!!, functionMask, TypeLists.BAS!!, TypeLists.IDK!!, TypeLists.NPC!!, TypeLists.OBJ!!, VarProviders.PLAYER_VAR_PROVIDER, null, PlayerUpdate.MY_PLAYER!!.playerAppearance)
                                    }
                                    if (renderedMediaModel == null && Component.missingMedia) {
                                        Component.redrawComponent(component)
                                    }
                                } else {
                                    val container = ItemContainer.getContainer(component.primaryMediaId, false)
                                    if (container != null) {
                                        renderedMediaModel = container.getAnimatedBodyModel(Renderers.activeToolkit!!, functionMask, component.animatingSequence, component.secondaryMediaId, component.mediaType == MediaType.ITEM_CONTAINER_FEMALE, if (component.wearCol) PlayerUpdate.MY_PLAYER!!.playerAppearance else null)
                                    }
                                }
                                if (renderedMediaModel != null) {
                                    if (component.modelTintScalar != 0) {
                                        renderedMediaModel.tint(component.modelTintRed, component.modelTintGreen, component.modelTintBlue, component.modelTintScalar)
                                    }
                                    val focalX: Int
                                    val focalY: Int
                                    if (component.aspectWidth > 0) {
                                        focalX = (component.width shl 9) / component.aspectWidth
                                    } else {
                                        focalX = 512
                                    }
                                    if (component.aspectHeight > 0) {
                                        focalY = (component.height shl 9) / component.aspectHeight
                                    } else {
                                        focalY = 512
                                    }
                                    val componentCenterX = component.width / 2 + startX
                                    val componentCenterY = component.height / 2 + startY
                                    var modelX = componentCenterX
                                    var modelY = componentCenterY
                                    if (!component.hasOrigin) {
                                        modelX += focalX * component.originX shr 9
                                        modelY += focalY * component.originY shr 9
                                    }
                                    Client.SUN_MATRIX.reset()
                                    Renderers.activeToolkit!!.setViewMatrix(Client.SUN_MATRIX)
                                    val perspectiveMatrix = Renderers.activeToolkit!!.getScratchMatrix()
                                    val viewRangeStart = Resource.mapRegion!!.viewRangeStart
                                    var viewRangeEnd = Resource.mapRegion!!.viewRangeEnd
                                    viewRangeEnd += component.spriteScale
                                    if (component.usesOrthogonal) {
                                        if (component.hasOrigin) {
                                            perspectiveMatrix!!.setPerspectiveFromViewport(modelX.toFloat(), modelY.toFloat(), focalX.toFloat(), focalY.toFloat(), viewRangeStart.toFloat(), viewRangeEnd.toFloat(), GameShell.screenCanvasWidth.toFloat(), (GameShell.screenCanvasHeight * -969250379).toFloat(), component.spriteScale.toFloat())
                                        } else {
                                            perspectiveMatrix!!.setPerspectiveFromViewport(modelX.toFloat(), modelY.toFloat(), focalX.toFloat(), focalY.toFloat(), viewRangeStart.toFloat(), viewRangeEnd.toFloat(), GameShell.screenCanvasWidth.toFloat(), (GameShell.screenCanvasHeight * -969250379).toFloat(), (component.spriteScale shl 2).toFloat())
                                        }
                                    } else {
                                        perspectiveMatrix!!.setPerspective(modelX.toFloat(), modelY.toFloat(), focalX.toFloat(), focalY.toFloat(), viewRangeStart.toFloat(), viewRangeEnd.toFloat(), GameShell.screenCanvasWidth.toFloat(), (GameShell.screenCanvasHeight * -969250379).toFloat())
                                    }
                                    Renderers.activeToolkit!!.setModelMatrix(perspectiveMatrix)
                                    Renderers.activeToolkit!!.applyClearFlags(2, 0)
                                    if (component.priorityRender) {
                                        Renderers.activeToolkit!!.setDepthBuffering(false)
                                    }
                                    if (component.hasOrigin) {
                                        Client.INTERFACE_MODEL_MATRIX.rotateMatrix(1.0f, 0.0f, 0.0f, MathUtils.getTheta(component.spritePitch))
                                        Client.INTERFACE_MODEL_MATRIX.rotate(0.0f, 1.0f, 0.0f, MathUtils.getTheta(component.spriteRoll))
                                        Client.INTERFACE_MODEL_MATRIX.rotate(0.0f, 0.0f, 1.0f, MathUtils.getTheta(component.spriteYaw))
                                        Client.INTERFACE_MODEL_MATRIX.translate(component.originX.toFloat(), component.originY.toFloat(), component.originZ.toFloat())
                                    } else {
                                        val pitchOffsetY = MathUtils.SINE[component.spritePitch shl 3] * (component.spriteScale shl 2) shr 14
                                        val pitchOffsetZ = MathUtils.COSINE[component.spritePitch shl 3] * (component.spriteScale shl 2) shr 14
                                        Client.INTERFACE_MODEL_MATRIX.rotateMatrix(0.0f, 0.0f, 1.0f, MathUtils.getTheta(-component.spriteYaw shl 3))
                                        Client.INTERFACE_MODEL_MATRIX.rotate(0.0f, 1.0f, 0.0f, MathUtils.getTheta(component.spriteRoll shl 3))
                                        Client.INTERFACE_MODEL_MATRIX.translate((component.offsetX shl 2).toFloat(), (pitchOffsetY + centerOffsetY + (component.offsetY shl 2)).toFloat(), (pitchOffsetZ + (component.offsetY shl 2)).toFloat())
                                        Client.INTERFACE_MODEL_MATRIX.rotate(1.0f, 0.0f, 0.0f, MathUtils.getTheta(component.spritePitch shl 3))
                                    }
                                    val toolkit = Renderers.activeToolkit
                                    component.cycleParticles(toolkit!!, renderedMediaModel, Client.INTERFACE_MODEL_MATRIX, Client.cycles)
                                    if (Client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit!!.expandClipping(startX, startY, startX + component.width, startY + component.height)
                                    }
                                    renderedMediaModel.drawMatrixThreaded(Client.INTERFACE_MODEL_MATRIX, null, ModelFunctionMaskGL.SMOOTH_EDGES)
                                    if (!component.usesOrthogonal && component.particleSystem != null) {
                                        Renderers.activeToolkit!!.renderParticles(component.particleSystem!!.rebuildRenderedParticles())
                                    }
                                    if (Client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                    }
                                    if (component.priorityRender) {
                                        Renderers.activeToolkit!!.setDepthBuffering(true)
                                    }
                                }
                            } else if (component.type == ComponentType.LINE) {
                                val lineStartX: Int
                                val lineStartY: Int
                                val lineEndX: Int
                                val lineEndY: Int
                                if (component.lineDirection) {
                                    lineStartX = startX
                                    lineStartY = startY + component.height
                                    lineEndX = startX + component.width
                                    lineEndY = startY
                                } else {
                                    lineStartX = startX
                                    lineStartY = startY
                                    lineEndX = startX + component.width
                                    lineEndY = startY + component.height
                                }
                                if (component.lineWidth == 1) {
                                    Renderers.activeToolkit!!.drawTransparentLine(lineStartX, lineStartY, lineEndX, lineEndY, component.color, 0)
                                } else {
                                    Renderers.activeToolkit!!.drawThickLine(lineStartX, lineStartY, lineEndX, lineEndY, component.color, component.lineWidth, 0)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun updateEntityOnInterface(components: Array<Component?>, component: Component, fireResizeHooks: Boolean) {
        val componentWidth = if (component.scrollWidth != 0) component.scrollWidth else component.width
        val componentHeight = if (component.scrollHeight != 0) component.scrollHeight else component.height
        InterfaceRenderer.updateInterfaceDimensions(components, component.idHash, componentWidth, componentHeight, fireResizeHooks)
        val itemSlots = component.itemSlots
        if (itemSlots != null) {
            InterfaceRenderer.updateInterfaceDimensions(itemSlots, component.idHash, componentWidth, componentHeight, fireResizeHooks)
        }
        val subInterface = Client.OPEN_INTERFACES!!.get(component.idHash.toLong())
        if (subInterface != null) {
            InterfaceRenderer.updateInterfaceDimensions(subInterface.subInterfaceId, componentWidth, componentHeight, fireResizeHooks)
        }
        if (component === Client.GAME_SCREEN_INTERFACE) {
            val playerCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES
            val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES
            for (index in 0 until playerCount) {
                val playerEntity = PlayerUpdate.players[playerIndices[index]]
                if (playerEntity != null) {
                    playerEntity.updateInterfaceDisplay(componentWidth, componentHeight, fireResizeHooks)
                }
            }
            for (npcUpdateIndex in 0 until Client.NPC_UPDATE_INDEX) {
                val updateKey = Client.NPC_UPDATE_INDICES[npcUpdateIndex]
                val npcObjectNode = Client.NPC_MAP.get(updateKey.toLong())
                if (npcObjectNode != null) {
                    (npcObjectNode.value as PathingEntity).updateInterfaceDisplay(componentWidth, componentHeight, fireResizeHooks)
                }
            }
        }
    }

    @JvmStatic
    fun componentPositionAdjustment(component: Component, parentWidth: Int, parentHeight: Int) {
        if (component.aspectXType.toInt() == 0) {
            component.x = component.basePositionX
        } else if (component.aspectXType.toInt() == 1) {
            component.x = component.basePositionX + (parentWidth - component.width) / 2
        } else if (component.aspectXType.toInt() == 2) {
            component.x = parentWidth - component.width - component.basePositionX
        } else if (component.aspectXType.toInt() == 3) {
            component.x = component.basePositionX * parentWidth shr 14
        } else if (component.aspectXType.toInt() == 4) {
            component.x = (parentWidth - component.width) / 2 + (component.basePositionX * parentWidth shr 14)
        } else {
            component.x = parentWidth - component.width - (component.basePositionX * parentWidth shr 14)
        }
        if (component.aspectYType.toInt() == 0) {
            component.y = component.basePositionY
        } else if (component.aspectYType.toInt() == 1) {
            component.y = (parentHeight - component.height) / 2 + component.basePositionY
        } else if (component.aspectYType.toInt() == 2) {
            component.y = parentHeight - component.height - component.basePositionY
        } else if (component.aspectYType.toInt() == 3) {
            component.y = parentHeight * component.basePositionY shr 14
        } else if (component.aspectYType.toInt() == 4) {
            component.y = (parentHeight - component.height) / 2 + (parentHeight * component.basePositionY shr 14)
        } else {
            component.y = parentHeight - component.height - (parentHeight * component.basePositionY shr 14)
        }
        if (Client.DISPLAY_HIDDEN_MENU_OPTIONS && (Client.getComponentSettings(component)!!.settingsHash != 0 || component.type == ComponentType.CONTAINER)) {
            if (component.x < 0) {
                component.x = 0
            } else if (component.x + component.width > parentWidth) {
                component.x = parentWidth - component.width
            }
            if (component.y < 0) {
                component.y = 0
            } else if (component.height + component.y > parentHeight) {
                component.y = parentHeight - component.height
            }
        }
    }

    @JvmStatic
    fun drawMinimapText(component: Component, dimension: ComponentSurface?, clipLeft: Int, clipTop: Int, offsetX: Int, offsetZ: Int, iconHeight: Int, text: String?, fontRenderer: AbstractFontRenderer?, fontMetrics: FontMetrics?, color: Int) {
        val angle: Int
        if (Camera.cameraCalculationType == 4) {
            angle = Camera.camAngleY.toInt() and 0x3fff
        } else {
            angle = Camera.camAngleY.toInt() + Client.minimapRotation and 0x3fff
        }
        val maxDistance = Math.max(component.width / 2, component.height / 2) + 10
        val distanceSq = offsetZ * offsetZ + offsetX * offsetX
        if (distanceSq <= maxDistance * maxDistance) {
            var sin = MathUtils.SINE[angle]
            var cos = MathUtils.COSINE[angle]
            if (Camera.cameraCalculationType != 4) {
                sin = sin * 256 / (Client.minimapZoom + 256)
                cos = cos * 256 / (Client.minimapZoom + 256)
            }
            val mapX = cos * offsetX + offsetZ * sin shr 14
            val mapY = offsetZ * cos - sin * offsetX shr 14
            val textWidth = fontMetrics!!.getTextWidthMultiline(text!!, 100, null)
            val textHeight = fontMetrics.getTextHeightMultiline(text, 100, 0, null)
            var textX = mapX - textWidth / 2
            if (textX >= -component.width && textX <= component.width && mapY >= -component.height && mapY <= component.height) {
                fontRenderer!!.drawText(text, textX + component.width / 2 + clipLeft, clipTop + component.height / 2 - mapY - iconHeight - textHeight, textWidth, 50, color, 0, 1, 0, 0, null, null, dimension, clipLeft, clipTop)
            }
        }
    }

    @JvmStatic
    fun inboundsComponentToTop(x: Int, y: Int, width: Int, height: Int) {
        for (index in 0 until Client.componentBackgroundCount) {
            val rectangle = Client.INTERFACE_BG_COMPONENTS[index] ?: continue
            if (rectangle.x + rectangle.width > x && rectangle.x < x + width && rectangle.height + rectangle.y > y && rectangle.y < height + y) {
                Client.IS_COMPONENT_ON_TOP[index] = true
            }
        }
    }

    @JvmStatic
    fun setChatDisplay(component: Component?, x: Int, y: Int) {
        chatDisplayComponent = component
        chatDisplayX = x
        chatDisplayY = y
    }

    @JvmStatic
    fun renderItems(toolkit: AbstractToolkit?) {
        if (toolkit == null) return
        if (!Component.ITEMS.isEmpty()) {
            if (PreferenceManager.preferences!!.activeToolkit!!.getValue() == 0) {
                var item = Component.ITEMS.getFirst() as Item?
                while (item != null) {
                    TypeLists.OBJ!!.getSprite(toolkit, toolkit, item.id, item.amount, item.outlineSize, item.shadowColor, false, false, item.stackMode, Renderers.FONT_RENDERER!!, if (item.hasPlayerAppearance) PlayerUpdate.MY_PLAYER!!.playerAppearance else null)
                    item.unlink()
                    item = Component.ITEMS.next() as Item?
                }
                GameShell.setComponentsOnTop()
            } else {
                if (Renderers.GPU_ITEM_RENDERER == null) {
                    val itemCanvas = Canvas()
                    itemCanvas.setSize(36, 32)
                    Renderers.GPU_ITEM_RENDERER = AbstractToolkit.create(0, itemCanvas, TypeLists.MATERIALS, Resource.SHADER, 0)
                    Renderers.GPU_ITEM_FONT_RENDERER = Renderers.GPU_ITEM_RENDERER!!.createFontRenderer(FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p11FullIndex), SpriteType.decodeGroup(Resource.SPRITES, FontManager.p11FullIndex), true)
                }
                var item = Component.ITEMS.getFirst() as Item?
                while (item != null) {
                    TypeLists.OBJ!!.getSprite(Renderers.GPU_ITEM_RENDERER!!, toolkit, item.id, item.amount, item.outlineSize, item.shadowColor, false, false, item.stackMode, Renderers.GPU_ITEM_FONT_RENDERER!!, if (item.hasPlayerAppearance) PlayerUpdate.MY_PLAYER!!.playerAppearance else null)
                    item.unlink()
                    item = Component.ITEMS.next() as Item?
                }
            }
        }
    }
}
