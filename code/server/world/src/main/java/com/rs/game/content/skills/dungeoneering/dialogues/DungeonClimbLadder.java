package com.rs.game.content.skills.dungeoneering.dialogues;

import com.rs.engine.dialogue.Conversation;
import com.rs.game.content.skills.dungeoneering.DungeonController;
import com.rs.game.model.entity.player.Player;

public class DungeonClimbLadder extends Conversation {

	public DungeonClimbLadder(Player player, DungeonController controller) {
		super(player);
		addOptions("Are you sure you wish to proceed and take your party with you?", (ops) -> {
			ops.add("Yes.", controller::voteToMoveOn);
			ops.add("No.");
		});
	}
}
