package com.rs.game.content.skills.dungeoneering.dialogues;
import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.Options;
import com.rs.game.model.entity.player.Player;

public class DungeonDifficulty extends Conversation {
	public DungeonDifficulty(Player player, int partySize) {
		super(player);
		addOptions("What difficulty of dungeon would you like?", (ops) -> {
			for(int i = 1; i <= partySize; i++) {
				final int size = i;
				ops.option(i + (i == partySize ? " (recommended)" : ""), new Dialogue().addNext(() -> {
					player.getDungManager().setDificulty(size);
					player.getDungManager().enterDungeon(true);
				}));
			}
		});
		create();
	}
}