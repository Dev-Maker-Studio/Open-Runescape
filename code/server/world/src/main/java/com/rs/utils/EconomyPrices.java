package com.rs.utils;

import org.darkan.core.cache.ItemDefinitions;
import world.gregs.voidps.cache.definition.data.ItemDefinition;
import com.rs.game.content.ItemConstants;
import org.darkan.core.model.Item;

public final class EconomyPrices {

	public static int getPrice(int itemId) {
		ItemDefinition defs = ItemDefinitions.getDefs(itemId);
		if (defs.isNoted())
			itemId = defs.getCertId();
		else if (defs.isLended())
			itemId = defs.getLendId();
		if (!ItemConstants.isTradeable(new Item(itemId, 1)))
			return 0;
		if (itemId == 12183)
			return 25;
		if (itemId == 995) // TODO after here
			return 1;
		return defs.getHighAlchPrice(); // TODO get price from real item from saved
		// prices from ge
	}

	private EconomyPrices() {

	}
}
