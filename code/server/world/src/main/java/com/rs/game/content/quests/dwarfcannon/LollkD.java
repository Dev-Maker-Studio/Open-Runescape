package com.rs.game.content.quests.dwarfcannon;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.quest.Quest;
import com.rs.game.model.entity.player.Player;

public class LollkD extends Conversation {

	private static final int Lollk = 207;

	public LollkD(Player player) {
		super(player);
		int currentStage = player.getQuestManager().getStage(Quest.DWARF_CANNON);
		switch(currentStage) {
		case 5:
			addNPC(Lollk, HeadE.HAPPY_TALKING, "Thank the heavens, you saved me! I thought I'd be goblin lunch for sure!");
			addPlayer(HeadE.NO_EXPRESSION, "Are you okay?");
			addNPC(Lollk, HeadE.HAPPY_TALKING, "I think so, I'd better run off home.");
			addPlayer(HeadE.NO_EXPRESSION, "That's right, you get going. I'll catch up.");
			addNPC(Lollk, HeadE.HAPPY_TALKING, "Thanks again, brave adventurer.", () -> {
				player.sendMessage("The dwarf child runs off into the caverns.");
				player.getQuestManager().setStage(Quest.DWARF_CANNON, 6);
			});
			break;
		default:
			addPlayer(HeadE.NERVOUS, "Hello.");
			addNPC(Lollk, HeadE.HAPPY_TALKING, "Hello.");
		}
		create();
	}
}
