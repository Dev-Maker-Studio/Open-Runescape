package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.npcs.LakkTheRiftSplitter;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;
import kotlin.Pair;

import java.util.LinkedList;
import java.util.List;

public class LakkTheRiftSplitterCombat extends CombatScript {

	//	private static final int[] VOICES = { 3034, 2993, 3007 };
	private static final String[] MESSAGES = { "A flame portal will flush you out!", "Taste miasma!", "This will cut you down to size!" };

	@Override
	public Object[] getKeys() {
		return new Object[] { "Har'Lakk the Riftsplitter" };
	}

	@Override
	public int attack(NPC npc, Entity target) {
		final LakkTheRiftSplitter boss = (LakkTheRiftSplitter) npc;

		DungeonManager manager = boss.getManager();

		boolean smash = false;
		for (Player player : manager.getParty().getTeam())
			if (WorldUtil.collides(player.getX(), player.getY(), player.getSize(), npc.getX(), npc.getY(), npc.getSize())) {
				smash = true;
				player.setProtectionPrayBlock(2);
				delayHit(npc, 0, player, Hit.flat(npc, getMaxHit(npc, (int) (npc.getLevelForStyle(CombatStyle.MELEE) * .85), CombatStyle.MELEE, player)));
				delayHit(npc, 0, player, Hit.flat(npc, getMaxHit(npc, (int) (npc.getLevelForStyle(CombatStyle.MELEE) * .60), CombatStyle.MELEE, player)));
			}
		if (smash) {
			npc.setNextAnimation(new Animation(14383));
			return 5;
		}

		if (Utils.random(4) == 0) {
			final int type = Utils.random(3);
			switch (type) {
				case 0, 1, 2:
					final List<Tile> boundary = new LinkedList<>();
					for (int x = -1; x < 2; x++)
						for (int y = -1; y < 2; y++)
							boundary.add(target.transform(x, y, 0));
					if (boss.doesBoundaryOverlap(boundary)) {
						regularMagicAttack(target, npc);
						return 5;
					}
					// npc.playSoundEffect(VOICES[type]);
					WorldTasks.scheduleTimer(1, (ticks) -> {
						boss.setNextForceTalk(new ForceTalk(MESSAGES[type]));
						boss.setNextAnimation(new Animation(14398));
						boss.addPortalCluster(type, boundary.toArray(new Tile[1]));
						return false;
					});
					return 5;
			}
		}

		// melee or magic
		boolean onRange = WorldUtil.isInRange(npc.getX(), npc.getY(), npc.getSize(), target.getX(), target.getY(), target.getSize(), 0);
		boolean melee = onRange && Utils.random(2) == 0;
		if (melee) {
			npc.setNextAnimation(new Animation(14375));
			delayHit(npc, 0, target, Hit.melee(npc, getMaxHitFromAttackStyleLevel(npc, CombatStyle.MELEE, target)));
		} else
			regularMagicAttack(target, npc);
		return 5;
	}

	private void regularMagicAttack(Entity target, NPC npc) {
		npc.setNextAnimation(new Animation(14398));
		World.sendProjectile(npc, target, 2579, new Pair<>(50, 30), 41, 5, 0);
		if (target instanceof Player player) {
			int damage = getMaxHitFromAttackStyleLevel(npc, CombatStyle.MAGIC, player);
			if (player.getPrayer().getPoints() > 0 && player.getPrayer().isProtectingMage()) {
				player.getPrayer().drainPrayer((int) (damage * .5));
				player.sendMessage("Your prayer points feel drained.");
			} else
				delayHit(npc, 1, player, Hit.magic(npc, damage));
		}
		target.setNextSpotAnim(new SpotAnim(2580, 75, 0));
	}
}
