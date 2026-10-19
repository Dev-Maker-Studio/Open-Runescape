package com.rs.game.content.minigames.fightcaves.npcs;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;

public class TzKihCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Tz-Kih" };
	}

	@Override
	public int attack(NPC npc, Entity target) {// yoa
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int damage = 0;
		npc.setNextAnimation(new Animation(defs.getAttackEmote()));
		damage = getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target);
		if (target instanceof Player player)
			player.getPrayer().drainPrayer(damage + 10);
		delayHit(npc, 0, target, Hit.melee(npc, damage));
		return npc.getAttackSpeed();
	}
}
