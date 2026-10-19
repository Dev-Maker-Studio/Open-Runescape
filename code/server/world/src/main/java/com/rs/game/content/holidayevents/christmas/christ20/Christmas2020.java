package com.rs.game.content.holidayevents.christmas.christ20;

import org.darkan.core.model.Animation;
import org.darkan.core.model.SpotAnim;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.annotations.ServerStartupEvent.Priority;
import com.rs.plugin.handlers.ItemClickHandler;
import com.rs.plugin.handlers.LoginHandler;
import com.rs.utils.spawns.NPCSpawn;
import com.rs.utils.spawns.NPCSpawns;
import org.darkan.core.gameval.gen.Varbit;

@PluginEventHandler
public class Christmas2020 {

	public static final String STAGE_KEY = "christ2025";

	private static final boolean ACTIVE = false;

	@ServerStartupEvent(Priority.FILE_IO)
	public static void load() {
		if (!ACTIVE)
			return;
		NPCSpawns.add(new NPCSpawn(9398, Tile.of(2655, 5678, 0), "Queen of Snow"));
		NPCSpawns.add(new NPCSpawn(9400, Tile.of(2654, 5679, 0), "Santa"));
	}

	public static LoginHandler login = new LoginHandler(e -> {
		if (!ACTIVE)
			return;
		e.getPlayer().getVars().setVarBit(Varbit.xmas09_santa_free, 1);
	});
}
