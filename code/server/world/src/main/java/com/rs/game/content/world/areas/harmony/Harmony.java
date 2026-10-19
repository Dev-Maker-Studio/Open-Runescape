package com.rs.game.content.world.areas.harmony;

import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class Harmony {
	//Harmony
	public static ObjectClickHandler handleharmonystairs = new ObjectClickHandler(new Object[] { 22247, 22253 }, e -> {
		if (e.getObjectId() == 22247)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? 3 : e.getObj().getRotation() == 3 ? -3 : 0, e.getObj().getRotation() == 2 ? -3 : e.getObj().getRotation() == 0 ? 3 : 0,  1));
		else if (e.getObjectId() == 22253)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? -3 : e.getObj().getRotation() == 3 ? 3 : 0, e.getObj().getRotation() == 2 ? 3 : e.getObj().getRotation() == 0 ? -3 : 0, -1));
	});
	public static ObjectClickHandler handleharmonystairs2 = new ObjectClickHandler(new Object[] { 22248, 22254 }, e -> {
		if (e.getObjectId() == 22248)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? -3 : e.getObj().getRotation() == 3 ? -3 : 0, e.getObj().getRotation() == 2 ? -3 : e.getObj().getRotation() == 0 ? 3 : 0,  1));
		else if (e.getObjectId() == 22254)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 1 ? 3 : e.getObj().getRotation() == 3 ? 3 : 0, e.getObj().getRotation() == 2 ? 3 : e.getObj().getRotation() == 0 ? -3 : 0, -1));
	});

	public static ObjectClickHandler handlepirateshipladders = new ObjectClickHandler(new Object[] { 16945, 16946, 16947 }, e -> {
		if (e.getObjectId() == 16945)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 0 ? 3 : e.getObj().getRotation() == 2 ? -3 : 0, e.getObj().getRotation() == 3 ? 3 : e.getObj().getRotation() == 1 ? -3 : 0, 1));
		else if (e.getObjectId() == 16947)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 0 ? -3 : e.getObj().getRotation() == 2 ? 3 : 0, e.getObj().getRotation() == 3 ? -3 : e.getObj().getRotation() == 1 ? 3 : 0, -1));
		else if (e.getObjectId() == 16946)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 2 ? -3 : e.getObj().getRotation() == 0 ? 3 : 0, e.getObj().getRotation() == 1 ? -3 : e.getObj().getRotation() == 3 ? 3 : 0, 1));

	});



}
