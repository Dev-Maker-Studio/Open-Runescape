package com.rs.game.content.quests.watchtower.areas

import org.darkan.core.gameval.gen.Obj
import org.darkan.core.gameval.gen.Loc
import com.rs.game.content.world.LightSource
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Equipment
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onItemClick
import com.rs.plugin.kts.onPickupItem

private const val NIGHTSHADE_POISON_DAMAGE = 20

private val CAVE_1_SURFACE = Tile.of(2522, 3068, 0)
private val CAVE_2_SURFACE = Tile.of(2539, 3052, 0)
private val CAVE_3_SURFACE = Tile.of(2552, 3052, 0)
private val CAVE_4_SURFACE = Tile.of(2553, 3033, 0)
private val CAVE_5_SURFACE = Tile.of(2560, 3022, 0)
private val CAVE_6_SURFACE = Tile.of(2527, 3011, 0)

private val CAVE_1_INSIDE = Tile.of(2532, 9469, 0)
private val CAVE_2_INSIDE = Tile.of(2518, 9455, 0)
private val CAVE_3_INSIDE = Tile.of(2498, 9451, 0)
private val CAVE_4_INSIDE = Tile.of(2498, 9418, 0)
private val CAVE_5_INSIDE = Tile.of(2504, 9441, 0)
private val CAVE_6_INSIDE = Tile.of(2522, 9411, 0)

private val CAVE_1_OUTSIDE = Tile.of(2524, 3070, 0)
private val CAVE_2_OUTSIDE = Tile.of(2540, 3054, 0)
private val CAVE_3_OUTSIDE = Tile.of(2553, 3054, 0)
private val CAVE_4_OUTSIDE = Tile.of(2552, 3034, 0)
private val CAVE_5_OUTSIDE = Tile.of(2562, 3023, 0)
private val CAVE_6_OUTSIDE = Tile.of(2529, 3012, 0)

private val YANILLE_HOLE_IN_TILE = Tile.of(2530, 9415, 0)
private val YANILLE_HOLE_DESTINATION = Tile.of(2588, 3106, 0)

private data class SkavidCave(
    val entranceId: Int,
    val entranceTile: Tile,
    val insideTile: Tile,
    val exitId: Int,
    val outsideTile: Tile
)

private val SKAVID_CAVES = listOf(
    SkavidCave(
        Loc.skavid_cave2,
        CAVE_1_SURFACE,
        CAVE_1_INSIDE,
        Loc.cave2exit,
        CAVE_1_OUTSIDE
    ),
    SkavidCave(
        Loc.skavid_cave3,
        CAVE_2_SURFACE,
        CAVE_2_INSIDE,
        Loc.cave3exit,
        CAVE_2_OUTSIDE
    ),
    SkavidCave(
        Loc.skavid_cave4,
        CAVE_3_SURFACE,
        CAVE_3_INSIDE,
        Loc.cave4exit,
        CAVE_3_OUTSIDE
    ),
    SkavidCave(
        Loc.skavid_cave5,
        CAVE_4_SURFACE,
        CAVE_4_INSIDE,
        Loc.cave1exit,
        CAVE_4_OUTSIDE
    ),
    SkavidCave(
        Loc.skavid_cave1,
        CAVE_5_SURFACE,
        CAVE_5_INSIDE,
        Loc.cave5exit,
        CAVE_5_OUTSIDE
    ),
    SkavidCave(
        Loc.skavid_cave6,
        CAVE_6_SURFACE,
        CAVE_6_INSIDE,
        Loc.cave6exit,
        CAVE_6_OUTSIDE
    )
)

private val CAVES_BY_ENTRANCE =
    SKAVID_CAVES.associateBy { it.entranceId }

private val CAVES_BY_EXIT =
    SKAVID_CAVES.associateBy { it.exitId }

@ServerStartupEvent
fun mapWatchtowerSkavidCaves() {
    onItemClick(Obj.skavidmap, options = arrayOf("Read")) { (player) ->
        player.simpleDialogue("The map marks six skavid caves around Gu'Tanoth: one in the north-west, two in the north-east, one inside the city, one in the south-east and one in the south-west. Keep this map with you to find your way through the caves.")
    }

    onObjectClick(
        *CAVES_BY_ENTRANCE.keys.toTypedArray<Any>(),
        checkDistance = true
    ) { (player, obj) ->
        val cave =
            CAVES_BY_ENTRANCE[obj.id]
                ?: return@onObjectClick

        if (!obj.tile.isAt(cave.entranceTile.x, cave.entranceTile.y))
            return@onObjectClick

        if (!player.inventory.containsItem(Obj.skavidmap, 1)) {
            player.sendMessage(
                "The caves are too complex to navigate without a map."
            )
            return@onObjectClick
        }

        if (!LightSource.hasLightSource(player)) {
            player.sendMessage(
                "It is too dark to enter the cave without a light source."
            )
            return@onObjectClick
        }

        player.useStairs(cave.insideTile)
    }

    onObjectClick(
        *CAVES_BY_EXIT.keys.toTypedArray<Any>(),
        checkDistance = true
    ) { (player, obj) ->
        val cave =
            CAVES_BY_EXIT[obj.id]
                ?: return@onObjectClick

        player.useStairs(cave.outsideTile)
    }

    onObjectClick(Loc.yanilleholein, tiles = arrayOf(YANILLE_HOLE_IN_TILE)) { (player, _, option) ->
        if (option != "Climb-down") return@onObjectClick
        player.sendMessage("You enter the tunnel.")
        player.useStairs(YANILLE_HOLE_DESTINATION)
        player.sendMessage("So that's how the skavids are getting into Yanille!")
    }

    onPickupItem(Obj.nightshade) { event ->
        val player = event.player

        if (hasArmouredGloves(player)) {
            return@onPickupItem
        }

        event.cancelPickup()

        player.applyHit(
            Hit(
                player,
                NIGHTSHADE_POISON_DAMAGE,
                Hit.HitLook.POISON_DAMAGE
            )
        )

        player.sendMessage(
            "The nightshade is highly poisonous! You need armoured gloves to pick it safely."
        )
    }
}

private fun hasArmouredGloves(player: Player): Boolean {
    val gloves =
        player.equipment[Equipment.HANDS]
            ?: return false

    if (gloves.name.contains("bracelet", ignoreCase = true)) {
        return false
    }

    val bonuses =
        gloves.definitions.getBonuses()

    return (5..10).any { index ->
        bonuses.getOrElse(index) { 0 } > 0
    }
}
