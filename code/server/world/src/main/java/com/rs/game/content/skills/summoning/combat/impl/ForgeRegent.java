package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.Animation;
import kotlin.Pair;

public class ForgeRegent extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.FORGE_REGENT.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (npc.inMeleeRange(target)) {
			npc.setNextAnimation(new Animation(7863));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MELEE, target)));
		} else {
			npc.anim(7866);
			delayHit(npc, World.sendProjectile(npc, target, 1330, new Pair<>(34, 16), 30, 5, 16).getTaskDelay(), target, Hit.magic(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MAGIC, target)));
		}
		return npc.getAttackSpeed();
	}
}
