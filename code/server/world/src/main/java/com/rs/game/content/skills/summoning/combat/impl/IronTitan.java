package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.Utils;
import kotlin.Pair;

public class IronTitan extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.IRON_TITAN.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (Utils.random(5) != 0)
			return CANCEL;
		npc.sync(7694, 1452);
		delayHit(npc, World.sendProjectile(npc, target, 1454, new Pair<>(34, 16), 30, 5, 16).getTaskDelay(), target, Hit.magic(npc, getMaxHit(npc, 255, CombatStyle.MAGIC, target)));
		return npc.getAttackSpeed();
	}
}
