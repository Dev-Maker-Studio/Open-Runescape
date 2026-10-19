package com.rs.game.content.world.doors;

import com.google.gson.JsonIOException;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.JsonFileManager;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.annotations.ServerStartupEvent.Priority;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@PluginEventHandler
public class DoorPair {

	private static final String PATH = "./data/map/doorPairs.json";

	private static DoorPair[] DOOR_PAIRS;
	private static final Map<Integer, Integer> PAIRING_MAP = new HashMap<>();

	private final int closed;
	private final int open;

	@ServerStartupEvent(Priority.FILE_IO)
	public static void loadPairs() {
		try {
			DOOR_PAIRS = (DoorPair[]) JsonFileManager.loadJsonFile(new File(PATH), DoorPair[].class);
		} catch (JsonIOException e) {
			e.printStackTrace();
		}
		for (DoorPair pair : DOOR_PAIRS) {
			PAIRING_MAP.put(pair.open, pair.closed);
			PAIRING_MAP.put(pair.closed, pair.open);
		}
	}

	public static int getOpposingDoor(Player player, GameObject door) {
		return PAIRING_MAP.get(door.getDefinitions(player).getId()) != null ? PAIRING_MAP.get(door.getDefinitions(player).getId()) : 1532;
	}

	public DoorPair(int closed, int open) {
		this.closed = closed;
		this.open = open;
	}

	public int getClosed() {
		return closed;
	}

	public int getOpen() {
		return open;
	}

}
