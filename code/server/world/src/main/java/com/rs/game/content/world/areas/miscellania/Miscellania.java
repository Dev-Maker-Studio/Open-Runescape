package com.rs.game.content.world.areas.miscellania;

import com.rs.engine.dialogue.Conversation;
import com.rs.engine.dialogue.HeadE;
import com.rs.engine.dialogue.Options;
import com.rs.game.content.achievements.AchievementSystemD;
import com.rs.game.content.achievements.SetReward;
import world.gregs.voidps.type.Tile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class Miscellania {

	public static NPCClickHandler handleAdvisorGhrim = new NPCClickHandler(new Object[] { 1375 }, e -> e.getPlayer().startConversation(new Conversation(e.getPlayer()) {
        {
            addNPC(e.getNPCId(), HeadE.CHEERFUL, "Hello, what can I do for you?");
            addOptions("What would you like to say?", new Options() {
                @Override
                public void create() {
                    option("About the Achievement System...", () -> new AchievementSystemD(player, e.getNPCId(), SetReward.FREMENNIK_BOOTS));
                }
            });
        }
    }));

	public static ObjectClickHandler handleUndergroundEntrance = new ObjectClickHandler(new Object[] { 15115, 15116 }, e -> e.getPlayer().ladder(e.getObjectId() == 15115 ? Tile.of(2509, 3847, 0) : Tile.of(2509, 10245, 0)));

	public static ObjectClickHandler handleUndergroundCrevices = new ObjectClickHandler(new Object[] { 15186, 15187 }, e -> e.getPlayer().tele(e.getObjectId() == 15186 ? Tile.of(2505, 10283, 0) : Tile.of(2505, 10280, 0)));

	public static ObjectClickHandler handleTrees = new ObjectClickHandler(new Object[] { 46274, 46275, 46277, 15062 }, e -> e.getPlayer().sendMessage("You'd better leave that to the serfs."));

}
