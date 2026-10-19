package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.GroundItem;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.handlers.PickupItemHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PickupItemEvent implements PluginEvent {

	private static final Map<Object, Map<Integer, List<PickupItemHandler>>> HANDLERS = new HashMap<>();

	private final Player player;
	private final GroundItem item;
	private boolean cancelPickup;
	private final boolean telegrabbed;

	public PickupItemEvent(Player player, GroundItem item, boolean telegrabbed) {
		this.player = player;
		this.item = item;
		this.telegrabbed = telegrabbed;
	}

	public Player getPlayer() {
		return player;
	}

	public GroundItem getItem() {
		return item;
	}

	public boolean isTelegrabbed() {
		return telegrabbed;
	}

	@Override
	public List<PluginHandler<? extends PluginEvent>> getMethods() {
		List<PluginHandler<? extends PluginEvent>> valids = new ArrayList<>();
		Map<Integer, List<PickupItemHandler>> methodMapping = HANDLERS.get(getItem().getId());
		if (methodMapping == null)
			methodMapping = HANDLERS.get(getItem().getName());
		if (methodMapping == null)
			return null;
		List<PickupItemHandler> methods = methodMapping.get(getItem().getTile().getTileHash());
		if (methods == null)
			methods = methodMapping.get(-getItem().getId());
		if (methods == null)
			methods = methodMapping.get(0);
		if (methods == null)
			return null;
        valids.addAll(methods);
		return valids;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		PickupItemHandler handler = (PickupItemHandler) method;
		for (Object key : handler.keys()) {
            Map<Integer, List<PickupItemHandler>> locMap = HANDLERS.computeIfAbsent(key, k -> new HashMap<>());
            if (handler.getTiles() == null || handler.getTiles().length <= 0) {
				List<PickupItemHandler> methods = locMap.get(0);
				if (methods == null)
					methods = new ArrayList<>();
				methods.add(handler);
				locMap.put(0, methods);
			} else
				for (Tile tile : handler.getTiles()) {
					List<PickupItemHandler> methods = locMap.get(tile.getTileHash());
					if (methods == null)
						methods = new ArrayList<>();
					methods.add(handler);
					locMap.put(tile.getTileHash(), methods);
				}
		}
	}

	public boolean isCancelPickup() {
		return cancelPickup;
	}

	public void cancelPickup() {
		this.cancelPickup = true;
	}
}
