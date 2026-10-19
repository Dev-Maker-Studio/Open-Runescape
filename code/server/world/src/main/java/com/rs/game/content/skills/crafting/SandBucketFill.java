package com.rs.game.content.skills.crafting;

import com.rs.game.content.minigames.ectofuntus.Ectofuntus;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnObjectHandler;

@PluginEventHandler
public class SandBucketFill extends PlayerAction {

	public SandBucketFill() {
	}

	@Override
	public boolean start(Player player) {
		return true;
	}

	@Override
	public boolean process(Player player) {
		if (player.getInventory().containsItem(Ectofuntus.EMPTY_BUCKET, 1))
			return true;
		return false;
	}

	@Override
	public int processWithDelay(Player player) {
		if (fillBucket(player))
			return 1;
		return 1;
	}

	@Override
	public void stop(Player player) {

	}

	public boolean fillBucket(Player player) {
		if (player.getInventory().containsItem(Ectofuntus.EMPTY_BUCKET, 1)) {
			player.anim(4471);
			player.getInventory().deleteItem(Ectofuntus.EMPTY_BUCKET, 1);
			player.getInventory().addItem(1783, 1);
			return true;
		}
		return false;
	}

	public static ItemOnObjectHandler handleBucketsOnSand = new ItemOnObjectHandler(new Object[] { "Sand", "Sand pit", "Sandpit", "Sand pile" }, new Object[] { 1925 }, e -> e.getPlayer().getActionManager().setAction(new SandBucketFill()));
}
