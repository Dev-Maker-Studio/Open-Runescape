package com.rs.game.content.minigames.fightcaves.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;

import java.util.List;

public class FightCavesNPC extends NPC {

	public FightCavesNPC(int id, Tile tile) {
		super(id, tile, true);
		setForceMultiArea(true);
		setIgnoreDocile(true);
		setNoDistanceCheck(true);
	}

	@Override
	public void sendDeath(Entity source) {
		setNextSpotAnim(new SpotAnim(2924 + getSize()));
		super.sendDeath(source);
	}

	@Override
	public List<Entity> getPossibleTargets() {
		return queryNearbyPlayersByTileRangeAsEntityList(64, player -> !player.isDead());
	}

}
