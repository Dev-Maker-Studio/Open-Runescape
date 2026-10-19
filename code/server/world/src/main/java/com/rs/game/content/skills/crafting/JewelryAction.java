package com.rs.game.content.skills.crafting;

import org.darkan.core.cache.ItemDefinitions;
import com.rs.game.content.skills.summoning.Scroll;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.actions.PlayerAction;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;

public class JewelryAction extends PlayerAction {

	private final Bling bling;
	private int numberToMake;
	private final boolean pyrefiend;
	
	public JewelryAction(Bling bling, int number, boolean pyrefiend) {
		this.bling = bling;
		this.numberToMake = number;
		this.pyrefiend = pyrefiend;
	}

	public boolean checkAll(Player player) {
		if (bling == null || player == null)
			return false;
		if (pyrefiend && (player.getFamiliar() == null || !player.getInventory().containsItem(Scroll.IMMENSE_HEAT.getId())))
			return false;
		if (!player.getInventory().containsItem(bling.getMouldRequired(), 1)) {
			player.sendMessage("You need one " + ItemDefinitions.getDefs(bling.getMouldRequired()).getName().toLowerCase() + " to make that.");
			return false;
		}
		if (!player.getInventory().containsItems(bling.getItemsRequired())) {
			player.sendMessage("You don't have the items required to make that.");
			return false;
		}
		if (player.getSkills().getLevel(Constants.CRAFTING) < bling.getLevelRequired()) {
			player.sendMessage("You need " + bling.getLevelRequired() + " crafting to make that.");
			return false;
		}
		return true;
	}

	@Override
	public boolean start(Player player) {
		if (checkAll(player))
			return true;
		return false;
	}

	@Override
	public boolean process(Player player) {
		if (checkAll(player)) {
			if (player.getTempAttribs().getO("jewelryObject") != null)
				player.faceObject(player.getTempAttribs().getO("jewelryObject"));
			return true;
		}
		return false;
	}

	@Override
	public int processWithDelay(Player player) {
		numberToMake--;
		if (!pyrefiend)
			player.setNextAnimation(new Animation(3243));
		player.getSkills().addXp(Constants.CRAFTING, bling.getExperience());
		if (pyrefiend) {
			player.getFamiliar().sync(8082, 1394);
			player.spotAnim(1463);
			player.getInventory().deleteItem(Scroll.IMMENSE_HEAT.getId(), 1);
		}
		for (Item required : bling.getItemsRequired())
			player.getInventory().deleteItem(required.getId(), required.getAmount());
		player.getInventory().addItemDrop(bling.getProduct(), 1);
		player.sendMessage("You make a " + ItemDefinitions.getDefs(bling.getProduct()).getName().toLowerCase() + ".", true);
		if (numberToMake > 0)
			return 2;
		return -1;
	}

	@Override
	public void stop(Player player) {

	}

}
