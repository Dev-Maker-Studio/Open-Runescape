package com.rs.game.content.bosses.pestqueen;

import com.rs.game.content.bosses.pestqueen.attack.Attack;
import com.rs.game.content.bosses.pestqueen.attack.impl.MeleeAttack;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;

/**
 *
 * @author Tyler
 *
 */
public class PestQueen extends NPC {

	public Attack currentAttack;

	public PestQueen(int id, Tile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea) {
		super(id, tile);
		setHitpoints(20000);
		setCombatLevel(599);
		currentAttack = new MeleeAttack();
	}

	@Override
	public void processNPC() {

	}
}
