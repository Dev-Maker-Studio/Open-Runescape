package com.rs.game.content.skills.util;

import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;

public class CreateAction extends PlayerAction {

	private final int[] anims;
	private final Item[][] materials;
	private final Item[][] products;
	private final double[] xp;
	private final int[] reqs;
	private final int skill;
	private final int delay;
	private final int choice;
	private int quantity = -1;

	public CreateAction(Item[][] materials, Item[][] products, double[] xp, int[] anim, int[] reqs, int skill, int delay, int choice) {
		anims = anim;
		this.materials = materials;
		this.products = products;
		this.xp = xp;
		this.skill = skill;
		this.delay = delay;
		this.choice = choice;
		this.reqs = reqs;
	}

	public CreateAction setQuantity(int quantity) {
		this.quantity = quantity;
		return this;
	}

	public boolean checkAll(Player player) {
		if ((choice >= materials.length) || !player.getInventory().containsItems(materials[choice]) || !player.getInventory().hasRoomFor(materials[choice], products[choice]))
			return false;
		if (reqs != null) {
			if (skill != -1 && player.getSkills().getLevel(skill) < reqs[choice]) {
				player.sendMessage("You need a " + Constants.SKILL_NAME[skill] + " level of " + reqs[choice] + " to make a " + products[choice][0].getName() + ".");
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean start(Player player) {
		return checkAll(player);
	}

	@Override
	public boolean process(Player player) {
		return checkAll(player);
	}

	@Override
	public int processWithDelay(Player player) {
		if (quantity != -1) {
			quantity--;
			if (quantity < 0)
				return -1;
		}
		if (!player.getInventory().hasRoomFor(materials[choice], products[choice])) {
			player.sendMessage("You don't have enough inventory space.");
			return -1;
		}
		if (anims != null)
			player.setNextAnimation(new Animation(anims[choice]));
		for (int i = 0; i < materials[choice].length; i++)
			player.getInventory().deleteItem(materials[choice][i]);
		for (int i = 0; i < products[choice].length; i++)
			player.getInventory().addItemDrop(products[choice][i].clone());
		if (xp != null && skill != -1)
			player.getSkills().addXp(skill, xp[choice]);
		return delay;
	}

	@Override
	public void stop(Player player) {

	}

}
