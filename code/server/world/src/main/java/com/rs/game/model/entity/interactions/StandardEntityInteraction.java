package com.rs.game.model.entity.interactions;

import com.rs.game.model.entity.Entity;

public class StandardEntityInteraction extends EntityInteraction {

	private final Runnable onReached;

	public StandardEntityInteraction(Entity target, int distance, Runnable onReached) {
		super(target, distance);
		this.onReached = onReached;
	}

	@Override
	public boolean canStart(Entity player) {
		return true;
	}

	@Override
	public boolean checkAll(Entity player) {
		return true;
	}

	@Override
	public void interact(Entity player) {
		onReached.run();
	}

	@Override
	public void onStop(Entity player) {

	}

}