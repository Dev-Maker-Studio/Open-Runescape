package com.rs.plugin.handlers;

import com.rs.plugin.events.ItemClickEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ItemClickHandler extends PluginHandler<ItemClickEvent> {
	private Set<String> options;

	public ItemClickHandler(Object[] namesOrIds, Consumer<ItemClickEvent> handler) {
		super(namesOrIds, handler);
	}

	public ItemClickHandler(Object[] namesOrIds, String[] options, Consumer<ItemClickEvent> handler) {
		super(namesOrIds, handler);
		this.options = new HashSet<>(Arrays.asList(options));
	}

	public ItemClickHandler(String[] options, Consumer<ItemClickEvent> handler) {
		super(null, handler);
		this.options = new HashSet<>(Arrays.asList(options));
	}

	public boolean containsOption(String option) {
		return (options == null || options.size() == 0) ? true : options.contains(option);
	}

	@Override
	public Object[] keys() {
		return (keys == null || keys.length <= 0) ? options.toArray() : keys;
	}
}
