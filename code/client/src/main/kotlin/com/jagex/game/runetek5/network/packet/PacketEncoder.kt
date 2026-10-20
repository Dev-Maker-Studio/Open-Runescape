package com.jagex.game.runetek5.network.packet

import com.darkan.bot.componentIdFromHash
import com.darkan.bot.interfaceIdFromHash
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.JFS32
import com.jagex.game.runetek5.client.GameShell
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.Client
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
import com.darkan.crash.CrashHandler
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
import com.jagex.game.runetek5.movement.pathfinding.RouteFinder
import com.jagex.game.runetek5.movement.pathfinding.RouteStrategies
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

object PacketEncoder {

    @JvmStatic
    fun sendLeaveFriendsChatPacket() {
        val connectionContext = Client.connectionContext
        val tcpPacket = createPacket(ClientProt.FC_JOIN, connectionContext.isaac)
        tcpPacket.buffer.writeByte(0)
        connectionContext.queuePacket(tcpPacket)
    }

    @JvmStatic
    fun sendSignUpForm(email: String?, password: String?, age: Int, agreedToTerms: Boolean) {
        if (Client.gameState == GameState.IN_ACCOUNT_CREATION) {
            val packet = createPacket(ClientProt.SEND_SIGN_UP_FORM, Client.LOBBY_CONNECTION_CONTEXT.isaac)
            packet.buffer.writeShort(0)
            val encryptStart = packet.buffer.currentPosition
            packet.buffer.writeString(email ?: "")
            packet.buffer.writeString(password ?: "")
            packet.buffer.writeByte(age)
            packet.buffer.writeByte(if (agreedToTerms) 1 else 0)
            packet.buffer.currentPosition += 7
            packet.buffer.encryptXtea(AccountCreationStage.LOGIN_XTEAS!!, encryptStart, packet.buffer.currentPosition)
            packet.buffer.writeShortLength(packet.buffer.currentPosition - encryptStart)
            Client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet)
            if (age < 13) {
                Client.hasJustCreatedAccount = true
            }

            AccountCreationStage.currentRequest = CreateAccountRequest.AWAITING_RESPONSE
        }
    }

    @JvmStatic
    fun sendPlayerOperation(operationId: Int, playerName: String?) {
        val playerCount = PlayerUpdate.NUM_OUT_PLAYER_INDICES
        val playerIndices = PlayerUpdate.OUT_PLAYER_INDICES
        var found = false

        for (index in 0 until playerCount) {
            val player: PlayerEntity? = PlayerUpdate.players[playerIndices[index]]
            if (player != null && player !== PlayerUpdate.MY_PLAYER!! && player.displayName != null && player.displayName.equals(playerName, ignoreCase = true)) {
                val clientProt: ClientProt? = when (operationId) {
                    1 -> ClientProt.PLAYER_OP1
                    4 -> ClientProt.PLAYER_OP4
                    5 -> ClientProt.PLAYER_OP5
                    6 -> ClientProt.PLAYER_OP6
                    7 -> ClientProt.PLAYER_OP7
                    9 -> ClientProt.PLAYER_OP9
                    else -> null
                }

                if (clientProt != null) {
                    val packet = createPacket(clientProt, Client.GAME_CONNECTION_CONTEXT.isaac)
                    packet.buffer.writeShort(playerIndices[index])
                    packet.buffer.writeByteSubtract(0)
                    Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
                }

                found = true
                break
            }
        }

        if (!found) {
            ChatBox.writeGameMessage(LocalizedText.UNABLE_TO_FIND.translate(Client.CURRENT_LANGUAGE!!) + playerName)
        }
    }

    @JvmStatic
    fun sendInterfaceDragOntoInterface(sourceComponent: Component?, targetComponent: Component?) {
        val packet = createPacket(ClientProt.IF_DRAG_ONTO_IF, Client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShortAddLittle(targetComponent!!.slotId)
        packet.buffer.writeShortLittle(sourceComponent!!.slotId)
        packet.buffer.writeShort(targetComponent.containerItemId)
        packet.buffer.writeShortAddLittle(sourceComponent.containerItemId)
        packet.buffer.writeIntMiddle(sourceComponent.idHash)
        packet.buffer.writeIntLittle(targetComponent.idHash)
        Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    @JvmStatic
    fun sendInterfaceOnInterface(targetComponent: Component?) {
        if (Client.interfaceSelectionActive) {
            if (targetComponent!!.onTargetLeave != null) {
                val selectedComponent = Component.listIcomponent(Client.selectedComponentHash, Client.selectedSlotId)
                if (selectedComponent != null) {
                    val hookRequest = HookRequest()
                    hookRequest.source = targetComponent
                    hookRequest.targetComponent = selectedComponent
                    hookRequest.params = targetComponent.onTargetLeave
                    CS2Executor.executeHookInner(hookRequest)
                }
            }
            val packet = createPacket(ClientProt.IF_ON_IF, Client.GAME_CONNECTION_CONTEXT.isaac)
            Console.writeLine("IF_ON_IF: [" + interfaceIdFromHash(Client.selectedComponentHash) + ", " + componentIdFromHash(Client.selectedComponentHash) + ", " + Client.selectedSlotId + ", " + Client.selectedItemId + "] -> [" + interfaceIdFromHash(targetComponent!!.idHash) + ", " + componentIdFromHash(targetComponent.idHash) + ", " + targetComponent.slotId + ", " + targetComponent.containerItemId + "]")
            packet.buffer.writeShortAddLittle(targetComponent.slotId)
            packet.buffer.writeShortLittle(Client.selectedSlotId)
            packet.buffer.writeShortAddLittle(targetComponent.containerItemId)
            packet.buffer.writeIntLittle(targetComponent.idHash)
            packet.buffer.writeIntInverseMiddle(Client.selectedComponentHash)
            packet.buffer.writeShortLittle(Client.selectedItemId)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
        }
    }

    @JvmStatic
    fun checkSongLoaded() {
        if (SongPlayer.currentSong != null && !SongPlayer.currentSong!!.isLoaded() && SongPlayer.currentSong!!.initialize()) {
            val packet = createPacket(ClientProt.SONG_LOADED, Client.GAME_CONNECTION_CONTEXT.isaac)
            packet.buffer.writeInt(SongPlayer.currentSong!!.musicId)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
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
        val packet = createPacket(ClientProt.SONG_LOADED, Client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeInt(-1)
        Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
        SongPlayer.currentSong = SongReference(Resource.MUSIC!!, songId)
    }

    @JvmStatic
    fun writeInterfaceComponentData(packet: TcpPacket?, interfaceHash: Int, slotId: Int, itemId: Int) {
        Console.writeLine(packet!!.packet.toString() + ": interfaceId: " + interfaceIdFromHash(interfaceHash) + " componentId: " + componentIdFromHash(interfaceHash) + " slotId: " + slotId + " itemId: " + itemId)
        packet.buffer.writeIntLittle(interfaceHash)
        packet.buffer.writeShort(itemId)
        packet.buffer.writeShortAdd(slotId)
    }

    @JvmStatic
    fun decodePlayerCounts(buffer: Packet?) {
        for (worldIndex in 0 until WorldMeta.WORLD_LIST_SIZE) {
            val worldId = buffer!!.readUSmart()
            var playerCount = buffer.readUShort()
            if (playerCount == 65535) {
                playerCount = -1
            }
            WorldMeta.WORLD_LIST_DESCRIPTORS?.get(worldId)?.let { it.playerCount = playerCount }
        }
    }

    @JvmStatic
    fun sendClanOperation(playerIndex: Int) {
        if (Client.CLAN_CHANNEL != null && playerIndex >= 0 && playerIndex < Client.CLAN_CHANNEL!!.numPlayers) {
            val clanPlayer: ClanChannelPlayer? = Client.CLAN_CHANNEL!!.players?.get(playerIndex)
            if (clanPlayer != null && clanPlayer.rank.toInt() == -1) {
                val connectionContext = Client.connectionContext
                val packet = createPacket(ClientProt.UNUSED_CLAN_OP, connectionContext.isaac)
                packet.buffer.writeByte(2 + TextUtils.getLength(clanPlayer.name ?: ""))
                packet.buffer.writeShort(playerIndex)
                packet.buffer.writeString(clanPlayer.name ?: "")
                connectionContext.queuePacket(packet)
            }
        }
    }

    @JvmStatic
    fun sendTransmitVarVerifyid() {
        val packet = createPacket(ClientProt.TRANSMITVAR_VERIFYID, Client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeInt(Client.varVerifyId)
        Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    @JvmStatic
    fun sendCheckEmailPacket(email: String?) {
        if (Client.gameState == GameState.IN_ACCOUNT_CREATION) {
            val message = createPacket(ClientProt.CHECK_EMAIL_VALIDITY, Client.LOBBY_CONNECTION_CONTEXT.isaac)
            message.buffer.writeShort(0)
            val start = message.buffer.currentPosition
            message.buffer.writeString(email ?: "")
            message.buffer.currentPosition += 7
            message.buffer.encryptXtea(AccountCreationStage.LOGIN_XTEAS!!, start, message.buffer.currentPosition)
            message.buffer.writeShortLength(message.buffer.currentPosition - start)
            Client.LOBBY_CONNECTION_CONTEXT.queuePacket(message)
            AccountCreationStage.checkemailRequeststatus = CheckEmailRequest.SENT_REQUEST
        }
    }

    @JvmStatic
    fun sendLobbyHyperlink(url: String?, displayText: String?, fallbackUrl: String?, isNewWindow: Boolean, isExternal: Boolean) {
        val connectionContext = Client.connectionContext
        if (connectionContext.getConnection() != null) {
            val packet = createPacket(ClientProt.LOBBY_HYPERLINK, connectionContext.isaac)
            packet.buffer.writeShort(TextUtils.getLength(url ?: "") + TextUtils.getLength(displayText ?: "") + TextUtils.getLength(fallbackUrl ?: "") + 1)
            packet.buffer.writeString(url ?: "")
            packet.buffer.writeString(displayText ?: "")
            packet.buffer.writeString(fallbackUrl ?: "")
            var flags = 0
            if (isNewWindow) {
                flags = flags or 0x1
            }
            if (isExternal) {
                flags = flags or 0x2
            }
            packet.buffer.writeByte(flags)
            connectionContext.queuePacket(packet)
        }
    }

    @JvmStatic
    fun sendJoinFriendchat(channelName: String?) {
        if (!channelName.isNullOrEmpty()) {
            val connectionContext = Client.connectionContext
            val packet = createPacket(ClientProt.FC_JOIN, connectionContext.isaac)
            packet.buffer.writeByte(TextUtils.getLength(channelName ?: ""))
            packet.buffer.writeString(channelName ?: "")
            connectionContext.queuePacket(packet)
        }
    }

    @JvmStatic
    fun walkToTile(x: Int, y: Int, movementType: Int): TcpPacket? {
        var packet: TcpPacket? = null
        if (movementType == 0) {
            packet = createPacket(ClientProt.WALK, Client.GAME_CONNECTION_CONTEXT.isaac)
        }

        if (movementType == 1) {
            packet = createPacket(ClientProt.MINI_WALK, Client.GAME_CONNECTION_CONTEXT.isaac)
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
        val cc: ClanChannel? = if (mainCC) Client.CLAN_CHANNEL else Client.LISTENED_CLAN_CHANNEL
        if (cc != null && playerIndex >= 0 && playerIndex < cc.numPlayers) {
            val ccPlayer: ClanChannelPlayer? = cc.players?.get(playerIndex)
            if (ccPlayer != null && ccPlayer.rank.toInt() == -1) {
                val ccPlayerName: String? = ccPlayer.name
                val connection = Client.connectionContext
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
        val packet = createPacket(ClientProt.CLOSE_INTERFACE, Client.GAME_CONNECTION_CONTEXT.isaac)
        Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
        var subInterface: SubInterface? = Client.OPEN_INTERFACES!!.first()
        while (subInterface != null) {
            if (!subInterface.linked()) {
                subInterface = Client.OPEN_INTERFACES!!.first()
                if (subInterface == null) {
                    break
                }
            }
            if (subInterface.overlay == 0) {
                Interface.closeChildInterface(subInterface, true, true)
            }
            subInterface = Client.OPEN_INTERFACES!!.next()
        }
        val dialogueContinue = Client.activeDialogueContinue
        if (dialogueContinue != null) {
            Component.redrawComponent(dialogueContinue)
            Client.activeDialogueContinue = null
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
    fun reportBug(bugType: Int, title: String?, description: String?) {
        val packet = createPacket(ClientProt.BUG_REPORT, Client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeShort(1 + Utils.stringLengthPlus2(title ?: "") + Utils.stringLengthPlus2(description ?: ""))
        packet.buffer.writeByte(bugType)
        packet.buffer.writeJagString(description ?: "")
        packet.buffer.writeJagString(title ?: "")
        Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    @JvmStatic
    fun generateSeed(loginPacket: TcpPacket?): IntArray {
        val rsaBuffer = Packet(518)
        val seeds = IntArray(4)
        var seedIndex: Int
        for (seedIndex in 0..3) {
            seeds[seedIndex] = (Math.random() * 9.9999999E7).toInt()
        }
        rsaBuffer.writeByte(10)
        rsaBuffer.writeInt(seeds[0])
        rsaBuffer.writeInt(seeds[1])
        rsaBuffer.writeInt(seeds[2])
        rsaBuffer.writeInt(seeds[3])
        for (seedIndex in 0..9) {
            rsaBuffer.writeInt((Math.random() * 9.9999999E7).toInt())
        }
        rsaBuffer.writeShort((Math.random() * 9.9999999E7).toInt())
        rsaBuffer.applyRSA()
        loginPacket!!.buffer.writeBytes(rsaBuffer.buffer, 0, rsaBuffer.currentPosition)
        return seeds
    }

    @JvmStatic
    fun sendWindowmodePreferenceToServer() {
        val clientServerPacket = createPacket(ClientProt.SCREEN_SIZE, Client.GAME_CONNECTION_CONTEXT.isaac)
        clientServerPacket.buffer.writeByte(WindowMode.getWindowMode())
        clientServerPacket.buffer.writeShort(GameShell.screenCanvasWidth)
        clientServerPacket.buffer.writeShort(GameShell.screenCanvasHeight * -969250379)
        clientServerPacket.buffer.writeByte(PreferenceManager.preferences!!.antiAliasingPreview!!.getValue())
        Client.GAME_CONNECTION_CONTEXT.queuePacket(clientServerPacket)
    }

    @JvmStatic
    fun writeVarcPreferences() {
        var preferenceFile: JFS32? = null
        try {
            preferenceFile = PreferenceManager.loadPreferenceFile("2", Client.CURRENT_GAME!!.name, true)
            val buffer = Packet(Client.VARCS_BOOLS_LOADED * 6 + 3)
            buffer.writeByte(1)
            buffer.writeShort(Client.VARCS_BOOLS_LOADED)
            for (varcIndex in VarcTypeList.VARC_INT!!.indices) {
                if (VarcTypeList.VARC_PERSISTENT!![varcIndex]) {
                    buffer.writeShort(varcIndex)
                    buffer.writeInt(VarcTypeList.VARC_INT!![varcIndex])
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
        Client.exitTime = Utils.time()
        Client.clientIsActive = false
    }

    @JvmStatic
    fun submitQueuedPackets() {
        MouseActionServer.cycle()
        MouseActionServer.MOVE_PACKET.submitAll()
        MouseActionServer.CLICK_PACKET.submitAll()
        var tcpPacket: TcpPacket
        if (Client.maximumHeldKeys > 0) {
            tcpPacket = createPacket(ClientProt.KEY_PRESS, Client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeShort(Client.maximumHeldKeys * 4)

            for (index in 0 until Client.maximumHeldKeys) {
                val record: KeyRecord = Client.KEY_RECORDS[index]!!
                var timeBetweenClicks = record.getTimeSubmitted() - MouseActionServer.lastSubmitted
                if (timeBetweenClicks > 16777215L) {
                    timeBetweenClicks = 16777215L
                }

                MouseActionServer.lastSubmitted = record.getTimeSubmitted()
                tcpPacket.buffer.writeByte(record.getCode())
                tcpPacket.buffer.writeMedium(timeBetweenClicks.toInt())
            }

            Client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
        }

        if (StaticMethods.cameraUpdateCooldown > 0) {
            StaticMethods.cameraUpdateCooldown--
        }

        if (Client.sendCameraAngleToServer && StaticMethods.cameraUpdateCooldown <= 0) {
            StaticMethods.cameraUpdateCooldown = 20
            Client.sendCameraAngleToServer = false
            tcpPacket = createPacket(ClientProt.MOVE_CAMERA, Client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeShortAddLittle(Camera.camAngleX.toInt() shr 3)
            tcpPacket.buffer.writeShortAdd(Camera.camAngleY.toInt() shr 3)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
        }

        if (MouseActionServer.appletHasFocus != GameShell.appletHasFocus) {
            MouseActionServer.appletHasFocus = GameShell.appletHasFocus
            tcpPacket = createPacket(ClientProt.CLIENT_FOCUS, Client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeByte(if (GameShell.appletHasFocus) 1 else 0)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
        }

        if (!Client.preferencesSavedOnServer) {
            tcpPacket = createPacket(ClientProt.SEND_PREFERENCES, Client.GAME_CONNECTION_CONTEXT.isaac)
            tcpPacket.buffer.writeByte(0)
            val position = tcpPacket.buffer.currentPosition
            val buffer = PreferenceManager.preferences!!.encode()
            tcpPacket.buffer.writeBytes(buffer.buffer, 0, buffer.currentPosition)
            tcpPacket.buffer.writeIndex(tcpPacket.buffer.currentPosition - position)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(tcpPacket)
            Client.preferencesSavedOnServer = true
        }
    }

    @JvmStatic
    fun continueInterfacePressed(componentHash: Int, slotId: Int) {
        Console.writeLine("IF_CONTINUE interfaceId: " + interfaceIdFromHash(componentHash) + " componentId: " + componentIdFromHash(componentHash) + " slotId: " + slotId)
        val packet = createPacket(ClientProt.IF_CONTINUE, Client.GAME_CONNECTION_CONTEXT.isaac)
        packet.buffer.writeIntMiddle(componentHash)
        packet.buffer.writeShortAddLittle(slotId)
        Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
    }

    @JvmStatic
    fun sendInterfaceOnPlayer(playerName: String?) {
        if (Client.interfaceSelectionActive && (USE_OPTIONS_FLAGS and 0x18) != 0) {
            var found = false
            val count = PlayerUpdate.NUM_OUT_PLAYER_INDICES
            val indices = PlayerUpdate.OUT_PLAYER_INDICES
            for (i in 0 until count) {
                val player: PlayerEntity? = PlayerUpdate.players[indices[i]]
                if (player != null && player.displayName != null && player.displayName.equals(playerName, ignoreCase = true) && (player === PlayerUpdate.MY_PLAYER!! && (USE_OPTIONS_FLAGS and 0x10) != 0 || (USE_OPTIONS_FLAGS and 0x8) != 0)) {
                    val packet = createPacket(ClientProt.IF_ON_PLAYER, Client.GAME_CONNECTION_CONTEXT.isaac)
                    packet.buffer.writeShort(Client.selectedSlotId)
                    packet.buffer.writeShortLittle(indices[i])
                    packet.buffer.writeByteSubtract(0)
                    packet.buffer.writeIntInverseMiddle(Client.selectedComponentHash)
                    packet.buffer.writeShortLittle(Client.selectedItemId)
                    Client.GAME_CONNECTION_CONTEXT.queuePacket(packet)
                    val size = player.getSize()
                    RouteFinder.calculateRoute(player.movementBaseX[0], player.movementBaseY[0], true, RouteStrategies.forPlayer(player.movementBaseX[0], player.movementBaseY[0], size, size))
                    found = true
                    break
                }
            }
            if (!found) {
                ChatBox.writeGameMessage(LocalizedText.UNABLE_TO_FIND.translate(Client.CURRENT_LANGUAGE!!) + playerName)
            }
            if (Client.interfaceSelectionActive) {
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
            CrashHandler.noteError(addition, throwable)
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }

    @JvmStatic
    fun sendAccountCreationStage(stage: Int) {
        if (Client.gameState == GameState.IN_ACCOUNT_CREATION) {
            val packet = createPacket(ClientProt.ACCOUNT_CREATION_STAGE, Client.LOBBY_CONNECTION_CONTEXT.isaac)
            packet.buffer.writeByte(stage)
            Client.LOBBY_CONNECTION_CONTEXT.queuePacket(packet)
        }
    }

    @JvmStatic
    fun queueCutsceneExit(forceExit: Boolean) {
        if (Client.MAP_LOADING_STAGE != 2 && Client.MAP_LOADING_STAGE != 1) {
            if (!forceExit) {
                val actions = CutsceneTypeList.CUTSCENE_MISC_ACTIONS

                if (actions != null) {
                    for (actionIndex in actions.indices) {
                        val action: CutsceneAction? = actions[actionIndex]
                        action?.stopAction()
                    }
                }
            }

            Client.MAP_LOADING_STAGE = 2
            MapRegion.CUTSCENE_MAP_XTEAS = null
            CutsceneTypeList.CUTSCENE_BUFFER = null
            Client.cutsceneCameraActive = false
            CutsceneTypeList.resetCutsceneConfigs()
            val cutsceneExitPacket = createPacket(ClientProt.CUTSCENE_FINISHED, Client.GAME_CONNECTION_CONTEXT.isaac)
            cutsceneExitPacket.buffer.writeByte(if (forceExit) 1 else 0)
            Client.GAME_CONNECTION_CONTEXT.queuePacket(cutsceneExitPacket)
        }
    }
}
