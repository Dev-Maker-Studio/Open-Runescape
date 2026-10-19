package com.rs.plugin.events;

import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import com.rs.plugin.handlers.ItemOnNPCHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class ItemOnNPCEvent implements PluginEvent {

	private static final Map<Object, ItemOnNPCHandler> HANDLERS = new HashMap<>();

	private final Player player;
	private final NPC npc;
	private final Item item;
	private final boolean atNPC;

	public ItemOnNPCEvent(Player player, NPC npc, Item item, boolean atNPC) {
		this.player = player;
		this.npc = npc;
		this.item = item;
		this.atNPC = atNPC;
	}

	public Player getPlayer() {
		return player;
	}

	public NPC getNPC() {
		return npc;
	}

	public Item getItem() {
		return item;
	}

	public Player component1() {
		return player;
	}

	public Item component2() {
		return item;
	}

	public NPC component3() {
		return npc;
	}

	public boolean isAtNPC() {
		return atNPC;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		ItemOnNPCHandler method = HANDLERS.get(getNPC().getId());
		if (method == null)
			method = HANDLERS.get(getNPC().getDefinitions().getName());
		if ((method == null) || (!isAtNPC() && method.isCheckDistance()))
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, (ItemOnNPCHandler) method);
			if (old != null)
				System.err.println("ERROR: Duplicate ItemOnNPC methods for key: " + key);
		}
	}
}
