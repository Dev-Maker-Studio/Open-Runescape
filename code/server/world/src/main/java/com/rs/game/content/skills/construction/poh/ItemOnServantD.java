package com.rs.game.content.skills.construction.poh;

import org.darkan.core.cache.ItemDefinitions;
import world.gregs.voidps.cache.definition.data.ItemDefinition;
import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.content.skills.construction.SawmillOperator;
import com.rs.game.content.skills.construction.poh.ServantNPC.RequestType;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;

public class ItemOnServantD extends Conversation {

	public ItemOnServantD(Player player, NPC npc, int item, boolean isSawmill) {
		super(player);
		if (!(npc instanceof ServantNPC servant))
			return;
		boolean proceed = false;
		for (int bankable : HouseConstants.BANKABLE_ITEMS) {
			if (item == bankable) {
				proceed = true;
				break;
			}
		}
		ItemDefinition definition = ItemDefinitions.getDefs(item);
		final int[] plank = SawmillOperator.getPlankForLog(item);
		if (plank != null || definition.isNoted())
			proceed = true;
		if (!proceed)
			return;
		int paymentStage = player.getHouse().getPaymentStage();
		if (paymentStage >= 10) {
			addNPC(servant.getId(), servant.getServantData() == HouseServants.DEMON_BUTLER ? HeadE.CAT_CALM_TALK2 : HeadE.CALM_TALK, "Excuse me, but before I can continue working you must pay my fee.");
			return;
		}
		String name = definition.getName().toLowerCase();

		if (definition.isNoted()) {
			addOptions("Un-cert this item?", ops -> {
				ops.add("Un-cert " + name + ".", () -> setFetchAttributes(RequestType.UNNOTE, item, "How many would you like to un-note?"));
				ops.add("Fetch another " + name + ".", () -> setFetchAttributes(RequestType.WITHDRAW, item, "How many would you like to retrieve?"));
				ops.add("Bank", () -> setFetchAttributes(RequestType.DEPOSIT, item, "How many would you like to bank?"));
				ops.add("Cancel");
			});
		} else if (isSawmill && plank != null) {
			addOptions("Take this to the sawmill?", ops -> {
				ops.add("Take it to the sawmill.", () -> setFetchAttributes(RequestType.SAWMILL, item, "How many would you like to create?"));
				ops.add("Bank", () -> setFetchAttributes(RequestType.DEPOSIT, item, "How many would you like to bank?"));
				ops.add("Cancel");
			});
		} else {
			addOptions("Take this item to the bank?", ops -> {
				ops.add("Fetch another " + name + ".", () -> setFetchAttributes(RequestType.WITHDRAW, item, "How many would you like to retrieve?"));
				ops.add("Bank", () -> setFetchAttributes(RequestType.DEPOSIT, item, "How many would you like to bank?"));
				ops.add("Cancel");
			});
		}
		create();
	}

	private void setFetchAttributes(RequestType type, int item, String title) {
		player.sendInputInteger(title, amount -> {
			if (!player.getHouse().isLoaded() || !player.getHouse().getPlayers().contains(player))
				return;
			player.getHouse().getServantInstance().requestType(item, amount, type);
		});
	}
}
