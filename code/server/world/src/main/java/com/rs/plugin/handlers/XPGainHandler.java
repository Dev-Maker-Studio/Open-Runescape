package com.rs.plugin.handlers;

import com.rs.plugin.events.XPGainEvent;

import java.util.function.Consumer;

public class XPGainHandler extends PluginHandler<XPGainEvent> {
	public XPGainHandler(Consumer<XPGainEvent> handler) {
		super(new Object[] { "meme" }, handler);
	}
}
