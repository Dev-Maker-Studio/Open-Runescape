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

object InterfaceSetterOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.IF_SETPOSITION -> ifSetPosition(exec)
            ClientScriptOpCode.IF_SETSIZE -> ifSetSize(exec)
            ClientScriptOpCode.IF_SETHIDE -> hideComp(exec)
            ClientScriptOpCode.IF_SETASPECT -> ifSetAspect(exec)
            ClientScriptOpCode.IF_SETNOCLICKTHROUGH -> ifSetNoClickThrough(exec)
            ClientScriptOpCode.IF_SETSCROLLPOS -> ifSetScrollPos(exec)
            ClientScriptOpCode.IF_SETCOLOUR -> ifSetColour(exec)
            ClientScriptOpCode.IF_SETFILL -> ifSetFill(exec)
            ClientScriptOpCode.IF_SETTRANS -> setCompTransparency(exec)
            ClientScriptOpCode.IF_SETLINEWID -> ifSetLineWid(exec)
            ClientScriptOpCode.IF_SETGRAPHIC -> setCompSprite(exec)
            ClientScriptOpCode.IF_SET2DANGLE -> ifSet2DAngle(exec)
            ClientScriptOpCode.IF_SETTILING -> ifSetTiling(exec)
            ClientScriptOpCode.IF_SETMODEL -> ifSetModel(exec)
            ClientScriptOpCode.IF_SETMODELANGLE -> ifSetModelAngle(exec)
            ClientScriptOpCode.IF_SETMODELANIM -> animateComp(exec)
            ClientScriptOpCode.IF_SETMODELORTHOG -> ifSetModelOrthog(exec)
            ClientScriptOpCode.IF_SETMODELTINT -> ifSetModelTint(exec)
            ClientScriptOpCode.IF_SETTEXT -> setCompText(exec)
            ClientScriptOpCode.IF_SETTEXTFONT -> ifSetfont(exec)
            ClientScriptOpCode.IF_SETTEXTALIGN -> setIfTextAlign(exec)
            ClientScriptOpCode.IF_SETTEXTSHADOW -> ifSetTextShadow(exec)
            ClientScriptOpCode.IF_SETTEXTANTIMACRO -> ifSetTextAntimacro(exec)
            ClientScriptOpCode.IF_SETOUTLINE -> ifSetOutline(exec)
            ClientScriptOpCode.IF_SETGRAPHICSHADOW -> ifSetGraphicShadow(exec)
            ClientScriptOpCode.IF_SETVFLIP -> ifSetVFlip(exec)
            ClientScriptOpCode.IF_SETHFLIP -> ifSetHFlip(exec)
            ClientScriptOpCode.IF_SETSCROLLSIZE -> ifSetScrollSize(exec)
            ClientScriptOpCode.IF_SETALPHA -> ifSetAlpha(exec)
            ClientScriptOpCode.IF_SETMODELZOOM -> ifSetModelZoom(exec)
            ClientScriptOpCode.IF_SETLINEDIRECTION -> ifSetLineDirection(exec)
            ClientScriptOpCode.IF_SETMODELORIGIN -> ifSetModelOrigin(exec)
            ClientScriptOpCode.IF_SETMAXLINES -> ifSetMaxLines(exec)
            ClientScriptOpCode.IF_SETPARAM_INT -> ifSetParamInt(exec)
            ClientScriptOpCode.IF_SETPARAM_STRING -> ifSetParamString(exec)
            ClientScriptOpCode.IF_SETVIDEO_GRAPHIC -> ifSetVideoGraphic(exec)
            ClientScriptOpCode.IF_SETVIDEO_TEXT -> ifSetVideoText(exec)
            ClientScriptOpCode.IF_SETRECOL -> ifSetRecol(exec)
            ClientScriptOpCode.IF_SETRETEX -> ifSetRetex(exec)
            ClientScriptOpCode.IF_SETFONTMONO -> ifSetfontMono(exec)
            ClientScriptOpCode.IF_SETCLICKMASK -> ifSetClickmask(exec)
            ClientScriptOpCode.IF_SETOBJECT -> ifSetItem(exec)
            ClientScriptOpCode.IF_SETNPCHEAD -> ifSetNpcHead(exec)
            ClientScriptOpCode.IF_SETPLAYERHEAD_SELF -> ifSetPlayerHeadSelf(exec)
            ClientScriptOpCode.IF_SETNPCMODEL -> ifSetNpcModel(exec)
            ClientScriptOpCode.IF_SETPLAYERMODEL -> ifSetPlayerModel(exec)
            ClientScriptOpCode.IF_SETOBJECT_NONUM -> ifSetItemNoNum(exec)
            ClientScriptOpCode.IF_SETMODELLIGHTING -> ifSetModelLighting(exec)
            ClientScriptOpCode.IF_SETMODELLIGHTING_SUNROTATION -> ifSetModelLightingSunRotation(exec)
            ClientScriptOpCode.IF_SETOBJECT_WEARCOL -> ifSetItemWearCol(exec)
            ClientScriptOpCode.IF_SETOBJECT_WEARCOL_NONUM -> ifSetObjectWearColNoNum(exec)
            ClientScriptOpCode.IF_SETMODEL_ITEMCONTAINER -> ifSetModelItemContainer(exec)
            ClientScriptOpCode.IF_SETPLAYERMODEL_SELF -> ifSetPlayerModelSelf(exec)
            ClientScriptOpCode.IF_SETOBJECT_ALWAYSNUM -> ifSetItemAlwaysNum(exec)
            ClientScriptOpCode.IF_SETOBJECT_WEARCOL_ALWAYSNUM -> ifSetObjectWearColAlwaysNum(exec)
            ClientScriptOpCode.IF_SETOP -> ifSetop(exec)
            ClientScriptOpCode.IF_SETDRAGGABLE -> ifSetDraggable(exec)
            ClientScriptOpCode.IF_SETDRAGRENDERBEHAVIOUR -> ifSetDragRenderBehaviour(exec)
            ClientScriptOpCode.IF_SETDRAGDEADZONE -> ifSetDragDeadZone(exec)
            ClientScriptOpCode.IF_SETDRAGDEADTIME -> ifSetDragDeadTime(exec)
            ClientScriptOpCode.IF_SETOPBASE -> ifSetopBase(exec)
            ClientScriptOpCode.IF_SETTARGETVERB -> ifSetTargetVerb(exec)
            ClientScriptOpCode.IF_CLEAROPS -> ifClearOps(exec)
            ClientScriptOpCode.IF_SETTARGETCURSORS -> ifSetTargetCursors(exec)
            ClientScriptOpCode.IF_SETOPCURSOR -> ifSetopCursor(exec)
            ClientScriptOpCode.IF_SETPAUSETEXT -> ifSetPauseText(exec)
            ClientScriptOpCode.IF_SETTARGETOPCURSOR -> ifSetTargetOpCursor(exec)
            ClientScriptOpCode.IF_SETOPCHAR -> ifSetopChar(exec)
            ClientScriptOpCode.IF_SETOPTCHAR -> ifSetopTChar(exec)
            ClientScriptOpCode.IF_SETOPKEY -> ifSetopKey(exec)
            ClientScriptOpCode.IF_SETOPTKEY -> ifSetopTKey(exec)
            ClientScriptOpCode.IF_SETMOUSEOVERCURSOR -> ifSetMouseOverCursor(exec)
            ClientScriptOpCode.IF_SETONCLICK -> ifSetOnClick(exec)
            ClientScriptOpCode.IF_SETONHOLD -> ifSetOnHold(exec)
            ClientScriptOpCode.IF_SETONRELEASE -> ifSetOnRelease(exec)
            ClientScriptOpCode.IF_SETONMOUSEOVER -> ifSetOnMouseOver(exec)
            ClientScriptOpCode.IF_SETONMOUSELEAVE -> ifSetOnMouseLeave(exec)
            ClientScriptOpCode.IF_SETONDRAG -> ifSetOnDrag(exec)
            ClientScriptOpCode.IF_SETONTARGETLEAVE -> ifSetOnTargetLeave(exec)
            ClientScriptOpCode.IF_SETONVARTRANSMIT -> ifSetOnVarTransmit(exec)
            ClientScriptOpCode.IF_SETONTIMER -> ifSetOnTimer(exec)
            ClientScriptOpCode.IF_SETONOP -> ifSetOnOp(exec)
            ClientScriptOpCode.IF_SETONDRAGCOMPLETE -> ifSetOnDragComplete(exec)
            ClientScriptOpCode.IF_SETONCLICKREPEAT -> ifSetOnClickRepeat(exec)
            ClientScriptOpCode.IF_SETONMOUSEREPEAT -> ifSetOnMouseRepeat(exec)
            ClientScriptOpCode.IF_SETONINVTRANSMIT -> ifSetOnInvTransmit(exec)
            ClientScriptOpCode.IF_SETONSTATTRANSMIT -> ifSetOnStatTransmit(exec)
            ClientScriptOpCode.IF_SETONTARGETENTER -> ifSetOnTargetEnter(exec)
            ClientScriptOpCode.IF_SETONSCROLLWHEEL -> ifSetOnScrollWheel(exec)
            ClientScriptOpCode.IF_SETONCHATTRANSMIT -> ifSetOnChatTransmit(exec)
            ClientScriptOpCode.IF_SETONKEY -> ifSetOnKey(exec)
            ClientScriptOpCode.IF_SETONFRIENDTRANSMIT -> ifSetOnFriendTransmit(exec)
            ClientScriptOpCode.IF_SETONCLANTRANSMIT -> ifSetOnClanTransmit(exec)
            ClientScriptOpCode.IF_SETONMISCTRANSMIT -> ifSetOnMiscTransmit(exec)
            ClientScriptOpCode.IF_SETONDIALOGABORT -> ifSetOnDialogAbort(exec)
            ClientScriptOpCode.IF_SETONSUBCHANGE -> ifSetOnSubchange(exec)
            ClientScriptOpCode.IF_SETONSTOCKTRANSMIT -> ifSetOnStocktransmit(exec)
            ClientScriptOpCode.IF_SETONCAMFINISHED -> ifSetOnCamfinished(exec)
            ClientScriptOpCode.IF_SETONRESIZE -> ifSetOnResize(exec)
            ClientScriptOpCode.IF_SETONVARCTRANSMIT -> ifSetOnVarcTransmit(exec)
            ClientScriptOpCode.IF_SETONVARCSTRTRANSMIT -> ifSetOnVarcStrTransmit(exec)
            ClientScriptOpCode.IF_SETONOPT -> ifSetOnOpt(exec)
            ClientScriptOpCode.IF_SETONCLANSETTINGSTRANSMIT -> ifSetOnClanSettingsTransmit(exec)
            ClientScriptOpCode.IF_SETONCLANCHANNELTRANSMIT -> ifSetOnClanChannelTransmit(exec)
            ClientScriptOpCode.IF_SETONVARCLANTRANSMIT -> ifSetOnVarClanTransmit(exec)
            ClientScriptOpCode.IF_CLEARSCRIPTHOOKS -> ifClearscripthooks(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by InterfaceSetterOps")
        }
    }

    @JvmStatic
    fun ifSetParamInt(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setParamInt(component!!, executor)
    }

    @JvmStatic
    fun ifSetopChar(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        CS2SharedOps.setOpChar(component!!, executor)
    }

    @JvmStatic
    fun ifSetopTKey(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOpTKey(component!!, executor)
    }

    @JvmStatic
    fun ifSetScrollSize(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setScrollSize(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetColour(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.ifSetColor(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetNpcModel(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setNpcModel(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetFill(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setFill(component!!, executor)
    }

    @JvmStatic
    fun ifSetLineWid(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setLineWid(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnTargetEnter(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnTargetEnter(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnVarcStrTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnScrollWheel(component!!, executor)
    }

    @JvmStatic
    fun ifSetop(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOp(component!!, executor)
    }

    @JvmStatic
    fun ifSetNpcHead(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.ifSetNPCHead(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifClearOps(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.clearOps(component!!)
    }

    @JvmStatic
    fun hideComp(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setHide(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetScrollPos(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setScrollPos(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetModelItemContainer(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelItemContainer(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetVFlip(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setVFlip(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnCamfinished(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnCamfinished(component!!, executor)
    }

    @JvmStatic
    fun ifSetTextAntimacro(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setTextAntimacro(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnClick(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnClick(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnTargetLeave(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnOp(component!!, executor)
    }

    @JvmStatic
    fun ifSetTargetOpCursor(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setTargetOpCursor(component!!, executor)
    }

    @JvmStatic
    fun ifSetVideoGraphic(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setVideoSprite(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetModelLighting(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelLighting(component!!, executor)
    }

    @JvmStatic
    fun ifSetPosition(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setComponentPosition(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetPauseText(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setResumeText(component!!, executor)
    }

    @JvmStatic
    fun ifSetClickmask(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setClickmask(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetAlpha(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setAlpha(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnOp(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnOpt(component!!, executor)
    }

    @JvmStatic
    fun ifSetMouseOverCursor(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setMouseOverCursor(component!!, executor)
    }

    @JvmStatic
    fun ifSetDragDeadTime(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setDragDeadTime(component!!, executor)
    }

    @JvmStatic
    fun setCompSprite(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setGraphic(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetopBase(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setOpBase(component!!, executor)
    }

    @JvmStatic
    fun ifSetPlayerHeadSelf(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setPlayerHeadSelf(component!!, parentInterface!!)
    }

    @JvmStatic
    fun ifSetRecol(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setRecol(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnDialogAbort(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnDialogAbort(component!!, executor)
    }

    @JvmStatic
    fun ifSetVideoText(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setVideoText(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun setIfTextAlign(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setComponentTextAlign(component!!, executor)
    }

    @JvmStatic
    fun ifSetObjectWearColAlwaysNum(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, true, Component.StackMode.ALWAYS_NUM, executor)
    }

    @JvmStatic
    fun ifSetDraggable(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setDepthChild(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnHold(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnHold(component!!, executor)
    }

    @JvmStatic
    fun ifSetModelOrigin(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setComponentModelOrigin(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnKey(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnKey(component!!, executor)
    }

    @JvmStatic
    fun ifSetopTChar(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        CS2SharedOps.setOpTChar(component!!, executor)
    }

    @JvmStatic
    fun ifSetMaxLines(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setMaxLines(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnInvTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnMouseLeave(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnClanSettingsTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnClanSettingsTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetAspect(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setComponentAspect(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOutline(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setOutline(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnStatTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnStatTransmit(component!!, executor)
    }

    @JvmStatic
    fun animateComp(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelAnim(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun setCompTransparency(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setTransparency(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnVarTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnVarTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetSize(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setComponentSize(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnMiscTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnMiscTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetfont(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setIfFont(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetModelOrthog(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelOrthog(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnMouseRepeat(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnMouseOver(component!!, executor)
    }

    @JvmStatic
    fun ifSetLineDirection(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setLineDirection(component!!, executor)
    }

    @JvmStatic
    fun ifSetTextShadow(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setTextShadow(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnOpt(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnTargetLeave(component!!, executor)
    }

    @JvmStatic
    fun ifSetModelTint(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelTint(component!!, executor)
    }

    @JvmStatic
    fun ifSetPlayerModel(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setPlayerModel(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnRelease(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnRelease(component!!, executor)
    }

    @JvmStatic
    fun ifSetDragRenderBehaviour(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setDragRenderBehaviour(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnMouseLeave(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnMouseExit(component!!, executor)
    }

    @JvmStatic
    fun ifSetRetex(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setRetex(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnTimer(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnTimer(component!!, executor)
    }

    @JvmStatic
    fun ifSetModelLightingSunRotation(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelLightingSunRotation(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnVarClanTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnVarClanTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetItemNoNum(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, false, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun ifSetModelAngle(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelAngle(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetModelZoom(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setModelZoom(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnClanChannelTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnClanChannelTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetTargetVerb(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setTargetVerb(component!!, executor)
    }

    @JvmStatic
    fun ifSetopKey(executor: CS2Executor) {
        executor.intStackPtr -= 3
        @Suppress("UNCHECKED_CAST")
        val keyCodes: ByteArray? = executor.globalArrays[executor.intStack[executor.intStackPtr]] as ByteArray?
        @Suppress("UNCHECKED_CAST")
        val keyModifiers: ByteArray? = executor.globalArrays[executor.intStack[executor.intStackPtr + 1]] as ByteArray?
        val idHash = executor.intStack[executor.intStackPtr + 2]
        val component: Component? = Component.fromHash(idHash)
        CS2SharedOps.setOpKey(component!!, keyCodes, keyModifiers, executor)
    }

    @JvmStatic
    fun ifSetPlayerModelSelf(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setPlayerModelSelf(component!!, parentInterface!!)
    }

    @JvmStatic
    fun ifSetItem(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, false, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun ifSetOnDragComplete(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnDragComplete(component!!, executor)
    }

    @JvmStatic
    fun ifSetModel(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setComponentModel(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnMouseOver(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnMouseEnter(component!!, executor)
    }

    @JvmStatic
    fun ifSetHFlip(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setHFlip(component!!, executor)
    }

    @JvmStatic
    fun ifSetTiling(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setTiling(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnChatTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnChatTransmit(component!!, executor)
    }

    @JvmStatic
    fun setCompText(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setComponentText(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnClanTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnFriendsChatTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifClearscripthooks(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.clearScriptHooks(component!!)
    }

    @JvmStatic
    fun ifSetObjectWearColNoNum(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, true, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun ifSetOnVarcTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnInvTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetParamString(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setParamString(component!!, executor)
    }

    @JvmStatic
    fun ifSet2DAngle(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.set2DAngle(component!!, executor)
    }

    @JvmStatic
    fun ifSetNoClickThrough(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setNoClickThrough(component!!, executor)
    }

    @JvmStatic
    fun ifSetfontMono(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setFontMono(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ifSetOnClickRepeat(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnClickRepeat(component!!, executor)
    }

    @JvmStatic
    fun ifSetGraphicShadow(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setGraphicShadow(component!!, executor)
    }

    @JvmStatic
    fun ifSetItemWearCol(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, true, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun ifSetItemAlwaysNum(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, false, Component.StackMode.ALWAYS_NUM, executor)
    }

    @JvmStatic
    fun ifSetOnFriendTransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnFriendTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnSubchange(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnSubchange(component!!, executor)
    }

    @JvmStatic
    fun ifSetTargetCursors(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setTargetCursors(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnDrag(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnDrag(component!!, executor)
    }

    @JvmStatic
    fun ifSetopCursor(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOpCursor(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnStocktransmit(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnStockTransmit(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnResize(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnResize(component!!, executor)
    }

    @JvmStatic
    fun ifSetOnScrollWheel(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        CS2SharedOps.setOnMouseRepeat(component!!, executor)
    }

    @JvmStatic
    fun ifSetDragDeadZone(executor: CS2Executor) {
        val idHash = executor.intStack[--executor.intStackPtr]
        val component: Component? = Component.fromHash(idHash)
        val parentInterface: Interface? = Interface.INTERFACES!![(idHash shr 16)]
        assert(component != null)
        CS2SharedOps.setDragDeadZone(component!!, executor)
    }

}
