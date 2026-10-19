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
import com.jagex.game.runetek5.preference.impl.ToolkitPreference
import com.jagex.game.runetek5.preference.impl.WaterPreference

object CameraLoginPrefOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.CAM2_ISENABLED -> cam2IsEnabled(exec)
            ClientScriptOpCode.LOGIN_REQUEST -> loginRequest(exec)
            ClientScriptOpCode.LOGIN_CONTINUE -> loginContinue()
            ClientScriptOpCode.LOGIN_RESETREPLY -> loginResetReply()
            ClientScriptOpCode.CREATE_AVAILABLEREQUEST -> createAvailableRequest(exec)
            ClientScriptOpCode.CREATE_CREATEREQUEST -> createCreateRequest(exec)
            ClientScriptOpCode.CREATE_CONNECTREQUEST -> createConnectRequest()
            ClientScriptOpCode.CREATE_STEP_REACHED -> createStepReached(exec)
            ClientScriptOpCode.LOGIN_REPLY_WORLD_ALT -> loginReplyWorldAlt(exec)
            ClientScriptOpCode.LOGIN_HOPTIME -> loginHopTime(exec)
            ClientScriptOpCode.CREATE_REPLY -> createReply(exec)
            ClientScriptOpCode.CREATE_EMAIL_VALIDATE_REPLY -> createEmailValidateReply(exec)
            ClientScriptOpCode.CREATE_CONNECT_REPLY -> createConnectReply(exec)
            ClientScriptOpCode.LOGIN_DISALLOWRESULT -> loginDisallowResult(exec)
            ClientScriptOpCode.LOBBY_ENTERGAME -> lobbyEnterGame()
            ClientScriptOpCode.LOBBY_ENTERGAMEREPLY -> lobbyEnterGameReply(exec)
            ClientScriptOpCode.LOBBY_ENTERLOBBY -> lobbyEnterLobby(exec)
            ClientScriptOpCode.LOBBY_LEAVELOBBY -> lobbyLeaveLobby()
            ClientScriptOpCode.LOBBY_ENTERLOBBYREPLY -> lobbyEnterLobbyReply(exec)
            ClientScriptOpCode.HAS_SIGNONKEY -> hasSignOnKey(exec)
            ClientScriptOpCode.USERFLOWFLAGS -> userFlowFlags(exec)
            ClientScriptOpCode.CREATE_UNDER13 -> createUnder13(exec)
            ClientScriptOpCode.CREATE_SETUNDER13 -> createSetUnder13()
            ClientScriptOpCode.LOGIN_LAST_TRANSFER_REPLY -> loginLastTransferReply(exec)
            ClientScriptOpCode.LOGIN_INPROGRESS -> loginInProgress(exec)
            ClientScriptOpCode.LOGIN_QUEUE_POSITION -> loginQueuePosition(exec)
            ClientScriptOpCode.LOGIN_CANCEL -> loginCancel()
            ClientScriptOpCode.LOGIN_REQUEST_SOCIAL_NETWORK -> loginRequestSocialNetwork(exec)
            ClientScriptOpCode.LOBBY_ENTERLOBBY_SOCIAL_NETWORK -> lobbyEnterLobbySocialNetwork(exec)
            ClientScriptOpCode.LOGIN_DISALLOWTRIGGER -> loginDisallowTrigger(exec)
            ClientScriptOpCode.CREATE_GET_EMAIL -> createGetEmail(exec)
            ClientScriptOpCode.DETAIL_BRIGHTNESS -> detailBrightness(exec)
            ClientScriptOpCode.DETAIL_REMOVEROOFS_OPTION -> detailRemoveRoofsOption(exec)
            ClientScriptOpCode.DETAIL_GROUNDDECOR_ON -> detailGrounddecorOn(exec)
            ClientScriptOpCode.DETAIL_IDLEANIMS_MANY -> detailIdleAnimsMany(exec)
            ClientScriptOpCode.DETAIL_FLICKERING_ON -> detailFlickeringOn(exec)
            ClientScriptOpCode.DETAIL_SPOTSHADOWS_ON -> detailSpotshadowsOn(exec)
            ClientScriptOpCode.DETAIL_HARDSHADOWS -> detailHardShadows(exec)
            ClientScriptOpCode.DETAIL_LIGHTDETAIL_HIGH -> detailLightDetailHigh(exec)
            ClientScriptOpCode.DETAIL_WATERDETAIL_HIGH -> detailWaterDetailHigh(exec)
            ClientScriptOpCode.DETAIL_FOG_ON -> detailFog(exec)
            ClientScriptOpCode.DETAIL_ANTIALIASING -> setAntiAliasingPreview(exec)
            ClientScriptOpCode.DETAIL_STEREO -> setMonoStero(exec)
            ClientScriptOpCode.DETAIL_SOUNDVOL -> setEffectVolume(exec)
            ClientScriptOpCode.DETAIL_MUSICVOL -> detailMusicVol(exec)
            ClientScriptOpCode.DETAIL_BGSOUNDVOL -> detailBgsoundVol(exec)
            ClientScriptOpCode.DETAIL_REMOVEROOFS_OPTION_OVERRIDE -> detailRemoveRoofsOptionOverride(exec)
            ClientScriptOpCode.DETAIL_PARTICLES -> detailParticles(exec)
            ClientScriptOpCode.DETAIL_ANTIALIASING_DEFAULT -> saveAntiAliasingPreference(exec)
            ClientScriptOpCode.DETAIL_BUILDAREA -> detailBuildArea(exec)
            ClientScriptOpCode.DETAIL_BLOOM -> setBloom(exec)
            ClientScriptOpCode.DETAIL_CUSTOMCURSORS -> detailCustomCursors(exec)
            ClientScriptOpCode.DETAIL_IDLEANIMS -> detailIdleAnims(exec)
            ClientScriptOpCode.DETAIL_GROUNDBLENDING -> detailGroundBlending(exec)
            ClientScriptOpCode.DETAIL_TOOLKIT -> detailToolkit(exec)
            ClientScriptOpCode.DETAIL_TOOLKIT_DEFAULT -> detailToolkitDefault(exec)
            ClientScriptOpCode.DETAIL_CPUUSAGE -> detailCpuUsage(exec)
            ClientScriptOpCode.DETAIL_TEXTURING -> detailTexturing(exec)
            ClientScriptOpCode.DETAIL_MAXSCREENSIZE -> detailMaxScreenSize(exec)
            ClientScriptOpCode.DETAIL_SPEECHVOL -> detailSpeechVol(exec)
            ClientScriptOpCode.DETAIL_LOGINVOL -> detailLoginVol(exec)
            ClientScriptOpCode.DETAIL_LOADINGSCREENTYPE -> detailLoadingScreenType(exec)
            ClientScriptOpCode.DETAIL_SKYDETAIL -> detailSkyDetail(exec)
            ClientScriptOpCode.DETAILGET_BRIGHTNESS -> detailGetBrightness(exec)
            ClientScriptOpCode.DETAILGET_REMOVEROOFS_OPTION -> detailGetRemoveRoofsOption(exec)
            ClientScriptOpCode.DETAILGET_GROUNDDECOR_ON -> detailGetGrounddecorOn(exec)
            ClientScriptOpCode.DETAILGET_IDLEANIMS_MANY -> detailGetIdleAnimsMany(exec)
            ClientScriptOpCode.DETAILGET_FLICKERING_ON -> detailGetFlickeringOn(exec)
            ClientScriptOpCode.DETAILGET_SPOTSHADOWS_ON -> detailGetSpotshadowsOn(exec)
            ClientScriptOpCode.DETAILGET_HARDSHADOWS -> detailGetHardShadows(exec)
            ClientScriptOpCode.DETAILGET_LIGHTDETAIL_HIGH -> detailGetLightDetailHigh(exec)
            ClientScriptOpCode.DETAILGET_WATERDETAIL_HIGH -> getWaterPreference(exec)
            ClientScriptOpCode.DETAILGET_FOG_ON -> pushFogPreference(exec)
            ClientScriptOpCode.DETAILGET_ANTIALIASING -> detailGetAntialiasing(exec)
            ClientScriptOpCode.DETAILGET_STEREO -> detailGetStereo(exec)
            ClientScriptOpCode.DETAILGET_SOUNDVOL -> detailGetSoundVol(exec)
            ClientScriptOpCode.DETAILGET_MUSICVOL -> detailGetMusicVolume(exec)
            ClientScriptOpCode.DETAILGET_BGSOUNDVOL -> detailGetBgSoundVol(exec)
            ClientScriptOpCode.DETAILGET_PARTICLES -> detailGetParticles(exec)
            ClientScriptOpCode.DETAILGET_ANTIALIASING_DEFAULT -> detailGetAntialiasingDefault(exec)
            ClientScriptOpCode.DETAILGET_BUILDAREA -> detailGetBuildArea(exec)
            ClientScriptOpCode.DETAILGET_BLOOM -> detailGetBloom(exec)
            ClientScriptOpCode.DETAILGET_CUSTOMCURSORS -> detailGetCustomCursors(exec)
            ClientScriptOpCode.DETAILGET_IDLEANIMS -> detailGetIdleAnims(exec)
            ClientScriptOpCode.DETAILGET_GROUNDBLENDING -> detailGetGroundBlending(exec)
            ClientScriptOpCode.DETAILGET_TOOLKIT -> detailGetToolkit(exec)
            ClientScriptOpCode.DETAILGET_TOOLKIT_DEFAULT -> detailGetToolkitDefault(exec)
            ClientScriptOpCode.DETAILGET_CPUUSAGE -> detailGetCpuUsage(exec)
            ClientScriptOpCode.DETAILGET_TEXTURING -> detailGetTexturing(exec)
            ClientScriptOpCode.PROFILE_TOOLKIT -> profileToolkit(exec)
            ClientScriptOpCode.DETAILGET_MAXSCREENSIZE -> detailGetMaxScreenSize(exec)
            ClientScriptOpCode.DETAILGET_SPEECHVOL -> detailGetSpeechVol(exec)
            ClientScriptOpCode.DETAILGET_LOGINVOL -> detailGetLoginVol(exec)
            ClientScriptOpCode.DETAILGET_SAFEMODE -> detailGetSafeMode(exec)
            ClientScriptOpCode.DETAILGET_LOADINGSCREENTYPE -> detailGetLoadingScreenType(exec)
            ClientScriptOpCode.DETAILGET_DEFAULTTOOLKIT -> detailGetDefaultToolkit(exec)
            ClientScriptOpCode.DETAILGET_CANCHOOSESAFEMODE -> detailGetCanChooseSafeMode(exec)
            ClientScriptOpCode.DETAILGET_CHOSESAFEMODE -> detailGetChoseSafeMode(exec)
            ClientScriptOpCode.DETAILGET_SKYDETAIL -> detailGetSkyDetail(exec)
            ClientScriptOpCode.VIEWPORT_SETFOV -> viewportSetFov(exec)
            ClientScriptOpCode.VIEWPORT_SETZOOM -> viewportSetZoom(exec)
            ClientScriptOpCode.VIEWPORT_CLAMPFOV -> viewportClampFov(exec)
            ClientScriptOpCode.VIEWPORT_GETEFFECTIVESIZE -> viewportGetEffectiveSize(exec)
            ClientScriptOpCode.VIEWPORT_GETZOOM -> viewportGetZoom(exec)
            ClientScriptOpCode.VIEWPORT_GETFOV -> viewportGetFov(exec)
            ClientScriptOpCode.DATE_MINUTES -> dateMinutes(exec)
            ClientScriptOpCode.DATE_RUNEDAY -> dateRuneday(exec)
            ClientScriptOpCode.DATE_RUNEDAY_FROMDATE -> dateRunedayFromdate(exec)
            ClientScriptOpCode.DATE_YEAR -> dateYear(exec)
            ClientScriptOpCode.DATE_ISLEAPYEAR -> dateIsLeapYear(exec)
            ClientScriptOpCode.DATE_RUNEDAY_TODATE -> dateRunedayToDate(exec)
            ClientScriptOpCode.DATE_MINUTES_FROMRUNEDAY -> dateMinutesFromRuneday(exec)
            ClientScriptOpCode.WORLDLIST_FETCH -> worldlistFetch(exec)
            ClientScriptOpCode.WORLDLIST_START -> worldlistStart(exec)
            ClientScriptOpCode.WORLDLIST_NEXT -> worldlistNext(exec)
            ClientScriptOpCode.WORLDLIST_SWITCH -> worldListSwitch(exec)
            ClientScriptOpCode.WORLDLIST_SPECIFIC -> worldlistSpecific(exec)
            ClientScriptOpCode.WORLDLIST_SORT -> worldlistSort(exec)
            ClientScriptOpCode.WORLDLIST_AUTOWORLD -> worldListAutoWorld()
            else -> throw IllegalStateException("opcode $operation is not handled by CameraLoginPrefOps")
        }
    }

    @JvmStatic
    fun detailParticles(executor: CS2Executor) {
        val particles = executor.intStack[--executor.intStackPtr]
        PreferenceManager.preferences!!.particles!!.setValue(particles)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun getWaterPreference(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.water!!.menuLevel()
    }

    @JvmStatic
    fun detailLoadingScreenType(executor: CS2Executor) {
        var stage = executor.intStack[--executor.intStackPtr]
        if (stage > 255 || stage < 0) {
            stage = 0
        }
        if (stage != PreferenceManager.preferences!!.gametipStartStagePreference!!.getValue()) {
            PreferenceManager.preferences!!.gametipStartStagePreference!!.setValue(stage)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun createConnectRequest() {
        GameState.startGameLogin()
    }

    @JvmStatic
    fun createSetUnder13() {
        Client.hasJustCreatedAccount = true
    }

    @JvmStatic
    fun detailGetBuildArea(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.mapSize!!.getValue()
    }

    @JvmStatic
    fun detailGetSoundVol(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.soundEffectVolume!!.getValue()
    }

    @JvmStatic
    fun viewportGetEffectiveSize(executor: CS2Executor) {
        AbstractToolkit.calculateGameArea(0, 0, Client.GAME_SCREEN_INTERFACE!!.width, Client.GAME_SCREEN_INTERFACE!!.height, false)
        executor.intStack[++executor.intStackPtr - 1] = Client.gameAreaWidth
        executor.intStack[++executor.intStackPtr - 1] = Client.gameAreaHeight
    }

    @JvmStatic
    fun detailGetAntialiasingDefault(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasing!!.getValue()
    }

    @JvmStatic
    fun detailCustomCursors(executor: CS2Executor) {
        PreferenceManager.preferences!!.customCursor!!.setValue(if (executor.intStack[--executor.intStackPtr] != 0) 1 else 0)
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun detailSpotshadowsOn(executor: CS2Executor) {
        PreferenceManager.preferences!!.characterShadows!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailGetChoseSafeMode(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.safemodeForcedDuringStartup) 1  else 0
    }

    @JvmStatic
    fun detailTexturing(executor: CS2Executor) {
        PreferenceManager.preferences!!.textures!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        PreferenceManager.savePreferences()
        GraphicsCache.resetGraphicCaches()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun loginRequest(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val username: String? = executor.stringStack[executor.stringStackPtr] as String?
        val password: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        LoginManager.directLogin(username!!, password!!)
    }

    @JvmStatic
    fun detailGetHardShadows(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.sceneryShadows!!.getValue()
    }

    @JvmStatic
    fun detailMaxScreenSize(executor: CS2Executor) {
        PreferenceManager.preferences!!.maxScreenSize!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.needsResize = true
    }

    @JvmStatic
    fun detailGetIdleAnims(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.idleAnimations!!.getValue()
    }

    @JvmStatic
    fun detailGetIdleAnimsMany(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.idleAnimations!!.getValue()
    }

    @JvmStatic
    fun detailGetParticles(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ParticleSystemManager.getDetailLevel()
    }

    @JvmStatic
    fun lobbyLeaveLobby() {
        Client.logout(false)
    }

    @JvmStatic
    fun worldListSwitch(executor: CS2Executor) {
        val world = executor.intStack[--executor.intStackPtr]
        val host: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress()) {
            executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.setGameHost(world, host!!)) 1 else 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun detailGetLoginVol(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.soundLevel!!.getValue()
    }

    @JvmStatic
    fun lobbyEnterLobbyReply(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.lobbyLoginResponse
    }

    @JvmStatic
    fun worldlistSpecific(executor: CS2Executor) {
        val worldNumber = executor.intStack[--executor.intStackPtr]
        val descriptor: WorldDescriptor? = WorldDescriptor.getWorld(worldNumber)
        if (descriptor != null) {
            executor.intStack[++executor.intStackPtr - 1] = descriptor!!.flags
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor!!.unknown
            val world: WorldMeta? = descriptor!!.getWorld()
            executor.intStack[++executor.intStackPtr - 1] = world!!.countryId
            executor.stringStack[++executor.stringStackPtr - 1] = world!!.activity
            executor.intStack[++executor.intStackPtr - 1] = descriptor!!.playerCount
            executor.intStack[++executor.intStackPtr - 1] = descriptor!!.ping
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor!!.ipAddress
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
    fun detailGetFlickeringOn(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.flickeringEffects!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun detailGetToolkit(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.activeToolkit!!.getValue()
    }

    @JvmStatic
    fun createEmailValidateReply(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.getCheckEmailStatus().getValue()
    }

    @JvmStatic
    fun pushFogPreference(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.fog!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun loginContinue() {
        LoginManager.confirmLoginContinue()
    }

    @JvmStatic
    fun createReply(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = currentCreateAccountRequest()!!.getValue()
    }

    @JvmStatic
    fun saveAntiAliasingPreference(executor: CS2Executor) {
        PreferenceManager.preferences!!.antiAliasing!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun createStepReached(executor: CS2Executor) {
        PacketEncoder.sendAccountCreationStage(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun detailGetSkyDetail(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.skyboxes!!.getValue()
    }

    @JvmStatic
    fun dateIsLeapYear(executor: CS2Executor) {
        val year = executor.intStack[--executor.intStackPtr]
        var isLeapYear = true
        if (year < 0) {
            isLeapYear = (year + 1) % 4 == 0
        } else if (year < 1582) {
            isLeapYear = year % 4 == 0
        } else if (year % 4 != 0) {
            isLeapYear = false
        } else if (year % 100 != 0) {
        } else if (year % 400 != 0) {
            isLeapYear = false
        }
        executor.intStack[++executor.intStackPtr - 1] = if (isLeapYear) 1  else 0
    }

    @JvmStatic
    fun detailGetRemoveRoofsOption(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.removeRoofs!!.getValue() == 2) 1 else 0
    }

    @JvmStatic
    fun detailGetSpeechVol(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.voiceOverVolume!!.getValue()
    }

    @JvmStatic
    fun detailFlickeringOn(executor: CS2Executor) {
        PreferenceManager.preferences!!.flickeringEffects!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailGetSpotshadowsOn(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.characterShadows!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun dateYear(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = CalenderUtil.getLocalYear(Utils.time())
    }

    @JvmStatic
    fun detailGetGroundBlending(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.goundBlending!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun dateRunedayFromdate(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val day = executor.intStack[executor.intStackPtr]
        val month = executor.intStack[executor.intStackPtr + 1]
        val year = executor.intStack[executor.intStackPtr + 2]
        val noonMillis = CalenderUtil.getGmtNoon(day, month, year)
        var runeday = StaticMethods.millisToJagexDays(noonMillis)
        if (year < 1970) {
            --runeday
        }
        executor.intStack[++executor.intStackPtr - 1] = runeday
    }

    @JvmStatic
    fun loginRequestSocialNetwork(executor: CS2Executor) {
        val worldId = executor.intStack[--executor.intStackPtr]
        LoginManager.snLogin(worldId)
    }

    @JvmStatic
    fun lobbyEnterGameReply(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.worldLoginResponse
    }

    @JvmStatic
    fun lobbyEnterLobby(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val username: String? = executor.stringStack[executor.stringStackPtr] as String?
        val password: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        LoginManager.switchWorldAndLogin(username!!, password!!)
    }

    @JvmStatic
    fun detailGetCanChooseSafeMode(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (SystemInfo.SYSTEM_INFO!!.ram >= 512 && !Client.isForcedSafemode) 1 else 0
    }

    @JvmStatic
    fun detailGetMusicVolume(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.musicVolume!!.getValue()
    }

    @JvmStatic
    fun detailWaterDetailHigh(executor: CS2Executor) {
        val water = PreferenceManager.preferences!!.water!!
        val wasHighDetail = water.highDetail()
        water.setValue(WaterPreference.fromMenuLevel(executor.intStack[--executor.intStackPtr]))
        if (water.highDetail() != wasHighDetail) {
            Resource.mapRegion!!.reload()
        }
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun lobbyEnterLobbySocialNetwork(executor: CS2Executor) {
        val worldId = executor.intStack[--executor.intStackPtr]
        StaticMethods.switchToWorldLogin(worldId)
    }

    @JvmStatic
    fun detailRemoveRoofsOptionOverride(executor: CS2Executor) {
        val removeRoofs = PreferenceManager.preferences!!.removeRoofs!!.getValue()
        PreferenceManager.preferences!!.removeRoofsOptionOverride!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 0 else removeRoofs)
        Scene.setRoofOverride()
    }

    @JvmStatic
    fun detailHardShadows(executor: CS2Executor) {
        PreferenceManager.preferences!!.sceneryShadows!!.setValue(executor.intStack[--executor.intStackPtr])
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailBuildArea(executor: CS2Executor) {
        PreferenceManager.preferences!!.mapSize!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun dateRunedayToDate(executor: CS2Executor) {
        val runeday = executor.intStack[--executor.intStackPtr]
        val ddmmyyyy: IntArray? = CalenderUtil.getLocalDdmmyyyy(runeday)
        ArrayUtils.arrayCopy(ddmmyyyy!!, 0, executor.intStack, executor.intStackPtr, 3)
        executor.intStackPtr += 3
    }

    @JvmStatic
    fun detailGroundBlending(executor: CS2Executor) {
        PreferenceManager.preferences!!.goundBlending!!.setValue(if (executor.intStack[--executor.intStackPtr] != 0) 1 else 0)
        PreferenceManager.savePreferences()
        Resource.mapRegion!!.reload()
    }

    @JvmStatic
    fun detailCpuUsage(executor: CS2Executor) {
        PreferenceManager.preferences!!.cpu!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun viewportGetFov(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.viewportFovMin.toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.viewportFovMax.toInt()
    }

    @JvmStatic
    fun detailGetAntialiasing(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasingPreview!!.getValue()
    }

    @JvmStatic
    fun detailRemoveRoofsOption(executor: CS2Executor) {
        val removeRoofs = executor.intStack[--executor.intStackPtr] == 1
        PreferenceManager.preferences!!.removeRoofs!!.setValue(if (removeRoofs) 2  else 1)
        PreferenceManager.preferences!!.removeRoofsOptionOverride!!.setValue(if (removeRoofs) 2  else 1)
        Scene.setRoofOverride()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun dateRuneday(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ((Utils.time() / 86400000L) - 11745).toInt()
    }

    @JvmStatic
    fun detailGetBloom(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.bloom!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun profileToolkit(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = DisplayModeDetails.getProfilingPerformance(PreferenceManager.preferences!!.activeToolkit!!.getValue(), 200)
    }

    @JvmStatic
    fun detailMusicVol(executor: CS2Executor) {
        val volume = executor.intStack[--executor.intStackPtr]
        val currentVolume = PreferenceManager.preferences!!.musicVolume!!.getValue()
        if (volume != currentVolume) {
            if (GameState.isRenderingScene(Client.gameState)) {
                if (currentVolume == 0 && AreaSoundPlayer.currentMusicArchive != -1) {
                    AreaSoundPlayer.playMusic(Resource.MUSIC!!, AreaSoundPlayer.currentMusicArchive, volume)
                    SongPlayer.resetChannelVolumes()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else if (volume == 0) {
                	SoundResourceManager.discardSongData()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else {
                    SoundEffectPlayer.setMusicVolume(volume)
                }
            }
            PreferenceManager.preferences!!.musicVolume!!.setValue(volume)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun setMonoStero(executor: CS2Executor) {
        PreferenceManager.preferences!!.monoStereo!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        SoundEffectPlayer.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun hasSignOnKey(executor: CS2Executor) {
        if (Client.base64Url != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun detailBrightness(executor: CS2Executor) {
        val brightness = executor.intStack[--executor.intStackPtr]
        PreferenceManager.preferences!!.brightness!!.setValue(brightness)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun loginHopTime(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.loginQueuePosition
    }

    @JvmStatic
    fun detailGetStereo(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.monoStereo!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun detailGetLightDetailHigh(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.lightDetail!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun detailLoginVol(executor: CS2Executor) {
        val volume = executor.intStack[--executor.intStackPtr]
        val currentVolume = PreferenceManager.preferences!!.soundLevel!!.getValue()
        if (currentVolume != volume && AreaSoundPlayer.titlescreenMusicArchive == AreaSoundPlayer.currentMusicArchive) {
            if (!GameState.isRenderingScene(Client.gameState)) {
                if (currentVolume == 0) {
                    AreaSoundPlayer.playMusic(Resource.MUSIC!!, AreaSoundPlayer.currentMusicArchive, volume)
                    SongPlayer.resetChannelVolumes()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else if (volume == 0) {
                	SoundResourceManager.discardSongData()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else {
                    SoundEffectPlayer.setMusicVolume(volume)
                }
            }
            PreferenceManager.preferences!!.soundLevel!!.setValue(volume)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun detailGetTexturing(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.textures!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun worldlistFetch(executor: CS2Executor) {
        if (Client.gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress()) {
            if (WorldConnection.requestingWorldList) {
                executor.intStack[++executor.intStackPtr - 1] = 0
            } else if (WorldConnection.worldListReceivedTime > Utils.time() - 1000L) {
                executor.intStack[++executor.intStackPtr - 1] = 1
            } else {
                WorldConnection.requestingWorldList = true
                val packet: TcpPacket? = PacketEncoder.createPacket(ClientProt.REQUEST_WORLD_LIST, Client.LOBBY_CONNECTION_CONTEXT.isaac)
                packet!!.buffer.writeInt(StaticMethods.WORLD_LIST_VERSION)
                Client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet)
                executor.intStack[++executor.intStackPtr - 1] = 0
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 1
        }
    }

    @JvmStatic
    fun detailSpeechVol(executor: CS2Executor) {
        PreferenceManager.preferences!!.voiceOverVolume!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailGetToolkitDefault(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.toolkit!!.getValue()
    }

    @JvmStatic
    fun loginReplyWorldAlt(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.worldLoginResponse
    }

    @JvmStatic
    fun viewportSetZoom(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Client.cameraZoomMin = executor.intStack[executor.intStackPtr].toShort()
        if (Client.cameraZoomMin <= 0) {
            Client.cameraZoomMin = 256
        }
        Client.cameraZoomMax = executor.intStack[executor.intStackPtr + 1].toShort()
        if (Client.cameraZoomMax <= 0) {
            Client.cameraZoomMax = 320
        }
    }

    @JvmStatic
    fun detailGetCustomCursors(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.customCursor!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun worldlistSort(executor: CS2Executor) {
        executor.intStackPtr -= 4
        val sortMode = executor.intStack[executor.intStackPtr]
        val ascending = executor.intStack[executor.intStackPtr + 1] == 1
        val filterMode = executor.intStack[executor.intStackPtr + 2]
        val filterEnabled = executor.intStack[executor.intStackPtr + 3] == 1
        WorldConnection.sortAndClearPings(sortMode, ascending, filterMode, filterEnabled)
    }

    @JvmStatic
    fun setBloom(executor: CS2Executor) {
        var bloom = executor.intStack[--executor.intStackPtr]
        if (bloom < 0 || bloom > 1) {
            bloom = 0
        }
        PreferenceManager.setBloom(bloom == 1)
    }

    @JvmStatic
    fun worldListAutoWorld() {
        LoginManager.prepareHost()
    }

    @JvmStatic
    fun loginQueuePosition(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.loginHandshakeCode
    }

    @JvmStatic
    fun dateMinutesFromRuneday(executor: CS2Executor) {
        val runeday = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (CalenderUtil.convertDaysToMilliseconds(runeday) / 60000L).toInt()
    }

    @JvmStatic
    fun detailGetSafeMode(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.isForcedSafemode) 1  else 0
    }

    @JvmStatic
    fun detailToolkitDefault(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val toolkitId = executor.intStack[executor.intStackPtr]
        val keepGraphicsPreset = executor.intStack[executor.intStackPtr + 1] == 1
        PreferenceManager.preferences!!.toolkit!!.setValue(toolkitId)
        if (!keepGraphicsPreset) {
            PreferenceManager.preferences!!.graphics!!.setValue(0)
        }
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun userFlowFlags(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (Client.machineUid shr 32).toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.machineUid.toInt()
    }

    @JvmStatic
    fun detailGetCpuUsage(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.cpu!!.getValue()
    }

    @JvmStatic
    fun viewportClampFov(executor: CS2Executor) {
        executor.intStackPtr -= 4
        Client.fovScaleMin = executor.intStack[executor.intStackPtr].toShort()
        if (Client.fovScaleMin <= 0) {
            Client.fovScaleMin = 1
        }
        Client.fovScaleMax = executor.intStack[executor.intStackPtr + 1].toShort()
        if (Client.fovScaleMax <= 0) {
            Client.fovScaleMax = 32767
        } else if (Client.fovScaleMax < Client.fovScaleMin) {
            Client.fovScaleMax = Client.fovScaleMin
        }
        Client.aspectFovMin = executor.intStack[executor.intStackPtr + 2].toShort()
        if (Client.aspectFovMin <= 0) {
            Client.aspectFovMin = 1
        }
        Client.aspectFovMax = executor.intStack[executor.intStackPtr + 3].toShort()
        if (Client.aspectFovMax <= 0) {
            Client.aspectFovMax = 32767
        } else if (Client.aspectFovMax < Client.aspectFovMin) {
            Client.aspectFovMax = Client.aspectFovMin
        }
    }

    @JvmStatic
    fun createAvailableRequest(executor: CS2Executor) {
        val email: String? = executor.stringStack[--executor.stringStackPtr] as String?
        PacketEncoder.sendCheckEmailPacket(email)
    }

    @JvmStatic
    fun worldlistNext(executor: CS2Executor) {
        val descriptor: WorldDescriptor? = WorldConnection.getNextWorldDescriptor()
        if (descriptor != null) {
            executor.intStack[++executor.intStackPtr - 1] = descriptor.worldNumber
            executor.intStack[++executor.intStackPtr - 1] = descriptor.flags
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.unknown
            val world: WorldMeta? = descriptor.getWorld()
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
    fun detailGetLoadingScreenType(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.gametipStartStagePreference!!.getValue()
    }

    @JvmStatic
    fun loginResetReply() {
        if (!LoginManager.isLoginInProgress()) {
            LoginManager.resetLoginState()
        }
    }

    @JvmStatic
    fun worldlistStart(executor: CS2Executor) {
        val descriptor: WorldDescriptor? = WorldDescriptor.getFirstWorldDescriptor()
        if (descriptor != null) {
            executor.intStack[++executor.intStackPtr - 1] = descriptor.worldNumber
            executor.intStack[++executor.intStackPtr - 1] = descriptor.flags
            executor.stringStack[++executor.stringStackPtr - 1] = descriptor.unknown
            val world: WorldMeta? = descriptor.getWorld()
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
    fun lobbyEnterGame() {
        LoginManager.returnToLoginScreen()
    }

    @JvmStatic
    fun createConnectReply(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = AccountCreationResponseOpcodes.getReceivedResponse().getValue()
    }

    @JvmStatic
    fun detailLightDetailHigh(executor: CS2Executor) {
        PreferenceManager.preferences!!.lightDetail!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        GraphicsCache.resetGraphicCaches()
        Resource.mapRegion!!.getEnvironmentManager().requestDirectEnvironmentTransition()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailSkyDetail(executor: CS2Executor) {
        val skyboxes = executor.intStack[--executor.intStackPtr]
        if (skyboxes != PreferenceManager.preferences!!.skyboxes!!.getValue()) {
            PreferenceManager.preferences!!.skyboxes!!.setValue(skyboxes)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun createUnder13(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.hasJustCreatedAccount) 1  else 0
    }

    @JvmStatic
    fun loginLastTransferReply(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.lastDisconnectResponse
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.lastDisconnectArg2
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.lastDisconnectArg1
        StaticMethods.lastDisconnectResponse = -2
        StaticMethods.lastDisconnectArg2 = -1
        StaticMethods.lastDisconnectArg1 = -1
    }

    @JvmStatic
    fun loginCancel() {
        LoginManager.cancelLogin()
    }

    @JvmStatic
    fun setEffectVolume(executor: CS2Executor) {
        PreferenceManager.preferences!!.soundEffectVolume!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailFog(executor: CS2Executor) {
        PreferenceManager.preferences!!.fog!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun viewportGetZoom(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.cameraZoomMin.toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.cameraZoomMax.toInt()
    }

    @JvmStatic
    fun detailGetMaxScreenSize(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.maxScreenSize!!.getValue()
    }

    @JvmStatic
    fun setAntiAliasingPreview(executor: CS2Executor) {
        PreferenceManager.preferences!!.antiAliasingPreview!!.setValue(executor.intStack[--executor.intStackPtr])
        Client.loadAssets(PreferenceManager.preferences!!.activeToolkit!!.getValue(), false)
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun detailBgsoundVol(executor: CS2Executor) {
        PreferenceManager.preferences!!.ambientSoundVolume!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailToolkit(executor: CS2Executor) {
        var toolkitId = executor.intStack[--executor.intStackPtr]
        if (toolkitId !in ToolkitPreference.AVAILABLE) {
            toolkitId = ToolkitPreference.SAFE_MODE
        }
        Client.loadAssets(toolkitId, false)
    }

    @JvmStatic
    fun detailGrounddecorOn(executor: CS2Executor) {
        PreferenceManager.preferences!!.groundDecoration!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun loginInProgress(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.isLoginInProgress()) 1  else 0
    }

    @JvmStatic
    fun createGetEmail(executor: CS2Executor) {
        if (Client.customStringParam != null) {
            executor.stringStack[++executor.stringStackPtr - 1] = Client.customStringParam
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        }
    }

    @JvmStatic
    fun detailIdleAnims(executor: CS2Executor) {
        PreferenceManager.preferences!!.idleAnimations!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun detailIdleAnimsMany(executor: CS2Executor) {
        PreferenceManager.preferences!!.idleAnimations!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun loginDisallowTrigger(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.loginResponseExtraInfo
    }

    @JvmStatic
    fun loginDisallowResult(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.loginResponseDetail
    }

    @JvmStatic
    fun dateMinutes(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (Utils.time() / 60000L).toInt()
    }

    @JvmStatic
    fun cam2IsEnabled(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Camera.cameraCalculationType == 2) 1 else 0
    }

    @JvmStatic
    fun detailGetDefaultToolkit(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.defaultToolkit!!.getValue()
    }

    @JvmStatic
    fun createCreateRequest(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStackPtr -= 2
        PacketEncoder.sendSignUpForm(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1] == 1)
    }

    @JvmStatic
    fun viewportSetFov(executor: CS2Executor) {
        executor.intStackPtr -= 2
        Client.viewportFovMin = executor.intStack[executor.intStackPtr].toShort()
        if (Client.viewportFovMin <= 0) {
            Client.viewportFovMin = 256
        }
        Client.viewportFovMax = executor.intStack[executor.intStackPtr + 1].toShort()
        if (Client.viewportFovMax <= 0) {
            Client.viewportFovMax = 205
        }
    }

    @JvmStatic
    fun detailGetBgSoundVol(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.ambientSoundVolume!!.getValue()
    }

    @JvmStatic
    fun detailGetBrightness(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.brightness!!.getValue()
    }

    @JvmStatic
    fun detailGetGrounddecorOn(executor: CS2Executor) {
        val intStack: IntArray? = executor.intStack
        val index = ++executor.intStackPtr - 1
        val enabled = if (PreferenceManager.preferences!!.groundDecoration!!.getValue() == 1) 1 else 0
        intStack!![index] = enabled
    }

    @JvmStatic
    fun currentCreateAccountRequest(): CreateAccountRequest? {
        return if (AccountCreationStage.currentRequest == null) CreateAccountRequest.NOT_STARTED else AccountCreationStage.currentRequest
    }

}
