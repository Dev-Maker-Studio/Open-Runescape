package com.rs.game.content.world.areas.port_phasmatys.npcs;

import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.utils.shop.ShopsHandler;

@PluginEventHandler
public class GhostShopkeeper {
	private static final int npcId = 1699;

	public static NPCClickHandler ghostShopkeeper = new NPCClickHandler(new Object[] { npcId }, e -> {
		switch (e.getOption()) {
		case "Talk-to" -> {
			if (e.getPlayer().getEquipment().getNeckId() != 552) {
				e.getPlayer().startConversation(new Dialogue().addNPC(npcId, HeadE.FRUSTRATED, "Woooo wooo wooooo woooo"));
				e.getPlayer().sendMessage("You cannot understand the ghost.");
				return;
			}
				
			e.getPlayer().startConversation(new Dialogue()
					.addNPC(npcId, HeadE.SECRETIVE, "Would you like to buy or sell anything?")
					.addOptions(ops -> {
						ops.add("I'd like to see what you have for sale.", () -> ShopsHandler.openShop(e.getPlayer(), "port_phasmatys_general_store"));
						ops.add("No thanks.")
							.addPlayer(HeadE.CONFUSED, "No thanks.");
					}));
		}
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "port_phasmatys_general_store");
		}
	});
}

