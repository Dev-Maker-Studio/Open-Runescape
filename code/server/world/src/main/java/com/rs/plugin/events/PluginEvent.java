package com.rs.plugin.events;

import com.rs.plugin.handlers.PluginHandler;

import java.util.List;

public interface PluginEvent {

	public default PluginHandler<? extends PluginEvent> getMethod() {
		return null;
	}

	public default List<PluginHandler<? extends PluginEvent>> getMethods() {
		return null;
	}
}
