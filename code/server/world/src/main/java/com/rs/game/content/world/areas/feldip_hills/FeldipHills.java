package com.rs.game.content.world.areas.feldip_hills;

import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class FeldipHills {
	public static ObjectClickHandler handleRantzCaves = new ObjectClickHandler(new Object[] { 3379, 32068, 32069 }, e -> e.getPlayer().useStairs(e.getObjectId() == 3379 ? Tile.of(2646, 9378, 0) : Tile.of(2631, 2997, 0)));
	public static ObjectClickHandler handleStairsAndTunnel = new ObjectClickHandler(new Object[]{2811, 2812}, e -> {
		Tile destination = (e.getObjectId() == 2812) ? Tile.of(2501, 2989, 0) : Tile.of(2574, 3029, 0);
		e.getPlayer().useStairs(827, destination);
		WorldTasks.schedule(() -> e.getPlayer().playerDialogue(HeadE.AMAZED, "Wow! That tunnel went a long way."));
	});

}
