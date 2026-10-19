package com.rs.game.content.world.areas.waterfall_dungeon;


import com.rs.game.content.skills.agility.Agility;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnObjectHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

//wrong animations
@PluginEventHandler
public class waterfall_dungeon {

	public static ObjectClickHandler handlewaterfallexit = new ObjectClickHandler(new Object[] { 32711 }, e -> e.getPlayer().tele(Tile.of(2511, 3463, 0)));

}
