package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;

import java.util.Map;

public class PlayerAnimationAction extends CutsceneAction {

	private final Animation anim;

	public PlayerAnimationAction(Animation anim, int actionDelay) {
		super(null, actionDelay);
		this.anim = anim;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		player.setNextAnimation(anim);
	}

}
