package com.rs.game.content.world;

import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class DepositChests {

	public static ObjectClickHandler handleDepositChests = new ObjectClickHandler(new Object[] { "Deposit chest" }, e -> {
		if (e.getOption().equals("Deposit"))
			e.getPlayer().getBank().openDepositBox();
	});

}
