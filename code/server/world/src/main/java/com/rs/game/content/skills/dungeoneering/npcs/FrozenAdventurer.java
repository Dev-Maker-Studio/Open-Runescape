package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.npcs.combat.ToKashBloodChillerCombat;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

public class FrozenAdventurer extends NPC {

	private transient Player player;

	public FrozenAdventurer(int id, Tile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
		super(id, tile, true);
	}

	@Override
	public void processNPC() {
		if (player == null || player.isDead() || player.hasFinished()) {
			finish();
			return;
		}
		if (!player.getAppearance().isNPC()) {
			ToKashBloodChillerCombat.removeSpecialFreeze(player);
			finish();
			return;
		}
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	public Player getFrozenPlayer() {
		return player;
	}

}
