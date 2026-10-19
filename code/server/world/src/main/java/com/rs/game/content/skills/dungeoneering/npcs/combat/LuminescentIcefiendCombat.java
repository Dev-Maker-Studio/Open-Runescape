package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.npcs.LuminscentIcefiend;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import kotlin.Pair;

public class LuminescentIcefiendCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Luminescent icefiend" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final LuminscentIcefiend icefiend = (LuminscentIcefiend) npc;

		if (icefiend.isSpecialEnabled()) {
			npc.setNextAnimation(new Animation(13338));
			npc.setNextSpotAnim(new SpotAnim(2524));

			icefiend.commenceSpecial();
			return 20;
		}

		boolean magicAttack = Utils.random(2) == 0;

		if (magicAttack) {
			npc.setNextAnimation(new Animation(13352));
			World.sendProjectile(npc, target, 2529, new Pair<>(15, 16), 35, 5, 16);
			delayHit(icefiend, 2, target, Hit.magic(npc, getMaxHit(npc, icefiend.getMaxHit(), CombatStyle.MAGIC, target)));
		} else {
			npc.setNextAnimation(new Animation(13337));
			World.sendProjectile(npc, target, 2530, new Pair<>(30, 16), 35, 5, 0);
			delayHit(icefiend, 2, target, Hit.range(npc, getMaxHit(npc, (int) (icefiend.getMaxHit() * .90), CombatStyle.RANGE, target)));
			WorldTasks.schedule(new Task() {

				@Override
				public void run() {
					target.setNextSpotAnim(new SpotAnim(2531));
				}
			}, 2);
		}
		return 4;
	}
}
