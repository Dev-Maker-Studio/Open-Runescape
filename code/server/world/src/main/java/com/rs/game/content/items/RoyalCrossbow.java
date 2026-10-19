package com.rs.game.content.items;

import com.rs.game.content.bosses.qbd.QBDController;
import com.rs.game.model.entity.player.Equipment;
import com.rs.game.model.entity.player.Player;
import com.rs.game.tasks.WorldTasks;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemClickHandler;

@PluginEventHandler
public class RoyalCrossbow {

	public static ItemClickHandler handleRCB = new ItemClickHandler(new Object[] { 24337, 24338, 24339 }, new String[] { "Split", "Brandish" }, e -> {
		Player player = e.getPlayer();
		switch (e.getOption()) {
			case "Split" -> {
				if (player.getInventory().getFreeSlots() < 4) {
					player.sendMessage("You don't have enough inventory space.");
					return;
				}
				player.sendOptionDialogue("Would you like to undo Thurgo's hard work and retrieve the parts back?", ops -> {
					ops.add("Yes, please.", () -> {
						player.getInventory().deleteItem(e.getItem().getId(), 1);
						player.getInventory().addItemDrop(24303, 1);
						player.getInventory().addItemDrop(24340, 1);
						player.getInventory().addItemDrop(24342, 1);
						player.getInventory().addItemDrop(24344, 1);
						player.getInventory().addItemDrop(24346, 1);
						player.sendMessage("You disassemble the crossbow.");
					});
					ops.add("No, thanks.");
				});
			}
			case "Brandish" -> {
				if (!player.getControllerManager().isIn(QBDController.class)) {
					player.sendMessage("There isn't even close to enough heat here. Perhaps the Queen Black Dragon's breath would be hot enough.");
					return;
				}
				if (!player.getTempAttribs().getB("canBrandish")) {
					player.sendMessage("It doesn't seem like a good time to be doing that!");
					return;
				}
				player.anim(16870);
				player.lock(5);
				WorldTasks.delay(4, player::resetReceivedHits);
				player.sendMessage("You brandish the crossbow and it absorbs the dragon's extremely hot fire.");
				e.getItem().setId(24338);
				e.getItem().deleteMetaData();
				player.getEquipment().refresh(Equipment.WEAPON);
				player.getInventory().refresh();
			}
		}
	});

}
