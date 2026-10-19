package com.rs.plugin.handlers;

import org.darkan.core.model.ObjectShape;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.events.ObjectClickEvent;

import java.util.function.Consumer;

public class ObjectClickHandler extends PluginHandler<ObjectClickEvent> {

	private Tile[] tiles;
	private ObjectShape type;
	private boolean checkDistance = true;

	public ObjectClickHandler(boolean checkDistance, Object[] namesOrIds, Tile[] tiles, Consumer<ObjectClickEvent> handler) {
		super(namesOrIds, handler);
		this.tiles = tiles;
		this.checkDistance = checkDistance;
	}

	public ObjectClickHandler(boolean checkDistance, Object[] namesOrIds, Consumer<ObjectClickEvent> handler) {
		super(namesOrIds, handler);
		this.checkDistance = checkDistance;
	}

	public ObjectClickHandler(Object[] namesOrIds, Tile[] tiles, Consumer<ObjectClickEvent> handler) {
		this(true, namesOrIds, tiles, handler);
	}

	public ObjectClickHandler(Object[] namesOrIds, Tile tile, Consumer<ObjectClickEvent> handler) {
		this(true, namesOrIds, new Tile[] { tile }, handler);
	}

	public ObjectClickHandler(Object[] namesOrIds, ObjectShape type, Consumer<ObjectClickEvent> handler) {
		this(true, namesOrIds, null, handler);
		this.type = type;
	}

	public ObjectClickHandler(Object[] namesOrIds, Consumer<ObjectClickEvent> handler) {
		this(true, namesOrIds, null, handler);
	}

	public ObjectShape getType() {
		return type;
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}

	public Tile[] getTiles() {
		return tiles;
	}
}
