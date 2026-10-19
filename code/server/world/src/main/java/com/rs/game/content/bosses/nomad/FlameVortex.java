package com.rs.game.content.bosses.nomad;

import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class FlameVortex extends NPC {

	private long explodeTime;

	public FlameVortex(Tile tile) {
		this(9441, tile, true);
	}

	public FlameVortex(int id, Tile tile, boolean spawned) {
		super(id, tile, spawned);
		explodeTime = System.currentTimeMillis() + 60000;
		setNextAnimation(new Animation(12720));
	}

	@Override
	public void processNPC() {
		if (explodeTime == -1)
			return;
		Player target = getTargetToCheck();
		if (target != null
				&& ((target.getX() == getX() && target.getY() == getY()) || (target.getNextRunDirection() != null && target.getX() - target.getNextRunDirection().dx == getX() && target.getY()
				- target.getNextRunDirection().dy == getY())))
			explode(target, 400);
		else if (explodeTime < System.currentTimeMillis())
			explode(target != null && withinDistance(target, 1) ? target : null, Utils.random(400, 701));
	}

	public void explode(final Player target, final int damage) {
		explodeTime = -1;
		final NPC npc = this;
		WorldTasks.scheduleLooping(new Task() {

			private boolean secondLoop;

			@Override
			public void run() {
				if (!secondLoop) {
					setNextAnimation(new Animation(12722));
					if (target != null) {
						target.applyHit(new Hit(npc, damage, HitLook.TRUE_DAMAGE));
						target.setRunEnergy(0);
						target.setNextForceTalk(new ForceTalk("Aiiiiiieeeee!"));
					}
					secondLoop = true;
				} else {
					finish();
					stop();
				}
			}
		}, 0, 0);
	}

	public Player getTargetToCheck() {
		for (Player player : queryNearbyPlayersByTileRange(0, player -> true))
			return player;
		return null;
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(9441, (npcId, tile) -> new FlameVortex(npcId, tile, false));
}
