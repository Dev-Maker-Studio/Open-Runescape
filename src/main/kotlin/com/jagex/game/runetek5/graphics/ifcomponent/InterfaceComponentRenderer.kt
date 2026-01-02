package com.jagex.game.runetek5.graphics.ifcomponent

import java.awt.Canvas
import java.awt.Rectangle

import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.midi.VorbisNode
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
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
import com.jagex.game.runetek5.math.Matrix44
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

    @JvmField var aClass118Array3772: Array<Component?>? = null
    @JvmField var anInt4822: Int = 0
    @JvmField var aClass118183: Component? = null
    @JvmField var anInt195: Int = -1
    @JvmField var anInt179: Int = -1
    @JvmField var anInt5157: Int = 0
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
        if (client.interfaceSelectionActive) {
            val component = Component.listIcomponent(client.anInt56, client.anInt7345)
            if (component != null && component.hookParams != null) {
                val hookRequest = HookRequest()
                hookRequest.source = component
                hookRequest.params = component.hookParams
                CS2Executor.executeHookInner(hookRequest)
            }
            client.defaultMenuCursor = -1
            client.anInt7346 = -1
            client.interfaceSelectionActive = false
            if (component != null) {
                Component.redrawComponent(component)
            }
        }
    }

    @JvmStatic
    fun drawObjSprite(toolkit: AbstractToolkit, component: Component) {
        val created = TypeLists.OBJ!!.createNativeSprite(toolkit, component.containerItemId, component.itemStackSize, component.borderThickness, -0x1000000 or component.spriteShadow, component.renderStack, if (component.wearCol) PlayerUpdate.MY_PLAYER!!.playerAppearance else null) == null
        if (created) {
            Component.ITEMS.addFirst(Item(component.containerItemId, component.itemStackSize, component.borderThickness, -0x1000000 or component.spriteShadow, component.renderStack, component.wearCol))
            Component.redrawComponent(component)
        }
    }

    @JvmStatic
    fun drawComponents(components: Array<Component?>, i_1: Int, clippingLeft: Int, clippingTop: Int, clippingRight: Int, clippingBottom: Int, offsetX: Int, offsetY: Int, i_8: Int, bool9: Boolean) {
        Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
        for (componentIndex in components.indices) {
            val component = components[componentIndex]
            if (component != null && (component.parentId == i_1 || i_1 == -1412584499 && component === client.draggingComponent)) {
                val componentReference: Int
                if (i_8 == -1) {
                    client.INTERFACE_BG_COMPONENTS[client.anInt7407].setBounds(component.x + offsetX, component.y + offsetY, component.width, component.height)
                    componentReference = ++client.anInt7407 - 1
                } else {
                    componentReference = i_8
                }
                component.cacheReference = componentReference
                component.lastRenderedCycle = client.cycles
                if (!InterfaceRenderer.isComponentHidden(component)) {
                    if (component.contentType != null) {
                        Component.prepareSequenceOnInterface(component)
                    }
                    val drawX = component.x + offsetX
                    val drawY = component.y + offsetY
                    var drawTransparency = component.transparency
                    if (client.DISPLAY_HIDDEN_MENU_OPTIONS && (client.getComponentSettings(component).settingsHash != 0 || component.type == ComponentType.CONTAINER) && drawTransparency > 127) {
                        drawTransparency = 127
                    }
                    val startX: Int
                    val startY: Int
                    if (component === client.draggingComponent) {
                        if (i_1 != -1412584499 && (component.anUnkSetting11 == Component.UnkSetting1.UNK_2 || component.anUnkSetting11 == Component.UnkSetting1.UNK_0 || client.getComponentSettings(component).ignoresDepthFlags())) {
                            aClass118Array3772 = components
                            anInt4822 = offsetX
                            anInt5157 = offsetY
                            continue
                        }
                        if (client.aBool7364 && client.isComponentSelected) {
                            var dragX = MouseRecord.recorder!!.getMouseX()
                            var dragY = MouseRecord.recorder!!.getMouseY()
                            dragX -= client.mouseX
                            dragY -= client.mouseY
                            if (dragX < client.selectedComponentX) {
                                dragX = client.selectedComponentX
                            }
                            if (dragX + component.width > client.selectedComponentX + client.selectedComponentWidth) {
                                dragX = client.selectedComponentX + client.selectedComponentWidth - component.width
                            }
                            if (dragY < client.selectedComponentY) {
                                dragY = client.selectedComponentY
                            }
                            if (dragY + component.height > client.selectedComponentY + client.selectedComponentHeight) {
                                dragY = client.selectedComponentHeight + client.selectedComponentY - component.height
                            }
                            if (client.getComponentSettings(component).ignoresDepthFlags()) {
                                inboundsComponentToTop(dragX, dragY, component.width, component.height)
                            }
                            startX = dragX
                            startY = dragY
                        } else {
                            startX = drawX
                            startY = drawY
                        }
                        if (component.anUnkSetting11 == Component.UnkSetting1.UNK_0) {
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
                        var lineStartX = startX + component.width
                        var lineStartY = startY + component.height
                        if (component.type == ComponentType.LINE) {
                            lineStartX++
                            lineStartY++
                        }
                        endX = Math.min(lineStartX, clippingRight)
                        endY = Math.min(lineStartY, clippingBottom)
                    }
                    val clipStartX = if (component.type == ComponentType.TYPE_2) clippingLeft else Math.max(startX, clippingLeft)
                    val clipStartY = if (component.type == ComponentType.TYPE_2) clippingTop else Math.max(startY, clippingTop)
                    if (clipStartX < endX && clipStartY < endY) {
                        if (component.contentType != null) {
                            if (component.contentType == GAME_WINDOW || component.contentType == CONTENT_TYPE_1403) {
                                client.drawGameArea(startX, startY, component.width, component.height, component.contentType == CONTENT_TYPE_1403)
                                EntityUpdating.method12366(componentReference, clipStartX, clipStartY, endX, endY, startX, startY)
                                Renderers.activeToolkit!!.calculateViewportCenter()
                                Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                client.IS_COMPONENT_ON_TOP[componentReference] = true
                                continue
                            }
                            if (component.contentType == MINIMAP && client.MAP_LOADING_STAGE == 1) {
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
                                client.IS_COMPONENT_ON_TOP[componentReference] = true
                                Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                continue
                            }
                            if (component.contentType == WORLD_MAP_OVERVIEW) {
                                WorldMapOverview.drawWorldmapOverview(Renderers.activeToolkit!!, startX, startY, component.width, component.height)
                                client.IS_COMPONENT_ON_TOP[componentReference] = true
                                Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                continue
                            }
                            if (component.contentType == DEBUG_INFORMATION) {
                                if (client.DRAW_DEBUG || client.DEBUG_MODE) {
                                    SystemInfo.drawDebugInformation(startX, startY, component)
                                    client.IS_COMPONENT_ON_TOP[componentReference] = true
                                }
                                continue
                            }
                        }

                        if (component.type == ComponentType.CONTAINER) {
                            if (component.contentType == CUTSCENE_FADE && Renderers.activeToolkit!!.bloomEnabled()) {
                                Renderers.activeToolkit!!.method8525(GameShell.screenCanvasWidth, GameShell.screenCanvasHeight * -969250379)
                            }
                            drawComponents(components, component.idHash, clipStartX, clipStartY, endX, endY, startX - component.scrollX, startY - component.scrollY, componentReference, bool9)
                            val itemSlots = component.itemSlots
                            if (itemSlots != null) {
                                drawComponents(itemSlots, component.idHash, clipStartX, clipStartY, endX, endY, startX - component.scrollX, startY - component.scrollY, componentReference, bool9)
                            }
                            val subInterface = client.OPEN_INTERFACES.get(component.idHash.toLong())
                            if (subInterface != null) {
                                Component.method3629(subInterface.subInterfaceId, clipStartX, clipStartY, endX, endY, startX, startY, componentReference)
                            }
                            if (component.contentType == CUTSCENE_FADE) {
                                if (Renderers.activeToolkit!!.bloomEnabled()) {
                                    Renderers.activeToolkit!!.method8474()
                                }
                                if (client.MAP_LOADING_STAGE == 4) {
                                    var fadeAlpha = cutsceneFadeAlpha
                                    var fadeRed = cutsceneFadeRed
                                    var fadeGreen = cutsceneFadeGreen
                                    var fadeBlue = cutsceneFadeBlue
                                    if (client.cycles < cutsceneFadeEndCycle) {
                                        val completion = (client.cycles - cycleCutsceneFadeStarted) * 1.0f / (cutsceneFadeEndCycle - cycleCutsceneFadeStarted)
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
                        if (client.PREV_COMPONENT_ON_TOP[componentReference] || client.anInt7412 > 1) {
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
                                val fontRenderer = component.getFontRenderer(StaticMethods.initializedFontcombos!!, client.FONT_PROVIDER)
                                if (fontRenderer == null) {
                                    if (Component.missingMedia) {
                                        Component.redrawComponent(component)
                                    }
                                } else {
                                    var color = component.color
                                    var itemDisplayText = component.text
                                    if (component.containerItemId != -1) {
                                        val objType = TypeLists.OBJ!!.list(component.containerItemId)
                                        itemDisplayText = objType!!.name
                                        if (itemDisplayText == null) {
                                            itemDisplayText = "null"
                                        }
                                        if ((objType.stackable == 1 || component.itemStackSize != 1) && component.itemStackSize != -1) {
                                            itemDisplayText = TextUtils.setTextColor(16748608) + itemDisplayText + "</col>" + " x" + TextUtils.formatStackSize(component.itemStackSize)
                                        }
                                    }
                                    if (component.anInt1435 != -1) {
                                        itemDisplayText = SoundResourceManager.method2550(component.anInt1435)!!
                                    }
                                    if (component === client.aClass1187352) {
                                        itemDisplayText = LocalizedText.PLEASE_WAIT.translate(client.CURRENT_LANGUAGE)
                                        color = component.color
                                    }
                                    if (client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit!!.expandClipping(startX, startY, startX + component.width, startY + component.height)
                                    }
                                    if (component.textAntiMacro) {
                                        fontRenderer.renderTextAntiMacro(itemDisplayText!!, startX, startY, component.width, component.height, 255 - (drawTransparency and 0xff) shl 24 or color, if (component.shadow) 255 - (drawTransparency and 0xff) shl 24 else -1, component.textHorizontalAli, component.textVerticalAli, client.RANDOMIZER, client.randomSeed, client.anIntArray7438, StaticMedia.nameIconSprites, null)
                                    } else {
                                        fontRenderer.renderText(itemDisplayText!!, startX, startY, component.width, component.height, 255 - (drawTransparency and 0xff) shl 24 or color, if (component.shadow) 255 - (drawTransparency and 0xff) shl 24 else -1, component.textHorizontalAli, component.textVerticalAli, component.anInt1358, component.maxTextLines, StaticMedia.nameIconSprites, null, null, 0, 0)
                                    }
                                    if (client.CLIP_INTERFACES) {
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
                                    } else if (component.anInt1435 != -1) {
                                        sprite = VorbisNode.method8858(Renderers.activeToolkit!!, component.anInt1435)
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
                                                val height = (spriteWidth - 1 + component.width) / spriteWidth
                                                val i_39 = (spriteHeight - 1 + component.height) / spriteHeight
                                                for (i_27 in 0 until height) {
                                                    for (i_28 in 0 until i_39) {
                                                        if (component.color != 0) {
                                                            sprite.method2790((startX + i_27 * spriteWidth) + spriteWidth / 2.0f, spriteHeight / 2.0f + (startY + spriteHeight * i_28), 4096, component.angle2d, drawColor)
                                                        } else {
                                                            sprite.method2758((startX + spriteWidth * i_27) + spriteWidth / 2.0f, (startY + spriteHeight * i_28) + spriteHeight / 2.0f, 4096, component.angle2d)
                                                        }
                                                    }
                                                }
                                            } else if (component.color == 0 && drawTransparency == 0) {
                                                sprite.method2756(startX, startY, component.width, component.height)
                                            } else {
                                                sprite.method2772(startX, startY, component.width, component.height, 0, drawColor, 1)
                                            }
                                            Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                        } else if (component.color == 0 && drawTransparency == 0) {
                                            if (component.angle2d != 0) {
                                                sprite.method2758(component.width / 2.0f + startX, startY + component.height / 2.0f, component.width * 4096 / spriteWidth, component.angle2d)
                                            } else if (spriteWidth == component.width && spriteHeight == component.height) {
                                                sprite.draw(startX, startY)
                                            } else {
                                                sprite.drawCircle(startX, startY, component.width, component.height)
                                            }
                                        } else if (component.angle2d != 0) {
                                            sprite.method2790(component.width / 2.0f + startX, component.height / 2.0f + startY, component.width * 4096 / spriteWidth, component.angle2d, drawColor)
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
                                var centerY = 0
                                if (component.containerItemId != -1) {
                                    val objType = TypeLists.OBJ!!.list(component.containerItemId)
                                    if (objType != null) {
                                        val stackAltType = objType.getStackSizeAlt(component.itemStackSize)
                                        renderedMediaModel = stackAltType.getAnimatedModel(Renderers.activeToolkit!!, functionMask, 1, if (component.wearCol) PlayerUpdate.MY_PLAYER!!.playerAppearance else null, component.animatingSequence, 0, 0, 0, 0)
                                        if (renderedMediaModel != null) {
                                            centerY = -renderedMediaModel.getMinY() shr 1
                                        } else {
                                            Component.redrawComponent(component)
                                        }
                                    }
                                } else if (component.mediaType == MediaType.PLAYER_HEAD) {
                                    val lineHeight = component.primaryMediaId
                                    if (lineHeight >= 0 && lineHeight < 2048) {
                                        val player = PlayerUpdate.players[lineHeight]
                                        if (player != null && (lineHeight == PlayerUpdate.myPlayerIndex || Utils.stringToInt(player.displayName!!) == component.secondaryMediaId)) {
                                            val toolkit = Renderers.activeToolkit
                                            renderedMediaModel = component.getAnimatedMediaModel(toolkit!!, functionMask, TypeLists.BAS!!, TypeLists.IDK!!, TypeLists.NPC!!, TypeLists.OBJ!!, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, player.playerAppearance)
                                            if (renderedMediaModel == null && Component.missingMedia) {
                                                Component.redrawComponent(component)
                                            }
                                        }
                                    }
                                } else if (component.mediaType == MediaType.PLAYER_MODEL) {
                                    val target = component.primaryMediaId
                                    if (target >= 0 && target < 2048) {
                                        val player = PlayerUpdate.players[target]
                                        if (player != null && (target == PlayerUpdate.myPlayerIndex || Utils.stringToInt(player.displayName!!) == component.secondaryMediaId)) {
                                            renderedMediaModel = player.playerAppearance!!.getBodyModel(Renderers.activeToolkit!!, functionMask, TypeLists.BAS!!, TypeLists.IDK!!, TypeLists.NPC!!, TypeLists.OBJ!!, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, null, null, null, 0, TypeLists.EQUIPMENT_DEFAULTS!!)
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
                                    val width: Int
                                    val height: Int
                                    if (component.aspectWidth > 0) {
                                        width = (component.width shl 9) / component.aspectWidth
                                    } else {
                                        width = 512
                                    }
                                    if (component.aspectHeight > 0) {
                                        height = (component.height shl 9) / component.aspectHeight
                                    } else {
                                        height = 512
                                    }
                                    val i_39 = component.width / 2 + startX
                                    val i_27 = component.height / 2 + startY
                                    var modelX = i_39
                                    var modelY = i_27
                                    if (!component.hasOrigin) {
                                        modelX += width * component.originX shr 9
                                        modelY += height * component.originY shr 9
                                    }
                                    client.SUN_MATRIX.reset()
                                    Renderers.activeToolkit!!.setSunPosition(client.SUN_MATRIX)
                                    val matrix44 = Renderers.activeToolkit!!.method8449()
                                    val viewRangeStart = Resource.mapRegion!!.viewRangeStart
                                    var viewRangeEnd = Resource.mapRegion!!.viewRangeEnd
                                    viewRangeEnd += component.spriteScale
                                    if (component.usesOrthogonal) {
                                        if (component.hasOrigin) {
                                            matrix44!!.method6532(modelX.toFloat(), modelY.toFloat(), width.toFloat(), height.toFloat(), viewRangeStart.toFloat(), viewRangeEnd.toFloat(), GameShell.screenCanvasWidth.toFloat(), (GameShell.screenCanvasHeight * -969250379).toFloat(), component.spriteScale.toFloat())
                                        } else {
                                            matrix44!!.method6532(modelX.toFloat(), modelY.toFloat(), width.toFloat(), height.toFloat(), viewRangeStart.toFloat(), viewRangeEnd.toFloat(), GameShell.screenCanvasWidth.toFloat(), (GameShell.screenCanvasHeight * -969250379).toFloat(), (component.spriteScale shl 2).toFloat())
                                        }
                                    } else {
                                        matrix44!!.method6531(modelX.toFloat(), modelY.toFloat(), width.toFloat(), height.toFloat(), viewRangeStart.toFloat(), viewRangeEnd.toFloat(), GameShell.screenCanvasWidth.toFloat(), (GameShell.screenCanvasHeight * -969250379).toFloat())
                                    }
                                    Renderers.activeToolkit!!.setModelMatrix(matrix44)
                                    Renderers.activeToolkit!!.applyClearFlags(2, 0)
                                    if (component.priorityRender) {
                                        Renderers.activeToolkit!!.setDepthBuffering(false)
                                    }
                                    if (component.hasOrigin) {
                                        client.INTERFACE_MODEL_MATRIX.rotateMatrix(1.0f, 0.0f, 0.0f, MathUtils.getTheta(component.spritePitch))
                                        client.INTERFACE_MODEL_MATRIX.rotate(0.0f, 1.0f, 0.0f, MathUtils.getTheta(component.spriteRoll))
                                        client.INTERFACE_MODEL_MATRIX.rotate(0.0f, 0.0f, 1.0f, MathUtils.getTheta(component.spriteYaw))
                                        client.INTERFACE_MODEL_MATRIX.translate(component.originX.toFloat(), component.originY.toFloat(), component.originZ.toFloat())
                                    } else {
                                        val i_31 = MathUtils.SINE[component.spritePitch shl 3] * (component.spriteScale shl 2) shr 14
                                        val i_32 = MathUtils.COSINE[component.spritePitch shl 3] * (component.spriteScale shl 2) shr 14
                                        client.INTERFACE_MODEL_MATRIX.rotateMatrix(0.0f, 0.0f, 1.0f, MathUtils.getTheta(-component.spriteYaw shl 3))
                                        client.INTERFACE_MODEL_MATRIX.rotate(0.0f, 1.0f, 0.0f, MathUtils.getTheta(component.spriteRoll shl 3))
                                        client.INTERFACE_MODEL_MATRIX.translate((component.offsetX shl 2).toFloat(), (i_31 + centerY + (component.offsetY shl 2)).toFloat(), (i_32 + (component.offsetY shl 2)).toFloat())
                                        client.INTERFACE_MODEL_MATRIX.rotate(1.0f, 0.0f, 0.0f, MathUtils.getTheta(component.spritePitch shl 3))
                                    }
                                    val toolkit = Renderers.activeToolkit
                                    component.cycleParticles(toolkit!!, renderedMediaModel, client.INTERFACE_MODEL_MATRIX, client.cycles)
                                    if (client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit!!.expandClipping(startX, startY, startX + component.width, startY + component.height)
                                    }
                                    renderedMediaModel.drawMatrixThreaded(client.INTERFACE_MODEL_MATRIX, null, ModelFunctionMaskGL.SMOOTH_EDGES)
                                    if (!component.usesOrthogonal && component.particleSystem != null) {
                                        Renderers.activeToolkit!!.renderParticles(component.particleSystem!!.rebuildRenderedParticles())
                                    }
                                    if (client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit!!.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom)
                                    }
                                    if (component.priorityRender) {
                                        Renderers.activeToolkit!!.setDepthBuffering(true)
                                    }
                                }
                            } else if (component.type == ComponentType.LINE) {
                                val lineStartX: Int
                                val lineStartY: Int
                                val lineWidth: Int
                                val lineHeight: Int
                                if (component.lineDirection) {
                                    lineStartX = startX
                                    lineStartY = startY + component.height
                                    lineWidth = startX + component.width
                                    lineHeight = startY
                                } else {
                                    lineStartX = startX
                                    lineStartY = startY
                                    lineWidth = startX + component.width
                                    lineHeight = startY + component.height
                                }
                                if (component.lineWidth == 1) {
                                    Renderers.activeToolkit!!.drawTransparentLine(lineStartX, lineStartY, lineWidth, lineHeight, component.color, 0)
                                } else {
                                    Renderers.activeToolkit!!.drawThickLine(lineStartX, lineStartY, lineWidth, lineHeight, component.color, component.lineWidth, 0)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun updateEntityOnInterface(components: Array<Component?>, component: Component, bool2: Boolean) {
        val componentWidth = if (component.scrollWidth != 0) component.scrollWidth else component.width
        val componentHeight = if (component.scrollHeight != 0) component.scrollHeight else component.height
        InterfaceRenderer.updateInterfaceDimensions(components, component.idHash, componentWidth, componentHeight, bool2)
        val itemSlots = component.itemSlots
        if (itemSlots != null) {
            InterfaceRenderer.updateInterfaceDimensions(itemSlots, component.idHash, componentWidth, componentHeight, bool2)
        }
        val subInterface = client.OPEN_INTERFACES.get(component.idHash.toLong())
        if (subInterface != null) {
            InterfaceRenderer.updateInterfaceDimensions(subInterface.subInterfaceId, componentWidth, componentHeight, bool2)
        }
        if (component === client.GAME_SCREEN_INTERFACE) {
            val playerIndiceCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES
            val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES
            for (index in 0 until playerIndiceCount) {
                val playerEntity = PlayerUpdate.players[playerIndices[index]]
                if (playerEntity != null) {
                    playerEntity.updateInterfaceDisplay(componentWidth, componentHeight, bool2)
                }
            }
            for (npcUpdateIndex in 0 until client.NPC_UPDATE_INDEX) {
                val updateKey = client.NPC_UPDATE_INDICES[npcUpdateIndex]
                val npcObjectNode = client.NPC_MAP.get(updateKey.toLong())
                if (npcObjectNode != null) {
                    (npcObjectNode.value as PathingEntity).updateInterfaceDisplay(componentWidth, componentHeight, bool2)
                }
            }
        }
    }

    @JvmStatic
    fun componentPositionAdjustment(component: Component, x: Int, y: Int) {
        if (component.aspectXType.toInt() == 0) {
            component.x = component.basePositionX
        } else if (component.aspectXType.toInt() == 1) {
            component.x = component.basePositionX + (x - component.width) / 2
        } else if (component.aspectXType.toInt() == 2) {
            component.x = x - component.width - component.basePositionX
        } else if (component.aspectXType.toInt() == 3) {
            component.x = component.basePositionX * x shr 14
        } else if (component.aspectXType.toInt() == 4) {
            component.x = (x - component.width) / 2 + (component.basePositionX * x shr 14)
        } else {
            component.x = x - component.width - (component.basePositionX * x shr 14)
        }
        if (component.aspectYType.toInt() == 0) {
            component.y = component.basePositionY
        } else if (component.aspectYType.toInt() == 1) {
            component.y = (y - component.height) / 2 + component.basePositionY
        } else if (component.aspectYType.toInt() == 2) {
            component.y = y - component.height - component.basePositionY
        } else if (component.aspectYType.toInt() == 3) {
            component.y = y * component.basePositionY shr 14
        } else if (component.aspectYType.toInt() == 4) {
            component.y = (y - component.height) / 2 + (y * component.basePositionY shr 14)
        } else {
            component.y = y - component.height - (y * component.basePositionY shr 14)
        }
        if (client.DISPLAY_HIDDEN_MENU_OPTIONS && (client.getComponentSettings(component).settingsHash != 0 || component.type == ComponentType.CONTAINER)) {
            if (component.x < 0) {
                component.x = 0
            } else if (component.x + component.width > x) {
                component.x = x - component.width
            }
            if (component.y < 0) {
                component.y = 0
            } else if (component.height + component.y > y) {
                component.y = y - component.height
            }
        }
    }

    @JvmStatic
    fun method4152(component: Component, dimension: ComponentSurface?, i_2: Int, i_3: Int, i_4: Int, i_5: Int, i_6: Int, string_7: String?, fontRenderer: AbstractFontRenderer?, fontmetrics9: FontMetrics?, i_10: Int) {
        val angle: Int
        if (Camera.cameraCalculationType == 4) {
            angle = Camera.camAngleY.toInt() and 0x3fff
        } else {
            angle = Camera.camAngleY.toInt() + client.minimapRotation and 0x3fff
        }
        val i_13 = Math.max(component.width / 2, component.height / 2) + 10
        val i_14 = i_5 * i_5 + i_4 * i_4
        if (i_14 <= i_13 * i_13) {
            var sin = MathUtils.SINE[angle]
            var cos = MathUtils.COSINE[angle]
            if (Camera.cameraCalculationType != 4) {
                sin = sin * 256 / (client.minimapZoom + 256)
                cos = cos * 256 / (client.minimapZoom + 256)
            }
            val i_17 = cos * i_4 + i_5 * sin shr 14
            val i_18 = i_5 * cos - sin * i_4 shr 14
            val i_19 = fontmetrics9!!.getTextWidthMultiline(string_7!!, 100, null)
            val i_20 = fontmetrics9.getTextHeightMultiline(string_7, 100, 0, null)
            var textX = i_17 - i_19 / 2
            if (textX >= -component.width && textX <= component.width && i_18 >= -component.height && i_18 <= component.height) {
                fontRenderer!!.drawText(string_7, textX + component.width / 2 + i_2, i_3 + component.height / 2 - i_18 - i_6 - i_20, i_19, 50, i_10, 0, 1, 0, 0, null, null, dimension, i_2, i_3)
            }
        }
    }

    @JvmStatic
    fun inboundsComponentToTop(x: Int, y: Int, offsetX: Int, offsetY: Int) {
        for (index in 0 until client.anInt7407) {
            val rectangle = client.INTERFACE_BG_COMPONENTS[index]
            if (rectangle.x + rectangle.width > x && rectangle.x < x + offsetX && rectangle.height + rectangle.y > y && rectangle.y < offsetY + y) {
                client.IS_COMPONENT_ON_TOP[index] = true
            }
        }
    }

    @JvmStatic
    fun method544(component: Component?, x: Int, y: Int) {
        aClass118183 = component
        anInt195 = x
        anInt179 = y
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
