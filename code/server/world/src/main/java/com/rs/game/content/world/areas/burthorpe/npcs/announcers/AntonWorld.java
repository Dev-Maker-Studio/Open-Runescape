package com.rs.game.content.world.areas.burthorpe.npcs.announcers;

import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class AntonWorld extends NPC {
	private static final int npcId = 4295;

	public AntonWorld(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (Utils.random(50) == 0)
			setNextForceTalk(new ForceTalk("Imported weapons from the finest smithys around the lands!"));
		if (Utils.random(50) == 25)
			setNextForceTalk(new ForceTalk("Ow my toe! That armour is heavy."));
		if (Utils.random(50) == 50)
			setNextForceTalk(new ForceTalk("Armour and axes to suit your needs."));
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { npcId }, AntonWorld::new);
}
