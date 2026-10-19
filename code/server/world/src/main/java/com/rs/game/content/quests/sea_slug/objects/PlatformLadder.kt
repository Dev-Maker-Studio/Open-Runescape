package com.rs.game.content.quests.sea_slug.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Obj
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.sea_slug.*
import com.rs.game.model.entity.Hit
import com.rs.game.model.entity.player.Player
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val LOWER_LADDER_DESTINATION = Tile.of(2784, 3286, 0)
private val UPPER_LADDER_DESTINATION = Tile.of(2784, 3286, 1)

@ServerStartupEvent
fun mapSeaSlugPlatformLadder() {
    onObjectClick(Loc.seaslug_ladder, Loc.seaslug_ladder_top) { (player, obj) ->
        if (obj.tile != LOWER_LADDER_DESTINATION && obj.tile != UPPER_LADDER_DESTINATION) return@onObjectClick
        if (obj.id == Loc.seaslug_ladder_top) {
            player.useLadder(LOWER_LADDER_DESTINATION)
            return@onObjectClick
        }
        player.climbPlatformLadder()
    }
}

private fun Player.climbPlatformLadder() {
    val stage = getQuestStage(Quest.SEA_SLUG)
    if (stage >= STAGE_KENNITH_RESCUED) {
        useLadder(UPPER_LADDER_DESTINATION)
        return
    }
    if (stage == STAGE_UNSTARTED) {
        sendMessage("You have no reason to force your way past the fishermen.")
        return
    }
    if (stage < STAGE_RESCUE_KENNITH) {
        useLadder(UPPER_LADDER_DESTINATION)
        return
    }
    if (!inventory.containsItem(Obj.torch_lit)) {
        sendMessage("You attempt to climb up the ladder.")
        sendMessage("The fishermen approach you...")
        sendMessage("...and smack you on the head with a fishing rod!")
        applyHit(Hit(this, 40, Hit.HitLook.TRUE_DAMAGE))
        sendMessage("Ouch!")
        return
    }
    sendMessage("The fishermen seem afraid of your torch.")
    useLadder(UPPER_LADDER_DESTINATION)
}
