package com.rs.game.content.minigames.fightkiln.npcs;

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

public class TokHaarMej extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 15203 };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int distanceX = target.getX() - npc.getX();
		int distanceY = target.getY() - npc.getY();
		int size = npc.getSize();
		int hit = 0;
		int attackStyle = Utils.random(2);
		if (attackStyle == 0 && (distanceX > size || distanceX < -1 || distanceY > size || distanceY < -1))
			attackStyle = 1;
		switch (attackStyle) {
		case 0:
			hit = getMaxHit(npc, defs.getMaxHit() - 36, CombatStyle.MELEE, target);
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			delayHit(npc, 0, target, Hit.melee(npc, hit));
			break;
		case 1:
			hit = getMaxHit(npc, defs.getMaxHit(), CombatStyle.MAGIC, target);
			npc.setNextAnimation(new Animation(16122));
			World.sendProjectile(npc, target, 2991, new Pair<>(34, 16), 30, 10, 16);
			delayHit(npc, 2, target, Hit.magic(npc, hit));
			break;
		}
		return npc.getAttackSpeed();
	}
}
