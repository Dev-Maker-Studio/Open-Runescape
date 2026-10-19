package com.rs.game.content.bosses.glacor;

import com.rs.game.World;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import kotlin.Pair;

public class SappingMinion extends NPC {

	public Glacor parent;
	public boolean defeated = false;

	public SappingMinion(int id, Tile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea, boolean spawned, Glacor parent) {
		super(id, tile, spawned);
		this.parent = parent;
		setForceAgressive(true);
		setForceMultiAttacked(true);
	}

	@Override
	public void processEntity() {
		super.processEntity();
		if (getHitpoints() <= 0 || isDead()) {
			if (!defeated)
				World.sendProjectile(this, parent, 634, new Pair<>(34, 16), 30, 10, 16);
			defeated = true;
		}
	}

}
