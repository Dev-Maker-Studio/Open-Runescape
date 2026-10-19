package com.rs.utils.spawns;

import com.google.gson.JsonIOException;
import org.darkan.core.JsonFileManager;
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
public final class ObjectSpawns {

	private final static String PATH = "data/map/objectspawns/";
	final static Charset ENCODING = StandardCharsets.UTF_8;

	private static final ArrayList<ObjectSpawn> ALL_SPAWNS = new ArrayList<>();
	private static final Map<Integer, List<ObjectSpawn>> OBJECT_SPAWNS = new HashMap<>();

	@ServerStartupEvent(Priority.FILE_IO)
	public static void init() throws JsonIOException, IOException {
		Logger.info(ObjectSpawns.class, "init", "Loading map object spawns...");
		File[] spawnFiles = new File(PATH).listFiles();
		if (spawnFiles == null) {
			Logger.info(ObjectSpawns.class, "init", "No object spawn directory found at " + PATH);
			return;
		}
		for (File f : spawnFiles) {
			ObjectSpawn[] spawns = (ObjectSpawn[]) JsonFileManager.loadJsonFile(f, ObjectSpawn[].class);
			if (spawns != null)
				for(ObjectSpawn spawn : spawns)
					if (spawn != null)
						add(spawn);
		}
		Logger.info(ObjectSpawns.class, "init", "Loaded " + ALL_SPAWNS.size() + " map object spawns...");
	}

	public static void loadObjectSpawns(int chunkId) {
		List<ObjectSpawn> spawns = OBJECT_SPAWNS.get(chunkId);
		if (spawns != null)
			for (ObjectSpawn spawn : spawns)
				spawn.spawn();
	}

	public static void add(ObjectSpawn spawn) {
		ALL_SPAWNS.add(spawn);
		List<ObjectSpawn> regionSpawns = OBJECT_SPAWNS.get(spawn.getTile().getChunkId());
		if (regionSpawns == null)
			regionSpawns = new ArrayList<>();
		regionSpawns.add(spawn);
		OBJECT_SPAWNS.put(spawn.getTile().getChunkId(), regionSpawns);
	}
}
