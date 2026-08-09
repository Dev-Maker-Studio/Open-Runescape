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

object WorldMapUserDetailOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.WORLDLIST_PINGWORLDS -> setPingWorlds(exec)
            ClientScriptOpCode.WORLDLIST_SPECIFIC_THISWORLD -> method3039(exec)
            ClientScriptOpCode.IF_DEBUG_GETOPENIFCOUNT -> ifDebugGetOpenIfCount(exec)
            ClientScriptOpCode.IF_DEBUG_GETOPENIFID -> ifDebugGetOpenIfId(exec)
            ClientScriptOpCode.IF_DEBUG_GETNAME -> ifDebugGetName(exec)
            ClientScriptOpCode.IF_DEBUG_GETCOMCOUNT -> ifDebugGetComCount(exec)
            ClientScriptOpCode.IF_DEBUG_GETCOMNAME -> ifDebugGetComName(exec)
            ClientScriptOpCode.IF_DEBUG_GETSERVERTRIGGERS -> ifDebugGetServerTriggers(exec)
            ClientScriptOpCode.NOOP -> method7187()
            ClientScriptOpCode.IF_DEBUG_BUTTON1 -> ifButton1(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON2 -> ifButton2(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON3 -> ifButton3(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON4 -> ifButton4(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON5 -> ifButton5(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON6 -> ifButton6(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON7 -> ifButton7(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON8 -> ifButton8(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON9 -> ifButton9(exec)
            ClientScriptOpCode.IF_DEBUG_BUTTON10 -> ifButton10(exec)
            ClientScriptOpCode.IF_DEBUG_TARGET -> ifButtonT(exec)
            ClientScriptOpCode.OPCOUNT -> opcount(exec)
            ClientScriptOpCode.MEC_TEXT -> mecText(exec)
            ClientScriptOpCode.MEC_SPRITE -> mecSprite(exec)
            ClientScriptOpCode.MEC_TEXTSIZE -> mecTextSize(exec)
            ClientScriptOpCode.MEC_CATEGORY -> mecCategory(exec)
            ClientScriptOpCode.MEC_PARAM -> getWorldMapParam(exec)
            ClientScriptOpCode.USERDETAIL_QUICKCHAT -> userDetailQuickchat(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_MEMBERSHIP -> userDetailLobbyMembership(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_RECOVERYDAY -> userDetailLobbyRecoveryDay(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_UNREADMESSAGES -> userDetailLobbyUnreadMessages(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LASTLOGINDAY -> userDetailLobbyLastLoginDay(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LASTLOGINADDRESS -> userDetailLobbyLastLoginAddress(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_EMAILSTATUS -> userDetailLobbyEmailStatus(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_CCEXPIRY -> userDetailLobbyCcExpiry(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_GRACEEXPIRY -> userDetailLobbyGraceExpiry(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_DOBREQUESTED -> userDetailLobbyDobRequested(exec)
            ClientScriptOpCode.USERDETAIL_DOB -> userDetailDob(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_MEMBERSSTATS -> userDetailLobbyMembersStats(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_PLAYAGE -> userDetailLobbyPlayage(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_JCOINS_BALANCE -> getJcoins(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LOYALTY_ENABLED -> userDetailLobbyLoyaltyEnabled(exec)
            ClientScriptOpCode.USERDETAIL_LOBBY_LOYALTY_BALANCE -> userDetailLobbyLoyaltyBalance(exec)
            ClientScriptOpCode.CHOOSE_RENDER_TYPE_ENTER -> enterDisplaymode(exec)
            ClientScriptOpCode.AUTOSETUP_SETHIGH -> autoSetupSetHigh()
            ClientScriptOpCode.AUTOSETUP_SETMEDIUM -> autoSetupSetMedium()
            ClientScriptOpCode.AUTOSETUP_SETLOW -> autoSetupSetlow()
            ClientScriptOpCode.AUTOSETUP_SETMIN -> autoSetupSetmin()
            ClientScriptOpCode.DETAIL_GRAPHICS -> detailGraphics()
            ClientScriptOpCode.CHANGE_RENDER_APPLY -> computeGraphicsMode()
            ClientScriptOpCode.DETAILGET_GRAPHICS -> detailGetGraphics(exec)
            ClientScriptOpCode.SOUND_VORBIS_PRELOAD -> soundVorbisPreload(exec)
            ClientScriptOpCode.SOUND_VORBIS_STOP -> soundVorbisStop(exec)
            ClientScriptOpCode.SOUND_VORBIS_PLAY -> soundVorbisPlay(exec)
            ClientScriptOpCode.VIDEOADVERT_ISSUPPORTED -> videoAdvertHasFinished(exec)
            ClientScriptOpCode.BROWSER_AGECHECK -> method3170(exec)
            ClientScriptOpCode.BROWSER_REFRESH -> method7962()
            ClientScriptOpCode.BROWSER_FOCUS -> method14839()
            ClientScriptOpCode.BROWSER_ISSUPPORTED -> method15406(exec)
            ClientScriptOpCode.BROWSER_ISPAGEOPEN -> method572(exec)
            ClientScriptOpCode.BROWSER_HASPLUGIN -> method6350(exec)
            ClientScriptOpCode.BROWSER_SETPAGE -> method2597(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by WorldMapUserDetailOps")
        }
    }

    @JvmStatic
    fun method2597(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
    }

    @JvmStatic
    fun mecSprite(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val worldmapareadefs3: MECType? = TypeLists.MEC!!.list(value)
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3!!.defaultIconArchive
    }

    @JvmStatic
    fun setPingWorlds(executor: CS2Executor) {
        if (Client.gameState == GameState.UNK_0)
            WorldConnection.PING_WORLDS = executor.intStack[--executor.intStackPtr] == 1
    }

    @JvmStatic
    fun ifButton4(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(4, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun enterDisplaymode(executor: CS2Executor) {
        val hraphicsPreference = GameShell.calculateGraphics()
        CS2Interpreter.lastEnteredGraphicsMode = PreferenceManager.preferences!!.activeToolkit!!.getValue()
        executor.intStack[++executor.intStackPtr - 1] = CS2Interpreter.lastEnteredGraphicsMode
        executor.intStack[++executor.intStackPtr - 1] = hraphicsPreference
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun method3170(executor: CS2Executor) {
        val flag = false
        val param = executor.intStack[--executor.intStackPtr]
        executor.intStack[++executor.intStackPtr - 1] = if (flag) 1  else 0
    }

    @JvmStatic
    fun mecText(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val worldmapareadefs3: MECType? = TypeLists.MEC!!.list(value)
        if (worldmapareadefs3!!.areaName == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            executor.stringStack[++executor.stringStackPtr - 1] = worldmapareadefs3!!.areaName
        }
    }

    @JvmStatic
    fun userDetailLobbyEmailStatus(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.EMAIL_STATUS
    }

    @JvmStatic
    fun userDetailLobbyLastLoginDay(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.LASTLOGINLDAY
    }

    @JvmStatic
    fun userDetailLobbyGraceExpiry(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = FileOnDisk.lobbyGraceExpiry
    }

    @JvmStatic
    fun soundVorbisPreload(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        var i_3 = executor.intStack[executor.intStackPtr + 1]
        if (i_2 != -1) {
            if (i_3 > 255) {
                i_3 = 255
            } else if (i_3 < 0) {
                i_3 = 0
            }
            VorbisSoundManager.preloadVorbisSound(i_2, i_3, false)
        }
    }

    @JvmStatic
    fun userDetailQuickchat(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Client.USERDETAIL_QUICKCHAT && !Client.VERIFIED_EMAIL_ADDRESS) 1 else 0
    }

    @JvmStatic
    fun method6350(executor: CS2Executor) {
        val flag = false
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStack[++executor.intStackPtr - 1] = if (flag) 1  else 0
    }

    @JvmStatic
    fun ifDebugGetName(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (Interface.INTERFACES!![value] == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            val text: String? = Interface.INTERFACES!![value]!!.components!![0]!!.name
            if (text == null) {
                executor.stringStack[++executor.stringStackPtr - 1] = ""
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = text.substring(0, text.indexOf(58.toChar()))
            }
        }
    }

    @JvmStatic
    fun ifButton2(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(2, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun ifDebugGetComCount(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (Interface.INTERFACES!![value] == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Interface.INTERFACES!![value]!!.components!!.size
        }
    }

    @JvmStatic
    fun soundVorbisStop(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (value != -1) {
            StaticMethods.stopVorbisStream(value)
        }
    }

    @JvmStatic
    fun autoSetupSetlow() {
        PreferenceManager.loadLowDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun getWorldMapParam(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val attributedefault4: ParamType? = TypeLists.PARAMS!!.list(i_3)
        if (attributedefault4!!.isString()) {
            executor.stringStack[++executor.stringStackPtr - 1] = TypeLists.MEC!!.list(i_2).getStringParameter(i_3, attributedefault4!!.typeName!!)
        } else {
            executor.intStack[++executor.intStackPtr - 1] = TypeLists.MEC!!.list(i_2).getIntParameter(i_3, attributedefault4!!.defaultInt)
        }
    }

    @JvmStatic
    fun method572(executor: CS2Executor) {
        val flag = true
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        executor.intStack[++executor.intStackPtr - 1] = if (flag) 1  else 0
    }

    @JvmStatic
    fun mecCategory(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val worldmapareadefs3: MECType? = TypeLists.MEC!!.list(value)
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3!!.categoryId
    }

    @JvmStatic
    fun opcount(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = CS2Executor.instructionCount
    }

    @JvmStatic
    fun userDetailLobbyCcExpiry(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.lobbyCreditCardExpiry
    }

    @JvmStatic
    fun mecTextSize(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val worldmapareadefs3: MECType? = TypeLists.MEC!!.list(value)
        executor.intStack[++executor.intStackPtr - 1] = worldmapareadefs3!!.baseTextzoom
    }

    @JvmStatic
    fun userDetailLobbyLoyaltyEnabled(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (QueueIterator.LOYALTY_ENABLED) 1  else 0
    }

    @JvmStatic
    fun ifButtonT(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val component: Component? = Component.listIcomponent((i_2 shl 16) or i_3, i_4)
        InterfaceComponentRenderer.cancelInterfaceSelection()
        assert(component != null)
        val class282Sub106: IFEvents? = Client.getComponentSettings(component!!)
        InterfaceRenderer.setUseOptionFlags(component, class282Sub106!!.getUseOptionFlags(), class282Sub106.interfaceId)
    }

    @JvmStatic
    fun computeGraphicsMode() {
        if (CS2Interpreter.lastEnteredGraphicsMode == 2) {
            GameShell.softwareMode = true
        } else if (CS2Interpreter.lastEnteredGraphicsMode == 1) {
            GameShell.openglMode = true
        } else if (CS2Interpreter.lastEnteredGraphicsMode == 3) {
            GameShell.directxMode = true
        }
    }

    @JvmStatic
    fun ifButton10(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(10, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun detailGraphics() {
        PreferenceManager.preferences!!.graphics!!.setValue(0)
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun autoSetupSetMedium() {
    	PreferenceManager.loadMidDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun ifButton1(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(1, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun soundVorbisPlay(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        if (value != -1) {
            StaticMethods.setVorbisSound(value)
        }
    }

    @JvmStatic
    fun ifDebugGetOpenIfCount(executor: CS2Executor) {
        var value = Client.OPEN_INTERFACES!!.size()
        if (Client.BASE_WINDOW_ID != -1) {
            ++value
        }
        executor.intStack[++executor.intStackPtr - 1] = value
    }

    @JvmStatic
    fun detailGetGraphics(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = PreferenceManager.preferences!!.graphics!!.getValue()
    }

    @JvmStatic
    fun ifDebugGetServerTriggers(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (Interface.INTERFACES!![i_2] == null) {
            executor.intStack[++executor.intStackPtr - 1] = 0
        } else {
            executor.intStack[++executor.intStackPtr - 1] = Interface.INTERFACES!![i_2]!!.components!![i_3]!!.serverTriggers
        }
    }

    @JvmStatic
    fun userDetailDob(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.PLAYER_DOB
    }

    @JvmStatic
    fun autoSetupSetHigh() {
        PreferenceManager.loadHighDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun userDetailLobbyMembership(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = (LoginManager.MEMBERSHIP_END / 60000L).toInt()
        executor.intStack[++executor.intStackPtr - 1] = ((LoginManager.MEMBERSHIP_END - Utils.time() - LoginManager.membershipTimeCorrection) / 60000L).toInt()
        executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.IS_MEMBER_SUBSCRIPTION) 1 else 0
    }

    @JvmStatic
    fun ifButton9(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(9, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun ifButton5(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(5, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun getJcoins(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.JCOINS
    }

    @JvmStatic
    fun autoSetupSetmin() {
        PreferenceManager.loadMinDetailPreferences()
        Resource.mapRegion!!.reload()
        PreferenceManager.savePreferences()
        Client.preferencesSavedOnServer = false
    }

    @JvmStatic
    fun userDetailLobbyRecoveryDay(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.RECOVERYQUESTIONSSETDATE
    }

    @JvmStatic
    fun method3039(executor: CS2Executor) {
        val class217Sub12: WorldDescriptor? = WorldDescriptor.get_world()
        executor.intStack[++executor.intStackPtr - 1] = if (class217Sub12 == null) 0 else class217Sub12!!.flags
    }

    @JvmStatic
    fun ifButton8(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(8, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun userDetailLobbyUnreadMessages(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.MESSAGES
    }

    @JvmStatic
    fun ifDebugGetComName(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        if (Interface.INTERFACES!![i_2] == null) {
            executor.stringStack[++executor.stringStackPtr - 1] = ""
        } else {
            val text: String? = Interface.INTERFACES!![i_2]!!.components!![i_3]!!.name
            if (text == null) {
                executor.stringStack[++executor.stringStackPtr - 1] = ""
            } else {
                executor.stringStack[++executor.stringStackPtr - 1] = text
            }
        }
    }

    @JvmStatic
    fun ifButton6(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(6, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun ifDebugGetOpenIfId(executor: CS2Executor) {
        var value = executor.intStack[--executor.intStackPtr]
        if (Client.BASE_WINDOW_ID != -1) {
            if (value == 0) {
                executor.intStack[++executor.intStackPtr - 1] = Client.BASE_WINDOW_ID
                return
            }
            --value
        }
        var class282Sub443: SubInterface? = Client.OPEN_INTERFACES!!.first()
        while (value-- > 0) {
            class282Sub443 = Client.OPEN_INTERFACES!!.next()
        }
        executor.intStack[++executor.intStackPtr - 1] = class282Sub443!!.subInterfaceId
    }

    @JvmStatic
    fun userDetailLobbyPlayage(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.lobbyPlayAge
    }

    @JvmStatic
    fun userDetailLobbyDobRequested(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (LoginManager.isDobRequested) 1  else 0
    }

    @JvmStatic
    fun method7962() {
    }

    @JvmStatic
    fun method7187() {
    }

    @JvmStatic
    fun userDetailLobbyLoyaltyBalance(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = LoginManager.LOYALTY_BALANCE
    }

    @JvmStatic
    fun videoAdvertHasFinished(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = if (Resource.NATIVE_LIBRARY_LOADER!!.isJagtheoraLoaded()) 1  else 0
    }

    @JvmStatic
    fun method14839() {
    }

    @JvmStatic
    fun userDetailLobbyLastLoginAddress(executor: CS2Executor) {
        var text: String? = null
        if (PacketDecoder.HOSTNAME_IDENTIFIER != null) {
            text = PacketDecoder.HOSTNAME_IDENTIFIER!!.getHostName()
        }
        if (text == null) {
            text = ""
        }
        executor.stringStack[++executor.stringStackPtr - 1] = text
    }

    @JvmStatic
    fun ifButton3(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(3, (i_2 shl 16) or i_3, i_4, "")
    }

    @JvmStatic
    fun method15406(executor: CS2Executor) {
        val flag = true
        executor.intStack[++executor.intStackPtr - 1] = if (flag) 1  else 0
    }

    @JvmStatic
    fun userDetailLobbyMembersStats(executor: CS2Executor) {
        executor.intStack[++executor.intStackPtr - 1] = Client.lobbyMembershipStatus
    }

    @JvmStatic
    fun ifButton7(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        Interface.ifOptionInteract(7, (i_2 shl 16) or i_3, i_4, "")
    }

}
