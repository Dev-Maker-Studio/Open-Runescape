package com.rs.game.content.world.areas.burthorpe.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class Servant extends Conversation {
	private static final int npcId = 1081;


	public static NPCClickHandler Servant = new NPCClickHandler(new Object[]{npcId}, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Servant(e.getPlayer()));
		}
	});

	public Servant(Player player) {
		super(player);
		addPlayer(HeadE.HAPPY_TALKING, "Hi!");
		addNPC(npcId, HeadE.HAPPY_TALKING, "Hi!");
		addNPC(npcId,HeadE.SKEPTICAL_HEAD_SHAKE,"Look, I'd better not talk. I'll get in trouble.");
		addNPC(npcId, HeadE.CALM_TALK, "If you want someone to show you round the castle ask Eohric, the Head Servant.");
		create();
	}
}
