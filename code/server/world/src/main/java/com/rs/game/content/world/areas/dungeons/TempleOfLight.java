package com.rs.game.content.world.areas.dungeons;

import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class TempleOfLight {

	//Temple of Light
	public static ObjectClickHandler handletempleoflightstairswide = new ObjectClickHandler(new Object[] { 10015, 10016 }, e -> {
		if (e.getObjectId() == 10015)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? 4 : e.getObj().getRotation() == 3 ? -4 : 0, e.getObj().getRotation() == 0 ? 4 : e.getObj().getRotation() == 2 ? -4 : 0, 1));
		else if (e.getObjectId() == 10016)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? -4 : e.getObj().getRotation() == 3 ? 4 : 0, e.getObj().getRotation() == 0 ? -4 : e.getObj().getRotation() == 2 ? 4 : 0, -1));
	});

	public static ObjectClickHandler handletempleoflightstairsthin = new ObjectClickHandler(new Object[] { 10018, 10017 }, e -> {
		if (e.getObjectId() == 10018)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? -4 : e.getObj().getRotation() == 3 ? 4 : 0, e.getObj().getRotation() == 0 ? -4 : e.getObj().getRotation() == 2 ? 4 : 0, -1));
		else if (e.getObjectId() == 10017)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? 4 : e.getObj().getRotation() == 3 ? -4 : 0, e.getObj().getRotation() == 0 ? 4 : e.getObj().getRotation() == 2 ? -4 : 0, 1));
	});



}
