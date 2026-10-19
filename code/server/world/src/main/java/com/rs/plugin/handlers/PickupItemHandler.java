package com.rs.plugin.handlers;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.events.PickupItemEvent;

import java.util.function.Consumer;

public class PickupItemHandler extends PluginHandler<PickupItemEvent> {

	private final Tile[] tiles;

	public PickupItemHandler(Object[] namesOrIds, Tile[] tiles, Consumer<PickupItemEvent> handler) {
		super(namesOrIds, handler);
		this.tiles = tiles;
	}
	
	public PickupItemHandler(Object[] namesOrIds, Tile tile, Consumer<PickupItemEvent> handler) {
		super(namesOrIds, handler);
		this.tiles = new Tile[] { tile };
	}
	
	public PickupItemHandler(int id, Tile[] tiles, Consumer<PickupItemEvent> handler) {
		this(new Object[] { id }, tiles, handler);
	}
	
	public PickupItemHandler(int id, Tile tile, Consumer<PickupItemEvent> handler) {
		this(new Object[] { id }, tile, handler);
	}
	
	public PickupItemHandler(String name, Tile[] tiles, Consumer<PickupItemEvent> handler) {
		this(new Object[] { name }, tiles, handler);
	}

	public PickupItemHandler(String name, Tile tile, Consumer<PickupItemEvent> handler) {
		this(new Object[] { name }, tile, handler);
	}
	
	public Tile[] getTiles() {
		return tiles;
	}
}
