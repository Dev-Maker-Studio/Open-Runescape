package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

import java.util.Map;

public class PlayerFaceTileAction extends CutsceneAction {

	private final int x;
    private final int y;

	public PlayerFaceTileAction(int x, int y, int actionDelay) {
		super(null, actionDelay);
		this.x = x;
		this.y = y;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");
		player.setNextFaceTile(Tile.of(scene.getX(x), scene.getY(y), player.getPlane()));
	}

}
