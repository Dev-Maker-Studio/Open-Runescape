package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.magic.Magic;
import com.rs.game.content.skills.magic.TeleType;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

public class WarpedSphere extends DungeonNPC {

	private static final int[][] ORB_TELEPORT_LOCATIONS = {
		{ 0, 0 },
		{ 0, 0 },
		{ 13, 4 },
		{ 11, 12 },
		{ 3, 4 },
		{ 3, 12 }
	};

	private static final int[][] PLAYER_TELEPORT_LOCATIONS = {
		{ 0, 0 },
		{ 0, 0 },
		{ 6, 11 },
		{ 10, 4 },
		{ 10, 11 },
		{ 10, 4 }
	};

	private final RoomReference reference;
	private int stage;

	public WarpedSphere(RoomReference reference, int id, Tile tile, DungeonManager manager) {
		super(id, tile, manager);
		this.reference = reference;
		this.stage = -1; // Gotta follow warmonger
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (stage > 0)
			if (hasWalkSteps()) {
				Tile previousTile = getLastTile();
				if (previousTile == null)
					return;
				final int[] TELEPORT_TILE = PLAYER_TELEPORT_LOCATIONS[stage];
				for (Player player : getManager().getParty().getTeam())
					if (!player.isLocked() && player.matches(previousTile)) {
						player.applyHit(new Hit(this, 100, HitLook.REFLECTED_DAMAGE));
						Magic.sendTeleportSpell(player, 13493, 13494, 2437, 2435, 0, 0, getManager().getTile(reference, TELEPORT_TILE[0], TELEPORT_TILE[1]), 6, false, TeleType.MAGIC, null);
					}
			}

		if (!hasWalkSteps()) {
			if (Math.random() > 0.59564) {
				int moveX = (int) Math.round(Math.random() * 10.0 - 5.0);
				int moveY = (int) Math.round(Math.random() * 10.0 - 5.0);
				addWalkSteps(getRespawnTile().getX() + moveX, getRespawnTile().getY() + moveY, 5, true);
			}
		}
	}

	public void nextStage() {
		stage++;
		final int[] TELEPORT_TILE = ORB_TELEPORT_LOCATIONS[stage];
		if (TELEPORT_TILE[0] == 0)
			return;
		Tile nextTile = getManager().getTile(reference, TELEPORT_TILE[0], TELEPORT_TILE[1]);
		setRespawnTile(nextTile);
		tele(nextTile);
	}
}
