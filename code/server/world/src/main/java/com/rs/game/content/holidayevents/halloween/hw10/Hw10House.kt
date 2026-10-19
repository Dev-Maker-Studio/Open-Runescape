package com.rs.game.content.holidayevents.halloween.hw10

import com.rs.game.model.entity.npc.NPC
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.instantiateNpc
import com.rs.plugin.kts.onNpcClick
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.spawns.NPCSpawn
import com.rs.utils.spawns.NPCSpawns
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj
import world.gregs.voidps.type.Tile

const val PREP_HOUSE_REGION = 17237
const val PARTY_HOUSE_REGION = 17235
private const val PARTY_HOUSE_OFFSET_Y = -128

val PREP_ARRIVAL: Tile = Tile.of(4319, 5455, 0)
val PARTY_ARRIVAL: Tile = PREP_ARRIVAL.inPartyHouse()
val PARTY_DANCE_FLOOR: Tile = Tile.of(4319, 5331, 0)
private val PREP_EXIT_PORTAL = Tile.of(4319, 5453, 0)

val HW10_EVENT_ITEMS = intArrayOf(
    Obj.obj_19833, Obj.obj_19834, Obj.obj_19835, Obj.obj_19836, Obj.obj_19837, Obj.obj_19838,
    Obj.obj_19839, Obj.obj_19840, Obj.obj_19841, Obj.obj_19842, Obj.obj_19845
)

private class HouseSpawn(val npc: Int, val tile: Tile, val name: String)

private val PREP_HOUSE_SPAWNS = listOf(
    HouseSpawn(Npc.hw10_pumpkin_pete_2, Tile.of(4321, 5456, 0), "Pumpkin Pete"),
    HouseSpawn(Npc.hw10_djweb, Tile.of(4316, 5469, 0), "Minstrel Web"),
    HouseSpawn(Npc.hw10_alan, Tile.of(4315, 5470, 0), "Allan"),
    HouseSpawn(Npc.hw10_grim, Tile.of(4331, 5470, 1), "Grim Reaper"),
    HouseSpawn(Npc.hw10_black_zabeth, Tile.of(4328, 5462, 0), "Zabeth Corvid"),
    HouseSpawn(Npc.hw10_edgar, Tile.of(4320, 5456, 1), "Edgar"),
    HouseSpawn(Npc.hw09_skeletal_hellhound, Tile.of(4330, 5460, 0), "Muncher"),
    HouseSpawn(Npc.npc_12382, Tile.of(4311, 5470, 0), "Greta, singer until the moonshine is delivered"),
    HouseSpawn(Npc.npc_12386, Tile.of(4313, 5470, 0), "Helga, singer until the moonshine is delivered"),
    HouseSpawn(Npc.npc_12390, Tile.of(4314, 5470, 0), "Moira, singer until the moonshine is delivered"),
    HouseSpawn(Npc.npc_12383, Tile.of(4320, 5470, 0), "Greta, on stage once the moonshine is delivered"),
    HouseSpawn(Npc.npc_12387, Tile.of(4319, 5470, 0), "Helga, on stage once the moonshine is delivered"),
    HouseSpawn(Npc.npc_12391, Tile.of(4321, 5470, 0), "Moira, on stage once the moonshine is delivered"),
    HouseSpawn(Npc.npc_12419, Tile.of(4333, 5468, 0), "Orange spider, loose on the ground floor"),
    HouseSpawn(Npc.npc_12420, Tile.of(4319, 5456, 1), "Turquoise spider, listening to Edgar"),
    HouseSpawn(Npc.npc_12421, Tile.of(4320, 5466, 1), "Wandering pink spider"),
    HouseSpawn(Npc.npc_12422, Tile.of(4322, 5474, 1), "Wandering beige spider"),
) + Hw10Spider.entries.flatMap {
    listOf(
        HouseSpawn(it.offTileParent, it.offTile, "${it.colour} spider, on the dance floor"),
        HouseSpawn(it.onTileParent, it.litTile, "${it.colour} spider, on its lit tile"),
    )
}

private val PARTY_HOUSE_SPAWNS = listOf(
    HouseSpawn(Npc.hw10_pumpkin_pete_2, Tile.of(4316, 5335, 0), "Pumpkin Pete"),
    HouseSpawn(Npc.hw10_djweb, Tile.of(4316, 5341, 0), "Minstrel Web"),
    HouseSpawn(Npc.hw10_alan, Tile.of(4315, 5342, 0), "Allan"),
    HouseSpawn(Npc.hw10_grim, Tile.of(4331, 5342, 1), "Grim Reaper"),
    HouseSpawn(Npc.hw10_edgar, Tile.of(4320, 5328, 1), "Edgar"),
    HouseSpawn(Npc.hw09_skeletal_hellhound, Tile.of(4330, 5332, 0), "Muncher"),
    HouseSpawn(Npc.hw10_black_zabeth_band, Tile.of(4320, 5341, 0), "Zabeth Corvid"),
    HouseSpawn(Npc.hw10_greta_band, Tile.of(4320, 5342, 0), "Greta"),
    HouseSpawn(Npc.hw10_helga_band, Tile.of(4319, 5342, 0), "Helga"),
    HouseSpawn(Npc.hw10_moira_band, Tile.of(4321, 5342, 0), "Moira"),
    HouseSpawn(Npc.hw10_dancer_headbang, Tile.of(4316, 5338, 0), "Dancer"),
    HouseSpawn(Npc.hw10_dancer_headbang2, Tile.of(4322, 5338, 0), "Dancer"),
    HouseSpawn(Npc.hw10_dancer_dance, Tile.of(4316, 5334, 0), "Dancer"),
    HouseSpawn(Npc.hw10_dancer_dance2, Tile.of(4322, 5334, 0), "Dancer"),
    HouseSpawn(Npc.hw10_dancer_dance_spin, Tile.of(4321, 5337, 0), "Dancer"),
    HouseSpawn(Npc.hw10_party_pete, Tile.of(4321, 5335, 0), "Party Pete"),
) + Hw10Spider.entries.map { HouseSpawn(it.npc, it.litTile.inPartyHouse(), "${it.colour} spider, lighting its dance floor tile") }

fun Tile.inPartyHouse(): Tile = transform(0, PARTY_HOUSE_OFFSET_Y)

val Player.inHw10House get() = regionId == PREP_HOUSE_REGION || regionId == PARTY_HOUSE_REGION

val Player.inPartyHouse get() = regionId == PARTY_HOUSE_REGION

val Player.hw10Arrival: Tile get() = if (hw10Stage >= Hw10Stage.PARTY) PARTY_ARRIVAL else PREP_ARRIVAL

fun Player.enterHw10House() {
    controllerManager.startController(Halloween2010Controller())
}

fun Player.leaveHw10House() {
    vanishHw10Items()
    controllerManager.forceStop()
    useStairs(OUTSIDE_ARRIVAL)
}

fun Player.vanishHw10Items() {
    val carried = HW10_EVENT_ITEMS.filter { inventory.containsItem(it) }
    if (carried.isEmpty())
        return
    carried.forEach { inventory.deleteItem(it, Int.MAX_VALUE) }
    sendMessage("The Halloween items vanish. You feel a little strange.")
}

private val KEEPS_OWN_FACING = arrayOf<Any>(
    Npc.hw10_grim, Npc.hw10_djweb, Npc.hw10_black_zabeth, Npc.hw10_black_zabeth_band,
    Npc.npc_12382, Npc.npc_12386, Npc.npc_12390,
)

@ServerStartupEvent(ServerStartupEvent.Priority.FILE_IO)
fun loadHalloween2010HouseSpawns() {
    for (spawn in PREP_HOUSE_SPAWNS + PARTY_HOUSE_SPAWNS)
        NPCSpawns.add(NPCSpawn(spawn.npc, spawn.tile, "Halloween 2010: ${spawn.name}"))
}

@ServerStartupEvent
fun mapHalloween2010House() {
    onObjectClick(Loc.hw09_grim_portal, tiles = arrayOf(PREP_EXIT_PORTAL, PREP_EXIT_PORTAL.inPartyHouse())) { (player) ->
        player.leaveHw10House()
    }

    instantiateNpc(*KEEPS_OWN_FACING) { id, tile -> NPC(id, tile).apply { setKeepsOwnFacing(true) } }

    instantiateNpc(*Hw10Spider.entries.map { it.npc }.toTypedArray<Any>()) { id, tile -> NPC(id, tile).apply { setRandomWalk(false) } }

    onNpcClick(Npc.hw09_skeletal_hellhound, options = arrayOf("Interact")) { (player) ->
        if (player.inHw10House)
            player.sendMessage("Muncher doesn't seem particularly pleased to see you...or anyone.")
    }
}
