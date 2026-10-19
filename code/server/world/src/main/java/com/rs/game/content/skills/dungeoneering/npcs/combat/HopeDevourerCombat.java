package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.npcs.HopeDevourer;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;

public class HopeDevourerCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Hope devourer" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final HopeDevourer boss = (HopeDevourer) npc;
		final DungeonManager manager = boss.getManager();

		boolean stomp = false;
		for (Player player : manager.getParty().getTeam())
			if (WorldUtil.collides(player.getX(), player.getY(), player.getSize(), npc.getX(), npc.getY(), npc.getSize())) {
				stomp = true;
				delayHit(npc, 0, player, Hit.flat(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, player)));
			}
		if (stomp) {
			npc.setNextAnimation(new Animation(14459));
			return 6;
		}

		if (Utils.random(10) == 0) {
			npc.setNextForceTalk(new ForceTalk("Grrrrrrrrrroooooooooaaaarrrrr"));
			WorldTasks.scheduleTimer(2, (ticks) -> {
				npc.setNextAnimation(new Animation(14460));
				npc.setNextSpotAnim(new SpotAnim(2844, 30, 0));
				int healedDamage = 0;
				for (Entity t : npc.getPossibleTargets()) {
					Player player = (Player) t;
					int damage = (int) Utils.random(npc.getLevelForStyle(CombatStyle.MAGIC) * .85, npc.getLevelForStyle(CombatStyle.MAGIC));
					if (damage > 0 && player.getPrayer().isUsingProtectionPrayer()) {
						healedDamage += damage;
						player.setProtectionPrayBlock(2);
						t.setNextSpotAnim(new SpotAnim(2845, 75, 0));
						delayHit(npc, 0, t, Hit.magic(npc, damage));
					}
				}
				npc.heal(healedDamage);
				return false;
			});
			return 8;
		}

		if (!WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0))
			return 0;

		if (Utils.random(5) == 0) {
			npc.setNextAnimation(new Animation(14458));
			final int damage = (int) Utils.random(npc.getLevelForStyle(CombatStyle.MELEE) * .85, npc.getLevelForStyle(CombatStyle.MELEE));
			if (target instanceof Player player)
				player.getSkills().set(Constants.DEFENSE, (int) (player.getSkills().getLevel(Constants.DEFENSE) - (damage * .05)));
			delayHit(npc, 0, target, Hit.melee(npc, damage));
			WorldTasks.scheduleLooping(new Task() {
				private int ticks;
				private Tile tile;

				@Override
				public void run() {
					ticks++;
					if (ticks == 1) {
						if (target instanceof Player player) {
							player.lock(2);
							player.stopAll();
						}
						byte[] dirs = Utils.getDirection(npc.getFaceAngle());
						for (int distance = 2; distance >= 0; distance--) {
							tile = Tile.of(Tile.of(target.getX() + (dirs[0] * distance), target.getY() + (dirs[1] * distance), target.getPlane()));
							if (World.floorFree(tile.getPlane(), tile.getX(), tile.getY()) && manager.isAtBossRoom(tile))
								break;
							if (distance == 0)
								tile = Tile.of(target.getTile());
						}
						target.faceEntityTile(boss);
						target.forceMove(tile, 10070, 1, 60);
						stop();
					}
				}
			}, 0, 0);
		} else {
			npc.setNextAnimation(new Animation(14457));
			int damage = (int) Utils.random(npc.getLevelForStyle(CombatStyle.MELEE) * .75, npc.getLevelForStyle(CombatStyle.MELEE));
			if (target instanceof Player player)
				if (player.getPrayer().isProtectingMelee()) {
					player.sendMessage("Your prayer completely negates the attack.", true);
					damage = 0;
				}
			delayHit(npc, 0, target, Hit.melee(npc, damage));
		}
		return 6;
	}
}
