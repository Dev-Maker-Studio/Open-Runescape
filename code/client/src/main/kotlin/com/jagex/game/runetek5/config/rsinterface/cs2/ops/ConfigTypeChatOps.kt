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
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2QueryResults
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode

object ConfigTypeChatOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.OC_NAME -> ocName(exec)
            ClientScriptOpCode.OC_OP -> ocOp(exec)
            ClientScriptOpCode.OC_IOP -> ocIop(exec)
            ClientScriptOpCode.OC_COST -> ocCost(exec)
            ClientScriptOpCode.OC_STACKABLE -> ocStackable(exec)
            ClientScriptOpCode.OC_CERT -> ocCert(exec)
            ClientScriptOpCode.OC_UNCERT -> ocUncert(exec)
            ClientScriptOpCode.OC_WEARPOS -> ocWearpos(exec)
            ClientScriptOpCode.OC_WEARPOS2 -> ocWearpos2(exec)
            ClientScriptOpCode.OC_WEARPOS3 -> ocWearpos3(exec)
            ClientScriptOpCode.OC_MEMBERS -> ocMembers(exec)
            ClientScriptOpCode.OC_PARAM -> getItemParam(exec)
            ClientScriptOpCode.OC_ICURSOR -> ocIcursor(exec)
            ClientScriptOpCode.OC_FIND -> ocFind(exec)
            ClientScriptOpCode.OC_FINDNEXT -> ocFindnext(exec)
            ClientScriptOpCode.OC_FINDRESTART -> ocFindrestart()
            ClientScriptOpCode.OC_MULTISTACKSIZE -> ocMultiStackSize(exec)
            ClientScriptOpCode.ITEM_FIND_PARAMINT -> itemFindParamInt(exec)
            ClientScriptOpCode.ITEM_FIND_PARAMSTR -> itemFindParamStr(exec)
            ClientScriptOpCode.OC_MINIMENU_COLOUR_OVERRIDDEN -> ocMiniMenuColourOverridden(exec)
            ClientScriptOpCode.OC_MINIMENU_COLOUR -> ocMiniMenuColour(exec)
            ClientScriptOpCode.NC_PARAM -> ncParam(exec)
            ClientScriptOpCode.OBJECT_PARAM -> getObjectParam(exec)
            ClientScriptOpCode.STRUCT_PARAM -> getStructParam(exec)
            ClientScriptOpCode.SEQ_PARAM -> getAnimationParam(exec)
            ClientScriptOpCode.BAS_GETANIM_READY -> basGetAnimReady(exec)
            ClientScriptOpCode.UNUSED_LOGIN_GLOBAL_BOOL -> unusedLoginGlobalBool(exec)
            ClientScriptOpCode.UNUSED_PACKET_SEND_STRING -> sendDisplayNameChangeRequest(exec)
            ClientScriptOpCode.UNUSED_PACKET_SET_GLOBAL_BYTE -> unusedPacketSetGlobalByte(exec)
            ClientScriptOpCode.CHAT_GETFILTER_PUBLIC -> setPublicFilter(exec)
            ClientScriptOpCode.CHAT_SETFILTER -> ChatBox.setFilter(exec)
            ClientScriptOpCode.CHAT_SENDABUSEREPORT -> sendReportAbusePacket(exec)
            ClientScriptOpCode.CHAT_GETHISTORYMESSAGE -> chatGetHistoryMessage(exec)
            ClientScriptOpCode.CHAT_GETHISTORYTYPE -> chatGetHistoryType(exec)
            ClientScriptOpCode.CHAT_GETFILTER_PRIVATE -> setPrivateFilter(exec)
            ClientScriptOpCode.CHAT_SETMODE -> chatSetMode(exec)
            ClientScriptOpCode.CHAT_SENDPUBLIC -> chatSendPublic(exec)
            ClientScriptOpCode.CHAT_SENDPRIVATE -> chatSendPrivate(exec)
            ClientScriptOpCode.CHAT_GETHISTORYNAME -> chatGetHistoryName(exec)
            ClientScriptOpCode.CHAT_GETHISTORYCLAN -> chatGetHistoryClan(exec)
            ClientScriptOpCode.CHAT_GETHISTORYPHRASE -> chatGetHistoryPhrase(exec)
            ClientScriptOpCode.CHAT_PLAYERNAME_UNFILTERED -> chatPlayerNameUnfiltered(exec)
            ClientScriptOpCode.CHAT_GETFILTER_TRADE -> setTradeFilter(exec)
            ClientScriptOpCode.CHAT_GETHISTORYLENGTH -> chatGetHistoryLength(exec)
            ClientScriptOpCode.CHATLINE_GETEFFECTFLAGS -> chatlineGeteffectflags(exec)
            ClientScriptOpCode.UNKNOWN_COMMAND_5019 -> unknownCommand5019(exec)
            ClientScriptOpCode.CHAT_PLAYERNAME -> chatPlayerName(exec)
            ClientScriptOpCode.CHATLINE_GETUID -> chatlineGetUid(exec)
            ClientScriptOpCode.CHAT_GETPREVUID -> chatGetPrevUid(exec)
            ClientScriptOpCode.CHATLINE_GETSIMPLENAME -> chatlineGetSimpleName(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by ConfigTypeChatOps")
        }
    }

    @JvmStatic
    fun unusedLoginGlobalBool(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.hasDisplayNamePending) 1  else 0
    }

    @JvmStatic
    fun chatGetHistoryType(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var type = -1
        if (chatLine != null) {
            type = chatLine!!.type!!.getValue()
        }
        executor.intStack[++executor.intStackPtr - 1] = type
    }

    @JvmStatic
    fun ocFindnext(executor: CS2Executor) {
        if (CS2QueryResults.hasNext()) {
            executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.next()
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun chatPlayerNameUnfiltered(executor: CS2Executor) {
        var name: String? = null
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER!!.username != null) {
            name = PlayerUpdate.MY_PLAYER!!.getUsernameWithTitle()
        } else {
            name = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = name
    }

    @JvmStatic
    fun chatGetHistoryPhrase(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var quickchatId = -1
        if (chatLine != null) {
            quickchatId = chatLine.quickchatMessageId
        }
        executor.intStack[++executor.intStackPtr - 1] = quickchatId
    }

    @JvmStatic
    fun ncParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val npcId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (paramType!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.NPC!!.list(npcId).getCS2Param(paramId, paramType!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.NPC!!.list(npcId).getCs2Value(paramId, paramType!!.defaultInt)
        }
    }

    @JvmStatic
    fun chatGetHistoryLength(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.getLineCount()
    }

    @JvmStatic
    fun getAnimationParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val seqId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (paramType!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.SEQ!!.list(seqId).getCs2Object(paramId, paramType!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.SEQ!!.list(seqId).getCs2Integer(paramId, paramType!!.defaultInt)
        }
    }

    @JvmStatic
    fun ocUncert(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        val objType: ItemType? = TypeLists.OBJ!!.list(objId)
        if (objType!!.certTemplateId >= 0 && objType!!.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = objType!!.certId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = objId
        }
    }

    @JvmStatic
    fun ocCert(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        val objType: ItemType? = TypeLists.OBJ!!.list(objId)
        if (objType!!.certTemplateId == -1 && objType!!.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = objType!!.certId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = objId
        }
    }

    @JvmStatic
    fun ocCost(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(objId).value
    }

    @JvmStatic
    fun chatGetPrevUid(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var cycles = -1
        if (chatLine != null) {
            cycles = chatLine.time
        }
        executor.intStack[++executor.intStackPtr - 1] = cycles
    }

    @JvmStatic
    fun itemFindParamInt(executor: CS2Executor) {
        val itemName: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 3
        val tradeableOnly = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramIntQuery = executor.intStack[executor.intStackPtr + 2]
        StaticMethods.queryItemParamInt(itemName!!, tradeableOnly == 1, paramId, paramIntQuery)
        executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.count
    }

    @JvmStatic
    fun ocFind(executor: CS2Executor) {
        val itemName: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val tradeableOnly = executor.intStack[--executor.intStackPtr]
        StaticMethods.queryItemNoParams(itemName!!, tradeableOnly == 1)
        executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.count
    }

    @JvmStatic
    fun getObjectParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val locId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (paramType!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = Resource.mapRegion!!.getLoctypeList().list(locId).getParamsString(paramId, paramType!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Resource.mapRegion!!.getLoctypeList().list(locId).getParamsInt(paramId, paramType!!.defaultInt)
        }
    }

    @JvmStatic
    fun setPublicFilter(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.PUBLIC_FILTER
    }

    @JvmStatic
    fun ocName(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ!!.list(objId).name
    }

    @JvmStatic
    fun sendDisplayNameChangeRequest(executor: CS2Executor) {
        val displayName: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress()) {
            if (displayName!!.length > 20) {
                Client.displayNameChangeState = -4
            } else {
                Client.displayNameChangeState = -1
                val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.DEPRECATED_97_SERVERPACKET_52, Client.LOBBY_CONNECTION_CONTEXT.isaac)
                packet!!.buffer.writeByte(0)
                val position = packet!!.buffer.currentPosition
                packet!!.buffer.writeString(displayName!!)
                packet!!.buffer.writeIndex(packet!!.buffer.currentPosition - position)
                Client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet)
            }
        } else {
            Client.displayNameChangeState = -5
        }
    }

    @JvmStatic
    fun ocStackable(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        val objType = TypeLists.OBJ!!.list(objId)
        executor.intStack[++executor.intStackPtr - 1] = if (objType.stackable == 1) 1 else 0
    }

    @JvmStatic
    fun ocIop(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val objId = executor.intStack[executor.intStackPtr]
        val op = executor.intStack[executor.intStackPtr + 1]
        val objType: ItemType? = TypeLists.OBJ!!.list(objId)
        if (op >= 1 && op <= 5 && objType!!.inventoryActions!![op - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = objType!!.inventoryActions!![op - 1]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun chatPlayerName(executor: CS2Executor) {
        var displayName: String? = null
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER!!.username != null) {
            displayName = PlayerUpdate.MY_PLAYER!!.getDisplayName()
        } else {
            displayName = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = displayName
    }

    @JvmStatic
    fun chatlineGetUid(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var uid = -1
        if (chatLine != null) {
            uid = chatLine.identifier
        }
        executor.intStack[++executor.intStackPtr - 1] = uid
    }

    @JvmStatic
    fun itemFindParamStr(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStackPtr -= 2
        val itemName: String? = executor.stringStack[executor.stringStackPtr] as String?
        val tradeableOnly = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramString: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        CS2Executor.queryItemParamString(itemName!!, tradeableOnly == 1, paramId, paramString!!)
        executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.count
    }

    @JvmStatic
    fun ocWearpos2(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(objId).wearPos2
    }

    @JvmStatic
    fun ocMembers(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (TypeLists.OBJ!!.list(objId).membersOnly) 1  else 0
    }

    @JvmStatic
    fun chatlineGetSimpleName(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var simpleName: String? = ""
        if (chatLine != null && chatLine.simpleName != null) {
            simpleName = chatLine.simpleName
        }
        executor.stringStack[++executor.stringStackPtr - 1] = simpleName
    }

    @JvmStatic
    fun chatGetHistoryMessage(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var message: String? = ""
        if (chatLine != null && chatLine.message != null) {
            message = chatLine.message
        }
        executor.stringStack[++executor.stringStackPtr - 1] = message
    }

    @JvmStatic
    fun getItemParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val objId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (paramType!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ!!.list(objId).getCs2String(paramId, paramType!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(objId).getCs2Integer(paramId, paramType!!.defaultInt)
        }
    }

    @JvmStatic
    fun setPrivateFilter(executor: CS2Executor) {
        if (ChatBox.PRIVATE_FILTER == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = ChatBox.PRIVATE_FILTER!!.id
        }
    }

    @JvmStatic
    fun ocMultiStackSize(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(objId).multiStackSize
    }

    @JvmStatic
    fun unusedPacketSetGlobalByte(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.displayNameChangeState.toInt()
        if (Client.displayNameChangeState.toInt() != -1) {
            Client.displayNameChangeState = -6
        }
    }

    @JvmStatic
    fun chatlineGeteffectflags(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var effectFlags = 0
        if (chatLine != null) {
            effectFlags = chatLine.effectFlags
        }
        executor.intStack[++executor.intStackPtr - 1] = effectFlags
    }

    @JvmStatic
    fun getStructParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val structId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (paramType!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.STRUCT!!.getStruct(structId).getString(paramId, paramType!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.STRUCT!!.getStruct(structId).getInteger(paramId, paramType!!.defaultInt)
        }
    }

    @JvmStatic
    fun chatGetHistoryClan(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var clan: String? = ""
        if (chatLine != null && chatLine.clan != null) {
            clan = chatLine.clan
        }
        executor.stringStack[++executor.stringStackPtr - 1] = clan
    }

    @JvmStatic
    fun chatGetHistoryName(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var crownedName: String? = ""
        if (chatLine != null && chatLine.crownedName != null) {
            crownedName = chatLine.crownedName
        }
        executor.stringStack[++executor.stringStackPtr - 1] = crownedName
    }

    @JvmStatic
    fun ocWearpos(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(objId).wearPos
    }

    @JvmStatic
    fun chatSetMode(executor: CS2Executor) {
        val mode = executor.intStack[--executor.intStackPtr]
        val connection: BufferedConnectionContext? = Client.connectionContext
        val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.CHAT_TYPE, connection?.isaac)
        packet!!.buffer.writeByte(mode)
        connection!!.queuePacket(packet)
    }

    @JvmStatic
    fun sendReportAbusePacket(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val reportedName: String? = executor.stringStack[executor.stringStackPtr] as String?
        var comment: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        executor.intStackPtr -= 2
        val ruleId = executor.intStack[executor.intStackPtr]
        val mute = executor.intStack[executor.intStackPtr + 1]
        if (comment == null) {
            comment = ""
        }
        if (comment!!.length > 80) {
            comment = comment!!.substring(0, 80)
        }
        val connection: BufferedConnectionContext? = Client.connectionContext
        val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.REPORT_ABUSE, connection!!.isaac)
        packet!!.buffer.writeByte(TextUtils.getLength(reportedName!!) + 2 + TextUtils.getLength(comment!!))
        packet!!.buffer.writeString(reportedName!!)
        packet!!.buffer.writeByte(ruleId - 1)
        packet!!.buffer.writeByte(mute)
        packet!!.buffer.writeString(comment!!)
        connection!!.queuePacket(packet)
    }

    @JvmStatic
    fun ocOp(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val objId = executor.intStack[executor.intStackPtr]
        val op = executor.intStack[executor.intStackPtr + 1]
        val objType: ItemType? = TypeLists.OBJ!!.list(objId)
        if (op >= 1 && op <= 5 && objType!!.groundActions!![op - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = objType!!.groundActions!![op - 1]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ocWearpos3(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(objId).wearPos3
    }

    @JvmStatic
    fun ocMiniMenuColourOverridden(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        val objType: ItemType? = TypeLists.OBJ!!.list(objId)
        executor.intStack[++executor.intStackPtr - 1] = if (objType!!.hasTooltipColor) 1  else 0
    }

    @JvmStatic
    fun ocIcursor(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val objId = executor.intStack[executor.intStackPtr]
        val op = executor.intStack[executor.intStackPtr + 1] - 1
        val objType: ItemType? = TypeLists.OBJ!!.list(objId)
        if (op == objType!!.customCursorOp1) {
            executor.intStack[++executor.intStackPtr - 1] = objType!!.customCursorId1
        } else if (op == objType!!.customCursorOp2) {
            executor.intStack[++executor.intStackPtr - 1] = objType!!.customCursorId2
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun ocFindrestart() {
        CS2QueryResults.cursor = 0
    }

    @JvmStatic
    fun unknownCommand5019(executor: CS2Executor) {
        val index = executor.intStack[--executor.intStackPtr]
        val chatLine: ChatLine? = ChatBox.getLine(index)
        var name: String? = ""
        if (chatLine != null && chatLine.name != null) {
            name = chatLine.name
        }
        executor.stringStack[++executor.stringStackPtr - 1] = name
    }

    @JvmStatic
    fun chatSendPrivate(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val recipient: String? = executor.stringStack[executor.stringStackPtr] as String?
        val message: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (Client.PLAYER_RIGHTS != 0 || (!Client.USERDETAIL_QUICKCHAT || Client.VERIFIED_EMAIL_ADDRESS) && !Client.IS_QUICKCHAT_ONLY) {
            val connection: BufferedConnectionContext? = Client.connectionContext
            val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.SEND_PRIVATE_MESSAGE, connection!!.isaac)
            packet!!.buffer.writeShort(0)
            val position = packet!!.buffer.currentPosition
            packet!!.buffer.writeString(recipient!!)
            PacketEncoder.chatMessageToBuffer(packet!!.buffer, message)
            packet!!.buffer.writeShortLength(packet!!.buffer.currentPosition - position)
            connection!!.queuePacket(packet)
        }
    }

    @JvmStatic
    fun ocMiniMenuColour(executor: CS2Executor) {
        val objId = executor.intStack[--executor.intStackPtr]
        val objType: ItemType? = TypeLists.OBJ!!.list(objId)
        var color: Int = 0
        if (objType!!.hasTooltipColor) {
            color = objType!!.tooltipColor
        } else if (objType!!.membersOnly) {
            color = TypeLists.INTERACTION_DEFAULTS!!.tooltipColorP2p
        } else {
            color = TypeLists.INTERACTION_DEFAULTS!!.tooltipColorF2p
        }
        executor.intStack[++executor.intStackPtr - 1] = color
    }

    @JvmStatic
    fun setTradeFilter(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.TRADE_FILTER
    }

    @JvmStatic
    fun chatSendPublic(executor: CS2Executor) {
        var message: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (com.darkan.bot.DisplayToggle.intercept(message)) return
        if (Client.PLAYER_RIGHTS != 0 || (!Client.USERDETAIL_QUICKCHAT || Client.VERIFIED_EMAIL_ADDRESS) && !Client.IS_QUICKCHAT_ONLY) {
            var lower: String? = message!!.lowercase()
            var color = 0
            if (lower!!.startsWith(LocalizedText.YELLOW.translate(Language.ENGLISH))) {
                message = message!!.substring(LocalizedText.YELLOW.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.RED.translate(Language.ENGLISH))) {
                color = 1
                message = message!!.substring(LocalizedText.RED.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.GREEN.translate(Language.ENGLISH))) {
                color = 2
                message = message!!.substring(LocalizedText.GREEN.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.CYAN.translate(Language.ENGLISH))) {
                color = 3
                message = message!!.substring(LocalizedText.CYAN.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.PURPLE.translate(Language.ENGLISH))) {
                color = 4
                message = message!!.substring(LocalizedText.PURPLE.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.WHITE.translate(Language.ENGLISH))) {
                color = 5
                message = message!!.substring(LocalizedText.WHITE.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.FLASH1.translate(Language.ENGLISH))) {
                color = 6
                message = message!!.substring(LocalizedText.FLASH1.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.FLASH2.translate(Language.ENGLISH))) {
                color = 7
                message = message!!.substring(LocalizedText.FLASH2.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.FLASH3.translate(Language.ENGLISH))) {
                color = 8
                message = message!!.substring(LocalizedText.FLASH3.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.GLOW1.translate(Language.ENGLISH))) {
                color = 9
                message = message!!.substring(LocalizedText.GLOW1.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.GLOW2.translate(Language.ENGLISH))) {
                color = 10
                message = message!!.substring(LocalizedText.GLOW2.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.GLOW3.translate(Language.ENGLISH))) {
                color = 11
                message = message!!.substring(LocalizedText.GLOW3.translate(Language.ENGLISH).length)
            } else if (Client.CURRENT_LANGUAGE!! != Language.ENGLISH) {
                if (lower!!.startsWith(LocalizedText.YELLOW.translate(Client.CURRENT_LANGUAGE!!))) {
                    message = message!!.substring(LocalizedText.YELLOW.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.RED.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 1
                    message = message!!.substring(LocalizedText.RED.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.GREEN.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 2
                    message = message!!.substring(LocalizedText.GREEN.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.CYAN.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 3
                    message = message!!.substring(LocalizedText.CYAN.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.PURPLE.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 4
                    message = message!!.substring(LocalizedText.PURPLE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.WHITE.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 5
                    message = message!!.substring(LocalizedText.WHITE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.FLASH1.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 6
                    message = message!!.substring(LocalizedText.FLASH1.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.FLASH2.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 7
                    message = message!!.substring(LocalizedText.FLASH2.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.FLASH3.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 8
                    message = message!!.substring(LocalizedText.FLASH3.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.GLOW1.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 9
                    message = message!!.substring(LocalizedText.GLOW1.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.GLOW2.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 10
                    message = message!!.substring(LocalizedText.GLOW2.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.GLOW3.translate(Client.CURRENT_LANGUAGE!!))) {
                    color = 11
                    message = message!!.substring(LocalizedText.GLOW3.translate(Client.CURRENT_LANGUAGE!!).length)
                }
            }
            lower = message!!.lowercase()
            var effect = 0
            if (lower!!.startsWith(LocalizedText.WAVE.translate(Language.ENGLISH))) {
                effect = 1
                message = message!!.substring(LocalizedText.WAVE.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.WAVE2.translate(Language.ENGLISH))) {
                effect = 2
                message = message!!.substring(LocalizedText.WAVE2.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.SHAKE.translate(Language.ENGLISH))) {
                effect = 3
                message = message!!.substring(LocalizedText.SHAKE.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.SCROLL.translate(Language.ENGLISH))) {
                effect = 4
                message = message!!.substring(LocalizedText.SCROLL.translate(Language.ENGLISH).length)
            } else if (lower!!.startsWith(LocalizedText.SLIDE.translate(Language.ENGLISH))) {
                effect = 5
                message = message!!.substring(LocalizedText.SLIDE.translate(Language.ENGLISH).length)
            } else if (Client.CURRENT_LANGUAGE!! != Language.ENGLISH) {
                if (lower!!.startsWith(LocalizedText.WAVE.translate(Client.CURRENT_LANGUAGE!!))) {
                    effect = 1
                    message = message!!.substring(LocalizedText.WAVE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.WAVE2.translate(Client.CURRENT_LANGUAGE!!))) {
                    effect = 2
                    message = message!!.substring(LocalizedText.WAVE2.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.SHAKE.translate(Client.CURRENT_LANGUAGE!!))) {
                    effect = 3
                    message = message!!.substring(LocalizedText.SHAKE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.SCROLL.translate(Client.CURRENT_LANGUAGE!!))) {
                    effect = 4
                    message = message!!.substring(LocalizedText.SCROLL.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (lower!!.startsWith(LocalizedText.SLIDE.translate(Client.CURRENT_LANGUAGE!!))) {
                    effect = 5
                    message = message!!.substring(LocalizedText.SLIDE.translate(Client.CURRENT_LANGUAGE!!).length)
                }
            }
            val connection: BufferedConnectionContext? = Client.connectionContext
            val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.CHAT, connection!!.isaac)
            packet!!.buffer.writeByte(0)
            val position = packet!!.buffer.currentPosition
            packet!!.buffer.writeByte(color)
            packet!!.buffer.writeByte(effect)
            PacketEncoder.chatMessageToBuffer(packet!!.buffer, message)
            packet!!.buffer.writeIndex(packet!!.buffer.currentPosition - position)
            connection!!.queuePacket(packet)
        }
    }

    @JvmStatic
    fun basGetAnimReady(executor: CS2Executor) {
        val basId = executor.intStack[--executor.intStackPtr]
        val bas: BasType? = TypeLists.BAS!!.list(basId)
        if (bas!!.randomStandSequences != null && bas!!.randomStandSequences!!.size > 0) {
            var bestIndex = 0
            var bestChance = bas!!.randomStandSequenceChances!![0]
            for (i in 1 until bas!!.randomStandSequences!!.size) {
                if (bas!!.randomStandSequenceChances!![i] > bestChance) {
                    bestIndex = i
                    bestChance = bas!!.randomStandSequenceChances!![i]
                }
            }
            executor.intStack[++executor.intStackPtr - 1] = bas!!.randomStandSequences!![bestIndex]
        } else {
            executor.intStack[++executor.intStackPtr - 1] = bas!!.standSequenceId
        }
    }

}
