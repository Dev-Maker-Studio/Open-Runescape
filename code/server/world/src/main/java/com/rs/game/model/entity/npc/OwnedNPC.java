package com.rs.game.model.entity.npc;

import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

public class OwnedNPC extends NPC {

	private final Player owner;
	private final boolean hideFromOtherPlayers;
	private boolean autoDespawnAtDistance = true;

	public OwnedNPC(Player owner, int id, Tile tile, boolean hideFromOtherPlayers) {
		super(id, tile, true);
		this.owner = owner;
		this.hideFromOtherPlayers = hideFromOtherPlayers;
	}

	@Override
	public void processNPC() {
		if (getOwner() == null || getOwner().hasFinished() || (autoDespawnAtDistance && !withinDistance(getOwner(), 15))) {
			onDespawnEarly();
			finish();
		}
		super.processNPC();
	}

	@Override
	public boolean withinDistance(Player player, int distance) {
		if (!hideFromOtherPlayers || player == getOwner())
			return super.withinDistance(player, distance);
		return false;
	}

	@Override
	public boolean canBeAttackedBy(Player player) {
		if (getOwner() != player) {
			player.sendMessage("They aren't interested in you.");
			return false;
		}
		return true;
	}

	public void onDespawnEarly() {}

	public Player getOwner() {
		return owner;
	}

	public void teleToOwner() {
		Tile tile = owner.getNearestTeleTile(this);
		if (tile != null)
			tele(tile);
	}

	public boolean isAutoDespawnAtDistance() {
		return autoDespawnAtDistance;
	}

	public void setAutoDespawnAtDistance(boolean autoDespawnAtDistance) {
		this.autoDespawnAtDistance = autoDespawnAtDistance;
	}
}
