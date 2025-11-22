package com.jagex.game.runetek5.network

import com.jagex.game.runetek5.client.GameState
import com.jagex.game.runetek5.client.client
import com.jagex.game.runetek5.network.world.WorldDescriptor
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages world connections and world list.
 */
object WorldConnection {

    @JvmField var GAME_CONNECTION: ConnectionInfo? = null
    @JvmField var CONNECTION_INFO: ConnectionInfo? = null
    @JvmField var LOBBY_CONNECTION_INFO: ConnectionInfo? = null
    @JvmField var worldListReceivedTime: Long = 0L
    @JvmField var reconnectingWorld: Boolean = false
    @JvmField var connectionInfo: ConnectionInfo? = null
    @JvmField var requestingWorldList: Boolean = false
    @JvmField var PING_WORLDS: Boolean = false
    @JvmField var WORLD_DESCRIPTORS_BYID: Array<WorldDescriptor?> = emptyArray()
    @JvmField var CURRENT_WORLD: Int = 1000336
    @JvmField var gameConnection: Connection? = null
    @JvmField var WORLD_PING_REQUESTS: MutableMap<Int, PingRequest> = ConcurrentHashMap()

    /**
     * Sets the lobby connection host and port.
     *
     * @param host The lobby server host
     * @param port The lobby server port
     */
    @JvmStatic
    fun setLobbyConnection(host: String, port: Int) {
        if (LOBBY_CONNECTION_INFO == null) {
            LOBBY_CONNECTION_INFO = ConnectionInfo()
        }
        LOBBY_CONNECTION_INFO!!.host = host
        LOBBY_CONNECTION_INFO!!.worldId = port + 1099
        LOBBY_CONNECTION_INFO!!.devWorld1 = LOBBY_CONNECTION_INFO!!.worldId
        LOBBY_CONNECTION_INFO!!.devWorld2 = LOBBY_CONNECTION_INFO!!.worldId
    }

    /**
     * Drops the current client connection.
     */
    @JvmStatic
    fun dropClient() {
        if (!GameState.inLobby(client.gameState) && !GameState.isLoggedOut(client.gameState)) {
            gameConnection = client.GAME_CONNECTION_CONTEXT.getConnection()
            client.GAME_CONNECTION_CONTEXT.reset()
            client.setGameState(GameState.UNK_10)
        } else {
            client.logout(false)
        }
    }

    /**
     * Sorts world descriptors using quicksort.
     *
     * @param srcPos The starting position
     * @param size The ending position
     * @param sortMode The sort mode
     * @param ascending Whether to sort ascending
     * @param filterMode The filter mode
     * @param filterEnabled Whether filtering is enabled
     */
    @JvmStatic
    fun quicksort(srcPos: Int, size: Int, sortMode: Int, ascending: Boolean, filterMode: Int, filterEnabled: Boolean) {
        if (srcPos < size) {
            val center = (srcPos + size) / 2
            var currentPosition = srcPos
            val pivot = WORLD_DESCRIPTORS_BYID[center]!!
            WORLD_DESCRIPTORS_BYID[center] = WORLD_DESCRIPTORS_BYID[size]
            WORLD_DESCRIPTORS_BYID[size] = pivot

            for (i in srcPos until size) {
                if (WorldDescriptor.method4325(WORLD_DESCRIPTORS_BYID[i]!!, pivot, sortMode, ascending, filterMode, filterEnabled) <= 0) {
                    val temp = WORLD_DESCRIPTORS_BYID[i]
                    WORLD_DESCRIPTORS_BYID[i] = WORLD_DESCRIPTORS_BYID[currentPosition]
                    WORLD_DESCRIPTORS_BYID[currentPosition++] = temp
                }
            }

            WORLD_DESCRIPTORS_BYID[size] = WORLD_DESCRIPTORS_BYID[currentPosition]
            WORLD_DESCRIPTORS_BYID[currentPosition] = pivot
            quicksort(srcPos, currentPosition - 1, sortMode, ascending, filterMode, filterEnabled)
            quicksort(currentPosition + 1, size, sortMode, ascending, filterMode, filterEnabled)
        }
    }

    /**
     * Sorts and clears ping requests.
     *
     * @param sortMode The sort mode
     * @param ascending Whether to sort ascending
     * @param filterMode The filter mode
     * @param filterEnabled Whether filtering is enabled
     */
    @JvmStatic
    fun method6299(sortMode: Int, ascending: Boolean, filterMode: Int, filterEnabled: Boolean) {
        quicksort(0, WORLD_DESCRIPTORS_BYID.size - 1, sortMode, ascending, filterMode, filterEnabled)
        WORLD_PING_REQUESTS.clear()
    }

    /**
     * Gets the next world descriptor.
     *
     * @return The next world descriptor, or null if at the end
     */
    @JvmStatic
    fun getNextWorldDescriptor(): WorldDescriptor? {
        return if (CURRENT_WORLD < WORLD_DESCRIPTORS_BYID.size) WORLD_DESCRIPTORS_BYID[CURRENT_WORLD++] else null
    }
}
