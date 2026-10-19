package com.rs.game.content.skills.dungeoneering.skills;

import com.rs.game.World;
import com.rs.game.map.ChunkManager;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.Utils;

public final class DungeoneeringWoodcutting extends PlayerAction {

	private final GameObject treeObj;
	private final DungTree type;
	private DungHatchet hatchet;

	public DungeoneeringWoodcutting(GameObject treeObj, DungTree type) {
		this.treeObj = treeObj;
		this.type = type;
	}

	@Override
	public boolean start(Player player) {
		if (!checkAll(player))
			return false;
		player.sendMessage("You swing your hatchet at the tree...", true);
		setActionDelay(player, 4);
		return true;
	}

	private boolean checkAll(Player player) {
		hatchet = DungHatchet.getHatchet(player);
		if (hatchet == null) {
			player.sendMessage("You dont have the required level to use that axe or you don't have a hatchet.");
			return false;
		}
		if (!hasWoodcuttingLevel(player))
			return false;
		if (!player.getInventory().hasFreeSlots()) {
			player.sendMessage("Not enough space in your inventory.");
			return false;
		}
		return true;
	}

	private boolean hasWoodcuttingLevel(Player player) {
		if (type.getLevel() > player.getSkills().getLevel(8)) {
			player.sendMessage("You need a woodcutting level of " + type.getLevel() + " to chop down this tree.");
			return false;
		}
		return true;
	}

	@Override
	public boolean process(Player player) {
		player.setNextAnimation(new Animation(hatchet.getEmoteId()));
		return checkTree(player);
	}

	@Override
	public int processWithDelay(Player player) {
		if (type.rollSuccess(player.getSkills().getLevel(Constants.WOODCUTTING), hatchet)) {
			type.giveLog(player);
			if (Utils.random(8) == 0) {
				World.spawnObject(new GameObject(treeObj.getId() + 1, treeObj.getType(), treeObj.getRotation(), treeObj.getTile()));
				player.sendMessage("You have depleted this resource.");
				return -1;
			}
		}
		return 4;
	}

	private boolean checkTree(Player player) {
		return ChunkManager.getChunk(treeObj.getTile().getChunkId()).objectExists(treeObj);
	}

	@Override
	public void stop(Player player) {
		setActionDelay(player, 4);
	}
}
