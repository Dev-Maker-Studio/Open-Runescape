package com.rs.game.content.quests.deserttreasure.objects

import com.rs.engine.quest.Quest
import com.rs.game.content.quests.deserttreasure.STAGE_RETURN_TO_JALDRAOCHT
import com.rs.game.content.world.doors.Doors
import com.rs.game.model.entity.npc.OwnedNPC
import com.rs.game.model.entity.player.Player
import com.rs.game.model.entity.player.Skills
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import world.gregs.voidps.type.Tile

private val PYRAMID_DOOR_LEFT_TILE = Tile.of(3233, 2899, 0)
private val PYRAMID_DOOR_RIGHT_TILE = Tile.of(3232, 2899, 0)

private val EXTERIOR_DOWN_TILE = Tile.of(3233, 2897, 0)

private val LEVEL_1_UP_TILE = Tile.of(2913, 4953, 3)
private val LEVEL_1_DOWN_TILE = Tile.of(2909, 4964, 3)

private val LEVEL_2_UP_TILE = Tile.of(2845, 4964, 2)
private val LEVEL_2_DOWN_TILE = Tile.of(2846, 4973, 2)

private val LEVEL_3_UP_TILE = Tile.of(2782, 4973, 1)
private val LEVEL_3_DOWN_TILE = Tile.of(2784, 4941, 1)

private val LEVEL_4_UP_TILE = Tile.of(3232, 9293, 0)

private val PYRAMID_TRAP_EXIT = Tile.of(3233, 2883, 0)

private val LEVEL_1_TRAP_TILES = regionTiles(45, 77, 3)
private val LEVEL_2_TRAP_TILES = regionTiles(44, 77, 2)
private val LEVEL_3_TRAP_TILES = regionTiles(43, 77, 1)
private val LEVEL_4_TRAP_TILES = regionTiles(50, 145, 0)

private val SAFE_LADDER_TILES = setOf(
    LEVEL_1_UP_TILE, LEVEL_1_DOWN_TILE,
    LEVEL_2_UP_TILE, LEVEL_2_DOWN_TILE,
    LEVEL_3_UP_TILE, LEVEL_3_DOWN_TILE, LEVEL_4_UP_TILE
)

private const val BASE_TRAP_DENOMINATOR = 18

private const val SCARAB_ROLL = 180
private const val SCARAB_COOLDOWN_MS = 15_000L
private const val SCARAB_COOLDOWN_KEY = "dtPyramidLastScarab"

@ServerStartupEvent
fun mapJaldraochtPyramid() {
    onObjectClick(
        Loc.four_diamonds_door_1,
        Loc.four_diamonds_door_mirror,
        tiles = arrayOf(PYRAMID_DOOR_LEFT_TILE, PYRAMID_DOOR_RIGHT_TILE)
    ) { (player, obj) ->
        if (!canEnterPyramid(player)) {
            player.sendMessage("A powerful magical force prevents you from opening the pyramid.")
            return@onObjectClick
        }
        Doors.handleDoubleDoor(player, obj)
    }

    onObjectClick(Loc.desert_laddertop, tiles = arrayOf(EXTERIOR_DOWN_TILE)) { (player) ->
        if (!canEnterPyramid(player)) {
            player.sendMessage("A powerful magical force prevents you from entering the pyramid.")
            return@onObjectClick
        }
        player.useLadder(LEVEL_1_UP_TILE)
    }

    onObjectClick(Loc.desert_ladder, tiles = arrayOf(LEVEL_1_UP_TILE)) { (player) ->
        player.useLadder(EXTERIOR_DOWN_TILE)
    }
    onObjectClick(Loc.desert_laddertop3_2, tiles = arrayOf(LEVEL_1_DOWN_TILE)) { (player) ->
        player.useLadder(LEVEL_2_UP_TILE)
    }

    onObjectClick(Loc.desert_ladder2_3, tiles = arrayOf(LEVEL_2_UP_TILE)) { (player) ->
        player.useLadder(LEVEL_1_DOWN_TILE)
    }
    onObjectClick(Loc.desert_laddertop2_1, tiles = arrayOf(LEVEL_2_DOWN_TILE)) { (player) ->
        player.useLadder(LEVEL_3_UP_TILE)
    }

    onObjectClick(Loc.desert_ladder1_2, tiles = arrayOf(LEVEL_3_UP_TILE)) { (player) ->
        player.useLadder(LEVEL_2_DOWN_TILE)
    }
    onObjectClick(Loc.desert_laddertop1_0, tiles = arrayOf(LEVEL_3_DOWN_TILE)) { (player) ->
        player.useLadder(LEVEL_4_UP_TILE)
    }

    onObjectClick(Loc.desert_ladder0_1, tiles = arrayOf(LEVEL_4_UP_TILE)) { (player) ->
        player.useLadder(LEVEL_3_DOWN_TILE)
    }

    onPlayerStep(LEVEL_1_TRAP_TILES) { (player, _, tile) ->
        processPyramidStep(player, tile)
    }
    onPlayerStep(LEVEL_2_TRAP_TILES) { (player, _, tile) ->
        processPyramidStep(player, tile)
    }
    onPlayerStep(LEVEL_3_TRAP_TILES) { (player, _, tile) ->
        processPyramidStep(player, tile)
    }
    onPlayerStep(LEVEL_4_TRAP_TILES) { (player, _, tile) ->
        processPyramidStep(player, tile)
    }
}

private fun canEnterPyramid(player: Player): Boolean =
    player.getQuestStage(Quest.DESERT_TREASURE) >= STAGE_RETURN_TO_JALDRAOCHT

private fun processPyramidStep(player: Player, tile: Tile) {
    if (!canEnterPyramid(player) || tile in SAFE_LADDER_TILES || player.isLocked ||
        (tile.plane == 0 && tile.x in 3228..3238 && tile.y in 9310..9320))
        return

    if (rollPyramidTrap(player)) {
        player.stopAll()
        player.sendMessage("A trapdoor opens beneath your feet!")
        player.tele(PYRAMID_TRAP_EXIT)
        return
    }

    rollScarabEncounter(player)
}

private fun rollPyramidTrap(player: Player): Boolean {
    val agility = player.skills.getLevel(Skills.AGILITY)
    val thieving = player.skills.getLevel(Skills.THIEVING)
    val weight = player.weight

    val skillBonus = (agility + thieving) / 4
    val weightAdjustment = when {
        weight <= 0.0 -> 45
        weight <= 10.0 -> 30
        weight <= 20.0 -> 15
        weight <= 30.0 -> 0
        weight <= 40.0 -> -4
        else -> -8
    }
    val runningBonus = if (player.isRunning) 10 else 0

    val denominator = (BASE_TRAP_DENOMINATOR + skillBonus + weightAdjustment + runningBonus)
        .coerceAtLeast(8)

    return Utils.random(denominator) == 0
}

private fun rollScarabEncounter(player: Player) {
    val now = System.currentTimeMillis()
    if (now - player.getL(SCARAB_COOLDOWN_KEY, 0L) < SCARAB_COOLDOWN_MS)
        return
    if (Utils.random(SCARAB_ROLL) != 0)
        return

    player.set(SCARAB_COOLDOWN_KEY, now)
    player.stopAll()

    OwnedNPC(player, Npc.ics_scarab_swarm, player.tile, false).combatTarget = player
    player.sendMessage("A swarm of scarabs boils up from beneath the floor!")
}

private fun regionTiles(regionX: Int, regionY: Int, plane: Int): Array<Tile> {
    val baseX = regionX * 64
    val baseY = regionY * 64
    return Array(64 * 64) { index ->
        val localX = index and 0x3F
        val localY = index shr 6
        Tile.of(baseX + localX, baseY + localY, plane)
    }
}
