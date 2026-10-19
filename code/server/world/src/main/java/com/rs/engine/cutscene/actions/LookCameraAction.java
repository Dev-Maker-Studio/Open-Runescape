package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

import java.util.Map;

public class LookCameraAction extends CutsceneAction {

	private final int viewLocalX;
	private final int viewLocalY;
	private final int viewZ;
	private final int speedToExactDestination;
	private final int speedOnRoutePath;

	public LookCameraAction(int viewLocalX, int viewLocalY, int viewZ, int speedToExactDestination, int speedOnRoutePath, int actionDelay) {
		super(null, actionDelay);
		this.viewLocalX = viewLocalX;
		this.viewLocalY = viewLocalY;
		this.viewZ = viewZ;
		this.speedToExactDestination = speedToExactDestination;
		this.speedOnRoutePath = speedOnRoutePath;
	}

	public LookCameraAction(int viewLocalX, int viewLocalY, int viewZ, int actionDelay) {
		this(viewLocalX, viewLocalY, viewZ, -1, -1, actionDelay);
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");
		player.getPackets().sendCameraLook(Tile.of(scene.getX(viewLocalX), scene.getY(viewLocalY), 0), viewZ, speedToExactDestination, speedOnRoutePath);
	}

}
