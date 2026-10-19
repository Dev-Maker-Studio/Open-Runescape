package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

import java.util.Map;

public class PosCameraAction extends CutsceneAction {

	private final int moveLocalX;
	private final int moveLocalY;
	private final int moveZ;
	private final int speed;
	private final int speed2;

	public PosCameraAction(int moveLocalX, int moveLocalY, int moveZ, int speed, int speed2, int actionDelay) {
		super(null, actionDelay);
		this.moveLocalX = moveLocalX;
		this.moveLocalY = moveLocalY;
		this.moveZ = moveZ;
		this.speed = speed;
		this.speed2 = speed2;
	}

	public PosCameraAction(int moveLocalX, int moveLocalY, int moveZ, int actionDelay) {
		this(moveLocalX, moveLocalY, moveZ, -1, -1, actionDelay);
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");
		player.getPackets().sendCameraPos(Tile.of(scene.getX(moveLocalX), scene.getY(moveLocalY), 0), moveZ, speed, speed2);
	}

}
