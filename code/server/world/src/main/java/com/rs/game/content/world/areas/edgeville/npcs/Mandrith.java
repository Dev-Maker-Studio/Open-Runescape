package com.rs.game.content.world.areas.edgeville.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.Options;
import com.rs.game.content.Statuettes;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class Mandrith extends Conversation {
	private static final int npcId = 6537;

	public static NPCClickHandler Mandrith = new NPCClickHandler(new Object[]{npcId}, e -> {
		switch (e.getOption()) {
		
		case "Talk-to" -> e.getPlayer().startConversation(new Mandrith(e.getPlayer()));
		}
	});

	public Mandrith(Player player) {
		super(player);
		addPlayer(HeadE.CONFUSED, "Who are you?");
		addNPC(npcId, HeadE.AMAZED, "Why, I'm Mandrith! Inspiration to combatants both mighty and puny!");
		addPlayer(HeadE.CONFUSED, "Okay...fair enough.");
		addOptions(new Options() {
			@Override
			public void create() {

				option("Can you exchange some Ancient Revenant Artefacts?", new Dialogue()
						.addNext(() -> Statuettes.exchangeStatuettes(player)));//TODO INV check
				option("Bye.", new Dialogue()
						.addPlayer(HeadE.HAPPY_TALKING, "Bye.")
				);
			}
		});
	}
}

