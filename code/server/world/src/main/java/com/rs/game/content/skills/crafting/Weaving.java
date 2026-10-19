package com.rs.game.content.skills.crafting;

import com.rs.game.content.skills.util.CreateActionD;
import org.darkan.core.Constants;
import org.darkan.core.model.Item;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class Weaving  {

	private static final Item[][] materials = { { new Item(1759, 4) }, { new Item(5931, 4) }, { new Item(5933, 6) }, { new Item(401, 5), new Item(1794) } };
	private static final Item[][] products = { { new Item(3224) }, { new Item(5418) }, { new Item(5376) }, { new Item(14859) } };
	private static final int[] reqs = { 10, 21, 36, 52 };
	private static final double[] xp = { 12, 38, 56, 83 };
	private static final int[] anims = { -1, -1, -1, -1 };

	public static ObjectClickHandler onClick = new ObjectClickHandler(new Object[] { "Loom" }, e -> e.getPlayer().startConversation(new CreateActionD(e.getPlayer(), materials, products, xp, anims, reqs, Constants.CRAFTING, 2)));
}
