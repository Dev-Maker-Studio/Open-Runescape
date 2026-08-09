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

object QuickChatDisplayOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.QC_GETNAME -> qcGetName(exec)
            ClientScriptOpCode.CHATCAT_GETDESC -> chatCatGetDesc(exec)
            ClientScriptOpCode.CHATCAT_GETSUBCAT -> chatCatGetSubCat(exec)
            ClientScriptOpCode.CHATCAT_GETSUBCATCOUNT -> chatCatGetSubCatCount(exec)
            ClientScriptOpCode.QC_GETMESSAGE -> qcGetMessage(exec)
            ClientScriptOpCode.CHATPHRASE_GETTEXT -> chatPhraseGetText(exec)
            ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSECOUNT -> method8865(exec)
            ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSE -> method1362(exec)
            ClientScriptOpCode.QC_SETMESSAGE -> qcSetMessage(exec)
            ClientScriptOpCode.QC_SENDPUBLICMESSAGE -> qcSendPublicMessage(exec)
            ClientScriptOpCode.QC_SENDPRIVATEMESSAGE -> qcSendPrivateMessage(exec)
            ClientScriptOpCode.QC_SENDFRIENDSCHATMESSAGE -> qcSendFriendsChatMessage(exec)
            ClientScriptOpCode.QC_GETSUBHOTKEY -> qcGetSubHotkey(exec)
            ClientScriptOpCode.CHATCAT_GETPHRASESHORTCUT -> method7826(exec)
            ClientScriptOpCode.CHATCAT_FINDSUBCATBYSHORTCUT -> method4148(exec)
            ClientScriptOpCode.CHATCAT_FINDPHRASEBYSHORTCUT -> method1783(exec)
            ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMAND -> method6304(exec)
            ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM -> method6439(exec)
            ClientScriptOpCode.QC_SETVALUE -> method4631(exec)
            ClientScriptOpCode.QC_SETVALUE_ALT -> method1085(exec)
            ClientScriptOpCode.QC_GETPHRASEVALUE -> method2091(exec)
            ClientScriptOpCode.CHATPHRASE_FIND -> chatPhraseFind(exec)
            ClientScriptOpCode.CHATPHRASE_FINDNEXT -> chatPhraseFindnext(exec)
            ClientScriptOpCode.CHATPHRASE_FINDRESET -> chatPhraseFindrestart()
            ClientScriptOpCode.QC_SENDCLANCHATMESSAGE -> qcSendClanChatMessage(exec)
            ClientScriptOpCode.QC_SENDGUESTCLANCHATMESSAGE -> qcSendGuestClanChatMessage(exec)
            ClientScriptOpCode.KEYHELD_CTRL -> keyHeldCtrl(exec)
            ClientScriptOpCode.KEYHELD_SHIFT -> keyHeldShift(exec)
            ClientScriptOpCode.KEYHELD_ALT -> keyHeldAlt(exec)
            ClientScriptOpCode.WORLDMAP_SETZOOM -> zoomWorldmap(exec)
            ClientScriptOpCode.WORLDMAP_GETZOOM -> worldMapGetZoom(exec)
            ClientScriptOpCode.WORLDMAP_SETMAP -> worldMapSetMap(exec)
            ClientScriptOpCode.WORLDMAP_GETMAP -> worldMapGetMap(exec)
            ClientScriptOpCode.WORLDMAP_GETMAPNAME -> worldMapGetMapName(exec)
            ClientScriptOpCode.WORLDMAP_GETCONFIGSIZE -> method8219(exec)
            ClientScriptOpCode.WORLDMAP_GETDISPLAYPOSITION -> worldMapGetDisplayPosition(exec)
            ClientScriptOpCode.WORLDMAP_GETCONFIGORIGIN -> method7276(exec)
            ClientScriptOpCode.WORLDMAP_GETSIZE -> worldMapGetSize(exec)
            ClientScriptOpCode.WORLDMAP_LISTELEMENT_START -> worldMapListElementStart(exec)
            ClientScriptOpCode.WORLDMAP_LISTELEMENT_NEXT -> worldMapListElementNext(exec)
            ClientScriptOpCode.WORLDMAP_JUMPTOSOURCECOORD -> method3556(exec)
            ClientScriptOpCode.WORLDMAP_COORDINMAP -> method2856(exec)
            ClientScriptOpCode.WORLDMAP_GETCONFIGZOOM -> method856(exec)
            ClientScriptOpCode.WORLDMAP_ISLOADED -> method2737(exec)
            ClientScriptOpCode.WORLDMAP_JUMPTODISPLAYCOORD -> worldMapJumpToDisplayCoord(exec)
            ClientScriptOpCode.WORLDMAP_GETSOURCEPOSITION -> method7043(exec)
            ClientScriptOpCode.WORLDMAP_SETMAP_COORD -> worldMapSetMapCoord(exec)
            ClientScriptOpCode.WORLDMAP_GETDISPLAYCOORD -> worldMapGetDisplayCoord(exec)
            ClientScriptOpCode.WORLDMAP_GETSOURCECOORD -> method12572(exec)
            ClientScriptOpCode.WORLDMAP_FLASHELEMENT -> method810(exec)
            ClientScriptOpCode.WORLDMAP_SETMAP_COORD_OVERRIDE -> worldMapSetMapCoordOverride(exec)
            ClientScriptOpCode.WORLDMAP_DISABLEELEMENTS -> method3919(exec)
            ClientScriptOpCode.WORLDMAP_GETDISABLEELEMENTS -> method11339(exec)
            ClientScriptOpCode.WORLDMAP_FLASHELEMENTCATEGORY -> method5829(exec)
            ClientScriptOpCode.WORLDMAP_DISABLEELEMENTCATEGORY -> method12506(exec)
            ClientScriptOpCode.WORLDMAP_GETDISABLEELEMENTCATEGORY -> method2963(exec)
            ClientScriptOpCode.WORLDMAP_DISABLEELEMENT -> method14836(exec)
            ClientScriptOpCode.WORLDMAP_GETDISABLEELEMENT -> method3557(exec)
            ClientScriptOpCode.WORLDMAP_GETCURRENTMAP -> worldMapGetCurrentMap(exec)
            ClientScriptOpCode.WORLDMAP_FINDNEARESTELEMENT -> method12660(exec)
            ClientScriptOpCode.WORLDMAP_CLOSEMAP -> worldmapCloseMap()
            ClientScriptOpCode.FULLSCREEN -> fullscreen(exec)
            ClientScriptOpCode.FULLSCREEN_RESTORE -> method3613()
            ClientScriptOpCode.FULLSCREEN_MODECOUNT -> fullscreenModeCount(exec)
            ClientScriptOpCode.FULLSCREEN_GETMODE -> fullscreenGetMode(exec)
            ClientScriptOpCode.FULLSCREEN_LASTMODE -> fullscreenLastMode(exec)
            ClientScriptOpCode.DETAIL_GET_WINDOWED_MODE -> method8199(exec)
            ClientScriptOpCode.CHOOSE_RENDER_TYPE -> chooseRenderType(exec)
            ClientScriptOpCode.DETAILGET_FIXEDSCREEN -> method3358(exec)
            ClientScriptOpCode.DETAIL_SET_SCREENSIZE -> detailScreenSize(exec)
            ClientScriptOpCode.OPENURL_NOLOGIN -> method14434(exec)
            ClientScriptOpCode.SETRECOLPALETTE -> method1861(exec)
            ClientScriptOpCode.SPLINE_NEW -> splineNew(exec)
            ClientScriptOpCode.SPLINE_ADDPOINT -> splineSetPoint(exec)
            ClientScriptOpCode.SPLINE_LENGTH -> camGetSplineLength(exec)
            ClientScriptOpCode.CLIENT_QUIT -> WindowMode.notifyError()
            ClientScriptOpCode.GETHOSTNAME -> opGetHostName(exec)
            ClientScriptOpCode.OPENURL_FULLSCREENSAFE -> method8263(exec)
            ClientScriptOpCode.CONSOLE_PRINT -> method1604(exec)
            ClientScriptOpCode.SETUP_MENUCHROME -> method8206(exec)
            ClientScriptOpCode.RESET_CHATMEDIA -> method7277()
            ClientScriptOpCode.SETDEFAULTCURSORS -> setDefaultCursors(exec)
            ClientScriptOpCode.POP_2_INT -> pop2Int(exec)
            ClientScriptOpCode.IF_GETTOP -> ifGettop(exec)
            ClientScriptOpCode.DOCHEAT -> method584(exec)
            ClientScriptOpCode.BROWSER_CLOSE -> method2171()
            ClientScriptOpCode.BROWSER_OPEN -> method587()
            ClientScriptOpCode.GETCLIPBOARD -> getClipboard(exec)
            ClientScriptOpCode.MINIMENU_SETMAXENTRIES -> method7209(exec)
            ClientScriptOpCode.OPENURL_SHIM -> method12210(exec)
            ClientScriptOpCode.IS_APPLET -> opIsApplet(exec)
            ClientScriptOpCode.CHECK_JAVA_VERSION -> checkJavaVersion(exec)
            ClientScriptOpCode.IS_GAMESCREEN_STATE -> isGameScreenState(exec)
            ClientScriptOpCode.GETPREFERENCEFILE -> method15513(exec)
            ClientScriptOpCode.OPENDIRECTORYPICKER -> method8718(exec)
            ClientScriptOpCode.GETDIRECTORYPICKERRESULT -> method7021(exec)
            ClientScriptOpCode.VALIDATECACHEDIRECTORY -> method13359(exec)
            ClientScriptOpCode.CAM_MOVETO -> camMoveTo(exec)
            ClientScriptOpCode.CAM_LOOKAT -> camLookat(exec)
            ClientScriptOpCode.CAM_MOVEALONG -> method1366(exec)
            ClientScriptOpCode.CAM_SMOOTHRESET -> camSmoothReset()
            ClientScriptOpCode.CAM_FORCEANGLE -> method16079(exec)
            ClientScriptOpCode.CAM_GETANGLE_XA -> camGetAngleXa(exec)
            ClientScriptOpCode.CAM_GETANGLE_YA -> camGetAngleYa(exec)
            ClientScriptOpCode.CAM_INC_X -> camIncX()
            ClientScriptOpCode.CAM_DEC_X -> camDecX()
            ClientScriptOpCode.CAM_INC_Y -> camIncY()
            ClientScriptOpCode.CAM_DEC_Y -> camDecY()
            ClientScriptOpCode.CAM_SETFOLLOWHEIGHT -> camSetFollowHeight(exec)
            ClientScriptOpCode.CAM_GETFOLLOWHEIGHT -> camGetFollowHeight(exec)
            ClientScriptOpCode.COORD_CLAMPTOSCENE -> method11439(exec)
            ClientScriptOpCode.CAM_RESET -> camReset()
            ClientScriptOpCode.COORD_CLAMPTOSCENE_ORDEFAULT -> method3038(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by QuickChatDisplayOps")
        }
    }

    @JvmStatic
    fun camDecY() {
        StaticMethods.applyCameraYawLeft()
    }

    @JvmStatic
    fun method15513(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = GameShell.getPreferenceFile().toString()
    }

    @JvmStatic
    fun method1366(executor: CS2Executor) {
        executor.intStackPtr -= 6
        val i_2 = executor.intStack[executor.intStackPtr]
        if (i_2 >= 2) {
            throw RuntimeException()
        } else {
            Client.cameraPositionPathIndex = i_2
            val i_3 = executor.intStack[executor.intStackPtr + 1]
            if (i_3 + 1 >= (Client.cameraSplinePaths[Client.cameraPositionPathIndex]!!.size shr 1)) {
                throw RuntimeException()
            } else {
                Client.cameraPositionKeyframe = i_3
                Client.cameraSplineProgress = 0
                Client.cameraSplineSpeedStart = executor.intStack[executor.intStackPtr + 2]
                Client.cameraSplineSpeedEnd = executor.intStack[executor.intStackPtr + 3]
                val i_4 = executor.intStack[executor.intStackPtr + 4]
                if (i_4 >= 2) {
                    throw RuntimeException()
                } else {
                    Client.cameraLookAtPathIndex = i_4
                    val i_5 = executor.intStack[executor.intStackPtr + 5]
                    if (i_5 + 1 >= (Client.cameraSplinePaths[Client.cameraLookAtPathIndex]!!.size shr 1)) {
                        throw RuntimeException()
                    } else {
                        Client.cameraLookAtKeyframe = i_5
                        Camera.cameraCalculationType = 3
                        StaticMethods.cameraTargetAbsY = -1
                        PingRequester.cameraTargetAbsX = -1
                    }
                }
            }
        }
    }

    @JvmStatic
    fun camSetFollowHeight(executor: CS2Executor) {
        var value = executor.intStack[--executor.intStackPtr]
        if (value < 0) {
            value = 0
        }
        Client.cameraMaxZoom = value + Client.cameraBaseZoom
    }

    @JvmStatic
    fun camGetAngleYa(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Camera.camAngleY.toInt() shr 3
    }

    @JvmStatic
    fun method12572(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub63: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub63 != null) {
            val flag = class282Sub50Sub63.mapToGameCoords((value shr 14) and 0x3fff, value and 0x3fff, CS2Executor.coordConversionBuffer)
            if (flag) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.coordConversionBuffer!![1]
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.coordConversionBuffer!![2]
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
    fun method8718(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.filePicker = DirectoryPicker(text!!)
    }

    @JvmStatic
    fun chatCatGetSubCat(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val class282Sub50Sub144: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2)
        val i_5 = class282Sub50Sub144!!.subCategories!![i_3]
        executor.intStack[++executor.intStackPtr - 1] = i_5
    }

    @JvmStatic
    fun method3556(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub63: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub63 != null) {
            val flag = class282Sub50Sub63.gameToMapCoordsForPlane((value shr 28) and 0x3, (value shr 14) and 0x3fff, value and 0x3fff, CS2Executor.coordConversionBuffer)
            if (flag) {
                StaticMethods.setWorldMapScrollTarget(CS2Executor.coordConversionBuffer!![1], CS2Executor.coordConversionBuffer!![2])
            }
        }
    }

    @JvmStatic
    fun method3557(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val node3: Node<*>? = WorldMapSub1.hiddenMecIds.get(value.toLong())
        executor.intStack[++executor.intStackPtr - 1] = if (node3 != null) 1 else 0
    }

    @JvmStatic
    fun qcSendClanChatMessage(executor: CS2Executor) {
        val class1842: BufferedConnectionContext? = Client.connectionContext
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        val value = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(2)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - value)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method8263(executor: CS2Executor) {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
        }
        val string_2: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val flag = executor.intStack[--executor.intStackPtr] == 1
        val string_4: String? = ServerEnvironment.getUrl() + string_2
        Browser.openOpenjs(string_4!!, flag, Client.useBrowserAppletMode)
    }

    @JvmStatic
    fun method5829(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        WorldMapSub1.highlightCategory(value)
    }

    @JvmStatic
    fun method12660(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = (i_3 shr 14) and 0x3fff
        val i_5 = i_3 and 0x3fff
        val i_6 = WorldMapCategory.findNearestElement(i_2, i_4, i_5)
        if (i_6 < 0) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = i_6
        }
    }

    @JvmStatic
    fun chatCatGetDesc(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub143: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(value)
        if (class282Sub50Sub143!!.subCategories == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub143!!.subCategories!!.size
        }
    }

    @JvmStatic
    fun method8199(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = WindowMode.getWindowMode()
    }

    @JvmStatic
    fun method7021(executor: CS2Executor) {
        var flag = false
        var path: String? = ""
        val filePicker = executor.filePicker
        if (filePicker != null && filePicker.fileChoserOpen()) {
            val directory: File? = filePicker.getPickedDirectory()
            if (directory != null) {
                path = directory.getPath()
            }
            flag = true
            executor.filePicker = null
        }
        executor.intStack[++executor.intStackPtr - 1] = if (flag) 1  else 0
        executor.stringStack[++executor.stringStackPtr - 1] = path
    }

    @JvmStatic
    fun fullscreenModeCount(executor: CS2Executor) {
        if (WindowMode.fullScreenSupported) {
            val arr_2 = WindowMode.getActiveDisplayModeDetails()
            executor.intStack[++executor.intStackPtr - 1] = arr_2!!.size
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method3358(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.fixedScreen!!.getValue()
    }

    @JvmStatic
    fun method12506(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val value = executor.intStack[executor.intStackPtr]
        val flag = executor.intStack[executor.intStackPtr + 1] == 1
        var node4: Node<Any?>? = WorldMapSub1.hiddenCategoryIds.get(value.toLong())
        if (node4 != null && !flag) {
            node4.unlink()
        } else if (node4 == null && flag) {
            node4 = object : Node<Any?>() {}
            WorldMapSub1.hiddenCategoryIds.put(node4!!, value.toLong())
        }
    }

    @JvmStatic
    fun worldMapGetCurrentMap(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (WorldMap.staticElement != null) WorldMap.staticElement!!.elementId else -1
    }

    @JvmStatic
    fun chooseRenderType(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (value >= 1 && value <= 2) {
            WindowMode.requestWindowMode(value, -1, -1, false)
        }
    }

    @JvmStatic
    fun method3919(executor: CS2Executor) {
        WorldMapSub1.hideIcons = executor.intStack[--executor.intStackPtr] == 1
    }

    @JvmStatic
    fun qcSendFriendsChatMessage(executor: CS2Executor) {
        val class1842: BufferedConnectionContext? = Client.connectionContext
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        val value = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(1)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - value)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun qcGetSubHotkey(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).subCategoryHotkeys!![i_3].code
    }

    @JvmStatic
    fun chatPhraseFind(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val flag = executor.intStack[--executor.intStackPtr] == 1
        QuickchatMessageType.searchQuickchatMessages(text!!, flag)
        executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.count
    }

    @JvmStatic
    fun camIncY() {
        Client.applyCameraYawRight()
    }

    @JvmStatic
    fun method2737(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (ComponentMinimap.mapAssetLoadingPercentage == 100) 1 else 0
    }

    @JvmStatic
    fun method1362(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2).responses!![i_3]
    }

    @JvmStatic
    fun chatPhraseGetText(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(value).getPlaceholderText()
    }

    @JvmStatic
    fun qcSendPublicMessage(executor: CS2Executor) {
        val class1842: BufferedConnectionContext? = Client.connectionContext
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        val value = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(0)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - value)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun isGameScreenState(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (GameState.isRenderingScene(Client.gameState)) 1  else 0
    }

    @JvmStatic
    fun worldMapListElementNext(executor: CS2Executor) {
        val class282Sub362: WorldMapCategory? = WorldMap.getNextActiveCategory()
        if (class282Sub362 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub362.mecId
            val value = (class282Sub362!!.plane shl 28) or class282Sub362.absX + (WorldMap.mapSquareAbsX shl 14) or class282Sub362.absY + WorldMap.mapSquareAbsY
            executor.intStack[++executor.intStackPtr - 1] = value
        }
    }

    @JvmStatic
    fun setDefaultCursors(executor: CS2Executor) {
        executor.intStackPtr -= 2
        CursorManager.defaultCursorId = executor.intStack[executor.intStackPtr]
        CursorManager.defaultAttackCursor = executor.intStack[executor.intStackPtr + 1]
    }

    @JvmStatic
    fun camGetSplineLength(executor: CS2Executor) {
        val value = (Client.cameraSplinePaths!![executor.intStack[--executor.intStackPtr]]!!.size shr 1)
        executor.intStack[++executor.intStackPtr - 1] = value
    }

    @JvmStatic
    fun camMoveTo(executor: CS2Executor) {
        executor.intStackPtr -= 4
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val i_5 = executor.intStack[executor.intStackPtr + 3]
        val coordgrid6: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        Camera.moveCamera(((i_2 shr 14) and 0x3fff) - coordgrid6!!.x, (i_2 and 0x3fff) - coordgrid6!!.y, (i_3 shl 2), i_4, i_5, false)
    }

    @JvmStatic
    fun method1783(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_3 == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).messageForHotkey(i_3.toChar())
        }
    }

    @JvmStatic
    fun opGetHostName(executor: CS2Executor) {
        var text: String? = null
        text = PacketDecoder.HOSTNAME_IDENTIFIER!!.getHostName()
        if (text == null) {
            text = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = text
    }

    @JvmStatic
    fun camIncX() {
        StaticMethods.applyCameraPitchUp()
    }

    @JvmStatic
    fun worldMapSetMapCoordOverride(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        WorldMapSub1.reset(i_2, (i_3 shr 14) and 0x3fff, i_3 and 0x3fff, true)
    }

    @JvmStatic
    fun fullscreen(executor: CS2Executor) {
        executor.intStackPtr -= 2
        if (WindowMode.fullScreenSupported) {
            executor.intStack[++executor.intStackPtr - 1] = if (WindowMode.fullScreenFrame != null) 1 else 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method8219(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.worldMapViewWidth
        executor.intStack[++executor.intStackPtr - 1] = ComponentMinimap.mapViewportScale
    }

    @JvmStatic
    fun method3613() {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
        }
    }

    @JvmStatic
    fun fullscreenGetMode(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (WindowMode.fullScreenSupported) {
            val arr_3 = WindowMode.getActiveDisplayModeDetails()
            executor.intStack[++executor.intStackPtr - 1] = arr_3!![value].width
            executor.intStack[++executor.intStackPtr - 1] = arr_3!![value].height
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method11439(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        var i_3 = (i_2 shr 14) and 0x3fff
        var i_4 = i_2 and 0x3fff
        val coordgrid5: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
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
        Client.cameraTargetAbsX = ((i_3 shl 9)) + 256
        Client.cameraTargetAbsY = ((i_4 shl 9)) + 256
        Camera.cameraCalculationType = 4
        StaticMethods.cameraTargetAbsY = -1
        PingRequester.cameraTargetAbsX = -1
    }

    @JvmStatic
    fun worldMapGetDisplayPosition(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = WorldMapSub1.mapCenterX + WorldMap.mapSquareAbsX
        executor.intStack[++executor.intStackPtr - 1] = WorldMapSub1.mapCenterY + WorldMap.mapSquareAbsY
    }

    @JvmStatic
    fun method16079(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Camera.setAngle(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], 0)
    }

    @JvmStatic
    fun camDecX() {
        StaticMethods.applyCameraPitchDown()
    }

    @JvmStatic
    fun method2963(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val node3: Node<Any?>? = WorldMapSub1.hiddenCategoryIds.get(value.toLong())
        executor.intStack[++executor.intStackPtr - 1] = if (node3 != null) 1 else 0
    }

    @JvmStatic
    fun keyHeldShift(executor: CS2Executor) {
        if (KeyRecorder.keyRecorder!!.held(82)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method2856(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val position = executor.intStack[executor.intStackPtr]
        val value = executor.intStack[executor.intStackPtr + 1]
        val class4774: Queue<WorldMapElement>? = WorldMap.getRenderedElements((position shr 14) and 0x3fff, position and 0x3fff)
        var flag = false
        var class282Sub50Sub66: WorldMapElement? = class4774?.getNext() as WorldMapElement?
        while (class282Sub50Sub66 != null) {
            if (value == class282Sub50Sub66.elementId) {
                flag = true
                break
            }
            class282Sub50Sub66 = class4774?.getDirectNext() as WorldMapElement?
        }
        if (flag) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun worldMapJumpToDisplayCoord(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        StaticMethods.setWorldMapScrollTarget((value shr 14) and 0x3fff, value and 0x3fff)
    }

    @JvmStatic
    fun qcGetMessage(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).messages!![i_3]
    }

    @JvmStatic
    fun method7276(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub63: WorldMapElement? = WorldMap.poll(value)
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val quickchatdefinitions5: QuickchatMessageType? = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2)
        if (quickchatdefinitions5!!.getValueType(i_3)!!.id != 0) {
            throw RuntimeException("")
        } else {
            executor.intStack[++executor.intStackPtr - 1] = quickchatdefinitions5!!.getValueConfig(i_3, i_4)
        }
    }

    @JvmStatic
    fun camReset() {
        Camera.startSmoothTransition()
    }

    @JvmStatic
    fun pop2Int(executor: CS2Executor) {
        executor.intStackPtr -= 2
    }

    @JvmStatic
    fun method1085(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.currentQuickChatMessage!!.dynamicValues!![i_2] = i_3
    }

    @JvmStatic
    fun method2171() {
    }

    @JvmStatic
    fun method6439(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(i_2).getValueType(i_3)!!.id
        executor.intStack[++executor.intStackPtr - 1] = i_4
    }

    @JvmStatic
    fun getClipboard(executor: CS2Executor) {
        var str2: String? = ""
        if (Console.clipboard != null) {
            val transferable3: Transferable? = Console.clipboard!!.getContents(null)
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
    fun method584(executor: CS2Executor) {
        CommandExecutor.executeCommand(executor.stringStack[--executor.stringStackPtr] as String, false, false)
    }

    @JvmStatic
    fun method587() {
    }

    @JvmStatic
    fun method6304(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(value).getValueTypeCount()
    }

    @JvmStatic
    fun chatPhraseFindnext(executor: CS2Executor) {
        if (CS2QueryResults.hasNext()) {
            executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.next()
        } else {
            executor.intStack[++executor.intStackPtr - 1] = -1
        }
    }

    @JvmStatic
    fun camGetAngleXa(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Camera.camAngleX.toInt() shr 3
    }

    @JvmStatic
    fun worldMapGetMapName(executor: CS2Executor) {
        val class282Sub50Sub62: WorldMapElement? = WorldMap.poll(executor.intStack[--executor.intStackPtr])
        if (class282Sub50Sub62 != null) {
            if (class282Sub50Sub62.displayName != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = class282Sub50Sub62.displayName
                return
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = ""
    }

    @JvmStatic
    fun ifGettop(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val stack: IntArray? = executor.intStack
        val i_5 = ++executor.intStackPtr - 1
        val b_6 = if (Interface.hasActiveMenuEntry(i_2, i_3)) 1 else 0
        stack!![i_5] = b_6
    }

    @JvmStatic
    fun method3038(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        if (i_2 == -1) {
            var i_3 = 16383
            var i_4 = i_2 and 0x3fff
            val coordgrid5: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
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
            PingRequester.cameraTargetAbsX = ((i_3 shl 9)) + 256
            StaticMethods.cameraTargetAbsY = ((i_4 shl 9)) + 256
        } else {
            PingRequester.cameraTargetAbsX = -1
            StaticMethods.cameraTargetAbsY = -1
        }
    }

    @JvmStatic
    fun keyHeldCtrl(executor: CS2Executor) {
        if (KeyRecorder.keyRecorder!!.held(86)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun splineSetPoint(executor: CS2Executor) {
        executor.intStackPtr -= 7
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1] shl 1
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val i_5 = executor.intStack[executor.intStackPtr + 3]
        val i_6 = executor.intStack[executor.intStackPtr + 4]
        val i_7 = executor.intStack[executor.intStackPtr + 5]
        val i_8 = executor.intStack[executor.intStackPtr + 6]
        if (i_2 >= 0 && i_2 < 2 && Client.cameraSplinePaths[i_2] != null && i_3 >= 0 && i_3 < Client.cameraSplinePaths[i_2]!!.size) {
            Client.cameraSplinePaths[i_2]!![i_3] = intArrayOf(((i_4 shr 14) and 0x3fff) shl 9, i_5 shl 2, ((i_4 and 0x3fff) shl 9), i_8)
            Client.cameraSplinePaths[i_2]!![i_3 + 1] = intArrayOf(((i_6 shr 14) and 0x3fff) shl 9, i_7 shl 2, (i_6 and 0x3fff) shl 9)
        }
    }

    @JvmStatic
    fun method810(executor: CS2Executor) {
        StaticMethods.selectWorldMapElement(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun chatCatGetSubCatCount(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub143: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(value)
        if (class282Sub50Sub143!!.messages == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub143!!.messages!!.size
        }
    }

    @JvmStatic
    fun qcGetName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(value).name
    }

    @JvmStatic
    fun method7209(executor: CS2Executor) {
        MiniMenu.maxVisibleEntries = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun method4631(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.currentQuickChatMessage!!.dynamicValues!![i_2] = i_3
    }

    @JvmStatic
    fun qcSendGuestClanChatMessage(executor: CS2Executor) {
        val class1842: BufferedConnectionContext? = Client.connectionContext
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, class1842!!.isaac)
        tcpmessage3!!.buffer.writeByte(0)
        val value = tcpmessage3!!.buffer.currentPosition
        tcpmessage3!!.buffer.writeByte(3)
        tcpmessage3!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(tcpmessage3!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        tcpmessage3!!.buffer.writeIndex(tcpmessage3!!.buffer.currentPosition - value)
        class1842!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method1604(executor: CS2Executor) {
        System.out.println(executor.stringStack[--executor.stringStackPtr])
    }

    @JvmStatic
    fun worldMapListElementStart(executor: CS2Executor) {
        val class282Sub362: WorldMapCategory? = WorldMapSub1.getFirstActiveCategory()
        if (class282Sub362 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub362.mecId
            val value = (class282Sub362!!.plane shl 28) or class282Sub362.absX + (WorldMap.mapSquareAbsX shl 14) or class282Sub362.absY + WorldMap.mapSquareAbsY
            executor.intStack[++executor.intStackPtr - 1] = value
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
    fun method856(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub63: WorldMapElement? = WorldMap.poll(value)
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.defaultZoomLevel
        }
    }

    @JvmStatic
    fun worldMapGetDisplayCoord(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub63: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub63 != null) {
            val flag = class282Sub50Sub63.gameToMapCoordsForPlane((value shr 28) and 0x3, (value shr 14) and 0x3fff, value and 0x3fff, CS2Executor.coordConversionBuffer)
            if (flag) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.coordConversionBuffer!![1]
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.coordConversionBuffer!![2]
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
    fun worldMapGetMap(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub63: WorldMapElement? = WorldMap.getMapDef((value shr 14) and 0x3fff, value and 0x3fff)
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.elementId
        }
    }

    @JvmStatic
    fun worldmapCloseMap() {
        Client.setLoadingWorldMap()
    }

    @JvmStatic
    fun method13359(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val flag = ClientStartup.activeClient!!.validateCacheDirectory(File(text))
        executor.intStack[++executor.intStackPtr - 1] = if (flag) 1  else 0
    }

    @JvmStatic
    fun camGetFollowHeight(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.cameraMaxZoom - Client.cameraBaseZoom
    }

    @JvmStatic
    fun method8206(executor: CS2Executor) {
        executor.intStackPtr -= 12
        MiniMenu.close()
        StaticMedia.resetChatMedia()
        TextureOpIrregularBricks.backgroundColor = executor.intStack[executor.intStackPtr]
        NativeSprite.backgroundTransparency = executor.intStack[executor.intStackPtr + 1]
        MiniMenu.menuHoverHighlightColor = executor.intStack[executor.intStackPtr + 2]
        StaticMethods.menuBackgroundAlpha = executor.intStack[executor.intStackPtr + 3]
        StaticMethods.loadingBarBackgroundSpriteId = executor.intStack[executor.intStackPtr + 4]
        NativeSprite.loadingBarTileSpriteId = executor.intStack[executor.intStackPtr + 5]
        StaticMethods.menuTopBorderSpriteId = executor.intStack[executor.intStackPtr + 6]
        StaticMethods.menuSideBorderSpriteId = executor.intStack[executor.intStackPtr + 7]
        StaticMethods.menuCornerSpriteId = executor.intStack[executor.intStackPtr + 8]
        MiniMenu.menuTextColor = executor.intStack[executor.intStackPtr + 9]
        StaticMethods.menuHoverTextColor = executor.intStack[executor.intStackPtr + 10]
        StaticMethods.menuFontIndex = executor.intStack[executor.intStackPtr + 11]
        Resource.SPRITES!!.isFileCached(StaticMethods.loadingBarBackgroundSpriteId)
        Resource.SPRITES!!.isFileCached(NativeSprite.loadingBarTileSpriteId)
        Resource.SPRITES!!.isFileCached(StaticMethods.menuTopBorderSpriteId)
        Resource.SPRITES!!.isFileCached(StaticMethods.menuSideBorderSpriteId)
        Resource.SPRITES!!.isFileCached(StaticMethods.menuCornerSpriteId)
        Resource.SPRITES!!.isFileCached(StaticMethods.menuFontIndex)
        Resource.FONT_METRICS!!.isFileCached(StaticMethods.menuFontIndex)
        StaticMethods.menuTopBorderSprite = null
        StaticMethods.menuBottomRightCornerSprite = null
        StaticMethods.menuBottomLeftCornerSprite = null
        StaticMethods.loadingBarTileSprite = null
        StaticMedia.loadingBarEdgeSprite = null
        StaticMedia.loadingBarBackgroundSprite = null
        StaticMedia.menuRightSideBorderSprite = null
        StaticMedia.menuLeftSideBorderSprite = null
        FontMetrics.useBoldFont = true
    }

    @JvmStatic
    fun method8865(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val quickchatdefinitions3: QuickchatMessageType? = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(value)
        if (quickchatdefinitions3!!.responses == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = quickchatdefinitions3!!.responses!!.size
        }
    }

    @JvmStatic
    fun method7826(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).messageHotkeys!![i_3].code
    }

    @JvmStatic
    fun method14434(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        PacketEncoder.sendLobbyHyperlink(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, "", executor.intStack[--executor.intStackPtr] == 1, false)
    }

    @JvmStatic
    fun opIsApplet(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.isApplet) 1  else 0
    }

    @JvmStatic
    fun qcSendPrivateMessage(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val class1843: BufferedConnectionContext? = Client.connectionContext
        val tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PRIVATE, class1843!!.isaac)
        tcpmessage4!!.buffer.writeByte(0)
        val value = tcpmessage4!!.buffer.currentPosition
        tcpmessage4!!.buffer.writeString(text!!)
        tcpmessage4!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(tcpmessage4!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        tcpmessage4!!.buffer.writeIndex(tcpmessage4!!.buffer.currentPosition - value)
        class1843!!.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun keyHeldAlt(executor: CS2Executor) {
        if (KeyRecorder.keyRecorder!!.held(81)) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun worldMapGetZoom(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.getWorldMapZoomLevel()
    }

    @JvmStatic
    fun method14836(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val value = executor.intStack[executor.intStackPtr]
        val flag = executor.intStack[executor.intStackPtr + 1] == 1
        var node4: Node<*>? = WorldMapSub1.hiddenMecIds.get(value.toLong())
        if (node4 != null && !flag) {
            node4.unlink()
        } else if (node4 == null && flag) {
            node4 = object : Node<Any?>() {}
            WorldMapSub1.hiddenMecIds.put(node4, value.toLong())
        }
    }

    @JvmStatic
    fun worldMapSetMapCoord(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        WorldMapSub1.reset(i_2, (i_3 shr 14) and 0x3fff, i_3 and 0x3fff, false)
    }

    @JvmStatic
    fun method12210(executor: CS2Executor) {
        executor.stringStackPtr -= 3
        PacketEncoder.sendLobbyHyperlink(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, executor.stringStack[executor.stringStackPtr + 2] as String?, executor.intStack[--executor.intStackPtr] == 1, true)
    }

    @JvmStatic
    fun chatPhraseFindrestart() {
        CS2QueryResults.cursor = 0
    }

    @JvmStatic
    fun camLookat(executor: CS2Executor) {
        executor.intStackPtr -= 4
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val i_5 = executor.intStack[executor.intStackPtr + 3]
        val coordgrid6: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        Camera.lookat(((i_2 shr 14) and 0x3fff) - coordgrid6!!.x, (i_2 and 0x3fff) - coordgrid6!!.y, (i_3 shl 2), i_4, i_5)
    }

    @JvmStatic
    fun qcSetMessage(executor: CS2Executor) {
        executor.currentQuickChatMessage = QuickChatMessage()
        executor.currentQuickChatMessage!!.id = executor.intStack[--executor.intStackPtr]
        executor.currentQuickChatMessage!!.message = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.dynamicValues = IntArray(executor.currentQuickChatMessage!!.message!!.getValueTypeCount())
    }

    @JvmStatic
    fun fullscreenLastMode(executor: CS2Executor) {
        val i_2 = WindowMode.fullScreenWidth
        val i_3 = WindowMode.fullScreenHeight
        var i_4 = -1
        if (WindowMode.fullScreenSupported) {
            val arr_5: Array<DisplayModeDetails>? = WindowMode.getActiveDisplayModeDetails()
            for (i_6 in 0 until arr_5!!.size) {
                val class4677: DisplayModeDetails? = arr_5[i_6]
                if (i_2 == class4677!!.width && i_3 == class4677!!.height) {
                    i_4 = i_6
                    break
                }
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = i_4
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
    fun camSmoothReset() {
        Camera.smoothReset()
    }

    @JvmStatic
    fun splineNew(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 >= 0 && i_2 < 2) {
            Client.cameraSplinePaths!![i_2] = Array(4) { IntArray(i_3 shl 1) }
        }
    }

    @JvmStatic
    fun worldMapGetSize(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class282Sub50Sub63: WorldMapElement? = WorldMap.poll(value)
        if (class282Sub50Sub63 == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.boundsMaxX - class282Sub50Sub63.boundsMinX
            executor.intStack[++executor.intStackPtr - 1] = class282Sub50Sub63.boundsMaxY - class282Sub50Sub63.boundsMinY
        }
    }

    @JvmStatic
    fun method7043(executor: CS2Executor) {
        val class282Sub50Sub62: WorldMapElement? = WorldMapSub1.getStaticElement()
        if (class282Sub50Sub62 != null) {
            val flag = class282Sub50Sub62.mapToGameCoords(WorldMap.mapSquareAbsX + WorldMapSub1.mapCenterX, WorldMap.mapSquareAbsY + WorldMapSub1.mapCenterY, CS2Executor.coordConversionBuffer)
            if (flag) {
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.coordConversionBuffer!![1]
                executor.intStack[++executor.intStackPtr - 1] = CS2Executor.coordConversionBuffer!![2]
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
    fun zoomWorldmap(executor: CS2Executor) {
        WorldMapInteraction.requestZoomChange(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun method11339(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (WorldMapSub1.hideIcons) 1  else 0
    }

    @JvmStatic
    fun detailScreenSize(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (value >= 1 && value <= 2) {
            PreferenceManager.preferences!!.fixedScreen!!.setValue(value)
            PreferenceManager.preferences!!.fullScreen!!.setValue(value)
            PreferenceManager.savePreferences()
        }
    }

    @JvmStatic
    fun method4148(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_3 == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(i_2).subCategoryForHotkey(i_3.toChar())
        }
    }

    @JvmStatic
    fun worldMapSetMap(executor: CS2Executor) {
        WorldMapSub1.reset(executor.intStack[--executor.intStackPtr], -1, -1, false)
    }

}
