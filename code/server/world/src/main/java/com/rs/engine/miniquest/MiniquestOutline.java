package com.rs.engine.miniquest;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.Utils;

import java.util.List;

public abstract class MiniquestOutline {
	public final Miniquest getMiniquest() {
		return getClass().getAnnotation(MiniquestHandler.class).miniquest();
	}
	public final int getCompletedStage() {
		return getClass().getAnnotation(MiniquestHandler.class).completedStage();
	}
	public abstract List<String> getJournalLines(Player player, int stage);
	public abstract void complete(Player player);
	public abstract void updateStage(Player player);

	public final String getStartLocationDescription() {
		return getClass().getAnnotation(MiniquestHandler.class).startText();
	}

	public final String getRequiredItemsString() {
		return getClass().getAnnotation(MiniquestHandler.class).itemsText();
	}

	public final String getCombatInformationString() {
		return getClass().getAnnotation(MiniquestHandler.class).combatText();
	}

	public final String getRewardsString() {
		return getClass().getAnnotation(MiniquestHandler.class).rewardsText();
	}

	public final void sendQuestCompleteInterface(Player player, int itemId) {
		int jingleNum = Utils.random(0, 4);
		if(jingleNum == 3)
			jingleNum = 318;
		else
			jingleNum+=152;
		player.jingle(jingleNum);

		player.getPackets().sendVarcString(359, getRewardsString());
		player.getInterfaceManager().sendInterface(1244);
		player.getPackets().setIFItem(1244, 24, itemId, 1);
		player.getPackets().setIFText(1244, 25, "You have completed "+getMiniquest().getName()+"!");
	}
}
