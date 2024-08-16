package com.jagex.game.runetek5.client;

public class Game {

    public static final Game RUNESCAPE = new Game("darkan", "Darkan", 0);
    public static final Game STELLAR_DAWN = new Game("stellardawn", "Stellar Dawn", 1);
    public static final Game GAME3 = new Game("game3", "Game 3", 2);
    public static final Game GAME4 = new Game("game4", "Game 4", 3);
    public static final Game GAME5 = new Game("game5", "Game 5", 4);

    public final String name;
    public final String formattedName;
    public final int id;

    public Game(String name, String formattedName, int id) {
        this.name = name;
        this.formattedName = formattedName;
        this.id = id;
    }

    public static Game[] values() {
        return new Game[]{GAME3, GAME4, STELLAR_DAWN, GAME5, RUNESCAPE};
    }

	public static Game forid(int id) {
	    Game[] values = values();
	    for (int index = 0; index < values.length; index++) {
	        Game game = values[index];
	        if (id == game.id) {
	            return game;
	        }
	    }
	    return null;
	}
}
