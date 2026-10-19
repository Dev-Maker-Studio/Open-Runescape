package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.SpotAnim;

import java.util.Map;

public class PlayerGraphicAction extends CutsceneAction {

	private final SpotAnim gfx;

	public PlayerGraphicAction(SpotAnim gfx, int actionDelay) {
		super(null, actionDelay);
		this.gfx = gfx;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		player.setNextSpotAnim(gfx);
	}

}
