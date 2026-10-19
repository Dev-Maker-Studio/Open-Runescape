package com.rs.game.model.entity.interactions;

import com.rs.game.model.entity.Entity;

public abstract class Interaction {
	private boolean stopped = false;
	public abstract boolean start(Entity player);
	public abstract boolean process(Entity player);
	public void stop(Entity player) {
		stopped = true;
	}

	public boolean isStopped() {
		return stopped;
	}
}
