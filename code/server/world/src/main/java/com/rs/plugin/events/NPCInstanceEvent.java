package com.rs.plugin.events;

import org.darkan.core.cache.NPCDefinitions;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.handlers.NPCInstanceHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class NPCInstanceEvent implements PluginEvent {

	private static final Map<Object, NPCInstanceHandler> HANDLERS = new HashMap<>();

	private final int npcId;
	private final Tile tile;
	private final boolean spawned;

	public NPCInstanceEvent(int npcId, Tile tile, boolean spawned) {
		this.npcId = npcId;
		this.tile = tile;
		this.spawned = spawned;
	}

	public int getNpcId() {
		return npcId;
	}

	public Tile getTile() {
		return tile;
	}

	public boolean isSpawned() {
		return spawned;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		NPCInstanceHandler method = HANDLERS.get(npcId);
		if (method == null)
			method = HANDLERS.get(NPCDefinitions.getDefs(npcId).getName());
		if (method == null)
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, (NPCInstanceHandler) method);
			if (old != null)
				System.err.println("ERROR: Duplicate NPC instance methods for key: " + key);
		}
	}
}
