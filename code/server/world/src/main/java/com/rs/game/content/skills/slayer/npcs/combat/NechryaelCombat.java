package com.rs.game.content.skills.slayer.npcs.combat;

import com.rs.game.content.skills.slayer.npcs.Nechryael;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;

public class NechryaelCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Nechryael" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		NPCCombatDefinitions def = npc.getCombatDefinitions();
		if (npc instanceof Nechryael n)
			if (Utils.random(10) == 0 && !n.hasActiveSpawns())
				n.summonDeathSpawns();
		npc.setNextAnimation(new Animation(npc.getCombatDefinitions().getAttackEmote()));
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, def.getMaxHit(), def.getAttackStyle(), target)));
		return npc.getAttackSpeed();
	}
}
