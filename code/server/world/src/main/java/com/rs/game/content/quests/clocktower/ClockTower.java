package com.rs.game.content.quests.clocktower;

import com.rs.engine.quest.Quest;
import com.rs.engine.quest.QuestHandler;
import com.rs.engine.quest.QuestOutline;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import com.rs.plugin.annotations.PluginEventHandler;

import java.util.ArrayList;
import java.util.List;


@QuestHandler(
		quest = Quest.CLOCK_TOWER,
		startText = "Speak to Brother Kojo in the Clock Tower south of Ardougne.",
		itemsText = "Bucket of water or ice gloves",
		combatText = "None.",
		rewardsText = "500 coins",
		completedStage = ClockTower.QUEST_COMPLETE
)
@PluginEventHandler
public class ClockTower extends QuestOutline {
	public final static int NOT_STARTED = 0;
	public final static int REPAIR_CLOCK_TOWER = 1;
	public final static int QUEST_COMPLETE = 2;

	@Override
	public List<String> getJournalLines(Player player, int stage) {
		ArrayList<String> lines = new ArrayList<>();
		switch (stage) {
			case NOT_STARTED -> {
				lines.add("Help the confused Brother Kojo find the missing cogs and fix his");
				lines.add("Clock Tower. Search the dungeon using brawn and brains to correctly");
				lines.add("place the four cogs.");
				lines.add("");
				lines.add("Talk to Brother Kojo at the Clock Tower, south of Ardougne, to start");
				lines.add("this quest.");
				lines.add("");
			}
			case REPAIR_CLOCK_TOWER -> {
				lines.add("I must find all 4 cogs to fix the clocktower. Here are the spindles I");
				lines.add("have fixed:");
				for(int id = 20; id <= 23; id++) {
					String line = "-" + new Item(id, 1).getName();
					if (player.getQuestManager().getAttribs(Quest.CLOCK_TOWER).getB(new Item(id, 1).getName() + "Done"))
						line = "<str>" + line;
					lines.add(line);
				}
				lines.add("");
			}
			case QUEST_COMPLETE -> {
				lines.add("");
				lines.add("");
				lines.add("QUEST COMPLETE!");
			}
			default -> lines.add("Invalid quest stage. Report this to an administrator.");
		}
		return lines;
	}

	public static boolean allCogsFinished(Player p) {
		for(int id = 20; id <= 23; id++)
			if(!p.getQuestManager().getAttribs(Quest.CLOCK_TOWER).getB(new Item(id, 1).getName() + "Done"))
				return false;
		return true;
	}

	@Override
	public void complete(Player player) {
		player.sendMessage("Congratulations! You have completed: 'Clock Tower'.");
		player.getInventory().addCoins(500);
		sendQuestCompleteInterface(player, 6964);
	}
}
