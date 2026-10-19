package com.rs.game;

public record WorldInfo(int number, String ipAddress, int port, String activity, int country, boolean quickChat, boolean lootShare, boolean members, boolean botsAllowed, boolean highlight) {

	public static String getCountryFromId(int country) {
		return switch(country) {
			case 1 -> "USA";
			default -> "Murica";
		};
	}

	public String getCountryName() {
		return getCountryFromId(country);
	}

	public boolean isLobby() {
		return number >= 1000;
	}

	@Override
	public String toString() {
		return "[" + number + ", " + ipAddress + ", " + port + ", " + activity + "]";
	}
}
