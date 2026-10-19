package com.rs.game.content.skills.cooking;

import com.rs.game.content.world.areas.lumbridge.npcs.GrilleGoatsDialogue;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;

public class CowMilkingAction extends PlayerAction {

	public static final int EMPTY_BUCKET = 1925;

	public CowMilkingAction() {

	}

	@Override
	public boolean start(Player player) {
		if (!player.getInventory().containsItem(EMPTY_BUCKET, 1)) {
			player.startConversation(new GrilleGoatsDialogue(player));
			return false;
		}
		return true;
	}

	@Override
	public boolean process(Player player) {
		return player.getInventory().hasFreeSlots() && player.getInventory().containsItem(EMPTY_BUCKET, 1);
	}

	@Override
	public int processWithDelay(Player player) {
		player.setNextAnimation(new Animation(2305));
		player.soundEffect(372, true);
		player.getInventory().deleteItem(new Item(EMPTY_BUCKET, 1));
		player.getInventory().addItem(new Item(1927));
		player.sendMessage("You milk the cow.");
		return 5;
	}

	@Override
	public void stop(Player player) {
		setActionDelay(player, 3);
	}

}
