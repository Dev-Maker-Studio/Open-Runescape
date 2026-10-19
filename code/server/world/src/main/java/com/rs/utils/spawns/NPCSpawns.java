package com.rs.utils.spawns;

import com.google.gson.JsonIOException;
import org.darkan.core.cache.NPCDefinitions;
import com.rs.game.World;
import org.darkan.core.JsonFileManager;
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
public final class NPCSpawns {

	private final static String PATH = "data/npcs/spawns/";
	final static Charset ENCODING = StandardCharsets.UTF_8;

	private static final Object lock = new Object();
	private static final ArrayList<NPCSpawn> ALL_SPAWNS = new ArrayList<>();
	private static final ArrayList<NPCSpawn> ADDED_SPAWNS = new ArrayList<>();
	private static final Map<Integer, List<NPCSpawn>> NPC_SPAWNS = new HashMap<>();

	public static boolean addSpawn(String username, int id, Tile tile) {
		synchronized (lock) {
			File file = new File("./data/npcs/addedSpawns.json");
			ADDED_SPAWNS.add(new NPCSpawn(id, tile, ""+NPCDefinitions.getDefs(id).getName()+" added by " + username));
			World.spawnNPC(id, tile);
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
		Logger.info(NPCSpawns.class, "init", "Loading NPC spawns...");
		File[] spawnFiles = new File(PATH).listFiles();
		for (File f : spawnFiles)
			load(f);
		Logger.info(NPCSpawns.class, "init", "Loaded " + ALL_SPAWNS.size() + " NPC spawns...");
	}

	public static void load(File file) throws IOException {
		if (file.getName().startsWith("_"))
			return;
		if (file.isDirectory()) {
			for (File f : file.listFiles())
				load(f);
			return;
		}
		NPCSpawn[] spawns = (NPCSpawn[]) JsonFileManager.loadJsonFile(file, NPCSpawn[].class);
		if (spawns != null)
			for(NPCSpawn spawn : spawns)
				add(spawn);
	}

	public static void add(NPCSpawn spawn) {
		if (spawn != null) {
			ALL_SPAWNS.add(spawn);
			List<NPCSpawn> regionSpawns = NPC_SPAWNS.get(spawn.getTile().getChunkId());
			if (regionSpawns == null)
				regionSpawns = new ArrayList<>();
			regionSpawns.add(spawn);
			NPC_SPAWNS.put(spawn.getTile().getChunkId(), regionSpawns);
		}
	}

	public static List<NPCSpawn> getAllSpawns() {
		return ALL_SPAWNS;
	}

	public static void loadNPCSpawns(int chunkId) {
		List<NPCSpawn> spawns = NPC_SPAWNS.get(chunkId);
		if (spawns != null)
			for (NPCSpawn spawn : spawns)
				spawn.spawn();
	}

	public static List<NPCSpawn> getSpawnsForChunk(int chunkId) {
		return NPC_SPAWNS.get(chunkId);
	}
}
