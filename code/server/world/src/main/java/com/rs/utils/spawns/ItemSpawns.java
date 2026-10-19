package com.rs.utils.spawns;

import com.google.gson.JsonIOException;
import org.darkan.core.cache.ItemDefinitions;
import com.rs.game.World;
import org.darkan.core.JsonFileManager;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Logger;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.annotations.ServerStartupEvent.Priority;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PluginEventHandler
public final class ItemSpawns {

	private final static String PATH = "data/items/spawns/";
	final static Charset ENCODING = StandardCharsets.UTF_8;

	private static final Object lock = new Object();
	private static final ArrayList<ItemSpawn> ALL_SPAWNS = new ArrayList<>();
	private static final ArrayList<ItemSpawn> ADDED_SPAWNS = new ArrayList<>();
	private static final Map<Integer, List<ItemSpawn>> ITEM_SPAWNS = new HashMap<>();

	@SuppressWarnings("deprecation")
	public static boolean addSpawn(String username, int id, int amount, int respawnTicks, Tile tile) {
		synchronized (lock) {
			File file = new File("data/items/addedSpawns.json");
			ADDED_SPAWNS.add(new ItemSpawn(id, amount, tile, respawnTicks, ""+ItemDefinitions.getDefs(id).getName()+" added by " + username));
			World.addGroundItemForever(new Item(id, amount), tile, respawnTicks);
			try {
				JsonFileManager.saveJsonFile(ADDED_SPAWNS, file);
			} catch (Exception e) {
				return false;
			}
			return true;
		}
	}

	@ServerStartupEvent(Priority.FILE_IO)
	public static void init() throws JsonIOException, IOException {
		Logger.info(ItemSpawns.class, "init", "Loading item spawns...");
		File[] spawnFiles = new File(PATH).listFiles();
		for (File f : spawnFiles)
			load(f);
		Logger.info(ItemSpawns.class, "init", "Loaded " + ALL_SPAWNS.size() + " item spawns...");
	}

	public static void load(File file) throws IOException {
		if (file.getName().startsWith("_"))
			return;
		if (file.isDirectory()) {
			for (File f : file.listFiles())
				load(f);
			return;
		}
		ItemSpawn[] spawns = JsonFileManager.loadJsonFile(file, ItemSpawn[].class);
		if (spawns != null)
			for(ItemSpawn spawn : spawns)
				add(spawn);
	}

	public static void add(ItemSpawn spawn) {
		if (spawn != null) {
			ALL_SPAWNS.add(spawn);
			List<ItemSpawn> regionSpawns = ITEM_SPAWNS.get(spawn.getTile().getChunkId());
			if (regionSpawns == null)
				regionSpawns = new ArrayList<>();
			regionSpawns.add(spawn);
			ITEM_SPAWNS.put(spawn.getTile().getChunkId(), regionSpawns);
		}
	}

	public static void loadItemSpawns(int chunkId) {
		List<ItemSpawn> spawns = ITEM_SPAWNS.get(chunkId);
		if (spawns != null)
			for (ItemSpawn spawn : spawns)
				spawn.spawn();
	}
}
