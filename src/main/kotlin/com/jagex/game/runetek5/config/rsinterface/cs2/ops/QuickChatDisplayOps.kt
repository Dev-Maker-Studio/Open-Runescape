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
            ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSECOUNT -> chatPhraseGetAutoResponseCount(exec)
            ClientScriptOpCode.CHATPHRASE_GETAUTORESPONSE -> chatPhraseGetAutoResponse(exec)
            ClientScriptOpCode.QC_SETMESSAGE -> qcSetMessage(exec)
            ClientScriptOpCode.QC_SENDPUBLICMESSAGE -> qcSendPublicMessage(exec)
            ClientScriptOpCode.QC_SENDPRIVATEMESSAGE -> qcSendPrivateMessage(exec)
            ClientScriptOpCode.QC_SENDFRIENDSCHATMESSAGE -> qcSendFriendsChatMessage(exec)
            ClientScriptOpCode.QC_GETSUBHOTKEY -> qcGetSubHotkey(exec)
            ClientScriptOpCode.CHATCAT_GETPHRASESHORTCUT -> chatCatGetPhraseShortcut(exec)
            ClientScriptOpCode.CHATCAT_FINDSUBCATBYSHORTCUT -> chatCatFindSubCatByShortcut(exec)
            ClientScriptOpCode.CHATCAT_FINDPHRASEBYSHORTCUT -> chatCatFindPhraseByShortcut(exec)
            ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMAND -> chatPhraseGetDynamicCommand(exec)
            ClientScriptOpCode.CHATPHRASE_GETDYNAMICCOMMANDPARAM_ENUM -> chatPhraseGetDynamicCommandParamEnum(exec)
            ClientScriptOpCode.QC_SETVALUE -> qcSetValue(exec)
            ClientScriptOpCode.QC_SETVALUE_ALT -> qcSetValueAlt(exec)
            ClientScriptOpCode.QC_GETPHRASEVALUE -> qcGetPhraseValue(exec)
            ClientScriptOpCode.CHATPHRASE_FIND -> chatPhraseFind(exec)
            ClientScriptOpCode.CHATPHRASE_FINDNEXT -> chatPhraseFindnext(exec)
            ClientScriptOpCode.instr6601 -> chatPhraseFindrestart()
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
            ClientScriptOpCode.WORLDMAP_GETCONFIGSIZE -> worldMapGetConfigSize(exec)
            ClientScriptOpCode.WORLDMAP_GETDISPLAYPOSITION -> worldMapGetDisplayPosition(exec)
            ClientScriptOpCode.WORLDMAP_GETCONFIGORIGIN -> worldMapGetConfigOrigin(exec)
            ClientScriptOpCode.WORLDMAP_GETSIZE -> worldMapGetSize(exec)
            ClientScriptOpCode.WORLDMAP_LISTELEMENT_START -> worldMapListElementStart(exec)
            ClientScriptOpCode.WORLDMAP_LISTELEMENT_NEXT -> worldMapListElementNext(exec)
            ClientScriptOpCode.WORLDMAP_JUMPTOSOURCECOORD -> worldMapJumpToSourceCoord(exec)
            ClientScriptOpCode.WORLDMAP_COORDINMAP -> worldMapCoordInMap(exec)
            ClientScriptOpCode.WORLDMAP_GETCONFIGZOOM -> worldMapGetConfigZoom(exec)
            ClientScriptOpCode.WORLDMAP_ISLOADED -> worldMapIsLoaded(exec)
            ClientScriptOpCode.WORLDMAP_JUMPTODISPLAYCOORD -> worldMapJumpToDisplayCoord(exec)
            ClientScriptOpCode.WORLDMAP_GETSOURCEPOSITION -> worldMapGetSourcePosition(exec)
            ClientScriptOpCode.WORLDMAP_SETMAP_COORD -> worldMapSetMapCoord(exec)
            ClientScriptOpCode.WORLDMAP_GETDISPLAYCOORD -> worldMapGetDisplayCoord(exec)
            ClientScriptOpCode.WORLDMAP_GETSOURCECOORD -> worldMapGetSourceCoord(exec)
            ClientScriptOpCode.WORLDMAP_FLASHELEMENT -> worldMapFlashElement(exec)
            ClientScriptOpCode.WORLDMAP_SETMAP_COORD_OVERRIDE -> worldMapSetMapCoordOverride(exec)
            ClientScriptOpCode.WORLDMAP_DISABLEELEMENTS -> worldMapDisableElements(exec)
            ClientScriptOpCode.WORLDMAP_GETDISABLEELEMENTS -> worldMapGetDisableElements(exec)
            ClientScriptOpCode.WORLDMAP_FLASHELEMENTCATEGORY -> worldMapFlashElementCategory(exec)
            ClientScriptOpCode.WORLDMAP_DISABLEELEMENTCATEGORY -> worldMapDisableElementCategory(exec)
            ClientScriptOpCode.WORLDMAP_GETDISABLEELEMENTCATEGORY -> worldMapGetDisableElementCategory(exec)
            ClientScriptOpCode.WORLDMAP_DISABLEELEMENT -> worldMapDisableElement(exec)
            ClientScriptOpCode.WORLDMAP_GETDISABLEELEMENT -> worldMapGetDisableElement(exec)
            ClientScriptOpCode.WORLDMAP_GETCURRENTMAP -> worldMapGetCurrentMap(exec)
            ClientScriptOpCode.WORLDMAP_FINDNEARESTELEMENT -> worldMapFindNearestElement(exec)
            ClientScriptOpCode.WORLDMAP_CLOSEMAP -> worldmapCloseMap()
            ClientScriptOpCode.FULLSCREEN -> fullscreen(exec)
            ClientScriptOpCode.FULLSCREEN_RESTORE -> fullscreenRestore()
            ClientScriptOpCode.FULLSCREEN_MODECOUNT -> fullscreenModeCount(exec)
            ClientScriptOpCode.FULLSCREEN_GETMODE -> fullscreenGetMode(exec)
            ClientScriptOpCode.FULLSCREEN_LASTMODE -> fullscreenLastMode(exec)
            ClientScriptOpCode.DETAIL_GET_WINDOWED_MODE -> detailGetWindowedMode(exec)
            ClientScriptOpCode.CHOOSE_RENDER_TYPE -> chooseRenderType(exec)
            ClientScriptOpCode.DETAILGET_FIXEDSCREEN -> detailGetFixedScreen(exec)
            ClientScriptOpCode.DETAIL_SET_SCREENSIZE -> detailScreenSize(exec)
            ClientScriptOpCode.OPENURL_NOLOGIN -> openUrlNoLogin(exec)
            ClientScriptOpCode.SETRECOLPALETTE -> setRecolPalette(exec)
            ClientScriptOpCode.SPLINE_NEW -> splineNew(exec)
            ClientScriptOpCode.SPLINE_ADDPOINT -> splineSetPoint(exec)
            ClientScriptOpCode.SPLINE_LENGTH -> camGetSplineLength(exec)
            ClientScriptOpCode.CLIENT_QUIT -> WindowMode.notifyError()
            ClientScriptOpCode.GETHOSTNAME -> opGetHostName(exec)
            ClientScriptOpCode.OPENURL_FULLSCREENSAFE -> openUrlFullscreenSafe(exec)
            ClientScriptOpCode.CONSOLE_PRINT -> consolePrint(exec)
            ClientScriptOpCode.SETUP_MENUCHROME -> setupMenuChrome(exec)
            ClientScriptOpCode.RESET_CHATMEDIA -> resetChatMediaAndFont()
            ClientScriptOpCode.SETDEFAULTCURSORS -> setDefaultCursors(exec)
            ClientScriptOpCode.POP_2_INT -> pop2Int(exec)
            ClientScriptOpCode.IF_GETTOP -> ifGettop(exec)
            ClientScriptOpCode.DOCHEAT -> doCheat(exec)
            ClientScriptOpCode.BROWSER_CLOSE -> browserClose()
            ClientScriptOpCode.BROWSER_OPEN -> browserOpen()
            ClientScriptOpCode.GETCLIPBOARD -> getClipboard(exec)
            ClientScriptOpCode.MINIMENU_SETMAXENTRIES -> miniMenuSetMaxEntries(exec)
            ClientScriptOpCode.OPENURL_SHIM -> openUrlShim(exec)
            ClientScriptOpCode.IS_APPLET -> opIsApplet(exec)
            ClientScriptOpCode.CHECK_JAVA_VERSION -> checkJavaVersion(exec)
            ClientScriptOpCode.IS_GAMESCREEN_STATE -> isGameScreenState(exec)
            ClientScriptOpCode.GETPREFERENCEFILE -> getPreferenceFile(exec)
            ClientScriptOpCode.OPENDIRECTORYPICKER -> openDirectoryPicker(exec)
            ClientScriptOpCode.GETDIRECTORYPICKERRESULT -> getDirectoryPickerResult(exec)
            ClientScriptOpCode.VALIDATECACHEDIRECTORY -> opValidateCacheDirectory(exec)
            ClientScriptOpCode.CAM_MOVETO -> camMoveTo(exec)
            ClientScriptOpCode.CAM_LOOKAT -> camLookat(exec)
            ClientScriptOpCode.CAM_MOVEALONG -> camMoveAlong(exec)
            ClientScriptOpCode.CAM_SMOOTHRESET -> camSmoothReset()
            ClientScriptOpCode.CAM_FORCEANGLE -> camForceAngle(exec)
            ClientScriptOpCode.CAM_GETANGLE_XA -> camGetAngleXa(exec)
            ClientScriptOpCode.CAM_GETANGLE_YA -> camGetAngleYa(exec)
            ClientScriptOpCode.CAM_INC_X -> camIncX()
            ClientScriptOpCode.CAM_DEC_X -> camDecX()
            ClientScriptOpCode.CAM_INC_Y -> camIncY()
            ClientScriptOpCode.CAM_DEC_Y -> camDecY()
            ClientScriptOpCode.CAM_SETFOLLOWHEIGHT -> camSetFollowHeight(exec)
            ClientScriptOpCode.CAM_GETFOLLOWHEIGHT -> camGetFollowHeight(exec)
            ClientScriptOpCode.COORD_CLAMPTOSCENE -> coordClampToScene(exec)
            ClientScriptOpCode.CAM_RESET -> camReset()
            ClientScriptOpCode.COORD_CLAMPTOSCENE_ORDEFAULT -> coordClampToSceneOrDefault(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by QuickChatDisplayOps")
        }
    }

    @JvmStatic
    fun camDecY() {
        StaticMethods.applyCameraYawLeft()
    }

    @JvmStatic
    fun getPreferenceFile(executor: CS2Executor) {
        executor.stringStack[++executor.stringStackPtr - 1] = GameShell.getPreferenceFile().toString()
    }

    @JvmStatic
    fun camMoveAlong(executor: CS2Executor) {
        executor.intStackPtr -= 6
        val positionPathIndex = executor.intStack[executor.intStackPtr]
        if (positionPathIndex >= 2) {
            throw RuntimeException()
        } else {
            Client.cameraPositionPathIndex = positionPathIndex
            val positionKeyframe = executor.intStack[executor.intStackPtr + 1]
            if (positionKeyframe + 1 >= (Client.cameraSplinePaths[Client.cameraPositionPathIndex]!!.size shr 1)) {
                throw RuntimeException()
            } else {
                Client.cameraPositionKeyframe = positionKeyframe
                Client.cameraSplineProgress = 0
                Client.cameraSplineSpeedStart = executor.intStack[executor.intStackPtr + 2]
                Client.cameraSplineSpeedEnd = executor.intStack[executor.intStackPtr + 3]
                val lookAtPathIndex = executor.intStack[executor.intStackPtr + 4]
                if (lookAtPathIndex >= 2) {
                    throw RuntimeException()
                } else {
                    Client.cameraLookAtPathIndex = lookAtPathIndex
                    val lookAtKeyframe = executor.intStack[executor.intStackPtr + 5]
                    if (lookAtKeyframe + 1 >= (Client.cameraSplinePaths[Client.cameraLookAtPathIndex]!!.size shr 1)) {
                        throw RuntimeException()
                    } else {
                        Client.cameraLookAtKeyframe = lookAtKeyframe
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
        var followHeight = executor.intStack[--executor.intStackPtr]
        if (followHeight < 0) {
            followHeight = 0
        }
        Client.cameraMaxZoom = followHeight + Client.cameraBaseZoom
    }

    @JvmStatic
    fun camGetAngleYa(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Camera.camAngleY.toInt() shr 3
    }

    @JvmStatic
    fun worldMapGetSourceCoord(executor: CS2Executor) {
        val displayCoord = executor.intStack[--executor.intStackPtr]
        val mapElement: WorldMapElement? = WorldMapDisplay.getStaticElement()
        if (mapElement != null) {
            val converted = mapElement.mapToGameCoords((displayCoord shr 14) and 0x3fff, displayCoord and 0x3fff, CS2Executor.coordConversionBuffer)
            if (converted) {
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
    fun openDirectoryPicker(executor: CS2Executor) {
        val dialogTitle: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.filePicker = DirectoryPicker(dialogTitle!!)
    }

    @JvmStatic
    fun chatCatGetSubCat(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val categoryId = executor.intStack[executor.intStackPtr]
        val subCatIndex = executor.intStack[executor.intStackPtr + 1]
        val category: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId)
        val subCatId = category!!.subCategories!![subCatIndex]
        executor.intStack[++executor.intStackPtr - 1] = subCatId
    }

    @JvmStatic
    fun worldMapJumpToSourceCoord(executor: CS2Executor) {
        val sourceCoord = executor.intStack[--executor.intStackPtr]
        val mapElement: WorldMapElement? = WorldMapDisplay.getStaticElement()
        if (mapElement != null) {
            val converted = mapElement.gameToMapCoordsForPlane((sourceCoord shr 28) and 0x3, (sourceCoord shr 14) and 0x3fff, sourceCoord and 0x3fff, CS2Executor.coordConversionBuffer)
            if (converted) {
                StaticMethods.setWorldMapScrollTarget(CS2Executor.coordConversionBuffer!![1], CS2Executor.coordConversionBuffer!![2])
            }
        }
    }

    @JvmStatic
    fun worldMapGetDisableElement(executor: CS2Executor) {
        val elementId = executor.intStack[--executor.intStackPtr]
        val hiddenNode: Node<*>? = WorldMapDisplay.hiddenMecIds.get(elementId.toLong())
        executor.intStack[++executor.intStackPtr - 1] = if (hiddenNode != null) 1 else 0
    }

    @JvmStatic
    fun qcSendClanChatMessage(executor: CS2Executor) {
        val connection: BufferedConnectionContext? = Client.connectionContext
        val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, connection!!.isaac)
        packet!!.buffer.writeByte(0)
        val payloadStart = packet!!.buffer.currentPosition
        packet!!.buffer.writeByte(2)
        packet!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(packet!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        packet!!.buffer.writeIndex(packet!!.buffer.currentPosition - payloadStart)
        connection!!.queuePacket(packet)
    }

    @JvmStatic
    fun openUrlFullscreenSafe(executor: CS2Executor) {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
        }
        val urlPath: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val loggedIn = executor.intStack[--executor.intStackPtr] == 1
        val fullUrl: String? = ServerEnvironment.getUrl() + urlPath
        Browser.openOpenjs(fullUrl!!, loggedIn, Client.useBrowserAppletMode)
    }

    @JvmStatic
    fun worldMapFlashElementCategory(executor: CS2Executor) {
        val categoryId = executor.intStack[--executor.intStackPtr]
        WorldMapDisplay.highlightCategory(categoryId)
    }

    @JvmStatic
    fun worldMapFindNearestElement(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val mecId = executor.intStack[executor.intStackPtr]
        val packedCoord = executor.intStack[executor.intStackPtr + 1]
        val coordX = (packedCoord shr 14) and 0x3fff
        val coordY = packedCoord and 0x3fff
        val nearestCoord = WorldMapCategory.findNearestElement(mecId, coordX, coordY)
        if (nearestCoord < 0) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = nearestCoord
        }
    }

    @JvmStatic
    fun chatCatGetDesc(executor: CS2Executor) {
        val categoryId = executor.intStack[--executor.intStackPtr]
        val category: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId)
        if (category!!.subCategories == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = category!!.subCategories!!.size
        }
    }

    @JvmStatic
    fun detailGetWindowedMode(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = WindowMode.getWindowMode()
    }

    @JvmStatic
    fun getDirectoryPickerResult(executor: CS2Executor) {
        var pickerFinished = false
        var path: String? = ""
        val filePicker = executor.filePicker
        if (filePicker != null && filePicker.fileChoserOpen()) {
            val directory: File? = filePicker.getPickedDirectory()
            if (directory != null) {
                path = directory.getPath()
            }
            pickerFinished = true
            executor.filePicker = null
        }
        executor.intStack[++executor.intStackPtr - 1] = if (pickerFinished) 1  else 0
        executor.stringStack[++executor.stringStackPtr - 1] = path
    }

    @JvmStatic
    fun fullscreenModeCount(executor: CS2Executor) {
        if (WindowMode.fullScreenSupported) {
            val displayModes = WindowMode.getActiveDisplayModeDetails()
            executor.intStack[++executor.intStackPtr - 1] = displayModes!!.size
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun detailGetFixedScreen(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.fixedScreen!!.getValue()
    }

    @JvmStatic
    fun worldMapDisableElementCategory(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val categoryId = executor.intStack[executor.intStackPtr]
        val disabled = executor.intStack[executor.intStackPtr + 1] == 1
        var hiddenNode: Node<Any?>? = WorldMapDisplay.hiddenCategoryIds.get(categoryId.toLong())
        if (hiddenNode != null && !disabled) {
            hiddenNode.unlink()
        } else if (hiddenNode == null && disabled) {
            hiddenNode = object : Node<Any?>() {}
            WorldMapDisplay.hiddenCategoryIds.put(hiddenNode!!, categoryId.toLong())
        }
    }

    @JvmStatic
    fun worldMapGetCurrentMap(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (WorldMap.staticElement != null) WorldMap.staticElement!!.elementId else -1
    }

    @JvmStatic
    fun chooseRenderType(executor: CS2Executor) {
        val windowMode = executor.intStack[--executor.intStackPtr]
        if (windowMode >= 1 && windowMode <= 2) {
            WindowMode.requestWindowMode(windowMode, -1, -1, false)
        }
    }

    @JvmStatic
    fun worldMapDisableElements(executor: CS2Executor) {
        WorldMapDisplay.hideIcons = executor.intStack[--executor.intStackPtr] == 1
    }

    @JvmStatic
    fun qcSendFriendsChatMessage(executor: CS2Executor) {
        val connection: BufferedConnectionContext? = Client.connectionContext
        val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, connection!!.isaac)
        packet!!.buffer.writeByte(0)
        val payloadStart = packet!!.buffer.currentPosition
        packet!!.buffer.writeByte(1)
        packet!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(packet!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        packet!!.buffer.writeIndex(packet!!.buffer.currentPosition - payloadStart)
        connection!!.queuePacket(packet)
    }

    @JvmStatic
    fun qcGetSubHotkey(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val categoryId = executor.intStack[executor.intStackPtr]
        val subCatIndex = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId).subCategoryHotkeys!![subCatIndex].code
    }

    @JvmStatic
    fun chatPhraseFind(executor: CS2Executor) {
        val query: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val isMenuSearch = executor.intStack[--executor.intStackPtr] == 1
        QuickchatMessageType.searchQuickchatMessages(query!!, isMenuSearch)
        executor.intStack[++executor.intStackPtr - 1] = CS2QueryResults.count
    }

    @JvmStatic
    fun camIncY() {
        Client.applyCameraYawRight()
    }

    @JvmStatic
    fun worldMapIsLoaded(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (ComponentMinimap.mapAssetLoadingPercentage == 100) 1 else 0
    }

    @JvmStatic
    fun chatPhraseGetAutoResponse(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val messageId = executor.intStack[executor.intStackPtr]
        val responseIndex = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(messageId).responses!![responseIndex]
    }

    @JvmStatic
    fun chatPhraseGetText(executor: CS2Executor) {
        val messageId = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(messageId).getPlaceholderText()
    }

    @JvmStatic
    fun qcSendPublicMessage(executor: CS2Executor) {
        val connection: BufferedConnectionContext? = Client.connectionContext
        val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, connection!!.isaac)
        packet!!.buffer.writeByte(0)
        val payloadStart = packet!!.buffer.currentPosition
        packet!!.buffer.writeByte(0)
        packet!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(packet!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        packet!!.buffer.writeIndex(packet!!.buffer.currentPosition - payloadStart)
        connection!!.queuePacket(packet)
    }

    @JvmStatic
    fun isGameScreenState(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (GameState.isRenderingScene(Client.gameState)) 1  else 0
    }

    @JvmStatic
    fun worldMapListElementNext(executor: CS2Executor) {
        val category: WorldMapCategory? = WorldMap.getNextActiveCategory()
        if (category == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = category.mecId
            val coord = (category!!.plane shl 28) or category.absX + (WorldMap.mapSquareAbsX shl 14) or category.absY + WorldMap.mapSquareAbsY
            executor.intStack[++executor.intStackPtr - 1] = coord
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
        val keyframeCount = (Client.cameraSplinePaths!![executor.intStack[--executor.intStackPtr]]!!.size shr 1)
        executor.intStack[++executor.intStackPtr - 1] = keyframeCount
    }

    @JvmStatic
    fun camMoveTo(executor: CS2Executor) {
        executor.intStackPtr -= 4
        val coord = executor.intStack[executor.intStackPtr]
        val height = executor.intStack[executor.intStackPtr + 1]
        val constantSpeed = executor.intStack[executor.intStackPtr + 2]
        val variableSpeed = executor.intStack[executor.intStackPtr + 3]
        val sceneBase: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        Camera.moveCamera(((coord shr 14) and 0x3fff) - sceneBase!!.x, (coord and 0x3fff) - sceneBase!!.y, (height shl 2), constantSpeed, variableSpeed, false)
    }

    @JvmStatic
    fun chatCatFindPhraseByShortcut(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val categoryId = executor.intStack[executor.intStackPtr]
        val hotkey = executor.intStack[executor.intStackPtr + 1]
        if (hotkey == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId).messageForHotkey(hotkey.toChar())
        }
    }

    @JvmStatic
    fun opGetHostName(executor: CS2Executor) {
        var hostName: String? = null
        hostName = PacketDecoder.HOSTNAME_IDENTIFIER!!.getHostName()
        if (hostName == null) {
            hostName = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = hostName
    }

    @JvmStatic
    fun camIncX() {
        StaticMethods.applyCameraPitchUp()
    }

    @JvmStatic
    fun worldMapSetMapCoordOverride(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val elementId = executor.intStack[executor.intStackPtr]
        val coord = executor.intStack[executor.intStackPtr + 1]
        WorldMapDisplay.reset(elementId, (coord shr 14) and 0x3fff, coord and 0x3fff, true)
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
    fun worldMapGetConfigSize(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.worldMapViewWidth
        executor.intStack[++executor.intStackPtr - 1] = ComponentMinimap.mapViewportScale
    }

    @JvmStatic
    fun fullscreenRestore() {
        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
        }
    }

    @JvmStatic
    fun fullscreenGetMode(executor: CS2Executor) {
        val modeIndex = executor.intStack[--executor.intStackPtr]
        if (WindowMode.fullScreenSupported) {
            val displayModes = WindowMode.getActiveDisplayModeDetails()
            executor.intStack[++executor.intStackPtr - 1] = displayModes!![modeIndex].width
            executor.intStack[++executor.intStackPtr - 1] = displayModes!![modeIndex].height
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun coordClampToScene(executor: CS2Executor) {
        val packedCoord = executor.intStack[--executor.intStackPtr]
        var sceneTileX = (packedCoord shr 14) and 0x3fff
        var sceneTileY = packedCoord and 0x3fff
        val sceneBase: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        sceneTileX -= sceneBase!!.x
        if (sceneTileX < 0) {
            sceneTileX = 0
        } else if (sceneTileX >= Resource.mapRegion!!.sceneSizeX) {
            sceneTileX = Resource.mapRegion!!.sceneSizeX
        }
        sceneTileY -= sceneBase!!.y
        if (sceneTileY < 0) {
            sceneTileY = 0
        } else if (sceneTileY >= Resource.mapRegion!!.sceneSizeY) {
            sceneTileY = Resource.mapRegion!!.sceneSizeY
        }
        Client.cameraTargetAbsX = ((sceneTileX shl 9)) + 256
        Client.cameraTargetAbsY = ((sceneTileY shl 9)) + 256
        Camera.cameraCalculationType = 4
        StaticMethods.cameraTargetAbsY = -1
        PingRequester.cameraTargetAbsX = -1
    }

    @JvmStatic
    fun worldMapGetDisplayPosition(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = WorldMapDisplay.mapCenterX + WorldMap.mapSquareAbsX
        executor.intStack[++executor.intStackPtr - 1] = WorldMapDisplay.mapCenterY + WorldMap.mapSquareAbsY
    }

    @JvmStatic
    fun camForceAngle(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Camera.setAngle(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], 0)
    }

    @JvmStatic
    fun camDecX() {
        StaticMethods.applyCameraPitchDown()
    }

    @JvmStatic
    fun worldMapGetDisableElementCategory(executor: CS2Executor) {
        val categoryId = executor.intStack[--executor.intStackPtr]
        val hiddenNode: Node<Any?>? = WorldMapDisplay.hiddenCategoryIds.get(categoryId.toLong())
        executor.intStack[++executor.intStackPtr - 1] = if (hiddenNode != null) 1 else 0
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
    fun worldMapCoordInMap(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val packedCoord = executor.intStack[executor.intStackPtr]
        val elementId = executor.intStack[executor.intStackPtr + 1]
        val renderedElements: Queue<WorldMapElement>? = WorldMap.getRenderedElements((packedCoord shr 14) and 0x3fff, packedCoord and 0x3fff)
        var found = false
        var mapElement: WorldMapElement? = renderedElements?.getNext() as WorldMapElement?
        while (mapElement != null) {
            if (elementId == mapElement.elementId) {
                found = true
                break
            }
            mapElement = renderedElements?.getDirectNext() as WorldMapElement?
        }
        if (found) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun worldMapJumpToDisplayCoord(executor: CS2Executor) {
        val displayCoord = executor.intStack[--executor.intStackPtr]
        StaticMethods.setWorldMapScrollTarget((displayCoord shr 14) and 0x3fff, displayCoord and 0x3fff)
    }

    @JvmStatic
    fun qcGetMessage(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val categoryId = executor.intStack[executor.intStackPtr]
        val messageIndex = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId).messages!![messageIndex]
    }

    @JvmStatic
    fun worldMapGetConfigOrigin(executor: CS2Executor) {
        val elementId = executor.intStack[--executor.intStackPtr]
        val mapElement: WorldMapElement? = WorldMap.poll(elementId)
        if (mapElement == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = (mapElement.bitpackedPlacement shr 14) and 0x3fff
            executor.intStack[++executor.intStackPtr - 1] = mapElement.bitpackedPlacement and 0x3fff
        }
    }

    @JvmStatic
    fun resetChatMediaAndFont() {
        StaticMedia.resetChatMediaAndIcons()
        FontMetrics.useBoldFont = false
    }

    @JvmStatic
    fun qcGetPhraseValue(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val messageId = executor.intStack[executor.intStackPtr]
        val valueTypeIndex = executor.intStack[executor.intStackPtr + 1]
        val configIndex = executor.intStack[executor.intStackPtr + 2]
        val messageType: QuickchatMessageType? = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(messageId)
        if (messageType!!.getValueType(valueTypeIndex)!!.id != 0) {
            throw RuntimeException("")
        } else {
            executor.intStack[++executor.intStackPtr - 1] = messageType!!.getValueConfig(valueTypeIndex, configIndex)
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
    fun qcSetValueAlt(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val valueIndex = executor.intStack[executor.intStackPtr]
        val dynamicValue = executor.intStack[executor.intStackPtr + 1]
        executor.currentQuickChatMessage!!.dynamicValues!![valueIndex] = dynamicValue
    }

    @JvmStatic
    fun browserClose() {
    }

    @JvmStatic
    fun chatPhraseGetDynamicCommandParamEnum(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val messageId = executor.intStack[executor.intStackPtr]
        val valueTypeIndex = executor.intStack[executor.intStackPtr + 1]
        val valueTypeId = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(messageId).getValueType(valueTypeIndex)!!.id
        executor.intStack[++executor.intStackPtr - 1] = valueTypeId
    }

    @JvmStatic
    fun getClipboard(executor: CS2Executor) {
        var clipboardText: String? = ""
        if (Console.clipboard != null) {
            val transferable: Transferable? = Console.clipboard!!.getContents(null)
            if (transferable != null) {
                try {
                    clipboardText = transferable.getTransferData(DataFlavor.stringFlavor) as? String
                } catch (ignored: Exception) {
                }
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = clipboardText
    }

    @JvmStatic
    fun doCheat(executor: CS2Executor) {
        CommandExecutor.executeCommand(executor.stringStack[--executor.stringStackPtr] as String, false, false)
    }

    @JvmStatic
    fun browserOpen() {
    }

    @JvmStatic
    fun chatPhraseGetDynamicCommand(executor: CS2Executor) {
        val messageId = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(messageId).getValueTypeCount()
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
        val mapElement: WorldMapElement? = WorldMap.poll(executor.intStack[--executor.intStackPtr])
        if (mapElement != null) {
            if (mapElement.displayName != null) {
                executor.stringStack[++executor.stringStackPtr - 1] = mapElement.displayName
                return
            }
        }
        executor.stringStack[++executor.stringStackPtr - 1] = ""
    }

    @JvmStatic
    fun ifGettop(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val componentHash = executor.intStack[executor.intStackPtr]
        val slotId = executor.intStack[executor.intStackPtr + 1]
        val stack: IntArray? = executor.intStack
        val stackIndex = ++executor.intStackPtr - 1
        val hasEntry = if (Interface.hasActiveMenuEntry(componentHash, slotId)) 1 else 0
        stack!![stackIndex] = hasEntry
    }

    @JvmStatic
    fun coordClampToSceneOrDefault(executor: CS2Executor) {
        val packedCoord = executor.intStack[--executor.intStackPtr]
        if (packedCoord == -1) {
            var sceneTileX = 16383
            var sceneTileY = packedCoord and 0x3fff
            val sceneBase: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
            sceneTileX -= sceneBase!!.x
            if (sceneTileX < 0) {
                sceneTileX = 0
            } else if (sceneTileX >= Resource.mapRegion!!.sceneSizeX) {
                sceneTileX = Resource.mapRegion!!.sceneSizeX
            }
            sceneTileY -= sceneBase!!.y
            if (sceneTileY < 0) {
                sceneTileY = 0
            } else if (sceneTileY >= Resource.mapRegion!!.sceneSizeY) {
                sceneTileY = Resource.mapRegion!!.sceneSizeY
            }
            PingRequester.cameraTargetAbsX = ((sceneTileX shl 9)) + 256
            StaticMethods.cameraTargetAbsY = ((sceneTileY shl 9)) + 256
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
        val pathIndex = executor.intStack[executor.intStackPtr]
        val keyframeBase = executor.intStack[executor.intStackPtr + 1] shl 1
        val posCoord = executor.intStack[executor.intStackPtr + 2]
        val posHeight = executor.intStack[executor.intStackPtr + 3]
        val lookAtCoord = executor.intStack[executor.intStackPtr + 4]
        val lookAtHeight = executor.intStack[executor.intStackPtr + 5]
        val pitch = executor.intStack[executor.intStackPtr + 6]
        if (pathIndex >= 0 && pathIndex < 2 && Client.cameraSplinePaths[pathIndex] != null && keyframeBase >= 0 && keyframeBase < Client.cameraSplinePaths[pathIndex]!!.size) {
            Client.cameraSplinePaths[pathIndex]!![keyframeBase] = intArrayOf(((posCoord shr 14) and 0x3fff) shl 9, posHeight shl 2, ((posCoord and 0x3fff) shl 9), pitch)
            Client.cameraSplinePaths[pathIndex]!![keyframeBase + 1] = intArrayOf(((lookAtCoord shr 14) and 0x3fff) shl 9, lookAtHeight shl 2, (lookAtCoord and 0x3fff) shl 9)
        }
    }

    @JvmStatic
    fun worldMapFlashElement(executor: CS2Executor) {
        StaticMethods.selectWorldMapElement(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun chatCatGetSubCatCount(executor: CS2Executor) {
        val categoryId = executor.intStack[--executor.intStackPtr]
        val category: QuickChatCategoryType? = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId)
        if (category!!.messages == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = category!!.messages!!.size
        }
    }

    @JvmStatic
    fun qcGetName(executor: CS2Executor) {
        val categoryId = executor.intStack[--executor.intStackPtr]
        executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId).name
    }

    @JvmStatic
    fun miniMenuSetMaxEntries(executor: CS2Executor) {
        MiniMenu.maxVisibleEntries = executor.intStack[--executor.intStackPtr]
    }

    @JvmStatic
    fun qcSetValue(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val valueIndex = executor.intStack[executor.intStackPtr]
        val dynamicValue = executor.intStack[executor.intStackPtr + 1]
        executor.currentQuickChatMessage!!.dynamicValues!![valueIndex] = dynamicValue
    }

    @JvmStatic
    fun qcSendGuestClanChatMessage(executor: CS2Executor) {
        val connection: BufferedConnectionContext? = Client.connectionContext
        val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PUBLIC, connection!!.isaac)
        packet!!.buffer.writeByte(0)
        val payloadStart = packet!!.buffer.currentPosition
        packet!!.buffer.writeByte(3)
        packet!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(packet!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        packet!!.buffer.writeIndex(packet!!.buffer.currentPosition - payloadStart)
        connection!!.queuePacket(packet)
    }

    @JvmStatic
    fun consolePrint(executor: CS2Executor) {
        System.out.println(executor.stringStack[--executor.stringStackPtr])
    }

    @JvmStatic
    fun worldMapListElementStart(executor: CS2Executor) {
        val category: WorldMapCategory? = WorldMapDisplay.getFirstActiveCategory()
        if (category == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = category.mecId
            val coord = (category!!.plane shl 28) or category.absX + (WorldMap.mapSquareAbsX shl 14) or category.absY + WorldMap.mapSquareAbsY
            executor.intStack[++executor.intStackPtr - 1] = coord
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
    fun worldMapGetConfigZoom(executor: CS2Executor) {
        val elementId = executor.intStack[--executor.intStackPtr]
        val mapElement: WorldMapElement? = WorldMap.poll(elementId)
        if (mapElement == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = mapElement.defaultZoomLevel
        }
    }

    @JvmStatic
    fun worldMapGetDisplayCoord(executor: CS2Executor) {
        val sourceCoord = executor.intStack[--executor.intStackPtr]
        val mapElement: WorldMapElement? = WorldMapDisplay.getStaticElement()
        if (mapElement != null) {
            val converted = mapElement.gameToMapCoordsForPlane((sourceCoord shr 28) and 0x3, (sourceCoord shr 14) and 0x3fff, sourceCoord and 0x3fff, CS2Executor.coordConversionBuffer)
            if (converted) {
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
        val coord = executor.intStack[--executor.intStackPtr]
        val mapElement: WorldMapElement? = WorldMap.getMapDef((coord shr 14) and 0x3fff, coord and 0x3fff)
        if (mapElement == null) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = mapElement.elementId
        }
    }

    @JvmStatic
    fun worldmapCloseMap() {
        Client.setLoadingWorldMap()
    }

    @JvmStatic
    fun opValidateCacheDirectory(executor: CS2Executor) {
        val cacheDirectoryPath: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val isValid = ClientStartup.activeClient!!.validateCacheDirectory(File(cacheDirectoryPath))
        executor.intStack[++executor.intStackPtr - 1] = if (isValid) 1  else 0
    }

    @JvmStatic
    fun camGetFollowHeight(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.cameraMaxZoom - Client.cameraBaseZoom
    }

    @JvmStatic
    fun setupMenuChrome(executor: CS2Executor) {
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
    fun chatPhraseGetAutoResponseCount(executor: CS2Executor) {
        val messageId = executor.intStack[--executor.intStackPtr]
        val messageType: QuickchatMessageType? = TypeLists.QUICK_CHAT_MESSAGE!!.listMessage(messageId)
        if (messageType!!.responses == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = messageType!!.responses!!.size
        }
    }

    @JvmStatic
    fun chatCatGetPhraseShortcut(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val categoryId = executor.intStack[executor.intStackPtr]
        val messageIndex = executor.intStack[executor.intStackPtr + 1]
        executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId).messageHotkeys!![messageIndex].code
    }

    @JvmStatic
    fun openUrlNoLogin(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        PacketEncoder.sendLobbyHyperlink(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, "", executor.intStack[--executor.intStackPtr] == 1, false)
    }

    @JvmStatic
    fun opIsApplet(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.isApplet) 1  else 0
    }

    @JvmStatic
    fun qcSendPrivateMessage(executor: CS2Executor) {
        val recipientName: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val connection: BufferedConnectionContext? = Client.connectionContext
        val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.QUICKCHAT_PRIVATE, connection!!.isaac)
        packet!!.buffer.writeByte(0)
        val payloadStart = packet!!.buffer.currentPosition
        packet!!.buffer.writeString(recipientName!!)
        packet!!.buffer.writeShort(executor.currentQuickChatMessage!!.id)
        executor.currentQuickChatMessage!!.message!!.encodeValues(packet!!.buffer, executor.currentQuickChatMessage!!.dynamicValues!!)
        packet!!.buffer.writeIndex(packet!!.buffer.currentPosition - payloadStart)
        connection!!.queuePacket(packet)
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
    fun worldMapDisableElement(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val elementId = executor.intStack[executor.intStackPtr]
        val disabled = executor.intStack[executor.intStackPtr + 1] == 1
        var hiddenNode: Node<*>? = WorldMapDisplay.hiddenMecIds.get(elementId.toLong())
        if (hiddenNode != null && !disabled) {
            hiddenNode.unlink()
        } else if (hiddenNode == null && disabled) {
            hiddenNode = object : Node<Any?>() {}
            WorldMapDisplay.hiddenMecIds.put(hiddenNode, elementId.toLong())
        }
    }

    @JvmStatic
    fun worldMapSetMapCoord(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val elementId = executor.intStack[executor.intStackPtr]
        val coord = executor.intStack[executor.intStackPtr + 1]
        WorldMapDisplay.reset(elementId, (coord shr 14) and 0x3fff, coord and 0x3fff, false)
    }

    @JvmStatic
    fun openUrlShim(executor: CS2Executor) {
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
        val coord = executor.intStack[executor.intStackPtr]
        val height = executor.intStack[executor.intStackPtr + 1]
        val speed = executor.intStack[executor.intStackPtr + 2]
        val angle = executor.intStack[executor.intStackPtr + 3]
        val sceneBase: CoordGrid? = Resource.mapRegion!!.getBaseCoordinates()
        Camera.lookat(((coord shr 14) and 0x3fff) - sceneBase!!.x, (coord and 0x3fff) - sceneBase!!.y, (height shl 2), speed, angle)
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
        val width = WindowMode.fullScreenWidth
        val height = WindowMode.fullScreenHeight
        var modeIndex = -1
        if (WindowMode.fullScreenSupported) {
            val displayModes: Array<DisplayModeDetails>? = WindowMode.getActiveDisplayModeDetails()
            for (i in 0 until displayModes!!.size) {
                val mode: DisplayModeDetails? = displayModes[i]
                if (width == mode!!.width && height == mode!!.height) {
                    modeIndex = i
                    break
                }
            }
        }
        executor.intStack[++executor.intStackPtr - 1] = modeIndex
    }

    @JvmStatic
    fun setRecolPalette(executor: CS2Executor) {
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
        val pathIndex = executor.intStack[executor.intStackPtr]
        val keyframeCount = executor.intStack[executor.intStackPtr + 1]
        if (pathIndex >= 0 && pathIndex < 2) {
            Client.cameraSplinePaths!![pathIndex] = Array(4) { IntArray(keyframeCount shl 1) }
        }
    }

    @JvmStatic
    fun worldMapGetSize(executor: CS2Executor) {
        val elementId = executor.intStack[--executor.intStackPtr]
        val mapElement: WorldMapElement? = WorldMap.poll(elementId)
        if (mapElement == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = mapElement.boundsMaxX - mapElement.boundsMinX
            executor.intStack[++executor.intStackPtr - 1] = mapElement.boundsMaxY - mapElement.boundsMinY
        }
    }

    @JvmStatic
    fun worldMapGetSourcePosition(executor: CS2Executor) {
        val mapElement: WorldMapElement? = WorldMapDisplay.getStaticElement()
        if (mapElement != null) {
            val converted = mapElement.mapToGameCoords(WorldMap.mapSquareAbsX + WorldMapDisplay.mapCenterX, WorldMap.mapSquareAbsY + WorldMapDisplay.mapCenterY, CS2Executor.coordConversionBuffer)
            if (converted) {
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
    fun worldMapGetDisableElements(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (WorldMapDisplay.hideIcons) 1  else 0
    }

    @JvmStatic
    fun detailScreenSize(executor: CS2Executor) {
        val windowMode = executor.intStack[--executor.intStackPtr]
        if (windowMode >= 1 && windowMode <= 2) {
            PreferenceManager.preferences!!.fixedScreen!!.setValue(windowMode)
            PreferenceManager.preferences!!.fullScreen!!.setValue(windowMode)
            PreferenceManager.savePreferences()
        }
    }

    @JvmStatic
    fun chatCatFindSubCatByShortcut(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val categoryId = executor.intStack[executor.intStackPtr]
        val hotkey = executor.intStack[executor.intStackPtr + 1]
        if (hotkey == -1) {
            executor.intStack[++executor.intStackPtr - 1] = -1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.QUICK_CHAT_CATEGORY!!.list(categoryId).subCategoryForHotkey(hotkey.toChar())
        }
    }

    @JvmStatic
    fun worldMapSetMap(executor: CS2Executor) {
        WorldMapDisplay.reset(executor.intStack[--executor.intStackPtr], -1, -1, false)
    }

}
