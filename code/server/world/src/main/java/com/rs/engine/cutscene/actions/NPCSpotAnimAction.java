package com.rs.engine.cutscene.actions;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.SpotAnim;

import java.util.Map;

public class NPCSpotAnimAction extends CutsceneAction {

	private final SpotAnim gfx;

	public NPCSpotAnimAction(String key, SpotAnim gfx, int actionDelay) {
		super(key, actionDelay);
		this.gfx = gfx;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		NPC npc = (NPC) objects.get(getObjectKey());
		npc.setNextSpotAnim(gfx);
	}

}
