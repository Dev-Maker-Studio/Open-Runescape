package com.rs.game.content.skills.util;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.statements.MakeXStatement;
import com.rs.game.model.entity.player.Player;

import java.util.Arrays;

public class CreationActionD extends Conversation {

	public CreationActionD(Player player, Category category, int material, int animation, int delay, int gfx, int product, CreationAction customAction, ReqItem[] options, boolean consistentAnim) {
		super(player);
		options = product != -1 ? new ReqItem[] { ReqItem.getRequirements(product) } : options != null ? options : ReqItem.getProducts(category, material);

		var makeX = addNext(new MakeXStatement("What would you like to make?", Arrays.stream(options).mapToInt(item -> item.getProduct().getId()).toArray(), 28));

		for (ReqItem item : options) {
			makeX.addNext(() -> {
				int quantity = MakeXStatement.getQuantity(player);
				ReqItem produce = product == -1 ? item : ReqItem.getRequirements(product);
				player.getActionManager().setAction(customAction != null ? customAction : new CreationAction(produce, animation, gfx, delay, quantity).setConsistentAnimation(consistentAnim));
			});
		}

		create();
	}

	public CreationActionD(Player player, Category category, int material, int animation, int delay) {
		this(player, category, material, animation, delay, -1, -1, null, null, false);
	}

	public CreationActionD(Player player, int product, int animation, int delay) {
		this(player, null, -1, animation, delay, -1, -1, null, null, false);
	}

	public CreationActionD(Player player, Category category, ReqItem[] options, int animation, int delay) {
		this(player, category, -1, animation, delay, -1, -1, null, options, false);
	}
	
	public CreationActionD(Player player, Category category, int material, int animation, int delay, CreationAction action, boolean consistentAnim) {
		this(player, category, material, animation, delay, -1, -1, action, null, false);
	}

	public CreationActionD(Player player, int product, int animation, int delay, CreationAction action) {
		this(player, null, -1, animation, delay, -1, -1, action, null, false);
	}

	public CreationActionD(Player player, Category category, ReqItem[] options, int animation, int delay, CreationAction action) {
		this(player, category, -1, animation, delay, -1, -1, action, options, false);
	}
	
	public CreationActionD(Player player, Category category, int material, int animation, int delay, boolean consistentAnim) {
		this(player, category, material, animation, delay, -1, -1, null, null, consistentAnim);
	}

	public CreationActionD(Player player, int product, int animation, int delay, boolean consistentAnim) {
		this(player, null, -1, animation, delay, -1, -1, null, null, consistentAnim);
	}

	public CreationActionD(Player player, Category category, ReqItem[] options, int animation, int delay, boolean consistentAnim) {
		this(player, category, -1, animation, delay, -1, -1, null, options, consistentAnim);
	}
}
