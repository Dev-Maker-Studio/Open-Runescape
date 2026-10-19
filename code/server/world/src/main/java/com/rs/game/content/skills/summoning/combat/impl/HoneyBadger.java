package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Familiar;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.Utils;

public class HoneyBadger extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.HONEY_BADGER.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (!(npc instanceof Familiar fam))
			return CANCEL;
		if (fam.getAttribs().getL("insaneFerocity") > World.getServerTicks() || Utils.getRandomInclusive(2) != 0)
			return CANCEL;

		npc.sync(7930, 1397);
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MELEE, target)));
		delayHit(npc, 1, target, Hit.melee(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MELEE, target)));
		return npc.getAttackSpeed();
	}
}
