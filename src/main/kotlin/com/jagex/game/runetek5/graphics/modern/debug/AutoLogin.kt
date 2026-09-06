package com.jagex.game.runetek5.graphics.modern.debug

import com.darkan.bot.Bot
import com.darkan.bot.utils.Tile
import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.GraphicsCache
import com.jagex.game.runetek5.client.PreferenceManager
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
import com.jagex.game.runetek5.config.TypeLists
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.staticmethods.StaticMethods
import java.io.File

/**
 * Drives the client from the login screen into world 1 without a human at the
 * keyboard, so a renderer change can be verified against what is ACTUALLY ON
 * SCREEN rather than against a headless model dump.
 *
 * Geometry-level oracles (see [GeometryDiff]) proved the mesh-built and legacy
 * paths byte-identical while the visible bugs were completely untouched — the
 * defects live in the scene/culling/draw layers, which only a real logged-in
 * frame exercises. Hence this: build, launch, log in, screenshot, compare, kill.
 *
 * Credentials, in priority order:
 *   1. `-Dautologin=user:pass`
 *   2. env `DARKAN_USER` / `DARKAN_PASS`
 *   3. a file (default `~/.darkan_login`, override with `-Dautologin.file=path`)
 *      containing `user:pass` or two lines
 *
 * Nothing is logged that could leak the password — only the username and the
 * state transitions.
 */
object AutoLogin {

    private const val DEFAULT_CREDENTIAL_FILE = ".darkan_login"

    /**
     * World to join, or null to let the client pick its default.
     *
     * Deliberately NOT defaulted to 1. selectedWorldId is written into the login
     * seed block as a single byte and also switches startWorldLogin() onto the
     * stored-credentials path; forcing 1 made the world login fall straight back
     * to the lobby, whereas leaving it at -1 takes the plain worldLogin path and
     * reaches the server's actual world.
     */
    private val targetWorld: Int? by lazy {
        System.getProperty("autologin.world")?.trim()?.toIntOrNull()
    }

    private data class Credentials(val user: String, val pass: String)

    private val credentials: Credentials? by lazy { resolveCredentials() }

    private fun resolveCredentials(): Credentials? {
        System.getProperty("autologin")?.trim()?.let { prop ->
            if (prop.isNotEmpty() && prop != "true" && prop.contains(':')) {
                return Credentials(prop.substringBefore(':'), prop.substringAfter(':'))
            }
        }

        val envUser = System.getenv("DARKAN_USER")
        val envPass = System.getenv("DARKAN_PASS")
        if (!envUser.isNullOrBlank() && !envPass.isNullOrBlank()) {
            return Credentials(envUser, envPass)
        }

        val path = System.getProperty("autologin.file")
            ?: (System.getProperty("user.home") + File.separator + DEFAULT_CREDENTIAL_FILE)
        val file = File(path)
        if (file.isFile) {
            val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size == 1 && lines[0].contains(':')) {
                return Credentials(lines[0].substringBefore(':'), lines[0].substringAfter(':'))
            }
            if (lines.size >= 2) return Credentials(lines[0], lines[1])
        }
        return null
    }

    fun isEnabled(): Boolean = System.getProperty("autologin") != null

    private var lastState: GameState? = null
    private var lastAttemptNs = 0L
    private var announcedReady = false
    private var readyFrame = 0
    private var spinAccum = 0f
    private var commandIssued = false
    private var buttonIssued = false
    private val pendingCommands = ArrayList<Pair<Int, String>>()
    private var equipIssued = false
    private var bloomForced = false
    private var walkIssued = false
    private var texturesIssued = false

    /**
     * Retry spacing. A login round-trip to prod takes seconds; hammering it every
     * frame would spam the server and trip its throttling.
     */
    private const val RETRY_INTERVAL_NS = 6_000_000_000L

    /**
     * Frames to let the client finish booting before touching login.
     *
     * Two separate failures came from firing on the very first frame: driving
     * setGameState that early blew up on `lateinit PLAYER_VAR_PROVIDER` inside
     * processLobbyTick, and even the direct lobbyLogin produced a handshake the
     * lobby server never answered. The login screen is only genuinely ready some
     * way into startup, and nothing in LoginManager guards against being called
     * before then.
     */
    private const val WARMUP_FRAMES = 600

    private var frames = 0

    /** Call once per frame. Cheap, self-disarming once in-game. */
    fun tick() {
        if (!isEnabled()) return
        if (frames++ < WARMUP_FRAMES) return

        val state = Client.gameState
        if (state != lastState) {
            System.err.println("[AutoLogin] state=$state lobbyStage=${LoginManager.lobbyStage} loginStage=${LoginManager.loginStage}")
            lastState = state
        }

        if (state == GameState.LOGGED_INGAME) {
            // Pin the camera EVERY frame once in-game. Without this, successive
            // captures came back at different yaw/pitch, and comparing two renderers
            // across two different viewpoints is worthless — it already led me to
            // read a camera difference as "the walls are missing".
            // cameraYaw/cameraPitch are OUTPUTS of Camera.processCamera, recomputed
            // every frame from the user-controlled camAngleX/camAngleY (and the zoom),
            // so pinning them changed nothing. Pin the inputs: "-Dautologin.camera=X,Y[,zoom]".
            System.getProperty("autologin.camera")?.let { spec ->
                val parts = spec.split(',').mapNotNull { it.trim().toIntOrNull() }
                if (parts.size >= 2) {
                    Camera.camAngleX = parts[0].toFloat()
                    // "-Dautologin.spin=N": rotate the yaw N units (of 16384) per frame
                    // on top of the pinned value, to measure camera rotation cost.
                    spinAccum += System.getProperty("autologin.spin")?.toFloatOrNull() ?: 0f
                    Camera.camAngleY = ((parts[1] + spinAccum) % 16384f + 16384f) % 16384f
                    if (parts.size >= 3) Camera.cameraZoom = parts[2]
                }
            }
            if (!announcedReady) {
                announcedReady = true
                // The capture script waits for exactly this line.
                System.err.println("[AutoLogin] READY — logged in to world ${targetWorld ?: "(default)"}")
                readyFrame = frames
            }
            // "-Dautologin.bloom=1": switch the toolkit's bloom effect on for this run
            // only (toolkit level, so the preference file is never touched).
            if (!bloomForced && System.getProperty("autologin.bloom") == "1") {
                bloomForced = true
                val ok = Renderers.activeToolkit?.bloomSupported() ?: false
                System.err.println("[AutoLogin] bloom forced on: supported=$ok enabled=${Renderers.activeToolkit?.bloomEnabled()}")
            }
            // "-Dautologin.textures=0|1": flip the Textures graphics option in-game the
            // way the options interface does (preference + graphics cache reset +
            // region reload), without saving the preference file.
            val texturesSpec = System.getProperty("autologin.textures")?.toIntOrNull()
            if (texturesSpec != null && !texturesIssued && frames - readyFrame == 150) {
                texturesIssued = true
                PreferenceManager.preferences?.textures?.setValue(if (texturesSpec == 1) 1 else 0)
                GraphicsCache.resetGraphicCaches()
                System.err.println("[AutoLogin] TEXTURES set to $texturesSpec")
            }
            // Debug walk: "-Dautologin.walk=X,Y" (absolute tile) sends one walk request
            // a short while after login, through the bot's own packet path (no input
            // injection), so captures can be taken under roofs / on other planes.
            val walkSpec = System.getProperty("autologin.walk")
            if (walkSpec != null && !walkIssued && frames - readyFrame == 90) {
                walkIssued = true
                val parts = walkSpec.split(',').mapNotNull { it.trim().toIntOrNull() }
                try {
                    val px = Bot.getMyPlayerX(); val py = Bot.getMyPlayerY()
                    if (parts.size >= 2) {
                        Bot.walkTo(Tile(parts[0], parts[1]))
                        System.err.println("[AutoLogin] WALK from ($px,$py) to (${parts[0]},${parts[1]})")
                    }
                } catch (t: Throwable) {
                    System.err.println("[AutoLogin] WALK failed: $t")
                }
            }
            // "-Dautologin.command=item 20767": one server command (";;" prefix is added
            // by Bot.command) a few seconds after login; "-Dautologin.equip=20767" then
            // equips that item from the inventory once it arrives. Both go through the
            // bot's packet path — no input injection.
            // '+' stands for a space so the value survives the launcher's word splitting.
            val cmdSpec = System.getProperty("autologin.command")?.replace('+', ' ')
            if (cmdSpec != null && !commandIssued && frames - readyFrame == 120) {
                commandIssued = true
                cmdSpec.split(';').map { it.trim() }.filter { it.isNotEmpty() }.forEachIndexed { index, cmd ->
                    pendingCommands.add(Pair(frames + index * 60, cmd))
                }
            }
            val buttonSpec = System.getProperty("autologin.ifbutton")
            if (buttonSpec != null && !buttonIssued && frames - readyFrame == 240) {
                buttonIssued = true
                val parts = buttonSpec.split(':').mapNotNull { it.trim().toIntOrNull() }
                if (parts.size >= 2) {
                    try {
                        Interface.ifOptionInteract(parts.getOrNull(2) ?: 1, (parts[0] shl 16) or parts[1], -1, "")
                        System.err.println("[AutoLogin] IFBUTTON ${parts[0]}:${parts[1]}")
                    } catch (t: Throwable) { System.err.println("[AutoLogin] IFBUTTON failed: $t") }
                }
            }
            while (pendingCommands.isNotEmpty() && pendingCommands.first().first <= frames) {
                val cmd = pendingCommands.removeAt(0).second
                try { Bot.command(cmd); System.err.println("[AutoLogin] COMMAND ;;$cmd") } catch (t: Throwable) { System.err.println("[AutoLogin] COMMAND failed: $t") }
            }
            val equipId = System.getProperty("autologin.equip")?.toIntOrNull()
            if (equipId != null && !equipIssued && frames - readyFrame >= 240 && (frames - readyFrame) % 60 == 0) {
                try {
                    val slot = Bot.inventory.container.itemIds?.indexOf(equipId) ?: -1
                    if (slot >= 0) {
                        equipIssued = true
                        Bot.equipItem(equipId, slot)
                        val name = try { TypeLists.OBJ?.list(equipId)?.name } catch (t: Throwable) { null }
                        System.err.println("[AutoLogin] EQUIP item=$equipId slot=$slot name=$name")
                    } else if ((frames - readyFrame) % 600 == 0) {
                        System.err.println("[AutoLogin] EQUIP waiting: item $equipId not in inventory")
                    }
                } catch (t: Throwable) { System.err.println("[AutoLogin] EQUIP failed: $t") }
            }
            if (frames - readyFrame == 60 || (walkIssued && (frames - readyFrame) % 150 == 0)) {
                try {
                    System.err.println("[AutoLogin] player tile=(${Bot.getMyPlayerX()},${Bot.getMyPlayerY()}) plane=${PlayerUpdate.MY_PLAYER?.plane}")
                } catch (t: Throwable) {
                    System.err.println("[AutoLogin] player tile unavailable: $t")
                }
            }
            return
        }

        val creds = credentials
        if (creds == null) {
            if (!announcedReady) {
                announcedReady = true
                System.err.println(
                    "[AutoLogin] NO CREDENTIALS — set -Dautologin=user:pass, " +
                        "DARKAN_USER/DARKAN_PASS, or ~/$DEFAULT_CREDENTIAL_FILE"
                )
            }
            return
        }

        // A login is already in flight; let it finish.
        if (LoginManager.loginStage != LoginManager.LoginStage.IDLE) return

        val now = System.nanoTime()
        if (now - lastAttemptNs < RETRY_INTERVAL_NS) return
        lastAttemptNs = now

        StaticMethods.loginUsername = creds.user
        StaticMethods.loginPassword = creds.pass

        when (LoginManager.lobbyStage) {
            // Not authenticated yet: authenticate against the lobby first, exactly
            // as the login screen's button does.
            LoginManager.LobbyStage.LOGGED_OUT -> {
                System.err.println("[AutoLogin] lobby login as '${creds.user}' (state=${Client.gameState})")
                LoginManager.lobbyLogin(creds.user, creds.pass)
            }

            LoginManager.LobbyStage.LOGGING_IN_LOBBY -> {
                // LOGGING_IN_LOBBY means "logging in", NOT "logged in". Firing the
                // world login here on stage alone crashed the client:
                // getSeedBlock dereferences lobbyAuthToken!!, which only exists once
                // the lobby response has been parsed.
                //
                // That same response sets MY_PLAYER_USERNAME two fields after
                // lobbyAuthToken (LoginManager:525-533), so it is an exact,
                // observable proxy for "token is present" without touching private
                // state. Wait for it.
                if (PlayerUpdate.MY_PLAYER_USERNAME == null) return

                System.err.println(
                    "[AutoLogin] lobby ready as '${PlayerUpdate.MY_PLAYER_USERNAME}', " +
                        "entering world ${targetWorld ?: "(client default)"}"
                )
                targetWorld?.let { StaticMethods.selectedWorldId = it }
                LoginManager.startWorldLogin()
            }

            LoginManager.LobbyStage.LOBBY_TO_WORLD -> Unit // in flight
        }
    }
}
