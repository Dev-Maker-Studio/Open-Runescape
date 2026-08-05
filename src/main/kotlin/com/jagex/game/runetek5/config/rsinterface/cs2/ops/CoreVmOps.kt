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

object CoreVmOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.PUSH_CONSTANT_INT -> pushInt(exec)
            ClientScriptOpCode.PUSH_VAR -> loadVarp(exec)
            ClientScriptOpCode.POP_VAR -> storeVarp(exec)
            ClientScriptOpCode.PUSH_CONSTANT_STRING -> pushString(exec)
            ClientScriptOpCode.BRANCH -> gotoInstr(exec)
            ClientScriptOpCode.BRANCH_NOT -> intNotEqual(exec)
            ClientScriptOpCode.BRANCH_EQUALS -> intEqual(exec)
            ClientScriptOpCode.BRANCH_LESS_THAN -> intLessThan(exec)
            ClientScriptOpCode.BRANCH_GREATER_THAN -> intGreaterThan(exec)
            ClientScriptOpCode.GET_VARP_OLD -> getVarpOld(exec)
            ClientScriptOpCode.GET_VARPBIT_OLD -> getVarpbitOld(exec)
            ClientScriptOpCode.GET_VARN_OLD -> getVarnOld(exec)
            ClientScriptOpCode.GET_VARNBIT_OLD -> getVarnbitOld(exec)
            ClientScriptOpCode.RETURN -> returnInstr(exec)
            ClientScriptOpCode.PUSH_VARBIT -> loadVarpbit(exec)
            ClientScriptOpCode.POP_VARBIT -> storeVarpbit(exec)
            ClientScriptOpCode.BRANCH_LESS_THAN_OR_EQUALS -> intLessOrEqual(exec)
            ClientScriptOpCode.BRANCH_GREATER_THAN_OR_EQUALS -> intGreaterOrEqual(exec)
            ClientScriptOpCode.PUSH_INT_LOCAL -> loadInt(exec)
            ClientScriptOpCode.POP_INT_LOCAL -> storeInt(exec)
            ClientScriptOpCode.PUSH_STRING_LOCAL -> loadString(exec)
            ClientScriptOpCode.POP_STRING_LOCAL -> storeString(exec)
            ClientScriptOpCode.JOIN_STRING -> mergeStrings(exec)
            ClientScriptOpCode.POP_INT_DISCARD -> popInt(exec)
            ClientScriptOpCode.POP_STRING_DISCARD -> popString(exec)
            ClientScriptOpCode.GOSUB_WITH_PARAMS -> callCS2Script(exec)
            ClientScriptOpCode.LOAD_VARC -> loadVarc(exec)
            ClientScriptOpCode.STORE_VARC -> storeVarc(exec)
            ClientScriptOpCode.DEFINE_ARRAY -> arrayNew(exec)
            ClientScriptOpCode.PUSH_ARRAY_INT -> arrayLoad(exec)
            ClientScriptOpCode.POP_ARRAY_INT -> arrayStore(exec)
            ClientScriptOpCode.LOAD_VARC_STRING -> loadVarcString(exec)
            ClientScriptOpCode.STORE_VARC_STRING -> storeVarcString(exec)
            ClientScriptOpCode.SWITCH -> switchInstr(exec)
            ClientScriptOpCode.PUSH_LONG_CONSTANT -> pushLong(exec)
            ClientScriptOpCode.POP_LONG_DISCARD -> popLong(exec)
            ClientScriptOpCode.PUSH_LONG_LOCAL -> loadLong(exec)
            ClientScriptOpCode.POP_LONG_LOCAL -> storeLong(exec)
            ClientScriptOpCode.LONG_BRANCH_NOT -> longNotEqual(exec)
            ClientScriptOpCode.LONG_BRANCH_EQUALS -> longEqual(exec)
            ClientScriptOpCode.LONG_BRANCH_LESS_THAN -> longLessThan(exec)
            ClientScriptOpCode.LONG_BRANCH_GREATER_THAN -> longGreaterThan(exec)
            ClientScriptOpCode.LONG_BRANCH_LESS_THAN_OR_EQUALS -> longLessOrEqual(exec)
            ClientScriptOpCode.LONG_BRANCH_GREATER_THAN_OR_EQUALS -> longGreaterOrEqual(exec)
            ClientScriptOpCode.BRANCH_IF_TRUE -> branchEq1(exec)
            ClientScriptOpCode.BRANCH_IF_FALSE -> branchEq0(exec)
            ClientScriptOpCode.LOAD_CLAN_VAR -> loadClanVar(exec)
            ClientScriptOpCode.LOAD_CLAN_VARBIT -> loadClanVarbit(exec)
            ClientScriptOpCode.LOAD_CLAN_VAR_LONG -> loadClanVarLong(exec)
            ClientScriptOpCode.LOAD_CLAN_VAR_STRING -> loadClanVarString(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VAR -> loadClanSettingVar(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VARBIT -> loadClanSettingVarbit(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VAR_LONG -> loadClanSettingVarLong(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VAR_STRING -> loadClanSettingVarString(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by CoreVmOps")
        }
    }

    @JvmStatic
    fun loadClanVarbit(executor: CS2Executor) {
        val i_2 = executor.intOpValues!![executor.instrPtr]
        val class4053: ClanVarType? = TypeLists.CLAN_VAR!!.list(i_2)
        if (class4053!!.aChar4832.code != 1) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
        val integer4: Int? = ClanVarTypeList.CLAN_VARS!![class4053!!.baseVar] as Int?
        if (integer4 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            val i_5 = if (class4053!!.endBit == 31) -1 else ((1 shl (class4053!!.endBit + 1)) - 1)
            executor.intStack[++executor.intStackPtr - 1] = (integer4!!.toInt() and i_5) ushr class4053!!.startBit
        }
    }

    @JvmStatic
    fun loadClanSettingVarLong(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        val long_3: Long? = executor.currentClanSettings!!.getLongVar((Client.CURRENT_GAME!!.id shl 16) or value)
        var long_4: Long = 0L
        if (long_3 == null) {
            long_4 = -1L
        } else {
            long_4 = long_3
        }
        executor.longStack[++executor.longStackPtr - 1] = long_4
    }

    @JvmStatic
    fun loadClanSettingVarString(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        val string_3: String? = executor.currentClanSettings!!.getStringVar((Client.CURRENT_GAME!!.id shl 16) or value)
        var string_4: String? = null
        if (string_3 == null) {
            string_4 = ""
        } else {
            string_4 = string_3
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_4
    }

    @JvmStatic
    fun gotoInstr(executor: CS2Executor) {
        executor.instrPtr += executor.intOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun arrayLoad(executor: CS2Executor) {
        val i_2 = executor.intOpValues!![executor.instrPtr]
        val i_3 = executor.intStack[--executor.intStackPtr]
        if (i_3 >= 0 && i_3 < executor.globalArrayLengths[i_2]) {
            executor.intStack[++executor.intStackPtr - 1] = executor.globalArrays[i_2][i_3]
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun longLessThan(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] < executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun branchEq1(executor: CS2Executor) {
        if (executor.intStack[--executor.intStackPtr] == 1) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun branchEq0(executor: CS2Executor) {
        if (executor.intStack[--executor.intStackPtr] == 0) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun storeVarc(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        VarcTypeList.VARC_INT!![value] = executor.intStack[--executor.intStackPtr]
        PulseEventQueue.delayedClientvarcUpdate(value)
        Client.clientIsActive = Client.clientIsActive or VarcTypeList.VARC_BOOL!![value]
    }

    @JvmStatic
    fun longGreaterThan(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] > executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun storeString(executor: CS2Executor) {
        executor.objectLocals!![executor.intOpValues!![executor.instrPtr]] = executor.stringStack[--executor.stringStackPtr]
    }

    @JvmStatic
    fun storeVarcString(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        VarcTypeList.VARC_STRING!![value] = executor.stringStack[--executor.stringStackPtr] as String?
        PulseEventQueue.delayedVarcstringUpdate(value)
    }

    @JvmStatic
    fun longGreaterOrEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] >= executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun intEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr + 1] == executor.intStack[executor.intStackPtr]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun loadClanVarString(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        val text: String? = ClanVarTypeList.CLAN_VARS!![value] as String?
        if (text == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = text
        }
    }

    @JvmStatic
    fun loadInt(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.intLocals!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun intLessThan(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] < executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun pushLong(executor: CS2Executor) {
        executor.longStack[++executor.longStackPtr - 1] = executor.current!!.longOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun storeVarp(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        VarProviders.PLAYER_VAR_PROVIDER.setVarValue(value, executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun loadVarc(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = VarcTypeList.VARC_INT!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun intGreaterOrEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] >= executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun storeLong(executor: CS2Executor) {
        executor.longLocals!![executor.intOpValues!![executor.instrPtr]] = executor.longStack[--executor.longStackPtr]
    }

    @JvmStatic
    fun longLessOrEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] <= executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun loadClanSettingVarbit(executor: CS2Executor) {
        val i_2 = executor.intOpValues!![executor.instrPtr]
        val class5373: ClanVarSettingsType? = TypeLists.CLAN_VAR_SETTINGS!!.list(i_2)
        if (class5373 == null) {
            throw RuntimeException()
        } else {
            val integer4: Int? = executor.currentClanSettings!!.getVariableBitfield((Client.CURRENT_GAME!!.id shl 16) or class5373!!.baseVar, class5373!!.startBit, class5373!!.endBit)
            var i_5: Int = 0
            if (integer4 == null) {
                i_5 = 0
            } else {
                i_5 = integer4
            }
            executor.intStack[++executor.intStackPtr - 1] = i_5
        }
    }

    @JvmStatic
    fun loadVarcString(executor: CS2Executor) {
        var text: String? = VarcTypeList.VARC_STRING!![executor.intOpValues!![executor.instrPtr]]
        if (text == null) {
            text = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = text
    }

    @JvmStatic
    fun popString(executor: CS2Executor) {
        --executor.stringStackPtr
    }

    @JvmStatic
    fun getVarpOld(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as PlayerEntity).playerConfigs.getVarp(value)
    }

    @JvmStatic
    fun arrayNew(executor: CS2Executor) {
        val i_2 = executor.intOpValues!![executor.instrPtr] shr 16
        val i_3 = executor.intOpValues!![executor.instrPtr] and 0xffff
        val i_4 = executor.intStack[--executor.intStackPtr]
        if (i_4 >= 0 && i_4 <= 5000) {
            executor.globalArrayLengths[i_2] = i_4
            var b_5 = -1
            if (i_3 == 105) {
                b_5 = 0
            }
            for (i_6 in 0 until i_4) {
                executor.globalArrays[i_2][i_6] = b_5
            }
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun longNotEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] != executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun getVarnbitOld(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as NpcEntity).varns.getVarnBit(value)
    }

    @JvmStatic
    fun returnInstr(executor: CS2Executor) {
        if (executor.returnValuesPtr != 0) {
            val class5092: CS2CallFrame? = executor.returnValues[--executor.returnValuesPtr]
            executor.current = class5092!!.savedScript
            executor.operations = executor.current!!.operations
            executor.intOpValues = executor.current!!.intOpValues
            executor.instrPtr = class5092!!.savedInstrPtr
            executor.intLocals = class5092!!.savedIntLocals
            executor.objectLocals = class5092!!.savedStringLocals
            executor.longLocals = class5092!!.savedLongLocals
        }
    }

    @JvmStatic
    fun switchInstr(executor: CS2Executor) {
        val iterablenodemap2: IterableNodeMap<IntNode>? = executor.current!!.switchMaps!![executor.intOpValues!![executor.instrPtr]]
        val class282Sub383: IntNode? = iterablenodemap2?.get(executor.intStack[--executor.intStackPtr].toLong())
        if (class282Sub383 != null) {
            executor.instrPtr += class282Sub383.value
        }
    }

    @JvmStatic
    fun callCS2Script(executor: CS2Executor) {
        val i_2 = executor.intOpValues!![executor.instrPtr]
        val cs2script3: CS2Script? = CS2Script.getCs2Script(i_2)
        if (cs2script3 == null) {
            throw RuntimeException()
        } else {
            val stack: IntArray? = IntArray(cs2script3!!.intLocalsCount)
            val arr_5: Array<Any?>? = arrayOfNulls<Any>(cs2script3!!.stringLocalsCount)
            val longs_6: LongArray? = LongArray(cs2script3!!.longLocalsCount)
            val i_7: Int = 0
            for (i_7 in 0 until cs2script3!!.intArgsCount) {
                stack!![i_7] = executor.intStack[i_7 + (executor.intStackPtr - cs2script3!!.intArgsCount)]
            }
            for (i_7 in 0 until cs2script3!!.stringArgsCount) {
                arr_5!![i_7] = executor.stringStack[i_7 + (executor.stringStackPtr - cs2script3!!.stringArgsCount)]
            }
            for (i_7 in 0 until cs2script3!!.longArgsCount) {
                longs_6!![i_7] = executor.longStack[i_7 + (executor.longStackPtr - cs2script3!!.longArgsCount)]
            }
            executor.intStackPtr -= cs2script3!!.intArgsCount
            executor.stringStackPtr -= cs2script3!!.stringArgsCount
            executor.longStackPtr -= cs2script3!!.longArgsCount
            val class5098: CS2CallFrame? = CS2CallFrame()
            class5098!!.savedScript = executor.current
            class5098!!.savedInstrPtr = executor.instrPtr
            class5098!!.savedIntLocals = executor.intLocals
            class5098!!.savedStringLocals = executor.objectLocals
            class5098!!.savedLongLocals = executor.longLocals
            if (executor.returnValuesPtr >= executor.returnValues!!.size) {
                throw RuntimeException()
            } else {
                executor.returnValues[++executor.returnValuesPtr - 1] = class5098
                executor.current = cs2script3
                executor.operations = executor.current!!.operations
                executor.intOpValues = executor.current!!.intOpValues
                executor.instrPtr = -1
                executor.intLocals = stack
                executor.objectLocals = arr_5
                executor.longLocals = longs_6
            }
        }
    }

    @JvmStatic
    fun storeInt(executor: CS2Executor) {
        executor.intLocals!![executor.intOpValues!![executor.instrPtr]] = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun arrayStore(executor: CS2Executor) {
        val i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStackPtr -= 2
        val i_3 = executor.intStack[executor.intStackPtr]
        if (i_3 >= 0 && i_3 < executor.globalArrayLengths[i_2]) {
            executor.globalArrays[i_2][i_3] = executor.intStack[executor.intStackPtr + 1]
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun getVarnOld(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as NpcEntity).varns.getVarn(value)
    }

    @JvmStatic
    fun loadLong(executor: CS2Executor) {
        executor.longStack[++executor.longStackPtr - 1] = executor.longLocals!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun loadClanSettingVar(executor: CS2Executor) {
        val i_2 = executor.intOpValues!![executor.instrPtr]
        val class5373: ClanVarSettingsType? = TypeLists.CLAN_VAR_SETTINGS!!.list(i_2)
        if (class5373 == null) {
            throw RuntimeException()
        } else {
            val integer4: Int? = executor.currentClanSettings!!.getIntVar((Client.CURRENT_GAME!!.id shl 16) or i_2)
            var i_5: Int = 0
            if (integer4 == null) {
                if (class5373!!.aChar7096.code != 105 && class5373!!.aChar7096.code != 49) {
                    i_5 = -1
                } else {
                    i_5 = 0
                }
            } else {
                i_5 = integer4
            }
            executor.intStack[++executor.intStackPtr - 1] = i_5
        }
    }

    @JvmStatic
    fun loadClanVar(executor: CS2Executor) {
        val param = executor.intOpValues!![executor.instrPtr]
        val integer3: Int? = ClanVarTypeList.CLAN_VARS!![param] as Int?
        if (integer3 == null) {
            val clanvarType: ClanVarType? = TypeLists.CLAN_VAR!!.list(param)
            if (clanvarType!!.aChar4832.code != 105 && clanvarType!!.aChar4832.code != 49) {
                executor.intStack[++executor.intStackPtr - 1] = -1
            } else {
                executor.intStack[++executor.intStackPtr - 1] = 0
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = integer3
        }
    }

    @JvmStatic
    fun loadString(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.objectLocals!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun mergeStrings(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        executor.stringStackPtr -= value
        val text: String? = Utils.joinStrings(executor.stringStack, executor.stringStackPtr, value)
        executor.stringStack[++executor.stringStackPtr - 1] = text
    }

    @JvmStatic
    fun loadVarpbit(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(value)
    }

    @JvmStatic
    fun intLessOrEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] <= executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun longEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr + 1] == executor.longStack[executor.longStackPtr]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun loadVarp(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = VarProviders.PLAYER_VAR_PROVIDER.activeVars[value]
    }

    @JvmStatic
    fun pushInt(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.intOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun pushString(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.current!!.stringOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun intGreaterThan(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] > executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun loadClanVarLong(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        val longValue: Long? = ClanVarTypeList.CLAN_VARS!![value] as Long?
        if (longValue == null) {
            executor.longStack[++executor.longStackPtr - 1] = -1L
        } else {
            executor.longStack[++executor.longStackPtr - 1] = longValue
        }
    }

    @JvmStatic
    fun popInt(executor: CS2Executor) {
        --executor.intStackPtr
    }

    @JvmStatic
    fun popLong(executor: CS2Executor) {
        --executor.longStackPtr
    }

    @JvmStatic
    fun getVarpbitOld(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as PlayerEntity).playerConfigs.getVarbit(value)
    }

    @JvmStatic
    fun storeVarpbit(executor: CS2Executor) {
        val value = executor.intOpValues!![executor.instrPtr]
        VarProviders.PLAYER_VAR_PROVIDER.setVarBitValue(value, executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun intNotEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] != executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

}
