package com.rs.game.content.world.areas.desert.npcs;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.Dialogue;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.statements.NPCStatement;
import com.rs.engine.dialogue.statements.PlayerStatement;
import com.rs.game.model.entity.player.Player;

public class SimonTempletonD extends Conversation {

	public SimonTempletonD(Player player, int npcId) {
		super(player);

		if (player.getInventory().containsItem(6970)) {
			addNext(new PlayerStatement(HeadE.CHEERFUL, "I have a pyramid top I can sell you!"));
			addNext(new Dialogue(new NPCStatement(npcId, HeadE.CHEERFUL, "Excellent job mate! Here's your money."), () -> {
				int totalMoney = player.getInventory().getAmountOf(6970) * 10000;
				player.getInventory().deleteItem(6970, Integer.MAX_VALUE);
				player.getInventory().addCoins(totalMoney);
			}));
		} else {
			addNext(new PlayerStatement(HeadE.CHEERFUL, "Hi, what do you do here?"));
			addNext(new NPCStatement(npcId, HeadE.CHEERFUL, "I'll buy any special artefacts you find here in the desert. If you happen to find any pyramid tops, I'll buy them for 10,000 gold each."));
			addNext(new PlayerStatement(HeadE.CHEERFUL, "Great, I'll be sure to come back if I find any."));
		}

		create();
	}

}
