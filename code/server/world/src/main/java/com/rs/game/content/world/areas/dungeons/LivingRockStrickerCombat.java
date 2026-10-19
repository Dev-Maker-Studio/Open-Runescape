package com.rs.game.content.world.areas.dungeons;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;

public class LivingRockStrickerCombat extends CombatScript {

	@Override
	public Object[] getKeys() {

		return new Object[] { 8833 };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int distanceX = target.getX() - npc.getX();
		int distanceY = target.getY() - npc.getY();
		int size = npc.getSize();
		if ((distanceX <= size) && (distanceX >= -1) && (distanceY <= size) && (distanceY >= -1)) {
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, 84, CombatStyle.MELEE, target)));
			return npc.getAttackSpeed();
		}
		// TODO add projectile
		npc.setNextAnimation(new Animation(12196));
		delayHit(npc, 1, target, Hit.range(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.RANGE, target)));

		return npc.getAttackSpeed();
	}

}
