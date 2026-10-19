package com.rs.game.content.world.unorganized_dialogue;

import com.rs.engine.dialogue.Conversation;
import com.rs.game.World;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.gameobject.GameObject;
import org.darkan.core.model.Animation;

public class FlowerPickup extends Conversation {

	public FlowerPickup(Player player, GameObject flowerObject, int flowerId) {
		super(player);
		addOptions("What do you want to do with the flowers?", ops -> {
			ops.add("Pick", () -> {
				player.setNextAnimation(new Animation(827));
				player.getInventory().addItem(getFlowerId(flowerId), 1);
				player.getInventory().refresh();
				World.removeObject(flowerObject);
			});
			ops.add("Leave them");
		});
	}

	public int getFlowerId(int objectId) {
		return 2460 + ((objectId - 2980) * 2);
	}
}