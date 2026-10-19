package com.rs.game.content.skills.farming;

import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import org.darkan.core.model.Item;

public class FillCompostBin extends PlayerAction {

	private final FarmPatch patch;
	private final Item item;
	private int compostType;

	public FillCompostBin(FarmPatch patch, Item item) {
		this.patch = patch;
		this.item = item;
		for (int reg : FarmPatch.COMPOST_ORGANIC)
			if (reg == item.getId())
				compostType = 1;
		for (int reg : FarmPatch.SUPER_COMPOST_ORGANIC)
			if (reg == item.getId())
				compostType = 2;
	}

	@Override
	public boolean start(Player player) {
		if (compostType == -1) {
			player.sendMessage("You can't compost that.");
			return false;
		}
		if (patch.seed != null) {
			player.sendMessage("There's already some " + patch.seed.name().toLowerCase() + " decomposing in there.");
			return false;
		}
		return true;
	}

	@Override
	public boolean process(Player player) {
		if ((patch.lives <= -15) || !player.getInventory().containsItem(item.getId()))
			return false;
		return true;
	}

	@Override
	public int processWithDelay(Player player) {
		if ((patch.lives <= -15) || !player.getInventory().containsItem(item.getId()))
			return -1;
		switch(compostType) {
		case -1:
			player.sendMessage("You can't compost that.");
			return -1;
		case 1:
			patch.compostLevel = 1;
			break;
		case 2:
			if (patch.compostLevel == 1)
				patch.compostLevel = 1;
			else
				patch.compostLevel = 2;
			break;
		}
		player.setNextAnimation(FarmPatch.FILL_COMPOST_ANIMATION);
		patch.lives--;
		patch.updateVars(player);
		player.getInventory().deleteItem(item.getId(), 1);
		return 1;
	}

	@Override
	public void stop(Player player) {

	}

}
