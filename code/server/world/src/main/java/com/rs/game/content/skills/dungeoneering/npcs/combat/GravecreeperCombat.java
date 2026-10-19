package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.npcs.Gravecreeper;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class GravecreeperCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Gravecreeper" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final Gravecreeper boss = (Gravecreeper) npc;
		if (boss.getSpecialDelay() != -2 && (boss.getSpecialDelay() == -1 || (Utils.random(10) == 0 && boss.getSpecialDelay() <= World.getServerTicks()))) {
			if ((boss.getSpecialDelay() == -1) || (Utils.random(5) == 0)) {
				boss.useSpecial();
				return 4;
			}
			boss.setNextForceTalk(new ForceTalk("Burrnnn!"));
			WorldTasks.scheduleTimer(1, (ticks) -> {
				boss.createBurnTiles(Tile.of(boss.getTile()));
				return false;
			});
			boss.setSpecialDelay(World.getServerTicks() + Gravecreeper.BURN_DELAY);
			if (WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0)) {
				boss.setForceFollowClose(true);
				WorldTasks.scheduleTimer(7, (ticks) -> {
					boss.setForceFollowClose(false);
					return false;
				});
			}
			return 4;
		}

		boolean atDistance = !WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0);
		int attack = Utils.random(!atDistance ? 2 : 1);
		switch (attack) {
		case 0:// range
			npc.setNextAnimation(new Animation(14504));
			World.sendProjectile(npc, target, 2753, new Pair<>(65, 65), 30, 5, 0);
			delayHit(npc, 1, target, Hit.range(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.RANGE, target)));
			break;
		case 1:// melee
			npc.setNextAnimation(new Animation(14503));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, target)));
			break;
		}
		return 4;
	}
}
