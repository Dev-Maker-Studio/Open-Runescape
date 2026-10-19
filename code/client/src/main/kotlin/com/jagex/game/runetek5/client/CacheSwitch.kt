package com.jagex.game.runetek5.client

import com.jagex.game.runetek5.cache.node.IterableNodeMap
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.config.Resource
import com.jagex.game.runetek5.config.rsinterface.Interface
import com.jagex.game.runetek5.config.rsinterface.cs2.CS2Executor
import com.jagex.game.runetek5.js5.Js5Endpoint
import com.jagex.game.runetek5.network.Connection
import com.jagex.game.runetek5.network.ConnectionInfo
import com.jagex.game.runetek5.rendering.Renderers
import com.jagex.game.runetek5.toolkit.OpenGlToolkit
import com.jagex.game.runetek5.utils.Utils

object CacheSwitch {

    private var resume: GameState? = null
    private var resuming = false
    private var credentials: LoginManager.Credentials? = null
    private var gameConnection: ConnectionInfo? = null
    private var cameFromLobby = false
    private var connectingText: String? = null

    @JvmStatic
    val inProgress: Boolean
        get() = resume != null

    @JvmStatic
    fun drawingProgress(): Boolean = inProgress || (connectingText != null && isLoginState(Client.gameState))

    @JvmStatic
    fun progressText(): String? = if (inProgress) LoadingStage.LOADING_TEXT else connectingText

    @JvmStatic
    fun stateChanged(state: GameState) {
        if (connectingText != null && !isLoginState(state)) connectingText = null
    }

    private fun isLoginState(state: GameState) = state == GameState.UNK_19 || state == GameState.UNK_12 || state == GameState.UNK_14

    @JvmStatic
    fun intercept(state: GameState): Boolean {
        if (inProgress || resuming) return false
        val target = endpointFor(state) ?: return false
        if (Js5Endpoint.current().equals(target)) return false
        begin(target, state)
        return true
    }

    private fun endpointFor(state: GameState): ConnectionInfo? = when (state) {
        GameState.UNK_7, GameState.UNK_19 -> LoginManager.GAME_CONNECTION_INFO?.let { Js5Endpoint.forWorld(it.worldId) }
        GameState.UNK_5, GameState.UNK_12, GameState.UNK_14 -> Js5Endpoint.lobby()
        else -> null
    }

    private fun begin(target: ConnectionInfo, state: GameState) {
        val toWorld = !Js5Endpoint.isLobby(target)
        println("[CacheSwitch] ${Js5Endpoint.describe(Js5Endpoint.current())} -> ${Js5Endpoint.describe(target)} for state $state")
        resume = if (toWorld) GameState.UNK_19 else state
        credentials = LoginManager.captureCredentials()
        gameConnection = LoginManager.GAME_CONNECTION_INFO
        cameFromLobby = LoginManager.currentlyInLobby
        if (toWorld) {
            Client.LOBBY_CONNECTION_CONTEXT.closeConnection()
        }
        closeInterfaces()
        CS2Executor.CS2_CACHE.reset()
        Js5Endpoint.active = target
        dropUpdateServer()
        LoadingStage.restart(if (toWorld) "Loading World ${target.worldId}'s files - " else "Loading Darkan's files - ")
        Client.gameState = GameState.UNK_4
    }

    @JvmStatic
    fun finish(): Boolean {
        val state = resume ?: return false
        resume = null
        println("[CacheSwitch] now on ${Js5Endpoint.describe(Js5Endpoint.current())}, resuming $state")
        resuming = true
        try {
            if (state == GameState.UNK_5) {
                Client.setGameState(GameState.UNK_5)
            }
            credentials?.let { LoginManager.restoreCredentials(it) }
            credentials = null
            gameConnection?.let { LoginManager.GAME_CONNECTION_INFO = it }
            gameConnection = null
            LoginManager.currentlyInLobby = cameFromLobby
            if (state != GameState.UNK_5) {
                connectingText = if (state == GameState.UNK_19) "Connecting to World ${LoginManager.GAME_CONNECTION_INFO?.worldId}" else "Returning to the lobby"
                Client.setGameState(state)
            }
        } finally {
            resuming = false
        }
        return true
    }

    @JvmStatic
    fun reloadToolkitAssets() {
        Client.resetGraphicsState(shutdownToolkit = false)
        GraphicsCache.resetWorldmapCaches(false)
        (Renderers.activeToolkit as? OpenGlToolkit)?.textureCache?.resetBloomCache()
        Client.reloadToolkitAssets()
    }

    private fun closeInterfaces() {
        if (Client.BASE_WINDOW_ID != -1 && Interface.INTERFACES != null) {
            Interface.unloadInterface(Client.BASE_WINDOW_ID)
        }
        Client.BASE_WINDOW_ID = -1
        Client.OPEN_INTERFACES = IterableNodeMap(8)
    }

    private fun dropUpdateServer() {
        Resource.remoteFileRequesterJs5!!.disconnect()
        try {
            Client.clientSocket?.close()
        } catch (ignored: Exception) {
            // NOOP
        }
        Client.clientSocket = null
        Connection.updateConnection = null
        Client.updateStage = 0
        Client.failedRequests = 0
        Client.pulsesBeforeJs5Reconnect = 0
        Client.requestManager = null
        Resource.remoteFileWorkersByIndex = null
        ClientStartup.loadingStatusDependencies = 0
        Client.js5ConnectTime = Utils.time()
    }
}
