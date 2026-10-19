package com.rs.engine.command;

import com.rs.game.model.entity.player.Player;

public interface CommandExecution {
	void run(Player p, String[] args);
}
