package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.DungeonUtils;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.content.skills.dungeoneering.npcs.bosses.DungeonBoss;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import kotlin.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FleshspoilerHaasghenahk extends DungeonBoss {

	private final List<FleshspoilerSpawn> creatures = new CopyOnWriteArrayList<>();

	private Entity cachedTarget;
	private boolean secondStage, useMagicOnly;
	private int fleshTicks;

	public FleshspoilerHaasghenahk(Tile tile, DungeonManager manager, RoomReference reference) {
		super(DungeonUtils.getClosestToCombatLevel(Utils.range(11925, 11939), manager.getBossLevel()), tile, manager, reference);
		useMagicOnly = true;
		setHitpoints(getMaxHitpoints());
	}

	@Override
	public void processNPC() {
		fleshTicks++;
		if (fleshTicks % 25 == 0) {
			if (secondStage)
				setNextAnimation(new Animation(14469));
			for (Entity t : getPossibleTargets()) {
				t.setNextSpotAnim(new SpotAnim(2775));
				t.applyHit(new Hit(this, (int) Utils.random(Utils.random(getMaxHit() * .01, getMaxHit() * .1), getMaxHit()), HitLook.TRUE_DAMAGE));
			}
		}
		if (secondStage && cachedTarget != null && (cachedTarget.isDead() || cachedTarget.hasFinished())) {
			calculateNextTarget();
			for (DungeonNPC npc : creatures) {
				npc.resetCombat();
				if (cachedTarget != null)
					npc.getCombat().setTarget(cachedTarget);
			}
		}
		super.processNPC();
	}

	private void addFleshCreatures() {
		final Tile centerTile = getManager().getTile(getReference(), 8, 7);
		final List<Tile> tiles = new LinkedList<>();
		for (int i = 0; i < 5; i++) {
			Tile tile = World.getFreeTile(centerTile, 6);
			World.sendProjectile(this, tile, 2765, new Pair<>(150, 0), 30, 5, 40);
			tiles.add(tile);
		}
		final FleshspoilerHaasghenahk boss = this;
		WorldTasks.scheduleTimer(3, (ticks) -> {
			for (int index = 0; index < 5; index++)
				creatures.add(new FleshspoilerSpawn(boss, tiles.get(index), getManager()));
			return false;
		});
	}

	public void removeFleshCreature(FleshspoilerSpawn spoiler_spawn) {
		creatures.remove(spoiler_spawn);
	}

	@Override
	public void sendDeath(Entity source) {
		if (!secondStage) {
			secondStage = true;
			calculateNextTarget();
			setNextAnimation(new Animation(14467));
			setNextSpotAnim(new SpotAnim(2765, 240, 0));
			WorldTasks.scheduleTimer(5, (ticks) -> {
				setNextAnimation(new Animation(-1));
				addFleshCreatures();
				transformIntoNPC(getId() - 30);
				setHitpoints(getMaxHitpoints());
				setLureDelay(Integer.MAX_VALUE);
				setCantFollowUnderCombat(true);
				resetBonuses();
				return false;
			});
			return;
		}
		for (DungeonNPC npc : creatures)
			npc.sendDeath(this);
		creatures.clear();
		super.sendDeath(source);
	}

	public boolean isSecondStage() {
		return secondStage;
	}

	public boolean canUseMagicOnly() {
		return useMagicOnly;
	}

	public void setUseMagicOnly(boolean useMagicOnly) {
		this.useMagicOnly = useMagicOnly;
	}

	public Entity getCachedTarget() {
		return cachedTarget;
	}

	public void calculateNextTarget() {
		cachedTarget = null;
		List<Entity> targets = getPossibleTargets();
		if (targets.isEmpty())
			return;
		cachedTarget = targets.get(Utils.random(targets.size()));
	}
}
