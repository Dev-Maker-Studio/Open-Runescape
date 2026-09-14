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

object DetailCapabilityOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.DETAILCANMOD_GROUNDDECOR -> detailCanModGroundDecor(exec)
            ClientScriptOpCode.DETAILCANMOD_CHARSHADOWS -> detailCanModCharShadows(exec)
            ClientScriptOpCode.DETAILCANMOD_SPOTSHADOWS -> detailCanModSpotShadows(exec)
            ClientScriptOpCode.DETAILCANMOD_WATERDETAIL -> detailCanModWaterDetail(exec)
            ClientScriptOpCode.DETAILCANMOD_ANTIALIASING -> detailCanModAntialiasing(exec)
            ClientScriptOpCode.DETAILCANMOD_PARTICLES -> detailCanModParticles(exec)
            ClientScriptOpCode.DETAILCANMOD_BUILDAREA -> loadMemoryMode(exec)
            ClientScriptOpCode.DETAILCANMOD_BLOOM -> detailCanModBloom(exec)
            ClientScriptOpCode.DETAILCANMOD_GROUNDBLENDING -> detailCanModGroundBlending(exec)
            ClientScriptOpCode.DETAILCANMOD_TEXTURING -> detailCanModTexturing(exec)
            ClientScriptOpCode.DETAILCANMOD_MAXSCREENSIZE -> detailCanModMaxScreenSize(exec)
            ClientScriptOpCode.DETAILCANMOD_FOG -> detailCanModFog(exec)
            ClientScriptOpCode.DETAILCANMOD_DEFAULTTOOLKIT -> detailCanModDefaultToolkit(exec)
            ClientScriptOpCode.DETAILCANMOD_TOOLKIT_DEFAULT -> detailCanModToolkitDefault(exec)
            ClientScriptOpCode.DETAILCANMOD_SKYDETAIL -> detailCanModSkyDetail(exec)
            ClientScriptOpCode.DETAILCANSET_GROUNDDECOR -> detailCanSetGroundDecor(exec)
            ClientScriptOpCode.DETAILCANSET_CHARSHADOWS -> detailCanSetCharShadows(exec)
            ClientScriptOpCode.DETAILCANSET_SPOTSHADOWS -> detailCanSetSpotShadows(exec)
            ClientScriptOpCode.DETAILCANSET_WATERDETAIL -> detailCanSetWaterDetail(exec)
            ClientScriptOpCode.DETAILCANSET_ANTIALIASING -> detailCanSetAntialiasing(exec)
            ClientScriptOpCode.DETAILCANSET_PARTICLES -> detailCanSetParticles(exec)
            ClientScriptOpCode.DETAILCANSET_BUILDAREA -> detailCanSetBuildArea(exec)
            ClientScriptOpCode.DETAILCANSET_BLOOM -> detailCanSetBloom(exec)
            ClientScriptOpCode.DETAILCANSET_GROUNDBLENDING -> detailCanSetGroundBlending(exec)
            ClientScriptOpCode.DETAILCANSET_TEXTURING -> detailCanSetTexturing(exec)
            ClientScriptOpCode.DETAILCANSET_MAXSCREENSIZE -> detailCanSetMaxScreenSize(exec)
            ClientScriptOpCode.DETAILCANSET_FOG -> detailCanSetFog(exec)
            ClientScriptOpCode.DETAILCANSET_DEFAULTTOOLKIT -> detailCanSetDefaultToolkit(exec)
            ClientScriptOpCode.DETAILCANSET_TOOLKIT_DEFAULT -> detailCanSetToolkitDefault(exec)
            ClientScriptOpCode.DETAILCANSET_SKYDETAIL -> detailCanSetSkyDetail(exec)
            ClientScriptOpCode.SETENTITYOVERHEADIF -> setEntityOverHeadIf(exec)
            ClientScriptOpCode.GETENTITYOVERHEADIF -> getEntityOverHeadIf(exec)
            ClientScriptOpCode.GET_ENTITY_SAY -> getEntitySay(exec)
            ClientScriptOpCode.GET_DISPLAYNAME_WITHEXTRAS -> getDisplayNameWithExtras(exec)
            ClientScriptOpCode.CC_LOADENTITYMODEL -> ccLoadEntityModel(exec)
            ClientScriptOpCode.IF_CREATE -> ifCreate(exec)
            ClientScriptOpCode.GET_NPC_NAME -> getNpcName(exec)
            ClientScriptOpCode.GET_NPC_VISLEVEL -> getNpcVIsLevel(exec)
            ClientScriptOpCode.GET_ENTITY_SCREEN_POSITION -> pushEntityHintPosition(exec)
            ClientScriptOpCode.IF_GET_GAMESCREEN -> ifGetGameScreen(exec)
            ClientScriptOpCode.GET_ENTITY_OVERLAY_HEIGHT -> getEntityOverlayHeight(exec)
            ClientScriptOpCode.GET_NPC_STAT -> getNpcStat(exec)
            ClientScriptOpCode.IS_NPC_ACTIVE -> isNpcActive(exec)
            ClientScriptOpCode.IS_NPC_VISIBLE -> isNpcVisible(exec)
            ClientScriptOpCode.IS_TARGETED_ENTITY -> isTargetedEntity(exec)
            ClientScriptOpCode.IF_DELETEALL -> ifDeleteAll(exec)
            ClientScriptOpCode.NPC_TYPE -> npcType(exec)
            ClientScriptOpCode.GET_LOC_SCREEN_POSITION -> pushGameobjectHintLocation(exec)
            ClientScriptOpCode.GET_OBJ_SCREEN_POSITION -> pushGroundItemHintPosition(exec)
            ClientScriptOpCode.GET_LOC_OVERLAY_HEIGHT -> getLocOverlayHeight(exec)
            ClientScriptOpCode.GET_OBJ_OVERLAY_HEIGHT -> getObjOverlayHeight(exec)
            ClientScriptOpCode.GET_LOC_BOUNDING_BOX -> getLocBoundingBox(exec)
            ClientScriptOpCode.GET_OBJ_BOUNDING_BOX -> getObjBoundingBox(exec)
            ClientScriptOpCode.GET_ENTITY_BOUNDING_BOX -> getEntityBoundingBox(exec)
            ClientScriptOpCode.BUG_REPORT -> bugReport(exec)
            ClientScriptOpCode.ARRAY_SORT -> arraySort(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by DetailCapabilityOps")
        }
    }

    @JvmStatic
    fun getEntityOverlayHeight(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentEntity!!.getIconHeight()
    }

    @JvmStatic
    fun detailCanModToolkitDefault(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.toolkit!!.visible()) 1  else 0
    }

    @JvmStatic
    fun detailCanSetParticles(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.particles!!.canModify(value)
    }

    @JvmStatic
    fun getEntitySay(executor: CS2Executor) {
        val hintDetails: HintDetails? = executor.currentEntity!!.getHintDetails()
        if (hintDetails == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = hintDetails.getText()
        }
    }

    @JvmStatic
    fun detailCanModTexturing(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.textures!!.playingRunescape()) 1  else 0
    }

    @JvmStatic
    fun detailCanModGroundBlending(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.goundBlending!!.playingRunescape()) 1  else 0
    }

    @JvmStatic
    fun detailCanSetSkyDetail(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.skyboxes!!.canModify(value)
    }

    @JvmStatic
    fun detailCanSetCharShadows(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.characterShadows!!.canModify(value)
    }

    @JvmStatic
    fun getEntityBoundingBox(executor: CS2Executor) {
        getBoundingBox(executor.currentEntity as SceneGraphEntity, executor)
    }

    @JvmStatic
    fun loadMemoryMode(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.mapSize!!.allowSelection()) 1  else 0
    }

    @JvmStatic
    fun pushGameobjectHintLocation(executor: CS2Executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentLocation as SceneGraphEntity, executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[0].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[1].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[2].toInt()
    }

    @JvmStatic
    fun ifCreate(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val idHash = executor.intStack[executor.intStackPtr]
        val componentTypeId = executor.intStack[executor.intStackPtr + 1]
        val slotId = executor.intStack[executor.intStackPtr + 2]
        CS2SharedOps.createComponent(executor.currentEntity!!.inter!!, idHash and 0xffff, componentTypeId, slotId, executor.hasIntOperand, executor)
    }

    @JvmStatic
    fun npcType(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as NpcEntity).definitions!!.type
    }

    @JvmStatic
    fun detailCanSetGroundBlending(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.goundBlending!!.canModify(value)
    }

    @JvmStatic
    fun bugReport(executor: CS2Executor) {
        val bugType = executor.intStack[--executor.intStackPtr]
        executor.stringStackPtr -= 2
        val title: String? = executor.stringStack[executor.stringStackPtr] as String?
        val description: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (title!!.length <= 500 && description!!.length <= 500) {
            PacketEncoder.reportBug(bugType, title!!, description!!)
        }
    }

    @JvmStatic
    fun detailCanModBloom(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.bloom!!.visible() && Renderers.activeToolkit!!.hasBloomSupport()) 1 else 0
    }

    @JvmStatic
    fun isNpcActive(executor: CS2Executor) {
        val npc: NpcEntity? = executor.currentEntity as NpcEntity
        var visible = false
        var npcType: NPCType? = npc!!.definitions
        if (npcType!!.alternativeIds != null) {
            npcType = npcType!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
        }
        if (npcType != null) {
            visible = npcType!!.visible
        }
        executor.intStack[++executor.intStackPtr - 1] = if (visible) 1  else 0
    }

    @JvmStatic
    fun getNpcVIsLevel(executor: CS2Executor) {
        val componentId = executor.intStack[--executor.intStackPtr]
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (target!!.load(executor.currentEntity!!.inter, componentId)) 1 else 0
    }

    @JvmStatic
    fun detailCanModAntialiasing(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.antiAliasing!!.visible() && Renderers.activeToolkit!!.hasAntiAliasing()) 1 else 0
    }

    @JvmStatic
    fun detailCanModParticles(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.particles!!.supported()) 1  else 0
    }

    @JvmStatic
    fun detailCanModWaterDetail(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.water!!.display()) 1  else 0
    }

    @JvmStatic
    fun getObjOverlayHeight(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentGroundItem!!.getIconHeight()
    }

    @JvmStatic
    fun ifGetGameScreen(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.GAME_SCREEN_INTERFACE == null) -1 else Client.GAME_SCREEN_INTERFACE!!.idHash
    }

    @JvmStatic
    fun getNpcStat(executor: CS2Executor) {
        val statIndex = executor.intStack[--executor.intStackPtr]
        val npc: NpcEntity? = executor.currentEntity as NpcEntity
        val maxStat = npc!!.getStat(statIndex)
        val currentStat = npc!!.getCurrentStat(statIndex)
        executor.intStack[++executor.intStackPtr - 1] = maxStat
        executor.intStack[++executor.intStackPtr - 1] = currentStat
    }

    @JvmStatic
    fun pushGroundItemHintPosition(executor: CS2Executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentGroundItem!! as SceneGraphEntity, executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[0].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[1].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[2].toInt()
    }

    @JvmStatic
    fun detailCanSetBloom(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (!Renderers.activeToolkit!!.hasBloomSupport()) {
            executor.intStack[++executor.intStackPtr - 1] = 3
        } else {
            executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.bloom!!.canModify(value)
        }
    }

    @JvmStatic
    fun detailCanSetSpotShadows(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.sceneryShadows!!.canModify(value)
    }

    @JvmStatic
    fun setEntityOverHeadIf(executor: CS2Executor) {
        executor.currentEntity!!.setOverheadInterfaceId(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun getLocBoundingBox(executor: CS2Executor) {
        getBoundingBox(executor.currentLocation!! as SceneGraphEntity, executor)
    }

    @JvmStatic
    fun pushEntityHintPosition(executor: CS2Executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentEntity!! as SceneGraphEntity, executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[0].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[1].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[2].toInt()
    }

    @JvmStatic
    fun ccLoadEntityModel(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val componentId = executor.intStack[executor.intStackPtr]
        val unusedArg = executor.intStack[executor.intStackPtr + 1]
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (target!!.load(executor.currentEntity!!.inter, componentId)) 1 else 0
    }

    @JvmStatic
    fun getDisplayNameWithExtras(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = (executor.currentEntity as PlayerEntity).getUsernameWithTitle()
    }

    @JvmStatic
    fun detailCanSetMaxScreenSize(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.maxScreenSize!!.canModify(value)
    }

    @JvmStatic
    fun detailCanSetAntialiasing(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (!Renderers.activeToolkit!!.hasAntiAliasing()) {
            executor.intStack[++executor.intStackPtr - 1] = 3
        } else {
            executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasing!!.canModify(value)
        }
    }

    @JvmStatic
    fun detailCanSetWaterDetail(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.water!!.canModify(value)
    }

    @JvmStatic
    fun detailCanSetToolkitDefault(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.toolkit!!.canModify(value)
    }

    @JvmStatic
    fun isTargetedEntity(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.ENTITY_TARGET == executor.currentEntity!!.getCS2Index()) 1 else 0
    }

    @JvmStatic
    fun detailCanModFog(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.fog!!.visible()) 1  else 0
    }

    @JvmStatic
    fun detailCanModCharShadows(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.characterShadows!!.visible()) 1  else 0
    }

    @JvmStatic
    fun getObjBoundingBox(executor: CS2Executor) {
        getBoundingBox(executor.currentGroundItem!! as SceneGraphEntity, executor)
    }

    @JvmStatic
    fun detailCanModDefaultToolkit(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.defaultToolkit!!.visible()) 1  else 0
    }

    @JvmStatic
    fun getEntityOverHeadIf(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentEntity!!.loadedInterfaceId
    }

    @JvmStatic
    fun detailCanSetDefaultToolkit(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.defaultToolkit!!.canModify(value)
    }

    @JvmStatic
    fun getNpcName(executor: CS2Executor) {
        val npc: NpcEntity? = executor.currentEntity as NpcEntity
        var name: String? = npc!!.modifiedName
        var npcType: NPCType? = npc!!.definitions
        if (npcType!!.alternativeIds != null) {
            npcType = npcType!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
            if (npcType == null) {
                name = ""
            } else {
                name = npcType!!.name
            }
        }
        if (name == null) {
            name = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = name
    }

    @JvmStatic
    fun detailCanSetBuildArea(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.mapSize!!.canModify(value)
    }

    @JvmStatic
    fun isNpcVisible(executor: CS2Executor) {
        val npc: NpcEntity? = executor.currentEntity as NpcEntity
        var npcType: NPCType? = npc!!.definitions
        if (npcType!!.alternativeIds != null) {
            npcType = npcType!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
        }
        executor.intStack[++executor.intStackPtr - 1] = if (npcType != null) 1 else 0
    }

    @JvmStatic
    fun getLocOverlayHeight(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentLocation as SceneGraphEntity?)!!.getIconHeight()
    }

    @JvmStatic
    fun detailCanSetTexturing(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.textures!!.canModify(value)
    }

    @JvmStatic
    fun detailCanModSkyDetail(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.skyboxes!!.display()) 1  else 0
    }

    @JvmStatic
    fun ifDeleteAll(executor: CS2Executor) {
        val component: Component? = executor.currentEntity!!.inter!!.getComponent(executor.intStack[--executor.intStackPtr])
        component!!.slotChildren = null
        component!!.itemSlots = null
        Component.redrawComponent(component!!)
    }

    @JvmStatic
    fun detailCanModGroundDecor(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.groundDecoration!!.playingRunescape()) 1  else 0
    }

    @JvmStatic
    fun detailCanModMaxScreenSize(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.maxScreenSize!!.visible()) 1  else 0
    }

    @JvmStatic
    fun detailCanSetFog(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.fog!!.canModify(value)
    }

    @JvmStatic
    fun detailCanModSpotShadows(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.sceneryShadows!!.enabled()) 1  else 0
    }

    @JvmStatic
    fun detailCanSetGroundDecor(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.groundDecoration!!.canModify(value)
    }

    @JvmStatic
    fun arraySort(executor: CS2Executor) {
        val count = executor.intStack[executor.intStackPtr - 3]
        val keyArrayId = executor.intStack[executor.intStackPtr - 2]
        val valueArrayId = executor.intStack[executor.intStackPtr - 1]
        executor.intStackPtr -= 3
        if (count > executor.globalArrayLengths[keyArrayId]) {
            throw RuntimeException()
        } else if (count > executor.globalArrayLengths[valueArrayId]) {
            throw RuntimeException()
        } else if (keyArrayId == valueArrayId) {
            throw RuntimeException()
        } else {
            ArrayUtils.quicksort(executor.globalArrays[keyArrayId], executor.globalArrays[valueArrayId], 0, count - 1)
        }
    }

    @JvmStatic
    fun getBoundingBox(sceneObject: SceneGraphEntity, executor: CS2Executor) {
        var found = false
        var minX = 0
        var minY = 0
        var maxX = 0
        var maxY = 0
        if (sceneObject!!.boundingBoxPolygons != null) {
            for (index in 0 until sceneObject!!.boundingBoxPolygons!!.size) {
                val box: BoundingBoxPolygon? = sceneObject!!.boundingBoxPolygons!![index]
                if (box!!.positionCalculated) {
                    var left: Int = 0
                    var right: Int = 0
                    if (box!!.right < box!!.left) {
                        left = box!!.right - box!!.size
                        right = box!!.size + box!!.left
                    } else {
                        left = box!!.left - box!!.size
                        right = box!!.right + box!!.size
                    }
                    var bottom: Int = 0
                    var top: Int = 0
                    if (box!!.top < box!!.bottom) {
                        bottom = box!!.top - box!!.size
                        top = box!!.size + box!!.bottom
                    } else {
                        bottom = box!!.bottom - box!!.size
                        top = box!!.top + box!!.size
                    }
                    if (!found || left < minX) {
                        minX = left
                    }
                    if (!found || bottom < minY) {
                        minY = bottom
                    }
                    if (!found || right > maxX) {
                        maxX = right
                    }
                    if (!found || top > maxY) {
                        maxY = top
                    }
                    found = true
                }
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = if (found) 1  else 0
        executor.intStack[++executor.intStackPtr - 1] = minX
        executor.intStack[++executor.intStackPtr - 1] = minY
        executor.intStack[++executor.intStackPtr - 1] = maxX
        executor.intStack[++executor.intStackPtr - 1] = maxY
    }

}
