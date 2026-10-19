package com.rs.game.content.quests.observatoryquest

import com.rs.engine.pathfinder.RouteEvent
import com.rs.engine.quest.Quest
import com.rs.game.World
import com.rs.game.model.entity.async.schedule
import com.rs.game.model.entity.player.Player
import com.rs.game.model.gameobject.GameObject
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onChunkEnter
import com.rs.plugin.kts.onLogin
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Seq
import org.darkan.core.model.ObjectShape
import world.gregs.voidps.type.Tile

private val OBSERVATORY_ROPE_TILE = Tile.of(2446, 3171, 0)
private val OBSERVATORY_ROPE_UP_DESTINATION = Tile.of(2445, 3166, 0)
private val OBSERVATORY_ROPE = GameObject(Loc.myq5_drakans_bunker_rope, ObjectShape.SCENERY_INTERACT, 0, OBSERVATORY_ROPE_TILE)

fun syncObservatoryRope(player: Player) {
    if (player.isQuestComplete(Quest.OBSERVATORY_QUEST))
        player.packets.sendAddObject(OBSERVATORY_ROPE)
    else
        player.packets.sendRemoveObject(OBSERVATORY_ROPE)
}

@ServerStartupEvent
fun mapObservatoryExit() {
    World.spawnObject(OBSERVATORY_ROPE, false)

    onLogin { (player) ->
        player.schedule {
            wait(2)
            if (player.plane == 0 && player.x in 2432..2455 && player.y in 3152..3175)
                syncObservatoryRope(player)
        }
    }
    onChunkEnter { e ->
        val player = e.player ?: return@onChunkEnter
        if (player.plane == 0 && player.x in 2432..2455 && player.y in 3152..3175)
            syncObservatoryRope(player)
    }

    onObjectClick(Loc.qip_obs_door_left, Loc.qip_obs_door_right, tiles = arrayOf(Tile.of(2445, 3165, 0), Tile.of(2444, 3166, 0))) { (player, door) ->
        if (!player.isQuestComplete(Quest.OBSERVATORY_QUEST)) {
            player.sendMessage("The door is locked.")
            return@onObjectClick
        }
        val left = World.getObjectWithId(Tile.of(2445, 3165, 0), Loc.qip_obs_door_left) ?: return@onObjectClick
        val right = World.getObjectWithId(Tile.of(2444, 3166, 0), Loc.qip_obs_door_right) ?: return@onObjectClick
        World.spawnObjectTemporary(GameObject(left.id, left.type, (left.rotation + 1) and 3, left.tile), 4, false)
        World.spawnObjectTemporary(GameObject(right.id, right.type, (right.rotation + 3) and 3, right.tile), 4, false)
        val destination = if (player.x + player.y < door.x + door.y) OBSERVATORY_ROPE_UP_DESTINATION else Tile.of(2443, 3164, 0)
        player.addWalkSteps(destination, 4, false)
    }

    onObjectClick(Loc.myq5_drakans_bunker_rope, tiles = arrayOf(OBSERVATORY_ROPE_TILE), checkDistance = false) { (player) ->
        if (player.isLocked || player.plane != 0) return@onObjectClick
        if (!player.isQuestComplete(Quest.OBSERVATORY_QUEST)) {
            player.sendMessage("The professor has not secured the rope yet.")
            return@onObjectClick
        }
        val descending = player.y <= 3168
        val approach = if (descending) OBSERVATORY_ROPE_UP_DESTINATION else OBSERVATORY_ROPE_TILE
        val destination = if (descending) OBSERVATORY_ROPE_TILE else OBSERVATORY_ROPE_UP_DESTINATION
        val climb = Runnable {
            if (player.isLocked || !player.isQuestComplete(Quest.OBSERVATORY_QUEST) || !player.withinDistance(approach, 1)) return@Runnable
            player.useStairs(Seq.emote_climbing_rope, destination, 1, 2)
        }
        player.stopAll()
        if (player.withinDistance(approach, 1)) climb.run()
        else player.setRouteEvent(RouteEvent(approach, climb))
    }
}
