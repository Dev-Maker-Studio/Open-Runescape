package com.rs.game.content.world.unorganized_dialogue;

import com.rs.engine.dialogue.Conversation;
import com.rs.game.model.entity.player.Equipment;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemClickHandler;

@PluginEventHandler
public class FlamingSkull extends Conversation {

	private static final String[] COLORS = { "Green", "Purple", "Blue", "Red" };
	
	public static ItemClickHandler handleSwitchColor = new ItemClickHandler(new Object[] { 24437, 24439, 24440, 24441 }, new String[] { "Change-colour", "Change colour" }, e -> e.getPlayer().startConversation(new FlamingSkull(e.getPlayer(), e.getItem(), e.isEquipped())));
	
	public FlamingSkull(Player player, Item item, boolean worn) {
		super(player);
		int index = (item.getId() == 24437 ? 24442 : item.getId()) - 24439;
		addOptions("What color do you want your skull to be?", ops -> {
			ops.add(COLORS[(index + 1) % 4], () -> {
				int itemId = 24439 + ((index + 1) % 4);
				item.setId(itemId == 24442 ? 24437 : itemId);
				if (worn) {
					player.getEquipment().refresh(Equipment.HEAD);
					player.getAppearance().generateAppearanceData();
				} else
					player.getInventory().refresh(item.getSlot());
			});
			ops.add(COLORS[(index + 2) % 4], () -> {
				int itemId = 24439 + ((index + 2) % 4);
				item.setId(itemId == 24442 ? 24437 : itemId);
				if (worn) {
					player.getEquipment().refresh(Equipment.HEAD);
					player.getAppearance().generateAppearanceData();
				} else
					player.getInventory().refresh(item.getSlot());
			});
			ops.add(COLORS[(index + 3) % 4], () -> {
				int itemId = 24439 + ((index + 3) % 4);
				item.setId(itemId == 24442 ? 24437 : itemId);
				if (worn) {
					player.getEquipment().refresh(Equipment.HEAD);
					player.getAppearance().generateAppearanceData();
				} else
					player.getInventory().refresh(item.getSlot());
			});
		});
	}
}
