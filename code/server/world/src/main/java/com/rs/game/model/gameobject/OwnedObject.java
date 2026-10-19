package com.rs.game.model.gameobject;

import org.darkan.core.model.ObjectShape;
import com.rs.game.World;
import com.rs.game.model.entity.player.Player;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class OwnedObject extends GameObject {

	private static final Map<Integer, OwnedObject> OBJECTS = new ConcurrentHashMap<>();
	private static final Map<String, Map<Integer, OwnedObject>> OWNER_MAP = new ConcurrentHashMap<>();

	private final String owner;
	private boolean destroyed;

	public OwnedObject(Player player, GameObject object) {
		this(player, object.getId(), object.getType(), object.getRotation(), object.getTile());
	}

	public OwnedObject(Player player, GameObject object, int newId) {
		this(player, newId, object.getType(), object.getRotation(), object.getTile());
	}

	public OwnedObject(Player player, int id, ObjectShape type, int rotation, Tile tile) {
		super(id, type, rotation, tile);
		owner = player.getUsername();
	}

	public void tick(Player owner) {

	}

	public void onDestroy() {

	}

	public void onCreate() {

	}

	public Player getOwner() {
		return World.getPlayerByUsername(owner);
	}

	public boolean ownedBy(Player player) {
		return player.getUsername().equals(owner);
	}

	public static void processAll() {
		for (Integer key : OBJECTS.keySet()) {
			OwnedObject o = OBJECTS.get(key);
			if (o == null || o.destroyed)
				continue;
			try {
				o.tick(World.getPlayerByUsername(o.owner));
			} catch(Throwable e) {
				Logger.handle(OwnedObject.class, "process", e);
			}
		}
	}

	public boolean overlapsExisting() {
		return World.getObject(this.getTile(), getType()) != null;
	}

	public final boolean createNoReplace() {
		if (World.getObject(this.getTile(), getType()) != null)
			return false;
		World.spawnObject(this);
		OBJECTS.put(getTile().getTileHash(), this);
		onCreate();
		Map<Integer, OwnedObject> ownedBy = OWNER_MAP.get(owner);
		if (ownedBy == null)
			ownedBy = new ConcurrentHashMap<>();
		ownedBy.put(getTile().getTileHash(), this);
		OWNER_MAP.put(owner, ownedBy);
		return true;
	}

	public final void createReplace() {
		World.spawnObject(this);
		OBJECTS.put(getTile().getTileHash(), this);
		onCreate();
		Map<Integer, OwnedObject> ownedBy = OWNER_MAP.get(owner);
		if (ownedBy == null)
			ownedBy = new ConcurrentHashMap<>();
		ownedBy.put(getTile().getTileHash(), this);
		OWNER_MAP.put(owner, ownedBy);
	}

	public final void destroy() {
		World.removeObject(this);
		OBJECTS.remove(getTile().getTileHash());
		onDestroy();
		Map<Integer, OwnedObject> ownedBy = OWNER_MAP.get(owner);
		if (ownedBy != null) {
			ownedBy.remove(getTile().getTileHash());
			if (ownedBy.size() == 0)
				OWNER_MAP.remove(owner);
			destroyed = true;
		}
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public static Collection<OwnedObject> getOwnedBy(Player owner) {
		if (OWNER_MAP.get(owner.getUsername()) == null)
			return new CopyOnWriteArrayList<>();
		return OWNER_MAP.get(owner.getUsername()).values();
	}

	public static int getNumOwned(Player owner, Class<?> type) {
		int count = 0;
		Collection<OwnedObject> objs = getOwnedBy(owner);
		if (objs == null)
			return 0;
		for (OwnedObject obj : objs)
			if (type.isInstance(obj))
				count++;
		return count;
	}

}
