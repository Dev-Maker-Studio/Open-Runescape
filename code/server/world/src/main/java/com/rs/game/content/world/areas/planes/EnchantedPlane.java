package com.rs.game.content.world.areas.planes;

import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class EnchantedPlane {
	public static ObjectClickHandler handleEnchantedTree = new ObjectClickHandler(new Object[] { 16265 }, e -> { });
}
