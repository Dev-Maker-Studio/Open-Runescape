package com.rs.game.content.bosses.dagkings;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.WorldProjectile;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Equipment;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;
import kotlin.Pair;

public class SpinolypCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Spinolyp" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		switch (Utils.random(2)) {
		case 0 -> {
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			WorldProjectile projectile = World.sendProjectile(npc, target, 2705, new Pair<>(34, 16), 35, 5, 10);
			Hit hit = Hit.magic(npc, getMaxHit(npc, CombatStyle.RANGE, target));
			delayHit(npc, projectile.getTaskDelay(), target, hit, () -> {
				if (hit.getDamage() > 0 && target instanceof Player p)
					p.getPrayer().drainPrayer(p.getEquipment().wearingSlot(Equipment.SHIELD, 13744, 23700) ? 0.5 : 1.0);
			});
		}
		case 1 -> {
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			WorldProjectile projectile = World.sendProjectile(npc, target, 473, new Pair<>(34, 16), 35, 5, 10);
			Hit hit = Hit.range(npc, getMaxHit(npc, CombatStyle.RANGE, target));
			delayHit(npc, projectile.getTaskDelay(), target, hit, () -> {
				if (hit.getDamage() > 0 && Utils.random(10) == 0)
					target.getPoison().makePoisoned(68);
			});
		}
		}
		if (Utils.random(10) == 0)
			target.getPoison().makePoisoned(68);
		return npc.getAttackSpeed();
	}
}
