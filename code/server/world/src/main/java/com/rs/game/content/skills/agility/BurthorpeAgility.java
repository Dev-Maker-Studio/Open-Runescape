package com.rs.game.content.skills.agility;

import org.darkan.core.map.pathfinder.Direction;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class BurthorpeAgility  {
	public static ObjectClickHandler handleLogWalk = new ObjectClickHandler(new Object[] { 66894 }, e -> Agility.walkToAgility(e.getPlayer(), 155, Direction.NORTH, 6, 6, 5.5));
	public static ObjectClickHandler handleClimb1 = new ObjectClickHandler(new Object[] { 66912 }, e -> Agility.handleObstacle(e.getPlayer(), 15765, 7, Tile.of(2919, 3562, 1), 5.5));
	public static ObjectClickHandler handleRopeSwing = new ObjectClickHandler(false, new Object[] { 66904 }, e -> Agility.swingOnRopeSwing(e.getPlayer(), Tile.of(2912, 3562, 1), Tile.of(2916, 3562, 1), e.getObj(), 5.5));
	public static ObjectClickHandler handleMonkeyBars = new ObjectClickHandler(false, new Object[] { 66897 }, e -> Agility.crossMonkeybars(e.getPlayer(), Tile.of(2917, 3561, 1), Tile.of(2917, 3554, 1), 5.5));
	public static ObjectClickHandler handleShimmy = new ObjectClickHandler(new Object[] { 66909 }, e -> Agility.walkToAgility(e.getPlayer(), 2349, Direction.WEST, 4, 4, 5.5));
	public static ObjectClickHandler handleClimb2 = new ObjectClickHandler(new Object[] { 66902 }, e -> Agility.handleObstacle(e.getPlayer(), 15782, 2, Tile.of(2912, 3562, 1), 5.5));

	public static ObjectClickHandler handleJumpDown = new ObjectClickHandler(new Object[] { 66910 }, e -> {
		Agility.handleObstacle(e.getPlayer(), 2588, 1, Tile.of(2916, 3552, 0), 46);
		e.getPlayer().incrementCount("Burthorpe laps");
	});
}
