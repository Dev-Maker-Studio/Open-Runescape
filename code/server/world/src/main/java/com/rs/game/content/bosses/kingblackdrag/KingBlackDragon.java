package com.rs.game.content.bosses.kingblackdrag;

import com.rs.game.model.entity.npc.NPC;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class KingBlackDragon extends NPC {

	public KingBlackDragon(int id, Tile tile, boolean spawned) {
		super(id, tile, spawned);
		setLureDelay(3000);
		setIntelligentRouteFinder(true);
		setIgnoreDocile(true);
	}

	public static boolean atKBD(Tile tile) {
		if ((tile.getX() >= 2250 && tile.getX() <= 2292) && (tile.getY() >= 4675 && tile.getY() <= 4710))
			return true;
		return false;
	}

	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(new Object[] { 50, 2642 }, (npcId, tile) -> new KingBlackDragon(npcId, tile, false));
}
