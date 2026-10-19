package com.rs.game.content.holidayevents.halloween;

import com.rs.game.World;
import com.rs.game.content.holidayevents.halloween.hw07.Halloween2007;
import com.rs.game.content.holidayevents.halloween.hw09.Halloween2009;
import com.rs.game.map.Chunk;
import com.rs.game.map.ChunkManager;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.model.GroundItem;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Logger;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.annotations.ServerStartupEvent;
import com.rs.plugin.annotations.ServerStartupEvent.Priority;
import com.rs.utils.Ticks;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.List;

@PluginEventHandler
public class PumpkinSpawning {

	static IntSet regionsToSpawn = IntSet.of(12850, 11828, 12084, 12853, 12597, 12342, 10806, 10547, 13105);
	static int pumpkinCount = 0;
	static int pumpkinsPerChunk = 2;

	@ServerStartupEvent(Priority.POST_PROCESS)
	public static void initSpawning() {
		if (!Halloween2007.ENABLED && !Halloween2009.ENABLED)
			return;
		ChunkManager.permanentlyPreloadRegions(regionsToSpawn);
		WorldTasks.scheduleHalfHourly(() -> {
			try {
				spawnPumpkins();
				World.sendWorldMessage("<col=EB6123><shad=000000>Pumpkins have spawned in various cities around the world!", false);
			} catch (Throwable e) {
				Logger.handle(PumpkinSpawning.class, "initSpawning", e);
			}
		});
	}

	public static int countPumpkins(int chunkId) {
		pumpkinCount = 0;
		List<GroundItem> itemSpawns = ChunkManager.getChunk(chunkId, true).getAllGroundItems();
		if (itemSpawns != null && !itemSpawns.isEmpty())
			itemSpawns.forEach( spawn -> {
				if (spawn.getId() == 1959)
					pumpkinCount += 1;
			});
		return pumpkinCount;
	}

	public static void spawnPumpkins() {
		for (int id : World.mapRegionIdsToChunks(regionsToSpawn, 0)) {
			Chunk r = ChunkManager.getChunk(id, true);
			int eggsNeeded = pumpkinsPerChunk-countPumpkins(id);
			for (int i = 0; i < eggsNeeded; i++) {
				int x = r.getBaseX()+Utils.random(8);
				int y = r.getBaseY()+Utils.random(8);
				Tile tile = Tile.of(x, y, 0);
				int tries = 0;
				while (!World.floorAndWallsFree(tile, 1) && tries < 5) {
					x = r.getBaseX()+Utils.random(8);
					y = r.getBaseY()+Utils.random(8);
					tile = Tile.of(x, y, 0);
					tries++;
				}
				World.addGroundItemNoExpire(new Item(1959), Tile.of(x, y, 0));
			}
		}
	}
}
