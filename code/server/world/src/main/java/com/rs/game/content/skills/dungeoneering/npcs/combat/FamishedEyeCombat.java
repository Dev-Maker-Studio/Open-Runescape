package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.npcs.FamishedEye;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.npc.combat.CombatScript;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import kotlin.Pair;

import java.util.LinkedList;
import java.util.List;

public class FamishedEyeCombat extends CombatScript {

	@Override
	public Object[] getKeys() {
		return new Object[] { "Famished warrior-eye", "Famished ranger-eye", "Famished mage-eye" };
	}

	@Override
	public int attack(NPC npc, final Entity target) {
		final FamishedEye eye = (FamishedEye) npc;

		if (eye.isInactive())
			return 0;
		if (!eye.isFirstHit()) {
			eye.setFirstHit(true);
			return Utils.random(5, 15);
		}

		npc.setNextAnimation(new Animation(14916));
		WorldTasks.scheduleLooping(new Task() {

			private List<Tile> tiles;
			private Tile targetTile;

			int cycles;

			@Override
			public void run() {
				cycles++;
				if (cycles == 1) {
					tiles = new LinkedList<>();
					targetTile = Tile.of(target.getTile());
					World.sendProjectile(eye, targetTile, 2849, new Pair<>(35, 30), 41, 5, 15);
				} else if (cycles == 2)
					for (int x = -1; x < 2; x++)
						for (int y = -1; y < 2; y++) {
							Tile attackedTile = targetTile.transform(x, y, 0);
							if (x != y)
								World.sendProjectile(targetTile, attackedTile, 2851, new Pair<>(35, 0), 26, 10, 16);
							tiles.add(attackedTile);
						}
				else if (cycles == 3) {
					for (Tile tile : tiles) {
						if (!tile.matches(targetTile))
							World.sendSpotAnim(tile, new SpotAnim(2852, 35, 5));
						for (Entity t : eye.getPossibleTargets())
							if (t.matches(tile))
								t.applyHit(new Hit(eye, (int) Utils.random(eye.getMaxHit() * .25, eye.getMaxHit()), HitLook.TRUE_DAMAGE));
					}
					tiles.clear();
					stop();
				}
			}
		}, 0, 0);
		return Utils.random(5, 35);
	}
}
