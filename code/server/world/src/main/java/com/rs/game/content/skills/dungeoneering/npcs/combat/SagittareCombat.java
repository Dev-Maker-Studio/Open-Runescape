package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.Sagittare;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import kotlin.Pair;

public class SagittareCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Sagittare" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final Sagittare boss = (Sagittare) npc;
		if (boss.isUsingSpecial() && !boss.isCantInteract()) {
			sendRainDropAttack(boss);
			return 10;
		}
		int attack = Utils.random(5);
		switch (attack) {
			// Normal range, Normal magic, Multi magic
			case 0, 1, 2 -> {
				npc.setNextAnimation(new Animation(13271));
				npc.setNextSpotAnim(new SpotAnim(attack == 0 ? 2532 : 2534, 0, 96));
				for (Entity t : npc.getPossibleTargets()) {
					if ((attack == 0 || attack == 1) && t != target)
						continue;
					World.sendProjectile(npc, t, attack == 0 ? 2533 : 2535, new Pair<>(65, 50), 54, 5, 5);
					if (attack == 0)
						delayHit(npc, 1, t, Hit.range(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.RANGE, t)));
					else
						delayHit(npc, 1, t, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, t)));
				}
			}
			// Bind attacks
			case 3, 4 -> {
				boolean isMagicAttack = attack == 3;
				npc.setNextAnimation(new Animation(13271));
				npc.setNextSpotAnim(new SpotAnim(isMagicAttack ? 2536 : 2539, 0, 96));
				for (Entity t : npc.getPossibleTargets()) {
					if (!(t instanceof Player player))
						continue;
					boolean bindTarget = false;

					World.sendProjectile(npc, t, isMagicAttack ? 2537 : 2540, new Pair<>(65, 50), 54, 5, 5);
					if (isMagicAttack) {
						if (!player.getPrayer().isProtectingMage())
							bindTarget = true;
						delayHit(npc, 1, t, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, t)));
					} else {
						bindTarget = Utils.random(2) == 0;// 50/50
						delayHit(npc, 1, t, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, t)));
					}
					if (bindTarget) {
						player.freeze(8);
						player.setRun(false);
					}
					player.setNextSpotAnim(new SpotAnim(2541, 75, 65));
				}
			}
		}
		return 5;
	}

	private void sendRainDropAttack(final Sagittare boss) {
		boss.setCantInteract(true);
		boss.setNextForceTalk(new ForceTalk("Back off!"));
		final Tile center = Tile.of(boss.getTile());
		final DungeonManager manager = boss.getManager();
		final RoomReference rRef = manager.getCurrentRoomReference(center);
		if (rRef == null)
			return;
		WorldTasks.scheduleTimer((ticks) -> {
			if (boss.isDead()) {
				return false;
			}
			if (ticks == 2) {
				boss.setNextForceTalk(new ForceTalk("Arrow-rain!"));
				boss.setNextAnimation(new Animation(13270));
				boss.setNextSpotAnim(new SpotAnim(2542, 0, 93));
			} else if (ticks == 5) {
				boss.setCantInteract(false);
				boss.setNextAnimation(new Animation(8939));
				boss.setNextSpotAnim(new SpotAnim(1576));
			} else if (ticks == 7) {
				int stage = boss.getStage();
				Tile teleport = World.getFreeTile(manager.getTile(rRef, 6, 6), 1);

				if (stage != 1 && stage != -1) {
					int corner = Utils.random(4);
					if (corner == 0)// this is good
						teleport = manager.getTile(rRef, 1, Utils.random(14) + 1);// 1,1 14, 1, 14, 14, 2, 14
					else if (corner == 1)// this is good
						teleport = manager.getTile(rRef, 14 - Utils.random(14), 1);
					else if (corner == 2)// this is good
						teleport = manager.getTile(rRef, 14, 14 - Utils.random(14));
					else
						// this is good
						teleport = manager.getTile(rRef, Utils.random(14) + 1, 14);
				}

				for (int x = -1; x < 2; x++)
					for (int y = -1; y < 2; y++)
						World.sendProjectile(boss, center.transform(x, y, 0), 2533, new Pair<>(250, 0), 40, 3, 0);
				boss.tele(teleport);
				boss.setNextAnimation(new Animation(8941));
				boss.setNextSpotAnim(new SpotAnim(1577));
			} else if (ticks == 8) {
				targetL: for (Entity target : boss.getPossibleTargets()) {
					if (!(target instanceof Player player))
						continue;
					for (int x = -1; x < 2; x++)
						for (int y = -1; y < 2; y++) {
							Tile projectileTile = center.transform(x, y, 0);
							if (player.getX() != projectileTile.getX() || player.getY() != projectileTile.getY())
								continue targetL;
						}
					player.setRun(false);
					player.freeze(8);
					player.sendMessage("You have been injured and can't move.");
					int hit = (int) (boss.getMaxHit() * .1 + getMaxHit(boss, (int) (boss.getMaxHit() * .90), CombatStyle.RANGE, player));
					player.applyHit(new Hit(boss, hit, HitLook.TRUE_DAMAGE));
				}
				boss.setUsingSpecial(false);
				return false;
			}
			return true;
		});
	}
}
