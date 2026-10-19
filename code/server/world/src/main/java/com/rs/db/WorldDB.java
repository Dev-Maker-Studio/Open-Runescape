package com.rs.db;

import com.rs.Settings;
import com.rs.db.collection.GEManager;
import com.rs.db.collection.HighscoresManager;
import com.rs.db.collection.PlayerManager;
import com.rs.db.collection.logs.LogManager;
import org.darkan.core.Logger;

public class WorldDB extends DBConnection {

	private static final PlayerManager PLAYERS = new PlayerManager();
	private static final HighscoresManager HIGHSCORES = new HighscoresManager();
	private static final GEManager GE = new GEManager();
	private static final LogManager LOGS = new LogManager();

	public WorldDB() {
		super(Settings.getConfig().getMongoDb(), Settings.getConfig().getMongoDBName());
		Logger.info(WorldDB.class, "constructor()", "Connecting to MongoDB and initializing databases...");
		addItemManager(PLAYERS);
		addItemManager(HIGHSCORES);
		addItemManager(GE);
		addItemManager(LOGS);
	}

	public static PlayerManager getPlayers() {
		return PLAYERS;
	}

	public static HighscoresManager getHighscores() {
		return HIGHSCORES;
	}

	public static GEManager getGE() {
		return GE;
	}

	public static LogManager getLogs() {
		return LOGS;
	}

}
