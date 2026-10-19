package org.darkan.lobby

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import org.darkan.core.EnvVars
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import org.darkan.core.deploy.ArtifactWatcher
import org.darkan.core.Logger
import org.darkan.core.Logger.logInfo
import org.darkan.core.engine.EngineLoop
import org.darkan.core.formatNumber
import org.darkan.core.net.JS5Server
import org.darkan.core.net.prot.handler.PacketHandlers
import org.darkan.core.net.web.DiscordWebhook
import org.darkan.core.ticksToTimeString
import org.darkan.core.worldlist.WorldList
import org.darkan.lobby.server.LobbyServer
import org.darkan.lobby.web.model.AccountCreationSession
import org.darkan.lobby.web.model.LobbyPlayer
import org.darkan.lobby.web.module
import world.gregs.voidps.cache.Cache
import world.gregs.voidps.cache.file.FileProvider
import world.gregs.voidps.cache.file.prefetchKeys
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/** 20 ticks, matching the world's login cutoff before a system update. */
private const val UPDATE_LOGIN_CUTOFF_MILLIS = 20 * 600L

object Lobby : CoroutineScope {
    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    private lateinit var cache: Cache
    private lateinit var cacheProvider: FileProvider
    private lateinit var js5Server: JS5Server
    private lateinit var httpServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private lateinit var lobbyServer: LobbyServer

    val worldList = WorldList(300)

    @Volatile
    var cacheCrcs: IntArray = IntArray(0)
        private set
    private val accountCreationSessions = ConcurrentHashMap.newKeySet<AccountCreationSession>()
    private val lobbyPlayers = ConcurrentHashMap<String, LobbyPlayer>()
    private lateinit var lobbyEngineLoop: EngineLoop

    /** Epoch millis of the scheduled restart, or 0 when none is pending. */
    @Volatile
    private var updateAt = 0L
    private val updateWatcher = ArtifactWatcher("lobby-server", this) { version ->
        scheduleUpdate(EnvVars.autoUpdateCountdownMinutes.minutes, "build $version")
    }

    /** True inside the last moments before a scheduled restart; logins are refused so nobody lands on a dying lobby. */
    val updatingSoon: Boolean
        get() = updateAt != 0L && updateAt - System.currentTimeMillis() < UPDATE_LOGIN_CUTOFF_MILLIS

    /**
     * The lobby's equivalent of the world's `::update`: stops accepting logins near the end of
     * [countdown], then stops cleanly. The run script restarts the process on the newest build, and
     * the worlds' social clients reconnect on their own.
     */
    fun scheduleUpdate(countdown: Duration, reason: String) {
        if (updateAt != 0L) return
        updateAt = System.currentTimeMillis() + countdown.inWholeMilliseconds
        logInfo("Lobby restart scheduled in $countdown ($reason)")
        // A plain thread, not a coroutine on this scope: stop() cancels and joins the scope, which
        // would cancel the caller itself and never reach the exit. exitProcess runs the shutdown
        // hook from Main, which performs the clean stop() on its own thread.
        thread(name = "lobby-update", isDaemon = true) {
            Thread.sleep(countdown.inWholeMilliseconds)
            logInfo("Restarting lobby for $reason")
            exitProcess(0)
        }
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun start() {
        logInfo("Starting application services...")
        cache = Cache.get()
        cacheCrcs = cache.indexCrcs()
        cacheProvider = FileProvider.load(cache)
        js5Server = JS5Server(cacheProvider, prefetchKeys(cache))
        PacketHandlers.loadHandlersFromPackage("org.darkan.lobby.server.packet")
        startLobbyAPI()
        runBlocking {
            startLobbyServer()
        }
        lobbyEngineLoop = EngineLoop(
            arrayOf(
                processInput,
                flushOutgoingNetworkSessions,
                expireOfflineWorlds
            ),
            ::reportTickConcern
        )
        lobbyEngineLoop.start(CoroutineScope(newSingleThreadContext("Lobby engine loop")))
        updateWatcher.start()
        logInfo("All services started successfully")
    }

    suspend fun stop() {
        logInfo("Stopping application services...")

        try {
            if (::lobbyServer.isInitialized) {
                lobbyServer.stop()
            }

            if (::httpServer.isInitialized) {
                httpServer.stop(1000, 2000)
            }

            job.cancelAndJoin()
            logInfo("All services stopped successfully")
        } catch (e: Exception) {
            Logger.logError("Error during shutdown", e)
            throw e
        }
    }

    private fun startLobbyAPI() {
        httpServer = embeddedServer(Netty, port = EnvVars.lobbyApiPort, module = Application::module)
        httpServer.start(wait = false)
        logInfo("API server started on port ${EnvVars.lobbyApiPort}")
    }

    private suspend fun startLobbyServer() {
        lobbyServer = LobbyServer(js5Server)
        lobbyServer.start()
    }

    fun addLobbyPlayer(lobbyPlayer: LobbyPlayer) = lobbyPlayers.put(lobbyPlayer.account.username, lobbyPlayer)

    /** Only the session that owns the entry may drop it: a stale goodbye must not evict a fresh login. */
    fun removeLobbyPlayer(lobbyPlayer: LobbyPlayer) = lobbyPlayers.remove(lobbyPlayer.account.username, lobbyPlayer)

    fun addAccountCreation(session: AccountCreationSession) = accountCreationSessions.add(session)
    fun removeAccountCreation(session: AccountCreationSession) = accountCreationSessions.remove(session)

    val processInput = Runnable {
        runBlocking {
            lobbyPlayers.values.forEach { it.handleDecodedPackets() }
            accountCreationSessions.forEach { it.handleDecodedPackets() }
        }
    }

    val expireOfflineWorlds = Runnable {
        worldList.expireOffline().forEach { logInfo("Removed world ${it.metadata.number} (${it.metadata.activity}) after ${WorldList.OFFLINE_TTL} offline") }
    }

    val flushOutgoingNetworkSessions = Runnable {
        // Only signals each session's writer coroutine, so one stalled socket can't hold the tick.
        lobbyPlayers.values.forEach { it.session.requestFlush() }
        accountCreationSessions.forEach { it.session.requestFlush() }
    }

    fun reportTickConcern(actualTime: Long, stepTimes: Map<String, Long>) {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val heap = memoryBean.heapMemoryUsage
        val nonHeap = memoryBean.nonHeapMemoryUsage

        val heapUsed = heap.used / 1024 / 1024
        val nonHeapUsed = nonHeap.used / 1024 / 1024
        val totalUsed = heapUsed + nonHeapUsed
        val maxMemory = (heap.max + nonHeap.max) / 1024 / 1024
        val usagePercent = (totalUsed.toDouble() / maxMemory) * 100

        val content = buildString {
            appendLine("__**Tick concern**__")
            appendLine("__**Lobby Data**__")
            appendLine("```")
            appendLine("Lobby${EnvVars.serverName} ${if (EnvVars.debug) "(debug)" else ""}")
            appendLine("Uptime: ${lobbyEngineLoop.uptimeTicks.ticksToTimeString()}")
            appendLine("Account creation sessions: ${accountCreationSessions.size.formatNumber()}")
            appendLine("Player sessions: ${lobbyPlayers.size.formatNumber()}")
            appendLine("```")
            appendLine("__**Tick Time: ${actualTime}ms (min: ${lobbyEngineLoop.lowestMillis.formatNumber()}ms avg: ${lobbyEngineLoop.averageMillis.formatNumber()}ms max: ${lobbyEngineLoop.highestMillis.formatNumber()}ms)**__")
            appendLine("```")
            for (key in stepTimes.keys)
                appendLine("$key: ${stepTimes[key]}")
            appendLine("```")
            appendLine("__**JVM Stats:**__")
            appendLine("```")
            appendLine("Total JVM memory usage: ${totalUsed.formatNumber()}mb/${maxMemory.formatNumber()}mb (${usagePercent.formatNumber()}%)")
            appendLine("```")
        }
        DiscordWebhook.sendStaffMessage(content)
    }
}
