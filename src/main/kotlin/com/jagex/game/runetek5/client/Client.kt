package com.jagex.game.runetek5.client

import com.darkan.bot.Bot
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.botdetection.ReflectionCheckManager
import com.jagex.game.runetek5.buffer.CircularBuffer
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.impl.ObjectNode
import com.jagex.game.runetek5.cache.node.lru.LRUCache
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.CommandExecutor
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.CanvasKeyRecorder
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord
import com.jagex.game.runetek5.client.input.keyboard.KeyRecorder
import com.jagex.game.runetek5.client.input.mouse.AbstractMouseManager
import com.jagex.game.runetek5.client.input.mouse.CursorManager
import com.jagex.game.runetek5.client.input.mouse.MouseActionClient
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer
import com.jagex.game.runetek5.client.input.mouse.MouseRecord
import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.client.login.LoginProt
import com.jagex.game.runetek5.client.login.LoginStateUpdate
import com.jagex.game.runetek5.client.menu.MenuInteraction
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.periodic.PulseEventHandler
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.client.window.WindowSettings
import com.jagex.game.runetek5.config.Identifiable
import com.jagex.game.runetek5.config.IndexReference
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.cursor.CursorTypeList
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.cutscene.action.CutsceneBasicMovement
import com.jagex.game.runetek5.config.defaults.EntityDefaults
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.midi.VorbisNode
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.IFEvents
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.sequence.MovementType
import com.jagex.game.runetek5.config.sequence.PositionedGfx
import com.jagex.game.runetek5.config.sun.Sun
import com.jagex.game.runetek5.entity.ComparableGraphNode
import com.jagex.game.runetek5.entity.ComparableInteractiveEntity
import com.jagex.game.runetek5.entity.EntityNodeList
import com.jagex.game.runetek5.entity.grounditem.GroundItemWrapper
import com.jagex.game.runetek5.entity.pathingentity.EntityOverhead
import com.jagex.game.runetek5.entity.pathingentity.EntityUpdating
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.npc.NPCUpdate
import com.jagex.game.runetek5.entity.pathingentity.npc.NpcEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.font.BasicFontProvider
import com.jagex.game.runetek5.graphics.font.FontManager
import com.jagex.game.runetek5.graphics.font.FontProvider
import com.jagex.game.runetek5.graphics.hint.HintDetails
import com.jagex.game.runetek5.graphics.hint.HintRenderer
import com.jagex.game.runetek5.graphics.hint.HintTrail
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.loginscreen.LoginScreenComponent
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMap
import com.jagex.game.runetek5.graphics.map.worldmap.WorldMapSub1
import com.jagex.game.runetek5.graphics.model.particle.ParticleProducer
import com.jagex.game.runetek5.graphics.sprite.StaticMedia
import com.jagex.game.runetek5.icmp.IcmpServiceJaclib
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel
import com.jagex.game.runetek5.interaction.chat.clanchat.settings.ClanSettings
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat
import com.jagex.game.runetek5.js5.dependency.CacheableDependencies
import com.jagex.game.runetek5.js5.request.RequestManager
import com.jagex.game.runetek5.js5.request.local.LocalRequester
import com.jagex.game.runetek5.js5.request.remote.RemoteFileRequester
import com.jagex.game.runetek5.math.Matrix43
import com.jagex.game.runetek5.math.QuaternionPool
import com.jagex.game.runetek5.math.Vector3Pool
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies
import com.jagex.game.runetek5.network.AsyncConnection
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.Connection
import com.jagex.game.runetek5.network.ConnectionInfo
import com.jagex.game.runetek5.network.PingRequester
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.packet.ClientProt
import com.jagex.game.runetek5.network.packet.GrandExchangeSlot
import com.jagex.game.runetek5.network.packet.PacketDecoder
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.rendering.MessageBox
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.scene.AsyncMapLoader
import com.jagex.game.runetek5.scene.FrameRenderException
import com.jagex.game.runetek5.scene.LocAction
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.scene.RegionLoadRequest
import com.jagex.game.runetek5.scene.RegionLoadingStage
import com.jagex.game.runetek5.scene.RegionLoadingType
import com.jagex.game.runetek5.scene.RenderedEntityCache
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.scene.environment.Environment
import com.jagex.game.runetek5.scene.tile.TileMessage
import com.jagex.game.runetek5.sound.SoundResourceManager
import com.jagex.game.runetek5.sound.player.area.AmbientSound
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.sound.player.area.SoundEffectPlayer
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.toolkit.AbstractNativeToolkit
import com.jagex.game.runetek5.toolkit.AbstractToolkit
import com.jagex.game.runetek5.tools.Browser
import com.jagex.game.runetek5.tools.Language
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.tools.TimeTools
import com.jagex.game.runetek5.utils.ArrayUtils
import com.jagex.game.runetek5.utils.Base64
import com.jagex.game.runetek5.utils.MathUtils
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarProviders
import jaclib.ping.Ping
import java.awt.Color
import java.awt.Frame
import java.awt.Rectangle
import java.io.IOException
import java.net.Socket
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Random
import java.util.TimeZone
import java.util.Vector
import kotlin.concurrent.Volatile
import kotlin.math.sin

class Client : GameShell() {
    public override fun init() {
        var buildEnvironmentMinor: String? = ""
        val parameters = Parameter.values()

        for (index in parameters.indices) {
            val parameter = parameters[index]
            val paramString = suppliedApplet!!.getParameter(parameter.parameterName)
            if (paramString != null) {
                when (parameter.parameterName.toInt()) {
                    1 -> {
                        base64Url = Base64.decode(TextUtils.decodeUrl(paramString))
                        if (base64Url!!.size < 16) {
                            base64Url = null
                        }
                    }

                    2 -> useBrowserAppletMode = "true".equals(paramString, ignoreCase = true)
                    3 -> customStringParam = paramString
                    4 -> {
                        supplementaryLoginData = paramString
                        if (supplementaryLoginData!!.length > 100) {
                            supplementaryLoginData = null
                        }
                    }

                    5 -> {
                        if (WorldConnection.LOBBY_CONNECTION_INFO == null) {
                            WorldConnection.LOBBY_CONNECTION_INFO = ConnectionInfo()
                        }

                        WorldConnection.LOBBY_CONNECTION_INFO!!.host = paramString
                    }

                    6 -> COUNTRY = paramString.toInt()
                    7 -> buildEnvironmentMinor = paramString
                    8 -> CURRENT_GAME = Game.forid(paramString.toInt())
                    9 -> isApplet = "true".equals(paramString, ignoreCase = true)
                    10 -> showErrorOnFailedZap = "true".equals(paramString, ignoreCase = true)
                    11 -> addGamescreenPadding = "true".equals(paramString, ignoreCase = true)
                    12, 21 -> {}
                    13 -> loginToken = paramString
                    14 -> {
                        if (WorldConnection.LOBBY_CONNECTION_INFO == null) {
                            WorldConnection.LOBBY_CONNECTION_INFO = ConnectionInfo()
                        }

                        WorldConnection.LOBBY_CONNECTION_INFO!!.worldId = paramString.toInt()
                    }

                    15 -> {
                        BUILD_ENVIRONMENT = BuildType.parse(paramString.toInt())
                        if (BUILD_ENVIRONMENT != BuildType.WIP && BUILD_ENVIRONMENT != BuildType.RC && BUILD_ENVIRONMENT != BuildType.BUILD_LIVE && BUILD_ENVIRONMENT != BuildType.LIVE) {
                            BUILD_ENVIRONMENT = BuildType.LIVE
                        }
                    }

                    16 -> {
                        WorldConnection.GAME_CONNECTION = ConnectionInfo()
                        WorldConnection.GAME_CONNECTION!!.worldId = paramString.toInt()
                    }

                    17 -> js5AuthToken = paramString
                    18 -> if ("true".equals(paramString, ignoreCase = true)) {
                        IS_MEMBER = true
                    }

                    19 -> PacketEncoder.sendErrorToServer("", RuntimeException())
                    20 -> loginScreenImageResourceNames = paramString
                    22 -> hasSignedTermsAgreement = "true".equals(paramString, ignoreCase = true)
                    23 -> AFFILIATE = paramString.toInt()
                    24 -> {
                        ServerEnvironment.SERVER_ENVIRONMENT = Identifiable.identify(ServerEnvironment.values(), paramString.toInt()) as ServerEnvironment?
                        if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LOCAL) {
                            ServerEnvironment.SERVER_ENVIRONMENT = ServerEnvironment.WTWIP
                        } else if (!ServerEnvironment.isHostedTestEnvironment(ServerEnvironment.SERVER_ENVIRONMENT!!) && ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
                            ServerEnvironment.SERVER_ENVIRONMENT = ServerEnvironment.LIVE
                        }
                    }

                    25 -> suppressTutorials = "true".equals(paramString, ignoreCase = true)
                    26 -> machineUid = paramString.toLong()
                    27 -> sessionToken = paramString.toInt()
                    28 -> hasJustCreatedAccount = "true" == paramString
                    29 -> billingFlag = "true" == paramString
                    30 -> loginType = paramString.toInt()
                    31 -> CURRENT_LANGUAGE = Language.getLanguage(paramString.toInt())
                    32 -> mapOwnerIdentifier = paramString
                    33 -> affiliateP = paramString
                    34 -> Browser.errorWebsiteBaseUrl = paramString
                    35 -> {
                        DEBUG_DISPLAY_COLOR_INDEX = paramString.toInt()
                        if (DEBUG_DISPLAY_COLOR_INDEX < 0 || DEBUG_DISPLAY_COLOR_INDEX >= DEBUG_LOAD_COLOR.size) {
                            DEBUG_DISPLAY_COLOR_INDEX = 0
                        }
                    }

                    else -> PacketEncoder.sendErrorToServer("", RuntimeException())
                }
            }
        }

        if (affiliateP == null) {
            affiliateP = ""
        }

        checkNotNull(CURRENT_GAME)
        val windowSettings = WindowSettings(GAME_WIDTH, GAME_HEIGHT, BORDER_WIDTH, BORDER_HEIGHT, CURRENT_GAME!!.formattedName)
        ClientStartup.activeClient = this
        var buildEnvironmentMajor: String = BUILD_ENVIRONMENT!!.getName()
        var storeId: Int = BUILD_ENVIRONMENT!!.id + 32
        if (!buildEnvironmentMinor!!.isEmpty()) {
            buildEnvironmentMajor = buildEnvironmentMajor + "_" + buildEnvironmentMinor
            storeId = 0
        }

        initializeFrame(windowSettings, CURRENT_GAME!!.name, buildEnvironmentMajor, storeId, IndexReference.getMaxIndexReference(), addGamescreenPadding)
    }

    public override fun onPulse() {
        if (PreferenceManager.preferences!!.activeToolkit!!.getValue() == 2) {
            try {
                pulse()
            } catch (threaddeath3: ThreadDeath) {
                throw threaddeath3
            } catch (throwable4: Throwable) {
                //was added throwable_4.printStackTrace();
                PacketEncoder.sendErrorToServer(throwable4.message + " " + getDebugString(), throwable4)
                renderWithCpu = true
                loadAssets(0, false)
            }
        } else {
            pulse()
        }
    }

    fun pulse() {
        if (gameState != GameState.UNK_16) {
            //System.out.println("[Client]GameState: "+client.gameState);
            if (maploadingLockTaken) {
                synchronized(ASYNC_MAPLOADING_LOCK) {
                    (ASYNC_MAPLOADING_LOCK as Object).notify()
                }
                synchronized(MAP_REGION_LOADER_LOCK) {
                    try {
                        (MAP_REGION_LOADER_LOCK as Object).wait()
                    } catch (ie: InterruptedException) {
                        ie.printStackTrace()
                    }
                }

                maploadingLockTaken = false
            }


            ++cycles
            if (cycles % 1000 == 1) {
                val calender = GregorianCalendar()
                randomSeed = calender.get(Calendar.HOUR_OF_DAY) * 600 + calender.get(Calendar.MINUTE) * 10 + calender.get(Calendar.SECOND) / 6
                RANDOMIZER.setSeed(randomSeed.toLong())
            }

            GAME_CONNECTION_CONTEXT.cycle()
            LOBBY_CONNECTION_CONTEXT.cycle()
            pulseFileRequester()
            if (requestManager != null) {
                requestManager!!.pulse()
            }

            StaticMethods.updateSoundSystems()
            VorbisNode.processVorbisStreams()
            KeyRecorder.keyRecorder!!.prepareAwaitingKeys()
            MouseRecord.recorder!!.syncMouseState()
            if (Renderers.activeToolkit != null) {
                Renderers.activeToolkit!!.animateTextures(Utils.time().toInt())
            }

            keyCachePointerPoss = 0
            maximumHeldKeys = 0

            run {
                var record = KeyRecorder.keyRecorder!!.popHead()
                while (record != null) {
                    val type = record.getType()
                    if (type != 2 && type != 3) {
                        if (type == 0 && maximumHeldKeys < 75) {
                            KEY_RECORDS[maximumHeldKeys++] = record
                        }
                    } else {
                        val keyCode = record.getCharacter()
                        if (GameState.isIngame() && (keyCode.code == 96 || keyCode.code == 167 || keyCode.code == 178)) {
                            if (Console.isOpen()) {
                                Console.close()
                            } else {
                                Console.open()
                            }
                        } else if (keyCachePointerPoss < 131) {
                            KEYS_PRESSED[keyCachePointerPoss++] = record
                        }
                    }
                    record = KeyRecorder.keyRecorder!!.popHead()
                }
            }

            mouseWheelDelta = 0

            var record = MouseRecord.recorder!!.nextSubmission()
            while (record != null) {
                val type = record.getClickType()
                if (type == -1) {
                    MouseRecord.record(record)
                } else if (type == 6) {
                    mouseWheelDelta += record.getMeta()
                    record.pushToTop()
                } else if (MouseRecord.clicktypeRequiresRecord(type)) {
                    MOUSE_MOVEMENTS.addFirst(record)
                    if (MOUSE_MOVEMENTS.size() > 10) {
                        val head = MOUSE_MOVEMENTS.popHead() as MouseRecord?
                        if (head != null) {
                            head.pushToTop()
                        }
                    }
                }
                record = MouseRecord.recorder!!.nextSubmission()
            }

            if (Console.isOpen()) {
                Console.processConsoleInput()
            }

            if (GameState.isOnloginScreen(gameState)) {
                LoadingStage.cycle()
                resetDrawCache()
            } else if (GameState.isLoadingRegion(gameState)) {
                Resource.mapRegion!!.loadRegion()
            }


            if (GameState.isLoggedOut(gameState) && !GameState.isLoadingRegion(gameState)) {
                processLobbyTick()
                AccountCreationStage.handleAccountCreationStart()
                LoginManager.loginBlock()
            } else if (GameState.inLobby(gameState) && !GameState.isLoadingRegion(gameState)) {
                processLobbyTick()
                LoginManager.loginBlock()
            } else if (gameState == GameState.UNK_12) {
                LoginManager.loginBlock()
            } else if (GameState.isRenderingScene(gameState) && !GameState.isLoadingRegion(gameState)) {
                processGameTick()
            } else if (gameState == GameState.UNK_10 || gameState == GameState.UNK_17) {
                LoginManager.loginBlock()
                if (StaticMethods.worldLoginResponse != -3 && StaticMethods.worldLoginResponse != 2 && StaticMethods.worldLoginResponse != 15) {
                    if (gameState == GameState.UNK_17) {
                        StaticMethods.lastDisconnectArg2 = LoginManager.loginResponseDetail
                        StaticMethods.lastDisconnectArg1 = LoginManager.loginResponseExtraInfo
                        StaticMethods.lastDisconnectResponse = StaticMethods.worldLoginResponse
                        if (WorldConnection.reconnectingWorld) {
                            LoginManager.setGameHost(WorldConnection.connectionInfo!!.worldId, WorldConnection.connectionInfo!!.host!!)
                            GAME_CONNECTION_CONTEXT.reset()
                            setGameState(GameState.UNK_10)
                        } else {
                            logout(LoginManager.currentlyInLobby)
                        }
                    } else {
                        logout(false)
                    }
                }
            }

            InterfaceComponentRenderer.renderItems(Renderers.activeToolkit)
            MOUSE_MOVEMENTS.popHead()
        }
        Bot.pulse()
    }

    fun redraw() {
        if (gameState != GameState.UNK_16) {
            val long_2: Long = currentCycle / 1000000L - lastRedrawTimeMillis
            lastRedrawTimeMillis = currentCycle / 1000000L
            val bool4 = SoundResourceManager.loadPendingSong()
            if (bool4 && SoundEffectPlayer.soundJinglePlaying && SoundEffectPlayer.jinglePlayer != null) {
                SoundEffectPlayer.jinglePlayer!!.start()
            }

            if (GameState.isWindowUpdate(gameState)) {
                if (WindowMode.nextDimensionUpdateCheck != 0L && Utils.time() > WindowMode.nextDimensionUpdateCheck) {
                    WindowMode.requestWindowMode(WindowMode.getWindowMode(), -1, -1, false)
                } else if (!Renderers.activeToolkit!!.supportsFullscreen() && clippingUpdateNeeded) {
                    WindowMode.exitFullscreen()
                }
            }
            if (WindowMode.fullScreenFrame == null) {
                val activeContainer = WindowMode.getActiveContainer()
                var width = activeContainer.getSize().width
                var height = activeContainer.getSize().height
                if (activeContainer === engineFrame) {
                    val engineFrameInsets = engineFrame!!.getInsets()
                    width -= engineFrameInsets.left + engineFrameInsets.right
                    height -= engineFrameInsets.bottom + engineFrameInsets.top
                }

                if (width != WindowMode.resizableScreenWidth || height != WindowMode.resizableScreenHeight || needsResize) {
                    if (Renderers.activeToolkit != null && !Renderers.activeToolkit!!.forceFixedScreenmode()) {
                        WindowMode.resizableScreenWidth = width
                        WindowMode.resizableScreenHeight = height
                    } else {
                        WindowMode.adjustActiveWindow()
                    }

                    WindowMode.nextDimensionUpdateCheck = Utils.time() + 500L
                    needsResize = false
                }
            }

            if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null && !appletHasFocus && GameState.isWindowUpdate(gameState)) {
                WindowMode.requestWindowMode(PreferenceManager.preferences!!.fixedScreen!!.getValue(), -1, -1, false)
            }

            var drawBackground = false
            if (WindowMode.redrawWindowBackground) {
                WindowMode.redrawWindowBackground = false
                drawBackground = true
            }

            if (drawBackground) {
                WindowMode.fillWindowRect()
            }

            if (Renderers.activeToolkit != null && Renderers.activeToolkit!!.supportsFullscreen() || WindowMode.getWindowMode() != 1) {
                setComponentsOnTop()
            }

            if (GameState.isOnloginScreen(gameState)) {
                LoadingStage.initialize(drawBackground)
            } else if (isInLobbyOrCreation(gameState)) {
                drawInterfaceFrame()
            } else if (isInLobbyConnection(gameState)) {
                drawInterfaceFrame()
            } else if (GameState.isLoadingRegion(gameState)) {
                if (Resource.mapRegion!!.currentLoadingStage == RegionLoadingStage.LOADING_MAPS) {
                    val completion = Resource.mapRegion!!.completion / 2
                    MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE!!) + "<br>" + "(" + completion + "%)", true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
                } else if (Resource.mapRegion!!.currentLoadingStage == RegionLoadingStage.LOADING_LOCS) {
                    val completion = 50 + Resource.mapRegion!!.locLoadingPercent() / 2
                    MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE!!) + "<br>" + "(" + completion + "%)", true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
                } else {
                    MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE!!), true, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
                }
            } else if (gameState == GameState.LOGGED_INGAME) {
                drawGameFrame(long_2)
            } else if (gameState == GameState.UNK_10) {
                MessageBox.draw(LocalizedText.CONNECTION_LOST.translate(CURRENT_LANGUAGE!!) + "<br>" + LocalizedText.ATTEMPTING_TO_REESTABLISH.translate(CURRENT_LANGUAGE!!), false, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
            } else if (gameState == GameState.UNK_17) {
                MessageBox.draw(LocalizedText.PLEASE_WAIT.translate(CURRENT_LANGUAGE!!), false, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
            }

            if (interfaceDebugMode == 3) {
                for (index in 0..<componentBackgroundCount) {
                    val rectangle10: Rectangle? = INTERFACE_BG_COMPONENTS[index]
                    if (PREV_COMPONENT_ON_TOP[index]) {
                        checkNotNull(Renderers.activeToolkit)
                        Renderers.activeToolkit!!.drawOpaqueRectangle(rectangle10!!.x, rectangle10!!.y, rectangle10!!.width, rectangle10!!.height, -65281)
                    } else {
                        checkNotNull(Renderers.activeToolkit)
                        Renderers.activeToolkit!!.drawOpaqueRectangle(rectangle10!!.x, rectangle10!!.y, rectangle10!!.width, rectangle10!!.height, -16711936)
                    }
                }
            }

            if (Console.isOpen()) {
                checkNotNull(Renderers.activeToolkit)
                Console.renderConsole(Renderers.activeToolkit!!)
            }

            if (!GameState.isOnloginScreen(gameState) && !GameState.isLoadingRegion(gameState) && BASE_WINDOW_ID != -1) {
                try {
                    checkNotNull(Renderers.activeToolkit)
                    Renderers.activeToolkit!!.renderFrame()
                } catch (sceneRenderException: FrameRenderException) {
                    PacketEncoder.sendErrorToServer(sceneRenderException.message + " " + getDebugString(), sceneRenderException)
                    loadAssets(0, false)
                }
            }

            VorbisNode.cleanupStaleStreams()
            val cpuUsagePreference = PreferenceManager.preferences!!.cpu!!.getValue()
            if (cpuUsagePreference == 0) {
                TimeTools.sleep(15L)
            } else if (cpuUsagePreference == 1) {
                TimeTools.sleep(10L)
            } else if (cpuUsagePreference == 2) {
                TimeTools.sleep(5L)
            } else if (cpuUsagePreference == 3) {
                TimeTools.sleep(2L)
            }

            if (typelistCycleUpdate) {
                TypeLists.clean()
            }

            if (PreferenceManager.preferences!!.safeMode!!.getValue() == 1 && gameState == GameState.UNK_5 && BASE_WINDOW_ID != -1) {
                PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.safeMode!!, 0)
                PreferenceManager.savePreferences()
            }
        }
    }

    public override fun getDebugString(): String {
        var debug = " "

        try {
            val base = Resource.mapRegion!!.getBaseCoordinates()
            debug = debug + base.x + "," + base.y + "," + Resource.mapRegion!!.sceneSizeX + "," + Resource.mapRegion!!.sceneSizeY + " "
            if (PlayerUpdate.MY_PLAYER != null) {
                debug = debug + PlayerUpdate.MY_PLAYER_PLANE + "," + (PlayerUpdate.MY_PLAYER!!.movementBaseX[0] + base.x) + "," + (PlayerUpdate.MY_PLAYER!!.movementBaseY[0] + base.y) + " "
            } else {
                debug = debug + PlayerUpdate.MY_PLAYER_PLANE + "," + PlayerUpdate.MY_PLAYER_PLANE + "," + PlayerUpdate.MY_PLAYER_PLANE + "," + " "
            }

            debug = (debug + PreferenceManager.preferences!!.activeToolkit!!.getValue() + " " + PreferenceManager.preferences!!.antiAliasing!!.getValue() + " " + WindowMode.getWindowMode() + " " + screenCanvasWidth + "," + screenCanvasHeight * -969250379 + " ")
            debug = debug + PreferenceManager.preferences!!.lightDetail!!.getValue() + " "
            debug = debug + PreferenceManager.preferences!!.sceneryShadows!!.getValue() + " "
            debug = debug + PreferenceManager.preferences!!.water!!.getValue() + " "
            debug = debug + PreferenceManager.preferences!!.textures!!.getValue() + " "
            debug = debug + PreferenceManager.preferences!!.bloom!!.getValue() + " "
            debug += "0 "
            debug = debug + maxMemory + " "
            debug = debug + gameState + " "
            if (SystemInfo.SYSTEM_INFO != null) {
                debug += SystemInfo.SYSTEM_INFO!!.ram
            } else {
                debug += -1
            }

            debug += " "
            if (js5AuthToken != null) {
                debug += js5AuthToken
            } else {
                debug += ","
            }

            try {
                if (PreferenceManager.preferences!!.activeToolkit!!.getValue() == 2) {
                    val classLoader = Class.forName("java.lang.ClassLoader")
                    val nativeLibsField = classLoader.getDeclaredField("nativeLibraries")
                    val accessibleObject = Class.forName("java.lang.reflect.AccessibleObject")
                    val setAccessibleMethod = accessibleObject.getDeclaredMethod("setAccessible", java.lang.Boolean.TYPE)
                    setAccessibleMethod.invoke(nativeLibsField, java.lang.Boolean.TRUE)
                    val objectVector = nativeLibsField.get(Client::class.java.getClassLoader()) as Vector<*>

                    for (index in objectVector.indices) {
                        try {
                            val `object`: Any = objectVector.elementAt(index)
                            val field = `object`.javaClass.getDeclaredField("name")
                            setAccessibleMethod.invoke(field, java.lang.Boolean.TRUE)

                            try {
                                val fieldName = field.get(`object`) as String?
                                if (fieldName != null && fieldName.indexOf("sw3d.dll") != -1) {
                                    val handleField = `object`.javaClass.getDeclaredField("handle")
                                    setAccessibleMethod.invoke(handleField, java.lang.Boolean.TRUE)
                                    debug = debug + " " + java.lang.Long.toHexString(handleField.getLong(`object`))
                                    setAccessibleMethod.invoke(handleField, java.lang.Boolean.FALSE)
                                }
                            } catch (throwable: Throwable) {
                                // NOOP
                            }

                            setAccessibleMethod.invoke(field, java.lang.Boolean.FALSE)
                        } catch (throwable: Throwable) {
                            // NOOP
                        }
                    }
                }
            } catch (throwable: Throwable) {
                // NOOP
            }
        } catch (throwable: Throwable) {
            // NOOP
        }

        return debug
    }

    fun pulseFileRequester() {
        val connected = Resource.remoteFileRequesterJs5!!.pulse()
        if (!connected) {
            setupJs5Connection()
        }
    }

    fun setupJs5Connection() {
        if (Resource.remoteFileRequesterJs5!!.failedRequests > failedRequests) {
            LoginManager.GAME_CONNECTION_INFO!!.swap()
            pulsesBeforeJs5Reconnect = Resource.remoteFileRequesterJs5!!.failedRequests * 250 - 250
            if (pulsesBeforeJs5Reconnect > 3000) {
                pulsesBeforeJs5Reconnect = 3000
            }

            if (Resource.remoteFileRequesterJs5!!.failedRequests >= 2 && Resource.remoteFileRequesterJs5!!.connectionStatus == 6) {
                raiseErrorScreen("js5connect_outofdate")
                gameState = GameState.UNK_16
                return
            }

            if (Resource.remoteFileRequesterJs5!!.failedRequests >= 1 && Resource.remoteFileRequesterJs5!!.connectionStatus == 48) {
                raiseErrorScreen("sessionexpired")
                gameState = GameState.UNK_16
                return
            }

            if (Resource.remoteFileRequesterJs5!!.failedRequests >= 4 && Resource.remoteFileRequesterJs5!!.connectionStatus == -1) {
                raiseErrorScreen("js5crc")
                gameState = GameState.UNK_16
                return
            }

            if (Resource.remoteFileRequesterJs5!!.failedRequests >= 4 && GameState.isOnloginScreen(gameState)) {
                if (Resource.remoteFileRequesterJs5!!.connectionStatus != 7 && Resource.remoteFileRequesterJs5!!.connectionStatus != 9) {
                    if (Resource.remoteFileRequesterJs5!!.connectionStatus > 0) {
                        if (js5ProxyAddress == null) {
                            raiseErrorScreen("js5connect")
                        } else {
                            raiseErrorScreen("js5proxy_" + js5ProxyAddress!!.trim { it <= ' ' })
                        }
                    } else {
                        raiseErrorScreen("js5io" + Resource.remoteFileRequesterJs5!!.connectionStatus)
                    }
                } else {
                    raiseErrorScreen("js5connect_full")
                }

                gameState = GameState.UNK_16
                return
            }
        }

        failedRequests = Resource.remoteFileRequesterJs5!!.failedRequests
        if (pulsesBeforeJs5Reconnect > 0) {
            pulsesBeforeJs5Reconnect--
            return
        }
        try {
            if (updateStage == 0) {
                clientSocket = LoginManager.GAME_CONNECTION_INFO!!.createSocket()
                updateStage++
            }

            if (updateStage == 1) {
                Connection.updateConnection = AsyncConnection.createAsyncConnection(clientSocket!!, 125000)
                if (Bot.PROD) {
                    val length: Int = 13 + js5AuthToken!!.length
                    val stream = Packet(length + 4)
                    stream.writeByte(LoginProt.INIT_JS5REMOTE_CONNECTION.id)
                    stream.writeByte(length)
                    stream.writeInt(6)
                    stream.writeInt(Loader.MAJOR_BUILD)
                    stream.writeInt(Loader.MINOR_BUILD)
                    stream.writeString(js5AuthToken!!)
                    Connection.updateConnection!!.write(stream.buffer, length + 2)
                } else {
                    val length: Int = 9 + js5AuthToken!!.length
                    val stream = Packet(length + 4)
                    stream.writeByte(LoginProt.INIT_JS5REMOTE_CONNECTION.id)
                    stream.writeByte(length)
                    stream.writeInt(Loader.MAJOR_BUILD)
                    stream.writeInt(Loader.MINOR_BUILD)
                    stream.writeString(js5AuthToken!!)
                    Connection.updateConnection!!.write(stream.buffer, length + 2)
                }
                updateStage++
                js5ConnectTime = Utils.time()
            }

            if (updateStage == 2) {
                if (Connection.updateConnection!!.available(1)) {
                    val js5Action = ByteArray(1)
                    val status = Connection.updateConnection!!.read(js5Action, 0, 1)
                    if (js5Action[0].toInt() == 25) {
                        return
                    }

                    if (js5Action[0].toInt() != 0) {
                        updateJs5ConnectionStatus(status)
                        return
                    }

                    updateStage++
                } else if (Utils.time() - js5ConnectTime > 30000L) {
                    updateJs5ConnectionStatus(1001)
                    return
                }
            }

            if (updateStage == 3) {
                val dependencies = CacheableDependencies.getAll()
                val byteCount = dependencies.size * 4
                if (Connection.updateConnection!!.available(byteCount)) {
                    val buffer = Packet(byteCount)
                    Connection.updateConnection!!.read(buffer.buffer, 0, buffer.buffer.size)

                    for (dependency in dependencies) dependency.setArchiveCount(buffer.readInt())

                    val loggedOut = GameState.isOnloginScreen(gameState) || GameState.isLoggedOut(gameState) || GameState.inLobby(gameState)
                    Resource.remoteFileRequesterJs5!!.init(Connection.updateConnection!!, !loggedOut)
                    clientSocket = null
                    Connection.updateConnection = null
                    updateStage = 0
                }
            }
        } catch (ioe: IOException) {
            ioe.printStackTrace()
            updateJs5ConnectionStatus(1002)
        }
    }

    fun updateJs5ConnectionStatus(connectionStatus: Int) {
        clientSocket = null
        Connection.updateConnection = null
        updateStage = 0
        Resource.remoteFileRequesterJs5!!.failedRequests++
        Resource.remoteFileRequesterJs5!!.connectionStatus = connectionStatus
    }

    public override fun mainRedraw() {
        if (PreferenceManager.preferences!!.activeToolkit!!.getValue() == 2) {
            try {
                redraw()
            } catch (threadDeath: ThreadDeath) {
                throw threadDeath
            } catch (throwable: Throwable) {
                PacketEncoder.sendErrorToServer(throwable.message + " " + getDebugString(), throwable)
                renderWithCpu = true
                loadAssets(0, false)
            }
        } else {
            redraw()
        }
    }

    public override fun shutdown() {
        if (clientIsActive) {
            PacketEncoder.writeVarcPreferences()
        }

        LoadingStage.killLoginscreenThread()
        if (Renderers.activeToolkit != null) {
            Renderers.activeToolkit!!.cleanupAndShutdown()
        }

        if (WindowMode.fullScreenSupported && WindowMode.fullScreenFrame != null) {
            WindowMode.disposeFrame(WindowMode.activeWindowMode!!, WindowMode.fullScreenFrame!!)
            WindowMode.fullScreenFrame = null
        }

        GAME_CONNECTION_CONTEXT.closeConnection()
        GAME_CONNECTION_CONTEXT.pinger.disable()
        LOBBY_CONNECTION_CONTEXT.closeConnection()
        LOBBY_CONNECTION_CONTEXT.pinger.disable()
        SoundEffectPlayer.shutdownPlayers()
        Resource.remoteFileRequesterJs5!!.closeStream()
        Resource.localRequesterJs5!!.shutdown()
        if (PING_REQUESTER != null) {
            PING_REQUESTER!!.shutdown()
            PING_REQUESTER = null
        }

        try {
            Ping.quit()
        } catch (ignored: Throwable) {
            // NOOP
        }

        shutdownIcmpservice()

        try {
            Console.shutdown()
        } catch (ignored: Exception) {
            // NOOP
        }

        if (raiseBrowserErrorscreen) {
            Browser.openBrowserErrorscreenOpenjs()
        }
    }

    @Suppress("deprecation")
    public override fun initializeEssentials() {
        val frame = Frame(" ")
        frame.pack()
        frame.dispose()
        frameComponent = Component()
        WindowMode.adjustActiveWindow()
        Resource.localRequesterJs5 = LocalRequester()
        Resource.remoteFileRequesterJs5 = RemoteFileRequester()
        CircularBuffer.initialize(intArrayOf(20, 260), intArrayOf(1000, 100))
        Vector3Pool.initVectorStack()
        QuaternionPool.initialize()
        MouseActionClient.initialize()
        MouseActionServer.initialize()
        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
            TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA = arrayOfNulls<ByteArray>(50)
        }

        PreferenceManager.preferences = PreferenceManager.getGamePreferences()
        if (PreferenceManager.preferences!!.asyncMaploading!!.getValue() == 1) {
            Scene.synchronousMaploading = false
        }

        if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LIVE) {
            WorldConnection.GAME_CONNECTION!!.host = suppliedApplet!!.getCodeBase()!!.getHost()
        } else if (ServerEnvironment.isHostedTestEnvironment(ServerEnvironment.SERVER_ENVIRONMENT!!)) {
            WorldConnection.GAME_CONNECTION!!.host = suppliedApplet!!.getCodeBase()!!.getHost()
            WorldConnection.GAME_CONNECTION!!.devWorld1 = WorldConnection.GAME_CONNECTION!!.worldId * -1708079975 + 1140744768
            WorldConnection.LOBBY_CONNECTION_INFO!!.devWorld1 = WorldConnection.LOBBY_CONNECTION_INFO!!.worldId * -1708079975 + 1140744768
            WorldConnection.GAME_CONNECTION!!.devWorld2 = WorldConnection.GAME_CONNECTION!!.worldId * -1473668237 + 1047080176
            WorldConnection.LOBBY_CONNECTION_INFO!!.devWorld2 = WorldConnection.LOBBY_CONNECTION_INFO!!.worldId * -1473668237 + 1047080176
        } else if (ServerEnvironment.SERVER_ENVIRONMENT == ServerEnvironment.LOCAL) {
            WorldConnection.GAME_CONNECTION!!.host = "127.0.0.1"
            WorldConnection.LOBBY_CONNECTION_INFO!!.host = "127.0.0.1"
            WorldConnection.GAME_CONNECTION!!.devWorld1 = WorldConnection.GAME_CONNECTION!!.worldId * -1708079975 + 1140744768
            WorldConnection.LOBBY_CONNECTION_INFO!!.devWorld1 = WorldConnection.LOBBY_CONNECTION_INFO!!.worldId * -1708079975 + 1140744768
            WorldConnection.GAME_CONNECTION!!.devWorld2 = WorldConnection.GAME_CONNECTION!!.worldId * -1473668237 + 1047080176
            WorldConnection.LOBBY_CONNECTION_INFO!!.devWorld2 = WorldConnection.LOBBY_CONNECTION_INFO!!.worldId * -1473668237 + 1047080176
        }

        LoginManager.GAME_CONNECTION_INFO = WorldConnection.GAME_CONNECTION
        if (Game.RUNESCAPE == CURRENT_GAME) {
            playingAltGame = false
        }

        ItemType.recolorPalette = ShortArray(256)
        NPCType.npcRecolorPalette = ItemType.recolorPalette
        ObjectType.locRecolorPalette = NPCType.npcRecolorPalette
        recolorPalette = ObjectType.locRecolorPalette

        try {
            Console.clipboard = WindowMode.getActiveContainer().getToolkit().getSystemClipboard()
        } catch (exception: Exception) {
            // NOOP
        }

        KeyRecorder.keyRecorder = CanvasKeyRecorder.create(gameCanvas!!)
        MouseRecord.recorder = AbstractMouseManager.inializeMouseRecorder(gameCanvas!!)
        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
            DRAW_DEBUG = true
        }

        translatedLoadingText = LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE!!)
        Resource.mapRegionLoaderTask = AsyncMapLoader()
        (Thread(Resource.mapRegionLoaderTask)).start()
    }

    fun processLobbyTick() {
        if (gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress() || gameState == GameState.UNK_7 && StaticMethods.worldLoginResponse == 42 || gameState == GameState.IN_ACCOUNT_CREATION) {
            if (REBOOT_TIMER > 1) {
                REBOOT_TIMER--
                lastMiscUpdateTick = serverTick
            }

            if (!MiniMenu.isOpen) {
                MiniMenu.resetEntries()
            }

            var packetLoop = 0
            while (packetLoop < 100 && PacketDecoder.decodePacketFromStream(LOBBY_CONNECTION_CONTEXT)) {
                packetLoop++
            }
        }

        PulseEventHandler.processPulseEvents()
        MouseActionServer.refreshCache()
        animatingStep++
        MiniMenu.setViewportOrigin(-1, -1)
        InterfaceComponentRenderer.setChatDisplay(null, -1, -1)
        Interface.ifHover()
        serverTick++

        for (index in 0..<npcCount) {
            val npc = NPC_ARRAY[index]!!.value as NpcEntity?
            if (npc != null) {
                val walkMask = npc.definitions!!.walkingFlags
                if ((walkMask.toInt() and 0x1) != 0) {
                    val size = npc.getSize()
                    if ((walkMask.toInt() and 0x2) != 0 && npc.movementStepsLeft == 0 && Math.random() * 1000.0 < 10.0) {
                        val random1 = Math.round(Math.random() * 10.0 - 5.0).toInt()
                        val random2 = Math.round(Math.random() * 10.0 - 5.0).toInt()
                        if (random1 != 0 || random2 != 0) {
                            var x = random1 + npc.movementBaseX[0]
                            var y = random2 + npc.movementBaseY[0]
                            if (x < 0) {
                                x = 0
                            } else if (x > Resource.mapRegion!!.sceneSizeX - size - 1) {
                                x = Resource.mapRegion!!.sceneSizeX - size - 1
                            }

                            if (y < 0) {
                                y = 0
                            } else if (y > Resource.mapRegion!!.sceneSizeY - size - 1) {
                                y = Resource.mapRegion!!.sceneSizeY - size - 1
                            }

                            var numSteps = RouteFinder.findRoute(
                                npc.movementBaseX[0], npc.movementBaseY[0], size, RouteStrategies.forNpc(x, y, size, size), Resource.mapRegion!!.getCollisionMap(npc.plane.toInt()), true, WALKING_STEPS_X, WALKING_STEPS_Y
                            )
                            if (numSteps > 0) {
                                if (numSteps > 9) {
                                    numSteps = 9
                                }

                                for (step in 0..<numSteps) {
                                    npc.movementBaseX[step] = WALKING_STEPS_X[numSteps - step - 1]
                                    npc.movementBaseY[step] = WALKING_STEPS_Y[numSteps - step - 1]
                                    npc.movementTypes[step] = MovementType.WALKING.id
                                }

                                npc.movementStepsLeft = numSteps
                            }
                        }
                    }

                    EntityUpdating.updatePosition(npc, true)
                    val rotation = EntityUpdating.turnTo(npc)
                    EntityUpdating.rotateEquipment(npc)
                    PathingEntity.animateMovement(npc, EntityUpdating.currentMovementType, EntityUpdating.turnDirectionIndex, rotation)
                    PlayerUpdate.animateMovement(npc, EntityUpdating.currentMovementType.toByte())
                    PathingEntity.animateMovement(npc)
                    val quaternion = QuaternionPool.newQuaternion()
                    quaternion.rotateEuler(MathUtils.getTheta(npc.yaw.getRotation()), MathUtils.getTheta(npc.pitch.getRotation()), MathUtils.getTheta(npc.roll.getRotation()))
                    npc.rotationUpdate(quaternion)
                    quaternion.store()
                }
            }
        }

        if ((gameState == GameState.UNK_5 || gameState == GameState.UNK_7 || gameState == GameState.UNK_0 || gameState == GameState.IN_ACCOUNT_CREATION) && (!LoginManager.isLoginInProgress() || gameState == GameState.UNK_7 && StaticMethods.worldLoginResponse == 42) && !AccountCreationStage.isCreatingAccount()) {
            if (Camera.cameraCalculationType == 5) {
                Camera.calculateCameraPosition()
            } else {
                Camera.calculateCutsceneCameraPosition()
            }

            if (Camera.cameraPositionAbsoluteX shr 9 < 14 || Camera.cameraPositionAbsoluteX shr 9 >= Resource.mapRegion!!.sceneSizeX - 14 || Camera.cameraPositionAbsoluteY shr 9 < 14 || Camera.cameraPositionAbsoluteY shr 9 >= Resource.mapRegion!!.sceneSizeY - 14) {
                Resource.mapRegion!!.initialize(RegionLoadRequest(RegionLoadingType.ANIMATED_LOGIN_SCREEN, null))
            }
        }

        while (true) {
            var hookRequest: HookRequest?
            var parentComponent: Component?
            var srcComponent: Component?
            do {
                hookRequest = TICK_HOOK_REQUESTS.popHead() as HookRequest?
                if (hookRequest == null) {
                    while (true) {
                        do {
                            hookRequest = MOUSE_HOOK_REQUESTS.popHead() as HookRequest?
                            if (hookRequest == null) {
                                while (true) {
                                    do {
                                        hookRequest = PENDING_HOOK_REQUESTS.popHead() as HookRequest?
                                        if (hookRequest == null) {
                                            if (draggingComponent != null) {
                                                processDrag()
                                            }

                                            if (gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress() || gameState == GameState.UNK_7 && StaticMethods.worldLoginResponse == 42) {
                                                MenuInteraction.processMenuInput()
                                            }

                                            WorldMeta.pingWorlds()
                                            if (clientIsActive && exitTime < Utils.time() - 60000L) {
                                                PacketEncoder.writeVarcPreferences()
                                            }

                                            var loginStateUpdate = PLAYER_LOGINSTATE_UPDATES.getFirst() as LoginStateUpdate?
                                            while (loginStateUpdate != null) {
                                                if (loginStateUpdate.time < Utils.time() / 1000L - 5L) {
                                                    if (loginStateUpdate.actionType > 0) {
                                                        ChatBox.writeMessage(ChatMessageType.FRIENDS_STATUS, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_IN.translate(CURRENT_LANGUAGE!!))
                                                    }

                                                    if (loginStateUpdate.actionType.toInt() == 0) {
                                                        ChatBox.writeMessage(ChatMessageType.FRIENDS_STATUS, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_OUT.translate(CURRENT_LANGUAGE!!))
                                                    }

                                                    loginStateUpdate.unlinkNode()
                                                }
                                                loginStateUpdate = PLAYER_LOGINSTATE_UPDATES.getNext() as LoginStateUpdate?
                                            }

                                            if (gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress() || gameState == GameState.UNK_7 && StaticMethods.worldLoginResponse == 42 || gameState == GameState.IN_ACCOUNT_CREATION) {
                                                if (gameState != GameState.UNK_7 && LOBBY_CONNECTION_CONTEXT.getConnection() == null) {
                                                    logout(false)
                                                } else {
                                                    if (++LOBBY_CONNECTION_CONTEXT.lastFlush > 50) {
                                                        val ping = PacketEncoder.createPacket(ClientProt.PING, LOBBY_CONNECTION_CONTEXT.isaac)
                                                        LOBBY_CONNECTION_CONTEXT.queuePacket(ping)
                                                    }

                                                    try {
                                                        LOBBY_CONNECTION_CONTEXT.flush()
                                                    } catch (ioexception: IOException) {
                                                        if (gameState == GameState.UNK_7) {
                                                            LOBBY_CONNECTION_CONTEXT.closeConnection()
                                                        } else {
                                                            logout(false)
                                                        }
                                                    }
                                                }
                                            }

                                            return
                                        }

                                        srcComponent = hookRequest.source
                                        if (srcComponent!!.slotId < 0) {
                                            break
                                        }

                                        parentComponent = Component.fromHash(srcComponent.parentId)
                                    } while (parentComponent == null || parentComponent.slotChildren == null || srcComponent.slotId >= parentComponent.slotChildren!!.size || srcComponent != parentComponent.slotChildren!![srcComponent.slotId])

                                    CS2Executor.executeHookInner(hookRequest)
                                }
                            }

                            srcComponent = hookRequest.source
                            if (srcComponent!!.slotId < 0) {
                                break
                            }

                            parentComponent = Component.fromHash(srcComponent.parentId)
                        } while (parentComponent == null || parentComponent.slotChildren == null || srcComponent.slotId >= parentComponent.slotChildren!!.size || srcComponent != parentComponent.slotChildren!![srcComponent.slotId])

                        CS2Executor.executeHookInner(hookRequest)
                    }
                }

                srcComponent = hookRequest.source
                if (srcComponent!!.slotId < 0) {
                    break
                }

                parentComponent = Component.fromHash(srcComponent.parentId)
            } while (parentComponent == null || parentComponent.slotChildren == null || srcComponent.slotId >= parentComponent.slotChildren!!.size || srcComponent != parentComponent.slotChildren!![srcComponent.slotId])

            CS2Executor.executeHookInner(hookRequest)
        }
    }

    companion object {
        val HINT_TRAILS: Array<HintTrail?> = arrayOfNulls<HintTrail>(9)
        val MOUSE_MOVEMENTS: NodeCollection = NodeCollection()
        val KEYS_PRESSED: Array<KeyRecord?> = arrayOfNulls<KeyRecord>(131)
        val KEY_RECORDS: Array<KeyRecord?> = arrayOfNulls<KeyRecord>(75)
        val DEBUG_LOAD_COLOR: Array<Color?> = arrayOf<Color?>(Color(9179409), Color(3289650), Color(3289650), Color(3289650))
        val DEBUG_BG_COLOR: Array<Color?> = arrayOf<Color?>(Color(9179409), Color(16777215), Color(16726277), Color(16726277))
        val DEBUG_TEXT_COLOR: Array<Color?> = arrayOf<Color?>(Color(16777215), Color(16777215), Color(16741381), Color(16741381))
        @JvmField
        val FONT_PROVIDER: FontProvider = BasicFontProvider()
        val NPC_MAP: IterableNodeMap<ObjectNode> = IterableNodeMap<ObjectNode>(64)
        val NPC_ARRAY: Array<ObjectNode?> = arrayOfNulls<ObjectNode>(1024)
        val NPC_UPDATE_INDICES: IntArray = IntArray(1024)
        @JvmField
        val LOBBY_CONNECTION_CONTEXT: BufferedConnectionContext = BufferedConnectionContext()
        @JvmField
        val GAME_CONNECTION_CONTEXT: BufferedConnectionContext = BufferedConnectionContext()
        val connectionContexts: Array<BufferedConnectionContext>
        val MAP_REGION_LOADER_LOCK: Any
        val ASYNC_MAPLOADING_LOCK: Any
        val npcSpawnMaps: IntArray
        val OBJECT_TYPE_SLOTS: IntArray
        @JvmField
        val cameraBaseZoom: Int
        @JvmField
        val cameraSplinePaths: Array<Array<IntArray?>?>
        @JvmField
        val HINT_POSITION: FloatArray
        val GROUND_ITEMS: IterableNodeMap<GroundItemWrapper>
        val PROJECTILES: NodeCollection
        val ACTIVE_GFX: IterableNodeMap<PositionedGfx>
        val TILE_MESSAGES: EntityNodeList<TileMessage>
        @JvmField
        val SKILL_LEVEL_VISIBLE: IntArray
        @JvmField
        val SKILL_LEVEL_ACTUAL: IntArray
        @JvmField
        val SKILL_XP: IntArray
        @JvmField
        val PENDING_HOOK_REQUESTS: NodeCollection
        val IS_COMPONENT_ON_TOP: BooleanArray
        @JvmField
        val GRAND_EXCHANGE_SLOTS: Array<GrandExchangeSlot?>
        val INTERACTIVE_OBJECT_COMPARABLE: ComparableGraphNode
        val DUMMY: LRUCache = LRUCache(4)
        val SUN_MATRIX: Matrix43 = Matrix43()
        val INTERFACE_MODEL_MATRIX: Matrix43 = Matrix43()
        val clientRandomSeed: Long = (Math.random() * 9.999999999E9).toLong()
        val OVERHEAD_SPRITES: LRUCache = LRUCache(8)
        val NPC_INDICES: IntArray = IntArray(261)
        val LOCAL_NPC_INDICES: IntArray = IntArray(1005)
        val bigX: IntArray
        val bigY: IntArray
        val WALKING_STEPS_X: IntArray
        val WALKING_STEPS_Y: IntArray
        val NPC_TURN_DIRECTIONS: IntArray
        val RANDOMIZER: Random
        val ENTITY_INDEX_REFERENCES: IntArray
        val NPC_INDEX_REFERENCES: IntArray
        val ENTITY_OVERHEADS: EntityNodeList<EntityOverhead>
        val BORDER_WIDTH: Int
        val BORDER_HEIGHT: Int
        val PLAYER_OP_BASE_TYPES: ShortArray
        val PLAYER_OPTION_CURSORS: IntArray
        val PLAYER_OPTIONS: Array<String?>
        val PLAYER_OPTION_REDUCED_PRIORITY: BooleanArray
        val DISPLAY_HIDDEN_MENU_OPTIONS: Boolean
        val prefixMinimenuOps: Boolean
        val CLIP_INTERFACES: Boolean
        val PULSE_EVENT_CONFIGS_CACHE: IntArray
        val RECENTLY_USED_ITEM_CONTAINERS: IntArray
        val recentSkillUpdates: IntArray
        val recentKeyUpdates: IntArray
        val recentMouseWheels: IntArray
        val CLAN_VAR_KEYS: IntArray
        val TICK_HOOK_REQUESTS: NodeCollection
        val MOUSE_HOOK_REQUESTS: NodeCollection
        val ICOMPONENT_SETTINGS_SLOTS: IterableNodeMap<IFEvents>
        val PREV_COMPONENT_ON_TOP: BooleanArray
        val INTERFACE_BG_COMPONENTS: Array<Rectangle?>
        val interfaceDebugMode: Int
        val ANTI_MACRO_TEXT_BOUNDS: IntArray
        val MAX_HINTS: Int
        val HINT_X: IntArray
        val HINT_Y: IntArray
        val HINT_WIDTH: IntArray
        val HINT_DETAILS: Array<HintDetails?>
        val OVERHEAD_TEXT_COLORS: IntArray
        val CHAT_MESSAGE_UNIQUE_HASHES: LongArray
        val CAM_SHAKING: BooleanArray
        val CAM_SHAKE_JITTER: IntArray
        val CUSTOM_LOWEST_YAW: IntArray
        val CAM_SHAKE_FREQUENCY: IntArray
        val CAM_SHAKE_PHASE: IntArray
        val PLAYER_LOGINSTATE_UPDATES: EntityNodeList<LoginStateUpdate>
        val ACTIVE_TIMEZONE_CALENDER: Calendar?
        val HITSPLAT_SIZE_CACHE: IntArray
        @JvmField
        var CURRENT_GAME: Game? = null
        @JvmField
        var isApplet: Boolean = false
        @JvmField
        var AFFILIATE: Int = 0
        var DEBUG_DISPLAY_COLOR_INDEX: Int = 0
        @JvmField
        var billingFlag: Boolean = false
        @JvmField
        var base64Url: ByteArray? = null
        @JvmField
        var machineUid: Long = 0
        @JvmField
        var isForcedSafemode: Boolean = false
        @JvmField
        var safemodeForcedDuringStartup: Boolean = false
        var loginScreenImageResourceNames: String? = null
        @JvmField
        var useBrowserAppletMode: Boolean = false
        var suppressTutorials: Boolean = false
        @JvmField
        var ENTITY_TARGET: Int = 0
        @JvmField
        var customStringParam: String? = null
        @JvmField
        var gameState: GameState = GameState.UNK_4
        @JvmField
        var renderWithCpu: Boolean = false
        var TEST_ENVIRONMENT_APPEARANCE_OVERRIDES: Int = 0
        @JvmField
        var cycles: Int = 0
        @JvmField
        var preferencesSavedOnServer: Boolean = true
        @JvmField
        var REBOOT_TIMER: Int = 0
        var typelistCycleUpdate: Boolean = false
        @JvmField
        var needsResize: Boolean = false
        var sceneDummyOp: Int = 0
        var jagtheoraLoaded: Boolean = false
        @JvmField
        var hasJustCreatedAccount: Boolean = false
        var keyCachePointerPoss: Int = 0
        var maximumHeldKeys: Int = 0
        var npcCount: Int = 0
        var NPC_UPDATE_INDEX: Int = 0
        var PING_REQUESTER: PingRequester?

        @Volatile
        var maploadingLockTaken: Boolean
        var npcSpawnCursorPos: Int
        var CURRENT_CUTSCENE: Int
        var MAP_LOADING_STAGE: Int
        var cutsceneCameraActive: Boolean
        @JvmField
        var cameraTargetAbsX: Int
        @JvmField
        var cameraTargetAbsY: Int
        @JvmField
        var cameraMaxZoom: Int
        var sendCameraAngleToServer: Boolean
        @JvmField
        var cameraPositionPathIndex: Int
        @JvmField
        var cameraLookAtPathIndex: Int
        @JvmField
        var cameraPositionKeyframe: Int
        @JvmField
        var cameraLookAtKeyframe: Int
        @JvmField
        var cameraSplineProgress: Int
        @JvmField
        var cameraSplineSpeedStart: Int
        @JvmField
        var cameraSplineSpeedEnd: Int
        var timesSceneRendered: Int
        var GAME_WIDTH: Int
        var GAME_HEIGHT: Int
        @JvmField
        var IS_MEMBER: Boolean
        @JvmField
        var memberWorld: Boolean
        @JvmField
        var PLAYER_RIGHTS: Int
        @JvmField
        var PLAYER_MOD_LEVEL: Int
        @JvmField
        var USERDETAIL_QUICKCHAT: Boolean
        @JvmField
        var VERIFIED_EMAIL_ADDRESS: Boolean
        @JvmField
        var hasDisplayNamePending: Boolean
        @JvmField
        var IS_QUICKCHAT_ONLY: Boolean
        @JvmField
        var CURRENT_CURSOR: Int
        @JvmField
        var interfaceSelectionActive: Boolean
        @JvmField
        var useOnType: String?
        @JvmField
        var useOnTarget: String?

        @JvmField
        @Volatile
        var BASE_WINDOW_ID: Int
        @JvmField
        var OPEN_INTERFACES: IterableNodeMap<SubInterface>?
        @JvmField
        var activeDialogueContinue: Component?
        @JvmField
        var RUN_ENERGY: Int
        @JvmField
        var RUN_WEIGHT: Int
        @JvmField
        var GAME_SCREEN_INTERFACE: Component?
        var serverTick: Int
        var lastChatMessageTick: Int
        var VARCS_BOOLS_LOADED: Int
        @JvmField
        var clientIsActive: Boolean
        @JvmField
        var viewportFovMin: Short
        @JvmField
        var viewportFovMax: Short
        @JvmField
        var cameraZoomMin: Short
        @JvmField
        var cameraZoomMax: Short
        @JvmField
        var fovScaleMin: Short
        @JvmField
        var fovScaleMax: Short
        @JvmField
        var aspectFovMin: Short
        @JvmField
        var aspectFovMax: Short
        @JvmField
        var gameAreaWidth: Int
        @JvmField
        var gameAreaHeight: Int
        @JvmField
        var displayNameChangeState: Byte
        var showErrorOnFailedZap: Boolean = false
        var affiliateP: String? = null
        var supplementaryLoginData: String? = null
        var hasSignedTermsAgreement: Boolean = false
        var addGamescreenPadding: Boolean = false
        var sessionToken: Int = -1
        var js5AuthToken: String? = null
        var lastRedrawTimeMillis: Long = 0
        var awaitingPriorityRequests: Boolean = true
        var DRAW_DEBUG: Boolean = false
        var DEBUG_MODE: Boolean = false
        var hideNpcs: Boolean = false
        var playingAltGame: Boolean = true
        var drawsSinceLastPacket: Int = 0
        var mouseWheelDelta: Int = 0
        var updateStage: Int = 0
        var pulsesBeforeJs5Reconnect: Int = 0
        var failedRequests: Int = 0
        var js5ProxyAddress: String? = null
        var npcListSize: Int = 0
        var npcUpdateCycle: Int = 0
        var localNpcCount: Int = 0
        var varVerifyId: Int
        var sendTransmitVarVerifyid: Boolean
        var cycleCutsceneLoaded: Int
        var CUTSCENE_MISC_ACTION_INDEX: Int
        var cameraRandomizationCycle: Int
        var minimapRotation: Int
        var minimapRotationStep: Int
        var minimapZoom: Int
        var minimapZoomStep: Int
        var minimapRandomizationCycle: Int
        var animatingStep: Int
        var cameraYawVelocity: Float
        var cameraPitchVelocity: Float
        var cameraPitchVelocityApplied: Boolean
        var cameraYawVelocityApplied: Boolean
        var cutsceneCameraCompleted: Boolean
        var cutsceneCameraPreviouslyCompleted: Boolean
        var overlappingEntityCount: Int
        var prioritizePlayerDraw: Boolean
        var crossX: Int
        var crossY: Int
        var crossSpritePtr: Int
        var crossType: Int
        var menuItemHoverCycle: Int
        var ROOF_OVERRIDE: Int
        var visibleBelowDrawheight: IntArray?
        var visibleAboveX: IntArray?
        var visibleBelowX: IntArray?
        var visibleBelowY: IntArray?
        var visibleAboveY: IntArray?
        var hasAccountSecurity: Boolean
        var REDUCE_ATTACK_PRIORITY: Boolean
        var menuOpenMode: Int
        var defaultMenuCursor: Int
        var selectedSlotId: Int
        var selectedItemId: Int
        var overheadChatFilter: Int
        var draggingComponent: Component?
        var selectedComponent: Component?
        var mouseX: Int
        var mouseY: Int
        var hoveredComponent: Component?
        var isComponentSelected: Boolean
        var selectedComponentX: Int
        var selectedComponentY: Int
        var selectedComponentWidth: Int
        var selectedComponentHeight: Int
        var isDraggingComponent: Boolean
        var dragStartX: Int
        var dragStartY: Int
        var isDragThresholdMet: Boolean
        var menuItemCount: Int
        var isMapMoving: Boolean
        var pulseEventConfigsPtr: Int
        var RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS: Int
        var skillUpdateCycle: Int
        var keyCycle: Int
        var mouseWheelCycle: Int
        var CLAN_VAR_COUNTER: Int
        var lastFriendIgnoreUpdateTick: Int
        var lastFriendsChatUpdateTick: Int
        var lastClanSettingsUpdateTick: Int
        var lastClanChannelUpdateTick: Int
        var lastGrandExchangeUpdateTick: Int
        var lastMiscUpdateTick: Int
        var exitTime: Long
        var componentBackgroundCount: Int
        var lastRenderedCycle: Int
        var currentHintPtr: Int
        var chatMessageHashIndex: Int
        var lastCameraSmoothCycle: Int
        var hintArrowOffsetX: Int
        var hintArrowOffsetY: Int
        var viewportScaledFov: Int
        var selectedComponentHash: Int = 0
        var requestManager: RequestManager? = null
        @JvmField
        var CURRENT_LANGUAGE: Language? = null
        var BUILD_ENVIRONMENT: BuildType? = null
        var js5ConnectTime: Long = 0
        var clientSocket: Socket? = null

        init {
            connectionContexts = arrayOf<BufferedConnectionContext>(GAME_CONNECTION_CONTEXT, LOBBY_CONNECTION_CONTEXT)
            varVerifyId = 0
            sendTransmitVarVerifyid = false
            PING_REQUESTER = PingRequester()
            Resource.mapRegion = MapRegion(false)
            maploadingLockTaken = false
            MAP_REGION_LOADER_LOCK = Any()
            ASYNC_MAPLOADING_LOCK = Any()
            npcSpawnMaps = IntArray(64)
            npcSpawnCursorPos = 0
            OBJECT_TYPE_SLOTS = intArrayOf(0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3)
            CURRENT_CUTSCENE = -1
            MAP_LOADING_STAGE = 1
            cutsceneCameraActive = false
            cycleCutsceneLoaded = -1
            CUTSCENE_MISC_ACTION_INDEX = 0
            InterfaceComponentRenderer.cycleCutsceneFadeStarted = -1
            InterfaceComponentRenderer.cutsceneFadeEndCycle = -1
            InterfaceComponentRenderer.cutsceneFadeAlpha = 255
            InterfaceComponentRenderer.cutsceneFadeRed = 0
            InterfaceComponentRenderer.cutsceneFadeGreen = 0
            InterfaceComponentRenderer.cutsceneFadeBlue = 0
            bigX = IntArray(4096)
            bigY = IntArray(4096)
            WALKING_STEPS_X = IntArray(50)
            WALKING_STEPS_Y = IntArray(50)
            NPC_TURN_DIRECTIONS = intArrayOf(-1, 8192, 0, -1, 12288, 10240, 14336, -1, 4096, 6144, 2048)
            Camera.cameraRandomisationH = 0
            Camera.cameraStepH = 2
            Camera.cameraRandomisationV = 0
            Camera.cameraStepV = 2
            Camera.cameraRandomisationA = 0
            Camera.cameraStepA = 1
            cameraRandomizationCycle = 0
            minimapRotation = 0
            minimapRotationStep = 2
            minimapZoom = 0
            minimapZoomStep = 1
            minimapRandomizationCycle = 0
            RANDOMIZER = Random()
            animatingStep = 0
            cameraTargetAbsX = 0
            cameraTargetAbsY = 0
            Camera.camAngleX = 1052.0f
            Camera.camAngleY = 0.0f
            cameraYawVelocity = 0.0f
            cameraPitchVelocity = 0.0f
            cameraPitchVelocityApplied = false
            cameraYawVelocityApplied = false
            cameraBaseZoom = 46
            cameraMaxZoom = cameraBaseZoom + 800
            sendCameraAngleToServer = true
            Camera.regularLowestYaw = 0
            cameraSplinePaths = arrayOfNulls<Array<IntArray?>>(2)
            cameraPositionPathIndex = -1
            cameraLookAtPathIndex = -1
            cameraPositionKeyframe = 0
            cameraLookAtKeyframe = 0
            cameraSplineProgress = 0
            cameraSplineSpeedStart = 0
            cameraSplineSpeedEnd = 0
            cutsceneCameraCompleted = false
            cutsceneCameraPreviouslyCompleted = false
            timesSceneRendered = 0
            ENTITY_INDEX_REFERENCES = IntArray(508)
            NPC_INDEX_REFERENCES = IntArray(502)
            overlappingEntityCount = 0
            ENTITY_OVERHEADS = EntityNodeList<EntityOverhead>()
            prioritizePlayerDraw = false
            HINT_POSITION = FloatArray(3)
            crossX = 0
            crossY = 0
            crossSpritePtr = 0
            crossType = 0
            menuItemHoverCycle = 0
            ROOF_OVERRIDE = -1
            visibleBelowDrawheight = IntArray(2)
            visibleAboveX = IntArray(2)
            visibleBelowX = IntArray(2)
            visibleBelowY = IntArray(2)
            visibleAboveY = IntArray(2)
            WindowMode.nextDimensionUpdateCheck = 0L
            WindowMode.fullScreenWindowMode = true
            BORDER_WIDTH = 32
            BORDER_HEIGHT = 16
            GAME_WIDTH = 765
            GAME_HEIGHT = 553
            PlayerUpdate.players = arrayOfNulls<PlayerEntity>(2048)
            PlayerUpdate.myPlayerIndex = -1
            IS_MEMBER = false
            memberWorld = false
            PLAYER_RIGHTS = 0
            PLAYER_MOD_LEVEL = 0
            USERDETAIL_QUICKCHAT = false
            VERIFIED_EMAIL_ADDRESS = false
            hasAccountSecurity = false
            hasDisplayNamePending = false
            REDUCE_ATTACK_PRIORITY = true
            IS_QUICKCHAT_ONLY = false
            PLAYER_OP_BASE_TYPES = shortArrayOf(44.toShort(), 45.toShort(), 46.toShort(), 47.toShort(), 48.toShort(), 49.toShort(), 50.toShort(), 51.toShort())
            PLAYER_OPTION_CURSORS = IntArray(8)
            PLAYER_OPTIONS = arrayOfNulls<String>(8)
            PLAYER_OPTION_REDUCED_PRIORITY = BooleanArray(8)
            CursorManager.activeCursorId = -1
            GROUND_ITEMS = IterableNodeMap<GroundItemWrapper>(64)
            PROJECTILES = NodeCollection()
            ACTIVE_GFX = IterableNodeMap<PositionedGfx>(16)
            TILE_MESSAGES = EntityNodeList<TileMessage>()
            SKILL_LEVEL_VISIBLE = IntArray(25)
            SKILL_LEVEL_ACTUAL = IntArray(25)
            SKILL_XP = IntArray(25)
            menuOpenMode = 0
            CursorManager.defaultCursorId = -1
            defaultMenuCursor = -1
            CursorManager.defaultAttackCursor = -1
            CURRENT_CURSOR = -1
            interfaceSelectionActive = false
            selectedSlotId = -1
            selectedItemId = -1
            useOnType = null
            useOnTarget = null
            BASE_WINDOW_ID = -1
            OPEN_INTERFACES = IterableNodeMap<SubInterface>(8)
            overheadChatFilter = 0
            activeDialogueContinue = null
            RUN_ENERGY = 0
            RUN_WEIGHT = 0
            DISPLAY_HIDDEN_MENU_OPTIONS = false
            prefixMinimenuOps = false
            GAME_SCREEN_INTERFACE = null
            CLIP_INTERFACES = false
            draggingComponent = null
            selectedComponent = null
            mouseX = 0
            mouseY = 0
            hoveredComponent = null
            isComponentSelected = false
            selectedComponentX = -1
            selectedComponentY = -1
            selectedComponentWidth = -1
            selectedComponentHeight = -1
            isDraggingComponent = false
            dragStartX = -1
            dragStartY = -1
            isDragThresholdMet = false
            menuItemCount = 0
            isMapMoving = false
            CommandExecutor.teleportCommandRequested = false
            serverTick = 1
            PULSE_EVENT_CONFIGS_CACHE = IntArray(32)
            pulseEventConfigsPtr = 0
            RECENTLY_USED_ITEM_CONTAINERS = IntArray(32)
            RECENTLY_USED_ITEM_CONTAINERS_CURSOR_POS = 0
            recentSkillUpdates = IntArray(32)
            skillUpdateCycle = 0
            recentKeyUpdates = IntArray(32)
            keyCycle = 0
            recentMouseWheels = IntArray(32)
            mouseWheelCycle = 0
            CLAN_VAR_KEYS = IntArray(32)
            CLAN_VAR_COUNTER = 0
            lastChatMessageTick = 0
            lastFriendIgnoreUpdateTick = 0
            lastFriendsChatUpdateTick = 0
            lastClanSettingsUpdateTick = 0
            lastClanChannelUpdateTick = 0
            lastGrandExchangeUpdateTick = 0
            lastMiscUpdateTick = 0
            VARCS_BOOLS_LOADED = 0
            clientIsActive = false
            exitTime = -1L
            PENDING_HOOK_REQUESTS = NodeCollection()
            TICK_HOOK_REQUESTS = NodeCollection()
            MOUSE_HOOK_REQUESTS = NodeCollection()
            ICOMPONENT_SETTINGS_SLOTS = IterableNodeMap<IFEvents>(512)
            componentBackgroundCount = 0
            lastRenderedCycle = -2
            IS_COMPONENT_ON_TOP = BooleanArray(107)
            PREV_COMPONENT_ON_TOP = BooleanArray(107)
            INTERFACE_BG_COMPONENTS = arrayOfNulls<Rectangle>(107)

            for (i_0 in 0..106) {
                INTERFACE_BG_COMPONENTS[i_0] = Rectangle()
            }

            interfaceDebugMode = 0
            ANTI_MACRO_TEXT_BOUNDS = IntArray(4)
            currentHintPtr = 0
            MAX_HINTS = 51
            HINT_X = IntArray(MAX_HINTS)
            HINT_Y = IntArray(MAX_HINTS)
            HINT_WIDTH = IntArray(MAX_HINTS)
            HINT_DETAILS = arrayOfNulls<HintDetails>(MAX_HINTS)
            OVERHEAD_TEXT_COLORS = intArrayOf(16776960, 16711680, 65280, 65535, 16711935, 16777215)
            CHAT_MESSAGE_UNIQUE_HASHES = LongArray(100)
            chatMessageHashIndex = 0
            FriendsChat.FC_NAME = null
            FriendsChat.FC_OWNER_NAME = null
            GRAND_EXCHANGE_SLOTS = arrayOfNulls<GrandExchangeSlot>(6)
            lastCameraSmoothCycle = 0
            CAM_SHAKING = BooleanArray(5)
            CAM_SHAKE_JITTER = IntArray(5)
            CUSTOM_LOWEST_YAW = IntArray(5)
            CAM_SHAKE_FREQUENCY = IntArray(5)
            CAM_SHAKE_PHASE = IntArray(5)
            viewportFovMin = 256
            viewportFovMax = 205
            cameraZoomMin = 256
            cameraZoomMax = 320
            fovScaleMin = 1
            fovScaleMax = 32767
            aspectFovMin = 1
            aspectFovMax = 32767
            hintArrowOffsetX = 0
            hintArrowOffsetY = 0
            gameAreaWidth = 0
            gameAreaHeight = 0
            viewportScaledFov = 0
            PLAYER_LOGINSTATE_UPDATES = EntityNodeList<LoginStateUpdate>()
            ACTIVE_TIMEZONE_CALENDER = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
            displayNameChangeState = -6
            INTERACTIVE_OBJECT_COMPARABLE = ComparableInteractiveEntity()
            HITSPLAT_SIZE_CACHE = IntArray(4)
        }

        @JvmStatic
        fun getComponentSettings(icomponentdefinitions0: Component): IFEvents? {
            val class282Sub101 = ICOMPONENT_SETTINGS_SLOTS.get((icomponentdefinitions0.idHash.toLong() shl 32) + icomponentdefinitions0.slotId) as IFEvents?
            return if (class282Sub101 != null) class282Sub101 else icomponentdefinitions0.targetParams
        }

        fun getDragTarget(component: Component?): Component? {
            var component = component
            val targetParams: IFEvents? = getComponentSettings(component!!)
            if (targetParams!!.ignoresDepthFlags()) {
                return frameComponent
            }
            val depthFlags = targetParams.depthFlags()
            if (depthFlags == 0) {
                return null
            }
            for (i_3 in 0..<depthFlags) {
                component = Interface.getSub(Interface.getInterface(component!!.idHash)!!, component)
                if (component == null) {
                    return frameComponent
                }
            }

            return component
        }

        fun buildOverlappingEntityList() {
            overlappingEntityCount = 0

            for (index in 0..<NPC_UPDATE_INDEX) {
                val npc = (NPC_MAP.get(NPC_UPDATE_INDICES[index].toLong()))!!.value as NpcEntity?
                if (npc!!.isHidden && npc.getOverheadSprite() != -1) {
                    val size = (npc.getSize() - 1) * 256 + 252
                    val abs = npc.getAbsolutePosition().coords
                    val x = abs.x.toInt() - size shr 9
                    val z = abs.z.toInt() - size shr 9
                    val pathing = PathingEntity.getLargestEntity(npc.plane.toInt(), x, z)
                    if (pathing != null) {
                        var indexReference = pathing.playerIndex
                        if (pathing is NpcEntity) {
                            indexReference += 2048
                        }

                        if (pathing.overheadSpriteCount == 0 && pathing.getOverheadSprite() != -1) {
                            ENTITY_INDEX_REFERENCES[overlappingEntityCount] = indexReference
                            NPC_INDEX_REFERENCES[overlappingEntityCount] = indexReference
                            overlappingEntityCount++
                            pathing.overheadSpriteCount++
                        }

                        ENTITY_INDEX_REFERENCES[overlappingEntityCount] = indexReference
                        NPC_INDEX_REFERENCES[overlappingEntityCount] = npc.playerIndex + 2048
                        overlappingEntityCount++
                        pathing.overheadSpriteCount++
                    }
                }
            }

            ArrayUtils.quicksort(NPC_INDEX_REFERENCES, ENTITY_INDEX_REFERENCES, 0, overlappingEntityCount - 1)
        }

        fun setGameState(state: GameState) {
            if (state != gameState) {
                drawsSinceLastPacket = 0
                if (state == GameState.UNK_10 || state == GameState.UNK_17) {
                    LoginManager.startWorldLogin()
                }

                if (state != GameState.UNK_10 && WorldConnection.gameConnection != null) {
                    WorldConnection.gameConnection!!.close()
                    WorldConnection.gameConnection = null
                }

                if (state == GameState.UNK_5) {
                    ClientStartup.preparePlayer(gameState == GameState.IN_ACCOUNT_CREATION || gameState == GameState.UNK_8 || BASE_WINDOW_ID != ENTITY_DEFAULTS!!.loginInterfaceId)
                }

                if (state == GameState.UNK_0) {
                    LoginManager.openLobby(BASE_WINDOW_ID != ENTITY_DEFAULTS!!.lobbyWindow)
                }

                if (state != GameState.UNK_14 && state != GameState.UNK_12) {
                    if (state == GameState.UNK_19 || state == GameState.UNK_7 && gameState != GameState.UNK_6) {
                        LoginManager.startWorldLogin()
                    } else if (state == GameState.UNK_8) {
                        AccountCreationStage.initAccountCreation()
                    }
                } else {
                    LoginManager.startLobbyLogin()
                }

                if (GameState.isLoadingRegion(state)) {
                    Resource.mapRegion!!.setLoadingRegion()
                    GraphicsCache.resetWorldmapCaches(true)
                }

                val isOutsideGame = state == GameState.UNK_1 || GameState.isLoggedOut(state) || GameState.inLobby(state)
                val wasOutsideGame = gameState == GameState.UNK_1 || GameState.isLoggedOut(gameState) || GameState.inLobby(gameState)
                if (isOutsideGame != wasOutsideGame) {
                    if (isOutsideGame) {
                        AreaSoundPlayer.currentMusicArchive = AreaSoundPlayer.titlescreenMusicArchive
                        if (PreferenceManager.preferences!!.soundLevel!!.getValue() != 0) {
                            AreaSoundPlayer.musicSourceUpdate(Resource.MUSIC!!, AreaSoundPlayer.titlescreenMusicArchive, PreferenceManager.preferences!!.soundLevel!!.getValue())
                            SongPlayer.resetChannelVolumes()
                        } else {
                            disableTitlescreenMusic()
                        }

                        Resource.remoteFileRequesterJs5!!.sendLoginState(false)
                    } else {
                        disableTitlescreenMusic()
                        Resource.remoteFileRequesterJs5!!.sendLoginState(true)
                    }
                }

                if (GameState.isLoadingRegion(state) || state == GameState.UNK_10 || state == GameState.UNK_17) {
                    Renderers.activeToolkit!!.renderLastFrame()
                }

                gameState = state
            }
        }

        @JvmStatic
        fun logout(reconnect: Boolean) {
            val contexts: Array<BufferedConnectionContext> = connectionContexts
            for (index in contexts.indices) {
                val connectionContext = contexts[index]
                try {
                    connectionContext.flush()
                } catch (ignored: IOException) {
                    // NOOP
                }
                connectionContext.closeConnection()
            }
            LoginManager.resetLoginState()
            TypeLists.resetCachedData(false)
            Resource.mapRegion!!.resetInteractiveEntities()
            Resource.mapRegion!!.resetBasePosition()
            GraphicsCache.resetWorldmapCaches(false)
            disableTitlescreenMusic()
            AreaSoundPlayer.currentMusicArchive = -1
            SoundEffectPlayer.soundJinglePlaying = false
            SongPlayer.resetChannelVolumes()
            AmbientSound.unlinkSounds(true)
            AbstractNativeToolkit.drawLoadingPleaseWaitHardware()
            clearWorldMapCaches()
            resetGameState()
            if (reconnect) {
                setGameState(GameState.UNK_12)
            } else {
                setGameState(GameState.UNK_5)
            }
        }

        fun hasDepthFlags(component: Component): Boolean {
            val targetParams: IFEvents? = getComponentSettings(component)
            return targetParams!!.depthFlags() > 0 || (targetParams.ignoresDepthFlags() || component.depthChildOverride != null)
        }

        fun buildCanvasComponents() {
            KeyRecorder.keyRecorder!!.addKeyListeners()
            MouseRecord.recorder!!.reset()
            ClientStartup.activeClient!!.buildCanvas()
            gameCanvas!!.setBackground(Color.black)
            CURRENT_CURSOR = -1
            KeyRecorder.keyRecorder = CanvasKeyRecorder.create(gameCanvas!!)
            MouseRecord.recorder = AbstractMouseManager.inializeMouseRecorder(gameCanvas!!)
        }

        @JvmStatic
        fun setLoadingWorldMap() {
            setGameState(GameState.LOADING_PLEASE_WAIT)
            AbstractNativeToolkit.drawLoadingPleaseWaitHardware()
            System.gc()
        }

        fun shutdownIcmpservice() {
            IcmpServiceJaclib.exit()
        }

        val currentCycle: Long
            get() = cycleManager!!.getCurrentCycle()

        fun clearWorldMapCaches() {
            WorldMapSub1.hiddenMecIds.clear()
            WorldMapSub1.hiddenCategoryIds.clear()
        }

        @JvmStatic
        fun applyCameraYawRight() {
            if (!cameraYawVelocityApplied) {
                cameraYawVelocity += (24.0f - cameraYawVelocity) / 2.0f
                sendCameraAngleToServer = true
                cameraYawVelocityApplied = true
            }
        }

        fun isInLobbyOrCreation(i_0: GameState?): Boolean {
            return i_0 == GameState.UNK_5 || i_0 == GameState.UNK_14 || i_0 == GameState.UNK_19 || i_0 == GameState.IN_ACCOUNT_CREATION || i_0 == GameState.UNK_8 || i_0 == GameState.UNK_9
        }

        fun isInLobbyConnection(i_0: GameState?): Boolean {
            return i_0 == GameState.UNK_0 || i_0 == GameState.UNK_7
        }

        fun disableTitlescreenMusic() {
            SoundResourceManager.songLoadState = 1
            AreaSoundPlayer.MUSIC_RESOURCE = null
            SongPlayer.musicArchiveId = -1
            SoundResourceManager.musicFileId = -1
            SoundResourceManager.pendingMidiStream = null
            AreaSoundPlayer.SOUND_LEVEL = 0
            StaticMethods.songLooping = false
            CutsceneBasicMovement.musicFadeStep = 2
            SongPlayer.songTransition = null
        }

        fun requestVarVerifyid(): Boolean {
            varVerifyId++
            sendTransmitVarVerifyid = true
            return true
        }

        fun processGameTick() {
            if (REBOOT_TIMER > 1) {
                REBOOT_TIMER--
                lastMiscUpdateTick = serverTick
            }
            if (GAME_CONNECTION_CONTEXT.dropped) {
                GAME_CONNECTION_CONTEXT.dropped = false
                WorldConnection.dropClient()
            } else {
                if (!MiniMenu.isOpen) {
                    MiniMenu.resetEntries()
                }
                var i_1 = 0
                while (i_1 < 100 && PacketDecoder.decodePacketFromStream(GAME_CONNECTION_CONTEXT)) {
                    i_1++
                }
                if (gameState == GameState.LOGGED_INGAME) {
                    while (ReflectionCheckManager.hasPendingChecks()) {
                        val reflectionCheckPacket = PacketEncoder.createPacket(ClientProt.REFLECTION_CHECK, GAME_CONNECTION_CONTEXT.isaac)
                        reflectionCheckPacket.buffer.writeByte(0)
                        val position = reflectionCheckPacket.buffer.currentPosition
                        ReflectionCheckManager.collectResult(reflectionCheckPacket.buffer)
                        reflectionCheckPacket.buffer.writeIndex(reflectionCheckPacket.buffer.currentPosition - position)
                        GAME_CONNECTION_CONTEXT.queuePacket(reflectionCheckPacket)
                    }
                    if (PingRequester.CURRENT_REQUEST != null) {
                        if (PingRequester.CURRENT_REQUEST!!.ping != -1) {
                            val writePingPacket = PacketEncoder.createPacket(ClientProt.WRITE_PING, GAME_CONNECTION_CONTEXT.isaac)
                            writePingPacket.buffer.writeShort(PingRequester.CURRENT_REQUEST!!.ping)
                            GAME_CONNECTION_CONTEXT.queuePacket(writePingPacket)
                            PingRequester.CURRENT_REQUEST = null
                            PingRequester.NEXT_PING_UPDATE_TO_SERVER = Utils.time() + 30000L
                        }
                    } else if (Utils.time() >= PingRequester.NEXT_PING_UPDATE_TO_SERVER) {
                        PingRequester.CURRENT_REQUEST = PING_REQUESTER!!.createPingRequest(LoginManager.GAME_CONNECTION_INFO!!.host)
                    }
                    PacketEncoder.submitQueuedPackets()
                    val record = MOUSE_MOVEMENTS.getFirst() as MouseRecord?
                    if (Resource.mapRegion!!.getScene() != null) {
                        if (Camera.cameraCalculationType == 5) {
                            Camera.calculateCameraPosition()
                        } else if (Camera.cameraCalculationType == 3) {
                            Camera.calculateCutsceneCameraPosition()
                        }
                    }
                    if (cameraPitchVelocityApplied) {
                        cameraPitchVelocityApplied = false
                    } else {
                        cameraPitchVelocity /= 2.0f
                    }
                    if (cameraYawVelocityApplied) {
                        cameraYawVelocityApplied = false
                    } else {
                        cameraYawVelocity /= 2.0f
                    }
                    ComponentMinimap.drawGame()
                    if (gameState == GameState.LOGGED_INGAME) {
                        Resource.mapRegion!!.getEnvironmentManager().updateAtmosphere(Resource.mapRegion!!)
                        LocAction.transformObjects()
                        AreaSoundPlayer.updateAreaSounds()
                        if (drawsSinceLastPacket > 10) {
                            ++GAME_CONNECTION_CONTEXT.idleReadPulses
                        }
                        if (GAME_CONNECTION_CONTEXT.idleReadPulses > 2250) {
                            WorldConnection.dropClient()
                        } else {
                            if (MAP_LOADING_STAGE == 1) {
                                PlayerUpdate.movePlayers()
                                NPCUpdate.moveNpcs()
                            } else if (TypeLists.CUTSCENE_DEFAULTS!!.exitCutscene!!.valid(record, KEY_RECORDS, maximumHeldKeys, KeyRecorder.keyRecorder)) {
                                PacketEncoder.queueCutsceneExit(false)
                            } else {
                                if (MAP_LOADING_STAGE == 3 && CutsceneTypeList.isFullyCached(CURRENT_CUTSCENE)) {
                                    Resource.mapRegion!!.initialize(RegionLoadRequest(RegionLoadingType.CUTSCENE, null))
                                    MAP_LOADING_STAGE = 0
                                }
                                if (MAP_LOADING_STAGE == 0 && gameState != GameState.LOADING_PLEASE_WAIT) {
                                    CutsceneTypeList.CONFIG_CUTSCENES.clear()
                                    MAP_LOADING_STAGE = 4
                                    cycleCutsceneLoaded = cycles
                                    CUTSCENE_MISC_ACTION_INDEX = 0
                                    StaticMethods.saveFovForCutscene()
                                }
                                if (MAP_LOADING_STAGE == 4) {
                                    val cyclesPassed: Int = cycles - cycleCutsceneLoaded
                                    if (CUTSCENE_MISC_ACTION_INDEX < CutsceneTypeList.CUTSCENE_MISC_ACTIONS!!.size) {
                                        do {
                                            val action = CutsceneTypeList.CUTSCENE_MISC_ACTIONS!![CUTSCENE_MISC_ACTION_INDEX]
                                            if (action.lengthInCycles > cyclesPassed) {
                                                break
                                            }
                                            action.perform()
                                        } while (MAP_LOADING_STAGE == 4 && ++CUTSCENE_MISC_ACTION_INDEX < CutsceneTypeList.CUTSCENE_MISC_ACTIONS!!.size)
                                    }
                                    if (MAP_LOADING_STAGE == 4) {
                                        for (index in CutsceneTypeList.CUTSCENE_ENTITIES!!.indices) {
                                            val cutsceneEntity = CutsceneTypeList.CUTSCENE_ENTITIES!![index]
                                            if (cutsceneEntity.initialized) {
                                                val pathingEntity = cutsceneEntity.getEntity()
                                                EntityUpdating.move(pathingEntity, true)
                                            }
                                        }
                                    }
                                }
                            }
                            PlayerUpdate.updateHintDetailsOnTick()
                            MapRegion.cycleGfx()
                            PulseEventHandler.processPulseEvents()
                            ++animatingStep
                            if (crossType != 0) {
                                crossSpritePtr += 20
                                if (crossSpritePtr >= 400) {
                                    crossType = 0
                                }
                            }
                            if (MenuInteraction.lastHoveredComponent != null) {
                                ++menuItemHoverCycle
                                if (menuItemHoverCycle >= 15) {
                                    Component.redrawComponent(MenuInteraction.lastHoveredComponent!!)
                                    MenuInteraction.lastHoveredComponent = null
                                }
                            }
                            hoveredComponent = null
                            isDraggingComponent = false
                            isComponentSelected = false
                            WorldMap.worldmapComponent = null
                            InterfaceComponentRenderer.setChatDisplay(null, -1, -1)
                            if (!interfaceSelectionActive) {
                                defaultMenuCursor = -1
                            }
                            Interface.ifHover()
                            ++serverTick
                            if (CommandExecutor.teleportCommandRequested) {
                                val tpPacket = PacketEncoder.createPacket(ClientProt.WORLD_MAP_CLICK, GAME_CONNECTION_CONTEXT.isaac)
                                tpPacket.buffer.writeIntLittle(CommandExecutor.teleportCommandPositionZ shl 28 or (CommandExecutor.teleportCommandPositionX shl 14) or CommandExecutor.teleportCommandPositionY)
                                GAME_CONNECTION_CONTEXT.queuePacket(tpPacket)
                                CommandExecutor.teleportCommandRequested = false
                            }

                            while (true) {
                                var icomponentdefinitions10: Component?
                                var icomponentdefinitions11: Component?
                                var hookRequest: HookRequest?
                                do {
                                    hookRequest = TICK_HOOK_REQUESTS.popHead() as HookRequest?
                                    if (hookRequest == null) {
                                        while (true) {
                                            do {
                                                hookRequest = MOUSE_HOOK_REQUESTS.popHead() as HookRequest?
                                                if (hookRequest == null) {
                                                    while (true) {
                                                        do {
                                                            hookRequest = PENDING_HOOK_REQUESTS.popHead() as HookRequest?
                                                            if (hookRequest == null) {
                                                                if (WorldMap.worldmapComponent == null) {
                                                                    menuItemCount = 0
                                                                }
                                                                if (draggingComponent != null) {
                                                                    processDrag()
                                                                }
                                                                MenuInteraction.processMenuInput()
                                                                /**
                                                                 * Shift & Control pressed --> Scroll mouse = change plane
                                                                 */
                                                                if (PLAYER_RIGHTS > 0 && KeyRecorder.keyRecorder!!.held(82) && KeyRecorder.keyRecorder!!.held(81) && mouseWheelDelta != 0) {
                                                                    var plane: Int = PlayerUpdate.MY_PLAYER!!.plane - mouseWheelDelta
                                                                    if (plane < 0) {
                                                                        plane = 0
                                                                    } else if (plane > 3) {
                                                                        plane = 3
                                                                    }
                                                                    val base = Resource.mapRegion!!.getBaseCoordinates()
                                                                    CommandExecutor.executeTeleportCommand(plane, PlayerUpdate.MY_PLAYER!!.movementBaseX[0] + base.x, PlayerUpdate.MY_PLAYER!!.movementBaseY[0] + base.y)
                                                                }
                                                                for (index in 0..4) {
                                                                    CAM_SHAKE_PHASE[index]++
                                                                }
                                                                if (clientIsActive && exitTime < Utils.time() - 60000L) {
                                                                    PacketEncoder.writeVarcPreferences()
                                                                }
                                                                var loginStateUpdate = PLAYER_LOGINSTATE_UPDATES.getFirst() as LoginStateUpdate?
                                                                while (loginStateUpdate != null) {
                                                                    if (loginStateUpdate.time < Utils.time() / 1000L - 5L) {
                                                                        if (loginStateUpdate.actionType > 0) {
                                                                            ChatBox.writeMessage(ChatMessageType.FRIENDS_STATUS, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_IN.translate(CURRENT_LANGUAGE!!))
                                                                        }
                                                                        if (loginStateUpdate.actionType.toInt() == 0) {
                                                                            ChatBox.writeMessage(ChatMessageType.FRIENDS_STATUS, 0, "", "", "", loginStateUpdate.username + LocalizedText.HAS_LOGGED_OUT.translate(CURRENT_LANGUAGE!!))
                                                                        }
                                                                        loginStateUpdate.unlinkNode()
                                                                    }
                                                                    loginStateUpdate = PLAYER_LOGINSTATE_UPDATES.getNext() as LoginStateUpdate?
                                                                }
                                                                ++cameraRandomizationCycle
                                                                if (cameraRandomizationCycle > 508) {
                                                                    cameraRandomizationCycle = 0
                                                                    val direction = (Math.random() * 8.0).toInt()
                                                                    if ((direction and 0x1) == 1) {
                                                                        Camera.cameraRandomisationH += Camera.cameraStepH
                                                                    }
                                                                    if ((direction and 0x2) == 2) {
                                                                        Camera.cameraRandomisationV += Camera.cameraStepV
                                                                    }
                                                                    if ((direction and 0x4) == 4) {
                                                                        Camera.cameraRandomisationA += Camera.cameraStepA
                                                                    }
                                                                }
                                                                if (Camera.cameraRandomisationH < -51) {
                                                                    Camera.cameraStepH = 2
                                                                }
                                                                if (Camera.cameraRandomisationH > 56) {
                                                                    Camera.cameraStepH = -2
                                                                }
                                                                if (Camera.cameraRandomisationV < -63) {
                                                                    Camera.cameraStepV = 2
                                                                }
                                                                if (Camera.cameraRandomisationV > 56) {
                                                                    Camera.cameraStepV = -2
                                                                }
                                                                if (Camera.cameraRandomisationA < -44) {
                                                                    Camera.cameraStepA = 1
                                                                }
                                                                if (Camera.cameraRandomisationA > 40) {
                                                                    Camera.cameraStepA = -1
                                                                }
                                                                ++minimapRandomizationCycle
                                                                if (minimapRandomizationCycle > 508) {
                                                                    minimapRandomizationCycle = 0
                                                                    val i_2 = (Math.random() * 8.0).toInt()
                                                                    if ((i_2 and 0x1) == 1) {
                                                                        minimapRotation += minimapRotationStep
                                                                    }
                                                                    if ((i_2 and 0x2) == 2) {
                                                                        minimapZoom += minimapZoomStep
                                                                    }
                                                                }
                                                                if (minimapRotation < -60) {
                                                                    minimapRotationStep = 2
                                                                }
                                                                if (minimapRotation > 65) {
                                                                    minimapRotationStep = -2
                                                                }
                                                                if (minimapZoom < -20) {
                                                                    minimapZoomStep = 1
                                                                }
                                                                if (minimapZoom > 11) {
                                                                    minimapZoomStep = -1
                                                                }
                                                                if (++GAME_CONNECTION_CONTEXT.lastFlush > 50) {
                                                                    val pingPacket = PacketEncoder.createPacket(ClientProt.PING, GAME_CONNECTION_CONTEXT.isaac)
                                                                    GAME_CONNECTION_CONTEXT.queuePacket(pingPacket)
                                                                }
                                                                if (sendTransmitVarVerifyid) {
                                                                    PacketEncoder.sendTransmitVarVerifyid()
                                                                    sendTransmitVarVerifyid = false
                                                                }
                                                                try {
                                                                    GAME_CONNECTION_CONTEXT.flush()
                                                                } catch (ioexception: IOException) {
                                                                    WorldConnection.dropClient()
                                                                }
                                                                return
                                                            }
                                                            icomponentdefinitions10 = hookRequest.source
                                                            if (icomponentdefinitions10!!.slotId < 0) {
                                                                break
                                                            }
                                                            icomponentdefinitions11 = Component.fromHash(icomponentdefinitions10.parentId)
                                                        } while (icomponentdefinitions11 == null || icomponentdefinitions11.slotChildren == null || icomponentdefinitions10.slotId >= icomponentdefinitions11.slotChildren!!.size || icomponentdefinitions10 != icomponentdefinitions11.slotChildren!![icomponentdefinitions10.slotId])
                                                        CS2Executor.executeHookInner(hookRequest)
                                                    }
                                                }
                                                icomponentdefinitions10 = hookRequest.source
                                                if (icomponentdefinitions10!!.slotId < 0) {
                                                    break
                                                }
                                                icomponentdefinitions11 = Component.fromHash(icomponentdefinitions10.parentId)
                                            } while (icomponentdefinitions11 == null || icomponentdefinitions11.slotChildren == null || icomponentdefinitions10.slotId >= icomponentdefinitions11.slotChildren!!.size || icomponentdefinitions10 != icomponentdefinitions11.slotChildren!![icomponentdefinitions10.slotId])
                                            CS2Executor.executeHookInner(hookRequest)
                                        }
                                    }
                                    icomponentdefinitions10 = hookRequest.source
                                    if (icomponentdefinitions10!!.slotId < 0) {
                                        break
                                    }
                                    icomponentdefinitions11 = Component.fromHash(icomponentdefinitions10.parentId)
                                } while (icomponentdefinitions11 == null || icomponentdefinitions11.slotChildren == null || icomponentdefinitions10.slotId >= icomponentdefinitions11.slotChildren!!.size || icomponentdefinitions10 != icomponentdefinitions11.slotChildren!![icomponentdefinitions10.slotId])
                                CS2Executor.executeHookInner(hookRequest)
                            }
                        }
                    }
                }
            }
        }

        @JvmField
        var CLAN_CHANNEL: ClanChannel? = null
        @JvmField
        var mapOwnerIdentifier: String? = null
        @JvmField
        var lobbyMembershipStatus: Int = 0
        var loginToken: String? = null
        var frameComponent: Component? = null
        var ENTITY_DEFAULTS: EntityDefaults? = null
        @JvmField
        var CLAN_SETTINGS: ClanSettings? = null
        @JvmField
        var GUEST_CLAN_SETTINGS: ClanSettings? = null
        @JvmField
        var recolorPalette: ShortArray? = null
        var randomSeed: Int = 0
        @JvmField
        var COUNTRY: Int = 0
        @JvmField
        var LISTENED_CLAN_CHANNEL: ClanChannel? = null
        var loginType: Int = 0
        var TEST_ENVIRONMENT_APPEARANCE_OVERRIDE_DATA: Array<ByteArray?>? = null
        @JvmField
        var JCOINS: Int = 0

        fun drawGameArea(x: Int, y: Int, gameAreaWidth: Int, gameAreaHeight: Int, bool4: Boolean) {
            var x = x
            var y = y
            var gameAreaWidth = gameAreaWidth
            var gameAreaHeight = gameAreaHeight
            if (Resource.mapRegion!!.getScene() == null) {
                Renderers.activeToolkit!!.clearPixels(x, y, gameAreaWidth, gameAreaHeight, 0xffffff.inv())
            } else {
                val abs = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
                var clearPixels = false
                if (MAP_LOADING_STAGE != 1) {
                    if (!cutsceneCameraActive) {
                        clearPixels = true
                    }
                } else if (abs.x.toInt() < 0 || abs.x.toInt() >= Resource.mapRegion!!.sceneSizeX * 512 || abs.z.toInt() < 0 || abs.z.toInt() >= Resource.mapRegion!!.sceneSizeY * 512) {
                    clearPixels = true
                }
                if (clearPixels) {
                    Renderers.activeToolkit!!.clearPixels(x, y, gameAreaWidth, gameAreaHeight, 0xffffff.inv())
                } else {
                    timesSceneRendered++
                    if (PlayerUpdate.MY_PLAYER != null && abs.x.toInt() - (PlayerUpdate.MY_PLAYER!!.getSize() - 1) * 256 shr 9 == ComponentMinimap.minimapFlagX && abs.z.toInt() - (PlayerUpdate.MY_PLAYER!!.getSize() - 1) * 256 shr 9 == ComponentMinimap.minimapFlagY) {
                        ComponentMinimap.minimapFlagX = -1
                        ComponentMinimap.minimapFlagY = -1
                        PulseEventQueue.delayedClearMinimapFlagUpdate()
                    }
                    EntityUpdating.positionAllEntities()
                    if (!bool4) {
                        MapRegion.addProjectileSpawnsToScene()
                    }
                    MapRegion.addGfxsToScene()
                    AbstractToolkit.calculateGameArea(x, y, gameAreaWidth, gameAreaHeight, true)
                    x = hintArrowOffsetX
                    y = hintArrowOffsetY
                    gameAreaWidth = gameAreaWidth
                    gameAreaHeight = gameAreaHeight
                    MiniMenu.setViewportOrigin(x, y)
                    if (Camera.cameraCalculationType == 2) {
                        var yaw = Camera.camAngleX.toInt()
                        if (Camera.regularLowestYaw shr 8 > yaw) {
                            yaw = Camera.regularLowestYaw shr 8
                        }
                        if (CAM_SHAKING[4] && CUSTOM_LOWEST_YAW[4] + 128 > yaw) {
                            yaw = CUSTOM_LOWEST_YAW[4] + 128
                        }
                        val roll = Camera.camAngleY.toInt() + Camera.cameraRandomisationA and 0x3fff
                        Camera.setCameraPosition(Camera.cameraBaseX, MapRegion.tileheightForAbsolutePosition(abs.x.toInt(), abs.z.toInt(), PlayerUpdate.MY_PLAYER_PLANE) - 200, Camera.cameraBaseY, yaw, roll, (yaw shr 3) * 3 + Camera.cameraZoom shl 2, gameAreaHeight)
                    } else if (Camera.cameraCalculationType == 4) {
                        var yaw = Camera.camAngleX.toInt()
                        if (Camera.regularLowestYaw shr 8 > yaw) {
                            yaw = Camera.regularLowestYaw shr 8
                        }
                        if (CAM_SHAKING[4] && CUSTOM_LOWEST_YAW[4] + 128 > yaw) {
                            yaw = CUSTOM_LOWEST_YAW[4] + 128
                        }
                        val roll = Camera.camAngleY.toInt() and 0x3fff
                        Camera.setCameraPosition(Camera.cameraBaseX, MapRegion.tileheightForAbsolutePosition(cameraTargetAbsX, cameraTargetAbsY, PlayerUpdate.MY_PLAYER_PLANE) - 200, Camera.cameraBaseY, yaw, roll, (yaw shr 3) * 3 + Camera.cameraZoom shl 2, gameAreaHeight)
                    } else if (Camera.cameraCalculationType == 1) {
                        Camera.updateSmoothTransitionCamera(gameAreaHeight)
                    }
                    val camAbsX = Camera.cameraPositionAbsoluteX
                    val camAbsZ = Camera.cameraPositionAbsoluteZ
                    val camAbsY = Camera.cameraPositionAbsoluteY
                    val yaw = Camera.cameraYaw
                    val roll = Camera.cameraRoll
                    for (slot in 0..4) {
                        if (CAM_SHAKING[slot]) {
                            val transition = (Math.random() * (CAM_SHAKE_JITTER[slot] * 2 + 1) - CAM_SHAKE_JITTER[slot] + sin(CAM_SHAKE_PHASE[slot] * (CAM_SHAKE_FREQUENCY[slot] / 100.0)) * CUSTOM_LOWEST_YAW[slot]).toInt()
                            if (slot == 0) {
                                Camera.cameraPositionAbsoluteX += transition shl 2
                            }
                            if (slot == 1) {
                                Camera.cameraPositionAbsoluteZ += transition shl 2
                            }
                            if (slot == 2) {
                                Camera.cameraPositionAbsoluteY += transition shl 2
                            }
                            if (slot == 3) {
                                Camera.cameraRoll = transition + Camera.cameraRoll and 0x3fff
                            }
                            if (slot == 4) {
                                Camera.cameraYaw += transition
                                if (Camera.cameraYaw < 1024) {
                                    Camera.cameraYaw = 1024
                                } else if (Camera.cameraYaw > 3072) {
                                    Camera.cameraYaw = 3072
                                }
                            }
                        }
                    }
                    if (Camera.cameraPositionAbsoluteX < 0) {
                        Camera.cameraPositionAbsoluteX = 0
                    }
                    if (Camera.cameraPositionAbsoluteX > (Resource.mapRegion!!.getScene()!!.sceneDimensionX shl 9) - 1) {
                        Camera.cameraPositionAbsoluteX = (Resource.mapRegion!!.getScene()!!.sceneDimensionX shl 9) - 1
                    }
                    if (Camera.cameraPositionAbsoluteY < 0) {
                        Camera.cameraPositionAbsoluteY = 0
                    }
                    if (Camera.cameraPositionAbsoluteY > (Resource.mapRegion!!.getScene()!!.sceneDimensionY shl 9) - 1) {
                        Camera.cameraPositionAbsoluteY = (Resource.mapRegion!!.getScene()!!.sceneDimensionY shl 9) - 1
                    }
                    MapRegion.handleRoofDisplay()
                    Renderers.activeToolkit!!.setDrawableArea(x, y, gameAreaWidth, gameAreaHeight)
                    Renderers.activeToolkit!!.setClipping(x, y, gameAreaWidth + x, gameAreaHeight + y)
                    val sceneAtmosphere = Resource.mapRegion!!.getEnvironmentManager().getEnvironment()
                    val fogColor = sceneAtmosphere.getFogColor()
                    SUN_MATRIX.setViewPosition(-Camera.cameraPositionAbsoluteX.toFloat(), -Camera.cameraPositionAbsoluteZ.toFloat(), -Camera.cameraPositionAbsoluteY.toFloat())
                    SUN_MATRIX.rotate(0.0f, -1.0f, 0.0f, MathUtils.getTheta(-Camera.cameraRoll and 0x3fff))
                    SUN_MATRIX.rotate(-1.0f, 0.0f, 0.0f, MathUtils.getTheta(-Camera.cameraYaw and 0x3fff))
                    SUN_MATRIX.rotate(0.0f, 0.0f, -1.0f, MathUtils.getTheta(-Camera.cameraPitch and 0x3fff))
                    Renderers.activeToolkit!!.setSunPosition(SUN_MATRIX)
                    StaticMethods.setupOrthoProjection(Camera.projectionMatrix, (gameAreaWidth / 2).toFloat(), (gameAreaHeight / 2).toFloat(), (viewportScaledFov shl 1).toFloat(), (viewportScaledFov shl 1).toFloat(), gameAreaWidth, gameAreaHeight)
                    Renderers.activeToolkit!!.setModelMatrix(Camera.projectionMatrix)
                    if (sceneAtmosphere.getSkybox() != null) {
                        Renderers.activeToolkit!!.updateSun(1.0f)
                        Renderers.activeToolkit!!.applyLighting(16777215, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
                        sceneAtmosphere.getSkybox()!!.render(Renderers.activeToolkit!!, Environment.skyboxRotation shl 3, x, y, gameAreaWidth, gameAreaHeight, Camera.cameraYaw, Camera.cameraRoll, Camera.cameraPitch, fogColor)
                    } else {
                        Renderers.activeToolkit!!.applyClearFlags(3, fogColor)
                    }
                    Camera.storePickingState(SUN_MATRIX, Camera.projectionMatrix, gameAreaWidth, gameAreaHeight)
                    Resource.mapRegion!!.getEnvironmentManager().updateAtmosphere(Resource.mapRegion!!)
                    val roofPreference = if (PreferenceManager.preferences!!.removeRoofsOptionOverride!!.getValue() == 2) timesSceneRendered.toByte() else 1
                    Resource.mapRegion!!.getScene()!!.drawScene(
                        cycles, Camera.cameraPositionAbsoluteX, Camera.cameraPositionAbsoluteZ, Camera.cameraPositionAbsoluteY, Resource.mapRegion!!.bits, visibleBelowDrawheight, visibleAboveX, visibleBelowX, visibleBelowY, visibleAboveY, PlayerUpdate.MY_PLAYER!!.plane + 1, roofPreference, PreferenceManager.preferences!!.flickeringEffects!!.getValue() == 0
                    )
                    drawsSinceLastPacket++
                    SoundEffectPlayer.updatePlayers()
                    if (gameState == GameState.LOGGED_INGAME) {
                        StaticMethods.drawTileMessages(x, y)
                        EntityUpdating.drawEntitites(x, y, gameAreaWidth, gameAreaHeight)
                        HintRenderer.drawHintArrow(x, y)
                        StaticMedia.drawClickCross()
                    }
                    Resource.mapRegion!!.getScene()!!.clearInteractives()
                    Camera.cameraPositionAbsoluteX = camAbsX
                    Camera.cameraPositionAbsoluteZ = camAbsZ
                    Camera.cameraPositionAbsoluteY = camAbsY
                    Camera.cameraYaw = yaw
                    Camera.cameraRoll = roll
                    if (awaitingPriorityRequests && Resource.remoteFileRequesterJs5!!.priorityRequestCount() == 0) {
                        awaitingPriorityRequests = false
                    }
                    if (awaitingPriorityRequests) {
                        Renderers.activeToolkit!!.clearPixels(x, y, gameAreaWidth, gameAreaHeight, 0xffffff.inv())
                        MessageBox.draw(LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE!!), false, Renderers.activeToolkit, FontManager.debugOverlayFont, FontManager.messageboxFont)
                    }
                    StaticMethods.setupOrthoProjection(Camera.projectionMatrix, (x + gameAreaWidth / 2).toFloat(), (gameAreaHeight / 2 + y).toFloat(), (viewportScaledFov shl 1).toFloat(), (viewportScaledFov shl 1).toFloat(), gameAreaWidth, gameAreaHeight)
                    Renderers.activeToolkit!!.setModelMatrix(Camera.projectionMatrix)
                    Camera.storeViewProjectionMatrix(Camera.projectionMatrix)
                }
            }
        }

        fun initializeTips() {
            if (LoadingStage.gametipsToDisplay != null) {
                val tips: Array<LoginScreenComponent> = LoadingStage.gametipsToDisplay!!
                for (index in tips.indices) {
                    val tip = tips[index]
                    tip.initialize()
                }
            }
        }

        @JvmStatic
        val connectionContext: BufferedConnectionContext
            get() = if (GameState.inLobby(gameState)) LOBBY_CONNECTION_CONTEXT else GAME_CONNECTION_CONTEXT

        fun resetCutscenes() {
            CURRENT_CUTSCENE = -1
            MAP_LOADING_STAGE = 1
            MapRegion.CUTSCENE_MAP_XTEAS = null
            CutsceneTypeList.CUTSCENE_BUFFER = null
            CutsceneTypeList.resetCutsceneConfigs()
        }

        fun resetGraphicsState() {
            if (Renderers.activeToolkit != null) {
                Resource.mapRegion!!.resetInteractiveEntities()
                ComponentMinimap.reset()
                StaticMedia.resetChatMediaAndIcons()
                RenderedEntityCache.reset()
                Resource.mapRegion!!.resetAtmosphere()
                Sun.reset()
                TypeLists.resetCachedData(false)
                StaticMedia.reset()
                GraphicsCache.resetWorldmapCaches(true)
                WorldMapSub1.reset()
                initializeTips()
                for (hintIndex in HINT_TRAILS.indices) {
                    val hint: HintTrail? = HINT_TRAILS[hintIndex]
                }
                for (playerIndex in 0..2047) {
                    val player = PlayerUpdate.players[playerIndex]
                    if (player != null) {
                        for (index in player.models.indices) {
                            player.models[index] = null
                        }
                    }
                }
                for (npcIndex in 0..<npcCount) {
                    val npc = NPC_ARRAY[npcIndex]!!.value as NpcEntity?
                    if (npc != null) {
                        for (index in npc.models.indices) {
                            npc.models[index] = null
                        }
                    }
                }
                ACTIVE_GFX.clear()
                Renderers.activeToolkit!!.cleanupAndShutdown()
                Renderers.activeToolkit = null
            }
        }

        fun drawInterfaceFrame() {
            ParticleProducer.cycle(Renderers.activeToolkit!!, cycles.toLong())
            if (BASE_WINDOW_ID != -1) {
                Component.update(BASE_WINDOW_ID)
            }

            for (index in 0..<componentBackgroundCount) {
                PREV_COMPONENT_ON_TOP[index] = IS_COMPONENT_ON_TOP[index]
                IS_COMPONENT_ON_TOP[index] = false
            }

            lastRenderedCycle = cycles
            if (BASE_WINDOW_ID != -1) {
                componentBackgroundCount = 0
                StaticMethods.drawInterfaces()
            }

            Renderers.activeToolkit!!.resetClipping()
            MiniMenu.render(Renderers.activeToolkit!!)
            var cursorId = MiniMenu.getMinimenuCursor()
            if (cursorId == -1) {
                cursorId = defaultMenuCursor
            }

            if (cursorId == -1) {
                cursorId = CursorManager.defaultCursorId
            }

            CursorTypeList.setCustomCursor(cursorId)
            animatingStep = 0
        }

        fun processDrag() {
            Component.redrawComponent(draggingComponent!!)
            ++InterfaceRenderer.dragTickCount
            if (isDraggingComponent && isComponentSelected) {
                var i_1 = MouseRecord.recorder!!.getMouseX()
                var i_2 = MouseRecord.recorder!!.getMouseY()
                i_1 -= mouseX
                i_2 -= mouseY
                if (i_1 < selectedComponentX) {
                    i_1 = selectedComponentX
                }
                if (i_1 + draggingComponent!!.width > selectedComponentWidth + selectedComponentX) {
                    i_1 = selectedComponentWidth + selectedComponentX - draggingComponent!!.width
                }
                if (i_2 < selectedComponentY) {
                    i_2 = selectedComponentY
                }
                if (i_2 + draggingComponent!!.height > selectedComponentY + selectedComponentHeight) {
                    i_2 = selectedComponentHeight + selectedComponentY - draggingComponent!!.height
                }
                val i_3: Int
                val i_4: Int
                if (selectedComponent == frameComponent) {
                    i_3 = i_1
                    i_4 = i_2
                } else {
                    i_3 = i_1 - selectedComponentX + selectedComponent!!.scrollX
                    i_4 = i_2 - selectedComponentY + selectedComponent!!.scrollY
                }
                val hookrequest5: HookRequest?
                if (!MouseRecord.recorder!!.isMouseButtonPressed()) {
                    if (isDragThresholdMet) {
                        InterfaceComponentRenderer.cancelInterfaceSelection()
                        if (draggingComponent!!.onDragComplete != null) {
                            hookrequest5 = HookRequest()
                            hookrequest5.source = draggingComponent
                            hookrequest5.mouseX = i_3
                            hookrequest5.mouseY = i_4
                            hookrequest5.targetComponent = hoveredComponent
                            hookrequest5.params = draggingComponent!!.onDragComplete
                            CS2Executor.executeHookInner(hookrequest5)
                        }
                        if (hoveredComponent != null && getDragTarget(draggingComponent) != null) {
                            PacketEncoder.sendInterfaceDragOntoInterface(draggingComponent, hoveredComponent)
                        }
                    } else if ((menuOpenMode == 1 || MiniMenu.isTopEntryInterfaceAction()) && MiniMenu.entryCount > 2) {
                        StaticMethods.processMenuSelection(mouseX + dragStartX, dragStartY + mouseY)
                    } else if (MiniMenu.hasEntries()) {
                        StaticMethods.processMenuSelection(dragStartX + mouseX, dragStartY + mouseY)
                    }
                    draggingComponent = null
                    selectedComponent = null
                } else {
                    if (InterfaceRenderer.dragTickCount > draggingComponent!!.dragDeadTime) {
                        val i_7: Int = i_1 - dragStartX
                        val i_6: Int = i_2 - dragStartY
                        if (i_7 > draggingComponent!!.dragDeadzone || i_7 < -draggingComponent!!.dragDeadzone || i_6 > draggingComponent!!.dragDeadzone || i_6 < -draggingComponent!!.dragDeadzone) {
                            isDragThresholdMet = true
                        }
                    }
                    if (draggingComponent!!.onDrag != null && isDragThresholdMet) {
                        hookrequest5 = HookRequest()
                        hookrequest5.source = draggingComponent
                        hookrequest5.mouseX = i_3
                        hookrequest5.mouseY = i_4
                        hookrequest5.params = draggingComponent!!.onDrag
                        CS2Executor.executeHookInner(hookrequest5)
                    }
                }
            } else if (InterfaceRenderer.dragTickCount > 1) {
                draggingComponent = null
                selectedComponent = null
            }
        }

        fun loadAssets(toolkitId: Int, leftTopScreenboxText: String?, verifySettingsPreUpdate: Boolean) {
            LoadingStage.killLoginscreenThread()
            resetGraphicsState()
            buildCanvasComponents()
            Renderers.prepareForRendering(toolkitId, leftTopScreenboxText, verifySettingsPreUpdate)
            StaticMethods.initializedFontcombos!!.resetFontMap()
            StaticMethods.initializedFontcombos!!.initializeFontMap(FONT_PROVIDER)
            FontManager.initialize()
            StaticMedia.loadSprites(Renderers.activeToolkit!!, Resource.SPRITES!!)
            StaticMedia.resetChatMediaAndIcons()
            StaticMedia.initializeChatIcons(StaticMedia.nameIconSprites!!)
            setComponentsOnTop()
            GraphicsCache.resetGraphicCaches()
            if (gameState == GameState.UNK_5) {
                setGameState(GameState.UNK_11)
            } else if (gameState == GameState.UNK_0) {
                setGameState(GameState.UNK_2)
            } else if (gameState == GameState.UNK_7) {
                setGameState(GameState.UNK_6)
            } else if (gameState == GameState.LOGGED_INGAME) {
                setGameState(GameState.LOADING_PLEASE_WAIT)
            } else if (gameState != GameState.UNK_15 && gameState != GameState.UNK_1) {
                if (gameState == GameState.IN_ACCOUNT_CREATION) {
                    setGameState(GameState.UNK_9)
                }
            } else {
                initializeTips()
            }
        }

        @JvmStatic
        fun loadAssets(toolkitId: Int, verifySettingsPreUpdate: Boolean) {
            loadAssets(toolkitId, LocalizedText.LOADING_PLEASE_WAIT.translate(CURRENT_LANGUAGE!!), verifySettingsPreUpdate)
        }

        fun drawGameFrame(long_0: Long) {
            if (Resource.mapRegion!!.getScene() != null) {
                if (Camera.cameraCalculationType != 2 && Camera.cameraCalculationType != 1) {
                    if (Camera.cameraCalculationType == 4) {
                        Camera.updateOrbitTargetCamera(long_0)
                    }
                } else {
                    Camera.updateFollowPlayerCamera(long_0)
                }
            }
            ParticleProducer.cycle(Renderers.activeToolkit!!, cycles.toLong())
            if (BASE_WINDOW_ID != -1) {
                Component.update(BASE_WINDOW_ID)
            }
            for (index in 0..<componentBackgroundCount) {
                PREV_COMPONENT_ON_TOP[index] = IS_COMPONENT_ON_TOP[index]
                IS_COMPONENT_ON_TOP[index] = false
            }
            lastRenderedCycle = cycles
            MiniMenu.setViewportOrigin(-1, -1)
            if (BASE_WINDOW_ID != -1) {
                componentBackgroundCount = 0
                StaticMethods.drawInterfaces()
            }
            Renderers.activeToolkit!!.resetClipping()
            MiniMenu.render(Renderers.activeToolkit!!)
            var cursor = MiniMenu.getMinimenuCursor()
            if (cursor == -1) {
                cursor = defaultMenuCursor
            }
            if (cursor == -1) {
                cursor = CursorManager.defaultCursorId
            }
            CursorTypeList.setCustomCursor(cursor)
            val size = PlayerUpdate.MY_PLAYER!!.getSize() shl 8
            val absolute = PlayerUpdate.MY_PLAYER!!.getAbsolutePosition().coords
            SoundEffectPlayer.updateAllAmbientSounds(PlayerUpdate.MY_PLAYER!!.plane.toInt(), size + absolute.x.toInt(), size + absolute.z.toInt(), animatingStep)
            animatingStep = 0
        }

        fun resetGameState() {
            resetCutscenes()
            Resource.mapRegion!!.resetBasePosition()

            for (index in PlayerUpdate.HINT_ARROWS.indices) {
                PlayerUpdate.HINT_ARROWS[index] = null
            }

            for (index in HINT_TRAILS.indices) {
                HINT_TRAILS[index] = null
            }

            PlayerUpdate.reset()

            for (index in 0..2047) {
                PlayerUpdate.players[index] = null
            }

            NPC_UPDATE_INDEX = 0
            NPC_MAP.clear()
            npcCount = 0
            GROUND_ITEMS.clear()
            Camera.smoothReset()
            varVerifyId = 0
            VarProviders.PLAYER_VAR_PROVIDER.reset()
            GUEST_CLAN_SETTINGS = null
            CLAN_SETTINGS = null
            LISTENED_CLAN_CHANNEL = null
            CLAN_CHANNEL = null
            PingRequester.CURRENT_REQUEST = null
            PingRequester.NEXT_PING_UPDATE_TO_SERVER = 0L
            MiniMenu.close()
            MiniMenu.resetEntries()
        }
    }
}