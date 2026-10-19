package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class PlayerFaceEntityAction extends CutsceneAction {

	public PlayerFaceEntityAction(String key, int actionDelay) {
		super(key, actionDelay);
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		NPC npc = (NPC) objects.get(getObjectKey());
		player.faceEntityTile(npc);
	}

}
