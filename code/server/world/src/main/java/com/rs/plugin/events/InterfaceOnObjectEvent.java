package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import com.rs.plugin.handlers.InterfaceOnObjectHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class InterfaceOnObjectEvent implements PluginEvent {

	private static final Map<Object, InterfaceOnObjectHandler> HANDLERS = new HashMap<>();

	private final Player player;
	private final int interfaceId;
	private final int componentId;
	private final int slotId;
	private final GameObject object;
	private final boolean atObject;

	public InterfaceOnObjectEvent(Player player, GameObject object, int interfaceId, int componentId, int slotId, boolean atObject) {
		this.player = player;
		this.interfaceId = interfaceId;
		this.componentId = componentId;
		this.slotId = slotId;
		this.object = object;
		this.atObject = atObject;
	}

	public Player getPlayer() {
		return player;
	}

	public int getInterfaceId() {
		return interfaceId;
	}

	public int getComponentId() {
		return componentId;
	}

	public int getSlotId() {
		return slotId;
	}

	public GameObject getObject() {
		return object;
	}

	public int getObjectId() {
		return object.getId();
	}

	public boolean isAtObject() {
		return atObject;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		InterfaceOnObjectHandler method = HANDLERS.get((interfaceId << 16) + componentId);
		if (method == null)
			method = HANDLERS.get(getInterfaceId());
		if ((method == null) || (!isAtObject() && method.isCheckDistance()))
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, (InterfaceOnObjectHandler) method);
			if (old != null)
				System.err.println("ERROR: Duplicate InterfaceOnObject methods for key: " + key);
		}
	}
}
