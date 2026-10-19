package com.rs.game.content.skills.smithing;

import com.rs.game.content.minigames.ectofuntus.Ectofuntus;
import com.rs.game.content.skills.util.CreateActionD;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Constants;
import org.darkan.core.model.Item;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ItemOnObjectHandler;

@PluginEventHandler
public class MoltenGlassMaking {

	public static int SODA_ASH = 1781;
	public static int BUCKET_OF_SAND = 1783;
	public static int MOLTEN_GLASS = 1775;

	private static final Item[][] materials = { { new Item(SODA_ASH), new Item(BUCKET_OF_SAND) } };
	private static final Item[][] products = { { new Item(MOLTEN_GLASS), new Item(Ectofuntus.EMPTY_BUCKET) } };
	private static final int[] reqs = { 1 };
	private static final double[] xp = { 20 };
	private static final int[] anims = { 3243 };

	public static ItemOnObjectHandler handleCreate = new ItemOnObjectHandler(new Object[] { "Furnace" }, new Object[] { SODA_ASH, BUCKET_OF_SAND }, e -> openDialogue(e.getPlayer()));

	public static void openDialogue(Player player) {
		player.startConversation(new CreateActionD(player, materials, products, xp, anims, reqs, Constants.CRAFTING, 2));
	}
}
