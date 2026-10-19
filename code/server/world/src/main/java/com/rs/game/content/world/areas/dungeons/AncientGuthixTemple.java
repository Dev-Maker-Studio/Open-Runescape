package com.rs.game.content.world.areas.dungeons;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class AncientGuthixTemple {

	public static ObjectClickHandler handleClimbWall = new ObjectClickHandler(false, new Object[] { 40261, 40262 }, e -> e.getPlayer().walkToAndExecute(e.getObj().getTile().transform(0, 1, 0), () -> e.getPlayer().useStairs(-1, e.getPlayer().transform(0, -1, 1), 1, 1)));

	public static ObjectClickHandler handleJumpDownWall = new ObjectClickHandler(new Object[] { 40849 }, e -> e.getPlayer().useStairs(-1, e.getPlayer().transform(0, 1, -1), 1, 1));

	public static ObjectClickHandler handleSkullEntrance = new ObjectClickHandler(new Object[] { 48248 }, e -> e.getPlayer().useStairs(-1, Tile.of(2525, 5810, 0), 1, 1));

	public static ObjectClickHandler handleSkullExit = new ObjectClickHandler(new Object[] { 41077 }, e -> e.getPlayer().useStairs(-1, Tile.of(2527, 5830, 2), 1, 1));

}
