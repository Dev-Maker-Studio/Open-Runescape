package com.rs.utils.spawns;

import org.darkan.core.model.ObjectShape;
import com.rs.game.World;
import com.rs.game.model.gameobject.GameObject;
import world.gregs.voidps.type.Tile;

public class ObjectSpawn {

	private final String comment;
	private final int objectId;
	private final int type;
	private final int rotation;
	private final Tile tile;

	public ObjectSpawn(int objectId, int type, int rotation, Tile tile) {
		this(objectId, type, rotation, tile, null);
	}

	public ObjectSpawn(int objectId, int type, int rotation, Tile tile, String comment) {
		this.objectId = objectId;
		this.type = type;
		this.rotation = rotation;
		this.tile = tile;
		this.comment = comment;
	}

	public String getComment() {
		return comment;
	}

	public int getObjectId() {
		return objectId;
	}

	public int getType() {
		return type;
	}

	public int getRotation() {
		return rotation;
	}

	public Tile getTile() {
		return tile;
	}

	public void spawn() {
		World.spawnObject(new GameObject(objectId, ObjectShape.forId(type), rotation, tile.getX(), tile.getY(), tile.getPlane()));
	}
}
