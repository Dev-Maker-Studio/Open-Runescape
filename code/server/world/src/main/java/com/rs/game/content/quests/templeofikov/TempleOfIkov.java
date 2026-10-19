package com.rs.game.content.quests.templeofikov;

import com.rs.engine.quest.Quest;
import com.rs.engine.quest.QuestHandler;
import com.rs.engine.quest.QuestOutline;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.Skills;
import org.darkan.core.Constants;
import com.rs.plugin.annotations.PluginEventHandler;

import java.util.ArrayList;
import java.util.List;


@QuestHandler(
		quest = Quest.TEMPLE_OF_IKOV,
		startText = "Speak to Lucien in the Flying Horse Inn, in the north-west of East Ardougne.",
		itemsText = "Light source, 20 limpwurt roots.",
		combatText = "Be able to defeat a level 77 foe.",
		rewardsText = "10,500 Ranged XP<br>8,000 Fletching XP<brBoots of lightness<br>Access to the Temple of Ikov",
		completedStage = 2
)
@PluginEventHandler
public class TempleOfIkov extends QuestOutline {
	public final static int NOT_STARTED = 0;
	public final static int HELP_LUCIEN = 1;
	public final static int QUEST_COMPLETE = 2;

	@Override
	public List<String> getJournalLines(Player player, int stage) {
		ArrayList<String> lines = new ArrayList<>();
		switch (stage) {
		case NOT_STARTED -> {
			lines.add("A mysterious stranger called Lucien asks you to go on a mission");
			lines.add("deep under the Temple of Ikov in central Kandarin. He wants you");
			lines.add("to retrieve an artifact known as the Staff of Armadyl. Towards");
			lines.add("the end of the quest you are presented with a choice on how to");
			lines.add("complete the quest.");
			lines.add("");
			lines.add("You can start this quest by talking to Lucien in the north");
			lines.add("northwestern part of East Ardougne in The Flying Horse Inn.");
			lines.add("");
			lines.add("~~~Requirements~~~");
			lines.add((player.getSkills().getLevel(Constants.THIEVING) >= 42 ? "<str>" : "") + "42 Thieving");
			lines.add("");
		}
		case HELP_LUCIEN -> {
			lines.add("I must solve the puzzle of the Temple of Ikov dungeon to either");
			lines.add("help Lucien or go against him...");
			lines.add("");
		}
		case QUEST_COMPLETE -> {
			if(isLucienSide(player))
				lines.add("You chose to help Lucien...");
			if(!isLucienSide(player))
				lines.add("You chose to go against Lucien...");
			lines.add("");
			lines.add("QUEST COMPLETE!");
		}
		default -> lines.add("Invalid quest stage. Report this to an administrator.");
		}
		return lines;
	}

	public static void setIkovLucienSide(Player p, boolean isLucienSide) {
		p.set("IkovQuestHelpedLucien", isLucienSide);
	}

	public static boolean isLucienSide(Player p) {
		return p.getBool("IkovQuestHelpedLucien");
	}

	public static boolean meetsRequirements(Player p) {
		return p.getSkills().getLevel(Skills.THIEVING) >= 42;
	}

	@Override
	public void complete(Player player) {
		player.sendMessage("Congratulations! You have completed: 'Temple of Ikov'.");
		player.getSkills().addXpQuest(Constants.RANGE, 10_500);
		player.getSkills().addXpQuest(Constants.FLETCHING, 8_000);
		sendQuestCompleteInterface(player, 855);
	}

}
