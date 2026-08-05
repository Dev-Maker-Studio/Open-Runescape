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
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1
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
            ClientScriptOpCode.NPC_PARAM -> getNPCParam(exec)
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
            ClientScriptOpCode.CHAT_GETBYLINE -> chatGetByline(exec)
            ClientScriptOpCode.CHAT_GETTYPEBYLINE -> chatGetTypeByline(exec)
            ClientScriptOpCode.CHAT_GETFILTER_PRIVATE -> setPrivateFilter(exec)
            ClientScriptOpCode.CHAT_SETMODE -> chatSetMode(exec)
            ClientScriptOpCode.CHAT_SENDPUBLIC -> chatSendPublic(exec)
            ClientScriptOpCode.CHAT_SENDPRIVATE -> chatSendPrivate(exec)
            ClientScriptOpCode.CHATLINE_GETCROWNEDNAME -> method6237(exec)
            ClientScriptOpCode.CHAT_GETCLAN -> chatGetClan(exec)
            ClientScriptOpCode.CHATLINE_GETQUICKCHATID -> chatlineGetQuickchatId(exec)
            ClientScriptOpCode.CHAT_PLAYERNAME -> chatPlayerName(exec)
            ClientScriptOpCode.CHAT_GETFILTER_TRADE -> setTradeFilter(exec)
            ClientScriptOpCode.CHAT_GETHISTORYLENGTH -> chatGetHistoryLength(exec)
            ClientScriptOpCode.CHATLINE_GETEFFECTFLAGS -> chatlineGeteffectflags(exec)
            ClientScriptOpCode.CHATLINE_GETNAME -> chatlineGetName(exec)
            ClientScriptOpCode.GETMYDISPLAYNAME -> getMyDisplayname(exec)
            ClientScriptOpCode.CHATLINE_GETUID -> chatlineGetUid(exec)
            ClientScriptOpCode.CHATLINE_GETCYCLES20MS -> chatLineGetCycles20Ms(exec)
            ClientScriptOpCode.CHATLINE_GETSIMPLENAME -> method565(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by ConfigTypeChatOps")
        }
    }

    @JvmStatic
    fun unusedLoginGlobalBool(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.hasDisplayNamePending) 1  else 0
    }

    @JvmStatic
    fun chatGetTypeByline(executor: CS2Executor) {
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
    fun chatPlayerName(executor: CS2Executor) {
        var text: String? = null
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER!!.username != null) {
            text = PlayerUpdate.MY_PLAYER!!.getUsernameWithTitle()
        } else {
            text = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = text
    }

    @JvmStatic
    fun chatlineGetQuickchatId(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(i_2)
        var i_4 = -1
        if (chatline3 != null) {
            i_4 = chatline3.quickchatMessageId
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun getNPCParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.NPC!!.list(i_2).getCS2Param(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.NPC!!.list(i_2).getCs2Value(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun chatGetHistoryLength(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.getLineCount()
    }

    @JvmStatic
    fun getAnimationParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.SEQ!!.list(i_2).getCs2Object(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.SEQ!!.list(i_2).getCs2Integer(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun ocUncert(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(value)
        if (itemdefinitions3!!.certTemplateId >= 0 && itemdefinitions3!!.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions3!!.certId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = value
        }
    }

    @JvmStatic
    fun ocCert(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(value)
        if (itemdefinitions3!!.certTemplateId == -1 && itemdefinitions3!!.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions3!!.certId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = value
        }
    }

    @JvmStatic
    fun ocCost(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).value
    }

    @JvmStatic
    fun chatLineGetCycles20Ms(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(i_2)
        var i_4 = -1
        if (chatline3 != null) {
            i_4 = chatline3.time
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun itemFindParamInt(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 3
        val i_3 = executor.intStack[executor.intStackPtr]
        val i_4 = executor.intStack[executor.intStackPtr + 1]
        val i_5 = executor.intStack[executor.intStackPtr + 2]
        StaticMethods.queryItemParamInt(text!!, i_3 == 1, i_4, i_5)
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = Resource.mapRegion!!.getLoctypeList().list(i_2).getParamsString(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Resource.mapRegion!!.getLoctypeList().list(i_2).getParamsInt(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun setPublicFilter(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.PUBLIC_FILTER
    }

    @JvmStatic
    fun ocName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ!!.list(value).name
    }

    @JvmStatic
    fun sendDisplayNameChangeRequest(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress()) {
            if (text!!.length > 20) {
                Client.displayNameChangeState = -4
            } else {
                Client.displayNameChangeState = -1
                val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.DEPRECATED_97_SERVERPACKET_52, Client.LOBBY_CONNECTION_CONTEXT.isaac)
                tcpmessage3!!.buffer.writeByte(0)
                val value = tcpmessage3!!.buffer.currentPosition
                tcpmessage3!!.buffer.writeString(text!!)
                tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - value)
                Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage3)
            }
        } else {
            Client.displayNameChangeState = -5
        }
    }

    @JvmStatic
    fun ocStackable(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val objType = TypeLists.OBJ!!.list(value)
        executor.intStack[++executor.intStackPtr - 1] = if (objType.stackable == 1) 1 else 0
    }

    @JvmStatic
    fun ocIop(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val itemdefinitions4: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (i_3 >= 1 && i_3 <= 5 && itemdefinitions4!!.inventoryActions!![i_3 - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = itemdefinitions4!!.inventoryActions!![i_3 - 1]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun getMyDisplayname(executor: CS2Executor) {
        var text: String? = null
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER!!.username != null) {
            text = PlayerUpdate.MY_PLAYER!!.getDisplayName()
        } else {
            text = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = text
    }

    @JvmStatic
    fun chatlineGetUid(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(i_2)
        var i_4 = -1
        if (chatline3 != null) {
            i_4 = chatline3.identifier
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun itemFindParamStr(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStackPtr -= 2
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val i_3 = executor.intStack[executor.intStackPtr]
        val i_4 = executor.intStack[executor.intStackPtr + 1]
        val string_5: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        CS2Executor.queryItemParamString(string_2!!, i_3 == 1, i_4, string_5!!)
        executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.count
    }

    @JvmStatic
    fun ocWearpos2(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(value).wearPos2
    }

    @JvmStatic
    fun ocMembers(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (TypeLists.OBJ!!.list(value).membersOnly) 1  else 0
    }

    @JvmStatic
    fun method565(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(value)
        var str4: String? = ""
        if (chatline3 != null && chatline3.simpleName != null) {
            str4 = chatline3.simpleName
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun chatGetByline(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(value)
        var str4: String? = ""
        if (chatline3 != null && chatline3.message != null) {
            str4 = chatline3.message
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun getItemParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ!!.list(i_2).getCs2String(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).getCs2Integer(i_3, attributedefault4!!.defaultInt)
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
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(value).multiStackSize
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
        val line = executor.intStack[--executor.intStackPtr]
        val chatline: ChatLine? = ChatBox.getLine(line)
        var effectFlags = 0
        if (chatline != null) {
            effectFlags = chatline.effectFlags
        }
        executor.intStack[++executor.intStackPtr - 1] = effectFlags
    }

    @JvmStatic
    fun getStructParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.STRUCT!!.getStruct(i_2).getString(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.STRUCT!!.getStruct(i_2).getInteger(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun chatGetClan(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(value)
        var str4: String? = ""
        if (chatline3 != null && chatline3.clan != null) {
            str4 = chatline3.clan
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun method6237(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(value)
        var str4: String? = ""
        if (chatline3 != null && chatline3.crownedName != null) {
            str4 = chatline3.crownedName
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun ocWearpos(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(value).wearPos
    }

    @JvmStatic
    fun chatSetMode(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class1843: BufferedConnectionContext? = Client.connectionContext
        val tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.CHAT_TYPE, class1843?.isaac)
        tcpmessage4!!.buffer.writeByte(value)
        class1843!!.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun sendReportAbusePacket(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        executor.intStackPtr -= 2
        val i_4 = executor.intStack[executor.intStackPtr]
        val i_5 = executor.intStack[executor.intStackPtr + 1]
        if (string_3 == null) {
            string_3 = ""
        }
        if (string_3!!.length > 80) {
            string_3 = string_3!!.substring(0, 80)
        }
        val class1846: BufferedConnectionContext? = Client.connectionContext
        val tcpmessage7: TcpPacket? = PacketEncoder.createPacket(ClientProt.REPORT_ABUSE, class1846!!.isaac)
        tcpmessage7!!.buffer.writeByte(TextUtils.getLength(string_2!!) + 2 + TextUtils.getLength(string_3!!))
        tcpmessage7!!.buffer.writeString(string_2!!)
        tcpmessage7!!.buffer.writeByte(i_4 - 1)
        tcpmessage7!!.buffer.writeByte(i_5)
        tcpmessage7!!.buffer.writeString(string_3!!)
        class1846!!.queuePacket(tcpmessage7)
    }

    @JvmStatic
    fun ocOp(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val itemdefinitions4: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (i_3 >= 1 && i_3 <= 5 && itemdefinitions4!!.groundActions!![i_3 - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = itemdefinitions4!!.groundActions!![i_3 - 1]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ocWearpos3(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(value).wearPos3
    }

    @JvmStatic
    fun ocMiniMenuColourOverridden(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(value)
        executor.intStack[++executor.intStackPtr - 1] = if (itemdefinitions3!!.hasTooltipColor) 1  else 0
    }

    @JvmStatic
    fun ocIcursor(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1] - 1
        val itemdefinitions4: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (i_3 == itemdefinitions4!!.customCursorOp1) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions4!!.customCursorId1
        } else if (i_3 == itemdefinitions4!!.customCursorOp2) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions4!!.customCursorId2
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun ocFindrestart() {
        CS2QueryResults.cursor = 0
    }

    @JvmStatic
    fun chatlineGetName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val chatline3: ChatLine? = ChatBox.getLine(value)
        var str4: String? = ""
        if (chatline3 != null && chatline3.name != null) {
            str4 = chatline3.name
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun chatSendPrivate(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (Client.PLAYER_RIGHTS != 0 || (!Client.USERDETAIL_QUICKCHAT || Client.VERIFIED_EMAIL_ADDRESS) && !Client.IS_QUICKCHAT_ONLY) {
            val class1844: BufferedConnectionContext? = Client.connectionContext
            val tcpmessage5: TcpPacket? = PacketEncoder.createPacket(ClientProt.SEND_PRIVATE_MESSAGE, class1844!!.isaac)
            tcpmessage5!!.buffer.writeShort(0)
            val value = tcpmessage5!!.buffer.currentPosition
            tcpmessage5!!.buffer.writeString(string_2!!)
            PacketEncoder.chatMessageToBuffer(tcpmessage5!!.buffer, string_3)
            tcpmessage5!!.buffer.writeShortLength(tcpmessage5!!.buffer.currentPosition - value)
            class1844!!.queuePacket(tcpmessage5)
        }
    }

    @JvmStatic
    fun ocMiniMenuColour(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(i_2)
        var i_4: Int = 0
        if (itemdefinitions3!!.hasTooltipColor) {
            i_4 = itemdefinitions3!!.tooltipColor
        } else if (itemdefinitions3!!.membersOnly) {
            i_4 = TypeLists.INTERACTION_DEFAULTS!!.tooltipColorP2p
        } else {
            i_4 = TypeLists.INTERACTION_DEFAULTS!!.tooltipColorF2p
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun setTradeFilter(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.TRADE_FILTER
    }

    @JvmStatic
    fun chatSendPublic(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.PLAYER_RIGHTS != 0 || (!Client.USERDETAIL_QUICKCHAT || Client.VERIFIED_EMAIL_ADDRESS) && !Client.IS_QUICKCHAT_ONLY) {
            var string_3: String? = string_2!!.lowercase()
            var b_4 = 0
            if (string_3!!.startsWith(LocalizedText.YELLOW.translate(Language.ENGLISH))) {
                string_2 = string_2!!.substring(LocalizedText.YELLOW.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.RED.translate(Language.ENGLISH))) {
                b_4 = 1
                string_2 = string_2!!.substring(LocalizedText.RED.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.GREEN.translate(Language.ENGLISH))) {
                b_4 = 2
                string_2 = string_2!!.substring(LocalizedText.GREEN.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.CYAN.translate(Language.ENGLISH))) {
                b_4 = 3
                string_2 = string_2!!.substring(LocalizedText.CYAN.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.PURPLE.translate(Language.ENGLISH))) {
                b_4 = 4
                string_2 = string_2!!.substring(LocalizedText.PURPLE.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.WHITE.translate(Language.ENGLISH))) {
                b_4 = 5
                string_2 = string_2!!.substring(LocalizedText.WHITE.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.FLASH1.translate(Language.ENGLISH))) {
                b_4 = 6
                string_2 = string_2!!.substring(LocalizedText.FLASH1.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.FLASH2.translate(Language.ENGLISH))) {
                b_4 = 7
                string_2 = string_2!!.substring(LocalizedText.FLASH2.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.FLASH3.translate(Language.ENGLISH))) {
                b_4 = 8
                string_2 = string_2!!.substring(LocalizedText.FLASH3.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.GLOW1.translate(Language.ENGLISH))) {
                b_4 = 9
                string_2 = string_2!!.substring(LocalizedText.GLOW1.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.GLOW2.translate(Language.ENGLISH))) {
                b_4 = 10
                string_2 = string_2!!.substring(LocalizedText.GLOW2.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.GLOW3.translate(Language.ENGLISH))) {
                b_4 = 11
                string_2 = string_2!!.substring(LocalizedText.GLOW3.translate(Language.ENGLISH).length)
            } else if (Client.CURRENT_LANGUAGE!! != Language.ENGLISH) {
                if (string_3!!.startsWith(LocalizedText.YELLOW.translate(Client.CURRENT_LANGUAGE!!))) {
                    string_2 = string_2!!.substring(LocalizedText.YELLOW.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.RED.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 1
                    string_2 = string_2!!.substring(LocalizedText.RED.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.GREEN.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 2
                    string_2 = string_2!!.substring(LocalizedText.GREEN.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.CYAN.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 3
                    string_2 = string_2!!.substring(LocalizedText.CYAN.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.PURPLE.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 4
                    string_2 = string_2!!.substring(LocalizedText.PURPLE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.WHITE.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 5
                    string_2 = string_2!!.substring(LocalizedText.WHITE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.FLASH1.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 6
                    string_2 = string_2!!.substring(LocalizedText.FLASH1.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.FLASH2.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 7
                    string_2 = string_2!!.substring(LocalizedText.FLASH2.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.FLASH3.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 8
                    string_2 = string_2!!.substring(LocalizedText.FLASH3.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.GLOW1.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 9
                    string_2 = string_2!!.substring(LocalizedText.GLOW1.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.GLOW2.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 10
                    string_2 = string_2!!.substring(LocalizedText.GLOW2.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.GLOW3.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_4 = 11
                    string_2 = string_2!!.substring(LocalizedText.GLOW3.translate(Client.CURRENT_LANGUAGE!!).length)
                }
            }
            string_3 = string_2!!.lowercase()
            var b_5 = 0
            if (string_3!!.startsWith(LocalizedText.WAVE.translate(Language.ENGLISH))) {
                b_5 = 1
                string_2 = string_2!!.substring(LocalizedText.WAVE.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.WAVE2.translate(Language.ENGLISH))) {
                b_5 = 2
                string_2 = string_2!!.substring(LocalizedText.WAVE2.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.SHAKE.translate(Language.ENGLISH))) {
                b_5 = 3
                string_2 = string_2!!.substring(LocalizedText.SHAKE.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.SCROLL.translate(Language.ENGLISH))) {
                b_5 = 4
                string_2 = string_2!!.substring(LocalizedText.SCROLL.translate(Language.ENGLISH).length)
            } else if (string_3!!.startsWith(LocalizedText.SLIDE.translate(Language.ENGLISH))) {
                b_5 = 5
                string_2 = string_2!!.substring(LocalizedText.SLIDE.translate(Language.ENGLISH).length)
            } else if (Client.CURRENT_LANGUAGE!! != Language.ENGLISH) {
                if (string_3!!.startsWith(LocalizedText.WAVE.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_5 = 1
                    string_2 = string_2!!.substring(LocalizedText.WAVE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.WAVE2.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_5 = 2
                    string_2 = string_2!!.substring(LocalizedText.WAVE2.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.SHAKE.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_5 = 3
                    string_2 = string_2!!.substring(LocalizedText.SHAKE.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.SCROLL.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_5 = 4
                    string_2 = string_2!!.substring(LocalizedText.SCROLL.translate(Client.CURRENT_LANGUAGE!!).length)
                } else if (string_3!!.startsWith(LocalizedText.SLIDE.translate(Client.CURRENT_LANGUAGE!!))) {
                    b_5 = 5
                    string_2 = string_2!!.substring(LocalizedText.SLIDE.translate(Client.CURRENT_LANGUAGE!!).length)
                }
            }
            val class1846: BufferedConnectionContext? = Client.connectionContext
            val tcpmessage7: TcpPacket? = PacketEncoder.createPacket(ClientProt.CHAT, class1846!!.isaac)
            tcpmessage7!!.buffer.writeByte(0)
            val value = tcpmessage7!!.buffer.currentPosition
            tcpmessage7!!.buffer.writeByte(b_4)
            tcpmessage7!!.buffer.writeByte(b_5)
            PacketEncoder.chatMessageToBuffer(tcpmessage7!!.buffer, string_2)
            tcpmessage7!!.buffer.writeIndex(tcpmessage7!!.buffer.currentPosition - value)
            class1846!!.queuePacket(tcpmessage7)
        }
    }

    @JvmStatic
    fun basGetAnimReady(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val bas: BasType? = TypeLists.BAS!!.list(i_2)
        if (bas!!.randomStandSequences != null && bas!!.randomStandSequences!!.size > 0) {
            var i_4 = 0
            var i_5 = bas!!.randomStandSequenceChances!![0]
            for (i_6 in 1 until bas!!.randomStandSequences!!.size) {
                if (bas!!.randomStandSequenceChances!![i_6] > i_5) {
                    i_4 = i_6
                    i_5 = bas!!.randomStandSequenceChances!![i_6]
                }
            }
            executor.intStack[++executor.intStackPtr - 1] = bas!!.randomStandSequences!![i_4]
        } else {
            executor.intStack[++executor.intStackPtr - 1] = bas!!.standSequenceId
        }
    }

}
