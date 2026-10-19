package com.rs.game.content.world.areas.catherby;

import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class Catherby {

	public static ObjectClickHandler taverlyDungeonClimbToWaterObelisk = new ObjectClickHandler(new Object[] { 32015 }, new Tile[] { Tile.of(2842, 9824, 0) }, e -> e.getPlayer().ladder(Tile.of(2842, 3423, 0)));

	public static ObjectClickHandler ArheinsShip = new ObjectClickHandler(new Object[] { 69 }, new Tile[] { Tile.of(2805, 3421, 0) }, e -> {
		int npcId = 563;
		e.getPlayer().startConversation(new Dialogue()
				.addNPC(npcId, HeadE.ANGRY, "Hey buddy! Get away from my ship alright?")
				.addPlayer(HeadE.SAD, "Yeah... uh... sorry...")
		);
	});
	
}
