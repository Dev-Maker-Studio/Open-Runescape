package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import org.darkan.core.map.pathfinder.Direction;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class PlaneFreezerLakhrahnazCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Plane-freezer Lakhrahnaz" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		if (Utils.random(8) == 0) {
			npc.resetWalkSteps();
			npc.addWalkSteps(npc.getX() + Utils.random(3) - 2, npc.getY() + Utils.random(3) - 2);
		}
		if (Utils.random(3) == 0) {
			int attackStyle = Utils.random(2);
			if (attackStyle == 1 && !WorldUtil.isInRange(target.getX(), target.getY(), target.getSize(), npc.getX(), npc.getY(), npc.getSize(), 0))
				attackStyle = 0;
			switch (attackStyle) {
				case 0 -> {
					npc.setNextAnimation(new Animation(13775));
					for (Entity t : npc.getPossibleTargets()) {
						World.sendProjectile(npc, t, 2577, new Pair<>(16, 16), 41, 5, 0);
						t.setNextSpotAnim(new SpotAnim(2578, 70, 0));
						delayHit(npc, 1, t, Hit.magic(npc, getMaxHit(npc, 100, CombatStyle.MAGIC, target)));
					}
				}
				case 1 -> {
					npc.setNextAnimation(new Animation(defs.getAttackEmote()));
					Direction dir = Direction.random();
					target.addWalkSteps(target.getX() + dir.dx, target.getY() + dir.dy, 1);
					delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, 100, CombatStyle.MELEE, target)));
				}
			}
			return npc.getAttackSpeed();
		}
		npc.setNextAnimation(new Animation(13775));
		npc.setNextSpotAnim(new SpotAnim(2574));
		World.sendProjectile(npc, target, 2595, new Pair<>(16, 16), 41, 5, 0);
		target.setNextSpotAnim(new SpotAnim(2576, 70, 0));
		delayHit(npc, 1, target, Hit.range(npc, getMaxHit(npc, 100, CombatStyle.RANGE, target)));
		return npc.getAttackSpeed();
	}
}
