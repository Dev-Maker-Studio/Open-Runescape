package com.rs.game.content.skills.dungeoneering.dialogues;

import com.rs.engine.dialogue.Conversation;
import com.rs.game.content.skills.dungeoneering.DungeonController;
import com.rs.game.model.entity.player.Player;

public class DungeonLeave extends Conversation {

	public DungeonLeave(Player player, DungeonController controller) {
		super(player);
		addOptions("Leave the dungeon permanently?", (ops) -> {
			ops.add("Yes.", controller::leaveDungeonPermanently);
			ops.add("No.");
		});
	}
}
