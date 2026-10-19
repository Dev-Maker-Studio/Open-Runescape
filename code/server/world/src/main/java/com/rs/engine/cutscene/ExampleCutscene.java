package com.rs.engine.cutscene;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.MoveType;

public class ExampleCutscene extends Cutscene {
	
	@Override
	public void construct(Player player) {
		fadeIn(5);
		dynamicRegion(player.getTile(), 178, 554, 4, 4);
		playerMove(15, 20, 0, MoveType.TELEPORT);
		spawnObj(67500, 0, 14, 23, 0);
		npcCreate("meme", 50, 14, 20, 0, 0);
		fadeOut(5);
		action(5, () -> getNPC("meme").transformIntoNPC(13447));
		delay(20);
	}
}
