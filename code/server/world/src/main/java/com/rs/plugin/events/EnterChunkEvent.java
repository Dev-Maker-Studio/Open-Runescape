package com.rs.plugin.events;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.handlers.PluginHandler;

import java.util.ArrayList;
import java.util.List;

public class EnterChunkEvent implements PluginEvent {

	private static final List<PluginHandler<? extends PluginEvent>> HANDLERS = new ArrayList<>();

	private final Entity entity;
	private final int chunkId;

	public EnterChunkEvent(Entity entity, int chunkId) {
		this.entity = entity;
		this.chunkId = chunkId;
	}

	public NPC getNPC() {
		if (entity instanceof NPC n)
			return n;
		return null;
	}

	public Player getPlayer() {
		if (entity instanceof Player p)
			return p;
		return null;
	}

	public Entity getEntity() {
		return entity;
	}

	public int getChunkId() {
		return chunkId;
	}

	@Override
	public List<PluginHandler<? extends PluginEvent>> getMethods() {
		return HANDLERS;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		HANDLERS.add(method);
	}

	public Entity component1() {
		return entity;
	}

	public int component2() {
		return chunkId;
	}
}
