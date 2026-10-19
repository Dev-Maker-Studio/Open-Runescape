package com.rs.game.content.minigames.barbarianassault

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.model.GroundItem
import world.gregs.voidps.type.Tile

const val NORMAL_CHUNK_X = 232
const val NORMAL_CHUNK_Y = 680
const val QUEEN_CHUNK_X = 232
const val QUEEN_CHUNK_Y = 672
const val ARENA_CHUNKS = 8

class ArenaLayout(
    val starts: List<Tile>,
    val spawns: Map<PenanceFamily, Tile>,
    val logs: List<Tile>,
    val hammer: Tile,
    val cannons: List<Cannon>,
    val traps: List<Tile>,
)

enum class Cannon(val displayName: String, val launcher: Tile, val hopper: Tile) {
    WEST("west", Tile.of(21, 34, 0), Tile.of(22, 34, 0)),
    EAST("east", Tile.of(40, 34, 0), Tile.of(41, 34, 0)),
}

val NORMAL_ARENA = ArenaLayout(
    starts = listOf(Tile.of(31, 18, 0), Tile.of(30, 17, 0), Tile.of(32, 17, 0), Tile.of(29, 16, 0), Tile.of(33, 16, 0)),
    spawns = mapOf(
        PenanceFamily.RUNNER to Tile.of(36, 47, 0),
        PenanceFamily.HEALER to Tile.of(42, 45, 0),
        PenanceFamily.FIGHTER to Tile.of(24, 46, 0),
        PenanceFamily.RANGER to Tile.of(18, 45, 0),
    ),
    logs = listOf(Tile.of(28, 47, 0), Tile.of(29, 46, 0)),
    hammer = Tile.of(28, 46, 0),
    cannons = listOf(Cannon.WEST, Cannon.EAST),
    traps = listOf(Tile.of(15, 33, 0), Tile.of(45, 34, 0)),
)

val QUEEN_ARENA = ArenaLayout(
    starts = listOf(Tile.of(30, 18, 0), Tile.of(29, 17, 0), Tile.of(31, 17, 0), Tile.of(32, 16, 0), Tile.of(28, 16, 0)),
    spawns = mapOf(
        PenanceFamily.RUNNER to Tile.of(42, 46, 0),
        PenanceFamily.HEALER to Tile.of(36, 47, 0),
        PenanceFamily.FIGHTER to Tile.of(24, 46, 0),
        PenanceFamily.RANGER to Tile.of(18, 45, 0),
    ),
    logs = listOf(Tile.of(29, 47, 0), Tile.of(30, 46, 0)),
    hammer = Tile.of(28, 46, 0),
    cannons = listOf(Cannon.WEST),
    traps = listOf(Tile.of(15, 33, 0), Tile.of(45, 34, 0)),
)

val QUEEN_EMERGE = Tile.of(29, 30, 0)
const val TRAP_RADIUS = 1
const val CANNON_RANGE = 15

val BAIT_OBJS = intArrayOf(Obj.barbassault_runner_food, Obj.barbassault_runner_food2, Obj.barbassault_runner_food3)
val POISON_OBJS = intArrayOf(Obj.barbassault_poision_01, Obj.barbassault_poision_02, Obj.barbassault_poision_03)
val VIALS = intArrayOf(Obj.obj_10546, Obj.barbassault_vial_04_1q, Obj.barbassault_vial_04_half, Obj.barbassault_vial_04_3q, Obj.barbassault_vial_04_full)
val EGG_OBJS = intArrayOf(Obj.barbassault_egg_01, Obj.barbassault_egg_02, Obj.barbassault_egg_03)
val QUEEN_EGG_OBJS = intArrayOf(Obj.barbassault_egg_04, Obj.barbassault_egg_poison, Obj.obj_10536, Obj.barbassault_egg_heated_poison_spikes)
val COLLECTION_BAGS = intArrayOf(Obj.barbassault_coll_bag_01, Obj.barbassault_coll_bag_02, Obj.barbassault_coll_bag_03, Obj.barbassault_coll_bag_04, Obj.barbassault_coll_bag_05)

val ARENA_ONLY_OBJS: Set<Int> = buildSet {
    addAll(Role.allHorns)
    addAll(BAIT_OBJS.toList())
    addAll(POISON_OBJS.toList())
    addAll(VIALS.toList())
    addAll(EGG_OBJS.toList())
    addAll(QUEEN_EGG_OBJS.toList())
    addAll(COLLECTION_BAGS.toList())
    add(Obj.barbassault_spikes)
}

val TRACKED_SUPPLIES = intArrayOf(
    Obj.logs, Obj.hammer, Obj.bronze_arrow, Obj.iron_arrow, Obj.steel_arrow, Obj.mithril_arrow,
    Obj.airrune, Obj.waterrune, Obj.earthrune, Obj.firerune, Obj.mindrune,
)

class Bait(val obj: Int, val tile: Tile, val good: Boolean, val ordinal: Int) {
    lateinit var ground: GroundItem
}
