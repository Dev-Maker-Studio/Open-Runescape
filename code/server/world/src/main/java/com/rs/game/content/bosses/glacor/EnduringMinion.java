package com.rs.game.content.bosses.glacor;

import com.rs.game.World;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import kotlin.Pair;

public class EnduringMinion extends NPC {

	public Glacor parent;
	public boolean defeated = false;

	public EnduringMinion(int id, Tile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea, boolean spawned, Glacor parent) {
		super(id, tile, spawned);
		this.parent = parent;
		setForceMultiAttacked(true);
	}

	@Override
	public void processEntity() {
		super.processEntity();
		if (getHitpoints() <= 0 || isDead()) {
			if (!defeated)
				World.sendProjectile(this, parent, 634, new Pair<>(34, 16), 30, 5, 16);
			defeated = true;
		}
	}

	@Override
	public void handlePreHit(Hit hit) {
		int distance = (int) Utils.getDistance(parent.getX(), parent.getY(), getX(), getY());
		double damageReduction = distance * 0.1;
		hit.setDamage((int) (hit.getDamage() * damageReduction));

		super.handlePreHit(hit);
	}

}
