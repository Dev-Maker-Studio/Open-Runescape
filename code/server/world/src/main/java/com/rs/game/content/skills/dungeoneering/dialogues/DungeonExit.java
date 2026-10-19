package com.rs.game.content.skills.dungeoneering.dialogues;

import com.rs.engine.dialogue.Conversation;
import com.rs.game.content.skills.dungeoneering.DungeonController;
import com.rs.game.model.entity.player.Player;

public class DungeonExit extends Conversation {

	public DungeonExit(Player player, DungeonController controller) {
		super(player);
		addSimple("This ladder leads back to the surface. You will not be able to come back to this dungeon if you leave.");
		addOptions("Leave the dungeon and return to the surface?", (ops) -> {
			ops.add("Yes.", controller::leaveDungeon);
			ops.add("No.");
		});
	}
}
