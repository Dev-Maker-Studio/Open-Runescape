package com.rs.game.content.minigames.treasuretrails;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.player.Player;
import org.darkan.core.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class UgiDialogue extends Conversation {
	
	public static NPCClickHandler talk = new NPCClickHandler(new Object[] { 5141 }, e -> {
		if (e.getNPC() instanceof Ugi ugi)
			e.getPlayer().startConversation(new UgiDialogue(e.getPlayer(), ugi));
	});

	public UgiDialogue(Player player, Ugi ugi) {
		super(player);
		
		if (ugi.getOwner() == player && player.getTreasureTrailsManager().getPhase() == 4) {
			addNPC(ugi.getId(), HeadE.CHEERFUL, TreasureTrailsManager.UGIS_QUOTES[Utils.random(TreasureTrailsManager.UGIS_QUOTES.length)]);
			addPlayer(HeadE.CONFUSED, "What?");
			addNext(() -> {
				ugi.finish();
				player.getTreasureTrailsManager().setPhase(5);
				player.getTreasureTrailsManager().setNextClue(TreasureTrailsManager.SOURCE_EMOTE, false);
			});
		} else
			addNPC(ugi.getId(), HeadE.CONFUSED, TreasureTrailsManager.UGI_BADREQS);
		create();
	}
}
