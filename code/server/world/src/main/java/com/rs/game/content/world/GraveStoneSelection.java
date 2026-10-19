package com.rs.game.content.world;

import org.darkan.core.cache.EnumDefinitions;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ButtonClickHandler;
import org.darkan.core.gameval.gen.Varp;

@PluginEventHandler
public class GraveStoneSelection {

	public static void openSelectionInterface(Player player) {
		player.getInterfaceManager().sendInterface(652);
		player.getPackets().setIFRightClickOps(652, 31, 0, 78, 0, 1);
		player.getPackets().setIFRightClickOps(652, 34, 0, 13, 0, 1);
		player.getVars().setVar(Varp.gravestone, player.getGraveStone() | 262112);
	}

	public static ButtonClickHandler handleSelectionInterface = new ButtonClickHandler(652, e -> {
		if (e.getComponentId() == 31)
			e.getPlayer().getTempAttribs().setI("graveSelection", e.getSlotId());
		else if (e.getComponentId() == 34)
			confirmSelection(e.getPlayer());
	});

	private static String getStoneName(int slot) {
		return EnumDefinitions.getEnum(1099).getStringValue(slot);
	}

	private static int getStonePrice(int slot) {
		return EnumDefinitions.getEnum(1101).getIntValue(slot);
	}

	public static void confirmSelection(Player player) {
		int slot = player.getTempAttribs().getI("graveSelection", -1) / 6;
		int price = getStonePrice(slot);
		String name = getStoneName(slot);
		if (slot != -1) {
			if (player.getGraveStone() == slot) {
				player.sendMessage("You already have this gravestone selected.");
				return;
			}
			if (!player.getInventory().hasCoins(price)) {
				player.sendMessage("You need " + Utils.formatNumber(price) + " coins to purchase " + Utils.addArticle(name) + ".");
				return;
			}
			player.getTempAttribs().removeI("graveSelection");
			player.getInventory().removeCoins(price);
			player.setGraveStone(slot);
			player.closeInterfaces();
		}
	}
}