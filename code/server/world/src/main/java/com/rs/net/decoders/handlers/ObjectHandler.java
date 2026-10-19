package com.rs.net.decoders.handlers;

import world.gregs.voidps.cache.definition.data.ObjectDefinition;
import com.rs.engine.pathfinder.RouteEvent;
import com.rs.game.content.minigames.ectofuntus.Ectofuntus;
import com.rs.game.content.skills.dungeoneering.rooms.puzzles.FishingFerretRoom;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.model.Item;
import org.darkan.core.model.Rights;
import org.darkan.core.Logger;
import org.darkan.core.Utils;
import com.rs.plugin.PluginManager;
import com.rs.plugin.events.ItemOnObjectEvent;
import com.rs.plugin.events.ObjectClickEvent;

import java.util.Arrays;

public final class ObjectHandler {

	public static void handleOption1(final Player player, final GameObject object) {
		final ObjectDefinition objectDef = object.getDefinitions(player);
		if (objectDef.getOption(1) == null || PluginManager.handle(new ObjectClickEvent(player, object, 1, false)))
			return;

		player.setRouteEvent(new RouteEvent(object, () -> {
			player.stopAll();
			player.faceObject(object);
			if (!player.getControllerManager().processObjectClick1(object) || player.getTreasureTrailsManager().useObject(object))
				return;
			PluginManager.handle(new ObjectClickEvent(player, object, 1, true));
        }));
		Logger.debug(ObjectHandler.class, "handleOption1", "Object interaction 1: " + object);
	}

	public static void handleOption2(final Player player, final GameObject object) {
		final ObjectDefinition def = object.getDefinitions(player);

		if (def.getOption(2) == null || PluginManager.handle(new ObjectClickEvent(player, object, 2, false)))
			return;

		player.setRouteEvent(new RouteEvent(object, () -> {
			player.stopAll();
			player.faceObject(object);
			if (!player.getControllerManager().processObjectClick2(object) || player.getTreasureTrailsManager().useObject(object))
				return;
			PluginManager.handle(new ObjectClickEvent(player, object, 2, true));
		}));
		Logger.debug(ObjectHandler.class, "handleOption2", "Object interaction 2: " + object);
	}

	public static void handleOption3(final Player player, final GameObject object) {
		final ObjectDefinition def = object.getDefinitions(player);

		if (def.getOption(3) == null || PluginManager.handle(new ObjectClickEvent(player, object, 3, false)))
			return;

		player.setRouteEvent(new RouteEvent(object, () -> {
			player.stopAll();
			player.faceObject(object);
			if (!player.getControllerManager().processObjectClick3(object))
				return;
			PluginManager.handle(new ObjectClickEvent(player, object, 3, true));
		}));
		Logger.debug(ObjectHandler.class, "handleOption3", "Object interaction 3: " + object);
	}

	public static void handleOption4(final Player player, final GameObject object) {
		final ObjectDefinition def = object.getDefinitions(player);

		if (def.getOption(4) == null || PluginManager.handle(new ObjectClickEvent(player, object, 4, false)))
			return;

		player.setRouteEvent(new RouteEvent(object, () -> {
			player.stopAll();
			player.faceObject(object);
			if (!player.getControllerManager().processObjectClick4(object) || PluginManager.handle(new ObjectClickEvent(player, object, 4, true)))
				return;
			player.sendMessage("Nothing interesting happens.");
			Logger.debug(ObjectHandler.class, "handleOption4", "Object interaction 4: " + object);
		}));
	}

	public static void handleOption5(final Player player, final GameObject object) {
		if (object.getDefinitions(player).getOption(5) == null || PluginManager.handle(new ObjectClickEvent(player, object, 5, false)))
			return;

		player.setRouteEvent(new RouteEvent(object, () -> {
			player.stopAll();
			player.faceObject(object);
			if (!player.getControllerManager().processObjectClick5(object) || PluginManager.handle(new ObjectClickEvent(player, object, 5, true)))
				return;
			if (object.getId() != -1)
				player.sendMessage("Nothing interesting happens.");
			Logger.debug(ObjectHandler.class, "handleOption5", "Object interaction 5: " + object);
		}));
	}

	public static void handleOptionExamine(final Player player, final GameObject object) {
		player.getPackets().sendObjectMessage(0, 0xFFFFFF, object, "It's " + Utils.addArticle(object.getDefinitions(player).getName()).toLowerCase() + ".");
		if (player.hasRights(Rights.DEVELOPER)) {
			player.sendMessage(object.toString());
			if (object.getDefinitions().getVarbit() != -1)
				player.sendMessage("Transforms with varbit " + object.getDefinitions().getVarbit() + " - current obj: " + object.getDefinitions(player).getId() + " - current varbit: " + player.getVars().getVarBit(object.getDefinitions().getVarbit()));
			if (object.getDefinitions().getVarp() != -1)
				player.sendMessage("Transforms with var " + object.getDefinitions().getVarp() + " - current obj: " + object.getDefinitions(player).getId());
			if (object.getMeshModifier() != null)
				player.sendMessage(Arrays.toString(object.getMeshModifier().getModelIds()));
		}
	}

	public static void handleItemOnObject(final Player player, final GameObject object, final int interfaceId, final Item item, final int slot) {
		final int itemId = item.getId();
		final ObjectDefinition objectDef = object.getDefinitions(player);

		if (PluginManager.handle(new ItemOnObjectEvent(player, item, object, false)) || FishingFerretRoom.handleFerretThrow(player, object, item))
			return;

		player.setRouteEvent(new RouteEvent(object, () -> {
			player.faceObject(object);
			if (!player.getControllerManager().handleItemOnObject(object, item) || Ectofuntus.handleItemOnObject(player, itemId, object.getId()))
				return;
			PluginManager.handle(new ItemOnObjectEvent(player, item, object, true));
		}));
	}
}