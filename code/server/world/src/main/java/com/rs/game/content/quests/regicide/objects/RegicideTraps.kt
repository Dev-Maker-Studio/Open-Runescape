package com.rs.game.content.quests.regicide.objects

import com.rs.game.World
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.Hit.HitLook
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import world.gregs.voidps.type.Tile
import kotlin.math.roundToInt

private const val PIT_RETURN = "regicideTrapReturn"

private data class LeafPit(val centre: Tile, val room: Tile, val vertical: Boolean = true) {
    val leaves = (-1..1).flatMap { x -> (-1..1).map { y -> centre.transform(x, y, 0) } }
    val rocks = (0..1).flatMap { x -> (0..1).map { y -> room.transform(x, y, 0) } }
    fun end(positive: Boolean): Tile = centre.transform(
        if (vertical) 0 else if (positive) 2 else -2,
        if (!vertical) 0 else if (positive) 2 else -2, 0
    )
    fun positive(tile: Tile) = if (vertical) tile.y > centre.y else tile.x > centre.x
    fun nearEdge(tile: Tile, positive: Boolean) =
        if (vertical) tile.y == centre.y + if (positive) 1 else -1
        else tile.x == centre.x + if (positive) 1 else -1
}

// Surface leaves and four two-by-two pit rooms from cache maps 34_50, 35_49,
// 35_50 and 36_150. The saved return tile, not the rock clicked, determines the exit.
private val LEAF_PITS = listOf(
    LeafPit(Tile.of(2209, 3203, 0), Tile.of(2313, 9656, 0)),
    LeafPit(Tile.of(2267, 3203, 0), Tile.of(2336, 9656, 0)),
    LeafPit(Tile.of(2274, 3174, 0), Tile.of(2354, 9643, 0)),
    LeafPit(Tile.of(2277, 3262, 0), Tile.of(2354, 9656, 0), false)
)

private data class ForestTrap(val tile: Tile, val id: Int, val rotation: Int) {
    private val vertical = (rotation % 2 == 1) == (id == Loc.regicide_trap_woodspring)
    // Tiles before the dividing wall loc the map places inside the trap; rotating by 180 degrees mirrors it.
    private val wallOffset = if (id == Loc.regicide_trap_woodspring && (rotation == 0 || rotation == 3)) 2 else 1
    private fun along(offset: Int): Tile = tile.transform(if (vertical) 0 else offset, if (vertical) offset else 0, 0)
    private fun beforeWall(player: Player) = (if (vertical) player.y - tile.y else player.x - tile.x) < wallOffset
    fun start(player: Player): Tile = along(if (beforeWall(player)) wallOffset - 1 else wallOffset)
    fun destination(player: Player): Tile = along(if (beforeWall(player)) wallOffset else wallOffset - 1)
    fun knockback(player: Player): Tile = along(if (beforeWall(player)) wallOffset - 2 else wallOffset + 1)
}

private val FOREST_TRAPS = listOf(
    ForestTrap(Tile.of(2181, 3209, 0), Loc.regicide_trap_woodspring, 3),
    ForestTrap(Tile.of(2200, 3169, 0), Loc.regicide_trap_woodspring, 2),
    ForestTrap(Tile.of(2235, 3181, 0), Loc.regicide_trap_woodspring, 0),
    ForestTrap(Tile.of(2257, 3227, 0), Loc.regicide_trap_woodspring, 2),
    ForestTrap(Tile.of(2275, 3163, 0), Loc.regicide_trap_woodspring, 2),
    ForestTrap(Tile.of(2295, 3214, 0), Loc.regicide_trap_woodspring, 1),
    ForestTrap(Tile.of(2215, 3154, 0), Loc.regicide_trap_tripwire, 2),
    ForestTrap(Tile.of(2220, 3153, 0), Loc.regicide_trap_tripwire, 0),
    ForestTrap(Tile.of(2251, 3168, 0), Loc.regicide_trap_tripwire, 3),
    ForestTrap(Tile.of(2285, 3188, 0), Loc.regicide_trap_tripwire, 3),
    ForestTrap(Tile.of(2294, 3243, 0), Loc.regicide_trap_tripwire, 0)
)

@ServerStartupEvent
fun mapRegicideTraps() {
    onObjectClick(Loc.regicide_trap_woodspring, Loc.regicide_trap_tripwire, checkDistance = false) { (player, obj) ->
        val trap = FOREST_TRAPS.firstOrNull { it.tile == obj.tile && it.id == obj.id } ?: return@onObjectClick
        if (player.isLocked || player.plane != obj.plane)
            return@onObjectClick
        val start = trap.start(player)
        val destination = trap.destination(player)
        player.approachRegicideTrap(start) {
            if (!player.isLocked && player.tile == start) {
                val success = player.passForestTrap(false)
                if (trap.id == Loc.regicide_trap_tripwire) {
                    player.forceMove(destination, Seq.regicide_stepover, 0, 60) {
                        if (!success && !player.isDead)
                            player.triggerTripwire()
                    }
                } else if (success)
                    player.forceMove(destination, Seq.regicide_stepover, 0, 60)
                else
                    player.failStickTrap(trap.knockback(player))
            }
        }
    }
    onObjectClick(Loc.loc_3923, Loc.regicide_pitfall_mid, Loc.loc_3925, checkDistance = false) { (player, obj) ->
        val pit = LEAF_PITS.firstOrNull { obj.tile in it.leaves } ?: return@onObjectClick
        if (player.isLocked || player.plane != 0)
            return@onObjectClick
        val positive = pit.positive(player.tile)
        val start = pit.end(positive)
        player.approachRegicideTrap(start) {
            if (!player.isLocked && player.tile == start) {
                val destination = pit.end(!positive)
                if (!player.clearTrapExit(destination))
                    return@approachRegicideTrap
                if (pit.nearEdge(obj.tile, positive) && player.passForestTrap(true))
                    player.forceMove(destination, Seq.luc2_player_dive, 0, 25)
                else
                    player.fallIntoLeafPit(pit, start)
            }
        }
    }
    LEAF_PITS.forEach { pit ->
        onPlayerStep(pit.leaves.toTypedArray()) { (player) ->
            if (!player.isLocked) {
                val start = pit.end(pit.positive(player.tile))
                player.resetWalkSteps()
                player.lock()
                player.schedule("regicideLeafTrap") {
                    wait(1)
                    if (!player.isDead)
                        player.fallIntoLeafPit(pit, start)
                    else
                        player.unlock()
                }
            }
        }
        onObjectClick(Loc.regicide_trap_hand_holds, tiles = pit.rocks.toTypedArray(), checkDistance = false) { (player, obj) ->
            if (player.isLocked || player.tile !in pit.rocks)
                return@onObjectClick
            val saved = player.getI(PIT_RETURN)
            val destination = listOf(pit.end(false), pit.end(true)).firstOrNull { it.id == saved } ?: pit.end(false)
            if (player.clearTrapExit(destination)) {
                player.lock()
                player.faceObject(obj)
                player.anim(Seq.human_climbing)
                player.schedule("regicideClimbPit") {
                    wait(2)
                    if (!player.isDead) {
                        player.tele(destination)
                        player.savingAttributes.remove(PIT_RETURN)
                    }
                    player.unlock()
                }
            }
        }
    }
}

private fun Player.approachRegicideTrap(start: Tile, action: () -> Unit) {
    if (tile == start) {
        action()
        return
    }
    if (plane != start.plane)
        return
    if (getLongestDelta(start) <= 1) {
        forceMove(start, Seq.human_walk_f, 0, 30) {
            if (!isDead && tile == start)
                action()
        }
        return
    }
    walkToAndExecute(start) { action() }
}

private fun Player.clearTrapExit(destination: Tile): Boolean {
    if (World.floorAndWallsFree(destination, 1))
        return true
    sendMessage("The other side is blocked.")
    return false
}

private fun Player.passForestTrap(leaves: Boolean): Boolean {
    if (auraManager.isSurefootedAura())
        return true
    val level = skills.getLevel(Constants.AGILITY).coerceIn(1, 99)
    val low = if (leaves) 181 else 31
    val high = if (leaves) 256 else 156
    // Wiki tables use nearest-integer interpolation in a 256-outcome roll.
    val threshold = (low + (high - low) * (level - 1) / 98.0).roundToInt()
    return Utils.random(256) < threshold
}

private fun Player.failStickTrap(knockback: Tile) {
    sendMessage("The sticks spring back and knock you backwards!")
    applyHit(Hit(80, HitLook.TRUE_DAMAGE))
    forceMove(knockback, Seq.human_rockfall, 0, 30)
}

private fun Player.triggerTripwire() {
    sendMessage("You trigger the tripwire and are hit by poisoned arrows!")
    applyHit(Hit(100, HitLook.TRUE_DAMAGE))
    poison.makePoisoned(20)
}

private fun Player.fallIntoLeafPit(pit: LeafPit, start: Tile) {
    set(PIT_RETURN, start.id)
    resetWalkSteps()
    lock()
    anim(Seq.human_falling)
    sendMessage("You fall through the leaves into a pit of spikes!")
    schedule("regicideFallPit") {
        wait(2)
        if (!isDead) {
            tele(pit.room)
            applyHit(Hit((Utils.random(18) + 1) * 10, HitLook.TRUE_DAMAGE))
        }
        unlock()
    }
}
