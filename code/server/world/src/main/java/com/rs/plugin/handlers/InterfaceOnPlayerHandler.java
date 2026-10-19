package com.rs.plugin.handlers;

import com.rs.plugin.events.IFOnPlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InterfaceOnPlayerHandler extends PluginHandler<IFOnPlayerEvent> {
	private boolean checkDistance = true;

	public InterfaceOnPlayerHandler(boolean checkDistance, int[] interfaceIds, int[] componentIds, Consumer<IFOnPlayerEvent> handler) {
		super(null, handler);
		this.checkDistance = checkDistance;
		List<Object> list = new ArrayList<>();
		if (componentIds == null || componentIds.length <= 0)
			for (Integer id : interfaceIds)
				list.add(id);
		else
			for (int id : interfaceIds)
				for (int comp : componentIds)
					list.add((id << 16) + comp);
		keys = list.toArray();
	}
	
	public InterfaceOnPlayerHandler(boolean checkDistance, int[] interfaceIds, Consumer<IFOnPlayerEvent> handler) {
		this(checkDistance, interfaceIds, null, handler);
	}

	public InterfaceOnPlayerHandler(int[] interfaceIds, int[] componentIds, Consumer<IFOnPlayerEvent> handler) {
		this(true, interfaceIds, componentIds, handler);
	}
	
	public InterfaceOnPlayerHandler(int[] interfaceIds, Consumer<IFOnPlayerEvent> handler) {
		this(true, interfaceIds, null, handler);
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}
}
