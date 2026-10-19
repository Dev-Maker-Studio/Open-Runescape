package com.rs.game.content.skills.dungeoneering.npcs.misc;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.npcs.DungeonNPC;
import com.rs.game.content.skills.dungeoneering.skills.DungeoneeringFishing.Fish;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

public class DungeonFishSpot extends DungeonNPC {

	private final Fish fish;
	private int fishes;

	public DungeonFishSpot(int id, Tile tile, DungeonManager manager, Fish fish) {
		super(id, tile, manager);
		this.fish = fish;
		this.fishes = 14;
		setName(Utils.formatPlayerNameForDisplay(fish.toString()));
	}

	@Override
	public void processNPC() {

	}

	public Fish getFish() {
		return fish;
	}

	public int decreaseFishes() {
		return fishes--;
	}

	public void addFishes() {
		fishes += Utils.random(5, 10);
	}
}
