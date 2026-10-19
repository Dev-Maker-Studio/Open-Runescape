package com.rs.plugin.handlers;

import com.rs.plugin.events.PluginEvent;

import java.util.function.Consumer;

public abstract class PluginHandler<T extends PluginEvent> {
	protected Object[] keys;
	protected Consumer<T> handler;

	public PluginHandler(Object[] keys, Consumer<T> handler) {
		this.keys = keys;
		this.handler = handler;
	}

	public boolean handleGlobal(T e) {
		return false;
	}
	
	public Object getObj(T e) {
		return null;
	}

	public Object[] keys() {
		return keys;
	}

	public Consumer<T> getHandler() {
		return handler;
	}
}
