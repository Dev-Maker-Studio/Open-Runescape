package com.rs.game.content.world.areas.barbarian_village;

import com.rs.game.content.world.unorganized_dialogue.StrongholdRewardD;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.managers.EmotesManager;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.ObjectClickHandler;

@PluginEventHandler
public class BarbarianVillage {

    public static ObjectClickHandler handleStrongholdRewards = new ObjectClickHandler(new Object[] { 16135, 16077, 16118, 16047 }, e -> {
        Player player = e.getPlayer();
        switch(e.getObjectId()) {
            case 16135 -> {
                if (player.getEmotesManager().unlockedEmote(EmotesManager.Emote.FLAP))
                    player.sendMessage("You have already claimed your reward from this level.");
                else
                    player.startConversation(new StrongholdRewardD(player, 0));
            }
            case 16077 -> {
                if (player.getEmotesManager().unlockedEmote(EmotesManager.Emote.SLAP_HEAD))
                    player.sendMessage("You have already claimed your reward from this level.");
                else
                    player.startConversation(new StrongholdRewardD(player, 1));
            }
            case 16118 -> {
                if (player.getEmotesManager().unlockedEmote(EmotesManager.Emote.IDEA))
                    player.sendMessage("You have already claimed your reward from this level.");
                else
                    player.startConversation(new StrongholdRewardD(player, 2));
            }
            case 16047 -> player.startConversation(new StrongholdRewardD(player, 3));
        }
    });
}
