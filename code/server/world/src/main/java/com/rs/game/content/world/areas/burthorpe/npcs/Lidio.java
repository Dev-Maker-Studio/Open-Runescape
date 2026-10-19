package com.rs.game.content.world.areas.burthorpe.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.utils.shop.ShopsHandler;

@PluginEventHandler
public class Lidio extends Conversation {
	private static final int npcId = 4293;

	public static NPCClickHandler Lidio = new NPCClickHandler(new Object[]{npcId}, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Lidio(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "warrior_guild_food_shop");
		}
	});

	public Lidio(Player player) {
		super(player);
		addNPC(npcId, HeadE.HAPPY_TALKING, "Greetings, warrior, how can I fill your stomach today?");
		addOptions(new Options() {
			@Override
			public void create() {

				option("With food preferably.", new Dialogue()
						.addNext(() -> ShopsHandler.openShop(player, "warrior_guild_food_shop")));

				option("I think I'll give it a miss.", new Dialogue()
						.addPlayer(HeadE.CALM_TALK, "I think I'll give it a miss.")
						.addNPC(npcId, HeadE.HAPPY_TALKING, "Okay, come back soon!")
				);

			}


		});
	}
}
