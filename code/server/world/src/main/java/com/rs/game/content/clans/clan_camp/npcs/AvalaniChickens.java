package com.rs.game.content.clans.clan_camp.npcs;

import com.rs.game.model.entity.ForceTalk;
import com.rs.game.model.entity.npc.NPC;
import org.darkan.core.model.Animation;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class AvalaniChickens extends NPC {

	public AvalaniChickens(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public void processNPC() {
		super.processNPC();
		if (Utils.random(100) == 0) {
			setNextAnimation(new Animation(3831));
			setNextForceTalk(new ForceTalk("Fly, my pretty, fly!"));
		}
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 13827 }, AvalaniChickens::new);
}
