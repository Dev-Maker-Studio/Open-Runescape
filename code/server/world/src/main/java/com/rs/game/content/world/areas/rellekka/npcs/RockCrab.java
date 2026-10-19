package com.rs.game.content.world.areas.rellekka.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class RockCrab extends NPC {

	private final int realId;

	public RockCrab(int id, Tile tile, boolean spawned) {
		super(id, tile, spawned);
		realId = id;
		setForceAgressive(true); // to ignore combat lvl
	}

	@Override
	public void setCombatTarget(Entity entity) {
		if (realId == getId()) {
			transformIntoNPC(realId - 1);
			setHitpoints(getMaxHitpoints());
		}
		super.setCombatTarget(entity);
	}

	@Override
	public void reset() {
		setNPC(realId);
		super.reset();
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 1266, 1268, 2453, 2886 }, (npcId, tile) -> new RockCrab(npcId, tile, false));
}
