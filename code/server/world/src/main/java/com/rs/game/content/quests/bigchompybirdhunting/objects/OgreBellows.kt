package com.rs.game.content.quests.bigchompybirdhunting

import com.rs.engine.quest.Quest
import com.rs.game.model.entity.player.Player
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemOnNpc
import com.rs.plugin.kts.onItemOnObject
import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Npc
import org.darkan.core.gameval.gen.Obj

private const val MAX_BLOATED_TOADS = 3

@ServerStartupEvent
fun mapBigChompyOgreBellows() {
    onItemOnObject(arrayOf(Loc.swampbubbles, Loc.swampbubbles_swamp), arrayOf(Obj.empty_ogre_bellows, Obj.filled_ogre_bellow1, Obj.filled_ogre_bellow2, Obj.filled_ogre_bellow3)) { (player, _, item) ->
        if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) < STAGE_GETTING_BLOATED_TOAD) {
            player.sendMessage("Nothing interesting happens.")
            return@onItemOnObject
        }
        player.inventory.replaceItem(Obj.filled_ogre_bellow3, 1, item.slot)
        player.sendMessage("You fill the ogre bellows with swamp gas.")
    }

    onItemOnNpc(Npc.toad) { (player, item) ->
        if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) < STAGE_GETTING_BLOATED_TOAD)
            return@onItemOnNpc
        when (item.id) {
            Obj.empty_ogre_bellows -> player.sendMessage("The air seems too thin to stay in the toad. Perhaps you need something thicker than air.")
            Obj.filled_ogre_bellow3 -> inflateToad(player, item.slot, Obj.filled_ogre_bellow2)
            Obj.filled_ogre_bellow2 -> inflateToad(player, item.slot, Obj.filled_ogre_bellow1)
            Obj.filled_ogre_bellow1 -> inflateToad(player, item.slot, Obj.empty_ogre_bellows)
        }
    }
}

private fun inflateToad(player: Player, bellowsSlot: Int, nextBellowsId: Int) {
    if (player.inventory.getNumberOf(Obj.bloated_toad) < MAX_BLOATED_TOADS && player.inventory.freeSlots == 0) {
        player.sendMessage("You need a free inventory space to catch a bloated toad.")
        return
    }
    player.inventory.replaceItem(nextBellowsId, 1, bellowsSlot)
    player.sendMessage("You manage to catch the toad and inflate it with the swamp gas.")
    if (player.inventory.getNumberOf(Obj.bloated_toad) >= MAX_BLOATED_TOADS) {
        player.sendMessage("One of your bloated toads manage to escape.")
        return
    }
    player.inventory.addItemDrop(Obj.bloated_toad, 1)
    player.sendMessage("You add the bloated toad to your inventory.")
    if (player.getQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING) == STAGE_GETTING_BLOATED_TOAD)
        player.setQuestStage(Quest.BIG_CHOMPY_BIRD_HUNTING, STAGE_SHOWING_BLOATED_TOAD)
}
