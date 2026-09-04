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

object QuestOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.QUEST_GETNAME -> questGetName(exec)
            ClientScriptOpCode.QUEST_GETSORTNAME -> questGetSortname(exec)
            ClientScriptOpCode.QUEST_TYPE -> questType(exec)
            ClientScriptOpCode.QUEST_GETDIFFICULTY -> questGetDifficulty(exec)
            ClientScriptOpCode.QUEST_GETMEMBERS -> questGetMembers(exec)
            ClientScriptOpCode.QUEST_POINTS -> questPoints(exec)
            ClientScriptOpCode.QUEST_QUESTREQ_COUNT -> questQuestReqCount(exec)
            ClientScriptOpCode.QUEST_QUESTREQ -> questQuestReq(exec)
            ClientScriptOpCode.QUEST_QUESTREQ_MET -> questQuestReqMet(exec)
            ClientScriptOpCode.QUEST_POINTSREQ -> questPointsReq(exec)
            ClientScriptOpCode.QUEST_POINTSREQ_MET -> questPointsReqMet(exec)
            ClientScriptOpCode.QUEST_STATREQ_COUNT -> questStatReqCount(exec)
            ClientScriptOpCode.QUEST_STATREQ_STAT -> questStatReqStat(exec)
            ClientScriptOpCode.QUEST_STATREQ_LEVEL -> questStatReqLevel(exec)
            ClientScriptOpCode.QUEST_STATREQ_MET -> questStatReqMet(exec)
            ClientScriptOpCode.QUEST_VARPREQ_COUNT -> questVarpReqCount(exec)
            ClientScriptOpCode.QUEST_VARPREQ_DESC -> questVarpReqDesc(exec)
            ClientScriptOpCode.QUEST_VARPREQ_MET -> questVarpReqMet(exec)
            ClientScriptOpCode.QUEST_VARBITREQ_COUNT -> questVarbitReqCount(exec)
            ClientScriptOpCode.QUEST_VARBITREQ_DESC -> questVarbitReqDesc(exec)
            ClientScriptOpCode.QUEST_VARBITREQ_MET -> questVarbitReqMet(exec)
            ClientScriptOpCode.QUEST_ALLREQMET -> questAllReqMet(exec)
            ClientScriptOpCode.QUEST_STARTED -> questStarted(exec)
            ClientScriptOpCode.QUEST_FINISHED -> questFinished(exec)
            ClientScriptOpCode.QUEST_PARAM -> questParam(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by QuestOps")
        }
    }

    @JvmStatic
    fun questVarpReqDesc(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).varpRequirementNames!![executor.intStack[executor.intStackPtr - 1]]
        executor.intStackPtr -= 2
    }

    @JvmStatic
    fun questPointsReqMet(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1])!!.hasQuestpointRequirement(VarProviders.PLAYER_VAR_PROVIDER)) 1 else 0
    }

    @JvmStatic
    fun questQuestReq(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).questPrerequisiteIds!![executor.intStack[executor.intStackPtr - 1]]
        --executor.intStackPtr
    }

    @JvmStatic
    fun questVarbitReqMet(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasVarbitRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun questParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val questId = executor.intStack[executor.intStackPtr]
        val paramId = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (paramType!!.isString()) {
            val defaultString: String? = paramType!!.typeName
            if (questId == -1) {
                executor.stringStack[++executor.stringStackPtr - 1] = defaultString
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(questId).getParamstring(paramId, defaultString)
            }
        } else {
            val defaultInt = paramType!!.defaultInt
            if (questId == -1) {
                executor.intStack[++executor.intStackPtr - 1] = defaultInt
            } else {
                executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUEST!!.list(questId).getParamint(paramId, defaultInt)
            }
        }
    }

    @JvmStatic
    fun questGetSortname(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr]).sortName
    }

    @JvmStatic
    fun questVarpReqMet(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasVarpRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun questPointsReq(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).questpointRequirement
    }

    @JvmStatic
    fun questGetName(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr]).name
    }

    @JvmStatic
    fun questQuestReqCount(executor: CS2Executor) {
        val quest: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (quest!!.questPrerequisiteIds == null) 0 else quest.questPrerequisiteIds!!.size
    }

    @JvmStatic
    fun questStatReqCount(executor: CS2Executor) {
        val quest: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (quest!!.levelRequirements == null) 0 else quest!!.levelRequirements!!.size
    }

    @JvmStatic
    fun questAllReqMet(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).hasAllRequirements(VarProviders.PLAYER_VAR_PROVIDER, Client.SKILL_LEVEL_ACTUAL)) 1 else 0
    }

    @JvmStatic
    fun questStarted(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).isStarted(VarProviders.PLAYER_VAR_PROVIDER)) 1 else 0
    }

    @JvmStatic
    fun questStatReqStat(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).levelRequirements!![executor.intStack[executor.intStackPtr - 1]][0]
        --executor.intStackPtr
    }

    @JvmStatic
    fun questVarbitReqDesc(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).varbitRequirementNames!![executor.intStack[executor.intStackPtr - 1]]
        executor.intStackPtr -= 2
    }

    @JvmStatic
    fun questVarbitReqCount(executor: CS2Executor) {
        val quest: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (quest!!.varbitRequirements == null) 0 else quest!!.varbitRequirements!!.size
    }

    @JvmStatic
    fun questGetDifficulty(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).difficulty
    }

    @JvmStatic
    fun questType(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).type
    }

    @JvmStatic
    fun questFinished(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).isComplete(VarProviders.PLAYER_VAR_PROVIDER)) 1 else 0
    }

    @JvmStatic
    fun questPoints(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).questPointReward
    }

    @JvmStatic
    fun questQuestReqMet(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasPrerequisiteRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun questVarpReqCount(executor: CS2Executor) {
        val quest: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (quest!!.varpRequirements == null) 0 else quest!!.varpRequirements!!.size
    }

    @JvmStatic
    fun questStatReqMet(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasLevelRequirement(Client.SKILL_LEVEL_ACTUAL, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun questGetMembers(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).members) 1 else 0
    }

    @JvmStatic
    fun questStatReqLevel(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).levelRequirements!![executor.intStack[executor.intStackPtr - 1]][1]
        --executor.intStackPtr
    }

}
