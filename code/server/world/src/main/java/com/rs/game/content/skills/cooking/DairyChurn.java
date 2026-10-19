package com.rs.game.content.skills.cooking;

import com.rs.game.content.skills.util.CreateActionD;
import org.darkan.core.Constants;
import org.darkan.core.model.Item;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class DairyChurn  {
	private static final Item[][] materials = { { new Item(1927) }, { new Item(1927) }, { new Item(1927) } };
	private static final Item[][] products = { { new Item(2130), new Item(3727) }, { new Item(6697), new Item(3727) }, { new Item(1985), new Item(3727) } };
	private static final int[] reqs = { 21, 38, 48 };
	private static final double[] xp = { 18, 40.5, 64 };
	private static final int[] anims = { -1, -1, -1 };

	public static ObjectClickHandler handleChurns = new ObjectClickHandler(new Object[] { "Dairy churn", "Dairy Churn" }, e -> e.getPlayer().startConversation(new CreateActionD(e.getPlayer(), materials, products, xp, anims, reqs, Constants.COOKING, 8)));
}
