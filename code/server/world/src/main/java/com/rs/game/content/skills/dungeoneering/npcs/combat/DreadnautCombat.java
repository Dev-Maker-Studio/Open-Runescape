package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.npcs.Dreadnaut;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class DreadnautCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Dreadnaut" };// GFX 2859 Poop bubbles that drain prayer
	}

	@Override
	public int attack(NPC npc, Entity target) {
		Dreadnaut boss = (Dreadnaut) npc;

		if (!WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0))
			return 0;

		if (Utils.random(5) == 0) {
			npc.setNextAnimation(new Animation(14982));
			npc.setNextSpotAnim(new SpotAnim(2865));
			int damage = getMaxHit(boss, boss.getMaxHit(), CombatStyle.MELEE, target);
			if (damage > 0) {
				target.setNextSpotAnim(new SpotAnim(2866, 75, 0));
				sendReductionEffect(boss, target, damage);
			}
			if (target instanceof Player player) {
				player.sendMessage("You have been injured and are unable to use protection prayers.");
				player.setProtectionPrayBlock(12);
			}
			delayHit(npc, 1, target, Hit.melee(npc, damage));
		} else {
			npc.setNextAnimation(new Animation(14973));
			npc.setNextSpotAnim(new SpotAnim(2856));

			for (Entity t : boss.getPossibleTargets()) {
				if (!t.withinDistance(target.getTile(), 2))
					continue;
				int damage = getMaxHit(boss, boss.getMaxHit(), CombatStyle.MELEE, t);
				World.sendProjectile(boss, t, 2857, new Pair<>(30, 30), 25, 5, 15);
				if (damage > 0) {
					sendReductionEffect(boss, t, damage);
					boss.addSpot(Tile.of(t.getTile()));
				} else
					t.setNextSpotAnim(new SpotAnim(2858, 75, 0));
				delayHit(npc, 1, t, Hit.melee(npc, damage));
			}
		}
		return 5;
	}

	private void sendReductionEffect(Dreadnaut boss, Entity target, int damage) {
		if (!boss.canReduceMagicLevel() || !(target instanceof Player player))
			return;
		player.getSkills().set(Constants.MAGIC, (int) (player.getSkills().getLevel(Constants.MAGIC) - (damage * .10)));
	}
}
