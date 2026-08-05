@file:Suppress("UNCHECKED_CAST", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "PLATFORM_CLASS_MAPPED_TO_KOTLIN", "VARIABLE_WITH_REDUNDANT_INITIALIZER", "SENSELESS_COMPARISON")
package com.jagex.game.runetek5.client.menu

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.NodeIterator
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.cache.node.lru.CacheableNode
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.Client
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
import com.jagex.game.runetek5.graphics.sprite.Sprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.vardomain.VarProviders

object MiniMenu {

    @JvmField
    var entryCount = 0

    @JvmField
    var topEntry: MiniMenuEntry? = null

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
            entryList.addFirst(entry)
            entryCount++
            var coloredEntry: ColoredMiniMenuEntry?
            if (!entry.isUncategorized && entry.coloredName != null && !entry.coloredName.isEmpty()) {
                val hash = entry.bitpacked
                coloredEntry = coloredEntryMap.get(hash) as? ColoredMiniMenuEntry
                while (coloredEntry != null && !coloredEntry.tagStart.equals(entry.coloredName)) {
                    coloredEntry = coloredEntryMap.nextInBucket() as? ColoredMiniMenuEntry
                }
                if (coloredEntry == null) {
                    coloredEntry = coloredEntryCache.get(hash) as? ColoredMiniMenuEntry
                    if (coloredEntry != null && !coloredEntry.tagStart.equals(entry.coloredName)) {
                        coloredEntry = null
                    }
                    if (coloredEntry == null) {
                        coloredEntry = ColoredMiniMenuEntry(entry.coloredName)
                    }
                    coloredEntryMap.put(coloredEntry, hash)
                    ++coloredEntryCount
                }
            } else {
                coloredEntry = ColoredMiniMenuEntry(entry.coloredName)
                ++coloredEntryCount
            }
            if (coloredEntry.addEntry(entry)) {
                insertSortedEntry(coloredEntry)
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
        var width = fontmetrics.getTextWidth(text, StaticMedia.chatIcons as Array<Sprite>?)
        if (menuEntry.showOtherLevelIcon) {
            width += (StaticMedia.otherLevelSprite?.getWidth() ?: 0) + 4
        }
        return width
    }

    @JvmStatic
    fun getActionText(entry: MiniMenuEntry?): String {
        return if (!isOpen && entry != null) entry.action else ""
    }

    @JvmStatic
    fun openSubmenu(coloredEntry: ColoredMiniMenuEntry, screenY: Int) {
        if (isOpen) {
            val fontMetrics = FontMetrics.getForMinimenu()
            var maxEntryWidth = 0

            var entryWidth: Int
            var entry: MiniMenuEntry? = coloredEntry.nodeQueue.getNext() as? MiniMenuEntry
            while (entry != null) {
                entryWidth = getWidthForEntryWithQuests(entry, fontMetrics)
                if (entryWidth > maxEntryWidth) {
                    maxEntryWidth = entryWidth
                }
                entry = coloredEntry.nodeQueue.getDirectNext() as? MiniMenuEntry
            }

            maxEntryWidth += 8
            val submenuHeight = coloredEntry.queueSize * entryRowHeight + 21
            StationaryWall.menuHeight = (if (FontMetrics.useBoldFont) 26 else 22) + entryRowHeight * coloredEntry.queueSize
            entryWidth = StaticMethods.menuOpenX + menuWidth
            if (maxEntryWidth + entryWidth > GameShell.screenCanvasWidth) {
                entryWidth = StaticMethods.menuOpenX - maxEntryWidth
            }

            if (entryWidth < 0) {
                entryWidth = 0
            }

            val headerHeight = if (FontMetrics.useBoldFont) fontMetrics.topPadding + 1 + 20 else 31
            var submenuStartY = screenY - headerHeight + fontMetrics.topPadding + 1
            if (submenuHeight + submenuStartY > GameShell.screenCanvasHeight * -969250379) {
                submenuStartY = GameShell.screenCanvasHeight * -969250379 - submenuHeight
            }

            if (submenuStartY < 0) {
                submenuStartY = 0
            }

            submenuX = entryWidth
            submenuY = submenuStartY
            submenuWidth = maxEntryWidth
            activeSubmenu = coloredEntry
        }

    }

    @JvmStatic
    fun renderTooltipText() {
        if ((entryCount >= 2 || Client.interfaceSelectionActive) && Client.draggingComponent == null) {
            var menuText: String
            if (Client.interfaceSelectionActive && entryCount < 2) {
                menuText = Client.useOnType + LocalizedText.DELIMITER.translate(Client.CURRENT_LANGUAGE!!) + Client.useOnTarget + " " + "->"
            } else if (KeyDownCommand.minimenuKeyDown() && entryCount > 2) {
                menuText = getTextFrom(activeEntry!!)
            } else {
                val menuEntry = activeEntry
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
                menuText = menuText + TextUtils.setTextColor(16777215) + " / " + (entryCount - 2) + LocalizedText.MORE_OPTIONS.translate(Client.CURRENT_LANGUAGE!!)
            }
            val comp = InterfaceComponentRenderer.chatDisplayComponent
            if (comp != null) {
                var fontRenderer = comp.getFontRenderer(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
                if (fontRenderer == null) {
                    fontRenderer = minimenuHeader
                }
                @Suppress("UNCHECKED_CAST")
                fontRenderer!!.renderTextAntiMacro(menuText, InterfaceComponentRenderer.chatDisplayX, InterfaceComponentRenderer.chatDisplayY, comp.width, comp.height, comp.color, comp.spriteShadow, comp.textHorizontalAli, comp.textVerticalAli, Client.RANDOMIZER, Client.randomSeed, Client.ANTI_MACRO_TEXT_BOUNDS, StaticMedia.chatIcons as Array<NativeSprite>?, StaticMedia.chatScaleHeights)
                InterfaceComponentRenderer.inboundsComponentToTop(Client.ANTI_MACRO_TEXT_BOUNDS[0], Client.ANTI_MACRO_TEXT_BOUNDS[1], Client.ANTI_MACRO_TEXT_BOUNDS[2], Client.ANTI_MACRO_TEXT_BOUNDS[3])
            }
        }
    }

    @JvmStatic
    fun getColoredNameText(entry: MiniMenuEntry?): String {
        if (!isOpen && entry != null) {
            val targetName = entry.targetName
            val colorName = entry.coloredName
            return if ((colorName == null || colorName.isEmpty()) && targetName != null && !targetName.isEmpty()) targetName else (colorName ?: "")
        }
        return ""
    }

    @JvmStatic
    fun getSecondaryEntry(): MiniMenuEntry? {
        return secondaryEntry
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
                StaticMethods.isPlayerOption(clickType) -> 7
                clickType == 16 -> 8
                else -> 5
            }
        }
    }

    @JvmStatic
    fun resetEntries() {
        var coloredEntry: ColoredMiniMenuEntry? = sortedColoredEntries.getNext() as? ColoredMiniMenuEntry
        while (coloredEntry != null) {
            if (coloredEntry.queueSize > 1) {
                coloredEntry.queueSize = 0
                coloredEntryCache.put(coloredEntry, ((coloredEntry.nodeQueue.current.nextCache as MiniMenuEntry).bitpacked))
                coloredEntry.nodeQueue.reset()
            }
            coloredEntry = sortedColoredEntries.getDirectNext() as? ColoredMiniMenuEntry
        }

        coloredEntryCount = 0
        entryCount = 0
        entryList.remove_all()
        coloredEntryMap.clear()
        sortedColoredEntries.reset()
        addEntry(cancelOption)
    }

    @JvmStatic
    fun removeMatchingEntries() {
        var menuEntry: MiniMenuEntry? = entryList.getFirst() as? MiniMenuEntry
        while (menuEntry != null) {
            if (Mouse.method539(menuEntry.clickType)) {
                removeEntry(menuEntry)
            }
            menuEntry = entryList.next() as? MiniMenuEntry
        }
    }

    @JvmStatic
    fun isTopEntryInterfaceAction(): Boolean {
        return if (topEntry == null) {
            false
        } else {
            if (topEntry!!.clickType >= 2000) {
                topEntry!!.clickType -= 2000
            }
            topEntry!!.clickType == 1007
        }
    }

    @JvmStatic
    fun getPrefixTextWidth(coloredChatText: ColoredMiniMenuEntry, fontMetrics: FontMetrics): Int {
        val formattedText = formatTag(coloredChatText)
        @Suppress("UNCHECKED_CAST")
        return fontMetrics.getTextWidth(formattedText, StaticMedia.chatIcons as Array<Sprite>?)
    }

    @JvmStatic
    fun formatTag(coloredChatText: ColoredMiniMenuEntry): String {
        return coloredChatText.tagStart + TextUtils.setTextColor(16777215) + " >"
    }

    @JvmStatic
    fun getTextFrom(menuEntry: MiniMenuEntry): String {
        val targetName = menuEntry.targetName
        val colorName = menuEntry.coloredName
        return if (targetName != null && !targetName.isEmpty())
            if (colorName != null && !colorName.isEmpty())
                menuEntry.action + LocalizedText.DELIMITER.translate(Client.CURRENT_LANGUAGE!!) + colorName + LocalizedText.DELIMITER.translate(Client.CURRENT_LANGUAGE!!) + targetName
            else
                menuEntry.action + LocalizedText.DELIMITER.translate(Client.CURRENT_LANGUAGE!!) + targetName
        else
            if (colorName != null && !colorName.isEmpty())
                menuEntry.action + LocalizedText.DELIMITER.translate(Client.CURRENT_LANGUAGE!!) + colorName
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
                val objectNode = Client.NPC_MAP.get((menuEntry.entityUid).toLong())
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
    fun addEntry(action: String, coloredName: String?, cursor: Int, clickType: Int, id: Int, entityUid: Long, param0: Int, param1: Int, isSelectable: Boolean, showOtherLevelIcon: Boolean, bitpacked: Long, isUncategorized: Boolean) {
        if (!isOpen && entryCount < 520) {
            val resolvedCursor = if (cursor != -1) cursor else Client.defaultMenuCursor
            val menuEntry = MiniMenuEntry(action ?: "", coloredName ?: "", resolvedCursor, clickType, id, entityUid, param0, param1, isSelectable, showOtherLevelIcon, bitpacked, isUncategorized)
            addEntry(menuEntry)
        }
    }

    @JvmStatic
    fun removeEntry(entry: MiniMenuEntry) {
        if (!isOpen) {
            entry.unlink()
            --entryCount
            if (!entry.isUncategorized) {
                val bitpacked = entry.bitpacked
                var coloredEntry: ColoredMiniMenuEntry? = coloredEntryMap.get(bitpacked) as? ColoredMiniMenuEntry
                while (coloredEntry != null && !coloredEntry.tagStart.equals(entry.coloredName)) {
                    coloredEntry = coloredEntryMap.nextInBucket() as? ColoredMiniMenuEntry
                }
                if (coloredEntry != null && coloredEntry.removeEntry(entry)) {
                    insertSortedEntry(coloredEntry)
                }
            } else {
                var sortedEntry: ColoredMiniMenuEntry? = sortedColoredEntries.getNext() as? ColoredMiniMenuEntry
                while (sortedEntry != null) {
                    if (sortedEntry.tagStart.equals(entry.coloredName)) {
                        var found = false
                        var currentEntry: MiniMenuEntry? = sortedEntry.nodeQueue.getNext() as? MiniMenuEntry
                        while (currentEntry != null) {
                            if (currentEntry == entry) {
                                if (sortedEntry.removeEntry(entry)) {
                                    insertSortedEntry(sortedEntry)
                                }
                                found = true
                                break
                            }
                            currentEntry = sortedEntry.nodeQueue.getDirectNext() as? MiniMenuEntry
                        }
                        if (found) {
                            break
                        }
                    }
                    sortedEntry = sortedColoredEntries.getDirectNext() as? ColoredMiniMenuEntry
                }
            }
        }
    }

    @JvmField
    val entryList = NodeCollection()

    @JvmStatic
    fun hasHigherPriority(clicktypeBase: Int, clicktypeSub: Int): Boolean {
        return clicktypeSub >= 1000 && clicktypeBase < 1000 || (clicktypeSub < 1000 && clicktypeBase < 1000 && (isInterfaceClickType(clicktypeBase) || !isInterfaceClickType(clicktypeSub)) || clicktypeSub >= 1000)
    }

    @JvmStatic
    fun isInterfaceClickType(clickType: Int): Boolean {
        return clickType == 59 || clickType == 2 || clickType == 8 || clickType == 17 || clickType == 15 || clickType == 16 || clickType == 58
    }

    @JvmStatic
    fun reset() {
        var coloredEntry: ColoredMiniMenuEntry? = sortedColoredEntries.getNext() as? ColoredMiniMenuEntry
        while (coloredEntry != null) {
            if (coloredEntry.queueSize > 1) {
                coloredEntry.queueSize = 0
                coloredEntryCache.put(coloredEntry, ((coloredEntry.nodeQueue.current.nextCache as MiniMenuEntry).bitpacked))
                coloredEntry.nodeQueue.reset()
            }
            coloredEntry = sortedColoredEntries.getDirectNext() as? ColoredMiniMenuEntry
        }
        coloredEntryCount = 0
        entryCount = 0
        entryList.remove_all()
        coloredEntryMap.clear()
        sortedColoredEntries.reset()
        isOpen = false
    }

    @JvmStatic
    fun render(toolkit: AbstractToolkit) {
        if (isOpen) {
            renderOpenMenu(toolkit)
        } else {
            renderTooltipText()
        }

    }

    @JvmStatic
    fun hasEntries(): Boolean {
        return entryCount > 0
    }

    @JvmStatic
    fun insertSortedEntry(coloredEntry: ColoredMiniMenuEntry) {
        var inserted = false
        coloredEntry.unlinkSub()
        var existingEntry: ColoredMiniMenuEntry? = sortedColoredEntries.getNext() as? ColoredMiniMenuEntry
        while (existingEntry != null) {
            if (hasHigherPriority(coloredEntry.getClickType(), existingEntry.getClickType())) {
                CacheableNode.rebuild(coloredEntry, existingEntry)
                inserted = true
                break
            }
            existingEntry = sortedColoredEntries.getDirectNext() as? ColoredMiniMenuEntry
        }
        if (!inserted) {
            sortedColoredEntries.insert(coloredEntry)
        }
    }

    @JvmStatic
    fun raycastTerrainIntersection(deltaX: Float, deltaY: Float, deltaZ: Float, x: Float, y: Float, z: Float, recursionDepth: Int): Float {
        var interpolation = 0.0F
        val directionX = x - deltaX
        val directionY = y - deltaY
        val directionZ = z - deltaZ
        var prevAbsX = 0.0F
        var prevAbsY = 0.0F
        var prevAbsZ = 0.0F
        val mapRegion = Resource.mapRegion
        if (mapRegion != null) {
            val scene = mapRegion.getScene()!!
            while (interpolation < 1.1F) {
                val absX = interpolation * directionX + deltaX
                val absY = directionY * interpolation + deltaY
                val absZ = deltaZ + interpolation * directionZ
                val sceneX = (absX.toInt()) shr 9
                val sceneZ = (absZ.toInt()) shr 9
                if (sceneX > 0 && sceneZ > 0 && sceneX < mapRegion.sceneSizeX && sceneZ < mapRegion.sceneSizeY) {
                    var plane = PlayerUpdate.MY_PLAYER!!.plane.toInt()
                    if (plane < 3 && ((mapRegion.settingsBits.tileFunctionMasks[1][sceneX][sceneZ].toInt() and 0x2) != 0)) {
                        plane++
                    }
                    val height = scene.surfaceHeightmap!![plane]!!.getAverageHeight(absX.toInt(), absZ.toInt())
                    if (height < absY) {
                        if (recursionDepth >= 2) {
                            return interpolation - 0.1F + raycastTerrainIntersection(prevAbsX, prevAbsY, prevAbsZ, absX, absY, absZ, recursionDepth - 1) * 0.1F
                        }
                        return interpolation
                    }
                }
                prevAbsX = absX
                prevAbsY = absY
                prevAbsZ = absZ
                interpolation += 0.1F
            }
        }
        return -1.0f
    }

    @JvmField
    var deferredEntry: MiniMenuEntry? = null

    @JvmField
    var submenuY = 0

    @JvmStatic
    fun renderOpenMenuSimple(toolkit: AbstractToolkit) {
        val fillColor = -10660793
        AbstractToolkit.drawOutlinedFilledOpaqueRectangle(toolkit, StaticMethods.menuOpenX, StaticMethods.menuOpenY, menuWidth, menuHeight, fillColor)
        minimenuHeader!!.renderPlain(LocalizedText.CHOOSE_OPTION.translate(Client.CURRENT_LANGUAGE!!), StaticMethods.menuOpenX + 3, StaticMethods.menuOpenY + 14, fillColor, -1)
        val mouseX = MouseRecord.recorder!!.getMouseX()
        val mouseY = MouseRecord.recorder!!.getMouseY()
        var entryIndex: Int
        var entry: MiniMenuEntry?
        var entryY: Int
        if (!isCollapsed) {
            entryIndex = 0
            var currentEntry: MiniMenuEntry? = entryList.getFirst() as? MiniMenuEntry
            while (currentEntry != null) {
                entry = currentEntry
                entryY = (entryCount - 1 - entryIndex) * entryRowHeight + StaticMethods.menuOpenY + 31
                drawMenuEntry(mouseX, mouseY, StaticMethods.menuOpenX, menuWidth, entryY, entry, minimenuHeader!!, FontMetrics.defaultFontMetrics!!, -1, -256)
                ++entryIndex
                currentEntry = entryList.next() as? MiniMenuEntry
            }
        } else {
            entryIndex = 0
            var coloredEntry: ColoredMiniMenuEntry? = sortedColoredEntries.getNext() as? ColoredMiniMenuEntry
            while (coloredEntry != null) {
                entryY = entryIndex * entryRowHeight + StaticMethods.menuOpenY + 31
                if (coloredEntry.queueSize == 1) {
                    drawMenuEntry(mouseX, mouseY, StaticMethods.menuOpenX, menuWidth, entryY, (coloredEntry.nodeQueue.current.nextCache as MiniMenuEntry), minimenuHeader!!, FontMetrics.defaultFontMetrics!!, -1, -256)
                } else {
                    StaticMethods.drawColoredMenuEntry(mouseX, mouseY, StaticMethods.menuOpenX, menuWidth, entryY, coloredEntry, minimenuHeader!!, FontMetrics.defaultFontMetrics!!, -1, -256)
                }
                ++entryIndex
                coloredEntry = sortedColoredEntries.getDirectNext() as? ColoredMiniMenuEntry
            }
            if (activeSubmenu != null) {
                AbstractToolkit.drawOutlinedFilledOpaqueRectangle(toolkit, submenuX, submenuY, submenuWidth, StationaryWall.menuHeight, fillColor)
                minimenuHeader!!.renderPlain(activeSubmenu!!.tagStart, submenuX + 3, submenuY + 14, fillColor, -1)
                entryIndex = 0
                entry = activeSubmenu!!.nodeQueue.getNext() as? MiniMenuEntry
                while (entry != null) {
                    entryY = entryIndex * entryRowHeight + submenuY + 31
                    drawMenuEntry(mouseX, mouseY, submenuX, submenuWidth, entryY, entry, minimenuHeader!!, FontMetrics.defaultFontMetrics!!, -1, -256)
                    ++entryIndex
                    entry = activeSubmenu!!.nodeQueue.getDirectNext() as? MiniMenuEntry
                }
            }
        }
    }

    @JvmStatic
    fun close() {
        closeSubmenu()
        isOpen = false
        InterfaceComponentRenderer.inboundsComponentToTop(StaticMethods.menuOpenX, StaticMethods.menuOpenY, menuWidth, menuHeight)
        activeEntry = null
        secondaryEntry = null
    }

    @JvmStatic
    fun containsEntryForNpc(uid: Int): Boolean {
        var entry: MiniMenuEntry? = entryList.getFirst() as? MiniMenuEntry
        while (entry != null) {
            if (Mouse.isClicktypeNpc(entry.clickType) && uid == entry.entityUid.toInt()) {
                return true
            }
            entry = entryList.next() as? MiniMenuEntry
        }
        return false
    }

    @JvmField
    var coloredEntryCount = 0

    @JvmField
    var maxVisibleEntries = -1

    @JvmField
    var cancelOption: MiniMenuEntry? = null

    @JvmField
    var secondaryEntry: MiniMenuEntry? = null

    @JvmField
    var entryRowHeight = 16

    @JvmField
    var isCollapsed = false

    @JvmField
    var activeSubmenu: ColoredMiniMenuEntry? = null

    @JvmField
    var deferredClickType = 0

    @JvmField
    val coloredEntryMap = IterableNodeMap<ColoredMiniMenuEntry>(16)

    @JvmField
    val sortedColoredEntries = Queue<ColoredMiniMenuEntry>()

    @JvmField
    val coloredEntryCache = LRUCache(30)

    @JvmField
    var viewportX = -1

    @JvmField
    var viewportY = -1

    @JvmStatic
    fun getMinimenuCursor(): Int {
        if (Client.draggingComponent == null) {
            if (!isOpen) {
                if (TypeLists.INTERACTION_DEFAULTS!!.minimenuCursor) { // own cursor?
                    if (secondaryEntry != null) {
                        return secondaryEntry!!.cursor
                    }
                } else if (activeEntry != null) { // default cursor?
                    return activeEntry!!.cursor
                }
            } else {
                val minimenuFont = FontMetrics.getForMinimenu()
                val mouseX = MouseRecord.recorder!!.getMouseX()
                val mouseY = MouseRecord.recorder!!.getMouseY()
                var hoveredIndex: Int
                var entryIndex: Int
                var entry: MiniMenuEntry?
                var entryY: Int
                if (!isCollapsed) {
                    if (mouseX > StaticMethods.menuOpenX && mouseX < StaticMethods.menuOpenX + menuWidth) {
                        hoveredIndex = -1
                        entryIndex = 0
                        while (entryIndex < entryCount) {
                            if (FontMetrics.useBoldFont) {
                                entryY = (entryCount - 1 - entryIndex) * entryRowHeight + minimenuFont.topPadding + StaticMethods.menuOpenY + 1 + 20
                            } else {
                                entryY = (entryCount - 1 - entryIndex) * entryRowHeight + StaticMethods.menuOpenY + 31
                            }
                            if (mouseY > entryY - minimenuFont.topPadding - 1 && mouseY < entryY + minimenuFont.bottomPadding) {
                                hoveredIndex = entryIndex
                            }
                            entryIndex++
                        }
                        if (hoveredIndex != -1) {
                            entryIndex = 0
                            val nodeIterator = NodeIterator(entryList)
                            entry = nodeIterator.nextAndReset() as? MiniMenuEntry
                            while (entry != null) {
                                if (entryIndex++ == hoveredIndex) {
                                    return entry.cursor
                                }
                                entry = nodeIterator.next() as? MiniMenuEntry
                            }
                        }
                    }
                } else {
                    var queueIterator: QueueIterator
                    if (mouseX > StaticMethods.menuOpenX && mouseX < StaticMethods.menuOpenX + menuWidth) {
                        hoveredIndex = -1
                        entryIndex = 0
                        while (entryIndex < coloredEntryCount) {
                            if (FontMetrics.useBoldFont) {
                                entryY = entryIndex * entryRowHeight + minimenuFont.topPadding + StaticMethods.menuOpenY + 1 + 20
                            } else {
                                entryY = entryIndex * entryRowHeight + StaticMethods.menuOpenY + 31
                            }
                            if (mouseY > entryY - minimenuFont.topPadding - 1 && mouseY < entryY + minimenuFont.bottomPadding) {
                                hoveredIndex = entryIndex
                            }
                            entryIndex++
                        }
                        if (hoveredIndex != -1) {
                            entryIndex = 0
                            queueIterator = QueueIterator(sortedColoredEntries)
                            var coloredEntry: ColoredMiniMenuEntry? = queueIterator.nextAndReset() as? ColoredMiniMenuEntry
                            while (coloredEntry != null) {
                                if (entryIndex++ == hoveredIndex) {
                                    return ((coloredEntry.nodeQueue.current.nextCache as MiniMenuEntry).cursor)
                                }
                                coloredEntry = queueIterator.next() as? ColoredMiniMenuEntry
                            }
                        }
                    } else if (activeSubmenu != null && mouseX > submenuX && mouseX < submenuWidth + submenuX) {
                        hoveredIndex = -1
                        entryIndex = 0
                        while (entryIndex < activeSubmenu!!.queueSize) {
                            if (FontMetrics.useBoldFont) {
                                entryY = entryIndex * entryRowHeight + minimenuFont.topPadding + submenuY + 1 + 20
                            } else {
                                entryY = entryIndex * entryRowHeight + submenuY + 31
                            }
                            if (mouseY > entryY - minimenuFont.topPadding - 1 && mouseY < entryY + minimenuFont.bottomPadding) {
                                hoveredIndex = entryIndex
                            }
                            entryIndex++
                        }
                        if (hoveredIndex != -1) {
                            entryIndex = 0
                            queueIterator = QueueIterator(activeSubmenu!!.nodeQueue)
                            entry = queueIterator.nextAndReset() as? MiniMenuEntry
                            while (entry != null) {
                                if (entryIndex++ == hoveredIndex) {
                                    return entry.cursor
                                }
                                entry = queueIterator.next() as? MiniMenuEntry
                            }
                        }
                    }
                }
            }
        }
        return -1
    }

    @JvmStatic
    fun closeSubmenu() {
        if (activeSubmenu != null) {
            activeSubmenu = null
            InterfaceComponentRenderer.inboundsComponentToTop(submenuX, submenuY, submenuWidth, StationaryWall.menuHeight)
        }

    }

    @JvmField
    var submenuX = 0

    @JvmStatic
    fun renderOpenMenuStyled(toolkit: AbstractToolkit) {
        val menuX = StaticMethods.menuOpenX
        val menuY = StaticMethods.menuOpenY
        val menuW = menuWidth
        val menuH = menuHeight - 3
        val headerSize = 20
        if (FontMetrics.boldFontRenderer == null || StaticMedia.boldFontMetrics == null) {
            FontMetrics.boldFontRenderer = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, StaticMethods.menuFontIndex, true, true) as? AbstractFontRenderer
            StaticMedia.boldFontMetrics = StaticMethods.initializedFontcombos!!.list(Client.FONT_PROVIDER, StaticMethods.menuFontIndex)
            if (FontMetrics.boldFontRenderer != null && StaticMedia.boldFontMetrics != null) {
                close()
                var centeredX = menuW / 2 + menuX
                if (centeredX + menuW > GameShell.screenCanvasWidth) {
                    centeredX = GameShell.screenCanvasWidth - menuW
                }
                if (centeredX < 0) {
                    centeredX = 0
                }
                StaticMethods.openContextMenu(centeredX, StaticMethods.menuOpenY)
                return
            }
        }
        val fontRenderer: AbstractFontRenderer?
        if (FontMetrics.boldFontRenderer != null && StaticMedia.boldFontMetrics != null) {
            fontRenderer = FontMetrics.boldFontRenderer
        } else {
            fontRenderer = minimenuHeader
        }
        val fontMetrics = FontMetrics.getForMinimenu()
        NativeSprite.drawLoadingBarBackground(toolkit, StaticMethods.menuOpenX, StaticMethods.menuOpenY, menuWidth, menuHeight, headerSize, fontRenderer!!, fontMetrics!!, LocalizedText.CHOOSE_OPTION.translate(Client.CURRENT_LANGUAGE!!)!!)
        var alpha = 255 - StaticMethods.menuBackgroundAlpha - NativeSprite.alphaJitter
        if (alpha < 0) {
            alpha = 0
        }
        val mouseX = MouseRecord.recorder!!.getMouseX()
        val mouseY = MouseRecord.recorder!!.getMouseY()
        var entryIndex: Int
        var entry: MiniMenuEntry?
        var entryY: Int
        var coloredEntry: ColoredMiniMenuEntry?
        if (!isCollapsed) {
            entryIndex = 0
            var currentEntry: MiniMenuEntry? = entryList.getFirst() as? MiniMenuEntry
            while (currentEntry != null) {
                entry = currentEntry
                entryY = menuY + headerSize + (entryCount - 1 - entryIndex) * entryRowHeight + fontMetrics.topPadding + 1
                if (mouseX > StaticMethods.menuOpenX && mouseX < StaticMethods.menuOpenX + menuWidth && mouseY > entryY - fontMetrics.topPadding - 1 && mouseY < entryY + fontMetrics.bottomPadding && entry.isSelectable) {
                    toolkit.drawFilledRectangle(StaticMethods.menuOpenX, entryY - fontMetrics.topPadding, menuWidth, entryRowHeight, alpha shl 24 or menuHoverHighlightColor, 1)
                }
                ++entryIndex
                currentEntry = entryList.next() as? MiniMenuEntry
            }
        } else {
            entryIndex = 0
            var currentColoredEntry: ColoredMiniMenuEntry? = sortedColoredEntries.getNext() as? ColoredMiniMenuEntry
            while (currentColoredEntry != null) {
                coloredEntry = currentColoredEntry
                entryY = menuY + headerSize + entryIndex * entryRowHeight + fontMetrics.topPadding + 1
                if (mouseX > StaticMethods.menuOpenX && mouseX < menuWidth + StaticMethods.menuOpenX && mouseY > entryY - fontMetrics.topPadding - 1 && mouseY < entryY + fontMetrics.bottomPadding && (coloredEntry.queueSize > 1 || ((coloredEntry.nodeQueue.current.nextCache as MiniMenuEntry).isSelectable))) {
                    toolkit.drawFilledRectangle(StaticMethods.menuOpenX, entryY - fontMetrics.topPadding, menuWidth, entryRowHeight, alpha shl 24 or menuHoverHighlightColor, 1)
                }
                ++entryIndex
                currentColoredEntry = sortedColoredEntries.getDirectNext() as? ColoredMiniMenuEntry
            }
            if (activeSubmenu != null) {
                NativeSprite.drawLoadingBarBackground(toolkit, submenuX, submenuY, submenuWidth, StationaryWall.menuHeight, headerSize, fontRenderer, fontMetrics, activeSubmenu!!.tagStart)
                entryIndex = 0
                entry = activeSubmenu!!.nodeQueue.getNext() as? MiniMenuEntry
                while (entry != null) {
                    entryY = headerSize + entryIndex * entryRowHeight + fontMetrics.topPadding + submenuY + 1
                    if (mouseX > submenuX && mouseX < submenuWidth + submenuX && mouseY > entryY - fontMetrics.topPadding - 1 && mouseY < entryY + fontMetrics.bottomPadding && entry.isSelectable) {
                        toolkit.drawFilledRectangle(submenuX, entryY - fontMetrics.topPadding, submenuWidth, entryRowHeight, alpha shl 24 or menuHoverHighlightColor, 1)
                    }
                    ++entryIndex
                    entry = activeSubmenu!!.nodeQueue.getDirectNext() as? MiniMenuEntry
                }
                StaticMethods.drawMenuBorderSprites(toolkit, submenuX, submenuY, submenuWidth, StationaryWall.menuHeight, headerSize)
            }
        }
        StaticMethods.drawMenuBorderSprites(toolkit, StaticMethods.menuOpenX, StaticMethods.menuOpenY, menuWidth, menuHeight, headerSize)
        if (!isCollapsed) {
            entryIndex = 0
            var currentEntry2: MiniMenuEntry? = entryList.getFirst() as? MiniMenuEntry
            while (currentEntry2 != null) {
                entry = currentEntry2
                entryY = menuY + headerSize + (entryCount - 1 - entryIndex) * entryRowHeight + fontMetrics.topPadding + 1
                drawMenuEntry(mouseX, mouseY, menuX, menuW, entryY, entry, fontRenderer, fontMetrics, menuTextColor or 0xffffff.inv(), StaticMethods.menuHoverTextColor or 0xffffff.inv())
                ++entryIndex
                currentEntry2 = entryList.next() as? MiniMenuEntry
            }
        } else {
            entryIndex = 0
            var currentColoredEntry2: ColoredMiniMenuEntry? = sortedColoredEntries.getNext() as? ColoredMiniMenuEntry
            while (currentColoredEntry2 != null) {
                coloredEntry = currentColoredEntry2
                entryY = headerSize + entryIndex * entryRowHeight + fontMetrics.topPadding + StaticMethods.menuOpenY + 1
                if (coloredEntry.queueSize == 1) {
                    drawMenuEntry(mouseX, mouseY, StaticMethods.menuOpenX, menuWidth, entryY, (coloredEntry.nodeQueue.current.nextCache as MiniMenuEntry), fontRenderer, fontMetrics, menuTextColor or 0xffffff.inv(), StaticMethods.menuHoverTextColor or 0xffffff.inv())
                } else {
                    StaticMethods.drawColoredMenuEntry(mouseX, mouseY, StaticMethods.menuOpenX, menuWidth, entryY, coloredEntry, fontRenderer, fontMetrics, menuTextColor or 0xffffff.inv(), StaticMethods.menuHoverTextColor or 0xffffff.inv())
                }
                ++entryIndex
                currentColoredEntry2 = sortedColoredEntries.getDirectNext() as? ColoredMiniMenuEntry
            }
            if (activeSubmenu != null) {
                entryIndex = 0
                entry = activeSubmenu!!.nodeQueue.getNext() as? MiniMenuEntry
                while (entry != null) {
                    entryY = headerSize + entryIndex * entryRowHeight + fontMetrics.topPadding + submenuY + 1
                    drawMenuEntry(mouseX, mouseY, submenuX, submenuWidth, entryY, entry, fontRenderer, fontMetrics, menuTextColor or 0xffffff.inv(), StaticMethods.menuHoverTextColor or 0xffffff.inv())
                    ++entryIndex
                    entry = activeSubmenu!!.nodeQueue.getDirectNext() as? MiniMenuEntry
                }
            }
        }
    }

    @JvmStatic
    fun renderOpenMenu(toolkit: AbstractToolkit) {
        if (!FontMetrics.useBoldFont) {
            renderOpenMenuSimple(toolkit)
        } else {
            renderOpenMenuStyled(toolkit)
        }
    }

    @JvmField
    var activeEntry: MiniMenuEntry? = null

    @JvmField
    var menuWidth = 0

    @JvmField
    var menuHeight = 0

    @JvmField
    var useOnCursor = 0

    @JvmField
    var minimenuHeader: AbstractFontRenderer? = null

    @JvmField
    var menuHoverHighlightColor = 0

    @JvmField
    var USE_INTERFACE_ID = 0

    @JvmField
    val lowPriorityEntries = NodeCollection()

    @JvmField
    val interfaceEntries = NodeCollection()

    @JvmStatic
    fun setViewportOrigin(x: Int, y: Int) {
        viewportX = x
        viewportY = y
    }

    @JvmStatic
    fun addCancelOption() {
        cancelOption = MiniMenuEntry(LocalizedText.CANCEL.translate(Client.CURRENT_LANGUAGE!!)!!, "", Client.defaultMenuCursor, 1006, -1, 0L, 0, 0, true, false, 0L, true)
    }

    @JvmField
    var menuTextColor = 0

    @JvmStatic
    fun drawMenuEntry(mouseX: Int, mouseY: Int, menuX: Int, menuW: Int, entryY: Int, entry: MiniMenuEntry, fontRenderer: AbstractFontRenderer?, fontMetrics: FontMetrics, textColor: Int, hoverColor: Int) {
        var activeColor = textColor
        if (mouseX > menuX && mouseX < menuX + menuW && mouseY > entryY - fontMetrics.topPadding - 1 && mouseY < fontMetrics.bottomPadding + entryY && entry.isSelectable) {
            activeColor = hoverColor
        }
        val questIds = getQuestsFrom(entry)
        var text = getTextFrom(entry)
        if (questIds != null) {
            text += ChatBox.addQuestChatIcons(questIds)
        }
        @Suppress("UNCHECKED_CAST")
        fontRenderer?.renderAntiMacroText(text, menuX + 3, entryY, activeColor, Client.RANDOMIZER, Client.randomSeed, StaticMedia.chatIcons as Array<NativeSprite>?, StaticMedia.chatScaleHeights)
        if (entry.showOtherLevelIcon) {
            StaticMedia.otherLevelSprite!!.draw(5 + menuX + fontMetrics.getWidthNoSprites(text), entryY - fontMetrics.topPadding)
        }
    }

    @JvmField
    var submenuWidth = 0
}
