package org.darkan.world.model

import com.rs.game.model.entity.player.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry of online players by username.
 * Provides O(1) username lookup for social/gateway features.
 */
object WorldPlayers {
    private val playersByUsername = ConcurrentHashMap<String, Player>()

    fun get(username: String): Player? = playersByUsername[username]

    fun put(player: Player) {
        playersByUsername[player.account.username] = player
    }

    fun remove(username: String): Player? = playersByUsername.remove(username)

    fun values(): Collection<Player> = playersByUsername.values
}
