package com.rs.game.content.skills.dungeoneering.dialogues;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;

public class DungeonPartyStart extends Conversation {
	public DungeonPartyStart(Player player) {
		super(player);
		addSimple("You must be in a party to enter a dungeon.");
		addOptions("Would you like to start a dungeon?", (ops) -> {
			ops.add("Yes.", player.getDungManager()::formParty);
			ops.add("No.");
		});
		create();
	}
}
