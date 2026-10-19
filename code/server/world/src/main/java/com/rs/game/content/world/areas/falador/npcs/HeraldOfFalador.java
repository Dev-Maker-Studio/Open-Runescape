package com.rs.game.content.world.areas.falador.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class HeraldOfFalador extends Conversation {
	private static final int npcId = 13939;

	public static NPCClickHandler IkisKrum = new NPCClickHandler(new Object[]{ npcId }, e -> {
		switch (e.getOption()) {
			
			case "Talk-to" -> e.getPlayer().startConversation(new HeraldOfFalador(e.getPlayer()));
		}
	});

	public HeraldOfFalador(Player player) {
		super(player);
		addNPC(npcId, HeadE.HAPPY_TALKING, "A fine day to be visiting Falador is it not? Welcome, ally!");
		addPlayer(HeadE.HAPPY_TALKING, "Hello to you as well! What do you do here?");
		addNPC(npcId, HeadE.HAPPY_TALKING, "As the Falador herald, I've been put in command of keeping Falador a strong and mighty region within Gielinor.");
		addNPC(npcId, HeadE.FRUSTRATED, "My fighting days are behind me, but I can still conquer the hearts of the people with marching parades and our herald capes.");
	}
}
