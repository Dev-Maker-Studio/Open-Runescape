package com.rs.game.content.minigames.barrows;

import java.util.ArrayList;

public class BarrowsRoom {

	public enum RoomType {
		CORNER, EDGE, TREASURE;
	}

	protected ArrayList<Link> links;

	private final RoomType type;

	private final String name;

	public BarrowsRoom(RoomType type, String name) {
		this.name = name;
		this.type = type;
		links = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public RoomType getType() {
		return type;
	}

	public ArrayList<Link> getLinks() {
		return links;
	}

	public void addLink(BarrowsRoom roomB) {
		Link roomLink = new Link(this, roomB);
		links.add(roomLink);
		roomB.links.add(roomLink);
	}
}