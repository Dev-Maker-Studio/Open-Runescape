package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.handlers.ObjectClickHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectClickEvent implements PluginEvent {

	private static final Map<Object, Map<Integer, List<ObjectClickHandler>>> METHODS = new HashMap<>();

	private final Player player;
	private final GameObject obj;
	private final int opNum;
	private final String option;
	private final boolean atObject;
	private final int objectId;

	public ObjectClickEvent(Player player, GameObject object, int opNum, boolean atObject) {
		this.player = player;
		this.obj = object;
		this.opNum = opNum;
		this.atObject = atObject;
		objectId = object.getId();
		option = object.getDefinitions(player).getOption(opNum);
	}

	public String getOption() {
		return option;
	}

	public Player getPlayer() {
		return player;
	}

	public GameObject getObj() {
		return obj;
	}

	public int getOpNum() {
		return opNum;
	}

	public boolean isAtObject() {
		return atObject;
	}

	public int getObjectId() {
		return objectId;
	}

	public boolean objectAt(int x, int y) {
		return obj.getTile().isAt(x, y);
	}

	public boolean objectAt(int x, int y, int plane) {
		return obj.getTile().isAt(x, y, plane);
	}

	@Override
	public List<PluginHandler<? extends PluginEvent>> getMethods() {
		List<PluginHandler<? extends PluginEvent>> valids = new ArrayList<>();
		Map<Integer, List<ObjectClickHandler>> methodMapping = METHODS.get(getObjectId());
		if (methodMapping == null)
			methodMapping = METHODS.get(getObj().getDefinitions(getPlayer()).getName());
		if (methodMapping == null)
			return null;
		List<ObjectClickHandler> methods = methodMapping.get(getObj().getTile().getTileHash());
		if (methods == null)
			methods = methodMapping.get(-getObj().getType().getId());
		if (methods == null)
			methods = methodMapping.get(0);
		if (methods == null)
			return null;
		for (ObjectClickHandler method : methods) {
			if (!isAtObject() && method.isCheckDistance())
				continue;
			valids.add(method);
		}
		return valids;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		ObjectClickHandler handler = (ObjectClickHandler) method;
		for (Object key : handler.keys()) {
			Map<Integer, List<ObjectClickHandler>> locMap = METHODS.computeIfAbsent(key, k -> new HashMap<>());
			if (handler.getType() == null && (handler.getTiles() == null || handler.getTiles().length <= 0)) {
				List<ObjectClickHandler> methods = locMap.get(0);
				if (methods == null)
					methods = new ArrayList<>();
				methods.add(handler);
				locMap.put(0, methods);
			} else if (handler.getType() != null) {
				List<ObjectClickHandler> methods = locMap.get(-handler.getType().getId());
				if (methods == null)
					methods = new ArrayList<>();
				methods.add(handler);
				locMap.put(-handler.getType().getId(), methods);
			} else
				for (Tile tile : handler.getTiles()) {
					List<ObjectClickHandler> methods = locMap.get(tile.getTileHash());
					if (methods == null)
						methods = new ArrayList<>();
					methods.add(handler);
					locMap.put(tile.getTileHash(), methods);
				}
		}
	}

	public Player component1() {
		return player;
	}

	public GameObject component2() {
		return obj;
	}

	public String component3() {
		return option;
	}

}
