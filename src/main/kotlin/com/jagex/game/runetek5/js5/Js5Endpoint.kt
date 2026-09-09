package com.jagex.game.runetek5.js5

import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.network.ConnectionInfo
import com.jagex.game.runetek5.network.WorldConnection
import com.jagex.game.runetek5.network.world.WorldDescriptor

object Js5Endpoint {

    @JvmField
    var active: ConnectionInfo? = null

    @JvmStatic
    fun lobby(): ConnectionInfo = WorldConnection.GAME_CONNECTION!!

    @JvmStatic
    fun current(): ConnectionInfo = active ?: lobby()

    @JvmStatic
    fun isLobby(endpoint: ConnectionInfo): Boolean = endpoint.equals(lobby())

    @JvmStatic
    fun forWorld(worldId: Int): ConnectionInfo {
        val world = WorldDescriptor.getWorld(worldId) ?: return lobby()
        if (!world.hasCustomCache()) return lobby()
        val host = world.ipAddress ?: LoginManager.GAME_CONNECTION_INFO?.host ?: return lobby()
        return ConnectionInfo().also {
            it.worldId = worldId
            it.host = host
            it.devWorld1 = worldId
            it.devWorld2 = worldId
        }
    }

    @JvmStatic
    fun describe(endpoint: ConnectionInfo): String =
        if (isLobby(endpoint)) "lobby (${endpoint.host})" else "world ${endpoint.worldId} (${endpoint.host})"
}
