package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.model.entity.Entity;
import world.gregs.voidps.type.Tile;

public class FleshspoilerSpawn extends DungeonNPC {

	private final FleshspoilerHaasghenahk boss;

	public FleshspoilerSpawn(FleshspoilerHaasghenahk boss, Tile tile, DungeonManager manager) {
		super(getId(boss.getId()), tile, manager);
		this.boss = boss;
		setForceAgressive(true);
		setLureDelay(Integer.MAX_VALUE);
		setForceFollowClose(true);
		setRun(true);
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0.0;//Fully block it.
	}

	@Override
	public void drop() {

	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		boss.removeFleshCreature(this);
	}

	private static int getId(int bossId) {
		if (bossId >= 11895 && bossId <= 11909)
			return bossId + 15;
		return bossId - 15;
	}
}
