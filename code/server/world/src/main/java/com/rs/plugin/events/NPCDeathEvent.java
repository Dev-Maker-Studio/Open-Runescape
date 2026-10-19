package com.rs.plugin.events;

import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class NPCDeathEvent implements PluginEvent {

	private static final Map<Object, PluginHandler<? extends PluginEvent>> HANDLERS = new HashMap<>();

	private final NPC npc;
	private final Entity killer;

	public NPCDeathEvent(NPC npc, Entity killer) {
		this.npc = npc;
		this.killer = killer;
	}

	public NPC getNPC() {
		return npc;
	}

	public Entity getKiller() {
		return killer;
	}

	public Entity component1() {
		return killer;
	}

	public NPC component2() {
		return npc;
	}

	public boolean killedByPlayer() {
		if (killer != null && (killer instanceof Player))
			return true;
		return false;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		PluginHandler<? extends PluginEvent> method = HANDLERS.get(npc.getId());
		if (method == null)
			method = HANDLERS.get(npc.getDefinitions().getName());
		if (method == null)
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, method);
			if (old != null)
				System.err.println("ERROR: Duplicate NPCDeath methods for key: " + key);
		}
	}

}
