package com.rs.game.content.skills.crafting;

import com.rs.game.content.skills.util.CreateActionD;
import org.darkan.core.Constants;
import org.darkan.core.model.Item;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnItemHandler;

@PluginEventHandler
public class BattlestaffCrafting  {

	private static final Item[][] materials = { { new Item(1391), new Item(571) }, { new Item(1391), new Item(575) }, { new Item(1391), new Item(569) }, { new Item(1391), new Item(573) } };
	private static final Item[][] products = { { new Item(1395) }, { new Item(1399) }, { new Item(1393) }, { new Item(1397) } };
	private static final int[] reqs = { 54, 58, 62, 66 };
	private static final double[] xp = { 100, 112.5, 125, 137.5 };
	private static final int[] anims = { 16448, 16447, 16449, 16446 };

	public static ItemOnItemHandler craftStaves = new ItemOnItemHandler(1391, new int[] { 569, 571, 573, 575 }, e -> e.getPlayer().startConversation(new CreateActionD(e.getPlayer(), materials, products, xp, anims, reqs, Constants.CRAFTING, 2)));
}