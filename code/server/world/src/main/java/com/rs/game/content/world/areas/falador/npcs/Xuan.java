package com.rs.game.content.world.areas.falador.npcs;

import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.content.AchievementTitles;
import com.rs.game.content.world.LoyaltyShop;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class Xuan {
	public static NPCClickHandler onNPCClick = new NPCClickHandler(new Object[] { 13727 }, e -> {
		Player player = e.getPlayer();
		NPC npc = e.getNPC();
		switch (e.getOpNum()) {
			case 1 -> {
				if (player.getAuraManager().getJotSkills() >= 10) {
					player.startConversation(new Dialogue()
							.addNPC(npc, HeadE.CHEERFUL, "Good day, my friend! Good day!").voiceEffect(12253)
							.addNPC(npc, HeadE.CHEERFUL, "Wait! I have something here for you...").voiceEffect(11083)
							.addItem(20960, "Xuan hands you a reward book for completing the Jack of Trades.", () -> {
								player.getInventory().addItemDrop(20960, 1);
								player.getAuraManager().deactivate();
								player.getAuraManager().clearJotFlags();
								player.incrementCount("Jack of Trades completed");
							}));
					return;
				}
				player.startConversation(new Dialogue()
						.addNPC(npc, HeadE.CHEERFUL, "It is my privilege to offer you access to an exclusive stock of the finest and most exotic wares.").voiceEffect(13331)
						.addOptions("What would you like help with?", (ops) -> {
							ops.add("Check the Loyalty Point Shop", () -> LoyaltyShop.open(player));
							ops.add("Re-apply my account type title", player::applyAccountTitle);
							ops.add("See your available titles", () -> AchievementTitles.openInterface(player));
							ops.add("Clear my title")
									.addOptions("Really clear your title?", (ops2) -> {
										ops2.add("Yes", player::clearTitle);
										ops2.add("No");
									});
						}));
			}
			case 3 -> LoyaltyShop.open(player);
			case 4 -> player.sendOptionDialogue("Really clear your title?", (ops) -> {
                ops.add("Yes", player::clearTitle);
                ops.add("No");
            });
		}
	});

}
