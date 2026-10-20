package com.rs.game.model.entity.player.managers

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.Maps
import com.rs.db.IfaceTracker
import com.rs.game.content.BuffBar
import com.rs.game.content.achievements.Achievement
import com.rs.game.content.achievements.AchievementInterface
import com.rs.game.model.entity.player.Player
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Component
import org.darkan.core.gameval.gen.Interface
import org.darkan.core.gameval.gen.Varc
import org.darkan.core.gameval.gen.Varp

class InterfaceManager(private val player: Player) {

    var topInterface = 0
        private set

    val tracker = IfaceTracker(player)

    fun opened(): Map<Int, Int> = synchronized(openedInterfaces) { HashMap(openedInterfaces) }

    private val openedInterfaces: BiMap<Int, Int> = Maps.synchronizedBiMap(HashBiMap.create())

    enum class ScreenMode {
        FULLSCREEN,
        FIXED,
        RESIZEABLE,
        FULLSCREEN2MAYBE;

        fun resizeable() = this == RESIZEABLE || this == FULLSCREEN

        companion object {
            @JvmStatic
            fun forId(id: Int): ScreenMode? = entries.getOrNull(id)
        }
    }

    enum class Sub(
        val resizeable: Int,
        val fixed: Int,
        private val defaultInter: Int = -1,
        val defaultProcedure: (Player) -> Unit = {},
    ) {
        RENDER_SPACE(48, 0),
        CENTRAL(29, 44),
        FULL_GAMESPACE_BG(2, 28),
        FULL_GAMESPACE_BG2(11, 29),
        MINIGAME_HUD(12, 47),
        FADING(13, 48),
        ABOVE_CHATBOX(27, 43),
        BUFF_BAR(458, 425, Interface.buff_bar, BuffBar::refresh),
        LEVEL_UP(35, 46),
        ACHIEVEMENT_NOTIF(41, 271),
        FULLSCREEN_BG(42, 15),
        FULLSCREEN_BG2(44, 17),
        FULLSCREEN_FG(45, 18),
        FULLSCREEN_FG2(46, 204),
        CHATBOX(22, 168, CHATBOX_TOP),
        CHATBAR_SETTINGS(23, 53, Interface.interface_751, { it.interfaceManager.sendChatBoxInterface(CHATBOX_LINES, Interface.interface_137) }),
        MULTICOMBAT_SIGN(16, 40, Interface.interface_745),
        SYSTEM_UPDATE(25, 42, Interface.interface_754),
        LOGOUT(130, 194, Interface.interface_182),
        XP_DROPS(39, 16, Interface.interface_1213, { it.packets.setIFHidden(if (it.resizeable()) RESIZEABLE_TOP else FIXED_TOP, if (it.resizeable()) 39 else 16, !it.skills.xpDropsActive()) }),
        XP_COUNTER(202, 50, Interface.interface_1215),
        ORB_HP(196, 160, Interface.interface_748),
        ORB_PRAYER(197, 161, Interface.interface_749),
        ORB_SUMMONING(199, 164, Interface.interface_747),
        ORB_RUN(198, 162, Interface.interface_750, Player::sendRunButtonConfig),
        ORB_MONEYPOUCH(208, 167),
        INVENTORY_OVERLAY(109, 172),
        TAB_COMBAT(112, 176, Interface.interface_884, { it.combatDefinitions.sendUnlockAttackStylesButtons() }),
        TAB_ACHIEVEMENT(113, 177, Interface.interface_1056, AchievementInterface::init),
        TAB_SKILLS(114, 178, Interface.interface_320),
        TAB_QUEST(115, 179, Interface.interface_190, { it.questManager.unlockQuestTabOptions() }),
        TAB_INVENTORY(116, 180, Interface.inventory, { it.inventory.unlockInventoryOptions() }),
        TAB_EQUIPMENT(117, 181, Interface.interface_387),
        TAB_PRAYER(118, 182, Interface.interface_271, { it.prayer.unlockPrayerBookButtons() }),
        TAB_MAGIC(119, 183) {
            override fun getDefaultInterfaceId(player: Player) = player.combatDefinitions.getSpellbook()?.interfaceId ?: -1
        },
        TAB_FOLLOWER(120, 184) {
            override fun getDefaultInterfaceId(player: Player) = if (player.familiar != null || player.pet != null) Interface.interface_662 else -1
        },
        TAB_FRIENDS(121, 185, Interface.interface_550),
        TAB_FRIENDS_CHAT(122, 186, Interface.interface_1109),
        TAB_CLAN_CHAT(123, 187, Interface.interface_1110),
        TAB_SETTINGS(124, 188, Interface.interface_261),
        TAB_EMOTES(125, 189, Interface.interface_590, { it.emotesManager.unlockEmotesBook() }),
        TAB_MUSIC(126, 190, Interface.interface_187, { it.musicsManager.unlockMusicPlayer() }),
        TAB_NOTES(127, 191, Interface.interface_34);

        open fun getDefaultInterfaceId(player: Player) = defaultInter

        fun getComponent(resizeableScreen: Boolean) = if (resizeableScreen) resizeable else fixed

        fun isClicked(interfaceId: Int, componentId: Int) =
            (interfaceId == RESIZEABLE_TOP && componentId == resizeable) || (interfaceId == FIXED_TOP && componentId == fixed)

        fun getHash(oldMode: ScreenMode): Int =
            Utils.toInterfaceHash(if (oldMode.resizeable()) RESIZEABLE_TOP else FIXED_TOP, if (oldMode.resizeable()) resizeable else fixed)

        companion object {
            @JvmField
            val ALL_GAME_TABS = arrayOf(TAB_COMBAT, TAB_ACHIEVEMENT, TAB_SKILLS, TAB_QUEST, TAB_INVENTORY, TAB_EQUIPMENT, TAB_PRAYER, TAB_MAGIC, TAB_FOLLOWER, TAB_FRIENDS, TAB_FRIENDS_CHAT, TAB_CLAN_CHAT, TAB_SETTINGS, TAB_EMOTES, TAB_MUSIC, TAB_NOTES, ORB_RUN)

            private val BY_HASH = HashMap<Int, Sub>().apply {
                for (sub in Sub.entries) {
                    if (put(Utils.toInterfaceHash(FIXED_TOP, sub.fixed), sub) != null)
                        System.err.println("Duplicate fixed hash for: $sub - ${sub.fixed}")
                    if (put(Utils.toInterfaceHash(RESIZEABLE_TOP, sub.resizeable), sub) != null)
                        System.err.println("Duplicate resizeable hash for: $sub - ${sub.resizeable}")
                }
            }

            @JvmStatic
            fun forHash(hash: Int): Sub? = BY_HASH[hash]
        }
    }

    fun sendGameWindowSub(resizable: Int, fixed: Int, interfaceId: Int, clickThrough: Boolean) {
        sendSubSpecific(clickThrough, defaultTopInterface(), if (player.resizeable()) resizable else fixed, interfaceId)
    }

    fun removeGameWindowSub(resizableComponentId: Int, fixedComponentId: Int) {
        removeSubSpecific(defaultTopInterface(), if (player.resizeable()) resizableComponentId else fixedComponentId)
    }

    fun sendChatBoxInterface(interfaceId: Int) = sendSubSpecific(true, CHATBOX_TOP, CHATBOX_SUB, interfaceId)

    fun sendChatBoxInterface(componentId: Int, interfaceId: Int) = sendSubSpecific(true, CHATBOX_TOP, componentId, interfaceId)

    fun closeChatBoxInterface() = removeSubSpecific(CHATBOX_TOP, CHATBOX_SUB)

    fun containsChatBoxInter() = isSubOpen(CHATBOX_TOP, CHATBOX_SUB)

    fun isSubOpen(parentInterfaceId: Int, parentInterfaceComponentId: Int) =
        openedInterfaces.containsKey(getComponentUId(parentInterfaceId, parentInterfaceComponentId))

    fun isSubOpenWindow(componentId: Int) = isSubOpen(defaultTopInterface(), componentId)

    fun sendSubSpecific(clickThrough: Boolean, parentInterfaceId: Int, parentInterfaceComponentId: Int, interfaceId: Int) {
        val parentComponentUid = getComponentUId(parentInterfaceId, parentInterfaceComponentId)
        openedInterfaces[parentComponentUid]?.let { clearChilds(it) }
        openedInterfaces[parentComponentUid] = interfaceId
        tracker.opened(parentComponentUid, interfaceId, clickThrough)
        player.packets.sendInterface(clickThrough, parentInterfaceId, parentInterfaceComponentId, interfaceId)
    }

    fun removeSubSpecific(parentInterfaceId: Int, parentInterfaceComponentId: Int) =
        removeInterfaceByParent(getComponentUId(parentInterfaceId, parentInterfaceComponentId))

    fun removeInterfaceByParent(parentUid: Int) {
        val removedInterface = openedInterfaces.remove(parentUid) ?: return
        clearChilds(removedInterface)
        tracker.closed(removedInterface)
        player.packets.closeInterface(parentUid)
    }

    fun getInterfaceParentId(interfaceId: Int): Int {
        if (interfaceId == topInterface)
            return -1
        return openedInterfaces.inverse()[interfaceId] ?: -1
    }

    private fun clearChilds(parentInterfaceId: Int) {
        openedInterfaces.keys.removeIf { it shr 16 == parentInterfaceId }
    }

    fun removeInterface(interfaceId: Int) {
        val parentUid = getInterfaceParentId(interfaceId)
        if (parentUid != -1)
            removeInterfaceByParent(parentUid)
    }

    fun topOpen(interfaceId: Int) = interfaceId == topInterface || openedInterfaces.containsValue(interfaceId)

    @JvmOverloads
    fun sendSub(sub: Sub, interfaceId: Int, clickThrough: Boolean = true) {
        if (interfaceId == -1)
            return
        sendGameWindowSub(sub.resizeable, sub.fixed, interfaceId, clickThrough)
    }

    fun removeSubs(vararg subs: Sub) = subs.forEach(::removeSub)

    fun removeSub(sub: Sub) = removeGameWindowSub(sub.resizeable, sub.fixed)

    fun sendSubDefault(sub: Sub) {
        sendSub(sub, sub.getDefaultInterfaceId(player), true)
        sub.defaultProcedure(player)
    }

    fun sendSubDefaults(vararg subs: Sub) = subs.forEach(::sendSubDefault)

    fun isSubClick(sub: Sub, interfaceId: Int, componentId: Int) = sub.isClicked(interfaceId, componentId)

    fun flashTab(tab: Sub) {
        player.vars.setVar(Varp.varp_1021, tabIndex(tab) + 1)
    }

    fun openTab(tab: Sub) {
        player.packets.sendVarc(Varc.varc_168, maxOf(tabIndex(tab), 0))
    }

    private fun tabIndex(tab: Sub) = when (tab) {
        Sub.TAB_COMBAT -> 0
        Sub.TAB_ACHIEVEMENT -> 1
        Sub.TAB_SKILLS -> 2
        Sub.TAB_QUEST -> 3
        Sub.TAB_INVENTORY -> 4
        Sub.TAB_EQUIPMENT -> 5
        Sub.TAB_PRAYER -> 6
        Sub.TAB_MAGIC -> 7
        Sub.TAB_FOLLOWER -> 8
        Sub.TAB_FRIENDS -> 9
        Sub.TAB_FRIENDS_CHAT -> 10
        Sub.TAB_CLAN_CHAT -> 11
        Sub.TAB_SETTINGS -> 12
        Sub.TAB_EMOTES -> 13
        Sub.TAB_MUSIC -> 14
        Sub.TAB_NOTES -> 15
        Sub.ORB_RUN -> 16
        else -> -1
    }

    fun flashTabOff() {
        player.vars.setVar(Varp.varp_1021, 0)
    }

    fun sendInterfaces() {
        openedInterfaces.clear()
        tracker.reset()
        setDefaultTopInterface()
        if (player.familiar != null && player.isRunning)
            player.familiar.sendMainConfigs()
        Sub.entries.forEach(::sendSubDefault)
        player.controllerManager.sendInterfaces()
    }

    fun switchDisplayModes(screenMode: ScreenMode) {
        if (player.screenMode == screenMode)
            return
        val old = closeGameFrameSubs()
        val oldMode = player.screenMode
        player.screenMode = screenMode
        setDefaultTopInterface()
        restoreGameFrameSubs(old, oldMode)
    }

    private fun closeGameFrameSubs(): Map<Int, Int> {
        val open = HashMap(openedInterfaces)
        for (parentUid in open.keys)
            Sub.forHash(parentUid)?.let(::removeSub)
        openedInterfaces.clear()
        return open
    }

    private fun restoreGameFrameSubs(open: Map<Int, Int>, mode: ScreenMode) {
        if (player.familiar != null && player.isRunning)
            player.familiar.sendMainConfigs()
        for (sub in Sub.entries) {
            val prev = open[sub.getHash(mode)] ?: continue
            sendSub(sub, prev, sub != Sub.CENTRAL)
            sub.defaultProcedure(player)
        }
    }

    @JvmOverloads
    fun sendInterface(interfaceId: Int, clickThrough: Boolean = false) {
        val known = Utils.getInterfaceDefinitionsSize()
        if (known > 0 && interfaceId > known)
            return
        sendSub(Sub.CENTRAL, interfaceId, clickThrough)
    }

    fun sendInventoryInterface(interfaceId: Int) = sendSub(Sub.INVENTORY_OVERLAY, interfaceId, false)

    fun sendInventoryOverlay(interfaceId: Int) = sendSub(Sub.INVENTORY_OVERLAY, interfaceId, true)

    fun containsReplacedChatBoxInter() = isSubOpen(CHATBOX_TOP, CHATBOX_REPLACEMENT)

    fun replaceRealChatBoxInterface(interfaceId: Int) = sendSubSpecific(true, CHATBOX_TOP, CHATBOX_REPLACEMENT, interfaceId)

    fun closeReplacedRealChatBoxInterface() = removeSubSpecific(CHATBOX_TOP, CHATBOX_REPLACEMENT)

    fun setDefaultTopInterface() = setTopInterface(defaultTopInterface(), false)

    fun defaultTopInterface() = if (player.resizeable()) RESIZEABLE_TOP else FIXED_TOP

    fun isDefaultTopOpen() = topInterface == defaultTopInterface()

    fun setTopInterface(rootInterface: Int, gc: Boolean) {
        topInterface = rootInterface
        player.packets.sendWindowsPane(rootInterface, if (gc) 3 else 0)
    }

    fun setWindowsPane(windowsPane: Int) {
        topInterface = windowsPane
        player.packets.sendWindowsPane(windowsPane, 2)
    }

    fun sendAchievementComplete(achievement: Achievement?) {
        if (achievement == null)
            return
        sendSub(Sub.ACHIEVEMENT_NOTIF, Interface.interface_1055)
        player.packets.sendVarc(Varc.varc_1425, achievement.id)
    }

    fun containsScreenInter() = isSubOpenWindow(Sub.CENTRAL.getComponent(player.resizeable()))

    fun containsInventoryInter() = isSubOpenWindow(Sub.INVENTORY_OVERLAY.getComponent(player.resizeable()))

    fun removeInventoryInterface() = removeSub(Sub.INVENTORY_OVERLAY)

    fun setFadingInterface(interfaceId: Int) = sendSub(Sub.FADING, interfaceId)

    fun closeFadingInterface() = removeSub(Sub.FADING)

    fun removeCentralInterface() = removeSub(Sub.CENTRAL)

    fun setFullscreenInterface(backgroundInterface: Int, interfaceId: Int) {
        removeCentralInterface()
        sendSub(Sub.FULLSCREEN_BG, backgroundInterface, false)
        sendSub(Sub.FULLSCREEN_BG2, interfaceId, false)
        player.setCloseInterfacesEvent { removeSubs(Sub.FULLSCREEN_BG, Sub.FULLSCREEN_BG2) }
    }

    fun gazeOrbOfOculus() {
        val mode = player.screenMode
        val gameFrameSubs = closeGameFrameSubs()
        val chatBar = Utils.componentIdFromHash(Component.interface_475.component_475_57)
        val chatBox = Utils.componentIdFromHash(Component.interface_475.component_475_55)
        setTopInterface(Interface.interface_475, false)
        sendSubSpecific(true, Interface.interface_475, chatBar, Interface.interface_751)
        sendSubSpecific(true, Interface.interface_475, chatBox, Interface.interface_752)
        sendChatBoxInterface(CHATBOX_LINES, Interface.interface_137)
        player.setCloseInterfacesEvent {
            removeSubSpecific(Interface.interface_475, chatBar)
            removeSubSpecific(Interface.interface_475, chatBox)
            setDefaultTopInterface()
            restoreGameFrameSubs(gameFrameSubs, mode)
            player.packets.sendResetCamera()
        }
    }

    fun fadeIn() = setFadingInterface(Interface.interface_115)

    fun fadeOut() = setFadingInterface(Interface.interface_170)

    fun fadeInBG() = sendBackgroundInterfaceOverGameWindow(Interface.interface_115)

    fun fadeOutBG() = sendBackgroundInterfaceOverGameWindow(Interface.interface_170)

    fun sendBackgroundInterfaceOverGameWindow(id: Int) = sendSub(Sub.FULLSCREEN_BG, id)

    fun sendForegroundInterfaceOverGameWindow(id: Int) = sendSub(Sub.FULLSCREEN_BG2, id)

    fun closeInterfacesOverGameWindow() = removeSubs(Sub.FULLSCREEN_BG, Sub.FULLSCREEN_BG2)

    @JvmOverloads
    fun removeOverlay(fullGameWindow: Boolean = false) = removeSub(if (fullGameWindow) Sub.FULL_GAMESPACE_BG2 else Sub.MINIGAME_HUD)

    @JvmOverloads
    fun sendOverlay(id: Int, fullGameWindow: Boolean = false) = sendSub(if (fullGameWindow) Sub.FULL_GAMESPACE_BG2 else Sub.MINIGAME_HUD, id)

    companion object {
        const val FIXED_TOP = Interface.interface_548
        const val RESIZEABLE_TOP = Interface.interface_746
        const val CHATBOX_TOP = Interface.interface_752

        private val CHATBOX_SUB = Utils.componentIdFromHash(Component.interface_752.component_752_13)
        private val CHATBOX_REPLACEMENT = Utils.componentIdFromHash(Component.interface_752.component_752_11)
        private val CHATBOX_LINES = Utils.componentIdFromHash(Component.interface_752.component_752_9)

        private fun getComponentUId(interfaceId: Int, componentId: Int) = interfaceId shl 16 or componentId
    }
}
