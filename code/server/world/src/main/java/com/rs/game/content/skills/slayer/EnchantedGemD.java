package com.rs.game.content.skills.slayer;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Utils;

public class EnchantedGemD extends Conversation {
	public EnchantedGemD(Player player, Master master) {
		super(player);
		int NPC = master.npcId;
		addNPC(NPC, HeadE.CALM_TALK, "Hello there, "+ player.getDisplayName() + ", what can I help you with?");
		addOptions("Choose an option:", new Options() {
			@Override
			public void create() {
				option("How am I doing so far?", new Dialogue()
						.addPlayer(HeadE.HAPPY_TALKING, "How am I doing so far?")
						.addNPC(NPC, HeadE.CALM_TALK, player.getSlayer().getTaskString())
				);
				option("Who are you?", new Dialogue()
						.addPlayer(HeadE.HAPPY_TALKING, "Who are you?")
						.addNPC(NPC, HeadE.CALM_TALK, "My name is "+Utils.formatPlayerNameForDisplay(Master.getMasterForId(NPC).name().toLowerCase())+"; I'm a Slayer Master.")
				);
				option("Where are you?", new Dialogue()
						.addPlayer(HeadE.HAPPY_TALKING, "Where are you?")
						.addNPC(NPC, HeadE.CALM_TALK, "I am talking to you through a spectral dimension and don't want to reveal my location.")
						.addPlayer(HeadE.HAPPY_TALKING, "Is that so?")
						.addNPC(NPC, HeadE.CALM_TALK, "Yes.")
				);
			}
		});
		create();
	}
}
