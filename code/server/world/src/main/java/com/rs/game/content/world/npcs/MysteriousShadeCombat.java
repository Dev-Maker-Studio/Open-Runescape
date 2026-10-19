package com.rs.game.content.world.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.WorldProjectile;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import kotlin.Pair;

public class MysteriousShadeCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Mysterious shade" };
	}

	@Override
	public int attack(NPC npc, final Entity target) {
		final boolean rangeAttack = Utils.random(1) == 0;

		npc.setNextAnimation(new Animation(rangeAttack ? 13396 : 13398));
		npc.setNextSpotAnim(new SpotAnim(rangeAttack ? 2514 : 2515));
		WorldProjectile projectile = World.sendProjectile(npc, target, rangeAttack ? 2510 : 2511, new Pair<>(18, 18), 25, 7, 0);
		if (rangeAttack)
			delayHit(npc, projectile.getTaskDelay(), target, Hit.range(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.RANGE, target)), () -> target.setNextSpotAnim(new SpotAnim(2512)));
		else
			delayHit(npc, projectile.getTaskDelay(), target, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, target)), () -> target.setNextSpotAnim(new SpotAnim(2513)));
		target.setNextSpotAnim(new SpotAnim(rangeAttack ? 2512 : 2513, projectile.getTaskDelay(), 0));
		return npc.getAttackSpeed();
	}
}
