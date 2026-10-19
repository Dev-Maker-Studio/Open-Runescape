package com.rs.game.content.bosses.godwars.factions.zaros

import com.rs.game.World
import com.rs.game.model.entity.Entity
import com.rs.game.model.entity.player.Player
import com.rs.game.tasks.WorldTasks
import com.rs.utils.Ticks
import java.util.Collections
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

/**
 * The war roster for the public Nex arena. Nex herself owns her minions, so this only tracks who is
 * fighting and starts or ends the encounter.
 */
class NexArena {
    private val players: MutableList<Player> = Collections.synchronizedList(ArrayList())
    var nex: Nex? = null
        private set

    val playersCount: Int get() = players.size

    fun getPlayers(): List<Player> = players

    fun addPlayer(player: Player) {
        if (players.contains(player)) return
        players.add(player)
        startWar()
    }

    fun removePlayer(player: Player) {
        players.remove(player)
        if (playersCount == 0) deleteNPCS()
    }

    fun deleteNPCS() {
        nex?.let {
            it.killBloodReavers()
            it.finish()
        }
        nex = null
    }

    val possibleTargets: MutableList<Entity>
        get() = players.filterNotTo(ArrayList()) { it.isDead || it.hasFinished() || !it.isRunning }

    fun moveNextStage() {
        nex?.nextPhase()
    }

    fun endWar() {
        deleteNPCS()
        WorldTasks.schedule(Ticks.fromMinutes(1)) { startWar() }
    }

    private fun startWar() {
        if (playersCount < 1 || nex != null) return
        val summoned = World.spawnNPC(Npc.godwars2_zaros_avatar, HOME, true, true) as? Nex ?: return
        summoned.arena = this
        nex = summoned
    }

    companion object {
        @JvmStatic
        val globalInstance = NexArena()

        private val HOME = Tile.of(2924, 5202, 0)
    }
}
