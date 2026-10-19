package com.rs.game.content.quests.hazeel_cult.objects

import org.darkan.core.gameval.gen.Loc
import org.darkan.core.gameval.gen.Varp
import com.rs.engine.quest.Quest
import com.rs.game.content.quests.hazeel_cult.STAGE_BRANCH_TASK
import world.gregs.voidps.type.Tile
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

private val CAVE_RAFT_TILE = Tile.of(2567, 9679, 0)
private val CAVE_RAFT_EXIT = Tile.of(2567, 9679, 0)

private val destinations = mapOf(
    1 to Tile.of(2578, 9687, 0),
    2 to Tile.of(2593, 9694, 0),
    3 to Tile.of(2616, 9725, 0),
    4 to Tile.of(2599, 9711, 0),
    5 to Tile.of(2607, 9692, 0)
)

@ServerStartupEvent
fun mapHazeelRaft() {
    onObjectClick(Loc.hazeelsewerraft) { (player, obj) ->
        if (!obj.tile.withinDistance(CAVE_RAFT_TILE, 4)) {
            player.sendMessage("The raft flows back to the cave entrance.")
            player.fadeScreen {
                player.tele(CAVE_RAFT_EXIT)
            }
            return@onObjectClick
        }
        if (player.questManager.getStage(Quest.HAZEEL_CULT) < STAGE_BRANCH_TASK && !player.isQuestComplete(Quest.HAZEEL_CULT)) {
            player.sendMessage("You should speak to Clivet before using his raft.")
            return@onObjectClick
        }

        val correctlySet = (1..5).count { player.vars.bitFlagged(Varp.sewervalves, it) }
        if (correctlySet == 0) {
            player.sendMessage("The current is flowing against the raft. It won't move.")
            return@onObjectClick
        }

        if (correctlySet == 5) {
            player.sendMessage("The raft washes up the sewer until it reaches the end of the passage.")
        } else {
            val island = when (correctlySet) {
                3 -> "fourth"
                4 -> "third"
                1 -> "first"
                else -> "second"
            }
            player.sendMessage("The raft washes up the sewer and stops at the $island island. You'll need to find the right combination of the five sewer valves above if you want to go further.")
        }
        player.fadeScreen {
            player.tele(destinations.getValue(correctlySet))
        }
    }
}
