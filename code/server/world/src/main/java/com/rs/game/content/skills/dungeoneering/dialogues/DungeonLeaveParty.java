package com.rs.game.content.skills.dungeoneering.dialogues;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;

public class DungeonLeaveParty extends Conversation {
	public DungeonLeaveParty(Player player) {
		super(player);
		addSimple("Warning: If you leave the dungeon, you will not be able to return to it!");
		addOptions("Leave the dungeon for good?", (ops) -> {
			ops.add("Yes.", player.getDungManager()::leaveParty);
			ops.add("No.", new Dialogue());
		});
		create();
	}
}
