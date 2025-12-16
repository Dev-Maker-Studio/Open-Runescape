package com.jagex.game.runetek5.network.packet

import com.darkan.bot.Bot
import com.darkan.bot.componentIdFromHash
import com.darkan.bot.interfaceIdFromHash
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.JFS32
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.console.Console
import com.jagex.game.runetek5.client.input.keyboard.KeyDownCommand
import com.jagex.game.runetek5.client.input.keyboard.KeyRecord
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer
import com.jagex.game.runetek5.client.login.AccountCreationStage
import com.jagex.game.runetek5.client.login.CheckEmailRequest
import com.jagex.game.runetek5.client.login.CreateAccountRequest
import com.jagex.game.runetek5.client.menu.USE_OPTIONS_FLAGS
import com.jagex.game.runetek5.client.periodic.PulseEventQueue
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.cutscene.CutsceneAction
import com.jagex.game.runetek5.config.cutscene.CutsceneTypeList
import com.jagex.game.runetek5.config.midi.SongReference
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.HookRequest
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.SubInterface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.encoding.HuffmanList
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceComponentRenderer
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannel
import com.jagex.game.runetek5.interaction.chat.clanchat.channel.ClanChannelPlayer
import com.jagex.game.runetek5.movement.CoordGrid
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies
import com.jagex.game.runetek5.network.BufferedConnectionContext
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.scene.MapRegion
import com.jagex.game.runetek5.sound.player.song.SongPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.tools.DebugDisplay
import com.jagex.game.runetek5.tools.Isaac
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.TextUtils
import com.jagex.game.runetek5.utils.Utils
import java.io.DataInputStream
import java.net.URL

object PacketEncoder {

    @JvmStatic
    fun sendJoinFriendchatPacket() {
        val connectionContext = client.getConnectionContext()
        val tcpPacket = createPacket(ClientProt.FC_JOIN, connectionContext.isaac)
        tcpPacket.buffer.writeByte(0)
        connectionContext.queuePacket(tcpPacket)
    }

    @JvmStatic
    fun method2074(string_0: String?, string_1: String?, i_2: Int, bool3: Boolean) {
        if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
            val tcpmessage5 = createPacket(ClientProt.SEND_SIGN_UP_FORM, client.LOBBY_CONNECTION_CONTEXT.isaac)
            tcpmessage5.buffer.writeShort(0)
            val i_6 = tcpmessage5.buffer.currentPosition
            tcpmessage5.buffer.writeString(string_0 ?: "")
            tcpmessage5.buffer.writeString(string_1 ?: "")
            tcpmessage5.buffer.writeByte(i_2)
            tcpmessage5.buffer.writeByte(if (bool3) 1 else 0)
            tcpmessage5.buffer.currentPosition += 7
            tcpmessage5.buffer.encryptXtea(AccountCreationStage.LOGIN_XTEAS!!, i_6, tcpmessage5.buffer.currentPosition)
            tcpmessage5.buffer.writeShortLength(tcpmessage5.buffer.currentPosition - i_6)
            client.LOBBY_CONNECTION_CONTEXT.queuePacket(tcpmessage5)
            if (i_2 < 13) {
                client.aBool7189 = true
            }

            AccountCreationStage.aClass4942916 = CreateAccountRequest.aClass4945790
        }
    }

    @JvmStatic
    fun method2075(i_0: Int, string_1: String?) {
        val i_3 = PlayerUpdate.NUM_OUT_PLAYER_INDICES
        val ints_4 = PlayerUpdate.OUT_PLAYER_INDICES
        var bool5 = false

        for (i_6 in 0 until i_3) {
            val player7: PlayerEntity? = PlayerUpdate.players[ints_4[i_6]]
            if (player7 != null && player7 !== PlayerUpdate.MY_PLAYER && player7.displayName != null && player7.displayName.equals(string_1, ignoreCase = true)) {
                val outgoingpacket8: ClientProt? = when (i_0) {
                    1 -> ClientProt.PLAYER_OP1
                    4 -> ClientProt.PLAYER_OP4
                    5 -> ClientProt.PLAYER_OP5
                    6 -> ClientProt.PLAYER_OP6
                    7 -> ClientProt.PLAYER_OP7
                    9 -> ClientProt.PLAYER_OP9
                    else -> null
                }

                if (outgoingpacket8 != null) {
                    val tcpmessage9 = createPacket(outgoingpacket8, client.GAME_CONNECTION_CONTEXT.isaac)
                    tcpmessage9.buffer.writeShort(ints_4[i_6])
                    tcpmessage9.buffer.writeByteSubtract(0)
                    client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage9)
                }

                bool5 = true
                break
            }
        }

        if (!bool5) {
            ChatBox.writeGameMessage(LocalizedText.UNABLE_TO_FIND.translate(client.CURRENT_LANGUAGE) + string_1)
        }
    }

    @JvmStatic
    fun method2076(icomponentdefinitions0: Component?, icomponentdefinitions1: Component?) {
        val tcpmessage3 = createPacket(ClientProt.IF_DRAG_ONTO_IF, client.GAME_CONNECTION_CONTEXT.isaac)
        tcpmessage3.buffer.writeShortAddLittle(icomponentdefinitions1!!.slotId)
        tcpmessage3.buffer.writeShortLittle(icomponentdefinitions0!!.slotId)
        tcpmessage3.buffer.writeShort(icomponentdefinitions1.containerItemId)
        tcpmessage3.buffer.writeShortAddLittle(icomponentdefinitions0.containerItemId)
        tcpmessage3.buffer.writeIntMiddle(icomponentdefinitions0.idHash)
        tcpmessage3.buffer.writeIntLittle(icomponentdefinitions1.idHash)
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage3)
    }

    @JvmStatic
    fun method2590(icomponentdefinitions0: Component?) {
        if (client.interfaceSelectionActive) {
            if (icomponentdefinitions0!!.anObjectArray1393 != null) {
                val icomponentdefinitions2 = Component.listIcomponent(client.anInt56, client.anInt7345)
                if (icomponentdefinitions2 != null) {
                    val hookrequest3 = HookRequest()
                    hookrequest3.source = icomponentdefinitions0
                    hookrequest3.aClass1188057 = icomponentdefinitions2
                    hookrequest3.params = icomponentdefinitions0.anObjectArray1393
                    CS2Executor.executeHookInner(hookrequest3)
                }
            }
            val tcpmessage4 = createPacket(ClientProt.IF_ON_IF, client.GAME_CONNECTION_CONTEXT.isaac)
            Console.writeLine("IF_ON_IF: [" + interfaceIdFromHash(client.anInt56) + ", " + componentIdFromHash(client.anInt56) + ", " + client.anInt7345 + ", " + client.anInt7346 + "] -> [" + interfaceIdFromHash(icomponentdefinitions0!!.idHash) + ", " + componentIdFromHash(icomponentdefinitions0.idHash) + ", " + icomponentdefinitions0.slotId + ", " + icomponentdefinitions0.containerItemId + "]")
            tcpmessage4.buffer.writeShortAddLittle(icomponentdefinitions0.slotId)
            tcpmessage4.buffer.writeShortLittle(client.anInt7345)
            tcpmessage4.buffer.writeShortAddLittle(icomponentdefinitions0.containerItemId)
            tcpmessage4.buffer.writeIntLittle(icomponentdefinitions0.idHash)
            tcpmessage4.buffer.writeIntInverseMiddle(client.anInt56)
            tcpmessage4.buffer.writeShortLittle(client.anInt7346)
            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage4)
        }
    }

    @JvmStatic
    fun method3675() {
        if (SongPlayer.currentSong != null && !SongPlayer.currentSong!!.isLoaded() && SongPlayer.currentSong!!.initialize()) {
            val tcpmessage1 = createPacket(ClientProt.SONG_LOADED, client.GAME_CONNECTION_CONTEXT.isaac)
            tcpmessage1.buffer.writeInt(SongPlayer.currentSong!!.musicId)
            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage1)
        }
    }

    @JvmStatic
    fun createPacket(clientprot: ClientProt?, isaac: Isaac?): TcpPacket {
        val packet = TcpPacket.createPacket()
        packet.packet = clientprot
        packet.position = clientprot!!.size
        packet.buffer = when {
            packet.position == -1 -> Packet.Bit(260)
            packet.position == -2 -> Packet.Bit(10000)
            packet.position <= 18 -> Packet.Bit(20)
            packet.position <= 98 -> Packet.Bit(100)
            else -> Packet.Bit(260)
        }
        packet.buffer.setIsaacCipher(isaac)
        packet.buffer.writeIsaacByte(packet.packet!!.id)
        packet.capacity = 0
        return packet
    }

    @JvmStatic
    fun preloadSong(songId: Int) {
        val tcpmessage2 = createPacket(ClientProt.SONG_LOADED, client.GAME_CONNECTION_CONTEXT.isaac)
        tcpmessage2.buffer.writeInt(-1)
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage2)
        SongPlayer.currentSong = SongReference(Resource.MUSIC!!, songId)
    }

    @JvmStatic
    fun method12590(packet: TcpPacket?, interfaceHash: Int, slotId: Int, itemId: Int) {
        Console.writeLine(packet!!.packet.toString() + ": interfaceId: " + interfaceIdFromHash(interfaceHash) + " componentId: " + componentIdFromHash(interfaceHash) + " slotId: " + slotId + " itemId: " + itemId)
        packet.buffer.writeIntLittle(interfaceHash)
        packet.buffer.writeShort(itemId)
        packet.buffer.writeShortAdd(slotId)
    }

    @JvmStatic
    fun decodePlayerCounts(rsbytebuffer0: Packet?) {
        for (i_2 in 0 until WorldMeta.WORLD_LIST_SIZE) {
            val worldId = rsbytebuffer0!!.readUSmart()
            var playerCount = rsbytebuffer0.readUShort()
            if (playerCount == 65535) {
                playerCount = -1
            }
            WorldMeta.WORLD_LIST_DESCRIPTORS?.get(worldId)?.let { it.playerCount = playerCount }
        }
    }

    @JvmStatic
    fun method6811(i_0: Int) {
        if (client.CLAN_CHANNEL != null && i_0 >= 0 && i_0 < client.CLAN_CHANNEL!!.numPlayers) {
            val class572: ClanChannelPlayer? = client.CLAN_CHANNEL!!.players?.get(i_0)
            if (class572 != null && class572.rank.toInt() == -1) {
                val class1843 = client.getConnectionContext()
                val tcpmessage4 = createPacket(ClientProt.UNUSED_CLAN_OP, class1843.isaac)
                tcpmessage4.buffer.writeByte(2 + TextUtils.getLength(class572.name ?: ""))
                tcpmessage4.buffer.writeShort(i_0)
                tcpmessage4.buffer.writeString(class572.name ?: "")
                class1843.queuePacket(tcpmessage4)
            }
        }
    }

    @JvmStatic
    fun sendTransmitVarVerifyid() {
        val tcpmessage1 = createPacket(ClientProt.TRANSMITVAR_VERIFYID, client.GAME_CONNECTION_CONTEXT.isaac)
        tcpmessage1.buffer.writeInt(client.varVerifyId)
        client.GAME_CONNECTION_CONTEXT.queuePacket(tcpmessage1)
    }

    @JvmStatic
    fun sendCheckEmailPacket(string_0: String?) {
        if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
            val message = createPacket(ClientProt.CHECK_EMAIL_VALIDITY, client.LOBBY_CONNECTION_CONTEXT.isaac)
            message.buffer.writeShort(0)
            val start = message.buffer.currentPosition
            message.buffer.writeString(string_0 ?: "")
            message.buffer.currentPosition += 7
            message.buffer.encryptXtea(AccountCreationStage.LOGIN_XTEAS!!, start, message.buffer.currentPosition)
            message.buffer.writeShortLength(message.buffer.currentPosition - start)
            client.LOBBY_CONNECTION_CONTEXT.queuePacket(message)
            AccountCreationStage.checkemailRequeststatus = CheckEmailRequest.SENT_REQUEST
        }
    }

    @JvmStatic
    fun method11404(string_0: String?, string_1: String?, string_2: String?, bool3: Boolean, bool4: Boolean) {
        val class1846 = client.getConnectionContext()
        if (class1846.getConnection() != null) {
            val tcpmessage7 = createPacket(ClientProt.LOBBY_HYPERLINK, class1846.isaac)
            tcpmessage7.buffer.writeShort(TextUtils.getLength(string_0 ?: "") + TextUtils.getLength(string_1 ?: "") + TextUtils.getLength(string_2 ?: "") + 1)
            tcpmessage7.buffer.writeString(string_0 ?: "")
            tcpmessage7.buffer.writeString(string_1 ?: "")
            tcpmessage7.buffer.writeString(string_2 ?: "")
            var i_8 = 0
            if (bool3) {
                i_8 = i_8 or 0x1
            }
            if (bool4) {
                i_8 = i_8 or 0x2
            }
            tcpmessage7.buffer.writeByte(i_8)
            class1846.queuePacket(tcpmessage7)
        }
    }

    @JvmStatic
    fun method3345(string_0: String?) {
        if (!string_0.isNullOrEmpty()) {
            val class1842 = client.getConnectionContext()
            val tcpmessage3 = createPacket(ClientProt.FC_JOIN, class1842.isaac)
            tcpmessage3.buffer.writeByte(TextUtils.getLength(string_0 ?: ""))
            tcpmessage3.buffer.writeString(string_0 ?: "")
            class1842.queuePacket(tcpmessage3)
        }
    }

    @JvmStatic
    fun walkToTile(x: Int, y: Int, movementType: Int): TcpPacket? {
        var packet: TcpPacket? = null
        if (movementType == 0) {
            packet = createPacket(ClientProt.WALK, client.GAME_CONNECTION_CONTEXT.isaac)
        }

        if (movementType == 1) {
            packet = createPacket(ClientProt.MINI_WALK, client.GAME_CONNECTION_CONTEXT.isaac)
        }

        val base = Resource.mapRegion!!.getBaseCoordinates()
        packet!!.buffer.writeByte(if (KeyDownCommand.runKeyDown()) 1 else 0)
        packet.buffer.writeShort(x + base.x)
        packet.buffer.writeShortLittle(y + base.y)
        ComponentMinimap.minimapFlagX = x
        ComponentMinimap.minimapFlagY = y
        ComponentMinimap.minimapContainsFlag = false
        PulseEventQueue.delayedClearMinimapFlagUpdate()
        return packet
    }

    @JvmStatic
    fun kickPlayer(mainCC: Boolean, playerIndex: Int) {
        val cc: ClanChannel? = if (mainCC) client.CLAN_CHANNEL else client.LISTENED_CLAN_CHANNEL
        if (cc != null && playerIndex >= 0 && playerIndex < cc.numPlayers) {
            val ccPlayer: ClanChannelPlayer? = cc.players?.get(playerIndex)
            if (ccPlayer != null && ccPlayer.rank.toInt() == -1) {
                val ccPlayerName: String? = ccPlayer.name
                val connection = client.getConnectionContext()
                val packet = createPacket(ClientProt.CLANCHANNEL_KICKUSER, connection.isaac)
                packet.buffer.writeByte(3 + TextUtils.getLength(ccPlayerName ?: ""))
                packet.buffer.writeByte(if (mainCC) 1 else 0)
                packet.buffer.writeShort(playerIndex)
                packet.buffer.writeString(ccPlayerName ?: "")
                connection.queuePacket(packet)
            }
        }
    }

    @JvmStatic
    fun closeInterface() {
        val packet = createPacket(ClientProt.CLOSE_INTERFACE, client.GAME_CONNECTION_CONTEXT.isaac)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
        var subInterface: SubInterface? = client.OPEN_INTERFACES.first()
        while (subInterface != null) {
            if (!subInterface.linked()) {
                subInterface = client.OPEN_INTERFACES.first()
                if (subInterface == null) {
                    break
                }
            }
            if (subInterface.overlay == 0) {
                Interface.closeChildInterface(subInterface, true, true)
            }
            subInterface = client.OPEN_INTERFACES.next()
        }
        if (client.aClass1187352 != null) {
            Component.redrawComponent(client.aClass1187352)
            client.aClass1187352 = null
        }
    }

    @JvmStatic
    fun chatMessageToBuffer(buffer: Packet?, chatMessage: String?): Int {
        val length = buffer!!.currentPosition
        val data = Utils.characterSequenceToBytearray(chatMessage ?: "")
        buffer.writeSmart(data.size)
        buffer.currentPosition += HuffmanList.HUFFMAN!!.decompress(data, 0, data.size, buffer.buffer, buffer.currentPosition)
        return buffer.currentPosition - length
    }

    @JvmStatic
    fun reportBug(i_0: Int, string_1: String?, string_2: String?) {
        val packet = createPacket(ClientProt.BUG_REPORT, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShort(1 + Utils.stringLengthPlus2(string_1 ?: "") + Utils.stringLengthPlus2(string_2 ?: ""))
        packet.buffer.writeByte(i_0)
        packet.buffer.writeJagString(string_2 ?: "")
        packet.buffer.writeJagString(string_1 ?: "")
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    @JvmStatic
    fun generateSeed(tcpmessage0: TcpPacket?): IntArray {
        val rsbytebuffer2 = Packet(518)
        val ints_3 = IntArray(4)
        var i_4: Int
        for (i_4 in 0..3) {
            ints_3[i_4] = (Math.random() * 9.9999999E7).toInt()
        }
        rsbytebuffer2.writeByte(10)
        rsbytebuffer2.writeInt(ints_3[0])
        rsbytebuffer2.writeInt(ints_3[1])
        rsbytebuffer2.writeInt(ints_3[2])
        rsbytebuffer2.writeInt(ints_3[3])
        for (i_4 in 0..9) {
            rsbytebuffer2.writeInt((Math.random() * 9.9999999E7).toInt())
        }
        rsbytebuffer2.writeShort((Math.random() * 9.9999999E7).toInt())
        rsbytebuffer2.applyRSA()
        tcpmessage0!!.buffer.writeBytes(rsbytebuffer2.buffer, 0, rsbytebuffer2.currentPosition)
        return ints_3
    }

    @JvmStatic
    fun sendWindowmodePreferenceToServer() {
        val clientServerPacket = createPacket(ClientProt.SCREEN_SIZE, client.GAME_CONNECTION_CONTEXT.isaac)
        clientServerPacket.buffer.writeByte(WindowMode.getWindowMode())
        clientServerPacket.buffer.writeShort(GameShell.screenCanvasWidth)
        clientServerPacket.buffer.writeShort(client.screenCanvasHeight * -969250379)
        clientServerPacket.buffer.writeByte(PreferenceManager.preferences!!.antiAliasingPreview!!.getValue())
        client.GAME_CONNECTION_CONTEXT.queuePacket(clientServerPacket)
    }

    @JvmStatic
    fun writeVarcPreferences() {
        var preferenceFile: JFS32? = null
        try {
            preferenceFile = PreferenceManager.loadPreferenceFile("2", client.CURRENT_GAME.name, true)
            val buffer = Packet(client.VARCS_BOOLS_LOADED * 6 + 3)
            buffer.writeByte(1)
            buffer.writeShort(client.VARCS_BOOLS_LOADED)
            for (i_3 in VarcTypeList.VARC_INT!!.indices) {
                if (VarcTypeList.VARC_BOOL!![i_3]) {
                    buffer.writeShort(i_3)
                    buffer.writeInt(VarcTypeList.VARC_INT!![i_3])
                }
            }
            preferenceFile.write(buffer.buffer, 0, buffer.currentPosition)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        try {
            preferenceFile?.close()
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
        client.exitTime = Utils.time()
        client.clientIsActive = false
    }

    @JvmStatic
    fun submitQueuedPackets() {
        MouseActionServer.cycle()
        MouseActionServer.MOVE_PACKET.submitAll()
        MouseActionServer.CLICK_PACKET.submitAll()
        var tcpPacket: TcpPacket
        if (client.maximumHeldKeys > 0) {
            tcpPacket = createPacket(ClientProt.KEY_PRESS, client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeShort(client.maximumHeldKeys * 4)

            for (index in 0 until client.maximumHeldKeys) {
                val record: KeyRecord = client.KEY_RECORDS[index]
                var timeBetweenClicks = record.getTimeSubmitted() - MouseActionServer.lastSubmitted
                if (timeBetweenClicks > 16777215L) {
                    timeBetweenClicks = 16777215L
                }

                MouseActionServer.lastSubmitted = record.getTimeSubmitted()
                tcpPacket.buffer.writeByte(record.getCode())
                tcpPacket.buffer.writeMedium(timeBetweenClicks.toInt())
            }

            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
        }

        if (StaticMethods.anInt2912 > 0) {
            StaticMethods.anInt2912--
        }

        if (client.sendCameraAngleToServer && StaticMethods.anInt2912 <= 0) {
            StaticMethods.anInt2912 = 20
            client.sendCameraAngleToServer = false
            tcpPacket = createPacket(ClientProt.MOVE_CAMERA, client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeShortAddLittle(Camera.camAngleX.toInt() shr 3)
            tcpPacket.buffer.writeShortAdd(Camera.camAngleY.toInt() shr 3)
            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
        }

        if (MouseActionServer.appletHasFocus != GameShell.appletHasFocus) {
            MouseActionServer.appletHasFocus = GameShell.appletHasFocus
            tcpPacket = createPacket(ClientProt.CLIENT_FOCUS, client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeByte(if (GameShell.appletHasFocus) 1 else 0)
            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
        }

        if (!client.preferencesSavedOnServer) {
            tcpPacket = createPacket(ClientProt.SEND_PREFERENCES, client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeByte(0)
            val position = tcpPacket.buffer.currentPosition
            val buffer = PreferenceManager.preferences!!.encode()
            tcpPacket.buffer.writeBytes(buffer.buffer, 0, buffer.currentPosition)
            tcpPacket.buffer.writeIndex(tcpPacket.buffer.currentPosition - position)
            client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
            client.preferencesSavedOnServer = true
        }
    }

    @JvmStatic
    fun continueInterfacePressed(i_0: Int, i_1: Int) {
        Console.writeLine("IF_CONTINUE interfaceId: " + interfaceIdFromHash(i_0) + " componentId: " + componentIdFromHash(i_0) + " slotId: " + i_1)
        val packet = createPacket(ClientProt.IF_CONTINUE, client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeIntMiddle(i_0)
        packet.buffer.writeShortAddLittle(i_1)
        client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    @JvmStatic
    fun method4167(string_0: String?) {
        if (client.interfaceSelectionActive && (USE_OPTIONS_FLAGS and 0x18) != 0) {
            var bool2 = false
            val count = PlayerUpdate.NUM_OUT_PLAYER_INDICES
            val indices = PlayerUpdate.OUT_PLAYER_INDICES
            for (i in 0 until count) {
                val player6: PlayerEntity? = PlayerUpdate.players[indices[i]]
                if (player6 != null && player6.displayName != null && player6.displayName.equals(string_0, ignoreCase = true) && (player6 === PlayerUpdate.MY_PLAYER && (USE_OPTIONS_FLAGS and 0x10) != 0 || (USE_OPTIONS_FLAGS and 0x8) != 0)) {
                    val packet = createPacket(ClientProt.IF_ON_PLAYER, client.GAME_CONNECTION_CONTEXT.isaac)
                    packet.buffer.writeShort(client.anInt7345)
                    packet.buffer.writeShortLittle(indices[i])
                    packet.buffer.writeByteSubtract(0)
                    packet.buffer.writeIntInverseMiddle(client.anInt56)
                    packet.buffer.writeShortLittle(client.anInt7346)
                    client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
                    val size = player6.getSize()
                    RouteFinder.calculateRoute(player6.movementBaseX[0], player6.movementBaseY[0], true, RouteStrategies.forPlayer(player6.movementBaseX[0], player6.movementBaseY[0], size, size))
                    bool2 = true
                    break
                }
            }
            if (!bool2) {
                ChatBox.writeGameMessage(LocalizedText.UNABLE_TO_FIND.translate(client.CURRENT_LANGUAGE) + string_0)
            }
            if (client.interfaceSelectionActive) {
                InterfaceComponentRenderer.cancelInterfaceSelection()
            }
        }
    }

    @JvmStatic
    fun sendErrorToServer(addition: String?, throwable: Throwable?) {
        try {
            var errorString = ""
            if (throwable != null) {
                errorString = DebugDisplay.getStringFromThrowable(throwable)
            }
            if (addition != null) {
                if (throwable != null) {
                    errorString += " | "
                }
                errorString += addition
            }
            CS2Executor.logError(errorString)
            errorString = Utils.formatRequestParameter(errorString)
            if (StaticMethods.initialApplet != null) {
                var vendor = "Unknown"
                var version = "1.1"
                try {
                    vendor = System.getProperty("java.vendor")
                    version = System.getProperty("java.version")
                } catch (exception: Exception) {
                    // NOOP
                }
                val url = URL(StaticMethods.initialApplet!!.getCodeBase(), "clienterror.ws?c=" + SystemInfo.clientBuild + "&cs=" + SystemInfo.clientBuildSub + "&u=" + (if (PlayerUpdate.MY_PLAYER_USERNAME != null) Utils.formatRequestParameter(PlayerUpdate.MY_PLAYER_USERNAME) else "" + StaticMethods.aLong10459) + "&v1=" + Utils.formatRequestParameter(vendor) + "&v2=" + Utils.formatRequestParameter(version) + "&e=" + errorString)
                val dataInputStream = DataInputStream(url.openStream())
                dataInputStream.read()
                dataInputStream.close()
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    @JvmStatic
    fun method16088(i_0: Int) {
        if (client.gameState == GameState.IN_ACCOUNT_CREATION) {
            val packet = createPacket(ClientProt.ACCOUNT_CREATION_STAGE, client.LOBBY_CONNECTION_CONTEXT.isaac)
            packet.buffer.writeByte(i_0)
            client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet)
        }
    }

    @JvmStatic
    fun queueCutsceneExit(bool0: Boolean) {
        if (client.MAP_LOADING_STAGE != 2 && client.MAP_LOADING_STAGE != 1) {
            if (!bool0) {
                val arr_2 = CutsceneTypeList.CUTSCENE_MISC_ACTIONS

                if (arr_2 != null) {
                    for (i_3 in arr_2.indices) {
                        val class964: CutsceneAction? = arr_2[i_3]
                        class964?.method1593()
                    }
                }
            }

            client.MAP_LOADING_STAGE = 2
            MapRegion.CUTSCENE_MAP_XTEAS = null
            CutsceneTypeList.CUTSCENE_BUFFER = null
            client.aBool7321 = false
            CutsceneTypeList.resetCutsceneConfigs()
            val cutsceneExitPacket = createPacket(ClientProt.CUTSCENE_FINISHED, client.GAME_CONNECTION_CONTEXT.isaac)
            cutsceneExitPacket.buffer.writeByte(if (bool0) 1 else 0)
            client.GAME_CONNECTION_CONTEXT.queuePacket(cutsceneExitPacket)
        }
    }
}
