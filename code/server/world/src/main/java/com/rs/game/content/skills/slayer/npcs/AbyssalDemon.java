package com.rs.game.content.skills.slayer.npcs;

import com.rs.game.World;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.map.pathfinder.Direction;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;
import com.rs.utils.WorldUtil;

@PluginEventHandler
public class AbyssalDemon extends NPC {

	public AbyssalDemon(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void processNPC() {
		super.processNPC();
		Entity target = getCombat().getTarget();
		if (target != null && WorldUtil.isInRange(target.getX(), target.getY(), target.getSize(), getX(), getY(), getSize(), 4) && Utils.random(50) == 0)
			sendTeleport(Utils.random(2) == 0 ? target : this);
	}

	private void sendTeleport(Entity entity) {
		if (entity.isLocked())
			return;
		int entitySize = entity.getSize();
		for (int c = 0; c < 10; c++) {
			Direction dir = Direction.values()[Utils.random(Direction.values().length)];
			if (World.checkWalkStep(entity.getTile(), dir, entitySize)) {
				entity.setNextSpotAnim(new SpotAnim(409));
				entity.tele(entity.transform(dir.dx, dir.dy, 0));
				break;
			}
		}
	}

	public static NPCInstanceHandler toAbyssalDemon = new NPCInstanceHandler(new Object[] { 1615 }, AbyssalDemon::new);
}