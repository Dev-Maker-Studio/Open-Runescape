package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;

import java.util.Map;

public class NPCForceTalkAction extends CutsceneAction {

	private final String text;

	public NPCForceTalkAction(String key, String text, int actionDelay) {
		super(key, actionDelay);
		this.text = text;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		NPC npc = (NPC) objects.get(getObjectKey());
		npc.setNextForceTalk(new ForceTalk(text));
	}

}
