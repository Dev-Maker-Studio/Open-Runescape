package com.rs.game.content.items;

public class Lamp {
	private final int slot;
	private final int id;
	private final int req;
	private int selectedSkill;
	private double xp;

	public Lamp(int id, int slot, int req) {
		this.slot = slot;
		this.id = id;
		this.req = req;
	}

	public int getSlot() {
		return slot;
	}

	public int getId() {
		return id;
	}

	public int getReq() {
		return req;
	}

	public int getSelectedSkill() {
		return selectedSkill;
	}

	public void setSelectedSkill(int selectedSkill) {
		this.selectedSkill = selectedSkill;
	}

	public double getXp() {
		return xp;
	}

	public void setXp(double xp) {
		this.xp = xp;
	}
}
