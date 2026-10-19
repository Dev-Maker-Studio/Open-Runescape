package com.rs.game.content.skills.dungeoneering.npcs.bosses.balak;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.DungeonUtils;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.utils.WorldUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BalLakThePummeler extends DungeonBoss {

	private boolean skip;
	private int barPercentage;

	private final List<PoisonPuddle> puddles = new CopyOnWriteArrayList<>();

	public BalLakThePummeler(Tile tile, DungeonManager manager, RoomReference reference) {
		super(DungeonUtils.getClosestToCombatLevel(Utils.range(10128, 10141), manager.getBossLevel()), tile, manager, reference);
		setLureDelay(6000); // this way you can lure him hehe, still not as much as outside dung npcs
		setHitpoints(getMaxHitpoints());
	}

	@Override
	public void processNPC() {
		if (isDead())
			return;
		super.processNPC();
		skip = !skip;
		if (!skip) {
			boolean reduced = false;
			for (PoisonPuddle puddle : puddles) {
				puddle.cycles++;
				if (puddle.canDestroyPoison()) {
					puddles.remove(puddle);
					continue;
				}
				List<Entity> targets = getPossibleTargets(true);
				if (WorldUtil.collides(getX(), getY(), getSize(), puddle.tile.getX(), puddle.tile.getY(), 1)) {
					barPercentage = barPercentage > 1 ? barPercentage - 2 : 0;
					sendDefenceBar();
					reduced = true;
				}
				for (Entity t : targets) {
					if (!t.withinDistance(puddle.tile, 1))
						continue;
					t.applyHit(new Hit(this, Utils.random((int) (t.getHitpoints() * 0.25)) + 1, HitLook.TRUE_DAMAGE));
				}
			}
			if (!reduced)
				if (!isUnderCombat()) {
					if (barPercentage > 0) {
						barPercentage--;
						sendDefenceBar();
					}
				} else if (barPercentage < 100) {
					barPercentage++;
					sendDefenceBar();
				}
		}
	}

	@Override
	public void processHit(Hit hit) {
		int damage = hit.getDamage();
		HitLook look = hit.getLook();
		if (damage > 0)
			if (look == HitLook.MELEE_DAMAGE || look == HitLook.RANGE_DAMAGE || look == HitLook.MAGIC_DAMAGE) {
				double multiplier = (100D - (barPercentage)) / 100D;
				hit.setDamage((int) (damage * multiplier));
			}
		super.processHit(hit);
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		puddles.clear();
		sendDefenceBar();
	}

	private void sendDefenceBar() {
		if (isDead())
			getManager().hideBar(getReference());
		else
			getManager().showBar(getReference(), "Demon's Defence", barPercentage);
	}

	private static class PoisonPuddle {
		final Tile tile;
		int cycles;

		public PoisonPuddle(Tile tile, int barPercentage) {
			this.tile = tile;
		}

		public boolean canDestroyPoison() {
			return cycles == 15;
		}
	}

	public void addPoisonBubble(Tile centerTile) {
		puddles.add(new PoisonPuddle(centerTile, barPercentage));
		addPoisonBubbleSpotAnimations(centerTile);
	}

	private void addPoisonBubbleSpotAnimations(Tile centerTile) {
		World.sendSpotAnim(Tile.of(centerTile.getX(), centerTile.getY(), centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX()+1, centerTile.getY(), centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX(), centerTile.getY()+1, centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX()+1, centerTile.getY()+1, centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX()-1, centerTile.getY(), centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX(), centerTile.getY()-1, centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX()-1, centerTile.getY()-1, centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX()-1, centerTile.getY()+1, centerTile.getPlane()), new SpotAnim(2588));
		World.sendSpotAnim(Tile.of(centerTile.getX()+1, centerTile.getY()-1, centerTile.getPlane()), new SpotAnim(2588));
	}

	public List<PoisonPuddle> getPoisonPuddles() {
		return puddles;
	}
}
