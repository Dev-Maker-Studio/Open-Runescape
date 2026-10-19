package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class PlayerMusicEffectAction extends CutsceneAction {

	private final int id;

	public PlayerMusicEffectAction(int id, int actionDelay) {
		super(null, actionDelay);
		this.id = id;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		player.jingle(id);
	}

}
