package com.rs.game.content.skills.dungeoneering.npcs;

import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.RoomReference;
import com.rs.game.model.entity.Entity;
import world.gregs.voidps.type.Tile;

public class DungeonSkeletonBoss extends DungeonNPC {

	private final DivineSkinweaver boss;

	public DungeonSkeletonBoss(int id, Tile tile, DungeonManager manager, RoomReference reference) {
		super(id, tile, manager);
		setForceAgressive(true);
		setIntelligentRouteFinder(true);
		setLureDelay(0);
		boss = (DivineSkinweaver) getNPC(10058);
	}

	@Override
	public void sendDeath(Entity source) {
		super.sendDeath(source);
		boss.removeSkeleton(this);
	}

	@Override
	public void drop() {

	}

}
