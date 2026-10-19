package com.rs.game.content;

import org.darkan.core.cache.ItemDefinitions;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ButtonClickHandler;
import java.util.Arrays;
import org.darkan.core.gameval.gen.Varp;

@PluginEventHandler
public class SkillCapeCustomizer {

	public static void resetSkillCapes(Player player) {
		player.setMaxedCapeCustomized(Arrays.copyOf(ItemDefinitions.getDefs(20767).getOriginalModelColors(), 4));
		player.setCompletionistCapeCustomized(Arrays.copyOf(ItemDefinitions.getDefs(20769).getOriginalModelColors(), 4));
	}

	public static void startCustomizing(Player player, int itemId) {
		int[] colors = itemId == 20767 ? player.getMaxedCapeCustomized() : player.getCompletionistCapeCustomized();
		player.getTempAttribs().setO("scCustomColorArr", colors);
		player.getInterfaceManager().sendInterface(20);
		for (int i = 0; i < 4; i++)
			player.getVars().setVarBit(9254 + i, colors[i]);
		player.getPackets().setIFModel(20, 55, player.getAppearance().isMale() ? ItemDefinitions.getDefs(itemId).getMaleWornModelId1() : ItemDefinitions.getDefs(itemId).getFemaleWornModelId1());
	}

	public static void customizeSlot(Player player, int part) {
		int[] colors = player.getTempAttribs().getO("scCustomColorArr");
		if (colors == null) {
			player.closeInterfaces();
			return;
		}
		player.getInterfaceManager().sendInterface(19);
		player.getVars().setVar(Varp.skillcape_colour_interface, colors[part]);
		player.sendInputHSL(colorId -> {
			colors[part] = colorId;
			player.getVars().setVarBit(9254 + part, colorId);
			player.getInterfaceManager().sendInterface(20);
		});
	}

	public static ButtonClickHandler handleSkillCapeCustomizer = new ButtonClickHandler(20, e -> {
		int[] colors = e.getPlayer().getTempAttribs().getO("scCustomColorArr");
		if (colors == null) {
			e.getPlayer().closeInterfaces();
			return;
		}
		switch(e.getComponentId()) {
		case 58 -> {
			int[] orig = Arrays.copyOf(ItemDefinitions.getDefs(20767).getOriginalModelColors(), 4);
            System.arraycopy(orig, 0, colors, 0, orig.length);
			for (int i = 0; i < 4; i++)
				e.getPlayer().getVars().setVarBit(9254 + i, colors[i]);
		}
		case 34 -> customizeSlot(e.getPlayer(), 0);
		case 71 -> customizeSlot(e.getPlayer(), 1);
		case 83 -> customizeSlot(e.getPlayer(), 2);
		case 95 -> customizeSlot(e.getPlayer(), 3);
		case 114, 142 -> {
			e.getPlayer().getAppearance().generateAppearanceData();
			e.getPlayer().closeInterfaces();
		}
		}
	});
}
