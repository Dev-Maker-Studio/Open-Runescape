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
public class Wayne extends Conversation {
	private static final int npcId = 581;

	public static NPCClickHandler Wayne = new NPCClickHandler(new Object[]{ npcId }, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Wayne(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "waynes_chains");
		}
	});

	public Wayne(Player player) {
		super(player);
		addNPC(npcId, HeadE.HAPPY_TALKING, "Welcome to Wayne's Chains. Do you wanna buy or sell some chainmail?");
		addOptions(new Options() {
			@Override
			public void create() {

				option("Yes, please.", new Dialogue()
						.addNext(() -> ShopsHandler.openShop(player, "waynes_chains")));
				option("No, thank you.", new Dialogue()
						.addPlayer(HeadE.CALM_TALK, "No, thank you.")
						.addNPC(npcId, HeadE.HAPPY_TALKING, "Okay, come back soon.")
				);

			}


		});
	}
}
