package com.rs.game.content.world.areas.falador.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.utils.shop.ShopsHandler;

@PluginEventHandler
public class Cassie extends Conversation {
	private static final int npcId = 577;

	public static NPCClickHandler Cassie = new NPCClickHandler(new Object[]{ npcId }, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Cassie(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "cassies_shield_shop");
		}
	});

	public Cassie(Player player) {
		super(player);
		addNPC(npcId, HeadE.HAPPY_TALKING, "I buy and sell shields; do you want to trade?");
		addOptions(new Options() {
			@Override
			public void create() {

				option("Yes, please.", new Dialogue()
						.addNext(() -> ShopsHandler.openShop(player, "cassies_shield_shop")));
				option("No, thank you.", new Dialogue()
						.addPlayer(HeadE.CALM_TALK, "No, thank you.")
						.addNPC(npcId, HeadE.HAPPY_TALKING, "Okay, come back soon.")
				);

			}


		});
	}
}
