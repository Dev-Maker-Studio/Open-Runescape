package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;

public class LavaTitan extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.LAVA_TITAN.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		npc.setNextAnimation(new Animation(7980));
		npc.setNextSpotAnim(new SpotAnim(1490));
		delayHit(npc, 1, target, Hit.melee(npc, getMaxHit(npc, 140, CombatStyle.MELEE, target)));
		if (Utils.getRandomInclusive(10) == 0) // 1/10 chance of happening
			delayHit(npc, 1, target, Hit.melee(npc, Utils.getRandomInclusive(50)));
		return npc.getAttackSpeed();
	}
}
