package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class DelayAction extends CutsceneAction {

	public DelayAction(int delay) {
		super(null, delay);
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
	}
}
