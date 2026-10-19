package com.rs.game.content.world.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;
import kotlin.Pair;

public class WaterfiendCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 5361 };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int spellType = Utils.getRandomInclusive(2);
		Hit hit;
		if (spellType > 1)
			hit = Hit.magic(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.MAGIC, target));
		else
			hit = Hit.range(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.RANGE, target));

		npc.setNextAnimation(new Animation(defs.getAttackEmote()));
		if (spellType == 0) {
			World.sendProjectile(npc, target, 11, new Pair<>(20, 20), 25, 5, 25);
			delayHit(npc, 2, target, hit);
		} else if (spellType == 1) {
			World.sendProjectile(npc, target, 11, new Pair<>(20, 20), 25, 5, 25);
			delayHit(npc, 2, target, hit);
		} else if (spellType == 2) {
			World.sendProjectile(npc, target, 2704, new Pair<>(20, 20), 25, 5, 25);
			delayHit(npc, 2, target, hit);
		} else {
			World.sendProjectile(npc, target, 2704, new Pair<>(20, 20), 25, 5, 25);
			delayHit(npc, 2, target, hit);
		}

		return npc.getAttackSpeed();
	}
}
