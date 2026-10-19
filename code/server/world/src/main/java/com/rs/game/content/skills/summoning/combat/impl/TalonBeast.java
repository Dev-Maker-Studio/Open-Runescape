package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;

public class TalonBeast extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.TALON_BEAST.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		npc.anim(npc.getCombatDefinitions().getAttackEmote());
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, 100, CombatStyle.MELEE, target)));
		delayHit(npc, 1, target, Hit.melee(npc, getMaxHit(npc, 100, CombatStyle.MELEE, target)));
		delayHit(npc, 2, target, Hit.melee(npc, getMaxHit(npc, 100, CombatStyle.MELEE, target)));
		return npc.getAttackSpeed();
	}
}
