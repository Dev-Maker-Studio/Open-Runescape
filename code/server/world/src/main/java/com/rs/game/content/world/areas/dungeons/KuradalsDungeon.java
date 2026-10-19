package com.rs.game.content.world.areas.dungeons;

import org.darkan.core.map.pathfinder.Direction;
import com.rs.game.content.skills.slayer.TaskMonster;
import com.rs.game.content.world.areas.dungeons.ancientcavern.KuradalDungeonController;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.Skills;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class KuradalsDungeon {

	public static ObjectClickHandler handleBarriers = new ObjectClickHandler(new Object[] { 47236 }, e -> {
		if (e.getObj().getRotation() == 2)
			e.getPlayer().walkOneStep(e.getPlayer().getX() > e.getObj().getX() ? -1 : 1, 0, false);
		else
			e.getPlayer().walkOneStep(0, e.getPlayer().getY() == e.getObj().getY() ? -1 : 1, false);
	});

	public static ObjectClickHandler handleWallRunShortcut = new ObjectClickHandler(new Object[] { 47237 }, e -> {
		Player p = e.getPlayer();

		if(p.getSkills().getLevel(Skills.AGILITY) < 90) {
			p.getPackets().sendGameMessage("You need level 90 agility to use this shortcut.");
			return;
		}

		WorldTasks.scheduleLooping(new Task() {
			int ticks = 0;
			boolean isPlayerNorth = true;

			@Override
			public void run() {
				if(ticks == 0)
					if (p.getX() == 1641 && p.getY() == 5268) {
						p.setNextAnimation(new Animation(2929));
						isPlayerNorth = true;
					}
					else if (p.getX() == 1641 && p.getY() == 5260) {
						p.setNextAnimation(new Animation(2922));
						isPlayerNorth = false;
					} else
						return;

				if (ticks == 1) {
					p.forceMove(isPlayerNorth ? Tile.of(1641, 5260, 0) : Tile.of(1641, 5268, 0), 0, 30);
					stop();
				}
				ticks++;
			}
		}, 0, 1);
	});

	public static ObjectClickHandler handleLowWall = new ObjectClickHandler(new Object[] { 47233, 47234, 47235 }, e -> {
		Player p = e.getPlayer();
		if(p.getSkills().getLevel(Skills.AGILITY) < 86) {
			p.getPackets().sendGameMessage("You need level 86 agility to use this shortcut.");
			return;
		}

		WorldTasks.scheduleLooping(new Task() {
			int ticks = 0;
			boolean isPlayerNorth = true;

			@Override
			public void run() {
				if (ticks == 0) {
					if (p.getX() == 1633 && p.getY() == 5294) {
						p.setFaceAngle(Direction.SOUTH.getAngle());
						p.setNextAnimation(new Animation(839));
						isPlayerNorth = true;
					} else if (p.getX() == 1633 && p.getY() == 5292) {
						p.setFaceAngle(Direction.NORTH.getAngle());
						p.setNextAnimation(new Animation(839));
						isPlayerNorth = false;
					} else
						return;
				} else if (ticks >= 1) {
					if (isPlayerNorth)
						p.tele(Tile.of(1633, 5292, 0));
					if (!isPlayerNorth)
						p.tele(Tile.of(1633, 5294, 0));
					stop();
				}
				ticks++;
			}

		}, 0, 1);
	});

	public static ObjectClickHandler handleEntrances = new ObjectClickHandler(new Object[] { 47232 }, e -> {
		if (e.getPlayer().getSlayer().getTask() != null) {
			TaskMonster currentTask = e.getPlayer().getSlayer().getTask().getMonster();
			switch (currentTask) {
			case HELLHOUNDS:
			case GREATER_DEMONS:
			case BLUE_DRAGONS:
			case GARGOYLES:
			case ABYSSAL_DEMONS:
			case DARK_BEASTS:
			case IRON_DRAGONS:
			case STEEL_DRAGONS:
				e.getPlayer().getControllerManager().startController(new KuradalDungeonController());
				return;
			default:
			}
		}
		e.getPlayer().sendMessage("Sorry, this dungeon is exclusive only to those who need to go in there.");
	});
}
