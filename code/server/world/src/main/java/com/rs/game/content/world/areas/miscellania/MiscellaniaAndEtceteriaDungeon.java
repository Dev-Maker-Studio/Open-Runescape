package com.rs.game.content.world.areas.miscellania;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnObjectHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class MiscellaniaAndEtceteriaDungeon {

	public static ObjectClickHandler secondcreviceentrance = new ObjectClickHandler(new Object[] { 15194 }, e -> e.getPlayer().tele(Tile.of(2585, 10262, 0)));
	public static ObjectClickHandler secondcreviceexit = new ObjectClickHandler(new Object[] { 15195 }, e -> e.getPlayer().tele(Tile.of(2585, 10259, 0)));
	public static ObjectClickHandler thirdcreviceentrance = new ObjectClickHandler(new Object[] { 15196 }, e -> e.getPlayer().tele(Tile.of(2617, 10274, 0)));
	public static ObjectClickHandler thirdcreviceexit = new ObjectClickHandler(new Object[] { 15197 }, e -> e.getPlayer().tele(Tile.of(2617, 10271, 0)));
	public static ObjectClickHandler takeropecrate = new ObjectClickHandler(new Object[] { 15245 }, e -> e.getPlayer().tele(Tile.of(2509, 10288, 1)));
	public static ObjectClickHandler crawltunnelentrance = new ObjectClickHandler(new Object[] { 15188 }, e -> e.getPlayer().tele(Tile.of(2514, 10291, 0)));
	public static ObjectClickHandler crawltunnelexit = new ObjectClickHandler(new Object[] { 15189 }, e -> e.getPlayer().tele(Tile.of(2510, 10287, 1)));
	public static ItemOnObjectHandler TEMPneedropeswingrock = new ItemOnObjectHandler(new Object[] { 15252 }, new Object[] { 954 }, e -> {
		if (e.getPlayer().getX() == 2538 && e.getPlayer().getY() == 10299) {
			e.getPlayer().tele(Tile.of(2543, 10299, 0));
		} else
			e.getPlayer().sendMessage("You are too far away to do this.");
	});
	public static ObjectClickHandler ropeswingexit = new ObjectClickHandler(new Object[] { 15216 }, e -> e.getPlayer().tele(Tile.of(2536, 10296, 0)));
	public static ItemOnObjectHandler TEMPlightropeup = new ItemOnObjectHandler(new Object[] { 15193 }, new Object[] { 954 }, e -> e.getPlayer().tele(Tile.of(2620, 3864, 0)));
	public static ItemOnObjectHandler TEMPlightropedown = new ItemOnObjectHandler(new Object[] { 15200 }, new Object[] { 954 }, e -> e.getPlayer().tele(Tile.of(2617, 10265, 0)));



}
