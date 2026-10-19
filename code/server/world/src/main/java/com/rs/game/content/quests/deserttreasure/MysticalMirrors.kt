package com.rs.game.content.quests.deserttreasure

import com.rs.engine.quest.Quest
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick
import org.darkan.core.gameval.gen.Loc

private val mirrorViews = mapOf(
    listOf(Loc.loc_6422, Loc.fd_mirror1) to "You see the Bandit Camp in the desert.",
    listOf(Loc.loc_6424, Loc.fd_mirror2) to "You see a graveyard south-east of Canifis.",
    listOf(Loc.loc_6426, Loc.fd_mirror3) to "You see the icy mountains around Trollheim.",
    listOf(Loc.loc_6428, Loc.fd_mirror4) to "You see the Smoke Dungeon near Pollnivneach.",
    listOf(Loc.loc_6430, Loc.fd_mirror5) to "You see the area around Baxtorian Falls.",
    listOf(Loc.loc_6432, Loc.fd_mirror6) to "You see the Jaldraocht Pyramid in the desert."
)

@ServerStartupEvent
fun mapDesertTreasureMysticalMirrors() {
    mirrorViews.forEach { (mirrors, view) ->
        onObjectClick(*mirrors.toTypedArray()) { (player, _, option) ->
            if (option != "Look-into") return@onObjectClick
            if (player.getQuestStage(Quest.DESERT_TREASURE) < STAGE_RETURN_TO_MIRRORS) return@onObjectClick
            player.simpleDialogue("You gaze into the mirror...", view)
        }
    }
}
