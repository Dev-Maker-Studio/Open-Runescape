package com.rs.engine.cutscene.actions;

import com.rs.engine.cutscene.Cutscene;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;

import java.util.Map;

public class ConstructMapAction extends CutsceneAction {

	private final Tile returnTile;
	private final int baseChunkX;
    private final int baseChunkY;
    private final int widthChunks;
    private final int heightChunks;
	private final boolean copyNpcs;

	public ConstructMapAction(Tile returnTile, int baseChunkX, int baseChunkY, int widthChunks, int heightChunks, boolean copyNpcs) {
		super(null, 1);
		this.returnTile = returnTile;
		this.baseChunkX = baseChunkX;
		this.baseChunkY = baseChunkY;
		this.widthChunks = widthChunks;
		this.heightChunks = heightChunks;
		this.copyNpcs = copyNpcs;
	}

	@Override
	public void process(Player player, Map<String, Object> objects) {
		Cutscene scene = (Cutscene) objects.get("cutscene");
		scene.constructArea(returnTile, baseChunkX, baseChunkY, widthChunks, heightChunks, copyNpcs);
	}
}
