package com.rs.game.content.bosses.instance

import com.rs.game.model.entity.player.Player
import com.rs.utils.Ticks

enum class SpawnSpeed(val label: String, val seconds: Int) {
    STANDARD("Standard", 72),
    FAST("Fast", 50),
    FASTEST("Fastest", 27),
    RAPID("Rapid", 10);

    val ticks: Int get() = Ticks.fromSeconds(seconds)

    companion object {
        const val MIN_SECONDS = 10
        val slowestFirst: List<SpawnSpeed> = entries.sortedByDescending { it.seconds }
    }
}

enum class InstanceProtection(val label: String) {
    FFA("FFA"),
    PIN("PIN"),
    FRIENDS("Friends"),
    FRIENDS_CHAT("Friends chat"),
    CLAN("Clan");

    fun admits(owner: Player, joiner: Player, pin: Int, enteredPin: Int): Boolean = when (this) {
        FFA -> true
        PIN -> pin == enteredPin
        FRIENDS -> owner.account.social.friends.contains(joiner.username)
        FRIENDS_CHAT -> owner.account.social.currentFriendsChat != null && owner.account.social.currentFriendsChat == joiner.account.social.currentFriendsChat
        CLAN -> owner.account.social.clanName != null && owner.account.social.clanName == joiner.account.social.clanName
    }
}

data class InstanceSettings(
    val maxPlayers: Int,
    val minCombat: Int,
    val spawnSpeed: SpawnSpeed,
    val protection: InstanceProtection,
    val pin: Int = 0,
)
