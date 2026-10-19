package com.rs.game.content.quests.the_grand_tree.objects

import org.darkan.core.gameval.gen.Loc
import com.rs.game.content.quests.the_grand_tree.npcs.UNDERGROUND_NARNODE
import com.rs.game.content.quests.the_grand_tree.npcs.spawnUndergroundNarnode
import com.rs.game.content.quests.the_grand_tree.*
import com.rs.game.tasks.WorldTasks
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onObjectClick

@ServerStartupEvent
fun mapNarnodesTrapdoor() {
    onObjectClick(Loc.grandtree_trapdoorunder) { (player) ->
        player.useStairs(UNDERGROUND_NARNODE.transform(1, 0))
        if (player.grandTreeStage in STAGE_DEMON_DEFEATED..STAGE_COMPLETE || player.grandTreeStage == STAGE_STARTED)
            WorldTasks.schedule(2) {
                if (player.isInGrandTreeDungeon()) spawnUndergroundNarnode(player)
            }
    }
}
