package com.jagex.game.runetek5.client.login

import com.darkan.bot.Bot
import com.jagex.game.launch.Loader
import com.jagex.game.runetek5.botdetection.ReflectionCheckManager
import com.jagex.game.runetek5.buffer.Packet
import com.jagex.game.runetek5.cache.FileOnDisk
import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.cache.node.NodeCollection
import com.jagex.game.runetek5.cache.node.QueueIterator
import com.jagex.game.runetek5.client.*
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.input.mouse.MouseActionServer
import com.jagex.game.runetek5.client.menu.MiniMenu
import com.jagex.game.runetek5.client.periodic.PulseEventHandler
import com.jagex.game.runetek5.client.window.WindowMode
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.config.`object`.ObjectType
import com.jagex.game.runetek5.config.item.ItemType
import com.jagex.game.runetek5.config.npc.NPCType
import com.jagex.game.runetek5.config.rsinterface.Component
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.itemcontainer.ItemContainer
import com.jagex.game.runetek5.config.varc.VarcTypeList
import com.jagex.game.runetek5.entity.pathingentity.PathingEntity
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.graphics.ifcomponent.InterfaceRenderer
import com.jagex.game.runetek5.graphics.map.minimap.ComponentMinimap
import com.jagex.game.runetek5.interaction.FriendList
import com.jagex.game.runetek5.interaction.chat.ChatBox
import com.jagex.game.runetek5.interaction.chat.friendschat.FriendsChat
import com.jagex.game.runetek5.network.*
import com.jagex.game.runetek5.network.packet.GrandExchangeSlot
import com.jagex.game.runetek5.network.packet.PacketDecoder
import com.jagex.game.runetek5.network.packet.PacketEncoder
import com.jagex.game.runetek5.network.packet.ServerProt
import com.jagex.game.runetek5.network.packet.TcpPacket
import com.jagex.game.runetek5.network.world.WorldMeta
import com.jagex.game.runetek5.scene.LocAction
import com.jagex.game.runetek5.scene.RegionLoadRequest
import com.jagex.game.runetek5.scene.RegionLoadingType
import com.jagex.game.runetek5.sound.player.area.AreaSoundPlayer
import com.jagex.game.runetek5.staticmethods.StaticMethods
import com.jagex.game.runetek5.systeminfo.SystemInfo
import com.jagex.game.runetek5.tools.Browser
import com.jagex.game.runetek5.tools.Isaac
import com.jagex.game.runetek5.tools.LocalizedText
import com.jagex.game.runetek5.utils.Utils
import com.jagex.game.runetek5.vardomain.VarProviders
import java.io.IOException

object LoginManager {

    enum class LoginStage {
        IDLE,
        OPENING_CONNECTION,
        AWAITING_INIT_RESPONSE,
        READING_SOCIAL_RESPONSE_SIZE,
        READING_SOCIAL_URL_DATA,
        AWAITING_SOCIAL_AUTH_CONFIRM,
        READING_SOCIAL_TOKENS,
        SENDING_LOGIN_REQUEST,
        AWAITING_LOGIN_RESPONSE,
        AWAITING_CLIENT_CONFIRM,
        SENDING_LOGIN_CONTINUE,
        READING_QUEUE_POSITION,
        READING_PAYLOAD_SIZE,
        READING_LOGIN_PAYLOAD,
        READING_FIRST_GAME_PACKET,
        READING_PACKET_HEADER,
        READING_PACKET_OPCODE,
        READING_RESPONSE_DETAIL,
        READING_RECONNECT_RESPONSE,
        AWAITING_TIME_SYNC
    }

    enum class LobbyStage {
        LOGGED_OUT,
        LOGGING_IN_LOBBY,
        LOBBY_TO_WORLD
    }

    private var lobbyAuthToken: String? = null

    @JvmStatic
    fun setLoginStage(loginStage: LoginStage) {
        println("Login stage: ${LoginManager.loginStage} -> $loginStage")
        LoginManager.loginStage = loginStage
    }

    @JvmStatic
    fun setLobbyStage(lobbyStage: LobbyStage) {
        println("Lobby stage: ${LoginManager.lobbyStage} -> $lobbyStage")
        LoginManager.lobbyStage = lobbyStage
    }

    @JvmField
    var loginStage = LoginStage.IDLE

    @JvmField
    var lobbyStage = LobbyStage.LOGGED_OUT

    @JvmField
    var loginResponseCode = 0

    @JvmField
    var LAST_IP_ADDRESS = 0

    @JvmField
    var loginPayloadSize = 0

    @JvmField
    var clientSeed: IntArray? = null

    @JvmStatic
    fun loginBlock() {
        if (loginStage != LoginStage.IDLE && loginStage != LoginStage.AWAITING_CLIENT_CONFIRM) {
            try {
                val s_1: Short = if (connectionRetryCount == 0) {
                    250
                } else {
                    2000
                }
                var timeout = s_1
                if (socialNetworkLogin && loginStage.ordinal >= LoginStage.AWAITING_SOCIAL_AUTH_CONFIRM.ordinal) {
                    timeout = 6000
                }
                if (lobbyStage != LobbyStage.LOBBY_TO_WORLD || loginStage != LoginStage.AWAITING_TIME_SYNC && StaticMethods.worldLoginResponse != 42) {
                    connectionTickCount++
                }
                if (connectionTickCount > timeout) {
                    CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                    if (connectionRetryCount >= 3) {
                        loginStage = LoginStage.IDLE
                        setLoginResponse(-5)
                        return
                    }
                    if (lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
                        GAME_CONNECTION_INFO!!.swap()
                    } else {
                        WorldConnection.LOBBY_CONNECTION_INFO!!.swap()
                    }
                    connectionTickCount = 0
                    connectionRetryCount++
                    loginStage = LoginStage.OPENING_CONNECTION
                }
                var tcpPacket: TcpPacket
                var tcpPacketPositionBefore: Int
                var loginBlockPositionBefore: Int
                if (loginStage == LoginStage.OPENING_CONNECTION) {
                    if (lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
                        CURRENT_CONNECTION_CONTEXT!!.init(AsyncConnection.createAsyncConnection(GAME_CONNECTION_INFO!!.createSocket(), 15000), GAME_CONNECTION_INFO!!.host!!)
                    } else {
                        CURRENT_CONNECTION_CONTEXT!!.init(AsyncConnection.createAsyncConnection(WorldConnection.LOBBY_CONNECTION_INFO!!.createSocket(), 15000), WorldConnection.LOBBY_CONNECTION_INFO!!.host!!)
                    }
                    CURRENT_CONNECTION_CONTEXT!!.clearAllQueuedPackets()
                    tcpPacket = TcpPacket.createLoginPacket()
                    if (socialNetworkLogin) {
                        tcpPacket.buffer.writeByte(LoginProt.INIT_SOCIAL_NETWORK_CONNECTION.id)
                        tcpPacket.buffer.writeShort(0)
                        tcpPacketPositionBefore = tcpPacket.buffer.currentPosition
                        tcpPacket.buffer.writeInt(Loader.MAJOR_BUILD)
                        tcpPacket.buffer.writeInt(Loader.MINOR_BUILD)
                        if (lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
                            tcpPacket.buffer.writeByte(if (Client.gameState == GameState.UNK_10) 1 else 0)
                        }
                        val seedBlock = encodeClientSeed()
                        seedBlock.writeByte(StaticMethods.selectedWorldId)
                        seedBlock.writeShort((Math.random() * 9.9999999E7).toInt())
                        seedBlock.writeByte(Client.CURRENT_LANGUAGE!!.getValue())
                        seedBlock.writeInt(Client.AFFILIATE)
                        for (i in 0 until 6) {
                            seedBlock.writeInt((Math.random() * 9.9999999E7).toInt())
                        }
                        seedBlock.writeLong(Client.clientRandomSeed)
                        seedBlock.writeByte(Client.CURRENT_GAME!!.id)
                        seedBlock.writeByte((Math.random() * 9.9999999E7).toInt())
                        seedBlock.applyRSA()
                        tcpPacket.buffer.writeBytes(seedBlock.buffer, 0, seedBlock.currentPosition)
                        tcpPacket.buffer.writeShortLength(tcpPacket.buffer.currentPosition - tcpPacketPositionBefore)
                    } else {
                        tcpPacket.buffer.writeByte(LoginProt.INIT_GAME_CONNECTION.id)
                    }
                    CURRENT_CONNECTION_CONTEXT!!.queuePacket(tcpPacket)
                    CURRENT_CONNECTION_CONTEXT!!.flush()
                    loginStage = LoginStage.AWAITING_INIT_RESPONSE
                }
                if (loginStage == LoginStage.AWAITING_INIT_RESPONSE) {
                    if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(1)) {
                        return
                    }
                    CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 1)
                    val i_10 = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff
                    if (i_10 != 0) {
                        loginStage = LoginStage.IDLE
                        setLoginResponse(i_10)
                        CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                        GameState.method6270()
                        return
                    }
                    loginStage = if (socialNetworkLogin) {
                        LoginStage.READING_SOCIAL_RESPONSE_SIZE
                    } else {
                        LoginStage.SENDING_LOGIN_REQUEST
                    }
                }
                if (loginStage == LoginStage.READING_SOCIAL_RESPONSE_SIZE) {
                    if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(2)) {
                        return
                    }
                    CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 2)
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.readUShort()
                    loginStage = LoginStage.READING_SOCIAL_URL_DATA
                }
                if (loginStage == LoginStage.READING_SOCIAL_URL_DATA) {
                    if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition)) {
                        return
                    }
                    CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition)
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.decryptFullXtea(clientSeed!!)
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                    val url = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.readJagString()
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                    val callName = JSObjectManager.LOGIN_PROMPT_BECOME_MEMBER.getCallName()
                    if (!Client.isApplet || !Browser.openBrowser(url, 1, callName)) {
                        Browser.openBrowser(url, true, callName, Client.useBrowserAppletMode)
                    }
                    loginStage = LoginStage.AWAITING_SOCIAL_AUTH_CONFIRM
                }
                if (loginStage == LoginStage.AWAITING_SOCIAL_AUTH_CONFIRM) {
                    if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(1)) {
                        return
                    }
                    CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 1)
                    if ((CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff) == 1) {
                        loginStage = LoginStage.READING_SOCIAL_TOKENS
                    }
                }
                if (loginStage == LoginStage.READING_SOCIAL_TOKENS) {
                    if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(16)) {
                        return
                    }
                    CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 16)
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 16
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.decryptFullXtea(clientSeed!!)
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                    socialNetworkToken = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.readLong()
                    socialNetworkNonce = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.readLong()
                    loginStage = LoginStage.SENDING_LOGIN_REQUEST
                }
                var loginBlock: Packet.Bit
                if (loginStage == LoginStage.SENDING_LOGIN_REQUEST) {
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                    CURRENT_CONNECTION_CONTEXT!!.clearAllQueuedPackets()
                    tcpPacket = TcpPacket.createLoginPacket()
                    loginBlock = tcpPacket.buffer
                    var startPositionLoginBlock: Int
                    val currentLoginProt: LoginProt
                    if (lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
                        currentLoginProt = if (socialNetworkLogin) {
                            LoginProt.SOCIAL_NETWORK_LOGIN
                        } else {
                            LoginProt.GAMELOGIN
                        }
                        loginBlock.writeByte(currentLoginProt.id)
                        loginBlock.writeShort(0)
                        loginBlockPositionBefore = loginBlock.currentPosition
                        startPositionLoginBlock = loginBlock.currentPosition
                        if (!socialNetworkLogin) {
                            loginBlock.writeInt(Loader.MAJOR_BUILD)
                            loginBlock.writeInt(Loader.MINOR_BUILD)
                            loginBlock.writeByte(if (Client.gameState == GameState.UNK_10) 1 else 0)
                            startPositionLoginBlock = loginBlock.currentPosition
                            val seedBlock = getSeedBlock(false)
                            loginBlock.writeBytes(seedBlock.buffer, 0, seedBlock.currentPosition)
                            startPositionLoginBlock = loginBlock.currentPosition
                            loginBlock.writeByte(if (socialNetworkToken == -1L) 1 else 0)
                            if (socialNetworkToken == -1L) {
                                loginBlock.writeString(StaticMethods.loginUsername)
                            } else {
                                loginBlock.writeLong(socialNetworkToken)
                            }
                        }
                        loginBlock.writeByte(WindowMode.getWindowMode())
                        loginBlock.writeShort(GameShell.screenCanvasWidth)
                        loginBlock.writeShort(GameShell.screenCanvasHeight * -969250379)
                        loginBlock.writeByte(PreferenceManager.preferences!!.antiAliasingPreview!!.value)
                        pushRandomDat(loginBlock)
                        loginBlock.writeString(Client.affiliateP!!)
                        loginBlock.writeInt(Client.AFFILIATE)
                        val preferenceBlock = PreferenceManager.preferences!!.encode()
                        loginBlock.writeByte(preferenceBlock.currentPosition)
                        loginBlock.writeBytes(preferenceBlock.buffer, 0, preferenceBlock.currentPosition)
                        Client.preferencesSavedOnServer = true
                        val systemInfoBlock = Packet(SystemInfo.SYSTEM_INFO!!.encode())
                        SystemInfo.SYSTEM_INFO!!.writeMachineInformation(systemInfoBlock)
                        loginBlock.writeBytes(systemInfoBlock.buffer, 0, systemInfoBlock.buffer.size)
                        loginBlock.writeInt(Client.varVerifyId)
                        loginBlock.writeLong(Client.machineUid)
                        loginBlock.writeString(Client.loginToken!!)
                        val str7156 = Client.supplementaryLoginData
                        loginBlock.writeByte(if (str7156 == null) 0 else 1)
                        if (str7156 != null) {
                            loginBlock.writeString(str7156)
                        }
                        loginBlock.writeByte(if (Resource.NATIVE_LIBRARY_LOADER!!.isJagtheoraLoaded()) 1 else 0)
                        loginBlock.writeByte(if (Client.isApplet) 1 else 0)
                        loginBlock.writeByte(if (Client.hasSignedTermsAgreement) 1 else 0)
                        loginBlock.writeByte(Client.loginType)
                        loginBlock.writeInt(Client.sessionToken)
                        loginBlock.writeString(Client.js5AuthToken!!)
                        loginBlock.writeByte(if (WorldConnection.CONNECTION_INFO != null && GAME_CONNECTION_INFO!!.worldId == WorldConnection.CONNECTION_INFO!!.worldId) 0 else 1)
                        val worldId = GAME_CONNECTION_INFO!!.worldId
                        loginBlock.writeInt(worldId)
                        writeResourceCrc(loginBlock)
                    } else {
                        currentLoginProt = if (socialNetworkLogin) {
                            LoginProt.SOCIAL_NETWORK_LOGIN
                        } else {
                            LoginProt.LOBBYLOGIN
                        }
                        loginBlock.writeByte(currentLoginProt.id)
                        loginBlock.writeShort(0)
                        loginBlockPositionBefore = loginBlock.currentPosition
                        startPositionLoginBlock = loginBlock.currentPosition
                        if (!socialNetworkLogin) {
                            loginBlock.writeInt(Loader.MAJOR_BUILD)
                            loginBlock.writeInt(Loader.MINOR_BUILD)
                            val seedBlock = getSeedBlock(true)
                            loginBlock.writeBytes(seedBlock.buffer, 0, seedBlock.currentPosition)
                            startPositionLoginBlock = loginBlock.currentPosition
                            loginBlock.writeByte(if (socialNetworkToken == -1L) 1 else 0)
                            if (socialNetworkToken == -1L) {
                                loginBlock.writeString(StaticMethods.loginUsername)
                            } else {
                                loginBlock.writeLong(socialNetworkToken)
                            }
                        }
                        loginBlock.writeByte(Client.CURRENT_GAME!!.id)
                        loginBlock.writeByte(Client.CURRENT_LANGUAGE!!.getValue())
                        pushRandomDat(loginBlock)
                        loginBlock.writeString(Client.affiliateP!!)
                        val preferenceBlock = PreferenceManager.preferences!!.encode()
                        loginBlock.writeByte(preferenceBlock.currentPosition)
                        loginBlock.writeBytes(preferenceBlock.buffer, 0, preferenceBlock.currentPosition)
                        loginBlock.writeString(Client.loginToken!!)
                        loginBlock.writeInt(Client.AFFILIATE)
                        loginBlock.writeInt(Client.sessionToken)
                        loginBlock.writeString(Client.js5AuthToken!!)
                        writeResourceCrc(loginBlock)
                    }
                    loginBlock.encryptXtea(clientSeed!!, startPositionLoginBlock, loginBlock.currentPosition)
                    loginBlock.writeShortLength(loginBlock.currentPosition - loginBlockPositionBefore)
                    CURRENT_CONNECTION_CONTEXT!!.queuePacket(tcpPacket)
                    CURRENT_CONNECTION_CONTEXT!!.flush()
                    CURRENT_CONNECTION_CONTEXT!!.isaac = Isaac(clientSeed!!)
                    for (index in 0 until 4) {
                        clientSeed!![index] += 50
                    }
                    CURRENT_CONNECTION_CONTEXT!!.instreamEncryption = Isaac(clientSeed!!)
                    Isaac(clientSeed!!)
                    CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.setIsaacCipher(CURRENT_CONNECTION_CONTEXT!!.instreamEncryption)
                    clientSeed = null
                    loginStage = LoginStage.AWAITING_LOGIN_RESPONSE
                }
                if (loginStage == LoginStage.AWAITING_LOGIN_RESPONSE) {
                    if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(1)) {
                        return
                    }
                    CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 1)
                    val i_10 = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff
                    println("Login response: $i_10")
                    when {
                        i_10 == 21 -> {
                            loginStage = LoginStage.READING_QUEUE_POSITION
                        }
                        i_10 == 29 || i_10 == 45 -> {
                            loginResponseCode = i_10
                            loginStage = LoginStage.READING_RESPONSE_DETAIL
                        }
                        i_10 == 1 -> {
                            loginStage = LoginStage.AWAITING_CLIENT_CONFIRM
                            setLoginResponse(1)
                            return
                        }
                        i_10 == 2 -> {
                            loginStage = LoginStage.READING_PAYLOAD_SIZE
                        }
                        i_10 == 15 -> {
                            CURRENT_CONNECTION_CONTEXT!!.currentPacketSize = -2
                            loginStage = LoginStage.READING_RECONNECT_RESPONSE
                        }
                        i_10 == 23 && connectionRetryCount < 3 -> {
                            connectionTickCount = 0
                            connectionRetryCount++
                            loginStage = LoginStage.OPENING_CONNECTION
                            CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                            return
                        }
                        i_10 == 42 -> {
                            loginStage = LoginStage.AWAITING_TIME_SYNC
                            setLoginResponse(42)
                            return
                        }
                        StaticMethods.hasStoredCredentials && !socialNetworkLogin && StaticMethods.selectedWorldId != -1 && i_10 == 35 -> {
                            socialNetworkLogin = true
                            connectionTickCount = 0
                            loginStage = LoginStage.OPENING_CONNECTION
                            CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                        }
                        else -> {
                            loginStage = LoginStage.IDLE
                            setLoginResponse(i_10)
                            CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                            GameState.method6270()
                        }
                    }
                    return
                }
                if (loginStage == LoginStage.SENDING_LOGIN_CONTINUE) {
                    CURRENT_CONNECTION_CONTEXT!!.clearAllQueuedPackets()
                    tcpPacket = TcpPacket.createLoginPacket()
                    loginBlock = tcpPacket.buffer
                    loginBlock.setIsaacCipher(CURRENT_CONNECTION_CONTEXT!!.isaac)
                    loginBlock.writeIsaacByte(LoginProt.GAMELOGIN_CONTINUE.id)
                    CURRENT_CONNECTION_CONTEXT!!.queuePacket(tcpPacket)
                    CURRENT_CONNECTION_CONTEXT!!.flush()
                    loginStage = LoginStage.AWAITING_LOGIN_RESPONSE
                } else if (loginStage == LoginStage.READING_QUEUE_POSITION) {
                    if (CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(1)) {
                        CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 1)
                        val i_10 = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff
                        loginQueuePosition = i_10 * 50
                        loginStage = LoginStage.IDLE
                        setLoginResponse(21)
                        CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                        GameState.method6270()
                    }
                } else if (loginStage == LoginStage.AWAITING_TIME_SYNC) {
                    if (CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(2)) {
                        CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 2)
                        StaticMethods.loginHandshakeCode = ((CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff) shl 8) + (CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[1].toInt() and 0xff)
                        loginStage = LoginStage.AWAITING_LOGIN_RESPONSE
                    }
                } else if (loginStage == LoginStage.READING_RESPONSE_DETAIL) {
                    if (loginResponseCode == 29) {
                        if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(1)) {
                            return
                        }
                        CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 1)
                        loginResponseDetail = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff
                    } else {
                        if (loginResponseCode != 45) {
                            throw IllegalStateException()
                        }
                        if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(3)) {
                            return
                        }
                        CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 3)
                        loginResponseDetail = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff
                        loginResponseExtraInfo = (CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[2].toInt() and 0xff) + ((CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[1].toInt() and 0xff) shl 8)
                    }
                    loginStage = LoginStage.IDLE
                    setLoginResponse(loginResponseCode)
                    CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                    GameState.method6270()
                } else if (loginStage == LoginStage.READING_PAYLOAD_SIZE) {
                    if (CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(1)) {
                        CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 1)
                        loginPayloadSize = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer[0].toInt() and 0xff
                        loginStage = LoginStage.READING_LOGIN_PAYLOAD
                    }
                } else {
                    var buffer: Packet.Bit
                    if (loginStage == LoginStage.READING_LOGIN_PAYLOAD) {
                        buffer = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer
                        if (lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
                            if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(loginPayloadSize)) {
                                return
                            }
                            CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(buffer.buffer, 0, loginPayloadSize)
                            buffer.currentPosition = 0
                            Client.PLAYER_RIGHTS = buffer.readUByte()
                            Client.PLAYER_MOD_LEVEL = buffer.readUByte()
                            Client.USERDETAIL_QUICKCHAT = buffer.readUByte() == 1
                            Client.VERIFIED_EMAIL_ADDRESS = buffer.readUByte() == 1
                            Client.hasAccountSecurity = buffer.readUByte() == 1
                            Client.IS_QUICKCHAT_ONLY = buffer.readUByte() == 1
                            PlayerUpdate.myPlayerIndex = buffer.readUShort()
                            Client.IS_MEMBER = buffer.readUByte() == 1
                            PLAYER_DOB = buffer.readMedium()
                            Client.memberWorld = buffer.readUByte() == 1
                            Client.mapOwnerIdentifier = buffer.readString()
                            Resource.mapRegion!!.loctypeList!!.setMember(Client.memberWorld)
                            Resource.mapRegionLoaderTask!!.region.loctypeList!!.setMember(Client.memberWorld)
                            TypeLists.OBJ!!.setMembersOnly(Client.memberWorld)
                            TypeLists.NPC!!.setMember(Client.memberWorld)
                        } else {
                            if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(loginPayloadSize)) {
                                return
                            }
                            CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(buffer.buffer, 0, loginPayloadSize)
                            buffer.currentPosition = 0
                            Client.PLAYER_RIGHTS = buffer.readUByte()
                            Client.PLAYER_MOD_LEVEL = buffer.readUByte()
                            Client.USERDETAIL_QUICKCHAT = buffer.readUByte() == 1
                            PLAYER_DOB = buffer.readMedium()
                            PlayerUpdate.MY_PLAYER!!.male = buffer.readUByte().toByte()
                            Client.VERIFIED_EMAIL_ADDRESS = buffer.readUByte() == 1
                            Client.hasAccountSecurity = buffer.readUByte() == 1
                            MEMBERSHIP_END = buffer.readLong()
                            membershipTimeCorrection = MEMBERSHIP_END - Utils.time() - buffer.read40BitLong()
                            tcpPacketPositionBefore = buffer.readUByte()
                            Client.IS_MEMBER = (tcpPacketPositionBefore and 0x1) != 0
                            IS_MEMBER_SUBSCRIPTION = (tcpPacketPositionBefore and 0x2) != 0
                            Client.JCOINS = buffer.readInt()
                            QueueIterator.LOYALTY_ENABLED = buffer.readUByte() == 1
                            LOYALTY_BALANCE = buffer.readInt()
                            RECOVERYQUESTIONSSETDATE = buffer.readUShort()
                            MESSAGES = buffer.readUShort()
                            LASTLOGINLDAY = buffer.readUShort()
                            LAST_IP_ADDRESS = buffer.readInt()
                            lobbyAuthToken = buffer.readString()
                            PacketDecoder.HOSTNAME_IDENTIFIER = HostNameIdentifier(LAST_IP_ADDRESS)
                            Thread(PacketDecoder.HOSTNAME_IDENTIFIER).start()
                            EMAIL_STATUS = buffer.readUByte()
                            lobbyCreditCardExpiry = buffer.readUShort()
                            FileOnDisk.anInt5357 = buffer.readUShort()
                            isDobRequested = buffer.readUByte() == 1
                            PlayerUpdate.MY_PLAYER!!.username = buffer.readJagString().also { PlayerUpdate.MY_PLAYER!!.displayName = it; PlayerUpdate.MY_PLAYER_USERNAME = it }
                            Client.lobbyMembershipStatus = buffer.readUByte()
                            lobbyPlayAge = buffer.readInt()
                            Client.hasDisplayNamePending = buffer.readUByte() == 1
                            WorldConnection.CONNECTION_INFO = ConnectionInfo()
                            WorldConnection.CONNECTION_INFO!!.worldId = buffer.readUShort()
                            if (WorldConnection.CONNECTION_INFO!!.worldId == 65535) {
                                WorldConnection.CONNECTION_INFO!!.worldId = -1
                            }
                            WorldConnection.CONNECTION_INFO!!.host = buffer.readJagString()
                            if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
                                WorldConnection.CONNECTION_INFO!!.devWorld1 = WorldConnection.CONNECTION_INFO!!.worldId
                                WorldConnection.CONNECTION_INFO!!.devWorld2 = WorldConnection.CONNECTION_INFO!!.worldId
                            }
                            if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LOCAL && (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.WTQA || Client.PLAYER_RIGHTS < 2) && GAME_CONNECTION_INFO == WorldConnection.GAME_CONNECTION) {
                                prepareHost()
                            }
                        }
                        if (lobbyStage != LobbyStage.LOBBY_TO_WORLD) {
                            loginStage = LoginStage.IDLE
                            setLoginResponse(2)
                            prepareIngame()
                            Client.setGameState(GameState.UNK_0)
                            CURRENT_CONNECTION_CONTEXT!!.currentPacket = null
                            return
                        }
                        loginStage = LoginStage.READING_PACKET_HEADER
                    }
                    if (loginStage == LoginStage.READING_PACKET_HEADER) {
                        if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(3)) {
                            return
                        }
                        CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 3)
                        loginStage = LoginStage.READING_PACKET_OPCODE
                    }
                    if (loginStage == LoginStage.READING_PACKET_OPCODE) {
                        buffer = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer
                        buffer.currentPosition = 0
                        if (buffer.nextIsSmart()) {
                            if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(1)) {
                                return
                            }
                            CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(buffer.buffer, 3, 1)
                        }
                        tcpPacketPositionBefore = buffer.readEncryptedSmart()
                        CURRENT_CONNECTION_CONTEXT!!.currentPacket = ServerProt.forId(tcpPacketPositionBefore)
                        CURRENT_CONNECTION_CONTEXT!!.currentPacketSize = buffer.readUShort()
                        loginStage = LoginStage.READING_FIRST_GAME_PACKET
                    }
                    if (loginStage == LoginStage.READING_FIRST_GAME_PACKET) {
                        if (CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(CURRENT_CONNECTION_CONTEXT!!.currentPacketSize)) {
                            CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, CURRENT_CONNECTION_CONTEXT!!.currentPacketSize)
                            CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                            val i_10 = CURRENT_CONNECTION_CONTEXT!!.currentPacketSize
                            loginStage = LoginStage.IDLE
                            setLoginResponse(2)
                            onEnterIngame()
                            PlayerUpdate.decodeOwnPlayer(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer)
                            tcpPacketPositionBefore = i_10 - CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition
                            val rsbitsbuffer21 = Packet.Bit(tcpPacketPositionBefore)
                            System.arraycopy(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition, rsbitsbuffer21.buffer, 0, tcpPacketPositionBefore)
                            CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition += tcpPacketPositionBefore
                            if (CURRENT_CONNECTION_CONTEXT!!.currentPacket == ServerProt.DYNAMIC_MAP_REGION) {
                                Resource.mapRegion!!.initialize(RegionLoadRequest(RegionLoadingType.INSTANCE, rsbitsbuffer21))
                            } else {
                                Resource.mapRegion!!.initialize(RegionLoadRequest(RegionLoadingType.STANDARD, rsbitsbuffer21))
                            }
                            if (CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition != i_10) {
                                throw RuntimeException("${CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition} $i_10")
                            }
                            CURRENT_CONNECTION_CONTEXT!!.currentPacket = null
                        }
                    } else if (loginStage == LoginStage.READING_RECONNECT_RESPONSE) {
                        println("Logging in check")
                        if (CURRENT_CONNECTION_CONTEXT!!.currentPacketSize == -2) {
                            if (!CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(2)) {
                                return
                            }
                            CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, 2)
                            CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                            CURRENT_CONNECTION_CONTEXT!!.currentPacketSize = CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.readUShort()
                        }
                        if (CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.available(CURRENT_CONNECTION_CONTEXT!!.currentPacketSize)) {
                            CURRENT_CONNECTION_CONTEXT!!.getConnection()!!.read(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.buffer, 0, CURRENT_CONNECTION_CONTEXT!!.currentPacketSize)
                            CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
                            val i_10 = CURRENT_CONNECTION_CONTEXT!!.currentPacketSize
                            loginStage = LoginStage.IDLE
                            setLoginResponse(15)
                            onReconnect()
                            PlayerUpdate.decodeOwnPlayer(CURRENT_CONNECTION_CONTEXT!!.receivedBuffer)
                            if (CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition != i_10) {
                                throw RuntimeException("${CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition} $i_10")
                            }
                            CURRENT_CONNECTION_CONTEXT!!.currentPacket = null
                        }
                    }
                }
            } catch (ioexception: IOException) {
                CURRENT_CONNECTION_CONTEXT!!.closeConnection()
                if (connectionRetryCount < 3) {
                    if (lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
                        GAME_CONNECTION_INFO!!.swap()
                    } else {
                        WorldConnection.LOBBY_CONNECTION_INFO!!.swap()
                    }
                    connectionTickCount = 0
                    ++connectionRetryCount
                    loginStage = LoginStage.OPENING_CONNECTION
                } else {
                    loginStage = LoginStage.IDLE
                    setLoginResponse(-4)
                    GameState.method6270()
                }
            }
        }
    }

    @JvmStatic
    fun isLoginInProgress(): Boolean {
        return loginStage != LoginStage.IDLE
    }

    @Suppress("unused")
    @JvmStatic
    fun lobbyLogin(string_0: String, string_1: String) {
        lobbyStage = LobbyStage.LOGGING_IN_LOBBY
        CURRENT_CONNECTION_CONTEXT = Client.LOBBY_CONNECTION_CONTEXT
        initializeLogin(false, false, string_0, string_1, -1L)
    }

    @JvmStatic
    fun lobbyLoginWithStoredCredentials() {
        lobbyStage = LobbyStage.LOGGING_IN_LOBBY
        CURRENT_CONNECTION_CONTEXT = Client.LOBBY_CONNECTION_CONTEXT
        initializeLogin(socialNetworkToken == -1L, true, "", "", socialNetworkToken)
    }

    @JvmStatic
    fun lobbyLoginWithBase64Url() {
        lobbyStage = LobbyStage.LOGGING_IN_LOBBY
        CURRENT_CONNECTION_CONTEXT = Client.LOBBY_CONNECTION_CONTEXT
        Client.base64Url?.let { url ->
            val buffer = Packet(url)
            socialNetworkToken = buffer.readLong()
            socialNetworkNonce = buffer.readLong()
        }
        if (socialNetworkToken < 0L) {
            setLoginResponse(35)
        } else {
            initializeLogin(false, true, "", "", socialNetworkToken)
        }
    }

    @JvmStatic
    fun startLobbyLogin() {
        when {
            Client.base64Url != null -> lobbyLoginWithBase64Url()
            StaticMethods.selectedWorldId != -1 -> lobbyLoginWithStoredCredentials()
            else -> lobbyLogin(StaticMethods.loginUsername, StaticMethods.loginPassword)
        }
    }

    @JvmStatic
    fun openLobby(ingame: Boolean) {
        if (ingame) {
            if (Client.BASE_WINDOW_ID != -1) {
                Interface.unloadInterface(Client.BASE_WINDOW_ID)
            }
            var subInterface = Client.OPEN_INTERFACES!!.first()
            while (subInterface != null) {
                if (!subInterface.linked()) {
                    subInterface = Client.OPEN_INTERFACES!!.first()
                    if (subInterface == null) {
                        break
                    }
                }
                Interface.closeChildInterface(subInterface, true, false)
                subInterface = Client.OPEN_INTERFACES!!.next()
            }
            Client.BASE_WINDOW_ID = -1
            Client.OPEN_INTERFACES = IterableNodeMap(8)
            Interface.initializeTables()
            Client.BASE_WINDOW_ID = Client.ENTITY_DEFAULTS!!.lobbyWindow
            WindowMode.interfaceResize(false)
            GameShell.setComponentsOnTop()
            Component.runScriptsIfLoaded(Client.BASE_WINDOW_ID, null)
        }
        currentlyInLobby = true
    }

    @JvmStatic
    fun getSeedBlock(password: Boolean): Packet {
        val clientSeed = encodeClientSeed()
        clientSeed.writeLong(0L)
        clientSeed.writeString(if (password) StaticMethods.loginPassword!! else lobbyAuthToken!!)
        clientSeed.writeLong(socialNetworkNonce)
        clientSeed.writeLong(Client.clientRandomSeed)
        clientSeed.applyRSA()
        return clientSeed
    }

    @JvmStatic
    fun returnToLoginScreen() {
        if (Client.gameState == GameState.UNK_0 && !isLoginInProgress() && !AccountCreationStage.isCreatingAccount()) {
            Client.setGameState(GameState.UNK_7)
        }
    }

    @JvmStatic
    fun worldLogin(string_0: String, string_1: String) {
        lobbyStage = LobbyStage.LOBBY_TO_WORLD
        CURRENT_CONNECTION_CONTEXT = Client.GAME_CONNECTION_CONTEXT
        initializeLogin(false, false, string_0, string_1, -1L)
    }

    @JvmStatic
    fun startWorldLogin() {
        when {
            Client.base64Url != null -> worldLoginWithBase64Url()
            StaticMethods.selectedWorldId != -1 -> worldLoginWithStoredCredentials()
            else -> worldLogin(StaticMethods.loginUsername, StaticMethods.loginPassword)
        }
    }

    @JvmStatic
    fun cancelLogin() {
        if (loginStage != LoginStage.IDLE) {
            CURRENT_CONNECTION_CONTEXT!!.closeConnection()
            resetLoginState()
            GameState.method6270()
        }
    }

    @JvmStatic
    fun setLoginResponse(i_0: Int) {
        if (lobbyStage == LobbyStage.LOGGING_IN_LOBBY) {
            StaticMethods.lobbyLoginResponse = i_0
        } else if (lobbyStage == LobbyStage.LOBBY_TO_WORLD) {
            StaticMethods.worldLoginResponse = i_0
        }
    }

    @JvmStatic
    fun confirmLoginContinue() {
        if (loginStage == LoginStage.AWAITING_CLIENT_CONFIRM) {
            loginStage = LoginStage.SENDING_LOGIN_CONTINUE
        }
    }

    @JvmStatic
    fun switchWorldAndLogin(string_0: String, string_1: String) {
        if (string_0.length <= 320 && GameState.method6693()) {
            Client.LOBBY_CONNECTION_CONTEXT.closeConnection()
            resetCredentials()
            StaticMethods.loginUsername = string_0
            StaticMethods.loginPassword = string_1
            Client.setGameState(GameState.UNK_14)
        }
    }

    @JvmStatic
    fun resetLoginState() {
        loginStage = LoginStage.IDLE
        StaticMethods.worldLoginResponse = -2
        StaticMethods.lobbyLoginResponse = -2
    }

    @JvmStatic
    fun directLogin(string_0: String, string_1: String) {
        if (string_0.length <= 320 && GameState.method6693()) {
            resetCredentials()
            StaticMethods.loginUsername = string_0
            StaticMethods.loginPassword = string_1
            Client.setGameState(GameState.UNK_19)
        }
    }

    @JvmStatic
    fun snLogin(i_0: Int) {
        if (GameState.method6693()) {
            if (i_0 != StaticMethods.selectedWorldId) {
                socialNetworkToken = -1L
            }
            StaticMethods.selectedWorldId = i_0
            Client.setGameState(GameState.UNK_19)
        }
    }

    @JvmField
    var socialNetworkLogin = false

    @JvmStatic
    fun pushRandomDat(buffer: Packet) {
        val randomDatData = ByteArray(24)
        try {
            GameShell.playerRandomDat!!.setPosition(0L)
            GameShell.playerRandomDat!!.load(randomDatData)
            var emptyStats = 0
            while (emptyStats < 24 && randomDatData[emptyStats] == 0.toByte()) {
                emptyStats++
            }
            if (emptyStats >= 24) {
                throw IOException()
            }
        } catch (exception: Exception) {
            for (index in 0 until 24) {
                randomDatData[index] = -1
            }
        }
        buffer.writeBytes(randomDatData, 0, 24)
    }

    @JvmStatic
    fun writeResourceCrc(buffer: Packet.Bit) {
        buffer.writeInt(Resource.FRAME_SET!!.getCrc())
        buffer.writeInt(Resource.FRAME_BASE!!.getCrc())
        buffer.writeInt(Resource.CONFIG!!.getCrc())
        buffer.writeInt(Resource.INTERFACE!!.getCrc())
        buffer.writeInt(Resource.SOUND_EFFECT!!.getCrc())
        buffer.writeInt(Resource.MAPS!!.getCrc())
        buffer.writeInt(Resource.MUSIC!!.getCrc())
        buffer.writeInt(Resource.MODEL!!.getCrc())
        buffer.writeInt(Resource.SPRITES!!.getCrc())
        buffer.writeInt(Resource.TEXTURE!!.getCrc())
        buffer.writeInt(Resource.HUFFMAN!!.getCrc())
        buffer.writeInt(Resource.JINGLE!!.getCrc())
        buffer.writeInt(Resource.CS2!!.getCrc())
        buffer.writeInt(Resource.FONT_METRICS!!.getCrc())
        buffer.writeInt(Resource.MIDI_INSTRUMENT!!.getCrc())
        buffer.writeInt(Resource.SOUND_MIDI!!.getCrc())
        buffer.writeInt(Resource.LOC!!.getCrc())
        buffer.writeInt(Resource.ENUM!!.getCrc())
        buffer.writeInt(Resource.NPC!!.getCrc())
        buffer.writeInt(Resource.OBJ!!.getCrc())
        buffer.writeInt(Resource.SEQ!!.getCrc())
        buffer.writeInt(Resource.GFX!!.getCrc())
        buffer.writeInt(Resource.VARBIT!!.getCrc())
        buffer.writeInt(Resource.WORLD_MAP!!.getCrc())
        buffer.writeInt(Resource.QC_MESSAGES!!.getCrc())
        buffer.writeInt(Resource.QC_MENU!!.getCrc())
        buffer.writeInt(Resource.MATERIAL!!.getCrc())
        buffer.writeInt(Resource.PARTICLE!!.getCrc())
        buffer.writeInt(Resource.DEFAULTS!!.getCrc())
        buffer.writeInt(Resource.BILLBOARD!!.getCrc())
        buffer.writeInt(Resource.NATIVE_LIBRARY!!.getCrc())
        buffer.writeInt(Resource.SHADER!!.getCrc())
        buffer.writeInt(Resource.getFontCrc())
        buffer.writeInt(Resource.getTipsCrc())
        buffer.writeInt(Resource.CUTSCENE!!.getCrc())
        buffer.writeInt(Resource.VORBIS!!.getCrc())
    }

    @JvmStatic
    fun encodeClientSeed(): Packet {
        val buffer = Packet(518)
        clientSeed = IntArray(4)
        clientSeed!![0] = (Math.random() * 9.9999999E7).toInt()
        clientSeed!![1] = (Math.random() * 9.9999999E7).toInt()
        clientSeed!![2] = (Math.random() * 9.9999999E7).toInt()
        clientSeed!![3] = (Math.random() * 9.9999999E7).toInt()
        buffer.writeByte(10)
        buffer.writeInt(clientSeed!![0])
        buffer.writeInt(clientSeed!![1])
        buffer.writeInt(clientSeed!![2])
        buffer.writeInt(clientSeed!![3])
        return buffer
    }

    @JvmStatic
    fun resetCredentials() {
        socialNetworkToken = -1L
        socialNetworkNonce = 0L
        StaticMethods.selectedWorldId = -1
    }

    @JvmField
    var lobbyPlayAge = 0

    @JvmStatic
    fun prepareIngame() {
        prepareForIngame()
    }

    @JvmField
    var GAME_CONNECTION_INFO: ConnectionInfo? = null

    @JvmField
    var CURRENT_CONNECTION_CONTEXT: BufferedConnectionContext? = null

    @JvmStatic
    fun onReconnect() {
        CURRENT_CONNECTION_CONTEXT!!.clearAllQueuedPackets()
        CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
        CURRENT_CONNECTION_CONTEXT!!.currentPacket = null
        CURRENT_CONNECTION_CONTEXT!!.lastPacket = null
        CURRENT_CONNECTION_CONTEXT!!.secondLastPacket = null
        CURRENT_CONNECTION_CONTEXT!!.thirdLastPacket = null
        CURRENT_CONNECTION_CONTEXT!!.currentPacketSize = 0
        CURRENT_CONNECTION_CONTEXT!!.idleReadPulses = 0
        Client.REBOOT_TIMER = 0
        MiniMenu.reset()
        ComponentMinimap.resetMinimapFlag()

        for (index in 0 until 2048) {
            PlayerUpdate.players[index] = null
        }

        PlayerUpdate.MY_PLAYER = null

        for (index in 0 until Client.npcCount) {
            val pathingEntity = Client.NPC_ARRAY[index]!!.value as PathingEntity?
            if (pathingEntity != null) {
                pathingEntity.faceEntity = -1
            }
        }

        ItemContainer.resetContainerMap()
        Camera.cameraCalculationType = 2
        StaticMethods.cameraTargetAbsY = -1
        PingRequester.cameraTargetAbsX = -1
        Client.setGameState(GameState.LOGGED_INGAME)

        for (index in 0 until 107) {
            Client.IS_COMPONENT_ON_TOP[index] = true
        }

        PacketEncoder.sendWindowmodePreferenceToServer()
        PingRequester.CURRENT_REQUEST = null
        PingRequester.NEXT_PING_UPDATE_TO_SERVER = 0L
        Client.resetCutscenes()
    }

    @JvmField
    var MESSAGES = 0

    @JvmField
    var RECOVERYQUESTIONSSETDATE = 0

    @JvmStatic
    fun setGameHost(world: Int, host: String): Boolean {
        if (Bot.ENABLED) {
            var worldInList = false
            for (i in WorldConnection.WORLD_DESCRIPTORS_BYID.indices) {
                if (WorldConnection.WORLD_DESCRIPTORS_BYID[i] != null &&
                    WorldConnection.WORLD_DESCRIPTORS_BYID[i]!!.worldNumber == world) {
                    worldInList = true
                    break
                }
            }
            if (!worldInList)
                return false
        }

        GAME_CONNECTION_INFO = ConnectionInfo()
        GAME_CONNECTION_INFO!!.worldId = world
        GAME_CONNECTION_INFO!!.host = host
        if (ServerEnvironment.SERVER_ENVIRONMENT != ServerEnvironment.LIVE) {
            GAME_CONNECTION_INFO!!.devWorld1 = GAME_CONNECTION_INFO!!.worldId
            GAME_CONNECTION_INFO!!.devWorld2 = GAME_CONNECTION_INFO!!.worldId
        }
        return true
    }

    @JvmField
    var PLAYER_DOB = 0

    @JvmStatic
    fun worldLoginWithStoredCredentials() {
        lobbyStage = LobbyStage.LOBBY_TO_WORLD
        CURRENT_CONNECTION_CONTEXT = Client.GAME_CONNECTION_CONTEXT
        initializeLogin(socialNetworkToken == -1L, true, "", "", socialNetworkToken)
    }

    @JvmStatic
    fun onEnterIngame() {
        if (Client.gameState == GameState.UNK_17) {
            Client.resetGameState()
        }

        prepareForIngame()
        Client.LOBBY_CONNECTION_CONTEXT.closeConnection()
        GameShell.appletHasFocus = true
        ReflectionCheckManager.setupPendingChecks()

        for (index in PlayerUpdate.HINT_ARROWS.indices) {
            PlayerUpdate.HINT_ARROWS[index] = null
        }

        Client.interfaceSelectionActive = false
        AreaSoundPlayer.initialize()
        Camera.cameraRandomisationH = (Math.random() * 100.0).toInt() - 50
        Camera.cameraRandomisationV = (Math.random() * 110.0).toInt() - 55
        Camera.cameraRandomisationA = (Math.random() * 80.0).toInt() - 40
        Client.minimapRotation = (Math.random() * 120.0).toInt() - 60
        Client.minimapZoom = (Math.random() * 30.0).toInt() - 20
        Camera.camAngleY = ((Math.random() * 160.0).toInt() - 80 and 0x3fff).toFloat()
        ComponentMinimap.resetMinimapFlagAndPlane()

        for (index in 0 until 2048) {
            PlayerUpdate.players[index] = null
        }

        PlayerUpdate.MY_PLAYER = null
        Client.NPC_UPDATE_INDEX = 0
        Client.npcCount = 0
        Client.NPC_MAP.clear()
        Client.PROJECTILES.remove_all()
        Client.ACTIVE_GFX.clear()
        Client.TILE_MESSAGES.unlink()
        Client.GROUND_ITEMS.clear()
        LocAction.pendingRemovals = NodeCollection()
        LocAction.pendingCustomizations = NodeCollection()
        VarProviders.PLAYER_VAR_PROVIDER.reset()
        PulseEventHandler.clearAllPulseEvents()
        Camera.cameraMovementLocalX = 0
        Camera.cameraMovementLocalY = 0
        Camera.cameraMovementZ = 0
        Camera.cameraConstantMoveSpeed = 0
        Camera.cameraVariableMoveSpeed = 0
        Camera.lookX = 0
        Camera.lookY = 0
        Camera.lookAtHeightOffset = 0
        Camera.moveCameraSpeed = 0
        Camera.moveCameraAngle = 0

        for (index in VarcTypeList.VARC_INT!!.indices) {
            if (!VarcTypeList.VARC_BOOL!![index]) {
                VarcTypeList.VARC_INT!![index] = -1
            }
        }

        if (Client.BASE_WINDOW_ID != -1) {
            Interface.unloadInterface(Client.BASE_WINDOW_ID)
        }

        var subInterface = Client.OPEN_INTERFACES!!.first()
        while (subInterface != null) {
            if (!subInterface.linked()) {
                subInterface = Client.OPEN_INTERFACES!!.first()
                if (subInterface == null) {
                    break
                }
            }
            Interface.closeChildInterface(subInterface, true, false)
            subInterface = Client.OPEN_INTERFACES!!.next()
        }

        Client.BASE_WINDOW_ID = -1
        Client.OPEN_INTERFACES = IterableNodeMap(8)
        Interface.initializeTables()
        Client.activeDialogueContinue = null

        for (index in 0 until 8) {
            Client.PLAYER_OPTIONS[index] = null
            Client.PLAYER_OPTION_REDUCED_PRIORITY[index] = false
            Client.PLAYER_OPTION_CURSORS[index] = -1
        }

        ItemContainer.resetContainerMap()
        Client.awaitingPriorityRequests = true

        for (index in 0 until 107) {
            Client.IS_COMPONENT_ON_TOP[index] = true
        }

        for (index in 0 until 6) {
            Client.GRAND_EXCHANGE_SLOTS[index] = GrandExchangeSlot()
        }

        Resource.mapRegion!!.environmentManager!!.requestDirectEnvironmentTransition()
        Client.sendCameraAngleToServer = true
        Client.recolorPalette = ShortArray(256).also {
            ObjectType.locRecolorPalette = it
            NPCType.npcRecolorPalette = it
            ItemType.recolorPalette = it
        }
        InterfaceRenderer.translatedUptext = LocalizedText.WALK_HERE.translate(Client.CURRENT_LANGUAGE!!)
        PreferenceManager.preferences!!.setValue(PreferenceManager.preferences!!.removeRoofsOptionOverride!!, PreferenceManager.preferences!!.removeRoofs!!.value)
        Client.varVerifyId = 0
        MiniMenu.reset()
        PacketEncoder.sendWindowmodePreferenceToServer()
        PingRequester.CURRENT_REQUEST = null
        PingRequester.NEXT_PING_UPDATE_TO_SERVER = 0L
    }

    @JvmStatic
    fun prepareHost() {
        if (WorldConnection.CONNECTION_INFO!!.worldId != -1) {
            setGameHost(WorldConnection.CONNECTION_INFO!!.worldId, WorldConnection.CONNECTION_INFO!!.host!!)
        }
    }

    @JvmField
    var MEMBERSHIP_END = 0L

    @JvmField
    var EMAIL_STATUS = 0

    @JvmStatic
    fun initializeLogin(socialNetworkLogin: Boolean, bool1: Boolean, string_2: String, string_3: String, long_4: Long) {
        LoginManager.socialNetworkLogin = socialNetworkLogin
        if (!bool1) {
            StaticMethods.selectedWorldId = -1
        }
        StaticMethods.hasStoredCredentials = bool1
        StaticMethods.loginUsername = string_2
        StaticMethods.loginPassword = string_3
        socialNetworkToken = long_4
        if (StaticMethods.hasStoredCredentials || StaticMethods.loginUsername.isNotEmpty() && StaticMethods.loginPassword.isNotEmpty()) {
            if (lobbyStage != LobbyStage.LOGGING_IN_LOBBY) {
                loginQueuePosition = 0
                loginResponseDetail = -1
                loginResponseExtraInfo = -1
            }
            CURRENT_CONNECTION_CONTEXT!!.dropped = false
            setLoginResponse(-3)
            loginStage = LoginStage.OPENING_CONNECTION
            connectionTickCount = 0
            connectionRetryCount = 0
        } else {
            setLoginResponse(3)
        }
    }

    @JvmField
    var IS_MEMBER_SUBSCRIPTION = false

    @JvmStatic
    fun worldLoginWithBase64Url() {
        lobbyStage = LobbyStage.LOBBY_TO_WORLD
        CURRENT_CONNECTION_CONTEXT = Client.GAME_CONNECTION_CONTEXT
        Client.base64Url?.let { url ->
            val rsbytebuffer1 = Packet(url)
            socialNetworkToken = rsbytebuffer1.readLong()
            socialNetworkNonce = rsbytebuffer1.readLong()
        }
        if (socialNetworkToken < 0L) {
            setLoginResponse(35)
        } else {
            initializeLogin(false, true, "", "", socialNetworkToken)
        }
    }

    @JvmStatic
    fun prepareForIngame() {
        CURRENT_CONNECTION_CONTEXT!!.clearAllQueuedPackets()
        CURRENT_CONNECTION_CONTEXT!!.receivedBuffer.currentPosition = 0
        CURRENT_CONNECTION_CONTEXT!!.lastPacket = null
        CURRENT_CONNECTION_CONTEXT!!.secondLastPacket = null
        CURRENT_CONNECTION_CONTEXT!!.thirdLastPacket = null
        CURRENT_CONNECTION_CONTEXT!!.idleReadPulses = 0
        Client.REBOOT_TIMER = 0
        ChatBox.reset()
        FriendList.LOADED = 0
        FriendList.FRIEND_COUNT = 0
        FriendsChat.FC_NAME = null
        FriendsChat.FC_PLAYER_COUNT = 0
        FriendsChat.FC_PLAYERS = null
        Client.GUEST_CLAN_SETTINGS = null
        Client.CLAN_SETTINGS = null
        MouseActionServer.reset()

        for (skillIndex in 0 until 25) {
            Client.SKILL_LEVEL_VISIBLE[skillIndex] = 0
            Client.SKILL_LEVEL_ACTUAL[skillIndex] = 0
            Client.SKILL_XP[skillIndex] = 0
        }
    }

    @JvmField
    var WORLDS: Array<WorldMeta?>? = null

    @JvmField
    var LOYALTY_BALANCE = 0

    @JvmField
    var LASTLOGINLDAY = 0

    @JvmField
    var connectionTickCount = 0

    @JvmField
    var connectionRetryCount = 0

    @JvmField
    var loginQueuePosition = 0

    @JvmField
    var loginResponseDetail = -1

    @JvmField
    var loginResponseExtraInfo = -1

    @JvmField
    var socialNetworkToken = -1L

    @JvmField
    var socialNetworkNonce = 0L

    @JvmField
    var lobbyCreditCardExpiry = 0

    @JvmField
    var membershipTimeCorrection = 0L

    @JvmField
    var currentlyInLobby = false

    @JvmField
    var isDobRequested = false
}
