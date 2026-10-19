package com.rs.game.content.world.areas.rellekka.npcs;

import com.rs.engine.quest.Quest;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class PeerTheSeer {

	public static NPCClickHandler handler = new NPCClickHandler(new Object[] { "Peer the Seer" }, e -> {
		switch(e.getOption()) {
		case "Deposit":
			if (e.getPlayer().isQuestComplete(Quest.FREMENNIK_TRIALS, "to deposit with Peer."))
				e.getPlayer().getBank().openDepositBox();
			break;
		}
	});

}
