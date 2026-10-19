package com.rs.game.content.world.areas.ape_atoll.npcs;

import org.darkan.core.cache.ItemDefinitions;
import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.utils.shop.ShopsHandler;

@PluginEventHandler
public class Ifaba extends Conversation {
	private static final int npcId = 1436;

	public static NPCClickHandler Ifaba = new NPCClickHandler(new Object[]{npcId}, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Ifaba(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "ifaba_general_store");
		}
	});

	public boolean MonkeyEquipped() {
		int neckId = player.getEquipment().getNeckId();
		int weaponId = player.getEquipment().getWeaponId();
		if (neckId == -1 || weaponId == -1)
			return false;
		return ItemDefinitions.getDefs(neckId).getName().contains("Monkeyspeak") ||
				ItemDefinitions.getDefs(weaponId).getName().contains("Greegree"); //Greegree needs testing
	};

	public Ifaba(Player player) {
		super(player);
		if (MonkeyEquipped())
		{
		addNPC(npcId, HeadE.SECRETIVE, "Would you like to buy or sell anything?");
		addOptions(new Options() {
			@Override
			public void create() {

				option("I'd like to see what you have for sale.", new Dialogue()
						.addNext(() -> ShopsHandler.openShop(player, "ifaba_general_store")));

				option("No thanks.", new Dialogue()
						.addPlayer(HeadE.CONFUSED, "No thanks.")
				);
			}


		});
	}
		else {
			addNPC(npcId,HeadE.FRUSTRATED,"Ook-ook! Eeek-aak-eek!");
			create();
			player.sendMessage("You don't understand Ifaba.");
		};
	}
}

