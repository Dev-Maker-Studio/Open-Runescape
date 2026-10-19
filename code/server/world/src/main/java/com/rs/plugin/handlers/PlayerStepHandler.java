package com.rs.plugin.handlers;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.events.PlayerStepEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PlayerStepHandler extends PluginHandler<PlayerStepEvent> {

	public PlayerStepHandler(Tile[] tiles, Consumer<PlayerStepEvent> handler) {
		super(null, handler);
		List<Integer> tileHashes = new ArrayList<>();
		for (Tile tile : tiles)
			tileHashes.add(tile.getTileHash());
		keys = tileHashes.toArray();
	}
	
	public PlayerStepHandler(Tile tile, Consumer<PlayerStepEvent> handler) {
		this(new Tile[] { tile }, handler);
	}
}
