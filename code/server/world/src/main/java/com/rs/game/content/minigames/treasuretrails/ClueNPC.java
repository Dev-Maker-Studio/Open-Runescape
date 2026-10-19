package com.rs.game.content.minigames.treasuretrails;

import com.rs.game.model.entity.npc.OwnedNPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

public class ClueNPC extends OwnedNPC {

	public ClueNPC(Player target, int id, Tile tile) {
		super(target, id, tile, false);
	}

	@Override
	public void onDespawnEarly() {
		getOwner().getTreasureTrailsManager().setPhase(0);
	}

	@Override
	public void drop() {
		getOwner().getTreasureTrailsManager().setPhase(2);
	}
}
