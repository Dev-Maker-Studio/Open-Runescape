package com.rs.utils.spawns;

import com.rs.game.World;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;

public class ItemSpawn {

	private final String comment;
	private final int itemId;
	private final int amount;
	private final Tile tile;
	private final int respawnTicks;

	public ItemSpawn(int itemId, int amount, Tile tile, int respawnTicks, String comment) {
		this.itemId = itemId;
		this.amount = amount;
		this.tile = tile;
		this.respawnTicks = respawnTicks;
		this.comment = comment;
	}

	@SuppressWarnings("deprecation")
	public void spawn() {
		World.addGroundItemForever(new Item(itemId, amount), tile, getRespawnTicks());
	}

	public Tile getTile() {
		return tile;
	}

	public int getItemId() {
		return itemId;
	}

	public int getAmount() {
		return amount;
	}

	public String getComment() {
		return comment;
	}

	public int getRespawnTicks() {
		return respawnTicks <= 0 ? 25 : respawnTicks;
	}
}
