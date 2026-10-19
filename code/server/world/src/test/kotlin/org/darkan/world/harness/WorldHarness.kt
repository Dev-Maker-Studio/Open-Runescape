package org.darkan.world.harness

import com.rs.Settings
import com.rs.engine.thread.AsyncTaskExecutor
import com.rs.engine.thread.WorldThread
import com.rs.game.World
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.PluginManager
import org.darkan.core.model.MachineInformation
import com.rs.utils.MapXTEAs
import com.rs.utils.json.WorldGson
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.darkan.core.JsonFileManager
import org.darkan.core.Logger
import org.darkan.core.map.MapXteas
import org.darkan.core.model.Account
import org.darkan.core.model.Rights
import org.darkan.core.net.prot.ClientProt
import org.darkan.core.net.prot.Codec
import org.darkan.core.net.prot.handler.PacketHandlers
import org.darkan.core.net.prot.revision.rev727.register727
import org.darkan.world.server.WorldServer
import world.gregs.voidps.cache.Cache
import java.util.concurrent.Executors
import java.util.logging.Level

/**
 * Boots exactly as much of the world server as content needs, with no socket, no world thread and
 * no database, so dispatch can be driven and asserted from a unit test.
 *
 * Definitions come from [TestDefinitions], never from a packed cache, so the suite passes on a
 * machine that has never built one.
 *
 * What it boots, in [Launcher][com.rs.Launcher] order: config, cache, map XTEAs, the rev727 codec,
 * the async executors, the plugin system (handler registration + `@ServerStartupEvent` hooks, which
 * is where every `Commands.add` lives) and the packet handler registry.
 *
 * What it does not boot: `WorldDB` (so nothing writes to Mongo - the log writers swallow the
 * uninitialised-connection error on their own executor), `WorldServer.start` (no listening socket,
 * no social gateway) and `WorldThread` (the test drives ticks itself via [tick]).
 *
 * The test task runs with `workingDir = rootDir`, so `.env`, `data/` and the built cache resolve
 * the same way they do for a real server.
 */
object WorldHarness {

    @Volatile
    private var booted = false

    @Volatile
    private var cacheDefinitions = false

    /** Lets the world boot on an empty cache; a server without this dies on a cache it cannot read. */
    private const val OPTIONAL_CACHE = "darkan.cache.optional"

    /** The rev727 codec, available once [boot] has run. */
    lateinit var codec: Codec
        private set

    @Synchronized
    fun boot() {
        if (booted) return

        Logger.setupFormat()
        Logger.setLevel(Level.WARNING)
        JsonFileManager.setGSON(WorldGson.create())

        System.setProperty(OPTIONAL_CACHE, "true")
        Settings.loadConfig()
        if (!cacheDefinitions) TestDefinitions.install()
        Cache.init(Settings.getConfig().cachePath)
        MapXTEAs.loadKeys()
        MapXteas.load("./data/map/xteaKeys.json")

        register727()
        codec = requireNotNull(Codec.get(727)) { "rev727 codec did not register" }

        AsyncTaskExecutor.initExecutors()

        // Player.run() fires off account/clan/presence work on WorldServer's dispatcher. The real
        // one is created by WorldServer.start(); the harness installs a plain thread pool instead
        // so nothing opens a socket. socialClient stays unset - every use of it inside those
        // coroutines is already wrapped in try/catch.
        installWorldServerDispatcher()

        PluginManager.loadPlugins()
        PluginManager.executeStartupHooks()
        PacketHandlers.loadHandlersFromPackage("org.darkan.world.server.packet")

        WorldThread.WORLD_CYCLE = System.currentTimeMillis() / 600L
        booted = true
    }

    /**
     * Creates a fully initialised, started player backed by a [RecordingSession], the same way
     * `WorldServer.initWorldLogin` does: `init(...)`, `start()`, flush, `setStarted()`.
     */
    fun login(
        username: String = "harness",
        rights: Rights = Rights.OWNER,
        screenMode: Int = 2,
    ): HarnessPlayer {
        boot()
        val account = Account(username, "$username@test.invalid", "hash").apply {
            this.rights = rights
        }.view()
        val session = RecordingSession(codec)
        val player = Player(account)
        player.init(session, account, screenMode, 1024, 768, MachineInformation())
        // Before start(), which is where an unchosen account type takes over the login: tutorial
        // island in a normal build, character customising in a debug one. Either leaves the player
        // locked, so setting it afterwards made every test read the developer's own .env.
        player.isChosenAccountType = true
        player.start()
        player.setStarted()
        return HarnessPlayer(player, session)
    }

    /** For `cacheTest`, whose subject is what the packed cache holds rather than how content behaves. */
    @Synchronized
    fun bootWithCacheDefinitions() {
        check(!booted || cacheDefinitions) { "the harness has already booted on test definitions" }
        cacheDefinitions = true
        boot()
    }

    /** Removes [player] from the world so a later login of the same name is not rejected. */
    fun logout(player: HarnessPlayer) {
        World.removePlayer(player.player)
    }

    /**
     * Runs one world tick over the players and NPCs currently in the world, mirroring
     * [WorldThread]'s body: chunk processing, scheduled tasks, entity processing, movement, the
     * post-sync pass that actually emits the inventory/var/XP packets, and the flush request.
     */
    fun tick() {
        ChunkManager.processChunks()
        WorldTasks.processTasks()

        for (npc in World.NPCs) {
            if (npc == null || npc.hasFinished()) continue
            npc.processTasks()
        }
        for (player in World.players) {
            player?.processTasks()
        }
        for (npc in World.NPCs) {
            if (npc == null || npc.hasFinished()) continue
            npc.processEntity()
        }
        for (player in startedPlayers()) {
            player.processEntity()
        }
        for (npc in World.NPCs) {
            if (npc == null || npc.hasFinished()) continue
            npc.processMovement()
        }
        for (player in startedPlayers()) {
            player.processMovement()
        }
        for (player in startedPlayers()) {
            player.packets.sendLocalPlayersUpdate()
            player.packets.sendLocalNPCsUpdate()
            player.postSync()
        }
        ChunkManager.processUpdateZones()
        for (player in startedPlayers()) {
            player.session.requestFlush()
        }
        WorldThread.WORLD_CYCLE++
    }

    /** Runs [count] ticks. */
    fun tick(count: Int) {
        repeat(count) { tick() }
    }

    fun tickUntil(timeoutMillis: Long = 30_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "Condition not met within ${timeoutMillis}ms of ticking" }
            tick()
            Thread.sleep(10)
        }
    }

    private fun startedPlayers(): List<Player> =
        World.players.filterNotNull().filter { it.hasStarted() && !it.hasFinished() }

    private fun installWorldServerDispatcher() {
        val dispatcher: ExecutorCoroutineDispatcher =
            Executors.newSingleThreadExecutor { r -> Thread(r, "harness-worldserver").apply { isDaemon = true } }
                .asCoroutineDispatcher()
        val field = WorldServer::class.java.getDeclaredField("dispatcher").apply { isAccessible = true }
        field.set(WorldServer, dispatcher)
    }

    private val Player.packets get() = getPackets()
}

/** A logged-in harness player together with the session recording everything sent to it. */
data class HarnessPlayer(val player: Player, val session: RecordingSession) {
    /** Everything queued to the client so far. */
    val sent get() = session.sent

    fun clearSent() = session.clearSent()

    fun push(packet: ClientProt) = session.pushClientPacket(packet)
}
