package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import world.gregs.voidps.type.Tile;

public class Guardian extends DungeonNPC {

	public Guardian(int id, Tile tile, DungeonManager manager) {
		super(id, tile, manager);
		setForceAgressive(true);
	}

}
