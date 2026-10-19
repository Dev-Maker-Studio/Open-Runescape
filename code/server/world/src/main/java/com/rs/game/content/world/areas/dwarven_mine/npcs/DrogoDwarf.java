package com.rs.game.content.world.areas.dwarven_mine.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.utils.shop.ShopsHandler;

@PluginEventHandler
public class DrogoDwarf extends Conversation {
	private static final int npcId = 579;

	public static NPCClickHandler DrogoDwarf = new NPCClickHandler(new Object[]{npcId}, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new DrogoDwarf(e.getPlayer()));
		case "Trade" -> ShopsHandler.openShop(e.getPlayer(), "drogos_mining_emporium");
		}
	});

	public DrogoDwarf(Player player) {
		super(player);
		addNPC(npcId, HeadE.CHEERFUL, "'Ello. Welcome to my Mining shop, friend.");
		addOptions(ops -> {
			ops.add("Do you want to trade?")
				.addPlayer(HeadE.CONFUSED, "Do you want to trade?")
				.addNext(() -> ShopsHandler.openShop(player, "drogos_mining_emporium"));
			
			ops.add("Hello, shorty.")
				.addPlayer(HeadE.CHEERFUL, "Hello, shorty.")
				.addNPC(npcId, HeadE.ANGRY, "I may be short but at least I've got manners.");
			
			ops.add("Why don't you ever restock ores and bars?")
				.addPlayer(HeadE.CONFUSED, "Why don't you ever restock ores and bars?")
				.addNPC(npcId, HeadE.CHEERFUL, "The only ores and bars I sell are those sold to me.");
		});
	}
}
