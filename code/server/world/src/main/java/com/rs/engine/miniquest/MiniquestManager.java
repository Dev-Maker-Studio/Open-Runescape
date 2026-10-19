package com.rs.engine.miniquest;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.AttribMap;
import com.rs.plugin.annotations.PluginEventHandler;

import java.util.HashMap;
import java.util.Map;

@PluginEventHandler
public class MiniquestManager {
	private transient Player player;

	private final Map<Miniquest, Integer> questStages;
	private final Map<Miniquest, AttribMap> questAttribs;

	public MiniquestManager() {
		questStages = new HashMap<>();
		questAttribs = new HashMap<>();
	}

	public int getStage(Miniquest quest) {
		if (questStages.get(quest) == null)
			return 0;
		return questStages.get(quest);
	}

	public void setStage(Miniquest quest, int stage) {
		setStage(quest, stage, true);
	}

	public void setStage(Miniquest quest, int stage, boolean updateJournal) {
		if (!quest.isImplemented())
			return;
		questStages.put(quest, stage);
		if (updateJournal)
			sendStage(quest);
	}

	public void complete(Miniquest quest) {
		if (!quest.isImplemented())
			return;
		if (!isComplete(quest)) {
			setStage(quest, quest.getHandler().getCompletedStage());
			clearQuestAttributes(quest);
			quest.getHandler().complete(player);
			sendStage(quest);
		}
	}

	public void reset(Miniquest quest) {
		if (!quest.isImplemented())
			return;
		clearQuestAttributes(quest);
		setStage(quest, 0);
	}

	private void clearQuestAttributes(Miniquest quest) {
		questAttribs.remove(quest);
	}

	public AttribMap getAttribs(Miniquest quest) {
        AttribMap map = questAttribs.computeIfAbsent(quest, k -> new AttribMap());
        return map;
	}

	public boolean completedAll() {
		for (Miniquest quest : Miniquest.values()) {
			if (!quest.isImplemented())
				continue;
			if (!isComplete(quest))
				return false;
		}
		return true;
	}

	public boolean isComplete(Miniquest quest, String actionForUnimplemented, boolean outputReqs) {
		if (!quest.isImplemented())
			return quest.meetsReqs(player, actionForUnimplemented, outputReqs);
		if (getStage(quest) == quest.getHandler().getCompletedStage())
			return true;
		if (actionForUnimplemented != null)
			if (outputReqs)
			player.sendMessage("You must have completed " + quest.getName() + " " + actionForUnimplemented);
		return false;
	}

	public boolean isComplete(Miniquest quest) {
		return isComplete(quest, null, false);
	}

	public boolean isComplete(Miniquest quest, boolean outputReqs) {
		return isComplete(quest, null, outputReqs);
	}

	public void updateAllStages() {
		for (Miniquest quest : Miniquest.values())
			sendStage(quest);
	}

	public void sendStage(Miniquest quest) {
		if (quest.getHandler() != null)
			quest.getHandler().updateStage(player);
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}
}
