package com.rs.game.content.minigames.shadesofmortton;

import com.rs.game.content.world.doors.Doors;
import world.gregs.voidps.type.Tile;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnObjectHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class ShadeCatacombs {

	public static ObjectClickHandler handleEntrance = new ObjectClickHandler(new Object[] { 31294, 34947 }, e -> {
		if (e.getObjectId() == 31294 && !e.getPlayer().getInventory().containsOneItem(Utils.range(3450, 3469)) && !e.getPlayer().getInventory().containsItem(21511)) {
			e.getPlayer().sendMessage("The door seems securely locked.");
			return;
		}
		e.getPlayer().useStairs(e.getObjectId() == 31294 ? Tile.of(3493, 9725, 0) : Tile.of(3484, 3321, 0));
	});

	public static ObjectClickHandler handleDoors = new ObjectClickHandler(new Object[] { 4106, 4107, 4108, 4109 }, e -> {
		switch(e.getObjectId()) {
		case 4106:
			if (!e.getPlayer().getInventory().containsOneItem(Utils.range(3450, 3469)) && !e.getPlayer().getInventory().containsItem(21511)) {
				e.getPlayer().sendMessage("The door seems securely locked.");
				return;
			}
			Doors.handleDoor(e.getPlayer(), e.getObj());
			break;
		case 4107:
			if (!e.getPlayer().getInventory().containsOneItem(Utils.range(3455, 3469)) && !e.getPlayer().getInventory().containsItem(21511)) {
				e.getPlayer().sendMessage("The door seems securely locked.");
				return;
			}
			Doors.handleDoor(e.getPlayer(), e.getObj());
			break;
		case 4108:
			if (!e.getPlayer().getInventory().containsOneItem(Utils.range(3460, 3469)) && !e.getPlayer().getInventory().containsItem(21511)) {
				e.getPlayer().sendMessage("The door seems securely locked.");
				return;
			}
			Doors.handleDoor(e.getPlayer(), e.getObj());
			break;
		case 4109:
			if (!e.getPlayer().getInventory().containsOneItem(Utils.range(3465, 3469)) && !e.getPlayer().getInventory().containsItem(21511)) {
				e.getPlayer().sendMessage("The door seems securely locked.");
				return;
			}
			Doors.handleDoor(e.getPlayer(), e.getObj());
			break;
		}
	});

	public static ObjectClickHandler handleChestClick = new ObjectClickHandler(new Object[] { 4111, 4112, 4113, 4114, 4115, 4116, 4117, 4118, 4119, 4120, 4121, 4122, 4123, 4124, 4125, 4126, 4127, 4128, 4129, 4130, 59731 }, 
			e -> e.getPlayer().sendMessage("The chest is locked."));

	public static ItemOnObjectHandler handleChests = new ItemOnObjectHandler(new Object[] { 4111, 4112, 4113, 4114, 4115, 4116, 4117, 4118, 4119, 4120, 4121, 4122, 4123, 4124, 4125, 4126, 4127, 4128, 4129, 4130, 59731 }, null, e -> {
		ShadeChest chest = ShadeChest.forId(e.getObjectId());
		chest.open(e.getPlayer(), e.getObject(), e.getItem().getId());
	});
}
