package com.rs.game.content.world.areas.global;

import org.darkan.core.cache.AnimationDefinitions;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.Skills;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import world.gregs.voidps.type.Tile;

public class AgilityShortcuts {
	public static void climbOver(Player player, Tile toTile) {
		climbOver(player, toTile, 839);
	}

	public static void climbOver(Player player, Tile toTile, int animId) {
		player.forceMove(toTile, animId, 33, AnimationDefinitions.getDefs(animId).getEmoteTime() - 5);
	}

	public static void sidestep(final Player player, Tile toTile) {
		player.lock();
		WorldTasks.schedule(1, () -> player.forceMove(toTile, 3844, 0, 120));
	}

	public static void crawlUnder(final Player player, Tile toTile) {
		player.lock();
		WorldTasks.schedule(1, () -> player.forceMove(toTile, 2589, 35, 130));
		WorldTasks.schedule(4, () -> player.anim(2591));
	}

	public static void walkLog(final Player player, Tile toTile, int delay) {
		final boolean running = player.getRun();
		player.setRunHidden(false);
		player.lock(delay);
		player.addWalkSteps(toTile.getX(), toTile.getY(), -1, false);
		player.sendMessage("You walk carefully across the slippery log...", true);
		WorldTasks.scheduleLooping(new Task() {
			boolean secondloop;

			@Override
			public void run() {
				if (!secondloop) {
					secondloop = true;
					player.getAppearance().setBAS(155);
				} else {
					player.getAppearance().setBAS(-1);
					player.setRunHidden(running);
					player.getSkills().addXp(Skills.AGILITY, 7.5);
					player.sendMessage("... and make it safely to the other side.", true);
					stop();
				}
			}
		}, 0, delay);
	}
}
