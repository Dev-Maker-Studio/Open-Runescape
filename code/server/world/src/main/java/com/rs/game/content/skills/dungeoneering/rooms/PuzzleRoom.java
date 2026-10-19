package com.rs.game.content.skills.dungeoneering.rooms;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.VisibleRoom;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.Constants;
import org.darkan.core.Utils;

public abstract class PuzzleRoom extends VisibleRoom {

	private boolean complete;
	private final int[] requirements = new int[25], giveXPCount = new int[25];

	public final boolean hasRequirement(Player p, int skill) {
		return p.getSkills().getLevel(skill) >= getRequirement(skill);
	}

	public final int getRequirement(int skill) {
		setLevel(skill);
		return requirements[skill];
	}

	public final void giveXP(Player player, int skill) {
		if (giveXPCount[skill] < 4) {
			//You only gain xp for the first 4 times you do an action
			giveXPCount[skill]++;
			player.getSkills().addXp(skill, getRequirement(skill) * 5 +10);
		}
	}

	private void setLevel(int skill) {
		if (requirements[skill] == 0)
			requirements[skill] = !manager.getRoom(reference).isCritPath() ? Utils.random(30, skill == Constants.SUMMONING || skill == Constants.PRAYER ? 100 : 106) : Math.max(1, (manager.getParty().getMaxLevel(skill) - Utils.random(10)));
	}

	public void replaceObject(GameObject object, int newId) {
		if(object == null)
			return;
		GameObject newObject = new GameObject(object);
		newObject.changeId(newId);
		World.spawnObject(newObject);
	}

	public boolean isComplete() {
		return complete;
	}

	public void setComplete() {
		complete = true;
		if (getCompleteMessage() != null)
			manager.message(reference, getCompleteMessage());
		manager.getRoom(reference).removeChallengeDoors();
	}

	public String getCompleteMessage() {
		return "You hear a clunk as the doors unlock.";
	}

	public String getLockMessage() {
		return "The door is locked. You can't see any obvious keyhole or mechanism.";
	}

}
