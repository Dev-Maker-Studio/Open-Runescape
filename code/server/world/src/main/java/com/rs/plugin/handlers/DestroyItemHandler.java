package com.rs.plugin.handlers;

import com.rs.plugin.events.DestroyItemEvent;

import java.util.function.Consumer;

public class DestroyItemHandler extends PluginHandler<DestroyItemEvent> {
	public DestroyItemHandler(Object[] namesOrIds, Consumer<DestroyItemEvent> handler) {
		super(namesOrIds, handler);
	}
}
