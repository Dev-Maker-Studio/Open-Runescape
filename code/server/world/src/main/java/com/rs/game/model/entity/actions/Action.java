package com.rs.game.model.entity.actions;

import com.rs.game.model.entity.Entity;

public abstract class Action {
	public abstract boolean start(Entity entity);
	public abstract boolean process(Entity entity);
	public abstract int processWithDelay(Entity entity);
	public abstract void stop(Entity entity);
	

	public void setActionDelay(Entity entity, int delay) {
		entity.getActionManager().setActionDelay(delay);
	}
}
