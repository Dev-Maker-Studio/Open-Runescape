package com.rs.utils.record;

import com.rs.game.World;

public class RecordedAction implements Comparable<RecordedAction> {
	private final int time;
	private final long tick;
	private final long timeLogged;

	public RecordedAction(long timeLogged, int time) {
		this.timeLogged = timeLogged;
		this.time = time;
		this.tick = World.getServerTicks();
	}

	@Override
	public int compareTo(RecordedAction other) {
		return Long.compare(timeLogged, other.timeLogged);
	}

	public int getTime() {
		return time;
	}

	public long getTick() {
		return tick;
	}
	public long getTimeLogged() {
		return timeLogged;
	}
	
	@Override
	public String toString() {
		return "("+time+"ms) " + getClass().getSimpleName() + ": ";
	}
}
