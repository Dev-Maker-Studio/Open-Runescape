package com.rs.game.content.world.areas.falador

import com.rs.game.content.bosses.instance.isInstanceOption
import com.rs.game.content.items.Spade
import com.rs.game.content.skills.agility.Agility
import com.rs.game.content.world.areas.global.AgilityShortcuts
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.plugin.kts.onPlayerStep
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import org.darkan.core.map.pathfinder.Direction
import world.gregs.voidps.type.Tile

private const val ARTISANS_WORKSHOP_SONG = 582
private const val RISING_SUN_INN_SONG = 718

@ServerStartupEvent
fun mapFalador() {
    onPlayerStep(arrayOf(Tile.of(3035, 3339, 0), Tile.of(3035, 3338, 0), Tile.of(3034, 3339, 0), Tile.of(3034, 3338, 0))) { (player, step, tile) ->
        when {
            tile.x == 3035 && step.dir == Direction.EAST -> player.musicsManager.playSpecificAmbientSong(ARTISANS_WORKSHOP_SONG, true)
            tile.x == 3034 && player.musicsManager.isPlaying(ARTISANS_WORKSHOP_SONG) -> player.musicsManager.nextAmbientSong()
        }
    }

    onPlayerStep(arrayOf(Tile.of(2956, 3378, 0), Tile.of(2956, 3379, 0), Tile.of(2961, 3372, 0), Tile.of(2962, 3372, 0))) { (player, step, tile) ->
        when {
            tile.y == 3378 && step.dir == Direction.SOUTH -> player.musicsManager.playSpecificAmbientSong(RISING_SUN_INN_SONG, true)
            tile.x == 2961 && step.dir == Direction.WEST -> player.musicsManager.playSpecificAmbientSong(RISING_SUN_INN_SONG, true)
            (tile.y == 3379 || tile.x == 2961) && player.musicsManager.isPlaying(RISING_SUN_INN_SONG) -> player.musicsManager.nextAmbientSong()
        }
    }

    onObjectClick(Loc.falador_sc_castlewall_south, Loc.falador_sc_castlewall_north) { (player, obj) ->
        if (!Agility.hasLevel(player, 26)) return@onObjectClick
        AgilityShortcuts.crawlUnder(player, player.transform(0, if (obj.id == Loc.falador_sc_castlewall_north) -4 else 4, 0))
    }

    onObjectClick(Loc.fai_falador_castle_crumble_mid) { (player, obj) ->
        if (!Agility.hasLevel(player, 5)) return@onObjectClick
        AgilityShortcuts.climbOver(player, player.transform(if (player.x < obj.x) 2 else -2, 0, 0))
    }

    onObjectClick(Loc.farming_style, tiles = arrayOf(Tile.of(3063, 3282, 0))) { (player, obj) ->
        if (player.y != obj.y) AgilityShortcuts.climbOver(player, Tile.of(obj.x, obj.y + if (player.y > obj.y) -1 else 1, obj.plane))
    }

    onObjectClick(Loc.farming_style, tiles = arrayOf(Tile.of(3043, 3305, 0))) { (player, obj) ->
        if (player.x != obj.x) AgilityShortcuts.climbOver(player, Tile.of(obj.x + if (player.x > obj.x) -1 else 1, obj.y, obj.plane))
    }

    onObjectClick(Loc.fai_falador_stairs, Loc.fai_falador_stairstop) { (player, obj) ->
        val up = obj.id == Loc.fai_falador_stairs
        val dy = if (obj.rotation == 0) (if (up) 4 else -4) else 0
        player.tele(player.transform(0, dy, if (up) 1 else -1))
    }

    onObjectClick(Loc.artisan_reward_tunnel, Loc.artisan_track_tunnel) { (player, obj) ->
        player.useStairs(-1, if (obj.id == Loc.artisan_reward_tunnel) Tile.of(3037, 3342, 0) else Tile.of(3061, 3335, 0), 1, 1)
    }

    onObjectClick(Loc.artisan_stairs_to_cannons, Loc.artisan_stairs_to_reward, Loc.artisan_stairs_to_tracks) { (player, obj) ->
        player.useStairs(-1, if (obj.id == Loc.artisan_stairs_to_tracks) Tile.of(3035, 9713, 0) else Tile.of(3067, 9710, 0), 1, 1)
    }

    onObjectClick(Loc.loc_12202) { (player, obj, option) ->
        if (isInstanceOption(option)) return@onObjectClick
        if (!player.inventory.containsItem(Obj.spade, 1)) return@onObjectClick player.sendMessage("You need a spade to dig this.")
        if (player.x == obj.x && player.y == obj.y) return@onObjectClick Spade.dig(player)
        player.lock()
        player.addWalkSteps(obj.x, obj.y)
        WorldTasks.schedule(1) { Spade.dig(player) }
    }

    onObjectClick(Loc.loc_12230) { (player) -> player.tele(Tile.of(2996, 3378, 0)) }

    onObjectClick(Loc.fai_falador_outdoorstairs_bottom, Loc.fai_falador_outdoorstairs_top) { (player, obj) ->
        if (obj.id == Loc.fai_falador_outdoorstairs_bottom) player.useStairs(-1, Tile.of(2968, 3348, 1), 0, 1)
        else player.useStairs(-1, Tile.of(2971, 3347, 0), 0, 1)
    }
}
