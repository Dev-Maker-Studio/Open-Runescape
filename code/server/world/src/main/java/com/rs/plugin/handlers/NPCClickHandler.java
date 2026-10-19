package com.rs.plugin.handlers;

import com.rs.plugin.events.NPCClickEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class NPCClickHandler extends PluginHandler<NPCClickEvent> {
	private boolean checkDistance = true;
	private Set<String> options;

	public NPCClickHandler(boolean checkDistance, Object[] namesOrIds, String[] options, Consumer<NPCClickEvent> handler) {
		super(namesOrIds == null ? new Object[] { null } : namesOrIds, handler);
		if (options != null && options.length > 0)
			this.options = new HashSet<>(Arrays.asList(options));
		this.checkDistance = checkDistance;
	}

	public NPCClickHandler(Object[] namesOrIds, String[] options, Consumer<NPCClickEvent> handler) {
		this(true, namesOrIds, options, handler);
	}

	public NPCClickHandler(boolean checkDistance, Object[] namesOrIds, Consumer<NPCClickEvent> handler) {
		this(checkDistance, namesOrIds, null, handler);
	}

	public NPCClickHandler(Object[] namesOrIds, Consumer<NPCClickEvent> handler) {
		this(true, namesOrIds, null, handler);
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}

	public Set<String> getOptions() {
		return options;
	}
	public boolean containsOption(String option) {
		return (options == null || options.size() == 0) ? true : options.contains(option);
	}
}
