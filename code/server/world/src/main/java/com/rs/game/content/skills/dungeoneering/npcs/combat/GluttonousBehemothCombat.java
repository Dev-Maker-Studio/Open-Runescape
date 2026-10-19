package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.GluttonousBehemoth;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.npc.combat.NPCCombatDefinitions;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

public class GluttonousBehemothCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Gluttonous behemoth" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final NPCCombatDefinitions defs = npc.getCombatDefinitions();
		DungeonBoss boss = (DungeonBoss) npc;
		DungeonManager manager = boss.getManager();
		boolean lessThanHalf = npc.getHitpoints() < npc.getMaxHitpoints() * .5;
		if (lessThanHalf && !npc.getTempAttribs().getB("GLUTTONOUS_HEALING")) {
			RoomReference reference = manager.getCurrentRoomReference(npc.getTile());
			GameObject food1 = manager.getObject(reference, 49283, 0, 11);
			GameObject food2 = manager.getParty().getTeam().size() <= 1 ? null : manager.getObject(reference, 49283, 11, 11);
			GameObject food = food1;
			if (food1 != null)
				for (Player player : manager.getParty().getTeam())
					if (player.withinDistance(food1.getTile(), food1.getDefinitions().getSizeX() + 1)) {
						food = null;
						break;
					}
			if (food == null && food2 != null) {
				food = food2;
				for (Player player : manager.getParty().getTeam())
					if (player.withinDistance(food2.getTile(), food1.getDefinitions().getSizeX() + 1)) {
						food = null;
						break;
					}
			}
			if (food != null) {
				npc.getTempAttribs().setB("GLUTTONOUS_HEALING", true);
				((GluttonousBehemoth) npc).setHeal(food);
				return 0;
			}
		}
		boolean stomp = false;
		for (Player player : manager.getParty().getTeam())
			if (WorldUtil.collides(player.getX(), player.getY(), player.getSize(), npc.getX(), npc.getY(), npc.getSize())) {
				stomp = true;
				delayHit(npc, 0, player, Hit.flat(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, player)));
			}
		if (stomp) {
			npc.setNextAnimation(new Animation(13718));
			return npc.getAttackSpeed();
		}
		int attackStyle = Utils.getRandomInclusive(2);
		if (attackStyle == 2) {
			if (WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0)) {
				npc.setNextAnimation(new Animation(defs.getAttackEmote()));
				delayHit(npc, 0, target, Hit.melee(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, target)));
				return npc.getAttackSpeed();
			}
			attackStyle = Utils.getRandomInclusive(1);
		}
		if (attackStyle == 0) {
			npc.setNextAnimation(new Animation(13719));
			World.sendProjectile(npc, target, 2612, new Pair<>(41, 16), 41, 5, 16);
			int damage = getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, target);
			delayHit(npc, 2, target, Hit.magic(npc, damage));
			if (damage != 0)
				WorldTasks.schedule(new Task() {
					@Override
					public void run() {
						target.setNextSpotAnim(new SpotAnim(2613));
					}
				}, 1);
		} else if (attackStyle == 1) {
			npc.setNextAnimation(new Animation(13721));
			World.sendProjectile(npc, target, 2610, new Pair<>(41, 16), 41, 5, 16);
			delayHit(npc, 2, target, Hit.range(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.RANGE, target)));
			WorldTasks.schedule(new Task() {
				@Override
				public void run() {
					target.setNextSpotAnim(new SpotAnim(2611));
				}
			}, 1);
		}
		return npc.getAttackSpeed();
	}
}
