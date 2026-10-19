package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.npcs.Stomp;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class StompCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Stomp" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();

		Stomp stomp = (Stomp) npc;

		if (npc.getHitpoints() <= 10 || npc.getHitpoints() < (npc.getMaxHitpoints() * (2 - stomp.getStage()) * 0.33)) {
			stomp.charge();
			return npc.getAttackSpeed();
		}
		// 0 - first 33%
		// 1 - 66-33%
		// 2 - 33-0%
		// 3 - 0%

		if (stomp.getStage() > 1 && Utils.random(10) == 0) {
			final Tile tile = Tile.of(target.getTile());
			World.sendSpotAnim(tile, new SpotAnim(2400));
			WorldTasks.delay(4, () -> {
				for (Entity entity : npc.getPossibleTargets())
					if (entity.getX() == tile.getX() && entity.getY() == tile.getY())
						entity.applyHit(new Hit(npc, (int) (entity.getMaxHitpoints() * 0.25), HitLook.RANGE_DAMAGE));
			});
		}

		int attackStyle = Utils.random(/* stomp.getStage() > 1 ? 4 : */stomp.getStage() > 0 ? 3 : 2);
		if (attackStyle == 0 && !WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0))
			attackStyle = 1;

		switch (attackStyle) {
			case 0 -> {
				npc.setNextAnimation(new Animation(defs.getAttackEmote()));
				delayHit(npc, 0, target, Hit.melee(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, target)));
			}
			case 1 -> {
				npc.setNextAnimation(new Animation(13449));
				npc.setNextSpotAnim(new SpotAnim(2401));
				for (Entity t : npc.getPossibleTargets()) {
					World.sendProjectile(npc, t, 2402, new Pair<>(16, 16), 41, 5, 0);
					t.setNextSpotAnim(new SpotAnim(2403, 70, 0));
					delayHit(npc, 1, t, Hit.range(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.RANGE, target)));
				}
			}
			case 2 -> {
				npc.setNextAnimation(new Animation(13450));
				npc.setNextSpotAnim(new SpotAnim(2404));
				World.sendProjectile(npc, target, 2405, new Pair<>(30, 16), 41, 6, 0);
				target.setNextSpotAnim(new SpotAnim(2406, 120, 0));
				delayHit(npc, 2, target, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, target)));
			}
		}
		return npc.getAttackSpeed();
	}
}
