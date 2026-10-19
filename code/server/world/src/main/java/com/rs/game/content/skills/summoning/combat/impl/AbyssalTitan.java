package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;

public class AbyssalTitan extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.ABYSSAL_TITAN.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		npc.setNextAnimation(new Animation(7693));
		Hit hit = Hit.melee(npc, getMaxHit(npc, 220, CombatStyle.MELEE, target));
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, 220, CombatStyle.MELEE, target)));
		if (target instanceof Player player)
			if (hit.getDamage() > 0 && player.getPrayer().getPoints() > 0)
				player.getPrayer().drainPrayer((double) hit.getDamage() / 2);
		return npc.getAttackSpeed();
	}
}
