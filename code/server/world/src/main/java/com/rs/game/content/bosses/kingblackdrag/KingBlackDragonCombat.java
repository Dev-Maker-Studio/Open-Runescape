package com.rs.game.content.bosses.kingblackdrag;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.combat.PlayerCombatKt;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;
import com.rs.utils.Ticks;
import kotlin.Pair;

public class KingBlackDragonCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { 50 };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int attackStyle = Utils.getRandomInclusive(5);
		int size = npc.getSize();

		if (attackStyle == 0) {
			int distanceX = target.getX() - npc.getX();
			int distanceY = target.getY() - npc.getY();
			if ((distanceX <= size) && (distanceX >= -1) && (distanceY <= size) && (distanceY >= -1)) {
				delayHit(npc, 0, target, Hit.melee(npc, getMaxHit(npc, defs.getMaxHit(), CombatStyle.MELEE, target)));
				npc.setNextAnimation(new Animation(defs.getAttackEmote()));
				return npc.getAttackSpeed();
			}
			attackStyle = Utils.getRandomInclusive(4) + 1;
		}
		if (attackStyle == 1 || attackStyle == 2) {
			int damage = Utils.getRandomInclusive(650);
			final Player player = target instanceof Player p ? p : null;
			npc.soundEffect(3750, true);
			int protection = PlayerCombatKt.getAntifireLevel(player, true);
			if (protection == 1)
				damage = Utils.getRandomInclusive(150);
			if (protection == 2)
				damage = 0;
			delayHit(npc, 2, target, Hit.flat(npc, damage));
			World.sendProjectile(npc, target, 393, new Pair<>(34, 16), 30, 5, 16);
			npc.setNextAnimation(new Animation(81));

		} else if (attackStyle == 3) {
			int damage = Utils.getRandomInclusive(650);
			final Player player = target instanceof Player p ? p : null;
			npc.soundEffect(587, true);
			int protection = PlayerCombatKt.getAntifireLevel(player, true);
			if (protection == 1)
				damage = getMaxHit(npc, 164, CombatStyle.MAGIC, target);
			if (protection == 2)
				damage = getMaxHit(npc, 100, CombatStyle.MAGIC, target);
			if (Utils.getRandomInclusive(2) == 0)
				target.getPoison().makePoisoned(80);
			delayHit(npc, 2, target, Hit.flat(npc, damage));
			World.sendProjectile(npc, target, 394, new Pair<>(34, 16), 30, 5, 16);
			npc.setNextAnimation(new Animation(81));
		} else if (attackStyle == 4) {
			int damage = Utils.getRandomInclusive(650);
			final Player player = target instanceof Player p ? p : null;
			npc.soundEffect(586, true);
			int protection = PlayerCombatKt.getAntifireLevel(player, true);
			if (protection == 1)
				damage = getMaxHit(npc, 164, CombatStyle.MAGIC, target);
			if (protection == 2)
				damage = getMaxHit(npc, 100, CombatStyle.MAGIC, target);
			if (Utils.getRandomInclusive(2) == 0)
				target.freeze(Ticks.fromSeconds(15));
			delayHit(npc, 2, target, Hit.flat(npc, damage));
			World.sendProjectile(npc, target, 395, new Pair<>(34, 16), 30, 5, 16);
			npc.setNextAnimation(new Animation(81));
		} else {
			int damage = Utils.getRandomInclusive(650);
			final Player player = target instanceof Player p ? p : null;
			npc.soundEffect(584, true);
			int protection = PlayerCombatKt.getAntifireLevel(player, true);
			if (protection == 1)
				damage = getMaxHit(npc, 164, CombatStyle.MAGIC, target);
			if (protection == 2)
				damage = getMaxHit(npc, 100, CombatStyle.MAGIC, target);
			delayHit(npc, 2, target, Hit.flat(npc, damage));
			World.sendProjectile(npc, target, 396, new Pair<>(34, 16), 30, 5, 16);
			npc.setNextAnimation(new Animation(81));
		}
		return npc.getAttackSpeed();
	}
}

