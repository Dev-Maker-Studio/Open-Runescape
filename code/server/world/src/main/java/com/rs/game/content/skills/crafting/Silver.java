package com.rs.game.content.skills.crafting;

import com.rs.game.model.entity.player.Player;
import org.darkan.core.model.Item;
import org.darkan.core.model.Rights;

import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ButtonClickHandler;

import java.util.HashMap;
import java.util.Map;

@PluginEventHandler
public class Silver {

	public static final int SILVER_BAR = 2355;

	public enum SilverItems {

		HOLY_SYMBOL(16, 50, 1599, new Item(1714, 1), 16),
		UNHOLY_SYMBOL(17, 50, 1594, new Item(1720, 1), 23),
		SILVER_SICKLE(18, 50, 2976, new Item(2961, 1), 30),
		LIGHTNING_ROD(20, 50, 4200, new Item(4201, 1), 37),
		DEMONIC_SIGIL(20, 50, 6747, new Item(6748, 1), 59),
		SILVER_BOLTS(21, 50, 9434, new Item(9382, 10), 66),
		TIARA(23, 52.5, 5523, new Item(5525), 44);

		private static final Map<Integer, SilverItems> rings = new HashMap<>();

		public static SilverItems forId(int buttonId) {
			return rings.get(buttonId);
		}

		static {
			for (SilverItems ring : SilverItems.values())
				rings.put(ring.getButtonId(), ring);
		}

		private final int levelRequired;
		private final double experience;
		private final int mould;
		private final int buttonId;
		private final Item product;

		private SilverItems(int levelRequired, double experience, int itemsRequired, Item producedBar, int buttonId) {
			this.levelRequired = levelRequired;
			this.experience = experience;
			mould = itemsRequired;
			product = producedBar;
			this.buttonId = buttonId;
		}

		public int getMouldRequired() {
			return mould;
		}

		public int getLevelRequired() {
			return levelRequired;
		}

		public Item getProduct() {
			return product;
		}

		public double getExperience() {
			return experience;
		}

		public int getButtonId() {
			return buttonId;
		}
	}

	public static void openSilverInterface(Player player) {
		player.getInterfaceManager().sendInterface(438);
		for (SilverItems item : SilverItems.values())
			player.getPackets().setIFItem(438, item.getButtonId()+1, item.getProduct().getId(), item.getProduct().getAmount());
	}

	public static ButtonClickHandler handleButtons = new ButtonClickHandler(438, e -> {
		e.getPlayer().closeInterfaces();
		SilverItems silver = SilverItems.forId(e.getComponentId());
		int numberToMake = getNumberToMake(e.getOpNum());
		if (numberToMake == -5)
			e.getPlayer().sendInputInteger("How many would you like to make?", number -> {
				if (silver != null)
					e.getPlayer().getActionManager().setAction(new SilverCraftingAction(silver, number));
				else if (e.getPlayer().hasRights(Rights.DEVELOPER))
					e.getPlayer().sendMessage("SILVER: component: " + e.getComponentId() + " packetId: " + e.getOpNum());
				else
					e.getPlayer().sendMessage("You are unable to craft this item at the moment.");
			});
		else if (silver != null)
			e.getPlayer().getActionManager().setAction(new SilverCraftingAction(silver, numberToMake));
		else if (e.getPlayer().hasRights(Rights.DEVELOPER))
			e.getPlayer().sendMessage("SILVER: component: " + e.getComponentId() + " packetId: " + e.getOpNum());
		else
			e.getPlayer().sendMessage("You are unable to craft this item at the moment.");
	});

	public static int getNumberToMake(int packetId) {
        return switch (packetId) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 28;
            case 4 -> -5;
            default -> 1;
        };
	}

}
