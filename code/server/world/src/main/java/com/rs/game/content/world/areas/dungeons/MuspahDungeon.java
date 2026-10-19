package com.rs.game.content.world.areas.dungeons;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class MuspahDungeon {

	public static ObjectClickHandler handleExit = new ObjectClickHandler(new Object[] { 42891 }, e -> e.getPlayer().tele(Tile.of(2736, 3729, 0)));

	public static ObjectClickHandler handleEntrance = new ObjectClickHandler(new Object[] { 42793 }, e -> {
		//subtract 64 from x if muspah has not escaped
		e.getPlayer().tele(Tile.of(3485, 5511, 0));
	});

	public static ObjectClickHandler handleOpenings = new ObjectClickHandler(new Object[] { 42794, 42795 }, e -> e.getPlayer().tele(e.getPlayer().transform(0, e.getObjectId() == 42794 ? 8 : -8, 0)));

	public static ObjectClickHandler handleEnterIceStryke = new ObjectClickHandler(new Object[] { 48188 }, e -> e.getPlayer().tele(Tile.of(3435, 5646, 0)));

	public static ObjectClickHandler handleExitIceStryke = new ObjectClickHandler(new Object[] { 48189 }, e -> e.getPlayer().tele(Tile.of(3509, 5515, 0)));
}
