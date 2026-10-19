package com.rs.game.content.minigames.ectofuntus;

import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;

import static com.rs.game.content.minigames.ectofuntus.EctofuntusKt.*;

public class EctoBones extends PlayerAction {

	public EctoBones(int itemId) {
		this.itemId = itemId;
	}

	int itemId;
	int step = 0;

	@Override
	public boolean start(Player player) {
		step = 0;
		return true;
	}

	@Override
	public boolean process(Player player) {
		if (step == -1)
			return false;
		return true;
	}

	@Override
	public int processWithDelay(Player player) {
		switch (step) {
		case 0:
			if (!hopper(player, itemId))
				step = -1;
			else
				step++;
			return 2;
		case 1:
			if (!grinder(player))
				step = -1;
			else
				step++;
			return 2;
		case 2:
			if (!bin(player))
				step = -1;
			else
				step = 0;
			return 2;
		default:
			stop(player);
		}
		return 4;
	}

	@Override
	public void stop(Player player) {
	}
}
