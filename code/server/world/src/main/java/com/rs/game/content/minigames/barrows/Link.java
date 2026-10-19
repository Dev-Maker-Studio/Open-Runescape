package com.rs.game.content.minigames.barrows;

public class Link {

	public enum RoomStatus {
		CLOSED, OPEN, UNDETERMINED;
	}

	private final BarrowsRoom roomA;
	private final BarrowsRoom roomB;
	private RoomStatus state;

	public Link(BarrowsRoom a, BarrowsRoom b) {
		roomA = a;
		roomB = b;
		state = RoomStatus.UNDETERMINED;
	}

	public BarrowsRoom getOther(BarrowsRoom current) {
		return (current == roomA ? roomB : roomA);
	}

	public BarrowsRoom getRoomA() {
		return roomA;
	}

	public BarrowsRoom getRoomB() {
		return roomB;
	}

	public RoomStatus getState() {
		return state;
	}

	public void setState(RoomStatus state) {
		if (this.state == RoomStatus.UNDETERMINED)
			this.state = state;
	}

	@Override
	public String toString() {
		return "Room " + roomA.getName() + " to Room " + roomB.getName() + " = " + getState();
	}
}
