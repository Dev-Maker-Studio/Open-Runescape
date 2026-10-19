package com.rs.game.content.world.areas.falador.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class IkisKrum extends Conversation {
	private static final int npcId = 12237;

	public static NPCClickHandler IkisKrum = new NPCClickHandler(new Object[]{ npcId }, e -> {
		switch (e.getOption()) {
			
			case "Talk-to" -> e.getPlayer().startConversation(new IkisKrum(e.getPlayer()));
		}
	});

	public IkisKrum(Player player) {
		super(player);
		addPlayer(HeadE.HAPPY_TALKING, "Hello.");
		addNPC(npcId, HeadE.HAPPY_TALKING, "Good day, " + player.getPronoun("sir", "madam") + ". What brings you to this end of town?");
		addPlayer(HeadE.HAPPY_TALKING, "Well, what is there to do around here?");
		addNPC(npcId, HeadE.HAPPY_TALKING, "If you're into Mining, plenty! The dwarves have one of the largest mines in the world just under our feet. There's an entrance in the building just north-east of my house.");
		addNPC(npcId, HeadE.FRUSTRATED, "If you're one of these young, loud, hipster sorts you could visit the Party Room, just north of here. That blasted Pete parties all night and I never get any sleep!");
		addPlayer(HeadE.HAPPY_TALKING, "Thanks.");
	}
}
