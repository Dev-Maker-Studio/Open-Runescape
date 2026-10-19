package com.rs.game.content.bosses.nomad;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import kotlin.Pair;

public class FakeNomadCombat extends CombatScript {

	@Override
	public Object[] getKeys() {

		return new Object[] { 8529 };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		npc.setNextAnimation(new Animation(12697));
		boolean hit = getMaxHit(npc, 50, CombatStyle.MAGIC, target) != 0;
		delayHit(npc, 2, target, Hit.flat(npc, hit ? 50 : 0));
		World.sendProjectile(npc, target, 1657, new Pair<>(30, 30), 75, 10, 0);
		if (hit)
			WorldTasks.schedule(new Task() {
				@Override
				public void run() {
					target.setNextSpotAnim(new SpotAnim(2278, 0, 100));
				}
			}, 1);
		return npc.getAttackSpeed();
	}

}
