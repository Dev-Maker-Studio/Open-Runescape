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
public class Herquin extends Conversation {
	private static final int npcId = 584;

	public static NPCClickHandler Herquin = new NPCClickHandler(new Object[]{ npcId }, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Herquin(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "herquins_gems");
		}
	});

	public Herquin(Player player) {
		super(player);
		addNPC(npcId, HeadE.HAPPY_TALKING, "Do you wish to trade?");
		addOptions(new Options() {
			@Override
			public void create() {

				option("Yes, please.", new Dialogue()
						.addNext(() -> ShopsHandler.openShop(player, "herquins_gems")));
				option("Sorry, I don't want to talk to you, actually.", new Dialogue()
						.addPlayer(HeadE.CALM_TALK, "Sorry, I don't want to talk to you, actually.")
						.addNPC(npcId, HeadE.SHAKING_HEAD, "Huh, charming.")
				);

			}


		});
	}
}
