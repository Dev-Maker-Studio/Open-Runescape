package com.rs.game.content.quests.waterfallquest;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.Options;
import com.rs.engine.quest.Quest;
import com.rs.game.model.entity.player.Player;

public class AlmeraD extends Conversation {
	public AlmeraD(Player player) {
		super(player);
		int NPC = 304;
		addOptions("Choose an option:", new Options() {
			@Override
			public void create() {
				option("How is life on the waterfall?", new Dialogue()
						.addPlayer(HeadE.HAPPY_TALKING, "How is life on the waterfall?")
						.addNPC(NPC, HeadE.CALM_TALK, "I am worried about my son, but other than that everything is fine.")
				);
				if (!player.isQuestStarted(Quest.WATERFALL_QUEST))
					option("I am looking for a quest.", new Dialogue()
							.addPlayer(HeadE.HAPPY_TALKING, "I am looking for a quest.")
							.addNPC(NPC, HeadE.CALM_TALK, "I might have one for you. My son Hudon has gone missing on some hunt for treasure in the waterfall.")
							.addNPC(NPC, HeadE.CALM_TALK, "Could you please go make sure he is alright for me?")
							.addQuestStart(Quest.WATERFALL_QUEST)
							.addOptions("Choose an option:", new Options() {
								@Override
								public void create() {
									option("Of course.", new Dialogue()
											.addPlayer(HeadE.HAPPY_TALKING, "Of course I will.", () -> player.getQuestManager().setStage(Quest.WATERFALL_QUEST, 1))
											.addNPC(NPC, HeadE.CALM_TALK, "Thank you so much!")
									);
									option("No thanks, I don't have time right now.", new Dialogue()
											.addPlayer(HeadE.HAPPY_TALKING, "Sorry, but I don't have time right now. Bye.")
									);
								}
							})
					);
			}
		});
	}
}
