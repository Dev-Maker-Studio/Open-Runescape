package com.rs.plugin.events;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.handlers.NPCInteractionDistanceHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class NPCInteractionDistanceEvent implements PluginEvent {

	private static final Map<Object, NPCInteractionDistanceHandler> HANDLERS = new HashMap<>();

	private final Player player;
	private final NPC npc;

	public NPCInteractionDistanceEvent(Player player, NPC npc) {
		this.player = player;
		this.npc = npc;
	}

	public Player getPlayer() {
		return player;
	}

	public NPC getNpc() {
		return npc;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		NPCInteractionDistanceHandler method = HANDLERS.get(npc.getId());
		if (method == null)
			method = HANDLERS.get(npc.getDefinitions().getName());
		if (method == null)
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, (NPCInteractionDistanceHandler) method);
			if (old != null)
				System.err.println("ERROR: Duplicate ItemOnNPC methods for key: " + key);
		}
	}
}
