package com.rs.game.content.skills.dungeoneering.npcs.combat;

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
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class AnimatedBookCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Animated book" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		boolean meleeAttack = Utils.random(2) == 0;
		if (!meleeAttack || !WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0)) {
			magicAttack(npc, target);
			return npc.getAttackSpeed();
		}
		meleeAttack(npc, target);
		return npc.getAttackSpeed();
	}

	private void meleeAttack(NPC npc, Entity target) {
		npc.setNextAnimation(new Animation(13479));
		delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, 100, CombatStyle.MELEE, target)));
	}

	private void magicAttack(NPC npc, final Entity target) {
		npc.setNextAnimation(new Animation(13480));
		npc.setNextSpotAnim(new SpotAnim(2728));
		delayHit(npc, 1, target, Hit.magic(npc, getMaxHit(npc, 100, CombatStyle.MAGIC, target)));
		World.sendProjectile(npc, target, 2731, new Pair<>(34, 16), 30, 5, 16);
		WorldTasks.schedule(new Task() {

			@Override
			public void run() {
				target.setNextSpotAnim(new SpotAnim(2738, 0, 80));
			}
		}, 2);
	}
}
