package com.rs.game.content.skills.cooking;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.statements.MakeXStatement;
import com.rs.game.content.skills.cooking.FruitCutting.CuttableFruit;
import com.rs.game.model.entity.player.Player;

public class FruitCuttingD extends Conversation {

	public FruitCuttingD(Player player, CuttableFruit fruit) {
		super(player);

		Dialogue makeX = addNext(new MakeXStatement("Choose how many you wish to cut,<br>then click on the item to begin.", fruit.getProductIds(), player.getInventory().getItems().getNumberOf(fruit.getFruitId())));

		for (int i = 0;i < fruit.getProductIds().length;i++) {
			final int option = i;
			makeX.addNext(() -> {
				int quantity = MakeXStatement.getQuantity(player);
				int invQuantity = player.getInventory().getItems().getNumberOf(fruit.getFruitId());
				if (quantity > invQuantity)
					quantity = invQuantity;
				player.getActionManager().setAction(new FruitCutting(fruit, option, quantity));
			});
		}

		create();
	}
}
