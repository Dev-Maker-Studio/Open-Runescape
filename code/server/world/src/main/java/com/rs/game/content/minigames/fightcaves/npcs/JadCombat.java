package com.rs.game.content.minigames.fightcaves.npcs;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import kotlin.Pair;

public class JadCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 2745, 15208 };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int attackStyle = Utils.random(3);
		if (attackStyle == 2) {
			if (npc.inMeleeRange(target)) {
				npc.anim(defs.getAttackEmote());
				delayHit(npc, 1, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target)));
				return npc.getAttackSpeed();
			}
			attackStyle = Utils.random(2);
		}
		if (attackStyle == 1) { // range
			npc.sync(16202, 2994);
			WorldTasks.scheduleTimer((ticks) -> {
				switch (ticks) {
					case 2 -> target.spotAnim(3000);
					case 3 -> target.spotAnim(new SpotAnim(2741, 0, 100));
					case 4 -> {
						delayHit(npc, 1, target, Hit.range(npc, getMaxHit(npc, defs.getMaxHit() - 2, CombatStyle.RANGE, target)));
						return false;
					}
				}
				return true;
			});
		} else { // mage
			npc.sync(16195, 2995);
			WorldTasks.scheduleTimer((ticks) -> {
				switch (ticks) {
					case 2 -> World.sendProjectile(npc, target, 2996, new Pair<>(80, 30), 40, 10, 5);
					case 3 -> delayHit(npc, 1, target, Hit.magic(npc, getMaxHit(npc, defs.getMaxHit() - 2, CombatStyle.MAGIC, target)));
					case 4 -> {
						target.spotAnim(new SpotAnim(2741, 0, 100));
						return false;
					}
				}
				return true;
			});
		}

		return 8;
	}

}
