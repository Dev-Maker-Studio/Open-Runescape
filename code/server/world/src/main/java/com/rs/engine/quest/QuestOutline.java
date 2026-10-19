package com.rs.engine.quest;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.Utils;

import java.util.List;

public abstract class QuestOutline {
	public final Quest getQuest() {
		return getClass().getAnnotation(QuestHandler.class).quest();
	}

	public final int getCompletedStage() {
		return getClass().getAnnotation(QuestHandler.class).completedStage();
	};

	public abstract List<String> getJournalLines(Player player, int stage);
	public abstract void complete(Player player);

	public final String getStartLocationDescription() {
		return getClass().getAnnotation(QuestHandler.class).startText();
	}

	public final String getRequiredItemsString() {
		return getClass().getAnnotation(QuestHandler.class).itemsText();
	}

	public final String getCombatInformationString() {
		return getClass().getAnnotation(QuestHandler.class).combatText();
	}

	public final String getRewardsString() {
		return getQuest().getQuestPointRewardLine() + getClass().getAnnotation(QuestHandler.class).rewardsText();
	}

	/** Player-specific rewards shown in the quest info screen. Override when rewards differ per player. */
	public String getRewardsString(Player player) {
		return getRewardsString();
	}

	/** Player-specific combat info shown in the quest info screen. Override when it differs per player. */
	public String getCombatInformationString(Player player) {
		return getCombatInformationString();
	}

	public void updateStage(Player player, int stage) {

	}

	public final void sendQuestCompleteInterface(Player player, int itemId) {
		showQuestCompleteInterface(player, itemId, getRewardsString());
	}

	/**
	 * Same as {@link #sendQuestCompleteInterface(Player, int)} but shows the given reward lines instead of the
	 * annotation's static rewardsText, for quests whose rewards differ per player (e.g. Hazeel Cult sides).
	 * The quest point line is prepended automatically.
	 */
	public final void sendQuestCompleteInterface(Player player, int itemId, String rewardsText) {
		showQuestCompleteInterface(player, itemId, getQuest().getQuestPointRewardLine() + rewardsText);
	}

	private void showQuestCompleteInterface(Player player, int itemId, String rewardsString) {
		int jingleNum = Utils.random(0, 4);
		if(jingleNum == 3)
			jingleNum = 318;
		else
			jingleNum+=152;
		player.jingle(jingleNum);

		player.getPackets().sendVarcString(359, rewardsString);
		player.getInterfaceManager().sendInterface(1244);
		player.getPackets().setIFItem(1244, 24, itemId, 1);
		player.getPackets().setIFText(1244, 25, "You have completed "+getQuest().getDefs().name+"!");
		player.getPackets().setIFText(1244, 27, "Quest points: "+player.getQuestManager().getQuestPoints());
	}
}
