package com.rs.game.content.world.areas.burthorpe.npcs.announcers;

import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class LidioWorld extends NPC {
	private static final int npcId = 4293;

	public LidioWorld(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (Utils.random(50) == 0)
			setNextForceTalk(new ForceTalk("Potatoes are filling and healthy too!"));
		if (Utils.random(50) == 25)
			setNextForceTalk(new ForceTalk("Come try my lovely pizza or maybe some fish!"));
		if (Utils.random(50) == 50)
			setNextForceTalk(new ForceTalk("Stew to fill the belly, on sale here!"));
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { npcId }, LidioWorld::new);
}
