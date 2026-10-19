package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.World;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

import java.util.Map;
import java.util.function.Consumer;

public class CreateNPCAction extends CutsceneAction {

	private final int id;
    private final int x;
    private final int y;
    private final int plane;
	private final Consumer<NPC> configureNpc;

	public CreateNPCAction(String key, int id, int x, int y, int plane, int actionDelay, Consumer<NPC> configureNpc) {
		super(key, actionDelay);
		this.id = id;
		this.x = x;
		this.y = y;
		this.plane = plane;
		this.configureNpc = configureNpc;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");
		if (objects.get(getObjectKey()) != null)
			scene.deleteObject(objects.get(getObjectKey()));
		NPC npc = World.spawnNPC(id, Tile.of(scene.getX(x), scene.getY(y), plane), true, true);
		objects.put(getObjectKey(), npc);
		npc.setRandomWalk(false);
		if (configureNpc != null)
			configureNpc.accept(npc);
	}

}
