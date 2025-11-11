package com.jagex.game.runetek5.client.menu;

import com.jagex.game.runetek5.cache.node.IterableNodeMap;
import com.jagex.game.runetek5.cache.node.NodeCollection;
import com.jagex.game.runetek5.cache.node.NodeIterator;
import com.jagex.game.runetek5.cache.node.Queue;
import com.jagex.game.runetek5.cache.node.QueueIterator;
import com.jagex.game.runetek5.cache.node.impl.ObjectNode;
import com.jagex.game.runetek5.cache.node.lru.CacheableNode;
import com.jagex.game.runetek5.cache.node.lru.LRUCache;
import com.jagex.game.runetek5.client.GameShell;
import com.jagex.game.runetek5.client.client;
import com.jagex.game.runetek5.client.input.keyboard.KeyDownCommand;
import com.jagex.game.runetek5.client.input.mouse.Mouse;
import com.jagex.game.runetek5.client.input.mouse.MouseRecord;
import com.jagex.game.runetek5.config.Resource;
import com.jagex.game.runetek5.config.TypeLists;
import com.jagex.game.runetek5.config.object.ObjectType;
import com.jagex.game.runetek5.config.npc.NPCType;
import com.jagex.game.runetek5.entity.gameobject.StationaryWall;
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity;
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate;
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer;
import com.jagex.game.runetek5.graphics.font.FontMetrics;
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer;
import com.jagex.game.runetek5.graphics.sprite.NativeSprite;
import com.jagex.game.runetek5.graphics.sprite.StaticMedia;
import com.jagex.game.runetek5.interaction.chat.ChatBox;
import com.jagex.game.runetek5.scene.Scene;
import com.jagex.game.runetek5.staticmethods.StaticMethods;
import com.jagex.game.runetek5.toolkit.AbstractToolkit;
import com.jagex.game.runetek5.tools.LocalizedText;
import com.jagex.game.runetek5.utils.TextUtils;
import com.jagex.game.runetek5.vardomain.VarProviders;

public class MiniMenu {

    public static int entryCount;
    public static MiniMenuEntry aCacheableNodeSub712;

    public static String[] prefixWithIndex(String[] srcOptions) {
        String[] dstOptions = new String[5];
        for (int index = 0; index < 5; index++) {
            dstOptions[index] = index + " ";
            if (srcOptions != null && srcOptions[index] != null) {
                dstOptions[index] += srcOptions[index];
            }
        }
        return dstOptions;
    }

    public static void addEntry(MiniMenuEntry entry) {
        if (entry != null) {
            MiniMenu.aClass482171.addFirst(entry);
            entryCount++;
            ColoredMiniMenuEntry c;
            if (!entry.aBool9586 && entry.coloredName != null && !entry.coloredName.isEmpty()) {
                long hash = entry.bitpacked;
                for (c = (ColoredMiniMenuEntry) MiniMenu.aClass465172.get(hash); c != null && !c.tagStart.equals(entry.coloredName);
					 c = (ColoredMiniMenuEntry) MiniMenu.aClass465172.nextInBucket()) { }
                if (c == null) {
                    c = (ColoredMiniMenuEntry) MiniMenu.aClass229164.get(hash);
                    if (c != null && !c.tagStart.equals(entry.coloredName)) {
                        c = null;
                    }
                    if (c == null) {
                        c = new ColoredMiniMenuEntry(entry.coloredName);
                    }
                    MiniMenu.aClass465172.put(c, hash);
                    ++MiniMenu.anInt170;
                }
            } else {
                c = new ColoredMiniMenuEntry(entry.coloredName);
                ++MiniMenu.anInt170;
            }
            if (c.addEntry(entry)) {
                MiniMenu.method503(c);
            }
        }
    }

    public static int getWidthForEntryWithQuests(MiniMenuEntry menuEntry, FontMetrics fontmetrics) {
        String text = MiniMenu.getTextFrom(menuEntry);
        int[] quests = MiniMenu.getQuestsFrom(menuEntry);
        if (quests != null) {
            text += ChatBox.addQuestChatIcons(quests);
        }
        int width = fontmetrics.getTextWidth(text, StaticMedia.chatIcons);
        if (menuEntry.aBool9577) {
            width += StaticMedia.otherLevelSprite.getWidth() + 4;
        }
        return width;
    }

    public static String method13768(MiniMenuEntry class282Sub50Sub70) {
        return !MiniMenu.isOpen && class282Sub50Sub70 != null ? class282Sub50Sub70.action : "";
    }

    public static void method12212(ColoredMiniMenuEntry class282Sub50Sub150, int i_1) {
        if (MiniMenu.isOpen) {
            FontMetrics fontmetrics3 = FontMetrics.getForMinimenu();
            int i_4 = 0;

            int i_6;
            for (MiniMenuEntry class282Sub50Sub75 = (MiniMenuEntry) class282Sub50Sub150.nodeQueue.getNext(); class282Sub50Sub75 != null; class282Sub50Sub75 = (MiniMenuEntry) class282Sub50Sub150.nodeQueue.getDirectNext()) {
                i_6 = getWidthForEntryWithQuests(class282Sub50Sub75, fontmetrics3);
                if (i_6 > i_4) {
                    i_4 = i_6;
                }
            }

            i_4 += 8;
            int i_9 = class282Sub50Sub150.queueSize * MiniMenu.anInt178 + 21;
            StationaryWall.anInt10526 = (FontMetrics.useBoldFont ? 26 : 22) + MiniMenu.anInt178 * class282Sub50Sub150.queueSize;
            i_6 = StaticMethods.anInt3555 + MiniMenu.anInt8975;
            if (i_4 + i_6 > GameShell.screenCanvasWidth) {
                i_6 = StaticMethods.anInt3555 - i_4;
            }

            if (i_6 < 0) {
                i_6 = 0;
            }

            int i_7 = FontMetrics.useBoldFont ? fontmetrics3.topPadding + 1 + 20 : 31;
            int i_8 = i_1 - i_7 + fontmetrics3.topPadding + 1;
            if (i_9 + i_8 > GameShell.screenCanvasHeight * -969250379) {
                i_8 = GameShell.screenCanvasHeight * -969250379 - i_9;
            }

            if (i_8 < 0) {
                i_8 = 0;
            }

            MiniMenu.anInt3996 = i_6;
            MiniMenu.anInt9471 = i_8;
            MiniMenu.anInt8518 = i_4;
            MiniMenu.aCacheableNodeSub15163 = class282Sub50Sub150;
        }

    }

    public static void method4800() {
        if ((entryCount >= 2 || client.interfaceSelectionActive) && client.draggingComponent == null) {
            String menuText;
            if (client.interfaceSelectionActive && entryCount < 2) {
                menuText = client.useOnType + LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + client.useOnTarget + " " + "->";
            } else if (KeyDownCommand.minimenuKeyDown() && entryCount > 2) {
                menuText = MiniMenu.getTextFrom(MiniMenu.aCacheableNodeSub79445);
            } else {
                MiniMenuEntry menuEntry = MiniMenu.aCacheableNodeSub79445;
                if (menuEntry == null) {
                    return;
                }
                menuText = MiniMenu.getTextFrom(menuEntry);
                int[] quests = MiniMenu.getQuestsFrom(menuEntry);
                if (quests != null) {
                    menuText += ChatBox.addQuestChatIcons(quests);
                }
            }
            if (entryCount > 2) {
                menuText = menuText + TextUtils.setTextColor(16777215) + " / " + (entryCount - 2) + LocalizedText.MORE_OPTIONS.translate(client.CURRENT_LANGUAGE);
            }
            if (InterfaceComponentRenderer.aClass118183 != null) {
                AbstractFontRenderer fontRenderer = InterfaceComponentRenderer.aClass118183.getFontRenderer(StaticMethods.initializedFontcombos, client.FONT_PROVIDER);
                if (fontRenderer == null) {
                    fontRenderer = MiniMenu.minimenuHeader;
                }
                fontRenderer.renderTextAntiMacro(menuText, InterfaceComponentRenderer.anInt195, InterfaceComponentRenderer.anInt179, InterfaceComponentRenderer.aClass118183.width, InterfaceComponentRenderer.aClass118183.height, InterfaceComponentRenderer.aClass118183.color, InterfaceComponentRenderer.aClass118183.spriteShadow, InterfaceComponentRenderer.aClass118183.textHorizontalAli, InterfaceComponentRenderer.aClass118183.textVerticalAli, client.RANDOMIZER, client.randomSeed, client.anIntArray7438, StaticMedia.chatIcons, StaticMedia.chatScaleHeights);
                InterfaceComponentRenderer.inboundsComponentToTop(client.anIntArray7438[0], client.anIntArray7438[1], client.anIntArray7438[2], client.anIntArray7438[3]);
            }
        }
    }

    public static String method7303(MiniMenuEntry entry) {
        return !MiniMenu.isOpen && entry != null ? ((entry.coloredName == null || entry.coloredName.isEmpty())
        		&& entry.aString9585 != null && !entry.aString9585.isEmpty() ? entry.aString9585 : entry.coloredName) : "";
    }

    public static MiniMenuEntry method8727() {
        return MiniMenu.aCacheableNodeSub7168;
    }

    public static String addQuestChaticons(MiniMenuEntry menuEntry) {
        if (!MiniMenu.isOpen && menuEntry != null) {
            int[] quests = MiniMenu.getQuestsFrom(menuEntry);
            return quests == null ? "" : ChatBox.addQuestChatIcons(quests);
        } else {
            return "";
        }
    }

    public static int getClickType(MiniMenuEntry menuEntry) {
        if (MiniMenu.isOpen) {
            return 6;
        } else if (menuEntry == null) {
            return 0;
        } else {
            int clickType = menuEntry.clickType;
            return Mouse.method539(clickType) ? 1 : (Mouse.isClicktypeObj(clickType) ? 2 : (Mouse.isClicktypeLoc(clickType) ? 3 : (Mouse.isClicktypeNpc(clickType) ? 4 : (StaticMethods.method6176(clickType) ? 7 : (clickType == 16 ? 8 : 5)))));
        }
    }

    public static void resetEntries() {
        for (ColoredMiniMenuEntry class282Sub50Sub151 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getNext(); class282Sub50Sub151 != null; class282Sub50Sub151 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getDirectNext()) {
            if (class282Sub50Sub151.queueSize > 1) {
                class282Sub50Sub151.queueSize = 0;
                MiniMenu.aClass229164.put(class282Sub50Sub151, ((MiniMenuEntry) class282Sub50Sub151.nodeQueue.current.next).bitpacked);
                class282Sub50Sub151.nodeQueue.reset();
            }
        }

        MiniMenu.anInt170 = 0;
        entryCount = 0;
        MiniMenu.aClass482171.remove_all();
        MiniMenu.aClass465172.clear();
        MiniMenu.aClass477182.reset();
        addEntry(MiniMenu.cancelOption);
    }

	public static void method420() {
	    for (MiniMenuEntry menuEntry = (MiniMenuEntry) MiniMenu.aClass482171.getFirst(); menuEntry != null; menuEntry = (MiniMenuEntry) MiniMenu.aClass482171.next()) {
	        if (Mouse.method539(menuEntry.clickType)) {
	            MiniMenu.method2095(menuEntry);
	        }
	    }	
	}

	public static boolean method12562() {
	    if (aCacheableNodeSub712 == null) {
	        return false;
	    } else {
	        if (aCacheableNodeSub712.clickType >= 2000) {
	            aCacheableNodeSub712.clickType -= 2000;
	        }
	        return aCacheableNodeSub712.clickType == 1007;
	    }
	}

	public static int getPrefixTextWidth(ColoredMiniMenuEntry coloredChatText, FontMetrics fontMetrics) {
	    String formattedText = MiniMenu.formatTag(coloredChatText);
	    return fontMetrics.getTextWidth(formattedText, StaticMedia.chatIcons);
	}

	public static String formatTag(ColoredMiniMenuEntry coloredChatText) {
	    return coloredChatText.tagStart + TextUtils.setTextColor(16777215) + " >";
	}

	public static String getTextFrom(MiniMenuEntry menuEntry) {
		return menuEntry.aString9585 != null && !menuEntry.aString9585.isEmpty()
				? (menuEntry.coloredName != null && !menuEntry.coloredName.isEmpty() ? menuEntry.action
						+ LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + menuEntry.coloredName
						+ LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + menuEntry.aString9585
						: menuEntry.action + LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE)
								+ menuEntry.aString9585)
				: (menuEntry.coloredName != null && !menuEntry.coloredName.isEmpty() ? menuEntry.action
						+ LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + menuEntry.coloredName
						: menuEntry.action);
	}

	public static int[] getQuestsFrom(MiniMenuEntry menuEntry) {
	    int[] quests = null;
	    if (Mouse.isClicktypeObj(menuEntry.clickType)) {
	        quests = TypeLists.OBJ.list((int) menuEntry.entityUid).quests;
	    } else if (menuEntry.itemId != -1) {
	        quests = TypeLists.OBJ.list(menuEntry.itemId).quests;
	    } else if (Mouse.isClicktypeNpc(menuEntry.clickType)) {
	        ObjectNode objectNode = client.NPC_MAP.get((int) menuEntry.entityUid);
	        if (objectNode != null) {
	            NpcEntity npcEntity = (NpcEntity) objectNode.value;
	            NPCType npcType = npcEntity.definitions;
	            if (npcType.alternativeIds != null) {
	                npcType = npcType.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER);
	            }
	            if (npcType != null) {
	                quests = npcType.quests;
	            }
	        }
	    } else if (Mouse.isClicktypeLoc(menuEntry.clickType)) {
	        ObjectType locType = Resource.mapRegion.getLoctypeList().list((int) (menuEntry.entityUid >>> 32 & 0x7fffffffL));
	        if (locType.alternativeIds != null) {
	            locType = locType.getMultiLoc(VarProviders.PLAYER_VAR_PROVIDER);
	        }
	        if (locType != null) {
	            quests = locType.quests;
	        }
	    }
	    return quests;
	}

	public static boolean isOpen;

	public static void addEntry(String action, String coloredName, int cursor, int clickType, int id, long long_5, int i_7, int i_8, boolean bool9, boolean bool10, long bitpacked, boolean bool13) {
	    if (!isOpen && entryCount < 520) {
	        cursor = cursor != -1 ? cursor : client.defaultMenuCursor;
	        MiniMenuEntry menuEntry = new MiniMenuEntry(action, coloredName, cursor, clickType, id, long_5, i_7, i_8, bool9, bool10, bitpacked, bool13);
	        addEntry(menuEntry);
	    }
	}

    public static void method2095(MiniMenuEntry entry) {
	    if (!isOpen) {
	        entry.unlink();
	        --entryCount;
	        if (!entry.aBool9586) {
	            long long_2 = entry.bitpacked;
	            ColoredMiniMenuEntry class282Sub50Sub154;
	            for (class282Sub50Sub154 = (ColoredMiniMenuEntry) MiniMenu.aClass465172.get(long_2); class282Sub50Sub154 != null && !class282Sub50Sub154.tagStart.equals(entry.coloredName); class282Sub50Sub154 = (ColoredMiniMenuEntry) MiniMenu.aClass465172.nextInBucket()) {
	            }
	            if (class282Sub50Sub154 != null && class282Sub50Sub154.removeEntry(entry)) {
	                MiniMenu.method503(class282Sub50Sub154);
	            }
	        } else {
	            for (ColoredMiniMenuEntry class282Sub50Sub155 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getNext(); class282Sub50Sub155 != null; class282Sub50Sub155 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getDirectNext()) {
	                if (class282Sub50Sub155.tagStart.equals(entry.coloredName)) {
	                    boolean bool3 = false;
	                    for (MiniMenuEntry class282Sub50Sub76 = (MiniMenuEntry) class282Sub50Sub155.nodeQueue.getNext(); class282Sub50Sub76 != null; class282Sub50Sub76 = (MiniMenuEntry) class282Sub50Sub155.nodeQueue.getDirectNext()) {
	                        if (class282Sub50Sub76 == entry) {
	                            if (class282Sub50Sub155.removeEntry(entry)) {
	                                MiniMenu.method503(class282Sub50Sub155);
	                            }
	                            bool3 = true;
	                            break;
	                        }
	                    }
	                    if (bool3) {
	                        break;
	                    }
	                }
	            }
	        }
	    }
	}

	public static final NodeCollection aClass482171 = new NodeCollection();

	public static boolean method5334(int clicktypeBase, int clicktypeSub) {
	    return clicktypeSub >= 1000 && clicktypeBase < 1000 || (clicktypeSub < 1000 && clicktypeBase < 1000 ? MiniMenu.method13788(clicktypeBase) || !MiniMenu.method13788(clicktypeSub) : clicktypeSub >= 1000);
	}

	public static boolean method13788(int clickType) {
	    return clickType == 59 || clickType == 2 || clickType == 8 || clickType == 17 || clickType == 15 || clickType == 16 || clickType == 58;
	}

	public static void reset() {
	    for (ColoredMiniMenuEntry class282Sub50Sub151 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getNext(); class282Sub50Sub151 != null; class282Sub50Sub151 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getDirectNext()) {
	        if (class282Sub50Sub151.queueSize > 1) {
	            class282Sub50Sub151.queueSize = 0;
	            MiniMenu.aClass229164.put(class282Sub50Sub151, ((MiniMenuEntry) class282Sub50Sub151.nodeQueue.current.next).bitpacked);
	            class282Sub50Sub151.nodeQueue.reset();
	        }
	    }
	    MiniMenu.anInt170 = 0;
	    entryCount = 0;
	    aClass482171.remove_all();
	    MiniMenu.aClass465172.clear();
	    MiniMenu.aClass477182.reset();
	    isOpen = false;
	}

	public static void method15411(AbstractToolkit toolkit) {
	    if (isOpen) {
	        MiniMenu.method6216(toolkit);
	    } else {
	        method4800();
	    }
	
	}

	public static boolean hasEntries() {
	    return entryCount > 0;
	}

	public static void method503(ColoredMiniMenuEntry class282Sub50Sub150) {
	    boolean bool2 = false;
	    class282Sub50Sub150.unlinkSub();
	    for (ColoredMiniMenuEntry class282Sub50Sub153 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getNext(); class282Sub50Sub153 != null; class282Sub50Sub153 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getDirectNext()) {
	        if (method5334(class282Sub50Sub150.getClickType(), class282Sub50Sub153.getClickType())) {
	            CacheableNode.rebuild(class282Sub50Sub150, class282Sub50Sub153);
	            bool2 = true;
	            break;
	        }
	    }
	    if (!bool2) {
	        MiniMenu.aClass477182.insert(class282Sub50Sub150);
	    }
	}

	public static float method288(float deltaX, float deltaY, float deltaZ, float x, float y, float z, int i_6) {
	    float f_8 = 0.0F;
	    float f_9 = x - deltaX;
	    float f_10 = y - deltaY;
	    float f_11 = z - deltaZ;
	    float f_12 = 0.0F;
	    float f_13 = 0.0F;
	    float f_14 = 0.0F;
	    for (Scene scene = Resource.mapRegion.getScene(); f_8 < 1.1F; f_8 += 0.1F) {
	        float absX = f_8 * f_9 + deltaX;
	        float absY = f_10 * f_8 + deltaY;
	        float absZ = deltaZ + f_8 * f_11;
	        int sceneX = (int) absX >> 9;
	        int sceneZ = (int) absZ >> 9;
	        if (sceneX > 0 && sceneZ > 0 && sceneX < Resource.mapRegion.getSceneSizeX() && sceneZ < Resource.mapRegion.getSceneSizeY()) {
	            int plane = PlayerUpdate.MY_PLAYER.plane;
	            if (plane < 3 && (Resource.mapRegion.getSettingsBits().tileFunctionMasks[1][sceneX][sceneZ] & 0x2) != 0) {
	                plane++;
	            }
	            int height = scene.surfaceHeightmap[plane].getAverageHeight((int) absX, (int) absZ);
	            if (height < absY) {
	                if (i_6 >= 2) {
	                    return f_8 - 0.1F + method288(f_12, f_13, f_14, absX, absY, absZ, i_6 - 1) * 0.1F;
	                }
	                return f_8;
	            }
	        }
	        f_12 = absX;
	        f_13 = absY;
	        f_14 = absZ;
	    }
	    return -1.0f;
	}

	public static MiniMenuEntry aCacheableNodeSub7438;
	public static int anInt9471;

	public static void method4908(AbstractToolkit toolkit) {
	    int fillColor = -10660793;
	    AbstractToolkit.drawOutlinedFilledOpaqueRectangle(toolkit, StaticMethods.anInt3555, StaticMethods.anInt2300, MiniMenu.anInt8975, MiniMenu.anInt1104, fillColor);
	    MiniMenu.minimenuHeader.renderPlain(LocalizedText.CHOOSE_OPTION.translate(client.CURRENT_LANGUAGE), StaticMethods.anInt3555 + 3, StaticMethods.anInt2300 + 14, fillColor, -1);
	    int mouseX = MouseRecord.recorder.getMouseX();
	    int mouseY = MouseRecord.recorder.getMouseY();
	    int i_5;
	    MiniMenuEntry class282Sub50Sub76;
	    int i_7;
	    if (!MiniMenu.aBool162) {
	        i_5 = 0;
	        for (class282Sub50Sub76 = (MiniMenuEntry) aClass482171.getFirst(); class282Sub50Sub76 != null; class282Sub50Sub76 = (MiniMenuEntry) aClass482171.next()) {
	            i_7 = (entryCount - 1 - i_5) * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31;
	            MiniMenu.method12400(mouseX, mouseY, StaticMethods.anInt3555, MiniMenu.anInt8975, i_7, class282Sub50Sub76, MiniMenu.minimenuHeader, FontMetrics.aClass4143438, -1, -256);
	            ++i_5;
	        }
	    } else {
	        i_5 = 0;
	        for (ColoredMiniMenuEntry class282Sub50Sub158 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getNext(); class282Sub50Sub158 != null; class282Sub50Sub158 = (ColoredMiniMenuEntry) MiniMenu.aClass477182.getDirectNext()) {
	            i_7 = i_5 * MiniMenu.anInt178 + StaticMethods.anInt2300 + 31;
	            if (class282Sub50Sub158.queueSize == 1) {
	                MiniMenu.method12400(mouseX, mouseY, StaticMethods.anInt3555, MiniMenu.anInt8975, i_7, (MiniMenuEntry) class282Sub50Sub158.nodeQueue.current.next, MiniMenu.minimenuHeader, FontMetrics.aClass4143438, -1, -256);
	            } else {
	                StaticMethods.method1630(mouseX, mouseY, StaticMethods.anInt3555, MiniMenu.anInt8975, i_7, class282Sub50Sub158, MiniMenu.minimenuHeader, FontMetrics.aClass4143438, -1, -256);
	            }
	            ++i_5;
	        }
	        if (MiniMenu.aCacheableNodeSub15163 != null) {
	            AbstractToolkit.drawOutlinedFilledOpaqueRectangle(toolkit, MiniMenu.anInt3996, anInt9471, MiniMenu.anInt8518, StationaryWall.anInt10526, fillColor);
	            MiniMenu.minimenuHeader.renderPlain(MiniMenu.aCacheableNodeSub15163.tagStart, MiniMenu.anInt3996 + 3, anInt9471 + 14, fillColor, -1);
	            i_5 = 0;
	            for (class282Sub50Sub76 = (MiniMenuEntry) MiniMenu.aCacheableNodeSub15163.nodeQueue.getNext(); class282Sub50Sub76 != null; class282Sub50Sub76 = (MiniMenuEntry) MiniMenu.aCacheableNodeSub15163.nodeQueue.getDirectNext()) {
	                i_7 = i_5 * MiniMenu.anInt178 + anInt9471 + 31;
	                MiniMenu.method12400(mouseX, mouseY, MiniMenu.anInt3996, MiniMenu.anInt8518, i_7, class282Sub50Sub76, MiniMenu.minimenuHeader, FontMetrics.aClass4143438, -1, -256);
	                ++i_5;
	            }
	        }
	    }
	}

	public static void close() {
	    MiniMenu.method6810();
	    isOpen = false;
	    InterfaceComponentRenderer.inboundsComponentToTop(StaticMethods.anInt3555, StaticMethods.anInt2300, MiniMenu.anInt8975, MiniMenu.anInt1104);
	    MiniMenu.aCacheableNodeSub79445 = null;
	    MiniMenu.aCacheableNodeSub7168 = null;
	}

	public static boolean containsEntryForNpc(int uid) {
	    for (MiniMenuEntry entry = (MiniMenuEntry) aClass482171.getFirst(); entry != null; entry = (MiniMenuEntry) aClass482171.next()) {
	        if (Mouse.isClicktypeNpc(entry.clickType) && uid == entry.entityUid) {
	            return true;
	        }
	    }
	    return false;
	}

	public static int anInt170;
	public static int anInt198 = -1;
	public static MiniMenuEntry cancelOption;
	public static MiniMenuEntry aCacheableNodeSub7168;
	public static int anInt178 = 16;
	public static boolean aBool162;
	public static ColoredMiniMenuEntry aCacheableNodeSub15163;
	public static int anInt167;
	public static final IterableNodeMap aClass465172 = new IterableNodeMap(16);
	public static final Queue aClass477182 = new Queue();
	public static final LRUCache aClass229164 = new LRUCache(30);
	public static int anInt180 = -1;
	public static int anInt181 = -1;

	public static int getMinimenuCursor() {
	    if (client.draggingComponent == null) {
	        if (!isOpen) {
	            if (TypeLists.INTERACTION_DEFAULTS.minimenuCursor) { // own cursor?
	                if (aCacheableNodeSub7168 != null) {
	                    return aCacheableNodeSub7168.cursor;
	                }
	            } else if (MiniMenu.aCacheableNodeSub79445 != null) { // default cursor?
	                return MiniMenu.aCacheableNodeSub79445.cursor;
	            }
	        } else {
	            FontMetrics minimenuFont = FontMetrics.getForMinimenu();
	            int mouseX = MouseRecord.recorder.getMouseX();
	            int mouseY = MouseRecord.recorder.getMouseY();
	            int i_4;
	            int i_5;
	            MiniMenuEntry class282Sub50Sub77;
	            int i_8;
	            if (!aBool162) {
	                if (mouseX > StaticMethods.anInt3555 && mouseX < StaticMethods.anInt3555 + MiniMenu.anInt8975) {
	                    i_4 = -1;
	                    for (i_5 = 0; i_5 < entryCount; i_5++) {
	                        if (FontMetrics.useBoldFont) {
	                            i_8 = (entryCount - 1 - i_5) * anInt178 + minimenuFont.topPadding + StaticMethods.anInt2300 + 1 + 20;
	                        } else {
	                            i_8 = (entryCount - 1 - i_5) * anInt178 + StaticMethods.anInt2300 + 31;
	                        }
	                        if (mouseY > i_8 - minimenuFont.topPadding - 1 && mouseY < i_8 + minimenuFont.bottomPadding) {
	                            i_4 = i_5;
	                        }
	                    }
	                    if (i_4 != -1) {
	                        i_5 = 0;
	                        NodeIterator nodeiterator6 = new NodeIterator(aClass482171);
	                        for (class282Sub50Sub77 = (MiniMenuEntry) nodeiterator6.method7683(); class282Sub50Sub77 != null; class282Sub50Sub77 = (MiniMenuEntry) nodeiterator6.next()) {
	                            if (i_5++ == i_4) {
	                                return class282Sub50Sub77.cursor;
	                            }
	                        }
	                    }
	                }
	            } else {
	                QueueIterator class4699;
	                if (mouseX > StaticMethods.anInt3555 && mouseX < StaticMethods.anInt3555 + MiniMenu.anInt8975) {
	                    i_4 = -1;
	                    for (i_5 = 0; i_5 < anInt170; i_5++) {
	                        if (FontMetrics.useBoldFont) {
	                            i_8 = i_5 * anInt178 + minimenuFont.topPadding + StaticMethods.anInt2300 + 1 + 20;
	                        } else {
	                            i_8 = i_5 * anInt178 + StaticMethods.anInt2300 + 31;
	                        }
	                        if (mouseY > i_8 - minimenuFont.topPadding - 1 && mouseY < i_8 + minimenuFont.bottomPadding) {
	                            i_4 = i_5;
	                        }
	                    }
	                    if (i_4 != -1) {
	                        i_5 = 0;
	                        class4699 = new QueueIterator(aClass477182);
	                        for (ColoredMiniMenuEntry class282Sub50Sub1510 = (ColoredMiniMenuEntry) class4699.method7790(); class282Sub50Sub1510 != null; class282Sub50Sub1510 = (ColoredMiniMenuEntry) class4699.next()) {
	                            if (i_5++ == i_4) {
	                                return ((MiniMenuEntry) class282Sub50Sub1510.nodeQueue.current.next).cursor;
	                            }
	                        }
	                    }
	                } else if (aCacheableNodeSub15163 != null && mouseX > MiniMenu.anInt3996 && mouseX < MiniMenu.anInt8518 + MiniMenu.anInt3996) {
	                    i_4 = -1;
	                    for (i_5 = 0; i_5 < aCacheableNodeSub15163.queueSize; i_5++) {
	                        if (FontMetrics.useBoldFont) {
	                            i_8 = i_5 * anInt178 + minimenuFont.topPadding + anInt9471 + 1 + 20;
	                        } else {
	                            i_8 = i_5 * anInt178 + anInt9471 + 31;
	                        }
	                        if (mouseY > i_8 - minimenuFont.topPadding - 1 && mouseY < i_8 + minimenuFont.bottomPadding) {
	                            i_4 = i_5;
	                        }
	                    }
	                    if (i_4 != -1) {
	                        i_5 = 0;
	                        class4699 = new QueueIterator(aCacheableNodeSub15163.nodeQueue);
	                        for (class282Sub50Sub77 = (MiniMenuEntry) class4699.method7790(); class282Sub50Sub77 != null; class282Sub50Sub77 = (MiniMenuEntry) class4699.next()) {
	                            if (i_5++ == i_4) {
	                                return class282Sub50Sub77.cursor;
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	    return -1;
	}

	public static void method6810() {
	    if (aCacheableNodeSub15163 != null) {
	        aCacheableNodeSub15163 = null;
	        InterfaceComponentRenderer.inboundsComponentToTop(MiniMenu.anInt3996, anInt9471, MiniMenu.anInt8518, StationaryWall.anInt10526);
	    }
	
	}

	public static int anInt3996;

	public static void method857(AbstractToolkit toolkit) {
	    int i_2 = StaticMethods.anInt3555;
	    int i_3 = StaticMethods.anInt2300;
	    int i_4 = MiniMenu.anInt8975;
	    int i_5 = MiniMenu.anInt1104 - 3;
	    byte b_6 = 20;
	    if (FontMetrics.aFontRenderer2348 == null || StaticMedia.aClass4142200 == null) {
	        FontMetrics.aFontRenderer2348 = (AbstractFontRenderer) StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, StaticMethods.anInt2982, true, true);
	        StaticMedia.aClass4142200 = StaticMethods.initializedFontcombos.list(client.FONT_PROVIDER, StaticMethods.anInt2982);
	        if (FontMetrics.aFontRenderer2348 != null && StaticMedia.aClass4142200 != null) {
	            close();
	            int i_15 = i_4 / 2 + i_2;
	            if (i_15 + i_4 > GameShell.screenCanvasWidth) {
	                i_15 = GameShell.screenCanvasWidth - i_4;
	            }
	            if (i_15 < 0) {
	                i_15 = 0;
	            }
	            StaticMethods.method4653(i_15, StaticMethods.anInt2300);
	            return;
	        }
	    }
	    AbstractFontRenderer fontrenderer7;
	    if (FontMetrics.aFontRenderer2348 != null && StaticMedia.aClass4142200 != null) {
	        fontrenderer7 = FontMetrics.aFontRenderer2348;
	    } else {
	        fontrenderer7 = MiniMenu.minimenuHeader;
	    }
	    FontMetrics fontmetrics8 = FontMetrics.getForMinimenu();
	    NativeSprite.method12680(toolkit, StaticMethods.anInt3555, StaticMethods.anInt2300, MiniMenu.anInt8975, MiniMenu.anInt1104, b_6, fontrenderer7, fontmetrics8, LocalizedText.CHOOSE_OPTION.translate(client.CURRENT_LANGUAGE));
	    int alpha = 255 - StaticMethods.anInt2262 - NativeSprite.anInt156;
	    if (alpha < 0) {
	        alpha = 0;
	    }
	    int i_10 = MouseRecord.recorder.getMouseX();
	    int i_11 = MouseRecord.recorder.getMouseY();
	    int i_12;
	    MiniMenuEntry class282Sub50Sub713;
	    int i_14;
	    ColoredMiniMenuEntry class282Sub50Sub1516;
	    if (!aBool162) {
	        i_12 = 0;
	        for (class282Sub50Sub713 = (MiniMenuEntry) aClass482171.getFirst(); class282Sub50Sub713 != null; class282Sub50Sub713 = (MiniMenuEntry) aClass482171.next()) {
	            i_14 = i_3 + b_6 + (entryCount - 1 - i_12) * anInt178 + fontmetrics8.topPadding + 1;
	            if (i_10 > StaticMethods.anInt3555 && i_10 < StaticMethods.anInt3555 + MiniMenu.anInt8975 && i_11 > i_14 - fontmetrics8.topPadding - 1 && i_11 < i_14 + fontmetrics8.bottomPadding && class282Sub50Sub713.aBool9583) {
	                toolkit.drawFilledRectangle(StaticMethods.anInt3555, i_14 - fontmetrics8.topPadding, MiniMenu.anInt8975, anInt178, alpha << 24 | MiniMenu.anInt188, 1);
	            }
	            ++i_12;
	        }
	    } else {
	        i_12 = 0;
	        for (class282Sub50Sub1516 = (ColoredMiniMenuEntry) aClass477182.getNext(); class282Sub50Sub1516 != null; class282Sub50Sub1516 = (ColoredMiniMenuEntry) aClass477182.getDirectNext()) {
	            i_14 = i_3 + b_6 + i_12 * anInt178 + fontmetrics8.topPadding + 1;
	            if (i_10 > StaticMethods.anInt3555 && i_10 < MiniMenu.anInt8975 + StaticMethods.anInt3555 && i_11 > i_14 - fontmetrics8.topPadding - 1 && i_11 < i_14 + fontmetrics8.bottomPadding && (class282Sub50Sub1516.queueSize > 1 || ((MiniMenuEntry) class282Sub50Sub1516.nodeQueue.current.next).aBool9583)) {
	                toolkit.drawFilledRectangle(StaticMethods.anInt3555, i_14 - fontmetrics8.topPadding, MiniMenu.anInt8975, anInt178, alpha << 24 | MiniMenu.anInt188, 1);
	            }
	            ++i_12;
	        }
	        if (aCacheableNodeSub15163 != null) {
	            NativeSprite.method12680(toolkit, anInt3996, anInt9471, MiniMenu.anInt8518, StationaryWall.anInt10526, b_6, fontrenderer7, fontmetrics8, aCacheableNodeSub15163.tagStart);
	            i_12 = 0;
	            for (class282Sub50Sub713 = (MiniMenuEntry) aCacheableNodeSub15163.nodeQueue.getNext(); class282Sub50Sub713 != null; class282Sub50Sub713 = (MiniMenuEntry) aCacheableNodeSub15163.nodeQueue.getDirectNext()) {
	                i_14 = b_6 + i_12 * anInt178 + fontmetrics8.topPadding + anInt9471 + 1;
	                if (i_10 > anInt3996 && i_10 < MiniMenu.anInt8518 + anInt3996 && i_11 > i_14 - fontmetrics8.topPadding - 1 && i_11 < i_14 + fontmetrics8.bottomPadding && class282Sub50Sub713.aBool9583) {
	                    toolkit.drawFilledRectangle(anInt3996, i_14 - fontmetrics8.topPadding, MiniMenu.anInt8518, anInt178, alpha << 24 | MiniMenu.anInt188, 1);
	                }
	                ++i_12;
	            }
	            StaticMethods.method8209(toolkit, anInt3996, anInt9471, MiniMenu.anInt8518, StationaryWall.anInt10526, b_6);
	        }
	    }
	    StaticMethods.method8209(toolkit, StaticMethods.anInt3555, StaticMethods.anInt2300, MiniMenu.anInt8975, MiniMenu.anInt1104, b_6);
	    if (!aBool162) {
	        i_12 = 0;
	        for (class282Sub50Sub713 = (MiniMenuEntry) aClass482171.getFirst(); class282Sub50Sub713 != null; class282Sub50Sub713 = (MiniMenuEntry) aClass482171.next()) {
	            i_14 = i_3 + b_6 + (entryCount - 1 - i_12) * anInt178 + fontmetrics8.topPadding + 1;
	            MiniMenu.method12400(i_10, i_11, i_2, i_4, i_14, class282Sub50Sub713, fontrenderer7, fontmetrics8, MiniMenu.anInt4722 | ~0xffffff, StaticMethods.anInt1005 | ~0xffffff);
	            ++i_12;
	        }
	    } else {
	        i_12 = 0;
	        for (class282Sub50Sub1516 = (ColoredMiniMenuEntry) aClass477182.getNext(); class282Sub50Sub1516 != null; class282Sub50Sub1516 = (ColoredMiniMenuEntry) aClass477182.getDirectNext()) {
	            i_14 = b_6 + i_12 * anInt178 + fontmetrics8.topPadding + StaticMethods.anInt2300 + 1;
	            if (class282Sub50Sub1516.queueSize == 1) {
	                MiniMenu.method12400(i_10, i_11, StaticMethods.anInt3555, MiniMenu.anInt8975, i_14, (MiniMenuEntry) class282Sub50Sub1516.nodeQueue.current.next, fontrenderer7, fontmetrics8, MiniMenu.anInt4722 | ~0xffffff, StaticMethods.anInt1005 | ~0xffffff);
	            } else {
	                StaticMethods.method1630(i_10, i_11, StaticMethods.anInt3555, MiniMenu.anInt8975, i_14, class282Sub50Sub1516, fontrenderer7, fontmetrics8, MiniMenu.anInt4722 | ~0xffffff, StaticMethods.anInt1005 | ~0xffffff);
	            }
	            ++i_12;
	        }
	        if (aCacheableNodeSub15163 != null) {
	            i_12 = 0;
	            for (class282Sub50Sub713 = (MiniMenuEntry) aCacheableNodeSub15163.nodeQueue.getNext(); class282Sub50Sub713 != null; class282Sub50Sub713 = (MiniMenuEntry) aCacheableNodeSub15163.nodeQueue.getDirectNext()) {
	                i_14 = b_6 + i_12 * anInt178 + fontmetrics8.topPadding + anInt9471 + 1;
	                MiniMenu.method12400(i_10, i_11, anInt3996, MiniMenu.anInt8518, i_14, class282Sub50Sub713, fontrenderer7, fontmetrics8, MiniMenu.anInt4722 | ~0xffffff, StaticMethods.anInt1005 | ~0xffffff);
	                ++i_12;
	            }
	        }
	    }
	}

	public static void method6216(AbstractToolkit toolkit) {
	    if (!FontMetrics.useBoldFont) {
	        method4908(toolkit);
	    } else {
	        method857(toolkit);
	    }
	}

	public static MiniMenuEntry aCacheableNodeSub79445;
	public static int anInt8975;
	public static int anInt1104;
	public static int useOnCursor;
	public static AbstractFontRenderer minimenuHeader;
	public static int anInt188;
	public static int USE_INTERFACE_ID;

	public static final NodeCollection aClass482174 = new NodeCollection();
	public static final NodeCollection aClass482175 = new NodeCollection();

	public static void method6451(int i_0, int i_1) {
	    anInt180 = i_0;
	    anInt181 = i_1;
	}

	public static void addCancelOption() {
	    cancelOption = new MiniMenuEntry(LocalizedText.CANCEL.translate(client.CURRENT_LANGUAGE), "", client.defaultMenuCursor, 1006, -1, 0L, 0, 0, true, false, 0L, true);
	}

	public static int anInt4722;

	public static void method12400(int i_0, int i_1, int i_2, int i_4, int i_6, MiniMenuEntry entry, AbstractFontRenderer fontRenderer, FontMetrics fontmetrics9, int i_10, int i_11) {
	    int i_101 = i_10;
	    if (i_0 > i_2 && i_0 < i_2 + i_4 && i_1 > i_6 - fontmetrics9.topPadding - 1 && i_1 < fontmetrics9.bottomPadding + i_6 && entry.aBool9583) {
	        i_101 = i_11;
	    }
	    int[] questIds = getQuestsFrom(entry);
	    String text = getTextFrom(entry);
	    if (questIds != null) {
	        text += ChatBox.addQuestChatIcons(questIds);
	    }
	    fontRenderer.method386(text, i_2 + 3, i_6, i_101, client.RANDOMIZER, client.randomSeed, StaticMedia.chatIcons, StaticMedia.chatScaleHeights);
	    if (entry.aBool9577) {
	        StaticMedia.otherLevelSprite.draw(5 + i_2 + fontmetrics9.getWidthNoSprites(text), i_6 - fontmetrics9.topPadding);
	    }
	}

	public static int anInt8518;
}
