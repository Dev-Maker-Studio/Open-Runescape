package com.rs.game.content.minigames.fightkiln.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;

import java.util.List;

public class HarAkenTentacle extends NPC {

	private final HarAken aken;

	public HarAkenTentacle(int id, Tile tile, HarAken aken) {
		super(id, tile, true);
		setForceMultiArea(true);
		setCantFollowUnderCombat(true);
		setForceAgressive(true);
		setNextAnimation(new Animation(id == 15209 ? 16238 : 16241));
		freeze(10000000);
		this.aken = aken;
	}

	@Override
	public void sendDeath(Entity source) {
		aken.removeTentacle(this);
		super.sendDeath(source);
	}

	@Override
	public boolean ignoreWallsWhenMeleeing() {
		return true;
	}

	@Override
	public List<Entity> getPossibleTargets() {
		return queryNearbyPlayersByTileRangeAsEntityList(30, player -> !player.isDead());
	}

	@Override
	public double getMagePrayerMultiplier() {
		return 0.1;
	}

	@Override
	public double getRangePrayerMultiplier() {
		return 0.1;
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0.1;
	}
}
