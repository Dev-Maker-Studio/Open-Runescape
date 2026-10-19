package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import com.rs.plugin.handlers.ItemClickHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class ItemClickEvent implements PluginEvent {

	private static final Map<Object, ItemClickHandler> HANDLERS = new HashMap<>();

	private final Player player;
	private final Item item;
	private final String option;
	private final boolean isEquipped;
	private final int slotId;

	public ItemClickEvent(Player player, Item item, int slotId, String option) {
		this(player, item, slotId, option, false);
	}

	public ItemClickEvent(Player player, Item item, int slotId, String option, boolean isEquipped) {
		this.player = player;
		this.item = item.withSlot(slotId);
		this.option = option;
		this.isEquipped = isEquipped;
		this.slotId = slotId;
	}

	public boolean isEquipped() {
		return isEquipped;
	}

	public String getOption() {
		return option;
	}

	public Player getPlayer() {
		return player;
	}

	public Item getItem() {
		return item;
	}

	public int getSlotId() {
		return slotId;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		ItemClickHandler method = HANDLERS.get(item.getId());
		if (method == null || !method.containsOption(option))
			method = HANDLERS.get(item.getDefinitions().getName());
		if (method == null || !method.containsOption(option))
			method = HANDLERS.get(option);
		if (method == null)
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, (ItemClickHandler) method);
			if (old != null)
				System.err.println("ERROR: Duplicate ItemClick methods for key: " + key);
		}
	}

	public Player component1() {
		return player;
	}

	public Item component2() {
		return item;
	}

	public String component3() {
		return option;
	}

	public boolean component4() {
		return isEquipped;
	}
}
