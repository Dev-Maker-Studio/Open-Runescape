package com.rs.plugin.handlers;

import com.rs.plugin.events.DropItemEvent;

import java.util.function.Consumer;

public class DropItemHandler extends PluginHandler<DropItemEvent> {
	public DropItemHandler(Object[] namesOrIds, Consumer<DropItemEvent> handler) {
		super(namesOrIds, handler);
	}
}
