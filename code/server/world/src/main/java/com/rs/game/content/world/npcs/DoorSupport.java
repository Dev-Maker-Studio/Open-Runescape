package com.rs.game.content.world.npcs;

import com.rs.game.World;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.tasks.WorldTasks;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Logger;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;
import com.rs.utils.Ticks;

@PluginEventHandler
public class DoorSupport extends NPC {

	public DoorSupport(int id, Tile tile) {
		super(id, tile, true);
		setCantFollowUnderCombat(true);
	}

	@Override
	public void processNPC() {
		stopFaceEntity();
	}

	public boolean canDestroy(Player player) {
		if (getId() == 2446)
			return player.getY() < getY();
		if (getId() == 2440)
			return player.getY() > getY();
			return player.getX() > getX();
	}

	@Override
	public boolean ignoreWallsWhenMeleeing() {
		return true;
	}

	@Override
	public void sendDeath(Entity killer) {
		transformIntoNPC(getId() + 1);
		final GameObject door = World.getObjectWithId(getTile(), 8967);
		if (door != null)
			World.removeObject(door);
		WorldTasks.schedule(Ticks.fromSeconds(60), () -> {
			try {
				transformIntoNPC(getId() - 1);
				reset();
				if (door != null)
					World.spawnObject(door);
			} catch (Throwable e) {
				Logger.handle(DoorSupport.class, "DoorSupport.sendDeath", e);
			}
		});
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 2440, 2443, 2446 }, DoorSupport::new);
}
