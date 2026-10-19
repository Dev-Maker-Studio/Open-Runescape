package com.rs.game.content.skills.dungeoneering.rooms;

import com.rs.game.content.skills.dungeoneering.DungeonConstants;

public class BossRoom extends HandledRoom {

	private final int minFloor;
	private final int musicId;

	public BossRoom(RoomEvent event, int musicId, int minFloor, int chunkX, int chunkY) {
		super(chunkX, chunkY, event, null, DungeonConstants.SOUTH_DOOR);
		this.minFloor = minFloor;
		this.musicId = musicId;
	}

	public int getMinFloor() {
		return minFloor;
	}

	public int getMusicId() {
		return musicId;
	}

	@Override
	public boolean allowResources() {
		return false;
	}

	//	@Override
	//	public final boolean allowSpecialDoors() {
	//		return false;
	//	}
}
