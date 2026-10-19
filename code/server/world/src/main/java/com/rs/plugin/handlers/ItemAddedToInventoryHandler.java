package com.rs.plugin.handlers;

import com.rs.plugin.events.ItemAddedToInventoryEvent;

import java.util.function.Consumer;

public class ItemAddedToInventoryHandler extends PluginHandler<ItemAddedToInventoryEvent> {
	public ItemAddedToInventoryHandler(Object[] namesOrIds, Consumer<ItemAddedToInventoryEvent> handler) {
		super(namesOrIds, handler);
	}
	
	public ItemAddedToInventoryHandler(int id, Consumer<ItemAddedToInventoryEvent> handler) {
		this(new Object[] { id }, handler);
	}
	
	public ItemAddedToInventoryHandler(String name, Consumer<ItemAddedToInventoryEvent> handler) {
		this(new Object[] { name }, handler);
	}
}
