package com.rs.plugin.handlers;

import com.rs.plugin.events.ItemAddedToBankEvent;
import com.rs.plugin.events.ItemAddedToInventoryEvent;

import java.util.function.Consumer;

public class ItemAddedToBankHandler extends PluginHandler<ItemAddedToBankEvent> {
	public ItemAddedToBankHandler(Object[] namesOrIds, Consumer<ItemAddedToBankEvent> handler) {
		super(namesOrIds, handler);
	}

	public ItemAddedToBankHandler(int id, Consumer<ItemAddedToBankEvent> handler) {
		this(new Object[] { id }, handler);
	}

	public ItemAddedToBankHandler(String name, Consumer<ItemAddedToBankEvent> handler) {
		this(new Object[] { name }, handler);
	}
}
