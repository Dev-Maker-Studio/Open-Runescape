package com.rs.game.content.skills.slayer.npcs.combat;

import com.rs.game.World;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class DarkBeastCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 2783 };
	}

	@Override
	public int attack(NPC npc, final Entity target) {
		NPCCombatDefinitions def = npc.getCombatDefinitions();
		npc.setNextAnimation(new Animation(2731));
		if (WorldUtil.isInRange(target, npc, 3))
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, 170, def.getAttackStyle(), target)));
		else {
			final int damage = getMaxHit(npc, 90, def.getAttackStyle(), target);
			World.sendProjectile(npc, target, 2181, new Pair<>(41, 16), 41, 5, 16);
			delayHit(npc, 2, target, Hit.magic(npc, damage));
		}
		return npc.getAttackSpeed();
	}
}
