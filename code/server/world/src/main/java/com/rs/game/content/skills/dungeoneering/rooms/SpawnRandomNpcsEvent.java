package com.rs.game.content.skills.dungeoneering.rooms;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.RoomReference;

public class SpawnRandomNpcsEvent implements RoomEvent {
	@Override
	public void openRoom(DungeonManager dungeon, RoomReference reference) {
		dungeon.spawnRandomNPCS(reference);
	}

}
