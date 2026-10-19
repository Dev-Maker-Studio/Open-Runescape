package com.rs.game.content.world.areas.fisher_realm;

import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class FisherRealm {
	//Fisher Realm
	public static ObjectClickHandler handlefisherkingstairs = new ObjectClickHandler(new Object[] { 1730, 1731 }, e -> {
		if (e.getObjectId() == 1730)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 0 ? -0 : e.getObj().getRotation() == 3 ? -0 : 0, e.getObj().getRotation() == 0 ? 4 : e.getObj().getRotation() == 3 ? -0 : 0, 1));
		else if (e.getObjectId() == 1731)
			e.getPlayer().tele(e.getPlayer().transform(e.getObj().getRotation() == 0 ? 0 : e.getObj().getRotation() == 3 ? -0 : 0, e.getObj().getRotation() == 0 ? -4 : e.getObj().getRotation() == 3 ? 3 : 0, -1));

	});

}
