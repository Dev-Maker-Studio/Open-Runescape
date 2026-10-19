package com.rs.plugin.events;

import org.darkan.core.map.pathfinder.WalkStep;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.handlers.PlayerStepHandler;
import com.rs.plugin.handlers.PluginHandler;

import java.util.HashMap;
import java.util.Map;

public class PlayerStepEvent implements PluginEvent {

	private static final Map<Object, PlayerStepHandler> METHODS = new HashMap<>();

	private final Player player;
	private final Tile tile;
	private final WalkStep step;

	public PlayerStepEvent(Player player, WalkStep step, Tile tile) {
		this.player = player;
		this.tile = tile;
		this.step = step;
	}

	public Player getPlayer() {
		return player;
	}

	public Tile getTile() {
		return tile;
	}

	public WalkStep getStep() {
		return step;
	}

	public Player component1() {
		return player;
	}

	public WalkStep component2() {
		return step;
	}

	public Tile component3() {
		return tile;
	}

	@Override
	public PluginHandler<? extends PluginEvent> getMethod() {
		return METHODS.get(tile.getTileHash());
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		PlayerStepHandler handler = (PlayerStepHandler) method;
		for (Object key : handler.keys())
			if (METHODS.put(key, handler) != null)
				System.err.println("Duplicate player step events for key " + key);
	}

}
