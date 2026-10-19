package com.rs.game.content.skills.dungeoneering.npcs.misc;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.npcs.DungeonNPC;
import com.rs.game.content.skills.dungeoneering.npcs.MastyxTrap;
import com.rs.game.content.skills.dungeoneering.skills.DungeoneeringTraps;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;

import java.util.List;

public class DungeonHunterNPC extends DungeonNPC {

	public DungeonHunterNPC(int id, Tile tile, DungeonManager manager) {
		super(id, tile, manager);
	}

	@Override
	public void processNPC() {
		if (isCantInteract() || getId() >= 11096 || getManager() == null)
			return;
		super.processNPC();
		List<MastyxTrap> traps = getManager().getMastyxTraps();
		if (traps.isEmpty())
			return;
		final int tier = DungeoneeringTraps.getNPCTier(getId());
		for (final MastyxTrap trap : traps) {
			if (!withinDistance(trap.getTile(), 3) || Utils.getRandomInclusive(3) != 0)
				continue;
			trap.setCantInteract(true);
			setCantInteract(true);
			setNextFaceEntity(trap);
			addWalkSteps(trap.getX() + 1, trap.getY() + 1);

			final int trap_tier = trap.getTier();
			double successRatio = getSuccessRatio(tier, trap_tier);
			final boolean failed = successRatio < Math.random();

			setCantInteract(true);
			if (failed)
				WorldTasks.scheduleTimer((ticks) -> {
					if (ticks == 5)
						anim(13264);
					else if (ticks == 8) {
						trap.transformIntoNPC(1957);
						trap.spotAnim(2561 + trap_tier);
					} else if (ticks == 16) {
						getManager().removeMastyxTrap(trap);
						setCantInteract(false);
						return false;
					}
					return true;
				});
			else
				WorldTasks.scheduleTimer((ticks) -> {
					if (ticks == 9) {
						trap.transformIntoNPC(1957);
						trap.spotAnim(2551 + trap_tier);
					} else if (ticks == 13)
						anim(13260);
					else if (ticks == 18)
						transformIntoNPC(getId() + 10);
					else if (ticks == 20) {
						setCantInteract(false);
						getManager().removeMastyxTrap(trap);
						return false;
					}
					return true;
				});
		}
	}

	@Override
	public void drop() {
		/*World.addGroundItem(new Item(532), Tile.of(this)); //big bones
		Drops drops = NPCDrops.getDrops(getId());
		if (drops == null)
			return;
		Drop drop = drops.getDrop(Drops.COMMOM, Double.MAX_VALUE); //to make 100% chance
		if (drop == null) //shouldnt
			return;
		World.addGroundItem(new Item(drop.getItemId()), Tile.of(this)); //hide*/
	}

	private static double getSuccessRatio(int tier, int trapTier) {
		double successRatio = 0.0;
		int tierProduct = trapTier - tier;
		if (tierProduct == 0)
			successRatio = 0.5;
		else if (tierProduct > 0)
			successRatio = 0.5 + (tierProduct / 10.0);

		if (successRatio > 0.9)
			successRatio = 0.9;
		return successRatio;
	}
}
