package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;

import java.util.Map;

public abstract class CutsceneAction {
	private final String objectKey;
	private final int delay;
	
	public CutsceneAction(String objectKey, int delay) {
		this.objectKey = objectKey;
		this.delay = delay;
	}
	
	public abstract void process(Player player, Map<String, Object> objects);

	public String getObjectKey() {
		return objectKey;
	}

	public int getDelay() {
		return delay;
	}
}
