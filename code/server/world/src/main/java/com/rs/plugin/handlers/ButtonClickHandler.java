package com.rs.plugin.handlers;

import com.rs.plugin.events.ButtonClickEvent;

import java.util.function.Consumer;

public class ButtonClickHandler extends PluginHandler<ButtonClickEvent> {
	public ButtonClickHandler(Object[] interfaceIds, Consumer<ButtonClickEvent> handler) {
		super(interfaceIds, handler);
	}
	
	public ButtonClickHandler(Object interfaceId, Consumer<ButtonClickEvent> handler) {
		super(new Object[] { interfaceId }, handler);
	}
}
