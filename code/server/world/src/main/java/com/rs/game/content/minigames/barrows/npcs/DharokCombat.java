package com.rs.game.content.minigames.barrows.npcs;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;

public class DharokCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 2026 };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		npc.setNextAnimation(new Animation(defs.getAttackEmote()));
		int damage = getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target);
		if (damage != 0) {
			double perc = 1 - ((double) npc.getHitpoints() / npc.getMaxHitpoints());
			damage += perc * 380;
		}
		delayHit(npc, 0, target, Hit.melee(npc, damage));
		return npc.getAttackSpeed();
	}
}
