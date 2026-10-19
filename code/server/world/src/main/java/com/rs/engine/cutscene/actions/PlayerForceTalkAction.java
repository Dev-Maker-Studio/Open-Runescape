package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class PlayerForceTalkAction extends CutsceneAction {

	private final String text;

	public PlayerForceTalkAction(String text, int actionDelay) {
		super(null, actionDelay);
		this.text = text;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		player.setNextForceTalk(new ForceTalk(text));
	}

}
