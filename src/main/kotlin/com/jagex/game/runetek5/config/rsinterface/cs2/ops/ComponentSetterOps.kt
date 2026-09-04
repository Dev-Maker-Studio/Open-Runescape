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

object ComponentSetterOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.CC_SETPOSITION -> ccSetPosition(exec)
            ClientScriptOpCode.CC_SETSIZE -> ccSetSize(exec)
            ClientScriptOpCode.CC_SETHIDE -> ccSetHide(exec)
            ClientScriptOpCode.CC_SETASPECT -> ccSetAspect(exec)
            ClientScriptOpCode.CC_SETNOCLICKTHROUGH -> ccSetNoClickThrough(exec)
            ClientScriptOpCode.CC_SETSCROLLPOS -> ccSetScrollPos(exec)
            ClientScriptOpCode.CC_SETCOLOUR -> ccSetColour(exec)
            ClientScriptOpCode.CC_SETFILL -> ccSetFill(exec)
            ClientScriptOpCode.CC_SETTRANS -> ccSetTrans(exec)
            ClientScriptOpCode.CC_SETLINEWID -> ccSetLineWid(exec)
            ClientScriptOpCode.CC_SETGRAPHIC -> ccSetGraphic(exec)
            ClientScriptOpCode.CC_SET2DANGLE -> ccSet2DAngle(exec)
            ClientScriptOpCode.CC_SETTILING -> ccSetTiling(exec)
            ClientScriptOpCode.CC_SETMODEL -> ccSetModel(exec)
            ClientScriptOpCode.CC_SETMODELANGLE -> ccSetModelAngle(exec)
            ClientScriptOpCode.CC_SETMODELANIM -> ccSetModelAnim(exec)
            ClientScriptOpCode.CC_SETMODELORTHOG -> ccSetModelOrthog(exec)
            ClientScriptOpCode.CC_SETMODELTINT -> ccSetModelTint(exec)
            ClientScriptOpCode.CC_SETTEXT -> setCCText(exec)
            ClientScriptOpCode.CC_SETTEXTFONT -> setCCFont(exec)
            ClientScriptOpCode.CC_SETTEXTALIGN -> setCCTextAlign(exec)
            ClientScriptOpCode.CC_SETTEXTSHADOW -> ccSetTextShadow(exec)
            ClientScriptOpCode.CC_SETTEXTANTIMACRO -> ccSetTextAntimacro(exec)
            ClientScriptOpCode.CC_SETOUTLINE -> ccSetOutline(exec)
            ClientScriptOpCode.CC_SETGRAPHICSHADOW -> ccSetGraphicShadow(exec)
            ClientScriptOpCode.CC_SETVFLIP -> ccSetVFlip(exec)
            ClientScriptOpCode.CC_SETHFLIP -> ccSetHFlip(exec)
            ClientScriptOpCode.CC_SETSCROLLSIZE -> ccSetScrollSize(exec)
            ClientScriptOpCode.CC_SETALPHA -> ccSetAlpha(exec)
            ClientScriptOpCode.CC_SETMODELZOOM -> ccSetModelZoom(exec)
            ClientScriptOpCode.CC_SETLINEDIRECTION -> ccSetLineDirection(exec)
            ClientScriptOpCode.CC_SETMODELORIGIN -> ccSetModelOrigin(exec)
            ClientScriptOpCode.CC_SETMAXLINES -> ccSetMaxLines(exec)
            ClientScriptOpCode.CC_SETPARAM_INT -> ccSetParamInt(exec)
            ClientScriptOpCode.CC_SETPARAM_STRING -> ccSetParamString(exec)
            ClientScriptOpCode.instr6050 -> ccSetVideoSprite(exec)
            ClientScriptOpCode.instr6051 -> ccSetVideoText(exec)
            ClientScriptOpCode.CC_SETRECOL -> ccSetRecol(exec)
            ClientScriptOpCode.CC_SETRETEX -> ccSetRetex(exec)
            ClientScriptOpCode.CC_SETFONTMONO -> ccSetfontMono(exec)
            ClientScriptOpCode.CC_SETPARAM -> ccSetParam(exec)
            ClientScriptOpCode.CC_SETCLICKMASK -> ccSetClickmask(exec)
            ClientScriptOpCode.CC_SETOBJECT -> ccSetItem(exec)
            ClientScriptOpCode.CC_SETNPCHEAD -> ccSetNpcHead(exec)
            ClientScriptOpCode.CC_SETPLAYERHEAD_SELF -> ccSetPlayerHeadSelf(exec)
            ClientScriptOpCode.CC_SETNPCMODEL -> ccSetNpcModel(exec)
            ClientScriptOpCode.CC_SETPLAYERMODEL -> ccSetPlayerModel(exec)
            ClientScriptOpCode.CC_SETOBJECT_NONUM -> ccSetItemNoNum(exec)
            ClientScriptOpCode.CC_SETMODELLIGHTING -> ccSetModelLighting(exec)
            ClientScriptOpCode.CC_SETMODELLIGHTING_SUNROTATION -> ccSetModelLightingSunRotation(exec)
            ClientScriptOpCode.CC_SETOBJECT_WEARCOL -> ccSetItemWearCol(exec)
            ClientScriptOpCode.CC_SETOBJECT_WEARCOL_NONUM -> ccSetItemWearColNoNum(exec)
            ClientScriptOpCode.instr6895 -> ccSetModelItemContainer(exec)
            ClientScriptOpCode.CC_SETPLAYERMODEL_SELF -> ccSetPlayerModelSelf(exec)
            ClientScriptOpCode.CC_SETOBJECT_ALWAYSNUM -> ccSetItemAlwaysNum(exec)
            ClientScriptOpCode.CC_SETOBJECT_WEARCOL_ALWAYSNUM -> ccSetItemWearColAlwaysNum(exec)
            ClientScriptOpCode.CC_SETOP -> ccSetop(exec)
            ClientScriptOpCode.CC_SETDEPTHCHILD -> ccSetDepthChild(exec)
            ClientScriptOpCode.CC_SETDRAGRENDERBEHAVIOUR -> ccSetDragRenderBehaviour(exec)
            ClientScriptOpCode.CC_SETDRAGDEADZONE -> ccSetDragDeadZone(exec)
            ClientScriptOpCode.CC_SETDRAGDEADTIME -> ccSetDragDeadTime(exec)
            ClientScriptOpCode.CC_SETOPBASE -> ccSetOpBase(exec)
            ClientScriptOpCode.CC_SETTARGETVERB -> ccSetTargetVerb(exec)
            ClientScriptOpCode.CC_CLEAROPS -> ccClearOps(exec)
            ClientScriptOpCode.CC_SETTARGETCURSORS -> ccSetTargetCursors(exec)
            ClientScriptOpCode.CC_SETOPCURSOR -> ccSetopCursor(exec)
            ClientScriptOpCode.CC_SETRESUMETEXT -> ccSetResumeText(exec)
            ClientScriptOpCode.CC_SETTARGETOPCURSOR -> ccSetTargetOpCursor(exec)
            ClientScriptOpCode.CC_SETOPCHAR -> ccSetopChar(exec)
            ClientScriptOpCode.CC_SETOPTCHAR -> ccSetopTChar(exec)
            ClientScriptOpCode.CC_SETOPKEY -> ccSetopKey(exec)
            ClientScriptOpCode.CC_SETOPTKEY -> ccSetopTKey(exec)
            ClientScriptOpCode.CC_SETMOUSEOVERCURSOR -> ccSetMouseOverCursor(exec)
            ClientScriptOpCode.CC_SETONCLICK -> ccSetOnClick(exec)
            ClientScriptOpCode.CC_SETONHOLD -> ccSetOnHold(exec)
            ClientScriptOpCode.CC_SETONRELEASE -> ccSetOnRelease(exec)
            ClientScriptOpCode.CC_HOOK_MOUSE_ENTER -> ccHookMouseEnter(exec)
            ClientScriptOpCode.CC_HOOK_MOUSE_EXIT -> ccHookMouseExit(exec)
            ClientScriptOpCode.CC_SETONDRAG -> ccSetOnDrag(exec)
            ClientScriptOpCode.CC_SETONOP -> ccSetOnOp(exec)
            ClientScriptOpCode.CC_SETONVARTRANSMIT -> ccSetOnVarTransmit(exec)
            ClientScriptOpCode.CC_SETONTIMER -> ccSetOnTimer(exec)
            ClientScriptOpCode.CC_SETONOPT -> ccSetOnOpt(exec)
            ClientScriptOpCode.CC_SETONDRAGCOMPLETE -> ccSetOnDragComplete(exec)
            ClientScriptOpCode.CC_SETONCLICKREPEAT -> ccSetOnClickRepeat(exec)
            ClientScriptOpCode.CC_SETONMOUSEOVER -> ccSetOnMouseOver(exec)
            ClientScriptOpCode.CC_SETONMOUSELEAVE -> ccSetOnMouseLeave(exec)
            ClientScriptOpCode.CC_SETONSTATTRANSMIT -> ccSetOnStatTransmit(exec)
            ClientScriptOpCode.CC_SETONTARGETENTER -> ccSetOnTargetEnter(exec)
            ClientScriptOpCode.CC_SETONMOUSEREPEAT -> ccSetOnMouseRepeat(exec)
            ClientScriptOpCode.CC_SETONCHATTRANSMIT -> ccSetOnChatTransmit(exec)
            ClientScriptOpCode.CC_SETONKEY -> ccSetOnKey(exec)
            ClientScriptOpCode.CC_SETONFRIENDTRANSMIT -> ccSetOnFriendTransmit(exec)
            ClientScriptOpCode.CC_SETONCLANCHANNELTRANSMIT -> ccSetOnClanchannelTransmit(exec)
            ClientScriptOpCode.CC_SETONMISCTRANSMIT -> ccSetOnMiscTransmit(exec)
            ClientScriptOpCode.CC_SETONDIALOGABORT -> ccSetOnDialogAbort(exec)
            ClientScriptOpCode.CC_SETONSUBCHANGE -> ccSetOnSubchange(exec)
            ClientScriptOpCode.CC_SETONSTOCKTRANSMIT -> ccSetOnStocktransmit(exec)
            ClientScriptOpCode.CC_SETONCAMFINISHED -> ccSetOnCamfinished(exec)
            ClientScriptOpCode.CC_SETONRESIZE -> ccSetOnResize(exec)
            ClientScriptOpCode.CC_SETONINVTRANSMIT -> ccSetOnInvTransmit(exec)
            ClientScriptOpCode.CC_SETONSCROLLWHEEL -> ccSetOnScrollWheel(exec)
            ClientScriptOpCode.CC_SETONTARGETLEAVE -> ccSetOnTargetLeave(exec)
            ClientScriptOpCode.CC_SETONCLANSETTINGSTRANSMIT -> ccSetOnClanSettingsTransmit(exec)
            ClientScriptOpCode.CC_SETLINKACTIVECLANCHANNEL -> ccSetLinkActiveClanChannel(exec)
            ClientScriptOpCode.CC_SETONVARCLANTRANSMIT -> ccSetOnVarClanTransmit(exec)
            ClientScriptOpCode.CC_CLEARSCRIPTHOOKS -> ccClearscripthooks(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by ComponentSetterOps")
        }
    }

    @JvmStatic
    fun ccSetOnClanSettingsTransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnClanSettingsTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetOpBase(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOpBase(component!!, executor)
    }

    @JvmStatic
    fun ccSetTextAntimacro(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setTextAntimacro(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun setCCFont(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setIfFont(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetOnHold(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnHold(component!!, executor)
    }

    @JvmStatic
    fun ccSetModelTint(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelTint(component!!, executor)
    }

    @JvmStatic
    fun setCCText(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setComponentText(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetClickmask(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setClickmask(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetTextShadow(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setTextShadow(component!!, executor)
    }

    @JvmStatic
    fun ccSetLinkActiveClanChannel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnClanChannelTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnSubchange(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnSubchange(component!!, executor)
    }

    @JvmStatic
    fun ccClearscripthooks(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.clearScriptHooks(component!!)
    }

    @JvmStatic
    fun ccSetItem(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, false, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun ccSetop(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOp(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnRelease(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnRelease(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnTargetLeave(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnTargetLeave(component!!, executor)
    }

    @JvmStatic
    fun ccHookMouseExit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnMouseExit(component!!, executor)
    }

    @JvmStatic
    fun ccSetAspect(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setComponentAspect(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetPlayerModel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setPlayerModel(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetNpcHead(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.ifSetNPCHead(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetVideoSprite(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setVideoSprite(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetScrollSize(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setScrollSize(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetAlpha(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setAlpha(component!!, executor)
    }

    @JvmStatic
    fun ccSetModelOrigin(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setComponentModelOrigin(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnCamfinished(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnCamfinished(component!!, executor)
    }

    @JvmStatic
    fun ccSetParam(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStackPtr -= 2
        val paramId = executor.intStack[executor.intStackPtr]
        val paramValue = executor.intStack[executor.intStackPtr + 1]
        val paramType: ParamType? = TypeLists.PARAMS!!.list(paramId)
        if (paramValue != paramType!!.defaultInt) {
            component!!.setIntParam(paramId, paramValue)
        } else {
            component!!.removeParam(paramId)
        }
    }

    @JvmStatic
    fun ccSetLineWid(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setLineWid(component!!, executor)
    }

    @JvmStatic
    fun ccSetRetex(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setRetex(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetFill(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setFill(component!!, executor)
    }

    @JvmStatic
    fun ccSetMouseOverCursor(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setMouseOverCursor(component!!, executor)
    }

    @JvmStatic
    fun ccSetItemWearColNoNum(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, true, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun ccSetModelLightingSunRotation(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelLightingSunRotation(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnClanchannelTransmit(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnFriendsChatTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetTrans(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setTransparency(component!!, executor)
    }

    @JvmStatic
    fun ccSetItemWearCol(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, true, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun ccSetGraphic(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setGraphic(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetopKey(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        executor.intStackPtr -= 10
        var keyCodes: ByteArray? = null
        var keyModifiers: ByteArray? = null
        var value: Int = 0
        while (value < 10 && executor.intStack[value + executor.intStackPtr] >= 0) {
            value += 2
        }
        if (value > 0) {
            keyCodes = ByteArray(value / 2)
            keyModifiers = ByteArray(value / 2)
            value -= 2
            while (value >= 0) {
                keyCodes[value / 2] = executor.intStack[value + executor.intStackPtr].toByte()
                keyModifiers[value / 2] = executor.intStack[value + executor.intStackPtr + 1].toByte()
                value -= 2
            }
        }
        CS2SharedOps.setOpKey(component!!, keyCodes, keyModifiers, executor)
    }

    @JvmStatic
    fun ccSetPlayerHeadSelf(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setPlayerHeadSelf(component!!, parentInterface!!)
    }

    @JvmStatic
    fun ccSetModel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setComponentModel(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetModelOrthog(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelOrthog(component!!, executor)
    }

    @JvmStatic
    fun ccSetVideoText(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setVideoText(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetParamString(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setParamString(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnOp(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnOp(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnChatTransmit(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnChatTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetHFlip(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setHFlip(component!!, executor)
    }

    @JvmStatic
    fun ccSetOutline(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOutline(component!!, executor)
    }

    @JvmStatic
    fun ccSetModelLighting(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelLighting(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnOpt(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnOpt(component!!, executor)
    }

    @JvmStatic
    fun setCCTextAlign(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setComponentTextAlign(component!!, executor)
    }

    @JvmStatic
    fun ccSetDragDeadZone(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setDragDeadZone(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnTargetEnter(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnTargetEnter(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnStatTransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnStatTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetModelAnim(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelAnim(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetOnInvTransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnInvTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetHide(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setHide(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetColour(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.ifSetColor(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetLineDirection(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setLineDirection(component!!, executor)
    }

    @JvmStatic
    fun ccSetDepthChild(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setDepthChild(component!!, executor)
    }

    @JvmStatic
    fun ccSetNoClickThrough(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setNoClickThrough(component!!, executor)
    }

    @JvmStatic
    fun ccSetSize(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setComponentSize(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetOnDrag(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnDrag(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnFriendTransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnFriendTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetDragRenderBehaviour(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setDragRenderBehaviour(component!!, executor)
    }

    @JvmStatic
    fun ccSet2DAngle(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.set2DAngle(component!!, executor)
    }

    @JvmStatic
    fun ccSetMaxLines(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setMaxLines(component!!, executor)
    }

    @JvmStatic
    fun ccSetopChar(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        CS2SharedOps.setOpChar(component!!, executor)
    }

    @JvmStatic
    fun ccSetGraphicShadow(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setGraphicShadow(component!!, executor)
    }

    @JvmStatic
    fun ccSetResumeText(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setResumeText(component!!, executor)
    }

    @JvmStatic
    fun ccSetfontMono(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setFontMono(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetTargetVerb(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setTargetVerb(component!!, executor)
    }

    @JvmStatic
    fun ccSetopTKey(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOpTKey(component!!, executor)
    }

    @JvmStatic
    fun ccSetPosition(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setComponentPosition(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetOnResize(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnResize(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnVarClanTransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnVarClanTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnMouseRepeat(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnMouseRepeat(component!!, executor)
    }

    @JvmStatic
    fun ccSetopCursor(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOpCursor(component!!, executor)
    }

    @JvmStatic
    fun ccSetParamInt(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setParamInt(component!!, executor)
    }

    @JvmStatic
    fun ccSetNpcModel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setNpcModel(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetModelAngle(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelAngle(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetOnClick(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnClick(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnDragComplete(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnDragComplete(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnKey(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnKey(component!!, executor)
    }

    @JvmStatic
    fun ccSetModelItemContainer(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelItemContainer(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetOnMouseOver(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnMouseOver(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnVarTransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnVarTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnStocktransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnStockTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnMiscTransmit(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnMiscTransmit(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnMouseLeave(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnMouseLeave(component!!, executor)
    }

    @JvmStatic
    fun ccSetItemNoNum(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, false, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun ccHookMouseEnter(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnMouseEnter(component!!, executor)
    }

    @JvmStatic
    fun ccClearOps(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.clearOps(component!!)
    }

    @JvmStatic
    fun ccSetPlayerModelSelf(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setPlayerModelSelf(component!!, parentInterface!!)
    }

    @JvmStatic
    fun ccSetScrollPos(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setScrollPos(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetTiling(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setTiling(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnScrollWheel(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnScrollWheel(component!!, executor)
    }

    @JvmStatic
    fun ccSetopTChar(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        CS2SharedOps.setOpTChar(component!!, executor)
    }

    @JvmStatic
    fun ccSetVFlip(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setVFlip(component!!, executor)
    }

    @JvmStatic
    fun ccSetItemWearColAlwaysNum(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, true, Component.StackMode.ALWAYS_NUM, executor)
    }

    @JvmStatic
    fun ccSetRecol(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setRecol(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetOnDialogAbort(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnDialogAbort(component!!, executor)
    }

    @JvmStatic
    fun ccSetModelZoom(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setModelZoom(component!!, parentInterface!!, executor)
    }

    @JvmStatic
    fun ccSetTargetOpCursor(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setTargetOpCursor(component!!, executor)
    }

    @JvmStatic
    fun ccSetTargetCursors(executor: CS2Executor) {
        var target: CS2Interface? = null
        if (executor.hasIntOperand) {
            target = executor.hookedInterface2
        } else {
            target = executor.hookedInterface1
        }
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setTargetCursors(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnClickRepeat(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnClickRepeat(component!!, executor)
    }

    @JvmStatic
    fun ccSetDragDeadTime(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setDragDeadTime(component!!, executor)
    }

    @JvmStatic
    fun ccSetOnTimer(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setOnTimer(component!!, executor)
    }

    @JvmStatic
    fun ccSetItemAlwaysNum(executor: CS2Executor) {
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        val component: Component? = target!!.comp
        val parentInterface: Interface? = target!!.inter
        CS2SharedOps.setItemIFComp(component!!, parentInterface!!, false, Component.StackMode.ALWAYS_NUM, executor)
    }

}
