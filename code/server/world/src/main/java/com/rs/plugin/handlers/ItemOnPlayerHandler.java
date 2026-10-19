package com.rs.plugin.handlers;

import com.rs.plugin.events.ItemOnPlayerEvent;

import java.util.function.Consumer;

public class ItemOnPlayerHandler extends PluginHandler<ItemOnPlayerEvent> {
	private boolean checkDistance = true;

	public ItemOnPlayerHandler(boolean checkDistance, Object[] namesOrIds, Consumer<ItemOnPlayerEvent> handler) {
		super(namesOrIds, handler);
		this.checkDistance = checkDistance;
	}

	public ItemOnPlayerHandler(Object[] namesOrIds, Consumer<ItemOnPlayerEvent> handler) {
		super(namesOrIds, handler);
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}
}
