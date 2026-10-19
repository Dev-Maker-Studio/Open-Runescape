package com.rs.engine.command;

import com.rs.game.model.entity.player.Player;

public class Command {

	private final String usage;
	private final String description;
	private final CommandExecution execution;

	public Command(String usage, String description, CommandExecution execution) {
		this.description = description;
		this.execution = execution;
		this.usage = usage;
	}

	public void execute(Player player, String[] args) {
		execution.run(player, args);
	}

	public String getDescription() {
		return description;
	}

	public String getUsage() {
		return usage;
	}
}
