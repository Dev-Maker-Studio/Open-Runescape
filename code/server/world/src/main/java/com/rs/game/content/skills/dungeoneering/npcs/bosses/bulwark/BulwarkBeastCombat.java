package com.rs.game.content.skills.dungeoneering.npcs.bosses.bulwark;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.npc.combat.NPCCombatUtil;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BulwarkBeastCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Bulwark beast" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final BulwarkBeast beast = (BulwarkBeast) npc;
		beast.refreshBar();

		final NPCCombatDefinitions defs = npc.getCombatDefinitions();

		if (Utils.random(15) == 0) {
			List<Entity> targets = npc.getPossibleTargets();
			npc.setNextAnimation(new Animation(13007));
			for (Entity t : targets)
				if (WorldUtil.isInRange(t.getX(), t.getY(), t.getSize(), npc.getX(), npc.getY(), npc.getSize(), 0)) {
					t.setNextSpotAnim(new SpotAnim(2400));
					delayHit(npc, 1, t, Hit.flat(npc, 1 + Utils.random((int) (npc.getLevelForStyle(CombatStyle.MELEE) * 0.7))));
				}
			return npc.getAttackSpeed();
		}

		// mage, range, melee
		int attackStyle = Utils.random(WorldUtil.isInRange(target.getX(), target.getY(), target.getSize(), npc.getX(), npc.getY(), npc.getSize(), 0) ? 3 : 2);
		switch (attackStyle) {
			case 0 -> {
				npc.sync(13004, 2397);
				NPCCombatUtil.projectileBounce(npc, target, new HashSet<>(Set.of(target)), 2398, 2399, 5, null, nextTarget ->
					delayHit(npc, 0, nextTarget, Hit.magic(npc, getMaxHit(npc, npc.getMaxHit(), CombatStyle.MAGIC, nextTarget))));
			}
			case 1 -> {
				npc.setNextAnimation(new Animation(13006));
				npc.setNextSpotAnim(new SpotAnim(2394));
				List<Entity> targets = npc.getPossibleTargets();
				for (Entity t : targets) {
					World.sendProjectile(npc, t, 2395, new Pair<>(35, 30), 41, 5, 0);
					t.setNextSpotAnim(new SpotAnim(2396, 75, 0));
					delayHit(npc, 1, t, Hit.range(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.RANGE, t)));
				}
			}
			case 2 -> {
				npc.setNextAnimation(new Animation(defs.getAttackEmote()));
				delayHit(npc, 0, target, Hit.melee(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, target)));
			}
		}
		return npc.getAttackSpeed();
	}
}
