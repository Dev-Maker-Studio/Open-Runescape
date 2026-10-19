package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import com.rs.plugin.handlers.PlayerClickHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class PlayerClickEvent implements PluginEvent {

	private static final Map<Object, PlayerClickHandler> HANDLERS = new HashMap<>();

	private final Player player;
	private final Player otherPlayer;
	private final String option;
	private final boolean atPlayer;

	public PlayerClickEvent(Player player, Player otherPlayer, String option, boolean atPlayer) {
		this.player = player;
		this.otherPlayer = otherPlayer;
		this.option = option;
		this.atPlayer = atPlayer;
	}

	public Player getPlayer() {
		return player;
	}

	public Player getTarget() {
		return otherPlayer;
	}

	public String getOption() {
		return option;
	}

	public boolean isAtPlayer() {
		return atPlayer;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		PlayerClickHandler method = HANDLERS.get(option);
		if ((method == null) || (!isAtPlayer() && method.isCheckDistance()))
			return null;
		return method;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		for (Object key : method.keys()) {
			PluginHandler<? extends PluginEvent> old = HANDLERS.put(key, (PlayerClickHandler) method);
			if (old != null)
				System.err.println("ERROR: Duplicate NPCClick methods for key: " + key);
		}
	}

	public Player component1() {
		return player;
	}

	public Player component2() {
		return otherPlayer;
	}

	public String component3() {
		return option;
	}
}
