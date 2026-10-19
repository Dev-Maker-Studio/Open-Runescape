package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

import java.util.Map;

public class NPCFaceTileAction extends CutsceneAction {

	private final int x;
    private final int y;

	public NPCFaceTileAction(String key, int x, int y, int actionDelay) {
		super(key, actionDelay);
		this.x = x;
		this.y = y;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");;
		NPC npc = (NPC) objects.get(getObjectKey());
		npc.setNextFaceTile(Tile.of(scene.getX(x), scene.getY(y), npc.getPlane()));
	}

}
