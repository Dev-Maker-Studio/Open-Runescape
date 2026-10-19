package com.rs.plugin.handlers;

import com.rs.plugin.events.NPCKillParticipatedEvent;

import java.util.function.Consumer;

public class NPCKillParticipatedHandler extends PluginHandler<NPCKillParticipatedEvent> {
	public NPCKillParticipatedHandler(Object[] namesOrIds, Consumer<NPCKillParticipatedEvent> handler) {
		super(namesOrIds, handler);
	}

	public NPCKillParticipatedHandler(int id, Consumer<NPCKillParticipatedEvent> handler) {
		super(new Object[] { id }, handler);
	}

	public NPCKillParticipatedHandler(String name, Consumer<NPCKillParticipatedEvent> handler) {
		super(new Object[] { name }, handler);
	}
}
