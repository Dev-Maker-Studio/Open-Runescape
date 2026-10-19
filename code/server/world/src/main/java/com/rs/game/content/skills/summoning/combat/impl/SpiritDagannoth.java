package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.Utils;

public class SpiritDagannoth extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.SPIRIT_DAGANNOTH.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (Utils.getRandomInclusive(4) != 0)
			return CANCEL;

		npc.sync(7791, 1427);
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MELEE, target)));
		delayHit(npc, 1, target, Hit.melee(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MELEE, target)));
		return npc.getAttackSpeed();
	}
}
