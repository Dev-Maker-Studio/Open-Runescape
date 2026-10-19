package com.rs.game.content.world.areas.edgeville.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;

public class Monk extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Monk" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions def = npc.getCombatDefinitions();
		if (Utils.random(100) < 70) {
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, npc.getMaxHit(), def.getAttackStyle(), target)));
			npc.setNextAnimation(new Animation(def.getAttackEmote()));
		} else {
			npc.heal(20);
			npc.setNextSpotAnim(new SpotAnim(84));
		}
		return npc.getAttackSpeed();
	}
}
