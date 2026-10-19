package com.rs.game.content.world.areas.ape_atoll;

import com.rs.game.World.getObjectWithId
import com.rs.game.World.spawnObjectTemporary
import com.rs.game.model.gameobject.GameObject
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapApeAtoll() {
	val ladders = mapOf(
		4714 to Tile.of(2803, 9170, 0),
		4743 to Tile.of(2803, 2725, 0),
		4728 to Tile.of(2765, 2768, 0),
		4889 to Tile.of(2748, 2767, 0)
	)

	ladders.forEach { (objectId, toTile) ->
		onObjectClick(objectId) { (player) ->
			player.useLadder(toTile)
		}
	}

	onObjectClick(4773, 4779) { (player, obj) ->
		val deltaX = when(obj.rotation) {
			0 -> if (obj.id == 4773) -2 else 2
			3 -> 0
			else -> 0
		}
		val deltaY = when(obj.rotation) {
			0 -> 0
			3 -> if (obj.id == 4773) -2 else 2
			else -> 0
		}
		val deltaZ = if (obj.id == 4773) 1 else -1

		player.tele(player.transform(deltaX, deltaY, deltaZ))
	}

	onObjectClick(4859) { (player) ->
		player.prayer.worshipAltar()
	}

	onObjectClick(4755) { (player, obj) ->
		when (obj.rotation) {
			0 -> player.useStairs(-1, player.transform(0, -4, -1), 1, 1);
			1 -> player.useStairs(-1, player.transform(4, 0, -1), 1, 1);
			2 -> player.useStairs(-1, player.transform(0, 4, -1), 1, 1);
			3 -> player.useStairs(-1, player.transform(-4, 0, -1), 1, 1);
		}
	}

	onObjectClick(4756) { (player, obj) ->
		when (obj.rotation) {
			0 -> player.useStairs(-1, player.transform(0, 4, 1), 1, 1);
			1 -> player.useStairs(-1, player.transform(-4, 0, 1), 1, 1);
			2 -> player.useStairs(-1, player.transform(0, -4, 1), 1, 1);
			3 -> player.useStairs(-1, player.transform(4, 0, 1), 1, 1);
		}
	}

	// Main gates into Ape Atoll
	onObjectClick(4787, 4788) { (player) ->
		val eastDoor = getObjectWithId(Tile.of(2721, 2766, 0), 4788)
		val westDoor = getObjectWithId(Tile.of(2719, 2766, 0), 4787)
		if (eastDoor != null && westDoor != null) {
			spawnObjectTemporary(GameObject(eastDoor, 83), 3, true)
			spawnObjectTemporary(GameObject(westDoor, 83), 3, true)
			spawnObjectTemporary(GameObject(eastDoor.id+2, eastDoor.type, eastDoor.getRotation(0), eastDoor.tile.transform(1, 0, 0)), 3, true)
			spawnObjectTemporary(GameObject(westDoor.id+2, westDoor.type, westDoor.getRotation(0), westDoor.tile.transform(0, 0, 0)), 3, true)
			player.addWalkSteps(player.x, player.y + (if (player.y > 2765) -2 else 2), 2, false)
		} else {
			player.sendMessage("Someone else is using the gate at the moment.")
		}
	}
}
