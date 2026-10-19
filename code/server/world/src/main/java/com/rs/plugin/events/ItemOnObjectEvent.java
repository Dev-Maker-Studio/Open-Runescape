package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.handlers.ItemOnObjectHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemOnObjectEvent implements PluginEvent {

	private static final Map<Object, Map<Object, Map<Integer, List<ItemOnObjectHandler>>>> OBJECT_HANDLERS = new HashMap<>();
	private static final Map<Object, Map<Integer, List<ItemOnObjectHandler>>> ITEM_HANDLERS = new HashMap<>();

	private final Player player;
	private final Item item;
	private final GameObject object;
	private final boolean atObject;
	private final int objectId;

	public ItemOnObjectEvent(Player player, Item item, GameObject object, boolean atObject) {
		this.player = player;
		this.item = item;
		this.object = object;
		this.atObject = atObject;
		objectId = object.getId();
	}

	public Player getPlayer() {
		return player;
	}

	public Item getItem() {
		return item;
	}

	public GameObject getObject() {
		return object;
	}

	public boolean isAtObject() {
		return atObject;
	}

	public int getObjectId() {
		return objectId;
	}

	public Player component1() {
		return player;
	}

	public GameObject component2() {
		return object;
	}

	public Item component3() {
		return item;
	}

	@Override
	public List<PluginHandler<? extends PluginEvent>> getMethods() {
		List<PluginHandler<? extends PluginEvent>> valids = new ArrayList<>();
		Map<Integer, List<ItemOnObjectHandler>> tileMaps = null;
		Map<Object, Map<Integer, List<ItemOnObjectHandler>>> itemMappings = OBJECT_HANDLERS.get(object.getId());
		if (itemMappings == null)
			itemMappings = OBJECT_HANDLERS.get(object.getDefinitions(player).getName());
		if (itemMappings != null) {
			if (tileMaps == null)
				tileMaps = itemMappings.get(item.getDefinitions().getName());
			if (tileMaps == null)
				tileMaps = itemMappings.get(item.getId());
			if (tileMaps == null)
				tileMaps = itemMappings.get(-1);
		} else {
			if (tileMaps == null)
				tileMaps = ITEM_HANDLERS.get(item.getDefinitions().getName());
			if (tileMaps == null)
				tileMaps = ITEM_HANDLERS.get(item.getId());
		}
		if (tileMaps == null)
			return null;
		List<ItemOnObjectHandler> methods = tileMaps.get(getObject().getTile().getTileHash());
		if (methods == null)
			methods = tileMaps.get(-getObject().getType().getId());
		if (methods == null)
			methods = tileMaps.get(0);
		for (ItemOnObjectHandler method : methods) {
			if (!isAtObject() && method.isCheckDistance())
				continue;
			valids.add(method);
		}
		return valids;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		ItemOnObjectHandler handler = (ItemOnObjectHandler) method;
		if (handler.getObjectKeys() != null) {
			for (Object objectKey : handler.getObjectKeys()) {
				Map<Object, Map<Integer, List<ItemOnObjectHandler>>> itemMap = OBJECT_HANDLERS.computeIfAbsent(objectKey, k -> new HashMap<>());
				if (handler.getItemKeys() != null) {
					for (Object itemKey : handler.getItemKeys())
						updateLocationMap(handler, itemMap.computeIfAbsent(itemKey, k -> new HashMap<>()));
				} else
					updateLocationMap(handler, itemMap.computeIfAbsent(-1, k -> new HashMap<>()));
			}
		} else {
			for (Object itemKey : handler.getItemKeys())
				updateLocationMap(handler, ITEM_HANDLERS.computeIfAbsent(itemKey, k -> new HashMap<>()));
		}
	}

	private static void updateLocationMap(ItemOnObjectHandler handler, Map<Integer, List<ItemOnObjectHandler>> locMap) {
		if (handler.getTiles() == null || handler.getTiles().length <= 0) {
			List<ItemOnObjectHandler> methods = locMap.computeIfAbsent(0, k -> new ArrayList<>());
			methods.add(handler);
		} else {
			for (Tile tile : handler.getTiles()) {
				List<ItemOnObjectHandler> methods = locMap.computeIfAbsent(tile.getTileHash(), k -> new ArrayList<>());
				methods.add(handler);
			}
		}
	}

}
