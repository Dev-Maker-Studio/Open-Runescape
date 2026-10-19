package com.rs.plugin.handlers;

import world.gregs.voidps.type.Tile;
import com.rs.plugin.events.ItemOnObjectEvent;

import java.util.function.Consumer;

public class ItemOnObjectHandler extends PluginHandler<ItemOnObjectEvent> {

	private final Object[] objectKeys;
    private final Object[] itemKeys;
	private final Tile[] tiles;
	private boolean checkDistance = true;

	public ItemOnObjectHandler(boolean checkDistance, Object[] objectNamesOrIds, Object[] itemNamesOrIds, Tile[] tiles, Consumer<ItemOnObjectEvent> handler) {
		super(new Object[] { "meme" }, handler);
		this.tiles = tiles;
		this.checkDistance = checkDistance;
		this.objectKeys = objectNamesOrIds;
		this.itemKeys = itemNamesOrIds;
	}

	public ItemOnObjectHandler(Object[] objectNamesOrIds, Object[] itemNamesOrIds, Tile[] tiles, Consumer<ItemOnObjectEvent> handler) {
		this(true, objectNamesOrIds, itemNamesOrIds, tiles, handler);
	}
	
	public ItemOnObjectHandler(boolean checkDistance, Object[] objectNamesOrIds, Object[] itemNamesOrIds, Consumer<ItemOnObjectEvent> handler) {
		this(true, objectNamesOrIds, itemNamesOrIds, null, handler);
		this.checkDistance = checkDistance;
	}

	public ItemOnObjectHandler(Object[] objectNamesOrIds, Object[] itemNamesOrIds, Consumer<ItemOnObjectEvent> handler) {
		this(true, objectNamesOrIds, itemNamesOrIds, null, handler);
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}

	public Tile[] getTiles() {
		return tiles;
	}

	public Object[] getObjectKeys() {
		return objectKeys;
	}

	public Object[] getItemKeys() {
		return itemKeys;
	}
}
