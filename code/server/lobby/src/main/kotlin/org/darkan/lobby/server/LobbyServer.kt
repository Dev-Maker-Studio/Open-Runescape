package org.darkan.lobby.server

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.*
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.io.readByteArray
import kotlinx.io.readUByte
import kotlinx.io.readUShort
import org.darkan.core.EnvVars
import org.darkan.core.Logger.logError
import org.darkan.core.Logger.logInfo
import org.darkan.core.Logger.logTrace
import org.darkan.core.net.Session
import org.darkan.core.crypt.Crypto
import org.darkan.core.generateRandom24ByteArray
import org.darkan.core.model.MachineInformation
import org.darkan.core.model.Account
import org.darkan.core.mongo.collections.Accounts
import org.darkan.core.net.*
import org.darkan.core.isBotName
import org.darkan.core.net.login.HeadlessClient
import org.darkan.core.net.login.LOGIN_CRC_COUNT
import org.darkan.core.net.login.LobbyLoginRequest
import org.darkan.core.net.login.LoginDecode
import org.darkan.core.net.login.LoginRsaKey
import org.darkan.core.net.prot.Codec
import org.darkan.core.net.prot.packet.UpdateUid192
import org.darkan.lobby.Lobby
import org.darkan.lobby.web.model.AccountCreationSession
import org.darkan.lobby.web.model.LobbyPlayer
import world.gregs.voidps.buffer.*
import world.gregs.voidps.cache.Cache
import world.gregs.voidps.cache.secure.RSA
import world.gregs.voidps.cache.secure.decryptXtea
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class LobbyServer(val js5: JS5Server) {
    private lateinit var job: Job
    private lateinit var dispatcher: ExecutorCoroutineDispatcher

    private lateinit var serverSocket: ServerSocket
    private lateinit var selectorManager: SelectorManager

    private val js5RsaMod = BigInteger(EnvVars.js5RsaModulus)
    private val js5RsaExp = BigInteger(EnvVars.js5RsaExponent)
    private val loginKey = LoginRsaKey(js5RsaMod, js5RsaExp)

    private val pendingLogins = ConcurrentHashMap.newKeySet<String>()
    private val botIpWhitelist = IpAllowList(EnvVars.botIpWhitelist)

    suspend fun start(): Job {
        val executor = Executors.newCachedThreadPool()
        dispatcher = executor.asCoroutineDispatcher()
        selectorManager = ActorSelectorManager(dispatcher)
        val scope = CoroutineScope(dispatcher)
        logInfo("Starting lobby server...")
        serverSocket = aSocket(selectorManager).tcp().bind("0.0.0.0", EnvVars.lobbyPort) { reuseAddress = true }
        logInfo("Started lobby server...")
        job = scope.launch {
            try {
                supervisorScope {
                    logInfo("Lobby server started on port ${EnvVars.lobbyPort}")
                    while (isActive) {
                        val socket = serverSocket.accept()
                        connectClient(socket)
                    }
                }
            } catch (_: CancellationException) {
                logInfo("Lobby server stopping...")
            } catch (e: Exception) {
                logError("Error in Lobby server", e)
            }
        }
        return job
    }

    fun stop() {
        try {
            job.cancel()
            dispatcher.close()
            if (::serverSocket.isInitialized)
                serverSocket.close()
            if (::selectorManager.isInitialized)
                selectorManager.close()
        } catch (e: Exception) {
            logError("Error stopping Lobby server", e)
            throw e
        }
    }

    private fun CoroutineScope.connectClient(socket: Socket) = launch(dispatcher + CoroutineExceptionHandler { context, throwable -> logTrace("Error connecting client: ${throwable.message}") }) {
        try {
            val input = socket.openReadChannel()
            val output = socket.openWriteChannel(autoFlush = false)
            val peerIp = socket.remoteAddress.ipString()
            val ip = ProxyProtocol.clientIp(peerIp, input)
            if (ip == null) {
                logInfo("Dropped connection from trusted proxy $peerIp without a valid PROXY header")
                return@launch
            }
            logInfo("Client connected: $ip")

            try {
                when (val reqOpcode = input.readByte().toInt()) {
                    RequestOpcode.CONNECT_LOGIN -> init(input, output, ip)
                    RequestOpcode.JS5_INIT -> js5.init(input, output, ip)
                    RequestOpcode.ACCOUNT_CREATION -> initAccountCreation(input, output, ip)
                    else -> {
                        logTrace("Invalid request opcode: $reqOpcode")
                        output.finish(ResponseOpcode.INVALID_LOGIN_SERVER)
                    }
                }
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
            when {
                e is CancellationException -> {}
                Session.isExpectedDisconnect(e) -> logTrace("Lobby client disconnected: ${e::class.simpleName}")
                else -> logError("Error handling client connection", e)
            }
        } finally {
            socket.close()
        }
    }

    private suspend fun init(input: ByteReadChannel, output: ByteWriteChannel, ip: String) {
        output.respond(ResponseOpcode.JS5_SYNC)
        val opcode = input.readByte().toInt()
        if (opcode != RequestOpcode.LOBBY) return output.finish(ResponseOpcode.COULD_NOT_COMPLETE_LOGIN)
        if (pendingLogins.size > MAX_LOGINS_IN_FLIGHT) return output.finish(ResponseOpcode.NO_REPLY_FROM_LOGIN_SERVER)
        if (Lobby.updatingSoon) return output.finish(ResponseOpcode.SERVER_BEING_UPDATED)
        val size = input.readShort().toInt()
        val packet = input.readPacket(size)
        val major = packet.readInt()
        val minor = packet.readInt()
        val codec = Codec.get(major) ?: return output.finish(ResponseOpcode.GAME_UPDATE)
        val crcCount = minOf(Cache.get().indexCount(), LOGIN_CRC_COUNT)
        val request = when (val decoded = LobbyLoginRequest.decode(major, minor, packet, loginKey, crcCount)) {
            is LoginDecode.Rejected -> return output.finish(decoded.response)
            is LoginDecode.Decoded -> decoded.request
        }
        val username = request.username
        val password = request.password
        val isaacKeys = request.isaacSeed

        if (request.loginServerToken != EnvVars.loginServerToken)
            logError("Login server token mismatch: ${EnvVars.loginServerToken}, ${request.loginServerToken} from $username")
        if (request.js5ServerToken != EnvVars.js5ServerToken)
            logError("JS5 server token mismatch: ${EnvVars.js5ServerToken}, ${request.js5ServerToken} from $username")
        if (request.clientKey != EnvVars.clientKey)
            logError("Client key mismatch: ${EnvVars.clientKey}, ${request.clientKey} from $username")

        val crcs = Cache.get().indexCrcs()
        for (index in 0 until minOf(crcCount, INDEX_CRCS_CHECKED)) {
            if (crcs[index] != request.indexCrcs[index]) {
                logError("CRC mismatch: ${crcs[index]}, ${request.indexCrcs[index]} from $username")
                return output.finish(ResponseOpcode.GAME_UPDATE)
            }
        }

        if (!pendingLogins.add(username)) return output.finish(ResponseOpcode.LOGIN_LIMIT_EXCEEDED)
        // Whatever happens below, the username must not stay in pendingLogins - a throwing decode
        // used to leave it there for good and lock the account out until restart.
        val account = try {
            if (HeadlessClient.isHeadless(request.fingerprint) && username.isBotName())
                authenticateBot(username, ip, output) ?: return
            else
                authenticate(username, password, output) ?: return
        } finally {
            pendingLogins.remove(username)
        }

        logInfo("Logging in $username")
        val session = initSession(output, isaacKeys, ip, codec)
        val lobbyPlayer = LobbyPlayer(session, account)
        Lobby.addLobbyPlayer(lobbyPlayer)
        session.onDisconnected { Lobby.removeLobbyPlayer(lobbyPlayer) }
        lobbyPlayer.login(input)
    }

    /**
     * Resolves and verifies the account for a login attempt. Returns null after writing the
     * response when the login must be refused. Argon2 is authoritative; the older SHA-1/AES
     * credentials are only consulted until the account has been upgraded by a successful login.
     */
    private suspend fun authenticate(username: String, password: String, output: ByteWriteChannel): Account? {
        val account = try {
            Accounts.find(username)
        } catch (e: Exception) {
            logError("Failed to load account $username", e)
            output.finish(ResponseOpcode.COULD_NOT_COMPLETE_LOGIN)
            return null
        }
        if (account == null) {
            logInfo("Account not found: $username")
            output.finish(ResponseOpcode.INVALID_CREDENTIALS)
            return null
        }
        if (account.bot) {
            logInfo("Refused password login to bot account $username")
            output.finish(ResponseOpcode.INVALID_CREDENTIALS)
            return null
        }
        if (Crypto.isArgon2Hash(account.passwordHash)) {
            if (!Crypto.verifyPasswordArgon2(password, account.passwordHash)) {
                logInfo("Invalid argon password hash for found: $username")
                output.finish(ResponseOpcode.INVALID_CREDENTIALS)
                return null
            }
            //Drop any AES/SHA-1 credential left behind by an earlier upgrade
            if (account.password != null || account.legacyPass != null) {
                account.password = null
                account.legacyPass = null
                Accounts.save(account)
            }
        } else if (account.password != null) {
            if (!Crypto.legacyCompare(password, account.password!!.map { it.toByte() }.toByteArray())) {
                logInfo("Invalid legacy password hash for found: $username")
                output.finish(ResponseOpcode.INVALID_CREDENTIALS)
                return null
            }
            account.passwordHash = Crypto.hashPasswordArgon2(password)
            account.password = null
            Accounts.save(account)
        } else if (account.legacyPass != null) {
            if (!Crypto.gigaLegacyCompare(password, account.legacyPass!!)) {
                logInfo("Invalid giga legacy password hash for found: $username")
                output.finish(ResponseOpcode.INVALID_CREDENTIALS)
                return null
            }
            account.passwordHash = Crypto.hashPasswordArgon2(password)
            account.legacyPass = null
            Accounts.save(account)
        } else {
            logInfo("Account has no usable credential: $username")
            output.finish(ResponseOpcode.INVALID_CREDENTIALS)
            return null
        }
        return account
    }

    private suspend fun authenticateBot(username: String, ip: String, output: ByteWriteChannel): Account? {
        if (!botIpWhitelist.contains(ip)) {
            logInfo("Refused bot login $username from non-whitelisted $ip")
            output.finish(ResponseOpcode.INVALID_CREDENTIALS)
            return null
        }
        val account = try {
            Accounts.find(username) ?: Accounts.createBot(username)?.also { logInfo("Created bot account $username for $ip") }
        } catch (e: Exception) {
            logError("Failed to load bot account $username", e)
            output.finish(ResponseOpcode.COULD_NOT_COMPLETE_LOGIN)
            return null
        }
        if (account == null || !account.bot) {
            logInfo("Refused bot login $username: name belongs to a player account")
            output.finish(ResponseOpcode.INVALID_CREDENTIALS)
            return null
        }
        return account
    }

    private suspend fun initAccountCreation(input: ByteReadChannel, output: ByteWriteChannel, ip: String) {
        if (pendingLogins.size > MAX_LOGINS_IN_FLIGHT) return output.finish(ResponseOpcode.NO_REPLY_FROM_LOGIN_SERVER)
        val size = input.readShort().toInt()
        val packet = input.readPacket(size)
        val major = packet.readShort().toInt()
        val minor = packet.readShort().toInt()
        val rsaSize = packet.readUShort().toInt()
        val codec = Codec.get(major) ?: return output.finish(ResponseOpcode.GAME_UPDATE)
        val sensitiveData = ByteReadPacket(RSA.crypt(packet.readByteArray(rsaSize), js5RsaMod, js5RsaExp))
        if (sensitiveData.readUByte().toInt() != 10) return output.finish(ResponseOpcode.BAD_SESSION_ID)
        val isaacKeys = IntArray(4) { sensitiveData.readInt() }

        //read 10 random ints and a random short
        for (i in 1..10)
            sensitiveData.readInt()
        sensitiveData.readUShort()

        val xtea = packet.decryptXtea(isaacKeys)
        val js5ServerToken = xtea.readRSString()
        val affiliate = xtea.readShort()
        val clientParam26 = xtea.readLong()
        val clientParam13 = xtea.readRSString()
        val languageId = xtea.readUByte()
        val gameId = xtea.readUByte()
        val randomDat = xtea.readByteArray(24)
        if (xtea.readBoolean())
            xtea.readRSString() //loginServerToken?
        val machineInfo = MachineInformation.parse(xtea)

//        if (loginServerToken != EnvVars.loginServerToken) {
//            logError("Login server token mismatch: ${EnvVars.loginServerToken}, $loginServerToken from $username")
//            //return output.finish(ResponseOpcode.GAME_UPDATE)
//        }
//
        if (js5ServerToken != EnvVars.js5ServerToken) {
            logError("JS5 server token mismatch: ${EnvVars.js5ServerToken}, $js5ServerToken from $ip")
            //return output.finish(ResponseOpcode.GAME_UPDATE)
        }
//
//        if (clientKey != EnvVars.clientKey) {
//            logError("Client key mismatch: ${EnvVars.clientKey}, $clientKey from $username")
//            //return output.finish(ResponseOpcode.GAME_UPDATE)
//        }

        logInfo("Account creation session started from $ip")
        val session = initSession(output, isaacKeys, ip, codec)
        val accCreationSession = AccountCreationSession(session)
        session.onDisconnected { Lobby.removeAccountCreation(accCreationSession) }
        Lobby.addAccountCreation(accCreationSession)
        output.respond(ResponseOpcode.SUCCESS)
        if (randomDat.any { it != (-1).toByte() })
            session.send(UpdateUid192(generateRandom24ByteArray()))
        session.readPackets(input)
    }

    private companion object {
        // The client retries NO_REPLY_FROM_LOGIN_SERVER by itself, so a login rush waits its turn
        // rather than being told its password is insecure, which is what response 11 renders as.
        const val MAX_LOGINS_IN_FLIGHT = 20
        const val INDEX_CRCS_CHECKED = 32
    }

    private fun initSession(write: ByteWriteChannel, isaacKeys: IntArray, hostname: String, codec: Codec): Session {
        val inCipher = Isaac(isaacKeys)
        for (i in isaacKeys.indices)
            isaacKeys[i] += 50
        val outCipher = Isaac(isaacKeys)
        return Session(write, inCipher, outCipher, hostname, codec)
    }
}