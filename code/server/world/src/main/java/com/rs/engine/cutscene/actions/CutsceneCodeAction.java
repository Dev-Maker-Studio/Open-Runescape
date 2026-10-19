package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class CutsceneCodeAction extends CutsceneAction {

	private final Runnable runnable;

	public CutsceneCodeAction(Runnable runnable, int actionDelay) {
		super(null, actionDelay);
		this.runnable = runnable;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		runnable.run();
	}

}
