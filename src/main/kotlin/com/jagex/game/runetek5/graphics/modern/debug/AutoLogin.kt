package com.jagex.game.runetek5.graphics.modern.debug

import com.jagex.game.runetek5.client.Client
import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.input.camera.Camera
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.entity.pathingentity.player.PlayerUpdate
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
            System.getProperty("autologin.camera")?.let { spec ->
                val parts = spec.split(',').mapNotNull { it.trim().toIntOrNull() }
                if (parts.size >= 2) {
                    Camera.cameraYaw = parts[0]
                    Camera.cameraPitch = parts[1]
                }
            }
            if (!announcedReady) {
                announcedReady = true
                // The capture script waits for exactly this line.
                System.err.println("[AutoLogin] READY — logged in to world ${targetWorld ?: "(default)"}")
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
