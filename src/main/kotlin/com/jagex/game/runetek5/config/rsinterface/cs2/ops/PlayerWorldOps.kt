package com.jagex.game.runetek5.config.rsinterface.cs2.ops

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.cache.FileOnDisk
import com.jagex.game.runetek5.cache.node.IntNode
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.Node
import com.jagex.game.runetek5.cache.node.Queue
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.input.mouse.Mouse
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.client.login.AccountCreationResponseOpcodes
import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.CreateAccountRequest
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.menu.MiniMenuEntry
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.client.window.DisplayModeDetails
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.bas.BasType
import com.jagex.game.runetek5.config.clanvar.ClanVarType
import com.jagex.game.runetek5.config.clanvar.ClanVarTypeList
import com.jagex.game.runetek5.config.clanvarsettings.ClanVarSettingsType
import com.jagex.game.runetek5.config.enumtype.EnumType
import com.jagex.game.runetek5.config.material.texture.ColorImageCache
import com.jagex.game.runetek5.config.material.texture.operation.TextureOpIrregularBricks
import com.jagex.game.runetek5.config.mec.MECType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.param.ParamType
import com.jagex.game.runetek5.config.quest.QuestType
import com.jagex.game.runetek5.config.quickchat.QuickChatCategoryType
import com.jagex.game.runetek5.config.quickchat.QuickChatMessage
import com.jagex.game.runetek5.config.quickchat.QuickchatMessageType
import com.jagex.game.runetek5.config.rsinterface.InteractiveComponentSprite
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.ComponentType
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.IFEvents
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainerType
import com.jagex.game.runetek5.config.sequence.InterfaceSequence
import com.jagex.game.runetek5.config.staticelements.StaticElements
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.entity.BoundingBoxPolygon
import com.jagex.game.runetek5.entity.SceneGraphEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCTypeCustomisation
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.FontMetrics
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.graphics.hint.HintRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapCategory
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapElement
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapInteraction
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapDisplay
import com.jagex.game.runetek5.graphics.model.MediaType
import com.jagex.game.runetek5.graphics.model.particle.ParticleSystemManager
import com.jagex.game.runetek5.graphics.sprite.NativeSprite
import com.jagex.game.runetek5.graphics.sprite.Sprite
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorX
import com.jagex.game.runetek5.graphics.sprite.meta.AnchorY
import com.jagex.game.runetek5.interaction.Friend
import com.jagex.game.runetek5.interaction.FriendList
import com.jagex.game.runetek5.interaction.Ignore
import com.jagex.game.runetek5.interaction.IgnoreList
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat
import com.jagex.game.runetek5.math.Vector3
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.ChatLine
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.PacketDecoder
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.network.world.WorldDescriptor
import com.jagex.game.runetek5.rendering.MessageBox
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.*
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.VorbisSoundManager
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.ProfileCpu
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Browser
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.*
import com.jagex.game.runetek5.vardomain.VarProviders
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Interpreter
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2CallFrame
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode

object PlayerWorldOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.CLIENTCLOCK -> clientClock(exec)
            ClientScriptOpCode.INV_GETOBJ -> invGetobj(exec)
            ClientScriptOpCode.INV_GETNUM -> invGetnum(exec)
            ClientScriptOpCode.INV_TOTAL -> invTotal(exec)
            ClientScriptOpCode.INV_SIZE -> invSize(exec)
            ClientScriptOpCode.INV_STOCKBASE -> invStockBase(exec)
            ClientScriptOpCode.STAT -> stat(exec)
            ClientScriptOpCode.STAT_BASE -> statBase(exec)
            ClientScriptOpCode.STAT_VISIBLE_XP -> statVisibleXp(exec)
            ClientScriptOpCode.COORD -> getMyPlayerPos(exec)
            ClientScriptOpCode.COORDX -> coordx(exec)
            ClientScriptOpCode.COORDY -> coordy(exec)
            ClientScriptOpCode.COORDZ -> coordz(exec)
            ClientScriptOpCode.MAP_MEMBERS -> mapMembers(exec)
            ClientScriptOpCode.INVOTHER_GETOBJ -> invOtherGetobj(exec)
            ClientScriptOpCode.INVOTHER_GETNUM -> invOtherGetnum(exec)
            ClientScriptOpCode.INVOTHER_TOTAL -> invOtherTotal(exec)
            ClientScriptOpCode.STAFFMODLEVEL -> staffModLevel(exec)
            ClientScriptOpCode.REBOOTTIMER -> rebootTimer(exec)
            ClientScriptOpCode.MAP_WORLD -> getWorldId(exec)
            ClientScriptOpCode.RUNENERGY_VISIBLE -> getRunEnergy(exec)
            ClientScriptOpCode.RUNWEIGHT_VISIBLE -> getRunWeight(exec)
            ClientScriptOpCode.PLAYERMOD -> playermod(exec)
            ClientScriptOpCode.PLAYERMODLEVEL -> playermodlevel(exec)
            ClientScriptOpCode.PLAYERMEMBER -> getPlayerIsMember(exec)
            ClientScriptOpCode.COMLEVEL_ACTIVE -> getPlayerCombatLevel(exec)
            ClientScriptOpCode.GENDER -> getPlayerGender(exec)
            ClientScriptOpCode.MAP_QUICKCHAT -> getWorldQuickchatOnly(exec)
            ClientScriptOpCode.INV_FREESPACE -> getContainerFreeSpace(exec)
            ClientScriptOpCode.INV_TOTALPARAM -> containerTotalParam(exec)
            ClientScriptOpCode.INV_TOTALPARAM_STACK -> containerTotalParamStack(exec)
            ClientScriptOpCode.MAP_LANG -> getWorldLanguage(exec)
            ClientScriptOpCode.MOVECOORD -> moveCoord(exec)
            ClientScriptOpCode.AFFILIATE -> affiliate(exec)
            ClientScriptOpCode.PROFILE_CPU -> getCPUProfile(exec)
            ClientScriptOpCode.PLAYERDEMO -> playerDemo(exec)
            ClientScriptOpCode.APPLET_HASFOCUS -> getAppletFocus(exec)
            ClientScriptOpCode.FROMBILLING -> fromBilling(exec)
            ClientScriptOpCode.GET_MOUSEX -> getMouseX(exec)
            ClientScriptOpCode.GET_MOUSEY -> getMouseY(exec)
            ClientScriptOpCode.GET_ACTIVE_MINIMENU_ENTRY -> getActiveMiniMenuEntry(exec)
            ClientScriptOpCode.GET_SECOND_MINIMENU_ENTRY -> getSecondMiniMenuEntry(exec)
            ClientScriptOpCode.GET_MINIMENU_LENGTH -> getMiniMenuLength(exec)
            ClientScriptOpCode.GET_CURRENTCURSOR -> getCurrentCursor(exec)
            ClientScriptOpCode.GET_SELFYANGLE -> getSelfYAngle(exec)
            ClientScriptOpCode.MAP_ISOWNER -> mapIsOwner(exec)
            ClientScriptOpCode.GET_MOUSEBUTTONS -> getMouseButtons(exec)
            ClientScriptOpCode.SELF_PLAYER_UID -> selfPlayerUid(exec)
            ClientScriptOpCode.GET_MINIMENU_TARGET -> getMiniMenuTarget(exec)
            ClientScriptOpCode.INV_GETTOOLTIP -> invGetTooltip(exec, false)
            ClientScriptOpCode.INV_OTHER_GETTOOLTIP -> invGetTooltip(exec, true)
            ClientScriptOpCode.GET_ACTIVE_MINIMENU_INVSLOT -> getActiveMiniMenuInvSlot(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by PlayerWorldOps")
        }
    }

    @JvmStatic
    fun getMyPlayerPos(executor: CS2Executor) {
        val plane = PlayerUpdate.MY_PLAYER!!.plane
        val pos: Vector3? = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
        val baseCoord: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        val x = (pos!!.x.toInt() shr 9) + baseCoord!!.x
        val y = (pos!!.z.toInt() shr 9) + baseCoord!!.y
        executor.intStack[++executor.intStackPtr - 1] = (plane.toInt() shl 28) + (x shl 14) + y
    }

    @JvmStatic
    fun getPlayerGender(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null && PlayerUpdate.MY_PLAYER!!.playerAppearance!!.female) 1 else 0
    }

    @JvmStatic
    fun mapIsOwner(executor: CS2Executor) {
        val ownerName: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.mapOwnerIdentifier != null && Client.mapOwnerIdentifier.equals(ownerName!!)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun affiliate(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.AFFILIATE
    }

    @JvmStatic
    fun getMouseButtons(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (MouseRecord.recorder!!.leftButtonDown()) 1  else 0
        executor.intStack[++executor.intStackPtr - 1] = if (MouseRecord.recorder!!.rightButtonDown()) 1  else 0
        executor.intStack[++executor.intStackPtr - 1] = if (MouseRecord.recorder!!.middleButtonDown()) 1  else 0
    }

    @JvmStatic
    fun mapMembers(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.memberWorld) 1  else 0
    }

    @JvmStatic
    fun invTotal(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val itemId = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getContainerTotal(containerId, itemId, false)
    }

    @JvmStatic
    fun invOtherTotal(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val itemId = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getContainerTotal(containerId, itemId, true)
    }

    @JvmStatic
    fun getPlayerIsMember(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.IS_MEMBER) 1  else 0
    }

    @JvmStatic
    fun getMouseY(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder!!.getMouseY()
    }

    @JvmStatic
    fun getAppletFocus(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (GameShell.appletHasFocus) 1  else 0
    }

    @JvmStatic
    fun fromBilling(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.billingFlag) 1  else 0
    }

    @JvmStatic
    fun containerTotalParamStack(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getParamBasedContainerTotal(containerId, paramId, true)
    }

    @JvmStatic
    fun playerDemo(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = 0
    }

    @JvmStatic
    fun invGetnum(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val slot = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getAmountAtSlot(containerId, slot, false)
    }

    @JvmStatic
    fun getMiniMenuTarget(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.interfaceSelectionActive) 1  else 0
        executor.stringStack[++executor.stringStackPtr - 1] = if (Client.useOnTarget == null) "" else Client.useOnTarget
        executor.stringStack[++executor.stringStackPtr - 1] = if (Client.useOnType == null) "" else Client.useOnType
    }

    @JvmStatic
    fun getSecondMiniMenuEntry(executor: CS2Executor) {
        pushMiniMenuEntry(MiniMenu.getSecondaryEntry()!!, executor)
    }

    @JvmStatic
    fun getCurrentCursor(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.CURRENT_CURSOR
    }

    @JvmStatic
    fun invSize(executor: CS2Executor) {
        val containerId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.CONTAINER!!.list(containerId).maxItemSize
    }

    @JvmStatic
    fun getMiniMenuLength(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = MiniMenu.entryCount
        executor.intStack[++executor.intStackPtr - 1] = MiniMenu.coloredEntryCount
    }

    @JvmStatic
    fun coordx(executor: CS2Executor) {
        val coord = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (coord shr 14) and 0x3fff
    }

    @JvmStatic
    fun playermod(executor: CS2Executor) {
        if (Client.PLAYER_MOD_LEVEL >= 5 && Client.PLAYER_MOD_LEVEL <= 9) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun containerTotalParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getParamBasedContainerTotal(containerId, paramId, false)
    }

    @JvmStatic
    fun getContainerFreeSpace(executor: CS2Executor) {
        val containerId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getFreeSlots(containerId)
    }

    @JvmStatic
    fun getWorldLanguage(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.CURRENT_LANGUAGE!!.getValue()
    }

    @JvmStatic
    fun invOtherGetnum(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val slot = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getAmountAtSlot(containerId, slot, true)
    }

    @JvmStatic
    fun moveCoord(executor: CS2Executor) {
        executor.intStackPtr -= 4
        var coord = executor.intStack[executor.intStackPtr]
        val dx = executor.intStack[executor.intStackPtr + 1]
        val dy = executor.intStack[executor.intStackPtr + 2]
        val dz = executor.intStack[executor.intStackPtr + 3]
        coord += (dx shl 14)
        coord += (dy shl 28)
        coord += dz
        executor.intStack[++executor.intStackPtr - 1] = coord
    }

    @JvmStatic
    fun getMouseX(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder!!.getMouseX()
    }

    @JvmStatic
    fun statVisibleXp(executor: CS2Executor) {
        val skillId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.SKILL_XP[skillId]
    }

    @JvmStatic
    fun getCPUProfile(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ProfileCpu.getProfile()
    }

    @JvmStatic
    fun invGetobj(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val slot = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getItemIdAtSlot(containerId, slot, false)
    }

    @JvmStatic
    fun statBase(executor: CS2Executor) {
        val skillId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.SKILL_LEVEL_ACTUAL[skillId]
    }

    @JvmStatic
    fun getSelfYAngle(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.MY_PLAYER!!.yaw.getRotation() shr 3
    }

    @JvmStatic
    fun playermodlevel(executor: CS2Executor) {
        if (Client.PLAYER_MOD_LEVEL >= 5 && Client.PLAYER_MOD_LEVEL <= 9) {
            executor.intStack[++executor.intStackPtr - 1] = Client.PLAYER_MOD_LEVEL
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun getWorldId(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.GAME_CONNECTION_INFO!!.worldId
    }

    @JvmStatic
    fun getRunEnergy(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.RUN_ENERGY
    }

    @JvmStatic
    fun getActiveMiniMenuEntry(executor: CS2Executor) {
        pushMiniMenuEntry(getActiveEntry(), executor)
    }

    @JvmStatic
    fun invGetTooltip(executor: CS2Executor, otherParty: Boolean) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val slot = executor.intStack[executor.intStackPtr + 1]
        executor.stringStack[++executor.stringStackPtr - 1] = ItemContainer.getTooltipAtSlot(containerId, slot, otherParty) ?: ""
    }

    @JvmStatic
    fun getActiveMiniMenuInvSlot(executor: CS2Executor) {
        var containerId = -1
        var slot = -1
        var otherParty = false
        val entry = if (MiniMenu.isOpen) null else getActiveEntry()
        if (entry != null && entry.itemId != -1 && Mouse.isClicktypeComponent(entry.clickType)) {
            var component = Component.fromHash(entry.param1)
            search@ while (component != null) {
                for (candidate in component.mouseLeaveArrayParams ?: intArrayOf()) {
                    for (other in booleanArrayOf(false, true)) {
                        val found = ItemContainer.findTooltipSlot(candidate, entry.param0, entry.itemId, other)
                        if (found != -1) {
                            containerId = candidate
                            slot = found
                            otherParty = other
                            break@search
                        }
                    }
                }
                component = if (component.parentId == -1) null else Component.fromHash(component.parentId)
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = containerId
        executor.intStack[++executor.intStackPtr - 1] = slot
        executor.intStack[++executor.intStackPtr - 1] = if (otherParty) 1 else 0
    }

    @JvmStatic
    fun getActiveEntry(): MiniMenuEntry? {
        return MiniMenu.activeEntry
    }

    @JvmStatic
    fun staffModLevel(executor: CS2Executor) {
        if (Client.PLAYER_RIGHTS >= 2) {
            executor.intStack[++executor.intStackPtr - 1] = Client.PLAYER_RIGHTS
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun coordz(executor: CS2Executor) {
        val coord = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = coord and 0x3fff
    }

    @JvmStatic
    fun selfPlayerUid(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.myPlayerIndex
    }

    @JvmStatic
    fun getWorldQuickchatOnly(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.IS_QUICKCHAT_ONLY) 1  else 0
    }

    @JvmStatic
    fun invStockBase(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerType: ItemContainerType? = TypeLists.CONTAINER!!.list(executor.intStack[executor.intStackPtr])
        val itemId = executor.intStack[executor.intStackPtr + 1]
        var amount = -1
        for (i in 0 until containerType!!.defaultItemSize) {
            if (itemId == containerType!!.itemIds!![i]) {
                amount = containerType!!.amounts!![i]
                break
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = amount
    }

    @JvmStatic
    fun clientClock(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.cycles
    }

    @JvmStatic
    fun getPlayerCombatLevel(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.MY_PLAYER!!.combatLevel
    }

    @JvmStatic
    fun rebootTimer(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.REBOOT_TIMER
    }

    @JvmStatic
    fun coordy(executor: CS2Executor) {
        val coord = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (coord shr 28)
    }

    @JvmStatic
    fun invOtherGetobj(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val containerId = executor.intStack[executor.intStackPtr]
        val slot = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getItemIdAtSlot(containerId, slot, true)
    }

    @JvmStatic
    fun stat(executor: CS2Executor) {
        val skillId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.SKILL_LEVEL_VISIBLE[skillId]
    }

    @JvmStatic
    fun getRunWeight(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.RUN_WEIGHT
    }

    @JvmStatic
    fun pushMiniMenuEntry(entry: MiniMenuEntry?, executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = MiniMenu.getClickType(entry)
        executor.stringStack[++executor.stringStackPtr - 1] = MiniMenu.getActionText(entry)
        executor.stringStack[++executor.stringStackPtr - 1] = MiniMenu.getColoredNameText(entry)
        executor.stringStack[++executor.stringStackPtr - 1] = MiniMenu.addQuestChaticons(entry)
    }

}
