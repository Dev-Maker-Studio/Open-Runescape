package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import kotlin.Pair;

public class GeyserTitan extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.GEYSER_TITAN.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (npc.inMeleeRange(target)) {
			npc.setNextAnimation(new Animation(7879));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, 190, CombatStyle.MELEE, target)));
		} else {
			npc.setNextAnimation(new Animation(7883));
			npc.setNextSpotAnim(new SpotAnim(1375));
			delayHit(npc, World.sendProjectile(npc, target, 1374, new Pair<>(34, 16), 30, 5, 16).getTaskDelay(), target, Hit.range(npc, getMaxHit(npc, 190, CombatStyle.RANGE, target)));
		}
		return npc.getAttackSpeed();
	}
}
