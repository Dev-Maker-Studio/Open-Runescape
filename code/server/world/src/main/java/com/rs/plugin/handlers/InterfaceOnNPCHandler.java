package com.rs.plugin.handlers;

import com.rs.plugin.events.IFOnNPCEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class InterfaceOnNPCHandler extends PluginHandler<IFOnNPCEvent> {
	private boolean checkDistance = true;

	public InterfaceOnNPCHandler(boolean checkDistance, int[] interfaceIds, int[] componentIds, Consumer<IFOnNPCEvent> handler) {
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
	
	public InterfaceOnNPCHandler(boolean checkDistance, int[] interfaceIds, Consumer<IFOnNPCEvent> handler) {
		this(checkDistance, interfaceIds, null, handler);
	}

	public InterfaceOnNPCHandler(int[] interfaceIds, int[] componentIds, Consumer<IFOnNPCEvent> handler) {
		this(true, interfaceIds, componentIds, handler);
	}
	
	public InterfaceOnNPCHandler(int[] interfaceIds, Consumer<IFOnNPCEvent> handler) {
		this(true, interfaceIds, null, handler);
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}
}
