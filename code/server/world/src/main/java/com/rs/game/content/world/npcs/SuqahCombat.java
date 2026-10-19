package com.rs.game.content.world.npcs;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import com.rs.utils.Ticks;

public class SuqahCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Suqah" };
	}

	@Override
	public int attack(NPC npc, final Entity target) {
		NPCCombatDefinitions defs = npc.getCombatDefinitions();
		boolean meleeOnly = npc.getId() == 4528 || npc.getId() == 4529 || npc.getId() == 4531 || npc.getId() == 4532;
		if (!meleeOnly && Utils.getRandomInclusive(3) == 0) {// barrage
			boolean hit = Utils.getRandomInclusive(1) == 0;
			delayHit(npc, 2, target, Hit.magic(npc, hit ? 100 : 0));
			WorldTasks.schedule(new Task() {
				@Override
				public void run() {
					target.setNextSpotAnim(new SpotAnim(369));
					npc.soundEffect(168, true);
					target.freeze(Ticks.fromSeconds(5));
				}
			});
		} else {
			npc.setNextAnimation(new Animation(defs.getAttackEmote()));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), defs.getAttackStyle(), target)));
		}
		return npc.getAttackSpeed();
	}
}
