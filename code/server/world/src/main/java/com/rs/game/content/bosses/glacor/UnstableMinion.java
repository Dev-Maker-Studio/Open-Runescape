package com.rs.game.content.bosses.glacor;

import com.rs.game.World;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.TimerBar;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import kotlin.Pair;

public class UnstableMinion extends NPC {

	public Glacor parent;
	public boolean defeated = false;

	public boolean startedTimer = false;
	public boolean healing = false;

	public int gainedHp = 0;

	final int EXPLODE_GFX = 956;

	final NPC thisNpc = this;

	public UnstableMinion(int id, Tile tile, int mapAreaNameHash, boolean canBeAttackFromOutOfArea, boolean spawned, Glacor parent) {
		super(id, tile, spawned);
		this.parent = parent;
		setForceAgressive(true);
		setForceMultiAttacked(true);
	}

	@Override
	public void processEntity() {
		super.processEntity();

		if ((getHitpoints() < 500) && !isDead() && (getHitpoints() != 0))
			this.heal(10);

		if (!startedTimer && getHitpoints() > 500) {
			getNextHitBars().add(new TimerBar(700));
			startedTimer = true;
			startExplosionTimer();
			startStopMovingTimer();
		}

		if (getHitpoints() <= 0 || isDead()) {
			if (!defeated)
				World.sendProjectile(this, parent, 634, new Pair<>(34, 16), 30, 5, 16);
			defeated = true;
		}
	}

	public void startStopMovingTimer() {
		WorldTasks.schedule(new Task() {
			@Override
			public void run() {
				if (thisNpc.getHitpoints() <= 0 || thisNpc.isDead())
					return;
				thisNpc.freeze(50000000);
			}
		}, 22);
		WorldTasks.schedule(new Task() {
			@Override
			public void run() {
				if (thisNpc.getHitpoints() <= 0 || thisNpc.isDead())
					return;
				thisNpc.freeze(0);
			}
		}, 26);
	}

	public void startExplosionTimer() {
		WorldTasks.schedule(new Task() {
			@Override
			public void run() {
				if (thisNpc.getHitpoints() <= 0 || thisNpc.isDead())
					return;
				for (Player player : queryNearbyPlayersByTileRange(2, player -> !player.isDead()))
					if (Utils.getDistance(thisNpc.getX(), thisNpc.getY(), player.getX(), player.getY()) < 2)
						player.applyHit(new Hit(player, player.getHitpoints() / 3, HitLook.TRUE_DAMAGE));
				thisNpc.applyHit(new Hit(thisNpc, (int) (thisNpc.getHitpoints() * 0.90), HitLook.TRUE_DAMAGE));
				thisNpc.setNextSpotAnim(new SpotAnim(EXPLODE_GFX));
				startedTimer = false;
			}
		}, 25);
	}

}
