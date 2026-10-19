package com.rs.game.content.skills.dungeoneering.rooms;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.RoomReference;

public interface RoomEvent {
	void openRoom(DungeonManager dungeon, RoomReference reference);
}
