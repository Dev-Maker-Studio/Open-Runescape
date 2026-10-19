package com.rs.engine.dialogue.statements;

import com.rs.game.model.entity.player.Player;

public interface Statement {
	void send(Player player);
	int getOptionId(int componentId);
	void close(Player player);
}
