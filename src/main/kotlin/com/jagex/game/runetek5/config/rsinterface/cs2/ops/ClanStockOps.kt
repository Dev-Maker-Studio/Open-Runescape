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

object ClanStockOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.ACTIVECLANSETTINGS_FIND_LISTENED -> activeClanSettingsFindListened(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_FIND_AFFINED -> activeClanSettingsFindAffined(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETCLANNAME -> activeClanSettingsGetClanName(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETALLOWUNAFFINED -> activeClanSettingsGetAllowUnaffined(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETRANKTALK -> activeClanSettingsGetRankTalk(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETRANKKICK -> activeClanSettingsGetRankKick(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETRANKLOOTSHARE -> activeClanSettingsGetRankLootShare(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETCOINSHARE -> activeClanSettingsGetCoinShare(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDCOUNT -> activeClanSettingsGetAffinedCount(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME -> activeClanSettingsGetAffinedDisplayName(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDRANK -> activeClanSettingsGetAffinedRank(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETBANNEDCOUNT -> activeClanSettingsGetBannedCount(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME -> activeClanSettingsGetBannedDisplayName(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO -> activeClanSettingsGetAffinedExtraInfo(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT -> activeClanSettingsGetCurrentOwnerSlot(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT -> activeClanSettingsGetReplacementOwnerSlot(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDSLOT -> activeClanSettingsGetAffinedSlot(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT -> activeClanSettingsGetSortedAffinedSlot(exec)
            ClientScriptOpCode.UNUSED_CLAN_OP -> unusedClanOp(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY -> activeClanSettingsGetAffinedJoinRuneday(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_FIND_LISTENED -> activeClanchannelFindListened(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_FIND_AFFINED -> activeClanchannelFindAffined(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETCLANNAME -> activeClanChannelGetClanName(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETRANKKICK -> activeClanChannelGetRankKick(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETRANKTALK -> activeClanChannelGetRankTalk(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERCOUNT -> activeClanChannelGetUserCount(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERDISPLAYNAME -> activeClanChannelGetUserDisplayName(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERRANK -> activeClanChannelGetUserRank(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERWORLD -> activeClanChannelGetUserWorld(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_KICKUSER -> activeClanChannelKickUser(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERSLOT -> activeClanChannelGetUserSlot(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETSORTEDUSERSLOT -> activeClanChannelGetSortedUserSlot(exec)
            ClientScriptOpCode.CLANPROFILE_FIND -> clanProfileFind(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERTYPE -> stockMarketGetOfferType(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERITEM -> getGEOfferItem(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERPRICE -> getGEOfferPrice(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERCOUNT -> stockMarketGetOfferCount(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERCOMPLETEDCOUNT -> stockMarketGetOfferCompletedCount(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERCOMPLETEDGOLD -> stockMarketGetOfferCompletedGold(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFEREMPTY -> stockMarketIsOfferEmpty(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFERSTABLE -> stockMarketIsOfferStable(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFERFINISHED -> stockMarketIsOfferFinished(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFERADDING -> stockMarketIsOfferAdding(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by ClanStockOps")
        }
    }

    @JvmStatic
    fun stockMarketIsOfferStable(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        val state = Client.GRAND_EXCHANGE_SLOTS[slot]!!.getSlotState()
        executor.intStack[++executor.intStackPtr - 1] = if (state == 2) 1 else 0
    }

    @JvmStatic
    fun activeClanSettingsGetBannedDisplayName(executor: CS2Executor) {
        val bannedSlot = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings!!.bannedUserNames!![bannedSlot]
    }

    @JvmStatic
    fun activeClanChannelGetSortedUserSlot(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = executor.clanChannel!!.getPlayersSortedByName()[executor.intStack[executor.intStackPtr - 1]]
    }

    @JvmStatic
    fun activeClanChannelGetUserCount(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.numPlayers
    }

    @JvmStatic
    fun stockMarketIsOfferEmpty(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        val state = Client.GRAND_EXCHANGE_SLOTS[slot]!!.getSlotState()
        executor.intStack[++executor.intStackPtr - 1] = if (state == 0) 1 else 0
    }

    @JvmStatic
    fun activeClanSettingsGetRankTalk(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.talkRank.toInt()
    }

    @JvmStatic
    fun activeClanSettingsGetReplacementOwnerSlot(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.replacementOwner
    }

    @JvmStatic
    fun activeClanSettingsGetAffinedSlot(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.getMemberIndex(executor.stringStack[--executor.stringStackPtr] as String?)
    }

    @JvmStatic
    fun activeClanSettingsGetSortedAffinedSlot(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = executor.currentClanSettings!!.getSortedAffinedSlots()[executor.intStack[executor.intStackPtr - 1]]
    }

    @JvmStatic
    fun stockMarketGetOfferCompletedGold(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[slot]!!.totalPrice
    }

    @JvmStatic
    fun activeClanChannelGetUserWorld(executor: CS2Executor) {
        val userSlot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.players!![userSlot]!!.world
    }

    @JvmStatic
    fun activeClanChannelGetRankKick(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.minRankToKick.toInt()
    }

    @JvmStatic
    fun activeClanSettingsGetAffinedRank(executor: CS2Executor) {
        val memberSlot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.memberRanks!![memberSlot].toInt()
    }

    @JvmStatic
    fun stockMarketIsOfferAdding(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        val state = Client.GRAND_EXCHANGE_SLOTS[slot]!!.getSlotState()
        val stack: IntArray? = executor.intStack
        val resultIndex = ++executor.intStackPtr - 1
        var result: Int
        if (state == 1) {
            result = 1
        } else {
            result = 0
        }
        stack!![resultIndex] = result
    }

    @JvmStatic
    fun getGEOfferPrice(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[slot]!!.price
    }

    @JvmStatic
    fun getGEOfferItem(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[slot]!!.itemId
    }

    @JvmStatic
    fun activeClanSettingsGetClanName(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings!!.clanName
    }

    @JvmStatic
    fun activeClanSettingsGetCurrentOwnerSlot(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.currentOwner
    }

    @JvmStatic
    fun stockMarketGetOfferCount(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[slot]!!.amount
    }

    @JvmStatic
    fun activeClanchannelFindAffined(executor: CS2Executor) {
        if (Client.CLAN_CHANNEL != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
            executor.clanChannel = Client.CLAN_CHANNEL
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun activeClanChannelKickUser(executor: CS2Executor) {
        val userSlot = executor.intStack[--executor.intStackPtr]
        PacketEncoder.kickPlayer(executor.clanChannel == Client.CLAN_CHANNEL, userSlot)
    }

    @JvmStatic
    fun activeClanSettingsGetAffinedCount(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.memberCount
    }

    @JvmStatic
    fun activeClanSettingsGetRankLootShare(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.lootshareRank.toInt()
    }

    @JvmStatic
    fun activeClanSettingsFindAffined(executor: CS2Executor) {
        if (Client.CLAN_SETTINGS != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
            executor.currentClanSettings = Client.CLAN_SETTINGS
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun stockMarketGetOfferCompletedCount(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[slot]!!.currentAmount
    }

    @JvmStatic
    fun stockMarketIsOfferFinished(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        val state = Client.GRAND_EXCHANGE_SLOTS[slot]!!.getSlotState()
        executor.intStack[++executor.intStackPtr - 1] = if (state == 5) 1 else 0
    }

    @JvmStatic
    fun activeClanchannelFindListened(executor: CS2Executor) {
        if (Client.LISTENED_CLAN_CHANNEL != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
            executor.clanChannel = Client.LISTENED_CLAN_CHANNEL
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun activeClanSettingsGetAffinedJoinRuneday(executor: CS2Executor) {
        val memberSlot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.memberJoinDays!![memberSlot]
    }

    @JvmStatic
    fun stockMarketGetOfferType(executor: CS2Executor) {
        val slot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[slot]!!.isBuyOffer()
    }

    @JvmStatic
    fun activeClanSettingsGetAffinedDisplayName(executor: CS2Executor) {
        val memberSlot = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings!!.memberNames!![memberSlot]
    }

    @JvmStatic
    fun activeClanChannelGetUserSlot(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.getUserSlot(executor.stringStack[--executor.stringStackPtr] as String)
    }

    @JvmStatic
    fun activeClanChannelGetRankTalk(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.guestsTalk.toInt()
    }

    @JvmStatic
    fun activeClanChannelGetClanName(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.clanChannel!!.clanName
    }

    @JvmStatic
    fun activeClanSettingsGetRankKick(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.kickRank.toInt()
    }

    @JvmStatic
    fun activeClanSettingsGetAffinedExtraInfo(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val memberSlot = executor.intStack[executor.intStackPtr]
        val startBit = executor.intStack[executor.intStackPtr + 1]
        val endBit = executor.intStack[executor.intStackPtr + 2]
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.getClanFlagBitfield(memberSlot, startBit, endBit)
    }

    @JvmStatic
    fun unusedClanOp(executor: CS2Executor) {
        PacketEncoder.sendClanOperation(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun activeClanChannelGetUserRank(executor: CS2Executor) {
        val userSlot = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.players!![userSlot]!!.rank.toInt()
    }

    @JvmStatic
    fun activeClanSettingsGetBannedCount(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.banCount
    }

    @JvmStatic
    fun activeClanChannelGetUserDisplayName(executor: CS2Executor) {
        val userSlot = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = executor.clanChannel!!.players!![userSlot]!!.name
    }

    @JvmStatic
    fun activeClanSettingsFindListened(executor: CS2Executor) {
        if (Client.GUEST_CLAN_SETTINGS != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
            executor.currentClanSettings = Client.GUEST_CLAN_SETTINGS
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun activeClanSettingsGetAllowUnaffined(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (executor.currentClanSettings!!.allowGuests) 1  else 0
    }

    @JvmStatic
    fun clanProfileFind(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (ClanVarTypeList.CLAN_VARS != null) 1 else 0
    }

    @JvmStatic
    fun activeClanSettingsGetCoinShare(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.coinShare.toInt()
    }

}
