package com.rs.game.content.minigames.sorcgarden;

import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class SorceressGardenNPCs {
	
	public static NPCClickHandler handleNpcs = new NPCClickHandler(new Object[] { 5532, 5563 }, e -> {
		if (e.getNPCId() == 5563) {
			e.getPlayer().startConversation(new Dialogue()
				.addPlayer(HeadE.CHEERFUL, "Hey kitty!")
				.addNPC(5563, HeadE.CAT_SURPRISED, "Hiss!"));
		} else if (e.getNPCId() == 5532) {
			e.getPlayer().startConversation(new Dialogue()
					.addPlayer(HeadE.CONFUSED, "Hey apprentice, do you want to try out your teleport skills again?")
					.addNPC(5532, HeadE.CHEERFUL, "Okay, here goes - and remember, to return just drink from the fountain.")
					.addNext(() -> SorceressGardenController.teleportToSorceressGardenNPC(e.getNPC(), e.getPlayer())));
		}
	});

}
