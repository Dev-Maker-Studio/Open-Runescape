package com.rs.game.content.quests.bigchompybirdhunting.objects

import com.rs.engine.dialogue.HeadE.CALM_TALK
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.bigchompybirdhunting.STAGE_GETTING_BLOATED_TOAD
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import com.rs.utils.Ticks
import org.darkan.core.Constants
import org.darkan.core.Utils
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj

private val OGRE_BELLOWS = intArrayOf(Obj.empty_ogre_bellows, Obj.filled_ogre_bellow1, Obj.filled_ogre_bellow2, Obj.filled_ogre_bellow3)

@ServerStartupEvent
fun mapBigChompyOgreBellowsChest() {
    onObjectClick(Loc.chompybird_chest) { (player, obj) ->
        if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) < STAGE_GETTING_BLOATED_TOAD) {
            player.sendMessage("Hmmm, a rock for a lock....")
            return@onObjectClick
        }
        if (Utils.random(4) == 0) {
            player.playerDialogue(CALM_TALK, "Humph!")
            player.skills.drainLevel(Constants.STRENGTH, 1)
            player.sendMessage("You try to lift the huge rock, but it is too heavy.")
            return@onObjectClick
        }
        player.sendMessage("Hmmm, a rock for a lock....")
        player.playerDialogue(CALM_TALK, "Humph!")
        player.sendMessage("You manage to lift the huge rock off the chest.")
        obj.setIdTemporary(Loc.chompybird_chest_open, Ticks.fromSeconds(30))
        player.playerDialogue(CALM_TALK, "I guess this is what an ogre would call a locked chest.")
    }

    onObjectClick(Loc.chompybird_chest_open) { (player) ->
        if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) < STAGE_GETTING_BLOATED_TOAD) {
            player.sendMessage("You search the chest but find nothing of interest.")
            return@onObjectClick
        }
        if (hasOgreBellows(player)) {
            player.sendMessage("You search but find nothing in the ogre chest. You have already taken the ogre bellows.")
            player.simpleDialogue("You search but find nothing", "in the ogre chest. You have already taken the ogre", "bellows.")
            return@onObjectClick
        }
        player.inventory.addItemDrop(Obj.empty_ogre_bellows, 1)
        player.sendMessage("You search the chest and find a pair of ogre bellows.")
    }
}

private fun hasOgreBellows(player: Player): Boolean =
    OGRE_BELLOWS.any { player.inventory.containsItem(it, 1) || player.bank.containsItem(it, 1) }
