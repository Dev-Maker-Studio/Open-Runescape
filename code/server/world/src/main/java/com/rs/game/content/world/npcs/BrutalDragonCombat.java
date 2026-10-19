package com.rs.game.content.world.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.combat.PlayerCombatKt;
import com.rs.game.model.WorldProjectile;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import kotlin.Pair;

public class BrutalDragonCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Brutal green dragon" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int damage;
		switch (Utils.getRandomInclusive(3)) {
		case 0: // Melee
			if (npc.withinDistance(target.getTile(), 3)) {
				damage = getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target);
				npc.setNextAnimation(new Animation(defs.getAttackEmote()));
				delayHit(npc, 0, target, Hit.melee(npc, damage));
			} else {
				damage = Utils.getRandomInclusive(500);
				int protection = PlayerCombatKt.getAntifireLevel(target, true);
				if (protection == 1)
					damage = Utils.getRandomInclusive(50);
				else if (protection == 2)
					damage = 0;
				npc.setNextAnimation(new Animation(12259));
				WorldProjectile p = World.sendProjectile(npc, target, 393, new Pair<>(28, 32), 50, 5, 16);
				delayHit(npc, p.getTaskDelay(), target, Hit.flat(npc, damage));
			}
			break;
		case 1: // Dragon breath
			if (npc.withinDistance(target.getTile(), 3)) {
				damage = Utils.getRandomInclusive(650);
				int protection = PlayerCombatKt.getAntifireLevel(target, true);
				if (protection == 1)
					damage = Utils.getRandomInclusive(300);
				else if (protection == 2)
					damage = 0;
				npc.setNextAnimation(new Animation(14245));
				npc.setNextSpotAnim(new SpotAnim(2465));
				delayHit(npc, 1, target, Hit.flat(npc, damage));
			} else {
				damage = Utils.getRandomInclusive(650);
				int protection = PlayerCombatKt.getAntifireLevel(target, true);
				if (protection == 1)
					damage = Utils.getRandomInclusive(40);
				else if (protection == 2)
					damage = 0;
				npc.setNextAnimation(new Animation(12259));
				WorldProjectile p = World.sendProjectile(npc, target, 393, new Pair<>(28, 32), 50, 5, 16);
				delayHit(npc, p.getTaskDelay(), target, Hit.flat(npc, damage));
			}
			break;
		case 2:
		case 3:
			damage = Utils.getRandomInclusive(180);
			npc.setNextAnimation(new Animation(12259));
			WorldProjectile p = World.sendProjectile(npc, target, 2705, new Pair<>(28, 32), 50, 5, 16);
			delayHit(npc, p.getTaskDelay(), target, Hit.magic(npc, damage));
			break;
		}
		return npc.getAttackSpeed();
	}

}
