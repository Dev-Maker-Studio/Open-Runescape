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

object DialogOps {

    @JvmStatic
    fun dispatch(operation: ClientScriptOpCode, exec: CS2Executor) {
        when (operation) {
            ClientScriptOpCode.RESET_MYPLAYER_ANIMS -> resetMyPlayerAnims(exec)
            ClientScriptOpCode.IF_CLOSE -> ifClose()
            ClientScriptOpCode.RESUME_COUNTDIALOG -> resumeCountDialog(exec)
            ClientScriptOpCode.RESUME_NAMEDIALOG -> resumeNamedDialog(exec)
            ClientScriptOpCode.RESUME_STRINGDIALOG -> resumeStringDialog(exec)
            ClientScriptOpCode.OPPLAYER -> opplayer(exec)
            ClientScriptOpCode.IF_DRAGPICKUP -> ifDragPickup(exec)
            ClientScriptOpCode.CC_DRAGPICKUP -> ccDragPickup(exec)
            ClientScriptOpCode.RESUME_OBJDIALOG -> resumeObjdialog(exec)
            ClientScriptOpCode.IF_OPENSUBCLIENT -> ifOpenSubClient(exec)
            ClientScriptOpCode.IF_CLOSESUBCLIENT -> ifCloseSubClient(exec)
            ClientScriptOpCode.OPPLAYERT -> opplayerT(exec)
            ClientScriptOpCode.MES_TYPED -> appendTypedMessage(exec)
            ClientScriptOpCode.SETUP_MESSAGEBOX -> loadMessagebox(exec)
            ClientScriptOpCode.RESUME_HSLDIALOG -> resumeHSLDialog(exec)
            ClientScriptOpCode.RESUME_CLANFORUMQFCDIALOG -> resumeClanForumQFCDialog(exec)
            ClientScriptOpCode.SOUND_SYNTH -> playSoundSynth(exec)
            ClientScriptOpCode.SOUND_SONG -> soundSong(exec)
            ClientScriptOpCode.SOUND_JINGLE -> soundJingle(exec)
            ClientScriptOpCode.SOUND_SYNTH_VOLUME -> soundSynthVolume(exec)
            ClientScriptOpCode.SOUND_SONG_VOLUME -> soundSongVolume(exec)
            ClientScriptOpCode.SOUND_JINGLE_VOLUME -> soundJingleVolume(exec)
            ClientScriptOpCode.SOUND_VORBIS_VOLUME -> soundVorbisVolume(exec)
            ClientScriptOpCode.SOUND_SPEECH_VOLUME -> soundSpeechVolume(exec)
            ClientScriptOpCode.SOUND_SYNTH_RATE -> soundSynthRate(exec)
            ClientScriptOpCode.SOUND_VORBIS_RATE -> soundVorbisRate(exec)
            else -> throw IllegalStateException("opcode $operation is not handled by DialogOps")
        }
    }

    @JvmStatic
    fun soundSong(executor: CS2Executor) {
        SongPlayer.playSong(executor.intStack[--executor.intStackPtr], 255, 50)
    }

    @JvmStatic
    fun opplayerT(executor: CS2Executor) {
        PacketEncoder.sendInterfaceOnPlayer(executor.stringStack[--executor.stringStackPtr] as String?)
    }

    @JvmStatic
    fun playSoundSynth(executor: CS2Executor) {
        executor.intStackPtr -= 3
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], 255, 256)
    }

    @JvmStatic
    fun resumeCountDialog(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        var value = 0
        if (Utils.isWrongJava(text!!)) {
            value = Utils.parseInt(text!!)
        }
        val tcpmessage4: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_COUNTDIALOG, Client.connectionContext!!.isaac)
        tcpmessage4!!.buffer.writeInt(value)
        Client.connectionContext!!.queuePacket(tcpmessage4)
    }

    @JvmStatic
    fun resumeClanForumQFCDialog(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_CLANFORUMQFCDIALOG, Client.connectionContext!!.isaac)
        tcpmessage3!!.buffer.writeByte(text!!.length + 1)
        tcpmessage3!!.buffer.writeString(text!!)
        Client.connectionContext!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun ifOpenSubClient(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        Interface.openSub(i_2, SubInterface(i_3, 3), null, true)
    }

    @JvmStatic
    fun soundSongVolume(executor: CS2Executor) {
        executor.intStackPtr -= 3
        SongPlayer.playSong(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2])
    }

    @JvmStatic
    fun ifCloseSubClient(executor: CS2Executor) {
        --executor.intStackPtr
        val value = executor.intStack[executor.intStackPtr]
        val class282Sub443: SubInterface? = Client.OPEN_INTERFACES!!.get(value.toLong())
        if (class282Sub443 != null && class282Sub443.overlay == 3) {
            Interface.closeChildInterface(class282Sub443, true, true)
        }
    }

    @JvmStatic
    fun opplayer(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        PacketEncoder.sendPlayerOperation(value, text)
    }

    @JvmStatic
    fun ifClose() {
        PacketEncoder.closeInterface()
    }

    @JvmStatic
    fun soundVorbisVolume(executor: CS2Executor) {
        executor.intStackPtr -= 4
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], false, 256)
    }

    @JvmStatic
    fun soundVorbisRate(executor: CS2Executor) {
        executor.intStackPtr -= 5
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], false, executor.intStack[executor.intStackPtr + 4])
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
    fun appendTypedMessage(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val type: ChatMessageType? = ChatMessageType.forId(executor.intStack[executor.intStackPtr])
        val effectFlags = executor.intStack[executor.intStackPtr + 1]
        val message: String? = executor.stringStack[--executor.stringStackPtr] as String?
        if (type == ChatMessageType.DEV_CONSOLE) {
            Console.writeLine(message!!)
        } else if (type == ChatMessageType.DEV_CONSOLE_CLEAR) {
            Console.setConsoleText(message!!)
        } else {
            ChatBox.writeMessage(type!!, effectFlags, "", "", "", message!!)
        }
    }

    @JvmStatic
    fun resumeStringDialog(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_TEXTDIALOG, Client.connectionContext!!.isaac)
        tcpmessage3!!.buffer.writeByte(text!!.length + 1)
        tcpmessage3!!.buffer.writeString(text!!)
        Client.connectionContext!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun resumeNamedDialog(executor: CS2Executor) {
        val text: String? = executor.stringStack[--executor.stringStackPtr] as String?
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_NAMEDIALOG, Client.connectionContext!!.isaac)
        tcpmessage3!!.buffer.writeByte(text!!.length + 1)
        tcpmessage3!!.buffer.writeString(text!!)
        Client.connectionContext!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun soundSynthRate(executor: CS2Executor) {
        executor.intStackPtr -= 5
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], executor.intStack[executor.intStackPtr + 4])
    }

    @JvmStatic
    fun soundJingle(executor: CS2Executor) {
        executor.intStackPtr -= 2
        SoundEffectPlayer.playSoundJingle(executor.intStack[executor.intStackPtr], 255)
    }

    @JvmStatic
    fun soundJingleVolume(executor: CS2Executor) {
        executor.intStackPtr -= 3
        SoundEffectPlayer.playSoundJingle(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 2])
    }

    @JvmStatic
    fun ccDragPickup(executor: CS2Executor) {
        executor.intStackPtr -= 2
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val target: CS2Interface? = if (executor.hasIntOperand) executor.hookedInterface2  else executor.hookedInterface1
        InterfaceRenderer.startDragging(target!!.comp!!, i_2, i_3)
    }

    @JvmStatic
    fun ifDragPickup(executor: CS2Executor) {
        executor.intStackPtr -= 3
        val i_2 = executor.intStack[executor.intStackPtr]
        val i_3 = executor.intStack[executor.intStackPtr + 1]
        val i_4 = executor.intStack[executor.intStackPtr + 2]
        val component: Component? = Component.fromHash(i_4)
        InterfaceRenderer.startDragging(component!!, i_2, i_3)
    }

    @JvmStatic
    fun resumeObjdialog(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.GRAND_EXCHANGE_ITEM_SELECT, Client.connectionContext!!.isaac)
        tcpmessage3!!.buffer.writeShort(value)
        Client.connectionContext!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun resetMyPlayerAnims(executor: CS2Executor) {
        executor.intStackPtr -= 2
        PlayerUpdate.animateZero(PlayerUpdate.MY_PLAYER!!, executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1])
    }

    @JvmStatic
    fun resumeHSLDialog(executor: CS2Executor) {
        val value = executor.intStack[--executor.intStackPtr]
        val tcpmessage3: TcpPacket? = PacketEncoder.createPacket(ClientProt.RESUME_HSLDIALOG, Client.connectionContext!!.isaac)
        tcpmessage3!!.buffer.writeShort(value)
        Client.connectionContext!!.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun soundSpeechVolume(executor: CS2Executor) {
        executor.intStackPtr -= 4
        AreaSoundPlayer.playSoundVorbis(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], true, 256)
    }

    @JvmStatic
    fun soundSynthVolume(executor: CS2Executor) {
        executor.intStackPtr -= 4
        AreaSoundPlayer.playSoundSynth(executor.intStack[executor.intStackPtr], executor.intStack[executor.intStackPtr + 1], executor.intStack[executor.intStackPtr + 2], executor.intStack[executor.intStackPtr + 3], 256)
    }

}
