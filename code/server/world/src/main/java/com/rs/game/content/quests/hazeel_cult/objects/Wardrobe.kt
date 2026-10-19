package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.*
import com.rs.game.content.quests.hazeel_cult.npcs.exposeButlerJones
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapJonesWardrobe() {
    onObjectClick(Loc.hazeelcbshut, Loc.hazeelcbopen) { (player) ->
        if (player.isCultSide() || player.questManager.getStage(Quest.HAZEEL_CULT) != STAGE_ENTER_HIDEOUT) {
            player.sendMessage("You search the wardrobe but find nothing of interest.")
            return@onObjectClick
        }
        exposeButlerJones(player)
    }
}
