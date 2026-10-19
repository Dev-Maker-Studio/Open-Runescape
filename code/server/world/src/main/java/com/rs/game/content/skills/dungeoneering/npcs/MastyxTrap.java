package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;

public class MastyxTrap extends NPC {

	private static final int BASE_TRAP = 11076;

	private final String playerName;
	private int ticks;

	public MastyxTrap(String playerName, int id, Tile tile) {
		super(id, tile);
		this.playerName = playerName;
	}

	@Override
	public void processNPC() {
		//Doesn't move or do anything so we don't process it.
		ticks++;
		if (ticks == 500)
			finish();
	}

	public String getPlayerName() {
		return playerName;
	}

	public int getTier() {
		return getId() - BASE_TRAP;
	}
}
