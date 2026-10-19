package com.rs.game.tasks;

public abstract class Task implements Runnable {
	protected boolean needRemove;

	public final void stop() {
		needRemove = true;
	}
}
