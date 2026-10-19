package com.rs.plugin.handlers;

import com.rs.plugin.events.LoginEvent;

import java.util.function.Consumer;

public class LoginHandler extends PluginHandler<LoginEvent> {
	public LoginHandler(Consumer<LoginEvent> handler) {
		super(new Object[] { "meme" }, handler);
	}
}
