package com.rs.plugin.handlers;

import com.rs.plugin.events.ItemOnNPCEvent;

import java.util.function.Consumer;

public class ItemOnNPCHandler extends PluginHandler<ItemOnNPCEvent> {
	private boolean checkDistance = true;

	public ItemOnNPCHandler(boolean checkDistance, Object[] namesOrIds, Consumer<ItemOnNPCEvent> handler) {
		super(namesOrIds, handler);
		this.checkDistance = checkDistance;
	}
	
	public ItemOnNPCHandler(int id, Consumer<ItemOnNPCEvent> handler) {
		super(new Object[] { id }, handler);
	}
	
	public ItemOnNPCHandler(String name, Consumer<ItemOnNPCEvent> handler) {
		super(new Object[] { name }, handler);
	}

	public ItemOnNPCHandler(Object[] namesOrIds, Consumer<ItemOnNPCEvent> handler) {
		super(namesOrIds, handler);
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}
}
