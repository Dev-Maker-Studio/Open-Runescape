package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class PlayerTransformAction extends CutsceneAction {

	private final int npcId;

	public PlayerTransformAction(int npcId, int actionDelay) {
		super(null, actionDelay);
		this.npcId = npcId;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		player.getAppearance().transformIntoNPC(npcId);
	}

}
