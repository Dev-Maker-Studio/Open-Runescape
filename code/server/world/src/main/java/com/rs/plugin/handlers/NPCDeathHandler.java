package com.rs.plugin.handlers;

import com.rs.plugin.events.NPCDeathEvent;

import java.util.function.Consumer;

public class NPCDeathHandler extends PluginHandler<NPCDeathEvent> {
	public NPCDeathHandler(Object[] namesOrIds, Consumer<NPCDeathEvent> handler) {
		super(namesOrIds, handler);
	}
	
	public NPCDeathHandler(int id, Consumer<NPCDeathEvent> handler) {
		super(new Object[] { id }, handler);
	}
	
	public NPCDeathHandler(String name, Consumer<NPCDeathEvent> handler) {
		super(new Object[] { name }, handler);
	}
}
