package com.rs.game.model.entity;

public class TestBar extends HitBar {

	private final int type;
	private final int delay;
	private final int perc;
	private final int toPerc;
	private final int timer;

	public TestBar(int type, int delay, int perc, int toPerc, int timer) {
		this.type = type;
		this.delay = delay;
		this.perc = perc;
		this.toPerc = toPerc;
		this.timer = timer;
	}

	@Override
	public int getTimer() {
		return timer;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public int getToPercentage() {
		return toPerc;
	}

	@Override
	public int getDelay() {
		return delay;
	}

	@Override
	public int getPercentage() {
		return perc;
	}

}
