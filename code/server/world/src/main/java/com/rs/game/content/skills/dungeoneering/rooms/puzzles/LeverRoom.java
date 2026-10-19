package com.rs.game.content.skills.dungeoneering.rooms.puzzles;

import com.rs.game.World;
import com.rs.game.content.skills.dungeoneering.rooms.PuzzleRoom;
import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;

public class LeverRoom extends PuzzleRoom {

	/*
	private static final int[] SWITCH_UP = {
	49381, 49382, 49383, 54333, 33675
	};
	 */

	private static final int[] SWITCH_DOWN = { 49384, 49385, 49386, 49386, 49386 }; //TODO find down of 54333, 33675

	private int leverCount, leverTicks, maxTicks;
	private Task resetTask;

	@Override
	public void openRoom() {
		manager.spawnRandomNPCS(reference);
	}

	@Override
	public boolean processObjectClick1(Player player, GameObject object) {
		if (object.getDefinitions().getName().equals("Switch")) {
			player.setNextAnimation(new Animation(3611));
			if (isComplete()) {
				player.sendMessage("The lever doesn't seem to respond.");
				return false;
			}
			if (resetTask == null)
				addResetTask();
			GameObject down = new GameObject(object);
			down.setId(SWITCH_DOWN[type.ordinal()]);
			World.spawnObjectTemporary(down, (maxTicks - leverTicks));
			leverCount++;
			return false;
		}
		return true;
	}

	private void addResetTask() {
		//Still want it to be possible when people leave a 5:5 (4:5), and very easy on a 5:1
		int size =  manager.getParty().getTeam().size();
		int difficulty = Math.min(manager.getParty().getDificulty(), size);
		//5 - 2.4 seconds
		//4 - 3.6 seconds
		//3 - 5.4 seconds
		//2 - 7.2 seconds
		//1 - 14.4 seconds
		maxTicks = (6 - difficulty) + ((size == 1 ? 23 : 20) / difficulty);
		resetTask = new ResetTask();
		WorldTasks.scheduleLooping(resetTask, 0, 0);
	}

	private void resetTask() {
		leverTicks = 0;
		maxTicks = 0;
		resetTask = null;
	}

	private class ResetTask extends Task {

		@Override
		public void run() {
			if (manager == null || manager.isDestroyed()) {
				stop();
				return;
			}
			if (leverCount == 5) {
				setComplete();
				resetTask();
				stop();
				return;
			}

			leverTicks++;
			if (leverTicks >= maxTicks) {
				resetTask();
				if (leverCount != 5) {
					leverCount = 0;

					for (Player player : manager.getParty().getTeam()) {
						player.sendMessage("You hear a loud noise and all the switches toggle back off.");
						if (player.withinDistance(manager.getTile(reference, 7, 8), 2) || !manager.getCurrentRoomReference(Tile.of(player.getTile())).equals(reference))
							continue;
						World.sendSpotAnim(Tile.of(player.getTile()), new SpotAnim(2759));
						player.setNextAnimation(new Animation(13694));
						player.applyHit(new Hit(player, (int) (player.getMaxHitpoints() * .3), HitLook.TRUE_DAMAGE));
					}
					stop();
					return;
				}
			}
		}
	}

	@Override
	public String getCompleteMessage() {
		return "As the last lever is pulled, you hear a click. All the doors in the room are now unlocked.";
	}
}
