package com.rs.game.content.skills.summoning.combat.impl;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.summoning.Pouch;
import com.rs.game.content.skills.summoning.combat.FamiliarCombatScript;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import kotlin.Pair;

public class TzKih extends FamiliarCombatScript {

	@Override
	public Object[] getKeys() {
		return Pouch.SPIRIT_TZ_KIH.getIdKeys();
	}
	
	@Override
	public int alternateAttack(final NPC npc, final Entity target) {
		npc.sync(npc.getCombatDefinitions().getAttackEmote(), 1422);
		Hit hit = Hit.magic(npc, getMaxHit(npc, npc.getCombatDefinitions().getMaxHit(), CombatStyle.MAGIC, target));
		delayHit(npc, World.sendProjectile(npc, target, 1423, new Pair<>(34, 16), 30, 5, 16).getTaskDelay(), target, hit);
		if (hit.getDamage() > 0 && target instanceof Player player)
			player.getPrayer().drainPrayer(player.getPrayer().getPoints() * 0.02);
		return npc.getAttackSpeed();
	}
}
