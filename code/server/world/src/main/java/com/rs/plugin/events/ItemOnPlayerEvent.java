package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import com.rs.plugin.handlers.ItemOnPlayerHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class ItemOnPlayerEvent implements PluginEvent {

	private static final Map<Object, ItemOnPlayerHandler> HANDLERS = new HashMap<>();

	private final Player player;
	private final Player otherPlayer;
	private final Item item;
	private final boolean atPlayer;

	public ItemOnPlayerEvent(Player player, Player otherPlayer, Item item, boolean atPlayer) {
		this.player = player;
		this.otherPlayer = otherPlayer;
		this.item = item;
		this.atPlayer = atPlayer;
	}

	public Player getPlayer() {
		return player;
	}

	public Player getTarget() {
		return otherPlayer;
	}

	public Item getItem() {
		return item;
	}

	public boolean isAtPlayer() {
		return atPlayer;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		ItemOnPlayerHandler method = HANDLERS.get(item.getId());
		if (method == null)
			method = HANDLERS.get(item.getDefinitions().getName());
		if ((method == null) || (!isAtPlayer() && method.isCheckDistance()))
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			ItemOnPlayerHandler old = HANDLERS.put(key, (ItemOnPlayerHandler) method);
			if (old != null)
				System.err.println("ERROR: Duplicate ItemOnPlayer methods for key: " + key);
		}
	}
}
