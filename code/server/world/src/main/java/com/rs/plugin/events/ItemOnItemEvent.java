package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import org.darkan.core.Utils;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class ItemOnItemEvent implements PluginEvent {

	private static final Map<Object, PluginHandler<? extends PluginEvent>> HANDLERS = new HashMap<>();

	private final Player player;
	private final Item item1;
	private final Item item2;

	public ItemOnItemEvent(Player player, Item item1, Item item2) {
		this.player = player;
		this.item1 = item1;
		this.item2 = item2;
	}

	public boolean usedWith(int item1, int item2) {
		return (this.item1.getId() == item1 && this.item2.getId() == item2) || (this.item1.getId() == item2 && this.item2.getId() == item1);
	}

	public Item getUsedWith(int used) {
		if (item1.getId() == used)
			return item2;
		if (item2.getId() == used)
			return item1;
		return null;
	}

	public Item getUsedWith(int... used) {
		for (int use : used) {
			if (item1.getId() == use)
				return item2;
			if (item2.getId() == use)
				return item1;
		}
		return null;
	}

	public Item getItem2() {
		return item2;
	}

	public Item getItem1() {
		return item1;
	}

	public Player getPlayer() {
		return player;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		int hash1 = (item1.getId() << 16) + item2.getId();
		int hash2 = (item2.getId() << 16) + item1.getId();
		PluginHandler<? extends PluginEvent> method = HANDLERS.get(hash1);
		if (method == null)
			method = HANDLERS.get(hash2);
		if (method == null)
			method = HANDLERS.get(-item1.getId());
		if (method == null)
			method = HANDLERS.get(-item2.getId());
		if (method == null)
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, method);
			if (old != null) {
				int item1 = Utils.interfaceIdFromHash((int) key);
				int item2 = Utils.componentIdFromHash((int) key);
				System.err.println("ERROR: Duplicate ItemOnItem methods for key: " + key + "(" + item1 + ", " + item2 + ") " + method);
			}
		}
	}
}
