package com.rs.plugin.handlers;

import com.rs.plugin.events.InterfaceOnObjectEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InterfaceOnObjectHandler extends PluginHandler<InterfaceOnObjectEvent> {
	private boolean checkDistance = true;

	public InterfaceOnObjectHandler(boolean checkDistance, int[] interfaceIds, int[] componentIds, Consumer<InterfaceOnObjectEvent> handler) {
		super(null, handler);
		this.checkDistance = checkDistance;
		List<Object> list = new ArrayList<>();
		if (componentIds.length <= 0)
			for (Integer id : interfaceIds)
				list.add(id);
		else
			for (int id : interfaceIds)
				for (int comp : componentIds)
					list.add((id << 16) + comp);
		keys = list.toArray();
	}

	public InterfaceOnObjectHandler(int[] interfaceIds, int[] componentIds, Consumer<InterfaceOnObjectEvent> handler) {
		this(true, interfaceIds, componentIds, handler);
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}
}
