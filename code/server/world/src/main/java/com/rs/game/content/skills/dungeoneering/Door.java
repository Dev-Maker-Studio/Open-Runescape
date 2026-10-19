package com.rs.game.content.skills.dungeoneering;

public class Door {

	private final DungeonConstants.DoorType type;
	private final int id, level;

	public Door(DungeonConstants.DoorType type, int id, int level) {
		this.type = type;
		this.id = id;
		this.level = level;
	}

	public Door(DungeonConstants.DoorType type, int id) {
		this(type, id, 0);
	}

	public Door(DungeonConstants.DoorType type) {
		this(type, 0);
	}

	public DungeonConstants.DoorType getType() {
		return type;
	}

	public int getId() {
		return id;
	}

	public int getLevel() {
		return level;
	}
}
