package com.rs.game.content.skills.smithing;

import com.rs.engine.quest.Quest;
import com.rs.game.content.skills.util.CreateActionD;
import org.darkan.core.Constants;
import org.darkan.core.model.Item;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnObjectHandler;

@PluginEventHandler
public class CannonBallSmelting  {

	public static int AMMO_MOULD = 4;
	public static int STEEL_BAR = 2353;

	private static final Item[][] mats = { { new Item(2353) } };
	private static final Item[][] prods = { { new Item(2, 4) } };
	private static final int[] reqs = { 35 };
	private static final double[] xp = { 25.6 };
	private static final int[] anims = { 3243 }; //827, 899

	public static ItemOnObjectHandler handleCreate = new ItemOnObjectHandler(new Object[] { "Furnace" }, new Object[] { STEEL_BAR, AMMO_MOULD }, e -> {
		if (!e.getPlayer().getInventory().containsItem(AMMO_MOULD)) {
			e.getPlayer().sendMessage("You need an ammo mould to smelt cannonballs.");
			return;
		}
		if (e.getPlayer().isQuestComplete(Quest.DWARF_CANNON, "before you can smith cannonballs."))
			e.getPlayer().startConversation(new CreateActionD(e.getPlayer(), mats, prods, xp, anims, reqs, Constants.SMITHING, 7));
	});

}
