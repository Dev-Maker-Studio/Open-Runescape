package com.rs.game.content.minigames.creations

import com.rs.game.content.Effect
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Spotanim
import org.darkan.core.model.SpotAnim
import world.gregs.voidps.type.Tile

enum class BarrierTier(val item: Int, private val blueWalls: IntArray, private val redWalls: IntArray, private val blueGates: IntArray, private val redGates: IntArray) {
    CLASS_1(Obj.sc_plank_1, intArrayOf(Loc.loc_39770, Loc.loc_39772, Loc.loc_39774, Loc.loc_39776), intArrayOf(Loc.loc_39771, Loc.loc_39773, Loc.loc_39775, Loc.loc_39777), intArrayOf(Loc.loc_39778, Loc.loc_39780), intArrayOf(Loc.loc_39779, Loc.loc_39781)),
    CLASS_2(Obj.sc_plank_2, intArrayOf(Loc.loc_39782, Loc.loc_39784, Loc.loc_39786, Loc.loc_39788), intArrayOf(Loc.loc_39783, Loc.loc_39785, Loc.loc_39787, Loc.loc_39789), intArrayOf(Loc.loc_39790, Loc.loc_39792), intArrayOf(Loc.loc_39791, Loc.loc_39793)),
    CLASS_3(Obj.sc_plank_3, intArrayOf(Loc.loc_39794, Loc.loc_39796, Loc.loc_39798, Loc.loc_39800), intArrayOf(Loc.loc_39795, Loc.loc_39797, Loc.loc_39799, Loc.loc_39801), intArrayOf(Loc.loc_39802, Loc.loc_39804), intArrayOf(Loc.loc_39803, Loc.loc_39805)),
    CLASS_4(Obj.sc_plank_4, intArrayOf(Loc.loc_39806, Loc.loc_39808, Loc.loc_39810, Loc.loc_39812), intArrayOf(Loc.loc_39807, Loc.loc_39809, Loc.loc_39811, Loc.loc_39813), intArrayOf(Loc.loc_39814, Loc.loc_39816), intArrayOf(Loc.loc_39815, Loc.loc_39817)),
    CLASS_5(Obj.sc_plank_5, intArrayOf(Loc.loc_39818, Loc.loc_39820, Loc.loc_39822, Loc.loc_39824), intArrayOf(Loc.loc_39819, Loc.loc_39821, Loc.loc_39823, Loc.loc_39825), intArrayOf(Loc.loc_39826, Loc.loc_39828), intArrayOf(Loc.loc_39827, Loc.loc_39829));

    val tier get() = ordinal + 1
    val maxHealth get() = tier * 4

    fun wall(red: Boolean, damage: Int) = (if (red) redWalls else blueWalls)[damage]

    fun gates(red: Boolean) = if (red) redGates else blueGates

    companion object {
        const val PLOT_INSET = 1
        const val PLOT_OUTSET = 6
        const val BUILD_COST = 4
        val EMPTY_HOTSPOTS = setOf(Loc.sc_barrier_hotspot, Loc.sc_barrier_gate_l_hotspot, Loc.sc_barrier_gate_r_hotspot)
        private val GATES = listOf(false, true).associateWith { red -> entries.flatMap { it.gates(red).asIterable() }.toSet() }
        private val WALLS = listOf(false, true).associateWith { red -> entries.flatMap { (if (red) it.redWalls else it.blueWalls).asIterable() }.toSet() }

        fun forTier(tier: Int) = entries.getOrNull(tier - 1)

        fun gateIds(red: Boolean) = GATES.getValue(red)

        fun isGate(id: Int, red: Boolean) = id in GATES.getValue(red)

        fun isBarrier(id: Int, red: Boolean) = isGate(id, red) || id in WALLS.getValue(red)

        fun damageLevel(health: Int, tier: Int) = (4.0 - 4.0 * (health / (tier * 4.0))).toInt()
    }
}

fun gateInside(gate: GameObject): Tile? = when (gate.rotation) {
    0 -> Tile.of(gate.x - 1, gate.y, gate.plane)
    1 -> Tile.of(gate.x, gate.y + 1, gate.plane)
    2 -> Tile.of(gate.x + 1, gate.y, gate.plane)
    3 -> Tile.of(gate.x, gate.y - 1, gate.plane)
    else -> null
}

private fun Player.isAt(x: Int, y: Int) = this.x == x && this.y == y

private fun Player.isAtGate(gate: GameObject) = isAt(gate.x, gate.y) || gateInside(gate)?.let { isAt(it.x, it.y) } == true

private fun Player.walkToGate(gate: GameObject): Boolean {
    if (isAtGate(gate)) return true
    val approach = gateInside(gate) ?: return false
    return addWalkSteps(gate.x, gate.y) || addWalkSteps(approach.x, approach.y)
}

private fun Player.gateExit(gate: GameObject): Tile? = if (!isAt(gate.x, gate.y)) Tile.of(gate.x, gate.y, gate.plane) else gateInside(gate)

fun Player.passBarrier(gate: GameObject, red: Boolean) {
    if (hasEffect(Effect.FREEZE)) {
        sendMessage("A mysterious force prevents you from moving.")
        return
    }
    lock(3)
    if (!walkToGate(gate)) {
        unlock()
        return
    }
    schedule {
        wait { isAtGate(gate) || (!hasWalkSteps() && nextWalkDirection == null) }
        val exit = if (isAtGate(gate)) gateExit(gate) else null
        if (exit == null) {
            unlock()
            return@schedule
        }
        sendMessage("You pass through the barrier.")
        tele(exit)
        forceMove(exit, Seq.sc_walking_through_forcefield, 5, 30)
        setNextSpotAnim(SpotAnim(if (red) Spotanim.sc_walking_through_forcefield_spotanim_2 else Spotanim.sc_walking_through_forcefield_spotanim))
        wait(1)
        unlock()
    }
}
