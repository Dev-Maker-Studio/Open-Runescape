package com.rs.plugin.handlers;

import com.rs.plugin.events.EnterChunkEvent;

import java.util.function.Consumer;

public class EnterChunkHandler extends PluginHandler<EnterChunkEvent> {
	public EnterChunkHandler(Consumer<EnterChunkEvent> handler) {
		super(new Object[] { "meme" }, handler);
	}
}
