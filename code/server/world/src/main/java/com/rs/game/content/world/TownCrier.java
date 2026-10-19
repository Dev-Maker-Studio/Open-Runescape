package com.rs.game.content.world;

import com.rs.Settings;
import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class TownCrier extends NPC {

	public TownCrier(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void processNPC() {
		if (Settings.getConfig().getLoginMessage() != null && Utils.random(100) == 0)
			forceTalk(Settings.getConfig().getLoginMessage());
		super.processNPC();
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 6135, 6136, 6137, 6138, 6139 }, TownCrier::new);

}
