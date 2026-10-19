package com.rs.game.content.skills.magic;

import org.darkan.core.cache.ItemDefinitions;
import world.gregs.voidps.cache.definition.data.ItemDefinition;
import com.rs.game.World;
import com.rs.game.content.ItemConstants;
import com.rs.game.content.skills.smithing.Smelting.SmeltingBar;
import com.rs.game.model.entity.player.Equipment;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.managers.InterfaceManager.Sub;
import org.darkan.core.Constants;
import org.darkan.core.model.Animation;
import org.darkan.core.model.Item;
import org.darkan.core.model.SpotAnim;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.InterfaceOnInterfaceHandler;
import org.darkan.core.gameval.gen.Interface;

@PluginEventHandler
public class Alchemy {

	public static InterfaceOnInterfaceHandler handleAlchSuperheat = new InterfaceOnInterfaceHandler(192, new int[] { 38, 50, 59 }, Interface.inventory, null, e -> {
		Item item = e.getPlayer().getInventory().getItem(e.getToSlotId());
		if (item == null)
			return;
		switch(e.getFromComponentId()) {
			case 38 -> handleAlchemy(e.getPlayer(), item, true, true);
			case 50 -> handleSuperheat(e.getPlayer(), item, true);
			case 59 -> handleAlchemy(e.getPlayer(), item, false, true);
		}
	});

	public static boolean handleSuperheat(Player player, Item item, boolean useRunes) {
		if (useRunes)
			player.getInterfaceManager().openTab(Sub.TAB_MAGIC);
		if (!player.canCastSpell() || !Magic.checkMagicAndRunes(player, 43, false, useRunes ? new RuneSet(Rune.NATURE, 1, Rune.FIRE, 4) : null))
			return false;
		player.stopAll(false, false, true);
		SmeltingBar bar = SmeltingBar.forOre(player, item.getId());
		if (bar == null) {
			player.sendMessage("You can only cast this spell on an ore.");
			return false;
		}

		if (!player.getInventory().containsItem(bar.getItemsRequired()[0].getId(), bar.getItemsRequired()[0].getAmount())) {
			player.simpleDialogue("You need " + bar.getItemsRequired()[0].getDefinitions().getName() + " to create a " + bar.getProducedBar().getDefinitions().getName() + ".");
			return false;
		}
		if (bar.getItemsRequired().length > 1)
			if (!player.getInventory().containsItem(bar.getItemsRequired()[1].getId(), bar.getItemsRequired()[1].getAmount()) && !(bar.getItemsRequired()[1].getId() == 453 && (player.getI("coalBag")+player.getInventory().getAmountOf(453)) >= bar.getItemsRequired()[1].getAmount())) {
				player.simpleDialogue("You need " + bar.getItemsRequired()[1].getDefinitions().getName() + " to create a " + bar.getProducedBar().getDefinitions().getName() + ".");
				return false;
			}
		if (player.getSkills().getLevel(Constants.SMITHING) < bar.getLevelRequired()) {
			player.simpleDialogue("You need a Smithing level of at least " + bar.getLevelRequired() + " to smelt " + bar.getProducedBar().getDefinitions().getName());
			return false;
		}
		Magic.checkMagicAndRunes(player, 43, true, useRunes ? new RuneSet(Rune.NATURE, 1, Rune.FIRE, 4) : null);
		player.anim(725);
		player.spotAnim(148, 0, 96);
		for (Item itemReq : bar.getItemsRequired())
			if (itemReq.getId() == 453 && player.getInventory().containsItem(18339) && player.getI("coalBag") > 0) {
				int coalBag = player.getI("coalBag");
				if (coalBag > itemReq.getAmount())
					player.set("coalBag", coalBag - itemReq.getAmount());
				else {
					player.set("coalBag", 0);
					player.getInventory().deleteItem(453, itemReq.getAmount()-coalBag);
				}
			} else
				player.getInventory().deleteItem(itemReq.getId(), itemReq.getAmount());
		player.getInventory().addItem(bar.getProducedBar().getId(), 1);
		player.getSkills().addXp(Constants.MAGIC, 53);
		if (bar == SmeltingBar.GOLD && player.getEquipment().getGlovesId() == 776)
			player.getSkills().addXp(Constants.SMITHING, 56.2);
		else
			player.getSkills().addXp(Constants.SMITHING, bar.getExperience());
		player.addSpellDelay(2);
		return true;
	}

	public static boolean handleAlchemy(Player player, Item item, boolean lowAlch, boolean useRunes) {
		if (useRunes)
			player.getInterfaceManager().openTab(Sub.TAB_MAGIC);
		if (!player.canCastSpell())
			return false;
		if ((!ItemConstants.isTradeable(item)) || item.getId() == 995) {
			player.sendMessage("You can't cast alchemy on that!");
			return false;
		}
		int magicLevel = lowAlch ? 21 : 55;
		RuneSet cost = useRunes ? new RuneSet(Rune.NATURE, 1, Rune.FIRE, lowAlch ? 3 : 5) : null;
		if (!Magic.checkMagicAndRunes(player, magicLevel, false, cost))
			return false;
		if (cost != null) {
			int spent = 0;
			for (Item rune : cost.getRunesToDelete(player))
				if (rune.getId() == item.getId())
					spent += rune.getAmount();
			if (player.getInventory().getNumberOf(item.getId()) - spent < 1) {
				player.sendMessage("You can't cast alchemy on that!");
				return false;
			}
			cost.deleteRunes(player);
		}
		player.stopAll(false, false, true);
		ItemDefinition def = ItemDefinitions.getDefs(item.getId());
		if (def == null)
			return false;
		ItemDefinition wDef = null;
		if (player.getEquipment().getItem(Equipment.WEAPON) != null)
			wDef = ItemDefinitions.getDefs(player.getEquipment().getWeaponId());
		player.incrementCount("Items alched");
		if (lowAlch) {
			if (wDef != null && wDef.getName().toLowerCase().contains("staff")) {
				player.setNextAnimation(new Animation(9625));
				player.setNextSpotAnim(new SpotAnim(1692));
			} else {
				player.setNextAnimation(new Animation(712));
				player.setNextSpotAnim(new SpotAnim(112));

			}
			World.soundEffect(player.getTile(), 98);
			player.getInventory().deleteItem(item.getId(), 1);
			player.getInventory().addCoins(def.getSellPrice());
			player.getSkills().addXp(Constants.MAGIC, 31);
			player.addSpellDelay(2);
		} else {
			if (wDef != null && wDef.getName().toLowerCase().contains("staff")) {
				player.setNextAnimation(new Animation(9633));
				player.setNextSpotAnim(new SpotAnim(1693));
			} else {
				player.setNextAnimation(new Animation(713));
				player.setNextSpotAnim(new SpotAnim(113));
			}
			World.soundEffect(player.getTile(), 98); //low alch id... high alch doesnt match osrs... reeeee
			player.getInventory().deleteItem(item.getId(), 1);
			player.getInventory().addCoins(def.getHighAlchPrice());
			player.getSkills().addXp(Constants.MAGIC, 65);
			player.addSpellDelay(4);
		}
		return true;
	}

}
