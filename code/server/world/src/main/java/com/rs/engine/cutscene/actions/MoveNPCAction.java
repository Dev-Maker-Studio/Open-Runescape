package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.MoveType;
import world.gregs.voidps.type.Tile;

import java.util.Map;

public class MoveNPCAction extends CutsceneAction {

	private final int x;
    private final int y;
    private final int plane;
	private final MoveType movementType;

	public MoveNPCAction(String key, int x, int y, boolean run, int actionDelay) {
		this(key, x, y, 0, run ? MoveType.RUN : MoveType.WALK, actionDelay);
	}

	public MoveNPCAction(String key, int x, int y, int plane, MoveType movementType, int actionDelay) {
		super(key, actionDelay);
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.movementType = movementType;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		NPC npc = (NPC) objects.get(getObjectKey());
		Cutscene scene = (Cutscene) objects.get("cutscene");
		if (movementType == MoveType.TELEPORT) {
			npc.tele(Tile.of(scene.getX(x), scene.getY(y), plane));
			return;
		}
		npc.setRun(movementType == MoveType.RUN);
		npc.addWalkSteps(scene.getX(x), scene.getY(y), 25, false);
	}

}
