package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;

import java.util.Map;

public class NPCAnimationAction extends CutsceneAction {

	private final Animation anim;

	public NPCAnimationAction(String key, Animation anim, int actionDelay) {
		super(key, actionDelay);
		this.anim = anim;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		NPC npc = (NPC) objects.get(getObjectKey());
		npc.setNextAnimation(anim);
	}

}
