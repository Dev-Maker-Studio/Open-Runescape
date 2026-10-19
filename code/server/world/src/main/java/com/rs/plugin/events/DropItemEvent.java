package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class DropItemEvent implements PluginEvent {

	private static final Map<Object, PluginHandler<? extends PluginEvent>> HANDLERS = new HashMap<>();

	private final Player player;
	private final Item item;
	private boolean cancelDrop = false;

	public DropItemEvent(Player player, Item item) {
		this.player = player;
		this.item = item;
	}

	public Player getPlayer() {
		return player;
	}

	public Item getItem() {
		return item;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		PluginHandler<? extends PluginEvent> method = HANDLERS.get(item.getId());
		if (method == null)
			method = HANDLERS.get(item.getDefinitions().getName());
		if (method == null)
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, method);
			if (old != null)
				System.err.println("ERROR: Duplicate DropItem methods for key: " + key);
		}
	}

	public boolean dropCancelled() {
		return cancelDrop;
	}

	public void cancelDrop() {
		this.cancelDrop = true;
	}

	public Player component1() {
		return player;
	}

	public Item component2() {
		return item;
	}
}
