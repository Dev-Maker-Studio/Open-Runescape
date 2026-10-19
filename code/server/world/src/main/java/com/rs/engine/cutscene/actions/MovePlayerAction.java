package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.MoveType;
import world.gregs.voidps.type.Tile;

import java.util.Map;

public class MovePlayerAction extends CutsceneAction {

	private final int x;
    private final int y;
    private final int plane;
	private final MoveType movementType;

	public MovePlayerAction(int x, int y, int plane, MoveType movementType, int actionDelay) {
		super(null, actionDelay);
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.movementType = movementType;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");
		if (movementType == MoveType.TELEPORT) {
			player.tele(Tile.of(scene.getX(x), scene.getY(y), plane));
			return;
		}
		player.setRun(movementType == MoveType.RUN);
		player.addWalkSteps(scene.getX(x), scene.getY(y), 25, false);
	}

}
