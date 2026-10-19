package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class UnholyCursebearerCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Unholy cursebearer" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		int attackStyle = WorldUtil.isInRange(target.getX(), target.getY(), target.getSize(), npc.getX(), npc.getY(), npc.getSize(), 0) ? Utils.random(2) : 0;
		if (target instanceof Player player && target.getTempAttribs().getI("UNHOLY_CURSEBEARER_ROT") == -1) {
			target.getTempAttribs().setI("UNHOLY_CURSEBEARER_ROT", 1);
			player.sendMessage("An undead rot starts to work at your body.");
			WorldTasks.scheduleTimer(0, 12, (ticks) -> {
				int value = target.getTempAttribs().getI("UNHOLY_CURSEBEARER_ROT");
				if (player.hasFinished() || npc.hasFinished() || !((DungeonBoss) npc).getManager().isAtBossRoom(player.getTile()) || value == -1) {
					target.getTempAttribs().removeI("UNHOLY_CURSEBEARER_ROT");
					return false;
				}
				int damage = 20 * value;
				for (int stat = 0; stat < 7; stat++) {
					if (stat == Constants.HITPOINTS)
						continue;
					int drain = Utils.random(5) + 1;
					if (stat == Constants.PRAYER)
						player.getPrayer().drainPrayer(drain * 10);
					player.getSkills().drainLevel(stat, drain);
				}
				int maxDamage = player.getMaxHitpoints() / 10;
				if (damage > maxDamage)
					damage = maxDamage;
				if (value == 6)
					player.sendMessage("The undead rot can now be cleansed by the unholy font.");
				player.applyHit(new Hit(npc, damage, HitLook.TRUE_DAMAGE));
				player.setNextSpotAnim(new SpotAnim(2440));
				target.getTempAttribs().incI("UNHOLY_CURSEBEARER_ROT");
				return true;
			});
		}
		switch (attackStyle) {
			case 0 -> {
				boolean multiTarget = Utils.random(2) == 0;
				npc.setNextAnimation(new Animation(multiTarget ? 13176 : 13175));
				if (multiTarget) {
					npc.setNextSpotAnim(new SpotAnim(2441));
					for (Entity t : npc.getPossibleTargets()) {
						World.sendProjectile(npc, t, 88, new Pair<>(50, 30), 41, 4, 0);
						delayHit(npc, 1, t, Hit.magic(npc, getMaxHit(npc, (int) (npc.getLevelForStyle(CombatStyle.MAGIC) * 0.6), CombatStyle.MAGIC, t)));
					}
				} else {
					World.sendProjectile(npc, target, 88, new Pair<>(50, 30), 41, 5, 0);
					delayHit(npc, 1, target, Hit.magic(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, target)));
				}
			}
			case 1 -> {
				npc.setNextAnimation(new Animation(defs.getAttackEmote()));
				delayHit(npc, 0, target, Hit.melee(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, target)));
			}
		}
		return npc.getAttackSpeed();
	}
}
