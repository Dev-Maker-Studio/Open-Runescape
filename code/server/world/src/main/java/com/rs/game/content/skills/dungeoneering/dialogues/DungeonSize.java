package com.rs.game.content.skills.dungeoneering.dialogues;

import com.rs.engine.dialogue.Conversation;
import com.rs.game.content.skills.dungeoneering.DungeonConstants;
import com.rs.game.content.skills.dungeoneering.DungeonPartyManager;
import com.rs.game.model.entity.player.Player;

public class DungeonSize extends Conversation {
	public DungeonSize(Player player) {
		super(player);
		DungeonPartyManager party = player.getDungManager().getParty();
		addSimple("What size of dungeon would you like?");
		addOptions("Would you like to start a dungeon?", (ops) -> {
			ops.add("Small.", () -> {
				if(party != null) {
					player.getDungManager().setSize(DungeonConstants.Size.Small);
					player.getDungManager().enterDungeon(false);
				}
			});
			ops.add("Medium.", () -> {
				if(party != null) {
					player.getDungManager().setSize(DungeonConstants.Size.Medium);
					player.getDungManager().enterDungeon(false);
				}
			});
			ops.add("Large.", () -> {
				if(party != null) {
					player.getDungManager().setSize(DungeonConstants.Size.Large);
					player.getDungManager().enterDungeon(false);
				}
			});
		});
		create();
	}
}
