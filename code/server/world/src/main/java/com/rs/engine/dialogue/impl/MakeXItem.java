package com.rs.engine.dialogue.impl;

import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.statements.MakeXStatement;
import com.rs.game.content.skills.util.CreateAction;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;

public class MakeXItem extends Dialogue {

	private final int itemId;

	public MakeXItem(Player player, Item[] materials, Item[] products, double xp, int anim, int req, int skill, int delay) {
		itemId = products[0].getId();
		setFunc(() -> {
			int quantity = MakeXStatement.getQuantity(player);
			for (Item mat : materials) {
				int newQ = player.getInventory().getNumberOf(mat.getId()) / mat.getAmount();
				if (newQ < quantity)
					quantity = newQ;
			}
			player.getActionManager().setAction(new CreateAction(new Item[][] { materials }, new Item[][] { products }, new double [] { xp }, new int[] { anim },  new int[] { req }, skill, delay, 0).setQuantity(quantity));
		});
	}

	public MakeXItem(Player player, Item material, Item product, double xp, int anim, int req, int skill, int delay) {
		this(player, new Item[] { material }, new Item[] { product }, xp, anim, req, skill, delay);
	}

	public int getItemId() {
		return itemId;
	}
}
