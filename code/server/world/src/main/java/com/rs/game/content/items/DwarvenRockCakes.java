package com.rs.game.content.items;

import com.rs.game.model.entity.Hit;
import com.rs.game.model.entity.Hit.HitLook;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemClickHandler;

@PluginEventHandler
public class DwarvenRockCakes {

	public static ItemClickHandler handleRockCakeClick = new ItemClickHandler(new Object[] { 7509, 7510 }, new String[] { "Eat" }, e -> {
		if (e.getItem().getId() == 7509 && e.getPlayer().getHitpoints() > 20) {
			Hit h = new Hit(20, HitLook.TRUE_DAMAGE);
			e.getPlayer().removeHitpoints(h);
			e.getPlayer().fakeHit(h);
			e.getPlayer().forceTalk("Ow! Ow! That's hot!");
		} else if (e.getItem().getId() == 7510 && e.getPlayer().getHitpoints() > 100) {
			Hit h = new Hit(100, HitLook.TRUE_DAMAGE);
			e.getPlayer().removeHitpoints(h);
			e.getPlayer().fakeHit(h);
			e.getPlayer().forceTalk("Ow! I nearly broke a tooth!");
		}
	});

}
