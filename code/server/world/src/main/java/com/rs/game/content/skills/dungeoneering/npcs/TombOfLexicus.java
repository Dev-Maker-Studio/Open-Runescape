package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.model.entity.Entity;
import world.gregs.voidps.type.Tile;

public class TombOfLexicus extends DungeonNPC {

	private final LexicusRunewright boss;

	public TombOfLexicus(LexicusRunewright boss, int id, Tile tile, DungeonManager manager) {
		super(id, tile, manager);
		setForceAgressive(true);
		this.boss = boss;
	}

	@Override
	public double getMeleePrayerMultiplier() {
		return 0.6;
	}

	@Override
	public double getMagePrayerMultiplier() {
		return 0.6;
	}

	@Override
	public double getRangePrayerMultiplier() {
		return 0.6;
	}

	@Override
	public void drop() {

	}

	@Override
	public int getMaxHitpoints() {
		return 10;
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		boss.removeBook(this);
	}
}
