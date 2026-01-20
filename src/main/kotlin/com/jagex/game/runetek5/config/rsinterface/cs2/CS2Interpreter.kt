package com.jagex.game.runetek5.config.rsinterface.cs2

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

object CS2Interpreter {

    @JvmField
    val anIntArray428: IntArray = intArrayOf(0, 1, 2, 3, 4, 5, 6, 14)

    @JvmField
    val anIntArray422: IntArray = intArrayOf(7, 8, 9, 10, 11, 12, 13, 15)

    @JvmField
    var lastEnteredGraphicsMode: Int = 0

    @JvmField
    var CS2_QUERY_RESULTS_LEN: Int = 0

    @JvmStatic
    fun executeOperation(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.PUSH_INT -> pushInt(exec)
            ClientScriptOpCode.LOAD_VARP -> loadVarp(exec)
            ClientScriptOpCode.STORE_VARP -> storeVarp(exec)
            ClientScriptOpCode.PUSH_STRING -> pushString(exec)
            ClientScriptOpCode.GOTO -> gotoInstr(exec)
            ClientScriptOpCode.INT_NE -> intNotEqual(exec)
            ClientScriptOpCode.INT_EQ -> intEqual(exec)
            ClientScriptOpCode.INT_LT -> intLessThan(exec)
            ClientScriptOpCode.INT_GT -> intGreaterThan(exec)
            ClientScriptOpCode.GET_VARP_OLD -> getVarpOld(exec)
            ClientScriptOpCode.GET_VARPBIT_OLD -> getVarpbitOld(exec)
            ClientScriptOpCode.GET_VARN_OLD -> getVarnOld(exec)
            ClientScriptOpCode.GET_VARNBIT_OLD -> getVarnbitOld(exec)
            ClientScriptOpCode.RETURN -> returnInstr(exec)
            ClientScriptOpCode.LOAD_VARPBIT -> loadVarpbit(exec)
            ClientScriptOpCode.STORE_VARPBIT -> storeVarpbit(exec)
            ClientScriptOpCode.INT_LE -> intLessOrEqual(exec)
            ClientScriptOpCode.INT_GE -> intGreaterOrEqual(exec)
            ClientScriptOpCode.LOAD_INT -> loadInt(exec)
            ClientScriptOpCode.STORE_INT -> storeInt(exec)
            ClientScriptOpCode.LOAD_STRING -> loadString(exec)
            ClientScriptOpCode.STORE_STRING -> storeString(exec)
            ClientScriptOpCode.MERGE_STRINGS -> mergeStrings(exec)
            ClientScriptOpCode.POP_INT -> popInt(exec)
            ClientScriptOpCode.POP_STRING -> popString(exec)
            ClientScriptOpCode.CALL_CS2 -> callCS2Script(exec)
            ClientScriptOpCode.LOAD_VARC -> loadVarc(exec)
            ClientScriptOpCode.STORE_VARC -> storeVarc(exec)
            ClientScriptOpCode.ARRAY_NEW -> arrayNew(exec)
            ClientScriptOpCode.ARRAY_LOAD -> arrayLoad(exec)
            ClientScriptOpCode.ARRAY_STORE -> arrayStore(exec)
            ClientScriptOpCode.LOAD_VARC_STRING -> loadVarcString(exec)
            ClientScriptOpCode.STORE_VARC_STRING -> storeVarcString(exec)
            ClientScriptOpCode.SWITCH -> switchInstr(exec)
            ClientScriptOpCode.PUSH_LONG -> pushLong(exec)
            ClientScriptOpCode.POP_LONG -> popLong(exec)
            ClientScriptOpCode.LOAD_LONG -> loadLong(exec)
            ClientScriptOpCode.STORE_LONG -> storeLong(exec)
            ClientScriptOpCode.LONG_NE -> longNotEqual(exec)
            ClientScriptOpCode.LONG_EQ -> longEqual(exec)
            ClientScriptOpCode.LONG_LT -> longLessThan(exec)
            ClientScriptOpCode.LONG_GT -> longGreaterThan(exec)
            ClientScriptOpCode.LONG_LE -> longLessOrEqual(exec)
            ClientScriptOpCode.LONG_GE -> longGreaterOrEqual(exec)
            ClientScriptOpCode.BRANCH_EQ1 -> branchEq1(exec)
            ClientScriptOpCode.BRANCH_EQ0 -> branchEq0(exec)
            ClientScriptOpCode.LOAD_CLAN_VAR -> loadClanVar(exec)
            ClientScriptOpCode.LOAD_CLAN_VARBIT -> loadClanVarbit(exec)
            ClientScriptOpCode.LOAD_CLAN_VAR_LONG -> loadClanVarLong(exec)
            ClientScriptOpCode.LOAD_CLAN_VAR_STRING -> loadClanVarString(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VAR -> loadClanSettingVar(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VARBIT -> loadClanSettingVarbit(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VAR_LONG -> loadClanSettingVarLong(exec)
            ClientScriptOpCode.LOAD_CLAN_SETTING_VAR_STRING -> loadClanSettingVarString(exec)
            ClientScriptOpCode.CC_CREATE -> ccCreate(exec)
            ClientScriptOpCode.CC_DELETE -> ccDelete(exec)
            ClientScriptOpCode.CC_DELETEALL -> clearCompChildren(exec)
            ClientScriptOpCode.CC_SETCHILD_SLOT -> method6668(exec)
            ClientScriptOpCode.CC_SETCHILD -> method2620(exec)
            ClientScriptOpCode.IF_SENDTOFRONT -> ifSetFront(true, exec)
            ClientScriptOpCode.IF_SENDTOBACK -> ifSetFront(false, exec)
            ClientScriptOpCode.CC_SENDTOFRONT -> ccSetFront(true, exec)
            ClientScriptOpCode.CC_SENDTOBACK -> ccSetFront(false, exec)
            ClientScriptOpCode.IF_RESUME_PAUSEBUTTON -> ifResumePauseButton(exec)
            ClientScriptOpCode.CC_RESUME_PAUSEBUTTON -> ccResumePauseButton(exec)
            ClientScriptOpCode.BASE_IDKIT -> setBaseIdentiKit(exec)
            ClientScriptOpCode.BASE_COLOR -> setBaseColor(exec)
            ClientScriptOpCode.SET_GENDER -> setGender(exec)
            ClientScriptOpCode.SET_ITEM -> setItem(exec)
            ClientScriptOpCode.CC_SETPOSITION -> ccSetPosition(exec)
            ClientScriptOpCode.CC_SETSIZE -> ccSetSize(exec)
            ClientScriptOpCode.CC_SETHIDE -> ccSetHide(exec)
            ClientScriptOpCode.CC_SETASPECT -> ccSetAspect(exec)
            ClientScriptOpCode.CC_SETNOCLICKTHROUGH -> ccSetNoClickThrough(exec)
            ClientScriptOpCode.CC_SETSCROLLPOS -> ccSetScrollPos(exec)
            ClientScriptOpCode.CC_SETCOLOR -> method5314(exec)
            ClientScriptOpCode.CC_SETFILL -> method3918(exec)
            ClientScriptOpCode.CC_SETTRANS -> method5487(exec)
            ClientScriptOpCode.CC_SETLINEWID -> method6687(exec)
            ClientScriptOpCode.CC_SETGRAPHIC -> method6071(exec)
            ClientScriptOpCode.CC_SET2DANGLE -> method5332(exec)
            ClientScriptOpCode.CC_SETTILING -> method5774(exec)
            ClientScriptOpCode.CC_SETMODEL -> method8327(exec)
            ClientScriptOpCode.CC_SETMODELANGLE -> method6364(exec)
            ClientScriptOpCode.CC_SETMODELANIM -> method8726(exec)
            ClientScriptOpCode.CC_SETMODELORTHOG -> method15523(exec)
            ClientScriptOpCode.CC_SETMODELTINT -> method5099(exec)
            ClientScriptOpCode.CC_SETTEXT -> setCCText(exec)
            ClientScriptOpCode.CC_SETTEXTFONT -> setCCFont(exec)
            ClientScriptOpCode.CC_SETTEXTALIGN -> setCCTextAlign(exec)
            ClientScriptOpCode.CC_SETTEXTSHADOW -> method12659(exec)
            ClientScriptOpCode.CC_SETTEXTANTIMACRO -> method8714(exec)
            ClientScriptOpCode.CC_SETOUTLINE -> method4965(exec)
            ClientScriptOpCode.CC_SETGRAPHICSHADOW -> method1497(exec)
            ClientScriptOpCode.CC_SETVFLIP -> method1569(exec)
            ClientScriptOpCode.CC_SETHFLIP -> method15448(exec)
            ClientScriptOpCode.CC_SETSCROLLSIZE -> method7929(exec)
            ClientScriptOpCode.CC_SETALPHA -> method7933(exec)
            ClientScriptOpCode.CC_SETMODELZOOM -> method12116(exec)
            ClientScriptOpCode.CC_SETLINEDIRECTION -> method1259(exec)
            ClientScriptOpCode.CC_SETMODELORIGIN -> method893(exec)
            ClientScriptOpCode.CC_SETMAXLINES -> method1949(exec)
            ClientScriptOpCode.CC_SETPARAM_INT -> method3031(exec)
            ClientScriptOpCode.CC_SETPARAM_STRING -> method4558(exec)
            ClientScriptOpCode.instr6050 -> method6210(exec)
            ClientScriptOpCode.instr6051 -> method8344(exec)
            ClientScriptOpCode.CC_SETRECOL -> method11380(exec)
            ClientScriptOpCode.CC_SETRETEX -> method6688(exec)
            ClientScriptOpCode.CC_SETFONTMONO -> method12846(exec)
            ClientScriptOpCode.CC_SETPARAM -> method8753(exec)
            ClientScriptOpCode.CC_SETCLICKMASK -> method5833(exec)
            ClientScriptOpCode.CC_SETITEM -> ccSetItem(exec)
            ClientScriptOpCode.CC_SETNPCHEAD -> method3356(exec)
            ClientScriptOpCode.CC_SETPLAYERHEAD_SELF -> method6802(exec)
            ClientScriptOpCode.CC_SETNPCMODEL -> method8212(exec)
            ClientScriptOpCode.CC_SETPLAYERMODEL -> method6283(exec)
            ClientScriptOpCode.CC_SETITEM_NONUM -> ccSetItemNoNum(exec)
            ClientScriptOpCode.CC_SETMODELLIGHTING -> method4970(exec)
            ClientScriptOpCode.instr6064 -> method2567(exec)
            ClientScriptOpCode.CC_SETITEM_WEARCOL -> ccSetItemWearCol(exec)
            ClientScriptOpCode.CC_SETITEM_WEARCOL_NONUM -> ccSetItemWearColNoNum(exec)
            ClientScriptOpCode.instr6895 -> method6510(exec)
            ClientScriptOpCode.CC_SETPLAYERMODEL_SELF -> method4381(exec)
            ClientScriptOpCode.CC_SETITEM_ALWAYSNUM -> ccSetItemAlwaysNum(exec)
            ClientScriptOpCode.CC_SETITEM_WEARCOL_ALWAYSNUM -> ccSetItemWearColAlwaysNum(exec)
            ClientScriptOpCode.CC_SETOP -> method6452(exec)
            ClientScriptOpCode.instr6676 -> ccSetDepthChild(exec)
            ClientScriptOpCode.instr6073 -> method2169(exec)
            ClientScriptOpCode.instr6443 -> method2965(exec)
            ClientScriptOpCode.instr6075 -> method4801(exec)
            ClientScriptOpCode.CC_SETOPBASE -> ccSetOpBase(exec)
            ClientScriptOpCode.CC_SETTARGETVERB -> ccSetTargetVerb(exec)
            ClientScriptOpCode.CC_CLEAROPS -> method8298(exec)
            ClientScriptOpCode.CC_SETTARGETCURSORS -> method6150(exec)
            ClientScriptOpCode.CC_SETOPCURSOR -> method5203(exec)
            ClientScriptOpCode.CC_SETRESUMETEXT -> method541(exec)
            ClientScriptOpCode.CC_SETTARGETOPCURSOR -> ccSetTargetOpCursor(exec)
            ClientScriptOpCode.CC_SETOPCHAR -> method6305(exec)
            ClientScriptOpCode.CC_SETOPTCHAR -> method5512(exec)
            ClientScriptOpCode.CC_SETOPKEY -> method4200(exec)
            ClientScriptOpCode.CC_SETOPTKEY -> method15416(exec)
            ClientScriptOpCode.CC_SETMOUSEOVERCURSOR -> method6776(exec)
            ClientScriptOpCode.instr6088 -> method1606(exec)
            ClientScriptOpCode.instr5957 -> method3892(exec)
            ClientScriptOpCode.instr6450 -> method4030(exec)
            ClientScriptOpCode.instr6091 -> method8285(exec)
            ClientScriptOpCode.instr6092 -> method5008(exec)
            ClientScriptOpCode.instr6224 -> method6120(exec)
            ClientScriptOpCode.instr6094 -> method1631(exec)
            ClientScriptOpCode.instr6095 -> method5454(exec)
            ClientScriptOpCode.instr6556 -> method14521(exec)
            ClientScriptOpCode.instr6687 -> method4971(exec)
            ClientScriptOpCode.instr6499 -> method14746(exec)
            ClientScriptOpCode.instr6084 -> method4797(exec)
            ClientScriptOpCode.CC_SETONMOUSEOVER -> method260(exec)
            ClientScriptOpCode.CC_SETONMOUSELEAVE -> method6043(exec)
            ClientScriptOpCode.instr6899 -> method8723(exec)
            ClientScriptOpCode.instr6103 -> method485(exec)
            ClientScriptOpCode.instr6765 -> method12499(exec)
            ClientScriptOpCode.instr6370 -> method6395(exec)
            ClientScriptOpCode.instr6106 -> method12802(exec)
            ClientScriptOpCode.instr6580 -> method1084(exec)
            ClientScriptOpCode.instr6437 -> method5296(exec)
            ClientScriptOpCode.instr6903 -> method16095(exec)
            ClientScriptOpCode.instr6099 -> method3561(exec)
            ClientScriptOpCode.instr6111 -> method3676(exec)
            ClientScriptOpCode.instr6112 -> method5004(exec)
            ClientScriptOpCode.instr6113 -> method14340(exec)
            ClientScriptOpCode.instr6492 -> method3041(exec)
            ClientScriptOpCode.instr6096 -> method7270(exec)
            ClientScriptOpCode.instr6116 -> method5775(exec)
            ClientScriptOpCode.instr6054 -> method4283(exec)
            ClientScriptOpCode.instr6782 -> method3546(exec)
            ClientScriptOpCode.CC_SETLINKACTIVECLANCHANNEL -> method741(exec)
            ClientScriptOpCode.instr6120 -> method3370(exec)
            ClientScriptOpCode.instr6121 -> method6800(exec)
            ClientScriptOpCode.instr6552 -> method8863(exec)
            ClientScriptOpCode.instr6772 -> method6796(exec)
            ClientScriptOpCode.instr6124 -> method7032(exec)
            ClientScriptOpCode.instr6125 -> method5598(exec)
            ClientScriptOpCode.CC_ISHIDDEN -> method892(exec)
            ClientScriptOpCode.instr6462 -> method13491(exec)
            ClientScriptOpCode.instr6128 -> method3168(exec)
            ClientScriptOpCode.instr6762 -> method7219(exec)
            ClientScriptOpCode.instr6130 -> method11125(exec)
            ClientScriptOpCode.instr6131 -> method5123(exec)
            ClientScriptOpCode.instr6132 -> method7502(exec)
            ClientScriptOpCode.instr6133 -> method4901(exec)
            ClientScriptOpCode.instr6134 -> method14578(exec)
            ClientScriptOpCode.CC_GETSPRITESCALE -> method11249(exec)
            ClientScriptOpCode.CC_GETSPRITEPITCH -> method12925(exec)
            ClientScriptOpCode.CC_GETSPRITEYAW -> method8211(exec)
            ClientScriptOpCode.instr6670 -> method6236(exec)
            ClientScriptOpCode.CC_GETTRANSPARENCY -> method8735(exec)
            ClientScriptOpCode.instr6005 -> method11350(exec)
            ClientScriptOpCode.instr6262 -> method1498(exec)
            ClientScriptOpCode.instr6928 -> method6314(exec)
            ClientScriptOpCode.instr6771 -> method1134(exec)
            ClientScriptOpCode.instr6144 -> method12723(exec)
            ClientScriptOpCode.instr6230 -> method6449(exec)
            ClientScriptOpCode.instr6566 -> method15559(exec)
            ClientScriptOpCode.instr6842 -> method7676(exec)
            ClientScriptOpCode.instr6148 -> method8154(exec)
            ClientScriptOpCode.CC_CONTAINERITEMID -> method4277(exec)
            ClientScriptOpCode.instr6150 -> method4565(exec)
            ClientScriptOpCode.instr6151 -> method4802(exec)
            ClientScriptOpCode.CC_GETOPTIONFLAGS -> method1562(exec)
            ClientScriptOpCode.instr6153 -> method779(exec)
            ClientScriptOpCode.instr6154 -> method1864(exec)
            ClientScriptOpCode.instr6155 -> method401(exec)
            ClientScriptOpCode.instr6662 -> method8859(exec)
            ClientScriptOpCode.instr6157 -> method6138(exec)
            ClientScriptOpCode.IF_SETPOSITION -> ifSetPosition(exec)
            ClientScriptOpCode.IF_SETSIZE -> method2826(exec)
            ClientScriptOpCode.IF_SETHIDE -> hideComp(exec)
            ClientScriptOpCode.IF_SETASPECT -> method283(exec)
            ClientScriptOpCode.IF_SETNOCLICKTHROUGH -> method1167(exec)
            ClientScriptOpCode.IF_SETSCROLLPOS -> method5302(exec)
            ClientScriptOpCode.IF_SETCOLOR -> method15514(exec)
            ClientScriptOpCode.IF_SETFILL -> method3554(exec)
            ClientScriptOpCode.IF_SETTRANS -> setCompTransparency(exec)
            ClientScriptOpCode.IF_SETLINEWID -> method6016(exec)
            ClientScriptOpCode.IF_SETGRAPHIC -> setCompSprite(exec)
            ClientScriptOpCode.IF_SET2DANGLE -> method460(exec)
            ClientScriptOpCode.IF_SETTILING -> method3250(exec)
            ClientScriptOpCode.IF_SETMODEL -> method4311(exec)
            ClientScriptOpCode.IF_SETMODELANGLE -> method3020(exec)
            ClientScriptOpCode.IF_SETMODELANIM -> animateComp(exec)
            ClientScriptOpCode.instr6174 -> method11492(exec)
            ClientScriptOpCode.IF_SETMODELTINT -> method1509(exec)
            ClientScriptOpCode.IF_SETTEXT -> setCompText(exec)
            ClientScriptOpCode.IF_SETTEXTFONT -> ifSetfont(exec)
            ClientScriptOpCode.IF_SETTEXTALIGN -> setIfTextAlign(exec)
            ClientScriptOpCode.instr6170 -> method8018(exec)
            ClientScriptOpCode.instr6180 -> method3064(exec)
            ClientScriptOpCode.instr6181 -> method484(exec)
            ClientScriptOpCode.IF_SETSPRITESHADOW -> method4960(exec)
            ClientScriptOpCode.instr6183 -> method6669(exec)
            ClientScriptOpCode.instr6184 -> method1492(exec)
            ClientScriptOpCode.IF_SETSCROLLSIZE -> method15510(exec)
            ClientScriptOpCode.IF_SETALPHA -> method11252(exec)
            ClientScriptOpCode.instr6187 -> method7208(exec)
            ClientScriptOpCode.instr6188 -> method6738(exec)
            ClientScriptOpCode.instr6377 -> method563(exec)
            ClientScriptOpCode.instr6190 -> method1624(exec)
            ClientScriptOpCode.instr6191 -> method5756(exec)
            ClientScriptOpCode.instr6192 -> method12839(exec)
            ClientScriptOpCode.instr6193 -> method14342(exec)
            ClientScriptOpCode.instr6194 -> method305(exec)
            ClientScriptOpCode.instr6195 -> method4174(exec)
            ClientScriptOpCode.instr6196 -> method7560(exec)
            ClientScriptOpCode.instr6197 -> method3949(exec)
            ClientScriptOpCode.IF_SETCLICKMASK -> method1069(exec)
            ClientScriptOpCode.IF_SETITEM -> ifSetItem(exec)
            ClientScriptOpCode.IF_SETNPCHEAD -> method4863(exec)
            ClientScriptOpCode.IF_SETPLAYERHEAD_SELF -> method11403(exec)
            ClientScriptOpCode.instr6715 -> method2865(exec)
            ClientScriptOpCode.instr6864 -> method1510(exec)
            ClientScriptOpCode.instr6879 -> ifSetItemNoNum(exec)
            ClientScriptOpCode.instr6205 -> method8750(exec)
            ClientScriptOpCode.instr6919 -> method12492(exec)
            ClientScriptOpCode.instr6207 -> ifSetItemWearCol(exec)
            ClientScriptOpCode.instr6208 -> ifSetItemWearColNoNum(exec)
            ClientScriptOpCode.instr6209 -> method11126(exec)
            ClientScriptOpCode.instr6210 -> method3117(exec)
            ClientScriptOpCode.instr6129 -> ifSetItemAlwaysNum(exec)
            ClientScriptOpCode.instr6793 -> ifSetItemWearColAlwaysNum(exec)
            ClientScriptOpCode.instr6213 -> method15475(exec)
            ClientScriptOpCode.instr6214 -> ifSetDepthChild(exec)
            ClientScriptOpCode.instr6215 -> method2150(exec)
            ClientScriptOpCode.instr5969 -> method8208(exec)
            ClientScriptOpCode.instr6217 -> method5488(exec)
            ClientScriptOpCode.instr6569 -> method6310(exec)
            ClientScriptOpCode.instr6219 -> method4633(exec)
            ClientScriptOpCode.instr6220 -> method4864(exec)
            ClientScriptOpCode.instr6221 -> method11373(exec)
            ClientScriptOpCode.instr6222 -> method8152(exec)
            ClientScriptOpCode.instr6223 -> method6141(exec)
            ClientScriptOpCode.instr6888 -> method1385(exec)
            ClientScriptOpCode.instr6136 -> method5758(exec)
            ClientScriptOpCode.instr6226 -> method6851(exec)
            ClientScriptOpCode.instr6477 -> method6938(exec)
            ClientScriptOpCode.instr6228 -> method7520(exec)
            ClientScriptOpCode.instr6229 -> method1349(exec)
            ClientScriptOpCode.HOOK_MOUSE_PRESS -> hookMousePress(exec)
            ClientScriptOpCode.instr6231 -> method11438(exec)
            ClientScriptOpCode.instr6232 -> method4904(exec)
            ClientScriptOpCode.HOOK_MOUSE_ENTER -> hookMouseEnter(exec)
            ClientScriptOpCode.HOOK_MOUSE_EXIT -> hookMouseExit(exec)
            ClientScriptOpCode.instr6235 -> method2071(exec)
            ClientScriptOpCode.instr6527 -> method3357(exec)
            ClientScriptOpCode.instr6237 -> method3940(exec)
            ClientScriptOpCode.instr6342 -> method11246(exec)
            ClientScriptOpCode.instr6239 -> method2564(exec)
            ClientScriptOpCode.instr6240 -> method3596(exec)
            ClientScriptOpCode.instr5973 -> method5511(exec)
            ClientScriptOpCode.IF_SETONMOUSEOVER -> ifSetOnMouseOver(exec)
            ClientScriptOpCode.IF_SETONMOUSELEAVE -> ifSetOnMouseLeave(exec)
            ClientScriptOpCode.instr6393 -> method6762(exec)
            ClientScriptOpCode.instr6376 -> method13408(exec)
            ClientScriptOpCode.instr6246 -> method14640(exec)
            ClientScriptOpCode.instr6247 -> method5000(exec)
            ClientScriptOpCode.instr6248 -> method564(exec)
            ClientScriptOpCode.instr6708 -> method11338(exec)
            ClientScriptOpCode.instr6250 -> method7961(exec)
            ClientScriptOpCode.instr6251 -> method11593(exec)
            ClientScriptOpCode.instr6252 -> method4175(exec)
            ClientScriptOpCode.instr6253 -> method11372(exec)
            ClientScriptOpCode.instr6254 -> method8694(exec)
            ClientScriptOpCode.instr6255 -> method4033(exec)
            ClientScriptOpCode.instr6256 -> method5595(exec)
            ClientScriptOpCode.instr6257 -> method6685(exec)
            ClientScriptOpCode.instr6507 -> method8843(exec)
            ClientScriptOpCode.instr6259 -> method1813(exec)
            ClientScriptOpCode.instr6260 -> method2855(exec)
            ClientScriptOpCode.IF_SETLINKACTIVECLANCHANNEL -> method4629(exec)
            ClientScriptOpCode.instr6898 -> method6372(exec)
            ClientScriptOpCode.IF_CLEARSCRIPTHOOKS -> method6040(exec)
            ClientScriptOpCode.IF_GETX -> method4146(exec)
            ClientScriptOpCode.IF_GETY -> method6151(exec)
            ClientScriptOpCode.IF_GETWIDTH -> getCompWidth(exec)
            ClientScriptOpCode.IF_GETHEIGHT -> getCompHeight(exec)
            ClientScriptOpCode.IF_GETHIDE -> getCompHidden(exec)
            ClientScriptOpCode.IF_GETLAYER -> method5045(exec)
            ClientScriptOpCode.IF_GETPARENTLAYER -> method8220(exec)
            ClientScriptOpCode.IF_GETCOLOR -> method454(exec)
            ClientScriptOpCode.IF_GETSCROLLX -> method7422(exec)
            ClientScriptOpCode.IF_GETSCROLLY -> method11473(exec)
            ClientScriptOpCode.IF_GETTEXT -> getCompText(exec)
            ClientScriptOpCode.IF_GETSCROLLWIDTH -> method858(exec)
            ClientScriptOpCode.instr6276 -> method14659(exec)
            ClientScriptOpCode.instr6277 -> method289(exec)
            ClientScriptOpCode.instr6292 -> method2945(exec)
            ClientScriptOpCode.instr6279 -> method6174(exec)
            ClientScriptOpCode.instr6280 -> method1571(exec)
            ClientScriptOpCode.instr6281 -> method11247(exec)
            ClientScriptOpCode.IF_GETOFFSETX -> method5926(exec)
            ClientScriptOpCode.IF_GETOFFSETY -> method6696(exec)
            ClientScriptOpCode.IF_GETSPRITEID -> method6212(exec)
            ClientScriptOpCode.instr6072 -> method3065(exec)
            ClientScriptOpCode.instr6159 -> method1512(exec)
            ClientScriptOpCode.instr6287 -> method14602(exec)
            ClientScriptOpCode.instr6288 -> method7643(exec)
            ClientScriptOpCode.instr6289 -> method6142(exec)
            ClientScriptOpCode.instr6290 -> method15392(exec)
            ClientScriptOpCode.instr6875 -> method4201(exec)
            ClientScriptOpCode.instr6805 -> method2914(exec)
            ClientScriptOpCode.instr6293 -> method4898(exec)
            ClientScriptOpCode.IF_ISOPEN -> method5002(exec)
            ClientScriptOpCode.instr6295 -> method14679(exec)
            ClientScriptOpCode.instr6296 -> method6041(exec)
            ClientScriptOpCode.instr6297 -> method8045(exec)
            ClientScriptOpCode.instr6298 -> method8215(exec)
            ClientScriptOpCode.instr6299 -> method8715(exec)
            ClientScriptOpCode.instr6300 -> method855(exec); //IF_GETCHARINDEXATPOS?
            ClientScriptOpCode.instr6391 -> method6941(exec); //IF_GETCHARPOSATINDEX?
            ClientScriptOpCode.instr6302 -> method6670(exec)
            ClientScriptOpCode.instr6303 -> method1967(exec)
            ClientScriptOpCode.instr6304 -> method14660(exec)
            ClientScriptOpCode.instr6646 -> method4865(exec)
            ClientScriptOpCode.instr6186 -> method3201(exec)
            ClientScriptOpCode.instr6307 -> method5029(exec)
            ClientScriptOpCode.instr6002 -> method3171(exec)
            ClientScriptOpCode.instr6309 -> method12583(exec)
            ClientScriptOpCode.MES -> sendMessage(exec)
            ClientScriptOpCode.RESET_MYPLAYER_ANIMS -> method1836(exec)
            ClientScriptOpCode.IF_CLOSE -> method3939()
            ClientScriptOpCode.RESUME_COUNTDIALOG -> resumeCountDialog(exec)
            ClientScriptOpCode.RESUME_NAMEDIALOG -> resumeNamedDialog(exec)
            ClientScriptOpCode.RESUME_STRINGDIALOG -> resumeStringDialog(exec)
            ClientScriptOpCode.OPPLAYER -> method6194(exec)
            ClientScriptOpCode.IF_DRAGPICKUP -> method870(exec)
            ClientScriptOpCode.CC_DRAGPICKUP -> method5082(exec)
            ClientScriptOpCode.RESUME_ITEMDIALOG -> method12841(exec)
            ClientScriptOpCode.IF_OPENSUBCLIENT -> method1355(exec)
            ClientScriptOpCode.IF_CLOSESUBCLIENT -> method4839(exec)
            ClientScriptOpCode.OPPLAYERT -> method15399(exec)
            ClientScriptOpCode.MES_TYPED -> appendTypedMessage(exec)
            ClientScriptOpCode.SETUP_MESSAGEBOX -> loadMessagebox(exec)
            ClientScriptOpCode.RESUME_HSLDIALOG -> resumeHSLDialog(exec)
            ClientScriptOpCode.RESUME_CLANFORUMQFCDIALOG -> resumeClanForumQFCDialog(exec)
            ClientScriptOpCode.SOUND_SYNTH -> playSoundSynth(exec)
            ClientScriptOpCode.SOUND_SONG -> method3555(exec)
            ClientScriptOpCode.SOUND_JINGLE -> method5027(exec)
            ClientScriptOpCode.SOUND_SYNTH_VOLUME -> method14520(exec)
            ClientScriptOpCode.SOUND_SONG_VOLUME -> method4557(exec)
            ClientScriptOpCode.SOUND_JINGLE_VOLUME -> method11612(exec)
            ClientScriptOpCode.SOUND_VORBIS_VOLUME -> method2823(exec)
            ClientScriptOpCode.SOUND_SPEECH_VOLUME -> method8153(exec)
            ClientScriptOpCode.SOUND_SYNTH_RATE -> method1888(exec)
            ClientScriptOpCode.SOUND_VORBIS_RATE -> method5335(exec)
            ClientScriptOpCode.CLIENTCLOCK -> method456(exec)
            ClientScriptOpCode.INV_GETITEM -> method7675(exec)
            ClientScriptOpCode.INV_GETNUM -> method5197(exec)
            ClientScriptOpCode.INV_TOTAL -> method15389(exec)
            ClientScriptOpCode.INV_SIZE -> method2729(exec)
            ClientScriptOpCode.INV_TOTALCAT -> method16089(exec)
            ClientScriptOpCode.STAT -> method3913(exec)
            ClientScriptOpCode.STAT_BASE -> method11486(exec)
            ClientScriptOpCode.STAT_VISIBLE_XP -> method7444(exec)
            ClientScriptOpCode.COORD -> getMyPlayerPos(exec)
            ClientScriptOpCode.COORDX -> method13057(exec)
            ClientScriptOpCode.COORDY -> method7427(exec)
            ClientScriptOpCode.COORDZ -> method5003(exec)
            ClientScriptOpCode.WORLD_MEMBERS -> method5733(exec)
            ClientScriptOpCode.INVOTHER_GETITEM -> method4825(exec)
            ClientScriptOpCode.INVOTHER_GETNUM -> method6230(exec)
            ClientScriptOpCode.INVOTHER_TOTAL -> method5490(exec)
            ClientScriptOpCode.STAFFMODLEVEL -> staffModLevel(exec)
            ClientScriptOpCode.GET_SYSTEM_UPDATE_TIMER -> getSystemUpdateTimer(exec)
            ClientScriptOpCode.WORLD_ID -> getWorldId(exec)
            ClientScriptOpCode.RUNENERGY_VISIBLE -> getRunEnergy(exec)
            ClientScriptOpCode.RUNWEIGHT_VISIBLE -> getRunWeight(exec)
            ClientScriptOpCode.PLAYERMOD -> method1467(exec)
            ClientScriptOpCode.PLAYERMODLEVEL -> method3033(exec)
            ClientScriptOpCode.PLAYERMEMBER -> getPlayerIsMember(exec)
            ClientScriptOpCode.COMLEVEL_ACTIVE -> getPlayerCombatLevel(exec)
            ClientScriptOpCode.GENDER -> getPlayerGender(exec)
            ClientScriptOpCode.WORLD_QUICKCHAT -> getWorldQuickchatOnly(exec)
            ClientScriptOpCode.CONTAINER_FREE_SPACE -> getContainerFreeSpace(exec)
            ClientScriptOpCode.CONTAINER_TOTAL_PARAM -> containerTotalParam(exec)
            ClientScriptOpCode.CONTAINER_TOTAL_PARAM_STACK -> containerTotalParamStack(exec)
            ClientScriptOpCode.WORLD_LANGUAGE -> getWorldLanguage(exec)
            ClientScriptOpCode.MOVECOORD -> method1613(exec)
            ClientScriptOpCode.AFFILIATE -> method6799(exec)
            ClientScriptOpCode.PROFILE_CPU -> getCPUProfile(exec)
            ClientScriptOpCode.PLAYERDEMO -> method6399(exec)
            ClientScriptOpCode.APPLET_HASFOCUS -> getAppletFocus(exec)
            ClientScriptOpCode.FROMBILLING -> method7699(exec)
            ClientScriptOpCode.GET_MOUSE_X -> getMouseX(exec)
            ClientScriptOpCode.GET_MOUSE_Y -> getMouseY(exec)
            ClientScriptOpCode.GET_ACTIVE_MINIMENU_ENTRY -> method1801(exec)
            ClientScriptOpCode.GET_SECOND_MINIMENU_ENTRY -> method7322(exec)
            ClientScriptOpCode.GET_MINIMENU_LENGTH -> method6121(exec)
            ClientScriptOpCode.GET_CURRENTCURSOR -> method2552(exec)
            ClientScriptOpCode.GET_SELFYANGLE -> method3340(exec)
            ClientScriptOpCode.MAP_ISOWNER -> method13494(exec)
            ClientScriptOpCode.GET_MOUSEBUTTONS -> method4031(exec)
            ClientScriptOpCode.SELF_PLAYER_UID -> method7186(exec)
            ClientScriptOpCode.GET_MINIMENU_TARGET -> method6275(exec)
            ClientScriptOpCode.ENUM_STRING -> method4966(exec)
            ClientScriptOpCode.ENUM -> method5901(exec)
            ClientScriptOpCode.ENUM_HASOUTPUT -> method14503(exec)
            ClientScriptOpCode.ENUM_HASOUTPUT_STRING -> method5776(exec)
            ClientScriptOpCode.ENUM_GETOUTPUTCOUNT -> getEnumSize(exec)
            ClientScriptOpCode.ENUM_GETREVERSECOUNT -> method1853(exec)
            ClientScriptOpCode.ENUM_GETREVERSECOUNT_STRING -> method8757(exec)
            ClientScriptOpCode.ENUM_GETREVERSEINDEX -> method6242(exec)
            ClientScriptOpCode.ENUM_GETREVERSEINDEX_STRING -> method536(exec)
            ClientScriptOpCode.EMAIL_VALIDATION_SUBMIT_CODE -> method11594(exec)
            ClientScriptOpCode.EMAIL_VALIDATION_CHANGE_ADDRESS -> method7501(exec)
            ClientScriptOpCode.EMAIL_VALIDATION_ADD_NEW_ADDRESS -> emailValidationAddNewAddress(exec)
            ClientScriptOpCode.FRIEND_COUNT -> method4773(exec)
            ClientScriptOpCode.FRIEND_GETNAME -> method2096(exec)
            ClientScriptOpCode.FRIEND_GETWORLD -> method5695(exec)
            ClientScriptOpCode.FRIEND_GETRANK -> method3597(exec)
            ClientScriptOpCode.FRIEND_GETWORLDFLAGS -> method11370(exec)
            ClientScriptOpCode.FRIEND_SETRANK -> method1641(exec)
            ClientScriptOpCode.FRIEND_ADD -> method1500(exec)
            ClientScriptOpCode.FRIEND_DEL -> method4278(exec)
            ClientScriptOpCode.IGNORE_ADD -> method1452(exec)
            ClientScriptOpCode.IGNORE_DEL -> method4181(exec)
            ClientScriptOpCode.FRIEND_TEST -> method7932(exec)
            ClientScriptOpCode.FRIEND_GETWORLDNAME -> method2871(exec)
            ClientScriptOpCode.FC_GETCHATDISPLAYNAME -> loadFriendchatName(exec)
            ClientScriptOpCode.FC_GETCHATCOUNT -> method6680(exec)
            ClientScriptOpCode.FC_GETCHATUSERNAME -> method8870(exec)
            ClientScriptOpCode.FC_GETCHATUSERWORLD -> method4905(exec)
            ClientScriptOpCode.FC_GETCHATUSERRANK -> method8756(exec)
            ClientScriptOpCode.FC_GETCHATMINKICK -> method3829(exec)
            ClientScriptOpCode.FC_KICKUSER -> method6152(exec)
            ClientScriptOpCode.FC_GETCHATRANK -> method14491(exec)
            ClientScriptOpCode.FC_JOINCHAT -> method4902(exec)
            ClientScriptOpCode.FC_LEAVECHAT -> sendJoinFriendchatPacket()
            ClientScriptOpCode.IGNORE_COUNT -> method4829(exec)
            ClientScriptOpCode.IGNORE_GETNAME -> method5489(exec)
            ClientScriptOpCode.IGNORE_TEST -> method16124(exec)
            ClientScriptOpCode.FC_ISSELF -> method1608(exec)
            ClientScriptOpCode.FC_GETCHATOWNERNAME -> method13044(exec)
            ClientScriptOpCode.FC_GETCHATUSERWORLDNAME -> method11355(exec)
            ClientScriptOpCode.FRIEND_PLATFORM -> method1625(exec)
            ClientScriptOpCode.FRIEND_GETSLOTFROMNAME -> method1775(exec)
            ClientScriptOpCode.PLAYERCOUNTRY -> method14833(exec)
            ClientScriptOpCode.IGNORE_ADD_TEMP -> method1462(exec)
            ClientScriptOpCode.IGNORE_IS_TEMP -> method3164(exec)
            ClientScriptOpCode.FC_GETCHATUSERNAME_UNFILTERED -> method4566(exec)
            ClientScriptOpCode.IGNORE_GETNAME_UNFILTERED -> method8019(exec)
            ClientScriptOpCode.FRIEND_IS_REFERRER -> method3739(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_FIND_LISTENED -> method8698(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_FIND_AFFINED -> method12881(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETCLANNAME -> method5922(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETALLOWUNAFFINED -> method8701(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETRANKTALK -> method5902(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETRANKKICK -> method1889(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETRANKLOOTSHARE -> method7274(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETCOINSHARE -> method1135(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDCOUNT -> method1802(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDDISPLAYNAME -> method8037(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDRANK -> method2102(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETBANNEDCOUNT -> method1171(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETBANNEDDISPLAYNAME -> method3779(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDEXTRAINFO -> method5362(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETCURRENTOWNER_SLOT -> method933(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETREPLACEMENTOWNER_SLOT -> method4295(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDSLOT -> method3361(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETSORTEDAFFINEDSLOT -> method6205(exec)
            ClientScriptOpCode.UNUSED_CLAN_OP -> method15075(exec)
            ClientScriptOpCode.ACTIVECLANSETTINGS_GETAFFINEDJOINRUNEDAY -> method6741(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_FIND_LISTENED -> activeClanchannelFindListened(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_FIND_AFFINED -> activeClanchannelFindAffined(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETCLANNAME -> method811(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETRANKKICK -> method14644(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETRANKTALK -> method3021(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERCOUNT -> method13410(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERDISPLAYNAME -> method1979(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERRANK -> method6677(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERWORLD -> method3169(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_KICKUSER -> method4968(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETUSERSLOT -> method3043(exec)
            ClientScriptOpCode.ACTIVECLANCHANNEL_GETSORTEDUSERSLOT -> method471(exec)
            ClientScriptOpCode.CLAN_VARS_ENABLED -> clanVarsEnabled(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERTYPE -> method6231(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERITEM -> getGEOfferItem(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERPRICE -> getGEOfferPrice(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERCOUNT -> method3802(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERCOMPLETEDCOUNT -> method7715(exec)
            ClientScriptOpCode.STOCKMARKET_GETOFFERCOMPLETEDGOLD -> method6206(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFEREMPTY -> method6797(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFERSTABLE -> method8716(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFERFINISHED -> method1951(exec)
            ClientScriptOpCode.STOCKMARKET_ISOFFERADDING -> method5297(exec)
            ClientScriptOpCode.ADD -> add(exec)
            ClientScriptOpCode.SUBTRACT -> subtract(exec)
            ClientScriptOpCode.MULTIPLY -> multiply(exec)
            ClientScriptOpCode.DIVIDE -> divide(exec)
            ClientScriptOpCode.RANDOM -> random(exec)
            ClientScriptOpCode.RANDOM_INCLUSIVE -> randomInclusive(exec)
            ClientScriptOpCode.INTERPOLATE -> interpolate(exec)
            ClientScriptOpCode.ADD_PERCENT -> addPercent(exec)
            ClientScriptOpCode.SET_BIT -> setBit(exec)
            ClientScriptOpCode.CLEAR_BIT -> clearBit(exec)
            ClientScriptOpCode.BIT_FLAGGED -> bitFlagged(exec)
            ClientScriptOpCode.MODULO -> modulo(exec)
            ClientScriptOpCode.POW -> pow(exec)
            ClientScriptOpCode.POW_INVERSE -> powInverse(exec)
            ClientScriptOpCode.BIT_AND -> bitAnd(exec)
            ClientScriptOpCode.BIT_OR -> bitOr(exec)
            ClientScriptOpCode.MIN -> min(exec)
            ClientScriptOpCode.MAX -> max(exec)
            ClientScriptOpCode.SCALE -> scale(exec)
            ClientScriptOpCode.RANDOM_SOUND_PITCH -> randSoundPitch(exec)
            ClientScriptOpCode.HSVTORGB -> hsvToRGB(exec)
            ClientScriptOpCode.BIT_NOT -> bitNot(exec)
            ClientScriptOpCode.APPEND_NUM -> strAppendNum(exec)
            ClientScriptOpCode.APPEND -> strAppend(exec)
            ClientScriptOpCode.APPEND_SIGNNUM -> strAppendSigNum(exec)
            ClientScriptOpCode.GET_COL_TAG -> getColTag(exec)
            ClientScriptOpCode.LOWERCASE -> lowerString(exec)
            ClientScriptOpCode.FROMDATE -> fromDate(exec)
            ClientScriptOpCode.TEXT_GENDER -> textGender(exec)
            ClientScriptOpCode.TOSTRING -> toString(exec)
            ClientScriptOpCode.COMPARE -> stringEqual(exec)
            ClientScriptOpCode.PARAHEIGHT -> method3346(exec)
            ClientScriptOpCode.PARAWIDTH -> method5735(exec)
            ClientScriptOpCode.TEXT_SWITCH -> method7556(exec)
            ClientScriptOpCode.ESCAPE -> method5456(exec)
            ClientScriptOpCode.APPEND_CHAR -> method6117(exec)
            ClientScriptOpCode.CHAR_ISPRINTABLE -> method4252(exec)
            ClientScriptOpCode.CHAR_ISALPHANUMERIC -> method8218(exec)
            ClientScriptOpCode.CHAR_ISALPHA -> charIsalpha(exec)
            ClientScriptOpCode.CHAR_ISNUMERIC -> charIsnumeric(exec)
            ClientScriptOpCode.STRING_LENGTH -> stringLength(exec)
            ClientScriptOpCode.SUBSTRING -> substring(exec)
            ClientScriptOpCode.REMOVETAGS -> removetags(exec)
            ClientScriptOpCode.STRING_INDEXOF_CHAR -> method5915(exec)
            ClientScriptOpCode.STRING_INDEXOF_STRING -> method3547(exec)
            ClientScriptOpCode.CHAR_TOLOWERCASE -> method3951(exec)
            ClientScriptOpCode.CHAR_TOUPPERCASE -> method6373(exec)
            ClientScriptOpCode.TOSTRING_LOCALISED -> method6691(exec)
            ClientScriptOpCode.STRINGWIDTH -> method1854(exec)
            ClientScriptOpCode.FORMAT_DATETIME_FROM_MINUTES -> method3830(exec)
            ClientScriptOpCode.CLANFORUMQFC_TOSTRING -> method6139(exec)
            ClientScriptOpCode.ITEM_NAME -> method4247(exec)
            ClientScriptOpCode.ITEM_OP -> method3042(exec)
            ClientScriptOpCode.ITEM_IOP -> method4202(exec)
            ClientScriptOpCode.ITEM_COST -> method6284(exec)
            ClientScriptOpCode.ITEM_STACKABLE -> method11406(exec)
            ClientScriptOpCode.ITEM_CERT -> method12215(exec)
            ClientScriptOpCode.ITEM_UNCERT -> method12724(exec)
            ClientScriptOpCode.ITEM_WEARPOS -> method7445(exec)
            ClientScriptOpCode.ITEM_WEARPOS2 -> method5765(exec)
            ClientScriptOpCode.ITEM_WEARPOS3 -> method6781(exec)
            ClientScriptOpCode.ITEM_MEMBERS -> method5198(exec)
            ClientScriptOpCode.ITEM_PARAM -> getItemParam(exec)
            ClientScriptOpCode.OC_ICURSOR -> method4630(exec)
            ClientScriptOpCode.ITEM_FIND -> method5691(exec)
            ClientScriptOpCode.ITEM_FINDNEXT -> method2868(exec)
            ClientScriptOpCode.OC_FINDRESTART -> method4315()
            ClientScriptOpCode.ITEM_MULTISTACKSIZE -> method3229(exec)
            ClientScriptOpCode.ITEM_FIND_PARAMINT -> method4089(exec)
            ClientScriptOpCode.ITEM_FIND_PARAMSTR -> method6400(exec)
            ClientScriptOpCode.ITEM_MINIMENU_COLOUR_OVERRIDDEN -> method3030(exec)
            ClientScriptOpCode.ITEM_MINIMENU_COLOUR -> method911(exec)
            ClientScriptOpCode.NPC_PARAM -> getNPCParam(exec)
            ClientScriptOpCode.OBJECT_PARAM -> getObjectParam(exec)
            ClientScriptOpCode.STRUCT_PARAM -> getStructParam(exec)
            ClientScriptOpCode.ANIMATION_PARAM -> getAnimationParam(exec)
            ClientScriptOpCode.BAS_GETANIM_READY -> method15408(exec)
            ClientScriptOpCode.UNUSED_LOGIN_GLOBAL_BOOL -> method5755(exec)
            ClientScriptOpCode.UNUSED_PACKET_SEND_STRING -> sendAClass3794624(exec)
            ClientScriptOpCode.UNUSED_PACKET_SET_GLOBAL_BYTE -> method546(exec)
            ClientScriptOpCode.CHAT_GETFILTER_PUBLIC -> setPublicFilter(exec)
            ClientScriptOpCode.CHAT_SETFILTER -> ChatBox.setFilter(exec)
            ClientScriptOpCode.CHAT_SENDABUSEREPORT -> sendReportAbusePacket(exec)
            ClientScriptOpCode.CHAT_GETBYLINE -> method2824(exec)
            ClientScriptOpCode.CHAT_GETTYPEBYLINE -> method15512(exec)
            ClientScriptOpCode.CHAT_GETFILTER_PRIVATE -> setPrivateFilter(exec)
            ClientScriptOpCode.instr6563 -> method6005(exec)
            ClientScriptOpCode.SEND_PUBLIC_CHAT -> method1860(exec)
            ClientScriptOpCode.SEND_PRIVATE_MESSAGE -> method6913(exec)
            ClientScriptOpCode.instr6189 -> method6237(exec)
            ClientScriptOpCode.CHAT_GETCLAN -> method6733(exec)
            ClientScriptOpCode.instr6565 -> method3783(exec)
            ClientScriptOpCode.CHAT_PLAYERNAME -> method8717(exec)
            ClientScriptOpCode.CHAT_GETFILTER_TRADE -> setTradeFilter(exec)
            ClientScriptOpCode.instr6225 -> method5100(exec)
            ClientScriptOpCode.instr6572 -> chatlineGeteffectflags(exec)
            ClientScriptOpCode.instr6573 -> method14835(exec)
            ClientScriptOpCode.instr6574 -> method7771(exec)
            ClientScriptOpCode.instr6682 -> method3803(exec)
            ClientScriptOpCode.CHATLINE_GETCYCLES20MS -> method894(exec)
            ClientScriptOpCode.instr6577 -> method565(exec)
            ClientScriptOpCode.QC_GETNAME -> method12689(exec)
            ClientScriptOpCode.instr6202 -> method742(exec)
            ClientScriptOpCode.instr6030 -> method3780(exec)
            ClientScriptOpCode.instr6581 -> method814(exec)
            ClientScriptOpCode.QC_GETMESSAGE -> method14492(exec)
            ClientScriptOpCode.instr6583 -> method5697(exec)
            ClientScriptOpCode.instr6584 -> method8865(exec)
            ClientScriptOpCode.instr6069 -> method1362(exec)
            ClientScriptOpCode.QC_SETMESSAGE -> method11154(exec)
            ClientScriptOpCode.QC_SENDPUBLICMESSAGE -> method6315(exec)
            ClientScriptOpCode.QC_SENDPRIVATEMESSAGE -> method8296(exec)
            ClientScriptOpCode.QC_SENDFRIENDSCHATMESSAGE -> method3921(exec)
            ClientScriptOpCode.QC_GETSUBHOTKEY -> method291(exec)
            ClientScriptOpCode.instr6591 -> method7826(exec)
            ClientScriptOpCode.instr6592 -> method4148(exec)
            ClientScriptOpCode.instr6449 -> method1783(exec)
            ClientScriptOpCode.instr6594 -> method6304(exec)
            ClientScriptOpCode.instr6595 -> method6439(exec)
            ClientScriptOpCode.instr6596 -> method4631(exec)
            ClientScriptOpCode.instr6597 -> method1085(exec)
            ClientScriptOpCode.instr6598 -> method2091(exec)
            ClientScriptOpCode.instr6599 -> method1070(exec)
            ClientScriptOpCode.instr6613 -> method3986(exec)
            ClientScriptOpCode.instr6601 -> method3950()
            ClientScriptOpCode.QC_SENDCLANCHATMESSAGE -> method468(exec)
            ClientScriptOpCode.QC_SENDGUESTCLANCHATMESSAGE -> method1603(exec)
            ClientScriptOpCode.instr6233 -> method3988(exec)
            ClientScriptOpCode.instr6716 -> method928(exec)
            ClientScriptOpCode.instr6606 -> method8297(exec)
            ClientScriptOpCode.instr6607 -> zoomWorldmap(exec)
            ClientScriptOpCode.instr6608 -> method15417(exec)
            ClientScriptOpCode.instr6308 -> method7668(exec)
            ClientScriptOpCode.instr6610 -> method13756(exec)
            ClientScriptOpCode.instr6611 -> method6073(exec)
            ClientScriptOpCode.instr6612 -> method8219(exec)
            ClientScriptOpCode.instr6849 -> method1391(exec)
            ClientScriptOpCode.instr6614 -> method7276(exec)
            ClientScriptOpCode.instr6615 -> method4988(exec)
            ClientScriptOpCode.instr6616 -> method5788(exec)
            ClientScriptOpCode.instr6617 -> method787(exec)
            ClientScriptOpCode.instr6090 -> method3556(exec)
            ClientScriptOpCode.instr6074 -> method2856(exec)
            ClientScriptOpCode.instr6620 -> method856(exec)
            ClientScriptOpCode.instr6621 -> method2737(exec)
            ClientScriptOpCode.instr6439 -> method3768(exec)
            ClientScriptOpCode.instr6685 -> method7043(exec)
            ClientScriptOpCode.instr6638 -> method2584(exec)
            ClientScriptOpCode.instr6212 -> method2916(exec)
            ClientScriptOpCode.instr5966 -> method12572(exec)
            ClientScriptOpCode.instr6206 -> method810(exec)
            ClientScriptOpCode.instr6628 -> method7774(exec)
            ClientScriptOpCode.instr6238 -> method3919(exec)
            ClientScriptOpCode.instr6630 -> method11339(exec)
            ClientScriptOpCode.instr6631 -> method5829(exec)
            ClientScriptOpCode.instr6632 -> method12506(exec)
            ClientScriptOpCode.instr6633 -> method2963(exec)
            ClientScriptOpCode.instr6634 -> method14836(exec)
            ClientScriptOpCode.instr6582 -> method3557(exec)
            ClientScriptOpCode.instr6671 -> method4091(exec)
            ClientScriptOpCode.instr6637 -> method12660(exec)
            ClientScriptOpCode.instr6818 -> method15403()
            ClientScriptOpCode.FULLSCREEN -> method3804(exec)
            ClientScriptOpCode.instr6488 -> method3613()
            ClientScriptOpCode.FULLSCREEN_MODECOUNT -> method3066(exec)
            ClientScriptOpCode.FULLSCREEN_GETMODE -> method5769(exec)
            ClientScriptOpCode.FULLSCREEN_LASTMODE -> method14244(exec)
            ClientScriptOpCode.instr6644 -> method8199(exec)
            ClientScriptOpCode.CHOOSE_RENDER_TYPE -> method12933(exec)
            ClientScriptOpCode.instr6406 -> method3358(exec)
            ClientScriptOpCode.instr6647 -> method8738(exec)
            ClientScriptOpCode.instr6648 -> method14434(exec)
            ClientScriptOpCode.instr6649 -> method1861(exec)
            ClientScriptOpCode.instr6650 -> method296(exec)
            ClientScriptOpCode.instr6651 -> method2112(exec)
            ClientScriptOpCode.instr6652 -> method4176(exec)
            ClientScriptOpCode.instr6653 -> WindowMode.notifyError()
            ClientScriptOpCode.instr6654 -> method4774(exec)
            ClientScriptOpCode.instr6655 -> method8263(exec)
            ClientScriptOpCode.instr6656 -> method1604(exec)
            ClientScriptOpCode.instr6657 -> method8206(exec)
            ClientScriptOpCode.instr6658 -> method7277()
            ClientScriptOpCode.instr6666 -> method11405(exec)
            ClientScriptOpCode.POP_2_INT -> method3938(exec)
            ClientScriptOpCode.instr6661 -> method254(exec)
            ClientScriptOpCode.instr6866 -> method584(exec)
            ClientScriptOpCode.instr6416 -> method2171()
            ClientScriptOpCode.instr6410 -> method587()
            ClientScriptOpCode.GETCLIPBOARD -> method6440(exec)
            ClientScriptOpCode.instr6780 -> method7209(exec)
            ClientScriptOpCode.instr6667 -> method12210(exec)
            ClientScriptOpCode.instr6668 -> method6044(exec)
            ClientScriptOpCode.CHECK_JAVA_VERSION -> checkJavaVersion(exec)
            ClientScriptOpCode.IS_GAMESCREEN_STATE -> method6317(exec)
            ClientScriptOpCode.instr6923 -> method15513(exec)
            ClientScriptOpCode.instr6672 -> method8718(exec)
            ClientScriptOpCode.instr6673 -> method7021(exec)
            ClientScriptOpCode.instr6451 -> method13359(exec)
            ClientScriptOpCode.instr6675 -> method4177(exec)
            ClientScriptOpCode.instr6162 -> method1162(exec)
            ClientScriptOpCode.instr6677 -> method1366(exec)
            ClientScriptOpCode.instr6678 -> method1837()
            ClientScriptOpCode.instr6679 -> method16079(exec)
            ClientScriptOpCode.instr6456 -> method1456(exec)
            ClientScriptOpCode.instr6681 -> method1370(exec)
            ClientScriptOpCode.instr6663 -> method4775()
            ClientScriptOpCode.instr6108 -> method1627()
            ClientScriptOpCode.instr6684 -> method1071()
            ClientScriptOpCode.instr6823 -> method7518()
            ClientScriptOpCode.instr6686 -> method1367(exec)
            ClientScriptOpCode.instr6331 -> method8205(exec)
            ClientScriptOpCode.instr6627 -> method11439(exec)
            ClientScriptOpCode.instr6236 -> method8742()
            ClientScriptOpCode.instr6013 -> method3038(exec)
            ClientScriptOpCode.CAM_MODEISFOLLOWPLAYER -> method12115(exec)
            ClientScriptOpCode.instr6692 -> method6674(exec)
            ClientScriptOpCode.instr6204 -> method6695()
            ClientScriptOpCode.instr6694 -> method3368()
            ClientScriptOpCode.SEND_VERIFY_EMAIL_PACKET -> sendVerifyEmailPacket(exec)
            ClientScriptOpCode.SEND_SIGNUP_FORM_PACKET -> sendSignupFormPacket(exec)
            ClientScriptOpCode.instr6697 -> method3172()
            ClientScriptOpCode.instr6698 -> method7704(exec)
            ClientScriptOpCode.instr6699 -> method8303(exec)
            ClientScriptOpCode.instr6700 -> method4417(exec)
            ClientScriptOpCode.instr6323 -> method4161(exec)
            ClientScriptOpCode.instr6702 -> method5119(exec)
            ClientScriptOpCode.instr6703 -> method5205(exec)
            ClientScriptOpCode.instr6774 -> method4826(exec)
            ClientScriptOpCode.instr6641 -> method2107()
            ClientScriptOpCode.instr6305 -> method301(exec)
            ClientScriptOpCode.instr6707 -> method5925(exec)
            ClientScriptOpCode.instr6623 -> method775()
            ClientScriptOpCode.instr6709 -> method6285(exec)
            ClientScriptOpCode.instr6710 -> method2825(exec)
            ClientScriptOpCode.instr6711 -> method12597(exec)
            ClientScriptOpCode.instr6712 -> method453(exec)
            ClientScriptOpCode.instr6642 -> method6280()
            ClientScriptOpCode.instr6714 -> method3119(exec)
            ClientScriptOpCode.instr6576 -> method14669(exec)
            ClientScriptOpCode.instr6101 -> method2153(exec)
            ClientScriptOpCode.instr6717 -> method3120()
            ClientScriptOpCode.instr6718 -> method6169(exec)
            ClientScriptOpCode.instr6719 -> method6396(exec)
            ClientScriptOpCode.instr6720 -> method7744(exec)
            ClientScriptOpCode.instr6721 -> method3748(exec)
            ClientScriptOpCode.instr6722 -> method5047(exec)
            ClientScriptOpCode.instr6609 -> method284(exec)
            ClientScriptOpCode.instr6724 -> method457(exec)
            ClientScriptOpCode.instr6725 -> method7500(exec)
            ClientScriptOpCode.instr6726 -> method2568(exec)
            ClientScriptOpCode.instr6727 -> method14857(exec)
            ClientScriptOpCode.instr6860 -> method3611(exec)
            ClientScriptOpCode.DETAIL_LIGHTDETAIL_HIGH -> method451(exec)
            ClientScriptOpCode.DETAIL_WATERDETAIL_HIGH -> method3693(exec)
            ClientScriptOpCode.instr6731 -> method12803(exec)
            ClientScriptOpCode.instr6674 -> setAntiAliasingPreview(exec)
            ClientScriptOpCode.DETAIL_STEREO -> setMonoStero(exec)
            ClientScriptOpCode.DETAIL_SOUNDVOL -> setEffectVolume(exec)
            ClientScriptOpCode.DETAIL_MUSICVOL -> method2554(exec)
            ClientScriptOpCode.DETAIL_BGSOUNDVOL -> method12718(exec)
            ClientScriptOpCode.DETAIL_REMOVEROOFS_OPTION_OVERRIDE -> method6397(exec)
            ClientScriptOpCode.DETAIL_PARTICLES -> method11218(exec)
            ClientScriptOpCode.DETAIL_ANTIALIASING_DEFAULT -> saveAntiAliasingPreference(exec)
            ClientScriptOpCode.DETAIL_BUILDAREA -> method5766(exec)
            ClientScriptOpCode.DETAIL_BLOOM -> setBloom(exec)
            ClientScriptOpCode.DETAIL_CUSTOMCURSOR -> method1857(exec)
            ClientScriptOpCode.instr6743 -> method3750(exec)
            ClientScriptOpCode.instr6744 -> method1803(exec)
            ClientScriptOpCode.CHANGE_RENDER -> method6678(exec)
            ClientScriptOpCode.instr6922 -> method6729(exec)
            ClientScriptOpCode.instr6747 -> method2586(exec)
            ClientScriptOpCode.instr6748 -> method3079(exec)
            ClientScriptOpCode.instr6570 -> method4035(exec)
            ClientScriptOpCode.instr6750 -> method3200(exec)
            ClientScriptOpCode.instr6629 -> method4899(exec)
            ClientScriptOpCode.instr6752 -> method1586(exec)
            ClientScriptOpCode.instr6948 -> method452(exec)
            ClientScriptOpCode.instr6754 -> method8156(exec)
            ClientScriptOpCode.instr6285 -> method292(exec)
            ClientScriptOpCode.instr6756 -> method15555(exec)
            ClientScriptOpCode.instr6688 -> method6773(exec)
            ClientScriptOpCode.instr6489 -> method12560(exec)
            ClientScriptOpCode.instr6759 -> method1350(exec)
            ClientScriptOpCode.instr6785 -> method6675(exec)
            ClientScriptOpCode.instr6761 -> method585(exec)
            ClientScriptOpCode.DETAILGET_WATERDETAIL_HIGH -> getWaterPreference(exec)
            ClientScriptOpCode.instr6060 -> pushFogPreference(exec)
            ClientScriptOpCode.instr6938 -> method6195(exec)
            ClientScriptOpCode.instr6755 -> method7082(exec)
            ClientScriptOpCode.instr6766 -> method4881(exec)
            ClientScriptOpCode.instr6767 -> method3692(exec)
            ClientScriptOpCode.instr6768 -> method13404(exec)
            ClientScriptOpCode.instr5975 -> method4279(exec)
            ClientScriptOpCode.instr6770 -> method2978(exec)
            ClientScriptOpCode.instr6605 -> method3893(exec)
            ClientScriptOpCode.instr6640 -> method15449(exec)
            ClientScriptOpCode.instr6773 -> method7334(exec)
            ClientScriptOpCode.instr6925 -> method13301(exec)
            ClientScriptOpCode.instr5954 -> method1140(exec)
            ClientScriptOpCode.instr6600 -> method3667(exec)
            ClientScriptOpCode.instr6777 -> method6908(exec)
            ClientScriptOpCode.instr6778 -> method7448(exec)
            ClientScriptOpCode.instr6286 -> method4900(exec)
            ClientScriptOpCode.instr6400 -> method2549(exec)
            ClientScriptOpCode.instr6781 -> method1463(exec)
            ClientScriptOpCode.instr6301 -> method5734(exec)
            ClientScriptOpCode.instr6783 -> method7143(exec)
            ClientScriptOpCode.instr6784 -> method6238(exec)
            ClientScriptOpCode.instr6941 -> method4324(exec)
            ClientScriptOpCode.instr6786 -> method6153(exec)
            ClientScriptOpCode.instr6775 -> method934(exec)
            ClientScriptOpCode.instr6097 -> method745(exec)
            ClientScriptOpCode.instr6789 -> method2873(exec)
            ClientScriptOpCode.instr6790 -> method1643(exec)
            ClientScriptOpCode.instr6764 -> method1815(exec)
            ClientScriptOpCode.instr6792 -> method15404(exec)
            ClientScriptOpCode.instr6141 -> method4882(exec)
            ClientScriptOpCode.instr6486 -> method4973(exec)
            ClientScriptOpCode.instr5988 -> method2587(exec)
            ClientScriptOpCode.instr6365 -> method7551(exec)
            ClientScriptOpCode.instr6797 -> method287(exec)
            ClientScriptOpCode.instr6798 -> method4178(exec)
            ClientScriptOpCode.instr6799 -> method5299(exec)
            ClientScriptOpCode.instr6800 -> method1479(exec)
            ClientScriptOpCode.instr6821 -> method6272(exec)
            ClientScriptOpCode.instr6802 -> method4194(exec)
            ClientScriptOpCode.instr6803 -> method15437(exec)
            ClientScriptOpCode.instr6227 -> method6374(exec)
            ClientScriptOpCode.instr6829 -> method11244(exec)
            ClientScriptOpCode.instr6806 -> method5010(exec); //setgamehost
            ClientScriptOpCode.instr6807 -> method1387(exec)
            ClientScriptOpCode.instr6808 -> method7726(exec)
            ClientScriptOpCode.instr6809 -> method5816()
            ClientScriptOpCode.WORLDLIST_PINGWORLDS -> setPingWorlds(exec)
            ClientScriptOpCode.instr6589 -> method3039(exec)
            ClientScriptOpCode.IF_DEBUG_GETOPENIFCOUNT -> method927(exec)
            ClientScriptOpCode.IF_DEBUG_GETOPENIFID -> method11613(exec)
            ClientScriptOpCode.IF_DEBUG_GETNAME -> method7006(exec)
            ClientScriptOpCode.IF_DEBUG_GETCOMCOUNT -> method5904(exec)
            ClientScriptOpCode.IF_DEBUG_GETCOMNAME -> method3341(exec)
            ClientScriptOpCode.IF_DEBUG_GETSERVERTRIGGERS -> method8724(exec)
            ClientScriptOpCode.instr6386 -> method7187()
            ClientScriptOpCode.instr6659 -> method11440(exec)
            ClientScriptOpCode.instr6794 -> method11128(exec)
            ClientScriptOpCode.instr6278 -> method3562(exec)
            ClientScriptOpCode.instr6890 -> method15387(exec)
            ClientScriptOpCode.instr6350 -> method8021(exec)
            ClientScriptOpCode.instr6824 -> method6940(exec)
            ClientScriptOpCode.instr6825 -> method11340(exec)
            ClientScriptOpCode.instr6826 -> method3714(exec)
            ClientScriptOpCode.instr6827 -> method548(exec)
            ClientScriptOpCode.instr6828 -> method3911(exec)
            ClientScriptOpCode.instr6920 -> method1138(exec)
            ClientScriptOpCode.instr5955 -> method504(exec)
            ClientScriptOpCode.MEC_TEXT -> method15616(exec)
            ClientScriptOpCode.MEC_SPRITE -> method1255(exec)
            ClientScriptOpCode.MEC_TEXTSIZE -> method3755(exec)
            ClientScriptOpCode.MEC_CATEGORY -> method290(exec)
            ClientScriptOpCode.MEC_PARAM -> getWorldMapParam(exec)
            ClientScriptOpCode.USERDETAIL_QUICKCHAT -> method5832(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_MEMBERSHIP -> method1499(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_RECOVERYDAY -> method14655(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_UNREADMESSAGES -> method7915(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LASTLOGINDAY -> method12859(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LASTLOGINADDRESS -> method7462(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_EMAILSTATUS -> method3894(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_CCEXPIRY -> method1357(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_GRACEEXPIRY -> method1855(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_DOBREQUESTED -> method12352(exec)
            ClientScriptOpCode.USERDETAIL_DOB -> method586(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_MEMBERSSTATS -> method13467(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_PLAYAGE -> method8869(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_JCOINS_BALANCE -> getJcoins(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LOYALTY_ENABLED -> method1351(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LOYALTY_BALANCE -> method6046(exec)
            ClientScriptOpCode.instr6852 -> enterDisplaymode(exec)
            ClientScriptOpCode.AUTOSETUP_SETHIGH -> method3233()
            ClientScriptOpCode.AUTOSETUP_SETMEDIUM -> method7773()
            ClientScriptOpCode.AUTOSETUP_SETLOW -> method15306()
            ClientScriptOpCode.AUTOSETUP_SETMIN -> method6239()
            ClientScriptOpCode.instr6857 -> method4776()
            ClientScriptOpCode.instr6858 -> computeGraphicsMode()
            ClientScriptOpCode.instr6859 -> method481(exec)
            ClientScriptOpCode.instr6046 -> method6290(exec)
            ClientScriptOpCode.instr6861 -> method12561(exec)
            ClientScriptOpCode.instr6862 -> method5199(exec)
            ClientScriptOpCode.instr6863 -> method14818(exec)
            ClientScriptOpCode.instr6838 -> method3170(exec)
            ClientScriptOpCode.instr6680 -> method7962()
            ClientScriptOpCode.instr6476 -> method14839()
            ClientScriptOpCode.instr6867 -> method15406(exec)
            ClientScriptOpCode.instr6868 -> method572(exec)
            ClientScriptOpCode.instr6869 -> method6350(exec)
            ClientScriptOpCode.instr6058 -> method2597(exec)
            ClientScriptOpCode.DETAILCANMOD_GROUNDDECOR -> method1862(exec)
            ClientScriptOpCode.DETAILCANMOD_CHARSHADOWS -> method15371(exec)
            ClientScriptOpCode.DETAILCANMOD_SPOTSHADOWS -> method4989(exec)
            ClientScriptOpCode.DETAILCANMOD_WATERDETAIL -> method2588(exec)
            ClientScriptOpCode.DETAILCANMOD_ANTIALIASING -> method8343(exec)
            ClientScriptOpCode.DETAILCANMOD_PARTICLES -> method5767(exec)
            ClientScriptOpCode.DETAILCANMOD_BUILDAREA -> loadMemoryMode(exec)
            ClientScriptOpCode.DETAILCANMOD_BLOOM -> method6072(exec)
            ClientScriptOpCode.DETAILCANMOD_GROUNDBLENDING -> method8844(exec)
            ClientScriptOpCode.DETAILCANMOD_TEXTURING -> method743(exec)
            ClientScriptOpCode.DETAILCANMOD_MAXSCREENSIZE -> method7552(exec)
            ClientScriptOpCode.DETAILCANMOD_FOG -> method5065(exec)
            ClientScriptOpCode.DETAILCANMOD_TOOLKIT_DEFAULT -> method7420(exec)
            ClientScriptOpCode.DETAILCANMOD_TOOLKIT -> method3781(exec)
            ClientScriptOpCode.DETAILCANMOD_SKYBOXES -> method1574(exec)
            ClientScriptOpCode.DETAILCANSET_GROUNDDECOR -> method11341(exec)
            ClientScriptOpCode.DETAILCANSET_CHARSHADOWS -> method15551(exec)
            ClientScriptOpCode.DETAILCANSET_SPOTSHADOWS -> method11351(exec)
            ClientScriptOpCode.DETAILCANSET_WATERDETAIL -> method3713(exec)
            ClientScriptOpCode.DETAILCANSET_ANTIALIASING -> method7218(exec)
            ClientScriptOpCode.DETAILCANSET_PARTICLES -> method6279(exec)
            ClientScriptOpCode.DETAILCANSET_BUILDAREA -> method14649(exec)
            ClientScriptOpCode.DETAILCANSET_BLOOM -> method4413(exec)
            ClientScriptOpCode.DETAILCANSET_GROUNDBLENDING -> method1352(exec)
            ClientScriptOpCode.DETAILCANSET_TEXTURING -> method1572(exec)
            ClientScriptOpCode.DETAILCANSET_MAXSCREENSIZE -> method12596(exec)
            ClientScriptOpCode.DETAILCANSET_FOG -> method15407(exec)
            ClientScriptOpCode.DETAILCANSET_TOOLKIT_DEFAULT -> method1968(exec)
            ClientScriptOpCode.DETAILCANSET_RENDERER -> method2108(exec)
            ClientScriptOpCode.DETAILCANSET_SKYBOXES -> method4867(exec)
            ClientScriptOpCode.instr6388 -> method543(exec)
            ClientScriptOpCode.instr6902 -> method261(exec)
            ClientScriptOpCode.GET_ENTITY_SAY -> method6281(exec)
            ClientScriptOpCode.GET_DISPLAYNAME_WITHEXTRAS -> method6730(exec)
            ClientScriptOpCode.instr6905 -> method5814(exec)
            ClientScriptOpCode.instr6906 -> method12586(exec)
            ClientScriptOpCode.GET_NPC_NAME -> method12915(exec)
            ClientScriptOpCode.instr6908 -> method1784(exec)
            ClientScriptOpCode.GET_ENTITY_SCREEN_POSITION -> pushEntityHintPosition(exec)
            ClientScriptOpCode.IF_GET_GAMESCREEN -> method14606(exec)
            ClientScriptOpCode.instr6911 -> method2867(exec)
            ClientScriptOpCode.GET_NPC_STAT -> method2639(exec)
            ClientScriptOpCode.IS_NPC_ACTIVE -> method300(exec)
            ClientScriptOpCode.IS_NPC_VISIBLE -> method3740(exec)
            ClientScriptOpCode.IS_TARGETED_ENTITY -> method3344(exec)
            ClientScriptOpCode.IF_DELETEALL -> method4830(exec)
            ClientScriptOpCode.NPC_TYPE -> method2103(exec)
            ClientScriptOpCode.GET_OBJECT_SCREEN_POSITION -> pushGameobjectHintLocation(exec)
            ClientScriptOpCode.GET_ITEM_SCREEN_POSITION -> pushGroundItemHintPosition(exec)
            ClientScriptOpCode.GET_OBJECT_OVERLAY_HEIGHT -> method6042(exec)
            ClientScriptOpCode.GET_ITEM_OVERLAY_HEIGHT -> method3769(exec)
            ClientScriptOpCode.GET_OBJECT_BOUNDING_BOX -> method7332(exec)
            ClientScriptOpCode.GET_ITEM_BOUNDING_BOX -> method798(exec)
            ClientScriptOpCode.GET_ENTITY_BOUNDING_BOX -> method3067(exec)
            ClientScriptOpCode.BUG_REPORT -> method5298(exec)
            ClientScriptOpCode.ARRAY_SORT -> method8700(exec)
            ClientScriptOpCode.QUEST_GETNAME -> method2101(exec)
            ClientScriptOpCode.QUEST_GETSORTNAME -> method5736(exec)
            ClientScriptOpCode.QUEST_TYPE -> method356(exec)
            ClientScriptOpCode.QUEST_GETDIFFICULTY -> method7434(exec)
            ClientScriptOpCode.QUEST_GETMEMBERS -> method7461(exec)
            ClientScriptOpCode.QUEST_POINTS -> method1799(exec)
            ClientScriptOpCode.QUEST_QUESTREQ_COUNT -> method5016(exec)
            ClientScriptOpCode.QUEST_QUESTREQ -> method777(exec)
            ClientScriptOpCode.QUEST_QUESTREQ_MET -> method4314(exec)
            ClientScriptOpCode.QUEST_POINTSREQ -> method3753(exec)
            ClientScriptOpCode.QUEST_POINTSREQ_MET -> method466(exec)
            ClientScriptOpCode.QUEST_STATREQ_COUNT -> method2953(exec)
            ClientScriptOpCode.QUEST_STATREQ_STAT -> method2092(exec)
            ClientScriptOpCode.QUEST_STATREQ_LEVEL -> method2841(exec)
            ClientScriptOpCode.QUEST_STATREQ_MET -> method14841(exec)
            ClientScriptOpCode.QUEST_VARPREQ_COUNT -> method868(exec)
            ClientScriptOpCode.QUEST_VARPREQ_DESC -> method3553(exec)
            ClientScriptOpCode.QUEST_VARPREQ_MET -> method1067(exec)
            ClientScriptOpCode.QUEST_VARBITREQ_COUNT -> method7083(exec)
            ClientScriptOpCode.QUEST_VARBITREQ_DESC -> method11592(exec)
            ClientScriptOpCode.QUEST_VARBITREQ_MET -> method12587(exec)
            ClientScriptOpCode.QUEST_ALLREQMET -> method4556(exec)
            ClientScriptOpCode.QUEST_STARTED -> method8216(exec)
            ClientScriptOpCode.QUEST_FINISHED -> method5290(exec)
            ClientScriptOpCode.QUEST_PARAM -> method4249(exec)
            else -> throw RuntimeException()
        }
    }

    @JvmStatic
    fun ccSetFront(bool: Boolean, executor: CS2Executor) {
        var underlaydefinition3: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions4: Component? = underlaydefinition3!!.comp
        var interface5: Interface? = underlaydefinition3!!.inter
        if (bool) {
            Interface.method7554(interface5!!, icomponentdefinitions4)
        } else {
            Interface.method3710(interface5!!, icomponentdefinitions4)
        }
    }

    @JvmStatic
    fun ifSetFront(bool: Boolean, executor: CS2Executor) {
        var i_3 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions4: Component? = Component.fromHash(i_3)
        var interface5: Interface? = Interface.INTERFACES!![(i_3 shr 16)]
        if (bool) {
            Interface.method7554(interface5!!, icomponentdefinitions4)
        } else {
            Interface.method3710(interface5!!, icomponentdefinitions4)
        }

    }

    @JvmStatic
    fun method5755(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.aBool7323) 1  else 0
    }

    @JvmStatic
    fun method5756(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method1454(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5758(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        method7927(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3546(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method5017(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3547(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        var i_4 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = string_2!!.indexOf(string_3!!, i_4)
    }

    @JvmStatic
    fun method2597(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
    }

    @JvmStatic
    fun loadClanVarbit(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var class4053: ClanVarType? = TypeLists.CLAN_VAR!!.list(i_2)
        if (class4053!!.aChar4832.code != 1) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
        var integer4: Int? = ClanVarTypeList.CLAN_VARS!![class4053!!.baseVar] as Int?
        if (integer4 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            var i_5 = if (class4053!!.endBit == 31) -1 else ((1 shl (class4053!!.endBit + 1)) - 1)
            executor.intStack[++executor.intStackPtr - 1] = (integer4!!.toInt() and i_5) ushr class4053!!.startBit
        }
    }

    @JvmStatic
    fun loadClanSettingVarLong(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var long_3: Long? = executor.currentClanSettings!!.getLongVar((Client.CURRENT_GAME!!.id shl 16) or i_2)
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
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var string_3: String? = executor.currentClanSettings!!.getStringVar((Client.CURRENT_GAME!!.id shl 16) or i_2)
        var string_4: String? = null
        if (string_3 == null) {
            string_4 = ""
        } else {
            string_4 = string_3
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_4
    }

    @JvmStatic
    fun method11218(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        PreferenceManager.preferences!!.particles!!.setValue(i_2)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun gotoInstr(executor: CS2Executor) {
        executor.instrPtr += executor.intOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun method1255(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var worldmapareadefs3: MECType? = TypeLists.MEC!!.list(i_2)
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3!!.defaultIconArchive
    }

    @JvmStatic
    fun method7518() {
        StaticMethods.method8146()
    }

    @JvmStatic
    fun method7520(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method12209(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun getWaterPreference(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.water!!.getValue() == 2) 1 else 0
    }

    @JvmStatic
    fun ccSetOpBase(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setOpBase(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun getMyPlayerPos(executor: CS2Executor) {
        var b_2 = PlayerUpdate.MY_PLAYER!!.plane
        var vector33: Vector3? = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
        var coordgrid4: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        var i_5 = (vector33!!.x.toInt() shr 9) + coordgrid4!!.x
        var i_6 = (vector33!!.z.toInt() shr 9) + coordgrid4!!.y
        executor.intStack[++executor.intStackPtr - 1] = (i_5 shl 14) + i_6 + b_2.toInt() shl 28
    }

    @JvmStatic
    fun method15510(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method506(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method15512(executor: CS2Executor) {
        var index = executor.intStack[--executor.intStackPtr]
        var chatLine: ChatLine? = ChatBox.getLine(index)
        var type = -1
        if (chatLine != null) {
            type = chatLine!!.type!!.getValue()
        }
        executor.intStack[++executor.intStackPtr - 1] = type
    }

    @JvmStatic
    fun method15513(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = GameShell.getPreferenceFile().toString()
    }

    @JvmStatic
    fun method15514(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        ifSetColor(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method2865(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method11337(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method2867(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentEntity!!.getIconHeight()
    }

    @JvmStatic
    fun method2868(executor: CS2Executor) {
        if (ColorImageCache.CS2_QUERY_RESULTS != null && StaticElements.CS2_QUERY_RESULT_IDX < CS2_QUERY_RESULTS_LEN) {
            executor.intStack[++executor.intStackPtr - 1] = ColorImageCache.CS2_QUERY_RESULTS!![++StaticElements.CS2_QUERY_RESULT_IDX - 1].toInt() and 0xffff
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun method1366(executor: CS2Executor) {
        executor.intStackPtr -= 6
        var i_2 = executor.intStack[executor.intStackPtr]
        if (i_2 >= 2) {
            throw RuntimeException()
        } else {
            Client.anInt7277 = i_2
            var i_3 = executor.intStack[executor.intStackPtr + 1]
            if (i_3 + 1 >= (Client.anIntArrayArrayArray7302[Client.anInt7277]!!.size shr 1)) {
                throw RuntimeException()
            } else {
                Client.anInt7279 = i_3
                Client.anInt7256 = 0
                Client.anInt7327 = executor.intStack[executor.intStackPtr + 2]
                Client.anInt7182 = executor.intStack[executor.intStackPtr + 3]
                var i_4 = executor.intStack[executor.intStackPtr + 4]
                if (i_4 >= 2) {
                    throw RuntimeException()
                } else {
                    Client.anInt7448 = i_4
                    var i_5 = executor.intStack[executor.intStackPtr + 5]
                    if (i_5 + 1 >= (Client.anIntArrayArrayArray7302[Client.anInt7448]!!.size shr 1)) {
                        throw RuntimeException()
                    } else {
                        Client.anInt7280 = i_5
                        Camera.cameraCalculationType = 3
                        StaticMethods.anInt833 = -1
                        PingRequester.anInt5864 = -1
                    }
                }
            }
        }
    }

    @JvmStatic
    fun method1367(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 < 0) {
            i_2 = 0
        }
        Client.anInt7271 = i_2 + Client.anInt7270
    }

    @JvmStatic
    fun method1370(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Camera.camAngleY.toInt() shr 3
    }

    @JvmStatic
    fun method12572(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub63: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub63 != null) {
            var bool4 = class282Sub50Sub63.method14775((i_2 shr 14) and 0x3fff, i_2 and 0x3fff, CS2Executor.anIntArray5900)
            if (bool4) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900!![1]
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900!![2]
            } else {
                executor.intStack[++executor.intStackPtr - 1] = -1
                executor.intStack[++executor.intStackPtr - 1] = -1
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun setPingWorlds(executor: CS2Executor) {
        if (Client.gameState == GameState.UNK_0)
            WorldConnection.PING_WORLDS = executor.intStack[--executor.intStackPtr] == 1
    }

    @JvmStatic
    fun method8714(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method2960(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method8715(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        method3251(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8716(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.method5908()
        executor.intStack[++executor.intStackPtr - 1] = if (i_3 == 2) 1 else 0
    }

    @JvmStatic
    fun method8717(executor: CS2Executor) {
        var string_2: String? = null
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER!!.username != null) {
            string_2 = PlayerUpdate.MY_PLAYER!!.getUsernameWithTitle()
        } else {
            string_2 = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2
    }

    @JvmStatic
    fun method8718(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.filePicker = DirectoryPicker(string_2!!)
    }

    @JvmStatic
    fun scale(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var long_2 = executor.intStack[executor.intStackPtr]
        var long_4 = executor.intStack[executor.intStackPtr + 1]
        var long_6 = executor.intStack[executor.intStackPtr + 2]
        executor.intStack[++executor.intStackPtr - 1] = (long_6.toLong() * long_2.toLong() / long_4.toLong()).toInt()
    }

    @JvmStatic
    fun arrayLoad(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var i_3 = executor.intStack[--executor.intStackPtr]
        if (i_3 >= 0 && i_3 < executor.globalArrayLengths[i_2]) {
            executor.intStack[++executor.intStackPtr - 1] = executor.globalArrays[i_2][i_3]
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun method14679(executor: CS2Executor) {
        method1573(executor)
    }

    @JvmStatic
    fun method3779(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings!!.bannedUserNames!![i_2]
    }

    @JvmStatic
    fun method3780(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var class282Sub50Sub144: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2)
        var i_5 = class282Sub50Sub144!!.subCategories!![i_3]
        executor.intStack[++executor.intStackPtr - 1] = i_5
    }

    @JvmStatic
    fun method3781(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.toolkit!!.visible()) 1  else 0
    }

    @JvmStatic
    fun method3783(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var i_4 = -1
        if (chatline3 != null) {
            i_4 = chatline3.quickchatMessageId
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun method3553(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).varpRequirementNames!![executor.intStack[executor.intStackPtr - 1]]
        executor.intStackPtr -= 2
    }

    @JvmStatic
    fun method3554(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method8754(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3555(executor: CS2Executor) {
        SongPlayer.playSong(executor.intStack[--executor.intStackPtr], 255, 50)
    }

    @JvmStatic
    fun method3556(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub63: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub63 != null) {
            var bool4 = class282Sub50Sub63.method14778((i_2 shr 28) and 0x3, (i_2 shr 14) and 0x3fff, i_2 and 0x3fff, CS2Executor.anIntArray5900)
            if (bool4) {
                StaticMethods.method14263(CS2Executor.anIntArray5900!![1], CS2Executor.anIntArray5900!![2])
            }
        }
    }

    @JvmStatic
    fun method3557(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var node3: Node<*>? = WorldMapSub1.aClass4658025.get(i_2.toLong())
        executor.intStack[++executor.intStackPtr - 1] = if (node3 != null) 1 else 0
    }

    @JvmStatic
    fun method466(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1])!!.hasQuestpointRequirement(VarProviders.PLAYER_VAR_PROVIDER)) 1 else 0
    }

    @JvmStatic
    fun longLessThan(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] < executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method468(executor: CS2Executor) {
        var class1842: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        var i_4 = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(2)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.method14896(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.anIntArray4046!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - i_4)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method471(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = executor.clanChannel!!.getPlayersSortedByName()[executor.intStack[executor.intStackPtr - 1]]
    }

    @JvmStatic
    fun multiply(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_3 * i_2
    }

    @JvmStatic
    fun method15387(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(4, (i_2 shl 16) or i_3, i_4, "")
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
    fun method1586(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 > 255 || i_2 < 0) {
            i_2 = 0
        }
        if (i_2 != PreferenceManager.preferences!!.gametipStartStagePreference!!.getValue()) {
            PreferenceManager.preferences!!.gametipStartStagePreference!!.setValue(i_2)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun enterDisplaymode(executor: CS2Executor) {
        var hraphicsPreference = GameShell.calculateGraphics()
        lastEnteredGraphicsMode = PreferenceManager.preferences!!.activeToolkit!!.getValue()
        executor.intStack[++executor.intStackPtr - 1] = lastEnteredGraphicsMode
        executor.intStack[++executor.intStackPtr - 1] = hraphicsPreference
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method3170(executor: CS2Executor) {
        var bool2 = false
        var param = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (bool2) 1  else 0
    }

    @JvmStatic
    fun method3171(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        method3601(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3172() {
        GameState.method12426()
    }

    @JvmStatic
    fun method6279(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.particles!!.canModify(i_2)
    }

    @JvmStatic
    fun method6280() {
        Client.aBool7189 = true
    }

    @JvmStatic
    fun method6281(executor: CS2Executor) {
        var class1632: HintDetails? = executor.currentEntity!!.getHintDetails()
        if (class1632 == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = class1632.getText()
        }
    }

    @JvmStatic
    fun method15616(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var worldmapareadefs3: MECType? = TypeLists.MEC!!.list(i_2)
        if (worldmapareadefs3!!.areaName == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = worldmapareadefs3!!.areaName
        }
    }

    @JvmStatic
    fun method6016(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method14892(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun setCCFont(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setIfFont(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method8263(executor: CS2Executor) {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
        }
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var bool3 = executor.intStack[--executor.intStackPtr] == 1
        var string_4: String? = ServerEnvironment.getUrl() + string_2
        Browser.openOpenjs(string_4!!, bool3, Client.aBool7158)
    }

    @JvmStatic
    fun getNPCParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.NPC!!.list(i_2).getCS2Param(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.NPC!!.list(i_2).getCs2Value(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun addPercent(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var long_2 = executor.intStack[executor.intStackPtr]
        var long_4 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = (long_2 + long_2.toLong() * long_4.toLong() / 100L).toInt()
    }

    @JvmStatic
    fun method3892(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method3078(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3893(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.mapSize!!.getValue()
    }

    @JvmStatic
    fun method3894(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.EMAIL_STATUS
    }

    @JvmStatic
    fun getPlayerGender(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null && PlayerUpdate.MY_PLAYER!!.playerAppearance!!.female) 1 else 0
    }

    @JvmStatic
    fun method4881(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.soundEffectVolume!!.getValue()
    }

    @JvmStatic
    fun method4882(executor: CS2Executor) {
        AbstractToolkit.calculateGameArea(0, 0, Client.GAME_SCREEN_INTERFACE!!.width, Client.GAME_SCREEN_INTERFACE!!.height, false)
        executor.intStack[++executor.intStackPtr - 1] = Client.gameAreaWidth
        executor.intStack[++executor.intStackPtr - 1] = Client.gameAreaHeight
    }

    @JvmStatic
    fun storeVarc(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        VarcTypeList.VARC_INT!![i_2] = executor.intStack[--executor.intStackPtr]
        PulseEventQueue.delayedClientvarcUpdate(i_2)
        Client.clientIsActive = Client.clientIsActive or VarcTypeList.VARC_BOOL!![i_2]
    }

    @JvmStatic
    fun method5099(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method4159(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5100(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.getLineCount()
    }

    @JvmStatic
    fun method13408(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method1642(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method12859(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.LASTLOGINLDAY
    }

    @JvmStatic
    fun method2978(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasing!!.getValue()
    }

    @JvmStatic
    fun method1853(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        if (i_3 == -1) {
            throw RuntimeException()
        } else {
            var enumdefinitions5: EnumType? = TypeLists.ENUM!!.list(i_3)
            if (i_2 != enumdefinitions5!!.valueType.code) {
                throw RuntimeException()
            } else {
                var ints_6: IntArray? = enumdefinitions5!!.getValueReferences(i_4)
                var i_7 = 0
                if (ints_6 != null) {
                    i_7 = ints_6!!.size
                }
                executor.intStack[++executor.intStackPtr - 1] = i_7
            }
        }
    }

    @JvmStatic
    fun method1854(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var i_3 = executor.intStack[--executor.intStackPtr]
        var fontmetrics4: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_3)
        assert(fontmetrics4 != null)
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics4!!.getTextWidth(string_2, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun method1855(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = FileOnDisk.anInt5357
    }

    @JvmStatic
    fun stringLength(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (string_2 != null) {
            executor.intStack[++executor.intStackPtr - 1] = string_2!!.length
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method1857(executor: CS2Executor) {
        PreferenceManager.preferences!!.customCursor!!.setValue(if (executor.intStack[--executor.intStackPtr] != 0) 1 else 0)
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun method7643(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var i_4 = -1
        var i_5 = -1
        assert(icomponentdefinitions3 != null)
        var class1196: InteractiveComponentSprite? = icomponentdefinitions3!!.addInteractiveSprite(Renderers.activeToolkit!!)
        if (class1196 != null) {
            i_4 = class1196.width
            i_5 = class1196.height
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
        executor.intStack[++executor.intStackPtr - 1] = i_5
    }

    @JvmStatic
    fun method6290(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 != -1) {
            if (i_3 > 255) {
                i_3 = 255
            } else if (i_3 < 0) {
                i_3 = 0
            }
            VorbisSoundManager.method1575(i_2, i_3, false)
        }
    }

    @JvmStatic
    fun setCCText(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setCompText(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method13494(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.aString3643 != null && Client.aString3643.equals(string_2!!)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method14857(executor: CS2Executor) {
        PreferenceManager.preferences!!.characterShadows!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun emailValidationAddNewAddress(executor: CS2Executor) {
        --executor.stringStackPtr
        executor.intStackPtr -= 3
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var bool3 = executor.intStack[executor.intStackPtr] == 1
        var bool4 = executor.intStack[executor.intStackPtr + 1] == 1
        var bool5 = executor.intStack[executor.intStackPtr + 2] == 1
        var tcpmessage6: TcpPacket? = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_ADD_NEW_ADDRESS, Client.LOBBY_CONNECTION_CONTEXT.isaac)
        tcpmessage6!!.buffer.writeShort(TextUtils.getLength(string_2!!) + 1)
        tcpmessage6!!.buffer.writeString(string_2!!)
        var i_7 = 0
        if (bool3) {
            i_7 = i_7 or 0x1
        }
        if (bool4) {
            i_7 = i_7 or 0x2
        }
        if (bool5) {
            i_7 = i_7 or 0x4
        }
        tcpmessage6!!.buffer.writeByte(i_7)
        Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage6)
    }

    @JvmStatic
    fun method5829(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        WorldMapSub1.method2094(i_2)
    }

    @JvmStatic
    fun randomInclusive(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (Math.random() * (i_2 + 1)).toInt()
    }

    @JvmStatic
    fun method5832(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.USERDETAIL_QUICKCHAT && !Client.VERIFIED_EMAIL_ADDRESS) 1 else 0
    }

    @JvmStatic
    fun method5833(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method13468(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method12659(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method7142(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method12660(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = (i_3 shr 14) and 0x3fff
        var i_5 = i_3 and 0x3fff
        var i_6 = WorldMapCategory.method12702(i_2, i_4, i_5)
        if (i_6 < 0) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = i_6
        }
    }

    @JvmStatic
    fun method13410(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.numPlayers
    }

    @JvmStatic
    fun method12925(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spritePitch
    }

    @JvmStatic
    fun method1864(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        if (icomponentdefinitions3!!.opBase == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3!!.opBase
        }
    }

    @JvmStatic
    fun sendJoinFriendchatPacket() {
        PacketEncoder.sendJoinFriendchatPacket()
    }

    @JvmStatic
    fun method741(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method5455(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method742(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub143: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2)
        if (class282Sub50Sub143!!.subCategories == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub143!!.subCategories!!.size
        }
    }

    @JvmStatic
    fun method743(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.textures!!.playingRunescape()) 1  else 0
    }

    @JvmStatic
    fun method745(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.safemodeForcedDuringStartup) 1  else 0
    }

    @JvmStatic
    fun substring(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        var i_3 = executor.intStack[executor.intStackPtr]
        var i_4 = executor.intStack[executor.intStackPtr + 1]
        executor.stringStack[++executor.stringStackPtr - 1] = string_2!!.substring(i_3, i_4)
    }

    @JvmStatic
    fun method8843(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method6288(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8844(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.goundBlending!!.playingRunescape()) 1  else 0
    }

    @JvmStatic
    fun getAnimationParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.SEQ!!.list(i_2).getCs2Object(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.SEQ!!.list(i_2).getCs2Integer(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun method3676(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method7889(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun longGreaterThan(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] > executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method6350(executor: CS2Executor) {
        var bool2 = false
        var string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStack[++executor.intStackPtr - 1] = if (bool2) 1  else 0
    }

    @JvmStatic
    fun method6799(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.AFFILIATE
    }

    @JvmStatic
    fun method6800(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method12630(icomponentdefinitions3!!)
    }

    @JvmStatic
    fun method15475(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method5291(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3079(executor: CS2Executor) {
        PreferenceManager.preferences!!.textures!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        PreferenceManager.savePreferences()
        GraphicsCache.resetGraphicCaches()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun ccSetItem(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setItemIFComp(icomponentdefinitions3!!, interface4!!, false, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun method15399(executor: CS2Executor) {
        PacketEncoder.method4167(executor.stringStack[--executor.stringStackPtr] as String?)
    }

    @JvmStatic
    fun method6796(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.y
    }

    @JvmStatic
    fun method6797(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.method5908()
        executor.intStack[++executor.intStackPtr - 1] = if (i_3 == 0) 1 else 0
    }

    @JvmStatic
    fun method8199(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = WindowMode.getWindowMode()
    }

    @JvmStatic
    fun method7006(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (Interface.INTERFACES!![i_2] == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            var string_3: String? = Interface.INTERFACES!![i_2]!!.components!![0]!!.name
            if (string_3 == null) {
                executor.stringStack[++executor.stringStackPtr - 1] = ""
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = string_3.substring(0, string_3.indexOf(58.toChar()))
            }
        }
    }

    @JvmStatic
    fun storeString(executor: CS2Executor) {
        executor.objectLocals!![executor.intOpValues!![executor.instrPtr]] = executor.stringStack[--executor.stringStackPtr]
    }

    @JvmStatic
    fun method4863(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        ifSetNPCHead(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method4864(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method15474(icomponentdefinitions3!!)
    }

    @JvmStatic
    fun method4865(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method1494(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4867(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.skyboxes!!.canModify(i_2)
    }

    @JvmStatic
    fun storeVarcString(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        VarcTypeList.VARC_STRING!![i_2] = executor.stringStack[--executor.stringStackPtr] as String?
        PulseEventQueue.delayedVarcstringUpdate(i_2)
    }

    @JvmStatic
    fun hideComp(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method786(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method5302(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method1852(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method11125(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.scrollX
    }

    @JvmStatic
    fun method11126(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method1455(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method11128(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(2, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method5901(executor: CS2Executor) {
        executor.intStackPtr -= 4
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        var enumDefinitions: EnumType? = TypeLists.ENUM!!.list(i_4)
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
    fun method5902(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.talkRank.toInt()
    }

    @JvmStatic
    fun method5904(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (Interface.INTERFACES!![i_2] == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Interface.INTERFACES!![i_2]!!.components!!.size
        }
    }

    @JvmStatic
    fun method6668(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var ifComp = executor.intStack[executor.intStackPtr]
        var slotId = executor.intStack[executor.intStackPtr + 1]
        var inter: CS2Interface? = null
        if (executor.aBool7022) {
            inter = executor.hookedInterface2
        } else {
            inter = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (slotId != -1 && inter!!.setChild(ifComp, slotId)) 1 else 0
    }

    @JvmStatic
    fun method6669(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method4280(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6670(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        method14680(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6674(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        LoginManager.directLogin(string_2!!, string_3!!)
    }

    @JvmStatic
    fun method6675(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.sceneryShadows!!.getValue()
    }

    @JvmStatic
    fun method6449(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = if (icomponentdefinitions3!!.mediaType == MediaType.RAW_MODEL) icomponentdefinitions3!!.primaryMediaId else -1
    }

    @JvmStatic
    fun method6452(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method5291(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4030(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method3365(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4031(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (MouseRecord.recorder!!.leftButtonDown()) 1  else 0
        executor.intStack[++executor.intStackPtr - 1] = if (MouseRecord.recorder!!.rightButtonDown()) 1  else 0
        executor.intStack[++executor.intStackPtr - 1] = if (MouseRecord.recorder!!.middleButtonDown()) 1  else 0
    }

    @JvmStatic
    fun method4033(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method14653(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4035(executor: CS2Executor) {
        PreferenceManager.preferences!!.maxScreenSize!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.needsResize = true
    }

    @JvmStatic
    fun min(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = Math.min(i_2, i_3)
    }

    @JvmStatic
    fun longGreaterOrEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] >= executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method7021(executor: CS2Executor) {
        var bool2 = false
        var path: String? = ""
        val filePicker = executor.filePicker
        if (filePicker != null && filePicker.fileChoserOpen()) {
            var directory: File? = filePicker.getPickedDirectory()
            if (directory != null) {
                path = directory.getPath()
            }
            bool2 = true
            executor.filePicker = null
        }
        executor.intStack[++executor.intStackPtr - 1] = if (bool2) 1  else 0
        executor.stringStack[++executor.stringStackPtr - 1] = path
    }

    @JvmStatic
    fun method13301(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.idleAnimations!!.getValue()
    }

    @JvmStatic
    fun method6773(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.idleAnimations!!.getValue()
    }

    @JvmStatic
    fun method15551(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.characterShadows!!.canModify(i_2)
    }

    @JvmStatic
    fun method4277(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.containerItemId
    }

    @JvmStatic
    fun method4278(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        FriendList.remove(string_2)
    }

    @JvmStatic
    fun method4279(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ParticleSystemManager.method5206()
    }

    @JvmStatic
    fun method4283(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method11221(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun setItem(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        val playerAppearance = PlayerUpdate.MY_PLAYER!!.playerAppearance
        if (playerAppearance != null) {
            playerAppearance!!.addAppearanceItem(i_2, i_3, TypeLists.OBJ!!)
        }
    }

    @JvmStatic
    fun method775() {
        Client.logout(false)
    }

    @JvmStatic
    fun method777(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).questPrerequisiteIds!![executor.intStack[executor.intStackPtr - 1]]
        --executor.intStackPtr
    }

    @JvmStatic
    fun method779(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var i_4 = executor.intStack[--executor.intStackPtr]
        --i_4
        val options = icomponentdefinitions3!!.options
        if (options != null && i_4 < options.size && options[i_4] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = options[i_4]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun method4295(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.replacementOwner
    }

    @JvmStatic
    fun intEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr + 1] == executor.intStack[executor.intStackPtr]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method3064(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method2960(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method3065(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.angle2d
    }

    @JvmStatic
    fun method3066(executor: CS2Executor) {
        if (WindowMode.fullScreenSupported) {
            var arr_2 = WindowMode.getActiveDisplayModeDetails()
            executor.intStack[++executor.intStackPtr - 1] = arr_2!!.size
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method3067(executor: CS2Executor) {
        getBoundingBox(executor.currentEntity as SceneGraphEntity, executor)
    }

    @JvmStatic
    fun charIsalpha(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (StaticMethods.method1169(i_2.toChar())) 1 else 0
    }

    @JvmStatic
    fun method5008(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method11223(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5010(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.gameState == GameState.UNK_0 && !LoginManager.method3360()) {
            executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.setGameHost(i_2, string_3!!)) 1 else 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method12724(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (itemdefinitions3!!.certTemplateId >= 0 && itemdefinitions3!!.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions3!!.certId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = i_2
        }
    }

    @JvmStatic
    fun method12215(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (itemdefinitions3!!.certTemplateId == -1 && itemdefinitions3!!.certId >= 0) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions3!!.certId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = i_2
        }
    }

    @JvmStatic
    fun method7143(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.soundLevel!!.getValue()
    }

    @JvmStatic
    fun ccSetAspect(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        ifSetAspect(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method6283(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method5066(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method6284(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).value
    }

    @JvmStatic
    fun method6285(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt72
    }

    @JvmStatic
    fun method1775(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (string_2!!.startsWith(TextUtils.getCrown(0)) || string_2!!.startsWith(TextUtils.getCrown(1))) {
            string_2 = string_2!!.substring(7)
        }
        executor.intStack[++executor.intStackPtr - 1] = FriendList.getIndex(string_2)
    }

    @JvmStatic
    fun hookMousePress(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method15409(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun loadClanVarString(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var string_3: String? = ClanVarTypeList.CLAN_VARS!![i_2] as String?
        if (string_3 == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3
        }
    }

    @JvmStatic
    fun method3356(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        ifSetNPCHead(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method3357(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method14586(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3358(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.fixedScreen!!.getValue()
    }

    @JvmStatic
    fun method3361(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.getMemberIndex(executor.stringStack[--executor.stringStackPtr] as String?)
    }

    @JvmStatic
    fun method1385(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setTargetOpCursor(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1387(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class217Sub13: WorldDescriptor? = WorldDescriptor.getWorld(i_2)
        if (class217Sub13 != null) {
            executor.intStack[++executor.intStackPtr - 1] = class217Sub13!!.flags
            executor.stringStack[++executor.stringStackPtr - 1] = class217Sub13!!.unknown
            var class2134: WorldMeta? = class217Sub13!!.getWorld()
            executor.intStack[++executor.intStackPtr - 1] = class2134!!.countryId
            executor.stringStack[++executor.stringStackPtr - 1] = class2134!!.activity
            executor.intStack[++executor.intStackPtr - 1] = class217Sub13!!.playerCount
            executor.intStack[++executor.intStackPtr - 1] = class217Sub13!!.ping
            executor.stringStack[++executor.stringStackPtr - 1] = class217Sub13!!.ipAddress
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun loadMemoryMode(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.mapSize!!.allowSelection()) 1  else 0
    }

    @JvmStatic
    fun method6205(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = executor.currentClanSettings!!.getSortedAffinedSlots()[executor.intStack[executor.intStackPtr - 1]]
    }

    @JvmStatic
    fun method6206(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.totalPrice
    }

    @JvmStatic
    fun random(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (Math.random() * i_2).toInt()
    }

    @JvmStatic
    fun method6210(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method789(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun strAppend(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        executor.stringStack[++executor.stringStackPtr - 1] = string_2 + string_3
    }

    @JvmStatic
    fun method7929(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method506(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun getCompWidth(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.width
    }

    @JvmStatic
    fun method7932(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (string_2!!.startsWith(TextUtils.getCrown(0)) || string_2!!.startsWith(TextUtils.getCrown(1))) {
            string_2 = string_2!!.substring(7)
        }
        executor.intStack[++executor.intStackPtr - 1] = if (FriendList.contains(string_2!!)) 1  else 0
    }

    @JvmStatic
    fun method7933(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method2741(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method893(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        ifSetModelOrigin(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method894(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var i_4 = -1
        if (chatline3 != null) {
            i_4 = chatline3.time
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun loadInt(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.intLocals!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun method12506(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var bool3 = executor.intStack[executor.intStackPtr + 1] == 1
        var node4: Node<Any?>? = WorldMapSub1.aClass4658029.get(i_2.toLong())
        if (node4 != null && !bool3) {
            node4.unlink()
        } else if (node4 == null && bool3) {
            node4 = object : Node<Any?>() {}
            WorldMapSub1.aClass4658029.put(node4!!, i_2.toLong())
        }
    }

    @JvmStatic
    fun method12560(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.flickeringEffects!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method12561(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 != -1) {
            StaticMethods.method1563(i_2)
        }
    }

    @JvmStatic
    fun method4089(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 3
        var i_3 = executor.intStack[executor.intStackPtr]
        var i_4 = executor.intStack[executor.intStackPtr + 1]
        var i_5 = executor.intStack[executor.intStackPtr + 2]
        StaticMethods.queryItemParamInt(string_2!!, i_3 == 1, i_4, i_5)
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN
    }

    @JvmStatic
    fun method4091(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (WorldMap.staticElement != null) WorldMap.staticElement!!.anInt9536 else -1
    }

    @JvmStatic
    fun method15306() {
        PreferenceManager.loadLowDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method3667(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.activeToolkit!!.getValue()
    }

    @JvmStatic
    fun method14340(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method14653(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method14342(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method789(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method12933(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 >= 1 && i_2 <= 2) {
            WindowMode.requestWindowMode(i_2, -1, -1, false)
        }
    }

    @JvmStatic
    fun intLessThan(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] < executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method8750(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method6313(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8753(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStackPtr -= 2
        var i_4 = executor.intStack[executor.intStackPtr]
        var i_5 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault6: ParamType? = TypeLists.PARAMS!!.list(i_4)
        if (i_5 != attributedefault6!!.defaultInt) {
            icomponentdefinitions3!!.method1999(i_4, i_5)
        } else {
            icomponentdefinitions3!!.method1995(i_4)
        }
    }

    @JvmStatic
    fun method8756(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYERS!![i_2]!!.rank.toInt()
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method8757(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (i_2 == -1) {
            throw RuntimeException()
        } else {
            var enumdefinitions4: EnumType? = TypeLists.ENUM!!.list(i_2)
            if (enumdefinitions4!!.valueType.code != 115) {
                throw RuntimeException()
            } else {
                var ints_5: IntArray? = enumdefinitions4!!.getValueReferences(string_3!!)
                var i_6 = 0
                if (ints_5 != null) {
                    i_6 = ints_5!!.size
                }
                executor.intStack[++executor.intStackPtr - 1] = i_6
            }
        }
    }

    @JvmStatic
    fun getWorldMapParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.MEC!!.list(i_2).getStringParameter(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.MEC!!.list(i_2).getIntParameter(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun method5119(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.method14668().getValue()
    }

    @JvmStatic
    fun pushFogPreference(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.fog!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method5123(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions4!!.scrollY
    }

    @JvmStatic
    fun method14578(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.scrollHeight
    }

    @JvmStatic
    fun ifSetPosition(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setComponentPosition(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method6687(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method14892(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6688(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method6010(icomponentdefinitions4!!, interface5!!, executor)
    }

    @JvmStatic
    fun method6691(executor: CS2Executor) {
        var bool2 = executor.intStack[--executor.intStackPtr] != 0
        var i_3 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = SystemInfo.getCacheCompletionPercent(i_3.toLong(), 0, bool2, Client.CURRENT_LANGUAGE!!)
    }

    @JvmStatic
    fun method6695() {
        LoginManager.method1774()
    }

    @JvmStatic
    fun method5691(executor: CS2Executor) {
        var itemName: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var tradeableOnly = executor.intStack[--executor.intStackPtr]
        StaticMethods.queryItemNoParams(itemName!!, tradeableOnly == 1)
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN
    }

    @JvmStatic
    fun pushGameobjectHintLocation(executor: CS2Executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentLocation as SceneGraphEntity, executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[0].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[1].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[2].toInt()
    }

    @JvmStatic
    fun method5695(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2]!!.worldId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun playSoundSynth(executor: CS2Executor) {
        executor.intStackPtr -= 3
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], 255, 256)
    }

    @JvmStatic
    fun method3169(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.players!![i_2]!!.world
    }

    @JvmStatic
    fun method4161(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = CS2Interpreter.method2598()!!.getValue()
    }

    @JvmStatic
    fun saveAntiAliasingPreference(executor: CS2Executor) {
        PreferenceManager.preferences!!.antiAliasing!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun method14644(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.minRankToKick.toInt()
    }

    @JvmStatic
    fun method12586(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        method2738(executor.currentEntity!!.inter!!, i_2 and 0xffff, i_3, i_4, executor.aBool7022, executor)
    }

    @JvmStatic
    fun method12587(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasVarbitRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun method572(executor: CS2Executor) {
        var bool2 = true
        var string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStack[++executor.intStackPtr - 1] = if (bool2) 1  else 0
    }

    @JvmStatic
    fun method7704(executor: CS2Executor) {
        PacketEncoder.method16088(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun method14503(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        if (i_3 == -1) {
            throw RuntimeException()
        } else {
            var enumdefinitions5: EnumType? = TypeLists.ENUM!!.list(i_3)
            if (i_2 != enumdefinitions5!!.valueType.code) {
                throw RuntimeException()
            } else {
                executor.intStack[++executor.intStackPtr - 1] = if (enumdefinitions5!!.containsKey(i_4)) 1  else 0
            }
        }
    }

    @JvmStatic
    fun resumeCountDialog(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var i_3 = 0
        if (Utils.isWrongJava(string_2!!)) {
            i_3 = Utils.parseInt(string_2!!)
        }
        var tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_COUNTDIALOG, Client.GAME_CONNECTION_CONTEXT!!.isaac)
        tcpmessage4!!.buffer.writeInt(i_3)
        Client.GAME_CONNECTION_CONTEXT!!.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun pushLong(executor: CS2Executor) {
        executor.longStack[++executor.longStackPtr - 1] = executor.current!!.longOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun method5915(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        var i_3 = executor.intStack[executor.intStackPtr]
        var i_4 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = string_2!!.indexOf(i_3.toChar(), i_4)
    }

    @JvmStatic
    fun getObjectParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = Resource.mapRegion!!.getLoctypeList().list(i_2).getParamsString(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Resource.mapRegion!!.getLoctypeList().list(i_2).getParamsInt(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun setBaseIdentiKit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        val playerAppearance = PlayerUpdate.MY_PLAYER!!.playerAppearance
        if (playerAppearance != null) {
            var i_4: Int = 0
            for (i_4 in 0 until anIntArray428.size) {
                if (anIntArray428[i_4] == i_2) {
                    playerAppearance!!.addAppearanceIdentity(i_4, i_3, TypeLists.IDK!!)
                    return
                }
            }
            for (i_4 in 0 until anIntArray422.size) {
                if (anIntArray422[i_4] == i_2) {
                    playerAppearance!!.addAppearanceIdentity(i_4, i_3, TypeLists.IDK!!)
                    break
                }
            }
        }
    }

    @JvmStatic
    fun resumeClanForumQFCDialog(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_CLANFORUMQFCDIALOG, Client.GAME_CONNECTION_CONTEXT!!.isaac)
        tcpmessage3!!.buffer.writeByte(string_2!!.length + 1)
        tcpmessage3!!.buffer.writeString(string_2!!)
        Client.GAME_CONNECTION_CONTEXT!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method2871(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendList.FRIENDS[i_2]!!.worldName
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun setPublicFilter(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.PUBLIC_FILTER
    }

    @JvmStatic
    fun method2873(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.skyboxes!!.getValue()
    }

    @JvmStatic
    fun method4247(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ!!.list(i_2).name
    }

    @JvmStatic
    fun method4249(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            var string_5: String? = attributedefault4!!.typeName
            if (i_2 == -1) {
                executor.stringStack[++executor.stringStackPtr - 1] = string_5
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(i_2).getParamstring(i_3, string_5)
            }
        } else {
            var i_6 = attributedefault4!!.defaultInt
            if (i_2 == -1) {
                executor.intStack[++executor.intStackPtr - 1] = i_6
            } else {
                executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUEST!!.list(i_2).getParamint(i_3, i_6)
            }
        }
    }

    @JvmStatic
    fun method4252(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (StaticMethods.method12402(i_2.toChar())) 1 else 0
    }

    @JvmStatic
    fun method1479(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var bool3 = true
        if (i_2 < 0) {
            bool3 = (i_2 + 1) % 4 == 0
        } else if (i_2 < 1582) {
            bool3 = i_2 % 4 == 0
        } else if (i_2 % 4 != 0) {
            bool3 = false
        } else if (i_2 % 100 != 0) {
        } else if (i_2 % 400 != 0) {
            bool3 = false
        }
        executor.intStack[++executor.intStackPtr - 1] = if (bool3) 1  else 0
    }

    @JvmStatic
    fun method3918(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method8754(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3919(executor: CS2Executor) {
        WorldMapSub1.aBool8027 = executor.intStack[--executor.intStackPtr] == 1
    }

    @JvmStatic
    fun method3921(executor: CS2Executor) {
        var class1842: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        var i_4 = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(1)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.method14896(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.anIntArray4046!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - i_4)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method6138(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method5001(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6139(executor: CS2Executor) {
        var long_2 = executor.longStack[--executor.longStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = if (long_2 == -1L) "" else java.lang.Long.toString(long_2, 36).uppercase()
    }

    @JvmStatic
    fun method6141(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setResumeText(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6142(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.fontId
    }

    @JvmStatic
    fun method289(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteScale
    }

    @JvmStatic
    fun method290(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var worldmapareadefs3: MECType? = TypeLists.MEC!!.list(i_2)
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3!!.anInt2718
    }

    @JvmStatic
    fun method291(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).subCategoryHotkeys!![i_3].code
    }

    @JvmStatic
    fun method292(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.removeRoofs!!.getValue() == 2) 1 else 0
    }

    @JvmStatic
    fun method5733(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.memberWorld) 1  else 0
    }

    @JvmStatic
    fun method5734(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.voiceOverVolume!!.getValue()
    }

    @JvmStatic
    fun method5735(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        var i_3 = executor.intStack[executor.intStackPtr]
        var i_4 = executor.intStack[executor.intStackPtr + 1]
        var fontmetrics5: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_4)
        assert(fontmetrics5 != null)
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics5!!.getTextWidthMultiline(string_2!!, i_3, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun method5736(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr]).sortName
    }

    @JvmStatic
    fun method504(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anInt5904
    }

    @JvmStatic
    fun method1067(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasVarpRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun method1069(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method13468(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method1070(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var bool3 = executor.intStack[--executor.intStackPtr] == 1
        QuickchatMessageType.method1360(string_2!!, bool3)
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN
    }

    @JvmStatic
    fun method1071() {
        Client.method4195()
    }

    @JvmStatic
    fun storeVarp(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        VarProviders.PLAYER_VAR_PROVIDER.setVarValue(i_2, executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun method2737(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (ComponentMinimap.mapAssetLoadingPercentage == 100) 1 else 0
    }

    @JvmStatic
    fun method1357(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt8307
    }

    @JvmStatic
    fun method1362(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2).responses!![i_3]
    }

    @JvmStatic
    fun method11252(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method2741(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3753(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).questpointRequirement
    }

    @JvmStatic
    fun method3755(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var worldmapareadefs3: MECType? = TypeLists.MEC!!.list(i_2)
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3!!.baseTextzoom
    }

    @JvmStatic
    fun method15389(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getContainerTotal(i_2, i_3, false)
    }

    @JvmStatic
    fun method1562(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = Client.getComponentSettings(icomponentdefinitions3!!)!!.getUseOptionFlags()
    }

    @JvmStatic
    fun method2101(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr]).name
    }

    @JvmStatic
    fun method2102(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.memberRanks!![i_2].toInt()
    }

    @JvmStatic
    fun method2103(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as NpcEntity).definitions!!.type
    }

    @JvmStatic
    fun loadVarc(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = VarcTypeList.VARC_INT!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun method6776(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method2955(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5697(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2).method14898()
    }

    @JvmStatic
    fun pow(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Math.pow(i_2.toDouble(), i_3.toDouble()).toInt()
        }
    }

    @JvmStatic
    fun ccSetItemWearColNoNum(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setItemIFComp(icomponentdefinitions3!!, interface4!!, true, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun method2564(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method910(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun sendAClass3794624(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.gameState == GameState.UNK_0 && !LoginManager.method3360()) {
            if (string_2!!.length > 20) {
                Client.aByte7458 = -4
            } else {
                Client.aByte7458 = -1
                var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.DEPRECATED_97_SERVERPACKET_52, Client.LOBBY_CONNECTION_CONTEXT.isaac)
                tcpmessage3!!.buffer.writeByte(0)
                var i_4 = tcpmessage3!!.buffer.currentPosition
                tcpmessage3!!.buffer.writeString(string_2!!)
                tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - i_4)
                Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage3)
            }
        } else {
            Client.aByte7458 = -5
        }
    }

    @JvmStatic
    fun method2567(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method7729(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method2568(executor: CS2Executor) {
        PreferenceManager.preferences!!.flickeringEffects!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method1349(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method2955(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1350(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.characterShadows!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method1351(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (QueueIterator.LOYALTY_ENABLED) 1  else 0
    }

    @JvmStatic
    fun method1352(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.goundBlending!!.canModify(i_2)
    }

    @JvmStatic
    fun method1355(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        Interface.openSub(i_2, SubInterface(i_3, 3), null, true)
    }

    @JvmStatic
    fun ccCreate(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Component.fromHash(i_2)
        method2738(Interface.INTERFACES!![i_2 ushr 16]!!, i_2 and 0xffff, i_3, i_4, executor.aBool7022, executor)
    }

    @JvmStatic
    fun method5296(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method3366(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method5297(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.method5908()
        var ints_4: IntArray? = executor.intStack
        var i_5 = ++executor.intStackPtr - 1
        var b_6: Int
        if (i_3 == 1) {
            b_6 = 1
        } else {
            b_6 = 0
        }
        ints_4!![i_5] = b_6
    }

    @JvmStatic
    fun method5298(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStackPtr -= 2
        var string_3: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_4: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (string_3!!.length <= 500 && string_4!!.length <= 500) {
            PacketEncoder.reportBug(i_2, string_3!!, string_4!!)
        }
    }

    @JvmStatic
    fun method5299(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = CalenderUtil.getLocalYear(Utils.time())
    }

    @JvmStatic
    fun getGEOfferPrice(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.price
    }

    @JvmStatic
    fun getCompText(executor: CS2Executor) {
        var component = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(component)
        assert(icomponentdefinitions3 != null)
        executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3!!.text
    }

    @JvmStatic
    fun method5487(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setTransparency(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5488(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method15256(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5489(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED != 0 && i_2 < IgnoreList.IGNORE_COUNT) {
            var class103: Ignore? = IgnoreList.IGNORED[i_2]
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
    fun method5490(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getContainerTotal(i_2, i_3, true)
    }

    @JvmStatic
    fun method6314(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteId
    }

    @JvmStatic
    fun method6315(executor: CS2Executor) {
        var class1842: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        var i_4 = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(0)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.method14896(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.anIntArray4046!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - i_4)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun getGEOfferItem(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.itemId
    }

    @JvmStatic
    fun method6317(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (GameState.isRenderingScene(Client.gameState)) 1  else 0
    }

    @JvmStatic
    fun ccSetItemWearCol(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setItemIFComp(icomponentdefinitions3!!, interface4!!, true, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun setCompSprite(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method8390(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun getPlayerIsMember(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.IS_MEMBER) 1  else 0
    }

    @JvmStatic
    fun method1138(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var icomponentdefinitions5: Component? = Component.listIcomponent((i_2 shl 16) or i_3, i_4)
        InterfaceComponentRenderer.cancelInterfaceSelection()
        assert(icomponentdefinitions5 != null)
        var class282Sub106: IFEvents? = Client.getComponentSettings(icomponentdefinitions5!!)
        InterfaceRenderer.setUseOptionFlags(icomponentdefinitions5, class282Sub106!!.getUseOptionFlags(), class282Sub106.interfaceId)
    }

    @JvmStatic
    fun method1140(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.goundBlending!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method6310(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setOpBase(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method787(executor: CS2Executor) {
        var class282Sub362: WorldMapCategory? = WorldMap.method11595()
        if (class282Sub362 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub362.mecId
            var i_3 = (class282Sub362!!.anInt7988 shl 28) or class282Sub362.absX + (WorldMap.mapSquareAbsX shl 14) or class282Sub362.absY + WorldMap.mapSquareAbsY
            executor.intStack[++executor.intStackPtr - 1] = i_3
        }
    }

    @JvmStatic
    fun method11403(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method3987(icomponentdefinitions3!!, interface4!!)
    }

    @JvmStatic
    fun method11405(executor: CS2Executor) {
        executor.intStackPtr -= 2
        CursorManager.defaultCursorId = executor.intStack[executor.intStackPtr]
        CursorManager.defaultAttackCursor = executor.intStack[executor.intStackPtr + 1]
    }

    @JvmStatic
    fun method11406(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        val objType = TypeLists.OBJ!!.list(i_2)
        executor.intStack[++executor.intStackPtr - 1] = if (objType.stackable == 1) 1 else 0
    }

    @JvmStatic
    fun ifResumePauseButton(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        method6942(icomponentdefinitions3!!)
    }

    @JvmStatic
    fun method4174(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method7667(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method4175(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method2170(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4176(executor: CS2Executor) {
        var i_2 = (Client.anIntArrayArrayArray7302!![executor.intStack[--executor.intStackPtr]]!!.size shr 1)
        executor.intStack[++executor.intStackPtr - 1] = i_2
    }

    @JvmStatic
    fun method4177(executor: CS2Executor) {
        executor.intStackPtr -= 4
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        var coordgrid6: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        Camera.moveCamera(((i_2 shr 14) and 0x3fff) - coordgrid6!!.x, (i_2 and 0x3fff) - coordgrid6!!.y, (i_3 shl 2), i_4, i_5, false)
    }

    @JvmStatic
    fun method4178(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var long_5 = CalenderUtil.getGmtNoon(i_2, i_3, i_4)
        var i_7 = StaticMethods.method7914(long_5)
        if (i_4 < 1970) {
            --i_7
        }
        executor.intStack[++executor.intStackPtr - 1] = i_7
    }

    @JvmStatic
    fun method4181(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        IgnoreList.remove(string_2)
    }

    @JvmStatic
    fun method6071(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method8390(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method6072(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.bloom!!.visible() && Renderers.activeToolkit!!.hasBloomSupport()) 1 else 0
    }

    @JvmStatic
    fun method6169(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        LoginManager.snLogin(i_2)
    }

    @JvmStatic
    fun intGreaterOrEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] >= executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun getMouseY(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder!!.getMouseY()
    }

    @JvmStatic
    fun method300(executor: CS2Executor) {
        var npc2: NpcEntity? = executor.currentEntity as NpcEntity
        var bool3 = false
        var npcdefinitions4: NPCType? = npc2!!.definitions
        if (npcdefinitions4!!.alternativeIds != null) {
            npcdefinitions4 = npcdefinitions4!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
        }
        if (npcdefinitions4 != null) {
            bool3 = npcdefinitions4!!.visible
        }
        executor.intStack[++executor.intStackPtr - 1] = if (bool3) 1  else 0
    }

    @JvmStatic
    fun method301(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt106
    }

    @JvmStatic
    fun method305(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method6186(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun computeGraphicsMode() {
        if (lastEnteredGraphicsMode == 2) {
            GameShell.softwareMode = true
        } else if (lastEnteredGraphicsMode == 1) {
            GameShell.openglMode = true
        } else if (lastEnteredGraphicsMode == 3) {
            GameShell.directxMode = true
        }
    }

    @JvmStatic
    fun method4200(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStackPtr -= 10
        var bytes_4: ByteArray? = null
        var bytes_5: ByteArray? = null
        var i_6: Int = 0
        while (i_6 < 10 && executor.intStack[i_6 + executor.intStackPtr] >= 0) {
            i_6 += 2
        }
        if (i_6 > 0) {
            bytes_4 = ByteArray(i_6 / 2)
            bytes_5 = ByteArray(i_6 / 2)
            i_6 -= 2
            while (i_6 >= 0) {
                bytes_4[i_6 / 2] = executor.intStack[i_6 + executor.intStackPtr].toByte()
                bytes_5[i_6 / 2] = executor.intStack[i_6 + executor.intStackPtr + 1].toByte()
                i_6 -= 2
            }
        }
        method1838(icomponentdefinitions3!!, bytes_4, bytes_5, executor)
    }

    @JvmStatic
    fun method4201(executor: CS2Executor) {
        var icomponentdefinitions2: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(icomponentdefinitions2 != null)
        if (icomponentdefinitions2!!.containerItemId != -1) {
            executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions2!!.itemStackSize
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method4202(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var itemdefinitions4: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (i_3 >= 1 && i_3 <= 5 && itemdefinitions4!!.inventoryActions!![i_3 - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = itemdefinitions4!!.inventoryActions!![i_3 - 1]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun setIfTextAlign(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setIfTextAlign(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun getAppletFocus(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (GameShell.appletHasFocus) 1  else 0
    }

    @JvmStatic
    fun method6802(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method3987(icomponentdefinitions3!!, interface4!!)
    }

    @JvmStatic
    fun method8327(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method7555(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun storeLong(executor: CS2Executor) {
        executor.longLocals!![executor.intOpValues!![executor.instrPtr]] = executor.longStack[--executor.longStackPtr]
    }

    @JvmStatic
    fun method15523(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method7267(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5016(executor: CS2Executor) {
        var questdefinitions2: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (questdefinitions2!!.questPrerequisiteIds == null) 0 else questdefinitions2.questPrerequisiteIds!!.size
    }

    @JvmStatic
    fun longLessOrEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] <= executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method5922(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings!!.clanName
    }

    @JvmStatic
    fun method5925(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        LoginManager.method2635(string_2!!, string_3!!)
    }

    @JvmStatic
    fun method5926(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.offsetX
    }

    @JvmStatic
    fun method7699(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.aBool7151) 1  else 0
    }

    @JvmStatic
    fun loadClanSettingVarbit(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var class5373: ClanVarSettingsType? = TypeLists.CLAN_VAR_SETTINGS!!.list(i_2)
        if (class5373 == null) {
            throw RuntimeException()
        } else {
            var integer4: Int? = executor.currentClanSettings!!.method1225((Client.CURRENT_GAME!!.id shl 16) or class5373!!.baseVar, class5373!!.startBit, class5373!!.endBit)
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
    fun method8859(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        method3965(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method8863(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.x
    }

    @JvmStatic
    fun method933(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.currentOwner
    }

    @JvmStatic
    fun method934(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (SystemInfo.SYSTEM_INFO!!.ram >= 512 && !Client.isForcedSafemode && !Client.renderWithCpu) 1 else 0
    }

    @JvmStatic
    fun bitOr(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 or i_3
    }

    @JvmStatic
    fun method3692(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.musicVolume!!.getValue()
    }

    @JvmStatic
    fun method3693(executor: CS2Executor) {
        PreferenceManager.preferences!!.water!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 2 else 0)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun clearCompChildren(executor: CS2Executor) {
        var icomponentdefinitions2: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(icomponentdefinitions2 != null)
        icomponentdefinitions2!!.slotChildren = null
        icomponentdefinitions2!!.itemSlots = null
        Component.redrawComponent(icomponentdefinitions2!!)
    }

    @JvmStatic
    fun method3911(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(10, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method1783(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_3 == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).messageForHotkey(i_3.toChar())
        }
    }

    @JvmStatic
    fun method1784(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var underlaydefinition3: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition3 = executor.hookedInterface2
        } else {
            underlaydefinition3 = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (underlaydefinition3!!.load(executor.currentEntity!!.inter, i_2)) 1 else 0
    }

    @JvmStatic
    fun method2953(executor: CS2Executor) {
        var questdefinitions2: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (questdefinitions2!!.levelRequirements == null) 0 else questdefinitions2!!.levelRequirements!!.size
    }

    @JvmStatic
    fun method4773(executor: CS2Executor) {
        if (FriendList.LOADED == 0) {
            executor.intStack[++executor.intStackPtr - 1] = -2
        } else if (FriendList.LOADED == 1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIEND_COUNT
        }
    }

    @JvmStatic
    fun method4774(executor: CS2Executor) {
        var string_2: String? = null
        string_2 = PacketDecoder.HOSTNAME_IDENTIFIER!!.getHostName()
        if (string_2 == null) {
            string_2 = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2
    }

    @JvmStatic
    fun method4775() {
        StaticMethods.method4166()
    }

    @JvmStatic
    fun method4776() {
        PreferenceManager.preferences!!.graphics!!.setValue(0)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun containerTotalParamStack(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getParamBasedContainerTotal(i_2, i_3, true)
    }

    @JvmStatic
    fun loadVarcString(executor: CS2Executor) {
        var string_2: String? = VarcTypeList.VARC_STRING!![executor.intOpValues!![executor.instrPtr]]
        if (string_2 == null) {
            string_2 = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2
    }

    @JvmStatic
    fun popString(executor: CS2Executor) {
        --executor.stringStackPtr
    }

    @JvmStatic
    fun method7771(executor: CS2Executor) {
        var string_2: String? = null
        if (PlayerUpdate.MY_PLAYER != null && PlayerUpdate.MY_PLAYER!!.username != null) {
            string_2 = PlayerUpdate.MY_PLAYER!!.getDisplayName()
        } else {
            string_2 = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2
    }

    @JvmStatic
    fun method7773() {
    	PreferenceManager.loadMidDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method7774(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        WorldMapSub1.reset(i_2, (i_3 shr 14) and 0x3fff, i_3 and 0x3fff, true)
    }

    @JvmStatic
    fun getVarpOld(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as PlayerEntity).playerConfigs.getVarp(i_2)
    }

    @JvmStatic
    fun method8343(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.antiAliasing!!.visible() && Renderers.activeToolkit!!.hasAntiAliasing()) 1 else 0
    }

    @JvmStatic
    fun method8344(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6186(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method3802(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.amount
    }

    @JvmStatic
    fun method3803(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var i_4 = -1
        if (chatline3 != null) {
            i_4 = chatline3.identifier
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun method3804(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (WindowMode.fullScreenSupported) {
            executor.intStack[++executor.intStackPtr - 1] = if (WindowMode.fullScreenFrame != null) 1 else 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun ifSetItemWearColAlwaysNum(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setItemIFComp(icomponentdefinitions3!!, interface4!!, true, Component.StackMode.ALWAYS_NUM, executor)
    }

    @JvmStatic
    fun method4556(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).hasAllRequirements(VarProviders.PLAYER_VAR_PROVIDER, Client.SKILL_LEVEL_ACTUAL)) 1 else 0
    }

    @JvmStatic
    fun method4557(executor: CS2Executor) {
        executor.intStackPtr -= 3
        SongPlayer.playSong(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2])
    }

    @JvmStatic
    fun method4558(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method505(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun stringEqual(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStack[++executor.intStackPtr - 1] = TextUtils.method7557(executor.stringStack[executor.stringStackPtr] as CharSequence, executor.stringStack[executor.stringStackPtr + 1] as CharSequence, Client.CURRENT_LANGUAGE!!)
    }

    @JvmStatic
    fun toString(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = Integer.toString(i_2)
    }

    @JvmStatic
    fun method8215(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        if (icomponentdefinitions3!!.opBase == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3!!.opBase
        }
    }

    @JvmStatic
    fun method8216(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).isStarted(VarProviders.PLAYER_VAR_PROVIDER)) 1 else 0
    }

    @JvmStatic
    fun method8218(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (TextUtils.isAlphabeticalOrNumeric(i_2.toChar())) 1 else 0
    }

    @JvmStatic
    fun method8219(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt5760
        executor.intStack[++executor.intStackPtr - 1] = ComponentMinimap.anInt4097
    }

    @JvmStatic
    fun method8220(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![i_2  ushr  16]
        assert(icomponentdefinitions3 != null)
        var icomponentdefinitions5: Component? = Interface.getSub(interface4!!, icomponentdefinitions3!!)
        var ints_6: IntArray? = executor.intStack
        var i_7 = ++executor.intStackPtr - 1
        var i_8: Int = 0
        if (icomponentdefinitions5!! == null) {
            i_8 = -1
        } else {
            i_8 = icomponentdefinitions5!!.idHash
        }
        ints_6!![i_7] = i_8
    }

    @JvmStatic
    fun method1631(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method14586(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun ifSetDepthChild(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        setDepthChild(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4839(executor: CS2Executor) {
        --executor.intStackPtr
        var i_2 = executor.intStack[executor.intStackPtr]
        var class282Sub443: SubInterface? = Client.OPEN_INTERFACES!!.get(i_2.toLong())
        if (class282Sub443 != null && class282Sub443.overlay == 3) {
            Interface.closeChildInterface(class282Sub443, true, true)
        }
    }

    @JvmStatic
    fun method6395(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method5009(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method6396(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        StaticMethods.method15430(i_2)
    }

    @JvmStatic
    fun method6397(executor: CS2Executor) {
        var i_2 = PreferenceManager.preferences!!.removeRoofs!!.getValue()
        PreferenceManager.preferences!!.removeRoofsOptionOverride!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 0 else i_2)
        Scene.setRoofOverride()
    }

    @JvmStatic
    fun method6399(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = 0
    }

    @JvmStatic
    fun method6400(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var i_3 = executor.intStack[executor.intStackPtr]
        var i_4 = executor.intStack[executor.intStackPtr + 1]
        var string_5: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        CS2Executor.queryItemParamString(string_2!!, i_3 == 1, i_4, string_5!!)
        executor.intStack[++executor.intStackPtr - 1] = CS2_QUERY_RESULTS_LEN
    }

    @JvmStatic
    fun method13044(executor: CS2Executor) {
        if (FriendsChat.FC_OWNER_NAME != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_OWNER_NAME
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
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
    fun method3611(executor: CS2Executor) {
        PreferenceManager.preferences!!.sceneryShadows!!.setValue(executor.intStack[--executor.intStackPtr])
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method3613() {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
        }
    }

    @JvmStatic
    fun divide(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 / i_3
    }

    @JvmStatic
    fun method5765(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).wearPos2
    }

    @JvmStatic
    fun method5766(executor: CS2Executor) {
        PreferenceManager.preferences!!.mapSize!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method5767(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.particles!!.supported()) 1  else 0
    }

    @JvmStatic
    fun method5769(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (WindowMode.fullScreenSupported) {
            var arr_3 = WindowMode.getActiveDisplayModeDetails()
            executor.intStack[++executor.intStackPtr - 1] = arr_3!![i_2].width
            executor.intStack[++executor.intStackPtr - 1] = arr_3!![i_2].height
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method6174(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteYaw
    }

    @JvmStatic
    fun method15448(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method774(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method11438(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method3078(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method11439(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = (i_2 shr 14) and 0x3fff
        var i_4 = i_2 and 0x3fff
        var coordgrid5: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        i_3 -= coordgrid5!!.x
        if (i_3 < 0) {
            i_3 = 0
        } else if (i_3 >= Resource.mapRegion!!.sceneSizeX) {
            i_3 = Resource.mapRegion!!.sceneSizeX
        }
        i_4 -= coordgrid5!!.y
        if (i_4 < 0) {
            i_4 = 0
        } else if (i_4 >= Resource.mapRegion!!.sceneSizeY) {
            i_4 = Resource.mapRegion!!.sceneSizeY
        }
        Client.anInt7262 = ((i_3 shl 9)) + 256
        Client.anInt7376 = ((i_4 shl 9)) + 256
        Camera.cameraCalculationType = 4
        StaticMethods.anInt833 = -1
        PingRequester.anInt5864 = -1
    }

    @JvmStatic
    fun method11440(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(1, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method4965(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method4160(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4966(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var enumdefinitions4: EnumType? = TypeLists.ENUM!!.list(i_2)
        executor.stringStack[++executor.stringStackPtr - 1] = enumdefinitions4!!.getStringValue(i_3)
    }

    @JvmStatic
    fun fromDate(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.method13045(CalenderUtil.method5363(executor.intStack[--executor.intStackPtr]), Client.CURRENT_LANGUAGE!!.getValue())
    }

    @JvmStatic
    fun method4968(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        PacketEncoder.kickPlayer(executor.clanChannel == Client.CLAN_CHANNEL, i_2)
    }

    @JvmStatic
    fun method4970(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6313(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4971(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method910(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5197(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getAmountAtSlot(i_2, i_3, false)
    }

    @JvmStatic
    fun method5198(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (TypeLists.OBJ!!.list(i_2).membersOnly) 1  else 0
    }

    @JvmStatic
    fun method5199(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 != -1) {
            StaticMethods.setVorbisSound(i_2)
        }
    }

    @JvmStatic
    fun method563(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        ifSetModelOrigin(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method564(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method4651(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method565(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var str4: String? = ""
        if (chatline3 != null && chatline3.simpleName != null) {
            str4 = chatline3.simpleName
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun method6851(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        method14605(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1391(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = WorldMapSub1.anInt5123 + WorldMap.mapSquareAbsX
        executor.intStack[++executor.intStackPtr - 1] = WorldMapSub1.anInt5624 + WorldMap.mapSquareAbsY
    }

    @JvmStatic
    fun setCCTextAlign(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setIfTextAlign(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6272(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var ints_3: IntArray? = CalenderUtil.getLocalDdmmyyyy(i_2)
        ArrayUtils.arrayCopy(ints_3!!, 0, executor.intStack, executor.intStackPtr, 3)
        executor.intStackPtr += 3
    }

    @JvmStatic
    fun method6275(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.interfaceSelectionActive) 1  else 0
        executor.stringStack[++executor.stringStackPtr - 1] = if (Client.useOnTarget == null) "" else Client.useOnTarget
        executor.stringStack[++executor.stringStackPtr - 1] = if (Client.useOnType == null) "" else Client.useOnType
    }

    @JvmStatic
    fun method16079(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Camera.setAngle(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], 0)
    }

    @JvmStatic
    fun method1624(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method569(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1625(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && i_2 >= 0 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2]!!.platform
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method1627() {
        StaticMethods.method16114()
    }

    @JvmStatic
    fun arrayNew(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr] shr 16
        var i_3 = executor.intOpValues!![executor.instrPtr] and 0xffff
        var i_4 = executor.intStack[--executor.intStackPtr]
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
    fun subtract(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 - i_3
    }

    @JvmStatic
    fun method2963(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var node3: Node<Any?>? = WorldMapSub1.aClass4658029.get(i_2.toLong())
        executor.intStack[++executor.intStackPtr - 1] = if (node3 != null) 1 else 0
    }

    @JvmStatic
    fun method2965(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method8387(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method1802(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.memberCount
    }

    @JvmStatic
    fun method1803(executor: CS2Executor) {
        PreferenceManager.preferences!!.goundBlending!!.setValue(if (executor.intStack[--executor.intStackPtr] != 0) 1 else 0)
        PreferenceManager.savePreferences()
        Resource.mapRegion!!.reload()
    }

    @JvmStatic
    fun strAppendSigNum(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var i_3 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = string_2 + TextUtils.createNumberCenteredWithinText(i_3, true)
    }

    @JvmStatic
    fun method927(executor: CS2Executor) {
        var i_2 = Client.OPEN_INTERFACES!!.size()
        if (Client.BASE_WINDOW_ID != -1) {
            ++i_2
        }
        executor.intStack[++executor.intStackPtr - 1] = i_2
    }

    @JvmStatic
    fun method928(executor: CS2Executor) {
        if (KeyRecorder.keyRecorder!!.held(82)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method2586(executor: CS2Executor) {
        PreferenceManager.preferences!!.cpu!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun method2587(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.aShort7436.toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.aShort7437.toInt()
    }

    @JvmStatic
    fun method2588(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.water!!.display()) 1  else 0
    }

    @JvmStatic
    fun ifSetOnMouseLeave(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        setOnMouseLeave(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6194(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        PacketEncoder.method2075(i_2, string_3)
    }

    @JvmStatic
    fun method6195(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasingPreview!!.getValue()
    }

    @JvmStatic
    fun longNotEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr] != executor.longStack[executor.longStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method2855(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method5017(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method2856(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var position = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var class4774: Queue<WorldMapElement>? = WorldMap.getRenderedElements((position shr 14) and 0x3fff, position and 0x3fff)
        var bool5 = false
        var class282Sub50Sub66: WorldMapElement? = class4774?.getNext() as WorldMapElement?
        while (class282Sub50Sub66 != null) {
            if (i_3 == class282Sub50Sub66.anInt9536) {
                bool5 = true
                break
            }
            class282Sub50Sub66 = class4774?.getDirectNext() as WorldMapElement?
        }
        if (bool5) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method3768(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        StaticMethods.method14263((i_2 shr 14) and 0x3fff, i_2 and 0x3fff)
    }

    @JvmStatic
    fun method3769(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentGroundItem!!.getIconHeight()
    }

    @JvmStatic
    fun method14491(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = FriendsChat.MY_PLAYER_FC_RANK.toInt()
    }

    @JvmStatic
    fun method14492(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).messages!![i_3]
    }

    @JvmStatic
    fun method283(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        ifSetAspect(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method284(executor: CS2Executor) {
        var bool2 = executor.intStack[--executor.intStackPtr] == 1
        PreferenceManager.preferences!!.removeRoofs!!.setValue(if (bool2) 2  else 1)
        PreferenceManager.preferences!!.removeRoofsOptionOverride!!.setValue(if (bool2) 2  else 1)
        Scene.setRoofOverride()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method287(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ((Utils.time() / 86400000L) - 11745).toInt()
    }

    @JvmStatic
    fun method481(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.graphics!!.getValue()
    }

    @JvmStatic
    fun method484(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method4160(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method485(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method1642(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method14606(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.GAME_SCREEN_INTERFACE == null) -1 else Client.GAME_SCREEN_INTERFACE!!.idHash
    }

    @JvmStatic
    fun method7322(executor: CS2Executor) {
        method11160(MiniMenu.method8727()!!, executor)
    }

    @JvmStatic
    fun method6762(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method3338(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8723(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method3338(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8724(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (Interface.INTERFACES!![i_2] == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Interface.INTERFACES!![i_2]!!.components!![i_3]!!.serverTriggers
        }
    }

    @JvmStatic
    fun method8726(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method797(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method7270(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method502(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method7274(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.lootshareRank.toInt()
    }

    @JvmStatic
    fun method7276(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub63: WorldMapElement? = WorldMap.poll(i_2)
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = (class282Sub50Sub63.bitpackedPlacement shr 14) and 0x3fff
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.bitpackedPlacement and 0x3fff
        }
    }

    @JvmStatic
    fun method7277() {
        StaticMedia.resetChatMediaAndIcons()
        FontMetrics.useBoldFont = false
    }

    @JvmStatic
    fun method2091(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var quickchatdefinitions5: QuickchatMessageType? = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2)
        if (quickchatdefinitions5!!.getValueType(i_3)!!.id != 0) {
            throw RuntimeException("")
        } else {
            executor.intStack[++executor.intStackPtr - 1] = quickchatdefinitions5!!.method14901(i_3, i_4)
        }
    }

    @JvmStatic
    fun method2092(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).levelRequirements!![executor.intStack[executor.intStackPtr - 1]][0]
        --executor.intStackPtr
    }

    @JvmStatic
    fun getVarnbitOld(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as NpcEntity).varns.getVarnBit(i_2)
    }

    @JvmStatic
    fun method2096(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            var class63: Friend? = FriendList.FRIENDS[i_2]
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
    fun ccSetHide(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method786(icomponentdefinitions4!!, interface5!!, executor)
    }

    @JvmStatic
    fun method2639(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var npc3: NpcEntity? = executor.currentEntity as NpcEntity
        var i_4 = npc3!!.getStat(i_2)
        var i_5 = npc3!!.getCurrentStat(i_2)
        executor.intStack[++executor.intStackPtr - 1] = i_4
        executor.intStack[++executor.intStackPtr - 1] = i_5
    }

    @JvmStatic
    fun animateComp(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method797(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method8742() {
        Camera.method12606()
    }

    @JvmStatic
    fun returnInstr(executor: CS2Executor) {
        if (executor.returnValuesPtr != 0) {
            var class5092: CS2ReturnValue? = executor.returnValues[--executor.returnValuesPtr]
            executor.current = class5092!!.aCacheableNodeSub55869
            executor.operations = executor.current!!.operations
            executor.intOpValues = executor.current!!.intOpValues
            executor.instrPtr = class5092!!.anInt5866
            executor.intLocals = class5092!!.anIntArray5867
            executor.objectLocals = class5092!!.anObjectArray5865
            executor.longLocals = class5092!!.aLongArray5868
        }
    }

    @JvmStatic
    fun pushGroundItemHintPosition(executor: CS2Executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentGroundItem!! as SceneGraphEntity, executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[0].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[1].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[2].toInt()
    }

    @JvmStatic
    fun switchInstr(executor: CS2Executor) {
        var iterablenodemap2: IterableNodeMap<IntNode>? = executor.current!!.switchMaps!![executor.intOpValues!![executor.instrPtr]]
        var class282Sub383: IntNode? = iterablenodemap2?.get(executor.intStack[--executor.intStackPtr].toLong())
        if (class282Sub383 != null) {
            executor.instrPtr += class282Sub383.value
        }
    }

    @JvmStatic
    fun method5314(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        ifSetColor(icomponentdefinitions4!!, interface5!!, executor)
    }

    @JvmStatic
    fun callCS2Script(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var cs2script3: CS2Script? = CS2Script.getCs2Script(i_2)
        if (cs2script3 == null) {
            throw RuntimeException()
        } else {
            var ints_4: IntArray? = IntArray(cs2script3!!.intLocalsCount)
            var arr_5: Array<Any?>? = arrayOfNulls<Any>(cs2script3!!.stringLocalsCount)
            var longs_6: LongArray? = LongArray(cs2script3!!.longLocalsCount)
            var i_7: Int = 0
            for (i_7 in 0 until cs2script3!!.intArgsCount) {
                ints_4!![i_7] = executor.intStack[i_7 + (executor.intStackPtr - cs2script3!!.intArgsCount)]
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
            var class5098: CS2ReturnValue? = CS2ReturnValue()
            class5098!!.aCacheableNodeSub55869 = executor.current
            class5098!!.anInt5866 = executor.instrPtr
            class5098!!.anIntArray5867 = executor.intLocals
            class5098!!.anObjectArray5865 = executor.objectLocals
            class5098!!.aLongArray5868 = executor.longLocals
            if (executor.returnValuesPtr >= executor.returnValues!!.size) {
                throw RuntimeException()
            } else {
                executor.returnValues[++executor.returnValuesPtr - 1] = class5098
                executor.current = cs2script3
                executor.operations = executor.current!!.operations
                executor.intOpValues = executor.current!!.intOpValues
                executor.instrPtr = -1
                executor.intLocals = ints_4
                executor.objectLocals = arr_5
                executor.longLocals = longs_6
            }
        }
    }

    @JvmStatic
    fun method15449(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.bloom!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method2549(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = DisplayModeDetails.getProfilingPerformance(PreferenceManager.preferences!!.activeToolkit!!.getValue(), 200)
    }

    @JvmStatic
    fun method2552(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.CURRENT_CURSOR
    }

    @JvmStatic
    fun method2554(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = PreferenceManager.preferences!!.musicVolume!!.getValue()
        if (i_2 != i_3) {
            if (GameState.isRenderingScene(Client.gameState)) {
                if (i_3 == 0 && AreaSoundPlayer.currentMusicArchive != -1) {
                    AreaSoundPlayer.method13400(Resource.MUSIC!!, AreaSoundPlayer.currentMusicArchive, i_2)
                    SongPlayer.method12658()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else if (i_2 == 0) {
                	SoundResourceManager.discardSongData()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else {
                    SoundEffectPlayer.method1491(i_2)
                }
            }
            PreferenceManager.preferences!!.musicVolume!!.setValue(i_2)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun method7032(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.width
    }

    @JvmStatic
    fun method14659(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.scrollHeight
    }

    @JvmStatic
    fun method14660(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        method1494(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun setCompTransparency(executor: CS2Executor) {
        var interfaceId = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(interfaceId)
        var interface4: Interface? = Interface.INTERFACES!![(interfaceId shr 16)]
        assert(icomponentdefinitions3 != null)
        setTransparency(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1259(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method13450(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun removetags(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var stringbuilder3: StringBuilder? = StringBuilder(string_2!!.length)
        var bool4 = false
        for (i_5 in 0 until string_2!!.length) {
            val var_6: Char = string_2!![i_5]
            if (var_6 == '<') {
                bool4 = true
            } else if (var_6 == '>') {
                bool4 = false
            } else if (!bool4) {
                stringbuilder3!!.append(var_6)
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = stringbuilder3!!.toString()
    }

    @JvmStatic
    fun method3938(executor: CS2Executor) {
        executor.intStackPtr -= 2
    }

    @JvmStatic
    fun method3939() {
        PacketEncoder.closeInterface()
    }

    @JvmStatic
    fun method3940(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method6689(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun ccSetDepthChild(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setDepthChild(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun setMonoStero(executor: CS2Executor) {
        PreferenceManager.preferences!!.monoStereo!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        SoundEffectPlayer.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method2823(executor: CS2Executor) {
        executor.intStackPtr -= 4
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], false, 256)
    }

    @JvmStatic
    fun method2824(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var str4: String? = ""
        if (chatline3 != null && chatline3.message != null) {
            str4 = chatline3.message
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun method2825(executor: CS2Executor) {
        if (Client.base64Url != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method2826(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method12260(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun getItemParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.OBJ!!.list(i_2).getCs2String(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).getCs2Integer(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun method2729(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.CONTAINER!!.list(i_2).maxItemSize
    }

    @JvmStatic
    fun ccSetNoClickThrough(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setNoClickThrough(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3164(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (IgnoreList.IGNORED!![i_2]!!.temporary) 1  else 0
    }

    @JvmStatic
    fun method3168(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        var icomponentdefinitions5: Component? = Interface.getSub(interface4!!, icomponentdefinitions3!!)
        executor.intStack[++executor.intStackPtr - 1] = if (icomponentdefinitions5!! == null) -1 else icomponentdefinitions5!!.idHash
    }

    @JvmStatic
    fun ccSetSize(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method12260(icomponentdefinitions4!!, interface5!!, executor)
    }

    @JvmStatic
    fun method6117(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var i_3 = executor.intStack[--executor.intStackPtr]
        if (i_3 == -1) {
            throw RuntimeException("")
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2 + i_3.toChar()
        }
    }

    @JvmStatic
    fun method6120(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method1083(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6121(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = MiniMenu.entryCount
        executor.intStack[++executor.intStackPtr - 1] = MiniMenu.anInt170
    }

    @JvmStatic
    fun method1084(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6158(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1085(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.currentQuickChatMessage!!.anIntArray4046!![i_2] = i_3
    }

    @JvmStatic
    fun storeInt(executor: CS2Executor) {
        executor.intLocals!![executor.intOpValues!![executor.instrPtr]] = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun method13057(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (i_2 shr 14) and 0x3fff
    }

    @JvmStatic
    fun method1467(executor: CS2Executor) {
        if (Client.PLAYER_MOD_LEVEL >= 5 && Client.PLAYER_MOD_LEVEL <= 9) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method2169(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method869(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method2171() {
    }

    @JvmStatic
    fun method6439(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2).getValueType(i_3)!!.id
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun method6440(executor: CS2Executor) {
        var str2: String? = ""
        if (Console.clipboard != null) {
            var transferable3: Transferable? = Console.clipboard!!.getContents(null)
            if (transferable3 != null) {
                try {
                    str2 = transferable3.getTransferData(DataFlavor.stringFlavor) as? String
                } catch (ignored: Exception) {
                }
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str2
    }

    @JvmStatic
    fun method5045(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.parentId
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
    fun method5047(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        PreferenceManager.preferences!!.brightness!!.setValue(i_2)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method4413(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (!Renderers.activeToolkit!!.hasBloomSupport()) {
            executor.intStack[++executor.intStackPtr - 1] = 3
        } else {
            executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.bloom!!.canModify(i_2)
        }
    }

    @JvmStatic
    fun method4417(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt103
    }

    @JvmStatic
    fun method12881(executor: CS2Executor) {
        if (Client.CLAN_SETTINGS != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
            executor.currentClanSettings = Client.CLAN_SETTINGS
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method11350(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.offsetX
    }

    @JvmStatic
    fun method11351(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.sceneryShadows!!.canModify(i_2)
    }

    @JvmStatic
    fun method11355(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS!![i_2]!!.worldName
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun method7676(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var i_4 = -1
        var i_5 = -1
        var class1196: InteractiveComponentSprite? = icomponentdefinitions3!!.addInteractiveSprite(Renderers.activeToolkit!!)
        if (class1196 != null) {
            i_4 = class1196.width
            i_5 = class1196.height
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
        executor.intStack[++executor.intStackPtr - 1] = i_5
    }

    @JvmStatic
    fun method892(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = if (icomponentdefinitions3!!.hidden) 1  else 0
    }

    @JvmStatic
    fun method6212(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteId
    }

    @JvmStatic
    fun bitFlagged(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = if ((i_2 and (1 shl i_3)) != 0) 1 else 0
    }

    @JvmStatic
    fun method11592(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).varbitRequirementNames!![executor.intStack[executor.intStackPtr - 1]]
        executor.intStackPtr -= 2
    }

    @JvmStatic
    fun method11593(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method4772(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method11594(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_SUBMIT_CODE, Client.LOBBY_CONNECTION_CONTEXT.isaac)
        tcpmessage3!!.buffer.writeByte(TextUtils.getLength(string_2!!))
        tcpmessage3!!.buffer.writeString(string_2!!)
        Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method7715(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.currentAmount
    }

    @JvmStatic
    fun method5332(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method8310(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5335(executor: CS2Executor) {
        executor.intStackPtr -= 5
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], false, executor.intStack[executor.intStackPtr + 4])
    }

    @JvmStatic
    fun method7082(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.monoStereo!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method7083(executor: CS2Executor) {
        var questdefinitions2: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (questdefinitions2!!.varbitRequirements == null) 0 else questdefinitions2!!.varbitRequirements!!.size
    }

    @JvmStatic
    fun method584(executor: CS2Executor) {
        CommandExecutor.executeCommand(executor.stringStack[--executor.stringStackPtr] as String, false, false)
    }

    @JvmStatic
    fun method585(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.lightDetail!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method586(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.PLAYER_DOB
    }

    @JvmStatic
    fun method587() {
    }

    @JvmStatic
    fun method1949(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method569(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1951(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.method5908()
        executor.intStack[++executor.intStackPtr - 1] = if (i_3 == 5) 1 else 0
    }

    @JvmStatic
    fun method4898(executor: CS2Executor) {
        var icomponentdefinitions2: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(icomponentdefinitions2 != null)
        if (icomponentdefinitions2!!.slotChildren == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            var i_3 = icomponentdefinitions2!!.slotChildren!!.size
            for (i_4 in 0 until icomponentdefinitions2!!.slotChildren!!.size) {
                if (icomponentdefinitions2!!.slotChildren!![i_4] == null) {
                    i_3 = i_4
                    break
                }
            }
            executor.intStack[++executor.intStackPtr - 1] = i_3
        }
    }

    @JvmStatic
    fun method4899(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = PreferenceManager.preferences!!.soundLevel!!.getValue()
        if (i_3 != i_2 && AreaSoundPlayer.titlescreenMusicArchive == AreaSoundPlayer.currentMusicArchive) {
            if (!GameState.isRenderingScene(Client.gameState)) {
                if (i_3 == 0) {
                    AreaSoundPlayer.method13400(Resource.MUSIC!!, AreaSoundPlayer.currentMusicArchive, i_2)
                    SongPlayer.method12658()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else if (i_2 == 0) {
                	SoundResourceManager.discardSongData()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else {
                    SoundEffectPlayer.method1491(i_2)
                }
            }
            PreferenceManager.preferences!!.soundLevel!!.setValue(i_2)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun method4900(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.textures!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method4901(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.scrollWidth
    }

    @JvmStatic
    fun method4902(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        PacketEncoder.method3345(string_2)
    }

    @JvmStatic
    fun method15437(executor: CS2Executor) {
        if (Client.gameState == GameState.UNK_0 && !LoginManager.method3360()) {
            if (WorldConnection.requestingWorldList) {
                executor.intStack[++executor.intStackPtr - 1] = 0
            } else if (WorldConnection.worldListReceivedTime > Utils.time() - 1000L) {
                executor.intStack[++executor.intStackPtr - 1] = 1
            } else {
                WorldConnection.requestingWorldList = true
                var tcpmessage2: TcpPacket? = PacketEncoder.createPacket(ClientProt.REQUEST_WORLD_LIST, Client.LOBBY_CONNECTION_CONTEXT.isaac)
                tcpmessage2!!.buffer.writeInt(StaticMethods.WORLD_LIST_VERSION)
                Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage2)
                executor.intStack[++executor.intStackPtr - 1] = 0
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 1
        }
    }

    @JvmStatic
    fun containerTotalParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getParamBasedContainerTotal(i_2, i_3, false)
    }

    @JvmStatic
    fun method3229(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).multiStackSize
    }

    @JvmStatic
    fun ifSetfont(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setIfFont(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method3233() {
        PreferenceManager.loadHighDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method11492(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method7267(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6304(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2).method14916()
    }

    @JvmStatic
    fun method6305(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method7927(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method11473(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.scrollY
    }

    @JvmStatic
    fun method3200(executor: CS2Executor) {
        PreferenceManager.preferences!!.voiceOverVolume!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method3201(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        method15207(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun loadMessagebox(executor: CS2Executor) {
        executor.intStackPtr -= 11
        val anchorX = AnchorX.values()
        val anchorY = AnchorY.values()
        MessageBox.initialize(
        		anchorX[executor.intStack[executor.intStackPtr]],
        		anchorY[executor.intStack[executor.intStackPtr + 1]],
        		executor.intStack[executor.intStackPtr + 2], 
        		executor.intStack[executor.intStackPtr + 3], 
        		executor.intStack[executor.intStackPtr + 4], 
        		executor.intStack[executor.intStackPtr + 5], 
        		executor.intStack[executor.intStackPtr + 6], 
        		executor.intStack[executor.intStackPtr + 7], 
        		executor.intStack[executor.intStackPtr + 8], 
        		executor.intStack[executor.intStackPtr + 9], 
        		executor.intStack[executor.intStackPtr + 10])
    }

    @JvmStatic
    fun method6908(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.toolkit!!.getValue()
    }

    @JvmStatic
    fun method1497(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method2915(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1498(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.offsetY
    }

    @JvmStatic
    fun method1499(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (LoginManager.MEMBERSHIP_END / 60000L).toInt()
        executor.intStack[++executor.intStackPtr - 1] = ((LoginManager.MEMBERSHIP_END - Utils.time() - LoginManager.aLong420) / 60000L).toInt()
        executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.IS_MEMBER_SUBSCRIPTION) 1 else 0
    }

    @JvmStatic
    fun method1500(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        FriendList.add(string_2)
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
    fun method14602(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.fontId
    }

    @JvmStatic
    fun bitAnd(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 and i_3
    }

    @JvmStatic
    fun method8303(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt106
    }

    @JvmStatic
    fun ifSetOnMouseOver(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        setOnMouseOver(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6738(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method13450(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6741(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.memberJoinDays!![i_2]
    }

    @JvmStatic
    fun method541(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setResumeText(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method543(executor: CS2Executor) {
        executor.currentEntity!!.method15813(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun setBit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 or (1 shl i_3)
    }

    @JvmStatic
    fun method546(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.aByte7458.toInt()
        if (Client.aByte7458.toInt() != -1) {
            Client.aByte7458 = -6
        }
    }

    @JvmStatic
    fun method548(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(9, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun charIsnumeric(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (Utils.isNumber(i_2.toChar())) 1 else 0
    }

    @JvmStatic
    fun method8018(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method7142(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8019(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED != 0 && i_2 < IgnoreList.IGNORE_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = IgnoreList.IGNORED!![i_2]!!.unfilteredUsername
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun method8021(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(5, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method8045(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var i_4 = executor.intStack[--executor.intStackPtr]
        --i_4
        assert(icomponentdefinitions3 != null)
        if (icomponentdefinitions3!!.options != null && i_4 < icomponentdefinitions3!!.options!!.size && icomponentdefinitions3!!.options!![i_4] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3!!.options!![i_4]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun method1813(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method11221(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun chatlineGeteffectflags(executor: CS2Executor) {
        var line = executor.intStack[--executor.intStackPtr]
        var chatline: ChatLine? = ChatBox.getLine(line)
        var effectFlags = 0
        if (chatline != null) {
            effectFlags = chatline.effectFlags
        }
        executor.intStack[++executor.intStackPtr - 1] = effectFlags
    }

    @JvmStatic
    fun method1815(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Client.aShort7394 = executor.intStack[executor.intStackPtr].toShort()
        if (Client.aShort7394 <= 0) {
            Client.aShort7394 = 256
        }
        Client.aShort7324 = executor.intStack[executor.intStackPtr + 1].toShort()
        if (Client.aShort7324 <= 0) {
            Client.aShort7324 = 320
        }
    }

    @JvmStatic
    fun getJcoins(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.JCOINS
    }

    @JvmStatic
    fun getColTag(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TextUtils.setTextColor(i_2)
    }

    @JvmStatic
    fun method7332(executor: CS2Executor) {
        getBoundingBox(executor.currentLocation!! as SceneGraphEntity, executor)
    }

    @JvmStatic
    fun method7334(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.customCursor!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun method12846(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6697(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method7726(executor: CS2Executor) {
        executor.intStackPtr -= 4
        var i_2 = executor.intStack[executor.intStackPtr]
        var bool3 = executor.intStack[executor.intStackPtr + 1] == 1
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var bool5 = executor.intStack[executor.intStackPtr + 3] == 1
        WorldConnection.method6299(i_2, bool3, i_4, bool5)
    }

    @JvmStatic
    fun setBloom(executor: CS2Executor) {
        var enabled = executor.intStack[--executor.intStackPtr]
        if (enabled < 0 || enabled > 1) {
            enabled = 0
        }
        PreferenceManager.setBloom(enabled == 1)
    }

    @JvmStatic
    fun method1509(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method4159(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1510(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method5066(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun ccSetTargetVerb(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setTargetVerb(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1512(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = if (icomponentdefinitions3!!.mediaType == MediaType.RAW_MODEL) icomponentdefinitions3!!.primaryMediaId else -1
    }

    @JvmStatic
    fun arrayStore(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStackPtr -= 2
        var i_3 = executor.intStack[executor.intStackPtr]
        if (i_3 >= 0 && i_3 < executor.globalArrayLengths[i_2]) {
            executor.globalArrays[i_2][i_3] = executor.intStack[executor.intStackPtr + 1]
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun method4904(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method3365(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4905(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYERS!![i_2]!!.worldId
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun pushEntityHintPosition(executor: CS2Executor) {
        HintRenderer.calculateSceneobjectHintProjection(executor.currentEntity!! as SceneGraphEntity, executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[0].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[1].toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.HINT_POSITION[2].toInt()
    }

    @JvmStatic
    fun method5814(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var underlaydefinition4: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition4 = executor.hookedInterface2
        } else {
            underlaydefinition4 = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (underlaydefinition4!!.load(executor.currentEntity!!.inter, i_2)) 1 else 0
    }

    @JvmStatic
    fun method5816() {
        LoginManager.prepareHost()
    }

    @JvmStatic
    fun method2150(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method869(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun getStructParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.STRUCT!!.getStruct(i_2).getString(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.STRUCT!!.getStruct(i_2).getInteger(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun method2153(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt113
    }

    @JvmStatic
    fun getContainerFreeSpace(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getFreeSlots(i_2)
    }

    @JvmStatic
    fun method3986(executor: CS2Executor) {
        if (ColorImageCache.CS2_QUERY_RESULTS != null && StaticElements.CS2_QUERY_RESULT_IDX < CS2_QUERY_RESULTS_LEN) {
            executor.intStack[++executor.intStackPtr - 1] = ColorImageCache.CS2_QUERY_RESULTS!![++StaticElements.CS2_QUERY_RESULT_IDX - 1].toInt() and 0xffff
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun method15416(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method12209(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6733(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var str4: String? = ""
        if (chatline3 != null && chatline3.clan != null) {
            str4 = chatline3.clan
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun getVarnOld(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as NpcEntity).varns.getVarn(i_2)
    }

    @JvmStatic
    fun loadLong(executor: CS2Executor) {
        executor.longStack[++executor.longStackPtr - 1] = executor.longLocals!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun method13491(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.parentId
    }

    @JvmStatic
    fun getWorldLanguage(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.CURRENT_LANGUAGE!!.getValue()
    }

    @JvmStatic
    fun method4194(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (CalenderUtil.method5363(i_2) / 60000L).toInt()
    }

    @JvmStatic
    fun method6236(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteRoll
    }

    @JvmStatic
    fun method6237(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var str4: String? = ""
        if (chatline3 != null && chatline3.crownedName != null) {
            str4 = chatline3.crownedName
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun method6238(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.isForcedSafemode) 1  else 0
    }

    @JvmStatic
    fun method6239() {
        PreferenceManager.loadMinDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun appendTypedMessage(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var type: ChatMessageType? = ChatMessageType.forId(executor.intStack[executor.intStackPtr])
        var effectFlags = executor.intStack[executor.intStackPtr + 1]
        var message: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (type == ChatMessageType.DEV_CONSOLE) {
            Console.writeLine(message!!)
        } else if (type == ChatMessageType.DEV_CONSOLE_CLEAR) {
            Console.setConsoleText(message!!)
        } else {
            ChatBox.writeMessage(type!!, effectFlags, "", "", "", message!!)
        }
    }

    @JvmStatic
    fun method6230(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getAmountAtSlot(i_2, i_3, true)
    }

    @JvmStatic
    fun method6231(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.GRAND_EXCHANGE_SLOTS[i_2]!!.method5909()
    }

    @JvmStatic
    fun getCompHidden(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = if (icomponentdefinitions3!!.hidden) 1  else 0
    }

    @JvmStatic
    fun method1613(executor: CS2Executor) {
        executor.intStackPtr -= 4
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        i_2 += (i_3 shl 14)
        i_2 += (i_4 shl 28)
        i_2 += i_5
        executor.intStack[++executor.intStackPtr - 1] = i_2
    }

    @JvmStatic
    fun method6729(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var bool3 = executor.intStack[executor.intStackPtr + 1] == 1
        PreferenceManager.preferences!!.toolkit!!.setValue(i_2)
        if (!bool3) {
            PreferenceManager.preferences!!.graphics!!.setValue(0)
        }
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method6730(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = (executor.currentEntity as PlayerEntity).getUsernameWithTitle()
    }

    @JvmStatic
    fun getEnumSize(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var enumdefinitions3: EnumType? = TypeLists.ENUM!!.list(i_2)
        executor.intStack[++executor.intStackPtr - 1] = enumdefinitions3!!.getSize()
    }

    @JvmStatic
    fun method12596(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.maxScreenSize!!.canModify(i_2)
    }

    @JvmStatic
    fun method12597(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (Client.aLong7153 shr 32).toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.aLong7153.toInt()
    }

    @JvmStatic
    fun ccDelete(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        if (underlaydefinition2!!.comp!!.slotId == -1) {
            throw RuntimeException("")
        } else {
            var icomponentdefinitions3: Component? = underlaydefinition2!!.method8772()
            @Suppress("UNCHECKED_CAST")
            (icomponentdefinitions3!!.slotChildren as Array<Component?>?)!![underlaydefinition2!!.comp!!.slotId] = null
            Component.redrawComponent(icomponentdefinitions3)
        }
    }

    @JvmStatic
    fun hookMouseExit(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method11223(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun getMouseX(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = MouseRecord.recorder!!.getMouseX()
    }

    @JvmStatic
    fun lowerString(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.stringStack[++executor.stringStackPtr - 1] = string_2!!.lowercase()
    }

    @JvmStatic
    fun resumeStringDialog(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_TEXTDIALOG, Client.GAME_CONNECTION_CONTEXT!!.isaac)
        tcpmessage3!!.buffer.writeByte(string_2!!.length + 1)
        tcpmessage3!!.buffer.writeString(string_2!!)
        Client.GAME_CONNECTION_CONTEXT!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method1456(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Camera.camAngleX.toInt() shr 3
    }

    @JvmStatic
    fun method14655(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.RECOVERYQUESTIONSSETDATE
    }

    @JvmStatic
    fun method6073(executor: CS2Executor) {
        var class282Sub50Sub62: WorldMapElement? = WorldMap.poll(executor.intStack[--executor.intStackPtr])
        if (class282Sub50Sub62 != null) {
            if (class282Sub50Sub62.aString9534 != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = class282Sub50Sub62.aString9534
                return
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = ""
    }

    @JvmStatic
    fun loadClanSettingVar(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var class5373: ClanVarSettingsType? = TypeLists.CLAN_VAR_SETTINGS!!.list(i_2)
        if (class5373 == null) {
            throw RuntimeException()
        } else {
            var integer4: Int? = executor.currentClanSettings!!.getIntVar((Client.CURRENT_GAME!!.id shl 16) or i_2)
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
    fun method7556(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (executor.intStack[--executor.intStackPtr] == 1) {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3
        }
    }

    @JvmStatic
    fun method7560(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method6010(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun ccSetPosition(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setComponentPosition(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method7434(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).difficulty
    }

    @JvmStatic
    fun method7444(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.SKILL_XP[i_2]
    }

    @JvmStatic
    fun method7445(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).wearPos
    }

    @JvmStatic
    fun method7448(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.cpu!!.getValue()
    }

    @JvmStatic
    fun method11246(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method4136(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method11247(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.transparency
    }

    @JvmStatic
    fun method11249(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteScale
    }

    @JvmStatic
    fun method254(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var ints_4: IntArray? = executor.intStack
        var i_5 = ++executor.intStackPtr - 1
        val b_6 = if (Interface.method4248(i_2, i_3)) 1 else 0
        ints_4!![i_5] = b_6
    }

    @JvmStatic
    fun method15404(executor: CS2Executor) {
        executor.intStackPtr -= 4
        Client.aShort7276 = executor.intStack[executor.intStackPtr].toShort()
        if (Client.aShort7276 <= 0) {
            Client.aShort7276 = 1
        }
        Client.aShort7441 = executor.intStack[executor.intStackPtr + 1].toShort()
        if (Client.aShort7441 <= 0) {
            Client.aShort7441 = 32767
        } else if (Client.aShort7441 < Client.aShort7276) {
            Client.aShort7441 = Client.aShort7276
        }
        Client.aShort7214 = executor.intStack[executor.intStackPtr + 2].toShort()
        if (Client.aShort7214 <= 0) {
            Client.aShort7214 = 1
        }
        Client.aShort7474 = executor.intStack[executor.intStackPtr + 3].toShort()
        if (Client.aShort7474 <= 0) {
            Client.aShort7474 = 32767
        } else if (Client.aShort7474 < Client.aShort7214) {
            Client.aShort7474 = Client.aShort7214
        }
    }

    @JvmStatic
    fun method356(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).type
    }

    @JvmStatic
    fun method7218(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (!Renderers.activeToolkit!!.hasAntiAliasing()) {
            executor.intStack[++executor.intStackPtr - 1] = 3
        } else {
            executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasing!!.canModify(i_2)
        }
    }

    @JvmStatic
    fun method7219(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.color
    }

    @JvmStatic
    fun method6005(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class1843: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
        var tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.CHAT_TYPE, class1843!!.isaac)
        tcpmessage4!!.buffer.writeByte(i_2)
        class1843!!.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun sendVerifyEmailPacket(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        PacketEncoder.sendCheckEmailPacket(string_2)
    }

    @JvmStatic
    fun method11244(executor: CS2Executor) {
        var descriptor: WorldDescriptor? = WorldConnection.getNextWorldDescriptor()
        if (descriptor != null) {
            executor.intStack[++executor.intStackPtr - 1] = descriptor.worldNumber
            executor.intStack[++executor.intStackPtr - 1] = descriptor.flags
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.unknown
            var world: WorldMeta? = descriptor.getWorld()
            executor.intStack[++executor.intStackPtr - 1] = world!!.countryId
            executor.stringStack[++executor.stringStackPtr - 1] = world!!.activity
            executor.intStack[++executor.intStackPtr - 1] = descriptor.playerCount
            executor.intStack[++executor.intStackPtr - 1] = descriptor.ping
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.ipAddress
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun sendReportAbusePacket(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        executor.intStackPtr -= 2
        var i_4 = executor.intStack[executor.intStackPtr]
        var i_5 = executor.intStack[executor.intStackPtr + 1]
        if (string_3 == null) {
            string_3 = ""
        }
        if (string_3!!.length > 80) {
            string_3 = string_3!!.substring(0, 80)
        }
        var class1846: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
        var tcpmessage7: TcpPacket? = PacketEncoder.createPacket(ClientProt.REPORT_ABUSE, class1846!!.isaac)
        tcpmessage7!!.buffer.writeByte(TextUtils.getLength(string_2!!) + 2 + TextUtils.getLength(string_3!!))
        tcpmessage7!!.buffer.writeString(string_2!!)
        tcpmessage7!!.buffer.writeByte(i_4 - 1)
        tcpmessage7!!.buffer.writeByte(i_5)
        tcpmessage7!!.buffer.writeString(string_3!!)
        class1846!!.queuePacket(tcpmessage7)
    }

    @JvmStatic
    fun method8037(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = executor.currentClanSettings!!.memberNames!![i_2]
    }

    @JvmStatic
    fun method12492(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method7729(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun loadClanVar(executor: CS2Executor) {
        var param = executor.intOpValues!![executor.instrPtr]
        var integer3: Int? = ClanVarTypeList.CLAN_VARS!![param] as Int?
        if (integer3 == null) {
            var clanvarType: ClanVarType? = TypeLists.CLAN_VAR!!.list(param)
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
    fun method4324(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.gametipStartStagePreference!!.getValue()
    }

    @JvmStatic
    fun method3038(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 == -1) {
            var i_3 = 16383
            var i_4 = i_2 and 0x3fff
            var coordgrid5: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
            i_3 -= coordgrid5!!.x
            if (i_3 < 0) {
                i_3 = 0
            } else if (i_3 >= Resource.mapRegion!!.sceneSizeX) {
                i_3 = Resource.mapRegion!!.sceneSizeX
            }
            i_4 -= coordgrid5!!.y
            if (i_4 < 0) {
                i_4 = 0
            } else if (i_4 >= Resource.mapRegion!!.sceneSizeY) {
                i_4 = Resource.mapRegion!!.sceneSizeY
            }
            PingRequester.anInt5864 = ((i_3 shl 9)) + 256
            StaticMethods.anInt833 = ((i_4 shl 9)) + 256
        } else {
            PingRequester.anInt5864 = -1
            StaticMethods.anInt833 = -1
        }
    }

    @JvmStatic
    fun method3039(executor: CS2Executor) {
        var class217Sub12: WorldDescriptor? = WorldDescriptor.get_world()
        executor.intStack[++executor.intStackPtr - 1] = if (class217Sub12 == null) 0 else class217Sub12!!.flags
    }

    @JvmStatic
    fun method3041(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method1845(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3042(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var itemdefinitions4: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (i_3 >= 1 && i_3 <= 5 && itemdefinitions4!!.groundActions!![i_3 - 1] != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = itemdefinitions4!!.groundActions!![i_3 - 1]
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun method3043(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.getUserSlot(executor.stringStack[--executor.stringStackPtr] as String)
    }

    @JvmStatic
    fun method3713(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.water!!.canModify(i_2)
    }

    @JvmStatic
    fun method3714(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(8, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method15392(executor: CS2Executor) {
        var icomponentdefinitions2: Component? = Component.fromHash(executor.intStack[--executor.intStackPtr])
        assert(icomponentdefinitions2 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions2!!.containerItemId
    }

    @JvmStatic
    fun loadString(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.objectLocals!![executor.intOpValues!![executor.instrPtr]]
    }

    @JvmStatic
    fun method3368() {
        if (!LoginManager.method3360()) {
            LoginManager.method5558()
        }
    }

    @JvmStatic
    fun method3370(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method542(icomponentdefinitions3!!, executor)
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
    fun method6372(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method542(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6373(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Character.toUpperCase(i_2.toChar()).code
    }

    @JvmStatic
    fun method6374(executor: CS2Executor) {
        var descriptor: WorldDescriptor? = WorldDescriptor.getFirstWorldDescriptor()
        if (descriptor != null) {
            executor.intStack[++executor.intStackPtr - 1] = descriptor.worldNumber
            executor.intStack[++executor.intStackPtr - 1] = descriptor.flags
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.unknown
            var world: WorldMeta? = descriptor.getWorld()
            executor.intStack[++executor.intStackPtr - 1] = world!!.countryId
            executor.stringStack[++executor.stringStackPtr - 1] = world!!.activity
            executor.intStack[++executor.intStackPtr - 1] = descriptor.playerCount
            executor.intStack[++executor.intStackPtr - 1] = descriptor.ping
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.ipAddress
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun ifSetItemNoNum(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setItemIFComp(icomponentdefinitions3!!, interface4!!, false, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun method7915(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.MESSAGES
    }

    @JvmStatic
    fun method3988(executor: CS2Executor) {
        if (KeyRecorder.keyRecorder!!.held(86)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method6781(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.OBJ!!.list(i_2).wearPos3
    }

    @JvmStatic
    fun getCPUProfile(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ProfileCpu.getProfile()
    }

    @JvmStatic
    fun method3020(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method4276(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method3021(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.guestsTalk.toInt()
    }

    @JvmStatic
    fun method7675(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getItemIdAtSlot(i_2, i_3, false)
    }

    @JvmStatic
    fun method2107() {
        LoginManager.method7777()
    }

    @JvmStatic
    fun method2108(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.toolkit!!.canModify(i_2)
    }

    @JvmStatic
    fun method2112(executor: CS2Executor) {
        executor.intStackPtr -= 7
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1] shl 1
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        var i_6 = executor.intStack[executor.intStackPtr + 4]
        var i_7 = executor.intStack[executor.intStackPtr + 5]
        var i_8 = executor.intStack[executor.intStackPtr + 6]
        if (i_2 >= 0 && i_2 < 2 && Client.anIntArrayArrayArray7302[i_2] != null && i_3 >= 0 && i_3 < Client.anIntArrayArrayArray7302[i_2]!!.size) {
            Client.anIntArrayArrayArray7302[i_2]!![i_3] = intArrayOf(((i_4 shr 14) and 0x3fff) shl 9, i_5 shl 2, ((i_4 and 0x3fff) shl 9), i_8)
            Client.anIntArrayArrayArray7302[i_2]!![i_3 + 1] = intArrayOf(((i_6 shr 14) and 0x3fff) shl 9, i_7 shl 2, (i_6 and 0x3fff) shl 9)
        }
    }

    @JvmStatic
    fun method11486(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.SKILL_LEVEL_ACTUAL[i_2]
    }

    @JvmStatic
    fun mergeStrings(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.stringStackPtr -= i_2
        var string_3: String? = Utils.method6398(executor.stringStack, executor.stringStackPtr, i_2)
        executor.stringStack[++executor.stringStackPtr - 1] = string_3
    }

    @JvmStatic
    fun method12499(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method3672(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method810(executor: CS2Executor) {
        StaticMethods.method458(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun method811(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.clanChannel!!.clanName
    }

    @JvmStatic
    fun method814(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub143: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2)
        if (class282Sub50Sub143!!.messages == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub143!!.messages!!.size
        }
    }

    @JvmStatic
    fun ccResumePauseButton(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method6942(icomponentdefinitions3!!)
    }

    @JvmStatic
    fun method12689(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).name
    }

    @JvmStatic
    fun method1452(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        IgnoreList.add(string_2, false)
    }

    @JvmStatic
    fun sendMessage(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        ChatBox.writeUnfilterableMessage(string_2!!)
    }

    @JvmStatic
    fun method3340(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.MY_PLAYER!!.yaw.getRotation() shr 3
    }

    @JvmStatic
    fun method3341(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (Interface.INTERFACES!![i_2] == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            var string_4: String? = Interface.INTERFACES!![i_2]!!.components!![i_3]!!.name
            if (string_4 == null) {
                executor.stringStack[++executor.stringStackPtr - 1] = ""
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = string_4
            }
        }
    }

    @JvmStatic
    fun method3344(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.ENTITY_TARGET == executor.currentEntity!!.getCS2Index()) 1 else 0
    }

    @JvmStatic
    fun method3346(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStackPtr -= 2
        var i_3 = executor.intStack[executor.intStackPtr]
        var i_4 = executor.intStack[executor.intStackPtr + 1]
        var fontmetrics5: FontMetrics? = FontMetrics.getFontMetrics(Resource.FONT_METRICS, i_4)
        assert(fontmetrics5 != null)
        executor.intStack[++executor.intStackPtr - 1] = fontmetrics5!!.method6949(string_2!!, i_3, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
    }

    @JvmStatic
    fun loadVarpbit(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = VarProviders.PLAYER_VAR_PROVIDER.getVarBitValue(i_2)
    }

    @JvmStatic
    fun method5203(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method14482(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun intLessOrEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] <= executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method5205(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = AccountCreationResponseOpcodes.method1489().getValue()
    }

    @JvmStatic
    fun modulo(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 % i_3
    }

    @JvmStatic
    fun method3829(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_MIN_RANK_CAN_KICK.toInt()
    }

    @JvmStatic
    fun method3830(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = CalenderUtil.getLanguageTimestring(executor.intStack[--executor.intStackPtr] * 60000L, Client.CURRENT_LANGUAGE!!.getValue()) + " UTC"
    }

    @JvmStatic
    fun method5065(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.fog!!.visible()) 1  else 0
    }

    @JvmStatic
    fun method7208(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method15511(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method7209(executor: CS2Executor) {
        MiniMenu.anInt198 = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun longEqual(executor: CS2Executor) {
        executor.longStackPtr -= 2
        if (executor.longStack[executor.longStackPtr + 1] == executor.longStack[executor.longStackPtr]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method15371(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.characterShadows!!.visible()) 1  else 0
    }

    @JvmStatic
    fun method3030(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(i_2)
        executor.intStack[++executor.intStackPtr - 1] = if (itemdefinitions3!!.hasTooltipColor) 1  else 0
    }

    @JvmStatic
    fun method3031(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method1454(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3033(executor: CS2Executor) {
        if (Client.PLAYER_MOD_LEVEL >= 5 && Client.PLAYER_MOD_LEVEL <= 9) {
            executor.intStack[++executor.intStackPtr - 1] = Client.PLAYER_MOD_LEVEL
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method15559(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.fontId
    }

    @JvmStatic
    fun method4629(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method5455(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4630(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1] - 1
        var itemdefinitions4: ItemType? = TypeLists.OBJ!!.list(i_2)
        if (i_3 == itemdefinitions4!!.customCursorOp1) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions4!!.customCursorId1
        } else if (i_3 == itemdefinitions4!!.customCursorOp2) {
            executor.intStack[++executor.intStackPtr - 1] = itemdefinitions4!!.customCursorId2
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun method4631(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.currentQuickChatMessage!!.anIntArray4046!![i_2] = i_3
    }

    @JvmStatic
    fun method4633(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setTargetVerb(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6938(executor: CS2Executor) {
        executor.intStackPtr -= 3
        @Suppress("UNCHECKED_CAST")
        var bytes_2: ByteArray? = executor.globalArrays[executor.intStack[executor.intStackPtr]] as ByteArray?
        @Suppress("UNCHECKED_CAST")
        var bytes_3: ByteArray? = executor.globalArrays[executor.intStack[executor.intStackPtr + 1]] as ByteArray?
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var icomponentdefinitions5: Component? = Component.fromHash(i_4)
        method1838(icomponentdefinitions5!!, bytes_2, bytes_3, executor)
    }

    @JvmStatic
    fun method6940(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(6, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method6941(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        method5001(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method451(executor: CS2Executor) {
        PreferenceManager.preferences!!.lightDetail!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        GraphicsCache.resetGraphicCaches()
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method452(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 != PreferenceManager.preferences!!.skyboxes!!.getValue()) {
            PreferenceManager.preferences!!.skyboxes!!.setValue(i_2)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun method453(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.aBool7189) 1  else 0
    }

    @JvmStatic
    fun method454(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.color
    }

    @JvmStatic
    fun method8211(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteYaw
    }

    @JvmStatic
    fun method8212(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method11337(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun loadVarp(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = VarProviders.PLAYER_VAR_PROVIDER.activeVars[i_2]
    }

    @JvmStatic
    fun method6364(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method4276(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method3117(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method12401(icomponentdefinitions3!!, interface4!!)
    }

    @JvmStatic
    fun method3119(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt107
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt109
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.anInt108
        StaticMethods.anInt107 = -2
        StaticMethods.anInt109 = -1
        StaticMethods.anInt108 = -1
    }

    @JvmStatic
    fun method3120() {
        LoginManager.method1478()
    }

    @JvmStatic
    fun method1603(executor: CS2Executor) {
        var class1842: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        var i_4 = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(3)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.method14896(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.anIntArray4046!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - i_4)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method1604(executor: CS2Executor) {
        System.out.println(executor.stringStack[--executor.stringStackPtr])
    }

    @JvmStatic
    fun method1606(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method15409(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1608(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_PLAYERS != null && i_2 < FriendsChat.FC_PLAYER_COUNT && FriendsChat.FC_PLAYERS!![i_2]!!.username.equals(PlayerUpdate.MY_PLAYER!!.displayName)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun resumeNamedDialog(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_NAMEDIALOG, Client.GAME_CONNECTION_CONTEXT!!.isaac)
        tcpmessage3!!.buffer.writeByte(string_2!!.length + 1)
        tcpmessage3!!.buffer.writeString(string_2!!)
        Client.GAME_CONNECTION_CONTEXT!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method5788(executor: CS2Executor) {
        var class282Sub362: WorldMapCategory? = WorldMapSub1.method12538()
        if (class282Sub362 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub362.mecId
            var i_3 = (class282Sub362!!.anInt7988 shl 28) or class282Sub362.absX + (WorldMap.mapSquareAbsX shl 14) or class282Sub362.absY + WorldMap.mapSquareAbsY
            executor.intStack[++executor.intStackPtr - 1] = i_3
        }
    }

    @JvmStatic
    fun method12583(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method3601(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun getWorldId(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.GAME_CONNECTION_INFO!!.worldId
    }

    @JvmStatic
    fun method1888(executor: CS2Executor) {
        executor.intStackPtr -= 5
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], executor.intStack[executor.intStackPtr + 4])
    }

    @JvmStatic
    fun method1889(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.kickRank.toInt()
    }

    @JvmStatic
    fun powInverse(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else if (i_3 == 0) {
            executor.intStack[++executor.intStackPtr - 1] = Integer.MAX_VALUE
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Math.pow(i_2.toDouble(), 1.0 / i_3).toInt()
        }
    }

    @JvmStatic
    fun checkJavaVersion(executor: CS2Executor) {
        if (SystemInfo.SYSTEM_INFO!!.javaBuild < 6) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else if (SystemInfo.SYSTEM_INFO!!.javaBuild == 6 && SystemInfo.SYSTEM_INFO!!.javaUpdate < 10) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 1
        }
    }

    @JvmStatic
    fun ifSetItem(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setItemIFComp(icomponentdefinitions3!!, interface4!!, false, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun method5027(executor: CS2Executor) {
        executor.intStackPtr -= 2
        SoundEffectPlayer.playSoundJingle(executor.intStack[executor.intStackPtr], 255)
    }

    @JvmStatic
    fun method5029(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method15207(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method14746(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method2593(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8735(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions4!!.transparency
    }

    @JvmStatic
    fun getRunEnergy(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.RUN_ENERGY
    }

    @JvmStatic
    fun method3596(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method2593(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3597(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2]!!.friendsChatRank
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun setEffectVolume(executor: CS2Executor) {
        PreferenceManager.preferences!!.soundEffectVolume!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method855(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        method3965(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method856(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub63: WorldMapElement? = WorldMap.poll(i_2)
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9542
        }
    }

    @JvmStatic
    fun method858(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.scrollWidth
    }

    @JvmStatic
    fun method5290(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).isComplete(VarProviders.PLAYER_VAR_PROVIDER)) 1 else 0
    }

    @JvmStatic
    fun hsvToRGB(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = ColorUtil.hsvToRgbTable!![ColorUtil.adjustHsv(i_2).toInt() and 0xffff]
    }

    @JvmStatic
    fun method12802(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method4651(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method12803(executor: CS2Executor) {
        PreferenceManager.preferences!!.fog!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun pushInt(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.intOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun method6510(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method1455(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method2914(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub443: SubInterface? = Client.OPEN_INTERFACES!!.get(i_2.toLong())
        if (class282Sub443 != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method2916(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub63: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub63 != null) {
            var bool4 = class282Sub50Sub63.method14778((i_2 shr 28) and 0x3, (i_2 shr 14) and 0x3fff, i_2 and 0x3fff, CS2Executor.anIntArray5900)
            if (bool4) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900!![1]
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900!![2]
            } else {
                executor.intStack[++executor.intStackPtr - 1] = -1
                executor.intStack[++executor.intStackPtr - 1] = -1
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun method13756(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub63: WorldMapElement? = WorldMap.getMapDef((i_2 shr 14) and 0x3fff, i_2 and 0x3fff)
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9536
        }
    }

    @JvmStatic
    fun method15403() {
        Client.setLoadingWorldMap()
    }

    @JvmStatic
    fun method798(executor: CS2Executor) {
        getBoundingBox(executor.currentGroundItem!! as SceneGraphEntity, executor)
    }

    @JvmStatic
    fun method7420(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.defaultToolkit!!.visible()) 1  else 0
    }

    @JvmStatic
    fun method7422(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.scrollX
    }

    @JvmStatic
    fun method1799(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).questPointReward
    }

    @JvmStatic
    fun pushString(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = executor.current!!.stringOpValues!![executor.instrPtr]
    }

    @JvmStatic
    fun method1801(executor: CS2Executor) {
        method11160(method8697(), executor)
    }

    @JvmStatic
    fun method8697(): MiniMenuEntry? {
        return MiniMenu.aCacheableNodeSub79445
    }

    @JvmStatic
    fun method260(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setOnMouseOver(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method261(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentEntity!!.loadedInterfaceId
    }

    @JvmStatic
    fun method11612(executor: CS2Executor) {
        executor.intStackPtr -= 3
        SoundEffectPlayer.playSoundJingle(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 2])
    }

    @JvmStatic
    fun method11613(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (Client.BASE_WINDOW_ID != -1) {
            if (i_2 == 0) {
                executor.intStack[++executor.intStackPtr - 1] = Client.BASE_WINDOW_ID
                return
            }
            --i_2
        }
        var class282Sub443: SubInterface? = Client.OPEN_INTERFACES!!.first()
        while (i_2-- > 0) {
            class282Sub443 = Client.OPEN_INTERFACES!!.next()
        }
        executor.intStack[++executor.intStackPtr - 1] = class282Sub443!!.subInterfaceId
    }

    @JvmStatic
    fun setGender(executor: CS2Executor) {
        var bool2 = executor.intStack[--executor.intStackPtr] != 0
        if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null) {
            PlayerUpdate.MY_PLAYER!!.playerAppearance!!.changeGender(bool2)
        }
    }

    @JvmStatic
    fun method4311(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method7555(icomponentdefinitions3!!, interface4!!, executor)
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
    fun method4314(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasPrerequisiteRequirement(VarProviders.PLAYER_VAR_PROVIDER, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun method4315() {
        StaticElements.CS2_QUERY_RESULT_IDX = 0
    }

    @JvmStatic
    fun method13359(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var bool3 = ClientStartup.activeClient!!.method4658(File(string_2))
        executor.intStack[++executor.intStackPtr - 1] = if (bool3) 1  else 0
    }

    @JvmStatic
    fun method4973(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.aShort7394.toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.aShort7324.toInt()
    }

    @JvmStatic
    fun method5454(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6689(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5456(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.stringStack[++executor.stringStackPtr - 1] = TextUtils.atLtGtTags(string_2!!)
    }

    @JvmStatic
    fun hookMouseEnter(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method6289(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1492(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method774(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1967(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method14680(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1968(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.defaultToolkit!!.canModify(i_2)
    }

    @JvmStatic
    fun method5362(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.method1198(i_2, i_3, i_4)
    }

    @JvmStatic
    fun method8205(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.anInt7271 - Client.anInt7270
    }

    @JvmStatic
    fun method8206(executor: CS2Executor) {
        executor.intStackPtr -= 12
        MiniMenu.close()
        StaticMedia.resetChatMedia()
        TextureOpIrregularBricks.anInt9792 = executor.intStack[executor.intStackPtr]
        NativeSprite.anInt3556 = executor.intStack[executor.intStackPtr + 1]
        MiniMenu.anInt188 = executor.intStack[executor.intStackPtr + 2]
        StaticMethods.anInt2262 = executor.intStack[executor.intStackPtr + 3]
        StaticMethods.anInt5438 = executor.intStack[executor.intStackPtr + 4]
        NativeSprite.anInt4350 = executor.intStack[executor.intStackPtr + 5]
        StaticMethods.anInt3145 = executor.intStack[executor.intStackPtr + 6]
        StaticMethods.anInt5396 = executor.intStack[executor.intStackPtr + 7]
        StaticMethods.anInt148 = executor.intStack[executor.intStackPtr + 8]
        MiniMenu.anInt4722 = executor.intStack[executor.intStackPtr + 9]
        StaticMethods.anInt1005 = executor.intStack[executor.intStackPtr + 10]
        StaticMethods.anInt2982 = executor.intStack[executor.intStackPtr + 11]
        Resource.SPRITES!!.isFileCached(StaticMethods.anInt5438)
        Resource.SPRITES!!.isFileCached(NativeSprite.anInt4350)
        Resource.SPRITES!!.isFileCached(StaticMethods.anInt3145)
        Resource.SPRITES!!.isFileCached(StaticMethods.anInt5396)
        Resource.SPRITES!!.isFileCached(StaticMethods.anInt148)
        Resource.SPRITES!!.isFileCached(StaticMethods.anInt2982)
        Resource.FONT_METRICS!!.isFileCached(StaticMethods.anInt2982)
        StaticMethods.aNativeSprite4110 = null
        StaticMethods.aNativeSprite9315 = null
        StaticMethods.aNativeSprite1571 = null
        StaticMethods.aNativeSprite612 = null
        StaticMedia.aNativeSprite647 = null
        StaticMedia.aNativeSprite5576 = null
        StaticMedia.aNativeSprite3452 = null
        StaticMedia.aNativeSprite5339 = null
        FontMetrics.useBoldFont = true
    }

    @JvmStatic
    fun method3250(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method501(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8865(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var quickchatdefinitions3: QuickchatMessageType? = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2)
        if (quickchatdefinitions3!!.responses == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = quickchatdefinitions3!!.responses!!.size
        }
    }

    @JvmStatic
    fun method8869(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt1526
    }

    @JvmStatic
    fun method8870(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS!![i_2]!!.displayName
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun method12915(executor: CS2Executor) {
        var npc2: NpcEntity? = executor.currentEntity as NpcEntity
        var string_3: String? = npc2!!.modifiedName
        var npcdefinitions4: NPCType? = npc2!!.definitions
        if (npcdefinitions4!!.alternativeIds != null) {
            npcdefinitions4 = npcdefinitions4!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
            if (npcdefinitions4 == null) {
                string_3 = ""
            } else {
                string_3 = npcdefinitions4!!.name
            }
        }
        if (string_3 == null) {
            string_3 = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_3
    }

    @JvmStatic
    fun method7826(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).messageHotkeys!![i_3].code
    }

    @JvmStatic
    fun method12352(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.aBool5556) 1  else 0
    }

    @JvmStatic
    fun bitNot(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = i_2.inv()
    }

    @JvmStatic
    fun method5000(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method5009(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5002(executor: CS2Executor) {
        method1573(executor)
    }

    @JvmStatic
    fun method5003(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = i_2 and 0x3fff
    }

    @JvmStatic
    fun method5004(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6690(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method15075(executor: CS2Executor) {
        PacketEncoder.method6811(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun method16095(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method4772(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method14434(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        PacketEncoder.method11404(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, "", executor.intStack[--executor.intStackPtr] == 1, false)
    }

    @JvmStatic
    fun setCompText(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setCompText(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun randSoundPitch(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 > 700 || i_3 > 700) {
            executor.intStack[++executor.intStackPtr - 1] = 256
        }
        var d_4 = (Math.random() * (i_3 + i_2) - i_2 + 800.0) / 100.0
        executor.intStack[++executor.intStackPtr - 1] = (Math.pow(2.0, d_4) + 0.5).toInt()
    }

    @JvmStatic
    fun method7961(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method3366(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method7962() {
    }

    @JvmStatic
    fun method6242(executor: CS2Executor) {
        executor.intStackPtr -= 5
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        var i_6 = executor.intStack[executor.intStackPtr + 4]
        if (i_4 == -1) {
            throw RuntimeException()
        } else {
            var enumdefinitions7: EnumType? = TypeLists.ENUM!!.list(i_4)
            if (i_3 != enumdefinitions7!!.keyType.code) {
                throw RuntimeException()
            } else if (i_2 != enumdefinitions7!!.valueType.code) {
                throw RuntimeException()
            } else {
                var ints_8: IntArray? = enumdefinitions7!!.getValueReferences(i_5)
                if (i_6 >= 0 && ints_8 != null && ints_8!!.size > i_6) {
                    executor.intStack[++executor.intStackPtr - 1] = ints_8!![i_6]
                } else {
                    throw RuntimeException()
                }
            }
        }
    }

    @JvmStatic
    fun textGender(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null && PlayerUpdate.MY_PLAYER!!.playerAppearance!!.female) {
            executor.stringStack[++executor.stringStackPtr - 1] = string_3
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = string_2
        }
    }

    @JvmStatic
    fun clearBit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 and -1 - ((1 shl i_3))
    }

    @JvmStatic
    fun method1462(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        IgnoreList.add(string_2, true)
    }

    @JvmStatic
    fun method1463(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.maxScreenSize!!.getValue()
    }

    @JvmStatic
    fun setAntiAliasingPreview(executor: CS2Executor) {
        PreferenceManager.preferences!!.antiAliasingPreview!!.setValue(executor.intStack[--executor.intStackPtr])
        Client.loadAssets(PreferenceManager.preferences!!.activeToolkit!!.getValue(), false)
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun method14649(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.mapSize!!.canModify(i_2)
    }

    @JvmStatic
    fun method5082(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var underlaydefinition4: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        InterfaceRenderer.method12221(underlaydefinition4!!.comp!!, i_2, i_3)
    }

    @JvmStatic
    fun method7186(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.myPlayerIndex
    }

    @JvmStatic
    fun method7187() {
    }

    @JvmStatic
    fun method3739(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = if (FriendList.FRIENDS[i_2]!!.referrer) 1  else 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method3740(executor: CS2Executor) {
        var npc2: NpcEntity? = executor.currentEntity as NpcEntity
        var npcdefinitions3: NPCType? = npc2!!.definitions
        if (npcdefinitions3!!.alternativeIds != null) {
            npcdefinitions3 = npcdefinitions3!!.getMultiNpc(VarProviders.PLAYER_VAR_PROVIDER)
        }
        executor.intStack[++executor.intStackPtr - 1] = if (npcdefinitions3 != null) 1 else 0
    }

    @JvmStatic
    fun getWorldQuickchatOnly(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.IS_QUICKCHAT_ONLY) 1  else 0
    }

    @JvmStatic
    fun intGreaterThan(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] > executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method16089(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var inventorydef2: ItemContainerType? = TypeLists.CONTAINER!!.list(executor.intStack[executor.intStackPtr])
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = -1
        for (i_5 in 0 until inventorydef2!!.defaultItemSize) {
            if (i_3 == inventorydef2!!.itemIds!![i_5]) {
                i_4 = inventorydef2!!.amounts!![i_5]
                break
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun method6040(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method12630(icomponentdefinitions3!!)
    }

    @JvmStatic
    fun method6041(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = Client.getComponentSettings(icomponentdefinitions3!!)!!.getUseOptionFlags()
    }

    @JvmStatic
    fun method6042(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentLocation as SceneGraphEntity?)!!.getIconHeight()
    }

    @JvmStatic
    fun method6043(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setOnMouseLeave(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method6044(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.isApplet) 1  else 0
    }

    @JvmStatic
    fun method6046(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.LOYALTY_BALANCE
    }

    @JvmStatic
    fun method12718(executor: CS2Executor) {
        PreferenceManager.preferences!!.ambientSoundVolume!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun ccSetItemNoNum(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        setItemIFComp(icomponentdefinitions4!!, interface5!!, false, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun method8285(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6289(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun ifSetItemWearColNoNum(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setItemIFComp(icomponentdefinitions3!!, interface4!!, true, Component.StackMode.NONUM, executor)
    }

    @JvmStatic
    fun method8296(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var class1843: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
        var tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PRIVATE, class1843!!.isaac)
        tcpmessage4!!.buffer.writeByte(0)
        var i_5 = tcpmessage4!!.buffer.currentPosition
        tcpmessage4!!.buffer.writeString(string_2!!)
        tcpmessage4!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.method14896(tcpmessage4!!.buffer, executor.currentQuickChatMessage!!.anIntArray4046!!)
        tcpmessage4!!.buffer.writeIndex(tcpmessage4!!.buffer.currentPosition - i_5)
        class1843!!.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun method8297(executor: CS2Executor) {
        if (KeyRecorder.keyRecorder!!.held(81)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method8298(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method15474(icomponentdefinitions4!!)
    }

    @JvmStatic
    fun method868(executor: CS2Executor) {
        var questdefinitions2: QuestType? = TypeLists.QUEST!!.list(executor.intStack[--executor.intStackPtr])
        executor.intStack[++executor.intStackPtr - 1] = if (questdefinitions2!!.varpRequirements == null) 0 else questdefinitions2!!.varpRequirements!!.size
    }

    @JvmStatic
    fun method870(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var icomponentdefinitions5: Component? = Component.fromHash(i_4)
        InterfaceRenderer.method12221(icomponentdefinitions5!!, i_2, i_3)
    }

    @JvmStatic
    fun add(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = i_2 + i_3
    }

    @JvmStatic
    fun method15417(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.method304()
    }

    @JvmStatic
    fun method4381(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method12401(icomponentdefinitions4!!, interface5!!)
    }

    @JvmStatic
    fun method14818(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Resource.NATIVE_LIBRARY_LOADER!!.isJagtheoraLoaded()) 1  else 0
    }

    @JvmStatic
    fun method14833(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.COUNTRY
    }

    @JvmStatic
    fun method14835(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var chatline3: ChatLine? = ChatBox.getLine(i_2)
        var str4: String? = ""
        if (chatline3 != null && chatline3.name != null) {
            str4 = chatline3.name
        }
        executor.stringStack[++executor.stringStackPtr - 1] = str4
    }

    @JvmStatic
    fun method14836(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var bool3 = executor.intStack[executor.intStackPtr + 1] == 1
        var node4: Node<*>? = WorldMapSub1.aClass4658025.get(i_2.toLong())
        if (node4 != null && !bool3) {
            node4.unlink()
        } else if (node4 == null && bool3) {
            node4 = object : Node<Any?>() {}
            WorldMapSub1.aClass4658025.put(node4, i_2.toLong())
        }
    }

    @JvmStatic
    fun method14839() {
    }

    @JvmStatic
    fun method14841(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).hasLevelRequirement(Client.SKILL_LEVEL_ACTUAL, executor.intStack[executor.intStackPtr - 1])) 1 else 0
        --executor.intStackPtr
    }

    @JvmStatic
    fun method6677(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = executor.clanChannel!!.players!![i_2]!!.rank.toInt()
    }

    @JvmStatic
    fun method6678(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 < 0 || i_2 > 5) {
            i_2 = 2
        }
        Client.loadAssets(i_2, false)
    }

    @JvmStatic
    fun method6680(executor: CS2Executor) {
        if (FriendsChat.FC_NAME != null) {
            executor.intStack[++executor.intStackPtr - 1] = FriendsChat.FC_PLAYER_COUNT
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method6685(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method502(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method12839(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method505(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method12841(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.GRAND_EXCHANGE_ITEM_SELECT, Client.GAME_CONNECTION_CONTEXT!!.isaac)
        tcpmessage3!!.buffer.writeShort(i_2)
        Client.GAME_CONNECTION_CONTEXT!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method456(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.cycles
    }

    @JvmStatic
    fun method457(executor: CS2Executor) {
        PreferenceManager.preferences!!.groundDecoration!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun ccSetScrollPos(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method1852(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method460(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method8310(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5774(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method501(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5775(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method6288(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5776(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (i_2 == -1) {
            throw RuntimeException()
        } else {
            var enumdefinitions4: EnumType? = TypeLists.ENUM!!.list(i_2)
            if (enumdefinitions4!!.valueType.code != 's'.code) {
                throw RuntimeException()
            } else {
                executor.intStack[++executor.intStackPtr - 1] = if (enumdefinitions4!!.containsKey(string_3!!)) 1  else 0
            }
        }
    }

    @JvmStatic
    fun method14669(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.method3360()) 1  else 0
    }

    @JvmStatic
    fun method2584(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        WorldMapSub1.reset(i_2, (i_3 shr 14) and 0x3fff, i_3 and 0x3fff, false)
    }

    @JvmStatic
    fun method6913(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        var string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        if (Client.PLAYER_RIGHTS != 0 || (!Client.USERDETAIL_QUICKCHAT || Client.VERIFIED_EMAIL_ADDRESS) && !Client.IS_QUICKCHAT_ONLY) {
            var class1844: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
            var tcpmessage5: TcpPacket? = PacketEncoder.createPacket(ClientProt.SEND_PRIVATE_MESSAGE, class1844!!.isaac)
            tcpmessage5!!.buffer.writeShort(0)
            var i_6 = tcpmessage5!!.buffer.currentPosition
            tcpmessage5!!.buffer.writeString(string_2!!)
            PacketEncoder.chatMessageToBuffer(tcpmessage5!!.buffer, string_3)
            tcpmessage5!!.buffer.writeShortLength(tcpmessage5!!.buffer.currentPosition - i_6)
            class1844!!.queuePacket(tcpmessage5)
        }
    }

    @JvmStatic
    fun getPlayerCombatLevel(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PlayerUpdate.MY_PLAYER!!.combatLevel
    }

    @JvmStatic
    fun loadClanVarLong(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        var long_3: Long? = ClanVarTypeList.CLAN_VARS!![i_2] as Long?
        if (long_3 == null) {
            executor.longStack[++executor.longStackPtr - 1] = -1L
        } else {
            executor.longStack[++executor.longStackPtr - 1] = long_3
        }
    }

    @JvmStatic
    fun method3748(executor: CS2Executor) {
        if (Client.aString7165 != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = Client.aString7165
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun popInt(executor: CS2Executor) {
        --executor.intStackPtr
    }

    @JvmStatic
    fun method3750(executor: CS2Executor) {
        PreferenceManager.preferences!!.idleAnimations!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun getSystemUpdateTimer(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.REBOOT_TIMER
    }

    @JvmStatic
    fun method911(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var itemdefinitions3: ItemType? = TypeLists.OBJ!!.list(i_2)
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
    fun method536(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var string_5: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (i_3 == -1) {
            throw RuntimeException()
        } else {
            var enumdefinitions6: EnumType? = TypeLists.ENUM!!.list(i_3)
            if (i_2 != enumdefinitions6!!.keyType.code) {
                throw RuntimeException()
            } else if (enumdefinitions6!!.valueType.code != 115) {
                throw RuntimeException()
            } else {
                var ints_7: IntArray? = enumdefinitions6!!.getValueReferences(string_5!!)
                if (i_4 >= 0 && ints_7 != null && ints_7!!.size > i_4) {
                    executor.intStack[++executor.intStackPtr - 1] = ints_7!![i_4]
                } else {
                    throw RuntimeException()
                }
            }
        }
    }

    @JvmStatic
    fun method7427(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (i_2 shr 28)
    }

    @JvmStatic
    fun method1167(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setNoClickThrough(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method1171(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.banCount
    }

    @JvmStatic
    fun setTradeFilter(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ChatBox.TRADE_FILTER
    }

    @JvmStatic
    fun method401(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method3251(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method12210(executor: CS2Executor) {
        executor.stringStackPtr -= 3
        PacketEncoder.method11404(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, executor.stringStack[executor.stringStackPtr + 2] as String?, executor.intStack[--executor.intStackPtr] == 1, true)
    }

    @JvmStatic
    fun method7500(executor: CS2Executor) {
        PreferenceManager.preferences!!.idleAnimations!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method7501(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var string_3: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.EMAIL_VALIDATION_CHANGE_ADDRESS, Client.LOBBY_CONNECTION_CONTEXT.isaac)
        tcpmessage4!!.buffer.writeShort(TextUtils.getLength(string_2!!) + TextUtils.getLength(string_3!!))
        tcpmessage4!!.buffer.writeString(string_2!!)
        tcpmessage4!!.buffer.writeString(string_3!!)
        Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun method7502(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions4!!.text
    }

    @JvmStatic
    fun method3949(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method6697(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method3950() {
        StaticElements.CS2_QUERY_RESULT_IDX = 0
    }

    @JvmStatic
    fun method3951(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Character.toLowerCase(i_2.toChar()).code
    }

    @JvmStatic
    fun method5511(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method2874(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method5512(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        method14605(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4960(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method2915(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun ifSetItemWearCol(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setItemIFComp(icomponentdefinitions3!!, interface4!!, true, Component.StackMode.WHEN_MULTIPLE, executor)
    }

    @JvmStatic
    fun ifSetItemAlwaysNum(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        setItemIFComp(icomponentdefinitions3!!, interface4!!, false, Component.StackMode.ALWAYS_NUM, executor)
    }

    @JvmStatic
    fun method1569(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method4280(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun ccSetItemWearColAlwaysNum(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setItemIFComp(icomponentdefinitions3!!, interface4!!, true, Component.StackMode.ALWAYS_NUM, executor)
    }

    @JvmStatic
    fun method1571(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spriteRoll
    }

    @JvmStatic
    fun method1572(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.textures!!.canModify(i_2)
    }

    @JvmStatic
    fun method1573(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var class282Sub444: SubInterface? = Client.OPEN_INTERFACES!!.get(i_2.toLong())
        if (class282Sub444 != null && i_3 == class282Sub444.subInterfaceId) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method1574(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.skyboxes!!.display()) 1  else 0
    }

    @JvmStatic
    fun method12723(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.angle2d
    }

    @JvmStatic
    fun method1162(executor: CS2Executor) {
        executor.intStackPtr -= 4
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        var coordgrid6: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        Camera.lookat(((i_2 shr 14) and 0x3fff) - coordgrid6!!.x, (i_2 and 0x3fff) - coordgrid6!!.y, (i_3 shl 2), i_4, i_5)
    }

    @JvmStatic
    fun method11154(executor: CS2Executor) {
        executor.currentQuickChatMessage = QuickChatMessage()
        executor.currentQuickChatMessage!!.id = executor.intStack[--executor.intStackPtr]
        executor.currentQuickChatMessage!!.message = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.anIntArray4046 = IntArray(executor.currentQuickChatMessage!!.message!!.method14916())
    }

    @JvmStatic
    fun method1979(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = executor.clanChannel!!.players!![i_2]!!.name
    }

    @JvmStatic
    fun method7744(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt94
    }

    @JvmStatic
    fun method14244(executor: CS2Executor) {
        var i_2 = WindowMode.fullScreenWidth
        var i_3 = WindowMode.fullScreenHeight
        var i_4 = -1
        if (WindowMode.fullScreenSupported) {
            var arr_5: Array<DisplayModeDetails>? = WindowMode.getActiveDisplayModeDetails()
            for (i_6 in 0 until arr_5!!.size) {
                var class4677: DisplayModeDetails? = arr_5[i_6]
                if (i_2 == class4677!!.width && i_3 == class4677!!.height) {
                    i_4 = i_6
                    break
                }
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun method11380(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method7667(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun method4825(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = ItemContainer.getItemIdAtSlot(i_2, i_3, true)
    }

    @JvmStatic
    fun method4826(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.anInt112
    }

    @JvmStatic
    fun method4829(executor: CS2Executor) {
        if (FriendList.LOADED == 0) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = IgnoreList.IGNORE_COUNT
        }
    }

    @JvmStatic
    fun method4830(executor: CS2Executor) {
        var icomponentdefinitions2: Component? = executor.currentEntity!!.inter!!.getComponent(executor.intStack[--executor.intStackPtr])
        icomponentdefinitions2!!.slotChildren = null
        icomponentdefinitions2!!.itemSlots = null
        Component.redrawComponent(icomponentdefinitions2!!)
    }

    @JvmStatic
    fun method7461(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 1] = if (TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 1]).members) 1 else 0
    }

    @JvmStatic
    fun method7462(executor: CS2Executor) {
        var string_2: String? = null
        if (PacketDecoder.HOSTNAME_IDENTIFIER != null) {
            string_2 = PacketDecoder.HOSTNAME_IDENTIFIER!!.getHostName()
        }
        if (string_2 == null) {
            string_2 = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = string_2
    }

    @JvmStatic
    fun interpolate(executor: CS2Executor) {
        executor.intStackPtr -= 5
        var a = executor.intStack[executor.intStackPtr]
        var b = executor.intStack[executor.intStackPtr + 1]
        var c = executor.intStack[executor.intStackPtr + 2]
        var d = executor.intStack[executor.intStackPtr + 3]
        var e = executor.intStack[executor.intStackPtr + 4]
        executor.intStack[++executor.intStackPtr - 1] = a + (e - c) * (b - a) / (d - c)
    }

    @JvmStatic
    fun method1860(executor: CS2Executor) {
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
            var class1846: BufferedConnectionContext? = Client.GAME_CONNECTION_CONTEXT
            var tcpmessage7: TcpPacket? = PacketEncoder.createPacket(ClientProt.CHAT, class1846!!.isaac)
            tcpmessage7!!.buffer.writeByte(0)
            var i_8 = tcpmessage7!!.buffer.currentPosition
            tcpmessage7!!.buffer.writeByte(b_4)
            tcpmessage7!!.buffer.writeByte(b_5)
            PacketEncoder.chatMessageToBuffer(tcpmessage7!!.buffer, string_2)
            tcpmessage7!!.buffer.writeIndex(tcpmessage7!!.buffer.currentPosition - i_8)
            class1846!!.queuePacket(tcpmessage7)
        }
    }

    @JvmStatic
    fun method1861(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Client.recolorPalette!![executor.intStack[executor.intStackPtr]] = ColorUtil.rgbToHsl24(executor.intStack[executor.intStackPtr + 1]).toShort()
        TypeLists.OBJ!!.resetModelCache()
        TypeLists.OBJ!!.resetRecolorCache()
        TypeLists.NPC!!.resetBodyCaches()
        GameShell.setComponentsOnTop()
    }

    @JvmStatic
    fun method1862(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.groundDecoration!!.playingRunescape()) 1  else 0
    }

    @JvmStatic
    fun setBaseColor(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (PlayerUpdate.MY_PLAYER!!.playerAppearance != null) {
            PlayerUpdate.MY_PLAYER!!.playerAppearance!!.recolor(i_2, i_3)
        }
    }

    @JvmStatic
    fun method1836(executor: CS2Executor) {
        executor.intStackPtr -= 2
        PlayerUpdate.animateZero(PlayerUpdate.MY_PLAYER!!, executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1])
    }

    @JvmStatic
    fun method1837() {
        Camera.smoothReset()
    }

    @JvmStatic
    fun method7551(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (Utils.time() / 60000L).toInt()
    }

    @JvmStatic
    fun method7552(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.maxScreenSize!!.visible()) 1  else 0
    }

    @JvmStatic
    fun method16124(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (string_2!!.startsWith(TextUtils.getCrown(0)) || string_2!!.startsWith(TextUtils.getCrown(1))) {
            string_2 = string_2!!.substring(7)
        }
        executor.intStack[++executor.intStackPtr - 1] = if (IgnoreList.contains(string_2!!)) 1  else 0
    }

    @JvmStatic
    fun method3561(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method2170(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method3562(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(3, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method15406(executor: CS2Executor) {
        var bool2 = true
        executor.intStack[++executor.intStackPtr - 1] = if (bool2) 1  else 0
    }

    @JvmStatic
    fun method15407(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.fog!!.canModify(i_2)
    }

    @JvmStatic
    fun method15408(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var bas: BasType? = TypeLists.BAS!!.list(i_2)
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

    @JvmStatic
    fun method12115(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Camera.cameraCalculationType == 2) 1 else 0
    }

    @JvmStatic
    fun method12116(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method15511(icomponentdefinitions3!!, interface4!!, executor)
    }

    @JvmStatic
    fun ccSetTargetOpCursor(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setTargetOpCursor(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method296(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 >= 0 && i_2 < 2) {
            Client.anIntArrayArrayArray7302!![i_2] = Array(4) { IntArray(i_3 shl 1) }
        }
    }

    @JvmStatic
    fun popLong(executor: CS2Executor) {
        --executor.longStackPtr
    }

    @JvmStatic
    fun method6150(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition2 = executor.hookedInterface2
        } else {
            underlaydefinition2 = executor.hookedInterface1
        }
        var icomponentdefinitions4: Component? = underlaydefinition2!!.comp
        var interface5: Interface? = underlaydefinition2!!.inter
        method6045(icomponentdefinitions4!!, executor)
    }

    @JvmStatic
    fun method6151(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.y
    }

    @JvmStatic
    fun method6152(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        FriendsChat.kickPlayer(string_2!!)
    }

    @JvmStatic
    fun method6153(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.defaultToolkit!!.getValue()
    }

    @JvmStatic
    fun method13467(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.anInt3644
    }

    @JvmStatic
    fun getVarpbitOld(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        executor.intStack[++executor.intStackPtr - 1] = (executor.currentEntity as PlayerEntity).playerConfigs.getVarbit(i_2)
    }

    @JvmStatic
    fun strAppendNum(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var i_3 = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = string_2 + i_3
    }

    @JvmStatic
    fun method4988(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var class282Sub50Sub63: WorldMapElement? = WorldMap.poll(i_2)
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9540 - class282Sub50Sub63.anInt9539
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.anInt9535 - class282Sub50Sub63.anInt9541
        }
    }

    @JvmStatic
    fun method4989(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.sceneryShadows!!.enabled()) 1  else 0
    }

    @JvmStatic
    fun method7043(executor: CS2Executor) {
        var class282Sub50Sub62: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub62 != null) {
            var bool3 = class282Sub50Sub62.method14775(WorldMap.mapSquareAbsX + WorldMapSub1.anInt5123, WorldMap.mapSquareAbsY + WorldMapSub1.anInt5624, CS2Executor.anIntArray5900)
            if (bool3) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900!![1]
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.anIntArray5900!![2]
            } else {
                executor.intStack[++executor.intStackPtr - 1] = -1
                executor.intStack[++executor.intStackPtr - 1] = -1
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun method4797(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method2874(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun zoomWorldmap(executor: CS2Executor) {
        WorldMapInteraction.requestZoomChange(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun method4801(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method15256(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4802(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.slotId
    }

    @JvmStatic
    fun method11338(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method6158(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method11339(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (WorldMapSub1.aBool8027) 1  else 0
    }

    @JvmStatic
    fun method11340(executor: CS2Executor) {
        executor.intStackPtr -= 3
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(7, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method11341(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.groundDecoration!!.canModify(i_2)
    }

    @JvmStatic
    fun method11370(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendList.LOADED == 2 && i_2 < FriendList.FRIEND_COUNT) {
            executor.intStack[++executor.intStackPtr - 1] = FriendList.FRIENDS[i_2]!!.worldFlags
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method11372(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method7889(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method11373(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method6045(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4565(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        if (icomponentdefinitions3!!.containerItemId != -1) {
            executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.itemStackSize
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method4566(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (FriendsChat.FC_NAME != null && i_2 < FriendsChat.FC_PLAYER_COUNT) {
            executor.stringStack[++executor.stringStackPtr - 1] = FriendsChat.FC_PLAYERS!![i_2]!!.username
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun max(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = Math.max(i_2, i_3)
    }

    @JvmStatic
    fun sendSignupFormPacket(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStackPtr -= 2
        PacketEncoder.method2074(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1] == 1)
    }

    @JvmStatic
    fun method2071(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method1083(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun resumeHSLDialog(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_HSLDIALOG, Client.GAME_CONNECTION_CONTEXT!!.isaac)
        tcpmessage3!!.buffer.writeShort(i_2)
        Client.GAME_CONNECTION_CONTEXT!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun storeVarpbit(executor: CS2Executor) {
        var i_2 = executor.intOpValues!![executor.instrPtr]
        VarProviders.PLAYER_VAR_PROVIDER.setVarBitValue(i_2, executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun method2945(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.spritePitch
    }

    @JvmStatic
    fun method3913(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = Client.SKILL_LEVEL_VISIBLE[i_2]
    }

    @JvmStatic
    fun method1641(executor: CS2Executor) {
        var string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var i_3 = executor.intStack[--executor.intStackPtr]
        FriendsChat.setRank(string_2!!, i_3)
    }

    @JvmStatic
    fun method1643(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Client.aShort7436 = executor.intStack[executor.intStackPtr].toShort()
        if (Client.aShort7436 <= 0) {
            Client.aShort7436 = 256
        }
        Client.aShort7437 = executor.intStack[executor.intStackPtr + 1].toShort()
        if (Client.aShort7437 <= 0) {
            Client.aShort7437 = 205
        }
    }

    @JvmStatic
    fun getRunWeight(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.RUN_WEIGHT
    }

    @JvmStatic
    fun method13404(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.ambientSoundVolume!!.getValue()
    }

    @JvmStatic
    fun method8152(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method14482(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8153(executor: CS2Executor) {
        executor.intStackPtr -= 4
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], true, 256)
    }

    @JvmStatic
    fun method8154(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.fontId
    }

    @JvmStatic
    fun method8156(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.brightness!!.getValue()
    }

    @JvmStatic
    fun method8694(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method6690(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method8698(executor: CS2Executor) {
        if (Client.GUEST_CLAN_SETTINGS != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
            executor.currentClanSettings = Client.GUEST_CLAN_SETTINGS
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method8700(executor: CS2Executor) {
        var i_2 = executor.intStack[executor.intStackPtr - 3]
        var i_3 = executor.intStack[executor.intStackPtr - 2]
        var i_4 = executor.intStack[executor.intStackPtr - 1]
        executor.intStackPtr -= 3
        if (i_2 > executor.globalArrayLengths[i_3]) {
            throw RuntimeException()
        } else if (i_2 > executor.globalArrayLengths[i_4]) {
            throw RuntimeException()
        } else if (i_3 == i_4) {
            throw RuntimeException()
        } else {
            ArrayUtils.quicksort(executor.globalArrays[i_3], executor.globalArrays[i_4], 0, i_2 - 1)
        }
    }

    @JvmStatic
    fun method8701(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (executor.currentClanSettings!!.allowGuests) 1  else 0
    }

    @JvmStatic
    fun method8738(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 >= 1 && i_2 <= 2) {
            PreferenceManager.preferences!!.fixedScreen!!.setValue(i_2)
            PreferenceManager.preferences!!.fullScreen!!.setValue(i_2)
            PreferenceManager.savePreferences()
        }
    }

    @JvmStatic
    fun method14520(executor: CS2Executor) {
        executor.intStackPtr -= 4
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], 256)
    }

    @JvmStatic
    fun method14521(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        method4136(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method4146(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.x
    }

    @JvmStatic
    fun getCompHeight(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.height
    }

    @JvmStatic
    fun method4148(executor: CS2Executor) {
        executor.intStackPtr -= 2
        var i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_3 == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).subCategoryForHotkey(i_3.toChar())
        }
    }

    @JvmStatic
    fun method5595(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method1845(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun intNotEqual(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (executor.intStack[executor.intStackPtr] != executor.intStack[executor.intStackPtr + 1]) {
            executor.instrPtr += executor.intOpValues!![executor.instrPtr]
        }
    }

    @JvmStatic
    fun method5598(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.height
    }

    @JvmStatic
    fun method2841(executor: CS2Executor) {
        executor.intStack[executor.intStackPtr - 2] = TypeLists.QUEST!!.list(executor.intStack[executor.intStackPtr - 2]).levelRequirements!![executor.intStack[executor.intStackPtr - 1]][1]
        --executor.intStackPtr
    }

    @JvmStatic
    fun clanVarsEnabled(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (ClanVarTypeList.CLAN_VARS != null) 1 else 0
    }

    @JvmStatic
    fun method14640(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        method3672(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun ccSetItemAlwaysNum(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var interface4: Interface? = underlaydefinition2!!.inter
        setItemIFComp(icomponentdefinitions3!!, interface4!!, false, Component.StackMode.ALWAYS_NUM, executor)
    }

    @JvmStatic
    fun method15555(executor: CS2Executor) {
        var ints_2: IntArray? = executor.intStack
        var i_3 = ++executor.intStackPtr - 1
        val b_4 = if (PreferenceManager.preferences!!.groundDecoration!!.getValue() == 1) 1 else 0
        ints_2!![i_3] = b_4
    }

    @JvmStatic
    fun method8208(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        var interface4: Interface? = Interface.INTERFACES!![(i_2 shr 16)]
        assert(icomponentdefinitions3 != null)
        method8387(icomponentdefinitions3!!, executor)
    }

    @JvmStatic
    fun method7668(executor: CS2Executor) {
        WorldMapSub1.reset(executor.intStack[--executor.intStackPtr], -1, -1, false)
    }

    @JvmStatic
    fun method6696(executor: CS2Executor) {
        var i_2 = executor.intStack[--executor.intStackPtr]
        var icomponentdefinitions3: Component? = Component.fromHash(i_2)
        assert(icomponentdefinitions3 != null)
        executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.offsetY
    }

    @JvmStatic
    fun method2620(executor: CS2Executor) {
        var ifComp = executor.intStack[--executor.intStackPtr]
        var underlaydefinition3: CS2Interface? = null
        if (executor.aBool7022) {
            underlaydefinition3 = executor.hookedInterface2
        } else {
            underlaydefinition3 = executor.hookedInterface1
        }
        executor.intStack[++executor.intStackPtr - 1] = if (underlaydefinition3!!.setChild(ifComp, -1)) 1 else 0
    }

    @JvmStatic
    fun method1134(executor: CS2Executor) {
        var underlaydefinition2: CS2Interface? = if (executor.aBool7022) executor.hookedInterface2  else executor.hookedInterface1
        var icomponentdefinitions3: Component? = underlaydefinition2!!.comp
        var i_4 = executor.intStack[--executor.intStackPtr]
        var attributedefault5: ParamType? = TypeLists.PARAMS!!.list(i_4)
        if (attributedefault5!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = icomponentdefinitions3!!.method1998(i_4, attributedefault5!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = icomponentdefinitions3!!.method1997(i_4, attributedefault5!!.defaultInt)
        }
    }

    @JvmStatic
    fun method1135(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = executor.currentClanSettings!!.coinShare.toInt()
    }

    @JvmStatic
    fun method15511(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.spriteScale = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method14680(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        var i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        var i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (icomponentdefinitions0!!.mediaType != MediaType.NPC_MODEL) {
            throw RuntimeException("")
        } else {
            var npcdefinitions5: NPCType? = TypeLists.NPC!!.list(icomponentdefinitions0!!.primaryMediaId)
            if (icomponentdefinitions0!!.npcCustomisation == null) {
                icomponentdefinitions0!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, true)
            }
            icomponentdefinitions0!!.npcCustomisation!!.uid = CS2Executor.method1480()
            if (i_4 >= 0 && i_4 < npcdefinitions5!!.modelIds!!.size) {
                icomponentdefinitions0!!.npcCustomisation!!.modelIds[i_4] = i_3
                Component.redrawComponent(icomponentdefinitions0!!)
            } else {
                throw RuntimeException("" + i_4)
            }
        }
    }

    @JvmStatic
    fun getBoundingBox(transform: SceneGraphEntity, cs2executor: CS2Executor) {
        var bool3 = false
        var minX = 0
        var minY = 0
        var maxX = 0
        var maxY = 0
        if (transform!!.boundingBoxPolygons != null) {
            for (index in 0 until transform!!.boundingBoxPolygons!!.size) {
                var box: BoundingBoxPolygon? = transform!!.boundingBoxPolygons!![index]
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
                    if (!bool3 || left < minX) {
                        minX = left
                    }
                    if (!bool3 || bottom < minY) {
                        minY = bottom
                    }
                    if (!bool3 || right > maxX) {
                        maxX = right
                    }
                    if (!bool3 || top > maxY) {
                        maxY = top
                    }
                    bool3 = true
                }
            }
        }
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = if (bool3) 1  else 0
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = minX
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = minY
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = maxX
        cs2executor.intStack[++cs2executor.intStackPtr - 1] = maxY
    }

    @JvmStatic
    fun method4883(icomponentdefinitions0: Component, i_1: Int, i_2: Int) {
        if (icomponentdefinitions0!!.anIntArray1267 == null) {
            if (i_2 <= 0) {
                return
            }
            icomponentdefinitions0!!.keyTriggeringAction = Array(11) { ByteArray(0) }
            icomponentdefinitions0!!.keyTriggerParams = Array(11) { ByteArray(0) }
            icomponentdefinitions0!!.cycleSteps = IntArray(11)
            icomponentdefinitions0!!.anIntArray1267 = IntArray(11)
        }
        icomponentdefinitions0!!.anIntArray1267!![i_1] = i_2
        if (i_2 > 0) {
            icomponentdefinitions0!!.hasInteraction = true
        } else {
            icomponentdefinitions0!!.hasInteraction = false
            val keyTriggeringActionArr = icomponentdefinitions0!!.keyTriggeringAction!!
            for (i_5 in 0 until keyTriggeringActionArr.size) {
                if (keyTriggeringActionArr[i_5] != null || icomponentdefinitions0!!.anIntArray1267!![i_5] > 0) {
                    icomponentdefinitions0!!.hasInteraction = true
                    break
                }
            }
        }
    }

    @JvmStatic
    fun method15207(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        var i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        var i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (icomponentdefinitions0!!.mediaType != MediaType.NPC_MODEL && icomponentdefinitions0!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            var npcdefinitions5: NPCType? = TypeLists.NPC!!.list(icomponentdefinitions0!!.primaryMediaId)
            if (icomponentdefinitions0!!.npcCustomisation == null) {
                icomponentdefinitions0!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, icomponentdefinitions0!!.mediaType == MediaType.NPC_MODEL)
            }
            icomponentdefinitions0!!.npcCustomisation!!.uid = CS2Executor.method1480()
            if (npcdefinitions5!!.recolorDst != null && i_4 >= 0 && i_4 < npcdefinitions5!!.recolorDst!!.size) {
                icomponentdefinitions0!!.npcCustomisation!!.recolorDst!![i_4] = i_3.toShort()
                Component.redrawComponent(icomponentdefinitions0!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

    @JvmStatic
    fun method1852(component: Component, intr: Interface, cs2: CS2Executor) {
        cs2.intStackPtr -= 2
        component!!.scrollX = cs2.intStack[cs2.intStackPtr]
        if (component!!.scrollX > component!!.scrollWidth - component!!.width) {
            component!!.scrollX = component!!.scrollWidth - component!!.width
        }
        if (component!!.scrollX < 0) {
            component!!.scrollX = 0
        }
        component!!.scrollY = cs2.intStack[cs2.intStackPtr + 1]
        if (component!!.scrollY > component!!.scrollHeight - component!!.height) {
            component!!.scrollY = component!!.scrollHeight - component!!.height
        }
        if (component!!.scrollY < 0) {
            component!!.scrollY = 0
        }
        Component.redrawComponent(component)
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIfscrollposUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method6289(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onMouseOver = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method7927(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        cs2executor1.intStackPtr -= 2
        var i_3 = cs2executor1.intStack[cs2executor1.intStackPtr] - 1
        var i_4 = cs2executor1.intStack[cs2executor1.intStackPtr + 1]
        if (i_3 >= 0 && i_3 <= 9) {
            method4883(icomponentdefinitions0!!, i_3, i_4)
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun method3078(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onHold = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method3672(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onMouseMove = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setOnMouseLeave(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        var ints_5: IntArray? = popIntArrayParam(string_4, cs2executor2)
        if (ints_5 != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.mouseLeaveScript = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.mouseLeaveArrayParams = ints_5
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method7889(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.hookrequestType1Params = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method774(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.flipHorizontal = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun ifSetModelOrigin(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        icomponentdefinitions0!!.originX = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.originY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method2874(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1319 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method7267(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.usesOrthogonal = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method5009(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1413 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method7142(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.shadow = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method6288(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        var ints_5: IntArray? = popIntArrayParam(string_4, cs2executor2)
        if (ints_5 != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onScrollWheel = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.mouseWheelArray = ints_5
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method2915(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.spriteShadow = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method8754(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.filled = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun ifSetColor(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.color = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfcolorUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method6689(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        var ints_5: IntArray? = popIntArrayParam(string_4, cs2executor2)
        if (ints_5 != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1397 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.varps = ints_5
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method6690(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1361 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method4159(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        icomponentdefinitions0!!.modelTintRed = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.modelTintGreen = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        icomponentdefinitions0!!.modelTintBlue = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        icomponentdefinitions0!!.modelTintScalar = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method4160(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.borderThickness = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method11221(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1393 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method11223(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onMouseLeaveScript = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method501(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.tiling = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method502(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        var ints_5: IntArray? = popIntArrayParam(string_4, cs2executor2)
        if (ints_5 != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onKey = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.keyPressArray = ints_5
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method505(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        var string_5: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        var attributedefault6: ParamType? = TypeLists.PARAMS!!.list(i_4)
        if (!attributedefault6!!.typeName.equals(string_5!!)) {
            icomponentdefinitions0!!.method2000(i_4, string_5)
        } else {
            icomponentdefinitions0!!.method1995(i_4)
        }
    }

    @JvmStatic
    fun method506(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        icomponentdefinitions0!!.scrollWidth = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.scrollHeight = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icomponentdefinitions0, false)
        }
    }

    @JvmStatic
    fun method2738(interface0: Interface, i_1: Int, i_2: Int, i_3: Int, bool4: Boolean, cs2executor5: CS2Executor) {
        if (i_2 == 0) {
            throw RuntimeException()
        } else {
            var icomponentdefinitions7: Component? = interface0.components[i_1]
            if (icomponentdefinitions7!!.slotChildren == null) {
                @Suppress("UNCHECKED_CAST")
                icomponentdefinitions7!!.slotChildren = arrayOfNulls<Component>(i_3 + 1) as Array<Component>?
                @Suppress("UNCHECKED_CAST")
                icomponentdefinitions7!!.itemSlots = icomponentdefinitions7!!.slotChildren as Array<Component?>?
            }
            if (icomponentdefinitions7!!.slotChildren!!.size <= i_3) {
                @Suppress("UNCHECKED_CAST")
                var arr_8: Array<Component>? = arrayOfNulls<Component>(i_3 + 1) as Array<Component>?
                if (icomponentdefinitions7!!.slotChildren == icomponentdefinitions7!!.itemSlots) {
                    System.arraycopy(icomponentdefinitions7!!.slotChildren, 0, arr_8, 0, icomponentdefinitions7!!.slotChildren!!.size)
                    icomponentdefinitions7!!.slotChildren = arr_8
                    @Suppress("UNCHECKED_CAST")
                    icomponentdefinitions7!!.itemSlots = arr_8 as Array<Component?>?
                } else {
                    @Suppress("UNCHECKED_CAST")
                    var arr_11: Array<Component?>? = arrayOfNulls<Component>(i_3 + 1)
                    for (i_10 in 0 until icomponentdefinitions7!!.slotChildren!!.size) {
                        arr_8!![i_10] = icomponentdefinitions7!!.slotChildren!![i_10]
                        arr_11!![i_10] = icomponentdefinitions7!!.itemSlots!![i_10]
                    }
                    icomponentdefinitions7!!.slotChildren = arr_8
                    icomponentdefinitions7!!.itemSlots = arr_11
                }
            }
            if (i_3 > 0 && icomponentdefinitions7!!.slotChildren!![i_3 - 1] == null) {
                throw RuntimeException("" + (i_3 - 1))
            } else {
                var icomponentdefinitions12: Component? = Component()
                icomponentdefinitions12!!.type = ComponentType.forId(i_2)
                icomponentdefinitions12!!.idHash = icomponentdefinitions7!!.idHash
                icomponentdefinitions12!!.parentId = icomponentdefinitions7!!.idHash
                icomponentdefinitions12!!.slotId = i_3
                icomponentdefinitions7!!.slotChildren!![i_3] = icomponentdefinitions12
                if (icomponentdefinitions7!!.slotChildren != icomponentdefinitions7!!.itemSlots) {
                    icomponentdefinitions7!!.itemSlots!![i_3] = icomponentdefinitions12
                }
                var underlaydefinition13: CS2Interface? = null
                if (bool4) {
                    underlaydefinition13 = cs2executor5.hookedInterface2
                } else {
                    underlaydefinition13 = cs2executor5.hookedInterface1
                }
                underlaydefinition13!!.inter = interface0
                underlaydefinition13!!.comp = icomponentdefinitions12
                Component.redrawComponent(icomponentdefinitions7!!)
            }
        }
    }

    @JvmStatic
    fun method2741(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var bool5: Boolean = false
        bool5 = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        icomponentdefinitions0!!.alpha = bool5
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method8387(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.anInt1380 = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method8390(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        var i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (i_4 != icomponentdefinitions0!!.spriteId) {
            icomponentdefinitions0!!.spriteId = i_4
            Component.redrawComponent(icomponentdefinitions0!!)
        }
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfgraphicUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method15256(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.anInt1381 = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method4651(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1292 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method6313(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        icomponentdefinitions0!!.skyboxId = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.lightPositionX = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        icomponentdefinitions0!!.lightPositionY = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        icomponentdefinitions0!!.lightPositionZ = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method786(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        var bool4 = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        if (bool4 != icomponentdefinitions0!!.hidden) {
            icomponentdefinitions0!!.hidden = bool4
            Component.redrawComponent(icomponentdefinitions0!!)
        }
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfhide(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method789(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        if (icomponentdefinitions0!!.type == ComponentType.SPRITE) {
            method5459(icomponentdefinitions0!!, interface1!!, cs2executor2)
        }
    }

    @JvmStatic
    fun method5017(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1383 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method2955(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.moveOverCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method4772(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1419 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setTriggerKey(icomponentdefinitions0: Component, i_1: Int, bytes_2: ByteArray?, bytes_3: ByteArray?) {
        if (icomponentdefinitions0!!.keyTriggeringAction == null) {
            if (bytes_2 == null) {
                return
            }
            icomponentdefinitions0!!.keyTriggeringAction = Array(11) { ByteArray(0) }
            icomponentdefinitions0!!.keyTriggerParams = Array(11) { ByteArray(0) }
            icomponentdefinitions0!!.cycleSteps = IntArray(11)
            icomponentdefinitions0!!.anIntArray1267 = IntArray(11)
        }
        icomponentdefinitions0!!.keyTriggeringAction!![i_1] = bytes_2!!
        if (bytes_2 != null) {
            icomponentdefinitions0!!.hasInteraction = true
        } else {
            icomponentdefinitions0!!.hasInteraction = false
            for (i_6 in 0 until icomponentdefinitions0!!.keyTriggeringAction!!.size) {
                if (icomponentdefinitions0!!.keyTriggeringAction!![i_6] != null || icomponentdefinitions0!!.anIntArray1267!![i_6] > 0) {
                    icomponentdefinitions0!!.hasInteraction = true
                    break
                }
            }
        }
        icomponentdefinitions0!!.keyTriggerParams!![i_1] = bytes_3!!
    }

    @JvmStatic
    fun ifSetNPCHead(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.mediaType = MediaType.NPC_HEAD
        icomponentdefinitions0!!.npcCustomisation = null
        icomponentdefinitions0!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method2960(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.textAntiMacro = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedClearIftextantimacro(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method11160(class282Sub50Sub70: MiniMenuEntry?, cs2executor1: CS2Executor) {
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = MiniMenu.getClickType(class282Sub50Sub70)
        cs2executor1.stringStack[++cs2executor1.stringStackPtr - 1] = MiniMenu.method13768(class282Sub50Sub70)
        cs2executor1.stringStack[++cs2executor1.stringStackPtr - 1] = MiniMenu.method7303(class282Sub50Sub70)
        cs2executor1.stringStack[++cs2executor1.stringStackPtr - 1] = MiniMenu.addQuestChaticons(class282Sub50Sub70)
    }

    @JvmStatic
    fun method2593(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1394 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method13468(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.clickMask = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfclickmaskUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method4136(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1409 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setItemIFComp(ifComp: Component, interface1: Interface, wearCol: Boolean, renderStack: Component.StackMode, cs2executor4: CS2Executor) {
        cs2executor4.intStackPtr -= 2
        var itemId = cs2executor4.intStack[cs2executor4.intStackPtr]
        var i_7 = cs2executor4.intStack[cs2executor4.intStackPtr + 1]
        if (ifComp.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfitemUpdate(ifComp.idHash)
            PulseEventQueue.delayedIfangleUpdate(ifComp.idHash)
            PulseEventQueue.delayedIfoffsetUpdate(ifComp.idHash)
        }
        if (itemId == -1) {
            ifComp.mediaType = MediaType.RAW_MODEL
            ifComp.primaryMediaId = -1
            ifComp.containerItemId = -1
        } else {
            ifComp.containerItemId = itemId
            ifComp.itemStackSize = i_7
            ifComp.wearCol = wearCol
            var itemdefinitions8: ItemType? = TypeLists.OBJ!!.list(itemId)
            ifComp.spritePitch = itemdefinitions8!!.modelRotationX
            ifComp.spriteRoll = itemdefinitions8!!.modelRotationY
            ifComp.spriteYaw = itemdefinitions8!!.modelRotationZ
            ifComp.offsetX = itemdefinitions8!!.modelOffsetX
            ifComp.offsetY = itemdefinitions8!!.modelOffsetY
            ifComp.spriteScale = itemdefinitions8!!.modelZoom
            ifComp.renderStack = renderStack
            if (ifComp.aspectWidth > 0) {
                ifComp.spriteScale = ifComp.spriteScale * 32 / ifComp.aspectWidth
            } else if (ifComp.baseWidth > 0) {
                ifComp.spriteScale = ifComp.spriteScale * 32 / ifComp.baseWidth
            }
        }
    }

    @JvmStatic
    fun setCompText(component: Component, intr: Interface, cs2: CS2Executor) {
        var text: String? = cs2.stringStack[--cs2.stringStackPtr] as String?
        if (!text.equals(component!!.text)) {
            component!!.text = text ?: ""
            Component.redrawComponent(component)
        }
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIftextUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setOpBase(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.opBase = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            icomponentdefinitions0!!.opBase += "(opBase)"
    }

    @JvmStatic
    fun method14482(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        var i_5 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (i_5 >= 1 && i_5 <= 10) {
            icomponentdefinitions0!!.addCursor(i_5 - 1, i_4)
        }
    }

    @JvmStatic
    fun method1083(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1451 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setIfTextAlign(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 3
        icomponentdefinitions0!!.textHorizontalAli = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.textVerticalAli = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        icomponentdefinitions0!!.anInt1358 = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method2170(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.hookrequestType0Params = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method14605(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        var b_3 = 10
        var i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        method4883(icomponentdefinitions0!!, b_3, i_4)
    }

    @JvmStatic
    fun method6186(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        if (icomponentdefinitions0!!.type == ComponentType.TEXT) {
            method5459(icomponentdefinitions0!!, interface1!!, cs2executor2)
        }
    }

    @JvmStatic
    fun method8310(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.angle2d = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method542(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1407 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setIfFont(component: Component, intr: Interface, cs2: CS2Executor) {
        component!!.fontId = cs2.intStack[--cs2.intStackPtr]
        Component.redrawComponent(component)
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIffontUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun method7729(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        icomponentdefinitions0!!.sunRotationX = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.sunRotationY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun setComponentPosition(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        icomponentdefinitions0!!.basePositionX = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.basePositionY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        var i_4 = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        if (i_4 < 0) {
            i_4 = 0
        } else if (i_4 > 5) {
            i_4 = 5
        }
        var i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        if (i_5 < 0) {
            i_5 = 0
        } else if (i_5 > 5) {
            i_5 = 5
        }
        icomponentdefinitions0!!.aspectXType = i_4.toByte()
        icomponentdefinitions0!!.aspectYType = i_5.toByte()
        Component.redrawComponent(icomponentdefinitions0!!)
        method913(interface1, icomponentdefinitions0)
        if (icomponentdefinitions0!!.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icomponentdefinitions0, false)
        }
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfpositionUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method1454(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        var i_4 = cs2executor2.intStack[cs2executor2.intStackPtr]
        var i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        var attributedefault6: ParamType? = TypeLists.PARAMS!!.list(i_4)
        if (i_5 != attributedefault6!!.defaultInt) {
            icomponentdefinitions0!!.method1999(i_4, i_5)
        } else {
            icomponentdefinitions0!!.method1995(i_4)
        }
    }

    @JvmStatic
    fun method1455(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 4
        icomponentdefinitions0!!.primaryMediaId = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.secondaryMediaId = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        if (cs2executor2.intStack[cs2executor2.intStackPtr + 2] == 1) {
            icomponentdefinitions0!!.mediaType = MediaType.ITEM_CONTAINER_FEMALE
        } else {
            icomponentdefinitions0!!.mediaType = MediaType.ITEM_CONTAINER_MALE
        }
        icomponentdefinitions0!!.wearCol = cs2executor2.intStack[cs2executor2.intStackPtr + 3] == 1
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method14653(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1353 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method12260(icDefs: Component, interface1: Interface, executor: CS2Executor) {
        executor.intStackPtr -= 4
        icDefs.baseWidth = executor.intStack[executor.intStackPtr]
        icDefs.baseHeight = executor.intStack[executor.intStackPtr + 1]
        icDefs.aspectWidth = 0
        icDefs.aspectHeight = 0
        var i_4 = executor.intStack[executor.intStackPtr + 2]
        if (i_4 < 0) {
            i_4 = 0
        } else if (i_4 > 4) {
            i_4 = 4
        }
        var i_5 = executor.intStack[executor.intStackPtr + 3]
        if (i_5 < 0) {
            i_5 = 0
        } else if (i_5 > 4) {
            i_5 = 4
        }
        icDefs.aspectWidthType = i_4.toByte()
        icDefs.aspectHeightType = i_5.toByte()
        Component.redrawComponent(icDefs)
        method913(interface1, icDefs)
        if (icDefs.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icDefs, false)
        }
    }

    @JvmStatic
    fun method7555(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.mediaType = MediaType.RAW_MODEL
        icomponentdefinitions0!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method6010(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 3
        var i_4 = cs2executor2.intStack[cs2executor2.intStackPtr]
        var s_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1].toShort()
        var s_6 = cs2executor2.intStack[cs2executor2.intStackPtr + 2].toShort()
        if (i_4 >= 0 && i_4 < 5) {
            icomponentdefinitions0!!.retexture(i_4, s_5, s_6)
            Component.redrawComponent(icomponentdefinitions0!!)
            if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
                PulseEventQueue.delayedIfretex(icomponentdefinitions0!!.idHash, i_4)
            }
        }
    }

    @JvmStatic
    fun method1845(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1271 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method3365(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onRelease = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method3366(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1416 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setTargetVerb(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.targetVerb = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            icomponentdefinitions0!!.targetVerb += "(targetVerb)"
    }

    @JvmStatic
    fun method3965(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        var fontmetrics3: FontMetrics? = icomponentdefinitions0!!.getFontMetrics(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
        var i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        var i_5 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        var i_6 = fontmetrics3!!.method6956(icomponentdefinitions0!!.text, icomponentdefinitions0!!.width, icomponentdefinitions0!!.anInt1358, i_5, i_4, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = i_6
    }

    @JvmStatic
    fun method3987(icomponentdefinitions0: Component, interface1: Interface) {
        icomponentdefinitions0!!.mediaType = MediaType.PLAYER_HEAD
        icomponentdefinitions0!!.primaryMediaId = PlayerUpdate.myPlayerIndex
        icomponentdefinitions0!!.secondaryMediaId = 0
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method1494(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        var i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        var i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (icomponentdefinitions0!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            var npcdefinitions5: NPCType? = TypeLists.NPC!!.list(icomponentdefinitions0!!.primaryMediaId)
            if (icomponentdefinitions0!!.npcCustomisation == null) {
                icomponentdefinitions0!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, false)
            }
            icomponentdefinitions0!!.npcCustomisation!!.uid = CS2Executor.method1480()
            if (i_4 >= 0 && i_4 < npcdefinitions5!!.headModels!!.size) {
                icomponentdefinitions0!!.npcCustomisation!!.modelIds[i_4] = i_3
                Component.redrawComponent(icomponentdefinitions0!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

    @JvmStatic
    fun setResumeText(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.resumeText = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (Loader.DEBUG_IF_OPS)
            icomponentdefinitions0!!.resumeText += "(resumeText)"
    }

    @JvmStatic
    fun method12401(icomponentdefinitions0: Component, interface1: Interface) {
        icomponentdefinitions0!!.mediaType = MediaType.PLAYER_MODEL
        icomponentdefinitions0!!.primaryMediaId = PlayerUpdate.myPlayerIndex
        icomponentdefinitions0!!.secondaryMediaId = 0
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method14892(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.lineWidth = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method4276(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 6
        icomponentdefinitions0!!.offsetX = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.offsetY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        icomponentdefinitions0!!.spritePitch = cs2executor2.intStack[cs2executor2.intStackPtr + 2]
        icomponentdefinitions0!!.spriteRoll = cs2executor2.intStack[cs2executor2.intStackPtr + 3]
        icomponentdefinitions0!!.spriteYaw = cs2executor2.intStack[cs2executor2.intStackPtr + 4]
        icomponentdefinitions0!!.spriteScale = cs2executor2.intStack[cs2executor2.intStackPtr + 5]
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfangleUpdate(icomponentdefinitions0!!.idHash)
            PulseEventQueue.delayedIfoffsetUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method4280(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var bool5: Boolean = false
        bool5 = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        icomponentdefinitions0!!.flipVertical = bool5
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method12630(icomponentdefinitions0: Component) {
        icomponentdefinitions0!!.reset()
    }

    @JvmStatic
    fun method5066(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.mediaType = MediaType.PLAYER_MODEL
        icomponentdefinitions0!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method13450(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        icomponentdefinitions0!!.lineDirection = i_4 == 1
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method6942(component: Component) {
        if (Client.getComponentSettings(component)!!.continueOptionEnabled() && Client.aClass1187352 == null) {
            PacketEncoder.continueInterfacePressed(component!!.idHash, component!!.slotId)
            Client.aClass1187352 = Component.listIcomponent(component!!.idHash, component!!.slotId)
            assert(Client.aClass1187352 != null)
            Component.redrawComponent(Client.aClass1187352!!)
        }
    }

    @JvmStatic
    fun method3601(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        var i_3 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        var i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr] - 1
        if (icomponentdefinitions0!!.mediaType != MediaType.NPC_MODEL && icomponentdefinitions0!!.mediaType != MediaType.NPC_HEAD) {
            throw RuntimeException("")
        } else {
            var npcdefinitions5: NPCType? = TypeLists.NPC!!.list(icomponentdefinitions0!!.primaryMediaId)
            if (icomponentdefinitions0!!.npcCustomisation == null) {
                icomponentdefinitions0!!.npcCustomisation = NPCTypeCustomisation(npcdefinitions5!!, icomponentdefinitions0!!.mediaType == MediaType.NPC_MODEL)
            }
            icomponentdefinitions0!!.npcCustomisation!!.uid = CS2Executor.method1480()
            if (npcdefinitions5!!.retextureDst != null && i_4 >= 0 && i_4 < npcdefinitions5!!.retextureDst!!.size) {
                icomponentdefinitions0!!.npcCustomisation!!.retextureDst!![i_4] = i_3.toShort()
                Component.redrawComponent(icomponentdefinitions0!!)
            } else {
                throw RuntimeException("")
            }
        }
    }

    @JvmStatic
    fun method5291(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr] - 1
        if (i_4 >= 0 && i_4 <= 9) {
            icomponentdefinitions0!!.addOptionName(i_4, cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String)
        } else {
            --cs2executor2.stringStackPtr
        }
    }

    @JvmStatic
    fun method797(component: Component, intr: Interface, cs2: CS2Executor) {
        var sequenceId = cs2.intStack[--cs2.intStackPtr]
        if (sequenceId != component!!.animation) {
            if (sequenceId != -1) {
                if (component!!.animatingSequence == null) {
                    component!!.animatingSequence = InterfaceSequence()
                }
                component!!.animatingSequence!!.update(sequenceId)
            } else {
                component!!.animatingSequence = null
            }
            component!!.animation = sequenceId
            Component.redrawComponent(component)
        }
        if (component!!.slotId == -1 && !intr.preventInteraction) {
            PulseEventQueue.delayedIfanimUpdate(component!!.idHash)
        }
    }

    @JvmStatic
    fun setTransparency(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.transparency = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method5455(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1418 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method5459(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        var i_4 = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (i_4 != icomponentdefinitions0!!.anInt1435) {
            icomponentdefinitions0!!.anInt1435 = i_4
            Component.redrawComponent(icomponentdefinitions0!!)
        }
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.method5581(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method569(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.maxTextLines = cs2executor2.intStack[--cs2executor2.intStackPtr]
        Component.redrawComponent(icomponentdefinitions0!!)
    }

    @JvmStatic
    fun method3251(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        if (cs2executor1.hookRequestCount >= 10) {
            throw RuntimeException()
        } else {
            if (icomponentdefinitions0!!.anObjectArray1271 != null) {
                var hookrequest3: HookRequest? = HookRequest()
                hookrequest3!!.source = icomponentdefinitions0
                hookrequest3!!.params = icomponentdefinitions0!!.anObjectArray1271
                hookrequest3!!.requestId = cs2executor1.hookRequestCount + 1
                Client.PENDING_HOOK_REQUESTS.addFirst(hookrequest3)
            }
        }
    }

    @JvmStatic
    fun method5001(icomponentdefinitions0: Component, cs2executor1: CS2Executor) {
        var fontmetrics3: FontMetrics? = icomponentdefinitions0!!.getFontMetrics(StaticMethods.initializedFontcombos!!, Client.FONT_PROVIDER)
        var i_4 = cs2executor1.intStack[--cs2executor1.intStackPtr]
        var point5: Point? = fontmetrics3!!.method6954(icomponentdefinitions0!!.text, icomponentdefinitions0!!.width, icomponentdefinitions0!!.anInt1358, i_4, @Suppress("UNCHECKED_CAST") (StaticMedia.nameIconSprites as Array<Sprite>?))
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = point5!!.x
        cs2executor1.intStack[++cs2executor1.intStackPtr - 1] = point5!!.y
    }

    @JvmStatic
    fun method3338(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        var ints_5: IntArray? = popIntArrayParam(string_4, cs2executor2)
        if (ints_5 != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1387 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.anIntArray1402 = ints_5
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun ifSetAspect(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        icomponentdefinitions0!!.aspectX = cs2executor2.intStack[cs2executor2.intStackPtr]
        icomponentdefinitions0!!.aspectY = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        Component.redrawComponent(icomponentdefinitions0!!)
        method913(interface1, icomponentdefinitions0)
        if (icomponentdefinitions0!!.type == ComponentType.CONTAINER) {
            Component.method483(interface1, icomponentdefinitions0, false)
        }
    }

    @JvmStatic
    fun method913(interface0: Interface, icomponentdefinitions1: Component) {
        var icomponentdefinitions3: Component? = Interface.getSub(interface0, icomponentdefinitions1)
        var i_4: Int = 0
        var i_5: Int = 0
        if (icomponentdefinitions3 == null) {
            i_4 = GameShell.screenCanvasWidth
            i_5 = GameShell.screenCanvasHeight
        } else {
            i_4 = icomponentdefinitions3!!.width
            i_5 = icomponentdefinitions3!!.height
        }
        InterfaceRenderer.initSizes(icomponentdefinitions1!!, i_4, i_5, false)
        InterfaceComponentRenderer.componentPositionAdjustment(icomponentdefinitions1!!, i_4, i_5)
    }

    @JvmStatic
    fun setDepthChild(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        var i_4 = cs2executor2.intStack[cs2executor2.intStackPtr]
        var i_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1]
        if (i_4 == -1 && i_5 == -1) {
            icomponentdefinitions0!!.depthChildOverride = null
        } else {
            icomponentdefinitions0!!.depthChildOverride = Component.listIcomponent(i_4, i_5)
        }
    }

    @JvmStatic
    fun method14586(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.hookParams = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method6045(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.targetLeaveCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
        icomponentdefinitions0!!.targetOverCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun setOnMouseOver(iCompDefs: Component, cs2executor2: CS2Executor) {
        var paramTypes: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(paramTypes, cs2executor2) != null) {
            paramTypes = paramTypes!!.substring(0, paramTypes!!.length - 1)
        }
        iCompDefs.popupScript = popParamsDynamic(paramTypes, cs2executor2)
        iCompDefs.hasScript = true
    }

    @JvmStatic
    fun method869(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var i_4: Component.UnkSetting1? = Component.UnkSetting1.forId(cs2executor2.intStack[--cs2executor2.intStackPtr])
        if (i_4 != null)
            icomponentdefinitions0.anUnkSetting11 = i_4
    }

    @JvmStatic
    fun method910(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.params = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setNoClickThrough(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.preventClickThrough = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
    }

    @JvmStatic
    fun method12209(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 2
        var b_4 = 10
        @Suppress("UNCHECKED_CAST")
        var bytes_5: ByteArray? = cs2executor2.globalArrays[cs2executor2.intStack[cs2executor2.intStackPtr]] as ByteArray?
        @Suppress("UNCHECKED_CAST")
        var bytes_6: ByteArray? = cs2executor2.globalArrays[cs2executor2.intStack[cs2executor2.intStackPtr + 1]] as ByteArray?
        setTriggerKey(icomponentdefinitions0!!, b_4, bytes_5, bytes_6)
    }

    @JvmStatic
    fun method1838(icomponentdefinitions0: Component, bytes_1: ByteArray?, bytes_2: ByteArray?, cs2executor3: CS2Executor) {
        var i_5 = cs2executor3.intStack[--cs2executor3.intStackPtr] - 1
        if (i_5 >= 0 && i_5 <= 9) {
            setTriggerKey(icomponentdefinitions0!!, i_5, bytes_1, bytes_2)
        } else {
            throw RuntimeException()
        }
    }

    @JvmStatic
    fun method15409(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.onClick = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method6158(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1415 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun method11337(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.mediaType = MediaType.NPC_MODEL
        icomponentdefinitions0!!.npcCustomisation = null
        icomponentdefinitions0!!.primaryMediaId = cs2executor2.intStack[--cs2executor2.intStackPtr]
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayIfcontentUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method15474(icomponentdefinitions0: Component) {
        icomponentdefinitions0!!.options = null
    }

    @JvmStatic
    fun method1642(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        var string_4: String? = cs2executor2.stringStack[--cs2executor2.stringStackPtr] as String?
        if (popIntArrayParam(string_4, cs2executor2) != null) {
            string_4 = string_4!!.substring(0, string_4!!.length - 1)
        }
        icomponentdefinitions0!!.anObjectArray1400 = popParamsDynamic(string_4, cs2executor2)
        icomponentdefinitions0!!.hasScript = true
    }

    @JvmStatic
    fun setTargetOpCursor(icomponentdefinitions0: Component, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.targetOpCursor = cs2executor2.intStack[--cs2executor2.intStackPtr]
    }

    @JvmStatic
    fun method7667(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        cs2executor2.intStackPtr -= 3
        var i_4 = cs2executor2.intStack[cs2executor2.intStackPtr]
        var s_5 = cs2executor2.intStack[cs2executor2.intStackPtr + 1].toShort()
        var s_6 = cs2executor2.intStack[cs2executor2.intStackPtr + 2].toShort()
        if (i_4 >= 0 && i_4 < 5) {
            icomponentdefinitions0!!.recolor(i_4, s_5, s_6)
            Component.redrawComponent(icomponentdefinitions0!!)
            if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
                PulseEventQueue.delayedIfrecol(icomponentdefinitions0!!.idHash, i_4)
            }
        }
    }

    @JvmStatic
    fun method6697(icomponentdefinitions0: Component, interface1: Interface, cs2executor2: CS2Executor) {
        icomponentdefinitions0!!.monospaced = cs2executor2.intStack[--cs2executor2.intStackPtr] == 1
        Component.redrawComponent(icomponentdefinitions0!!)
        if (icomponentdefinitions0!!.slotId == -1 && !interface1.preventInteraction) {
            PulseEventQueue.delayedIfmonospacedUpdate(icomponentdefinitions0!!.idHash)
        }
    }

    @JvmStatic
    fun method2598(): CreateAccountRequest? {
        return if (AccountCreationStage.aClass4942916 == null) CreateAccountRequest.aClass4945786 else AccountCreationStage.aClass4942916
    }

    @JvmStatic
    fun queryItem(queryParam: String?, tradeableOnly: Boolean, paramId: Int, paramIntQuery: Int, paramStringQuery: String?, paramIsString: Boolean) {
	    val query = queryParam!!.lowercase()
	    var results = ShortArray(16)
	    var paramIntDefault = -1
	    var paramStrDefault: String? = null
	    if (paramId != -1) {
	        var paramDef: ParamType? = TypeLists.PARAMS!!.list(paramId)
	        if (paramDef == null || paramDef!!.isString() != paramIsString) {
	            return
	        }
	        if (paramDef!!.isString()) {
	            paramStrDefault = paramDef!!.typeName
	        } else {
	            paramIntDefault = paramDef!!.defaultInt
	        }
	    }
	    var numResults = 0
	    for (i in 0 until TypeLists.OBJ!!.totalItemCount) {
	        var itemDef: ItemType? = TypeLists.OBJ!!.list(i)
	        if ((!tradeableOnly || itemDef!!.tradeable) && itemDef!!.certTemplateId == -1 && itemDef!!.lendTemplateId == -1 && itemDef!!.bindTemplateId == -1 && itemDef!!.unknownInt6 == 0 && (itemDef!!.name ?: "").lowercase().indexOf(query) != -1) {
	            if (paramId != -1) {
	                if (paramIsString) {
	                    if (!paramStringQuery.equals(itemDef!!.getCs2String(paramId, paramStrDefault ?: ""))) {
	                        continue
	                    }
	                } else if (paramIntQuery != itemDef!!.getCs2Integer(paramId, paramIntDefault)) {
	                    continue
	                }
	            }
	            if (numResults >= 250) {
	                CS2_QUERY_RESULTS_LEN = -1
	                ColorImageCache.CS2_QUERY_RESULTS = null
	                return
	            }
	            if (numResults >= results.size) {
	                var newResults = ShortArray(results.size * 2)
	                System.arraycopy(results, 0, newResults, 0, numResults)
	                results = newResults
	            }
	            results[numResults++] = i.toShort()
	        }
	    }
	    ColorImageCache.CS2_QUERY_RESULTS = results
	    StaticElements.CS2_QUERY_RESULT_IDX = 0
	    CS2_QUERY_RESULTS_LEN = numResults
	    var resultNames: Array<String?> = arrayOfNulls<String>(CS2_QUERY_RESULTS_LEN)
	    for (i_12 in 0 until CS2_QUERY_RESULTS_LEN) {
	        resultNames[i_12] = TypeLists.OBJ!!.list(results[i_12].toInt()).name
	    }
	    @Suppress("UNCHECKED_CAST")
        Utils.sortAlphabetically(resultNames as Array<String>, ColorImageCache.CS2_QUERY_RESULTS)
    }

    @JvmStatic
    fun popIntArrayParam(paramTypes: String?, cs2executor1: CS2Executor): IntArray? {
        var intArr: IntArray? = null
        if (!paramTypes.isNullOrEmpty() && paramTypes[paramTypes.length - 1] == 'Y') {
            var size = cs2executor1.intStack[--cs2executor1.intStackPtr]
            if (size > 0) {
                intArr = IntArray(size)
                while (size-- > 0) {
                    intArr[size] = cs2executor1.intStack[--cs2executor1.intStackPtr]
                }
            }
        }
        return intArr
    }

    @JvmStatic
    fun popParamsDynamic(paramTypes: String?, executor: CS2Executor): Array<Any?>? {
        if (paramTypes == null) return null
        var params: Array<Any?>? = arrayOfNulls(paramTypes.length + 1)
        for (i in params!!.size - 1 downTo 1) {
            if (paramTypes[i - 1] == 's') {
                params[i] = executor.stringStack[--executor.stringStackPtr]
            } else if (paramTypes[i - 1] == '\u00a7') {
                params[i] = java.lang.Long.valueOf(executor.longStack[--executor.longStackPtr])
            } else {
                params[i] = Integer.valueOf(executor.intStack[--executor.intStackPtr])
            }
        }
        val scriptId = executor.intStack[--executor.intStackPtr]
        if (scriptId != -1) {
            params[0] = Integer.valueOf(scriptId)
        } else {
            params = null
        }
        return params
    }

}
