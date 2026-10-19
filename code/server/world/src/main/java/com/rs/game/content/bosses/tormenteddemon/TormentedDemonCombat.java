package com.rs.game.content.bosses.tormenteddemon;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import kotlin.Pair;

public class TormentedDemonCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Tormented demon" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		TormentedDemon torm = null;
		if (npc instanceof TormentedDemon td)
			torm = td;
		if (torm == null)
			return 0;
		int hit = 0;
		int attackStyle = torm.combatStyle;
		if (torm.getCombatStyleTimer() >= 23)
			return 0;
		switch (attackStyle) {
		case 0:
			if (!npc.inMeleeRange(target) || !npc.lineOfSightTo(target, true)) {
				npc.calcFollow(target, false);
				return 0;
			}
			hit = getMaxHit(npc, 189, CombatStyle.MELEE, target);
			npc.setNextAnimation(new Animation(10922));
			npc.setNextSpotAnim(new SpotAnim(1886));
			delayHit(npc, 1, target, Hit.melee(npc, hit));
			return 7;
		case 1:
			hit = getMaxHit(npc, 270, CombatStyle.MAGIC, target);
			npc.setNextAnimation(new Animation(10918));
			npc.setNextSpotAnim(new SpotAnim(1883, 0, 96 << 16));
			delayHit(npc, World.sendProjectile(npc, target, 1884, new Pair<>(34, 16), 30, 5, 16).getTaskDelay(), target, Hit.magic(npc, hit));
			break;
		case 2:
			hit = getMaxHit(npc, 270, CombatStyle.RANGE, target);
			npc.setNextAnimation(new Animation(10919));
			npc.setNextSpotAnim(new SpotAnim(1888));
			delayHit(npc, World.sendProjectile(npc, target, 1887, new Pair<>(34, 16), 30, 5, 16).getTaskDelay(), target, Hit.range(npc, hit));
			break;
		}
		return 7;
	}
}
