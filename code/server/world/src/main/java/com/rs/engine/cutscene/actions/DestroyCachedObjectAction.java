package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class DestroyCachedObjectAction extends CutsceneAction {

	public DestroyCachedObjectAction(String objectKey, int actionDelay) {
		super(objectKey, actionDelay);
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");
		scene.deleteObject(objects.get(getObjectKey()));
	}

}
