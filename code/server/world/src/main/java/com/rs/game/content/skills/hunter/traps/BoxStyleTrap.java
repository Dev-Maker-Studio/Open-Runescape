package com.rs.game.content.skills.hunter.traps;

import org.darkan.core.cache.ItemDefinitions;
import org.darkan.core.cache.NPCDefinitions;
import org.darkan.core.model.ObjectShape;
import com.rs.game.World;
import com.rs.game.content.GlobalNPCDropPluginsKt;
import com.rs.game.content.minigames.herblorehabitat.JadinkoType;
import com.rs.game.content.skills.hunter.BoxHunterNPC;
import com.rs.game.content.skills.hunter.BoxHunterType;
import com.rs.game.content.skills.hunter.BoxTrapType;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.OwnedObject;
import com.rs.game.tasks.Task;
import com.rs.game.tasks.WorldTasks;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;
import world.gregs.voidps.type.Tile;

import java.util.Arrays;

public class BoxStyleTrap extends OwnedObject {

	private static final int[] VALID_BAITS = { 1963, 5972, 12535 };

	public enum Status {
		IDLE,
		CATCHING,
		SUCCESS,
		FAIL
	}

	protected BoxTrapType type;
	private int bait = -1;
	private int life = 0;
	private Status status = Status.IDLE;
	private BoxHunterType npcTrapped;

	public BoxStyleTrap(Player player, BoxTrapType type, Tile tile) {
		super(player, type.getObjectId(), ObjectShape.SCENERY_INTERACT, 0, tile);
		this.type = type;
		if (type != BoxTrapType.DEAD_FALL)
			routeType = RouteType.WALK_ONTO;
	}

	public BoxStyleTrap(Player player, BoxTrapType type, Tile tile, int setId, int setRotation) {
		super(player, setId, ObjectShape.SCENERY_INTERACT, setRotation, tile);
		this.type = type;
		if (type != BoxTrapType.DEAD_FALL)
			routeType = RouteType.WALK_ONTO;
	}

	@Override
	public void tick(Player player) {
		life++;
		if (life >= 100) {
			expire(player);
			destroy();
		}
	}

	public void expire(Player player) {
		World.addGroundItem(new Item(type.getId(), 1), Tile.of(this.getTile()), player, true, 60);
		if (player != null)
			player.sendMessage("The " + type.getName() + " trap that you set has collapsed.");
		if (bait != -1)
			World.addGroundItem(new Item(bait, 1), Tile.of(this.getTile()), player, true, 60);
	}

	public void handleCatch(BoxHunterNPC npc, boolean success) {
		BoxHunterType npcType = npc.getType(getOwner());
		if (npcType == null)
			return;
		if (type == BoxTrapType.BIRD_SNARE) {
			changeId(success ? npcType.getObjectCatch() : 19176);
		} else if (type == BoxTrapType.DEAD_FALL)
			changeId(success ? npcType.getObjectCatch() : 19219);
		else
			changeId(npcType.getObjectCatch());
		WorldTasks.schedule(new Task() {
			@Override
			public void run() {
				if (!success && type == BoxTrapType.DEAD_FALL) {
					destroy();
					return;
				}
				setNpcTrapped(npcType);
				changeId(success ? npcType.getObjectSuccess() : npcType.getObjectFail());
				setStatus(success ? Status.SUCCESS : Status.FAIL);
				if (success) {
					npc.anim(new Animation(-1));
					npc.setRespawnTask();
				}
			}
		}, 0);
	}

	public void dismantle(Player player) {
		destroy();
		player.anim(getTrapType().getPickUpAnimation());
		if (getTrapType() == BoxTrapType.TREE_NET)
			player.getInventory().addItemDrop(954, 1);
		player.getInventory().addItemDrop(getTrapType().getId(), 1);
		player.sendMessage("You dismantle the trap.");
		if (bait != -1)
			player.getInventory().addItemDrop(bait, 1);
	}

	public void check(Player player) {
		destroy();
		player.incrementCount(NPCDefinitions.getDefs(getNpcTrapped().getNpcId()).getName()+" trapped");
		player.sendMessage("You've caught a " + NPCDefinitions.getDefs(getNpcTrapped().getNpcId()).getName().toLowerCase() + ".");
		player.anim(getTrapType().getPickUpAnimation());
		for (Item i : getNpcTrapped().getItems(player)) {
			if (i == null || GlobalNPCDropPluginsKt.bonecrush(player, i) || GlobalNPCDropPluginsKt.herbicide(player, i))
				continue;
			player.getInventory().addItemDrop(i);
		}
		if (getTrapType() != BoxTrapType.MAGIC_BOX && getTrapType() != BoxTrapType.DEAD_FALL)
			player.getInventory().addItemDrop(getTrapType().getId(), 1);
		if (getTrapType() == BoxTrapType.TREE_NET)
			player.getInventory().addItemDrop(954, 1);
		if (getTrapType() == BoxTrapType.MARASAMAW_PLANT) {
			player.getSkills().addXp(Constants.HUNTER,getNpcTrapped().getXp() * player.getEquipment().getWitchDoctorBoost());
			BoxHunterType boxType = getNpcTrapped();
			if (boxType == null) {
				return;
			}
			String boxName = boxType.name();
			String baseName = boxName.substring(0, boxName.lastIndexOf("_JADINKO"));
			JadinkoType jadinkoType;
			try {
				jadinkoType = JadinkoType.valueOf(baseName);
			} catch (IllegalArgumentException e) {
				return;
			}
			JadinkoType.Companion.captureJadinko(player, jadinkoType);
		}
		else
			player.getSkills().addXp(Constants.HUNTER, getNpcTrapped().getXp());
		player.getInventory().refresh();
	}

	public BoxTrapType getTrapType() {
		return type;
	}

	public int getBait() {
		return bait;
	}

	public boolean tryBait(Player player, int bait) {
		if (Arrays.binarySearch(VALID_BAITS, bait) < 0) {
			player.sendMessage("That's not something you'd want to use to bait a trap with.");
			return false;
		}
		if (!player.getInventory().containsItem(bait))
			return false;
		if (this.bait != -1) {
			player.sendMessage("This trap has already been baited.");
			return false;
		}
		player.getInventory().deleteItem(bait, 1);
		this.bait = bait;
		player.sendMessage("You bait the trap with the " + ItemDefinitions.getDefs(bait).getName().toLowerCase() + ".");
		return true;
	}

	public int getLife() {
		return life;
	}

	public void setStatus(Status status) {
		this.status = status;
		life = 0;
	}

	public Status getStatus() {
		return status;
	}

	public void setLife(int life) {
		this.life = life;
	}

	public BoxHunterType getNpcTrapped() {
		return npcTrapped;
	}

	public void setNpcTrapped(BoxHunterType npcTrapped) {
		this.npcTrapped = npcTrapped;
	}
}
