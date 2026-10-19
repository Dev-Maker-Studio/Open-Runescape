package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.model.entity.Entity;
import world.gregs.voidps.type.Tile;

public class SkeletalMinion extends DungeonNPC {

	private final NecroLord boss;

	public SkeletalMinion(NecroLord boss, int id, Tile tile, DungeonManager manager) {
		super(id, tile, manager);
		setForceAgressive(true);
		this.boss = boss;
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0.6;
	}

	@Override
	public void drop() {

	}

	@Override
	public int getMaxHit() {
		return super.getMaxHit() * 2;
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		boss.removeSkeleton(this);
	}
}
