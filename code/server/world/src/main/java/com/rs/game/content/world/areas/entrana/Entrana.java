package com.rs.game.content.world.areas.entrana;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.game.content.skills.magic.Magic;
import com.rs.game.content.world.doors.Doors;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class Entrana {

	public static ObjectClickHandler handleMagicDoor = new ObjectClickHandler(new Object[] { 2407 }, e -> {
		Doors.handleDoor(e.getPlayer(), e.getObj());
		Magic.sendNormalTeleportSpell(e.getPlayer(), 0, 0, Tile.of(3093, 3222, 0));
	});

	public static ObjectClickHandler handleEntranaDungeonLadders = new ObjectClickHandler(new Object[] { 2408 }, e -> e.getPlayer().startConversation(new Conversation(e.getPlayer())
            .addNPC(656, HeadE.CONFUSED, "Be careful going in there! You are unarmed, and there is much evilness lurking down there! The evilness seems to block off our contact with our gods,")
            .addNPC(656, HeadE.CONFUSED, "so our prayers seem to have less effect down there. Oh, also, you won't be able to come back this way - This ladder only goes one way!")
            .addNPC(656, HeadE.CONFUSED, "The only exit from the caves below is a portal which is guarded by greater demons!")
            .addOption("Select an Option", "Well, that is a risk I will have to take.", "I don't think I'm strong enough to enter then.")
            .addPlayer(HeadE.CALM_TALK, "Well, that's a risk I will have to take.")
            .addNext(() -> e.getPlayer().useLadder(Tile.of(2822, 9774, 0)))));

	public static NPCClickHandler handleCaveMonkDialogue = new NPCClickHandler(new Object[] { 656 }, e -> e.getPlayer().startConversation(new Conversation(e.getPlayer()) {
        {
            addNPC(656, HeadE.CALM_TALK, "Hello, I don't recommend going down. But if you must, be careful, it is a one way path!");
            addPlayer(HeadE.CALM_TALK, "All right...");
            create();
        }
    }));
}
