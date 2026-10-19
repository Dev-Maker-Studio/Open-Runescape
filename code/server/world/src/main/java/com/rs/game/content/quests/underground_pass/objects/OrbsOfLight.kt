package com.rs.game.content.quests.underground_pass.objects

import org.darkan.core.gameval.gen.Bas
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.model.ObjectShape
import org.darkan.core.map.pathfinder.Direction
import com.rs.engine.quest.Quest
import org.darkan.core.gameval.gen.Varbit
import com.rs.game.World
import com.rs.game.content.quests.underground_pass.*
import com.rs.game.content.skills.agility.Agility
import com.rs.game.map.ChunkManager
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import org.darkan.core.Constants
import org.darkan.core.model.GroundItem
import org.darkan.core.model.GroundItemType
import org.darkan.core.model.Item
import world.gregs.voidps.type.Tile
import org.darkan.core.Utils
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onItemOnObject
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPickupItem
import com.rs.plugin.kts.onPlayerStep
import kotlin.math.ceil

private const val QUEST_ORB_METADATA = "UndergroundPassQuestOrb"
private const val PLACED_PLANK_ATTRIBUTE = "UndergroundPassPlacedPlank:"
private val ORBS = intArrayOf(Obj.caveorb1, Obj.caveorb2, Obj.caveorb3, Obj.caveorb4)

private data class OrbSpawn(val itemId: Int, val tile: Tile)

private data class FlatRockTrap(val tile: Tile, val horizontalTraversal: Boolean) {
    val rotation: Int
        get() = if (horizontalTraversal) 1 else 0

    val stalagmiteTiles: Set<Tile>
        get() = setOf(
            tile.transform(if (horizontalTraversal) 0 else -1, if (horizontalTraversal) -1 else 0),
            tile.transform(if (horizontalTraversal) 0 else 1, if (horizontalTraversal) 1 else 0)
        )

    val hazardTiles: Set<Tile>
        get() = stalagmiteTiles + tile
}

private val VISIBLE_ORB_SPAWNS = listOf(
    OrbSpawn(Obj.caveorb4, Tile.of(2416, 9696, 0)),
    OrbSpawn(Obj.caveorb3, Tile.of(2389, 9685, 0)),
    OrbSpawn(Obj.caveorb2, Tile.of(2389, 9676, 0)),
    OrbSpawn(Obj.caveorb1, Tile.of(2382, 9668, 0))
)
private val TRAPPED_ORB_TILE = Tile.of(2382, 9668, 0)
private val VISIBLE_ORB_TILES = VISIBLE_ORB_SPAWNS.map { it.tile }.toTypedArray()
private const val ORB_RENDER_DISTANCE = 48

private val FLAT_ROCK_TRAPS = listOf(
    FlatRockTrap(Tile.of(2393, 9676, 0), horizontalTraversal = true),
    FlatRockTrap(Tile.of(2396, 9677, 0), horizontalTraversal = true),
    FlatRockTrap(Tile.of(2401, 9675, 0), horizontalTraversal = true),
    FlatRockTrap(Tile.of(2404, 9675, 0), horizontalTraversal = true),
    FlatRockTrap(Tile.of(2406, 9719, 0), horizontalTraversal = true),
    FlatRockTrap(Tile.of(2406, 9725, 0), horizontalTraversal = true),
    FlatRockTrap(Tile.of(2408, 9674, 0), horizontalTraversal = true),
    FlatRockTrap(Tile.of(2416, 9689, 0), horizontalTraversal = false),
    FlatRockTrap(Tile.of(2418, 9681, 0), horizontalTraversal = false),
    FlatRockTrap(Tile.of(2418, 9685, 0), horizontalTraversal = false)
)
private val FLAT_ROCK_HAZARDS = FLAT_ROCK_TRAPS
    .flatMap { trap -> trap.hazardTiles.map { it to trap } }
    .toMap()

@ServerStartupEvent
fun mapOrbsOfLight() {
    onLogin { (player) ->
        removePlacedPlanks(player)
        VISIBLE_ORB_SPAWNS.forEach { removeVisibleOrb(player, it) }
    }

    onChunkEnter { (entity, _) ->
        val player = entity as? Player ?: return@onChunkEnter
        syncVisibleOrbs(player)
    }

    onPickupItem(*ORBS.map { it as Any }.toTypedArray(), tiles = VISIBLE_ORB_TILES) { event ->
        if (event.item.getMetaData(QUEST_ORB_METADATA) != true)
            return@onPickupItem
        if (
            event.player.getQuestStage(Quest.UNDERGROUND_PASS) != STAGE_CROSSED_BRIDGE ||
            event.player.vars.getVarBit(orbVarbit(event.item.id)) == 1
        ) {
            event.cancelPickup()
            World.removeGroundItem(event.item)
            return@onPickupItem
        }
        if (event.player.hasOrb(event.item.id)) {
            event.cancelPickup()
            World.removeGroundItem(event.item)
            event.player.sendMessage("You are already carrying this orb.")
            return@onPickupItem
        }
        if (event.item.tile == TRAPPED_ORB_TILE) {
            event.cancelPickup()
            event.player.sendMessage("As you reach for the orb, you trigger the trap!")
            event.player.triggerFlatRockTrap()
        }
    }

    onObjectClick(Loc.upass_double_springtrap_trigger, checkDistance = false) { (player, obj) ->
        if (player.getLongestDelta(obj.tile) > 2) {
            player.sendMessage("You can't reach that.")
            return@onObjectClick
        }
        player.sendMessage("The rock appears to move slightly as you touch it...")
        player.sendMessage("It's a trap! You don't see any way to disarm it; perhaps you can cross over it.")
        player.triggerFlatRockTrap()
    }

    onPlayerStep(FLAT_ROCK_HAZARDS.keys.toTypedArray()) { (player, _, tile) ->
        val trap = FLAT_ROCK_HAZARDS[tile] ?: return@onPlayerStep
        if (player.tile !in trap.hazardTiles && !hasPlacedPlank(player, trap.tile))
            player.triggerFlatRockTrap()
    }

    onItemOnObject(arrayOf(Loc.upass_double_springtrap_trigger, Loc.upass_double_springtrap), arrayOf(Obj.woodplank), checkDistance = false) { (player, obj) ->
        player.crossFlatRockWithPlank(resolveFlatRockTrap(obj))
    }

    onObjectClick(Loc.upass_logtrap_trigger, tiles = arrayOf(TRAPPED_ORB_TILE)) { (player) ->
        if (player.getQuestStage(Quest.UNDERGROUND_PASS) < STAGE_CROSSED_BRIDGE) {
            player.sendMessage("You find nothing useful.")
            return@onObjectClick
        }
        if (player.vars.getVarBit(orbVarbit(Obj.caveorb1)) == 1) {
            player.sendMessage("The orb that was hidden here has already been destroyed.")
            return@onObjectClick
        }
        if (player.hasOrb(Obj.caveorb1)) {
            player.sendMessage("You are already carrying this orb.")
            return@onObjectClick
        }
        if (!player.inventory.hasRoomFor(Item(Obj.caveorb1))) {
            player.sendMessage("You need some free inventory space to take the orb.")
            return@onObjectClick
        }
        player.faceTile(TRAPPED_ORB_TILE)
        player.anim(Seq.human_pickuptable)
        player.sendMessage("You try to disarm the trap...", true)
        if (!Utils.skillSuccess(
                player.skills.getLevel(Constants.THIEVING),
                player.auraManager.thievingMul,
                160,
                300
            )
        ) {
            player.sendMessage("You accidentally trigger the trap!", true)
            player.triggerFlatRockTrap()
            return@onObjectClick
        }
        player.giveUniqueQuestItem(Obj.caveorb1)
        removeVisibleOrb(player, Obj.caveorb1)
        player.sendMessage("You succeed at disarming the trap and take the orb.", true)
        player.sendMessage("You hear the trap resetting itself as you do so.", true)
    }

    onItemOnObject(
        arrayOf(Loc.furnace_upass),
        arrayOf(Obj.caveorb1, Obj.caveorb2, Obj.caveorb3, Obj.caveorb4)
    ) { (player, _, item) ->
        player.anim(Seq.human_furnace)
        player.inventory.deleteItem(item.id, 1)
        player.vars.saveVarBit(orbVarbit(item.id), 1)
        removeVisibleOrb(player, item.id)
        player.sendMessage("The orb cracks and is consumed by the furnace.")
        if (ORBS.all { player.vars.getVarBit(orbVarbit(it)) == 1 }) {
            player.sendMessage("With all four orbs destroyed, the magical barrier fades.")
            player.advanceUndergroundPass(STAGE_DESTROYED_ORBS)
        }
    }
}

private fun Player.hasOrb(itemId: Int) =
    inventory.containsItem(itemId) || bank.containsItem(itemId)

private fun syncVisibleOrbs(player: Player) {
    VISIBLE_ORB_SPAWNS.forEach { spawn ->
        if (
            player.getQuestStage(Quest.UNDERGROUND_PASS) != STAGE_CROSSED_BRIDGE ||
            player.getLongestDelta(spawn.tile) > ORB_RENDER_DISTANCE
        ) {
            removeVisibleOrb(player, spawn)
        } else {
            if (player.vars.getVarBit(orbVarbit(spawn.itemId)) == 1) {
                removeVisibleOrb(player, spawn)
                return@forEach
            }
            val chunk = ChunkManager.getChunk(spawn.tile.getChunkId())
            val existing = chunk.getGroundItem(spawn.itemId, spawn.tile, player.uuid)
            if (existing?.getMetaData(QUEST_ORB_METADATA) == true)
                return@forEach
            val groundItem = GroundItem(
                Item(spawn.itemId).addMetaData(QUEST_ORB_METADATA, true),
                spawn.tile,
                player.username,
                GroundItemType.INVISIBLE
            )
            chunk.addGroundItem(groundItem)
        }
    }
}

private fun removeVisibleOrb(player: Player, itemId: Int) {
    VISIBLE_ORB_SPAWNS.filter { it.itemId == itemId }.forEach { removeVisibleOrb(player, it) }
}

private fun removeVisibleOrb(player: Player, spawn: OrbSpawn) {
    val chunk = ChunkManager.getChunk(spawn.tile.getChunkId())
    val groundItem = chunk.getGroundItem(spawn.itemId, spawn.tile, player.uuid)
    if (groundItem?.getMetaData(QUEST_ORB_METADATA) == true)
        World.removeGroundItem(groundItem)
}

private fun Player.triggerFlatRockTrap() {
    val damage = ceil(hitpoints * 0.2).toInt().coerceAtLeast(1)
    forceTalk("Ouch!")
    applyHit(Hit(damage, Hit.HitLook.TRUE_DAMAGE))
}

private fun resolveFlatRockTrap(obj: GameObject): FlatRockTrap {
    val horizontalTraversal = obj.rotation % 2 == 1
    val flatRockTile = if (obj.id == Loc.upass_double_springtrap) {
        obj.tile.transform(
            if (horizontalTraversal) 0 else 1,
            if (horizontalTraversal) 1 else 0
        )
    } else {
        obj.tile
    }
    return FlatRockTrap(flatRockTile, horizontalTraversal)
}

private fun Player.crossFlatRockWithPlank(trap: FlatRockTrap) {
    val negativeSide = if (trap.horizontalTraversal) x < trap.tile.x else y < trap.tile.y
    val start = if (trap.horizontalTraversal)
        trap.tile.transform(if (negativeSide) -1 else 1, 0)
    else
        trap.tile.transform(0, if (negativeSide) -1 else 1)
    val destination = if (trap.horizontalTraversal)
        trap.tile.transform(if (negativeSide) 1 else -1, 0)
    else
        trap.tile.transform(0, if (negativeSide) 1 else -1)
    val direction = if (trap.horizontalTraversal) {
        if (negativeSide) Direction.EAST else Direction.WEST
    } else {
        if (negativeSide) Direction.NORTH else Direction.SOUTH
    }

    walkToAndExecute(start) {
        resetWalkSteps()
        faceTile(trap.tile)
        showPlacedPlank(this, trap)
        val crossingDistance = getLongestDelta(destination)
        sendMessage("You place the plank across the flat rock...", true)
        Agility.walkToAgility(this, Bas.human_walk_logbalance, direction, crossingDistance, crossingDistance)
        schedule {
            wait(crossingDistance + 3)
            removePlacedPlank(this@crossFlatRockWithPlank, trap)
            sendMessage("...and quickly walk over.", true)
        }
    }
}

private fun showPlacedPlank(player: Player, trap: FlatRockTrap) {
    player.tempAttribs.setB(placedPlankAttribute(trap.tile), true)
    player.packets.sendAddObject(
        GameObject(
            Loc.upass_crossing_plank,
            ObjectShape.GROUND_DECORATION,
            trap.rotation,
            trap.tile
        )
    )
}

private fun hasPlacedPlank(player: Player, tile: Tile) =
    player.tempAttribs.getB(placedPlankAttribute(tile))

private fun removePlacedPlank(player: Player, trap: FlatRockTrap) {
    if (!hasPlacedPlank(player, trap.tile))
        return
    player.tempAttribs.removeB(placedPlankAttribute(trap.tile))
    player.packets.sendAddObject(
        GameObject(
            Loc.upass_double_springtrap_trigger,
            ObjectShape.GROUND_DECORATION,
            trap.rotation,
            trap.tile
        )
    )
}

private fun removePlacedPlanks(player: Player) {
    FLAT_ROCK_TRAPS.forEach { removePlacedPlank(player, it) }
}

private fun placedPlankAttribute(tile: Tile) = "$PLACED_PLANK_ATTRIBUTE${tile.getTileHash()}"

private fun orbVarbit(itemId: Int) = when (itemId) {
    Obj.caveorb1 -> Varbit.upass_orb_1
    Obj.caveorb2 -> Varbit.upass_orb_2
    Obj.caveorb3 -> Varbit.upass_orb_3
    else -> Varbit.upass_orb_4
}
