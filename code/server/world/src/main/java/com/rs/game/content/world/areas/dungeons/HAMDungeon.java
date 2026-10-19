package com.rs.game.content.world.areas.dungeons;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.LoginHandler;
import com.rs.plugin.handlers.ObjectClickHandler;
import org.darkan.core.gameval.gen.Varbit;

@PluginEventHandler
public class HAMDungeon {

	public static LoginHandler unlockTrapdoor = new LoginHandler(e -> e.getPlayer().getVars().setVarBit(Varbit.dttd_ham_trapdoor_state, 2));

	public static ObjectClickHandler handleKeyTrapdoor = new ObjectClickHandler(new Object[] { 15766, 15747 }, e -> {
		if (e.getObjectId() == 15766) {
			if (e.getOption().equals("Climb-down"))
				e.getPlayer().useLadder(Tile.of(2568, 5185, 0));
		} else
			e.getPlayer().useLadder(Tile.of(3166, 9623, 0));
	});
}
