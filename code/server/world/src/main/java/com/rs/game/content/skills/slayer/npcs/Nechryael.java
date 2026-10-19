package com.rs.game.content.skills.slayer.npcs;

import com.rs.game.World;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class Nechryael extends NPC {

	private NPC[] deathSpawns;

	public Nechryael(int id, Tile tile, boolean spawned) {
		super(id, tile, spawned);
	}

	@Override
	public void processNPC() {
		if (hasActiveSpawns() && !isUnderCombat())
			removeDeathSpawns();
		super.processNPC();
	}

	public void summonDeathSpawns() {
		deathSpawns = new NPC[2];
		Entity target = getCombat().getTarget();
		for (int idx = 0; idx < deathSpawns.length; idx++) {
			deathSpawns[idx] = World.spawnNPC(getId() + 1, World.getFreeTile(getTile(), 2), true, true);
			if (target != null)
				deathSpawns[idx].setCombatTarget(target);
		}
	}

	private void removeDeathSpawns() {
		if (deathSpawns == null)
			return;
		for (NPC npc : deathSpawns)
			npc.finish();
		deathSpawns = null;
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		removeDeathSpawns();
	}

	public boolean hasActiveSpawns() {
		return deathSpawns != null;
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 1613, 10702 }, (npcId, tile) -> new Nechryael(npcId, tile, false));
}
