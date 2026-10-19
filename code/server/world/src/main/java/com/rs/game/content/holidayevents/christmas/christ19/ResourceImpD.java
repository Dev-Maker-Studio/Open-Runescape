package com.rs.game.content.holidayevents.christmas.christ19;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.model.entity.player.Player;
import com.rs.plugin.PluginManager;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.events.EnterChunkEvent;
import com.rs.plugin.handlers.NPCClickHandler;

@PluginEventHandler
public class ResourceImpD extends Conversation {

	private static final int IMP_HEAD = 9364;

	public static NPCClickHandler handleSnowImpTalk = new NPCClickHandler(new Object[] { 9372, 9373, 9374, 9375 }, e -> {
		String noun = "";
		int stage = switch (e.getNPC().getId()) {
            case 9372 -> {
                noun = "wine";
                yield 5;
            }
            case 9373 -> {
                noun = "yule logs";
                yield 9;
            }
            case 9374 -> {
                noun = "turkeys";
                yield 7;
            }
            case 9375 -> {
                noun = "taters";
                yield 3;
            }
            default -> 0;
        };
        e.getPlayer().startConversation(new ResourceImpD(e.getPlayer(), noun, stage));
	});

	public ResourceImpD(Player player, String noun, int stage) {
		super(player);

		switch(player.getI(Christmas2019.STAGE_KEY, 0)) {
		case 2:
		case 4:
		case 6:
		case 8:
			addPlayer(HeadE.ANGRY, "Hey! Give those "+noun+" back!");
			addNPC(IMP_HEAD, HeadE.EVIL_LAUGH, "Tee hee hee! Why should I listen to you?");
			addNPC(IMP_HEAD, HeadE.ANGRY, "Because I'm wif him. Give him da "+noun+" back, Snowie's orders!");
			addNPC(IMP_HEAD, HeadE.SCARED, "Oh.. Rasmus.. Sorry, guv. I'll bring da "+noun+" back to da feast den.");
			addNPC(IMP_HEAD, HeadE.ANGRY, "You betta! Get moving dis instant!");
			addPlayer(HeadE.CHEERFUL, "Alright, we'll see you there.", () -> {
				player.set(Christmas2019.STAGE_KEY, stage);
				player.delete(Christmas2019.STAGE_KEY+"loc");
				PluginManager.handle(new EnterChunkEvent(player, player.getChunkId()));
			});
			break;
		default:
			addSimple("You've harassed the imp enough. Talk to Rasmus for the next location.");
			break;
		}

		create();
	}

}
