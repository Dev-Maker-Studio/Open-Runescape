package com.rs.plugin.handlers;

import com.rs.plugin.events.PlayerClickEvent;

import java.util.function.Consumer;

public class PlayerClickHandler extends PluginHandler<PlayerClickEvent> {

	private boolean checkDistance = true;

	public PlayerClickHandler(boolean checkDistance, String option, Consumer<PlayerClickEvent> handler) {
		super(new Object[] { option }, handler);
		this.checkDistance = checkDistance;
	}

	public boolean isCheckDistance() {
		return checkDistance;
	}

}
