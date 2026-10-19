package com.rs.utils.spawns;

import org.darkan.core.cache.NPCDefinitions;
import world.gregs.voidps.cache.definition.data.NPCDefinition;
import org.darkan.core.map.pathfinder.collision.CollisionStrategy;
import org.darkan.core.map.pathfinder.collision.CollisionStrategyType;
import com.rs.game.World;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.map.pathfinder.Direction;
import world.gregs.voidps.type.Tile;

public class NPCSpawn {

	private final String comment;
	private final int npcId;
	private final Tile tile;
	private final Direction dir;
	private final CollisionStrategyType clip;
	private String customName;

	public NPCSpawn(int npcId, Tile tile, Direction dir, String comment, CollisionStrategyType clip) {
		this.npcId = npcId;
		this.tile = tile;
		this.dir = dir;
		this.comment = comment;
        this.clip = clip;
    }

	public NPCSpawn(int npcId, Tile tile, String comment, CollisionStrategyType clip) {
		this(npcId, tile, null, comment, clip);
	}

	public NPCSpawn(int npcId, Tile tile, Direction dir, String comment) {
		this(npcId, tile, dir, comment, null);
	}

	public NPCSpawn(int npcId, Tile tile, String comment) {
		this(npcId, tile, null, comment, null);
	}

	public NPC spawn() {
		Tile spawnTile = tile;
		if ((npcId == 1973 || npcId == 1976) && tile.getPlane() == 0 && tile.getX() >= 2624 && tile.getX() <= 2743 && tile.getY() >= 5056 && tile.getY() <= 5119 && !World.floorAndWallsFree(tile, getDefs().getSize())) {
			spawnTile = nearestWalkableTile(tile, getDefs().getSize());
		}
		NPC npc = World.spawnNPC(npcId, spawnTile, dir, false, true, customName);
		if (clip != null)
			npc.setCollisionStrategyType(clip);
		return npc;
	}

	private Tile nearestWalkableTile(Tile origin, int size) {
		for (int radius = 1; radius <= 8; radius++) {
			for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
				for (int y = origin.getY() - radius; y <= origin.getY() + radius; y++) {
					if (Math.max(Math.abs(x - origin.getX()), Math.abs(y - origin.getY())) != radius)
						continue;
					Tile candidate = Tile.of(x, y, origin.getPlane());
					if (World.floorAndWallsFree(candidate, size))
						return candidate;
				}
			}
		}
		throw new IllegalStateException("No walkable spawn tile near " + origin + " for NPC " + npcId);
	}

	public NPC spawnAtCoords(Tile tile, Direction dir) {
		NPC npc = World.spawnNPC(npcId, tile, dir, false, true, customName);
		if (clip != null)
			npc.setCollisionStrategyType(clip);
		return npc;
	}

	public Tile getTile() {
		return tile;
	}

	public int getNPCId() {
		return npcId;
	}

	public String getComment() {
		return comment;
	}

	public Direction getDir() {
		return dir;
	}

	public NPCSpawn setCustomName(String customName) {
		this.customName = customName;
		return this;
	}

	public String getCustomName() {
		return customName;
	}

	public NPCDefinition getDefs() {
		return NPCDefinitions.getDefs(npcId);
	}

	public CollisionStrategyType getCollisionStrategyType() {
		return clip == null ? CollisionStrategyType.NORMAL : clip;
	}
}
