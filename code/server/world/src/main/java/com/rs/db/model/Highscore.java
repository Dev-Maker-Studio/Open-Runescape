package com.rs.db.model;

import com.rs.game.model.entity.player.Player;

public class Highscore {

	private final String username;
	private final String displayName;
	private final boolean ironman;
	private final int totalLevel;
	private final long totalXp;
	private final int[] xp;

	public Highscore(Player player) {
		if (player.getDisplayName() == null)
			throw new RuntimeException("No display name found for player: " + player.getUsername());
		username = player.getUsername();
		displayName = player.getDisplayName();
		ironman = player.isIronMan();
		totalLevel = player.getSkills().getTotalLevel();
		totalXp = player.getSkills().getTotalXp();
		xp = player.getSkills().getXpInt();
	}

	public boolean isIronman() {
		return ironman;
	}

	public int getTotalLevel() {
		return totalLevel;
	}

	public long getTotalXp() {
		return totalXp;
	}

	public int[] getXp() {
		return xp;
	}

	public String getUsername() {
		return username;
	}

	public String getDisplayName() {
		return displayName;
	}
}
