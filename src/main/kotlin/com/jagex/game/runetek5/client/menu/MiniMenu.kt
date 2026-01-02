@file:Suppress("UNCHECKED_CAST", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "PLATFORM_CLASS_MAPPED_TO_KOTLIN", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "SENSELESS_COMPARISON")
package com.jagex.game.runetek5.client.menu

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.NodeIterator
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.keyboard.KeyDownCommand
import com.jagex.game.runetek5.client.input.mouse.Mouse
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.entity.gameobject.StationaryWall
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.AbstractFontRenderer
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.vardomain.VarProviders

object MiniMenu {

    @JvmField
    var entryCount = 0

    @JvmField
    var aCacheableNodeSub712: MiniMenuEntry? = null

    @JvmStatic
    fun prefixWithIndex(srcOptions: Array<String?>?): Array<String?> {
        val dstOptions = arrayOfNulls<String>(5)
        for (index in 0 until 5) {
            dstOptions[index] = "$index "
            if (srcOptions != null && srcOptions[index] != null) {
                dstOptions[index] += srcOptions[index]
            }
        }
        return dstOptions
    }

    @JvmStatic
    fun addEntry(entry: MiniMenuEntry?) {
        if (entry != null) {
            aClass482171.addFirst(entry)
            entryCount++
            var c: ColoredMiniMenuEntry?
            if (!entry.aBool9586 && entry.coloredName != null && !entry.coloredName.isEmpty()) {
                val hash = entry.bitpacked
                c = aClass465172.get(hash) as? ColoredMiniMenuEntry
                while (c != null && !c.tagStart.equals(entry.coloredName)) {
                    c = aClass465172.nextInBucket() as? ColoredMiniMenuEntry
                }
                if (c == null) {
                    c = aClass229164.get(hash) as? ColoredMiniMenuEntry
                    if (c != null && !c.tagStart.equals(entry.coloredName)) {
                        c = null
                    }
                    if (c == null) {
                        c = ColoredMiniMenuEntry(entry.coloredName)
                    }
                    aClass465172.put(c, hash)
                    ++anInt170
                }
            } else {
                c = ColoredMiniMenuEntry(entry.coloredName)
                ++anInt170
            }
            if (c.addEntry(entry)) {
                method503(c)
            }
        }
    }

    @JvmStatic
    fun getWidthForEntryWithQuests(menuEntry: MiniMenuEntry, fontmetrics: FontMetrics): Int {
        var text = getTextFrom(menuEntry)
        val quests = getQuestsFrom(menuEntry)
        if (quests != null) {
            text += ChatBox.addQuestChatIcons(quests)
        }
        @Suppress("UNCHECKED_CAST")
        var width = fontmetrics.getTextWidth(text, StaticMedia.chatIcons as Array<com.jagex.game.runetek5.graphics.sprite.Sprite>?)
        if (menuEntry.aBool9577) {
            width += (StaticMedia.otherLevelSprite?.getWidth() ?: 0) + 4
        }
        return width
    }

    @JvmStatic
    fun method13768(class282Sub50Sub70: MiniMenuEntry?): String {
        return if (!isOpen && class282Sub50Sub70 != null) class282Sub50Sub70.action else ""
    }

    @JvmStatic
    fun method12212(class282Sub50Sub150: ColoredMiniMenuEntry, i_1: Int) {
        if (isOpen) {
            val fontmetrics3 = FontMetrics.getForMinimenu()
            var i_4 = 0

            var i_6: Int
            var class282Sub50Sub75: MiniMenuEntry? = class282Sub50Sub150.nodeQueue.getNext() as? MiniMenuEntry
            while (class282Sub50Sub75 != null) {
                i_6 = getWidthForEntryWithQuests(class282Sub50Sub75, fontmetrics3)
                if (i_6 > i_4) {
                    i_4 = i_6
                }
                class282Sub50Sub75 = class282Sub50Sub150.nodeQueue.getDirectNext() as? MiniMenuEntry
            }

            i_4 += 8
            val i_9 = class282Sub50Sub150.queueSize * anInt178 + 21
            StationaryWall.anInt10526 = (if (FontMetrics.useBoldFont) 26 else 22) + anInt178 * class282Sub50Sub150.queueSize
            i_6 = StaticMethods.anInt3555 + anInt8975
            if (i_4 + i_6 > GameShell.screenCanvasWidth) {
                i_6 = StaticMethods.anInt3555 - i_4
            }

            if (i_6 < 0) {
                i_6 = 0
            }

            val i_7 = if (FontMetrics.useBoldFont) fontmetrics3.topPadding + 1 + 20 else 31
            var i_8 = i_1 - i_7 + fontmetrics3.topPadding + 1
            if (i_9 + i_8 > GameShell.screenCanvasHeight * -969250379) {
                i_8 = GameShell.screenCanvasHeight * -969250379 - i_9
            }

            if (i_8 < 0) {
                i_8 = 0
            }

            anInt3996 = i_6
            anInt9471 = i_8
            anInt8518 = i_4
            aCacheableNodeSub15163 = class282Sub50Sub150
        }

    }

    @JvmStatic
    fun method4800() {
        if ((entryCount >= 2 || client.interfaceSelectionActive) && client.draggingComponent == null) {
            var menuText: String
            if (client.interfaceSelectionActive && entryCount < 2) {
                menuText = client.useOnType + LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + client.useOnTarget + " " + "->"
            } else if (KeyDownCommand.minimenuKeyDown() && entryCount > 2) {
                menuText = getTextFrom(aCacheableNodeSub79445!!)
            } else {
                val menuEntry = aCacheableNodeSub79445
                if (menuEntry == null) {
                    return
                }
                menuText = getTextFrom(menuEntry)
                val quests = getQuestsFrom(menuEntry)
                if (quests != null) {
                    menuText += ChatBox.addQuestChatIcons(quests)
                }
            }
            if (entryCount > 2) {
                menuText = menuText + TextUtils.setTextColor(16777215) + " / " + (entryCount - 2) + LocalizedText.MORE_OPTIONS.translate(client.CURRENT_LANGUAGE)
            }
            val comp = InterfaceComponentRenderer.aClass118183
            if (comp != null) {
                var fontRenderer = comp.getFontRenderer(StaticMethods.initializedFontcombos!!, client.FONT_PROVIDER)
                if (fontRenderer == null) {
                    fontRenderer = minimenuHeader
                }
                @Suppress("UNCHECKED_CAST")
                fontRenderer!!.renderTextAntiMacro(menuText, InterfaceComponentRenderer.anInt195, InterfaceComponentRenderer.anInt179, comp.width, comp.height, comp.color, comp.spriteShadow, comp.textHorizontalAli, comp.textVerticalAli, client.RANDOMIZER, client.randomSeed, client.anIntArray7438, StaticMedia.chatIcons as Array<com.jagex.game.runetek5.graphics.sprite.NativeSprite>?, StaticMedia.chatScaleHeights)
                InterfaceComponentRenderer.inboundsComponentToTop(client.anIntArray7438[0], client.anIntArray7438[1], client.anIntArray7438[2], client.anIntArray7438[3])
            }
        }
    }

    @JvmStatic
    fun method7303(entry: MiniMenuEntry?): String {
        if (!isOpen && entry != null) {
            val aString = entry.aString9585
            val colorName = entry.coloredName
            return if ((colorName == null || colorName.isEmpty()) && aString != null && !aString.isEmpty()) aString else (colorName ?: "")
        }
        return ""
    }

    @JvmStatic
    fun method8727(): MiniMenuEntry? {
        return aCacheableNodeSub7168
    }

    @JvmStatic
    fun addQuestChaticons(menuEntry: MiniMenuEntry?): String {
        if (!isOpen && menuEntry != null) {
            val quests = getQuestsFrom(menuEntry)
            return quests?.let { ChatBox.addQuestChatIcons(it) } ?: ""
        } else {
            return ""
        }
    }

    @JvmStatic
    fun getClickType(menuEntry: MiniMenuEntry?): Int {
        return if (isOpen) {
            6
        } else if (menuEntry == null) {
            0
        } else {
            val clickType = menuEntry.clickType
            when {
                Mouse.method539(clickType) -> 1
                Mouse.isClicktypeObj(clickType) -> 2
                Mouse.isClicktypeLoc(clickType) -> 3
                Mouse.isClicktypeNpc(clickType) -> 4
                StaticMethods.method6176(clickType) -> 7
                clickType == 16 -> 8
                else -> 5
            }
        }
    }

    @JvmStatic
    fun resetEntries() {
        var class282Sub50Sub151: ColoredMiniMenuEntry? = aClass477182.getNext() as? ColoredMiniMenuEntry
        while (class282Sub50Sub151 != null) {
            if (class282Sub50Sub151.queueSize > 1) {
                class282Sub50Sub151.queueSize = 0
                aClass229164.put(class282Sub50Sub151, ((class282Sub50Sub151.nodeQueue.current.nextCache as MiniMenuEntry).bitpacked))
                class282Sub50Sub151.nodeQueue.reset()
            }
            class282Sub50Sub151 = aClass477182.getDirectNext() as? ColoredMiniMenuEntry
        }

        anInt170 = 0
        entryCount = 0
        aClass482171.remove_all()
        aClass465172.clear()
        aClass477182.reset()
        addEntry(cancelOption)
    }

    @JvmStatic
    fun method420() {
        var menuEntry: MiniMenuEntry? = aClass482171.getFirst() as? MiniMenuEntry
        while (menuEntry != null) {
            if (Mouse.method539(menuEntry.clickType)) {
                method2095(menuEntry)
            }
            menuEntry = aClass482171.next() as? MiniMenuEntry
        }
    }

    @JvmStatic
    fun method12562(): Boolean {
        return if (aCacheableNodeSub712 == null) {
            false
        } else {
            if (aCacheableNodeSub712!!.clickType >= 2000) {
                aCacheableNodeSub712!!.clickType -= 2000
            }
            aCacheableNodeSub712!!.clickType == 1007
        }
    }

    @JvmStatic
    fun getPrefixTextWidth(coloredChatText: ColoredMiniMenuEntry, fontMetrics: FontMetrics): Int {
        val formattedText = formatTag(coloredChatText)
        @Suppress("UNCHECKED_CAST")
        return fontMetrics.getTextWidth(formattedText, StaticMedia.chatIcons as Array<com.jagex.game.runetek5.graphics.sprite.Sprite>?)
    }

    @JvmStatic
    fun formatTag(coloredChatText: ColoredMiniMenuEntry): String {
        return coloredChatText.tagStart + TextUtils.setTextColor(16777215) + " >"
    }

    @JvmStatic
    fun getTextFrom(menuEntry: MiniMenuEntry): String {
        val aString = menuEntry.aString9585
        val colorName = menuEntry.coloredName
        return if (aString != null && !aString.isEmpty())
            if (colorName != null && !colorName.isEmpty())
                menuEntry.action + LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + colorName + LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + aString
            else
                menuEntry.action + LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + aString
        else
            if (colorName != null && !colorName.isEmpty())
                menuEntry.action + LocalizedText.DELIMITER.translate(client.CURRENT_LANGUAGE) + colorName
            else
                menuEntry.action
    }

    @JvmStatic
    fun getQuestsFrom(menuEntry: MiniMenuEntry): IntArray? {
        var quests: IntArray? = null
        when {
            Mouse.isClicktypeObj(menuEntry.clickType) -> {
                quests = TypeLists.OBJ!!.list((menuEntry.entityUid).toInt()).quests
            }
            menuEntry.itemId != -1 -> {
                quests = TypeLists.OBJ!!.list(menuEntry.itemId).quests
            }
            Mouse.isClicktypeNpc(menuEntry.clickType) -> {
                val objectNode = client.NPC_MAP.get((menuEntry.entityUid).toLong())
                if (objectNode != null) {
                    val npcEntity = objectNode.value as NpcEntity
                    var npcType: NPCType? = npcEntity.definitions
                    if (npcType?.alternativeIds != null) {
                        npcType = npcType?.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
                    }
                    if (npcType != null) {
                        quests = npcType.quests
                    }
                }
            }
            Mouse.isClicktypeLoc(menuEntry.clickType) -> {
                val mapRegion = Resource.mapRegion
                if (mapRegion != null) {
                    val locType = mapRegion.getLoctypeList().list((menuEntry.entityUid shr 32 and 0x7fffffffL).toInt())
                    if (locType.alternativeIds != null) {
                        val multiLoc = locType.getMultiLoc(VarProviders.PLAYER_VAR_PROVIDER)
                        if (multiLoc != null) {
                            quests = multiLoc.quests
                        }
                    } else if (locType != null) {
                        quests = locType.quests
                    }
                }
            }
        }
        return quests
    }

    @JvmField
    var isOpen = false

    @JvmStatic
    fun addEntry(action: String, coloredName: String?, cursor: Int, clickType: Int, id: Int, long_5: Long, i_7: Int, i_8: Int, bool9: Boolean, bool10: Boolean, bitpacked: Long, bool13: Boolean) {
        if (!isOpen && entryCount < 520) {
            val resolvedCursor = if (cursor != -1) cursor else client.defaultMenuCursor
            val menuEntry = MiniMenuEntry(action ?: "", coloredName ?: "", resolvedCursor, clickType, id, long_5, i_7, i_8, bool9, bool10, bitpacked, bool13)
            addEntry(menuEntry)
        }
    }

    @JvmStatic
    fun method2095(entry: MiniMenuEntry) {
        if (!isOpen) {
            entry.unlink()
            --entryCount
            if (!entry.aBool9586) {
                val long_2 = entry.bitpacked
                var class282Sub50Sub154: ColoredMiniMenuEntry? = aClass465172.get(long_2) as? ColoredMiniMenuEntry
                while (class282Sub50Sub154 != null && !class282Sub50Sub154.tagStart.equals(entry.coloredName)) {
                    class282Sub50Sub154 = aClass465172.nextInBucket() as? ColoredMiniMenuEntry
                }
                if (class282Sub50Sub154 != null && class282Sub50Sub154.removeEntry(entry)) {
                    method503(class282Sub50Sub154)
                }
            } else {
                var class282Sub50Sub155: ColoredMiniMenuEntry? = aClass477182.getNext() as? ColoredMiniMenuEntry
                while (class282Sub50Sub155 != null) {
                    if (class282Sub50Sub155.tagStart.equals(entry.coloredName)) {
                        var bool3 = false
                        var class282Sub50Sub76: MiniMenuEntry? = class282Sub50Sub155.nodeQueue.getNext() as? MiniMenuEntry
                        while (class282Sub50Sub76 != null) {
                            if (class282Sub50Sub76 == entry) {
                                if (class282Sub50Sub155.removeEntry(entry)) {
                                    method503(class282Sub50Sub155)
                                }
                                bool3 = true
                                break
                            }
                            class282Sub50Sub76 = class282Sub50Sub155.nodeQueue.getDirectNext() as? MiniMenuEntry
                        }
                        if (bool3) {
                            break
                        }
                    }
                    class282Sub50Sub155 = aClass477182.getDirectNext() as? ColoredMiniMenuEntry
                }
            }
        }
    }

    @JvmField
    val aClass482171 = NodeCollection()

    @JvmStatic
    fun method5334(clicktypeBase: Int, clicktypeSub: Int): Boolean {
        return clicktypeSub >= 1000 && clicktypeBase < 1000 || (clicktypeSub < 1000 && clicktypeBase < 1000 && (method13788(clicktypeBase) || !method13788(clicktypeSub)) || clicktypeSub >= 1000)
    }

    @JvmStatic
    fun method13788(clickType: Int): Boolean {
        return clickType == 59 || clickType == 2 || clickType == 8 || clickType == 17 || clickType == 15 || clickType == 16 || clickType == 58
    }

    @JvmStatic
    fun reset() {
        var class282Sub50Sub151: ColoredMiniMenuEntry? = aClass477182.getNext() as? ColoredMiniMenuEntry
        while (class282Sub50Sub151 != null) {
            if (class282Sub50Sub151.queueSize > 1) {
                class282Sub50Sub151.queueSize = 0
                aClass229164.put(class282Sub50Sub151, ((class282Sub50Sub151.nodeQueue.current.nextCache as MiniMenuEntry).bitpacked))
                class282Sub50Sub151.nodeQueue.reset()
            }
            class282Sub50Sub151 = aClass477182.getDirectNext() as? ColoredMiniMenuEntry
        }
        anInt170 = 0
        entryCount = 0
        aClass482171.remove_all()
        aClass465172.clear()
        aClass477182.reset()
        isOpen = false
    }

    @JvmStatic
    fun method15411(toolkit: AbstractToolkit) {
        if (isOpen) {
            method6216(toolkit)
        } else {
            method4800()
        }

    }

    @JvmStatic
    fun hasEntries(): Boolean {
        return entryCount > 0
    }

    @JvmStatic
    fun method503(class282Sub50Sub150: ColoredMiniMenuEntry) {
        var bool2 = false
        class282Sub50Sub150.unlinkSub()
        var class282Sub50Sub153: ColoredMiniMenuEntry? = aClass477182.getNext() as? ColoredMiniMenuEntry
        while (class282Sub50Sub153 != null) {
            if (method5334(class282Sub50Sub150.getClickType(), class282Sub50Sub153.getClickType())) {
                CacheableNode.rebuild(class282Sub50Sub150, class282Sub50Sub153)
                bool2 = true
                break
            }
            class282Sub50Sub153 = aClass477182.getDirectNext() as? ColoredMiniMenuEntry
        }
        if (!bool2) {
            aClass477182.insert(class282Sub50Sub150)
        }
    }

    @JvmStatic
    fun method288(deltaX: Float, deltaY: Float, deltaZ: Float, x: Float, y: Float, z: Float, i_6: Int): Float {
        var f_8 = 0.0F
        val f_9 = x - deltaX
        val f_10 = y - deltaY
        val f_11 = z - deltaZ
        var f_12 = 0.0F
        var f_13 = 0.0F
        var f_14 = 0.0F
        val mapRegion = Resource.mapRegion
        if (mapRegion != null) {
            val scene = mapRegion.getScene()!!
            while (f_8 < 1.1F) {
                val absX = f_8 * f_9 + deltaX
                val absY = f_10 * f_8 + deltaY
                val absZ = deltaZ + f_8 * f_11
                val sceneX = (absX.toInt()) shr 9
                val sceneZ = (absZ.toInt()) shr 9
                if (sceneX > 0 && sceneZ > 0 && sceneX < mapRegion.sceneSizeX && sceneZ < mapRegion.sceneSizeY) {
                    var plane = PlayerUpdate.MY_PLAYER!!.plane.toInt()
                    if (plane < 3 && ((mapRegion.settingsBits.tileFunctionMasks[1][sceneX][sceneZ].toInt() and 0x2) != 0)) {
                        plane++
                    }
                    val height = scene.surfaceHeightmap!![plane]!!.getAverageHeight(absX.toInt(), absZ.toInt())
                    if (height < absY) {
                        if (i_6 >= 2) {
                            return f_8 - 0.1F + method288(f_12, f_13, f_14, absX, absY, absZ, i_6 - 1) * 0.1F
                        }
                        return f_8
                    }
                }
                f_12 = absX
                f_13 = absY
                f_14 = absZ
                f_8 += 0.1F
            }
        }
        return -1.0f
    }

    @JvmField
    var aCacheableNodeSub7438: MiniMenuEntry? = null

    @JvmField
    var anInt9471 = 0

    @JvmStatic
    fun method4908(toolkit: AbstractToolkit) {
        val fillColor = -10660793
        AbstractToolkit.drawOutlinedFilledOpaqueRectangle(toolkit, StaticMethods.anInt3555, StaticMethods.anInt2300, anInt8975, anInt1104, fillColor)
        minimenuHeader!!.renderPlain(LocalizedText.CHOOSE_OPTION.translate(client.CURRENT_LANGUAGE), StaticMethods.anInt3555 + 3, StaticMethods.anInt2300 + 14, fillColor, -1)
        val mouseX = MouseRecord.recorder!!.getMouseX()
        val mouseY = MouseRecord.recorder!!.getMouseY()
        var i_5: Int
        var class282Sub50Sub76: MiniMenuEntry?
        var i_7: Int
        if (!aBool162) {
            i_5 = 0
            var class282Sub50Sub76Temp: MiniMenuEntry? = aClass482171.getFirst() as? MiniMenuEntry
            while (class282Sub50Sub76Temp != null) {
                class282Sub50Sub76 = class282Sub50Sub76Temp
                i_7 = (entryCount - 1 - i_5) * anInt178 + StaticMethods.anInt2300 + 31
                method12400(mouseX, mouseY, StaticMethods.anInt3555, anInt8975, i_7, class282Sub50Sub76, minimenuHeader!!, FontMetrics.aClass4143438!!, -1, -256)
                ++i_5
                class282Sub50Sub76Temp = aClass482171.next() as? MiniMenuEntry
            }
        } else {
            i_5 = 0
            var class282Sub50Sub158: ColoredMiniMenuEntry? = aClass477182.getNext() as? ColoredMiniMenuEntry
            while (class282Sub50Sub158 != null) {
                i_7 = i_5 * anInt178 + StaticMethods.anInt2300 + 31
                if (class282Sub50Sub158.queueSize == 1) {
                    method12400(mouseX, mouseY, StaticMethods.anInt3555, anInt8975, i_7, (class282Sub50Sub158.nodeQueue.current.next as MiniMenuEntry), minimenuHeader!!, FontMetrics.aClass4143438!!, -1, -256)
                } else {
                    StaticMethods.method1630(mouseX, mouseY, StaticMethods.anInt3555, anInt8975, i_7, class282Sub50Sub158, minimenuHeader!!, FontMetrics.aClass4143438!!, -1, -256)
                }
                ++i_5
                class282Sub50Sub158 = aClass477182.getDirectNext() as? ColoredMiniMenuEntry
            }
            if (aCacheableNodeSub15163 != null) {
                AbstractToolkit.drawOutlinedFilledOpaqueRectangle(toolkit, anInt3996, anInt9471, anInt8518, StationaryWall.anInt10526, fillColor)
                minimenuHeader!!.renderPlain(aCacheableNodeSub15163!!.tagStart, anInt3996 + 3, anInt9471 + 14, fillColor, -1)
                i_5 = 0
                class282Sub50Sub76 = aCacheableNodeSub15163!!.nodeQueue.getNext() as? MiniMenuEntry
                while (class282Sub50Sub76 != null) {
                    i_7 = i_5 * anInt178 + anInt9471 + 31
                    method12400(mouseX, mouseY, anInt3996, anInt8518, i_7, class282Sub50Sub76, minimenuHeader!!, FontMetrics.aClass4143438!!, -1, -256)
                    ++i_5
                    class282Sub50Sub76 = aCacheableNodeSub15163!!.nodeQueue.getDirectNext() as? MiniMenuEntry
                }
            }
        }
    }

    @JvmStatic
    fun close() {
        method6810()
        isOpen = false
        InterfaceComponentRenderer.inboundsComponentToTop(StaticMethods.anInt3555, StaticMethods.anInt2300, anInt8975, anInt1104)
        aCacheableNodeSub79445 = null
        aCacheableNodeSub7168 = null
    }

    @JvmStatic
    fun containsEntryForNpc(uid: Int): Boolean {
        var entry: MiniMenuEntry? = aClass482171.getFirst() as? MiniMenuEntry
        while (entry != null) {
            if (Mouse.isClicktypeNpc(entry.clickType) && uid == entry.entityUid.toInt()) {
                return true
            }
            entry = aClass482171.next() as? MiniMenuEntry
        }
        return false
    }

    @JvmField
    var anInt170 = 0

    @JvmField
    var anInt198 = -1

    @JvmField
    var cancelOption: MiniMenuEntry? = null

    @JvmField
    var aCacheableNodeSub7168: MiniMenuEntry? = null

    @JvmField
    var anInt178 = 16

    @JvmField
    var aBool162 = false

    @JvmField
    var aCacheableNodeSub15163: ColoredMiniMenuEntry? = null

    @JvmField
    var anInt167 = 0

    @JvmField
    val aClass465172 = IterableNodeMap<ColoredMiniMenuEntry>(16)

    @JvmField
    val aClass477182 = Queue<ColoredMiniMenuEntry>()

    @JvmField
    val aClass229164 = LRUCache(30)

    @JvmField
    var anInt180 = -1

    @JvmField
    var anInt181 = -1

    @JvmStatic
    fun getMinimenuCursor(): Int {
        if (client.draggingComponent == null) {
            if (!isOpen) {
                if (TypeLists.INTERACTION_DEFAULTS!!.minimenuCursor) { // own cursor?
                    if (aCacheableNodeSub7168 != null) {
                        return aCacheableNodeSub7168!!.cursor
                    }
                } else if (aCacheableNodeSub79445 != null) { // default cursor?
                    return aCacheableNodeSub79445!!.cursor
                }
            } else {
                val minimenuFont = FontMetrics.getForMinimenu()
                val mouseX = MouseRecord.recorder!!.getMouseX()
                val mouseY = MouseRecord.recorder!!.getMouseY()
                var i_4: Int
                var i_5: Int
                var class282Sub50Sub77: MiniMenuEntry?
                var i_8: Int
                if (!aBool162) {
                    if (mouseX > StaticMethods.anInt3555 && mouseX < StaticMethods.anInt3555 + anInt8975) {
                        i_4 = -1
                        i_5 = 0
                        while (i_5 < entryCount) {
                            if (FontMetrics.useBoldFont) {
                                i_8 = (entryCount - 1 - i_5) * anInt178 + minimenuFont.topPadding + StaticMethods.anInt2300 + 1 + 20
                            } else {
                                i_8 = (entryCount - 1 - i_5) * anInt178 + StaticMethods.anInt2300 + 31
                            }
                            if (mouseY > i_8 - minimenuFont.topPadding - 1 && mouseY < i_8 + minimenuFont.bottomPadding) {
                                i_4 = i_5
                            }
                            i_5++
                        }
                        if (i_4 != -1) {
                            i_5 = 0
                            val nodeiterator6 = NodeIterator(aClass482171)
                            class282Sub50Sub77 = nodeiterator6.nextAndReset() as? MiniMenuEntry
                            while (class282Sub50Sub77 != null) {
                                if (i_5++ == i_4) {
                                    return class282Sub50Sub77.cursor
                                }
                                class282Sub50Sub77 = nodeiterator6.next() as? MiniMenuEntry
                            }
                        }
                    }
                } else {
                    var class4699: QueueIterator
                    if (mouseX > StaticMethods.anInt3555 && mouseX < StaticMethods.anInt3555 + anInt8975) {
                        i_4 = -1
                        i_5 = 0
                        while (i_5 < anInt170) {
                            if (FontMetrics.useBoldFont) {
                                i_8 = i_5 * anInt178 + minimenuFont.topPadding + StaticMethods.anInt2300 + 1 + 20
                            } else {
                                i_8 = i_5 * anInt178 + StaticMethods.anInt2300 + 31
                            }
                            if (mouseY > i_8 - minimenuFont.topPadding - 1 && mouseY < i_8 + minimenuFont.bottomPadding) {
                                i_4 = i_5
                            }
                            i_5++
                        }
                        if (i_4 != -1) {
                            i_5 = 0
                            class4699 = QueueIterator(aClass477182)
                            var class282Sub50Sub1510: ColoredMiniMenuEntry? = class4699.nextAndReset() as? ColoredMiniMenuEntry
                            while (class282Sub50Sub1510 != null) {
                                if (i_5++ == i_4) {
                                    return ((class282Sub50Sub1510.nodeQueue.current.nextCache as MiniMenuEntry).cursor)
                                }
                                class282Sub50Sub1510 = class4699.next() as? ColoredMiniMenuEntry
                            }
                        }
                    } else if (aCacheableNodeSub15163 != null && mouseX > anInt3996 && mouseX < anInt8518 + anInt3996) {
                        i_4 = -1
                        i_5 = 0
                        while (i_5 < aCacheableNodeSub15163!!.queueSize) {
                            if (FontMetrics.useBoldFont) {
                                i_8 = i_5 * anInt178 + minimenuFont.topPadding + anInt9471 + 1 + 20
                            } else {
                                i_8 = i_5 * anInt178 + anInt9471 + 31
                            }
                            if (mouseY > i_8 - minimenuFont.topPadding - 1 && mouseY < i_8 + minimenuFont.bottomPadding) {
                                i_4 = i_5
                            }
                            i_5++
                        }
                        if (i_4 != -1) {
                            i_5 = 0
                            class4699 = QueueIterator(aCacheableNodeSub15163!!.nodeQueue)
                            class282Sub50Sub77 = class4699.nextAndReset() as? MiniMenuEntry
                            while (class282Sub50Sub77 != null) {
                                if (i_5++ == i_4) {
                                    return class282Sub50Sub77.cursor
                                }
                                class282Sub50Sub77 = class4699.next() as? MiniMenuEntry
                            }
                        }
                    }
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun method6810() {
        if (aCacheableNodeSub15163 != null) {
            aCacheableNodeSub15163 = null
            InterfaceComponentRenderer.inboundsComponentToTop(anInt3996, anInt9471, anInt8518, StationaryWall.anInt10526)
        }

    }

    @JvmField
    var anInt3996 = 0

    @JvmStatic
    fun method857(toolkit: AbstractToolkit) {
        val i_2 = StaticMethods.anInt3555
        val i_3 = StaticMethods.anInt2300
        val i_4 = anInt8975
        val i_5 = anInt1104 - 3
        val b_6 = 20
        if (FontMetrics.aFontRenderer2348 == null || StaticMedia.aClass4142200 == null) {
            FontMetrics.aFontRenderer2348 = StaticMethods.initializedFontcombos!!.list(client.FONT_PROVIDER, StaticMethods.anInt2982, true, true) as? AbstractFontRenderer
            StaticMedia.aClass4142200 = StaticMethods.initializedFontcombos!!.list(client.FONT_PROVIDER, StaticMethods.anInt2982)
            if (FontMetrics.aFontRenderer2348 != null && StaticMedia.aClass4142200 != null) {
                close()
                var i_15 = i_4 / 2 + i_2
                if (i_15 + i_4 > GameShell.screenCanvasWidth) {
                    i_15 = GameShell.screenCanvasWidth - i_4
                }
                if (i_15 < 0) {
                    i_15 = 0
                }
                StaticMethods.method4653(i_15, StaticMethods.anInt2300)
                return
            }
        }
        val fontrenderer7: AbstractFontRenderer?
        if (FontMetrics.aFontRenderer2348 != null && StaticMedia.aClass4142200 != null) {
            fontrenderer7 = FontMetrics.aFontRenderer2348
        } else {
            fontrenderer7 = minimenuHeader
        }
        val fontmetrics8 = FontMetrics.getForMinimenu()
        NativeSprite.method12680(toolkit, StaticMethods.anInt3555, StaticMethods.anInt2300, anInt8975, anInt1104, b_6, fontrenderer7!!, fontmetrics8!!, LocalizedText.CHOOSE_OPTION.translate(client.CURRENT_LANGUAGE)!!)
        var alpha = 255 - StaticMethods.anInt2262 - NativeSprite.anInt156
        if (alpha < 0) {
            alpha = 0
        }
        val i_10 = MouseRecord.recorder!!.getMouseX()
        val i_11 = MouseRecord.recorder!!.getMouseY()
        var i_12: Int
        var class282Sub50Sub713: MiniMenuEntry?
        var i_14: Int
        var class282Sub50Sub1516: ColoredMiniMenuEntry?
        if (!aBool162) {
            i_12 = 0
            var class282Sub50Sub713Temp: MiniMenuEntry? = aClass482171.getFirst() as? MiniMenuEntry
            while (class282Sub50Sub713Temp != null) {
                class282Sub50Sub713 = class282Sub50Sub713Temp
                i_14 = i_3 + b_6 + (entryCount - 1 - i_12) * anInt178 + fontmetrics8.topPadding + 1
                if (i_10 > StaticMethods.anInt3555 && i_10 < StaticMethods.anInt3555 + anInt8975 && i_11 > i_14 - fontmetrics8.topPadding - 1 && i_11 < i_14 + fontmetrics8.bottomPadding && class282Sub50Sub713.aBool9583) {
                    toolkit.drawFilledRectangle(StaticMethods.anInt3555, i_14 - fontmetrics8.topPadding, anInt8975, anInt178, alpha shl 24 or anInt188, 1)
                }
                ++i_12
                class282Sub50Sub713Temp = aClass482171.next() as? MiniMenuEntry
            }
        } else {
            i_12 = 0
            var class282Sub50Sub1516Temp: ColoredMiniMenuEntry? = aClass477182.getNext() as? ColoredMiniMenuEntry
            while (class282Sub50Sub1516Temp != null) {
                class282Sub50Sub1516 = class282Sub50Sub1516Temp
                i_14 = i_3 + b_6 + i_12 * anInt178 + fontmetrics8.topPadding + 1
                if (i_10 > StaticMethods.anInt3555 && i_10 < anInt8975 + StaticMethods.anInt3555 && i_11 > i_14 - fontmetrics8.topPadding - 1 && i_11 < i_14 + fontmetrics8.bottomPadding && (class282Sub50Sub1516.queueSize > 1 || ((class282Sub50Sub1516.nodeQueue.current.nextCache as MiniMenuEntry).aBool9583))) {
                    toolkit.drawFilledRectangle(StaticMethods.anInt3555, i_14 - fontmetrics8.topPadding, anInt8975, anInt178, alpha shl 24 or anInt188, 1)
                }
                ++i_12
                class282Sub50Sub1516Temp = aClass477182.getDirectNext() as? ColoredMiniMenuEntry
            }
            if (aCacheableNodeSub15163 != null) {
                NativeSprite.method12680(toolkit, anInt3996, anInt9471, anInt8518, StationaryWall.anInt10526, b_6, fontrenderer7, fontmetrics8, aCacheableNodeSub15163!!.tagStart)
                i_12 = 0
                class282Sub50Sub713 = aCacheableNodeSub15163!!.nodeQueue.getNext() as? MiniMenuEntry
                while (class282Sub50Sub713 != null) {
                    i_14 = b_6 + i_12 * anInt178 + fontmetrics8.topPadding + anInt9471 + 1
                    if (i_10 > anInt3996 && i_10 < anInt8518 + anInt3996 && i_11 > i_14 - fontmetrics8.topPadding - 1 && i_11 < i_14 + fontmetrics8.bottomPadding && class282Sub50Sub713.aBool9583) {
                        toolkit.drawFilledRectangle(anInt3996, i_14 - fontmetrics8.topPadding, anInt8518, anInt178, alpha shl 24 or anInt188, 1)
                    }
                    ++i_12
                    class282Sub50Sub713 = aCacheableNodeSub15163!!.nodeQueue.getDirectNext() as? MiniMenuEntry
                }
                StaticMethods.method8209(toolkit, anInt3996, anInt9471, anInt8518, StationaryWall.anInt10526, b_6)
            }
        }
        StaticMethods.method8209(toolkit, StaticMethods.anInt3555, StaticMethods.anInt2300, anInt8975, anInt1104, b_6)
        if (!aBool162) {
            i_12 = 0
            var class282Sub50Sub713Temp2: MiniMenuEntry? = aClass482171.getFirst() as? MiniMenuEntry
            while (class282Sub50Sub713Temp2 != null) {
                class282Sub50Sub713 = class282Sub50Sub713Temp2
                i_14 = i_3 + b_6 + (entryCount - 1 - i_12) * anInt178 + fontmetrics8.topPadding + 1
                method12400(i_10, i_11, i_2, i_4, i_14, class282Sub50Sub713, fontrenderer7, fontmetrics8, anInt4722 or -0xffffff.inv(), StaticMethods.anInt1005 or -0xffffff.inv())
                ++i_12
                class282Sub50Sub713Temp2 = aClass482171.next() as? MiniMenuEntry
            }
        } else {
            i_12 = 0
            var class282Sub50Sub1516Temp2: ColoredMiniMenuEntry? = aClass477182.getNext() as? ColoredMiniMenuEntry
            while (class282Sub50Sub1516Temp2 != null) {
                class282Sub50Sub1516 = class282Sub50Sub1516Temp2
                i_14 = b_6 + i_12 * anInt178 + fontmetrics8.topPadding + StaticMethods.anInt2300 + 1
                if (class282Sub50Sub1516.queueSize == 1) {
                    method12400(i_10, i_11, StaticMethods.anInt3555, anInt8975, i_14, (class282Sub50Sub1516.nodeQueue.current.next as MiniMenuEntry), fontrenderer7, fontmetrics8, anInt4722 or -0xffffff.inv(), StaticMethods.anInt1005 or -0xffffff.inv())
                } else {
                    StaticMethods.method1630(i_10, i_11, StaticMethods.anInt3555, anInt8975, i_14, class282Sub50Sub1516, fontrenderer7, fontmetrics8, anInt4722 or -0xffffff.inv(), StaticMethods.anInt1005 or -0xffffff.inv())
                }
                ++i_12
                class282Sub50Sub1516Temp2 = aClass477182.getDirectNext() as? ColoredMiniMenuEntry
            }
            if (aCacheableNodeSub15163 != null) {
                i_12 = 0
                class282Sub50Sub713 = aCacheableNodeSub15163!!.nodeQueue.getNext() as? MiniMenuEntry
                while (class282Sub50Sub713 != null) {
                    i_14 = b_6 + i_12 * anInt178 + fontmetrics8.topPadding + anInt9471 + 1
                    method12400(i_10, i_11, anInt3996, anInt8518, i_14, class282Sub50Sub713, fontrenderer7, fontmetrics8, anInt4722 or -0xffffff.inv(), StaticMethods.anInt1005 or -0xffffff.inv())
                    ++i_12
                    class282Sub50Sub713 = aCacheableNodeSub15163!!.nodeQueue.getDirectNext() as? MiniMenuEntry
                }
            }
        }
    }

    @JvmStatic
    fun method6216(toolkit: AbstractToolkit) {
        if (!FontMetrics.useBoldFont) {
            method4908(toolkit)
        } else {
            method857(toolkit)
        }
    }

    @JvmField
    var aCacheableNodeSub79445: MiniMenuEntry? = null

    @JvmField
    var anInt8975 = 0

    @JvmField
    var anInt1104 = 0

    @JvmField
    var useOnCursor = 0

    @JvmField
    var minimenuHeader: AbstractFontRenderer? = null

    @JvmField
    var anInt188 = 0

    @JvmField
    var USE_INTERFACE_ID = 0

    @JvmField
    val aClass482174 = NodeCollection()

    @JvmField
    val aClass482175 = NodeCollection()

    @JvmStatic
    fun method6451(i_0: Int, i_1: Int) {
        anInt180 = i_0
        anInt181 = i_1
    }

    @JvmStatic
    fun addCancelOption() {
        cancelOption = MiniMenuEntry(LocalizedText.CANCEL.translate(client.CURRENT_LANGUAGE)!!, "", client.defaultMenuCursor, 1006, -1, 0L, 0, 0, true, false, 0L, true)
    }

    @JvmField
    var anInt4722 = 0

    @JvmStatic
    fun method12400(i_0: Int, i_1: Int, i_2: Int, i_4: Int, i_6: Int, entry: MiniMenuEntry, fontRenderer: AbstractFontRenderer?, fontmetrics9: FontMetrics, i_10: Int, i_11: Int) {
        var i_101 = i_10
        if (i_0 > i_2 && i_0 < i_2 + i_4 && i_1 > i_6 - fontmetrics9.topPadding - 1 && i_1 < fontmetrics9.bottomPadding + i_6 && entry.aBool9583) {
            i_101 = i_11
        }
        val questIds = getQuestsFrom(entry)
        var text = getTextFrom(entry)
        if (questIds != null) {
            text += ChatBox.addQuestChatIcons(questIds)
        }
        @Suppress("UNCHECKED_CAST")
        fontRenderer?.method386(text, i_2 + 3, i_6, i_101, client.RANDOMIZER, client.randomSeed, StaticMedia.chatIcons as Array<com.jagex.game.runetek5.graphics.sprite.NativeSprite>?, StaticMedia.chatScaleHeights)
        if (entry.aBool9577) {
            StaticMedia.otherLevelSprite!!.draw(5 + i_2 + fontmetrics9.getWidthNoSprites(text), i_6 - fontmetrics9.topPadding)
        }
    }

    @JvmField
    var anInt8518 = 0
}
