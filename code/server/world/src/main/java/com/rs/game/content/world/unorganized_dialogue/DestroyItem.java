package com.rs.game.content.world.unorganized_dialogue;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.statements.DestroyItemStatement;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import com.rs.plugin.PluginManager;
import com.rs.plugin.events.DestroyItemEvent;

public class DestroyItem extends Conversation {
	
	public DestroyItem(Player player, int slotId, Item item) {
		this(player, slotId, item, null);
	}

	public DestroyItem(Player player, int slotId, Item item, String message) {
		super(player);
		addNext(new DestroyItemStatement(item, message));
		addNext(() -> {
			PluginManager.handle(new DestroyItemEvent(player, item));
			player.getInventory().deleteItem(slotId, item);
			if (item.getDefinitions().isBinded())
				player.getDungManager().unbind(item);
			player.soundEffect(4500, false);
		});
		create();
	}

}
