package com.rs.game.content.world.unorganized_dialogue.skillmasters;

import com.rs.engine.dialogue.Conversation;
import com.rs.game.content.Skillcapes;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class GenericSkillcapeOwnerD extends Conversation {

	public static NPCClickHandler skillcapeSkill = new NPCClickHandler(new Object[]{8269, 705, 961, 1685, 802, 847, 4906, 575, 308, 4946, 805, 3295, 437, 2270, 3299, 13632, 5113, 9713}, new String[]{"Talk-to", "Talk to"}, e -> {
				String skill = null;
				switch (e.getNPCId()) {
					case 8269 -> skill = "Strength";
					case 705 -> skill = "Defence";
					case 961 -> skill = "Constitution";
					// case 682 -> skill = "Ranging"; see AaronsArcheryAppendagesD.kt
					case 802 -> skill = "Prayer";
					case 1658 -> skill = "Magic";
					case 847 -> skill = "Cooking";
					case 4906 -> skill = "Woodcutting";
					case 575 -> skill = "Fletching";
					case 308 -> skill = "Fishing";
					case 4946 -> skill = "Firemaking";
					case 805 -> skill = "Crafting";
					case 3295 -> skill = "Mining";
					case 437 -> skill = "Agility";
					case 2270 -> skill = "Thieving";
					case 3299 -> skill = "Farming";
					case 13632 -> skill = "Runecrafting";
					case 5113 -> skill = "Hunter";
					case 9713 -> skill = "Dungeoneering";
				}
				e.getPlayer().startConversation(new GenericSkillcapeOwnerD(e.getPlayer(), e.getNPCId(), Skillcapes.valueOf(skill)));
			});

public GenericSkillcapeOwnerD(Player player, int npcId, Skillcapes cape) {
		super(player);
		addOption("Choose an option", "What is that cape you're wearing?", "Bye.")
		.addNext(() -> cape.getOffer99CapeDialogue(player, npcId));
		create();
	}
}
