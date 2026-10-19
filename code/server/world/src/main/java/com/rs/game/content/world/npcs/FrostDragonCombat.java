package com.rs.game.content.world.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.combat.PlayerCombatKt;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class FrostDragonCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Frost dragon" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int damage;
		int mageRange = npc.getTempAttribs().getI("frostAtkStyle", -1) == -1 ? Utils.getRandomInclusive(1) : npc.getTempAttribs().getI("frostAtkStyle", -1);
		npc.getTempAttribs().setI("frostAtkStyle", mageRange);

		if (Utils.random(3) == 0) {
			if (WorldUtil.isInRange(npc, target, 0)) {
				damage = Utils.getRandomInclusive(500);
				int protection = PlayerCombatKt.getAntifireLevel(target, true);
				if (protection == 1)
					damage = Utils.getRandomInclusive(40);
				else if (protection == 2)
					damage = 0;
				npc.setNextAnimation(new Animation(13152));
				npc.setNextSpotAnim(new SpotAnim(2465));
				delayHit(npc, 1, target, Hit.flat(npc, damage));
			} else {
				damage = Utils.getRandomInclusive(500);
				int protection = PlayerCombatKt.getAntifireLevel(target, true);
				if (protection == 1)
					damage = Utils.getRandomInclusive(50);
				else if (protection == 2)
					damage = 0;
				npc.setNextAnimation(new Animation(13155));
				delayHit(npc, World.sendProjectile(npc, target, 393, new Pair<>(28, 16), 35, 5, 16).getTaskDelay(), target, Hit.flat(npc, damage));
			}
		} else if (npc.withinDistance(target.getTile(), 3) && Utils.random(2) == 0) {
			damage = getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target);
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			delayHit(npc, 0, target, Hit.melee(npc, damage));
		} else if (mageRange == 0) {
			damage = Utils.getRandomInclusive(250);
			npc.setNextAnimation(new Animation(13155));
			delayHit(npc, World.sendProjectile(npc, target, 2705, new Pair<>(28, 16), 35, 5, 16).getTaskDelay(), target, Hit.magic(npc, damage), () -> target.setNextSpotAnim(new SpotAnim(2711)));
		} else {
			damage = Utils.getRandomInclusive(250);
			npc.setNextAnimation(new Animation(13155));
			delayHit(npc, World.sendProjectile(npc, target, 11, new Pair<>(28, 16), 35, 5, 16).getTaskDelay(), target, Hit.range(npc, damage));
		}
		return npc.getAttackSpeed();
	}

}
