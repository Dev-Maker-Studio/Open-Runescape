package com.rs.game.content.skills.dungeoneering.skills;

import com.rs.game.content.skills.dungeoneering.DungeonConstants;
import com.rs.game.content.skills.dungeoneering.DungeonManager;
import com.rs.game.content.skills.dungeoneering.skills.DungeoneeringFarming.Harvest;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.model.gameobject.OwnedObject;

public class DungFarmPatch extends OwnedObject {

	private int time;
	private int stage = 1;
	private final Harvest harvest;
	private final DungeonManager manager;

	public DungFarmPatch(Player player, Harvest harvest, GameObject basePatch, DungeonManager manager) {
		super(player, DungeonConstants.EMPTY_FARMING_PATCH + 1 + 1 + (harvest.ordinal() * 3), basePatch.getType(), basePatch.getRotation(), basePatch.getTile());
		this.harvest = harvest;
		this.manager = manager;
	}

	@Override
	public void onDestroy() {
		manager.getFarmingPatches().remove(this);
	}

	@Override
	public void tick(Player owner) {
		time++;
		if (time >= 50 && stage < 3) {
			time = 0;
			stage++;
			changeId(DungeonConstants.EMPTY_FARMING_PATCH + 1 + stage + (harvest.ordinal() * 3));
		}
	}
}
