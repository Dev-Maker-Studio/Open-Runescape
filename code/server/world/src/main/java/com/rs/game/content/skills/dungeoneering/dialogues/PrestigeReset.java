package com.rs.game.content.skills.dungeoneering.dialogues;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;

public class PrestigeReset extends Conversation {
	public PrestigeReset(Player player) {
		super(player);
		addSimple("Are you sure you want to reset your dungeon progress? Your previous progress will be set to the number of floors you have completed and all floors will be marked as incomplete. This cannot be undone.");
		addOptions("Are you sure?", (ops) -> {
				ops.add("Yes, reset my progress.", player.getDungManager()::resetProgress);
				ops.add("No, don't reset my progress.");
		});
		create();
	}
}
