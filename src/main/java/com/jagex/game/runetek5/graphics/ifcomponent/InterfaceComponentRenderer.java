package com.jagex.game.runetek5.graphics.ifcomponent;

import java.awt.Canvas;
import java.awt.Rectangle;

import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.PreferenceManager;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.camera.Camera;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.midi.VorbisNode;
import com.jagex.game.runetek5.config.item.ItemType;
import com.jagex.game.runetek5.config.rsinterface.Component;
import com.jagex.game.runetek5.config.rsinterface.ComponentSurface;
import com.jagex.game.runetek5.config.rsinterface.ComponentType;
import com.jagex.game.runetek5.config.rsinterface.HookRequest;
import com.jagex.game.runetek5.config.rsinterface.SubInterface;
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor;
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer;
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating;
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerAppearance;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontManager;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap;
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapOverview;
import com.jagex.game.runetek5.graphics.model.Model;
import com.jagex.game.runetek5.graphics.model.ModelFunctionMaskGL;
import com.jagex.game.runetek5.graphics.model.MediaType;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.SpriteType;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.math.Matrix44;
import com.jagex.game.runetek5.rendering.Renderers;
import com.jagex.game.runetek5.sound.SoundResourceManager;
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.systeminfo.SystemInfo;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.MathUtils;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.utils.Utils;
import com.jagex.game.runetek5.vardomain.VarProviders;

import static com.jagex.game.runetek5.config.rsinterface.ComponentContentType.*;

public class InterfaceComponentRenderer {

	public static void cancelInterfaceSelection() {
	    if (client.interfaceSelectionActive) {
	        Component component = Component.listIcomponent(client.anInt56, client.anInt7345);
	        if (component != null && component.hookParams != null) {
	            HookRequest hookRequest = new HookRequest();
	            hookRequest.source = component;
	            hookRequest.params = component.hookParams;
	            CS2Executor.executeHookInner(hookRequest);
	        }
	        client.defaultMenuCursor = -1;
	        client.anInt7346 = -1;
	        client.interfaceSelectionActive = false;
	        if (component != null) {
	            Component.redrawComponent(component);
	        }
	    }
	}

	public static void drawObjSprite(AbstractToolkit toolkit, Component component) {
	    boolean created = TypeLists.OBJ.createNativeSprite(toolkit, component.containerItemId, component.itemStackSize, component.borderThickness, -0x1000000 | component.spriteShadow, component.renderStack, component.wearCol ? PlayerUpdate.MY_PLAYER.playerAppearance : null) == null;
	    if (created) {
	        Component.ITEMS.addFirst(new Item(component.containerItemId, component.itemStackSize, component.borderThickness, -0x1000000 | component.spriteShadow, component.renderStack, component.wearCol));
	        Component.redrawComponent(component);
	    }
	}

	public static void drawComponents(Component[] components, int i_1, int clippingLeft, int clippingTop, int clippingRight, int clippingBottom, int offsetX, int offsetY, int i_8, boolean bool9) {
	    Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
	    for (int componentIndex = 0; componentIndex < components.length; componentIndex++) {
	        Component component = components[componentIndex];
	        if (component != null && (component.parentId == i_1 || i_1 == -1412584499 && component == client.draggingComponent)) {
	            int componentReference;
	            if (i_8 == -1) {
	                client.INTERFACE_BG_COMPONENTS[client.anInt7407].setBounds(component.x + offsetX, component.y + offsetY, component.width, component.height);
	                componentReference = ++client.anInt7407 - 1;
	            } else {
	                componentReference = i_8;
	            }
	            component.cacheReference = componentReference;
	            component.lastRenderedCycle = client.cycles;
	            if (!InterfaceRenderer.isComponentHidden(component)) {
	                if (component.contentType != null) {
	                    Component.prepareSequenceOnInterface(component);
	                }
	                int drawX = component.x + offsetX;
	                int drawY = component.y + offsetY;
	                int drawTransparency = component.transparency;
	                if (client.DISPLAY_HIDDEN_MENU_OPTIONS && (client.getComponentSettings(component).settingsHash != 0 || component.type == ComponentType.CONTAINER) && drawTransparency > 127) {
	                    drawTransparency = 127;
	                }
	                int startX;
	                int startY;
	                if (component == client.draggingComponent) {
	                    if (i_1 != -1412584499 && (component.anUnkSetting11 == Component.UnkSetting1.UNK_2 || component.anUnkSetting11 == Component.UnkSetting1.UNK_0 || client.getComponentSettings(component).ignoresDepthFlags())) {
	                        InterfaceComponentRenderer.aClass118Array3772 = components;
	                        InterfaceComponentRenderer.anInt4822 = offsetX;
	                        InterfaceComponentRenderer.anInt5157 = offsetY;
	                        continue;
	                    }
	                    if (client.aBool7364 && client.isComponentSelected) {
	                        startX = MouseRecord.recorder.getMouseX();
	                        startY = MouseRecord.recorder.getMouseY();
	                        startX -= client.mouseX;
	                        startY -= client.mouseY;
	                        if (startX < client.selectedComponentX) {
	                            startX = client.selectedComponentX;
	                        }
	                        if (startX + component.width > client.selectedComponentX + client.selectedComponentWidth) {
	                            startX = client.selectedComponentX + client.selectedComponentWidth - component.width;
	                        }
	                        if (startY < client.selectedComponentY) {
	                            startY = client.selectedComponentY;
	                        }
	                        if (startY + component.height > client.selectedComponentY + client.selectedComponentHeight) {
	                            startY = client.selectedComponentHeight + client.selectedComponentY - component.height;
	                        }
	                        if (client.getComponentSettings(component).ignoresDepthFlags()) {
	                            InterfaceComponentRenderer.inboundsComponentToTop(startX, startY, component.width, component.height);
	                        }
	                        drawX = startX;
	                        drawY = startY;
	                    }
	                    if (component.anUnkSetting11 == Component.UnkSetting1.UNK_0) { // Something for bank?
	                        drawTransparency = 128;
	                    }
	                }
	                int endX;
	                int endY;
	                if (component.type == ComponentType.TYPE_2) {
	                    startX = clippingLeft;
	                    startY = clippingTop;
	                    endX = clippingRight;
	                    endY = clippingBottom;
	                } else {
	                    int lineStartX = drawX + component.width;
	                    int lineStartY = drawY + component.height;
	                    if (component.type == ComponentType.LINE) {
	                        lineStartX++;
	                        lineStartY++;
	                    }
	                    startX = Math.max(drawX, clippingLeft);
	                    startY = Math.max(drawY, clippingTop);
	                    endX = Math.min(lineStartX, clippingRight);
	                    endY = Math.min(lineStartY, clippingBottom);
	                }
	                if (startX < endX && startY < endY) {
	                    if (component.contentType != null) {
	                        if (component.contentType == GAME_WINDOW || component.contentType == CONTENT_TYPE_1403) {
	                            client.drawGameArea(drawX, drawY, component.width, component.height, component.contentType == CONTENT_TYPE_1403);
	                            EntityUpdating.method12366(componentReference, startX, startY, endX, endY, drawX, drawY);
	                            Renderers.activeToolkit.calculateViewportCenter();
	                            Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
	                            client.IS_COMPONENT_ON_TOP[componentReference] = true;
	                            continue;
	                        }
	                        if (component.contentType == MINIMAP && client.MAP_LOADING_STAGE == 1) {
	                            if (component.addInteractiveSprite(Renderers.activeToolkit) != null) {
	                                SoundEffectPlayer.updatePlayers();
	                                ComponentMinimap.drawMinimap(Renderers.activeToolkit, component, drawX, drawY);
	                                Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
	                            }
	                            continue;
	                        }
	                        if (component.contentType == COLOR_PICKER) {
	                            ColorPicker.drawColorPicker(Renderers.activeToolkit, drawX, drawY, component);
	                            continue;
	                        }
	                        if (component.contentType == CONTENT_TYPE_1409) {
	                            AbstractToolkit.drawFadingFilledRectangle(Renderers.activeToolkit, drawX, drawY, component, component.color % 64);
	                            continue;
	                        }
	                        if (component.contentType == COMPASS) {
	                            if (component.addInteractiveSprite(Renderers.activeToolkit) != null) {
	                                ComponentMinimap.drawCompass(component, drawX, drawY);
	                                Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
	                            }
	                            continue;
	                        }
	                        if (component.contentType == WORLDMAP) {
	                            WorldMap.drawWorldmap(Renderers.activeToolkit, TypeLists.MATERIALS, drawX, drawY, component.width, component.height);
	                            client.IS_COMPONENT_ON_TOP[componentReference] = true;
	                            Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
	                            continue;
	                        }
	                        if (component.contentType == WORLD_MAP_OVERVIEW) {
	                            WorldMapOverview.drawWorldmapOverview(Renderers.activeToolkit, drawX, drawY, component.width, component.height);
	                            client.IS_COMPONENT_ON_TOP[componentReference] = true;
	                            Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
	                            continue;
	                        }
	                        if (component.contentType == DEBUG_INFORMATION) {
	                            if (client.DRAW_DEBUG || client.DEBUG_MODE) {
	                                SystemInfo.drawDebugInformation(drawX, drawY, component);
	                                client.IS_COMPONENT_ON_TOP[componentReference] = true;
	                            }
	                            continue;
	                        }
	                    }

	                    if (component.type == ComponentType.CONTAINER) {
	                        if (component.contentType == CUTSCENE_FADE && Renderers.activeToolkit.bloomEnabled()) {
	                            Renderers.activeToolkit.method8525(GameShell.screenCanvasWidth, client.screenCanvasHeight * -969250379);
	                        }
	                        drawComponents(components, component.idHash, startX, startY, endX, endY, drawX - component.scrollX, drawY - component.scrollY, componentReference, bool9);
	                        if (component.itemSlots != null) {
	                            drawComponents(component.itemSlots, component.idHash, startX, startY, endX, endY, drawX - component.scrollX, drawY - component.scrollY, componentReference, bool9);
	                        }
	                        SubInterface subInterface = client.OPEN_INTERFACES.get(component.idHash);
	                        if (subInterface != null) {
	                            Component.method3629(subInterface.subInterfaceId, startX, startY, endX, endY, drawX, drawY, componentReference);
	                        }
	                        if (component.contentType == CUTSCENE_FADE) {
	                            if (Renderers.activeToolkit.bloomEnabled()) {
	                                Renderers.activeToolkit.method8474();
	                            }
	                            if (client.MAP_LOADING_STAGE == 4) {
	                                int fadeAlpha = cutsceneFadeAlpha;
	                                int fadeRed = cutsceneFadeRed;
	                                int fadeGreen = cutsceneFadeGreen;
	                                int fadeBlue = cutsceneFadeBlue;
	                                if (client.cycles < cutsceneFadeEndCycle) {
	                                    float completion = (client.cycles - cycleCutsceneFadeStarted) * 1.0F / (cutsceneFadeEndCycle - cycleCutsceneFadeStarted);
	                                    fadeAlpha = (int) (cutsceneFadeAlpha * completion + (1.0F - completion) * prevCutsceneFadeAlpha);
	                                    fadeRed = (int) (cutsceneFadeRed * completion + prevCutsceneFadeRed * (1.0F - completion));
	                                    fadeGreen = (int) (completion * cutsceneFadeGreen + prevCutsceneFadeGreen * (1.0F - completion));
	                                    fadeBlue = (int) (completion * cutsceneFadeBlue + prevCutsceneFadeBlue * (1.0F - completion));
	                                }
	                                if (fadeAlpha > 0) {
	                                    Renderers.activeToolkit.clearPixels(startX, startY, endX - startX, endY - startY, fadeAlpha << 24 | fadeRed << 16 | fadeGreen << 8 | fadeBlue);
	                                }
	                            }
	                        }
	                        Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
	                    }
	                    if (client.PREV_COMPONENT_ON_TOP[componentReference] || client.anInt7412 > 1) {
	                        if (component.type == ComponentType.FIGURE) {
	                            if (drawTransparency == 0) {
	                                if (component.filled) {
	                                    Renderers.activeToolkit.drawFilledRectangle(drawX, drawY, component.width, component.height, component.color, 0);
	                                } else {
	                                    Renderers.activeToolkit.drawRectangleOutline(drawX, drawY, component.width, component.height, component.color, 0);
	                                }
	                            } else if (component.filled) {
	                                Renderers.activeToolkit.drawFilledRectangle(drawX, drawY, component.width, component.height, 255 - (drawTransparency & 0xff) << 24 | component.color & 0xffffff, 1);
	                            } else {
	                                Renderers.activeToolkit.drawRectangleOutline(drawX, drawY, component.width, component.height, 255 - (drawTransparency & 0xff) << 24 | component.color & 0xffffff, 1);
	                            }
	                        } else if (component.type == ComponentType.TEXT) {
                                AbstractFontRenderer fontRenderer = component.getFontRenderer(StaticMethods.initializedFontcombos, client.FONT_PROVIDER);
                                if (fontRenderer == null) {
                                    if (Component.missingMedia) {
                                        Component.redrawComponent(component);
                                    }
                                } else {
                                    int color = component.color;
                                    String itemDisplayText = component.text;
                                    if (component.containerItemId != -1) {
                                        ItemType objType = TypeLists.OBJ.list(component.containerItemId);
                                        itemDisplayText = objType.name;
                                        if (itemDisplayText == null) {
                                            itemDisplayText = "null";
                                        }
                                        if ((objType.stackable == 1 || component.itemStackSize != 1) && component.itemStackSize != -1) {
                                            itemDisplayText = TextUtils.setTextColor(16748608) + itemDisplayText + "</col>" + " x" + TextUtils.formatStackSize(component.itemStackSize);
                                        }
                                    }
                                    if (component.anInt1435 != -1) {
                                        itemDisplayText = SoundResourceManager.method2550(component.anInt1435);
                                        if (itemDisplayText == null) {
                                            itemDisplayText = "";
                                        }
                                    }
                                    if (component == client.aClass1187352) {
                                        itemDisplayText = LocalizedText.PLEASE_WAIT.translate(client.CURRENT_LANGUAGE);
                                        color = component.color;
                                    }
                                    if (client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit.expandClipping(drawX, drawY, drawX + component.width, drawY + component.height);
                                    }
                                    if (component.textAntiMacro) {
                                        fontRenderer.renderTextAntiMacro(itemDisplayText, drawX, drawY, component.width, component.height, 255 - (drawTransparency & 0xff) << 24 | color, component.shadow ? 255 - (drawTransparency & 0xff) << 24 : -1, component.textHorizontalAli, component.textVerticalAli, client.RANDOMIZER, client.randomSeed, client.anIntArray7438, StaticMedia.nameIconSprites, null);
                                    } else {
                                        fontRenderer.renderText(itemDisplayText, drawX, drawY, component.width, component.height, 255 - (drawTransparency & 0xff) << 24 | color, component.shadow ? 255 - (drawTransparency & 0xff) << 24 : -1, component.textHorizontalAli, component.textVerticalAli, component.anInt1358, component.maxTextLines, StaticMedia.nameIconSprites, null, null, 0, 0);
                                    }
                                    if (client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
                                    }
                                }
                            } else if (component.type == ComponentType.SPRITE) {
                                if (component.skyboxId >= 0) {
                                    component.calculateLighting(TypeLists.SKYBOX, TypeLists.SUN).render(Renderers.activeToolkit, drawX, drawY, component.width, component.height, component.sunRotationX << 3, component.sunRotationY << 3);
                                } else {
                                    NativeSprite sprite;
                                    if (component.containerItemId != -1) {
                                        PlayerAppearance appearance = component.wearCol ? PlayerUpdate.MY_PLAYER.playerAppearance : null;
                                        sprite = TypeLists.OBJ.createNativeSprite(Renderers.activeToolkit, component.containerItemId, component.itemStackSize, component.borderThickness, ~0xffffff | component.spriteShadow, component.renderStack, appearance);
                                    } else if (component.anInt1435 != -1) {
                                        sprite = VorbisNode.method8858(Renderers.activeToolkit, component.anInt1435);
                                    } else {
                                        sprite = component.draw(Renderers.activeToolkit);
                                    }
                                    if (sprite != null) {
                                        int spriteWidth = sprite.getSpriteWidth();
                                        int spriteHeight = sprite.getSpriteHeight();
                                        int drawColor = 255 - (drawTransparency & 0xff) << 24 | (component.color != 0 ? component.color & 0xffffff : 16777215);
                                        if (component.tiling) {
                                            Renderers.activeToolkit.expandClipping(drawX, drawY, drawX + component.width, drawY + component.height);
                                            if (component.angle2d != 0) {
                                                int height = (spriteWidth - 1 + component.width) / spriteWidth;
                                                int i_39 = (spriteHeight - 1 + component.height) / spriteHeight;
                                                for (int i_27 = 0; i_27 < height; i_27++) {
                                                    for (int i_28 = 0; i_28 < i_39; i_28++) {
                                                        if (component.color != 0) {
                                                            sprite.method2790((drawX + i_27 * spriteWidth) + spriteWidth / 2.0F, spriteHeight / 2.0F + (drawY + spriteHeight * i_28), 4096, component.angle2d, drawColor);
                                                        } else {
                                                            sprite.method2758((drawX + spriteWidth * i_27) + spriteWidth / 2.0F, (drawY + spriteHeight * i_28) + spriteHeight / 2.0F, 4096, component.angle2d);
                                                        }
                                                    }
                                                }
                                            } else if (component.color == 0 && drawTransparency == 0) {
                                                sprite.method2756(drawX, drawY, component.width, component.height);
                                            } else {
                                                sprite.method2772(drawX, drawY, component.width, component.height, 0, drawColor, 1);
                                            }
                                            Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
                                        } else if (component.color == 0 && drawTransparency == 0) {
                                            if (component.angle2d != 0) {
                                                sprite.method2758(component.width / 2.0F + drawX, drawY + component.height / 2.0F, component.width * 4096 / spriteWidth, component.angle2d);
                                            } else if (spriteWidth == component.width && spriteHeight == component.height) {
                                                sprite.draw(drawX, drawY);
                                            } else {
                                                sprite.drawCircle(drawX, drawY, component.width, component.height);
                                            }
                                        } else if (component.angle2d != 0) {
                                            sprite.method2790(component.width / 2.0F + drawX, component.height / 2.0F + drawY, component.width * 4096 / spriteWidth, component.angle2d, drawColor);
                                        } else if (spriteWidth == component.width && spriteHeight == component.height) {
                                            sprite.draw(drawX, drawY, 0, drawColor, 1);
                                        } else {
                                            sprite.drawCircle(drawX, drawY, component.width, component.height, 0, drawColor, 1);
                                        }
                                    } else if (Component.missingMedia) {
                                        Component.redrawComponent(component);
                                    }
                                }
                            } else if (component.type == ComponentType.MODEL) {
                                Resource.mapRegion.getEnvironmentManager().prepareLights();
                                Model renderedMediaModel = null;
                                int functionMask = 2048;
                                if (component.modelTintScalar != 0) {
                                    functionMask |= 0x80000;
                                }
                                int centerY = 0;
                                if (component.containerItemId != -1) {
                                    ItemType objType = TypeLists.OBJ.list(component.containerItemId);
                                    if (objType != null) {
                                        objType = objType.getStackSizeAlt(component.itemStackSize);
                                        renderedMediaModel = objType.getAnimatedModel(Renderers.activeToolkit, functionMask, 1, component.wearCol ? PlayerUpdate.MY_PLAYER.playerAppearance : null, component.animatingSequence, 0, 0, 0, 0);
                                        if (renderedMediaModel != null) {
                                            centerY = -renderedMediaModel.getMinY() >> 1;
                                        } else {
                                            Component.redrawComponent(component);
                                        }
                                    }
                                } else if (component.mediaType == MediaType.PLAYER_HEAD) {
                                    int lineHeight = component.primaryMediaId;
                                    if (lineHeight >= 0 && lineHeight < 2048) {
                                        PlayerEntity player = PlayerUpdate.players[lineHeight];
                                        if (player != null && (lineHeight == PlayerUpdate.myPlayerIndex || Utils.stringToInt(player.displayName) == component.secondaryMediaId)) {
                                            renderedMediaModel = component.getAnimatedMediaModel(Renderers.activeToolkit, functionMask, TypeLists.BAS, TypeLists.IDK, TypeLists.NPC, TypeLists.OBJ, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, player.playerAppearance);
                                            if (renderedMediaModel == null && Component.missingMedia) {
                                                Component.redrawComponent(component);
                                            }
                                        }
                                    }
                                } else if (component.mediaType == MediaType.PLAYER_MODEL) {
                                    int target = component.primaryMediaId;
                                    if (target >= 0 && target < 2048) {
                                        PlayerEntity player = PlayerUpdate.players[target];
                                        if (player != null && (target == PlayerUpdate.myPlayerIndex || Utils.stringToInt(player.displayName) == component.secondaryMediaId)) {
                                            renderedMediaModel = player.playerAppearance.getBodyModel(Renderers.activeToolkit, functionMask, TypeLists.BAS, TypeLists.IDK, TypeLists.NPC, TypeLists.OBJ, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, null, null, null, 0, TypeLists.EQUIPMENT_DEFAULTS);
                                        }
                                    }
                                } else if (component.mediaType != MediaType.ITEM_CONTAINER_MALE && component.mediaType != MediaType.ITEM_CONTAINER_FEMALE) {
                                    if (component.animatingSequence != null && component.animatingSequence.hasType()) {
                                        renderedMediaModel = component.getAnimatedMediaModel(Renderers.activeToolkit, functionMask, TypeLists.BAS, TypeLists.IDK, TypeLists.NPC, TypeLists.OBJ, VarProviders.PLAYER_VAR_PROVIDER, component.animatingSequence, PlayerUpdate.MY_PLAYER.playerAppearance);
                                    } else {
                                        renderedMediaModel = component.getAnimatedMediaModel(Renderers.activeToolkit, functionMask, TypeLists.BAS, TypeLists.IDK, TypeLists.NPC, TypeLists.OBJ, VarProviders.PLAYER_VAR_PROVIDER, null, PlayerUpdate.MY_PLAYER.playerAppearance);
                                    }
                                    if (renderedMediaModel == null && Component.missingMedia) {
                                        Component.redrawComponent(component);
                                    }
                                } else {
                                    ItemContainer container = ItemContainer.getContainer(component.primaryMediaId, false);
                                    if (container != null) {
                                        renderedMediaModel = container.getAnimatedBodyModel(Renderers.activeToolkit, functionMask, component.animatingSequence, component.secondaryMediaId, component.mediaType == MediaType.ITEM_CONTAINER_FEMALE, component.wearCol ? PlayerUpdate.MY_PLAYER.playerAppearance : null);
                                    }
                                }
                                if (renderedMediaModel != null) {
                                    if (component.modelTintScalar != 0) {
                                        renderedMediaModel.tint(component.modelTintRed, component.modelTintGreen, component.modelTintBlue, component.modelTintScalar);
                                    }
                                    int width;
                                    int height;
                                    if (component.aspectWidth > 0) {
                                        width = (component.width << 9) / component.aspectWidth;
                                    } else {
                                        width = 512;
                                    }
                                    if (component.aspectHeight > 0) {
                                        height = (component.height << 9) / component.aspectHeight;
                                    } else {
                                        height = 512;
                                    }
                                    int i_39 = component.width / 2 + drawX;
                                    int i_27 = component.height / 2 + drawY;
                                    if (!component.hasOrigin) {
                                        i_39 += width * component.originX >> 9;
                                        i_27 += height * component.originY >> 9;
                                    }
                                    client.SUN_MATRIX.reset();
                                    Renderers.activeToolkit.setSunPosition(client.SUN_MATRIX);
                                    Matrix44 matrix44 = Renderers.activeToolkit.method8449();
                                    int viewRangeStart = Resource.mapRegion.getViewRangeStart();
                                    int viewRangeEnd = Resource.mapRegion.getViewRangeEnd();
                                    viewRangeEnd += component.spriteScale;
                                    if (component.usesOrthogonal) {
                                        if (component.hasOrigin) {
                                            matrix44.method6532(i_39, i_27, width, height, viewRangeStart, viewRangeEnd, GameShell.screenCanvasWidth, (client.screenCanvasHeight * -969250379), component.spriteScale);
                                        } else {
                                            matrix44.method6532(i_39, i_27, width, height, viewRangeStart, viewRangeEnd, GameShell.screenCanvasWidth, (client.screenCanvasHeight * -969250379), (component.spriteScale << 2));
                                        }
                                    } else {
                                        matrix44.method6531(i_39, i_27, width, height, viewRangeStart, viewRangeEnd, GameShell.screenCanvasWidth, (client.screenCanvasHeight * -969250379));
                                    }
                                    Renderers.activeToolkit.setModelMatrix(matrix44);
                                    Renderers.activeToolkit.applyClearFlags(2, 0);
                                    if (component.priorityRender) {
                                        Renderers.activeToolkit.setDepthBuffering(false);
                                    }
                                    if (component.hasOrigin) {
                                        client.INTERFACE_MODEL_MATRIX.rotateMatrix(1.0F, 0.0F, 0.0F, MathUtils.getTheta(component.spritePitch));
                                        client.INTERFACE_MODEL_MATRIX.rotate(0.0F, 1.0F, 0.0F, MathUtils.getTheta(component.spriteRoll));
                                        client.INTERFACE_MODEL_MATRIX.rotate(0.0F, 0.0F, 1.0F, MathUtils.getTheta(component.spriteYaw));
                                        client.INTERFACE_MODEL_MATRIX.translate(component.originX, component.originY, component.originZ);
                                    } else {
                                        int i_31 = MathUtils.SINE[component.spritePitch << 3] * (component.spriteScale << 2) >> 14;
                                        int i_32 = MathUtils.COSINE[component.spritePitch << 3] * (component.spriteScale << 2) >> 14;
                                        client.INTERFACE_MODEL_MATRIX.rotateMatrix(0.0F, 0.0F, 1.0F, MathUtils.getTheta(-component.spriteYaw << 3));
                                        client.INTERFACE_MODEL_MATRIX.rotate(0.0F, 1.0F, 0.0F, MathUtils.getTheta(component.spriteRoll << 3));
                                        client.INTERFACE_MODEL_MATRIX.translate((component.offsetX << 2), (i_31 + centerY + (component.offsetY << 2)), (i_32 + (component.offsetY << 2)));
                                        client.INTERFACE_MODEL_MATRIX.rotate(1.0F, 0.0F, 0.0F, MathUtils.getTheta(component.spritePitch << 3));
                                    }
                                    component.cycleParticles(Renderers.activeToolkit, renderedMediaModel, client.INTERFACE_MODEL_MATRIX, client.cycles);
                                    if (client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit.expandClipping(drawX, drawY, drawX + component.width, drawY + component.height);
                                    }
                                    renderedMediaModel.drawMatrixThreaded(client.INTERFACE_MODEL_MATRIX, null, ModelFunctionMaskGL.SMOOTH_EDGES);
                                    if (!component.usesOrthogonal && component.particleSystem != null) {
                                        Renderers.activeToolkit.renderParticles(component.particleSystem.rebuildRenderedParticles());
                                    }
                                    if (client.CLIP_INTERFACES) {
                                        Renderers.activeToolkit.setClipping(clippingLeft, clippingTop, clippingRight, clippingBottom);
                                    }
                                    if (component.priorityRender) {
                                        Renderers.activeToolkit.setDepthBuffering(true);
                                    }
                                }
                            } else if (component.type == ComponentType.LINE) {
                                int lineStartX;
                                int lineStartY;
                                int lineWidth;
                                int lineHeight;
                                if (component.lineDirection) {
                                    lineStartX = drawX;
                                    lineStartY = drawY + component.height;
                                    lineWidth = drawX + component.width;
                                    lineHeight = drawY;
                                } else {
                                    lineStartX = drawX;
                                    lineStartY = drawY;
                                    lineWidth = drawX + component.width;
                                    lineHeight = drawY + component.height;
                                }
                                if (component.lineWidth == 1) {
                                    Renderers.activeToolkit.drawTransparentLine(lineStartX, lineStartY, lineWidth, lineHeight, component.color, 0);
                                } else {
                                    Renderers.activeToolkit.drawThickLine(lineStartX, lineStartY, lineWidth, lineHeight, component.color, component.lineWidth, 0);
                                }
                            }
	                    }
	                }
	            }
	        }
	    }
	}

	public static void updateEntityOnInterface(Component[] components, Component component, boolean bool2) {
	    int componentWidth = component.scrollWidth != 0 ? component.scrollWidth : component.width;
	    int componentHeight = component.scrollHeight != 0 ? component.scrollHeight : component.height;
	    InterfaceRenderer.updateInterfaceDimensions(components, component.idHash, componentWidth, componentHeight, bool2);
	    if (component.itemSlots != null) {
	        InterfaceRenderer.updateInterfaceDimensions(component.itemSlots, component.idHash, componentWidth, componentHeight, bool2);
	    }
	    SubInterface subInterface = client.OPEN_INTERFACES.get(component.idHash);
	    if (subInterface != null) {
	        InterfaceRenderer.updateInterfaceDimensions(subInterface.subInterfaceId, componentWidth, componentHeight, bool2);
	    }
	    if (component == client.GAME_SCREEN_INTERFACE) {
	        int playerIndiceCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES;
	        int[] playerIndices = PlayerUpdate.OUT_PLAYER_INDICES;
	        for (int index = 0; index < playerIndiceCount; index++) {
	            PlayerEntity playerEntity = PlayerUpdate.players[playerIndices[index]];
	            if (playerEntity != null) {
	                playerEntity.updateInterfaceDisplay(componentWidth, componentHeight, bool2);
	            }
	        }
	        for (int npcUpdateIndex = 0; npcUpdateIndex < client.NPC_UPDATE_INDEX; npcUpdateIndex++) {
	            int updateKey = client.NPC_UPDATE_INDICES[npcUpdateIndex];
	            ObjectNode npcObjectNode = client.NPC_MAP.get(updateKey);
	            if (npcObjectNode != null) {
	                ((PathingEntity) npcObjectNode.value).updateInterfaceDisplay(componentWidth, componentHeight, bool2);
	            }
	        }
	    }
	}

	public static void componentPositionAdjustment(Component component, int x, int y) {
	    if (component.aspectXType == 0) {
	        component.x = component.basePositionX;
	    } else if (component.aspectXType == 1) {
	        component.x = component.basePositionX + (x - component.width) / 2;
	    } else if (component.aspectXType == 2) {
	        component.x = x - component.width - component.basePositionX;
	    } else if (component.aspectXType == 3) {
	        component.x = component.basePositionX * x >> 14;
	    } else if (component.aspectXType == 4) {
	        component.x = (x - component.width) / 2 + (component.basePositionX * x >> 14);
	    } else {
	        component.x = x - component.width - (component.basePositionX * x >> 14);
	    }
	    if (component.aspectYType == 0) {
	        component.y = component.basePositionY;
	    } else if (component.aspectYType == 1) {
	        component.y = (y - component.height) / 2 + component.basePositionY;
	    } else if (component.aspectYType == 2) {
	        component.y = y - component.height - component.basePositionY;
	    } else if (component.aspectYType == 3) {
	        component.y = y * component.basePositionY >> 14;
	    } else if (component.aspectYType == 4) {
	        component.y = (y - component.height) / 2 + (y * component.basePositionY >> 14);
	    } else {
	        component.y = y - component.height - (y * component.basePositionY >> 14);
	    }
	    if (client.DISPLAY_HIDDEN_MENU_OPTIONS && (client.getComponentSettings(component).settingsHash != 0 || component.type == ComponentType.CONTAINER)) {
	        if (component.x < 0) {
	            component.x = 0;
	        } else if (component.x + component.width > x) {
	            component.x = x - component.width;
	        }
	        if (component.y < 0) {
	            component.y = 0;
	        } else if (component.height + component.y > y) {
	            component.y = y - component.height;
	        }
	    }
	}

	public static void method4152(Component component, ComponentSurface dimension, int i_2, int i_3, int i_4, int i_5, int i_6, String string_7, AbstractFontRenderer fontRenderer, FontMetrics fontmetrics9, int i_10) {
	    int angle;
	    if (Camera.cameraCalculationType == 4) {
	        angle = (int) Camera.camAngleY & 0x3fff;
	    } else {
	        angle = (int) Camera.camAngleY + client.minimapRotation & 0x3fff;
	    }
	    int i_13 = Math.max(component.width / 2, component.height / 2) + 10;
	    int i_14 = i_5 * i_5 + i_4 * i_4;
	    if (i_14 <= i_13 * i_13) {
	        int sin = MathUtils.SINE[angle];
	        int cos = MathUtils.COSINE[angle];
	        if (Camera.cameraCalculationType != 4) {
	            sin = sin * 256 / (client.minimapZoom + 256);
	            cos = cos * 256 / (client.minimapZoom + 256);
	        }
	        int i_17 = cos * i_4 + i_5 * sin >> 14;
	        int i_18 = i_5 * cos - sin * i_4 >> 14;
	        int i_19 = fontmetrics9.getTextWidthMultiline(string_7, 100, null);
	        int i_20 = fontmetrics9.getTextHeightMultiline(string_7, 100, 0, null);
	        i_17 -= i_19 / 2;
	        if (i_17 >= -component.width && i_17 <= component.width && i_18 >= -component.height && i_18 <= component.height) {
	            fontRenderer.drawText(string_7, i_17 + component.width / 2 + i_2, i_3 + component.height / 2 - i_18 - i_6 - i_20, i_19, 50, i_10, 0, 1, 0, 0, null, null, dimension, i_2, i_3);
	        }
	    }
	}

	public static Component[] aClass118Array3772;

	public static void inboundsComponentToTop(int x, int y, int offsetX, int offsetY) {
	    for (int index = 0; index < client.anInt7407; index++) {
	        Rectangle rectangle = client.INTERFACE_BG_COMPONENTS[index];
	        if (rectangle.x + rectangle.width > x && rectangle.x < x + offsetX && rectangle.height + rectangle.y > y && rectangle.y < offsetY + y) {
	            client.IS_COMPONENT_ON_TOP[index] = true;
	        }
	    }
	}

	public static int anInt4822;

	public static void method544(Component component, int x, int y) {
	    InterfaceComponentRenderer.aClass118183 = component;
	    InterfaceComponentRenderer.anInt195 = x;
	    InterfaceComponentRenderer.anInt179 = y;
	}

	public static Component aClass118183;
	public static int anInt195 = -1;
	public static int anInt179 = -1;
	public static int anInt5157;

	public static void renderItems(AbstractToolkit toolkit) {
	    if (!Component.ITEMS.isEmpty()) {
	        if (PreferenceManager.preferences.activeToolkit.getValue() == 0) {
	            for (Item item = (Item) Component.ITEMS.getFirst(); item != null; item = (Item) Component.ITEMS.next()) {
	                TypeLists.OBJ.getSprite(toolkit, toolkit, item.id, item.amount, item.outlineSize, item.shadowColor, false, false, item.stackMode, Renderers.FONT_RENDERER, item.hasPlayerAppearance ? PlayerUpdate.MY_PLAYER.playerAppearance : null);
	                item.unlink();
	            }
	            GameShell.setComponentsOnTop();
	        } else {
	            if (Renderers.GPU_ITEM_RENDERER == null) {
	                Canvas itemCanvas = new Canvas();
	                itemCanvas.setSize(36, 32);
	                Renderers.GPU_ITEM_RENDERER = AbstractToolkit.create(0, itemCanvas, TypeLists.MATERIALS, Resource.SHADER, 0);
	                Renderers.GPU_ITEM_FONT_RENDERER = Renderers.GPU_ITEM_RENDERER.createFontRenderer(FontMetrics.getFontMetrics(Resource.FONT_METRICS, FontManager.p11FullIndex), SpriteType.decodeGroup(Resource.SPRITES, FontManager.p11FullIndex), true);
	            }
	            for (Item item = (Item) Component.ITEMS.getFirst(); item != null; item = (Item) Component.ITEMS.next()) {
	                TypeLists.OBJ.getSprite(Renderers.GPU_ITEM_RENDERER, toolkit, item.id, item.amount, item.outlineSize, item.shadowColor, false, false, item.stackMode, Renderers.GPU_ITEM_FONT_RENDERER, item.hasPlayerAppearance ? PlayerUpdate.MY_PLAYER.playerAppearance : null);
	                item.unlink();
	            }
	        }
	    }
	}

	public static int prevCutsceneFadeBlue;
	public static int prevCutsceneFadeGreen;
	public static int prevCutsceneFadeRed;
	public static int prevCutsceneFadeAlpha;
	public static int cutsceneFadeAlpha;
	public static int cutsceneFadeRed;
	public static int cutsceneFadeBlue;
	public static int cutsceneFadeGreen;
	public static int cutsceneFadeEndCycle;
	public static int cycleCutsceneFadeStarted;

}
