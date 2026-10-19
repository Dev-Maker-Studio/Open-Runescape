package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;

public class FleshSpoilerSpawnCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Flesh-Spoiler spawn" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		npc.setNextAnimation(new Animation(Utils.random(3) == 0 ? 14474 : 14475));
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, target)));
		return 3;
	}
}
