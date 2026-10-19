package com.rs.game.content.skills.dungeoneering.npcs.combat;

import com.rs.game.World;
import com.rs.game.content.combat.CombatStyle;
import com.rs.game.content.skills.dungeoneering.npcs.WarpedGulega;
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

public class WarpedGulegaCombat extends CombatScript {

	private static final SpotAnim MELEE = new SpotAnim(2878);

	@Override
	public Object[] getKeys() {
		return new Object[] { "Warped Gulega" };
	}

	@Override
	public int attack(final NPC npc, final Entity target) {
		final WarpedGulega boss = (WarpedGulega) npc;

		int style = Utils.random(4);
		switch (style) {
			// reg aeo melee
			case 3 -> {
				npc.setNextAnimation(new Animation(15004));
				final List<Tile> attackTiles = new LinkedList<>();
				for (Entity t : boss.getPossibleTargets(true))
					attackTiles.add(Tile.of(t.getTile()));
				WorldTasks.schedule(new Task() {

					@Override
					public void run() {
						for (Tile tile : attackTiles)
							World.sendSpotAnim(tile, MELEE);
						for (Entity t : boss.getPossibleTargets(true))
							tileLoop:for (Tile tile : attackTiles)
								if (t.getX() == tile.getX() && t.getY() == tile.getY()) {
									delayHit(npc, 0, t, Hit.melee(npc, getMaxHit(npc, (int) (npc.getLevelForStyle(CombatStyle.MELEE) * 0.75), CombatStyle.MELEE, t)));
									break tileLoop;
								}
					}
				});
			}
			// reg range aeo
			case 1 -> {
				npc.setNextAnimation(new Animation(15001));
				npc.setNextSpotAnim(new SpotAnim(2882));
				for (Entity t : npc.getPossibleTargets(true)) {
					World.sendProjectile(npc, t, 2883, new Pair<>(75, 25), 30, 5, 15);
					t.setNextSpotAnim(new SpotAnim(2884, 90, 0));
					delayHit(npc, 2, t, Hit.range(npc, getMaxHit(npc, (int) (npc.getLevelForStyle(CombatStyle.MELEE) * 0.75), CombatStyle.RANGE, t)));
				}
			}
			// reg magic aeo
			case 2 -> {
				npc.setNextAnimation(new Animation(15007));
				for (Entity t : npc.getPossibleTargets(true)) {
					World.sendProjectile(npc, t, 2880, new Pair<>(150, 75), 30, 5, 15);
					t.setNextSpotAnim(new SpotAnim(2881, 90, 0));
					delayHit(npc, 2, t, Hit.magic(npc, getMaxHit(npc, (int) (npc.getLevelForStyle(CombatStyle.MELEE) * 0.75), CombatStyle.MAGIC, t)));
				}
			}
			case 0 -> {
				npc.setNextAnimation(new Animation(15004));
				WorldTasks.scheduleLooping(new Task() {

					Tile center;
					int cycles;

					@Override
					public void run() {
						cycles++;

						if (cycles == 1) {
							center = Tile.of(target.getTile());
							sendTenticals(boss, center, 2);
						} else if (cycles == 3)
							sendTenticals(boss, center, 1);
						else if (cycles == 5)
							sendTenticals(boss, center, 0);
						else if (cycles == 6) {
							for (Entity t : npc.getPossibleTargets(true))
								if (t.getX() == center.getX() && t.getY() == center.getY())
									t.applyHit(new Hit(npc, t.getHitpoints() - 1, HitLook.TRUE_DAMAGE));
							stop();
							return;
						}
					}
				}, 0, 0);
				return 7;
			}
		}
		return 4;
	}

	private void sendTenticals(NPC npc, Tile center, int stage) {
		if (stage == 0)
			World.sendSpotAnim(center, MELEE);
		else if (stage == 2 || stage == 1) {
			World.sendSpotAnim(center.transform(-stage, stage, 0), MELEE);
			World.sendSpotAnim(center.transform(stage, stage, 0), MELEE);
			World.sendSpotAnim(center.transform(-stage, -stage, 0), MELEE);
			World.sendSpotAnim(center.transform(stage, -stage, 0), MELEE);
		}
	}
}
