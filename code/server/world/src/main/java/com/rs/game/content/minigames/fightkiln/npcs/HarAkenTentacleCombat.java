package com.rs.game.content.minigames.fightkiln.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.WorldProjectile;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;
import kotlin.Pair;

public class HarAkenTentacleCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 15209, 15210 };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int distanceX = target.getX() - npc.getX();
		int distanceY = target.getY() - npc.getY();
		int size = npc.getSize();
		int attackStyle = Utils.random(2);
		if (attackStyle == 0 && (distanceX > size || distanceX < -1 || distanceY > size || distanceY < -1))
			attackStyle = 1;
		switch (attackStyle) {
		case 0:
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit() - 36, CombatStyle.MELEE, target)));
			break;
		case 1:
			npc.setNextAnimation(new Animation(npc.getId() == 15209 ? 16253 : 16242));
			WorldProjectile p = World.sendProjectile(npc, target, npc.getId() == 15209 ? 3004 : 2922, new Pair<>(140, 35), 80, 10, 16);
			if (npc.getId() == 15209)
				delayHit(npc, p.getTaskDelay(), target, Hit.range(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.RANGE, target)));
			else
				delayHit(npc, p.getTaskDelay(), target, Hit.magic(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.MAGIC, target)));
			break;
		}
		return npc.getAttackSpeed();
	}
}
