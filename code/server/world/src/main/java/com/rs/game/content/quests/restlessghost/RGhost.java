package com.rs.game.content.quests.restlessghost;

import com.rs.engine.quest.Quest;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class RGhost extends NPC {

	public RGhost(int id, Tile tile) {
		super(id, tile);
	}

	@Override
	public boolean withinDistance(Player tile, int distance) {
		return tile.getQuestManager().getStage(Quest.RESTLESS_GHOST) == 3 && super.withinDistance(tile, distance);
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(457, RGhost::new);
}
