package com.rs.game.content.world.areas.canifis.npcs;

import com.rs.game.model.entity.Entity;
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
public class Werewolf extends NPC {

	private final int realId;

	public Werewolf(int id, Tile tile, boolean spawned) {
		super(id, tile, spawned);
		realId = id;
	}

	public boolean hasWolfbane(Entity target) {
		if (target instanceof NPC)
			return false;
		return ((Player) target).getEquipment().getWeaponId() == 2952;
	}

	@Override
	public void processNPC() {
		if (isDead() || isCantInteract())
			return;
		if (isUnderCombat() && getId() == realId && Utils.random(5) == 0) {
			final Entity target = getCombat().getTarget();
			if (!hasWolfbane(target)) {
				setNextAnimation(new Animation(6554));
				setCantInteract(true);
				WorldTasks.schedule(new Task() {
					@Override
					public void run() {
						transformIntoNPC(realId - 20);
						setNextAnimation(new Animation(-1));
						setCantInteract(false);
						setCombatTarget(target);
					}
				}, 1);
				return;
			}
		}
		super.processNPC();
	}

	@Override
	public void reset() {
		setNPC(realId);
		super.reset();
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 6026, 6027, 6028, 6029, 6030, 6031, 6032, 6033, 6034, 6035, 6036, 6037, 6038, 6039, 6040, 6041, 6042, 6043, 6044, 6045 }, (npcId, tile) -> new Werewolf(npcId, tile, false));
}
