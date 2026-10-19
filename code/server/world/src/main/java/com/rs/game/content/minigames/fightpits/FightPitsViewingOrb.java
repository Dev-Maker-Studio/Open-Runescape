package com.rs.game.content.minigames.fightpits;

import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import world.gregs.voidps.type.Tile;

public class FightPitsViewingOrb extends PlayerAction {

	public static final Tile[] ORB_TELEPORTS = { Tile.of(4571, 5092, 0), Tile.of(4571, 5107, 0), Tile.of(4590, 5092, 0), Tile.of(4571, 5077, 0), Tile.of(4557, 5092, 0) };

	private Tile tile;

	@Override
	public boolean start(Player player) {
		if (!process(player))
			return false;
		tile = Tile.of(player.getTile());
		player.getAppearance().setHidden(true);
		player.getPackets().setBlockMinimapState(5);
		player.tele(ORB_TELEPORTS[0]);
		player.getInterfaceManager().sendInventoryInterface(374);
		return true;
	}

	@Override
	public boolean process(Player player) {
		if (player.getPoison().isPoisoned()) {
			player.sendMessage("You can't use orb while you're poisoned.");
			return false;
		}
		if (player.getFamiliar() != null) {
			player.sendMessage("You can't use orb with a familiar.");
			return false;
		}
		return true;
	}

	@Override
	public int processWithDelay(Player player) {
		return 0;
	}

	@Override
	public void stop(final Player player) {
		player.lock(2);
		player.getInterfaceManager().removeInventoryInterface();
		player.getAppearance().setHidden(false);
		player.getPackets().setBlockMinimapState(0);
		player.tele(tile);
	}

}
