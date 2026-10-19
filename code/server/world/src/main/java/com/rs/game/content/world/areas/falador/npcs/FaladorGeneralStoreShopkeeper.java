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
public class FaladorGeneralStoreShopkeeper extends Conversation {
	private static final int npcId = 526;

	public static NPCClickHandler FaladorGeneralStoreAssistant = new NPCClickHandler(new Object[]{ npcId }, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new FaladorGeneralStoreShopkeeper(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "falador_general_store");
		}
	});

	public FaladorGeneralStoreShopkeeper(Player player) {
		super(player);
		addNPC(npcId, HeadE.HAPPY_TALKING, "Can I help you at all?");
		addOptions(new Options() {
			@Override
			public void create() {

				option("Yes, please. What are you selling?", new Dialogue()
						.addNext(() -> ShopsHandler.openShop(player, "falador_general_store")));
				option("How should I use your shop?", new Dialogue()
						.addNPC(npcId, HeadE.HAPPY_TALKING, "I'm glad you ask! You can buy as many of the items stocked as you wish. You can also sell most items to the shop.")
						.addNext(() -> ShopsHandler.openShop(player, "falador_general_store")));
				option("I think I'll give it a miss.", new Dialogue()
						.addPlayer(HeadE.CALM_TALK, "I think I'll give it a miss.")
						.addNPC(npcId, HeadE.HAPPY_TALKING, "Okay, come back soon.")
				);

			}


		});
	}
}
