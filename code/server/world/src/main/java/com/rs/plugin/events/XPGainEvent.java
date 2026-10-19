package com.rs.plugin.events;

import com.rs.game.model.entity.player.Player;
import com.rs.plugin.handlers.PluginHandler;

import java.util.ArrayList;
import java.util.List;

public class XPGainEvent implements PluginEvent {

	private static final List<PluginHandler<? extends PluginEvent>> HANDLERS = new ArrayList<>();

	private final Player player;
	private final int skillId;
	private final double xp;
	private double multiplier = 1.0;

	public XPGainEvent(Player player, int skillId, double xp) {
		this.player = player;
		this.skillId = skillId;
		this.xp = xp;
	}

	public Player getPlayer() {
		return player;
	}

	public int getSkillId() {
		return skillId;
	}

	public double getXp() {
		return xp;
	}

	public double getMultiplier() {
		return multiplier;
	}

	public void setMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	@Override
	public List<PluginHandler<? extends PluginEvent>> getMethods() {
		return HANDLERS;
	}

	public static void registerMethod(Class<?> eventType, PluginHandler<? extends PluginEvent> method) {
		HANDLERS.add(method);
	}
}
