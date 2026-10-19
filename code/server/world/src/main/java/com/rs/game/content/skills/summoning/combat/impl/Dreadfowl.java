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
import org.darkan.core.Utils;
import kotlin.Pair;

public class Dreadfowl extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.DREADFOWL.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		if (Utils.getRandomInclusive(10) != 0)
			return CANCEL;
		
		npc.setNextAnimation(new Animation(7810));
		npc.setNextSpotAnim(new SpotAnim(1523));
		delayHit(npc, 1, target, Hit.magic(npc, getMaxHit(npc, 30, CombatStyle.MAGIC, target)));
		World.sendProjectile(npc, target, 1318, new Pair<>(34, 16), 30, 5, 16);
		return npc.getAttackSpeed();
	}
}
