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
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Script
import com.jagex.game.runetek5.config.rsinterface.cs2.ClientScriptOpCode

object EnumEmailFriendOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.ENUM_STRING -> enumString(exec)
            ClientScriptOpCode.ENUM -> enumLookup(exec)
            ClientScriptOpCode.ENUM_HASOUTPUT -> enumHasOutput(exec)
            ClientScriptOpCode.ENUM_HASOUTPUT_STRING -> enumHasOutputString(exec)
            ClientScriptOpCode.ENUM_GETOUTPUTCOUNT -> getEnumSize(exec)
            ClientScriptOpCode.ENUM_GETREVERSECOUNT -> enumGetReverseCount(exec)
            ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING -> enumGetReverseCountString(exec)
            ClientScriptOpCode.ENUM_GETREVERSEINDEX -> enumGetReverseIndex(exec)
            ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING -> enumGetReverseIndexString(exec)
            ClientScriptOpCode.EMAIL_VALIDATION_SUBMIT_CODE -> emailValidationSubmitCode(exec)
            ClientScriptOpCode.EMAIL_VALIDATION_CHANGE_ADDRESS -> emailValidationChangeAddress(exec)
            ClientScriptOpCode.EMAIL_VALIDATION_ADD_NEW_ADDRESS -> emailValidationAddNewAddress(exec)
            ClientScriptOpCode.FRIEND_COUNT -> friendCount(exec)
            ClientScriptOpCode.FRIEND_GETNAME -> friendGetName(exec)
            ClientScriptOpCode.FRIEND_GETWORLD -> friendGetWorld(exec)
            ClientScriptOpCode.FRIEND_GETRANK -> friendGetRank(exec)
            ClientScriptOpCode.FRIEND_GETWORLDFLAGS -> friendGetWorldFlags(exec)
            ClientScriptOpCode.FRIEND_SETRANK -> friendSetRank(exec)
            ClientScriptOpCode.FRIEND_ADD -> friendAdd(exec)
            ClientScriptOpCode.FRIEND_DEL -> friendDel(exec)
            ClientScriptOpCode.IGNORE_ADD -> ignoreAdd(exec)
            ClientScriptOpCode.IGNORE_DEL -> ignoreDel(exec)
            ClientScriptOpCode.FRIEND_TEST -> friendTest(exec)
            ClientScriptOpCode.FRIEND_GETWORLDNAME -> friendGetWorldName(exec)
            ClientScriptOpCode.FC_GETCHATDISPLAYNAME -> loadFriendchatName(exec)
            ClientScriptOpCode.FC_GETCHATCOUNT -> fcGetChatCount(exec)
            ClientScriptOpCode.FC_GETCHATUSERNAME -> fcGetChatUserName(exec)
            ClientScriptOpCode.FC_GETCHATUSERWORLD -> fcGetChatUserWorld(exec)
            ClientScriptOpCode.FC_GETCHATUSERRANK -> fcGetChatUserRank(exec)
            ClientScriptOpCode.FC_GETCHATMINKICK -> fcGetChatMinKick(exec)
            ClientScriptOpCode.FC_KICKUSER -> fcKickUser(exec)
            ClientScriptOpCode.FC_GETCHATRANK -> fcGetChatRank(exec)
            ClientScriptOpCode.FC_JOINCHAT -> fcJoinChat(exec)
            ClientScriptOpCode.FC_LEAVECHAT -> fcLeaveChat()
            ClientScriptOpCode.IGNORE_COUNT -> ignoreCount(exec)
            ClientScriptOpCode.IGNORE_GETNAME -> ignoreGetName(exec)
            ClientScriptOpCode.IGNORE_TEST -> ignoreTest(exec)
            ClientScriptOpCode.FC_ISSELF -> fcIsself(exec)
            ClientScriptOpCode.FC_GETCHATOWNERNAME -> fcGetChatOwnerName(exec)
            ClientScriptOpCode.FC_GETCHATUSERWORLDNAME -> fcGetChatUserWorldName(exec)
            ClientScriptOpCode.FRIEND_PLATFORM -> friendPlatform(exec)
            ClientScriptOpCode.FRIEND_GETSLOTFROMNAME -> friendGetSlotFromName(exec)
            ClientScriptOpCode.PLAYERCOUNTRY -> playerCountry(exec)
            ClientScriptOpCode.IGNORE_ADD_TEMP -> ignoreAddTemp(exec)
            ClientScriptOpCode.IGNORE_IS_TEMP -> ignoreIsTemp(exec)
            ClientScriptOpCode.FC_GETCHATUSERNAME_UNFILTERED -> fcGetChatUserNameUnfiltered(exec)
            ClientScriptOpCode.IGNORE_GETNAME_UNFILTERED -> ignoreGetNameUnfiltered(exec)
            ClientScriptOpCode.FRIEND_IS_REFERRER -> friendIsReferrer(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by EnumEmailFriendOps")
        }
    }

    @JvmStatic
    fun enumGetReverseCount(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        if (i_3 == -1) {
            throw RuntimeException()
        } else {
            val enumdefinitions5: EnumType? = TypeLists.ENUM!!.list(i_3)
            if (i_2 != enumdefinitions5!!.valueType.code) {
                throw RuntimeException()
            } else {
                val stack: IntArray? = enumdefinitions5!!.getValueReferences(i_4)
                var i_7 = 0
                if (stack != null) {
                    i_7 = stack!!.size
                }
                executor.intStack[++executor.intStackPtr - 1] = i_7
            }
        }
    }

    @JvmStatic
    fun emailValidationAddNewAddress(executor: CS2Executor) {
        --executor.stringStackPtr
        executor.intStackPtr -= 3
        val text: String? = executor.stringStack[executor.stringStackPtr] as String?
        val bool3 = executor.intStack[executor.intStackPtr] == 1
        val bool4 = executor.intStack[executor.intStackPtr + 1] == 1
        val bool5 = executor.intStack[executor.intStackPtr + 2] == 1
        val tcpmessage6: TcpPacket? = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_ADD_NEW_ADDRESS, Client.LOBBY_CONNECTION_CONTEXT.isaac)
        tcpmessage6!!.buffer.writeShort(TextUtils.getLength(text!!) + 1)
        tcpmessage6!!.buffer.writeString(text!!)
        var value = 0
        if (bool3) {
            value = value or 0x1
        }
        if (bool4) {
            value = value or 0x2
        }
        if (bool5) {
            value = value or 0x4
        }
        tcpmessage6!!.buffer.writeByte(value)
        Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage6)
    }

    @JvmStatic
    fun fcLeaveChat() {
        PacketEncoder.sendLeaveFriendsChatPacket()
    }

    @JvmStatic
    fun enumLookup(executor: CS2Executor) {
        executor.intStackPtr -= 4
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val i_5 = executor.intStack[executor.intStackPtr + 3]
        val enumDefinitions: EnumType? = TypeLists.ENUM!!.list(i_4)
        if (i_2 == enumDefinitions!!.keyType.code && i_3 == enumDefinitions!!.valueType.code) {
            if (i_3 == 's'.code) {
                executor.stringStack[++executor.stringStackPtr - 1] = enumDefinitions!!.getStringValue(i_5)
            } else {
                executor.intStack[++executor.intStackPtr - 1] = enumDefinitions!!.getIntValue(i_5)
            }
        } else {
            throw RuntimeException("$i_4 $i_5")
        }
    }

    @JvmStatic
    fun friendDel(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        FriendList.remove(text)
    }

    @JvmStatic
    fun friendGetSlotFromName(executor: CS2Executor) {
        var text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (text!!.startsWith(TextUtils.getCrown(0)) || text!!.startsWith(TextUtils.getCrown(1))) {
            text = text!!.substring(7)
        }
        executor.intStack[++executor.intStackPtr - 1] = FriendList.getIndex(text)
    }

    @JvmStatic
    fun friendTest(executor: CS2Executor) {
        var text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (text!!.startsWith(TextUtils.getCrown(0)) || text!!.startsWith(TextUtils.getCrown(1))) {
            text = text!!.substring(7)
        }
        executor.intStack[++executor.intStackPtr - 1] = if (FriendList.contains(text!!)) 1  else 0
    }

    @JvmStatic
    fun fcGetChatUserRank(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && value < FriendsChat.FC_PLAYER_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYERS!![value]!!.rank.toInt()
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun enumGetReverseCountString(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (i_2 == -1) {
            throw RuntimeException()
        } else {
            val enumdefinitions4: EnumType? = TypeLists.ENUM!!.list(i_2)
            if (enumdefinitions4!!.valueType.code != 115) {
                throw RuntimeException()
            } else {
                val stack: IntArray? = enumdefinitions4!!.getValueReferences(text!!)
                var i_6 = 0
                if (stack != null) {
                    i_6 = stack!!.size
                }
                executor.intStack[++executor.intStackPtr - 1] = i_6
            }
        }
    }

    @JvmStatic
    fun friendGetWorld(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && value < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[value]!!.worldId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun enumHasOutput(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        if (i_3 == -1) {
            throw RuntimeException()
        } else {
            val enumdefinitions5: EnumType? = TypeLists.ENUM!!.list(i_3)
            if (i_2 != enumdefinitions5!!.valueType.code) {
                throw RuntimeException()
            } else {
                executor.intStack[++executor.intStackPtr - 1] = if (enumdefinitions5!!.containsKey(i_4)) 1  else 0
            }
        }
    }

    @JvmStatic
    fun friendGetWorldName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && value < FriendList.FRIEND_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendList.FRIENDS[value]!!.worldName
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ignoreGetName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED != 0 && value < IgnoreList.IGNORE_COUNT) {
            val class103: Ignore? = IgnoreList.IGNORED[value]
            executor.stringStack[++executor.stringStackPtr - 1] = class103!!.unfilteredUsername
            if (class103!!.displayName != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = class103!!.displayName
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = ""
            }
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ignoreDel(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        IgnoreList.remove(text)
    }

    @JvmStatic
    fun friendCount(executor: CS2Executor) {
        if (FriendList.LOADED == 0) {
            executor.intStack[++executor.intStackPtr - 1] = -2
        } else if (FriendList.LOADED == 1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIEND_COUNT
        }
    }

    @JvmStatic
    fun fcGetChatOwnerName(executor: CS2Executor) {
        if (FriendsChat.FC_OWNER_NAME != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_OWNER_NAME
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun enumString(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val enumdefinitions4: EnumType? = TypeLists.ENUM!!.list(i_2)
        executor.stringStack[++executor.stringStackPtr - 1] = enumdefinitions4!!.getStringValue(i_3)
    }

    @JvmStatic
    fun friendPlatform(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && value >= 0 && value < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[value]!!.platform
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun fcGetChatRank(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = FriendsChat.MY_PLAYER_FC_RANK.toInt()
    }

    @JvmStatic
    fun friendGetName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && value < FriendList.FRIEND_COUNT) {
            val class63: Friend? = FriendList.FRIENDS[value]
            executor.stringStack[++executor.stringStackPtr - 1] = class63!!.displayName
            if (class63!!.previousDisplayName != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = class63!!.previousDisplayName
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = ""
            }
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ignoreIsTemp(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (IgnoreList.IGNORED!![value]!!.temporary) 1  else 0
    }

    @JvmStatic
    fun fcGetChatUserWorldName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && value < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS!![value]!!.worldName
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun emailValidationSubmitCode(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_SUBMIT_CODE, Client.LOBBY_CONNECTION_CONTEXT.isaac)
        tcpmessage3!!.buffer.writeByte(TextUtils.getLength(text!!))
        tcpmessage3!!.buffer.writeString(text!!)
        Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun fcJoinChat(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        PacketEncoder.sendJoinFriendchat(text)
    }

    @JvmStatic
    fun friendAdd(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        FriendList.add(text)
    }

    @JvmStatic
    fun ignoreGetNameUnfiltered(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED != 0 && value < IgnoreList.IGNORE_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = IgnoreList.IGNORED!![value]!!.unfilteredUsername
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun fcGetChatUserWorld(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && value < FriendsChat.FC_PLAYER_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYERS!![value]!!.worldId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun getEnumSize(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val enumdefinitions3: EnumType? = TypeLists.ENUM!!.list(value)
        executor.intStack[++executor.intStackPtr - 1] = enumdefinitions3!!.getSize()
    }

    @JvmStatic
    fun loadFriendchatName(executor: CS2Executor) {
        if (FriendsChat.FC_NAME != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = StaticMethods.decodeFriendchatName(FriendsChat.FC_NAME!!)
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ignoreAdd(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        IgnoreList.add(text, false)
    }

    @JvmStatic
    fun fcGetChatMinKick(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_MIN_RANK_CAN_KICK.toInt()
    }

    @JvmStatic
    fun fcIsself(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_PLAYERS != null && value < FriendsChat.FC_PLAYER_COUNT && FriendsChat.FC_PLAYERS!![value]!!.username.equals(PlayerUpdate.MY_PLAYER!!.displayName)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun friendGetRank(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && value < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[value]!!.friendsChatRank
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun fcGetChatUserName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && value < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS!![value]!!.displayName
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun enumGetReverseIndex(executor: CS2Executor) {
        executor.intStackPtr -= 5
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val i_5 = executor.intStack[executor.intStackPtr + 3]
        val i_6 = executor.intStack[executor.intStackPtr + 4]
        if (i_4 == -1) {
            throw RuntimeException()
        } else {
            val enumdefinitions7: EnumType? = TypeLists.ENUM!!.list(i_4)
            if (i_3 != enumdefinitions7!!.keyType.code) {
                throw RuntimeException()
            } else if (i_2 != enumdefinitions7!!.valueType.code) {
                throw RuntimeException()
            } else {
                val stack: IntArray? = enumdefinitions7!!.getValueReferences(i_5)
                if (i_6 >= 0 && stack != null && stack!!.size > i_6) {
                    executor.intStack[++executor.intStackPtr - 1] = stack!![i_6]
                } else {
                    throw RuntimeException()
                }
            }
        }
    }

    @JvmStatic
    fun ignoreAddTemp(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        IgnoreList.add(text, true)
    }

    @JvmStatic
    fun friendIsReferrer(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && value < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = if (FriendList.FRIENDS[value]!!.referrer) 1  else 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun playerCountry(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.COUNTRY
    }

    @JvmStatic
    fun fcGetChatCount(executor: CS2Executor) {
        if (FriendsChat.FC_NAME != null) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYER_COUNT
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun enumHasOutputString(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (value == -1) {
            throw RuntimeException()
        } else {
            val enumdefinitions4: EnumType? = TypeLists.ENUM!!.list(value)
            if (enumdefinitions4!!.valueType.code != 's'.code) {
                throw RuntimeException()
            } else {
                executor.intStack[++executor.intStackPtr - 1] = if (enumdefinitions4!!.containsKey(text!!)) 1  else 0
            }
        }
    }

    @JvmStatic
    fun enumGetReverseIndexString(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (i_3 == -1) {
            throw RuntimeException()
        } else {
            val enumdefinitions6: EnumType? = TypeLists.ENUM!!.list(i_3)
            if (i_2 != enumdefinitions6!!.keyType.code) {
                throw RuntimeException()
            } else if (enumdefinitions6!!.valueType.code != 115) {
                throw RuntimeException()
            } else {
                val stack: IntArray? = enumdefinitions6!!.getValueReferences(text!!)
                if (i_4 >= 0 && stack != null && stack!!.size > i_4) {
                    executor.intStack[++executor.intStackPtr - 1] = stack!![i_4]
                } else {
                    throw RuntimeException()
                }
            }
        }
    }

    @JvmStatic
    fun emailValidationChangeAddress(executor: CS2Executor) {
        val string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_CHANGE_ADDRESS, Client.LOBBY_CONNECTION_CONTEXT.isaac)
        tcpmessage4!!.buffer.writeShort(TextUtils.getLength(string_2!!) + TextUtils.getLength(string_3!!))
        tcpmessage4!!.buffer.writeString(string_2!!)
        tcpmessage4!!.buffer.writeString(string_3!!)
        Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun ignoreCount(executor: CS2Executor) {
        if (FriendList.LOADED == 0) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = IgnoreList.IGNORE_COUNT
        }
    }

    @JvmStatic
    fun ignoreTest(executor: CS2Executor) {
        var text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (text!!.startsWith(TextUtils.getCrown(0)) || text!!.startsWith(TextUtils.getCrown(1))) {
            text = text!!.substring(7)
        }
        executor.intStack[++executor.intStackPtr - 1] = if (IgnoreList.contains(text!!)) 1  else 0
    }

    @JvmStatic
    fun fcKickUser(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        FriendsChat.kickPlayer(text!!)
    }

    @JvmStatic
    fun friendGetWorldFlags(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && value < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[value]!!.worldFlags
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun fcGetChatUserNameUnfiltered(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && value < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS!![value]!!.username
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun friendSetRank(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val value = executor.intStack[--executor.intStackPtr]
        FriendsChat.setRank(text!!, value)
    }

}
