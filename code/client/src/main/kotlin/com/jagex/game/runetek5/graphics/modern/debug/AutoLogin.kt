package com.jagex.game.runetek5.graphics.modern.debug

import com.darkan.bot.Bot
import com.darkan.bot.utils.Tile
import com.jagex.game.launch.Loader
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
import com.jagex.game.runetek5.scene.Scene
import com.jagex.game.runetek5.staticmethods.StaticMethods
import java.io.File
import java.lang.management.ManagementFactory

object AutoLogin {

    private const val DEFAULT_CREDENTIAL_FILE = ".darkan_login"

    private const val WORLD_LIST_WAIT_NS = 10_000_000_000L

    private var worldWaitStart = 0L

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
    private var readyNs = 0L
    private var spinAccum = 0f
    private var commandIssued = false
    private var buttonIssued = false
    private val pendingCommands = ArrayList<Pair<Int, String>>()
    private var equipIssued = false
    private var bloomForced = false
    private var walkIssued = false
    private var texturesIssued = false
    private var roofsIssued = false
    private var skyIssued = false

    private const val RETRY_INTERVAL_NS = 1_000_000_000L

    private const val SETTLE_NANOS = 3_000_000_000L

    private var frames = 0
    private var cacheReady = false
    private var lobbyMarked = false
    private var worldMarked = false

    private fun milestone(name: String) {
        val elapsed = System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().startTime
        println("[Milestone] $name ${elapsed}ms")
    }

    fun tick() {
        if (!isEnabled()) return
        frames++

        val state = Client.gameState
        if (state != lastState) {
            System.err.println("[AutoLogin] state=$state lobbyStage=${LoginManager.lobbyStage} loginStage=${LoginManager.loginStage}")
            lastState = state
        }
        if (!cacheReady) {
            if (state != GameState.UNK_5) return
            cacheReady = true
            milestone("cache ready")
        }

        if (state == GameState.LOGGED_INGAME) {
            System.getProperty("autologin.camera")?.let { spec ->
                val parts = spec.split(',').mapNotNull { it.trim().toIntOrNull() }
                if (parts.size >= 2) {
                    Camera.camAngleX = parts[0].toFloat()
                    spinAccum += System.getProperty("autologin.spin")?.toFloatOrNull() ?: 0f
                    Camera.camAngleY = ((parts[1] + spinAccum) % 16384f + 16384f) % 16384f
                    if (parts.size >= 3) Camera.cameraZoom = parts[2]
                }
            }
            if (!announcedReady) {
                announcedReady = true
                milestone("first world frame")
                System.err.println("[AutoLogin] READY — logged in to world ${targetWorld ?: "(default)"}")
                readyFrame = frames
                readyNs = System.nanoTime()
            }
            if (!bloomForced && System.getProperty("autologin.bloom") == "1") {
                bloomForced = true
                val ok = Renderers.activeToolkit?.bloomSupported() ?: false
                System.err.println("[AutoLogin] bloom forced on: supported=$ok enabled=${Renderers.activeToolkit?.bloomEnabled()}")
            }
            val texturesSpec = System.getProperty("autologin.textures")?.toIntOrNull()
            if (texturesSpec != null && !texturesIssued && readyNs != 0L && System.nanoTime() - readyNs >= SETTLE_NANOS) {
                texturesIssued = true
                PreferenceManager.preferences?.textures?.setValue(if (texturesSpec == 1) 1 else 0)
                GraphicsCache.resetGraphicCaches()
                System.err.println("[AutoLogin] TEXTURES set to $texturesSpec")
            }
            val roofSpec = System.getProperty("autologin.roofs")?.trim()?.toIntOrNull()
            if (roofSpec != null && !roofsIssued && readyNs != 0L && System.nanoTime() - readyNs >= SETTLE_NANOS) {
                roofsIssued = true
                val roofValue = roofSpec.coerceIn(0, 2)
                PreferenceManager.preferences?.removeRoofs?.setValue(roofValue)
                PreferenceManager.preferences?.removeRoofsOptionOverride?.setValue(roofValue)
                Scene.setRoofOverride()
                System.err.println("[AutoLogin] ROOFS set to $roofValue")
            }
            val skySpec = System.getProperty("autologin.skybox")?.trim()?.toIntOrNull()
            if (skySpec != null && !skyIssued && readyNs != 0L && System.nanoTime() - readyNs >= SETTLE_NANOS) {
                skyIssued = true
                PreferenceManager.preferences?.skyboxes?.let { PreferenceManager.preferences?.setValue(it, skySpec.coerceIn(0, 1)) }
                System.err.println("[AutoLogin] SKYBOX set to ${skySpec.coerceIn(0, 1)}")
            }
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

        if (LoginManager.loginStage != LoginManager.LoginStage.IDLE) return

        val now = System.nanoTime()
        if (now - lastAttemptNs < RETRY_INTERVAL_NS) return
        lastAttemptNs = now

        StaticMethods.loginUsername = creds.user
        StaticMethods.loginPassword = creds.pass

        when (LoginManager.lobbyStage) {
            LoginManager.LobbyStage.LOGGED_OUT -> {
                System.err.println("[AutoLogin] lobby login as '${creds.user}' (state=${Client.gameState})")
                LoginManager.lobbyLogin(creds.user, creds.pass)
            }

            LoginManager.LobbyStage.LOGGING_IN_LOBBY -> {
                if (PlayerUpdate.MY_PLAYER_USERNAME == null) return
                if (!lobbyMarked) {
                    lobbyMarked = true
                    milestone("lobby logged in")
                }

                System.err.println(
                    "[AutoLogin] lobby ready as '${PlayerUpdate.MY_PLAYER_USERNAME}', " +
                        "entering world ${targetWorld ?: "(client default)"}"
                )
                targetWorld?.let {
                    if (!LoginManager.setGameHost(it, Loader.IP_ADDRESS ?: "localhost")) {
                        if (worldWaitStart == 0L) worldWaitStart = now
                        if (now - worldWaitStart < WORLD_LIST_WAIT_NS) return
                        System.err.println("[AutoLogin] world $it is not in the world list after 10s, using the default connection")
                    }
                }
                if (!worldMarked) {
                    worldMarked = true
                    milestone("world selected")
                }
                if (Client.gameState == GameState.UNK_0) LoginManager.returnToLoginScreen() else LoginManager.startWorldLogin()
            }

            LoginManager.LobbyStage.LOBBY_TO_WORLD -> Unit
        }
    }
}
