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

public class Hydra extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.HYDRA.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (Utils.random(2) != 0)
			return CANCEL;
		npc.anim(npc.getCombatDefinitions().getAttackEmote());
		delayHit(npc, World.sendProjectile(npc, target, 1489, new Pair<>(34, 16), 30, 5, 16).getTaskDelay(), target, Hit.magic(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MAGIC, target)));
		return npc.getAttackSpeed();
	}
}
