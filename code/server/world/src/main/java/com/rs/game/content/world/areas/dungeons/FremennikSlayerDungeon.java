package com.rs.game.content.world.areas.dungeons;

import com.rs.game.content.skills.agility.Agility;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class FremennikSlayerDungeon {

	public static ObjectClickHandler handleChasm = new ObjectClickHandler(false, new Object[] { 44339 }, e -> {
		if (!Agility.hasLevel(e.getPlayer(), 81))
			return;
		final Tile toTile = e.getPlayer().getX() < 2772 ? Tile.of(2775, 10002, 0) : Tile.of(2768, 10002, 0);
		e.getPlayer().walkToAndExecute(e.getPlayer().getX() > 2772 ? Tile.of(2775, 10002, 0) : Tile.of(2768, 10002, 0), () -> e.getPlayer().forceMove(toTile, 4721, 20, 85, () -> e.getPlayer().anim(-1)));
	});

	public static ObjectClickHandler handleShortcut2 = new ObjectClickHandler(new Object[] { 9321 }, e -> {
		if (!Agility.hasLevel(e.getPlayer(), 62))
			return;
		e.getPlayer().forceMove(e.getPlayer().transform(e.getObj().getRotation() == 0 ? 5 : -5, 0), 3844, 25, 75);
	});
}
