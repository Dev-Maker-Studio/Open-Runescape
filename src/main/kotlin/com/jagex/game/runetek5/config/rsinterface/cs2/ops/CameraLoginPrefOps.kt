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

object CameraLoginPrefOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.CAM_MODEISFOLLOWPLAYER -> camModeIsFollowPlayer(exec)
            ClientScriptOpCode.LOGIN_REQUEST -> loginRequest(exec)
            ClientScriptOpCode.LOGIN_CONTINUE -> method6695()
            ClientScriptOpCode.LOGIN_RESETREPLY -> loginResetReply()
            ClientScriptOpCode.SEND_VERIFY_EMAIL_PACKET -> sendVerifyEmailPacket(exec)
            ClientScriptOpCode.SEND_SIGNUP_FORM_PACKET -> sendSignupFormPacket(exec)
            ClientScriptOpCode.LOGIN_STARTGAME -> loginStartGame()
            ClientScriptOpCode.CREATE_CREATEREQUEST -> createCreateRequest(exec)
            ClientScriptOpCode.instr6699 -> loginReplyWorld(exec)
            ClientScriptOpCode.LOGIN_QUEUE_POSITION -> loginGetQueuePosition(exec)
            ClientScriptOpCode.CREATE_GET_EMAIL -> createGetEmail(exec)
            ClientScriptOpCode.CREATE_EMAIL_VALIDATE_REPLY -> createEmailValidateReply(exec)
            ClientScriptOpCode.CREATE_REPLY -> createReply(exec)
            ClientScriptOpCode.LOGIN_REPLY -> loginReply(exec)
            ClientScriptOpCode.LOGIN_RETURNTOLOGINSCREEN -> loginReturnToLoginScreen()
            ClientScriptOpCode.instr6305 -> method301(exec)
            ClientScriptOpCode.LOGIN_SWITCHWORLD -> loginSwitchWorld(exec)
            ClientScriptOpCode.LOGOUT -> opLogout()
            ClientScriptOpCode.instr6709 -> method6285(exec)
            ClientScriptOpCode.HAS_BASE64URL -> method2825(exec)
            ClientScriptOpCode.GETMACHINEUID -> method12597(exec)
            ClientScriptOpCode.CREATE_UNDER13 -> method453(exec)
            ClientScriptOpCode.CREATE_STEP_REACHED -> method6280()
            ClientScriptOpCode.LOGIN_DISCONNECT_REPLY -> method3119(exec)
            ClientScriptOpCode.LOGIN_INPROGRESS -> method14669(exec)
            ClientScriptOpCode.LOGIN_HANDSHAKE_CODE -> method2153(exec)
            ClientScriptOpCode.LOGIN_CANCEL -> loginCancel()
            ClientScriptOpCode.LOGIN_REQUEST_SOCIAL_NETWORK -> method6169(exec)
            ClientScriptOpCode.LOGIN_SWITCHTOWORLD -> loginSwitchToWorld(exec)
            ClientScriptOpCode.LOGIN_REPLY_EXTRAINFO -> loginReplyExtraInfo(exec)
            ClientScriptOpCode.GETCUSTOMSTRINGPARAM -> getCustomStringParam(exec)
            ClientScriptOpCode.DETAIL_BRIGHTNESS -> method5047(exec)
            ClientScriptOpCode.DETAIL_REMOVEROOFS_OPTION -> detailRemoveRoofsOption(exec)
            ClientScriptOpCode.DETAIL_GROUNDDECOR_ON -> detailGrounddecorOn(exec)
            ClientScriptOpCode.DETAIL_IDLEANIMS_MANY -> detailIdleAnimsMany(exec)
            ClientScriptOpCode.DETAIL_FLICKERING_ON -> method2568(exec)
            ClientScriptOpCode.DETAIL_SPOTSHADOWS_ON -> detailSpotshadowsOn(exec)
            ClientScriptOpCode.DETAIL_SCENERYSHADOWS -> method3611(exec)
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
            ClientScriptOpCode.DETAIL_CUSTOMCURSOR -> detailCustomCursor(exec)
            ClientScriptOpCode.DETAIL_IDLEANIMS -> detailIdleAnims(exec)
            ClientScriptOpCode.DETAIL_GROUNDBLENDING -> detailGroundBlending(exec)
            ClientScriptOpCode.CHANGE_RENDER -> changeRender(exec)
            ClientScriptOpCode.DETAIL_TOOLKIT -> detailToolkit(exec)
            ClientScriptOpCode.DETAIL_CPU -> detailCpu(exec)
            ClientScriptOpCode.DETAIL_TEXTURES -> detailTextures(exec)
            ClientScriptOpCode.DETAIL_MAXSCREENSIZE -> detailMaxScreenSize(exec)
            ClientScriptOpCode.DETAIL_VOICEOVERVOLUME -> method3200(exec)
            ClientScriptOpCode.DETAIL_LOGINVOL -> detailLoginVol(exec)
            ClientScriptOpCode.DETAIL_GAMETIPSTARTSTAGEPREFERENCE -> method1586(exec)
            ClientScriptOpCode.DETAIL_SKYBOXES -> detailSkyboxes(exec)
            ClientScriptOpCode.DETAILGET_BRIGHTNESS -> method8156(exec)
            ClientScriptOpCode.DETAILGET_REMOVEROOFS_OPTION -> detailGetRemoveRoofsOption(exec)
            ClientScriptOpCode.DETAILGET_GROUNDDECOR_ON -> detailGetGrounddecorOn(exec)
            ClientScriptOpCode.DETAILGET_IDLEANIMS -> detailGetIdleAnims(exec)
            ClientScriptOpCode.DETAILGET_FLICKERING_ON -> method12560(exec)
            ClientScriptOpCode.DETAILGET_SPOTSHADOWS_ON -> detailGetSpotshadowsOn(exec)
            ClientScriptOpCode.DETAILGET_SCENERYSHADOWS -> method6675(exec)
            ClientScriptOpCode.DETAILGET_LIGHTDETAIL_HIGH -> detailGetLightDetailHigh(exec)
            ClientScriptOpCode.DETAILGET_WATERDETAIL_HIGH -> getWaterPreference(exec)
            ClientScriptOpCode.DETAILGET_FOG_ON -> pushFogPreference(exec)
            ClientScriptOpCode.DETAILGET_ANTIALIASINGPREVIEW -> detailGetAntialiasingPreview(exec)
            ClientScriptOpCode.DETAILGET_STEREO -> detailGetStereo(exec)
            ClientScriptOpCode.DETAILGET_SOUNDEFFECTVOLUME -> detailGetSoundEffectVolume(exec)
            ClientScriptOpCode.DETAILGET_MUSICVOL -> detailGetMusicVolume(exec)
            ClientScriptOpCode.DETAILGET_AMBIENTSOUNDVOLUME -> method13404(exec)
            ClientScriptOpCode.DETAILGET_PARTICLES -> detailGetParticles(exec)
            ClientScriptOpCode.DETAILGET_ANTIALIASING -> detailGetAntialiasing(exec)
            ClientScriptOpCode.DETAILGET_MAPSIZE -> detailGetMapSize(exec)
            ClientScriptOpCode.DETAILGET_BLOOM -> detailGetBloom(exec)
            ClientScriptOpCode.DETAILGET_CUSTOMCURSORS -> detailGetCustomCursors(exec)
            ClientScriptOpCode.DETAILGET_IDLEANIMS_MANY -> detailGetIdleAnimsMany(exec)
            ClientScriptOpCode.DETAILGET_GROUNDBLENDING -> detailGetGroundBlending(exec)
            ClientScriptOpCode.DETAILGET_ACTIVETOOLKIT -> detailGetActiveToolkit(exec)
            ClientScriptOpCode.DETAILGET_TOOLKIT -> detailGetToolkit(exec)
            ClientScriptOpCode.DETAILGET_CPU -> detailGetCpu(exec)
            ClientScriptOpCode.DETAILGET_TEXTURING -> detailGetTexturing(exec)
            ClientScriptOpCode.DETAILGET_CPUUSAGE -> method2549(exec)
            ClientScriptOpCode.DETAILGET_MAXSCREENSIZE -> detailGetMaxScreenSize(exec)
            ClientScriptOpCode.DETAILGET_VOICEOVERVOLUME -> method5734(exec)
            ClientScriptOpCode.DETAILGET_SOUNDLEVEL -> detailGetSoundLevel(exec)
            ClientScriptOpCode.instr6784 -> method6238(exec)
            ClientScriptOpCode.DETAILGET_GAMETIPSTARTSTAGEPREFERENCE -> method4324(exec)
            ClientScriptOpCode.DETAILGET_DEFAULTTOOLKIT -> detailGetDefaultToolkit(exec)
            ClientScriptOpCode.instr6775 -> detailGetToolkitDefault(exec)
            ClientScriptOpCode.instr6097 -> method745(exec)
            ClientScriptOpCode.DETAILGET_SKYBOXES -> detailGetSkyboxes(exec)
            ClientScriptOpCode.VIEWPORT_SETFOV -> method1643(exec)
            ClientScriptOpCode.VIEWPORT_SETZOOM -> method1815(exec)
            ClientScriptOpCode.VIEWPORT_CLAMPFOV -> method15404(exec)
            ClientScriptOpCode.VIEWPORT_GETEFFECTIVESIZE -> method4882(exec)
            ClientScriptOpCode.VIEWPORT_GETZOOM -> method4973(exec)
            ClientScriptOpCode.VIEWPORT_GETFOV -> method2587(exec)
            ClientScriptOpCode.DATE_MINUTES -> dateMinutes(exec)
            ClientScriptOpCode.DATE_RUNEDAY -> dateRuneday(exec)
            ClientScriptOpCode.DATE_RUNEDAY_FROMDATE -> dateRunedayFromdate(exec)
            ClientScriptOpCode.DATE_YEAR -> dateYear(exec)
            ClientScriptOpCode.DATE_ISLEAPYEAR -> method1479(exec)
            ClientScriptOpCode.DATE_RUNEDAY_TODATE -> dateRunedayToDate(exec)
            ClientScriptOpCode.DATE_MINUTES_FROMRUNEDAY -> dateMinutesFromRuneday(exec)
            ClientScriptOpCode.WORLDLIST_FETCH -> worldlistFetch(exec)
            ClientScriptOpCode.WORLDLIST_START -> worldlistStart(exec)
            ClientScriptOpCode.WORLDLIST_NEXT -> worldlistNext(exec)
            ClientScriptOpCode.SETGAMEHOST -> opSetGameHost(exec)
            ClientScriptOpCode.WORLDLIST_SPECIFIC -> method1387(exec)
            ClientScriptOpCode.WORLDLIST_SORT -> worldlistSort(exec)
            ClientScriptOpCode.PREPAREGAMEHOST -> method5816()
            else -> throw IllegalStateException("opcode $operation is not handled by CameraLoginPrefOps")
        }
    }

    @JvmStatic
    fun detailParticles(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        PreferenceManager.preferences!!.particles!!.setValue(value)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun getWaterPreference(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.water!!.getValue() == 2) 1 else 0
    }

    @JvmStatic
    fun method1586(executor: CS2Executor) {
        var value = executor.intStack[--executor.intStackPtr]
        if (value > 255 || value < 0) {
            value = 0
        }
        if (value != PreferenceManager.preferences!!.gametipStartStagePreference!!.getValue()) {
            PreferenceManager.preferences!!.gametipStartStagePreference!!.setValue(value)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun loginStartGame() {
        GameState.startGameLogin()
    }

    @JvmStatic
    fun method6280() {
        Client.hasJustCreatedAccount = true
    }

    @JvmStatic
    fun detailGetMapSize(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.mapSize!!.getValue()
    }

    @JvmStatic
    fun detailGetSoundEffectVolume(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.soundEffectVolume!!.getValue()
    }

    @JvmStatic
    fun method4882(executor: CS2Executor) {
        AbstractToolkit.calculateGameArea(0, 0, Client.GAME_SCREEN_INTERFACE!!.width, Client.GAME_SCREEN_INTERFACE!!.height, false)
        executor.intStack[++executor.intStackPtr - 1] = Client.gameAreaWidth
        executor.intStack[++executor.intStackPtr - 1] = Client.gameAreaHeight
    }

    @JvmStatic
    fun detailGetAntialiasing(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasing!!.getValue()
    }

    @JvmStatic
    fun detailCustomCursor(executor: CS2Executor) {
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
    fun method745(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.safemodeForcedDuringStartup) 1  else 0
    }

    @JvmStatic
    fun detailTextures(executor: CS2Executor) {
        PreferenceManager.preferences!!.textures!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        PreferenceManager.savePreferences()
        GraphicsCache.resetGraphicCaches()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun loginRequest(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        LoginManager.directLogin(string_2!!, string_3!!)
    }

    @JvmStatic
    fun method6675(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.sceneryShadows!!.getValue()
    }

    @JvmStatic
    fun detailMaxScreenSize(executor: CS2Executor) {
        PreferenceManager.preferences!!.maxScreenSize!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.needsResize = true
    }

    @JvmStatic
    fun detailGetIdleAnimsMany(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.idleAnimations!!.getValue()
    }

    @JvmStatic
    fun detailGetIdleAnims(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.idleAnimations!!.getValue()
    }

    @JvmStatic
    fun detailGetParticles(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = ParticleSystemManager.getDetailLevel()
    }

    @JvmStatic
    fun opLogout() {
        Client.logout(false)
    }

    @JvmStatic
    fun opSetGameHost(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (Client.gameState == GameState.UNK_0 && !LoginManager.isLoginInProgress()) {
            executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.setGameHost(value, text!!)) 1 else 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun detailGetSoundLevel(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.soundLevel!!.getValue()
    }

    @JvmStatic
    fun method6285(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.lobbyLoginResponse
    }

    @JvmStatic
    fun method1387(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val class217Sub13: WorldDescriptor? = WorldDescriptor.getWorld(value)
        if (class217Sub13 != null) {
            executor.intStack[++executor.intStackPtr - 1] = class217Sub13!!.flags
            executor.stringStack[++executor.stringStackPtr - 1] = class217Sub13!!.unknown
            val class2134: WorldMeta? = class217Sub13!!.getWorld()
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
    fun method12560(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.flickeringEffects!!.getValue() == 1) 1 else 0
    }

    @JvmStatic
    fun detailGetActiveToolkit(executor: CS2Executor) {
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
    fun method6695() {
        LoginManager.confirmLoginContinue()
    }

    @JvmStatic
    fun createGetEmail(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = method2598()!!.getValue()
    }

    @JvmStatic
    fun saveAntiAliasingPreference(executor: CS2Executor) {
        PreferenceManager.preferences!!.antiAliasing!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun createCreateRequest(executor: CS2Executor) {
        PacketEncoder.sendAccountCreationStage(executor.intStack[--executor.intStackPtr])
    }

    @JvmStatic
    fun detailGetSkyboxes(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.skyboxes!!.getValue()
    }

    @JvmStatic
    fun method1479(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        var flag = true
        if (value < 0) {
            flag = (value + 1) % 4 == 0
        } else if (value < 1582) {
            flag = value % 4 == 0
        } else if (value % 4 != 0) {
            flag = false
        } else if (value % 100 != 0) {
        } else if (value % 400 != 0) {
            flag = false
        }
        executor.intStack[++executor.intStackPtr - 1] = if (flag) 1  else 0
    }

    @JvmStatic
    fun detailGetRemoveRoofsOption(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (PreferenceManager.preferences!!.removeRoofs!!.getValue() == 2) 1 else 0
    }

    @JvmStatic
    fun method5734(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.voiceOverVolume!!.getValue()
    }

    @JvmStatic
    fun method2568(executor: CS2Executor) {
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val longValue = CalenderUtil.getGmtNoon(i_2, i_3, i_4)
        var i_7 = StaticMethods.millisToJagexDays(longValue)
        if (i_4 < 1970) {
            --i_7
        }
        executor.intStack[++executor.intStackPtr - 1] = i_7
    }

    @JvmStatic
    fun method6169(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        LoginManager.snLogin(value)
    }

    @JvmStatic
    fun method301(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.worldLoginResponse
    }

    @JvmStatic
    fun loginSwitchWorld(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        val string_2: String? = executor.stringStack[executor.stringStackPtr] as String?
        val string_3: String? = executor.stringStack[executor.stringStackPtr + 1] as String?
        LoginManager.switchWorldAndLogin(string_2!!, string_3!!)
    }

    @JvmStatic
    fun detailGetToolkitDefault(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (SystemInfo.SYSTEM_INFO!!.ram >= 512 && !Client.isForcedSafemode && !Client.renderWithCpu) 1 else 0
    }

    @JvmStatic
    fun detailGetMusicVolume(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.musicVolume!!.getValue()
    }

    @JvmStatic
    fun detailWaterDetailHigh(executor: CS2Executor) {
        PreferenceManager.preferences!!.water!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 2 else 0)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun loginSwitchToWorld(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        StaticMethods.switchToWorldLogin(value)
    }

    @JvmStatic
    fun detailRemoveRoofsOptionOverride(executor: CS2Executor) {
        val value = PreferenceManager.preferences!!.removeRoofs!!.getValue()
        PreferenceManager.preferences!!.removeRoofsOptionOverride!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 0 else value)
        Scene.setRoofOverride()
    }

    @JvmStatic
    fun method3611(executor: CS2Executor) {
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
        val value = executor.intStack[--executor.intStackPtr]
        val stack: IntArray? = CalenderUtil.getLocalDdmmyyyy(value)
        ArrayUtils.arrayCopy(stack!!, 0, executor.intStack, executor.intStackPtr, 3)
        executor.intStackPtr += 3
    }

    @JvmStatic
    fun detailGroundBlending(executor: CS2Executor) {
        PreferenceManager.preferences!!.goundBlending!!.setValue(if (executor.intStack[--executor.intStackPtr] != 0) 1 else 0)
        PreferenceManager.savePreferences()
        Resource.mapRegion!!.reload()
    }

    @JvmStatic
    fun detailCpu(executor: CS2Executor) {
        PreferenceManager.preferences!!.cpu!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
    }

    @JvmStatic
    fun method2587(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.viewportFovMin.toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.viewportFovMax.toInt()
    }

    @JvmStatic
    fun detailGetAntialiasingPreview(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.antiAliasingPreview!!.getValue()
    }

    @JvmStatic
    fun detailRemoveRoofsOption(executor: CS2Executor) {
        val flag = executor.intStack[--executor.intStackPtr] == 1
        PreferenceManager.preferences!!.removeRoofs!!.setValue(if (flag) 2  else 1)
        PreferenceManager.preferences!!.removeRoofsOptionOverride!!.setValue(if (flag) 2  else 1)
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
    fun method2549(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = DisplayModeDetails.getProfilingPerformance(PreferenceManager.preferences!!.activeToolkit!!.getValue(), 200)
    }

    @JvmStatic
    fun detailMusicVol(executor: CS2Executor) {
        val i_2 = executor.intStack[--executor.intStackPtr]
        val i_3 = PreferenceManager.preferences!!.musicVolume!!.getValue()
        if (i_2 != i_3) {
            if (GameState.isRenderingScene(Client.gameState)) {
                if (i_3 == 0 && AreaSoundPlayer.currentMusicArchive != -1) {
                    AreaSoundPlayer.playMusic(Resource.MUSIC!!, AreaSoundPlayer.currentMusicArchive, i_2)
                    SongPlayer.resetChannelVolumes()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else if (i_2 == 0) {
                	SoundResourceManager.discardSongData()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else {
                    SoundEffectPlayer.setMusicVolume(i_2)
                }
            }
            PreferenceManager.preferences!!.musicVolume!!.setValue(i_2)
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
    fun method2825(executor: CS2Executor) {
        if (Client.base64Url != null) {
            executor.intStack[++executor.intStackPtr - 1] = 1
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 0
        }
    }

    @JvmStatic
    fun method5047(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        PreferenceManager.preferences!!.brightness!!.setValue(value)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun loginGetQueuePosition(executor: CS2Executor) {
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
        val i_2 = executor.intStack[--executor.intStackPtr]
        val i_3 = PreferenceManager.preferences!!.soundLevel!!.getValue()
        if (i_3 != i_2 && AreaSoundPlayer.titlescreenMusicArchive == AreaSoundPlayer.currentMusicArchive) {
            if (!GameState.isRenderingScene(Client.gameState)) {
                if (i_3 == 0) {
                    AreaSoundPlayer.playMusic(Resource.MUSIC!!, AreaSoundPlayer.currentMusicArchive, i_2)
                    SongPlayer.resetChannelVolumes()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else if (i_2 == 0) {
                	SoundResourceManager.discardSongData()
                    SoundEffectPlayer.soundJinglePlaying = false
                } else {
                    SoundEffectPlayer.setMusicVolume(i_2)
                }
            }
            PreferenceManager.preferences!!.soundLevel!!.setValue(i_2)
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
                val tcpmessage2: TcpPacket? = PacketEncoder.createPacket(ClientProt.REQUEST_WORLD_LIST, Client.LOBBY_CONNECTION_CONTEXT.isaac)
                tcpmessage2!!.buffer.writeInt(StaticMethods.WORLD_LIST_VERSION)
                Client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage2)
                executor.intStack[++executor.intStackPtr - 1] = 0
            }
        } else {
            executor.intStack[++executor.intStackPtr - 1] = 1
        }
    }

    @JvmStatic
    fun method3200(executor: CS2Executor) {
        PreferenceManager.preferences!!.voiceOverVolume!!.setValue(executor.intStack[--executor.intStackPtr])
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun detailGetToolkit(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.toolkit!!.getValue()
    }

    @JvmStatic
    fun loginReplyWorld(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.worldLoginResponse
    }

    @JvmStatic
    fun method1815(executor: CS2Executor) {
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
        val i_2 = executor.intStack[executor.intStackPtr]
        val bool3 = executor.intStack[executor.intStackPtr + 1] == 1
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val bool5 = executor.intStack[executor.intStackPtr + 3] == 1
        WorldConnection.sortAndClearPings(i_2, bool3, i_4, bool5)
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
    fun method5816() {
        LoginManager.prepareHost()
    }

    @JvmStatic
    fun method2153(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = StaticMethods.loginHandshakeCode
    }

    @JvmStatic
    fun dateMinutesFromRuneday(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = (CalenderUtil.convertDaysToMilliseconds(value) / 60000L).toInt()
    }

    @JvmStatic
    fun method6238(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.isForcedSafemode) 1  else 0
    }

    @JvmStatic
    fun detailToolkit(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val value = executor.intStack[executor.intStackPtr]
        val flag = executor.intStack[executor.intStackPtr + 1] == 1
        PreferenceManager.preferences!!.toolkit!!.setValue(value)
        if (!flag) {
            PreferenceManager.preferences!!.graphics!!.setValue(0)
        }
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method12597(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (Client.machineUid shr 32).toInt()
        executor.intStack[++executor.intStackPtr - 1] = Client.machineUid.toInt()
    }

    @JvmStatic
    fun detailGetCpu(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.cpu!!.getValue()
    }

    @JvmStatic
    fun method15404(executor: CS2Executor) {
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
    fun sendVerifyEmailPacket(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        PacketEncoder.sendCheckEmailPacket(text)
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
    fun method4324(executor: CS2Executor) {
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
    fun loginReturnToLoginScreen() {
        LoginManager.returnToLoginScreen()
    }

    @JvmStatic
    fun createReply(executor: CS2Executor) {
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
    fun detailSkyboxes(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (value != PreferenceManager.preferences!!.skyboxes!!.getValue()) {
            PreferenceManager.preferences!!.skyboxes!!.setValue(value)
            PreferenceManager.savePreferences()
            Client.preferencesSavedOnServer = false
        }
    }

    @JvmStatic
    fun method453(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.hasJustCreatedAccount) 1  else 0
    }

    @JvmStatic
    fun method3119(executor: CS2Executor) {
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
    fun method4973(executor: CS2Executor) {
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
    fun changeRender(executor: CS2Executor) {
        var value = executor.intStack[--executor.intStackPtr]
        if (value < 0 || value > 5) {
            value = 2
        }
        Client.loadAssets(value, false)
    }

    @JvmStatic
    fun detailGrounddecorOn(executor: CS2Executor) {
        PreferenceManager.preferences!!.groundDecoration!!.setValue(if (executor.intStack[--executor.intStackPtr] == 1) 1 else 0)
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method14669(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.isLoginInProgress()) 1  else 0
    }

    @JvmStatic
    fun getCustomStringParam(executor: CS2Executor) {
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
    fun loginReplyExtraInfo(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.loginResponseExtraInfo
    }

    @JvmStatic
    fun loginReply(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.loginResponseDetail
    }

    @JvmStatic
    fun dateMinutes(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (Utils.time() / 60000L).toInt()
    }

    @JvmStatic
    fun camModeIsFollowPlayer(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Camera.cameraCalculationType == 2) 1 else 0
    }

    @JvmStatic
    fun detailGetDefaultToolkit(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.defaultToolkit!!.getValue()
    }

    @JvmStatic
    fun sendSignupFormPacket(executor: CS2Executor) {
        executor.stringStackPtr -= 2
        executor.intStackPtr -= 2
        PacketEncoder.sendSignUpForm(executor.stringStack[executor.stringStackPtr] as String?, executor.stringStack[executor.stringStackPtr + 1] as String?, executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1] == 1)
    }

    @JvmStatic
    fun method1643(executor: CS2Executor) {
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
    fun method13404(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.ambientSoundVolume!!.getValue()
    }

    @JvmStatic
    fun method8156(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.brightness!!.getValue()
    }

    @JvmStatic
    fun detailGetGrounddecorOn(executor: CS2Executor) {
        val stack: IntArray? = executor.intStack
        val value = ++executor.intStackPtr - 1
        val b_4 = if (PreferenceManager.preferences!!.groundDecoration!!.getValue() == 1) 1 else 0
        stack!![value] = b_4
    }

    @JvmStatic
    fun method2598(): CreateAccountRequest? {
        return if (AccountCreationStage.currentRequest == null) CreateAccountRequest.NOT_STARTED else AccountCreationStage.currentRequest
    }

}
