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
public class Anton extends Conversation {
	private static final int npcId = 4295;


	public static NPCClickHandler Anton = new NPCClickHandler(new Object[]{npcId}, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Anton(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "warrior_guild_armory"); //TODO Guild Armoury: Stabbing Weapons / Slashing Weapons / Crushing Weapons / Armour separation
		}
	});

	public Anton(Player player) {
		super(player);
		addNPC(npcId, HeadE.HAPPY_TALKING, "Ahhh, hello there. How can I help you?");
		addPlayer(HeadE.CALM_TALK,"Looks like you have a good selection of weapons around here...");
		addNPC(npcId, HeadE.CALM_TALK, "Indeed so, specially imported from the finest smiths around the lands, take a look at my wares.");
		addOptions(new Options() {
			@Override
			public void create() {

				option("Okay, show me what you have.", new Dialogue()
						.addNext(() -> ShopsHandler.openShop(player, "warrior_guild_armory")));

				option("I think I'll give it a miss.", new Dialogue()
						.addPlayer(HeadE.CALM_TALK, "I think I'll give it a miss.")
						.addNPC(npcId, HeadE.HAPPY_TALKING, "Okay, come back soon.")
				);

			}


		});
	}
}
