package com.rs.utils;

import com.google.gson.internal.LinkedTreeMap;
import com.rs.game.model.entity.Entity;
import org.darkan.core.map.pathfinder.Direction;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.item.ItemsContainer;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;
import org.darkan.core.model.WorldObject;
import org.darkan.core.Logger;
import org.darkan.core.model.Vec2;
import kotlin.Pair;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorldUtil {

	public static Pair<Integer, Integer> regionIdToChunkCoords(int regionId) {
		return new Pair<>((regionId >> 8) * 8, (regionId & 0xFF) * 8);
	}

	// TODO: QCMesDefinitions has been removed. This method needs to be reimplemented
	// using QuickChatPhraseDefinition from the modern cache system.
	public static byte[] completeQuickMessage(Player player, int fileId, byte[] data) {
		// Stub: return the raw data as-is until QC phrase completion is reimplemented
		return data;
	}

	public static Tile targetToTile(Object object) {
		if (object instanceof Tile t)
			return t;
		else if (object instanceof Entity e)
			return e.getTile();
		else if (object instanceof WorldObject w)
			return w.getTile();
		throw new IllegalArgumentException("Invalid target object passed.");
	}

	public static Direction getDirectionTo(Entity entity, Object target) {
		Tile targTile = WorldUtil.targetToTile(target);
		Vec2 from = entity.getMiddleTileAsVector();
		Vec2 to = target instanceof Entity e ? e.getMiddleTileAsVector() : new Vec2(targTile);
		Vec2 norm = to.sub(from).norm();
		Tile delta = norm.toTile();
		return Direction.forDelta(delta.getX(), delta.getY());
	}

	public static Direction getFaceDirection(Tile faceTile, Player player) {
		if (player.getX() < faceTile.getX())
			return Direction.EAST;
		if (player.getX() > faceTile.getX())
			return Direction.WEST;
		if (player.getY() < faceTile.getY())
			return Direction.NORTH;
		else if (player.getY() > faceTile.getY())
			return Direction.SOUTH;
		else
			return Direction.NORTH;
	}

	public static boolean collides(Entity entity, Entity target) {
		return entity.getPlane() == target.getPlane() && collides(entity.getX(), entity.getY(), entity.getSize(), target.getX(), target.getY(), target.getSize());
	}

	public static boolean collides(Tile entity, Tile target) {
		return entity.getPlane() == target.getPlane() && collides(entity.getX(), entity.getY(), 1, target.getX(), target.getY(), 1);
	}

	public static boolean collides(Tile entity, Tile target, int s1, int s2) {
		return entity.getPlane() == target.getPlane() && collides(entity.getX(), entity.getY(), s1, target.getX(), target.getY(), s2);
	}

	public static boolean isInRange(Tile entity, Tile target, int rangeRatio) {
		return entity.getPlane() == target.getPlane() && isInRange(entity.getX(), entity.getY(), 1, target.getX(), target.getY(), 1, rangeRatio);
	}

	public static boolean isInRange(Entity entity, Entity target, int rangeRatio) {
		return entity.getPlane() == target.getPlane() && isInRange(entity.getX(), entity.getY(), entity.getSize(), target.getX(), target.getY(), target.getSize(), rangeRatio);
	}

	public static boolean isInRange(Tile entity, Tile target, int rangeRatio, int s1, int s2) {
		return entity.getPlane() == target.getPlane() && isInRange(entity.getX(), entity.getY(), s1, target.getX(), target.getY(), s2, rangeRatio);
	}

	public static boolean collides(int x1, int y1, int size1, int x2, int y2, int size2) {
		int distanceX = x1 - x2;
		int distanceY = y1 - y2;
		return distanceX < size2 && distanceX > -size1 && distanceY < size2 && distanceY > -size1;
	}

	public static boolean isInRange(int x1, int y1, int size1, int x2, int y2, int size2, int maxDistance) {
		int distanceX = x1 - x2;
		int distanceY = y1 - y2;
		if (distanceX > size2 + maxDistance || distanceX < -size1 - maxDistance || distanceY > size2 + maxDistance || distanceY < -size1 - maxDistance)
			return false;
		return true;
	}

	public static double getMemUsedPerc() {
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

		long jvmHeapUsed = heapMemoryUsage.getUsed() / 1048576L; // in MB
		long jvmNonHeapUsed = nonHeapMemoryUsage.getUsed() / 1048576L; // in MB
		long jvmTotalUsed = jvmHeapUsed + jvmNonHeapUsed;

		long jvmMaxMemory = (heapMemoryUsage.getMax() + nonHeapMemoryUsage.getMax()) / 1048576L; // in MB
		double jvmMemUsedPerc = ((double) jvmTotalUsed / jvmMaxMemory) * 100.0;
		return jvmMemUsedPerc;
	}

	public static ItemsContainer<Item> gsonTreeMapToItemContainer(Object metadata) {
		if (metadata == null)
			return null;
		try {
			ItemsContainer<Item> container = null;
			if (metadata instanceof ItemsContainer)
				return (ItemsContainer<Item>) metadata;
			if (metadata instanceof LinkedTreeMap<?,?> serializedContainer) {
				if (serializedContainer.get("alwaysStackable") != null) {
					List<?> items = (List<?>) serializedContainer.get("data");
					container = new ItemsContainer<>(items.size(), (Boolean) serializedContainer.get("alwaysStackable"));
					metadata = items;
				}
			}
			if (metadata instanceof List<?> list) {
				if (container == null)
					container = new ItemsContainer<>(list.size(), false);
				for (Object itemObj : list) {
					if (itemObj instanceof Item item)
						container.add(item);
					else if (itemObj instanceof LinkedTreeMap<?, ?> item) {
						if (item.get("metadata") != null) {
							Object rawMetadata = item.get("metadata");
							if (rawMetadata instanceof Map<?, ?> rawMap) {
                                Map<String, Object> metadataMap = new HashMap<>();
								boolean valid = true;
								for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
									if (entry.getKey() instanceof String) {
										metadataMap.put((String) entry.getKey(), entry.getValue());
									} else {
										valid = false;
										break;
									}
								}
								if (valid)
									container.add(new Item(((Double) item.get("id")).intValue(), ((Double) item.get("amount")).intValue(), metadataMap));
							}
						} else
							container.add(new Item(((Double) item.get("id")).intValue(), ((Double) item.get("amount")).intValue()));
					}
				}
			}
			return container != null && container.isEmpty() ? null : container;
		} catch(Throwable e) {
			Logger.error(WorldUtil.class, "gsonTreeMapToItemContainer", e);
			return null;
		}
	}
}
